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
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;



/**
 * The class defines a SLAMD message that may be sent from a client to the
 * server immediately before the client closes the connection.  It can include a
 * disconnect reason.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientDisconnect
       extends SLAMDMessage
{
  // The reason that the connection needs to be closed.
  private String disconnectReason;



  /**
   * Creates a new instance of this client disconnect message which is intended
   * for use in decoding a message transmitted between the server and the
   * client.  It is not intended for general use.
   */
  public ClientDisconnect()
  {
    super();

    disconnectReason = null;
  }



  /**
   * Creates a new instance of this client disconnect message with the provided
   * information.
   *
   * @param  messageID         The message ID for this SLAMD message.
   * @param  extraProperties   The "extra" properties for this SLAMD message.
   *                           Both the names and values for the properties must
   *                           be strings.
   * @param  disconnectReason  The reason that the client has initiated the
   *                           disconnect process.
   */
  public ClientDisconnect(int messageID, HashMap<String,String> extraProperties,
                          String disconnectReason)
  {
    super(messageID, extraProperties);

    this.disconnectReason = disconnectReason;
  }



  /**
   * Retrieves the reason that the client has initiated the disconnect process.
   *
   * @return  The reason that the client has initiated the disconnect process,
   *          or <CODE>null</CODE> if none was provided.
   */
  public String getDisconnectReason()
  {
    return disconnectReason;
  }



  /**
   * Specifies the reason that the client has initiated the disconnect process.
   *
   * @param  disconnectReason  The reason that the client has initiated the
   *                           disconnect process.
   */
  public void setDisconnectReason(String disconnectReason)
  {
    this.disconnectReason = disconnectReason;
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
    }
  }
}

