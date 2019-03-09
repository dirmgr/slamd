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



import com.slamd.job.JobClass;



/**
 * This class defines a generic instruction that can be executed as part of an
 * LDAP script.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class Instruction
{
  // The line number in the script on which this instruction begins.
  protected int lineNumber;



  /**
   * Creates a new instruction.
   *
   * @param  lineNumber  The line number of the script on which the instruction
   *                     starts.
   */
  public Instruction(int lineNumber)
  {
    this.lineNumber = lineNumber;
  }



  /**
   * Retrieves the line number on which this instruction starts in the script.
   *
   * @return  The line number on which this instruction starts in the script.
   */
  public int getLineNumber()
  {
    return lineNumber;
  }



  /**
   * Performs the function associated with this instruction.
   *
   * @param  jobThread  The job thread that will be executing the script.
   *
   * @throws  ScriptException  If a problem occurs while processing this
   *                           instruction.
   */
  public abstract void execute(JobClass jobThread)
         throws ScriptException;



  /**
   * Performs the function associated with this instruction, writing debug
   * information about its execution to the client's message writer using the
   * <CODE>writeVerbose</CODE> method.
   *
   * @param  jobThread  The job thread that will be executing the script.
   *
   * @throws  ScriptException  If a problem occurs while processing this
   *                           instruction.
   */
  public abstract void debugExecute(JobClass jobThread)
         throws ScriptException;



  /**
   * Retrieves a string representation of this instruction as could be found in
   * a script file.  It may consist of multiple lines if necessary.
   *
   * @return  A string representation of this instruction as could be found in a
   *          script file.
   */
  @Override()
  public String toString()
  {
    return toString(0);
  }



  /**
   * Retrieves a string representation of this instruction as could be found in
   * a script file.  It may consist of multiple lines if necessary.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this instruction as could be found in a
   *          script file.
   */
  public abstract String toString(int indent);
}

