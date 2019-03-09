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
package com.slamd.stat;



import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.client.Client;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.Job;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class defines a stat tracker that can be used to track events that occur
 * periodically.  It records the time and magnitude of each occurrence.
 *
 *
 * @author   Neil A. Wilson
 */
public class PeriodicEventTracker
       implements StatTracker
{
  /**
   * The initial size that will be used for the value arrays for this stat
   * tracker, as well as the size increment that will be used when the arrays
   * get full.
   */
  public static final int ARRAY_SIZE_INCREMENT = 1000;



  // Indicates whether the generated graph should be based at zero or at the
  // lowest actual value.
  private boolean baseAtZero;

  // Indicates whether the generated graph should be flat between data points.
  private boolean flatBetweenPoints;

  // Indicates whether this stat tracker has been started.
  private boolean hasBeenStarted;

  // Indicates whether the generated graph should include an average line.
  private boolean includeAverage;

  // Indicates whether the generated graph should include a horizontal grid.
  private boolean includeHorizontalGrid;

  // Indicates whether the generated graph should include a legend.
  private boolean includeLegend;

  // Indicates whether the generated graph should include a vertical grid.
  private boolean includeVerticalGrid;

  // Indicates whether this stat tracker is currently running.
  private boolean isRunning;

  // The decimal format that will be used to format floating-point values.
  private DecimalFormat decimalFormat;

  // The total of all values provided.
  private double totalValue;

  // The magnitudes for the tracked event.
  private double[] eventValues;

  // The length of time in seconds that this tracker was active.
  private int duration;

  // The statistics collection interval to use for this stat tracker.  This
  // isn't important for normal operations, but it may be needed for other
  // things like real-time reporting.
  private int collectionInterval;

  // The number of times that the tracked event has occurred.
  private int numOccurrences;

  // The time that this stat tracker started collecting statistics.
  private long startTime;

  // The time that this stat tracker stopped collecting statistics.
  private long stopTime;

  // The times that the tracked event occurred.
  private long[] eventTimes;

  // The date formatter that will be used to format dates.
  private SimpleDateFormat dateFormat;

  // The ID assigned to the client running this stat tracker.
  private String clientID;

  // The display name for this stat tracker;
  private String displayName;

  // The ID assigned to the thread running this stat tracker.
  private String threadID;



  /**
   * Creates a new instance of this stat intended for use as a placeholder for
   * decoding purposes.  This version of the constructor should not be used
   * by job classes.
   */
  public PeriodicEventTracker()
  {
    this.clientID           = "";
    this.threadID           = "";
    this.displayName        = "";
    this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;

    baseAtZero             = true;
    flatBetweenPoints      = true;
    includeAverage         = false;
    includeHorizontalGrid  = true;
    includeVerticalGrid    = true;
    includeLegend          = false;
    totalValue             = 0.0;
    eventValues            = new double[ARRAY_SIZE_INCREMENT];
    eventTimes             = new long[ARRAY_SIZE_INCREMENT];
    numOccurrences         = 0;
    startTime              = System.currentTimeMillis();
    stopTime               = startTime;
    duration               = 0;
    decimalFormat          = new DecimalFormat("0.000");
    dateFormat             =
         new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    hasBeenStarted         = false;
    isRunning              = false;
  }



  /**
   * Creates a new instance of this stat tracker based on the provided
   * information.
   *
   * @param  clientID            The client ID of the client that used this
   *                             stat tracker.
   * @param  threadID            The thread ID of the thread that used this
   *                             stat tracker.
   * @param  displayName         The display name to use for this stat tracker.
   * @param  collectionInterval  The collection interval in seconds that
   *                             should be used for this stat tracker.
   */
  public PeriodicEventTracker(String clientID, String threadID,
                              String displayName, int collectionInterval)
  {
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.displayName        = displayName;
    this.collectionInterval = collectionInterval;

    baseAtZero             = true;
    flatBetweenPoints      = true;
    includeAverage         = false;
    includeHorizontalGrid  = true;
    includeVerticalGrid    = true;
    includeLegend          = false;
    totalValue             = 0.0;
    eventValues            = new double[ARRAY_SIZE_INCREMENT];
    eventTimes             = new long[ARRAY_SIZE_INCREMENT];
    numOccurrences         = 0;
    startTime              = System.currentTimeMillis();
    stopTime               = startTime;
    duration               = 0;
    decimalFormat          = new DecimalFormat("0.000");
    dateFormat             =
         new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    hasBeenStarted         = false;
    isRunning              = false;
  }



  /**
   * Creates a new instance of this stat tracker.  The new instance should have
   * the same type, display name, client ID, thread ID, and collection interval
   * as the stat tracker used to create it.
   *
   * @return  The new instance of this stat tracker.
   */
  public StatTracker newInstance()
  {
    PeriodicEventTracker tracker =
         new PeriodicEventTracker(clientID, threadID, displayName,
                                  collectionInterval);
    tracker.setFlatBetweenPoints(flatBetweenPoints);
    tracker.setBaseAtZero(baseAtZero);
    tracker.setIncludeAverage(includeAverage);
    tracker.setIncludeHorizontalGrid(includeHorizontalGrid);
    tracker.setIncludeVerticalGrid(includeVerticalGrid);

    return tracker;
  }



  /**
   * Indicates that the stat tracker is to start maintaining statistics and that
   * it should start its internal timer.
   */
  public void startTracker()
  {
    // If the tracker has already been started, then print an error message.
    // Otherwise, indicate that it is started and running.
    if (hasBeenStarted)
    {
      System.err.println("***** WARNING:  Multiple calls to start " +
                         "periodic event stat tracker " + displayName);
    }
    else
    {
      hasBeenStarted = true;
      isRunning = true;
    }

    // Register this tracker with the persistence thread.
    Client.registerPersistentStatistic(this);

    startTime      = System.currentTimeMillis();
    eventTimes     = new long[ARRAY_SIZE_INCREMENT];
    eventValues    = new double[ARRAY_SIZE_INCREMENT];
    numOccurrences = 0;
  }



  /**
   * Indicates that the stat tracker that there will not be any more statistics
   * collection done and that it should stop its internal timer.
   */
  public void stopTracker()
  {
    stopTime = System.currentTimeMillis();


    // If the tracker was never started, then print an error message.
    // Otherwise, indicate that it is no longer running.
    if (! hasBeenStarted)
    {
      System.err.println("***** WARNING:  Attempting to stop periodic event " +
                         "stat tracker " + displayName +
                         " without having started it");
    }
    else
    {
      isRunning = false;
    }


    long[] newTimes = new long[numOccurrences];
    System.arraycopy(eventTimes, 0, newTimes, 0, numOccurrences);
    eventTimes = newTimes;

    double[] newValues = new double[numOccurrences];
    System.arraycopy(eventValues, 0, newValues, 0, numOccurrences);
    eventValues = newValues;

    duration = (int) ((stopTime - startTime) / 1000);
  }



  /**
   * Indicates whether this stat tracker has been started, regardless of whether
   * it is currently running.
   *
   * @return  <CODE>true</CODE> if this stat tracker has been started, or
   *          <CODE>false</CODE> if it has not yet been started.
   */
  public boolean hasBeenStarted()
  {
    return hasBeenStarted;
  }



  /**
   * Indicates whether this stat tracker is currently running.
   *
   * @return  <CODE>true</CODE> if this stat tracker is currently running, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isRunning()
  {
    return isRunning;
  }



  /**
   * Indicates that the stat tracker should enable real-time statistics
   * collection.  Note that some stat trackers may not support real-time
   * statistics collection, in which case this method may be ignored.
   *
   * @param  statReporter  The stat-reporter that should be used to report
   *                       real-time statistics to the SLAMD server.
   * @param  jobID         The job ID of the job that will be reporting the
   *                       data.
   */
  public void enableRealTimeStats(RealTimeStatReporter statReporter,
                                  String jobID)
  {
    // No action required.  Real-time stat reporting is currently not supported
    // for this tracker.
  }



  /**
   * Updates the tracker with the provided value.  The current time will be used
   * as the time of the update.
   *
   * @param  value  The magnitude for the update.
   */
  public void update(double value)
  {
    update(System.currentTimeMillis(), value);
  }



  /**
   * Updates the tracker with the provided time and value.
   *
   * @param  time   The time for the update.
   * @param  value  The magnitude for the update.
   */
  public void update(long time, double value)
  {
    if (numOccurrences >= eventTimes.length)
    {
      long[] newTimes = new long[eventTimes.length + ARRAY_SIZE_INCREMENT];
      System.arraycopy(eventTimes, 0, newTimes, 0, numOccurrences);
      eventTimes = newTimes;

      double[] newValues =
           new double[eventValues.length + ARRAY_SIZE_INCREMENT];
      System.arraycopy(eventValues, 0, newValues, 0, numOccurrences);
      eventValues = newValues;
    }

    eventTimes[numOccurrences]  = time;
    eventValues[numOccurrences] = value;
    numOccurrences++;
    totalValue += value;
  }



  /**
   * Specifies the data associated with this tracker.  Note that the number of
   * values in the time array must equal the number of elements in the value
   * array.
   *
   * @param  eventTimes   The times of the event occurrences.
   * @param  eventValues  The values associated with the event occurrences.
   */
  public void setEventData(long[] eventTimes, double[] eventValues)
  {
    this.eventTimes  = eventTimes;
    this.eventValues = eventValues;
    numOccurrences = eventTimes.length;

    totalValue = 0.0;
    for (int i=0; i < numOccurrences; i++)
    {
      totalValue += eventValues[i];
    }
  }



  /**
   * Indicates whether graphs generated from this tracker should have a lower
   * range based at zero or the actual collected values.
   *
   * @param  baseAtZero  Indicates whether graphs generated from this tracker
   *                     should have a lower range based at zero.
   */
  public void setBaseAtZero(boolean baseAtZero)
  {
    this.baseAtZero = baseAtZero;
  }



  /**
   * Indicates whether graphs generated from this tracker should have lines that
   * are flat between data points rather than directly connecting the points.
   *
   * @param  flatBetweenPoints  Indicates whether graphs generated from this
   *                            tracker should have lines that are flat between
   *                            data points.
   */
  public void setFlatBetweenPoints(boolean flatBetweenPoints)
  {
    this.flatBetweenPoints = flatBetweenPoints;
  }



  /**
   * Indicates whether graphs generated from this tracker should include an
   * average line.
   *
   * @param  includeAverage  Indicates whether graphs generated from this
   *                         tracker should include an average line.
   */
  public void setIncludeAverage(boolean includeAverage)
  {
    this.includeAverage = includeAverage;
  }



  /**
   * Indicates whether graphs generated from this tracker should include
   * horizontal grid lines.
   *
   * @param  includeHorizontalGrid  Indicates whether graphs generated from this
   *                                tracker should include horizontal grid
   *                                lines.
   */
  public void setIncludeHorizontalGrid(boolean includeHorizontalGrid)
  {
    this.includeHorizontalGrid = includeHorizontalGrid;
  }



  /**
   * Indicates whether graphs generated from this tracker should include
   * vertical grid lines.
   *
   * @param  includeVerticalGrid  Indicates whether graphs generated from this
   *                              tracker should include vertical grid lines.
   */
  public void setIncludeVerticalGrid(boolean includeVerticalGrid)
  {
    this.includeVerticalGrid = includeVerticalGrid;
  }



  /**
   * Indicates whether graphs generated from this tracker should include a
   * legend.
   *
   * @param  includeLegend  Indicates whether graphs generated from this tracker
   *                        should include a legend.
   */
  public void setIncludeLegend(boolean includeLegend)
  {
    this.includeLegend = includeLegend;
  }



  /**
   * Retrieves the number of times the tracked event occurred.
   *
   * @return  The number of times the tracked event occurred.
   */
  public int getNumOccurrences()
  {
    return numOccurrences;
  }



  /**
   * Retrieves the sum of all the values of the tracked event.
   *
   * @return  The sum of all the values of the tracked event.
   */
  public double getTotalValue()
  {
    return totalValue;
  }



  /**
   * Retrieves the average of all the values of the tracked event.
   *
   * @return  The average of all the values of the tracked event.
   */
  public double getAverageValue()
  {
    if (numOccurrences > 0)
    {
      return (totalValue / numOccurrences);
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves the maximum value of the tracked event.
   *
   * @return  The maximum value of the tracked event.
   */
  public double getMaxValue()
  {
    if (numOccurrences > 0)
    {
      double maxValue = eventValues[0];
      for (int i=1; i < numOccurrences; i++)
      {
        if (eventValues[i] > maxValue)
        {
          maxValue = eventValues[i];
        }
      }

      return maxValue;
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves the maximum value of the tracked event.
   *
   * @return  The maximum value of the tracked event.
   */
  public double getMinValue()
  {
    if (numOccurrences > 0)
    {
      double minValue = eventValues[0];
      for (int i=1; i < numOccurrences; i++)
      {
        if (eventValues[i] < minValue)
        {
          minValue = eventValues[i];
        }
      }

      return minValue;
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves the average length of time in milliseconds between occurrences
   * of the tracked event.
   *
   * @return  The average length of time in milliseconds between occurrences of
   *          the tracked event.
   */
  public double getAverageTimeBetweenOccurrences()
  {
    if (numOccurrences == 0)
    {
      return 0.0;
    }

    return (1.0 * (eventTimes[numOccurrences-1] - eventTimes[0]) /
            (numOccurrences-1));
  }



  /**
   * Retrieves an array containing the time that each occurrence of the tracked
   * event took place.
   *
   * @return  An array containing the time that each occurrence of the tracked
   *          event took place.
   */
  public long[] getEventTimes()
  {
    long[] times = new long[numOccurrences];
    System.arraycopy(eventTimes, 0, times, 0, numOccurrences);
    return times;
  }



  /**
   * Retrieves an array whose elements are the values associated with each
   * occurrence of the tracked event.
   *
   * @return  An array whose elements are the values associated with each
   *          occurrence of the tracked event.
   */
  public double[] getEventValues()
  {
    double[] values = new double[numOccurrences];
    System.arraycopy(eventValues, 0, values, 0, numOccurrences);
    return values;
  }




  /**
   * Retrieves the client ID of the client that used this stat tracker.
   *
   * @return  The client ID of the client that used this stat tracker.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Specifies the client ID of the client that used this stat tracker.  Note
   * that this should only be used when creating a new stat tracker based on
   * encoded data and not when using it to collect statistics.
   *
   * @param  clientID  The client ID of the client that used this stat tracker.
   */
  public void setClientID(String clientID)
  {
    this.clientID = clientID;
  }



  /**
   * Retrieves the thread ID of the client thread that used this stat tracker.
   *
   * @return  The thread ID of the client thread that used this stat tracker.
   */
  public String getThreadID()
  {
    return threadID;
  }



  /**
   * Specifies the thread ID of the client thread that used this stat tracker.
   * Note that this should only be used when creating a new stat tracker based
   * on encoded data and not when using it to collect statistics.
   *
   * @param  threadID  The thread ID of the client thread that used this stat
   *                   tracker.
   */
  public void setThreadID(String threadID)
  {
    this.threadID = threadID;
  }



  /**
   * Retrieves the user-friendly name associated with this stat tracker.
   *
   * @return  The user-friendly name associated with this stat tracker.
   */
  public String getDisplayName()
  {
    return displayName;
  }



  /**
   * Specifies the display name for this stat tracker.  Note that this should
   * only be used when creating a new stat tracker based on encoded data and not
   * when using it to collect statistics.
   *
   * @param  displayName The display name for this stat tracker.
   */
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }



  /**
   * Retrieves the collection interval (in seconds) that will be used for this
   * stat tracker.
   *
   * @return  The collection interval (in seconds) that will be used for this
   *          stat tracker.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Specifies the collection interval for this stat tracker.  Note that this
   * should only be used when creating a new stat tracker based on encoded data
   * and not when using it to collect statistics.
   *
   * @param  collectionInterval  The collection interval in seconds to use for
   *                             this stat tracker.
   */
  public void setCollectionInterval(int collectionInterval)
  {
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves the total length of time in seconds that this stat tracker was
   * capturing statistics.
   *
   * @return  The total length of time in seconds that this stat tracker was
   *          capturing statistics.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Specifies the duration for this stat tracker.  Note that this should only
   * be used when creating a new stat tracker based on encoded data and not when
   * using it to collect statistics.
   *
   * @param  duration  The duration for this stat tracker.
   */
  public void setDuration(int duration)
  {
    this.duration = duration;
  }



  /**
   * Indicates whether the user may search for jobs with statistics collected by
   * this stat tracker.  The search will be "greater than" and "less than" some
   * user-specified value.
   *
   * @return  <CODE>true</CODE> if statistics collected by this stat tracker
   *          should be searchable, or <CODE>false</CODE> if not.
   */
  public boolean isSearchable()
  {
    // Searching will not be allowed because this tracker is not interval-based.
    return false;
  }



  /**
   * Indicates whether the value associated with this stat tracker is greater
   * than or equal to the provided value.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what exactly
   * "the value of this stat tracker" means will be left up to those stat
   * trackers that are searchable.
   *
   * @param  value  The value against which the value of this stat tracker is to
   *                be compared.
   *
   * @return  <CODE>true</CODE> if the value of this stat tracker is greater
   *          than or equal to the provided value, or <CODE>false</CODE> if not.
   */
  public boolean isAtLeast(double value)
  {
    return false;
  }



  /**
   * Indicates whether the value associated with this stat tracker is less than
   * or equal to the provided value.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what exactly
   * "the value of this stat tracker" means will be left up to those stat
   * trackers that are searchable.
   *
   * @param  value  The value against which the value of this stat tracker is to
   *                be compared.
   *
   * @return  <CODE>true</CODE> if the value of this stat tracker is less than
   *          or equal to the provided value, or <CODE>false</CODE> if not.
   */
  public boolean isAtMost(double value)
  {
    return false;
  }



  /**
   * Retrieves the number of intervals for which data is available for this stat
   * tracker.
   *
   * @return  The number of intervals for which data is available for this stat
   *          tracker.
   */
  public int getNumIntervals()
  {
    // This tracker doesn't really work on intervals, but this is close enough.
    return numOccurrences;
  }



  /**
   * Aggregates the information collected by the provided set of stat trackers
   * into a single tracker that represents the information gathered from the
   * entire set of data.  All of the stat trackers in the provided array must be
   * of the same type as the instance into which the information will be
   * aggregated.
   *
   * @param  trackers  The set of stat trackers whose data is to be aggregated.
   */
  public void aggregate(StatTracker[] trackers)
  {
    if ((trackers == null) || (trackers.length == 0))
    {
      return;
    }
    else if (trackers.length == 1)
    {
      PeriodicEventTracker tracker = (PeriodicEventTracker) trackers[0];
      numOccurrences = tracker.numOccurrences;
      startTime      = tracker.startTime;
      stopTime       = tracker.stopTime;
      duration       = (int) ((stopTime - startTime) / 1000);
      totalValue     = tracker.totalValue;

      eventTimes = new long[numOccurrences];
      System.arraycopy(tracker.eventTimes, 0, eventTimes, 0, numOccurrences);

      eventValues = new double[numOccurrences];
      System.arraycopy(tracker.eventValues, 0, eventValues, 0, numOccurrences);

      clientID           = tracker.clientID;
      threadID           = tracker.threadID;
      displayName        = tracker.displayName;
      collectionInterval = tracker.collectionInterval;
    }
    else
    {
      PeriodicEventTracker[] pTrackers =
           new PeriodicEventTracker[trackers.length];
      for (int i=0; i < trackers.length; i++)
      {
        pTrackers[i] = (PeriodicEventTracker) trackers[i];
      }

      numOccurrences = pTrackers[0].numOccurrences;
      startTime      = pTrackers[0].startTime;
      stopTime       = pTrackers[0].stopTime;
      totalValue     = pTrackers[0].totalValue;
      for (int i=1; i < trackers.length; i++)
      {
        numOccurrences += pTrackers[i].numOccurrences;
        totalValue     += pTrackers[i].totalValue;

        if (pTrackers[i].startTime < startTime)
        {
          startTime = pTrackers[i].startTime;
        }

        if (pTrackers[i].stopTime > stopTime)
        {
          stopTime = pTrackers[i].stopTime;
        }
      }

      duration    = (int) ((stopTime - startTime) / 1000);
      eventTimes  = new long[numOccurrences];
      eventValues = new double[numOccurrences];

      int[] positions = new int[pTrackers.length];
      for (int i=0; i < numOccurrences; i++)
      {
        double eventValue = 0.0;
        int    pos        = -1;
        long   eventTime  = -1;

        for (int j=0; j < pTrackers.length; j++)
        {
          if (positions[j] < pTrackers[j].numOccurrences)
          {
            if ((eventTime < 0) ||
                (pTrackers[j].eventTimes[positions[j]] < eventTime))
            {
              eventTime  = pTrackers[j].eventTimes[positions[j]];
              eventValue = pTrackers[j].eventValues[positions[j]];
              pos        = j;
            }
          }
        }

        eventTimes[i]  = eventTime;
        eventValues[i] = eventValue;
        positions[pos]++;
      }
    }
  }



  /**
   * Retrieves the value associated with this stat tracker.  This is only
   * applicable if <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what
   * exactly "the value associated with this stat tracker" means will be left up
   * to those stat trackers that are searchable.
   *
   * @return  The value associated with this stat tracker.
   */
  public double getSummaryValue()
  {
    if (numOccurrences == 0)
    {
      return 0.0;
    }

    return (totalValue / numOccurrences);
  }



  /**
   * Retrieves brief one-line summary string with cumulative information about
   * this stat tracker.
   *
   * @return  A brief one-line summary string containing cumulative information
   *          about this stat tracker.
   */
  public String getSummaryString()
  {
    return displayName + " -- Occurrences:  " + numOccurrences +
           "; Avg Value:  " + decimalFormat.format(getAverageValue()) +
           "; Total Value:  " + decimalFormat.format(totalValue) +
           "; Avg Time Between Occurrences (ms):  " +
           decimalFormat.format(getAverageTimeBetweenOccurrences());
  }



  /**
   * Retrieves a version of the summary information for this stat tracker
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the summary data for this stat tracker.
   */
  public String getSummaryHTML()
  {
    StringBuilder buffer = new StringBuilder();

    buffer.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    buffer.append("  <TR>" + Constants.EOL);
    buffer.append("    <TD><B>Occurrences</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Avg Value</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Total Value</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Avg Time Between Occurrences</B></TD>" +
                  Constants.EOL);
    buffer.append("  </TR>" + Constants.EOL);
    buffer.append("  <TR>" + Constants.EOL);
    buffer.append("    <TD>" + numOccurrences + "</TD>" + Constants.EOL);
    buffer.append("    <TD>" + decimalFormat.format(getAverageValue()) +
                  "</TD>" + Constants.EOL);
    buffer.append("    <TD>" + decimalFormat.format(totalValue) + "</TD>" +
                  Constants.EOL);
    buffer.append("    <TD>" +
                  decimalFormat.format(getAverageTimeBetweenOccurrences()) +
                  "</TD>" + Constants.EOL);
    buffer.append("  </TR>" + Constants.EOL);
    buffer.append("</TABLE>" + Constants.EOL);

    return buffer.toString();
  }



  /**
   * Retrieves a detailed (potentially multi-line) string with verbose
   * information about the data collected by this stat tracker.
   *
   * @return  A detailed string with verbose information about the data
   *          collected by this stat tracker.
   */
  public String getDetailString()
  {
    StringBuilder buffer = new StringBuilder();
    buffer.append(displayName + Constants.EOL);
    buffer.append("Occurrences:  " + numOccurrences + Constants.EOL);
    buffer.append("Average Value:  " + decimalFormat.format(getAverageValue()) +
                  Constants.EOL);
    buffer.append("Total Value:  " + decimalFormat.format(totalValue) +
                  Constants.EOL);
    buffer.append("Maximum Value:  " + decimalFormat.format(getMaxValue()) +
                  Constants.EOL);
    buffer.append("Minimum Value:  " + decimalFormat.format(getMinValue()) +
                  Constants.EOL);
    buffer.append("Average Time Between Occurrences (ms):  " +
                  decimalFormat.format(getAverageTimeBetweenOccurrences()) +
                  Constants.EOL);

    for (int i=0; i < numOccurrences; i++)
    {
      buffer.append(dateFormat.format(new Date(eventTimes[i])) + ":  " +
                    decimalFormat.format(eventValues[i]) + Constants.EOL);
    }

    return buffer.toString();
  }



  /**
   * Retrieves a version of the verbose information for this stat tracker,
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the verbose data for this stat tracker.
   */
  public String getDetailHTML()
  {
    StringBuilder buffer = new StringBuilder();

    buffer.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    buffer.append("  <TR>" + Constants.EOL);
    buffer.append("    <TD><B>Occurrences</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Avg Value</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Total Value</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Max Value</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Min Value</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Avg Time Between Occurrences</B></TD>" +
                  Constants.EOL);
    buffer.append("  </TR>" + Constants.EOL);
    buffer.append("  <TR>" + Constants.EOL);
    buffer.append("    <TD>" + numOccurrences + "</TD>" + Constants.EOL);
    buffer.append("    <TD>" + decimalFormat.format(getAverageValue()) +
                  "</TD>" + Constants.EOL);
    buffer.append("    <TD>" + decimalFormat.format(totalValue) + "</TD>" +
                  Constants.EOL);
    buffer.append("    <TD>" + decimalFormat.format(getMaxValue()) + "</TD>" +
                  Constants.EOL);
    buffer.append("    <TD>" + decimalFormat.format(getMinValue()) + "</TD>" +
                  Constants.EOL);
    buffer.append("    <TD>" +
                  decimalFormat.format(getAverageTimeBetweenOccurrences()) +
                  "</TD>" + Constants.EOL);
    buffer.append("  </TR>" + Constants.EOL);
    buffer.append("</TABLE>" + Constants.EOL);

    buffer.append("<BR><BR>" + Constants.EOL);

    buffer.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    buffer.append("  <TR>" + Constants.EOL);
    buffer.append("    <TD><B>Event Time</B></TD>" + Constants.EOL);
    buffer.append("    <TD><B>Event Value</B></TD>" + Constants.EOL);
    buffer.append("  </TR>" + Constants.EOL);

    for (int i=0; i < numOccurrences; i++)
    {
      buffer.append("  <TR>" + Constants.EOL);
      buffer.append("    <TD>" + dateFormat.format(new Date(eventTimes[i])) +
                    "</TD>" + Constants.EOL);
      buffer.append("    <TD>" + decimalFormat.format(eventValues[i]) +
                    "</TD>" + Constants.EOL);
      buffer.append("  </TR>" + Constants.EOL);
    }

    buffer.append("</TABLE>" + Constants.EOL);

    return buffer.toString();
  }



  /**
   * Retrieves a string array with the labels corresponding to the values
   * returned from the <CODE>getSummaryData</CODE> method.
   *
   * @return  A string array with the labels corresponding to the values
   *          returned from the <CODE>getSummaryData</CODE> method.
   */
  public String[] getSummaryLabels()
  {
    return new String[]
    {
      "Occurrences",
      "Average Value",
      "Total Value",
      "Average Time Between Occurrences"
    };
  }



  /**
   * Retrieves the summary string data for this stat tracker as separate values.
   *
   * @return  The summary string data for this stat tracker as separate values.
   */
  public String[] getSummaryData()
  {
    return new String[]
    {
      String.valueOf(numOccurrences),
      decimalFormat.format(getAverageValue()),
      decimalFormat.format(totalValue),
      decimalFormat.format(getAverageTimeBetweenOccurrences())
    };
  }



  /**
   * Retrieves the raw data associated with this stat tracker in a form that
   * can be easily converted for export to CSV, tab-delimited text, or some
   * other format for use in an external application.  There should be one value
   * per "cell".
   *
   *
   * @param  includeLabels  Indicates whether the information being exported
   *                        should contain labels.
   *
   * @return  The raw data associated with this stat tracker in a form that can
   *           be exported to some external form.
   */
  public String[][] getDataForExport(boolean includeLabels)
  {
    String[][] returnArray;
    int        arrayPos;
    if (includeLabels)
    {
      returnArray       = new String[numOccurrences+1][2];
      arrayPos          = 1;
      returnArray[0][0] = "Event Time";
      returnArray[0][1] = "Event Value";
    }
    else
    {
      returnArray = new String[numOccurrences][2];
      arrayPos    = 0;
    }

    for (int i=0; i < numOccurrences; i++,arrayPos++)
    {
      returnArray[arrayPos][0] = dateFormat.format(new Date(eventTimes[i]));
      returnArray[arrayPos][1] = decimalFormat.format(eventValues[i]);
    }

    return returnArray;
  }



  /**
   * Encodes the data collected by this tracker into a byte array that may be
   * transferred over the network or written out to persistent storage.
   *
   * @return  The data collected by this tracker encoded as a byte array.
   */
  public byte[] encode()
  {
    ASN1Element[] sequenceElements = new ASN1Element[(numOccurrences*2)+3];

    sequenceElements[0] = new ASN1OctetString(String.valueOf(startTime));
    sequenceElements[1] = new ASN1OctetString(String.valueOf(stopTime));

    ASN1Element[] configElements = new ASN1Element[]
    {
      new ASN1OctetString(Constants.SERVLET_PARAM_BASE_AT_ZERO + "=" +
                          String.valueOf(baseAtZero)),
      new ASN1OctetString(Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS + "=" +
                          String.valueOf(flatBetweenPoints)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_AVERAGE + "=" +
                          String.valueOf(includeAverage)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID + "=" +
                          String.valueOf(includeHorizontalGrid)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID + "=" +
                          String.valueOf(includeVerticalGrid)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_LABELS + "=" +
                          String.valueOf(includeLegend)),
    };

    sequenceElements[2] = new ASN1Sequence(configElements);

    int pos = 3;
    for (int i=0; i < numOccurrences; i++)
    {
      sequenceElements[pos++] =
           new ASN1OctetString(String.valueOf(eventTimes[i]));
      sequenceElements[pos++] =
           new ASN1OctetString(String.valueOf(eventValues[i]));
    }

    return new ASN1Sequence(sequenceElements).encode();
  }



  /**
   * Decodes the provided data and uses it as the data for this stat tracker.
   *
   * @param  encodedData   The encoded version of the data to use for this
   *                       stat tracker.
   *
   * @throws  SLAMDException  If the provided data cannot be decoded and used
   *                          with this stat tracker.
   */
  public void decode(byte[] encodedData)
    throws SLAMDException
  {
    try
    {
      ASN1Element[] elements =
           ASN1Element.decodeAsSequence(encodedData).getElements();
      numOccurrences = (elements.length-3) / 2;
      eventTimes     = new long[numOccurrences];
      eventValues    = new double[numOccurrences];
      totalValue     = 0.0;

      startTime =
           Long.parseLong(elements[0].decodeAsOctetString().getStringValue());
      stopTime =
           Long.parseLong(elements[1].decodeAsOctetString().getStringValue());
      duration = (int) ((stopTime - startTime) / 1000);

      ASN1Element[] configElements =
                         elements[2].decodeAsSequence().getElements();
      for (int i=0; i < configElements.length; i++)
      {
        String elementStr =
             configElements[i].decodeAsOctetString().getStringValue();
        int equalPos = elementStr.indexOf('=');
        String name = elementStr.substring(0, equalPos);
        boolean value = Boolean.valueOf(elementStr.substring(equalPos+1));

        if (name.equals(Constants.SERVLET_PARAM_BASE_AT_ZERO))
        {
          baseAtZero = value;
        }
        else if (name.equals(Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS))
        {
          flatBetweenPoints = value;
        }
        else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_AVERAGE))
        {
          includeAverage = value;
        }
        else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID))
        {
          includeHorizontalGrid = value;
        }
        else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID))
        {
          includeVerticalGrid = value;
        }
        else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_LABELS))
        {
          includeLegend = value;
        }
      }

      for (int i=0,j=3; i < numOccurrences; i++)
      {
        eventTimes[i] =
             Long.parseLong(
                  elements[j++].decodeAsOctetString().getStringValue());
        eventValues[i] =
             Double.parseDouble(
                  elements[j++].decodeAsOctetString().getStringValue());
        totalValue += eventValues[i];
      }
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to decode data for periodic event " +
                               "tracker:  " + e, e);
    }
  }



  /**
   * Retrieves the set of parameters that may be specified to customize the
   * graph that is generated based on the statistical information in the stat
   * trackers.
   *
   * @param  job  The job containing the statistical information to be graphed.
   *
   * @return  The set of parameters that may be used to customize the graph that
   *          is generated.
   */
  public ParameterList getGraphParameterStubs(Job job)
  {
    BooleanParameter baseAtZeroParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO,
                              "Base at Zero",
                              "Indicates whether the lower bound for the " +
                              "graph should be based at zero rather than " +
                              "dynamically calculated from the information " +
                              "contained in the data provided.", baseAtZero);
    BooleanParameter showPointsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_SHOW_POINTS,
                              "Show Data Points",
                              "Indicates whether the individual data points " +
                              "should be depicted on the graph.", true);
    BooleanParameter flatBetweenPointsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS,
                              "Flat Between Data Points",
                              "Indicates whether the data points should be " +
                              "connected using only horizontal and vertical " +
                              "lines or if they should be directly connected.",
                              flatBetweenPoints);
    BooleanParameter includeAverageParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_AVERAGE,
                              "Include Average Line",
                              "Indicates whether the graph generated should " +
                              "include a line that shows the average value " +
                              "for the displayed data set.", includeAverage);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.",
                             includeHorizontalGrid);
    BooleanParameter includeVGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID,
                             "Include Vertical Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include vertical grid lines.",
                             includeVerticalGrid);

    Parameter[] parameters = new Parameter[]
    {
      includeAverageParameter,
      showPointsParameter,
      flatBetweenPointsParameter,
      includeHGridParameter,
      includeVGridParameter,
      baseAtZeroParameter
    };

    return new ParameterList(parameters);
  }



  /**
   * Retrieves the set of parameters that may be specified to customize the
   * graph that is generated based on the resource monitor information in the
   * stat trackers.
   *
   * @param  job  The job containing the resource monitor information to be
   *              graphed.
   *
   * @return  The set of parameters that may be used to customize the graph that
   *          is generated.
   */
  public ParameterList getMonitorGraphParameterStubs(Job job)
  {
    BooleanParameter baseAtZeroParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO,
                              "Base at Zero",
                              "Indicates whether the lower bound for the " +
                              "graph should be based at zero rather than " +
                              "dynamically calculated from the information " +
                              "contained in the data provided.", baseAtZero);
    BooleanParameter showPointsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_SHOW_POINTS,
                              "Show Data Points",
                              "Indicates whether the individual data points " +
                              "should be depicted on the graph.", true);
    BooleanParameter flatBetweenPointsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS,
                              "Flat Between Data Points",
                              "Indicates whether the data points should be " +
                              "connected using only horizontal and vertical " +
                              "lines or if they should be directly connected.",
                              flatBetweenPoints);
    BooleanParameter includeAverageParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_AVERAGE,
                              "Include Average Line",
                              "Indicates whether the graph generated should " +
                              "include a line that shows the average value " +
                              "for the displayed data set.", includeAverage);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.",
                             includeHorizontalGrid);
    BooleanParameter includeVGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID,
                             "Include Vertical Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include vertical grid lines.",
                             includeVerticalGrid);

    Parameter[] parameters = new Parameter[]
    {
      includeAverageParameter,
      showPointsParameter,
      flatBetweenPointsParameter,
      includeHGridParameter,
      includeVGridParameter,
      baseAtZeroParameter
    };

    return new ParameterList(parameters);
  }



  /**
   * Retrieves the set of parameters that may be specified to customize the
   * graph that is generated based on the statistical information in the stat
   * trackers.
   *
   * @param  jobs  The job containing the statistical information to be compared
   *               and graphed.
   *
   * @return  The set of parameters that may be used to customize the graph that
   *          is generated.
   */
  public ParameterList getGraphParameterStubs(Job[] jobs)
  {
    BooleanParameter baseAtZeroParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO,
                              "Base at Zero",
                              "Indicates whether the lower bound for the " +
                              "graph should be based at zero rather than " +
                              "dynamically calculated from the information " +
                              "contained in the data provided.", baseAtZero);
    BooleanParameter showPointsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_SHOW_POINTS,
                              "Show Data Points",
                              "Indicates whether the individual data points " +
                              "should be depicted on the graph.", true);
    BooleanParameter flatBetweenPointsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS,
                              "Flat Between Data Points",
                              "Indicates whether the data points should be " +
                              "connected using only horizontal and vertical " +
                              "lines or if they should be directly connected.",
                              flatBetweenPoints);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.",
                             includeHorizontalGrid);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", includeLegend);
    BooleanParameter includeVGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID,
                             "Include Vertical Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include vertical grid lines.",
                             includeVerticalGrid);

    Parameter[] parameters = new Parameter[]
    {
      includeLegendParameter,
      showPointsParameter,
      flatBetweenPointsParameter,
      includeHGridParameter,
      includeVGridParameter,
      baseAtZeroParameter
    };

    return new ParameterList(parameters);
  }



  /**
   * Retrieves the data that represents the points in a line graph for this
   * stat tracker.    This is only applicable if <CODE>isSearchable</CODE>
   * returns <CODE>true</CODE>.
   *
   * @return  The data that represents the points in a line graph for this stat
   *          tracker, or <CODE>null</CODE> if that data is not available.
   */
  public double[] getGraphData()
  {
    return null;
  }



  /**
   * Retrieves the label that should be included along the vertical axis in a
   * line graph for this stat tracker.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>.
   *
   * @return  The label that should be included along the vertical axis in a
   *          line graph for this stat tracker, or <CODE>null</CODE> if that
   *          data is not applicable.
   */
  public String getAxisLabel()
  {
    return null;
  }



  /**
   * Creates a graph that visually depicts the information in the provided set
   * of stat trackers.  The provided stat trackers must be the of the same type
   * as this stat tracker.
   *
   * @param  job         The job containing the statistical information to be
   *                     graphed.
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   * @param  parameters  The set of parameters that may be used to customize the
   *                     graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createGraph(Job job, int width, int height,
                                   ParameterList parameters)
  {
    boolean showPoints        = true;
    boolean includeHGrid      = false;
    boolean includeVGrid      = false;


    BooleanParameter includeAverageParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_AVERAGE);
    if (includeAverageParameter != null)
    {
      includeAverage = includeAverageParameter.getBooleanValue();
    }

    BooleanParameter showPointsParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_SHOW_POINTS);
    if (showPointsParameter != null)
    {
      showPoints = showPointsParameter.getBooleanValue();
    }

    BooleanParameter flatBetweenPointsParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS);
    if (flatBetweenPointsParameter != null)
    {
      flatBetweenPoints = flatBetweenPointsParameter.getBooleanValue();
    }

    BooleanParameter includeHGridParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID);
    if (includeHGridParameter != null)
    {
      includeHGrid = includeHGridParameter.getBooleanValue();
    }

    BooleanParameter includeVGridParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_VERT_GRID);
    if (includeVGridParameter != null)
    {
      includeVGrid = includeVGridParameter.getBooleanValue();
    }

    BooleanParameter baseAtZeroParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO);
    if (baseAtZeroParameter != null)
    {
      baseAtZero = baseAtZeroParameter.getBooleanValue();
    }

    StatTracker[] trackersToGraph = job.getStatTrackers(displayName);
    if ((trackersToGraph == null) || (trackersToGraph.length == 0))
    {
      return null;
    }

    PeriodicEventTracker tracker =
         (PeriodicEventTracker) trackersToGraph[0].newInstance();
    tracker.aggregate(trackersToGraph);

    double[][] xCoordiantes = new double[1][tracker.numOccurrences];
    for (int i=0; i < tracker.numOccurrences; i++)
    {
      xCoordiantes[0][i] = (tracker.eventTimes[i] - tracker.startTime) / 1000.0;
    }

    double[][] yCoordinates = new double[1][tracker.numOccurrences];
    yCoordinates[0] = tracker.eventValues;

    String graphTitle = displayName + " for job " + job.getJobID();
    StatGrapher grapher = new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setFlatBetweenPoints(flatBetweenPoints);
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIncludeAverage(includeAverage);

    return grapher.generateXYLineGraph(xCoordiantes, yCoordinates,
                                       new String[] { job.getJobID() },
                                       showPoints, true);
  }



  /**
   * Creates a graph that visually depicts the information collected by resource
   * monitors associated with the provided job.
   *
   * @param  job         The job containing the statistical information to be
   *                     graphed.
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   * @param  parameters  The set of parameters that may be used to customize
   *                     the graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createMonitorGraph(Job job, int width, int height,
                                          ParameterList parameters)
  {
    boolean showPoints        = true;
    boolean includeHGrid      = false;
    boolean includeVGrid      = false;


    BooleanParameter includeAverageParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_INCLUDE_AVERAGE);
    if (includeAverageParameter != null)
    {
      includeAverage = includeAverageParameter.getBooleanValue();
    }

    BooleanParameter showPointsParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_SHOW_POINTS);
    if (showPointsParameter != null)
    {
      showPoints = showPointsParameter.getBooleanValue();
    }

    BooleanParameter flatBetweenPointsParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS);
    if (flatBetweenPointsParameter != null)
    {
      flatBetweenPoints = flatBetweenPointsParameter.getBooleanValue();
    }

    BooleanParameter includeHGridParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID);
    if (includeHGridParameter != null)
    {
      includeHGrid = includeHGridParameter.getBooleanValue();
    }

    BooleanParameter includeVGridParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_VERT_GRID);
    if (includeVGridParameter != null)
    {
      includeVGrid = includeVGridParameter.getBooleanValue();
    }

    BooleanParameter baseAtZeroParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO);
    if (baseAtZeroParameter != null)
    {
      baseAtZero = baseAtZeroParameter.getBooleanValue();
    }

    String graphTitle = displayName + " for job " + job.getJobID();
    StatGrapher grapher = new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setFlatBetweenPoints(flatBetweenPoints);
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIncludeAverage(includeAverage);

    StatTracker[] trackersToGraph = job.getResourceStatTrackers(displayName);
    if ((trackersToGraph == null) || (trackersToGraph.length == 0))
    {
      return null;
    }

    PeriodicEventTracker tracker =
         (PeriodicEventTracker) trackersToGraph[0].newInstance();
    tracker.aggregate(trackersToGraph);

    double[][] xCoordiantes = new double[1][tracker.numOccurrences];
    for (int i=0; i < tracker.numOccurrences; i++)
    {
      xCoordiantes[0][i] = (tracker.eventTimes[i] - tracker.startTime) / 1000.0;
    }

    double[][] yCoordinates = new double[1][tracker.numOccurrences];
    yCoordinates[0] = tracker.eventValues;

    return grapher.generateXYLineGraph(xCoordiantes, yCoordinates,
                                       new String[] { job.getJobID() },
                                       showPoints, true);
  }



  /**
   * Creates a graph that visually depicts the information in the provided set
   * of stat trackers.  The provided stat trackers must be the of the same type
   * as this stat tracker.
   *
   * @param  jobs        The job containing the statistical information to be
   *                     compared and graphed.
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   * @param  parameters  The set of parameters that may be used to customize
   *                     the graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createGraph(Job[] jobs, int width, int height,
                                   ParameterList parameters)
  {
    boolean showPoints        = true;
    boolean includeHGrid      = false;
    boolean includeVGrid      = false;


    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter showPointsParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_SHOW_POINTS);
    if (showPointsParameter != null)
    {
      showPoints = showPointsParameter.getBooleanValue();
    }

    BooleanParameter flatBetweenPointsParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_FLAT_BETWEEN_POINTS);
    if (flatBetweenPointsParameter != null)
    {
      flatBetweenPoints = flatBetweenPointsParameter.getBooleanValue();
    }

    BooleanParameter includeHGridParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID);
    if (includeHGridParameter != null)
    {
      includeHGrid = includeHGridParameter.getBooleanValue();
    }

    BooleanParameter includeVGridParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_VERT_GRID);
    if (includeVGridParameter != null)
    {
      includeVGrid = includeVGridParameter.getBooleanValue();
    }

    BooleanParameter baseAtZeroParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO);
    if (baseAtZeroParameter != null)
    {
      baseAtZero = baseAtZeroParameter.getBooleanValue();
    }

    String graphTitle = "Comparison of " + displayName + " Values";
    StatGrapher grapher = new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setFlatBetweenPoints(flatBetweenPoints);
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIncludeLegend(includeLegend, "Job IDs");

    double[][] xCoordinates = new double[jobs.length][];
    double[][] yCoordinates = new double[jobs.length][];
    String[]   jobIDs       = new String[jobs.length];
    for (int i=0; i < jobs.length; i++)
    {
      StatTracker[] trackers = jobs[i].getStatTrackers(displayName);
      if ((trackers == null) || (trackers.length == 0))
      {
        return null;
      }

      PeriodicEventTracker tracker =
          (PeriodicEventTracker) trackers[0].newInstance();
      tracker.aggregate(trackers);

      jobIDs[i]       = jobs[i].getJobID();
      yCoordinates[i] = tracker.eventValues;

      long startTime  = tracker.startTime;
      xCoordinates[i] = new double[tracker.eventTimes.length];
      for (int j=0; j < xCoordinates[i].length; j++)
      {
        xCoordinates[i][j] = (tracker.eventTimes[j] - startTime) / 1000.0;
      }
    }

    return grapher.generateXYLineGraph(xCoordinates, yCoordinates, jobIDs,
                                       showPoints, true);
  }



  /**
   * Creates a graph that visually depicts the information in this stat tracker
   * using all the default settings.
   *
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   *
   * @return  The graph created from this stat tracker.
   */
  public BufferedImage createGraph(int width, int height)
  {
    double[][] xCoordiantes = new double[1][numOccurrences];
    for (int i=0; i < numOccurrences; i++)
    {
      xCoordiantes[0][i] = (eventTimes[i] - startTime) / 1000.0;
    }

    double[][] yCoordinates = new double[1][numOccurrences];
    yCoordinates[0] = eventValues;

    String graphTitle = displayName;
    StatGrapher grapher = new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(true);
    grapher.setFlatBetweenPoints(true);
    grapher.setIncludeHorizontalGrid(true);
    grapher.setIncludeVerticalGrid(true);

    return grapher.generateXYLineGraph(xCoordiantes, yCoordinates,
                                       new String[] { displayName },
                                       true, true);
  }
}

