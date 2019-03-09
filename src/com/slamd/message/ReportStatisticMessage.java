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
 * This class defines a message that clients will use to report statistics to
 * the SLAMD server's real-time stat collection facility.
 *
 *
 * @author   Neil A. Wilson
 */
public class ReportStatisticMessage
       extends Message
{
  // The ASN.1 sequence containing the data being reported.
  private final ASN1Sequence[] dataSequences;

  // The job ID of the job with which this data is associated.
  private final String jobID;



  /**
   * Creates a new report statistic message with the provided information.
   *
   * @param  messageID      The message ID to use for this message.
   * @param  jobID          The job ID with which this data is associated.
   * @param  dataSequences  The ASN.1 sequences containing the data to be
   *                        reported.
   */
  public ReportStatisticMessage(int messageID, String jobID,
                                ASN1Sequence[] dataSequences)
  {
    super(messageID, Constants.MESSAGE_TYPE_REAL_TIME_STAT_DATA);

    this.dataSequences = dataSequences;
    this.jobID         = jobID;
  }



  /**
   * Retrieves the job ID of the job with which this data is associated.
   *
   * @return  The job ID of the job with which this data is associated.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the ASN.1 sequences containing the data to be reported.
   *
   * @return  The ASN.1 sequences containing the data to be reported.
   */
  public ASN1Sequence[] getDataSequences()
  {
    return dataSequences;
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

    return "Report Statistic Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Data Sequences:  <" + dataSequences.length + " elements>" + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a report statistic message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the ReportStat sequence.
   *
   * @return  The report statistic message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a report statistic message.
   */
  public static ReportStatisticMessage decodeReportStatMessage(int messageID,
                                            ASN1Element element)
         throws SLAMDException
  {
    ASN1Element[] elements = null;
    try
    {
      elements = element.decodeAsSequence().getElements();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Unable to decode the report stat sequence", ae);
    }

    if (elements.length != 2)
    {
      throw new SLAMDException("There must be exactly two elements in a " +
                               "ReportStat sequence.");
    }

    String jobID;
    try
    {
      jobID = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Unable to decode the first element as an " +
                               "octet string", ae);
    }


    ASN1Element[] dataElements;
    try
    {
      dataElements = elements[1].decodeAsSequence().getElements();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Unable to decode the second element as a " +
                               "sequence", ae);
    }

    ASN1Sequence[] dataSequences = new ASN1Sequence[dataElements.length];
    for (int i=0; i < dataElements.length; i++)
    {
      try
      {
        dataSequences[i] = dataElements[i].decodeAsSequence();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("Unable to decode data element " + i, ae);
      }
    }


    return new ReportStatisticMessage(messageID, jobID, dataSequences);
  }



  /**
   * Encodes this message into an ASN.1 element.  A report statistic message
   * has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>ReportStat ::= [APPLICATION 20] SEQUENCE {</CODE><BR>
   * <CODE>    jobID         OCTET STRING,</CODE><BR>
   * <CODE>    dataSequence  SEQUENCE OF SEQUENCE }</CODE><BR>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer  messageIDElement   = new ASN1Integer(messageID);

    ASN1Element[] sequenceElements = new ASN1Element[]
    {
      new ASN1OctetString(jobID),
      new ASN1Sequence(dataSequences)
    };

    ASN1Sequence reportStatSequence = new ASN1Sequence(ASN1_TYPE_REPORT_STAT,
                                                       sequenceElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      reportStatSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

