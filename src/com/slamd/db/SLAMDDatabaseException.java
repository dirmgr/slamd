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
package com.slamd.db;



import com.sleepycat.je.DatabaseException;



/**
 * This class provides a concrete {@code DatabaseException} implementation that
 * can be thrown by SLAMD.
 */
public final class SLAMDDatabaseException
       extends DatabaseException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 8341063799311240545L;



  /**
   * Creates a new SLAMD database exception with the provided message.
   *
   * @param  message  The message to use for the exception.  It must not be
   *                  {@code null}.
   */
  public SLAMDDatabaseException(final String message)
  {
    super(message);
  }



  /**
   * Creates a new SLAMD database exception with the provided message and cause.
   *
   * @param  message  The message to use for the exception.  It must not be
   *                  {@code null}.
   * @param  cause    The underlying exception that triggered this exception.
   *                  It may be {@code null} if no cause is available.
   */
  public SLAMDDatabaseException(final String message, final Throwable cause)
  {
    super(message, cause);
  }



  /**
   * Creates a new SLAMD database exception with the provided cause.
   *
   * @param  cause  The underlying exception that triggered this exception. It
   *                may be {@code null} if no cause is available.
   */
  public SLAMDDatabaseException(final Throwable cause)
  {
    super(cause);
  }
}
