/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
package com.slamd.jobs;



import java.util.concurrent.Semaphore;

import com.unboundid.ldap.sdk.AsyncRequestID;
import com.unboundid.ldap.sdk.AsyncSearchResultListener;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;

import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class provides an implementation of an asynchronous search result
 * listener that may be used to perform all processing necessary for an
 * asynchronous search operation.
 */
final class LDAPAsynchronousSearchRateListener
      implements AsyncSearchResultListener
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -6999763945597256112L;



  // The stat tracker that will be updated with information about the result
  // code received.
  private final CategoricalTracker resultCodes;

  // The stat tracker that will be updated when the search has completed.
  private final IncrementalTracker searchesCompleted;

  // The stat tracker that will be updated if the time required to process the
  // search exceeds the response time threshold.
  private final IncrementalTracker searchesExceedingThreshold;

  // The stat tracker that will be updated with the number of entries returned.
  private final IntegerValueTracker entriesReturned;

  // The time that this object was created.
  private final long createTimeMillis;

  // The response time threshold for the job.
  private final long responseTimeThresholdMillis;

  // The semaphore used to limit the number of outstanding operations.
  private final Semaphore outstandingRequests;

  // The stat tracker that will be updated with the length of time required to
  // process the search.
  private final TimeTracker searchTimer;



  /**
   * Creates a new instance of this class with the provided information.
   *
   * @param  responseTimeThresholdMillis  The time in milliseconds under which
   *                                      responses are expected to be returned.
   * @param  outstandingRequests          The semaphore used to limit the number
   *                                      of outstanding requests, or
   *                                      {@code null} if no limit should be
   *                                      enforced.
   * @param  resultCodes                  The stat tracker used to keep track of
   *                                      the result codes returned for
   *                                      search operations, or {@code null } if
   *                                      this should not be kept.
   * @param  searchesCompleted            The stat tracker used to keep track of
   *                                      the number of search operations
   *                                      completed, or {@code null} if this
   *                                      should not be kept.
   * @param  searchesExceedingThreshold   The stat tracker used to keep track of
   *                                      searches exceeding the response time
   *                                      threshold, or {@code null} if this
   *                                      should not be kept.
   * @param  entriesReturned              The stat tracker used to keep track of
   *                                      the number of entries returned per
   *                                      search, or {@code null} if this should
   *                                      not be kept.
   * @param  searchTimer                  The stat tracker used to keep track of
   *                                      the length of time required to process
   *                                      each search, or {@code null} if this
   *                                      should not be kept.
   */
  LDAPAsynchronousSearchRateListener(final long responseTimeThresholdMillis,
       final Semaphore outstandingRequests,
       final CategoricalTracker resultCodes,
       final IncrementalTracker searchesCompleted,
       final IncrementalTracker searchesExceedingThreshold,
       final IntegerValueTracker entriesReturned,
       final TimeTracker searchTimer)
  {
    this.responseTimeThresholdMillis = responseTimeThresholdMillis;
    this.outstandingRequests         = outstandingRequests;
    this.resultCodes                 = resultCodes;
    this.searchesCompleted           = searchesCompleted;
    this.searchesExceedingThreshold  = searchesExceedingThreshold;
    this.entriesReturned             = entriesReturned;
    this.searchTimer                 = searchTimer;

    createTimeMillis = System.currentTimeMillis();
  }



  /**
   * Indicates that the provided search result entry has been received for the
   * associated search.
   *
   * @param  entry  The entry that was received.
   */
  public void searchEntryReturned(final SearchResultEntry entry)
  {
    // No implementation is required.
  }



  /**
   * Indicates that the provided search result reference has been received for
   * the associated search.
   *
   * @param  reference  The reference that was received.
   */
  public void searchReferenceReturned(final SearchResultReference reference)
  {
    // No implementation is required.
  }



  /**
   * Indicates that the associated search operation has completed.
   *
   * @param  id      The asynchronous request ID for the search.
   * @param  result  The result received for the search.
   */
  public void searchResultReceived(final AsyncRequestID id,
                                   final SearchResult result)
  {
    final long elapsedTime = System.currentTimeMillis() - createTimeMillis;

    if (resultCodes != null)
    {
      resultCodes.increment(result.getResultCode().toString());
    }

    if (searchesCompleted != null)
    {
      searchesCompleted.increment();
    }

    if ((searchesExceedingThreshold != null) &&
        (responseTimeThresholdMillis > 0L) &&
        (elapsedTime > responseTimeThresholdMillis))
    {
      searchesExceedingThreshold.increment();
    }

    if (entriesReturned != null)
    {
      entriesReturned.addValue(result.getEntryCount());
    }

    if (searchTimer != null)
    {
      searchTimer.updateTimer((int) elapsedTime);
    }

    if (outstandingRequests != null)
    {
      outstandingRequests.release();
    }
  }
}
