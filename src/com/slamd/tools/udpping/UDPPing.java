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



import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;



/**
 * This program defines a client that uses UDP packets to determine information
 * about the network link between two systems.  It functions in a manner that is
 * very similar to the ICMP-based ping utility, but using UDP as the transport
 * mechanism.  It should be used in conjunction with either the standard UDP
 * echo service available on most systems, or with the provided UDP Ping Server
 * which also provides this functionality.
 *
 *
 * @author   Neil A. Wilson
 */
public class UDPPing
       extends Thread
{
  /**
   * The default port that will be used if none is specified.
   */
  public static final int DEFAULT_PORT = 7;



  /**
   * The default count that will be used if none is specified.
   */
  public static final int DEFAULT_COUNT = -1;



  /**
   * The default interval that will be used if none is specified.
   */
  public static final int DEFAULT_INTERVAL = 1000;



  /**
   * The default timeout that will be used if none is specified.
   */
  public static final int DEFAULT_TIMEOUT = 1000;



  /**
   * The default datagram size that will be used if none is specified.
   */
  public static final int DEFAULT_DATAGRAM_SIZE = 64;



  /**
   * The minimum size in bytes that will be allowed for UDP ping packets to
   * accommodate the information that needs to be stored in them.
   */
  public static final int MIN_DATAGRAM_SIZE = 12;



  // Indicates whether the shutdown hook has been fired to request that the ping
  // stop running.
  private boolean shutdownRequested;

  // Indicates whether to use a shutdown hook to detect when the user wants to
  // stop the ping (e.g., using a Ctrl+C).
  private boolean useShutdownHook;

  // The decimal formatter used to display numeric values.
  private DecimalFormat decimalFormat;

  // The maximum length of time in milliseconds required to get a response.
  private double maxDuration;

  // The minimum length of time in milliseconds required to get a response.
  private double minDuration;

  // The sum of all the response times seen so far.
  private double sumOfResponseTimes;

  // The sum of the squares of all the response times seen so far.
  private double sumOfSquaresOfResponseTimes;

  // The number of packets to send to the target system.
  private int count;

  // The size of the packet to send, in bytes.
  private int datagramSize;

  // The length of time in milliseconds between the "ping" packets.
  private int interval;

  // The UDP port to use when communicating with the target system.
  private int targetPort;

  // The maximum length of time in milliseconds to wait for a response from the
  // target system before considering the packet lost.
  private int timeout;

  // The number of duplicate response packets received (or responses received
  // after the timeout occurred).
  private int numDuplicates;

  // The number of ping requests sent for which no response was received in the
  // appropriate timeout period).
  private int numLost;

  // The total number of ping requests sent.
  private int numRequests;

  // The number of ping responses received in the appropriate time frame.
  private int numResponses;

  // The print stream to which standard output messages should be sent.
  private PrintStream errorStream;

  // The print stream to which standard error messages should be sent.
  private PrintStream outputStream;

  // The address of the target system to "ping".
  private String targetAddress;

  // The IP address of the target system to "ping".
  private String targetIP;

  // The thread that is used to actually perform the pings.
  private Thread pingClientThread;

  // The ping client with which this shutdown hook is associated.
  private UDPPing pingClient;



  /**
   * Parses the command-line arguments and starts the UDP ping server as
   * appropriate.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // First, set default values for all arguments.
    int    count         = -1;
    int    datagramSize  = 64;
    int    interval      = 1000;
    int    targetPort    = 7;
    int    timeout       = 1000;
    String targetAddress = null;


    // Process any command-line parameters that may have been provided.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        targetAddress = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        targetPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-c"))
      {
        count = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-s"))
      {
        datagramSize = Integer.parseInt(args[++i]);
        if (datagramSize < MIN_DATAGRAM_SIZE)
        {
          System.err.println("ERROR:  Datagram size may not be less than " +
                             MIN_DATAGRAM_SIZE);
          System.exit(1);
        }
      }
      else if (args[i].equals("-i"))
      {
        interval = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-t"))
      {
        timeout = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-R"))
      {
        // This is no longer needed, since we will always use the
        // high-resolution timer.
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


    // Make sure that the address was specified.
    if (targetAddress == null)
    {
      System.err.println("ERROR:  No target address specified (use -h)");
      displayUsage();
      System.exit(1);
    }


    // Invoke the constructor with all the appropriate options.
    UDPPing pingClient = new UDPPing(targetAddress, targetPort, count, interval,
                                     datagramSize, timeout, System.out,
                                     System.err, true);
    pingClient.doPing();
  }



  /**
   * Creates a new instance of this UDP ping client in a mode that is to be used
   * only as the shutdown hook thread.  This should not be used for any other
   * purpose.
   *
   * @param  pingClient  The ping client that should be notified when the
   *                     shutdown hook is fired.
   */
  private UDPPing(UDPPing pingClient)
  {
    this.pingClient = pingClient;
  }



  /**
   * Creates a new instance of this ping client with the provided information.
   *
   * @param  targetAddress    The address of the system to which the ping
   *                          datagrams should be sent.
   * @param  targetPort       The UDP port to which the ping datagrams should be
   *                          sent.
   * @param  count            The maximum number of ping datagrams to send.  A
   *                          negative value indicates no limit.
   * @param  interval         The interval in milliseconds to use when sending
   *                          the ping datagrams.
   * @param  datagramSize     The number of bytes to include in the ping
   *                          datagrams.
   * @param  timeout          The maximum length of time in milliseconds to wait
   *                          for a response before considering it lost.
   * @param  outputStream     The print stream to which standard output should
   *                          be sent.
   * @param  errorStream      The print stream to which standard error should be
   *                          sent.
   * @param  useShutdownHook  Indicates whether a shutdown hook should be
   *                          installed to detect if the user cancels the ping.
   */
  public UDPPing(String targetAddress, int targetPort, int count, int interval,
                 int datagramSize, int timeout, PrintStream outputStream,
                 PrintStream errorStream, boolean useShutdownHook)
  {
    this.targetAddress   = targetAddress;
    this.targetPort      = targetPort;
    this.count           = count;
    this.interval        = interval;
    this.datagramSize    = datagramSize;
    this.timeout         = timeout;
    this.outputStream    = outputStream;
    this.errorStream     = errorStream;
    this.useShutdownHook = useShutdownHook;
    decimalFormat = new DecimalFormat("0.000");

    pingClientThread  = null;
    shutdownRequested = false;
    if (useShutdownHook)
    {
      Runtime.getRuntime().addShutdownHook(new UDPPing(this));
    }
  }



  /**
   * Handles the work of actually sending the specified number of ping packets
   * to the remote system.
   */
  public void doPing()
  {
    // Initialize all the variables used for maintaining statistics.
    sumOfResponseTimes          = 0.0;
    sumOfSquaresOfResponseTimes = 0.0;
    maxDuration                 = Double.MIN_VALUE;
    minDuration                 = Double.MAX_VALUE;
    numDuplicates               = 0;
    numLost                     = 0;
    numRequests                 = 0;
    numResponses                = 0;


    // Establish a "connection" to the remote system.
    DatagramSocket pingSocket;
    try
    {
      targetIP   = InetAddress.getByName(targetAddress).getHostAddress();
      pingSocket = new DatagramSocket();
      pingSocket.connect(new InetSocketAddress(targetIP, targetPort));
      pingSocket.setSoTimeout(timeout);
    }
    catch (Exception e)
    {
      errorStream.println("Unable to initialize the datagram socket:  " + e);
      return;
    }


    // Construct the datagram packets for sending and receiving data.
    byte[]         rxBytes  = new byte[datagramSize];
    byte[]         txBytes  = new byte[datagramSize];
    DatagramPacket rxPacket = new DatagramPacket(rxBytes, datagramSize);
    DatagramPacket txPacket = new DatagramPacket(txBytes, datagramSize);


    // Actually perform the pings.  Set up to run for the specified number of
    // iterations.
    boolean infinite = (count <= 0);
hiResLoop:
    for (int i=0; ((! shutdownRequested) && (infinite || (i < count))); i++)
    {
      long startTimeMillis = System.currentTimeMillis();
      long startTimeNanos;
      try
      {
        startTimeNanos = System.nanoTime();
      }
      catch (Exception e)
      {
        errorStream.println("Unable to read the high-resolution timer to " +
                            "determine request time:  " + e);
        break;
      }


      // Construct the packet to send to the remote system.  Put the sequence
      // number in the first four bytes and the timestamp in the next eight.
      txBytes[0]  = (byte) ((i >>> 24) & 0x000000FF);
      txBytes[1]  = (byte) ((i >>> 16) & 0x000000FF);
      txBytes[2]  = (byte) ((i >>> 8)  & 0x000000FF);
      txBytes[3]  = (byte) (i & 0x000000FF);
      txBytes[4]  = (byte) ((startTimeNanos >>> 56) & 0x000000FF);
      txBytes[5]  = (byte) ((startTimeNanos >>> 48) & 0x000000FF);
      txBytes[6]  = (byte) ((startTimeNanos >>> 40) & 0x000000FF);
      txBytes[7]  = (byte) ((startTimeNanos >>> 32) & 0x000000FF);
      txBytes[8]  = (byte) ((startTimeNanos >>> 24) & 0x000000FF);
      txBytes[9]  = (byte) ((startTimeNanos >>> 16) & 0x000000FF);
      txBytes[10] = (byte) ((startTimeNanos >>> 8)  & 0x000000FF);
      txBytes[11] = (byte) (startTimeNanos & 0x000000FF);


      // Actually send the datagram packet.
      try
      {
        txPacket.setData(txBytes);
        pingSocket.send(txPacket);
        numRequests++;
      }
      catch (Exception e)
      {
        errorStream.println("ERROR:  Unable to send request " + i + " -- " +
                            e);
        break;
      }


      // Wait for and parse the response.
      try
      {
        while (true)
        {
          pingSocket.receive(rxPacket);
          rxBytes = rxPacket.getData();
          int sequence = (((rxBytes[0] & 0x000000FF) << 24) |
                          ((rxBytes[1] & 0x000000FF) << 16) |
                          ((rxBytes[2] & 0x000000FF) << 8)  |
                           (rxBytes[3] & 0x000000FF));
          if (sequence == i)
          {
            long currentTimeNanos;
            try
            {
              currentTimeNanos = System.nanoTime();
            }
            catch (Exception e)
            {
              errorStream.println("Unable to read the high-resolution " +
                                  "timer to determine response time:  " + e);
              break hiResLoop;
            }


            double elapsedTime = 0.000001 * (currentTimeNanos-startTimeNanos);
            sumOfResponseTimes += elapsedTime;
            sumOfSquaresOfResponseTimes += (elapsedTime * elapsedTime);
            numResponses++;

            if (elapsedTime < minDuration)
            {
              minDuration = elapsedTime;
            }
            if (elapsedTime > maxDuration)
            {
              maxDuration = elapsedTime;
            }

            outputStream.println(rxPacket.getLength() + " bytes from " +
                                 targetIP + ": seq=" + sequence + " time=" +
                                 decimalFormat.format(elapsedTime) + " ms");
            break;
          }
          else if (sequence < i)
          {
            long currentTimeNanos;
            try
            {
              currentTimeNanos = System.nanoTime();
            }
            catch (Exception e)
            {
              errorStream.println("Unable to read the high-resolution " +
                                  "timer to determine response time:  " + e);
              break hiResLoop;
            }

            long requestTimeNanos = 0;
            for (int j=4; j < 12; j++)
            {
              requestTimeNanos <<= 8;
              requestTimeNanos |= (rxBytes[j] & 0x000000FF);
            }

            double elapsedTime = 0.000001 *
                                 (currentTimeNanos-requestTimeNanos);
            errorStream.println(rxPacket.getLength() + " bytes from " +
                                targetIP + ": seq=" + sequence + " time=" +
                                decimalFormat.format(elapsedTime) +
                                " ms (DUP!)");
            numDuplicates++;
          }
          else
          {
            errorStream.println(rxPacket.getLength() + " bytes from " +
                                targetIP + ": seq=" + sequence +
                                " (INVALID SEQUENCE!)");
            break hiResLoop;
          }
        }
      }
      catch (SocketTimeoutException ste)
      {
        errorStream.println("PING " + targetIP + " seq=" + i + " Timed Out");
        numLost++;
      }
      catch (Exception e)
      {
        errorStream.println("PING " + targetIP + " seq=" + i +
                            " Exception -- " + e);
        break;
      }


      // Sleep for the appropriate amount of time before the next request.
      long sleepTime = (startTimeMillis + interval) -
                       System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        } catch (Exception e) {}
      }
    }


    // Close the "connection".
    try
    {
      pingSocket.disconnect();
    } catch (Exception e) {}
  }



  /**
   * Displays statistics to the end user in a ping-like form.
   */
  public void outputStatistics()
  {
    if (numRequests == 0)
    {
      return;
    }

    if ((minDuration == Double.MAX_VALUE) || (maxDuration == Double.MIN_VALUE))
    {
      minDuration = 0.0;
      maxDuration = 0.0;
    }

    double lossPercent        = 100.0 * numLost / numRequests;
    double avgResponseTime    = (sumOfResponseTimes / numResponses);
    double avgSqrResponseTime = (sumOfSquaresOfResponseTimes / numResponses);
    double meanDeviation      = Math.sqrt(avgSqrResponseTime -
                                          (avgResponseTime*avgResponseTime));

    outputStream.println();
    outputStream.println("--- " + targetAddress + " UDP ping statistics ---");
    outputStream.println(numRequests + " packets transmitted, " + numResponses +
                         " received, " + decimalFormat.format(lossPercent) +
                         "% packet loss");
    outputStream.println("rdd min/avg/max/mdev = " +
                         decimalFormat.format(minDuration) + '/' +
                         decimalFormat.format(avgResponseTime) + '/' +
                         decimalFormat.format(maxDuration) + '/' +
                         decimalFormat.format(meanDeviation));
  }



  /**
   * Retrieves the average response time for the response packets received.
   *
   * @return  The average response time for the response packets received.
   */
  public double getAverageResponseTime()
  {
    return (sumOfResponseTimes / numResponses);
  }



  /**
   * Retrieves the minimum response time for the response packets received.
   *
   * @return  The minimum response time for the response packets received.
   */
  public double getMinResponseTime()
  {
    return minDuration;
  }



  /**
   * Retrieves the maximum response time for the response packets received.
   *
   * @return  The maximum response time for the response packets received.
   */
  public double getMaxDuration()
  {
    return maxDuration;
  }



  /**
   * Retrieves the mean deviation for the response times of the response packets
   * received.
   *
   * @return  The mean deviation for the response times of the response packets
   *          received.
   */
  public double getResponseTimeMeanDeviation()
  {
    double avgResponseTime    = (sumOfResponseTimes / numResponses);
    double avgSqrResponseTime = (sumOfSquaresOfResponseTimes / numResponses);
    return Math.sqrt(avgSqrResponseTime - (avgResponseTime*avgResponseTime));
  }



  /**
   * Retrieves the total number of requests that were sent.
   *
   * @return  The total number of requests that were sent.
   */
  public int getNumRequests()
  {
    return numRequests;
  }



  /**
   * Retrieves the total number of responses that were received before a timeout
   * occurred.
   *
   * @return  The total number of requests that were received before a timeout
   *          occurred.
   */
  public int getNumResponses()
  {
    return numRequests;
  }



  /**
   * Retrieves the number of duplicate packets received, which may also include
   * valid responses that were received after the original request timed out.
   *
   * @return  The number of duplicate packets received.
   */
  public int getNumDuplicates()
  {
    return numDuplicates;
  }



  /**
   * Retrieves the number of requests sent for which there was no response
   * received before the timeout occurred.
   *
   * @return  The number of requests sent for which there was no response
   *          received before the timeout occurred.
   */
  public int getNumLost()
  {
    return numLost;
  }



  /**
   * Retrieves the percentage of all requests packets for which there was no
   * response received before the timeout occurred.
   *
   * @return  The percentage of all requests packets for which there was no
   *          response received before the timeout occurred.
   */
  public double getLossPercent()
  {
    return 100.0 * numLost / numRequests;
  }



  /**
   * Runs the shutdown hook to tell the ping client that the user has
   * interrupted the ping process.
   */
  public void run()
  {
    if (pingClient != null)
    {
      pingClient.shutdownRequested = true;
      pingClient.outputStatistics();
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String eol = System.getProperty("line.separator");

    System.err.println(
"USAGE:  java UDPPing [options]" + eol +
"        where [options] may include:" + eol +
"-h {host}      -- The address of the system to ping (required)" + eol +
"-p {port}      -- The port of the system to ping (default:  7)" + eol +
"-c {count}     -- The number of pings to send (default:  infinite)" + eol +
"-i {interval}  -- The interval in ms between pings (default:  1000)" + eol +
"-s {size}      -- The datagram size in bytes to use (default:  64)" + eol +
"-t {timeout}   -- The timeout in ms to use (default:  1000)" + eol +
"-R             -- Use the high-resolution timer (requires Java 1.5)" + eol +
"-H             -- Displays this usage information"
                      );
  }
}

