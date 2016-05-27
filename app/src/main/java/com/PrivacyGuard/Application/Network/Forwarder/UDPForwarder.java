/*
 * Implement a simple udp protocol
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.PrivacyGuard.Application.Network.Forwarder;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.MyVpnService;
import com.PrivacyGuard.Application.Network.IP.IPDatagram;
import com.PrivacyGuard.Application.Network.IP.IPHeader;
import com.PrivacyGuard.Application.Network.IP.IPPayLoad;
import com.PrivacyGuard.Application.Network.UDP.UDPDatagram;
import com.PrivacyGuard.Application.Network.UDP.UDPHeader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-29.
 */
public class UDPForwarder extends AbsForwarder implements ICommunication {
    private final String TAG = "UDPForwarder";
    private final int LIMIT = 32767;
    private InetAddress dstAddress;
    private int dstPort;
    private DatagramSocket socket;
    private ByteBuffer packet;
    private DatagramPacket response;

    public UDPForwarder(MyVpnService vpnService, int port) {
        super(vpnService, port);
        packet = ByteBuffer.allocate(LIMIT);
        response = new DatagramPacket(packet.array(), LIMIT);
    }

    @Override
    public void forwardRequest(IPDatagram ipDatagram) {
        if(closed) return;
        UDPDatagram udpDatagram = (UDPDatagram)ipDatagram.payLoad();
        setup(null, -1, ipDatagram.header().getDstAddress(), ipDatagram.payLoad().getDstPort());
        udpDatagram.debugInfo(dstAddress);
        send(udpDatagram);

        // May misorder responses when multiple UDP packets are concurrently sent from the
        // same source port, like concurrent DNS requests. But they should all go to the same
        // DNS server so reversing the wrong packet shouldn't be a problem.
        IPHeader newIPHeader = ipDatagram.header().reverse();
        UDPHeader newUDPHeader = (UDPHeader)udpDatagram.header().reverse();
        byte[] received = receive();
        if(received != null) {
            UDPDatagram response = new UDPDatagram(newUDPHeader, received);
            response.debugInfo(dstAddress);
            forwardResponse(newIPHeader, response);
        }
        close();
    }

    @Override
    // Should never get called since we don't use LocalServer for UDP
    public void forwardResponse(byte[] response) {
        Logger.d("UDPForwarder", "Unsolicited packet received: " + response);
    }

    @Override
    public boolean setup(InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort) {
        try {
            socket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        vpnService.protect(socket);
        this.dstAddress = dstAddress;
        this.dstPort = dstPort;
        return true;
    }

    @Override
    public void send(IPPayLoad payLoad) {
        try {
            // UDP packets are currently not filtered and don't go through LocalServer
            socket.send(new DatagramPacket(payLoad.data(), payLoad.dataLength(), dstAddress, dstPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] receive() {
        try {
            packet.clear();
            socket.setSoTimeout(10000);
            socket.receive(response);
        } catch (SocketTimeoutException e) {
            close();
            Logger.d("UDPForwarder", "Socket Timeout Exception");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Arrays.copyOfRange(response.getData(), 0, response.getLength());
    }

    @Override
    public void close() {
        if(socket != null && !socket.isClosed()) socket.close();
        closed = true;
        vpnService.getForwarderPools().release(this);
        Logger.d(TAG, "Releasing UDP forwarder for port " + port);
    }
}