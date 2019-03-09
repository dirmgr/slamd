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
import java.util.StringTokenizer;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;
import netscape.ldap.LDAPSocketFactory;
import netscape.ldap.factory.JSSESocketFactory;

import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;

import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class defines a SLAMD job that simulates the load that a Solaris 9
 * client places on the directory when authenticating telnet users through
 * pam_ldap.
 *
 *
 * @author  Neil A. Wilson
 */
public class SolarisLDAPAuthRateJobClass
       extends JobClass
{
  /**
   * The flag indicating that the "anonymous" credential level should be used.
   */
  public static final int CREDENTIAL_LEVEL_ANONYMOUS = 0;



  /**
   * The flag indicating that the "proxy" credential level should be used.
   */
  public static final int CREDENTIAL_LEVEL_PROXY = 1;



  /**
   * The human-readable strings corresponding to the credential level constants.
   */
  public static final String[] CREDENTIAL_LEVEL_STRINGS =
  {
    "Anonymous",
    "Proxy"
  };



  /**
   * The flag indicating that the authentication method should be "simple".
   */
  public static final int AUTH_METHOD_SIMPLE = 0;



  /**
   * The flag indicating that the authentication method should be "DIGEST-MD5".
   */
  public static final int AUTH_METHOD_DIGEST_MD5 = 1;



  /**
   * The flag indicating that the authentication method should be "simple"
   * over an SSL connection.
   */
  public static final int AUTH_METHOD_TLS_SIMPLE = 2;



  /**
   * The flag indicating that the authentication method should be "DIGEST-MD5"
   * over an SSL connection.
   */
  public static final int AUTH_METHOD_TLS_DIGEST_MD5 = 3;



  /**
   * The human-readable strings corresponding to the authentication method
   * constants.
   */
  public static final String[] AUTH_METHOD_STRINGS =
  {
    "Simple Authentication",
    "DIGEST-MD5 Authentication",
    "Simple Authentication over SSL",
    "DIGEST-MD5 Authentication over SSL"
  };



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
   * The name of the stat tracker that will be used to count the number of
   * authentication attempts.
   */
  public static final String STAT_TRACKER_AUTHENTICATION_ATTEMPTS =
       "Authentication Attempts";




  /**
   * The name of the stat tracker that will be used to keep track of the time
   * required to perform each authentication.
   */
  public static final String STAT_TRACKER_AUTHENTICATION_TIME =
       "Authentication Time";




  /**
   * The name of the stat tracker that will be used to count the number of
   * failed authentications.
   */
  public static final String STAT_TRACKER_FAILED_AUTHENTICATIONS =
       "Failed Authentications";



  /**
   * The name of the stat tracker that will be used to count the number of
   * successful authentications.
   */
  public static final String STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS =
       "Successful Authentications";



  // The parameter that indicates whether the client should trust any SSL cert.
  private BooleanParameter blindTrustParameter =
    new BooleanParameter("blind_trust", "Blindly Trust Any Certificate",
                         "Indicates whether the client should blindly trust " +
                         "any certificate presented by the server, or " +
                         "whether the key and trust stores should be used.",
                         true);

  // The parameter that specifies the maximum authentication rate.
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Authentication Rate (Auths/Second/Client)",
       "Specifies the maximum authentication rate (in auths per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform authentications as quickly as possible.",
       true, -1);

  // The parameter that specifies the directory server port number.
  private IntegerParameter portParameter =
       new IntegerParameter("ldap_port", "Directory Server Port",
                            "The port number of the directory server to " +
                            "use for authenticating clients.", true, 389, true,
                            1, true, 65535);

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

  // The parameter that specifies the authentication method to use when binding
  // to the server.
  private MultiChoiceParameter authMethodParameter =
       new MultiChoiceParameter("auth_method", "Authentication Method",
                                "The method that should be used when " +
                                "authenticating to the directory server as " +
                                "the end user.  If the proxy credential " +
                                "level is chosen, then this method will " +
                                "also be used to authenticate the proxy user.",
                                AUTH_METHOD_STRINGS, null);

  // The parameter that specifies the credential level to use when finding
  // the user's account.
  private MultiChoiceParameter credentialParameter =
       new MultiChoiceParameter("cred_level", "Credential Level",
                                "The credential level that indicates how " +
                                "the client should bind to the directory " +
                                "server in order to find the user's account " +
                                "to perform the authentication.  It may be " +
                                "either anonymous (no authentication) or " +
                                "proxy (authenticate as some proxy user to " +
                                "use when performing the queries)",
                                CREDENTIAL_LEVEL_STRINGS, null);

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter keyPWParameter =
       new PasswordParameter("sslkeypw", "SSL Key Store Password",
                             "The password for the JSSE key store.  This is " +
                             "not needed unless an SSL-based connection is " +
                             "to be used that requires access to a private " +
                             "key store", false, "");

  // The parameter that specifies the password for the SSL key store
  private PasswordParameter trustPWParameter =
       new PasswordParameter("ssltrustpw", "SSL Trust Store Password",
                             "The password for the JSSE trust store.  This " +
                             "is not needed unless an SSL-based connection " +
                             "is to be used that requires access to a trust " +
                             "store.", false, "");

  // The parameter that specifies the password for the proxy user.
  private PasswordParameter proxyPasswordParameter =
       new PasswordParameter("proxy_password", "Proxy User Password",
                             "The password that should be used for the " +
                             "proxy DN.  This must be provided if the " +
                             "proxy credential level is selected, but it is " +
                             "not needed otherwise.", false, "");

  // The parameter that specifies the password to use when authenticating.
  private PasswordParameter userPasswordParameter =
       new PasswordParameter("user_password", "User Password",
                             "The password that should be used when " +
                             "authenticating as the end user.  If the user " +
                             "ID is actually a range of users, then this " +
                             "password should be the same for all users.",
                             true, "");

  // A placeholder used only for cosmetic purposes.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter that specifies the base DN for information in the directory.
  private StringParameter baseParameter =
       new StringParameter("base_dn", "Directory Base DN",
                           "The DN that should be used as the search base " +
                           "all Solaris authentication information in the " +
                           "directory server", true, "");

  // The parameter that specifies the directory server address.
  private StringParameter hostParameter =
       new StringParameter("ldap_host", "Directory Server Address",
                           "The IP address or fully-qualified domain name of " +
                           "the directory server to use for authenticating " +
                           "clients.", true, "");

  // The parameter that specifies the IP addresses of hosts from which to
  // simulate telnet connections.
  private StringParameter ipAddressParameter =
       new StringParameter("ip_addr", "Simulated Client Address Range",
                           "The CIDR-style IP address range of the client " +
                           "systems that will be used when simulating a " +
                           "telnet connection to the Solaris system.  The " +
                           "address of the client is resolved using a call " +
                           "to gethostbyaddr, which generates a query of the " +
                           "directory server.", true, "192.168.1.0/24");

  // The parameter that specifies the location of the SSL key store
  private StringParameter keyStoreParameter =
       new StringParameter("sslkeystore", "SSL Key Store Location",
                           "The location of the JSSE key store.  This is not " +
                           "needed unless an SSL-based connection is to be " +
                           "used that requires access to a private key store",
                           false, "");

  // The parameter that specifies the location of the SSL key store
  private StringParameter trustStoreParameter =
       new StringParameter("ssltruststore", "SSL Trust Store Location",
                           "The location of the JSSE trust store.  This is " +
                           "not needed unless an SSL-based connection is to " +
                           "be used that requires access to a trust store.",
                           false, "");

  // The parameter that specifies the DN for the proxy user.
  private StringParameter proxyDNParameter =
       new StringParameter("proxy_dn", "Proxy User DN",
                           "The DN of the proxy user that should be used " +
                           "when binding to the directory server in order to " +
                           "locate the end user's account.  This must be " +
                           "provided if the proxy credential level is " +
                           "selected, but it is not required if the " +
                           "anonymous credential level is to be used.", false,
                           "");

  // The parameter that specifies the attribute to use for the user ID.
  private StringParameter userIDAttributeParameter =
       new StringParameter("user_id_attr", "User ID Attribute",
                           "The name of the LDAP attribute that contains the " +
                           "user ID that will be used for authentication.",
                           true, "uid");

  // The parameter that specifies the user ID(s) to use when authenticating.
  private StringParameter userIDParameter =
       new StringParameter("user_id", "User ID",
                           "The user ID to use when authenticating to the " +
                           "directory server.  It may include a numeric " +
                           "range of values in brackets separated by a " +
                           "dash (e.g., [1-1000]) to choose a value at " +
                           "random, or by a colon (e.g., [1:1000]) to " +
                           "iterate through them sequentially.", true, "");



  // Indicates whether to blindly trust any SSL certificate.
  private static boolean blindTrust;

  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;

  // The method that should be used to authenticate to the directory server
  // when the user is performing a bind.
  private static int authMethod;

  // The method that should be used for binding to the directory server to
  // perform searches to retrieve user information.
  private static int credentialLevel;

  // The base value for the IP address range of client systems, encoded as a
  // 32-bit value with the mask bits all set to zero.
  private static int ipAddressBase;

  // The mask value for the IP address range of client systems, encoded as a
  // 32-bit value.  The mask bits will all be set to one, and the remaining
  // bits will all be zero.
  private static int ipAddressMask;

  // The port number for the directory server.
  private static int ldapPort;

  // The scope to use when searching the directory server.  This will not be
  // configurable by the end user because we don't want to deal with custom
  // service search descriptors.
  private static int searchScope = LDAPConnection.SCOPE_SUB;

  // The search base to use for the directory server.
  private static String baseDN;

  // The address of the directory server.
  private static String ldapHost;

  // The DN to use when binding to the directory server as the proxy user.
  private static String proxyBindDN;

  // The password to use when binding to the directory server as the proxy user.
  private static String proxyBindPW;

  // The attribute that will hold the user ID value.
  private static String userIDAttribute;

  // The password that should be used when binding to the directory server as
  // the end user.
  private static String userPassword;

  // The value pattern to use to construct the user IDs.
  private static ValuePattern userIDPattern;



  // The connection to the directory server that is currently in use.
  private LDAPConnection conn;

  // The socket factory that should be used for creating the connections
  // to the directory used for bind and search operations.
  private LDAPSocketFactory bindSocketFactory;
  private LDAPSocketFactory proxySocketFactory;

  // The stat trackers used for this job.
  private IncrementalTracker authenticationAttempts;
  private IncrementalTracker failedAuthentications;
  private IncrementalTracker successfulAuthentications;
  private TimeTracker        authenticationTimer;


  // The random number generators used for this job.
  private static Random parentRandom;
  private Random random;



  /**
   * Creates a new instance of this job thread.  This constructor does not need
   * to do anything other than invoke the constructor for the superclass.
   */
  public SolarisLDAPAuthRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Solaris Authentication Load Generator";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Simulate the load generated by Solaris 9 Native LDAP clients";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to simulate the load observed by Solaris 9 " +
      "Native LDAP clients when attempting to authenticate users through a " +
      "service like telnet."
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
      baseParameter,
      placeholder,
      credentialParameter,
      proxyDNParameter,
      proxyPasswordParameter,
      placeholder,
      authMethodParameter,
      userIDAttributeParameter,
      userIDParameter,
      userPasswordParameter,
      placeholder,
      ipAddressParameter,
      placeholder,
      maxRateParameter,
      rateLimitDurationParameter,
      blindTrustParameter,
      keyStoreParameter,
      keyPWParameter,
      trustStoreParameter,
      trustPWParameter
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
                             STAT_TRACKER_AUTHENTICATION_ATTEMPTS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_AUTHENTICATION_TIME,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_FAILED_AUTHENTICATIONS,
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
      authenticationAttempts,
      authenticationTimer,
      successfulAuthentications,
      failedAuthentications
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
    // The user ID parameter must be parseable as a value pattern.
    StringParameter p =
         parameters.getStringParameter(userIDParameter.getName());
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


    // Make sure that a bind DN and password were provided if the proxy
    // credential level was chosen.
    MultiChoiceParameter credLevelParam =
         parameters.getMultiChoiceParameter(credentialParameter.getName());
    if (credLevelParam == null)
    {
      throw new InvalidValueException("Unable to determine the credential " +
                                      "level that should be used.");
    }

    String credLevel = credLevelParam.getStringValue();
    if (credLevel.equals(CREDENTIAL_LEVEL_STRINGS[CREDENTIAL_LEVEL_PROXY]))
    {
      StringParameter proxyDNParam =
           parameters.getStringParameter(proxyDNParameter.getName());
      PasswordParameter proxyPWParam =
           parameters.getPasswordParameter(proxyPasswordParameter.getName());
      if ((proxyDNParam == null) || (proxyDNParam.getStringValue() == null) ||
          (proxyDNParam.getStringValue().length() == 0) ||
          (proxyPWParam == null) || (proxyPWParam.getStringValue() == null) ||
          (proxyPWParam.getStringValue().length() == 0))
      {
        throw new InvalidValueException("Both a proxy bind DN and password " +
                                        "must be specified if the proxy " +
                                        "credential level is chosen.");
      }
    }


    // Make sure that the IP address range provided was either a single IP
    // address or a valid CIDR address.
    StringParameter ipRangeParam =
         parameters.getStringParameter(ipAddressParameter.getName());
    if (ipRangeParam == null)
    {
      throw new InvalidValueException("Unable to determine the IP address " +
                                      "range that should be used for " +
                                      "simulating Solaris clients.");
    }

    String ipAddrStr = ipRangeParam.getStringValue();
    int    slashPos  = ipAddrStr.indexOf('/');
    if (slashPos > 0)
    {
      try
      {
        int maskBits = Integer.parseInt(ipAddrStr.substring(slashPos+1));
        if ((maskBits < 0) || (maskBits > 32))
        {
          throw new InvalidValueException("Invalid IP address range -- " +
                                          "number of mask bits in a CIDR " +
                                          "address must be between 0 and 32");
        }

        ipAddrStr = ipAddrStr.substring(0, slashPos);
      }
      catch (InvalidValueException ive)
      {
        throw ive;
      }
      catch (Exception e)
      {
        throw new InvalidValueException("Invalid IP address range -- unable " +
                                        "to interpret number of mask bits " +
                                        "-- " + e);
      }
    }

    try
    {
      StringTokenizer tokenizer = new StringTokenizer(ipAddrStr, ".");

      int octet = Integer.parseInt(tokenizer.nextToken());
      if ((octet < 0) || (octet > 255))
      {
        throw new InvalidValueException("Invalid IP address range -- octet " +
                                        "1 value is not between 0 and 255");
      }

      octet = Integer.parseInt(tokenizer.nextToken());
      if ((octet < 0) || (octet > 255))
      {
        throw new InvalidValueException("Invalid IP address range -- octet " +
                                        "2 value is not between 0 and 255");
      }

      octet = Integer.parseInt(tokenizer.nextToken());
      if ((octet < 0) || (octet > 255))
      {
        throw new InvalidValueException("Invalid IP address range -- octet " +
                                        "3 value is not between 0 and 255");
      }

      octet = Integer.parseInt(tokenizer.nextToken());
      if ((octet < 0) || (octet > 255))
      {
        throw new InvalidValueException("Invalid IP address range -- octet " +
                                        "4 value is not between 0 and 255");
      }

      if (tokenizer.hasMoreTokens())
      {
        throw new InvalidValueException("Invalid IP address range -- too " +
                                        "many octets.");
      }
    }
    catch (InvalidValueException ive)
    {
      throw ive;
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Invalid IP address range -- unable to " +
                                      "interpret \"" + ipAddrStr +
                                      "\" as an IP address");
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
    // Get the parameters that should be tested.
    boolean bind      = false;
    boolean useSSL    = false;
    int     port      = -1;
    String  baseDN    = null;
    String  bindDN    = null;
    String  bindPW    = null;
    String  credLevel = CREDENTIAL_LEVEL_STRINGS[0];
    String  host      = null;

    StringParameter hostParam =
         parameters.getStringParameter(hostParameter.getName());
    if ((hostParam == null) || (! hostParam.hasValue()))
    {
      outputMessages.add("ERROR:  No directory server address was provided.");
      return false;
    }
    else
    {
      host = hostParam.getStringValue();
    }

    IntegerParameter portParam =
         parameters.getIntegerParameter(portParameter.getName());
    if ((portParam == null) || (! hostParam.hasValue()))
    {
      outputMessages.add("ERROR:  No directory server port was provided.");
      return false;
    }
    else
    {
      port = portParam.getIntValue();
    }

    StringParameter baseParam =
         parameters.getStringParameter(baseParameter.getName());
    if ((baseParam == null) || (! baseParam.hasValue()))
    {
      outputMessages.add("ERROR:  No directory base DN was provided.");
      return false;
    }
    else
    {
      baseDN = baseParam.getStringValue();
    }

    MultiChoiceParameter credLevelParam =
         parameters.getMultiChoiceParameter(credentialParameter.getName());
    if ((credLevelParam == null) || (! credLevelParam.hasValue()))
    {
      outputMessages.add("ERROR:  No credential level was provided.");
      return false;
    }
    else
    {
      credLevel = credLevelParam.getStringValue();
    }

    if (credLevel.equals(CREDENTIAL_LEVEL_STRINGS[CREDENTIAL_LEVEL_PROXY]))
    {
      bind = true;

      StringParameter bindDNParam =
           parameters.getStringParameter(proxyDNParameter.getName());
      if ((bindDNParam == null) || (! bindDNParam.hasValue()))
      {
        outputMessages.add("ERROR:  No proxy user DN was provided for the " +
                           "proxy credential level.");
        return false;
      }
      else
      {
        bindDN = bindDNParam.getStringValue();
      }

      PasswordParameter bindPWParam =
           parameters.getPasswordParameter(proxyPasswordParameter.getName());
      if ((bindPWParam == null) || (! bindPWParam.hasValue()))
      {
        outputMessages.add("ERROR:  No proxy user password was provided for " +
                           "the proxy credential level.");
        return false;
      }
      else
      {
        bindPW = bindPWParam.getStringValue();
      }
    }

    MultiChoiceParameter authMethodParam =
         parameters.getMultiChoiceParameter(authMethodParameter.getName());
    if ((authMethodParam == null) || (! authMethodParam.hasValue()))
    {
      outputMessages.add("ERROR:  No authentication method was provided.");
      return false;
    }
    else
    {
      String methodStr = authMethodParam.getStringValue();
      useSSL = (methodStr.equals(AUTH_METHOD_STRINGS[AUTH_METHOD_TLS_SIMPLE]) ||
           methodStr.equals(AUTH_METHOD_STRINGS[AUTH_METHOD_TLS_DIGEST_MD5]));
    }


    // Verify that we can establish a connection to the directory server.
    LDAPConnection conn;
    String message = "Attempting to connect to directory server " + host + ':' +
                     port;
    if (useSSL)
    {
      outputMessages.add(message + " overSSL");

      try
      {
        conn = new LDAPConnection(new JSSEBlindTrustSocketFactory());
      }
      catch (Exception e)
      {

        outputMessages.add("ERROR:  Unable to complete the SSL " +
                           "initialization:  " + e);
        return false;
      }
    }
    else
    {
      outputMessages.add(message);
      conn = new LDAPConnection(new SLAMDLDAPSocketFactory());
    }

    if (bind)
    {
      try
      {
        conn.connect(3, host, port, bindDN, bindPW);
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to connect to " + host + ':' + port +
                           " as " + bindDN + " -- " + e);
        return false;
      }
    }
    else
    {
      try
      {
        conn.connect(host, port);
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to connect to " + host + ':' + port +
                           " -- " + e);
        return false;
      }
    }


    // Make sure that the base DN exists.
    outputMessages.add("Connection established successfully.");
    outputMessages.add("");
    outputMessages.add("Attempting to retrieve the base entry " + baseDN);
    try
    {
      LDAPEntry entry = conn.read(baseDN);
      if (entry == null)
      {
        conn.disconnect();
        outputMessages.add("ERROR:  Unable to retrieve the base entry.");
        return false;
      }

      outputMessages.add("Successfully read the base entry.");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to retrieve the base entry:  " + e);

      try
      {
      } catch (Exception e2) {}

      return false;
    }


    // Close the connection to the server.
    outputMessages.add("");
    outputMessages.add("All tests completed successfully.");

    try
    {
      conn.disconnect();
    } catch (Exception e) {}

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
      ldapHost = hostParameter.getStringValue();
    }


    // Get the directory server port
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      ldapPort = portParameter.getIntValue();
    }


    // Get the directory base DN
    baseParameter = parameters.getStringParameter(baseParameter.getName());
    if (baseParameter != null)
    {
      baseDN = baseParameter.getStringValue();
    }


    // Get the credential level.
    credentialParameter =
         parameters.getMultiChoiceParameter(credentialParameter.getName());
    if (credentialParameter != null)
    {
      String credentialParamStr = credentialParameter.getStringValue();
      for (int i=0; i < CREDENTIAL_LEVEL_STRINGS.length; i++)
      {
        if (credentialParamStr.equals(CREDENTIAL_LEVEL_STRINGS[i]))
        {
          credentialLevel = i;
        }
      }
    }


    // Get the proxy DN.
    proxyDNParameter =
         parameters.getStringParameter(proxyDNParameter.getName());
    if (proxyDNParameter != null)
    {
      proxyBindDN = proxyDNParameter.getStringValue();
    }
    if (proxyBindDN == null)
    {
      proxyBindDN = "";
    }


    // Get the proxy password.
    proxyPasswordParameter =
         parameters.getPasswordParameter(proxyPasswordParameter.getName());
    if (proxyPasswordParameter != null)
    {
      proxyBindPW = proxyPasswordParameter.getStringValue();
    }
    if (proxyBindPW == null)
    {
      proxyBindPW = "";
    }


    // Get the authentication method.
    authMethodParameter =
         parameters.getMultiChoiceParameter(authMethodParameter.getName());
    if (authMethodParameter != null)
    {
      String authMethodStr = authMethodParameter.getStringValue();
      for (int i=0; i < AUTH_METHOD_STRINGS.length; i++)
      {
        if (authMethodStr.equals(AUTH_METHOD_STRINGS[i]))
        {
          authMethod = i;
        }
      }
    }


    // Get the user ID attribute.
    userIDAttributeParameter =
         parameters.getStringParameter(userIDAttributeParameter.getName());
    if (userIDAttributeParameter != null)
    {
      userIDAttribute = userIDAttributeParameter.getStringValue();
    }


    // Get the user ID.  It could be a range of values.
    userIDParameter = parameters.getStringParameter(userIDParameter.getName());
    if (userIDParameter != null)
    {
      try
      {
        userIDPattern = new ValuePattern(userIDParameter.getStringValue());
      }
      catch (Exception e)
      {
        throw new UnableToRunException(
             "Unable to parse the user ID pattern:  " + stackTraceToString(e),
             e);
      }
    }


    // Get the password for the user.
    userPasswordParameter =
         parameters.getPasswordParameter(userPasswordParameter.getName());
    if (userPasswordParameter != null)
    {
      userPassword = userPasswordParameter.getStringValue();
    }


    // Get the IP address for the simulated clients.
    ipAddressParameter =
         parameters.getStringParameter(ipAddressParameter.getName());
    if (ipAddressParameter != null)
    {
      String ipAddressStr = ipAddressParameter.getStringValue();
      int    maskBits     = 32;
      int slashPos = ipAddressStr.indexOf('/');
      if (slashPos >= 0)
      {
        maskBits     = Integer.parseInt(ipAddressStr.substring(slashPos+1));
        ipAddressStr = ipAddressStr.substring(0, slashPos);
      }

      StringTokenizer tokenizer = new StringTokenizer(ipAddressStr, ".");
      int a = Integer.parseInt(tokenizer.nextToken());
      int b = Integer.parseInt(tokenizer.nextToken());
      int c = Integer.parseInt(tokenizer.nextToken());
      int d = Integer.parseInt(tokenizer.nextToken());

      int ipAddrInt = ((0x000000FF & a) << 24) |
                      ((0x000000FF & b) << 16) |
                      ((0x000000FF & c) << 8) |
                       (0x000000FF & d);

      int baseMask  = 0x00000000;
      ipAddressMask = 0x00000000;
      for (int i=0; i < 32; i++)
      {
        baseMask <<= 1;
        ipAddressMask <<= 1;

        if (i < maskBits)
        {
          baseMask |= 0x0000001;
        }
        else
        {
          ipAddressMask |= 0x00000001;
        }
      }

      ipAddressBase = ipAddrInt & baseMask;
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

    blindTrustParameter =
         parameters.getBooleanParameter(blindTrustParameter.getName());
    if (blindTrustParameter != null)
    {
      blindTrust = blindTrustParameter.getBooleanValue();
    }

    keyStoreParameter =
         parameters.getStringParameter(keyStoreParameter.getName());
    if (keyStoreParameter != null)
    {
      String keyStoreLocation = keyStoreParameter.getStringValue();
      System.setProperty(SSL_KEY_STORE_PROPERTY, keyStoreLocation);
    }

    keyPWParameter = parameters.getPasswordParameter(keyPWParameter.getName());
    if (keyPWParameter != null)
    {
      String keyPW = keyPWParameter.getStringValue();
      System.setProperty(SSL_KEY_PASSWORD_PROPERTY, keyPW);
    }

    trustStoreParameter =
         parameters.getStringParameter(trustStoreParameter.getName());
    if (trustStoreParameter != null)
    {
      String trustStoreLocation = trustStoreParameter.getStringValue();
      System.setProperty(SSL_TRUST_STORE_PROPERTY, trustStoreLocation);
    }

    trustPWParameter =
         parameters.getPasswordParameter(trustPWParameter.getName());
    if (trustPWParameter != null)
    {
      String trustPW = trustPWParameter.getStringValue();
      System.setProperty(SSL_TRUST_PASSWORD_PROPERTY, trustPW);
    }


    // If the connection to the directory server should be over SSL, then create
    // an SSL-based connection now.  The reason for this is that the first time
    // an SSL-based connection is created, it can be a relatively expensive
    // process, taking up to a few seconds, and we want to get that out of the
    // way before the timer starts.
    if ((authMethod == AUTH_METHOD_TLS_SIMPLE) ||
        (authMethod == AUTH_METHOD_TLS_DIGEST_MD5))
    {
      try
      {
        LDAPConnection conn;
        if (blindTrust)
        {
          conn = new LDAPConnection(new JSSEBlindTrustSocketFactory());
        }
        else
        {
          conn = new LDAPConnection(new JSSESocketFactory(null));
        }

        conn.setConnectTimeout(10);
        conn.connect(ldapHost, ldapPort);
        conn.disconnect();
      } catch (Exception e) {}
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
    // Create the stat trackers.
    authenticationAttempts =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_AUTHENTICATION_ATTEMPTS,
                                collectionInterval);
    authenticationTimer = new TimeTracker(clientID, threadID,
                                          STAT_TRACKER_AUTHENTICATION_TIME,
                                          collectionInterval);
    successfulAuthentications =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS,
                                collectionInterval);
    failedAuthentications =
         new IncrementalTracker(clientID, threadID,
                                STAT_TRACKER_FAILED_AUTHENTICATIONS,
                                collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      authenticationAttempts.enableRealTimeStats(statReporter, jobID);
      authenticationTimer.enableRealTimeStats(statReporter, jobID);
      successfulAuthentications.enableRealTimeStats(statReporter, jobID);
      failedAuthentications.enableRealTimeStats(statReporter, jobID);
    }


    // Seed the random number generator for this thread.
    random = new Random(parentRandom.nextLong());


    // If necessary, create the socket factories to use for the proxy and bind
    // users.
    switch (authMethod)
    {
      case AUTH_METHOD_SIMPLE:
        bindSocketFactory  = null;
        proxySocketFactory = null;
        break;
      case AUTH_METHOD_DIGEST_MD5:
        try
        {
          bindSocketFactory  = new LDAPDigestMD5SocketFactory();
          proxySocketFactory = new LDAPDigestMD5SocketFactory();
        }
        catch (SLAMDException se)
        {
          throw new UnableToRunException("Unable to create the DIGEST-MD5 " +
                                         "socket factory -- " + se, se);
        }

        if (credentialLevel == CREDENTIAL_LEVEL_PROXY)
        {
          ((LDAPDigestMD5SocketFactory)
           proxySocketFactory).setAuthenticationInfo("dn:" + proxyBindDN,
                                                     proxyBindPW);
        }
        break;
      case AUTH_METHOD_TLS_SIMPLE:
        if (blindTrust)
        {
          try
          {
            bindSocketFactory  = new JSSEBlindTrustSocketFactory();
            proxySocketFactory = new JSSEBlindTrustSocketFactory();
          }
          catch (LDAPException le)
          {
            throw new UnableToRunException(le.getMessage(), le);
          }
        }
        else
        {
          bindSocketFactory  = new JSSESocketFactory(null);
          proxySocketFactory = new JSSESocketFactory(null);
        }
        break;
      case AUTH_METHOD_TLS_DIGEST_MD5:
        try
        {
          LDAPDigestMD5SocketFactory bindFactory =
               new LDAPDigestMD5SocketFactory();
          LDAPDigestMD5SocketFactory proxyFactory =
               new LDAPDigestMD5SocketFactory();

          if (blindTrust)
          {
            try
            {
              bindFactory.setAdditionalSocketFactory(
                               new JSSEBlindTrustSocketFactory());
              proxyFactory.setAdditionalSocketFactory(
                                new JSSEBlindTrustSocketFactory());
            }
            catch (LDAPException le)
            {
              throw new UnableToRunException(le.getMessage(), le);
            }
          }
          else
          {
            bindFactory.setAdditionalSocketFactory(new JSSESocketFactory(null));
            proxyFactory.setAdditionalSocketFactory(
                              new JSSESocketFactory(null));
          }

          if (credentialLevel == CREDENTIAL_LEVEL_PROXY)
          {
            proxyFactory.setAuthenticationInfo("dn:" + proxyBindDN,
                                               proxyBindPW);
          }

          bindSocketFactory  = bindFactory;
          proxySocketFactory = proxyFactory;
        }
        catch (SLAMDException se)
        {
          throw new UnableToRunException("Unable to create the DIGEST-MD5 " +
                                         "socket factory -- " + se, se);
        }
        break;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    authenticationAttempts.startTracker();
    successfulAuthentications.startTracker();
    failedAuthentications.startTracker();
    authenticationTimer.startTracker();


    while (! shouldStop())
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      try
      {
        // We will collect these later.
        int    uidNumber;
        String userDN;

        // Get the information to use for this authentication attempt.
        String ipAddress = getIPAddress();
        String userID    = userIDPattern.nextValue();

        // Start the timer and increment the attempt counter.
        authenticationAttempts.increment();
        authenticationTimer.startTimer();

        // First, issue a query to get the hostname of the system from which the
        // connection is being established.  We don't care about any return
        // value from this.
        getHostByAddress(ipAddress);

        // Next, get the shadow account for the user.  If it is successful, it
        // will return the DN of the user's entry.
        userDN = getShadowAccount(userID);
        if (userDN == null)
        {
          failedAuthentications.increment();
          authenticationTimer.stopTimer();
        }

        // Next, get the POSIX account for the user.  If it is successful, it
        // will return the uid number for the user.
        uidNumber = getPosixAccount(userID);
        if (uidNumber < 0)
        {
          failedAuthentications.increment();
          authenticationTimer.stopTimer();
        }

        // Issue a sequence of queries that are quite unnecessary but
        // unfortunately are performed nonetheless.
        getShadowAccount(userID);
        getPosixAccount(userID);
        getShadowAccount(userID);

        // Now get the user's entry and request all attributes (i.e., don't
        // specify an attribute list).
        getUserEntry(userID);

        // Bind as the user.
        bindAsUser(userDN, userPassword);

        // Get the extended attributes for the user.
        getUserAttr(userID);

        // Get project information for the user.
        getProjectByName("user." + userID);
        getProjectByName("group.other");

        // Get the shadow account again.
        getShadowAccount(userID);

        // Get group information for the user.
        getGroupsForUser(userID);

        // Still more retrievals of the POSIX and shadow information.
        getPosixAccount(userID);
        getShadowAccount(userID);

        // Get the NIS key information for the user.
        getNISKey(uidNumber);

        // Get the host information for the system we are authenticating.
        // We'll use the directory server's address for that.
        getHostByName(ldapHost);

        // If we've gotten here, then the authentication was successful.
        successfulAuthentications.increment();
        authenticationTimer.stopTimer();
      }
      catch (LDAPException le)
      {
        failedAuthentications.increment();
        authenticationTimer.stopTimer();

        try
        {
          conn.disconnect();
        } catch (LDAPException e) {}
      }
    }


    authenticationAttempts.stopTracker();
    successfulAuthentications.stopTracker();
    failedAuthentications.stopTracker();
    authenticationTimer.stopTracker();
  }



  /**
   * Retrieves a randomly-chosen IP address to use as the client's source
   * address.
   *
   * @return  A randomly-chosen IP address to use as the client's source
   *          address.
   */
  private String getIPAddress()
  {
    int ipAddressInt = ipAddressBase | (random.nextInt() & ipAddressMask);

    StringBuilder addrBuffer = new StringBuilder();

    addrBuffer.append((ipAddressInt >>> 24) & 0x000000FF);
    addrBuffer.append('.');
    addrBuffer.append((ipAddressInt >>> 16) & 0x000000FF);
    addrBuffer.append('.');
    addrBuffer.append((ipAddressInt >>> 8) & 0x000000FF);
    addrBuffer.append('.');
    addrBuffer.append(ipAddressInt & 0x000000FF);

    return addrBuffer.toString();
  }



  /**
   * Simulates the query issued to the directory server whenever the Solaris
   * gethostbyaddr function is called.
   *
   * @param  ipAddress  The dotted-quad (e.g., 1.2.3.4) IP address for which to
   *                    retrieve the value.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getHostByAddress(String ipAddress)
          throws LDAPException
  {
    String   filter = "(&(objectClass=ipHost)(ipHostNumber=" + ipAddress + "))";
    String[] attrs  = { "cn", "ipHostNumber" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server whenever the Solaris
   * gethostbyname function is called.
   *
   * @param  hostname  The hostname for which to retrieve the address.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getHostByName(String hostname)
          throws LDAPException
  {
    String   filter = "(&(objectClass=ipHost)(cn=" + hostname + "))";
    String[] attrs  = { "cn", "ipHostNumber" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server whenever the Solaris
   * getpwnam function is called.
   *
   * @param  userID  The user ID for which to retrieve the entry.
   *
   * @return  The uid number for the user whose account was returned, or -1
   *          if no user could be found.
   *
   * @throws  LDAPException  If a problem occurs while processing the query, or
   *                         if multiple entries matched the given query.
   */
  private int getPosixAccount(String userID)
          throws LDAPException
  {
    String   filter = "(&(objectClass=posixAccount)(" + userIDAttribute + '=' +
                      userID + "))";
    String[] attrs  = { "cn", userIDAttribute, "uidNumber", "gidNumber",
                        "gecos", "description", "homeDirectory", "loginShell" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    int uidNumber = -1;
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        if (uidNumber >= 0)
        {
          conn.disconnect();
          throw new LDAPException("Multiple entries returned for getpwnam",
                                  LDAPException.OTHER);
        }

        LDAPEntry entry = (LDAPEntry) element;
        LDAPAttribute attr = entry.getAttribute("uidNumber");
        if (attr == null)
        {
          conn.disconnect();
          throw new LDAPException("Unable to obtain uid number for entry " +
                                  entry.getDN());
        }
        String[] values = attr.getStringValueArray();
        if ((values == null) || (values.length == 0))
        {
          conn.disconnect();
          throw new LDAPException("Unable to obtain uid number for entry " +
                                  entry.getDN());
        }

        try
        {
          uidNumber = Integer.parseInt(values[0]);
        }
        catch (NumberFormatException nfe)
        {
          conn.disconnect();
          throw new LDAPException("Unable to parse uid number an an integer " +
                                  "for entry " + entry.getDN());
        }
      }
    }

    conn.disconnect();
    return uidNumber;
  }



  /**
   * Simulates the query issued to the directory server whenever the Solaris
   * getspnam function is called.
   *
   * @param  userID  The user ID for which to retrieve the entry.
   *
   * @return  The DN of the entry matching the provided criteria, or
   *          <CODE>null</CODE> if no entry was found.
   *
   * @throws  LDAPException  If a problem occurs while processing the query, or
   *                         if multiple entries matched the given query.
   */
  private String getShadowAccount(String userID)
          throws LDAPException
  {
    String   filter = "(&(objectClass=shadowAccount)(" + userIDAttribute + '=' +
                      userID + "))";
    String[] attrs  = { userIDAttribute, "userPassword", "shadowFlag" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    String userDN = null;
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        if (userDN != null)
        {
          conn.disconnect();
          throw new LDAPException("Multiple entries returned for getspnam",
                                  LDAPException.OTHER);
        }

        LDAPEntry entry = (LDAPEntry) element;
        userDN = entry.getDN();
      }
    }

    conn.disconnect();
    return userDN;
  }



  /**
   * Retrieves the entry for the user with all attributes.  I'm not sure which
   * system call this corresponds to, but it does happen during the login
   * process.
   *
   * @param  userID  The user ID for which to retrieve the entry.
   *
   * @return  <CODE>true</CODE> if a match was found, or <CODE>false</CODE> if
   *          there were no matches.
   *
   * @throws  LDAPException  If a problem occurs while processing the query, or
   *                         if multiple entries matched the given query.
   */
  private boolean getUserEntry(String userID)
          throws LDAPException
  {
    String filter = "(&(objectClass=posixAccount)(" + userIDAttribute + '=' +
                    userID + "))";

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, null,
                                            false);

    boolean matchFound = false;
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        if (matchFound)
        {
          conn.disconnect();
          throw new LDAPException("Multiple entries returned when getting " +
                                  "user entry", LDAPException.OTHER);
        }

        matchFound = true;
      }
    }

    conn.disconnect();
    return matchFound;
  }



  /**
   * Performs a bind as the user with the specified DN, using the configured
   * authentication method.
   *
   * @param  userDN        The DN for the user.
   * @param  userPassword  The password for the user.
   *
   * @throws  LDAPException  If a problem occurs while performing the bind.
   */
  private void bindAsUser(String userDN, String userPassword)
          throws LDAPException
  {
    if (bindSocketFactory == null)
    {
      conn = new LDAPConnection(new SLAMDLDAPSocketFactory());
    }
    else
    {
      if (bindSocketFactory instanceof LDAPDigestMD5SocketFactory)
      {
        LDAPDigestMD5SocketFactory md5SocketFactory =
             (LDAPDigestMD5SocketFactory) bindSocketFactory;
        md5SocketFactory.setAuthenticationInfo("dn:" + userDN, userPassword);
      }

      conn = new LDAPConnection(bindSocketFactory);
    }
    conn.setConnectTimeout(10);

    switch (authMethod)
    {
      case AUTH_METHOD_SIMPLE:
      case AUTH_METHOD_TLS_SIMPLE:
        conn.connect(3, ldapHost, ldapPort, userDN, userPassword);
        break;
      case AUTH_METHOD_DIGEST_MD5:
      case AUTH_METHOD_TLS_DIGEST_MD5:
        conn.connect(ldapHost, ldapPort);
        break;
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server when Solaris is looking
   * for the automount records for a specified key.
   *
   * @param  keyName  The name of the key for which to retrieve the automount
   *                  records.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getAutomountEntry(String keyName)
          throws LDAPException
  {
    String filter = "(&(objectClass=automount)(automountKey=" + keyName + "))";

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, null,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server when Solaris is trying
   * to retrieve extended attributes for a user entry.
   *
   * @param  userID  The user ID for which to retrieve the information.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getUserAttr(String userID)
          throws LDAPException
  {
    String   filter = "(&(objectClass=SolarisUserAttr)(" + userIDAttribute +
                      '=' + userID + "))";
    String[] attrs  = { userIDAttribute, "SolarisUserQualifier",
                        "SolarisAttrReserved1", "SolarisAttrReserved2",
                        "SolarisAttrKeyValue" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server when Solaris is trying
   * to retrieve the project with the given name.
   *
   * @param  projectName  The name of the project to retrieve.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getProjectByName(String projectName)
          throws LDAPException
  {
    String   filter = "(&(objectClass=SolarisProject)(SolarisProjectName=" +
                      projectName + "))";
    String[] attrs  = { "SolarisProjectName", "SolarisProjectID", "description",
                        "memberGid", "SolarisProjectAttr" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server when Solaris is trying
   * to determine the groups for the specified user.
   *
   * @param  userID  The user ID for which to retrieve the groups.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getGroupsForUser(String userID)
          throws LDAPException
  {
    String   filter = "(&(objectClass=posixGroup)(memberUID=" + userID + "))";
    String[] attrs  = { "cn", "gidNumber", "userPassword", "memberUID" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Simulates the query issued to the directory server when Solaris is trying
   * to get the NIS key information for the specified user.
   *
   * @param  uidNumber  The UID number for the user.
   *
   * @throws  LDAPException  If a problem occurs while processing the query.
   */
  private void getNISKey(int uidNumber)
          throws LDAPException
  {
    String   filter = "(&(objectClass=nisKeyObject)(uidNumber=" + uidNumber +
                      "))";
    String[] attrs  = { "nisPublicKey", "nisSecretKey" };

    getLDAPConnection();
    LDAPSearchResults results = conn.search(baseDN, searchScope, filter, attrs,
                                            false);

    while (results.hasMoreElements())
    {
      results.nextElement();
    }

    conn.disconnect();
  }



  /**
   * Establishes a connection to the directory server that is bound as an
   * appropriate user using the correct mechanism.
   *
   * @return  A connection to the directory server that is bound as an
   *          appropriate user using the correct mechanism.
   *
   * @throws  LDAPException  If a problem occurs while establishing the
   *                         connection.
   */
  private LDAPConnection getLDAPConnection()
          throws LDAPException
  {
    if (proxySocketFactory == null)
    {
      conn = new LDAPConnection(new SLAMDLDAPSocketFactory());
    }
    else
    {
      conn = new LDAPConnection(proxySocketFactory);
    }
    conn.setConnectTimeout(10);


    switch (credentialLevel)
    {
      case CREDENTIAL_LEVEL_ANONYMOUS:
        conn.connect(ldapHost, ldapPort);
        break;
      case CREDENTIAL_LEVEL_PROXY:
        if ((authMethod == AUTH_METHOD_SIMPLE) ||
            (authMethod == AUTH_METHOD_TLS_SIMPLE))
        {
          conn.connect(3, ldapHost, ldapPort, proxyBindDN, proxyBindPW);
        }
        else
        {
          conn.connect(ldapHost, ldapPort);
        }

        break;
    }

    return conn;
  }
}

