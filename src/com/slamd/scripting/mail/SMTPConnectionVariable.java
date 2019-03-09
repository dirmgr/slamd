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
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;

import com.unboundid.util.Base64;

import com.slamd.asn1.ASN1Element;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.job.JobClass;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that maintains a connection to an SMTP mail
 * server and allows for interaction with that server.  An SMTP connection has
 * the following methods:
 *
 * <UL>
 *   <LI>authCRAMMD5(string username, string password) -- Authenticates to the
 *       SMTP server using the CRAM-MD5 mechanism.  Returns a Boolean value
 *       indicating whether the authentication was successful.</LI>
 *   <LI>authLogin(string username, string password) -- Authenticates to the
 *       SMTP server using the AUTH LOGIN mechanism.  Returns a Boolean value
 *       indicating whether the authentication was successful.</LI>
 *   <LI>authPlain(string username, string password) -- Authenticates to the
 *       SMTP server using the AUTH PLAIN mechanism.  Returns a Boolean value
 *       indicating whether the authentication was successful.</LI>
 *   <LI>connect(string host, int port) -- Establishes an SMTP connection to the
 *       mail server.  Returns a Boolean value indicating whether the connection
 *       was established successfully.</LI>
 *   <LI>connect(string host, int port, boolean useSSL) -- Establishes an SMTP
 *       connection to the mail server, optionally using SSL.  Returns a Boolean
 *       value indicating whether the connection was established
 *       successfully.</LI>
 *   <LI>disconnect() -- Closes the connection to the SMTP server.  This method
 *       does not return a value.</LI>
 *   <LI>getFailureReason() -- Retrieves a string that provides information
 *       about the reason for the last failure, if that is available.</LI>
 *   <LI>noOp() -- Sends a NOOP command to the server, which has no effect but
 *       to prevent the connection from remaining idle for too long.  This
 *       method does not return a value.</LI>
 *   <LI>send(MailMessage message) -- Sends the provided mail message to the
 *       server.  This method returns a Boolean value indicating whether the
 *       message was sent successfully.</LI>
 *   <LI>sendCommand(string command) -- Sends the specified command to the SMTP
 *       server and retrieves the response line from the server.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class SMTPConnectionVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of SMTP connection variables.
   */
  public static final String SMTP_CONNECTION_VARIABLE_TYPE = "smtpconnection";



  /**
   * The name of the method that can be used to authenticate to the server using
   * CRAM-MD5.
   */
  public static final String AUTH_CRAM_MD5_METHOD_NAME = "authcrammd5";



  /**
   * The method number for the "authCRAMMD5" method.
   */
  public static final int AUTH_CRAM_MD5_METHOD_NUMBER = 0;



  /**
   * The name of the method that can be used to authenticate to the server using
   * AUTH LOGIN.
   */
  public static final String AUTH_LOGIN_METHOD_NAME = "authlogin";



  /**
   * The method number for the "authLogin" method.
   */
  public static final int AUTH_LOGIN_METHOD_NUMBER = 1;



  /**
   * The name of the method that can be used to authenticate to the server using
   * AUTH PLAIN.
   */
  public static final String AUTH_PLAIN_METHOD_NAME = "authplain";



  /**
   * The method number for the "authPlain" method.
   */
  public static final int AUTH_PLAIN_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to establish a connection to an
   * SMTP server.
   */
  public static final String CONNECT_METHOD_NAME = "connect";



  /**
   * The method number for the first "connect" method.
   */
  public static final int CONNECT_1_METHOD_NUMBER = 3;



  /**
   * The method number for the second "connect" method.
   */
  public static final int CONNECT_2_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to disconnect from an SMTP server.
   */
  public static final String DISCONNECT_METHOD_NAME = "disconnect";



  /**
   * The method number for the "disconnect" method.
   */
  public static final int DISCONNECT_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to determine the reason that the
   * last operation failed.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 6;



  /**
   * The name of the method that sends a "NOOP" to the server.
   */
  public static final String NOOP_METHOD_NAME = "noop";



  /**
   * The method number for the "noOp" method.
   */
  public static final int NOOP_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to send a message to the SMTP
   * server.
   */
  public static final String SEND_METHOD_NAME = "send";



  /**
   * The method number for the "send" method.
   */
  public static final int SEND_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to send a raw SMTP command to the
   * server.
   */
  public static final String SEND_COMMAND_METHOD_NAME = "sendcommand";



  /**
   * The method number for the "sendCommand" method.
   */
  public static final int SEND_COMMAND_METHOD_NUMBER = 9;



  /**
   * The set of methods associated with SMTP connection variables.
   */
  public static final Method[] SMTP_CONNECTION_VARIABLE_METHODS = new Method[]
  {
    new Method(AUTH_CRAM_MD5_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(AUTH_LOGIN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(AUTH_PLAIN_METHOD_NAME,
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
    new Method(DISCONNECT_METHOD_NAME, new String[0], null),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(NOOP_METHOD_NAME, new String[0], null),
    new Method(SEND_METHOD_NAME,
               new String[] { MailMessageVariable.MAIL_MESSAGE_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SEND_COMMAND_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE)
  };



  /**
   * The end of line character as required by RFC 821.
   */
  public static final String EOL = "\r\n";



  // The socket, reader, and writer used to communicate with the SMTP server.
  private BufferedReader reader;
  private BufferedWriter writer;
  private Socket         socket;


  // The reason that the last operation failed.
  private String failureReason;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public SMTPConnectionVariable()
         throws ScriptException
  {
    failureReason = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return SMTP_CONNECTION_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return SMTP_CONNECTION_VARIABLE_METHODS;
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
    for (int i=0; i < SMTP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (SMTP_CONNECTION_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < SMTP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (SMTP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < SMTP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (SMTP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
                                                           argumentTypes))
      {
        return SMTP_CONNECTION_VARIABLE_METHODS[i].getReturnType();
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
      case AUTH_CRAM_MD5_METHOD_NUMBER:
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        failureReason = null;

        try
        {
          writer.write("AUTH CRAM-MD5" + EOL);
          writer.flush();

          String line = reader.readLine();
          if (! line.startsWith("334 "))
          {
            failureReason = "Server rejected AUTH CRAM-MD5 (response was " +
                            line + ')';
            return new BooleanVariable(false);
          }

          String challenge = line.substring(4).trim();
          CRAMMD5Handler handler = new CRAMMD5Handler();
          String response =
               handler.generateCRAMMD5Response(sv1.getStringValue(),
                                               sv2.getStringValue(), challenge);
          writer.write(response + EOL);
          writer.flush();

          line = reader.readLine();
          if (! line.startsWith("235 "))
          {
            failureReason = "Server rejected CRAM-MD5 challenge response " +
                            "(response was " + line + ')';
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
      case AUTH_LOGIN_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();
        failureReason = null;

        try
        {
          writer.write("AUTH LOGIN" + EOL);
          writer.flush();

          String line = reader.readLine();
          if (! line.startsWith("334 "))
          {
            failureReason = "Server rejected AUTH LOGIN (response was " +
                            line + ')';
            return new BooleanVariable(false);
          }

          byte[] usernameBytes  = ASN1Element.getBytes(sv1.getStringValue());
          String usernameString = Base64.encode(usernameBytes);
          writer.write(usernameString + EOL);
          writer.flush();

          line = reader.readLine();
          if (! line.startsWith("334 "))
          {
            failureReason = "Server rejected username (response was " +
                            line + ')';
            return new BooleanVariable(false);
          }

          byte[] passwordBytes  = ASN1Element.getBytes(sv2.getStringValue());
          String passwordString = Base64.encode(passwordBytes);
          writer.write(passwordString + EOL);
          writer.flush();

          line = reader.readLine();
          if (line.startsWith("235 "))
          {
            return new BooleanVariable(true);
          }
          else
          {
            failureReason = "Server rejected credentials (response was " +
                            line + ')';
            return new BooleanVariable(false);
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }
      case AUTH_PLAIN_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();
        failureReason = null;

        try
        {
          writer.write("AUTH PLAIN" + EOL);
          writer.flush();

          String line = reader.readLine();
          if (! line.startsWith("334 "))
          {
            failureReason = "Server rejected AUTH PLAIN (response was " +
                            line + ')';
            return new BooleanVariable(false);
          }

          byte[] usernameBytes = ASN1Element.getBytes(sv1.getStringValue());
          byte[] passwordBytes = ASN1Element.getBytes(sv2.getStringValue());
          byte[] authBytes = new byte[usernameBytes.length +
                                      passwordBytes.length + 2];
          System.arraycopy(usernameBytes, 0, authBytes, 1,
                           usernameBytes.length);
          System.arraycopy(passwordBytes, 0, authBytes, usernameBytes.length+2,
                           passwordBytes.length);
          String authString = Base64.encode(authBytes);

          writer.write(authString + EOL);
          writer.flush();

          line = reader.readLine();
          if (line.startsWith("235 "))
          {
            return new BooleanVariable(true);
          }
          else
          {
            failureReason = "Server rejected credentials (response was " +
                            line + ')';
            return new BooleanVariable(false);
          }
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
          if ((line == null) || (! line.startsWith("220")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading greeting";
            }
            else
            {
              failureReason = "Error reading greeting: " + line;
            }
            reader.close();
            writer.close();
            socket.close();
            return new BooleanVariable(false);
          }

          String clientAddress = InetAddress.getLocalHost().getHostAddress();
          writer.write("HELO [" + clientAddress + ']' + EOL);
          writer.flush();

          line = reader.readLine();
          if ((line == null) || (! line.startsWith("250")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading HELO response";
            }
            else
            {
              failureReason = "Error reading HELO response: " + line;
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
          if ((line == null) || (! line.startsWith("220")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading greeting";
            }
            else
            {
              failureReason = "Error reading greeting: " + line;
            }
            reader.close();
            writer.close();
            socket.close();
            return new BooleanVariable(false);
          }

          String clientAddress = InetAddress.getLocalHost().getHostAddress();
          writer.write("HELO [" + clientAddress + ']' + EOL);
          writer.flush();

          line = reader.readLine();
          if ((line == null) || (! line.startsWith("250")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading HELO response";
            }
            else
            {
              failureReason = "Error reading HELO response: " + line;
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
      case DISCONNECT_METHOD_NUMBER:
        failureReason = null;

        try
        {
          writer.write("QUIT" + EOL);
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
      case NOOP_METHOD_NUMBER:
        failureReason = null;

        try
        {
          writer.write("QUIT" + EOL);
          writer.flush();
          reader.readLine();
        }
        catch (Exception e) {}

        return null;
      case SEND_METHOD_NUMBER:
        MailMessageVariable mmv =
             (MailMessageVariable) arguments[0].getArgumentValue();
        failureReason = null;

        // First, make sure that we have a sender and at least 1 recipient.
        String   sender     = mmv.getSender();
        String[] recipients = mmv.getRecipients();
        if ((sender == null) || (recipients == null) ||
            (recipients.length == 0))
        {
          if (sender == null)
          {
            failureReason = "Message has no sender";
          }
          else
          {
            failureReason = "Message has no recipients";
          }

          return new BooleanVariable(false);
        }


        // Next, send the MAIL FROM command and read the response.
        try
        {
          writer.write("MAIL FROM: ");
          if (sender.startsWith("<"))
          {
            writer.write(sender);
          }
          else
          {
            writer.write("<");
            writer.write(sender);
            writer.write(">");
          }

          writer.write(EOL);
          writer.flush();

          String line = reader.readLine();
          if ((line == null) || (! line.startsWith("250")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading MAIL FROM response";
            }
            else
            {
              failureReason = "Error reading MAIL FROM response:  " + line;
            }

            writer.write("RSET" + EOL);
            writer.flush();
            reader.readLine();

            return new BooleanVariable(false);
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception handling MAIL FROM:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }


        // Next, send a RCPT TO command for each recipient.
        try
        {
          for (int i=0; i < recipients.length; i++)
          {
            writer.write("RCPT TO: ");
            if (recipients[i].startsWith("<"))
            {
              writer.write(recipients[i]);
            }
            else
            {
              writer.write("<");
              writer.write(recipients[i]);
              writer.write(">");
            }

            writer.write(EOL);
            writer.flush();

            String line = reader.readLine();
            if ((line == null) || (! line.startsWith("25")))
            {
              if (line == null)
              {
                failureReason = "Unexpected end of input stream from server " +
                                "reading RCPT TO response";
              }
              else
              {
                failureReason = "Error reading RCPT TO response:  " + line;
              }

              writer.write("RSET" + EOL);
              writer.flush();
              reader.readLine();

              return new BooleanVariable(false);
            }
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception handling RCPT TO:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }


        // Next comes the DATA command for the actual message.
        try
        {
          writer.write("DATA" + EOL);
          writer.flush();

          String line = reader.readLine();
          if ((line == null) || (! line.startsWith("354")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading DATA response";
            }
            else
            {
              failureReason = "Error reading DATA response " + line;
            }

            writer.write("RSET" + EOL);
            writer.flush();
            reader.readLine();

            return new BooleanVariable(false);
          }

          ArrayList headerLines = mmv.getHeaderLines();
          if ((headerLines != null) && (! headerLines.isEmpty()))
          {
            for (int i=0; i < headerLines.size(); i++)
            {
              writer.write(headerLines.get(i) + EOL);
            }

            writer.write(EOL);
          }

          ArrayList bodyLines   = mmv.getBodyLines();
          if ((bodyLines != null) && (! bodyLines.isEmpty()))
          {
            for (int i=0; i < bodyLines.size(); i++)
            {
              line = (String) bodyLines.get(i);
              if (line.startsWith("."))
              {
                writer.write(".");
              }

              writer.write(line + EOL);
            }
          }

          writer.write('.' + EOL);
          writer.flush();

          line = reader.readLine();
          if ((line == null) || (! line.startsWith("250")))
          {
            if (line == null)
            {
              failureReason = "Unexpected end of input stream from server " +
                              "reading end of message response";
            }
            else
            {
              failureReason = "Error reading end of message response " + line;
            }

            writer.write("RSET" + EOL);
            writer.flush();
            reader.readLine();

            return new BooleanVariable(false);
          }
        }
        catch (Exception e)
        {
          failureReason = "Caught exception while processing DATA:  " +
                          JobClass.stackTraceToString(e);
          return new BooleanVariable(false);
        }

        return new BooleanVariable(true);
      case SEND_COMMAND_METHOD_NUMBER:
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        String command = sv1.getStringValue();
        failureReason  = null;

        try
        {
          writer.write(command);
          writer.flush();
          return new StringVariable(reader.readLine());
        }
        catch (Exception e)
        {
          failureReason = "Caught exception:  " +
                          JobClass.stackTraceToString(e);
        }

        return new StringVariable(null);
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
    if (! argument.getArgumentType().equals(SMTP_CONNECTION_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                SMTP_CONNECTION_VARIABLE_TYPE + " rejected.");
    }

    SMTPConnectionVariable scv =
         (SMTPConnectionVariable) argument.getArgumentValue();
    socket = scv.socket;
    reader = scv.reader;
    writer = scv.writer;
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
          return "smtps://" + host + ';' + port;
        }
        else
        {
          return "smtp://" + host + ':' + port;
        }
      }
    }
  }
}

