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



import java.util.ArrayList;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a client hello message that the client uses to identify
 * itself to the server and optionally perform authentication so that the
 * server can verify the identity of the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientHelloMessage
       extends Message
{
  // Indicates whether the client is requesting the server to authenticate
  // itself.
  private final boolean requestServerAuth;

  // Indicates whether this client will operate in restricted mode, in which
  // case it should only be asked to run jobs that explicitly name it as a
  // requested client.
  private final boolean restrictedMode;

  // Indicates whether this client supports time synchronization.
  private final boolean supportsTimeSync;

  // The type of authentication that the client is going to perform.
  private final int authType;

  // The credentials corresponding to the authentication type and ID.
  private final String authCredentials;

  // The ID that the client is using to authenticate.
  private final String authID;

  // The client identifier, which is a human-readable name that administrators
  // can use to identify the client when viewing server status messages
  private final String clientID;

  // The version that the client is using, which may be used to determine
  // functionality available to that client or determine whether the client
  // should be allowed to authenticate
  private final String clientVersion;



  /**
   * Creates a new client hello message that will only provide client version
   * and identification information but does not perform any authentication.
   *
   * @param  messageID         The message ID for this message.
   * @param  clientVersion     The version of the client software being used.
   * @param  clientID          The human-readable text that can be used to
   *                           identify this client.
   * @param  supportsTimeSync  Indicates whether the client supports time
   *                           synchronization with the server.
   */
  public ClientHelloMessage(int messageID, String clientVersion,
                            String clientID, boolean supportsTimeSync)
  {
    this(messageID, clientVersion, clientID, Constants.AUTH_TYPE_NONE, "",
         "", false, false, supportsTimeSync);
  }



  /**
   * Creates a new client hello message with the specified information.  Server
   * authentication will not be requested.
   *
   * @param  messageID         The message ID for this message.
   * @param  clientVersion     The version of the client software being used.
   * @param  clientID          The human-readable text that can be used to
   *                           identify this client.
   * @param  authType          The type of authentication that the client is
   *                           using.
   * @param  authID            The ID that the client is using to authenticate.
   * @param  authCredentials   The credentials that the client is using to
   *                           authenticate.
   * @param  supportsTimeSync  Indicates whether the client supports time
   *                           synchronization with the server.
   */
  public ClientHelloMessage(int messageID, String clientVersion,
                            String clientID, int authType, String authID,
                            String authCredentials,
                            boolean supportsTimeSync)
  {
    this(messageID, clientVersion, clientID, authType, authID, authCredentials,
         false, false, supportsTimeSync);
  }



  /**
   * Creates a new client hello message with the specified information.
   *
   * @param  messageID          The message ID for this message.
   * @param  clientVersion      The version of the client software being used.
   * @param  clientID           The human-readable text that can be used to
   *                            identify this client.
   * @param  authType           The type of authentication that the client is
   *                            using.
   * @param  authID             The ID that the client is using to authenticate.
   * @param  authCredentials    The credentials that the client is using to
   *                            authenticate.
   * @param  requestServerAuth  Indicates whether the client requests that the
   *                            server authenticate itself.
   * @param  restrictedMode     Indicates whether the client will operate in
   *                            restricted mode, in which case it should only
   *                            be used to run jobs for which it is explicitly
   *                            requested.
   * @param  supportsTimeSync   Indicates whether the client supports time
   *                            synchronization with the server.
   */
  public ClientHelloMessage(int messageID, String clientVersion,
                            String clientID, int authType, String authID,
                            String authCredentials, boolean requestServerAuth,
                            boolean restrictedMode, boolean supportsTimeSync)
  {
    super(messageID, Constants.MESSAGE_TYPE_CLIENT_HELLO);

    this.clientVersion     = clientVersion;
    this.clientID          = clientID;
    this.authType          = authType;
    this.authID            = authID;
    this.authCredentials   = authCredentials;
    this.requestServerAuth = requestServerAuth;
    this.restrictedMode    = restrictedMode;
    this.supportsTimeSync  = supportsTimeSync;
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
   * Retrieves the type of authentication that the client is using.
   *
   * @return  The type of authentication that the client is using.
   */
  public int getAuthType()
  {
    return authType;
  }



  /**
   * Retrieves the ID that the client is using to authenticate itself.
   *
   * @return  The ID that the client is using to authenticate itself.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Retrieves the credentials that the client is using to authenticate itself.
   *
   * @return  The credentials that the client is using to authenticate itself.
   */
  public String getAuthCredentials()
  {
    return authCredentials;
  }



  /**
   * Indicates whether the client is requesting server authentication.
   *
   * @return  <CODE>true</CODE> if the client is requesting server
   *          authentication, or <CODE>false</CODE> if not.
   */
  public boolean requestServerAuth()
  {
    return requestServerAuth;
  }



  /**
   * Indicates whether the client is operating in restricted mode, in which case
   * the server should only give it jobs for which the client was explicitly
   * requested.
   *
   * @return  <CODE>true</CODE> if the client is operating in restricted mode,
   *          or <CODE>false</CODE> if not.
   */
  public boolean restrictedMode()
  {
    return restrictedMode;
  }



  /**
   * Indicates whether this client supports time synchronization with the
   * server.
   *
   * @return  <CODE>true</CODE> if the client supports time synchronization, or
   *          <CODE>false</CODE> if not.
   */
  public boolean supportsTimeSync()
  {
    return supportsTimeSync;
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

    return "Client Hello Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Client ID:  " + clientID + eol +
           "  Client Version:  " + clientVersion + eol +
           "  Authentication Type:  " + authType + eol +
           "  Authentication ID:  " + authID + eol +
           "  Authentication Credentials:  <hidden>" + eol +
           "  Request Server Authentication:  " + (requestServerAuth
                                                   ? "true"
                                                   : "false") + eol +
           "  Restricted Mode:  " + (restrictedMode ? "true" : "false") +
           eol +
           "  Supports Time Synchronization:  " +
           (supportsTimeSync ? "true" : "false") + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a client hello message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the ClientHello sequence.
   *
   * @return  The client hello message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a client hello message.
   */
  public static ClientHelloMessage decodeClientHello(int messageID,
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
    if ((elements.length < 2) || (elements.length > 6))
    {
      throw new SLAMDException("There must be between 2 and 6 elements in a " +
                               "client hello message");
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


    int    authType        = Constants.AUTH_TYPE_NONE;
    String authID          = null;
    String authCredentials = null;
    if (elements.length >= 3)
    {
      try
      {
        ASN1Element[] authElements =
             elements[2].decodeAsSequence().getElements();

        if (authElements.length != 3)
        {
          throw new SLAMDException("There must be three elements in an " +
                                   "authentication sequence");
        }

        authType = authElements[0].decodeAsInteger().getIntValue();
        authID   = authElements[1].decodeAsOctetString().getStringValue();
        authCredentials =
             authElements[2].decodeAsOctetString().getStringValue();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("Could not decode the authentication " +
                                 "information", ae);
      }
    }

    boolean requestServerAuth = false;
    if (elements.length >= 4)
    {
      try
      {
        requestServerAuth = elements[3].decodeAsBoolean().getBooleanValue();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("Could not decode the requestServerAuth " +
                                 "information", ae);
      }
    }

    boolean restrictedMode = false;
    if (elements.length >= 5)
    {
      try
      {
        restrictedMode = elements[4].decodeAsBoolean().getBooleanValue();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("Could not decode the restricted mode " +
                                 "information", ae);
      }
    }

    boolean supportsTimeSync = false;
    if (elements.length == 6)
    {
      try
      {
        supportsTimeSync = elements[5].decodeAsBoolean().getBooleanValue();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("Could not decode the time synchronization " +
                                 "information", ae);
      }
    }

    return new ClientHelloMessage(messageID, clientVersion, clientID, authType,
                                  authID, authCredentials, requestServerAuth,
                                  restrictedMode, supportsTimeSync);
  }



  /**
   * Encodes this message into an ASN.1 element.  A client hello message has the
   * following ASN.1 syntax:
   * <BR><BR>
   * <CODE>ClientHello ::= [APPLICATION 0] SEQUENCE {</CODE>
   * <CODE>    clientVersion      OCTET STRING,</CODE>
   * <CODE>    clientID           OCTET STRING,</CODE>
   * <CODE>    authentication     Authentication OPTIONAL,</CODE>
   * <CODE>    requestServerAuth  BOOLEAN OPTIONAL DEFAULT FALSE,</CODE>
   * <CODE>    restrictedMode     BOOLEAN OPTIONAL DEFAULT FALSE }</CODE>
   * <CODE>    supportsTimeSync   BOOLEAN OPTIONAL DEFAULT FALSE }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();
    elementList.add(new ASN1OctetString(clientVersion));
    elementList.add(new ASN1OctetString(clientID));

    if ((authType != Constants.AUTH_TYPE_NONE) || requestServerAuth ||
        restrictedMode || supportsTimeSync)
    {
      if (authType == Constants.AUTH_TYPE_SIMPLE)
      {
        ASN1Element[] authElements = new ASN1Element[]
        {
          new ASN1Enumerated(Constants.AUTH_TYPE_SIMPLE),
          new ASN1OctetString(authID),
          new ASN1OctetString(authCredentials)
        };
        elementList.add(new ASN1Sequence(authElements));
      }
      else
      {
        ASN1Element[] authElements = new ASN1Element[]
        {
          new ASN1Enumerated(Constants.AUTH_TYPE_NONE),
          new ASN1OctetString(),
          new ASN1OctetString()
        };
        elementList.add(new ASN1Sequence(authElements));
      }

      if (requestServerAuth || restrictedMode || supportsTimeSync)
      {
        elementList.add(new ASN1Boolean(requestServerAuth));
      }

      if (restrictedMode || supportsTimeSync)
      {
        elementList.add(new ASN1Boolean(restrictedMode));
      }

      if (supportsTimeSync)
      {
        elementList.add(new ASN1Boolean(supportsTimeSync));
      }
    }

    ASN1Element[] clientHelloElements = new ASN1Element[elementList.size()];
    elementList.toArray(clientHelloElements);
    ASN1Sequence clientHelloSequence = new ASN1Sequence(ASN1_TYPE_CLIENT_HELLO,
                                                        clientHelloElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      clientHelloSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

