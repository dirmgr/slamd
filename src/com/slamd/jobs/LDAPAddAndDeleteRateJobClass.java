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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamd.common.SLAMDException;
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
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.util.FixedRateBarrier;



/**
 * This class provides a SLAMD job class that may be used to perform add and
 * delete operations against an LDAP directory server.
 */
public class LDAPAddAndDeleteRateJobClass
       extends LDAPJobClass
{
  /**
   * The display name for the stat tracker used to track add result codes.
   */
  private static final String STAT_ADD_RESULT_CODES = "Add Result Codes";



  /**
   * The display name for the stat tracker used to track add durations.
   */
  private static final String STAT_ADD_DURATION = "Add Duration (ms)";



  /**
   * The display name for the stat tracker used to track adds completed.
   */
  private static final String STAT_ADDS_COMPLETED = "Adds Completed";



  /**
   * The display name for the stat tracker used to track adds exceeding the
   * response time threshold.
   */
  private static final String STAT_ADDS_EXCEEDING_THRESHOLD =
       "Adds Exceeding Response Time Threshold";



  /**
   * The display name for the stat tracker used to track delete result codes.
   */
  private static final String STAT_DELETE_RESULT_CODES = "Delete Result Codes";



  /**
   * The display name for the stat tracker used to track delete durations.
   */
  private static final String STAT_DELETE_DURATION = "Delete Duration (ms)";



  /**
   * The display name for the stat tracker used to track deletes completed.
   */
  private static final String STAT_DELETES_COMPLETED = "Deletes Completed";



  /**
   * The display name for the stat tracker used to track deletes exceeding the
   * response time threshold.
   */
  private static final String STAT_DELETES_EXCEEDING_THRESHOLD =
       "Deletes Exceeding Response Time Threshold";



  /**
   * The default template lines to use when generating entries.
   */
  private static final String[] DEFAULT_TEMPLATE_LINES =
  {
    "objectClass: top",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "uid: <entryNumber>",
    "givenName: <random:alpha:8>",
    "sn: <random:alpha:10>",
    "cn: {givenName} {sn}",
    "initials: {givenName:1}<random:alpha:1>{sn:1}",
    "employeeNumber: {uid}",
    "mail: {uid}@example.com",
    "userPassword: password",
    "telephoneNumber: <random:telephone>",
    "homePhone: <random:telephone>",
    "pager: <random:telephone>",
    "mobile: <random:telephone>",
    "street: <random:numeric:5> <random:alpha:10> Street",
    "l: <random:alpha:10>",
    "st: <random:alpha:2>",
    "postalCode: <random:numeric:5>",
    "postalAddress: {cn}${street}${l}, {st}  {postalCode}",
    "description:  This is the description for {cn}",
  };



  /**
   * The processing type option indicating that all adds should be performed,
   * followed by all deletes.
   */
  private static final String PROCESSING_TYPE_ADDS_THEN_DELETES =
       "Perform all adds, then perform all deletes";



  /**
   * The processing type option indicating that adds and deletes should be
   * alternated.
   */
  private static final String PROCESSING_TYPE_ALTERNATE_ADDS_AND_DELETES =
       "Delete each entry immediately after adding it";



  /**
   * The processing type option indicating that only adds should be performed.
   */
  private static final String PROCESSING_TYPE_ONLY_ADDS =
       "Perform add operations but not delete operations";



  /**
   * The processing type option indicating that only deletes should be
   * performed.
   */
  private static final String PROCESSING_TYPE_ONLY_DELETES =
       "Perform delete operations but not add operations";



  /**
   * The set of available processing types.
   */
  private static final String[] PROCESSING_TYPES =
  {
    PROCESSING_TYPE_ADDS_THEN_DELETES,
    PROCESSING_TYPE_ALTERNATE_ADDS_AND_DELETES,
    PROCESSING_TYPE_ONLY_ADDS,
    PROCESSING_TYPE_ONLY_DELETES
  };



  // Variables used to hold the values of the parameters.
  private static boolean alternateAddsAndDeletes;
  private static boolean performAdds;
  private static boolean performDeletes;
  private static int     firstEntryNumber;
  private static int     lastEntryNumber;
  private static int     responseTimeThreshold;
  private static long    timeBetweenAddsAndDeletes;
  private static long    timeBetweenRequests;
  private static String  baseDN;
  private static String  rdnAttribute;

  // Stat trackers used by this job.
  private CategoricalTracker addResultCodes;
  private CategoricalTracker deleteResultCodes;
  private IncrementalTracker addsCompleted;
  private IncrementalTracker addsExceedingThreshold;
  private IncrementalTracker deletesCompleted;
  private IncrementalTracker deletesExceedingThreshold;
  private TimeTracker        addTimer;
  private TimeTracker        deleteTimer;

  // Random number generators used by this job.
  private static Random parentRandom;
  private Random random;

  // The number of threads that are currently performing adds.
  private static AtomicInteger activeAddThreads;

  // The entry number counter for this job.
  private static AtomicInteger entryNumber;

  // The entry generator for this job.
  private static TemplateBasedEntryGenerator entryGenerator;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // The LDAP connection used by this thread.
  private LDAPConnection conn;

  // The parameters used by this job.
  private IntegerParameter firstNumberParameter = new IntegerParameter("first",
       "First Entry Number",
       "The value that should be used for the entry number for the first " +
            "entry generated.",
       true, 0, true, 0, false, 0);
  private IntegerParameter lastNumberParameter = new IntegerParameter("last",
       "Last Entry Number",
       "The value that should be used for the entry number for the last " +
            "entry generated.  It must be greater than the first entry number",
       true, 1, true, 1, false, 0);
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
  private IntegerParameter timeBetweenAddsAndDelsParameter =
       new IntegerParameter("timeBetweenAddsAndDels",
                "Time Between Adds and Deletes (s)",
                "The minimum length of time in seconds to wait after " +
                     "completing all add operations before beginning the " +
                     "deletes.",
                true, 30, true, 0, false, 0);
  private IntegerParameter timeBetweenRequestsParameter = new IntegerParameter(
       "timeBetweenRequests", "Time Between Requests (ms)",
       "The minimum length of time in milliseconds that should pass between " +
            "the beginning of one request and the beginning of the next.",
       false, 0, true, 0, false, 0);
  private MultiChoiceParameter processingTypeParameter =
       new MultiChoiceParameter("processingType",
                "Type of Processing to Perform",
                "The types of operations to perform, and the order in which " +
                     "they should be processed.",
                PROCESSING_TYPES, PROCESSING_TYPE_ADDS_THEN_DELETES);
  private MultiLineTextParameter templateParameter = new MultiLineTextParameter(
       "templateLines", "Entry Template Lines",
       "The template that should be used to generate the entries to add.",
       DEFAULT_TEMPLATE_LINES, true);
  private StringParameter baseDNParameter = new StringParameter("baseDN",
       "Base DN",
       "The DN of the entry below which to perform the adds and deletes.",
       true, null);
  private StringParameter rdnAttributeParameter = new StringParameter("rdnAttr",
       "RDN Attribute",
       "The name of the attribute to use as the RDN attribute for entries " +
            "that are generated.  It must be unique among all other entries " +
            "being generated and that may already exist in the server below " +
            "the base DN.",
       true, "uid");



  /**
   * Creates a new instance of this job class.
   */
  public LDAPAddAndDeleteRateJobClass()
  {
    templateParameter.setVisibleColumns(80);
    templateParameter.setVisibleRows(20);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Add and Delete Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP add and delete operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job may be used to perform repeated add and delete operations " +
      "against an LDAP directory server.  The entries to be added will be " +
      "generated based on a user-defined template.  The entries will be " +
      "deleted after all add processing has been completed and an optional " +
      "delay.",

      "It is possible to use this job to perform only add operations, only " +
      "delete operations, or both adds and deletes.  If both add and delete " +
      "operations are to be performed, then it is possible to configure the " +
      "job so that it completes all add operations prior to processing any " +
      "of the deletes, or to delete each entry immediately after it has been " +
      "added."
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
         baseDNParameter,
         rdnAttributeParameter,
         firstNumberParameter,
         lastNumberParameter,
         new PlaceholderParameter(),
         templateParameter,
         new PlaceholderParameter(),
         processingTypeParameter,
         thresholdParameter,
         maxRateParameter,
         rateLimitDurationParameter,
         timeBetweenRequestsParameter,
         timeBetweenAddsAndDelsParameter);
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
      new IncrementalTracker(clientID, threadID, STAT_ADDS_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_ADD_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_ADD_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_ADDS_EXCEEDING_THRESHOLD,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_DELETES_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_DELETE_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_DELETE_RESULT_CODES,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_DELETES_EXCEEDING_THRESHOLD,
                             collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    ArrayList<StatTracker> statList = new ArrayList<StatTracker>(8);
    if (performAdds)
    {
      statList.add(addsCompleted);
      statList.add(addTimer);
      statList.add(addResultCodes);

      if (responseTimeThreshold > 0)
      {
        statList.add(addsExceedingThreshold);
      }
    }

    if (performDeletes)
    {
      statList.add(deletesCompleted);
      statList.add(deleteTimer);
      statList.add(deleteResultCodes);

      if (responseTimeThreshold > 0)
      {
        statList.add(deletesExceedingThreshold);
      }
    }

    StatTracker[] trackers = new StatTracker[statList.size()];
    return statList.toArray(trackers);
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


    // Make sure that the last entry number is greater than the first entry
    // number.
    IntegerParameter firstParam =
         parameters.getIntegerParameter(firstNumberParameter.getName());
    IntegerParameter lastParam =
         parameters.getIntegerParameter(lastNumberParameter.getName());
    if (firstParam.getIntValue() >= lastParam.getIntValue())
    {
      throw new InvalidValueException("The last entry number must be greater " +
           "than the first entry number.");
    }


    // Make sure that the template can be parsed.
    MultiLineTextParameter tmplParam =
         parameters.getMultiLineTextParameter(templateParameter.getName());
    try
    {
      new TemplateBasedEntryGenerator(tmplParam.getNonBlankLines(),
                                      firstParam.getIntValue());
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Unable to parse the template:  " +
           String.valueOf(e), e);
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
    activeAddThreads =
         new AtomicInteger(getClientSideJob().getThreadsPerClient());

    baseDNParameter = parameters.getStringParameter(baseDNParameter.getName());
    baseDN = baseDNParameter.getStringValue();

    rdnAttributeParameter =
         parameters.getStringParameter(rdnAttributeParameter.getName());
    rdnAttribute = rdnAttributeParameter.getStringValue();


    firstNumberParameter =
         parameters.getIntegerParameter(firstNumberParameter.getName());
    firstEntryNumber = firstNumberParameter.getIntValue();
    entryNumber = new AtomicInteger(firstEntryNumber);


    lastNumberParameter =
         parameters.getIntegerParameter(lastNumberParameter.getName());
    lastEntryNumber = lastNumberParameter.getIntValue();


    templateParameter =
         parameters.getMultiLineTextParameter(templateParameter.getName());
    String[] templateLines = templateParameter.getNonBlankLines();
    try
    {
      entryGenerator =
           new TemplateBasedEntryGenerator(templateLines, firstEntryNumber);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to create the entry generator:  " +
           String.valueOf(e), e);
    }


    alternateAddsAndDeletes = false;
    performAdds             = true;
    performDeletes          = true;
    processingTypeParameter = parameters.getMultiChoiceParameter(
         processingTypeParameter.getName());
    if ((processingTypeParameter != null) && processingTypeParameter.hasValue())
    {
      String val = processingTypeParameter.getStringValue();
      if (val.equalsIgnoreCase(PROCESSING_TYPE_ALTERNATE_ADDS_AND_DELETES))
      {
        alternateAddsAndDeletes = true;
      }
      else if (val.equalsIgnoreCase(PROCESSING_TYPE_ONLY_ADDS))
      {
        performDeletes = false;
      }
      else if (val.equalsIgnoreCase(PROCESSING_TYPE_ONLY_DELETES))
      {
        performAdds = false;
      }
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


    timeBetweenRequests = 0L;
    timeBetweenRequestsParameter =
         parameters.getIntegerParameter(timeBetweenRequestsParameter.getName());
    if ((timeBetweenRequestsParameter != null) &&
        timeBetweenRequestsParameter.hasValue())
    {
      timeBetweenRequests = timeBetweenRequestsParameter.getIntValue();
    }


    timeBetweenAddsAndDeletes = 0L;
    timeBetweenAddsAndDelsParameter = parameters.getIntegerParameter(
         timeBetweenAddsAndDelsParameter.getName());
    if ((timeBetweenAddsAndDelsParameter != null) &&
        timeBetweenAddsAndDelsParameter.hasValue())
    {
      timeBetweenAddsAndDeletes =
           1000L * timeBetweenAddsAndDelsParameter.getIntValue();
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
    addsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_ADDS_COMPLETED, collectionInterval);
    addTimer = new TimeTracker(clientID, threadID, STAT_ADD_DURATION,
         collectionInterval);
    addResultCodes = new CategoricalTracker(clientID, threadID,
         STAT_ADD_RESULT_CODES, collectionInterval);
    addsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_ADDS_EXCEEDING_THRESHOLD, collectionInterval);
    deletesCompleted = new IncrementalTracker(clientID, threadID,
         STAT_DELETES_COMPLETED, collectionInterval);
    deleteTimer = new TimeTracker(clientID, threadID, STAT_DELETE_DURATION,
         collectionInterval);
    deleteResultCodes = new CategoricalTracker(clientID, threadID,
         STAT_DELETE_RESULT_CODES, collectionInterval);
    deletesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         STAT_DELETES_EXCEEDING_THRESHOLD, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      addsCompleted.enableRealTimeStats(statReporter, jobID);
      addTimer.enableRealTimeStats(statReporter, jobID);
      addsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      deletesCompleted.enableRealTimeStats(statReporter, jobID);
      deleteTimer.enableRealTimeStats(statReporter, jobID);
      deletesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
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
    StringBuilder dnBuffer = new StringBuilder();


    // Start collecting the appropriate set of statistics.
    if (performAdds)
    {
      addsCompleted.startTracker();
      addTimer.startTracker();
      addResultCodes.startTracker();
      addsExceedingThreshold.startTracker();
    }

    if (performDeletes && alternateAddsAndDeletes)
    {
      deletesCompleted.startTracker();
      deleteTimer.startTracker();
      deleteResultCodes.startTracker();
      deletesExceedingThreshold.startTracker();
    }


    if (performAdds)
    {
      while (! shouldStop())
      {
        if (rateLimiter != null)
        {
          if (rateLimiter.await())
          {
            continue;
          }
        }

        long addStartTime = System.currentTimeMillis();
        int i = entryNumber.getAndIncrement();
        if (i > lastEntryNumber)
        {
          break;
        }

        Entry entry;
        try
        {
          dnBuffer.setLength(0);
          dnBuffer.append(rdnAttribute);
          dnBuffer.append('=');
          dnBuffer.append(i);
          dnBuffer.append(',');
          dnBuffer.append(baseDN);
          entry = entryGenerator.createEntry(random, i, dnBuffer.toString());
        }
        catch (SLAMDException se)
        {
          addResultCodes.increment(ResultCode.PARAM_ERROR.toString());
          continue;
        }


        addTimer.startTimer();

        try
        {
          LDAPResult addResult = conn.add(entry);
          addResultCodes.increment(addResult.getResultCode().toString());
        }
        catch (LDAPException le)
        {
          addResultCodes.increment(le.getResultCode().toString());
        }
        finally
        {
          addTimer.stopTimer();
          addsCompleted.increment();

          if ((responseTimeThreshold > 0) &&
              (addTimer.getLastOperationTime() > responseTimeThreshold))
          {
            addsExceedingThreshold.increment();
          }
        }

        if (timeBetweenRequests > 0)
        {
          long elapsedTime = System.currentTimeMillis() - addStartTime;
          long sleepTime   = timeBetweenRequests - elapsedTime;
          if (sleepTime > 0)
          {
            try
            {
              Thread.sleep(sleepTime);
            } catch (Exception e) {}
          }
        }

        if (alternateAddsAndDeletes)
        {
          if (rateLimiter != null)
          {
            rateLimiter.await();
          }

          long deleteStartTime = System.currentTimeMillis();
          deleteTimer.startTimer();
          try
          {
            LDAPResult deleteResult = conn.delete(entry.getDN());
            deleteResultCodes.increment(
                 deleteResult.getResultCode().toString());
          }
          catch (LDAPException le)
          {
            deleteResultCodes.increment(le.getResultCode().toString());
          }
          finally
          {
            deleteTimer.stopTimer();
            deletesCompleted.increment();

            if ((responseTimeThreshold > 0) &&
                (deleteTimer.getLastOperationTime() > responseTimeThreshold))
            {
              deletesExceedingThreshold.increment();
            }
          }

          if (timeBetweenRequests > 0)
          {
            long elapsedTime = System.currentTimeMillis() - deleteStartTime;
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
      }
    }

    if (performAdds)
    {
      addsCompleted.stopTracker();
      addTimer.stopTracker();
      addResultCodes.stopTracker();
      addsExceedingThreshold.stopTracker();

      if (alternateAddsAndDeletes)
      {
        deletesCompleted.stopTracker();
        deleteTimer.stopTracker();
        deleteResultCodes.stopTracker();
        deletesExceedingThreshold.stopTracker();
      }
    }


    if (performDeletes && (! alternateAddsAndDeletes))
    {
      // Wait until all of the threads have completed their adds, and then sleep
      // if necessary before starting the deletes.
      if (performAdds)
      {
        int remainingActive = activeAddThreads.decrementAndGet();
        if (remainingActive == 0)
        {
          if (timeBetweenAddsAndDeletes > 0)
          {
            try
            {
              Thread.sleep(timeBetweenAddsAndDeletes);
            } catch (Exception e) {}
          }

          entryNumber.set(firstEntryNumber);
          activeAddThreads.decrementAndGet();
        }
        else
        {
          while (activeAddThreads.get() >= 0)
          {
            try
            {
              Thread.sleep(0, 1);
            } catch (Exception e) {}
          }
        }
      }


      // Perform all of the deletes.
      deletesCompleted.startTracker();
      deleteTimer.startTracker();
      deleteResultCodes.startTracker();
      deletesExceedingThreshold.startTracker();

      while (! shouldStop())
      {
        if (rateLimiter != null)
        {
          if (rateLimiter.await())
          {
            continue;
          }
        }

        long deleteStartTime = System.currentTimeMillis();
        int i = entryNumber.getAndIncrement();
        if (i > lastEntryNumber)
        {
          break;
        }

        dnBuffer.setLength(0);
        dnBuffer.append(rdnAttribute);
        dnBuffer.append('=');
        dnBuffer.append(i);
        dnBuffer.append(',');
        dnBuffer.append(baseDN);
        deleteTimer.startTimer();

        try
        {
          LDAPResult deleteResult = conn.delete(dnBuffer.toString());
          deleteResultCodes.increment(deleteResult.getResultCode().toString());
        }
        catch (LDAPException le)
        {
          deleteResultCodes.increment(le.getResultCode().toString());
        }
        finally
        {
          deleteTimer.stopTimer();
          deletesCompleted.increment();

          if ((responseTimeThreshold > 0) &&
              (deleteTimer.getLastOperationTime() > responseTimeThreshold))
          {
            deletesExceedingThreshold.increment();
          }
        }

        if (timeBetweenRequests > 0)
        {
          long elapsedTime = System.currentTimeMillis() - deleteStartTime;
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

      deletesCompleted.stopTracker();
      deleteTimer.stopTracker();
      deleteResultCodes.stopTracker();
      deletesExceedingThreshold.stopTracker();
    }
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
