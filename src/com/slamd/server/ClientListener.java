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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.net.ssl.SSLServerSocketFactory;

import com.slamd.admin.AccessManager;
import com.slamd.admin.AdminAccess;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.RefCountMutex;
import com.slamd.common.SLAMDException;
import com.slamd.db.SLAMDDB;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.JobCompletedMessage;
import com.slamd.message.StatusRequestMessage;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class implements the client listener that the SLAMD server uses to
 * listen for connections from clients.  If fewer than the maximum connections
 * are established, then the connection will be accepted and a new connection
 * thread will be spawned to handle operations on that connection.  If the
 * maximum number of connections are already in use, then the new connection
 * will be rejected.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientListener
       extends Thread
       implements ConfigSubscriber
{
  // The set of connections that are available for use in job processing
  private ArrayList<ClientConnection> availableConnections;

  // The list of client connections that are currently established
  private ArrayList<ClientConnection> connectionList;

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
  protected int listenPort;

  // The maximum number of simultaneous clients that may be connected to the
  // server at once.
  private int maxClients;

  // The maximum length of time in seconds that the client connection will wait
  // for a response from a solicited message
  private int maxResponseWaitTime;

  // A mutex used to protect multithreaded access to the connection list
  private RefCountMutex connectionListMutex;

  // The server socket used to listen for new connections
  private ServerSocket serverSocket;

  // The configuration database associated with the SLAMD server
  private SLAMDDB configDB;

  // The SLAMD server with which this client listener is associated
  private SLAMDServer slamdServer;



  /**
   * Creates a new listener to accept client connections.
   *
   * @param  slamdServer  The SLAMD server with which this listener is
   *                      associated.
   */
  public ClientListener(SLAMDServer slamdServer)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Entering ClientListener constructor");

    setName("Client Listener");

    // Initialize all the instance variables
    this.slamdServer     = slamdServer;
    maxClients           = Constants.NO_MAX_CLIENTS;
    availableConnections = new ArrayList<ClientConnection>();
    connectionList       = new ArrayList<ClientConnection>();
    connectionListMutex  = new RefCountMutex();
    keepListening        = true;
    hasStopped           = true;


    // Read the appropriate information from the configuration
    configDB = slamdServer.getConfigDB();
    configDB.registerAsSubscriber(this);

    String maxClientsStr =
         configDB.getConfigParameter(Constants.PARAM_LISTENER_MAX_CLIENTS);
    if ((maxClientsStr != null) && (maxClientsStr.length() > 0))
    {
      try
      {
        maxClients = Integer.parseInt(maxClientsStr);
      } catch (NumberFormatException nfe) {}
    }

    keepaliveInterval = Constants.DEFAULT_LISTENER_KEEPALIVE_INTERVAL;
    String keepaliveStr =
         configDB.getConfigParameter(
              Constants.PARAM_LISTENER_KEEPALIVE_INTERVAL);
    if ((keepaliveStr != null) && (keepaliveStr.length() != 0))
    {
      try
      {
        keepaliveInterval = Integer.parseInt(keepaliveStr);
      } catch (NumberFormatException nfe) {}
    }

    listenPort = Constants.DEFAULT_LISTENER_PORT_NUMBER;
    String portStr =
         configDB.getConfigParameter(Constants.PARAM_LISTENER_PORT);
    if ((portStr != null) && (portStr.length() != 0))
    {
      try
      {
        listenPort = Integer.parseInt(portStr);
      } catch (NumberFormatException nfe) {}
    }

    maxResponseWaitTime = Constants.DEFAULT_MAX_RESPONSE_WAIT_TIME;
    String waitStr =
         configDB.getConfigParameter(Constants.PARAM_MAX_RESPONSE_WAIT_TIME);
    if ((waitStr != null) && (waitStr.length() > 0))
    {
      try
      {
        maxResponseWaitTime = Integer.parseInt(waitStr);
      } catch (NumberFormatException nfe) {}
    }

    requireAuthentication = Constants.DEFAULT_REQUIRE_AUTHENTICATION;
    String requireAuthStr =
         configDB.getConfigParameter(Constants.PARAM_REQUIRE_AUTHENTICATION);
    if ((requireAuthStr != null) && (requireAuthStr.length() > 0))
    {
      requireAuthentication =
           requireAuthStr.equals(Constants.CONFIG_VALUE_TRUE);
      if (requireAuthentication)
      {
        AccessManager accessManager = AdminAccess.getAccessManager();
        if (accessManager == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ADMIN,
                                 "Could not enable client authentication -- " +
                                 "access control is not enabled in the " +
                                 "admin interface.");
          requireAuthentication = false;
        }
      }
    }

    useSSL = Constants.DEFAULT_LISTENER_USE_SSL;
    String sslStr =
         configDB.getConfigParameter(Constants.PARAM_LISTENER_USE_SSL);
    if ((sslStr != null) && (sslStr.length() > 0))
    {
      useSSL = sslStr.equals(Constants.CONFIG_VALUE_TRUE);
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
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ClientListener constructor");
  }



  /**
   * Indicates that the listener should start listening for client connections.
   */
  public void startListening()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientListener.startListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientListener.startListening()");
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
                           "In ClientListener.stopListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientListener.stopListening()");
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
    connectionListMutex.getWriteLock();
    for (int i=0; i < connectionList.size(); i++)
    {
      ClientConnection clientConnection = connectionList.get(i);
      clientConnection.sendServerShutdownMessage(true);
    }
    connectionList.clear();
    availableConnections.clear();
    connectionListMutex.releaseWriteLock();
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
                           "In ClientLister.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientListener.run()");

    hasStopped = false;

    if (useSSL)
    {
      try
      {
        SSLServerSocketFactory socketFactory =
             (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = socketFactory.createServerSocket(listenPort);
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Listening for SSL-based client connections " +
                               "on port " + listenPort);
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
                               "Listening for client connections on port " +
                               listenPort);
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
                               "New client connection received from " +
                               clientSocket.getInetAddress().toString());
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception accepting client connection:  " +
                               ioe);
        continue;
      }

      try
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Going to get a write lock on the " +
                               "connection list.");
        try
        {
          connectionListMutex.getWriteLock(5000);
        }
        catch (InterruptedException ie)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(ie));
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                                 "Unable to obtain write lock on connection " +
                                 "list.  Refusing client connection.");
          HelloResponseMessage helloResp =
               new HelloResponseMessage(0,
                    Constants.MESSAGE_RESPONSE_SERVER_ERROR,
                    "Unable to obtain a write lock on the connection list.",
                    -1);
          ASN1Writer writer = new ASN1Writer(clientSocket.getOutputStream());
          writer.writeElement(helloResp.encode());
          clientSocket.close();
          continue;
        }
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Acquired the write lock.");
        if ((maxClients > 0) && (connectionList.size() >= maxClients))
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                                 "Refusing connection from " +
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
                                 "Assigned a connection ID of " + connectionID);
          ClientConnection clientConnection =
               new ClientConnection(slamdServer, this, clientSocket,
                                    connectionID);
          availableConnections.add(clientConnection);
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Added connection to the list of available " +
                                 "connections");
          connectionList.add(clientConnection);
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Added connection to the list of " +
                                 "established connections");
          clientConnection.start();
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Started the client thread");
          StatusRequestMessage statusRequest =
               new StatusRequestMessage(clientConnection.getMessageID());
          clientConnection.sendMessage(statusRequest);
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Sent a status request message to the client");
        }

        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Releasing the write lock on the client list.");
        connectionListMutex.releaseWriteLock();
      }
      catch (IOException ioe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Releasing the write lock on the client list " +
                               " -- I/O exception");
        connectionListMutex.releaseWriteLock();
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception sending message to client:  " +
                               ioe);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        ioe.printStackTrace();
      }
      catch (SLAMDException se)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Releasing the write lock on the client list " +
                               " -- Client hello exception");
        connectionListMutex.releaseWriteLock();
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Exception processing the client hello:  " +
                               se);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(se));
        se.printStackTrace();
      }
    }

    hasStopped = true;

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving ClientLister.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Leaving ClientListener.run()");
  }



  /**
   * Indicates whether the client listener currently has connections available
   * that satisfy the requirements for the indicated job.
   *
   * @param  job  The job for which to make the determination.
   *
   * @return  <CODE>true</CODE> if this client listener has an appropriate set
   *          of clients available, or <CODE>false</CODE> if not.
   */
  public boolean connectionsAvailable(Job job)
  {
    int clientsNeeded = job.getNumberOfClients();
    if (clientsNeeded > availableConnections.size())
    {
      return false;
    }

    connectionListMutex.getReadLock();

    // Create a clone of the available connection list.  This is necessary
    // because it is possible for the same client address to appear multiple
    // times in the list of requested clients, and we don't want the same
    // client connection to be counted multiple times.
    ArrayList<ClientConnection> availableClone =
         new ArrayList<ClientConnection>(availableConnections.size());
    for (int i=0; i < availableConnections.size(); i++)
    {
      availableClone.add(availableConnections.get(i));
    }

    String[] requestedClients = job.getRequestedClients();
    if (requestedClients != null)
    {
      for (int i=0; i < requestedClients.length; i++)
      {
        boolean matchFound = false;
        for (int j=0; j < availableClone.size(); j++)
        {
          ClientConnection connection = availableClone.get(j);
          if (connection.getClientIPAddress().equals(requestedClients[i]))
          {
            matchFound = true;
            availableClone.remove(j);
            clientsNeeded--;
            break;
          }
        }

        if (! matchFound)
        {
          connectionListMutex.releaseReadLock();
          return false;
        }
      }
    }

    int lookPos = 0;
    while ((clientsNeeded > 0) && (lookPos < availableClone.size()))
    {
      ClientConnection connection = availableClone.get(lookPos);
      if (connection.restrictedMode)
      {
        lookPos++;
      }
      else
      {
        availableClone.remove(lookPos);
        clientsNeeded--;
      }
    }

    connectionListMutex.releaseReadLock();
    return (clientsNeeded == 0);
  }



  /**
   * Retrieves the set of client connections for processing the specified job.
   *
   * @param  job  The job for which the connections are to be retrieved.
   *
   * @return  The appropriate set of clients for the specified job, or
   *          <CODE>null</CODE> if there is not an appropriate set of
   *          connections available.
   */
  public ClientConnection[] getClientConnections(Job job)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientLister.getClientConnections()");

    ArrayList<ClientConnection> jobConnections =
         new ArrayList<ClientConnection>();
    int numConnections = job.getNumberOfClients();

    if (availableConnections.size() < numConnections)
    {
      return null;
    }

    connectionListMutex.getWriteLock();
    String[] requestedClients = job.getRequestedClients();
    if (requestedClients != null)
    {
      for (int i=0; i < requestedClients.length; i++)
      {
        boolean matchFound = false;

        for (int j=0; j < availableConnections.size(); j++)
        {
          ClientConnection conn = availableConnections.get(j);
          if (conn.getClientIPAddress().equals(requestedClients[i]))
          {
            jobConnections.add(conn);
            availableConnections.remove(j);
            matchFound = true;
            break;
          }
        }

        if (! matchFound)
        {
          // The requested client could not be found.  Return the connections
          // allocated so far back to the set of available connections and
          // return null.
          for (int j=0; j < jobConnections.size(); j++)
          {
            availableConnections.add(jobConnections.get(j));
          }
          connectionListMutex.releaseWriteLock();
          return null;
        }
      }
    }

    // If we still don't have enough clients allocated for the job (i.e., either
    // no specific clients were requested, or there were not enough specific
    // clients requested) then try to add the appropriate number of remaining
    // clients, balancing across the set of client addresses.
    if (jobConnections.size() < numConnections)
    {
      int numAvailable = 0;
      HashMap<String,LinkedList<ClientConnection>> connHash =
           new HashMap<String,LinkedList<ClientConnection>>();
      for (int i=0; i < availableConnections.size(); i++)
      {
        ClientConnection connection = availableConnections.get(i);
        if (connection.restrictedMode())
        {
          continue;
        }

        String     clientIP   = connection.getClientIPAddress();
        LinkedList<ClientConnection> clientList = connHash.get(clientIP);
        if (clientList == null)
        {
          clientList = new LinkedList<ClientConnection>();
        }

        clientList.add(connection);
        connHash.put(clientIP, clientList);
        numAvailable++;
      }

      if ((jobConnections.size() + numAvailable) >= numConnections)
      {
        Iterator iterator = connHash.keySet().iterator();
        while (jobConnections.size() < numConnections)
        {
          String clientIP;
          if (! iterator.hasNext())
          {
            iterator = connHash.keySet().iterator();
          }

          clientIP = (String) iterator.next();
          LinkedList<ClientConnection> clientList = connHash.get(clientIP);

          ClientConnection connection = clientList.removeFirst();
          jobConnections.add(connection);
          availableConnections.remove(connection);

          if (clientList.isEmpty())
          {
            iterator.remove();
            connHash.remove(clientIP);
          }
        }
      }
    }

    // If we don't have enough clients available (e.g., some of them were in
    // restricted mode), then return them all back to the pool and return null.
    if (jobConnections.size() < numConnections)
    {
      for (int j=0; j < jobConnections.size(); j++)
      {
        availableConnections.add(jobConnections.get(j));
      }
      connectionListMutex.releaseWriteLock();
      return null;
    }

    // Return the set of allocated jobs.
    connectionListMutex.releaseWriteLock();
    ClientConnection[] conns = new ClientConnection[jobConnections.size()];
    jobConnections.toArray(conns);
    return conns;
  }



  /**
   * Retrieves the set of connections that are currently established.  This is
   * only for use for status info because not all of the connections may be
   * available for processing a new job.
   *
   * @return  The set of connections that are currently established.
   */
  public ClientConnection[] getConnectionList()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientLister.getConnectionList()");

    connectionListMutex.getReadLock();
    ClientConnection[] conns = new ClientConnection[connectionList.size()];
    connectionList.toArray(conns);
    connectionListMutex.releaseReadLock();
    return conns;
  }



  /**
   * Retrieves the set of connections that are currently established, sorted by
   * client ID.  This is only for use for status info because not all of the
   * connections may be available for processing a new job.
   *
   * @return  The set of connections that are currently established.
   */
  public ClientConnection[] getSortedConnectionList()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientLister.getConnectionList()");

    connectionListMutex.getReadLock();
    ClientConnection[] conns = new ClientConnection[connectionList.size()];
    connectionList.toArray(conns);
    connectionListMutex.releaseReadLock();

    Arrays.sort(conns);
    return conns;
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
                           "In ClientLister.getNewConnectionID()");

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
   * disconnect from the SLAMD server.  If it is currently processing a job,
   * then the client will be given an opportunity to send its results to the
   * SLAMD server.
   *
   * @param  clientID  The client ID of the client to which the request is to be
   *                   sent.
   *
   * @return  <CODE>true</CODE> if a request was sent to the specified client,
   *          or <CODE>false</CODE> if not.
   */
  public boolean requestDisconnect(String clientID)
  {
    ClientConnection clientToDisconnect = null;

    connectionListMutex.getWriteLock();
    for (int i=0; i < connectionList.size(); i++)
    {
      ClientConnection clientConnection = connectionList.get(i);
      if (clientConnection.getClientID().equals(clientID))
      {
        clientToDisconnect = clientConnection;
        break;
      }
    }
    connectionListMutex.releaseWriteLock();

    if (clientToDisconnect == null)
    {
      return false;
    }

    clientToDisconnect.sendServerShutdownMessage(false);
    availableConnections.remove(clientToDisconnect);
    return true;
  }



  /**
   * Sends a message to each client indicating that it should disconnect
   * itself from the SLAMD server.  If any clients are currently processing
   * jobs, then those clients will be given an opportunity to send their results
   * to the SLAMD server.
   */
  public void requestDisconnectAll()
  {
    connectionListMutex.getWriteLock();
    availableConnections.clear();

    ClientConnection[] connectionArray =
         new ClientConnection[connectionList.size()];
    connectionList.toArray(connectionArray);
    connectionListMutex.releaseWriteLock();

    for (int i=0; i < connectionArray.length; i++)
    {
      connectionArray[i].sendServerShutdownMessage(false);
    }
  }



  /**
   * Forcefully closes the connection to the specified client and removes all
   * references to it from the SLAMD server.  If it is currently processing a
   * job, then the job will be notified that the client has disconnected and
   * should not expect any results from that client.
   *
   * @param  clientID  The client ID of the client to be disconnected.
   *
   * @return  <CODE>true</CODE> if the client was disconnected, or
   *          <CODE>false</CODE> if not.
   */
  public boolean forceDisconnect(String clientID)
  {
    ClientConnection clientToDisconnect = null;

    connectionListMutex.getWriteLock();
    for (int i=0; i < connectionList.size(); i++)
    {
      ClientConnection clientConnection = connectionList.get(i);
      if (clientConnection.getClientID().equals(clientID))
      {
        clientToDisconnect = clientConnection;
        break;
      }
    }
    connectionListMutex.releaseWriteLock();

    if (clientToDisconnect == null)
    {
      return false;
    }

    try
    {
      clientToDisconnect.socket.close();
    } catch (Exception e) {}

    return true;
  }



  /**
   * Forcefully closes the connections for all clients connected to the SLAMD
   * server.  If any clients are currently processing a job, then no information
   * will be available for that job from those clients.
   */
  public void forcefullyDisconnectAll()
  {
    connectionListMutex.getWriteLock();
    availableConnections.clear();

    ClientConnection[] connectionArray =
         new ClientConnection[connectionList.size()];
    connectionList.toArray(connectionArray);
    connectionList.clear();

    connectionListMutex.releaseWriteLock();

    for (int i=0; i < connectionArray.length; i++)
    {
      try
      {
        connectionArray[i].socket.close();
      } catch (Exception e) {}

      connectionLost(connectionArray[i]);
    }
  }



  /**
   * Indicates that the specified connection is closing and all references to it
   * should be removed.
   *
   * @param  clientConnection  The connection that is shutting down.
   */
  public void connectionLost(ClientConnection clientConnection)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientLister.connectionLost()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                           "Lost connection to client " +
                           clientConnection.toString());

    connectionListMutex.getWriteLock();

    // First, iterate through the list of connections
    for (int i=0; i < connectionList.size(); i++)
    {
      ClientConnection conn = connectionList.get(i);
      if (conn.getConnectionID().equals(clientConnection.getConnectionID()))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Removing conn " + clientConnection.toString() +
                               " from connection list");
        connectionList.remove(i);
        break;
      }
    }

    // Next, iterate through the list of available connections
    for (int i=0; i < availableConnections.size(); i++)
    {
      ClientConnection conn = availableConnections.get(i);
      if (conn.getConnectionID().equals(clientConnection.getConnectionID()))
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Removing conn " + clientConnection.toString() +
                               " from available connections");
        availableConnections.remove(i);
        break;
      }
    }

    connectionListMutex.releaseWriteLock();

    // If there was a job in progress, it needs to be updated to indicate this
    // client won't be providing any results
    if (clientConnection.getJob() != null)
    {
      Job job = clientConnection.getJob();
      String[] messages = new String[]
      {
        "The job was cancelled on client " + clientConnection.getClientID() +
        " because the connection to the client was lost."
      };

      JobCompletedMessage completedMessage = new
           JobCompletedMessage(clientConnection.getMessageID(), job.getJobID(),
                               Constants.JOB_STATE_STOPPED_DUE_TO_ERROR,
                               job.getActualStartTime(), new Date(), -1,
                               new StatTracker[0], messages);

      clientConnection.getJob().clientDone(clientConnection, completedMessage);
    }

    // See if this client was associated with a client manager and if so, update
    // it.
    slamdServer.getClientManagerListener().clientConnectionLost(
                                                clientConnection);
  }



  /**
   * Indicates that the specified client connection is available for processing
   * new jobs.  This will be called by the client connection when the client
   * indicates that it has finished processing a job that had been assigned to
   * it.
   *
   * @param  clientConnection  The connection that is announcing its
   *                           availability.
   */
  public void setAvailableForProcessing(ClientConnection clientConnection)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientLister.setAvailableForProcessing()");

    connectionListMutex.getWriteLock();

    // First, make sure that the connection is not already on the available
    // list.  This shouldn't happen, but if it does then it could cause a busy
    // client to be assigned more work (which it will reject).
    for (int i=0; i < availableConnections.size(); i++)
    {
      ClientConnection conn = availableConnections.get(i);
      if (conn.getClientID().equals(clientConnection.getClientID()))
      {
        connectionListMutex.releaseWriteLock();
        return;
      }
    }


    // Add this connection to the list of available connections
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "Adding conn " + clientConnection.toString() +
                           " to available connection list");
    availableConnections.add(clientConnection);

    connectionListMutex.releaseWriteLock();
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
   * Retrieves the maximum number of concurrent client connections that should
   * be allowed for this listener.
   *
   * @return  The maximum number of concurrent connections that should be
   *          allowed for this listener.
   */
  public int getMaxClients()
  {
    return maxClients;
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
   * Indicates whether this client listener is configured to use SSL.
   *
   * @return  <CODE>true</CODE> if this client listener is configured to use
   *          SSL, or <CODE>false</CODE> if it is not.
   */
  public boolean useSSL()
  {
    return useSSL;
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
    return "SLAMD Client Listener";
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
                           "In ClientListener.getParameters()");

    BooleanParameter requireAuthParameter =
         new BooleanParameter(Constants.PARAM_REQUIRE_AUTHENTICATION,
                              "Require Authentication",
                              "Indicates whether the client listener will " +
                              "require clients to authenticate.",
                              requireAuthentication);
    BooleanParameter useSSLParameter =
         new BooleanParameter(Constants.PARAM_LISTENER_USE_SSL,
                              "Use SSL",
                              "Indicates whether the client listener will " +
                              "use SSL to encrypt communication between the " +
                              "clients and the SLAMD server.", useSSL);
    IntegerParameter portParameter =
         new IntegerParameter(Constants.PARAM_LISTENER_PORT,
                              "Client Listener Port",
                              "The port on which the SLAMD server listens " +
                              "for client connections.",
                              true,
                              listenPort, true, 1, true, 65535);
    IntegerParameter keepaliveParameter =
         new IntegerParameter(Constants.PARAM_LISTENER_KEEPALIVE_INTERVAL,
                              "Keepalive Interval",
                              "The length of time (in seconds) that will " +
                              "pass between keepalive messages.  A value of " +
                              "0 will disable keepalive messages.", true,
                              keepaliveInterval, true, 0, false, 0);
    IntegerParameter maxClientsParameter =
         new IntegerParameter(Constants.PARAM_LISTENER_MAX_CLIENTS,
                              "Maximum Number of Clients",
                              "The maximum number of clients that may be " +
                              "connected to the SLAMD server at any time.  " +
                              "A value of 0 indicates that there will be no " +
                              "limit.", true, maxClients, true, 0, false, 0);
    IntegerParameter maxWaitTimeParameter =
         new IntegerParameter(Constants.PARAM_MAX_RESPONSE_WAIT_TIME,
                              "Maximum Wait Time",
                              "The maximum length of time (in seconds) that " +
                              "the SLAMD server will wait for a response " +
                              "to a request issued to a client.", true,
                              maxResponseWaitTime, true, 0, false, 0);


    Parameter[] params = new Parameter[]
    {
      portParameter,
      keepaliveParameter,
      maxClientsParameter,
      maxWaitTimeParameter,
      useSSLParameter,
      requireAuthParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the client listener.  In
   * this case, the only options are the keepalive interval, the maximum number
   * of clients, and the maximum response wait time.  The listen port is not
   * dynamically reconfigurable.  Additionally, changes made to the keepalive
   * interval, maximum number of connections, and maximum wait time will only
   * apply to new connections established after this point.  Existing
   * connections will remain unchanged.
   *
   * @throws  SLAMDServerException  If there is a problem reading or applying
   *                                the changes.
   */
  public void refreshSubscriberConfiguration()
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientListener.refreshConfiguration()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientListener.refreshConfiguration()");

    // Get the keepalive interval.  Note that this will only apply to new client
    // connections -- existing client connections will not be impacted
    String keepAliveStr = configDB.getConfigParameter(
                               Constants.PARAM_LISTENER_KEEPALIVE_INTERVAL);
    if ((keepAliveStr != null) && (keepAliveStr.length() > 0))
    {
      try
      {
        keepaliveInterval = Integer.parseInt(keepAliveStr);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Set keepaliveInterval to " + keepaliveInterval);
      }
      catch (NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Keepalive Interval String " + keepAliveStr +
                               " not an integer");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
      }
    }
    else
    {
      keepaliveInterval = Constants.NO_KEEPALIVE_INTERVAL;
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Keepalive Messages Disabled");
    }


    // Get the maximum number of client connections.  This will only apply to
    // new client connections.
    String maxConnStr =
         configDB.getConfigParameter(Constants.PARAM_LISTENER_MAX_CLIENTS);
    if ((maxConnStr != null) && (maxConnStr.length() > 0))
    {
      try
      {
        maxClients = Integer.parseInt(maxConnStr);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Set max clients to " + maxClients);
      }
      catch (NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Max Clients String " + maxConnStr +
                               " not an integer");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
      }
    }
    else
    {
      maxClients = Constants.NO_MAX_CLIENTS;
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Now allowing unlimited client connections");
    }


    // Get the maximum response wait time, also applicable only to new
    // connections.
    String waitTimeStr =
         configDB.getConfigParameter(Constants.PARAM_MAX_RESPONSE_WAIT_TIME);
    if ((waitTimeStr != null) && (waitTimeStr.length() > 0))
    {
      try
      {
        maxResponseWaitTime = Integer.parseInt(waitTimeStr);
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Set max response wait time to " +
                               maxResponseWaitTime);
      }
      catch (NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Max Wait Time String " + waitTimeStr +
                               " not an integer");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
      }
    }
    else
    {
      maxResponseWaitTime = Constants.DEFAULT_MAX_RESPONSE_WAIT_TIME;
      slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                             "Set max response wait time to default");
    }


    // Determine whether the client listener will require clients to
    // authenticate.
    String requireAuthStr =
         configDB.getConfigParameter(Constants.PARAM_REQUIRE_AUTHENTICATION);
    if ((requireAuthStr != null) && (requireAuthStr.length() > 0))
    {
      requireAuthentication =
           requireAuthStr.equals(Constants.CONFIG_VALUE_TRUE);
      if (requireAuthentication)
      {
        AccessManager accessManager = AdminAccess.getAccessManager();
        if (accessManager == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ADMIN,
                                 "Could not enable client authentication -- " +
                                 "access control is not enabled in the " +
                                 "admin interface.");
          requireAuthentication = false;
        }
      }
    }


    // Determine whether the client listener will require clients to
    // authenticate.
    String sslStr =
         configDB.getConfigParameter(Constants.PARAM_LISTENER_USE_SSL);
    if ((sslStr != null) && (sslStr.length() > 0))
    {
      useSSL = sslStr.equals(Constants.CONFIG_VALUE_TRUE);
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
    }
  }



  /**
   * Re-reads the configuration for the specified parameter if the parameter is
   * applicable to the client listener.  In this case, only the keepalive
   * interval, maximum number of client connections, and maximum response wait
   * time are reconfigurable.
   *
   * @param  parameterName  The name of the parameter for which to reread the
   *                        configuration.
   *
   * @throws  SLAMDServerException  If there is a problem reading or applying
   *                                the specified change.
   */
  public void refreshSubscriberConfiguration(String parameterName)
       throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientListener.refreshConfiguration(" +
                           parameterName + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientListener.refreshConfiguration(" +
                           parameterName + ')');

    if (parameterName.equalsIgnoreCase(
             Constants.PARAM_LISTENER_KEEPALIVE_INTERVAL))
    {
      String keepAliveStr = configDB.getConfigParameter(
                                 Constants.PARAM_LISTENER_KEEPALIVE_INTERVAL);
      if ((keepAliveStr != null) && (keepAliveStr.length() > 0))
      {
        try
        {
          keepaliveInterval = Integer.parseInt(keepAliveStr);
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Set keepaliveInterval to " +
                                 keepaliveInterval);
        }
        catch (NumberFormatException nfe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Keepalive Interval String " + keepAliveStr +
                                 " not an integer");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        keepaliveInterval = Constants.NO_KEEPALIVE_INTERVAL;
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Keepalive Messages Disabled");
      }
    }
    else if (parameterName.equalsIgnoreCase(
                  Constants.PARAM_LISTENER_MAX_CLIENTS))
    {
      String maxConnStr =
           configDB.getConfigParameter(Constants.PARAM_LISTENER_MAX_CLIENTS);
      if ((maxConnStr != null) && (maxConnStr.length() > 0))
      {
        try
        {
          maxClients = Integer.parseInt(maxConnStr);
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Set max clients to " + maxClients);
        }
        catch (NumberFormatException nfe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Max Clients String " + maxConnStr +
                                 " not an integer");
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        maxClients = Constants.NO_MAX_CLIENTS;
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Now allowing unlimited client connections");
      }
    }
    else if (parameterName.equalsIgnoreCase(
                  Constants.PARAM_MAX_RESPONSE_WAIT_TIME))
    {
      String waitTimeStr =
           configDB.getConfigParameter(Constants.PARAM_MAX_RESPONSE_WAIT_TIME);
      if ((waitTimeStr != null) && (waitTimeStr.length() > 0))
      {
        try
        {
          maxResponseWaitTime = Integer.parseInt(waitTimeStr);
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Set max response wait time to " +
                                 maxResponseWaitTime);
        }
        catch (NumberFormatException nfe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                                 "Max Wait Time String " + waitTimeStr +
                                 " not an integer");
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        maxResponseWaitTime = Constants.DEFAULT_MAX_RESPONSE_WAIT_TIME;
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                               "Set max response wait time to default");
      }
    }
    else if (parameterName.equalsIgnoreCase(
                                Constants.PARAM_REQUIRE_AUTHENTICATION))
    {
      // Determine whether the client listener will require clients to
      // authenticate.
      String requireAuthStr =
           configDB.getConfigParameter(Constants.PARAM_REQUIRE_AUTHENTICATION);
      if ((requireAuthStr != null) && (requireAuthStr.length() > 0))
      {
        requireAuthentication =
             requireAuthStr.equals(Constants.CONFIG_VALUE_TRUE);
        if (requireAuthentication)
        {
          AccessManager accessManager = AdminAccess.getAccessManager();
          if (accessManager == null)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_ADMIN,
                                   "Could not enable client authentication " +
                                   "-- access control is not enabled in the " +
                                   "admin interface.");
            requireAuthentication = false;
          }
        }
      }
    }
    else if (parameterName.equalsIgnoreCase(Constants.PARAM_LISTENER_USE_SSL))
    {
      String sslStr =
           configDB.getConfigParameter(Constants.PARAM_LISTENER_USE_SSL);
      if ((sslStr != null) && (sslStr.length() > 0))
      {
        useSSL = sslStr.equals(Constants.CONFIG_VALUE_TRUE);
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
      }
    }
  }
}

