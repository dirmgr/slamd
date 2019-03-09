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



import java.net.URL;

import com.slamd.parameter.FileURLParameter;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that provides the capability to perform various
 * tasks that can be helpful when interacting with a directory server.  Those
 * tasks are made available through the following methods:
 *
 * <UL>
 *   <LI>createOrgEntry(string parentDN) -- Creates a new organization entry
 *       under the specified parent.</LI>
 *   <LI>createOrgUnitEntry(string parentDN) -- Creates a new organizationalUnit
 *       entry under the specified parent.</LI>
 *   <LI>createTemplateEntry(string parentDN, string templateName) -- Creates a
 *       new entry under the specified parent, using the indicated template.
 *       This will return a null entry if the template is not defined or the
 *       template file URL has not been specified.</LI>
 *   <LI>createUserEntry(string parentDN) -- Creates a new user entry under the
 *       specified parent.</LI>
 *   <LI>loadTemplateFileURL(string templateFileURL) -- Reads the template file
 *       located at the specified URL (file or HTTP) for use in creating entries
 *       from templates.  This method returns a Boolean value that indicates
 *       whether the information was successfully read.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class LDAPUtilsVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of LDAP utility variables.
   */
  public static final String LDAP_UTILS_VARIABLE_TYPE = "ldaputils";



  /**
   * The name of the method that creates an organization entry.
   */
  public static final String CREATE_ORG_ENTRY_METHOD_NAME = "createorgentry";



  /**
   * The methodNumber for the "createOrgEntry" method.
   */
  public static final int CREATE_ORG_ENTRY_METHOD_NUMBER = 0;



  /**
   * The name of the method that creates an organizationalUnit entry.
   */
  public static final String CREATE_ORG_UNIT_ENTRY_METHOD_NAME =
       "createorgunitentry";




  /**
   * The method number for the "createOrgUnitEntry" method.
   */
  public static final int CREATE_ORG_UNIT_ENTRY_METHOD_NUMBER = 1;



  /**
   * The name of the method that creates a new template-based entry.
   */
  public static final String CREATE_TEMPLATE_ENTRY_METHOD_NAME =
       "createtemplateentry";



  /**
   * The method number for the "createTemplateEntry" method.
   */
  public static final int CREATE_TEMPLATE_ENTRY_METHOD_NUMBER = 2;



  /**
   * The name of the method that creates a new user entry.
   */
  public static final String CREATE_USER_ENTRY_METHOD_NAME = "createuserentry";



  /**
   * The method number for the "createUserEntry" method.
   */
  public static final int CREATE_USER_ENTRY_METHOD_NUMBER = 3;



  /**
   * The name of the method that reads the template file.
   */
  public static final String LOAD_TEMPLATE_FILE_URL_METHOD_NAME =
       "loadtemplatefileurl";



  /**
   * The method number for the "loadTemplateFileURL" method.
   */
  public static final int LOAD_TEMPLATE_FILE_URL_METHOD_NUMBER = 4;



  /**
   * The name of the template that will be used for creating organization
   * entries.
   */
  public static final String ORG_TEMPLATE_NAME = "defaultorgtemplate";



  /**
   * The name of the template that will be used for creating organizationalUnit
   * entries.
   */
  public static final String ORG_UNIT_TEMPLATE_NAME = "defaultorgunittemplate";



  /**
   * The name of the template that will be used for creating user entries.
   */
  public static final String USER_TEMPLATE_NAME = "defaultusertemplate";



  /**
   * The set of methods that are defined for "ldaputils" variables.
   */
  public static final Method[] LDAP_UTILS_VARIABLE_METHODS = new Method[]
  {
    new Method(CREATE_ORG_ENTRY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               LDAPEntryVariable.LDAP_ENTRY_VARIABLE_TYPE),
    new Method(CREATE_ORG_UNIT_ENTRY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               LDAPEntryVariable.LDAP_ENTRY_VARIABLE_TYPE),
    new Method(CREATE_TEMPLATE_ENTRY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               LDAPEntryVariable.LDAP_ENTRY_VARIABLE_TYPE),
    new Method(CREATE_USER_ENTRY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               LDAPEntryVariable.LDAP_ENTRY_VARIABLE_TYPE),
    new Method(LOAD_TEMPLATE_FILE_URL_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };



  // The entry generator that will be used to create template-based entries.
  private LDAPEntryGenerator entryGenerator;

  // The template that will be used to create organization entries.
  private LDAPEntryTemplate orgTemplate;

  // The template that will be used to create organizationalUnit entries.
  private LDAPEntryTemplate orgUnitTemplate;

  // The template that will be used to create user entries.
  private LDAPEntryTemplate userTemplate;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   */
  public LDAPUtilsVariable()
  {
    entryGenerator = new LDAPEntryGenerator();

    orgTemplate = new LDAPEntryTemplate(ORG_TEMPLATE_NAME, "o");
    orgTemplate.addAttribute("objectclass", ": ", "top");
    orgTemplate.addAttribute("objectclass", ": ", "organization");
    orgTemplate.addAttribute("o", ": ", "<guid>");
    orgTemplate.completeInitialization();
    entryGenerator.addTemplate(orgTemplate);

    orgUnitTemplate = new LDAPEntryTemplate(ORG_UNIT_TEMPLATE_NAME, "ou");
    orgUnitTemplate.addAttribute("objectclass", ": ", "top");
    orgUnitTemplate.addAttribute("objectclass", ": ", "organizationalUnit");
    orgUnitTemplate.addAttribute("ou", ": ", "<guid>");
    orgUnitTemplate.completeInitialization();
    entryGenerator.addTemplate(orgUnitTemplate);

    userTemplate = new LDAPEntryTemplate(USER_TEMPLATE_NAME, "uid");
    userTemplate.addAttribute("objectclass", ": ", "top");
    userTemplate.addAttribute("objectclass", ": ", "person");
    userTemplate.addAttribute("objectclass", ": ", "organizationalPerson");
    userTemplate.addAttribute("objectclass", ": ", "inetOrgPerson");
    userTemplate.addAttribute("givenName", ": ", "<random:alpha:10>");
    userTemplate.addAttribute("sn", ": ", "<random:alpha:10>");
    userTemplate.addAttribute("uid", ": ", "{givenName}.{sn}");
    userTemplate.addAttribute("cn", ": ", "{givenName} {sn}");
    userTemplate.addAttribute("mail", ": ", "{uid}@example.com");
    userTemplate.addAttribute("userPassword", ": ", "<random:alphanumeric:8>");
    userTemplate.addAttribute("telephoneNumber", ": ", "<random:telephone>");
    userTemplate.addAttribute("pager", ": ", "<random:telephone>");
    userTemplate.addAttribute("mobile", ": ", "<random:telephone>");
    userTemplate.completeInitialization();
    entryGenerator.addTemplate(userTemplate);
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return LDAP_UTILS_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return LDAP_UTILS_VARIABLE_METHODS;
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
    for (int i=0; i < LDAP_UTILS_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_UTILS_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < LDAP_UTILS_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_UTILS_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < LDAP_UTILS_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_UTILS_VARIABLE_METHODS[i].hasSignature(methodName,
                                                      argumentTypes))
      {
        return LDAP_UTILS_VARIABLE_METHODS[i].getReturnType();
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
      case CREATE_ORG_ENTRY_METHOD_NUMBER:
        // Retrieve the parent DN.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();

        // Create and return the organization entry.
        return entryGenerator.createTemplateEntry(sv.getStringValue(),
                                                  ORG_TEMPLATE_NAME);
      case CREATE_ORG_UNIT_ENTRY_METHOD_NUMBER:
        // Retrieve the parent DN.
        sv = (StringVariable) arguments[0].getArgumentValue();

        // Create and return the organization entry.
        return entryGenerator.createTemplateEntry(sv.getStringValue(),
                                                  ORG_UNIT_TEMPLATE_NAME);
      case CREATE_TEMPLATE_ENTRY_METHOD_NUMBER:
        // Retrieve the parent DN and the template name.
        sv = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();

        // Create and return the organization entry.
        return entryGenerator.createTemplateEntry(sv.getStringValue(),
                                                  sv2.getStringValue());
      case CREATE_USER_ENTRY_METHOD_NUMBER:
        // Retrieve the parent DN.
        sv = (StringVariable) arguments[0].getArgumentValue();

        // Create and return the organization entry.
        return entryGenerator.createTemplateEntry(sv.getStringValue(),
                                                  USER_TEMPLATE_NAME);
      case LOAD_TEMPLATE_FILE_URL_METHOD_NUMBER:
        // Retrieve the file URL.
        sv = (StringVariable) arguments[0].getArgumentValue();

        // Get the data from the file and parse it as template information.
        try
        {
          URL fileURL = new URL(sv.getStringValue());
          FileURLParameter fup = new FileURLParameter("fileurl", "fileurl",
                                                      fileURL);
          String[] lines = fup.getFileLines();
          entryGenerator.parseTemplateFile(lines);
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          return new BooleanVariable(false);
        }
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
    if (! argument.getArgumentType().equals(LDAP_UTILS_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                LDAP_UTILS_VARIABLE_TYPE + " rejected.");
    }

    LDAPUtilsVariable luv = (LDAPUtilsVariable) argument.getArgumentValue();
    entryGenerator  = luv.entryGenerator;
    orgTemplate     = luv.orgTemplate;
    orgUnitTemplate = luv.orgUnitTemplate;
    userTemplate    = luv.userTemplate;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return "LDAP utils";
  }
}

