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
package com.slamd.tools.tcpreplay;



/**
 * This class defines an exception that may be thrown if a problem occurs while
 * attempting to decode capture data.
 *
 *
 * @author   Neil A. Wilson
 */
public class CaptureException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -4841538324710032680L;



  /**
   * Creates a new capture exception with the provided message.
   *
   * @param  message  The message that provides additional information about
   *                  this exception.
   */
  public CaptureException(String message)
  {
    super(message);
  }



  /**
   * Creates a new capture exception with the provided message.
   *
   * @param  message  The message that provides additional information about
   *                  this exception.
   * @param  cause    The underlying exception that was caught and triggered
   *                  this exception.
   */
  public CaptureException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

