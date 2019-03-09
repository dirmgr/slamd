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
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a stop client request message, which the SLAMD server
 * can use to request that one or more clients be stopped.
 *
 *
 * @author   Neil A. Wilson
 */
public class StopClientRequestMessage
       extends Message
{
  // The number of clients that should be stopped.
  private final int numClients;



  /**
   * Creates a new stop client request message that will provide the number
   * of clients to start and the port number of the SLAMD server's client
   * listener.
   *
   * @param  messageID  The message ID for this message.
   * @param  numClients The number of clients that should be stopped.
   */
  public StopClientRequestMessage(int messageID, int numClients)
  {
    super(messageID, Constants.MESSAGE_TYPE_STOP_CLIENT_REQUEST);

    this.numClients = numClients;
  }



  /**
   * Retrieves the number of clients that should be started.
   *
   * @return  The number of clients that should be started.
   */
  public int getNumClients()
  {
    return numClients;
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

    return "Stop Client Request Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Number of Clients:  " + numClients + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a stop client request message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the StopClientRequest
   *                    sequence.
   *
   * @return  The stop client request message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a stop client request message.
   */
  public static StopClientRequestMessage decodeStopClient(int messageID,
                                                          ASN1Element element)
         throws SLAMDException
  {
    int numClients = -1;
    try
    {
      numClients = element.decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The provided ASN.1 element cannot be decoded " +
                               "as an integer", ae);
    }


    return new StopClientRequestMessage(messageID, numClients);
  }



  /**
   * Encodes this message into an ASN.1 element.  A stop client request message
   * has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>StopClientRequest ::= [APPLICATION 17] INTEGER</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Element[] messageElements = new ASN1Element[]
    {
      new ASN1Integer(messageID),
      new ASN1Integer(ASN1_TYPE_STOP_CLIENT_REQUEST, numClients)
    };

    return new ASN1Sequence(messageElements);
  }
}

