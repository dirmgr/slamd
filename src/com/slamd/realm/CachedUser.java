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
package com.slamd.realm;



import java.security.Principal;



/**
 * This class implements a data structure used to store information about users
 * that have been authenticated using the LDAPRealm realm implementation.  The
 * information stored includes an expiration time that can be used to prevent
 * the information in the cached entry from becoming stale.
 *
 *
 * @author  Neil A. Wilson
 */
public class CachedUser
{
  // The password for this user, stored as a SHA-1 hash.
  private byte[] hashedPassword;

  // The time at which the information about this user expires and should no
  // longer be trusted.
  private long expirationTime;

  // The Principal object associated with the user.
  private Principal userPrincipal;

  // The DN of the user's entry in the directory server.
  private String userDN;

  // The username that the user provided when authenticating.
  private String userName;



  /**
   * Creates a new cached user with the specified information.
   *
   * @param  userName        The username provided when authenticating.
   * @param  userDN          The DN of the user's entry in the directory server.
   * @param  hashedPassword  The password for the user as a SHA-1 hash.
   * @param  userPrincipal   The Principal object associated with this user.
   * @param  expirationTime  The time at which this cached user information
   *                         should expire.
   */
  public CachedUser(String userName, String userDN, byte[] hashedPassword,
                    Principal userPrincipal, long expirationTime)
  {
    this.userName       = userName;
    this.userDN         = userDN;
    this.hashedPassword = hashedPassword;
    this.userPrincipal  = userPrincipal;
    this.expirationTime = expirationTime;
  }



  /**
   * Retrieves the user name provided by the user when authenticating.
   *
   * @return  The user name provided by the user when authenticating.
   */
  public String getUserName()
  {
    return userName;
  }



  /**
   * Retrieves the DN of the user's entry in the directory.
   *
   * @return  The DN of the user's entry in the directory.
   */
  public String getUserDN()
  {
    return userDN;
  }



  /**
   * Retrieves the SHA-1 hashed password for the user.
   *
   * @return  The SHA-1 hashed password for the user.
   */
  public byte[] getHashedPassword()
  {
    return hashedPassword;
  }



  /**
   * Retrieves the Principal object associated with the user.
   *
   * @return  The Principal object associated with the user.
   */
  public Principal getUserPrincipal()
  {
    return userPrincipal;
  }



  /**
   * Retrieves the time at which this cached information should be considered
   * expired.
   *
   * @return  The time at which this cached information should be considered
   *          expired.
   */
  public long getExpirationTime()
  {
    return expirationTime;
  }
}

