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
 * server to indicate that the client will be providing in-progress information
 * for a given statistic.  It will contain information about the statistic that
 * will be reported.
 *
 *
 * @author   Neil A. Wilson
 */
public class RegisterStatistic
       extends SLAMDMessage
{
  // The client ID of the client that will be reporting the data.
  private String clientID;

  // The display name of the stat tracker that will be reported.
  private String displayName;

  // The job ID of the job with which the data is associated.
  private String jobID;

  // The thread ID of the client thread that will be reporting the data.
  private String threadID;



  /**
   * Creates a new instance of this register statistic message which is intended
   * for use in decoding a message transmitted between the server and the
   * client.  It is not intended for general use.
   */
  public RegisterStatistic()
  {
    super();

    jobID       = null;
    clientID    = null;
    threadID    = null;
    displayName = null;
  }



  /**
   * Creates a new instance of this register statistic message with the provided
   * information.
   *
   * @param  jobID        The job ID of the job with which the data is
   *                      associated.
   * @param  clientID     The client ID of the client that will be reporting the
   *                      data;
   * @param  threadID     The thread ID of the client thread that will be
   *                      reporting the data;
   * @param  displayName  The display name of the stat tracker that will be
   *                      reported.
   */
  public RegisterStatistic(String jobID, String clientID, String threadID,
                           String displayName)
  {
    this.jobID       = jobID;
    this.clientID    = clientID;
    this.threadID    = threadID;
    this.displayName = displayName;
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
   * Retrieves the client ID of the client that will be reporting the data.
   *
   * @return  The client ID of the client that will be reporting the data.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Specifies the client ID of the client that will be reporting the data.
   *
   * @param  clientID  The client ID of the client that will be reporting the
   *                   data.
   */
  public void setClientID(String clientID)
  {
    this.clientID = clientID;
  }



  /**
   * Retrieves the thread ID of the client thread that will be reporting the
   * data.
   *
   * @return  The thread ID of the client thread that will be reporting the
   *          data.
   */
  public String getThreadID()
  {
    return threadID;
  }



  /**
   * Specifies the thread ID of the client thread that will be reporting the
   * data.
   *
   * @param  threadID  The thread ID of the client thread that will be reporting
   *                   the data.
   */
  public void setThreadID(String threadID)
  {
    this.threadID = threadID;
  }



  /**
   * Retrieves the display name of the stat tracker that will be reported.
   *
   * @return  The display name of the stat tracker that will be reported.
   */
  public String getDisplayName()
  {
    return displayName;
  }



  /**
   * Specifies the display name of the stat tracker that will be reported.
   *
   * @param  displayName  The display name of the stat tracker that will be
   *                      reported.
   */
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
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
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_ID,
                                        new ASN1OctetString(clientID)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_THREAD_ID,
                                        new ASN1OctetString(threadID)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_DISPLAY_NAME,
                                        new ASN1OctetString(displayName)));

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
      throw new SLAMDException("Register statistic message does not include " +
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


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_ID);
    if (valueElement == null)
    {
      throw new SLAMDException("Register statistic message does not include " +
                               "a client ID.");
    }
    else
    {
      try
      {
        clientID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client ID:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_THREAD_ID);
    if (valueElement == null)
    {
      throw new SLAMDException("Register statistic message does not include " +
                               "a thread ID.");
    }
    else
    {
      try
      {
        threadID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the thread ID:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_DISPLAY_NAME);
    if (valueElement == null)
    {
      throw new SLAMDException("Register statistic message does not include " +
                               "a display name.");
    }
    else
    {
      try
      {
        displayName = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the display name:  " + e, e);
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
    buffer.append("clientID = ");
    buffer.append(clientID);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("threadID = ");
    buffer.append(threadID);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("displayName = ");
    buffer.append(displayName);
  }
}

