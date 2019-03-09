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
package com.slamd.message;



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a client manager hello message that the client manager
 * uses to identify itself to the server and tell the server the maximum number
 * of clients that may be executed on the client system.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientManagerHelloMessage
       extends Message
{
  // The maximum number of clients that this client manager will allow.
  private final int maxClients;

  // The client identifier, which is a human-readable name that administrators
  // can use to identify the client when viewing server status messages
  private final String clientID;

  // The version that the client is using, which may be used to determine
  // functionality available to that client or determine whether the client
  // should be allowed to authenticate
  private final String clientVersion;



  /**
   * Creates a new client manager hello message that will provide client ID,
   * client version, and the maximum number of clients that may be created.
   *
   * @param  messageID      The message ID for this message.
   * @param  clientVersion  The version of the client software being used.
   * @param  clientID       The human-readable text that can be used to identify
   *                        this client.
   * @param  maxClients     The maximum number of clients that may be created.
   */
  public ClientManagerHelloMessage(int messageID, String clientVersion,
                                   String clientID, int maxClients)
  {
    super(messageID, Constants.MESSAGE_TYPE_CLIENT_MANAGER_HELLO);

    this.clientVersion = clientVersion;
    this.clientID      = clientID;
    this.maxClients    = maxClients;
  }



  /**
   * Retrieves the version of the client software being used.
   *
   * @return  The version of the client software being used.
   */
  public String getClientVersion()
  {
    return clientVersion;
  }



  /**
   * Retrieves the human-readable ID for the client.
   *
   * @return  The human-readable ID for the client.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Retrieves the maximum number of clients that will be allowed.
   *
   * @return  The maximum number of clients that will be allowed.
   */
  public int getMaxClients()
  {
    return maxClients;
  }



  /**
   * Retrieves a string representation of this message.
   *
   * @return  A string representation of this message.
   */
  @Override()
  public String toString()
  {
    String eol = System.getProperty("line.separator");

    return "Client Manager Hello Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Client ID:  " + clientID + eol +
           "  Client Version:  " + clientVersion + eol +
           "  Max Clients:  " + maxClients + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a client manager hello message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the ClientManagerHello
   *                    sequence.
   *
   * @return  The client manager hello message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a client manager hello message.
   */
  public static ClientManagerHelloMessage
                     decodeClientManagerHello(int messageID,
                                              ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence helloSequence = null;
    try
    {
      helloSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The provided ASN.1 element cannot be decoded " +
                               "as a sequence", ae);
    }

    ASN1Element[] elements = helloSequence.getElements();
    if (elements.length != 3)
    {
      throw new SLAMDException("There must be 3 elements in a client manager " +
                               "hello message");
    }

    String clientVersion = null;
    try
    {
      clientVersion = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode first element as an octet " +
                               "string", ae);
    }

    String clientID = null;
    try
    {
      clientID = elements[1].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode second element as an octet " +
                               "string", ae);
    }

    int maxClients = -1;
    try
    {
      maxClients = elements[2].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode third element as an integer",
                               ae);
    }


    return new ClientManagerHelloMessage(messageID, clientVersion, clientID,
                                         maxClients);
  }



  /**
   * Encodes this message into an ASN.1 element.  A client hello manager message
   * has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>ClientManagerHello ::= [APPLICATION 14] SEQUENCE {</CODE>
   * <CODE>    clientVersion      OCTET STRING,</CODE>
   * <CODE>    clientID           OCTET STRING,</CODE>
   * <CODE>    maxClients         INTEGER }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Element[] clientManagerHelloElements = new ASN1Element[]
    {
      new ASN1OctetString(clientVersion),
      new ASN1OctetString(clientID),
      new ASN1Integer(maxClients)
    };

    ASN1Sequence clientManagerHelloSequence =
         new ASN1Sequence(ASN1_TYPE_CLIENT_MANAGER_HELLO,
                          clientManagerHelloElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      clientManagerHelloSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

