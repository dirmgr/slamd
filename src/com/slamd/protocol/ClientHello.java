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
package com.slamd.protocol;



import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the client to the
 * server when the connection is first established.  It provides the server with
 * all the necessary properties and capabilities for this client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientHello
       extends SLAMDMessage
{
  // Indicates whether this client requires the server to authenticate itself.
  private boolean requireServerAuth;

  // Indicates whether this client is operating in restricted mode.
  private boolean restrictedMode;

  // The authentication credentials for this client.
  private byte[] authCredentials;

  // The port number for this client.
  private int clientPort;

  // The major version number for this client.
  private int majorVersion;

  // The minor version number for this client.
  private int minorVersion;

  // The point version number for this client.
  private int pointVersion;

  // The authentication ID for this client.
  private String authID;

  // The authentication method used by this client.
  private String authMethod;

  // The client ID for this client.
  private String clientID;

  // The IP address for this client.
  private String clientIP;

  // The ID of the client manager with which this client is associated, if any.
  private String clientManagerID;



  /**
   * Creates a new instance of this client hello message which is intended for
   * use in decoding a message transmitted between the client and the server.
   * It is not intended for general use.
   */
  public ClientHello()
  {
    super();

    clientIP          = null;
    clientPort        = -1;
    clientID          = null;
    clientManagerID   = null;
    majorVersion      = -1;
    minorVersion      = -1;
    pointVersion      = -1;
    restrictedMode    = false;
    authMethod        = null;
    authID            = null;
    authCredentials   = null;
    requireServerAuth = false;
  }



  /**
   * Creates a new instance of this client hello message with the provided
   * information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  clientIP         The IP address of the client system.
   * @param  clientPort       The port number of the client system.
   * @param  clientID         The client ID for the client system.
   * @param  clientManagerID  The ID for the associated client manager.
   * @param  majorVersion     The major version number for this client.
   * @param  minorVersion     The minor version number for this client.
   * @param  pointVersion     The point version number for this client.
   * @param  restrictedMode   Indicates whether this client is operating in
   *                          restricted mode.
   */
  public ClientHello(int messageID, HashMap<String,String> extraProperties,
                     String clientIP, int clientPort, String clientID,
                     String clientManagerID, int majorVersion, int minorVersion,
                     int pointVersion, boolean restrictedMode)
  {
    super(messageID, extraProperties);

    this.clientIP        = clientIP;
    this.clientPort      = clientPort;
    this.clientID        = clientID;
    this.clientManagerID = clientManagerID;
    this.majorVersion    = majorVersion;
    this.minorVersion    = minorVersion;
    this.pointVersion    = pointVersion;
    this.restrictedMode  = restrictedMode;

    authMethod        = null;
    authID            = null;
    authCredentials   = null;
    requireServerAuth = false;
  }



  /**
   * Creates a new instance of this client hello message with the provided
   * information.
   *
   * @param  messageID          The message ID for this SLAMD message.
   * @param  extraProperties    The "extra" properties for this SLAMD message.
   *                            Both the names and values for the properties
   *                            must be strings.
   * @param  clientIP           The IP address of the client system.
   * @param  clientPort         The port number of the client system.
   * @param  clientID           The client ID for the client system.
   * @param  clientManagerID    The ID for the associated client manager.
   * @param  majorVersion       The major version number for this client.
   * @param  minorVersion       The minor version number for this client.
   * @param  pointVersion       The point version number for this client.
   * @param  restrictedMode     Indicates whether this client is operating in
   *                            restricted mode.
   * @param  authMethod         The authentication method used by this client.
   * @param  authID             The authentication ID for this client.
   * @param  authCredentials    The authentication credentials for this client.
   * @param  requireServerAuth  Indicates whether this client requires the
   *                            server to provide its own authentication info.
   */
  public ClientHello(int messageID, HashMap<String,String> extraProperties,
                     String clientIP, int clientPort, String clientID,
                     String clientManagerID, int majorVersion, int minorVersion,
                     int pointVersion, boolean restrictedMode,
                     String authMethod, String authID, byte[] authCredentials,
                     boolean requireServerAuth)
  {
    super(messageID, extraProperties);

    this.clientIP          = clientIP;
    this.clientPort        = clientPort;
    this.clientID          = clientID;
    this.clientManagerID   = clientManagerID;
    this.majorVersion      = majorVersion;
    this.minorVersion      = minorVersion;
    this.pointVersion      = pointVersion;
    this.restrictedMode    = restrictedMode;
    this.authMethod        = authMethod;
    this.authID            = authID;
    this.authCredentials   = authCredentials;
    this.requireServerAuth = requireServerAuth;
  }



  /**
   * Retrieves the client IP address for this client connection.
   *
   * @return  The client IP address for this client connection.
   */
  public String getClientIP()
  {
    return clientIP;
  }



  /**
   * Specifies the client IP address for this client connection.
   *
   * @param  clientIP  The client IP address for this client connection.
   */
  public void setClientIP(String clientIP)
  {
    this.clientIP = clientIP;
  }



  /**
   * Retrieves the client port number for this client connection.
   *
   * @return  The client port number for this client connection.
   */
  public int getClientPort()
  {
    return clientPort;
  }



  /**
   * Specifies the client port number for this client connection.
   *
   * @param  clientPort  The client port number for this client connection.
   */
  public void setClientPort(int clientPort)
  {
    this.clientPort = clientPort;
  }



  /**
   * Retrieves the client ID for this client connection.
   *
   * @return  The client ID for this client connection.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Specifies the client ID for this client connection.
   *
   * @param  clientID  The client ID for this client connection.
   */
  public void setClientID(String clientID)
  {
    this.clientID = clientID;
  }



  /**
   * Retrieves the client manager ID for the client manager with which this
   * client connection is associated.
   *
   * @return  The client manager ID for the client manager with which this
   *          client connection is associated, or <CODE>null</CODE> if it is not
   *          associated with a client manager.
   */
  public String getClientManagerID()
  {
    return clientManagerID;
  }



  /**
   * Specifies the client manager ID for the client manager with which this
   * client connection is associated.
   *
   * @param  clientManagerID  The client manager ID for the client manager with
   *                          which this client connection is associated.
   */
  public void setClientManagerID(String clientManagerID)
  {
    this.clientManagerID = clientManagerID;
  }



  /**
   * Retrieves the major version number for the client software.
   *
   * @return  The major version number for the client software.
   */
  public int getMajorVersion()
  {
    return majorVersion;
  }



  /**
   * Specifies the major version number for the client software.
   *
   * @param  majorVersion  The major version number for the client software.
   */
  public void setMajorVersion(int majorVersion)
  {
    this.majorVersion = majorVersion;
  }



  /**
   * Retrieves the minor version number for the client software.
   *
   * @return  The minor version number for the client software.
   */
  public int getMinorVersion()
  {
    return minorVersion;
  }



  /**
   * Specifies the minor version number for the client software.
   *
   * @param  minorVersion  The minor version number for the client software.
   */
  public void setMinorVersion(int minorVersion)
  {
    this.minorVersion = minorVersion;
  }



  /**
   * Retrieves the point version number for the client software.
   *
   * @return  The point version number for the client software.
   */
  public int getPointVersion()
  {
    return pointVersion;
  }



  /**
   * Specifies the point version number for the client software.
   *
   * @param  pointVersion  The point version number for the client software.
   */
  public void setPointVersion(int pointVersion)
  {
    this.pointVersion = pointVersion;
  }



  /**
   * Indicates whether the client is running in restricted mode and should only
   * be assigned jobs for which it is explicitly requested.
   *
   * @return  <CODE>true</CODE> if this client is running in restricted mode, or
   *          <CODE>false</CODE> if it is not.
   */
  public boolean getRestrictedMode()
  {
    return restrictedMode;
  }



  /**
   * Specifies whether the client is running in restricted mode and should only
   * be assigned jobs for which it is explicitly requested.
   *
   * @param  restrictedMode  Specifies whether the client is running in
   *                         restricted mode.
   */
  public void setRestrictedMode(boolean restrictedMode)
  {
    this.restrictedMode = restrictedMode;
  }



  /**
   * Retrieves the name of the method that the client wishes to use to
   * authenticate to the server.
   *
   * @return  The name of the method that the client wishes to use to
   *          authenticate to the server, or <CODE>null</CODE> if no
   *          authentication is to be performed.
   */
  public String getAuthMethod()
  {
    return authMethod;
  }



  /**
   * Specifies the name of the method that the client wishes to use to
   * authenticate to the server.
   *
   * @param  authMethod  The name of the method that the client wishes to use to
   *                     authenticate to the server.
   */
  public void setAuthMethod(String authMethod)
  {
    this.authMethod = authMethod;
  }



  /**
   * Retrieves the authentication ID that the client wishes to use to
   * authenticate to the server.
   *
   * @return  The authentication ID that the client wishes to use to
   *          authenticate to the server, or <CODE>null</CODE> if no
   *          authentication is to be performed or no auth ID is required for
   *          the selected authentication method.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Specifies the authentication ID that the client wishes to use to
   * authenticate to the server.
   *
   * @param  authID  The authentication ID that the client wishes to use to
   *                 authenticate to the server.
   */
  public void setAuthID(String authID)
  {
    this.authID = authID;
  }



  /**
   * Retrieves the credentials that the client wishes to use to authenticate to
   * the server.
   *
   * @return  The credentials that the client wishes to use to authenticate to
   *          the server, or <CODE>null</CODE> if no authentication is to be
   *          performed or no credentials are required for the selected
   *          authentication method.
   */
  public byte[] getAuthCredentials()
  {
    return authCredentials;
  }



  /**
   * Specifies the credentials that the client wishes to use to authenticate to
   * the server.
   *
   * @param  authCredentials  The credentials that the client wishes to use to
   *                          authenticate to the server.
   */
  public void setAuthCredentials(byte[] authCredentials)
  {
    this.authCredentials = authCredentials;
  }



  /**
   * Indicates whether the client requires the server to authenticate itself to
   * the client.
   *
   * @return  <CODE>true</CODE> if the client requires the server to
   *          authenticate itself to the client, or <CODE>false</CODE> if not.
   */
  public boolean getRequireServerAuth()
  {
    return requireServerAuth;
  }



  /**
   * Specifies whether the client requires the server to authenticate itself to
   * the client.
   *
   * @param  requireServerAuth  Specifies whether the client requires the server
   *                            to authenticate itself to the client.
   */
  public void setRequireServerAuth(boolean requireServerAuth)
  {
    this.requireServerAuth = requireServerAuth;
  }



  /**
   * Encodes the payload component of this SLAMD message to an ASN.1 element for
   * inclusion in the message envelope.
   *
   * @return  The ASN.1 element containing the encoded message payload.
   */
  @Override()
  public ASN1Element encodeMessagePayload()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_IP,
                                        new ASN1OctetString(clientIP)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_PORT,
                                        new ASN1Integer(clientPort)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_ID,
                                        new ASN1OctetString(clientID)));

    if (clientManagerID != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_CLIENT_MANAGER_ID,
                           new ASN1OctetString(clientManagerID)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_MAJOR_VERSION,
                         new ASN1Integer(majorVersion)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_MINOR_VERSION,
                         new ASN1Integer(minorVersion)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_POINT_VERSION,
                         new ASN1Integer(pointVersion)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_RESTRICTED_MODE,
                         new ASN1Boolean(restrictedMode)));

    if (authMethod != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_AUTH_METHOD,
                           new ASN1OctetString(authMethod)));
    }

    if (authID != null)
    {
      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_AUTH_ID,
                                          new ASN1OctetString(authID)));
    }

    if (authCredentials != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_AUTH_CREDENTIALS,
                           new ASN1OctetString(authCredentials)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_REQUIRE_SERVER_AUTH,
                         new ASN1Boolean(requireServerAuth)));


    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the provided ASN.1 element and uses it as the payload for this
   * SLAMD message.
   *
   * @param  payloadElement  The ASN.1 element to decode as the payload for this
   *                         SLAMD message.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided ASN.1 element as the payload for this
   *                          SLAMD message.
   */
  @Override()
  public void decodeMessagePayload(ASN1Element payloadElement)
         throws SLAMDException
  {
    HashMap<String,ASN1Element> propertyMap =
         decodeNameValuePairSequence(payloadElement);

    ASN1Element valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_IP);
    if (valueElement == null)
    {
      throw new SLAMDException("Client hello message is missing the client " +
                               "IP address.");
    }
    else
    {
      try
      {
        clientIP = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client IP address:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_PORT);
    if (valueElement == null)
    {
      throw new SLAMDException("Client hello message is missing the client " +
                               "port number.");
    }
    else
    {
      try
      {
        clientPort = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client port number:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_ID);
    if (valueElement == null)
    {
      throw new SLAMDException("Client hello message is missing the client ID");
    }
    else
    {
      try
      {
        clientID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client ID:  " + e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_MANAGER_ID);
    if (valueElement != null)
    {
      try
      {
        clientManagerID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client manager ID:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_MAJOR_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Client hello message is missing the major " +
                               "version number.");
    }
    else
    {
      try
      {
        majorVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the major version " +
                                 "number:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_MINOR_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Client hello message is missing the minor " +
                               "version number.");
    }
    else
    {
      try
      {
        minorVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the minor version " +
                                 "number:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_POINT_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Client hello message is missing the point " +
                               "version number.");
    }
    else
    {
      try
      {
        pointVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the point version " +
                                 "number:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_RESTRICTED_MODE);
    if (valueElement != null)
    {
      try
      {
        restrictedMode = valueElement.decodeAsBoolean().getBooleanValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the restrictedMode flag:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_AUTH_METHOD);
    if (valueElement != null)
    {
      try
      {
        authMethod = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the auth method:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_AUTH_ID);
    if (valueElement != null)
    {
      try
      {
        authID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the auth ID:  " + e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_AUTH_CREDENTIALS);
    if (valueElement != null)
    {
      try
      {
        authCredentials = valueElement.decodeAsOctetString().getValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the auth credentials:  " +
                                 e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_REQUIRE_SERVER_AUTH);
    if (valueElement != null)
    {
      try
      {
        requireServerAuth = valueElement.decodeAsBoolean().getBooleanValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the requireServerAuth " +
                                 "flag:  " + e, e);
      }
    }
  }



  /**
   * Appends a string representation of the payload for this SLAMD message to
   * the provided buffer.  The string representation may contain multiple lines,
   * but the last line should not end with an end-of-line marker.
   *
   * @param  buffer  The buffer to which the string representation is to be
   *                 appended.
   * @param  indent  The number of spaces to indent the payload content.
   */
  @Override()
  public void payloadToString(StringBuilder buffer, int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    buffer.append(indentBuf);
    buffer.append("clientIP = ");
    buffer.append(clientIP);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("clientPort = ");
    buffer.append(clientPort);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("clientID = ");
    buffer.append(clientID);
    buffer.append(Constants.EOL);

    if (clientManagerID != null)
    {
      buffer.append(indentBuf);
      buffer.append("clientManagerID = ");
      buffer.append(clientManagerID);
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("majorVersion = ");
    buffer.append(majorVersion);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("minorVersion = ");
    buffer.append(minorVersion);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("pointVersion = ");
    buffer.append(pointVersion);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("restrictedMode = ");
    buffer.append(restrictedMode);
    buffer.append(Constants.EOL);

    if (authMethod != null)
    {
      buffer.append(indentBuf);
      buffer.append("authMethod = ");
      buffer.append(authMethod);
      buffer.append(Constants.EOL);
    }

    if (authID != null)
    {
      buffer.append(indentBuf);
      buffer.append("authID = ");
      buffer.append(authID);
      buffer.append(Constants.EOL);
    }

    if (authCredentials != null)
    {
      buffer.append(indentBuf);
      buffer.append("authCredentials = byte[");
      buffer.append(authCredentials.length);
      buffer.append(']');
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("requireServerAuth = ");
    buffer.append(requireServerAuth);
  }
}

