/*
 * Copyright 2008-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2008-2010.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;



/**
 * This class provides an API that should be extended by jobs which communicate
 * with one or more LDAP directory server instances.  It supports load-balancing
 * requests against multiple Directory Server addresses/ports, SSL or StartTLS
 * communication, and authentication, and it provides the ability to establish
 * connections using the specified properties.
 */
public abstract class LDAPJobClass
       extends JobClass
{
  /**
   * The security method string that indicates that no security should be used.
   */
  private static final String SECURITY_METHOD_NONE = "None";



  /**
   * The security method string that indicates that communication should be
   * protected with SSL.
   */
  private static final String SECURITY_METHOD_SSL = "SSL";



  /**
   * The security method string that indicates that communication should be
   * protected with StartTLS.
   */
  private static final String SECURITY_METHOD_STARTTLS = "StartTLS";



  /**
   * The array of possible security methods.
   */
  private static final String[] SECURITY_METHODS =
  {
    SECURITY_METHOD_NONE,
    SECURITY_METHOD_SSL,
    SECURITY_METHOD_STARTTLS
  };



  // Variables used to connect to the target server.
  private static boolean    useStartTLS;
  private static SSLContext sslContext;
  private static String     bindDN;
  private static String     bindPW;

  // The server set that will be used to create the connections.
  private static RoundRobinServerSet serverSet;

  // The parameters used to provide information about the connections.
  private MultiLineTextParameter addressesParameter =
       new MultiLineTextParameter("addresses", "Server Addresses",
                "The addresses (or addresses followed by colons and and port " +
                     "numbers) of the directory servers to which connections " +
                     "should be established.  If multiple addresses are " +
                     "given, then they should be provided on separate lines, " +
                     "and clients will load-balance between those servers on " +
                     "a round-robin manner.",
                null, true);
  private MultiChoiceParameter securityMethodParameter =
       new MultiChoiceParameter("securityMethod", "Security Method",
                "The type of security (if any) that should be used to " +
                     "protect communication with the directory server.",
                SECURITY_METHODS, SECURITY_METHOD_NONE);
  private StringParameter bindDNParameter =
       new StringParameter("bindDN", "Bind DN",
                "The DN to use to authenticate to the directory server.  If " +
                     "no value is provided then no authentication will be " +
                     "performed.",
                false, "");
  private PasswordParameter bindPWParameter =
       new PasswordParameter("bindPW", "Bind Password",
                "The password to use to authenticate to the directory " +
                "server.  If no value is provided then no authentication " +
                "will be performed.",
                false, "");



  /**
   * Creates a new instance of this job class.
   */
  protected LDAPJobClass()
  {
    super();
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
  public final ParameterList getParameterStubs()
  {
    ArrayList<Parameter> params = new ArrayList<Parameter>();

    params.add(new PlaceholderParameter());
    params.add(addressesParameter);
    params.add(securityMethodParameter);
    params.add(bindDNParameter);
    params.add(bindPWParameter);

    List<Parameter> nonLDAPStubs = getNonLDAPParameterStubs();
    if (nonLDAPStubs != null)
    {
      params.addAll(nonLDAPStubs);
    }

    Parameter[] paramArray = new Parameter[params.size()];
    return new ParameterList(params.toArray(paramArray));
  }



  /**
   * Retrieves the list of parameters needed by this job that are not needed to
   * connect or authenticate to the target server.
   *
   * @return  The list of parameters needed by this job that are not needed to
   *          connect or authenticate to the target server.
   */
  protected List<Parameter> getNonLDAPParameterStubs()
  {
    // No additional parameters are required by default.
    return Collections.emptyList();
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
    String[] addrs;
    MultiLineTextParameter addressesParam =
         parameters.getMultiLineTextParameter(addressesParameter.getName());
    if ((addressesParam == null) || (! addressesParam.hasValue()) ||
        ((addrs = addressesParam.getNonBlankLines()).length == 0))
    {
      throw new InvalidValueException("No addresses were provided.");
    }

    for (String a : addrs)
    {
      int colonPos = a.indexOf(':');
      if (colonPos == 0)
      {
        throw new InvalidValueException("Address '" + a +
                                        "' does not have a hostname");
      }
      else if (colonPos > 0)
      {
        try
        {
          int portNumber = Integer.parseInt(a.substring(colonPos+1));
          if ((portNumber < 1) || (portNumber > 65535))
          {
            throw new InvalidValueException("Address '" + a +
                 "' has an invalid port number (it must be between 1 and " +
                 "65535)");
          }
        }
        catch (Exception e)
        {
          throw new InvalidValueException("The value after the colon in " +
               "address '" + a + "' cannot be parsed as an integer port number",
               e);
        }
      }
    }


    StringParameter bindDNParam =
         parameters.getStringParameter(bindDNParameter.getName());
    boolean bindDNProvided = ((bindDNParam != null) && bindDNParam.hasValue());

    PasswordParameter bindPWParam =
         parameters.getPasswordParameter(bindPWParameter.getName());
    boolean bindPWProvided = ((bindPWParam != null) && bindPWParam.hasValue());

    if (bindDNProvided && (! bindPWProvided))
    {
      throw new InvalidValueException("If a bind DN is provided, then a " +
                                      "bind password must also be provided.");
    }

    if (bindPWProvided && (! bindDNProvided))
    {
      throw new InvalidValueException("If a bind password is provided, then " +
                                      "a bind DN must also be provided.");
    }


    validateNonLDAPJobInfo(numClients, threadsPerClient, threadStartupDelay,
         startTime, stopTime, duration, collectionInterval, parameters);
  }



  /**
   * Performs any validation that may be necessary for the provided job
   * information.
   *
   * @param  numClients          The number of clients on which the job should
   *                             run.
   * @param  threadsPerClient    The number of threads per client.
   * @param  threadStartupDelay  The length of time in milliseconds between
   *                             each thread to be started.
   * @param  startTime           The time that the job should start running, or
   *                             {@code null} if none was specified.
   * @param  stopTime            The time that the job should stop running, or
   *                             {@code null} if none was specified.
   * @param  duration            The maximum length of time in seconds that the
   *                             job should run.
   * @param  collectionInterval  The statistics collection interval in seconds.
   * @param  parameters          The set of parameters for the job.
   *
   * @throws  InvalidValueException  If any of the provided information is
   *                                 invalid for the job.
   */
  protected void validateNonLDAPJobInfo(final int numClients,
                                        final int threadsPerClient,
                                        final int threadStartupDelay,
                                        final Date startTime,
                                        final Date stopTime,
                                        final int duration,
                                        final int collectionInterval,
                                        final ParameterList parameters)
            throws InvalidValueException
  {
    // No validation is required by default.
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public final boolean providesParameterTest()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public final boolean testJobParameters(final ParameterList parameters,
                                         final ArrayList<String> outputMessages)
  {
    MultiLineTextParameter addressesParam =
         parameters.getMultiLineTextParameter(addressesParameter.getName());

    ArrayList<String>  addrList = new ArrayList<String>();
    ArrayList<Integer> portList = new ArrayList<Integer>();
    for (String a : addressesParam.getNonBlankLines())
    {
      int colonPos = a.indexOf(':');
      if (colonPos > 0)
      {
        addrList.add(a.substring(0, colonPos));
        portList.add(Integer.parseInt(a.substring(colonPos+1)));
      }
      else
      {
        addrList.add(a);
        portList.add(389);
      }
    }

    boolean ssl      = false;
    boolean startTLS = false;
    MultiChoiceParameter secMethodParam =
         parameters.getMultiChoiceParameter(securityMethodParameter.getName());
    if (secMethodParam.getValueString().equals(SECURITY_METHOD_SSL))
    {
      ssl = true;
    }
    else if (secMethodParam.getValueString().equals(SECURITY_METHOD_STARTTLS))
    {
      startTLS = true;
    }

    String dn = null;
    String pw = null;
    StringParameter bindDNParam =
         parameters.getStringParameter(bindDNParameter.getName());
    PasswordParameter bindPWParam =
         parameters.getPasswordParameter(bindPWParameter.getName());
    if ((bindDNParam != null) && bindDNParam.hasValue())
    {
      dn = bindDNParam.getStringValue();
      pw = bindPWParam.getStringValue();
    }


    SocketFactory socketFactory;
    SSLContext    sslCtx;
    if (ssl)
    {
      sslCtx = null;

      SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

      try
      {
        socketFactory = sslUtil.createSSLSocketFactory();
      }
      catch (Exception e)
      {
        outputMessages.add("Unable to create an SSL socket factory:  " +
                           stackTraceToString(e));
        return false;
      }
    }
    else if (startTLS)
    {
      socketFactory = SocketFactory.getDefault();

      SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

      try
      {
        sslCtx = sslUtil.createSSLContext();
      }
      catch (Exception e)
      {
        outputMessages.add("Unable to create an SSL context for StartTLS " +
                           "communication:  " + stackTraceToString(e));
        return false;
      }
    }
    else
    {
      socketFactory = SocketFactory.getDefault();
      sslCtx        = null;
    }


    boolean successful = true;
    for (int i=0; i < addrList.size(); i++)
    {
      String addr = addrList.get(i);
      int    port = portList.get(i);

      LDAPConnection c;
      if (ssl)
      {
        outputMessages.add("Attempting to create an SSL-based connection to " +
                           addr + ':' + port + "....");
      }
      else
      {
        outputMessages.add("Attempting to create an insecure connection to " +
                           addr + ':' + port + "....");
      }

      try
      {
        c = new LDAPConnection(socketFactory, addr, port);
        outputMessages.add("Connected successfully.");
        outputMessages.add("");
      }
      catch (LDAPException le)
      {
        successful = false;
        outputMessages.add("The connection attempt failed:  " +
                           le.getExceptionMessage());
        continue;
      }

      try
      {
        if (startTLS)
        {
          outputMessages.add("Attempting to perform StartTLS negotation....");
          ExtendedResult r = c.processExtendedOperation(
               new StartTLSExtendedRequest(sslCtx));
          if (r.getResultCode().equals(ResultCode.SUCCESS))
          {
            outputMessages.add("StartTLS negotiation successful.");
            outputMessages.add("");
          }
          else
          {
            successful = false;
            outputMessages.add("StartTLS negotiation failed:  " +
                               r.toString());
            outputMessages.add("");
            continue;
          }
        }

        if (dn != null)
        {
          outputMessages.add("Attempting to bind as " + dn + " ....");
          try
          {
            c.bind(dn, pw);
            outputMessages.add("Authentication successful.");
            outputMessages.add("");
          }
          catch (LDAPException le)
          {
            successful = false;
            outputMessages.add("Authentication failed:  " + le.toString());
            outputMessages.add("");
            continue;
          }
        }

        successful &= testNonLDAPJobParameters(parameters, c, outputMessages);
      }
      catch (LDAPException le)
      {
        outputMessages.add("An unexpected failure occurred:  " +
                           le.getExceptionMessage());

        successful = false;
      }
      catch (Exception e)
      {
        outputMessages.add("An unexpected failure occurred:  " +
                           stackTraceToString(e));

        successful = false;
      }
      finally
      {
        c.close();
      }
    }

    successful &= testNonLDAPJobParameters(parameters, outputMessages);

    if (successful)
    {
      outputMessages.add("All tests completed successfully.");
    }

    return successful;
  }



  /**
   * Performs any additional tests for job parameters that aren't related to the
   * target directory server(s).
   *
   * @param  parameters      The parameters provided for the job.
   * @param  outputMessages  A list into which output messages detailing the
   *                         test results should be added.
   *
   * @return  {@code true} if all tests were successful, or {@code false} if
   *          not.
   */
  protected boolean testNonLDAPJobParameters(final ParameterList parameters,
                         final ArrayList<String> outputMessages)
  {
    // No implementation is required by default.
    return true;
  }



  /**
   * Performs any additional tests for job parameters that aren't related to the
   * target directory server(s) using the provided connection.
   *
   * @param  parameters      The parameters provided for the job.
   * @param  connection      The connection to use to perform the test.
   * @param  outputMessages  A list into which output messages detailing the
   *                         test results should be added.
   *
   * @return  {@code true} if all tests were successful, or {@code false} if
   *          not.
   */
  protected boolean testNonLDAPJobParameters(final ParameterList parameters,
                         final LDAPConnection connection,
                         final ArrayList<String> outputMessages)
  {
    // No implementation is required by default.
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public final void initializeClient(final String clientID,
                                     final ParameterList parameters)
         throws UnableToRunException
  {
    ArrayList<String>  addrList = new ArrayList<String>();
    ArrayList<Integer> portList = new ArrayList<Integer>();
    addressesParameter =
         parameters.getMultiLineTextParameter(addressesParameter.getName());

    // We want to alter the order of the address:port values between clients so
    // that the load is better distributed across multiple clients.
    ArrayList<String> addrValues = new ArrayList<String>(
         Arrays.asList(addressesParameter.getNonBlankLines()));
    int offset = (getClientNumber() % addrValues.size());
    for (int i=0; i < offset; i++)
    {
      String addrValue = addrValues.remove(0);
      addrValues.add(addrValue);
    }

    for (String a : addrValues)
    {
      int colonPos = a.indexOf(':');
      if (colonPos > 0)
      {
        addrList.add(a.substring(0, colonPos));
        portList.add(Integer.parseInt(a.substring(colonPos+1)));
      }
      else
      {
        addrList.add(a);
        portList.add(389);
      }
    }

    String[] hosts = new String[addrList.size()];
    int[]    ports = new int[hosts.length];
    for (int i=0; i < hosts.length; i++)
    {
      hosts[i] = addrList.get(i);
      ports[i] = portList.get(i);
    }


    SocketFactory socketFactory;
    useStartTLS = false;
    securityMethodParameter =
         parameters.getMultiChoiceParameter(securityMethodParameter.getName());
    if (securityMethodParameter.getValueString().equals(SECURITY_METHOD_SSL))
    {
      SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

      try
      {
        socketFactory = sslUtil.createSSLSocketFactory();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Cannot create SSL socket factory:  " +
                                       String.valueOf(e), e);
      }
    }
    else if (securityMethodParameter.getValueString().equals(
                  SECURITY_METHOD_STARTTLS))
    {
      useStartTLS = true;

      SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
      try
      {
        sslContext = sslUtil.createSSLContext();
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Cannot create SSL context for " +
             "StartTLS negotiation:  " + String.valueOf(e), e);
      }

      socketFactory = SocketFactory.getDefault();
    }
    else
    {
      socketFactory = SocketFactory.getDefault();
    }

    LDAPConnectionOptions options = new LDAPConnectionOptions();
    options.setAutoReconnect(true);
    options.setUseSynchronousMode(useSynchronousMode());

    serverSet = new RoundRobinServerSet(hosts, ports, socketFactory, options);


    bindDN = null;
    bindPW = null;
    bindDNParameter = parameters.getStringParameter(bindDNParameter.getName());
    bindPWParameter =
         parameters.getPasswordParameter(bindPWParameter.getName());
    if ((bindDNParameter != null) && bindDNParameter.hasValue())
    {
      bindDN = bindDNParameter.getStringValue();
      bindPW = bindPWParameter.getStringValue();
    }


    initializeClientNonLDAP(clientID, parameters);
  }



  /**
   * Performs any necessary initialization that should be performed on a
   * per-client basis that doesn't involve parsing the LDAP-related parameters.
   *
   * @param  clientID    The client ID that has been assigned to the client on
   *                     which this method has been invoked.
   * @param  parameters  The set of parameters that have been scheduled for this
   *                     job.
   *
   * @throws  UnableToRunException  If the client cannot run the job with the
   *                                provided parameters.
   */
  protected void initializeClientNonLDAP(final String clientID,
                                         final ParameterList parameters)
            throws UnableToRunException
  {
    // No action required by default.
  }



  /**
   * Indicates whether the client should operate in synchronous mode.  This
   * should not be used if the client intends to perform asynchronous
   * operations (either explicitly or by sharing a connection across multiple
   * threads).
   *
   * @return  {@code true} if the client should operate in synchronous mode, or
   *          {@code false} if not.
   */
  protected boolean useSynchronousMode()
  {
    return true;
  }



  /**
   * Retrieves the server set that can be used to create the connections.
   *
   * @return  The server set that can be used to create the connections.
   */
  protected static RoundRobinServerSet getServerSet()
  {
    return serverSet;
  }



  /**
   * Retrieves a bind request that can be used to authenticate connections, if
   * the connections should be authenticated.
   *
   * @return  A bind request that can be used to authenticate connections, or
   *          {@code null} if no authentication should be performed.
   */
  protected static BindRequest getBindRequest()
  {
    if ((bindDN == null) || (bindPW == null))
    {
      return null;
    }

    return new SimpleBindRequest(bindDN, bindPW);
  }



  /**
   * Creates a new LDAP connection to one of the configured servers using the
   * provided parameters.
   *
   * @return  The created LDAP connection.
   *
   * @throws  LDAPException  If a problem occurs while trying to create the
   *                         connection.
   */
  protected static LDAPConnection createConnection()
            throws LDAPException
  {
    LDAPConnection c = serverSet.getConnection();

    prepareConnection(c);

    return c;
  }



  /**
   * Creates a new LDAP connection to the specified server using the provided
   * parameters.
   *
   * @param  address  The address of the server to which the connection should
   *                  be established.
   * @param  port     The port number of the server to which the connection
   *                  should be established.
   *
   * @return  The created LDAP connection.
   *
   * @throws  LDAPException  If a problem occurs while trying to create the
   *                         connection.
   */
  protected static LDAPConnection createConnection(final String address,
                                                   final int port)
            throws LDAPException
  {
    LDAPConnection c =
         new LDAPConnection(serverSet.getSocketFactory(), address, port);

    prepareConnection(c);

    return c;
  }



  /**
   * Creates a new LDAP connection pool based on the provided parameters.
   *
   * @param  initialConnections  The initial number of connections to include in
   *                             the pool.
   * @param  maxConnections      The maximum number of connections to include in
   *                             the pool.
   *
   * Creates a new LDAP connection to one of the configured servers using the
   * provided parameters.
   *
   * @return  The created LDAP connection.
   *
   * @throws  LDAPException  If a problem occurs while trying to create the
   *                         connection.
   */
  protected static LDAPConnectionPool createConnectionPool(
                                           final int initialConnections,
                                           final int maxConnections)
            throws LDAPException
  {
    SimpleBindRequest bindRequest = null;
    if ((bindDN != null) && (bindPW != null))
    {
      bindRequest = new SimpleBindRequest(bindDN, bindPW);
    }

    StartTLSPostConnectProcessor postConnectProcessor = null;
    if (useStartTLS)
    {
      postConnectProcessor = new StartTLSPostConnectProcessor(sslContext);
    }

    return new LDAPConnectionPool(serverSet, bindRequest, initialConnections,
                                  maxConnections, postConnectProcessor);
  }



  /**
   * Performs StartTLS negotiation and/or authentication on the provided
   * connection, if appropriate.
   *
   * @param  connection  The connection to process.
   *
   * @throws  LDAPException  If a problem occurs during processing.
   */
  private static void prepareConnection(final LDAPConnection connection)
          throws LDAPException
  {
    if (useStartTLS)
    {
      try
      {
        ExtendedResult r = connection.processExtendedOperation(
             new StartTLSExtendedRequest(sslContext));
        if (! r.getResultCode().equals(ResultCode.SUCCESS))
        {
          throw new LDAPException(r);
        }
      }
      catch (LDAPException le)
      {
        connection.close();
        throw le;
      }
    }

    if (bindDN != null)
    {
      try
      {
        connection.bind(bindDN, bindPW);
      }
      catch (LDAPException le)
      {
        connection.close();
        throw le;
      }
    }
  }
}
