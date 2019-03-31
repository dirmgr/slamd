/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2008-2019.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs.ldap;



import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.controls.AuthorizationIdentityRequestControl;
import com.unboundid.ldap.sdk.controls.ManageDsaITRequestControl;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ObjectPair;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ValuePattern;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.slamd.common.DurationParser;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
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



/**
 * This class provides a SLAMD job that can measure the authentication
 * performance (where an authentication attempt consists of a search to find a
 * user followed by a bind to verify the user's credentials) of an LDAP
 * directory server with a more comprehensive set of options than the
 * {@link BasicSearchAndBindRateJob} job offers.
 */
public final class ComprehensiveSearchAndBindRateJob
       extends JobClass
{
  /**
   * The display name for the stat tracker used to track authentications
   * completed.
   */
  private static final String STAT_AUTHS_COMPLETED =
       "Authentications Completed";



  /**
   * The display name for the stat tracker used to track authentication
   * durations.
   */
  private static final String STAT_AUTH_DURATION =
       "Authentication Duration (ms)";



  /**
   * The display name for the stat tracker used to track search durations.
   */
  private static final String STAT_SEARCH_DURATION = "Search Duration (ms)";



  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_SEARCH_RESULT_CODES = "Search Result Codes";



  /**
   * The display name for the stat tracker used to track bind durations.
   */
  private static final String STAT_BIND_DURATION = "Bind Duration (ms)";



  /**
   * The display name for the stat tracker used to track bind result codes.
   */
  private static final String STAT_BIND_RESULT_CODES = "Bind Result Codes";



  // The parameter used to provide a label for the connection details.
  private LabelParameter connectionLabelParameter = new LabelParameter(
       "Connection Parameters");

  // The parameter used to specify the directory server address.
  private MultiLineTextParameter addressesAndPortsParameter =
       new MultiLineTextParameter("addresses_and_ports",
            "Server Addresses and Ports",
            "The addresses (resolvable names or IP addresses) and port " +
                 "numbers of the directory servers in which to process the " +
                 "search and bind operations (for example, " +
                 "'ldap.example.com:389').  If multiple servers are to be " +
                 "targeted, then each address:port value should be listed on " +
                 "a separate line, and the job will use a round-robin " +
                 "algorithm when establishing connections to each server.",
            new String[0], true);

  // The mechanism to use to secure communication with the directory server.
  private static final String SECURITY_METHOD_NONE = "None";
  private static final String SECURITY_METHOD_SSL = "SSL";
  private static final String SECURITY_METHOD_START_TLS = "StartTLS";
  private MultiChoiceParameter securityMethodParameter =
       new MultiChoiceParameter("security_method", "Security Method",
            "The mechanism to use to secure communication with the " +
                 "directory server.",
            new String[]
            {
              SECURITY_METHOD_NONE,
              SECURITY_METHOD_SSL,
              SECURITY_METHOD_START_TLS
            },
            SECURITY_METHOD_NONE);

  // The parameter used to provide a label for the authentication details.
  private LabelParameter authenticationLabelParameter = new LabelParameter(
       "Search User Authentication Parameters");

  // The DN to use to bind to the directory server when processing searches.
  private StringParameter searchUserBindDNParameter = new StringParameter(
       "search_user_bind_dn", "Search User Bind DN",
       "The DN of the user as whom to bind to the directory server when " +
            "searching for users to authenticate.  If this is not provided, " +
            "then the search user bind password must also be empty, and the " +
            "searches will be performed over unauthenticated connections.",
       false, null);

  // The password to use to bind to the directory server.
  private PasswordParameter searchUserBindPasswordParameter =
       new PasswordParameter("search_user_bind_password",
            "Search User Bind Password",
            "The password of the user as whom to bind to the directory " +
                 "server when searching for users to authenticate.  If this " +
                 "is not provided, then the search user bind DN must also " +
                 "be empty, and the searches will be performed over " +
                 "unauthenticated connections.",
            false, null);

  // The parameter used to provide a label for the search request details.
  private LabelParameter searchRequestLabelParameter = new LabelParameter(
       "Search Request Parameters");

  // The parameter used to specify the search base DN pattern.
  private StringParameter baseDNPatternParameter = new StringParameter(
       "base_dn_pattern", "Search Base DN Pattern",
       "A pattern to use to generate the base DN to use for each search " +
            "request.  This may be a fixed DN to use for all searches, or it " +
            "may be a value pattern that can construct a different base DN " +
            "for each request.  See https://docs.ldap.com/ldap-sdk/docs/" +
            "javadoc/com/unboundid/util/ValuePattern.html for more " +
            "information about value patterns.  If no value is specified, " +
            "then the null DN will be used as the search base.",
       false, null);

  // The parameter used to specify the search scope.
  private static final String SCOPE_BASE_OBJECT =
       "baseObject:  Only the Search Base Entry";
  private static final String SCOPE_SINGLE_LEVEL =
       "singleLevel:  Only Immediate Subordinates of the Search Base";
  private static final String SCOPE_WHOLE_SUBTREE =
       "Whole Subtree:  The Search Base Entry and All Its Subordinates";
  private static final String SCOPE_SUBORDINATE_SUBTREE =
       "Subordinate Subtree:  All Subordinates of the Search Base Entry";
  private MultiChoiceParameter scopeParameter = new MultiChoiceParameter(
       "scope", "Search Scope",
       "The scope, relative to the search base DN, of entries that may be " +
            "returned by the search.",
       new String[]
       {
         SCOPE_BASE_OBJECT,
         SCOPE_SINGLE_LEVEL,
         SCOPE_WHOLE_SUBTREE,
         SCOPE_SUBORDINATE_SUBTREE
       },
       SCOPE_BASE_OBJECT);

  // The parameter used to specify the alias dereferencing policy.
  private static final String DEREF_NEVER =
       "Never Dereference Aliases";
  private static final String DEREF_SEARCHING =
       "Dereference Aliases Below the Search Base";
  private static final String DEREF_FINDING_BASE =
       "Dereference the Search Base Entry if It Is an Alias";
  private static final String DEREF_ALWAYS =
       "Dereference All Aliases";
  private MultiChoiceParameter derefPolicyParameter = new MultiChoiceParameter(
       "deref_policy", "Alias Dereferencing Policy",
       "The dereferencing policy to use for any aliases encountered in " +
            "search processing.",
       new String[]
       {
         DEREF_NEVER,
         DEREF_SEARCHING,
         DEREF_FINDING_BASE,
         DEREF_ALWAYS,
       },
       DEREF_NEVER);

  // The parameter used to specify the time limit.
  private IntegerParameter timeLimitParameter = new IntegerParameter(
       "time_limit_seconds", "Search Time Limit (Seconds)",
       "The maximum length of time, in seconds, that the server should spend " +
            "processing any individual search request.",
       true, 0, true, 0, false, Integer.MAX_VALUE);

  // The parameter used to specify the first search filter pattern.
  private StringParameter filterPattern1Parameter = new StringParameter(
       "filter_pattern_1", "Search Filter Pattern 1",
       "A pattern to use to generate the first filter to use for search " +
            "requests.  This may be a fixed filter to use for all searches, " +
            "or it may be a value pattern that can construct a different " +
            "filter for each request.  See https://docs.ldap.com/ldap-sdk/" +
            "docs/javadoc/com/unboundid/util/ValuePattern.html for more " +
            "information about value patterns.  This parameter always " +
            "requires a value.",
       true, null);

  // The parameter used to specify the second search filter pattern.
  private StringParameter filterPattern2Parameter = new StringParameter(
       "filter_pattern_2", "Search Filter Pattern 2",
       "A pattern to use to generate the first filter to use for search " +
            "requests.  This may be a fixed filter to use for all searches, " +
            "or it may be a value pattern that can construct a different " +
            "filter for each request.  See https://docs.ldap.com/ldap-sdk/" +
            "docs/javadoc/com/unboundid/util/ValuePattern.html for more " +
            "information about value patterns.  This parameter requires a " +
            "value if the Filter 1 Percentage value is less than 100.",
       false, null);

  // The parameter used to specify the percentage of searches that should use
  // the first pattern.
  private IntegerParameter filter1PercentageParameter = new IntegerParameter(
       "filter_1_percent", "Filter 1 Percentage",
       "The percentage of searches that should use a filter generated from " +
            "the first pattern rather than the second pattern.",
       true, 100, true, 0, true, 100);

  // The parameter used to specify the attributes to return.
  private MultiLineTextParameter attributesToReturnParameter =
       new MultiLineTextParameter("attributes_to_return",
            "Attributes to Return",
            "The names of the attributes to request that the server return " +
                 "in search result entries.  Multiple values may be " +
                 "provided, with one value per line.  Each value may be the " +
                 "name or OID of an LDAP attribute type, '*' (to request " +
                 "that the server return all user attributes), '+' (to " +
                 "request that the server return all operational " +
                 "attributes), '1.1' (to request that the server not " +
                 "return any attributes), or another type of value that the " +
                 "server may support.  If no values are provided, then the " +
                 "standard behavior is for the server to return all user " +
                 "attributes that the requester has permission to access.",
            new String[0], false);

  private BooleanParameter includeManageDSAITParameter = new BooleanParameter(
       "include_manage_dsa_it_control", "Include Manage DSA IT Request Control",
       "Indicates that search requests should include the manage DSA IT " +
            "request control, which indicates that the server should return " +
            "smart referral entries as regular entries rather than search " +
            "result references.",
       false);

  private StringParameter proxiedAuthzIDParameter = new StringParameter(
       "proxied_auth_id", "Proxied Authorization ID",
       "Indicates that search requests should include a proxied " +
            "authorization request control with the specified authorization " +
            "ID.  Note that this should be a static authorization ID string " +
            "rather than a value pattern.",
       false, null);

  // The parameter used to provide a label for the bind request parameters.
  private LabelParameter bindRequestLabelParameter = new LabelParameter(
       "Bind Request Parameters");

  // The parameter that specifies the password to authenticate found users.
  private PasswordParameter userPasswordParameter = new PasswordParameter(
       "user_authentication_password", "User Authentication Password",
       "The password to include in the simple bind request used to " +
            "authenticate a user identified by searching.  Either a user " +
            "authentication password or a user authentication password " +
            "attribute must be provided.",
       false, null);

  // The parameter that specifies the name of the user entry attribute that
  // holds the clear-text password used to authenticate found users.
  private StringParameter userPasswordAttributeParameter = new StringParameter(
       "user_authentication_password_attribute",
       "User Authentication Password Attribute",
       "The name of an attribute in a user's entry that contains the " +
            "clear-text password to use to authenticate that user.  Either a " +
            "user authentication password or a user authentication password " +
            "attribute must be provided.",
       false, null);

  private BooleanParameter includeAuthzIDControlParameter =
       new BooleanParameter("include_authz_id_control",
            "Include Authorization Identity Request Control",
            "Indicates that bind requests should include the authorization " +
                 "identity request control, to request that the server " +
                 "return the identity of the authenticated user.",
            false);

  // The parameter used to provide a label for the additional parameters.
  private LabelParameter additionalLabelParameter = new LabelParameter(
       "Additional Parameters");

  // The parameter used to specify a warm-up time.
  private StringParameter warmUpDurationParameter = new StringParameter(
       "warm_up_duration", "Warm-Up Duration",
       "The length of time that the job should be allowed to run before it " +
            "starts collecting statistics.  The value may be a duration with " +
            "units (for example, '30 seconds' or '2 minutes'), but if " +
            "the value is only numeric without a unit, then it will be " +
            "interpreted as a number of seconds.  A nonzero warm-up time may " +
            "give the job a chance for all client threads to be spun up and " +
            "JIT optimization to be performed so that the load against the " +
            "server will be more consistent.",
       true, "0 seconds");

  // The parameter used to specify a cool-down time.
  private StringParameter coolDownDurationParameter = new StringParameter(
       "cool_down_duration", "Cool-Down Duration",
       "The length of time before the job completes that the job should stop " +
            "collecting statistics.  The value may be a duration with units " +
            "(for example, '30 seconds' or '2 minutes'), but if the " +
            "value is only numeric without a unit, then it will be " +
            "interpreted as a number of seconds.  A nonzero cool-down time " +
            "may help ensure that all client threads are still running and " +
            "the load against the server will be more consistent.",
       true, "0 seconds");

  // The parameter used to indicate whether to follow referrals.
  private BooleanParameter followReferralsParameter = new BooleanParameter(
       "follow_referrals", "Follow Referrals",
       "Indicates whether the client should attempt to automatically follow " +
            "any referrals that are returned by the server.",
       false);

  // The parameter used to specify the maximum search rate to attempt.
  private IntegerParameter maxRateParameter = new IntegerParameter("max_rate",
       "Max Authentication Rate (Auths/Second/Client)",
       "The maximum authentication rate, in terms of searches and their " +
            "associated binds per second, that each client should attempt to " +
            "maintain.  Note that if the job runs on multiple clients, then " +
            "each client will try to maintain this rate, so the overall " +
            "desired rate should be divided by the number of clients.  If no " +
            "value is specified, then no rate limiting will be performed.",
       false, -1, true, -1, true, Integer.MAX_VALUE);

  // The parameter used to specify the number of searches between reconnects.
  private IntegerParameter authenticationsBetweenReconnectsParameter =
       new IntegerParameter("authentications_between_reconnects",
            "Authentications Between Reconnects",
            "The number of authentication operations (a search and its " +
                 "associated bind) that the job should process on " +
                 "a connection before closing that connection and " +
                 "establishing a new one.  A value of zero indicates that " +
                 "each thread should continue using the same connection for " +
                 "all authentication attempts (although it may re-establish " +
                 "the connection if it appears that it is no longer valid).",
            false, 0, true, 0, false, Integer.MAX_VALUE);


  // Variables needed to perform processing using the parameter values.  These
  // should be static so that the values are shared across all threads.
  private static Control[] bindRequestControls;
  private static DereferencePolicy dereferencePolicy = null;
  private static FixedRateBarrier rateLimiter = null;
  private static int filter1Percentage = -1;
  private static int authenticationsBetweenReconnects = -1;
  private static int timeLimitSeconds = -1;
  private static long coolDownDurationMillis = -1L;
  private static long warmUpDurationMillis = -1L;
  private static ManageDsaITRequestControl manageDSAITControl = null;
  private static ProxiedAuthorizationV2RequestControl proxiedAuthControl = null;
  private static SearchScope scope = null;
  private static SimpleBindRequest searchUserBindRequest = null;
  private static ServerSet serverSet = null;
  private static StartTLSPostConnectProcessor startTLSProcessor = null;
  private static String userAuthenticationPassword;
  private static String userAuthenticationPasswordAttribute;
  private static Set<String> requestedAttributes = null;
  private static ValuePattern baseDNPattern = null;
  private static ValuePattern filterPattern1 = null;
  private static ValuePattern filterPattern2 = null;


  // Random number generators to use for selecting which filter to use.  There
  // will be a parent random, which is shared by all threads, and a per-thread
  // random that will be used to actually select the filter pattern.
  private static Random parentRandom = null;
  private Random random = null;


  // A pair of connection pools (one for searches, and a second for binds) that
  // this thread may use to communicate with the directory server.  Each thread
  // will have its own set of pools with just a single connection each, so this
  // should be non-static.  We're not sharing the pool across the threads, which
  // will reduce contention, but the benefit of using a pool over a simple
  // connection is that it can automatically re-establish a connection if it
  // becomes invalid for some reason.
  private LDAPConnectionPool searchPool;
  private LDAPConnectionPool bindPool;


  // The search request that will be repeatedly issued.  It will be with a new
  // base DN and filter for each search to be processed.  It will not be
  // shared across all the threads, so it should be non-static.
  private SearchRequest searchRequest;


  // Stat trackers used by this job.  We should have a separate copy per thread,
  // so these should be non-static.
  private CategoricalTracker bindResultCodes;
  private CategoricalTracker searchResultCodes;
  private IncrementalTracker authenticationsCompleted;
  private ResponseTimeCategorizer authenticationResponseTimeCategorizer;
  private ResponseTimeCategorizer bindResponseTimeCategorizer;
  private ResponseTimeCategorizer searchResponseTimeCategorizer;
  private TimeTracker authenticationTimer;
  private TimeTracker bindTimer;
  private TimeTracker searchTimer;



  /**
   * Creates a new instance of this job class.
   */
  public ComprehensiveSearchAndBindRateJob()
  {
    super();

    searchPool = null;
    bindPool = null;
    searchRequest = null;

    authenticationsCompleted = null;
    authenticationTimer = null;
    searchTimer = null;
    bindTimer = null;
    searchResultCodes = null;
    bindResultCodes = null;
    authenticationResponseTimeCategorizer = null;
    searchResponseTimeCategorizer = null;
    bindResponseTimeCategorizer = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Comprehensive Search And Bind Rate";
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
      "This job can be used to perform repeated authentications against an " +
           "LDAP directory server (consisting of a search to find a user " +
           "followed by a bind to verify the user's credentials) with a " +
           "comprehensive set of options."
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
    final Parameter[] parameters =
    {
      new PlaceholderParameter(),
      connectionLabelParameter,
      addressesAndPortsParameter,
      securityMethodParameter,

      new PlaceholderParameter(),
      authenticationLabelParameter,
      searchUserBindDNParameter,
      searchUserBindPasswordParameter,

      new PlaceholderParameter(),
      searchRequestLabelParameter,
      baseDNPatternParameter,
      scopeParameter,
      derefPolicyParameter,
      timeLimitParameter,
      filterPattern1Parameter,
      filterPattern2Parameter,
      filter1PercentageParameter,
      attributesToReturnParameter,
      includeManageDSAITParameter,
      proxiedAuthzIDParameter,

      new PlaceholderParameter(),
      bindRequestLabelParameter,
      userPasswordParameter,
      userPasswordAttributeParameter,
      includeAuthzIDControlParameter,

      new PlaceholderParameter(),
      additionalLabelParameter,
      warmUpDurationParameter,
      coolDownDurationParameter,
      followReferralsParameter,
      maxRateParameter,
      authenticationsBetweenReconnectsParameter,

      new PlaceholderParameter()
    };

    return new ParameterList(parameters);
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
      new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
           collectionInterval),
      new TimeTracker(clientID, threadID, STAT_BIND_DURATION,
           collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_SEARCH_RESULT_CODES,
           collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_BIND_RESULT_CODES,
           collectionInterval),
      ResponseTimeCategorizer.getStatTrackerStub(
           "Authentication Response Time Categories", clientID, threadID,
           collectionInterval),
      ResponseTimeCategorizer.getStatTrackerStub(
           "Search Response Time Categories", clientID, threadID,
           collectionInterval),
      ResponseTimeCategorizer.getStatTrackerStub(
           "Bind Response Time Categories", clientID, threadID,
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
      authenticationsCompleted,
      authenticationTimer,
      searchTimer,
      bindTimer,
      searchResultCodes,
      bindResultCodes,
      authenticationResponseTimeCategorizer.getStatTracker(),
      searchResponseTimeCategorizer.getStatTracker(),
      bindResponseTimeCategorizer.getStatTracker()
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void validateJobInfo(final int numClients, final int threadsPerClient,
                              final int threadStartupDelay,
                              final Date startTime, final Date stopTime,
                              final int duration, final int collectionInterval,
                              final ParameterList parameters)
         throws InvalidValueException
  {
    // Make sure that we can validate the set of addresses and ports.
    getAddressesAndPorts(parameters);


    // The search bind DN and password must be both empty or both non-empty.
    // NOTE:  We won't try to verify that the bind DN is actually a valid LDAP
    // distinguished name because some protocol-violating directory servers
    // allow LDAP simple binds with bind DNs that aren't actually DNs.
    final StringParameter searchUserBindDNParam =
         parameters.getStringParameter(searchUserBindDNParameter.getName());
    final PasswordParameter searchUserBindPWParam = parameters.
         getPasswordParameter(searchUserBindPasswordParameter.getName());
    if ((searchUserBindDNParam != null) && searchUserBindDNParam.hasValue())
    {
      if ((searchUserBindPWParam == null) ||
           (! searchUserBindPWParam.hasValue()))
      {
        throw new InvalidValueException("If a search user bind DN is " +
             "provided, then a search user bind password must also be " +
             "provided.");
      }
    }
    else if ((searchUserBindPWParam != null) &&
         searchUserBindPWParam.hasValue())
    {
        throw new InvalidValueException("If a esarch user bind password is " +
             "provided, then a search user bind DN must also be provided.");
    }


    // Make sure that we can generate a valid LDAP DN from the base DN pattern.
    final StringParameter baseParam =
         parameters.getStringParameter(baseDNPatternParameter.getName());
    if ((baseParam != null) && (baseParam.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(baseParam.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse base DN pattern value '" + baseParam.getValue() +
                  "' as a valid value pattern:  " + e.getMessage(),
             e);
      }

      final String baseDNString = valuePattern.nextValue();
      try
      {
        new DN(baseDNString);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Unable to parse a constructed base DN '" + baseDNString +
                  "' as a valid LDAP distinguished name:  " + e.getMatchedDN(),
             e);
      }
    }


    // Make sure that we can validate the first filter pattern as a vaild LDAP
    // filter.
    final StringParameter filter1Param =
         parameters.getStringParameter(filterPattern1Parameter.getName());
    if ((filter1Param != null) && (filter1Param.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(filter1Param.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse filter pattern 1 value '" +
                  filter1Param.getValue() + "' as a valid value pattern:  " +
                  e.getMessage(),
             e);
      }

      final String filterString = valuePattern.nextValue();
      try
      {
        Filter.create(filterString);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Unable to parse a constructed filter 1 value '" + filterString +
                  "' as a valid LDAP filter:  " + e.getMatchedDN(),
             e);
      }
    }


    // If a second filter pattern was provided, then make sure it is valid.
    // If a second filter pattern was not provided,then make sure that's okay.
    final StringParameter filter2Param =
         parameters.getStringParameter(filterPattern2Parameter.getName());
    if ((filter2Param != null) && (filter2Param.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(filter2Param.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse filter pattern 2 value '" +
                  filter2Param.getValue() + "' as a valid value pattern:  " +
                  e.getMessage(),
             e);
      }

      final String filterString = valuePattern.nextValue();
      try
      {
        Filter.create(filterString);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Unable to parse a constructed filter 2 value '" + filterString +
                  "' as a valid LDAP filter:  " + e.getMatchedDN(),
             e);
      }
    }
    else
    {
      final int filter1Percent = parameters.getIntegerParameter(
           filter1PercentageParameter.getName()).getIntValue();
      if (filter1Percent < 100)
      {
        throw new InvalidValueException("If the filter 1 percentage value " +
             "is less than 100, then a second filter pattern must be " +
             "provided.");
      }
    }


    // Make sure that exactly one of the user password or user password
    // attribute parameters was given a value.
    final PasswordParameter userPasswordParam =
         parameters.getPasswordParameter(userPasswordParameter.getName());
    final StringParameter userPasswordAttrParam = parameters.getStringParameter(
         userPasswordAttributeParameter.getName());
    if ((userPasswordParam != null) && userPasswordParam.hasValue())
    {
      if ((userPasswordAttrParam != null) && userPasswordAttrParam.hasValue())
      {
        throw new InvalidValueException("You cannot specify both a user " +
             "authentication password or a user authentication password " +
             "attribute.");
      }
    }
    else if ((userPasswordAttrParam == null) ||
         (! userPasswordAttrParam.hasValue()))
    {
      throw new InvalidValueException("You must specify either a user " +
           "authentication password or a user authentication password " +
           "attribute.");
    }


    // If a warm-up duration was specified, then make sure that we can
    // parse it.
    final StringParameter warmUpParam =
         parameters.getStringParameter(warmUpDurationParameter.getName());
    if ((warmUpParam != null) && warmUpParam.hasValue())
    {
      try
      {
        DurationParser.parse(warmUpParam.getValue());
      }
      catch (final SLAMDException e)
      {
        throw new InvalidValueException(
             "Unable to parse warm-up duration value '" +
                  warmUpParam.getValue() + "' as a valid duration:  " +
                  e.getMessage(),
             e);
      }
    }


    // If a cool-down duration was specified, then make sure that we can
    // parse it.  Also, make sure that the job was configured with either a
    // stop time or a duration.
    final StringParameter coolDownParam =
         parameters.getStringParameter(coolDownDurationParameter.getName());
    if ((coolDownParam != null) && coolDownParam.hasValue())
    {
      if ((stopTime == null) && (duration <= 0))
      {
        throw new InvalidValueException(
             "A job can only be used with a cool-down duration if it is also " +
                  "configured with a stop time or a duration.");
      }

      try
      {
        DurationParser.parse(coolDownParam.getValue());
      }
      catch (final SLAMDException e)
      {
        throw new InvalidValueException(
             "Unable to parse cool-down duration value '" +
                  coolDownParam.getValue() + "' as a valid duration:  " +
                  e.getMessage());
      }
    }
  }



  /**
   * Retrieves and validates the set of server addresses and ports from the
   * provided parameter list.
   *
   * @param  parameters  The parameter list containing the parameters to
   *                     validate.
   *
   * @throws  InvalidValueException  If there is a problem with the configured
   *                                 set of server addresses and ports.
   */
  private List<ObjectPair<String,Integer>> getAddressesAndPorts(
                                                final ParameterList parameters)
          throws InvalidValueException
  {
    final MultiLineTextParameter hostPortParam = parameters.
         getMultiLineTextParameter(addressesAndPortsParameter.getName());
    if ((hostPortParam != null) && (hostPortParam.hasValue()))
    {
      final String[] hostPorts = hostPortParam.getNonBlankLines();
      if (hostPorts.length == 0)
      {
        throw new InvalidValueException(
             "No server address:port values were configured.");
      }

      final List<ObjectPair<String,Integer>> addressesAndPorts =
           new ArrayList<>(hostPorts.length);
      for (final String hostPort : hostPorts)
      {
        final int lastColonPos = hostPort.lastIndexOf(':');
        if (lastColonPos < 0)
        {
          throw new InvalidValueException("Server address:port value '" +
               hostPort + "' is not valid because it does not contain a " +
               "colon to separate the address from the port number.");
        }
        else if (lastColonPos == 0)
        {
          throw new InvalidValueException("Server address:port value '" +
               hostPort + "' is not valid because it does not specify a " +
               "server address before the port number.");
        }
        else if (lastColonPos == (hostPort.length() - 1))
        {
          throw new InvalidValueException("Server address:port value '" +
               hostPort + "' is not valid because it does not specify a " +
               "port number after the server address.");
        }

        final int portNumber;
        try
        {
          portNumber = Integer.parseInt(hostPort.substring(lastColonPos+1));
        }
        catch (final Exception e)
        {
          throw new InvalidValueException(
               "Server address:port value '" + hostPort + "' is not valid " +
                    "because the port number cannot be parsed as an integer.",
               e);
        }

        if ((portNumber < 1) || (portNumber > 65_535))
        {
          throw new InvalidValueException("Server address:port value '" +
               hostPort + "' is not valid because the port number is not " +
               "between 1 and 65535, inclusive.");
        }

        addressesAndPorts.add(new ObjectPair<>(
             hostPort.substring(0, lastColonPos), portNumber));
      }

      return addressesAndPorts;
    }
    else
    {
      throw new InvalidValueException(
           "No server address:port values were configured.");
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
  public boolean testJobParameters(final ParameterList parameters,
                                   final List<String> outputMessages)
  {
    // Make sure that we can get an LDAP connection to the specified address.
    final List<ObjectPair<String,Integer>> addressesAndPorts;
    try
    {
      addressesAndPorts = getAddressesAndPorts(parameters);
    }
    catch (final InvalidValueException e)
    {
      outputMessages.add(e.getMessage());
      return false;
    }

    final boolean useSSL;
    final boolean useStartTLS;
    final String securitymethod = parameters.getMultiChoiceParameter(
         securityMethodParameter.getName()).getValueString();
    switch (securitymethod)
    {
      case SECURITY_METHOD_NONE:
        useSSL = false;
        useStartTLS = false;
        break;

      case SECURITY_METHOD_SSL:
        useSSL = true;
        useStartTLS = false;
        break;

      case SECURITY_METHOD_START_TLS:
        useSSL = false;
        useStartTLS = true;
        break;

      default:
        outputMessages.add("ERROR:  Unrecognized security method '" +
             securitymethod + "'.");
        return false;
    }

    for (final ObjectPair<String,Integer> addressAndPort : addressesAndPorts)
    {
      if (! outputMessages.isEmpty())
      {
        outputMessages.add("");
      }

      final String address = addressAndPort.getFirst();
      final int port = addressAndPort.getSecond();

      final LDAPConnection connection;
      try
      {
        if (useSSL)
        {
          outputMessages.add("Trying to establish an SSL-based connection to " +
               address + ':' + port + "...");
          final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
          connection = new LDAPConnection(sslUtil.createSSLSocketFactory(),
               address, port);
        }
        else
        {
          outputMessages.add("Trying to establish an unencrypted connection " +
               "to " + address + ':' + port + "...");
          connection = new LDAPConnection(address, port);
        }

        outputMessages.add("Successfully connected.");
      }
      catch (final Exception e)
      {
        outputMessages.add("ERROR:  Unable to establish the connection:  " +
             StaticUtils.getExceptionMessage(e));
        return false;
      }

      try
      {
        // If we should secure the connection with StartTLS, then verify that.
        if (useStartTLS)
        {
          outputMessages.add("");
          outputMessages.add(
               "Trying to secure the connection with StartTLS...");

          try
          {
            final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            connection.processExtendedOperation(new StartTLSExtendedRequest(
                 sslUtil.createSSLSocketFactory()));
            outputMessages.add("Successfully secured the connection.");
          }
          catch (final Exception e)
          {
            outputMessages.add("ERROR:  StartTLS failed:  " +
                 StaticUtils.getExceptionMessage(e));
            return false;
          }
        }


        // If we should authenticate the connection, then verify that.
        final StringParameter bindDNParam =
             parameters.getStringParameter(searchUserBindDNParameter.getName());
        final PasswordParameter bindPWParam = parameters.getPasswordParameter(
             searchUserBindPasswordParameter.getName());
        if ((bindDNParam != null) && bindDNParam.hasValue())
        {
          outputMessages.add("");
          outputMessages.add("Trying to perform an LDAP simple bind with DN '" +
               bindDNParam.getValue() + "'...");

          try
          {
            connection.bind(bindDNParam.getValue(), bindPWParam.getValue());
            outputMessages.add("Successfully authenticated.");
          }
          catch (final Exception e)
          {
            outputMessages.add("ERROR:  Bind failed:  " +
                 StaticUtils.getExceptionMessage(e));
            return false;
          }
        }


        // If a base DN pattern was provided, then make sure we can retrieve the
        // entry specified as the search base.
        final StringParameter baseDNParam =
             parameters.getStringParameter(baseDNPatternParameter.getName());
        if ((baseDNParam != null) && baseDNParam.hasValue())
        {
          outputMessages.add("");

          String baseDN;
          try
          {
            final ValuePattern valuePattern =
                 new ValuePattern(baseDNParam.getValue());
            baseDN = valuePattern.nextValue();
          }
          catch (final ParseException e)
          {
            outputMessages.add("ERROR:  Unable to construct a base DN from " +
                 "pattern '" + baseDNParam.getValue() + "':  " +
                 StaticUtils.getExceptionMessage(e));
            return false;
          }

          outputMessages.add("Verifying that search base entry '" + baseDN +
               "' exists...");
          try
          {
            final Entry baseEntry = connection.getEntry(baseDN);
            if (baseEntry == null)
            {
              outputMessages.add(
                   "ERROR:  The base entry could not be retrieved.");
              return false;
            }
            else
            {
              outputMessages.add("Successfully retrieved the entry.");
            }
          }
          catch (final Exception e)
          {
            outputMessages.add("ERROR:  Search attempt failed:  " +
                 StaticUtils.getExceptionMessage(e));
            return false;
          }
        }
      }
      finally
      {
        connection.close();
      }
    }

    // If we've gotten here, then everything was successful.
    outputMessages.add("");
    outputMessages.add("All tests completed successfully.");
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(final String clientID,
                               final ParameterList parameters)
         throws UnableToRunException
  {
    // Initialize the parent random number generator.
    parentRandom = new Random();


    // Initialize the server addresses and ports.
    final List<ObjectPair<String,Integer>> addressesAndPorts;
    try
    {
      addressesAndPorts = getAddressesAndPorts(parameters);
    }
    catch (final InvalidValueException e)
    {
      throw new UnableToRunException(e.getMessage());
    }

    // Initialize the security method.
    final boolean useSSL;
    final boolean useStartTLS;
    final SSLUtil sslUtil;
    final String securityMethod = parameters.getMultiChoiceParameter(
         securityMethodParameter.getName()).getValueString();
    switch (securityMethod)
    {
      case SECURITY_METHOD_NONE:
        useSSL = false;
        useStartTLS = false;
        sslUtil = null;
        break;
      case SECURITY_METHOD_SSL:
        useSSL = true;
        useStartTLS = false;
        sslUtil = new SSLUtil(new TrustAllTrustManager());
        break;
      case SECURITY_METHOD_START_TLS:
        useSSL = false;
        useStartTLS = true;
        sslUtil = new SSLUtil(new TrustAllTrustManager());
        break;
      default:
        throw new UnableToRunException("Unrecognized security method '" +
             securityMethod + "'.");
    }


    // Initialize the LDAP connection options.
    final LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
    connectionOptions.setUseSynchronousMode(true);
    connectionOptions.setFollowReferrals(parameters.getBooleanParameter(
         followReferralsParameter.getName()).getBooleanValue());


    // Create the socket factory.
    final SocketFactory socketFactory;
    if (useSSL)
    {
      try
      {
        socketFactory = sslUtil.createSSLSocketFactory();
      }
      catch (final Exception e)
      {
        throw new UnableToRunException(
             "Unable to create an SSL socket factory for securing LDAP " +
                  "communication:  " + StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      socketFactory = SocketFactory.getDefault();
    }


    // Create the server set.
    if (addressesAndPorts.size() > 1)
    {
      final String[] addresses = new String[addressesAndPorts.size()];
      final int[] ports = new int[addressesAndPorts.size()];
      for (int i=0; i < addressesAndPorts.size(); i++)
      {
        final ObjectPair<String,Integer> addressAndPort =
             addressesAndPorts.get(i);
        addresses[i] = addressAndPort.getFirst();
        ports[i] = addressAndPort.getSecond();
        serverSet = new RoundRobinServerSet(addresses, ports, socketFactory,
             connectionOptions);
      }
    }
    else
    {
      final ObjectPair<String,Integer> addressAndPort =
           addressesAndPorts.get(0);
      serverSet = new SingleServerSet(addressAndPort.getFirst(),
           addressAndPort.getSecond(), socketFactory, connectionOptions);
    }


    // Initialize the search user bind request.
    final StringParameter bindDNParam =
         parameters.getStringParameter(searchUserBindDNParameter.getName());
    if ((bindDNParam != null) && bindDNParam.hasValue())
    {
      searchUserBindRequest = new SimpleBindRequest(bindDNParam.getValue(),
           parameters.getPasswordParameter(
                searchUserBindPasswordParameter.getName()).getValue());
    }
    else
    {
      searchUserBindRequest = null;
    }


    // Initialize the post-connect processor.
    if (useStartTLS)
    {
      try
      {
        startTLSProcessor = new StartTLSPostConnectProcessor(
             sslUtil.createSSLSocketFactory());
      }
      catch (final Exception e)
      {
        throw new UnableToRunException(
             "Unable to create an SSL socket factory for the StartTLS " +
                  "post-connect processor:  " +
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      startTLSProcessor = null;
    }


    // Initialize the base DN pattern.
    final StringParameter baseDNParam = parameters.getStringParameter(
         baseDNPatternParameter.getName());
    try
    {
      if ((baseDNParam != null) && baseDNParam.hasValue())
      {
        baseDNPattern = new ValuePattern(baseDNParam.getValue());
      }
      else
      {
        baseDNPattern = new ValuePattern("");
      }
    }
    catch (final Exception e)
    {
      throw new UnableToRunException(
           "Unable to initialize the base DN value pattern:  " +
                StaticUtils.getExceptionMessage(e),
           e);
    }


    // Get the search scope.
    final String scopeStr = parameters.getMultiChoiceParameter(
         scopeParameter.getName()).getValueString();
    switch (scopeStr)
    {
      case SCOPE_BASE_OBJECT:
        scope = SearchScope.BASE;
        break;
      case SCOPE_SINGLE_LEVEL:
        scope = SearchScope.ONE;
        break;
      case SCOPE_WHOLE_SUBTREE:
        scope = SearchScope.SUB;
        break;
      case SCOPE_SUBORDINATE_SUBTREE:
        scope = SearchScope.SUBORDINATE_SUBTREE;
        break;
      default:
        throw new UnableToRunException("Unrecognized search scope '" +
             scopeStr + "'.");
    }


    // Get the alias dereferencing policy.
    final String derefStr = parameters.getMultiChoiceParameter(
         derefPolicyParameter.getName()).getValueString();
    switch (derefStr)
    {
      case DEREF_NEVER:
        dereferencePolicy = DereferencePolicy.NEVER;
        break;
      case DEREF_SEARCHING:
        dereferencePolicy = DereferencePolicy.SEARCHING;
        break;
      case DEREF_FINDING_BASE:
        dereferencePolicy = DereferencePolicy.FINDING;
        break;
      case DEREF_ALWAYS:
        dereferencePolicy = DereferencePolicy.ALWAYS;
        break;
      default:
        throw new UnableToRunException("Unrecognized alias dereferencing " +
             "policy '" + derefStr + "'.");
    }


    // Get the search time limit.
    timeLimitSeconds = parameters.getIntegerParameter(
         timeLimitParameter.getName()).getIntValue();


    // Initialize the first filter pattern.
    try
    {
      filterPattern1 = new ValuePattern(parameters.getStringParameter(
           filterPattern1Parameter.getName()).getValue());
    }
    catch (final Exception e)
    {
      throw new UnableToRunException(
           "Unable to initialize the filter 1 value pattern:  " +
                StaticUtils.getExceptionMessage(e),
           e);
    }


    // Initialize the filter pattern 1 percentage.
    filter1Percentage = parameters.getIntegerParameter(
         filter1PercentageParameter.getName()).getIntValue();


    // Initialize the second filter pattern.
    if (filter1Percentage < 100)
    {
      try
      {
        filterPattern2 = new ValuePattern(parameters.getStringParameter(
             filterPattern2Parameter.getName()).getValue());
      }
      catch (final Exception e)
      {
        throw new UnableToRunException(
             "Unable to initialize the filter 2 value pattern:  " +
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      filterPattern2 = null;
    }


    // Initialize the requested attributes.
    final MultiLineTextParameter attrsParam = parameters.
         getMultiLineTextParameter(attributesToReturnParameter.getName());
    if ((attrsParam != null) && attrsParam.hasValue())
    {
      requestedAttributes = new LinkedHashSet<>(Arrays.asList(
           attrsParam.getNonBlankLines()));
    }
    else
    {
      requestedAttributes = new LinkedHashSet<>(0);
    }


    // Create the manage DSA IT request control, if appropriate.
    final BooleanParameter manageDSAITParam =
         parameters.getBooleanParameter(includeManageDSAITParameter.getName());
    if ((manageDSAITParam != null) && manageDSAITParam.getBooleanValue())
    {
      manageDSAITControl = new ManageDsaITRequestControl(false);
    }
    else
    {
      manageDSAITControl = null;
    }


    // Create the proxied authorization request control, if appropriate.
    final StringParameter proxiedAuthIDParam =
         parameters.getStringParameter(proxiedAuthzIDParameter.getName());
    if ((proxiedAuthIDParam != null) && proxiedAuthIDParam.hasValue())
    {
      proxiedAuthControl = new ProxiedAuthorizationV2RequestControl(
           proxiedAuthIDParam.getValueString());
    }
    else
    {
      proxiedAuthControl = null;
    }


    // Initialize the user authentication password, the authentication password
    // attribute, and the search request attributes.
    final PasswordParameter userPasswordParam = parameters.getPasswordParameter(
         userPasswordParameter.getName());
    if ((userPasswordParam != null) && userPasswordParam.hasValue())
    {
      userAuthenticationPassword = userPasswordParam.getValueString();
      userAuthenticationPasswordAttribute = null;
    }
    else
    {
      userAuthenticationPassword = null;
      userAuthenticationPasswordAttribute = parameters.getStringParameter(
           userPasswordAttributeParameter.getName()).getValueString();
      requestedAttributes.add(userAuthenticationPasswordAttribute);
    }


    // Create the authorization identity request control, if appropriate.
    final BooleanParameter includeAuthzIDControlParam = parameters.
         getBooleanParameter(includeAuthzIDControlParameter.getName());
    if ((includeAuthzIDControlParam != null) &&
         includeAuthzIDControlParam.getBooleanValue())
    {
      bindRequestControls = new Control[]
      {
        new AuthorizationIdentityRequestControl(false)
      };
    }
    else
    {
      bindRequestControls = StaticUtils.NO_CONTROLS;
    }


    // Initialize the warm-up duration.
    final StringParameter warmUpParam = parameters.getStringParameter(
         warmUpDurationParameter.getName());
    if ((warmUpParam != null) && warmUpParam.hasValue())
    {
      try
      {
        warmUpDurationMillis =
             DurationParser.parse(warmUpParam.getValue()) * 1000L;
      }
      catch (final Exception e)
      {
        throw new UnableToRunException(
             "Unable to parse the warm-up duration:  "+
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      warmUpDurationMillis = -1L;
    }


    // Initialize the cool-down duration.
    final StringParameter coolDownParam = parameters.getStringParameter(
         coolDownDurationParameter.getName());
    if ((coolDownParam != null) && coolDownParam.hasValue())
    {
      try
      {
        coolDownDurationMillis =
             DurationParser.parse(coolDownParam.getValue()) * 1000L;
      }
      catch (final Exception e)
      {
        throw new UnableToRunException(
             "Unable to parse the cool-down duration:  "+
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      coolDownDurationMillis = -1L;
    }


    // Initialize the rate limiter.
    final IntegerParameter maxRateParam = parameters.getIntegerParameter(
         maxRateParameter.getName());
    if ((maxRateParam != null) && maxRateParam.hasValue())
    {
      final int maxRateValue = maxRateParam.getValue();
      if (maxRateValue > 0)
      {
        rateLimiter = new FixedRateBarrier(1000L, maxRateValue);
      }
      else
      {
        rateLimiter = null;
      }
    }
    else
    {
      rateLimiter = null;
    }


    // Initialize the number of authentications between reconnects.
    final IntegerParameter authsBetweenReconnectsParam =
         parameters.getIntegerParameter(
              authenticationsBetweenReconnectsParameter.getName());
    if ((authsBetweenReconnectsParam != null) &&
         authsBetweenReconnectsParam.hasValue())
    {
      authenticationsBetweenReconnects =
           authsBetweenReconnectsParam.getIntValue();
    }
    else
    {
      authenticationsBetweenReconnects = 0;
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
    // Initialize the thread-specific random-number generator.
    random = new Random(parentRandom.nextLong());


    // Initialize the stat trackers.
    authenticationsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_AUTHS_COMPLETED, collectionInterval);
    authenticationTimer = new TimeTracker(clientID, threadID,
         STAT_AUTH_DURATION, collectionInterval);
    searchTimer = new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
           collectionInterval);
    bindTimer = new TimeTracker(clientID, threadID, STAT_BIND_DURATION,
         collectionInterval);
    searchResultCodes = new CategoricalTracker(clientID, threadID,
         STAT_SEARCH_RESULT_CODES, collectionInterval);
    bindResultCodes = new CategoricalTracker(clientID, threadID,
         STAT_BIND_RESULT_CODES, collectionInterval);
    authenticationResponseTimeCategorizer = new ResponseTimeCategorizer(
         "Authentication Response Time Categories", clientID, threadID,
         collectionInterval);
    searchResponseTimeCategorizer = new ResponseTimeCategorizer(
         "Search Response Time Categories", clientID, threadID,
         collectionInterval);
    bindResponseTimeCategorizer = new ResponseTimeCategorizer(
         "Bind Response Time Categories", clientID, threadID,
         collectionInterval);

    final RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      authenticationsCompleted.enableRealTimeStats(statReporter, jobID);
      authenticationTimer.enableRealTimeStats(statReporter, jobID);
      searchTimer.enableRealTimeStats(statReporter, jobID);
      bindTimer.enableRealTimeStats(statReporter, jobID);
    }


    // Create a search request object for this thread.  Use a placeholder base
    // DN and filter.
    searchRequest = new SearchRequest("", scope, dereferencePolicy, 1,
         timeLimitSeconds, false, Filter.createPresenceFilter("objectClass"),
         requestedAttributes.toArray(StaticUtils.NO_STRINGS));

    if (manageDSAITControl != null)
    {
      searchRequest.addControl(manageDSAITControl);
    }

    if (proxiedAuthControl != null)
    {
      searchRequest.addControl(proxiedAuthControl);
    }


    // Create a connection pool to use to process the searches and binds.  Each
    // thread will have its own pair of pools (one for searches and one for
    // binds) with just a single connection each.  We're not sharing the pools
    // across the connections, but the benefit of the pool is that it will
    // automatically re-establish connections that might have become invalid for
    // some reason.
    try
    {
      searchPool = new LDAPConnectionPool(serverSet, searchUserBindRequest, 1,
           1, startTLSProcessor, true);
      bindPool = new LDAPConnectionPool(serverSet, null, 1, 1,
           startTLSProcessor, true);
    }
    catch (final Exception e)
    {
      throw new UnableToRunException(
           "Unable to create a connection pool to communicate with the " +
                "directory server:  " + StaticUtils.getExceptionMessage(e),
           e);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Figure out when to start and stop collecting statistics.
    final long stopCollectingTime;
    if ((coolDownDurationMillis > 0) && (getShouldStopTime() > 0L))
    {
      stopCollectingTime = getShouldStopTime() - coolDownDurationMillis;
    }
    else
    {
      stopCollectingTime = Long.MAX_VALUE;
    }

    boolean collectingStats;
    final long startCollectingTime;
    if (warmUpDurationMillis > 0)
    {
      collectingStats = false;
      startCollectingTime = System.currentTimeMillis() + warmUpDurationMillis;
    }
    else
    {
      collectingStats = true;
      startCollectingTime = System.currentTimeMillis();
      startTrackers();
    }


    // Perform the authentications until it's time to stop.
    boolean doneCollecting = false;
    long reconnectCounter = 0L;
    while (! shouldStop())
    {
      // See if it's time to close and re-establish a connection.  If so, then
      // do it in each pool.
      if (authenticationsBetweenReconnects > 0)
      {
        reconnectCounter++;
        if ((reconnectCounter % authenticationsBetweenReconnects) == 0L)
        {
          searchPool.shrinkPool(0);
          bindPool.shrinkPool(0);
        }
      }


      // If we should rate-limit the searches, then wait if necesary.
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }


      // See if it's time to change the tracking state.
      final long searchStartTime = System.currentTimeMillis();
      if (collectingStats && (searchStartTime >= stopCollectingTime))
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


      // Update the search request with an appropriate base DN and filter.
      searchRequest.setBaseDN(baseDNPattern.nextValue());

      final String filter;
      if (random.nextInt(100) < filter1Percentage)
      {
        filter = filterPattern1.nextValue();
      }
      else
      {
        filter = filterPattern2.nextValue();
      }

      try
      {
        searchRequest.setFilter(filter);
      }
      catch (final Exception e)
      {
        logMessage("ERROR:  Generated invalid search filter '" + filter + "'.");
        indicateCompletedWithErrors();
        return;
      }


      // Start the authentication process.
      Long beforeAuthTimeNanos = null;
      Long afterAuthTimeNanos = null;
      try
      {
        // Process the search.
        if (collectingStats)
        {
          authenticationTimer.startTimer();
          searchTimer.startTimer();
        }

        final SearchResult searchResult;
        try
        {
          beforeAuthTimeNanos = System.nanoTime();
          searchResult = searchPool.search(searchRequest);
          final long afterSearchTimeNanos = System.nanoTime();
          if (collectingStats)
          {
            searchResultCodes.increment(
                 searchResult.getResultCode().toString());
            searchResponseTimeCategorizer.categorizeResponseTime(
                 beforeAuthTimeNanos, afterSearchTimeNanos);
          }
        }
        catch (final LDAPException le)
        {
          if (collectingStats)
          {
            searchResultCodes.increment(le.getResultCode().toString());
          }
          continue;
        }
        finally
        {
          if (collectingStats)
          {
            searchTimer.stopTimer();
          }
        }

        // Construct the bind request.
        if (searchResult.getEntryCount() == 0)
        {
          if (collectingStats)
          {
            bindResultCodes.increment(
                 ResultCode.NO_RESULTS_RETURNED.toString());
          }
          continue;
        }

        final SimpleBindRequest bindRequest;
        final Entry userEntry = searchResult.getSearchEntries().get(0);
        if (userAuthenticationPassword != null)
        {
          bindRequest = new SimpleBindRequest(userEntry.getDN(),
               userAuthenticationPassword, bindRequestControls);
        }
        else
        {
          final String passwordAttributeValue = userEntry.getAttributeValue(
               userAuthenticationPasswordAttribute);
          if (passwordAttributeValue == null)
          {
            if (collectingStats)
            {
              bindResultCodes.increment(
                   ResultCode.NO_SUCH_ATTRIBUTE.toString());
            }
            continue;
          }

          bindRequest = new SimpleBindRequest(userEntry.getDN(),
               passwordAttributeValue, bindRequestControls);
        }

        // Process the bind.
        try
        {
          if (collectingStats)
          {
            bindTimer.startTimer();
          }

          final long beforeBindTimeNanos = System.nanoTime();
          final BindResult bindResult = bindPool.bind(bindRequest);
          afterAuthTimeNanos = System.nanoTime();

          if (collectingStats)
          {
            bindResultCodes.increment(bindResult.getResultCode().toString());
            bindResponseTimeCategorizer.categorizeResponseTime(
                 beforeBindTimeNanos, afterAuthTimeNanos);
          }
        }
        catch (final LDAPException e)
        {
          if (collectingStats)
          {
            bindResultCodes.increment(e.getResultCode().toString());
          }
        }
        finally
        {
          if (collectingStats)
          {
            bindTimer.stopTimer();
          }
        }
      }
      finally
      {
        if (collectingStats)
        {
          if (afterAuthTimeNanos == null)
          {
            afterAuthTimeNanos = System.nanoTime();
          }

          authenticationTimer.stopTimer();
          authenticationsCompleted.increment();
          authenticationResponseTimeCategorizer.categorizeResponseTime(
               beforeAuthTimeNanos, afterAuthTimeNanos);
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
    authenticationsCompleted.startTracker();
    authenticationTimer.startTracker();
    searchTimer.startTracker();
    bindTimer.startTracker();
    searchResultCodes.startTracker();
    bindResultCodes.startTracker();
    authenticationResponseTimeCategorizer.startStatTracker();
    searchResponseTimeCategorizer.startStatTracker();
    bindResponseTimeCategorizer.startStatTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    authenticationsCompleted.stopTracker();
    authenticationTimer.stopTracker();
    searchTimer.stopTracker();
    bindTimer.stopTracker();
    searchResultCodes.stopTracker();
    bindResultCodes.stopTracker();
    authenticationResponseTimeCategorizer.stopStatTracker();
    searchResponseTimeCategorizer.stopStatTracker();
    bindResponseTimeCategorizer.stopStatTracker();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void finalizeThread()
  {
    if (searchPool != null)
    {
      searchPool.close();
      searchPool = null;
    }

    if (bindPool != null)
    {
      bindPool.close();
      bindPool = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void destroyThread()
  {
    if (searchPool != null)
    {
      searchPool.close();
      searchPool = null;
    }

    if (bindPool != null)
    {
      bindPool.close();
      bindPool = null;
    }
  }
}
