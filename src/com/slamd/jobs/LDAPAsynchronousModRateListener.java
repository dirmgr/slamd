/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
package com.slamd.jobs;



import java.util.concurrent.Semaphore;

import com.unboundid.ldap.sdk.AsyncRequestID;
import com.unboundid.ldap.sdk.AsyncResultListener;
import com.unboundid.ldap.sdk.LDAPResult;

import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class provides an implementation of an asynchronous LDAP result listener
 * that may be used to perform all processing necessary for an asynchronous
 * modify operation.
 */
final class LDAPAsynchronousModRateListener
      implements AsyncResultListener
{
  // The stat tracker that will be updated with information about the result
  // code received.
  private final CategoricalTracker resultCodes;

  // The stat tracker that will be updated when the modify has completed.
  private final IncrementalTracker modsCompleted;

  // The stat tracker that will be updated if the time required to process the
  // modify exceeds the response time threshold.
  private final IncrementalTracker modsExceedingThreshold;

  // The time that this object was created.
  private final long createTimeMillis;

  // The response time threshold for the job.
  private final long responseTimeThresholdMillis;

  // The semaphore used to limit the number of outstanding operations.
  private final Semaphore outstandingRequests;

  // The stat tracker that will be updated with the length of time required to
  // process the modify.
  private final TimeTracker modTimer;



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
   *                                      the result codes returned for modify
   *                                      operations, or {@code null } if this
   *                                      should not be kept.
   * @param  modsCompleted                The stat tracker used to keep track of
   *                                      the number of modify operations
   *                                      completed, or {@code null} if this
   *                                      should not be kept.
   * @param  modsExceedingThreshold       The stat tracker used to keep track of
   *                                      modifications exceeding the response
   *                                      time threshold, or {@code null} if
   *                                      this should not be kept.
   * @param  modTimer                     The stat tracker used to keep track of
   *                                      the length of time required to process
   *                                      each modify, or {@code null} if this
   *                                      should not be kept.
   */
  LDAPAsynchronousModRateListener(final long responseTimeThresholdMillis,
       final Semaphore outstandingRequests,
       final CategoricalTracker resultCodes,
       final IncrementalTracker modsCompleted,
       final IncrementalTracker modsExceedingThreshold,
       final TimeTracker modTimer)
  {
    this.responseTimeThresholdMillis = responseTimeThresholdMillis;
    this.outstandingRequests         = outstandingRequests;
    this.resultCodes                 = resultCodes;
    this.modsCompleted               = modsCompleted;
    this.modsExceedingThreshold      = modsExceedingThreshold;
    this.modTimer                    = modTimer;

    createTimeMillis = System.currentTimeMillis();
  }



  /**
   * Indicates that the associated modify operation has completed.
   *
   * @param  id      The asynchronous request ID for the search.
   * @param  result  The result received for the operation.
   */
  public void ldapResultReceived(final AsyncRequestID id,
                                 final LDAPResult result)
  {
    final long elapsedTime = System.currentTimeMillis() - createTimeMillis;

    if (resultCodes != null)
    {
      resultCodes.increment(result.getResultCode().toString());
    }

    if (modsCompleted != null)
    {
      modsCompleted.increment();
    }

    if ((modsExceedingThreshold != null) &&
        (responseTimeThresholdMillis > 0L) &&
        (elapsedTime > responseTimeThresholdMillis))
    {
      modsExceedingThreshold.increment();
    }

    if (modTimer != null)
    {
      modTimer.updateTimer((int) elapsedTime);
    }

    if (outstandingRequests != null)
    {
      outstandingRequests.release();
    }
  }
}
