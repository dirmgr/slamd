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

import com.slamd.admin.AccessManager;
import com.slamd.admin.AdminAccess;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.JobClassLoader;
import com.slamd.common.SLAMDException;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.message.ClassTransferRequestMessage;
import com.slamd.message.ClassTransferResponseMessage;
import com.slamd.message.ClientHelloMessage;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.JobCompletedMessage;
import com.slamd.message.JobControlRequestMessage;
import com.slamd.message.JobControlResponseMessage;
import com.slamd.message.JobRequestMessage;
import com.slamd.message.JobResponseMessage;
import com.slamd.message.KeepAliveMessage;
import com.slamd.message.Message;
import com.slamd.message.ServerShutdownMessage;
import com.slamd.message.StatusRequestMessage;
import com.slamd.message.StatusResponseMessage;



/**
 * This class defines a thread that is spawned by the server to handle each
 * client connection.  It takes care of reading messages in from the client and
 * provides methods for sending messages to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientConnection
       extends Thread
       implements Comparable
{
  // The list used to hold responses to solicited messages
  private ArrayList<Message> messageList;

  // The reader used to read ASN.1 elements from the client.
  private ASN1Reader reader;

  // The writer used to write ASN.1 elements to the client.
  private ASN1Writer writer;

  // Indicates whether this connection should keep listening for new messages
  // from the client.
  private boolean keepListening;

  // Indicates whether this client is operating in restricted mode.  If so, then
  // it should only be used to handle jobs for which it has been explicitly
  // requested.
  protected boolean restrictedMode;

  // Indicates whether this client supports time synchronization.
  private boolean supportsTimeSync;

  // The client listener that accepted this connection.
  private ClientListener clientListener;

  // The time that this connection was established.
  private Date establishedTime;

  // The type of authentication performed by the client.
  private int authType;

  // The length of time in seconds between keepalive messages
  private int keepAliveTime;

  // The maximum length of time in seconds that the getResponse method will wait
  // for a matching message to show up in the receive queue before returning
  // null
  private int maxResponseWaitTime;

  // The next message ID that will be used in a message originated by the server
  private int messageID;

  // The job that is being processed by this connection
  private Job jobInProgress;

  // A mutex used to provide threadsafe access to the message list
  private final Object messageListMutex;

  // The SLAMD server with which this client connection is associated
  private SLAMDServer slamdServer;

  // The network socket used to communicate with the client
  protected Socket socket;

  // The authentication ID provided by the client.
  private String authID;

  // The authentication credentials provided by the client.
  private String authCredentials;

  // The client ID that the client has assigned to itself.
  private String clientID;

  // The IP address of the client.
  private String clientIPAddress;

  // The unique ID assigned by the server that identifies this connection to the
  // server.
  private String connectionID;



  /**
   * Creates a new connection that the server will use to communicate with the
   * client.
   *
   * @param  slamdServer     The SLAMD server with which this connection is
   *                         associated.
   * @param  clientListener  The client listener that accepted this connection.
   * @param  socket          The socket that is used to communicate with the
   *                         client.
   * @param  connectionID    The unique identifier associated with the client.
   *
   * @throws  SLAMDException  If there is a problem with the client connection.
   */
  public ClientConnection(SLAMDServer slamdServer,
                          ClientListener clientListener, Socket socket,
                          String connectionID)
         throws SLAMDException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection constructor");

    setName("Client Connection " + connectionID);

    this.slamdServer      = slamdServer;
    this.clientListener   = clientListener;
    this.socket           = socket;
    this.clientID         = "(unknown)";
    this.clientIPAddress  = socket.getInetAddress().getHostAddress();
    this.connectionID     = connectionID;
    this.restrictedMode   = false;
    this.supportsTimeSync = false;

    this.establishedTime = new Date();

    messageList      = new ArrayList<Message>();
    messageListMutex = new Object();
    messageID        = 1;
    keepAliveTime    = slamdServer.getClientListener().getKeepAliveInterval();
    jobInProgress    = null;

    maxResponseWaitTime =
         slamdServer.getClientListener().getMaxResponseWaitTime();


    // Send the hello response to the client (for right now, always success)
    try
    {
      reader = new ASN1Reader(socket.getInputStream());
      writer = new ASN1Writer(socket.getOutputStream());

      ClientHelloMessage helloRequest;
      String respMesg = "";
      try
      {
        ASN1Element element =
             reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
        Message message = Message.decode(element);
        if (message instanceof ClientHelloMessage)
        {
          helloRequest = (ClientHelloMessage) message;
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Received hello request from client " +
                                 toString());
          this.clientID         = helloRequest.getClientID();
          this.restrictedMode   = helloRequest.restrictedMode();
          this.authID           = helloRequest.getAuthID();
          this.authCredentials  = helloRequest.getAuthCredentials();
          this.authType         = helloRequest.getAuthType();
          this.supportsTimeSync = helloRequest.supportsTimeSync();

          if ((authID == null) || (authID.length() == 0) ||
              (authCredentials == null) || (authCredentials.length() == 0))
          {
            if (clientListener.requireAuthentication())
            {
              String msg = "Authentication required but client did not " +
                           "provide sufficient authentication data.";
              HelloResponseMessage helloResp =
                   new HelloResponseMessage(0,
                            Constants.MESSAGE_RESPONSE_SERVER_ERROR, msg, -1);
              writer.writeElement(helloResp.encode());
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "Rejected new client connection " +
                                     clientID + " -- " + msg);
              try
              {
                socket.close();
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
              writer.writeElement(helloResp.encode());
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "Rejected new client connection " +
                                     clientID + " -- " + msg);
              try
              {
                socket.close();
              } catch (Exception e) {}
              throw new SLAMDException(msg);
            }

            StringBuilder msgBuffer = new StringBuilder();
            int resultCode =
                 accessManager.authenticateClient(authID, authCredentials,
                                                  msgBuffer);
            if (resultCode == Constants.MESSAGE_RESPONSE_SUCCESS)
            {
              respMesg = "Successfully authenticated as " + authID;
            }
            else
            {
              long serverTime = (supportsTimeSync
                                 ? System.currentTimeMillis()
                                 : -1);

              String msg = msgBuffer.toString();
              HelloResponseMessage helloResp =
                   new HelloResponseMessage(0, resultCode, msg, serverTime);
              writer.writeElement(helloResp.encode());
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "Rejected new client connection " +
                                     clientID + " -- " + msg);
              try
              {
                socket.close();
              } catch (Exception e) {}
              throw new SLAMDException(msg);
            }
          }
        }
        else
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Expected hello request from client but got " +
                                 "instance of " + message.getClass().getName());
        }
      }
      catch (ASN1Exception ae)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Unable to parse hello request message from " +
                               toString() + ":  " + ae);
        ae.printStackTrace();
      }
      catch (SLAMDException se)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Unable to obtain hello request message from " +
                               toString() + ":  " + se);
        se.printStackTrace();
      }


      long serverTime = (supportsTimeSync ? System.currentTimeMillis() : -1);
      HelloResponseMessage helloResp =
           new HelloResponseMessage(0, Constants.MESSAGE_RESPONSE_SUCCESS,
                                    respMesg, serverTime);
      writer.writeElement(helloResp.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                             "Accepted new client connection " + clientID +
                             " from " + socket.getInetAddress().toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "I/O exception in client handshake for " +
                             toString() + ":  " + ioe);
      ioe.printStackTrace();
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ClientConnection constructor");
  }



  /**
   * Retrieves the connection ID assigned to this client connection by the
   * SLAMD server.
   *
   * @return  The connection ID assigned to this client connection by the
   *          SLAMD server.
   */
  public String getConnectionID()
  {
    return connectionID;
  }



  /**
   * Retrieves the client ID that the client has chosen for itself.
   *
   * @return  The client ID that the client has chosen for itself.
   */
  public String getClientID()
  {
    return clientID;
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
   * Retrieves the IP address of the client associated with this connection.
   *
   * @return  The IP address of the client associated with this connection.
   */
  public String getClientIPAddress()
  {
    return clientIPAddress;
  }



  /**
   * Indicates whether this client is operating in restricted mode, in which
   * case it will only be used to handle jobs for which it has been explicitly
   * requested.
   *
   * @return  <CODE>true</CODE> if this client is operating in restricted mode,
   *          or <CODE>false</CODE> if it is not.
   */
  public boolean restrictedMode()
  {
    return restrictedMode;
  }



  /**
   * Retrieves the job that the client is currently processing (if any).
   *
   * @return  The job that the client is currently processing, or
   *          <CODE>null</CODE> if it is not processing any job.
   */
  public Job getJob()
  {
    return jobInProgress;
  }



  /**
   * Listens for messages from the client and either handles them or hands them
   * off to be handled elsewhere.
   */
  @Override()
  public void run()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.run() for " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.run() for " + clientID);

    keepListening = true;


    // First, set a timeout on the socket.  This will allow us to interrupt the
    // reads periodically to send keepalive messages.
    if (keepAliveTime > 0)
    {
      try
      {
        socket.setSoTimeout(keepAliveTime*1000);
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
        ASN1Element element = reader.readElement();
        if (element == null)
        {
          // This should only happen if the client has closed the connection.
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                 "Detected connection closure from client " +
                                 clientID);

          try
          {
            socket.close();
          } catch (IOException ioe2) {}
          slamdServer.getClientListener().connectionLost(this);
          return;
        }

        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Read a message from client " + clientID);
        Message message = Message.decode(element);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Decoded message from client " + clientID +
                               " -- " + message.toString());


        // We need to be able to handle two kinds of messages:  solicited and
        // unsolicited.  Solicited messages are those that the client provides
        // in response to a request from the server.  Unsolicited messages are
        // those that the client provides without a request from the server
        // (primarily job complete messages and status response messages that
        // indicate the client is shutting down).  This method will handle the
        // unsolicited messages, but the solicited messages will be placed into
        // a queue to be picked up by the method that issued the request.  It is
        // possible to tell the difference between solicited and unsolicited
        // messages because messages that are in response to a request from the
        // server (solicited messages) will have an odd message ID and messages
        // that originate from the client (unsolicited messages) will have an
        // even message ID.
        if ((message.getMessageID() % 2) != 0)
        {
          // This is a solicited message, so add it into the queue to be
          // picked up by something else
          synchronized (messageListMutex)
          {
            messageList.add(message);
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Queueing solicited response from client " +
                                   clientID);
          }
        }
        else if (message instanceof JobCompletedMessage)
        {
          // This is a job completed message, so update the scheduler that
          // the job is done
          JobCompletedMessage msg = (JobCompletedMessage) message;
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Job completed response from client " +
                                 clientID);
          if (msg.getJobID().equals(jobInProgress.getJobID()))
          {
            jobInProgress.clientDone(this, msg);
            jobInProgress = null;
            slamdServer.getClientListener().setAvailableForProcessing(this);
          }
        }
        else if (message instanceof StatusResponseMessage)
        {
          // This is a status response message, but not a solicited one.  It
          // almost certainly means the client is shutting down
          StatusResponseMessage msg = (StatusResponseMessage)
                                           message;
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Status response from client " + clientID);
          if (msg.getClientStatusCode() ==
              Constants.CLIENT_STATE_SHUTTING_DOWN)
          {
            // FIXME:  Check for job information in the response message
            // before destroying this connection
            slamdServer.getClientListener().connectionLost(this);
            try
            {
              socket.close();
            } catch (IOException ioe) {}
            return;
          }
        }
        else if (message instanceof ClassTransferRequestMessage)
        {
          // This is a request for a Java class file.
          ClassTransferRequestMessage msg = (ClassTransferRequestMessage)
                                            message;
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Class transfer request from client " +
                                 clientID + " for class " + msg.getClassName());
          sendClassFile(msg);
        }
      }
      catch (InterruptedIOException iioe)
      {
        // If this exception was thrown, it was because the socket timeout was
        // reached.  We need to send a keepalive message.
        try
        {
          KeepAliveMessage kaMsg =
               new KeepAliveMessage(getMessageID());
          writer.writeElement(kaMsg.encode());
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Sent keepalive to client " + clientID);
        }
        catch (IOException ioe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Unable to send keepalive to client " +
                                 clientID + ":  " + ioe);
        }
      }
      catch (IOException ioe)
      {
        // Some other I/O related exception was thrown
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "I/O exception from client " + clientID +
                               ":  " + ioe);

        // Some problem occurred.  See if this is a second consecutive failure
        // and if so, end the connection and this thread.  Otherwise set a
        // flag that will be used to detect a second consecutive failure
        if (consecutiveFailures)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Consecutive failures on client connection " +
                                 clientID + " -- closing");
          try
          {
            socket.close();
          } catch (IOException ioe2) {}
          slamdServer.getClientListener().connectionLost(this);
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
                               "Exception handling message from client " +
                               clientID + ":  " + se);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(se));
        se.printStackTrace();
      }
      catch (ASN1Exception ae)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception decoding message from client " +
                               clientID + ":  " + ae);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ae));
        ae.printStackTrace();
      }
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ClientConnection.run() for client " +
                           clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Leaving ClientConnection.run() for client " +
                           clientID);
  }



  /**
   * Sends the specified message to the client.
   *
   * @param  message  The message to send to the client.
   */
  public void sendMessage(Message message)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.sendMessage() for client " +
                           clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.sendMessage() for client " +
                           clientID);

    try
    {
      writer.writeElement(message.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Wrote message to client " + clientID + " -- " +
                             message.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not write message type " +
                             message.getMessageType() +
                             " to client " + clientID  + ":  " + ioe);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      ioe.printStackTrace();
    }
  }



  /**
   * Sends a class transfer response message to the client that either contains
   * the requested class information or a response code that indicates why it
   * could not be provided.
   *
   * @param  transferRequest  The class transfer request message for which the
   *                          response is to be provided.
   */
  public void sendClassFile(ClassTransferRequestMessage transferRequest)
  {
    // First, make sure that the server knows about the specified class.
    String className = transferRequest.getClassName();

    JobClassLoader jobClassLoader =
         new JobClassLoader(getClass().getClassLoader(),
                            slamdServer.getClassPath());
    byte[] classBytes;
    try
    {
      classBytes = jobClassLoader.getJobClassBytes(className);
    }
    catch (SLAMDException se)
    {
      ClassTransferResponseMessage responseMessage =
           new ClassTransferResponseMessage(transferRequest.getMessageID(),
                    Constants.MESSAGE_RESPONSE_CLASS_NOT_FOUND, className,
                    new byte[0]);
      sendMessage(responseMessage);
      return;
    }

    try
    {
      // Send the encoded response back to the client.
      ClassTransferResponseMessage responseMessage =
           new ClassTransferResponseMessage(transferRequest.getMessageID(),
                                            Constants.MESSAGE_RESPONSE_SUCCESS,
                                            className, classBytes);
      sendMessage(responseMessage);
      return;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      // An error occurred while retrieving the class data.  Send a "server
      // error" message back to the client.
      ClassTransferResponseMessage responseMessage =
           new ClassTransferResponseMessage(transferRequest.getMessageID(),
                    Constants.MESSAGE_RESPONSE_SERVER_ERROR, className,
                    new byte[0]);
      sendMessage(responseMessage);
      return;
    }
  }



  /**
   * Retrieves the message ID to use in the next message originating from the
   * server.
   *
   * @return  The message ID to use in the next message originating from the
   *          server.
   */
  public synchronized int getMessageID()
  {
    int returnValue = messageID;
    messageID += 2;
    return returnValue;
  }



  /**
   * Retrieves a message with the specified message ID and type from the queue
   * used to hold responses to solicited messages.  This will wait for a
   * configurable maximum amount of time for the message to appear before
   * returning <CODE>null</CODE>.
   *
   * @param  messageID    The message ID for the message that is expected.
   * @param  messageType  The type of message that is expected.
   *
   * @return  The message with the specified message ID and type from the queue
   *          used to hold responses to solicited messages.
   */
  public Message getResponse(int messageID, int messageType)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.getResponse(" + messageID +
                           ", " + messageType + ") for client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.getResponse(" + messageID +
                           ", " + messageType + ") for client " + clientID);

    long stopWaitingTime = System.currentTimeMillis() +
                           (maxResponseWaitTime * 1000);

    while (System.currentTimeMillis() < stopWaitingTime)
    {
      synchronized (messageListMutex)
      {
        for (int i=0; i < messageList.size(); i++)
        {
          Message message = messageList.get(i);
          int msgID   = message.getMessageID();
          int msgType = message.getMessageType();

          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Client connection for " + clientID +
                                 " looking at message " + msgID + " of type " +
                                 msgType + " -- " + message.toString());

          if (msgID == messageID)
          {
            if (msgType == messageType)
            {
              messageList.remove(i);
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                     "Returning requested message " +
                                     messageID + " of type " + messageType +
                                     " from client " + clientID);
              return message;
            }
            else
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "WARNING:  Message ID " + msgID +
                                     " was of the wrong type (expected " +
                                     messageType + ", got " + msgType +
                                     ") for client connection " + clientID);
            }
          }
          else
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Ignoring message of type " + msgType +
                                   " sent to client " + clientID +
                                   " with an unexpected message ID (expected " +
                                   messageID + ", got " + msgID + ')');
          }
        }
      }

      // The requested message wasn't found, so wait a short period of time
      // before checking again
      if (System.currentTimeMillis() < stopWaitingTime)
      {
        try
        {
          Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
        } catch (InterruptedException ie) {}
      }
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Timeout while waiting for message " + messageID +
                           " of type " + messageType + " from client " +
                           clientID);
    return null;
  }



  /**
   * Sends a job request message to the client.
   *
   * @param  job           The information that should be included in the job
   *                       request.
   * @param  clientNumber  The client number for this client.
   *
   * @return  The response from the client.
   */
  public JobResponseMessage sendJobRequest(Job job, int clientNumber)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.sendJobRequest(" +
                           job.getJobID() + ") for client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.sendJobRequest(" +
                           job.getJobID() + ") for client " + clientID);

    // First, make sure that we aren't already processing a job.  If we are,
    // then send back a rejected message on behalf of the client.
    if (jobInProgress != null)
    {
      JobResponseMessage response =
           new JobResponseMessage(getMessageID(), job.getJobID(),
                    Constants.MESSAGE_RESPONSE_JOB_REQUEST_REFUSED,
                    "Already processing job " + jobInProgress.getJobID());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Client " + clientID + " already processing job " +
                             jobInProgress.getJobID());
      return response;
    }


    this.jobInProgress = job;
    int messageID = getMessageID();
    String jobID = job.getJobID();

    JobRequestMessage request =
         new JobRequestMessage(messageID, jobID, job.getJobClassName(),
                               job.getStartTime(), job.getStopTime(),
                               clientNumber, job.getDuration(),
                               job.getThreadsPerClient(),
                               job.getThreadStartupDelay(),
                               job.getCollectionInterval(),
                               job.getParameterList());

    try
    {
      writer.writeElement(request.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Sent job request to client " + clientID + " -- " +
                             request.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send job request to client " +
                             clientID + ":  " + ioe);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      ioe.printStackTrace();
      return new JobResponseMessage(messageID, jobID,
                      Constants.MESSAGE_RESPONSE_LOCAL_ERROR);
    }


    Message response = getResponse(messageID,
                                 Constants.MESSAGE_TYPE_JOB_RESPONSE);
    if (response == null)
    {
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "No response to job request from client " +
                           clientID);
     return new JobResponseMessage(messageID, jobID,
                      Constants.MESSAGE_RESPONSE_NO_RESPONSE);
    }
    else
    {
      // Make sure that the job request was accepted.  If not, then set the
      // job in progress to null.
      JobResponseMessage jobResponse = (JobResponseMessage) response;
      if (jobResponse.getResponseCode() != Constants.MESSAGE_RESPONSE_SUCCESS)
      {
        jobInProgress = null;
      }

      return jobResponse;
    }
  }



  /**
   * Sends a job control request to the client for the specified job.
   *
   * @param  job          The job with which the request is associated.
   * @param  controlType  The type of operation being requested.
   *
   * @return  The response from the client.
   */
  public JobControlResponseMessage sendJobControlRequest(Job job,
                                                         int controlType)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.sendJobControlRequest(" +
                           job.getJobID() + ", " + controlType +
                           ") for client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.sendJobControlRequest(" +
                           job.getJobID() + ", " + controlType +
                           ") for client " + clientID);

    // First, make sure that the job requested is the one that we know about.
    // If not, return a failure message on behalf of the client
    if ((jobInProgress == null) ||
        (! jobInProgress.getJobID().equals(job.getJobID())))
    {
      JobControlResponseMessage response =
           new JobControlResponseMessage(getMessageID(), job.getJobID(),
                    Constants.MESSAGE_RESPONSE_NO_SUCH_JOB,
                    "Job " + job.getJobID() +
                    " has not been defined to this client");
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Job not known to client " + clientID);
      return response;
    }


    int    messageID = getMessageID();
    String jobID     = job.getJobID();

    JobControlRequestMessage request =
         new JobControlRequestMessage(messageID, jobID, controlType);
    try
    {
      writer.writeElement(request.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Sent job control request to client " +
                             clientID + " -- " + request.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send job control request to client " +
                             clientID + ":  " + ioe);
      ioe.printStackTrace();
      return new JobControlResponseMessage(messageID, jobID,
                      Constants.MESSAGE_RESPONSE_LOCAL_ERROR);
    }


    Message response =
         getResponse(messageID,
                     Constants.MESSAGE_TYPE_JOB_CONTROL_RESPONSE);
    if (response == null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "No response to job control request from client " +
                             clientID);
      return new JobControlResponseMessage(messageID, jobID,
                      Constants.MESSAGE_RESPONSE_NO_RESPONSE);
    }
    else
    {
      // Make sure that the job request was accepted.  If not, then set the
      // job in progress to null.
      JobControlResponseMessage jobControlResponse = (JobControlResponseMessage)
                                                     response;

      switch (jobControlResponse.getResponseCode())
      {
        case Constants.MESSAGE_RESPONSE_CLASS_NOT_FOUND:
        case Constants.MESSAGE_RESPONSE_CLASS_NOT_VALID:
        case Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE:
        case Constants.MESSAGE_RESPONSE_NO_SUCH_JOB:
             jobInProgress = null;
      }

      return jobControlResponse;
    }
  }



  /**
   * Sends a status request message to the client as a general status request.
   * This request will not be for job-specific information.
   *
   * @return  The status response message corresponding to this status request.
   */
  public StatusResponseMessage sendStatusRequestMessage()
  {
    return sendStatusRequestMessage(null);
  }



  /**
   * Sends a status request message to the client requesting information about
   * the specified job.
   *
   * @param  jobID  The ID of the job for which to request status information.
   *
   * @return  The status response message corresponding to this status request.
   */
  public StatusResponseMessage sendStatusRequestMessage(String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.sendStatusRequestMessage(" +
                           jobID + ") for client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.sendStatusRequestMessage(" +
                           jobID + ") for client " + clientID);

    int messageID = getMessageID();
    StatusRequestMessage request = null;
    if ((jobID == null) || (jobID.length() == 0))
    {
      request = new StatusRequestMessage(messageID);
    }
    else
    {
      request = new StatusRequestMessage(messageID, jobID);
    }


    try
    {
      writer.writeElement(request.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Sent status request message to client " +
                             clientID + " -- " + request.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send status request message to " +
                             "client " + clientID + ":  " + ioe);
      ioe.printStackTrace();
      return new StatusResponseMessage(messageID,
                                       Constants.MESSAGE_RESPONSE_NO_RESPONSE,
                                       Constants.CLIENT_STATE_UNKNOWN,
                                       "Unable to send status request to " +
                                       "client " + clientID + ":  " +
                                       ioe);
    }


    Message response =
         getResponse(messageID,
                     Constants.MESSAGE_TYPE_STATUS_RESPONSE);
    if (response == null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "No response to status request from client " +
                             clientID);
      return new StatusResponseMessage(messageID,
                                       Constants.MESSAGE_RESPONSE_NO_RESPONSE,
                                       Constants.CLIENT_STATE_UNKNOWN,
                                       "Did not receive status response " +
                                       "from client " + clientID);
    }
    else
    {
      return (StatusResponseMessage) response;
    }
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
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientConnection.sendServerShutdownMessage() " +
                           "for client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientConnection.sendServerShutdownMessage() " +
                           "for client " + clientID);

    if (jobInProgress != null)
    {
      sendJobControlRequest(jobInProgress,
                            Constants.JOB_CONTROL_TYPE_STOP_DUE_TO_SHUTDOWN);
      while (jobInProgress != null)
      {
        try
        {
          Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
        } catch (InterruptedException ie) {}
      }
    }

    ServerShutdownMessage shutdownMessage =
         new ServerShutdownMessage(getMessageID());

    try
    {
      writer.writeElement(shutdownMessage.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Sent shutdown message to client " + clientID +
                             " -- " + shutdownMessage.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send shutdown message to client " +
                             clientID + ":  " + ioe);
      ioe.printStackTrace();
      slamdServer.getClientListener().connectionLost(this);
    }

    if (closeSocket)
    {
      keepListening = false;
      try
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Closing socket for client " + clientID);
        socket.close();
      } catch (IOException ioe) {}
    }
  }



  /**
   * Retrieves a string with information about this client connection.
   *
   * @return  A string with information about this client connection.
   */
  @Override()
  public String toString()
  {
    return connectionID + " (" + clientID + ' ' + clientIPAddress + ')';
  }



  /**
   * Retrieves a string with basic status information about the current state of
   * the client.
   *
   * @return  A string with basic status information about the current state of
   *          the client.
   */
  public String getStatusString()
  {
    if (jobInProgress == null)
    {
      if (restrictedMode)
      {
        return "Idle (restricted use)";
      }
      else
      {
        return "Idle";
      }
    }
    else
    {
      return "Processing " + jobInProgress.getJobName() + " job " +
             jobInProgress.getJobID();
    }
  }



  /**
   * Compares this client connection with the provided object.  The given object
   * must be a client connection object, and the comparison will be made based
   * on the lexicographic ordering of the associated client IDs.
   *
   * @param  o  The client connection object to compare against this client
   *            connection.
   *
   * @return  A negative value if this client connection should come before the
   *          provided client connection in a sorted list, a positive value if
   *          this client connection should come after the provided client
   *          connection in a sorted list, or zero if there is no difference in
   *          their ordering as far as this method is concerned.
   *
   * @throws  ClassCastException  If the provided object is not a client
   *                              connection object.
   */
  public int compareTo(Object o)
         throws ClassCastException
  {
    ClientConnection c = (ClientConnection) o;
    return clientID.compareTo(c.clientID);
  }
}

