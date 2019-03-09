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
 * This class defines a message that will be provided in response to a job
 * control request.  It specifies whether the requested operation was performed
 * and, if not, the reason that it was not performed.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobControlResponseMessage
       extends Message
{
  // The response code that indicates whether an operation was successful and/or
  // the reason that it was not
  private final int responseCode;

  // The ID of the job for which the operation was requested
  private final String jobID;

  // A text-based message that may provide additional information about the
  // status of the job control operation
  private final String responseMessage;



  /**
   * Creates a new job control response message with the specified job ID and
   * response code.  There will not be an additional response message.
   *
   * @param  messageID     The message ID for this message.
   * @param  jobID         The ID of the job for which the job control request
   *                       was made.
   * @param  responseCode  The response code that indicates whether an operation
   *                       was successful and/or the reason that it was not.
   */
  public JobControlResponseMessage(int messageID, String jobID,
                                        int responseCode)
  {
    this(messageID, jobID, responseCode, "");
  }



  /**
   * Creates a new job control response message with the specified job ID and
   * response code.  There will not be an additional response message.
   *
   * @param  messageID        The message ID for this message.
   * @param  jobID            The ID of the job for which the job control
   *                          request was made.
   * @param  responseCode     The response code that indicates whether an
   *                          operation was successful and/or the reason that it
   *                          was not.
   * @param  responseMessage  The text message that provides additional
   *                          information about the status of the job control
   *                          operation.
   */
  public JobControlResponseMessage(int messageID, String jobID,
                                        int responseCode,
                                        String responseMessage)
  {
    super(messageID, Constants.MESSAGE_TYPE_JOB_CONTROL_RESPONSE);

    this.jobID           = jobID;
    this.responseCode    = responseCode;
    this.responseMessage = responseMessage;
  }



  /**
   * Retrieves the ID of the job with which this job control response is
   * associated.
   *
   * @return  The ID of the job with which this job control response is
   *          associated.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the response code associated with this job control response.
   *
   * @return  The response code associated with this job control response.
   */
  public int getResponseCode()
  {
    return responseCode;
  }



  /**
   * Retrieves the response message associated with this job control response.
   *
   * @return  The response message associated with this job control response.
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

    return "Job Control Response Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Response Code:  " + responseCode + eol +
           "  Response Message:  " + responseMessage + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a job control response message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the JobControlResponse
   *                    sequence.
   *
   * @return  The job control response decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a job control response message.
   */
  public static JobControlResponseMessage
                     decodeJobControResponse(int messageID, ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence responseSequence = null;
    try
    {
      responseSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the ASN.1 element as a " +
                               "sequence", ae);
    }


    ASN1Element[] elements = responseSequence.getElements();
    if (elements.length != 3)
    {
      throw new SLAMDException("There must be three elements in a job " +
                               "control response");
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


    int responseCode = 0;
    try
    {
      responseCode = elements[1].decodeAsEnumerated().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the second element as an " +
                               "enumerated", ae);
    }


    String responseMessage = null;
    try
    {
      responseMessage = elements[2].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the third element as an " +
                               "octet string", ae);
    }


    return new JobControlResponseMessage(messageID, jobID, responseCode,
                                              responseMessage);
  }



  /**
   * Encodes this message into an ASN.1 element.  A job control response message
   * has the following syntax:
   * <BR><BR>
   * <CODE>JobControlResponse ::= [APPLICATION 6] SEQUENCE {</CODE>
   * <CODE>    jobID            OCTET STRING,</CODE>
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

    ASN1OctetString jobIDElement = new ASN1OctetString(jobID);
    ASN1Integer responseCodeElement = new ASN1Integer(responseCode);
    ASN1OctetString responseMessageElement =
         new ASN1OctetString(responseMessage);

    ASN1Element[] jobControlResponseElements = new ASN1Element[]
    {
      jobIDElement,
      responseCodeElement,
      responseMessageElement
    };

    ASN1Sequence jobControlResponseSequence =
         new ASN1Sequence(ASN1_TYPE_JOB_CONTROL_RESPONSE,
                          jobControlResponseElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      jobControlResponseSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

