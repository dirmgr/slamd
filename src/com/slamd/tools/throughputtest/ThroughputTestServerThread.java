/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.tools.throughputtest;



import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;



/**
 * This class defines a thread that will be used by the throughput test server
 * that will spew data to a single client connection until that client
 * disconnects (or any kind of I/O problem occurs).
 *
 *
 * @author   Neil A. Wilson
 */
public class ThroughputTestServerThread
       extends Thread
{
  // The byte buffer that will be used when sending data to the client.
  private ByteBuffer dataBuffer;

  // The connection to the client.
  private SocketChannel clientChannel;

  // The address and port of the client that connected.
  private String clientAddress;



  /**
   * Creates a new throughput test server thread with the provided information.
   *
   * @param  throughputServer  The server that created this thread.
   * @param  clientChannel     The channel used to communicate with the client.
   *
   * @throws  IOException  If a problem occurs while creating the thread.
   */
  public ThroughputTestServerThread(ThroughputTestServer throughputServer,
                                    SocketChannel clientChannel)
         throws IOException
  {
    this.clientChannel = clientChannel;
    clientAddress = clientChannel.socket().getInetAddress().getHostAddress() +
                    ':' + clientChannel.socket().getPort();
    System.out.println("Accepted a client connection from " + clientAddress);

    if (throughputServer.useTCPNoDelay)
    {
      clientChannel.socket().setTcpNoDelay(throughputServer.useTCPNoDelay);
    }

    dataBuffer = ByteBuffer.allocateDirect(throughputServer.bufferSize);
    dataBuffer.put(new byte[throughputServer.bufferSize]);
    dataBuffer.rewind();
  }



  /**
   * Loops, sending data to the client as quickly as possible.
   */
  public void run()
  {
    try
    {
      while (true)
      {
        clientChannel.write(dataBuffer);
        dataBuffer.rewind();
      }
    }
    catch (Exception e)
    {
      try
      {
        clientChannel.close();
      } catch (Exception e2) {}
    }

    System.out.println("Closed client connection from " + clientAddress);
  }
}

