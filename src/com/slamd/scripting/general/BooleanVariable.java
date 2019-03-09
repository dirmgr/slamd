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



import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a Boolean variable that has a value of either "true" or
 * "false".  It also defines a set of methods for dealing with Boolean
 * variables:
 *
 * <UL>
 *   <LI>and(boolean booleanValue) -- Performs a logical AND between this and
 *       the provided Boolean variable.</LI>
 *   <LI>and(boolean value1, boolean value2) -- Performs a logical AND between
 *       the two Boolean values provided as arguments.</LI>
 *   <LI>assign(boolean booleanValue) -- Assigns the specified boolean value to
 *       this boolean variable.  This method does not return a value.</LI>
 *   <LI>assign(string stringValue, boolean defaultValue) -- Assigns the boolean
 *       equivalent of the provided string value to this boolean variable.  If
 *       the provided string cannot be interpreted as a boolean value, then the
 *       default value will be used.</LI>
 *   <LI>equals(boolean booleanValue) -- Determines whether this Boolean
 *       variable has the same value as the provided Boolean variable and
 *       returns the result as a Boolean value.</LI>
 *   <LI>equals(boolean value1, boolean value2) -- Determines whether the two
 *       Boolean values provided as arguments are equal.</LI>
 *   <LI>isFalse() -- Determines whether this Boolean variable has a value of
 *       false and provides the result as a Boolean value (i.e., it returns the
 *       inverse of this Boolean value).</LI>
 *   <LI>notEqual(boolean booleanValue) -- Determine whether this Boolean
 *       variable has a different value from the provided Boolean variable and
 *       returns the result as a Boolean value.
 *   <LI>notEqual(boolean value1, boolean value2) -- Determine whether the two
 *       provided Boolean variables have different values and returns the result
 *       as a Boolean value.
 *   <LI>or(boolean booleanValue) -- Performs a logical OR between this and
 *       the provided Boolean variable.</LI>
 *   <LI>or(boolean value1, boolean value2) -- Performs a logical OR between the
 *       two Boolean values provided as arguments.</LI>
 *   <LI>toString() -- Returns a string representation of the value of this
 *       Boolean variable.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class BooleanVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of Boolean variables.
   */
  public static final String BOOLEAN_VARIABLE_TYPE = "boolean";



  /**
   * The name of the method that will be used to perform a logical AND between
   * this and one or more other Boolean values.
   */
  public static final String AND_METHOD_NAME = "and";



  /**
   * The method number for the first "and" method.
   */
  public static final int AND_1_METHOD_NUMBER = 0;



  /**
   * The method number for the second "and" method.
   */
  public static final int AND_2_METHOD_NUMBER = 1;



  /**
   * The name of the method that will be used to assign a value to this Boolean
   * variable.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the first "assign" method.
   */
  public static final int ASSIGN_1_METHOD_NUMBER = 2;



  /**
   * The method number for the second "assign" method.
   */
  public static final int ASSIGN_2_METHOD_NUMBER = 3;



  /**
   * The name of the method that will be used to determine whether this Boolean
   * variable has a value equal to another Boolean variable.
   */
  public static final String EQUALS_METHOD_NAME = "equals";



  /**
   * The method number for the first "equals" method.
   */
  public static final int EQUALS_1_METHOD_NUMBER = 4;



  /**
   * The method number for the second "equals" method.
   */
  public static final int EQUALS_2_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to determine whether this Boolean
   * variable has a value of false.
   */
  public static final String IS_FALSE_METHOD_NAME = "isfalse";



  /**
   * The method number for the "isFalse" method.
   */
  public static final int IS_FALSE_METHOD_NUMBER = 6;



  /**
   * The name of the method that will be used to determine whether two Boolean
   * values are different.
   */
  public static final String NOT_EQUAL_METHOD_NAME = "notequal";



  /**
   * The method number for the first "notEqual" method.
   */
  public static final int NOT_EQUAL_1_METOHD_NUMBER = 7;



  /**
   * The method number for the second "notEqual" method.
   */
  public static final int NOT_EQUAL_2_METHOD_NUMBER = 8;



  /**
   * The name of the method that will be used to perform a logical OR between
   * this and one or more other Boolean values.
   */
  public static final String OR_METHOD_NAME = "or";



  /**
   * The method number for the first "or" method.
   */
  public static final int OR_1_METHOD_NUMBER = 9;



  /**
   * The method number for the second "or" method.
   */
  public static final int OR_2_METHOD_NUMBER = 10;



  /**
   * The name of the method that will be used to retrieve a string
   * representation of this Boolean variable.
   */
  public static final String TO_STRING_METHOD_NAME = "tostring";



  /**
   * The number for the "toString" method.
   */
  public static final int TO_STRING_METHOD_NUMBER = 11;



  /**
   * The set of methods associated with Boolean variables.
   */
  public static final Method[] BOOLEAN_VARIABLE_METHODS = new Method[]
  {
    new Method(AND_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(AND_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE,
                                               BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(ASSIGN_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE },
               null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              BOOLEAN_VARIABLE_TYPE },
               null),
    new Method(EQUALS_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(EQUALS_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE,
                                                  BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(IS_FALSE_METHOD_NAME, new String[0], BOOLEAN_VARIABLE_TYPE),
    new Method(NOT_EQUAL_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(NOT_EQUAL_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE,
                                                     BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(OR_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(OR_METHOD_NAME, new String[] { BOOLEAN_VARIABLE_TYPE,
                                              BOOLEAN_VARIABLE_TYPE },
               BOOLEAN_VARIABLE_TYPE),
    new Method(TO_STRING_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE)
  };



  // The Boolean value associated with this variable.
  private boolean booleanValue;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public BooleanVariable()
         throws ScriptException
  {
    // No implementation necessary.
  }



  /**
   * Creates a new Boolean variable with the provided value.
   *
   * @param  booleanValue  The value to use for this Boolean variable.
   */
  public BooleanVariable(boolean booleanValue)
  {
    this.booleanValue = booleanValue;
  }



  /**
   * Retrieves the boolean value associated with this Boolean variable.
   *
   * @return  The boolean value associated with this Boolean variable.
   */
  public boolean getBooleanValue()
  {
    return booleanValue;
  }



  /**
   * Specifies the boolean value to use for this Boolean variable.
   *
   * @param  booleanValue  The boolean value to use for this Boolean variable.
   */
  public void setBooleanValue(boolean booleanValue)
  {
    this.booleanValue = booleanValue;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return BOOLEAN_VARIABLE_TYPE;
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
    for (int i=0; i < BOOLEAN_VARIABLE_METHODS.length; i++)
    {
      if (BOOLEAN_VARIABLE_METHODS[i].getName().equals(methodName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return BOOLEAN_VARIABLE_METHODS;
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
    for (int i=0; i < BOOLEAN_VARIABLE_METHODS.length; i++)
    {
      if (BOOLEAN_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
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
    for (int i=0; i < BOOLEAN_VARIABLE_METHODS.length; i++)
    {
      if (BOOLEAN_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
      {
        return BOOLEAN_VARIABLE_METHODS[i].getReturnType();
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
      case AND_1_METHOD_NUMBER:
        // Get the value of the Boolean argument.
        BooleanVariable bv = (BooleanVariable) arguments[0].getArgumentValue();

        // Do the AND and return the result as a Boolean variable.
        return new BooleanVariable(booleanValue && bv.booleanValue);
      case AND_2_METHOD_NUMBER:
        // Get the values of the Boolean arguments.
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        BooleanVariable bv2 = (BooleanVariable) arguments[1].getArgumentValue();

        // Do the AND and return the result as a Boolean variable.
        return new BooleanVariable(bv.booleanValue && bv2.booleanValue);
      case ASSIGN_1_METHOD_NUMBER:
        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[0].getArgumentValue();

        // Make the assignment and don't return a value.
        this.booleanValue = bv.booleanValue;
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Get the value of the arguments.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        bv = (BooleanVariable) arguments[1].getArgumentValue();

        // Make the assignment and don't return a value.
        String stringValue = sv.getStringValue();
        if ((stringValue == null) || (stringValue.length() == 0))
        {
          this.booleanValue = bv.booleanValue;
        }
        else if (stringValue.equalsIgnoreCase("true"))
        {
          this.booleanValue = true;
        }
        else if (stringValue.equalsIgnoreCase("false"))
        {
          this.booleanValue = false;
        }
        else
        {
          this.booleanValue = bv.booleanValue;
        }
        return null;
      case EQUALS_1_METHOD_NUMBER:
        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[0].getArgumentValue();

        // Do the comparison and return the result as a Boolean variable.
        return new BooleanVariable(booleanValue == bv.booleanValue);
      case EQUALS_2_METHOD_NUMBER:
        // Get the values of the Boolean arguments.
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        bv2 = (BooleanVariable) arguments[1].getArgumentValue();

        // Do the comparison and return the result as a Boolean variable.
        return new BooleanVariable(bv.booleanValue == bv2.booleanValue);
      case IS_FALSE_METHOD_NUMBER:
        // Do the evaluation and return the result as a Boolean variable.
        return new BooleanVariable(! booleanValue);
      case NOT_EQUAL_1_METOHD_NUMBER:
        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[0].getArgumentValue();

        // Do the comparison and return the result as a Boolean variable.
        return new BooleanVariable(booleanValue != bv.booleanValue);
      case NOT_EQUAL_2_METHOD_NUMBER:
        // Get the values of the Boolean arguments.
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        bv2 = (BooleanVariable) arguments[1].getArgumentValue();

        // Do the comparison and return the result as a Boolean variable.
        return new BooleanVariable(bv.booleanValue != bv2.booleanValue);
      case OR_1_METHOD_NUMBER:
        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[0].getArgumentValue();

        // Do the and and return the result as a Boolean variable.
        return new BooleanVariable(booleanValue || bv.booleanValue);
      case OR_2_METHOD_NUMBER:
        // Get the values of the Boolean arguments.
        bv = (BooleanVariable) arguments[0].getArgumentValue();
        bv2 = (BooleanVariable) arguments[1].getArgumentValue();

        // Do the and and return the result as a Boolean variable.
        return new BooleanVariable(bv.booleanValue || bv2.booleanValue);
      case TO_STRING_METHOD_NUMBER:
        // Create and return the string representation.
        return new StringVariable(booleanValue
                                  ? BooleanLiteral.BOOLEAN_TRUE_VALUE
                                  : BooleanLiteral.BOOLEAN_FALSE_VALUE);
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
    if (! argument.getArgumentType().equals(BOOLEAN_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                BOOLEAN_VARIABLE_TYPE + " rejected.");
    }

    BooleanVariable bv = (BooleanVariable) argument.getArgumentValue();
    booleanValue = bv.booleanValue;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return String.valueOf(booleanValue);
  }
}


