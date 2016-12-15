package com.PrivacyGuard.Application.Network.Forwarder;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.IP.IPDatagram;
import com.PrivacyGuard.Application.Network.IP.IPHeader;
import com.PrivacyGuard.Application.Network.LocalServer;
import com.PrivacyGuard.Application.Network.UDP.UDPDatagram;
import com.PrivacyGuard.Application.Network.UDP.UDPHeader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * Created by uhengart on 09/06/16.
 *
 * Thread that reads UDP response packets from socket, adds header, and puts them into queue emptied by TunWriteThreat
 * There is one such thread per UDP request source port
 */
public class UDPForwarderWorker extends Thread {

    private final String TAG = "UDPForwarderWorker";
    private final int LIMIT = 32767;
    private UDPForwarder forwarder;
    private IPHeader newIPHeader;
    private UDPHeader newUDPHeader;
    private int srcPort;
    private DatagramSocket socket;

    // ipDatagram is a request UDP packet that is going to be put into socket by forwarder
    // this thread will wait for responses to this request (or later request UDP packets sent from same source port)
    public UDPForwarderWorker(IPDatagram ipDatagram, DatagramSocket socket, UDPForwarder forwarder) {
        this.socket = socket;
        this.forwarder = forwarder;
        this.newIPHeader = ipDatagram.header().reverse();
        UDPDatagram udpDatagram = (UDPDatagram)ipDatagram.payLoad();
        this.newUDPHeader = (UDPHeader)udpDatagram.header().reverse();
        // will have to update address/port of sender of response before we can create the UDP response
    }

    @Override
    public void interrupt(){
        super.interrupt();
        // closing the socket will interrupt the receive() operation
        if (socket != null) socket.close();
    }

    public void run() {
        ByteBuffer packet = ByteBuffer.allocate(LIMIT);
        DatagramPacket datagram = new DatagramPacket(packet.array(), LIMIT);

        try {
            while (!isInterrupted()) {
                packet.clear();
                //socket.setSoTimeout(60000);
                socket.receive(datagram);

                byte[] received = Arrays.copyOfRange(datagram.getData(), 0, datagram.getLength());
                if (received != null) {
                    newIPHeader.updateSrcAddress(datagram.getAddress());
                    newUDPHeader.updateSrcPort(datagram.getPort());
                    UDPDatagram response = new UDPDatagram(newUDPHeader, received);
                    //response.debugInfo(dstAddress);
                    Logger.d(TAG, "response is " + response.debugString());
                    forwarder.forwardResponse(newIPHeader, response);
                }
            }
        } catch (SocketException e) {
            // receive() got interrupted
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
