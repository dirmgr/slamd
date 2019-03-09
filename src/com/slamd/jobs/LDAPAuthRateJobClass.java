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
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.CRAMMD5BindRequest;
import com.unboundid.ldap.sdk.DIGESTMD5BindRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a SLAMD job class that may be used to perform repeated
 * searches to find a user entry followed by an attempt to bind as that user.
 */
public class LDAPAuthRateJobClass
       extends LDAPJobClass
{
  /**
   * The display name for the stat tracker used to track authentication
   * durations.
   */
  private static final String STAT_AUTH_DURATION =
       "Authentication Duration (ms)";



  /**
   * The display name for the stat tracker used to track authentications
   * completed.
   */
  private static final String STAT_AUTHS_COMPLETED =
       "Authentications Completed";



  /**
   * The display name for the stat tracker used to track authentications
   * exceeding the response time threshold.
   */
  private static final String STAT_AUTHS_EXCEEDING_THRESHOLD =
       "Authentications Exceeding Response Time Threshold";



  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_RESULT_CODES = "Result Codes";



  /**
   * The authentication type string that will be used for simple authentication.
   */
  private static final String AUTH_TYPE_SIMPLE = "Simple";



  /**
   * The authentication type string that will be used for CRAM-MD5
   * authentication.
   */
  private static final String AUTH_TYPE_CRAM_MD5 = "CRAM-MD5";



  /**
   * The authentication type string that will be used for DIGEST-MD5
   * authentication.
   */
  private static final String AUTH_TYPE_DIGEST_MD5 = "DIGEST-MD5";



  /**
   * The authentication type string that will be used for PLAIN authentication.
   */
  private static final String AUTH_TYPE_PLAIN = "PLAIN";



  /**
   * The set of defined authentication types.
   */
  private static final String[] AUTH_TYPES =
  {
    AUTH_TYPE_SIMPLE,
    AUTH_TYPE_CRAM_MD5,
    AUTH_TYPE_DIGEST_MD5,
    AUTH_TYPE_PLAIN
  };



  /**
   * The default set of attributes to include in matching entries.
   */
  private static final String[] DEFAULT_ATTRS = { "1.1" };



  // Variables used to hold the values of the parameters.
  private static int      authType;
  private static int      coolDownTime;
  private static int      filter1Percentage;
  private static int      responseTimeThreshold;
  private static int      warmUpTime;
  private static long     timeBetweenAuths;
  private static String   baseDN;
  private static String   filter1;
  private static String   filter2;
  private static String   userPassword;
  private static String[] attributes;

  // Stat trackers used by this job.
  private CategoricalTracker  resultCodes;
  private IncrementalTracker  authsCompleted;
  private IncrementalTracker  authsExceedingThreshold;
  private TimeTracker         authTimer;

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

  // The connections used by this thread.
  private LDAPConnection bindConn;
  private LDAPConnection searchConn;

  // The parameters used by this job.
  private IntegerParameter coolDownParameter = new IntegerParameter(
       "coolDownTime", "Cool Down Time",
       "The length of time in seconds to continue running after ending " +
            "statistics collection.",
       true, 0, true, 0, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Authentication Rate (Auths/Second/Client)",
       "Specifies the maximum authentication rate (in authentications per " +
            "second per client) to attempt to maintain.  If multiple clients " +
            "are used, then each client will attempt to maintain this rate.  " +
            "A value less than or equal to zero indicates that the client " +
            "should attempt to perform authentications as quickly as possible.",
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
  private IntegerParameter thresholdParameter = new IntegerParameter(
       "threshold", "Response Time Threshold (ms)",
       "Specifies a threshold in milliseconds for which to count the number " +
            "of authentications that take longer than this time to " +
            "complete.  A value less than or equal to zero indicates that " +
            "there will not be any threshold.",
       false, -1);
  private IntegerParameter timeBetweenAuthsParameter = new IntegerParameter(
       "timeBetweenAuths", "Time Between Authentications (ms)",
       "The minimum length of time in milliseconds that should pass between " +
            "the beginning of one authentication attempt and the beginning " +
            "of the next.  Note that an authentication attempt is comprised " +
            "of both the search and bind operations.",
       false, 0, true, 0, false, 0);
  private IntegerParameter warmUpParameter = new IntegerParameter(
       "warmUpTime", "Warm Up Time",
       "The length of time in seconds to run before beginning to collect " +
            "statistics.",
       true, 0, true, 0, false, 0);
  private MultiChoiceParameter authTypeParameter =
       new MultiChoiceParameter("authType", "Authentication Type",
            "The type of authentication to perform.", AUTH_TYPES,
            AUTH_TYPE_SIMPLE);
  private MultiLineTextParameter attributesParameter =
       new MultiLineTextParameter("attributes", "Attributes to Return",
            "The set of attributes to include in matching entries.  If no " +
                 "attribute names are provided, then all attributes will be " +
                 "returned.  If multiple names are provided, then they " +
                 "should be provided on separate lines.",
            DEFAULT_ATTRS, false);
  private PasswordParameter userPWParameter = new PasswordParameter("userPW",
       "User Password",
       "The password that should be used when attempting to bind as the " +
            "target users.",
       true, null);
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
  public LDAPAuthRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Auth Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP search and bind operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated authentications against " +
      "an LDAP directory server.  Each authentication consists of a " +
      "search to find a user entry and then a bind as that user.  The " +
      "search to find the user must match exactly one entry."
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
         filter1Parameter,
         filter2Parameter,
         percentageParameter,
         attributesParameter,
         new PlaceholderParameter(),
         authTypeParameter,
         userPWParameter,
         new PlaceholderParameter(),
         warmUpParameter,
         coolDownParameter,
         thresholdParameter,
         maxRateParameter,
         rateLimitDurationParameter,
         timeBetweenAuthsParameter);
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
      new IncrementalTracker(clientID, threadID, STAT_AUTHS_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_AUTH_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_AUTHS_EXCEEDING_THRESHOLD,
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
        authsCompleted,
        authTimer,
        resultCodes,
        authsExceedingThreshold
      };
    }
    else
    {
      return new StatTracker[]
      {
        authsCompleted,
        authTimer,
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


    attributes = new String[0];
    attributesParameter =
         parameters.getMultiLineTextParameter(attributesParameter.getName());
    if ((attributesParameter != null) && attributesParameter.hasValue())
    {
      attributes = attributesParameter.getNonBlankLines();
    }


    authType = 0;
    authTypeParameter =
         parameters.getMultiChoiceParameter(authTypeParameter.getName());
    if ((authTypeParameter != null) && authTypeParameter.hasValue())
    {
      String authTypeStr = authTypeParameter.getStringValue();
      for (int i=0; i < AUTH_TYPES.length; i++)
      {
        if (authTypeStr.equalsIgnoreCase(AUTH_TYPES[i]))
        {
          authType = i;
          break;
        }
      }
    }


    userPWParameter =
         parameters.getPasswordParameter(userPWParameter.getName());
    if ((userPWParameter != null) && userPWParameter.hasValue())
    {
      userPassword = userPWParameter.getStringValue();
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


    timeBetweenAuths = 0L;
    timeBetweenAuthsParameter =
         parameters.getIntegerParameter(timeBetweenAuthsParameter.getName());
    if ((timeBetweenAuthsParameter != null) &&
        timeBetweenAuthsParameter.hasValue())
    {
      timeBetweenAuths = timeBetweenAuthsParameter.getIntValue();
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
    authsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_AUTHS_COMPLETED, collectionInterval);
    authTimer = new TimeTracker(clientID, threadID, STAT_AUTH_DURATION,
         collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);
    authsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_AUTHS_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      authsCompleted.enableRealTimeStats(statReporter, jobID);
      authTimer.enableRealTimeStats(statReporter, jobID);
      authsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
    }

    random = new Random(parentRandom.nextLong());

    searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
         Filter.createPresenceFilter("objectClass"), attributes);
    searchRequest.setSizeLimit(1);

    try
    {
      searchConn = createConnection();

      bindConn = createConnection(searchConn.getConnectedAddress(),
                                  searchConn.getConnectedPort());
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
    if (searchConn != null)
    {
      searchConn.close();
      searchConn = null;
    }

    if (bindConn != null)
    {
      bindConn.close();
      bindConn = null;
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
      long authStartTime = System.currentTimeMillis();
      if (collectingStats && (coolDownTime > 0) &&
          (authStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (authStartTime >= startCollectingTime))
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


      // Process the authentication.
      if (collectingStats)
      {
        authTimer.startTimer();
      }

      try
      {
        // First, search to find the target user.
        SearchResult searchResult = searchConn.search(searchRequest);
        if (! searchResult.getResultCode().equals(ResultCode.SUCCESS))
        {
          if (collectingStats)
          {
            resultCodes.increment(searchResult.getResultCode().toString());
          }
          continue;
        }

        String userDN;
        switch (searchResult.getEntryCount())
        {
          case 0:
            if (collectingStats)
            {
              resultCodes.increment(ResultCode.NO_RESULTS_RETURNED.toString());
            }
            continue;

          case 1:
            userDN = searchResult.getSearchEntries().get(0).getDN();
            break;

          default:
            if (collectingStats)
            {
              resultCodes.increment(
                   ResultCode.MORE_RESULTS_TO_RETURN.toString());
            }
            continue;
        }


        // Now attempt to bind as the user.
        BindRequest bindRequest = null;
        switch (authType)
        {
          case 0: // Simple
            bindRequest = new SimpleBindRequest(userDN, userPassword);
            break;
          case 1: // CRAM-MD5
            bindRequest = new CRAMMD5BindRequest("dn:" + userDN, userPassword);
            break;
          case 2: // DIGEST-MD5
            bindRequest =
                 new DIGESTMD5BindRequest("dn:" + userDN, userPassword);
            break;
          case 3: // PLAIN
            bindRequest = new PLAINBindRequest("dn:" + userDN, userPassword);
            break;
        }

        BindResult bindResult = bindConn.bind(bindRequest);
        if (collectingStats)
        {
          resultCodes.increment(bindResult.getResultCode().toString());
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
          authTimer.stopTimer();
          authsCompleted.increment();

          if ((responseTimeThreshold > 0) &&
              (authTimer.getLastOperationTime() > responseTimeThreshold))
          {
            authsExceedingThreshold.increment();
          }
        }
      }


      // Sleep if necessary before the next request.
      if (timeBetweenAuths > 0L)
      {
        long elapsedTime = System.currentTimeMillis() - authStartTime;
        long sleepTime   = timeBetweenAuths - elapsedTime;
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
    authsCompleted.startTracker();
    authTimer.startTracker();
    resultCodes.startTracker();
    authsExceedingThreshold.startTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    authsCompleted.stopTracker();
    authTimer.stopTracker();
    resultCodes.stopTracker();
    authsExceedingThreshold.stopTracker();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void destroyThread()
  {
    if (searchConn != null)
    {
      searchConn.close();
      searchConn = null;
    }

    if (bindConn != null)
    {
      bindConn.close();
      bindConn = null;
    }
  }
}
