package com.y59song.Forwader;

import android.util.Log;
import com.y59song.Forwader.Receiver.TCPReceiver;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Network.LocalServer;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Network.TCPConnectionInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements ICommunication {
  private final String TAG = "TCPForwarder";
<<<<<<< HEAD
  protected SocketChannel socketChannel;
  protected Socket socket;
  protected TCPReceiver receiver;
  protected TCPConnectionInfo conn_info;
=======
  private SocketChannel socketChannel;
  private TCPReceiver receiver;
  private TCPConnectionInfo conn_info;
>>>>>>> retransmit

  public enum Status {
    DATA, LISTEN, SYN_ACK_SENT, HALF_CLOSE_BY_CLIENT, HALF_CLOSE_BY_SERVER, CLOSED;
  }

  protected Status status;

  public TCPForwarder(MyVpnService vpnService) {
    super(vpnService);
    status = Status.LISTEN;
  }

  /*
   * step 1 : reverse the IP header
   * step 2 : create a new TCP header, set the syn, ack right
   * step 3 : get the response if necessary
   * step 4 : combine the response and create a new tcp datagram
   * step 5 : update the datagram's checksum
   * step 6 : combine the tcp datagram and the ip datagram, update the ip header
   */
  protected synchronized void forward (IPDatagram ipDatagram) {
    if(closed) return;
    byte flag;
    int len, rlen;
    if(ipDatagram != null) {
      flag = ((TCPHeader)ipDatagram.payLoad().header()).getFlag();
      len = ipDatagram.payLoad().virtualLength();
      rlen = ipDatagram.payLoad().dataLength();
      if(conn_info == null) conn_info = new TCPConnectionInfo(ipDatagram);
      //conn_info.setAck(((TCPHeader)ipDatagram.payLoad().header()).getSeq_num());
      //conn_info.setSeq(((TCPHeader)ipDatagram.payLoad().header()).getAck_num());
    } else return;
    Log.d(TAG, "" + status + "," + closed);
    switch(status) {
      case LISTEN:
        if(flag != TCPHeader.SYN) {
          close();
          return;
        }
        conn_info.reset(ipDatagram);
<<<<<<< HEAD
        conn_info.setup(this);
=======
>>>>>>> retransmit
        conn_info.increaseSeq(
          forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.SYNACK), null))
        );
        status = Status.SYN_ACK_SENT;
        break;
      case SYN_ACK_SENT:
<<<<<<< HEAD
        if(flag == TCPHeader.SYN) {
          status = Status.LISTEN;
          forward(ipDatagram);
        } else {
          assert(flag == TCPHeader.ACK);
          status = Status.DATA;
=======
        if(flag != TCPHeader.ACK) {
          close();
          return;
>>>>>>> retransmit
        }
        status = Status.DATA;
        conn_info.setup(this);
        break;
      case DATA:
        assert((flag & TCPHeader.ACK) != 0);
        if(rlen > 0) { // send data
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          send(ipDatagram.payLoad());
        } else if(flag == TCPHeader.FINACK) { // FIN
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
          );
          status = Status.HALF_CLOSE_BY_CLIENT;
          close();
        } else if((flag & TCPHeader.RST) != 0) { // RST
          close();
        }
        break;
      case HALF_CLOSE_BY_CLIENT:
        assert(flag == TCPHeader.ACK);
        status = Status.CLOSED;
        close();
        break;
      case HALF_CLOSE_BY_SERVER:
        if(flag == TCPHeader.FINACK) {
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          status = Status.CLOSED;
          close();
        } // ELSE ACK for the finack sent by the server
        break;
      case CLOSED:
        status = Status.CLOSED;
      default:
        break;
    }
    //if(receiver == null || conn_info == null) return; // only if the client send ack
    //receiver.fetch(((TCPHeader)ipDatagram.payLoad().header()).getAck_num(), len > 0);
  }

  @Override
  public synchronized void receive (byte[] response) {
    if(conn_info == null) return;
//    /Log.d("Response", ByteOperations.byteArrayToHexString(response));
    conn_info.increaseSeq(
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.DATA), response))
    );
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void send(IPPayLoad payLoad) {
<<<<<<< HEAD
    if (socket != null && !socket.isConnected()) {
      status = Status.END_SERVER;
      forward(null);
    } else {
      try {
        socketChannel.write(ByteBuffer.wrap(payLoad.data()));
      } catch (IOException e) {
        e.printStackTrace();
      }
=======
    //receiver.clear(((TCPHeader)payLoad.header()).getAck_num());
    if(isClosed()) {
      status = Status.HALF_CLOSE_BY_SERVER;
      conn_info.increaseSeq(
        forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
      );
    }
    try {
      // Non-blocking
      socketChannel.write(ByteBuffer.wrap(payLoad.data()));
    } catch (IOException e) {
      e.printStackTrace();
>>>>>>> retransmit
    }
  }

  @Override
  public void open() {
    if(!closed) return;
    super.open();
    status = Status.LISTEN;
  }

  @Override
  public void close() {
    if(closed) return;
    closed = true;
    conn_info = null;
    if(socketChannel != null) {
      try {
        socketChannel.close();
        socketChannel = null;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    vpnService.getForwarderPools().release(this);
  }

  @Override
  public void setup(InetAddress srcAddress, int src_port) {
    try {
      if(socketChannel == null) socketChannel = SocketChannel.open();
<<<<<<< HEAD
      socket = socketChannel.socket();
      socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      socketChannel.connect(new InetSocketAddress(LocalServer.port));
=======
      vpnService.protect(socketChannel.socket());
      socketChannel.connect(new InetSocketAddress(dstAddress, port));
>>>>>>> retransmit
      socketChannel.configureBlocking(false);
      Selector selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ);
      receiver = new TCPReceiver(socketChannel, this, selector);
      new Thread(receiver).start();
      Log.d(TAG, "START");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
  public static void test() {
    try {
      SocketChannel socketChannel = SocketChannel.open();
      socketChannel.connect(new InetSocketAddress(12345));
      byte[] temp = new byte[1];
      temp[0] = 'a';
      Log.d("Test", "Test");
      socketChannel.write(ByteBuffer.wrap(temp));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  */
}
