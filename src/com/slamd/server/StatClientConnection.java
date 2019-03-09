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



import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.ArrayList;

import com.slamd.admin.AccessManager;
import com.slamd.admin.AdminAccess;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.message.ClientHelloMessage;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.KeepAliveMessage;
import com.slamd.message.Message;
import com.slamd.message.RegisterStatisticMessage;
import com.slamd.message.ReportStatisticMessage;
import com.slamd.message.ServerShutdownMessage;



/**
 * This class defines a thread that is spawned by the server to handle each
 * stat client connection.  It takes care of reading messages in from the client
 * and provides methods for sending messages to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatClientConnection
       extends Thread
       implements Comparable
{
  // The queue that will be used to hold messages received from the client.
  private ArrayList<Message> messageQueue;

  // The ASN.1 reader used to read data from the client.
  private ASN1Reader asn1Reader;

  // The ASN.1 writer used to write data to the client.
  private ASN1Writer asn1Writer;

  // Indicates whether this thread should continue listening for communication
  // from the client.
  private boolean keepListening;

  // Indicates whether this client supports time synchronization with the
  // server.
  private boolean supportsTimeSync;

  // The listener that accepted this client connection.
  private StatListener listener;

  // The next message ID that should be used for sending a request to the
  // client.
  private int nextMessageID;

  // A mutex used to provide threadsafe access to the message queue.
  private final Object messageQueueMutex;

  // The real-time stat handler to which data will be reported.
  private RealTimeStatHandler statHandler;

  // The SLAMD server with which this client connection is associated.
  private SLAMDServer slamdServer;

  // The socket that provides communication with the client.
  protected Socket clientSocket;

  // The client ID of the client associated with this connection.
  private String clientID;

  // The IP address for this client connection.
  private String clientIPAddress;

  // The version of the software on the client associated with this connection.
  private String clientVersion;



  /**
   * Creates a new stat client connection based on the provided information.
   *
   * @param  slamdServer   The SLAMD server with which this client is
   *                       associated.
   * @param  listener      The listener that accepted this connection.
   * @param  clientSocket  The socket used to communicate with the client.
   *
   * @throws  SLAMDException  If a problem occurs while creating the connection.
   */
  public StatClientConnection(SLAMDServer slamdServer, StatListener listener,
                              Socket clientSocket)
         throws SLAMDException
  {
    this.slamdServer      = slamdServer;
    this.listener         = listener;
    this.clientSocket     = clientSocket;
    this.supportsTimeSync = false;
    statHandler           = slamdServer.getStatHandler();
    messageQueue          = new ArrayList<Message>();
    messageQueueMutex     = new Object();
    nextMessageID         = 2;


    // Get the IP address of the client and create the ASN.1 reader and writer.
    try
    {
      clientIPAddress = clientSocket.getInetAddress().getHostAddress();
      asn1Reader      = new ASN1Reader(clientSocket);
      asn1Writer      = new ASN1Writer(clientSocket);
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDException("Unable to establish the reader and/or writer " +
                               "to the stat client.", ioe);
    }


    // Read the hello request from the client.
    ClientHelloMessage helloMessage = null;
    try
    {
      ASN1Element element =
           asn1Reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
      helloMessage = (ClientHelloMessage) Message.decode(element);
    }
    catch (Exception e)
    {
      try
      {
        clientSocket.close();
      } catch (Exception e2) {}
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new SLAMDException("Unable to read or parse the hello message " +
                               "from the stat client:  " + e, e);
    }


    // Determine how to handle client authentication.
    clientID               = helloMessage.getClientID();
    clientVersion          = helloMessage.getClientVersion();
    supportsTimeSync       = helloMessage.supportsTimeSync();
    String authID          = helloMessage.getAuthID();
    String authCredentials = helloMessage.getAuthCredentials();
    int    authType        = helloMessage.getAuthType();

    if ((authID == null) || (authID.length() == 0) ||
        (authCredentials == null) || (authCredentials.length() == 0))
    {
      if (listener.requireAuthentication())
      {
        String msg = "Authentication required but client did not " +
                     "provide sufficient authentication data.";
        HelloResponseMessage helloResp =
             new HelloResponseMessage(0,
                      Constants.MESSAGE_RESPONSE_SERVER_ERROR, msg, -1);
        try
        {
          asn1Writer.writeElement(helloResp.encode());
        } catch (IOException ioe) {}
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                               "Rejected new stat client connection " +
                               clientID + " -- " + msg);
        try
        {
          clientSocket.close();
        } catch (Exception e) {}
        throw new SLAMDException(msg);
      }
    }
    else
    {
      if (authType != Constants.AUTH_TYPE_SIMPLE)
      {
        throw new SLAMDException("Invalid authentication type " +
                                 authType);
      }

      AccessManager accessManager = AdminAccess.getAccessManager();
      if (accessManager == null)
      {
        String msg = "The SLAMD server is not properly configured " +
                     "to perform authentication.";
        HelloResponseMessage helloResp =
             new HelloResponseMessage(0,
                      Constants.MESSAGE_RESPONSE_SERVER_ERROR, msg, -1);
        try
        {
          asn1Writer.writeElement(helloResp.encode());
        } catch (IOException ioe) {}
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                               "Rejected new stat client connection " +
                               clientID + " -- " + msg);
        try
        {
          clientSocket.close();
        } catch (Exception e) {}
        throw new SLAMDException(msg);
      }

      StringBuilder msgBuffer = new StringBuilder();
      int resultCode =
           accessManager.authenticateClient(authID, authCredentials,
                                            msgBuffer);
      if (resultCode != Constants.MESSAGE_RESPONSE_SUCCESS)
      {
        String msg = msgBuffer.toString();
        HelloResponseMessage helloResp =
             new HelloResponseMessage(0, resultCode, msg, -1);
        try
        {
          asn1Writer.writeElement(helloResp.encode());
        } catch (IOException ioe) {}
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                               "Rejected new client connection " +
                               clientID + " -- " + msg);
        try
        {
          clientSocket.close();
        } catch (Exception e) {}
        throw new SLAMDException(msg);
      }
    }


    // If we should use keepalive messages, then add a socket timeout for this
    // connection.
    int keepAliveInterval = listener.getKeepAliveInterval();
    if (keepAliveInterval > 0)
    {
      try
      {
        clientSocket.setSoTimeout(keepAliveInterval*1000);
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Unable to set socket timeout for connection " +
                               "to stat client " + clientID +
                               " -- keepalive messages will not be used.");
      }
    }


    // See if the client ID for this client conflicts with the client ID of
    // a client that has already been connected.
    StatClientConnection[] conns = listener.getConnectionList();
    for (int i=0; i < conns.length; i++)
    {
      if (conns[i].getClientID().equalsIgnoreCase(clientID))
      {
        try
        {
          HelloResponseMessage helloResponse =
               new HelloResponseMessage(helloMessage.getMessageID(),
                        Constants.MESSAGE_RESPONSE_CLIENT_REJECTED,
                        "A stat client connection has already been " +
                        "established with client ID \"" + clientID + "\".", -1);
          asn1Writer.writeElement(helloResponse.encode());
          clientSocket.close();
        }
        catch (IOException ioe)
        {
          try
          {
            clientSocket.close();
          } catch (Exception e) {}
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(ioe));
          throw new SLAMDException("Unable to send the hello response to the " +
                                   "stat client:  " + ioe, ioe);
        }

        throw new SLAMDException("Rejected stat client connection due to " +
                                 "duplicate client ID -- " + clientID + '.');
      }
    }


    // Send the hello response to the client.
    try
    {
      long serverTime = (supportsTimeSync ? System.currentTimeMillis() : -1);
      HelloResponseMessage helloResponse =
           new HelloResponseMessage(helloMessage.getMessageID(),
                                    Constants.MESSAGE_RESPONSE_SUCCESS,
                                    serverTime);
      asn1Writer.writeElement(helloResponse.encode());
    }
    catch (IOException ioe)
    {
      try
      {
        clientSocket.close();
      } catch (Exception e) {}
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDException("Unable to send the hello response to the " +
                               "stat client:  " + ioe, ioe);
    }

    setName("Stat Client Connection " + clientID);
  }



  /**
   * Retrieves the client ID associated with this stat client connection.
   *
   * @return  The client ID associated with this stat client connection.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Retrieves the IP address of the client system associated with this
   * connection.
   *
   * @return  The IP address of the client system associated with this
   *          connection.
   */
  public String getClientIPAddress()
  {
    return clientIPAddress;
  }



  /**
   * Retrieves the version of the client software that the client system is
   * running.
   *
   * @return  The version of the client software that the client system is
   *          running.
   */
  public String getClientVersion()
  {
    return clientVersion;
  }



  /**
   * Retrieves the message ID that should be used for the next request sent to
   * the client.
   *
   * @return  The message ID that should be used for the next request sent to
   *          the client.
   */
  private int nextMessageID()
  {
    int messageID = nextMessageID;
    nextMessageID += 2;
    return messageID;
  }



  /**
   * Retrieves the message with the specified message ID from the receive queue.
   *
   * @param  messageID  The message ID of the message to retrieve.
   *
   * @return  The requested message, or <CODE>null</CODE> if an appropriate
   *          response does not arrive within an appropriate timeout period.
   */
  public Message getMessage(int messageID)
  {
    synchronized (messageQueueMutex)
    {
      for (int i=0; i < messageQueue.size(); i++)
      {
        Message message = messageQueue.get(i);
        if (message.getMessageID() == messageID)
        {
          messageQueue.remove(i);
          return message;
        }
      }

      // If we have gotten here, then the requested message wasn't in the
      // queue.  Wait for it to arrive.
      try
      {
        messageQueueMutex.wait(1000 * listener.getMaxResponseWaitTime());
      } catch (InterruptedException ie) {}

      for (int i=0; i < messageQueue.size(); i++)
      {
        Message message = messageQueue.get(i);
        if (message.getMessageID() == messageID)
        {
          messageQueue.remove(i);
          return message;
        }
      }
    }

    // If we have gotten here, then the message didn't arrive.  Return null.
    return null;
  }




  /**
   * Sends a message to the client that indicates the server is shutting down,
   * and then optionally closes the connection to the client.
   *
   * @param  closeSocket  Indicates whether the connection to the client should
   *                      be closed.
   */
  public void sendServerShutdownMessage(boolean closeSocket)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE, "In " +
                           "StatClientConnection.sendServerShutdownMessage() " +
                           "for " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG, "In " +
                           "ClientConnection.sendServerShutdownMessage() " +
                           "for " + clientID);

    ServerShutdownMessage shutdownMessage =
         new ServerShutdownMessage(nextMessageID());
    try
    {
      asn1Writer.writeElement(shutdownMessage.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Sent shutdown message to " + clientID);
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send shutdown message to " +
                             clientID + ":  " + ioe);
      ioe.printStackTrace();
      listener.connectionLost(this);
    }

    if (closeSocket)
    {
      keepListening = false;
      try
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Closing socket for " + clientID);
        clientSocket.close();
      } catch (IOException ioe) {}
    }
  }



  /**
   * Listens for messages from the client and either handles them or hands them
   * off to be handled elsewhere.
   */
  @Override()
  public void run()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In StatClientConnection.run() for " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In StatClientConnection.run() for " + clientID);

    keepListening = true;


    // First, set a timeout on the socket.  This will allow us to interrupt the
    // reads periodically to send keepalive messages.
    int keepAliveTime = listener.getKeepAliveInterval();
    if (keepAliveTime > 0)
    {
      try
      {
        clientSocket.setSoTimeout(keepAliveTime*1000);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Set socket timeout of " + keepAliveTime +
                               " seconds for " + clientID);
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Could not set timeout for client connection " +
                               clientID);
      }
    }


    // This flag will be used to detect two consecutive failures (indicates
    // that the connection has been closed without the client notifying us)
    boolean consecutiveFailures = false;


    // Loop infinitely (or at least until the client shuts down) and read
    // messages from the client
    while (keepListening)
    {
      try
      {
        ASN1Element element = asn1Reader.readElement();
        if (element == null)
        {
          // This should only happen if the client has closed the connection.
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                 "Detected connection closure from stat " +
                                 "client " + clientID);

          try
          {
            clientSocket.close();
          } catch (IOException ioe2) {}
          listener.connectionLost(this);
          return;
        }

        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Read a message from " + clientID);
        Message message = Message.decode(element);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Decoded message from " + clientID);


        if (message instanceof RegisterStatisticMessage)
        {
          RegisterStatisticMessage msg = (RegisterStatisticMessage) message;
          statHandler.handleRegisterStatMessage(msg);
        }
        else if (message instanceof ReportStatisticMessage)
        {
          ReportStatisticMessage msg = (ReportStatisticMessage) message;
          statHandler.handleReportStatMessage(msg);
        }
      }
      catch (InterruptedIOException iioe)
      {
        // If this exception was thrown, it was because the socket timeout was
        // reached.  We need to send a keepalive message.
        try
        {
          KeepAliveMessage kaMsg =
               new KeepAliveMessage(nextMessageID());
          asn1Writer.writeElement(kaMsg.encode());
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Sent keepalive to " + clientID);
        }
        catch (IOException ioe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Unable to send keepalive to " + clientID +
                                 ":  " + ioe);
        }
      }
      catch (IOException ioe)
      {
        // Some other I/O related exception was thrown
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "I/O exception from " + clientID +
                               ":  " + ioe);

        // Some problem occurred.  See if this is a second consecutive failure
        // and if so, end the connection and this thread.  Otherwise set a
        // flag that will be used to detect a second consecutive failure
        if (consecutiveFailures)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Consecutive failures on connection " +
                                 clientID + " -- closing");
          try
          {
            clientSocket.close();
          } catch (IOException ioe2) {}
          listener.connectionLost(this);
          return;
        }
        else
        {
          consecutiveFailures = true;
        }
      }
      catch (SLAMDException se)
      {
        // The specified class could not be found
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception handling message from " + clientID +
                               ":  " + se);
        se.printStackTrace();
      }
      catch (ASN1Exception ae)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception decoding message from " + clientID +
                               ":  " + ae);
        ae.printStackTrace();
      }
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving StatClientConnection.run() for " +
                           clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Leaving StatClientConnection.run() for " +
                           clientID);
  }



  /**
   * Compares this stat client connection with the provided object.  The given
   * object must be a stat client connection object, and the comparison will be
   * made based on the lexicographic ordering of the associated client IDs.
   *
   * @param  o  The stat client connection object to compare against this stat
   *            client connection.
   *
   * @return  A negative value if this stat client connection should come before
   *          the provided stat client connection in a sorted list, a positive
   *          value if this stat client connection should come after the
   *          provided stat client connection in a sorted list, or zero if there
   *          is no difference in their ordering as far as this method is
   *          concerned.
   *
   * @throws  ClassCastException  If the provided object is not a stat client
   *                              connection object.
   */
  public int compareTo(Object o)
         throws ClassCastException
  {
    StatClientConnection c = (StatClientConnection) o;
    return clientID.compareTo(c.clientID);
  }
}

