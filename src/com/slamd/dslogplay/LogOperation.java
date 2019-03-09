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
package com.slamd.dslogplay;



/**
 * This class defines a generic type of log operation that may be read from an
 * access log file and replayed against a Directory Server.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class LogOperation
{
  /**
   * The default result code string that should be used if the associated
   * operation was successful.
   */
  public static final String DEFAULT_RESULT_CODE = "0 (Success)";



  /**
   * Replays this operation against the directory server using the information
   * contained in the provided job thread.
   *
   * @param  jobThread  The job thread to use when replaying this operation.
   */
  public abstract void replayOperation(LogPlaybackJobClass jobThread);



  /**
   * Retrieves a string representation of this log operation.
   *
   * @return  A string representation of this log operation.
   */
  public abstract String toString();
}

