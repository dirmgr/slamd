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
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the server to a
 * client whenever the server is about to close that client connection.  It can
 * include a disconnect reason, an indication as to whether the disconnect is
 * permanent or transient (e.g., if the server is being restarted and should be
 * back up shortly), and can serve as an indication to the client that it needs
 * to close its connection to the server in the very near future, optionally
 * sending any job results first.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerDisconnect
       extends SLAMDMessage
{
  // Indicates whether the actual closure should be initiated by the client, so
  // that it can potentially finish what it's doing and send any results to the
  // server.
  private boolean clientShouldClose;

  // Indicates whether the disconnect is permanent or transient.
  private boolean isTransient;

  // The reason that the connection needs to be closed.
  private String disconnectReason;



  /**
   * Creates a new instance of this server disconnect message which is intended
   * for use in decoding a message transmitted between the server and the
   * client.  It is not intended for general use.
   */
  public ServerDisconnect()
  {
    super();

    disconnectReason  = null;
    isTransient       = false;
    clientShouldClose = false;
  }



  /**
   * Creates a new instance of this server disconnect message with the provided
   * information.
   *
   * @param  messageID          The message ID for this SLAMD message.
   * @param  extraProperties    The "extra" properties for this SLAMD message.
   *                            Both the names and values for the properties
   *                            must be strings.
   * @param  disconnectReason   The reason that the server has initiated the
   *                            disconnect process.
   * @param  isTransient        Indicates whether the disconnect is transient
   *                            (meaning that the server connection should be
   *                            available again shortly) or permanent.
   * @param  clientShouldClose  Indicates whether the actual connection closure
   *                            should be performed by the client, which may
   *                            allow it time to send any results that it may
   *                            have to the server.
   */
  public ServerDisconnect(int messageID, HashMap<String,String> extraProperties,
                          String disconnectReason, boolean isTransient,
                          boolean clientShouldClose)
  {
    super(messageID, extraProperties);

    this.disconnectReason  = disconnectReason;
    this.isTransient       = isTransient;
    this.clientShouldClose = clientShouldClose;
  }



  /**
   * Retrieves the reason that the server has initiated the disconnect process.
   *
   * @return  The reason that the server has initiated the disconnect process,
   *          or <CODE>null</CODE> if none was provided.
   */
  public String getDisconnectReason()
  {
    return disconnectReason;
  }



  /**
   * Specifies the reason that the server has initiated the disconnect process.
   *
   * @param  disconnectReason  The reason that the server has initiated the
   *                           disconnect process.
   */
  public void setDisconnectReason(String disconnectReason)
  {
    this.disconnectReason = disconnectReason;
  }



  /**
   * Indicates whether the disconnect is transient (meaning that the server
   * connection should be available again shortly, for example if the server is
   * being restarted) or permanent.  If the disconnect is transient, then the
   * client may be able to re-establish the connection after a short delay.
   *
   * @return  <CODE>true</CODE> if the disconnect is transient, or
   *          <CODE>false</CODE> if it is not.
   */
  public boolean isTransient()
  {
    return isTransient;
  }



  /**
   * Specifies whether the disconnect is transient.
   *
   * @param  isTransient  Specifies whether the disconnect is transient.
   */
  public void setTransient(boolean isTransient)
  {
    this.isTransient = isTransient;
  }



  /**
   * Indicates whether the actual disconnect should be initiated by the client.
   * If this is the case, then the client may have a short period of time during
   * which it may send any results that it may have to the server.  If this
   * returns <CODE>false</CODE>, then the client should assume that the server
   * will immediately terminate the connection.  If it returns
   * <CODE>true</CODE>, then the server may still close the connection if the
   * client does not do so in a sufficiently short period of time.
   *
   * @return  <CODE>true</CODE> if the actual closure should be done by the
   *          client, or <CODE>false</CODE> if it will be done by the server.
   */
  public boolean clientShouldClose()
  {
    return clientShouldClose;
  }



  /**
   * Specifies whether the actual disconnect should be initiated by the client.
   *
   * @param  clientShouldClose  Specifies whether the actual disconnect should
   *                            be initiated by the client.
   */
  public void setClientShouldClose(boolean clientShouldClose)
  {
    this.clientShouldClose = clientShouldClose;
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

    if (disconnectReason != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_DISCONNECT_REASON,
                           new ASN1OctetString(disconnectReason)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_DISCONNECT_IS_TRANSIENT,
                         new ASN1Boolean(isTransient)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_CLIENT_SHOULD_CLOSE,
                         new ASN1Boolean(clientShouldClose)));

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
         propertyMap.get(ProtocolConstants.PROPERTY_DISCONNECT_REASON);
    if (valueElement != null)
    {
      try
      {
        disconnectReason = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the disconnect reason:  " +
                                 e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_DISCONNECT_IS_TRANSIENT);
    if (valueElement != null)
    {
      try
      {
        isTransient = valueElement.decodeAsBoolean().getBooleanValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the isTransient flag:  " + e,
                                 e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_SHOULD_CLOSE);
    if (valueElement != null)
    {
      try
      {
        clientShouldClose = valueElement.decodeAsBoolean().getBooleanValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the clientShouldClose " +
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

    if (disconnectReason != null)
    {
      buffer.append(indentBuf);
      buffer.append("disconnectReason = ");
      buffer.append(disconnectReason);
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("isTransient = ");
    buffer.append(isTransient);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("clientShouldClose = ");
    buffer.append(clientShouldClose);
  }
}

