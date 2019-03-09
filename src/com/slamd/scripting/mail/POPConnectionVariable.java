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
 * This class defines a variable that maintains a connection to a POP3 mail
 * server and allows for interaction with that server.  A POP connection has the
 * the following methods:
 *
 * <UL>
 *   <LI>authenticate(string userID, string password) -- Authenticates to the
 *       POP3 server.  This method returns a Boolean value indicating whether
 *       the authentication was successful.</LI>
 *   <LI>connect(string host, int port) -- Establishes a POP3 connection to the
 *       mail server.  Returns a Boolean value indicating whether the connection
 *       was established successfully.</LI>
 *   <LI>connect(string host, int port, boolean useSSL) -- Establishes a POP3
 *       connection to the mail server, optionally using SSL.  Returns a Boolean
 *       value indicating whether the connection was established
 *       successfully.</LI>
 *   <LI>delete(int messageID) -- Attempts to delete the specified message from
 *       the POP3 server.  This method returns a Boolean value indicating
 *       whether the delete was successful.</LI>
 *   <LI>disconnect() -- Closes the connection to the POP3 server.  This method
 *       does not return a value.</LI>
 *   <LI>getFailureReason() -- Retrieves a string that provides information
 *       about the reason for the last failure, if that is available.</LI>
 *   <LI>list() -- Retrieves a list of the messages contained in the user's
 *       inbox.  This method returns a string array containing the lines of
 *       output from the list command.</LI>
 *   <LI>noOp() -- Sends a NOOP command to the server, which has no effect but
 *       to prevent the connection from remaining idle for too long.  This
 *       method does not return a value.</LI>
 *   <LI>retrieve(int messageID) -- Retrieves the specified message from the
 *       user's inbox.  The message will be returned as a mail message
 *       object.</LI>
 *   <LI>stat() -- Retrieves the number of messages contained in the user's
 *       inbox.  This method will return an integer indicating the number of
 *       messages in the inbox.</LI>
 *   <LI>top(int messageID, int lines) -- Retrieves the indicated number of
 *       lines from the top of the specified message.  This method will return
 *       a string array containing the requested lines.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class POPConnectionVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of POP connection variables.
   */
  public static final String POP_CONNECTION_VARIABLE_TYPE = "popconnection";



  /**
   * The name of the method that performs a POP3 authentication.
   */
  public static final String AUTHENTICATE_METHOD_NAME = "authenticate";



  /**
   * The method number for the "authenticate" method.
   */
  public static final int AUTHENTICATE_METHOD_NUMBER = 0;



  /**
   * The name of the method that establishes a connection to the POP server.
   */
  public static final String CONNECT_METHOD_NAME = "connect";



  /**
   * The method number for the first "connect" method.
   */
  public static final int CONNECT_1_METHOD_NUMBER = 1;



  /**
   * The method number for the second "connect" method.
   */
  public static final int CONNECT_2_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to delete a message from the POP3
   * server.
   */
  public static final String DELETE_METHOD_NAME = "delete";



  /**
   * The method number for the "delete" method.
   */
  public static final int DELETE_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to disconnect from the POP3 server.
   */
  public static final String DISCONNECT_METHOD_NAME = "disconnect";



  /**
   * The method number for the "disconnect" method.
   */
  public static final int DISCONNECT_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to determine the reason for the
   * last failure.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to list the messages in the POP3
   * server.
   */
  public static final String LIST_METHOD_NAME = "list";



  /**
   * The method number for the "list" method.
   */
  public static final int LIST_METHOD_NUMBER = 6;



  /**
   * The name of the method that sends a "NOOP" to the server.
   */
  public static final String NOOP_METHOD_NAME = "noop";



  /**
   * The method number for the "noop" method.
   */
  public static final int NOOP_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to retrieve a message in the POP3
   * server.
   */
  public static final String RETRIEVE_METHOD_NAME = "retrieve";



  /**
   * The method number for the "retrieve" method.
   */
  public static final int RETRIEVE_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to determine the number of messages
   * in the user's inbox.
   */
  public static final String STAT_METHOD_NAME = "stat";



  /**
   * The method number for the "stat" method.
   */
  public static final int STAT_METHOD_NUMBER = 9;



  /**
   * The name of the method that can be used to retrieve the top portion of a
   * mail message.
   */
  public static final String TOP_METHOD_NAME = "top";



  /**
   * The method number for the "top" method.
   */
  public static final int TOP_METHOD_NUMBER = 10;



  /**
   * The set of methods associated with POP connection variables.
   */
  public static final Method[] POP_CONNECTION_VARIABLE_METHODS = new Method[]
  {
    new Method(AUTHENTICATE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DELETE_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DISCONNECT_METHOD_NAME, new String[0], null),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(LIST_METHOD_NAME, new String[0],
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE),
    new Method(NOOP_METHOD_NAME, new String[0], null),
    new Method(RETRIEVE_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               MailMessageVariable.MAIL_MESSAGE_VARIABLE_TYPE),
    new Method(STAT_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(TOP_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE},
               StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE)
  };



  /**
   * The end of line character as required by RFC 1725.
   */
  public static final String EOL = "\r\n";



  // The socket, reader, and writer used to communicate with the POP3 server.
  private BufferedReader reader;
  private BufferedWriter writer;
  private Socket         socket;


  // The reason for the last failure experienced.
  private String failureReason;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public POPConnectionVariable()
         throws ScriptException
  {
    // No implementation required.
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return POP_CONNECTION_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return POP_CONNECTION_VARIABLE_METHODS;
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
    for (int i=0; i < POP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (POP_CONNECTION_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < POP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (POP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < POP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (POP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
                                                          argumentTypes))
      {
        return POP_CONNECTION_VARIABLE_METHODS[i].getReturnType();
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
          writer.write("user " + userID + EOL);
          writer.flush();

          String line = reader.readLine();
          if ((line == null) || (line.length() == 0) ||
              (line.charAt(0) != '+'))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading USER response";
            }
            else if (line.length() == 0)
            {
              failureReason = "Unexpected empty response to USER command";
            }
            else
            {
              failureReason = "Error response to USER command:  " + line;
            }

            return new BooleanVariable(false);
          }

          writer.write("pass " + userPW + EOL);
          writer.flush();

          line = reader.readLine();
          if ((line == null) || (line.length() == 0) ||
              (line.charAt(0) != '+'))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading PASS response";
            }
            else if (line.length() == 0)
            {
              failureReason = "Unexpected empty response to PASS command";
            }
            else
            {
              failureReason = "Error response to PASS command:  " + line;
            }

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
          if ((line == null) || (line.length() == 0) ||
              (line.charAt(0) != '+'))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading greeting line";
            }
            else if (line.length() == 0)
            {
              failureReason = "Unexpected empty response to greeting line";
            }
            else
            {
              failureReason = "Error response on greeting line:  " + line;
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
          if ((line == null) || (line.length() == 0) ||
              (line.charAt(0) != '+'))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading greeting line";
            }
            else if (line.length() == 0)
            {
              failureReason = "Unexpected empty response to greeting line";
            }
            else
            {
              failureReason = "Error response on greeting line:  " + line;
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
      case DELETE_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        int messageID = iv1.getIntValue();
        failureReason = null;

        try
        {
          writer.write("dele " + messageID + EOL);
          writer.flush();

          String line = reader.readLine();
          if ((line == null) || (line.length() == 0) ||
              (line.charAt(0) != '+'))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading DELE response";
            }
            else if (line.length() == 0)
            {
              failureReason = "Unexpected empty response to DELE command";
            }
            else
            {
              failureReason = "Error response to DELE command:  " + line;
            }

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
      case DISCONNECT_METHOD_NUMBER:
        failureReason = null;
        try
        {
          writer.write("quit" + EOL);
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
      case GET_FAILURE_REASON_METHOD_NUMBER:
        return new StringVariable(failureReason);
      case LIST_METHOD_NUMBER:
        StringArrayVariable sav = new StringArrayVariable();
        failureReason = null;

        try
        {
          writer.write("list" + EOL);
          writer.flush();

          while (true)
          {
            String line = reader.readLine();
            if ((line == null) || (line.length() == 0) ||
                (line.charAt(0) == '-') || (line.charAt(0) == '.'))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server " +
                                "while reading LIST response";
              }
              else if (line.length() == 0)
              {
                failureReason = "Unexpected empty response to LIST command";
              }
              else if (line.charAt(0) == '-')
              {
                failureReason = "Error response to LIST command:  " + line;
              }

              break;
            }
            else if (line.charAt(0) != '+')
            {
              sav.addStringValue(line);
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return sav;
      case NOOP_METHOD_NUMBER:
        failureReason = null;

        try
        {
          writer.write("noop" + EOL);
          writer.flush();
          reader.readLine();
        } catch (Exception e) {}

        return null;
      case RETRIEVE_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        messageID = iv1.getIntValue();
        MailMessageVariable mmv = new MailMessageVariable();
        failureReason = null;

        try
        {
          writer.write("retr " + messageID + EOL);
          writer.flush();

          String line = reader.readLine();
          if (line == null)
          {
            failureReason = "Unexpected end of input stream from server " +
                            "while reading RETR response";
            return mmv;
          }
          else if (line.charAt(0) != '+')
          {
            failureReason = line;
            return mmv;
          }

          boolean blankSeen = false;
          while (true)
          {
            line = reader.readLine();
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading RETR response";
              break;
            }
            else if ((line.length() == 0) && (! blankSeen))
            {
              blankSeen = true;
            }
            else if (line.equals("."))
            {
              break;
            }
            else
            {
              if (blankSeen)
              {
                mmv.addBodyLine(line);
              }
              else
              {
                mmv.addHeaderLine(line);
              }
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return mmv;
      case STAT_METHOD_NUMBER:
        try
        {
          failureReason = null;
          writer.write("stat" + EOL);
          writer.flush();

          String line = reader.readLine();
          if ((line == null) || (line.length() == 0) ||
              (line.charAt(0) != '+'))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "while reading STAT response";
            }
            else if (line.length() == 0)
            {
              failureReason = "Unexpected empty response to STAT command";
            }
            else
            {
              failureReason = "Error response to STAT command:  " + line;
            }

            return new IntegerVariable(-1);
          }

          int space1      = line.indexOf(' ');
          int space2      = line.indexOf(' ', space1+1);
          int returnValue = Integer.parseInt(line.substring(space1+1, space2));
          return new IntegerVariable(returnValue);
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new IntegerVariable(-1);
        }
      case TOP_METHOD_NUMBER:
        iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        int numLines = iv2.getIntValue();
        messageID    = iv1.getIntValue();
        sav          = new StringArrayVariable();

        try
        {
          writer.write("top " + messageID + ' ' + numLines + EOL);
          writer.flush();

          String line = reader.readLine();
          if (line == null)
          {
            failureReason = "Unexpected end of input stream from server " +
                            "while reading RETR response";
            return sav;
          }
          else if (line.charAt(0) != '+')
          {
            failureReason = line;
            return sav;
          }

          boolean blankSeen = false;
          while (true)
          {
            line = reader.readLine();
            if (line == null)
            {
              break;
            }
            else if ((line.length() == 0) && (! blankSeen))
            {
              blankSeen = true;
            }
            else if (line.charAt(0) == '.')
            {
              break;
            }
            else if ((line.charAt(0) != '+') && (blankSeen))
            {
              sav.addStringValue(line);
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
    if (! argument.getArgumentType().equals(POP_CONNECTION_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                POP_CONNECTION_VARIABLE_TYPE + " rejected.");
    }

    POPConnectionVariable pcv =
         (POPConnectionVariable) argument.getArgumentValue();
    socket = pcv.socket;
    reader = pcv.reader;
    writer = pcv.writer;
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
          return "pops://" + host + ':' + port;
        }
        else
        {
          return "pop://" + host + ':' + port;
        }
      }
    }
  }
}

