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
 * This class defines an instruction that consists of calling a method
 * associated with a variable.
 *
 *
 * @author   Neil A. Wilson
 */
public class MethodCallInstruction
       extends Instruction
       implements Argument
{
  // The set of arguments to be passed to the method.
  private Argument[] arguments;

  // The number assigned to the method being called.
  private int methodNumber;

  // The argument type for this method call (i.e., the return type).
  private String argumentType;

  // The name of the method to execute.
  private String methodName;

  // The value returned from executing the method.
  private Variable returnValue;

  // The variable to use to invoke the method.
  private Variable targetVariable;



  /**
   * Creates a new method call instruction.
   *
   * @param  lineNumber      The line number of the script on which this method
   *                         call begins.
   * @param  targetVariable  The variable on which the instruction will be
   *                         performed.
   * @param  methodName      The name of the method to execute.
   * @param  methodNumber    The method number for this method call.
   * @param  arguments       The set of arguments to pass to the method.
   */
  public MethodCallInstruction(int lineNumber, Variable targetVariable,
                               String methodName, int methodNumber,
                               Argument[] arguments)
  {
    super(lineNumber);

    this.targetVariable = targetVariable;
    this.methodName     = methodName;
    this.methodNumber   = methodNumber;
    this.arguments      = arguments;

    String[] argumentTypes = new String[arguments.length];
    for (int i=0; i < arguments.length; i++)
    {
      argumentTypes[i] = arguments[i].getArgumentType();
    }
    argumentType = targetVariable.getReturnTypeForMethod(methodName,
                                                         argumentTypes);
  }



  /**
   * Retrieves the variable on which the instruction will be performed.
   *
   * @return  The variable on which the instruction will be performed.
   */
  public Variable getTargetVariable()
  {
    return targetVariable;
  }



  /**
   * Retrieves the name of the method that will be executed.
   *
   * @return  The name of the method that will be executed.
   */
  public String getMethodName()
  {
    return methodName;
  }



  /**
   * Retrieves the method number for this method call.
   *
   * @return  The method number for this method call.
   */
  public int getMethodNumber()
  {
    return methodNumber;
  }



  /**
   * Retrieves the set of arguments that will be passed to the method.
   *
   * @return  The set of arguments that will be passed to the method.
   */
  public Argument[] getArguments()
  {
    return arguments;
  }



  /**
   * Retrieves the value returned from executing the method.
   *
   * @return  The value returned from executing the method.
   */
  public Variable getReturnValue()
  {
    return returnValue;
  }



  /**
   * Performs the function associated with this instruction.
   *
   * @param  jobThread  The job thread that will be executing the script.
   *
   * @throws  ScriptException  If a problem occurs while processing this
   *                           instruction.
   */
  public void execute(JobClass jobThread)
         throws ScriptException
  {
    targetVariable.executeMethod(lineNumber, methodNumber, arguments);
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
  public void debugExecute(JobClass jobThread)
         throws ScriptException
  {
    StringBuilder buffer = new StringBuilder();
    buffer.append(lineNumber);
    buffer.append(":  ");
    buffer.append(targetVariable.getName());
    buffer.append(".");
    buffer.append(methodName);
    buffer.append("(");

    if (arguments.length > 0)
    {
      buffer.append(arguments[0].getArgumentAsString());

      for (int i=1; i < arguments.length; i++)
      {
        buffer.append(", ");
        buffer.append(arguments[i].getArgumentAsString());
      }
    }

    buffer.append(")");
    jobThread.writeVerbose(buffer.toString());
    jobThread.writeVerbose("\tVariable " + targetVariable.getName() +
                           " initial value:  " +
                           targetVariable.getValueAsString());

    Variable returnValue =
         targetVariable.executeMethod(lineNumber, methodNumber, arguments);
    jobThread.writeVerbose("\tVariable " + targetVariable.getName() +
                           " resulting value:  " +
                           targetVariable.getValueAsString());
    if (returnValue != null)
    {
      jobThread.writeVerbose("\tReturn value:  " +
                             returnValue.getValueAsString());
    }
  }



  /**
   * Retrieves the name of the data type associated with this argument.
   *
   * @return  The name of the data type associated with this argument.
   */
  public String getArgumentType()
  {
    return argumentType;
  }



  /**
   * Retrieves the value of this argument.
   *
   * @return  The value of this argument.
   *
   * @throws  ScriptException  If a problem occurs while determining the value
   *                           of this argument.
   */
  public Variable getArgumentValue()
         throws ScriptException
  {
    return targetVariable.executeMethod(lineNumber, methodNumber, arguments);
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
  public String toString(int indent)
  {
    StringBuilder indentBuffer = new StringBuilder();
    for (int i=0; i < indent; i++)
    {
      indentBuffer.append(" ");
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuffer);
    buffer.append(targetVariable.getName());
    buffer.append(".");
    buffer.append(methodName);
    buffer.append("(");

    if (arguments.length > 0)
    {
      buffer.append(arguments[0].getArgumentAsString());

      for (int i=1; i < arguments.length; i++)
      {
        buffer.append(", ");
        buffer.append(arguments[i].getArgumentAsString());
      }
    }

    buffer.append(");");
    buffer.append(Constants.EOL);

    return buffer.toString();
  }



  /**
   * Retrieves a string representation of this argument in a form appropriate
   * for inclusion in a script.
   *
   * @return  A string representation of this argument in a form appropriate for
   *          inclusion in a script.
   */
  public String getArgumentAsString()
  {
    StringBuilder buffer = new StringBuilder();
    buffer.append(targetVariable.getName());
    buffer.append(".");
    buffer.append(methodName);
    buffer.append("(");

    if (arguments.length > 0)
    {
      buffer.append(arguments[0].getArgumentAsString());

      for (int i=1; i < arguments.length; i++)
      {
        buffer.append(", ");
        buffer.append(arguments[i].getArgumentAsString());
      }
    }

    buffer.append(")");

    return buffer.toString();
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   *
   * @throws  ScriptException  If a problem occurs while trying to determine the
   *                           value for this argument.
   */
  public String getValueAsString()
         throws ScriptException
  {
    return getArgumentValue().getValueAsString();
  }
}

