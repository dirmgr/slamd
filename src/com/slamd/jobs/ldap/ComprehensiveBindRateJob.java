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
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.CRAMMD5BindRequest;
import com.unboundid.ldap.sdk.DIGESTMD5BindRequest;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.controls.AuthorizationIdentityRequestControl;
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
 * This class provides a SLAMD job that can measure the bind performance of an
 * LDAP directory server with a more comprehensive set of options than the
 * {@link BasicBindRateJob} job offers.
 */
public final class ComprehensiveBindRateJob
       extends JobClass
{
  /**
   * The display name for the stat tracker used to track binds completed.
   */
  private static final String STAT_BINDS_COMPLETED = "Binds Completed";



  /**
   * The display name for the stat tracker used to track bind durations.
   */
  private static final String STAT_BIND_DURATION = "Bind Duration (ms)";



  /**
   * The display name for the stat tracker used to track result codes.
   */
  private static final String STAT_RESULT_CODES = "Result Codes";



  // The parameter used to provide a label for the connection details.
  private LabelParameter connectionLabelParameter = new LabelParameter(
       "Connection Parameters");

  // The parameter used to specify the directory server address.
  private MultiLineTextParameter addressesAndPortsParameter =
       new MultiLineTextParameter("addresses_and_ports",
            "Server Addresses and Ports",
            "The addresses (resolvable names or IP addresses) and port " +
                 "numbers of the directory servers in which to process the " +
                 "bind operations (for example, 'ldap.example.com:389').  " +
                 "If multiple servers are to be targeted, then each " +
                 "address:port value should be listed on a separate line, " +
                 "and the job will use a round-robin algorithm when " +
                 "establishing connections to each server.",
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

  // The parameter used to provide a label for the bind request parameters.
  private LabelParameter bindRequestLabelParameter = new LabelParameter(
       "Bind Request Parameters");

  // The parameter used to specify the first authentication identity pattern.
  private StringParameter authenticationIdentityPattern1Parameter =
       new StringParameter("authn_id_pattern_1",
            "Authentication Identity Pattern 1",
            "A pattern to use to generate the first set of authentication " +
                 "identities.  For simple binds, this should specify the " +
                 "bind DN.  For SASL binds it may specify the bind DN (as " +
                 "indicated by a prefix of 'dn:') or a username (as " +
                 "indicated by a prefix of 'u:').  This may be a fixed value " +
                 "to use for all bind requests, or it may be a value pattern " +
                 "that can generate a different authentication identity for " +
                 "each bind request.  See https://docs.ldap.com/ldap-sdk/" +
                 "docs/javadoc/com/unboundid/util/ValuePattern.html for more " +
                 "information about value patterns.  This parameter always " +
                 "requires a value.",
            true, null);

  // The parameter used to specify the second authentication identity pattern.
  private StringParameter authenticationIdentityPattern2Parameter =
       new StringParameter("authn_id_pattern_2",
            "Authentication Identity Pattern 2",
            "A pattern to use to generate the second set of authentication " +
                 "identities.  For simple binds, this should specify the " +
                 "bind DN.  For SASL binds it may specify the bind DN (as " +
                 "indicated by a prefix of 'dn:') or a username (as " +
                 "indicated by a prefix of 'u:').  This may be a fixed value " +
                 "to use for all bind requests, or it may be a value pattern " +
                 "that can generate a different authentication identity for " +
                 "each bind request.  See https://docs.ldap.com/ldap-sdk/" +
                 "docs/javadoc/com/unboundid/util/ValuePattern.html for more " +
                 "information about value patterns.  This parameter " +
                 "requires a value if the Authentication Identity 1 " +
                 "Percentage value is less than 100.",
            false, null);

  // The parameter used to specify the percentage of binds that should use
  // the first entry DN pattern.
  private IntegerParameter authenticationIdentity1PercentageParameter =
       new IntegerParameter("authn_id_1_percent",
            "Authentication Identity 1 Percentage",
            "The percentage of bind requests that should use an " +
                 "authentication identity from the first pattern rather than " +
                 "the second pattern.",
       true, 100, true, 0, true, 100);

  // The password to use to bind to the directory server.
  private PasswordParameter bindPasswordParameter = new PasswordParameter(
       "bind_password", "Bind Password",
       "The password to include in the generated bind requests.",
       true, null);

  // The parameter used to specify the authentication type.
  private static final String AUTH_TYPE_SIMPLE = "Simple";
  private static final String AUTH_TYPE_CRAM_MD5 = "SASL CRAM-MD5";
  private static final String AUTH_TYPE_DIGEST_MD5 = "SASL DIGEST-MD5";
  private static final String AUTH_TYPE_PLAIN = "SASL PLAIN";
  private MultiChoiceParameter authenticationTypeParameter =
       new MultiChoiceParameter("authentication_type", "Authentication Type",
            "The type of authentication to attempt.",
            new String[]
            {
              AUTH_TYPE_SIMPLE,
              AUTH_TYPE_CRAM_MD5,
              AUTH_TYPE_DIGEST_MD5,
              AUTH_TYPE_PLAIN,
            },
            AUTH_TYPE_SIMPLE);

  // The parameter used to specify an authorization identity value pattern.
  private StringParameter authorizationIdentityPatternParameter =
       new StringParameter("authz_id_pattern", "Authorization Identity Pattern",
            "A pattern to use to generate an authorization identity to " +
                 "include in the bind request.  This may only be used in " +
                 "conjunction with the DIGEST-MD5 or PLAIN authentication " +
                 "types, and its vvalue may be a fixed string that should be " +
                 "used in all bind requests, or it may be a value pattern to " +
                 "use to generate a different authorization identity value " +
                 "for each bind.  See https://docs.ldap.com/ldap-sdk/docs/" +
                 "javadoc/com/unboundid/util/ValuePattern.html for more " +
                 "information about value patterns.",
            false, null);

  // The parameter used to specify a realm for DIGEST-MD5 authentication.
  private StringParameter realmParameter = new StringParameter(
       "realm", "DIGEST-MD5 Realm",
       "The realm that should be used when attempting DIGEST-MD5 " +
            "authentication.  This may only be provided when performing " +
            "DIGEST-MD5 authentication, and it may be omitted if no realm is " +
            "required.",
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

  // The parameter used to specify the maximum bind rate to attempt.
  private IntegerParameter maxRateParameter = new IntegerParameter("max_rate",
       "Max Bind Rate (Binds/Second/Client)",
       "The maximum bind rate, in terms of binds per second, that each " +
            "client should attempt to maintain.  Note that if the job runs " +
            "on multiple clients, then each client will try to maintain this " +
            "rate, so the overall desired rate should be divided by the " +
            "number of clients.  If no value is specified, then no rate " +
            "limiting will be performed.",
       false, -1, true, -1, true, Integer.MAX_VALUE);

  // The parameter used to specify the number of binds between reconnects.
  private IntegerParameter bindsBetweenReconnectsParameter =
       new IntegerParameter("binds_between_reconnects",
            "Binds Between Reconnects",
            "The number of bind operations that the job should process on " +
                 "a connection before closing that connection and " +
                 "establishing a new one.  A value of zero indicates that " +
                 "each thread should continue using the same connection for " +
                 "all bind operations (although it may re-establish the " +
                 "connection if it appears that it is no longer valid).",
            false, 0, true, 0, false, Integer.MAX_VALUE);


  // Variables needed to perform processing using the parameter values.  These
  // should be static so that the values are shared across all threads.
  private static Control[] requestControls = null;
  private static FixedRateBarrier rateLimiter = null;
  private static int authenticationID1Percentage = -1;
  private static int bindsBetweenReconnects = -1;
  private static long coolDownDurationMillis = -1L;
  private static long warmUpDurationMillis = -1L;
  private static ServerSet serverSet = null;
  private static String bindPassword = null;
  private static String authenticationType = null;
  private static String realm = null;
  private static StartTLSPostConnectProcessor startTLSProcessor = null;
  private static ValuePattern authenticationIdentityPattern1 = null;
  private static ValuePattern authenticationIdentityPattern2 = null;
  private static ValuePattern authorizationIdentityPattern = null;


  // Random number generators to use for selecting which authentication identity
  // pattern to use.  There will be a parent random, which is shared by all
  // threads, and a per-thread random that will be used to actually select the
  // pattern.
  private static Random parentRandom = null;
  private Random random = null;


  // A connection pool that this thread may use to communicate with the
  // directory server.  Each thread will have its own pool with just a single
  // connection, so this should be non-static.  We're not sharing the pool
  // across the threads, which will reduce contention, but the benefit of using
  // a pool over a simple connection is that it can automatically re-establish
  // a connection if it becomes invalid for some reason.
  private LDAPConnectionPool connectionPool;


  // Stat trackers used by this job.  We should have a separate copy per thread,
  // so these should be non-static.
  private CategoricalTracker resultCodes;
  private IncrementalTracker bindsCompleted;
  private ResponseTimeCategorizer responseTimeCategorizer;
  private TimeTracker bindTimer;



  /**
   * Creates a new instance of this job class.
   */
  public ComprehensiveBindRateJob()
  {
    super();

    connectionPool = null;
    random = null;

    bindsCompleted = null;
    bindTimer = null;
    resultCodes = null;
    responseTimeCategorizer = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Comprehensive Bind Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP bind operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated bind operations against an " +
           "LDAP directory server with a comprehensive set of options."
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
      bindRequestLabelParameter,
      authenticationIdentityPattern1Parameter,
      authenticationIdentityPattern2Parameter,
      authenticationIdentity1PercentageParameter,
      bindPasswordParameter,
      authenticationTypeParameter,
      authorizationIdentityPatternParameter,
      realmParameter,
      includeAuthzIDControlParameter,

      new PlaceholderParameter(),
      additionalLabelParameter,
      warmUpDurationParameter,
      coolDownDurationParameter,
      maxRateParameter,
      bindsBetweenReconnectsParameter,

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
      new IncrementalTracker(clientID, threadID, STAT_BINDS_COMPLETED,
           collectionInterval),
      new TimeTracker(clientID, threadID, STAT_BIND_DURATION,
           collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_RESULT_CODES,
           collectionInterval),
      ResponseTimeCategorizer.getStatTrackerStub(clientID, threadID,
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
      bindsCompleted,
      bindTimer,
      resultCodes,
      responseTimeCategorizer.getStatTracker()
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


    // The first authentication identity pattern must be defined, and it must be
    // a valid value pattern.
    final StringParameter authnIDPattern1Param = parameters.getStringParameter(
         authenticationIdentityPattern1Parameter.getName());
    try
    {
      new ValuePattern(authnIDPattern1Param.getValueString());
    }
    catch (final ParseException e)
    {
      throw new InvalidValueException(
           "Unable to parse the first authentication identity pattern '" +
                authnIDPattern1Param.getStringValue() +
                "' as a valid value pattern:  " + e.getMessage(),
           e);
    }


    // If a second authentication identity pattern was provided, then it must be
    // a valid value pattern.  If not, then make sure that the authentication
    // ID 1 percentage is 100.
    final StringParameter authnIDPattern2Param = parameters.getStringParameter(
         authenticationIdentityPattern2Parameter.getName());
    if ((authnIDPattern2Param != null) && authnIDPattern2Param.hasValue())
    {
      try
      {
        new ValuePattern(authnIDPattern2Param.getValueString());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse the first authentication identity pattern '" +
                  authnIDPattern1Param.getStringValue() +
                  "' as a valid value pattern:  " + e.getMessage(),
             e);
      }
    }
    else
    {
      final IntegerParameter authID1PercentParam =
           parameters.getIntegerParameter(
                authenticationIdentity1PercentageParameter.getName());
      if (authID1PercentParam.getIntValue() < 100)
      {
        throw new InvalidValueException("If the authentication identity 1 " +
             "percentage value is less than 100, then a second " +
             "authentication identity pattern must be provided.");
      }
    }


    // If an authorization identity pattern was provided, then make sure that
    // it's allowed for the selected authentication type, and make sure that
    // it's a valid value pattern.
    final StringParameter authzIDPatternParam = parameters.getStringParameter(
         authorizationIdentityPatternParameter.getName());
    if ((authzIDPatternParam != null) && authzIDPatternParam.hasValue())
    {
      final String authType = parameters.getMultiChoiceParameter(
           authenticationTypeParameter.getName()).getValueString();
      switch (authType)
      {
        case AUTH_TYPE_DIGEST_MD5:
        case AUTH_TYPE_PLAIN:
          try
          {
            new ValuePattern(authzIDPatternParam.getValueString());
          }
          catch (final ParseException e)
          {
            throw new InvalidValueException(
                 "Unable to parse the authorization identity pattern '" +
                      authzIDPatternParam.getValueString() +
                      "' as a valid value pattern:  " + e.getMessage(),
                 e);
          }
          break;

        default:
          throw new InvalidValueException("An authorization identity may " +
               "only be provided with an authentication type of either '" +
               AUTH_TYPE_DIGEST_MD5 + "' or '" + AUTH_TYPE_PLAIN + "'.");
      }
    }


    // If a realm was configured, then make sure that the authentication type is
    // DIGEST-MD5.
    final StringParameter realmParam = parameters.getStringParameter(
         realmParameter.getName());
    if ((realmParam != null) && realmParam.hasValue())
    {
      final String authType = parameters.getMultiChoiceParameter(
           authenticationTypeParameter.getName()).getValueString();
      if (! authType.equals(AUTH_TYPE_DIGEST_MD5))
      {
        throw new InvalidValueException("A realm may only be provided with " +
             "an authentication type of '" + AUTH_TYPE_DIGEST_MD5 + "'.");
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
   * Retrieves and validates the set of server addresses and ports from the
   * provided parameter list.
   *
   * @param  parameters  The parameter list containing the parameters to
   *                     validate.
   *
   * @return  The set of server addresses and ports.
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


    // Initialize the authentication ID patterns.
    final StringParameter authIDPattern1Param = parameters.getStringParameter(
         authenticationIdentityPattern1Parameter.getName());
    try
    {
      authenticationIdentityPattern1 = new ValuePattern(
           authIDPattern1Param.getStringValue());
    }
    catch (final ParseException e)
    {
      throw new UnableToRunException(
           "Unable to construct an authentication ID 1 pattern:  " +
                StaticUtils.getExceptionMessage(e),
           e);
    }

    final StringParameter authIDPattern2Param = parameters.getStringParameter(
         authenticationIdentityPattern2Parameter.getName());
    if ((authIDPattern2Param != null) && authIDPattern2Param.hasValue())
    {
      try
      {
        authenticationIdentityPattern2 = new ValuePattern(
             authIDPattern2Param.getStringValue());
      }
      catch (final ParseException e)
      {
        throw new UnableToRunException(
             "Unable to construct an authentication ID 2 pattern:  " +
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      authenticationIdentityPattern2 = null;
    }


    // Initialize the authentication ID 1 percentage.
    authenticationID1Percentage = parameters.getIntegerParameter(
         authenticationIdentity1PercentageParameter.getName()).getIntValue();


    // Initialize the bind password.
    bindPassword = parameters.getPasswordParameter(
         bindPasswordParameter.getName()).getValueString();


    // Initialize the authentication type.
    authenticationType = parameters.getMultiChoiceParameter(
         authenticationTypeParameter.getName()).getValueString();


    // Initialize the authorization identity pattern.
    final StringParameter authzIDPatternParam = parameters.getStringParameter(
         authorizationIdentityPatternParameter.getName());
    if ((authzIDPatternParam != null) && authzIDPatternParam.hasValue())
    {
      try
      {
        authorizationIdentityPattern = new ValuePattern(
             authzIDPatternParam.getValueString());
      }
      catch (final ParseException e)
      {
        throw new UnableToRunException(
             "Unable to construct a value pattern from the authorization " +
                  " identity pattern:  " +
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }
    else
    {
      authorizationIdentityPattern = null;
    }


    // Initialize the realm.
    realm = parameters.getStringParameter(realmParameter.getName()).
         getValueString();


    // Initialize the request controls.
    final BooleanParameter includeAuthzIDControlParam = parameters.
         getBooleanParameter(includeAuthzIDControlParameter.getName());
    if ((includeAuthzIDControlParam != null) &&
         includeAuthzIDControlParam.getBooleanValue())
    {
      requestControls = new Control[]
      {
        new AuthorizationIdentityRequestControl(false)
      };
    }
    else
    {
      requestControls = StaticUtils.NO_CONTROLS;
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


    // Initialize the number of binds between reconnects.
    final IntegerParameter bindsBetweenReconnectsParam = parameters.
         getIntegerParameter(bindsBetweenReconnectsParameter.getName());
    if ((bindsBetweenReconnectsParam != null) &&
         bindsBetweenReconnectsParam.hasValue())
    {
      bindsBetweenReconnects = bindsBetweenReconnectsParam.getIntValue();
    }
    else
    {
      bindsBetweenReconnects = 0;
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
    bindsCompleted = new IncrementalTracker(clientID, threadID,
         STAT_BINDS_COMPLETED, collectionInterval);
    bindTimer = new TimeTracker(clientID, threadID, STAT_BIND_DURATION,
         collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);
    responseTimeCategorizer = new ResponseTimeCategorizer(clientID, threadID,
         collectionInterval);

    final RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      bindsCompleted.enableRealTimeStats(statReporter, jobID);
      bindTimer.enableRealTimeStats(statReporter, jobID);
    }


    // Create a connection pool to use to process the searches.  Each thread
    // will have its own pool with just a single connection.  We're not sharing
    // the pool across the connections, but the benefit of the pool is that it
    // will automatically re-establish connections that might have become
    // invalid for some reason.
    try
    {
      connectionPool = new LDAPConnectionPool(serverSet, null, 1, 1,
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


    // Perform the binds until it's time to stop.
    boolean doneCollecting = false;
    long reconnectCounter = 0L;
    while (! shouldStop())
    {
      // See if it's time to close and re-establish a connection.
      if (bindsBetweenReconnects > 0)
      {
        reconnectCounter++;
        if ((reconnectCounter % bindsBetweenReconnects) == 0L)
        {
          connectionPool.shrinkPool(0);
        }
      }


      // If we should rate-limit the binds, then wait if necesary.
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }


      // See if it's time to change the tracking state.
      final long bindStartTime = System.currentTimeMillis();
      if (collectingStats && (bindStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (bindStartTime >= startCollectingTime))
      {
        collectingStats = true;
        startTrackers();
      }


      // Generate the bind request to send to the server.
      final BindRequest bindRequest = generateBindRequest();


      // Process the bind oepration.
      if (collectingStats)
      {
        bindTimer.startTimer();
      }

      try
      {
        final long beforeBindTimeNanos = System.nanoTime();
        final BindResult bindResult = connectionPool.bind(bindRequest);
        final long afterBindTimeNanos = System.nanoTime();
        if (collectingStats)
        {
          resultCodes.increment(bindResult.getResultCode().toString());
          responseTimeCategorizer.categorizeResponseTime(beforeBindTimeNanos,
               afterBindTimeNanos);
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
          bindTimer.stopTimer();
          bindsCompleted.increment();
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
   * Generates a bind request to process.
   *
   * @return  The modify request that was generated.
   */
  private BindRequest generateBindRequest()
  {
    final String authenticationID;
    if (random.nextInt(100) < authenticationID1Percentage)
    {
      authenticationID = authenticationIdentityPattern1.nextValue();
    }
    else
    {
      authenticationID = authenticationIdentityPattern2.nextValue();
    }

    final String authorizationID;
    if (authorizationIdentityPattern == null)
    {
      authorizationID = null;
    }
    else
    {
      authorizationID = authorizationIdentityPattern.nextValue();
    }

    switch (authenticationType)
    {
      case AUTH_TYPE_SIMPLE:
        return new SimpleBindRequest(authenticationID, bindPassword,
             requestControls);

      case AUTH_TYPE_CRAM_MD5:
        return new CRAMMD5BindRequest(authenticationID, bindPassword,
             requestControls);

      case AUTH_TYPE_DIGEST_MD5:
        return new DIGESTMD5BindRequest(authenticationID, authorizationID,
             bindPassword, realm, requestControls);

      case AUTH_TYPE_PLAIN:
        return new PLAINBindRequest(authenticationID, authorizationID,
             bindPassword, requestControls);

      default:
        // This should never happen.
        return null;
    }
  }



  /**
   * Starts the stat trackers for this job.
   */
  private void startTrackers()
  {
    bindsCompleted.startTracker();
    bindTimer.startTracker();
    resultCodes.startTracker();
    responseTimeCategorizer.startStatTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    bindsCompleted.stopTracker();
    bindTimer.stopTracker();
    resultCodes.stopTracker();
    responseTimeCategorizer.stopStatTracker();
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
}
