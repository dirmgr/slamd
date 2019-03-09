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
 * This class defines a server hello message that the server uses to identify
 * itself, and optionally authenticate itself, to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerHelloMessage
       extends Message
{
  // The type of authentication that the server is going to perform.
  private final int authType;

  // The credentials corresponding to the authentication type and ID.
  private final String authCredentials;

  // The ID that the server is using to authenticate.
  private final String authID;

  // The server identifier, which is a human-readable name that administrators
  // can use to identify the server when viewing status messages
  private final String serverID;

  // The version that the server is using
  private final String serverVersion;



  /**
   * Creates a new server hello message that will only provide version and
   * identification information but does not perform any authentication.
   *
   * @param  messageID      The message ID for this message.
   * @param  serverVersion  The version of the server software being used.
   * @param  serverID       The human-readable text that can be used to identify
   *                        the server.
   */
  public ServerHelloMessage(int messageID, String serverVersion,
                            String serverID)
  {
    this(messageID, serverVersion, serverID, Constants.AUTH_TYPE_NONE, "",
         "");
  }



  /**
   * Creates a new server hello message with the specified information.  Server
   * authentication will not be requested.
   *
   * @param  messageID        The message ID for this message.
   * @param  serverVersion    The version of the server software being used.
   * @param  serverID         The human-readable text that can be used to
   *                          identify this server.
   * @param  authType         The type of authentication that the server is
   *                          using.
   * @param  authID           The ID that the server is using to authenticate.
   * @param  authCredentials  The credentials that the server is using to
   *                          authenticate.
   */
  public ServerHelloMessage(int messageID, String serverVersion,
                            String serverID, int authType, String authID,
                            String authCredentials)
  {
    super(messageID, Constants.MESSAGE_TYPE_SERVER_HELLO);

    this.serverVersion     = serverVersion;
    this.serverID          = serverID;
    this.authType          = authType;
    this.authID            = authID;
    this.authCredentials   = authCredentials;
  }



  /**
   * Retrieves the version of the server software being used.
   *
   * @return  The version of the server software being used.
   */
  public String getServerVersion()
  {
    return serverVersion;
  }



  /**
   * Retrieves the human-readable ID for the server.
   *
   * @return  The human-readable ID for the server.
   */
  public String getServerID()
  {
    return serverID;
  }



  /**
   * Retrieves the type of authentication that the server is using.
   *
   * @return  The type of authentication that the server is using.
   */
  public int getAuthType()
  {
    return authType;
  }



  /**
   * Retrieves the ID that the server is using to authenticate itself.
   *
   * @return  The ID that the server is using to authenticate itself.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Retrieves the credentials that the server is using to authenticate itself.
   *
   * @return  The credentials that the server is using to authenticate itself.
   */
  public String getAuthCredentials()
  {
    return authCredentials;
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

    return "Server Hello Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Server ID:  " + serverID + eol +
           "  Server Version:  " + serverVersion + eol +
           "  Authentication Type:  " + authType + eol +
           "  Authentication ID:  " + authID + eol +
           "  Authentication Credentials:  <hidden>" + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a server hello message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the ServerHello sequence.
   *
   * @return  The server hello message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a server hello message.
   */
  public static ServerHelloMessage decodeServerHello(int messageID,
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
      throw new SLAMDException("Could not decode the provided ASN.1 element " +
                               "as a sequence", ae);
    }


    ASN1Element[] elements = helloSequence.getElements();
    if (elements.length < 2)
    {
      throw new SLAMDException("There must be at least two elements in a " +
                               "server hello");
    }


    String serverVersion = null;
    try
    {
      serverVersion = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the first element as an " +
                               "octet string", ae);
    }


    String serverID = null;
    try
    {
      serverID = elements[1].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the second element as an " +
                               "octet string", ae);
    }


    // FIXME:  Decode the authentication if it's present


    return new ServerHelloMessage(messageID, serverVersion, serverID);
  }



  /**
   * Encodes this message into an ASN.1 element.  A server hello message has the
   * following ASN.1 syntax:
   * <BR><BR>
   * <CODE>ServerHello ::= [APPLICATION 2] SEQUENCE {</CODE>
   * <CODE>    serverVersion   OCTET STRING,</CODE>
   * <CODE>    serverID        OCTET STRING,</CODE>
   * <CODE>    authentication  Authentication OPTIONAL }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1OctetString versionElement = new ASN1OctetString(serverVersion);
    ASN1OctetString idElement = new ASN1OctetString(serverID);

    ASN1Element[] serverHelloElements = new ASN1Element[]
    {
      versionElement,
      idElement
    };

    ASN1Sequence serverHelloSequence = new ASN1Sequence(ASN1_TYPE_SERVER_HELLO,
                                                        serverHelloElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      serverHelloSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

