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
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;



/**
 * This program defines a multithreaded server that will accept connections from
 * clients and will then spew a sequence of bytes to that client as quickly as
 * possible.  The client is responsible for accepting that data and keeping
 * track of how much it received.  The server will continue to provide data to
 * the client until the client disconnects.
 *
 *
 * @author   Neil A. Wilson
 */
public class ThroughputTestServer
{
  /**
   * The default buffer size to use in bytes.
   */
  public static final int DEFAULT_BUFFER_SIZE = 8192;



  /**
   * The default port on which to listen for connections.
   */
  public static final int DEFAULT_LISTEN_PORT = 3333;



  // Indicates whether to use TCP_NODELAY when sending data to the client.
  protected boolean useTCPNoDelay;

  // The size of the buffer size to use when sending data to the client.
  protected int bufferSize;

  // The port on which the server will listen for connections from clients.
  private int listenPort;



  /**
   * Parses the command-line arguments and invokes the constructor with the
   * appropriate values.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    boolean useTCPNoDelay = false;
    int     bufferSize    = DEFAULT_BUFFER_SIZE;
    int     listenPort    = DEFAULT_LISTEN_PORT;

    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-b"))
      {
        bufferSize = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-p"))
      {
        listenPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-N"))
      {
        useTCPNoDelay = true;
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("Unrecognized argument \"" + args[i]);
        displayUsage();
        System.exit(1);
      }
    }


    ThroughputTestServer throughputServer =
         new ThroughputTestServer(listenPort, bufferSize, useTCPNoDelay);

    try
    {
      throughputServer.handleClients();
    }
    catch (IOException ioe)
    {
      System.err.println("Unable to listen for client connections:  " + ioe);
      ioe.printStackTrace();
      System.exit(1);
    }
  }



  /**
   * Creates a new instance of the throughput test server with the provided
   * information.
   *
   * @param  listenPort     The TCP port on which the server should listen for
   *                        connections from clients.
   * @param  bufferSize     The buffer size in bytes to use when sending data to
   *                        the clients.
   * @param  useTCPNoDelay  Indicates whether to use TCP_NODELAY when sending
   *                        data to the client.
   */
  public ThroughputTestServer(int listenPort, int bufferSize,
                              boolean useTCPNoDelay)
  {
    this.listenPort    = listenPort;
    this.bufferSize    = bufferSize;
    this.useTCPNoDelay = useTCPNoDelay;
  }



  /**
   * Creates a server socket to accept client connections and then operates in
   * an infinite loop, accepting new connections and handing them off to worker
   * threads to be handled.
   *
   * @throws  IOException  If a problem occurs while creating the server socket
   *                       to accept the client connections.
   */
  public void handleClients()
         throws IOException
  {
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.socket().bind(new InetSocketAddress(listenPort));
    serverChannel.configureBlocking(true);
    System.out.println("Listening on port " + listenPort +
                       " for client connections");

    while (true)
    {
      try
      {
        SocketChannel clientChannel = serverChannel.accept();
        new ThroughputTestServerThread(this, clientChannel).start();
      }
      catch (Exception e)
      {
        System.err.println("Error accepting client connection:  " + e);
        e.printStackTrace();
      }
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String EOL = System.getProperty("line.separator");

    System.err.println(
"Usage:  java ThroughputTestServer [options]" + EOL +
"        where [options] include:" + EOL +
"-b [size]  -- Specifies the buffer size to use in bytes (default is " +
     DEFAULT_BUFFER_SIZE + ')' + EOL +
"-p [port]  -- Specifies the port on which to listen for clients (default is " +
     DEFAULT_LISTEN_PORT + ')' + EOL +
"-N         -- Indicates that TCP_NODELAY should be used when sending data" +
     EOL +
"-H         -- Displays this usage information"
                      );
  }
}

