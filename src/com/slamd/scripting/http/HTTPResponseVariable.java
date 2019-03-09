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



import java.util.ArrayList;

import com.slamd.http.HTTPResponse;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that can be used to encapsulate a response from
 * an HTTP server.  An HTTP response variable offers the following methods:
 *
 * <UL>
 *   <LI>getContentLength() -- Returns an integer that contains the value of the
 *       "content-length" header for this response, or -1 if no content-length
 *       header is available.</LI>
 *   <LI>getContentType() -- Returns a string that contains the value of the
 *       "content-type" header for this response.</LI>
 *   <LI>getDataAsString() -- Returns a string that contains the string
         representation of the actual data contained in the response.</LI>
 *   <LI>getHeader(string name) -- Returns a string that contains the value of
 *       the header with the specified name.</LI>
 *   <LI>getHeaderNames() -- Returns a string array that contains the names of
 *       the headers associated with this response.</LI>
 *   <LI>getHeaderValues(string name) -- Returns a string array that contains
 *       the values of the header with the specified name.</LI>
 *   <LI>getHTMLDocument() -- Returns an HTMLDocument containing the data
 *       associated with this response.</LI>
 *   <LI>getResponseMessage() -- Returns a string value that contains the
 *       response message provided by the server.</LI>
 *   <LI>getProtocolVersion() -- Returns a string value that contains the
 *       protocol version used by the server in the response.</LI>
 *   <LI>getStatusCode() -- Returns an integer that contains the status code for
 *       this response.</LI>
 *   <LI>isNull() -- Indicates whether this represents a null response.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class HTTPResponseVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of HTTP response variables.
   */
  public static final String HTTP_RESPONSE_VARIABLE_TYPE = "httpresponse";



  /**
   * The name of the method used to retrieve the value of the Content-Length
   * header.
   */
  public static final String GET_CONTENT_LENGTH_METHOD_NAME =
       "getcontentlength";



  /**
   * The method number for the "getContentLength" method.
   */
  public static final int GET_CONTENT_LENGTH_METHOD_NUMBER = 0;



  /**
   * The name of the method used to retrieve the value of the Content-Type
   * header.
   */
  public static final String GET_CONTENT_TYPE_METHOD_NAME = "getcontenttype";



  /**
   * The method number for the "getContentType" method.
   */
  public static final int GET_CONTENT_TYPE_METHOD_NUMBER = 1;



  /**
   * The name of the method used to retrieve a string representation of the
   * data associated with this response.
   */
  public static final String GET_DATA_AS_STRING_METHOD_NAME = "getdataasstring";



  /**
   * The method number for the "getDataAsString" method.
   */
  public static final int GET_DATA_AS_STRING_METHOD_NUMBER = 2;



  /**
   * The name of the method used to retrieve the value of the requested header.
   */
  public static final String GET_HEADER_METHOD_NAME = "getheader";



  /**
   * The method number for the "getHeader" method.
   */
  public static final int GET_HEADER_METHOD_NUMBER = 3;



  /**
   * The name of the method used to retrieve the names of the headers associated
   * this this response.
   */
  public static final String GET_HEADER_NAMES_METHOD_NAME = "getheadernames";



  /**
   * The method number for the "getHeaderNames" method.
   */
  public static final int GET_HEADER_NAMES_METHOD_NUMBER = 4;



  /**
   * The name of the method used to retrieve the values of the requested header.
   */
  public static final String GET_HEADER_VALUES_METHOD_NAME = "getheadervalues";



  /**
   * The method number for the "getHeaderValues" method.
   */
  public static final int GET_HEADER_VALUES_METHOD_NUMBER = 5;



  /**
   * The name of the method used to retrieve the HTML document associated with
   * this response.
   */
  public static final String GET_HTML_DOCUMENT_METHOD_NAME = "gethtmldocument";



  /**
   * The method number for the "getHTMLDocument" method.
   */
  public static final int GET_HTML_DOCUMENT_METHOD_NUMBER = 6;



  /**
   * The name of the method used to retrieve the response message for this
   * response.
   */
  public static final String GET_RESPONSE_MESSAGE_METHOD_NAME =
       "getresponsemessage";



  /**
   * The method number for the "getResponseMessage" method.
   */
  public static final int GET_RESPONSE_MESSAGE_METHOD_NUMBER = 7;



  /**
   * The name of the method used to retrieve the protocol version string for
   * this response.
   */
  public static final String GET_PROTOCOL_VERSION_METHOD_NAME =
       "getprotocolversion";



  /**
   * The method number for the "getProtocolVersion" method.
   */
  public static final int GET_PROTOCOL_VERSION_METHOD_NUMBER = 8;



  /**
   * The name of the method used to retrieve the HTTP status code for this
   * response.
   */
  public static final String GET_STATUS_CODE_METHOD_NAME = "getstatuscode";



  /**
   * The method number for the "getStatusCode" method.
   */
  public static final int GET_STATUS_CODE_METHOD_NUMBER = 9;



  /**
   * The name of the method used to determine whether this is a null response.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 10;



  /**
   * The set of methods associated with HTML document variables.
   */
  public static final Method[] HTTP_RESPONSE_VARIABLE_METHODS = new Method[]
  {
    new Method(GET_CONTENT_LENGTH_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_CONTENT_TYPE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_DATA_AS_STRING_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_HEADER_NAMES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_HEADER_VALUES_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_HTML_DOCUMENT_METHOD_NAME, new String[0],
               HTMLDocumentVariable.HTML_DOCUMENT_VARIABLE_TYPE),
    new Method(GET_RESPONSE_MESSAGE_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_PROTOCOL_VERSION_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_STATUS_CODE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };


  // The actual HTTP response used to perform all processing.
  private HTTPResponse httpResponse;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public HTTPResponseVariable()
         throws ScriptException
  {
    httpResponse = null;
  }



  /**
   * Creates a new HTTP response variable based on the provided response.
   *
   * @param  httpResponse  The HTTP response to use to initialize this variable.
   */
  public HTTPResponseVariable(HTTPResponse httpResponse)
  {
    this.httpResponse = httpResponse;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return HTTP_RESPONSE_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return HTTP_RESPONSE_VARIABLE_METHODS;
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
    for (int i=0; i < HTTP_RESPONSE_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_RESPONSE_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < HTTP_RESPONSE_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_RESPONSE_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < HTTP_RESPONSE_VARIABLE_METHODS.length; i++)
    {
      if (HTTP_RESPONSE_VARIABLE_METHODS[i].hasSignature(methodName,
                                                         argumentTypes))
      {
        return HTTP_RESPONSE_VARIABLE_METHODS[i].getReturnType();
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
      case GET_CONTENT_LENGTH_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new IntegerVariable(-1);
        }
        else
        {
          return new IntegerVariable(httpResponse.getContentLength());
        }
      case GET_CONTENT_TYPE_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new StringVariable();
        }
        else
        {
          return new StringVariable(httpResponse.getContentType());
        }
      case GET_DATA_AS_STRING_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new StringVariable();
        }
        else
        {
          byte[] responseData = httpResponse.getResponseData();
          if ((responseData == null) || (responseData.length == 0))
          {
            return new StringVariable();
          }
          else
          {
            return new StringVariable(new String(responseData));
          }
        }
      case GET_HEADER_METHOD_NUMBER:
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();

        if (httpResponse == null)
        {
          return new StringVariable();
        }
        else
        {
          return
               new StringVariable(httpResponse.getHeader(sv.getStringValue()));
        }
      case GET_HEADER_NAMES_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new StringArrayVariable();
        }
        else
        {
          String[][] headerElements = httpResponse.getHeaderElements();
          ArrayList<String>  nameList = new ArrayList<String>();

          for (int i=0; i < headerElements.length; i++)
          {
            if (! nameList.contains(headerElements[0]))
            {
              nameList.add(headerElements[i][0]);
            }
          }

          String[] headerNames = new String[nameList.size()];
          nameList.toArray(headerNames);
          return new StringArrayVariable(headerNames);
        }
      case GET_HEADER_VALUES_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        if (httpResponse == null)
        {
          return new StringArrayVariable();
        }
        else
        {
          return new StringArrayVariable(
                          httpResponse.getHeaderValues(sv.getStringValue()));
        }
      case GET_HTML_DOCUMENT_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new HTMLDocumentVariable();
        }
        else
        {
          return new HTMLDocumentVariable(httpResponse.getHTMLDocument());
        }
      case GET_RESPONSE_MESSAGE_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new StringVariable();
        }
        else
        {
          return new StringVariable(httpResponse.getResponseMessage());
        }
      case GET_PROTOCOL_VERSION_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new StringVariable();
        }
        else
        {
          return new StringVariable(httpResponse.getProtocolVersion());
        }
      case GET_STATUS_CODE_METHOD_NUMBER:
        if (httpResponse == null)
        {
          return new IntegerVariable(-1);
        }
        else
        {
          return new IntegerVariable(httpResponse.getStatusCode());
        }
      case IS_NULL_METHOD_NUMBER:
        return new BooleanVariable(httpResponse == null);
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
    if (! argument.getArgumentType().equals(HTTP_RESPONSE_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                HTTP_RESPONSE_VARIABLE_TYPE + " rejected.");
    }

    HTTPResponseVariable hrv =
         (HTTPResponseVariable) argument.getArgumentValue();
    httpResponse = hrv.httpResponse;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (httpResponse == null)
    {
      return "null";
    }
    else
    {
      return httpResponse.getProtocolVersion() + ' ' +
             httpResponse.getStatusCode() + ' ' +
             httpResponse.getResponseMessage();
    }
  }
}

