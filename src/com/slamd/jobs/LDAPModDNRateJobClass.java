/*
 * Copyright 2008-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2008-2010.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.util.FixedRateBarrier;



/**
 * This class provides a SLAMD job class that may be used to perform modify DN
 * operations against an LDAP directory server.
 */
public class LDAPModDNRateJobClass
       extends LDAPJobClass
{
  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_RESULT_CODES = "Result Codes";



  /**
   * The display name for the stat tracker used to track operation durations.
   */
  private static final String STAT_DURATION = "Modify DN Duration (ms)";



  /**
   * The display name for the stat tracker used to track operations completed.
   */
  private static final String STAT_COMPLETED = "Modify DN Operations Completed";



  /**
   * The display name for the stat tracker used to track operations exceeding
   * the response time threshold.
   */
  private static final String STAT_EXCEEDING_THRESHOLD =
       "Modify DN Operations Exceeding Response Time Threshold";



  // Variables used to hold the values of the parameters.
  private static int      lowerBound;
  private static int      responseTimeThreshold;
  private static int      upperBound;
  private static long     timeBetweenRequests;
  private static String   parentDN;
  private static String   rdnAttribute;
  private static String   rdnValuePrefix;
  private static String   rdnValueSuffix;

  // Stat trackers used by this job.
  private CategoricalTracker resultCodes;
  private IncrementalTracker modDNsCompleted;
  private IncrementalTracker modDNsExceedingThreshold;
  private TimeTracker        modDNTimer;

  // The number of threads that are currently in the first pass.
  private static AtomicInteger activeThreads;

  // The entry number counter for this job.
  private static AtomicInteger entryNumber;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // The LDAP connection used by this thread.
  private LDAPConnection conn;

  // The parameters used by this job.
  private IntegerParameter lowerBoundParameter = new IntegerParameter(
       "lowerBound", "Lower Bound",
       "The lower bound of the numeric portion of the RDN value.", true, 0,
       true, 0, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Operation Rate (Ops/Second)",
       "Specifies the maximum operation rate (in operations per second) to " +
            "attempt to maintain.  A value less than or equal to zero " +
            "indicates that the client should attempt to process operations " +
            "as quickly as possible.",
       true, -1);
  private IntegerParameter rateLimitDurationParameter = new IntegerParameter(
       "maxRateDuration", "Max Rate Enforcement Interval (Seconds)",
       "Specifies the duration in seconds of the interval over which  to " +
            "attempt to maintain the configured maximum rate.  A value of " +
            "zero indicates that it should be equal to the statistics " +
            "collection interval.  Large values may allow more variation but " +
            "may be more accurate over time.  Small values can better " +
            "ensure that the rate doesn't exceed the requested level but may " +
            "be less able to achieve the desired rate.",
       true, 0, true,0, false, 0);
  private IntegerParameter thresholdParameter = new IntegerParameter(
       "threshold", "Response Time Threshold (ms)",
       "Specifies a threshold in milliseconds for which to count the number " +
            "of operations that take longer than this time to complete.  A " +
            "value less than or equal to zero indicates that there will not " +
            "be any threshold.",
       false, -1);
  private IntegerParameter timeBetweenRequestsParameter = new IntegerParameter(
       "timeBetweenRequests", "Time Between Requests (ms)",
       "The minimum length of time in milliseconds that should pass between " +
            "the beginning of one request and the beginning of the next.",
       false, 0, true, 0, false, 0);
  private IntegerParameter upperBoundParameter = new IntegerParameter(
       "upperBound", "Upper Bound",
       "The upper bound of the numeric portion of the RDN value.", true, 0,
       true, 0, false, 0);
  private StringParameter parentDNParameter = new StringParameter("parentDN",
       "Parent DN", "The parent DN for all entries to be renamed.", true, null);
  private StringParameter rdnAttributeParameter = new StringParameter("rdnAttr",
       "RDN Attribute",
       "The name of the RDN attribute for the entries to be renamed.", true,
       "uid");
  private StringParameter rdnPrefixParameter = new StringParameter("rdnPrefix",
       "RDN Value Prefix",
       "The string (if any) that appears before the numeric portion of the " +
            "RDN value.",
       false, null);
  private StringParameter rdnSuffixParameter = new StringParameter("rdnSuffix",
       "RDN Value Suffix",
       "The string (if any) that appears after the numeric portion of the " +
            "RDN value.",
       false, null);



  /**
   * Creates a new instance of this job class.
   */
  public LDAPModDNRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Modify DN Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP modify DN operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated modify DN operations against " +
      "an LDAP directory server.  Each entry in the range will be processed " +
      "twice.  The first set of modify DN operations will rename all entries " +
      "in the specified range to append the job ID to the current value, and " +
      "second set of operations will remove the job ID so that the entries " +
      "are renamed back to their original names."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int overrideNumClients()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected List<Parameter> getNonLDAPParameterStubs()
  {
    return Arrays.asList(
         new PlaceholderParameter(),
         rdnAttributeParameter,
         rdnPrefixParameter,
         lowerBoundParameter,
         upperBoundParameter,
         rdnSuffixParameter,
         parentDNParameter,
         new PlaceholderParameter(),
         thresholdParameter,
         maxRateParameter,
         rateLimitDurationParameter,
         timeBetweenRequestsParameter);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(final String clientID,
                                           final String threadID,
                                           final int collectionInterval)
  {
    return new StatTracker[]
    {
      new IncrementalTracker(clientID, threadID, STAT_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_EXCEEDING_THRESHOLD,
                             collectionInterval),
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    if (responseTimeThreshold > 0)
    {
      return new StatTracker[]
      {
        modDNsCompleted,
        modDNTimer,
        resultCodes,
        modDNsExceedingThreshold
      };
    }
    else
    {
      return new StatTracker[]
      {
        modDNsCompleted,
        modDNTimer,
        resultCodes
      };
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected void validateNonLDAPJobInfo(final int numClients,
                                        final int threadsPerClient,
                                        final int threadStartupDelay,
                                        final Date startTime,
                                        final Date stopTime,
                                        final int duration,
                                        final int collectionInterval,
                                        final ParameterList parameters)
            throws InvalidValueException
  {
    // Make sure that neither a stop time nor a duration were specified.
    if ((stopTime != null) || (duration > 0))
    {
      throw new InvalidValueException("Neither a stop time nor a duration " +
           "may be defined when scheduling this job.");
    }


    // Make sure that the upper bound is greater than the lower bound.
    IntegerParameter lowerParam =
         parameters.getIntegerParameter(lowerBoundParameter.getName());
    IntegerParameter upperParam =
         parameters.getIntegerParameter(upperBoundParameter.getName());
    if (lowerParam.getIntValue() >= upperParam.getIntValue())
    {
      throw new InvalidValueException("The upper bound must be greater than " +
           "the lower bound.");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean testNonLDAPJobParameters(final ParameterList parameters,
                         final LDAPConnection connection,
                         final ArrayList<String> outputMessages)
  {
    boolean successful = true;

    // Ensure that the parent DN exists.
    StringParameter parentDNParam =
         parameters.getStringParameter(parentDNParameter.getName());
    if ((parentDNParam != null) && parentDNParam.hasValue())
    {
      try
      {
        String base = parentDNParam.getStringValue();
        outputMessages.add("Ensuring that parent entry '" + base +
                           "' exists....");
        SearchResultEntry e = connection.getEntry(base);
        if (e == null)
        {
          outputMessages.add("ERROR:  The parent entry does not exist.");
          successful = false;
        }
        else
        {
          outputMessages.add("The parent entry exists.");
        }
      }
      catch (Exception e)
      {
        successful = false;
        outputMessages.add("Unable to perform the search:  " +
                           stackTraceToString(e));
      }

      outputMessages.add("");
    }

    return successful;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected void initializeClientNonLDAP(final String clientID,
                                         final ParameterList parameters)
            throws UnableToRunException
  {
    activeThreads =
         new AtomicInteger(getClientSideJob().getThreadsPerClient());


    rdnAttributeParameter =
         parameters.getStringParameter(rdnAttributeParameter.getName());
    rdnAttribute = rdnAttributeParameter.getStringValue();


    rdnValuePrefix = "";
    rdnPrefixParameter =
         parameters.getStringParameter(rdnPrefixParameter.getName());
    if ((rdnPrefixParameter != null) && rdnPrefixParameter.hasValue())
    {
      rdnValuePrefix = rdnPrefixParameter.getStringValue();
    }


    lowerBoundParameter =
         parameters.getIntegerParameter(lowerBoundParameter.getName());
    lowerBound = lowerBoundParameter.getIntValue();
    entryNumber = new AtomicInteger(lowerBound);


    upperBoundParameter =
         parameters.getIntegerParameter(upperBoundParameter.getName());
    upperBound = upperBoundParameter.getIntValue();


    rdnValueSuffix = "";
    rdnSuffixParameter =
         parameters.getStringParameter(rdnSuffixParameter.getName());
    if ((rdnSuffixParameter != null) && rdnSuffixParameter.hasValue())
    {
      rdnValueSuffix = rdnSuffixParameter.getStringValue();
    }


    parentDNParameter =
         parameters.getStringParameter(parentDNParameter.getName());
    parentDN = parentDNParameter.getStringValue();


    responseTimeThreshold = -1;
    thresholdParameter =
         parameters.getIntegerParameter(thresholdParameter.getName());
    if ((thresholdParameter != null) && thresholdParameter.hasValue())
    {
      responseTimeThreshold = thresholdParameter.getIntValue();
    }


    timeBetweenRequests = 0L;
    timeBetweenRequestsParameter =
         parameters.getIntegerParameter(timeBetweenRequestsParameter.getName());
    if ((timeBetweenRequestsParameter != null) &&
        timeBetweenRequestsParameter.hasValue())
    {
      timeBetweenRequests = timeBetweenRequestsParameter.getIntValue();
    }


    rateLimiter = null;
    maxRateParameter =
         parameters.getIntegerParameter(maxRateParameter.getName());
    if ((maxRateParameter != null) && maxRateParameter.hasValue())
    {
      int maxRate = maxRateParameter.getIntValue();
      if (maxRate > 0)
      {
        int rateIntervalSeconds = 0;
        rateLimitDurationParameter = parameters.getIntegerParameter(
             rateLimitDurationParameter.getName());
        if ((rateLimitDurationParameter != null) &&
            rateLimitDurationParameter.hasValue())
        {
          rateIntervalSeconds = rateLimitDurationParameter.getIntValue();
        }

        if (rateIntervalSeconds <= 0)
        {
          rateIntervalSeconds = getClientSideJob().getCollectionInterval();
        }

        rateLimiter = new FixedRateBarrier(rateIntervalSeconds * 1000L,
             maxRate * rateIntervalSeconds);
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(final String clientID, final String threadID,
                               final int collectionInterval,
                               final ParameterList parameters)
         throws UnableToRunException
  {
    modDNsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_COMPLETED, collectionInterval);
    modDNTimer = new TimeTracker(clientID, threadID, STAT_DURATION,
         collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
         collectionInterval);
    modDNsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      modDNsCompleted.enableRealTimeStats(statReporter, jobID);
      modDNTimer.enableRealTimeStats(statReporter, jobID);
      modDNsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
    }

    try
    {
      conn = createConnection();
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to establish a connection to " +
           "the target server:  " + stackTraceToString(e), e);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void finalizeThread()
  {
    if (conn != null)
    {
      conn.close();
      conn = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    String jobID        = getJobID();
    String prefix       = rdnAttribute + '=' + rdnValuePrefix;
    String dnSuffix     = rdnValueSuffix + ',' + parentDN;
    String newRDNSuffix = rdnValueSuffix + '-' + jobID;

    StringBuilder dnBuffer = new StringBuilder();
    StringBuilder newRDNBuffer = new StringBuilder();

    modDNsCompleted.startTracker();
    modDNTimer.startTracker();
    resultCodes.startTracker();
    modDNsExceedingThreshold.startTracker();


    // Iterate through the entries to rename them to include the job ID.
    while (! shouldStop())
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      long opStartTime = System.currentTimeMillis();
      int i = entryNumber.getAndIncrement();
      if (i > upperBound)
      {
        break;
      }

      dnBuffer.setLength(0);
      dnBuffer.append(prefix);
      dnBuffer.append(i);
      dnBuffer.append(dnSuffix);

      newRDNBuffer.setLength(0);
      newRDNBuffer.append(prefix);
      newRDNBuffer.append(i);
      newRDNBuffer.append(newRDNSuffix);

      modDNTimer.startTimer();

      try
      {
        LDAPResult modDNResult = conn.modifyDN(dnBuffer.toString(),
                                               newRDNBuffer.toString(), true);
        resultCodes.increment(modDNResult.getResultCode().toString());
      }
      catch (LDAPException le)
      {
        resultCodes.increment(le.getResultCode().toString());
      }
      finally
      {
        modDNTimer.stopTimer();
        modDNsCompleted.increment();

        if ((responseTimeThreshold > 0) &&
            (modDNTimer.getLastOperationTime() > responseTimeThreshold))
        {
          modDNsExceedingThreshold.increment();
        }
      }

      if (timeBetweenRequests > 0)
      {
        long elapsedTime = System.currentTimeMillis() - opStartTime;
        long sleepTime   = timeBetweenRequests - elapsedTime;
        if (sleepTime > 0)
        {
          try
          {
            Thread.sleep(sleepTime);
          } catch (Exception e) {}
        }
      }
    }

    // Wait until all of the threads have completed the first modify DN before
    // starting the second.
    int remainingActive = activeThreads.decrementAndGet();
    if (remainingActive == 0)
    {
      entryNumber.set(lowerBound);
      activeThreads.decrementAndGet();
    }
    else
    {
      while (activeThreads.get() >= 0)
      {
        try
        {
          Thread.sleep(0, 1);
        } catch (Exception e) {}
      }
    }


    // Iterate through the entries to rename them to remove the job ID.
    dnSuffix     = rdnValueSuffix + '-' + jobID + ',' + parentDN;
    newRDNSuffix = rdnValueSuffix;
    while (! shouldStop())
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      long opStartTime = System.currentTimeMillis();
      int i = entryNumber.getAndIncrement();
      if (i > upperBound)
      {
        break;
      }

      dnBuffer.setLength(0);
      dnBuffer.append(prefix);
      dnBuffer.append(i);
      dnBuffer.append(dnSuffix);

      newRDNBuffer.setLength(0);
      newRDNBuffer.append(prefix);
      newRDNBuffer.append(i);
      newRDNBuffer.append(newRDNSuffix);

      modDNTimer.startTimer();

      try
      {
        LDAPResult modDNResult = conn.modifyDN(dnBuffer.toString(),
                                               newRDNBuffer.toString(), true);
        resultCodes.increment(modDNResult.getResultCode().toString());
      }
      catch (LDAPException le)
      {
        resultCodes.increment(le.getResultCode().toString());
      }
      finally
      {
        modDNTimer.stopTimer();
        modDNsCompleted.increment();

        if ((responseTimeThreshold > 0) &&
            (modDNTimer.getLastOperationTime() > responseTimeThreshold))
        {
          modDNsExceedingThreshold.increment();
        }
      }

      if (timeBetweenRequests > 0)
      {
        long elapsedTime = System.currentTimeMillis() - opStartTime;
        long sleepTime   = timeBetweenRequests - elapsedTime;
        if (sleepTime > 0)
        {
          try
          {
            Thread.sleep(sleepTime);
          } catch (Exception e) {}
        }
      }
    }

    modDNsCompleted.stopTracker();
    modDNTimer.stopTracker();
    resultCodes.stopTracker();
    modDNsExceedingThreshold.stopTracker();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void destroyThread()
  {
    if (conn != null)
    {
      conn.close();
      conn = null;
    }
  }
}
