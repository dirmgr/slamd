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
package com.slamd.tools.udpping;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;



/**
 * This program defines a daemon that can be used to implement a basic echo
 * service over UDP (user datagram protocol).  Any UDP traffic that the server
 * receives will be immediately sent back to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class UDPPingServer
{
  /**
   * The default port on which this server will listen for connections.
   */
  public static final int DEFAULT_LISTEN_PORT = 7777;



  /**
   * Parses the command-line arguments and starts the UDP ping server as
   * appropriate.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // Set default values for all configurable options.
    boolean verboseMode = false;
    int     listenPort  = DEFAULT_LISTEN_PORT;


    // Process the command-line arguments.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-p"))
      {
        listenPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("Unrecognized argument \"" + args[i] +  '"');
        displayUsage();
        System.exit(1);
      }
    }


    // Create the "socket" that will be used to accept requests.
    DatagramSocket serverSocket = null;
    try
    {
      serverSocket = new DatagramSocket(listenPort);
      if (verboseMode)
      {
        System.err.println("Listening on UDP port " + listenPort +
                           " for requests.");
      }
    }
    catch (Exception e)
    {
      System.err.println("Unable to create server socket:  " + e);
      System.exit(1);
    }


    // Create an array that will be used to hold the data of the request.  In
    // this case, we'll just limit it to 4096 bytes.  Any larger datagrams that
    // are received (which should not happen) will simply be truncated.
    byte[]         rxBytes  = new byte[4096];
    DatagramPacket rxPacket = new DatagramPacket(rxBytes, rxBytes.length);


    // Operate in a loop that will wait for a request to arrive and then
    // immediately echo it back to the sender.
    while (true)
    {
      try
      {
        serverSocket.receive(rxPacket);
        serverSocket.send(rxPacket);

        if (verboseMode)
        {
          System.err.println(rxPacket.getLength() + " bytes received from " +
                             rxPacket.getAddress().getHostAddress());
        }
      }
      catch (IOException ioe)
      {
        if (verboseMode)
        {
          System.err.println("Error handling UDP request:  " + ioe);
        }
      }
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String eol = System.getProperty("line.separator");

    System.err.println(
"USAGE:  java UDPPingServer [options]" + eol +
"        where [options] may include:" + eol +
"-p {port}  -- Specifies the port on which this server should listen" + eol +
"-v         -- Indicates that the server should operate in verbose mode" + eol +
"-H         -- Displays this usage information"
                      );
  }
}

