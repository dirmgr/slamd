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
 * server to provide information about the current state of the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatusResponse
       extends SLAMDMessage
{
  // The status code for the client.
  private int clientState;

  // The status code for the job being processed by the client.
  private int jobState;

  // The result code for the status request.
  private int resultCode;

  // The job ID of a job with which the client might be associated.
  private String jobID;

  // A message providing additional information about the state of the client.
  private String stateMessage;



  /**
   * Creates a new instance of this status response message which is intended
   * for use in decoding a message transmitted between the server and the
   * client. It is not intended for general use.
   */
  public StatusResponse()
  {
    super();

    resultCode   = -1;
    clientState  = -1;
    stateMessage = null;
    jobID        = null;
    jobState     = -1;
  }



  /**
   * Creates a new instance of this status response message with the provided
   * information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  resultCode       The result code for the status request.
   * @param  clientState      An indicator of the current state for the client.
   * @param  stateMessage     A message providing additional information about
   *                          the state of the client.
   * @param  jobID            The job ID of any job with which the client might
   *                          be associated, or <CODE>null</CODE> if the client
   *                          is not associated with any job.
   * @param  jobState         The current state of the associated job, or -1 if
   *                          the client is not associated with any job.
   */
  public StatusResponse(int messageID, HashMap<String,String> extraProperties,
                        int resultCode, int clientState,
                        String stateMessage, String jobID, int jobState)
  {
    super(messageID, extraProperties);

    this.resultCode   = resultCode;
    this.clientState  = clientState;
    this.stateMessage = stateMessage;
    this.jobID        = jobID;
    this.jobState     = jobState;
  }



  /**
   * Retrieves the result code for the status request.
   *
   * @return  The result code for the status request.
   */
  public int getResultCode()
  {
    return resultCode;
  }



  /**
   * Specifies the result code for the status request.
   *
   * @param  resultCode  The result code for the status request.
   */
  public void setResultCode(int resultCode)
  {
    this.resultCode = resultCode;
  }



  /**
   * Retrieves a status code representing the current state for the client.
   *
   * @return  A status code representing the current state for the client.
   */
  public int getClientState()
  {
    return clientState;
  }



  /**
   * Specifies a status code representing the current state for the client.
   *
   * @param  clientState  A status code representing the current state for the
   *                      client.
   */
  public void setClientState(int clientState)
  {
    this.clientState = clientState;
  }



  /**
   * Retrieves a message with additional information about the state of the
   * client.
   *
   * @return  A message with additional information about the state of the
   *          client, or <CODE>null</CODE> if none was provided.
   */
  public String getStateMessage()
  {
    return stateMessage;
  }



  /**
   * Specifies a message with additional information about the state of the
   * client.
   *
   * @param  stateMessage  A message with additional information about the state
   *                       of the client.
   */
  public void setStateMessage(String stateMessage)
  {
    this.stateMessage = stateMessage;
  }



  /**
   * Retrieves the job ID for any job with which the client might be associated.
   *
   * @return  The job ID for any job with which the client might be associated,
   *          or <CODE>null</CODE> if the client is not associated with any job.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Specifies the job ID for a job with which the client might be associated.
   *
   * @param  jobID  The job ID for a job with which the client might be
   *                associated.
   */
  public void setJobID(String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Retrieves a status code for the job with which the client might be
   * associated.
   *
   * @return  A status code for the job with which the client might be
   *          associated, or -1 if there is no such job.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Specifies a status code for the job with which the client might be
   * associated.
   *
   * @param  jobState  A status code for the job with whcih the client might be
   *                   associated.
   */
  public void setJobState(int jobState)
  {
    this.jobState = jobState;
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

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_RESULT_CODE,
                                        new ASN1Enumerated(resultCode)));

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_STATE,
                                        new ASN1Enumerated(clientState)));

    if (stateMessage != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_CLIENT_STATE_MESSAGE,
                           new ASN1OctetString(stateMessage)));
    }

    if (jobID != null)
    {
      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_JOB_ID,
                                          new ASN1OctetString(jobID)));

      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_JOB_STATE,
                                          new ASN1Enumerated(jobState)));
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
         propertyMap.get(ProtocolConstants.PROPERTY_RESULT_CODE);
    if (valueElement == null)
    {
      throw new SLAMDException("Status response message does not include a " +
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

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_STATE);
    if (valueElement == null)
    {
      throw new SLAMDException("Status response message does not include " +
                               "the client state.");
    }
    else
    {
      try
      {
        clientState = valueElement.decodeAsEnumerated().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client state:  " + e, e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_STATE_MESSAGE);
    if (valueElement != null)
    {
      try
      {
        stateMessage = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client state " +
                                 "message:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_JOB_ID);
    if (valueElement != null)
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

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_JOB_STATE);
    if (valueElement != null)
    {
      try
      {
        jobState = valueElement.decodeAsEnumerated().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job state:  " + e, e);
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
    buffer.append("resultCode = ");
    buffer.append(resultCode);
    buffer.append(" (");
    buffer.append(Constants.responseCodeToString(resultCode));
    buffer.append(')');
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("clientState = ");
    buffer.append(clientState);
    buffer.append(" (");
    buffer.append(Constants.clientStateToString(clientState));
    buffer.append(')');

    if (stateMessage != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("clientStateMessage = ");
      buffer.append(stateMessage);
    }

    if (jobID != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("jobID = ");
      buffer.append(jobID);

      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("jobState = ");
      buffer.append(jobState);
      buffer.append(" (");
      buffer.append(Constants.jobStateToString(jobState));
      buffer.append(')');
    }
  }
}

