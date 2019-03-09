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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.IntegerParameter;
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
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.FixedRateBarrier;



/**
 * This class provides a SLAMD job class that may be used to perform searches
 * against an LDAP directory server.  The filters to use for the search requests
 * will be read from a file.
 */
public class LDAPFileBasedSearchRateJobClass
       extends LDAPJobClass
       implements SearchResultListener
{
  /**
   * The display name for the stat tracker used to track entries returned.
   */
  private static final String STAT_ENTRIES_RETURNED =
       "Entries Returned per Search";



  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_RESULT_CODES = "Result Codes";



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



  // Variables used to hold the values of the parameters.
  private static AtomicInteger sequentialCounter;
  private static boolean       sequential;
  private static Filter[]      filters;
  private static int           coolDownTime;
  private static int           responseTimeThreshold;
  private static int           sizeLimit;
  private static int           timeLimit;
  private static int           warmUpTime;
  private static long          timeBetweenRequests;
  private static SearchScope   scope;
  private static String        baseDN;
  private static String[]      attributes;

  // Stat trackers used by this job.
  private CategoricalTracker  resultCodes;
  private IncrementalTracker  searchesCompleted;
  private IncrementalTracker  searchesExceedingThreshold;
  private IntegerValueTracker entriesReturned;
  private TimeTracker         searchTimer;

  // Random number generators used by this job.
  private static Random parentRandom;
  private Random random;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // The search request to use for this thread.
  private SearchRequest searchRequest;

  // The LDAP connection used by this thread.
  private LDAPConnection conn;

  // The parameters used by this job.
  private BooleanParameter sequentialParameter = new BooleanParameter(
       "sequential", "Use Filters in Sequential Order",
       "Indicates whether to use the filters in sequential order rather than " +
            "at random.",
       false);
  private FileURLParameter filterFileParameter = new FileURLParameter(
       "filterFile", "Filter File URL",
       "The URL to a file containing the filters to use for search requests.",
       null, true);
  private IntegerParameter coolDownParameter = new IntegerParameter(
       "coolDownTime", "Cool Down Time",
       "The length of time in seconds to continue running after ending " +
            "statistics collection.",
       true, 0, true, 0, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Search Rate (Searches/Second/Client)",
       "Specifies the maximum search rate (in searches per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform searches as quickly as possible.",
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
  private IntegerParameter sizeLimitParameter = new IntegerParameter(
       "sizeLimit", "Size Limit",
       "The maximum number of entries that should be returned from each " +
            "search (zero indicates no limit).",
       false, 0, true, 0, false, 0);
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
       SCOPE_STR_SUB);
  private MultiLineTextParameter attributesParameter =
       new MultiLineTextParameter( "attributes", "Attributes to Return",
            "The set of attributes to include in matching entries.  If no " +
                 "attribute names are provided, then all attributes will be " +
                 "returned.  If multiple names are provided, then they " +
                 "should be provided on separate lines.",
            null, false);
  private StringParameter baseDNParameter = new StringParameter("baseDN",
       "Search Base", "The base entry to use for the searches.", false, "");



  /**
   * Creates a new instance of this job class.
   */
  public LDAPFileBasedSearchRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP File-Based SearchRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP search operations using filters read from " +
           "a file";
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
      "directory server using filters read from a file."
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
         attributesParameter,
         new PlaceholderParameter(),
         filterFileParameter,
         sequentialParameter,
         new PlaceholderParameter(),
         warmUpParameter,
         coolDownParameter,
         thresholdParameter,
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
      new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
                      collectionInterval),
      new IntegerValueTracker(clientID, threadID, STAT_ENTRIES_RETURNED,
                              collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_SEARCHES_EXCEEDING_THRESHOLD,
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
        searchesCompleted,
        searchTimer,
        entriesReturned,
        resultCodes,
        searchesExceedingThreshold
      };
    }
    else
    {
      return new StatTracker[]
      {
        searchesCompleted,
        searchTimer,
        entriesReturned,
        resultCodes
      };
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


    attributes = new String[0];
    attributesParameter =
         parameters.getMultiLineTextParameter(attributesParameter.getName());
    if ((attributesParameter != null) && attributesParameter.hasValue())
    {
      attributes = attributesParameter.getNonBlankLines();
    }


    String[] filterLines;
    filterFileParameter =
         parameters.getFileURLParameter(filterFileParameter.getName());
    try
    {
      filterLines = filterFileParameter.getNonBlankFileLines();
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to retrieve filter file " +
           filterFileParameter.getFileURL() + ":  " + stackTraceToString(e), e);
    }

    ArrayList<Filter> filterList = new ArrayList<Filter>(filterLines.length);
    for (String s : filterLines)
    {
      try
      {
        filterList.add(Filter.create(s));
      }
      catch (LDAPException le)
      {
        writeVerbose("Unable to parse string '" + s +
                     "' as a valid search filter:  " + le.getMessage());
      }
    }

    if (filterList.isEmpty())
    {
      throw new UnableToRunException("There are no valid search filters in " +
           "filter file " + filterFileParameter.getFileURL());
    }

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);


    sequentialParameter =
         parameters.getBooleanParameter(sequentialParameter.getName());
    sequential = sequentialParameter.getBooleanValue();
    sequentialCounter = new AtomicInteger(0);


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
    searchTimer = new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
         collectionInterval);
    entriesReturned = new IntegerValueTracker(clientID, threadID,
         STAT_ENTRIES_RETURNED, collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);
    searchesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_SEARCHES_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      searchesCompleted.enableRealTimeStats(statReporter, jobID);
      searchTimer.enableRealTimeStats(statReporter, jobID);
      entriesReturned.enableRealTimeStats(statReporter, jobID);
      searchesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
    }

    random = new Random(parentRandom.nextLong());

    searchRequest = new SearchRequest(this, baseDN, scope,
         Filter.createPresenceFilter("objectClass"), attributes);
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
      if (sequential)
      {
        int pos = sequentialCounter.incrementAndGet();
        if (pos >= filters.length)
        {
          sequentialCounter.set(1);
          searchRequest.setFilter(filters[0]);
        }
        else
        {
          searchRequest.setFilter(filters[pos]);
        }
      }
      else
      {
        searchRequest.setFilter(filters[random.nextInt(filters.length)]);
      }


      // Process the search.
      if (collectingStats)
      {
        searchTimer.startTimer();
      }

      try
      {
        SearchResult searchResult = conn.search(searchRequest);
        if (collectingStats)
        {
          entriesReturned.addValue(searchResult.getEntryCount());
          resultCodes.increment(searchResult.getResultCode().toString());
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
          searchTimer.stopTimer();
          searchesCompleted.increment();

          if ((responseTimeThreshold > 0) &&
              (searchTimer.getLastOperationTime() > responseTimeThreshold))
          {
            searchesExceedingThreshold.increment();
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
    searchTimer.startTracker();
    entriesReturned.startTracker();
    resultCodes.startTracker();
    searchesExceedingThreshold.startTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    searchesCompleted.stopTracker();
    searchTimer.stopTracker();
    entriesReturned.stopTracker();
    resultCodes.stopTracker();
    searchesExceedingThreshold.stopTracker();
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



  /**
   * Processes the provided search result entry returned from the search.
   *
   * @param  e  The search result entry to process.
   */
  public void searchEntryReturned(final SearchResultEntry e)
  {
    // No implementation required.
  }



  /**
   * Processes the provided search result reference returned from the search.
   *
   * @param  r  The search result reference to process.
   */
  public void searchReferenceReturned(final SearchResultReference r)
  {
    // No implementation required.
  }
}
