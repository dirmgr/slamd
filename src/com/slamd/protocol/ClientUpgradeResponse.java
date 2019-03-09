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
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the server to the
 * client in response to an upgrade request.  It will include a result code, and
 * either the requested upgrade file or an error message.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientUpgradeResponse
       extends SLAMDMessage
{
  // The data for the requested upgrade file.
  private byte[] upgradeFileData;

  // The result code for the client upgrade operation.
  private int resultCode;

  // The error message for the client upgrade operation.
  private String errorMessage;



  /**
   * Creates a new instance of this client upgrade response message which is
   * intended for use in decoding a message transmitted between the server and
   * the client.  It is not intended for general use.
   */
  public ClientUpgradeResponse()
  {
    super();

    upgradeFileData = null;
    resultCode      = -1;
    errorMessage    = null;
  }



  /**
   * Creates a new instance of this client upgrade response message with the
   * provided information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  resultCode       The result code for the client upgrade operation.
   * @param  errorMessage     The error message for the client upgrade
   *                          operation.
   * @param  upgradeFileData  The data for the requested upgrade file.
   */
  public ClientUpgradeResponse(int messageID,
                               HashMap<String,String> extraProperties,
                               int resultCode, String errorMessage,
                               byte[] upgradeFileData)
  {
    super(messageID, extraProperties);

    this.resultCode      = resultCode;
    this.errorMessage    = errorMessage;
    this.upgradeFileData = upgradeFileData;
  }



  /**
   * Retrieves the result code for the client upgrade operation.
   *
   * @return  The result code for the client upgrade operation.
   */
  public int getResultCode()
  {
    return resultCode;
  }



  /**
   * Specifies the result code for the client upgrade operation.
   *
   * @param  resultCode  The result code for the client upgrade operation.
   */
  public void setResultCode(int resultCode)
  {
    this.resultCode = resultCode;
  }



  /**
   * Retrieves the error message for the client upgrade operation.
   *
   * @return  The error message for the client upgrade operation, or
   *          <CODE>null</CODE> if none was provided.
   */
  public String getErrorMessage()
  {
    return errorMessage;
  }



  /**
   * Specifies the error message for the client upgrade operation.
   *
   * @param  errorMessage  The error message for the client upgrade operation.
   */
  public void setErrorMessage(String errorMessage)
  {
    this.errorMessage = errorMessage;
  }



  /**
   * Retrieves the data for the requested upgrade file.
   *
   * @return  The data for the requested upgrade file, or <CODE>null</CODE> if
   *          it was not provided.
   */
  public byte[] getUpgradeFileData()
  {
    return upgradeFileData;
  }



  /**
   * Specifies the data for the requested upgrade file.
   *
   * @param  upgradeFileData  The data for the requested upgrade file.
   */
  public void setUpgradeFileData(byte[] upgradeFileData)
  {
    this.upgradeFileData = upgradeFileData;
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

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_RESULT_CODE,
                                        new ASN1Enumerated(resultCode)));

    if (errorMessage != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_RESULT_MESSAGE,
                           new ASN1OctetString(errorMessage)));
    }

    if (upgradeFileData != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_UPGRADE_FILE_DATA,
                           new ASN1OctetString(upgradeFileData)));
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
      throw new SLAMDException("Class transfer response message does not " +
                               "include a result code.");
    }
    else
    {
      try
      {
        resultCode = valueElement.decodeAsEnumerated().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the result code:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_RESULT_MESSAGE);
    if (valueElement != null)
    {
      try
      {
        errorMessage = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the error message:  " + e,
                                 e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_UPGRADE_FILE_DATA);
    if (valueElement != null)
    {
      try
      {
        upgradeFileData = valueElement.decodeAsOctetString().getValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the upgrade file data:  " +
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
    buffer.append("resultCode = ");
    buffer.append(resultCode);
    buffer.append(" (");
    buffer.append(Constants.responseCodeToString(resultCode));
    buffer.append(')');

    if (errorMessage != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("errorMessage = ");
      buffer.append(errorMessage);
    }

    if (upgradeFileData != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("upgradeFileData = byte[");
      buffer.append(upgradeFileData.length);
      buffer.append(']');
    }
  }
}

