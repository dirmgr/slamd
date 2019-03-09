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
 * This class defines a special kind of script exception that may be thrown if a
 * continue instruction is encountered inside a loop.  The continue exception
 * indicates that the execution of the current loop iteration should terminate
 * and that the next iteration should start if appropriate.  To deal with the
 * performance penalty that can be associated with exceptions (particularly,
 * filling in the stack trace which is not even important in this case), a
 * static singleton instance will be created that should be thrown instead of
 * creating a new continue exception every time.
 *
 *
 * @author   Neil A. Wilson
 */
public class ContinueException
       extends ScriptException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 4395559919661280062L;



  /**
   * The singleton instance of this exception that should be thrown whenever a
   * continue is necessary.
   */
  public static final ContinueException CONTINUE = new ContinueException();



  /**
   * Creates a new continue exception.  The message and stack trace are
   * irrelevant since this exception is not used to indicate an error condition.
   */
  private ContinueException()
  {
    super("continue");
  }
}

