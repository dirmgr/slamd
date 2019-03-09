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
package com.slamd.common;



/**
 * This class defines the parent class for all exceptions that are defined in
 * the SLAMD environment.
 *
 *
 * @author   Neil A. Wilson
 */
public class SLAMDException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 8042254946234351437L;



  /**
   * Creates a new exception that may be thrown if a problem occurs in the SLAMD
   * environment.
   *
   * @param  message  A message that provides information about the nature of
   *                  the exception.
   */
  public SLAMDException(String message)
  {
    super(message);
  }



  /**
   * Creates a new exception that may be thrown if a problem occurs in the SLAMD
   * environment.
   *
   * @param  message  A message that provides information about the nature of
   *                  the exception.
   * @param  cause    The parent exception that triggered this SLAMD exception.
   */
  public SLAMDException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

