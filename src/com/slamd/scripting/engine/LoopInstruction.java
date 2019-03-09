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
import com.slamd.scripting.general.IntegerVariable;



/**
 * This class defines an instruction that will be executed a specified number
 * of times.
 *
 *
 * @author   Neil A. Wilson
 */
public class LoopInstruction
       extends Instruction
{
  // The number of times to execute the instruction.
  private Argument iterations;

  // The instruction to be executed.
  private Instruction instruction;



  /**
   * Creates a new loop instruction to perform repeated executions of a
   * specified instruction.
   *
   * @param  lineNumber   The line number in the script on which this loop
   *                      instruction starts.
   * @param  iterations   The number of times to execute the instruction.
   * @param  instruction  The instruction to execute the specified number of
   *                      times.
   */
  public LoopInstruction(int lineNumber, Argument iterations,
                         Instruction instruction)
  {
    super(lineNumber);

    this.iterations  = iterations;
    this.instruction = instruction;
  }



  /**
   * Retrieves the argument that will be evaluated at runtime to determine the
   * number of iterations to perform.
   *
   * @return  The argument that will be evaluated at runtime to determine the
   *          number of iterations to perform.
   */
  public Argument getIterationsArgument()
  {
    return iterations;
  }



  /**
   * Retrieves the instruction that will be executed by this loop.
   *
   * @return  The instruction that will be executed by this loop.
   */
  public Instruction getInstruction()
  {
    return instruction;
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
    // Determine the number of iterations to perform.
    IntegerVariable v = (IntegerVariable) iterations.getArgumentValue();
    int numIterations = v.getIntValue();


    // Execute instruction the appropriate number of times.
    for (int i=0; i < numIterations; i++)
    {
      try
      {
        instruction.execute(jobThread);
      }
      catch (BreakException be)
      {
        break;
      }
      catch (ContinueException ce)
      {
        continue;
      }
    }
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
    // Determine the number of iterations to perform.
    IntegerVariable v = (IntegerVariable) iterations.getArgumentValue();
    int numIterations = v.getIntValue();

    jobThread.writeVerbose(lineNumber + ":  loop " +
                           iterations.getArgumentAsString() + " (" +
                           numIterations + ')');

    // Execute instruction the appropriate number of times.
    for (int i=0; i < numIterations; i++)
    {
      jobThread.writeVerbose(lineNumber + ":  loop iteration " + i);

      try
      {
        instruction.debugExecute(jobThread);
      }
      catch (BreakException be)
      {
        break;
      }
      catch (ContinueException ce)
      {
        continue;
      }
    }
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
    StringBuilder indentBuffer = new StringBuilder();
    for (int i=0; i < indent; i++)
    {
      indentBuffer.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuffer);
    buffer.append("loop ");
    buffer.append(iterations.getArgumentAsString());
    buffer.append(Constants.EOL);
    buffer.append(instruction.toString(indent+Constants.SCRIPT_INDENT));

    return buffer.toString();
  }
}

