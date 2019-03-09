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



import java.util.ArrayList;
import java.util.Date;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.AccumulatingTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class implements a SLAMD job class that is intended for use in priming
 * an LDAP directory server.  It is very similar to the SearchRate job, but it
 * is designed specifically for priming, which reads all or a significant part
 * of the database.  Unfortunately, it is necessary to specify the number of
 * clients to use twice (once for the scheduling info and once as a job
 * parameter) because otherwise each client does not have any knowledge of the
 * total number of clients.  Note that this job expects the directory entries to
 * have a sequentially-incrementing counter as a value for one of the attributes
 * in it (it does not have to be the RDN attribute, but it should be indexed),
 * so it is unfortunately only feasible to use for contrived data sets.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPPrimeJobClass
       extends JobClass
{
  /**
   * The display name for the stat tracker that will be used to track the time
   * required to run each search.
   */
  public static final String STAT_TRACKER_SEARCH_TIME = "Search Time (ms)";



  /**
   * The display name for the stat tracker that will be used to track the number
   * of entries returned from each search.
   */
  public static final String STAT_TRACKER_ENTRY_COUNT = "Entries Returned";



  /**
   * The display name for the stat tracker that will be used to track the number
   * of successful searches.
   */
  public static final String STAT_TRACKER_SEARCHES_COMPLETED =
       "Searches Completed";



  /**
   * The display name for the stat tracker that will be used to accumulate the
   * total number of successful searches.
   */
  public static final String STAT_TRACKER_TOTAL_SEARCHES_COMPLETED =
       "Total Searches Completed";



  /**
   * The display name for the stat tracker that will be used to track the number
   * of exceptions caught.
   */
  public static final String STAT_TRACKER_EXCEPTIONS_CAUGHT =
       "Exceptions Caught";



  // The parameter that indicates whether the client should trust any SSL cert.
  private BooleanParameter blindTrustParameter =
    new BooleanParameter("blind_trust", "Blindly Trust Any Certificate",
                         "Indicates whether the client should blindly trust " +
                         "any certificate presented by the server, or " +
                         "whether the key and trust stores should be used.",
                         true);

  // The parameter that indicates whether the connection should use SSL or not
  private BooleanParameter useSSLParameter =
       new BooleanParameter("usessl", "Use SSL",
                            "Indicates whether to use SSL to encrypt the " +
                            "communication with the directory server", false);

  // The parameter that indicates the number of clients to use for the job.
  private IntegerParameter clientsParameter =
       new IntegerParameter("num_clients", "Number of Clients",
                            "The number of clients over which the priming " +
                            "should be distributed", true, 0, true, 1, false,
                            0);

  // The parameter that specifies the maximum request rate.
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Search Rate (Searches/Second/Client)",
       "Specifies the maximum search rate (in searches per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform searches as quickly as possible.",
       true, -1);

  // The parameter that indicates the port number for the directory server
  private IntegerParameter portParameter =
       new IntegerParameter("ldapport", "Directory Server Port",
                            "The port number for the LDAP directory server",
                            true, 389, true, 1, true, 65535);

  // The parameter that specifies the starting value for the attribute.
  private IntegerParameter rangeStartParameter =
       new IntegerParameter("rangestart", "Value Range Start",
                            "The value that should be used as the start of " +
                            "the range when priming the job.", true, 0, false,
                            0, false, 0);

  // The parameter that specifies the ending value for the attribute.
  private IntegerParameter rangeStopParameter =
       new IntegerParameter("rangestop", "Value Range End",
                            "The value that should be used as the end of " +
                            "the range when priming the job.", true, 0, false,
                            0, false, 0);

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

  // The parameter that indicates the maximum length of time to wait for results
  // to a search.
  private IntegerParameter timeLimitParameter =
       new IntegerParameter("timelimit", "Search Time Limit",
                            "The maximum length of time to wait for the " +
                            "result of a search operation (0 to wait forever)",
                            true, 0, true, 0, false, 0);

  // The placeholder parameter used as a spacer in the admin interface.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter that indicates the search base
  private StringParameter searchBaseParameter =
       new StringParameter("searchbase", "Search Base",
                           "The DN of the entry to use as the search base",
                           false, "");

  // The parameter that specifies the attribute name.
  private StringParameter attributeNameParameter =
       new StringParameter("attrname", "Attribute Name",
                           "The name of the LDAP attribute that contains the " +
                           "sequentially-incrementing value used to prime " +
                           "the directory", true, "");

  // The parameter that specifies the string to be prepended to the numeric
  // portion of the attribute value.
  private StringParameter valuePrefixParameter =
       new StringParameter("valueprefix", "Attribute Value Prefix",
                           "The static text that should be prepended to the " +
                           "numeric portion of the attribute value for use " +
                           "in the priming searches.", false, "");

  // The parameter that specifies the string to be appended to the numeric
  // portion of the attribute value.
  private StringParameter valueSuffixParameter =
       new StringParameter("valuesuffix", "Attribute Value Suffix",
                           "The static text that should be appended to the " +
                           "numeric portion of the attribute value for use " +
                           "in the priming searches.", false, "");

  // The parameter that indicates the DN to use when binding to the server
  private StringParameter bindDNParameter =
       new StringParameter("binddn", "Bind DN",
                           "The DN to use to bind to the server", false, "");

  // The parameter that indicates the address of the directory server
  private StringParameter hostParameter =
       new StringParameter("ldaphost", "Directory Server Host",
                           "The DNS hostname or IP address of the LDAP " +
                           "directory server", true, "");

  // The parameter that specifies the location of the SSL key store
  private StringParameter keyStoreParameter =
    new StringParameter("sslkeystore", "SSL Key Store",
                        "The path to the JSSE key store to use for an " +
                        "SSL-based connection", false, "");

  // The parameter that specifies the location of the SSL trust store
  private StringParameter trustStoreParameter =
    new StringParameter("ssltruststore", "SSL Trust Store",
                        "The path to the JSSE trust store to use for an " +
                        "SSL-based connection", false, "");

  // The parameter that indicates the bind password
  private PasswordParameter bindPWParameter =
       new PasswordParameter("bindpw", "Bind Password",
                             "The password for the bind DN", false, "");

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter keyPWParameter =
    new PasswordParameter("sslkeypw", "SSL Key Store Password",
                          "The password for the JSSE key store", false, "");

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter trustPWParameter =
    new PasswordParameter("ssltrustpw", "SSL Trust Store Password",
                          "The password for the JSSE trust store", false, "");


  // Instance variables that correspond to the parameter values
  private static boolean useSSL;
  private static int     ldapPort;
  private static int     numClients;
  private static int     rangeStart;
  private static int     rangeStop;
  private static int     timeLimit;
  private static String  attributeName;
  private static String  searchBase;
  private static String  bindDN;
  private static String  bindPassword;
  private static String  ldapHost;
  private static String  valuePrefix;
  private static String  valueSuffix;


  // The range that will be used for this client.
  private static int clientMax;
  private static int clientMin;


  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;


  // The range that will be used for this thread.
  private int threadMax;
  private int threadMin;


  // The LDAP connection that will be used to issue the searches.
  private LDAPConnection conn;


  // Variables used for status counters
  private AccumulatingTracker totalSearches;
  private IncrementalTracker  exceptionsCaught;
  private IncrementalTracker  successfulSearches;
  private IntegerValueTracker entryCount;
  private TimeTracker         searchTime;




  /**
   * The default constructor used to create a new instance of the search thread.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public LDAPPrimeJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Prime";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Prime directory server caches using LDAP search operations";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to attempt to prime the caches of an LDAP " +
      "directory server by performing search operations.  It is expected " +
      "that the directory server will have a data set containing an indexed " +
      "attribute whose value contains a sequentially-incrementing number."
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
    Parameter[] parameters = new Parameter[]
    {
      placeholder,
      hostParameter,
      portParameter,
      bindDNParameter,
      bindPWParameter,
      placeholder,
      searchBaseParameter,
      attributeNameParameter,
      rangeStartParameter,
      rangeStopParameter,
      valuePrefixParameter,
      valueSuffixParameter,
      timeLimitParameter,
      maxRateParameter,
      placeholder,
      useSSLParameter,
      blindTrustParameter,
      keyStoreParameter,
      keyPWParameter,
      trustStoreParameter,
      trustPWParameter
    };

    return new ParameterList(parameters);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getClientSideParameterStubs()
  {
    Parameter[] parameters = new Parameter[]
    {
      placeholder,
      hostParameter,
      portParameter,
      bindDNParameter,
      bindPWParameter,
      placeholder,
      searchBaseParameter,
      attributeNameParameter,
      rangeStartParameter,
      rangeStopParameter,
      valuePrefixParameter,
      valueSuffixParameter,
      timeLimitParameter,
      maxRateParameter,
      rateLimitDurationParameter,
      placeholder,
      useSSLParameter,
      blindTrustParameter,
      keyStoreParameter,
      keyPWParameter,
      trustStoreParameter,
      trustPWParameter,
      clientsParameter
    };

    return new ParameterList(parameters);
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
                             STAT_TRACKER_SEARCHES_COMPLETED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_EXCEPTIONS_CAUGHT,
                             collectionInterval),
      new IntegerValueTracker(clientID, threadID, STAT_TRACKER_ENTRY_COUNT,
                              collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME,
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
      successfulSearches,
      totalSearches,
      exceptionsCaught,
      entryCount,
      searchTime
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
    // This is sneaky.  We're going to pass the number of clients to the job as
    // a job parameter.  Otherwise, the job would have no idea how many clients
    // there are, and therefore would have no idea how to break up the work for
    // each client.
    IntegerParameter clientsParameter =
         new IntegerParameter("num_clients", numClients);
    parameters.addParameter(clientsParameter);
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
        outputMessages.add("ERROR:  Unable to instantiate the socket " +
             "factory for use in creating the SSL connection:  " +
             stackTraceToString(e));
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
      outputMessages.add("Attempting to establish a connection to " + host +
                         ':' + port + "....");
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
      if ((bindDN != null) && (bindDN.length() > 0) &&
          (bindPassword != null) && (bindPassword.length() > 0))
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
    // Get the address of the target directory server
    ldapHost = null;
    hostParameter = parameters.getStringParameter(hostParameter.getName());
    if (hostParameter != null)
    {
      ldapHost = hostParameter.getStringValue();
    }

    // Get the port for the target directory server
    ldapPort = 389;
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      ldapPort = portParameter.getIntValue();
    }

    // Get the bind DN for the target directory server
    bindDN = "";
    bindDNParameter = parameters.getStringParameter(bindDNParameter.getName());
    if (bindDNParameter != null)
    {
      bindDN = bindDNParameter.getStringValue();
    }

    // Get the bind password for the target directory server
    bindPassword = "";
    bindPWParameter =
         parameters.getPasswordParameter(bindPWParameter.getName());
    if (bindPWParameter != null)
    {
      bindPassword = bindPWParameter.getStringValue();
    }

    // Get the search base
    searchBase = "";
    searchBaseParameter =
         parameters.getStringParameter(searchBaseParameter.getName());
    if (searchBaseParameter != null)
    {
      searchBase = searchBaseParameter.getStringValue();
    }

    // Get the attribute name
    attributeName = null;
    attributeNameParameter =
         parameters.getStringParameter(attributeNameParameter.getName());
    if (attributeNameParameter != null)
    {
      attributeName = attributeNameParameter.getStringValue();
    }

    // Get the range start value.
    rangeStart = 0;
    rangeStartParameter =
         parameters.getIntegerParameter(rangeStartParameter.getName());
    if (rangeStartParameter != null)
    {
      rangeStart = rangeStartParameter.getIntValue();
    }

    // Get the range end value.
    rangeStop = Integer.MAX_VALUE;
    rangeStopParameter =
         parameters.getIntegerParameter(rangeStopParameter.getName());
    if (rangeStopParameter != null)
    {
      rangeStop = rangeStopParameter.getIntValue();
    }

    // Get the value prefix.
    valuePrefix = "";
    valuePrefixParameter =
         parameters.getStringParameter(valuePrefixParameter.getName());
    if ((valuePrefixParameter != null) && valuePrefixParameter.hasValue())
    {
      valuePrefix = valuePrefixParameter.getStringValue();
    }

    // Get the value suffix.
    valueSuffix = "";
    valueSuffixParameter =
         parameters.getStringParameter(valueSuffixParameter.getName());
    if ((valueSuffixParameter != null) && valueSuffixParameter.hasValue())
    {
      valueSuffix = valueSuffixParameter.getStringValue();
    }

    // Get the time limit per search.
    timeLimit = 0;
    timeLimitParameter =
         parameters.getIntegerParameter(timeLimitParameter.getName());
    if (timeLimitParameter != null)
    {
      timeLimit = timeLimitParameter.getIntValue();
    }

    // Get the maximum search rate.
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

    // Get the flag indicating whether we should use SSL or not.  If so, then
    // we'll blindly trust any certificate presented by the server and will
    // ignore any key store and trust store options that might have been
    // provided.
    useSSL = false;
    useSSLParameter = parameters.getBooleanParameter(useSSLParameter.getName());
    if (useSSLParameter != null)
    {
      useSSL = useSSLParameter.getBooleanValue();
    }

    // Get the total number of clients.
    numClients = 0;
    clientsParameter =
         parameters.getIntegerParameter(clientsParameter.getName());
    if (clientsParameter != null)
    {
      numClients = clientsParameter.getIntValue();
    }


    // Calculate the range of values that will be used by this client.
    int totalSpan     = rangeStop - rangeStart + 1;
    int spanPerClient = totalSpan / numClients;
    if ((totalSpan % numClients) != 0)
    {
      spanPerClient++;
    }

    int clientNumber = getClientNumber();
    if (clientNumber >= numClients)
    {
      throw new UnableToRunException("Detected that this client is client " +
                                     "number " + clientNumber +
                                     ", but the reported number of clients " +
                                     "was " + numClients +
                                     " -- skipping this client");
    }
    else if (clientNumber == (numClients - 1))
    {
      clientMin = (clientNumber * spanPerClient) + rangeStart;
      clientMax = rangeStop;
    }
    else
    {
      clientMin = (clientNumber * spanPerClient) + rangeStart;
      clientMax = clientMin + spanPerClient - 1;
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
    // Set up the status counters
    entryCount = new IntegerValueTracker(clientID, threadID,
                                         STAT_TRACKER_ENTRY_COUNT,
                                         collectionInterval);
    exceptionsCaught = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_EXCEPTIONS_CAUGHT,
                                              collectionInterval);
    searchTime = new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_TIME,
                                 collectionInterval);
    successfulSearches = new IncrementalTracker(clientID, threadID,
                                                STAT_TRACKER_SEARCHES_COMPLETED,
                                                collectionInterval);
    totalSearches =
         new AccumulatingTracker(clientID, threadID,
                                 STAT_TRACKER_TOTAL_SEARCHES_COMPLETED,
                                 collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      successfulSearches.enableRealTimeStats(statReporter, jobID);
      totalSearches.enableRealTimeStats(statReporter, jobID);
      exceptionsCaught.enableRealTimeStats(statReporter, jobID);
      entryCount.enableRealTimeStats(statReporter, jobID);
      searchTime.enableRealTimeStats(statReporter, jobID);
    }


    // Figure out the span to use for this particular thread.
    int numThreads    = getClientSideJob().getThreadsPerClient();
    int clientSpan    = clientMax - clientMin + 1;
    int spanPerThread = clientSpan / numThreads;
    if ((clientSpan % numThreads) != 0)
    {
      spanPerThread++;
    }

    int threadNumber = getThreadNumber();
    if (threadNumber == (numThreads - 1))
    {
      threadMin = (threadNumber * spanPerThread) + clientMin;
      threadMax = clientMax;
    }
    else
    {
      threadMin = (threadNumber * spanPerThread) + clientMin;
      threadMax = threadMin + spanPerThread - 1;
    }


    // If the connection is to use SSL, then establish a preliminary connection
    // now.  The first connection can take a significant amount of time to
    // establish, and we want to get it out of the way early before the timer
    // starts (if a duration is specified).  Don't worry about any exceptions
    // that may get thrown here because there's no easy way to report them back
    // but they will be repeated and handled when the job starts running anyway,
    // so no big deal.
    if (useSSL)
    {
      try
      {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        conn = new LDAPConnection(sslUtil.createSSLSocketFactory());

        conn.connect(ldapHost, ldapPort, 10000);
        if ((bindDN != null) && (bindDN.length() > 0) &&
            (bindPassword != null) && (bindPassword.length() > 0))
        {
          conn.bind(bindDN, bindPassword);
        }

        conn.close();
      }
      catch (Exception e) {}
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Establish the connection to the directory server.
    if (useSSL)
    {
      try
      {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        conn = new LDAPConnection(sslUtil.createSSLSocketFactory());
      }
      catch (Exception e)
      {
        logMessage(e.getMessage());
        indicateStoppedDueToError();
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
    }
    catch (LDAPException le)
    {
      logMessage("Unable to connect to directory server " + ldapHost + ':' +
                 ldapPort + ":  " + le);
      indicateCompletedWithErrors();
      return;
    }

    // Create the search request that will be used by this thread.
    SearchRequest searchRequest = new SearchRequest(searchBase, SearchScope.SUB,
         Filter.createEqualityFilter(attributeName, "x"));
    searchRequest.setTimeLimitSeconds(timeLimit);
    searchRequest.setResponseTimeoutMillis(1000L * timeLimit);


    // Tell the stat trackers that they should start tracking now
    successfulSearches.startTracker();
    totalSearches.startTracker();
    exceptionsCaught.startTracker();
    entryCount.startTracker();
    searchTime.startTracker();

    // Create a loop that will run until it needs to stop
    for (int i=threadMin; ((! shouldStop()) && (i <= threadMax)); i++)
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      // Create a flag that will be used to determine if the search was
      // successful or not
      boolean successfulSearch = false;


      // Create a counter that will be used to record the number of matching
      // entries
      int matchingEntries = 0;

      // Perform the search and iterate through all matching entries
      searchTime.startTimer();
      try
      {
        searchRequest.setFilter(Filter.createEqualityFilter(attributeName,
             valuePrefix + i + valueSuffix));
        SearchResult result = conn.search(searchRequest);
        successfulSearch = true;
        matchingEntries = result.getEntryCount();
      }
      catch (LDAPSearchException e)
      {
        exceptionsCaught.increment();
        matchingEntries = e.getEntryCount();
        indicateCompletedWithErrors();
      }
      catch (Exception e)
      {
        exceptionsCaught.increment();
        indicateCompletedWithErrors();
      }

      // Record the current time as the end of the search
      searchTime.stopTimer();


      // Update the appropriate status counters
      if (successfulSearch)
      {
        entryCount.addValue(matchingEntries);
        successfulSearches.increment();
        totalSearches.increment();
      }
    }


    // If the connection is still established, then close it
    conn.close();

    // Tell the stat trackers that they should stop tracking
    successfulSearches.stopTracker();
    totalSearches.stopTracker();
    exceptionsCaught.stopTracker();
    entryCount.stopTracker();
    searchTime.stopTracker();
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
}

