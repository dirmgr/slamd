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
package com.slamd.server;



import com.slamd.common.SLAMDException;



/**
 * This class defines an exception that may be thrown by the SLAMD server if a
 * problem occurs.  It may be a parent class for more specific kinds of
 * exceptions, or it may be thrown itself as an exception.
 *
 *
 * @author   Neil A. Wilson
 */
public class SLAMDServerException
       extends SLAMDException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -1374193206372472961L;



  /**
   * Creates a new exception that may be thrown if a problem occurs in the SLAMD
   * server.
   *
   * @param  message  A message that provides information about the nature of
   *                  the exception.
   */
  public SLAMDServerException(String message)
  {
    super(message);
  }



  /**
   * Creates a new exception that may be thrown if a problem occurs in the SLAMD
   * server.
   *
   * @param  message  A message that provides information about the nature of
   *                  the exception.
   * @param  cause    The parent exception that triggered this SLAMD server
   *                  exception.
   */
  public SLAMDServerException(String message, Throwable cause)
  {
    super(message, cause);
  }
}

