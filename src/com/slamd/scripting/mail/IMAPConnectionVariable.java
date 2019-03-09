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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.net.ssl.SSLSocket;

import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.job.JobClass;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that maintains a connection to an IMAPv4rev1
 * mail server and allows for interaction with that server.  An IMAP connection
 * has the the following methods:
 *
 * <UL>
 *   <LI>authenticate(string userID, string password) -- Authenticates to the
 *       IMAP server.  This method returns a Boolean value indicating whether
 *       the authentication was successful.</LI>
 *   <LI>capability() -- Retrieves a string array containing the list of
 *       capability values reported by the IMAP server.</LI>
 *   <LI>connect(string host, int port) -- Establishes an IMAP connection to the
 *       mail server.  This method returns a Boolean value indicating whether
 *       the connection was established successfully.</LI>
 *   <LI>connect(string host, int port, boolean useSSL) -- Establishes an IMAP
 *       connection to the mail server, optionally using SSL for secure
 *       communication.  This method returns a Boolean value indicating whether
 *       the connection was established successfully.</LI>
 *   <LI>copy(int messageID, string folderName) -- Copies the specified message
 *       into the indicated folder.  This method returns a Boolean value
 *       indicating whether the message was copied successfully.</LI>
 *   <LI>countMessages() -- Retrieves the number of messages in the selected
 *       folder as an integer value.</LI>
 *   <LI>countUnreadMessages() -- Retrieves the number of unread in the selected
 *       folder as an integer value.</LI>
 *   <LI>createFolder(string folderName) -- Creates a new folder with the
 *       specified name.  This method returns a Boolean value indicating whether
 *       the folder was created successfully.</LI>
 *   <LI>delete(int messageID) -- Attempts to delete the specified message from
 *       the current mailbox in the IMAP server.  This method returns a Boolean
 *       value indicating whether the delete was successful.</LI>
 *   <LI>deleteFolder(string folderName) -- Deletes the folder with the
 *       specified name.  This method returns a Boolean value indicating whether
 *       the folder was deleted successfully.</LI>
 *   <LI>disconnect() -- Closes the connection to the IMAP server.  This method
 *       does not return a value.</LI>
 *   <LI>expunge() -- Indicates that the IMAP server should actually remove any
 *       deleted messages from the user's mailbox.  This method returns a
 *       Boolean value indicating whether the expunge was successful.</LI>
 *   <LI>getFailureReason() -- Retrieves a string that provides information
 *       about the reason for the last failure, if that is available.</LI>
 *   <LI>listFolders() -- Retrieves a list of all folders in the user's mailbox
 *        as a string array.</LI>
 *   <LI>listMessages() -- Retrieves a list of all messages in the selected
 *       folder in the user's mailbox as a string array.</LI>
 *   <LI>listNewMessages() -- Retrieves a list of unread messages in the
 *       selected folder in the user's mailbox as a string array.</LI>
 *   <LI>move(int messageID, string folderName) -- Moves the specified message
 *       from the current folder to the specified folder.  This method returns a
 *       Boolean value indicating whether the move was successful.</LI>
 *   <LI>noOp() -- Sends a NOOP command to the server, which has no effect but
 *       to prevent the connection from remaining idle for too long.  This
 *       method does not return a value.</LI>
 *   <LI>retrieve(int messageID) -- Retrieves the specified message from the
 *       user's mailbox.  The message will be returned as a mail message
 *       object.</LI>
 *   <LI>selectFolder(string folderName) -- Indicates that the specified folder
 *       should become the current folder for subsequent operations.  This
 *       method returns a Boolean value indicating whether the operation was
 *       successful.</LI>
 *   <LI>sendCommand(string command) -- Sends the specified command to the IMAP
 *       server and retrieves a string array containing the individual lines of
 *       the response from the server.  The command sent to the server should
 *       not include an identifier at the beginning, as an appropriate
 *       identifer will be added automatically.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class IMAPConnectionVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of IMAP connection variables.
   */
  public static final String IMAP_CONNECTION_VARIABLE_TYPE = "imapconnection";



  /**
   * The name of the method that performs an IMAP authentication.
   */
  public static final String AUTHENTICATE_METHOD_NAME = "authenticate";



  /**
   * The method number for the "authenticate" method.
   */
  public static final int AUTHENTICATE_METHOD_NUMBER = 0;



  /**
   * The name of the method that retrieves the capability list from the IMAP
   * server.
   */
  public static final String CAPABILITY_METHOD_NAME = "capability";



  /**
   * The method number for the "capability" method.
   */
  public static final int CAPABILITY_METHOD_NUMBER = 1;



  /**
   * The name of the method that establishes a connection to the IMAP server.
   */
  public static final String CONNECT_METHOD_NAME = "connect";



  /**
   * The method number for the first "connect" method.
   */
  public static final int CONNECT_1_METHOD_NUMBER = 2;



  /**
   * The method number for the second "connect" method.
   */
  public static final int CONNECT_2_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to copy a message into another
   * folder.
   */
  public static final String COPY_METHOD_NAME = "copy";



  /**
   * The method number for the "copy" method.
   */
  public static final int COPY_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to retrieve the number of messages
   * in the current folder.
   */
  public static final String COUNT_MESSAGES_METHOD_NAME = "countmessages";



  /**
   * The method number for the "countMessages" method.
   */
  public static final int COUNT_MESSAGES_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to retrieve the number of unread
   * messages in the current folder.
   */
  public static final String COUNT_UNREAD_MESSAGES_METHOD_NAME =
       "countunreadmessages";



  /**
   * The method number for the "countUnreadMessages" method.
   */
  public static final int COUNT_UNREAD_MESSAGES_METHOD_NUMBER = 6;



  /**
   * The name of the method that can be used to create a new mailbox folder.
   */
  public static final String CREATE_FOLDER_METHOD_NAME = "createfolder";



  /**
   * The method number for the "createFolder" method.
   */
  public static final int CREATE_FOLDER_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to delete a message from the IMAP
   * server.
   */
  public static final String DELETE_METHOD_NAME = "delete";



  /**
   * The method number for the "delete" method.
   */
  public static final int DELETE_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to delete a mailbox folder.
   */
  public static final String DELETE_FOLDER_METHOD_NAME = "deletefolder";



  /**
   * The method number for the "deleteFolder" method.
   */
  public static final int DELETE_FOLDER_METHOD_NUMBER = 9;



  /**
   * The name of the method that can be used to disconnect from the IMAP server.
   */
  public static final String DISCONNECT_METHOD_NAME = "disconnect";



  /**
   * The method number for the "disconnect" method.
   */
  public static final int DISCONNECT_METHOD_NUMBER = 10;



  /**
   * The name of the method that can be used to expunge deleted messages from
   * the mailbox.
   */
  public static final String EXPUNGE_METHOD_NAME = "expunge";



  /**
   * The method number for the "expunge" method.
   */
  public static final int EXPUNGE_METHOD_NUMBER = 11;



  /**
   * The name of the method that can be used to determine the reason for the
   * last failure.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 12;



  /**
   * The name of the method that can be used to retrieve a list of the folders
   * in a user's mailbox.
   */
  public static final String LIST_FOLDERS_METHOD_NAME = "listfolders";



  /**
   * The method number for the "listFolders" method.
   */
  public static final int LIST_FOLDERS_METHOD_NUMBER = 13;



  /**
   * The name of the method that can be used to retrieve a list of the messages
   * in the current folder of a user's mailbox.
   */
  public static final String LIST_MESSAGES_METHOD_NAME = "listmessages";



  /**
   * The method number for the "listMessages" method.
   */
  public static final int LIST_MESSAGES_METHOD_NUMBER = 14;



  /**
   * The name of the method that can be used to retrieve a list of the new
   * (unread) messages in the current folder of a user's mailbox.
   */
  public static final String LIST_NEW_MESSAGES_METHOD_NAME = "listnewmessages";



  /**
   * The method number for the "listNewMessages" method.
   */
  public static final int LIST_NEW_MESSAGES_METHOD_NUMBER = 15;



  /**
   * The name of the method that can be used to move a message from the current
   * folder to another folder in a user's mailbox.
   */
  public static final String MOVE_METHOD_NAME = "move";



  /**
   * The method number for the "move" method.
   */
  public static final int MOVE_METHOD_NUMBER = 16;



  /**
   * The name of the method that sends a "NOOP" to the server.
   */
  public static final String NOOP_METHOD_NAME = "noop";



  /**
   * The method number for the "noop" method.
   */
  public static final int NOOP_METHOD_NUMBER = 17;



  /**
   * The name of the method that can be used to retrieve a specified message
   * from the current folder of a user's mailbox.
   */
  public static final String RETRIEVE_METHOD_NAME = "retrieve";



  /**
   * The method number for the "retrieve" method.
   */
  public static final int RETRIEVE_METHOD_NUMBER = 18;



  /**
   * The name of the method that can be used to change the current folder for
   * the user.
   */
  public static final String SELECT_FOLDER_METHOD_NAME = "selectfolder";



  /**
   * The method number for the "selectFolder" method.
   */
  public static final int SELECT_FOLDER_METHOD_NUMBER = 19;



  /**
   * The name of the method that can be used to send a raw IMAP command to the
   * server.
   */
  public static final String SEND_COMMAND_METHOD_NAME = "sendcommand";



  /**
   * The method number for the "sendCommand" method.
   */
  public static final int SEND_COMMAND_METHOD_NUMBER = 20;



  /**
   * The set of methods associated with IMAP connection variables.
   */
  public static final Method[] IMAP_CONNECTION_VARIABLE_METHODS = new Method[]
  {
    new Method(AUTHENTICATE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CAPABILITY_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(COPY_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE},
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(COUNT_MESSAGES_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(COUNT_UNREAD_MESSAGES_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CREATE_FOLDER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DELETE_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DELETE_FOLDER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DISCONNECT_METHOD_NAME, new String[0], null),
    new Method(EXPUNGE_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(LIST_FOLDERS_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(LIST_MESSAGES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(LIST_NEW_MESSAGES_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(MOVE_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE},
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(NOOP_METHOD_NAME, new String[0], null),
    new Method(RETRIEVE_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               MailMessageVariable.MAIL_MESSAGE_VARIABLE_TYPE),
    new Method(SELECT_FOLDER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SEND_COMMAND_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE)
  };



  /**
   * The end of line character as required by RFC 3501.
   */
  public static final String EOL = "\r\n";



  // The socket, reader, and writer used to communicate with the IMAP server.
  private BufferedReader reader;
  private BufferedWriter writer;
  private Socket         socket;


  // The counter that will be used to increment the message ID used for IMAP
  // requests.
  private int messageIDCounter;


  // The reason that the last operation failed.
  private String failureReason;


  // The name of the currently selected folder.
  private String folderName;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public IMAPConnectionVariable()
         throws ScriptException
  {
    messageIDCounter = 0;
    failureReason    = null;
    folderName       = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return IMAP_CONNECTION_VARIABLE_TYPE;
  }



  /**
   * Retrieves the request ID that should be used for the next request sent to
   * the IMAP server.
   *
   * @return  The request ID that should be used for the next request sent to
   *          the IMAP server.
   */
  private String getRequestID()
  {
    return "a" + (messageIDCounter++);
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return IMAP_CONNECTION_VARIABLE_METHODS;
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
    for (int i=0; i < IMAP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (IMAP_CONNECTION_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < IMAP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (IMAP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < IMAP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (IMAP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
                                                           argumentTypes))
      {
        return IMAP_CONNECTION_VARIABLE_METHODS[i].getReturnType();
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
      case AUTHENTICATE_METHOD_NUMBER:
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        String userID = sv1.getStringValue();
        String userPW = sv2.getStringValue();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " login " + userID + ' ' + userPW + EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case CAPABILITY_METHOD_NUMBER:
        StringArrayVariable sav = new StringArrayVariable();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " capability" + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server";
              break;
            }

            if (line.startsWith("*"))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken(); // The initial asterisk
              tokenizer.nextToken(); // The word "capability"

              while (tokenizer.hasMoreTokens())
              {
                sav.addStringValue(tokenizer.nextToken());
              }
            }
            else if (line.startsWith(requestID))
            {
              break;
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return sav;
      case CONNECT_1_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        IntegerVariable iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        String address = sv1.getStringValue();
        int    port    = iv1.getIntValue();
        failureReason  = null;

        try
        {
          socket = new Socket(address, port);
          reader = new BufferedReader(new InputStreamReader(
                                               socket.getInputStream()));
          writer = new BufferedWriter(new OutputStreamWriter(
                                               socket.getOutputStream()));

          String line = reader.readLine();
          if ((line == null) || (line.length() == 0))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server";
            }
            else
            {
              failureReason = "No greeting line read from server";
            }
            reader.close();
            writer.close();
            socket.close();
            return new BooleanVariable(false);
          }

          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case CONNECT_2_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        BooleanVariable bv1 = (BooleanVariable) arguments[2].getArgumentValue();

        address        = sv1.getStringValue();
        port           = iv1.getIntValue();
        boolean useSSL = bv1.getBooleanValue();
        failureReason  = null;

        try
        {
          if (useSSL)
          {
            JSSEBlindTrustSocketFactory socketFactory =
                 new JSSEBlindTrustSocketFactory();
            socket = socketFactory.makeSocket(address, port);
          }
          else
          {
            socket = new Socket(address, port);
          }

          reader = new BufferedReader(new InputStreamReader(
                                               socket.getInputStream()));
          writer = new BufferedWriter(new OutputStreamWriter(
                                               socket.getOutputStream()));

          String line = reader.readLine();
          if ((line == null) || (line.length() == 0))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server";
            }
            else
            {
              failureReason = "No greeting line read from server";
            }
            reader.close();
            writer.close();
            socket.close();
            return new BooleanVariable(false);
          }

          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case COPY_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        sv1 = (StringVariable) arguments[1].getArgumentValue();
        int    messageID  = iv1.getIntValue();
        String folderName = sv1.getStringValue();
        failureReason     = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " copy " + messageID + ' ' + folderName +
                       EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case COUNT_MESSAGES_METHOD_NUMBER:
        failureReason = null;
        int numExists = -1;

        try
        {
          // First, find out how many messages there are in this folder.
          String requestID = getRequestID();
          writer.write(requestID + " status " + this.folderName +
                       " (messages)" + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.startsWith(requestID)))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }

            String lowerLine = line.toLowerCase();
            int messagesPos = lowerLine.indexOf("(messages ");
            if (messagesPos > 0)
            {
              int closePos = lowerLine.indexOf(')', messagesPos+10);
              numExists = Integer.parseInt(line.substring(messagesPos+10,
                                                          closePos));
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return new IntegerVariable(numExists);
      case COUNT_UNREAD_MESSAGES_METHOD_NUMBER:
        failureReason = null;
        numExists     = -1;

        try
        {
          // First, find out how many messages there are in this folder.
          String requestID = getRequestID();
          writer.write(requestID + " status " + this.folderName +
                       " (unseen)" + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.startsWith(requestID)))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }

            String lowerLine = line.toLowerCase();
            int messagesPos = lowerLine.indexOf("(unseen ");
            if (messagesPos > 0)
            {
              int closePos = lowerLine.indexOf(')', messagesPos+10);
              numExists = Integer.parseInt(line.substring(messagesPos+10,
                                                          closePos));
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return new IntegerVariable(numExists);
      case CREATE_FOLDER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        folderName    = sv1.getStringValue();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " create " + folderName + EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case DELETE_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        messageID     = iv1.getIntValue();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " store " + messageID +
                       " +flags (\\Deleted)" + EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case DELETE_FOLDER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        folderName    = sv1.getStringValue();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " delete " + folderName + EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              failureReason = "Unexpected end of input stream from server";
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case DISCONNECT_METHOD_NUMBER:
        try
        {
          writer.write(getRequestID() + " logout" + EOL);
          writer.flush();
        }
        catch (Exception e) {}

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        return null;
      case EXPUNGE_METHOD_NUMBER:
        try
        {
          failureReason = null;
          String requestID = getRequestID();
          writer.write(requestID + " expunge" + EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case GET_FAILURE_REASON_METHOD_NUMBER:
        return new StringVariable(failureReason);
      case LIST_FOLDERS_METHOD_NUMBER:
        failureReason = null;
        sav           = new StringArrayVariable();

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " list \"\" *" + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.startsWith(requestID)))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }

            if (line.startsWith("*"))
            {
              if (line.endsWith("\""))
              {
                int lastQuotePos = line.lastIndexOf('"', line.length()-2);
                sav.addStringValue(line.substring(lastQuotePos+1,
                                                  line.length()-1));
              }
              else
              {
                int lastSpacePos = line.lastIndexOf(' ');
                sav.addStringValue(line.substring(lastSpacePos+1));
              }
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return sav;
      case LIST_MESSAGES_METHOD_NUMBER:
        failureReason = null;
        sav           = new StringArrayVariable();

        try
        {
          // First, find out how many messages there are in this folder.
          String requestID = getRequestID();
          writer.write(requestID + " status " + this.folderName +
                       " (messages)" + EOL);
          writer.flush();

          numExists = 0;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.startsWith(requestID)))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }

            String lowerLine = line.toLowerCase();
            int messagesPos = lowerLine.indexOf("(messages ");
            if (messagesPos > 0)
            {
              int closePos = lowerLine.indexOf(')', messagesPos+10);
              numExists = Integer.parseInt(line.substring(messagesPos+10,
                                                          closePos));
            }
          }


          // Now that we know how many messages there are, retrieve them.
          if (numExists > 0)
          {
            requestID = getRequestID();
            writer.write(requestID + " fetch 1:" + numExists + " all" + EOL);
            writer.flush();

            while (true)
            {
              String line = reader.readLine();
              if ((line == null) || (line.startsWith(requestID)))
              {
                if (line == null)
                {
                  failureReason = "Unexpected end of input stream from server";
                }
                break;
              }
              else if (line.startsWith("*"))
              {
                sav.addStringValue(line);
              }
            }
          }
          else
          {
            failureReason = "Unable to determine number of existing messages";
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return sav;
      case LIST_NEW_MESSAGES_METHOD_NUMBER:
        failureReason = null;
        sav           = new StringArrayVariable();

        try
        {
          // First, find out how many messages there are in this folder.
          String requestID = getRequestID();
          writer.write(requestID + " search unseen" + EOL);
          writer.flush();

          ArrayList<String> unseenList = new ArrayList<String>();
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.startsWith(requestID)))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }

            if (line.startsWith("*"))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken(); // The asterisk
              tokenizer.nextToken(); // The word "search"

              while (tokenizer.hasMoreTokens())
              {
                unseenList.add(tokenizer.nextToken());
              }
            }
          }

          // Now that we have a specific message list, retrieve them
          // individually.
          for (int i=0; i < unseenList.size(); i++)
          {
            requestID = getRequestID();
            writer.write(requestID + " fetch " + unseenList.get(i) + " all" +
                         EOL);
            writer.flush();

            while (true)
            {
              String line = reader.readLine();
              if ((line == null) || (line.startsWith(requestID)))
              {
                break;
              }
              else if (line.startsWith("*"))
              {
                sav.addStringValue(line);
              }
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return sav;
      case MOVE_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        sv1 = (StringVariable) arguments[1].getArgumentValue();
        messageID     = iv1.getIntValue();
        folderName    = sv1.getStringValue();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " copy " + messageID + ' ' + folderName +
                       EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server";
              }
              else
              {
                failureReason = "Unexpected empty response from server";
              }
              break;
            }
            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
              }
              else
              {
                failureReason = "Unable to copy message:  " + line;
              }

              break;
            }
          }

          if (successful)
          {
            requestID = getRequestID();
            writer.write(requestID + " store " + messageID +
                         " +flags (\\Deleted)" + EOL);
            writer.flush();

            while (true)
            {
              String line = reader.readLine();
              if ((line == null) || (line.length() == 0))
              {
                if (line == null)
                {
                  failureReason = "Unexpected end of input stream from server";
                }
                else
                {
                  failureReason = "Unexpected empty response from server";
                }
                break;
              }
              if (line.startsWith(requestID))
              {
                StringTokenizer tokenizer = new StringTokenizer(line);
                tokenizer.nextToken();
                String result = tokenizer.nextToken();
                if (result.equalsIgnoreCase("ok"))
                {
                  successful = true;
                }
                else
                {
                  failureReason = "Unable to delete original message:  " + line;
                }

                break;
              }
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case NOOP_METHOD_NUMBER:
        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " noop" + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.startsWith(requestID)))
            {
              break;
            }
          }
        } catch (Exception e) {}

        return null;
      case RETRIEVE_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        messageID     = iv1.getIntValue();
        failureReason = null;
        MailMessageVariable mmv = new MailMessageVariable();

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " fetch " + messageID + " RFC822" + EOL);
          writer.flush();

          boolean blankLineSeen = false;
          while (true)
          {
            String line = reader.readLine();
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server";
              break;
            }
            else if (line.length() == 0)
            {
              blankLineSeen = true;
            }
            else if (line.startsWith("* " + messageID) || line.equals(")"))
            {
              // Ignore it -- beginning or end of message wrapper.
            }
            else if (line.startsWith(requestID + ' '))
            {
              if (! line.toLowerCase().contains("ok"))
              {
                failureReason = line;
              }
              break;
            }
            else if (blankLineSeen)
            {
              mmv.addBodyLine(line);
            }
            else
            {
              mmv.addHeaderLine(line);
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return mmv;
      case SELECT_FOLDER_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        folderName    = sv1.getStringValue();
        failureReason = null;

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + " select " + folderName + EOL);
          writer.flush();

          boolean successful = false;
          while (true)
          {
            String line = reader.readLine();
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server";
              break;
            }

            if (line.startsWith(requestID))
            {
              StringTokenizer tokenizer = new StringTokenizer(line);
              tokenizer.nextToken();
              String result = tokenizer.nextToken();
              if (result.equalsIgnoreCase("ok"))
              {
                successful = true;
                this.folderName = folderName;
              }
              else
              {
                failureReason = line;
              }

              break;
            }
          }

          return new BooleanVariable(successful);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case SEND_COMMAND_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        String command = sv1.getStringValue();
        failureReason  = null;

        sav = new StringArrayVariable();

        try
        {
          String requestID = getRequestID();
          writer.write(requestID + ' ' + command + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server";
              break;
            }

            sav.addStringValue(line);
            if (line.length() > 0)
            {
              char c = line.charAt(0);
              if (line.startsWith(requestID))
              {
                break;
              }
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return sav;
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
    if (! argument.getArgumentType().equals(IMAP_CONNECTION_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                IMAP_CONNECTION_VARIABLE_TYPE + " rejected.");
    }

    IMAPConnectionVariable icv =
         (IMAPConnectionVariable) argument.getArgumentValue();
    socket = icv.socket;
    reader = icv.reader;
    writer = icv.writer;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (socket == null)
    {
      return "null";
    }
    else
    {
      boolean connected = socket.isConnected();

      if (! connected)
      {
        return "not connected";
      }
      else
      {
        String  host     = socket.getInetAddress().getHostAddress();
        int     port     = socket.getPort();
        boolean usingSSL = (socket instanceof SSLSocket);

        if (usingSSL)
        {
          return "imaps://" + host + ':' + port;
        }
        else
        {
          return "imap://" + host + ':' + port;
        }
      }
    }
  }
}

