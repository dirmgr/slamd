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
package com.slamd.http;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.StringTokenizer;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.slamd.asn1.ASN1Element;
import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.util.Base64;



/**
 * This class defines a client that may be used for communicating with Web
 * servers using HTTP or HTTPS.  It offers a number of features for behaving
 * like actual Web browsers, including the ability to parse the contents of an
 * HTML document, the ability to include images when retrieving a page, and the
 * ability to operate through a proxy server.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTTPClient
{
  /**
   * The size of the buffer that we will use for reading data.
   */
  public static final int BUFFER_SIZE = 4096;



  /**
   * The prefix that will be used for the HTTP header that sends authentication
   * information to the remote Web server.
   */
  public static final String AUTH_HEADER_PREFIX = "Authorization: Basic ";



  /**
   * The prefix that will be used for the HTTP header that sends authentication
   * information to the proxy server.
   */
  public static final String PROXY_AUTH_HEADER_PREFIX =
       "Proxy-Authorization: Basic ";



  /**
   * The display name for the stat tracker used to keep track of the number of
   * redirects followed.
   */
  public static final String STAT_TRACKER_REDIRECTS_FOLLOWED =
       "HTTP Redirects Followed";



  /**
   * The display name for the stat tracker used to keep track of the total
   * number of requests processed.
   */
  public static final String STAT_TRACKER_REQUESTS_PROCESSED =
       "HTTP Requests Processed";



  /**
   * The display name for the stat tracker used to keep track of the length of
   * time required to retrieve the content of the response.
   */
  public static final String STAT_TRACKER_RESPONSE_CONTENT_TIME =
       "HTTP Content Response Time";



  /**
   * The display name for the stat tracker used to keep track of the response
   * codes from all the requests.
   */
  public static final String STAT_TRACKER_RESPONSE_CODES =
       "HTTP Response Codes";



  /**
   * The display name for the stat tracker used to keep track of the length of
   * time required to retrieve send the request and retrieve the header.
   */
  public static final String STAT_TRACKER_RESPONSE_HEADER_TIME =
       "HTTP Header Response Time";



  /**
   * The display name for the stat tracker used to keep track of the size in
   * bytes of the response.
   */
  public static final String STAT_TRACKER_RESPONSE_SIZE =
       "HTTP Response Content Size";



  /**
   * The display name for the stat tracker used to keep track of the total
   * length of time required to handle a request.
   */
  public static final String STAT_TRACKER_TOTAL_REQUEST_TIME =
       "Total HTTP Request Time";



  // The list of cookies held by this client.
  ArrayList<HTTPCookie> cookieList;

  // Indicates whether the client will accept GZIP-encoded content.
  boolean enableGZIP;

  // Indicates whether cookie support is enabled for this client.
  boolean cookiesEnabled;

  // Indicates whether this client is operating in debug mode.
  boolean debugMode;

  // Indicates whether the client should automatically delete any cookie whose
  // value is set to "LOGOUT".
  boolean deleteLogoutCookies;

  // Indicates whether to automatically follow redirects returned by the server.
  boolean followRedirects;

  // Indicates whether to maintain statistics for the client.
  boolean keepStats;

  // Indicates whether associated files (e.g., images, style sheets, etc.)
  // should be retrieved whenever reading an HTML document.
  boolean retrieveAssociatedFiles;

  // Indicates whether the stat trackers are currently active.
  boolean trackersActive;

  // Indicates whether to use the HTTP 1.1 keepalive feature.
  boolean useKeepAlive;

  // The stat tracker used to keep track of the response codes for the requests.
  CategoricalTracker responseCodes;

  // A map that associates a host/port pair with a socket so that existing
  // connections can be re-used if available.
  HashMap<String,Socket> socketHash;

  // The stat tracker used to keep track of the number of redirects followed.
  IncrementalTracker redirectsFollowed;

  // The stat tracker used to keep track of the number of requests processed.
  IncrementalTracker requestsProcessed;

  // The address that should be used for the client system.
  InetAddress clientAddress;

  // The port number of the proxy server to use.
  int proxyPort;

  // The maximum length of time in milliseconds to block when trying to read
  // data from the client.
  int socketTimeout;

  // The stat tracker used to keep track of the average size of each response.
  IntegerValueTracker responseSizes;

  // The hash map containing common headers that should always be added to
  // requests.
  LinkedHashMap<String,String> commonHeaderMap;

  // The writer that will be used for debug messages.
  PrintStream debugWriter;

  // The socket factory used to create SSL sockets.
  SSLSocketFactory sslSocketFactory;

  // The user ID to use to authenticate to the remote server.
  String authID;

  // The password to use to authenticate to the remote server.
  String authPW;

  // The user ID to use to authenticate to the proxy server.
  String proxyAuthID;

  // The password to use to authenticate to the proxy server.
  String proxyAuthPW;

  // The address of the proxy server to use.
  String proxyHost;

  // The stat tracker used to keep track of the length of time required to
  // retrieve the content of the response.
  TimeTracker contentTimer;

  // The stat tracker used to keep track of the length of time required to
  // retrieve the header of the response.
  TimeTracker headerTimer;

  // The stat tracker used to keep track of the total length of time required to
  // process the request.
  TimeTracker requestTimer;



  /**
   * Creates a new instance of this HTTP client.  It will not use a proxy
   * server, it will not perform authentication, and it will not automatically
   * retrieve images when retrieving an HTML document.
   */
  public HTTPClient()
  {
    cookiesEnabled          = true;
    enableGZIP              = true;
    debugMode               = false;
    debugWriter             = null;
    deleteLogoutCookies     = false;
    followRedirects         = false;
    keepStats               = false;
    trackersActive          = false;
    retrieveAssociatedFiles = false;
    useKeepAlive            = false;
    authID                  = null;
    authPW                  = null;
    proxyAuthID             = null;
    proxyAuthPW             = null;
    socketHash              = new HashMap<String,Socket>();
    commonHeaderMap         = new LinkedHashMap<String,String>();
    cookieList              = new ArrayList<HTTPCookie>();
    sslSocketFactory        = null;
    clientAddress           = null;
    socketTimeout           = 0;


    // We'll allow the use of a client property for use when configuring a
    // proxy in case the underlying job doesn't support it.  This will only
    // be set if the parameters are defined, have a nonzero length, and the port
    // is a valid integer.  If the proxy is enabled, then it will be enabled for
    // both HTTP and HTTPS.
    String proxyHostProperty = System.getProperty("http.ProxyHost");
    String proxyPortProperty = System.getProperty("http.ProxyPort");
    if ((proxyHostProperty != null) && (proxyPortProperty != null) &&
        (proxyHostProperty.length() > 0) && (proxyPortProperty.length() > 0))
    {
      try
      {
        proxyPort = Integer.parseInt(proxyPortProperty);
        proxyHost = proxyHostProperty;
      } catch (Exception e) {}
    }
  }



  /**
   * Indicates that this HTTP client should operate in debug mode.  Debug
   * messages will be sent to standard error.
   */
  public void enableDebugMode()
  {
    debugMode   = true;
    debugWriter = System.err;
  }



  /**
   * Indicates that this HTTP client should operate in debug mode.  Debug
   * messages will be sent to the provided print stream.
   *
   * @param  debugStream  The print stream to which debug messages should be
   *                      sent.
   */
  public void enableDebugMode(PrintStream debugStream)
  {
    debugMode   = true;
    debugWriter = debugStream;
  }



  /**
   * Indicates that this HTTP client should not operate in debug mode.
   */
  public void disableDebugMode()
  {
    debugMode   = false;
    debugWriter = null;
  }



  /**
   * Writes the provided message to the debug writer.  Note that it is the
   * responsibility of the caller to ensure that debugging is enabled before
   * calling this method.
   *
   * @param  message  The message to be written.
   */
  public void debug(String message)
  {
    debugWriter.print("Thread ");
    debugWriter.print(Thread.currentThread().getName());
    debugWriter.print(" -- ");
    debugWriter.println(message);
  }



  /**
   * Retrieves the client address that will be used for the connections created
   * by this HTTP client.
   *
   * @return  The client address that will be used for the connections created
   *          by this HTTP client, or <CODE>null</CODE> if the default client
   *          address should be used.
   */
  public InetAddress getClientAddress()
  {
    return clientAddress;
  }



  /**
   * Specifies the client address that should be used when this HTTP client
   * creates outbound connections.
   *
   * @param  clientAddress  The client address that should be used when this
   *                        HTTP client creates outbound connections.
   */
  public void setClientAddress(InetAddress clientAddress)
  {
    this.clientAddress = clientAddress;
    closeAll();
  }



  /**
   * Specifies the client address that should be used when this HTTP client
   * creates outbound connections.
   *
   * @param  clientAddress  The client address that should be used when this
   *                        HTTP client creates outbound connections.
   *
   * @throws  UnknownHostException  If the provided address string cannot be
   *                                resolved to an actual address.
   */
  public void setClientAddress(String clientAddress)
         throws UnknownHostException
  {
    this.clientAddress = InetAddress.getByName(clientAddress);
    closeAll();
  }



  /**
   * Indicates whether this client should support GZIP-compressed data.
   *
   * @return  <CODE>true</CODE> if this client should support GZIP compression,
   *         or <CODE>false</CODE> if it should not.
   */
  public boolean enableGZIP()
  {
    return enableGZIP;
  }



  /**
   * Specifies whether this client should support accepting GZIP-compressed
   * data.
   *
   * @param  enableGZIP  Specifies whether this clietn should support accepting
   *                     GZIP-compressed data.
   */
  public void setEnableGZIP(boolean enableGZIP)
  {
    this.enableGZIP = enableGZIP;
  }



  /**
   * Specifies the socket factory that should be used to create SSL-based
   * connections.
   *
   * @param  sslSocketFactory  The socket factory that should be used to create
   *                           SSL-based connections.
   */
  public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory)
  {
    this.sslSocketFactory = sslSocketFactory;
  }



  /**
   * Retrieves the socket factory that will be used to create SSL-based
   * connections.
   *
   * @return  The socket factory that will be used to create SSL-based
   *          connections.
   */
  public SSLSocketFactory getSSLSocketFactory()
  {
    return sslSocketFactory;
  }



  /**
   * Indicates that the client should communicate with the specified proxy
   * server rather than attempting to communicate directly with the remote Web
   * server.  No authentication will be used when communicating with the proxy
   * server.
   *
   * @param  proxyHost  The address of the proxy server.
   * @param  proxyPort  The port of the proxy server.
   */
  public void enableProxy(String proxyHost, int proxyPort)
  {
    this.proxyHost   = proxyHost;
    this.proxyPort   = proxyPort;
    this.proxyAuthID = null;
    this.proxyAuthPW = null;
  }



  /**
   * Indicates that the client should communicate with the specified proxy
   * server rather than attempting to communicate directly with the remote Web
   * server.  Basic authentication will be performed using the provided user ID
   * and password.
   *
   * @param  proxyHost    The address of the proxy server.
   * @param  proxyPort    The port of the proxy server.
   * @param  proxyAuthID  The user ID to use to authenticate to the proxy
   *                      server.
   * @param  proxyAuthPW  The password to use to authenticate to the proxy
   *                      server.
   */
  public void enableProxy(String proxyHost, int proxyPort, String proxyAuthID,
                          String proxyAuthPW)
  {
    this.proxyHost   = proxyHost;
    this.proxyPort   = proxyPort;
    this.proxyAuthID = proxyAuthID;
    this.proxyAuthPW = proxyAuthPW;
  }



  /**
   * Indicates that the client should not use a proxy server but rather try to
   * communicate directly with the remote Web server.
   */
  public void disableProxy()
  {
    this.proxyHost   = null;
    this.proxyPort   = -1;
    this.proxyAuthID = null;
    this.proxyAuthPW = null;
  }



  /**
   * Indicates whether the client will attempt to forward requests through an
   * HTTP proxy server.
   *
   * @return  <CODE>true</CODE> if an HTTP proxy server will be used, or
   *          <CODE>false</CODE> if not.
   */
  public boolean proxyEnabled()
  {
    return ((proxyHost != null) && (proxyHost.length() > 0) &&
            (proxyPort >= 1) && (proxyPort <= 65535));
  }



  /**
   * Indicates whether the client will attempt to authenticate to an HTTP proxy
   * server.
   *
   * @return  <CODE>true</CODE> if an HTTP proxy server will be used and
   *          authentication information will be provided to it, or
   *          <CODE>false</CODE> if not.
   */
  public boolean proxyAuthenticationEnabled()
  {
    return ((proxyHost != null) && (proxyHost.length() > 0) &&
            (proxyPort >= 1) && (proxyPort <= 65535) &&
            (proxyAuthID != null) && (proxyAuthID.length() > 0) &&
            (proxyAuthPW != null) && (proxyAuthPW.length() > 0));
  }



  /**
   * Retrieves the address of the proxy server that has been configured.
   *
   * @return  The address of the proxy server that has been configured, or
   *          <CODE>null</CODE> if none has been specified.
   */
  public String getProxyHost()
  {
    return proxyHost;
  }



  /**
   * Retrieves the port number of the proxy server that has been configured.
   *
   * @return  The port number of the proxy server that has been configured, or
   *          -1 if none has been specified.
   */
  public int getProxyPort()
  {
    return proxyPort;
  }



  /**
   * Retrieves the username that will be provided to the HTTP proxy server if
   * authentication will be performed.
   *
   * @return  The username that will be provided to the HTTP proxy server if
   *          authentication will be performed, or <CODE>null</CODE> if no
   *          authentication will be performed.
   */
  public String getProxyAuthID()
  {
    return proxyAuthID;
  }



  /**
   * Retrieves the password that will be provided to the HTTP proxy server if
   * authentication will be performed.
   *
   * @return  The password that will be provided to the HTTP proxy server if
   *          authentication will be performed, or <CODE>null</CODE> if no
   *          authentication will be performed.
   */
  public String getProxyAuthPassword()
  {
    return proxyAuthPW;
  }



  /**
   * Indicates that authentication should be performed for the remote Web server
   * using the provided information.
   *
   * @param  authID  The user ID to use to authenticate to the remote Web
   *                 server.
   * @param  authPW  The password to use to authenticate to the remote Web
   *                 server.
   */
  public void enableAuthentication(String authID, String authPW)
  {
    this.authID = authID;
    this.authPW = authPW;
  }



  /**
   * Indicates that no authentication should be performed for the remote Web
   * server.
   */
  public void disableAuthentication()
  {
    authID = null;
    authPW = null;
  }



  /**
   * Indicates whether the client will attempt to provide authentication
   * information to the remote HTTP server.
   *
   * @return  <CODE>true</CODE> if HTTP authentication will be performed, or
   *          <CODE>false</CODE> if not.
   */
  public boolean authenticationEnabled()
  {
    return ((proxyHost != null) && (proxyHost.length() > 0) &&
            (proxyPort >= 1) && (proxyPort <= 65535));
  }



  /**
   * Retrieves the username that will be provided to the remote HTTP server if
   * authentication will be performed.
   *
   * @return  The username that will be provided to the remote HTTP server if
   *          authentication will be performed, or <CODE>null</CODE> if no
   *          authentication will be performed.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Retrieves the password that will be provided to the remote HTTP server if
   * authentication will be performed.
   *
   * @return  The password that will be provided to the remote HTTP server if
   *          authentication will be performed, or <CODE>null</CODE> if no
   *          authentication will be performed.
   */
  public String getAuthPassword()
  {
    return authPW;
  }



  /**
   * Indicates whether this client will automatically delete any cookie whose
   * value is set to "LOGOUT".
   *
   * @return  <CODE>true</CODE> if this client will automatically delete any
   *          cookie whose value is set to "LOGOUT", or <CODE>false</CODE> if
   *          not.
   */
  public boolean deleteLogoutCookies()
  {
    return deleteLogoutCookies;
  }



  /**
   * Specifies whether this client will automatically delete any cookie whose
   * value is set to "LOGOUT".
   *
   * @param  deleteLogoutCookies  Specifies whether this client will
   *                              automatically delete any cookie whose value is
   *                              set to "LOGOUT".
   */
  public void setDeleteLogoutCookies(boolean deleteLogoutCookies)
  {
    this.deleteLogoutCookies = deleteLogoutCookies;
  }



  /**
   * Indicates whether this client will attempt to automatically follow
   * redirects returned by the server.
   *
   * @return  <CODE>true</CODE> if this client will attempt to follow redirects,
   *          or <CODE>false</CODE> if not.
   */
  public boolean followRedirects()
  {
    return followRedirects;
  }



  /**
   * Specifies whether this client should attempt to automatically follow
   * redirects returned by the server.
   *
   * @param  followRedirects  Indicates whether to try to automatically follow
   *                          redirects returned by the server.
   */
  public void setFollowRedirects(boolean followRedirects)
  {
    this.followRedirects = followRedirects;
  }



  /**
   * Indicates whether this connection will attempt to use HTTP 1.1 keepalive to
   * possibly re-use the same connection for multiple requests.
   *
   * @return  <CODE>true</CODE> if the connection should attempt to use
   *          keepalive, or <CODE>false</CODE> if not.
   */
  public boolean useKeepAlive()
  {
    return useKeepAlive;
  }



  /**
   * Indicates whether to use HTTP 1.1 keepalive to possibly re-use the same
   * connection for multiple requests.
   *
   * @param  useKeepAlive  Indicates whether to use HTTP 1.1 keepalive to
   *                       possibly re-use the same connection for multiple
   *                       requests.
   */
  public void setUseKeepAlive(boolean useKeepAlive)
  {
    this.useKeepAlive = useKeepAlive;
  }



  /**
   * Retrieves the maximum length of time in milliseconds that the client should
   * block while waiting for data from the server.
   *
   * @return  The maximum length of time in milliseconds that the client should
   *          block while waiting for data from the server, or 0 if it should
   *          wait indefinitely (until there is data to read).
   */
  public int getSocketTimeout()
  {
    return socketTimeout;
  }



  /**
   * Specifies the maximum length of time in milliseconds that the client should
   * block while waiting for data from the server.  A value of zero indicates
   * that there should not be any time limit.
   *
   * @param  socketTimeout  The maximum length of time in milliseconds that the
   *                        client should block while waiting for data from the
   *                        server.
   */
  public void setSocketTimeout(int socketTimeout)
  {
    if (socketTimeout < 0)
    {
      this.socketTimeout = 0;
    }
    else
    {
      this.socketTimeout = socketTimeout;
    }
  }



  /**
   * Indicates whether this client should automatically retrieve any additional
   * files associated with the HTML documents that are retrieved.  Note that the
   * contents of those files will not be available to the client calling
   * <CODE>sendRequest()</CODE> -- only the contents of the primary document
   * requested.
   *
   * @return  <CODE>true</CODE> if this client should automatically retrieve any
   *          additional files associated with the HTML documents that are
   *          retrieved, or <CODE>false</CODE> if not.
   */
  public boolean retrieveAssociatedFiles()
  {
    return retrieveAssociatedFiles;
  }



  /**
   * Specifies whether this client should automatically retrieve any additional
   * files (images, external style sheets, frame elements, etc.) associated with
   * any HTML documents that it retrieves.
   *
   * @param  retrieveAssociatedFiles  Indicates whether this client should
   *                                  automatically retrieve any additional
   *                                  files associated with the HTML documents
   *                                  that are retrieved.
   */
  public void setRetrieveAssociatedFiles(boolean retrieveAssociatedFiles)
  {
    this.retrieveAssociatedFiles = retrieveAssociatedFiles;
  }



  /**
   * Retrieves a two-dimensional array containing the names and values of all
   * headers that will always be included in requests sent using this client.
   *
   * @return  A two-dimensional array containing the names and values of all
   *          headers that will always be included in requests sent using this
   *          client.
   */
  public String[][] getCommonHeaders()
  {
    Set        keySet        = commonHeaderMap.keySet();
    String[][] commonHeaders = new String[keySet.size()][2];

    int i = 0;
    for (String s : commonHeaderMap.keySet())
    {
      commonHeaders[i][0] = s;
      commonHeaders[i][1] = commonHeaderMap.get(s);
      i++;
    }

    return commonHeaders;
  }



  /**
   * Retrieves the value of the common header with the provided name.
   *
   * @param  name  The name of the common header whose value should be
   *               retrieved.
   *
   * @return  The value of the specified common header, or <CODE>null</CODE> if
   *          no such header has been defined.
   */
  public String getCommonHeader(String name)
  {
    return commonHeaderMap.get(name.toLowerCase());
  }



  /**
   * Retrieves the names of the common headers that have been defined for this
   * client.
   *
   * @return  The names of the common headers that have been defined for this
   *          client.
   */
  public String[] getCommonHeaderNames()
  {
    String[] commonHeaderValues = new String[commonHeaderMap.size()];
    return commonHeaderMap.keySet().toArray(commonHeaderValues);
  }



  /**
   * Retrieves the values of the common headers that have been defined for this
   * client.  The order of the header values will be the same as the order of
   * the names returned by the <CODE>getCommonHeaderNames()</CODE> method.
   *
   * @return  The values of the common headers that have been defined for this
   *          client.
   */
  public String[] getCommonHeaderValues()
  {
    String[] commonHeaderValues = new String[commonHeaderMap.size()];
    return commonHeaderMap.values().toArray(commonHeaderValues);
  }



  /**
   * Adds a common header with the specified name and value.  If a header
   * already exists with the specified name, then the given value will replace
   * the existing value.  If the given value is <CODE>null</CODE>, then any
   * existing header with that name will be removed.
   *
   * @param  name   The name to use for the common header.
   * @param  value  The value to use for the common header.
   */
  public void setCommonHeader(String name, String value)
  {
    String lowerName = name.toLowerCase();

    if (value == null)
    {
      commonHeaderMap.remove(lowerName);
    }
    else
    {
      commonHeaderMap.put(lowerName, value);
    }
  }



  /**
   * Removes the common header with the specified name.  If no such header is
   * defined for this client, then no action will be performed.
   *
   * @param  name  The name of the common header to be removed.
   */
  public void removeCommonHeader(String name)
  {
    commonHeaderMap.remove(name.toLowerCase());
  }



  /**
   * Removes all common headers that have been defined for this client.
   */
  public void clearCommonHeaders()
  {
    commonHeaderMap.clear();
  }



  /**
   * Indicates whether support for cookies is enabled in this client.
   *
   * @return  <CODE>true</CODE> if support for cookies is enabled, or
   *          <CODE>false</CODE> if not.
   */
  public boolean cookiesEnabled()
  {
    return cookiesEnabled;
  }



  /**
   * Specifies whether cookie support should be enabled for this client.
   *
   * @param  cookiesEnabled  Indicates whether cookie support should be enabled
   *                         for this client.
   */
  public void setCookiesEnabled(boolean cookiesEnabled)
  {
    this.cookiesEnabled = cookiesEnabled;
  }



  /**
   * Retrieves an array of cookies that apply to the given URL.  If there are no
   * applicable cookies, then an empty array will be returned.
   *
   * @param  requestURL  The URL for which to retrieve the applicable cookies.
   *
   * @return  An array of cookies that apply to the given URL.
   */
  public HTTPCookie[] getCookies(URL requestURL)
  {
    if (! cookiesEnabled)
    {
      return new HTTPCookie[0];
    }

    ArrayList<HTTPCookie> matchingCookies = new ArrayList<HTTPCookie>();

    long currentTime = System.currentTimeMillis();
    for (int i=0; i < cookieList.size(); i++)
    {
      HTTPCookie cookie = cookieList.get(i);
      if (cookie.appliesToRequest(requestURL, currentTime))
      {
        matchingCookies.add(cookie);
      }
    }

    HTTPCookie[] cookies = new HTTPCookie[matchingCookies.size()];
    matchingCookies.toArray(cookies);
    return cookies;
  }



  /**
   * Adds the specified cookie to the set of cookies associated with this
   * client.  If the cookie information provided matches that of another cookie
   * that already exists, then that cookie will be updated.  If the provided
   * cookie has an expiration date in the past, then the specified cookie will
   * be removed.
   *
   * @param  cookie  The cookie to be added to this client.
   */
  public void addCookie(HTTPCookie cookie)
  {
    if (! cookiesEnabled)
    {
      return;
    }


    // See if the provided cookie is expired and therefore should be deleted.
    if ((cookie.getExpirationDate() > 0) &&
        (cookie.getExpirationDate() < System.currentTimeMillis()))
    {
      for (int i=0; i < cookieList.size(); i++)
      {
        HTTPCookie existingCookie = cookieList.get(i);
        if (existingCookie.getName().equals(cookie.getName()) &&
            existingCookie.getDomain().equals(cookie.getDomain()))
        {
          cookieList.remove(i);
          return;
        }
      }
    }


    // See if the provided cookie already exists.  If so, replace it.
    for (int i=0; i < cookieList.size(); i++)
    {
      HTTPCookie existingCookie = cookieList.get(i);
      if (existingCookie.getName().equals(cookie.getName()) &&
          existingCookie.getDomain().equals(cookie.getDomain()))
      {
        // If we should automatically delete logout cookies and the value of the
        // new cookie is "LOGOUT", then delete it.  Otherwise, replace it.
        if (deleteLogoutCookies && cookie.getValue().equals("LOGOUT"))
        {
          cookieList.remove(i);
        }
        else
        {
          cookieList.set(i, cookie);
        }

        return;
      }
    }


    // Add the cookie to the list.
    cookieList.add(cookie);
  }



  /**
   * Removes the cookie with the specified name from the set of cookies for this
   * client.  If no cookie exists with the given name, then no action will be
   * taken.
   *
   * @param  name  The name of the cookie to remove.
   *
   * @return  <CODE>true</CODE> if the requested cookie was found and removed,
   *          or <CODE>false</CODE> if it was not.
   */
  public boolean removeCookie(String name)
  {
    if (! cookiesEnabled)
    {
      return false;
    }


    // See if the specified cookie exists.  If so, then remove it.
    for (int i=0; i < cookieList.size(); i++)
    {
      HTTPCookie cookie = cookieList.get(i);
      if (cookie.getName().equals(name))
      {
        cookieList.remove(i);
        return true;
      }
    }

    return false;
  }



  /**
   * Removes the cookie with the specified name and value from the set of
   * cookies for this client.  If no cookie exists with the given name, or if
   * the specified cookie exists with a different value, then no action will be
   * taken.
   *
   * @param  name   The name of the cookie to remove.
   * @param  value  The value for the cookie to remove.
   *
   * @return  <CODE>true</CODE> if the requested cookie was found and removed,
   *          or <CODE>false</CODE> if it was not.
   */
  public boolean removeCookie(String name, String value)
  {
    if (! cookiesEnabled)
    {
      return false;
    }


    // See if the specified cookie exists.  If so, then remove it.
    for (int i=0; i < cookieList.size(); i++)
    {
      HTTPCookie cookie = cookieList.get(i);
      if (cookie.getName().equals(name) && cookie.getValue().equals(value))
      {
        cookieList.remove(i);
        return true;
      }
    }

    return false;
  }



  /**
   * Clears all cookie information associated with this client.
   */
  public void clearCookies()
  {
    cookieList.clear();
  }



  /**
   * Indicates whether this HTTP client is currently configured to collect
   * statistics about the operations it performs.
   *
   * @return  <CODE>true</CODE> if this HTTP client is configured to collect
   *          statistics, or <CODE>false</CODE> if it is not.
   */
  public boolean statisticsCollectionEnabled()
  {
    return keepStats;
  }



  /**
   * Indicates that the client should automatically maintain a set of stat
   * trackers that keep track of various statistics around HTTP processing.
   *
   * @param  clientID            The client ID to use for the stat trackers.
   * @param  threadID            The thread ID to use for the stat trackers.
   * @param  collectionInterval  The statistics collection interval to use for
   *                             the stat trackers.
   */
  public void enableStatisticsCollection(String clientID, String threadID,
                                         int collectionInterval)
  {
    enableStatisticsCollection(clientID, threadID, collectionInterval, null,
                               null);
  }



  /**
   * Indicates that the client should automatically maintain a set of stat
   * trackers that keep track of various statistics around HTTP processing.
   *
   * @param  clientID            The client ID to use for the stat trackers.
   * @param  threadID            The thread ID to use for the stat trackers.
   * @param  collectionInterval  The statistics collection interval to use for
   *                             the stat trackers.
   * @param  jobID               The job ID of the job with which this client is
   *                             associated.
   * @param  statReporter        The real-time stat reporter that should be used
   *                             for the statistics collected.
   */
  public void enableStatisticsCollection(String clientID, String threadID,
                                         int collectionInterval, String jobID,
                                         RealTimeStatReporter statReporter)
  {
    requestsProcessed = new IncrementalTracker(clientID, threadID,
                                               STAT_TRACKER_REQUESTS_PROCESSED,
                                               collectionInterval);

    requestTimer = new TimeTracker(clientID, threadID,
                                   STAT_TRACKER_TOTAL_REQUEST_TIME,
                                   collectionInterval);

    responseCodes = new CategoricalTracker(clientID, threadID,
                                           STAT_TRACKER_RESPONSE_CODES,
                                           collectionInterval);

    responseSizes = new IntegerValueTracker(clientID, threadID,
                                            STAT_TRACKER_RESPONSE_SIZE,
                                            collectionInterval);

    redirectsFollowed = new IncrementalTracker(clientID, threadID,
                                               STAT_TRACKER_REDIRECTS_FOLLOWED,
                                               collectionInterval);

    headerTimer = new TimeTracker(clientID, threadID,
                                  STAT_TRACKER_RESPONSE_HEADER_TIME,
                                  collectionInterval);

    contentTimer = new TimeTracker(clientID, threadID,
                                   STAT_TRACKER_RESPONSE_CONTENT_TIME,
                                   collectionInterval);

    if ((statReporter != null) && (jobID != null))
    {
      requestsProcessed.enableRealTimeStats(statReporter, jobID);
      requestTimer.enableRealTimeStats(statReporter, jobID);
      responseSizes.enableRealTimeStats(statReporter, jobID);
      redirectsFollowed.enableRealTimeStats(statReporter, jobID);
      headerTimer.enableRealTimeStats(statReporter, jobID);
      contentTimer.enableRealTimeStats(statReporter, jobID);
    }

    requestsProcessed.startTracker();
    requestTimer.startTracker();
    responseCodes.startTracker();
    responseSizes.startTracker();
    redirectsFollowed.startTracker();
    headerTimer.startTracker();
    contentTimer.startTracker();

    keepStats = true;
    trackersActive = true;
  }



  /**
   * Stops all the stat trackers associated with this client but still
   * indicating stat statistics collection has been used so that the statistics
   * will be returned by the <CODE>getStatTrackers</CODE> method.
   */
  public void stopTrackers()
  {
    requestsProcessed.stopTracker();
    requestTimer.stopTracker();
    responseCodes.stopTracker();
    responseSizes.stopTracker();
    redirectsFollowed.stopTracker();
    headerTimer.stopTracker();
    contentTimer.stopTracker();

    trackersActive = false;
  }



  /**
   * Indicates that the client should not automatically maintain any statistics.
   * Any data that the client might have already collected will be lost.
   */
  public void disableStatisticsCollection()
  {
    keepStats      = false;
    trackersActive = false;
  }



  /**
   * Retrieves the stat tracker stubs that will be used to indicate the types
   * of statistics that will be collected by this HTTP client.  The stubs will
   * be returned whether or not statistics collection is enabled.
   *
   * @param  clientID            The client ID to use for the stubs.
   * @param  threadID            The thread ID to use for the stubs.
   * @param  collectionInterval  The collection interval to use for the stubs.
   *
   * @return  The requested stat tracker stubs.
   */
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[]
    {
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_REQUESTS_PROCESSED,
                             collectionInterval),

      new TimeTracker(clientID, threadID, STAT_TRACKER_TOTAL_REQUEST_TIME,
                      collectionInterval),

      new CategoricalTracker(clientID, threadID, STAT_TRACKER_RESPONSE_CODES,
                             collectionInterval),

      new IntegerValueTracker(clientID, threadID, STAT_TRACKER_RESPONSE_SIZE,
                              collectionInterval),

      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_REDIRECTS_FOLLOWED,
                             collectionInterval),

      new TimeTracker(clientID, threadID, STAT_TRACKER_RESPONSE_HEADER_TIME,
                      collectionInterval),

      new TimeTracker(clientID, threadID, STAT_TRACKER_RESPONSE_CONTENT_TIME,
                      collectionInterval)
    };
  }



  /**
   * Retrieves the set of stat trackers that have been maintained by this
   * client.
   *
   * @return  The set of stat trackers that have been maintained by this client,
   *          or an empty array if statistics collection has not been enabled.
   */
  public StatTracker[] getStatTrackers()
  {
    if (keepStats)
    {
      if (trackersActive)
      {
        requestsProcessed.stopTracker();
        requestTimer.stopTracker();
        responseCodes.stopTracker();
        responseSizes.stopTracker();
        redirectsFollowed.stopTracker();
        headerTimer.stopTracker();
        contentTimer.stopTracker();

        trackersActive = false;
      }

      return new StatTracker[]
      {
        requestsProcessed,
        requestTimer,
        responseCodes,
        responseSizes,
        redirectsFollowed,
        headerTimer,
        contentTimer
      };
    }
    else
    {
      return new StatTracker[0];
    }
  }



  /**
   * Sends the provided request to the specified server and returns the
   * response.  If so configured, any associated files will also be retrieved.
   *
   * @param  request  The request to send to the server.
   *
   * @return  The response returned by the server.
   *
   * @throws  HTTPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  public HTTPResponse sendRequest(HTTPRequest request)
         throws HTTPException
  {
    if (trackersActive)
    {
      requestTimer.startTimer();
      headerTimer.startTimer();
    }


    HTTPResponse response = sendRequestInternal(request, trackersActive);


    if (trackersActive)
    {
      contentTimer.stopTimer();
      requestTimer.stopTimer();

      requestsProcessed.increment();
      responseCodes.increment(String.valueOf(response.getStatusCode()));
      responseSizes.addValue(response.getResponseData().length);
    }

    return response;
  }



  /**
   * Processes the provided request, possibly keeping statistics.
   *
   * @param  request         The request to send to the server.
   * @param  trackersActive  Indicates whether to use the stat trackers.
   *
   * @return  The response read from the server.
   *
   * @throws  HTTPException  If a problem occurs while processing the request.
   */
  private HTTPResponse sendRequestInternal(HTTPRequest request,
                                           boolean trackersActive)
          throws HTTPException
  {
    String protocol = request.baseURL.getProtocol().toLowerCase();
    URL    url      = request.baseURL;

    boolean useSSL;
    if (protocol.equals("http"))
    {
      useSSL = false;
    }
    else if (protocol.equals("https"))
    {
      useSSL = true;
    }
    else
    {
      throw new HTTPException("Unsupported protocol \"" + protocol + '"');
    }

    if (debugMode)
    {
      debugWriter.println();
      debugWriter.println();
    }

    Socket socket = null;
    String hashKey;
    if (proxyHost == null)
    {
      String urlHost = url.getHost();
      int    urlPort = request.baseURL.getPort();
      if (urlPort == -1)
      {
        urlPort = (useSSL? 443 : 80);
      }

      hashKey = protocol + "://" + urlHost + ':' + urlPort;
      socket = socketHash.remove(hashKey);
      if ((socket == null) || (! socket.isConnected()))
      {
        if (useSSL)
        {
          try
          {
            if (sslSocketFactory == null)
            {
              sslSocketFactory =
                   (SSLSocketFactory) SSLSocketFactory.getDefault();
            }

            if (clientAddress == null)
            {
              socket = sslSocketFactory.createSocket(urlHost, urlPort);
            }
            else
            {
              socket = sslSocketFactory.createSocket(urlHost, urlPort,
                                                     clientAddress, 0);
            }

            socket.setReuseAddress(true);
            socket.setSoLinger(true, 0);
            socket.setSoTimeout(socketTimeout);

            if (debugMode)
            {
              debug("Established SSL connection " + hashKey);
            }
          }
          catch (Exception e)
          {
            throw new HTTPException("Unable to establish connection to " +
                                    hashKey + " -- " + e, e);
          }
        }
        else
        {
          try
          {
            if (clientAddress == null)
            {
              socket = new Socket(urlHost, urlPort);
            }
            else
            {
              socket = new Socket(urlHost, urlPort, clientAddress, 0);
            }

            socket.setReuseAddress(true);
            socket.setSoLinger(true, 0);
            socket.setSoTimeout(socketTimeout);

            if (debugMode)
            {
              debug("Established connection " + hashKey);
            }
          }
          catch (Exception e)
          {
            throw new HTTPException("Unable to establish connection to " +
                                    hashKey + " -- " + e, e);
          }
        }
      }
      else
      {
        if (debugMode)
        {
          debug("Retrieved connection " + hashKey + " from socket hash");
        }
      }
    }
    else
    {
      if (useSSL)
      {
        String urlHost = url.getHost();
        int    urlPort = request.baseURL.getPort();
        if (urlPort == -1)
        {
          urlPort = 443;
        }

        hashKey = "connect://" + urlHost + ':' + urlPort;
        socket  = getSSLThroughProxySocket(urlHost, urlPort);
      }
      else
      {
        hashKey = protocol + "://" + proxyHost + ':' + proxyPort;
        socket = socketHash.remove(hashKey);
        if ((socket == null) || (! socket.isConnected()))
        {
          try
          {
            if (clientAddress == null)
            {
              socket = new Socket(proxyHost, proxyPort);
            }
            else
            {
              socket = new Socket(proxyHost, proxyPort, clientAddress, 0);
            }

            if (debugMode)
            {
              debug("Established connection to proxy " + hashKey);
            }
          }
          catch (Exception e)
          {
            throw new HTTPException("Unable to establish HTTP connection to " +
                                    "proxy " + hashKey + " -- " + e, e);
          }
        }
        else
        {
          if (debugMode)
          {
            debug("Retrieved connection to proxy " + hashKey +
                  " from socket hash");
          }
        }
      }
    }


    InputStream  inputStream;
    OutputStream outputStream;
    try
    {
      inputStream  = socket.getInputStream();
      outputStream = socket.getOutputStream();
    }
    catch (Exception e)
    {
      throw new HTTPException("Unable to obtain input and/or output stream " +
                              "to communicate with the server " + hashKey +
                              " -- " + e, e);
    }


    try
    {
      String requestStr = request.generateHTTPRequest(this);
      outputStream.write(ASN1Element.getBytes(requestStr));
      if (debugMode)
      {
        debug("CLIENT REQUEST:");
        debug(requestStr);
      }
    }
    catch (IOException ioe)
    {
      throw new HTTPException("Unable to send request to the server " +
                              hashKey + " -- " + ioe, ioe);
    }


    HTTPResponse response;
    try
    {
      response = readResponse(request.getBaseURL(), inputStream,
                              trackersActive);
    }
    catch (Exception e)
    {
      try
      {
        inputStream.close();
        outputStream.close();
      } catch (Exception e2) {}

      try
      {
        socket.close();
      } catch (Exception e2) {}

      throw new HTTPException("Unable to read or parse the response from " +
                              "the server " + hashKey + " -- " + e, e);
    }


    // See if the connection is or should be closed.  If so, then try to close
    // it.  If not, then put it in the hash for later use.
    String connStr = response.getHeader("connection");
    if ((connStr == null) || (! connStr.equalsIgnoreCase("keep-alive")) ||
        (! socket.isConnected()))
    {
      try
      {
        inputStream.close();
        outputStream.close();
      } catch (Exception e) {}

      try
      {
        socket.close();
      } catch (Exception e) {}
    }
    else
    {
      socketHash.put(hashKey, socket);
    }


    // See if this is a redirect that should be followed.
    if (followRedirects && isRedirect(response.getStatusCode()))
    {
      String redirectURL = response.getHeader("location");
      if (redirectURL != null)
      {
        if (debugMode)
        {
          debug("Following redirect to " + redirectURL);
        }

        try
        {
          HTTPRequest redirectRequest = request.clone(new URL(redirectURL));
          HTTPResponse redirectResponse = sendRequestInternal(redirectRequest,
                                                              false);

          if (trackersActive)
          {
            redirectsFollowed.increment();
          }

          return redirectResponse;
        }
        catch (Exception e)
        {
          throw new HTTPException("Unable to follow redirect to " +
                                  redirectURL + ":  " + e, e);
        }
      }
    }


    // See if we should retrieve the files associated with this response.
    if (retrieveAssociatedFiles)
    {
      HTMLDocument document = response.getHTMLDocument();
      if (document != null)
      {
        String[] associatedFiles = document.getAssociatedFiles();
        for (int i=0; i < associatedFiles.length; i++)
        {
          if (debugMode)
          {
            debug("Trying to retrieve associated file " + associatedFiles[i]);
          }

          try
          {
            HTTPRequest associatedFileRequest =
                 new HTTPRequest(true, new URL(associatedFiles[i]));
            sendRequestInternal(associatedFileRequest, false);
          } catch (Exception e) {}
        }
      }
    }


    return response;
  }



  /**
   * Indicates whether the provided status code defines a redirect that may be
   * followed to get the actual content.
   *
   * @param  statusCode  The HTTP status code for which to make the
   *                     determination.
   *
   * @return  <CODE>true</CODE> if the provided status code will be used for a
   *          redirect, or <CODE>false</CODE> if not.
   */
  public static boolean isRedirect(int statusCode)
  {
    switch (statusCode)
    {
      case 300:
      case 301:
      case 302:
      case 303:
      case 305:
      case 307:
        return true;
      default:
        return false;
    }
  }



  /**
   * Reads an HTTP response from the server from the provided input stream.
   *
   * @param  requestURL   The URL used in the request sent to the server that
   *                      triggered this response.
   * @param  inputStream  The input stream from which to read the response.
   * @param  keepStats    Indicates whether to update the stat trackers as part
   *                      of this processing.
   *
   * @return  The response read from the server.
   *
   * @throws  IOException  If a problem occurs while reading data from the
   *                       server.
   *
   * @throws  HTTPException  If a problem occurs while trying to interpret the
   *                         response from the server.
   */
  private HTTPResponse readResponse(URL requestURL, InputStream inputStream,
                                    boolean keepStats)
          throws IOException, HTTPException
  {
    byte[] buffer  = new byte[BUFFER_SIZE];

    // Read an initial chunk of the response from the server.
    int bytesRead = inputStream.read(buffer);
    if (bytesRead < 0)
    {
      throw new IOException("Unexpected end of input stream from server");
    }

    // Hopefully, this initial chunk will contain the entire header, so look for
    // it.  Technically, HTTP is supposed to use CRLF as the end-of-line
    // character, so look for that first, but also check for LF by itself just
    // in case.
    int headerEndPos = -1;
    int dataStartPos = -1;
    for (int i=0; i < (bytesRead-3); i++)
    {
      if ((buffer[i] == '\r') && (buffer[i+1] == '\n') &&
          (buffer[i+2] == '\r') && (buffer[i+3] == '\n'))
      {
        headerEndPos = i;
        dataStartPos = i+4;
        break;
      }
    }

    if (headerEndPos < 0)
    {
      for (int i=0; i < (bytesRead-1); i++)
      {
        if ((buffer[i] == '\n') && (buffer[i+1] == '\n'))
        {
          headerEndPos = i;
          dataStartPos = i+2;
          break;
        }
      }
    }


    // In the event that we didn't get the entire header in the first pass, keep
    // reading until we do have enough.
    if (headerEndPos < 0)
    {
      byte[] buffer2 = new byte[BUFFER_SIZE];
      while (headerEndPos < 0)
      {
        int startPos      = bytesRead;
        int moreBytesRead = inputStream.read(buffer2);
        if (moreBytesRead < 0)
        {
          throw new IOException("Unexpected end of input stream from server " +
                                "when reading more data from response");
        }

        byte[] newBuffer = new byte[bytesRead + moreBytesRead];
        System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
        System.arraycopy(buffer2, 0, newBuffer, bytesRead, moreBytesRead);
        buffer = newBuffer;
        bytesRead += moreBytesRead;

        for (int i=startPos; i < (bytesRead-3); i++)
        {
          if ((buffer[i] == '\r') && (buffer[i+1] == '\n') &&
              (buffer[i+2] == '\r') && (buffer[i+3] == '\n'))
          {
            headerEndPos = i;
            dataStartPos = i+4;
            break;
          }
        }

        if (headerEndPos < 0)
        {
          for (int i=startPos; i < (bytesRead-1); i++)
          {
            if ((buffer[i] == '\n') && (buffer[i+1] == '\n'))
            {
              headerEndPos = i;
              dataStartPos = i+2;
              break;
            }
          }
        }
      }
    }


    // At this point, we should have the entire header, so read and analyze it.
    String          headerStr = new String(buffer, 0, headerEndPos);
    StringTokenizer tokenizer = new StringTokenizer(headerStr, "\r\n");
    HTTPResponse    response;
    if (tokenizer.hasMoreTokens())
    {
      String statusLine = tokenizer.nextToken();
      if (debugMode)
      {
        debug("RESPONSE STATUS:  " + statusLine);
      }

      int spacePos   = statusLine.indexOf(' ');
      if (spacePos < 0)
      {
        throw new HTTPException("Unable to parse response header -- could " +
                                "not find protocol/version delimiter");
      }

      String protocolVersion = statusLine.substring(0, spacePos);
      int    spacePos2       = statusLine.indexOf(' ', spacePos+1);
      if (spacePos2 < 0)
      {
        throw new HTTPException("Unable to parse response header -- could " +
                                "not find response code delimiter");
      }

      int statusCode;
      try
      {
        statusCode = Integer.parseInt(statusLine.substring(spacePos+1,
                                                           spacePos2));
      }
      catch (NumberFormatException nfe)
      {
        throw new HTTPException("Unable to parse response header -- could " +
                                "not interpret status code as an integer");
      }

      String responseMessage = statusLine.substring(spacePos2+1);
      response = new HTTPResponse(requestURL, statusCode, protocolVersion,
                                  responseMessage);

      while (tokenizer.hasMoreTokens())
      {
        String headerLine = tokenizer.nextToken();
        if (debugMode)
        {
          debug("RESPONSE HEADER:  " + headerLine);
        }

        int colonPos = headerLine.indexOf(':');
        if (colonPos < 0)
        {
          if (headerLine.toLowerCase().startsWith("http/"))
          {
            // This is a direct violation of RFC 2616, but certain HTTP servers
            // seem to immediately follow a 100 continue with a 200 ok without
            // the required CRLF in between.
            debug("Found illegal status line '" + headerLine +
                  "'in the middle of a response -- attempting to deal with " +
                  "it as the start of a new response.");
            statusLine = headerLine;
            spacePos   = statusLine.indexOf(' ');
            if (spacePos < 0)
            {
              throw new HTTPException("Unable to parse response header -- " +
                                      "could not find protocol/version " +
                                      "delimiter");
            }

            protocolVersion = statusLine.substring(0, spacePos);
            spacePos2       = statusLine.indexOf(' ', spacePos+1);
            if (spacePos2 < 0)
            {
              throw new HTTPException("Unable to parse response header -- " +
                                      "could not find response code delimiter");
            }

            try
            {
              statusCode = Integer.parseInt(statusLine.substring(spacePos+1,
                                                                 spacePos2));
            }
            catch (NumberFormatException nfe)
            {
              throw new HTTPException("Unable to parse response header -- " +
                                      "could not interpret status code as an " +
                                      "integer");
            }

            responseMessage = statusLine.substring(spacePos2+1);
            response = new HTTPResponse(requestURL, statusCode, protocolVersion,
                                        responseMessage);
            continue;
          }
          else
          {
            throw new HTTPException("Unable to parse response header -- no " +
                                    "colon found on header line \"" +
                                    headerLine + '"');
          }
        }

        String headerName  = headerLine.substring(0, colonPos);
        String headerValue = headerLine.substring(colonPos+1).trim();
        response.addHeader(headerName, headerValue);
      }
    }
    else
    {
      // This should never happen -- an empty response
      throw new HTTPException("Unable to parse response header -- empty " +
                              "header");
    }


    // If the status code was 100 (continue), then it was an intermediate header
    // and we need to keep reading until we get the real response header.
    while (response.getStatusCode() == 100)
    {
      if (dataStartPos < bytesRead)
      {
        byte[] newBuffer = new byte[bytesRead - dataStartPos];
        System.arraycopy(buffer, dataStartPos, newBuffer, 0, newBuffer.length);
        buffer = newBuffer;
        bytesRead = buffer.length;

        headerEndPos = -1;
        for (int i=0; i < (bytesRead-3); i++)
        {
          if ((buffer[i] == '\r') && (buffer[i+1] == '\n') &&
              (buffer[i+2] == '\r') && (buffer[i+3] == '\n'))
          {
            headerEndPos = i;
            dataStartPos = i+4;
            break;
          }
        }

        if (headerEndPos < 0)
        {
          for (int i=0; i < (bytesRead-1); i++)
          {
            if ((buffer[i] == '\n') && (buffer[i+1] == '\n'))
            {
              headerEndPos = i;
              dataStartPos = i+2;
              break;
            }
          }
        }
      }
      else
      {
        buffer       = new byte[0];
        bytesRead    = 0;
        headerEndPos = -1;
      }


      byte[] buffer2 = new byte[BUFFER_SIZE];
      while (headerEndPos < 0)
      {
        int startPos      = bytesRead;
        int moreBytesRead = inputStream.read(buffer2);

        if (moreBytesRead < 0)
        {
          throw new IOException("Unexpected end of input stream from server " +
                                "when reading more data from response");
        }

        byte[] newBuffer = new byte[bytesRead + moreBytesRead];
        System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
        System.arraycopy(buffer2, 0, newBuffer, bytesRead, moreBytesRead);
        buffer = newBuffer;
        bytesRead += moreBytesRead;

        for (int i=startPos; i < (bytesRead-3); i++)
        {
          if ((buffer[i] == '\r') && (buffer[i+1] == '\n') &&
              (buffer[i+2] == '\r') && (buffer[i+3] == '\n'))
          {
            headerEndPos = i;
            dataStartPos = i+4;
            break;
          }
        }

        if (headerEndPos < 0)
        {
          for (int i=startPos; i < (bytesRead-1); i++)
          {
            if ((buffer[i] == '\n') && (buffer[i+1] == '\n'))
            {
              headerEndPos = i;
              dataStartPos = i+2;
              break;
            }
          }
        }
      }


      // We should now have the next header, so examine it.
      headerStr = new String(buffer, 0, headerEndPos);
      tokenizer = new StringTokenizer(headerStr, "\r\n");
      if (tokenizer.hasMoreTokens())
      {
        String statusLine = tokenizer.nextToken();
        if (debugMode)
        {
          debug("RESPONSE STATUS:  " + statusLine);
        }

        int spacePos   = statusLine.indexOf(' ');
        if (spacePos < 0)
        {
          throw new HTTPException("Unable to parse response header -- could " +
                                  "not find protocol/version delimiter");
        }

        String protocolVersion = statusLine.substring(0, spacePos);
        int    spacePos2       = statusLine.indexOf(' ', spacePos+1);
        if (spacePos2 < 0)
        {
          throw new HTTPException("Unable to parse response header -- could " +
                                  "not find response code delimiter");
        }

        int statusCode;
        try
        {
          statusCode = Integer.parseInt(statusLine.substring(spacePos+1,
                                                             spacePos2));
        }
        catch (NumberFormatException nfe)
        {
          throw new HTTPException("Unable to parse response header -- could " +
                                  "not interpret status code as an integer");
        }

        String responseMessage = statusLine.substring(spacePos2+1);
        response = new HTTPResponse(requestURL, statusCode, protocolVersion,
                                    responseMessage);

        while (tokenizer.hasMoreTokens())
        {
          String headerLine = tokenizer.nextToken();
          if (debugMode)
          {
            debug("RESPONSE HEADER:  " + headerLine);
          }

          int colonPos = headerLine.indexOf(':');
          if (colonPos < 0)
          {
            throw new HTTPException("Unable to parse response header -- no " +
                                    "colon found on header line \"" +
                                    headerLine + '"');
          }

          String headerName  = headerLine.substring(0, colonPos);
          String headerValue = headerLine.substring(colonPos+1).trim();
          response.addHeader(headerName, headerValue);
        }
      }
      else
      {
        // This should never happen -- an empty response
        throw new HTTPException("Unable to parse response header -- empty " +
                                "header");
      }
    }


    // At this point, we're transitioning from the header to the content.  Stop
    // the header timer and start the content timer.
    if (keepStats)
    {
      headerTimer.stopTimer();
      contentTimer.startTimer();
    }


    // Now that we have parsed the header, use it to determine how much data
    // there is.  If we're lucky, the server will have told us using the
    // "Content-Length" header.
    int contentLength = response.getContentLength();
    if (contentLength >= 0)
    {
      readContentDataUsingLength(response, inputStream, contentLength, buffer,
                                 dataStartPos, bytesRead);
    }
    else
    {
      // We didn't get a content length.  See if the server wants to use a
      // chunked encoding.
      boolean useChunkedEncoding = false;
      String transferCoding = response.getHeader("transfer-encoding");
      if ((transferCoding != null) && (transferCoding.length() > 0))
      {
        if (transferCoding.equalsIgnoreCase("chunked"))
        {
          useChunkedEncoding = true;
        }
        else
        {
          throw new HTTPException("Unsupported transfer coding \"" +
                                  transferCoding + "\" used for response");
        }
      }

      if (useChunkedEncoding)
      {
        readContentDataUsingChunkedEncoding(response, inputStream, buffer,
                                            dataStartPos, bytesRead);
      }
      else
      {
        // It's not chunked encoding, so our last hope is that the connection
        // will be closed when all the data has been sent.
        String connectionStr = response.getHeader("connection");
        if ((connectionStr != null) &&
            (! connectionStr.equalsIgnoreCase("close")))
        {
          throw new HTTPException("Unable to determine how to find when the " +
                                  "end of the data has been reached (no " +
                                  "content length, not chunked encoding, " +
                                  "connection string is \"" + connectionStr +
                                  "\" rather than \"close\")");
        }
        else
        {
          readContentDataUsingConnectionClose(response, inputStream, buffer,
                                              dataStartPos, bytesRead);
        }
      }
    }


    // Read the cookies from the response and set them as appropriate.
    String[] cookieValues = response.getCookieValues();
    for (int i=0; i < cookieValues.length; i++)
    {
      if (debugMode)
      {
        debug("Parsing cookie response value " + cookieValues[i]);
      }

      try
      {
        addCookie(new HTTPCookie(requestURL, cookieValues[i]));
      }
      catch (HTTPException he)
      {
        // Ignore this cookie since we couldn't parse it.  Should we do anything
        // else with it?
        he.printStackTrace();
      }
    }


    // Finally, return the response to the caller.
    return response;
  }



  /**
   * Reads the actual data of the response based on the content length provided
   * by the server in the response header.
   *
   * @param  response       The response with which the data is associated.
   * @param  inputStream    The input stream from which to read the response.
   * @param  contentLength  The number of bytes that the server said are in the
   *                        response.
   * @param  dataRead       The data that we have already read.  This includes
   *                        the header data, but may also include some or all of
   *                        the content data as well.
   * @param  dataStartPos   The position in the provided array at which the
   *                        content data starts.
   * @param  dataBytesRead  The total number of valid bytes in the provided
   *                        array that should be considered part of the
   *                        response (the number of header bytes is included in
   *                        this count).
   *
   * @throws  IOException  If a problem occurs while reading data from the
   *                       server.
   */
  private void readContentDataUsingLength(HTTPResponse response,
                                          InputStream inputStream,
                                          int contentLength, byte[] dataRead,
                                          int dataStartPos, int dataBytesRead)
          throws IOException
  {
    if (contentLength <= 0)
    {
      response.setResponseData(new byte[0]);
      return;
    }


    byte[] contentBytes = new byte[contentLength];
    int    startPos     = 0;
    if (dataBytesRead > dataStartPos)
    {
      // We've already got some data to include in the header, so copy that into
      // the content array.  Make sure the server didn't do something stupid
      // like return more data than it told us was in the response.
      int bytesToCopy = Math.min(contentBytes.length,
                                 (dataBytesRead - dataStartPos));
      System.arraycopy(dataRead, dataStartPos, contentBytes, 0, bytesToCopy);
      startPos = bytesToCopy;
    }

    byte[] buffer = new byte[BUFFER_SIZE];
    while (startPos < contentBytes.length)
    {
      int bytesRead = inputStream.read(buffer);
      if (bytesRead < 0)
      {
        throw new IOException("Unexpected end of input stream reached when " +
                              "reading data from the server");
      }

      System.arraycopy(buffer, 0, contentBytes, startPos, bytesRead);
      startPos += bytesRead;
    }


    response.setResponseData(contentBytes);
  }



  /**
   * Reads the actual data of the response using chunked encoding, which is a
   * way for the server to provide the data in several chunks rather than all at
   * once.
   *
   * @param  response       The response with which the data is associated.
   * @param  inputStream    The input stream from which to read the response.
   * @param  dataRead       The data that we have already read.  This includes
   *                        the header data, but may also include some or all of
   *                        the content data as well.
   * @param  dataStartPos   The position in the provided array at which the
   *                        content data starts.
   * @param  dataBytesRead  The total number of valid bytes in the provided
   *                        array that should be considered part of the
   *                        response (the number of header bytes is included in
   *                        this count).
   *
   * @throws  IOException  If a problem occurs while reading data from the
   *                       server.
   *
   * @throws  HTTPException  If the data read cannot be properly interpreted
   *                         using chunked encoding.
   */
  private void readContentDataUsingChunkedEncoding(HTTPResponse response,
                                                   InputStream inputStream,
                                                   byte[] dataRead,
                                                   int dataStartPos,
                                                   int dataBytesRead)
          throws IOException, HTTPException
  {
    // Create an array list that we will use to hold the chunks of information
    // read from the server.
    ArrayList<byte[]> dataList = new ArrayList<byte[]>();


    // Create a variable to hold the total number of bytes in the data.
    int totalBytes = 0;


    // Create a variable that will be used in reading chunk size data.
    int[] bufferPosInfo = new int[] { dataStartPos, dataBytesRead };


    // Loop, reading all data from the server.
    while (true)
    {
      // First, read the chunk size.
      boolean eolFound  = false;
      boolean sizeRead  = false;
      int     chunkSize = 0;
      while (true)
      {
        int nextByte = nextByte(dataRead, bufferPosInfo, inputStream);
        if (nextByte < 0)
        {
          throw new IOException("Unexpected end of input stream when reading " +
                                "the chunk size");
        }
        if (((nextByte == '\r') || (nextByte == '\n')) && (! sizeRead))
        {
          continue;
        }
        else
        {
          sizeRead = true;
        }

        switch (nextByte)
        {
          case '0':
            chunkSize <<= 4;
            break;
          case '1':
            chunkSize = (chunkSize << 4) + 1;
            break;
          case '2':
            chunkSize = (chunkSize << 4) + 2;
            break;
          case '3':
            chunkSize = (chunkSize << 4) + 3;
            break;
          case '4':
            chunkSize = (chunkSize << 4) + 4;
            break;
          case '5':
            chunkSize = (chunkSize << 4) + 5;
            break;
          case '6':
            chunkSize = (chunkSize << 4) + 6;
            break;
          case '7':
            chunkSize = (chunkSize << 4) + 7;
            break;
          case '8':
            chunkSize = (chunkSize << 4) + 8;
            break;
          case '9':
            chunkSize = (chunkSize << 4) + 9;
            break;
          case 'a':
          case 'A':
            chunkSize = (chunkSize << 4) + 10;
            break;
          case 'b':
          case 'B':
            chunkSize = (chunkSize << 4) + 11;
            break;
          case 'c':
          case 'C':
            chunkSize = (chunkSize << 4) + 12;
            break;
          case 'd':
          case 'D':
            chunkSize = (chunkSize << 4) + 13;
            break;
          case 'e':
          case 'E':
            chunkSize = (chunkSize << 4) + 14;
            break;
          case 'f':
          case 'F':
            chunkSize = (chunkSize << 4) + 15;
            break;
          case '\r':
            nextByte = nextByte(dataRead, bufferPosInfo, inputStream);
            if (nextByte == '\n')
            {
              eolFound = true;
            }
            else if (nextByte < 0)
            {
              throw new IOException("Unexpected end of input stream reached " +
                                    "when reading data from server");
            }
            else
            {
              throw new HTTPException("Invalid character found in chunk " +
                                      "size specification:  '" +
                                      ((char) nextByte) + '\'');
            }
            break;
          case '\n':
            eolFound = true;
            break;
          case ' ':
            // This is technically in violation of the HTTP 1/1 specification,
            // but it appears that some HTTP servers sometimes include a space
            // between the end of the chunk size specification and the CRLF.
            // If we encounter any of these spaces, then just ignore them.
            break;
          default:
            if (nextByte < 0)
            {
              throw new IOException("Unexpected end of input stream reached " +
                                    "when reading data from server");
            }
            else
            {
              throw new HTTPException("Invalid character found in chunk size " +
                                      "specification:  '" + ((char) nextByte) +
                                      '\'');
            }
        }

        if (eolFound)
        {
          break;
        }
      }


      // Now we should know the chunk size.  If it is zero, then we don't need
      // to continue.
      if (chunkSize == 0)
      {
        break;
      }


      // Read the actual chunk data and store it in the array list.
      byte[] chunkData = new byte[chunkSize];
      readBytes(dataRead, bufferPosInfo, inputStream, chunkData);
      dataList.add(chunkData);
      totalBytes += chunkSize;
    }


    // Assemble the contents of all the buffers into a big array and store that
    // array in the response.
    int startPos = 0;
    byte[] contentData = new byte[totalBytes];
    for (int i=0; i < dataList.size(); i++)
    {
      byte[] chunkData = dataList.get(i);
      System.arraycopy(chunkData, 0, contentData, startPos, chunkData.length);
      startPos += chunkData.length;
    }
    response.setResponseData(contentData);
  }



  /**
   * Retrieves the next byte of data.  If data is available in the given byte
   * buffer, then that will be used.  Otherwise, it will be read from the
   * provided input stream.
   *
   * @param  buffer         The byte buffer from which to read the byte if
   *                        possible.
   * @param  bufferPosInfo  Information about the current position and end of
   *                        the data in the byte buffer.
   * @param  inputStream    The input stream from which to read the byte if
   *                        there is no more unread data in the buffer.
   *
   * @return  The next byte of data, or <CODE>-1</CODE> if the end of the input
   *          stream has been reached.
   *
   * @throws  IOException  If a problem occurs while trying to read data from
   *                       the input stream.
   */
  private int nextByte(byte[] buffer, int[] bufferPosInfo,
                       InputStream inputStream)
          throws IOException
  {
    int startPos = bufferPosInfo[0];
    int endPos   = bufferPosInfo[1];

    if (startPos >= endPos)
    {
      return inputStream.read();
    }
    else
    {
      bufferPosInfo[0] = startPos+1;
      return buffer[startPos];
    }
  }



  /**
   * Reads data from the provided byte buffer and/or input stream into the given
   * destination array.  Information will be taken from the buffer first until
   * it has been exhausted (or the destination array is full), and then the
   * remaining data for the destination array will be read from the provided
   * input stream.
   *
   * @param  buffer         The byte buffer from which to read the data if
   *                        possible.
   * @param  bufferPosInfo  Information about the current position and end of
   *                        the data in the byte buffer.
   * @param  inputStream    The input stream from which to read the data if
   *                        there is no more unread data in the buffer.
   * @param  destination    The array that should be used to hold the data read
   *                        from the server.
   *
   * @throws  IOException  If a problem occurs while trying to read data from
   *                       the input stream.
   */
  private void readBytes(byte[] buffer, int[] bufferPosInfo,
                         InputStream inputStream, byte[] destination)
          throws IOException
  {
    int destinationStartPos   = 0;
    int bufferBytesReamaining = bufferPosInfo[1] - bufferPosInfo[0];
    if (bufferBytesReamaining >= destination.length)
    {
      System.arraycopy(buffer, bufferPosInfo[0], destination, 0,
                       destination.length);
      bufferPosInfo[0] = bufferPosInfo[0] + destination.length;
      return;
    }
    else if (bufferBytesReamaining > 0)
    {
      System.arraycopy(buffer, bufferPosInfo[0], destination, 0,
                       bufferBytesReamaining);
      bufferPosInfo[0]    = bufferPosInfo[1];
      destinationStartPos = bufferBytesReamaining;
    }

    int destinationBytesRemaining = destination.length - destinationStartPos;
    while (destinationBytesRemaining > 0)
    {
      int bytesRead = inputStream.read(destination, destinationStartPos,
                                       destinationBytesRemaining);
      if (bytesRead < 0)
      {
        throw new IOException("Unexpected end of input stream while reading " +
                              "data from the server");
      }

      destinationStartPos       += bytesRead;
      destinationBytesRemaining -= bytesRead;
    }
  }



  /**
   * Reads the actual data of the response using chunked encoding, which is a
   * way for the server to provide the data in several chunks rather than all at
   * once.
   *
   * @param  response       The response with which the data is associated.
   * @param  inputStream    The input stream from which to read the response.
   * @param  dataRead       The data that we have already read.  This includes
   *                        the header data, but may also include some or all of
   *                        the content data as well.
   * @param  dataStartPos   The position in the provided array at which the
   *                        content data starts.
   * @param  dataBytesRead  The total number of valid bytes in the provided
   *                        array that should be considered part of the
   *                        response (the number of header bytes is included in
   *                        this count).
   *
   * @throws  IOException  If a problem occurs while reading data from the
   *                       server.
   */
  private void readContentDataUsingConnectionClose(HTTPResponse response,
                                                   InputStream inputStream,
                                                   byte[] dataRead,
                                                   int dataStartPos,
                                                   int dataBytesRead)
          throws IOException
  {
    // Create an array list that we will use to hold the chunks of information
    // read from the server.
    ArrayList<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();


    // Create a variable to hold the total number of bytes in the data.
    int totalBytes = 0;


    // See if we have unread data in the array already provided.
    int existingBytes = dataBytesRead - dataStartPos;
    if (existingBytes > 0)
    {
      ByteBuffer byteBuffer = ByteBuffer.allocate(existingBytes);
      byteBuffer.put(dataRead, dataStartPos, existingBytes);
      bufferList.add(byteBuffer);
      totalBytes += existingBytes;
    }


    // Keep reading until we hit the end of the input stream.
    byte[] buffer = new byte[BUFFER_SIZE];
    while (true)
    {
      try
      {
        int bytesRead = inputStream.read(buffer);
        if (bytesRead < 0)
        {
          // We've hit the end of the stream and therefore the end of the
          // document.
          break;
        }
        else if (bytesRead > 0)
        {
          ByteBuffer byteBuffer = ByteBuffer.allocate(bytesRead);
          byteBuffer.put(buffer, 0, bytesRead);
          bufferList.add(byteBuffer);
          totalBytes += bytesRead;
        }
      }
      catch (IOException ioe)
      {
        // In this case we'll assume that the end of the stream has been
        // reached.  It's possible that there was some other error, but we can't
        // do anything about it so try to process what we've got so far.
        break;
      }
    }


    // Assemble the contents of all the buffers into a big array and store that
    // array in the response.
    int startPos = 0;
    byte[] contentData = new byte[totalBytes];
    for (int i=0; i < bufferList.size(); i++)
    {
      ByteBuffer byteBuffer = bufferList.get(i);
      byteBuffer.flip();
      byteBuffer.get(contentData, startPos, byteBuffer.limit());
      startPos += byteBuffer.limit();
    }
    response.setResponseData(contentData);
  }



  /**
   * Closes all open connections that are associated with this HTTP client.  The
   * client will still be available for use.
   */
  public void closeAll()
  {
    Iterator sockets = socketHash.values().iterator();
    while (sockets.hasNext())
    {
      try
      {
        ((Socket) sockets.next()).close();
      }
      catch (Exception e) {}
    }

    socketHash.clear();
  }



  /**
   * Invalidates all SSL sessions associated with any connections held by this
   * HTTP client.  The existing SSL-based connections will still be valid, but
   * any new connections created will be required to complete the full SSL
   * negotiation process.
   */
  public void invalidateSSLSessions()
  {
    Iterator sockets = socketHash.values().iterator();
    while (sockets.hasNext())
    {
      Object o = sockets.next();
      if (o instanceof SSLSocket)
      {
        ((SSLSocket) o).getSession().invalidate();
      }
    }
  }



  /**
   * Retrieves a socket that may be used to communicate with an SSL-based server
   * through an HTTP proxy.  If an existing connection may be used, then it will
   * be.  Otherwise, a new connection will be established and a CONNECT will be
   * performed on that connection.
   *
   * @param  address  The address of the target system.
   * @param  port     The port of the target system.
   *
   * @return  A socket that may be used to communicate with the target system
   *          over SSL through a proxy.
   *
   * @throws  HTTPException  If a problem occurs while attempting to obtain a
   *                         connection through the proxy.
   */
  public Socket getSSLThroughProxySocket(String address, int port)
         throws HTTPException
  {
    String hashKey = "connect://" + address + ':' + port;
    Socket s = socketHash.remove(hashKey);
    if ((s != null) && s.isConnected())
    {
      return s;
    }

    try
    {
      if (clientAddress == null)
      {
        s = new Socket(proxyHost, proxyPort);
      }
      else
      {
        s = new Socket(proxyHost, proxyPort, clientAddress, 0);
      }

      BufferedReader r =
           new BufferedReader(new InputStreamReader(s.getInputStream()));
      BufferedWriter w =
           new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

      if (debugMode)
      {
        StringBuilder b = new StringBuilder();
        b.append("CLIENT REQUEST:  CONNECT ").append(address).append(':').
          append(port).append(" HTTP/1.1");
        debug(b.toString());
      }

      w.write("CONNECT ");
      w.write(address);
      w.write(":");
      w.write(String.valueOf(port));
      w.write(" HTTP/1.1\r\n");

      if (debugMode)
      {
        StringBuilder b = new StringBuilder();
        b.append("CLIENT REQUEST:  HOST: ").append(address);
        debug(b.toString());
      }

      w.write("HOST: ");
      w.write(address);
      w.write("\r\n");

      debug("CLIENT REQUEST:  CONNECTION: Keep-Alive");

      w.write("CONNECTION: Keep-Alive\r\n");

      if (proxyAuthenticationEnabled())
      {
        if (debugMode)
        {
          String authStr = proxyAuthID + ':' + proxyAuthPW;
          StringBuilder b = new StringBuilder();
          b.append("CLIENT REQUEST:  ").append(PROXY_AUTH_HEADER_PREFIX).
            append(Base64.encode(ASN1Element.getBytes(authStr)));
          debug(b.toString());
        }

        String authStr = proxyAuthID + ':' + proxyAuthPW;
        w.write(PROXY_AUTH_HEADER_PREFIX);
        w.write(Base64.encode(ASN1Element.getBytes(authStr)));
        w.write("\r\n");
      }

      debug("CLIENT REQUEST:  ");

      w.write("\r\n");
      w.flush();

      boolean firstLine = true;
      while (true)
      {
        String line = r.readLine();
        if (line == null)
        {
          throw new HTTPException("The connection to the proxy was " +
                                  "unexpectedly closed while waiting for " +
                                  "the CONNECT response.");
        }
        else if (line.length() == 0)
        {
          break;
        }
        else
        {
          debug("SERVER RESPONSE:  " + line);

          if (firstLine)
          {
            firstLine = false;
            StringTokenizer tokenizer = new StringTokenizer(line);
            tokenizer.nextToken(); // The protocol.

            int statusCode = Integer.parseInt(tokenizer.nextToken());
            if (statusCode != 200)
            {
              throw new HTTPException("Unsupported status " + statusCode +
                                      " in CONNECT response line " + line);
            }
          }
        }
      }


      // Perform the necessary SSL negotiation on the connection.
      if (sslSocketFactory == null)
      {
        sslSocketFactory =
             (SSLSocketFactory) SSLSocketFactory.getDefault();
      }

      s = sslSocketFactory.createSocket(s, address, port, true);
      s.setReuseAddress(true);
      s.setSoLinger(true, 0);
      s.setSoTimeout(socketTimeout);

      return s;
    }
    catch (HTTPException he)
    {
      throw he;
    }
    catch (Exception e)
    {
      throw new HTTPException("Unable to use CONNECT to tunnel through the " +
                              "proxy:  " + e, e);
    }
  }
}

