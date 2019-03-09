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
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the server to the
 * client manager in order to request that the client manager create one or more
 * new client instances for use in running jobs.
 *
 *
 * @author   Neil A. Wilson
 */
public class StartClientRequest
       extends SLAMDMessage
{
  // The number of new client instances that should be created.
  private int numClients;



  /**
   * Creates a new instance of this start client request message which is
   * intended for use in decoding a message transmitted between the server and
   * the client. It is not intended for general use.
   */
  public StartClientRequest()
  {
    super();

    numClients = -1;
  }



  /**
   * Creates a new instance of this start client request message with the
   * provided information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  numClients       The number of new client instances that should be
   *                          created.
   */
  public StartClientRequest(int messageID,
                            HashMap<String,String> extraProperties,
                            int numClients)
  {
    super(messageID, extraProperties);

    this.numClients = numClients;
  }



  /**
   * Retrieves the number of new client instances that should be created.
   *
   * @return  The number of new client instances that should be created.
   */
  public int getNumClients()
  {
    return numClients;
  }



  /**
   * Specifies the number of client instances that should be created.
   *
   * @param  numClients  The number of client instances that should be created.
   */
  public void setNumClients(int numClients)
  {
    this.numClients = numClients;
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

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_NUM_CLIENTS,
                                        new ASN1Integer(numClients)));

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
         propertyMap.get(ProtocolConstants.PROPERTY_NUM_CLIENTS);
    if (valueElement == null)
    {
      throw new SLAMDException("Start client request message does not " +
                               "include the number of clients to create.");
    }
    else
    {
      try
      {
        numClients = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the number of clients to " +
                                 "create:  " + e, e);
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
    buffer.append("numClients = ");
    buffer.append(numClients);
  }
}

