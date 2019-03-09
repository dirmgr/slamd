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



import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;



/**
 * This class defines a utility that may be used to replay capture data against
 * the same or a different server using one or more threads.  It can perform
 * this replay either as quickly as possible, or it can attempt to preserve the
 * original timing (or play back requests at some fraction thereof).
 *
 *
 * @author   Neil A. Wilson
 */
public class ReplayCapture
{
  // The list of socket channels that are pending registration with the
  // selector.
  private ArrayList<SocketChannel> pendingRegisterList =
       new ArrayList<SocketChannel>();

  // Indicates whether the replay client should attempt to preserve the original
  // timing associated with the requests.
  private boolean preserveTiming;

  // Indicates whether a request has been received to stop the replay.
  protected boolean stopRequested;

  // The data that is to be replayed against the target server.
  protected CaptureData[] captureData;

  // The multiplier that should be applied to the time between requests when
  // attempting to preserve the original timing.
  private float timeMultiplier;

  // The number of threads that are currently active.
  private int activeThreads;

  // The delay in milliseconds that should be used between iterations of the
  // capture data set.
  private int delayBetweenIterations;

  // The delay in milliseconds that should be used between individual request
  // captures if the original timing is not going to be preserved.
  private int delayBetweenRequests;

  // The maximum length of time in seconds to spend replaying data.
  private int maxDuration;

  // The maximum number of times that the entire capture set should be replayed.
  protected int numIterations;

  // The number of concurrent threads that should be used to perform the replay.
  private int numThreads;

  // The port of the target server against which the requests are to be
  // replayed.
  protected int targetPort;

  // The length of time in milliseconds to sleep between each capture data
  // packet if we are attempting to preserve the original timing.
  protected int[] sleepTimes;

  // The number of times the connection was unexpectedly closed during
  // processing.
  private long totalDisconnects;

  // The number of iterations through the entire data set that were completed.
  private long totalIterationsCompleted;

  // The number of individual capture packets that were replayed.
  private long totalPacketsReplayed;

  // The threads that will be used to actually perform the replay process.
  private ReplayThread[] replayThreads;

  // The selector that will be used to read data from the backend server
  // whenever it arrives.
  private Selector selector;

  // The address of the target server against which the requests are to be
  // replayed.
  protected String targetHost;



