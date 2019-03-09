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
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class defines a stat tracker that can be used to count the number
 * of times a given event occurs, as well as divide those occurrences into
 * separate categories.
 *
 *
 * @author   Neil A. Wilson
 */
public class CategoricalTracker
       implements StatTracker
{
  // Indicates whether this stat tracker has been started.
  private boolean hasBeenStarted;

  // Indicates whether this stat tracker is currently running.
  private boolean isRunning;

  // The formatter used to round off decimal values.
  private DecimalFormat decimalFormat;

  // The collection interval in seconds.
  private int collectionInterval;

  // The length of time in seconds that this tracker was collecting statistics.
  private int duration;

  // The total number of elements across all categories.
  private int totalCount;

  // The number of times each category of event has occurred during the current
  // interval.
  private int[] intervalCounts;

  // The number of times each category of event has occurred over the total
  // interval.
  private int[] totalCounts;

  // The time that the current collection interval should stop.
  private long intervalStopTime;

  // The time that startTracker was called.
  private long startTime;

  // The time that stopTracker was called.
  private long stopTime;

  // The client ID of the client that used this stat tracker.
  private String clientID;

  // The display name to use for this stat tracker.
  private String displayName;

  // The thread ID of the client thread that used this stat tracker.
  private String threadID;

  // The names associated with each of the categories.
  private String[] categoryNames;

  // The list that contains the data collected by this tracker, broken up into
  // intervals.
  private ArrayList<int[]> countList;



  /**
   * Creates a new categorical tracker intended for use as a placeholder for
   * decoding purposes.  This version of the constructor should not be used
   * by job classes.
   */
  public CategoricalTracker()
  {
    this.clientID           = "";
    this.threadID           = "";
    this.displayName        = "";
    this.collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;

    decimalFormat    = new DecimalFormat("0.000");
    intervalCounts   = new int[0];
    totalCounts      = new int[0];
    categoryNames    = new String[0];
    totalCount       = 0;
    intervalStopTime = 0;
    startTime        = System.currentTimeMillis();
    stopTime         = 0;
    duration         = 0;
    countList        = new ArrayList<int[]>();
    hasBeenStarted   = false;
    isRunning        = false;
  }




  /**
   * Creates a new categorical tracker with the specified information.
   *
   * @param  clientID            The client ID of the client that used this
   *                             stat tracker.
   * @param  threadID            The thread ID of the thread that used this
   *                             stat tracker.
   * @param  displayName         The display name to use for this stat tracker.
   * @param  collectionInterval  The collection interval in seconds that
   *                             should be used for this stat tracker.
   */
  public CategoricalTracker(String clientID, String threadID,
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

    decimalFormat    = new DecimalFormat("0.000");
    intervalCounts   = new int[0];
    totalCounts      = new int[0];
    categoryNames    = new String[0];
    totalCount       = 0;
    intervalStopTime = 0;
    startTime        = System.currentTimeMillis();
    stopTime         = 0;
    duration         = 0;
    countList        = new ArrayList<int[]>();
    hasBeenStarted   = false;
    isRunning        = false;
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
    return new CategoricalTracker(clientID, threadID, displayName,
                                  collectionInterval);
  }



  /**
   * Increments the count for the specified category.
   *
   * @param  category  The category in which to increment the count.
   */
  public void increment(String category)
  {
    long now = System.currentTimeMillis();
    totalCount++;

    // We're in the same interval as the last time the counter was incremented.
    // Just increment it again.
    if (now < intervalStopTime)
    {
      // See if the specified category is one we already know about.  If so,
      // then update the counter for that category.
      for (int i=0; i < categoryNames.length; i++)
      {
        if (category.equals(categoryNames[i]))
        {
          intervalCounts[i]++;
          totalCounts[i]++;
          return;
        }
      }

      // We don't have any information about this category, so create a new one.
      int currentCategories = categoryNames.length;
      String[] newCategoryNames  = new String[currentCategories+1];
      int[]    newIntervalCounts = new int[currentCategories+1];
      int[]    newTotalCounts    = new int[currentCategories+1];
      System.arraycopy(categoryNames, 0, newCategoryNames, 0,
                       categoryNames.length);
      System.arraycopy(intervalCounts, 0, newIntervalCounts, 0,
                       intervalCounts.length);
      System.arraycopy(totalCounts, 0, newTotalCounts, 0, totalCounts.length);
      newCategoryNames[currentCategories]  = category;
      newIntervalCounts[currentCategories] = 1;
      newTotalCounts[currentCategories]    = 1;
      intervalCounts = newIntervalCounts;
      totalCounts    = newTotalCounts;
      categoryNames  = newCategoryNames;
    }


    // The previous interval has stopped and a new one has started.  Close out
    // the old one and start the new.  Note that if this is an event that
    // happens infrequently, then multiple intervals could have passed, so make
    // sure that the appropriate number of intervals are added.
    else
    {
      // Make a copy of the current interval counts and add it to the list.
      int[] tmpCounts = new int[intervalCounts.length];
      System.arraycopy(intervalCounts, 0, tmpCounts, 0, tmpCounts.length);
      countList.add(tmpCounts);

      intervalStopTime += (1000 * collectionInterval);

      while (intervalStopTime < now)
      {
        int[] counts = new int[intervalCounts.length];
        for (int i=0; i < counts.length; i++)
        {
          counts[i] = 0;
        }
        countList.add(counts);
        intervalStopTime += (1000 * collectionInterval);
      }

      boolean matchFound = false;
      for (int i=0; i < categoryNames.length; i++)
      {
        if (category.equals(categoryNames[i]))
        {
          intervalCounts[i] = 1;
          totalCounts[i]++;
          matchFound = true;
        }
        else
        {
          intervalCounts[i] = 0;
        }
      }

      if (! matchFound)
      {
        int currentCategories = categoryNames.length;
        String[] newCategoryNames  = new String[currentCategories+1];
        int[]    newIntervalCounts = new int[currentCategories+1];
        int[]    newTotalCounts    = new int[currentCategories+1];
        System.arraycopy(categoryNames, 0, newCategoryNames, 0,
                         categoryNames.length);
        System.arraycopy(intervalCounts, 0, newIntervalCounts, 0,
                         intervalCounts.length);
        System.arraycopy(totalCounts, 0, newTotalCounts, 0, totalCounts.length);
        newCategoryNames[currentCategories]  = category;
        newIntervalCounts[currentCategories] = 1;
        newTotalCounts[currentCategories]    = 1;
        intervalCounts = newIntervalCounts;
        totalCounts    = newTotalCounts;
        categoryNames  = newCategoryNames;
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
                         "categorical stat tracker " + displayName);
    }
    else
    {
      hasBeenStarted = true;
      isRunning = true;
    }

    // Just in case, reset all the counter info.
    categoryNames  = new String[0];
    intervalCounts = new int[0];
    totalCounts    = new int[0];
    countList      = new ArrayList<int[]>();
    totalCount     = 0;

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
      System.err.println("***** WARNING:  Attempting to stop categorical " +
                         "stat tracker " + displayName +
                         " without having started it");
    }
    else
    {
      isRunning = false;
    }


    // If the previous interval had passed since the last update, make sure
    // that we add the appropriate number of empty intervals.
    while (intervalStopTime < now)
    {
      int[] counts = new int[intervalCounts.length];
      for (int i=0; i < counts.length; i++)
      {
        counts[i] = 0;
      }
      countList.add(counts);
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
   * Retrieves the total number of times that the tracked event occurred for the
   * specified category over the total duration.
   *
   * @param  categoryName  The name of the category for which to determine the
   *                       total.
   *
   * @return  The total number of times that the tracked event occurred for the
   *          specified category over the total duration.
   */
  public int getTotalCount(String categoryName)
  {
    for (int i=0; i < categoryNames.length; i++)
    {
      if (categoryName.equalsIgnoreCase(categoryNames[i]))
      {
        int total = 0;

        for (int j=0; j < countList.size(); j++)
        {
          total += countList.get(j)[i];
        }

        return total;
      }
    }

    return 0;
  }



  /**
   * Retrieves the total number of times that the tracked event occurred for
   * each category over the total duration.  The order of the elements in the
   * returned array will correspond to the order of the elements returned from
   * the <CODE>getCategoryNames()</CODE> method.
   *
   * @return  The total number of times that the tracked event occurred for each
   *          category over the total duration.
   */
  public int[] getTotalCounts()
  {
    int[] totalCounts = new int[categoryNames.length];

    for (int i=0; i < countList.size(); i++)
    {
      for (int j=0; j < categoryNames.length; j++)
      {
        totalCounts[j] += countList.get(i)[j];
      }
    }

    return totalCounts;
  }



  /**
   * Retrieves the category names associated with this stat tracker.
   *
   * @return  The category names associated with this stat tracker.
   */
  public String[] getCategoryNames()
  {
    return categoryNames;
  }



  /**
   * Retrieves the set of interval counts by category for this stat tracker.
   * Note that each element of the array will itself be an array of the counts,
   * with the elements in the same order as the category names.
   *
   * @return  The set of interval counts by category for this stat tracker.
   */
  public int[][] getIntervalCounts()
  {
    int[][] countsArray = new int[countList.size()][categoryNames.length];

    for (int i=0; i < countsArray.length; i++)
    {
      int[] counts = countList.get(i);
      int j;
      for (j=0; j < counts.length; j++)
      {
        countsArray[i][j] = counts[j];
      }
      for ( ; j < categoryNames.length; j++)
      {
        countsArray[i][j] = 0;
      }
    }

    return countsArray;
  }



  /**
   * Retrieves the set of interval counts for the specified category.
   *
   * @param  categoryName  The name of the category for which to retrieve the
   *                       interval counts.
   *
   * @return  The interval counts for the specified category, or an empty array
   *          if the specified category is not defined for this tracker.
   */
  public int[] getIntervalCounts(String categoryName)
  {
    for (int i=0; i < categoryNames.length; i++)
    {
      if (categoryName.equalsIgnoreCase(categoryNames[i]))
      {
        int[] counts = new int[countList.size()];

        for (int j=0; j < counts.length; j++)
        {
          counts[j] = countList.get(j)[i];
        }

        return counts;
      }
    }

    return new int[0];
  }



  /**
   * Specifies the number of occurrences of the tracked event for each interval,
   * separated by category names.
   *
   * @param  categoryNames   The names of the categories corresponding to the
   *                         values in the interval counts.
   * @param  intervalCounts  The number of occurrences of the tracked event
   *                         by category by interval.
   */
  public void setIntervalCounts(String[] categoryNames, int[][] intervalCounts)
  {
    totalCount  = 0;
    totalCounts = new int[categoryNames.length];

    this.categoryNames = categoryNames;

    countList = new ArrayList<int[]>();
    for (int i=0; i < intervalCounts.length; i++)
    {
      countList.add(intervalCounts[i]);

      for (int j=0; j < intervalCounts[i].length; j++)
      {
        totalCounts[j] += intervalCounts[i][j];
        totalCount     += intervalCounts[i][j];
      }
    }

    duration = collectionInterval * intervalCounts.length;
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
   * of the same type as the instance into which the information will be
   * aggregated.
   *
   * @param  trackers  The set of stat trackers whose data is to be aggregated.
   */
  public void aggregate(StatTracker[] trackers)
  {
    if (trackers.length == 0)
    {
      // If there aren't any trackers in the array, then this tracker will be
      // empty.
      clientID           = "";
      threadID           = "";
      displayName        = "";
      collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
      totalCounts        = new int[0];
      categoryNames      = new String[0];
      totalCount         = 0;
      countList          = new ArrayList<int[]>();
    }
    else if (trackers.length == 1)
    {
      // If there was only one tracker provided, then make this tracker look
      // like it.
      CategoricalTracker tracker = (CategoricalTracker) trackers[0];
      clientID           = tracker.clientID;
      threadID           = tracker.threadID;
      displayName        = tracker.displayName;
      collectionInterval = tracker.collectionInterval;
      totalCounts        = tracker.totalCounts;
      categoryNames      = tracker.categoryNames;
      totalCount         = tracker.totalCount;
      countList        = tracker.countList;
    }
    else
    {
      // There were multiple trackers provided, so we need to do a little
      // preliminary work.  Iterate through all the trackers and put together
      // a list of all the tracker names and figure out the maximum number of
      // intervals.
      ArrayList<String> categoryNameList = new ArrayList<String>();
      int maxIntervals = 0;
      for (int i=0; i < trackers.length; i++)
      {
        CategoricalTracker tracker = (CategoricalTracker) trackers[i];
        if (tracker.countList.size() > maxIntervals)
        {
          maxIntervals = tracker.countList.size();
        }

        for (int j=0; j < tracker.categoryNames.length; j++)
        {
          boolean matchFound = false;
          for (int k=0; k < categoryNameList.size(); k++)
          {
            if (tracker.categoryNames[j].equals(categoryNameList.get(k)))
            {
              matchFound = true;
              break;
            }
          }
          if (! matchFound)
          {
            categoryNameList.add(tracker.categoryNames[j]);
          }
        }
      }


      // Set the category names based on the list compiled from the trackers
      categoryNames = new String[categoryNameList.size()];
      categoryNameList.toArray(categoryNames);


      // Start with a blank tracker.
      clientID           = trackers[0].getClientID();
      threadID           = trackers[0].getThreadID();
      displayName        = trackers[0].getDisplayName();
      collectionInterval = trackers[0].getCollectionInterval();
      totalCount         = 0;
      countList          = new ArrayList<int[]>(maxIntervals);
      totalCounts        = new int[categoryNames.length];
      for (int i=0; i < totalCounts.length; i++)
      {
        totalCounts[i] = 0;
      }


      // Create a new array of ints to hold the global counts
      int[][] intervalCounts = new int[maxIntervals][categoryNames.length];
      for (int i=0; i < intervalCounts.length; i++)
      {
        for (int j=0; j < intervalCounts[i].length; j++)
        {
          intervalCounts[i][j] = 0;
        }
      }


      // Iterate through all of the trackers and update the data in this tracker
      // with data from the other trackers.
      for (int i=0; i < trackers.length; i++)
      {
        CategoricalTracker tracker         = (CategoricalTracker) trackers[i];
        String[]           trackerNames    = tracker.getCategoryNames();
        int[][]            trackerCounters = tracker.getIntervalCounts();

        // Go through each of the categories in this tracker and figure out
        // where they are in the global category list.
        for (int j=0; j < trackerNames.length; j++)
        {
          for (int k=0; k < categoryNames.length; k++)
          {
            if (trackerNames[j].equals(categoryNames[k]))
            {
              // This is the right category, so add all the information from
              // the current tracker to this tracker.
              for (int l=0; l < trackerCounters.length; l++)
              {
                int numInCategory = trackerCounters[l][j];
                intervalCounts[l][k] += numInCategory;
                totalCounts[k]       += numInCategory;
                totalCount           += numInCategory;
              }
              break;
            }
          }
        }
      }

      // Finally, convert the interval counts array into a list.
      for (int i=0; i < intervalCounts.length; i++)
      {
        countList.add(intervalCounts[i]);
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
    StringBuilder returnBuffer = new StringBuilder();
    returnBuffer.append(displayName);
    returnBuffer.append(" -- ");
    String separator = "";

    for (int i=0; i < categoryNames.length; i++)
    {
      returnBuffer.append(separator);
      returnBuffer.append(categoryNames[i]);
      returnBuffer.append(":  ");
      returnBuffer.append(totalCounts[i]);
      returnBuffer.append(" (");
      returnBuffer.append(decimalFormat.format(100.0 * totalCounts[i] /
                                               totalCount));
      returnBuffer.append("%)");
      separator = "; ";
    }

    return returnBuffer.toString();
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
    for (int i=0; i < categoryNames.length; i++)
    {
      returnBuffer.append("Total ");
      returnBuffer.append(categoryNames[i]);
      returnBuffer.append(":  ");
      returnBuffer.append(totalCounts[i]);
      returnBuffer.append(" (");
      returnBuffer.append(decimalFormat.format(100.0 * totalCounts[i] /
                                               totalCount));
      returnBuffer.append("%)");
      returnBuffer.append(Constants.EOL);
    }

    int[][] counts = getIntervalCounts();
    for (int i=0; i < counts.length; i++)
    {
      returnBuffer.append("Interval " + i);
      for (int j=0; j < counts[i].length; j++)
      {
        returnBuffer.append("; ");
        returnBuffer.append(categoryNames[j]);
        returnBuffer.append(":  ");
        returnBuffer.append(counts[i][j]);
        returnBuffer.append(decimalFormat.format(100.0 * counts[i][j] /
                                                 totalCounts[j]));
        returnBuffer.append(" (");
        returnBuffer.append("%)");
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

    for (int i=0; i < categoryNames.length; i++)
    {
      html.append("<TABLE BORDER=\"1\">" + Constants.EOL);
      html.append("  <TR>" + Constants.EOL);
      html.append("    <TD>" + categoryNames[i] + "</TD>" + Constants.EOL);
      html.append("  </TR>" + Constants.EOL);
      html.append("  <TR>" + Constants.EOL);
      html.append("    <TD>" + totalCounts[i] + " (" +
                  decimalFormat.format(100.0 * totalCounts[i] / totalCount) +
                  "%)</TD>" + Constants.EOL);
      html.append("  </TR>" + Constants.EOL);
      html.append("</TABLE>" + Constants.EOL);
    }

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
    html.append("    <TD>Interval</TD>" + Constants.EOL);
    for (int i=0; i < categoryNames.length; i++)
    {
      html.append("    <TD>" + categoryNames[i] + "</TD>" + Constants.EOL);
    }
    html.append("  </TR>" + Constants.EOL);

    html.append("  <TR>" + Constants.EOL);
    html.append("    <TD>Total</TD>" + Constants.EOL);
    for (int i=0; i < categoryNames.length; i++)
    {
      html.append("    <TD>" + totalCounts[i] + " (" +
                  decimalFormat.format(100.0 * totalCounts[i] / totalCount) +
                  "%)</TD>" + Constants.EOL);
    }
    html.append("  </TR>" + Constants.EOL);

    int[][] counts = getIntervalCounts();
    for (int i=0; i < counts.length; i++)
    {
      int intervalTotal = 0;
      for (int j=0; j < counts[i].length; j++)
      {
        intervalTotal += counts[i][j];
      }

      html.append("  <TR>" + Constants.EOL);
      html.append("    <TD>" + i + "</TD>" + Constants.EOL);
      for (int j=0; j < counts[i].length; j++)
      {
        html.append("    <TD>" + counts[i][j] + " (" +
                    decimalFormat.format(100.0 * counts[i][j] / intervalTotal) +
                    ")</TD>" + Constants.EOL);
      }
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
      getDisplayName()
    };
  }



  /**
   * Retrieves the summary string data for this stat tracker as separate values.
   *
   * @return  The summary string data for this stat tracker as separate values.
   */
  public String[] getSummaryData()
  {
    StringBuilder returnBuffer = new StringBuilder();

    String separator = "";
    for (int i=0; i < categoryNames.length; i++)
    {
      returnBuffer.append(separator);
      returnBuffer.append(categoryNames[i]);
      returnBuffer.append(":  ");
      returnBuffer.append(totalCounts[i]);
      returnBuffer.append(" (");
      returnBuffer.append(decimalFormat.format(100.0 * totalCounts[i] /
                                               totalCount));
      returnBuffer.append("%)");
      separator = "; ";
    }


    return new String[]
    {
      returnBuffer.toString()
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
    int[][] countArray = getIntervalCounts();

    if (includeLabels)
    {
      String[][] returnArray =
           new String[countArray.length+1][categoryNames.length+1];

      returnArray[0][0] = "Interval";
      for (int i=0; i < categoryNames.length; i++)
      {
        returnArray[0][i+1] = categoryNames[i];
      }

      for (int i=0; i < countArray.length; i++)
      {
        returnArray[i+1][0] = String.valueOf(i+1);

        for (int j=0; j < countArray[i].length; j++)
        {
          returnArray[i+1][j+1] = String.valueOf(countArray[i][j]);
        }
      }

      return returnArray;
    }
    else
    {
      String[][] returnArray =
           new String[countArray.length][categoryNames.length];

      for (int i=0; i < countArray.length; i++)
      {
        for (int j=0; j < countArray[i].length; j++)
        {
          returnArray[i][j] = String.valueOf(countArray[i][j]);
        }
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
    // This data is encoded as a sequence of sequences.  The first sequence is
    // the names of all the categories.  The second is the count for each
    // category for one interval.
    int numCategories = categoryNames.length;

    ASN1Element[] elements = new ASN1Element[countList.size() + 1];

    ASN1Element[] nameElements = new ASN1Element[numCategories];
    for (int i=0; i < numCategories; i++)
    {
      nameElements[i] = new ASN1OctetString(categoryNames[i]);
    }
    elements[0] = new ASN1Sequence(nameElements);

    int[][] countArray = getIntervalCounts();
    for (int i=0; i < countArray.length; i++)
    {
      ASN1Element[] intervalElements = new ASN1Element[numCategories];

      for (int j=0; j < intervalElements.length; j++)
      {
        intervalElements[j] = new ASN1Integer(countArray[i][j]);
      }

      elements[i+1] = new ASN1Sequence(intervalElements);
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

      ASN1Element[] categoryNameElements =
           elements[0].decodeAsSequence().getElements();
      categoryNames = new String[categoryNameElements.length];
      for (int i=0; i < categoryNames.length; i++)
      {
        categoryNames[i] =
             categoryNameElements[i].decodeAsOctetString().getStringValue();
      }

      countList   = new ArrayList<int[]>();
      totalCount  = 0;
      totalCounts = new int[categoryNames.length];
      for (int i=1; i < elements.length; i++)
      {
        ASN1Element[] countElements =
             elements[i].decodeAsSequence().getElements();
        int[] counts = new int[countElements.length];
        for (int j=0; j< counts.length; j++)
        {
          counts[j] = countElements[j].decodeAsInteger().getIntValue();
          totalCounts[j] += counts[j];
          totalCount += counts[j];
        }
        countList.add(counts);
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
    String[] clientIDs = job.getStatTrackerClientIDs();
    for (int i=0; i < clientIDs.length; i++)
    {
      dataSetList.add("Summary for Client " + clientIDs[i]);
    }
    for (int i=0; i < clientIDs.length; i++)
    {
      StatTracker[] threadTrackers = job.getStatTrackers(displayName,
                                                         clientIDs[i]);
      for (int j=0; j < threadTrackers.length; j++)
      {
        dataSetList.add("Detail for Thread " + threadTrackers[j].getThreadID());
      }
    }
    String[] dataSets = new String[dataSetList.size()];
    dataSetList.toArray(dataSets);


    BooleanParameter drawAsBarParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH,
                              "Draw as Bar Graph",
                              "Indicates whether this graph should be drawn " +
                              "as a bar graph instead of a line graph.", false);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", true);
    BooleanParameter showPercentagesParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_SHOW_PERCENTAGES,
                              "Show Percentages in Legend",
                              "Indicates whether the graph legend will show " +
                              "the percentage for each category by the " +
                              "category name.", true);
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
      drawAsBarParameter,
      includeLegendParameter,
      showPercentagesParameter
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
    BooleanParameter drawAsBarParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH,
                              "Draw as Bar Graph",
                              "Indicates whether this graph should be drawn " +
                              "as a bar graph instead of a line graph.", false);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", true);
    BooleanParameter showPercentagesParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_SHOW_PERCENTAGES,
                              "Show Percentages in Legend",
                              "Indicates whether the graph legend will show " +
                              "the percentage for each category by the " +
                              "category name.", true);

    Parameter[] parameters = new Parameter[]
    {
      drawAsBarParameter,
      includeLegendParameter,
      showPercentagesParameter
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
    BooleanParameter drawAsBarParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH,
                              "Draw as Bar Graph",
                              "Indicates whether this graph should be drawn " +
                              "as a bar graph instead of a line graph.", false);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend that shows the categories " +
                              "included in the graph.", false);
    BooleanParameter showPercentagesParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_SHOW_PERCENTAGES,
                              "Show Percentages in Legend",
                              "Indicates whether the graph legend will show " +
                              "the percentage for each category by the " +
                              "category name.", false);

    Parameter[] parameters = new Parameter[]
    {
      drawAsBarParameter,
      includeLegendParameter,
      showPercentagesParameter
    };

    return new ParameterList(parameters);
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
    boolean drawAsBarGraph  = false;
    boolean includeLegend   = false;
    boolean showPercentages = false;
    String  detailLevelStr  = "Overall Summary for Job " + job.getJobID();
    String  graphTitle      = displayName + " for Job " + job.getJobID();

    BooleanParameter drawAsBarParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH);
    if (drawAsBarParameter != null)
    {
      drawAsBarGraph = drawAsBarParameter.getBooleanValue();
    }

    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter showPercentagesParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_SHOW_PERCENTAGES);
    if (showPercentagesParameter != null)
    {
      showPercentages = showPercentagesParameter.getBooleanValue();
    }

    MultiChoiceParameter detailLevelParameter =
         parameters.getMultiChoiceParameter(
                         Constants.SERVLET_PARAM_DETAIL_LEVEL);
    if (detailLevelParameter != null)
    {
      detailLevelStr = detailLevelParameter.getStringValue();
    }

    if (detailLevelStr.startsWith("Summary for Client "))
    {
      String clientID = detailLevelStr.substring(19);
      graphTitle      = displayName + " for Client " + clientID;
      StatTracker[] trackers = job.getStatTrackers(displayName, clientID);
      CategoricalTracker tracker = new CategoricalTracker(clientID, "",
                                                          displayName,
                                                          collectionInterval);
      tracker.aggregate(trackers);
      StatGrapher grapher = new StatGrapher(width, height, graphTitle);
      grapher.setIncludeLegend(includeLegend, "Category Name");
      grapher.setShowPercentages(showPercentages);

      if (drawAsBarGraph)
      {
        return createBarGraph(grapher, tracker);
      }
      else
      {
        return grapher.generatePieGraph(tracker.categoryNames,
                                        tracker.totalCounts);
      }
    }
    else if (detailLevelStr.startsWith("Detail for Thread "))
    {
      String threadID = detailLevelStr.substring(18);
      graphTitle      = displayName + " for Client Thread " + threadID;
      StatTracker[] trackers = job.getStatTrackers(displayName, clientID);
      CategoricalTracker tracker = null;
      for (int i=0; i < trackers.length; i++)
      {
        if (trackers[i].getThreadID().equals(threadID))
        {
          tracker = (CategoricalTracker) trackers[i];
          break;
        }
      }

      if (tracker == null)
      {
        return null;
      }

      StatGrapher grapher = new StatGrapher(width, height, graphTitle);
      grapher.setIncludeLegend(includeLegend, "Category Name");
      grapher.setShowPercentages(showPercentages);

      if (drawAsBarGraph)
      {
        return createBarGraph(grapher, tracker);
      }
      else
      {
        return grapher.generatePieGraph(tracker.categoryNames,
                                        tracker.totalCounts);
      }
    }
    else
    {
      StatTracker[] trackers = job.getStatTrackers(displayName);
      CategoricalTracker tracker = new CategoricalTracker("", "",
                                                          displayName,
                                                          collectionInterval);
      tracker.aggregate(trackers);
      StatGrapher grapher = new StatGrapher(width, height, graphTitle);
      grapher.setIncludeLegend(includeLegend, "Category Name");
      grapher.setShowPercentages(showPercentages);

      if (drawAsBarGraph)
      {
        return createBarGraph(grapher, tracker);
      }
      else
      {
        return grapher.generatePieGraph(tracker.categoryNames,
                                        tracker.totalCounts);
      }
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
    boolean drawAsBarGraph  = false;
    boolean includeLegend   = true;
    boolean showPercentages = true;
    String  graphTitle      = displayName;

    BooleanParameter drawAsBarParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH);
    if (drawAsBarParameter != null)
    {
      drawAsBarGraph = drawAsBarParameter.getBooleanValue();
    }

    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter showPercentagesParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_SHOW_PERCENTAGES);
    if (showPercentagesParameter != null)
    {
      showPercentages = showPercentagesParameter.getBooleanValue();
    }

    StatTracker[] trackers = job.getResourceStatTrackers(displayName);
    CategoricalTracker tracker = new CategoricalTracker("", "",
                                                        displayName,
                                                        collectionInterval);
    tracker.aggregate(trackers);
    StatGrapher grapher = new StatGrapher(width, height, graphTitle);
    grapher.setIncludeLegend(includeLegend, "Category Name");
    grapher.setShowPercentages(showPercentages);

    if (drawAsBarGraph)
    {
      return createBarGraph(grapher, tracker);
    }
    else
    {
      return grapher.generatePieGraph(tracker.categoryNames,
                                      tracker.totalCounts);
    }
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
    // Categorical trackers are not searchable.
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
    // Categorical trackers are not searchable.
    return null;
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
    boolean drawAsBarGraph  = false;
    boolean includeLegend   = false;
    boolean showPercentages = false;
    String  graphTitle      = "Comparison of " + displayName;

    BooleanParameter drawAsBarParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_DRAW_AS_BAR_GRAPH);
    if (drawAsBarParameter != null)
    {
      drawAsBarGraph = drawAsBarParameter.getBooleanValue();
    }

    BooleanParameter includeLegendParameter =
         parameters.getBooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
    if (includeLegendParameter != null)
    {
      includeLegend = includeLegendParameter.getBooleanValue();
    }

    BooleanParameter showPercentagesParameter =
         parameters.getBooleanParameter(
                         Constants.SERVLET_PARAM_SHOW_PERCENTAGES);
    if (showPercentagesParameter != null)
    {
      showPercentages = showPercentagesParameter.getBooleanValue();
    }

    StatTracker[] trackers = new StatTracker[jobs.length];
    for (int i=0; i < jobs.length; i++)
    {
      trackers[i] =
           new CategoricalTracker(null, null, displayName,
                                  jobs[i].getCollectionInterval());
      StatTracker[] jobTrackers = jobs[i].getStatTrackers(displayName);
      trackers[i].aggregate(jobTrackers);
    }

    CategoricalTracker tracker =
         new CategoricalTracker(null, null, displayName, collectionInterval);
    tracker.aggregate(trackers);

    StatGrapher grapher = new StatGrapher(width, height, graphTitle);
    grapher.setIncludeLegend(includeLegend, "Category Name");
    grapher.setShowPercentages(showPercentages);

    if (drawAsBarGraph)
    {
      return createBarGraph(grapher, tracker);
    }
    else
    {
      return grapher.generatePieGraph(tracker.categoryNames,
                                      tracker.totalCounts);
    }
  }



  /**
   * Generates a bar graph representation of the information in this categorical
   * tracker.
   *
   * @param  grapher  The stat grapher that will be used to generate the graph.
   * @param  tracker  The categorical tracker containing the data to be graphed.
   *
   * @return  The bar graph generated from the provided stat tracker.
   */
  public BufferedImage createBarGraph(StatGrapher grapher,
                                      CategoricalTracker tracker)
  {
    DecimalFormat percentFormat = new DecimalFormat("0.00");
    String[] names  = tracker.categoryNames;
    int[]    counts = tracker.totalCounts;

    for (int i=0; i < names.length; i++)
    {
      double[] values = new double[] { counts[i] };
      String   name   = names[i];
      if (grapher.showPercentages)
      {
        name += " (" +
                percentFormat.format(100.0 * counts[i] / tracker.totalCount) +
                "%)";
      }

      grapher.addDataSet(values, collectionInterval, name);
    }

    return grapher.generateBarGraph();
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
    grapher.setIncludeLegend(true, "Category Name");
    grapher.setShowPercentages(true);
    return grapher.generatePieGraph(categoryNames, totalCounts);
  }
}

