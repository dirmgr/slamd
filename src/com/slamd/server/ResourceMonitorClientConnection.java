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
import java.util.HashMap;
import java.util.Iterator;

import com.slamd.admin.AccessManager;
import com.slamd.admin.AdminAccess;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
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
 * resource monitor client connection.  It takes care of reading messages in
 * from the client and provides methods for sending messages to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ResourceMonitorClientConnection
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

  // Indicates whether this client supports time synchronization.
  private boolean supportsTimeSync;

  // The client listener that accepted this connection.
  private ResourceMonitorClientListener clientListener;

  // The time that this connection was established.
  private Date establishedTime;

  // The set of jobs currently being processed by this monitor client.
  private HashMap<String,Job> jobHash;

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

  // A mutex used to provide threadsafe access to the job hash.
  private final Object jobHashMutex;

  // A mutex used to provide threadsafe access to the message list
  private final Object messageListMutex;

  // The SLAMD server with which this client connection is associated
  private SLAMDServer slamdServer;

  // The network socket used to communicate with the client
  private Socket socket;

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
   * Creates a new resource monitor connection that the server will use to
   * communicate with the client.
   *
   * @param  slamdServer     The SLAMD server with which this connection is
   *                         associated.
   * @param  clientListener  The resource monitor client listener that accepted
   *                         this connection.
   * @param  socket          The socket that is used to communicate with the
   *                         client.
   * @param  connectionID    The unique identifier associated with the client.
   *
   * @throws  SLAMDException  If a problem occurs setting up the connection.
   */
  public ResourceMonitorClientConnection(SLAMDServer slamdServer,
              ResourceMonitorClientListener clientListener, Socket socket,
              String connectionID)
         throws SLAMDException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientConnection constructor");

    setName("Resource Monitor Client Connection " + connectionID);

    this.slamdServer      = slamdServer;
    this.clientListener   = clientListener;
    this.socket           = socket;
    this.clientID         = "(unknown)";
    this.clientIPAddress  = socket.getInetAddress().getHostAddress();
    this.connectionID     = connectionID;
    this.supportsTimeSync = false;

    this.establishedTime = new Date();

    messageList      = new ArrayList<Message>();
    messageListMutex = new Object();
    messageID        = 1;
    keepAliveTime    = slamdServer.getClientListener().getKeepAliveInterval();
    jobHash          = new HashMap<String,Job>();
    jobHashMutex     = new Object();

    maxResponseWaitTime =
         slamdServer.getClientListener().getMaxResponseWaitTime();


    // Send the hello response to the client
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
                                 "Received hello request from resource " +
                                 "monitor client " + toString());
          this.clientID         = helloRequest.getClientID();
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
                                     "Rejected new monitor client connection " +
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
                                     "Rejected new monitor client connection " +
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
              String msg = msgBuffer.toString();
              HelloResponseMessage helloResp =
                   new HelloResponseMessage(0, resultCode, msg, -1);
              writer.writeElement(helloResp.encode());
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "Rejected new monitor client connection " +
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
          throw new SLAMDException("Expected hello request from client but " +
                                   "got instance of " +
                                   message.getClass().getName());
        }
      }
      catch (ASN1Exception ae)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Unable to parse hello request message from " +
                               toString() + ":  " + ae);
        ae.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ae));
        throw new SLAMDException("Unable to parse hello request message from " +
                                 toString() + ":  " + ae, ae);
      }
      catch (SLAMDException se)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Unable to obtain hello request message from " +
                               toString() + ":  " + se);
        se.printStackTrace();
        throw se;
      }


      // Make sure we don't already have a resource monitor connection from the
      // same client system.
      ResourceMonitorClientConnection[] conns =
           clientListener.getMonitorClientList();
      for (int i=0; i < conns.length; i++)
      {
        if (conns[i].getClientID().equals(clientID))
        {
          try
          {
            HelloResponseMessage helloResponse =
                 new HelloResponseMessage(helloRequest.getMessageID(),
                          Constants.MESSAGE_RESPONSE_CLIENT_REJECTED,
                          "A resource monitor client connection has already " +
                          "been established with client ID \"" + clientID +
                          "\".", -1);
            writer.writeElement(helloResponse.encode());
            socket.close();
          }
          catch (IOException ioe)
          {
            try
            {
              socket.close();
            } catch (Exception e) {}
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                   JobClass.stackTraceToString(ioe));
            throw new SLAMDException("Unable to send the hello response to " +
                                     "the resource monitor client:  " + ioe,
                                     ioe);
          }

          throw new SLAMDException("Rejected resource monitor client " +
                                   "connection due to duplicate client ID -- " +
                                   clientID + '.');
        }
      }

      long serverTime = (supportsTimeSync ? System.currentTimeMillis() : -1);
      HelloResponseMessage helloResp =
           new HelloResponseMessage(0, Constants.MESSAGE_RESPONSE_SUCCESS,
                                    respMesg, serverTime);
      writer.writeElement(helloResp.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                             "Accepted new resource monitor client " +
                             "connection " + clientID + " from " +
                             socket.getInetAddress().toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "I/O exception in client handshake for " +
                             toString() + ":  " + ioe);
      ioe.printStackTrace();
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ResourceMonitorClientConnection " +
                           "constructor");
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
   * Indicates whether at least one job is in progress.
   *
   * @return  <CODE>true</CODE> if at least one job is in progress, or
   *          <CODE>false</CODE> if there are no jobs in progress.
   */
  public boolean jobInProgress()
  {
    synchronized (jobHashMutex)
    {
      return (! jobHash.isEmpty());
    }
  }



  /**
   * Retrieves the list of jobs for which this client is collecting resource
   * utilization data.
   *
   * @return  The list of jobs for which this client is collecting resource
   *          utilization data.
   */
  public Job[] getJobsInProgress()
  {
    synchronized (jobHashMutex)
    {
      Job[] jobs = new Job[jobHash.size()];

      int i=0;
      Iterator iterator = jobHash.values().iterator();
      while (iterator.hasNext())
      {
        jobs[i++] = (Job) iterator.next();
      }

      return jobs;
    }
  }



  /**
   * Retrieves the list of jobs for which this client is collecting resource
   * utilization data.
   *
   * @return  The list of jobs for which this client is collecting resource
   *          utilization data.
   */
  public String[] getJobIDsInProgress()
  {
    synchronized (jobHashMutex)
    {
      String[] jobIDs = new String[jobHash.size()];

      int i=0;
      Iterator iterator = jobHash.values().iterator();
      while (iterator.hasNext())
      {
        jobIDs[i++] = ((Job) iterator.next()).getJobID();
      }

      return jobIDs;
    }
  }



  /**
   * Retrieves information about the specified job in progress.
   *
   * @param  jobID  The job ID of the job for which to retrieve information.
   *
   * @return  Information about the specified job in progress, or
   *          <CODE>null</CODE> if no information is available.
   */
  public Job getJobInProgress(String jobID)
  {
    synchronized (jobHashMutex)
    {
      return jobHash.get(jobID);
    }
  }



  /**
   * Adds the provided job to the list of jobs in progress.
   *
   * @param  job  The job to add to the list of jobs in progress.
   */
  public void addJobInProgress(Job job)
  {
    synchronized (jobHashMutex)
    {
      jobHash.put(job.getJobID(), job);
    }
  }



  /**
   * Removes the provided job from the list of jobs in progress.
   *
   * @param  job  The job to remove from the list of jobs in progress.
   */
  public void removeJobInProgress(Job job)
  {
    synchronized (jobHashMutex)
    {
      jobHash.remove(job.getJobID());
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
                           "In ResourceMonitorClientConnection.run() for " +
                           clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection.run() for " +
                           clientID);

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
                               " seconds for monitor client " + clientID);
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Could not set timeout for monitor client " +
                               "connection " + clientID);
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
                                 "Detected connection closure from monitor " +
                                 "client " + clientID);

          try
          {
            socket.close();
          } catch (IOException ioe2) {}
          clientListener.connectionLost(this);
          return;
        }

        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Read a message from monitor client " +
                               clientID);
        Message message = Message.decode(element);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Decoded message from monitor client " +
                               clientID + ":  " + message.toString());


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
                                   "Queueing solicited response from " +
                                   "monitor client " + clientID);
          }
        }
        else if (message instanceof JobCompletedMessage)
        {
          // This is a job completed message, so update the scheduler that
          // the job is done
          JobCompletedMessage msg = (JobCompletedMessage) message;
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Job completed response from monitor client " +
                                 clientID);
          Job job = getJobInProgress(msg.getJobID());
          if (job != null)
          {
            job.resourceClientDone(this, msg);
            removeJobInProgress(job);
          }
        }
        else if (message instanceof StatusResponseMessage)
        {
          // This is a status response message, but not a solicited one.  It
          // almost certainly means the client is shutting down
          StatusResponseMessage msg = (StatusResponseMessage)
                                           message;
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Status response from monitor client " +
                                 clientID);
          if (msg.getClientStatusCode() ==
              Constants.CLIENT_STATE_SHUTTING_DOWN)
          {
            clientListener.connectionLost(this);
            try
            {
              socket.close();
            } catch (IOException ioe) {}
            return;
          }
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
                                 "Sent keepalive to monitor client " +
                                 clientID);
        }
        catch (IOException ioe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Unable to send keepalive to monitor " +
                                 clientID + ":  " + ioe);
        }
      }
      catch (IOException ioe)
      {
        // Some other I/O related exception was thrown
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "I/O exception from monitor " + clientID +
                               ":  " + ioe);

        // Some problem occurred.  See if this is a second consecutive failure
        // and if so, end the connection and this thread.  Otherwise set a
        // flag that will be used to detect a second consecutive failure
        if (consecutiveFailures)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Consecutive failures on connection to " +
                                 "monitor client " + clientID + " -- closing");
          try
          {
            socket.close();
          } catch (IOException ioe2) {}
          clientListener.connectionLost(this);
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
                               "Exception handling message from monitor " +
                               "client " + clientID + ":  " + se);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(se));
        se.printStackTrace();
      }
      catch (ASN1Exception ae)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception decoding message from monitor " +
                               "client " + connectionID + ":  " + ae);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ae));
        ae.printStackTrace();

        try
        {
          socket.close();
        } catch (Exception e) {}
        clientListener.connectionLost(this);
        return;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
             "Exception in monitor client processing for client " +
             connectionID + ":  " + e);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(e));
        e.printStackTrace();

        try
        {
          socket.close();
        } catch (Exception e2) {}
        clientListener.connectionLost(this);
        return;
      }
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ResourceMonitorClientConnection.run() " +
                           "for monitor client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Leaving ResourceMonitorClientConnection.run() " +
                           "for monitor client " + clientID);
  }



  /**
   * Sends the specified message to the client.
   *
   * @param  message  The message to send to the client.
   */
  public void sendMessage(Message message)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientConnection.sendMessage() " +
                           "for monitor client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection.sendMessage() " +
                           "for monitor client " + clientID);

    try
    {
      writer.writeElement(message.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Wrote message to monitor client " + clientID +
                             " -- " + message.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not write message type " +
                             message.getMessageType() +
                             " to monitor client " + clientID + ":  " + ioe);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      ioe.printStackTrace();
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
                           "In ResourceMonitorClientConnection.getResponse(" +
                           messageID + ", " + messageType + ") for " +
                           clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection.getResponse(" +
                           messageID + ", " + messageType + ") for " +
                           clientID);

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
                                 "Monitor client connection for " + clientID +
                                 " looking at message " + msgID + " of type " +
                                 msgType + " -- " + message.toString());

          if (msgID == messageID)
          {
            if (msgType == messageType)
            {
              messageList.remove(i);
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                     "Returning requested message " +
                                     messageID + " from monitor client " +
                                     clientID);
              return message;
            }
            else
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                     "WARNING:  Message ID " + msgID +
                                     " was of the wrong type (expected " +
                                     messageType + ", got " + msgType +
                                     ") for monitor client connection " +
                                     clientID);
            }
          }
          else
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Ignoring message of type " + msgType +
                                   " sent to monitor client " + clientID +
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
                           "from monitor client " + clientID);
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
                           "In ResourceMonitorClientConnection." +
                           "sendJobRequest(" + job.getJobID() +
                           ") for monitor client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection." +
                           "sendJobRequest(" + job.getJobID() +
                           ") for monitor client " + clientID);


    addJobInProgress(job);
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
                             "Sent job request to monitor client " + clientID +
                             " -- " + request.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send job request to monitor client " +
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
                           "No response to job request from monitor client " +
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
        removeJobInProgress(job);
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
                           "In ResourceMonitorClientConnection." +
                           "sendJobControlRequest(" + job.getJobID() + ", " +
                           controlType + ") for monitor client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection." +
                           "sendJobControlRequest(" + job.getJobID() + ", " +
                           controlType + ") for monitor client " + clientID);

    // First, make sure that the job requested is the one that we know about.
    // If not, return a failure message on behalf of the client
    String jobID = job.getJobID();
    if (getJobInProgress(jobID) == null)
    {
      JobControlResponseMessage response =
           new JobControlResponseMessage(getMessageID(), job.getJobID(),
                    Constants.MESSAGE_RESPONSE_NO_SUCH_JOB,
                    "Job " + job.getJobID() +
                    " has not been defined to this client");
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Job not known to monitor client " + clientID);
      return response;
    }


    int messageID = getMessageID();
    JobControlRequestMessage request =
         new JobControlRequestMessage(messageID, jobID, controlType);
    try
    {
      writer.writeElement(request.encode());
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Sent job control request to monitor client " +
                             clientID + " -- " + request.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send job control request to monitor " +
                             "client " + clientID + ":  " + ioe);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
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
                             "No response to job control request from " +
                             "monitor client " + clientID);
      return new JobControlResponseMessage(messageID, jobID,
                      Constants.MESSAGE_RESPONSE_NO_RESPONSE);
    }
    else
    {
      // Make sure that the job request was accepted.  If not, then set the
      // job in progress to null.
      JobControlResponseMessage jobControlResponse = (JobControlResponseMessage)
                                                     response;
      if (jobControlResponse.getResponseCode() ==
          Constants.MESSAGE_RESPONSE_NO_SUCH_JOB)
      {
        removeJobInProgress(job);
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
                           "In ResourceMonitorClientConnection." +
                           "sendStatusRequestMessage(" + jobID + ") for " +
                           "monitor client " + clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection." +
                           "sendStatusRequestMessage(" + jobID + ") for " +
                           "monitor client " + clientID);

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
                             "Sent status request message to monitor client " +
                             clientID + " -- " + request.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send status request message to " +
                             "monitor client " + clientID + ":  " + ioe);
      ioe.printStackTrace();
      return new StatusResponseMessage(messageID,
                                       Constants.MESSAGE_RESPONSE_NO_RESPONSE,
                                       Constants.CLIENT_STATE_UNKNOWN,
                                       "Unable to send status request:  " +
                                       ioe);
    }


    Message response =
         getResponse(messageID,
                     Constants.MESSAGE_TYPE_STATUS_RESPONSE);
    if (response == null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "No response to status request from monitor " +
                             "client " + clientID);
      return new StatusResponseMessage(messageID,
                                       Constants.MESSAGE_RESPONSE_NO_RESPONSE,
                                       Constants.CLIENT_STATE_UNKNOWN,
                                       "Did not receive status response");
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
                           "In ResourceMonitorClientConnection." +
                           "sendServerShutdownMessage() for monitor client " +
                           clientID);
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientConnection." +
                           "sendServerShutdownMessage() for monitor client " +
                           clientID);

    Job[] jobsInProgress = getJobsInProgress();
    for (int i=0; i < jobsInProgress.length; i++)
    {
      sendJobControlRequest(jobsInProgress[i],
                            Constants.JOB_CONTROL_TYPE_STOP_DUE_TO_SHUTDOWN);

      while (jobInProgress())
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
                             "Sent shutdown message to monitor client " +
                             clientID + " -- " + shutdownMessage.toString());
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Could not send shutdown message to monitor " +
                             "client " + clientID + ":  " + ioe);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      ioe.printStackTrace();
      clientListener.connectionLost(this);
    }

    if (closeSocket)
    {
      keepListening = false;
      try
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Closing socket for monitor client " + clientID);
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
    String[] jobIDs = getJobIDsInProgress();
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      return "Idle";
    }
    else if (jobIDs.length == 1)
    {
      return "Processing job " + jobIDs[0];
    }
    else
    {
      String returnStr = "Processing job " + jobIDs[0];
      for (int i=1; i < jobIDs.length; i++)
      {
        returnStr += ", " + jobIDs[i];
      }
      return returnStr;
    }
  }



  /**
   * Compares this resource monitor client connection with the provided object.
   * The given object must be a resource monitor client connection object, and
   * the comparison will be made based on the lexicographic ordering of the
   * associated client IDs.
   *
   * @param  o  The resource monitor client connection object to compare against
   *            this resource monitor client connection.
   *
   * @return  A negative value if this resource monitor client connection should
   *          come before the provided resource monitor client connection in a
   *          sorted list, a positive value if this resource monitor client
   *          connection should come after the provided resource monitor client
   *          connection in a sorted list, or zero if there is no difference in
   *          their ordering as far as this method is concerned.
   *
   * @throws  ClassCastException  If the provided object is not a resource
   *                              monitor client connection object.
   */
  public int compareTo(Object o)
         throws ClassCastException
  {
    ResourceMonitorClientConnection c = (ResourceMonitorClientConnection) o;
    return clientID.compareTo(c.clientID);
  }
}

