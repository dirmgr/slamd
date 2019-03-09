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
import java.util.Enumeration;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPDN;
import netscape.ldap.LDAPEntry;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable intended to store an LDAP entry.  LDAP entries
 * have the following methods:
 *
 * <UL>
 *   <LI>addAttribute(ldapattribute attribute) -- Adds the specified attribute
 *       to this entry.  This method does not return a value.</LI>
 *   <LI>addAttribute(string name, string value) -- Adds an attribute with the
 *       specified name and value to this entry.  This method does not return a
 *       value.</LI>
 *   <LI>addAttribute(string name, stringarray values) -- Adds an attribute with
 *       the specified name and set of values to this entry.  This method does
 *       not return a value.</LI>
 *   <LI>assign(ldapentry entry) -- Initializes this LDAP entry with the
 *       information from the provided entry.
 *   <LI>assign(string dn) -- Initializes this LDAP entry with the specified DN
 *       and no attributes.
 *   <LI>getAttribute(string name) -- Retrieves the attribute with the specified
 *       name from this entry.  If the specified attribute does not exist, then
 *       a null attribute will be returned</LI>
 *   <LI>getAttributeAt(int position) -- Retrieves the attribute at the
 *       specified position from this entry.</LI>
 *   <LI>getAttributeCount() -- Retrieves the number of attributes stored
 *       in this entry.</LI>
 *   <LI>getAttributeNames() -- Retrieves the names of all the attributes stored
 *       in this entry.</LI>
 *   <LI>getDN() -- Retrieves the DN of this entry as a string value.</LI>
 *   <LI>getNormalizedDN() -- Retrieves the normalized version of the DN for
 *       this entry as a string value.  The DN will also be converted to all
 *       lowercase characters.</LI>
 *   <LI>getParentDN() -- Retrieves the DN of this entry's parent.</LI>
 *   <LI>getValue(string name) -- Retrieves the value of the attribute with the
 *       specified name.  If there are multiple values for the specified
 *       attribute, then the first will be retrieved.  If there are no values,
 *       or if the specified attribute does not exist in the entry, then
 *       a null string is returned.</LI>
 *   <LI>getValues(string name) -- Retrieves the set of values for the attribute
 *       with the specified name as a string array.</LI>
 *   <LI>hasAttribute(string name) -- Returns a Boolean value that indicates
 *       whether this entry contains an attribute with the specified name.</LI>
 *   <LI>isNull() -- Returns a Boolean value that indicates whether this entry
 *       has not been initialized.</LI>
 *   <LI>notNull() -- Returns a Boolean value that indicates whether this entry
 *       is not uninitialized.</LI>
 *   <LI>removeAllAttributes() -- Removes all attributes from this entry.  This
 *       method does not return a value.</LI>
 *   <LI>removeAttribute(string name) -- Removes the specified attribute from
 *       this entry.  A Boolean value is returned that indicates whether this
 *       attribute was actually removed.</LI>
 *   <LI>replaceAttribute(ldapattribute attribute) -- Removes the existing
 *       attribute with the same name as the provided attribute and replaces it
 *       with the one provided.  If the specified attribute did not already
 *       exist in the entry, then it will be added.  This method does not return
 *       a value.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class LDAPEntryVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of LDAP entry variables.
   */
  public static final String LDAP_ENTRY_VARIABLE_TYPE = "ldapentry";



  /**
   * The name of the method that will be used to add a new attribute to this
   * entry.
   */
  public static final String ADD_ATTRIBUTE_METHOD_NAME = "addattribute";



  /**
   * The method number for the first "addAttribute" method.
   */
  public static final int ADD_ATTRIBUTE_1_METHOD_NUMBER = 0;



  /**
   * The method number for the second "addAttribute" method.
   */
  public static final int ADD_ATTRIBUTE_2_METHOD_NUMBER = 1;



  /**
   * The method number for the third "addAttribute" method.
   */
  public static final int ADD_ATTRIBUTE_3_METHOD_NUMBER = 2;



  /**
   * The name of the method that will be used to initialize this entry.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the first "assign" method.
   */
  public static final int ASSIGN_1_METHOD_NUMBER = 3;



  /**
   * The method number for the second "assign" method.
   */
  public static final int ASSIGN_2_METHOD_NUMBER = 4;



  /**
   * The name of the method that will be used to retrieve the attribute with
   * the specified name from this entry.
   */
  public static final String GET_ATTRIBUTE_METHOD_NAME = "getattribute";



  /**
   * The method number for the "getAttribute" method.
   */
  public static final int GET_ATTRIBUTE_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to retrieve the attribute in the
   * specified position from this entry.
   */
  public static final String GET_ATTRIBUTE_AT_METHOD_NAME = "getattributeat";



  /**
   * The method number for the "getAttributeat" method.
   */
  public static final int GET_ATTRIBUTE_AT_METHOD_NUMBER = 6;



  /**
   * The name of the method that will be used to retrieve the number of
   * attributes stored in this entry.
   */
  public static final String GET_ATTRIBUTE_COUNT_METHOD_NAME =
       "getattributecount";



  /**
   * The method number for the "getAttributeCount" method.
   */
  public static final int GET_ATTRIBUTE_COUNT_METHOD_NUMBER = 7;



  /**
   * The name of the method that will be used to retrieve the names of the
   * attributes stored in this entry.
   */
  public static final String GET_ATTRIBUTE_NAMES_METHOD_NAME =
       "getattributenames";



  /**
   * The method number for the "getAttributeNames" method.
   */
  public static final int GET_ATTRIBUTE_NAMES_METHOD_NUMBER = 8;



  /**
   * The name of the method that will be used to retrieve the DN for this entry.
   */
  public static final String GET_DN_METHOD_NAME = "getdn";



  /**
   * The method number for the "getDN" method.
   */
  public static final int GET_DN_METHOD_NUMBER = 9;



  /**
   * The name of the method that will be used to retrieve the lowercase
   * normalized DN for this entry.
   */
  public static final String GET_NORMALIZED_DN_METHOD_NAME = "getnormalizeddn";



  /**
   * The method number for the "getNormalizedDN" method.
   */
  public static final int GET_NORMALIZED_DN_METHOD_NUMBER = 10;



  /**
   * The name of the method that will be used to retrieve the DN of this entry's
   * parent.
   */
  public static final String GET_PARENT_DN_METHOD_NAME = "getparentdn";



  /**
   * The method number for the "getParentDN" method.
   */
  public static final int GET_PARENT_DN_METHOD_NUMBER = 11;



  /**
   * The name of the method that will be used to retrieve the value for the
   * specified attribute.
   */
  public static final String GET_VALUE_METHOD_NAME = "getvalue";



  /**
   * The method number for the "getValue" method.
   */
  public static final int GET_VALUE_METHOD_NUMBER = 12;



  /**
   * The name of the method that will be used to retrieve the set of values for
   * the specified attribute.
   */
  public static final String GET_VALUES_METHOD_NAME = "getvalues";



  /**
   * The method number for the "getValues" method.
   */
  public static final int GET_VALUES_METHOD_NUMBER = 13;



  /**
   * The name of the method that will be used to determine if this entry
   * contains the specified attribute.
   */
  public static final String HAS_ATTRIBUTE_METHOD_NAME = "hasattribute";



  /**
   * The method number for the "hasAttribute" method.
   */
  public static final int HAS_ATTRIBUTE_METHOD_NUMBER = 14;



  /**
   * The name of the method that will be used in order to determine if this
   * entry has not been initialized.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 15;



  /**
   * The name of the method that will be used in order to determine if this
   * entry is not uninitialized.
   */
  public static final String NOT_NULL_METHOD_NAME = "notnull";



  /**
   * The method number for the "notNull" method.
   */
  public static final int NOT_NULL_METHOD_NUMBER = 16;



  /**
   * The name of the method that will be used to remove all attributes from this
   * entry.
   */
  public static final String REMOVE_ALL_ATTRIBUTES_METHOD_NAME =
       "removeallattributes";



  /**
   * The method number for the "removeAllAttributes" method.
   */
  public static final int REMOVE_ALL_ATTRIBUTES_METHOD_NUMBER = 17;



  /**
   * The name of the method that will be used to remove a specified attribute
   * from this entry.
   */
  public static final String REMOVE_ATTRIBUTE_METHOD_NAME = "removeattribute";



  /**
   * The method number for the "removeAttribute" method.
   */
  public static final int REMOVE_ATTRIBUTE_METHOD_NUMBER = 18;



  /**
   * The name of the method that will be used to replace an attribute in this
   * entry.
   */
  public static final String REPLACE_ATTRIBUTE_METHOD_NAME = "replaceattribute";



  /**
   * The method number for the "replaceAttribute" method.
   */
  public static final int REPLACE_ATTRIBUTE_METHOD_NUMBER = 19;



  /**
   * The set of methods associated with LDAP entry variables.
   */
  public static final Method[] LDAP_ENTRY_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_ATTRIBUTE_METHOD_NAME,
               new String[] { LDAPAttributeVariable.
                                   LDAP_ATTRIBUTE_VARIABLE_TYPE }, null),
    new Method(ADD_ATTRIBUTE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ADD_ATTRIBUTE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(ASSIGN_METHOD_NAME, new String[] { LDAP_ENTRY_VARIABLE_TYPE },
               null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(GET_ATTRIBUTE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               LDAPAttributeVariable.LDAP_ATTRIBUTE_VARIABLE_TYPE),
    new Method(GET_ATTRIBUTE_AT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               LDAPAttributeVariable.LDAP_ATTRIBUTE_VARIABLE_TYPE),
    new Method(GET_ATTRIBUTE_COUNT_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_ATTRIBUTE_NAMES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_DN_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_NORMALIZED_DN_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_PARENT_DN_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_VALUE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_VALUES_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(HAS_ATTRIBUTE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(NOT_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(REMOVE_ALL_ATTRIBUTES_METHOD_NAME, new String[0], null),
    new Method(REMOVE_ATTRIBUTE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(REPLACE_ATTRIBUTE_METHOD_NAME,
               new String[] { LDAPAttributeVariable.
                                   LDAP_ATTRIBUTE_VARIABLE_TYPE }, null)
  };



  // The set of attributes associated with this entry.
  private ArrayList<LDAPAttributeVariable> attributes;

  // The DN for this entry.
  private String entryDN;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   */
  public LDAPEntryVariable()
  {
    entryDN    = null;
    attributes = new ArrayList<LDAPAttributeVariable>();
  }



  /**
   * Creates a new LDAP entry variable base on the provided LDAPEntry object.
   *
   * @param  entry  The LDAPEntry object that should be used to create this LDAP
   *                entry variable.
   */
  public LDAPEntryVariable(LDAPEntry entry)
  {
    entryDN = entry.getDN();
    attributes = new ArrayList<LDAPAttributeVariable>();

    Enumeration attrs = entry.getAttributeSet().getAttributes();
    while (attrs.hasMoreElements())
    {
      attributes.add(new LDAPAttributeVariable((LDAPAttribute)
                                               attrs.nextElement()));
    }
  }



  /**
   * Converts this LDAP entry variable to an LDAPEntry object.
   *
   * @return  The LDAPEntry object converted from this variable.
   */
  public LDAPEntry toLDAPEntry()
  {
    LDAPAttributeSet attributeSet = new LDAPAttributeSet(getLDAPAttributes());
    return new LDAPEntry(entryDN, attributeSet);
  }



  /**
   * Retrieves the DN for this entry.
   *
   * @return  The DN for this entry.
   */
  public String getDN()
  {
    return entryDN;
  }



  /**
   * Specifies the DN for this entry.
   *
   * @param  entryDN  The DN for this entry.
   */
  public void setDN(String entryDN)
  {
    this.entryDN = entryDN;
  }



  /**
   * Retrieves the set of attributes stored in this entry.
   *
   * @return  The set of attributes stored in this entry.
   */
  public LDAPAttributeVariable[] getAttributes()
  {
    LDAPAttributeVariable[] attrs =
         new LDAPAttributeVariable[attributes.size()];
    attributes.toArray(attrs);
    return attrs;
  }



  /**
   * Retrieves the requested attribute from this entry.
   *
   * @param  attributeName  The name of the attribute to be retrieved.
   *
   * @return  The requested attribute, or <CODE>null</CODE> if the attribute is
   *          not defined.
   */
  public LDAPAttributeVariable getAttribute(String attributeName)
  {
    int size = attributes.size();
    for (int i=0; i < size; i++)
    {
      LDAPAttributeVariable lav = attributes.get(i);
      if (lav.getAttributeName().equalsIgnoreCase(attributeName))
      {
        return lav;
      }
    }

    return null;
  }



  /**
   * Retrieves the value of the requested attribute from this entry.
   *
   * @param  attributeName  The name of the attribute to be retrieved.
   *
   * @return  The value of the requested attribute, or <CODE>null</CODE> if the
   *          attribute is not defined or does not have any values.
   */
  public String getAttributeValue(String attributeName)
  {
    int size = attributes.size();
    for (int i=0; i < size; i++)
    {
      LDAPAttributeVariable lav = attributes.get(i);
      if (lav.getAttributeName().equalsIgnoreCase(attributeName))
      {
        return lav.getAttributeValue();
      }
    }

    return null;
  }



  /**
   * Retrieves the values of the requested attribute from this entry.
   *
   * @param  attributeName  The name of the attribute to be retrieved.
   *
   * @return  The values of the requested attribute, or <CODE>null</CODE> if the
   *          attribute is not defined.
   */
  public String[] getAttributeValues(String attributeName)
  {
    int size = attributes.size();
    for (int i=0; i < size; i++)
    {
      LDAPAttributeVariable lav = attributes.get(i);
      if (lav.getAttributeName().equalsIgnoreCase(attributeName))
      {
        return lav.getAttributeValues();
      }
    }

    return null;
  }



  /**
   * Retrieves the set of attributes stored in this entry as LDAPAttribute
   * objects.
   *
   * @return  The set of attributes stored in this entry as LDAPAttribute
   *          objects.
   */
  public LDAPAttribute[] getLDAPAttributes()
  {
    LDAPAttribute[] attrs = new LDAPAttribute[attributes.size()];
    for (int i=0; i < attrs.length; i++)
    {
      attrs[i] = attributes.get(i).toLDAPAttribute();
    }

    return attrs;
  }



  /**
   * Specifies the set of attributes to use for this entry.
   *
   * @param  attributes  The set of attributes to use for this entry.
   */
  public void setAttributes(LDAPAttributeVariable[] attributes)
  {
    this.attributes.clear();
    for (int i=0; i < attributes.length; i++)
    {
      this.attributes.add(attributes[i]);
    }
  }



  /**
   * Specifies the set of attributes to use for this entry.
   *
   * @param  attributes  The set of attributes to use for this entry.
   */
  public void setAttributes(LDAPAttribute[] attributes)
  {
    this.attributes.clear();
    for (int i=0; i < attributes.length; i++)
    {
      LDAPAttributeVariable attr = new LDAPAttributeVariable();
      attr.setAttributeName(attributes[i].getName());
      attr.setAttributeValues(attributes[i].getStringValueArray());
      this.attributes.add(attr);
    }
  }



  /**
   * Adds the specified attribute to this entry.  If the specified
   * attribute is not already in the set, then it is simply added.  If the
   * specified attribute is in the set, then the values are merged.
   *
   * @param  attribute  The attribute to be added to this entry.
   */
  public void addAttribute(LDAPAttributeVariable attribute)
  {
    for (int i=0; i < attributes.size(); i++)
    {
      LDAPAttributeVariable attr = attributes.get(i);
      if (attr.getAttributeName().equalsIgnoreCase(
                                       attribute.getAttributeName()))
      {
        // The attribute is already in the set, so merge the values.
        String[] values = attribute.getAttributeValues();
        for (int j=0; j < values.length; j++)
        {
          attr.addValue(values[j]);
        }
        return;
      }
    }


    // If we get here, then the attribute was not found in the set, so just
    // add it.
    attributes.add(attribute);
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return LDAP_ENTRY_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return LDAP_ENTRY_VARIABLE_METHODS;
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
    for (int i=0; i < LDAP_ENTRY_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_ENTRY_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < LDAP_ENTRY_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_ENTRY_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < LDAP_ENTRY_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_ENTRY_VARIABLE_METHODS[i].hasSignature(methodName,
                                                      argumentTypes))
      {
        return LDAP_ENTRY_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_ATTRIBUTE_1_METHOD_NUMBER:
        // Get the attribute to add
        LDAPAttributeVariable lav = (LDAPAttributeVariable)
                                    arguments[0].getArgumentValue();

        // Add the attribute and don't return a value.
        addAttribute(lav);
        return null;
      case ADD_ATTRIBUTE_2_METHOD_NUMBER:
        // Get the attribute name and value.
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();

        // Add the attribute and don't return a value.
        addAttribute(new LDAPAttributeVariable(sv1.getStringValue(),
                                               sv2.getStringValue()));
        return null;
      case ADD_ATTRIBUTE_3_METHOD_NUMBER:
        // Get the attribute name and values.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringArrayVariable sav = (StringArrayVariable)
                                  arguments[1].getArgumentValue();

        // Add the attribute and don't return a value.
        addAttribute(new LDAPAttributeVariable(sv1.getStringValue(),
                                               sav.getStringValues()));
        return null;
      case ASSIGN_1_METHOD_NUMBER:
        // Get the entry to use.
        LDAPEntryVariable lev = (LDAPEntryVariable)
                                arguments[0].getArgumentValue();

        // Assign this entry to the information in the provided entry.
        this.entryDN    = lev.entryDN;
        setAttributes(lev.getAttributes());
        return null;
      case ASSIGN_2_METHOD_NUMBER:
        // Get the entry DN
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        // Use the specified DN and no values.
        this.entryDN    = sv1.getStringValue();
        this.attributes.clear();
        return null;
      case GET_ATTRIBUTE_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        String name = sv1.getStringValue();

        // Append the strings and return the result as a string variable.
        for (int i=0; i < attributes.size(); i++)
        {
          lav = attributes.get(i);
          if (lav.getAttributeName().equalsIgnoreCase(name))
          {
            return lav;
          }
        }

        return new LDAPAttributeVariable();
      case GET_ATTRIBUTE_AT_METHOD_NUMBER:
        // Get the position as an integer value.
        IntegerVariable iv1 = (IntegerVariable) arguments[0].getArgumentValue();

        // Return the attribute in the specified position.
        return attributes.get(iv1.getIntValue());
      case GET_ATTRIBUTE_COUNT_METHOD_NUMBER:
        // Return the number of attributes as an integer value.
        return new IntegerVariable(attributes.size());
      case GET_ATTRIBUTE_NAMES_METHOD_NUMBER:
        // Get the names of the attributes and return as a string array.
        String[] names = new String[attributes.size()];
        for (int i=0; i < names.length; i++)
        {
          names[i] = attributes.get(i).getAttributeName();
        }
        return new StringArrayVariable(names);
      case GET_DN_METHOD_NUMBER:
        // Return the DN of the entry as a string value.
        return new StringVariable(entryDN);
      case GET_NORMALIZED_DN_METHOD_NUMBER:
        // Return the normalized DN of the entry as a string value.
        return new StringVariable(LDAPDN.normalize(entryDN));
      case GET_PARENT_DN_METHOD_NUMBER:
        // Create and return a string variable that contains the parent DN.
        String[] dncomps = LDAPDN.explodeDN(entryDN, false);
        String parentDN = "";
        String separator = "";
        for (int i=1; i < dncomps.length; i++)
        {
          parentDN += separator + dncomps[i];
          separator = ",";
        }
        return new StringVariable(parentDN);
      case GET_VALUE_METHOD_NUMBER:
        // Get the value of the string argument.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        name = sv1.getStringValue();

        // Get the value of the specified attribute and return it.
        for (int i=0; i < attributes.size(); i++)
        {
          lav = attributes.get(i);
          if (lav.getAttributeName().equalsIgnoreCase(name))
          {
            return new StringVariable(lav.getAttributeValue());
          }
        }

        // We didn't find the specified attribute, so return a null string.
        return new StringVariable();
      case GET_VALUES_METHOD_NUMBER:
        // Get the value of the string argument.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        name = sv1.getStringValue();

        // Get the values of the specified attribute and return them.
        for (int i=0; i < attributes.size(); i++)
        {
          lav = attributes.get(i);
          if (lav.getAttributeName().equalsIgnoreCase(name))
          {
            return new StringArrayVariable(lav.getAttributeValues());
          }
        }

        // We didn't find the specified attribute, so return an empty array.
        return new StringArrayVariable();
      case HAS_ATTRIBUTE_METHOD_NUMBER:
        // Get the value of the string argument.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        name = sv1.getStringValue();

        // See if the entry has the specified attribute.
        boolean matchFound = false;
        for (int i=0; i < attributes.size(); i++)
        {
          lav = attributes.get(i);
          if (lav.getAttributeName().equalsIgnoreCase(name))
          {
            matchFound = true;
            break;
          }
        }

        // Return the result as a Boolean value.
        return new BooleanVariable(matchFound);
      case IS_NULL_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(entryDN == null);
      case NOT_NULL_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(entryDN != null);
      case REMOVE_ALL_ATTRIBUTES_METHOD_NUMBER:
        // Remove all the attributes and don't return a value.
        attributes.clear();
        return null;
      case REMOVE_ATTRIBUTE_METHOD_NUMBER:
        // Get the value of the string argument.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        name = sv1.getStringValue();

        // See if the entry has the specified attribute.
        matchFound = false;
        for (int i=0; i < attributes.size(); i++)
        {
          lav = attributes.get(i);
          if (lav.getAttributeName().equalsIgnoreCase(name))
          {
            matchFound = true;
            attributes.remove(i);
            break;
          }
        }

        // Return the result as a Boolean value.
        return new BooleanVariable(matchFound);
      case REPLACE_ATTRIBUTE_METHOD_NUMBER:
        // Get the value of the attribute argument.
        lav = (LDAPAttributeVariable) arguments[0].getArgumentValue();
        name = lav.getAttributeName();

        // See if the entry has the specified attribute.
        for (int i=0; i < attributes.size(); i++)
        {
          lav = attributes.get(i);
          if (lav.getAttributeName().equalsIgnoreCase(name))
          {
            attributes.set(i, lav);
            return null;
          }
        }

        // It wasn't already there, so add it and don't return a value.
        attributes.add(lav);
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
    if (! argument.getArgumentType().equals(LDAP_ENTRY_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                LDAP_ENTRY_VARIABLE_TYPE + " rejected.");
    }

    LDAPEntryVariable lev = (LDAPEntryVariable) argument.getArgumentValue();
    entryDN    = lev.entryDN;
    attributes = lev.attributes;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (entryDN == null)
    {
      return "null";
    }
    else
    {
      return entryDN;
    }
  }
}

