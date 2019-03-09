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
package com.slamd.tools.ldapdecoder.snoop;



/**
 * This class defines an exception that may be thrown if a problem occurs while
 * attempting to decode snoop data.
 *
 *
 * @author   Neil A. Wilson
 */
public class SnoopException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 2900995602448794773L;



  /**
   * Creates a new snoop exception with the provided message.
   *
   * @param  message  The message explaining the reason for this exception.
   */
  public SnoopException(String message)
  {
    super(message);
  }



  /**
   * Creates a new snoop exception with the provided message.
   *
   * @param  message  The message explaining the reason for this exception.
   * @param  cause    The underlying cause that triggered this snoop exception.
   */
  public SnoopException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

