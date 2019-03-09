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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.LongValueTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD job that may be used in conjunction with the
 * throughput test server to measure the network throughput between two or more
 * systems (i.e., one server and one or more clients).
 *
 *
 * @author   Neil A. Wilson
 */
public class ThroughputTestJobClass
       extends JobClass
{
  /**
   * The display name of the stat tracker used to track the number of bits read
   * per second.
   */
  public static final String STAT_TRACKER_BITS_PER_SECOND =
       "Average Bits Read per Second";



  /**
   * The display name of the stat tracker used to track the number of bytes read
   * per second.
   */
  public static final String STAT_TRACKER_BYTES_PER_SECOND =
       "Average Bytes Read per Second";



  /**
   * The display name of the stat tracker used to track the number of kilobits
   * read per second.
   */
  public static final String STAT_TRACKER_KILOBITS_PER_SECOND =
       "Average Kilobits Read per Second";



  /**
   * The display name of the stat tracker used to track the number of kilobytes
   * read per second.
   */
  public static final String STAT_TRACKER_KILOBYTES_PER_SECOND =
       "Average Kilobytes Read per Second";



  /**
   * The display name of the stat tracker used to track the number of megabits
   * read per second.
   */
  public static final String STAT_TRACKER_MEGABITS_PER_SECOND =
       "Average Megabits Read per Second";



  /**
   * The display name of the stat tracker used to track the number of megabytes
   * read per second.
   */
  public static final String STAT_TRACKER_MEGABYTES_PER_SECOND =
       "Average Megabytes Read per Second";



  // The buffer size to use in bytes.
  private IntegerParameter bufferSizeParameter =
       new IntegerParameter("buffer_size", "Read Buffer Size",
                            "The buffer size to use when reading data from " +
                            "the server.", true, 8192, true, 1, false, 0);

  // The parameter specifying the amount of data to read.
  private IntegerParameter maxMegabytesParameter =
       new IntegerParameter("max_megabytes", "Megabytes to Transfer",
                            "The number of megabytes that each client thread " +
                            "should read from the server before " +
                            "disconnecting.  A negative value indicates " +
                            "that there should be no limit.", false, -1);

  // The parameter specifying the port of the throughput test server.
  private IntegerParameter portParameter =
       new IntegerParameter("server_port", "Server Port",
                            "The port on which the throughput test server is " +
                            "listening.", true, 3333, true, 1, true, 65535);

  // The parameter specifying the address of the throughput test server.
  private StringParameter addressParameter =
       new StringParameter("server_address", "Server Address",
                           "The address of the system running the throughput " +
                           "test server.", true, "");



  // Static variables corresponding to the parameter values.
  private static int    bufferSize;
  private static int    serverPort;
  private static long   maxBytes;
  private static String serverAddress;


  // The buffer that will be used to read data from the server.
  private ByteBuffer readBuffer;


  // The total number of bytes read so far by this thread.
  private long totalBytesRead;


  // The stat tracker used to actually measure the throughput.
  private LongValueTracker bytesRead;



  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public ThroughputTestJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Throughput Test";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Measure network throughput between client and server systems";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to measure network throughput between a given " +
      "server system and one or more client systems.  The server system must " +
      "be running an instance of the throughput test server tool."
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
      addressParameter,
      portParameter,
      bufferSizeParameter,
      maxMegabytesParameter
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
      new LongValueTracker(clientID, threadID,
                           STAT_TRACKER_MEGABYTES_PER_SECOND,
                           collectionInterval),
      new LongValueTracker(clientID, threadID,
                           STAT_TRACKER_MEGABITS_PER_SECOND,
                           collectionInterval),
      new LongValueTracker(clientID, threadID,
                           STAT_TRACKER_KILOBYTES_PER_SECOND,
                           collectionInterval),
      new LongValueTracker(clientID, threadID,
                           STAT_TRACKER_KILOBITS_PER_SECOND,
                           collectionInterval),
      new LongValueTracker(clientID, threadID, STAT_TRACKER_BYTES_PER_SECOND,
                           collectionInterval),
      new LongValueTracker(clientID, threadID, STAT_TRACKER_BITS_PER_SECOND,
                           collectionInterval),
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    LongValueTracker bitsRead =
         bytesRead.multiplyValues(STAT_TRACKER_BITS_PER_SECOND, 8);
    LongValueTracker kilobytesRead =
         bytesRead.divideValues(STAT_TRACKER_KILOBYTES_PER_SECOND, 1024);
    LongValueTracker kilobitsRead =
        bitsRead.divideValues(STAT_TRACKER_KILOBITS_PER_SECOND, 1024);
    LongValueTracker megabytesRead =
         bytesRead.divideValues(STAT_TRACKER_MEGABYTES_PER_SECOND, (1024*1024));
    LongValueTracker megabitsRead =
        bitsRead.divideValues(STAT_TRACKER_MEGABITS_PER_SECOND, (1024*1024));

    return new StatTracker[]
    {
      megabytesRead,
      megabitsRead,
      kilobytesRead,
      kilobitsRead,
      bytesRead,
      bitsRead
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    addressParameter =
         parameters.getStringParameter(addressParameter.getName());
    if (addressParameter != null)
    {
      serverAddress = addressParameter.getStringValue();
    }

    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      serverPort = portParameter.getIntValue();
    }

    maxBytes = -1;
    maxMegabytesParameter =
         parameters.getIntegerParameter(maxMegabytesParameter.getName());
    if ((maxMegabytesParameter != null) && maxMegabytesParameter.hasValue())
    {
      maxBytes = 1024 * 1024 * maxMegabytesParameter.getIntValue();
    }

    bufferSize = 8192;
    bufferSizeParameter =
         parameters.getIntegerParameter(bufferSizeParameter.getName());
    if ((bufferSizeParameter != null) && bufferSizeParameter.hasValue())
    {
      bufferSize = bufferSizeParameter.getIntValue();
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
    // Create the stat tracker that we will actually use.
    bytesRead = new LongValueTracker(clientID, threadID,
                                     STAT_TRACKER_BYTES_PER_SECOND,
                                     collectionInterval);


    // Initialize the remaining variables we will use for this thread.
    totalBytesRead = 0;
    readBuffer     = ByteBuffer.allocateDirect(bufferSize);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Establish the connection to the throughput test server.
    SocketChannel socketChannel;
    try
    {
      socketChannel =
           SocketChannel.open(new InetSocketAddress(serverAddress, serverPort));
    }
    catch (IOException ioe)
    {
      logMessage("Unable to connect to throughput test server " +
                 serverAddress + ':' + serverPort + " -- " + ioe);
      indicateStoppedDueToError();
      return;
    }

    bytesRead.startTracker();


    // Loop until it is determined that the job should stop.
    try
    {
      while (! shouldStop())
      {
        long numBytesRead = socketChannel.read(readBuffer);
        readBuffer.clear();

        if (numBytesRead < 0)
        {
          logMessage("Unexpected end of input stream from server");
          indicateStoppedDueToError();
          break;
        }

        bytesRead.addValue(numBytesRead);
        totalBytesRead += numBytesRead;
        if ((maxBytes > 0) && (totalBytesRead > maxBytes))
        {
          break;
        }
      }
    }
    catch (Exception e)
    {
      logMessage("Caught an exception while reading data from the server -- " +
                 stackTraceToString(e));
      indicateStoppedDueToError();
    }

    bytesRead.stopTracker();


    // Close the connection to the server.
    try
    {
      socketChannel.close();
    } catch (Exception e) {}
  }
}

