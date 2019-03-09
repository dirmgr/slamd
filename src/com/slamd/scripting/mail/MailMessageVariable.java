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
package com.slamd.scripting.mail;



import java.util.ArrayList;
import java.util.StringTokenizer;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that stores a mail message, including headers.
 * This message may be either retrieved via POP or IMAP or send via SMTP.  A
 * mail message has the following methods:
 *
 * <UL>
 *   <LI>addBodyLine(string line) -- Adds the provided string to the body of
 *       this message.  This method does not return a value.</LI>
 *   <LI>addBodyLines(stringarray lines) -- Adds the lines contained in the
 *       provided string array to the body of this message.  This method does
 *       not return a value.</LI>
 *   <LI>addCCRecipient(string address) -- Adds the specified address to the
 *       list of CC recipients for this message.  This method does not return a
 *       value.</LI>
 *   <LI>addHeaderLine(string line) -- Adds the provided string to the set of
 *       headers for this message.  This method does not return a value.</LI>
 *   <LI>addToRecipient(string address) -- Adds the specified address to the
 *       list of To recipients for this message.  This method does not return a
 *       value.</LI>
 *   <LI>assign(MailMessage message) -- Initializes this mail message with the
 *       information from the provided message.
 *   <LI>clear() -- Clears the header and body of this message.</LI>
 *   <LI>clearBody() -- Clears the body of this message.</LI>
 *   <LI>clearHeader() -- Clears the header of this message.</LI>
 *   <LI>getBodyLines() -- Retrieves the list of lines in the body of this
 *       message as a string array.</LI>
 *   <LI>getCCRecipients() -- Retrieves the list of CC recipients in this
 *       message as as string array.</LI>
 *   <LI>getHeader(string headerName) -- Retrieves the value of the specified
 *       header as a string value.</LI>
 *   <LI>getHeaderLines() -- Retrieves the list of header lines in this message
 *       as a string array.</LI>
 *   <LI>getSender() -- Retrieves the address of the sender for this message as
 *       a string value.</LI>
 *   <LI>getSubject() -- Retrieves the subject for this message as a string
 *       value.</LI>
 *   <LI>getToRecipients() -- Retrieves the list of To recipients in this
 *       message as a string array.</LI>
 *   <LI>setSender(string sender) -- Specifies the address to use for the sender
 *       of this message.  This method does not return a value.</LI>
 *   <LI>setSubject(string subject) -- Specifies subject to use for this
 *       message.  This method does not return a value.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class MailMessageVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of mail message variables.
   */
  public static final String MAIL_MESSAGE_VARIABLE_TYPE = "mailmessage";



  /**
   * The name of the method that can be used to add a line to the body of this
   * message.
   */
  public static final String ADD_BODY_LINE_METHOD_NAME = "addbodyline";



  /**
   * The method number for the "addBodyLine" method.
   */
  public static final int ADD_BODY_LINE_METHOD_NUMBER = 0;



  /**
   * The name of the method that can be used to add multiple lines to the body
   * of this message.
   */
  public static final String ADD_BODY_LINES_METHOD_NAME = "addbodylines";



  /**
   * The method number for the "addBodyLines" method.
   */
  public static final int ADD_BODY_LINES_METHOD_NUMBER = 1;



  /**
   * The name of the method that can be used to add a CC recipient to this
   * message.
   */
  public static final String ADD_CC_RECIPIENT_METHOD_NAME = "addccrecipient";



  /**
   * The method number for the "addCCRecipient" method.
   */
  public static final int ADD_CC_RECIPIENT_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to add a line to the set of headers
   * for this message.
   */
  public static final String ADD_HEADER_LINE_METHOD_NAME = "addheaderline";



  /**
   * The method number for the "addHeaderLine" method.
   */
  public static final int ADD_HEADER_LINE_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to add a To recipient to this
   * message.
   */
  public static final String ADD_TO_RECIPIENT_METHOD_NAME = "addtorecipient";



  /**
   * The method number for the "addToRecipient" method.
   */
  public static final int ADD_TO_RECIPIENT_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to assign a value to this message.
   */
  public static final String ASSIGN_METHOD_NAME = "assign";



  /**
   * The method number for the "assign" method.
   */
  public static final int ASSIGN_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to clear the contents of this
   * message.
   */
  public static final String CLEAR_METHOD_NAME = "clear";



  /**
   * The method number for the "clear" method.
   */
  public static final int CLEAR_METHOD_NUMBER = 6;



  /**
   * The name of the method that can be used to clear the contents of the
   * message body.
   */
  public static final String CLEAR_BODY_METHOD_NAME = "clearbody";



  /**
   * The method number for the "clearBody" method.
   */
  public static final int CLEAR_BODY_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to clear the contents of the
   * message header.
   */
  public static final String CLEAR_HEADER_METHOD_NAME = "clearheader";



  /**
   * The method number for the "clearHeader" method.
   */
  public static final int CLEAR_HEADER_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to retrieve the lines of the body
   * of this message.
   */
  public static final String GET_BODY_LINES_METHOD_NAME = "getbodylines";



  /**
   * The method number for the "getBodyLines" method.
   */
  public static final int GET_BODY_LINES_METHOD_NUMBER = 9;



  /**
   * The name of the method that can be used to retrieve the list of CC
   * recipients for this message.
   */
  public static final String GET_CC_RECIPIENTS_METHOD_NAME = "getccrecipients";



  /**
   * The method number for the "getCCRecipients" method.
   */
  public static final int GET_CC_RECIPIENTS_METHOD_NUMBER = 10;



  /**
   * The name of the method that can be used to retrieve a specific header for
   * this message.
   */
  public static final String GET_HEADER_METHOD_NAME = "getheader";



  /**
   * The method number for the "getHeader" method.
   */
  public static final int GET_HEADER_METHOD_NUMBER = 11;



  /**
   * The name of the method that can be used to retrieve the list of header
   * lines for this message.
   */
  public static final String GET_HEADER_LINES_METHOD_NAME = "getheaderlines";



  /**
   * The method number for the "getHeaderLines" method.
   */
  public static final int GET_HEADER_LINES_METHOD_NUMBER = 12;



  /**
   * The name of the method that can be used to clear the address of the sender
   * for this message.
   */
  public static final String GET_SENDER_METHOD_NAME = "getsender";



  /**
   * The method number for the "getSender" method.
   */
  public static final int GET_SENDER_METHOD_NUMBER = 13;



  /**
   * The name of the method that can be used to retrieve the subject of this
   * message.
   */
  public static final String GET_SUBJECT_METHOD_NAME = "getsubject";



  /**
   * The method number for the "getSubject" method.
   */
  public static final int GET_SUBJECT_METHOD_NUMBER = 14;



  /**
   * The name of the method that can be used to retrieve the list of "To"
   * recipients for this message.
   */
  public static final String GET_TO_RECIPIENTS_METHOD_NAME = "gettorecipients";



  /**
   * The method number for the "getToRecipients" method.
   */
  public static final int GET_TO_RECIPIENTS_METHOD_NUMBER = 15;



  /**
   * The name of the method that can be used to specify the address of the
   * sender for this message.
   */
  public static final String SET_SENDER_METHOD_NAME = "setsender";



  /**
   * The method number for the "setSender" message.
   */
  public static final int SET_SENDER_METHOD_NUMBER = 16;



  /**
   * The name of the method that can be used to specify the subject for this
   * message.
   */
  public static final String SET_SUBJECT_METHOD_NAME = "setsubject";



  /**
   * The method number for the "setSubject" message.
   */
  public static final int SET_SUBJECT_METHOD_NUMBER = 17;



  /**
   * The set of methods associated with mail message variables.
   */
  public static final Method[] MAIL_MESSAGE_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_BODY_LINE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ADD_BODY_LINES_METHOD_NAME,
               new String[] { StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               null),
    new Method(ADD_CC_RECIPIENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ADD_HEADER_LINE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ADD_TO_RECIPIENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(ASSIGN_METHOD_NAME,
               new String[] { MAIL_MESSAGE_VARIABLE_TYPE },
               null),
    new Method(CLEAR_METHOD_NAME, new String[0], null),
    new Method(CLEAR_BODY_METHOD_NAME, new String[0], null),
    new Method(CLEAR_HEADER_METHOD_NAME, new String[0], null),
    new Method(GET_BODY_LINES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_CC_RECIPIENTS_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_HEADER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_HEADER_LINES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(GET_SENDER_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_SUBJECT_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_TO_RECIPIENTS_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(SET_SENDER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(SET_SUBJECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null)
  };



  // Variables associated with the content of the mail message.
  private ArrayList<String> headerLines;
  private ArrayList<String> bodyLines;
  private ArrayList<String> toList;
  private ArrayList<String> ccList;
  private String            sender;
  private String            subject;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public MailMessageVariable()
         throws ScriptException
  {
    // No implementation required.
    headerLines = new ArrayList<String>();
    bodyLines   = new ArrayList<String>();
    toList      = new ArrayList<String>();
    ccList      = new ArrayList<String>();
    sender      = null;
    subject     = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  public String getVariableTypeName()
  {
    return MAIL_MESSAGE_VARIABLE_TYPE;
  }



  /**
   * Clears the contents of this message.
   */
  public void clear()
  {
    headerLines.clear();
    bodyLines.clear();
    toList.clear();
    ccList.clear();
    sender  = null;
    subject = null;
  }



  /**
   * Clears the body of this message.
   */
  public void clearBody()
  {
    bodyLines.clear();
  }



  /**
   * Clears the header of this message.
   */
  public void clearHeader()
  {
    headerLines.clear();
    toList.clear();
    ccList.clear();
    sender  = null;
    subject = null;
  }



  /**
   * Adds the specified line to the list of headers for this message.
   *
   * @param  line  The line to add to the list of headers for this message.
   */
  public void addHeaderLine(String line)
  {
    if (line == null)
    {
      return;
    }

    headerLines.add(line);

    String lowerLine = line.toLowerCase();
    if (lowerLine.startsWith("subject: "))
    {
      subject = line.substring(9);
    }
    else if (lowerLine.startsWith("from: "))
    {
      sender = line.substring(6);
    }
    else if (lowerLine.startsWith("to: "))
    {
      String toString = line.substring(4);
      StringTokenizer tokenizer = new StringTokenizer(toString, ", ");
      while (tokenizer.hasMoreTokens())
      {
        String token = tokenizer.nextToken();
        if (isEmailAddress(token))
        {
          toList.add(token);
        }
      }
    }
    else if (lowerLine.startsWith("cc: "))
    {
      String ccString = line.substring(4);
      StringTokenizer tokenizer = new StringTokenizer(ccString, ", ");
      while (tokenizer.hasMoreTokens())
      {
        String token = tokenizer.nextToken();
        if (isEmailAddress(token))
        {
          ccList.add(token);
        }
      }
    }
  }



  /**
   * Indicates whether the provided string value is a valid e-mail address.
   * Note that this doesn't do strict validation, but rather just determines
   * whether it meets the basic requirements for an address.
   *
   * @param  stringValue  The string value for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the provided string does represent a valid
   *          e-mail address, or <CODE>false</CODE> if not.
   */
  public static boolean isEmailAddress(String stringValue)
  {
    // First, make sure there is exactly one at symbol.
    int atPos = stringValue.indexOf('@');
    if ((atPos < 0) || (atPos != stringValue.lastIndexOf('@')))
    {
      return false;
    }


    // Make sure there are no spaces or tabs.
    if ((stringValue.indexOf(' ') >= 0) || (stringValue.indexOf('\t') >= 0))
    {
      return false;
    }


    // Split the value into two strings, before and after the at.
    String nameStr   = stringValue.substring(0, atPos);
    String domainStr = stringValue.substring(atPos+1);


    // The domain string must have at least one period, and there may not be
    // consecutive periods.  The name string must not have consecutive periods.
    if ((domainStr.indexOf('.') < 0) || (domainStr.contains("..")) ||
        (nameStr.contains("..")))
    {
      return false;
    }


    // OK.  That's good enough for our purposes.
    return true;
  }



  /**
   * Adds the specified line to the body for this message.
   *
   * @param  line  The line to add to the body of this message.
   */
  public void addBodyLine(String line)
  {
    bodyLines.add(line);
  }



  /**
   * Retrieves the list of CC recipients for this message.
   *
   * @return  The list of CC recipients for this message.
   */
  public ArrayList getCCList()
  {
    return ccList;
  }



  /**
   * Adds the specified address to the list of CC recipients for this message.
   *
   * @param  address  The address to add to the list of CC recipients for this
   *                  message.
   */
  public void addCCRecipient(String address)
  {
    ccList.add(address);

    boolean addedToHeader = false;
    for (int i=0; i < headerLines.size(); i++)
    {
      String headerLine = headerLines.get(i);
      String lowerLine  = headerLine.toLowerCase();
      if (lowerLine.startsWith("cc: "))
      {
        headerLines.set(i, headerLine + ", " + address);
        addedToHeader = true;
        break;
      }
    }

    if (! addedToHeader)
    {
      headerLines.add("CC: " + address);
    }
  }



  /**
   * Retrieves the list of To recipients for this message.
   *
   * @return  The list of To recipients for this message.
   */
  public ArrayList getToList()
  {
    return toList;
  }



  /**
   * Adds the specified address to the list of To recipients for this message.
   *
   * @param  address  The address to add to the list of To recipients for this
   *                  message.
   */
  public void addToRecipient(String address)
  {
    toList.add(address);

    boolean addedToHeader = false;
    for (int i=0; i < headerLines.size(); i++)
    {
      String headerLine = headerLines.get(i);
      String lowerLine  = headerLine.toLowerCase();
      if (lowerLine.startsWith("to: "))
      {
        headerLines.set(i, headerLine + ", " + address);
        addedToHeader = true;
        break;
      }
    }

    if (! addedToHeader)
    {
      headerLines.add("To: " + address);
    }
  }



  /**
   * Retrieves an array with all recipients (both To and CC) for this message.
   *
   * @return  An array with all recipients for this message.
   */
  public String[] getRecipients()
  {
    int toSize = toList.size();
    int ccSize = ccList.size();

    String[] recipientArray = new String[toSize + ccSize];
    for (int i=0; i < toSize; i++)
    {
      recipientArray[i] = toList.get(i);
    }

    for (int i=0,j=toSize; i < ccSize; i++,j++)
    {
      recipientArray[j] = ccList.get(i);
    }

    return recipientArray;
  }



  /**
   * Retrieves the address of the sender for this message.
   *
   * @return  The address of the sender for this message.
   */
  public String getSender()
  {
    return sender;
  }



  /**
   * Specifies the sender address for this message.
   *
   * @param  sender  The sender address to use for the message.
   */
  public void setSender(String sender)
  {
    this.sender = sender;

    boolean headerUpdated = false;
    for (int i=0; i < headerLines.size(); i++)
    {
      String headerLine = headerLines.get(i);
      String lowerLine  = headerLine.toLowerCase();

      if (lowerLine.startsWith("from: "))
      {
        headerLines.set(i, "From: " + sender);
        headerUpdated = true;
        break;
      }
    }

    if (! headerUpdated)
    {
      headerLines.add("From: " + sender);
    }
  }



  /**
   * Retrieves the subject for this message.
   *
   * @return  The subject for this message.
   */
  public String getSubject()
  {
    return subject;
  }



  /**
   * Specifies the subject for this message.
   *
   * @param  subject  The subject to use for the message.
   */
  public void setSubject(String subject)
  {
    this.sender = subject;

    boolean headerUpdated = false;
    for (int i=0; i < headerLines.size(); i++)
    {
      String headerLine = headerLines.get(i);
      String lowerLine  = headerLine.toLowerCase();

      if (lowerLine.startsWith("subject: "))
      {
        headerLines.set(i, "Subject: " + subject);
        headerUpdated = true;
        break;
      }
    }

    if (! headerUpdated)
    {
      headerLines.add("Subject: " + subject);
    }
  }



  /**
   * Retrieves the lines that make up the header of this message.
   *
   * @return  The lines that make up the header of this message.
   */
  public ArrayList getHeaderLines()
  {
    return headerLines;
  }



  /**
   * Retrieves the lines that make up the body of this message.
   *
   * @return  The lines that make up the body of this message.
   */
  public ArrayList getBodyLines()
  {
    return bodyLines;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return MAIL_MESSAGE_VARIABLE_METHODS;
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
    for (int i=0; i < MAIL_MESSAGE_VARIABLE_METHODS.length; i++)
    {
      if (MAIL_MESSAGE_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < MAIL_MESSAGE_VARIABLE_METHODS.length; i++)
    {
      if (MAIL_MESSAGE_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < MAIL_MESSAGE_VARIABLE_METHODS.length; i++)
    {
      if (MAIL_MESSAGE_VARIABLE_METHODS[i].hasSignature(methodName,
                                                        argumentTypes))
      {
        return MAIL_MESSAGE_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_BODY_LINE_METHOD_NUMBER:
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        addBodyLine(sv.getStringValue());
        return null;
      case ADD_BODY_LINES_METHOD_NUMBER:
        StringArrayVariable sav =
             (StringArrayVariable) arguments[0].getArgumentValue();
        String[] lines = sav.getStringValues();
        for (int i=0; ((lines != null) && (i < lines.length)); i++)
        {
          addBodyLine(lines[i]);
        }
        return null;
      case ADD_CC_RECIPIENT_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        addCCRecipient(sv.getStringValue());
        return null;
      case ADD_HEADER_LINE_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        addHeaderLine(sv.getStringValue());
        return null;
      case ADD_TO_RECIPIENT_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        addToRecipient(sv.getStringValue());
        return null;
      case ASSIGN_METHOD_NUMBER:
        assign(arguments[0]);
        return null;
      case CLEAR_METHOD_NUMBER:
        clear();
        return null;
      case CLEAR_BODY_METHOD_NUMBER:
        clearBody();
        return null;
      case CLEAR_HEADER_METHOD_NUMBER:
        clearHeader();
        return null;
      case GET_BODY_LINES_METHOD_NUMBER:
        String[] bodyLineArray = new String[bodyLines.size()];
        bodyLines.toArray(bodyLineArray);
        return new StringArrayVariable(bodyLineArray);
      case GET_CC_RECIPIENTS_METHOD_NUMBER:
        String[] ccArray = new String[ccList.size()];
        ccList.toArray(ccArray);
        return new StringArrayVariable(ccArray);
      case GET_HEADER_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        String lowerHeader = sv.getStringValue().toLowerCase() + ": ";

        for (int i=0; i < headerLines.size(); i++)
        {
          String headerLine = headerLines.get(i);
          String lowerLine  = headerLine.toLowerCase();
          if (lowerLine.startsWith(lowerHeader))
          {
            String returnStr = headerLine.substring(lowerHeader.length());
            return new StringVariable(returnStr);
          }
        }
        return new StringVariable(null);
      case GET_HEADER_LINES_METHOD_NUMBER:
        String[] headerLineArray = new String[headerLines.size()];
        headerLines.toArray(headerLineArray);
        return new StringArrayVariable(headerLineArray);
      case GET_SENDER_METHOD_NUMBER:
        return new StringVariable(sender);
      case GET_SUBJECT_METHOD_NUMBER:
        return  new StringVariable(subject);
      case GET_TO_RECIPIENTS_METHOD_NUMBER:
        String[] toArray = new String[toList.size()];
        toList.toArray(toArray);
        return new StringArrayVariable(toArray);
      case SET_SENDER_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        setSender(sv.getStringValue());
        return null;
      case SET_SUBJECT_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        setSubject(sv.getStringValue());
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
    if (! argument.getArgumentType().equals(MAIL_MESSAGE_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                MAIL_MESSAGE_VARIABLE_TYPE + " rejected.");
    }

    MailMessageVariable mmv = (MailMessageVariable) argument.getArgumentValue();
    this.headerLines = mmv.headerLines;
    this.bodyLines   = mmv.bodyLines;
    this.toList      = mmv.toList;
    this.ccList      = mmv.ccList;
    this.sender      = mmv.sender;
    this.subject     = mmv.subject;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if ((headerLines.isEmpty()) && (bodyLines.isEmpty()))
    {
      return "null";
    }
    else if (subject != null)
    {
      return "mail message \"" + subject + '"';
    }
    else if (sender != null)
    {
      return "mail message from " + sender;
    }
    else
    {
      return "mail message";
    }
  }
}

