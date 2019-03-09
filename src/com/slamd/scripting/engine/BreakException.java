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
 * break instruction is encountered inside a loop.  The break exception
 * indicates that the execution of the loop should terminate and that executions
 * should resume at the first instruction immediately after the end of the loop.
 * It is necessary to implement this as an exception because a loop may have
 * multiple depths of instructions and when the break is called it will be
 * necessary to break out of all of them.  To deal with the performance penalty
 * that can be associated with exceptions (particularly, filling in the stack
 * trace which is not even important in this case), a static singleton instance
 * will be created that should be thrown instead of creating a new break
 * exception every time.
 *
 *
 * @author   Neil A. Wilson
 */
public class BreakException
       extends ScriptException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 5115364798486611131L;



  /**
   * The singleton instance of this exception that should be thrown whenever a
   * break is necessary.
   */
  public static final BreakException BREAK = new BreakException();



  /**
   * Creates a new break exception.  The message and stack trace are irrelevant
   * since this exception is not used to indicate an error condition.
   */
  private BreakException()
  {
    super("break");
  }
}

