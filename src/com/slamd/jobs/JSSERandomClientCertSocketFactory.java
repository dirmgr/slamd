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



import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSocketFactory;



/**
 * This class provides an implementation of an SSL socket factory that will use
 * JSSE to create an SSL client socket.  The first time the server requests a
 * client certificate, one will be chosen at random from the appropriate set of
 * keys in the JSSE JKS-format key store.  Subsequent requests will continue to
 * use the same client certificate until the <CODE>chooseNewClientCert</CODE>
 * method is called.  In addition, this class implements a trust manager so that
 * any SSL certificate presented by the server will be trusted.
 *
 *
 * @author   Neil A. Wilson
 */
public class JSSERandomClientCertSocketFactory
       extends SSLSocketFactory
       implements LDAPSocketFactory, X509KeyManager, X509TrustManager
{
  // Indicates whether the client certificate to use should always be chosen at
  // random, or if it should only be chosen at random the first time a
  // certificate is needed and any time the chooseNewClientCert method is
  // called.
  private boolean alwaysRandom;

  // Indicates whether debug mode will be enabled (will print a message to
  // standard error whenever any method is called).
  private boolean debugMode;

  // Indicates whether to disable session caching for connections created with
  // this socket factory.
  private boolean disableSessionCaching;

  // The random number generator that will be used to select which client
  // certificate to present.
  private Random random;

  // The SSL context that will be used to manage all things SSL.
  private SSLContext sslContext;

  // The SSL socket factory that will actually be used to create the sockets.
  private SSLSocketFactory sslSocketFactory;

  // The alias of the certificate that should be presented to the server
  // whenever one is requested.
  private String currentAlias;

  // The set of aliases of the client certificates that are available for use.
  private String[] aliases;

  // The parent key manager that we will use for operations other than choosing
  // the alias of the client certificate to present.
  private X509KeyManager parentKeyManager;



  /**
   * Creates a new instance of this SSL socket factory.
   *
   * @param  keyStoreFile      The path to the JKS-format JSSE keystore
   *                           containing the client certificates to use in the
   *                           authentication process.
   * @param  keyStorePassword  The password needed to access the information in
   *                           the keystore, formatted as a character array.
   *
   * @throws  LDAPException  If a problem occurs while initializing this socket
   *                         factory.
   */
  public JSSERandomClientCertSocketFactory(String keyStoreFile,
                                           char[] keyStorePassword)
         throws LDAPException
  {
    this(keyStoreFile, keyStorePassword, false);
  }



  /**
   * Creates a new instance of this SSL socket factory.
   *
   * @param  keyStoreFile      The path to the JKS-format JSSE keystore
   *                           containing the client certificates to use in the
   *                           authentication process.
   * @param  keyStorePassword  The password needed to access the information in
   *                           the keystore, formatted as a character array.
   * @param  debugMode         Indicates whether this socket factory will
   *                           operate in debug mode.
   *
   * @throws  LDAPException  If a problem occurs while initializing this socket
   *                         factory.
   */
  public JSSERandomClientCertSocketFactory(String keyStoreFile,
                                           char[] keyStorePassword,
                                           boolean debugMode)
         throws LDAPException
  {
    this.debugMode        = debugMode;
    alwaysRandom          = false;
    disableSessionCaching = false;


    // Read in the contents of the JSSE keystore.
    KeyStore keyStore;
    try
    {
      FileInputStream inputStream = new FileInputStream(keyStoreFile);
      keyStore = KeyStore.getInstance("JKS");
      keyStore.load(inputStream, keyStorePassword);
      inputStream.close();
    }
    catch (Exception e)
    {
      String message = "Unable to read key store file \"" + keyStoreFile +
                       "\" -- " + e;

      if (debugMode)
      {
        System.err.println(message);
      }

      throw new LDAPException(message);
    }


    try
    {
      // Iterate through the key store to find all the aliases of the client
      // certificates.
      ArrayList<String> aliasList = new ArrayList<String>();
      Enumeration<String> keyStoreAliases = keyStore.aliases();

      while (keyStoreAliases.hasMoreElements())
      {
        String alias = keyStoreAliases.nextElement();
        if (keyStore.isKeyEntry(alias))
        {
          aliasList.add(keyStoreAliases.nextElement());
        }
      }

      aliases = new String[aliasList.size()];
      aliasList.toArray(aliases);
    }
    catch (KeyStoreException kse)
    {
      String message = "Unable to retrieve aliases of client certificates " +
                       "from the key store -- " + kse;

      if (debugMode)
      {
        System.err.println(message);
      }

      throw new LDAPException(message);
    }


    // Make sure that there is at least one client certificate available.
    if ((aliases == null) || (aliases.length == 0))
    {
      String message = "No client certificates found in key store \"" +
                       keyStoreFile + '"';

      if (debugMode)
      {
        System.err.println(message);
      }

      throw new LDAPException(message);
    }


    // Get the default X.509 key manager.
    try
    {
      KeyManagerFactory keyManagerFactory =
           KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, keyStorePassword);
      KeyManager[] managers = keyManagerFactory.getKeyManagers();
      if ((managers == null) || (managers.length == 0))
      {
        throw new NoSuchAlgorithmException("No X.509 key managers are " +
                                           "available.");
      }

      parentKeyManager = (X509KeyManager) managers[0];
    }
    catch (Exception e)
    {
      String message = "Unable to obtain a handle to the default X.509 key " +
                       "manager -- " + e;

      if (debugMode)
      {
        System.err.println(message);
      }

      throw new LDAPException(message);
    }



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
      String message = "Unable to initialize the SSL context -- " + nsae;

      if (debugMode)
      {
        System.err.println(message);
      }

      throw new LDAPException(message);
    }


    // Initialize the SSL context with our own trust manager (this class) to
    // use when determining whether to trust a client certificate.
    try
    {
      sslContext.init(new KeyManager[] { this }, new TrustManager[] { this },
                      null);
    }
    catch (KeyManagementException kme)
    {
      String message = "Unable to register a key and trust managers with the " +
                       "SSL context:  " + kme;

      if (debugMode)
      {
        System.err.println(message);
      }

      throw new LDAPException(message);
    }


    // Get the socket factory to use when creating the certificates.
    sslSocketFactory = sslContext.getSocketFactory();


    // Initialize the random number generator that we will use for selecting
    // client certificates.
    random = new Random();


    // If we are in debug mode, indicate that the socket factory has been
    // created.
    if (debugMode)
    {
      System.err.println("New JSSERandomClientCertSocketFactory created");
    }
  }



  /**
   * Retrieves the alias of the client certificate that will be used the next
   * time the client must present a certificate to an SSL server.
   *
   * @return  The alias of the client certificate that will be used the next
   *          time the client must present a certificate to an SSL server, or
   *          <CODE>null</CODE> if the next certificate will be chosen at
   *          random.
   */
  public String getCurrentAlias()
  {
    return currentAlias;
  }



  /**
   * Specifies the alias of the client certificate that should be used the next
   * time the client must present a certificate to an SSL server.  This
   * selection will remain in effect until the <CODE>chooseNewClientCert</CODE>
   * method is called (in which case the next certificate will be chosen at
   * random) or the <CODE>setCurrentAlias</CODE> method is called again to
   * choose a different alias.  Note that no error checking is performed, so if
   * the specified alias does not exist in the keystore, then attempts to use
   * that certificate will fail.  Also note that this will override the setting
   * of the <CODE>alwaysRandom</CODE> flag, so this certificate will always be
   * used until the <CODE>chooseNewClientCert</CODE> method is called, at which
   * point the <CODE>alwaysRandom</CODE> flag will again be honored.
   *
   * @param  alias  The alias of the client certificate that should be used the
   *                next time the client must present a certificate to an SSL
   *                server.  A value of <CODE>null</CODE> indicates that the
   *                next selection should be random.
   */
  public void setCurrentAlias(String alias)
  {
    this.currentAlias = alias;
  }



  /**
   * Retrieves the aliases of the client certificates that are available for use
   * in the key store.
   *
   * @return  The aliases of the client certificates that are available for use
   *          in the key store.
   */
  public String[] getAliases()
  {
    return aliases;
  }



  /**
   * Indicates that this socket factory should choose a new client certificate
   * at random the next time it must present a certificate to an SSL server.
   */
  public void chooseNewClientCert()
  {
    currentAlias = null;
  }



  /**
   * Indicates whether the client certificate selection will be always taken at
   * random, or if the selection should only be random the first time a
   * certificate is needed or after the <CODE>chooseNewClientCert</CODE> method
   * is called.
   *
   * @return  <CODE>true</CODE> if the client certificate selection will always
   *          be random, or <CODE>false</CODE> if not.
   */
  public boolean alwaysRandom()
  {
    return alwaysRandom;
  }



  /**
   * Specifies whether the client certificate selection should always be random,
   * or if the selection should only be random the first time a certificate is
   * needed or after the <CODE>chooseNewClientCert</CODE> method is called.
   *
   * @param  alwaysRandom  Specifies whether the client certificate selection
   *                       should always be random.
   */
  public void setAlwaysRandom(boolean alwaysRandom)
  {
    this.alwaysRandom = alwaysRandom;
  }



  /**
   * Indicates whether session caching has been disabled for SSL sockets created
   * using this socket factory.
   *
   * @return  <CODE>true</CODE> if session caching has been disabled, or
   *          <CODE>false</CODE> if not.
   */
  public boolean disableSessionCaching()
  {
    return disableSessionCaching;
  }



  /**
   * Specifies whether session caching should be disabled for SSL sockets
   * created using this socket factory.
   *
   * @param  disableSessionCaching  Indicates whether session caching should be
   *                                disabled for SSL sockets created using this
   *                                socket factory.
   */
  public void setDisableSessionCaching(boolean disableSessionCaching)
  {
    this.disableSessionCaching = disableSessionCaching;
  }



  /**
   * Chooses the alias of the client certificate that should be presented to the
   * server.
   *
   * @param  keyTypes  The key type algorithm name(s) to use in making the
   *                   selection.
   * @param  issuers   The set of accepted issuers to use in making the
   *                   selection.
   * @param  socket    The socket to use in making the selection.
   *
   * @return  The alias of the client certificate that should be presented to
   *          the server.
   */
  public String chooseClientAlias(String[] keyTypes, Principal[] issuers,
                                  Socket socket)
  {
    if (currentAlias != null)
    {
      return currentAlias;
    }

    String alias = aliases[(random.nextInt() & 0x7FFFFFFF) % aliases.length];
    if (! alwaysRandom)
    {
      currentAlias = alias;
    }

    return alias;
  }



  /**
   * Retrieves the aliases of the certificates available for use by clients, in
   * accordance with the provided criteria.
   *
   * @param  keyType  The key type algorithm name of certificates to include in
   *                  the set of aliases returned.
   * @param  issuers  The set of accepted issuers of certificates to include in
   *                  the set of aliases returned.
   *
   * @return  The aliases of the certificates available for use by clients, in
   *          accordance with the provided criteria.
   */
  public String[] getClientAliases(String keyType, Principal[] issuers)
  {
    return parentKeyManager.getClientAliases(keyType, issuers);
  }



  /**
   * Chooses the alias of the server certificate that should be presented to
   * clients.
   *
   * @param  keyType  The key type algorithm name to use in making the
   *                  selection.
   * @param  issuers  The set of accepted issuers to use in making the
   *                  selection.
   * @param  socket   The socket to use in making the selection.
   *
   * @return  The alias of the server certificate that should be presented to
   *          clients.
   */
  public String chooseServerAlias(String keyType, Principal[] issuers,
                                  Socket socket)
  {
    return parentKeyManager.chooseServerAlias(keyType, issuers, socket);
  }



  /**
   * Retrieves the aliases of the certificates available for use by an SSL
   * server, in accordance with the provided criteria.
   *
   * @param  keyType  The key type algorithm name of certificates to include in
   *                  the set of aliases returned.
   * @param  issuers  The set of accepted issuers of certificates to include in
   *                  the set of aliases returned.
   *
   * @return  The aliases of the certificates available for use by an SSL
   *          server, in accordance with the provided criteria.
   */
  public String[] getServerAliases(String keyType, Principal[] issuers)
  {
    return parentKeyManager.getServerAliases(keyType, issuers);
  }



  /**
   * Retrieves the private key for the certificate with the specified alias.
   *
   * @param  alias  The alias of the certificate for which to retrieve the
   *                private key.
   *
   * @return  The private key of the requested certificate, or <CODE>null</CODE>
   *          if the specified certificate cannot be found.
   */
  public PrivateKey getPrivateKey(String alias)
  {
    return parentKeyManager.getPrivateKey(alias);
  }



  /**
   * Retrieves the certificate chain for the certificate with the given alias.
   * The chain will be returned in order, with the specified certificate first
   * and the root issuer last.
   *
   * @param  alias  The alias of the certificate for which to retrieve the
   *                certificate chain.
   *
   * @return  The certificate chain for the certificate with the given alias, or
   *          <CODE>null</CODE> if the specified certificate cannot be found.
   */
  public X509Certificate[] getCertificateChain(String alias)
  {
    return parentKeyManager.getCertificateChain(alias);
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

    return null;
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
      SSLSocket sslSocket =
           (SSLSocket) sslSocketFactory.createSocket(host, port);
      if (disableSessionCaching)
      {
        sslSocket.getSession().invalidate();
      }

      return sslSocket;
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
  @Override()
  public Socket createSocket(String host, int port)
         throws IOException
  {
    if (debugMode)
    {
      System.err.println("createSocket(" + host + ',' + port + ") invoked");
    }

    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
    if (disableSessionCaching)
    {
      sslSocket.getSession().invalidate();
    }

    return sslSocket;
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
  @Override()
  public Socket createSocket(String host, int port, InetAddress localHost,
                             int localPort)
         throws IOException
  {
    if (debugMode)
    {
      System.err.println("createSocket(" + host + ',' + port + ", " +
                         localHost.getHostAddress() + ", " + localPort +
                         ") invoked");
    }

    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port,
                                                                    localHost,
                                                                    localPort);
    if (disableSessionCaching)
    {
      sslSocket.getSession().invalidate();
    }

    return sslSocket;
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
  @Override()
  public Socket createSocket(InetAddress host, int port)
         throws IOException
  {
    if (debugMode)
    {
      System.err.println("createSocket(" + host.getHostAddress() + ", " + port +
                         ") invoked");
    }

    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
    if (disableSessionCaching)
    {
      sslSocket.getSession().invalidate();
    }

    return sslSocket;
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
  @Override()
  public Socket createSocket(InetAddress host, int port,
                             InetAddress localAddress, int localPort)
         throws IOException
  {
    if (debugMode)
    {
      System.err.println("createSocket(" + host.getHostAddress() + ',' + port +
                         ", " + localAddress.getHostAddress() + ", " +
                         localPort + ") invoked");
    }

    SSLSocket sslSocket =
         (SSLSocket) sslSocketFactory.createSocket(host, port, localAddress,
                                                   localPort);
    if (disableSessionCaching)
    {
      sslSocket.getSession().invalidate();
    }

    return sslSocket;
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
  @Override()
  public Socket createSocket(Socket socket, String host, int port,
                             boolean autoClose)
         throws IOException
  {
    if (debugMode)
    {
      System.err.println("createSocket(Socket, " + host + ", " + port + ", " +
                         autoClose + ") invoked");
    }

    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket,
                                                                    host, port,
                                                                    autoClose);
    if (disableSessionCaching)
    {
      sslSocket.getSession().invalidate();
    }

    return sslSocket;
  }



  /**
   * Retrieves the set of cipher suites that are enabled by default.
   *
   * @return  The set of cipher suites that are enabled by default.
   */
  @Override()
  public String[] getDefaultCipherSuites()
  {
    if (debugMode)
    {
      System.err.println("getDefaultCipherSuites() invoked");
    }

    return sslSocketFactory.getDefaultCipherSuites();
  }



  /**
   * Retrieves the set of cipher suites that can be used to create SSL sockets.
   *
   * @return  The set of cipher suites that can be used to create SSL sockets.
   */
  @Override()
  public String[] getSupportedCipherSuites()
  {
    if (debugMode)
    {
      System.err.println("getSupportedCipherSuites() invoked");
    }

    return sslSocketFactory.getSupportedCipherSuites();
  }
}

