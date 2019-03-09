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
import java.util.Date;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.message.ClientManagerHelloMessage;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.KeepAliveMessage;
import com.slamd.message.Message;
import com.slamd.message.ServerShutdownMessage;
import com.slamd.message.StartClientRequestMessage;
import com.slamd.message.StartClientResponseMessage;
import com.slamd.message.StatusResponseMessage;
import com.slamd.message.StopClientRequestMessage;
import com.slamd.message.StopClientResponseMessage;



/**
 * This class encapsulates a connection to a client manager, and it is used to
 * keep track of information about them.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientManagerConnection
       extends Thread
       implements Comparable
{
  // The queue that will be used to hold messages received from the client
  // manager.
  private ArrayList<Message> messageQueue;

  // The ASN.1 reader used to read data from the client manager.
  private ASN1Reader asn1Reader;

  // The ASN.1 writer used to write data to the client manager.
  private ASN1Writer asn1Writer;

  // Indicates whether this thread should continue listening for communication
  // from the client manager.
  private boolean keepListening;

  // The listener that accepted this client manager connection.
  private ClientManagerListener listener;

  // The time that this connection was established.
  private Date establishedTime;

  // The maximum number of clients that may be created by this client manager.
  private int maxClients;

  // The next message ID that should be used for sending a request to the client
  // manager.
  private int nextMessageID;

  // The number of clients that have been started by this client manager.
  private int startedClients;

  // A mutex used to provide threadsafe access to the message queue.
  private final Object messageQueueMutex;

  // The SLAMD server with which this client manager connection is associated.
  private SLAMDServer slamdServer;

  // The socket that provides communication with the client manager.
  private Socket clientManagerSocket;

  // The client ID of the client associated with this connection.
  private String clientID;

  // The IP address for this client manager connection.
  private String clientIPAddress;

  // The version of the software on the client associated with this connection.
  private String clientVersion;



  /**
   * Creates a new client manager connection using the provided socket.
   *
   * @param  slamdServer          The SLAMD server with which this client
   *                              manager connection is associated.
   * @param  listener             The client manager listener associated with
   *                              this connection.
   * @param  clientManagerSocket  The socket used to communicate with the client
   *                              manager.
   *
   * @throws  SLAMDException  If a problem occurs while creating the client
   *                          manager connection.
   */
  public ClientManagerConnection(SLAMDServer slamdServer,
                                 ClientManagerListener listener,
                                 Socket clientManagerSocket)
         throws SLAMDException
  {
    this.slamdServer         = slamdServer;
    this.listener            = listener;
    this.clientManagerSocket = clientManagerSocket;
    messageQueue             = new ArrayList<Message>();
    messageQueueMutex        = new Object();
    startedClients           = 0;
    nextMessageID            = 2;
    establishedTime          = new Date();


    // Get the IP address of the client manager and create the ASN.1 reader and
    // writer.
    try
    {
      clientIPAddress = clientManagerSocket.getInetAddress().getHostAddress();
      asn1Reader      = new ASN1Reader(clientManagerSocket);
      asn1Writer      = new ASN1Writer(clientManagerSocket);
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDException("Unable to establish the reader and/or writer " +
                               "to the client manager.", ioe);
    }


    // Read the hello request from the client.
    ClientManagerHelloMessage helloMessage = null;
    try
    {
      ASN1Element element =
           asn1Reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
      helloMessage = (ClientManagerHelloMessage) Message.decode(element);
    }
    catch (Exception e)
    {
      disconnect(false);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new SLAMDException("Unable to read or parse the hello message " +
                               "from the client manager:  " + e, e);
    }


    // Extract the information contained in the hello request.
    clientID      = helloMessage.getClientID();
    clientVersion = helloMessage.getClientVersion();
    maxClients    = helloMessage.getMaxClients();


    // If we should use keepalive messages, then add a socket timeout for this
    // connection.
    int keepAliveInterval =
             slamdServer.getClientListener().getKeepAliveInterval();
    if (keepAliveInterval > 0)
    {
      try
      {
        clientManagerSocket.setSoTimeout(keepAliveInterval*1000);
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Unable to set socket timeout for connection " +
                               "to client manager " + clientID +
                               " -- keepalive messages will not be used.");
      }
    }


    // See if the client ID for this client conflicts with the client ID of
    // a client manager that has already been connected.
    ClientManagerConnection[] cmConns = listener.getClientManagers();
    for (int i=0; i < cmConns.length; i++)
    {
      if (cmConns[i].getClientID().equalsIgnoreCase(clientID))
      {
        try
        {
          HelloResponseMessage helloResponse =
               new HelloResponseMessage(helloMessage.getMessageID(),
                        Constants.MESSAGE_RESPONSE_CLIENT_REJECTED,
                        "A client manager connection has already been " +
                        "established with client ID \"" + clientID + "\".", -1);
          asn1Writer.writeElement(helloResponse.encode());
          disconnect(true);
        }
        catch (IOException ioe)
        {
          disconnect(false);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(ioe));
          throw new SLAMDException("Unable to send the hello response to the " +
                                   "client manager:  " + ioe, ioe);
        }

        throw new SLAMDException("Rejected client manager connection due to " +
                                 "duplicate client ID -- " + clientID + '.');
      }
    }


    // Send the hello response to the client manager.
    try
    {
      HelloResponseMessage helloResponse =
           new HelloResponseMessage(helloMessage.getMessageID(),
                                    Constants.MESSAGE_RESPONSE_SUCCESS, -1);
      asn1Writer.writeElement(helloResponse.encode());
    }
    catch (IOException ioe)
    {
      disconnect(false);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDException("Unable to send the hello response to the " +
                               "client manager:  " + ioe, ioe);
    }

    setName("Client Manager Connection " + clientID);
  }



  /**
   * Retrieves the ID of the client associated with this client manager
   * connection.
   *
   * @return  The ID of the client associated with this client manager
   *          connection.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Retrieves the IP address of the client manager system.
   *
   * @return  The IP address of the client manager system.
   */
  public String getClientIPAddress()
  {
    return clientIPAddress;
  }



  /**
   * Retrieves the software version of the client associated with this client
   * manager connection.
   *
   * @return  The software version of the client associated with this client
   *          manager connection.
   */
  public String getClientVersion()
  {
    return clientVersion;
  }



  /**
   * Retrieves the time at which this connection was established.
   *
   * @return  The time at which this connection was established.
   */
  public Date getEstablishedTime()
  {
    return establishedTime;
  }



  /**
   * Retrieves the maximum number of clients that may be started for this client
   * manager.
   *
   * @return  The maximum number of clients that may be started for this client
   *          manager.
   */
  public int getMaxClients()
  {
    return maxClients;
  }



  /**
   * Retrieves the number of clients associated with this client manager that
   * are currently running.
   *
   * @return  The number of clients associated with this client manager that are
   *          currently running.
   */
  public int getStartedClients()
  {
    return startedClients;
  }



  /**
   * Requests that the client manager start the specified number of clients.
   *
   * @param  numClients  The number of clients to be started by this client
   *                     manager.
   *
   * @throws  SLAMDException  If the number of clients requested would cause the
   *                          client manager to start more than the maximum
   *                          allowed number of clients, or if there is a
   *                          problem starting any of the clients.
   */
  public void startClients(int numClients)
         throws SLAMDException
  {
    // First, see if we should allow this based on what we believe is running.
    if ((maxClients > 0) && ((startedClients + numClients) > maxClients))
    {
      throw new SLAMDException("Requested number of clients (" + numClients +
                               ") would create more than the maximum number " +
                               "of allowed connections (" + maxClients + ')');
    }

    // Create the start client request and send it to the client manager.
    int messageID = nextMessageID();
    try
    {
      StartClientRequestMessage startMessage =
           new StartClientRequestMessage(messageID, numClients,
                    slamdServer.getClientListener().listenPort);
      asn1Writer.writeElement(startMessage.encode());
    }
    catch (IOException ioe)
    {
      disconnect(false);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDException("Unable to send start client request to " +
                               "client manager " + clientID +
                               " -- closing the connection.", ioe);
    }


    // Read the response from the client manager.
    try
    {
      StartClientResponseMessage startResponseMessage =
           (StartClientResponseMessage) getMessage(messageID);
      if (startResponseMessage == null)
      {
        disconnect(false);
        throw new SLAMDException("Unable to read response message from the " +
                                 "client manager -- closing the connection.");
      }
      else
      {
        if (startResponseMessage.getResponseCode() ==
            Constants.MESSAGE_RESPONSE_SUCCESS)
        {
          startedClients += numClients;
          return;
        }
        else
        {
          throw new SLAMDException("Unable to start requested clients:  " +
                                   startResponseMessage.getResponseMessage() +
                                   " (response code " +
                                   startResponseMessage.getResponseCode() +
                                   ')');
        }
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new SLAMDException("Unable to read response message from the " +
                               "client manager -- " + e, e);
    }
  }



  /**
   * Requests that the client manager start the specified number of clients.
   *
   * @param  numClients  The number of clients to be stopped by this client
   *                     manager.  A negative value will indicate that all
   *                     clients should be stopped.
   *
   * @throws  SLAMDException  If the requested number of clients is greater
   *                          than the number of clients actually running, or if
   *                          a problem occurs while trying to stop the clients.
   */
  public void stopClients(int numClients)
         throws SLAMDException
  {
    // See if there was a specific number of clients specified.  If so, then see
    // if we think there are that many running.
    if ((numClients > 0) && (numClients > startedClients))
    {
      throw new SLAMDException("Request to stop " + numClients +
                               " clients for client manager " + clientID +
                               " rejected -- only " + startedClients +
                               " clients have been started");
    }


    // Create the stop client request and send it to the client manager.
    int messageID = nextMessageID();
    try
    {
      StopClientRequestMessage stopMessage =
           new StopClientRequestMessage(messageID, numClients);
      asn1Writer.writeElement(stopMessage.encode());
    }
    catch (IOException ioe)
    {
      disconnect(false);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDException("Unable to send stop client request to " +
                               "client manager " + clientID +
                               " -- closing the connection.", ioe);
    }


    // Read the response from the client manager.
    try
    {
      StopClientResponseMessage stopResponseMessage =
           (StopClientResponseMessage) getMessage(messageID);
      if (stopResponseMessage == null)
      {
        disconnect(false);
        throw new SLAMDException("Unable to read response message from the " +
                                 "client manager -- closing the connection.");
      }
      else
      {
        if (stopResponseMessage.getResponseCode() ==
            Constants.MESSAGE_RESPONSE_SUCCESS)
        {
          if (numClients > 0)
          {
            startedClients -= numClients;
          }
          else
          {
            startedClients = 0;
          }
          return;
        }
        else
        {
          throw new SLAMDException("Unable to stop requested clients:  " +
                                   stopResponseMessage.getResponseMessage() +
                                   " (response code " +
                                   stopResponseMessage.getResponseCode() +
                                   ')');
        }
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new SLAMDException("Unable to read response message from the " +
                               "client manager -- " + e, e);
    }
  }



  /**
   * Closes the connection to the client manager.  The process of closing the
   * connection may optionally include sending a shutdown message to the client
   * manager before the actual disconnect.
   *
   * @param  sendShutdownMessage  Indicates whether a server shutdown message
   *                              should be sent to the client manager before
   *                              the connection is closed.
   */
  public void disconnect(boolean sendShutdownMessage)
  {
    // Create the shutdown message, if appropriate.
    if (sendShutdownMessage)
    {
      ServerShutdownMessage shutdownMessage =
           new ServerShutdownMessage(nextMessageID());
      try
      {
        asn1Writer.writeElement(shutdownMessage.encode());
      } catch (Exception e) {}
    }


    // Close the connection to the client manager.
    try
    {
      clientManagerSocket.close();
    } catch (Exception e) {}
  }



  /**
   * Indicates that a client believed to have been started by this client
   * manager has been lost and that the list of active connections should be
   * updated accordingly.
   */
  public void clientConnectionLost()
  {
    if (startedClients > 0)
    {
      startedClients--;
    }
  }



  /**
   * Retrieves the message ID that should be used for the next request sent to
   * the client manager.
   *
   * @return  The message ID that should be used for the next request sent to
   *          the client manager.
   */
  public synchronized int nextMessageID()
  {
    int returnID = nextMessageID;
    nextMessageID += 2;
    return returnID;
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
   * Create a loop that waits for new communication to arrive from the client
   * manager.  If it is a solicited response (i.e, has a message ID that is an
   * even number), then put it in the message queue to be picked up by an
   * appropriate listener.  If it is an unsolicited message (i.e., has an odd
   * number), then try to handle it directly.
   */
  @Override()
  public void run()
  {
    keepListening = true;
    while (keepListening)
    {
      try
      {
        ASN1Element element = asn1Reader.readElement();
        if (element == null)
        {
          // This should only happen if the client has closed the connection.
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                 "Detected connection closure from client " +
                                 "manager " + clientID);

          try
          {
            clientManagerSocket.close();
          } catch (IOException ioe2) {}
          listener.connectionLost(this);
          return;
        }

        Message message = Message.decode(element);
        int messageID   = message.getMessageID();
        if ((messageID % 2) == 0)
        {
          synchronized (messageQueueMutex)
          {
            messageQueue.add(message);
            messageQueueMutex.notify();
          }
        }
        else
        {
          // The only only unsolicited message type that we allow is a
          // status response message that indicates the client manager is
          // shutting down.
          if (message instanceof StatusResponseMessage)
          {
            StatusResponseMessage statusResponse = (StatusResponseMessage)
                                                   message;
            if (statusResponse.getClientStatusCode() ==
                Constants.CLIENT_STATE_SHUTTING_DOWN)
            {
              keepListening = false;
              break;
            }
            else
            {
              // This was not an expected response -- that's a protocol error.
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "Unexpected status response message " +
                                     "received from client manager " +
                                     clientID + ":  response code " +
                                     statusResponse.getClientStatusCode());
              disconnect(true);
              keepListening = false;
              break;
            }
          }
          else
          {
            // This was not an allowed message -- that's a protocol error and
            // we should close the connection to the client manager.
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                   "Unexpected unsolicited message " +
                                   "received from client manager " +
                                   clientID + ":  message type " +
                                   message.getMessageType());
            disconnect(true);
            keepListening = false;
            break;
          }
        }
      }
      catch (InterruptedIOException iioe)
      {
        // This means that a socket timeout occurred and that we should send a
        // keepalive message to the client manager.
        KeepAliveMessage keepAliveMessage =
             new KeepAliveMessage(nextMessageID());
        try
        {
          asn1Writer.writeElement(keepAliveMessage.encode());
        }
        catch (IOException ioe)
        {
          // This should not happen.  But if it does, close the connection to
          // the client.
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                 "Unable to send keepalive message to client " +
                                 clientID + " (" + ioe + ") -- disconnecting");
          keepListening = false;
          break;
        }
      }
      catch (Exception e)
      {
        // This should be either an ASN.1 exception or an I/O exception.  If
        // either occurs, then that almost certainly means that the connection
        // is unusable, so drop it.
        keepListening = false;
      }
    }

    synchronized (listener.clientManagerMutex)
    {
      listener.connectionLost(this);
    }
    try
    {
      clientManagerSocket.close();
    } catch (Exception e) {}
  }



  /**
   * Compares this client manager connection with the provided object.  The
   * given object must be a client manager connection object, and the comparison
   * will be made based on the lexicographic ordering of the associated client
   * IDs.
   *
   * @param  o  The client manager connection object to compare against this
   *            client manager connection.
   *
   * @return  A negative value if this client manager connection should come
   *          before the provided client manager connection in a sorted list, a
   *          positive value if this client manager connection should come after
   *          the provided client manager connection in a sorted list, or zero
   *          if there is no difference in their ordering as far as this method
   *          is concerned.
   *
   * @throws  ClassCastException  If the provided object is not a client manager
   *                              connection object.
   */
  public int compareTo(Object o)
         throws ClassCastException
  {
    ClientManagerConnection c = (ClientManagerConnection) o;
    return clientID.compareTo(c.clientID);
  }
}

