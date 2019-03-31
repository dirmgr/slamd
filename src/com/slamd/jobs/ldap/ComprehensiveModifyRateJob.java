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

import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.controls.AssertionRequestControl;
import com.unboundid.ldap.sdk.controls.ManageDsaITRequestControl;
import com.unboundid.ldap.sdk.controls.PermissiveModifyRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadRequestControl;
import com.unboundid.ldap.sdk.controls.PreReadRequestControl;
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
 * This class provides a SLAMD job that can measure the modify performance of an
 * LDAP directory server with a more comprehensive set of options than the
 * {@link BasicModifyRateJob} job offers.
 */
public final class ComprehensiveModifyRateJob
       extends JobClass
{
  /**
   * The display name for the stat tracker used to track modifies completed.
   */
  private static final String STAT_MODIFIES_COMPLETED = "Modifies Completed";



  /**
   * The display name for the stat tracker used to track modify durations.
   */
  private static final String STAT_MODIFY_DURATION = "Modify Duration (ms)";



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
                 "modify operations (for example, 'ldap.example.com:389').  " +
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

  // The parameter used to provide a label for the authentication details.
  private LabelParameter authenticationLabelParameter = new LabelParameter(
       "Authentication Parameters");

  // The DN to use to bind to the directory server.
  private StringParameter bindDNParameter = new StringParameter(
       "bind_dn", "Bind DN",
       "The DN of the user as whom to bind directory server using LDAP " +
            "simple authentication.",
       true, null);

  // The password to use to bind to the directory server.
  private PasswordParameter bindPasswordParameter = new PasswordParameter(
       "bind_password", "Bind Password",
       "The password to use to bind to the directory server using LDAP " +
            "simple authentication.",
       true, null);

  // The parameter used to provide a label for the modify request details.
  private LabelParameter modifyRequestLabelParameter = new LabelParameter(
       "Modify Request Parameters");

  // The parameter used to specify the first entry DN pattern.
  private StringParameter entryDNPattern1Parameter = new StringParameter(
       "entry_dn_pattern_1", "Entry DN Pattern 1",
       "A pattern to use to generate the DN of the entry to target with " +
            "modify requests.  This may be a fixed DN to use for all " +
            "modifies, or it may be a value pattern that can construct a " +
            "different DN for each request.  See https://docs.ldap.com/" +
            "ldap-sdk/docs/javadoc/com/unboundid/util/ValuePattern.html for " +
            "more information about value patterns.  This parameter always " +
            "requires a value.",
       true, null);

  // The parameter used to specify the second entry DN pattern.
  private StringParameter entryDNPattern2Parameter = new StringParameter(
       "entry_dn_pattern_2", "Entry DN Pattern 2",
       "A pattern to use to generate the DN of the entry to target with " +
            "modify requests.  This may be a fixed DN to use for all " +
            "modifies, or it may be a value pattern that can construct a " +
            "different DN for each request.  See https://docs.ldap.com/" +
            "ldap-sdk/docs/javadoc/com/unboundid/util/ValuePattern.html for " +
            "more information about value patterns.  This parameter requires " +
            "a value if the Entry DN 1 Percentage value is less than 100.",
       false, null);

  // The parameter used to specify the percentage of modifies that should use
  // the first entry DN pattern.
  private IntegerParameter entryDN1PercentageParameter = new IntegerParameter(
       "entry_dn_1_percent", "Entry DN 1 Percentage",
       "The percentage of modifies that should use an entry DN generated " +
            "from the first pattern rather than the second pattern.",
       true, 100, true, 0, true, 100);

  // The parameter used to specify the attributes to modify.
  private MultiLineTextParameter attributesToModifyParameter =
       new MultiLineTextParameter("attributes_to_modify",
            "Attributes to Modify",
            "The details of the attributes to modify.  Each line may provide " +
                 "information about an attribute whose values shoudl be " +
                 "replaced.  If the line contains just an attribute name, " +
                 "then a single replacement value will be generated for that " +
                 "attribute using 80 randomly selected alphabetic " +
                 "characters.  For more control, you may use the format " +
                 "'attributeName:numValues:valuePattern', where the name of " +
                 "the attribute is followed by a colon and the number of " +
                 "values to generate for that attribute, followed by a colon " +
                 "and the value pattern to use to generate values for that " +
                 "attribute.",
            new String[]
            {
              "description:1:[random:80:abcdefghijklmnopqrstuvwxyz]"
            },
            true);

  // The parameter used to provide a label for controls to include in the
  // request.
  private LabelParameter controlsLabelParameter = new LabelParameter(
       "Modify Request Control Parameters");

  private BooleanParameter includeManageDSAITParameter = new BooleanParameter(
       "include_manage_dsa_it_control", "Include Manage DSA IT Request Control",
       "Indicates that modify requests should include the manage DSA IT " +
            "request control, which indicates that the server should modify " +
            "smart referral entries as regular entries rather than " +
            "generating a referral result.",
       false);

  private BooleanParameter includePermissiveModifyParameter =
       new BooleanParameter("include_permissive_modify_control",
            "Include Permissive Modify Request Control",
            "Indicates that modify requests should include the permissive " +
                 "modify request control, which indicates that the server " +
                 "should ignore modifications that would not result in a " +
                 "change to the entry (for example, adding a value that " +
                 "already exists or deleting a value that does not exist).",
       false);

  private StringParameter proxiedAuthzIDParameter = new StringParameter(
       "proxied_auth_id", "Proxied Authorization ID",
       "Indicates that modify requests should include a proxied " +
            "authorization request control with the specified authorization " +
            "ID.  Note that this should be a static authorization ID string " +
            "rather than a value pattern.",
       false, null);

  private StringParameter assertionFilterParameter = new StringParameter(
       "assertion_filter", "Assertion Filter",
       "Indicates that modify requests should include an LDAP assertion " +
            "request control with the provided filter.  Modify operations " +
            "will only succeed if the target entry matches the provided " +
            "filter.  Note that this should be a static filter rather than " +
            "a value pattern.",
       false, null);

  private MultiLineTextParameter preReadAttributesParameter =
       new MultiLineTextParameter("pre_read_attributes", "Pre-Read Attributes",
            "Indicates that modify requests should include an LDAP pre-read " +
                 "request control to indicate that the server should return " +
                 "the values of the specified attributes as they appeared " +
                 "before the modify operation was processed.  Multiple " +
                 "pre-read attributes may be specified by placing each " +
                 "attribute on a separate line.",
            StaticUtils.NO_STRINGS, false);

  private MultiLineTextParameter postReadAttributesParameter =
       new MultiLineTextParameter("post_read_attributes",
            "Post-Read Attributes",
            "Indicates that modify requests should include an LDAP post-read " +
                 "request control to indicate that the server should return " +
                 "the values of the specified attributes as they appeared " +
                 "after the modify operation was processed.  Multiple " +
                 "post-read attributes may be specified by placing each " +
                 "attribute on a separate line.",
            StaticUtils.NO_STRINGS, false);

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

  // The parameter used to specify the maximum modify rate to attempt.
  private IntegerParameter maxRateParameter = new IntegerParameter("max_rate",
       "Max Modify Rate (Modifies/Second/Client)",
       "The maximum modify rate, in terms of modifies per second, that each " +
            "client should attempt to maintain.  Note that if the job runs " +
            "on multiple clients, then each client will try to maintain this " +
            "rate, so the overall desired rate should be divided by the " +
            "number of clients.  If no value is specified, then no rate " +
            "limiting will be performed.",
       false, -1, true, -1, true, Integer.MAX_VALUE);

  // The parameter used to specify the number of modifies between reconnects.
  private IntegerParameter modifiesBetweenReconnectsParameter =
       new IntegerParameter("modifies_between_reconnects",
            "Modifies Between Reconnects",
            "The number of modify operations that the job should process on " +
                 "a connection before closing that connection and " +
                 "establishing a new one.  A value of zero indicates that " +
                 "each thread should continue using the same connection for " +
                 "all modify operations (although it may re-establish the " +
                 "connection if it appears that it is no longer valid).",
            false, 0, true, 0, false, Integer.MAX_VALUE);


  // Variables needed to perform processing using the parameter values.  These
  // should be static so that the values are shared across all threads.
  private static Control[] requestControls = null;
  private static FixedRateBarrier rateLimiter = null;
  private static int entryDN1Percentage = -1;
  private static int modifiesBetweenReconnects = -1;
  private static List<ComprehensiveModifyRateAttributeModification>
       attributeModifications = null;
  private static long coolDownDurationMillis = -1L;
  private static long warmUpDurationMillis = -1L;
  private static SimpleBindRequest bindRequest = null;
  private static ServerSet serverSet = null;
  private static StartTLSPostConnectProcessor startTLSProcessor = null;
  private static ValuePattern entryDNPattern1 = null;
  private static ValuePattern entryDNPattern2 = null;


  // Random number generators to use for selecting which entry DN pattern to
  // use.  There will be a parent random, which is shared by all threads, and a
  // per-thread random that will be used to actually select the DN pattern.
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
  private IncrementalTracker modifiesCompleted;
  private ResponseTimeCategorizer responseTimeCategorizer;
  private TimeTracker modifyTimer;



  /**
   * Creates a new instance of this job class.
   */
  public ComprehensiveModifyRateJob()
  {
    super();

    connectionPool = null;
    random = null;

    modifiesCompleted = null;
    modifyTimer = null;
    resultCodes = null;
    responseTimeCategorizer = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Comprehensive Modify Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP modify operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated modify operations against an " +
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
      authenticationLabelParameter,
      bindDNParameter,
      bindPasswordParameter,

      new PlaceholderParameter(),
      modifyRequestLabelParameter,
      entryDNPattern1Parameter,
      entryDNPattern2Parameter,
      entryDN1PercentageParameter,
      attributesToModifyParameter,

      new PlaceholderParameter(),
      controlsLabelParameter,
      includeManageDSAITParameter,
      includePermissiveModifyParameter,
      proxiedAuthzIDParameter,
      assertionFilterParameter,
      preReadAttributesParameter,
      postReadAttributesParameter,

      new PlaceholderParameter(),
      additionalLabelParameter,
      warmUpDurationParameter,
      coolDownDurationParameter,
      followReferralsParameter,
      maxRateParameter,
      modifiesBetweenReconnectsParameter,

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
      new IncrementalTracker(clientID, threadID, STAT_MODIFIES_COMPLETED,
           collectionInterval),
      new TimeTracker(clientID, threadID, STAT_MODIFY_DURATION,
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
      modifiesCompleted,
      modifyTimer,
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


    // Make sure that we can generate a valid LDAP DN from the first entry DN
    // pattern.
    final StringParameter dnPattern1Param =
         parameters.getStringParameter(entryDNPattern1Parameter.getName());
    if ((dnPattern1Param != null) && (dnPattern1Param.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(dnPattern1Param.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse entry DN pattern 1 value '" +
                  dnPattern1Param.getValue() + "' as a valid value pattern:  " +
                  e.getMessage(),
             e);
      }

      final String dnString = valuePattern.nextValue();
      try
      {
        new DN(dnString);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Unable to parse DN '" + dnString +
                  "' (constructed from entry DN pattern 1) as a valid LDAP " +
                  "distinguished name:  " + e.getMatchedDN(),
             e);
      }
    }


    // If a second entry DN pattern was provided, then make sure that it is
    // valid.  If no entry DN pattern 2 was provided, make sure that's okay.
    final StringParameter dnPattern2Param =
         parameters.getStringParameter(entryDNPattern1Parameter.getName());
    if ((dnPattern2Param != null) && (dnPattern2Param.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(dnPattern2Param.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse entry DN pattern 2 value '" +
                  dnPattern2Param.getValue() + "' as a valid value pattern:  " +
                  e.getMessage(),
             e);
      }

      final String dnString = valuePattern.nextValue();
      try
      {
        new DN(dnString);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Unable to parse DN '" + dnString +
                  "' (constructed from entry DN pattern 2) as a valid LDAP " +
                  "distinguished name:  " + e.getMatchedDN(),
             e);
      }
    }
    else
    {
      final int dn1Percent = parameters.getIntegerParameter(
           entryDN1PercentageParameter.getName()).getIntValue();
      if (dn1Percent < 100)
      {
        throw new InvalidValueException("If the entry DN 1 percentage value " +
             "is less than 100, then a second entry DN pattern must be " +
             "provided.");
      }
    }


    // Validate the set of attributes to modify.
    getAttributesToModify(parameters);


    // If an assertion filter was provided, then validate it.
    final StringParameter assertionFilterParam =
         parameters.getStringParameter(assertionFilterParameter.getName());
    if ((assertionFilterParam != null) &&
         assertionFilterParam.hasValue())
    {
      final String filterStr = assertionFilterParam.getStringValue();
      try
      {
        Filter.create(filterStr);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Assertion filter '" + filterStr + "' is not valid:  " +
                  e.getMessage(),
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
   * Retrieves information about the set of attributes to modify.
   *
   * @param  parameters  The parameter list from which to parse the set of
   *                     attributes to modify.
   *
   * @return  Information about the set of attributes to modify.
   *
   * @throws  InvalidValueException  If a problem is encountered while
   *                                 constructing the set of attributes to
   *                                 modify.
   */
  private List<ComprehensiveModifyRateAttributeModification>
               getAttributesToModify(final ParameterList parameters)
          throws InvalidValueException
  {
    final MultiLineTextParameter param = parameters.getMultiLineTextParameter(
         attributesToModifyParameter.getName());
    final String[] nonBlankLines = param.getNonBlankLines();
    if (nonBlankLines.length == 0)
    {
      throw new InvalidValueException("The set of attributes to modify must " +
           "not be empty.");
    }

    final ArrayList<ComprehensiveModifyRateAttributeModification> attrMods =
         new ArrayList<>(nonBlankLines.length);
    for (final String line : nonBlankLines)
    {
      attrMods.add(ComprehensiveModifyRateAttributeModification.
           parseAttributeToModify(line));
    }

    return attrMods;
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


        // If a first entry DN pattern was provided, then make sure we can
        // retrieve an entry whose DN was generated from that pattern.
        final StringParameter entryDN1Param =
             parameters.getStringParameter(entryDNPattern1Parameter.getName());
        if ((entryDN1Param != null) && entryDN1Param.hasValue())
        {
          outputMessages.add("");

          String entryDN;
          try
          {
            final ValuePattern valuePattern =
                 new ValuePattern(entryDN1Param.getValue());
            entryDN = valuePattern.nextValue();
          }
          catch (final ParseException e)
          {
            outputMessages.add("ERROR:  Unable to construct an entry DN from " +
                 "pattern '" + entryDN1Param.getValue() + "':  " +
                 StaticUtils.getExceptionMessage(e));
            return false;
          }

          outputMessages.add("Verifying that entry '" + entryDN +
               "' (generated from entry DN pattern 1) exists...");
          try
          {
            final Entry baseEntry = connection.getEntry(entryDN);
            if (baseEntry == null)
            {
              outputMessages.add(
                   "ERROR:  The entry could not be retrieved.");
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


        // If a second entry DN pattern was provided, then make sure we can
        // retrieve an entry whose DN was generated from that pattern.
        final StringParameter entryDN2Param =
             parameters.getStringParameter(entryDNPattern2Parameter.getName());
        if ((entryDN2Param != null) && entryDN2Param.hasValue())
        {
          outputMessages.add("");

          String entryDN;
          try
          {
            final ValuePattern valuePattern =
                 new ValuePattern(entryDN2Param.getValue());
            entryDN = valuePattern.nextValue();
          }
          catch (final ParseException e)
          {
            outputMessages.add("ERROR:  Unable to construct an entry DN from " +
                 "pattern '" + entryDN2Param.getValue() + "':  " +
                 StaticUtils.getExceptionMessage(e));
            return false;
          }

          outputMessages.add("Verifying that entry '" + entryDN +
               "' (generated from entry DN pattern 2) exists...");
          try
          {
            final Entry baseEntry = connection.getEntry(entryDN);
            if (baseEntry == null)
            {
              outputMessages.add(
                   "ERROR:  The entry could not be retrieved.");
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


    // Initialize entry DN pattern 1.
    final StringParameter dn1Param = parameters.getStringParameter(
         entryDNPattern1Parameter.getName());
    try
    {
      entryDNPattern1 = new ValuePattern(dn1Param.getValue());
    }
    catch (final Exception e)
    {
      throw new UnableToRunException(
           "Unable to create a value pattern from the first entry DN " +
                "pattern:  " + StaticUtils.getExceptionMessage(e),
           e);
    }


    // Initialize the entry DN pattern 1 percentage.
    entryDN1Percentage = parameters.getIntegerParameter(
         entryDN1PercentageParameter.getName()).getIntValue();


    // Initialize the second filter pattern.
    if (entryDN1Percentage < 100)
    {
      // Initialize entry DN pattern 2.
      final StringParameter dn2Param = parameters.getStringParameter(
           entryDNPattern2Parameter.getName());
      if ((dn2Param != null) && dn2Param.hasValue())
      {
        try
        {
          entryDNPattern2 = new ValuePattern(dn2Param.getValue());
        }
        catch (final Exception e)
        {
          throw new UnableToRunException(
               "Unable to create a value pattern from the second entry DN " +
                    "pattern:  " + StaticUtils.getExceptionMessage(e),
               e);
        }
      }
    }
    else
    {
      entryDNPattern2 = null;
    }


    // Initialize the attribute modification data.
    final MultiLineTextParameter attrsParam = parameters.
         getMultiLineTextParameter(attributesToModifyParameter.getName());
    final String[] attrsTomodifyValues = attrsParam.getNonBlankLines();
    attributeModifications = new ArrayList<>(attrsTomodifyValues.length);
    for (final String s : attrsTomodifyValues)
    {
      try
      {
        attributeModifications.add(ComprehensiveModifyRateAttributeModification.
             parseAttributeToModify(s));
      }
      catch (final InvalidValueException e)
      {
        throw new UnableToRunException(e.getMessage(), e);
      }
    }


    // Create the manage DSA IT request control, if appropriate.
    final List<Control> controlList = new ArrayList<>(6);
    final BooleanParameter manageDSAITParam =
         parameters.getBooleanParameter(includeManageDSAITParameter.getName());
    if ((manageDSAITParam != null) && manageDSAITParam.getBooleanValue())
    {
      controlList.add(new ManageDsaITRequestControl(false));
    }


    // Create the permissive modify request control, if appropriate.
    final BooleanParameter permissiveModParam = parameters.
         getBooleanParameter(includePermissiveModifyParameter.getName());
    if ((permissiveModParam != null) && permissiveModParam.getBooleanValue())
    {
      controlList.add(new PermissiveModifyRequestControl(false));
    }


    // Create the proxied authorization request control, if appropriate.
    final StringParameter proxiedAuthIDParam =
         parameters.getStringParameter(proxiedAuthzIDParameter.getName());
    if ((proxiedAuthIDParam != null) && proxiedAuthIDParam.hasValue())
    {
      controlList.add(new ProxiedAuthorizationV2RequestControl(
           proxiedAuthIDParam.getValueString()));
    }


    // Create the assertion request control, if appropriate.
    final StringParameter assertionFilterParam =
         parameters.getStringParameter(assertionFilterParameter.getName());
    if ((assertionFilterParam != null) && assertionFilterParam.hasValue())
    {
      try
      {
        controlList.add(new AssertionRequestControl(
             assertionFilterParam.getValueString()));
      }
      catch (final LDAPException e)
      {
        throw new UnableToRunException(
             "Unable to parse the assertion request control filter '" +
             assertionFilterParam.getValueString() +
                  "' as a valid LDAP filter:  " + e.getMessage(),
             e);
      }
    }


    // Create the pre-read request control, if appropriate.
    final MultiLineTextParameter preReadAttrsParam = parameters.
         getMultiLineTextParameter(preReadAttributesParameter.getName());
    if ((preReadAttrsParam != null) && preReadAttrsParam.hasValue())
    {
      final String[] preReadAttrs = preReadAttrsParam.getNonBlankLines();
      if (preReadAttrs.length > 0)
      {
        controlList.add(new PreReadRequestControl(false, preReadAttrs));
      }
    }


    // Create the post-read request control, if appropriate.
    final MultiLineTextParameter postReadAttrsParam = parameters.
         getMultiLineTextParameter(postReadAttributesParameter.getName());
    if ((postReadAttrsParam != null) && postReadAttrsParam.hasValue())
    {
      final String[] postReadAttrs = postReadAttrsParam.getNonBlankLines();
      if (postReadAttrs.length > 0)
      {
        controlList.add(new PostReadRequestControl(false, postReadAttrs));
      }
    }


    // Convert the control list into an array.
    requestControls = new Control[controlList.size()];
    controlList.toArray(requestControls);


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


    // Initialize the number of modifies between reconnects.
    final IntegerParameter modsBetweenReconnectsParam = parameters.
         getIntegerParameter(modifiesBetweenReconnectsParameter.getName());
    if ((modsBetweenReconnectsParam != null) &&
         modsBetweenReconnectsParam.hasValue())
    {
      modifiesBetweenReconnects = modsBetweenReconnectsParam.getIntValue();
    }
    else
    {
      modifiesBetweenReconnects = 0;
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
    modifiesCompleted = new IncrementalTracker(clientID, threadID,
         STAT_MODIFIES_COMPLETED, collectionInterval);
    modifyTimer = new TimeTracker(clientID, threadID, STAT_MODIFY_DURATION,
         collectionInterval);
    resultCodes = new CategoricalTracker(clientID, threadID,
         STAT_RESULT_CODES, collectionInterval);
    responseTimeCategorizer = new ResponseTimeCategorizer(clientID, threadID,
         collectionInterval);

    final RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      modifiesCompleted.enableRealTimeStats(statReporter, jobID);
      modifyTimer.enableRealTimeStats(statReporter, jobID);
    }


    // Create a connection pool to use to process the modifies.  Each thread
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


    // Perform the modifies until it's time to stop.
    boolean doneCollecting = false;
    long reconnectCounter = 0L;
    while (! shouldStop())
    {
      // See if it's time to close and re-establish a connection.
      if (modifiesBetweenReconnects > 0)
      {
        reconnectCounter++;
        if ((reconnectCounter % modifiesBetweenReconnects) == 0L)
        {
          connectionPool.shrinkPool(0);
        }
      }


      // If we should rate-limit the modifies, then wait if necesary.
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }


      // See if it's time to change the tracking state.
      final long modifyStartTime = System.currentTimeMillis();
      if (collectingStats && (modifyStartTime >= stopCollectingTime))
      {
        stopTrackers();
        collectingStats = false;
        doneCollecting  = true;
      }
      else if ((! collectingStats) && (! doneCollecting) &&
               (modifyStartTime >= startCollectingTime))
      {
        collectingStats = true;
        startTrackers();
      }


      // Generate the modify request to send to the server.
      final ModifyRequest modifyRequest = generateModifyRequest();


      // Process the modify oepration.
      if (collectingStats)
      {
        modifyTimer.startTimer();
      }

      try
      {
        final long beforeModifyTimeNanos = System.nanoTime();
        final LDAPResult modifyResult = connectionPool.modify(modifyRequest);
        final long afterModifyTimeNanos = System.nanoTime();
        if (collectingStats)
        {
          resultCodes.increment(modifyResult.getResultCode().toString());
          responseTimeCategorizer.categorizeResponseTime(beforeModifyTimeNanos,
               afterModifyTimeNanos);
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
          modifyTimer.stopTimer();
          modifiesCompleted.increment();
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
   * Generates a modify request to process.
   *
   * @return  The modify request that was generated.
   */
  private ModifyRequest generateModifyRequest()
  {
    final String entryDN;
    if (random.nextInt(100) < entryDN1Percentage)
    {
      entryDN = entryDNPattern1.nextValue();
    }
    else
    {
      entryDN = entryDNPattern2.nextValue();
    }

    final Modification[] mods = new Modification[attributeModifications.size()];
    for (int i=0; i < mods.length; i++)
    {
      mods[i] = attributeModifications.get(i).generateModification();
    }

    return new ModifyRequest(entryDN, mods, requestControls);
  }



  /**
   * Starts the stat trackers for this job.
   */
  private void startTrackers()
  {
    modifiesCompleted.startTracker();
    modifyTimer.startTracker();
    resultCodes.startTracker();
    responseTimeCategorizer.startStatTracker();
  }



  /**
   * Stops the stat trackers for this job.
   */
  private void stopTrackers()
  {
    modifiesCompleted.stopTracker();
    modifyTimer.stopTracker();
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
