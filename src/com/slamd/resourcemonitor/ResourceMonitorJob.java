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
package com.slamd.resourcemonitor;



import java.util.ArrayList;

import com.slamd.job.JobClass;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.ResourceMonitorStatTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a "job" that will be used to tell resource monitor clients
 * how they should collect statistics.
 *
 *
 * @author   Neil A. Wilson
 */
public class ResourceMonitorJob
{
  // The list of resource monitors associated with this monitor job.
  private ArrayList<ResourceMonitor> activeMonitors;

  // The list of log messages associated with this monitor job.
  private ArrayList<String> logMessages;

  // The list of stat trackers maintained by each of the monitor threads.
  private ArrayList<ResourceMonitorStatTracker> statList;

  // Indicates whether this client should enable real-time statistics reporting.
  private boolean enableRealTimeStats;

  // A flag used to determine whether the monitor threads have already been
  // requested to stop.
  private boolean stopRequested;

  // The statistics collection interval that should be used by monitors.
  private int collectionInterval;

  // The maximum length of time that the monitor threads should run.
  private int duration;

  // The time that the job should start and stop running.
  private long startTime;
  private long stopTime;

  // The actual start and stop times for the job.
  private long actualStartTime;
  private long actualStopTime;

  // A mutex used to provide threadsafe access to the list of active monitors.
  private final Object monitorMutex;

  // The stat reporter to use for reporting real-time statistics to the SLAMD
  // server.
  private RealTimeStatReporter statReporter;

  // The resource monitor client with which this job is associated.
  private ResourceMonitorClient monitorClient;

  // The job ID of the SLAMD job with which this monitor job is associated.
  private String jobID;



  /**
   * Creates a new resource monitor job with the provided information.
   *
   * @param  monitorClient        The resource monitor client that created this
   *                              monitor job.
   * @param  jobID                The job ID of the SLAMD job with which this
   *                              monitor job is associated.
   * @param  startTime            The time that the monitor threads should start
   *                              collecting statistics.
   * @param  stopTime             The time that the monitor threads should stop
   *                              collecting statistics.
   * @param  duration             The maximum length of time in seconds that the
   *                              monitor threads should run.
   * @param  collectionInterval   The statistics collection interval that should
   *                              be used by monitor threads.
   * @param  enableRealTimeStats  Indicates whether real-time statistics
   *                              collection should be enabled for this job.
   * @param  statReporter         The real-time stat reporter to use for this
   *                              job.
   */
  public ResourceMonitorJob(ResourceMonitorClient monitorClient, String jobID,
                            long startTime, long stopTime, int duration,
                            int collectionInterval, boolean enableRealTimeStats,
                            RealTimeStatReporter statReporter)
  {
    this.monitorClient       = monitorClient;
    this.jobID               = jobID;
    this.startTime           = startTime;
    this.stopTime            = stopTime;
    this.duration            = duration;
    this.collectionInterval  = collectionInterval;
    this.enableRealTimeStats = enableRealTimeStats;
    this.statReporter        = statReporter;

    logMessages   = new ArrayList<String>();
    statList      = new ArrayList<ResourceMonitorStatTracker>();
    stopRequested = false;

    monitorMutex = new Object();
    synchronized (monitorMutex)
    {
      activeMonitors = new ArrayList<ResourceMonitor>();
      ResourceMonitor[] definedMonitors = monitorClient.getDefinedMonitors();
      for (int i=0; i < definedMonitors.length; i++)
      {
        try
        {
          ResourceMonitor monitor = definedMonitors[i].newInstance();
          monitor.setName(monitor.getMonitorName() + " -- " + jobID);
          activeMonitors.add(monitor);
        }
        catch (Exception e)
        {
          monitorClient.logVerbose("Unable to create resource monitor " +
                                   definedMonitors[i].getMonitorName() +
                                   " -- " + JobClass.stackTraceToString(e));
        }
      }
    }
  }



  /**
   * Retrieves the job ID of the SLAMD job with which this resource monitor job
   * is associated.
   *
   * @return  The job ID of the SLAMD job with which this resource monitor job
   *          is associated.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the time that this monitor job should start.
   *
   * @return  The time that this monitor job should start.
   */
  public long getStartTime()
  {
    return startTime;
  }



  /**
   * Retrieves the time that this monitor job should stop.
   *
   * @return  The time that this monitor job should stop.
   */
  public long getStopTime()
  {
    return stopTime;
  }



  /**
   * Retrieves the maximum length of time in seconds that this monitor job
   * should run.
   *
   * @return  The maximum length of time in seconds that this monitor job should
   *          run.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Retrieves the actual start time for this monitor job.
   *
   * @return  The actual start time for this monitor job.
   */
  public long getActualStartTime()
  {
    return actualStartTime;
  }



