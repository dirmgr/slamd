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
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.ResourceMonitorStatTracker;
import com.slamd.stat.StatEncoder;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD message that will be sent from the client to the
 * server whenever processing has been completed for a job.  It will primarily
 * contain statistical information for the job.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobCompleted
       extends SLAMDMessage
{
  // Information about any data files that might have been generated during job
  // processing that should be sent back to the server.
  private FileData[] fileData;

  // The actual length of time in seconds that the job was active.
  private int actualDuration;

  // The state of the job when processing was complete.
  private int jobState;

  // The time that the job actually started processing.
  private long actualStartTime;

  // The time that the job actually finished processing.
  private long actualStopTime;

  // The set of resource monitor statistics collected by the job.
  private ResourceMonitorStatTracker[] monitorStatTrackers;

  // The status counters maintained by the job.
  private StatTracker[] statTrackers;

  // The ID for the job being processed.
  private String jobID;

  // The set of messages that have been logged by the client.
  private String[] logMessages;



  /**
   * Creates a new instance of this job completed message which is intended for
   * use in decoding a message transmitted between the server and the client.
   * It is not intended for general use.
   */
  public JobCompleted()
  {
    super();

    jobID               = null;
    jobState            = Constants.JOB_STATE_UNKNOWN;
    actualStartTime     = -1;
    actualStopTime      = -1;
    actualDuration      = -1;
    statTrackers        = new StatTracker[0];
    monitorStatTrackers = new ResourceMonitorStatTracker[0];
    fileData            = new FileData[0];
    logMessages         = new String[0];
  }



  /**
   * Creates a new instance of this job completed message with the provided
   * information.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   * @param  jobID            The job ID for the associated job.
   * @param  jobState         The final state for the job.
   * @param  actualStartTime  The time that the job actually started processing.
   * @param  actualStopTime   The time that the job actually stopped processing.
   * @param  actualDuration   The length of time in seconds the job was active.
   * @param  statTrackers     The set of statistical information collected while
   *                          the job was running.
   * @param  logMessages      The set of log messages for the job.
   */
  public JobCompleted(int messageID, HashMap<String,String> extraProperties,
                      String jobID, int jobState, long actualStartTime,
                      long actualStopTime, int actualDuration,
                      StatTracker[] statTrackers, String[] logMessages)
  {
    super(messageID, extraProperties);

    this.jobID           = jobID;
    this.jobState        = jobState;
    this.actualStartTime = actualStartTime;
    this.actualStopTime  = actualStopTime;
    this.actualDuration  = actualDuration;

    if (statTrackers == null)
    {
      this.statTrackers = new StatTracker[0];
    }
    else
    {
      this.statTrackers = statTrackers;
    }

    if (logMessages == null)
    {
      this.logMessages = new String[0];
    }
    else
    {
      this.logMessages = logMessages;
    }

    monitorStatTrackers = new ResourceMonitorStatTracker[0];
    fileData            = new FileData[0];
  }



  /**
   * Retrieves the job ID for the associated job.
   *
   * @return  The job ID for the associated job.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Specifies the job ID for the associated job.
   *
   * @param  jobID  The job ID for the associated job.
   */
  public void setJobID(String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Returns the final job state code for the job.
   *
   * @return  The final job state code for the job.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Specifies the final job state code for the job.
   *
   * @param  jobState  The final job state code for the job.
   */
  public void setJobState(int jobState)
  {
    this.jobState = jobState;
  }



  /**
   * Retrieves the time that the job actually started processing.
   *
   * @return  The time that the job actually started processing.
   */
  public long getActualStartTime()
  {
    return actualStartTime;
  }



  /**
   * Specifies the time that the job actually started processing.
   *
   * @param  actualStartTime  The time that the job actually started processing.
   */
  public void setActualStartTime(long actualStartTime)
  {
    this.actualStartTime = actualStartTime;
  }



  /**
   * Retrieves the time that the job actually stopped processing.
   *
   * @return  The time that the job actually stopped processing.
   */
  public long getActualStopTime()
  {
    return actualStopTime;
  }



  /**
   * Specifies the time that the job actually stopped processing.
   *
   * @param  actualStopTime  The time that the job actually stopped processing.
   */
  public void setActualStopTime(long actualStopTime)
  {
    this.actualStopTime = actualStopTime;
  }



  /**
   * Retrieves the length of time in seconds that the job was active.
   *
   * @return  The length of time in seconds that the job was active.
   */
  public int getActualDuration()
  {
    return actualDuration;
  }



  /**
   * Specifies the length of time in seconds that the job was active.
   *
   * @param  actualDuration  The length of time in seconds that the job was
   *                         active.
   */
  public void setActualDuration(int actualDuration)
  {
    this.actualDuration = actualDuration;
  }



  /**
   * Retrieves the set of job statistics collected by the job.
   *
   * @return  The set of job statistics collected by the job.
   */
  public StatTracker[] getStatTrackers()
  {
    return statTrackers;
  }



  /**
   * Specifies the set of job statistics collected by the job.
   *
   * @param  statTrackers  The set of job statistics collected by the job.
   */
  public void setStatTrackers(StatTracker[] statTrackers)
  {
    if (statTrackers == null)
    {
      this.statTrackers = new StatTracker[0];
    }
    else
    {
      this.statTrackers = statTrackers;
    }
  }



  /**
   * Retrieves the set of resource monitor statistics collected by the job.
   *
   * @return  The set of resource monitor statistics collected by the job.
   */
  public ResourceMonitorStatTracker[] getResourceMonitorStatTrackers()
  {
    return monitorStatTrackers;
  }



  /**
   * Specifies the set of resource monitor statistics collected by the job.
   *
   * @param  monitorStatTrackers  The set of resource monitor statistics
   *                              collected by the job.
   */
  public void setResourceMonitorStatTrackers(ResourceMonitorStatTracker[]
                                                  monitorStatTrackers)
  {
    if (monitorStatTrackers == null)
    {
      this.monitorStatTrackers = new ResourceMonitorStatTracker[0];
    }
    else
    {
      this.monitorStatTrackers = monitorStatTrackers;
    }
  }



  /**
   * Retrieves a set of information about any files generated during job
   * processing that should be uploaded to the server.
   *
   * @return  A set of information about any files generated during job
   *          processing that should be uploaded to the server.
   */
  public FileData[] getFileData()
  {
    return fileData;
  }



  /**
   * Specifies a set of information about any files generated during job
   * processing that should be uploaded to the server.
   *
   * @param  fileData  A set of information about any files generated during
   *                   job processing that should be uploaded to the server.
   */
  public void setFileData(FileData[] fileData)
  {
    if (fileData == null)
    {
      this.fileData = new FileData[0];
    }
    else
    {
      this.fileData = fileData;
    }
  }



  /**
   * Retrieves the set of log messages generated during job processing.
   *
   * @return  The set of log messages generated during job processing.
   */
  public String[] getLogMessages()
  {
    return logMessages;
  }



  /**
   * Specifies the set of log messages generated during job processing.
   *
   * @param  logMessages  The set of log messages generated during job
   *                      processing.
   */
  public void setLogMessages(String[] logMessages)
  {
    if (logMessages == null)
    {
      this.logMessages = new String[0];
    }
    else
    {
      this.logMessages = logMessages;
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
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_JOB_STATE,
                                        new ASN1Enumerated(jobState)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_ACTUAL_START_TIME,
                         new ASN1OctetString(String.valueOf(actualStartTime))));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_ACTUAL_STOP_TIME,
                         new ASN1OctetString(String.valueOf(actualStopTime))));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_ACTUAL_DURATION,
                         new ASN1Integer(actualDuration)));

    if ((statTrackers != null) && (statTrackers.length > 0))
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_JOB_STATISTICS,
                           StatEncoder.trackersToSequence(statTrackers)));
    }

    if ((monitorStatTrackers != null) && (monitorStatTrackers.length > 0))
    {
      elementList.add(encodeNameValuePair(
           ProtocolConstants.PROPERTY_MONITOR_STATISTICS,
           ResourceMonitorStatTracker.trackersToSequence(monitorStatTrackers)));
    }

    if ((fileData != null) && (fileData.length > 0))
    {
      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_FILE_DATA,
                                          FileData.encodeArray(fileData)));
    }

    if ((logMessages != null) && (logMessages.length > 0))
    {
      // Although we could encode each message individually, that can cause a
      // very large performance hit for jobs with lots of log messages.
      // Instead, we'll concatenate it into a big string that we can decode
      // later.
      StringBuilder buffer = new StringBuilder();
      for (int i=0; i < logMessages.length; i++)
      {
        buffer.append(logMessages[i]);
        buffer.append('\n');
      }

      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_LOG_MESSAGES,
                           new ASN1OctetString(buffer.toString())));
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
      throw new SLAMDException("Job completed message does not include a job " +
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


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_JOB_STATE);
    if (valueElement == null)
    {
      throw new SLAMDException("Job completed message does not include the " +
                               "job state.");
    }
    else
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


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_ACTUAL_START_TIME);
    if (valueElement == null)
    {
      throw new SLAMDException("Job completed message does not include the " +
                               "actual start time.");
    }
    else
    {
      try
      {
        actualStartTime = Long.parseLong(valueElement.decodeAsOctetString().
                                              getStringValue());
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the actual start time:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_ACTUAL_STOP_TIME);
    if (valueElement == null)
    {
      throw new SLAMDException("Job completed message does not include the " +
                               "actual stop time.");
    }
    else
    {
      try
      {
        actualStopTime = Long.parseLong(valueElement.decodeAsOctetString().
                                             getStringValue());
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the actual stop time:  " + e,
                                 e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_ACTUAL_DURATION);
    if (valueElement == null)
    {
      throw new SLAMDException("Job completed message does not include the " +
                               "actual duration.");
    }
    else
    {
      try
      {
        actualDuration = Integer.parseInt(valueElement.decodeAsOctetString().
                                               getStringValue());
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the actual duration:  " + e,
                                 e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_JOB_STATISTICS);
    if (valueElement != null)
    {
      try
      {
        statTrackers =
             StatEncoder.sequenceToTrackers(valueElement.decodeAsSequence());
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job statistics:  " + e,
                                 e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_MONITOR_STATISTICS);
    if (valueElement != null)
    {
      try
      {
        monitorStatTrackers = ResourceMonitorStatTracker.sequenceToTrackers(
                                   valueElement.decodeAsSequence());
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the resource monitor " +
                                 "statistics:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_FILE_DATA);
    if (valueElement != null)
    {
      try
      {
        fileData = FileData.decodeArray(valueElement);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the file data: " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_LOG_MESSAGES);
    if (valueElement != null)
    {
      try
      {
        String messagesString =
             valueElement.decodeAsOctetString().getStringValue();
        StringTokenizer tokenizer = new StringTokenizer(messagesString, "\r\n");
        ArrayList<String> messageList = new ArrayList<String>();

        while (tokenizer.hasMoreTokens())
        {
          messageList.add(tokenizer.nextToken());
        }

        logMessages = new String[messageList.size()];
        messageList.toArray(logMessages);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the file data: " + e, e);
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
    buffer.append("jobState = ");
    buffer.append(jobState);
    buffer.append(" (");
    buffer.append(Constants.jobStateToString(jobState));
    buffer.append(')');
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("actualStartTime = ");
    buffer.append(new Date(actualStartTime));
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("actualStopTime = ");
    buffer.append(new Date(actualStopTime));
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("actualDuration = ");
    buffer.append(actualDuration);

    if ((statTrackers != null) && (statTrackers.length > 0))
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("jobStatistics =");

      for (int i=0; i < statTrackers.length; i++)
      {
        buffer.append(Constants.EOL);
        buffer.append(indentBuf);
        buffer.append("     ");
        buffer.append(statTrackers[i].getSummaryString());
      }
    }

    if ((monitorStatTrackers != null) && (monitorStatTrackers.length > 0))
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("monitorStatistics =");

      for (int i=0; i < monitorStatTrackers.length; i++)
      {
        buffer.append(Constants.EOL);
        buffer.append(indentBuf);
        buffer.append("     ");
        buffer.append(
             monitorStatTrackers[i].getStatTracker().getSummaryString());
      }
    }

    if ((fileData != null) && (fileData.length > 0))
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("fileData =");

      for (int i=0; i < fileData.length; i++)
      {
        buffer.append(Constants.EOL);
        fileData[i].toString(buffer, indent+5);
      }
    }

    if ((logMessages != null) && (logMessages.length > 0))
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("logMessages =");

      for (int i=0; i < logMessages.length; i++)
      {
        buffer.append(Constants.EOL);
        buffer.append(indentBuf);
        buffer.append("     ");
        buffer.append(logMessages[i]);
      }
    }
  }
}

