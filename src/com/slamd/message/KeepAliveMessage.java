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
 * This class defines a keepalive message that is used to ensure that a
 * connection is still alive, and in some cases to ensure that the connection
 * does not get closed (e.g., if a firewall between the client and server
 * automatically closes idle connections).
 *
 *
 * @author   Neil A. Wilson
 */
public class KeepAliveMessage
       extends Message
{
  /**
   * Creates a new keepalive message.
   *
   * @param  messageID          The message ID for this message.
   */
  public KeepAliveMessage(int messageID)
  {
    super(messageID, Constants.MESSAGE_TYPE_KEEPALIVE);
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

    return "KeepAlive Message" + eol +
           "  Message ID:  " + messageID + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a keepalive message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 keepalive element.
   *
   * @return  The keepalive message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a keepalive message.
   */
  public static KeepAliveMessage decodeKeepAlive(int messageID,
                                                      ASN1Element element)
         throws SLAMDException
  {
    try
    {
      ASN1Null nullElement = element.decodeAsNull();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The keepalive element cannot be decoded " +
                               "as a null", ae);
    }


    return new KeepAliveMessage(messageID);
  }



  /**
   * Encodes this message into an ASN.1 element.  A keepalive message has the
   * following syntax:
   * <BR><BR>
   * <CODE>KeepAlive ::= [APPLICATION 11] NULL</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Null keepAliveNull = new ASN1Null(ASN1_TYPE_KEEPALIVE);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      keepAliveNull
    };

    return new ASN1Sequence(messageElements);
  }
}

