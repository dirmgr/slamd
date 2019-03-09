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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;



/**
 * This program defines a simple client that will establish a
 * connection to the throughput test server and read data from it as quickly as
 * the server sends it.  The client can run for a given maximum duration or
 * until a specified number of bytes have been transferred.
 *
 *
 * @author   Neil A. Wilson
 */
public class ThroughputTestClient
{
  // Indicates whether the client should stop reading data from the server.
  protected boolean shouldStop;

  // The buffer used to hold data read from the server.
  private ByteBuffer readBuffer;

  // The maximum number length of time in seconds that the client should run.
  private int maxDuration;

  // The port to use to connect to the server.
  private int serverPort;

  // The maximum number of bytes of data to read from the server.
  private long maxBytes;

  // The number of bytes read so far from the server.
  private long bytesRead;

  // The time that the client started reading data from the server.
  private long startTime;

  // The time that the client stopped reading data from the server.
  private long stopTime;

  // The address to use to connect to the server.
  private String serverAddress;



  /**
   * Parses the command-line arguments and creates a new throughput test client
   * based on the provided information.
   *
   * @param  args  The command-line arguments to use for this program.
   */
  public static void main(String[] args)
  {
    // Set default values for the command-line arguments.
    int    bufferSize    = ThroughputTestServer.DEFAULT_BUFFER_SIZE;
    int    maxDuration   = -1;
    int    serverPort    = ThroughputTestServer.DEFAULT_LISTEN_PORT;
    long   maxBytes      = -1;
    String serverAddress = "127.0.0.1";


    // Parse  the arguments provided to the program.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        serverAddress = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        serverPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-B"))
      {
        bufferSize = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-b"))
      {
        maxBytes = Long.parseLong(args[++i]);
      }
      else if (args[i].equals("-d"))
      {
        maxDuration = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that either a max duration or max bytes was specified.
    if ((maxBytes <= 0) && (maxDuration <= 0))
    {
      System.err.println("Either maximum number of bytes (-b) or maximum " +
                         "duration (-d) must be greater than zero");
      displayUsage();
      System.exit(1);
    }


    // Create the client and do the work.
    ThroughputTestClient client =
         new ThroughputTestClient(serverAddress, serverPort, bufferSize,
                                  maxBytes, maxDuration);

    try
    {
      client.readData();
    }
    catch (IOException ioe)
    {
      System.err.println("Unable to connect to throughput test server:  " +
                         ioe);
      ioe.printStackTrace();
      System.exit(1);
    }

    long bytesRead            = client.getBytesRead();
    long elapsedTime          = client.getDurationMillis();
    double bytesPerSecond     = 1.0 * bytesRead / (elapsedTime / 1000);
    double bitsPerSecond      = bytesPerSecond * 8;
    double kilobytesPerSecond = bytesPerSecond / 1024;
    double kilobitsPerSecond  = bitsPerSecond/ 1024;
    double megabytesPerSecond = kilobytesPerSecond / 1024;
    double megabitsPerSecond  = kilobitsPerSecond / 1024;


    DecimalFormat decimalFormat = new DecimalFormat("0.000");
    System.out.println("Read " + bytesRead + " bytes in " + elapsedTime +
                       " milliseconds");
    System.out.println("Bits per second:       " +
                       decimalFormat.format(bitsPerSecond));
    System.out.println("Bytes per Second:      " +
                       decimalFormat.format(bytesPerSecond));
    System.out.println("Kilobits per Second:   " +
                       decimalFormat.format(kilobitsPerSecond));
    System.out.println("Kilobytes per Second:  " +
                       decimalFormat.format(kilobytesPerSecond));
    System.out.println("Megabits per Second:   " +
                       decimalFormat.format(megabitsPerSecond));
    System.out.println("Megabytes per Second:  " +
                       decimalFormat.format(megabytesPerSecond));
  }



  /**
   * Creates a new throughput test client with the provided information.  Note
   * that at least one of maxBytes and maxDuration must be greater than zero.
   *
   * @param  serverAddress  The address of the throughput test server.
   * @param  serverPort     The port of the throughput test server.
   * @param  bufferSize     The buffer size to use when reading data.
   * @param  maxBytes       The maximum number of bytes to read from the server.
   * @param  maxDuration    The maximum length of time in seconds to read data.
   */
  public ThroughputTestClient(String serverAddress, int serverPort,
                              int bufferSize, long maxBytes, int maxDuration)
  {
    this.serverAddress = serverAddress;
    this.serverPort    = serverPort;
    this.maxBytes      = maxBytes;
    this.maxDuration   = maxDuration;

    startTime  = 0;
    stopTime   = 0;
    bytesRead  = 0;
    readBuffer = ByteBuffer.allocateDirect(bufferSize);
  }



  /**
   * Establishes a connection to the throughput test server then reads data in a
   * loop until either the maximum number of bytes have been read, the client
   * has run for the maximum duration, or an error occurs while reading data
   * from the server.
   *
   * @throws  IOException  If a problem occurs while establishing the connection
   *                       to the server.
   */
  public void readData()
         throws IOException
  {
    InetSocketAddress socketAddress =
         new InetSocketAddress(serverAddress, serverPort);
    SocketChannel clientChannel = SocketChannel.open(socketAddress);
    clientChannel.configureBlocking(true);

    if (maxDuration > 0)
    {
      new ThroughputTestClientTimer(this, maxDuration).start();
    }

    startTime = System.currentTimeMillis();
    while (! shouldStop)
    {
      try
      {
        int bufferBytesRead = clientChannel.read(readBuffer);
        readBuffer.clear();

        if (bufferBytesRead > 0)
        {
          bytesRead += bufferBytesRead;
          if ((maxBytes > 0) && (bytesRead >= maxBytes))
          {
            shouldStop = true;
          }
        }
        else
        {
          System.err.println("Unexpected end of input stream from server");
          shouldStop = true;
        }
      }
      catch (Exception e)
      {
        System.err.println("Error reading data from server:  " + e);
        shouldStop = true;
      }
    }
    stopTime = System.currentTimeMillis();

    try
    {
      clientChannel.close();
    } catch (Exception e) {}
  }



  /**
   * Retrieves the length of time in milliseconds that the client was reading
   * data from the server.
   *
   * @return  The length of time in milliseconds that the client was reading
   *          data from the server.
   */
  public long getDurationMillis()
  {
    return (stopTime - startTime);
  }



  /**
   * Retrieves the total number of bytes read from the server.
   *
   * @return  The total number of bytes read from the server.
   */
  public long getBytesRead()
  {
    return bytesRead;
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String EOL = System.getProperty("line.separator");

    System.err.println(
"Usage:  java ThroughputTestClient [options]" + EOL +
"        where [options] include:" + EOL +
"-h [addr]  -- Specifies the address of the throughput server" + EOL +
"-p [port]  -- Specifies the port of the throughput server" + EOL +
"-b [size]  -- Specifies the maximum number of bytes to read from the server" +
     EOL +
"-d [time]  -- Specifies the maximum length of time in seconds to read" + EOL +
"              data from the server" + EOL +
"-B [size]  -- Specifies the size in bytes of the read buffer to use" + EOL +
"-H         -- Displays this usage information"
                      );
  }
}

