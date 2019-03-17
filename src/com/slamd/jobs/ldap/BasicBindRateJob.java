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

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
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
 * This class provides a SLAMD job that can measure the bind performance of an
 * LDAP directory server with a streamlined set of options.
 */
public final class BasicBindRateJob
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
  private StringParameter serverAddressParameter = new StringParameter(
       "address", "Directory Server Address",
       "The address of the directory server in which to bind.  It may be a " +
            "resolvable name or an IP address.",
       true, null);

  // The parameter used to specify the directory server port.
  private IntegerParameter serverPortParameter = new IntegerParameter(
       "port", "Directory Serve Port",
       "The port number of the directory server in which to bind.", true, 389,
       true, 1, true, 65535);

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
       "Bind Request Parameters");

  // The parameter used to specify the bind DN pattern.
  private StringParameter bindDNPatternParameter = new StringParameter(
       "bind_dn_pattern", "Bind DN Pattern",
       "A pattern to use to generate the bind DNs for LDAP simple " +
            "authentication.  This may be a fixed DN that should be reused " +
            "for all bind operations, or it may be a value pattern that can " +
            "generate a different bind DN for each request.  See " +
            "https://docs.ldap.com/ldap-sdk/docs/javadoc/com/unboundid/util/" +
            "ValuePattern.html for more information about value patterns.  " +
            "If no bind DN pattern is provided, then the password must also " +
            "be empty, and the job will perform anonymous simple binds.",
       false, null);

  // The password to use to bind to the directory server.
  private PasswordParameter bindPasswordParameter = new PasswordParameter(
       "bind_password", "Bind Password",
       "The password to use to bind to the directory server using LDAP " +
            "simple authentication.  If this is not provided, then the bind " +
            "DN must also be empty, and the job will perform anonymous " +
            "simple binds.",
       false, null);

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
  private static long coolDownDurationMillis = -1L;
  private static long warmUpDurationMillis = -1L;
  private static SingleServerSet serverSet = null;
  private static StartTLSPostConnectProcessor startTLSProcessor = null;
  private static String bindPassword = null;
  private static ValuePattern bindDNPattern = null;


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
  public BasicBindRateJob()
  {
    super();

    connectionPool = null;

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
    return "Basic Bind Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Perform repeated LDAP simple bind operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to perform repeated simple bind operations " +
           "against an LDAP directory server with a streamlined set of options."
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
      bindDNPatternParameter,
      bindPasswordParameter,

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
    // The bind DN pattern and password must be both empty or both non-empty.
    // If a bind DN pattern is specified, then it must be a valid value pattern.
    // Note that we don't want to ensure that it can be used to generate valid
    // distinguished names because some protocol-violating directory servers
    // allow LDAP simple binds with bind DNs that aren't actually DNs and we
    // will allow testing with those as well.
    final StringParameter bindDNPatternParam =
         parameters.getStringParameter(bindDNPatternParameter.getName());
    final PasswordParameter bindPasswordParam =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((bindDNPatternParam != null) && bindDNPatternParam.hasValue())
    {
      if ((bindPasswordParam != null) && bindPasswordParam.hasValue())
      {
        final String pattern = bindDNPatternParam.getValueString();
        try
        {
          new ValuePattern(pattern);
        }
        catch (final ParseException e)
        {
          throw new InvalidValueException(
               "Unable to parse bind DN pattern value '" + pattern +
                    "' as a valid value pattern:  " + e.getMessage(),
               e);
        }
      }
      else
      {
        throw new InvalidValueException("If a bind DN pattern is provided, " +
             "then a bind password must also be provided.");
      }
    }
    else
    {
      throw new InvalidValueException("If a bind password is provided, then " +
           "a bind DN pattern must also be provided.");
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


      // If a bind DN pattern and password were provided, then verify that we
      // can bind with a generated set of credentials.  If not, then try an
      // anonymous simple bind.
      final StringParameter bindDNPatternParam =
           parameters.getStringParameter(bindDNPatternParameter.getName());
      final PasswordParameter bindPWParam =
           parameters.getPasswordParameter(bindPasswordParameter.getName());
      if ((bindDNPatternParam != null) && bindDNPatternParam.hasValue())
      {
        outputMessages.add("");
        final String bindDN;
        final String pattern = bindDNPatternParam.getValueString();
        try
        {
          bindDN = new ValuePattern(pattern).nextValue();
        }
        catch (final ParseException e)
        {
          outputMessages.add("ERROR:  Unable to generate a bind DN from " +
               "pattern '" + pattern + "':  " +
               StaticUtils.getExceptionMessage(e));
          return false;
        }

        outputMessages.add("Attempting an LDAP simple bind with a DN of '" +
             bindDN + "' (generated from the bind DN pattern) and the " +
             "provided password...");
        try
        {
          connection.bind(bindDN, bindPWParam.getValueString());
          outputMessages.add("Bind succeeded.");
        }
        catch (final LDAPException e)
        {
          if (e.getDiagnosticMessage() == null)
          {
            outputMessages.add("ERROR:  Bind failed with result code " +
                 e.getResultCode());
          }
          else
          {
            outputMessages.add("ERROR:  Bind failed with result code " +
                 e.getResultCode() + " and diagnostic message " +
                 e.getDiagnosticMessage());
          }

          return false;
        }
      }
      else
      {
        outputMessages.add("");
        outputMessages.add("Trying to perform an anonymous simple bind...");
        try
        {
          connection.bind("", "");
          outputMessages.add("Bind succeeded.");
        }
        catch (final Exception e)
        {
          outputMessages.add("ERROR:  Bind failed:  " +
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


    // Initialize the bind DN pattern.
    final StringParameter bindDNPatternParam =
         parameters.getStringParameter(bindDNPatternParameter.getName());
    try
    {
      if ((bindDNPatternParam != null) && bindDNPatternParam.hasValue())
      {
        bindDNPattern = new ValuePattern(bindDNPatternParam.getValueString());
      }
      else
      {
        bindDNPattern = new ValuePattern("");
      }
    }
    catch (final ParseException e)
    {
      throw new UnableToRunException(
           "Unable to create the bind DN pattern:  " +
                StaticUtils.getExceptionMessage(e),
           e);
    }


    // Initialize the bind password.
    final PasswordParameter bindPWParam =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((bindPWParam != null) && bindPWParam.hasValue())
    {
      bindPassword = bindPWParam.getValueString();
    }
    else
    {
      bindPassword = "";
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


    // Create a connection pool to use to process the binds.  Each thread
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
    while (! shouldStop())
    {
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


      // Generate the simple bind request to send to the server.
      final SimpleBindRequest bindRequest =
           new SimpleBindRequest(bindDNPattern.nextValue(), bindPassword);


      // Process the bind.
      if (collectingStats)
      {
        bindTimer.startTimer();
      }

      try
      {
        final long beforeBindTimeNanos = System.nanoTime();
        final LDAPResult bindResult = connectionPool.bind(bindRequest);
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
