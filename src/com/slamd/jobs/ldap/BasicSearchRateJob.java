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
import java.util.Date;
import java.util.List;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.FixedRateBarrier;
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
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class provides a SLAMD job that can measure basic search performance
 * of an LDAP directory server.
 */
public final class BasicSearchRateJob
       extends JobClass
       implements SearchResultListener
{
  /**
   * The display name for the stat tracker used to track searches completed.
   */
  private static final String STAT_SEARCHES_COMPLETED = "Searches Completed";



  /**
   * The display name for the stat tracker used to track search durations.
   */
  private static final String STAT_SEARCH_DURATION = "Search Duration (ms)";



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
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -351414023004000562L;



  // The parameter used to provide a label for the connection details.
  private LabelParameter connectionLabelParameter = new LabelParameter(
       "Connection Parameters");

  // The parameter used to specify the directory server address.
  private StringParameter serverAddressParameter = new StringParameter(
       "address", "Directory Server Address",
       "The address of the directory server to search.  It may be a " +
            "resolvable name or an IP address.",
       true, null);

  // The parameter used to specify the directory server port.
  private IntegerParameter serverPortParameter = new IntegerParameter(
       "port", "Directory Serve Port",
       "The port number of the directory server to search.", true, 389, true, 1,
       true, 65535);

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
       "Authentication Parameters");

  // The DN to use to bind to the directory server.
  private StringParameter bindDNParameter = new StringParameter("bind_dn",
       "Bind DN",
       "The DN of the user as whom to bind directory server using LDAP " +
            "simple authentication.  If this is not provided, then the bind " +
            "password must also be empty, and the searches will be " +
            "performed over unauthenticated connections.",
       false, null);

  // The password to use to bind to the directory server.
  private PasswordParameter bindPasswordParameter = new PasswordParameter(
       "bind_password", "Bind Password",
       "The password to use to bind to the directory server using LDAP " +
            "simple authentication.  If this is not provided, then the bind " +
            "DN must also be empty, and the searches will be performed over " +
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

  // The parameter used to specify the search filter pattern.
  private StringParameter filterPatternParameter = new StringParameter(
       "filter_pattern", "Search Filter Pattern",
       "A pattern to use to generate the filter to use for each search " +
            "request.  This may be a fixed filter to use for all searches, " +
            "or it may be a value pattern that can construct a different " +
            "filter for each request.  See https://docs.ldap.com/ldap-sdk/" +
            "docs/javadoc/com/unboundid/util/ValuePattern.html for more " +
            "information about value patterns.",
       true, null);

  // The parameter used to specify the attributes to return.
  private MultiLineTextParameter attributesToReturnParameter =
       new MultiLineTextParameter("attributes_to_return",
            "Attributes to Return",
            "The names of the attributes to request that the server return " +
                 "in search result entries.  Multiple values may be " +
                 "provided, with one value per line.  Each value may be the " +
                 "name or OID of an LDAP attribute type, \"*\" (to request " +
                 "that the server return all user attributes), \"+\" (to " +
                 "request that the server return all operational " +
                 "attributes), \"1.1\" (to request that the server not " +
                 "return any attributes), or another type of value that the " +
                 "server may support.  If no values are provided, then the " +
                 "standard behavior is for the server to return all user " +
                 "attributes that the requester has permission to access.",
            new String[0], false);

  // The parameter used to provide a label for the additional parameters.
  private LabelParameter additionalLabelParameter = new LabelParameter(
       "Additional Parameters");

  // The parameter used to specify a warm-up time.
  private StringParameter warmUpDurationParameter = new StringParameter(
       "warm_up_duration", "Warm-Up Duration",
       "The length of time that the job should be allowed to run before it " +
            "starts collecting statistics.  The value may be a duration with " +
            "units (for example, \"30 seconds\" or \"2 minutes\"), but if " +
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
            "(for example, \"30 seconds\" or \"2 minutes\"), but if the " +
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
       "Max Search Rate (Searches/Second/Client)",
       "The maximum search rate, in terms of searches per second, that each " +
            "client should attempt to maintain.  Note that if the job runs " +
            "on multiple clients, then each client will try to maintain this " +
            "rate, so the overall desired rate should be divided by the " +
            "number  of clients.  If no value is specified, then no rate " +
            "limiting will be performed.",
       false, -1, true, -1, true, Integer.MAX_VALUE);


  // Variables needed to perform processing using the parameter values.  These
  // should be static so that the values are shared across all threads.
  private static FixedRateBarrier rateLimiter = null;
  private static long coolDownDurationMillis = -1L;
  private static long warmUpDurationMillis = -1L;
  private static SearchScope scope = null;
  private static SimpleBindRequest bindRequest = null;
  private static SingleServerSet serverSet = null;
  private static StartTLSPostConnectProcessor startTLSProcessor = null;
  private static String[] requestedAttributes = null;
  private static ValuePattern baseDNPattern = null;
  private static ValuePattern filterPattern = null;


  // A connection pool that this thread may use to communicate with the
  // directory server.  Each thread will have its own pool with just a single
  // connection, so this should be non-static.  We're not sharing the pool
  // across the threads, which will reduce contention, but the benefit of using
  // a pool over a simple connection is that it can automatically re-establish
  // a connection if it becomes invalid for some reason.
  private LDAPConnectionPool connectionPool;

  // The search request that will be repeatedly issued.  It will be with a new
  // base DN and filter for each search to be processed.  It will not be
  // shared across all the threads, so it should be non-static.
  private SearchRequest searchRequest;


  // Stat trackers used by this job.  We should have a separate copy per thread,
  // so these should be non-static.
  private CategoricalTracker  resultCodes;
  private IncrementalTracker  searchesCompleted;
  private IntegerValueTracker entriesReturned;
  private TimeTracker         searchTimer;



  /**
   * Creates a new instance of this job class.
   */
  public BasicSearchRateJob()
  {
    super();

    connectionPool = null;
    searchRequest = null;

    searchesCompleted = null;
    searchTimer = null;
    entriesReturned = null;
    resultCodes = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Basic Search Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP search operations";
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
           "directory server."
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
      serverAddressParameter,
      serverPortParameter,
      securityMethodParameter,

      new PlaceholderParameter(),
      authenticationLabelParameter,
      bindDNParameter,
      bindPasswordParameter,

      new PlaceholderParameter(),
      searchRequestLabelParameter,
      baseDNPatternParameter,
      scopeParameter,
      filterPatternParameter,
      attributesToReturnParameter,

      new PlaceholderParameter(),
      additionalLabelParameter,
      warmUpDurationParameter,
      coolDownDurationParameter,
      followReferralsParameter,
      maxRateParameter
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
      new IncrementalTracker(clientID, threadID, STAT_SEARCHES_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
                      collectionInterval),
      new IntegerValueTracker(clientID, threadID, STAT_ENTRIES_RETURNED,
                              collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
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
      searchesCompleted,
      searchTimer,
      entriesReturned,
      resultCodes
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
    // The bind DN and password must be both empty or both non-empty.
    // NOTE:  We won't try to verify that the bind DN is actually a valid LDAP
    // distinguished name because some protocol-violating directory servers
    // allow LDAP simple binds with bind DNs that aren't actually DNs.
    final StringParameter bindDNParam =
         parameters.getStringParameter(bindDNParameter.getName());
    final PasswordParameter bindPWParam =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((bindDNParam != null) && bindDNParam.hasValue())
    {
      if ((bindPWParam == null) || (! bindPWParam.hasValue()))
      {
        throw new InvalidValueException("If a bind DN is provided, then a " +
             "bind password must also be provided.");
      }
    }
    else if ((bindPWParam != null) && bindPWParam.hasValue())
    {
        throw new InvalidValueException("If a bind password is provided, " +
             "then a bind DN must also be provided.");
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


    // Make sure that we can validate the filter pattern as a vaild LDAP filter.
    final StringParameter filterParam =
         parameters.getStringParameter(filterPatternParameter.getName());
    if ((filterParam != null) && (filterParam.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(filterParam.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse filter pattern value '" + filterParam.getValue() +
                  "' as a valid value pattern:  " + e.getMessage(),
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
             "Unable to parse a constructed filter '" + filterString +
                  "' as a valid LDAP filter:  " + e.getMatchedDN(),
             e);
      }
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
    final String address = parameters.getStringParameter(
         serverAddressParameter.getName()).getValue();
    final int port = parameters.getIntegerParameter(
         serverPortParameter.getName()).getIntValue();

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
        outputMessages.add("Trying to establish an unencrypted connection to " +
             address + ':' + port + "...");
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
        outputMessages.add("Trying to secure the connection with StartTLS...");

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
           parameters.getStringParameter(bindDNParameter.getName());
      final PasswordParameter bindPWParam =
           parameters.getPasswordParameter(bindPasswordParameter.getName());
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

        outputMessages.add("Verifying that search bsae entry '" + baseDN +
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


      // If we've gotten here, then everything was successful.
      outputMessages.add("");
      outputMessages.add("All tests completed successfully.");
      return true;
    }
    finally
    {
      connection.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(final String clientID,
                               final ParameterList parameters)
         throws UnableToRunException
  {
    // Initialize the server address and port.
    final String serverAddress = parameters.getStringParameter(
         serverAddressParameter.getName()).getValue();
    final int serverPort = parameters.getIntegerParameter(
         serverPortParameter.getName()).getIntValue();

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


    // Create the server set.
    if (useSSL)
    {
      try
      {
        serverSet = new SingleServerSet(serverAddress, serverPort,
             sslUtil.createSSLSocketFactory(), connectionOptions);
      }
      catch (final Exception e)
      {
        throw new UnableToRunException(
             "Unable to create an SSL socket factory for securing " +
                  "communication to " + serverAddress + ':' + serverPort +
                  ":  " + StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      serverSet = new SingleServerSet(serverAddress, serverPort,
           connectionOptions);
    }


    // Initialize the bind request.
    final StringParameter bindDNParam =
         parameters.getStringParameter(bindDNParameter.getName());
    if ((bindDNParam != null) && bindDNParam.hasValue())
    {
      bindRequest = new SimpleBindRequest(bindDNParam.getValue(),
           parameters.getPasswordParameter(
                bindPasswordParameter.getName()).getValue());
    }
    else
    {
      bindRequest = null;
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


    // Get the search scope
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


    // Initialize the filter pattern.
    try
    {
      filterPattern = new ValuePattern(parameters.getStringParameter(
           filterPatternParameter.getName()).getValue());
    }
    catch (final Exception e)
    {
      throw new UnableToRunException(
           "Unable to initialize the filter value pattern:  " +
                StaticUtils.getExceptionMessage(e),
           e);
    }


    // Initialize the requested attributes.
    final MultiLineTextParameter attrsParam = parameters.
         getMultiLineTextParameter(attributesToReturnParameter.getName());
    if ((attrsParam != null) && attrsParam.hasValue())
    {
      requestedAttributes = attrsParam.getNonBlankLines();
    }
    else
    {
      requestedAttributes = new String[0];
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
    // Initialize the stat trackers.
    searchesCompleted = new IncrementalTracker(clientID, threadID,
         STAT_SEARCHES_COMPLETED, collectionInterval);
    searchTimer = new TimeTracker(clientID, threadID, STAT_SEARCH_DURATION,
         collectionInterval);
    entriesReturned = new IntegerValueTracker(clientID, threadID,
         STAT_ENTRIES_RETURNED, collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);

    final RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      searchesCompleted.enableRealTimeStats(statReporter, jobID);
      searchTimer.enableRealTimeStats(statReporter, jobID);
      entriesReturned.enableRealTimeStats(statReporter, jobID);
    }


    // Create a search request object for this thread.  Use a placeholder base
    // DN and filter.
    searchRequest = new SearchRequest("", scope,
         Filter.createPresenceFilter("objectClass"), requestedAttributes);


    // Create a connection pool to use to process the searches.  Each thread
    // will have its own pool with just a single connection.  We're not sharing
    // the pool across the connections, but the benefit of the pool is that it
    // will automatically re-establish connections that might have become
    // invalid for some reason.
    try
    {
      connectionPool = new LDAPConnectionPool(serverSet, bindRequest, 1, 1,
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


    // Perform the searches until it's time to stop.
    boolean doneCollecting = false;
    while (! shouldStop())
    {
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
      final String filter = filterPattern.nextValue();
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


      // Process the search.
      if (collectingStats)
      {
        searchTimer.startTimer();
      }

      try
      {
        final SearchResult searchResult = connectionPool.search(searchRequest);
        if (collectingStats)
        {
          entriesReturned.addValue(searchResult.getEntryCount());
          resultCodes.increment(searchResult.getResultCode().toString());
        }
      }
      catch (final LDAPException le)
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
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void finalizeThread()
  {
    if (connectionPool != null)
    {
      connectionPool.close();
      connectionPool = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public synchronized void destroyThread()
  {
    if (connectionPool != null)
    {
      connectionPool.close();
      connectionPool = null;
    }
  }



  /**
   * Indicates that the provided search result entry has been returned by the
   * server and may be processed by this search result listener.
   *
   * @param  searchEntry  The search result entry that has been returned by the
   *                      server.
   */
  @Override()
  public void searchEntryReturned(final SearchResultEntry searchEntry)
  {
    // We don't need to do anything with the entry.
  }



  /**
   * Indicates that the provided search result reference has been returned by
   * the server and may be processed by this search result listener.
   *
   * @param  searchReference  The search result reference that has been returned
   *                          by the server.
   */
  @Override()
  public void searchReferenceReturned(
                   final SearchResultReference searchReference)
  {
    // We don't need to do anything with the reference.
  }
}