  /**
   * Creates a new instance of this replay utility with the provided
   * information.
   *
   * @param  targetHost              The address of the target server against
   *                                 which to replay the the capture data.
   * @param  targetPort              The port of the target server against which
   *                                 to replay the capture data.
   * @param  captureFile             The path to the file containing the capture
   *                                 data to replay.
   * @param  preserveTiming          Indicates whether an attempt should be made
   *                                 to replay packets based on the original
   *                                 times between each request packet.
   * @param  timeMultiplier          A multiplier that will be applied to the
   *                                 time between individual packets if the
   *                                 original timing is to be preserved.  A
   *                                 value of 1.0 will preserve the original
   *                                 timing, while 0.5 will sleep half as long
   *                                 (and therefore try to go twice as fast),
   *                                 and 2.0 will sleep twice as long (and try
   *                                 to go twice as slow).
   * @param  delayBetweenRequests    The length of time in milliseconds to sleep
   *                                 between each capture packet replayed if the
   *                                 original timing is not to be preserved.
   * @param  numIterations           The maximum number of times the entire
   *                                 capture data set should be replayed (-1 for
   *                                 no limit).
   * @param  delayBetweenIterations  The length of time in milliseconds to sleep
   *                                 between iterations through the entire
   *                                 capture data set.
   * @param  maxDuration             The maximum length of time in seconds to
   *                                 spend replaying the capture data (-1 for no
   *                                 limit).
   * @param  numThreads              The number of threads to concurrently
   *                                 replay the captured data against the target
   *                                 server.
   *
   * @throws  IOException  If a problem occurs while attempting to read the
   *                       capture file containing the data to replay.
   *
   * @throws  CaptureException  If a problem occurs while trying to parse the
   *                            capture file.
   */
  public ReplayCapture(String targetHost, int targetPort, String captureFile,
                       boolean preserveTiming, float timeMultiplier,
                       int delayBetweenRequests, int numIterations,
                       int delayBetweenIterations, int maxDuration,
                       int numThreads)
         throws IOException, CaptureException
  {
    // Set the values of all the instance variables based on the arguments.
    this.targetHost             = targetHost;
    this.targetPort             = targetPort;
    this.preserveTiming         = preserveTiming;
    this.timeMultiplier         = timeMultiplier;
    this.delayBetweenRequests   = delayBetweenRequests;
    this.numIterations          = numIterations;
    this.delayBetweenIterations = delayBetweenIterations;
    this.maxDuration            = maxDuration;
    this.numThreads             = numThreads;

    stopRequested            = false;
    activeThreads            = 0;
    totalDisconnects         = 0;
    totalIterationsCompleted = 0;
    totalPacketsReplayed     = 0;


    // Parse the capture file and read the data into memory.
    ArrayList<CaptureData> captureList = new ArrayList<CaptureData>();
    FileInputStream inputStream = new FileInputStream(captureFile);
    while (true)
    {
      CaptureData capture = CaptureData.decodeFrom(inputStream);
      if (capture == null)
      {
        break;
      }
      else
      {
        captureList.add(capture);
      }
    }

    inputStream.close();

    captureData = new CaptureData[captureList.size()];
    sleepTimes  = new int[captureData.length];
    for (int i=0; i < captureData.length; i++)
    {
      captureData[i] = captureList.get(i);
      if (i == 0)
      {
        sleepTimes[i] = 0;
      }
      else if (preserveTiming)
      {
        sleepTimes[i] = (int) ((captureData[i].getCaptureTime() -
                                captureData[i-1].getCaptureTime()) *
                               timeMultiplier);
      }
      else
      {
        sleepTimes[i] = Math.max(0, delayBetweenRequests);
      }
    }


    // Create the appropriate number of replay threads, but don't start them
    // yet.
    replayThreads = new ReplayThread[numThreads];
    for (int i=0; i < numThreads; i++)
    {
      replayThreads[i] = new ReplayThread(this);
    }


    // Create the selector that will be used to read data any responses that
    // might arrive from the target server.
    pendingRegisterList = new ArrayList<SocketChannel>();
    selector = Selector.open();


    // Register a shutdown hook so that the replay will be stopped gracefully
    // when the JVM is shutting down.
    Runtime.getRuntime().addShutdownHook(new ReplayShutdownThread(this));
  }



