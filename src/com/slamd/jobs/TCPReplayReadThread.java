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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

import com.slamd.common.Constants;
import com.slamd.job.JobClass;



/**
 * This class defines a thread that works in conjunction with the TCPReplay job
 * to read any data returned on any of the connections associated with the job
 * threads.
 *
 *
 * @author   Neil A. Wilson
 */
public class TCPReplayReadThread
       extends Thread
{
  // The list of pending connections that need to be registered with the
  // selector.
  private ArrayList<SocketChannel> pendingConnectionList;

  // Indicates whether this thread has been started.
  private boolean threadStarted;

  // Indicates whether we have determined internally that we should stop the
  // selector because there are no more threads registered with it.
  private boolean stopSelector;

  // The value used to keep track of the number of threads that are currently
  // registered to use this read thread.
  private int registeredThreads;

  // A mutex used to provide threadsafe operations for this thread.
  private final Object threadMutex;

  // The selector that will be used to multiplex the reads.
  private Selector selector;

  // The TCPReplay job instance with which this thread is associated.
  private TCPReplayJobClass tcpReplayJob;



  /**
   * Creates a new instance of this TCPReplay read thread that will be used to
   * read data from connections associated with the provided TCPReplay job.
   *
   * @param  tcpReplayJob  The TCPReplay job instance for which this thread
   *                       should read all data from the server.
   *
   * @throws  IOException  If a problem occurs while creating the associated
   *                       selector.
   */
  public TCPReplayReadThread(TCPReplayJobClass tcpReplayJob)
         throws IOException
  {
    this.tcpReplayJob = tcpReplayJob;

    setName("TCP Replay Read Thread");

    selector = Selector.open();

    pendingConnectionList = new ArrayList<SocketChannel>();
    registeredThreads     = 0;
    threadStarted         = false;
    stopSelector          = false;
    threadMutex           = new Object();
  }



  /**
   * Registers a provided job thread with this read thread to indicate that it
   * will be sending connections to this thread for reading.
   */
  public void registerJobThread()
  {
    synchronized (threadMutex)
    {
      registeredThreads++;

      if (! threadStarted)
      {
        start();
        threadStarted = true;
      }
    }
  }



  /**
   * Deregisters a job thread from this read thread to indicate that it will no
   * longer be sending connections to this thread for reading.
   */
  public void threadDone()
  {
    synchronized (threadMutex)
    {
      registeredThreads--;

      if (registeredThreads <= 0)
      {
        stopSelector = true;
      }
    }
  }



  /**
   * Registers the provided socket channel with this read thread so that any
   * incoming data available on that connection will be read.
   *
   * @param  socketChannel  The socket channel to be registered.  It must
   *                        already be configured in non-blocking mode.
   */
  public void registerSocketChannel(SocketChannel socketChannel)
  {
    synchronized (threadMutex)
    {
      pendingConnectionList.add(socketChannel);
    }

    selector.wakeup();
  }



  /**
   * Loops, waiting for data to be available on any of the registered
   * connections, reading it as it becomes available.
   */
  @Override()
  public void run()
  {
    // Allocate the buffer that we will use to read data from clients.
    ByteBuffer buffer = ByteBuffer.allocateDirect(4096);


    // Loop, reading data from clients until we decide that we should stop for
    // some reason.
    boolean consecutiveFailures = false;
    while (! (tcpReplayJob.shouldStop() || stopSelector))
    {
      try
      {
        // See if there is any data available for reading.
        int selectedKeys = selector.select(100);
        if (selectedKeys > 0)
        {
          Iterator iterator = selector.selectedKeys().iterator();
          while (iterator.hasNext())
          {
            SelectionKey key = (SelectionKey) iterator.next();

            if (key.isReadable())
            {
              SocketChannel channel = (SocketChannel) key.channel();
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
                  key.cancel();
                }
              }
              else
              {
                key.cancel();
              }
            }

            iterator.remove();
          }
        }


        // See if there are any pending connections that need to be registered.
        synchronized (threadMutex)
        {
          if (! pendingConnectionList.isEmpty())
          {
            Iterator iterator = pendingConnectionList.iterator();
            while (iterator.hasNext())
            {
              SocketChannel channel = (SocketChannel) iterator.next();
              channel.register(selector, SelectionKey.OP_READ);
            }

            pendingConnectionList.clear();
          }
        }

        consecutiveFailures = false;
      }
      catch (Exception e)
      {
        if (consecutiveFailures)
        {
          tcpReplayJob.logMessage("Exception caught while trying to read " +
                                  "data from a client:  " +
                                  JobClass.stackTraceToString(e));
          stopSelector = true;
        }
        else
        {
          consecutiveFailures = true;
          tcpReplayJob.writeVerbose("Exception caught while trying to read " +
                                    "data from a client:  " +
                                    JobClass.stackTraceToString(e));
        }
      }
    }


    // If we've gotten here, then we can't do any more good.  Request that the
    // job stop and close any connections that may still be associated with the
    // selector.
    if (consecutiveFailures)
    {
      tcpReplayJob.stopJob(Constants.JOB_STATE_STOPPED_DUE_TO_ERROR);
    }
    else
    {
      tcpReplayJob.stopJob(Constants.JOB_STATE_COMPLETED_SUCCESSFULLY);
    }

    synchronized (threadMutex)
    {
      Iterator iterator = selector.keys().iterator();
      while (iterator.hasNext())
      {
        try
        {
          SelectionKey key = (SelectionKey) iterator.next();
          key.channel().close();
          key.cancel();
        } catch (Exception e) {}
      }
    }
  }
}

