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
package com.slamd.tools.tcpreplay;



import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;



/**
 * This class defines a utility that may be used to capture data from one or
 * more clients so that it may be replayed against a server later for the
 * purpose of load generation.  Any data captured will be forwarded onto the
 * server and any response from that server will be passed back to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class CaptureDaemon
{
  // Indicates whether a request has been made to stop this capture daemon.
  private boolean stopRequested;

  // The port on which to listen for new connections from clients.
  private int listenPort;

  // The port on the server to which data read from the client should be
  // forwarded.
  private int serverPort;

  // The selector that will be used to multiplex the accept and read operations.
  private Selector selector;

  // The name of the output file to which the captured data should be written.
  private String outputFile;

  // The address of the server to which data read from the client should be
  // forwarded.
  private String serverHost;

  // The IP address of the server.
  private String serverIP;



  /**
   * Creates a new capture daemon with the provided information.
   *
   * @param  listenPort  The port on which to listen for new connections from
   *                     clients.
   * @param  serverHost  The address of the server to which client requests
   *                     should be forwarded.
   * @param  serverPort  The port of the server to which client requests should
   *                     be forwarded.
   * @param  outputFile  The name of the file to which the captured data should
   *                     be written.
   *
   * @throws  UnknownHostException  If the server address cannot be resolved to
   *                                an IP address.
   */
  public CaptureDaemon(int listenPort, String serverHost, int serverPort,
                       String outputFile)
         throws UnknownHostException
  {
    this.listenPort = listenPort;
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.outputFile = outputFile;

    stopRequested = false;

    serverIP = InetAddress.getByName(serverHost).getHostAddress();

    Runtime.getRuntime().addShutdownHook(new CaptureShutdownThread(this));
  }



  /**
   * Starts listening for new connections from clients.  Whenever a connection
   * arrives, it will be handed off to other threads for processing.
   *
   * @throws  IOException  If a problem occurs while creating the server socket
   *                       to accept client connections or opening the output
   *                       file to which to write the captured data.
   */
  public void captureData()
         throws IOException
  {
    stopRequested = false;

    // Create the selector that will be used to multiplex accept and read
    // operations.
    selector = Selector.open();


    // Open the output stream that should be used to write the capture data.
    FileOutputStream outputStream = new FileOutputStream(outputFile, true);


    // Create the server socket channel that will be used to accept new
    // connections.
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(listenPort));
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);


    // Loop, waiting for new connections to arrive and/or data to be available
    // on existing connections.  When something does happen, handle it
    // appropriately.
    ByteBuffer buffer = ByteBuffer.allocate(4096);
    while (! stopRequested)
    {
      int selectedKeys = selector.select(100);
      if (selectedKeys > 0)
      {
        Iterator iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext())
        {
          try
          {
            SelectionKey key = (SelectionKey) iterator.next();

            if (key.isAcceptable())
            {
              // The server socket channel has a new connection available.
              // Connect to the backend server and
              serverSocketChannel = (ServerSocketChannel) key.channel();
              SocketChannel clientChannel = serverSocketChannel.accept();
              clientChannel.socket().setTcpNoDelay(true);
              clientChannel.configureBlocking(false);

              InetSocketAddress serverAddress =
                   new InetSocketAddress(serverHost, serverPort);
              SocketChannel serverChannel = SocketChannel.open(serverAddress);
              serverChannel.configureBlocking(false);
              serverChannel.finishConnect();
              serverChannel.socket().setTcpNoDelay(true);

              clientChannel.register(selector, SelectionKey.OP_READ,
                                     serverChannel);
              serverChannel.register(selector, SelectionKey.OP_READ,
                                     clientChannel);
            }
            else if (key.isReadable())
            {
              // The channel has data available for reading.  Read it and
              // forward it on to the other end of the connection.
              SocketChannel readChannel  = (SocketChannel) key.channel();
              SocketChannel writeChannel = (SocketChannel) key.attachment();

              // See if the data is from the client side.  If so, then we will
              // need to write the data to the output file.
              boolean fromClient = true;
              Socket  readSocket = readChannel.socket();
              if (readSocket.getInetAddress().getHostAddress().
                       equals(serverIP) && (readSocket.getPort() == serverPort))
              {
                fromClient = false;
              }


              int bytesRead = readChannel.read(buffer);
  outerLoop:  while (bytesRead > 0)
              {
                if (fromClient)
                {
                  buffer.flip();
                  byte[] data = new byte[bytesRead];
                  buffer.get(data);
                  CaptureData captureData =
                       new CaptureData(1, System.currentTimeMillis(), data);
                  captureData.encodeTo(outputStream);
                }

                buffer.flip();
                int totalBytesWritten = 0;
                while (totalBytesWritten < bytesRead)
                {
                  int bytesWritten = writeChannel.write(buffer);
                  if (bytesWritten < 0)
                  {
                    writeChannel.close();
                    readChannel.close();
                    key.cancel();
                    break outerLoop;
                  }
                  else
                  {
                    totalBytesWritten += bytesWritten;
                  }
                }

                buffer.clear();
                bytesRead = readChannel.read(buffer);
              }

              if (bytesRead < 0)
              {
                writeChannel.close();
                readChannel.close();
                key.cancel();
              }
            }
          }
          catch (Exception e)
          {
            System.err.println("Exception caught while processing selection " +
                               "key:  " + e);
          }

          iterator.remove();
        }
      }
    }


    // Close the output file.
    outputStream.flush();
    outputStream.close();


    // Close and cancel all the channels associated with the selector.
    Iterator iterator = selector.keys().iterator();
    while (iterator.hasNext())
    {
      SelectionKey key = (SelectionKey) iterator.next();
      key.channel().close();
      key.cancel();
    }
  }



  /**
   * Stops the process of capturing data from clients.
   */
  public void stopCapture()
  {
    stopRequested = true;
    selector.wakeup();
  }
}

