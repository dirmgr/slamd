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

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the server to the
 * client when the connection is first established.  It provides the client with
 * information about the state of the server and any authentication information
 * that the client might have requested.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerHello
       extends SLAMDMessage
{
  // The authentication credentials for the server.
  private byte[] authCredentials;

  // The result code from the client hello processing.
  private int clientHelloResultCode;

  // The major version number for the server.
  private int majorVersion;

  // The minor version number for the server.
  private int minorVersion;

  // The point version number for the server.
  private int pointVersion;

  // The authentication ID for the server.
  private String authID;

  // The authentication method used by the server.
  private String authMethod;

  // The message providing additional information about the client hello
  // processing.
  private String clientHelloResultMessage;



  /**
   * Creates a new instance of this server hello message which is intended for
   * use in decoding a message transmitted between the server and the client.
   * It is not intended for general use.
   */
  public ServerHello()
  {
    super();

    clientHelloResultCode    = -1;
    clientHelloResultMessage = null;
    majorVersion             = -1;
    minorVersion             = -1;
    pointVersion             = -1;
    authMethod               = null;
    authID                   = null;
    authCredentials          = null;
  }



  /**
   * Creates a new instance of this server hello message with the provided
   * information.
   *
   * @param  messageID                 The message ID for this SLAMD message.
   * @param  extraProperties           The "extra" properties for this SLAMD
   *                                   message.  Both the names and values for
   *                                   the properties must be strings.
   * @param  clientHelloResultCode     The result code from the client hello
   *                                   processing.
   * @param  clientHelloResultMessage  The message providing additional
   *                                   information about the client hello
   *                                   processing.
   * @param  majorVersion              The major version number for the server.
   * @param  minorVersion              The minor version number for the server.
   * @param  pointVersion              The point version number for the server.
   */
  public ServerHello(int messageID, HashMap<String,String> extraProperties,
                     int clientHelloResultCode, String clientHelloResultMessage,
                     int majorVersion, int minorVersion, int pointVersion)
  {
    super(messageID, extraProperties);

    this.clientHelloResultCode    = clientHelloResultCode;
    this.clientHelloResultMessage = clientHelloResultMessage;
    this.majorVersion             = majorVersion;
    this.minorVersion             = minorVersion;
    this.pointVersion             = pointVersion;

    authMethod        = null;
    authID            = null;
    authCredentials   = null;
  }



  /**
   * Creates a new instance of this server hello message with the provided
   * information.
   *
   * @param  messageID                 The message ID for this SLAMD message.
   * @param  extraProperties           The "extra" properties for this SLAMD
   *                                   message.  Both the names and values for
   *                                   the properties must be strings.
   * @param  clientHelloResultCode     The result code from the client hello
   *                                   processing.
   * @param  clientHelloResultMessage  The message providing additional
   *                                   information about the client hello
   *                                   processing.
   * @param  majorVersion              The major version number for the server.
   * @param  minorVersion              The minor version number for the server.
   * @param  pointVersion              The point version number for the server.
   * @param  authMethod                The authentication method used by this
   *                                   client.
   * @param  authID                    The authentication ID for this client.
   * @param  authCredentials           The authentication credentials for this
   *                                   client.
   */
  public ServerHello(int messageID, HashMap<String,String> extraProperties,
                     int clientHelloResultCode, String clientHelloResultMessage,
                     int majorVersion, int minorVersion, int pointVersion,
                     String authMethod, String authID, byte[] authCredentials)
  {
    super(messageID, extraProperties);

    this.clientHelloResultCode    = clientHelloResultCode;
    this.clientHelloResultMessage = clientHelloResultMessage;
    this.majorVersion             = majorVersion;
    this.minorVersion             = minorVersion;
    this.pointVersion             = pointVersion;
    this.authMethod               = authMethod;
    this.authID                   = authID;
    this.authCredentials          = authCredentials;
  }



  /**
   * Retrieves the result code from the client hello processing.
   *
   * @return  The result code from the client hello processing.
   */
  public int getClientHelloResultCode()
  {
    return clientHelloResultCode;
  }



  /**
   * Specifies the result code from the client hello processing.
   *
   * @param  clientHelloResultCode  The result code from the client hello
   *                                processing.
   */
  public void setClientHelloResultCode(int clientHelloResultCode)
  {
    this.clientHelloResultCode = clientHelloResultCode;
  }



  /**
   * Retrieves the message with additional information about the client hello
   * processing.
   *
   * @return  The message with additional information about the client hello
   *          processing, or <CODE>null</CODE> if there is no additional
   *          information.
   */
  public String getClientHelloResultMessage()
  {
    return clientHelloResultMessage;
  }



  /**
   * Specifies the message with additional information about the client hello
   * processing.
   *
   * @param  clientHelloResultMessage  The message with additional information
   *                                   about the client hello processing.
   */
  public void setClientHelloResultMessage(String clientHelloResultMessage)
  {
    this.clientHelloResultMessage = clientHelloResultMessage;
  }



  /**
   * Retrieves the major version number for the server software.
   *
   * @return  The major version number for the server software.
   */
  public int getMajorVersion()
  {
    return majorVersion;
  }



  /**
   * Specifies the major version number for the server software.
   *
   * @param  majorVersion  The major version number for the server software.
   */
  public void setMajorVersion(int majorVersion)
  {
    this.majorVersion = majorVersion;
  }



  /**
   * Retrieves the minor version number for the server software.
   *
   * @return  The minor version number for the server software.
   */
  public int getMinorVersion()
  {
    return minorVersion;
  }



  /**
   * Specifies the minor version number for the server software.
   *
   * @param  minorVersion  The minor version number for the server software.
   */
  public void setMinorVersion(int minorVersion)
  {
    this.minorVersion = minorVersion;
  }



  /**
   * Retrieves the point version number for the server software.
   *
   * @return  The point version number for the server software.
   */
  public int getPointVersion()
  {
    return pointVersion;
  }



  /**
   * Specifies the point version number for the server software.
   *
   * @param  pointVersion  The point version number for the server software.
   */
  public void setPointVersion(int pointVersion)
  {
    this.pointVersion = pointVersion;
  }



  /**
   * Retrieves the name of the method that the server wishes to use to
   * authenticate to the client.
   *
   * @return  The name of the method that the server wishes to use to
   *          authenticate to the client, or <CODE>null</CODE> if no
   *          authentication is to be performed.
   */
  public String getAuthMethod()
  {
    return authMethod;
  }



  /**
   * Specifies the name of the method that the server wishes to use to
   * authenticate to the client.
   *
   * @param  authMethod  The name of the method that the server wishes to use to
   *                     authenticate to the client.
   */
  public void setAuthMethod(String authMethod)
  {
    this.authMethod = authMethod;
  }



  /**
   * Retrieves the authentication ID that the server wishes to use to
   * authenticate to the client.
   *
   * @return  The authentication ID that the server wishes to use to
   *          authenticate to the client, or <CODE>null</CODE> if no
   *          authentication is to be performed or no auth ID is required for
   *          the selected authentication method.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Specifies the authentication ID that the server wishes to use to
   * authenticate to the client.
   *
   * @param  authID  The authentication ID that the server wishes to use to
   *                 authenticate to the client.
   */
  public void setAuthID(String authID)
  {
    this.authID = authID;
  }



  /**
   * Retrieves the credentials that the server wishes to use to authenticate to
   * the client.
   *
   * @return  The credentials that the server wishes to use to authenticate to
   *          the client, or <CODE>null</CODE> if no authentication is to be
   *          performed or no credentials are required for the selected
   *          authentication method.
   */
  public byte[] getAuthCredentials()
  {
    return authCredentials;
  }



  /**
   * Specifies the credentials that the server wishes to use to authenticate to
   * the client.
   *
   * @param  authCredentials  The credentials that the server wishes to use to
   *                          authenticate to the client.
   */
  public void setAuthCredentials(byte[] authCredentials)
  {
    this.authCredentials = authCredentials;
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

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_RESULT_CODE,
                         new ASN1Enumerated(clientHelloResultCode)));

    if (clientHelloResultMessage != null)
    {
      elementList.add(encodeNameValuePair(
           ProtocolConstants.PROPERTY_RESULT_MESSAGE,
           new ASN1OctetString(clientHelloResultMessage)));
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
         propertyMap.get(ProtocolConstants.PROPERTY_RESULT_CODE);
    if (valueElement == null)
    {
      throw new SLAMDException("Server hello message is missing the client " +
                               "hello result code.");
    }
    else
    {
      try
      {
        clientHelloResultCode = valueElement.decodeAsEnumerated().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client hello result " +
                                 "code:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_RESULT_MESSAGE);
    if (valueElement != null)
    {
      try
      {
        clientHelloResultMessage =
             valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client hello result " +
                                 "message:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_MAJOR_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Server hello message is missing the major " +
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
      throw new SLAMDException("Server hello message is missing the minor " +
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
      throw new SLAMDException("Server hello message is missing the point " +
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


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_AUTH_CREDENTIALS);
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
    buffer.append("clientHelloResultCode = ");
    buffer.append(clientHelloResultCode);
    buffer.append(" (");
    buffer.append(Constants.responseCodeToString(clientHelloResultCode));
    buffer.append(')');
    buffer.append(Constants.EOL);

    if (clientHelloResultMessage != null)
    {
      buffer.append(indentBuf);
      buffer.append("clientHelloResultMessage = ");
      buffer.append(clientHelloResultMessage);
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

    if (authMethod != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("authMethod = ");
      buffer.append(authMethod);
    }

    if (authID != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("authID = ");
      buffer.append(authID);
    }

    if (authCredentials != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("authCredentials = byte[");
      buffer.append(authCredentials.length);
      buffer.append(']');
    }
  }
}

