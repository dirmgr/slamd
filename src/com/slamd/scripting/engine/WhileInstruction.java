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
import com.slamd.scripting.general.BooleanVariable;



/**
 * This class defines an instruction that will be executed as long as the
 * specified condition is true.
 *
 *
 * @author   Neil A. Wilson
 */
public class WhileInstruction
       extends Instruction
{
  // The condition to evaluate in order to determine whether to execute the
  // instruction another time.
  private Argument condition;

  // The instruction to be executed.
  private Instruction instruction;



  /**
   * Creates a new while instruction.
   *
   * @param  lineNumber   The line number of the script on which this while
   *                      instruction begins.
   * @param  condition    The condition that will be evaluated to determine
   *                      whether the instruction will be executed another time.
   * @param  instruction  The instruction to execute as long as the condition
   *                      evaluates to true.
   */
  public WhileInstruction(int lineNumber, Argument condition,
                          Instruction instruction)
  {
    super(lineNumber);

    this.condition   = condition;
    this.instruction = instruction;
  }



  /**
   * Retrieves the condition for this while instruction.
   *
   * @return  The condition for this while instruction.
   */
  public Argument getCondition()
  {
    return condition;
  }



  /**
   * Retrieves the instruction that will be executed.
   *
   * @return  The instruction that will be executed.
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
    // Evaluate the condition to determine if it should be executed at all.
    BooleanVariable v = (BooleanVariable) condition.getArgumentValue();
    boolean executeAgain = v.getBooleanValue();

    while (executeAgain)
    {
      // Execute the instruction.
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
        // No action required, but we don't want the exception to propagate up.
      }

      // Re-evaluate the condition.
      v = (BooleanVariable) condition.getArgumentValue();
      executeAgain = v.getBooleanValue();
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
    // Evaluate the condition to determine if it should be executed at all.
    BooleanVariable v = (BooleanVariable) condition.getArgumentValue();
    boolean executeAgain = v.getBooleanValue();

    jobThread.writeVerbose(lineNumber + ":  while " +
                           condition.getArgumentAsString() + " (" +
                           executeAgain + ')');

    while (executeAgain)
    {
      // Execute the instruction.
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
        // No action required, but we don't want the exception to propagate up.
      }

      // Re-evaluate the condition.
      v = (BooleanVariable) condition.getArgumentValue();
      executeAgain = v.getBooleanValue();
      jobThread.writeVerbose(lineNumber + ":  while " +
                             condition.getArgumentAsString() + " (" +
                             executeAgain + ')');
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
    buffer.append("while ");
    buffer.append(condition.getArgumentAsString());
    buffer.append(Constants.EOL);
    buffer.append(instruction.toString(indent+Constants.SCRIPT_INDENT));

    return buffer.toString();
  }
}

