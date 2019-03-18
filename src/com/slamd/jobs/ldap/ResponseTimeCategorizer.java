/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2019.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs.ldap;



import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.StatTracker;



/**
 * This class provides a mechanism for categorizing operation response times.
 */
public final class ResponseTimeCategorizer
{
  /**
   * The display name for the stat tracker used to track response time
   * categories.
   */
  private static final String STAT_RESPONSE_TIME_CATEGORIES =
       "Response Time Categories";



  /**
   * The display name for the response time category used for response times
   * that are less than 1 millisecond.
   */
  private static final String RESPONSE_TIME_CATEGORY_LESS_THAN_1_MS =
       "Less Than 1ms";



  /**
   * The display name for the response time category used for response times
   * that are between 1 millisecond and 2 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_1_AND_2_MS =
       "Between 1ms and 2ms";



  /**
   * The display name for the response time category used for response times
   * that are between 2 milliseconds and 3 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_2_AND_3_MS =
       "Between 2ms and 3ms";



  /**
   * The display name for the response time category used for response times
   * that are between 3 milliseconds and 4 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_3_AND_4_MS =
       "Between 3ms and 4ms";



  /**
   * The display name for the response time category used for response times
   * that are between 4 milliseconds and 5 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_4_AND_5_MS =
       "Between 4ms and 5ms";



  /**
   * The display name for the response time category used for response times
   * that are between 5 milliseconds and 10 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_5_AND_10_MS =
       "Between 5ms and 10ms";



  /**
   * The display name for the response time category used for response times
   * that are between 10 milliseconds and 20 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_10_AND_20_MS =
       "Between 10ms and 20ms";



  /**
   * The display name for the response time category used for response times
   * that are between 20 milliseconds and 30 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_20_AND_30_MS =
       "Between 20ms and 30ms";



  /**
   * The display name for the response time category used for response times
   * that are between 30 milliseconds and 40 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_30_AND_40_MS =
       "Between 30ms and 40ms";



  /**
   * The display name for the response time category used for response times
   * that are between 40 milliseconds and 50 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_40_AND_50_MS =
       "Between 40ms and 50ms";



  /**
   * The display name for the response time category used for response times
   * that are between 50 milliseconds and 100 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_50_AND_100_MS =
       "Between 50ms and 100ms";



  /**
   * The display name for the response time category used for response times
   * that are between 100 milliseconds and 200 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_100_AND_200_MS =
       "Between 100ms and 200ms";



  /**
   * The display name for the response time category used for response times
   * that are between 200 milliseconds and 300 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_200_AND_300_MS =
       "Between 200ms and 300ms";



  /**
   * The display name for the response time category used for response times
   * that are between 300 milliseconds and 400 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_300_AND_400_MS =
       "Between 300ms and 400ms";



  /**
   * The display name for the response time category used for response times
   * that are between 400 milliseconds and 500 milliseconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_400_AND_500_MS =
       "Between 400ms and 500ms";



  /**
   * The display name for the response time category used for response times
   * that are between 500 milliseconds and 1 second.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_500_MS_AND_1_S =
       "Between 500ms and 1s";



  /**
   * The display name for the response time category used for response times
   * that are between 1 second and 2 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_1_AND_2_S =
       "Between 1s and 2s";



  /**
   * The display name for the response time category used for response times
   * that are between 2 seconds and 3 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_2_AND_3_S =
       "Between 2s and 2s";



  /**
   * The display name for the response time category used for response times
   * that are between 3 seconds and 4 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_3_AND_4_S =
       "Between 3s and 4s";



  /**
   * The display name for the response time category used for response times
   * that are between 4 seconds and 5 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_4_AND_5_S =
       "Between 4s and 5s";



  /**
   * The display name for the response time category used for response times
   * that are between 5 seconds and 10 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_5_AND_10_S =
       "Between 5s and 10s";



  /**
   * The display name for the response time category used for response times
   * that are between 10 seconds and 20 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_10_AND_20_S =
       "Between 10s and 20s";



  /**
   * The display name for the response time category used for response times
   * that are between 20 seconds and 30 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_20_AND_30_S =
       "Between 20s and 30s";



  /**
   * The display name for the response time category used for response times
   * that are between 30 seconds and 60 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_BETWEEN_30_AND_60_S =
       "Between 30s and 60s";



  /**
   * The display name for the response time category used for response times
   * that are greater than 60 seconds.
   */
  private static final String RESPONSE_TIME_CATEGORY_MORE_THAN_60_S =
       "Longer Than 60s";



  // The stat tracker instance maintained by this class.
  private final CategoricalTracker responseTimeCategories;



  /**
   * Creates a new instance of this response time categorizer with the provided
   * information.
   *
   * @param  clientID            The client ID for this instance.
   * @param  threadID            The thread ID for this instance.
   * @param  collectionInterval  The statistics collection interval for this
   *                             instance.
   */
  public ResponseTimeCategorizer(final String clientID, final String threadID,
                                 final int collectionInterval)
  {
    this(STAT_RESPONSE_TIME_CATEGORIES, clientID, threadID, collectionInterval);
  }



