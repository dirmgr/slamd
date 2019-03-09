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
package com.slamd.tools.ldapdecoder;



import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;
import javax.net.ssl.SSLSocketFactory;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.tools.ldapdecoder.protocol.LDAPMessage;



/**
 * This class defines a thread that may be used for handling all communication
 * with the directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPDecoderServerThread
       extends Thread
{
  // The ASN.1 reader that may be used to read responses from the directory
  // server.
  private ASN1Reader serverReader;

  // The ASN.1 writer that may be used to send requests to the directory server.
  private ASN1Writer serverWriter;

  // Indicates whether a request has been made to close the connection to the
  // server.
  private boolean closeRequested;

  // Indicates whether the decoded information should be written to a SLAMD job
  // script.
  private boolean writeJobScript;

  // The client connection with which this server thread is associated.
  private LDAPClientConnection clientConnection;

  // The LDAP decoder that created this server thread.
  private LDAPDecoder decoder;

  // The output writer that will be used to write information to the SLAMD job
  // script that is being generated.
  private PrintStream scriptWriter;

  // The socket that will be used for communicating with the directory server.
  private Socket serverSocket;



  /**
   * Creates a new thread for handling all interaction with the directory
   * server.
   *
   * @param  decoder           The LDAP decoder with which this server thread is
   *                           associated.
   * @param  clientConnection  The client connection with which this server
   *                           thread is associated.
   *
   * @throws  LDAPDecoderException  If a problem occurs while initializing the
   *                                thread.
   */
  public LDAPDecoderServerThread(LDAPDecoder decoder,
                                 LDAPClientConnection clientConnection)
         throws LDAPDecoderException
  {
    this.decoder          = decoder;
    this.clientConnection = clientConnection;

    setName("Connection " +
            clientConnection.clientSocket.getInetAddress().getHostAddress() +
            ':' + clientConnection.clientSocket.getPort() + " server thread");

    writeJobScript = decoder.writeJobScript;
    scriptWriter   = decoder.scriptWriter;


    // Establish the connection to the server.
    if (decoder.useSSLForServer)
    {
      try
      {
        SSLSocketFactory socketFactory =
             (SSLSocketFactory) SSLSocketFactory.getDefault();
        serverSocket = socketFactory.createSocket(decoder.serverAddress,
                                                  decoder.serverPort);
        serverReader = new ASN1Reader(serverSocket);
        serverWriter = new ASN1Writer(serverSocket);
        serverSocket.setTcpNoDelay(true);
      }
      catch (Exception e)
      {
        throw new LDAPDecoderException("Unable to establish SSL-based " +
                                       "connection to the directory server", e);
      }
    }
    else
    {
      try
      {
        serverSocket = new Socket(decoder.serverAddress, decoder.serverPort);
        serverReader = new ASN1Reader(serverSocket);
        serverWriter = new ASN1Writer(serverSocket);
        serverSocket.setTcpNoDelay(true);
      }
      catch (Exception e)
      {
        throw new LDAPDecoderException("Unable to establish a connection" +
                                       "to the directory server", e);
      }
    }


    closeRequested = false;
  }



  /**
   * Loops, reading information from the server, decoding it, and passing it on
   * to the client.
   */
  public void run()
  {
    boolean failedLastTime = false;

    while (! closeRequested)
    {
      try
      {
        // Read an element from the server.
        ASN1Element element = serverReader.readElement();
        if (element == null)
        {
          // The server closed the connection.
          closeRequested = true;
          clientConnection.closeConnection();
          return;
        }


        // Decode it as necessary and write information about it to the log.
        synchronized (clientConnection.outputWriter)
        {
          clientConnection.outputWriter.println(
               clientConnection.dateFormat.format(new Date()) +
               " -- Read data from the server");


          if (decoder.displayRawBytes)
          {
            byte[] elementBytes = element.encode();
            clientConnection.outputWriter.println("Raw Data from Server:");
            clientConnection.outputWriter.println(
                 LDAPMessage.byteArrayToString(elementBytes, 4));
          }

          try
          {
            LDAPMessage message = LDAPMessage.decode(element);
            clientConnection.outputWriter.println("Decoded Data from Server:");
            clientConnection.outputWriter.println(message.toString(4));
            if (writeJobScript && (! decoder.excludeResponses()))
            {
              synchronized (scriptWriter)
              {
                message.toSLAMDScript(scriptWriter);
              }
            }


            if (decoder.verboseMode)
            {
              System.err.println("Decoded an " +
                                 message.getProtocolOp().getProtocolOpType() +
                                 " message");
            }
          }
          catch (Exception e)
          {
            clientConnection.outputWriter.println("Unable to Decode Data " +
                                                  "from Server:");
            if (decoder.verboseMode)
            {
              e.printStackTrace(clientConnection.outputWriter);
            }
          }

          clientConnection.outputWriter.println();
          clientConnection.outputWriter.println();
        }


        // Forward the data on to the client.
        clientConnection.writeToClient(element);
      }
      catch (Exception e)
      {
        if (failedLastTime)
        {
          synchronized (clientConnection.outputWriter)
          {
            clientConnection.outputWriter.println(
                 clientConnection.dateFormat.format(new Date()) +
                 " -- Error reading data from the server");
            if (decoder.verboseMode)
            {
              e.printStackTrace(clientConnection.outputWriter);
            }
          }

          clientConnection.closeConnection();
        }
        else
        {
          failedLastTime = true;
        }
      }
    }
  }



  /**
   * Writes the provided ASN.1 element to the server.
   *
   * @param  element  The ASN.1 element to write to the server.
   *
   * @throws  IOException  If a problem occurs while trying to write the element
   *                       to the server.
   */
  public void writeToServer(ASN1Element element)
         throws IOException
  {
    serverWriter.writeElement(element);
  }



  /**
   * Closes the connection to the server.
   */
  public void closeConnection()
  {
    closeRequested = true;

    try
    {
      serverReader.close();
      serverWriter.close();
    } catch (Exception e) {}

    try
    {
      serverSocket.close();
    } catch (Exception e) {}
  }
}

