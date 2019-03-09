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



import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.slamd.http.HTTPClient;
import com.slamd.http.HTTPRequest;
import com.slamd.http.HTTPResponse;
import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.StatTracker;

import com.unboundid.util.FixedRateBarrier;



/**
 * This class implements a SLAMD job class for performing repeated HTTP GET
 * requests against a Web server.  All of the configuration for this job class
 * can be provided through parameters.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTTPGetRateJobClass
       extends JobClass
{
  /**
   * The system property used to specify the location of the JSSE key store.
   */
  public static final String SSL_KEY_STORE_PROPERTY =
       "javax.net.ssl.keyStore";



  /**
   * The system property used to specify the password for the JSSE key store.
   */
  public static final String SSL_KEY_PASSWORD_PROPERTY =
       "javax.net.ssl.keyStorePassword";



  /**
   * The system property used to specify the location of the JSSE trust store.
   */
  public static final String SSL_TRUST_STORE_PROPERTY =
       "javax.net.ssl.trustStore";



  /**
   * The system property used to specify the password for the JSSE trust store.
   */
  public static final String SSL_TRUST_PASSWORD_PROPERTY =
       "javax.net.ssl.trustStorePassword";



  /**
   * The display name for the stat tracker that will be used to track the number
   * of exceptions caught.
   */
  public static final String STAT_TRACKER_EXCEPTIONS_CAUGHT =
       "Exceptions Caught";



  // The parameter that indicates whether the client should trust any SSL cert.
  private BooleanParameter blindTrustParameter =
       new BooleanParameter("blind_trust", "Blindly Trust Any Certificate",
                            "Indicates whether the client should blindly " +
                            "trust any certificate presented by the server, " +
                            "or whether the key and trust stores should be " +
                            "used.", true);

  // The parameter that indicates whether to automatically follow any redirects
  // that are encountered.
  private BooleanParameter followRedirectsParameter =
      new BooleanParameter("follow_redirects", "Follow Redirects",
                           "Indicates whether the client should " +
                           "automatically follow any HTTP redirects that may " +
                           "be returned while retrieving a URL.", true);

  // The parameter that indicates whether associated files should automatically
  // be retrieved.
  private BooleanParameter retrieveAssociatedFilesParameter =
       new BooleanParameter("retrieve_associated_files",
                            "Retrieve Associated Files",
                            "Indicates whether to automatically retrieve " +
                            "associated files (e.g., images, frames, remote " +
                            "stylesheets, etc.) when retrieving an HTML " +
                            "document.", false);

  // The parameter that indicates whether to use a persistent connection.
  private BooleanParameter useKeepaliveParameter =
       new BooleanParameter("use_keepalive", "Use KeepAlive",
                            "Indicates whether the KeepAlive feature should " +
                            "be used to keep the connection open for " +
                            "multiple requests.", false);

  // The parameter that specifies the URL to a file containing a list of URLs to
  // retrieve.
  private FileURLParameter urlFileParameter =
      new FileURLParameter("url_file", "File with URLs to Retrieve",
                           "The URL to a file containing a list of URLs to " +
                           "retrieve.", null, false);

  // The parameter that specifies the cool down time.
  private IntegerParameter coolDownTimeParameter =
       new IntegerParameter("cool_down_time", "Cool Down Time",
                            "The length of time in seconds before the job " +
                            "stops running that the client should stop " +
                            "collecting statistics.  Note that this cannot " +
                            "always be accurate, particularly when no stop " +
                            "time or duration is provided.", true, 0, true, 0,
                            false, 0);

  // The parameter that specifies the number of iterations.
  private IntegerParameter iterationsParameter =
       new IntegerParameter("iterations", "Number of Iterations",
                            "The number of requests that each thread should " +
                            "issue to the server before completing (a value " +
                            "less than zero indicates no limit.", true, -1,
                            false, 0, false, 0);

  // The parameter that specifies the time between requests.
  private IntegerParameter delayParameter =
       new IntegerParameter("delay", "Time Between Requests (ms)",
                            "The minimum length of time in milliseconds that " +
                            "should pass between issuing one request and " +
                            "issuing the next.  Note that if it takes longer " +
                            "than this length of time to process the request " +
                            "then no sleep will be performed.", true, 0, true,
                            0, false, 0);

  // The parameter that specifies the maximum request rate.
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Request Rate (Requests/Second/Client)",
       "Specifies the maximum request rate (in requests per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform requests as quickly as possible.",
       true, -1);

  // The parameter that specifies the port to use for the proxy server.
  private IntegerParameter proxyPortParameter =
       new IntegerParameter("proxy_port", "Proxy Server Port",
                            "The port to use to communicate with the HTTP " +
                            "proxy server.", false, 8080, true, 1, true, 65535);

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

  // The parameter that specifies the warm up time.
  private IntegerParameter warmUpTimeParameter =
       new IntegerParameter("warm_up_time", "Warm Up Time",
                            "The length of time in seconds before the job " +
                            "stops running that the client should stop " +
                            "collecting statistics.  Note that this cannot " +
                            "always be accurate, particularly when no stop " +
                            "time or duration is provided.", true, 0, true, 0,
                            false, 0);

  // The parameter that specifies the set of client addresses that may be used
  // for the job.
  private MultiLineTextParameter clientAddressesParameter =
       new MultiLineTextParameter("client_addresses", "Client Addresses",
                                  "Specifies a set of IP addresses that " +
                                  "should be used as the source address when " +
                                  "establishing the HTTP connections.  Each " +
                                  "client thread will use a different client " +
                                  "address (although some threads may share " +
                                  "the same address if fewer addresses were " +
                                  "given than the number of client threads " +
                                  "requested).  This option is only " +
                                  "available for use if the job will run " +
                                  "using only one client.", null, false);

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter keyPWParameter =
       new PasswordParameter("sslkeypw", "SSL Key Store Password",
                             "The password for the JSSE key store.  This " +
                             "only applies for HTTPS URLs.", false, "");

  // The parameter that specifies the password to use to authenticate to the
  // proxy server.
  private PasswordParameter proxyUserPWParameter =
       new PasswordParameter("proxy_user_pw", "Proxy User Password",
                             "The password to use to authenticate to the " +
                             "proxy server if one is required.", false, "");

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter trustPWParameter =
       new PasswordParameter("ssltrustpw", "SSL Trust Store Password",
                             "The password for the JSSE trust store.  This " +
                             "only applies for HTTPS URLs.", false, "");

  // A placeholder parameter that is only used for formatting.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter that specifies the location of the SSL key store
  private StringParameter keyStoreParameter =
       new StringParameter("sslkeystore", "SSL Key Store",
                           "The path to the JSSE key store to use for an " +
                           "SSL-based connection.  This only applies for " +
                           "HTTPS URLs.", false, "");

  // The parameter that specifies the address of the proxy server to use.
  private StringParameter proxyHostParameter =
       new StringParameter("proxy_host", "Proxy Server Address",
                           "The address of the HTTP proxy server to use if " +
                           "one is required.", false, "");

  // The parameter that specifies the user ID to use to authenticate to the
  // proxy server.
  private StringParameter proxyUserIDParameter =
       new StringParameter("proxy_user_id", "Proxy User ID",
                           "The user ID to use to authenticate to the proxy " +
                           "server if one is required.", false, "");

  // The parameter that specifies the location of the SSL trust store
  private StringParameter trustStoreParameter =
       new StringParameter("ssltruststore", "SSL Trust Store",
                           "The path to the JSSE trust store to use for an " +
                           "SSL-based connection.  This only applies for " +
                           "HTTPS URLs.", false, "");

  // The parameter that provides the URL to retrieve.
  private StringParameter urlParameter =
       new StringParameter("url", "URL To Retrieve",
                           "The URL of the page to be retrieved.", false, "");


  // Instance variables that correspond to the parameter values.
  private static boolean       blindTrust;
  private static boolean       followRedirects;
  private static boolean       retrieveAssociatedFiles;
  private static boolean       useKeepAlive;
  private static HTTPRequest[] requests;
  private static int           coolDownTime;
  private static int           numIterations;
  private static int           proxyPort;
  private static int           timeBetweenRequests;
  private static int           warmUpTime;
  private static String        proxyHost;
  private static String        proxyUserID;
  private static String        proxyUserPW;
  private static String        sslKeyStore;
  private static String        sslKeyStorePassword;
  private static String        sslTrustStore;
  private static String        sslTrustStorePassword;
  private static String[]      clientAddresses;


  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;


  // The HTTP client that will be used by this thread to actually handle the
  // requests.
  private HTTPClient httpClient;


  // The stat tracker that will be used to track the number of exceptions
  // caught.
  private IncrementalTracker exceptionsCaught;


  // A random number generator to use for the client and the thread.
  private static Random parentRandom;
  private Random random;



  /**
   * Creates a new instance of this HTTP GetRate job class.
   */
  public HTTPGetRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "HTTP GetRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Issue repeated HTTP GET requests against a web server";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to issue repeated HTTP GET requests to a web " +
      "server to retrieve one or more URLs."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "HTTP";
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
      urlParameter,
      urlFileParameter,
      useKeepaliveParameter,
      followRedirectsParameter,
      retrieveAssociatedFilesParameter,
      placeholder,
      proxyHostParameter,
      proxyPortParameter,
      proxyUserIDParameter,
      proxyUserPWParameter,
      placeholder,
      warmUpTimeParameter,
      coolDownTimeParameter,
      iterationsParameter,
      delayParameter,
      maxRateParameter,
      rateLimitDurationParameter,
      clientAddressesParameter,
      placeholder,
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
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    StatTracker[] clientStubs =
         new HTTPClient().getStatTrackerStubs(clientID, threadID,
                                         collectionInterval);

    StatTracker[] stubs = new StatTracker[clientStubs.length+1];
    System.arraycopy(clientStubs, 0, stubs, 0, clientStubs.length);
    stubs[clientStubs.length] =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_EXCEPTIONS_CAUGHT,
                                collectionInterval);

    return stubs;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    StatTracker[] clientStats = httpClient.getStatTrackers();

    StatTracker[] stats = new StatTracker[clientStats.length+1];
    System.arraycopy(clientStats, 0, stats, 0, clientStats.length);
    stats[clientStats.length] = exceptionsCaught;

    return stats;
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
    // See if the user has provided either a single URL or a file with a list of
    // URLs.
    StringParameter urlParam =
         parameters.getStringParameter(urlParameter.getName());
    FileURLParameter urlFileParam =
         parameters.getFileURLParameter(urlFileParameter.getName());

    if (((urlParam == null) || (! urlParam.hasValue())) &&
        ((urlFileParam == null) || (! urlFileParam.hasValue())))
    {
      throw new InvalidValueException("A value must be provided for either " +
                                      "the \"" + urlParameter.getDisplayName() +
                                      "\" parameter or the \"" +
                                      urlFileParameter.getDisplayName() +
                                      "\" parameter.");
    }


    if ((urlParam != null) && urlParam.hasValue() &&
        (urlFileParam != null) && urlFileParam.hasValue())
    {
      throw new InvalidValueException("You may not provide a value for both " +
                                      "the \"" + urlParameter.getDisplayName() +
                                      "\" parameter and the \"" +
                                      urlFileParameter.getDisplayName() +
                                      "\" parameter.");
    }


    if ((urlParam != null) && urlParam.hasValue())
    {
      try
      {
        URL url = new URL(urlParam.getStringValue());
      }
      catch (Exception e)
      {
        throw new InvalidValueException("Unable to parse the provided URL \"" +
                                        urlParam.getStringValue() + "\":  " +
                                        e, e);
      }
    }


    if (numClients > 1)
    {
      MultiLineTextParameter clientAddrsParam =
           parameters.getMultiLineTextParameter(
                clientAddressesParameter.getName());
      if ((clientAddrsParam != null) && clientAddrsParam.hasValue())
      {
        throw new InvalidValueException("The set of client addresses may " +
                                        "only be specified if a single client" +
                                        "is to be used.");
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
    // Get the necessary parameter values.
    URL urlToRetrieve = null;
    StringParameter urlParam =
         parameters.getStringParameter(urlParameter.getName());
    if ((urlParam != null) && urlParam.hasValue())
    {
      try
      {
        urlToRetrieve = new URL(urlParam.getStringValue());
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to parse URL to retrieve '" +
                           urlParam.getStringValue() + "':  " +
                           stackTraceToString(e));
        return false;
      }
    }
    else
    {
      FileURLParameter urlFileParam =
           parameters.getFileURLParameter(urlFileParameter.getName());
      if ((urlFileParam == null) || (! urlFileParam.hasValue()))
      {
        outputMessages.add("ERROR:  Either a URL to retrieve or a URL file " +
                           "must be provided.");
        return false;
      }
      else
      {
        try
        {
          String[] urls = urlFileParam.getNonBlankFileLines();
          if ((urls == null) || (urls.length == 0))
          {
            outputMessages.add("ERROR:  The file containing the URLs to " +
                               "retrieve, '" +
                               urlFileParam.getFileURL().toExternalForm() +
                               "', appears to be empty.");
            return false;
          }
          else
          {
            try
            {
              urlToRetrieve = new URL(urls[0]);
            }
            catch (Exception e)
            {
              outputMessages.add("ERROR:  Unable to parse URL to retrieve '" +
                                 urls[0] + "':  " + stackTraceToString(e));
              return false;
            }
          }
        }
        catch (Exception e)
        {
          outputMessages.add("ERROR:  Unable to retrieve the URL file '" +
                             urlFileParam.getFileURL().toExternalForm() +
                             "':  " + stackTraceToString(e));
        }
      }
    }


    String proxyHost = null;
    StringParameter proxyHostParam =
         parameters.getStringParameter(proxyHostParameter.getName());
    if ((proxyHostParam != null) && proxyHostParam.hasValue())
    {
      proxyHost = proxyHostParam.getStringValue();
    }


    int proxyPort = -1;
    IntegerParameter proxyPortParam =
         parameters.getIntegerParameter(proxyPortParameter.getName());
    if ((proxyPortParam != null) && proxyPortParam.hasValue())
    {
      proxyPort = proxyPortParam.getIntValue();
    }


    String proxyUserID = null;
    StringParameter proxyUserIDParam =
         parameters.getStringParameter(proxyUserIDParameter.getName());
    if ((proxyUserIDParam != null) && proxyUserIDParam.hasValue())
    {
      proxyUserID = proxyUserIDParam.getStringValue();
    }


    String proxyUserPW = null;
    PasswordParameter proxyUserPWParam =
         parameters.getPasswordParameter(proxyUserPWParameter.getName());
    if ((proxyUserPWParam != null) && proxyUserPWParam.hasValue())
    {
      proxyUserPW = proxyUserPWParam.getStringValue();
    }


    boolean followRedirects = false;
    BooleanParameter redirectsParam =
         parameters.getBooleanParameter(followRedirectsParameter.getName());
    if (redirectsParam != null)
    {
      followRedirects = redirectsParam.getBooleanValue();
    }


    boolean blindTrust = true;
    BooleanParameter blindTrustParam =
         parameters.getBooleanParameter(blindTrustParameter.getName());
    if (blindTrustParam != null)
    {
      blindTrust = blindTrustParam.getBooleanValue();
    }


    String keyStore = null;
    StringParameter keyStoreParam =
         parameters.getStringParameter(keyStoreParameter.getName());
    if ((keyStoreParam != null) && keyStoreParam.hasValue())
    {
      keyStore = keyStoreParam.getStringValue();
      File keyStoreFile = new File(keyStore);
      if ((! blindTrust) && (! keyStoreFile.exists()))
      {
        outputMessages.add("WARNING:  Key store file \"" + keyStore +
                           "\" not found on SLAMD server system.  This test " +
                           "will blindly trust any SSL certificate " +
                           "presented by the directory server.");
        outputMessages.add("");
        blindTrust = true;
      }
      else
      {
        System.setProperty(SSL_KEY_STORE_PROPERTY, keyStore);
      }
    }


    String keyStorePassword = "";
    StringParameter keyPassParam =
         parameters.getStringParameter(keyPWParameter.getName());
    if ((keyPassParam != null) && keyPassParam.hasValue())
    {
      keyStorePassword = keyPassParam.getStringValue();
      System.setProperty(SSL_KEY_PASSWORD_PROPERTY, keyStorePassword);
    }


    String trustStore = null;
    StringParameter trustStoreParam =
         parameters.getStringParameter(trustStoreParameter.getName());
    if ((trustStoreParam != null) && trustStoreParam.hasValue())
    {
      trustStore = trustStoreParam.getStringValue();
      File trustStoreFile = new File(trustStore);
      if ((! blindTrust) && (! trustStoreFile.exists()))
      {
        outputMessages.add("WARNING:  trust store file \"" + trustStore +
                           "\" not found on SLAMD server system.  This test " +
                           "will blindly trust any SSL certificate " +
                           "presented by the directory server.");
        outputMessages.add("");
        blindTrust = true;
      }
      else
      {
        System.setProperty(SSL_TRUST_STORE_PROPERTY, trustStore);
      }
    }


    String trustStorePassword = "";
    StringParameter trustPassParam =
         parameters.getStringParameter(trustPWParameter.getName());
    if ((trustPassParam != null) && trustPassParam.hasValue())
    {
      trustStorePassword = trustPassParam.getStringValue();
      System.setProperty(SSL_TRUST_PASSWORD_PROPERTY, trustStorePassword);
    }


    // Create an HTTPClient to use to retrieve the URL.
    HTTPClient client = new HTTPClient();
    client.setFollowRedirects(followRedirects);

    if ((proxyHost != null) && (proxyPort > 0))
    {
      client.enableProxy(proxyHost, proxyPort, proxyUserID, proxyUserPW);
    }

    if (blindTrust)
    {
      try
      {
        client.setSSLSocketFactory(new JSSEBlindTrustSocketFactory());
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to instantiate blind trust socket " +
                           "factory:  " + stackTraceToString(e));
        return false;
      }
    }


    // Construct the request and send it to the server.
    try
    {
      outputMessages.add("Attempting to retrieve URL '" +
                         urlToRetrieve.toExternalForm() + "'....");

      HTTPRequest request = new HTTPRequest(true, urlToRetrieve);
      HTTPResponse response = client.sendRequest(request);

      int    statusCode      = response.getStatusCode();
      String responseMessage = response.getResponseMessage();

      outputMessages.add("HTTP response status code was " + statusCode + '.');
      if ((responseMessage != null) && (responseMessage.length() > 0))
      {
        outputMessages.add("HTTP response message was " + responseMessage +
                           '.');
      }

      outputMessages.add("");
      outputMessages.add("All tests completed.");
      client.closeAll();
      return true;
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to perform the GET:  " +
                         stackTraceToString(e));
      client.closeAll();
      return false;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    urlParameter = parameters.getStringParameter(urlParameter.getName());
    if ((urlParameter != null) && urlParameter.hasValue())
    {
      try
      {
        HTTPRequest request =
             new HTTPRequest(true, new URL(urlParameter.getStringValue()));
        requests = new HTTPRequest[] { request };
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to create HTTP request based " +
                                       "on provided URL \"" +
                                       urlParameter.getStringValue() + "\":  " +
                                       e, e);
      }
    }

    urlFileParameter =
         parameters.getFileURLParameter(urlFileParameter.getName());
    if ((urlFileParameter != null) && urlFileParameter.hasValue())
    {
      try
      {
        String[] urls = urlFileParameter.getFileLines();
        requests = new HTTPRequest[urls.length];
        for (int i=0; i < requests.length; i++)
        {
          requests[i] = new HTTPRequest(true, new URL(urls[i]));
        }
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to obtain or parse the set of " +
                                       "URLs to retrieve:  " + e, e);
      }
    }


    // See if we should use HTTP keepalive.
    useKeepAlive = false;
    useKeepaliveParameter =
         parameters.getBooleanParameter(useKeepaliveParameter.getName());
    if (useKeepaliveParameter != null)
    {
      useKeepAlive = useKeepaliveParameter.getBooleanValue();
    }


    // See if we should follow redirects.
    followRedirects = true;
    followRedirectsParameter =
         parameters.getBooleanParameter(followRedirectsParameter.getName());
    if (followRedirectsParameter != null)
    {
      followRedirects = followRedirectsParameter.getBooleanValue();
    }


    // See if we should automatically retrieve associated files.
    retrieveAssociatedFiles = false;
    retrieveAssociatedFilesParameter =
         parameters.getBooleanParameter(
              retrieveAssociatedFilesParameter.getName());
    if (retrieveAssociatedFilesParameter != null)
    {
      retrieveAssociatedFiles =
           retrieveAssociatedFilesParameter.getBooleanValue();
    }


    // See if we should use a proxy host.
    proxyHost = null;
    proxyHostParameter =
         parameters.getStringParameter(proxyHostParameter.getName());
    if ((proxyHostParameter != null) && proxyHostParameter.hasValue())
    {
      proxyHost = proxyHostParameter.getStringValue();
    }


    // Get the port for the proxy server.
    proxyPort = -1;
    proxyPortParameter =
         parameters.getIntegerParameter(proxyPortParameter.getName());
    if ((proxyPortParameter != null) && proxyPortParameter.hasValue())
    {
      proxyPort = proxyPortParameter.getIntValue();
    }


    // Get the user ID to use to authenticate to the proxy server.
    proxyUserID = null;
    proxyUserIDParameter =
         parameters.getStringParameter(proxyUserIDParameter.getName());
    if ((proxyUserIDParameter != null) && proxyUserIDParameter.hasValue())
    {
      proxyUserID = proxyUserIDParameter.getStringValue();
    }


    // Get the password to use to authenticate to the proxy server.
    proxyUserPW = null;
    proxyUserPWParameter =
         parameters.getPasswordParameter(proxyUserPWParameter.getName());
    if ((proxyUserPWParameter != null) && proxyUserPWParameter.hasValue())
    {
      proxyUserPW = proxyUserPWParameter.getStringValue();
    }


    // Get the warm up time.
    warmUpTime = 0;
    warmUpTimeParameter =
         parameters.getIntegerParameter(warmUpTimeParameter.getName());
    if ((warmUpTimeParameter != null) && warmUpTimeParameter.hasValue())
    {
      warmUpTime = warmUpTimeParameter.getIntValue();
    }


    // Get the cool down time.
    coolDownTime = 0;
    coolDownTimeParameter =
         parameters.getIntegerParameter(coolDownTimeParameter.getName());
    if ((coolDownTimeParameter != null) && coolDownTimeParameter.hasValue())
    {
      coolDownTime = coolDownTimeParameter.getIntValue();
    }


    // Get the time between requests.
    timeBetweenRequests = 0;
    delayParameter = parameters.getIntegerParameter(delayParameter.getName());
    if ((delayParameter != null) && delayParameter.hasValue())
    {
      timeBetweenRequests = delayParameter.getIntValue();
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


    // Get the number of iterations.
    numIterations = -1;
    iterationsParameter =
         parameters.getIntegerParameter(iterationsParameter.getName());
    if ((iterationsParameter != null) && iterationsParameter.hasValue())
    {
      numIterations = iterationsParameter.getIntValue();
    }


    // Get the set of client addresses.
    clientAddresses = new String[0];
    clientAddressesParameter =
         parameters.getMultiLineTextParameter(
              clientAddressesParameter.getName());
    if ((clientAddressesParameter != null) &&
        clientAddressesParameter.hasValue())
    {
      clientAddresses = clientAddressesParameter.getNonBlankLines();
    }


    // Get the arguments related to SSL usage.
    blindTrustParameter =
         parameters.getBooleanParameter(blindTrustParameter.getName());
    if (blindTrustParameter != null)
    {
      blindTrust = blindTrustParameter.getBooleanValue();
    }

    sslKeyStore = null;
    keyStoreParameter =
         parameters.getStringParameter(keyStoreParameter.getName());
    if ((keyStoreParameter != null) && keyStoreParameter.hasValue())
    {
      sslKeyStore = keyStoreParameter.getStringValue();
      System.setProperty(SSL_KEY_STORE_PROPERTY, sslKeyStore);
    }

    sslKeyStorePassword = null;
    keyPWParameter =
         parameters.getPasswordParameter(keyPWParameter.getName());
    if ((keyPWParameter != null) && keyPWParameter.hasValue())
    {
      sslKeyStorePassword = keyPWParameter.getStringValue();
      System.setProperty(SSL_KEY_PASSWORD_PROPERTY, sslKeyStorePassword);
    }

    sslTrustStore = null;
    trustStoreParameter =
         parameters.getStringParameter(trustStoreParameter.getName());
    if ((trustStoreParameter != null) && trustStoreParameter.hasValue())
    {
      sslTrustStore = trustStoreParameter.getStringValue();
      System.setProperty(SSL_TRUST_STORE_PROPERTY, sslTrustStore);
    }

    sslTrustStorePassword = null;
    trustPWParameter =
         parameters.getPasswordParameter(trustPWParameter.getName());
    if ((trustPWParameter != null) && trustPWParameter.hasValue())
    {
      sslTrustStorePassword = trustPWParameter.getStringValue();
      System.setProperty(SSL_TRUST_PASSWORD_PROPERTY, sslTrustStorePassword);
    }

    parentRandom = new Random();
  }


  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // Initialize the random number generator for this client.
    random = new Random(parentRandom.nextLong());


    // Initialize the exceptionsCaught tracker.
    exceptionsCaught = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_EXCEPTIONS_CAUGHT,
                                              collectionInterval);


    // Create and initialize the HTTP client that we will use for handling the
    // requests.
    httpClient = new HTTPClient();
    httpClient.setFollowRedirects(followRedirects);
    httpClient.setRetrieveAssociatedFiles(retrieveAssociatedFiles);
    httpClient.setUseKeepAlive(useKeepAlive);

    if ((clientAddresses != null) && (clientAddresses.length > 0))
    {
      int slot = getThreadNumber() % clientAddresses.length;

      try
      {
        httpClient.setClientAddress(clientAddresses[slot]);
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to set the client address for " +
                                       "thread " + threadID + " to " +
                                       clientAddresses[slot] + ":  " + e, e);
      }
    }

    if ((proxyHost != null) && (proxyPort > 0))
    {
      httpClient.enableProxy(proxyHost, proxyPort, proxyUserID, proxyUserPW);
    }

    if (blindTrust)
    {
      try
      {
        httpClient.setSSLSocketFactory(new JSSEBlindTrustSocketFactory());
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to use blind trust socket " +
                                       "factory:  " + e);
      }
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

    // First, see if this should operate "infinitely" (i.e., not a fixed number
    // of iterations
    boolean infinite = (numIterations <= 0);

    // Create a variable that will be used to handle the delay between requests.
    long requestStartTime = 0;


    // Create a loop that will run until it needs to stop
    for (int i=0; ((! shouldStop()) && ((infinite || (i < numIterations))));
         i++)
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
        // Tell the stat trackers that they should start tracking now
        httpClient.enableStatisticsCollection(getClientID(), getThreadID(),
                                              getCollectionInterval(),
                                              getJobID(), getStatReporter());
        exceptionsCaught.startTracker();
        collectingStats = true;
      }
      else if ((collectingStats) && (currentTime >= stopCollectingTime))
      {
        httpClient.stopTrackers();
        exceptionsCaught.stopTracker();
        collectingStats = false;
      }

      if (timeBetweenRequests > 0)
      {
        requestStartTime = System.currentTimeMillis();
      }

      try
      {
        HTTPResponse response = httpClient.sendRequest(getRandomRequest());
      }
      catch (Exception e)
      {
        if (collectingStats)
        {
          exceptionsCaught.increment();
        }
      }

      if (timeBetweenRequests > 0)
      {
        long requestStopTime = System.currentTimeMillis();
        long eTime           = requestStopTime - requestStartTime;
        long sleepTime       = timeBetweenRequests - eTime;
        if (sleepTime > 0)
        {
          try
          {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {}
        }
      }
    }

    if (collectingStats)
    {
      httpClient.stopTrackers();
      exceptionsCaught.stopTracker();
      collectingStats = false;
    }

    httpClient.closeAll();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    if (httpClient != null)
    {
      httpClient.closeAll();
    }
  }



  /**
   * Retrieves the next request that should be sent.
   *
   * @return  The next request that should be sent.
   */
  public HTTPRequest getRandomRequest()
  {
    if (requests.length == 1)
    {
      return requests[0];
    }
    else
    {
      int position = (random.nextInt() & 0x7FFFFFFF) % requests.length;
      return requests[position];
    }
  }
}

