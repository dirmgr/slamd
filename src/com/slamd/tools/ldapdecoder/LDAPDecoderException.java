/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.tools.ldapdecoder;



/**
 * This class defines an exception that may be thrown if a problem is
 * encountered in the LDAP decoder.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPDecoderException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -8040991223368523554L;



  /**
   * Creates a new protocol exception with the provided message.
   *
   * @param  message  The message that explains the reason for this exception.
   */
  public LDAPDecoderException(String message)
  {
    super(message);
  }



  /**
   * Creates a new protocol exception with the provided message.
   *
   * @param  message  The message that explains the reason for this exception.
   * @param  cause    The underlying cause that triggered this exception.
   */
  public LDAPDecoderException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

