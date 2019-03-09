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



import java.util.ArrayList;
import java.util.Random;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a string array variable that holds zero or more string
 * values.  It also defines a set of methods for dealing with string array
 * variables:
 *
 * <UL>
 *   <LI>addValue(string value) -- Adds the specified string value to this
 *       string array.  This method does not return a value.</LI>
 *   <LI>assign(stringarray values) -- Initializes this string array variable
 *       with the information in the provided string array.  This method does
 *       not return a value.</LI>
 *   <LI>contains(string value, boolean ignoreCase) -- Determines whether this
 *       array contains the specified string value and returns the result as a
 *       Boolean value.</LI>
 *   <LI>firstValue() -- Retrieves the first value in this string array as a
 *       string value.  If there are no values, then a null string will be
 *       returned.</LI>
 *   <LI>indexOf(string value, boolean ignoreCase) -- Determines the location of
 *       the specified string value in this array and returns the result as an
 *       integer value.</LI>
 *   <LI>indexOf(string value, int startPos, boolean ignoreCase) -- Determines
 *       the location of the specified string value in this array starting at
 *       the specified position and returns the result as an integer value.</LI>
 *   <LI>insert(string value, int position) -- Inserts the specified string
 *       value at the indicated position in this string array.  This method does
 *       not return a value.</LI>
 *   <LI>isEmpty() -- Returns a Boolean value that indicates whether this string
 *       array is empty (i.e., has no values).</LI>
 *   <LI>isNotEmpty() -- Returns a Boolean value that indicates whether this
 *       string array is empty (i.e., has no values).</LI>
 *   <LI>length() -- Returns the number of elements in this string array as an
 *       integer value.</LI>
 *   <LI>nextValue() -- Returns the next value from this string array as a
 *       string value.  If there are no more values, then a null string will be
 *       returned.</LI>
 *   <LI>randomValue() -- Returns a value from a random position in this string
 *       array as a string value.  If there are no values, then a null string
 *       will be returned.</LI>
 *   <LI>remove(string value, boolean ignoreCase) -- Removes the first
 *       occurrence of the specified value from this string array.  This method
 *       returns a Boolean value that indicates whether a value was actually
 *       removed.</LI>
 *   <LI>removeAll() -- Removes all values in this string array.  This method
 *       does not return a value.</LI>
 *   <LI>removeValueAt(int position) -- Removes the value at the specified
 *       position from this array.  This method returns the string value that
 *       was removed.</LI>
 *   <LI>setValueAt(string value, int position) -- Replaces the current value at
 *       the specified position with the provided value.  This method does not
 *       return a value.</LI>
 *   <LI>valueAt(int position) -- Retrieves the string value at the specified
 *       position in this array.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class StringArrayVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of string array variables.
   */
  public static final String STRING_ARRAY_VARIABLE_TYPE = "stringarray";



  /**
   * The name of the method that will be used to add a new value to this array.
   */
  public static final String ADD_VALUE_METHOD_NAME = "addvalue";



  /**
   * The method number for the "addValue" method.
   */
  public static final int ADD_VALUE_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to initialize this array.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the "assign" method.
   */
  public static final int ASSIGN_METHOD_NUMBER = 1;



  /**
   * The name of the method that will be used to determine if this method
   * contains the specified value.
   */
  public static final String CONTAINS_METHOD_NAME = "contains";



  /**
   * The method number for the "contains" method.
   */
  public static final int CONTAINS_METHOD_NUMBER = 2;



  /**
   * The name of the method that will be used to retrieve the first value from
   * this array.
   */
  public static final String FIRST_VALUE_METHOD_NAME = "firstvalue";



  /**
   * The method number for the "firstValue" method.
   */
  public static final int FIRST_VALUE_METHOD_NUMBER = 3;



  /**
   * The name of the method that will be used to determine the position of the
   * specified value.
   */
  public static final String INDEX_OF_METHOD_NAME = "indexof";



  /**
   * The method number for the first "indexOf" method.
   */
  public static final int INDEX_OF_1_METHOD_NUMBER = 4;



  /**
   * The method number for the second "indexOf" method.
   */
  public static final int INDEX_OF_2_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to insert a value into this array.
   */
  public static final String INSERT_METHOD_NAME = "insert";



  /**
   * The method number for the "insert" method.
   */
  public static final int INSERT_METHOD_NUMBER = 6;



  /**
   * The name of the method that will determine if this array is empty.
   */
  public static final String IS_EMPTY_METHOD_NAME = "isempty";



  /**
   * The method number for the "isEmpty" method.
   */
  public static final int IS_EMPTY_METHOD_NUMBER = 7;



  /**
   * The name of the method that will determine if this array is not empty.
   */
  public static final String IS_NOT_EMPTY_METHOD_NAME = "isnotempty";



  /**
   * The method number for the "isNotEmpty" method.
   */
  public static final int IS_NOT_EMPTY_METHOD_NUMBER = 8;



  /**
   * The name of the method that will be used to determine the number of
   * elements in this array.
   */
  public static final String LENGTH_METHOD_NAME = "length";



  /**
   * The method number for the "length" method.
   */
  public static final int LENGTH_METHOD_NUMBER = 9;



  /**
   * The name of the method that will be used to retrieve the next value from
   * the array.
   */
  public static final String NEXT_VALUE_METHOD_NAME = "nextvalue";



  /**
   * The method number for the "nextValue" method.
   */
  public static final int NEXT_VALUE_METHOD_NUMBER = 10;



  /**
   * The name of the method that will be used to retrieve a random value from
   * the array.
   */
  public static final String RANDOM_VALUE_METHOD_NAME = "randomvalue";



  /**
   * the method number for the "randomValue" method.
   */
  public static final int RANDOM_VALUE_METHOD_NUMBER = 11;



  /**
   * The name of the method that will be used to remove a value from this array.
   */
  public static final String REMOVE_METHOD_NAME = "remove";



  /**
   * The method number for the "remove" method.
   */
  public static final int REMOVE_METHOD_NUMBER = 12;



  /**
   * The name of the method that will be used to remove all values from this
   * array.
   */
  public static final String REMOVE_ALL_METHOD_NAME = "removeall";



  /**
   * The method number for the "removeAll" method.
   */
  public static final int REMOVE_ALL_METHOD_NUMBER = 13;



  /**
   * The name of the method that will be used to remove the value at the
   * specified position from the array.
   */
  public static final String REMOVE_VALUE_AT_METHOD_NAME = "removevalueat";



  /**
   * The method number for the "removeValueAt" method.
   */
  public static final int REMOVE_VALUE_AT_METHOD_NUMBER = 14;



  /**
   * The name of the method that will be used to set the value at the specified
   * position in the array.
   */
  public static final String SET_VALUE_AT_METHOD_NAME = "setvalueat";



  /**
   * The method number for the "setValueAt" method.
   */
  public static final int SET_VALUE_AT_METHOD_NUMBER = 15;



  /**
   * The name of the method that will be used to retrieve the value at the
   * specified position in the array.
   */
  public static final String VALUE_AT_METHOD_NAME = "valueat";



  /**
   * The method number for the "valueAt" method.
   */
  public static final int VALUE_AT_METHOD_NUMBER = 16;



  /**
   * The set of methods associated with string array variables.
   */
  public static final Method[] STRING_ARRAY_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME, new String[] { STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(CONTAINS_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(FIRST_VALUE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(INDEX_OF_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(INDEX_OF_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(INSERT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(IS_EMPTY_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(IS_NOT_EMPTY_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(LENGTH_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(NEXT_VALUE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(RANDOM_VALUE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(REMOVE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(REMOVE_ALL_METHOD_NAME, new String[0], null),
    new Method(REMOVE_VALUE_AT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(SET_VALUE_AT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(VALUE_AT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE)
  };



  // The position of the value that will be retrieved with the next call to
  // "nextValue".
  private int valuePosition;

  // The random number generator.
  private Random random;

  // The set of values associated with this array.
  private ArrayList<String> stringValues;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public StringArrayVariable()
         throws ScriptException
  {
    stringValues  = new ArrayList<String>();
    random        = new Random();
    valuePosition = 0;
  }



  /**
   * Creates a new string array variable with the specified set of values.
   *
   * @param  values  The set of values to use for this string array variable.
   */
  public StringArrayVariable(String[] values)
  {
    stringValues  = new ArrayList<String>();
    random        = new Random();
    valuePosition = 0;

    for (int i=0; i < values.length; i++)
    {
      stringValues.add(values[i]);
    }
  }



  /**
   * Retrieves the set of string values associated with this variable.
   *
   * @return  The set of string values associated with this variable.
   */
  public String[] getStringValues()
  {
    String[] returnValues = new String[stringValues.size()];
    stringValues.toArray(returnValues);
    return returnValues;
  }



  /**
   * Specifies the set of string values for this variable.
   *
   * @param  stringValues  The set of string values for this variable.
   */
  public void setStringValues(String[] stringValues)
  {
    this.stringValues.clear();
    for (int i=0; i < stringValues.length; i++)
    {
      this.stringValues.add(stringValues[i]);
    }
  }



  /**
   * Adds the specified value to this string array.
   *
   * @param  stringValue  The value to add to this string array.
   */
  public void addStringValue(String stringValue)
  {
    stringValues.add(stringValue);
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return STRING_ARRAY_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return STRING_ARRAY_VARIABLE_METHODS;
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
    for (int i=0; i < STRING_ARRAY_VARIABLE_METHODS.length; i++)
    {
      if (STRING_ARRAY_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < STRING_ARRAY_VARIABLE_METHODS.length; i++)
    {
      if (STRING_ARRAY_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < STRING_ARRAY_VARIABLE_METHODS.length; i++)
    {
      if (STRING_ARRAY_VARIABLE_METHODS[i].hasSignature(methodName,
                                                        argumentTypes))
      {
        return STRING_ARRAY_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_VALUE_METHOD_NUMBER:
        // Get the value of the new string to add and add it to the value set.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        stringValues.add(sv.getStringValue());

        // This method does not return a value.
        return null;
      case ASSIGN_METHOD_NUMBER:
        // Get the string array and use its values.
        StringArrayVariable sav = (StringArrayVariable)
                                  arguments[0].getArgumentValue();
        setStringValues(sav.getStringValues());

        // This method does not return a value.
        return null;
      case CONTAINS_METHOD_NUMBER:
        // Get the value of the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        String value = sv.getStringValue();

        // Get the value of the Boolean argument.
        BooleanVariable bv = (BooleanVariable) arguments[1].getArgumentValue();
        boolean ignoreCase = bv.getBooleanValue();

        // Iterate through all the values until we find a match or reach the end
        // of the list.
        boolean matchFound = false;
        int size = stringValues.size();
        for (int i=0; i < size; i++)
        {
          if (ignoreCase)
          {
            if (value.equalsIgnoreCase(stringValues.get(i)))
            {
              matchFound = true;
              break;
            }
          }
          else
          {
            if (value.equals(stringValues.get(i)))
            {
              matchFound = true;
              break;
            }
          }
        }

        // Return the result as a Boolean variable.
        return new BooleanVariable(matchFound);
      case FIRST_VALUE_METHOD_NUMBER:
        // Return the first value as a string and reset the value position
        // indicator.
        if (! stringValues.isEmpty())
        {
          valuePosition = 1;
          return new StringVariable(stringValues.get(0));
        }
        else
        {
          valuePosition = 0;
          return new StringVariable();
        }
      case INDEX_OF_1_METHOD_NUMBER:
        // Get the value of the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.getStringValue();

        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[1].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Iterate through all the values until we find a match or reach the end
        // of the list.
        int position = -1;
        size = stringValues.size();
        for (int i=0; i < size; i++)
        {
          if (ignoreCase)
          {
            if (value.equalsIgnoreCase(stringValues.get(i)))
            {
              position = i;
              break;
            }
          }
          else
          {
            if (value.equals(stringValues.get(i)))
            {
              position = i;
              break;
            }
          }
        }

        // Return the result as an integer variable.
        return new IntegerVariable(position);
      case INDEX_OF_2_METHOD_NUMBER:
        // Get the value of the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.getStringValue();

        // Get the value of the integer argument.
        IntegerVariable iv = (IntegerVariable) arguments[1].getArgumentValue();
        int startPos = iv.getIntValue();

        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[2].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Iterate through all the values until we find a match or reach the end
        // of the list.
        position = -1;
        size = stringValues.size();
        for (int i=startPos; i < size; i++)
        {
          if (ignoreCase)
          {
            if (value.equalsIgnoreCase(stringValues.get(i)))
            {
              position = i;
              break;
            }
          }
          else
          {
            if (value.equals(stringValues.get(i)))
            {
              position = i;
              break;
            }
          }
        }

        // Return the result as an integer variable.
        return new IntegerVariable(position);
      case INSERT_METHOD_NUMBER:
        // Get the value of the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.getStringValue();

        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[1].getArgumentValue();
        position = iv.getIntValue();

        // Insert the value in the specified position and don't return a value.
        stringValues.add(position, value);
        return null;
      case IS_EMPTY_METHOD_NUMBER:
        // Return a Boolean value that indicates whether this array is empty.
        return new BooleanVariable(stringValues.isEmpty());
      case IS_NOT_EMPTY_METHOD_NUMBER:
        // Return a Boolean value that indicates whether this array is not
        // empty.
        return new BooleanVariable(! stringValues.isEmpty());
      case LENGTH_METHOD_NUMBER:
        // Return the number of elements as an integer value.
        return new IntegerVariable(stringValues.size());
      case NEXT_VALUE_METHOD_NUMBER:
        // Return the next value as a string and update the value position
        // indicator.
        if (stringValues.size() > valuePosition)
        {
          return new StringVariable(stringValues.get(valuePosition++));
        }
        else
        {
          return new StringVariable();
        }
      case RANDOM_VALUE_METHOD_NUMBER:
        // Return the next value as a string and update the value position
        // indicator.
        if (! stringValues.isEmpty())
        {
          position = Math.abs(random.nextInt()) % stringValues.size();
          return new StringVariable(stringValues.get(position));
        }
        else
        {
          return new StringVariable();
        }
      case REMOVE_METHOD_NUMBER:
        // Get the value of the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.getStringValue();

        // Get the value of the Boolean argument.
        bv = (BooleanVariable) arguments[1].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Iterate through all the values until we find a match or reach the end
        // of the list.
        matchFound = false;
        size = stringValues.size();
        for (int i=0; i < size; i++)
        {
          if (ignoreCase)
          {
            if (value.equalsIgnoreCase(stringValues.get(i)))
            {
              matchFound = true;
              stringValues.remove(i);
              break;
            }
          }
          else
          {
            if (value.equals(stringValues.get(i)))
            {
              matchFound = true;
              stringValues.remove(i);
              break;
            }
          }
        }

        // Return the result as a Boolean variable.
        return new BooleanVariable(matchFound);
      case REMOVE_ALL_METHOD_NUMBER:
        // Remove all elements and don't return a value.
        stringValues.clear();
        return null;
      case REMOVE_VALUE_AT_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        position = iv.getIntValue();

        // Remove the value and return the value removed as a string.
        return new StringVariable(stringValues.remove(position));
      case SET_VALUE_AT_METHOD_NUMBER:
        // Get the value of the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.getStringValue();

        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[1].getArgumentValue();
        position = iv.getIntValue();

        // Replace the value in the specified position and don't return a value.
        stringValues.set(position, value);
        return null;
      case VALUE_AT_METHOD_NUMBER:
        // Get the value of the integer argument.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        position = iv.getIntValue();

        // Return the value as a string.
        return new StringVariable(stringValues.get(position));
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
    if (! argument.getArgumentType().equals(STRING_ARRAY_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                STRING_ARRAY_VARIABLE_TYPE + " rejected.");
    }

    StringArrayVariable sav = (StringArrayVariable) argument.getArgumentValue();
    stringValues  = sav.stringValues;
    valuePosition = sav.valuePosition;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    int numValues = stringValues.size();

    switch (numValues)
    {
      case 0:
        return "{ no values }";
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        StringBuilder buffer = new StringBuilder();
        buffer.append("{ ");
        buffer.append('"');
        buffer.append(stringValues.get(0));
        buffer.append('"');

        for (int i=1; i < numValues; i++)
        {
          buffer.append(", ");
          buffer.append('"');
          buffer.append(stringValues.get(i));
          buffer.append('"');
        }

        buffer.append(" }");
        return buffer.toString();
      default:
        return "{ " + stringValues.size() + " values }";
    }
  }
}

