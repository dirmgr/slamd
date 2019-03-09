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
 * This class defines a simple stat tracker that can be used to track events
 * associated with integer values.
 *
 *
 * @author   Neil A. Wilson
 */
public class IntegerValueTracker
       implements StatTracker
{
  /**
   * The text that will be used to indicate that the comparison between multiple
   * jobs will be performed in parallel.
   */
  public static final String COMPARE_IN_PARALLEL_STRING = "Compare in Parallel";



  /**
   * The text that will be used to indicate that the comparison between multiple
   * jobs will be performed over time.
   */
  public static final String COMPARE_OVER_TIME_STRING = "Compare over Time";



  // The list that contains the data collected by this tracker, broken up into
  // intervals.
  private ArrayList<Integer> countList;

  // The list that contains the sums of the values collected by this tracker,
  // broken up into intervals.
  private ArrayList<Integer> totalList;

  // Indicates whether to enable real-time statistics collection.
  private boolean enableRealTimeStats;

  // Indicates whether this stat tracker has been started.
  private boolean hasBeenStarted;

  // Indicates whether this stat tracker is currently running.
  private boolean isRunning;

  // The formatter used to round off decimal values.
  private DecimalFormat decimalFormat;

  // The number of occurrences so far for the current interval.
  private int intervalCount;

  // The current interval number for reporting real-time stats.
  private int intervalNum;

  // The total so far for the current interval.
  private int intervalTotal;

  // The maximum value provided while gathering statistics.
  private int maxValue;

  // The minimum value provided while gathering statistics.
  private int minValue;

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

  // The stat reporter used to report real-time statistics.
  private RealTimeStatReporter statReporter;

  // The client ID for the client that used this stat tracker.
  private String clientID;

  // The display name to use for this stat tracker.
  private String displayName;

  // The thread ID for the client thread that used this stat tracker.
  private String threadID;



  /**
   * Creates a new integer value tracker intended for use as a placeholder for
   * decoding purposes.  This version of the constructor should not be used
   * by job classes.
   */
  public IntegerValueTracker()
  {
    this.clientID           = "";
    this.threadID           = "";
    this.displayName        = "";
    this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;

    decimalFormat       = new DecimalFormat("0.000");
    intervalCount       = 0;
    intervalTotal       = 0;
    maxValue            = Integer.MIN_VALUE;
    minValue            = Integer.MAX_VALUE;
    duration            = 0;
    intervalStopTime    = 0;
    startTime           = System.currentTimeMillis();
    stopTime            = 0;
    countList           = new ArrayList<Integer>();
    totalList           = new ArrayList<Integer>();
    enableRealTimeStats = false;
    statReporter        = null;
    intervalNum         = 0;
    hasBeenStarted      = false;
    isRunning           = false;
  }



  /**
   * Creates a new integer value tracker with the specified information.
   *
   * @param  clientID            The client ID of the client that used this
   *                             stat tracker.
   * @param  threadID            The thread ID of the client thread that used
   *                             this stat tracker.
   * @param  displayName         The display name to use for this stat tracker.
   * @param  collectionInterval  The collection interval in seconds that
   *                             should be used for this stat tracker.
   */
  public IntegerValueTracker(String clientID, String threadID,
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
    intervalCount       = 0;
    intervalTotal       = 0;
    maxValue            = Integer.MIN_VALUE;
    minValue            = Integer.MAX_VALUE;
    duration            = 0;
    intervalStopTime    = 0;
    startTime           = System.currentTimeMillis();
    stopTime            = 0;
    countList           = new ArrayList<Integer>();
    totalList           = new ArrayList<Integer>();
    enableRealTimeStats = false;
    statReporter        = null;
    intervalNum         = 0;
    hasBeenStarted      = false;
    isRunning           = false;
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
    return new IntegerValueTracker(clientID, threadID, displayName,
                                   collectionInterval);
  }



  /**
   * Adds the specified value to the set of information collected so far for
   * this stat tracker.
   *
   * @param  intValue  The value to add to the set of information collected so
   *                   far for this stat tracker.
   */
  public void addValue(int intValue)
  {
    long now = System.currentTimeMillis();

    // We're in the same interval as the last time a value was added.  Just add
    // this new one.
    if (now < intervalStopTime)
    {
      intervalCount++;
      intervalTotal += intValue;
    }


    // The previous interval has stopped and a new one has started.  Close out
    // the old one and start the new.  Note that if this is an event that
    // happens infrequently, then multiple intervals could have passed, so make
    // sure that the appropriate number of intervals are added.
    else
    {
      countList.add(intervalCount);
      totalList.add(intervalTotal);
      if (enableRealTimeStats)
      {
        statReporter.reportStatToAverage(this, intervalNum++,
                                         (1.0 * intervalTotal / intervalCount));
      }

      intervalStopTime += (1000 * collectionInterval);

      while (intervalStopTime < now)
      {
        countList.add(0);
        totalList.add(0);
        if (enableRealTimeStats)
        {
          statReporter.reportStatToAverage(this, intervalNum++, 0.0);
        }
        intervalStopTime += (1000 * collectionInterval);
      }

      intervalCount = 1;
      intervalTotal = intValue;
    }


    // Check to see if this value is larger than the maximum or smaller than
    // the minimum
    if (intValue > maxValue)
    {
      maxValue = intValue;
    }
    if (intValue < minValue)
    {
      minValue = intValue;
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
                         "integer value stat tracker " + displayName);
    }
    else
    {
      hasBeenStarted = true;
      isRunning = true;
    }

    // Just in case, reset all the counter info.
    intervalCount = 0;
    intervalTotal = 0;
    maxValue      = Integer.MIN_VALUE;
    minValue      = Integer.MAX_VALUE;
    countList     = new ArrayList<Integer>();
    totalList     = new ArrayList<Integer>();

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
      System.err.println("***** WARNING:  Attempting to stop integer value " +
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
      countList.add(intervalCount);
      totalList.add(intervalTotal);
      if (enableRealTimeStats)
      {
        statReporter.reportStatToAverage(this, intervalNum++,
                                         (1.0 * intervalTotal / intervalCount));
      }
      intervalStopTime += (1000 * collectionInterval);
    }

    while (intervalStopTime < now)
    {
      countList.add(0);
      totalList.add(0);
      if (enableRealTimeStats)
      {
        statReporter.reportStatToAverage(this, intervalNum++, 0.0);
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
    int totalCount = 0;

    for (int i=0; i < countList.size(); i++)
    {
      totalCount += countList.get(i);
    }

    return totalCount;
  }



  /**
   * Retrieves the total number of times that the tracked event occurred over
   * the total duration.  This method will return the count as a long, which is
   * less likely to overflow than an integer.
   *
   * @return  The total number of times that the tracked event occurred over the
   *          total duration.
   */
  public long getTotalCountAsLong()
  {
    long totalCount = 0;

    for (int i=0; i < countList.size(); i++)
    {
      totalCount += countList.get(i);
    }

    return totalCount;
  }



  /**
   * Retrieves the sum of all the individual values added to this stat tracker
   * over the total duration.
   *
   * @return  The sum of all the individual values added to this stat tracker
   *          over the total duration.
   */
  public int getTotalValue()
  {
    int totalValue = 0;

    for (int i=0; i < totalList.size(); i++)
    {
      totalValue += totalList.get(i);
    }

    return totalValue;
  }



  /**
   * Retrieves the sum of all the individual values added to this stat tracker
   * over the total duration.  The total value will be returned as a long, which
   * is less likely to overflow than an integer.
   *
   * @return  The sum of all the individual values added to this stat tracker
   *          over the total duration.
   */
  public long getTotalValueAsLong()
  {
    long totalValue = 0;

    for (int i=0; i < totalList.size(); i++)
    {
      totalValue += totalList.get(i);
    }

    return totalValue;
  }



  /**
   * Retrieves the sum of all the values added to this stat tracker divided by
   * the number of values added to this stat tracker.
   *
   * @return  The average of all the values added to this stat tracker.
   */
  public double getAverageValue()
  {
    long totalCount = getTotalCountAsLong();
    if (totalCount > 0)
    {
      return (1.0 * getTotalValueAsLong() / totalCount);
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves an array indicating the number of times the tracked event
   * occurred over each interval.
   *
   * @return  An array indicating the number of times the tracked event occurred
   *          over each interval.
   */
  public int[] getIntervalCounts()
  {
    int[] intValues = new int[countList.size()];

    for (int i=0; i < intValues.length; i++)
    {
      intValues[i] = countList.get(i);
    }

    return intValues;
  }



  /**
   * Retrieves an array containing the sums of all the values added to this
   * tracker over each interval.
   *
   * @return  An array containing the sums of all the values added to this
   *          tracker over each interval.
   */
  public int[] getIntervalTotals()
  {
    int[] totalValues = new int[totalList.size()];

    for (int i=0; i < totalValues.length; i++)
    {
      totalValues[i] = totalList.get(i);
    }

    return totalValues;
  }



  /**
   * Specifies the data associated with this tracker in terms of the total value
   * per interval and the number of occurrences per interval.  Note that the
   * number of elements in each array must be identical.
   *
   * @param  intervalTotals  An array containing the sums of all the values
   *                         added to this tracker over each interval.
   * @param  intervalCounts  An array indicating the number of times the tracked
   *                         event occurred over each interval.
   */
  public void setIntervalData(int[] intervalTotals, int[] intervalCounts)
  {
    maxValue   = Integer.MIN_VALUE;
    minValue   = Integer.MAX_VALUE;

    totalList = new ArrayList<Integer>();
    countList = new ArrayList<Integer>();
    for (int i=0; i < intervalTotals.length; i++)
    {
      totalList.add(intervalTotals[i]);
      countList.add(intervalCounts[i]);

      if (intervalTotals[i] > maxValue)
      {
        maxValue = intervalTotals[i];
      }

      if (intervalTotals[i] < minValue)
      {
        minValue = intervalTotals[i];
      }
    }

    duration = collectionInterval * intervalTotals.length;
  }



  /**
   * Retrieves the maximum value supplied to this tracker over the entire
   * duration.
   *
   * @return  The maximum value supplied to this tracker over the entire
   *          duration.
   */
  public int getMaxValue()
  {
    return maxValue;
  }



  /**
   * Retrieves the minimum value supplied to this tracker over the entire
   * duration.
   *
   * @return  The minimum value supplied to this tracker over the entire
   *          duration.
   */
  public int getMinValue()
  {
    return minValue;
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
    if (! countList.isEmpty())
    {
      return (1.0 * getTotalCountAsLong() / countList.size());
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves the average total of all the values added in a single interval.
   *
   * @return  The average total of all values added in a single interval.
   */
  public double getAverageValuePerInterval()
  {
    if (! totalList.isEmpty())
    {
      return (1.0 * getTotalValueAsLong() / totalList.size());
    }
    else
    {
      return 0.0;
    }
  }



  /**
   * Retrieves the average number of times that this tracker was updated in a
   * single second.
   *
   * @return  The average number of times that this tracker was updated in a
   *          single second.
   */
  public double getAverageCountPerSecond()
  {
    if (duration > 0)
    {
      return (1.0 * getTotalCountAsLong() / duration);
    }
    else
    {
      return 0;
    }
  }



  /**
   * Retrieves the sum of all the values provided to this tracker divided by the
   * total number of seconds that it was collecting data.
   *
   * @return  The sum of all the values provided to this tracker divided by the
   *          total number of seconds that it was collecting data.
   */
  public double getAverageValuePerSecond()
  {
    if (duration > 0)
    {
      return (1.0 * getTotalValueAsLong() / duration);
    }
    else
    {
      return 0;
    }
  }



  /**
   * Retrieves the standard deviation for this tracker, based on the average
   * value per occurrence.
   *
   * @return  The standard deviation for this tracker, based on average value
   *          per occurrence.
   */
  public double getStandardDeviation()
  {
    double avgValue = getAverageValue();
    double sumOfDifferencesSquared = 0.0;
    int    n = 0;

    for (int i=0; i < countList.size(); i++)
    {
      int intervalTotal = totalList.get(i);
      int intervalCount = countList.get(i);

      if (intervalCount > 0)
      {
        double intervalAvgPerOccurrence = 1.0 * intervalTotal / intervalCount;
        double difference = (intervalAvgPerOccurrence - avgValue);
        sumOfDifferencesSquared += (difference * difference);
        n++;
      }
    }

    return Math.sqrt(sumOfDifferencesSquared / (n - 1));
  }



  /**
   * Retrieves the correlation coefficient for the data, which is a measure of
   * how strongly a change in one variable impacts a change in another.  If the
   * regression line is perfectly horizontal (i.e., a change in one variable has
   * no impact whatsoever on the value of the other), then the correlation
   * coefficient will be zero.  If the data is perfectly linear but not
   * horizontal (i.e., a change in one variable has a linear, proportional
   * change in the other), then it will be 1.0 or -1.0.  If the data is not
   * linear, then the value will be somewhere between -1.0 and 0.0, or between
   * 0.0 and 1.0.
   *
   * @return  The regression coefficient for the data.
   */
  public double getCorrelationCoefficient()
  {
    int    n    = countList.size();
    double avgX = getAverageValue();
    double avgY = (countList.size() * collectionInterval) / 2;

    double sumOfXDiffsSquared  = 0.0;
    double sumOfYDiffsSquared  = 0.0;
    double sumOfXYDiffs        = 0.0;
    for (int i=0; i < countList.size(); i++)
    {
      int intervalTotal = totalList.get(i);
      int intervalCount = countList.get(i);

      if (intervalCount > 0)
      {
        double xValue = intervalTotal / intervalCount;
        int    yValue = (collectionInterval * i);
        double xDiff  = xValue - avgX;
        double yDiff  = yValue - avgY;

        sumOfXDiffsSquared  += (xDiff*xDiff);
        sumOfYDiffsSquared  += (yDiff*yDiff);
        sumOfXYDiffs        += (xDiff*yDiff);
      }
    }

    if (sumOfXDiffsSquared == 0)
    {
      return 0.0;
    }

    double stdDevX = Math.sqrt(sumOfXDiffsSquared / (n-1));
    double stdDevY = Math.sqrt(sumOfYDiffsSquared / (n-1));

    return (sumOfXYDiffs / ((n-1)*stdDevX*stdDevY));
  }



  /**
   * Retrieves the linear regression coefficients for this tracker, based on the
   * average value per occurrence.  The array returned will have two values,
   * A and B, which can be used in the equation y = A + Bx to generate the line
   * that most closely approximates the set of results.
   *
   * @return  The linear regression coefficients for this tracker, based on the
   *          average value per occurrence.
   */
  public double[] getRegressionCoefficients()
  {
    double sx  = 0.0;
    double sy  = 0.0;
    double sxx = 0.0;
    double syy = 0.0;
    double sxy = 0.0;
    int    n   = 0;

    for (int i=0; i < countList.size(); i++)
    {
      int intervalTotal = totalList.get(i);
      int intervalCount = countList.get(i);

      if (intervalCount > 0)
      {
        double intervalAvgValue = 1.0 * intervalTotal / intervalCount;
        sx  += (collectionInterval*i);
        sy  += intervalAvgValue;
        sxx += (collectionInterval*collectionInterval*i*i);
        syy += (intervalAvgValue*intervalAvgValue);
        sxy += (collectionInterval*i*intervalAvgValue);
        n++;
      }

    }

    double b = (sxy - (sx*sy)/n) / (sxx - (sx*sx)/n);
    double a = (sy - b*sx) / n;

    return new double[] { a, b };
  }



  /**
   * Retrieves a T score value that can be used to determine how confident we
   * are that the result set is approximately linear.  It does this by dividing
   * the data set into two halves and performing a confidence test to determine
   * whether those two means can be considered equal.
   *
   * @return  The calculated T score value.
   */
  public double getHorizontalityTScore()
  {
    // First, get the counts and totals as integer arrays to make things easier.
    int[] totalArray = getIntervalTotals();
    int[] countArray = getIntervalCounts();


    // Break the data up into two equal halves.
    int n  = countArray.length;
    int n1 = n / 2;
    int n2 = n - n1;


    // Now calculate the totals for each half.
    int total1 = 0;
    int total2 = 0;
    int count1 = 0;
    int count2 = 0;
    for (int i=0; i < n; i++)
    {
      if (i < n1)
      {
        total1 += totalArray[i];
        count1 += countArray[i];
      }
      else
      {
        total2 += totalArray[i];
        count2 += countArray[i];
      }
    }


    // If the count for both halves is zero, then the T score will automatically
    // be zero.
    if ((count1 == 0) && (count2 == 0))
    {
      return 0.0;
    }


    // Find the average for each half.
    double mean1 = 1.0 * total1 / n1 / count1;
    double mean2 = 1.0 * total2 / n2 / count2;


    // Next, calculate the standard deviation for each half.
    double sumOfDifferencesSquared1 = 0.0;
    double sumOfDifferencesSquared2 = 0.0;
    for (int i=0; i < n; i++)
    {
      if (i < n1)
      {
        double intervalAvg = 1.0 * totalArray[i] / countArray[i];
        double difference = (mean1 - intervalAvg);
        sumOfDifferencesSquared1 += (difference * difference);
      }
      else
      {
        double intervalAvg = 1.0 * totalArray[i] / countArray[i];
        double difference = (mean2 - intervalAvg);
        sumOfDifferencesSquared2 += (difference * difference);
      }
    }

    double stdDev1 = Math.sqrt(sumOfDifferencesSquared1 / (n1 - 1));
    double stdDev2 = Math.sqrt(sumOfDifferencesSquared2 / (n2 - 1));


    // Finally, calculate the standard error and the T score.
    double stdError = Math.sqrt(stdDev1*stdDev1/n1 + stdDev2*stdDev2/n2);
    return Math.abs(mean1 - mean2) / stdError;
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
    return (getAverageValue() >= value);
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
    return (getAverageValue() <= value);
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
    return getAverageValue();
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
   * of the same type and have the same collection interval as the instance into
   * which the information will be aggregated.
   *
   * @param  trackers  The set of stat trackers whose data is to be aggregated.
   */
  public void aggregate(StatTracker[] trackers)
  {
    duration  = Integer.MAX_VALUE;
    maxValue  = Integer.MIN_VALUE;
    minValue  = Integer.MAX_VALUE;

    if (trackers.length > 0)
    {
      collectionInterval = trackers[0].getCollectionInterval();
    }

    int min = Integer.MAX_VALUE;
    int[][] counts = new int[trackers.length][];
    int[][] totals = new int[trackers.length][];
    for (int i=0; i < counts.length; i++)
    {
      counts[i] = ((IntegerValueTracker) trackers[i]).getIntervalCounts();
      totals[i] = ((IntegerValueTracker) trackers[i]).getIntervalTotals();
      if (counts[i].length < min)
      {
         min = counts[i].length;
      }

      if (trackers[i].getDuration() < duration)
      {
        duration = trackers[i].getDuration();
      }

      if (((IntegerValueTracker) trackers[i]).getMaxValue() > maxValue)
      {
        maxValue = ((IntegerValueTracker) trackers[i]).getMaxValue();
      }

      if (((IntegerValueTracker) trackers[i]).getMinValue() < minValue)
      {
        minValue = ((IntegerValueTracker) trackers[i]).getMinValue();
      }
    }

    int[] aggregateCounts = new int[min];
    int[] aggregateTotals = new int[min];
    for (int i=0; i < aggregateCounts.length; i++)
    {
      aggregateCounts[i] = 0;
      aggregateTotals[i] = 0;

      for (int j=0; j < counts.length; j++)
      {
        aggregateCounts[i] += counts[j][i];
        aggregateTotals[i] += totals[j][i];
      }
    }

    countList = new ArrayList<Integer>(aggregateCounts.length);
    totalList = new ArrayList<Integer>(aggregateCounts.length);
    for (int i=0; i < aggregateCounts.length; i++)
    {
      countList.add(aggregateCounts[i]);
      totalList.add(aggregateTotals[i]);
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
    return displayName + " -- Total:  " + getTotalValueAsLong() +
           ";  Avg Value:  " + decimalFormat.format(getAverageValue()) +
           ";  Avg/Second:  " +
           decimalFormat.format(getAverageValuePerSecond()) +
           ";  Avg/Interval:  " +
           decimalFormat.format(getAverageValuePerInterval()) +
           ";  Std Dev:  " + decimalFormat.format(getStandardDeviation()) +
           ";  Corr Coeff:  " +
           decimalFormat.format(getCorrelationCoefficient());
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
    double[] regressionCoefficients = getRegressionCoefficients();
    String regressionEquation = "y = " +
                                decimalFormat.format(regressionCoefficients[0]);
    if (regressionCoefficients[1] < 0)
    {
      regressionEquation += " - " +
           decimalFormat.format(Math.abs(regressionCoefficients[1])) + "x";
    }
    else
    {
      regressionEquation += " + " +
           decimalFormat.format(regressionCoefficients[1]) + "x";
    }

    returnBuffer.append(displayName + Constants.EOL);
    returnBuffer.append("Total:  " + getTotalValueAsLong() + Constants.EOL);
    returnBuffer.append("Count:  " + getTotalCountAsLong() + Constants.EOL);
    returnBuffer.append("Average Value:  " +
                        decimalFormat.format(getAverageValue()) +
                        Constants.EOL);
    if (getMaxValue() == Integer.MIN_VALUE)
    {
      returnBuffer.append("Maximum Value:  N/A" + Constants.EOL);
    }
    else
    {
      returnBuffer.append("Maximum Value:  " + getMaxValue() + Constants.EOL);
    }

    if (getMinValue() == Integer.MAX_VALUE)
    {
      returnBuffer.append("Minimum Value:  N/A" + Constants.EOL);
    }
    else
    {
      returnBuffer.append("Minimum Value:  " + getMinValue() + Constants.EOL);
    }

    returnBuffer.append("Average Total/Second:  " +
                        decimalFormat.format(getAverageValuePerSecond()) +
                        Constants.EOL);
    returnBuffer.append("Average Count/Second:  " +
                        decimalFormat.format(getAverageCountPerSecond()) +
                        Constants.EOL);
    returnBuffer.append("Average Total/Interval:  " +
                        decimalFormat.format(getAverageValuePerInterval()) +
                        Constants.EOL);
    returnBuffer.append("Average Count/Interval:  " +
                        decimalFormat.format(getAverageCountPerInterval()) +
                        Constants.EOL);
    returnBuffer.append("Std Dev:  " +
                        decimalFormat.format(getStandardDeviation()) +
                        Constants.EOL);
    returnBuffer.append("Correlation Coefficient:  " +
                        decimalFormat.format(getCorrelationCoefficient()) +
                        Constants.EOL);
    returnBuffer.append("Regression Equation:  " + regressionEquation +
                        Constants.EOL);
    returnBuffer.append("Horizontality T Score:  " +
                        decimalFormat.format(getHorizontalityTScore()) +
                        Constants.EOL);

    for (int i=0; i < countList.size(); i++)
    {
      int c = countList.get(i);
      int t = totalList.get(i);

      returnBuffer.append("Interval ");
      returnBuffer.append(i+1);
      returnBuffer.append(":  ");
      returnBuffer.append("Total=" + t);
      returnBuffer.append("; Count=" + c);
      if (c > 0)
      {
        returnBuffer.append("; Average=" + decimalFormat.format(1.0 * t / c));
      }
      else
      {
        returnBuffer.append("; Average=0.000");
      }
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
    html.append("    <TD><B>Total</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Value</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg/Second</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg/Interval</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Std Dev</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Corr Coeff</B></TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD>" + getTotalValueAsLong() + "</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getAverageValue()) + "</TD>" +
                Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getAverageValuePerSecond()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" +
                decimalFormat.format(getAverageValuePerInterval()) + "</TD>" +
                Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getStandardDeviation()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getCorrelationCoefficient()) +
                "</TD>" + Constants.EOL);
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
    double[] regressionCoefficients = getRegressionCoefficients();
    String regressionEquation = "y = " +
                                decimalFormat.format(regressionCoefficients[0]);
    if (regressionCoefficients[1] < 0)
    {
      regressionEquation += " - " +
           decimalFormat.format(Math.abs(regressionCoefficients[1])) + "x";
    }
    else
    {
      regressionEquation += " + " +
           decimalFormat.format(regressionCoefficients[1]) + "x";
    }

    html.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD><B>Total</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg Value</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Max Value</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Min Value</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg/Second</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg/Interval</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Std Dev</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Correlation Coefficient</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Regression Equation</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Horizontality T Score</B></TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD>" + getTotalValueAsLong() + "</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getAverageValue()) + "</TD>" +
                Constants.EOL);
    if (getMaxValue() == Integer.MIN_VALUE)
    {
      html.append("    <TD>N/A</TD>" + Constants.EOL);
    }
    else
    {
      html.append("    <TD>" + getMaxValue() + "</TD>" + Constants.EOL);
    }

    if (getMinValue() == Integer.MAX_VALUE)
    {
      html.append("    <TD>N/A</TD>" + Constants.EOL);
    }
    else
    {
      html.append("    <TD>" + getMinValue() + "</TD>" + Constants.EOL);
    }

    html.append("    <TD>" + decimalFormat.format(getAverageValuePerSecond()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" +
                decimalFormat.format(getAverageValuePerInterval()) + "</TD>" +
                Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getStandardDeviation()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getCorrelationCoefficient()) +
                "</TD>" + Constants.EOL);
    html.append("    <TD>" + regressionEquation +"</TD>" + Constants.EOL);
    html.append("    <TD>" + decimalFormat.format(getHorizontalityTScore()) +
                "</TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);
    html.append("</TABLE>" + Constants.EOL);

    html.append("<BR><BR>" + Constants.EOL);

    html.append("<TABLE BORDER=\"1\">" + Constants.EOL);
    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD><B>Interval</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Total</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Count</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Average</B></TD>" + Constants.EOL);
    html.append("    <TD><B>Avg/Second</B></TD>" + Constants.EOL);
    html.append("  </TR>" + Constants.EOL);

    for (int i=0; i < countList.size(); i++)
    {
      int c = countList.get(i);
      int t = totalList.get(i);

      html.append("  <TR>" + Constants.EOL);
      html.append("    <TD>" + (i+1) + "</TD>" + Constants.EOL);
      html.append("    <TD>" + t + "</TD>" + Constants.EOL);
      html.append("    <TD>" + c + "</TD>" + Constants.EOL);
      if (c > 0)
      {
        html.append("    <TD>" + decimalFormat.format(1.0 * t / c) + "</TD>" +
                    Constants.EOL);
      }
      else
      {
        html.append("    <TD>0.000</TD>" + Constants.EOL);
      }
      html.append("    <TD>" +
                  decimalFormat.format(1.0 * t / collectionInterval) + "</TD>" +
                  Constants.EOL);
      html.append("  </TR>" + Constants.EOL);
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
      displayName + " Total Value",
      displayName + " Avg Value",
      displayName + " Avg/Second",
      displayName + " Avg/Interval",
      displayName + " Std Dev",
      displayName + " Corr Coeff"
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
      String.valueOf(getTotalValueAsLong()),
      String.valueOf(decimalFormat.format(getAverageValue())),
      String.valueOf(decimalFormat.format(getAverageValuePerSecond())),
      String.valueOf(decimalFormat.format(getAverageValuePerInterval())),
      decimalFormat.format(getStandardDeviation()),
      decimalFormat.format(getCorrelationCoefficient())
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
      String[][] returnArray = new String[countList.size()+1][];

      returnArray[0] = new String[] { "Interval", "Total", "Count" };
      for (int i=0; i < countList.size(); i++)
      {
        returnArray[i+1] = new String[]
        {
          String.valueOf(i+1),
          String.valueOf(totalList.get(i)),
          String.valueOf(countList.get(i))
        };
      }

      return returnArray;
    }
    else
    {
      String[][] returnArray = new String[countList.size()][];

      for (int i=0; i < countList.size(); i++)
      {
        returnArray[i] = new String[]
        {
          String.valueOf(totalList.get(i)),
          String.valueOf(countList.get(i))
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
    // OK.  This is cheating because it's not really being encoded in the
    // spirit of ASN.1.  But the encoded representation is much smaller than
    // if I had used sequence of sequence, and this format isn't going to
    // be publicly documented anyway.
    ASN1Element[] elements = new ASN1Element[(2*countList.size())+2];

    elements[0] = new ASN1Integer(maxValue);
    elements[1] = new ASN1Integer(minValue);
    for (int i=0,j=2; i < countList.size(); i++, j += 2)
    {
      elements[j] = new ASN1Integer(totalList.get(i));
      elements[j+1] = new ASN1Integer(countList.get(i));

    }

    return (new ASN1Sequence(elements)).encode();
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

      countList  = new ArrayList<Integer>();
      totalList  = new ArrayList<Integer>();

      maxValue = elements[0].decodeAsInteger().getIntValue();
      minValue = elements[1].decodeAsInteger().getIntValue();


      for (int i=2; i < elements.length; i += 2)
      {
        int intervalTotal = elements[i].decodeAsInteger().getIntValue();
        int intervalCount = elements[i+1].decodeAsInteger().getIntValue();

        totalList.add(intervalTotal);
        countList.add(intervalCount);
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
    BooleanParameter includeAverageParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_AVERAGE,
                              "Include Average Line",
                              "Indicates whether the graph generated should " +
                              "include a line that shows the average value " +
                              "for the displayed data set.", false);
    BooleanParameter includeRegressionParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_REGRESSION,
                              "Include Regression Line",
                              "Indicates whether the graph generated should " +
                              "include a line that shows the calculated " +
                              "linear regression for the displayed data set " +
                              "(i.e., a trend line).", false);
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
      includeAverageParameter,
      includeRegressionParameter,
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
    BooleanParameter includeAverageParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_AVERAGE,
                              "Include Average Line",
                              "Indicates whether the graph generated should " +
                              "include a line that shows the average value " +
                              "for the displayed data set.", false);
    BooleanParameter includeRegressionParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_REGRESSION,
                              "Include Regression Line",
                              "Indicates whether the graph generated should " +
                              "include a line that shows the calculated " +
                              "linear regression for the displayed data set " +
                              "(i.e., a trend line).", false);
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
      includeAverageParameter,
      includeRegressionParameter,
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
    String[] choices = new String[]
    {
      COMPARE_IN_PARALLEL_STRING,
      COMPARE_OVER_TIME_STRING
    };

    BooleanParameter baseAtZeroParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO,
                              "Base at Zero",
                              "Indicates whether the lower bound for the " +
                              "graph should be based at zero rather than " +
                              "dynamically calculated from the information " +
                              "contained in the data provided.", true);
    BooleanParameter drawAsBarParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH,
                              "Draw as Bar Graph",
                              "Indicates whether this graph should be drawn " +
                              "as a bar graph instead of a line graph.",
                              (jobs.length > 2));
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
    MultiChoiceParameter comparisonTypeParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_COMPARE_TYPE,
                                  "Comparison Type",
                                  "Indicates whether the job information " +
                                  "should be compared in parallel (with " +
                                  "statistics overlayed), or over time (with " +
                                  "each job comprising a single data point).",
                                  choices, choices[0]);

    Parameter[] parameters = new Parameter[]
    {
//      comparisonTypeParameter,
      drawAsBarParameter,
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
    int[]    intervalTotals     = getIntervalTotals();
    double[] avgTotalsPerSecond = new double[intervalTotals.length];
    for (int i=0; i < avgTotalsPerSecond.length; i++)
    {
      avgTotalsPerSecond[i] = 1.0 * intervalTotals[i] / collectionInterval;
    }

    return avgTotalsPerSecond;
  }



  /**
   * Retrieves the label that should be included along the vertical axis in a
   * line graph for this stat tracker.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>.
   *
   * @return  The lable that should be included along the vertical axis in a
   *          line graph for this stat tracker, or <CODE>null</CODE> if that
   *          data is not applicable.
   */
  public String getAxisLabel()
  {
    return "Average/Second";
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
    boolean includeLegend     = false;
    boolean includeAverage    = false;
    boolean includeRegression = false;
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

    BooleanParameter includeAverageParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_INCLUDE_AVERAGE);
    if (includeAverageParameter != null)
    {
      includeAverage = includeAverageParameter.getBooleanValue();
    }

    BooleanParameter includeRegressionParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_REGRESSION);
    if (includeRegressionParameter != null)
    {
      includeRegression = includeRegressionParameter.getBooleanValue();
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

    StatTracker[] trackersToGraph;
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
          IntegerValueTracker tracker =
               new IntegerValueTracker(clientIDs[i], "", displayName,
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
      IntegerValueTracker tracker =
           new IntegerValueTracker("", "", displayName,
                                   trackers[0].getCollectionInterval());
      tracker.aggregate(trackers);
      trackersToGraph = new StatTracker[] { tracker };
    }

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setIncludeLegend(includeLegend, legendTitle);
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < trackersToGraph.length; i++)
    {
      IntegerValueTracker tracker = (IntegerValueTracker) trackersToGraph[i];

      int[] intervalTotals = tracker.getIntervalTotals();
      int[] intervalCounts = tracker.getIntervalCounts();

      int numElements = intervalTotals.length;
      int j = 0;
      int subtractor = 0;
      int lastIndex = intervalTotals.length;
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
      double[] avgTotalsPerInterval = new double[numElements];

      for ( ; j < lastIndex; j++)
      {
        if (intervalCounts[j] == 0)
        {
          avgTotalsPerInterval[j-subtractor] = Double.NaN;
        }
        else
        {
          avgTotalsPerInterval[j-subtractor] = 1.0 * intervalTotals[j] /
                                               intervalCounts[j];
        }
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

      grapher.addDataSet(avgTotalsPerInterval, job.getCollectionInterval(),
                         label);
    }

    grapher.setIncludeAverage(includeAverage);
    grapher.setIncludeRegression(includeRegression);

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
    boolean includeAverage    = false;
    boolean includeLegend     = false;
    boolean includeRegression = false;
    boolean excludeFirst      = false;
    boolean excludeLast       = false;
    boolean includeHGrid      = false;
    boolean includeVGrid      = false;
    boolean baseAtZero        = false;
    String  graphTitle        = displayName;


    BooleanParameter includeAverageParameter =
         parameters.getBooleanParameter(
                       Constants.SERVLET_PARAM_INCLUDE_AVERAGE);
    if (includeAverageParameter != null)
    {
      includeAverage = includeAverageParameter.getBooleanValue();
    }

    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter includeRegressionParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_INCLUDE_REGRESSION);
    if (includeRegressionParameter != null)
    {
      includeRegression = includeRegressionParameter.getBooleanValue();
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

    StatTracker[] trackers = job.getResourceStatTrackers(displayName);
    IntegerValueTracker tracker =
         new IntegerValueTracker("", "", displayName,
                                 trackers[0].getCollectionInterval());
    tracker.aggregate(trackers);
    StatTracker[] trackersToGraph = new StatTracker[] { tracker };

    StatGrapher grapher =
         new StatGrapher(width, height, graphTitle);
    grapher.setBaseAtZero(baseAtZero);
    grapher.setIncludeLegend(includeLegend, "Job ID");
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeHorizontalGrid(includeHGrid);
    grapher.setIncludeVerticalGrid(includeVGrid);
    grapher.setIgnoreZeroValues(false);

    for (int i=0; i < trackersToGraph.length; i++)
    {
      tracker = (IntegerValueTracker) trackersToGraph[i];

      int[] intervalTotals = tracker.getIntervalTotals();
      int[] intervalCounts = tracker.getIntervalCounts();

      int numElements = intervalTotals.length;
      int j = 0;
      int subtractor = 0;
      int lastIndex = intervalTotals.length;
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
      double[] avgTotalsPerInterval = new double[numElements];

      for ( ; j < lastIndex; j++)
      {
        if (intervalCounts[j] == 0)
        {
          avgTotalsPerInterval[j-subtractor] = Double.NaN;
        }
        else
        {
          avgTotalsPerInterval[j-subtractor] = 1.0 * intervalTotals[j] /
                                               intervalCounts[j];
        }
      }

      grapher.addDataSet(avgTotalsPerInterval, job.getCollectionInterval(),
                         "Job " + job.getJobID());
    }

    grapher.setIncludeAverage(includeAverage);
    grapher.setIncludeRegression(includeRegression);

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
    boolean compareOverTime   = false;
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

    MultiChoiceParameter compareOverTimeParameter =
         parameters.getMultiChoiceParameter(
                         Constants.SERVLET_PARAM_COMPARE_TYPE);
    if (compareOverTimeParameter != null)
    {
      String compareStr = compareOverTimeParameter.getStringValue();
      compareOverTime = compareStr.equalsIgnoreCase(COMPARE_OVER_TIME_STRING);
    }

    if (compareOverTime)
    {
      // NYI
      return null;
    }
    else
    {
      StatTracker[] trackersToGraph = new StatTracker[jobs.length];
      for (int i=0; i < jobs.length; i++)
      {
        StatTracker[] jobTrackers = jobs[i].getStatTrackers(displayName);
        IntegerValueTracker jobTracker =
             new IntegerValueTracker(null, null, displayName,
                                     jobTrackers[0].getCollectionInterval());
        jobTracker.aggregate(jobTrackers);
        trackersToGraph[i] = jobTracker;
      }

      StatGrapher grapher =
           new StatGrapher(width, height, graphTitle);
      grapher.setBaseAtZero(baseAtZero);
      grapher.setIncludeLegend(includeLegend, "Job");
      grapher.setVerticalAxisTitle("Average/Second");
      grapher.setIncludeHorizontalGrid(includeHGrid);
      grapher.setIncludeVerticalGrid(includeVGrid);
      grapher.setIgnoreZeroValues(false);

      for (int i=0; i < trackersToGraph.length; i++)
      {
        IntegerValueTracker tracker = (IntegerValueTracker) trackersToGraph[i];

        int[] intervalTotals = tracker.getIntervalTotals();
        int[] intervalCounts = tracker.getIntervalCounts();
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
        double[] avgTotalsPerInterval = new double[numElements];

        for ( ; j < lastIndex; j++)
        {
          if (intervalCounts[j] == 0)
          {
            avgTotalsPerInterval[j-subtractor] = Double.NaN;
          }
          else
          {
            avgTotalsPerInterval[j-subtractor] = 1.0 * intervalTotals[j] /
                                                 intervalCounts[j];
          }
        }

        String label = jobs[i].getJobDescription();
        if ((label == null) || (label.length() == 0))
        {
          label = jobs[i].getJobID();
        }
        grapher.addDataSet(avgTotalsPerInterval,
                           jobs[i].getCollectionInterval(), label);
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
    grapher.setVerticalAxisTitle("Average Value");
    grapher.setIncludeLegend(false, "");
    grapher.setIgnoreZeroValues(false);

    int[]    intervalTotals     = getIntervalTotals();
    double[] avgTotalsPerSecond = new double[intervalTotals.length];
    for (int i=0; i < intervalTotals.length; i++)
    {
      avgTotalsPerSecond[i] = 1.0 * intervalTotals[i] / collectionInterval;
    }

    grapher.addDataSet(avgTotalsPerSecond, collectionInterval, displayName);
    return grapher.generateLineGraph();
  }
}

