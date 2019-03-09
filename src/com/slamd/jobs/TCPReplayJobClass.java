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
package com.slamd.jobs;



import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.FloatParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD job that may be used to replay captured TCP
 * traffic against a target server.  The traffic to replay should be in a
 * capture file using the format used by the TCPCapture tool.
 *
 *
 * @author   Neil A. Wilson
 */
public class TCPReplayJobClass
       extends JobClass
{
  /**
   * The display name of the stat tracker used to track the number of times that
   * the replay process caught a disconnect from the remote server.
   */
  public static final String STAT_TRACKER_DISCONNECTS_CAUGHT =
       "Disconnects Caught";



  /**
   * The display name of the stat tracker used to track the number of times that
   * the entire data set has been replayed.
   */
  public static final String STAT_TRACKER_ITERATIONS_COMPLETED =
       "Iterations Completed";



  /**
   * The display name of the stat tracker used to track the number of request
   * packets that the client replayed to the server.
   */
  public static final String STAT_TRACKER_PACKETS_REPLAYED =
       "Request Packets Replayed";



  // The parameter used to indicate whether to preserve the original timing.
  private BooleanParameter preserveTimingParameter =
       new BooleanParameter("preserve_timing", "Preserve Original Timing",
                            "Indicates whether the client should attempt to " +
                            "preserve the original timing measured when the " +
                            "data was captured.", true);

  // The parameter used to specify the URL to the capture file.
  private FileURLParameter captureFileURLParameter =
       new FileURLParameter("capture_file_url", "Capture File URL",
                            "The URL to the capture file containing the data " +
                            "to replay.", null, true);

  // The parameter used to specify the multiplier for the time between requests.
  private FloatParameter multiplierParameter =
       new FloatParameter("timing_multiplier", "Timing Multiplier",
                          "The value that should be multiplied to the time " +
                          "between each request in an attempt to speed up or " +
                          "slow down the replay by the specified factor.  " +
                          "This is ignored if the client should not try to " +
                          "preserve the original timing.", true, (float) 1.0,
                          true, (float) 0.0, false, (float) 0.0);

  // The parameter used to specify the delay between iterations.
  private IntegerParameter iterationDelayParamter =
       new IntegerParameter("iteration_delay", "Delay Between Iterations (ms)",
                            "The length of time in milliseconds to sleep " +
                            "between individual iterations of the entire " +
                            "data set.", true, 0, true, 0, false, 0);

  // The parameter used to specify the maximum number of times to replay the
  // entire data set.
  private IntegerParameter maxIterationsParameter =
       new IntegerParameter("max_iterations", "Maximum Number of Iterations",
                            "The maximum number of times that each thread " +
                            "should attempt to replay the entire data set.",
                            true, -1, false, 0, false, 0);

  // The parameter used to specify the delay between packets.
  private IntegerParameter packetDelayParameter =
       new IntegerParameter("packet_delay", "Fixed Delay Between Packets (ms)",
                            "The length of time in milliseconds to sleep " +
                            "between individual packets when replaying the " +
                            "data set.  This is ignored if the client should " +
                            "try to preserve the original timing.", true, 0,
                            true, 0, false, 0);

  // The parameter used to specify the port of the target server.
  private IntegerParameter portParameter =
       new IntegerParameter("port", "Target Server Port",
                            "The port on the target server to which the " +
                            "traffic should be replayed.", true, 0, true, 1,
                            true, 65535);

  // A placeholder used for formatting.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter used to specify the address of the target server.
  private StringParameter hostParameter =
       new StringParameter("host", "Target Server Address",
                           "The address of the target server to which the " +
                           "traffic should be replayed.", true, "");



  // Static variables corresponding to the parameter values.
  private static boolean  preserveTiming;
  private static byte[][] captureData;
  private static float    timingMultiplier;
  private static int      delayBetweenIterations;
  private static int      delayBetweenPackets;
  private static int      maxIterations;
  private static int      targetPort;
  private static int[]    sleepTimes;
  private static String   targetHost;


  // Define a thread that will be used to read the data coming back from the
  // server on all the target connections.
  private static TCPReplayReadThread readThread;


  // The socket channel that will be used to communicate with the target server.
  private SocketChannel socketChannel;


  // The stat trackers that will be used to capture statistical data.
  private IncrementalTracker disconnectsCaught;
  private IncrementalTracker iterationsCompleted;
  private IncrementalTracker packetsReplayed;



  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public TCPReplayJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "TCP Replay";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Replay captured TCP data against a target server";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to replay captured TCP data against a target " +
      "server."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "Utility";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    Parameter[] params = new Parameter[]
    {
      placeholder,
      hostParameter,
      portParameter,
      captureFileURLParameter,
      placeholder,
      preserveTimingParameter,
      multiplierParameter,
      packetDelayParameter,
      placeholder,
      maxIterationsParameter,
      iterationDelayParamter
    };

    return new ParameterList(params);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[]
    {
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_PACKETS_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_ITERATIONS_COMPLETED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_DISCONNECTS_CAUGHT,
                             collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[]
    {
      packetsReplayed,
      iterationsCompleted,
      disconnectsCaught
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    // Get the values of the "simple" parameters.
    hostParameter = parameters.getStringParameter(hostParameter.getName());
    if ((hostParameter != null) && hostParameter.hasValue())
    {
      targetHost = hostParameter.getStringValue();
    }

    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if ((portParameter != null) && portParameter.hasValue())
    {
      targetPort = portParameter.getIntValue();
    }

    preserveTimingParameter =
         parameters.getBooleanParameter(preserveTimingParameter.getName());
    if (preserveTimingParameter != null)
    {
      preserveTiming = preserveTimingParameter.getBooleanValue();
    }

    multiplierParameter =
         parameters.getFloatParameter(multiplierParameter.getName());
    if ((multiplierParameter != null) && multiplierParameter.hasValue())
    {
      timingMultiplier = multiplierParameter.getFloatValue();
    }

    packetDelayParameter =
         parameters.getIntegerParameter(packetDelayParameter.getName());
    if ((packetDelayParameter != null) && packetDelayParameter.hasValue())
    {
      delayBetweenPackets = packetDelayParameter.getIntValue();
    }

    maxIterationsParameter =
        parameters.getIntegerParameter(maxIterationsParameter.getName());
    if ((maxIterationsParameter != null) && maxIterationsParameter.hasValue())
    {
      maxIterations = maxIterationsParameter.getIntValue();
    }

    iterationDelayParamter =
         parameters.getIntegerParameter(iterationDelayParamter.getName());
    if ((iterationDelayParamter != null) && iterationDelayParamter.hasValue())
    {
      delayBetweenIterations = iterationDelayParamter.getIntValue();
    }


    // Now process the capture file contents.  This logic is hard coded, so if
    // the capture format changes, then this will need to be re-written.
    ArrayList<byte[]> captureList = new ArrayList<byte[]>();
    ArrayList<Long>   timeList    = new ArrayList<Long>();
    try
    {
      captureFileURLParameter =
           parameters.getFileURLParameter(captureFileURLParameter.getName());
      InputStream inputStream = captureFileURLParameter.getInputStream();

outerLoop:
      while (true)
      {
        // The first four bytes hold the capture version, which must be 1.
        int captureVersion = 0;
        for (int i=0; i < 4; i++)
        {
          int byteRead = inputStream.read();
          if (byteRead < 0)
          {
            break outerLoop;
          }

          captureVersion = (captureVersion << 8) | (byteRead & 0xFF);
        }

        if (captureVersion != 1)
        {
          throw new UnableToRunException("Unable to parse the capture file:  " +
                                         "The capture version must be 1, but " +
                                         "a value of " + captureVersion +
                                         " was read.");
        }


        // The next eight bytes hold the timestamp.
        long captureTime = 0;
        for (int i=0; i < 8; i++)
        {
          captureTime= (captureTime << 8) | (inputStream.read() & 0xFF);
        }


        // The next four bytes hold the number of bytes in the capture.
        int captureLength = 0;
        for (int i=0; i < 4; i++)
        {
          captureLength = (captureLength << 8) | (inputStream.read() & 0xFF);
        }

        if ((captureLength < 0) || (captureLength > (1024*1024)))
        {
          throw new UnableToRunException("Unable to parse the capture file:  " +
                                         "The capture length must not be " +
                                         "greater than one megabyte (decoded " +
                                         "a length of " + captureLength + ").");
        }


        // Read the specified number of bytes.
        byte[] data = new byte[captureLength];
        int totalBytesRead = 0;
        while (totalBytesRead < captureLength)
        {
          int bytesRead = inputStream.read(data, totalBytesRead,
                                           (captureLength - totalBytesRead));
          if (bytesRead < 0)
          {
            throw new UnableToRunException("Unable to parse the capture " +
                                           "file:  The input stream was " +
                                           "unexpectedly closed before all " +
                                           "data could be read.");
          }

          totalBytesRead += bytesRead;
        }

        captureList.add(data);
        timeList.add(captureTime);
      }

      long lastTime = -1;
      captureData   = new byte[captureList.size()][];
      sleepTimes    = new int[captureData.length];
      for (int i=0; i < captureData.length; i++)
      {
        captureData[i] = captureList.get(i);

        if (i == 0)
        {
          sleepTimes[i] = 0;
          lastTime = timeList.get(i);
        }
        else if (preserveTiming)
        {
          long captureTime = timeList.get(i);
          sleepTimes[i] = (int) ((captureTime - lastTime) * timingMultiplier);
          lastTime = captureTime;
        }
        else
        {
          sleepTimes[i] = delayBetweenPackets;
        }
      }
    }
    catch (UnableToRunException utre)
    {
      throw utre;
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to parse the capture file:  " +
                                     stackTraceToString(e));
    }


    try
    {
      readThread = new TCPReplayReadThread(this);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Could not create the read thread:  " +
                                     stackTraceToString(e));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    packetsReplayed = new IncrementalTracker(clientID, threadID,
                                             STAT_TRACKER_PACKETS_REPLAYED,
                                             collectionInterval);
    iterationsCompleted =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_ITERATIONS_COMPLETED,
                                collectionInterval);
    disconnectsCaught = new IncrementalTracker(clientID, threadID,
                                               STAT_TRACKER_DISCONNECTS_CAUGHT,
                                               collectionInterval);

    socketChannel = null;
    readThread.registerJobThread();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    try
    {
      packetsReplayed.startTracker();
      iterationsCompleted.startTracker();
      disconnectsCaught.startTracker();

      boolean           connected     = false;
      boolean           infinite      = (maxIterations <= 0);
      InetSocketAddress socketAddress = new InetSocketAddress(targetHost,
                                                              targetPort);

outerLoop:
      for (int i=0; (infinite || (i < maxIterations)); i++)
      {
        if (shouldStop())
        {
          break;
        }

        for (int j=0; j < captureData.length; j++)
        {
          if (shouldStop())
          {
            break outerLoop;
          }

          if (! connected)
          {
            try
            {
              socketChannel = SocketChannel.open(socketAddress);
              socketChannel.configureBlocking(false);
              socketChannel.finishConnect();
              readThread.registerSocketChannel(socketChannel);
              connected = true;
            }
            catch (IOException ioe)
            {
              logMessage("ERROR:  Unable to connect to " + socketAddress +
                         " -- " + stackTraceToString(ioe));
              break outerLoop;
            }
          }

          if (sleepTimes[j] > 0)
          {
            Thread.sleep(sleepTimes[j]);
          }

          try
          {
            ByteBuffer buffer = ByteBuffer.wrap(captureData[j]);
            int bytesWritten = socketChannel.write(buffer);
            while (bytesWritten < captureData[j].length)
            {
              bytesWritten += socketChannel.write(buffer);
            }

            packetsReplayed.increment();
          }
          catch (IOException ioe)
          {
            try
            {
              socketChannel.close();
            } catch (Exception e) {}

            connected = false;
            disconnectsCaught.increment();
          }
        }

        if (! shouldStop())
        {
          iterationsCompleted.increment();
        }
      }
    }
    catch (Exception e)
    {
      logMessage("ERROR:  Uncaught exception during processing -- " +
                 stackTraceToString(e));
    }
    finally
    {
      readThread.threadDone();

      packetsReplayed.stopTracker();
      iterationsCompleted.stopTracker();
      disconnectsCaught.stopTracker();

    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    try
    {
      socketChannel.close();
    } catch (Exception e) {}
  }
}

