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
import com.slamd.stat.StatEncoder;
import com.slamd.stat.StatTracker;



/**
 * This class defines a message type that the client can use to provide status
 * information back to the server.  The information in the status response may
 * vary based on the request received from the server.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatusResponseMessage
       extends Message
{
  // A numeric code that indicates the current state of the client.
  private final int clientStatusCode;

  // The state of the requested job.
  private final int jobState;

  // The response code from the request operation
  private final int responseCode;

  // Status counter information that the client has about the specified job.
  private final StatTracker[] statTrackers;

  // A text message about the status of the client.
  private final String clientStatusMessage;

  // The ID of the job for which this status information is being provided.
  private final String jobID;



  /**
   * Creates a new status response message using the specified client status
   * code.  It will not contain a client status message or job-specific
   * information.
   *
   * @param  messageID         The message ID for this message.
   * @param  responseCode      The response code for the status request
   *                           operation.
   * @param  clientStatusCode  The client status code to include in this status
   *                           response message.
   */
  public StatusResponseMessage(int messageID, int responseCode,
                               int clientStatusCode)
  {
    this(messageID, responseCode, clientStatusCode, "", "",
         Constants.JOB_STATE_UNKNOWN, new StatTracker[0]);
  }



  /**
   * Creates a new status response message using the specified client status
   * code and message.  It will not contain any job-specific information.
   *
   * @param  messageID            The message ID for this message.
   * @param  responseCode         The response code for the status request
   *                              operation.
   * @param  clientStatusCode     The client status code to include in this
   *                              status response message.
   * @param  clientStatusMessage  The client status message to include in this
   *                              status response message.
   */
  public StatusResponseMessage(int messageID, int responseCode,
                               int clientStatusCode, String clientStatusMessage)
  {
    this(messageID, responseCode, clientStatusCode, clientStatusMessage, "",
         Constants.JOB_STATE_UNKNOWN, new StatTracker[0]);
  }



  /**
   * Creates a new status response message based on the provided client and
   * job-specific information.  There will be no client status message.
   *
   * @param  messageID        The message ID for this message.
   * @param  responseCode     The response code for the status request
   *                          operation.
   * @param clientStatusCode  The client status code to include in this status
   *                          response message.
   * @param  jobID            The ID of the job for which job-specific
   *                          information is being provided.
   * @param  jobState         The current processing state for the specified
   *                          job.
   * @param  statTrackers     The set of stat trackers associated with the job
   *                          the client is currently processing.
   */
  public StatusResponseMessage(int messageID, int responseCode,
                               int clientStatusCode, String jobID, int jobState,
                               StatTracker[] statTrackers)
  {
    this(messageID, responseCode, clientStatusCode, "", jobID, jobState,
         statTrackers);
  }



  /**
   * Creates a new status response message based on the provided client and
   * job-specific information.
   *
   * @param  messageID            The message ID for this message.
   * @param  responseCode         The response code for the status request
   *                              operation.
   * @param  clientStatusCode     The client status code to include in this
   *                              status response message.
   * @param  clientStatusMessage  The client status message to include in this
   *                              status response message.
   * @param  jobID                The ID of the job for which job-specific
   *                              information is being provided.
   * @param  jobState             The current processing state for the specified
   *                              job.
   * @param  statTrackers         The set of stat trackers associated with the
   *                              job the client is currently processing.
   */
  public StatusResponseMessage(int messageID, int responseCode,
                               int clientStatusCode, String clientStatusMessage,
                               String jobID, int jobState,
                               StatTracker[] statTrackers)
  {
    super(messageID, Constants.MESSAGE_TYPE_STATUS_RESPONSE);

    this.responseCode        = responseCode;
    this.clientStatusCode    = clientStatusCode;
    this.clientStatusMessage = clientStatusMessage;
    this.jobID               = jobID;
    this.jobState            = jobState;
    this.statTrackers        = statTrackers;
  }



  /**
   * Retrieves the response code associated with the status request.
   *
   * @return  The response code associated with the status request.
   */
  public int getResponseCode()
  {
    return responseCode;
  }



  /**
   * Retrieves the client status code associated with this status response
   * message.
   *
   * @return  The client status code associated with this status response
   *          message.
   */
  public int getClientStatusCode()
  {
    return clientStatusCode;
  }



  /**
   * Retrieves the client status message associated with this status response
   * message.
   *
   * @return  The client status message associated with this status response
   *          message.
   */
  public String getClientStatusMessage()
  {
    return clientStatusMessage;
  }



  /**
   * Retrieves the ID of the job associated with this status response message.
   *
   * @return  The ID of the job associated with this status response message.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the state of the job associated with this status response
   * message.
   *
   * @return  The state of the job associated with this status response message.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Retrieves the stat tracker information associated with this status response
   * message.
   *
   * @return  The stat tracker information associated with this status response
   *          message.
   */
  public StatTracker[] getStatTrackers()
  {
    return statTrackers;
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

    String statStr = "";
    for (int i=0; ((statTrackers != null) && (i < statTrackers.length));
         i++)
    {
      statStr += "    " + statTrackers[i].getSummaryString() + eol;
    }

    return "Status Response Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Client Status Code:  " + clientStatusCode + eol +
           "  Client Status Message:  " + clientStatusMessage + eol +
           "  Job ID:  " + jobID + eol +
           "  Job State:  " + jobState + eol +
           "  Stat Trackers:" + eol +
           statStr;
  }



  /**
   * Decodes the provided ASN.1 element as a status response message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the StatusResponse
   *                    sequence.
   *
   * @return  The status response message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a status response message.
   */
  public static StatusResponseMessage decodeStatusResponse(int messageID,
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
      throw new SLAMDException("Could not decode the provided ASN.1 element " +
                               "as a sequence", ae);
    }


    ASN1Element[] elements = responseSequence.getElements();
    if ((elements.length != 3) && (elements.length != 4))
    {
      throw new SLAMDException("There must be either 3 or 4 elements in a " +
                               "status response");
    }


    int responseCode = 0;
    try
    {
      responseCode = elements[0].decodeAsEnumerated().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the first element as an " +
                               "enumerated", ae);
    }


    int clientState = 0;
    try
    {
      clientState = elements[0].decodeAsEnumerated().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the second element as an " +
                               "enumerated", ae);
    }


    String clientMessage = null;
    try
    {
      clientMessage = elements[2].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the third element as an " +
                               "octet string", ae);
    }


    if (elements.length == 3)
    {
      return new StatusResponseMessage(messageID, responseCode,
                                            clientState, clientMessage);
    }


    ASN1Sequence jobStatusSequence = null;
    try
    {
      jobStatusSequence = elements[3].decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the fourth element as a " +
                               "sequence", ae);
    }


    elements = jobStatusSequence.getElements();
    if (elements.length != 3)
    {
      throw new SLAMDException("There must be 3 elements in a job status " +
                               "sequence");
    }


    String jobID = null;
    try
    {
      jobID = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode first job status element " +
                               "as an octet string", ae);
    }


    int jobState =0;
    try
    {
      jobState = elements[1].decodeAsEnumerated().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode second job status element " +
                               "as an enumerated", ae);
    }


    StatTracker[] statTrackers = null;
    try
    {
      ASN1Sequence trackerSequence = elements[2].decodeAsSequence();
      statTrackers = StatEncoder.sequenceToTrackers(trackerSequence);
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode third job status element " +
                               "as a sequence of stat trackers", ae);
    }


    return new StatusResponseMessage(messageID, responseCode, clientState,
                                     clientMessage, jobID, jobState,
                                     statTrackers);
  }



  /**
   * Encodes this message into an ASN.1 element.  A status response message has
   * the following syntax:
   * <BR><BR>
   * <CODE>StatusResponse ::= [APPLICATION 9] SEQUENCE {</CODE>
   * <CODE>    responseCode         ResponseCode,</CODE>
   * <CODE>    clientStatusCode     ClientState,</CODE>
   * <CODE>    clientStatusMessage  OCTET STRING,</CODE>
   * <CODE>    jobStatus            JobStatus OPTIONAL }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1Enumerated responseCodeElement = new ASN1Enumerated(responseCode);
    ASN1Enumerated clientStateElement = new ASN1Enumerated(clientStatusCode);
    ASN1OctetString clientMessageElement =
         new ASN1OctetString(clientStatusMessage);

    ASN1Element[] statusResponseElements;

    if ((jobID == null) || (jobID.length() == 0))
    {
      statusResponseElements = new ASN1Element[]
      {
        responseCodeElement,
        clientStateElement,
        clientMessageElement
      };
    }
    else
    {
      ASN1OctetString jobIDElement = new ASN1OctetString(jobID);
      ASN1Enumerated jobStateElement = new ASN1Enumerated(jobState);

      ASN1Sequence trackerSequence =
                        StatEncoder.trackersToSequence(statTrackers);

      ASN1Element[] jobStatusElements = new ASN1Element[]
      {
        jobIDElement,
        jobStateElement,
        trackerSequence
      };
      ASN1Sequence jobStatusSequence = new ASN1Sequence(jobStatusElements);

      statusResponseElements = new ASN1Element[]
      {
        responseCodeElement,
        clientStateElement,
        clientMessageElement,
        jobStatusSequence
      };
    }

    ASN1Sequence statusSequence =
         new ASN1Sequence(ASN1_TYPE_STATUS_RESPONSE, statusResponseElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      statusSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

