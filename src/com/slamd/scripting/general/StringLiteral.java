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
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a string literal value, which is simply a string value
 * that occurs in a script file without being encapsulated in a variable.
 *
 *
 * @author   Neil A. Wilson
 */
public class StringLiteral
       implements Argument
{
  // The string value associated with this string literal.
  private String stringValue;



  /**
   * Creates a new string literal with the specified value.
   *
   * @param  stringValue  The value to use for this string literal.
   */
  public StringLiteral(String stringValue)
  {
    this.stringValue = stringValue;
  }



  /**
   * Retrieves the name of the data type associated with this string literal.
   *
   * @return  The name of the data type associated with this string literal.
   */
  public String getArgumentType()
  {
    return StringVariable.STRING_VARIABLE_TYPE;
  }



  /**
   * Retrieves the value of this string literal encapsulated in a string
   * variable.
   *
   * @return  The value of this string literal encapsulated in a string
   *          variable.
   *
   * @throws  ScriptException  If a problem occurs while determining the value
   *                           of this string literal.
   */
  public Variable getArgumentValue()
         throws ScriptException
  {
    StringVariable sv = new StringVariable();
    sv.setStringValue(stringValue);
    return sv;
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
    return stringValue;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return stringValue;
  }
}

