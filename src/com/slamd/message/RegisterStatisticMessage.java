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
 * This class defines a message that clients will use to register statistics
 * with the SLAMD server's real-time stat collection facility.
 *
 *
 * @author   Neil A. Wilson
 */
public class RegisterStatisticMessage
       extends Message
{
  // The ID of the client registering the statistic.
  private final String clientID;

  // The display name of the statistic being registered.
  private final String displayName;

  // The job ID of the job with which this data is associated.
  private final String jobID;

  // The ID of the client thread registering the statistic.
  private final String threadID;



  /**
   * Creates a new register statistic message with the provided information.
   *
   * @param  messageID    The message ID to use for this message.
   * @param  jobID        The ID of the job with which the message is
   *                      associated.
   * @param  clientID     The ID of the client registering the statistic.
   * @param  threadID     The ID of the client thread registering the statistic.
   * @param  displayName  The display name of the statistic being registered.
   */
  public RegisterStatisticMessage(int messageID, String jobID, String clientID,
                                  String threadID, String displayName)
  {
    super(messageID, Constants.MESSAGE_TYPE_REGISTER_STATISTIC);

    this.jobID       = jobID;
    this.clientID    = clientID;
    this.threadID    = threadID;
    this.displayName = displayName;
  }



  /**
   * Retrieves the job ID of the job to which this message applies.
   *
   * @return  The job ID of the job to which this message applies.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the ID of the client that registered this statistic.
   *
   * @return  The ID of the client that registered this statistic.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Retrieves the ID of the client thread that registered this statistic.
   *
   * @return  The ID of the client thread that registered this statistic.
   */
  public String getThreadID()
  {
    return threadID;
  }



  /**
   * Retrieves the display name of the statistic being registered.
   *
   * @return  The display name of the statistic being registered.
   */
  public String getDisplayName()
  {
    return displayName;
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

    return "Register Statistic Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Client ID:  " + clientID + eol +
           "  Thread ID:  " + threadID + eol +
           "  Display Name:  " + displayName + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a register statistic message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the RegisterStat sequence.
   *
   * @return  The register statistic message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a register statistic message.
   */
  public static RegisterStatisticMessage
                     decodeRegisterStatMessage(int messageID,
                                               ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence registerStatSequence = null;
    try
    {
      registerStatSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode the ASN.1 element as a sequence",
                               ae);
    }


    ASN1Element[] elements = registerStatSequence.getElements();
    if (elements.length != 4)
    {
      throw new SLAMDException("A register statistic sequence must contain " +
                               "3 elements");
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

    String clientID = null;
    try
    {
      clientID = elements[1].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The second element cannot be decoded as an " +
                               "octet string", ae);
    }

    String threadID = null;
    try
    {
      threadID = elements[2].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The third element cannot be decoded as an " +
                               "octet string", ae);
    }

    String displayName = null;
    try
    {
      displayName = elements[3].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The fourth element cannot be decoded as an " +
                               "octet string", ae);
    }


    return new RegisterStatisticMessage(messageID, jobID, clientID, threadID,
                                        displayName);
  }



  /**
   * Encodes this message into an ASN.1 element.  A register statistic message
   * has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>RegisterStat ::= [APPLICATION 19] SEQUENCE {</CODE><BR>
   * <CODE>    jobID        OCTET STRING,</CODE><BR>
   * <CODE>    clientID     OCTET STRING,</CODE><BR>
   * <CODE>    threadID     OCTET STRING,</CODE><BR>
   * <CODE>    displayName  OCTET STRING }</CODE><BR>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Element[] registerStatElements = new ASN1Element[]
    {
      new ASN1OctetString(jobID),
      new ASN1OctetString(clientID),
      new ASN1OctetString(threadID),
      new ASN1OctetString(displayName)
    };

    ASN1Sequence registerStatSequence =
         new ASN1Sequence(ASN1_TYPE_REGISTER_STAT, registerStatElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      registerStatSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

