/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
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



/**
 * This class implements a SLAMD job class that has the ability to generate
 * various kinds of load against an LDAP directory server.  It can perform
 * search, compare, add, delete, modify, and modify RDN operations.  The
 * relative frequencies of each kind of operation may be specified by the user
 * scheduling the job for execution.
 *
 *
 * @author   Neil A. Wilson
 */
public class MultiSearchLDAPLoadJobClass
       extends JobClass
{
  /**
   * The set of characters that will make up randomly-generated strings.
   */
  public static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  /**
   * The name of the stat tracker that counts the number of attempted adds.
   */
  public static final String STAT_TRACKER_ADD_ATTEMPTS = "Add Attempts";



  /**
   * The name of the stat tracker that times add operations.
   */
  public static final String STAT_TRACKER_ADD_TIME = "Add Time (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted compares.
   */
  public static final String STAT_TRACKER_COMPARE_ATTEMPTS = "Compare Attempts";



  /**
   * The name of the stat tracker that times compare operations.
   */
  public static final String STAT_TRACKER_COMPARE_TIME = "Compare Time (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted deletes.
   */
  public static final String STAT_TRACKER_DELETE_ATTEMPTS = "Delete Attempts";



  /**
   * The name of the stat tracker that times delete operations.
   */
  public static final String STAT_TRACKER_DELETE_TIME = "Delete Time (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted modifies.
   */
  public static final String STAT_TRACKER_MODIFY_ATTEMPTS = "Modify Attempts";



  /**
   * The name of the stat tracker that times modify operations.
   */
  public static final String STAT_TRACKER_MODIFY_TIME = "Modify Time (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted modify RDN
   * operations.
   */
  public static final String STAT_TRACKER_MODIFY_RDN_ATTEMPTS =
       "Modify RDN Attempts";



  /**
   * The name of the stat tracker that times modify RDN operations.
   */
  public static final String STAT_TRACKER_MODIFY_RDN_TIME =
       "Modify RDN Time (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted
   * operations.
   */
  public static final String STAT_TRACKER_OPERATION_ATTEMPTS =
       "Overall Operations Attempted";



  /**
   * The name of the stat tracker that categorizes the attempted operations.
   */
  public static final String STAT_TRACKER_OPERATION_ATTEMPTS_BY_CATEGORY =
       "Types of Operations Attempted";



  /**
   * The name of the stat tracker that times attempted operations.
   */
  public static final String STAT_TRACKER_OPERATION_TIME =
       "Overall Operation Time";



  /**
   * The name of the stat tracker that categorizes the result codes received
   * from the operations.
   */
  public static final String STAT_TRACKER_RESULT_CODES = "Result Codes";



  /**
   * The name of the stat tracker that counts the number of attempted searches
   * from the first filter file.
   */
  public static final String STAT_TRACKER_SEARCH_ATTEMPTS_1 =
       "Search Attempts 1";



  /**
   * The name of the stat tracker that times search operations from the first
   * filter file.
   */
  public static final String STAT_TRACKER_SEARCH_TIME_1 = "Search Time 1 (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted searches
   * from the first filter file.
   */
  public static final String STAT_TRACKER_SEARCH_ATTEMPTS_2 =
       "Search Attempts 2";



  /**
   * The name of the stat tracker that times search operations from the first
   * filter file.
   */
  public static final String STAT_TRACKER_SEARCH_TIME_2 = "Search Time 2 (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted searches
   * from the first filter file.
   */
  public static final String STAT_TRACKER_SEARCH_ATTEMPTS_3 =
       "Search Attempts 3";



  /**
   * The name of the stat tracker that times search operations from the first
   * filter file.
   */
  public static final String STAT_TRACKER_SEARCH_TIME_3 = "Search Time 3 (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted searches
   * from the first filter file.
   */
  public static final String STAT_TRACKER_SEARCH_ATTEMPTS_4 =
       "Search Attempts 4";



  /**
   * The name of the stat tracker that times search operations from the first
   * filter file.
   */
  public static final String STAT_TRACKER_SEARCH_TIME_4 = "Search Time 4 (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted searches
   * from the first filter file.
   */
  public static final String STAT_TRACKER_SEARCH_ATTEMPTS_5 =
       "Search Attempts 5";



  /**
   * The name of the stat tracker that times search operations from the first
   * filter file.
   */
  public static final String STAT_TRACKER_SEARCH_TIME_5 = "Search Time 5 (ms)";



  /**
   * The name of the stat tracker that counts the number of attempted searches
   * from the first filter file.
   */
  public static final String STAT_TRACKER_SEARCH_ATTEMPTS_6 =
       "Search Attempts 6";



  /**
   * The name of the stat tracker that times search operations from the first
   * filter file.
   */
  public static final String STAT_TRACKER_SEARCH_TIME_6 = "Search Time 6 (ms)";



  // The parameter that indicates whether the client should trust any SSL cert.
  private BooleanParameter blindTrustParameter =
    new BooleanParameter("blind_trust", "Blindly Trust Any Certificate",
                         "Indicates whether the client should blindly trust " +
                         "any certificate presented by the server, or " +
                         "whether the key and trust stores should be used.",
                         true);

  // The parameter that indicates whether the job should clean up any entries
  // that may have been added during processing.
  private BooleanParameter cleanUpParameter =
       new BooleanParameter("cleanup", "Clean Up When Done",
                            "Indicates whether each client should clean up " +
                            "any entries that may have been added during " +
                            "processing that have not yet been removed.", true);

  // The parameter that indicates whether to disconnect after each operation.
  private BooleanParameter disconnectParameter =
       new BooleanParameter("disconnect", "Disconnect when Rebinding",
                            "Indicates whether to close and re-establish the " +
                            "connection to the directory server whenever a " +
                            "rebind occurs.", false);

  // The parameter that indicates whether to follow referrals.
  private BooleanParameter referralsParameter =
       new BooleanParameter("follow_referrals", "Follow Referrals",
                            "Indicates whether to follow referrals " +
                            "encountered while performing operations in the " +
                            "directory.", false);

  // The parameter that indicates whether to use SSL when communicating with the
  // directory server.
  private BooleanParameter useSSLParameter =
       new BooleanParameter("use_ssl", "Use SSL",
                            "Indicates whether to use SSL when communicating " +
                            "with the directory server.", false);

  // The parameter that specifies the URL of the search filter file.
  private FileURLParameter filterFile1URLParameter =
       new FileURLParameter("filter_file_1", "Search Filter File URL 1",
                            "Specifies the URL (FILE or HTTP) to the file " +
                            "that contains a set of filters that may be used " +
                            "when performing the the first kind of searches.",
                            null, false);

  // The parameter that specifies the URL of the search filter file.
  private FileURLParameter filterFile2URLParameter =
       new FileURLParameter("filter_file_2", "Search Filter File URL 2",
                            "Specifies the URL (FILE or HTTP) to the file " +
                            "that contains a set of filters that may be used " +
                            "when performing the the second kind of searches.",
                            null, false);

  // The parameter that specifies the URL of the search filter file.
  private FileURLParameter filterFile3URLParameter =
       new FileURLParameter("filter_file_3", "Search Filter File URL 3",
                            "Specifies the URL (FILE or HTTP) to the file " +
                            "that contains a set of filters that may be used " +
                            "when performing the the third kind of searches.",
                            null, false);

  // The parameter that specifies the URL of the search filter file.
  private FileURLParameter filterFile4URLParameter =
       new FileURLParameter("filter_file_4", "Search Filter File URL 4",
                            "Specifies the URL (FILE or HTTP) to the file " +
                            "that contains a set of filters that may be used " +
                            "when performing the the fourth kind of searches.",
                            null, false);

  // The parameter that specifies the URL of the search filter file.
  private FileURLParameter filterFile5URLParameter =
       new FileURLParameter("filter_file_5", "Search Filter File URL 5",
                            "Specifies the URL (FILE or HTTP) to the file " +
                            "that contains a set of filters that may be used " +
                            "when performing the the fifth kind of searches.",
                            null, false);

  // The parameter that specifies the URL of the search filter file.
  private FileURLParameter filterFile6URLParameter =
       new FileURLParameter("filter_file_6", "Search Filter File URL 6",
                            "Specifies the URL (FILE or HTTP) to the file " +
                            "that contains a set of filters that may be used " +
                            "when performing the the fifth kind of searches.",
                            null, false);

  // The parameter that specifies the frequency for add operations.
  private IntegerParameter addFrequencyParameter =
    new IntegerParameter("add_frequency", "Add Operation Frequency",
                         "Specifies the frequency with which adds should be " +
                         "performed relative to the other types of operations.",
                         true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for compare operations.
  private IntegerParameter compareFrequencyParameter =
       new IntegerParameter("compare_frequency", "Compare Operation Frequency",
                            "Specifies the frequency with which compares " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the cool-down time in seconds.
  private IntegerParameter coolDownParameter =
       new IntegerParameter("cool_down", "Cool Down Time",
                            "The time in seconds that the job should " +
                            "continue running after ending statistics " +
                            "collection.", true, 0, true, 0, false, 0);

  // The parameter that specifies the delay between requests.
  private IntegerParameter delayParameter =
       new IntegerParameter("request_delay", "Time Between Requests (ms)",
                            "Specifies the length of time in milliseconds " +
                            "that will pass between requests.  Note that " +
                            "is the time between requests and not the time " +
                            "between the end of one operation and the " +
                            "beginning of the next.  If any operation takes " +
                            "longer than this length of time, then there " +
                            "will be no delay before the start of the next " +
                            "operation.", false, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for delete operations.
  private IntegerParameter deleteFrequencyParameter =
       new IntegerParameter("delete_frequency", "Delete Operation Frequency",
                            "Specifies the frequency with which deletes " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for modify operations.
  private IntegerParameter modifyFrequencyParameter =
       new IntegerParameter("modify_frequency", "Modify Operation Frequency",
                            "Specifies the frequency with which modifies " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the maximum request rate.
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Operation Rate (Ops/Second/Client)",
       "Specifies the maximum operation rate (in ops per second per client) " +
            "to attempt to maintain.  If multiple clients are used, then " +
            "each client will attempt to maintain this rate.  A value less " +
            "than or equal to zero indicates that the client should attempt " +
            "to perform operations as quickly as possible.",
       true, -1);

  // The parameter that specifies the frequency for modify RDN operations.
  private IntegerParameter modifyRDNFrequencyParameter =
       new IntegerParameter("modify_rdn_frequency",
                            "Modify RDN Operation Frequency",
                            "Specifies the frequency with which modify RDNs " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the number of operations between binds.
  private IntegerParameter opsBetweenBindsParameter =
       new IntegerParameter("ops_between_binds", "Operations Between Binds",
                            "Specifies the number of operations to perform " +
                            "before re-binding as another user.", false, 0,
                            true, 0, false, 0);

  // The parameter that specifies the port number for the directory server.
  private IntegerParameter portParameter =
       new IntegerParameter("ldap_port", "Directory Server Port",
                            "Specifies the port number for the directory " +
                            "server.", true, 389, true, 1, true, 65535);

  // The parameter that specifies the interval over which to enforce the maximum
  // request rate.
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

  // The parameter that specifies the frequency for search operations.
  private IntegerParameter searchFrequency1Parameter =
       new IntegerParameter("search_frequency_1", "Search 1 Frequency",
                            "Specifies the frequency with which search 1 " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for search operations.
  private IntegerParameter searchFrequency2Parameter =
       new IntegerParameter("search_frequency_2", "Search 2 Frequency",
                            "Specifies the frequency with which search 2 " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for search operations.
  private IntegerParameter searchFrequency3Parameter =
       new IntegerParameter("search_frequency_3", "Search 3 Frequency",
                            "Specifies the frequency with which search 3 " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for search operations.
  private IntegerParameter searchFrequency4Parameter =
       new IntegerParameter("search_frequency_4", "Search 4 Frequency",
                            "Specifies the frequency with which search 4 " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for search operations.
  private IntegerParameter searchFrequency5Parameter =
       new IntegerParameter("search_frequency_5", "Search 5 Frequency",
                            "Specifies the frequency with which search 5 " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the frequency for search operations.
  private IntegerParameter searchFrequency6Parameter =
       new IntegerParameter("search_frequency_6", "Search 6 Frequency",
                            "Specifies the frequency with which search 6 " +
                            "should be performed relative to the other types " +
                            "of operations.", true, 0, true, 0, false, 0);

  // The parameter that specifies the maximum number of entries that should be
  // returned from a single search operation.
  private IntegerParameter sizeLimitParameter =
       new IntegerParameter("size_limit", "Search Size Limit",
                            "Specifies the maximum number of entries that " +
                            "should be returned from a single search " +
                            "operation.  A size limit of zero indicates that " +
                            "there is no limit.", false, 0, true, 0, false, 0);

  // The parameter that specifies the maximum length of time in seconds that any
  // operation will be allowed to take before being abandoned.
  private IntegerParameter timeLimitParameter =
       new IntegerParameter("time_limit", "Operation Time Limit",
                            "Specifies the maximum length of time in seconds " +
                            "will be allowed for any single operation.  If " +
                            "operation takes longer than this length of time " +
                            "it will be abandoned.  A time limit of zero " +
                            "indicates that there is no time limit.", false, 0,
                            true, 0, false, 0);

  // The parameter that specifies the warm-up time in seconds.
  private IntegerParameter warmUpParameter =
       new IntegerParameter("warm_up", "Warm Up Time",
                            "The time in seconds that the job should run " +
                            "before beginning statistics collection.",
                            true, 0, true, 0, false, 0);

  // The parameter that specifies the password to use when binding to the
  // directory server.
  private PasswordParameter bindPasswordParameter =
       new PasswordParameter("bind_pw", "Bind Password",
                             "Specifies the password to use when binding to " +
                             "the directory server.  If no password is " +
                             "specified, then the bind will be performed " +
                             "anonymously.", false, "");

  // The parameter that specifies the password to access the SSL key store.
  private PasswordParameter sslKeyPWParameter =
       new PasswordParameter("ssl_key_pw", "SSL Key Store Password",
                             "Specifies the password to use when accessing " +
                             "the JSSE key store.  If SSL is not used, then " +
                             "this does not need to be specified.", false, "");

  // The parameter that specifies the password to access the SSL trust store.
  private PasswordParameter sslTrustPWParameter =
       new PasswordParameter("ssl_trust_pw", "SSL Trust Store Password",
                             "Specifies the password to use when accessing " +
                             "the JSSE trust store.  If SSL is not used, " +
                             "then this does not need to be specified.", false,
                             "");

  // A placeholder parameter used to visually group related parameters.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter that specifies the address of the directory server.
  private StringParameter addressParameter =
       new StringParameter("ldap_host", "Directory Server Address",
                           "Specifies the address for the directory server.",
                           true, "");

  // The parameter that specifies the attribute to target for modify and compare
  // operations.
  private StringParameter attrParameter =
       new StringParameter("attr", "Attribute to Compare/Modify",
                           "Specifies the LDAP attribute at which modify and " +
                           "compare operations will be targeted.", true,
                           "description");

  // The parameter that specifies the base DN for operations in the directory.
  private StringParameter baseDNParameter =
       new StringParameter("base_dn", "Directory Base DN",
                           "Specifies the base DN under which all operations " +
                           "will be performed in the directory.", true, "");

  // The parameter that specifies the DN to use when binding to the directory.
  private StringParameter bindDNParameter =
       new StringParameter("bind_dn", "Bind DN",
                           "Specifies the DN to use when binding to the " +
                           "directory server for all operations.  If no bind " +
                           "DN is specified, then the bind will be performed " +
                           "anonymously.", true, "");

  // The parameter that specifies the location of the JSSE key store.
  private StringParameter sslKeyStoreParameter =
       new StringParameter("ssl_key_store", "SSL Key Store",
                           "Specifies the location of the JSSE key store to " +
                           "use with SSL.  If SSL is not used, then this " +
                           "value does not need to be specified.", false, "");

  // The parameter that specifies the location of the JSSE trust store.
  private StringParameter sslTrustStoreParameter =
       new StringParameter("ssl_trust_store", "SSL Trust Store",
                           "Specifies the location of the JSSE trust store " +
                           "to use with SSL.  If SSL is not used, then this " +
                           "value does not need to be specified.", false, "");

  // Static variables used to hold the values of the parameters in each client
  // (or variables related to the values of those parameters).
  private static boolean      alwaysDisconnect;
  private static boolean      cleanUp;
  private static boolean      followReferrals;
  private static boolean      useSSL;
  private static int          addFrequency;
  private static int          compareFrequency;
  private static int          coolDownTime;
  private static int          deleteFrequency;
  private static int          ldapPort;
  private static int          modifyFrequency;
  private static int          modifyRDNFrequency;
  private static int          operationDelay;
  private static int          opsBetweenBinds;
  private static int          searchFrequency1;
  private static int          searchFrequency2;
  private static int          searchFrequency3;
  private static int          searchFrequency4;
  private static int          searchFrequency5;
  private static int          searchFrequency6;
  private static int          sizeLimit;
  private static int          timeLimit;
  private static int          totalFrequency;
  private static int          warmUpTime;
  private static int[]        opWeights;
  private static Random       parentRandom;
  private static String       baseDN;
  private static String       bindPW;
  private static String       ldapHost;
  private static String       modAttr;
  private static String[]     searchFilters1;
  private static String[]     searchFilters2;
  private static String[]     searchFilters3;
  private static String[]     searchFilters4;
  private static String[]     searchFilters5;
  private static String[]     searchFilters6;
  private static ValuePattern bindDNPattern;

  // Static variables used to keep track of the DNs of all entries added to the
  // directory.  This list will be used for the entries to delete and to rename.
  private static int                dnsToDelete  = 0;
  private static LinkedList<String> addedDNs     = new LinkedList<String>();
  private static final Object       addedDNMutex = new Object();

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // Instance variables used as the stat trackers.
  private CategoricalTracker operationTypes;
  private CategoricalTracker resultCodes;
  private IncrementalTracker addCount;
  private IncrementalTracker compareCount;
  private IncrementalTracker deleteCount;
  private IncrementalTracker modifyCount;
  private IncrementalTracker modifyRDNCount;
  private IncrementalTracker operationCount;
  private IncrementalTracker searchCount1;
  private IncrementalTracker searchCount2;
  private IncrementalTracker searchCount3;
  private IncrementalTracker searchCount4;
  private IncrementalTracker searchCount5;
  private IncrementalTracker searchCount6;
  private TimeTracker        addTimer;
  private TimeTracker        compareTimer;
  private TimeTracker        deleteTimer;
  private TimeTracker        modifyTimer;
  private TimeTracker        modifyRDNTimer;
  private TimeTracker        operationTimer;
  private TimeTracker        searchTimer1;
  private TimeTracker        searchTimer2;
  private TimeTracker        searchTimer3;
  private TimeTracker        searchTimer4;
  private TimeTracker        searchTimer5;
  private TimeTracker        searchTimer6;

  // Other instance variables used in this thread.
  private boolean               collectingStats;
  private LDAPConnection        conn;
  private Random                random;
  private String                currentBindDN;




  /**
   * Creates a new instance of this job thread.  This constructor
   * does not need to do anything other than invoke the constructor
   * for the superclass.
    relative*/
  public MultiSearchLDAPLoadJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Load Generator (with multiple searches)";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Generate various kinds of load against an LDAP directory server";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to generate various kinds of load against an " +
      "LDAP directory server.  It provides the ability to perform multiple " +
      "kinds of searches using filters read from different files.  It may " +
      "also be used to perform add, compare, delete, modify, and modify DN " +
      "operations."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "LDAP";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    Parameter[] params = new Parameter[]
    {
      placeholder,
      addressParameter,
      portParameter,
      baseDNParameter,
      bindDNParameter,
      bindPasswordParameter,
      placeholder,
      addFrequencyParameter,
      compareFrequencyParameter,
      deleteFrequencyParameter,
      modifyFrequencyParameter,
      modifyRDNFrequencyParameter,
      searchFrequency1Parameter,
      searchFrequency2Parameter,
      searchFrequency3Parameter,
      searchFrequency4Parameter,
      searchFrequency5Parameter,
      searchFrequency6Parameter,
      placeholder,
      filterFile1URLParameter,
      filterFile2URLParameter,
      filterFile3URLParameter,
      filterFile4URLParameter,
      filterFile5URLParameter,
      filterFile6URLParameter,
      placeholder,
      attrParameter,
      placeholder,
      sizeLimitParameter,
      timeLimitParameter,
      warmUpParameter,
      coolDownParameter,
      delayParameter,
      maxRateParameter,
      rateLimitDurationParameter,
      opsBetweenBindsParameter,
      placeholder,
      useSSLParameter,
      blindTrustParameter,
      sslKeyStoreParameter,
      sslKeyPWParameter,
      sslTrustStoreParameter,
      sslTrustPWParameter,
      placeholder,
      cleanUpParameter,
      disconnectParameter,
      referralsParameter
    };

    return new ParameterList(params);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[]
    {
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_ADD_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_ADD_TIME,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_COMPARE_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_COMPARE_TIME,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_DELETE_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_DELETE_TIME,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_MODIFY_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_MODIFY_TIME,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_MODIFY_RDN_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_MODIFY_RDN_TIME,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCH_ATTEMPTS_1,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME_1,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCH_ATTEMPTS_2,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME_2,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCH_ATTEMPTS_3,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME_3,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCH_ATTEMPTS_4,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME_4,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCH_ATTEMPTS_5,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME_5,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCH_ATTEMPTS_6,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME_6,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_OPERATION_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_OPERATION_TIME,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID,
                             STAT_TRACKER_OPERATION_ATTEMPTS_BY_CATEGORY,
                             collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_TRACKER_RESULT_CODES,
                             collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();

    if (addCount.getTotalCount() > 0)
    {
      trackerList.add(addCount);
      trackerList.add(addTimer);
    }

    if (compareCount.getTotalCount() > 0)
    {
      trackerList.add(compareCount);
      trackerList.add(compareTimer);
    }

    if (deleteCount.getTotalCount() > 0)
    {
      trackerList.add(deleteCount);
      trackerList.add(deleteTimer);
    }

    if (modifyCount.getTotalCount() > 0)
    {
      trackerList.add(modifyCount);
      trackerList.add(modifyTimer);
    }

    if (modifyRDNCount.getTotalCount() > 0)
    {
      trackerList.add(modifyRDNCount);
      trackerList.add(modifyRDNTimer);
    }

    if (searchCount1.getTotalCount() > 0)
    {
      trackerList.add(searchCount1);
      trackerList.add(searchTimer1);
    }

    if (searchCount2.getTotalCount() > 0)
    {
      trackerList.add(searchCount2);
      trackerList.add(searchTimer2);
    }

    if (searchCount3.getTotalCount() > 0)
    {
      trackerList.add(searchCount3);
      trackerList.add(searchTimer3);
    }

    if (searchCount4.getTotalCount() > 0)
    {
      trackerList.add(searchCount4);
      trackerList.add(searchTimer4);
    }

    if (searchCount5.getTotalCount() > 0)
    {
      trackerList.add(searchCount5);
      trackerList.add(searchTimer5);
    }

    if (searchCount6.getTotalCount() > 0)
    {
      trackerList.add(searchCount6);
      trackerList.add(searchTimer6);
    }

    // These will always be added, no matter what.
    trackerList.add(operationCount);
    trackerList.add(operationTimer);
    trackerList.add(operationTypes);
    trackerList.add(resultCodes);

    StatTracker[] trackerArray = new StatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void validateJobInfo(int numClients, int threadsPerClient,
                              int threadStartupDelay, Date startTime,
                              Date stopTime, int duration,
                              int collectionInterval, ParameterList parameters)
         throws InvalidValueException
  {
    // The bind DN pattern must be a valid value pattern.
    StringParameter p =
         parameters.getStringParameter(bindDNParameter.getName());
    if ((p != null) && p.hasValue())
    {
      try
      {
        new ValuePattern(p.getValue());
      }
      catch (ParseException pe)
      {
        throw new InvalidValueException("The value for the '" +
             p.getDisplayName() + "' parameter is not a valid value " +
             "pattern:  " + pe.getMessage(), pe);
      }
    }


    // Make sure that at least one of the frequency parameters was given a
    // positive value.
    IntegerParameter addFreqParam =
         parameters.getIntegerParameter(addFrequencyParameter.getName());
    if ((addFreqParam != null) && (addFreqParam.getIntValue() > 0))
    {
      return;
    }

    IntegerParameter compareFreqParam =
         parameters.getIntegerParameter(compareFrequencyParameter.getName());
    if ((compareFreqParam != null) && (compareFreqParam.getIntValue() > 0))
    {
      return;
    }

    IntegerParameter deleteFreqParam =
         parameters.getIntegerParameter(deleteFrequencyParameter.getName());
    if ((deleteFreqParam != null) && (deleteFreqParam.getIntValue() > 0))
    {
      return;
    }

    IntegerParameter modifyFreqParam =
         parameters.getIntegerParameter(modifyFrequencyParameter.getName());
    if ((modifyFreqParam != null) && (modifyFreqParam.getIntValue() > 0))
    {
      return;
    }

    IntegerParameter modifyRDNFreqParam =
         parameters.getIntegerParameter(modifyRDNFrequencyParameter.getName());
    if ((modifyRDNFreqParam != null) && (modifyRDNFreqParam.getIntValue() > 0))
    {
      return;
    }

    IntegerParameter searchFreqParam =
         parameters.getIntegerParameter(searchFrequency1Parameter.getName());
    if ((searchFreqParam != null) && (searchFreqParam.getIntValue() > 0))
    {
      return;
    }

    searchFreqParam =
         parameters.getIntegerParameter(searchFrequency2Parameter.getName());
    if ((searchFreqParam != null) && (searchFreqParam.getIntValue() > 0))
    {
      return;
    }

    searchFreqParam =
         parameters.getIntegerParameter(searchFrequency3Parameter.getName());
    if ((searchFreqParam != null) && (searchFreqParam.getIntValue() > 0))
    {
      return;
    }

    searchFreqParam =
         parameters.getIntegerParameter(searchFrequency4Parameter.getName());
    if ((searchFreqParam != null) && (searchFreqParam.getIntValue() > 0))
    {
      return;
    }

    searchFreqParam =
         parameters.getIntegerParameter(searchFrequency5Parameter.getName());
    if ((searchFreqParam != null) && (searchFreqParam.getIntValue() > 0))
    {
      return;
    }

    searchFreqParam =
         parameters.getIntegerParameter(searchFrequency6Parameter.getName());
    if ((searchFreqParam != null) && (searchFreqParam.getIntValue() > 0))
    {
      return;
    }

    throw new InvalidValueException("At least one operation type must have " +
                                    "a nonzero frequency.");
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean providesParameterTest()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean testJobParameters(ParameterList parameters,
                                   ArrayList<String> outputMessages)
  {
    // Get all the parameters that we might need to perform the test.
    StringParameter hostParam =
         parameters.getStringParameter(addressParameter.getName());
    if ((hostParam == null) || (! hostParam.hasValue()))
    {
      outputMessages.add("ERROR:  No directory server address was provided.");
      return false;
    }
    String host = hostParam.getStringValue();


    IntegerParameter portParam =
         parameters.getIntegerParameter(portParameter.getName());
    if ((portParam == null) || (! hostParam.hasValue()))
    {
      outputMessages.add("ERROR:  No directory server port was provided.");
      return false;
    }
    int port = portParam.getIntValue();


    boolean useSSL = false;
    BooleanParameter useSSLParam =
         parameters.getBooleanParameter(useSSLParameter.getName());
    if (useSSLParam != null)
    {
      useSSL = useSSLParam.getBooleanValue();
    }


    String bindDN = "";
    StringParameter bindDNParam =
         parameters.getStringParameter(bindDNParameter.getName());
    if ((bindDNParam != null) && bindDNParam.hasValue())
    {
      bindDN = bindDNParam.getStringValue();
    }


    String bindPassword = "";
    PasswordParameter bindPWParam =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((bindPWParam != null) && bindPWParam.hasValue())
    {
      bindPassword = bindPWParam.getStringValue();
    }


    StringParameter baseDNParam =
         parameters.getStringParameter(baseDNParameter.getName());
    if ((baseDNParam == null) || (! baseDNParam.hasValue()))
    {
      outputMessages.add("ERROR:  No base DN was provided.");
      return false;
    }
    String baseDN = baseDNParam.getStringValue();


    // Create the LDAPConnection object that we will use to communicate with the
    // directory server.
    LDAPConnection conn;
    if (useSSL)
    {
      try
      {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        conn = new LDAPConnection(sslUtil.createSSLSocketFactory());
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to instantiate the blind trust " +
             "socket factory for use in creating the SSL " +
             "connection:  " + stackTraceToString(e));
        return false;
      }
    }
    else
    {
      conn = new LDAPConnection();
    }


    // Attempt to establish a connection to the directory server.
    try
    {
      if (useSSL)
      {
        outputMessages.add("Attempting to establish an SSL-based connection " +
                           "to " + host + ':' + port + "....");
      }
      else
      {
        outputMessages.add("Attempting to establish a connection to " + host +
                           ':' + port + "....");
      }
      conn.connect(host, port, 10000);
      outputMessages.add("Connected successfully.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to connect to the directory " +
                         "server:  " + stackTraceToString(e));
      return false;
    }


    // Attempt to bind to the directory server using the bind DN and password.
    try
    {
      if ((bindDN != null) && (bindDN.length() > 0) && (bindPassword != null) &&
          (bindPassword.length() > 0))
      {
        outputMessages.add("Attempting to perform an LDAPv3 bind to the " +
             "directory server with a DN of '" + bindDN + "'....");
        conn.bind(bindDN, bindPassword);
        outputMessages.add("Bound successfully.");
        outputMessages.add("");
      }
    }
    catch (Exception e)
    {
      try
      {
        conn.close();
      } catch (Exception e2) {}

      outputMessages.add("ERROR:  Unable to bind to the directory server:  " +
                         stackTraceToString(e));
      return false;
    }


    // Make sure that the entry specified as the base DN exists.
    try
    {
      outputMessages.add("Checking to make sure that the base DN entry '" +
                         baseDN + "' exists in the directory....");
      Entry baseDNEntry = conn.getEntry(baseDN, "1.1");
      if (baseDNEntry == null)
      {
        try
        {
          conn.close();
        } catch (Exception e2) {}

        outputMessages.add("ERROR:  Unable to retrieve the base DN entry.");
        return false;
      }
      else
      {
        outputMessages.add("Successfully read the base DN entry.");
        outputMessages.add("");
      }
    }
    catch (Exception e)
    {
      try
      {
        conn.close();
      } catch (Exception e2) {}

      outputMessages.add("ERROR:  Unable to retrieve the base DN entry:   " +
                         stackTraceToString(e));
      return false;
    }


    // At this point, all tests have passed.  Close the connection and return
    // true.
    try
    {
      conn.close();
    } catch (Exception e) {}

    outputMessages.add("All tests completed successfully.");
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    // Initialize the parent random number generator.
    parentRandom = new Random();


    // Get the address of the directory server.
    ldapHost = null;
    addressParameter =
         parameters.getStringParameter(addressParameter.getName());
    if ((addressParameter != null) && addressParameter.hasValue())
    {
      ldapHost = addressParameter.getStringValue();
    }

    // Get the port for the directory server.
    ldapPort = 389;
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if ((portParameter != null) && portParameter.hasValue())
    {
      ldapPort = portParameter.getIntValue();
    }

    // Get the base DN.
    baseDN = null;
    baseDNParameter = parameters.getStringParameter(baseDNParameter.getName());
    if ((baseDNParameter != null) && baseDNParameter.hasValue())
    {
      baseDN = baseDNParameter.getStringValue();

    }

    // Get the bind DN.
    try
    {
      bindDNPattern = new ValuePattern("");
      bindDNParameter =
           parameters.getStringParameter(bindDNParameter.getName());
      if ((bindDNParameter != null) && bindDNParameter.hasValue())
      {
        bindDNPattern = new ValuePattern(bindDNParameter.getStringValue());
      }
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Could not parse bind DN pattern:  " +
                                     stackTraceToString(e), e);
    }

    // Get the bind password.
    bindPW = "";
    bindPasswordParameter =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((bindPasswordParameter != null) && bindPasswordParameter.hasValue())
    {
      bindPW = bindPasswordParameter.getStringValue();
    }

    // Get the add frequency.
    addFrequency = 0;
    addFrequencyParameter =
         parameters.getIntegerParameter(addFrequencyParameter.getName());
    if ((addFrequencyParameter != null) && addFrequencyParameter.hasValue())
    {
      addFrequency = addFrequencyParameter.getIntValue();
    }

    // Get the compare frequency.
    compareFrequency = 0;
    compareFrequencyParameter =
         parameters.getIntegerParameter(compareFrequencyParameter.getName());
    if ((compareFrequencyParameter != null) &&
        compareFrequencyParameter.hasValue())
    {
      compareFrequency = compareFrequencyParameter.getIntValue();
    }

    // Get the delete frequency.
    deleteFrequency = 0;
    deleteFrequencyParameter =
         parameters.getIntegerParameter(deleteFrequencyParameter.getName());
    if ((deleteFrequencyParameter != null) &&
        deleteFrequencyParameter.hasValue())
    {
      deleteFrequency = deleteFrequencyParameter.getIntValue();
    }

    // Get the modify frequency.
    modifyFrequency = 0;
    modifyFrequencyParameter =
         parameters.getIntegerParameter(modifyFrequencyParameter.getName());
    if ((modifyFrequencyParameter != null) &&
        modifyFrequencyParameter.hasValue())
    {
      modifyFrequency = modifyFrequencyParameter.getIntValue();
    }

    // Get the modify RDN frequency.
    modifyRDNFrequency = 0;
    modifyRDNFrequencyParameter =
         parameters.getIntegerParameter(modifyRDNFrequencyParameter.getName());
    if ((modifyRDNFrequencyParameter != null) &&
        modifyRDNFrequencyParameter.hasValue())
    {
      modifyRDNFrequency = modifyRDNFrequencyParameter.getIntValue();
    }

    // Get the search frequency.
    searchFrequency1 = 0;
    searchFrequency1Parameter =
         parameters.getIntegerParameter(searchFrequency1Parameter.getName());
    if ((searchFrequency1Parameter != null) &&
        searchFrequency1Parameter.hasValue())
    {
      searchFrequency1 = searchFrequency1Parameter.getIntValue();
    }

    // Get the search frequency.
    searchFrequency2 = 0;
    searchFrequency2Parameter =
         parameters.getIntegerParameter(searchFrequency2Parameter.getName());
    if ((searchFrequency2Parameter != null) &&
        searchFrequency2Parameter.hasValue())
    {
      searchFrequency2 = searchFrequency2Parameter.getIntValue();
    }

    // Get the search frequency.
    searchFrequency3 = 0;
    searchFrequency3Parameter =
         parameters.getIntegerParameter(searchFrequency3Parameter.getName());
    if ((searchFrequency3Parameter != null) &&
        searchFrequency3Parameter.hasValue())
    {
      searchFrequency3 = searchFrequency3Parameter.getIntValue();
    }

    // Get the search frequency.
    searchFrequency4 = 0;
    searchFrequency4Parameter =
         parameters.getIntegerParameter(searchFrequency4Parameter.getName());
    if ((searchFrequency4Parameter != null) &&
        searchFrequency4Parameter.hasValue())
    {
      searchFrequency4 = searchFrequency4Parameter.getIntValue();
    }

    // Get the search frequency.
    searchFrequency5 = 0;
    searchFrequency5Parameter =
         parameters.getIntegerParameter(searchFrequency5Parameter.getName());
    if ((searchFrequency5Parameter != null) &&
        searchFrequency5Parameter.hasValue())
    {
      searchFrequency5 = searchFrequency5Parameter.getIntValue();
    }

    // Get the search frequency.
    searchFrequency6 = 0;
    searchFrequency6Parameter =
         parameters.getIntegerParameter(searchFrequency6Parameter.getName());
    if ((searchFrequency6Parameter != null) &&
        searchFrequency6Parameter.hasValue())
    {
      searchFrequency6 = searchFrequency6Parameter.getIntValue();
    }

    // Calculate the total of all the frequencies and create the frequency array
    totalFrequency = addFrequency + compareFrequency + deleteFrequency +
                     modifyFrequency + modifyRDNFrequency + searchFrequency1 +
                     searchFrequency2 + searchFrequency3 + searchFrequency4 +
                     searchFrequency5 + searchFrequency6;
    opWeights = new int[11];
    opWeights[0]  = addFrequency;
    opWeights[1]  = opWeights[0] + compareFrequency;
    opWeights[2]  = opWeights[1] + deleteFrequency;
    opWeights[3]  = opWeights[2] + modifyFrequency;
    opWeights[4]  = opWeights[3] + modifyRDNFrequency;
    opWeights[5]  = opWeights[4] + searchFrequency1;
    opWeights[6]  = opWeights[5] + searchFrequency2;
    opWeights[7]  = opWeights[6] + searchFrequency3;
    opWeights[8]  = opWeights[7] + searchFrequency4;
    opWeights[9]  = opWeights[8] + searchFrequency5;
    opWeights[10] = opWeights[9] + searchFrequency6;

    // Get the list of filters to use.
    searchFilters1 = new String[0];
    filterFile1URLParameter =
         parameters.getFileURLParameter(filterFile1URLParameter.getName());
    if ((filterFile1URLParameter != null) &&
        (filterFile1URLParameter.hasValue()))
    {
      try
      {
        searchFilters1 = filterFile1URLParameter.getNonBlankFileLines();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to retrieve filter list 1:  " +
                                       e, e);
      }
    }

    // Get the list of filters to use.
    searchFilters2 = new String[0];
    filterFile2URLParameter =
         parameters.getFileURLParameter(filterFile2URLParameter.getName());
    if ((filterFile2URLParameter != null) &&
        (filterFile2URLParameter.hasValue()))
    {
      try
      {
        searchFilters2 = filterFile2URLParameter.getNonBlankFileLines();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to retrieve filter list 2:  " +
                                       e, e);
      }
    }

    // Get the list of filters to use.
    searchFilters3 = new String[0];
    filterFile3URLParameter =
         parameters.getFileURLParameter(filterFile3URLParameter.getName());
    if ((filterFile3URLParameter != null) &&
        (filterFile3URLParameter.hasValue()))
    {
      try
      {
        searchFilters3 = filterFile3URLParameter.getNonBlankFileLines();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to retrieve filter list 3:  " +
                                       e, e);
      }
    }

    // Get the list of filters to use.
    searchFilters4 = new String[0];
    filterFile4URLParameter =
         parameters.getFileURLParameter(filterFile4URLParameter.getName());
    if ((filterFile4URLParameter != null) &&
        (filterFile4URLParameter.hasValue()))
    {
      try
      {
        searchFilters4 = filterFile4URLParameter.getNonBlankFileLines();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to retrieve filter list 4:  " +
                                       e, e);
      }
    }

    // Get the list of filters to use.
    searchFilters5 = new String[0];
    filterFile5URLParameter =
         parameters.getFileURLParameter(filterFile5URLParameter.getName());
    if ((filterFile5URLParameter != null) &&
        (filterFile5URLParameter.hasValue()))
    {
      try
      {
        searchFilters5 = filterFile5URLParameter.getNonBlankFileLines();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to retrieve filter list 5:  " +
                                       e, e);
      }
    }

    // Get the list of filters to use.
    searchFilters6 = new String[0];
    filterFile6URLParameter =
         parameters.getFileURLParameter(filterFile6URLParameter.getName());
    if ((filterFile6URLParameter != null) &&
        (filterFile6URLParameter.hasValue()))
    {
      try
      {
        searchFilters6 = filterFile6URLParameter.getNonBlankFileLines();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to retrieve filter list 6:  " +
                                       e, e);
      }
    }

    // Get the attribute to compare/modify
    attrParameter = parameters.getStringParameter(attrParameter.getName());
    if ((attrParameter != null) && attrParameter.hasValue())
    {
      modAttr = attrParameter.getStringValue();
    }

    // Get the size limit
    sizeLimit = 0;
    sizeLimitParameter =
         parameters.getIntegerParameter(sizeLimitParameter.getName());
    if ((sizeLimitParameter != null) && sizeLimitParameter.hasValue())
    {
      sizeLimit = sizeLimitParameter.getIntValue();
    }

    // Get the time limit
    timeLimit = 0;
    timeLimitParameter =
         parameters.getIntegerParameter(timeLimitParameter.getName());
    if ((timeLimitParameter != null) && timeLimitParameter.hasValue())
    {
      timeLimit = timeLimitParameter.getIntValue();
    }

    // Get the warm-up time.
    warmUpTime = 0;
    warmUpParameter = parameters.getIntegerParameter(warmUpParameter.getName());
    if ((warmUpParameter != null) && warmUpParameter.hasValue())
    {
      warmUpTime = warmUpParameter.getIntValue();
    }

    // Get the cool-down time.
    coolDownTime = 0;
    coolDownParameter =
         parameters.getIntegerParameter(coolDownParameter.getName());
    if ((coolDownParameter != null) && coolDownParameter.hasValue())
    {
      coolDownTime = coolDownParameter.getIntValue();
    }

    // Get the time between requests
    operationDelay = 0;
    delayParameter =
         parameters.getIntegerParameter(delayParameter.getName());
    if ((delayParameter != null) && delayParameter.hasValue())
    {
      operationDelay = delayParameter.getIntValue();
    }

    // Initialize the rate limiter.
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

    // Get the number of operations between rebinds.
    opsBetweenBinds = 0;
    opsBetweenBindsParameter =
         parameters.getIntegerParameter(opsBetweenBindsParameter.getName());
    if ((opsBetweenBindsParameter != null) &&
        (opsBetweenBindsParameter.hasValue()))
    {
      opsBetweenBinds = opsBetweenBindsParameter.getIntValue();
    }

    // Get the use SSL flag
    useSSL = false;
    useSSLParameter =
         parameters.getBooleanParameter(useSSLParameter.getName());
    if (useSSLParameter != null)
    {
      useSSL = useSSLParameter.getBooleanValue();
    }


    // Determine whether to perform cleanup after the job is done.
    cleanUp = true;
    cleanUpParameter =
         parameters.getBooleanParameter(cleanUpParameter.getName());
    if (cleanUpParameter != null)
    {
      cleanUp = cleanUpParameter.getBooleanValue();
    }

    // Get the always disconnect flag
    alwaysDisconnect = false;
    disconnectParameter =
         parameters.getBooleanParameter(disconnectParameter.getName());
    if (disconnectParameter != null)
    {
      alwaysDisconnect = disconnectParameter.getBooleanValue();
    }

    // Get the follow referrals flag
    followReferrals = false;
    referralsParameter =
         parameters.getBooleanParameter(referralsParameter.getName());
    if (referralsParameter != null)
    {
      followReferrals = referralsParameter.getBooleanValue();
    }


    // Make sure that the list of DNs to delete is empty.
    addedDNs.clear();
    dnsToDelete = 0;
  }


  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // Create all the stat trackers.
    addCount       = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_ADD_ATTEMPTS,
                                            collectionInterval);
    compareCount   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_COMPARE_ATTEMPTS,
                                            collectionInterval);
    deleteCount    = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_DELETE_ATTEMPTS,
                                            collectionInterval);
    modifyCount    = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_MODIFY_ATTEMPTS,
                                            collectionInterval);
    modifyRDNCount = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_MODIFY_RDN_ATTEMPTS,
                                            collectionInterval);
    searchCount1   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SEARCH_ATTEMPTS_1,
                                            collectionInterval);
    searchCount2   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SEARCH_ATTEMPTS_2,
                                            collectionInterval);
    searchCount3   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SEARCH_ATTEMPTS_3,
                                            collectionInterval);
    searchCount4   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SEARCH_ATTEMPTS_4,
                                            collectionInterval);
    searchCount5   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SEARCH_ATTEMPTS_5,
                                            collectionInterval);
    searchCount6   = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SEARCH_ATTEMPTS_6,
                                            collectionInterval);
    operationCount = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_OPERATION_ATTEMPTS,
                                            collectionInterval);
    addTimer       = new TimeTracker(clientID, threadID, STAT_TRACKER_ADD_TIME,
                                     collectionInterval);
    compareTimer   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_COMPARE_TIME,
                                     collectionInterval);
    deleteTimer    = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_DELETE_TIME,
                                     collectionInterval);
    modifyTimer    = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_MODIFY_TIME,
                                     collectionInterval);
    modifyRDNTimer = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_MODIFY_RDN_TIME,
                                     collectionInterval);
    searchTimer1   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_SEARCH_TIME_1,
                                     collectionInterval);
    searchTimer2   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_SEARCH_TIME_2,
                                     collectionInterval);
    searchTimer3   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_SEARCH_TIME_3,
                                     collectionInterval);
    searchTimer4   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_SEARCH_TIME_4,
                                     collectionInterval);
    searchTimer5   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_SEARCH_TIME_5,
                                     collectionInterval);
    searchTimer6   = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_SEARCH_TIME_6,
                                     collectionInterval);
    operationTimer = new TimeTracker(clientID, threadID,
                                     STAT_TRACKER_OPERATION_TIME,
                                     collectionInterval);
    resultCodes    = new CategoricalTracker(clientID, threadID,
                                            STAT_TRACKER_RESULT_CODES,
                                            collectionInterval);
    operationTypes =
         new CategoricalTracker(clientID, threadID,
                                STAT_TRACKER_OPERATION_ATTEMPTS_BY_CATEGORY,
                                collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      addCount.enableRealTimeStats(statReporter, jobID);
      compareCount.enableRealTimeStats(statReporter, jobID);
      deleteCount.enableRealTimeStats(statReporter, jobID);
      modifyCount.enableRealTimeStats(statReporter, jobID);
      modifyRDNCount.enableRealTimeStats(statReporter, jobID);
      searchCount1.enableRealTimeStats(statReporter, jobID);
      searchCount2.enableRealTimeStats(statReporter, jobID);
      searchCount3.enableRealTimeStats(statReporter, jobID);
      searchCount4.enableRealTimeStats(statReporter, jobID);
      searchCount5.enableRealTimeStats(statReporter, jobID);
      searchCount6.enableRealTimeStats(statReporter, jobID);
      operationCount.enableRealTimeStats(statReporter, jobID);
      addTimer.enableRealTimeStats(statReporter, jobID);
      compareTimer.enableRealTimeStats(statReporter, jobID);
      deleteTimer.enableRealTimeStats(statReporter, jobID);
      modifyTimer.enableRealTimeStats(statReporter, jobID);
      modifyRDNTimer.enableRealTimeStats(statReporter, jobID);
      searchTimer1.enableRealTimeStats(statReporter, jobID);
      searchTimer2.enableRealTimeStats(statReporter, jobID);
      searchTimer3.enableRealTimeStats(statReporter, jobID);
      searchTimer4.enableRealTimeStats(statReporter, jobID);
      searchTimer5.enableRealTimeStats(statReporter, jobID);
      searchTimer6.enableRealTimeStats(statReporter, jobID);
      operationTimer.enableRealTimeStats(statReporter, jobID);
    }


    // Create the random number generator for this thread
    random = new Random(parentRandom.nextLong());


    // If SSL will be used, then establish an SSL-based connection to the
    // directory server.  When using JSSE, the first connection always takes
    // longer than the subsequent connections, so we want to get that out of the
    // way before the job actually starts running.
    if (useSSL)
    {
      try
      {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        conn = new LDAPConnection(sslUtil.createSSLSocketFactory());
        conn.connect(ldapHost, ldapPort, 10000);
        conn.close();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to establish an SSL-based " +
                                       "connection to the directory:  " + e, e);
      }
    }
    else
    {
      conn = new LDAPConnection();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Determine the range of time for which we should collect statistics.
    long currentTime = System.currentTimeMillis();
    collectingStats = false;
    long startCollectingTime = currentTime + (1000 * warmUpTime);
    long stopCollectingTime  = Long.MAX_VALUE;
    if ((coolDownTime > 0) && (getShouldStopTime() > 0))
    {
      stopCollectingTime = getShouldStopTime() - (1000 * coolDownTime);
    }


    // Set a variable that we can use to determine if the connection is alive
    boolean bound     = false;
    boolean connected = false;

    currentBindDN = null;
    int currentOpNumber = 0;


    // Create a loop that will run for the appropriate length of time.
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
      if ((! collectingStats) && (opStartTime >= startCollectingTime) &&
          (opStartTime < stopCollectingTime))
      {
        // Tell the stat trackers that they should start tracking now
        addCount.startTracker();
        compareCount.startTracker();
        deleteCount.startTracker();
        modifyCount.startTracker();
        modifyRDNCount.startTracker();
        searchCount1.startTracker();
        searchCount2.startTracker();
        searchCount3.startTracker();
        searchCount4.startTracker();
        searchCount5.startTracker();
        searchCount6.startTracker();
        operationCount.startTracker();
        addTimer.startTracker();
        compareTimer.startTracker();
        deleteTimer.startTracker();
        modifyTimer.startTracker();
        modifyRDNTimer.startTracker();
        searchTimer1.startTracker();
        searchTimer2.startTracker();
        searchTimer3.startTracker();
        searchTimer4.startTracker();
        searchTimer5.startTracker();
        searchTimer6.startTracker();
        operationTimer.startTracker();
        operationTypes.startTracker();
        resultCodes.startTracker();
        collectingStats = true;
      }
      else if ((collectingStats) && (opStartTime >= stopCollectingTime))
      {
        addCount.stopTracker();
        compareCount.stopTracker();
        deleteCount.stopTracker();
        modifyCount.stopTracker();
        modifyRDNCount.stopTracker();
        searchCount1.stopTracker();
        searchCount2.stopTracker();
        searchCount3.stopTracker();
        searchCount4.stopTracker();
        searchCount5.stopTracker();
        searchCount6.stopTracker();
        operationCount.stopTracker();
        addTimer.stopTracker();
        compareTimer.stopTracker();
        deleteTimer.stopTracker();
        modifyTimer.stopTracker();
        modifyRDNTimer.stopTracker();
        searchTimer1.stopTracker();
        searchTimer2.stopTracker();
        searchTimer3.stopTracker();
        searchTimer4.stopTracker();
        searchTimer5.stopTracker();
        searchTimer6.stopTracker();
        operationTimer.stopTracker();
        operationTypes.stopTracker();
        resultCodes.stopTracker();
        collectingStats = false;
      }


      // If the connection is currently not connected, then establish it
      if (! connected)
      {
        try
        {
          currentBindDN = bindDNPattern.nextValue();
          conn.connect(ldapHost, ldapPort, 10000);

          if ((currentBindDN != null) && (currentBindDN.length() > 0) &&
              (bindPW != null) && (bindPW.length() > 0))
          {
            conn.bind(currentBindDN, bindPW);
          }

          bound     = true;
          connected = true;

          LDAPConnectionOptions opts = conn.getConnectionOptions();
          opts.setFollowReferrals(followReferrals);
          opts.setResponseTimeoutMillis(1000L * timeLimit);
          conn.setConnectionOptions(opts);
        }
        catch (Exception e)
        {
          logMessage("ERROR -- Could not connect to " + ldapHost + ':' +
                     ldapPort + " (" + e + ") -- aborting thread");
          if (collectingStats)
          {
            if (e instanceof LDAPException)
            {
              LDAPException le = (LDAPException) e;
              resultCodes.increment(String.valueOf(le.getResultCode()));
            }
            else
            {
              resultCodes.increment(String.valueOf(ResultCode.CONNECT_ERROR));
            }
          }
          indicateStoppedDueToError();
          break;
        }
      }
      else if (! bound)
      {
        try
        {
          currentBindDN = bindDNPattern.nextValue();
          if ((currentBindDN != null) && (currentBindDN.length() > 0) &&
              (bindPW != null) && (bindPW.length() > 0))
          {
            conn.bind(currentBindDN, bindPW);
          }
          bound     = true;
          connected = true;
        }
        catch (Exception e)
        {
          if (collectingStats)
          {
            if (e instanceof LDAPException)
            {
              LDAPException le = (LDAPException) e;
              resultCodes.increment(String.valueOf(le.getResultCode()));
            }
            else
            {
              resultCodes.increment(String.valueOf(ResultCode.CONNECT_ERROR));
            }
          }
        }
      }


      // Pick the type of operation to perform and then do it.
      ResultCode resultCode;
      int opType = (random.nextInt() & 0x7FFFFFFF) % totalFrequency;

      if (collectingStats)
      {
        operationCount.increment();
        operationTimer.startTimer();
      }

      currentOpNumber++;

      if (opType < opWeights[0])
      {
        if (collectingStats)
        {
          operationTypes.increment("Add");
        }
        resultCode = doAdd();
      }
      else if (opType < opWeights[1])
      {
        if (collectingStats)
        {
          operationTypes.increment("Compare");
        }
        resultCode = doCompare();
      }
      else if (opType < opWeights[2])
      {
        if (collectingStats)
        {
          operationTypes.increment("Delete");
        }
        resultCode = doDelete();
      }
      else if (opType < opWeights[3])
      {
        if (collectingStats)
        {
          operationTypes.increment("Modify");
        }
        resultCode = doModify();
      }
      else if (opType < opWeights[4])
      {
        if (collectingStats)
        {
          operationTypes.increment("Modify RDN");
        }
        resultCode = doModifyRDN();
      }
      else if (opType < opWeights[5])
      {
        if (collectingStats)
        {
          operationTypes.increment("Search 1");
        }
        resultCode = doSearch1();
      }
      else if (opType < opWeights[6])
      {
        if (collectingStats)
        {
          operationTypes.increment("Search 2");
        }
        resultCode = doSearch2();
      }
      else if (opType < opWeights[7])
      {
        if (collectingStats)
        {
          operationTypes.increment("Search 3");
        }
        resultCode = doSearch3();
      }
      else if (opType < opWeights[8])
      {
        if (collectingStats)
        {
          operationTypes.increment("Search 4");
        }
        resultCode = doSearch4();
      }
      else if (opType < opWeights[9])
      {
        if (collectingStats)
        {
          operationTypes.increment("Search 5");
        }
        resultCode = doSearch5();
      }
      else
      {
        if (collectingStats)
        {
          operationTypes.increment("Search 6");
        }
        resultCode = doSearch6();
      }

      if (collectingStats)
      {
        operationTimer.stopTimer();
        resultCodes.increment(String.valueOf(resultCode));
      }


      // See if we need to close the connection to the directory.
      if ((opsBetweenBinds > 0) && (currentOpNumber >= opsBetweenBinds))
      {
        if (alwaysDisconnect)
        {
          try
          {
            conn.close();
          } catch (Exception e) {}
          bound     = false;
          connected = false;
        }
        else
        {
          bound = false;
        }

        currentOpNumber = 0;
      }


      // See if we need to sleep until the next operation.
      if (operationDelay > 0)
      {
        long opTime = System.currentTimeMillis() - opStartTime;
        if ((opTime < operationDelay) && (! shouldStop()))
        {
          try
          {
            Thread.sleep(operationDelay - opTime);
          } catch (InterruptedException ie) {}
        }
      }
    }


    // If we are still collecting statistics, then stop.
    if (collectingStats)
    {
      addCount.stopTracker();
      compareCount.stopTracker();
      deleteCount.stopTracker();
      modifyCount.stopTracker();
      modifyRDNCount.stopTracker();
      searchCount1.stopTracker();
      searchCount2.stopTracker();
      searchCount3.stopTracker();
      searchCount4.stopTracker();
      searchCount5.stopTracker();
      searchCount6.stopTracker();
      operationCount.stopTracker();
      addTimer.stopTracker();
      compareTimer.stopTracker();
      deleteTimer.stopTracker();
      modifyTimer.stopTracker();
      modifyRDNTimer.stopTracker();
      searchTimer1.stopTracker();
      searchTimer2.stopTracker();
      searchTimer3.stopTracker();
      searchTimer4.stopTracker();
      searchTimer5.stopTracker();
      searchTimer6.stopTracker();
      operationTimer.stopTracker();
      operationTypes.stopTracker();
      resultCodes.stopTracker();
    }


    // If we are still connected to the directory, then disconnect.
    try
    {
      conn.close();
    } catch (Exception e) {}
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void finalizeClient()
  {
    if (cleanUp)
    {
      if (useSSL)
      {
        try
        {
          SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
          conn = new LDAPConnection(sslUtil.createSSLSocketFactory());
        }
        catch (Exception e)
        {
          logMessage("Unable to establish an SSL-based connection to the " +
                     "directory to perform cleanup:  " + e);
          return;
        }
      }
      else
      {
        conn = new LDAPConnection();
      }

      try
      {
        conn.connect(ldapHost, ldapPort, 10000);

        String bindDN = bindDNPattern.nextValue();
        if ((bindDN != null) && (bindDN.length() > 0) &&
            (bindPW != null) && (bindPW.length() > 0))
        {
          conn.bind(bindDN, bindPW);
        }
      }
      catch (Exception e)
      {
        logMessage("Unable to establish a connection to the directory to " +
                   "perform cleanup:  " + e);
        return;
      }


      while (! addedDNs.isEmpty())
      {
        String dnToDelete = addedDNs.removeFirst();

        try
        {
          conn.delete(dnToDelete);
        }
        catch (LDAPException le)
        {
          logMessage("Unable to perform cleanup -- exception thrown while " +
                     "trying to delete entry \"" + dnToDelete + "\":  " + le);
          try
          {
            conn.close();
          } catch (Exception e) {}
          return;
        }
      }

      try
      {
        conn.close();
      } catch (Exception e) {}
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    if (conn != null)
    {
      try
      {
        conn.close();
      } catch (Exception e) {}

      conn = null;
    }
  }



  /**
   * Performs an add operation in the directory.
   *
   * @return  The result code of the add operation.
   */
  private ResultCode doAdd()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    Entry      entry      = getEntry();
    String     entryDN    = entry.getDN();

    if (collectingStats)
    {
      addCount.increment();
      addTimer.startTimer();
    }

    try
    {
      conn.add(entry);
      addDNToDelete(entryDN);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      addTimer.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a compare operation in the directory.
   *
   * @return  The result code of the compare operation.
   */
  private ResultCode doCompare()
  {
    ResultCode resultCode;
    if (collectingStats)
    {
      compareCount.increment();
      compareTimer.startTimer();
    }

    try
    {
      LDAPResult result =
           conn.compare(currentBindDN, modAttr, getRandomString(80));
      resultCode = result.getResultCode();
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      compareTimer.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a delete operation in the directory.
   *
   * @return  The result code of the delete operation.
   */
  private ResultCode doDelete()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     dn         = getDNToDelete();

    if (dn == null)
    {
      return ResultCode.PARAM_ERROR;
    }

    if (collectingStats)
    {
      deleteCount.increment();
      deleteTimer.startTimer();
    }

    try
    {
      conn.delete(dn);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      deleteTimer.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a modify operation in the directory.
   *
   * @return  The result code of the modify operation.
   */
  private ResultCode doModify()
  {
    ResultCode   resultCode = ResultCode.SUCCESS;
    String       dn         = currentBindDN;
    Modification mod        = new Modification(ModificationType.REPLACE,
         modAttr, getRandomString(80));

    if (collectingStats)
    {
      modifyCount.increment();
      modifyTimer.startTimer();
    }

    try
    {
      conn.modify(dn, mod);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      modifyTimer.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a modify RDN (i.e., rename) operation in the directory.
   *
   * @return  The result code of the modify RDN operation.
   */
  private ResultCode doModifyRDN()
  {
    ResultCode resultCode  = ResultCode.SUCCESS;
    String     dn          = getDNToDelete();
    String     newRDNValue = getRandomString(80);

    if (dn == null)
    {
      return ResultCode.PARAM_ERROR;
    }

    if (collectingStats)
    {
      modifyRDNCount.increment();
      modifyRDNTimer.startTimer();
    }

    try
    {

      conn.modifyDN(dn, "uid=" + newRDNValue, true);
      addDNToDelete("uid=" + newRDNValue + ',' + baseDN);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      modifyRDNTimer.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a search operation in the directory.
   *
   * @return  The result code of the search operation.
   */
  private ResultCode doSearch1()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     filter     = searchFilters1[(random.nextInt() & 0x7FFFFFFF) %
                                           searchFilters1.length];

    if (collectingStats)
    {
      searchCount1.increment();
      searchTimer1.startTimer();
    }

    try
    {
      SearchRequest searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
           filter);
      searchRequest.setSizeLimit(sizeLimit);
      searchRequest.setTimeLimitSeconds(timeLimit);

      conn.search(searchRequest);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      searchTimer1.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a search operation in the directory.
   *
   * @return  The result code of the search operation.
   */
  private ResultCode doSearch2()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     filter     = searchFilters2[(random.nextInt() & 0x7FFFFFFF) %
                                           searchFilters2.length];

    if (collectingStats)
    {
      searchCount2.increment();
      searchTimer2.startTimer();
    }

    try
    {
      SearchRequest searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
           filter);
      searchRequest.setSizeLimit(sizeLimit);
      searchRequest.setTimeLimitSeconds(timeLimit);

      conn.search(searchRequest);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      searchTimer2.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a search operation in the directory.
   *
   * @return  The result code of the search operation.
   */
  private ResultCode doSearch3()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     filter     = searchFilters3[(random.nextInt() & 0x7FFFFFFF) %
                                           searchFilters3.length];

    if (collectingStats)
    {
      searchCount3.increment();
      searchTimer3.startTimer();
    }

    try
    {
      SearchRequest searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
           filter);
      searchRequest.setSizeLimit(sizeLimit);
      searchRequest.setTimeLimitSeconds(timeLimit);

      conn.search(searchRequest);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      searchTimer3.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a search operation in the directory.
   *
   * @return  The result code of the search operation.
   */
  private ResultCode doSearch4()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     filter     = searchFilters4[(random.nextInt() & 0x7FFFFFFF) %
                                           searchFilters4.length];

    if (collectingStats)
    {
      searchCount4.increment();
      searchTimer4.startTimer();
    }

    try
    {
      SearchRequest searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
           filter);
      searchRequest.setSizeLimit(sizeLimit);
      searchRequest.setTimeLimitSeconds(timeLimit);

      conn.search(searchRequest);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      searchTimer4.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a search operation in the directory.
   *
   * @return  The result code of the search operation.
   */
  private ResultCode doSearch5()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     filter     = searchFilters5[(random.nextInt() & 0x7FFFFFFF) %
                                           searchFilters5.length];

    if (collectingStats)
    {
      searchCount5.increment();
      searchTimer5.startTimer();
    }

    try
    {
      SearchRequest searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
           filter);
      searchRequest.setSizeLimit(sizeLimit);
      searchRequest.setTimeLimitSeconds(timeLimit);

      conn.search(searchRequest);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      searchTimer5.stopTimer();
    }

    return resultCode;
  }



  /**
   * Performs a search operation in the directory.
   *
   * @return  The result code of the search operation.
   */
  private ResultCode doSearch6()
  {
    ResultCode resultCode = ResultCode.SUCCESS;
    String     filter     = searchFilters6[(random.nextInt() & 0x7FFFFFFF) %
                                           searchFilters6.length];

    if (collectingStats)
    {
      searchCount6.increment();
      searchTimer6.startTimer();
    }

    try
    {
      SearchRequest searchRequest = new SearchRequest(baseDN, SearchScope.SUB,
           filter);
      searchRequest.setSizeLimit(sizeLimit);
      searchRequest.setTimeLimitSeconds(timeLimit);

      conn.search(searchRequest);
    }
    catch (LDAPException le)
    {
      resultCode = le.getResultCode();
    }

    if (collectingStats)
    {
      searchTimer6.stopTimer();
    }

    return resultCode;
  }



  /**
   * Retrieves a randomly-generated entry that may be added to the directory.
   *
   * @return  A randomly-generated entry that may be added to the directory.
   */
  private Entry getEntry()
  {
    String randomString = getRandomString(80);
    return new Entry("uid=" + randomString + ',' + baseDN,
         new Attribute("objectClass", "top", "person", "organizationalPerson",
              "inetOrgPerson"),
         new Attribute("uid", randomString),
         new Attribute("givenName", randomString),
         new Attribute("sn", randomString),
         new Attribute("cn", randomString),
         new Attribute("userPassword", randomString));
  }



  /**
   * Retrieves a randomly-generated string with the specified number of
   * characters.
   *
   * @param  length  The number of characters to include in the string.
   *
   * @return  The randomly-generated string.
   */
  private String getRandomString(int length)
  {
    char[] returnChars = new char[length];

    for (int i=0; i < returnChars.length; i++)
    {
      returnChars[i] = ALPHABET[(random.nextInt() & 0x7FFFFFFF) %
                                ALPHABET.length];
    }

    return new String(returnChars);
  }



  /**
   * Adds the specified DN to the list of DNs to be deleted and/or renamed.
   *
   * @param  entryDN  The DN to be added to the list.
   */
  private static void addDNToDelete(String entryDN)
  {
    synchronized (addedDNMutex)
    {
      dnsToDelete++;
      addedDNs.add(entryDN);
    }
  }



  /**
   * Retrieves the DN of an entry to be deleted or renamed.  Delete and modify
   * RDN operations will only be performed on entries that have been added.
   *
   * @return  The DN of an entry that can be deleted or renamed.  If there are
   *          no available DNs, then <CODE>null</CODE> will be returned.
   */
  private static String getDNToDelete()
  {
    synchronized (addedDNMutex)
    {
      if (dnsToDelete > 0)
      {
        dnsToDelete--;
        return addedDNs.removeFirst();
      }
    }

    return null;
  }
}

