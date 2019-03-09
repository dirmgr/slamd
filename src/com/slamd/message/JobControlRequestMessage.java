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
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a message that can be used to alter the state of a
 * running job (e.g., to request that a client prematurely stop processing a
 * job).
 *
 *
 * @author   Neil A. Wilson
 */
public class JobControlRequestMessage
       extends Message
{
  // The type of job control operation being requested.
  private final int jobControlOperation;

  // The ID of the job for which this action is being requested.
  private final String jobID;



  /**
   * Creates a new job control request message to perform the indicated
   * operation on the specified job.
   *
   * @param  messageID            The message ID for this message.
   * @param  jobID                The ID of the job for which the operation is
   *                              being requested.
   * @param  jobControlOperation  The type of operation being requested on the
   *                              specified job.
   */
  public JobControlRequestMessage(int messageID, String jobID,
                                  int jobControlOperation)
  {
    super(messageID, Constants.MESSAGE_TYPE_JOB_CONTROL_REQUEST);

    this.jobID               = jobID;
    this.jobControlOperation = jobControlOperation;
  }



  /**
   * Retrieves the ID of the job for which this operation is being requested.
   *
   * @return  The ID of the job for which this operation is being requested.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the type of operation that is being requested.
   *
   * @return  The type of operation that is being requested.
   */
  public int getJobControlOperation()
  {
    return jobControlOperation;
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

    return "Job Control Request Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Control Type:  " + jobControlOperation + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a job control request message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the JobControlRequest
   *                    sequence.
   *
   * @return  The job control request decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a job control request message.
   */
  public static JobControlRequestMessage
                     decodeJobControlRequest(int messageID, ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence requestSequence = null;
    try
    {
      requestSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode the ASN.1 element as a sequence",
                               ae);
    }


    ASN1Element[] elements = requestSequence.getElements();
    if (elements.length != 2)
    {
      throw new SLAMDException("A job control request sequence must contain " +
                               "2 elements");
    }


    String jobID = null;
    try
    {
      jobID = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The first element cannot be decoded as an " +
                               "octet string", ae);
    }


    int controlType = 0;
    try
    {
      controlType = elements[1].decodeAsEnumerated().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The second element cannot be decoded as an " +
                               "enumerated", ae);
    }


    return new JobControlRequestMessage(messageID, jobID, controlType);
  }



  /**
   * Encodes this message into an ASN.1 element.  A job control request message
   * has the following syntax:
   * <BR><BR>
   * <CODE>JobControlRequest ::= [APPLICATION 5] SEQUENCE {</CODE>
   * <CODE>    jobID        OCTET STRING,</CODE>
   * <CODE>    controlType  JobControlType }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1OctetString jobIDElement = new ASN1OctetString(jobID);
    ASN1Enumerated controlTypeElement = new ASN1Enumerated(jobControlOperation);


    ASN1Element[] jobControlElements = new ASN1Element[]
    {
      jobIDElement,
      controlTypeElement
    };

    ASN1Sequence requestSequence =
         new ASN1Sequence(ASN1_TYPE_JOB_CONTROL_REQUEST,
                          jobControlElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      requestSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

