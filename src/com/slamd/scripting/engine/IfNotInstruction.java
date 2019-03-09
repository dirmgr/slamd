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
 * This class defines an instruction that will be executed if the associated
 * condition is false.  It may also define an instruction that will be executed
 * if the associated condition is not false.
 *
 *
 * @author   Neil A. Wilson
 */
public class IfNotInstruction
       extends Instruction
{
  // The condition that determines whether the primary instruction is executed.
  private Argument condition;

  // The primary instruction that will be executed if the condition is false.
  private Instruction ifNotInstruction;

  // The optional instruction that will be executed if the condition is true.
  private Instruction elseInstruction;



  /**
   * Creates a new ifnot instruction with no else clause.
   *
   * @param  lineNumber        The line number on which the ifnot statement
   *                           occurs in the script.
   * @param  condition         The condition that will be evaluated to determine
   *                           whether the primary instruction is executed.
   * @param  ifNotInstruction  The primary instruction to execute if the
   *                           condition is false.
   */
  public IfNotInstruction(int lineNumber, Argument condition,
                          Instruction ifNotInstruction)
  {
    this (lineNumber, condition, ifNotInstruction, null);
  }



  /**
   * Creates a new ifnot instruction with an optional else clause.
   *
   * @param  lineNumber        The line number on which the ifnot statement
   *                           occurs in the script.
   * @param  condition         The condition that will be evaluated to determine
   *                           whether the primary instruction is executed.
   * @param  ifNotInstruction  The primary instruction to execute if the
   *                           condition is false.
   * @param  elseInstruction   The optional instruction to execute if the
   *                           condition is false.
   */
  public IfNotInstruction(int lineNumber, Argument condition,
                          Instruction ifNotInstruction,
                          Instruction elseInstruction)
  {
    super(lineNumber);

    this.condition        = condition;
    this.ifNotInstruction = ifNotInstruction;
    this.elseInstruction  = elseInstruction;
  }



  /**
   * Retrieves the condition that will be evaluated at runtime to determine
   * whether the primary instruction is executed.
   *
   * @return  The condition that will be evaluated.
   */
  public Argument getCondition()
  {
    return condition;
  }



  /**
   * Retrieves the instruction that will be executed if the condition evaluates
   * to true.
   *
   * @return  The instruction that will be executed if the condition evaluates
   *          to true, or <CODE>null</CODE> if no such instruction has been
   *          defined.
   */
  public Instruction getInstructionIfTrue()
  {
    return elseInstruction;
  }



  /**
   * Retrieves the instruction that will be executed if the condition evaluates
   * to false.
   *
   * @return  The instruction that will be executed if the condition evaluates
   *          to false.
   */
  public Instruction getInstructionIfFalse()
  {
    return ifNotInstruction;
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
    // Determine whether the condition is true or false and execute the
    // appropriate instruction.
    BooleanVariable v = (BooleanVariable) condition.getArgumentValue();
    if (! v.getBooleanValue())
    {
      ifNotInstruction.execute(jobThread);
    }
    else if (elseInstruction != null)
    {
      elseInstruction.execute(jobThread);
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
    BooleanVariable v = (BooleanVariable) condition.getArgumentValue();
    boolean conditionIsTrue = v.getBooleanValue();

    jobThread.writeVerbose(lineNumber + ":  ifnot " +
                           condition.getArgumentAsString() + " (" +
                           conditionIsTrue + ')');
    if (! conditionIsTrue)
    {
      ifNotInstruction.debugExecute(jobThread);
    }
    else
    {
      if (elseInstruction != null)
      {
        jobThread.writeVerbose(lineNumber + ":  else");
        elseInstruction.debugExecute(jobThread);
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
    buffer.append("ifnot ");
    buffer.append(condition.getArgumentAsString());
    buffer.append(Constants.EOL);

    buffer.append(ifNotInstruction.toString(indent+Constants.SCRIPT_INDENT));

    if (elseInstruction != null)
    {
      buffer.append(indentBuffer);
      buffer.append("else");
      buffer.append(Constants.EOL);
      buffer.append(elseInstruction.toString(indent+Constants.SCRIPT_INDENT));
    }

    return buffer.toString();
  }
}

