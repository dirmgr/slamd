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
 * This class defines an integer literal value, which is simply an integer value
 * that occurs in a script file without being encapsulated in a variable.
 *
 *
 * @author   Neil A. Wilson
 */
public class IntegerLiteral
       implements Argument
{
  // The int value associated with this integer literal.
  private int intValue;



  /**
   * Creates a new integer literal with the specified value.
   *
   * @param  intValue  The value to use for the integer literal.
   */
  public IntegerLiteral(int intValue)
  {
    this.intValue   = intValue;
  }



  /**
   * Retrieves the name of the data type associated with this integer literal.
   *
   * @return  The name of the data type associated with this integer literal.
   */
  public String getArgumentType()
  {
    return IntegerVariable.INTEGER_VARIABLE_TYPE;
  }



  /**
   * Retrieves the value of this integer literal encapsulated in an integer
   * variable.
   *
   * @return  The value of this integer literal encapsulated in an integer
   *          variable.
   *
   * @throws  ScriptException  If a problem occurs while determining the value
   *                           of this Integer literal.
   */
  public Variable getArgumentValue()
         throws ScriptException
  {
    IntegerVariable iv = new IntegerVariable();
    iv.setIntValue(intValue);
    return iv;
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
    return String.valueOf(intValue);
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

