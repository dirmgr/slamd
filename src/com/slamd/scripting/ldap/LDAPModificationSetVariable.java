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
package com.slamd.scripting.ldap;



import java.util.ArrayList;

import netscape.ldap.LDAPModification;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a variable intended to store LDAP modifications so that
 * they can be sent to a directory server.  An LDAP modification set has the
 * following methods:
 *
 * <UL>
 *   <LI>addModification(ldapmodification modification) -- Adds the specified
 *       LDAP modification to this modification set.</LI>
 *   <LI>removeAll() -- Removes all modifications from this modification set.
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class LDAPModificationSetVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of LDAP modification set
   * variables.
   */
  public static final String LDAP_MODIFICATION_SET_VARIABLE_TYPE =
       "ldapmodificationset";



  /**
   * The name of the method that adds a modification to this modification set.
   */
  public static final String ADD_MODIFICATION_METHOD_NAME = "addmodification";



  /**
   * The methodNumber for the "addModification" method.
   */
  public static final int ADD_MODIFICATION_METHOD_NUMBER = 0;



  /**
   * The name of the method that removes all modifications from this
   * modification set.
   */
  public static final String REMOVE_ALL_METHOD_NAME = "removeall";



  /**
   * The method number for the "removeAll" method.
   */
  public static final int REMOVE_ALL_METHOD_NUMBER = 1;



  /**
   * The set of methods associated with LDAP modification set variables.
   */
  public static final Method[] LDAP_MODIFICATION_SET_VARIABLE_METHODS =
       new Method[]
  {
    new Method(ADD_MODIFICATION_METHOD_NAME,
               new String[] { LDAPModificationVariable.
                                   LDAP_MODIFICATION_VARIABLE_TYPE }, null),
    new Method(REMOVE_ALL_METHOD_NAME, new String[0], null)
  };




  // The set of modifications stored in this modification set.
  private ArrayList<LDAPModificationVariable> modifications;




  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public LDAPModificationSetVariable()
         throws ScriptException
  {
    modifications = new ArrayList<LDAPModificationVariable>();
  }



  /**
   * Retrieves this set of LDAP modifications as an array of LDAPModification
   * objects.
   *
   * @return  This set of LDAP modifications as an array of LDAPModification
   *          objects.
   */
  public LDAPModification[] toLDAPModifications()
  {
    LDAPModification[] mods = new LDAPModification[modifications.size()];

    for (int i=0; i < mods.length; i++)
    {
      mods[i] = modifications.get(i).toLDAPModification();
    }

    return mods;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return LDAP_MODIFICATION_SET_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return LDAP_MODIFICATION_SET_VARIABLE_METHODS;
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
    for (int i=0; i < LDAP_MODIFICATION_SET_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_MODIFICATION_SET_VARIABLE_METHODS[i].getName().equals(
                                                                   methodName))
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
    for (int i=0; i < LDAP_MODIFICATION_SET_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_MODIFICATION_SET_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < LDAP_MODIFICATION_SET_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_MODIFICATION_SET_VARIABLE_METHODS[i].hasSignature(methodName,
                                                                 argumentTypes))
      {
        return LDAP_MODIFICATION_SET_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_MODIFICATION_METHOD_NUMBER:
        // Get the modification, add it to the set, and don't return a value.
        LDAPModificationVariable mod = (LDAPModificationVariable)
                                       arguments[0].getArgumentValue();
        modifications.add(mod);
        return null;
      case REMOVE_ALL_METHOD_NUMBER:
        // Remove all the modifications from the set and don't return a value.
        modifications.clear();
        return null;
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
    if (! argument.getArgumentType().equals(
               LDAP_MODIFICATION_SET_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                LDAP_MODIFICATION_SET_VARIABLE_TYPE +
                                " rejected.");
    }

    LDAPModificationSetVariable lmsv = (LDAPModificationSetVariable)
                                       argument.getArgumentValue();
    modifications = lmsv.modifications;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    int numModifications = modifications.size();

    switch (numModifications)
    {
      case 0:
        return "no modifications";
      case 1:
        return "1 modification -- " + modifications.get(0).getValueAsString();
      default:
        return numModifications + " modifications";
    }
  }
}

