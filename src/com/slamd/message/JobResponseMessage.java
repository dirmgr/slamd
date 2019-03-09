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
 * This class defines a method that will be provided in response to a job
 * request.  It specifies whether the request was accepted and, if not, the
 * reason that it was rejected.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobResponseMessage
       extends Message
{
  // The response code that indicates if the job request was successful and/or
  // why it failed.
  private final int responseCode;

  // The job ID of the job for which the request was made.
  private final String jobID;

  // The text message providing additional information not contained in the
  // response code.
  private final String responseMessage;



  /**
   * Creates a new job response message with the specified response code.  There
   * will not be any additional message included.
   *
   * @param  messageID     The message ID for this message.
   * @param  jobID         The ID of the job for which the request was made.
   * @param  responseCode  The response code that indicates whether the
   *                       operation was successful and if not provides the
   *                       reason that it failed.
   */
  public JobResponseMessage(int messageID, String jobID, int responseCode)
  {
    this(messageID, jobID, responseCode, "");
  }



  /**
   * Creates a new job response message with the specified response code and
   * message.
   *
   * @param  messageID        The message ID for this message.
   * @param  jobID            The ID of the job for which the request was made.
   * @param  responseCode     The response code that indicates whether the
   *                          operation was successful and if not provides the
   *                          reason that it failed.
   * @param  responseMessage  A text message providing additional information
   *                          about the status of the operation.
   */
  public JobResponseMessage(int messageID, String jobID, int responseCode,
                                 String responseMessage)
  {
    super(messageID, Constants.MESSAGE_TYPE_JOB_RESPONSE);

    this.jobID           = jobID;
    this.responseCode    = responseCode;
    this.responseMessage = responseMessage;
  }



  /**
   * Retrieves the ID of the job for which the request was made.
   *
   * @return  The ID of the job for which the request was made.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the response code for the job request.
   *
   * @return  The response code for the job request.
   */
  public int getResponseCode()
  {
    return responseCode;
  }



  /**
   * Retrieves the message associated with this job response.
   *
   * @return  The message associated with this job response.
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

    return "Job Response Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Response Code:  " + responseCode + eol +
           "  Response Message:  " + responseMessage + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a job response message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the JobResponse sequence.
   *
   * @return  The job response decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a job response message.
   */
  public static JobResponseMessage decodeJobResponse(int messageID,
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
      throw new SLAMDException("Could not decode the ASN.1 element as a " +
                               "sequence", ae);
    }


    ASN1Element[] elements = responseSequence.getElements();
    if (elements.length != 3)
    {
      throw new SLAMDException("There must be three elements in a job " +
                               "response sequence");
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


    return new JobResponseMessage(messageID, jobID, responseCode,
                                       responseMessage);
  }



  /**
   * Encodes this message into an ASN.1 element.  A job response message has the
   * following syntax:
   * <BR><BR>
   * <CODE>JobResponse ::= [APPLICATION 4] SEQUENCE {</CODE>
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
    ASN1Integer resultCodeElement = new ASN1Integer(responseCode);
    ASN1OctetString resultMsgElement =
         new ASN1OctetString(responseMessage);

    ASN1Element[] jobResponseElements = new ASN1Element[]
    {
      jobIDElement,
      resultCodeElement,
      resultMsgElement
    };

    ASN1Sequence jobResponseSequence = new ASN1Sequence(ASN1_TYPE_JOB_RESPONSE,
                                                        jobResponseElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      jobResponseSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

