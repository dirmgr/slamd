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



import java.net.URL;

import com.slamd.parameter.FileURLParameter;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a file variable that has allows the user to read text
 * files that are stored either on the local filesystem or available remotely
 * via HTTP.  The location of the file should be specified via a URL in the
 * format of either "file:///location" or "http://server:port/location".  There
 * are a number of methods for dealing with such text files:
 *
 * <UL>
 *   <LI>assign(string urlString) -- Specifies the URL for the file that will
 *        be read.  This method does not return a value.</LI>
 *   <LI>getLines() -- Returns a string array that contains the lines of the
 *       file that have been previously read using the <CODE>read()</CODE>
 *       method.</LI>
 *   <LI>getURL() -- Retrieves a string value containing the URL associated with
 *       this variable.
 *   <LI>read() -- Attempts to read the contents of the file into memory.  It
 *       will return a Boolean value that indicates whether the file was able to
 *       be read.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class FileURLVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of file URL variables.
   */
  public static final String FILE_URL_VARIABLE_TYPE = "fileurl";



  /**
   * The name of the method that will be used to specify the URL from which to
   * read the information.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the "assign" method.
   */
  public static final int ASSIGN_METHOD_NUMBER = 0;



  /**
   * The name of the method that will retrieve the lines stored in the specified
   * file.
   */
  public static final String GET_LINES_METHOD_NAME = "getlines";



  /**
   * The method number for the "getLines" method.
   */
  public static final int GET_LINES_METHOD_NUMBER = 1;



  /**
   * The name of the method that will retrieve the URL associated with this
   * variable.
   */
  public static final String GET_URL_METHOD_NAME = "geturl";



  /**
   * The method number for the "getURL" method.
   */
  public static final int GET_URL_METHOD_NUMBER = 2;



  /**
   * The name of the method that will read the contents of the file.
   */
  public static final String READ_METHOD_NAME = "read";



  /**
   * The method number for the "read" method.
   */
  public static final int READ_METHOD_NUMBER = 3;



  /**
   * The set of methods associated with file URL variables.
   */
  public static final Method[] FILE_URL_VARIABLE_METHODS = new Method[]
  {
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(GET_LINES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_URL_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(READ_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };



  // The URL string that indicates the location of the file.
  private String urlString;

  // The set of lines read from the file.
  private String[] fileLines;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public FileURLVariable()
         throws ScriptException
  {
    this.urlString = null;
    this.fileLines = new String[0];
  }



  /**
   * Creates a new file URL variable that will read its contents from the
   * specified location.
   *
   * @param  urlString  The URL that specifies the location of the file to
   *                    be read.
   */
  public FileURLVariable(String urlString)
  {
    this.urlString = urlString;
    this.fileLines = new String[0];
  }



  /**
   * Retrieves the URL string for this file URL variable.
   *
   * @return  The URL string for this file URL variable.
   */
  public String getURLString()
  {
    return urlString;
  }



  /**
   * Specifies the URL string to use for this file URL variable.
   *
   * @param  urlString  The URL string to use for this file URL variable.
   */
  public void setURLString(String urlString)
  {
    this.urlString = urlString;
  }



  /**
   * Retrieves the set of lines that have been read from the file.
   *
   * @return  The set of lines that have been read from the file.
   */
  public String[] getFileLines()
  {
    return fileLines;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return FILE_URL_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return FILE_URL_VARIABLE_METHODS;
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
    for (int i=0; i < FILE_URL_VARIABLE_METHODS.length; i++)
    {
      if (FILE_URL_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < FILE_URL_VARIABLE_METHODS.length; i++)
    {
      if (FILE_URL_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
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
    for (int i=0; i < FILE_URL_VARIABLE_METHODS.length; i++)
    {
      if (FILE_URL_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
      {
        return FILE_URL_VARIABLE_METHODS[i].getReturnType();
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
      case ASSIGN_METHOD_NUMBER:
        // Read the URL to retrieve and don't return a value.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        this.urlString = sv.getStringValue();
        return null;
      case GET_LINES_METHOD_NUMBER:
        // Return the set of lines read as a string array.
        return new StringArrayVariable(fileLines);
      case GET_URL_METHOD_NUMBER:
        // Return the URL as a string value.
        return new StringVariable(urlString);
      case READ_METHOD_NUMBER:
        // Try to read the file into memory.  If anything goes wrong, then
        // return a Boolean value of false.  Otherwise, return true.
        try
        {
          URL fileURL = new URL(urlString);
          FileURLParameter fup = new FileURLParameter("a", "b", fileURL);
          fileLines = fup.getFileLines();
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
    if (! argument.getArgumentType().equals(FILE_URL_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                FILE_URL_VARIABLE_TYPE + " rejected.");
    }

    FileURLVariable fuv = (FileURLVariable) argument.getArgumentValue();
    urlString = fuv.urlString;
    fileLines = fuv.fileLines;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (urlString == null)
    {
      return "null";
    }

    return urlString;
  }
}