  /**
   * Retrieves the actual stop time for this monitor job.
   *
   * @return  The actual stop time for this monitor job.
   */
  public long getActualStopTime()
  {
    return actualStopTime;
  }



  /**
   * Retrieves the time at which this job should stop running.
   *
   * @return  The time at which this job should stop running.
   */
  public long getShouldStopTime()
  {
    long durationStopTime = -1;
    if (duration > 0)
    {
      durationStopTime = actualStartTime + (duration * 1000);
    }

    if (stopTime > 0)
    {
      return Math.min(stopTime, durationStopTime);
    }
    else
    {
      return durationStopTime;
    }
  }



  /**
   * Retrieves the collection interval that monitor threads should use.
   *
   * @return  The collection interval that monitor threads should use.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Indicates whether this job should collect statistical data in real time.
   *
   * @return  <CODE>true</CODE> if the client should collect statistical data in
   *          real time, or <CODE>false</CODE> if not.
   */
  public boolean enableRealTimeStats()
  {
    return enableRealTimeStats;
  }



  /**
   * Retrieves the stat reporter that should be used to report real-time
   * statistical data.
   *
   * @return  The stat reporter that should be used to report real-time
   *          statistical data, or <CODE>null</CODE> if no reporting should be
   *          done.
   */
  public RealTimeStatReporter getStatReporter()
  {
    return statReporter;
  }



  /**
   * Notifies all the resource monitors that they should start collecting
   * statistics.
   */
  public void startCollecting()
  {
    actualStartTime = System.currentTimeMillis();

    synchronized (monitorMutex)
    {
      for (int i=0; i < activeMonitors.size(); i++)
      {
        activeMonitors.get(i).startCollecting(this);
      }
    }
  }



  /**
   * Notifies all the resource monitors that they should stop collecting
   * statistics.
   */
  public void stopCollecting()
  {
    ResourceMonitor[] runningMonitors;
    synchronized (monitorMutex)
    {
      runningMonitors = new ResourceMonitor[activeMonitors.size()];
      activeMonitors.toArray(runningMonitors);
    }

    if (stopRequested)
    {
      for (int i=0; i < runningMonitors.length; i++)
      {
        runningMonitors[i].stopAndWait();
      }
    }
    else
    {
      stopRequested = true;
      for (int i=0; i < runningMonitors.length; i++)
      {
        runningMonitors[i].stopCollecting();
      }
    }
  }



  /**
   * Indicates that the provided monitor thread has completed its processing for
   * this job.
   *
   * @param  monitor  The monitor thread that has completed its processing.
   */
  public void monitorDone(ResourceMonitor monitor)
  {
    boolean jobDone = false;

    synchronized (monitorMutex)
    {
      StatTracker[] monitorStats = monitor.getResourceStatistics();
      for (int i=0; ((monitorStats != null) && (i < monitorStats.length)); i++)
      {
        statList.add(new ResourceMonitorStatTracker(monitor, monitorStats[i]));
      }

      String[] monitorMessages = monitor.getLogMessages();
      for (int i=0; i < monitorMessages.length; i++)
      {
        logMessages.add(monitorMessages[i]);
      }

      activeMonitors.remove(monitor);

      if (activeMonitors.isEmpty())
      {
        // All the monitors are done.  Notify the monitor client.
        actualStopTime = System.currentTimeMillis();
        jobDone = true;
      }
    }

    if (jobDone)
    {
      monitorClient.jobDone(this);
    }
  }



  /**
   * Retrieves the set stat trackers associated with this resource monitor job.
   *
   * @return  The set of stat trackers associated with this resource monitor
   *          job.
   */
  public StatTracker[] getStatTrackers()
  {
    // FIXME -- Get rid of this method when we move to the new protocol since
    // it won't be valid anymore.
    StatTracker[] trackers = new StatTracker[statList.size()];
    for (int i=0; i < trackers.length; i++)
    {
      trackers[i] = statList.get(i).getStatTracker();
    }

    return trackers;
  }




  /**
   * Retrieves the set of resource monitor stat trackers associated with this
   * resource monitor job.
   *
   * @return  The set of resource monitor stat trackers associated with this
   *          resource monitor job.
   */
  public ResourceMonitorStatTracker[] getResourceMonitorStatTrackers()
  {
    ResourceMonitorStatTracker[] trackers =
         new ResourceMonitorStatTracker[statList.size()];
    statList.toArray(trackers);
    return trackers;
  }



  /**
   * Retrieves the set of log messages associated with this resource monitor
   * job.
   *
   * @return  The set of log messages associated with this resource monitor job.
   */
  public String[] getLogMessages()
  {
    String[] messages = new String[logMessages.size()];
    logMessages.toArray(messages);
    return messages;
  }
}

