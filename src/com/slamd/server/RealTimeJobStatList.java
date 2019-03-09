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



/**
 * This class defines a data type that will be used to hold real-time stat data
 * for a particular statistic in a particular job.  Note that this class is not
 * designed to be threadsafe, so any operations on it should be properly
 * synchronized by the callers.
 *
 *
 * @author   Neil A. Wilson
 */
public class RealTimeJobStatList
{
  // Indicates whether we need to average the data before making it available
  // externally.
  private boolean averageData;

  // The data associated with this stat list.
  private double[] intervalData;

  // A backup array that will be used if we need to shift data out of the
  // current data array.
  private double[] newData;

  // The interval number for the first interval currently held in this list.
  private int firstInterval;

  // The interval number for the last interval currently held in this list.
  private int lastInterval;

  // The current size of this list.
  private int listSize;

  // The maximum number of intervals that should be held in this list.
  private int maxIntervals;

  // The number of threads that have reported data for each interval.
  private int numReporters[];

  // A backup array that will be used if we need to shift data out of the
  // current reporter array.
  private int newReporters[];

  // The name of the statistic which which this data is associated.
  private String statName;



  /**
   * Creates a new real-time job stat list.
   *
   * @param  statName      The name of the statistic with which this data is
   *                       associated.
   * @param  maxIntervals  The maximum number of collection intervals that
   *                       will be maintained for this statistic.
   */
  public RealTimeJobStatList(String statName, int maxIntervals)
  {
    this.statName     = statName;
    this.maxIntervals = maxIntervals;

    intervalData  = new double[maxIntervals];
    newData       = new double[maxIntervals];
    numReporters  = new int[maxIntervals];
    newReporters  = new int[maxIntervals];
    averageData   = false;
    firstInterval = -1;
    lastInterval  = -1;
    listSize      = 0;
  }



  /**
   * Retrieves the name of the statistic with which this data is associated.
   *
   * @return  The name of the statistic with which this data is associated.
   */
  public String getStatName()
  {
    return statName;
  }



  /**
   * Retrieves the interval number of the first interval currently held in this
   * stat list.
   *
   * @return  The interval number of the first interval currently held in this
   *          stat list.
   */
  public int getFirstInterval()
  {
    return firstInterval;
  }



  /**
   * Retrieves the interval number of the last interval currently held in this
   * stat list.
   *
   * @return  The interval number of the last interval currently held in this
   *          stat list.
   */
  public int getLastInterval()
  {
    return lastInterval;
  }



  /**
   * Retrieves a copy of the data currently held in this stat list.
   *
   * @return  A copy of the data currently held in this stat list.
   */
  public double[] getStatData()
  {
    double[] dataCopy     = new double[listSize];
    int[]    reporterCopy = new int[listSize];

    System.arraycopy(intervalData, 0, dataCopy, 0, listSize);
    System.arraycopy(numReporters, 0, reporterCopy, 0, listSize);

    if (averageData)
    {
      for (int i=0; i < dataCopy.length; i++)
      {
        dataCopy[i] = dataCopy[i] / reporterCopy[i];
      }
    }

    return dataCopy;
  }



  /**
   * Adds the provided value to the data for the given interval in the list.
   *
   * @param  intervalNumber  The interval number in which the update is to be
   *                         made.
   * @param  value           The value to add to the data for that interval.
   * @param  averageData     Indicates whether the data associated with this
   *                         statistic should be averaged.
   */
  public void addValue(int intervalNumber, double value, boolean averageData)
  {
    this.averageData = averageData;


    if (listSize == 0)
    {
      // This is the first element in the list.
      firstInterval   = intervalNumber;
      lastInterval    = intervalNumber;
      listSize        = 1;
      intervalData[0] = value;
      numReporters[0] = 1;
    }
    else if (intervalNumber < firstInterval)
    {
      // Oops.  This is data that has already been flushed from the list.
      // Ignore it.
      return;
    }
    else if (intervalNumber > lastInterval)
    {
      // This is data beyond the end of the list.  Figure out how to handle it.
      if (intervalNumber == (lastInterval + 1))
      {
        // We need to add one more element to the list.
        if (listSize < maxIntervals)
        {
          // This is OK -- we already have room for the element.
          intervalData[listSize] = value;
          numReporters[listSize] = 1;
          lastInterval = intervalNumber;
          listSize++;
        }
        else
        {
          // We need to shift everything over one slot to make room for the
          // new value.  Do that with a separate array.
          System.arraycopy(intervalData, 1, newData, 0, (maxIntervals-1));
          newData[listSize-1] = value;

          System.arraycopy(numReporters, 1, newReporters, 0, (maxIntervals-1));
          newReporters[listSize-1] = 1;

          intervalData = newData;
          numReporters = newReporters;

          firstInterval++;
          lastInterval = intervalNumber;
        }
      }
      else
      {
        // This is well beyond the end of the list.  Ignore it.
        return;
      }
    }
    else
    {
      // This is data in the middle of the list.  We can handle that.
      int offset = intervalNumber - firstInterval;
      intervalData[offset] += value;
      numReporters[offset]++;
    }
  }
}

