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
import java.util.Iterator;
import javax.net.ssl.SSLServerSocketFactory;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.db.SLAMDDB;
import com.slamd.job.JobClass;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class implements a listener that can be used to accept connections from
 * clients for the use of reporting in-progress statistics while a job is
 * running.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatListener
       extends Thread
       implements ConfigSubscriber
{
  // The list of connections established to this listener.
  private ArrayList<StatClientConnection> connectionList;

  // Indicates whether we should keep listening for more connections.
  private boolean keepListening;

  // Indicates whether we have stopped running yet.
  private boolean hasStopped;

  // Indicates whether we will require clients to authenticate.
  private boolean requireAuthentication;

  // Indicates whether we will use SSL to communicate with the clients.
  private boolean useSSL;

  // The interval that we should use for keepalive messages to the clients.
  private int keepAliveInterval;

  // The port on which this listener will accept connections.
  private int listenPort;

  // The maximum length of time we should wait for a response from the client.
  private int maxResponseWaitTime;

  // The mutex providing threadsafe access to the connection list.
  private final Object connectionListMutex;

  // The server socket used to accept connections from clients.
  private ServerSocket serverSocket;

  // The configuration handler to use for this stat listener.
  private SLAMDDB configDB;

  // The SLAMD server with which this listener is associated.
  private SLAMDServer slamdServer;



  /**
   * Creates a new instance of this stat listener.
   *
   * @param  slamdServer  The SLAMD server with which this stat listener will be
   *                      associated.
   */
  public StatListener(SLAMDServer slamdServer)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Entering StatListener constructor");

    setName("Stat Client Listener");

    // Initialize all the instance variables
    this.slamdServer    = slamdServer;
    connectionList      = new ArrayList<StatClientConnection>();
    connectionListMutex = new Object();
    keepListening       = true;
    hasStopped          = true;

    // Determine which port we should listen on.
    listenPort = Constants.DEFAULT_STAT_LISTENER_PORT_NUMBER;
    configDB = slamdServer.getConfigDB();
    String portStr =
         configDB.getConfigParameter(Constants.PARAM_STAT_LISTENER_PORT);
    if ((portStr != null) && (portStr.length() > 0))
    {
      try
      {
        listenPort = Integer.parseInt(portStr);
      } catch (NumberFormatException nfe) {}
    }


    // Register as a configuration subscriber.
    configDB.registerAsSubscriber(this);

    ClientListener clientListener = slamdServer.getClientListener();
    keepAliveInterval     = clientListener.getKeepAliveInterval();
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
                           "Leaving StatListener constructor");
  }



  /**
   * Indicates that the listener should start listening for client connections.
   */
  public void startListening()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In StatListener.startListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In StatListener.startListening()");
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
                           "In StatListener.stopListening()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In StatListener.stopListening()");

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
    synchronized (connectionListMutex)
    {
      Iterator i = connectionList.iterator();
      while (i.hasNext())
      {
        StatClientConnection connection = (StatClientConnection) i.next();
        connection.sendServerShutdownMessage(true);
      }

      connectionList.clear();
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
   * Creates the server socket that will be used to accept new connections, then
   * listens for new connections until the SLAMD server is shut down.
   */
  @Override()
  public void run()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE, "In StatListener.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In StatListener.run()");

    hasStopped = false;

    if (useSSL)
    {
      try
      {
        SSLServerSocketFactory socketFactory =
             (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = socketFactory.createServerSocket(listenPort);
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Listening for SSL-based stat client " +
                               "connections on port " + listenPort);
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
                               "Listening for stat client connections on " +
                               "port " + listenPort);
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to create stat client server "  +
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
        StatClientConnection conn = new StatClientConnection(slamdServer, this,
                                                             clientSocket);
        synchronized (connectionListMutex)
        {
          connectionList.add(conn);
        }
        conn.start();
      }
      catch (SLAMDException se)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(se));
        slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT,
                               "Unable to create a new stat client " +
                               "connection -- " + se.getMessage());

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
                               "I/O error accepting stat client connection:  " +
                               ioe + " -- disabling the listener.");
        keepListening = false;
      }
    }

    hasStopped = true;
  }



  /**
   * Retrieves an array of the connections currently established to this stat
   * listener.
   *
   * @return  An array of the connections currently established to this stat
   *          listener.
   */
  public StatClientConnection[] getConnectionList()
  {
    synchronized (connectionListMutex)
    {
      StatClientConnection[] connArray =
           new StatClientConnection[connectionList.size()];
      connectionList.toArray(connArray);
      return connArray;
    }
  }



  /**
   * Retrieves an array of the connections currently established to this stat
   * listener, sorted by client ID.
   *
   * @return  An array of the connections currently established to this stat
   *          listener, sorted by client ID.
   */
  public StatClientConnection[] getSortedConnectionList()
  {
    synchronized (connectionListMutex)
    {
      StatClientConnection[] connArray =
           new StatClientConnection[connectionList.size()];
      connectionList.toArray(connArray);

      Arrays.sort(connArray);
      return connArray;
    }
  }



  /**
   * Sends a message to the specified client indicating that it should
   * disconnect from the SLAMD server.
   *
   * @param  clientID  The client ID of the client to which the request is to be
   *                   sent.
   *
   * @return  <CODE>true</CODE> if a request was sent to the specified client,
   *          or <CODE>false</CODE> if not.
   */
  public boolean requestDisconnect(String clientID)
  {
    StatClientConnection clientToDisconnect = null;

    synchronized (connectionListMutex)
    {
      for (int i=0; i < connectionList.size(); i++)
      {
        StatClientConnection clientConnection = connectionList.get(i);
        if (clientConnection.getClientID().equals(clientID))
        {
          clientToDisconnect = clientConnection;
          break;
        }
      }
    }

    if (clientToDisconnect == null)
    {
      return false;
    }

    clientToDisconnect.sendServerShutdownMessage(false);
    return true;
  }



  /**
   * Sends a message to all clients indicating that they should disconnect from
   * the SLAMD server.
   */
  public void requestDisconnectAll()
  {
    synchronized (connectionListMutex)
    {
      for (int i=0; i < connectionList.size(); i++)
      {
        StatClientConnection clientConnection = connectionList.get(i);
        clientConnection.sendServerShutdownMessage(false);
      }
    }
  }



  /**
   * Forcefully closes the connection to the specified client and removes all
   * references to it from the SLAMD server.
   *
   * @param  clientID  The client ID of the client to be disconnected.
   *
   * @return  <CODE>true</CODE> if the client was disconnected, or
   *          <CODE>false</CODE> if not.
   */
  public boolean forceDisconnect(String clientID)
  {
    StatClientConnection clientToDisconnect = null;

    synchronized (connectionListMutex)
    {
      for (int i=0; i < connectionList.size(); i++)
      {
        StatClientConnection clientConnection = connectionList.get(i);
        if (clientConnection.getClientID().equals(clientID))
        {
          clientToDisconnect = clientConnection;
          break;
        }
      }

      if (clientToDisconnect == null)
      {
        return false;
      }

      try
      {
        clientToDisconnect.clientSocket.close();
      } catch (Exception e) {}
    }

    connectionLost(clientToDisconnect);
    return true;
  }



  /**
   * Forcefully disconnects all real-time stat clients from the SLAMD server and
   * removes all references to them.
   */
  public void forceDisconnectAll()
  {
    synchronized (connectionListMutex)
    {
      for (int i=0; i < connectionList.size(); i++)
      {
        StatClientConnection clientConnection = connectionList.get(i);

        try
        {
          clientConnection.clientSocket.close();
        } catch (Exception e) {}
      }

      connectionList.clear();
    }
  }



  /**
   * Indicates that the specified stat client has disconnected from the SLAMD
   * server.
   *
   * @param  connection  The connection to the stat client that has been lost.
   */
  public void connectionLost(StatClientConnection connection)
  {
    // For now, just remove the client from the list of current connections.
    synchronized (connectionListMutex)
    {
      connectionList.remove(connection);
    }
  }



  /**
   * Retrieves the keepalive interval that should be used for new client
   * connections.
   *
   * @return  The keepalive interval that should be used for new client
   *          connections.
   */
  public int getKeepAliveInterval()
  {
    return keepAliveInterval;
  }



  /**
   * Retrieves the maximum length of time in seconds that the SLAMD server will
   * wait for a response to a request issued to a stat client.
   *
   * @return  The maximum length of time in seconds that the SLAMD server will
   *          wait for a response to a request issued to a stat client.
   */
  public int getMaxResponseWaitTime()
  {
    return maxResponseWaitTime;
  }



  /**
   * Indicates whether this listener is configured to require authentication.
   *
   * @return  <CODE>true</CODE> if this listener is configured to require
   *          authentication, or <CODE>false</CODE> if not.
   */
  public boolean requireAuthentication()
  {
    return requireAuthentication;
  }



  /**
   * Retrieves the name that the stat client listener uses to subscribe to
   * the configuration handler in order to be notified of configuration changes.
   *
   * @return  The name that the stat client listener uses to subscribe to the
   *          configuration handler in order to be notified of configuration
   *          changes.
   */
  public String getSubscriberName()
  {
    return "Real-Time Stat Listener";
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
                           "In StatListener.getParameters()");

    IntegerParameter portParameter =
         new IntegerParameter(Constants.PARAM_STAT_LISTENER_PORT,
                              "Stat Listener Port",
                              "The port on which the SLAMD server listens " +
                              "for connections from clients for reporting " +
                              "real-time statistics.",
                              true,
                              listenPort, true, 1, true, 65535);

    IntegerParameter maxIterationsParameter =
      new IntegerParameter(Constants.PARAM_MAX_STAT_INTERVALS,
                           "Max Stat Intervals",
                           "The maximum number of collection intervals that " +
                           "should be held for a job at any time.  Note that" +
                           "this will only take effect for jobs started " +
                           "after the change is made", true,
                           slamdServer.statHandler.maxIntervals, true,
                           2, false, 0);


    Parameter[] params = new Parameter[]
    {
      portParameter,
      maxIterationsParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the stat listener.
   * In this case, it doesn't do anything because nothing is dynamically
   * reconfigurable.
   *
   * @throws  SLAMDServerException  If there is a problem reading or applying
   *                                the changes.
   */
  public void refreshSubscriberConfiguration()
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "In StatListener.refreshConfiguration()");
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In StatListener.refreshConfiguration()");

    String intervalsStr =
         configDB.getConfigParameter(Constants.PARAM_MAX_STAT_INTERVALS);
    if ((intervalsStr != null) && (intervalsStr.length() > 0))
    {
      try
      {
        slamdServer.statHandler.maxIntervals = Integer.parseInt(intervalsStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                               "Invalid value for config parameter " +
                               Constants.PARAM_MAX_STAT_INTERVALS + ":  " +
                               intervalsStr + " -- " + e);
      }
    }
  }



  /**
   * Re-reads the configuration for the specified parameter if the parameter is
   * applicable to the stat listener.  In this case, nothing will be applicable.
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
                           "In StatListener.refreshConfiguration(" +
                           parameterName + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_CLIENT_DEBUG,
                           "In StatListener.refreshConfiguration(" +
                           parameterName + ')');

    if (parameterName.equals(Constants.PARAM_MAX_STAT_INTERVALS))
    {
      String intervalsStr =
           configDB.getConfigParameter(Constants.PARAM_MAX_STAT_INTERVALS);
      if ((intervalsStr != null) && (intervalsStr.length() > 0))
      {
        try
        {
          slamdServer.statHandler.maxIntervals = Integer.parseInt(intervalsStr);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Invalid value for config parameter " +
                                 Constants.PARAM_MAX_STAT_INTERVALS + ":  " +
                                 intervalsStr + " -- " + e);
        }
      }
    }
  }
}

