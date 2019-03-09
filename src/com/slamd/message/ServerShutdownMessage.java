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
import com.slamd.asn1.ASN1Null;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a message type that the server uses to indicate that it
 * is shutting down and that the connection to the client will be terminated.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerShutdownMessage
       extends Message
{
  /**
   * Creates a new message type that the server uses to indicate that it is
   * shutting down.
   *
   * @param  messageID  The message ID for this message.
   */
  public ServerShutdownMessage(int messageID)
  {
    super(messageID, Constants.MESSAGE_TYPE_SERVER_SHUTDOWN);
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

    return "Server Shutdown Message" + eol +
           "  Message ID:  " + messageID + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a server shutdown message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 server shutdown element.
   *
   * @return  The server shutdown message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a server shutdown message.
   */
  public static ServerShutdownMessage decodeShutdown(int messageID,
                                                          ASN1Element element)
         throws SLAMDException
  {
    try
    {
      ASN1Null nullElement = element.decodeAsNull();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The server shutdown element cannot be " +
                               "decoded as a null", ae);
    }


    return new ServerShutdownMessage(messageID);
  }



  /**
   * Encodes this message into an ASN.1 element.  A server shutdown message has
   * the following syntax:
   * <BR><BR>
   * <CODE>ServerShutdown ::= [APPLICATION 10] NULL</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Null serverShutdownNull = new ASN1Null(ASN1_TYPE_SERVER_SHUTDOWN);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      serverShutdownNull
    };

    return new ASN1Sequence(messageElements);
  }
}

