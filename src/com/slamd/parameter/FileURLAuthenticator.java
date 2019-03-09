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
package com.slamd.parameter;



import java.net.Authenticator;
import java.net.PasswordAuthentication;



/**
 * This class defines a very simple authenticator that will be used by the
 * FileURL parameter type in order to access resources that require
 * authentication.
 *
 *
 * @author   Neil A. Wilson
 */
public class FileURLAuthenticator
       extends Authenticator
{
  // The user ID to use in the authentication process.
  private final String authID;

  // The password to use in the authentication process.
  private final char[] authPassword;



  /**
   * Creates a new file URL authenticator that will use the provided ID and
   * password.
   *
   * @param  authID        The user ID to use in the authentication process.
   * @param  authPassword  The password to use in the authentication process.
   */
  public FileURLAuthenticator(String authID, String authPassword)
  {
    this.authID       = authID;
    this.authPassword = authPassword.toCharArray();
  }



  /**
   * Retrieves the information needed to authenticate to a protected resource.
   *
   * @return  The information needed to authenticate to a protected resource.
   */
  @Override()
  protected PasswordAuthentication getPasswordAuthentication()
  {
    return new PasswordAuthentication(authID, authPassword);
  }
}

