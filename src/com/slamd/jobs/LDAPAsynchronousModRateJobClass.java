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
 * Portions created by Neil A. Wilson are Copyright (C) 2008-2010.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ValuePattern;

import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class provides a SLAMD job class that may be used to perform
 * asynchronous modify operations against an LDAP directory server.  Each client
 * should have a single thread that will be used to send requests to the server.
 * That thread can have any number of connections, and requests will be sent
 * across those connections in a round-robin manner.  The rate at which requests
 * are sent to the server can be controlled in either or both of the following
 * ways:
 * <UL>
 *   <LI>You can specify a maximum request rate (in terms of the maximum number
 *       of requests per second) that the client should maintain.  Note that
 *       this will only be the maximum request rate and will not have any
 *       relation to the response rate, so if you specify a request rate that is
 *       higher than the response rate, then you will likely generate a
 *       significant backlog in the directory server.</LI>
 *   <LI>You can specify the maximum number of outstanding requests to allow at
 *       any given time.  The client will send requests repeatedly to the server
 *       until the maximum number of outstanding requests is met, at which point
 *       it will only send a new request whenever it receives a response from an
 *       earlier request.</LI>
 * </UL>
 */
public final class LDAPAsynchronousModRateJobClass
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
   * The display name for the stat tracker used to track modifies completed.
   */
  private static final String STAT_MODS_COMPLETED = "Modifies Completed";



  /**
   * The display name for the stat tracker used to track modifies exceeding the
   * response time threshold.
   */
  private static final String STAT_MODS_EXCEEDING_THRESHOLD =
       "Mods Exceeding Response Time Threshold";



  /**
   * The connection selection mode that indicates connections should be selected
   * based on the fewest outstanding operations.
   */
  private static final String SELECT_MODE_STR_FEWEST_OPS = "Fewest Operations";



  /**
   * The connection selection mode that indicates connections should be selected
   * in a round-robin manner.
   */
  private static final String SELECT_MODE_STR_ROUND_ROBIN = "Round Robin";



  /**
   * The set of defined connection selection modes.
   */
  private static final String[] SELECT_MODES =
  {
    SELECT_MODE_STR_FEWEST_OPS,
    SELECT_MODE_STR_ROUND_ROBIN
  };



  // Variables used to hold the values of the parameters.
  private boolean  useRoundRobin;
  private char[]   characterSet;
  private int      coolDownTime;
  private int      dn1Percentage;
  private int      responseTimeThreshold;
  private int      valueLength;
  private int      warmUpTime;
  private String[] modAttributes;

  // Stat trackers used by this job.  We will maintain a separate set of
  // statistics per connection, and then will merge them all before returning
  // them to the server.
  private CategoricalTracker[]  resultCodes;
  private IncrementalTracker [] modsCompleted;
  private IncrementalTracker[]  modsExceedingThreshold;
  private TimeTracker[]         modTimers;

  // The random number generator used for this job.
  private Random random;

  // The semaphore used to control the maximum number of outstanding requests.
  private Semaphore outstandingRequests;

  // The request rate limiter for this job.
  private FixedRateBarrier rateLimiter;

  // Value patterns used for the target entry DNs.
  private ValuePattern dn1Pattern;
  private ValuePattern dn2Pattern;

  // The set of LDAP connections that will be used by this thread.
  private LDAPConnection[] conns;

  // A counter that will be used to determine which connection to use for the
  // next request.
  private int nextConnSlot;

  // The parameters used by this job.
  private IntegerParameter connsPerClientParameter = new IntegerParameter(
       "connsPerClient", "Connections per Client",
       "Specifies the number of connections to establish per client.  " +
            "Requests will be spread across the client connections in a " +
            "round-robin manner",
       true, 1, true, 1, false, 0);
  private IntegerParameter coolDownParameter = new IntegerParameter(
       "coolDownTime", "Cool Down Time",
       "The length of time in seconds to continue running after ending " +
            "statistics collection.",
       true, 0, true, 0, false, 0);
  private IntegerParameter lengthParameter = new IntegerParameter("length",
       "Value Length",
       "The number of characters to include in generated values.", true, 80,
       true, 1, false, 0);
  private IntegerParameter maxOutstandingRequestsParameter =
       new IntegerParameter("maxOutstandingRequests",
            "Max Outstanding Requests",
            "Specifies the maximum number of outstanding modify requests to " +
                 "allow at any given time for each client.  If multiple " +
                 "clients are used, then each client will be allowed to " +
                 "have up to this many concurrent outstanding requests.  A " +
                 "value less than or equal to zero indicates that no " +
                 "limit should be enforced.",
            true, -1);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Request Rate (Requests/Second/Client)",
       "Specifies the maximum rate (in requests per second per client) to " +
            "attempt to maintain.  If multiple clients are used, then each " +
            "client will attempt to maintain this rate.  A value less than " +
            "or equal to zero indicates that the client should attempt to " +
            "perform modifications as quickly as possible.",
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
  private IntegerParameter warmUpParameter = new IntegerParameter(
       "warmUpTime", "Warm Up Time",
       "The length of time in seconds to run before beginning to collect " +
            "statistics.",
       true, 0, true, 0, false, 0);
  private MultiChoiceParameter selectModeParameter = new MultiChoiceParameter(
       "selectionMode", "Connection Selection Mode",
       "The algorithm to use to select which connection should be used for " +
            "each operation.  A value of 'Fewest Operations' will cause the " +
            "connection with the fewest outstanding operations to be " +
            "selected.  A value of 'Round Robin' will cause connection to be " +
            "selected in a round-robin manner.", SELECT_MODES,
       SELECT_MODE_STR_FEWEST_OPS);
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
  public LDAPAsynchronousModRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Asynchronous ModRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated asynchronous LDAP modify operations";
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
      "LDAP directory server using asynchronous operations."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int overrideThreadsPerClient()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean useSynchronousMode()
  {
    return false;
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
         connsPerClientParameter,
         selectModeParameter,
         warmUpParameter,
         coolDownParameter,
         thresholdParameter,
         maxOutstandingRequestsParameter,
         maxRateParameter,
         rateLimitDurationParameter);
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
      new IncrementalTracker(clientID, threadID,
           STAT_MODS_EXCEEDING_THRESHOLD, collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    final ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>(5);

    final int    collectionInterval = getCollectionInterval();
    final String clientID           = getClientID();
    final String threadID           = getThreadID();

    final IncrementalTracker aggregateModsCompleted =
         new IncrementalTracker(clientID, threadID, STAT_MODS_COMPLETED,
              collectionInterval);
    aggregateModsCompleted.aggregate(modsCompleted);
    trackerList.add(aggregateModsCompleted);

    final TimeTracker aggregateModTimer = new TimeTracker(clientID, threadID,
         STAT_MOD_DURATION, collectionInterval);
    aggregateModTimer.aggregate(modTimers);
    trackerList.add(aggregateModTimer);

    final CategoricalTracker aggregateResultCodes = new CategoricalTracker(
         clientID, threadID, STAT_RESULT_CODES, collectionInterval);
    aggregateResultCodes.aggregate(resultCodes);
    trackerList.add(aggregateResultCodes);

    if (responseTimeThreshold > 0)
    {
      final IncrementalTracker aggregateModsExceedingThreshold =
           new IncrementalTracker(clientID, threadID,
                STAT_MODS_EXCEEDING_THRESHOLD, collectionInterval);
      aggregateModsExceedingThreshold.aggregate(modsExceedingThreshold);
      trackerList.add(aggregateModsExceedingThreshold);
    }


    final StatTracker[] trackers = new StatTracker[trackerList.size()];
    return trackerList.toArray(trackers);
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
    // A maximum number of outstanding requests or a max request rate (or both)
    // must have been specified.
    int maxOutstanding = -1;
    final IntegerParameter maxOutstandingParam = parameters.getIntegerParameter(
         maxOutstandingRequestsParameter.getName());
    if ((maxOutstandingParam != null) && maxOutstandingParam.hasValue())
    {
      maxOutstanding = maxOutstandingParam.getIntValue();
    }

    int maxRate = -1;
    final IntegerParameter maxRateParam = parameters.getIntegerParameter(
         maxRateParameter.getName());
    if ((maxRateParam != null) && maxRateParam.hasValue())
    {
      maxRate = maxRateParam.getIntValue();
    }

    if ((maxOutstanding <= 0) && (maxRate <= 0))
    {
      throw new InvalidValueException("Either the maximum number of " +
           "outstanding requests parameter or the maximum request rate " +
           "parameter (or both) must have a positive value.");
    }


    // The entry DN parameters must be parseable as value patterns.
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
  public void initializeThread(final String clientID, final String threadID,
                               final int collectionInterval,
                               final ParameterList parameters)
         throws UnableToRunException
  {
    random = new Random();


    dn1Parameter = parameters.getStringParameter(dn1Parameter.getName());
    final String entryDN1 = dn1Parameter.getStringValue();


    dn2Parameter = parameters.getStringParameter(dn2Parameter.getName());
    final String entryDN2 = dn2Parameter.getStringValue();


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


    rateLimiter = null;
    maxRateParameter =
         parameters.getIntegerParameter(maxRateParameter.getName());
    if ((maxRateParameter != null) && maxRateParameter.hasValue())
    {
      final int maxRate = maxRateParameter.getIntValue();
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


    outstandingRequests = null;
    maxOutstandingRequestsParameter = parameters.getIntegerParameter(
         maxOutstandingRequestsParameter.getName());
    if ((maxOutstandingRequestsParameter != null) &&
        maxOutstandingRequestsParameter.hasValue())
    {
      final int maxOutstandingRequests =
           maxOutstandingRequestsParameter.getIntValue();
      if (maxOutstandingRequests > 0)
      {
        outstandingRequests = new Semaphore(maxOutstandingRequests, true);
      }
    }


    int numConns = -1;
    nextConnSlot = 0;
    conns = null;
    connsPerClientParameter = parameters.getIntegerParameter(
         connsPerClientParameter.getName());
    if ((connsPerClientParameter != null) && connsPerClientParameter.hasValue())
    {
      numConns = connsPerClientParameter.getIntValue();
      conns = new LDAPConnection[numConns];
      for (int i=0; i < conns.length; i++)
      {
        try
        {
          conns[i] = createConnection();
        }
        catch (final LDAPException le)
        {
          for (int j=0; j < (i-1); j++)
          {
            conns[j].close();
          }
          throw new UnableToRunException("An error occurred while attempting " +
               "to establish an LDAP connection:  " +
               StaticUtils.getExceptionMessage(le), le);
        }
      }
    }


    useRoundRobin = false;
    selectModeParameter =
         parameters.getMultiChoiceParameter(selectModeParameter.getName());
    if ((selectModeParameter != null) && selectModeParameter.hasValue())
    {
      final String selectModeStr = selectModeParameter.getStringValue();
      if (selectModeStr.equalsIgnoreCase(SELECT_MODE_STR_ROUND_ROBIN))
      {
        useRoundRobin = true;
      }
    }


    modsCompleted = new IncrementalTracker[numConns];
    for (int i=0; i < numConns; i++)
    {
      modsCompleted[i] = new IncrementalTracker(clientID,
           threadID + '-' + i, STAT_MODS_COMPLETED, collectionInterval);
    }

    modTimers = new TimeTracker[numConns];
    for (int i=0; i < numConns; i++)
    {
      modTimers[i] = new TimeTracker(clientID, threadID + '-' + i,
           STAT_MOD_DURATION, collectionInterval);
    }

    resultCodes = new CategoricalTracker[numConns];
    for (int i=0; i < numConns; i++)
    {
      resultCodes[i] = new CategoricalTracker(clientID, threadID + '-' + i,
           STAT_RESULT_CODES, collectionInterval);
    }

    modsExceedingThreshold = new IncrementalTracker[numConns];
    for (int i=0; i < numConns; i++)
    {
      modsExceedingThreshold[i] = new IncrementalTracker(clientID,
           threadID + '-' + i, STAT_MODS_EXCEEDING_THRESHOLD,
           collectionInterval);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void finalizeClient()
  {
    if (conns != null)
    {
      for (final LDAPConnection conn : conns)
      {
        conn.close();
      }

      conns = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Create a buffer that will be used to hold the modification value.
    final StringBuilder value = new StringBuilder(valueLength);


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
      if (outstandingRequests != null)
      {
        try
        {
          outstandingRequests.acquire();
        }
        catch (final Exception e)
        {
          continue;
        }
      }

      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      // See if it's time to change the tracking state.
      final long modStartTime = System.currentTimeMillis();
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


      // Figure out which connection should be used for the modification.
      int connSlot;
      if (useRoundRobin)
      {
        connSlot = nextConnSlot++;
        if (nextConnSlot >= conns.length)
        {
          nextConnSlot = 0;
        }
      }
      else
      {
        connSlot = 0;
        int minActiveCount = Integer.MAX_VALUE;
        for (int i=0; i < conns.length; i++)
        {
          final int activeCount = conns[i].getActiveOperationCount();
          if (activeCount == 0)
          {
            connSlot = i;
            break;
          }
          else if (activeCount < minActiveCount)
          {
            connSlot = i;
            minActiveCount = activeCount;
          }
        }
      }


      // Select the target entry DN.
      final String entryDN;
      if (random.nextInt(100) < dn1Percentage)
      {
        entryDN = dn1Pattern.nextValue();
      }
      else
      {
        entryDN = dn2Pattern.nextValue();
      }


      // Generate and process the modify request.
      final LDAPAsynchronousModRateListener listener;
      if (collectingStats)
      {
        listener = new LDAPAsynchronousModRateListener(responseTimeThreshold,
             outstandingRequests, resultCodes[connSlot],
             modsCompleted[connSlot], modsExceedingThreshold[connSlot],
             modTimers[connSlot]);
      }
      else
      {
        listener = new LDAPAsynchronousModRateListener(responseTimeThreshold,
             outstandingRequests, null, null, null, null);
      }

      value.setLength(0);
      for (int i=0; i < valueLength; i++)
      {
        value.append(characterSet[random.nextInt(characterSet.length)]);
      }
      final String valueStr = value.toString();

      final Modification[] mods = new Modification[modAttributes.length];
      for (int i=0; i < modAttributes.length; i++)
      {
        mods[i] = new Modification(ModificationType.REPLACE, modAttributes[i],
                                   valueStr);
      }

      final ModifyRequest modifyRequest = new ModifyRequest(entryDN, mods);

      try
      {
        conns[connSlot].asyncModify(modifyRequest, listener);
      }
      catch (LDAPException le)
      {
        if (collectingStats)
        {
          resultCodes[connSlot].increment(le.getResultCode().toString());
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
    for (final IncrementalTracker t : modsCompleted)
    {
      t.startTracker();
    }

    for (final TimeTracker t : modTimers)
    {
      t.startTracker();
    }

    for (final CategoricalTracker t : resultCodes)
    {
      t.startTracker();
    }

    for (final IncrementalTracker t : modsExceedingThreshold)
    {
      t.startTracker();
    }
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    for (final IncrementalTracker t : modsCompleted)
    {
      t.stopTracker();
    }

    for (final TimeTracker t : modTimers)
    {
      t.stopTracker();
    }

    for (final CategoricalTracker t : resultCodes)
    {
      t.stopTracker();
    }

    for (final IncrementalTracker t : modsExceedingThreshold)
    {
      t.stopTracker();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void destroyThread()
  {
    finalizeClient();
  }
}
