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
 * This class defines an integer variable that has an integer value.  It also
 * defines a set of methods for dealing with integer variables:
 *
 * <UL>
 *   <LI>add(int intValue) -- Adds the value of the specified integer to the
 *       value of this integer and returns the sum as an integer value.</LI>
 *   <LI>add(int value1, int value2) -- Adds the values of the two specified
 *       integers and returns the sum as an integer value.</LI>
 *   <LI>and(int intValue) -- Performs a bitwise AND between the value of this
 *       integer and the specified integer value and returns the result as an
 *       integer.</LI>
 *   <LI>and(int value1, int value2) -- Performs a bitwise AND between the
 *       values of the provided integers and returns the result as an
 *       integer.</LI>
 *   <LI>assign(int intValue) -- Specifies the value to use for this integer
 *       variable from the provided integer value.  This method does not return
 *       a value.</LI>
 *   <LI>assign(string stringValue, int defaultValue) -- Parses the provided
 *       string as an integer value and uses is as the value for this integer
 *       variable.  If the string value cannot be converted to an integer, then
 *       the provided default value will be used.</LI>
 *   <LI>decrement() -- Decreases the value of this integer variable by one.
 *       The decrement is made internally and this method does not return a
 *       value.</LI>
 *   <LI>decrement(int amount) -- Decreases the value of this integer variable
 *       by the specified amount.  The decrement is made internally and this
 *       method does not return a value.</LI>
 *   <LI>divide(int intValue) -- Divides the value of this integer
 *       variable by the value of the provided integer value and returns the
 *       quotient as an integer value.</LI>
 *   <LI>divide(int value1, int value2) -- Divides the value of the first
 *       integer by the value of the second and returns the quotient as an
 *       integer value.</LI>
 *   <LI>equals(int intValue) -- Determines whether the value of this
 *       integer variable is equal to the value of the provided integer
 *       variable.  The result is returned as a Boolean variable.</LI>
 *   <LI>equals(int value1, int value2) -- Determines whether the two integer
 *       values are equal and returns the result as a Boolean variable.</LI>
 *   <LI>greaterThan(int intValue) -- Determines whether the value of this
 *       integer variable is greater than the value of the provided integer
 *       variable and returns the result as a Boolean variable.</LI>
 *   <LI>greaterThan(int value1, int value2) -- Determines whether the first
 *        integer value is greater than the second and returns the result as an
 *        integer variable.</LI>
 *   <LI>greaterThanOrEqualTo(int intValue) -- Determines whether the value of
 *       this integer variable is greater than or equal to the value of the
 *       provided integer variable and returns the result as a Boolean
 *       variable.</LI>
 *   <LI>greaterThanOrEqualTo(int value1, int value2) -- Determines whether the
 *       first integer value is greater than or equal to the second and returns
 *       the result as an integer variable.</LI>
 *   <LI>increment() -- Increases the value of this integer variable by one.
 *       The increment is made internally and this method does not return a
 *       value.</LI>
 *   <LI>increment(int amount) -- Increases the value of this integer variable
 *       by the specified amount.  The increment is made internally and this
 *       method does not return a value.</LI>
 *   <LI>lessThan(int intValue) -- Determines whether the value of this integer
 *       variable is less than the value of the provided integer
 *       variable and returns the result as a Boolean variable.</LI>
 *   <LI>lessThan(int value1, int value2) -- Determines whether the first
 *       integer value is less than the second and returns the result as a
 *       Boolean variable.</LI>
 *   <LI>lessThanOrEqualTo(int intValue) -- Determines whether the value of this
 *       integer variable is less than or equal to the value of the provided
 *       integer variable and returns the result as a Boolean variable.</LI>
 *   <LI>lessThanorEqualTo(int value1, int value2) -- Determines whether the
 *       first integer value is less than or equal to the second and returns the
 *       result as a Boolean variable.</LI>
 *   <LI>multiply(int intValue) -- Multiplies the value of this integer variable
 *       by the value of the provided integer variable and returns the product
 *       as an integer variable.</LI>
 *   <LI>multiply(int value1, int value2) -- Multiplies the two provided values
 *       and returns their product as an integer value.</LI>
 *   <LI>not() -- Performs a bitwise NOT on this integer value and returns the
 *       result as an integer.</LI>
 *   <LI>not(int intValue) -- Performs a bitwise NOT on the provided integer
 *       value and returns the result as an integer.</LI>
 *   <LI>notEqual(int intValue) -- Determines whether the provided integer value
 *       does not match this integer and returns the result as a Boolean.</LI>
 *   <LI>notEqual(int value1, int value2) -- Determines whether the two provided
 *       integer values are different and returns the result as a Boolean.</LI>
 *   <LI>or(int intValue) -- Performs a bitwise OR between the value of this
 *       integer and the specified integer value and returns the result as an
 *       integer.</LI>
 *   <LI>or(int value1, int value2) -- Performs a bitwise OR between the
 *       values of the provided integers and returns the result as an
 *       integer.</LI>
 *   <LI>remainder(int intValue) -- Divides the value of this integer variable
 *       by the provided integer value and returns the remainder as an integer
 *       value.</LI>
 *   <LI>remainder(int value1, int value2) -- Divides the first integer value
 *       by the second and returns the remainder as an integer value.</LI>
 *   <LI>subtract(int intValue) -- Subtracts the the value of the provided
 *       integer variable from the value of this integer variable and returns
 *       the difference as an integer variable.</LI>
 *   <LI>subtract(int value1, int value2) -- Subtracts the first value from the
 *       second value and returns the difference as an integer variable.</LI>
 *   <LI>toString() -- Returns a string representation of this integer
 *       variable.</LI>
 *   <LI>xor(int intValue) -- Performs a bitwise exclusive OR between the value
 *       of this integer and the specified integer value and returns the result
 *       as an integer.</LI>
 *   <LI>xor(int value1, int value2) -- Performs a bitwise exclusive OR between
 *       the values of the provided integers and returns the result as an
 *       integer.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class IntegerVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of integer variables.
   */
  public static final String INTEGER_VARIABLE_TYPE = "int";



  /**
   * The name of the method that will be used to add integer values.
   */
  public static final String ADD_METHOD_NAME = "add";



  /**
   * The method number for the first "add" method.
   */
  public static final int ADD_1_METHOD_NUMBER = 0;



  /**
   * The method number for the second "add" method.
   */
  public static final int ADD_2_METHOD_NUMBER = 1;



  /**
   * The name of the method that will be used to perform bitwise AND operations.
   */
  public static final String AND_METHOD_NAME = "and";



  /**
   * The method number for the first "and" method.
   */
  public static final int AND_1_METHOD_NUMBER = 2;



  /**
   * The method number for the second "and" method.
   */
  public static final int AND_2_METHOD_NUMBER = 3;



  /**
   * The name of the method that will be used to specify the value of this
   * integer variable.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the first "assign" method.
   */
  public static final int ASSIGN_1_METHOD_NUMBER = 4;



  /**
   * The method number for the second "assign" method.
   */
  public static final int ASSIGN_2_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to decrement the value of this
   * integer variable.
   */
  public static final String DECREMENT_METHOD_NAME = "decrement";



  /**
   * The method number for the first "decrement" method.
   */
  public static final int DECREMENT_1_METHOD_NUMBER = 6;



  /**
   * The method number for the second "decrement" method.
   */
  public static final int DECREMENT_2_METHOD_NUMBER = 7;



  /**
   * The name of the method that will be used to divide one integer value by
   * another.
   */
  public static final String DIVIDE_METHOD_NAME = "divide";



  /**
   * The method number for the "divide" method.
   */
  public static final int DIVIDE_1_METHOD_NUMBER = 8;



  /**
   * The method number for the "divideBy" method.
   */
  public static final int DIVIDE_2_METHOD_NUMBER = 9;



  /**
   * The name of the method that will be used to determine whether the value
   * of this integer variable is equal to the value of another integer variable.
   */
  public static final String EQUALS_METHOD_NAME = "equals";



  /**
   * The method number for the first "equals" method.
   */
  public static final int EQUALS_1_METHOD_NUMBER = 10;



  /**
   * The method number for the second "equals" method.
   */
  public static final int EQUALS_2_METHOD_NUMBER = 11;



  /**
   * The name of the method that will be used to determine whether the value of
   * this integer variable is greater than the value of another integer
   * variable.
   */
  public static final String GREATER_THAN_METHOD_NAME = "greaterthan";



  /**
   * The method number for the first "greaterThan" method.
   */
  public static final int GREATER_THAN_1_METHOD_NUMBER = 12;



  /**
   * The method number for the second "greaterThan" method.
   */
  public static final int GREATER_THAN_2_METHOD_NUMBER = 13;



  /**
   * The name of the method that will be used to determine whether the value of
   * this integer variable is greater than or equal to the value of another
   * integer variable.
   */
  public static final String GREATER_OR_EQUAL_METHOD_NAME =
       "greaterthanorequalto";



  /**
   * The method number for the first "greaterThanOrEqualTo" method.
   */
  public static final int GREATER_OR_EQUAL_1_METHOD_NUMBER = 14;



  /**
   * The method number for the second "greaterThanOrEqualTo" method.
   */
  public static final int GREATER_OR_EQUAL_2_METHOD_NUMBER = 15;



  /**
   * The name of the method that will be used to increment the value of this
   * integer variable.
   */
  public static final String INCREMENT_METHOD_NAME = "increment";



  /**
   * The method number for the first "increment" method.
   */
  public static final int INCREMENT_1_METHOD_NUMBER = 16;



  /**
   * The method number for the second "increment" method.
   */
  public static final int INCREMENT_2_METHOD_NUMBER = 17;



  /**
   * The name of the method that will be used to determine whether the value of
   * this integer variable is less than the value of another integer variable.
   */
  public static final String LESS_THAN_METHOD_NAME = "lessthan";



  /**
   * The method number for the first "lessThan" method.
   */
  public static final int LESS_THAN_1_METHOD_NUMBER = 18;



  /**
   * The method number for the second "lessThan" method.
   */
  public static final int LESS_THAN_2_METHOD_NUMBER = 19;



  /**
   * The name of the method that will be used to determine whether the value of
   * this integer variable is less than or equal to the value of another integer
   * variable.
   */
  public static final String LESS_OR_EQUAL_METHOD_NAME = "lessthanorequalto";



  /**
   * The method number for the first "lessThanOrEqualTo" method.
   */
  public static final int LESS_OR_EQUAL_1_METHOD_NUMBER = 20;



  /**
   * The method number for the second "lessThanOrEqualTo" method.
   */
  public static final int LESS_OR_EQUAL_2_METHOD_NUMBER = 21;



  /**
   * The name of the method that multiplies the values of two integer variables.
   */
  public static final String MULTIPLY_METHOD_NAME = "multiply";



  /**
   * The method number for the "multiply" method.
   */
  public static final int MULTIPLY_1_METHOD_NUMBER = 22;



  /**
   * The method number for the "multiplyBy" method.
   */
  public static final int MULTIPLY_2_METHOD_NUMBER = 23;



  /**
   * The name of the method that will be used to perform bitwise NOT operations.
   */
  public static final String NOT_METHOD_NAME = "not";



  /**
   * The method number for the first "not" method.
   */
  public static final int NOT_1_METHOD_NUMBER = 24;



  /**
   * The method number for the second "not" method.
   */
  public static final int NOT_2_METHOD_NUMBER = 25;



  /**
   * The name of the method that will determine whether two integer values are
   * not equal.
   */
  public static final String NOT_EQUAL_METHOD_NAME = "notequal";



  /**
   * The method number for the first "notEqual" method.
   */
  public static final int NOT_EQUAL_1_METHOD_NUMBER = 26;



  /**
   * The method number for the second "notEqual" method.
   */
  public static final int NOT_EQUAL_2_METHOD_NUMBER = 27;



  /**
   * The name of the method that will be used to perform bitwise OR operations.
   */
  public static final String OR_METHOD_NAME = "or";



  /**
   * The method number for the first "and" method.
   */
  public static final int OR_1_METHOD_NUMBER = 28;



  /**
   * The method number for the second "and" method.
   */
  public static final int OR_2_METHOD_NUMBER = 29;



  /**
   * The name of the method that will determine the remainder when performing
   * integer division between two values.
   */
  public static final String REMAINDER_METHOD_NAME = "remainder";



  /**
   * The method number for the first "remainder" method.
   */
  public static final int REMAINDER_1_METHOD_NUMBER = 30;



  /**
   * The method number for the second "remainder" method.
   */
  public static final int REMAINDER_2_METHOD_NUMBER = 31;



  /**
   * The name of the method that subtracts the value of another integer variable
   * from the value of this integer variable.
   */
  public static final String SUBTRACT_METHOD_NAME = "subtract";



  /**
   * The method number for the first "subtract" method.
   */
  public static final int SUBTRACT_1_METHOD_NUMBER = 32;



  /**
   * The method number for the second "subtract" method.
   */
  public static final int SUBTRACT_2_METHOD_NUMBER = 33;



  /**
   * The name of the method that returns a string representation of this integer
   * variable.
   */
  public static final String TO_STRING_METHOD_NAME = "tostring";



  /**
   * The method number for the "toString" method.
   */
  public static final int TO_STRING_METHOD_NUMBER = 34;



  /**
   * The name of the method that will be used to perform bitwise exclusive OR
   * operations.
   */
  public static final String XOR_METHOD_NAME = "xor";



  /**
   * The method number for the first "xor" method.
   */
  public static final int XOR_1_METHOD_NUMBER = 35;



  /**
   * The method number for the second "xor" method.
   */
  public static final int XOR_2_METHOD_NUMBER = 36;



  /**
   * The set of methods associated with integer variables.
   */
  public static final Method[] INTEGER_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(ADD_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                               INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(AND_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(AND_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                               INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(ASSIGN_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              INTEGER_VARIABLE_TYPE }, null),
    new Method(DECREMENT_METHOD_NAME, new String[0], null),
    new Method(DECREMENT_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE } ,
               null),
    new Method(DIVIDE_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(DIVIDE_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                  INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(EQUALS_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(EQUALS_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                  INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GREATER_THAN_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GREATER_THAN_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                        INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GREATER_OR_EQUAL_METHOD_NAME,
               new String[] { INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GREATER_OR_EQUAL_METHOD_NAME,
               new String[] { INTEGER_VARIABLE_TYPE, INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(INCREMENT_METHOD_NAME, new String[0], null),
    new Method(INCREMENT_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE } ,
               null),
    new Method(LESS_THAN_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(LESS_THAN_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                     INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(LESS_OR_EQUAL_METHOD_NAME,
               new String[] { INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(LESS_OR_EQUAL_METHOD_NAME,
               new String[] { INTEGER_VARIABLE_TYPE, INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(MULTIPLY_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(MULTIPLY_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                    INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(NOT_METHOD_NAME, new String[0], INTEGER_VARIABLE_TYPE),
    new Method(NOT_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(NOT_EQUAL_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(NOT_EQUAL_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                     INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(OR_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(OR_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                              INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(REMAINDER_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(REMAINDER_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                     INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(SUBTRACT_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(SUBTRACT_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                                    INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(TO_STRING_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(XOR_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE),
    new Method(XOR_METHOD_NAME, new String[] { INTEGER_VARIABLE_TYPE,
                                               INTEGER_VARIABLE_TYPE },
               INTEGER_VARIABLE_TYPE)
  };



  // The integer value associated with this variable.
  private int intValue;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public IntegerVariable()
         throws ScriptException
  {
    // No implementation necessary
  }



  /**
   * Creates a new integer variable with the specified value.
   *
   * @param  intValue  The value to use for this integer variable.
   */
  public IntegerVariable(int intValue)
  {
    this.intValue = intValue;
  }



  /**
   * Retrieves the int value associated with this integer variable.
   *
   * @return  The int value associated with this integer variable.
   */
  public int getIntValue()
  {
    return intValue;
  }



  /**
   * Specifies the value to use for this integer variable.
   *
   * @param  intValue  The value to use for this integer variable.
   */
  public void setIntValue(int intValue)
  {
    this.intValue = intValue;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return INTEGER_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return INTEGER_VARIABLE_METHODS;
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
    for (int i=0; i < INTEGER_VARIABLE_METHODS.length; i++)
    {
      if (INTEGER_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < INTEGER_VARIABLE_METHODS.length; i++)
    {
      if (INTEGER_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
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
    for (int i=0; i < INTEGER_VARIABLE_METHODS.length; i++)
    {
      if (INTEGER_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
      {
        return INTEGER_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        IntegerVariable iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Do the addition and return the result as an integer variable.
        return new IntegerVariable(intValue + iv.intValue);
      case ADD_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Do the addition and return the result as an integer variable.
        return new IntegerVariable(iv.intValue + iv2.intValue);
      case AND_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();

        // Do the AND and return the result as an integer variable.
        return new IntegerVariable(intValue & iv.getIntValue());
      case AND_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Do the AND and return the result as an integer variable.
        return new IntegerVariable(iv.getIntValue() & iv2.getIntValue());
      case ASSIGN_1_METHOD_NUMBER:
        // Get the value of the integer argument
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Make the assignment and don't return a value.
        this.intValue = iv.intValue;
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Get the value of the string argument.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        iv = (IntegerVariable) arguments[1].getArgumentValue();

        // Make the assignment and don't return a value.
        try
        {
          this.intValue = Integer.parseInt(sv.getStringValue());
        }
        catch (Exception e)
        {
          this.intValue = iv.getIntValue();
        }
        return null;
      case DECREMENT_1_METHOD_NUMBER:
        // Perform the decrement and don't return a value.
        this.intValue--;
        return null;
      case DECREMENT_2_METHOD_NUMBER:
        // Perform the decrement and don't return a value.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        this.intValue -= iv.getIntValue();
        return null;
      case DIVIDE_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the division and return the result as an integer variable.
        return new IntegerVariable(intValue / iv.intValue);
      case DIVIDE_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the division and return the result as an integer variable.
        return new IntegerVariable(iv.intValue / iv2.intValue);
      case EQUALS_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(intValue == iv.intValue);
      case EQUALS_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(iv.intValue == iv2.intValue);
      case GREATER_THAN_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(intValue > iv.intValue);
      case GREATER_THAN_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(iv.intValue > iv2.intValue);
      case GREATER_OR_EQUAL_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(intValue >= iv.intValue);
      case GREATER_OR_EQUAL_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(iv.intValue >= iv2.intValue);
      case INCREMENT_1_METHOD_NUMBER:
        // Perform the increment and don't return a value.
        this.intValue++;
        return null;
      case INCREMENT_2_METHOD_NUMBER:
        // Perform the increment and don't return a value.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        this.intValue += iv.getIntValue();
        return null;
      case LESS_THAN_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(intValue < iv.intValue);
      case LESS_THAN_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(iv.intValue < iv2.intValue);
      case LESS_OR_EQUAL_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(intValue <= iv.intValue);
      case LESS_OR_EQUAL_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(iv.intValue <= iv2.intValue);
      case MULTIPLY_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the multiplication and return the result as an integer
        // variable.
        return new IntegerVariable(intValue * iv.intValue);
      case MULTIPLY_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the multiplication and return the result as an integer
        // variable.
        return new IntegerVariable(iv.intValue * iv2.intValue);
      case NOT_1_METHOD_NUMBER:
        // Do the NOT and return the result as an integer variable.
        return new IntegerVariable(~intValue);
      case NOT_2_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();

        // Do the NOT and return the result as an integer variable.
        return new IntegerVariable(~iv.getIntValue());
      case NOT_EQUAL_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(intValue != iv.intValue);
      case NOT_EQUAL_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the comparison and return the result as a Boolean variable.
        return new BooleanVariable(iv.intValue != iv2.intValue);
      case OR_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();

        // Do the OR and return the result as an integer variable.
        return new IntegerVariable(intValue | iv.getIntValue());
      case OR_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Do the OR and return the result as an integer variable.
        return new IntegerVariable(iv.getIntValue() | iv2.getIntValue());
      case REMAINDER_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the division and return the remainder as an integer variable.
        return new IntegerVariable(intValue % iv.intValue);
      case REMAINDER_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the division and return the remainder as an integer variable.
        return new IntegerVariable(iv.intValue % iv2.intValue);
      case SUBTRACT_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Perform the subtraction and return the result as an integer variable.
        return new IntegerVariable(intValue - iv.intValue);
      case SUBTRACT_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Perform the subtraction and return the result as an integer variable.
        return new IntegerVariable(iv.intValue - iv2.intValue);
      case TO_STRING_METHOD_NUMBER:
        // Return the string representation of this integer.
        return new StringVariable(String.valueOf(intValue));
      case XOR_1_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();

        // Do the exclusive OR and return the result as an integer variable.
        return new IntegerVariable(intValue ^ iv.getIntValue());
      case XOR_2_METHOD_NUMBER:
        // Get the values of the integer arguments.
        iv  = (IntegerVariable) arguments[0].getArgumentValue();
        iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        // Do the exclusive OR and return the result as an integer variable.
        return new IntegerVariable(iv.getIntValue() ^ iv2.getIntValue());
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
    if (! argument.getArgumentType().equals(INTEGER_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                INTEGER_VARIABLE_TYPE + " rejected.");
    }

    IntegerVariable iv = (IntegerVariable) argument.getArgumentValue();
    intValue = iv.intValue;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return String.valueOf(intValue);
  }
}

