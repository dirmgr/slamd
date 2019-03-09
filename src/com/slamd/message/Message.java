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



import java.io.IOException;
import java.io.ObjectOutputStream;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;



/**
 * This class defines a generic message that can be sent between the client and
 * the server to request some operation or provide information.  Subclasses will
 * define the actual messages that may be sent.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class Message
{
  /**
   * The ASN.1 type for a client hello message.
   */
  public static final byte ASN1_TYPE_CLIENT_HELLO = 0x60;



  /**
   * The ASN.1 type for a server hello message.
   */
  public static final byte ASN1_TYPE_SERVER_HELLO = 0x61;



  /**
   * The ASN.1 type for a hello response message.
   */
  public static final byte ASN1_TYPE_HELLO_RESPONSE = 0x62;



  /**
   * The ASN.1 type for a job request messages.
   */
  public static final byte ASN1_TYPE_JOB_REQUEST = 0x63;



  /**
   * The ASN.1 type for a job response message.
   */
  public static final byte ASN1_TYPE_JOB_RESPONSE = 0x64;



  /**
   * The ASN.1 type for a job control request message.
   */
  public static final byte ASN1_TYPE_JOB_CONTROL_REQUEST = 0x65;



  /**
   * The ASN.1 type for a job control response message.
   */
  public static final byte ASN1_TYPE_JOB_CONTROL_RESPONSE = 0x66;



  /**
   * The ASN.1 type for a job completed message.
   */
  public static final byte ASN1_TYPE_JOB_COMPLETED = 0x67;



  /**
   * The ASN.1 type for a status request message.
   */
  public static final byte ASN1_TYPE_STATUS_REQUEST = 0x68;



  /**
   * The ASN.1 type for a status response message.
   */
  public static final byte ASN1_TYPE_STATUS_RESPONSE = 0x69;



  /**
   * The ASN.1 type for a server shutdown message.
   */
  public static final byte ASN1_TYPE_SERVER_SHUTDOWN = 0x4A;



  /**
   * The ASN.1 type for a keepalive message.
   */
  public static final byte ASN1_TYPE_KEEPALIVE = 0x4B;



  /**
   * The ASN.1 type for a file transfer request message.
   */
  public static final byte ASN1_TYPE_CLASS_TRANSFER_REQUEST = 0x4C;



  /**
   * The ASN.1 type for a file transfer response message.
   */
  public static final byte ASN1_TYPE_CLASS_TRANSFER_RESPONSE = 0x6D;



  /**
   * The ASN.1 type for a client manager hello message.
   */
  public static final byte ASN1_TYPE_CLIENT_MANAGER_HELLO = 0x6E;



  /**
   * The ASN.1 type for a start client request.
   */
  public static final byte ASN1_TYPE_START_CLIENT_REQUEST = 0x6F;



  /**
   * The ASN.1 type for a start client response.
   */
  public static final byte ASN1_TYPE_START_CLIENT_RESPONSE = 0x70;



  /**
   * The ASN.1 type for a stop client request.
   */
  public static final byte ASN1_TYPE_STOP_CLIENT_REQUEST = 0x51;



  /**
   * The ASN.1 type for a stop client response.
   */
  public static final byte ASN1_TYPE_STOP_CLIENT_RESPONSE = 0x72;



  /**
   * The ASN.1 type for a register stat message.
   */
  public static final byte ASN1_TYPE_REGISTER_STAT = 0x73;



  /**
   * The ASN.1 type for a report stat message.
   */
  public static final byte ASN1_TYPE_REPORT_STAT = 0x74;



  /**
   * A unique (per connection) identifier that is included in both a request and
   * a response to help determine which responses are associated with which
   * requests.
   */
  protected final int messageID;

  // An indicator that allows the client and server to determine what kind of
  // message this is.
  private final int messageType;



  /**
   * Creates a new generic message of the specified type.
   *
   * @param  messageID    The message ID for this message.
   * @param  messageType  The message type.
   */
  protected Message(int messageID, int messageType)
  {
    this.messageID   = messageID;
    this.messageType = messageType;
  }



  /**
   * Retrieves the message ID for this message.
   *
   * @return  The message ID for this message.
   */
  public int getMessageID()
  {
    return messageID;
  }



  /**
   * Retrieves the message type for this message.
   *
   * @return  The message type for this message.
   */
  public int getMessageType()
  {
    return messageType;
  }



  /**
   * Sends this message over the provided output stream.  This can be used by
   * either the client or the server to send the message to the other side.
   *
   * @param  objectOutputStream  The output stream over which to send the
   *                             message.
   *
   * @throws  IOException  If there is a problem sending the message over the
   *                       output stream.
   */
  public void send(ObjectOutputStream objectOutputStream)
         throws IOException
  {
    objectOutputStream.writeObject(this);
    objectOutputStream.flush();
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

    return "SLAMD Message" + eol +
           "  Message Type:  " + messageType + eol +
           "  Message ID:  " + messageID + eol;
  }



  /**
   * Encodes this message into an ASN.1 element.
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  public abstract ASN1Element encode();



  /**
   * Decodes the provided ASN.1 element as a message.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded version of the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided element cannot be decoded to a
   *                          valid message.
   */
  public static Message decode(ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence messageSequence = null;
    try
    {
      messageSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the provided ASN.1 element " +
                               "as a sequence:  " + ae, ae);
    }

    ASN1Element[] elements = messageSequence.getElements();
    if (elements.length != 2)
    {
      throw new SLAMDException("There must be 2 elements in a SLAMD message " +
                               "sequence -- got " + elements.length +
                               " elements");
    }

    int messageID = 0;
    try
    {
      messageID = elements[0].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the message ID element as " +
                               "an integer", ae);
    }


    switch (elements[1].getType())
    {
      case ASN1_TYPE_CLIENT_HELLO:
           return ClientHelloMessage.decodeClientHello(messageID,
                                                            elements[1]);
      case ASN1_TYPE_SERVER_HELLO:
           return ServerHelloMessage.decodeServerHello(messageID,
                                                            elements[1]);
      case ASN1_TYPE_HELLO_RESPONSE:
           return HelloResponseMessage.decodeHelloResponse(messageID,
                                                                elements[1]);
      case ASN1_TYPE_JOB_REQUEST:
           return JobRequestMessage.decodeJobRequest(messageID,
                                                          elements[1]);
      case ASN1_TYPE_JOB_RESPONSE:
           return JobResponseMessage.decodeJobResponse(messageID,
                                                            elements[1]);
      case ASN1_TYPE_JOB_CONTROL_REQUEST:
           return
                JobControlRequestMessage.decodeJobControlRequest(
                                                   messageID, elements[1]);
      case ASN1_TYPE_JOB_CONTROL_RESPONSE:
           return
                JobControlResponseMessage.decodeJobControResponse(
                                                    messageID, elements[1]);
      case ASN1_TYPE_JOB_COMPLETED:
           return JobCompletedMessage.decodeJobCompleted(messageID,
                                                              elements[1]);
      case ASN1_TYPE_STATUS_REQUEST:
           return StatusRequestMessage.decodeStatusRequest(messageID,
                                                                elements[1]);
      case ASN1_TYPE_STATUS_RESPONSE:
           return StatusResponseMessage.decodeStatusResponse(messageID,
                                                                  elements[1]);
      case ASN1_TYPE_SERVER_SHUTDOWN:
           return ServerShutdownMessage.decodeShutdown(messageID,
                                                            elements[1]);
      case ASN1_TYPE_KEEPALIVE:
           return KeepAliveMessage.decodeKeepAlive(messageID, elements[1]);
      case ASN1_TYPE_CLASS_TRANSFER_REQUEST:
           return
                ClassTransferRequestMessage.decodeTransferRequest(messageID,
                                                                  elements[1]);
      case ASN1_TYPE_CLASS_TRANSFER_RESPONSE:
           return ClassTransferResponseMessage.
                       decodeTransferResponse(messageID, elements[1]);
      case ASN1_TYPE_CLIENT_MANAGER_HELLO:
           return
                ClientManagerHelloMessage.decodeClientManagerHello(messageID,
                                                                   elements[1]);
      case ASN1_TYPE_START_CLIENT_REQUEST:
           return StartClientRequestMessage.decodeStartClient(messageID,
                                                              elements[1]);
      case ASN1_TYPE_START_CLIENT_RESPONSE:
           return StartClientResponseMessage.
                       decodeStartClientResponse(messageID, elements[1]);
      case ASN1_TYPE_STOP_CLIENT_REQUEST:
           return StopClientRequestMessage.decodeStopClient(messageID,
                                                            elements[1]);
      case ASN1_TYPE_STOP_CLIENT_RESPONSE:
           return
                StopClientResponseMessage.decodeStopClientResponse(messageID,
                                                                   elements[1]);
      case ASN1_TYPE_REGISTER_STAT:
           return RegisterStatisticMessage.
                       decodeRegisterStatMessage(messageID, elements[1]);
      case ASN1_TYPE_REPORT_STAT:
           return ReportStatisticMessage.decodeReportStatMessage(messageID,
                                                                 elements[1]);
      default:
           throw new SLAMDException("Unknown message body element type:  " +
                                    elements[1].getType());
    }
  }
}

