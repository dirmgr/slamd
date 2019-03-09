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
package com.slamd.clientmanager;



import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import javax.net.ssl.SSLSocketFactory;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.client.Client;
import com.slamd.client.ClientMessageWriter;
import com.slamd.common.Constants;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.message.ClientManagerHelloMessage;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.Message;
import com.slamd.message.ServerShutdownMessage;
import com.slamd.message.StartClientRequestMessage;
import com.slamd.message.StartClientResponseMessage;
import com.slamd.message.StopClientRequestMessage;
import com.slamd.message.StopClientResponseMessage;



/**
 * This class defines a client manager that will manage the process of starting
 * clients independent of the SLAMD server itself.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientManager
{
  /**
   * The maximum length of time in milliseconds that any part of this client
   * manager should block at any one time.
   */
  public static final int MAX_BLOCK_TIME = 5000;



  /**
   * The length of time between checks to determine whether the SLAMD server is
   * available if it was detected that it has been taken offline.
   */
  public static final int SERVER_DOWN_CHECK_TIME = 30000;



  // The set of processes associated with this client manager.
  ArrayList<Process> processList;

  // The ASN.1 reader used to read messages from the server.
  ASN1Reader asn1Reader;

  // The ASN.1 writer used to write messages to the server.
  ASN1Writer asn1Writer;

  // Indicates whether the client manager should blindly trust any SSL
  // certificate presented by the SLAMD server.
  boolean blindTrust;

  // Indicates whether the client manager is currently connected to the SLAMD
  // server.
  boolean connected;

  // Indicates whether the client manager should be stopped.
  boolean stopClientManager;

  // Indicates whether the client manager should use SSL to communicate with
  // the SLAMD server.
  boolean useSSL;

  // The buffer used to read data from the client processes.
  byte[] readBuffer;

  // The client message writer that will be used for writing messages about the
  // progress of the client manager.
  ClientMessageWriter messageWriter;

  // The number of client instances to automatically create upon connecting to
  // the SLAMD server.
  int autoCreateClients;

  // The maximum number of clients that may be created on this system.
  int maxClients;

  // The ID of the next message that should be sent to the SLAMD server.
  int nextMessageID;

  // The port number for the SLAMD server.
  int serverPort;

  // The socket used to communicate with the SLAMD server.
  Socket socket;

  // The local address the client should use when connecting to the server.
  String clientAddress;

  // The client ID for this client manager.
  String clientID;

  // The address of the SLAMD server.
  String serverAddress;

  // The location of the JSSE key store.
  String sslKeyStore;

  // The password required to access the JSSE key store.
  String sslKeyStorePassword;

  // The location of the JSSE trust store.
  String sslTrustStore;

  // The password required to access the JSSE trust store.
  String sslTrustStorePassword;

  // The command to use to start an instance of the client.
  String startCommand;



  /**
   * Creates a new client manager that is intended to work with the specified
   * SLAMD server.
   *
   * @param  clientID               The client ID that should be used for the
   *                                client.  It may be {@code null} if the
   *                                client ID should be automatically selected.
   * @param  clientAddress          The IP address on the client system from
   *                                which the connection should originate.  It
   *                                may be {@code null} if the client address
   *                                should be automatically selected.
   * @param  serverAddress          The address of the SLAMD server.
   * @param  serverPort             The port number of the SLAMD server.
   * @param  useSSL                 Indicates whether to use SSL to communicate
   *                                with the SLAMD server.
   * @param  blindTrust             Indicates whether to blindly trust any SSL
   *                                certificate presented by the SLAMD server.
   * @param  sslKeyStore            The location of the JSSE key store.
   * @param  sslKeyStorePassword    The password required to access the SSL key
   *                                store.
   * @param  sslTrustStore          The location of the JSSE trust store.
   * @param  sslTrustStorePassword  The password required to access the SSL
   *                                trust store.
   * @param  maxClients             The maximum number of clients that may be
   *                                created on this system.
   * @param  startCommand           The command to execute to start a single
   *                                client instance.
   * @param  messageWriter          The message writer to use for output.
   */
  public ClientManager(String clientID, String clientAddress,
                       String serverAddress, int serverPort, boolean useSSL,
                       boolean blindTrust, String sslKeyStore,
                       String sslKeyStorePassword, String sslTrustStore,
                       String sslTrustStorePassword, int maxClients,
                       String startCommand, ClientMessageWriter messageWriter)
  {
    this.clientAddress         = clientAddress;
    this.serverAddress         = serverAddress;
    this.serverPort            = serverPort;
    this.useSSL                = useSSL;
    this.blindTrust            = blindTrust;
    this.sslKeyStore           = sslKeyStore;
    this.sslKeyStorePassword   = sslKeyStorePassword;
    this.sslTrustStore         = sslTrustStore;
    this.sslTrustStorePassword = sslTrustStorePassword;
    this.maxClients            = maxClients;
    this.startCommand          = startCommand;
    this.messageWriter         = messageWriter;

    autoCreateClients = 0;
    readBuffer        = new byte[1024];
    connected         = false;
    stopClientManager = false;
    nextMessageID     = 1;
    processList       = new ArrayList<Process>();
    socket            = null;
    asn1Reader        = null;
    asn1Writer        = null;

    if ((clientID == null) || (clientID.length() == 0))
    {
      try
      {
        clientID = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException uhe)
      {
        try
        {
          clientID = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e)
        {
          clientID = "unknown";
        }
      }
    }
    this.clientID = clientID;

    if (useSSL && (! blindTrust))
    {
      Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
      System.setProperty("java.protocol.handler.pkgs",
                         "com.sun.net.ssl.internal.www.protocol");
    }

    if ((sslKeyStore != null) && (sslKeyStore.length() > 0))
    {
      System.setProperty(Constants.SSL_KEY_STORE_PROPERTY, sslKeyStore);
    }

    if ((sslKeyStorePassword != null) && (sslKeyStorePassword.length() > 0))
    {
      System.setProperty(Constants.SSL_KEY_PASSWORD_PROPERTY,
                         sslKeyStorePassword);
    }

    if ((sslTrustStore != null) && (sslTrustStore.length() > 0))
    {
      System.setProperty(Constants.SSL_TRUST_STORE_PROPERTY, sslTrustStore);
    }

    if ((sslTrustStorePassword != null) && (sslTrustStorePassword.length() > 0))
    {
      System.setProperty(Constants.SSL_TRUST_PASSWORD_PROPERTY,
                         sslTrustStorePassword);
    }
  }



  /**
   * Specifies the number of client instances to create automatically when
   * establishing a connection to the SLAMD server.
   *
   * @param  autoCreateClients  The number of client instances to create
   *                            automatically when establishing a connection to
   *                            the SLAMD server.
   */
  public void setAutoCreateClients(int autoCreateClients)
  {
    this.autoCreateClients = autoCreateClients;
  }



  /**
   * Establishes a connection to the SLAMD server and then operates in a loop
   * waiting for requests to arrive.
   */
  public void run()
  {
    // Create a variable that we will use for detecting consecutive failures
    // when attempting to read requests from the SLAMD server.
    boolean consecutiveFailures = false;


    // Enter a loop that waits for messages to arrive from the SLAMD server.
    // This needs to handle the SLAMD server shutting down so that it can
    // reconnect to it a short time after it comes back up.
    while (! stopClientManager)
    {
      // See if we need to re-establish the connection to the SLAMD server.  If
      // so, then do it.  If the reconnect attempt fails, then sleep and try
      // again.
      if (! connected)
      {
        // Try to establish the socket to the SLAMD server.  If this fails,
        // then the server is probably offline.  Just sleep for a little bit and
        // try again.
        messageWriter.writeVerbose("Attempting to establish connection to " +
                                   serverAddress + ':' + serverPort);

        if (useSSL)
        {
          if (blindTrust)
          {
            try
            {
              JSSEBlindTrustSocketFactory socketFactory =
                   new JSSEBlindTrustSocketFactory();
              if ((clientAddress == null) || (clientAddress.length() == 0))
              {
                socket = socketFactory.makeSocket(serverAddress, serverPort);
              }
              else
              {
                InetAddress localAddress = InetAddress.getByName(clientAddress);
                socket = socketFactory.createSocket(serverAddress, serverPort,
                                                    localAddress, 0);
              }
              asn1Reader = new ASN1Reader(socket);
              asn1Writer = new ASN1Writer(socket);
              socket.setSoTimeout(MAX_BLOCK_TIME);
            }
            catch (Exception e)
            {
              messageWriter.writeVerbose("SSL blind trust connect attempt " +
                                         "failed:  " + e);
              sleepBeforeReconnectAttempt();
              continue;
            }
          }
          else
          {
            try
            {
              SSLSocketFactory socketFactory =
                   (SSLSocketFactory) SSLSocketFactory.getDefault();
              if ((clientAddress == null) || (clientAddress.length() == 0))
              {
                socket = socketFactory.createSocket(serverAddress, serverPort);
              }
              else
              {
                InetAddress localAddress = InetAddress.getByName(clientAddress);
                socket = socketFactory.createSocket(serverAddress, serverPort,
                                                    localAddress, 0);
              }
              asn1Reader = new ASN1Reader(socket);
              asn1Writer = new ASN1Writer(socket);
              socket.setSoTimeout(MAX_BLOCK_TIME);
            }
            catch (Exception e)
            {
              messageWriter.writeVerbose("SSL connect attempt failed:  " + e);
              sleepBeforeReconnectAttempt();
              continue;
            }
          }
        }
        else
        {
          try
          {
            if ((clientAddress == null) || (clientAddress.length() == 0))
            {
              socket = new Socket(serverAddress, serverPort);
            }
            else
            {
              InetAddress localAddress = InetAddress.getByName(clientAddress);
              socket = new Socket(serverAddress, serverPort, localAddress, 0);
            }
            asn1Reader = new ASN1Reader(socket);
            asn1Writer = new ASN1Writer(socket);
            socket.setSoTimeout(MAX_BLOCK_TIME);
          }
          catch (Exception e)
          {
            messageWriter.writeVerbose("Connect attempt failed:  " + e);
            sleepBeforeReconnectAttempt();
            continue;
          }
        }


        // Send the hello request message to the SLAMD server.
        messageWriter.writeVerbose("Attempting to send a hello request");
        ClientManagerHelloMessage helloMessage =
             new ClientManagerHelloMessage(nextMessageID(),
                                           Client.SLAMD_CLIENT_VERSION,
                                           clientID, maxClients);
        try
        {
          asn1Writer.writeElement(helloMessage.encode());
        }
        catch (Exception e)
        {
          messageWriter.writeVerbose("Hello request attempt failed:  " + e);
          sleepBeforeReconnectAttempt();
          continue;
        }

        // Read the hello response message.
        messageWriter.writeVerbose("Attempting to read the hello response");
        HelloResponseMessage helloResponse = null;
        try
        {
          ASN1Element element =
               asn1Reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
          helloResponse = (HelloResponseMessage) Message.decode(element);
        }
        catch (Exception e)
        {
          messageWriter.writeVerbose("Unable to read hello response:  " + e);
          sleepBeforeReconnectAttempt();
          continue;
        }

        // Interpret the hello response.
        if (helloResponse.getResponseCode() ==
            Constants.MESSAGE_RESPONSE_SUCCESS)
        {
          messageWriter.writeMessage("Successfully connected to the SLAMD " +
                                     "server at " + serverAddress + ':' +
                                     serverPort);
          connected           = true;
          consecutiveFailures = false;

          startClients(autoCreateClients);
        }
        else
        {
          boolean stillAvailable = true;
          switch (helloResponse.getResponseCode())
          {
            case Constants.MESSAGE_RESPONSE_UNKNOWN_AUTH_ID:
            case Constants.MESSAGE_RESPONSE_INVALID_CREDENTIALS:
            case Constants.MESSAGE_RESPONSE_UNSUPPORTED_AUTH_TYPE:
            case Constants.MESSAGE_RESPONSE_UNSUPPORTED_CLIENT_VERSION:
            case Constants.MESSAGE_RESPONSE_UNSUPPORTED_SERVER_VERSION:
            case Constants.MESSAGE_RESPONSE_CLIENT_REJECTED:
              stillAvailable = false;
              break;
          }

          messageWriter.writeMessage("Hello request rejected by SLAMD server " +
                                     "-- result code " +
                                     helloResponse.getResponseCode() +
                                     ", response message \"" +
                                     helloResponse.getResponseMessage() + '"');
          if (stillAvailable)
          {
            sleepBeforeReconnectAttempt();
          }
          else
          {
            messageWriter.writeMessage("This client manager will shut down.");
            stopClientManager = true;
          }

          continue;
        }
      }


      // See if the SLAMD server has issued a request to the client manager.
      ASN1Element element = null;
      try
      {
        element = asn1Reader.readElement();
        if (element == null)
        {
          messageWriter.writeMessage("Client manager connection closed by " +
               "the SLAMD server");
          connected = false;
          for (final Process p : processList)
          {
            try
            {
              p.destroy();
            } catch (final Exception e) {}
          }
          processList.clear();
          continue;
        }

        Message message = Message.decode(element);

        if (message instanceof StartClientRequestMessage)
        {
          messageWriter.writeMessage("Received a StartClient request");
          messageWriter.writeVerbose(message.toString());
          startClients((StartClientRequestMessage) message);
        }
        else if (message instanceof StopClientRequestMessage)
        {
          messageWriter.writeMessage("Received a StopClient request");
          messageWriter.writeVerbose(message.toString());
          stopClients((StopClientRequestMessage) message);
        }
        else if (message instanceof ServerShutdownMessage)
        {
          messageWriter.writeMessage("Received a ServerShutdown notification");
          messageWriter.writeVerbose(message.toString());
          disconnect();
          sleepBeforeReconnectAttempt();
        }
        else
        {
          messageWriter.writeMessage("Unsupported message type received:  " +
                                     message.getClass().getName());
        }

        consecutiveFailures = false;
      }
      catch (InterruptedIOException iioe)
      {
        // This just means that the read attempt timed out.  No big deal, and no
        // need to log anything.
      }
      catch (Exception e)
      {
        // This is more significant.  It could mean that there is a problem
        // decoding the message received (for which there will be no tolerance
        // and the connection will be terminated immediately, since it's likely
        // that we wouldn't know where to start looking for the next element),
        // that an I/O problem occurred (in which case the connection will be
        // closed if this is the second such failure in so many attempts), or
        // some other unforeseen problem occurred (which will just be ignored).
        if (e instanceof ASN1Exception)
        {
          messageWriter.writeMessage("Disconnecting from the SLAMD server " +
                                     "due to an ASN.1 decoding problem:   " +
                                     e);
          disconnect();
        }
        else if (e instanceof IOException)
        {
          if (consecutiveFailures)
          {
            messageWriter.writeMessage("Disconnecting from the SLAMD server " +
                                       "due to consecutive I/O failures:   " +
                                       e);
            disconnect();
          }
          else
          {
            consecutiveFailures = true;
          }
        }
        else
        {
          messageWriter.writeVerbose("Ignoring uncaught exception:  " + e);
          e.printStackTrace();
        }
      }


      // Iterate through all the clients.  Make sure they are still connected
      // and read anything they may have output.
      Iterator iterator = processList.iterator();
      while (iterator.hasNext())
      {
        Process process = (Process) iterator.next();

        // First, see if the process has exited.
        try
        {
          int exitValue = process.exitValue();
          messageWriter.writeMessage("Client exited with exit code " +
                                     exitValue);
          iterator.remove();
          continue;
        } catch (IllegalThreadStateException itse) {}


        // Read any output that may be available on the process.
        try
        {
          InputStream inputStream = process.getInputStream();
          while (inputStream.available() > 0)
          {
            inputStream.read(readBuffer);
          }
        }
        catch (Exception e)
        {
          messageWriter.writeVerbose("Error reading client output:   " + e);
        }
      }
    }
  }



  /**
   * Retrieves the next message ID that should be used to send a message to the
   * SLAMD server.
   *
   * @return  The next message ID that should be used to send a message to the
   *          SLAMD server.
   */
  public int nextMessageID()
  {
    int idToReturn = nextMessageID;
    nextMessageID += 2;
    return idToReturn;
  }



  /**
   * Attempts to start the specified number of clients, presumably as a result
   * of a start client request from the SLAMD server.  In general, this should
   * only be called when automatically creating client instances when the client
   * manager connects to the SLAMD server.
   *
   * @param  numClients  The number of instances of the client to create.
   */
  public void startClients(int numClients)
  {
    if ((maxClients > 0) && ((numClients + processList.size()) > maxClients))
    {
      messageWriter.writeMessage("Rejecting the StartClient request -- " +
                                 "insufficient clients available");
      return;
    }

    Runtime runtime = Runtime.getRuntime();
    for (int i=0; i < numClients; i++)
    {
      try
      {
        Process process = runtime.exec(startCommand);
        processList.add(process);
      }
      catch (Exception e)
      {
        messageWriter.writeMessage("Rejecting the StartClient request -- " +
                                   "unable to execute start client command");
        return;
      }
    }
  }



  /**
   * Attempts to start the specified number of clients, presumably as a result
   * of a start client request from the SLAMD server.
   *
   * @param  startClientRequest  The start client request to be processed.
   */
  public void startClients(StartClientRequestMessage startClientRequest)
  {
    int numClients = startClientRequest.getNumClients();
    if ((maxClients > 0) && ((numClients + processList.size()) > maxClients))
    {
      StartClientResponseMessage responseMessage =
        new StartClientResponseMessage(startClientRequest.getMessageID(),
                 Constants.MESSAGE_RESPONSE_INSUFFICIENT_CLIENTS,
                 "Insufficient clients are available to process the request.");
      messageWriter.writeMessage("Rejecting the StartClient request -- " +
                                 "insufficient clients available");
      messageWriter.writeVerbose(responseMessage.toString());

      try
      {
        asn1Writer.writeElement(responseMessage.encode());
      } catch (Exception e) {}
      return;
    }


    Runtime runtime = Runtime.getRuntime();
    for (int i=0; i < numClients; i++)
    {
      try
      {
        Process process = runtime.exec(startCommand);
        processList.add(process);
      }
      catch (Exception e)
      {
        StartClientResponseMessage responseMessage =
          new StartClientResponseMessage(startClientRequest.getMessageID(),
                   Constants.MESSAGE_RESPONSE_LOCAL_ERROR,
                   "Unable to execute the start client command:  " + e);
        messageWriter.writeMessage("Rejecting the StartClient request -- " +
                                   "unable to execute start client command");
        messageWriter.writeVerbose(responseMessage.toString());

        try
        {
          asn1Writer.writeElement(responseMessage.encode());
        } catch (Exception e2) {}
        return;
      }
    }


    StartClientResponseMessage responseMessage =
      new StartClientResponseMessage(startClientRequest.getMessageID(),
                                     Constants.MESSAGE_RESPONSE_SUCCESS);
    messageWriter.writeVerbose(responseMessage.toString());
    try
    {
      asn1Writer.writeElement(responseMessage.encode());
    } catch (Exception e) {}
  }



  /**
   * Attempts to stop the specified number of clients, presumably as a result of
   * a stop client request from the SLAMD server.
   *
   * @param  stopClientRequest  The request received from the server.
   */
  public void stopClients(StopClientRequestMessage stopClientRequest)
  {
    int numClients;
    if (stopClientRequest == null)
    {
      // This should never happen.
      numClients = processList.size();
    }
    else
    {
      numClients = stopClientRequest.getNumClients();
      if (numClients < 0)
      {
        numClients = processList.size();
      }
    }

    int      numKilled = 0;
    Iterator iterator  = processList.iterator();
    while ((numKilled < numClients) && iterator.hasNext())
    {
      Process process = (Process) iterator.next();
      try
      {
        process.destroy();
      } catch (Exception e) { e.printStackTrace(); }

      numKilled++;
    }

    if (stopClientRequest == null)
    {
      return;
    }

    StopClientResponseMessage response =
         new StopClientResponseMessage(stopClientRequest.getMessageID(),
                                       Constants.MESSAGE_RESPONSE_SUCCESS,
                                       numKilled + " clients stopped");
    messageWriter.writeVerbose(response.toString());
    try
    {
      asn1Writer.writeElement(response.encode());
    } catch (Exception e) {}
  }



  /**
   * Closes the connection to the SLAMD server and disconnects any clients that
   * may be connected.
   */
  public void disconnect()
  {
    messageWriter.writeMessage("Disconnecting from the SLAMD server");
    for (int i=0; i < processList.size(); i++)
    {
      Process process = processList.get(i);

      try
      {
        process.destroy();
      } catch (Exception e) {}
    }
    processList.clear();

    try
    {
      socket.close();
    } catch (Exception e2) {}
    connected = false;
  }



  /**
   * Allows the client manager to sleep for a period of time before it checks to
   * see if the SLAMD server is available.  It will generally sleep for a time
   * equal to <CODE>SERVER_DOWN_CHECK_TIME</CODE>, but it can stop sooner if the
   * client manager is asked to shut down.
   */
  public void sleepBeforeReconnectAttempt()
  {
    long now        = System.currentTimeMillis();
    long wakeUpTime = now + SERVER_DOWN_CHECK_TIME;

    while ((! stopClientManager) && (System.currentTimeMillis() < wakeUpTime))
    {
      try
      {
        Thread.sleep(MAX_BLOCK_TIME);
      } catch (InterruptedException ie) {}
    }
  }
}

