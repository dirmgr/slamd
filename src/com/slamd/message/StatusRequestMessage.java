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
 * This class defines a message type that the server can use to request status
 * information from a client.  It can be a request about just the availability
 * of the client or also about the status of a job that may be in progress.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatusRequestMessage
       extends Message
{
  // The ID of the job about which information is being requested.
  private final String jobID;



  /**
   * Creates a new status request message that is intended only to determine the
   * current status of the client.
   *
   * @param  messageID  The message ID for this message.
   */
  public StatusRequestMessage(int messageID)
  {
    this(messageID, "");
  }



  /**
   * Creates a new status request message that is intended to find information
   * about the specified job.
   *
   * @param  messageID  The message ID for this message.
   * @param  jobID      The ID of the job for which to retrieve status
   *                    information.
   */
  public StatusRequestMessage(int messageID, String jobID)
  {
    super(messageID, Constants.MESSAGE_TYPE_STATUS_REQUEST);
    this.jobID = jobID;
  }



  /**
   * Retrieves the ID of the job for which to find status information.
   *
   * @return  The ID of the job for which to find status information.
   */
  public String getJobID()
  {
    return jobID;
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

    return "Status Request Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a status request message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the StatusRequest sequence.
   *
   * @return  The status request message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a status request message.
   */
  public static StatusRequestMessage decodeStatusRequest(int messageID,
                                               ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence requestSequence = null;
    try
    {
      requestSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the provided ASN.1 element " +
                               "as a sequence", ae);
    }


    ASN1Element[] elements = requestSequence.getElements();
    if (elements.length == 0)
    {
      return new StatusRequestMessage(messageID);
    }
    else if (elements.length != 1)
    {
      throw new SLAMDException("A status request may have at most one element");
    }


    String jobID = null;
    try
    {
      jobID = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the first element as an " +
                               "octet string", ae);
    }


    return new StatusRequestMessage(messageID, jobID);
  }



  /**
   * Encodes this message into an ASN.1 element.  A status request message has
   * the following syntax:
   * <BR><BR>
   * <CODE>StatusRequest ::= [APPLICATION 8] SEQUENCE {</CODE>
   * <CODE>    jobID           OCTET STRING OPTIONAL }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);
    ASN1Sequence statusRequestSequence =
        new ASN1Sequence(ASN1_TYPE_STATUS_REQUEST);

    if (! ((jobID == null) || (jobID.length() == 0)))
    {
      statusRequestSequence.addElement(new ASN1OctetString(jobID));
    }

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      statusRequestSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

