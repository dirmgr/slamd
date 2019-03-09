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
package com.slamd.server;



import java.util.Iterator;
import java.util.LinkedHashMap;

import com.slamd.job.Job;



/**
 * This class defines a data type that will be used to hold real-time stat data
 * for a particular job.  Note that this class is not thread safe, so it is
 * assumed that whatever is using it performs adequate locking to ensure
 * proper safety.
 *
 *
 * @author   Neil A. Wilson
 */
public class RealTimeJobStats
{
  // The maximum number of collection intervals to retain.
  private int maxIntervals;

  // The total number of reporters registered with this stat handler.  Each
  // statistic for each thread counts as a separate stat handler.
  private int statThreadsRegistered;

  // The job with which this stat handler is associated.
  private Job job;

  // The hash map containing the actual data mapped by statistic.
  private LinkedHashMap<String,RealTimeJobStatList> statHash;

  // The time that this stat handler was last updated.
  private long lastUpdateTime;

  // The real-time stat handler associated with this stat data handler.
  private RealTimeStatHandler statHandler;

  // The SLAMD server with which this stat handler is associated.
  private SLAMDServer slamdServer;

  // The job ID of the job with which this data is associated.
  private String jobID;



  /**
   * Creates a new stat data handler for the specified job.
   *
   * @param  statHandler   The real-time stat handler that associated with this
   *                       stat data handler.
   * @param  jobID         The job ID with which this stat handler is
   *                       associated.
   * @param  maxIntervals  The maximum number of collection intervals to
   *                       retain.
   *
   * @throws  SLAMDServerException  If the specified job is unknown to the
   *                                SLAMD server.
   */
  public RealTimeJobStats(RealTimeStatHandler statHandler, String jobID,
                          int maxIntervals)
         throws SLAMDServerException
  {
    this.statHandler  = statHandler;
    this.slamdServer  = statHandler.getSLAMDServer();
    this.jobID        = jobID;
    this.maxIntervals = maxIntervals;

    lastUpdateTime = System.currentTimeMillis();
    statThreadsRegistered = 0;
    statHash = new LinkedHashMap<String,RealTimeJobStatList>();

    job = slamdServer.getScheduler().getJob(jobID);
    job.setRealTimeStats(this);
  }



  /**
   * Retrieves the job ID with which this handler is associated.
   *
   * @return  The job ID with which this handler is associated.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the job with which this handler is associated.
   *
   * @return  The job with which this handler is associated.
   */
  public Job getJob()
  {
    return job;
  }



  /**
   * Retrieves the display names of the statistics that are available in this
   * stat handler.
   *
   * @return  The display names of the statistics that are available in this
   *          stat handler.
   */
  public String[] getStatNames()
  {
    String[] statNames = new String[statHash.size()];

    int i =0;
    Iterator iterator = statHash.keySet().iterator();
    while (iterator.hasNext())
    {
      statNames[i++] = (String) iterator.next();
    }

    return statNames;
  }



  /**
   * Retrieves the data currently available for the specified statistic.
   *
   * @param  statName  The name of the statistic for which to retrieve the
   *                   data.
   *
   * @return  The data currently available for the specified statistic, or
   *          <CODE>null</CODE> if no data is available for the specified
   *          statistic.
   */
  public double[] getStatValues(String statName)
  {
    RealTimeJobStatList statList = statHash.get(statName);
    if (statList == null)
    {
      return null;
    }
    else
    {
      return statList.getStatData();
    }
  }



  /**
   * Retrieves the interval number of the first interval available for the
   * specified statistic.
   *
   * @param  statName  The name of the statistic for which to retrieve the
   *                   data.
   *
   * @return  The interval number of the first interval available for the
   *          specified statistic, or -1 if nothing is known about that
   *          statistic.
   */
  public int getFirstInterval(String statName)
  {
    RealTimeJobStatList statList = statHash.get(statName);
    if (statList == null)
    {
      return -1;
    }
    else
    {
      return statList.getFirstInterval();
    }
  }



  /**
   * Registers the specified statistic with this stat handler.
   *
   * @param  statName  The name of the statistic being registered.
   */
  public void registerStatistic(String statName)
  {
    Object statObject = statHash.get(statName);
    if (statObject == null)
    {
      statHash.put(statName, new RealTimeJobStatList(statName, maxIntervals));
    }

    statThreadsRegistered++;
  }



  /**
   * Deregisters a stat reporter from this stat handler.  This is only used to
   * determine whether there are any more threads still reporting.  If no more
   * threads are still reporting, then the reference to the stat information
   * for this job is removed.
   */
  public void deregisterStatistic()
  {
    statThreadsRegistered--;

    if (statThreadsRegistered == 0)
    {
      statHandler.removeJobStatsUnlocked(jobID);
      job.setRealTimeStats(null);
    }
  }



  /**
   * Updates the data for the specified statistic to indicate that the provided
   * value should be added to the existing data for the specified interval.
   *
   * @param  statName        The name of the statistic to update.
   * @param  intervalNumber  The interval number with which the value is
   *                         associated.
   * @param  value           The value to be added to the existing values for
   *                         the given interval.
   */
  public void updateStatToAdd(String statName, int intervalNumber, double value)
  {
    RealTimeJobStatList statList = statHash.get(statName);
    if (statList == null)
    {
      // A stat that has not been registered.  What to do here?
      // I guess just ignore it.
      return;
    }

    statList.addValue(intervalNumber, value, false);
  }



  /**
   * Updates the data for the specified statistic to indicate that the provided
   * value should be averaged with the existing data for the specified interval.
   *
   * @param  statName        The name of the statistic to update.
   * @param  intervalNumber  The interval number with which the value is
   *                         associated.
   * @param  value           The value to be averaged with the existing values
   *                         for the given interval.
   */
  public void updateStatToAverage(String statName, int intervalNumber,
                                  double value)
  {
    RealTimeJobStatList statList = statHash.get(statName);
    if (statList == null)
    {
      // A stat that has not been registered.  What to do here?
      // I guess just ignore it.
      return;
    }

    statList.addValue(intervalNumber, value, true);
  }



  /**
   * Indicates that the lastUpdateTime should be set to the current time.
   */
  public void setLastUpdateTime()
  {
    lastUpdateTime = System.currentTimeMillis();
  }



  /**
   * Retrieves the time that this data was last updated.
   *
   * @return  The time that this data was last updated.
   */
  public long getLastUpdateTime()
  {
    return lastUpdateTime;
  }
}

