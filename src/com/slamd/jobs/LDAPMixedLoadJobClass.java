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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LongParameter;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a SLAMD job class that may be used to perform a mix of
 * operations against an LDAP directory server.
 */
public class LDAPMixedLoadJobClass
       extends LDAPJobClass
{
  /**
   * The set of characters to include in the values to use for the target
   * attributes for modify operations.
   */
  private static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



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
   * The name of the stat tracker used to keep track of the number of operations
   * of each type that are performed.
   */
  private static final String STAT_OPERATION_TYPES = "Operation Types";



  /**
   * The name of the stat tracker used to keep track of the number of entries
   * returned from searches.
   */
  private static final String STAT_ENTRIES_RETURNED = "Search Entries Returned";



  /**
   * The end of the display name for the stat tracker used to track operations
   * completed.
   */
  private static final String STAT_SUFFIX_COMPLETED = " Operations Completed";



  /**
   * The end of the display name for the stat tracker used to track operation
   * durations.
   */
  private static final String STAT_SUFFIX_DURATION = " Operation Duration (ms)";



  /**
   * The end of the display name for the stat tracker used to track operations
   * exceeding the response time threshold.
   */
  private static final String STAT_SUFFIX_EXCEEDING_THRESHOLD =
       " Operations Exceeding the Response Time Threshold";



  /**
   * The end of the display name for the stat tracker used to track result
   * codes.
   */
  private static final String STAT_SUFFIX_RESULT_CODES = " Result Codes";



  // Variables used to hold the values of the parameters.
  private static boolean     cleanUp;
  private static int         addWeight;
  private static int         bindWeight;
  private static int         compareWeight;
  private static int         coolDownTime;
  private static int         deleteWeight;
  private static int         dn1Percentage;
  private static int         filter1Percentage;
  private static int         modifyWeight;
  private static int         modifyDNWeight;
  private static int         responseTimeThreshold;
  private static int         searchWeight;
  private static int         warmUpTime;
  private static int         valueLength;
  private static long        maxOpsPerThread;
  private static long        opsBeforeReconnect;
  private static long        timeBetweenRequests;
  private static SearchScope scope;
  private static String      attributeName;
  private static String      baseDN;
  private static String      entryDN1;
  private static String      entryDN2;
  private static String      filter1;
  private static String      filter2;
  private static String      rdnAttribute;
  private static String      userPassword;
  private static String[]    attributes;

  // Stat trackers used by this job.
  private CategoricalTracker  operationTypes;
  private CategoricalTracker  overallResultCodes;
  private CategoricalTracker  addResultCodes;
  private CategoricalTracker  bindResultCodes;
  private CategoricalTracker  compareResultCodes;
  private CategoricalTracker  deleteResultCodes;
  private CategoricalTracker  modifyResultCodes;
  private CategoricalTracker  modifyDNResultCodes;
  private CategoricalTracker  searchResultCodes;
  private IncrementalTracker  overallCompleted;
  private IncrementalTracker  addsCompleted;
  private IncrementalTracker  bindsCompleted;
  private IncrementalTracker  comparesCompleted;
  private IncrementalTracker  deletesCompleted;
  private IncrementalTracker  modifiesCompleted;
  private IncrementalTracker  modifyDNsCompleted;
  private IncrementalTracker  searchesCompleted;
  private IncrementalTracker  overallExceedingThreshold;
  private IncrementalTracker  addsExceedingThreshold;
  private IncrementalTracker  bindsExceedingThreshold;
  private IncrementalTracker  comparesExceedingThreshold;
  private IncrementalTracker  deletesExceedingThreshold;
  private IncrementalTracker  modifiesExceedingThreshold;
  private IncrementalTracker  modifyDNsExceedingThreshold;
  private IncrementalTracker  searchesExceedingThreshold;
  private IntegerValueTracker searchEntriesReturned;
  private TimeTracker         overallTimer;
  private TimeTracker         addTimer;
  private TimeTracker         bindTimer;
  private TimeTracker         compareTimer;
  private TimeTracker         deleteTimer;
  private TimeTracker         modifyTimer;
  private TimeTracker         modifyDNTimer;
  private TimeTracker         searchTimer;

  // Indicates whether the job is currently collecting statistics.
  private boolean collectingStats;

  // Variables used to figure out which kinds of operations to perform.
  private static int totalWeight;
  private static int addWeightThreshold;
  private static int bindWeightThreshold;
  private static int compareWeightThreshold;
  private static int deleteWeightThreshold;
  private static int modifyWeightThreshold;
  private static int modifyDNWeightThreshold;

  // Variables used to keep track of entries for add, delete, and modify DN
  // operations.
  private static TemplateBasedEntryGenerator entryGenerator;
  private int addCounter;
  private LinkedList<String> addList;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // The search request for search operations.
  private SearchRequest searchRequest;

  // Random number generators used by this job.
  private static Random parentRandom;
  private Random random;

  // Value patterns used for the entry DNs and filters.
  private static ValuePattern dn1Pattern;
  private static ValuePattern dn2Pattern;
  private static ValuePattern filter1Pattern;
  private static ValuePattern filter2Pattern;

  // The LDAP connections used by this thread.
  private LDAPConnection opConn;
  private LDAPConnection bindConn;

  // The parameters used by this job.
  private BooleanParameter cleanUpParameter = new BooleanParameter(
       "cleanUp", "Clean Up When Done",
       "Ensure that any entries created during the course of the job are " +
            "deleted when the job has completed.", true);
  private IntegerParameter addWeightParameter = new IntegerParameter(
       "addWeight", "Add Weight",
       "The frequency with which to perform add operations relative to the " +
            "weights of the other types of operations.",
       true, 0, true, 0, false, 0);
  private IntegerParameter bindWeightParameter = new IntegerParameter(
       "bindWeight", "Bind Weight",
       "The frequency with which to perform bind operations relative to the " +
            "weights of the other types of operations.",
       true, 0, true, 0, false, 0);
  private IntegerParameter compareWeightParameter = new IntegerParameter(
       "compareWeight", "Compare Weight",
       "The frequency with which to perform compare operations relative to " +
            "the weights of the other types of operations.",
       true, 0, true, 0, false, 0);
  private IntegerParameter coolDownParameter = new IntegerParameter(
       "coolDownTime", "Cool Down Time",
       "The length of time in seconds to continue running after ending " +
            "statistics collection.",
       true, 0, true, 0, false, 0);
  private IntegerParameter deleteWeightParameter = new IntegerParameter(
       "deleteWeight", "Delete Weight",
       "The frequency with which to perform delete operations relative to " +
            "the weights of the other types of operations.",
       true, 0, true, 0, false, 0);
  private IntegerParameter dn1PercentageParameter = new IntegerParameter(
       "dn1Percentage", "DN 1 Percentage",
       "The percentage of bind, compare, and modify operations which should " +
            "use the first DN pattern.",
       true, 50, true, 0, true, 100);
  private IntegerParameter filter1PercentageParameter = new IntegerParameter(
       "filter1Percentage", "Filter 1 Percentage",
       "The percentage of search operations which should use the first " +
            "filter pattern.",
       true, 50, true, 0, true, 100);
  private IntegerParameter lengthParameter = new IntegerParameter("length",
       "Value Length",
       "The number of characters to include in generated values for compare " +
            "and modify operations.",
       true, 80, true, 1, false, 0);
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Operation Rate (Ops/Second/Client)",
       "Specifies the maximum operation rate (in operations per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform operations as quickly as possible.",
       true, -1);
  private IntegerParameter modifyWeightParameter = new IntegerParameter(
       "modifyWeight", "Modify Weight",
       "The frequency with which to perform modify operations relative to " +
            "the weights of the other types of operations.",
       true, 0, true, 0, false, 0);
  private IntegerParameter modifyDNWeightParameter = new IntegerParameter(
       "modifyDNWeight", "Modify DN Weight",
       "The frequency with which to perform modify DN operations relative to " +
            "the weights of the other types of operations.",
       true, 0, true, 0, false, 0);
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
  private IntegerParameter searchWeightParameter = new IntegerParameter(
       "searchWeight", "Search Weight",
       "The frequency with which to perform search operations relative to " +
            "the weights of the other types of operations.",
       true, 0, true, 0, false, 0);
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
  private LongParameter maxOpsPerThreadParameter = new LongParameter(
       "maxOpsPerThread", "Max Ops per Thread",
       "The maximum number of operations to process in each thread.  A " +
            "value of zero indicates that there will be no maximum number of " +
            "operations per thread, and the job should stop only after it " +
            "has run for the configured duration, when the stop time has " +
            "been reached, or when it has been canceled by an administrator.",
       true, 0L, true, 0L, false, 0L);
  private LongParameter opsBeforeReconnectParameter = new LongParameter(
       "opsBeforeReconnect", "Ops Before Reconnect",
       "The number of operations to process on a connection before closing " +
            "and re-establishing that connection.  A value of zero indicates " +
            "that the connection should never be closed.  A value of one " +
            "indicates that the connection should be closed and " +
            "re-established after each operation.  A value greater than one " +
            "indicates that the connection should be closed and " +
            "re-established after the specified number of operations.",
       true, 0L, true, 0L, false, 0L);
  private MultiChoiceParameter scopeParameter = new MultiChoiceParameter(
       "scope", "Search Scope", "The scope to use for search operations.",
       SCOPES, SCOPE_STR_BASE);
  private MultiLineTextParameter attributesParameter =
       new MultiLineTextParameter("attributes", "Attributes to Return",
                "The set of attributes to return in search result entries.  " +
                     "If no attribute names are provided, then all user " +
                     "attributes will be returned.",
                null, false);
  private MultiLineTextParameter templateParameter = new MultiLineTextParameter(
       "templateLines", "Entry Template Lines",
       "The template that should be used to generate the entries to add.",
       DEFAULT_TEMPLATE_LINES, true);
  private PasswordParameter userPWParameter = new PasswordParameter("userPW",
       "User Password", "The password to use for bind operations.", true, null);
  private StringParameter attributeParameter = new StringParameter("attribute",
       "Attribute to Compare/Modify",
       "The name of the attribute to target with compare and modify " +
            "operations.",
       true, "description");
  private StringParameter baseDNParameter = new StringParameter("baseDN",
       "Base DN",
       "The base DN to use for search operations, and the entry below which " +
            "entries will be created for add operations.",
       true, null);
  private StringParameter dn1Parameter = new StringParameter("dn1",
       "Entry DN 1",
       "The target DN for bind, compare, and modify operations that fall " +
            "into the first category.",
       true, null);
  private StringParameter dn2Parameter = new StringParameter("dn2",
       "Entry DN 2",
       "The target DN for bind, compare, and modify operations that fall " +
            "into the second category.",
       true, null);
  private StringParameter filter1Parameter = new StringParameter("filter1",
       "Search Filter 1",
       "The filter for search operations that fall into the first category.",
       true, null);
  private StringParameter filter2Parameter = new StringParameter("filter2",
       "Search Filter 2",
       "The filter for search operations that fall into the second category.",
       true, null);
  private StringParameter rdnAttrParameter = new StringParameter("rdnAttr",
       "RDN Attribute",
       "The RDN attribute to use for add and modify DN operations.", true,
       "uid");



  /**
   * Creates a new instance of this job class.
   */
  public LDAPMixedLoadJobClass()
  {
    super();

    templateParameter.setVisibleColumns(80);
    templateParameter.setVisibleRows(20);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Mixed Load";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP operations of various types";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated operations against an LDAP " +
      "LDAP directory server.  It supports add, bind, compare, modify, " +
      "modify DN, and search operations.  The relative frequencies of each " +
      "type of operation may be controlled by defining weights for those " +
      "operation types.  If a given type of operation is not needed, then it " +
      "may be given a weight of zero.",

      "The entries to be deleted or renamed will be taken from the set of " +
      "entries that have been added over the course of the job, so the " +
      "weight assigned to add operations must be greater than or equal to " +
      "the combined weights assigned to delete and modify DN operations."
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
         addWeightParameter,
         bindWeightParameter,
         compareWeightParameter,
         deleteWeightParameter,
         modifyWeightParameter,
         modifyDNWeightParameter,
         searchWeightParameter,
         new PlaceholderParameter(),
         dn1Parameter,
         dn2Parameter,
         dn1PercentageParameter,
         new PlaceholderParameter(),
         attributeParameter,
         lengthParameter,
         userPWParameter,
         new PlaceholderParameter(),
         baseDNParameter,
         filter1Parameter,
         filter2Parameter,
         filter1PercentageParameter,
         scopeParameter,
         attributesParameter,
         new PlaceholderParameter(),
         rdnAttrParameter,
         templateParameter,
         new PlaceholderParameter(),
         warmUpParameter,
         coolDownParameter,
         cleanUpParameter,
         opsBeforeReconnectParameter,
         maxOpsPerThreadParameter,
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
    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>(33);
    String[] ops =
    {
      "Overall",
      "Add",
      "Bind",
      "Compare",
      "Delete",
      "Modify",
      "Modify DN",
      "Search"
    };

    for (String op : ops)
    {
      trackerList.add(new IncrementalTracker(clientID, threadID,
           op + STAT_SUFFIX_COMPLETED, collectionInterval));
      trackerList.add(new TimeTracker(clientID, threadID,
           op + STAT_SUFFIX_DURATION, collectionInterval));
      trackerList.add(new CategoricalTracker(clientID, threadID,
           op + STAT_SUFFIX_RESULT_CODES, collectionInterval));
      trackerList.add(new IncrementalTracker(clientID, threadID,
           op + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval));
    }

    trackerList.add(new IntegerValueTracker(clientID, threadID,
         STAT_ENTRIES_RETURNED, collectionInterval));
    trackerList.add(new CategoricalTracker(clientID, threadID,
         STAT_OPERATION_TYPES, collectionInterval));

    StatTracker[] trackers = new StatTracker[trackerList.size()];
    return trackerList.toArray(trackers);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>(33);
    trackerList.add(overallCompleted);
    trackerList.add(overallTimer);
    trackerList.add(overallResultCodes);
    if (responseTimeThreshold > 0)
    {
      trackerList.add(overallExceedingThreshold);
    }

    if (addWeight > 0)
    {
      trackerList.add(addsCompleted);
      trackerList.add(addTimer);
      trackerList.add(addResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(addsExceedingThreshold);
      }
    }

    if (bindWeight > 0)
    {
      trackerList.add(bindsCompleted);
      trackerList.add(bindTimer);
      trackerList.add(bindResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(bindsExceedingThreshold);
      }
    }

    if (compareWeight > 0)
    {
      trackerList.add(comparesCompleted);
      trackerList.add(compareTimer);
      trackerList.add(compareResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(comparesExceedingThreshold);
      }
    }

    if (deleteWeight > 0)
    {
      trackerList.add(deletesCompleted);
      trackerList.add(deleteTimer);
      trackerList.add(deleteResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(deletesExceedingThreshold);
      }
    }

    if (modifyWeight > 0)
    {
      trackerList.add(modifiesCompleted);
      trackerList.add(modifyTimer);
      trackerList.add(modifyResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(modifiesExceedingThreshold);
      }
    }

    if (modifyDNWeight > 0)
    {
      trackerList.add(modifyDNsCompleted);
      trackerList.add(modifyDNTimer);
      trackerList.add(modifyDNResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(modifyDNsExceedingThreshold);
      }
    }

    if (searchWeight > 0)
    {
      trackerList.add(searchesCompleted);
      trackerList.add(searchTimer);
      trackerList.add(searchResultCodes);
      if (responseTimeThreshold > 0)
      {
        trackerList.add(searchesExceedingThreshold);
      }
      trackerList.add(searchEntriesReturned);
    }

    trackerList.add(operationTypes);

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


    // The filter parameters must be parseable as value patterns.
    p = parameters.getStringParameter(filter1Parameter.getName());
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


    // At least one operation type must have a nonzero weight.
    String[] names =
    {
      addWeightParameter.getName(),
      bindWeightParameter.getName(),
      compareWeightParameter.getName(),
      deleteWeightParameter.getName(),
      modifyWeightParameter.getName(),
      modifyDNWeightParameter.getName(),
      searchWeightParameter.getName(),
    };

    int total = 0;
    for (String s : names)
    {
      IntegerParameter ip = parameters.getIntegerParameter(s);
      if ((ip != null) && ip.hasValue())
      {
        total++;
      }
    }

    if (total == 0)
    {
      throw new InvalidValueException("At least one operation must have a " +
           "positive weight.");
    }


    // The add weight must be greater than or equal to the sum of the delete
    // and modify DN weights.
    int addWt = 0;
    IntegerParameter addWtParam =
         parameters.getIntegerParameter(addWeightParameter.getName());
    if ((addWtParam != null) && addWtParam.hasValue())
    {
      addWt = addWtParam.getIntValue();
    }

    IntegerParameter deleteWtParam =
         parameters.getIntegerParameter(deleteWeightParameter.getName());
    if ((deleteWtParam != null) && deleteWtParam.hasValue())
    {
      addWt -= deleteWtParam.getIntValue();
    }

    IntegerParameter modDNWtParam =
         parameters.getIntegerParameter(modifyDNWeightParameter.getName());
    if ((modDNWtParam != null) && modDNWtParam.hasValue())
    {
      addWt -= modDNWtParam.getIntValue();
    }

    if (addWt < 0)
    {
      throw new InvalidValueException("The add weight must be greater than " +
           "or equal to the sum of the delete and modify DN weights.");
    }


    // Make sure that the template can be parsed.
    MultiLineTextParameter tmplParam =
         parameters.getMultiLineTextParameter(templateParameter.getName());
    try
    {
      new TemplateBasedEntryGenerator(tmplParam.getNonBlankLines(), 1);
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


    totalWeight = 0;
    addWeight = 0;
    addWeightParameter =
         parameters.getIntegerParameter(addWeightParameter.getName());
    if ((addWeightParameter != null) && addWeightParameter.hasValue())
    {
      addWeight = addWeightParameter.getIntValue();
      totalWeight += addWeight;
    }
    addWeightThreshold = totalWeight;


    bindWeight = 0;
    bindWeightParameter =
         parameters.getIntegerParameter(bindWeightParameter.getName());
    if ((bindWeightParameter != null) && bindWeightParameter.hasValue())
    {
      bindWeight = bindWeightParameter.getIntValue();
      totalWeight += bindWeight;
    }
    bindWeightThreshold = totalWeight;


    compareWeight = 0;
    compareWeightParameter =
         parameters.getIntegerParameter(compareWeightParameter.getName());
    if ((compareWeightParameter != null) && compareWeightParameter.hasValue())
    {
      compareWeight = compareWeightParameter.getIntValue();
      totalWeight += compareWeight;
    }
    compareWeightThreshold = totalWeight;


    deleteWeight = 0;
    deleteWeightParameter =
         parameters.getIntegerParameter(deleteWeightParameter.getName());
    if ((deleteWeightParameter != null) && deleteWeightParameter.hasValue())
    {
      deleteWeight = deleteWeightParameter.getIntValue();
      totalWeight += deleteWeight;
    }
    deleteWeightThreshold = totalWeight;


    modifyWeight = 0;
    modifyWeightParameter =
         parameters.getIntegerParameter(modifyWeightParameter.getName());
    if ((modifyWeightParameter != null) && modifyWeightParameter.hasValue())
    {
      modifyWeight = modifyWeightParameter.getIntValue();
      totalWeight += modifyWeight;
    }
    modifyWeightThreshold = totalWeight;


    modifyDNWeight = 0;
    modifyDNWeightParameter =
         parameters.getIntegerParameter(modifyDNWeightParameter.getName());
    if ((modifyDNWeightParameter != null) && modifyDNWeightParameter.hasValue())
    {
      modifyDNWeight = modifyDNWeightParameter.getIntValue();
      totalWeight += modifyDNWeight;
    }
    modifyDNWeightThreshold = totalWeight;


    searchWeight = 0;
    searchWeightParameter =
         parameters.getIntegerParameter(searchWeightParameter.getName());
    if ((searchWeightParameter != null) && searchWeightParameter.hasValue())
    {
      searchWeight = searchWeightParameter.getIntValue();
      totalWeight += searchWeight;
    }


    opsBeforeReconnect = Long.MAX_VALUE;
    opsBeforeReconnectParameter =
         parameters.getLongParameter(opsBeforeReconnectParameter.getName());
    if ((opsBeforeReconnectParameter != null) &&
        opsBeforeReconnectParameter.hasValue())
    {
      opsBeforeReconnect = opsBeforeReconnectParameter.getLongValue();
      if (opsBeforeReconnect <= 0L)
      {
        opsBeforeReconnect = Long.MAX_VALUE;
      }
    }


    maxOpsPerThread = Long.MAX_VALUE;
    maxOpsPerThreadParameter =
         parameters.getLongParameter(maxOpsPerThreadParameter.getName());
    if ((maxOpsPerThreadParameter != null) &&
        maxOpsPerThreadParameter.hasValue())
    {
      maxOpsPerThread = maxOpsPerThreadParameter.getLongValue();
      if (maxOpsPerThread <= 0L)
      {
        maxOpsPerThread = Long.MAX_VALUE;
      }
    }



    dn1Parameter = parameters.getStringParameter(dn1Parameter.getName());
    entryDN1 = dn1Parameter.getStringValue();


    dn2Parameter = parameters.getStringParameter(dn2Parameter.getName());
    entryDN2 = dn2Parameter.getStringValue();


    dn1Percentage = 50;
    dn1PercentageParameter =
         parameters.getIntegerParameter(dn1PercentageParameter.getName());
    if ((dn1PercentageParameter != null) && dn1PercentageParameter.hasValue())
    {
      dn1Percentage = dn1PercentageParameter.getIntValue();
    }


    attributeParameter =
         parameters.getStringParameter(attributeParameter.getName());
    attributeName = attributeParameter.getStringValue();


    valueLength = 80;
    lengthParameter = parameters.getIntegerParameter(lengthParameter.getName());
    if ((lengthParameter != null) && lengthParameter.hasValue())
    {
      valueLength = lengthParameter.getIntValue();
    }


    userPWParameter =
         parameters.getPasswordParameter(userPWParameter.getName());
    userPassword = userPWParameter.getStringValue();


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
    filter1PercentageParameter =
         parameters.getIntegerParameter(filter1PercentageParameter.getName());
    if ((filter1PercentageParameter != null) &&
        filter1PercentageParameter.hasValue())
    {
      filter1Percentage = filter1PercentageParameter.getIntValue();
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


    rdnAttrParameter =
         parameters.getStringParameter(rdnAttrParameter.getName());
    rdnAttribute = rdnAttrParameter.getStringValue();


    templateParameter =
         parameters.getMultiLineTextParameter(templateParameter.getName());

    try
    {
      entryGenerator = new TemplateBasedEntryGenerator(
           templateParameter.getNonBlankLines(), 1);
    }
    catch (Exception e)
    {
      throw new UnableToRunException(
           "Cannot initialize the entry generator:  " + String.valueOf(e), e);
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


    cleanUp = true;
    cleanUpParameter =
         parameters.getBooleanParameter(cleanUpParameter.getName());
    if ((cleanUpParameter != null) && cleanUpParameter.hasValue())
    {
      cleanUp = false;
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
    overallCompleted = new IncrementalTracker(clientID, threadID,
         "Overall" + STAT_SUFFIX_COMPLETED, collectionInterval);
    overallTimer = new TimeTracker(clientID, threadID,
         "Overall" + STAT_SUFFIX_DURATION, collectionInterval);
    overallResultCodes = new CategoricalTracker(clientID, threadID,
         "Overall" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    overallExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Overall" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    addsCompleted = new IncrementalTracker(clientID, threadID,
         "Add" + STAT_SUFFIX_COMPLETED, collectionInterval);
    addTimer = new TimeTracker(clientID, threadID,
         "Add" + STAT_SUFFIX_DURATION, collectionInterval);
    addResultCodes = new CategoricalTracker(clientID, threadID,
         "Add" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    addsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Add" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    bindsCompleted = new IncrementalTracker(clientID, threadID,
         "Bind" + STAT_SUFFIX_COMPLETED, collectionInterval);
    bindTimer = new TimeTracker(clientID, threadID,
         "Bind" + STAT_SUFFIX_DURATION, collectionInterval);
    bindResultCodes = new CategoricalTracker(clientID, threadID,
         "Bind" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    bindsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Bind" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    comparesCompleted = new IncrementalTracker(clientID, threadID,
         "Compare" + STAT_SUFFIX_COMPLETED, collectionInterval);
    compareTimer = new TimeTracker(clientID, threadID,
         "Compare" + STAT_SUFFIX_DURATION, collectionInterval);
    compareResultCodes = new CategoricalTracker(clientID, threadID,
         "Compare" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    comparesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Compare" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    deletesCompleted = new IncrementalTracker(clientID, threadID,
         "Delete" + STAT_SUFFIX_COMPLETED, collectionInterval);
    deleteTimer = new TimeTracker(clientID, threadID,
         "Delete" + STAT_SUFFIX_DURATION, collectionInterval);
    deleteResultCodes = new CategoricalTracker(clientID, threadID,
         "Delete" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    deletesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Delete" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    modifiesCompleted = new IncrementalTracker(clientID, threadID,
         "Modify" + STAT_SUFFIX_COMPLETED, collectionInterval);
    modifyTimer = new TimeTracker(clientID, threadID,
         "Modify" + STAT_SUFFIX_DURATION, collectionInterval);
    modifyResultCodes = new CategoricalTracker(clientID, threadID,
         "Modify" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    modifiesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Modify" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    modifyDNsCompleted = new IncrementalTracker(clientID, threadID,
         "Modify DN" + STAT_SUFFIX_COMPLETED, collectionInterval);
    modifyDNTimer = new TimeTracker(clientID, threadID,
         "Modify DN" + STAT_SUFFIX_DURATION, collectionInterval);
    modifyDNResultCodes = new CategoricalTracker(clientID, threadID,
         "Modify DN" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    modifyDNsExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Modify DN" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    searchesCompleted = new IncrementalTracker(clientID, threadID,
         "Search" + STAT_SUFFIX_COMPLETED, collectionInterval);
    searchTimer = new TimeTracker(clientID, threadID,
         "Search" + STAT_SUFFIX_DURATION, collectionInterval);
    searchResultCodes = new CategoricalTracker(clientID, threadID,
         "Search" + STAT_SUFFIX_RESULT_CODES, collectionInterval);
    searchesExceedingThreshold = new IncrementalTracker(clientID, threadID,
         "Search" + STAT_SUFFIX_EXCEEDING_THRESHOLD, collectionInterval);
    searchEntriesReturned = new IntegerValueTracker(clientID, threadID,
         STAT_ENTRIES_RETURNED, collectionInterval);
    operationTypes = new CategoricalTracker(clientID, threadID,
         STAT_OPERATION_TYPES, collectionInterval);

    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      overallCompleted.enableRealTimeStats(statReporter, jobID);
      overallTimer.enableRealTimeStats(statReporter, jobID);
      overallExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      addsCompleted.enableRealTimeStats(statReporter, jobID);
      addTimer.enableRealTimeStats(statReporter, jobID);
      addsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      bindsCompleted.enableRealTimeStats(statReporter, jobID);
      bindTimer.enableRealTimeStats(statReporter, jobID);
      bindsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      comparesCompleted.enableRealTimeStats(statReporter, jobID);
      compareTimer.enableRealTimeStats(statReporter, jobID);
      comparesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      deletesCompleted.enableRealTimeStats(statReporter, jobID);
      deleteTimer.enableRealTimeStats(statReporter, jobID);
      deletesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      modifiesCompleted.enableRealTimeStats(statReporter, jobID);
      modifyTimer.enableRealTimeStats(statReporter, jobID);
      modifiesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      modifyDNsCompleted.enableRealTimeStats(statReporter, jobID);
      modifyDNTimer.enableRealTimeStats(statReporter, jobID);
      modifyDNsExceedingThreshold.enableRealTimeStats(statReporter, jobID);
      searchesCompleted.enableRealTimeStats(statReporter, jobID);
      searchTimer.enableRealTimeStats(statReporter, jobID);
      searchesExceedingThreshold.enableRealTimeStats(statReporter, jobID);
    }


    random = new Random(parentRandom.nextLong());

    addCounter = 0;
    addList = new LinkedList<String>();

    Filter filter = Filter.createPresenceFilter("objectClass");
    searchRequest = new SearchRequest(baseDN, scope, filter, attributes);

    try
    {
      opConn = createConnection();

      bindConn = createConnection(opConn.getConnectedAddress(),
                                  opConn.getConnectedPort());
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
    // Delete any entries that may have been added but weren't removed.
    if (cleanUp)
    {
      for (String dn : addList)
      {
        try
        {
          opConn.delete(dn);
        } catch (Exception e) {}
      }
    }

    if (opConn != null)
    {
      opConn.close();
      opConn = null;
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


    // Perform operations until it's time to stop.
    long opsCompleted = 0L;
    boolean doneCollecting = false;
    while ((! shouldStop()) && (opsCompleted < maxOpsPerThread))
    {
      if ((opConn == null) || (bindConn == null) ||
          ((opsCompleted % opsBeforeReconnect) == 0L))
      {
        if (opsCompleted > 0)
        {
          if (opConn != null)
          {
            opConn.close();
            opConn = null;
          }

          if (bindConn != null)
          {
            bindConn.close();
            bindConn = null;
          }

          LDAPConnection opConnection   = null;
          try
          {
            opConnection = createConnection();
          }
          catch (LDAPException le)
          {
            try
            {
              Thread.sleep(1L);
            } catch (Exception e) {}
            continue;
          }

          LDAPConnection bindConnection = null;
          try
          {
            bindConnection = createConnection(
                 opConnection.getConnectedAddress(),
                 opConnection.getConnectedPort());
          }
          catch (LDAPException le)
          {
            opConnection.close();

            try
            {
              Thread.sleep(1L);
            } catch (Exception e) {}
            continue;
          }

          opConn   = opConnection;
          bindConn = bindConnection;
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
      long opStartTime = System.currentTimeMillis();
      if (collectingStats && (coolDownTime > 0) &&
          (opStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (opStartTime >= startCollectingTime))
      {
        collectingStats = true;
        startTrackers();
      }


      // Figure out which type of operation to perform.
      int opType = random.nextInt(totalWeight);
      if (opType < addWeightThreshold)
      {
        doAdd();
      }
      else if (opType < bindWeightThreshold)
      {
        doBind();
      }
      else if (opType < compareWeightThreshold)
      {
        doCompare();
      }
      else if (opType < deleteWeightThreshold)
      {
        doDelete();
      }
      else if (opType < modifyWeightThreshold)
      {
        doModify();
      }
      else if (opType < modifyDNWeightThreshold)
      {
        doModifyDN();
      }
      else
      {
        doSearch();
      }


      // Sleep if necessary before the next request.
      if (timeBetweenRequests > 0L)
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

      opsCompleted++;
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
    overallCompleted.startTracker();
    overallTimer.startTracker();
    overallResultCodes.startTracker();
    overallExceedingThreshold.startTracker();
    addsCompleted.startTracker();
    addTimer.startTracker();
    addResultCodes.startTracker();
    addsExceedingThreshold.startTracker();
    bindsCompleted.startTracker();
    bindTimer.startTracker();
    bindResultCodes.startTracker();
    bindsExceedingThreshold.startTracker();
    comparesCompleted.startTracker();
    compareTimer.startTracker();
    compareResultCodes.startTracker();
    comparesExceedingThreshold.startTracker();
    deletesCompleted.startTracker();
    deleteTimer.startTracker();
    deleteResultCodes.startTracker();
    deletesExceedingThreshold.startTracker();
    modifiesCompleted.startTracker();
    modifyTimer.startTracker();
    modifyResultCodes.startTracker();
    modifiesExceedingThreshold.startTracker();
    modifyDNsCompleted.startTracker();
    modifyDNTimer.startTracker();
    modifyDNResultCodes.startTracker();
    modifyDNsExceedingThreshold.startTracker();
    searchesCompleted.startTracker();
    searchTimer.startTracker();
    searchResultCodes.startTracker();
    searchesExceedingThreshold.startTracker();
    searchEntriesReturned.startTracker();
    operationTypes.startTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    overallCompleted.stopTracker();
    overallTimer.stopTracker();
    overallResultCodes.stopTracker();
    overallExceedingThreshold.stopTracker();
    addsCompleted.stopTracker();
    addTimer.stopTracker();
    addResultCodes.stopTracker();
    addsExceedingThreshold.stopTracker();
    bindsCompleted.stopTracker();
    bindTimer.stopTracker();
    bindResultCodes.stopTracker();
    bindsExceedingThreshold.stopTracker();
    comparesCompleted.stopTracker();
    compareTimer.stopTracker();
    compareResultCodes.stopTracker();
    comparesExceedingThreshold.stopTracker();
    deletesCompleted.stopTracker();
    deleteTimer.stopTracker();
    deleteResultCodes.stopTracker();
    deletesExceedingThreshold.stopTracker();
    modifiesCompleted.stopTracker();
    modifyTimer.stopTracker();
    modifyResultCodes.stopTracker();
    modifiesExceedingThreshold.stopTracker();
    modifyDNsCompleted.stopTracker();
    modifyDNTimer.stopTracker();
    modifyDNResultCodes.stopTracker();
    modifyDNsExceedingThreshold.stopTracker();
    searchesCompleted.stopTracker();
    searchTimer.stopTracker();
    searchResultCodes.stopTracker();
    searchesExceedingThreshold.stopTracker();
    searchEntriesReturned.stopTracker();
    operationTypes.stopTracker();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void destroyThread()
  {
    if (opConn != null)
    {
      opConn.close();
      opConn = null;
    }

    if (bindConn != null)
    {
      bindConn.close();
      bindConn = null;
    }
  }



  /**
   * Performs the processing necessary for an add operation.
   */
  private void doAdd()
  {
    if (collectingStats)
    {
      operationTypes.increment("Add");
    }

    // Construct the entry to add.
    int entryNumber = addCounter++;
    String entryDN = rdnAttribute + '=' + getThreadID() + '-' + entryNumber +
                     ',' + baseDN;

    Entry entry;
    try
    {
      entry = entryGenerator.createEntry(random, entryNumber, entryDN);
    }
    catch (Exception e)
    {
      if (collectingStats)
      {
        ResultCode rc = ResultCode.PARAM_ERROR;

        overallResultCodes.increment(rc.toString());
        addResultCodes.increment(rc.toString());
      }

      return;
    }


    // Add the entry to the server.
    if (collectingStats)
    {
      overallTimer.startTimer();
      addTimer.startTimer();
    }

    try
    {
      LDAPResult addResult = opConn.add(entry);
      if (collectingStats)
      {
        overallResultCodes.increment(addResult.getResultCode().toString());
        addResultCodes.increment(addResult.getResultCode().toString());
      }
      addList.add(entryDN);
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        addResultCodes.increment(le.getResultCode().toString());
      }

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        addTimer.stopTimer();
        overallCompleted.increment();
        addsCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = addTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            addsExceedingThreshold.increment();
          }
        }
      }
    }
  }



  /**
   * Performs the processing necessary for a bind operation.
   */
  private void doBind()
  {
    if (collectingStats)
    {
      operationTypes.increment("Bind");
    }

    // Choose a DN to use for the bind.
    String bindDN;
    if (random.nextInt(100) < dn1Percentage)
    {
      bindDN = dn1Pattern.nextValue();
    }
    else
    {
      bindDN = dn2Pattern.nextValue();
    }


    // Try to bind to the server.
    if (collectingStats)
    {
      overallTimer.startTimer();
      bindTimer.startTimer();
    }

    try
    {
      LDAPResult bindResult = bindConn.bind(bindDN, userPassword);
      if (collectingStats)
      {
        overallResultCodes.increment(bindResult.getResultCode().toString());
        bindResultCodes.increment(bindResult.getResultCode().toString());
      }
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        bindResultCodes.increment(le.getResultCode().toString());
      }

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        bindTimer.stopTimer();
        overallCompleted.increment();
        bindsCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = bindTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            bindsExceedingThreshold.increment();
          }
        }
      }
    }
  }



  /**
   * Performs the processing necessary for a compare operation.
   */
  private void doCompare()
  {
    if (collectingStats)
    {
      operationTypes.increment("Compare");
    }

    // Choose a DN to use for the compare.
    String targetDN;
    if (random.nextInt(100) < dn1Percentage)
    {
      targetDN = dn1Pattern.nextValue();
    }
    else
    {
      targetDN = dn2Pattern.nextValue();
    }


    // Generate an assertion value.
    StringBuilder assertionValue = new StringBuilder(valueLength);
    for (int i=0; i < valueLength; i++)
    {
      assertionValue.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }


    // Try to perform the compare operation.
    if (collectingStats)
    {
      overallTimer.startTimer();
      compareTimer.startTimer();
    }

    try
    {
      LDAPResult compareResult =
           opConn.compare(targetDN, attributeName, assertionValue.toString());
      if (collectingStats)
      {
        overallResultCodes.increment(compareResult.getResultCode().toString());
        compareResultCodes.increment(compareResult.getResultCode().toString());
      }
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        compareResultCodes.increment(le.getResultCode().toString());
      }

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        compareTimer.stopTimer();
        overallCompleted.increment();
        comparesCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = compareTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            comparesExceedingThreshold.increment();
          }
        }
      }
    }
  }



  /**
   * Performs the processing necessary for a delete operation.
   */
  private void doDelete()
  {
    if (collectingStats)
    {
      operationTypes.increment("Delete");
    }

    // If the add list is empty, then perform an add instead of a delete.
    if (addList.isEmpty())
    {
      doAdd();
      return;
    }

    String entryDN = addList.removeFirst();

    if (collectingStats)
    {
      overallTimer.startTimer();
      deleteTimer.startTimer();
    }

    try
    {
      LDAPResult deleteResult = opConn.delete(entryDN);
      if (collectingStats)
      {
        overallResultCodes.increment(deleteResult.getResultCode().toString());
        deleteResultCodes.increment(deleteResult.getResultCode().toString());
      }
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        deleteResultCodes.increment(le.getResultCode().toString());
      }

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        deleteTimer.stopTimer();
        overallCompleted.increment();
        deletesCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = deleteTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            deletesExceedingThreshold.increment();
          }
        }
      }
    }
  }



  /**
   * Performs the processing necessary for a modify operation.
   */
  private void doModify()
  {
    if (collectingStats)
    {
      operationTypes.increment("Modify");
    }

    // Choose a DN to target for the modify.
    String targetDN;
    if (random.nextInt(100) < dn1Percentage)
    {
      targetDN = dn1Pattern.nextValue();
    }
    else
    {
      targetDN = dn2Pattern.nextValue();
    }


    // Generate the modification.
    StringBuilder newValue = new StringBuilder(valueLength);
    for (int i=0; i < valueLength; i++)
    {
      newValue.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }

    Modification mod = new Modification(ModificationType.REPLACE, attributeName,
                                        newValue.toString());

    // Try to perform the modify operation.
    if (collectingStats)
    {
      overallTimer.startTimer();
      modifyTimer.startTimer();
    }

    try
    {
      LDAPResult modifyResult = opConn.modify(targetDN, mod);
      if (collectingStats)
      {
        overallResultCodes.increment(modifyResult.getResultCode().toString());
        modifyResultCodes.increment(modifyResult.getResultCode().toString());
      }
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        modifyResultCodes.increment(le.getResultCode().toString());
      }

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        modifyTimer.stopTimer();
        overallCompleted.increment();
        modifiesCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = modifyTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            modifiesExceedingThreshold.increment();
          }
        }
      }
    }
  }



  /**
   * Performs the processing necessary for a modify DN operation.
   */
  private void doModifyDN()
  {
    if (collectingStats)
    {
      operationTypes.increment("Modify DN");
    }

    // If the add list is empty, then perform an add instead of a modify DN.
    if (addList.isEmpty())
    {
      doAdd();
      return;
    }

    int entryNumber = addCounter++;
    String newRDN = rdnAttribute + '=' + getThreadID() + '-' + entryNumber;

    String entryDN = addList.removeFirst();

    if (collectingStats)
    {
      overallTimer.startTimer();
      modifyDNTimer.startTimer();
    }

    try
    {
      LDAPResult modifyDNResult = opConn.modifyDN(entryDN, newRDN, true);
      addList.add(newRDN + ',' + baseDN);

      if (collectingStats)
      {
        overallResultCodes.increment(modifyDNResult.getResultCode().toString());
        modifyDNResultCodes.increment(modifyDNResult.getResultCode().toString());
      }
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        modifyDNResultCodes.increment(le.getResultCode().toString());
      }
      addList.add(entryDN);

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        modifyDNTimer.stopTimer();
        overallCompleted.increment();
        modifyDNsCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = modifyDNTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            modifyDNsExceedingThreshold.increment();
          }
        }
      }
    }
  }



  /**
   * Performs the processing necessary for a search operation.
   */
  private void doSearch()
  {
    if (collectingStats)
    {
      operationTypes.increment("Search");
    }

    // Choose a filter for the search.
    String filter;
    if (random.nextInt(100) < filter1Percentage)
    {
      filter = filter1Pattern.nextValue();
    }
    else
    {
      filter = filter2Pattern.nextValue();
    }

    try
    {
      searchRequest.setFilter(filter);
    }
    catch (LDAPException le)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(le.getResultCode().toString());
        searchResultCodes.increment(le.getResultCode().toString());
      }
      return;
    }


    // Try to perform the search operation.
    if (collectingStats)
    {
      overallTimer.startTimer();
      searchTimer.startTimer();
    }

    try
    {
      SearchResult searchResult = opConn.search(searchRequest);
      if (collectingStats)
      {
        overallResultCodes.increment(searchResult.getResultCode().toString());
        searchResultCodes.increment(searchResult.getResultCode().toString());
        searchEntriesReturned.addValue(searchResult.getEntryCount());
      }
    }
    catch (LDAPSearchException lse)
    {
      if (collectingStats)
      {
        overallResultCodes.increment(lse.getResultCode().toString());
        searchResultCodes.increment(lse.getResultCode().toString());
        searchEntriesReturned.addValue(lse.getEntryCount());
      }

      opConn.close();
      opConn = null;

      bindConn.close();
      bindConn = null;
    }
    finally
    {
      if (collectingStats)
      {
        overallTimer.stopTimer();
        searchTimer.stopTimer();
        overallCompleted.increment();
        searchesCompleted.increment();

        if (responseTimeThreshold > 0)
        {
          long opTime = searchTimer.getLastOperationTime();
          if (opTime > responseTimeThreshold)
          {
            overallExceedingThreshold.increment();
            searchesExceedingThreshold.increment();
          }
        }
      }
    }
  }
}
