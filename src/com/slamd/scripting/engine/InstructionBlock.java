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
 * This class defines a generic instruction block, which is simply a container
 * that holds zero or more instructions to be executed as a single block.
 *
 *
 * @author   Neil A. Wilson
 */
public class InstructionBlock
       extends Instruction
{
  // The set of instructions contained in this instruction block.
  private Instruction[] instructions;



  /**
   * Creates an instruction block that will execute the specified set of
   * instructions.
   *
   * @param  lineNumber    The line number of the script on which this
   *                       instruction block begins.
   * @param  instructions  The set of instructions to include in this
   *                       instruction block.
   */
  public InstructionBlock(int lineNumber, Instruction[] instructions)
  {
    super(lineNumber);

    if (instructions == null)
    {
      this.instructions = new Instruction[0];
    }
    else
    {
      this.instructions = instructions;
    }
  }



  /**
   * Retrieves the set of instructions that will be executed in this instruction
   * block.
   *
   * @return  The set of instructions that will be executed in this instruction
   *          block.
   */
  public Instruction[] getInstructions()
  {
    return instructions;
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
    // Execute all the instructions in the block.  Each one should check the
    // shouldStop() method, so there's no need to do it here.
    for (int i=0; i < instructions.length; i++)
    {
      instructions[i].execute(jobThread);
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
    jobThread.writeVerbose(lineNumber + ":  begin");

    for (int i=0; i < instructions.length; i++)
    {
      instructions[i].debugExecute(jobThread);
    }

    jobThread.writeVerbose(lineNumber + ":  end");
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
    buffer.append("begin");
    buffer.append(Constants.EOL);

    for (int i=0; i < instructions.length; i++)
    {
      buffer.append(instructions[i].toString(indent+Constants.SCRIPT_INDENT));
    }

    buffer.append(indentBuffer);
    buffer.append("end;");
    buffer.append(Constants.EOL);

    return buffer.toString();
  }
}

