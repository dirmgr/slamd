/*
 * Copyright 2009-2010 UnboundID Corp.
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
 * Portions created by Neil A. Wilson are Copyright (C) 2009-2010.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

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

import com.unboundid.ldap.sdk.CompareRequest;
import com.unboundid.ldap.sdk.CompareResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a SLAMD job class that may be used to perform compare
 * operations against an LDAP directory server.
 */
public class LDAPCompareRateJobClass
       extends LDAPJobClass
{
  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_RESULT_CODES = "Result Codes";



  /**
   * The display name for the stat tracker used to track compare durations.
   */
  private static final String STAT_COMP_DURATION = "Compare Duration (ms)";



  /**
   * The display name for the stat tracker used to track compare operations
   * completed.
   */
  private static final String STAT_COMPS_COMPLETED = "Compares Completed";



  /**
   * The display name for the stat tracker used to track compare operations
   * exceeding the response time threshold.
   */
  private static final String STAT_COMPS_EXCEEDING_THRESHOLD =
       "Compare Operations Exceeding Response Time Threshold";



  // Variables used to hold the values of the parameters.
  private static int    coolDownTime;
  private static int    dn1Percentage;
  private static int    responseTimeThreshold;
  private static int    warmUpTime;
  private static long   timeBetweenRequests;
  private static String attributeName;
  private static String assertionValue;
  private static String entryDN1;
  private static String entryDN2;

  // Stat trackers used by this job.
  private CategoricalTracker resultCodes;
  private IncrementalTracker comparesCompleted;
  private IncrementalTracker comparesExceedingThreshold;
  private TimeTracker        compareTimer;

  // The random number generators to use to select which pattern to use.
  private static Random parentRandom;
  private Random random;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // Value patterns used for the entry DNs.
  private static ValuePattern dn1Pattern;
  private static ValuePattern dn2Pattern;

  // The LDAP connection used by this thread.
  private LDAPConnection conn;

  // The parameters used by this job.
  private IntegerParameter coolDownParameter = new IntegerParameter(
       "coolDownTime", "Cool Down Time",
       "The length of time in seconds to continue running after ending " +
            "statistics collection.",
       true, 0, true, 0, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Compare Rate (Compares/Second/Client)",
       "Specifies the maximum operation rate (in compares per " +
            "second per client) to attempt to maintain.  If multiple clients " +
            "are used, then each client will attempt to maintain this rate.  " +
            "A value less than or equal to zero indicates that the client " +
            "should attempt to perform compares as quickly as possible.",
       true, -1);
  private IntegerParameter percentageParameter = new IntegerParameter(
       "percentage", "DN 1 Percentage",
       "The percentage of the compare operations which should use the first " +
            "DN pattern.",
       true, 50, true, 0, true, 100);
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
  private IntegerParameter warmUpParameter = new IntegerParameter(
       "warmUpTime", "Warm Up Time",
       "The length of time in seconds to run before beginning to collect " +
            "statistics.",
       true, 0, true, 0, false, 0);
  private StringParameter attributeParameter = new StringParameter("attribute",
       "Attribute to Compare",
       "The name of the attribute to target with the compare operations.", true,
       "description");
  private StringParameter valueParameter = new StringParameter("value",
       "Assertion Value",
       "The assertion value to use for the compare operations.", true, "");
  private StringParameter dn1Parameter = new StringParameter("dn1",
       "Entry DN 1",
       "The target DN for compare operations that fall into the first " +
            "category.",
       true, null);
  private StringParameter dn2Parameter = new StringParameter("dn2",
       "Entry DN 2",
       "The target DN for compare operations that fall into the second " +
            "category.",
       true, null);



  /**
   * Creates a new instance of this job class.
   */
  public LDAPCompareRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Compare Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP compare operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated compare operations against " +
      "an LDAP directory server.  Each compare operation will use the " +
      "provided attribute name and assertion value."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected List<Parameter> getNonLDAPParameterStubs()
  {
    return Arrays.asList(
         new PlaceholderParameter(),
         dn1Parameter,
         dn2Parameter,
         percentageParameter,
         new PlaceholderParameter(),
         attributeParameter,
         valueParameter,
         new PlaceholderParameter(),
         warmUpParameter,
         coolDownParameter,
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
      new IncrementalTracker(clientID, threadID, STAT_COMPS_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_COMP_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_COMPS_EXCEEDING_THRESHOLD,
                             collectionInterval)
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
        comparesCompleted,
        compareTimer,
        resultCodes,
        comparesExceedingThreshold
      };
    }
    else
    {
      return new StatTracker[]
      {
        comparesCompleted,
        compareTimer,
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
    // The DN parameters must be parseable as value patterns.
    StringParameter p =
         parameters.getStringParameter(dn1Parameter.getName());
    if ((p != null) && p.hasValue())
    {
      try
      {
        new ValuePattern(p.getValue());
      }
      catch (ParseException pe)
      {
        throw new InvalidValueException("The value provided for the '" +
             p.getDisplayName() + "' parameter is not a valid value " +
             "pattern:  " + pe.getMessage(), pe);
      }
    }

    p = parameters.getStringParameter(dn2Parameter.getName());
    if ((p != null) && p.hasValue())
    {
      try
      {
        new ValuePattern(p.getValue());
      }
      catch (ParseException pe)
      {
        throw new InvalidValueException("The value provided for the '" +
             p.getDisplayName() + "' parameter is not a valid value " +
             "pattern:  " + pe.getMessage(), pe);
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected void initializeClientNonLDAP(final String clientID,
                                         final ParameterList parameters)
            throws UnableToRunException
  {
    parentRandom = new Random();

    dn1Parameter = parameters.getStringParameter(dn1Parameter.getName());
    entryDN1 = dn1Parameter.getStringValue();


    dn2Parameter = parameters.getStringParameter(dn2Parameter.getName());
    entryDN2 = dn2Parameter.getStringValue();


    dn1Percentage = 50;
    percentageParameter =
         parameters.getIntegerParameter(percentageParameter.getName());
    if ((percentageParameter != null) && percentageParameter.hasValue())
    {
      dn1Percentage = percentageParameter.getIntValue();
    }


    attributeParameter =
         parameters.getStringParameter(attributeParameter.getName());
    attributeName = attributeParameter.getStringValue();


    valueParameter =
         parameters.getStringParameter(valueParameter.getName());
    assertionValue = valueParameter.getStringValue();


    warmUpTime = 0;
    warmUpParameter = parameters.getIntegerParameter(warmUpParameter.getName());
    if ((warmUpParameter != null) && warmUpParameter.hasValue())
    {
      warmUpTime = warmUpParameter.getIntValue();
    }


    coolDownTime = 0;
    coolDownParameter =
         parameters.getIntegerParameter(coolDownParameter.getName());
    if ((coolDownParameter != null) && coolDownParameter.hasValue())
    {
      coolDownTime = coolDownParameter.getIntValue();
    }


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

    try
    {
      dn1Pattern = new ValuePattern(entryDN1);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to parse DN pattern 1:  " +
                                     stackTraceToString(e), e);
    }

    try
    {
      dn2Pattern = new ValuePattern(entryDN2);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to parse DN pattern 2:  " +
                                     stackTraceToString(e), e);
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
    random = new Random(parentRandom.nextLong());

    comparesCompleted = new IncrementalTracker(clientID, threadID,
         STAT_COMPS_COMPLETED, collectionInterval);
    compareTimer = new TimeTracker(clientID, threadID, STAT_COMP_DURATION,
         collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);
    comparesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_COMPS_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      comparesCompleted.enableRealTimeStats(statReporter, jobID);
      compareTimer.enableRealTimeStats(statReporter, jobID);
      comparesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
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
    // Figure out when to start and stop collecting statistics.
    long stopCollectingTime = Long.MAX_VALUE;
    if ((coolDownTime > 0) && (getShouldStopTime() > 0L))
    {
      stopCollectingTime = getShouldStopTime() - (1000L * coolDownTime);
    }

    boolean collectingStats;
    long startCollectingTime = 0L;
    if (warmUpTime > 0)
    {
      collectingStats = false;
      startCollectingTime = System.currentTimeMillis() + (1000L * warmUpTime);
    }
    else
    {
      collectingStats = true;
      startTrackers();
    }


    // Create the base compare request.
    CompareRequest compareRequest =
         new CompareRequest("", attributeName, assertionValue);


    // Perform the compare operations until it's time to stop.
    boolean doneCollecting = false;
    while (! shouldStop())
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      // See if it's time to change the tracking state.
      long compareStartTime = System.currentTimeMillis();
      if (collectingStats && (coolDownTime > 0) &&
          (compareStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (compareStartTime >= startCollectingTime))
      {
        collectingStats = true;
        startTrackers();
      }


      // Get the DN of the entry to target with the compare.
      if (random.nextInt(100) < dn1Percentage)
      {
        compareRequest.setDN(dn1Pattern.nextValue());
      }
      else
      {
        compareRequest.setDN(dn2Pattern.nextValue());
      }


      // Process the compare operation.
      if (collectingStats)
      {
        compareTimer.startTimer();
      }

      try
      {
        CompareResult result = conn.compare(compareRequest);
        if (collectingStats)
        {
          resultCodes.increment(result.getResultCode().toString());
        }
      }
      catch (LDAPException le)
      {
        if (collectingStats)
        {
          resultCodes.increment(le.getResultCode().toString());
        }
      }
      finally
      {
        if (collectingStats)
        {
          compareTimer.stopTimer();
          comparesCompleted.increment();

          if ((responseTimeThreshold > 0) &&
              (compareTimer.getLastOperationTime() > responseTimeThreshold))
          {
            comparesExceedingThreshold.increment();
          }
        }
      }


      // Sleep if necessary before the next request.
      if (timeBetweenRequests > 0L)
      {
        long elapsedTime = System.currentTimeMillis() - compareStartTime;
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


    // Stop collecting statistics if the trackers are still active.
    if (collectingStats)
    {
      stopTrackers();
      collectingStats = false;
    }
  }



  /**
   * Starts the stat trackers for this job.
   */
  private void startTrackers()
  {
    comparesCompleted.startTracker();
    compareTimer.startTracker();
    resultCodes.startTracker();
    comparesExceedingThreshold.startTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    comparesCompleted.stopTracker();
    compareTimer.stopTracker();
    resultCodes.stopTracker();
    comparesExceedingThreshold.stopTracker();
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
