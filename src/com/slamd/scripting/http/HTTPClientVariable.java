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
package com.slamd.scripting.http;



import java.net.InetAddress;
import java.net.URL;
import javax.net.ssl.SSLSocketFactory;

import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.http.HTTPClient;
import com.slamd.http.HTTPRequest;
import com.slamd.http.HTTPResponse;
import com.slamd.job.JobClass;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringVariable;
import com.slamd.stat.StatTracker;



/**
 * This class defines a variable that can be used to manage the interaction with
 * one or more servers using HTTP 1.1.  An HTTP client variable provides the
 * following methods:
 *
 * <UL>
 *   <LI>blindTrust() -- Indicates whether this client is configured to blindly
 *       trust any SSL certificate.</LI>
 *   <LI>clearCookies() -- Removes all cookies currently associated with this
 *       client.  This method does not return a value.</LI>
 *   <LI>closeAll() -- Closes all open connections associated with this client.
 *       This method does not return a value.</LI>
 *   <LI>cookiesEnabled() -- Indicates whether this client will provide support
 *       for cookies.</LI>
 *   <LI>deleteLogoutCookies() -- Indicates whether this client will
 *       automatically delete any cookie with a value of "LOGOUT".</LI>
 *   <LI>disableAuthentication() -- Indicates that no authentication information
 *       should be provided to the server as part of the request.  This method
 *       does not return a value.</LI>
 *   <LI>disableDebugMode() -- Indicates that the client should stop operating
 *       in debug mode.</LI>
 *   <LI>disableProxy() -- Indicates that requests should be sent directly to
 *       the target server rather than going through a proxy.  This method does
 *       not return a value.</LI>
 *   <LI>disableStatistics() -- Indicates that the client should disable
 *       statistics collection.</LI>
 *   <LI>enableAuthentication(string authID, string password) -- Indicates that
 *       HTTP basic authentication should be used to provide the given user ID
 *       and password to use to authenticate to the target server.  This method
 *       does not return a value.</LI>
 *   <LI>enableDebugMode() -- Indicates that the client should operate in debug
 *       mode, in which case debugging information will be printed to standard
 *       error.</LI>
 *   <LI>enableProxy(string host, int port) -- Indicates that all requests
 *       should be sent through the proxy server with the given host and port.
 *       No authentication will be used for the proxy server.  This method does
 *       not return a value.</LI>
 *   <LI>enableProxy(string host, int port, string authID, string password) --
 *       Indicates that all requests should be sent through the proxy server
 *       with the given host and port.  The client will authenticate itself to
 *       the proxy with the provided user ID and password.  This method does not
 *       return a value.</LI>
 *   <LI>enableStatistics() -- Indicates that the client should enable
 *       statistics collection.</LI>
 *   <LI>followRedirects() -- Returns a boolean value that indicates whether
 *       this client will automatically attempt to follow any HTTP redirects
 *       that it encounters when processing a request.</LI>
 *   <LI>getFailureReason() -- Returns a string value with information about the
 *       reason that the last operation failed.</LI>
 *   <LI>getSocketTimeout() -- Returns an integer value with the socket timeout
 *       for the client.</LI>
 *   <LI>gzipEnabled() -- Returns a boolean value that indicates whether the
 *       client will support documents compressed using the GZIP method.</LI>
 *   <LI>removeCommonHeader(string name) -- Removes the common header with the
 *       specified name.  This method does not return a value.</LI>
 *   <LI>removeCookie(string name) -- Removes the cookie with the specified
 *       name.  This method does not return a value.</LI>
 *   <LI>removeCookie(string name, string value) -- Removes the cookie with the
 *       specified name and value.  This method does not return a value.</LI>
 *   <LI>retrieveAssociatedFiles() -- Returns a boolean value that indicates
 *       whether this client will automatically attempt to retrieve any files
 *       associated with an HTML document that has been retrieved.</LI>
 *   <LI>sendRequest(HTTPRequest request) -- Sends the provided request to the
 *       and returns the response as an HTTPResponse variable.</LI>
 *   <LI>sendRequest(string url) -- Sends an HTTP GET request based on the
 *       information in the provided URL and returns the response as an
 *       HTTPResponse variable.</LI>.
 *   <LI>setBlindTrust(boolean blindTrust) -- Specifies whether the client
 *       should blindly trust any SSL certificate.</LI>
 *   <LI>setClientAddress(string address) -- Specifies that the client should
 *       use the provided local address for all connections.  An empty or null
 *       value will cause the client to automatically choose the appropriate
 *       local address.  This method will return a Boolean value indicating
 *       whether the address was set properly.</LI>
 *   <LI>setCommonHeader(string name, string value) -- Sets a common header that
 *       will be included in all requests sent using this client.  This method
 *       does not return a value.</LI>
 *   <LI>setCookiesEnabled(boolean cookiesEnabled) -- Specifies whether the
 *       client should automatically manage cookies included in responses from
 *       remote servers.</LI>
 *   <LI>setDeleteLogoutCookies(boolean deleteLogoutCookies) -- Specifies
 *       whether the client should automatically delete any cookie with a value
 *       of "LOGOUT".</LI>
 *   <LI>setFollowRedirects(boolean followRedirects) -- Specifies whether the
 *       client should automatically follow HTTP redirects that it encounters
 *       while processing a request.  This method does not return a value.</LI>
 *   <LI>setGZIPEnabled(boolean gzipEnabled) -- Specifies whether the client
 *       should support documents compressed with the GZIP method.  This method
 *       does not return a value.</LI>
 *   <LI>setRetrieveAssociatedFiles(boolean retrieveAssociatedFiles) --
 *       Specifies whether the client should automatically retrieve any files
 *       associated with a request being processed.  This method does not return
 *       a value.</LI>
 *   <LI>setSocketTimeou(int timeout) -- Specifies the socket timeout for the
 *       client.  This method does not return a value.</LI>
 *   <LI>setUseKeepAlive(boolean useKeepAlive) -- Specifies whether the client
 *       should use the HTTP 1.1 KeepAlive feature to allow multiple requests
 *       to be sent over the same connection.  This method does not return a
 *       value.</LI>
 *   <LI>useKeepAlive() -- Returns a boolean value that indicates whether the
 *       client will attempt to use HTTP 1.1 KeepAlive requests in order to
 *       re-use established connections.
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class HTTPClientVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of HTTP client variables.
   */
  public static final String HTTP_CLIENT_VARIABLE_TYPE = "httpclient";



  /**
   * The name of the method that can be used to determine whether the client
   * has been configured to blindly trust any SSL certificate.
   */
  public static final String BLIND_TRUST_METHOD_NAME = "blindtrust";



  /**
   * The method number for the "blindTrust" method.
   */
  public static final int BLIND_TRUST_METHOD_NUMBER = 0;



  /**
   * The name of the method that can be used to clear all cookie information
   * associated with this HTTP client.
   */
  public static final String CLEAR_COOKIES_METHOD_NAME = "clearcookies";



  /**
   * The method number for the "clearCookies" method.
   */
  public static final int CLEAR_COOKIES_METHOD_NUMBER = 1;



  /**
   * The name of the method that can be used to close all connections associated
   * with this HTTP client.
   */
  public static final String CLOSE_ALL_METHOD_NAME = "closeall";



  /**
   * The method number for the "closeAll" method.
   */
  public static final int CLOSE_ALL_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to determine if cookie support is
   * enabled.
   */
  public static final String COOKIES_ENABLED_METHOD_NAME = "cookiesenabled";



  /**
   * The method number for the "cookiesEnabled" method.
   */
  public static final int COOKIES_ENABLED_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to determine whether logout cookies
   * should be automatically deleted.
   */
  public static final String DELETE_LOGOUT_COOKIES_METHOD_NAME =
       "deletelogoutcookies";



  /**
   * The method number for the "deleteLogoutCookies" method.
   */
  public static final int DELETE_LOGOUT_COOKIES_METHOD_NUMBER = 4;



  /**
   * The name of the method that indicates that no authentication information
   * should be included in requests sent using this client.
   */
  public static final String DISABLE_AUTHENTICATION_METHOD_NAME =
       "disableauthentication";



  /**
   * The method number for the "disableAuthentication" method.
   */
  public static final int DISABLE_AUTHENTICATION_METHOD_NUMBER = 5;



  /**
   * The name of the method that indicates that the client should stop operating
   * in debug mode.
   */
  public static final String DISABLE_DEBUG_MODE_METHOD_NAME =
       "disabledebugmode";



  /**
   * The method number for the "disableDebugMode" method.
   */
  public static final int DISABLE_DEBUG_MODE_METHOD_NUMBER = 6;



  /**
   * The name of the method that indicates that no proxy server should be used
   * when sending requests using this client.
   */
  public static final String DISABLE_PROXY_METHOD_NAME = "disableproxy";



  /**
   * The method number for the "disableProxy" method.
   */
  public static final int DISABLE_PROXY_METHOD_NUMBER = 7;



  /**
   * The name of the method that indicates that statistics collection should be
   * disabled.
   */
  public static final String DISABLE_STATISTICS_METHOD_NAME =
       "disablestatistics";



  /**
   * The method number for the "disableStatistics" method.
   */
  public static final int DISABLE_STATISTICS_METHOD_NUMBER = 8;



  /**
   * The name of the method that indicates that authentication should be used
   * when sending requests using this client.
   */
  public static final String ENABLE_AUTHENTICATION_METHOD_NAME =
       "enableauthentication";



  /**
   * The method number for the "enableAuthentication" method.
   */
  public static final int ENABLE_AUTHENTICATION_METHOD_NUMBER = 9;



  /**
   * The name of the method that indicates that the client should operate in
   * debug mode.
   */
  public static final String ENABLE_DEBUG_MODE_METHOD_NAME =
       "enabledebugmode";



  /**
   * The method number for the "enableDebugMode" method.
   */
  public static final int ENABLE_DEBUG_MODE_METHOD_NUMBER = 10;



  /**
   * The name of the method that indicates that requests sent using this client
   * should be forwarded through a proxy server.
   */
  public static final String ENABLE_PROXY_METHOD_NAME = "enableproxy";



  /**
   * The method number for the first "enableProxy" method.
   */
  public static final int ENABLE_PROXY_1_METHOD_NUMBER = 11;



  /**
   * The method number for the second "enableProxy" method.
   */
  public static final int ENABLE_PROXY_2_METHOD_NUMBER = 12;



  /**
   * The name of the method that indicates whether the client should maintain a
   * number of statistics about the request processing.
   */
  public static final String ENABLE_STATISTICS_METHOD_NAME = "enablestatistics";



  /**
   * The method number for the "enableStatistics" method.
   */
  public static final int ENABLE_STATISTICS_METHOD_NUMBER = 13;



  /**
   * The name of the method that indicates whether this client will
   * automatically follow redirects encountered while processing requests.
   */
  public static final String FOLLOW_REDIRECTS_METHOD_NAME = "followredirects";



  /**
   * The method number for the "followRedirects" method.
   */
  public static final int FOLLOW_REDIRECTS_METHOD_NUMBER = 14;



  /**
   * The name of the method that can be used to determine the reason for the
   * failure of the last operation processed using this client.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 15;



  /**
   * The name of the method that can be used to get the socket timeout for this
   * client.
   */
  public static final String GET_SOCKET_TIMEOUT_METHOD_NAME =
       "getsockettimeout";



  /**
   * The method number for the "getSocketTimeout" method.
   */
  public static final int GET_SOCKET_TIMEOUT_METHOD_NUMBER = 16;



  /**
   * The name of the method that can be used to indicate whether GZIP
   * compression is enabled.
   */
  public static final String GZIP_ENABLED_METHOD_NAME = "gzipenabled";



  /**
   * The method number for the "gzipEnabled" method.
   */
  public static final int GZIP_ENABLED_METHOD_NUMBER = 17;



  /**
   * The name of the method that can be used to remove a common header from this
   * client.
   */
  public static final String REMOVE_COMMON_HEADER_METHOD_NAME =
       "removecommonheader";



  /**
   * The method number for the "removeCommonHeader" method.
   */
  public static final int REMOVE_COMMON_HEADER_METHOD_NUMBER = 18;



  /**
   * The name of the method that can be used to remove a cookie from the client
   * cookie jar.
   */
  public static final String REMOVE_COOKIE_METHOD_NAME = "removecookie";



  /**
   * The method number for the first "removeCookie" method.
   */
  public static final int REMOVE_COOKIE_1_METHOD_NUMBER = 19;



  /**
   * The method number for the second "removeCookie" method.
   */
  public static final int REMOVE_COOKIE_2_METHOD_NUMBER = 20;



  /**
   * The name of the method that indicates whether this client will
   * automatically retrieve any files associated with an HTML document retrieved
   * as part of processing requests.
   */
  public static final String RETRIEVE_ASSOCIATED_FILES_METHOD_NAME =
       "retrieveassociatedfiles";



  /**
   * The method number for the "retrieveAssociatedFiles" method.
   */
  public static final int RETRIEVE_ASSOCIATED_FILES_METHOD_NUMBER = 21;



  /**
   * The name of the method that may be used to send a request using this HTTP
   * client.
   */
  public static final String SEND_REQUEST_METHOD_NAME = "sendrequest";



  /**
   * The method number for the first "sendRequest" method.
   */
  public static final int SEND_REQUEST_1_METHOD_NUMBER = 22;



  /**
   * The method number for the second "sendRequest" method.
   */
  public static final int SEND_REQUEST_2_METHOD_NUMBER = 23;



  /**
   * The name of the method that can be used to specify whether the client
   * should blindly trust any SSL certificate.
   */
  public static final String SET_BLIND_TRUST_METHOD_NAME = "setblindtrust";



  /**
   * The method number for the "setBlindTrust" method.
   */
  public static final int SET_BLIND_TRUST_METHOD_NUMBER = 24;



  /**
   * The name of the method that can be used to specify the local address that
   * the client should use for outbound connections.
   */
  public static final String SET_CLIENT_ADDRESS_METHOD_NAME =
       "setclientaddress";



  /**
   * The method number for the "setClientAddress" method.
   */
  public static final int SET_CLIENT_ADDRESS_METHOD_NUMBER = 25;



  /**
   * The name of the method that can be used to set a common header to include
   * in all requests sent using this client.
   */
  public static final String SET_COMMON_HEADER_METHOD_NAME = "setcommonheader";



  /**
   * The method number for the "setCommonHeader" method.
   */
  public static final int SET_COMMON_HEADER_METHOD_NUMBER = 26;



  /**
   * The name of the method that can be used to specify whether cookie support
   * should be enabled.
   */
  public static final String SET_COOKIES_ENABLED_METHOD_NAME =
       "setcookiesenabled";



  /**
   * The method number for the "setCookiesEnabled" method.
   */
  public static final int SET_COOKIES_ENABLED_METHOD_NUMBER = 27;



  /**
   * The name of the method that can be used to specify whether the client
   * should automatically delete logout cookies.
   */
  public static final String SET_DELETE_LOGOUT_COOKIES_METHOD_NAME =
       "setdeletelogoutcookies";



  /**
   * The method number for the "setDeleteLogoutCookies" method.
   */
  public static final int SET_DELETE_LOGOUT_COOKIES_METHOD_NUMBER = 28;



  /**
   * The name of the method that specifies whether this HTTP client should
   * automatically follow any redirects encountered while processing requests.
   */
  public static final String SET_FOLLOW_REDIRECTS_METHOD_NAME =
       "setfollowredirects";



  /**
   * The method number for the "setFollowRedirects" method.
   */
  public static final int SET_FOLLOW_REDIRECTS_METHOD_NUMBER = 29;



  /**
   * The name of the method that specifies whether this HTTP client should
   * support documents compressed with the GZIP compression method.
   */
  public static final String SET_GZIP_ENABLED_METHOD_NAME = "setgzipenabled";



  /**
   * The method number for the "setGZIPEnabled" method.
   */
  public static final int SET_GZIP_ENABLED_METHOD_NUMBER = 30;



  /**
   * The name of the method that specifies whether this HTTP client should
   * automatically retrieve any files associated with an HTML document retrieved
   * as part of processing requests.
   */
  public static final String SET_RETRIEVE_ASSOCIATED_FILES_METHOD_NAME =
       "setretrieveassociatedfiles";



  /**
   * The method number for the "setRetrieveAssociatedFiles" method.
   */
  public static final int SET_RETRIEVE_ASSOCIATED_FILES_METHOD_NUMBER = 31;



  /**
   * The name of the method that may be used to specify a timeout for network
   * communication.
   */
  public static final String SET_SOCKET_TIMEOUT_METHOD_NAME =
       "setsockettimeout";



  /**
   * The method number for the "setSocketTimeout" method.
   */
  public static final int SET_SOCKET_TIMEOUT_METHOD_NUMBER = 32;



  /**
   * The name of the method that specifies whether this HTTP client should use
   * keep-alive in order to try to reuse a connection for multiple requests.
   */
  public static final String SET_USE_KEEPALIVE_METHOD_NAME = "setusekeepalive";



  /**
   * The method number for the "setUseKeepAlive" method.
   */
  public static final int SET_USE_KEEPALIVE_METHOD_NUMBER = 33;



  /**
   * The name of the method that indicates whether this client will use
   * keep-alive in order to try to reuse a connection for multiple requests.
   */
  public static final String USE_KEEPALIVE_METHOD_NAME = "usekeepalive";



  /**
   * The method number for the "useKeepAlive" method.
   */
  public static final int USE_KEEPALIVE_METHOD_NUMBER = 34;



  /**
   * The set of methods associated with HTTP client variables.
   */
  public static final Method[] HTTP_CLIENT_VARIABLE_METHODS = new Method[]
  {
    new Method(BLIND_TRUST_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CLEAR_COOKIES_METHOD_NAME, new String[0], null),
    new Method(CLOSE_ALL_METHOD_NAME, new String[0], null),
    new Method(COOKIES_ENABLED_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DELETE_LOGOUT_COOKIES_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DISABLE_AUTHENTICATION_METHOD_NAME, new String[0], null),
    new Method(DISABLE_DEBUG_MODE_METHOD_NAME, new String[0], null),
    new Method(DISABLE_PROXY_METHOD_NAME, new String[0], null),
    new Method(DISABLE_STATISTICS_METHOD_NAME, new String[0], null),
    new Method(ENABLE_AUTHENTICATION_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ENABLE_DEBUG_MODE_METHOD_NAME, new String[0], null),
    new Method(ENABLE_PROXY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(ENABLE_PROXY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ENABLE_STATISTICS_METHOD_NAME, new String[0], null),
    new Method(FOLLOW_REDIRECTS_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_SOCKET_TIMEOUT_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GZIP_ENABLED_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(REMOVE_COMMON_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(REMOVE_COOKIE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(REMOVE_COOKIE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(RETRIEVE_ASSOCIATED_FILES_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SEND_REQUEST_METHOD_NAME,
               new String[] { HTTPRequestVariable.HTTP_REQUEST_VARIABLE_TYPE },
               HTTPResponseVariable.HTTP_RESPONSE_VARIABLE_TYPE),
    new Method(SEND_REQUEST_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               HTTPResponseVariable.HTTP_RESPONSE_VARIABLE_TYPE),
    new Method(SET_BLIND_TRUST_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(SET_CLIENT_ADDRESS_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(SET_COMMON_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(SET_COOKIES_ENABLED_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(SET_DELETE_LOGOUT_COOKIES_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(SET_FOLLOW_REDIRECTS_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(SET_GZIP_ENABLED_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(SET_RETRIEVE_ASSOCIATED_FILES_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(SET_SOCKET_TIMEOUT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(SET_USE_KEEPALIVE_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE }, null),
    new Method(USE_KEEPALIVE_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };



  // The actual HTTP client that we will use to perform all processing.
  private HTTPClient httpClient;


  // The variables used for statistics collection.
  private int           collectionInterval;
  private JobClass      jobThread;
  private String        clientID;
  private String        threadID;
  private StatTracker[] statTrackers;


  // The variable that holds the reason for the last failure.
  private String failureReason;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public HTTPClientVariable()
         throws ScriptException
  {
    httpClient    = new HTTPClient();
    failureReason = null;
    statTrackers  = new StatTracker[0];
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return HTTP_CLIENT_VARIABLE_TYPE;
  }



  /**
   * Starts the stat trackers associated with this variable.
   *
   * @param  jobThread  The job thread with which the stat trackers are
   *                    associated.
   */
  @Override()
  public void startStatTrackers(JobClass jobThread)
  {
    this.jobThread     = jobThread;
    clientID           = jobThread.getClientID();
    threadID           = jobThread.getThreadID();
    collectionInterval = jobThread.getCollectionInterval();
  }



  /**
   * Stops the stat trackers associated with this variable.
   */
  @Override()
  public void stopStatTrackers()
  {
    statTrackers = httpClient.getStatTrackers();
  }



  /**
   * Retrieves the stat trackers that have been maintained for this LDAP
   * connection variable.
   *
   * @return  The stat trackers that have been maintained for this LDAP
   *          connection variable.
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return statTrackers;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return HTTP_CLIENT_VARIABLE_METHODS;
  }



  /**
   * Indicates whether this variable type has a method with the specified name.
   *
   * @param  methodName  The name of the method.
   *
   * @return  <CODE>true</CODE> if this variable has a method with the specified
   *          name, or <CODE>false</CODE> if it does not.
   */
  @Override()
  public boolean hasMethod(String methodName)
  {
    for (int i=0; i < HTTP_CLIENT_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_CLIENT_VARIABLE_METHODS[i].getName().equals(methodName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the method number for the method that has the specified name and
   * argument types, or -1 if there is no such method.
   *
   * @param  methodName     The name of the method.
   * @param  argumentTypes  The list of argument types for the method.
   *
   * @return  The method number for the method that has the specified name and
   *          argument types.
   */
  @Override()
  public int getMethodNumber(String methodName, String[] argumentTypes)
  {
    for (int i=0; i < HTTP_CLIENT_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_CLIENT_VARIABLE_METHODS[i].hasSignature(methodName,
                                                       argumentTypes))
      {
        return i;
      }
    }

    return -1;
  }



  /**
   * Retrieves the return type for the method with the specified name and
   * argument types.
   *
   * @param  methodName     The name of the method.
   * @param  argumentTypes  The set of argument types for the method.
   *
   * @return  The return type for the method, or <CODE>null</CODE> if there is
   *          no such method defined.
   */
  @Override()
  public String getReturnTypeForMethod(String methodName,
                                       String[] argumentTypes)
  {
    for (int i=0; i < HTTP_CLIENT_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_CLIENT_VARIABLE_METHODS[i].hasSignature(methodName,
                                                       argumentTypes))
      {
        return HTTP_CLIENT_VARIABLE_METHODS[i].getReturnType();
      }
    }

    return null;
  }



  /**
   * Executes the specified method, using the provided variables as arguments
   * to the method, and makes the return value available to the caller.
   *
   * @param  lineNumber    The line number of the script in which the method
   *                       call occurs.
   * @param  methodNumber  The method number of the method to execute.
   * @param  arguments     The set of arguments to use for the method.
   *
   * @return  The value returned from the method, or <CODE>null</CODE> if it
   *          does not return a value.
   *
   * @throws  ScriptException  If the specified method does not exist, or if a
   *                           problem occurs while attempting to execute it.
   */
  @Override()
  public Variable executeMethod(int lineNumber, int methodNumber,
                                Argument[] arguments)
         throws ScriptException
  {
    switch (methodNumber)
    {
      case BLIND_TRUST_METHOD_NUMBER:
        SSLSocketFactory socketFactory = httpClient.getSSLSocketFactory();
        if (socketFactory == null)
        {
          return new BooleanVariable(false);
        }
        else
        {

          return new BooleanVariable(socketFactory instanceof
                                     JSSEBlindTrustSocketFactory);
        }
      case CLEAR_COOKIES_METHOD_NUMBER:
        httpClient.clearCookies();
        return null;
      case CLOSE_ALL_METHOD_NUMBER:
        httpClient.closeAll();
        return null;
      case COOKIES_ENABLED_METHOD_NUMBER:
        return new BooleanVariable(httpClient.cookiesEnabled());
      case DELETE_LOGOUT_COOKIES_METHOD_NUMBER:
        return new BooleanVariable(httpClient.deleteLogoutCookies());
      case DISABLE_AUTHENTICATION_METHOD_NUMBER:
        httpClient.disableAuthentication();
        return null;
      case DISABLE_DEBUG_MODE_METHOD_NUMBER:
        httpClient.disableDebugMode();
        return null;
      case DISABLE_PROXY_METHOD_NUMBER:
        httpClient.disableProxy();
        return null;
      case DISABLE_STATISTICS_METHOD_NUMBER:
        httpClient.disableStatisticsCollection();
        return null;
      case ENABLE_AUTHENTICATION_METHOD_NUMBER:
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();

        httpClient.enableAuthentication(sv1.getStringValue(),
                                        sv2.getStringValue());
        return null;
      case ENABLE_DEBUG_MODE_METHOD_NUMBER:
        httpClient.enableDebugMode();
        return null;
      case ENABLE_PROXY_1_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        IntegerVariable iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        httpClient.enableProxy(sv1.getStringValue(), iv1.getIntValue());
        return null;
      case ENABLE_PROXY_2_METHOD_NUMBER:
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        sv2 = (StringVariable)  arguments[2].getArgumentValue();
        StringVariable sv3 = (StringVariable) arguments[3].getArgumentValue();

        httpClient.enableProxy(sv1.getStringValue(), iv1.getIntValue(),
                               sv2.getStringValue(), sv3.getStringValue());
        return null;
      case ENABLE_STATISTICS_METHOD_NUMBER:
        httpClient.enableStatisticsCollection(clientID, threadID,
                                              collectionInterval,
                                              jobThread.getJobID(),
                                              jobThread.getStatReporter());
        return null;
      case FOLLOW_REDIRECTS_METHOD_NUMBER:
        return new BooleanVariable(httpClient.followRedirects());
      case GET_FAILURE_REASON_METHOD_NUMBER:
        return new StringVariable(failureReason);
      case GET_SOCKET_TIMEOUT_METHOD_NUMBER:
        return new IntegerVariable(httpClient.getSocketTimeout());
      case GZIP_ENABLED_METHOD_NUMBER:
        return new BooleanVariable(httpClient.enableGZIP());
      case REMOVE_COMMON_HEADER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        httpClient.removeCommonHeader(sv1.getStringValue());
        return null;
      case REMOVE_COOKIE_1_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        httpClient.removeCookie(sv1.getStringValue());
        return null;
      case REMOVE_COOKIE_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();
        httpClient.removeCookie(sv1.getStringValue(), sv2.getStringValue());
        return null;
      case RETRIEVE_ASSOCIATED_FILES_METHOD_NUMBER:
        return new BooleanVariable(httpClient.retrieveAssociatedFiles());
      case SEND_REQUEST_1_METHOD_NUMBER:
        HTTPRequestVariable hrv =
             (HTTPRequestVariable) arguments[0].getArgumentValue();
        failureReason = null;

        try
        {
          HTTPResponse httpResponse = httpClient.sendRequest(hrv.httpRequest);
          return new HTTPResponseVariable(httpResponse);
        }
        catch (Exception e)
        {
          failureReason = e.toString();
          return new HTTPResponseVariable(null);
        }
      case SEND_REQUEST_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        failureReason = null;

        try
        {
          HTTPRequest httpRequest =
               new HTTPRequest(true, new URL(sv1.getStringValue()));
          HTTPResponse httpResponse = httpClient.sendRequest(httpRequest);
          return new HTTPResponseVariable(httpResponse);
        }
        catch (Exception e)
        {
          failureReason = e.toString();
          return new HTTPResponseVariable(null);
        }
      case SET_BLIND_TRUST_METHOD_NUMBER:
        BooleanVariable bv = (BooleanVariable) arguments[0].getArgumentValue();
        if (bv.getBooleanValue())
        {
          try
          {
            httpClient.setSSLSocketFactory(new JSSEBlindTrustSocketFactory());
          } catch (Exception e) {}
        }
        else
        {
          httpClient.setSSLSocketFactory(null);
        }
        return null;
      case SET_CLIENT_ADDRESS_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        try
        {
          String addressStr = sv1.getStringValue();
          if ((addressStr == null) || (addressStr.length() == 0))
          {
            httpClient.setClientAddress((InetAddress) null);
          }
          else
          {
            httpClient.setClientAddress(InetAddress.getByName(addressStr));
          }

          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = e.toString();
          return new BooleanVariable(false);
        }
      case SET_COMMON_HEADER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();
        httpClient.setCommonHeader(sv1.getStringValue(), sv2.getStringValue());
        return null;
      case SET_COOKIES_ENABLED_METHOD_NUMBER:
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        httpClient.setCookiesEnabled(bv.getBooleanValue());
        return null;
      case SET_DELETE_LOGOUT_COOKIES_METHOD_NUMBER:
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        httpClient.setDeleteLogoutCookies(bv.getBooleanValue());
        return null;
      case SET_FOLLOW_REDIRECTS_METHOD_NUMBER:
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        httpClient.setFollowRedirects(bv.getBooleanValue());
        return null;
      case SET_GZIP_ENABLED_METHOD_NUMBER:
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        httpClient.setEnableGZIP(bv.getBooleanValue());
        return null;
      case SET_RETRIEVE_ASSOCIATED_FILES_METHOD_NUMBER:
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        httpClient.setRetrieveAssociatedFiles(bv.getBooleanValue());
        return null;
      case SET_SOCKET_TIMEOUT_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        httpClient.setSocketTimeout(iv1.getIntValue());
        return null;
      case SET_USE_KEEPALIVE_METHOD_NUMBER:
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        httpClient.setUseKeepAlive(bv.getBooleanValue());
        return null;
      case USE_KEEPALIVE_METHOD_NUMBER:
        return new BooleanVariable(httpClient.useKeepAlive());
      default:
        throw new ScriptException(lineNumber,
                                  "There is no method " + methodNumber +
                                  " defined for " + getArgumentType() +
                                  " variables.");
    }
  }



  /**
   * Assigns the value of the provided argument to this variable.  The value of
   * the provided argument must be of the same type as this variable.
   *
   * @param  argument  The argument whose value should be assigned to this
   *                   variable.
   *
   * @throws  ScriptException  If a problem occurs while performing the
   *                           assignment.
   */
  @Override()
  public void assign(Argument argument)
         throws ScriptException
  {
    if (! argument.getArgumentType().equals(HTTP_CLIENT_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                HTTP_CLIENT_VARIABLE_TYPE + " rejected.");
    }

    HTTPClientVariable hcv =
         (HTTPClientVariable) argument.getArgumentValue();
    httpClient    = hcv.httpClient;
    failureReason = hcv.failureReason;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return "HTTP Client";
  }
}

