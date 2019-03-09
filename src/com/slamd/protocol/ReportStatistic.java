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
 * This class defines a SLAMD message that will be sent from the client to the
 * server to report in-progress statistical data.  It will include the job ID
 * and one or ASN.1 sequences with encoded in-progress results for that job.
 *
 *
 * @author   Neil A. Wilson
 */
public class ReportStatistic
       extends SLAMDMessage
{
  // A set of ASN.1 sequences that contain in-progress information for the job.
  private ASN1Sequence[] dataSequences;

  // The job ID of the job with which the data is associated.
  private String jobID;



  /**
   * Creates a new instance of this report statistic message which is intended
   * for use in decoding a message transmitted between the server and the
   * client.  It is not intended for general use.
   */
  public ReportStatistic()
  {
    super();

    jobID         = null;
    dataSequences = new ASN1Sequence[0];
  }



  /**
   * Creates a new instance of this report statistic message with the provided
   * information.
   *
   * @param  jobID          The job ID of the job with which the data is
   *                        associated.
   * @param  dataSequences  A set of ASN.1 sequences that contain in-progress
   *                        information for the job.
   */
  public ReportStatistic(String jobID, ASN1Sequence[] dataSequences)
  {
    this.jobID = jobID;

    if (dataSequences == null)
    {
      this.dataSequences = new ASN1Sequence[0];
    }
    else
    {
      this.dataSequences = dataSequences;
    }
  }



  /**
   * Retrieves the job ID of the job with which the data is associated.
   *
   * @return  The job ID of the job with which the data is associated.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Specifies the job ID of the job with which the data is associated.
   *
   * @param  jobID  The job ID of the job with which the data is associated.
   */
  public void setJobID(String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Retrieves a set of ASN.1 sequences that contain in-progress information for
   * the job.
   *
   * @return  A set of ASN.1 sequences that contain in-progress information for
   *          the job.
   */
  public ASN1Sequence[] getDataSequences()
  {
    return dataSequences;
  }



  /**
   * Specifies a set of ASN.1 sequences that contain in-progress information for
   * the job.
   *
   * @param  dataSequences  A set of ASN.1 sequences that contain in-progress
   *                        information for the job.
   */
  public void setDataSequences(ASN1Sequence[] dataSequences)
  {
    if (dataSequences == null)
    {
      this.dataSequences = new ASN1Sequence[0];
    }
    else
    {
      this.dataSequences = dataSequences;
    }
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

    if ((dataSequences != null) && (dataSequences.length > 0))
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_IN_PROGRESS_DATA,
                           new ASN1Sequence(dataSequences)));
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
      throw new SLAMDException("Report statistic message does not include " +
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


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_IN_PROGRESS_DATA);
    if (valueElement == null)
    {
      throw new SLAMDException("Report statistic message does not include " +
                               "any in-progress data.");
    }
    else
    {
      try
      {
        ASN1Element[] dataElements =
             valueElement.decodeAsSequence().getElements();
        dataSequences = new ASN1Sequence[dataElements.length];
        for (int i=0; i < dataSequences.length; i++)
        {
          dataSequences[i] = dataElements[i].decodeAsSequence();
        }
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the in-progress data:  " + e,
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

    if ((dataSequences != null) && (dataSequences.length > 0))
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("dataSequences = ArrayList[");
      buffer.append(dataSequences.length);
      buffer.append(']');
    }
  }
}

