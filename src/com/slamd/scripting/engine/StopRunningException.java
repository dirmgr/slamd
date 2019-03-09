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
package com.slamd.scripting.engine;



/**
 * This class defines an exception that will be thrown if the script should
 * stop running.  It will be propagated all the way up to the parser and
 * will therefore cause the script to stop running immediately.
 *
 *
 * @author   Neil A. Wilson
 */
public class StopRunningException
       extends ScriptException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -3633705886072323031L;



  /**
   * Creates a new stop running exception with the specified message.
   *
   * @param  message  The message that explains the cause of the exception.
   */
  public StopRunningException(String message)
  {
    super(message);
  }



  /**
   * Creates a new stop running exception with the specified message.
   *
   * @param  message  The message that explains the cause of the exception.
   * @param  cause    The parent exception that triggered this stop running
   *                  exception.
   */
  public StopRunningException(String message, Throwable cause)
  {
    super(message, cause);
  }



  /**
   * Creates a new stop running exception.
   *
   * @param  lineNumber  The line number of the script that generated the
   *                     exception.
   */
  public StopRunningException(int lineNumber)
  {
    this("The script exited at line " + lineNumber);
  }



  /**
   * Creates a new stop running exception.
   *
   * @param  lineNumber  The line number of the script that generated the
   *                     exception.
   * @param  cause       The parent exception that triggered this stop
   *                     running exception.
   */
  public StopRunningException(int lineNumber, Throwable cause)
  {
    this("The script exited at line " + lineNumber, cause);
  }



  /**
   * Creates a new stop running exception with the specified message.
   *
   * @param  lineNumber  The line number of the script that generated the
   *                     exception.
   * @param  message     The message that explains the cause of the exception.
   */
  public StopRunningException(int lineNumber, String message)
  {
    this("The script exited at line " + lineNumber + " because \"" + message +
         '"');
  }



  /**
   * Creates a new stop running exception with the specified message.
   *
   * @param  lineNumber  The line number of the script that generated the
   *                     exception.
   * @param  message     The message that explains the cause of the exception.
   * @param  cause       The parent exception that triggered this stop running
   *                     exception.
   */
  public StopRunningException(int lineNumber, String message,
                              Throwable cause)
  {
    this("The script exited at line " + lineNumber + " because \"" + message +
        '"', cause);
  }
}

