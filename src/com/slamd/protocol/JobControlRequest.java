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
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the server to the
 * client to request that the client take some action on a job.  This can
 * include actions like starting or stopping the job.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobControlRequest
       extends SLAMDMessage
{
  // The control type for this job control request.
  private String jobControlOperation;

  // The job ID for this job control request.
  private String jobID;



  /**
   * Creates a new instance of this job control request message which is
   * intended for use in decoding a message transmitted between the server and
   * the client. It is not intended for general use.
   */
  public JobControlRequest()
  {
    super();

    jobID               = null;
    jobControlOperation = null;
  }



  /**
   * Creates a new instance of this job control request message with the
   * provided information.
   *
   * @param  messageID            The message ID for this SLAMD message.
   * @param  extraProperties      The "extra" properties for this SLAMD message.
   *                              Both the names and values for the properties
   *                              must be strings.
   * @param  jobID                The job ID for this job control request.
   * @param  jobControlOperation  The operation for this job control request.
   */
  public JobControlRequest(int messageID,
                           HashMap<String,String> extraProperties, String jobID,
                           String jobControlOperation)
  {
    super(messageID, extraProperties);

    this.jobID               = jobID;
    this.jobControlOperation = jobControlOperation;
  }



  /**
   * Retrieves the job ID from the job control request.
   *
   * @return  The job ID from the job control request.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Specifies the job ID from the job control request.
   *
   * @param  jobID  The job ID from the job control request.
   */
  public void setJobID(String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Retrieves the operation for this job control request.
   *
   * @return  The operation for this job control request.
   */
  public String getJobControlOperation()
  {
    return jobControlOperation;
  }



  /**
   * Specifies the operation for this job control request.
   *
   * @param  jobControlOperation  The operation for this job control request.
   */
  public void setJobControlOperation(String jobControlOperation)
  {
    this.jobControlOperation = jobControlOperation;
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
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_JOB_CONTROL_OPERATION,
                         new ASN1OctetString(jobControlOperation)));

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
      throw new SLAMDException("Job control request message does not include " +
                               "a job ID.");
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

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_JOB_CONTROL_OPERATION);
    if (valueElement == null)
    {
      throw new SLAMDException("Job control request message does not " +
                               "include a job control operation.");
    }
    else
    {
      try
      {
        jobControlOperation =
             valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job control " +
                                 "operation:  " + e, e);
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
    buffer.append("jobControlOperation = ");
    buffer.append(jobControlOperation);
  }
}

