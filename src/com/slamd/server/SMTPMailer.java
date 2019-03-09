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
package com.slamd.server;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.slamd.common.Constants;
import com.slamd.db.SLAMDDB;
import com.slamd.job.JobClass;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.StringParameter;



/**
 * This class provides a means of sending an e-mail message over SMTP as defined
 * in RFC 821.
 *
 *
 * @author   Neil A. Wilson
 */
public class SMTPMailer
       implements ConfigSubscriber
{
  /**
   * The name used to register the mailer as a subscriber to the configuration
   * handler.
   */
  public static final String CONFIG_SUBSCRIBER_NAME = "SLAMD Mailer";



  // Indicates whether the mailer will be enabled for use in the SLAMD server.
  private boolean enableMailer;

  // The port number to use to contact the SMTP server when sending mail.
  private int mailPort;

  // The date formatter that will be used to format dates in the RFC-822 format.
  private SimpleDateFormat dateFormat;

  // The configuration database with which this mailer is associated.
  private SLAMDDB configDB;

  // The SLAMD server with which this mailer is associated.
  private SLAMDServer slamdServer;

  // The base URI for the SLAMD server's admin interface.
  private String servletBaseURI;

  // The address from which mail messages will be sent.
  private String fromAddress;

  // The address of this system, which will be sent in HELO messages.
  private String localAddress;

  // The address of the mail server to which admin alert messages will be sent.
  private String mailHost;



  /**
   * Creates a new instance of this SMTP mailer.
   *
   * @param  slamdServer  The SLAMD server instance with which this mailer is
   *                      associated.
   */
  public SMTPMailer(SLAMDServer slamdServer)
  {
    this.slamdServer = slamdServer;
    configDB         = slamdServer.getConfigDB();

    dateFormat = new SimpleDateFormat(Constants.MAIL_DATE_FORMAT);

    // Set default values for all the configurable parameters.
    enableMailer   = false;
    mailPort       = Constants.DEFAULT_SMTP_PORT;
    mailHost       = "";
    fromAddress    = "";
    servletBaseURI = "";

    configDB.registerAsSubscriber(this);
    refreshSubscriberConfiguration();

    // Get the local address that will be used in HELO messages.
    try
    {
      localAddress = InetAddress.getLocalHost().getHostName();
    }
    catch (IOException ioe)
    {
      try
      {
        localAddress = InetAddress.getLocalHost().getHostAddress();
      }
      catch (IOException ioe2)
      {
        localAddress = "slamd_server";
      }
    }
  }



  /**
   * Indicates whether this mailer is currently enabled.
   *
   * @return  <CODE>true</CODE> if this mailer is enabled, or <CODE>false</CODE>
   *          if it is not.
   */
  public boolean isEnabled()
  {
    return enableMailer;
  }



  /**
   * Retrieves the URL that can be used to access the admin interface.
   *
   * @return  The URL that can be used to access the admin interface.
   */
  public String getServletBaseURI()
  {
    return servletBaseURI;
  }



  /**
   * Sends a mail message to all configured recipients to notify them that the
   * specified critical event has occurred.
   *
   * @param  recipients  The addresses of the recipients to which the message
   *                     should be sent.
   * @param  subject     The subject to use for the message.
   * @param  message     The body of the mail message to be sent.
   */
  public void sendMessage(String[] recipients, String subject, String message)
  {
    if (! enableMailer)
    {
      return;
    }

    try
    {
      // Open the connection to the SMTP server.
      Socket socket = new Socket(mailHost, mailPort);
      BufferedReader reader =
           new BufferedReader(new InputStreamReader(socket.getInputStream()));
      BufferedWriter writer =
           new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));


      // The SMTP server should first introduce itself to the client.  Make sure
      // that the introduction is acceptable -- if so then it should start with
      // the number "220".
      String serverResponse = readLine(reader);
      if (! serverResponse.startsWith("220"))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to send mail message -- mail server " +
                               "provided an invalid hello message.");
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
        return;
      }


      // Send a "HELO" request to the server and read the response.  Make sure
      // that the response starts with a "250".
      sendLine(writer, "HELO " + localAddress);
      serverResponse = readLine(reader);
      if (! serverResponse.startsWith("250"))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to send mail message -- mail server " +
                               "provided an invalid hello response.");
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
        return;
      }


      // Specify the from address.  The server must provide a response starting
      // with "250" for this to be acceptable.
      sendLine(writer, "MAIL FROM:<" + fromAddress + '>');
      serverResponse = readLine(reader);
      if (! serverResponse.startsWith("250"))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to send mail message -- mail server " +
                               "provided an invalid MAIL FROM response " +
                               "message.");
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
        return;
      }

      // Specify the recipients.  The server should provide a response starting
      // with "250" or "251" for each of them.
      boolean onePassed = false;
      for (int i=0; i < recipients.length; i++)
      {
        sendLine(writer, "RCPT TO:<" + recipients[i] + '>');
        serverResponse = readLine(reader);
        if (! serverResponse.startsWith("25"))
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                                 "Unable to send mail message to " +
                                 recipients[i] + " -- mail server did not " +
                                 "accept that recipient address.");
        }
        else
        {
          onePassed = true;
        }
      }

      if (! onePassed)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to send mail message -- mail server " +
                               "rejected all recipient addresses.");
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
        return;
      }


      // Send the "DATA" header to the server.  The server must provide a
      // response starting with "354".
      sendLine(writer, "DATA");
      serverResponse = readLine(reader);
      if (! serverResponse.startsWith("354"))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to send mail message -- mail server " +
                               "returned an invalid DATA intermediate " +
                               "response message.");
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
        return;
      }


      // Send the message header.  The server will not provide a response to
      // this.
      sendLine(writer, "From: <" + fromAddress + '>');
      sendLine(writer, "MIME-Version: 1.0");
      sendLine(writer, "Content-type: text/plain; charset=us-ascii");
      sendLine(writer, "Date: " + dateFormat.format(new Date()));
      sendLine(writer, "Subject: " + subject);
      for (int i=0; i < recipients.length; i++)
      {
        sendLine(writer, "To: <" + recipients[i] + '>');
      }
      sendLine(writer, "");


      // Send the message body, followed by a line containing only a period.
      // The server should provide a response starting with "250".
      sendLine(writer, message);
      sendLine(writer, "");
      sendLine(writer, ".");
      serverResponse = readLine(reader);
      if (! serverResponse.startsWith("250"))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to send mail message -- mail server " +
                               "provided an invalid DATA complete response " +
                               "message.");
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
        return;
      }


      // The message is complete, so end the session with a "QUIT".
      sendLine(writer, "QUIT");
      writer.close();
      reader.close();
      socket.close();
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                             "Unable to send mail message -- I/O error wile " +
                             "interacting with the server:  " + ioe);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                             "Unable to send mail message -- uncaught " +
                             "exception while interacting with the server:  " +
                             e);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
    }
  }



  /**
   * Writes the provided message to the SMTP server, appending the correct
   * end-of-line character, and flushing the buffer to ensure that it is sent.
   *
   * @param  writer  The buffered writer to use to send the text.
   * @param  line    The line of text to be sent to the server.
   *
   * @throws  IOException  If an I/O error occurs while attempting to send the
   *                       line to the server.
   */
  private void sendLine(BufferedWriter writer, String line)
          throws IOException
  {
    writer.write(line);
    writer.write(Constants.SMTP_EOL);
    writer.flush();
  }



  /**
   * Reads a one-line response from the SMTP server.
   *
   * @param  reader  The buffered reader used to read information from the
   *                 server.
   *
   * @return  The response from the server.
   *
   * @throws  IOException  If an I/O error occurs while attempting to read data
   *                       from the server.
   */
  private String readLine(BufferedReader reader)
          throws IOException
  {
    return reader.readLine();
  }



  /**
   * Retrieves the name that the scheduler uses to subscribe to the
   * configuration handler in order to be notified of configuration changes.
   *
   * @return  The name that the scheduler uses to subscribe to the configuration
   *          handler in order to be notified of configuration changes.
   */
  public String getSubscriberName()
  {
    return CONFIG_SUBSCRIBER_NAME;
  }



  /**
   * Retrieves the set of configuration parameters associated with this
   * configuration subscriber.
   *
   * @return  The set of configuration parameters associated with this
   *          configuration subscriber.
   */
  public ParameterList getSubscriberParameters()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In SMTPMailer.getParameters()");

    BooleanParameter enableMailerParameter =
         new BooleanParameter(Constants.PARAM_ENABLE_MAIL_ALERTS,
                              "Enable SLAMD Mailer",
                              "Indicates whether the SLAMD mailer may be " +
                              "used to send mail messages whenever certain " +
                              "events occur.", enableMailer);

    IntegerParameter portParameter =
         new IntegerParameter(Constants.PARAM_SMTP_PORT, "SMTP Server Port",
                              "The port number that should be used to " +
                              "contact the SMTP server.", true, mailPort,
                              true, 1, true, 65535);

    StringParameter hostParameter =
         new StringParameter(Constants.PARAM_SMTP_SERVER, "SMTP Server Address",
                             "The address that should be used to contact the " +
                             "SMTP server.", true, mailHost);

    StringParameter fromAddressParameter =
         new StringParameter(Constants.PARAM_MAIL_FROM_ADDRESS, "From Address",
                             "The e-mail address that should be used as the " +
                             "sender address for messages sent from the " +
                             "SLAMD server.", true, fromAddress);

    StringParameter servletBaseURIParameter =
         new StringParameter(Constants.PARAM_SERVLET_BASE_URI,
                             "Servlet Base URI",
                             "The URI that may be used to access the admin " +
                             "interface for the SLAMD server.  It may be a " +
                             "URL to a read-only version of the server if " +
                             "desired.", false, servletBaseURI);


    Parameter[] params = new Parameter[]
    {
      enableMailerParameter,
      hostParameter,
      portParameter,
      fromAddressParameter,
      servletBaseURIParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the SLAMD scheduler.  In
   * this case, the only option is the delay between iterations of the scheduler
   * loop.
   */
  public void refreshSubscriberConfiguration()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In SMTPMailer.refreshConfiguration()");

    String paramValue =
         configDB.getConfigParameter(Constants.PARAM_ENABLE_MAIL_ALERTS);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      enableMailer =
           (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
    }
    else
    {
      enableMailer = false;
    }


    paramValue = configDB.getConfigParameter(Constants.PARAM_SMTP_SERVER);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      mailHost = paramValue;
    }
    else
    {
      enableMailer = false;
    }


    paramValue = configDB.getConfigParameter(Constants.PARAM_SMTP_PORT);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      try
      {
        mailPort = Integer.parseInt(paramValue);
      }
      catch (NumberFormatException nfe)
      {
        enableMailer = false;
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
      }
    }
    else
    {
      mailPort = Constants.DEFAULT_SMTP_PORT;
    }


    paramValue = configDB.getConfigParameter(Constants.PARAM_MAIL_FROM_ADDRESS);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      fromAddress = paramValue;
    }
    else
    {
      enableMailer = false;
    }


    paramValue = configDB.getConfigParameter(Constants.PARAM_SERVLET_BASE_URI);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      servletBaseURI = paramValue;
    }
    else
    {
      servletBaseURI = "";
    }
  }



  /**
   * Re-reads the configuration information for the specified parameter.  In
   * this case, the only option is the delay between iterations of the scheduler
   * loop.
   *
   * @param  parameterName  The name of the parameter to re-read from the
   *                        configuration.
   */
  public void refreshSubscriberConfiguration(String parameterName)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In SMTPMailer.refreshConfiguration(" +
                           parameterName + ')');

    if (parameterName.equalsIgnoreCase(Constants.PARAM_ENABLE_MAIL_ALERTS))
    {
      if ((mailHost == null) || (mailHost.length() == 0) ||
          (fromAddress == null) || (fromAddress.length() == 0))
      {
        enableMailer = false;
        return;
      }

      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_ENABLE_MAIL_ALERTS);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        enableMailer =
             (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
      }
      else
      {
        enableMailer = false;
      }
    }

    else if (parameterName.equalsIgnoreCase(Constants.PARAM_SMTP_SERVER))
    {
      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_SMTP_SERVER);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        mailHost = paramValue;
      }
      else
      {
        enableMailer = false;
      }
    }

    else if (parameterName.equalsIgnoreCase(Constants.PARAM_SMTP_PORT))
    {
      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_SMTP_PORT);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        try
        {
          mailPort = Integer.parseInt(paramValue);
        }
        catch (NumberFormatException nfe)
        {
          enableMailer = false;
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        mailPort = Constants.DEFAULT_SMTP_PORT;
      }
    }

    else if (parameterName.equalsIgnoreCase(Constants.PARAM_MAIL_FROM_ADDRESS))
    {
      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_MAIL_FROM_ADDRESS);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        fromAddress = paramValue;
      }
      else
      {
        enableMailer = false;
      }
    }

    else if (parameterName.equalsIgnoreCase(Constants.PARAM_SERVLET_BASE_URI))
    {
      String paramValue =
                  configDB.getConfigParameter(Constants.PARAM_SERVLET_BASE_URI);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        servletBaseURI = paramValue;
      }
      else
      {
        servletBaseURI = "";
      }
    }
  }
}

