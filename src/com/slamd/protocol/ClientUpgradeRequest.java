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
 * This class defines a SLAMD message that will be sent from the client to the
 * server whenever the server version is newer than the client version.  This
 * will prompt the server to send the client a JAR file containing the updated
 * client code.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientUpgradeRequest
       extends SLAMDMessage
{
  // The filename (without any path information) for the upgrade JAR file.
  private String upgradeFile;



  /**
   * Creates a new instance of this client upgrade request message which is
   * intended for use in decoding a message transmitted between the server and
   * the client.  It is not intended for general use.
   */
  public ClientUpgradeRequest()
  {
    super();

    upgradeFile = null;
  }



  /**
   * Creates a new instance of this client upgrade request message with the
   * provided information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  upgradeFile      The filename (without any path information) for
   *                          the upgrade JAR file being requested.
   */
  public ClientUpgradeRequest(int messageID,
                              HashMap<String,String> extraProperties,
                              String upgradeFile)
  {
    super(messageID, extraProperties);

    this.upgradeFile = upgradeFile;
  }



  /**
   * Retrieves the filename (without any path information) for the upgrade JAR
   * file being requested.
   *
   * @return  The filename for the upgrade JAR file being requested.
   */
  public String getUpgradeFile()
  {
    return upgradeFile;
  }



  /**
   * Specifies the filename (without any path information) for the upgrade JAR
   * file being requested.
   *
   * @param  upgradeFile  The filename for the upgrade JAR file being requested.
   */
  public void setUpgradeFile(String upgradeFile)
  {
    this.upgradeFile = upgradeFile;
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
                         ProtocolConstants.PROPERTY_UPGRADE_FILE_NAME,
                         new ASN1OctetString(upgradeFile)));

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
         propertyMap.get(ProtocolConstants.PROPERTY_UPGRADE_FILE_NAME);
    if (valueElement == null)
    {
      throw new SLAMDException("Client upgrade request does not include the " +
                               "filename for the upgrade file.");
    }
    else
    {
      try
      {
        upgradeFile = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the requested upgrade " +
                                 "file:  " + e, e);
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
    buffer.append("upgradeFile = ");
    buffer.append(upgradeFile);
  }
}

