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

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPModification;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable intended to store an LDAP modification.  LDAP
 * modifications have the following methods:
 *
 * <UL>
 *   <LI>addValue(string attributeValue) -- Adds the specified attribute value
 *       to this modification set.  This method does not return a value.</LI>
 *   <LI>assign(int modType, string attributeName) -- Initializes this
 *       modification with the specified modType and attribute name, and no
 *       values.  This method does not return a value.</LI>
 *   <LI>assign(int modType, string attributeName, string attributeValue) --
 *       Initializes this modification with the specified modType, attribute
 *       name, and attribute value.  This method does not return a value.</LI>
 *   <LI>assign(int modType, string attributeName, stringarray attributeValues)
 *       -- Initializes this modification with the specified modType, attribute
 *       name, and set of values.  This method does not return a value.</LI>
 *   <LI>getModType() -- Retrieves the modification type associated with this
 *       modification as an integer value.</LI>
 *   <LI>getName() -- Retrieves the name of the attribute at which this
 *       modification is targeted as a string value.</LI>
 *   <LI>getValues() -- Retrieves the set of values included in this
 *       modification as a string array value.  If there are no values (e.g.,
 *       removing an attribute) then an empty string array will be returned.
 *   <LI>isNull() -- Indicates whether this modification has been
 *       initialized.</LI>
 *   <LI>modTypeAdd() -- Retrieves the integer modification type that should be
 *       used for adding new attributes or attribute values to an entry.</LI>
 *   <LI>modTypeDelete() -- Retrieves the integer modification type that should
 *       be used for removing attributes or attribute values from an entry.</LI>
 *   <LI>modTypeReplace() -- Retrieves the integer modification type that should
 *       be used for replacing attribute values in an entry.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class LDAPModificationVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of LDAP modification
   * variables.
   */
  public static final String LDAP_MODIFICATION_VARIABLE_TYPE =
       "ldapmodification";



  /**
   * The name of the method that will be used to add a new value to this
   * modification.
   */
  public static final String ADD_VALUE_METHOD_NAME = "addvalue";



  /**
   * The method number for the "addValue" method.
   */
  public static final int ADD_VALUE_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to initialize this LDAP
   * modification.
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
   * The method number for the third "assign" method.
   */
  public static final int ASSIGN_3_METHOD_NUMBER = 3;



  /**
   * The name of the method that will be used to retrieve the modification type
   * for this modification.
   */
  public static final String GET_MOD_TYPE_METHOD_NAME = "getmodtype";



  /**
   * The method number for the "getModType" method.
   */
  public static final int GET_MOD_TYPE_METHOD_NUMBER = 4;



  /**
   * The name of the method that will be used to get the name of the LDAP
   * attribute.
   */
  public static final String GET_NAME_METHOD_NAME = "getname";



  /**
   * The method number for the "getName" method.
   */
  public static final int GET_NAME_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to get the set of values for the
   * LDAP attribute.
   */
  public static final String GET_VALUES_METHOD_NAME = "getvalues";



  /**
   * The method number for the "getValues" method.
   */
  public static final int GET_VALUES_METHOD_NUMBER = 6;



  /**
   * The name of the method that will be used to determine whether this LDAP
   * modification has been initialized.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 7;



  /**
   * The name of the method that will be used to retrieve the modification type
   * to use when adding new a attribute or values.
   */
  public static final String MOD_TYPE_ADD_METHOD_NAME = "modtypeadd";



  /**
   * The method number for the "modTypeAdd" method.
   */
  public static final int MOD_TYPE_ADD_METHOD_NUMBER = 8;



  /**
   * The name of the method that will be used to retrieve the modification type
   * to use when removing an attribute or values.
   */
  public static final String MOD_TYPE_DELETE_METHOD_NAME = "modtypedelete";



  /**
   * The method number for the "modTypeDelete" method.
   */
  public static final int MOD_TYPE_DELETE_METHOD_NUMBER = 9;



  /**
   * The name of the method that will be used to retrieve the modification type
   * to use when replacing attribute values.
   */
  public static final String MOD_TYPE_REPLACE_METHOD_NAME = "modtypereplace";



  /**
   * The method number for the "modTypeReplace" method.
   */
  public static final int MOD_TYPE_REPLACE_METHOD_NUMBER = 10;



  /**
   * The set of methods associated with LDAP modification variables.
   */
  public static final Method[] LDAP_MODIFICATION_VARIABLE_METHODS =
       new Method[]
  {
    new Method(ADD_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(GET_MOD_TYPE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_NAME_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_VALUES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(MOD_TYPE_ADD_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(MOD_TYPE_DELETE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(MOD_TYPE_REPLACE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE)
  };



  // The set of values associated with this array.
  ArrayList<String> attributeValues;

  // The modification type for this attribute.
  int modType;

  // The name of this LDAP attribute.
  String attributeName;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   */
  public LDAPModificationVariable()
  {
    attributeName   = null;
    modType         = -1;
    attributeValues = new ArrayList<String>();
  }



  /**
   * Retrieves the name of the LDAP attribute associated with this modification.
   *
   * @return  The name of the LDAP attribute associated with this modification.
   */
  public String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Specifies the name for the LDAP attribute associated with this
   * modification.
   *
   * @param  attributeName  The name for the LDAP attribute associated with this
   *                        modification.
   */
  public void setAttributeName(String attributeName)
  {
    this.attributeName = attributeName;
  }



  /**
   * Retrieves the set of values for the LDAP attribute associated with this
   * modification.
   *
   * @return  The set of values for the LDAP attribute associated with this
   *          modification.
   */
  public String[] getAttributeValues()
  {
    String[] values = new String[attributeValues.size()];
    attributeValues.toArray(values);
    return values;
  }



  /**
   * Specifies the set of values for the LDAP attribute associated with this
   * modification.
   *
   * @param  values  The set of values for the LDAP attribute associated with
   *                 this modification.
   */
  public void setAttributeValues(String[] values)
  {
    attributeValues.clear();
    for (int i=0; i < values.length; i++)
    {
      attributeValues.add(values[i]);
    }
  }



  /**
   * Adds the provided attribute value to the modification as long as it does
   * not already exist.
   *
   * @param  value  The attribute value to add to this modification.
   */
  public void addValue(String value)
  {
    for (int i=0; i < attributeValues.size(); i++)
    {
      if (value.equalsIgnoreCase(attributeValues.get(i)))
      {
        // The value already exists, so don't do anything.
        return;
      }
    }

    // The value was not in the set, so it is safe to add it.
    attributeValues.add(value);
  }



  /**
   * Retrieves this modification as an LDAPModification object.
   *
   * @return  This modification in the form of an LDAPModification object.
   */
  public LDAPModification toLDAPModification()
  {
    return new LDAPModification(modType,
                                new LDAPAttribute(attributeName,
                                                  getAttributeValues()));
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return LDAP_MODIFICATION_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return LDAP_MODIFICATION_VARIABLE_METHODS;
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
    for (int i=0; i < LDAP_MODIFICATION_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_MODIFICATION_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < LDAP_MODIFICATION_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_MODIFICATION_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < LDAP_MODIFICATION_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_MODIFICATION_VARIABLE_METHODS[i].hasSignature(methodName,
                                                                 argumentTypes))
      {
        return LDAP_MODIFICATION_VARIABLE_METHODS[i].getReturnType();
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
        // Get the value to add, add it to the value set, and don't return a
        // value.
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        addValue(sv1.getStringValue());
        return null;
      case ASSIGN_1_METHOD_NUMBER:
        // Get the modification type and attribute name and use them to
        // initialize this modification.  Don't return a value.
        IntegerVariable iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        sv1 = (StringVariable)  arguments[1].getArgumentValue();
        this.modType       = iv1.getIntValue();
        this.attributeName = sv1.getStringValue();
        attributeValues    = new ArrayList<String>();
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Get the modification type, attribute name, and value.  Use them to
        // initialize this modification.  Don't return a value.
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        sv1 = (StringVariable)  arguments[1].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[2].getArgumentValue();
        this.modType       = iv1.getIntValue();
        this.attributeName = sv1.getStringValue();
        attributeValues    = new ArrayList<String>();
        if (sv2.getStringValue() != null)
        {
          attributeValues.add(sv2.getStringValue());
        }
        return null;
      case ASSIGN_3_METHOD_NUMBER:
        // Get the modification type, attribute name, and values.  Use them to
        // initialize this modification.  Don't return a value.
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        sv1 = (StringVariable)  arguments[1].getArgumentValue();
        StringArrayVariable sav = (StringArrayVariable)
                                  arguments[2].getArgumentValue();
        this.modType       = iv1.getIntValue();
        this.attributeName = sv1.getStringValue();
        attributeValues    = new ArrayList<String>();
        String[] values = sav.getStringValues();
        for (int i=0; i < values.length; i++)
        {
          attributeValues.add(values[i]);
        }
        return null;
      case GET_MOD_TYPE_METHOD_NUMBER:
        // Return the mod type as an integer value.
        return new IntegerVariable(modType);
      case GET_NAME_METHOD_NUMBER:
        // Return the attribute name as a string value.
        return new StringVariable(attributeName);
      case GET_VALUES_METHOD_NUMBER:
        // Return the attribute values as a string array.
        return new StringArrayVariable(getAttributeValues());
      case IS_NULL_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(attributeName == null);
      case MOD_TYPE_ADD_METHOD_NUMBER:
        // Return the result as an integer value.
        return new IntegerVariable(LDAPModification.ADD);
      case MOD_TYPE_DELETE_METHOD_NUMBER:
        // Return the result as an integer value.
        return new IntegerVariable(LDAPModification.DELETE);
      case MOD_TYPE_REPLACE_METHOD_NUMBER:
        // Return the result as an integer value.
        return new IntegerVariable(LDAPModification.REPLACE);
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
    if (! argument.getArgumentType().equals(LDAP_MODIFICATION_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                LDAP_MODIFICATION_VARIABLE_TYPE + " rejected.");
    }

    LDAPModificationVariable lmv = (LDAPModificationVariable)
                                   argument.getArgumentValue();
    modType         = lmv.modType;
    attributeName   = lmv.attributeName;
    attributeValues = lmv.attributeValues;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if ((attributeName == null) || (attributeName.length() == 0))
    {
      return "null";
    }

    String typeStr;
    switch (modType)
    {
      case LDAPModification.ADD:
        typeStr = "add ";
        break;
      case LDAPModification.DELETE:
        typeStr = "delete ";
        break;
      case LDAPModification.REPLACE:
        typeStr = "replace ";
        break;
      default:
        return "invalid modification type";
    }


    StringBuilder buffer    = new StringBuilder();
    int          numValues = attributeValues.size();
    switch (numValues)
    {
      case 0:
        buffer.append(typeStr);
        buffer.append(attributeName);
        buffer.append(" { no values}");
        break;
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        buffer.append(typeStr);
        buffer.append(attributeName);
        buffer.append(" { \"");
        buffer.append(attributeValues.get(0));
        buffer.append('"');

        for (int i=1; i < numValues; i++)
        {
          buffer.append(", \"");
          buffer.append(attributeValues.get(i));
          buffer.append('"');
        }

        buffer.append(" }");
        break;
      default:
        buffer.append(typeStr);
        buffer.append(attributeName);
        buffer.append(" { ");
        buffer.append(numValues);
        buffer.append(" values}");
        break;
    }

    return buffer.toString();
  }
}

