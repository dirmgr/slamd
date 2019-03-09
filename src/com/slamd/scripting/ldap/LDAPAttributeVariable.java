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
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable intended to store LDAP attribute information.
 * LDAP attributes have the following methods:
 *
 * <UL>
 *   <LI>addValue(string value) -- Adds the specified string value to this LDAP
 *       attribute.  This method does not return a value.</LI>
 *   <LI>assign(ldapattribute attribute) -- Initializes this LDAP attribute with
 *       the information from the provided attribute.  This method does not
 *       return a value.</LI>
 *   <LI>assign(string name) -- Initializes this LDAP attribute with the
 *       specified name and no values.  This method does not return a
 *       value.</LI>
 *   <LI>assign(string name, string value) -- Initializes this LDAP attribute
 *       with the specified name and value.  This method does not return a
 *       value.</LI>
 *   <LI>assign(string name, stringarray values) -- Initializes this LDAP
 *       attribute with the specified name and set of values.  This method does
 *       not return a value.</LI>
 *   <LI>getName() -- Returns the name of this LDAP attribute as a string
 *       value.</LI>
 *   <LI>getValue() -- Returns the value of this LDAP attribute as a string
 *       value.  If the attribute has multiple values, then the first will be
 *       returned.  If the attribute does not have any values, then a null
 *       string will be returned.</LI>
 *   <LI>getValues() -- Returns the set of values for this LDAP attribute as a
 *       string array value.</LI>
 *   <LI>hasValue(string value) -- Returns a Boolean value that indicates
 *       whether this LDAP attribute has the specified value.</LI>
 *   <LI>isNull() -- Indicates whether this attribute variable has not been
 *       initialized.</LI>
 *   <LI>notNull() -- Indicates whether this attribute variable has been
 *       initialized.</LI>
 *   <LI>removeAllValues() -- Removes all values from this LDAP attribute.  This
 *       method does not return a value.</LI>
 *   <LI>removeValue(string value) -- Removes the specified value from this LDAP
 *       attribute.  A Boolean value is returned that indicates whether any
 *       value was actually removed.</LI>
 *   <LI>setName(string name) -- Specifies the name to use for this LDAP
 *        attribute.</LI>
 *   <LI>setValue(string value) -- Specifies the value to use for this LDAP
 *       attribute.</LI>
 *   <LI>setValues(stringarray values) -- Specifies the set of values to use
 *       for this LDAP attribute.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class LDAPAttributeVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of LDAP attribute variables.
   */
  public static final String LDAP_ATTRIBUTE_VARIABLE_TYPE = "ldapattribute";



  /**
   * The name of the method that will be used to add a new value to this array.
   */
  public static final String ADD_VALUE_METHOD_NAME = "addvalue";



  /**
   * The method number for the "addValue" method.
   */
  public static final int ADD_VALUE_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to initialize this LDAP attribute.
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
   * The method number for the fourth "assign" method.
   */
  public static final int ASSIGN_4_METHOD_NUMBER = 4;



  /**
   * The name of the method that will be used to get the name of this LDAP
   * attribute.
   */
  public static final String GET_NAME_METHOD_NAME = "getname";



  /**
   * The method number for the "getName" method.
   */
  public static final int GET_NAME_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to get the value of this LDAP
   * attribute.
   */
  public static final String GET_VALUE_METHOD_NAME = "getvalue";



  /**
   * The method number for the "getValue" method.
   */
  public static final int GET_VALUE_METHOD_NUMBER = 6;



  /**
   * The name of the method that will be used to get the set of values for this
   * LDAP attribute.
   */
  public static final String GET_VALUES_METHOD_NAME = "getvalues";



  /**
   * The method number for the "getValues" method.
   */
  public static final int GET_VALUES_METHOD_NUMBER = 7;



  /**
   * The name of the method that will be used to determine whether this LDAP
   * attribute has a specified value.
   */
  public static final String HAS_VALUE_METHOD_NAME = "hasvalues";



  /**
   * The method number for the "hasValue" method.
   */
  public static final int HAS_VALUE_METHOD_NUMBER = 8;



  /**
   * The name of the method that will be used to determine whether this LDAP
   * attribute has not been initialized.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 9;



  /**
   * The name of the method that will be used to determine whether this LDAP
   * attribute has been initialized.
   */
  public static final String NOT_NULL_METHOD_NAME = "notnull";



  /**
   * The method number for the "notNull" method.
   */
  public static final int NOT_NULL_METHOD_NUMBER = 10;



  /**
   * The name of the method that will be used to remove all values from this
   * LDAP attribute.
   */
  public static final String REMOVE_ALL_VALUES_METHOD_NAME = "removeallvalues";



  /**
   * The method number for the "removeAllValues" method.
   */
  public static final int REMOVE_ALL_VALUES_METHOD_NUMBER = 11;



  /**
   * The name of the method that will be used to remove a specified value from
   * this LDAP attribute.
   */
  public static final String REMOVE_VALUE_METHOD_NAME = "removevalue";



  /**
   * The method number for the removeValue method.
   */
  public static final int REMOVE_VALUE_METHOD_NUMBER = 12;



  /**
   * The name of the method that will be used to specify the name of this LDAP
   * attribute.
   */
  public static final String SET_NAME_METHOD_NAME = "setname";



  /**
   * The method number for the "setName" method.
   */
  public static final int SET_NAME_METHOD_NUMBER = 13;



  /**
   * The name of the method that will be used to specify the value for this LDAP
   * attribute.
   */
  public static final String SET_VALUE_METHOD_NAME = "setvalue";



  /**
   * The method number for the "setValue" method.
   */
  public static final int SET_VALUE_METHOD_NUMBER = 14;



  /**
   * The name of the method that will be used to specify the set of values for
   * this LDAP attribute.
   */
  public static final String SET_VALUES_METHOD_NAME = "setvalues";



  /**
   * The method number for the "setValues" method.
   */
  public static final int SET_VALUES_METHOD_NUMBER = 15;



  /**
   * The set of methods associated with LDAP attribute variables.
   */
  public static final Method[] LDAP_ATTRIBUTE_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { LDAP_ATTRIBUTE_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(GET_NAME_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_VALUE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_VALUES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(HAS_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(NOT_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(REMOVE_ALL_VALUES_METHOD_NAME, new String[0], null),
    new Method(REMOVE_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_NAME_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(SET_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(SET_VALUES_METHOD_NAME,
               new String[] { StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
                 null)
  };



  // The set of values associated with this array.
  private ArrayList<String> attributeValues;

  // The name of this LDAP attribute.
  private String attributeName;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   */
  public LDAPAttributeVariable()
  {
    attributeName   = null;
    attributeValues = new ArrayList<String>();
  }



  /**
   * Creates a new LDAP attribute variable with the specified name and value.
   *
   * @param  name   The name to use for this attribute.
   * @param  value  The value to use for this attribute.
   */
  public LDAPAttributeVariable(String name, String value)
  {
    attributeName   = name;
    attributeValues = new ArrayList<String>();
    attributeValues.add(value);
  }



  /**
   * Creates a new LDAP attribute variable with the specified name and set of
   * values.
   *
   * @param  name    The name to use for this attribute.
   * @param  values  The set of values to use for this attribute.
   */
  public LDAPAttributeVariable(String name, String[] values)
  {
    attributeName   = name;
    attributeValues = new ArrayList<String>();
    for (int i=0; i < values.length; i++)
    {
      attributeValues.add(values[i]);
    }
  }



  /**
   * Creates a new LDAP attribute variable from the provided LDAPAttribute
   * object.
   *
   * @param  attribute  The LDAPAttribute object to use to create this LDAP
   *                    attribute variable.
   */
  public LDAPAttributeVariable(LDAPAttribute attribute)
  {
    attributeName = attribute.getName();
    attributeValues = new ArrayList<String>();
    String[] values = attribute.getStringValueArray();
    for (int i=0; i < values.length; i++)
    {
      attributeValues.add(values[i]);
    }
  }



  /**
   * Retrieves the name of this LDAP attribute.
   *
   * @return  The name of this LDAP attribute.
   */
  public String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Specifies the name for this LDAP attribute.
   *
   * @param  attributeName  The name for this LDAP attribute.
   */
  public void setAttributeName(String attributeName)
  {
    this.attributeName = attributeName;
  }



  /**
   * Retrieves the value for this LDAP attribute.  If there are multiple values,
   * then the first will be returned.  If there are no values, then an empty
   * string will be returned.
   *
   * @return  The value for this LDAP attribute.
   */
  public String getAttributeValue()
  {
    if (! attributeValues.isEmpty())
    {
      return attributeValues.get(0);
    }
    else
    {
      return null;
    }
  }



  /**
   * Retrieves the set of values for this LDAP attribute.
   *
   * @return  The set of values for this LDAP attribute.
   */
  public String[] getAttributeValues()
  {
    String[] values = new String[attributeValues.size()];
    attributeValues.toArray(values);
    return values;
  }



  /**
   * Specifies the set of values for this LDAP attribute.
   *
   * @param  values  The set of values for this LDAP attribute.
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
   * Adds the provided value to this attribute as long as it does not already
   * exist.
   *
   * @param  value  The value to add to this attribute.
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
   * Retrieves this attribute as an LDAPAttribute object.
   *
   * @return  This attribute in the form of an LDAPAttribute object.
   */
  public LDAPAttribute toLDAPAttribute()
  {
    return new LDAPAttribute(attributeName, getAttributeValues());
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return LDAP_ATTRIBUTE_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return LDAP_ATTRIBUTE_VARIABLE_METHODS;
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
    for (int i=0; i < LDAP_ATTRIBUTE_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_ATTRIBUTE_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < LDAP_ATTRIBUTE_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_ATTRIBUTE_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < LDAP_ATTRIBUTE_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_ATTRIBUTE_VARIABLE_METHODS[i].hasSignature(methodName,
                                                          argumentTypes))
      {
        return LDAP_ATTRIBUTE_VARIABLE_METHODS[i].getReturnType();
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
        addValue(sv.getStringValue());

        // This method does not return a value.
        return null;
      case ASSIGN_1_METHOD_NUMBER:
        // Get the LDAP attribute and use it to set the value of this attribute.
        LDAPAttributeVariable lav = (LDAPAttributeVariable)
                                    arguments[0].getArgumentValue();
        this.attributeName   = lav.attributeName;
        this.attributeValues = lav.attributeValues;
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Get the name of the attribute and assign it to this attribute with
        // no values.
        sv = (StringVariable) arguments[0].getArgumentValue();
        this.attributeName = sv.getStringValue();
        this.attributeValues.clear();
        return null;
      case ASSIGN_3_METHOD_NUMBER:
        // Get the attribute name and value and assign them to this attribute.
        sv = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        this.attributeName = sv.getStringValue();
        this.attributeValues.clear();
        this.attributeValues.add(sv2.getStringValue());
        return null;
      case ASSIGN_4_METHOD_NUMBER:
        // Get the attribute name and values and assign them to this attribute.
        sv = (StringVariable) arguments[0].getArgumentValue();
        StringArrayVariable sav = (StringArrayVariable)
                                  arguments[1].getArgumentValue();
        this.attributeName = sv.getStringValue();
        this.attributeValues.clear();
        setAttributeValues(sav.getStringValues());
        return null;
      case GET_NAME_METHOD_NUMBER:
        // Return the attribute name as a string value.
        return new StringVariable(attributeName);
      case GET_VALUE_METHOD_NUMBER:
        // Return the attribute value as a string value.
        return new StringVariable(getAttributeValue());
      case GET_VALUES_METHOD_NUMBER:
        // Return the attribute values as a string array variable.
        return new StringArrayVariable(getAttributeValues());
      case HAS_VALUE_METHOD_NUMBER:
        // Get the attribute value as a string variable.
        sv = (StringVariable) arguments[0].getArgumentValue();
        String value = sv.getStringValue();

        // Iterate through all the values until we find a match or reach the end
        // of the list.
        boolean matchFound = false;
        int size = attributeValues.size();
        for (int i=0; i < size; i++)
        {
          if (value.equalsIgnoreCase(attributeValues.get(i)))
          {
            matchFound = true;
            break;
          }
        }

        // Return the result as a Boolean variable.
        return new BooleanVariable(matchFound);
      case IS_NULL_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(attributeName == null);
      case NOT_NULL_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(attributeName != null);
      case REMOVE_ALL_VALUES_METHOD_NUMBER:
        // Remove all values and don't return a value.
        attributeValues.clear();
        return null;
      case REMOVE_VALUE_METHOD_NUMBER:
        // Get the attribute value as a string variable.
        sv = (StringVariable) arguments[0].getArgumentValue();
        value = sv.getStringValue();

        // Iterate through all the values until we find a match or reach the end
        // of the list.
        matchFound = false;
        size = attributeValues.size();
        for (int i=0; i < size; i++)
        {
          if (value.equalsIgnoreCase(attributeValues.get(i)))
          {
            matchFound = true;
            attributeValues.remove(i);
            break;
          }
        }

        // Return the result as a Boolean variable.
        return new BooleanVariable(matchFound);
      case SET_NAME_METHOD_NUMBER:
        // Set the name and don't return a value.
        sv = (StringVariable) arguments[0].getArgumentValue();
        this.attributeName = sv.getStringValue();
        return null;
      case SET_VALUE_METHOD_NUMBER:
        // Set the value and don't return a value.
        sv = (StringVariable) arguments[0].getArgumentValue();
        this.attributeValues.clear();
        this.attributeValues.add(sv.getStringValue());
        return null;
      case SET_VALUES_METHOD_NUMBER:
        // Set the values and don't return a value.
        sav = (StringArrayVariable) arguments[0].getArgumentValue();
        setAttributeValues(sav.getStringValues());
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
    if (! argument.getArgumentType().equals(LDAP_ATTRIBUTE_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                LDAP_ATTRIBUTE_VARIABLE_TYPE + " rejected.");
    }

    LDAPAttributeVariable lav = (LDAPAttributeVariable)
                                argument.getArgumentValue();
    attributeName   = lav.attributeName;
    attributeValues = lav.attributeValues;
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

    StringBuilder buffer = new StringBuilder();
    buffer.append(attributeName);

    if (attributeValues.isEmpty())
    {
      buffer.append(" (no values)");
    }
    else
    {
      buffer.append(" { \"");
      buffer.append(attributeValues.get(0));
      buffer.append('"');

      for (int i=1; i < attributeValues.size(); i++)
      {
        buffer.append(", \"");
        buffer.append(attributeValues.get(i));
        buffer.append('"');
      }

      buffer.append(" }");
    }

    return buffer.toString();
  }
}

