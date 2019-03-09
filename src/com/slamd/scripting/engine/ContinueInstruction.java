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



import com.slamd.common.Constants;
import com.slamd.job.JobClass;



/**
 * This class defines an instruction that will cause the scripting engine to
 * immediately stop executing the current iteration of a loop and begin
 * executing the next iteration, if appropriate.
 *
 *
 * @author   Neil A. Wilson
 */
public class ContinueInstruction
       extends Instruction
{
  /**
   * Creates a new continue instruction.
   *
   * @param  lineNumber  The line number on which the if statement occurs in the
   *                     script.
   */
  public ContinueInstruction(int lineNumber)
  {
    super(lineNumber);
  }



  /**
   * Performs the function associated with this instruction.
   *
   * @param  jobThread  The job thread that will be executing the script.
   *
   * @throws  ScriptException  If a problem occurs while processing this
   *                           instruction.
   */
  @Override()
  public void execute(JobClass jobThread)
         throws ScriptException
  {
    throw ContinueException.CONTINUE;
  }



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
  @Override()
  public void debugExecute(JobClass jobThread)
         throws ScriptException
  {
    jobThread.writeVerbose(lineNumber + ":  continue");
    throw ContinueException.CONTINUE;
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
  @Override()
  public String toString(int indent)
  {
    StringBuilder buffer = new StringBuilder();
    for (int i=0; i < indent; i++)
    {
      buffer.append(' ');
    }

    buffer.append("continue;");
    buffer.append(Constants.EOL);

    return buffer.toString();
  }
}

