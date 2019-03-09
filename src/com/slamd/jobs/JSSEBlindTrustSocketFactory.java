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



import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSocketFactory;



/**
 * This class provides an implementation of an SSL socket factory that will use
 * JSSE to create the SSL socket.  In addition, it will implement a trust
 * mechanism in such a way that it will blindly trust any certificate that the
 * server presents to it, regardless of what we might think is wrong with it.
 *
 *
 * @author   Neil A. Wilson
 */
public class JSSEBlindTrustSocketFactory
       extends SSLSocketFactory
       implements LDAPSocketFactory, X509TrustManager
{
  // Indicates whether debug mode will be enabled (will print a message to
  // standard error whenever any method is called).
  private boolean debugMode;

  // Indicates whether the SSL session should be immediately invalidated to
  // prevent session reuse.
  private boolean disableSessionReuse;

  // The SSL context that will be used to manage all things SSL.
  private SSLContext sslContext;

  // The SSL socket factory that will actually be used to create the sockets.
  private SSLSocketFactory sslSocketFactory;

  // The set cipher names that should be used when creating sockets.
  private String[] cipherNames;



  /**
   * Creates a new instance of this LDAP socket factory.
   *
   * @throws  LDAPException  If a problem occurs while initializing this socket
   *                         factory.
   */
  public JSSEBlindTrustSocketFactory()
         throws LDAPException
  {
    this(false);
  }



  /**
   * Creates a new instance of this LDAP socket factory, optionally operating in
   * debug mode.
   *
   * @param  debugMode  Indicates whether to operate in debug mode.  If this is
   *                    enabled, a message will be printed to standard error
   *                    any time of of the methods of this class is called.
   *
   * @throws  LDAPException  If a problem occurs while initializing this socket
   *                         factory.
   */
  public JSSEBlindTrustSocketFactory(boolean debugMode)
         throws LDAPException
  {
    this.debugMode = debugMode;


    // Indicate that we will be using JSSE for the SSL-based connections.
    Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    System.setProperty("java.protocol.handler.pkgs",
                       "com.sun.net.ssl.internal.www.protocol");


    // Get the default SSL context.
    try
    {
      sslContext = SSLContext.getInstance("SSLv3");
    }
    catch (NoSuchAlgorithmException nsae)
    {
      throw new LDAPException("Unable to initialize the SSL context:  " + nsae);
    }


    // Initialize the SSL context with our own trust manager (this class) to
    // use when determining whether to trust a client certificate.
    try
    {
      sslContext.init(null, new TrustManager[] { this }, null);
    }
    catch (KeyManagementException kme)
    {
      throw new LDAPException("Unable to register a new trust manager with " +
                              "the SSL context:  " + kme);
    }


    // Get the socket factory to use when creating the certificates.
    sslSocketFactory = sslContext.getSocketFactory();


    // Set the values of the remaining instance variables.
    disableSessionReuse = false;
    cipherNames = sslSocketFactory.getDefaultCipherSuites();


    // If we are in debug mode, indicate that the socket factory has been
    // created.
    if (debugMode)
    {
      System.err.println("New JSSEBlindTrustSocketFactory created");
    }
  }



  /**
   * Determines whether the provided client certificate should be trusted.  In
   * this case, the certificate will always be trusted.
   *
   * @param  chain     The peer certificate chain.
   * @param  authType  The authentication type based on the client certificate.
   */
  public void checkClientTrusted(X509Certificate[] chain, String authType)
  {
    // No implementation required.  If we don't throw an exception, then there
    // is no problem with the cert.
    if (debugMode)
    {
      System.err.println("checkClientTrusted() invoked");
    }
  }



  /**
   * Determines whether the provided server certificate should be trusted.  In
   * this case, the certificate will always be trusted.
   *
   * @param  chain     The peer certificate chain.
   * @param  authType  The authentication type based on the server certificate.
   */
  public void checkServerTrusted(X509Certificate[] chain, String authType)
  {
    // No implementation required.  If we don't throw an exception, then there
    // is no problem with the cert.
    if (debugMode)
    {
      System.err.println("checkServerTrusted() invoked");
    }
  }



  /**
   * Retrieves an array of CA certificates that are trusted for authenticating
   * peers.
   *
   * @return  An empty array, because we don't care about any list of CAs.
   */
  public X509Certificate[] getAcceptedIssuers()
  {
    if (debugMode)
    {
      System.err.println("getAcceptedIssuers() invoked");
    }

    return new X509Certificate[0];
  }



  /**
   * Establishes an SSL socket to the provided host and port that can be used by
   * the LDAP SDK for Java for communicating with an LDAP directory server.
   *
   * @param  host  The address of the server to which the connection is to be
   *               established.
   * @param  port  The port number of the server to which the connection is to
   *               be established.
   *
   * @return  The SSL socket that may be used for communicating with the
   *          directory server.
   *
   * @throws  LDAPException  If a problem occurs while trying to establish the
   *                         connection.
   */
  public Socket makeSocket(String host, int port)
         throws LDAPException
  {
    if (debugMode)
    {
      System.err.println("makeSocket(" + host + ',' + port + ") invoked");
    }

    try
    {
      SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(host, port);
      s.setEnabledCipherSuites(cipherNames);
      s.setKeepAlive(true);
      s.setSoLinger(true, 0);
      s.setReuseAddress(true);
      s.setTcpNoDelay(true);

      if (disableSessionReuse)
      {
        s.getSession().invalidate();
      }

      return s;
    }
    catch (Exception e)
    {
      throw new LDAPException("Unable to establish the SSL connection:  " + e);
    }
  }



  /**
   * Creates a new SSL socket connected to the specified host and port.
   *
   * @param  host  The address of the system to which the SSL socket should be
   *               connected.
   * @param  port  The port on the target system to which the SSL socket should
   *               be connected.
   *
   * @return  The created SSL socket.
   *
   * @throws  IOException  If a problem occurs while creating the SSL socket.
   */
  public Socket createSocket(String host, int port)
         throws IOException
  {
    SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(host, port);
    s.setEnabledCipherSuites(cipherNames);
    s.setKeepAlive(true);
    s.setSoLinger(true, 0);
    s.setReuseAddress(true);
    s.setTcpNoDelay(true);

    if (disableSessionReuse)
    {
      s.getSession().invalidate();
    }

    return s;
  }



  /**
   * Creates a new SSL socket connected to the specified host and port.
   *
   * @param  host       The address of the system to which the SSL socket should
   *                    be connected.
   * @param  port       The port on the target system to which the SSL socket
   *                    should be connected.
   * @param  localHost  The address on the local system from which the socket
   *                    should originate.
   * @param  localPort  The port on the local system from which the socket
   *                    should originate.
   *
   * @return  The created SSL socket.
   *
   * @throws  IOException  If a problem occurs while creating the SSL socket.
   */
  public Socket createSocket(String host, int port, InetAddress localHost,
                             int localPort)
         throws IOException
  {
    SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(host, port,
                                                            localHost,
                                                            localPort);
    s.setEnabledCipherSuites(cipherNames);
    s.setKeepAlive(true);
    s.setSoLinger(true, 0);
    s.setReuseAddress(true);
    s.setTcpNoDelay(true);

    if (disableSessionReuse)
    {
      s.getSession().invalidate();
    }

    return s;
  }



  /**
   * Creates a new SSL socket connected to the specified host and port.
   *
   * @param  host  The address of the system to which the SSL socket should be
   *               connected.
   * @param  port  The port on the target system to which the SSL socket should
   *               be connected.
   *
   * @return  The created SSL socket.
   *
   * @throws  IOException  If a problem occurs while creating the SSL socket.
   */
  public Socket createSocket(InetAddress host, int port)
         throws IOException
  {
    SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(host, port);
    s.setEnabledCipherSuites(cipherNames);
    s.setKeepAlive(true);
    s.setSoLinger(true, 0);
    s.setReuseAddress(true);
    s.setTcpNoDelay(true);

    if (disableSessionReuse)
    {
      s.getSession().invalidate();
    }

    return s;
  }



  /**
   * Creates a new SSL socket connected to the specified host and port.
   *
   * @param  host          The address of the system to which the SSL socket
   *                       should be connected.
   * @param  port          The port on the target system to which the SSL socket
   *                       should be connected.
   * @param  localAddress  The address on the local system from which the socket
   *                       should originate.
   * @param  localPort     The port on the local system from which the socket
   *                       should originate.
   *
   * @return  The created SSL socket.
   *
   * @throws  IOException  If a problem occurs while creating the SSL socket.
   */
  public Socket createSocket(InetAddress host, int port,
                             InetAddress localAddress, int localPort)
         throws IOException
  {
    SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(host, port,
                                                            localAddress,
                                                            localPort);
    s.setEnabledCipherSuites(cipherNames);
    s.setKeepAlive(true);
    s.setSoLinger(true, 0);
    s.setReuseAddress(true);
    s.setTcpNoDelay(true);

    if (disableSessionReuse)
    {
      s.getSession().invalidate();
    }

    return s;
  }



  /**
   * Converts the provided socket to an SSL socket using this socket factory.
   *
   * @param  socket     The socket to convert to an SSL socket.
   * @param  host       The host to which the socket is connected.
   * @param  port       The port to which the socket is connected.
   * @param  autoClose  Indicates whether the underlying socket should be closed
   *                    when the returned SSL socket is closed.
   *
   * @return  The created SSL socket.
   *
   * @throws  IOException  If a problem occurs while creating the SSL socket.
   */
  public Socket createSocket(Socket socket, String host, int port,
                             boolean autoClose)
         throws IOException
  {
    SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(socket, host,
                                                            port, autoClose);
    s.setEnabledCipherSuites(cipherNames);
    s.setKeepAlive(true);
    s.setSoLinger(true, 0);
    s.setReuseAddress(true);
    s.setTcpNoDelay(true);

    if (disableSessionReuse)
    {
      s.getSession().invalidate();
    }

    return s;
  }



  /**
   * Retrieves the names of the ciphers that should be used for SSL sockets
   * created by this socket factory.
   *
   * @return  The names of the ciphers that should be used for SSL sockets
   *          created by this socket factory.
   */
  public String[] getCiphers()
  {
    return cipherNames;
  }



  /**
   * Specifies the name of the cipher that should be used for SSL sockets
   * created by this socket factory.
   *
   * @param  cipherName  The name of the cipher that should be used for SSL
   *                     sockets created by this socket factory.
   */
  public void setCipher(String cipherName)
  {
    if (cipherName == null)
    {
      cipherNames = sslSocketFactory.getDefaultCipherSuites();
    }
    else
    {
      cipherNames = new String[] { cipherName };
    }
  }



  /**
   * Specifies the names of the cipher that should be used for SSL sockets
   * created by this socket factory.
   *
   * @param  cipherNames  The names of the cipher that should be used for SSL
   *                      sockets created by this socket factory.
   */
  public void setCiphers(String[] cipherNames)
  {
    if (cipherNames == null)
    {
      this.cipherNames = sslSocketFactory.getDefaultCipherSuites();
    }
    else
    {
      this.cipherNames = cipherNames;
    }
  }



  /**
   * Retrieves the set of cipher suites that are enabled by default.
   *
   * @return  The set of cipher suites that are enabled by default.
   */
  public String[] getDefaultCipherSuites()
  {
    return cipherNames;
  }



  /**
   * Retrieves the set of cipher suites that can be used to create SSL sockets.
   *
   * @return  The set of cipher suites that can be used to create SSL sockets.
   */
  public String[] getSupportedCipherSuites()
  {
    return cipherNames;
  }



  /**
   * Indicates whether SSL sessions may be reused across multiple connections.
   *
   * @return  <CODE>true</CODE> if SSL sessions may be reused across multiple
   *          connections, or <CODE>false</CODE> if not.
   */
  public boolean getDisableSessionReuse()
  {
    return disableSessionReuse;
  }



  /**
   * Specifies whether to disable SSL session reuse across multiple connections.
   *
   * @param  disableSessionReuse  Indicates whether to disable SSL session
   *                              reuse across multiple connections.
   */
  public void setDisableSessionReuse(boolean disableSessionReuse)
  {
    this.disableSessionReuse = disableSessionReuse;
  }
}

