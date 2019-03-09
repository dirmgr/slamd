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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.net.ssl.SSLServerSocketFactory;

import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.db.SLAMDDB;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.JobCompletedMessage;
import com.slamd.message.StatusRequestMessage;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class implements the client listener that the SLAMD server uses to
 * listen for connections from resource monitor clients.  If fewer than the
 * maximum connections are established, then the connection will be accepted and
 * a new connection thread will be spawned to handle operations on that monitor
 * connection.  If the maximum number of connections are already in use, then
 * the new connection will be rejected.
 *
 *
 * @author   Neil A. Wilson
 */
public class ResourceMonitorClientListener
       extends Thread
       implements ConfigSubscriber
{
  // Indicates whether this listener should continue accepting client
  // connections
  private boolean keepListening;

  // Indicates whether this listener has actually stopped.
  private boolean hasStopped;

  // Indicates whether this listener requires clients to authenticate.
  private boolean requireAuthentication;

  // Indicates whether this listener should use SSL.
  private boolean useSSL;

  // The length of time in seconds that should pass between keepalive messages
  private int keepaliveInterval;

  // The port on which the server is listening for new connections
  private int listenPort;

  // The maximum number of simultaneous clients that may be connected to the
  // server at once.
  private int maxClients;

  // The maximum length of time in seconds that the client connection will wait
  // for a response from a solicited message
  private int maxResponseWaitTime;

  // A map of all the resource monitor client connections that have been
  // established.
  private LinkedHashMap<String,ResourceMonitorClientConnection> connectionHash;

  // A mutex used to protect multithreaded access to the connection hash.
  private final Object connectionHashMutex;

  // The configuration database associated with the SLAMD server
  private SLAMDDB configDB;

  // The server socket used to listen for new connections
  private ServerSocket serverSocket;

  // The SLAMD server with which this client listener is associated
  private SLAMDServer slamdServer;



  /**
   * Creates a new listener to accept resource monitor client connections.
   *
   * @param  slamdServer  The SLAMD server with which this listener is
   *                      associated.
   */
  public ResourceMonitorClientListener(SLAMDServer slamdServer)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Entering ResourceMonitorClientListener " +
                           "constructor");

    setName("Resource Monitor Client Listener");

    // Initialize all the instance variables
    this.slamdServer    = slamdServer;
    maxClients          = Constants.NO_MAX_CLIENTS;
    connectionHash      =
         new LinkedHashMap<String,ResourceMonitorClientConnection>();
    connectionHashMutex = new Object();
    keepListening       = true;
    hasStopped          = true;


    // Read the appropriate information from the configuration
    configDB = slamdServer.getConfigDB();
    configDB.registerAsSubscriber(this);

    listenPort = Constants.DEFAULT_MONITOR_LISTENER_PORT_NUMBER;
    String portStr =
         configDB.getConfigParameter(Constants.PARAM_MONITOR_LISTENER_PORT);
    if ((portStr != null) && (portStr.length() != 0))
    {
      try
      {
        listenPort = Integer.parseInt(portStr);
      } catch (NumberFormatException nfe) {}
    }

    ClientListener clientListener = slamdServer.getClientListener();
    keepaliveInterval     = clientListener.getKeepAliveInterval();
    maxClients            = clientListener.getMaxClients();
    maxResponseWaitTime   = clientListener.getMaxResponseWaitTime();
    requireAuthentication = clientListener.requireAuthentication();

    useSSL = clientListener.useSSL();
    if (useSSL)
    {
      String keyStore = slamdServer.getSSLKeyStore();
      if ((keyStore != null) && (keyStore.length() > 0))
      {
        System.setProperty(Constants.SSL_KEY_STORE_PROPERTY, keyStore);
      }

      String keyStorePassword = slamdServer.getSSLKeyStorePassword();
      if ((keyStorePassword != null) && (keyStorePassword.length() > 0))
      {
        System.setProperty(Constants.SSL_KEY_PASSWORD_PROPERTY,
                           keyStorePassword);
      }

      String trustStore = slamdServer.getSSLTrustStore();
      if ((trustStore != null) && (trustStore.length() > 0))
      {
        System.setProperty(Constants.SSL_TRUST_STORE_PROPERTY, trustStore);
      }

      String trustStorePassword = slamdServer.getSSLTrustStorePassword();
      if ((trustStorePassword != null) && (trustStorePassword.length() > 0))
      {
        System.setProperty(Constants.SSL_TRUST_PASSWORD_PROPERTY,
                           trustStorePassword);
      }
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ResourceMonitorClientListener constructor");
  }



  /**
   * Indicates that the listener should start listening for client connections.
   */
  public void startListening()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientListener.startListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientListener.startListening()");
    keepListening = true;
    start();
  }



  /**
   * Indicates that the listener should stop listening for client connections.
   * It will also notify all connected clients that the listener is shutting
   * down.
   */
  public void stopListening()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientListener.stopListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientListener.stopListening()");

    // Set the flag that indicates the server should no longer be listening
    keepListening = false;


    // Close the server socket so that no more connections will be accepted
    try
    {
      serverSocket.close();
    } catch (Exception e) {}


    // Iterate through all the connections and inform them that the server is
    // shutting down.  The sendServerShutdownMessage() method will also close
    // the connections.
    synchronized (connectionHashMutex)
    {
      Iterator iterator = connectionHash.values().iterator();
      while (iterator.hasNext())
      {
        ResourceMonitorClientConnection clientConnection =
             (ResourceMonitorClientConnection) iterator.next();
        clientConnection.sendServerShutdownMessage(true);
      }

      connectionHash.clear();
    }
  }



  /**
   * This method will not return until the client listener has actually stopped.
   * Note that it does not stop the listener -- you should first call the
   * <CODE>stopListening</CODE> method to signal the listener that it needs to
   * stop.
   */
  public void waitForStop()
  {
    while (! hasStopped)
    {
      try
      {
        Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
      } catch (InterruptedException ie) {}
    }
  }



  /**
   * Creates the server socket and listens for new connections.  If the
   * connection will be accepted, then a new connection thread will be spawned
   * to handle it.  If the connection will not be accepted, then it will be
   * rejected here.
   */
  @Override()
  public void run()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientLister.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ResourceMonitorClientListener.run()");

    hasStopped = false;

    if (useSSL)
    {
      try
      {
        SSLServerSocketFactory socketFactory =
             (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = socketFactory.createServerSocket(listenPort);
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Listening for SSL-based resource monitor " +
                               "client connections on port " + listenPort);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to create SSL server socket:  " + e);
        hasStopped = true;
        return;
      }
    }
    else
    {
      try
      {
        serverSocket = new ServerSocket(listenPort);
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Listening for resource monitor client " +
                               "connections on port " + listenPort);
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to create server socket:  " + ioe);
        hasStopped = true;
        return;
      }
    }


    while (keepListening)
    {
      Socket clientSocket = null;

      try
      {
        clientSocket = serverSocket.accept();
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "New resource monitor client connection " +
                               "received from " +
                               clientSocket.getInetAddress().toString());
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception accepting resource monitor client " +
                               "connection:  " + ioe);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        continue;
      }

      try
      {
        synchronized (connectionHashMutex)
        {
          if ((maxClients > 0) && (connectionHash.size() >= maxClients))
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                   "Refusing monitor connection from " +
                                   clientSocket.getInetAddress().toString() +
                                   " -- too many concurrent connections");

            // Normally, messages that the server originates are odd-numbered.
            // However, in this case the client should always send a hello as
            // the first operation that deserves a response, so we can use
            // message ID 0 for that.
            HelloResponseMessage helloResp =
                 new HelloResponseMessage(0,
                      Constants.MESSAGE_RESPONSE_CONNECTION_LIMIT_REACHED,
                      "The maximum number of simultaneous connections has " +
                      "been reached", -1);
            ASN1Writer writer = new ASN1Writer(clientSocket.getOutputStream());
            writer.writeElement(helloResp.encode());
            clientSocket.close();
          }
          else
          {
            String connectionID = getNewConnectionID();
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Assigned a monitor connection ID of " +
                                   connectionID);
            ResourceMonitorClientConnection clientConnection =
                 new ResourceMonitorClientConnection(slamdServer, this,
                                                     clientSocket,
                                                     connectionID);
            connectionHash.put(clientConnection.getClientIPAddress(),
                               clientConnection);
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Added monitor connection to the " +
                                   "connection hash");
            clientConnection.start();
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Started the monitor client thread");
            StatusRequestMessage statusRequest =
                 new StatusRequestMessage(clientConnection.getMessageID());
            clientConnection.sendMessage(statusRequest);
            slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                   "Sent a status request message to the " +
                                   "monitor client");
          }
        }
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception sending message to client:  " + e);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        e.printStackTrace();
      }
    }

    hasStopped = true;

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ResourceMonitorClientLister.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Leaving ResourceMonitorClientListener.run()");
  }



  /**
   * Retrieves the set of resource monitor client connections for processing the
   * specified job.
   *
   * @param  job  The job for which the connections are to be retrieved.
   *
   * @return  The appropriate set of monitor clients for the specified job, or
   *          <CODE>null</CODE> if the requested set of monitor clients are not
   *          available.
   */
  public ResourceMonitorClientConnection[] getMonitorClientConnections(Job job)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientLister." +
                           "getClientConnections()");

    ArrayList<ResourceMonitorClientConnection> clientList =
         new ArrayList<ResourceMonitorClientConnection>();
    String[] requestedClients = job.getResourceMonitorClients();

    synchronized (connectionHashMutex)
    {
      for (int i=0;
           ((requestedClients != null) && (i < requestedClients.length)); i++)
      {
        ResourceMonitorClientConnection client =
             connectionHash.get(requestedClients[i]);
        if (client == null)
        {
          return null;
        }

        clientList.add(client);
      }

      if (job.monitorClientsIfAvailable())
      {
        ClientConnection[] jobClients = job.getClientConnections();
        for (int i=0; i < jobClients.length; i++)
        {
          String clientIP = jobClients[i].getClientIPAddress();
          ResourceMonitorClientConnection client = connectionHash.get(clientIP);
          if (client != null)
          {
            if (! clientList.contains(client))
            {
              clientList.add(client);
            }
          }
        }
      }
    }

    ResourceMonitorClientConnection[] clients =
         new ResourceMonitorClientConnection[clientList.size()];
    clientList.toArray(clients);
    return clients;
  }



  /**
   * Retrieves a list of the resource monitor clients that are currently
   * connected to the SLAMD server.
   *
   * @return  The set of resource monitor clients that are currently connected.
   */
  public ResourceMonitorClientConnection[] getMonitorClientList()
  {
    synchronized (connectionHashMutex)
    {
      ResourceMonitorClientConnection[] conns =
           new ResourceMonitorClientConnection[connectionHash.size()];

      int i=0;
      Iterator iterator = connectionHash.values().iterator();
      while (iterator.hasNext())
      {
        conns[i++] = (ResourceMonitorClientConnection) iterator.next();
      }

      return conns;
    }
  }



  /**
   * Retrieves a list of the resource monitor clients that are currently
   * connected to the SLAMD server, sorted by client ID.
   *
   * @return  The set of resource monitor clients that are currently connected,
   *          sorted by client ID.
   */
  public ResourceMonitorClientConnection[] getSortedMonitorClientList()
  {
    synchronized (connectionHashMutex)
    {
      ResourceMonitorClientConnection[] conns =
           new ResourceMonitorClientConnection[connectionHash.size()];

      int i=0;
      Iterator iterator = connectionHash.values().iterator();
      while (iterator.hasNext())
      {
        conns[i++] = (ResourceMonitorClientConnection) iterator.next();
      }

      Arrays.sort(conns);
      return conns;
    }
  }



  /**
   * Retrieves a connection ID that can be used to uniquely identify each
   * client connection.
   *
   * @return  A connection ID that can be used to uniquely identify each client
   *          connection.
   */
  public String getNewConnectionID()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientLister." +
                           "getNewConnectionID()");

    // Cheat -- use the unique ID generator in the scheduler
    return slamdServer.getScheduler().generateUniqueID();
  }



  /**
   * Indicates whether this client listener requires clients to authenticate.
   *
   * @return  <CODE>true</CODE> if clients are required to authenticate, or
   *          <CODE>false</CODE> if they are not.
   */
  public boolean requireAuthentication()
  {
    return requireAuthentication;
  }



  /**
   * Sends a message to the specified client indicating that it should
   * disconnect itself from the SLAMD server.  If it is currently processing
   * jobs, then it will be given an opportunity to send its results to the SLAMD
   * server.
   *
   * @param  clientID  The client ID of the resource monitor client to
   *                   disconnect.
   *
   * @return  {@code true} if a disconnect request was sent to the resource
   *          monitor client, or {@code false} if not.
   */
  public boolean requestDisconnect(final String clientID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In ResourceMonitorClientLister.requestDisconnect(" + clientID +
              ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
         "Gracefully disconnecting client " + clientID);

    synchronized (connectionHashMutex)
    {
      for (final ResourceMonitorClientConnection c : connectionHash.values())
      {
        if (c.getClientID().equals(clientID))
        {
          c.sendServerShutdownMessage(false);
          return true;
        }
      }
    }

    return false;
  }



  /**
   * Sends a message to each client indicating that it should disconnect
   * itself from the SLAMD server.  If any clients are currently processing
   * jobs, then those clients will be given an opportunity to send their results
   * to the SLAMD server.
   */
  public void requestDisconnectAll()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In ResourceMonitorClientLister.forcefullyDisconnectAll()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
         "Gracefully disconnecting all client connections.");

    synchronized (connectionHashMutex)
    {
      Iterator iterator = connectionHash.values().iterator();
      while (iterator.hasNext())
      {
        ((ResourceMonitorClientConnection)
         iterator.next()).sendServerShutdownMessage(false);
      }
    }
  }



  /**
   * Forcefully closes the connection to the resource monitor client with the
   * specified client ID.  If it is currently processing jobs, then any data
   * for those jobs will be lost.
   *
   * @param  clientID  The client ID of the resource monitor client to
   *                   disconnect.
   *
   * @return  {@code true} if the client was disconnected, or {@code false} if
   *          not.
   */
  public boolean forceDisconnect(final String clientID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In ResourceMonitorClientLister.forcefullyDisconnect(" + clientID +
              ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
         "Forcefully disconnecting resource monitor client " + clientID);

    synchronized (connectionHashMutex)
    {
      for (final ResourceMonitorClientConnection c : connectionHash.values())
      {
        if (c.getClientID().equals(clientID))
        {
          c.sendServerShutdownMessage(true);
          connectionLostUnlocked(c);
          return true;
        }
      }
    }

    return false;
  }



  /**
   * Forcefully closes the connections for all resource monitor clients
   * connected to the SLAMD server.  If any clients are currently processing a
   * job, then no information will be available for that job from those clients.
   */
  public void forceDisconnectAll()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In ResourceMonitorClientLister.forcefullyDisconnectAll()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
         "Forcefully disconnecting all resource monitor client connections.");

    synchronized (connectionHashMutex)
    {
      ResourceMonitorClientConnection client;
      Iterator iterator = connectionHash.values().iterator();
      while (iterator.hasNext())
      {
        client = (ResourceMonitorClientConnection) iterator.next();
        client.sendServerShutdownMessage(true);
        connectionLostUnlocked(client);
      }

      connectionHash.clear();
    }
  }



  /**
   * Indicates that the specified connection is closing and all references to it
   * should be removed.
   *
   * @param  clientConnection  The connection that is shutting down.
   */
  public void connectionLost(ResourceMonitorClientConnection clientConnection)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientLister.connectionLost()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                           "Lost connection to resource monitor client " +
                           clientConnection.toString());

    synchronized (connectionHashMutex)
    {
      connectionHash.remove(clientConnection.getClientIPAddress());
    }

    Job[] jobsInProgress = clientConnection.getJobsInProgress();
    for (int i=0; i < jobsInProgress.length; i++)
    {
      String[] messages = new String[]
      {
        "The job was cancelled on resource monitor client " +
          clientConnection.getClientID() +
        " because the connection to the client was lost."
      };

      JobCompletedMessage completedMessage = new
           JobCompletedMessage(clientConnection.getMessageID(),
                               jobsInProgress[i].getJobID(),
                               Constants.JOB_STATE_STOPPED_DUE_TO_ERROR,
                               jobsInProgress[i].getActualStartTime(),
                               new Date(), -1, new StatTracker[0], messages);

      jobsInProgress[i].resourceClientDone(clientConnection, completedMessage);
    }
  }



  /**
   * Indicates that the specified connection is closing and all references to it
   * should be removed.  This method does not require a lock on the connection
   * hash and therefore requires that the caller already hold it.
   *
   * @param  clientConnection  The connection that is shutting down.
   */
  private void connectionLostUnlocked(ResourceMonitorClientConnection
                                      clientConnection)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ResourceMonitorClientLister.connectionLost()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                           "Lost connection to resource monitor client " +
                           clientConnection.toString());

    connectionHash.remove(clientConnection.getClientIPAddress());

    Job[] jobsInProgress = clientConnection.getJobsInProgress();
    for (int i=0; i < jobsInProgress.length; i++)
    {
      String[] messages = new String[]
      {
        "The job was cancelled on resource monitor client " +
        clientConnection.getClientID() +
        " because the connection to the client was lost."
      };

      JobCompletedMessage completedMessage = new
           JobCompletedMessage(clientConnection.getMessageID(),
                               jobsInProgress[i].getJobID(),
                               Constants.JOB_STATE_STOPPED_DUE_TO_ERROR,
                               jobsInProgress[i].getActualStartTime(),
                               new Date(), -1, new StatTracker[0], messages);

      jobsInProgress[i].resourceClientDone(clientConnection,
                                           completedMessage);
    }
  }



  /**
   * Indicates whether all the resource monitor clients needed for the specified
   * job are currently available.
   *
   * @param  job  The job that specifies which monitor clients are needed.
   *
   * @return  <CODE>true</CODE> if all requested monitor clients are available,
   *          or <CODE>false</CODE> if not.
   */
  public boolean connectionsAvailable(Job job)
  {
    String[] requestedClients = job.getResourceMonitorClients();
    if ((requestedClients == null) || (requestedClients.length == 0))
    {
      return true;
    }

    synchronized (connectionHashMutex)
    {
      for (int i=0; i < requestedClients.length; i++)
      {
        if (connectionHash.get(requestedClients[i]) == null)
        {
          return false;
        }
      }
    }

    return true;
  }



  /**
   * Retrieves the length of time that should pass between keepalive messages.
   * A keepalive message will be sent if there has been no interaction with the
   * client for the specified period of time.
   *
   * @return  The length of time that should pass between keepalive messages.
   */
  public int getKeepAliveInterval()
  {
    return keepaliveInterval;
  }



  /**
   * Retrieves the maximum amount of time in seconds that a client connection
   * should wait for a response to a solicited message before returning an
   * error.
   *
   * @return  The maximum amount of time in seconds that a client connection
   *          should wait for a response to a solicited message before returning
   *          an error.
   */
  public int getMaxResponseWaitTime()
  {
    return maxResponseWaitTime;
  }



  /**
   * Retrieves the name that the client listener uses to subscribe to the
   * configuration handler in order to be notified of configuration changes.
   *
   * @return  The name that the client listener uses to subscribe to the
   *          configuration handler in order to be notified of configuration
   *          changes.
   */
  public String getSubscriberName()
  {
    return "Resource Monitor Client Listener";
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
                           "In ResourceMonitorClientListener.getParameters()");

    IntegerParameter portParameter =
         new IntegerParameter(Constants.PARAM_MONITOR_LISTENER_PORT,
                              "Resource Monitor Client Listener Port",
                              "The port on which the SLAMD server listens " +
                              "for connections from resource monitor clients.",
                              true,
                              listenPort, true, 1, true, 65535);


    Parameter[] params = new Parameter[]
    {
      portParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the resource client
   * listener.  In this case, the only configuration parameter for this listener
   * is the port number, and that cannot change without restarting the SLAMD
   * server.  Therefore, this method does nothing.
   */
  public void refreshSubscriberConfiguration()
  {
    // No implementation required.
  }



  /**
   * Re-reads the configuration for the specified parameter if the parameter is
   * applicable to the resource monitor client listener.  In this case, the only
   * configuration parameter for this listener is the port number, and that
   * cannot change without restarting the SLAMD server.  Therefore, this method
   * does nothing.
   *
   * @param  parameterName  The name of the parameter for which to reread the
   *                        configuration.
   */
  public void refreshSubscriberConfiguration(String parameterName)
  {
    // No implementation required.
  }
}

