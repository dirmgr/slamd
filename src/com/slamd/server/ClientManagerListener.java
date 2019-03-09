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



import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import com.slamd.common.*;
import com.slamd.db.*;
import com.slamd.job.*;
import com.slamd.parameter.*;



/**
 * This class defines a thread that will listen for connections from client
 * managers.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientManagerListener
       extends Thread
       implements ConfigSubscriber
{
  // Indicates whether the listener has actually stopped listening.
  boolean hasStopped;

  // Indicates whether the listener should keep listening.
  boolean keepListening;

  // Indicates whether the listener should use SSL.
  boolean useSSL;

  // The port number on which this listener should listen.
  int listenPort;

  // The maximum length of time in seconds that the SLAMD server will wait for a
  // response for a request sent to a client manager.
  int maxResponseWaitTime;

  // The mutex used to provide threadsafe access to the list of client managers.
  final Object clientManagerMutex;

  // The server socket used to accept new connections from client managers.
  ServerSocket serverSocket;

  // The config database for the SLAMD server.
  SLAMDDB configDB;

  // The SLAMD server with which this listener is associated.
  SLAMDServer slamdServer;

  // The set of client manager connections that are currently established.
  ArrayList<ClientManagerConnection> clientManagers;



  /**
   * Creates a new listener that will listen for connections from client
   * managers.
   *
   * @param  slamdServer  The SLAMD server with which this client manager
   *                      listener is associated.
   */
  public ClientManagerListener(SLAMDServer slamdServer)
  {
    setName("Client Manager Listener");

    this.slamdServer   = slamdServer;

    // Read the appropriate information from the configuration
    configDB= slamdServer.getConfigDB();
    configDB.registerAsSubscriber(this);

    listenPort = Constants.DEFAULT_CLIENT_MANAGER_LISTENER_PORT_NUMBER;
    String listenPortStr = configDB.getConfigParameter(
                                Constants.PARAM_CLIENT_MANAGER_LISTENER_PORT);
    if (listenPortStr != null)
    {
      try
      {
        listenPort = Integer.parseInt(listenPortStr);
      } catch (NumberFormatException e) {}
    }

    maxResponseWaitTime = Constants.DEFAULT_CLIENT_MANAGER_MAX_WAIT_TIME;
    String maxWaitTimeStr = configDB.getConfigParameter(
                                 Constants.PARAM_CLIENT_MANAGER_MAX_WAIT_TIME);
    if (maxWaitTimeStr != null)
    {
      try
      {
        maxResponseWaitTime = Integer.parseInt(maxWaitTimeStr);
      } catch (Exception e) {}
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

    clientManagers     = new ArrayList<ClientManagerConnection>();
    clientManagerMutex = new Object();
    keepListening      = true;
    hasStopped         = false;
  }



  /**
   * Creates the server socket that will be used to accept new connections, then
   * listens for new connections until the SLAMD server is shut down.
   */
  @Override()
  public void run()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientManagerLister.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientManagerListener.run()");

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
                               "Listening for client manager connections on " +
                               "port " + listenPort);
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to create client manager server "  +
                               "socket:  " + ioe);
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
        ClientManagerConnection conn =
             new ClientManagerConnection(slamdServer, this, clientSocket);
        synchronized (clientManagerMutex)
        {
          clientManagers.add(conn);
        }
        conn.start();
      }
      catch (SLAMDException se)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                               "Unable to create a new client manager " +
                               "connection -- " + se.getMessage());
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(se));

        try
        {
          clientSocket.close();
        } catch (Exception e) {}
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "I/O error accepting client manager listener " +
                               "connection:  " + ioe +
                               " -- disabling the listener.");
        keepListening = false;
      }
    }

    hasStopped = true;
  }



  /**
   * Indicates that the listener should start listening for client manager
   * connections.
   */
  public void startListening()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientManagerListener.startListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientManagerListener.startListening()");
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
    synchronized (clientManagerMutex)
    {
      for (int i=0; i < clientManagers.size(); i++)
      {
        ClientManagerConnection clientConnection = clientManagers.get(i);
        clientConnection.disconnect(true);
      }

      clientManagers.clear();
    }
  }



  /**
   * This method will not return until the listener has actually stopped.  Note
   * that it does not stop the listener -- you should first call the
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
   * Retrieves the list of client manager connections associated with the client
   * managers that have connected to the SLAMD server.
   *
   * @return  The list of client manager connections associated with the client
   *          managers that have connected to the SLAMD server.
   */
  public ClientManagerConnection[] getClientManagers()
  {
    synchronized (clientManagerMutex)
    {
      ClientManagerConnection[] managerArray =
           new ClientManagerConnection[clientManagers.size()];
      clientManagers.toArray(managerArray);
      return managerArray;
    }
  }



  /**
   * Retrieves the list of client manager connections associated with the client
   * managers that have connected to the SLAMD server, sorted by client ID.
   *
   * @return  The list of client manager connections associated with the client
   *          managers that have connected to the SLAMD server, sorted by client
   *          ID.
   */
  public ClientManagerConnection[] getSortedClientManagers()
  {
    synchronized (clientManagerMutex)
    {
      ClientManagerConnection[] managerArray =
           new ClientManagerConnection[clientManagers.size()];
      clientManagers.toArray(managerArray);

      Arrays.sort(managerArray);
      return managerArray;
    }
  }



  /**
   * Retrieves a connection to the requested client manager.
   *
   * @param  clientID  The client ID associated with the client manager for
   *                   which to retrieve the connection.
   *
   * @return  The requested client manager connection, or <CODE>null</CODE> if
   *          no client manager is connected with the specified client ID.
   */
  public ClientManagerConnection getClientManager(String clientID)
  {
    synchronized (clientManagerMutex)
    {
      for (int i=0; i < clientManagers.size(); i++)
      {
        ClientManagerConnection conn = clientManagers.get(i);
        if (conn.getClientID().equals(clientID))
        {
          return conn;
        }
      }
    }

    return null;
  }



  /**
   * Indicates that the connection to the provided client manager has been lost
   * and that it should be removed from the list of available client managers.
   *
   * @param  clientManagerConnection  The client manager to which the connection
   *                                  was lost.
   */
  public void connectionLost(ClientManagerConnection clientManagerConnection)
  {
    // For now, just remove the manager from the list of current connections.
    synchronized (clientManagerMutex)
    {
      clientManagers.remove(clientManagerConnection);
    }
  }



  /**
   * Indicates that the specified client has disconnected from the SLAMD server
   * and that the client managers should be polled to determine if that client
   * might have been associated with a client manager.  If so, then the
   * associated client manager will be updated to reflect the lost connection.
   *
   * @param  clientConnection  The client connection that has disconnected from
   *                           the SLAMD server.
   */
  public void clientConnectionLost(ClientConnection clientConnection)
  {
    String clientIP = clientConnection.getClientIPAddress();
    synchronized (clientManagerMutex)
    {
      for (int i=0; i < clientManagers.size(); i++)
      {
        ClientManagerConnection conn = clientManagers.get(i);
        if (conn.getClientIPAddress().equals(clientIP))
        {
          conn.clientConnectionLost();
          return;
        }
      }
    }
  }



  /**
   * Retrieves the maximum length of time in seconds that the SLAMD server will
   * wait for a response to a request issued to a client manager.
   *
   * @return  The maximum length of time in seconds that the SLAMD server will
   *          wait for a response to a request issued to a client manager.
   */
  public int getMaxResponseWaitTime()
  {
    return maxResponseWaitTime;
  }



  /**
   * Retrieves the name that the client manager listener uses to subscribe to
   * the configuration handler in order to be notified of configuration changes.
   *
   * @return  The name that the client manager listener uses to subscribe to the
   *          configuration handler in order to be notified of configuration
   *          changes.
   */
  public String getSubscriberName()
  {
    return "Client Manager Listener";
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
                           "In ClientManagerListener.getParameters()");

    IntegerParameter maxWaitTimeParameter =
         new IntegerParameter(Constants.PARAM_CLIENT_MANAGER_MAX_WAIT_TIME,
                              "Maximum Wait Time",
                              "The maximum length of time (in seconds) that " +
                              "the SLAMD server will wait for a response " +
                              "to a request issued to a client manager.", true,
                              maxResponseWaitTime, true, 0, false, 0);
    IntegerParameter portParameter =
         new IntegerParameter(Constants.PARAM_CLIENT_MANAGER_LISTENER_PORT,
                              "Client Manager Listener Port",
                              "The port on which the SLAMD server listens " +
                              "for connections from client managers.",
                              true,
                              listenPort, true, 1, true, 65535);


    Parameter[] params = new Parameter[]
    {
      portParameter,
      maxWaitTimeParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the client listener manager.
   * In this case, only the maximum wait time will be read because the port
   * number is not dynamically reconfigurable.
   *
   * @throws  SLAMDServerException  If there is a problem reading or applying
   *                                the changes.
   */
  public void refreshSubscriberConfiguration()
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In ClientManagerListener.refreshConfiguration()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientManagerListener.refreshConfiguration()");

    maxResponseWaitTime = Constants.DEFAULT_CLIENT_MANAGER_MAX_WAIT_TIME;
    String maxWaitTimeStr = configDB.getConfigParameter(
                                 Constants.PARAM_CLIENT_MANAGER_MAX_WAIT_TIME);
    if (maxWaitTimeStr != null)
    {
      try
      {
        maxResponseWaitTime = Integer.parseInt(maxWaitTimeStr);
      } catch (Exception e) {}
    }
  }



  /**
   * Re-reads the configuration for the specified parameter if the parameter is
   * applicable to the client manager listener.  In this case, only the maximum
   * wait time will be read because the port number is not dynamically
   * reconfigurable.
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
                           "In ClientManagerListener.refreshConfiguration(" +
                           parameterName + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In ClientManagerListener.refreshConfiguration(" +
                           parameterName + ')');

    if (parameterName.equalsIgnoreCase(
                           Constants.PARAM_CLIENT_MANAGER_MAX_WAIT_TIME))
    {
      maxResponseWaitTime = Constants.DEFAULT_CLIENT_MANAGER_MAX_WAIT_TIME;
      String maxWaitTimeStr =
           configDB.getConfigParameter(
                Constants.PARAM_CLIENT_MANAGER_MAX_WAIT_TIME);
      if (maxWaitTimeStr != null)
      {
        try
        {
          maxResponseWaitTime = Integer.parseInt(maxWaitTimeStr);
        } catch (Exception e) {}
      }
    }
  }
}

