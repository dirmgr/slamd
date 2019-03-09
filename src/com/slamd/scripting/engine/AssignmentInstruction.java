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
 * This class defines an instruction that will assign the value of an argument
 * to a variable.  The variable to which the argument value is assigned must
 * have the same type as the argument value.
 *
 *
 * @author   Neil A. Wilson
 */
public class AssignmentInstruction
       extends Instruction
{
  // The argument whose value is being assigned to the variable.
  private Argument argument;

  // The variable whose value is to be assigned from the argument.
  private Variable variable;



  /**
   * Creates a new assignment instruction with the specified variable and
   * argument.
   *
   * @param  lineNumber  The line number on which the if statement occurs in the
   *                     script.
   * @param  variable    The variable that is to be assigned the value of the
   *                     argument.
   * @param  argument    The argument whose value is to be assigned to the
   *                     variable.
   */
  public AssignmentInstruction(int lineNumber, Variable variable,
                               Argument argument)
  {
    super(lineNumber);

    this.variable = variable;
    this.argument = argument;
  }



  /**
   * Retrieves the variable to which the argument value will be assigned.
   *
   * @return  The variable to which the argument value will be assigned.
   */
  public Variable getVariable()
  {
    return variable;
  }



  /**
   * Retrieves the argument whose value will be assigned to the variable.
   *
   * @return  The argument whose value will be assigned to the variable.
   */
  public Argument getArgument()
  {
    return argument;
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
    variable.assign(argument);
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
    jobThread.writeVerbose(lineNumber + ":  " + variable.getName() + " = " +
                           argument.getArgumentAsString());
    variable.assign(argument);
    jobThread.writeVerbose("\tAssignment value:  " +
                           variable.getValueAsString());
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

    buffer.append(variable.getName());
    buffer.append(" = ");
    buffer.append(argument.getArgumentAsString());
    buffer.append(';');
    buffer.append(Constants.EOL);

    return buffer.toString();
  }
}

