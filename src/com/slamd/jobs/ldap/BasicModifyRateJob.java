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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ValuePattern;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.slamd.common.DurationParser;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.MultiChoiceParameter;
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
 * LDAP directory server with a streamlined set of options.
 */
public final class BasicModifyRateJob
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
  private StringParameter serverAddressParameter = new StringParameter(
       "address", "Directory Server Address",
       "The address of the directory server in which to process the modify " +
            "operations.  It may be a resolvable name or an IP address.",
       true, null);

  // The parameter used to specify the directory server port.
  private IntegerParameter serverPortParameter = new IntegerParameter(
       "port", "Directory Serve Port",
       "The port number of the directory server in which to process the " +
            "modify operations.",
       true, 389, true, 1, true, 65535);

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

  // The parameter used to specify the entry DN pattern.
  private StringParameter entryDNPatternParameter = new StringParameter(
       "entry_dn_pattern", "Entry DN Pattern",
       "A pattern to use to generate the DN of the entry to target for each " +
            "modify request.  This may be a fixed DN to use for all modify " +
            "operations, or it may be a value pattern that can construct a " +
            "different entry DN for each request.  See https://docs.ldap.com/" +
            "ldap-sdk/docs/javadoc/com/unboundid/util/ValuePattern.html for " +
            "more information about value patterns.",
       true, null);

  // The parameter used to specify the name of the attribute to modify.
  private StringParameter attributeToModifyParameter = new StringParameter(
       "modify_attribute", "Attribute to Modify",
       "The name or OID of the attribute whose values should be replaced.",
       true, "description");

  // The parameter used to specify the set of characters to use when generating
  // the values to replace.
  private StringParameter characterSetParameter = new StringParameter(
       "character_set", "Generated Value Character Set",
       "The set of characters that may be included in generated values.",
       true, "abcdefghijklmnopqrstuvwxyz");

  // The parameter used to specify the number of values to generate for the
  // specified attribute.
  private IntegerParameter valueLengthParameter = new IntegerParameter(
       "value_length", "Value Length",
       "The number of characters to include in the generated values.",
       true, 80, true, 1, false, Integer.MAX_VALUE);

  // The parameter used to specify the number of values to generate for the
  // specified attribute.
  private IntegerParameter numberOfValuesParameter = new IntegerParameter(
       "number_of_values", "Number of Values",
       "The number of values to generate for the specified attribute.",
       true, 1, true, 0, false, Integer.MAX_VALUE);

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


  // Variables needed to perform processing using the parameter values.  These
  // should be static so that the values are shared across all threads.
  private static char[] characterSet = null;
  private static int numberOfValues = -1;
  private static int valueLength = -1;
  private static long coolDownDurationMillis = -1L;
  private static long warmUpDurationMillis = -1L;
  private static SimpleBindRequest bindRequest = null;
  private static SingleServerSet serverSet = null;
  private static StartTLSPostConnectProcessor startTLSProcessor = null;
  private static String attributeToModify = null;
  private static ValuePattern entryDNPattern = null;


  // A connection pool that this thread may use to communicate with the
  // directory server.  Each thread will have its own pool with just a single
  // connection, so this should be non-static.  We're not sharing the pool
  // across the threads, which will reduce contention, but the benefit of using
  // a pool over a simple connection is that it can automatically re-establish
  // a connection if it becomes invalid for some reason.
  private LDAPConnectionPool connectionPool;


  // Random number generators to use for generating values.  There will be a
  // parent random, which is shared by all threads, and a per-thread random that
  // will be used to actually generate the values.
  private static Random parentRandom = null;
  private Random random = null;


  // Arguments used for generating values.
  private Set<String> valueSet;
  private StringBuilder valueBuffer;


  // Stat trackers used by this job.  We should have a separate copy per thread,
  // so these should be non-static.
  private CategoricalTracker resultCodes;
  private IncrementalTracker modifiesCompleted;
  private ResponseTimeCategorizer responseTimeCategorizer;
  private TimeTracker modifyTimer;



  /**
   * Creates a new instance of this job class.
   */
  public BasicModifyRateJob()
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
    return "Basic Modify Rate";
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
           "LDAP directory server with a streamlined set of options."
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
      entryDNPatternParameter,
      attributeToModifyParameter,
      characterSetParameter,
      valueLengthParameter,
      numberOfValuesParameter,

      new PlaceholderParameter(),
      additionalLabelParameter,
      warmUpDurationParameter,
      coolDownDurationParameter,

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


    // Make sure that we can generate a valid LDAP DN from the entry DN pattern.
    final StringParameter entryDNParam =
         parameters.getStringParameter(entryDNPatternParameter.getName());
    if ((entryDNParam != null) && (entryDNParam.hasValue()))
    {
      final ValuePattern valuePattern;
      try
      {
        valuePattern = new ValuePattern(entryDNParam.getValue());
      }
      catch (final ParseException e)
      {
        throw new InvalidValueException(
             "Unable to parse entry DN pattern value '" +
                  entryDNParam.getValue() + "' as a valid value pattern:  " +
                  e.getMessage(),
             e);
      }

      final String entryDNString = valuePattern.nextValue();
      try
      {
        new DN(entryDNString);
      }
      catch (final LDAPException e)
      {
        throw new InvalidValueException(
             "Unable to parse a constructed entry DN '" + entryDNString +
                  "' as a valid LDAP distinguished name:  " + e.getMatchedDN(),
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


      // If an entry DN pattern was provided, then make sure we can retrieve an
      // entry whose DN was generated from that pattern.
      final StringParameter entryDNParam =
           parameters.getStringParameter(entryDNPatternParameter.getName());
      if ((entryDNParam != null) && entryDNParam.hasValue())
      {
        outputMessages.add("");

        String baseDN;
        try
        {
          final ValuePattern valuePattern =
               new ValuePattern(entryDNParam.getValue());
          baseDN = valuePattern.nextValue();
        }
        catch (final ParseException e)
        {
          outputMessages.add("ERROR:  Unable to construct an entry DN from " +
               "pattern '" + entryDNParam.getValue() + "':  " +
               StaticUtils.getExceptionMessage(e));
          return false;
        }

        outputMessages.add("Verifying that entry '" + baseDN +
             "' (generated from the entry DN pattern) exists...");
        try
        {
          final Entry entry = connection.getEntry(baseDN);
          if (entry == null)
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
    // Initialize the parent random number generator.
    parentRandom = new Random();


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


    // Initialize the entry DN pattern.
    final StringParameter entryDNParam = parameters.getStringParameter(
         entryDNPatternParameter.getName());
    try
    {
      entryDNPattern = new ValuePattern(entryDNParam.getValue());
    }
    catch (final Exception e)
    {
      throw new UnableToRunException(
           "Unable to initialize the entry DN value pattern:  " +
                StaticUtils.getExceptionMessage(e),
           e);
    }


    // Initialize the attribute to modify.
    attributeToModify = parameters.getStringParameter(
         attributeToModifyParameter.getName()).getStringValue();


    // Initialize the character set.
    characterSet = parameters.getStringParameter(
         characterSetParameter.getName()).getStringValue().toCharArray();


    // Initialize the value length.
    valueLength = parameters.getIntegerParameter(
         valueLengthParameter.getName()).getIntValue();


    // Initialize the number of values.
    numberOfValues = parameters.getIntegerParameter(
         numberOfValuesParameter.getName()).getIntValue();


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
    // Initialize the thread-specific random number generator.
    random = new Random(parentRandom.nextLong());


    // Initialize the value generation variables.
    valueSet = new HashSet<>(StaticUtils.computeMapCapacity(numberOfValues));
    valueBuffer = new StringBuilder(valueLength);


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


    // Create a connection pool to use to process the modify operations.  Each
    // thread will have its own pool with just a single connection.  We're not
    // sharing the pool across the connections, but the benefit of the pool is
    // that it will automatically re-establish connections that might have
    // become invalid for some reason.
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


    // Perform the modify operations until it's time to stop.
    boolean doneCollecting = false;
    while (! shouldStop())
    {
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


      // Generate the modify request.
      final ModifyRequest modifyRequest = generateModifyRequest();


      // Process the modify operation.
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
   * Generates the modify request.
   */
  private ModifyRequest generateModifyRequest()
  {
    valueSet.clear();
    for (int i=0; i < numberOfValues; i++)
    {
      valueSet.add(generateValue());
    }

    return new ModifyRequest(entryDNPattern.nextValue(),
         new Modification(ModificationType.REPLACE, attributeToModify,
              valueSet.toArray(StaticUtils.NO_STRINGS)));
  }



  /**
   * Generates a value for the modification.
   *
   * @return  The generated value.
   */
  private String generateValue()
  {
    valueBuffer.setLength(0);
    for (int i=0; i < valueLength; i++)
    {
      valueBuffer.append(characterSet[random.nextInt(characterSet.length)]);
    }

    return valueBuffer.toString();
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
