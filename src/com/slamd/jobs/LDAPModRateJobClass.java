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



import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiLineTextParameter;
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
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a SLAMD job class that may be used to perform
 * modifications against an LDAP directory server.
 */
public class LDAPModRateJobClass
       extends LDAPJobClass
{
  /**
   * The set of characters to include in the values to use for the target
   * attributes.
   */
  private static final String DEFAULT_CHARACTER_SET =
       "abcdefghijklmnopqrstuvwxyz";



  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_RESULT_CODES = "Result Codes";



  /**
   * The display name for the stat tracker used to track modify durations.
   */
  private static final String STAT_MOD_DURATION = "Modify Duration (ms)";



  /**
   * The display name for the stat tracker used to track modifications
   * completed.
   */
  private static final String STAT_MODS_COMPLETED = "Modifications Completed";



  /**
   * The display name for the stat tracker used to track modifications exceeding
   * the response time threshold.
   */
  private static final String STAT_MODS_EXCEEDING_THRESHOLD =
       "Modifications Exceeding Response Time Threshold";



  // Variables used to hold the values of the parameters.
  private static char[]   characterSet;
  private static int      coolDownTime;
  private static int      dn1Percentage;
  private static int      responseTimeThreshold;
  private static int      warmUpTime;
  private static int      valueLength;
  private static long     timeBetweenRequests;
  private static String   entryDN1;
  private static String   entryDN2;
  private static String[] modAttributes;

  // Stat trackers used by this job.
  private CategoricalTracker resultCodes;
  private IncrementalTracker modsCompleted;
  private IncrementalTracker modsExceedingThreshold;
  private TimeTracker        modTimer;

  // Random number generators used by this job.
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
  private IntegerParameter lengthParameter = new IntegerParameter("length",
       "Value Length",
       "The number of characters to include in generated values.", true, 80,
       true, 1, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Modification Rate (Mods/Second/Client)",
       "Specifies the maximum modification rate (in mods per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform modifications as quickly as possible.",
       true, -1);
  private IntegerParameter percentageParameter = new IntegerParameter(
       "percentage", "DN 1 Percentage",
       "The percentage of the modifications which should use the first DN " +
            "pattern.",
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
  private MultiLineTextParameter attributesParameter =
       new MultiLineTextParameter( "modAttributes", "Attribute(s) to Modify",
            "The set of attributes to modify in matching entries.  If " +
                 "multiple attribute names are provided, then they must be " +
                 "provided on separate lines and each of those attributes " +
                 "will be replaced with the same value generated for that " +
                 "entry.",
            new String[] { "description" }, true);
  private StringParameter characterSetParameter =
       new StringParameter("characterSet", "Character Set",
                "The set of characters to include in generated values used " +
                     "for the modifications.",
                true, DEFAULT_CHARACTER_SET);
  private StringParameter dn1Parameter = new StringParameter("dn1",
       "Entry DN 1",
       "The target DN for modifications that fall into the first category.",
       true, null);
  private StringParameter dn2Parameter = new StringParameter("dn2",
       "Entry DN 2",
       "The target DN for modifications that fall into the second category.",
       true, null);



  /**
   * Creates a new instance of this job class.
   */
  public LDAPModRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP ModRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP modify operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated modifications against an " +
      "LDAP directory server.  Each modification will replace the values for " +
      "the specified attribute(s) with a random string of the specified " +
      "number of ASCII alphabetic characters."
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
         attributesParameter,
         characterSetParameter,
         lengthParameter,
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
      new IncrementalTracker(clientID, threadID, STAT_MODS_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_MOD_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_MODS_EXCEEDING_THRESHOLD,
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
        modsCompleted,
        modTimer,
        resultCodes,
        modsExceedingThreshold
      };
    }
    else
    {
      return new StatTracker[]
      {
        modsCompleted,
        modTimer,
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


    attributesParameter =
         parameters.getMultiLineTextParameter(attributesParameter.getName());
    modAttributes = attributesParameter.getNonBlankLines();


    characterSet = DEFAULT_CHARACTER_SET.toCharArray();
    characterSetParameter =
         parameters.getStringParameter(characterSetParameter.getName());
    if ((characterSetParameter != null) && characterSetParameter.hasValue())
    {
      characterSet = characterSetParameter.getStringValue().toCharArray();
    }


    valueLength = 80;
    lengthParameter = parameters.getIntegerParameter(lengthParameter.getName());
    if ((lengthParameter != null) && lengthParameter.hasValue())
    {
      valueLength = lengthParameter.getIntValue();
    }


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
    modsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_MODS_COMPLETED, collectionInterval);
    modTimer = new TimeTracker(clientID, threadID, STAT_MOD_DURATION,
         collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);
    modsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_MODS_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      modsCompleted.enableRealTimeStats(statReporter, jobID);
      modTimer.enableRealTimeStats(statReporter, jobID);
      modsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
    }

    random = new Random(parentRandom.nextLong());

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
    StringBuilder value = new StringBuilder(valueLength);

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


    // Perform the modifications until it's time to stop.
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
      long modStartTime = System.currentTimeMillis();
      if (collectingStats && (coolDownTime > 0) &&
          (modStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (modStartTime >= startCollectingTime))
      {
        collectingStats = true;
        startTrackers();
      }


      // Get the DN of the entry to modify.
      String dn;
      if (random.nextInt(100) < dn1Percentage)
      {
        dn = dn1Pattern.nextValue();
      }
      else
      {
        dn = dn2Pattern.nextValue();
      }


      // Create the modification.
      value.setLength(0);
      for (int i=0; i < valueLength; i++)
      {
        value.append(characterSet[random.nextInt(characterSet.length)]);
      }
      String valueStr = value.toString();

      Modification[] mods = new Modification[modAttributes.length];
      for (int i=0; i < modAttributes.length; i++)
      {
        mods[i] = new Modification(ModificationType.REPLACE, modAttributes[i],
                                   valueStr);
      }


      // Process the modification.
      if (collectingStats)
      {
        modTimer.startTimer();
      }

      try
      {
        conn.modify(dn, mods);
        if (collectingStats)
        {
          resultCodes.increment(ResultCode.SUCCESS.toString());
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
          modTimer.stopTimer();
          modsCompleted.increment();

          if ((responseTimeThreshold > 0) &&
              (modTimer.getLastOperationTime() > responseTimeThreshold))
          {
            modsExceedingThreshold.increment();
          }
        }
      }


      // Sleep if necessary before the next request.
      if (timeBetweenRequests > 0L)
      {
        long elapsedTime = System.currentTimeMillis() - modStartTime;
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
    modsCompleted.startTracker();
    modTimer.startTracker();
    resultCodes.startTracker();
    modsExceedingThreshold.startTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    modsCompleted.stopTracker();
    modTimer.stopTracker();
    resultCodes.stopTracker();
    modsExceedingThreshold.stopTracker();
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
