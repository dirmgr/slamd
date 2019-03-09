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
import java.util.Random;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
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



/**
 * This class defines a SLAMD job that simulates the load that some versions of
 * Netegrity SiteMinder have been observed to place on an LDAP directory server.
 * It uses a weighted access pattern.  The load that it uses is based on the
 * following (rather inefficient) sequence of events:
 *
 * <OL>
 *   <LI>Perform a subtree search from the directory suffix to find the user's
 *       entry based on a login ID.</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve the
 *       objectClass attribute.</LI>
 *   <LI>Perform a bind as the user.  This is done on a different connection
 *       than all of the other steps.</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve a given
 *       user-specified attribute (attr1).</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve a second
 *       user-specified attribute (attr2).</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve the first
 *       attribute (attr1).</LI>
 *   <LI>Perform a modification on the user's entry.</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve the first
 *       attribute again.</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve the first
 *       attribute again.</LI>
 *   <LI>Perform a base-level search on the user's entry to retrieve a third
 *       attribute (attr3).</LI>
 * </OL>
 *
 *
 * @author  Neil A. Wilson
 */
public class WeightedSiteMinderJobClass
       extends JobClass
{
  /**
   * The set of characters that will be used to generate random values for the
   * modifications.
   */
  public static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  /**
   * The default value for the first attribute to retrieve during the
   * authentication process.
   */
  public static final String DEFAULT_ATTR1 = "givenName";



  /**
   * The default value for the second attribute to retrieve during the
   * authentication process.
   */
  public static final String DEFAULT_ATTR2 = "sn";



  /**
   * The default value for the third attribute to retrieve during the
   * authentication process.
   */
  public static final String DEFAULT_ATTR3= "cn";



  /**
   * The default attribute used as the login ID.
   */
  public static final String DEFAULT_LOG_ID_ATTR = "uid";



  /**
   * The name of the stat tracker that will be used to count the number of
   * overall authentication attempts.
   */
  public static final String STAT_TRACKER_OVERALL_AUTHENTICATION_ATTEMPTS =
       "Overall Authentication Attempts";



  /**
   * The name of the stat tracker that will be used to count the number of
   * authentication attempts based on the first user criteria.
   */
  public static final String STAT_TRACKER_AUTHENTICATION_1_ATTEMPTS =
       "Authentication 1 Attempts";



  /**
   * The name of the stat tracker that will be used to count the number of
   * authentication attempts based on the second user criteria.
   */
  public static final String STAT_TRACKER_AUTHENTICATION_2_ATTEMPTS =
       "Authentication 2 Attempts";



  /**
   * The name of the stat tracker that will be used to keep track of the overall
   * time required to perform each authentication.
   */
  public static final String STAT_TRACKER_OVERALL_AUTHENTICATION_TIME =
       "Overall Authentication Time (ms)";



  /**
   * The name of the stat tracker that will be used to keep track of the time
   * required to perform each authentication based on the first user criteria.
   */
  public static final String STAT_TRACKER_AUTHENTICATION_1_TIME =
       "Authentication 1 Time (ms)";



  /**
   * The name of the stat tracker that will be used to keep track of the time
   * required to perform each authentication based on the second user criteria.
   */
  public static final String STAT_TRACKER_AUTHENTICATION_2_TIME =
       "Authentication 2 Time (ms)";



  /**
   * The name of the stat tracker that will be used to count the number of
   * overall failed authentications.
   */
  public static final String STAT_TRACKER_OVERALL_FAILED_AUTHENTICATIONS =
       "Overall Failed Authentications";



  /**
   * The name of the stat tracker that will be used to count the number of
   * failed authentications based on the first user criteria.
   */
  public static final String STAT_TRACKER_FAILED_AUTHENTICATIONS_1 =
       "Failed Authentications 1";



  /**
   * The name of the stat tracker that will be used to count the number of
   * failed authentications based on the second user criteria.
   */
  public static final String STAT_TRACKER_FAILED_AUTHENTICATIONS_2 =
       "Failed Authentications 2";



  /**
   * The name of the stat tracker that will be used to categorize the reasons
   * for the failed auths.
   */
  public static final String STAT_TRACKER_FAIL_REASON = "Failure Reason";



  /**
   * The name of the stat tracker that will be used to count the overall number
   * of successful authentications.
   */
  public static final String STAT_TRACKER_OVERALL_SUCCESSFUL_AUTHENTICATIONS =
       "Overall Successful Authentications";



  /**
   * The name of the stat tracker that will be used to count the number of
   * successful authentications based on the first user criteria.
   */
  public static final String STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS_1 =
       "Successful Authentications 1";



  /**
   * The name of the stat tracker that will be used to count the number of
   * successful authentications based on the second user criteria.
   */
  public static final String STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS_2 =
       "Successful Authentications 2";



  /**
   * The default set of attributes to include in the modification.
   */
  public static final String[] DEFAULT_ATTRS_TO_MODIFY = new String[]
  {
    "description"
  };



  // Indicates whether bind failures because of invalid credentials will be
  // ignored (so you don't actually have to know user passwords).
  private static boolean ignoreInvalidCredentials;

  // Indicates whether the bind should be attempted or skipped.
  private static boolean skipBind;

  // Indicates whether all threads will used a shared set of connections or if
  // each thread will have its own connection.
  private static boolean useSharedConnections;

  // Indicates whether to use SSL when communicating with the directory.
  private static boolean useSSL;

  // The search filter to use when searching on the first attribute.
  private static Filter filter1;

  // The search filter to use when searching on the second attribute.
  private static Filter filter2;

  // The search filter to use when searching on the third attribute.
  private static Filter filter3;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // The time to keep working after stopping statistics collection.
  private static int coolDownTime;

  // The port number of the directory server.
  private static int directoryPort;

  // The percentage of the time that the first login ID should be used.
  private static int loginID1Percent;

  // The maximum length of time that any single LDAP operation will be allowed
  // to take before it is cancelled.
  private static int timeLimit;

  // The time to start working before beginning statistics collection.
  private static int warmUpTime;

  // The delay in milliseconds between authentication attempts.
  private static long delay;

  // The LDAP connection that will be used for shared authentication operations.
  private static LDAPConnection sharedAuthConnection;

  // The LDAP connection that will be used for shared bind operations.
  private static LDAPConnection sharedBindConnection;

  // The random number generator that will seed the thread-specific random
  // number generators.
  private static Random parentRandom;

  // The DN to use to bind to the directory when performing the search and
  // modify operations.
  private static String bindDN;

  // The password for the bind DN.
  private static String bindPW;

  // The address of the directory server.
  private static String directoryHost;

  // The name of the attribute that will be used to initially find the user's
  // entry (the login ID attribute).
  private static String loginIDAttr;

  // The password to use when authenticating.
  private static String loginPassword;

  // The name of the first attribute to retrieve.
  private static String searchAttr1;

  // The name of the second attribute to retrieve.
  private static String searchAttr2;

  // The name of the third attribute to retrieve.
  private static String searchAttr3;

  // The DN to use as the search base when trying to find user entries in the
  // directory.
  private static String searchBase;

  // The names of the attributes to alter in the modification.
  private static String[] modAttrs;

  // The set of attributes to return when retrieving the first attribute.
  private static String[] returnAttrs1;

  // The set of attributes to return when retrieving the second attribute.
  private static String[] returnAttrs2;

  // The set of attributes to return when retrieving the third attribute.
  private static String[] returnAttrs3;

  // The set of attributes to return when retrieving the set of object classes.
  private static String[] returnAttrsOC;

  // The value pattern to use to construct the first login ID pattern.
  private static ValuePattern loginIDPattern1;

  // The value pattern to use to construct the second login ID pattern.
  private static ValuePattern loginIDPattern2;



  // The parameter that indicates whether the client should trust any SSL cert.
  private BooleanParameter blindTrustParameter =
    new BooleanParameter("blind_trust", "Blindly Trust Any Certificate",
                         "Indicates whether the client should blindly trust " +
                         "any certificate presented by the server, or " +
                         "whether the key and trust stores should be used.",
                         true);

  // The parameter used to indicate whether invalid credential results are
  // ignored.
  private BooleanParameter ignoreInvCredParameter =
       new BooleanParameter("ignore_49", "Ignore Invalid Credentials Errors",
                            "Indicates whether bind failures because of " +
                            "invalid credentials (err=49).  This makes it " +
                            "possible to use this job without actually " +
                            "know user passwords.", false);

  // The parameter used to indicate whether connections are shared.
  private BooleanParameter shareConnsParameter =
       new BooleanParameter("share_conns", "Share Connections between Threads",
                            "Indicates whether the connections to the " +
                            "directory server will be shared between threads " +
                            "or if each client thread will have its own " +
                            "connections.", true);

  // The parameter used to indicate whether to skip the bind operation.
  private BooleanParameter skipBindParameter =
       new BooleanParameter("skip_bind", "Skip Bind Operation",
                            "Indicates whether the bind attempt should be " +
                            "skipped as part of the authentication process.",
                            false);

  // The parameter that indicates whether the connection should use SSL or not
  private BooleanParameter useSSLParameter =
       new BooleanParameter("usessl", "Use SSL",
                            "Indicates whether to use SSL to encrypt the " +
                            "communication with the directory server", false);

  // The parameter that specifies the cool-down time in seconds.
  private IntegerParameter coolDownParameter =
       new IntegerParameter("cool_down", "Cool Down Time",
                            "The time in seconds that the job should " +
                            "continue searching after ending statistics " +
                            "collection.", true, 0, true, 0, false, 0);

  // The parameter that indicates the delay that should be used between each
  // authentication attempt.
  private IntegerParameter delayParameter =
       new IntegerParameter("delay", "Time Between Authentications (ms)",
                            "Specifies the length of time in milliseconds " +
                            "each thread should wait between authentication " +
                            "attempts.  Note that this delay will be " +
                            "between the starts of consecutive attempts and " +
                            "not between the end of one attempt and the " +
                            "beginning of the next.  If an authentication " +
                            "takes longer than this length of time, then " +
                            "there will be no delay.", true, 0, true, 0, false,
                            0);

  // The parameter that specifies the maximum request rate.
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Authentication Rate (Auths/Second/Client)",
       "Specifies the maximum authentication rate (in auths per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform authentications as quickly as possible.",
       true, -1);

  // The parameter used to indicate the port number for the directory server.
  private IntegerParameter portParameter =
       new IntegerParameter("ldap_port", "Directory Server Port",
                            "The port number for the directory server.", true,
                            389, true, 1, true, 65535);

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

  // The parameter used to indicate the maximum length of time that any single
  // LDAP operation will be allowed to take.
  private IntegerParameter timeLimitParameter =
       new IntegerParameter("time_limit", "Operation Time Limit",
                            "The maximum length of time in seconds that any " +
                            "single LDAP operation will be allowed to take " +
                            "before it is cancelled.", true, 0, true, 0, false,
                            0);

  // The parameter that specifies the cool-down time in seconds.
  private IntegerParameter warmUpParameter =
       new IntegerParameter("warm_up", "Warm Up Time",
                            "The time in seconds that the job should " +
                            "search before beginning statistics collection.",
                            true, 0, true, 0, false, 0);

  // The parameter that specifies the percentage of the time that the first user
  // criteria will be used in the modification.
  private IntegerParameter weightParameter =
       new IntegerParameter("weight", "Login ID 1 Percentage",
                            "The percentage of the time that a login ID " +
                            "should be selected according to the value " +
                            "provided for the Login ID Value 1 parameter.",
                            true, 50, true, 0, true, 100);

  // The parameter used to indicate the attributes to modify.
  private MultiLineTextParameter modAttrsParameter =
       new MultiLineTextParameter("mod_attrs", "Attributes to Modify",
                                  "The set of attributes to modify.",
                                  DEFAULT_ATTRS_TO_MODIFY, false);

  // The parameter used to indicate the password for the bind DN.
  private PasswordParameter bindPWParameter =
       new PasswordParameter("bindpw", "Directory Bind Password",
                             "The password to use when binding to the " +
                             "directory server to perform search and modify " +
                             "operations.", false, "");

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter keyPWParameter =
    new PasswordParameter("sslkeypw", "SSL Key Store Password",
                          "The password for the JSSE key store", false, "");

  // The parameter used to indicate the password to use when authenticating to
  // the directory.
  private PasswordParameter loginPasswordParameter =
       new PasswordParameter("login_id_pw", "Login Password",
                             "The password to use when authenticating to the " +
                             "directory for user authentications.", false, "");

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter trustPWParameter =
    new PasswordParameter("ssltrustpw", "SSL Trust Store Password",
                          "The password for the JSSE trust store", false, "");

  // The placeholder parameter used as a spacer in the admin interface.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter used to indicate the first attribute to retrieve.
  private StringParameter attr1Parameter =
       new StringParameter("attr1", "First Attribute to Retrieve",
                           "The first attribute to retrieve from the user's " +
                           "entry as part of the authentication process.",
                           true, DEFAULT_ATTR1);

  // The parameter used to indicate the first attribute to retrieve.
  private StringParameter attr2Parameter =
       new StringParameter("attr2", "Second Attribute to Retrieve",
                           "The second attribute to retrieve from the user's " +
                           "entry as part of the authentication process.",
                           true, DEFAULT_ATTR2);

  // The parameter used to indicate the first attribute to retrieve.
  private StringParameter attr3Parameter =
       new StringParameter("attr3", "Third Attribute to Retrieve",
                           "The third attribute to retrieve from the user's " +
                           "entry as part of the authentication process.",
                           true, DEFAULT_ATTR3);

  // The parameter used to indicate the bind DN.
  private StringParameter bindDNParameter =
       new StringParameter("binddn", "Directory Bind DN",
                           "The DN to use when binding to the directory " +
                           "server to perform search and modify operations.",
                           false, "");

  // The parameter used to indicate the address of the directory server.
  private StringParameter hostParameter =
       new StringParameter("ldap_host", "Directory Server Address",
                           "The address for the directory server.", true, "");

  // The parameter that specifies the location of the SSL key store
  private StringParameter keyStoreParameter =
    new StringParameter("sslkeystore", "SSL Key Store",
                        "The path to the JSSE key store to use for an " +
                        "SSL-based connection", false, "");

  // The parameter used to indicate the attribute to use for the login ID.
  private StringParameter loginIDParameter =
       new StringParameter("login_id_attr", "Login ID Attribute",
                           "The attribute to use as the login ID to find the " +
                           "user's entry.", true, DEFAULT_LOG_ID_ATTR);

  // The parameter used to indicate the first login ID value or value pattern.
  private StringParameter loginIDValue1Parameter =
       new StringParameter("login_id_value_1", "Login ID Value 1",
                           "The text to use as the value of the login ID " +
                           "attribute in search filters created using the " +
                           "first set of user criteria.  The value may " +
                           "contain a range of numbers in square brackets.",
                           true, "");

  // The parameter used to indicate the second login ID value or value pattern.
  private StringParameter loginIDValue2Parameter =
       new StringParameter("login_id_value_2", "Login ID Value 2",
                           "The text to use as the value of the login ID " +
                           "attribute in search filters created using the " +
                           "second set of user criteria.  The value may " +
                           "contain a range of numbers in square brackets.",
                           true, "");

  // The parameter used to indicate the search base for the directory.private
  private StringParameter searchBaseParameter =
    new StringParameter("search_base", "User Search Base",
                        "The DN in the directory server under which user " +
                        "entries may be found.", true, "");

  // The parameter that specifies the location of the SSL trust store
  private StringParameter trustStoreParameter =
    new StringParameter("ssltruststore", "SSL Trust Store",
                        "The path to the JSSE trust store to use for an " +
                        "SSL-based connection", false, "");

  // The stat tracker that will categorize the reasons for auth failures.
  private CategoricalTracker failureReasonTracker;

  // The stat trackers that will count the numbers of authentication attempts.
  private IncrementalTracker overallAttemptCounter;
  private IncrementalTracker attemptCounter1;
  private IncrementalTracker attemptCounter2;

  // The stat trackers that will count the numbers of failed authentications.
  private IncrementalTracker overallFailureCounter;
  private IncrementalTracker failureCounter1;
  private IncrementalTracker failureCounter2;

  // The stat trackers that will count the numbers of successful
  // authentications.
  private IncrementalTracker overallSuccessCounter;
  private IncrementalTracker successCounter1;
  private IncrementalTracker successCounter2;

  // The LDAP connection that will be used for authentication operations by this
  // thread.
  private LDAPConnection authConnection;

  // The LDAP connection that will be used for bind operations by this thread.
  private LDAPConnection bindConnection;

  // The random number generator for this thread.
  private Random random;

  // The stat trackers that will time each authentication.
  private TimeTracker overallAuthTimer;
  private TimeTracker authTimer1;
  private TimeTracker authTimer2;



  /**
   * Creates a new instance of this job thread.  This constructor does not need
   * to do anything other than invoke the constructor for the superclass.
   */
  public WeightedSiteMinderJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Weighted SiteMinder Load Simulator";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Process a sequence of LDAP search, bind, and update operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to simulate the load observed when using some " +
      "versions of Netegrity SiteMinder.  That load consists of eight " +
      "search, one bind, and one modify operation."
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
    Parameter[] parameterArray = new Parameter[]
    {
      placeholder,
      hostParameter,
      portParameter,
      bindDNParameter,
      bindPWParameter,
      placeholder,
      searchBaseParameter,
      loginIDValue1Parameter,
      loginIDValue2Parameter,
      weightParameter,
      loginPasswordParameter,
      loginIDParameter,
      placeholder,
      attr1Parameter,
      attr2Parameter,
      attr3Parameter,
      modAttrsParameter,
      placeholder,
      warmUpParameter,
      coolDownParameter,
      timeLimitParameter,
      delayParameter,
      maxRateParameter,
      rateLimitDurationParameter,
      placeholder,
      useSSLParameter,
      blindTrustParameter,
      keyStoreParameter,
      keyPWParameter,
      trustStoreParameter,
      trustPWParameter,
      placeholder,
      skipBindParameter,
      ignoreInvCredParameter,
      shareConnsParameter
    };

    return new ParameterList(parameterArray);
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
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_OVERALL_AUTHENTICATION_ATTEMPTS,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_AUTHENTICATION_1_ATTEMPTS,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_AUTHENTICATION_2_ATTEMPTS,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_OVERALL_SUCCESSFUL_AUTHENTICATIONS,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS_1,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS_2,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_OVERALL_FAILED_AUTHENTICATIONS,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_FAILED_AUTHENTICATIONS_1,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_FAILED_AUTHENTICATIONS_2,
                             collectionInterval),
      new TimeTracker(clientID, threadID,
                      STAT_TRACKER_OVERALL_AUTHENTICATION_TIME,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_AUTHENTICATION_1_TIME,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_AUTHENTICATION_2_TIME,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_TRACKER_FAIL_REASON,
                             collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[]
    {
      overallAttemptCounter,
      attemptCounter1,
      attemptCounter2,
      overallSuccessCounter,
      successCounter1,
      successCounter2,
      overallFailureCounter,
      failureCounter1,
      failureCounter2,
      overallAuthTimer,
      authTimer1,
      authTimer2,
      failureReasonTracker
    };
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
    // The login ID value parameters must be parseable as value patterns.
    StringParameter p =
         parameters.getStringParameter(loginIDValue1Parameter.getName());
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

    p = parameters.getStringParameter(loginIDValue2Parameter.getName());
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
         parameters.getStringParameter(hostParameter.getName());
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
         parameters.getPasswordParameter(bindPWParameter.getName());
    if ((bindPWParam != null) && bindPWParam.hasValue())
    {
      bindPassword = bindPWParam.getStringValue();
    }


    StringParameter baseDNParam =
         parameters.getStringParameter(searchBaseParameter.getName());
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
      outputMessages.add("Attempting to perform an LDAPv3 bind to the " +
                         "directory server with a DN of '" + bindDN + "'....");
      conn.bind(bindDN, bindPassword);
      outputMessages.add("Bound successfully.");
      outputMessages.add("");
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
    // Seed the parent random number generator.
    parentRandom = new Random();


    // Get the directory server address
    hostParameter = parameters.getStringParameter(hostParameter.getName());
    if (hostParameter == null)
    {
      throw new UnableToRunException("No directory server host provided.");
    }
    else
    {
      directoryHost = hostParameter.getStringValue();
    }


    // Get the directory server port
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      directoryPort = portParameter.getIntValue();
    }

    // Get the DN to use to bind to the directory server.
    bindDNParameter = parameters.getStringParameter(bindDNParameter.getName());
    if (bindDNParameter == null)
    {
      bindDN = "";
    }
    else
    {
      bindDN = bindDNParameter.getStringValue();
    }

    // Get the password to use to bind to the directory server.
    bindPWParameter =
         parameters.getPasswordParameter(bindPWParameter.getName());
    if (bindPWParameter == null)
    {
      bindPW = "";
    }
    else
    {
      bindPW = bindPWParameter.getStringValue();
    }

    // Get the search base
    searchBaseParameter =
         parameters.getStringParameter(searchBaseParameter.getName());
    if (searchBaseParameter != null)
    {
      searchBase = searchBaseParameter.getStringValue();
    }


    // Get the password to use when authenticating to the directory.
    loginPasswordParameter =
         parameters.getPasswordParameter(loginPasswordParameter.getName());
    if ((loginPasswordParameter != null) &&
          (loginPasswordParameter.hasValue()))
    {
      loginPassword = loginPasswordParameter.getStringValue();
    }


    // Get the login ID value for the first user criteria.
    loginIDValue1Parameter =
         parameters.getStringParameter(loginIDValue1Parameter.getName());
    try
    {
      loginIDPattern1 =
           new ValuePattern(loginIDValue1Parameter.getStringValue());
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Cannot parse login ID pattern 1:  " +
                                     stackTraceToString(e), e);
    }


    // Get the login ID value for the first user criteria.
    loginIDValue2Parameter =
         parameters.getStringParameter(loginIDValue2Parameter.getName());
    try
    {
      loginIDPattern2 =
           new ValuePattern(loginIDValue2Parameter.getStringValue());
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Cannot parse login ID pattern 2:  " +
                                     stackTraceToString(e), e);
    }


    // Get the weight.
    weightParameter = parameters.getIntegerParameter(weightParameter.getName());
    if ((weightParameter != null) && (weightParameter.hasValue()))
    {
      loginID1Percent = weightParameter.getIntValue();
    }


    // Get the login ID attribute.
    loginIDParameter =
         parameters.getStringParameter(loginIDParameter.getName());
    if (loginIDParameter != null)
    {
      loginIDAttr = loginIDParameter.getStringValue();
    }


    // Get the attributes to retrieve.
    attr1Parameter = parameters.getStringParameter(attr1Parameter.getName());
    if (attr1Parameter != null)
    {
      searchAttr1  = attr1Parameter.getStringValue();
      filter1      = Filter.createPresenceFilter(searchAttr1);
      returnAttrs1 = new String[] { searchAttr1 };
    }

    attr2Parameter = parameters.getStringParameter(attr2Parameter.getName());
    if (attr2Parameter != null)
    {
      searchAttr2  = attr2Parameter.getStringValue();
      filter2      = Filter.createPresenceFilter(searchAttr2);
      returnAttrs2 = new String[] { searchAttr2 };
    }

    attr3Parameter = parameters.getStringParameter(attr3Parameter.getName());
    if (attr3Parameter != null)
    {
      searchAttr3  = attr3Parameter.getStringValue();
      filter3      = Filter.createPresenceFilter(searchAttr3);
      returnAttrs3 = new String[] { searchAttr3 };
    }

    returnAttrsOC = new String[] { "objectClass" };


    // Get the attributes to modify.
    modAttrs = null;
    modAttrsParameter =
         parameters.getMultiLineTextParameter(modAttrsParameter.getName());
    if ((modAttrsParameter != null) && (modAttrsParameter.hasValue()))
    {
      modAttrs = modAttrsParameter.getNonBlankLines();
    }

    // Determine whether to skip the bind attempt.
    skipBind = false;
    skipBindParameter =
         parameters.getBooleanParameter(skipBindParameter.getName());
    if (skipBindParameter != null)
    {
      skipBind = skipBindParameter.getBooleanValue();
    }

    // Get the warm up time.
    warmUpTime = 0;
    warmUpParameter = parameters.getIntegerParameter(warmUpParameter.getName());
    if (warmUpParameter != null)
    {
      warmUpTime = warmUpParameter.getIntValue();
    }

    // Get the cool down time.
    coolDownTime = 0;
    coolDownParameter =
         parameters.getIntegerParameter(coolDownParameter.getName());
    if (coolDownParameter != null)
    {
      coolDownTime = coolDownParameter.getIntValue();
    }

    // Get the max operation time limit.
    timeLimitParameter =
         parameters.getIntegerParameter(timeLimitParameter.getName());
    if (timeLimitParameter != null)
    {
      timeLimit = timeLimitParameter.getIntValue();
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

    // Get the delay between authentication attempts.
    delay = 0;
    delayParameter = parameters.getIntegerParameter(delayParameter.getName());
    if (delayParameter != null)
    {
      delay = delayParameter.getIntValue();
    }

    // Get the flag indicating whether we should use SSL or not
    useSSL = false;
    useSSLParameter = parameters.getBooleanParameter(useSSLParameter.getName());
    if (useSSLParameter != null)
    {
      useSSL = useSSLParameter.getBooleanValue();
    }


    // Get the indicator that specifies whether to ignore invalid credentials
    // errors.
    ignoreInvCredParameter =
         parameters.getBooleanParameter(ignoreInvCredParameter.getName());
    if (ignoreInvCredParameter != null)
    {
      ignoreInvalidCredentials = ignoreInvCredParameter.getBooleanValue();
    }

    // Get the indicator that specifies whether to use shared connections.
    shareConnsParameter =
         parameters.getBooleanParameter(shareConnsParameter.getName());
    if (shareConnsParameter != null)
    {
      useSharedConnections = shareConnsParameter.getBooleanValue();
    }


    // If we are to use shared connections, then establish them now.
    if (useSharedConnections)
    {
      if (useSSL)
      {
        try
        {
          SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
          sharedAuthConnection =
               new LDAPConnection(sslUtil.createSSLSocketFactory());
          sharedBindConnection =
               new LDAPConnection(sslUtil.createSSLSocketFactory());
        }
        catch (Exception e)
        {
          throw new UnableToRunException(String.valueOf(e), e);
        }
      }
      else
      {
        sharedAuthConnection = new LDAPConnection();
        sharedBindConnection = new LDAPConnection();
      }

      try
      {
        sharedAuthConnection.connect(directoryHost, directoryPort, 10000);
        sharedAuthConnection.bind(bindDN, bindPW);

        sharedBindConnection.connect(directoryHost, directoryPort, 10000);
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Could not establish shared " +
                                       "connections to the directory:  " + e,
                                       e);
      }

      LDAPConnectionOptions opts = sharedAuthConnection.getConnectionOptions();
      opts.setResponseTimeoutMillis(1000L * timeLimit);
      sharedAuthConnection.setConnectionOptions(opts);
      sharedBindConnection.setConnectionOptions(opts);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // Seed the random number generator for this thread.
    random = new Random(parentRandom.nextLong());

    // If we are not going to use shared connections, then create the
    // connections for use by this thread.  Otherwise, just grab the shared
    // connections.
    if (useSharedConnections)
    {
      authConnection = sharedAuthConnection;
      bindConnection = sharedBindConnection;
    }
    else
    {
      if (useSSL)
      {
        try
        {
          SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

          authConnection =
               new LDAPConnection(sslUtil.createSSLSocketFactory());
          bindConnection =
               new LDAPConnection(sslUtil.createSSLSocketFactory());
        }
        catch (Exception e)
        {
          throw new UnableToRunException(String.valueOf(e), e);
        }
      }
      else
      {
        authConnection = new LDAPConnection();
        bindConnection = new LDAPConnection();
      }

      try
      {
        authConnection.connect(directoryHost, directoryPort, 10000);
        authConnection.bind(bindDN, bindPW);

        bindConnection.connect(directoryHost, directoryPort, 10000);
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to establish the connections " +
                                       "to the directory server:  " + e, e);
      }

      LDAPConnectionOptions opts = authConnection.getConnectionOptions();
      opts.setResponseTimeoutMillis(1000L * timeLimit);
      authConnection.setConnectionOptions(opts);
      bindConnection.setConnectionOptions(opts);
    }


    // Create the stat trackers.
    overallAttemptCounter =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_OVERALL_AUTHENTICATION_ATTEMPTS,
                                collectionInterval);
    attemptCounter1 =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_AUTHENTICATION_1_ATTEMPTS,
                                collectionInterval);
    attemptCounter2 =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_AUTHENTICATION_2_ATTEMPTS,
                                collectionInterval);
    overallSuccessCounter =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_OVERALL_SUCCESSFUL_AUTHENTICATIONS,
                                collectionInterval);
    successCounter1 =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS_1,
                                collectionInterval);
    successCounter2 =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS_2,
                                collectionInterval);
    overallFailureCounter =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_OVERALL_FAILED_AUTHENTICATIONS,
                                collectionInterval);
    failureCounter1 =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_FAILED_AUTHENTICATIONS_1,
                                collectionInterval);
    failureCounter2 =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_FAILED_AUTHENTICATIONS_2,
                                collectionInterval);
    overallAuthTimer = new TimeTracker(clientID, threadID,
                                       STAT_TRACKER_OVERALL_AUTHENTICATION_TIME,
                                       collectionInterval);
    authTimer1 = new TimeTracker(clientID, threadID,
                                 STAT_TRACKER_AUTHENTICATION_1_TIME,
                                 collectionInterval);
    authTimer2 = new TimeTracker(clientID, threadID,
                                 STAT_TRACKER_AUTHENTICATION_2_TIME,
                                 collectionInterval);
    failureReasonTracker = new CategoricalTracker(clientID, threadID,
                                                  STAT_TRACKER_FAIL_REASON,
                                                  collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      overallAttemptCounter.enableRealTimeStats(statReporter, jobID);
      attemptCounter1.enableRealTimeStats(statReporter, jobID);
      attemptCounter2.enableRealTimeStats(statReporter, jobID);
      overallSuccessCounter.enableRealTimeStats(statReporter, jobID);
      successCounter1.enableRealTimeStats(statReporter, jobID);
      successCounter2.enableRealTimeStats(statReporter, jobID);
      overallFailureCounter.enableRealTimeStats(statReporter, jobID);
      failureCounter1.enableRealTimeStats(statReporter, jobID);
      failureCounter2.enableRealTimeStats(statReporter, jobID);
      overallAuthTimer.enableRealTimeStats(statReporter, jobID);
      authTimer1.enableRealTimeStats(statReporter, jobID);
      authTimer2.enableRealTimeStats(statReporter, jobID);
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
    boolean collectingStats = false;
    long startCollectingTime = currentTime + (1000 * warmUpTime);
    long stopCollectingTime  = Long.MAX_VALUE;
    if ((coolDownTime > 0) && (getShouldStopTime() > 0))
    {
      stopCollectingTime = getShouldStopTime() - (1000 * coolDownTime);
    }

    // Define a variable that will be used to determine how long to sleep
    // between attempts.
    long authStartTime = 0;


    // Loop until it is time to stop.
    while (! shouldStop())
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      currentTime = System.currentTimeMillis();
      if ((! collectingStats) && (currentTime >= startCollectingTime) &&
          (currentTime < stopCollectingTime))
      {
        // Start all the stat trackers.
        overallAttemptCounter.startTracker();
        attemptCounter1.startTracker();
        attemptCounter2.startTracker();
        overallSuccessCounter.startTracker();
        successCounter1.startTracker();
        successCounter2.startTracker();
        overallFailureCounter.startTracker();
        failureCounter1.startTracker();
        failureCounter2.startTracker();
        overallAuthTimer.startTracker();
        authTimer1.startTracker();
        authTimer2.startTracker();
        failureReasonTracker.startTracker();
        collectingStats = true;
      }
      else if ((collectingStats) && (currentTime >= stopCollectingTime))
      {
        overallAttemptCounter.stopTracker();
        attemptCounter1.stopTracker();
        attemptCounter2.stopTracker();
        overallSuccessCounter.stopTracker();
        successCounter1.stopTracker();
        successCounter2.stopTracker();
        overallFailureCounter.stopTracker();
        failureCounter1.stopTracker();
        failureCounter2.stopTracker();
        overallAuthTimer.stopTracker();
        authTimer1.stopTracker();
        authTimer2.stopTracker();
        failureReasonTracker.stopTracker();
        collectingStats = false;
      }

      // See if we need to sleep before the next attempt
      if ((delay > 0) && (authStartTime > 0))
      {
        long now = System.currentTimeMillis();
        long sleepTime = delay - (now - authStartTime);
        if (sleepTime > 0)
        {
          try
          {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {}

          if (shouldStop())
          {
            break;
          }
        }
      }


      // Get a random number between 1 and 100 to use to determine which user
      // criteria to use to attempt the authentication.
      int weight = ((random.nextInt() & 0x7FFFFFFF) % 100);
      if (weight < loginID1Percent)
      {
        // Get a random user number from the first range and translate that to a
        // login ID and password.
        String[] loginInfo = getLoginInfo1();
        String   loginID   = loginInfo[0];
        String   password  = loginInfo[1];


        // Start the auth attempt timer now.
        if (delay > 0)
        {
          authStartTime = System.currentTimeMillis();
        }


        // Increment the number of authentication attempts and start the timer
        if (collectingStats)
        {
          overallAttemptCounter.increment();
          attemptCounter1.increment();
          overallAuthTimer.startTimer();
          authTimer1.startTimer();
        }

        String failureReason = "Search 1";

        try
        {
          // First, issue a search to try to find the user's entry.
          String userDN = null;
          Filter filter = Filter.createEqualityFilter(loginIDAttr, loginID);
          SearchResult searchResult = authConnection.search(searchBase,
               SearchScope.SUB, filter, returnAttrsOC);
          userDN = searchResult.getSearchEntries().get(0).getDN();

          // Make sure that we got a user DN.  If not, then it's a failed
          // attempt.
          if (userDN == null)
          {
            if (collectingStats)
            {
              overallFailureCounter.increment();
              failureCounter1.increment();
              overallAuthTimer.stopTimer();
              authTimer1.stopTimer();
              failureReasonTracker.increment(failureReason);
            }
            continue;
          }

          // Now do a base-level search on the user's entry to retrieve the
          // objectClass attribute.
          failureReason = "Search 2";
          filter = Filter.createPresenceFilter("objectClass");
          authConnection.search(userDN, SearchScope.BASE, filter,
               returnAttrsOC);

          // Now bind as the user.
          if (! skipBind)
          {
            failureReason = "Bind";

            try
            {
              bindConnection.bind(userDN, password);
            }
            catch (LDAPException le)
            {
              if (le.getResultCode() == ResultCode.INVALID_CREDENTIALS)
              {
                if (! ignoreInvalidCredentials)
                {
                  if (collectingStats)
                  {
                    overallFailureCounter.increment();
                    failureCounter1.increment();
                    overallAuthTimer.stopTimer();
                    authTimer1.stopTimer();
                    failureReasonTracker.increment(failureReason);
                  }
                  continue;
                }
              }
              else
              {
                if (collectingStats)
                {
                  overallFailureCounter.increment();
                  failureCounter1.increment();
                  overallAuthTimer.stopTimer();
                  authTimer1.stopTimer();
                  failureReasonTracker.increment(failureReason);
                }
                continue;
              }
            }
          }

          // Now retrieve the "attr1" attribute from the user's entry.
          failureReason = "Search 3";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Retrieve the "attr2" attribute from the user's entry.
          failureReason = "Search 4";
          authConnection.search(userDN, SearchScope.BASE, filter2,
               returnAttrs2);

          // Retrieve the first attribute again.
          failureReason = "Search 5";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Perform a modification on the entry
          if ((modAttrs != null) && (modAttrs.length > 0))
          {
            failureReason = "Modify";
            Modification[] mods = new Modification[modAttrs.length];
            for (int i=0; i < modAttrs.length; i++)
            {
              mods[i] = new Modification(ModificationType.REPLACE, modAttrs[i],
                   getRandomString(80));
            }
            authConnection.modify(userDN, mods);
          }

          // Retrieve the first attribute again.
          failureReason = "Search 6";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Retrieve the first attribute again.
          failureReason = "Search 7";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Retrieve the third attribute.
          failureReason = "Search 8";
          authConnection.search(userDN, SearchScope.BASE, filter3,
               returnAttrs3);
        }
        catch (Exception e)
        {

          if (collectingStats)
          {
            overallFailureCounter.increment();
            failureCounter1.increment();
            overallAuthTimer.stopTimer();
            authTimer1.stopTimer();
            failureReasonTracker.increment(failureReason);
          }
          continue;
        }

        // If we have gotten here, then everything is done and we can consider
        // the authentication successful.
        if (collectingStats)
        {
          overallSuccessCounter.increment();
          successCounter1.increment();
          overallAuthTimer.stopTimer();
          authTimer1.stopTimer();
        }
      }
      else
      {
        // Get a random user number from the second range and translate that to
        // a login ID and password.
        String[] loginInfo = getLoginInfo2();
        String loginID  = loginInfo[0];
        String password = loginInfo[1];


        // Start the auth attempt timer now.
        if (delay > 0)
        {
          authStartTime = System.currentTimeMillis();
        }


        // Increment the number of authentication attempts and start the timer
        if (collectingStats)
        {
          overallAttemptCounter.increment();
          attemptCounter2.increment();
          overallAuthTimer.startTimer();
          authTimer2.startTimer();
        }

        String failureReason = "Search 1";

        try
        {
          // First, issue a search to try to find the user's entry.
          String userDN = null;
          Filter filter = Filter.createEqualityFilter(loginIDAttr, loginID);
          SearchResult searchResult = authConnection.search(searchBase,
               SearchScope.SUB, filter, returnAttrsOC);
          userDN = searchResult.getSearchEntries().get(0).getDN();

          // Make sure that we got a user DN.  If not, then it's a failed
          // attempt.
          if (userDN == null)
          {
            if (collectingStats)
            {
              overallFailureCounter.increment();
              failureCounter2.increment();
              overallAuthTimer.stopTimer();
              authTimer2.stopTimer();
              failureReasonTracker.increment(failureReason);
            }
            continue;
          }

          // Now do a base-level search on the user's entry to retrieve the
          // objectClass attribute.
          failureReason = "Search 2";
          filter = Filter.createPresenceFilter("objectClass");
          authConnection.search(userDN, SearchScope.BASE, filter,
               returnAttrsOC);

          // Now bind as the user.
          if (! skipBind)
          {
            failureReason = "Bind";

            try
            {
              bindConnection.bind(userDN, password);
            }
            catch (LDAPException le)
            {
              if (le.getResultCode() == ResultCode.INVALID_CREDENTIALS)
              {
                if (! ignoreInvalidCredentials)
                {
                  if (collectingStats)
                  {
                    overallFailureCounter.increment();
                    failureCounter2.increment();
                    overallAuthTimer.stopTimer();
                    authTimer2.stopTimer();
                    failureReasonTracker.increment(failureReason);
                  }
                  continue;
                }
              }
              else
              {
                if (collectingStats)
                {
                  overallFailureCounter.increment();
                  failureCounter2.increment();
                  overallAuthTimer.stopTimer();
                  authTimer2.stopTimer();
                  failureReasonTracker.increment(failureReason);
                }
                continue;
              }
            }
          }

          // Now retrieve the "attr1" attribute from the user's entry.
          failureReason = "Search 3";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Retrieve the "attr2" attribute from the user's entry.
          failureReason = "Search 4";
          authConnection.search(userDN, SearchScope.BASE, filter2,
               returnAttrs2);

          // Retrieve the first attribute again.
          failureReason = "Search 5";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Perform a modification on the entry
          if ((modAttrs != null) && (modAttrs.length > 0))
          {
            failureReason = "Modify";
            Modification[] mods = new Modification[modAttrs.length];
            for (int i=0; i < modAttrs.length; i++)
            {
              mods[i] = new Modification(ModificationType.REPLACE, modAttrs[i],
                   getRandomString(80));
            }
            authConnection.modify(userDN, mods);
          }

          // Retrieve the first attribute again.
          failureReason = "Search 6";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Retrieve the first attribute again.
          failureReason = "Search 7";
          authConnection.search(userDN, SearchScope.BASE, filter1,
               returnAttrs1);

          // Retrieve the third attribute.
          failureReason = "Search 8";
          authConnection.search(userDN, SearchScope.BASE, filter3,
               returnAttrs3);
        }
        catch (Exception e)
        {

          if (collectingStats)
          {
            overallFailureCounter.increment();
            failureCounter2.increment();
            overallAuthTimer.stopTimer();
            authTimer2.stopTimer();
            failureReasonTracker.increment(failureReason);
          }
          continue;
        }

        // If we have gotten here, then everything is done and we can consider
        // the authentication successful.
        if (collectingStats)
        {
          overallSuccessCounter.increment();
          successCounter2.increment();
          overallAuthTimer.stopTimer();
          authTimer2.stopTimer();
        }
      }
    }


    // Stop all the stat trackers.
    if (collectingStats)
    {
      overallAttemptCounter.stopTracker();
      attemptCounter1.stopTracker();
      attemptCounter2.stopTracker();
      overallSuccessCounter.stopTracker();
      successCounter1.stopTracker();
      successCounter2.stopTracker();
      overallFailureCounter.stopTracker();
      failureCounter1.stopTracker();
      failureCounter2.stopTracker();
      overallAuthTimer.stopTracker();
      authTimer1.stopTracker();
      authTimer2.stopTracker();
      failureReasonTracker.stopTracker();
    }


    // Close the connections to the directory server if appropriate.
    if (! useSharedConnections)
    {
      try
      {
        authConnection.close();
      } catch (Exception e) {}

      try
      {
        bindConnection.close();
      } catch (Exception e) {}
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    if (authConnection != null)
    {
      try
      {
        authConnection.close();
      } catch (Exception e) {}

      authConnection = null;
    }

    if (bindConnection != null)
    {
      try
      {
        bindConnection.close();
      } catch (Exception e) {}

      bindConnection = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void finalizeClient()
  {
    // Make sure that the shared connections get closed properly.
    if (useSharedConnections)
    {
      try
      {
        sharedAuthConnection.close();
      } catch (Exception e) {}

      try
      {
        sharedBindConnection.close();
      } catch (Exception e) {}
    }
  }



  /**
   * Retrieves an array containing the login ID and password that should be
   * used to authenticate to the directory based on the first set of user
   * criteria.
   *
   * @return  An array containing the login ID and password.
   */
  private static String[] getLoginInfo1()
  {
    return new String[]
    {
      loginIDPattern1.nextValue(),
      loginPassword
    };
  }



  /**
   * Retrieves an array containing the login ID and password that should be
   * used to authenticate to the directory based on the second set of user
   * criteria.
   *
   * @return  An array containing the login ID and password.
   */
  private static String[] getLoginInfo2()
  {
    return new String[]
    {
      loginIDPattern2.nextValue(),
      loginPassword
    };
  }



  /**
   * Retrieves a string of random characters of the specified length.
   *
   * @param  length  The number of characters to include in the string.
   *
   * @return  The generated string of random characters.
   */
  private String getRandomString(int length)
  {
    char[] returnArray = new char[length];

    for (int i=0; i < returnArray.length; i++)
    {
      returnArray[i] = ALPHABET[Math.abs((random.nextInt()) & 0x7FFFFFFF) %
                                ALPHABET.length];
    }

    return new String(returnArray);
  }
}

