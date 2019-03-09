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
 * This class defines a start client request message, which the SLAMD server
 * can use to request that one or more clients be started.
 *
 *
 * @author   Neil A. Wilson
 */
public class StartClientRequestMessage
       extends Message
{
  // The number of clients that should be started.
  private final int numClients;

  // The port number of the SLAMD server's client listener.
  private final int serverPort;



  /**
   * Creates a new start client request message that will provide the number
   * of clients to start and the port number of the SLAMD server's client
   * listener.
   *
   * @param  messageID   The message ID for this message.
   * @param  numClients  The number of clients that should be started.
   * @param  serverPort  The port number of the SLAMD client listener.
   */
  public StartClientRequestMessage(int messageID, int numClients,
                                   int serverPort)
  {
    super(messageID, Constants.MESSAGE_TYPE_START_CLIENT_REQUEST);

    this.numClients = numClients;
    this.serverPort = serverPort;
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
   * Retrieves the port number of the SLAMD server's client listener.
   *
   * @return  The port number of the SLAMD server's client listener.
   */
  public int getServerPort()
  {
    return serverPort;
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

    return "Start Client Request Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Number of Clients:  " + numClients + eol +
           "  Server Port:  " + serverPort + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a start client request message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the StartClientRequest
   *                    sequence.
   *
   * @return  The start client request message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a start client request message.
   */
  public static StartClientRequestMessage decodeStartClient(int messageID,
                                                            ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence startClientSequence = null;
    try
    {
      startClientSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The provided ASN.1 element cannot be decoded " +
                               "as a sequence", ae);
    }

    ASN1Element[] elements = startClientSequence.getElements();
    if (elements.length != 2)
    {
      throw new SLAMDException("There must be 2 elements in a start client " +
                               "request message");
    }

    int numClients;
    try
    {
      numClients = elements[0].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode first element as an integer",
                               ae);
    }

    int serverPort;
    try
    {
      serverPort = elements[1].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode second element as an integer",
                               ae);
    }


    return new StartClientRequestMessage(messageID, numClients, serverPort);
  }



  /**
   * Encodes this message into an ASN.1 element.  A start client request message
   * has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>StartClientRequest ::= [APPLICATION 15] SEQUENCE {</CODE>
   * <CODE>    numClients         INTEGER,</CODE>
   * <CODE>    serverPort         INTEGER }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Element[] startClientElements = new ASN1Element[]
    {
      new ASN1Integer(numClients),
      new ASN1Integer(serverPort)
    };

    ASN1Sequence startClientSequence =
         new ASN1Sequence(ASN1_TYPE_START_CLIENT_REQUEST, startClientElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      startClientSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

