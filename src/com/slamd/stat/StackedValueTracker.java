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
 * This class defines a stat tracker that can be used to report the relative
 * ratios of multiple related statistics.
 *
 *
 * @author   Neil A. Wilson
 */
public class StackedValueTracker
       implements StatTracker
{
  // The array list that will hold the number of values in each interval.
  private ArrayList<Integer> countList;

  // The array list that will hold the set of values for this tracker.
  private ArrayList<double[]> valueList;

  // Indicates whether the generated graph should be stacked or overlay.
  private boolean drawAsStackedGraph;

  // Indicates whether this stat tracker has been started.
  private boolean hasBeenStarted;

  // Indicates whether to include the legend in the generated graph.
  private boolean includeLegend;

  // Indicates whether to include horizontal grid lines in the generated graph.
  private boolean includeHorizontalGrid;

  // Indicates whether to include vertical grid lines in the generated graph.
  private boolean includeVerticalGrid;

  // Indicates whether this stat tracker is currently running.
  private boolean isRunning;

  // The formatter used to round off decimal values.
  private DecimalFormat decimalFormat;

  // The categorized totals maintained for the current interval.
  private double[] intervalTotals;

  // The collection interval for this stat tracker.
  private int collectionInterval;

  // The length of time this tracker was active.
  private int duration;

  // The number of data points collected for the current interval.
  private int intervalCount;

  // The time that the current interval should end and the next should start.
  private long intervalStopTime;

  // The time that this tracker started.
  private long startTime;

  // The time that this tracker stopped.
  private long stopTime;

  // The client ID for this tracker.
  private String clientID;

  // The display name for this tracker.
  private String displayName;

  // The thread ID for this tracker.
  private String threadID;

  // The names of the categories associated with this tracker.
  private String[] categoryNames;



  /**
   * Creates a new stacked value tracker intended for use as a placeholder for
   * decoding purposes.  This version of the constructor should not be used
   * by job classes.
   */
  public StackedValueTracker()
  {
    this.clientID           = "";
    this.threadID           = "";
    this.displayName        = "";
    this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;

    drawAsStackedGraph    = false;
    includeLegend         = false;
    includeHorizontalGrid = true;
    includeVerticalGrid   = true;
    decimalFormat         = new DecimalFormat("0.000");
    countList             = new ArrayList<Integer>();
    valueList             = new ArrayList<double[]>();
    intervalTotals        = new double[0];
    intervalCount         = 0;
    intervalStopTime      = 0;
    startTime             = System.currentTimeMillis();
    stopTime              = 0;
    duration              = 0;
    categoryNames         = new String[0];
    hasBeenStarted        = false;
    isRunning             = false;
  }



  /**
   * Creates a new stacked value tracker with the specified information.
   *
   * @param  clientID            The client ID of the client that used this
   *                             stat tracker.
   * @param  threadID            The thread ID of the thread that used this
   *                             stat tracker.
   * @param  displayName         The display name to use for this stat tracker.
   * @param  collectionInterval  The collection interval in seconds that
   *                             should be used for this stat tracker.
   * @param  categoryNames       The names of the categories associated with
   *                             this stat tracker.
   */
  public StackedValueTracker(String clientID, String threadID,
                             String displayName, int collectionInterval,
                             String[] categoryNames)
  {
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.displayName        = displayName;
    this.collectionInterval = collectionInterval;
    this.categoryNames      = categoryNames;

    drawAsStackedGraph    = false;
    includeLegend         = false;
    includeHorizontalGrid = true;
    includeVerticalGrid   = true;
    decimalFormat         = new DecimalFormat("0.000");
    countList             = new ArrayList<Integer>();
    valueList             = new ArrayList<double[]>();
    intervalTotals        = new double[categoryNames.length];
    intervalCount         = 0;
    intervalStopTime      = 0;
    startTime             = System.currentTimeMillis();
    stopTime              = 0;
    duration              = 0;
    hasBeenStarted        = false;
    isRunning             = false;
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
    StackedValueTracker tracker =
         new StackedValueTracker(clientID, threadID, displayName,
                                 collectionInterval, categoryNames);

    tracker.setDrawAsStackedGraph(drawAsStackedGraph);
    tracker.setIncludeLegend(includeLegend);
    tracker.setIncludeHorizontalGrid(includeHorizontalGrid);
    tracker.setIncludeVerticalGrid(includeVerticalGrid);

    return tracker;
  }



  /**
   * Adds data to this stat tracker.
   *
   * @param  values  The array of values to use for this tracker, with one
   *                 element per category name.
   */
  public void addData(double[] values)
  {
    long now = System.currentTimeMillis();

    // We're in the same interval as the last time the counter was incremented.
    // Just increment it again.
    if (now < intervalStopTime)
    {
      for (int i=0; i < values.length; i++)
      {
        intervalTotals[i] += values[i];
      }

      intervalCount++;
    }


    // The previous interval has stopped and a new one has started.  Close out
    // the old one and start the new.  Note that if this is an event that
    // happens infrequently, then multiple intervals could have passed, so make
    // sure that the appropriate number of intervals are added.
    else
    {
      countList.add(intervalCount);
      valueList.add(intervalTotals);
      intervalTotals = values;
      intervalCount  = 1;

      intervalStopTime += (1000 * collectionInterval);

      while (intervalStopTime < now)
      {
        valueList.add(new double[categoryNames.length]);
        countList.add(0);
        intervalStopTime += (1000 * collectionInterval);
      }
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
                         "stacked value stat tracker " + displayName);
    }
    else
    {
      hasBeenStarted = true;
      isRunning = true;
    }

    // Just in case, reset all the counter info.
    countList      = new ArrayList<Integer>();
    valueList      = new ArrayList<double[]>();
    intervalTotals = new double[categoryNames.length];
    intervalCount  = 0;

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
      System.err.println("***** WARNING:  Attempting to stop stacked value " +
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
      valueList.add(intervalTotals);
      countList.add(intervalCount);
    }

    while (intervalStopTime < now)
    {
      countList.add(0);
      valueList.add(new double[categoryNames.length]);
      intervalStopTime += (1000 * collectionInterval);
    }

    // Update the stop time to be the time that the last complete interval
    // ended and calculate the duration.
    stopTime = intervalStopTime - (1000 * collectionInterval);
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
    // No implementation required.  Categorical trackers do not support
    // real-time statistics collection.
  }



  /**
   * Indicates whether the values in the generated graph should be stacked.
   *
   * @param  drawAsStackedGraph  Indicates whether the values in the generated
   *                             graph should be stacked.
   */
  public void setDrawAsStackedGraph(boolean drawAsStackedGraph)
  {
    this.drawAsStackedGraph = drawAsStackedGraph;
  }



  /**
   * Indicates the generated graph should include a legend.
   *
   * @param  includeLegend  Indicates whether the generated graph should include
   *                        a legend.
   */
  public void setIncludeLegend(boolean includeLegend)
  {
    this.includeLegend = includeLegend;
  }



  /**
   * Indicates the generated graph should include horizontal grid lines.
   *
   * @param  includeHorizontalGrid  Indicates whether the generated graph should
   *                                include horizontal grid lines.
   */
  public void setIncludeHorizontalGrid(boolean includeHorizontalGrid)
  {
    this.includeHorizontalGrid = includeHorizontalGrid;
  }



  /**
   * Indicates the generated graph should include vertical grid lines.
   *
   * @param  includeVerticalGrid  Indicates whether the generated graph should
   *                              include vertical grid lines.
   */
  public void setIncludeVerticalGrid(boolean includeVerticalGrid)
  {
    this.includeVerticalGrid = includeVerticalGrid;
  }



  /**
   * Retrieves the data associated with this tracker.
   *
   * @return  The data associated with this tracker.
   */
  public double[][] getIntervalTotals()
  {
    double[][] values = new double[valueList.size()][];
    valueList.toArray(values);
    return values;
  }



  /**
   * Specifies the data to use for this tracker.  Note that the set of interval
   * totals and interval counts must have the same number of elements.
   *
   * @param  intervalTotals  The sum of all the values for each interval broken
   *                         down by category.
   * @param  intervalCounts  The number of occurrences of the tracked event
   *                         for each interval.
   */
  public void setIntervalTotals(double[][] intervalTotals, int[] intervalCounts)
  {
    valueList = new ArrayList<double[]>();
    countList = new ArrayList<Integer>();

    for (int i=0; i < intervalTotals.length; i++)
    {
      valueList.add(intervalTotals[i]);
      countList.add(intervalCounts[i]);
    }

    duration = intervalTotals.length * collectionInterval;
  }



  /**
   * Retrieves the average values for each interval.
   *
   * @return  The average values for each interval.
   */
  public double[][] getIntervalAverages()
  {
    double[][] values = new double[valueList.size()][categoryNames.length];

    for (int i=0; i < valueList.size(); i++)
    {
      int      count = countList.get(i);
      double[] data  = valueList.get(i);
      for (int j=0; j < categoryNames.length; j++)
      {
        values[i][j] = data[j] / count;
      }
    }

    return values;
  }



  /**
   * Retrieves the number of values in each interval.
   *
   * @return  The number of values in each interval.
   */
  public int[] getIntervalCounts()
  {
    int[] counts = new int[countList.size()];

    for (int i=0; i < counts.length; i++)
    {
      counts[i] = countList.get(i);
    }

    return counts;
  }



  /**
   * Retrieves the average number of data points added per collection interval.
   *
   * @return  The average number of data points added per collection interval.
   */
  public double getAverageCountPerInterval()
  {
    int totalCount = 0;
    for (int i=0; i < countList.size(); i++)
    {
      totalCount += countList.get(i);
    }

    return (1.0 * totalCount / countList.size());
  }



  /**
   * Retrieves the total values for this tracker.
   *
   * @return  The total values for this tracker.
   */
  public double[] getTotalValues()
  {
    double[] data  = new double[categoryNames.length];

    for (int i=0; i < valueList.size(); i++)
    {
      double[] intervalData  = valueList.get(i);
      for (int j=0; j < intervalData.length; j++)
      {
        data[j] += intervalData[j];
      }
    }

    return data;
  }



  /**
   * Retrieves the total value for the specified category for this tracker.
   *
   * @param  categoryName The name of the category for which to retrieve the
   *                      total value.
   *
   * @return  The total value for the specified category, or
   *          <CODE>Double.NaN</CODE> if no such category exists.
   */
  public double getTotalValue(String categoryName)
  {
    int categoryNum = -1;
    for (int i=0; i < categoryNames.length; i++)
    {
      if (categoryNames[i].equalsIgnoreCase(categoryName))
      {
        categoryNum = i;
        break;
      }
    }

    if (categoryNum < 0)
    {
      return Double.NaN;
    }

    double totalValue = 0.0;
    for (int i=0; i < valueList.size(); i++)
    {
      totalValue += valueList.get(i)[categoryNum];
    }
    return totalValue;
  }



  /**
   * Retrieves the average values for this tracker.
   *
   * @return  The average values for this tracker.
   */
  public double[] getAverageValues()
  {
    double[] data  = new double[categoryNames.length];
    int      count = 0;

    for (int i=0; i < valueList.size(); i++)
    {
      double[] intervalData  = valueList.get(i);
      for (int j=0; j < intervalData.length; j++)
      {
        data[j] += intervalData[j];
      }

      count += countList.get(i);
    }

    for (int i=0; i < data.length; i++)
    {
      data[i] = (data[i] / count);
    }

    return data;
  }



  /**
   * Retrieves the average value for the specified category for this tracker.
   *
   * @param  categoryName  The name of the category for which to retrieve the
   *                       average value.
   *
   * @return  The average value for the specified category, or
   *          <CODE>Double.NaN</CODE> if no such category exists.
   */
  public double getAverageValue(String categoryName)
  {
    int categoryNum = -1;
    for (int i=0; i < categoryNames.length; i++)
    {
      if (categoryNames[i].equalsIgnoreCase(categoryName))
      {
        categoryNum = i;
        break;
      }
    }

    if (categoryNum < 0)
    {
      return Double.NaN;
    }

    double totalValue = 0.0;
    int    count      = 0;
    for (int i=0; i < valueList.size(); i++)
    {
      totalValue += valueList.get(i)[categoryNum];
      count      += countList.get(i);
    }

    return (totalValue / count);
  }



  /**
   * Retrieves the names of the categories used by this tracker.
   *
   * @return  The names of the categories used by this tracker.
   */
  public String[] getCategoryNames()
  {
    return categoryNames;
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
   * Retrieves the value associated with this stat tracker.  This is only
   * applicable if <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what
   * exactly "the value associated with this stat tracker" means will be left up
   * to those stat trackers that are searchable.
   *
   * @return  The value associated with this stat tracker.
   */
  public double getSummaryValue()
  {
    return 0.0;
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
    return countList.size();
  }



  /**
   * Aggregates the information collected by the provided set of stat trackers
   * into a single tracker that represents the information gathered from the
   * entire set of data.  All of the stat trackers in the provided array must be
   * of the same type and have the same collection interval and categories as
   * the instance into which the information will be aggregated.
   *
   * @param  trackers  The set of stat trackers whose data is to be aggregated.
   */
  public void aggregate(StatTracker[] trackers)
  {
    if ((trackers == null) || (trackers.length == 0))
    {
      return;
    }

    int minIntervals = Integer.MAX_VALUE;
    for (int i=0; i < trackers.length; i++)
    {
      int numIntervals = ((StackedValueTracker) trackers[i]).valueList.size();
      if (numIntervals < minIntervals)
      {
        minIntervals = numIntervals;
      }
    }

    StackedValueTracker tracker = (StackedValueTracker) trackers[0];
    categoryNames      = tracker.categoryNames;
    collectionInterval = tracker.collectionInterval;
    startTime          = tracker.startTime;
    duration           = collectionInterval * minIntervals;
    stopTime           = startTime + (1000 * duration);

    for (int i=0; i < minIntervals; i++)
    {
      double[] trackerValues = tracker.valueList.get(i);
      Integer  trackerCount  = tracker.countList.get(i);

      double[] aggregateValues = new double[trackerValues.length];
      System.arraycopy(trackerValues, 0, aggregateValues, 0,
                       trackerValues.length);

      valueList.add(aggregateValues);
      countList.add(trackerCount);
    }

    for (int i=1; i < trackers.length; i++)
    {
      tracker = (StackedValueTracker) trackers[i];
      for (int j=0; j < minIntervals; j++)
      {
        double[] currentTotals = valueList.get(j);
        double[] trackerTotals = tracker.valueList.get(j);
        for (int k=0; k < currentTotals.length; k++)
        {
          currentTotals[k] += trackerTotals[k];
        }

        int currentCount = countList.get(j);
        int trackerCount = tracker.countList.get(j);
        countList.set(j, (currentCount + trackerCount));
      }
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
    double[] averageValues = getAverageValues();

    StringBuilder buf = new StringBuilder();
    buf.append(displayName);
    buf.append(" -- ");

    for (int i=0; i < categoryNames.length; i++)
    {
      if (i > 0)
      {
        buf.append(";  ");
      }

      buf.append("Average[");
      buf.append(categoryNames[i]);
      buf.append("]:  ");
      buf.append(decimalFormat.format(averageValues[i]));
    }

    return buf.toString();
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
    StringBuilder buf = new StringBuilder();
    buf.append(displayName);
    buf.append(Constants.EOL);

    double[] averageValues = getAverageValues();
    for (int i=0; i < categoryNames.length; i++)
    {
      buf.append("  Average[");
      buf.append(categoryNames[i]);
      buf.append("]:  ");
      buf.append(decimalFormat.format(averageValues[i]));
      buf.append(Constants.EOL);
    }

    for (int i=0; i < valueList.size(); i++)
    {
      double[] values = valueList.get(i);
      for (int j=0; j < categoryNames.length; j++)
      {
        buf.append("  Total[");
        buf.append(categoryNames[j]);
        buf.append(", ");
        buf.append(i);
        buf.append("]:  ");
        buf.append(decimalFormat.format(values[j]));
        buf.append(Constants.EOL);
      }
      buf.append("  Count[");
      buf.append(i);
      buf.append("]:  ");
      buf.append(countList.get(i));
      buf.append(Constants.EOL);
    }

    return buf.toString();
  }



  /**
   * Retrieves a version of the summary information for this stat tracker
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the summary data for this stat tracker.
   */
  public String getSummaryHTML()
  {
    StringBuilder buf = new StringBuilder();
    double[] averageValues = getAverageValues();

    for (int i=0; i < categoryNames.length; i++)
    {
      buf.append("<TABLE BORDER=\"1\">");
      buf.append(Constants.EOL);
      buf.append("  <TR>");
      buf.append(Constants.EOL);
      buf.append("    <TD><B>Average ");
      buf.append(categoryNames[i]);
      buf.append("</B></TD>");
      buf.append(Constants.EOL);
      buf.append("  </TR>");
      buf.append(Constants.EOL);
      buf.append("  <TR>");
      buf.append(Constants.EOL);
      buf.append("    <TD>");
      buf.append(decimalFormat.format(averageValues[i]));
      buf.append("</TD>");
      buf.append(Constants.EOL);
      buf.append(Constants.EOL);
      buf.append("  </TR>");
      buf.append(Constants.EOL);
      buf.append("</TABLE>");
      buf.append(Constants.EOL);
    }

    buf.append("<TABLE BORDER=\"1\">");
    buf.append(Constants.EOL);
    buf.append("  <TR>");
    buf.append(Constants.EOL);
    buf.append("    <TD><B>Average Count/Interval</TD>");
    buf.append(Constants.EOL);
    buf.append("  </TR>");
    buf.append(Constants.EOL);
    buf.append("  <TR>");
    buf.append(Constants.EOL);
    buf.append("    <TD>");
    buf.append(decimalFormat.format(getAverageCountPerInterval()));
    buf.append("</TD>");
    buf.append(Constants.EOL);
    buf.append("  </TR>");
    buf.append(Constants.EOL);
    buf.append("</TABLE>");
    buf.append(Constants.EOL);

    return buf.toString();
  }



  /**
   * Retrieves a version of the verbose information for this stat tracker,
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the verbose data for this stat tracker.
   */
  public String getDetailHTML()
  {
    StringBuilder buf = new StringBuilder();
    buf.append("<TABLE BORDER=\"1\">");
    buf.append(Constants.EOL);
    buf.append("  <TR>");
    buf.append(Constants.EOL);
    buf.append("    <TD><B>Interval</B></TD>");
    buf.append(Constants.EOL);

    for (int i=0; i < categoryNames.length; i++)
    {
      buf.append("    <TD><B>");
      buf.append(categoryNames[i]);
      buf.append("</B></TD>");
      buf.append(Constants.EOL);
    }

    buf.append("    <TD><B>Count</B></TD>");
    buf.append(Constants.EOL);
    buf.append("  </TR>");
    buf.append(Constants.EOL);

    for (int i=0; i < valueList.size(); i++)
    {
      buf.append("    <TD>");
      buf.append(i);
      buf.append("</TD>");
      buf.append(Constants.EOL);

      double[] values = valueList.get(i);
      for (int j=0; j < values.length; j++)
      {
        buf.append("    <TD>");
        buf.append(decimalFormat.format(values[j]));
        buf.append("</TD>");
        buf.append(Constants.EOL);
      }

      buf.append("    <TD>");
      buf.append(countList.get(i));
      buf.append("</TD>");
      buf.append(Constants.EOL);
      buf.append("  </TR>");
      buf.append(Constants.EOL);
    }

    buf.append("  <TR>");
    buf.append(Constants.EOL);
    buf.append("    <TD>Average</TD>");
    buf.append(Constants.EOL);

    double[] averageValues = getAverageValues();
    for (int i=0; i < averageValues.length; i++)
    {
      buf.append("    <TD>");
      buf.append(decimalFormat.format(averageValues[i]));
      buf.append("</TD>");
      buf.append(Constants.EOL);
    }

    buf.append("    <TD>");
    buf.append(decimalFormat.format(getAverageCountPerInterval()));
    buf.append("</TD>");
    buf.append(Constants.EOL);
    buf.append("  </TR>");
    buf.append(Constants.EOL);
    buf.append("</TABLE>");
    buf.append(Constants.EOL);

    return buf.toString();
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
    String[] returnArray = new String[categoryNames.length+1];

    for (int i=0; i < categoryNames.length; i++)
    {
      returnArray[i] = "Avg " + categoryNames[i];
    }
    returnArray[categoryNames.length] = "Avg Count/Interval";

    return returnArray;
  }



  /**
   * Retrieves the summary string data for this stat tracker as separate values.
   *
   * @return  The summary string data for this stat tracker as separate values.
   */
  public String[] getSummaryData()
  {
    String[] returnArray = new String[categoryNames.length+1];

    double[] values = getAverageValues();
    for (int i=0; i < values.length; i++)
    {
      returnArray[i] = decimalFormat.format(values[i]);
    }

    returnArray[values.length] =
         decimalFormat.format(getAverageCountPerInterval());
    return returnArray;
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
      String[][] returnArray = new String[valueList.size()+1][];

      returnArray[0] = new String[categoryNames.length + 2];
      returnArray[0][0] = "Interval";
      for (int i=0; i < categoryNames.length; i++)
      {
        returnArray[0][i+1] = categoryNames[i];
      }
      returnArray[0][categoryNames.length+1] = "Count";

      for (int i=0; i < valueList.size(); i++)
      {
        returnArray[i+1] = new String[categoryNames.length+2];
        returnArray[i+1][0] = String.valueOf(i+1);

        double[] values = valueList.get(i);
        for (int j=0; j < values.length; j++)
        {
          returnArray[i+1][j+1] = decimalFormat.format(values[j]);
        }

        returnArray[i+1][values.length+1] = String.valueOf(countList.get(i));
      }

      return returnArray;
    }
    else
    {
      String[][] returnArray = new String[valueList.size()][];

      for (int i=0; i < valueList.size(); i++)
      {
        returnArray[i] = new String[categoryNames.length+2];
        returnArray[i][0] = String.valueOf(i+1);

        double[] values = valueList.get(i);
        for (int j=0; j < values.length; j++)
        {
          returnArray[i][j+1] = decimalFormat.format(values[j]);
        }

        returnArray[i][values.length+1] =
             String.valueOf(countList.get(i));
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
    // The format used for stacked value trackers:
    // StackedValueTracker ::= SEQUENCE {
    //      categoryNames   SEQUENCE OF OCTET STRING,
    //      categoryData    SEQUENCE OF StackedValueData
    //      graphConfig     SEQUENCE OF OCTET STRING }
    //
    // StackedValueData ::= SEQUENCE {
    //      dataValues      SEQUENCE OF OCTET STRING,
    //      count           INTEGER }

    ASN1Element[] nameElements = new ASN1Element[categoryNames.length];
    for (int i=0; i < nameElements.length; i++)
    {
      nameElements[i] = new ASN1OctetString(categoryNames[i]);
    }

    ASN1Element[] dataElements = new ASN1Element[valueList.size()];
    for (int i=0; i < dataElements.length; i++)
    {
      double[] values = valueList.get(i);
      int      count  = countList.get(i);

      ASN1Element[] valueElements = new ASN1Element[values.length];
      for (int j=0; j < values.length; j++)
      {
        valueElements[j] = new ASN1OctetString(String.valueOf(values[j]));
      }

      ASN1Element[] sequenceElements = new ASN1Element[]
      {
        new ASN1Sequence(valueElements),
        new ASN1Integer(count)
      };
      dataElements[i] = new ASN1Sequence(sequenceElements);
    }

    ASN1Element[] graphConfigElements = new ASN1Element[]
    {
      new ASN1OctetString(Constants.SERVLET_PARAM_DRAW_AS_STACKED_GRAPH + "=" +
                          String.valueOf(drawAsStackedGraph)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_LABELS + "=" +
                          String.valueOf(includeLegend)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID + "=" +
                          String.valueOf(includeHorizontalGrid)),
      new ASN1OctetString(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID + "=" +
                          String.valueOf(includeVerticalGrid))
    };

    ASN1Element[] trackerElements = new ASN1Element[]
    {
      new ASN1Sequence(nameElements),
      new ASN1Sequence(dataElements),
      new ASN1Sequence(graphConfigElements)
    };
    return (new ASN1Sequence(trackerElements)).encode();
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
      ASN1Element[] trackerElements =
           ASN1Element.decodeAsSequence(encodedData).getElements();

      ASN1Element[] nameElements =
           trackerElements[0].decodeAsSequence().getElements();
      categoryNames = new String[nameElements.length];
      for (int i=0; i < nameElements.length; i++)
      {
        categoryNames[i] =
             nameElements[i].decodeAsOctetString().getStringValue();
      }

      ASN1Element[] sequenceElements =
           trackerElements[1].decodeAsSequence().getElements();
      for (int i=0; i < sequenceElements.length; i++)
      {
        ASN1Element[] dataElements =
             sequenceElements[i].decodeAsSequence().getElements();

        ASN1Element[] valueElements =
             dataElements[0].decodeAsSequence().getElements();
        double[] values = new double[valueElements.length];
        for (int j=0; j < valueElements.length; j++)
        {
          values[j] =
               Double.parseDouble(
                    valueElements[j].decodeAsOctetString().getStringValue());
        }

        int count = dataElements[1].decodeAsInteger().getIntValue();
        valueList.add(values);
        countList.add(count);
      }

      if (trackerElements.length > 2)
      {
        ASN1Element[] configElements =
          trackerElements[2].decodeAsSequence().getElements();
        for (int i=0; i < configElements.length; i++)
        {
          String elementStr =
               configElements[i].decodeAsOctetString().getStringValue();
          int equalPos = elementStr.indexOf('=');
          String name = elementStr.substring(0, equalPos);
          boolean value = Boolean.valueOf(elementStr.substring(equalPos+1));

          if (name.equals(Constants.SERVLET_PARAM_DRAW_AS_STACKED_GRAPH))
          {
            drawAsStackedGraph = value;
          }
          else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_LABELS))
          {
            includeLegend = value;
          }
          else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID))
          {
            includeHorizontalGrid = value;
          }
          else if (name.equals(Constants.SERVLET_PARAM_INCLUDE_VERT_GRID))
          {
            includeVerticalGrid = value;
          }
        }
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
    BooleanParameter drawAsStackedGraphParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_DRAW_AS_STACKED_GRAPH,
                              "Draw as Stacked Area Graph",
                              "Indicates whether the data should be graphed " +
                              "as lines for each category or as a stacked " +
                              "area graph", drawAsStackedGraph);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", includeLegend);
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
      drawAsStackedGraphParameter,
      includeLegendParameter,
      includeHGridParameter,
      includeVGridParameter
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
    BooleanParameter drawAsStackedGraphParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_DRAW_AS_STACKED_GRAPH,
                              "Draw as Stacked Area Graph",
                              "Indicates whether the data should be graphed " +
                              "as lines for each category or as a stacked " +
                              "area graph", true);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", includeLegend);
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
      drawAsStackedGraphParameter,
      includeLegendParameter,
      includeHGridParameter,
      includeVGridParameter
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
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", includeLegend);
    BooleanParameter includeHGridParameter =
        new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_HORIZ_GRID,
                             "Include Horizontal Grid Lines",
                             "Indicates whether the graph generated should " +
                             "include horizontal grid lines.",
                             includeHorizontalGrid);

    Parameter[] parameters = new Parameter[]
    {
      includeLegendParameter,
      includeHGridParameter,
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
    return "Average Value";
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
   * @param  parameters  The set of parameters that may be used to customize
   *                     the graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createGraph(Job job, int width, int height,
                                   ParameterList parameters)
  {
    boolean stackedGraph  = drawAsStackedGraph;
    boolean includeHGrid  = includeHorizontalGrid;
    boolean includeVGrid  = includeVerticalGrid;
    String  graphTitle    = displayName + " for Job " + job.getJobID();
    String  legendTitle   = "Category";


    BooleanParameter drawAsStackedGraphParameter =
         parameters.getBooleanParameter(
              Constants.SERVLET_PARAM_DRAW_AS_STACKED_GRAPH);
    if (drawAsStackedGraphParameter != null)
    {
      stackedGraph = drawAsStackedGraphParameter.getBooleanValue();
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

    StatTracker[] trackers = job.getStatTrackers(displayName);
    StackedValueTracker tracker =
         new StackedValueTracker("", "", displayName,
                  trackers[0].getCollectionInterval(),
                  ((StackedValueTracker) trackers[0]).categoryNames);
    tracker.aggregate(trackers);
    double[][] averageValues = tracker.getIntervalAverages();

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(true);
    grapher.setIncludeLegend(includeLegend, legendTitle);
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < categoryNames.length; i++)
    {
      double[] values = new double[averageValues.length];
      for (int j=0; j < values.length; j++)
      {
        values[j] = averageValues[j][i];
      }

      grapher.addDataSet(values, job.getCollectionInterval(), categoryNames[i]);
    }

    grapher.setIncludeAverage(false);
    grapher.setIncludeRegression(false);

    if (stackedGraph)
    {
      return grapher.generateStackedAreaGraph();
    }
    else
    {
      return grapher.generateLineGraph();
    }
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
    boolean stackedGraph  = drawAsStackedGraph;
    boolean includeHGrid  = includeHorizontalGrid;
    boolean includeVGrid  = includeVerticalGrid;
    String  graphTitle    = displayName;
    String  legendTitle   = "Category";


    BooleanParameter stackedParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_DRAW_AS_STACKED_GRAPH);
    if (stackedParameter != null)
    {
      stackedGraph = stackedParameter.getBooleanValue();
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

    StatTracker[] trackers = job.getResourceStatTrackers(displayName);
    StackedValueTracker tracker =
         new StackedValueTracker("", "", displayName,
                  trackers[0].getCollectionInterval(),
                  ((StackedValueTracker) trackers[0]).categoryNames);
    tracker.aggregate(trackers);
    double[][] averageValues = tracker.getIntervalAverages();

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(true);
    grapher.setIncludeLegend(includeLegend, legendTitle);
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < categoryNames.length; i++)
    {
      double[] values = new double[averageValues.length];
      for (int j=0; j < values.length; j++)
      {
        values[j] = averageValues[j][i];
      }

      grapher.addDataSet(values, job.getCollectionInterval(), categoryNames[i]);
    }

    grapher.setIncludeAverage(false);
    grapher.setIncludeRegression(false);
    if (stackedGraph)
    {
      return grapher.generateStackedAreaGraph();
    }
    else
    {
      return grapher.generateLineGraph();
    }
  }



  /**
   * Creates a graph that visually depicts the information in the provided set
   * of stat trackers.  The provided stat trackers must be the of the same type
   * as this stat tracker and must have exactly the same set of categories in
   * the same order.
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
    boolean includeHGrid = includeHorizontalGrid;

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

    String  graphTitle    = "Comparison of " + displayName;
    String  legendTitle   = "Category";

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(true);
    grapher.setIncludeLegend(includeLegend, legendTitle);
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(false);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < jobs.length; i++)
    {
      StatTracker[] trackers = jobs[i].getStatTrackers(displayName);
      StackedValueTracker tracker =
           new StackedValueTracker("", "", displayName,
                    trackers[0].getCollectionInterval(),
                    ((StackedValueTracker) trackers[0]).categoryNames);
      tracker.aggregate(trackers);
      double[] averageValues = tracker.getAverageValues();

      String dataSetName = jobs[i].getJobDescription();
      if ((dataSetName == null) || (dataSetName.length() == 0))
      {
        dataSetName = jobs[i].getJobID();
      }
      grapher.addStackedBarGraphDataSet(dataSetName, tracker.categoryNames,
                                        averageValues);
    }

    return grapher.generateStackedBarGraph();
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
    StatGrapher grapher =
         new StatGrapher(width, height, displayName);
    grapher.setBaseAtZero(true);
    grapher.setIncludeLegend(true, "Category");
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeHorizontalGrid(true);
    grapher.setIncludeVerticalGrid(true);
    grapher.setIgnoreZeroValues(false);

    double[][] averageValues = getIntervalAverages();
    for (int i=0; i < categoryNames.length; i++)
    {
      double[] values = new double[averageValues.length];
      for (int j=0; j < values.length; j++)
      {
        values[j] = averageValues[j][i];
      }

      grapher.addDataSet(values, collectionInterval, categoryNames[i]);
    }

    return grapher.generateStackedAreaGraph();
  }
}

