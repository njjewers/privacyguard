package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
<<<<<<< HEAD
  private final int limit = 2048;
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  private LinkedList<byte[]> request;

  public TCPReceiver(Socket socket, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socket.getChannel();
=======
  private ConcurrentLinkedQueue<byte[]> responses = new ConcurrentLinkedQueue<byte[]>();
  private int lastAck = 1, start = 1, seq = 1, counter = 0;
  private final int maxlength = 2500, maxCounter = 2;
  private ByteBuffer msg = ByteBuffer.allocate(maxlength);
  private long lastTime = 0, timeout = 1000;

  public TCPReceiver(SocketChannel socketChannel, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socketChannel;
>>>>>>> retransmit
    this.forwarder = forwarder;
    this.selector = selector;
    request = new LinkedList<byte[]>();
  }

  public void send(byte[] data) {
    synchronized(request) {
      request.add(data);
    }
  }

  @Override
  public void run() {
    while(!forwarder.isClosed() && selector.isOpen()) {
      try {
        if(selector.select() == 0) break;
      } catch (IOException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "Selected");
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
<<<<<<< HEAD
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "Selected");
=======
>>>>>>> retransmit
      while(iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(!key.isValid()) continue;
        else if(key.isReadable()) {
          try {
            Log.d(TAG, "Readable");
            msg.clear();
            int length = socketChannel.read(msg);
            if(length < 0) {
              forwarder.close();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            //responses.add(temp);
            //Log.d(TAG, "" + seq + " , " + lastAck + " , " + length);
            //if(seq == lastAck) {
            //  fetch(seq, false);
            //}
            forwarder.receive(temp);
            Thread.sleep(100);
          } catch (IOException e) {
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
    Log.d(TAG, "Thread exit");
  }

  private void handleTimeout(int ack, boolean data) {

  }
<<<<<<< HEAD
=======

  public synchronized void fetch(int ack, boolean data) {
    long current = System.nanoTime();
    Log.d(TAG, "Time : " + (current - lastTime));
    if(current - lastTime > timeout) handleTimeout(ack, data);
    Log.d(TAG, "ACK : " + ack + ", " + start);
    if(ack == lastAck) counter ++;
    if(counter > maxCounter) { // lost too much, just leave them
      counter = 0;
      while(seq > ack && !responses.isEmpty()) {
        seq -= responses.element().length;
        responses.remove();
      }
    }
    lastAck = ack;
    seq = lastAck;
    while(start < ack && !responses.isEmpty()) {
      start += responses.element().length;
      responses.remove();
    }
    if(start < ack) {
      Log.e(TAG, "ERROR : " + ack + ", " + start);
      return;
    }
    assert(start == ack);
    if(!responses.isEmpty()) {
      seq += responses.element().length;
      lastTime = System.nanoTime();
      forwarder.receive(responses.element());
    }
  }
>>>>>>> retransmit
}
