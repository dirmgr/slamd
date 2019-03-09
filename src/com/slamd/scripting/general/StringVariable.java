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
 * This class defines a string variable that has an string value.  It also
 * defines a set of methods for dealing with string variables:
 *
 * <UL>
 *   <LI>append(string stringValue) -- Appends the value of the provided string
 *       to the value of this string and returns the result as a string.</LI>
 *   <LI>assign(integer intValue) -- Sets the value of this string to the string
 *       representation of the provided integer value.  This method does not
 *       return a value.</LI>
 *   <LI>assign(string stringValue) -- Sets the value of this string to the
 *       string value of the provided argument.  This method does not return
 *       a value.</LI>
 *   <LI>compareTo(string stringValue, boolean ignoreCase) -- Performs a
 *       lexicographical comparison between the value of this string and the
 *       value of the provided string.  If this string comes before the provided
 *       string, then a negative integer will be returned.  If this string comes
 *       after the provided string, then a positive integer will be returned.
 *       If the two strings are equal, then a value of zero will be
 *       returned.</LI>
 *   <LI>contains(string substring, boolean ignoreCase) -- Indicates whether
 *       this string contains the specified substring and returns the result as
 *       a Boolean value.</LI>
 *   <LI>endsWith(string substring, boolean ignoreCase) -- Indicates whether
 *       this string ends with the specified substring and returns the result as
 *       a Boolean value.</LI>
 *   <LI>equals(string stringValue, boolean ignoreCase) -- Indicates whether
 *       this string value is equal to the provided string value and returns the
 *       result as a Boolean value.</LI>
 *   <LI>indexOf(string substring, boolean ignoreCase) -- Indicates the position
 *       of the first occurrence of the specified substring in this string,
 *       returning the result as an integer value.  If the specified substring
 *       is not found then a value of -1 will be returned.</LI>
 *   <LI>indexOf(string substring, int startPos, boolean ignoreCase) --
 *       Indicates the position of the first occurrence specified substring
 *       starting at the indicated position, returning the result as an integer
 *       value.  If the specified substring is not found, then a value of -1
 *       will be returned.</LI>
 *   <LI>isNull() -- Indicates whether this string variable has not been
 *       assigned a value.</LI>
 *   <LI>lastIndexOf(string substring, boolean ignoreCase) -- Indicates the
 *       position of the last occurrence of the specified substring in this
 *       string, returning the result as an integer value.  If the specified
 *       substring is not found then a value of -1 will be returned.</LI>
 *   <LI>lastIndexOf(string substring, int startPos, boolean ignoreCase) --
 *       Indicates the position of the last occurrence specified substring at or
 *       before the indicated position, returning the result as an integer
 *       value.  If the specified substring is not found, then a value of -1
 *       will be returned.</LI>
 *   <LI>length() -- Determines the length of this string value and returns the
 *       result as an integer.</LI>
 *   <LI>notNull() -- Indicates whether this string variable has been assigned
 *       a value.</LI>
 *   <LI>splitAt(string delimiter) -- Splits this string into a string array
 *       using the specified delimiter.  Multiple consecutive delimiters will be
 *       seen as separate elements.
 *   <LI>splitAtTabs() -- Splits this string into a string array using tab
 *       characters as the delimiter.  Multiple consecutive tab characters will
 *       be seen as separate delimiters.</LI>
 *   <LI>startsWith(string stringValue, boolean ignoreCase) -- Indicates whether
 *       this string starts with the specified substring and returns the result
 *       as a Boolean value.</LI>
 *   <LI>substring(int startPos) -- Retrieves a substring of this string,
 *       starting at the specified position and continuing to the end of the
 *       string.</LI>
 *   <LI>substring(int startPos, int endPos) -- Retrieves a substring of this
 *       string, starting at the specified start position and ending at the
 *       specified end position.</LI>
 *   <LI>toLowerCase() -- Retrieves a string value that is the same as this
 *       value but all characters converted to lowercase.</LI>
 *   <LI>toUpperCase() -- Retrieves a string value that is the same as this
 *       value but all characters converted to uppercase.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class StringVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of string variables.
   */
  public static final String STRING_VARIABLE_TYPE = "string";



  /**
   * The name of the method that will be used to append strings together.
   */
  public static final String APPEND_METHOD_NAME = "append";



  /**
   * The method number for the "append" method.
   */
  public static final int APPEND_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to assign a value to the variable.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the first "assign" method.
   */
  public static final int ASSIGN_1_METHOD_NUMBER = 1;



  /**
   * The method number for the second "assign" method.
   */
  public static final int ASSIGN_2_METHOD_NUMBER = 2;



  /**
   * The name of the method that will perform lexicographical comparisons of
   * strings.
   */
  public static final String COMPARE_TO_METHOD_NAME = "compareto";



  /**
   * The method number for the "compareTo" method.
   */
  public static final int COMPARE_TO_METHOD_NUMBER = 3;



  /**
   * The name of the method that will determine whether this string contains a
   * specified substring.
   */
  public static final String CONTAINS_METHOD_NAME = "contains";



  /**
   * The method number for the "contains" method.
   */
  public static final int CONTAINS_METHOD_NUMBER = 4;



  /**
   * The name of the method that will determine whether this string ends with a
   * specified substring.
   */
  public static final String ENDS_WITH_METHOD_NAME = "endswith";



  /**
   * The method number for the "endsWith" method.
   */
  public static final int ENDS_WITH_METHOD_NUMBER = 5;



  /**
   * The name of the method that will determine whether two strings are equal.
   * specified substring.
   */
  public static final String EQUALS_METHOD_NAME = "equals";



  /**
   * The method number for the "equals" method.
   */
  public static final int EQUALS_METHOD_NUMBER = 6;



  /**
   * The name of the method that may be used to determine the position of a
   * specified substring.
   */
  public static final String INDEX_OF_METHOD_NAME = "indexof";



  /**
   * The method number for the first "indexOf" method.
   */
  public static final int INDEX_OF_1_METHOD_NUMBER = 7;



  /**
   * The method number for the second "indexOf" method.
   */
  public static final int INDEX_OF_2_METHOD_NUMBER = 8;



  /**
   * The name of the method that will determine whether this string has not been
   * assigned a value.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the isNull method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 9;



  /**
   * The name of the method that may be used to determine the position of the
   * last occurrence of a specified substring.
   */
  public static final String LAST_INDEX_OF_METHOD_NAME = "lastindexof";



  /**
   * The method number for the first "lastIndexOf" method.
   */
  public static final int LAST_INDEX_OF_1_METHOD_NUMBER = 10;



  /**
   * The method number for the second "lastIndexOf" method.
   */
  public static final int LAST_INDEX_OF_2_METHOD_NUMBER = 11;



  /**
   * The name of the method that will determine the length of this string.
   */
  public static final String LENGTH_METHOD_NAME = "length";



  /**
   * The method number for the "length" method.
   */
  public static final int LENGTH_METHOD_NUMBER = 12;



  /**
   * The name of the method that will determine whether this string has been
   * assigned a value.
   */
  public static final String NOT_NULL_METHOD_NAME = "notnull";



  /**
   * The method number for the "notNull" method.
   */
  public static final int NOT_NULL_METHOD_NUMBER = 13;



  /**
   * The name of the method that will be used to split this string at a
   * specified delimiter.
   */
  public static final String SPLIT_AT_METHOD_NAME = "splitat";



  /**
   * The method number for the "splitAt" method.
   */
  public static final int SPLIT_AT_METHOD_NUMBER = 14;



  /**
   * The name of the method that will be used to split this string at tab
   * characters.
   */
  public static final String SPLIT_AT_TABS_METHOD_NAME = "splitattabs";



  /**
   * The method number for the "splitAtTabs" method.
   */
  public static final int SPLIT_AT_TABS_METHOD_NUMBER = 15;



  /**
   * The name of the method that will determine whether this string starts with
   * a specified substring.
   */
  public static final String STARTS_WITH_METHOD_NAME = "startswith";



  /**
   * The method number for the "startsWith" method.
   */
  public static final int STARTS_WITH_METHOD_NUMBER = 16;



  /**
   * The name of the method that will retrieve a substring of the overall string
   * value.
   */
  public static final String SUBSTRING_METHOD_NAME = "substring";



  /**
   * The method number for the first "substring" method.
   */
  public static final int SUBSTRING_1_METHOD_NUMBER = 17;



  /**
   * The method number for the first "second" method.
   */
  public static final int SUBSTRING_2_METHOD_NUMBER = 18;



  /**
   * The name of the method that will convert the string to all lowercase.
   */
  public static final String TO_LOWERCASE_METHOD_NAME = "tolowercase";



  /**
   * The method number for the "toLowerCase" method.
   */
  public static final int TO_LOWERCASE_METHOD_NUMBER = 19;



  /**
   * The name of the method that will convert the string to all uppercase.
   */
  public static final String TO_UPPERCASE_METHOD_NAME = "touppercase";



  /**
   * The method number for the "toUpperCase" method.
   */
  public static final int TO_UPPERCASE_METHOD_NUMBER = 20;



  /**
   * The set of methods associated with string variables.
   */
  public static final Method[] STRING_VARIABLE_METHODS = new Method[]
  {
    new Method(APPEND_METHOD_NAME, new String[] { STRING_VARIABLE_TYPE },
               STRING_VARIABLE_TYPE),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME, new String[] { STRING_VARIABLE_TYPE },
               null),
    new Method(COMPARE_TO_METHOD_NAME, new String[] { STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CONTAINS_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(ENDS_WITH_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(EQUALS_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(INDEX_OF_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(INDEX_OF_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(LAST_INDEX_OF_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(LAST_INDEX_OF_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(LENGTH_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(NOT_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SPLIT_AT_METHOD_NAME,  new String[] { STRING_VARIABLE_TYPE },
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(SPLIT_AT_TABS_METHOD_NAME,  new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(STARTS_WITH_METHOD_NAME,
               new String[] { STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SUBSTRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               STRING_VARIABLE_TYPE),
    new Method(SUBSTRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               STRING_VARIABLE_TYPE),
    new Method(TO_LOWERCASE_METHOD_NAME, new String[0], STRING_VARIABLE_TYPE),
    new Method(TO_UPPERCASE_METHOD_NAME, new String[0], STRING_VARIABLE_TYPE)
  };



  // The String value associated with this variable.
  private String stringValue;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public StringVariable()
         throws ScriptException
  {
    // Initialize the string value to null.
    stringValue = null;
  }



  /**
   * Creates a new string variable with the provided value.
   *
   * @param  stringValue  The value to use for this String variable.
   */
  public StringVariable(String stringValue)
  {
    this.stringValue = stringValue;
  }



  /**
   * Retrieves the string value associated with this variable.
   *
   * @return  The string value associated with this variable.
   */
  public String getStringValue()
  {
    return stringValue;
  }



  /**
   * Retrieves the string value associated with this variable, converted to
   * lowercase.
   *
   * @return  The string value associated with this variable in lowercase form.
   */
  public String toLowerCase()
  {
    return stringValue.toLowerCase();
  }



  /**
   * Specifies the string value for this variable.
   *
   * @param  stringValue  The string value for this variable.
   */
  public void setStringValue(String stringValue)
  {
    this.stringValue = stringValue;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return STRING_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return STRING_VARIABLE_METHODS;
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
    for (int i=0; i < STRING_VARIABLE_METHODS.length; i++)
    {
      if (STRING_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < STRING_VARIABLE_METHODS.length; i++)
    {
      if (STRING_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
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
    for (int i=0; i < STRING_VARIABLE_METHODS.length; i++)
    {
      if (STRING_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
      {
        return STRING_VARIABLE_METHODS[i].getReturnType();
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
      case APPEND_METHOD_NUMBER:
        // Get the value of the string argument.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        String value = sv.getStringValue();

        // Append the strings and return the result as a string variable.
        return new StringVariable(stringValue + value);
      case ASSIGN_1_METHOD_NUMBER:
        // Get the integer value, use it for this string value, and don't
        // return anything.
        IntegerVariable iv = (IntegerVariable) arguments[0].getArgumentValue();
        int intValue = iv.getIntValue();
        this.stringValue = String.valueOf(intValue);
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Get the string value, use it for this string value, and don't
        // return anything.
        sv = (StringVariable) arguments[0].getArgumentValue();
        this.stringValue = sv.stringValue;
        return null;
      case COMPARE_TO_METHOD_NUMBER:
        // Get the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.stringValue;

        // Get the Boolean argument.
        BooleanVariable bv = (BooleanVariable) arguments[1].getArgumentValue();
        boolean ignoreCase = bv.getBooleanValue();

        // Perform the comparison and return the integer result.
        String compareValue = stringValue;
        if (ignoreCase)
        {
          value = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new IntegerVariable(compareValue.compareTo(value));
      case CONTAINS_METHOD_NUMBER:
        // Get the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.stringValue;

        // Get the Boolean argument.
        bv = (BooleanVariable) arguments[1].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Perform the comparison and return the integer result.
        compareValue = stringValue;
        if (ignoreCase)
        {
          value = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new BooleanVariable(compareValue.contains(value));
      case ENDS_WITH_METHOD_NUMBER:
        // Get the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.stringValue;

        // Get the Boolean argument.
        bv = (BooleanVariable) arguments[1].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Perform the comparison and return the integer result.
        compareValue = stringValue;
        if (ignoreCase)
        {
          value = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new BooleanVariable(compareValue.endsWith(value));
      case EQUALS_METHOD_NUMBER:
        // Get the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.stringValue;

        // Get the Boolean argument.
        bv = (BooleanVariable) arguments[1].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Perform the comparison and return the integer result.
        compareValue = stringValue;
        if (ignoreCase)
        {
          value = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new BooleanVariable(compareValue.equals(value));
      case INDEX_OF_1_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        bv = (BooleanVariable) arguments[1].getArgumentValue();

        value        = sv.stringValue;
        compareValue = stringValue;
        if (bv.getBooleanValue())
        {
          value        = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new IntegerVariable(compareValue.indexOf(value));
      case INDEX_OF_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        iv = (IntegerVariable) arguments[1].getArgumentValue();
        bv = (BooleanVariable) arguments[2].getArgumentValue();

        value        = sv.stringValue;
        compareValue = stringValue;
        if (bv.getBooleanValue())
        {
          value        = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new IntegerVariable(compareValue.indexOf(value,
                                                        iv.getIntValue()));
      case IS_NULL_METHOD_NUMBER:
        // Make the determination and return the Boolean result.
        return new BooleanVariable(stringValue == null);
      case LAST_INDEX_OF_1_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        bv = (BooleanVariable) arguments[1].getArgumentValue();

        value        = sv.stringValue;
        compareValue = stringValue;
        if (bv.getBooleanValue())
        {
          value        = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new IntegerVariable(compareValue.lastIndexOf(value));
      case LAST_INDEX_OF_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        iv = (IntegerVariable) arguments[1].getArgumentValue();
        bv = (BooleanVariable) arguments[2].getArgumentValue();

        value        = sv.stringValue;
        compareValue = stringValue;
        if (bv.getBooleanValue())
        {
          value        = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new IntegerVariable(compareValue.lastIndexOf(value,
                                                            iv.getIntValue()));
      case LENGTH_METHOD_NUMBER:
        // Make the determination and return the integer result.
        return new IntegerVariable(stringValue.length());
      case NOT_NULL_METHOD_NUMBER:
        // Make the determination and return the Boolean result.
        return new BooleanVariable(stringValue != null);
      case SPLIT_AT_METHOD_NUMBER:
        // Get the delimiter to use.
        sv = (StringVariable) arguments[0].getArgumentValue();
        String delimiter = sv.getStringValue();

        // Create a string array variable and iterate through this string value
        // to split it at the specified delimiter.
        StringArrayVariable sav = new StringArrayVariable();
        int startPos = 0;
        int delimiterPos;
        while ((delimiterPos = stringValue.indexOf(delimiter, startPos)) >= 0)
        {
          sav.addStringValue(stringValue.substring(startPos, delimiterPos));
          startPos = delimiterPos + delimiter.length();
        }
        sav.addStringValue(stringValue.substring(startPos));
        return sav;
      case SPLIT_AT_TABS_METHOD_NUMBER:
        // Get the delimiter to use.
        delimiter = "\t";

        // Create a string array variable and iterate through this string value
        // to split it at the specified delimiter.
        sav = new StringArrayVariable();
        startPos = 0;
        while ((delimiterPos = stringValue.indexOf(delimiter, startPos)) >= 0)
        {
          sav.addStringValue(stringValue.substring(startPos, delimiterPos));
          startPos = delimiterPos + delimiter.length();
        }
        sav.addStringValue(stringValue.substring(startPos));
        return sav;
      case STARTS_WITH_METHOD_NUMBER:
        // Get the string argument.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.stringValue;

        // Get the Boolean argument.
        bv = (BooleanVariable) arguments[1].getArgumentValue();
        ignoreCase = bv.getBooleanValue();

        // Perform the comparison and return the integer result.
        compareValue = stringValue;
        if (ignoreCase)
        {
          value = value.toLowerCase();
          compareValue = compareValue.toLowerCase();
        }
        return new BooleanVariable(compareValue.startsWith(value));
      case SUBSTRING_1_METHOD_NUMBER:
        // Return the requested substring.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        return new StringVariable(stringValue.substring(iv.getIntValue()));
      case SUBSTRING_2_METHOD_NUMBER:
        // Return the requested substring.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[1].getArgumentValue();
        return new StringVariable(stringValue.substring(iv.getIntValue(),
                                                        iv2.getIntValue()));
      case TO_LOWERCASE_METHOD_NUMBER:
        // Return the string result
        return new StringVariable(stringValue.toLowerCase());
      case TO_UPPERCASE_METHOD_NUMBER:
        // Return the string result
        return new StringVariable(stringValue.toUpperCase());
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
    if (! argument.getArgumentType().equals(STRING_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                STRING_VARIABLE_TYPE + " rejected.");
    }

    StringVariable sv = (StringVariable) argument.getArgumentValue();
    stringValue = sv.stringValue;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (stringValue == null)
    {
      return "null";
    }

    return stringValue;
  }
}

