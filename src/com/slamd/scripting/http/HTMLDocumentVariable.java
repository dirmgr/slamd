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



import com.slamd.http.HTMLDocument;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that can be used to encapsulate an HTML
 * document retrieved as part of processing a request.  An HTML document
 * variable offers the following methods:
 *
 * <UL>
 *   <LI>assign(string documentURL, string htmlData) -- Initializes this HTML
 *       document with the provided URL and HTML data.  This method returns a
 *       boolean value that indicates whether the assignment was
 *       successful.</LI>
 *   <LI>getAssociatedFiles() -- Returns a string array containing a list of
 *       URLs of files associated with this HTML document.</LI>
 *   <LI>getDocumentFrames() -- Returns a string array containing a list of URLs
 *       of frames contained in this HTML document.</LI>
 *   <LI>getDocumentImages() -- Returns a string array containing a list of URLs
 *       of images associated with this HTML document.</LI>
 *   <LI>getDocumentLinks() -- Returns a string array containing a list of URLs
 *       of hyperlinks associated with this HTML document.</LI>
 *   <LI>getHTMLData() -- Returns a string containing the raw HTML that makes up
 *       this HTML document.</LI>
 *   <LI>getTextData() -- Returns a string containing the data that makes up
 *       this HTML document with all tags removed.</LI>
 *   <LI>isNull() -- Returns a boolean value that indicates whether this is a
 *       null document.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class HTMLDocumentVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of HTML document variables.
   */
  public static final String HTML_DOCUMENT_VARIABLE_TYPE = "htmldocument";



  /**
   * The name of the method used to initialize an HTML document.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the "assign" method.
   */
  public static final int ASSIGN_METHOD_NUMBER = 0;



  /**
   * The name of the method used to retrieve a list of the files associated with
   * this HTML document.
   */
  public static final String GET_ASSOCIATED_FILES_METHOD_NAME =
       "getassociatedfiles";



  /**
   * The method number for the "getAssociatedFiles" method.
   */
  public static final int GET_ASSOCIATED_FILES_METHOD_NUMBER = 1;



  /**
   * The name of the method used to retrieve a list of the frames contained in
   * this HTML document.
   */
  public static final String GET_DOCUMENT_FRAMES_METHOD_NAME =
       "getdocumentframes";



  /**
   * The method number for the "getDocumentImages" method.
   */
  public static final int GET_DOCUMENT_FRAMES_METHOD_NUMBER = 2;



  /**
   * The name of the method used to retrieve a list of the images referenced by
   * this HTML document.
   */
  public static final String GET_DOCUMENT_IMAGES_METHOD_NAME =
       "getdocumentimages";



  /**
   * The method number for the "getDocumentImages" method.
   */
  public static final int GET_DOCUMENT_IMAGES_METHOD_NUMBER = 3;


  /**
   * The name of the method used to retrieve a list of the hyperlinks contained
   * in this HTML document.
   */
  public static final String GET_DOCUMENT_LINKS_METHOD_NAME =
       "getdocumentlinks";



  /**
   * The method number for the "getDocumentLinks" method.
   */
  public static final int GET_DOCUMENT_LINKS_METHOD_NUMBER = 4;



  /**
   * The name of the method used to retrieve the raw HTML associated with this
   * HTML document.
   */
  public static final String GET_HTML_DATA_METHOD_NAME = "gethtmldata";



  /**
   * The method number for the "getHTMLData" method.
   */
  public static final int GET_HTML_DATA_METHOD_NUMBER = 5;



  /**
   * The name of the method used to retrieve the text (with HTML tags removed)
   * associated with this HTML document.
   */
  public static final String GET_TEXT_DATA_METHOD_NAME = "gettextdata";



  /**
   * The method number for the "getTextData" method.
   */
  public static final int GET_TEXT_DATA_METHOD_NUMBER = 6;



  /**
   * The name of the method used to determine whether this represents a null
   * HTML document.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 7;



  /**
   * The set of methods associated with HTML document variables.
   */
  public static final Method[] HTML_DOCUMENT_VARIABLE_METHODS = new Method[]
  {
    new Method(ASSIGN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_ASSOCIATED_FILES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_DOCUMENT_FRAMES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_DOCUMENT_IMAGES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_DOCUMENT_LINKS_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_HTML_DATA_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_TEXT_DATA_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };


  // The actual HTML document used to perform all processing.
  private HTMLDocument htmlDocument;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public HTMLDocumentVariable()
         throws ScriptException
  {
    htmlDocument = null;
  }



  /**
   * Creates a new HTML document variable based on the provided document.
   *
   * @param  htmlDocument  The HTML document to use to initialize this variable.
   */
  public HTMLDocumentVariable(HTMLDocument htmlDocument)
  {
    this.htmlDocument = htmlDocument;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return HTML_DOCUMENT_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return HTML_DOCUMENT_VARIABLE_METHODS;
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
    for (int i=0; i < HTML_DOCUMENT_VARIABLE_METHODS.length; i++)
    {
      if (HTML_DOCUMENT_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < HTML_DOCUMENT_VARIABLE_METHODS.length; i++)
    {
      if (HTML_DOCUMENT_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < HTML_DOCUMENT_VARIABLE_METHODS.length; i++)
    {
      if (HTML_DOCUMENT_VARIABLE_METHODS[i].hasSignature(methodName,
                                                         argumentTypes))
      {
        return HTML_DOCUMENT_VARIABLE_METHODS[i].getReturnType();
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
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();

        boolean parsed = false;

        try
        {
          htmlDocument = new HTMLDocument(sv1.getStringValue(),
                                          sv2.getStringValue());
          parsed = htmlDocument.parse();
        }
        catch (Exception e)
        {
          htmlDocument = null;
        }

        return new BooleanVariable(parsed);
      case GET_ASSOCIATED_FILES_METHOD_NUMBER:
        if (htmlDocument == null)
        {
          return new StringArrayVariable();
        }
        else
        {
          return new StringArrayVariable(htmlDocument.getAssociatedFiles());
        }
      case GET_DOCUMENT_FRAMES_METHOD_NUMBER:
        if (htmlDocument == null)
        {
          return new StringArrayVariable();
        }
        else
        {
          return new StringArrayVariable(htmlDocument.getDocumentFrames());
        }
      case GET_DOCUMENT_IMAGES_METHOD_NUMBER:
        if (htmlDocument == null)
        {
          return new StringArrayVariable();
        }
        else
        {
          return new StringArrayVariable(htmlDocument.getDocumentImages());
        }
      case GET_DOCUMENT_LINKS_METHOD_NUMBER:
        if (htmlDocument == null)
        {
          return new StringArrayVariable();
        }
        else
        {
          return new StringArrayVariable(htmlDocument.getDocumentLinks());
        }
      case GET_HTML_DATA_METHOD_NUMBER:
        if (htmlDocument == null)
        {
          return new StringVariable(null);
        }
        else
        {
          return new StringVariable(htmlDocument.getHTMLData());
        }
      case GET_TEXT_DATA_METHOD_NUMBER:
        if (htmlDocument == null)
        {
          return new StringVariable(null);
        }
        else
        {
          return new StringVariable(htmlDocument.getTextData());
        }
      case IS_NULL_METHOD_NUMBER:
        return new BooleanVariable(htmlDocument == null);
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
    if (! argument.getArgumentType().equals(HTML_DOCUMENT_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                HTML_DOCUMENT_VARIABLE_TYPE + " rejected.");
    }

    HTMLDocumentVariable hdv =
         (HTMLDocumentVariable) argument.getArgumentValue();
    htmlDocument = hdv.htmlDocument;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (htmlDocument == null)
    {
      return "null";
    }
    else
    {
      return htmlDocument.getDocumentURL();
    }
  }
}

