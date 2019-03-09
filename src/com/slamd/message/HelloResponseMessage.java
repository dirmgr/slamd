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



import java.text.SimpleDateFormat;
import java.util.Date;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a message that will be sent as a response to a hello
 * message (either client or server hello).  It provides a response code that
 * indicates whether the application was successful or if not the reason that it
 * was not successful.  It may also optionally contain a message explaining the
 * reason for the failure.
 *
 *
 * @author   Neil A. Wilson
 */
public class HelloResponseMessage
       extends Message
{
  // The response code from processing the hello request
  private final int responseCode;

  // The current time on the server at the time the message was created.
  private final long serverTime;

  // An optional message that can provide more information about this response
  private final String responseMessage;



  /**
   * Creates a new hello response message with the specified response code.  It
   * will not contain a response message.
   *
   * @param  messageID     The message ID for this message.
   * @param  responseCode  The response code to include in this response
   *                       message.
   * @param  serverTime    The current time on the server at the time this
   *                       message was created.
   */
  public HelloResponseMessage(int messageID, int responseCode, long serverTime)
  {
    this(messageID, responseCode, "", serverTime);
  }



  /**
   * Creates a new hello response message with the specified response code and
   * message.
   *
   * @param  messageID        The message ID for this message.
   * @param  responseCode     The response code to include in this response
   *                          message.
   * @param  responseMessage  The text to include in this response message.
   * @param  serverTime       The current time on the server at the time this
   *                          message was created.
   */
  public HelloResponseMessage(int messageID, int responseCode,
                              String responseMessage, long serverTime)
  {
    super(messageID, Constants.MESSAGE_TYPE_HELLO_RESPONSE);

    this.responseCode    = responseCode;
    this.responseMessage = responseMessage;
    this.serverTime      = serverTime;
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
   * Retrieves the text message included in this hello response.
   *
   * @return  The text message included in this hello response.
   */
  public String getResponseMessage()
  {
    return responseMessage;
  }



  /**
   * Retrieves the current time on the server at the time this response was
   * created.
   *
   * @return  The current time on the server at the time this response was
   *          created, or -1 if the client does not support time
   *          synchronization.
   */
  public long getServerTime()
  {
    return serverTime;
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

    if (serverTime > 0)
    {
      SimpleDateFormat dateFormat =
           new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);

      return "Hello Response Message" + eol +
             "  Message ID:  " + messageID + eol +
             "  Response Code:  " + responseCode + eol +
             "  Response Message:  " + responseMessage + eol +
             "  Server Time:  " + dateFormat.format(new Date(serverTime)) + eol;
    }
    else
    {
      return "Hello Response Message" + eol +
             "  Message ID:  " + messageID + eol +
             "  Response Code:  " + responseCode + eol +
             "  Response Message:  " + responseMessage + eol;
    }
  }



  /**
   * Decodes the provided ASN.1 element as a hello response message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the HelloResponse sequence.
   *
   * @return  The hello response message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a hello response message.
   */
  public static HelloResponseMessage decodeHelloResponse(int messageID,
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
    if ((elements.length < 2) || (elements.length > 3))
    {
      throw new SLAMDException("A hello response message must have two or " +
                               "three elements");
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

    long serverTime = -1;
    if (elements.length == 3)
    {
      try
      {
        String timeStr = elements[2].decodeAsOctetString().getStringValue();
        serverTime = Long.parseLong(timeStr);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Could not decode the third element as the " +
                                 "server time", e);
      }
    }

    return new HelloResponseMessage(messageID, responseCode, responseMessage,
                                    serverTime);
  }



  /**
   * Encodes this message into an ASN.1 element.  A hello response message has
   * the following syntax:
   * <BR><BR>
   * <CODE>HelloResponse ::= [APPLICATION 2] SEQUENCE {</CODE>
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

    ASN1Element[] helloResponseElements;
    if (serverTime > 0)
    {
      helloResponseElements = new ASN1Element[]
      {
        responseCodeElement,
        responseMessageElement,
        new ASN1OctetString(String.valueOf(serverTime))
      };
    }
    else
    {
      helloResponseElements = new ASN1Element[]
      {
        responseCodeElement,
        responseMessageElement
      };
    }

    ASN1Sequence helloResponseSequence =
         new ASN1Sequence(ASN1_TYPE_HELLO_RESPONSE, helloResponseElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      helloResponseSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

