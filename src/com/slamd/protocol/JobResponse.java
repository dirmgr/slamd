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
package com.slamd.protocol;



import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the client to the
 * server in response to a job request.  It will include the job ID, a result
 * code and an optional error message.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobResponse
       extends SLAMDMessage
{
  // The result code for the job request.
  private int resultCode;

  // The error message for the job request.
  private String errorMessage;

  // The job ID from the job request.
  private String jobID;



  /**
   * Creates a new instance of this job response message which is intended for
   * use in decoding a message transmitted between the server and the client.
   * It is not intended for general use.
   */
  public JobResponse()
  {
    super();

    jobID        = null;
    resultCode   = -1;
    errorMessage = null;
  }



  /**
   * Creates a new instance of this job response message with the provided
   * information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  jobID            The job ID for the job in the request.
   * @param  resultCode       The result code for the job request.
   * @param  errorMessage     The error message for the job request.
   */
  public JobResponse(int messageID, HashMap<String,String> extraProperties,
                     String jobID, int resultCode, String errorMessage)
  {
    super(messageID, extraProperties);

    this.jobID        = jobID;
    this.resultCode   = resultCode;
    this.errorMessage = errorMessage;
  }



  /**
   * Retrieves the job ID for the job in the request.
   *
   * @return  The job ID for the job in the request.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Specifies the job ID for the job in the request.
   *
   * @param  jobID  The job ID for the job in the request.
   */
  public void setJobID(String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Retrieves the result code for the job request.
   *
   * @return  The result code for the job request.
   */
  public int getResultCode()
  {
    return resultCode;
  }



  /**
   * Specifies the result code for the job request.
   *
   * @param  resultCode  The result code for the job request.
   */
  public void setResultCode(int resultCode)
  {
    this.resultCode = resultCode;
  }



  /**
   * Retrieves the error message for the job request.
   *
   * @return  The error message for the job request, or <CODE>null</CODE> if
   *          none was provided.
   */
  public String getErrorMessage()
  {
    return errorMessage;
  }



  /**
   * Specifies the error message for the job request.
   *
   * @param  errorMessage  The error message for the job request.
   */
  public void setErrorMessage(String errorMessage)
  {
    this.errorMessage = errorMessage;
  }



  /**
   * Encodes the payload component of this SLAMD message to an ASN.1 element for
   * inclusion in the message envelope.
   *
   * @return  The ASN.1 element containing the encoded message payload.
   */
  @Override()
  public ASN1Element encodeMessagePayload()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_JOB_ID,
                                        new ASN1OctetString(jobID)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_RESULT_CODE,
                                        new ASN1Enumerated(resultCode)));

    if (errorMessage != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_RESULT_MESSAGE,
                           new ASN1OctetString(errorMessage)));
    }

    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the provided ASN.1 element and uses it as the payload for this
   * SLAMD message.
   *
   * @param  payloadElement  The ASN.1 element to decode as the payload for this
   *                         SLAMD message.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided ASN.1 element as the payload for this
   *                          SLAMD message.
   */
  @Override()
  public void decodeMessagePayload(ASN1Element payloadElement)
         throws SLAMDException
  {
    HashMap<String,ASN1Element> propertyMap =
         decodeNameValuePairSequence(payloadElement);

    ASN1Element valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_JOB_ID);
    if (valueElement == null)
    {
      throw new SLAMDException("Job response message does not include a job " +
                               "ID.");
    }
    else
    {
      try
      {
        jobID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job ID:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_RESULT_CODE);
    if (valueElement == null)
    {
      throw new SLAMDException("Job response message does not include a " +
                               "result code.");
    }
    else
    {
      try
      {
        resultCode = valueElement.decodeAsEnumerated().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the result code:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_RESULT_MESSAGE);
    if (valueElement != null)
    {
      try
      {
        errorMessage = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the error message:  " + e,
                                 e);
      }
    }
  }



  /**
   * Appends a string representation of the payload for this SLAMD message to
   * the provided buffer.  The string representation may contain multiple lines,
   * but the last line should not end with an end-of-line marker.
   *
   * @param  buffer  The buffer to which the string representation is to be
   *                 appended.
   * @param  indent  The number of spaces to indent the payload content.
   */
  @Override()
  public void payloadToString(StringBuilder buffer, int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    buffer.append(indentBuf);
    buffer.append("jobID = ");
    buffer.append(jobID);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("resultCode = ");
    buffer.append(resultCode);
    buffer.append(" (");
    buffer.append(Constants.responseCodeToString(resultCode));
    buffer.append(')');

    if (errorMessage != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("errorMessage = ");
      buffer.append(errorMessage);
    }
  }
}