  /**
   * Starts all the individual replay threads, then will loop, waiting for new
   * data to be available for reading on any of the connections established to
   * the target server.
   */
  public void replayData()
  {
    activeThreads = numThreads;
    for (int i=0; i < numThreads; i++)
    {
      replayThreads[i].start();
    }

    long stopTime;
    if (maxDuration > 0)
    {
      stopTime = System.currentTimeMillis() + (1000 * maxDuration);
    }
    else
    {
      stopTime = Long.MAX_VALUE;
    }

    boolean    consecutiveFailures = false;
    ByteBuffer buffer              = ByteBuffer.allocate(4096);
    while ((! stopRequested) && (System.currentTimeMillis() < stopTime))
    {
      try
      {
        // Perform a select to see if there is any data available for reading.
        int numKeys = selector.select(100);
        if (numKeys > 0)
        {
          Iterator iterator = selector.selectedKeys().iterator();
          while (iterator.hasNext())
          {
            SelectionKey selectionKey = (SelectionKey) iterator.next();

            if (selectionKey.isReadable())
            {
              SocketChannel channel = (SocketChannel) selectionKey.channel();
              if (channel.isConnected())
              {
                int bytesRead = channel.read(buffer);
                buffer.clear();

                while (bytesRead > 0)
                {
                  bytesRead = channel.read(buffer);
                  buffer.clear();
                }

                if (bytesRead < 0)
                {
                  selectionKey.cancel();
                }
              }
              else
              {
                selectionKey.cancel();
              }
            }

            iterator.remove();
          }
        }


        // See if there are any new connections that need to be registered.
        synchronized (pendingRegisterList)
        {
          if (! pendingRegisterList.isEmpty())
          {
            Iterator<SocketChannel> iterator = pendingRegisterList.iterator();
            while (iterator.hasNext())
            {
              SocketChannel socketChannel = iterator.next();
              socketChannel.register(selector, SelectionKey.OP_READ);
            }

            pendingRegisterList.clear();
          }
        }


        // Reset the consecutive failures flag since this iteration was
        // successful.
        consecutiveFailures = false;
      }
      catch (Exception e)
      {
        System.err.println("Caught exception replay capture:");
        e.printStackTrace();

        if (consecutiveFailures)
        {
          // There have been multiple consecutive failures in the selector, so
          // exit gracefully.
          stopRequested = true;
          break;
        }
        else
        {
          consecutiveFailures = true;
          continue;
        }
      }
    }

    // If we have gotten here, then either a request has been received to stop
    // running or we have run for the maximum duration.  Signal all the other
    // threads to stop running.
    stopRequested = true;


    // Close all the connections that may still be associated with the selector.
    synchronized (pendingRegisterList)
    {
      Iterator<SelectionKey> iterator = selector.keys().iterator();
      while (iterator.hasNext())
      {
        try
        {
          SelectionKey selectionKey = iterator.next();
          selectionKey.channel().close();
        } catch (Exception e) {}
      }
    }
  }



  /**
   * Registers a socket channel with this selector so that any data available
   * for reading on the channel will be consumed.
   *
   * @param  socketChannel  The socket channel that is to be registered with the
   *                        selector.
   */
  public void registerSocketChannel(SocketChannel socketChannel)
  {
    synchronized (pendingRegisterList)
    {
      pendingRegisterList.add(socketChannel);
    }

    selector.wakeup();
  }



  /**
   * Indicates that the specified thread has completed all the processing that
   * it will perform.
   *
   * @param  replayThread  The thread that has completed.
   */
  public void threadDone(ReplayThread replayThread)
  {
    synchronized (pendingRegisterList)
    {
      totalDisconnects         += replayThread.disconnects;
      totalIterationsCompleted += replayThread.iterationsCompleted;
      totalPacketsReplayed     += replayThread.packetsReplayed;

      activeThreads--;
      if (activeThreads <= 0)
      {
        stopRequested = true;
        selector.wakeup();
      }
    }
  }



  /**
   * Requests that the replay process stop as soon as possible.
   */
  public void stopReplay()
  {
    stopRequested = true;
    selector.wakeup();
  }



  /**
   * Waits for all the replay threads to complete before returning.
   */
  public void waitForReplayThreads()
  {
    while (activeThreads > 0)
    {
      try
      {
        Thread.sleep(10);
      } catch (Exception e) {}
    }
  }



  /**
   * Retrieves the number of capture packets that have been read into memory.
   *
   * @return  The number of capture packets that have been read into memory.
   */
  public int getNumPackets()
  {
    return captureData.length;
  }



  /**
   * Retrieves the total number of disconnects that were encountered when all
   * threads were running.
   *
   * @return  The total number of disconnects that were encountered when all
   *          threads were running.
   */
  public long getTotalDisconnects()
  {
    return totalDisconnects;
  }



  /**
   * Retrieves the total number of times that the replay threads made it
   * entirely through the capture data set (does not include partial iterations
   * completed).
   *
   * @return  The total number of times that the replay threads made it entirely
   *          through the capture data set.
   */
  public long getTotalIterationsCompleted()
  {
    return totalIterationsCompleted;
  }



  /**
   * Retrieves the total number of request packets that the client replayed.
   *
   * @return  The total number of request packets that the client replayed.
   */
  public long getTotalPacketsReplayed()
  {
    return totalPacketsReplayed;
  }
}

