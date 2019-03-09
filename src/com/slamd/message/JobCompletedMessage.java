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



import java.text.SimpleDateFormat;
import java.util.Date;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.stat.StatEncoder;
import com.slamd.stat.StatTracker;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a message that will be sent from the client to the server
 * when work on a job has completed.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobCompletedMessage
       extends Message
{
  // The actual length of time in seconds that the job was active.
  private final int actualDuration;

  // The state of the job when processing was complete.
  private final int jobState;

  // The time that the job actually started processing.
  private final long actualStartTime;

  // The time that the job actually finished processing.
  private final long actualStopTime;

  // The status counters maintained by the job.
  private final StatTracker[] statTrackers;

  // The ID for the job being processed.
  private final String jobID;

  // The set of messages that have been logged by the client.
  private final String[] logMessages;



  /**
   * Creates a new job completed message with the provided information.
   *
   * @param  messageID        The message ID for this message.
   * @param  jobID            The ID of the job that has completed.
   * @param  jobState         The state of the job when processing was complete.
   * @param  actualStartTime  The time (as a Java Date) that the job actually
   *                          started processing.
   * @param  actualStopTime   The time (as a Java Date) that the job actually
   *                          completed/was stopped.
   * @param  actualDuration   The length of time in seconds that the job was
   *                          being processed.
   * @param  statTrackers     The set of stat trackers that were maintained for
   *                          the job.
   * @param  logMessages      The messages logged by the job.
   */
  public JobCompletedMessage(int messageID, String jobID, int jobState,
                             Date actualStartTime, Date actualStopTime,
                             int actualDuration, StatTracker[] statTrackers,
                             String[] logMessages)
  {
    this(messageID, jobID, jobState,
         ((actualStartTime == null) ? 0 : actualStartTime.getTime()),
         ((actualStopTime == null) ? 0 : actualStopTime.getTime()),
         actualDuration, statTrackers, logMessages);
  }



  /**
   * Creates a new job completed message with the provided information.
   *
   * @param  messageID        The message ID for this message.
   * @param  jobID            The ID of the job that has completed.
   * @param  jobState         The state of the job when processing was complete.
   * @param  actualStartTime  The time (in number of seconds since January 1,
   *                          1970) that the job actually started processing.
   * @param  actualStopTime   The time (in number of seconds since January 1,
   *                          1970) that the job actually completed/was stopped.
   * @param  actualDuration   The length of time in seconds that the job was
   *                          being processed.
   * @param  statTrackers     The set of stat trackers that were maintained for
   *                          the job.
   * @param  logMessages      The messages logged by the job.
   */
  public JobCompletedMessage(int messageID, String jobID, int jobState,
                             long actualStartTime, long actualStopTime,
                             int actualDuration, StatTracker[] statTrackers,
                             String[] logMessages)
  {
    super(messageID, Constants.MESSAGE_TYPE_JOB_COMPLETED);

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
  }



  /**
   * Retrieves the ID of the job being processed.
   *
   * @return  The ID of the job being processed.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the state of the job being processed.
   *
   * @return  The state of the job being processed.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Retrieves the time that the job actually started being processed.
   *
   * @return  The time that the job actually started being processed.
   */
  public Date getActualStartTime()
  {
    return new Date(actualStartTime);
  }



  /**
   * Retrieves the time that the job actually finished processing.
   *
   * @return  The time that the job actually finished processing.
   */
  public Date getActualStopTime()
  {
    return new Date(actualStopTime);
  }



  /**
   * Retrieves the amount of time in seconds spent processing the job.
   *
   * @return  The amount of time in seconds spent processing the job.
   */
  public int getActualDuration()
  {
    return actualDuration;
  }



  /**
   * Retrieves the stat trackers associated with the job.
   *
   * @return  The stat trackers associated with the job.
   */
  public StatTracker[] getStatTrackers()
  {
    return statTrackers;
  }



  /**
   * Retrieves the set of log messages associated with the job.
   *
   * @return  The set of log messages associated with the job.
   */
  public String[] getLogMessages()
  {
    return logMessages;
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

    SimpleDateFormat dateFormat =
         new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    String startTimeStr = dateFormat.format(new Date(actualStartTime));
    String stopTimeStr  = dateFormat.format(new Date(actualStopTime));

    String statTrackersStr = "";
    for (int i=0; ((statTrackers != null) &&
                   (i < statTrackers.length)); i++)
    {
      statTrackersStr += "    " + statTrackers[i].getSummaryString() + eol;
    }

    String logMessagesStr = "";
    for (int i=0; ((logMessages != null) && (i < logMessages.length)); i++)
    {
      logMessagesStr += "    " + logMessages[i] + eol;
    }

    return "Job Completed Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Job State:  " + jobState + eol +
           "  Actual Start Time:  " + startTimeStr + eol +
           "  Actual Stop Time:  " + stopTimeStr + eol +
           "  Actual Duration:  " + actualDuration + eol +
           "  Stat Trackers:" + eol +
           statTrackersStr +
           "  Log Messages:" + eol +
           logMessagesStr;
  }



  /**
   * Decodes the provided ASN.1 element as a job completed message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the JobCompleted sequence.
   *
   * @return  The job completed message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a job completed message.
   */
  public static JobCompletedMessage decodeJobCompleted(int messageID,
                                                       ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence completedSequence = null;
    try
    {
      completedSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      ae.printStackTrace();
      throw new SLAMDException("Could not decode ASN.1 element as a sequence",
                               ae);
    }


    ASN1Element[] elements = completedSequence.getElements();
    if (elements.length != 7)
    {
      throw new SLAMDException("A job completed message must have seven " +
                               "elements");
    }


    String jobID = null;
    try
    {
      jobID = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode first element as an octet " +
                               "string", ae);
    }


    int jobState = 0;
    try
    {
      jobState = elements[1].decodeAsEnumerated().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode second element as an enumerated",
                               ae);
    }


    long actualStartTime = 0;
    try
    {
      String startTimeStr = elements[2].decodeAsOctetString().getStringValue();
      try
      {
        actualStartTime = Long.parseLong(startTimeStr);
      }
      catch (NumberFormatException nfe)
      {
        throw new SLAMDException("Could not convert " + startTimeStr +
                                 "to a long", nfe);
      }
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode third element as an octet " +
                               "string", ae);
    }


    long actualStopTime = 0;
    try
    {
      String stopTimeStr = elements[3].decodeAsOctetString().getStringValue();
      try
      {
        actualStopTime = Long.parseLong(stopTimeStr);
      }
      catch (NumberFormatException nfe)
      {
        throw new SLAMDException("Could not convert " + stopTimeStr +
                                 "to a long", nfe);
      }
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode fourth element as an octet " +
                               "string", ae);
    }


    int actualDuration = 0;
    try
    {
      actualDuration = elements[4].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode fifth element as an integer", ae);
    }


    StatTracker[] statTrackers = null;
    try
    {
      ASN1Sequence trackerSequence = elements[5].decodeAsSequence();
      statTrackers = StatEncoder.sequenceToTrackers(trackerSequence);
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode sixth element as a sequence " +
                               "of stat trackers", ae);
    }


    String[] logMessages = null;
    try
    {
      ASN1Element[] logElements = elements[6].decodeAsSequence().getElements();
      logMessages = new String[logElements.length];
      for (int i=0; i < logElements.length; i++)
      {
        logMessages[i] = logElements[i].decodeAsOctetString().getStringValue();
      }
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Cannot decode seventh element as a sequence " +
                               "of octet strings", ae);
    }


    return new JobCompletedMessage(messageID, jobID, jobState, actualStartTime,
                                   actualStopTime, actualDuration, statTrackers,
                                   logMessages);
  }



  /**
   * Encodes this message into an ASN.1 element.  A job completed message has
   * the following syntax:
   * <BR><BR>
   * <CODE>JobCompleted ::= [APPLICATION 7] SEQUENCE {</CODE>
   * <CODE>    jobID            OCTET STRING,</CODE>
   * <CODE>    jobState         JobState,</CODE>
   * <CODE>    actualStartTime  OCTET STRING,</CODE>
   * <CODE>    actualStopTime   OCTET STRING,</CODE>
   * <CODE>    actualDuration   INTEGER,</CODE>
   * <CODE>    statTrackers     StatTrackers,</CODE>
   * <CODE>    logMessages      LogMessages }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1OctetString idElement = new ASN1OctetString(jobID);
    ASN1Enumerated stateElement = new ASN1Enumerated(jobState);
    ASN1OctetString startTimeElement =
         new ASN1OctetString(String.valueOf(actualStartTime));
    ASN1OctetString stopTimeElement =
         new ASN1OctetString(String.valueOf(actualStopTime));
    ASN1Integer durationElement = new ASN1Integer(actualDuration);
    ASN1Sequence trackerElement = StatEncoder.trackersToSequence(statTrackers);

    ASN1Element[] messagesElements = new ASN1Element[logMessages.length];
    for (int i=0; i < messagesElements.length; i++)
    {
      messagesElements[i] = new ASN1OctetString(logMessages[i]);
    }
    ASN1Sequence logSequence = new ASN1Sequence(messagesElements);


    ASN1Element[] jobCompletedElements = new ASN1Element[]
    {
      idElement,
      stateElement,
      startTimeElement,
      stopTimeElement,
      durationElement,
      trackerElement,
      logSequence
    };

    ASN1Sequence jobCompletedSequence =
         new ASN1Sequence(ASN1_TYPE_JOB_COMPLETED, jobCompletedElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      jobCompletedSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

