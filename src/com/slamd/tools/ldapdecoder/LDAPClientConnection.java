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
import java.text.SimpleDateFormat;
import java.util.Date;

import com.slamd.asn1.ASN1Element;



/**
 * This class defines a data structure for holding information about a
 * connection from an LDAP client.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPClientConnection
{
  // The LDAP decoder that accepted this client connection.
  private LDAPDecoder decoder;

  // The thread that will be processing requests from the client.
  private LDAPDecoderClientThread clientThread;

  // The thread that will be processing responses from the server.
  private LDAPDecoderServerThread serverThread;

  // The print stream to which the decoded information will be written.
  protected PrintStream outputWriter;

  // The date formatter that will be used to format operation timestamps.
  protected SimpleDateFormat dateFormat;

  // The socket to be used for communicating with the client.
  protected Socket clientSocket;



  /**
   * Creates a new LDAP client connection with the provided information.
   *
   * @param  decoder       The LDAP decoder that accepted this connection.
   * @param  clientSocket  The socket to be used for communicating with the
   *                       client.
   * @param  outputWriter  The print stream to which all decoded traffic will
   *                       be written.
   *
   * @throws  LDAPDecoderException  If a problem occurs while establishing the
   *                                communication between the client and the
   *                                server.
   */
  public LDAPClientConnection(LDAPDecoder decoder, Socket clientSocket,
                              PrintStream outputWriter)
         throws LDAPDecoderException
  {
    this.decoder      = decoder;
    this.clientSocket = clientSocket;
    this.outputWriter = outputWriter;

    dateFormat = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss.SSS Z]");

    outputWriter.println(dateFormat.format(new Date()) +
                         " -- New client connection from " +
                         clientSocket.getInetAddress().getHostAddress() + ':' +
                         clientSocket.getPort());

    serverThread = new LDAPDecoderServerThread(decoder, this);
    clientThread = new LDAPDecoderClientThread(decoder, this);

    serverThread.start();
    clientThread.start();
  }



  /**
   * Writes the provided ASN.1 element to the client.
   *
   * @param  element  The ASN.1 element to be written to the client.
   *
   * @throws  IOException  If a problem occurs while trying to write the data.
   */
  public void writeToClient(ASN1Element element)
         throws IOException
  {
    clientThread.writeToClient(element);
  }



  /**
   * Writes the provided ASN.1 element to the server.
   *
   * @param  element  The ASN.1 element to be written to the server.
   *
   * @throws  IOException  If a problem occurs while trying to write the data.
   */
  public void writeToServer(ASN1Element element)
         throws IOException
  {
    serverThread.writeToServer(element);
  }



  /**
   * Closes the connection between the client and the server.
   */
  public void closeConnection()
  {
    clientThread.closeConnection();
    serverThread.closeConnection();

    outputWriter.println(dateFormat.format(new Date()) +
                         " -- Connection from " +
                         clientSocket.getInetAddress().getHostAddress() +
                         " closed");
  }
}

