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
package com.slamd.client;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import javax.net.ssl.SSLSocketFactory;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;
import com.slamd.common.SLAMDException;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.job.JobClass;
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
import com.slamd.parameter.ParameterList;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatPersistenceThread;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD client that can connect to the SLAMD server and
 * interact with it to process the various jobs.  Note that this version of the
 * SLAMD client will only work on a single job at a time, although that job may
 * spawn as many threads as it needs to operate.  Also, this SLAMD client
 * requires a Java version of at least 1.3 because it makes use of shutdown
 * hooks to perform cleanup work when the client is shutting down.
 *
 *
 * @author   Neil A. Wilson
 */
public class Client
       extends Thread
{
  /**
   * The version of the SLAMD client software being used.
   */
  public static final String SLAMD_CLIENT_VERSION =
                                  DynamicConstants.SLAMD_VERSION;



  /**
   * The name to assign to the main client thread.
   */
  public static final String MAIN_THREAD_NAME = "Main Client Thread";



  // Static variables that will be used for handling client-side persistence.
  static boolean               persistStatistics     = false;
  static int                   persistenceInterval   = 0;
  static String                persistenceDirectory  = null;
  static StatPersistenceThread statPersistenceThread = null;



  // The ASN.1 reader used to read messages from the server.
  ASN1Reader reader;

  // The ASN.1 writer used to write messages to the server.
  ASN1Writer writer;

  // Indicates whether the client should aggregate the data from all the
  // individual threads before sending the results to the server.
  boolean aggregateThreadData;

  // Indicates whether the client should blindly trust any SSL certificate
  // presented by the SLAMD server.
  boolean blindTrust;

  // Indicates whether the client should disconnect from the SLAMD server.
  boolean disconnectRequested;

  // Indicates whether the client should enable the collection of real-time
  // statistics.
  boolean enableRealTimeStats;

  // Indicates whether the client has disconnected from the SLAMD server.
  boolean hasDisconnected;

  // Indicates whether this client should operate in restricted mode.
  boolean restrictedMode;

  // Indicates whether this client has already received a stop request for the
  // current job.
  boolean stopRequested;

  // Indicates whether this client provides support for time synchronization.
  boolean supportsTimeSync;

  // Indicates whether to use the standard or custom class loader.
  boolean useCustomClassLoader;

  // Indicates whether the client will use SSL to communicate with the SLAMD
  // server.
  boolean useSSL;

  // The type of authentication that the client will perform with the SLAMD
  // server.
  int authType;

  // The current state of the client.
  int clientState;

  // Specifies the message ID of the next message the client will send.
  int messageID;

  // The port number that will be used to communicate with the SLAMD server.
  int serverPort;

  // The port number that will be used to communicate with the SLAMD server to
  // provide real-time stat data.
  int serverStatPort;

  // The interval in seconds that clients should use when reporting statistics
  // to the server in real-time.
  int statReportInterval;

  // The difference in milliseconds between the clock on the client system and
  // the clock on the server system.
  long serverTimeOffset;

  // The message writer to which all informational and verbose messages will be
  // written.
  ClientMessageWriter messageWriter;

  // The shutdown listener that will be notified when the client has stopped.
  ClientShutdownListener shutdownListener;

  // The local definition of a job that is currently defined
  ClientSideJob jobInProgress;

  // The stat reporter that can be used for sending real-time statistics to the
  // SLAMD server.
  RealTimeStatReporter statReporter;

  // The socket that will be used to communicate with the SLAMD server
  Socket clientSocket;

  // The ID that the client will use to authenticate to the SLAMD server.
  String authID;

  // The credentials that the client will use to authenticate to the SLAMD
  // server.
  private String authCredentials;

  // The name of the directory to which Java class files provided by the server
  // will be written
  String classPath;

  // The identifier that will be used for this client.
  String clientID;

  // The address of the SLAMD server to which the client will authenticate.
  String serverAddress;

  // The location of the JSSE key store that will be used if the communication
  // between the client and the server is SSL-based.
  String sslKeyStore;

  // The password needed to access the JSSE key store.
  String sslKeyStorePassword;

  // The location of the JSSE trust store that will be used if the communication
  // between the client and the server is SSL-based.
  String sslTrustStore;

  // The password needed to access the JSSE trust store.
  String sslTrustStorePassword;



  /**
   * Creates a new client that will communicate with the SLAMD server at the
   * specified address and port.  There will not be any authentication
   * performed, nor will the connection use SSL.
   *
   * @param  serverAddress         The IP address or DNS hostname of the SLAMD
   *                               server.
   * @param  serverPort            The port number on which the SLAMD server is
   *                               listening for client connections.
   * @param  serverStatPort        The port number on which the SLAMD server
   *                               listens for real-time statistical data.
   * @param  supportsTimeSync      Indicates whether the client should support
   *                               time synchronization with the server.
   * @param  enableRealTimeStats   Indicates whether the client should support
   *                               the collection of real-time statistics for
   *                               reporting back to the SLAMD server.
   * @param  statReportInterval    The length of time in seconds between stat
   *                               updates to the server.
   * @param  persistStatistics     Indicates whether the client should
   *                               periodically save statistical data on disk.
   * @param  persistenceDirectory  The directory in which the persisted data
   *                               will be written.
   * @param  persistenceInterval   The interval in seconds between saves of the
   *                               persistence data.
   * @param  useCustomClassLoader  Indicates whether to use the custom class
   *                               loader for loading job classes.
   * @param  classPath             The path to which Java class files will be
   *                               written if they are sent from the client to
   *                               the server using a class transfer response.
   * @param  messageWriter         The writer to which informational and verbose
   *                               messages will be written.
   *
   * @throws  ClientException  If some problem is encountered while creating the
   *                           client.
   */
  public Client(String serverAddress, int serverPort, int serverStatPort,
                boolean supportsTimeSync,  boolean enableRealTimeStats,
                int statReportInterval, boolean persistStatistics,
                String persistenceDirectory, int persistenceInterval,
                boolean useCustomClassLoader, String classPath,
                ClientMessageWriter messageWriter)
         throws ClientException
  {
    this(null, null, serverAddress, serverPort, serverStatPort,
         supportsTimeSync, enableRealTimeStats, statReportInterval,
         persistStatistics, persistenceDirectory, persistenceInterval,
         Constants.AUTH_TYPE_NONE, null, null, false, useCustomClassLoader,
         classPath, false, false, null, null, null, null, messageWriter);
  }



  /**
   * Creates a new client that will communicate with the SLAMD server at the
   * specified address and port, and will have the option of using SSL.  There
   * will not be any authentication performed.
   *
   * @param  serverAddress          The IP address or DNS hostname of the SLAMD
   *                                server.
   * @param  serverPort             The port number on which the SLAMD server is
   *                                listening for client connections.
   * @param  serverStatPort         The port number on which the SLAMD server
   *                                listens for real-time statistical data.
   * @param  supportsTimeSync       Indicates whether the client should support
   *                                time synchronization with the server.
   * @param  enableRealTimeStats    Indicates whether the client should support
   *                                the collection of real-time statistics for
   *                                reporting back to the SLAMD server.
   * @param  statReportInterval     The length of time in seconds between stat
   *                                updates to the server.
   * @param  persistStatistics      Indicates whether the client should
   *                                periodically save statistical data on disk.
   * @param  persistenceDirectory   The directory in which the persisted data
   *                                will be written.
   * @param  persistenceInterval    The interval in seconds between saves of the
   *                                persistence data.
   * @param  restrictedMode         Indicates whether this client will operate
   *                                in restricted mode, meaning that the server
   *                                should only ask it to run jobs for which it
   *                                has been explicitly requested.
   * @param  useCustomClassLoader   Indicates whether to use the custom class
   *                                loader for loading job classes.
   * @param  classPath              The path to which Java class files will be
   *                                written if they are sent from the client to
   *                                the server using a class transfer response.
   * @param  useSSL                 Indicates whether SSL will be used on the
   *                                connection between the client and the
   *                                server.
   * @param  blindTrust             Indicates whether the client should blindly
   *                                trust any SSL certificate presented by the
   *                                SLAMD server.
   * @param  sslKeyStore            The location of the JSSE key store that will
   *                                be used if the communication between the
   *                                client and the server is SSL-based.
   * @param  sslKeyStorePassword    The password needed to access the SSL key
   *                                store.
   * @param  sslTrustStore          The location of the JSSE trust store that
   *                                will be used if the communication between
   *                                the client and the server is SSL-based.
   * @param  sslTrustStorePassword  The password needed to access the SSL trust
   *                                store
   * @param  messageWriter          The writer to which informational and
   *                                verbose messages will be written.
   *
   * @throws  ClientException  If some problem is encountered while creating the
   *                           client.
   */
  public Client(String serverAddress, int serverPort, int serverStatPort,
                boolean supportsTimeSync, boolean enableRealTimeStats,
                int statReportInterval, boolean persistStatistics,
                String persistenceDirectory, int persistenceInterval,
                boolean restrictedMode, boolean useCustomClassLoader,
                String classPath, boolean useSSL, boolean blindTrust,
                String sslKeyStore, String sslKeyStorePassword,
                String sslTrustStore, String sslTrustStorePassword,
                ClientMessageWriter messageWriter)
         throws ClientException
  {
    this(null, null, serverAddress, serverPort, serverStatPort,
         supportsTimeSync, enableRealTimeStats, statReportInterval,
         persistStatistics, persistenceDirectory, persistenceInterval,
         Constants.AUTH_TYPE_NONE, null, null, restrictedMode,
         useCustomClassLoader, classPath, useSSL, blindTrust, sslKeyStore,
         sslKeyStorePassword, sslTrustStore,
         sslTrustStorePassword, messageWriter);
  }



  /**
   * Creates a new client that will communicate with the SLAMD server at the
   * specified address and port, and will use the specified authentication type.
   * The connection will not use SSL.
   *
   * @param  serverAddress         The IP address or DNS hostname of the SLAMD
   *                               server.
   * @param  serverPort            The port number on which the SLAMD server is
   *                               listening for client connections.
   * @param  serverStatPort        The port number on which the SLAMD server
   *                               listens for real-time statistical data.
   * @param  supportsTimeSync      Indicates whether the client should support
   *                               time synchronization with the server.
   * @param  enableRealTimeStats   Indicates whether the client should support
   *                               the collection of real-time statistics for
   *                               reporting back to the SLAMD server.
   * @param  statReportInterval    The length of time in seconds between stat
   *                               updates to the server.
   * @param  persistStatistics     Indicates whether the client should
   *                               periodically save statistical data on disk.
   * @param  persistenceDirectory   The directory in which the persisted data
   *                                will be written.
   * @param  persistenceInterval   The interval in seconds between saves of the
   *                               persistence data.
   * @param  authType              The type of authentication to perform with
   *                               the SLAMD server.
   * @param  authID                The ID to use to authenticate to the SLAMD
   *                               server.
   * @param  authCredentials       The credentials that will be used to
   *                               authenticate to the SLAMD server.
   * @param  restrictedMode        Indicates whether this client should operate
   *                               in restricted mode.
   * @param  useCustomClassLoader  Indicates whether to use the custom class
   *                               loader for loading job classes.
   * @param  classPath             The path to which Java class files will be
   *                               written if they are sent from the client to
   *                               the server using a class transfer response.
   * @param  messageWriter         The writer to which informational and verbose
   *                               messages will be written.
   *
   * @throws  ClientException  If some problem is encountered while creating the
   *                           client.
   */
  public Client(String serverAddress, int serverPort, int serverStatPort,
                boolean supportsTimeSync, boolean enableRealTimeStats,
                int statReportInterval, boolean persistStatistics,
                String persistenceDirectory, int persistenceInterval,
                int authType, String authID, String authCredentials,
                boolean restrictedMode, boolean useCustomClassLoader,
                String classPath, ClientMessageWriter messageWriter)
         throws ClientException
  {
    this(null, null, serverAddress, serverPort, serverStatPort,
         supportsTimeSync, enableRealTimeStats, statReportInterval,
         persistStatistics, persistenceDirectory, persistenceInterval, authType,
         authID, authCredentials, restrictedMode, useCustomClassLoader,
         classPath, false, false, null, null, null, null, messageWriter);
  }



  /**
   * Creates a new client that will communicate with the SLAMD server at the
   * specified address and port, and will use the specified authentication type
   * and optionally may use SSL.
   *
   * @param  clientID               The client ID that should be used for the
   *                                client.  It may be {@code null} if the
   *                                client ID should be automatically selected.
   * @param  clientAddress          The IP address on the client system from
   *                                which the connection should originate.  It
   *                                may be {@code null} if the client address
   *                                should be automatically selected.
   * @param  serverAddress          The IP address or DNS hostname of the SLAMD
   *                                server.
   * @param  serverPort             The port number on which the SLAMD server is
   *                                listening for client connections.
   * @param  serverStatPort         The port number on which the SLAMD server
   *                                listens for real-time statistical data.
   * @param  supportsTimeSync       Indicates whether the client should support
   *                                time synchronization with the server.
   * @param  enableRealTimeStats    Indicates whether the client should support
   *                                the collection of real-time statistics for
   *                                reporting back to the SLAMD server.
   * @param  statReportInterval     The length of time in seconds between stat
   *                                updates to the server.
   * @param  persistStatistics      Indicates whether the client should
   *                                periodically save statistical data on disk.
   * @param  persistenceDirectory   The directory in which the persisted data
   *                                will be written.
   * @param  persistenceInterval    The interval in seconds between saves of the
   *                                persistence data.
   * @param  authType               The type of authentication to perform with
   *                                the SLAMD server.
   * @param  authID                 The ID to use to authenticate to the SLAMD
   *                                server.
   * @param  authCredentials        The credentials that will be used to
   *                                authenticate to the SLAMD server.
   * @param  restrictedMode         Indicates whether this client will operate
   *                                in restricted mode, meaning that the server
   *                                should only ask it to run jobs for which it
   *                                has been explicitly requested.
   * @param  useCustomClassLoader   Indicates whether to use the custom class
   *                                loader for loading job classes.
   * @param  classPath              The path to which Java class files will be
   *                                written if they are sent from the client to
   *                                the server using a class transfer response.
   * @param  useSSL                 Indicates whether SSL will be used on the
   *                                connection between the client and the
   *                                server.
   * @param  blindTrust             Indicates whether the client should blindly
   *                                trust any SSL certificate presented by the
   *                                SLAMD server.
   * @param  sslKeyStore            The location of the JSSE key store that will
   *                                be used if the communication between the
   *                                client and the server is SSL-based.
   * @param  sslKeyStorePassword    The password needed to access the SSL key
   *                                store.
   * @param  sslTrustStore          The location of the JSSE trust store that
   *                                will be used if the communication between
   *                                the client and the server is SSL-based.
   * @param  sslTrustStorePassword  The password needed to access the SSL trust
   *                                store
   * @param  messageWriter          The writer to which informational and
   *                                verbose messages will be written.
   *
   * @throws  ClientException  If some problem is encountered while creating the
   *                           client.
   */
  public Client(String clientID, String clientAddress, String serverAddress,
                int serverPort, int serverStatPort, boolean supportsTimeSync,
                boolean enableRealTimeStats, int statReportInterval,
                boolean persistStatistics, String persistenceDirectory,
                int persistenceInterval, int authType, String authID,
                String authCredentials, boolean restrictedMode,
                boolean useCustomClassLoader, String classPath, boolean useSSL,
                boolean blindTrust, String sslKeyStore,
                String sslKeyStorePassword, String sslTrustStore,
                String sslTrustStorePassword, ClientMessageWriter messageWriter)
         throws ClientException
  {
    setName(MAIN_THREAD_NAME);
    messageWriter.writeVerbose("SLAMDClient starting up...");
    messageWriter.writeVerbose("");


    // Print out stats about the client system
    Runtime runtime = Runtime.getRuntime();
    messageWriter.writeVerbose("Java Version:                         " +
                               System.getProperty("java.version"));
    messageWriter.writeVerbose("Java Installation:                    " +
                               System.getProperty("java.home"));
    messageWriter.writeVerbose("Operating System Name:                " +
                               System.getProperty("os.name"));
    messageWriter.writeVerbose("Operating System Version:             " +
                               System.getProperty("os.version"));
    messageWriter.writeVerbose("Total CPUs Available to JVM:          " +
                               runtime.availableProcessors());
    messageWriter.writeVerbose("Total Memory Available to JVM:        " +
                               runtime.maxMemory());
    messageWriter.writeVerbose("Memory Currently Held by JVM:         " +
                               runtime.totalMemory());
    messageWriter.writeVerbose("Unused Memory Currently Held by JVM:  " +
                               runtime.freeMemory());
    messageWriter.writeVerbose("");


    // Register a shutdown hook with this client.
    runtime.addShutdownHook(new ClientShutdownHook(this));


    // First, set all the values of the instance variables corresponding to
    // the provided parameters.
    this.serverAddress         = serverAddress;
    this.serverPort            = serverPort;
    this.serverStatPort        = serverStatPort;
    this.supportsTimeSync      = supportsTimeSync;
    this.enableRealTimeStats   = enableRealTimeStats;
    this.statReportInterval    = statReportInterval;
    this.authType              = authType;
    this.authID                = authID;
    this.authCredentials       = authCredentials;
    this.restrictedMode        = restrictedMode;
    this.useCustomClassLoader  = useCustomClassLoader;
    this.classPath             = classPath;
    this.useSSL                = useSSL;
    this.blindTrust            = blindTrust;
    this.sslKeyStore           = sslKeyStore;
    this.sslKeyStorePassword   = sslKeyStorePassword;
    this.sslTrustStore         = sslTrustStore;
    this.sslTrustStorePassword = sslTrustStorePassword;
    this.messageWriter         = messageWriter;
    this.shutdownListener      = null;


    // Set the persistence variables.
    Client.persistStatistics    = persistStatistics;
    Client.persistenceDirectory = persistenceDirectory;
    Client.persistenceInterval  = persistenceInterval;


    // Make sure that a class path has been provided.
    if (classPath == null)
    {
      throw new ClientException("No job class path specified", false);
    }


    // Indicate that the client has not yet received a stop request for the
    // current job.
    stopRequested = false;

    // Indicate that the client should not yet disconnect from the server.
    disconnectRequested = false;

    // Then set the values of all instance variables that need to be initialized
    // but were not provided as parameters.
    aggregateThreadData = false;
    clientState         = Constants.CLIENT_STATE_NOT_CONNECTED;
    messageID           = 0;


    // Next, establish the connection to the server.  If it can't be done, then
    // throw an exception.
    if (useSSL)
    {
      if (blindTrust)
      {
        try
        {
          JSSEBlindTrustSocketFactory socketFactory =
               new JSSEBlindTrustSocketFactory();
          if (clientAddress == null)
          {
            clientSocket = socketFactory.makeSocket(serverAddress, serverPort);
          }
          else
          {
            clientSocket = socketFactory.createSocket(serverAddress, serverPort,
                                InetAddress.getByName(clientAddress), 0);
          }
          writer = new ASN1Writer(clientSocket.getOutputStream());
          reader = new ASN1Reader(clientSocket.getInputStream());
        }
        catch (Exception e)
        {
          String message = "Unable to establish an SSL blind trust " +
                           "connection to the SLAMD server:  " + e;
          messageWriter.writeMessage(message);
          throw new ClientException(message, false, e);
        }
      }
      else
      {
        try
        {
          if ((sslKeyStore != null) && (sslKeyStore.length() > 0))
          {
            System.setProperty(Constants.SSL_KEY_STORE_PROPERTY, sslKeyStore);
          }

          if ((sslKeyStorePassword != null) &&
              (sslKeyStorePassword.length() > 0))
          {
            System.setProperty(Constants.SSL_KEY_PASSWORD_PROPERTY,
                               sslKeyStorePassword);
          }

          if ((sslTrustStore != null) && (sslTrustStore.length() > 0))
          {
            System.setProperty(Constants.SSL_TRUST_STORE_PROPERTY,
                               sslTrustStore);
          }

          if ((sslTrustStorePassword != null) &&
              (sslTrustStorePassword.length() > 0))
          {
            System.setProperty(Constants.SSL_TRUST_PASSWORD_PROPERTY,
                               sslTrustStorePassword);
          }

          SSLSocketFactory socketFactory =
               (SSLSocketFactory) SSLSocketFactory.getDefault();
          if (clientAddress == null)
          {
            clientSocket =
                 socketFactory.createSocket(serverAddress, serverPort);
          }
          else
          {
            clientSocket = socketFactory.createSocket(serverAddress, serverPort,
                                InetAddress.getByName(clientAddress), 0);
          }
          writer = new ASN1Writer(clientSocket.getOutputStream());
          reader = new ASN1Reader(clientSocket.getInputStream());
        }
        catch (Exception e)
        {
          String message = "Unable to establish an SSL-based connection to " +
                           "the SLAMD server:  " + e;
          messageWriter.writeMessage(message);
          throw new ClientException(message, false, e);
        }
      }
    }
    else
    {
      try
      {
        if (clientAddress == null)
        {
          clientSocket = new Socket(serverAddress, serverPort);
        }
        else
        {
          clientSocket = new Socket(serverAddress, serverPort,
                                    InetAddress.getByName(clientAddress), 0);
        }

        writer = new ASN1Writer(clientSocket.getOutputStream());
        reader = new ASN1Reader(clientSocket.getInputStream());
      }
      catch (IOException ioe)
      {
        messageWriter.writeMessage("Unable to connect to SLAMD server:  " +
                                   ioe);
        throw new ClientException("Unable to connect to SLAMD server:  " + ioe,
                                  false, ioe);
      }
    }


    if (clientID == null)
    {
      try
      {
        clientID = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException uhe)
      {
        clientID = "unknown";
      }
    }

    clientID += ":" + clientSocket.getLocalPort();
    this.clientID = clientID;

    messageWriter.writeVerbose("Set Client ID:  " + clientID);


    // Create the client hello message to send to the SLAMD server.
    ClientHelloMessage helloMessage =
         new ClientHelloMessage(getMessageID(), SLAMD_CLIENT_VERSION,
                                clientID, authType, authID, authCredentials,
                                false, restrictedMode, supportsTimeSync);
    try
    {
      writer.writeElement(helloMessage.encode());
      if (messageWriter.usingVerboseMode())
      {
        messageWriter.writeVerbose("Sent the hello request");
        messageWriter.writeVerbose(helloMessage.toString());
      }
    }
    catch (IOException ioe)
    {
      messageWriter.writeMessage("ERROR sending hello message:  " + ioe);
      throw new ClientException("ERROR sending hello message:  " + ioe, false,
           ioe);
    }


    // Read the hello response from the server
    try
    {
      HelloResponseMessage helloResp =
           (HelloResponseMessage)
           Message.decode(reader.readElement(Constants.MAX_BLOCKING_READ_TIME));
      if (messageWriter.usingVerboseMode())
      {
        messageWriter.writeVerbose("Received the hello response");
        messageWriter.writeVerbose(helloResp.toString());
      }

      if (helloResp.getResponseCode() != Constants.MESSAGE_RESPONSE_SUCCESS)
      {
        String msg = "Server rejected hello request with return code " +
                     helloResp.getResponseCode();
        String respMsg = helloResp.getResponseMessage();
        if ((respMsg != null) && (respMsg.length() != 0))
        {
          msg += " (" + respMsg + ')';
        }

        throw new ClientException(msg, false);
      }

      // Get the server's current time and use it to calculate the time
      // difference between the client and the server.
      long clientTime = System.currentTimeMillis();
      long serverTime = helloResp.getServerTime();
      if (serverTime > 0)
      {
        serverTimeOffset = (serverTime - clientTime);
      }
      else
      {
        serverTimeOffset = 0;
      }

      // If the time skew is significant, print a warning message.
      long absOffset = Math.abs(serverTimeOffset);
      if (absOffset > 2000)
      {
        messageWriter.writeMessage("WARNING:  Time skew of " + absOffset +
                                   " milliseconds detected between the " +
                                   "client and the server.  The client " +
                                   "will attempt to correct for this.");
      }
    }
    catch (Exception e)
    {
      if (e instanceof ClientException)
      {
        throw (ClientException) e;
      }
      else
      {
        throw new ClientException("ERROR reading server hello response:  " + e,
             false, e);
      }
    }


    // If we are going to have the ability to collect real-time statistics, then
    // do so here.
    if (enableRealTimeStats)
    {
      try
      {
        statReporter = new RealTimeStatReporter(serverAddress, serverStatPort,
                                                statReportInterval, clientID,
                                                authID, authCredentials, useSSL,
                                                blindTrust, sslKeyStore,
                                                sslKeyStorePassword,
                                                sslTrustStore,
                                                sslTrustStorePassword);
        statReporter.start();
      }
      catch (SLAMDException se)
      {
        throw new ClientException("ERROR creating stat reporter:  " +
             se.getMessage(), false, se);
      }
    }


    // If we are going to persist statistics on the client side, then create
    // a stat persistence thread to manage this.
    if (persistStatistics)
    {
      try
      {
        statPersistenceThread =
             new StatPersistenceThread(clientID, persistenceDirectory,
                                       persistenceInterval);
        statPersistenceThread.start();
      }
      catch (IOException ioe)
      {
        throw new ClientException("ERROR starting stat persistence thread:  " +
             ioe, false, ioe);
      }
    }


    // Configure a timeout for the socket so that we can determine when we
    // want to disconnect.
    try
    {
      clientSocket.setSoTimeout(1000);
    } catch (IOException ioe) {}


    // If we have gotten here, then the hello was successful.  Return control
    // back to the invoker so that it can control how this client is used.
    clientState = Constants.CLIENT_STATE_IDLE;
    writeMessage("The SLAMD client has started.");
    return;
  }



  /**
   * Disconnects the client from the SLAMD server.  This method will not return
   * until the client has actually disconnected.
   */
  public void disconnect()
  {
    messageWriter.writeVerbose("Client requested a disconnect from the " +
                               "server.");
    disconnectRequested = true;

    while (! hasDisconnected)
    {
      try
      {
        Thread.sleep(100);
      } catch (InterruptedException ie) {}
    }

    messageWriter.writeMessage("Disconnected from the SLAMD server");
  }



  /**
   * Retrieves the message writer associated with this client.
   *
   * @return  The message writer associated with this client.
   */
  public ClientMessageWriter getMessageWriter()
  {
    return messageWriter;
  }



  /**
   * Specifies the listener that should be notified when the client stops
   * running.
   *
   * @param  shutdownListener  The listener that should be notified when the
   *                           client stops running.
   */
  public void setShutdownListener(ClientShutdownListener shutdownListener)
  {
    this.shutdownListener = shutdownListener;
  }



  /**
   * Indicates whether the client should aggregate the data collected from all
   * the threads before sending the results to the SLAMD server.
   *
   * @return  <CODE>true</CODE> if the client should aggregate the thread data
   *          before sending the results to the SLAMD server, or
   *          <CODE>false</CODE> if the data for each thread is to be reported
   *          separately.
   */
  public boolean aggregateThreadData()
  {
    return aggregateThreadData;
  }



  /**
   * Specifies whether the client should aggregate the data collected from all
   * the threads before sending the results to the SLAMD server.
   *
   * @param  aggregateThreadData  Specifies whether the client should aggregate
   *                              the data collected from all the threads before
   *                              sending the results to the SLAMD server.
   */
  public void aggregateThreadData(boolean aggregateThreadData)
  {
    this.aggregateThreadData = aggregateThreadData;
  }



  /**
   * Indicates whether the connection between the client and the server is using
   * SSL.
   *
   * @return  <CODE>true</CODE> if the communication between the client and the
   *          server is using SSL, or <CODE>false</CODE> if not.
   */
  public boolean usingSSL()
  {
    return useSSL;
  }



  /**
   * Retrieves the type of authentication that the client used.
   *
   * @return  The type of authentication that the client used.
   */
  public int getAuthType()
  {
    return authType;
  }



  /**
   * Retrieves the port on which the SLAMD client is communicating with the
   * SLAMD server.
   *
   * @return  The port on which the client is communicating with the SLAMD
   *          server.
   */
  public int getServerPort()
  {
    return serverPort;
  }



  /**
   * Retrieves the ID that the client used when authenticating to the server.
   *
   * @return  The ID that the client used when authenticating to the server.
   */
  public String getAuthenticationID()
  {
    return authID;
  }



  /**
   * Retrieves the path to which Java class files sent by the server will be
   * written.
   *
   * @return  The path to which Java class files sent by the server will be
   *          written.
   */
  public String getClassPath()
  {
    return classPath;
  }



  /**
   * Retrieves the user-friendly client ID that should show up in log messages.
   *
   * @return  The user-friendly client ID that should show up in log messages.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Retrieves the address of the SLAMD server to which this client is
   * connected.
   *
   * @return  The address of the SLAMD server to which this client is connected.
   */
  public String getServerAddress()
  {
    return serverAddress;
  }



  /**
   * Retrieves the location of the SSL key store that this client is using.
   *
   * @return  The location of the SSL key store that this client is using.
   */
  public String getSSLKeyStore()
  {
    return sslKeyStore;
  }



  /**
   * Retrieves the location of the SSL trust store that this client is using.
   *
   * @return  The location of the SSL trust store that this client is using.
   */
  public String getSSLTrustStore()
  {
    return sslTrustStore;
  }



  /**
   * Retrieves the reference to the thread used for persisting statistical data
   * on disk.
   *
   * @return  The reference to the thread used for persisting statistical data
   *          on disk, or <CODE>null</CODE> if client-side persistence is not
   *          enabled.
   */
  public static StatPersistenceThread getStatPersistenceThread()
  {
    if (persistStatistics)
    {
      return statPersistenceThread;
    }
    else
    {
      return null;
    }
  }



  /**
   * Registers the provided stat tracker with the persistence thread so that its
   * data will be periodically persisted on disk.  This will have no effect if
   * persistence is not enabled.
   *
   * @param  tracker  The stat tracker to register with the persistence thread.
   */
  public static void registerPersistentStatistic(StatTracker tracker)
  {
    if (persistStatistics)
    {
      statPersistenceThread.registerTracker(tracker);
    }
  }



  /**
   * Loop infinitely waiting for a request to come in.  When a new request does
   * arrive, handle it appropriately and wait for the next request.  The process
   * of handling the request should send any appropriate responses and may
   * involve interacting with client threads.
   */
  @Override()
  public void run()
  {
    writeMessage("Ready to accept new job requests.");
    hasDisconnected = false;


    while (! disconnectRequested)
    {
      ASN1Element element = null;
      try
      {
        element = reader.readElement();
      }
      catch (InterruptedIOException iioe)
      {
        // We can ignore this -- we just got a timeout while waiting for a
        // message.
        continue;
      }
      catch (IOException ioe)
      {
        writeMessage("I/O Exception in handleRequests:  " + ioe);
        break;
      }
      catch (ASN1Exception ae)
      {
        writeMessage("ASN.1 parse exception in handleRequests:  " + ae);
        continue;
      }


      Message message = null;
      try
      {
        message = Message.decode(element);
      }
      catch (SLAMDException se)
      {
        writeMessage("Unable to convert element to SLAMD message");
        se.printStackTrace();
        continue;
      }


      if (message instanceof JobControlRequestMessage)
      {
        // Received a job control request.  Perform any appropriate work and
        // send the job control response.
        JobControlRequestMessage jobControlRequest = (JobControlRequestMessage)
                                                     message;

        int requestType = jobControlRequest.getJobControlOperation();
        switch (requestType)
        {
          case Constants.JOB_CONTROL_TYPE_START:
            writeMessage("Received a request to start processing.");
            break;
          case Constants.JOB_CONTROL_TYPE_STOP:
          case Constants.JOB_CONTROL_TYPE_STOP_AND_WAIT:
            writeMessage("Received a request to stop processing.");
            break;
          case Constants.JOB_CONTROL_TYPE_STOP_DUE_TO_SHUTDOWN:
            writeMessage("Received a request to stop processing due to " +
                         "SLAMD server shutdown.");
            break;
          default:
            writeMessage("Received an unknown job control request type:  " +
                         requestType);
        }

        if (messageWriter.usingVerboseMode())
        {
          writeVerbose("Received a job control request");
          writeVerbose(jobControlRequest.toString());
        }


        try
        {
          handleJobControlRequest(jobControlRequest);
        }
        catch (Exception e)
        {
          JobControlResponseMessage response =
            new JobControlResponseMessage(jobControlRequest.getMessageID(),
                     jobControlRequest.getJobID(),
                     Constants.MESSAGE_RESPONSE_LOCAL_ERROR,
                     "Unable to handle job control request:  " + e);

          try
          {
            writer.writeElement(response.encode());
          }
          catch (IOException ioe)
          {
            writeMessage("Unable to send job control failure response:  " +
                         ioe);
            ioe.printStackTrace();
          }
        }
      }
      else if (message instanceof JobRequestMessage)
      {
        // Received a new job request.  Create a new job and send the job
        // response message.
        JobRequestMessage jobRequest = (JobRequestMessage) message;
        writeMessage("Received a request to process job " +
                     jobRequest.getJobID());

        if (messageWriter.usingVerboseMode())
        {
          writeVerbose("Received a job request");
          writeVerbose(jobRequest.toString());
        }
        try
        {
          handleJobRequest(jobRequest);
        }
        catch (Exception e)
        {
          JobResponseMessage response =
               new JobResponseMessage(message.getMessageID(),
                        jobRequest.getJobID(),
                        Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE,
                        "Unable to handle job request:  " + e);

          try
          {
            writer.writeElement(response.encode());
          }
          catch (IOException ioe)
          {
            writeMessage("Unable to send job failure response:  " + ioe);
            ioe.printStackTrace();
          }
        }
      }
      else if (message instanceof ClassTransferResponseMessage)
      {
        // Received a class transfer response.  Decode it and if it contains
        // the requested class, write it to the appropriate location in the
        // class path.
        ClassTransferResponseMessage transferResponse =
             (ClassTransferResponseMessage) message;
        if (messageWriter.usingVerboseMode())
        {
          writeVerbose("Received a class transfer response");
          writeVerbose(transferResponse.toString());
        }

        if ((transferResponse.getResponseCode() ==
             Constants.MESSAGE_RESPONSE_SUCCESS) &&
            (transferResponse.getClassData().length > 0) && (classPath != null))
        {
          // Get the file path separator for this platform.
          char separator;
          try
          {
            separator = System.getProperty("file.separator").charAt(0);
          }
          catch (Exception e)
          {
            separator = '/';
          }

          // Convert the class name to a file name.
          String classFile = transferResponse.getClassName();
          String filename  = classPath + separator +
                             classFile.replace('.', separator) + ".class";

          // Make sure that all the necessary directories exist on the client.
          int lastSlashPos = filename.lastIndexOf(separator);
          if (lastSlashPos > 0)
          {
            try
            {
              File parentDir = new File(filename.substring(0, lastSlashPos));
              parentDir.mkdirs();
            } catch (Exception e) {}
          }

          // Try to write the file data to disk.
          try
          {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(transferResponse.getClassData());
            baos.writeTo(new FileOutputStream(filename, false));
            writeVerbose("Wrote class file " + filename);
          }
          catch (Exception e)
          {
            writeMessage("Unable to write class file " + filename + ":  " +
                         e);
          }
        }
      }
      else if (message instanceof KeepAliveMessage)
      {
        // Received a keepalive message.  Ignore it.
        if (messageWriter.usingVerboseMode())
        {
          writeVerbose("Received a keepalive message");
          writeVerbose(message.toString());
        }
      }
      else if (message instanceof StatusRequestMessage)
      {
        // Received a status request message.  Send the appropriate status
        // response.
        if (messageWriter.usingVerboseMode())
        {
          writeVerbose("Received a status request message");
          writeVerbose(message.toString());
        }
        handleStatusRequestMessage((StatusRequestMessage) message);
      }
      else if (message instanceof ServerShutdownMessage)
      {
        // Received a server shutdown message.  We're done.
        writeMessage("Received a server shutdown message.");
        writeVerbose(message.toString());
        if (jobInProgress != null)
        {
          jobInProgress.stopAndWait(Constants.JOB_STATE_STOPPED_BY_SHUTDOWN);
        }

        if (statReporter != null)
        {
          statReporter.stopRunning();
        }

        try
        {
          clientSocket.close();
        } catch (Exception e) {}
        hasDisconnected = true;

        if (shutdownListener != null)
        {
          shutdownListener.clientDisconnected();
        }
        return;
      }
      else if (message instanceof Message)
      {
        // We got a valid SLAMD message, but it's not one that we should
        // handle.  What do we do?
        writeMessage("Received an inappropriate SLAMD message of " +
                     "type " + message.getClass().getName());
      }
      else
      {
        // We didn't get a valid SLAMD message.  What do we do?
        writeMessage("Received an inappropriate non-SLAMD message of " +
                     "type " + message.getClass().getName());
      }
    }

    // If we've gotten here, then the client could have gotten a request to
    // disconnect.  If a job is in progress, then stop it.
    if (jobInProgress != null)
    {
      jobInProgress.stopAndWait(Constants.JOB_STATE_CANCELLED);
      while (jobInProgress != null)
      {
        try
        {
          Thread.sleep(100);
        } catch (Exception e) {}
      }
    }

    try
    {
      clientSocket.close();
    } catch (Exception e) {}

    try
    {
      statReporter.stopRunning();
    } catch (Exception e) {}

    hasDisconnected = true;

    if (shutdownListener != null)
    {
      shutdownListener.clientDisconnected();
    }
  }



  /**
   * Performs any work requested by the provide job control request and sends
   * the appropriate response.
   *
   * @param  request  The job control request to be processed.
   */
  public void handleJobControlRequest(JobControlRequestMessage request)
  {
    // These are the values that we will include in the job control response
    int    responseCode = -1;
    String responseMsg  = "";


    // Any response we send back needs to have the same message ID as the
    // request, so get that now.
    int messageID = request.getMessageID();


    // Make sure that we're actually working on the specified job.
    String jobID = request.getJobID();
    if ((jobInProgress == null) || (! jobInProgress.getJobID().equals(jobID)))
    {
      responseCode = Constants.MESSAGE_RESPONSE_NO_SUCH_JOB;
      responseMsg  = "Unknown job:  " + jobID;
    }
    else
    {
      // See what kind of job control request this is.  Currently, we only
      // support "start", "stop", and "stop and wait" requests.
      int controlType = request.getJobControlOperation();
      switch (controlType)
      {
        case Constants.JOB_CONTROL_TYPE_START:
          // Try to start the job
          stopRequested = false;
          responseCode  = startJob();
          switch (responseCode)
          {
            case Constants.MESSAGE_RESPONSE_CLASS_NOT_FOUND:
            case Constants.MESSAGE_RESPONSE_CLASS_NOT_VALID:
            case Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE:
              String[] logMessages = jobInProgress.getLogMessages();
              if ((logMessages != null) && (logMessages.length > 0))
              {
                responseMsg = "Unable to start job.  Messages logged were:  " +
                              '"' + logMessages[0] + '"';
                for (int i=1; i < logMessages.length; i++)
                {
                  responseMsg += ", \"" + logMessages[i] + '"';
                }
              }

              if (persistStatistics)
              {
                try
                {
                  statPersistenceThread.jobDone();
                }
                catch (Exception e)
                {
                  responseMsg += "; unable to save persistence data:  " + e;
                }
              }

              jobInProgress = null;
          }
          break;
        case Constants.JOB_CONTROL_TYPE_STOP:
          // Signal the job to stop running, but don't wait for it to actually
          // stop before returning.  If a stop request has already been received
          // for the current job, then try to forcefully stop the job.
          if ((stopRequested) && (jobInProgress != null))
          {
            jobInProgress.forcefullyStop(Constants.JOB_STATE_STOPPED_BY_USER);
            responseCode = Constants.MESSAGE_RESPONSE_SUCCESS;
          }
          else
          {
            stopRequested = true;
            responseCode = stopJob(Constants.JOB_STATE_STOPPED_BY_USER,
                                   false);
          }
          break;
        case Constants.JOB_CONTROL_TYPE_STOP_AND_WAIT:
          // Signal the job to stop running and make sure that it is stopped
          // before returning.  If a stop request has already been received
          // for the current job, the try to forcefully stop the job.
          responseCode = stopJob(Constants.JOB_STATE_STOPPED_BY_USER,
                                 true);
          break;
        case Constants.JOB_CONTROL_TYPE_STOP_DUE_TO_SHUTDOWN:
          // Signal the job to stop running and make sure that it is stopped
          // before returning
          if ((stopRequested) && (jobInProgress != null))
          {
            jobInProgress.forcefullyStop(Constants.JOB_STATE_STOPPED_BY_USER);
            responseCode = Constants.MESSAGE_RESPONSE_SUCCESS;
          }
          else
          {
            stopRequested = true;
            responseCode = stopJob(Constants.JOB_STATE_STOPPED_BY_SHUTDOWN,
                                   true);
          }
          break;
        default:
          responseCode =
               Constants.MESSAGE_RESPONSE_UNSUPPORTED_CONTROL_TYPE;
          responseMsg  = "Unknown job control type:  " + controlType;
      }
    }


    // Send back the appropriate job control response message
    JobControlResponseMessage response =
         new JobControlResponseMessage(messageID, jobID, responseCode,
                                       responseMsg);
    try
    {
      writer.writeElement(response.encode());
      if (messageWriter.usingVerboseMode())
      {
        writeVerbose("Sent job control response message");
        writeVerbose(response.toString());
      }
    }
    catch (IOException ioe)
    {
      writeMessage("Unable to send job control response message:  " +ioe);
      writeMessage(response.toString());
    }
  }



  /**
   * Configures the client to start processing the new job specified in the
   * job request and sends the appropriate response.
   *
   * @param  request  The job request to be processed.
   */
  public void handleJobRequest(JobRequestMessage request)
  {
    JobResponseMessage response = null;
    String jobID = request.getJobID();


    // If there is already a job defined, then we won't process another one.
    // Otherwise, we'll accept it.
    if (jobInProgress == null)
    {
      String jobClass = request.getJobClass();
      int threadsPerClient = request.getThreadsPerClient();
      Date startTime = request.getStartTime();
      Date stopTime = request.getStopTime();
      int clientNumber = request.getClientNumber();
      int duration = request.getDuration();
      int collectionInterval = request.getCollectionInterval();
      int threadStartupDelay = request.getThreadStartupDelay();
      ParameterList parameters = request.getParameters();

      // Deal with the time skew if appropriate.
      if (serverTimeOffset != 0)
      {
        startTime = new Date(startTime.getTime() - serverTimeOffset);

        if (stopTime != null)
        {
          stopTime = new Date(stopTime.getTime() - serverTimeOffset);
        }
      }

      try
      {
        jobInProgress = new ClientSideJob(this, jobID, jobClass,
                                          threadsPerClient, startTime, stopTime,
                                          clientNumber, duration,
                                          collectionInterval,
                                          threadStartupDelay, parameters,
                                          useCustomClassLoader,
                                          enableRealTimeStats, statReporter);
        response = new JobResponseMessage(request.getMessageID(), jobID,
                            Constants.MESSAGE_RESPONSE_SUCCESS,
                            "Accepted job " + jobID + " for processing");
      }
      catch (Exception e)
      {
        response =
             new JobResponseMessage(request.getMessageID(), jobID,
                      Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE,
                      "Unable to create the client-side job:  " +
                      JobClass.stackTraceToString(e));
      }

    }
    else
    {
      response = new JobResponseMessage(request.getMessageID(), jobID,
                          Constants.MESSAGE_RESPONSE_JOB_REQUEST_REFUSED,
                          "Already accepted job " + jobInProgress.getJobID());
    }


    try
    {
      writer.writeElement(response.encode());
      if (messageWriter.usingVerboseMode())
      {
        writeVerbose("Sent job response message");
        writeVerbose(response.toString());
      }
    }
    catch (IOException ioe)
    {
      writeMessage("Unable to send job response message:  " +ioe);
      writeMessage(response.toString());
    }
  }



  /**
   * Handles the provided status request by gathering the requested information
   * and sending it back to the server.
   *
   * @param  request  The status request to be processed.
   */
  public void handleStatusRequestMessage(StatusRequestMessage request)
  {
    StatusResponseMessage response = null;
    String clientStateStr = "";
    switch (clientState)
    {
      case Constants.CLIENT_STATE_IDLE:
           clientStateStr = "Ready to accept jobs";
           break;
      case Constants.CLIENT_STATE_JOB_NOT_YET_STARTED:
           clientStateStr = "Job " + jobInProgress.getJobID() +
                            " defined but not yet started";
           break;
      case Constants.CLIENT_STATE_RUNNING_JOB:
           clientStateStr = "Running job " + jobInProgress.getJobID();
           break;
      case Constants.CLIENT_STATE_SHUTTING_DOWN:
           clientStateStr = "Client is shutting down";
           break;
    }

    String jobID = request.getJobID();
    if ((jobID == null) || (jobID.length() == 0))
    {
      // This is a general client health request -- no need to include job
      // information
      response = new StatusResponseMessage(request.getMessageID(),
                          Constants.MESSAGE_RESPONSE_SUCCESS,
                          clientState, clientStateStr);
    }
    else
    {
      // We should also include job-specific information
      if ((jobInProgress != null) && jobID.equals(jobInProgress.getJobID()))
      {
        response = new StatusResponseMessage(request.getMessageID(),
                            Constants.MESSAGE_RESPONSE_SUCCESS,
                            clientState, clientStateStr, jobID,
                            jobInProgress.getJobState(),
                            jobInProgress.getStatTrackers(aggregateThreadData));
      }
      else
      {
        int jobState = Constants.JOB_STATE_NO_SUCH_JOB;
        response = new StatusResponseMessage(request.getMessageID(),
                            Constants.MESSAGE_RESPONSE_NO_SUCH_JOB,
                            clientState, clientStateStr, jobID, jobState,
                            new StatTracker[0]);
      }
    }


    try
    {
      writer.writeElement(response.encode());
      if (messageWriter.usingVerboseMode())
      {
        writeVerbose("Sent status response message");
        writeVerbose(response.toString());
      }
    }
    catch (IOException ioe)
    {
      writeMessage("Unable to send status response message:  " +ioe);
      writeMessage(response.toString());
    }
  }



  /**
   * Sends the requested message to the SLAMD server.  The response will be
   * handled asynchronously, so there is no way for something that calls this
   * method to be immediately notified of any response.
   *
   * @param  message  The message to send to the SLAMD server.
   *
   * @throws  IOException  If a problem occurs while sending the message to the
   *                       SLAMD server.
   */
  public void sendMessage(Message message)
         throws IOException
  {
    writer.writeElement(message.encode());
  }



  /**
   * Attempts to start the current job.
   *
   * @return  The response code associated with the attempt.
   */
  public int startJob()
  {
    return jobInProgress.start();
  }



  /**
   * Attempts to stop the current job.
   *
   * @param  stopReason   The reason that the job is to be stopped.
   * @param  waitForStop  Indicates whether this method should wait for the job
   *                      to actually stop before returning, or if it should
   *                      return as soon as the stop signal has been issued.
   *
   * @return  The result code from the stop operation.
   */
  public int stopJob(int stopReason, boolean waitForStop)
  {
    if (waitForStop)
    {
      return jobInProgress.stopAndWait(stopReason);
    }
    else
    {
      return jobInProgress.stop(stopReason);
    }
  }



  /**
   * Used by the job to indicate to the client that it has completed processing
   * and that the client should gather all the appropriate summary information
   * and send it back to the server.
   */
  public void jobDone()
  {
    if (jobInProgress == null)
    {
      // This should never happen, but just in case....
      writeMessage("Job done notification received, but no job");
      return;
    }


    if (jobInProgress.isDone())
    {
      // Make sure that the final results are persisted if appropriate.
      if (persistStatistics)
      {
        try
        {
          statPersistenceThread.jobDone();
        }
        catch (Exception e)
        {
          jobInProgress.logMessage("Job Done Thread",
                                   "Unable to persist statistical data:  " + e);
        }
      }


      // The job really is done, so send back a job completed message
      String jobID           = jobInProgress.getJobID();
      int    jobState        = jobInProgress.getJobState();
      long   actualStartTime = jobInProgress.getActualStartTime();
      long   actualStopTime  = jobInProgress.getActualStopTime();
      int    actualDuration  = jobInProgress.getActualDuration();

      if (serverTimeOffset != 0)
      {
        actualStartTime += serverTimeOffset;
        actualStopTime  += serverTimeOffset;
      }

      StatTracker[] statTrackers =
           jobInProgress.getStatTrackers(aggregateThreadData);
      String[] logMessages = jobInProgress.getLogMessages();

      JobCompletedMessage msg = new JobCompletedMessage(getMessageID(),
                                                        jobID, jobState,
                                                        actualStartTime,
                                                        actualStopTime,
                                                        actualDuration,
                                                        statTrackers,
                                                        logMessages);


      try
      {
        writeMessage("Done processing job " + jobInProgress.getJobID());
        writer.writeElement(msg.encode());
        if (messageWriter.usingVerboseMode())
        {
          writeVerbose("Sent a job completed message");
          writeVerbose(msg.toString());
        }
      }
      catch (IOException ioe)
      {
        writeMessage("Unable to send job completed message:  " +ioe);
        writeMessage(msg.toString());
      }


      // Make sure to remove the reference to the job on the client so we won't
      // reject any additional requests
      jobInProgress = null;
    }
    else
    {
      // This should not happen either
      writeMessage("Job done notification received, but job is not done");
    }
  }



  /**
   * Retrieves the message ID that the client should use for the next message
   * that will be sent to the SLAMD server.  The client will use even-numbered
   * message IDs for messages that it originates and the server will use
   * odd-numbered message IDs for messages that it originates.  Messages sent in
   * response to a previous request will use the same message ID as the request.
   *
   * @return  The message ID that the client should use for the next message
   *          that will be sent to the SLAMD server.
   */
  public synchronized int getMessageID()
  {
    int returnID = messageID;
    messageID += 2;
    return returnID;
  }



  /**
   * Writes a message that may be seen by clients.
   *
   * @param  message  The message to be written.
   */
  public void writeMessage(String message)
  {
    messageWriter.writeMessage(message);
  }



  /**
   * Writes a verbose message that may be seen by clients if they have enabled
   * verbose messages.
   *
   * @param  message  The message to be written.
   */
  public void writeVerbose(String message)
  {
    messageWriter.writeVerbose(message);
  }
}

