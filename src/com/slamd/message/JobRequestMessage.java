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
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class defines a message that is used by the server to submit a job
 * request to a client.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobRequestMessage
       extends Message
{
  // The length of time in seconds to use as the statistics collection interval.
  private final int collectionInterval;

  // The maximum length of time in seconds that this job should be allowed to
  // run.
  private final int duration;

  // The client number associated with this job request message.
  private final int clientNumber;

  // The number of threads that each client should create to execute this job.
  private final int threadsPerClient;

  // The length of time in milliseconds that each client should wait between
  // starting each thread.
  private final int threadStartupDelay;

  // The time at which the job processing should begin (in number of seconds
  // since January 1, 1970).
  private final long startTime;

  // The time at which the job processing should end (in number of seconds
  // since January 1, 1970).
  private final long stopTime;

  // The list of parameters that can be used to customize the way that the job
  // works.
  private final ParameterList parameters;

  // The Java class name that should be invoked to actually run the job.
  private final String jobClass;

  // The job ID for the job to be run.
  private final String jobID;



  /**
   * Creates a new job request message with the specified information.
   *
   * @param  messageID           The message ID for this message.
   * @param  jobID               The job ID of the job to be executed.
   * @param  jobClass            The name of the Java class that is a subclass
   *                             of com.slamd.job.JobClass that can be
   *                             executed to perform the work of this job.
   * @param  startTime           The time (as a Java Date) at which the job
   *                             should be started.
   * @param  stopTime            The time (as a Java Date) at which the job
   *                             should be stopped if it is still running.
   * @param  clientNumber        The client number assigned to the client for
   *                             this job request.
   * @param  duration            The maximum length of time in seconds that the
   *                             job should be allowed to run.
   * @param  threadsPerClient    The number of threads that each client should
   *                             start to run the job.
   * @param  threadStartupDelay  The delay in milliseconds that should be used
   *                             between starting each thread on the client.
   * @param  collectionInterval  The length of time in seconds to use as the
   *                             statistics collection interval.
   * @param  parameters          The list of parameters that can customize the
   *                             way that the job works.
   */
  public JobRequestMessage(int messageID, String jobID, String jobClass,
                           Date startTime, Date stopTime, int clientNumber,
                           int duration, int threadsPerClient,
                           int threadStartupDelay, int collectionInterval,
                           ParameterList parameters)
  {
    this(messageID, jobID, jobClass, startTime.getTime(),
         ((stopTime == null) ? 0 : stopTime.getTime()), clientNumber, duration,
         threadsPerClient, threadStartupDelay, collectionInterval, parameters);
  }



  /**
   * Creates a new job request message with the specified information.
   *
   * @param  messageID           The message ID for this message.
   * @param  jobID               The job ID of the job to be executed.
   * @param  jobClass            The name of the Java class that is a subclass
   *                             of com.slamd.job.JobClass that can be
   *                             executed to perform the work of this job.
   * @param  startTime           The time (as a Java Date) at which the job
   *                             should be started.
   * @param  stopTime            The time (as a Java Date) at which the job
   *                             should be stopped if it is still running.
   * @param  clientNumber        The client number associated with this job
   *                             request message.
   * @param  duration            The maximum length of time in seconds that the
   *                             job should be allowed to run.
   * @param  threadsPerClient    The number of threads that each client should
   *                             start to run the job.
   * @param  threadStartupDelay  The delay in milliseconds that should be used
   *                             between starting each thread on the client.
   * @param  collectionInterval  The length of time in seconds to use as the
   *                             statistics collection interval.
   * @param  parameters          The list of parameters that can customize the
   *                             way that the job works.
   */
  public JobRequestMessage(int messageID, String jobID, String jobClass,
                           long startTime, long stopTime, int clientNumber,
                           int duration, int threadsPerClient,
                           int threadStartupDelay, int collectionInterval,
                           ParameterList parameters)
  {
    super(messageID, Constants.MESSAGE_TYPE_JOB_REQUEST);

    this.jobID              = jobID;
    this.jobClass           = jobClass;
    this.startTime          = startTime;
    this.stopTime           = stopTime;
    this.clientNumber       = clientNumber;
    this.duration           = duration;
    this.threadsPerClient   = threadsPerClient;
    this.threadStartupDelay = threadStartupDelay;
    this.collectionInterval = collectionInterval;
    this.parameters         = parameters;
  }



  /**
   * Retrieves the job ID associated with this job request.
   *
   * @return  The job ID associated with this job request.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the name of the Java class that should be executed for this job.
   *
   * @return  The name of the Java class that should be executed for this job.
   */
  public String getJobClass()
  {
    return jobClass;
  }



  /**
   * Retrieves the time at which the job should start running.
   *
   * @return  The time at which the job should stop running.
   */
  public Date getStartTime()
  {
    if (startTime > 0)
    {
      return new Date(startTime);
    }
    else
    {
      return null;
    }
  }



  /**
   * Retrieves the time at which the job should stop running if it has not
   * already completed.
   *
   * @return  The time at which the job should stop running if it has not
   *          already completed.
   */
  public Date getStopTime()
  {
    if (stopTime > 0)
    {
      return new Date(stopTime);
    }
    else
    {
      return null;
    }
  }



  /**
   * Retrieves the client number associated with this job request message.
   *
   * @return  The client number associated with this job request message.
   */
  public int getClientNumber()
  {
    return clientNumber;
  }



  /**
   * Retrieves the maximum length of time in seconds that the job should be
   * allowed to run.
   *
   * @return  The maximum length of time in seconds that the job should be
   *          allowed to run.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Retrieves the number of threads that the client should create to execute
   * this job.
   *
   * @return  The number of threads that the client should create to execute
   *          this job.
   */
  public int getThreadsPerClient()
  {
    return threadsPerClient;
  }



  /**
   * Retrieves the number of milliseconds that a client should wait between
   * starting each thread.
   *
   * @return  The number of milliseconds that a client should wait between
   *          starting each thread.
   */
  public int getThreadStartupDelay()
  {
    return threadStartupDelay;
  }



  /**
   * Retrieves the length of time in seconds that should be used as the
   * statistics collection interval.
   *
   * @return  The length of time in seconds that should be used as the
   *          statistics collection interval.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Retrieves the list of parameters that should be used to customize the way
   * the job operates.
   *
   * @return  The list of parameters that should be used to customize the way
   *          the job operates.
   */
  public ParameterList getParameters()
  {
    return parameters;
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
    String startTimeStr = dateFormat.format(new Date(startTime));
    String stopTimeStr = "<not defined>";
    if (stopTime > 0)
    {
      stopTimeStr  = dateFormat.format(new Date(stopTime));
    }

    String paramStr = "";
    Parameter[] params = parameters.getParameters();
    for (int i=0; i < params.length; i++)
    {
      paramStr += "    " + params[i].toString() + eol;
    }

    return "Job Request Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Job ID:  " + jobID + eol +
           "  Job Class:  " + jobClass + eol +
           "  Threads per Client:  " + threadsPerClient + eol +
           "  Thread Startup Delay:  " + threadStartupDelay + eol +
           "  Start Time:  " + startTimeStr + eol +
           "  Stop Time:  " + stopTimeStr + eol +
           "  Client Number:  " + clientNumber + eol +
           "  Duration:  " + duration + eol +
           "  Collection Interval:  " + collectionInterval + eol +
           "  Job-Specific Parameters:" + eol +
           paramStr;
  }



  /**
   * Decodes the provided ASN.1 element as a job request message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the JobRequest sequence.
   *
   * @return  The job request decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a job request message.
   */
  public static JobRequestMessage decodeJobRequest(int messageID,
                                                   ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence requestSequence = null;
    try
    {
      requestSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The ASN.1 element cannot be decoded as a " +
                               "sequence", ae);
    }


    ASN1Element[] elements = requestSequence.getElements();
    if (elements.length != 10)
    {
      throw new SLAMDException("There must be ten elements in a job " +
                               "request sequence -- I counted " +
                               elements.length);
    }


    String jobID = null;
    try
    {
      jobID = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the first element as an " +
                               "octet string", ae);
    }


    String jobClass = null;
    try
    {
      jobClass = elements[1].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the second element as an " +
                               "octet string", ae);
    }



    long startTime = 0;
    try
    {
      String startTimeStr = elements[2].decodeAsOctetString().getStringValue();
      try
      {
        startTime = Long.parseLong(startTimeStr);
      }
      catch (NumberFormatException nfe)
      {
        throw new SLAMDException("Could not decode " + startTimeStr +
                                 " as a long", nfe);
      }
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the third element as an " +
                               "octet string", ae);
    }


    long stopTime = 0;
    try
    {
      String stopTimeStr = elements[3].decodeAsOctetString().getStringValue();
      try
      {
        stopTime = Long.parseLong(stopTimeStr);
      }
      catch (NumberFormatException nfe)
      {
        throw new SLAMDException("Could not decode " + stopTimeStr +
                                 " as a long", nfe);
      }
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the fourth element as an " +
                               "octet string", ae);
    }


    int clientNumber = -1;
    try
    {
      clientNumber = elements[4].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the fifth element as an " +
                               "integer", ae);
    }


    int duration = 0;
    try
    {
      duration = elements[5].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the sixth element as an " +
                               "integer", ae);
    }


    int threadsPerClient = 0;
    try
    {
      threadsPerClient = elements[6].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the seventh element as an " +
                               "integer", ae);
    }


    int threadStartupDelay = 0;
    try
    {
      threadStartupDelay = elements[7].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the eighth element as an " +
                               "integer", ae);
    }


    int collectionInterval = 0;
    try
    {
      collectionInterval = elements[8].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Could not decode the ninth element as an " +
                               "integer", ae);
    }


    ParameterList parameters = ParameterList.decode(elements[9]);


    return new JobRequestMessage(messageID, jobID, jobClass, startTime,
                                 stopTime, clientNumber, duration,
                                 threadsPerClient, threadStartupDelay,
                                 collectionInterval, parameters);
  }



  /**
   * Encodes this message into an ASN.1 element.  A job request message has
   * the following syntax:
   * <BR><BR>
   * <CODE>JobRequest ::= [APPLICATION 3] SEQUENCE {</CODE>
   * <CODE>    jobID               OCTET STRING,</CODE>
   * <CODE>    jobClass            OCTET STRING,</CODE>
   * <CODE>    startTime           OCTET STRING,</CODE>
   * <CODE>    stopTime            OCTET STRING,</CODE>
   * <CODE>    clientNumber        INTEGER,</CODE>
   * <CODE>    duration            INTEGER,</CODE>
   * <CODE>    threadsPerClient    INTEGER,</CODE>
   * <CODE>    threadStartupDelay  INTEGER,</CODE>
   * <CODE>    collectionInterval  INTEGER,</CODE>
   * <CODE>    parameters          Parameters }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1OctetString jobIDElement = new ASN1OctetString(jobID);
    ASN1OctetString jobClassElement = new ASN1OctetString(jobClass);
    ASN1OctetString startTimeElement =
         new ASN1OctetString(String.valueOf(startTime));
    ASN1OctetString stopTimeElement =
         new ASN1OctetString(String.valueOf(stopTime));
    ASN1Integer clientNumberElement = new ASN1Integer(clientNumber);
    ASN1Integer durationElement = new ASN1Integer(duration);
    ASN1Integer threadsElement = new ASN1Integer(threadsPerClient);
    ASN1Integer delayElement = new ASN1Integer(threadStartupDelay);
    ASN1Integer collectionIntervalElement = new ASN1Integer(collectionInterval);
    ASN1Element parametersElement = parameters.encode();

    ASN1Element[] jobRequestElements = new ASN1Element[]
    {
      jobIDElement,
      jobClassElement,
      startTimeElement,
      stopTimeElement,
      clientNumberElement,
      durationElement,
      threadsElement,
      delayElement,
      collectionIntervalElement,
      parametersElement
    };


    ASN1Sequence jobRequestSequence =
         new ASN1Sequence(ASN1_TYPE_JOB_REQUEST, jobRequestElements);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      jobRequestSequence
    };

    return new ASN1Sequence(messageElements);
  }
}

