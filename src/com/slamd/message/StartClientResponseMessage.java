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
 * This class defines a message that will be sent as a response to a start
 * client message.  It provides a response code that indicates whether the
 * request was processed successfully, or if not the reason that it was not
 * successful.  It may also optionally contain a message explaining the reason
 * for the failure.
 *
 *
 * @author   Neil A. Wilson
 */
public class StartClientResponseMessage
       extends Message
{
  // The response code from processing the start client request
  private final int responseCode;

  // An optional message that can provide more information about this response
  private final String responseMessage;



  /**
   * Creates a new start client response message with the specified response
   * code.  It will not contain a response message.
   *
   * @param  messageID     The message ID for this message.
   * @param  responseCode  The response code to include in this response
   *                       message.
   */
  public StartClientResponseMessage(int messageID, int responseCode)
  {
    this(messageID, responseCode, "");
  }



  /**
   * Creates a new start client response message with the specified response
   * code and message.
   *
   * @param  messageID        The message ID for this message.
   * @param  responseCode     The response code to include in this response
   *                          message.
   * @param  responseMessage  The text to include in this response message.
   */
  public StartClientResponseMessage(int messageID, int responseCode,
                                    String responseMessage)
  {
    super(messageID, Constants.MESSAGE_TYPE_START_CLIENT_RESPONSE);

    this.responseCode    = responseCode;
    this.responseMessage = responseMessage;
  }



  /**
   * Retrieves the response code associated with this message.  The value will
   * be that of one of the MESSAGE_RESPONSE_* constants.
   *
   * @return  The response code associated with this response message.
   */
  public int getResponseCode()
  {
    return responseCode;
  }



  /**
   * Retrieves the text message included in this start client response.
   *
   * @return  The text message included in this start client response.
   */
  public String getResponseMessage()
  {
    return responseMessage;
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

    return "Start Client Response Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Response Code:  " + responseCode + eol +
           "  Response Message:  " + responseMessage + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a start client response message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the StartClientResponse
   *                    sequence.
   *
   * @return  The start client response message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a start client response message.
   */
  public static StartClientResponseMessage
                     decodeStartClientResponse(int messageID,
                                               ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence responseSequence = null;
    try
    {
      responseSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode ASN.1 element as a sequence",
                               ae);
    }


    ASN1Element[] elements = responseSequence.getElements();
    if (elements.length != 2)
    {
      throw new SLAMDException("A start client response message must have " +
                               "two elements");
    }

    int responseCode = 0;
    try
    {
      responseCode = elements[0].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the first element as an " +
                               "integer", ae);
    }

    String responseMessage = null;
    try
    {
      responseMessage = elements[1].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the second element as an " +
                               "octet string", ae);
    }

    return new StartClientResponseMessage(messageID, responseCode,
                                          responseMessage);
  }



  /**
   * Encodes this message into an ASN.1 element.  A start client response
   * message has the following syntax:
   * <BR><BR>
   * <CODE>StartClientResponse ::= [APPLICATION 16] SEQUENCE {</CODE>
   * <CODE>    responseCode     ResponseCode,</CODE>
   * <CODE>    responseMessage  OCTET STRING }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Integer responseCodeElement = new ASN1Integer(responseCode);
    ASN1OctetString responseMessageElement =
         new ASN1OctetString(responseMessage);

    ASN1Element[] startClientResponseElements = new ASN1Element[]
    {
      responseCodeElement,
      responseMessageElement
    };

    ASN1Sequence startClientResponseSequence =
         new ASN1Sequence(ASN1_TYPE_START_CLIENT_RESPONSE,
                          startClientResponseElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      startClientResponseSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

