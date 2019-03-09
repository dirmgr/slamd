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



/**
 * This class defines an exception that may be thrown if a problem occurs while
 * attempting to communicate with a server over HTTP.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTTPException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 9145863581747657628L;



  /**
   * Creates a new exception that may be thrown if a problem occurs while trying
   * to communicate with a server over HTTP.
   *
   * @param  message  A message providing information about the exception.
   */
  public HTTPException(String message)
  {
    super(message);
  }



  /**
   * Creates a new exception that may be thrown if a problem occurs while trying
   * to communicate with a server over HTTP.
   *
   * @param  message  A message providing information about the exception.
   * @param  cause    The cause that triggered this HTTP exception.
   */
  public HTTPException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

