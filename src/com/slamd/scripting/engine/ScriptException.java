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
 * This class defines an exception that may be thrown if a problem occurs while
 * parsing or executing an LDAP script.
 *
 *
 * @author   Neil A. Wilson
 */
public class ScriptException
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -2815106049290481156L;



  /**
   * Creates a new script exception with the specified message.
   *
   * @param  message  The message that specifies the problem that occurred.
   */
  public ScriptException(String message)
  {
    super(message);
  }



  /**
   * Creates a new script exception with the specified message.
   *
   * @param  message  The message that specifies the problem that occurred.
   * @param  cause    The parent exception that triggered this script exception.
   */
  public ScriptException(String message, Throwable cause)
  {
    super(message, cause);
  }



  /**
   * Creates a new script exception with the specified line number and message.
   *
   * @param  lineNumber  The line number of the script on which the problem
   *                     occurred.
   * @param  message     The message that specifies the problem that occurred.
   */
  public ScriptException(int lineNumber, String message)
  {
    // The script parser always uses zero-based numbering,  but we should use
    // one-based numbering when reporting errors to the end user to avoid
    // frustration.  So just add one to the line number provided.
    this("Script error on line " + (lineNumber+1) + ":  " + message);
  }



  /**
   * Creates a new script exception with the specified line number and message.
   *
   * @param  lineNumber  The line number of the script on which the problem
   *                     occurred.
   * @param  message     The message that specifies the problem that occurred.
   * @param  cause       The parent exception that triggered this script
   *                     exception.
   */
  public ScriptException(int lineNumber, String message,
                         Throwable cause)
  {
    // The script parser always uses zero-based numbering,  but we should use
    // one-based numbering when reporting errors to the end user to avoid
    // frustration.  So just add one to the line number provided.
    this("Script error on line " + (lineNumber+1) + ":  " + message, cause);
  }



  /**
   * Creates a new script exception with the specified line number and message.
   *
   * @param  lineNumber       The line number of the script on which the problem
   *                          occurred.
   * @param  characterNumber  The character number of the line in which the
   *                          problem occurred.
   * @param  message          The message that specifies the problem that
   *                          occurred.
   */
  public ScriptException(int lineNumber, int characterNumber, String message)
  {
    // The script parser always uses zero-based numbering,  but we should use
    // one-based numbering when reporting errors to the end user to avoid
    // frustration.  So just add one to the line and character numbers provided.
    this("Script error at or near character " + (characterNumber+1) +
         " on line " + (lineNumber+1) + ":  " + message);
  }



  /**
   * Creates a new script exception with the specified line number and message.
   *
   * @param  lineNumber       The line number of the script on which the problem
   *                          occurred.
   * @param  characterNumber  The character number of the line in which the
   *                          problem occurred.
   * @param  message          The message that specifies the problem that
   *                          occurred.
   * @param  cause            The parent exception that triggered this script
   *                          exception.
   */
  public ScriptException(int lineNumber, int characterNumber, String message,
                         Throwable cause)
  {
    // The script parser always uses zero-based numbering,  but we should use
    // one-based numbering when reporting errors to the end user to avoid
    // frustration.  So just add one to the line and character numbers provided.
    this("Script error at or near character " + (characterNumber+1) +
         " on line " + (lineNumber+1) + ":  " + message, cause);
  }
}

