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



import java.net.InetSocketAddress;
import java.net.Socket;

import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSocketFactory;



/**
 * This class provides a simple LDAP socket factory that creates plain sockets
 * that are well-suited for LDAP performance and benchmarking.  In particular,
 * it sets the TCP_NODELAY option to ensure that requests packets are sent as
 * quickly as possible, and it uses the SO_LINGER and SO_REUSEADDR options to be
 * well-suited for lots of short-lived connections.
 *
 *
 * @author   Neil A. Wilson
 */
public class SLAMDLDAPSocketFactory
       implements LDAPSocketFactory
{
  // Indicates whether to use the SO_KEEPALIVE socket option.
  private boolean setKeepAlive;

  // Indicates whether to use the SO_LINGER socket option.
  private boolean setLinger;

  // Indicates whether to use the SO_REUSEADDR socket option.
  private boolean setReuseAddress;

  // Indicates whether to use the TCP_NODELAY socket option.
  private boolean setTCPNoDelay;

  // The maximum length of time in milliseconds to wait for the connection to be
  // established.
  private int connectTimeout;




  /**
   * Creates a new instance of this LDAP socket factory with the default
   * settings.  The keepalive, linger, reuse address, and TCP nodelay socket
   * options will all be enabled.
   */
  public SLAMDLDAPSocketFactory()
  {
    setKeepAlive    = true;
    setLinger       = true;
    setReuseAddress = true;
    setTCPNoDelay   = true;
    connectTimeout  = 10000;
  }



  /**
   * Creates a new instance of this LDAP socket factory with the provided
   * settings.
   *
   * @param  setKeepAlive     Inkdicates whether to use the SO_KEEPALIVE socket
   *                          option.
   * @param  setLinger        Indicates whether to use the SO_LINGER socket
   *                          option.
   * @param  setReuseAddress  Indicates whether to use the SO_REUSEADDR socket
   *                          option.
   * @param  setTCPNoDelay    Indicates whether to use the TCP_NODELAY socket
   *                          option.
   */
  public SLAMDLDAPSocketFactory(boolean setKeepAlive, boolean setLinger,
                                boolean setReuseAddress, boolean setTCPNoDelay)
  {
    this.setKeepAlive    = setKeepAlive;
    this.setLinger       = setLinger;
    this.setReuseAddress = setReuseAddress;
    this.setTCPNoDelay   = setTCPNoDelay;
    this.connectTimeout  = 10000;
  }



  /**
   * Indicates whether the SO_KEEPALIVE socket option should be used for new
   * connections.
   *
   * @return  <CODE>true</CODE> if the SO_KEEPALIVE socket option should be
   *          used, or <CODE>false</CODE> if not.
   */
  public boolean getKeepAlive()
  {
    return setKeepAlive;
  }



  /**
   * Specifies whether to use the SO_KEEPALIVE socket option.
   *
   * @param  setKeepAlive  Specifies whether to use the SO_KEEPALIVE socket
   *                       option.
   */
  public void setKeepAlive(boolean setKeepAlive)
  {
    this.setKeepAlive = setKeepAlive;
  }



  /**
   * Indicates whether the SO_LINGER socket option should be used for new
   * connections.
   *
   * @return  <CODE>true</CODE> if the SO_LINGER socket option should be used,
   *          or <CODE>false</CODE> if not.
   */
  public boolean getLinger()
  {
    return setLinger;
  }



  /**
   * Specifies whether to use the SO_LINGER socket option.
   *
   * @param  setLinger  Specifies whether to use the SO_LINGER socket option.
   */
  public void setLinger(boolean setLinger)
  {
    this.setLinger = setLinger;
  }



  /**
   * Indicates whether the SO_REUSEADDR socket option should be used for new
   * connections.
   *
   * @return  <CODE>true</CODE> if the SO_REUSEADDR socket option should be
   *          used, or <CODE>false</CODE> if not.
   */
  public boolean getReuseAddress()
  {
    return setReuseAddress;
  }



  /**
   * Specifies whether to use the SO_REUSEADDR socket option.
   *
   * @param  setReuseAddress  Specifies whether to use the SO_REUSEADDR socket
   *                          option.
   */
  public void setReuseAddress(boolean setReuseAddress)
  {
    this.setReuseAddress = setReuseAddress;
  }



  /**
   * Indicates whether the TCP_NODELAY socket option should be used for new
   * connections.
   *
   * @return  <CODE>true</CODE> if the TCP_NODELAY socket option should be used,
   *          or <CODE>false</CODE> if not.
   */
  public boolean getTCPNoDelay()
  {
    return setTCPNoDelay;
  }



  /**
   * Specifies whether to use the TCP_NODELAY socket option.
   *
   * @param  setTCPNoDelay  Specifies whether to use the TCP_NODELAY socket
   *                        option.
   */
  public void setTCPNoDelay(boolean setTCPNoDelay)
  {
    this.setTCPNoDelay = setTCPNoDelay;
  }



  /**
   * Retrieves the maximum length of time in milliseconds to wait while
   * attempting to connect before giving up.
   *
   * @return  The maximum length of time in milliseconds to wait while
   *          attempting to connect before giving up.
   */
  public int getConnectTimeout()
  {
    return connectTimeout;
  }



  /**
   * Specifies the maximum length of time in milliseconds to wait while
   * attempting to connect before giving up.
   *
   * @param  connectTimeout  The maximum length of time in milliseconds to wait
   *                         while attempting to connect before giving up.
   */
  public void setConnectTimeout(int connectTimeout)
  {
    this.connectTimeout = connectTimeout;
  }



  /**
   * Establishes a socket to the provided host and port with the specified
   * settings that can be used by the LDAP SDK for Java.
   *
   * @param  host  The address of the server to which the connection is to be
   *               established.
   * @param  port  The port number of the server to which the connection is to
   *               be established.
   *
   * @return  The socket that may be used for communicating with the directory
   *          server.
   *
   * @throws  LDAPException  If a problem occurs while trying to establish the
   *                         connection.
   */
  public Socket makeSocket(String host, int port)
         throws LDAPException
  {
    try
    {
      Socket s = new Socket();

      s.connect(new InetSocketAddress(host, port), connectTimeout);
      s.setKeepAlive(setKeepAlive);
      s.setSoLinger(setLinger, 0);
      s.setReuseAddress(setReuseAddress);
      s.setTcpNoDelay(setTCPNoDelay);

      return s;
    }
    catch (Exception e)
    {
      throw new LDAPException("Unable to establish the connection:  " + e);
    }
  }
}

