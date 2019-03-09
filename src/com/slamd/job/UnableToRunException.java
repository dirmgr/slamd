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
package com.slamd.job;



import com.slamd.common.SLAMDException;



/**
 * This class defines an exception that may be thrown if an attempt is made to
 * start a job that is unable to run for some reason.
 *
 *
 * @author   Neil A. Wilson
 */
public class UnableToRunException
       extends SLAMDException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -6449659728326282093L;



  /**
   * Creates a new exception that may be thrown if an attempt is made to start
   * a job that is unable to run.
   *
   * @param  message  A message providing information about the exception.
   */
  public UnableToRunException(String message)
  {
    super(message);
  }



  /**
   * Creates a new exception that may be thrown if an attempt is made to start
   * a job that is unable to run.
   *
   * @param  message  A message providing information about the exception.
   * @param  cause    The parent exception that triggered this unable to run
   *                  exception.
   */
  public UnableToRunException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

