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



import com.slamd.job.JobClass;
import com.slamd.stat.StatTracker;
import com.slamd.scripting.general.BooleanLiteral;



/**
 * This class defines a generic variable that may be defined in an LDAP script.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class Variable
       implements Argument
{
  // The name of the variable.
  private String name;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   */
  public Variable()
  {
    // No implementation required.
  }



  /**
   * Creates a generic variable with a name but no value.
   *
   * @param  name  The name to use for this variable.
   */
  public Variable(String name)
  {
    this.name  = name;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  public abstract String getVariableTypeName();



  /**
   * Sets the name of this variable.
   *
   * @param  name  The name to use for this variable.
   */
  public void setName(String name)
  {
    this.name = name;
  }




  /**
   * Retrieves the name of this variable.
   *
   * @return  The name of this variable.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Indicates whether the specified string may be used as a variable or method
   * identifier in a SLAMD script.  All valid identifiers must start with an
   * alphabetic character and must contain only alphabetic characters, numeric
   * digits, or underscore characters.
   *
   * @param  identifierName  The name of the identifier for which to make the
   *                         determination.
   *
   * @return  <CODE>true</CODE> if the provided name may be used as a valid
   *          identifier, or <CODE>false</CODE> if not.
   */
  public static boolean isValidIdentifier(String identifierName)
  {
    // We can't allow an empty identifier name.
    if ((identifierName == null) || (identifierName.length() == 0))
    {
      return false;
    }


    // Convert the identifier name to all lowercase.
    identifierName = identifierName.toLowerCase();


    // Make sure that the provided identifier name is not one of the reserved
    // words.
    if (ScriptParser.isReservedWord(identifierName))
    {
      return false;
    }


    // Make sure that the provided identifier name is not one of the boolean
    // literal values.
    if (identifierName.equals(BooleanLiteral.BOOLEAN_TRUE_VALUE) ||
        identifierName.equals(BooleanLiteral.BOOLEAN_FALSE_VALUE))
    {
      return false;
    }


    // Convert the provided name to a character array for easier comparison.
    char[] nameChars = identifierName.toLowerCase().toCharArray();


    // The first character must be alphabetic.
    if (! ((nameChars[0] >= 'a') && (nameChars[0] <= 'z')))
    {
      return false;
    }


    // Now iterate through the remaining characters and check to make sure they
    // are alphabetic, numeric, or underscore characters.
    for (int i=1; i < nameChars.length; i++)
    {
      if (! (((nameChars[i] >= 'a') && (nameChars[i] <= 'z')) ||
             ((nameChars[i] >= '0') && (nameChars[i] <= '9')) ||
             (nameChars[i] == '_')))
      {
        return false;
      }
    }


    // If we got here, then there doesn't seem to be anything wrong with it.
    return true;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  public abstract Method[] getMethods();



  /**
   * Indicates whether this variable type has a method with the specified name.
   *
   * @param  methodName  The name of the method.
   *
   * @return  <CODE>true</CODE> if this variable has a method with the specified
   *          name, or <CODE>false</CODE> if it does not.
   */
  public abstract boolean hasMethod(String methodName);



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
  public abstract int getMethodNumber(String methodName,
                                      String[] argumentTypes);



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
  public abstract String getReturnTypeForMethod(String methodName,
                                                String[] argumentTypes);



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
  public abstract Variable executeMethod(int lineNumber, int methodNumber,
                                         Argument[] arguments)
         throws ScriptException;



  /**
   * Retrieves the set of stat trackers that are maintained for this variable
   * type.  Note that most variables should not maintain stat trackers -- only
   * those that perform a function for which it is worthwhile to report
   * statistics back to the SLAMD server.  Also note that if this method is
   * implemented by a variable type, then the <CODE>startStatTrackers()</CODE>
   * and <CODE>stopStatTrackers()</CODE> methods must also be implemented to
   * make sure that they are handled properly.
   *
   * @return  The set of stat trackers that are maintained for this variable
   *          type.
   */
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[0];
  }



  /**
   * Notifies all the stat trackers associated with this variable type that they
   * should start collecting statistics.
   *
   * @param  jobThread  The job thread for which the statistics will be
   *                    gathered.
   */
  public void startStatTrackers(JobClass jobThread)
  {
    return;
  }



  /**
   * Notifies all the stat trackers associated with this variable type that they
   * should start collecting statistics.
   */
  public void stopStatTrackers()
  {
    return;
  }



  /**
   * Retrieves the name of the data type associated with this argument.
   *
   * @return  The name of the data type associated with this argument.
   */
  public String getArgumentType()
  {
    return getVariableTypeName();
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
    return this;
  }



  /**
   * Assigns the value of the provided argument to this variable.  The value of
   * the provided argument must be of the same type as this variable.
   *
   * @param  argument  The argument whose value should be assigned to this
   *                   variable.
   *
   * @throws  ScriptException  If there is a problem while performing the
   *                           assignment.
   */
  public abstract void assign(Argument argument)
         throws ScriptException;



  /**
   * Retrieves a string representation of this argument in a form appropriate
   * for inclusion in a script.
   *
   * @return  A string representation of this argument in a form appropriate for
   *          inclusion in a script.
   */
  public String getArgumentAsString()
  {
    return name;
  }
}

