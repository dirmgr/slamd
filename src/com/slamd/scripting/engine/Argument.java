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



/**
 * This interface defines a set of methods that are required to be implemented
 * by any script element that may be a parameter to a script method.
 *
 *
 * @author   Neil A. Wilson
 */
public interface Argument
{
  /**
   * Retrieves the name of the data type associated with this argument.
   *
   * @return  The name of the data type associated with this argument.
   */
  public String getArgumentType();



  /**
   * Retrieves the value of this argument.
   *
   * @return  The value of this argument.
   *
   * @throws  ScriptException  If a problem occurs while determining the value
   *                           of this argument.
   */
  public Variable getArgumentValue()
         throws ScriptException;



  /**
   * Retrieves a string representation of this argument in a form appropriate
   * for inclusion in a script.
   *
   * @return  A string representation of this argument in a form appropriate for
   *          inclusion in a script.
   */
  public String getArgumentAsString();



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   *
   * @throws  ScriptException  If a problem occurs while retrieving the value as
   *                           a string.
   */
  public String getValueAsString()
         throws ScriptException;
}

