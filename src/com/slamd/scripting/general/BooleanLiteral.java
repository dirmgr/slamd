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
 * This class defines a Boolean literal value, which is simply a Boolean value
 * that occurs in a script file without being encapsulated in a variable.
 *
 *
 * @author   Neil A. Wilson
 */
public class BooleanLiteral
       implements Argument
{
  /**
   * The token that will be used for a Boolean value of "true".
   */
  public static final String BOOLEAN_TRUE_VALUE = "true";



  /**
   * The token that will be used for a Boolean value of "false".
   */
  public static final String BOOLEAN_FALSE_VALUE = "false";



  // The boolean value associated with this Boolean literal.
  private boolean booleanValue;



  /**
   * Creates a new Boolean literal with the specified value.
   *
   * @param  booleanValue  The value to use for this Boolean literal.
   */
  public BooleanLiteral(boolean booleanValue)
  {
    this.booleanValue = booleanValue;
  }



  /**
   * Retrieves the name of the data type associated with this Boolean literal.
   *
   * @return  The name of the data type associated with this Boolean literal.
   */
  public String getArgumentType()
  {
    return BooleanVariable.BOOLEAN_VARIABLE_TYPE;
  }



  /**
   * Retrieves the value of this Boolean literal encapsulated in a Boolean
   * variable.
   *
   * @return  The value of this Boolean literal encapsulated in a Boolean
   *          variable.
   *
   * @throws  ScriptException  If a problem occurs while determining the value
   *                           of this Boolean literal.
   */
  public Variable getArgumentValue()
         throws ScriptException
  {
    BooleanVariable bv = new BooleanVariable();
    bv.setBooleanValue(booleanValue);
    return bv;
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
    if (booleanValue)
    {
      return BOOLEAN_TRUE_VALUE;
    }
    else
    {
      return BOOLEAN_FALSE_VALUE;
    }
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (booleanValue)
    {
      return BOOLEAN_TRUE_VALUE;
    }
    else
    {
      return BOOLEAN_FALSE_VALUE;
    }
  }
}

