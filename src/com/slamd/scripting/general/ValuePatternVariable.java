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
package com.slamd.scripting.general;



import java.util.Random;

import com.unboundid.util.ValuePattern;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a variable that can be used to generate values based on a
 * user-defined pattern.  That pattern may include a numeric range that
 * indicates that values should be chosen at random or in sequential order.
 *
 * <UL>
 *   <LI>assign(string valueString) -- Specifies the value string that should
 *       be parsed to create the returned value.</LI>
 *   <LI>nextValue() -- Retrieves the next value that was generated from the
 *       pattern as a string value.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class ValuePatternVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of value pattern variables.
   */
  public static final String VALUE_PATTERN_VARIABLE_TYPE = "valuepattern";



  /**
   * The name of the method that will be used to specify the pattern to use to
   * construct the values.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the first "assign" method.
   */
  public static final int ASSIGN_1_METHOD_NUMBER = 0;



  /**
   * The method number for the second "assign" method.
   */
public static final int ASSIGN_2_METHOD_NUMBER = 1;


  /**
   * The name of the method that will retrieve the next value based on the
   * user-defined pattern.
   */
  public static final String NEXT_VALUE_METHOD_NAME = "nextvalue";



  /**
   * The method number for the "nextValue" method.
   */
  public static final int NEXT_VALUE_METHOD_NUMBER = 2;



  // The parent random number generator to use to initialize the generators for
  // each thread.
  static Random parentRandom = new Random();



  /**
   * The set of methods associated with file URL variables.
   */
  public static final Method[] VALUE_PATTERN_VARIABLE_METHODS = new Method[]
  {
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(NEXT_VALUE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
  };



  // The value pattern to use to construct the values.
  ValuePattern pattern;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public ValuePatternVariable()
         throws ScriptException
  {
    try
    {
      pattern = new ValuePattern("");
    } catch (Exception e) {}
  }



  /**
   * Retrieves the URL string for this file URL variable.
   *
   * @return  The URL string for this file URL variable.
   */
  public String nextValue()
  {
    return pattern.nextValue();
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return VALUE_PATTERN_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return VALUE_PATTERN_VARIABLE_METHODS;
  }



  /**
   * Indicates whether this variable type has a method with the specified name.
   *
   * @param  methodName  The name of the method.
   *
   * @return  <CODE>true</CODE> if this variable has a method with the specified
   *          name, or <CODE>false</CODE> if it does not.
   */
  @Override()
  public boolean hasMethod(String methodName)
  {
    for (int i=0; i < VALUE_PATTERN_VARIABLE_METHODS.length; i++)
    {
      if (VALUE_PATTERN_VARIABLE_METHODS[i].getName().equals(methodName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the method number for the method that has the specified name and
   * argument types, or -1 if there is no such method.
   *
   * @param  methodName     The name of the method.
   * @param  argumentTypes  The list of argument types for the method.
   *
   * @return  The method number for the method that has the specified name and
   *          argument types.
   */
  @Override()
  public int getMethodNumber(String methodName, String[] argumentTypes)
  {
    for (int i=0; i < VALUE_PATTERN_VARIABLE_METHODS.length; i++)
    {
      if (VALUE_PATTERN_VARIABLE_METHODS[i].hasSignature(methodName,
                                                         argumentTypes))
      {
        return i;
      }
    }

    return -1;
  }



  /**
   * Retrieves the return type for the method with the specified name and
   * argument types.
   *
   * @param  methodName     The name of the method.
   * @param  argumentTypes  The set of argument types for the method.
   *
   * @return  The return type for the method, or <CODE>null</CODE> if there is
   *          no such method defined.
   */
  @Override()
  public String getReturnTypeForMethod(String methodName,
                                       String[] argumentTypes)
  {
    for (int i=0; i < VALUE_PATTERN_VARIABLE_METHODS.length; i++)
    {
      if (VALUE_PATTERN_VARIABLE_METHODS[i].hasSignature(methodName,
                                                         argumentTypes))
      {
        return VALUE_PATTERN_VARIABLE_METHODS[i].getReturnType();
      }
    }

    return null;
  }



  /**
   * Executes the specified method, using the provided variables as arguments
   * to the method, and makes the return value available to the caller.
   *
   * @param  lineNumber    The line number of the script in which the method
   *                       call occurs.
   * @param  methodNumber  The method number of the method to execute.
   * @param  arguments     The set of arguments to use for the method.
   *
   * @return  The value returned from the method, or <CODE>null</CODE> if it
   *          does not return a value.
   *
   * @throws  ScriptException  If the specified method does not exist, or if a
   *                           problem occurs while attempting to execute it.
   */
  @Override()
  public Variable executeMethod(int lineNumber, int methodNumber,
                                Argument[] arguments)
         throws ScriptException
  {
    switch (methodNumber)
    {
      case ASSIGN_1_METHOD_NUMBER:
        // Read the value pattern to use and don't return a value.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        try
        {
          pattern = new ValuePattern(sv.getStringValue());
        }
        catch (Exception e)
        {
          throw new ScriptException(lineNumber,
               "Unable to parse the provided value pattern:  " + e, e);
        }
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Read the value pattern and format string to use and don't return a
        // value.
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        String fmt = sv1.getStringValue() + '%' + sv2.getStringValue();
        try
        {
          pattern = new ValuePattern(fmt);
        }
        catch (Exception e)
        {
          throw new ScriptException(lineNumber,
               "Unable to parse the provided value pattern:  " + e, e);
        }
        return null;
      case NEXT_VALUE_METHOD_NUMBER:
        // Return the set of lines read as a string array.
        return new StringVariable(nextValue());
      default:
        throw new ScriptException(lineNumber,
                                  "There is no method " + methodNumber +
                                  " defined for " + getArgumentType() +
                                  " variables.");
    }
  }



  /**
   * Assigns the value of the provided argument to this variable.  The value of
   * the provided argument must be of the same type as this variable.
   *
   * @param  argument  The argument whose value should be assigned to this
   *                   variable.
   *
   * @throws  ScriptException  If a problem occurs while performing the
   *                           assignment.
   */
  @Override()
  public void assign(Argument argument)
         throws ScriptException
  {
    if (! argument.getArgumentType().equals(VALUE_PATTERN_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                VALUE_PATTERN_VARIABLE_TYPE + " rejected.");
    }

    ValuePatternVariable vpv =
         (ValuePatternVariable) argument.getArgumentValue();
    pattern = vpv.pattern;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return pattern.toString();
  }
}

