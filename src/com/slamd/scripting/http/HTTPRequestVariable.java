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
package com.slamd.scripting.http;



import java.net.URL;
import java.util.Set;

import com.slamd.http.HTTPRequest;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that can be used to encapsulate a request to
 * send to an HTTP server.  An HTTP request variable offers the following
 * methods:
 *
 * <UL>
 *   <LI>addParameter(string name, string value) -- Adds a parameter with the
 *       specified name and value to this request.  This method does not return
 *       a value.</LI>
 *   <LI>addParameter(string name, stringarray values) -- Adds a parameter with
 *       the specified name and set of values to this request.  This method does
 *       not return a value.</LI>
 *   <LI>assign(string url) -- Initializes this request using the provided URL.
 *       This method returns a boolean value that indicates whether the
 *       assignment was completed successfully.</LI>
 *   <LI>assign(string method, string url) -- Initializes this request using the
 *       provided method (either GET or POST) and URL.  This method returns a
 *       boolean value that indicates whether the  assignment was completed
 *       successfully.</LI>
 *   <LI>clearHeaders() -- Clears all header information associated with this
 *       request.  This method does not return a value.</LI>
 *   <LI>getHeader(string name) -- Returns a string containing value of the
 *       header with the specified name.</LI>
 *   <LI>getHeaderNames() -- Returns a string array containing the names of all
 *       the headers associated with this request.</LI>
 *   <LI>getMethod() -- Returns a string value containing the method used for
 *       this request.</LI>
 *   <LI>getParameter(string name) -- Returns a string containing the value of
 *       the specified parameter.</LI>
 *   <LI>getParameterNames() -- Returns a string array containing the names of
 *       all parameters associated with this request.</LI>
 *   <LI>getParameterValues(string name) -- Returns a string array containing
 *       the set of values for the specified parameter.</LI>
 *   <LI>getURL() -- Returns a string containing the URL for the request.</LI>
 *   <LI>removeAllParameters() -- Removes all parameters from this request.
 *       This method does not return a value.</LI>
 *   <LI>removeHeader(string name) -- Removes the header with the specified name
 *       from this request.  This method does not return a value.</LI>
 *   <LI>removeParameter(string name) -- Removes all values for the parameter
 *       with the specified name from this request.  This method does not return
 *       a value.</LI>
 *   <LI>removeParameter(string name, string value) -- Removes the parameter
 *       with the specified name and value from this request.  This method does
 *       not return a value.</LI>
 *   <LI>replaceParameter(string name, string value) -- Replaces the set of
 *       values for the parameter with the specified name with the provided
 *       value.  This method does not return a value.</LI>
 *   <LI>replaceParameter(string name, stringarray values) -- Replaces the set
 *       of values for the parameter with the specified name with the given set
 *       of values.  This method does not return a value.</LI>
 *   <LI>setHeader(string name, string value) -- Sets the value of the header
 *       with the given name to the provided value.  This method does not return
 *       a value.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class HTTPRequestVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of HTTP request variables.
   */
  public static final String HTTP_REQUEST_VARIABLE_TYPE = "httprequest";



  /**
   * The name of the method that can be used to add a parameter value or set of
   * values to this request.
   */
  public static final String ADD_PARAMETER_METHOD_NAME = "addparameter";



  /**
   * The method number for the first "addParameter" method.
   */
  public static final int ADD_PARAMETER_1_METHOD_NUMBER = 0;



  /**
   * The method number for the second "addParameter" method.
   */
  public static final int ADD_PARAMETER_2_METHOD_NUMBER = 1;



  /**
   * The name of the method that can be used to initialize this request.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the first "assign" method.
   */
  public static final int ASSIGN_1_METHOD_NUMBER = 2;



  /**
   * The method number for the second "assign" method.
   */
  public static final int ASSIGN_2_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to remove all header information
   * from this request.
   */
  public static final String CLEAR_HEADERS_METHOD_NAME = "clearheaders";



  /**
   * The method number for the "clearHeaders" method.
   */
  public static final int CLEAR_HEADERS_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to retrieve the value of a
   * specified header from this request.
   */
  public static final String GET_HEADER_METHOD_NAME = "getheader";



  /**
   * The method number for the "getHeader" method.
   */
  public static final int GET_HEADER_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to retrieve the names of the
   * headers associated with this request.
   */
  public static final String GET_HEADER_NAMES_METHOD_NAME = "getheadernames";



  /**
   * The method number for the "getHeaderNames" method.
   */
  public static final int GET_HEADER_NAMES_METHOD_NUMBER = 6;



  /**
   * The name of the method that can be used to retrieve the request method for
   * this request.
   */
  public static final String GET_METHOD_METHOD_NAME = "getmethod";



  /**
   * The method number for the "getMethod" method.
   */
  public static final int GET_METHOD_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to retrieve the value of the
   * specified parameter from this request.
   */
  public static final String GET_PARAMETER_METHOD_NAME = "getparameter";



  /**
   * The method number for the "getParameter" method.
   */
  public static final int GET_PARAMETER_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to retrieve the names of the
   * parameters associated with this request.
   */
  public static final String GET_PARAMETER_NAMES_METHOD_NAME =
       "getparameternames";



  /**
   * The method number for the "getParameterNames" method.
   */
  public static final int GET_PARAMETER_NAMES_METHOD_NUMBER = 9;



  /**
   * The name of the method that can be used to retrieve the set of values for
   * the specified parameter from this request.
   */
  public static final String GET_PARAMETER_VALUES_METHOD_NAME =
       "getparametervalues";



  /**
   * The method number for the "getParameterValues" method.
   */
  public static final int GET_PARAMETER_VALUES_METHOD_NUMBER = 10;



  /**
   * The name of the method that can be used to retrieve the URL associated with
   * this request.
   */
  public static final String GET_URL_METHOD_NAME = "geturl";



  /**
   * The method number for the "getURL" method.
   */
  public static final int GET_URL_METHOD_NUMBER = 11;



  /**
   * The name of the method that can be used to remove all parameter information
   * associated with this request.
   */
  public static final String REMOVE_ALL_PARAMETERS_METHOD_NAME =
       "removeallparameters";



  /**
   * The method number for the "removeAllParameters" method.
   */
  public static final int REMOVE_ALL_PARAMETERS_METHOD_NUMBER = 12;



  /**
   * The name of the method that can be used to remove the specified header from
   * this request.
   */
  public static final String REMOVE_HEADER_METHOD_NAME = "removeheader";



  /**
   * The method number for the "removeHeader" method.
   */
  public static final int REMOVE_HEADER_METHOD_NUMBER = 13;



  /**
   * The name of the method that can be used to remove the specified parameter
   * or parameter value from this request.
   */
  public static final String REMOVE_PARAMETER_METHOD_NAME = "removeparameter";



  /**
   * The method number for the first "removeParameter" method.
   */
  public static final int REMOVE_PARAMETER_1_METHOD_NUMBER = 14;



  /**
   * The method number for the second "removeParameter" method.
   */
  public static final int REMOVE_PARAMETER_2_METHOD_NUMBER = 15;



  /**
   * The name of the method that can be used to replace the set of values for
   * the specified parameter in this request.
   */
  public static final String REPLACE_PARAMETER_METHOD_NAME = "replaceparameter";



  /**
   * The method number for the first "replaceParameter" method.
   */
  public static final int REPLACE_PARAMETER_1_METHOD_NUMBER = 16;



  /**
   * The method number for the second "replaceParameter" method.
   */
  public static final int REPLACE_PARAMETER_2_METHOD_NUMBER = 17;



  /**
   * The name of the method that can be used to set the value of a header for
   * this request.
   */
  public static final String SET_HEADER_METHOD_NAME = "setheader";



  /**
   * The method number for the "setHeader" method.
   */
  public static final int SET_HEADER_METHOD_NUMBER = 18;



  /**
   * The set of methods associated with HTTP request variables.
   */
  public static final Method[] HTTP_REQUEST_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ADD_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CLEAR_HEADERS_METHOD_NAME, new String[0], null),
    new Method(GET_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_HEADER_NAMES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_METHOD_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_PARAMETER_NAMES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_PARAMETER_VALUES_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_URL_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(REMOVE_ALL_PARAMETERS_METHOD_NAME, new String[0], null),
    new Method(REMOVE_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(REMOVE_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(REMOVE_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(REPLACE_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(REPLACE_PARAMETER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(SET_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE }, null)
  };



  // The actual HTTP request that we will use to perform all processing.
  protected HTTPRequest httpRequest;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public HTTPRequestVariable()
         throws ScriptException
  {
    httpRequest = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return HTTP_REQUEST_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return HTTP_REQUEST_VARIABLE_METHODS;
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
    for (int i=0; i < HTTP_REQUEST_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_REQUEST_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < HTTP_REQUEST_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_REQUEST_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < HTTP_REQUEST_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_REQUEST_VARIABLE_METHODS[i].hasSignature(methodName,
                                                        argumentTypes))
      {
        return HTTP_REQUEST_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_PARAMETER_1_METHOD_NUMBER:
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.addParameter(sv1.getStringValue(), sv2.getStringValue());
        }

        return null;
      case ADD_PARAMETER_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringArrayVariable sav =
             (StringArrayVariable) arguments[1].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.addParameter(sv1.getStringValue(), sav.getStringValues());
        }

        return null;
      case ASSIGN_1_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        try
        {
          httpRequest = new HTTPRequest(true, new URL(sv1.getStringValue()));
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          return new BooleanVariable(false);
        }
      case ASSIGN_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();

        try
        {
          String method = sv1.getStringValue().toLowerCase();
          if (method.equals("get"))
          {
            httpRequest = new HTTPRequest(true, new URL(sv2.getStringValue()));
            return new BooleanVariable(true);
          }
          else if (method.equals("post"))
          {
            httpRequest = new HTTPRequest(false, new URL(sv2.getStringValue()));
            return new BooleanVariable(true);
          }
          else
          {
            return new BooleanVariable(false);
          }
        }
        catch (Exception e)
        {
          return new BooleanVariable(false);
        }
      case CLEAR_HEADERS_METHOD_NUMBER:
        if (httpRequest != null)
        {
          httpRequest.clearHeaders();
        }

        return null;
      case GET_HEADER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        if (httpRequest != null)
        {
          return new StringVariable(
               httpRequest.getHeaderMap().get(sv1.getStringValue()));
        }
        else
        {
          return new StringVariable();
        }
      case GET_HEADER_NAMES_METHOD_NUMBER:
        if (httpRequest != null)
        {
          Set<String> keySet = httpRequest.getHeaderMap().keySet();
          String[] headerNames = new String[keySet.size()];
          keySet.toArray(headerNames);
          return new StringArrayVariable(headerNames);
        }
        else
        {
          return new StringArrayVariable();
        }
      case GET_METHOD_METHOD_NUMBER:
        if (httpRequest != null)
        {
          return new StringVariable(httpRequest.getRequestMethod());
        }
        else
        {
          return new StringVariable();
        }
      case GET_PARAMETER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        if (httpRequest != null)
        {
          return new StringVariable(
                          httpRequest.getParameterValue(sv1.getStringValue()));
        }
        else
        {
          return new StringVariable();
        }
      case GET_PARAMETER_NAMES_METHOD_NUMBER:
        if (httpRequest != null)
        {
          return new StringArrayVariable(httpRequest.getParameterNames());
        }
        else
        {
          return new StringArrayVariable();
        }
      case GET_PARAMETER_VALUES_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        if (httpRequest != null)
        {
          return new StringArrayVariable(
                          httpRequest.getParameterValues(sv1.getStringValue()));
        }
        else
        {
          return new StringArrayVariable();
        }
      case GET_URL_METHOD_NUMBER:
        if (httpRequest != null)
        {
          return new StringVariable(httpRequest.getBaseURL().toExternalForm());
        }
        else
        {
          return new StringVariable();
        }
      case REMOVE_ALL_PARAMETERS_METHOD_NUMBER:
        if (httpRequest != null)
        {
          httpRequest.removeAllParameters();
        }
        return null;
      case REMOVE_HEADER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.removeHeader(sv1.getStringValue());
        }

        return null;
      case REMOVE_PARAMETER_1_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.removeParameter(sv1.getStringValue());
        }

        return null;
      case REMOVE_PARAMETER_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.removeParameter(sv1.getStringValue(),
                                      sv2.getStringValue());
        }

        return null;
      case REPLACE_PARAMETER_1_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.replaceParameter(sv1.getStringValue(),
                                       sv2.getStringValue());
        }

        return null;
      case REPLACE_PARAMETER_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sav = (StringArrayVariable) arguments[1].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.replaceParameter(sv1.getStringValue(),
                                       sav.getStringValues());
        }

        return null;
      case SET_HEADER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();

        if (httpRequest != null)
        {
          httpRequest.setHeader(sv1.getStringValue(), sv2.getStringValue());
        }

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
    if (! argument.getArgumentType().equals(HTTP_REQUEST_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                HTTP_REQUEST_VARIABLE_TYPE + " rejected.");
    }

    HTTPRequestVariable hrv =
         (HTTPRequestVariable) argument.getArgumentValue();
    httpRequest = hrv.httpRequest;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (httpRequest == null)
    {
      return "null";
    }
    else
    {
      return httpRequest.getRequestMethod() + ' ' + httpRequest.getBaseURL();
    }
  }
}

