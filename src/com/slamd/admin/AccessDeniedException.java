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
package com.slamd.admin;



/**
 * This class defines an exception that may be thrown if a problem occurs while
 * evaluating access control information for the SLAMD administrative interface.
 *
 *
 * @author   Neil A. Wilson
 */
public class AccessDeniedException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 2350875899152461739L;



  /**
   * Creates a new access denied exception with the specified message.
   *
   * @param  message  The message that indicates the cause of the exception.
   */
  public AccessDeniedException(String message)
  {
    super(message);
  }



  /**
   * Creates a new access denied exception with the specified message and parent
   * exception.
   *
   * @param  message  The message associated with this exception.
   * @param  cause    The parent exception that triggered this access denied
   *                  exception.
   */
  public AccessDeniedException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

