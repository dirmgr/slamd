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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a SLAMD job class that may be used to perform search and
 * modify operations against an LDAP directory server.
 */
public class LDAPSearchAndModRateJobClass
       extends LDAPJobClass
{
  /**
   * The display name for the stat tracker used to track entries returned.
   */
  private static final String STAT_ENTRIES_RETURNED =
       "Entries Returned per Search";



  /**
   * The display name for the stat tracker used to track result codes for search
   * operations.
   */
  private static final String STAT_SEARCH_RESULT_CODES = "Search Result Codes";



  /**
   * The display name for the stat tracker used to track result codes for modify
   * operations.
   */
  private static final String STAT_MOD_RESULT_CODES = "Modify Result Codes";



  /**
   * The display name for the stat tracker used to track search durations.
   */
  private static final String STAT_SEARCH_DURATION = "Search Duration (ms)";



  /**
   * The display name for the stat tracker used to track searches completed.
   */
  private static final String STAT_SEARCHES_COMPLETED = "Searches Completed";



  /**
   * The display name for the stat tracker used to track searches exceeding the
   * response time threshold.
   */
  private static final String STAT_SEARCHES_EXCEEDING_THRESHOLD =
       "Searches Exceeding Response Time Threshold";



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



  /**
   * The search scope that indicates that only the base entry should be
   * targeted.
   */
  private static final String SCOPE_STR_BASE = "Search Base Only";



  /**
   * The search scope that indicates that only immediate children of the search
   * base should be targeted.
   */
  private static final String SCOPE_STR_ONE = "One Level Below Base";



  /**
   * The search scope that indicates that the base DN and all its descendants
   * may be targeted.
   */
  private static final String SCOPE_STR_SUB = "Whole Subtree";



  /**
   * The search scope that indicates that all entries below the search base (but
   * not the base entry itself) may be targeted.
   */
  private static final String SCOPE_STR_SUBORD = "Subordinate Subtree";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 4870520300740292912L;



  /**
   * The set of defined search scopes.
   */
  private static final String[] SCOPES =
  {
    SCOPE_STR_BASE,
    SCOPE_STR_ONE,
    SCOPE_STR_SUB,
    SCOPE_STR_SUBORD
  };



  /**
   * The set of characters to include in the values to use for the target
   * attributes.
   */
  private static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  // Variables used to hold the values of the parameters.
  private static int          coolDownTime;
  private static int          filter1Percentage;
  private static int          modTimeThreshold;
  private static int          searchTimeThreshold;
  private static int          sizeLimit;
  private static int          timeLimit;
  private static int          valueLength;
  private static int          warmUpTime;
  private static long         timeBetweenRequests;
  private static SearchScope  scope;
  private static String       baseDN;
  private static String       filter1;
  private static String       filter2;
  private static String[]     modAttributes;
  private static String[]     searchAttributes;

  // Stat trackers used by this job.
  private CategoricalTracker  modResultCodes;
  private CategoricalTracker  searchResultCodes;
  private IncrementalTracker  modsCompleted;
  private IncrementalTracker  modsExceedingThreshold;
  private IncrementalTracker  searchesCompleted;
  private IncrementalTracker  searchesExceedingThreshold;
  private IntegerValueTracker entriesReturned;
  private TimeTracker         modTimer;
  private TimeTracker         searchTimer;

  // Random number generators used by this job.
  private static Random parentRandom;
  private Random random;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // Value patterns used for the filters.
  private static ValuePattern filter1Pattern;
  private static ValuePattern filter2Pattern;

  // The search request to use for this thread.
  private SearchRequest searchRequest;

  // The LDAP connection used by this thread.
  private LDAPConnection conn;

  // The parameters used by this job.
  private IntegerParameter coolDownParameter = new IntegerParameter(
       "coolDownTime", "Cool Down Time",
       "The length of time in seconds to continue running after ending " +
            "statistics collection.",
       true, 0, true, 0, false, 0);
  private IntegerParameter lengthParameter = new IntegerParameter("length",
       "Modification Value Length",
       "The number of characters to include in modification values.", true, 80,
       true, 1, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Operation Rate (Ops/Second/Client)",
       "Specifies the maximum operation rate (in combined searches and " +
            "modifies per second per client) to attempt to maintain.  If " +
            "multiple clients are used, then each client will attempt to " +
            "maintain this rate.  A value less than or equal to zero " +
            "indicates that the client should attempt to perform operations " +
            "as quickly as possible.",
       true, -1);
  private IntegerParameter percentageParameter = new IntegerParameter(
       "percentage", "Filter 1 Percentage",
       "The percentage of the searches which should use the first filter " +
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
  private IntegerParameter sizeLimitParameter = new IntegerParameter(
       "sizeLimit", "Size Limit",
       "The maximum number of entries that should be returned from each " +
            "search (zero indicates no limit).",
       false, 0, true, 0, false, 0);
  private IntegerParameter modThresholdParameter = new IntegerParameter(
       "threshold", "Modify Response Time Threshold (ms)",
       "Specifies a threshold in milliseconds for which to count the number " +
            "of modifications that take longer than this time to complete.  " +
            "A value less than or equal to zero indicates that there will " +
            "not be any threshold.",
       false, -1);
  private IntegerParameter searchThresholdParameter = new IntegerParameter(
       "threshold", "Search Response Time Threshold (ms)",
       "Specifies a threshold in milliseconds for which to count the number " +
            "of searches that take longer than this time to complete.  A " +
            "value less than or equal to zero indicates that there will not " +
            "be any threshold.",
       false, -1);
  private IntegerParameter timeBetweenRequestsParameter = new IntegerParameter(
       "timeBetweenRequests", "Time Between Requests (ms)",
       "The minimum length of time in milliseconds that should pass between " +
            "the beginning of one request and the beginning of the next.",
       false, 0, true, 0, false, 0);
  private IntegerParameter timeLimitParameter = new IntegerParameter(
       "timeLimit", "Time Limit",
       "The maximum length of time in seconds that the server should spend " +
            "processing each search (zero indicates no limit).",
       false, 0, true, 0, false, 0);
  private IntegerParameter warmUpParameter = new IntegerParameter(
       "warmUpTime", "Warm Up Time",
       "The length of time in seconds to run before beginning to collect " +
            "statistics.",
       true, 0, true, 0, false, 0);
  private MultiChoiceParameter scopeParameter = new MultiChoiceParameter(
       "scope", "Search Scope",
       "The scope of entries to target with the searches.", SCOPES,
       SCOPE_STR_BASE);
  private MultiLineTextParameter modAttributesParameter =
       new MultiLineTextParameter( "modAttributes", "Attribute(s) to Modify",
            "The set of attributes to modify in matching entries.  If " +
                 "multiple attribute names are provided, then they must be " +
                 "provided on separate lines and each of those attributes " +
                 "will be replaced with the same value generated for that " +
                 "entry.",
            new String[] { "description" }, true);
  private MultiLineTextParameter searchAttributesParameter =
       new MultiLineTextParameter( "searchAttributes", "Attributes to Return",
            "The set of attributes to include in matching entries.  If no " +
                 "attribute names are provided, then all attributes will be " +
                 "returned.  If multiple names are provided, then they " +
                 "should be provided on separate lines.",
            null, false);
  private StringParameter baseDNParameter = new StringParameter("baseDN",
       "Search Base", "The base entry to use for the searches.", false, "");
  private StringParameter filter1Parameter = new StringParameter("filter1",
       "Search Filter 1",
       "The search filter to use for searches that fall into the first " +
            "category.",
       true, null);
  private StringParameter filter2Parameter = new StringParameter("filter2",
       "Search Filter 2",
       "The search filter to use for searches that fall into the second " +
            "category.",
       true, null);



  /**
   * Creates a new instance of this job class.
   */
  public LDAPSearchAndModRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Search and Modify Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP search and modify operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated searches against an LDAP " +
      "directory server.  Each entry matching the search criteria will be " +
      "modified to replace the values for the specified attribute or set of " +
      "attributes."
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
         baseDNParameter,
         scopeParameter,
         searchAttributesParameter,
         new PlaceholderParameter(),
         filter1Parameter,
         filter2Parameter,
         percentageParameter,
         new PlaceholderParameter(),
         modAttributesParameter,
         lengthParameter,
         new PlaceholderParameter(),
         warmUpParameter,
         coolDownParameter,
         searchThresholdParameter,
         modThresholdParameter,
         maxRateParameter,
         rateLimitDurationParameter,
         sizeLimitParameter,
         timeLimitParameter,
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
      new IncrementalTracker(clientID, threadID, STAT_SEARCHES_COMPLETED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_MODS_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_MOD_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_SEARCH_RESULT_CODES,
                             collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_MOD_RESULT_CODES,
                             collectionInterval),
      new IntegerValueTracker(clientID, threadID, STAT_ENTRIES_RETURNED,
                              collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_SEARCHES_EXCEEDING_THRESHOLD,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_MODS_EXCEEDING_THRESHOLD,
                             collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>(9);
    trackerList.add(searchesCompleted);
    trackerList.add(modsCompleted);
    trackerList.add(searchTimer);
    trackerList.add(modTimer);
    trackerList.add(searchResultCodes);
    trackerList.add(modResultCodes);
    trackerList.add(entriesReturned);

    if (searchTimeThreshold > 0)
    {
      trackerList.add(searchesExceedingThreshold);
    }

    if (modTimeThreshold > 0)
    {
      trackerList.add(modsExceedingThreshold);
    }

    StatTracker[] trackers = new StatTracker[trackerList.size()];
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
    // The filter parameters must be parseable as value patterns.
    StringParameter p =
         parameters.getStringParameter(filter1Parameter.getName());
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

    p = parameters.getStringParameter(filter2Parameter.getName());
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
  protected boolean testNonLDAPJobParameters(final ParameterList parameters,
                         final LDAPConnection connection,
                         final ArrayList<String> outputMessages)
  {
    boolean successful = true;

    // Ensure that the base DN exists.
    StringParameter baseDNParam =
         parameters.getStringParameter(baseDNParameter.getName());
    if ((baseDNParam != null) && baseDNParam.hasValue())
    {
      try
      {
        String base = baseDNParam.getStringValue();
        outputMessages.add("Ensuring that base entry '" + base +
                           "' exists....");
        SearchResultEntry e = connection.getEntry(base);
        if (e == null)
        {
          outputMessages.add("ERROR:  The base entry does not exist.");
          successful = false;
        }
        else
        {
          outputMessages.add("The base entry exists.");
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
    parentRandom = new Random();


    baseDN = "";
    baseDNParameter = parameters.getStringParameter(baseDNParameter.getName());
    if ((baseDNParameter != null) && baseDNParameter.hasValue())
    {
      baseDN = baseDNParameter.getStringValue();
    }


    scope = SearchScope.BASE;
    scopeParameter =
         parameters.getMultiChoiceParameter(scopeParameter.getName());
    if ((scopeParameter != null) && scopeParameter.hasValue())
    {
      String scopeStr = scopeParameter.getStringValue();
      if (scopeStr.equalsIgnoreCase(SCOPE_STR_BASE))
      {
        scope = SearchScope.BASE;
      }
      else if (scopeStr.equalsIgnoreCase(SCOPE_STR_ONE))
      {
        scope  = SearchScope.ONE;
      }
      else if (scopeStr.equalsIgnoreCase(SCOPE_STR_SUB))
      {
        scope = SearchScope.SUB;
      }
      else if (scopeStr.equalsIgnoreCase(SCOPE_STR_SUBORD))
      {
        scope = SearchScope.SUBORDINATE_SUBTREE;
      }
    }


    searchAttributes = new String[0];
    searchAttributesParameter = parameters.getMultiLineTextParameter(
         searchAttributesParameter.getName());
    if ((searchAttributesParameter != null) &&
        searchAttributesParameter.hasValue())
    {
      searchAttributes = searchAttributesParameter.getNonBlankLines();
    }


    filter1Parameter =
         parameters.getStringParameter(filter1Parameter.getName());
    filter1 = filter1Parameter.getStringValue();


    filter2Parameter =
         parameters.getStringParameter(filter2Parameter.getName());
    filter2 = filter2Parameter.getStringValue();


    filter1Percentage = 50;
    percentageParameter =
         parameters.getIntegerParameter(percentageParameter.getName());
    if ((percentageParameter != null) && percentageParameter.hasValue())
    {
      filter1Percentage = percentageParameter.getIntValue();
    }


    modAttributesParameter = parameters.getMultiLineTextParameter(
         modAttributesParameter.getName());
    modAttributes = modAttributesParameter.getNonBlankLines();


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


    searchTimeThreshold = -1;
    searchThresholdParameter =
         parameters.getIntegerParameter(searchThresholdParameter.getName());
    if ((searchThresholdParameter != null) &&
        searchThresholdParameter.hasValue())
    {
      searchTimeThreshold = searchThresholdParameter.getIntValue();
    }


    modTimeThreshold = -1;
    modThresholdParameter =
         parameters.getIntegerParameter(modThresholdParameter.getName());
    if ((modThresholdParameter != null) && modThresholdParameter.hasValue())
    {
      modTimeThreshold = modThresholdParameter.getIntValue();
    }


    sizeLimit = 0;
    sizeLimitParameter =
         parameters.getIntegerParameter(sizeLimitParameter.getName());
    if ((sizeLimitParameter != null) && sizeLimitParameter.hasValue())
    {
      sizeLimit = sizeLimitParameter.getIntValue();
    }


    timeLimit = 0;
    timeLimitParameter =
         parameters.getIntegerParameter(timeLimitParameter.getName());
    if ((timeLimitParameter != null) && timeLimitParameter.hasValue())
    {
      timeLimit = timeLimitParameter.getIntValue();
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
      filter1Pattern = new ValuePattern(filter1);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to parse filter pattern 1:  " +
                                     stackTraceToString(e), e);
    }

    try
    {
      filter2Pattern = new ValuePattern(filter2);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to parse filter pattern 2:  " +
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
    searchesCompleted = new IncrementalTracker(clientID, threadID,
         STAT_SEARCHES_COMPLETED, collectionInterval);
    modsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_MODS_COMPLETED, collectionInterval);
    searchTimer = new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
         collectionInterval);
    modTimer = new TimeTracker(clientID, threadID, STAT_MOD_DURATION,
         collectionInterval);
    entriesReturned = new IntegerValueTracker(clientID, threadID,
         STAT_ENTRIES_RETURNED, collectionInterval);
    searchResultCodes = new CategoricalTracker(clientID, threadID,
         STAT_SEARCH_RESULT_CODES, collectionInterval);
    modResultCodes = new CategoricalTracker(clientID, threadID,
         STAT_MOD_RESULT_CODES, collectionInterval);
    searchesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_SEARCHES_EXCEEDING_THRESHOLD, collectionInterval);
    modsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_MODS_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      searchesCompleted.enableRealTimeStats(statReporter, jobID);
      modsCompleted.enableRealTimeStats(statReporter, jobID);
      searchTimer.enableRealTimeStats(statReporter, jobID);
      modTimer.enableRealTimeStats(statReporter, jobID);
      entriesReturned.enableRealTimeStats(statReporter, jobID);
      searchesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      modsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
    }

    random = new Random(parentRandom.nextLong());

    searchRequest = new SearchRequest(baseDN, scope,
         Filter.createPresenceFilter("objectClass"), searchAttributes);
    searchRequest.setSizeLimit(sizeLimit);
    searchRequest.setTimeLimitSeconds(timeLimit);
    if (timeLimit > 0)
    {
      searchRequest.setResponseTimeoutMillis(1000L * timeLimit);
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

    StringBuilder value = new StringBuilder(valueLength);

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


    // Perform the searches until it's time to stop.
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
      long searchStartTime = System.currentTimeMillis();
      if (collectingStats && (coolDownTime > 0) &&
          (searchStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (searchStartTime >= startCollectingTime))
      {
        collectingStats = true;
        startTrackers();
      }


      // Update the search request with an appropriate filter.
      try
      {
        if (random.nextInt(100) < filter1Percentage)
        {
          searchRequest.setFilter(filter1Pattern.nextValue());
        }
        else
        {
          searchRequest.setFilter(filter2Pattern.nextValue());
        }
      }
      catch (Exception e)
      {
        logMessage("ERROR -- Generated an invalid search filter:  " +
                   stackTraceToString(e));
        indicateStoppedDueToError();
        break;
      }


      // Process the search.
      if (collectingStats)
      {
        searchTimer.startTimer();
      }

      List<SearchResultEntry> entries = null;
      try
      {
        SearchResult searchResult = conn.search(searchRequest);
        entries = searchResult.getSearchEntries();
        if (collectingStats)
        {
          entriesReturned.addValue(searchResult.getEntryCount());
          searchResultCodes.increment(searchResult.getResultCode().toString());
        }
      }
      catch (LDAPSearchException lse)
      {
        entries = lse.getSearchEntries();
      }
      catch (LDAPException le)
      {
        if (collectingStats)
        {
          searchResultCodes.increment(le.getResultCode().toString());
        }
      }
      finally
      {
        if (collectingStats)
        {
          searchTimer.stopTimer();
          searchesCompleted.increment();

          if ((searchTimeThreshold > 0) &&
              (searchTimer.getLastOperationTime() > searchTimeThreshold))
          {
            searchesExceedingThreshold.increment();
          }
        }
      }


      // For each entry returned, perform a modification to replace the values
      // of the specified set of attributes.
      if ((entries != null) && (! entries.isEmpty()))
      {
        value.setLength(0);
        for (int i=0; i < valueLength; i++)
        {
          value.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        String valueStr = value.toString();

        Modification[] mods = new Modification[modAttributes.length];
        for (int i=0; i < modAttributes.length; i++)
        {
          mods[i] = new Modification(ModificationType.REPLACE, modAttributes[i],
                                     valueStr);
        }

        ModifyRequest modifyRequest = new ModifyRequest("", mods);
        for (SearchResultEntry e : entries)
        {
          if (rateLimiter != null)
          {
            if (rateLimiter.await())
            {
              continue;
            }
          }

          modifyRequest.setDN(e.getDN());
          if (collectingStats)
          {
            modTimer.startTimer();
          }

          try
          {
            LDAPResult modResult = conn.modify(modifyRequest);
            if (collectingStats)
            {
              modResultCodes.increment(modResult.getResultCode().toString());
            }
          }
          catch (LDAPException le)
          {
            if (collectingStats)
            {
              modResultCodes.increment(le.getResultCode().toString());
            }
          }
          finally
          {
            if (collectingStats)
            {
              modTimer.stopTimer();
              modsCompleted.increment();

              if ((modTimeThreshold > 0) &&
                  (modTimer.getLastOperationTime() > modTimeThreshold))
              {
                modsExceedingThreshold.increment();
              }
            }
          }
        }
      }


      // Sleep if necessary before the next request.
      if (timeBetweenRequests > 0L)
      {
        long elapsedTime = System.currentTimeMillis() - searchStartTime;
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
    searchesCompleted.startTracker();
    modsCompleted.startTracker();
    searchTimer.startTracker();
    modTimer.startTracker();
    entriesReturned.startTracker();
    searchResultCodes.startTracker();
    modResultCodes.startTracker();
    searchesExceedingThreshold.startTracker();
    modsExceedingThreshold.startTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    searchesCompleted.stopTracker();
    modsCompleted.stopTracker();
    searchTimer.stopTracker();
    modTimer.stopTracker();
    entriesReturned.stopTracker();
    searchResultCodes.stopTracker();
    modResultCodes.stopTracker();
    searchesExceedingThreshold.stopTracker();
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
