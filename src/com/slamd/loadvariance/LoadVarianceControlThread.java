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
package com.slamd.loadvariance;



import java.util.Arrays;



/**
 * This class defines a thread that is used to keep track of when the actual job
 * threads should start and stop their processing (which may need to happen
 * several times over the course of a job).  It does this by setting boolean
 * variables that the job should watch in order to determine whether it should
 * currently be running.
 */
public class LoadVarianceControlThread
       extends Thread
{
  // Indicates whether a request has been received for this thread to start.
  private boolean startRequested;

  // Indicates whether a request has been received for this thread stop stop.
  private boolean stopRequested;

  // The load variance job instance that this control thread should manage.
  private LoadVarianceJobClass jobClass;




  /**
   * Creates a new instance of this load variance control thread that will be
   * used to manage the provided job class.
   *
   * @param  jobClass  The job class that this control thread will manage.
   */
  public LoadVarianceControlThread(LoadVarianceJobClass jobClass)
  {
    this.jobClass = jobClass;

    setName("Load Variance Control Thread");

    startRequested = false;
    stopRequested  = false;
  }



  /**
   * Sets a flag that indicates that the control thread should start managing
   * the job.
   */
  public void startRunning()
  {
    startRequested = true;
  }



  /**
   * Sets a flag that indicates that the control thread should stop managing the
   * job.
   */
  public void stopRunning()
  {
    stopRequested = true;

    // Make sure to signal any the remaining threads that they should stop.
    Arrays.fill(jobClass.threadsActive, false);
  }



  /**
   * Waits for the actual job to start, then sets flags that will be read by
   * that job to indicate whether a given thread should be started or stopped.
   */
  public void run()
  {
    // If there are no lines in the load distribution file, then we can simply
    // turn on all the threads and exit right away.
    if ((jobClass.varianceData == null) || (jobClass.varianceData.length == 0))
    {
      Arrays.fill(jobClass.threadsActive, true);
      return;
    }


    // Wait until we either get a request to start or stop running.
    while (! (startRequested || stopRequested || jobClass.shouldStop()))
    {
      try
      {
        Thread.sleep(10);
      } catch (Exception e) {}
    }


    // Loop until we get a request to stop.
    int  activeThreads = 0;
    int  maxThreads    = jobClass.getClientSideJob().getThreadsPerClient();
    int  slotPos       = 0;
    long jobStartTime  = System.currentTimeMillis();
    long nextOpTime    = jobStartTime + jobClass.varianceData[0][0];
    while (! (stopRequested || jobClass.shouldStop()))
    {
      long now = System.currentTimeMillis();
      if (now >= nextOpTime)
      {
        // It is time to start or stop the next thread or set of threads.
        int numThreads = jobClass.varianceData[slotPos++][1];
        if (numThreads > 0)
        {
          // We need to start one or more threads.
          for (int i=0; ((i < numThreads) && (activeThreads < maxThreads)); i++)
          {
            jobClass.threadsActive[activeThreads++] = true;
          }
        }
        else
        {
          // We need to stop one or more threads.
          for (int i=numThreads; ((i < 0) && (activeThreads > 0)); i++)
          {
            jobClass.threadsActive[--activeThreads] = false;
          }
        }


        // Now figure out how long it should be before we do the next round of
        // start/stop.  If there is no more work to do, then exit.
        if (slotPos >= jobClass.varianceData.length)
        {
          if (jobClass.loopVarianceDefinition)
          {
            slotPos      = 0;
            jobStartTime = System.currentTimeMillis();
            nextOpTime   = jobStartTime + jobClass.varianceData[0][0];
          }
          else
          {
            return;
          }
        }
        else
        {
          nextOpTime = jobStartTime + jobClass.varianceData[slotPos][0];
        }
      }
      else if ((nextOpTime + 100) > now)
      {
        // We have less than 100 milliseconds before the next thread should be
        // started.  Sleep just for that length of time.
        try
        {
          Thread.sleep(nextOpTime - now);
        } catch (Exception e) {}
      }
      else
      {
        // We have at least 100 milliseconds before the next thread should be
        // started.  Just sleep for 100 milliseconds at a time so that we can
        // detect and respond to cancel requests and other kinds of job
        // termination quickly.
        try
        {
          Thread.sleep(100);
        } catch (Exception e) {}
      }
    }


    // Make sure to signal any the remaining threads that they should stop.
    Arrays.fill(jobClass.threadsActive, false);
  }
}

