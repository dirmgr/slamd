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

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class defines a SLAMD message that will be sent from the server to the
 * client to provide it with information about a job that is to be processed.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobRequest
       extends SLAMDMessage
{
  // Indicates whether the client should report in-progress statistics for the
  // job.
  private boolean reportInProgressStats;

  // The time that job processing should start.
  private Date startTime;

  // The time that job processing should end.
  private Date stopTime;

  // The client number assigned to the client being given this request.
  private int clientNumber;

  // The statistics collection interval in seconds for the job.
  private int collectionInterval;

  // The maximum length of time in seconds that the job should run.
  private int duration;

  // The interval to use when reporting in-progress statistics.
  private int inProgressReportInterval;

  // The version of the job class to use when running the job.
  private int jobClassVersion;

  // The number of clients to use to process the job.
  private int numClients;

  // The number of threads per client to use to process the job.
  private int threadsPerClient;

  // The delay in milliseconds between the startups for each of the threads.
  private int threadStartupDelay;

  // The list of parameters to use when running the job.
  private ParameterList parameterList;

  // The fully-qualified name of the job class to use for the job.
  private String jobClassName;

  // The job ID for the job to process.
  private String jobID;



  /**
   * Creates a new instance of this job request message which is intended for
   * use in decoding a message transmitted between the server and the client.
   * It is not intended for general use.
   */
  public JobRequest()
  {
    super();

    reportInProgressStats    = false;
    startTime                = null;
    stopTime                 = null;
    clientNumber             = -1;
    collectionInterval       = -1;
    duration                 = -1;
    inProgressReportInterval = -1;
    jobClassVersion          = -1;
    numClients               = -1;
    threadsPerClient         = -1;
    threadStartupDelay       = -1;
    parameterList            = new ParameterList();
    jobClassName             = null;
    jobID                    = null;
  }



  /**
   * Creates a new instance of this job request message with the provided
   * information.
   *
   * @param  messageID                 The message ID for this SLAMD message.
   * @param  extraProperties           The "extra" properties for this SLAMD
   *                                   message.  Both the names and values for
   *                                   the properties must be strings.
   * @param  jobID                     The job ID for the job to process.
   * @param  jobClassName              The fully-qualified name of the job class
   *                                   for the job to process.
   * @param  jobClassVersion           The job class version for the job to
   *                                   process.
   * @param  numClients                The number of clients that should be used
   *                                   to process the job.
   * @param  threadsPerClient          The number of threads per client that
   *                                   should be used to process the job.
   * @param  startTime                 The time that processing should begin for
   *                                   the job.
   * @param  stopTime                  The time that processing should stop for
   *                                   the job.
   * @param  duration                  The maximum length of time in seconds
   *                                   that the job should run.
   * @param  collectionInterval        The statistics collection interval for
   *                                   the job.
   * @param  threadStartupDelay        The delay in milliseconds that should be
   *                                   used between the startup for each job
   *                                   thread.
   * @param  parameterList             The list of parameters to use when
   *                                   processing the job.
   * @param  reportInProgressStats     Indicates whether the client should
   *                                   periodically report its in-progress
   *                                   statistics to the server.
   * @param  inProgressReportInterval  The interval to use when reporting
   *                                   in-progress statistics to the server.
   */
  public JobRequest(int messageID, HashMap<String,String> extraProperties,
                    String jobID, String jobClassName, int jobClassVersion,
                    int numClients, int threadsPerClient, Date startTime,
                    Date stopTime, int duration, int collectionInterval,
                    int threadStartupDelay, ParameterList parameterList,
                    boolean reportInProgressStats, int inProgressReportInterval)
  {
    super(messageID, extraProperties);

    this.jobID                    = jobID;
    this.jobClassName             = jobClassName;
    this.jobClassVersion          = jobClassVersion;
    this.numClients               = numClients;
    this.threadsPerClient         = threadsPerClient;
    this.startTime                = startTime;
    this.stopTime                 = stopTime;
    this.duration                 = duration;
    this.collectionInterval       = collectionInterval;
    this.threadStartupDelay       = threadStartupDelay;
    this.parameterList            = parameterList;
    this.reportInProgressStats    = reportInProgressStats;
    this.inProgressReportInterval = inProgressReportInterval;

    clientNumber = -1;
  }



  /**
   * Retrieves the job ID for the job to process.
   *
   * @return  The job ID for the job to process.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Specifies the job ID for the job to process.
   *
   * @param  jobID  The job ID for the job to process.
   */
  public void setJobID(String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Retrieves the fully-qualified name of the job class for the job to process.
   *
   * @return  The fully-qualified name of the job class for the job to process.
   */
  public String getJobClassName()
  {
    return jobClassName;
  }



  /**
   * Specifies the fully-qualified name of the job class for the job to process.
   *
   * @param  jobClassName  The fully-qualified name of the job class for the job
   *                       to process.
   */
  public void setJobClassName(String jobClassName)
  {
    this.jobClassName = jobClassName;
  }



  /**
   * Retrieves the job class version for the job to process.
   *
   * @return  The job class version for the job to process, or -1 if no version
   *          information is available.
   */
  public int getJobClassVersion()
  {
    return jobClassVersion;
  }



  /**
   * Specifies the job class version for the job to process.
   *
   * @param  jobClassVersion  The job class version for the job to process.
   */
  public void setJobClassVersion(int jobClassVersion)
  {
    this.jobClassVersion = jobClassVersion;
  }



  /**
   * Retrieves the number of clients that should be used to process the job.
   *
   * @return  The number of clients that should be used to process the job.
   */
  public int getNumClients()
  {
    return numClients;
  }



  /**
   * Specifies the number of clients that should be used to process the job.
   *
   * @param  numClients  The number of clients that should be used to process
   *                     the job.
   */
  public void setNumClients(int numClients)
  {
    this.numClients = numClients;
  }



  /**
   * Retrieves the number of threads per client that should be used to process
   * the job.
   *
   * @return  The number of threads per client that should be used to process
   *          the job.
   */
  public int getThreadsPerClient()
  {
    return threadsPerClient;
  }



  /**
   * Specifies the number of threads per client that should be used to process
   * the job.
   *
   * @param  threadsPerClient  The number of threads per client that should be
   *                           used to process the job.
   */
  public void setThreadsPerClient(int threadsPerClient)
  {
    this.threadsPerClient = threadsPerClient;
  }



  /**
   * Retrieves the time at which processing should start for the job.
   *
   * @return  The time at which processing should start for the job.
   */
  public Date getStartTime()
  {
    return startTime;
  }



  /**
   * Specifies the time at which processing should start for the job.
   *
   * @param  startTime  The time at which processing should start for the job.
   */
  public void setStartTime(Date startTime)
  {
    this.startTime = startTime;
  }



  /**
   * Retrieves the time at which processing should stop for the job.
   *
   * @return  The time at which processing should stop for the job, or
   *          <CODE>null</CODE> if no stop time has been specified.
   */
  public Date getStopTime()
  {
    return stopTime;
  }



  /**
   * Specifies the time at which processing should stop for the job.
   *
   * @param  stopTime  The time at which processing should stop for the job.
   */
  public void setStopTime(Date stopTime)
  {
    this.stopTime = stopTime;
  }



  /**
   * Retrieves the maximum length of time in seconds that the job should run.
   *
   * @return  The maximum length of time in seconds that the job should run, or
   *          a value less than or equal to zero if there is no duration.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Specifies the maximum length of time in seconds that the job should run.
   *
   * @param  duration  The maximum length of time in seconds that the job should
   *                   run.
   */
  public void setDuration(int duration)
  {
    this.duration = duration;
  }



  /**
   * Retrieves the statistics collection interval for the job.
   *
   * @return  The statistics collection interval for the job.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Specifies the statistics collection interval for the job.
   *
   * @param  collectionInterval  The statistics collection interval for the job.
   */
  public void setCollectionInterval(int collectionInterval)
  {
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves the delay in milliseconds that should be used when starting the
   * individual threads for the job.
   *
   * @return  The delay in milliseconds that should be used when starting the
   *          individual threads for the job, or a value less than or equal to
   *          zero to indicate that there should not be any delay.
   */
  public int getThreadStartupDelay()
  {
    return threadStartupDelay;
  }



  /**
   * Speifies the delay in milliseconds that should be used when starting the
   * individual threads for the job.
   *
   * @param  threadStartupDelay  The delay in milliseconds that should be used
   *                             when starting the individual threads for the
   *                             job.
   */
  public void setThreadStartupDelay(int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
  }



  /**
   * Retrieves the set of parameters that should be used to process the job.
   *
   * @return  The set of parameters that should be used to process the job.
   */
  public ParameterList getParameterList()
  {
    return parameterList;
  }



  /**
   * Specifies the set of parameters that should be used to process the job.
   *
   * @param  parameterList  The set of parameters that should be used to process
   *                        the job.
   */
  public void setParameterList(ParameterList parameterList)
  {
    this.parameterList = parameterList;
  }



  /**
   * Indicates whether the client should periodically report the collected
   * results to the server.
   *
   * @return  <CODE>true</CODE> if the client should periodically report the
   *          collected results to the server, or <CODE>false</CODE> if not.
   */
  public boolean reportInProgressStats()
  {
    return reportInProgressStats;
  }



  /**
   * Specifies whether the client should periodically report the collected
   * results to the server.
   *
   * @param  reportInProgressStats  Specifies whether the client should
   *                                periodically report the collected results to
   *                                the server.
   */
  public void setReportInProgressStats(boolean reportInProgressStats)
  {
    this.reportInProgressStats = reportInProgressStats;
  }



  /**
   * Retrieves the interval that the client should use to report the in-progress
   * results to the server.  This is irrelevant for cases in which real-time
   * reporting is not enabled.
   *
   * @return  The interval that the client should use to report the in-progress
   *          results to the server.
   */
  public int getInProgressResportInterval()
  {
    return inProgressReportInterval;
  }



  /**
   * Specifies the interval that the client should use to report the in-progress
   * results to the server.
   *
   * @param  inProgressReportInterval  The interval that the client should use
   *                                   to report the in-progress results to the
   *                                   server.
   */
  public void setInProgressReportInterval(int inProgressReportInterval)
  {
    this.inProgressReportInterval = inProgressReportInterval;
  }



  /**
   * Retrieves the client number for the client to which this request is being
   * sent.  This value should be  different for each client that receives this
   * job request, with the first client number being zero and subsequent clients
   * increasing this value by one.
   *
   * @return  The client number for the client to which this request is being
   *          sent.
   */
  public int getClientNumber()
  {
    return clientNumber;
  }



  /**
   * Specifies the client number for the client to which this request is being
   * sent.  This method must be called to set the client number before sending
   * the job request message to that client.  The first client should receive a
   * client number of zero, with the value incrementing sequentially for each
   * subsequent client.
   *
   * @param  clientNumber  The client number for the client to which this
   *                       request is being sent.
   */
  public void setClientNumber(int clientNumber)
  {
    this.clientNumber = clientNumber;
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
                         ProtocolConstants.PROPERTY_JOB_CLASS_NAME,
                         new ASN1OctetString(jobClassName)));

    if (jobClassVersion > 0)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_JOB_CLASS_VERSION,
                           new ASN1Integer(jobClassVersion)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_NUM_CLIENTS,
                         new ASN1Integer(numClients)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_THREADS_PER_CLIENT,
                         new ASN1Integer(threadsPerClient)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_START_TIME,
         new ASN1OctetString(String.valueOf(startTime.getTime()))));

    if (stopTime != null)
    {
      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_STOP_TIME,
           new ASN1OctetString(String.valueOf(stopTime.getTime()))));
    }

    if (duration > 0)
    {
      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_DURATION,
                                          new ASN1Integer(duration)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_COLLECTION_INTERVAL,
                         new ASN1Integer(collectionInterval)));

    if (threadStartupDelay > 0)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_THREAD_STARTUP_DELAY,
                           new ASN1Integer(threadStartupDelay)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_PARAMETER_LIST,
                         parameterList.encode()));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_REPORT_IN_PROGRESS_STATS,
                         new ASN1Boolean(reportInProgressStats)));

    if (reportInProgressStats)
    {
      elementList.add(encodeNameValuePair(
           ProtocolConstants.PROPERTY_IN_PROGRESS_REPORT_INTERVAL,
           new ASN1Integer(inProgressReportInterval)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_CLIENT_NUMBER,
                         new ASN1Integer(clientNumber)));

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
      throw new SLAMDException("Job request message does not include the job " +
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

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_JOB_CLASS_NAME);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the job " +
                               "class name.");
    }
    else
    {
      try
      {
        jobClassName = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job class name:  " + e,
                                 e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_JOB_CLASS_VERSION);
    if (valueElement != null)
    {
      try
      {
        jobClassVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job class version:  " +
                                 e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_NUM_CLIENTS);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the " +
                               "number of clients.");
    }
    else
    {
      try
      {
        numClients = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the number of clients:  " +
                                 e, e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_THREADS_PER_CLIENT);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the " +
                               "number of threads per client.");
    }
    else
    {
      try
      {
        numClients = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the number of threads per " +
                                 "client:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_START_TIME);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the " +
                               "start time.");
    }
    else
    {
      try
      {
        String startTimeStr =
             valueElement.decodeAsOctetString().getStringValue();
        startTime = new Date(Long.parseLong(startTimeStr));
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the start time:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_STOP_TIME);
    if (valueElement != null)
    {
      try
      {
        String stopTimeStr =
             valueElement.decodeAsOctetString().getStringValue();
        stopTime = new Date(Long.parseLong(stopTimeStr));
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the stop time:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_DURATION);
    if (valueElement != null)
    {
      try
      {
        duration = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the duration:  " + e, e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_COLLECTION_INTERVAL);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the " +
                               "statistics collection interval.");
    }
    else
    {
      try
      {
        collectionInterval = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the statistics collection " +
                                 "interval:  " + e, e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_THREAD_STARTUP_DELAY);
    if (valueElement != null)
    {
      try
      {
        threadStartupDelay = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the thread startup " +
                                 "delay:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_PARAMETER_LIST);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the " +
                               "job parameter list.");
    }
    else
    {
      try
      {
        parameterList = ParameterList.decode(valueElement);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the job parameter list:  " +
                                 e, e);
      }
    }

    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_REPORT_IN_PROGRESS_STATS);
    if (valueElement != null)
    {
      try
      {
        reportInProgressStats =
             valueElement.decodeAsBoolean().getBooleanValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the reportInProgressStats " +
                                 "flag:  " + e, e);
      }
    }

    valueElement = propertyMap.get(
         ProtocolConstants.PROPERTY_IN_PROGRESS_REPORT_INTERVAL);
    if (valueElement != null)
    {
      try
      {
        inProgressReportInterval = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the in progress report " +
                                 "interval:  " + e, e);
      }
    }

    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_NUMBER);
    if (valueElement == null)
    {
      throw new SLAMDException("Job request message does not include the " +
                               "client number.");
    }
    else
    {
      try
      {
        clientNumber = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client number:  " + e,
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
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("jobClassName = ");
    buffer.append(jobClassName);
    buffer.append(Constants.EOL);

    if (jobClassVersion > 0)
    {
      buffer.append(indentBuf);
      buffer.append("jobClassVersion = ");
      buffer.append(jobClassVersion);
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("numClients = ");
    buffer.append(numClients);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("threadsPerClient = ");
    buffer.append(threadsPerClient);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("startTime = ");
    buffer.append(startTime);
    buffer.append(Constants.EOL);

    if (stopTime != null)
    {
      buffer.append(indentBuf);
      buffer.append("stopTime = ");
      buffer.append(stopTime);
      buffer.append(Constants.EOL);
    }

    if (duration > 0)
    {
      buffer.append(indentBuf);
      buffer.append("duration = ");
      buffer.append(duration);
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("collectionInterval = ");
    buffer.append(collectionInterval);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("threadStartupDelay = ");
    buffer.append(threadStartupDelay);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("parameterList =");
    buffer.append(Constants.EOL);
    Parameter[] params = parameterList.getParameters();
    for (int i=0; i < params.length; i++)
    {
      buffer.append(indentBuf);
      buffer.append("     ");
      buffer.append(params[i].getDisplayName());
      buffer.append(" = ");
      buffer.append(params[i].getDisplayValue());
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("reportInProgressStats = ");
    buffer.append(reportInProgressStats);
    buffer.append(Constants.EOL);

    if (reportInProgressStats)
    {
      buffer.append(indentBuf);
      buffer.append("inProgressResportInterval = ");
      buffer.append(inProgressReportInterval);
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("clientNumber = ");
    buffer.append(clientNumber);
  }
}

