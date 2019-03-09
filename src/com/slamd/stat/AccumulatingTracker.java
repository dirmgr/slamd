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
import java.util.ArrayList;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.client.Client;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.Job;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class defines a simple stat tracker that can be used to count the number
 * of times a given event occurs.  Unlike the incremental tracker, however, the
 * count is not reset at the beginning of each collection interval.
 *
 *
 * @author   Neil A. Wilson
 */
public class AccumulatingTracker
       implements StatTracker
{
  // The list that contains the data collected by this tracker, broken up into
  // intervals.
  private ArrayList<Integer> totalList;

  // Indicates whether to enable real-time statistics reporting.
  private boolean enableRealTimeStats;

  // Indicates whether this stat tracker has been started.
  private boolean hasBeenStarted;

  // Indicates whether this stat tracker is currently running.
  private boolean isRunning;

  // The formatter used to round off decimal values.
  private DecimalFormat decimalFormat;

  // The current interval number when reporting real-time stats.
  private int intervalNum;

  // The total number of occurrences so far since the call to startTracker.
  private int totalCount;

  // The collection interval in seconds.
  private int collectionInterval;

  // The length of time in seconds that this tracker was collecting statistics.
  private int duration;

  // The time that the current collection interval should stop.
  private long intervalStopTime;

  // The time that startTracker was called.
  private long startTime;

  // The time that stopTracker was called.
  private long stopTime;

  // The stat reporter used to report real-time stats.
  private RealTimeStatReporter statReporter;

  // The client ID of the client that used this stat tracker.
  private String clientID;

  // The display name to use for this stat tracker.
  private String displayName;

  // The thread ID of the client thread that used this stat tracker.
  private String threadID;



  /**
   * Creates a new accumulating tracker intended for use as a placeholder for
   * decoding purposes.  This version of the constructor should not be used
   * by job classes.
   */
  public AccumulatingTracker()
  {
    this.clientID           = "";
    this.threadID           = "";
    this.displayName        = "";
    this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;

    decimalFormat       = new DecimalFormat("0.000");
    totalCount          = 0;
    intervalStopTime    = 0;
    startTime           = System.currentTimeMillis();
    stopTime            = 0;
    duration            = 0;
    totalList           = new ArrayList<Integer>();
    enableRealTimeStats = false;
    statReporter        = null;
    intervalNum         = 0;
    hasBeenStarted      = false;
    isRunning           = false;
  }




  /**
   * Creates a new accumulating tracker with the specified information.
   *
   * @param  clientID            The client ID of the client that used this
   *                             stat tracker.
   * @param  threadID            The thread ID of the thread that used this
   *                             stat tracker.
   * @param  displayName         The display name to use for this stat tracker.
   * @param  collectionInterval  The collection interval in seconds that
   *                             should be used for this stat tracker.
   */
  public AccumulatingTracker(String clientID, String threadID,
                             String displayName, int collectionInterval)
  {
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.displayName        = displayName;
    this.collectionInterval = collectionInterval;

    if (collectionInterval <= 0)
    {
      this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
    }

    decimalFormat       = new DecimalFormat("0.000");
    totalCount          = 0;
    intervalStopTime    = 0;
    startTime           = System.currentTimeMillis();
    stopTime            = 0;
    duration            = 0;
    totalList           = new ArrayList<Integer>();
    enableRealTimeStats = false;
    statReporter        = null;
    intervalNum         = 0;
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
    return new AccumulatingTracker(clientID, threadID, displayName,
                                   collectionInterval);
  }



  /**
   * Increments the counter to indicated that the event we are tracking has
   * occurred.
   */
  public void increment()
  {
    long now = System.currentTimeMillis();

    // We're in the same interval as the last time the counter was incremented.
    // Just increment it again.
    if (now < intervalStopTime)
    {
      totalCount++;
    }


    // The previous interval has stopped and a new one has started.  Close out
    // the old one and start the new.  Note that if this is an event that
    // happens infrequently, then multiple intervals could have passed, so make
    // sure that the appropriate number of intervals are added.
    else
    {
      totalList.add(totalCount);
      intervalStopTime += (1000 * collectionInterval);
      if (enableRealTimeStats)
      {
        statReporter.reportStatToAdd(this, intervalNum++, totalCount);
      }

      while (intervalStopTime < now)
      {
        totalList.add(totalCount);
        if (enableRealTimeStats)
        {
          statReporter.reportStatToAdd(this, intervalNum++, totalCount);
        }

        intervalStopTime += (1000 * collectionInterval);
      }

      totalCount++;
    }
  }



  /**
   * Reverts the last increment performed using this tracker.  Note that this
   * method should not be used multiple times between calls of the
   * <CODE>increment()</CODE> method.
   */
  public void undoLastIncrement()
  {
    if (totalCount > 0)
    {
      totalCount--;
    }
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
                         "accumulating stat tracker " + displayName);
    }
    else
    {
      hasBeenStarted = true;
      isRunning = true;
    }

    // Just in case, reset all the counter info.
    totalCount     = 0;
    totalList      = new ArrayList<Integer>();
    intervalNum    = 0;

    // Register this tracker with the persistence thread.
    Client.registerPersistentStatistic(this);

    // Set the start time and the interval stop time.
    long now = System.currentTimeMillis();
    startTime = now;
    intervalStopTime = now + (1000 * collectionInterval);
  }



  /**
   * Indicates that the stat tracker that there will not be any more statistics
   * collection done and that it should stop its internal timer.
   */
  public void stopTracker()
  {
    long now = System.currentTimeMillis();


    // If the tracker was never started, then print an error message.
    // Otherwise, indicate that it is no longer running.
    if (! hasBeenStarted)
    {
      System.err.println("***** WARNING:  Attempting to stop accumulating " +
                         "stat tracker " + displayName +
                         " without having started it");
    }
    else
    {
      isRunning = false;
    }


    // If the previous interval had passed since the last update, make sure
    // that we add the appropriate number of empty intervals.
    if (intervalStopTime < now)
    {
      totalList.add(totalCount);
      if (enableRealTimeStats)
      {
        statReporter.reportStatToAdd(this, intervalNum++, totalCount);
      }

      intervalStopTime += (1000 * collectionInterval);
    }

    while (intervalStopTime < now)
    {
      totalList.add(totalCount);
      if (enableRealTimeStats)
      {
        statReporter.reportStatToAdd(this, intervalNum++, totalCount);
      }

      intervalStopTime += (1000 * collectionInterval);
    }

    // Update the stop time to be the time that the last complete interval
    // ended and calculate the duration.
    stopTime = intervalStopTime - (1000 * collectionInterval);
    duration = (int) ((stopTime - startTime) / 1000);

    if (enableRealTimeStats)
    {
      statReporter.doneReporting(this, intervalNum++);
    }
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
    if (statReporter != null)
    {
      enableRealTimeStats = true;
      this.statReporter   = statReporter;
      statReporter.registerStat(jobID, this);
    }
  }



  /**
   * Retrieves the total number of times that the tracked event occurred over
   * the total duration.
   *
   * @return  The total number of times that the tracked event occurred over the
   *          total duration.
   */
  public int getTotalCount()
  {
    return totalCount;
  }



  /**
   * Retrieves an array containing the accumulated totals by interval.
   *
   * @return  An array containing the accumulated totals by interval.
   */
  public int[] getTotalsByInterval()
  {
    int[] intValues = new int[totalList.size()];

    for (int i=0; i < intValues.length; i++)
    {
      intValues[i] = totalList.get(i);
    }

    return intValues;
  }



  /**
   * Specifies the data for this stat tracker in the form of an array containing
   * the accumulated totals by interval.
   *
   * @param  intervalTotals  An array containing the accumulated totals by
   *                         interval.
   */
  public void setTotalsByInterval(int[] intervalTotals)
  {
    totalCount = intervalTotals[intervalTotals.length-1];

    totalList = new ArrayList<Integer>(intervalTotals.length);
    for (int i=0; i < intervalTotals.length; i++)
    {
      totalList.add(i);
    }

    duration = intervalTotals.length * collectionInterval;
  }



  /**
   * Retrieves the average number of times the tracked event occurred in a
   * single interval.
   *
   * @return  The average number of times the tracked event occurred in a single
   *          interval.
   */
  public double getAverageCountPerInterval()
  {
    if (! totalList.isEmpty())
    {
      return (1.0 * totalCount / totalList.size());
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves the average number of times the tracked event occurred in a
   * single second.
   *
   * @return  The average number of times the tracked event occurred in a single
   *          second.
   */
  public double getAverageCountPerSecond()
  {
    if (duration > 0)
    {
      return (1.0 * totalCount / duration);
    }
    else
    {
      return 0;
    }
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
   * Specifies the collection interval in seconds to use fort this stat tracker.
   * This should not be used while the stat tracker is actively collecting
   * statistics.
   *
   * @param  collectionInterval  The collection interval in seconds to use for
   *                             this stat tracker.
   */
  public void setCollectionInterval(int collectionInterval)
  {
    if (collectionInterval > 0)
    {
      this.collectionInterval = collectionInterval;
    }
    else
    {
      this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
    }
  }



  /**
   * Retrieves the total length of time in seconds that this stat tracker was
   * capturing statistics.
   *
   * @return  The total length of time in seconds that this stat tracker was
   *
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
    return true;
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
    return (totalCount >= value);
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
    return (totalCount <= value);
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
    return totalCount;
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
    return totalList.size();
  }



  /**
   * Aggregates the information collected by the provided set of stat trackers
   * into a single tracker that represents the information gathered from the
   * entire set of data.  All of the stat trackers in the provided array must be
   * of the same type and have the same collection interval as the instance into
   * which the information will be aggregated.
   *
   * @param  trackers  The set of stat trackers whose data is to be aggregated.
   */
  public void aggregate(StatTracker[] trackers)
  {
    totalCount     = 0;
    duration       = Integer.MAX_VALUE;

    if (trackers.length > 0)
    {
      collectionInterval = trackers[0].getCollectionInterval();
    }
    else
    {
      return;
    }

    int min = Integer.MAX_VALUE;
    int[][] totals = new int[trackers.length][];
    for (int i=0; i < totals.length; i++)
    {
      totals[i] = ((AccumulatingTracker) trackers[i]).getTotalsByInterval();
      if (totals[i].length < min)
      {
         min = totals[i].length;
      }

      if (trackers[i].getDuration() < duration)
      {
        duration = trackers[i].getDuration();
      }
    }

    int[] aggregateCounts = new int[min];
    for (int i=0; i < aggregateCounts.length; i++)
    {
      aggregateCounts[i] = 0;

      for (int j=0; j < totals.length; j++)
      {
        aggregateCounts[i] += totals[j][i];
      }

      totalCount = aggregateCounts[i];
    }

    totalList = new ArrayList<Integer>(aggregateCounts.length);
    for (int i=0; i < aggregateCounts.length; i++)
    {
      totalList.add(aggregateCounts[i]);
    }
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
    return displayName + " -- Total Count:  " + totalCount +
           ";  Avg Count/Second:  " +
           decimalFormat.format(getAverageCountPerSecond()) +
           ";  Avg Count/Interval:  " +
           decimalFormat.format(getAverageCountPerInterval());

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
    StringBuilder returnBuffer = new StringBuilder();
    returnBuffer.append(displayName + Constants.EOL);
    returnBuffer.append("Total Count:  " + totalCount + Constants.EOL);
    returnBuffer.append("Average Count/Second:  " +
                        decimalFormat.format(getAverageCountPerSecond()) +
                        Constants.EOL);
    returnBuffer.append("Average Count/Interval:  " +
                        decimalFormat.format(getAverageCountPerInterval()) +
                        Constants.EOL);

    for (int i=0; i < totalList.size(); i++)
    {
      returnBuffer.append("Interval ");
      returnBuffer.append(i+1);
      returnBuffer.append(":  ");
      returnBuffer.append(totalList.get(i));
      returnBuffer.append(Constants.EOL);
    }

    return returnBuffer.toString();
  }



  /**
   * Retrieves a version of the summary information for this stat tracker
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the summary data for this stat tracker.
   */
  public String getSummaryHTML()
  {
    StringBuilder html = new StringBuilder();
    html.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD><B>Total Count</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Count/Second</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Count/Interval</B></TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD>" + totalCount + "</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getAverageCountPerSecond()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" +
                decimalFormat.format(getAverageCountPerInterval()) + "</TD>" +
                Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("</TABLE>" + Constants.EOL);

    return html.toString();
  }



  /**
   * Retrieves a version of the verbose information for this stat tracker,
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the verbose data for this stat tracker.
   */
  public String getDetailHTML()
  {
    StringBuilder html = new StringBuilder();
    html.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD><B>Total Count</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Count/Second</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Count/Interval</B></TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD>" + totalCount + "</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getAverageCountPerSecond()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" +
                decimalFormat.format(getAverageCountPerInterval()) + "</TD>" +
                Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("</TABLE>" + Constants.EOL);

    html.append("<BR><BR>" + Constants.EOL);

    html.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD><B>Interval</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Accumulated Total</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Count/Second</B></TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);

    int lastValue = 0;
    for (int i=0; i < totalList.size(); i++)
    {
      int currentValue = totalList.get(i);

      html.append("  <TR>" + Constants.EOL);
      html.append("    <TD>" + (i+1) + "</TD>" + Constants.EOL);
      html.append("    <TD>" + currentValue + "</TD>" + Constants.EOL);
      html.append("    <TD>" +
                  decimalFormat.format(1.0 * (currentValue-lastValue) /
                                       collectionInterval) + "</TD>" +
                  Constants.EOL);
      html.append("  </TR>" + Constants.EOL);

      lastValue = currentValue;
    }

    html.append("</TABLE>" + Constants.EOL);

    return html.toString();
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
      displayName + " Total Count",
      displayName + " Avg Count/Second",
      displayName + " Avg Count/Interval"
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
      String.valueOf(totalCount),
      decimalFormat.format(getAverageCountPerSecond()),
      decimalFormat.format(getAverageCountPerInterval())
    };
  }



  /**
   * Retrieves the raw data associated with this stat tracker in a form that
   * can be easily converted for export to CSV, tab-delimited text, or some
   * other format for use in an external application.  There should be one value
   * per "cell".
   *
   * @param  includeLabels  Indicates whether the information being exported
   *                        should contain labels.
   *
   * @return  The raw data associated with this stat tracker in a form that can
   *           be exported to some external form.
   */
  public String[][] getDataForExport(boolean includeLabels)
  {
    if (includeLabels)
    {
      String[][] returnArray = new String[totalList.size()+1][];

      returnArray[0] = new String[] { "Interval", "Accumulated Total" };
      for (int i=0; i < totalList.size(); i++)
      {
        returnArray[i+1] = new String[]
        {
          String.valueOf(i+1),
          String.valueOf(totalList.get(i))
        };
      }

      return returnArray;
    }
    else
    {
      String[][] returnArray = new String[totalList.size()][];

      for (int i=0; i < totalList.size(); i++)
      {
        returnArray[i] = new String[]
        {
          String.valueOf(totalList.get(i))
        };
      }

      return returnArray;
    }
  }



  /**
   * Encodes the data collected by this tracker into a byte array that may be
   * transferred over the network or written out to persistent storage.
   *
   * @return  The data collected by this tracker encoded as a byte array.
   */
  public byte[] encode()
  {
    ASN1Element[] elements = new ASN1Element[totalList.size()];

    for (int i=0; i < elements.length; i++)
    {
      elements[i] = new ASN1Integer(totalList.get(i));
    }

    return new ASN1Sequence(elements).encode();
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
           ASN1Element.decode(encodedData).decodeAsSequence().getElements();

      totalList  = new ArrayList<Integer>(elements.length);
      totalCount   = 0;

      for (int i=0; i < elements.length; i++)
      {
        int accumulatedTotal = elements[i].decodeAsInteger().getIntValue();
        totalList.add(accumulatedTotal);
        totalCount = accumulatedTotal;
      }
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to decode data:  " + e, e);
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
    ArrayList<String> dataSetList = new ArrayList<String>();
    dataSetList.add("Overall Summary for Job " + job.getJobID());
    dataSetList.add("Summary Statistics Per Client");
    String[] clientIDs = job.getStatTrackerClientIDs();
    for (int i=0; i < clientIDs.length; i++)
    {
      dataSetList.add("Detail Statistics for Client " + clientIDs[i]);
    }
    String[] dataSets = new String[dataSetList.size()];
    dataSetList.toArray(dataSets);


    BooleanParameter baseAtZeroParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO,
                              "Base at Zero",
                              "Indicates whether the lower bound for the " +
                              "graph should be based at zero rather than " +
                              "dynamically calculated from the information " +
                              "contained in the data provided.", true);
    BooleanParameter excludeFirstIntervalParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL,
                              "Exclude First Interval",
                              "Indicates whether information captured during " +
                              "the first collection interval should be " +
                              "ignored while generating the graph.", false);
    BooleanParameter excludeLastIntervalParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_EXCLUDE_LAST_INTERVAL,
                              "Exclude Last Interval",
                              "Indicates whether information captured during " +
                              "the last collection interval should be " +
                              "ignored while generating the graph.", false);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.", true);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", false);
    BooleanParameter includeVGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID,
                             "Include Vertical Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include vertical grid lines.", true);
    MultiChoiceParameter detailLevelParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_DETAIL_LEVEL,
                                  "Data Set to Display",
                                  "Indicates the data that should be " +
                                  "displayed in the graph (overall for the " +
                                  "job, a summary for the client, or detail " +
                                  "for a particular client thread.",
                                  dataSets, dataSets[0]);

    Parameter[] parameters = new Parameter[]
    {
      detailLevelParameter,
      includeLegendParameter,
      excludeFirstIntervalParameter,
      excludeLastIntervalParameter,
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
                              "contained in the data provided.", true);
    BooleanParameter excludeFirstIntervalParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL,
                              "Exclude First Interval",
                              "Indicates whether information captured during " +
                              "the first collection interval should be " +
                              "ignored while generating the graph.", false);
    BooleanParameter excludeLastIntervalParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_EXCLUDE_LAST_INTERVAL,
                              "Exclude Last Interval",
                              "Indicates whether information captured during " +
                              "the last collection interval should be " +
                              "ignored while generating the graph.", false);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.", true);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", false);
    BooleanParameter includeVGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID,
                             "Include Vertical Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include vertical grid lines.", true);

    Parameter[] parameters = new Parameter[]
    {
      includeLegendParameter,
      excludeFirstIntervalParameter,
      excludeLastIntervalParameter,
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
                              "contained in the data provided.", true);
    BooleanParameter excludeFirstIntervalParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL,
                              "Exclude First Interval",
                              "Indicates whether information captured during " +
                              "the first collection interval should be " +
                              "ignored while generating the graph.", false);
    BooleanParameter excludeLastIntervalParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_EXCLUDE_LAST_INTERVAL,
                              "Exclude Last Interval",
                              "Indicates whether information captured during " +
                              "the last collection interval should be " +
                              "ignored while generating the graph.", false);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", false);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.", true);
    BooleanParameter includeVGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID,
                             "Include Vertical Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include vertical grid lines.", true);

    Parameter[] parameters = new Parameter[]
    {
      includeLegendParameter,
      excludeFirstIntervalParameter,
      excludeLastIntervalParameter,
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
    int[] intervalTotals = getTotalsByInterval();
    double[] doubleTotals = new double[intervalTotals.length];
    for (int i=0; i < doubleTotals.length; i++)
    {
      doubleTotals[i] = intervalTotals[i];
    }

    return doubleTotals;
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
    return "Accumulated Total";
  }



  /**
   * Creates a graph that visually depicts the information in the provided set
   * of stat trackers.  The provided stat trackers must be of the same type as
   * this stat tracker.
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
    boolean includeLegend     = false;
    boolean excludeFirst      = false;
    boolean excludeLast       = false;
    boolean includeHGrid      = false;
    boolean includeVGrid      = false;
    boolean baseAtZero        = false;
    String  detailLevelStr    = "Overall Summary for Job " + job.getJobID();
    String  graphTitle        = displayName + " for Job " + job.getJobID();
    String  legendTitle       = "Job ID";


    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter excludeFirstParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL);
    if (excludeFirstParameter != null)
    {
      excludeFirst = excludeFirstParameter.getBooleanValue();
    }

    BooleanParameter excludeLastParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_EXCLUDE_LAST_INTERVAL);
    if (excludeLastParameter != null)
    {
      excludeLast = excludeLastParameter.getBooleanValue();
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

    MultiChoiceParameter detailLevelParameter =
         parameters.getMultiChoiceParameter(
                         Constants.SERVLET_PARAM_DETAIL_LEVEL);
    if (detailLevelParameter != null)
    {
      detailLevelStr = detailLevelParameter.getStringValue();
    }

    StatTracker[] trackersToGraph = new StatTracker[0];
    int statCategory = Constants.STAT_CATEGORY_JOB_STATS;
    if (detailLevelStr.equals("Summary Statistics Per Client"))
    {
      statCategory = Constants.STAT_CATEGORY_CLIENT_STATS;
      graphTitle   = displayName + " by Client";
      legendTitle  = "Client ID";
      String[] clientIDs = job.getStatTrackerClientIDs();
      ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
      for (int i=0; i < clientIDs.length; i++)
      {
        StatTracker[] trackers = job.getStatTrackers(displayName, clientIDs[i]);
        if ((trackers != null) && (trackers.length > 0))
        {
          AccumulatingTracker tracker =
               new AccumulatingTracker(clientIDs[i], "", displayName,
                                       trackers[0].getCollectionInterval());
          tracker.aggregate(trackers);
          trackerList.add(tracker);
        }
      }

      trackersToGraph = new StatTracker[trackerList.size()];
      trackerList.toArray(trackersToGraph);
    }
    else if (detailLevelStr.startsWith("Detail Statistics for Client "))
    {
      statCategory = Constants.STAT_CATEGORY_THREAD_STATS;
      String clientID = detailLevelStr.substring(29);
      graphTitle  = displayName + " by Thread for Client " + clientID;
      legendTitle = "Thread ID";
      trackersToGraph = job.getStatTrackers(displayName, clientID);
    }
    else
    {
      StatTracker[] trackers = job.getStatTrackers(displayName);
      AccumulatingTracker tracker =
           new AccumulatingTracker("", "", displayName,
                                  trackers[0].getCollectionInterval());
      tracker.aggregate(trackers);
      trackersToGraph = new StatTracker[] { tracker };
    }

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setIncludeLegend(includeLegend, legendTitle);
    grapher.setVerticalAxisTitle("Accumulated Total");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < trackersToGraph.length; i++)
    {
      AccumulatingTracker tracker = (AccumulatingTracker) trackersToGraph[i];

      int[] intervalTotals = tracker.getTotalsByInterval();
      int numElements = intervalTotals.length;
      int j = 0;
      int subtractor = 0;
      int lastIndex  = intervalTotals.length;
      if (excludeFirst)
      {
        numElements--;
        j = 1;
        subtractor = 1;
      }
      if (excludeLast)
      {
        numElements--;
        lastIndex--;
      }

      double[] graphTotals = new double[numElements];
      for ( ; j < lastIndex; j++)
      {
        graphTotals[j-subtractor] = intervalTotals[j];
      }

      String label;
      switch (statCategory)
      {
        case Constants.STAT_CATEGORY_CLIENT_STATS:
             label = trackersToGraph[i].getClientID();
             break;
        case Constants.STAT_CATEGORY_THREAD_STATS:
             label = trackersToGraph[i].getThreadID();
             break;
        default:
             label = "Job " + job.getJobID();
             break;
      }

      grapher.addDataSet(graphTotals, job.getCollectionInterval(), label);
    }

    return grapher.generateLineGraph();
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
    boolean excludeFirst  = false;
    boolean excludeLast   = false;
    boolean includeLegend = false;
    boolean includeHGrid  = false;
    boolean includeVGrid  = false;
    boolean baseAtZero    = false;
    String  graphTitle    = displayName;


    BooleanParameter excludeFirstParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL);
    if (excludeFirstParameter != null)
    {
      excludeFirst = excludeFirstParameter.getBooleanValue();
    }

    BooleanParameter excludeLastParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_EXCLUDE_LAST_INTERVAL);
    if (excludeLastParameter != null)
    {
      excludeLast = excludeLastParameter.getBooleanValue();
    }

    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
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

    StatTracker[] trackers = job.getResourceStatTrackers(displayName);
    AccumulatingTracker tracker =
         new AccumulatingTracker("", "", displayName,
                                 trackers[0].getCollectionInterval());
    tracker.aggregate(trackers);
    StatTracker[] trackersToGraph = new StatTracker[] { tracker };

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setIncludeLegend(includeLegend, "Job ID");
    grapher.setVerticalAxisTitle("Accumulated Total");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < trackersToGraph.length; i++)
    {
      tracker = (AccumulatingTracker) trackersToGraph[i];

      int[] intervalTotals = tracker.getTotalsByInterval();
      int numElements = intervalTotals.length;
      int j = 0;
      int subtractor = 0;
      int lastIndex  = intervalTotals.length;
      if (excludeFirst)
      {
        numElements--;
        j = 1;
        subtractor = 1;
      }
      if (excludeLast)
      {
        numElements--;
        lastIndex--;
      }

      double[] graphTotals = new double[numElements];
      for ( ; j < lastIndex; j++)
      {
        graphTotals[j-subtractor] = intervalTotals[j];
      }

      grapher.addDataSet(graphTotals, job.getCollectionInterval(),
                         "Job " + job.getJobID());
    }

    return grapher.generateLineGraph();
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
   * @param  parameters  The set of parameters that may be used to customize the
   *                     graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createGraph(Job[] jobs, int width, int height,
                                   ParameterList parameters)
  {
    boolean drawAsBarGraph    = false;
    boolean includeLegend     = false;
    boolean excludeFirst      = false;
    boolean excludeLast       = false;
    boolean includeHGrid      = false;
    boolean includeVGrid      = false;
    boolean baseAtZero        = false;
    String  graphTitle        = "Comparison of " + displayName;


    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter drawAsBarParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH);
    if (drawAsBarParameter != null)
    {
      drawAsBarGraph = drawAsBarParameter.getBooleanValue();
    }

    BooleanParameter excludeFirstParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL);
    if (excludeFirstParameter != null)
    {
      excludeFirst = excludeFirstParameter.getBooleanValue();
    }

    BooleanParameter excludeLastParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_EXCLUDE_LAST_INTERVAL);
    if (excludeLastParameter != null)
    {
      excludeLast = excludeLastParameter.getBooleanValue();
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

    StatTracker[] trackersToGraph = new StatTracker[jobs.length];
    for (int i=0; i < jobs.length; i++)
    {
      StatTracker[] jobTrackers = jobs[i].getStatTrackers(displayName);
      AccumulatingTracker jobTracker =
           new AccumulatingTracker(null, null, displayName,
                                   jobTrackers[0].getCollectionInterval());
      jobTracker.aggregate(jobTrackers);
      trackersToGraph[i] = jobTracker;
    }

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setIncludeLegend(includeLegend, "Job");
    grapher.setVerticalAxisTitle("Accumulated Total");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < trackersToGraph.length; i++)
    {
      AccumulatingTracker tracker = (AccumulatingTracker) trackersToGraph[i];

      int[] intervalTotals = tracker.getTotalsByInterval();
      int numElements = intervalTotals.length;
      int j = 0;
      int subtractor = 0;
      int lastIndex  = intervalTotals.length;
      if (excludeFirst)
      {
        numElements--;
        j = 1;
        subtractor = 1;
      }
      if (excludeLast)
      {
        numElements--;
        lastIndex--;
      }

      double[] graphTotals = new double[numElements];
      for ( ; j < lastIndex; j++)
      {
        graphTotals[j-subtractor] = intervalTotals[j];
      }

      String label = jobs[i].getJobDescription();
      if ((label == null) || (label.length() == 0))
      {
        label = jobs[i].getJobID();
      }
      grapher.addDataSet(graphTotals, jobs[i].getCollectionInterval(), label);
    }

    if (drawAsBarGraph)
    {
      return grapher.generateBarGraph();
    }
    else
    {
      return grapher.generateLineGraph();
    }
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
    StatGrapher grapher = new StatGrapher(width, height, displayName);
    grapher.setBaseAtZero(true);
    grapher.setIncludeHorizontalGrid(true);
    grapher.setIncludeVerticalGrid(true);
    grapher.setVerticalAxisTitle("Accumulated Total");
    grapher.setIncludeLegend(false, "");
    grapher.setIgnoreZeroValues(false);

    int[]    intervalTotals = getTotalsByInterval();
    double[] graphTotals    = new double[intervalTotals.length];
    for (int i=0; i< intervalTotals.length; i++)
    {
      graphTotals[i] = intervalTotals[i];
    }

    grapher.addDataSet(graphTotals, collectionInterval, displayName);
    return grapher.generateLineGraph();
  }
}

