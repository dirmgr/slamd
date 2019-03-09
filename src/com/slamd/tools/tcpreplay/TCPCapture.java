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



import java.net.UnknownHostException;



/**
 * This program can be used to capture communication between one or more clients
 * and a backend server so that the data can be replayed later for the purpose
 * of load generation.
 *
 *
 * @author   Neil A. Wilson
 */
public class TCPCapture
{
  /**
   * Parses the command-line arguments, creates the TCP capture daemon, and
   * starts capturing data.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // Specify default values for all the command-line arguments.
    int    listenPort = -1;
    int    serverPort = -1;
    String outputFile = null;
    String serverHost = null;


    // Parse the arguments.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-L"))
      {
        listenPort = Integer.parseInt(args[++i]);
        if ((listenPort < 1) || (listenPort > 65535))
        {
          System.err.println("ERROR:  Listen port must be between 1 and 65535");
          System.exit(1);
        }
      }
      else if (args[i].equals("-h"))
      {
        serverHost = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        serverPort = Integer.parseInt(args[++i]);
        if ((serverPort < 1) || (serverPort > 65535))
        {
          System.err.println("ERROR:  Server port must be between 1 and 65535");
          System.exit(1);
        }
      }
      else if (args[i].equals("-o"))
      {
        outputFile = args[++i];
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("ERROR:  Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }



    // Verify that all the required parameters were provided.
    if (listenPort < 0)
    {
      System.err.println("ERROR:  No listen port provided (use -L)");
      displayUsage();
      System.exit(1);
    }

    if (serverHost == null)
    {
      System.err.println("ERROR:  No server host provided (use -h)");
      displayUsage();
      System.exit(1);
    }

    if (serverPort < 0)
    {
      System.err.println("ERROR:  No server port provided (use -p)");
      displayUsage();
      System.exit(1);
    }

    if (outputFile == null)
    {
      System.err.println("ERROR:  No output file provided (use -o)");
      displayUsage();
      System.exit(1);
    }


    // Create the capture daemon.
    CaptureDaemon captureDaemon = null;
    try
    {
      captureDaemon = new CaptureDaemon(listenPort, serverHost, serverPort,
                                        outputFile);
      System.out.println("Listening for client connections on port " +
                         listenPort);
    }
    catch (UnknownHostException uhe)
    {
      System.err.println("ERROR:  Unable to resolve server address \"" +
                         serverHost + "\" to an IP address.");
      System.exit(1);
    }


    // Start capturing data.  The capture will continue until the user
    // interrupts the process (e.g., with a Ctrl+C).
    try
    {
      captureDaemon.captureData();
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  Exception caught while capturing data:  " +
                         e);
      System.exit(1);
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String EOL = System.getProperty("line.separator");
    System.out.println(
"USAGE:  java TCPCapture {options}" + EOL +
"        where {options} include" + EOL +
"-L {port}     -- Specifies the port on which accept client connections" + EOL +
"-h {address}  -- Specifies the address of the server to which to" + EOL +
"                 forward requests from clients" + EOL +
"-p {port}     -- Specifies the port of the server to which to forward" + EOL +
"                 requests from clients" + EOL +
"-o {file}     -- Specifies the output file to which the captured data" + EOL +
"                 should be written" + EOL +
"-H            -- Displays this usage information"
                      );
  }
}