  /**
   * Creates a new instance of this response time categorizer with the provided
   * information.
   *
   * @param  trackerName         The display name for the stat tracker.
   * @param  clientID            The client ID for this instance.
   * @param  threadID            The thread ID for this instance.
   * @param  collectionInterval  The statistics collection interval for this
   *                             instance.
   */
  public ResponseTimeCategorizer(final String trackerName,
                                 final String clientID, final String threadID,
                                 final int collectionInterval)
  {
    responseTimeCategories = new CategoricalTracker(clientID, threadID,
         trackerName, collectionInterval);
  }



  /**
   * Retrieves a stub for the stat tracker maintained by this class.
   *
   * @param  clientID            The client ID to use for the stub.
   * @param  threadID            The thread ID to use for the stub.
   * @param  collectionInterval  The collection interval to use for the sub.
   *
   * @return  The stat tracker stub that was created.
   */
  public static StatTracker getStatTrackerStub(final String clientID,
                                               final String threadID,
                                               final int collectionInterval)
  {
    return getStatTrackerStub(STAT_RESPONSE_TIME_CATEGORIES, clientID,
         threadID, collectionInterval);
  }



  /**
   * Retrieves a stub for the stat tracker maintained by this class.
   *
   * @param  trackerName         The display name for the stat tracker.
   * @param  clientID            The client ID to use for the stub.
   * @param  threadID            The thread ID to use for the stub.
   * @param  collectionInterval  The collection interval to use for the sub.
   *
   * @return  The stat tracker stub that was created.
   */
  public static StatTracker getStatTrackerStub(final String trackerName,
                                               final String clientID,
                                               final String threadID,
                                               final int collectionInterval)
  {
    return new CategoricalTracker(clientID, threadID, trackerName,
         collectionInterval);
  }



  /**
   * Indicates that the categorizer should start tracking statistics.
   */
  public void startStatTracker()
  {
    responseTimeCategories.startTracker();
  }



  /**
   * Indicates that the categorizer should stop tracking statistics.
   */
  public void stopStatTracker()
  {
    responseTimeCategories.stopTracker();
  }



  /**
   * Retrieves the stat tracker maintained by this response time categorizer.
   *
   * @return  The stat tracker maintained by this response time categorizer.
   */
  public StatTracker getStatTracker()
  {
    return responseTimeCategories;
  }



  /**
   * Increments a counter associated with an appropriate response time category.
   *
   * @param  startTimeNanos  The time the operation started, as returned by
   *                         {@code System.nanoTime}.
   * @param  endTimeNanos    The time the operation completed, as returned by
   *                         {@code System.nanoTime}.
   */
  public void categorizeResponseTime(final long startTimeNanos,
                                     final long endTimeNanos)
  {
    final long elapsedTimeNanos = endTimeNanos - startTimeNanos;
    if (elapsedTimeNanos < 1_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_LESS_THAN_1_MS);
    }
    else if (elapsedTimeNanos < 2_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_1_AND_2_MS);
    }
    else if (elapsedTimeNanos < 3_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_2_AND_3_MS);
    }
    else if (elapsedTimeNanos < 4_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_3_AND_4_MS);
    }
    else if (elapsedTimeNanos < 5_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_4_AND_5_MS);
    }
    else if (elapsedTimeNanos < 10_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_5_AND_10_MS);
    }
    else if (elapsedTimeNanos < 20_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_10_AND_20_MS);
    }
    else if (elapsedTimeNanos < 30_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_20_AND_30_MS);
    }
    else if (elapsedTimeNanos < 40_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_30_AND_40_MS);
    }
    else if (elapsedTimeNanos < 50_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_40_AND_50_MS);
    }
    else if (elapsedTimeNanos < 100_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_50_AND_100_MS);
    }
    else if (elapsedTimeNanos < 200_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_100_AND_200_MS);
    }
    else if (elapsedTimeNanos < 300_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_200_AND_300_MS);
    }
    else if (elapsedTimeNanos < 400_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_300_AND_400_MS);
    }
    else if (elapsedTimeNanos < 500_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_400_AND_500_MS);
    }
    else if (elapsedTimeNanos < 1_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_500_MS_AND_1_S);
    }
    else if (elapsedTimeNanos < 2_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_1_AND_2_S);
    }
    else if (elapsedTimeNanos < 3_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_2_AND_3_S);
    }
    else if (elapsedTimeNanos < 4_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_3_AND_4_S);
    }
    else if (elapsedTimeNanos < 5_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_4_AND_5_S);
    }
    else if (elapsedTimeNanos < 10_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_5_AND_10_S);
    }
    else if (elapsedTimeNanos < 20_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_10_AND_20_S);
    }
    else if (elapsedTimeNanos < 30_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_20_AND_30_S);
    }
    else if (elapsedTimeNanos < 60_000_000_000L)
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_BETWEEN_30_AND_60_S);
    }
    else
    {
      responseTimeCategories.increment(
           RESPONSE_TIME_CATEGORY_MORE_THAN_60_S);
    }
  }
}
