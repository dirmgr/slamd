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
package com.slamd.stat;



import java.io.IOException;
import java.net.Socket;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

import netscape.ldap.LDAPException;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.asn1.ASN1Writer;
import com.slamd.client.Client;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.message.ClientHelloMessage;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.Message;
import com.slamd.message.RegisterStatisticMessage;
import com.slamd.message.ReportStatisticMessage;
import com.slamd.message.ServerShutdownMessage;



/**
 * This class defines a component that can be used to report statistical data
 * to the SLAMD server in real-time.
 *
 *
 * @author   Neil A. Wilson
 */
public class RealTimeStatReporter
       extends Thread
{
  // The ASN.1 reader used to read messages from the SLAMD server.
  private ASN1Reader reader;

  // The ASN.1 writer used to write messages to the SLAMD server.
  private ASN1Writer writer;

  // Indicates whether this stat reporter should stop running.
  private boolean shouldStop;

  // The next message ID that should be sent to the server.
  private int nextMessageID;

  // The interval to use when reporting statistics back to the server.
  private int reportInterval;

  // The socket used to communicate with the SLAMD server.
  private Socket socket;

  // The job ID of the job for which we are currently reporting statistics.
  private String jobID;

  // The list of statistical data that should be sent to the monitor server.
  private Vector<ASN1Element[]> currentStatList;

  // The stat list that will become the current list whenever we are ready to
  // report to the server.
  private Vector<ASN1Element[]> standbyStatList;



  /**
   * Creates a new instance of this real-time stat reporter.
   *
   * @param  serverAddress     The address to use to connect to the SlAMD server
   *                           when reporting statistics.
   * @param  serverStatPort    The port number to use to connect to the SLAMD
   *                           server when reporting statistics.
   * @param  reportInterval    The interval at which statistics should be
   *                           reported to the server.
   * @param  clientID          The ID assigned to the client that will be
   *                           providing the statistics to report.
   * @param  authID            The ID to use to authenticate to the SLAMD
   *                           server.
   * @param  authPW            The password to use to authenticate to the SLAMD
   *                           server.
   * @param  useSSL            Indicates whether to use SSL to communicate with
   *                           the server.
   * @param  blindTrust        Indicates whether to blindly trust any SSL
   *                           certificate presented by the server.
   * @param  sslKeyStore       The JSSE key store to use.
   * @param  sslKeyPassword    The password to use to access the information in
   *                           the JSSE key store.
   * @param  sslTrustStore     The JSSE trust store to use.
   * @param  sslTrustPassword  The password to use to access the information in
   *                           the JSSE trust store.
   *
   * @throws  SLAMDException  If a problem occurs establishing the connection
   *                          to the SLAMD server.
   */
  public RealTimeStatReporter(String serverAddress, int serverStatPort,
                              int reportInterval, String clientID,
                              String authID, String authPW, boolean useSSL,
                              boolean blindTrust, String sslKeyStore,
                              String sslKeyPassword, String sslTrustStore,
                              String sslTrustPassword)
         throws SLAMDException
  {
    setName("Real-Time Stat Reporter");

    this.reportInterval = reportInterval;
    nextMessageID = 1;

    // Establish the connection to the SLAMD server.
    if (useSSL)
    {
      if (blindTrust)
      {
        try
        {
          JSSEBlindTrustSocketFactory socketFactory =
               new JSSEBlindTrustSocketFactory();
          socket = socketFactory.makeSocket(serverAddress, serverStatPort);
        }
        catch (LDAPException le)
        {
          throw new SLAMDException("Unable to create the stat reporter SSL " +
                                   "socket:  " + le, le);
        }
      }
      else
      {
        if ((sslKeyStore != null) && (sslKeyStore.length() > 0))
        {
          System.setProperty(Constants.SSL_KEY_STORE_PROPERTY, sslKeyStore);
        }

        if ((sslKeyPassword != null) && (sslKeyPassword.length() > 0))
        {
          System.setProperty(Constants.SSL_KEY_PASSWORD_PROPERTY,
                             sslKeyPassword);
        }

        if ((sslTrustStore != null) && (sslTrustStore.length() > 0))
        {
          System.setProperty(Constants.SSL_TRUST_STORE_PROPERTY, sslTrustStore);
        }

        if ((sslTrustPassword != null) && (sslTrustPassword.length() > 0))
        {
          System.setProperty(Constants.SSL_TRUST_PASSWORD_PROPERTY,
                             sslTrustPassword);
        }

        try
        {
          SSLSocketFactory socketFactory =
               (SSLSocketFactory) SSLSocketFactory.getDefault();
          socket = socketFactory.createSocket(serverAddress, serverStatPort);
        }
        catch (IOException ioe)
        {
          throw new SLAMDException("Unable to create the stat reporter SSL " +
                                   "socket:  " + ioe, ioe);
        }
      }
    }
    else
    {
      try
      {
        socket = new Socket(serverAddress, serverStatPort);
      }
      catch (IOException ioe)
      {
        throw new SLAMDException("Unable to create the stat reporter " +
                                 "socket:  " + ioe, ioe);
      }
    }


    // Create the reader and writer for the socket.
    try
    {
      reader = new ASN1Reader(socket);
      writer = new ASN1Writer(socket);
    }
    catch (IOException ioe)
    {
      try
      {
        socket.close();
      } catch (Exception e) {}

      throw new SLAMDException("Unable to create the reader or writer for " +
                               "the stat reporter socket:  " + ioe, ioe);
    }

    int authType = Constants.AUTH_TYPE_NONE;
    if ((authID != null) && (authID.length() > 0) && (authPW != null) &&
        (authPW.length() > 0))
    {
      authType = Constants.AUTH_TYPE_SIMPLE;
    }

    ClientHelloMessage helloRequest =
         new ClientHelloMessage(nextMessageID(), Client.SLAMD_CLIENT_VERSION,
                                clientID, authType, authID, authPW, true);
    Message responseMessage = sendMessageAndReadResponse(helloRequest);
    if (responseMessage == null)
    {
      throw new SLAMDException("Unable to read the hello response from SLAMD " +
                               "server");
    }
    else if (! (responseMessage instanceof HelloResponseMessage))
    {
      throw new SLAMDException("Received an inappropriate message from " +
                               "the SLAMD server in response to a hello " +
                               "request:  " + responseMessage.toString());
    }

    HelloResponseMessage helloResponse = (HelloResponseMessage) responseMessage;
    if (helloResponse.getResponseCode() != Constants.MESSAGE_RESPONSE_SUCCESS)
    {
      int    respCode = helloResponse.getResponseCode();
      String respMsg  = helloResponse.getResponseMessage();

      String message = "Unable to complete hello sequence with server -- " +
                       "(err=" + respCode;
      if ((respMsg == null) || (respMsg.length() == 0))
      {
        message += ")";
      }
      else
      {
        message += ", msg=\"" + respMsg + "\")";
      }

      throw new SLAMDException(message);
    }

    shouldStop      = false;
    currentStatList = new Vector<ASN1Element[]>();
    standbyStatList = new Vector<ASN1Element[]>();
  }



  /**
   * Sends a message to the SLAMD server indicating that a new statistic will
   * be reported.  This should be called for each individual stat tracker being
   * used (even for those running in separate threads on the same client).
   *
   * @param  jobID        The ID of the job for which the statistical data will
   *                      be provided.
   * @param  statTracker  The stat tracker being registered.
   */
  public void registerStat(String jobID, StatTracker statTracker)
  {
    if (shouldStop)
    {
      return;
    }

    this.jobID = jobID;
    sendMessage(new RegisterStatisticMessage(nextMessageID(), jobID,
                                             statTracker.getClientID(),
                                             statTracker.getThreadID(),
                                             statTracker.getDisplayName()));
  }



  /**
   * Indicates that the provided value should be added to the corresponding
   * values for all threads by all clients for this statistic and interval.
   *
   * @param  statTracker     The stat tracker reporting the statistic.
   * @param  intervalNumber  The interval number for this value.
   * @param  statValue       the value being reported.
   */
  public void reportStatToAdd(StatTracker statTracker, int intervalNumber,
                              double statValue)
  {
    if (shouldStop)
    {
      return;
    }

    ASN1Element[] dataElements = new ASN1Element[]
    {
      new ASN1OctetString(statTracker.getClientID()),
      new ASN1OctetString(statTracker.getThreadID()),
      new ASN1OctetString(statTracker.getDisplayName()),
      new ASN1Integer(intervalNumber),
      new ASN1Enumerated(Constants.STAT_REPORT_TYPE_ADD),
      new ASN1OctetString(String.valueOf(statValue))
    };

    currentStatList.add(dataElements);
  }



  /**
   * Indicates that the provided value should be averaged with the corresponding
   * values for all threads by all clients for this statistic and interval.
   *
   * @param  statTracker     The stat tracker reporting the statistic.
   * @param  intervalNumber  The interval number for this value.
   * @param  statValue       the value being reported.
   */
  public void reportStatToAverage(StatTracker statTracker, int intervalNumber,
                                  double statValue)
  {
    if (shouldStop)
    {
      return;
    }

    ASN1Element[] dataElements = new ASN1Element[]
    {
      new ASN1OctetString(statTracker.getClientID()),
      new ASN1OctetString(statTracker.getThreadID()),
      new ASN1OctetString(statTracker.getDisplayName()),
      new ASN1Integer(intervalNumber),
      new ASN1Enumerated(Constants.STAT_REPORT_TYPE_AVERAGE),
      new ASN1OctetString(String.valueOf(statValue))
    };

    currentStatList.add(dataElements);
  }



  /**
   * Indicates that the stat tracker will not be providing any more data.
   *
   * @param  statTracker     The stat tracker that is done reporting.
   * @param  intervalNumber  The interval number for this message.
   */
  public void doneReporting(StatTracker statTracker, int intervalNumber)
  {
    if (shouldStop)
    {
      return;
    }

    ASN1Element[] dataElements = new ASN1Element[]
    {
      new ASN1OctetString(statTracker.getClientID()),
      new ASN1OctetString(statTracker.getThreadID()),
      new ASN1OctetString(statTracker.getDisplayName()),
      new ASN1Integer(intervalNumber),
      new ASN1Enumerated(Constants.STAT_REPORT_TYPE_DONE),
    };

    currentStatList.add(dataElements);
  }



  /**
   * Sends the provided message to the SLAMD server.
   *
   * @param  message  The message to be sent.
   */
  private void sendMessage(Message message)
  {
    try
    {
      writer.writeElement(message.encode());
    }
    catch (IOException ioe)
    {
      System.err.println("Unable to send stat reporting message to the SLAMD " +
                         "server:  " + ioe);
      shouldStop = true;
    }
  }



  /**
   * Sends the provided message to the SLAMD server and reads the corresponding
   * response.
   *
   * @param  message  The message to be sent.
   *
   * @return  The response received for the specified message, or
   *          <CODE>null</CODE> if a problem occurred or the server is shutting
   *          down.
   */
  private Message sendMessageAndReadResponse(Message message)
  {
    try
    {
      writer.writeElement(message.encode());
    }
    catch (IOException ioe)
    {
      System.err.println("Unable to send stat reporting message to the SLAMD " +
                         "server:  " + ioe);
      shouldStop = true;
      return null;
    }

    try
    {
      while (! shouldStop)
      {
        Message respMsg =
             Message.decode(
                  reader.readElement(Constants.MAX_BLOCKING_READ_TIME));
        if (respMsg.getMessageID() == message.getMessageID())
        {
          return respMsg;
        }
        else
        {
          // This is not good.  We got a message that doesn't correspond to our
          // message ID.  Look at it briefly to see if it's a server shutdown
          // message, and if not, then just throw it away.  Is there anything
          // else we can do about it?
          if (respMsg instanceof ServerShutdownMessage)
          {
            shouldStop = true;
            return null;
          }
        }
      }

      // If we've gotten here, then we mush be shutting down.
      return null;
    }
    catch (Exception e)
    {
      System.err.println("Unable to read stat reporting response from the " +
                         "SLAMD server:  " + e);
      shouldStop = true;
      return null;
    }
  }



  /**
   * Retrieves the message ID that should be used in the next message sent to
   * the SLAMD server.
   *
   * @return  The message ID that should be used in the next message sent to the
   *          SLAMD server.
   */
  private int nextMessageID()
  {
    int returnID = nextMessageID;
    nextMessageID += 2;
    return returnID;
  }



  /**
   * Loops repeatedly waiting for new messages to send to the SLAMD server.
   */
  public void run()
  {
    while (! shouldStop)
    {
      long stopSleepTime = System.currentTimeMillis() + (1000 * reportInterval);
      Vector<ASN1Element[]> list  = currentStatList;
      currentStatList = standbyStatList;

      if (list.isEmpty())
      {
        standbyStatList = list;
      }
      else
      {
        ASN1Sequence[] dataSequences = new ASN1Sequence[list.size()];
        for (int i=0; i < dataSequences.length; i++)
        {
          dataSequences[i] = new ASN1Sequence(list.get(i));
        }
        sendMessage(new ReportStatisticMessage(nextMessageID(), jobID,
                                               dataSequences));

        // Clear the list and make it the new standby
        list.clear();
        standbyStatList = list;
      }

      // Sleep before checking again.
      long sleepTime = stopSleepTime - System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {}
      }
    }
  }



  /**
   * Indicates that this stat reporter should stop running.
   */
  public void stopRunning()
  {
    shouldStop = true;
  }
}

