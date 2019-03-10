/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.job;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.db.DecodeException;
import com.slamd.message.JobCompletedMessage;
import com.slamd.message.JobControlResponseMessage;
import com.slamd.message.JobResponseMessage;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.resourcemonitor.LegacyResourceMonitor;
import com.slamd.resourcemonitor.ResourceMonitor;
import com.slamd.server.ClientConnection;
import com.slamd.server.RealTimeJobStats;
import com.slamd.server.ResourceMonitorClientConnection;
import com.slamd.server.SLAMDServer;
import com.slamd.stat.ResourceMonitorStatTracker;
import com.slamd.stat.StatEncoder;
import com.slamd.stat.StatTracker;



/**
 * This class defines a job that can be run by the SLAMD server.  Most of the
 * information in this class is used for administrative purposes.  All of the
 * actual processing is defined in the job class associated with each job
 * implementation -- this class merely handles starting the appropriate number
 * of job threads on the appropriate set of clients.
 *
 *
 * @author   Neil A. Wilson
 */
public final class Job
       implements Comparable<Job>, JobItem
{
  /**
   * The name of the encoded element that holds the job ID.
   */
  private static final String ELEMENT_JOB_ID = "job_id";



  /**
   * The name of the encoded element that holds the job class name.
   */
  private static final String ELEMENT_JOB_CLASS = "job_class";



  /**
   * The name of the encoded element that holds the optimizing job ID.
   */
  private static final String ELEMENT_OPTIMIZING_JOB_ID = "optimizing_job_id";



  /**
   * The name of the encoded element that holds the job group name.
   */
  private static final String ELEMENT_JOB_GROUP = "job_group";



  /**
   * The name of the encoded element that holds the folder name.
   */
  private static final String ELEMENT_FOLDER = "folder";



  /**
   * The name of the encoded element that holds the job state.
   */
  private static final String ELEMENT_JOB_STATE = "job_state";



  /**
   * The name of the encoded element that indicates whether this job should be
   * displayed in restricted read-only mode.
   */
  private static final String ELEMENT_DISPLAY_IN_READ_ONLY =
       "display_in_read_only";



  /**
   * The name of the encoded element that holds the job description.
   */
  private static final String ELEMENT_DESCRIPTION = "description";



  /**
   * The name of the encoded element that holds the scheduled start time.
   */
  private static final String ELEMENT_START_TIME = "start_time";



  /**
   * The name of the encoded element that holds the scheduled stop time.
   */
  private static final String ELEMENT_STOP_TIME = "stop_time";



  /**
   * The name of the encoded element that holds the scheduled duration.
   */
  private static final String ELEMENT_DURATION = "duration";



  /**
   * The name of the encoded element that holds the number of clients.
   */
  private static final String ELEMENT_NUM_CLIENTS = "num_clients";



  /**
   * The name of the encoded element that holds the requested clients.
   */
  private static final String ELEMENT_REQUESTED_CLIENTS = "requested_clients";



  /**
   * The name of the encoded element that holds the requested resource monitor
   * clients.
   */
  private static final String ELEMENT_MONITOR_CLIENTS = "monitor_clients";



  /**
   * The name of the encoded element that indicates whether the clients should
   * be monitored if they are running resource monitor clients.
   */
  private static final String ELEMENT_MONITOR_CLIENTS_IF_AVAILABLE =
       "monitor_clients_if_available";



  /**
   * The name of the encoded element that indicates whether to wait for clients
   * to become available.
   */
  private static final String ELEMENT_WAIT_FOR_CLIENTS = "wait_for_clients";



  /**
   * The name of the encoded element that holds the number of threads per
   * client.
   */
  private static final String ELEMENT_THREADS_PER_CLIENT = "threads_per_client";



  /**
   * The name of the encoded element that holds the thread startup delay.
   */
  private static final String ELEMENT_THREAD_STARTUP_DELAY =
       "thread_startup_delay";



  /**
   * The name of the encoded element that holds the set of dependencies.
   */
  private static final String ELEMENT_DEPENDENCIES = "dependencies";



  /**
   * The name of the encoded element that holds the e-mail addresses of the
   * users to notify on completion.
   */
  private static final String ELEMENT_NOTIFY_ADDRESSES = "notify_addresses";



  /**
   * The name of the encoded element that holds the statistics collection
   * interval.
   */
  private static final String ELEMENT_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the encoded element that holds the job comments.
   */
  private static final String ELEMENT_COMMENTS = "comments";



  /**
   * The name of the encoded element that holds the job parameters.
   */
  private static final String ELEMENT_PARAMETERS = "parameters";



  /**
   * The name of the encoded element that holds the actual start time.
   */
  private static final String ELEMENT_ACTUAL_START_TIME = "actual_start_time";



  /**
   * The name of the encoded element that holds the actual stop time.
   */
  private static final String ELEMENT_ACTUAL_STOP_TIME = "actual_stop_time";



  /**
   * The name of the encoded element that holds the actual duration.
   */
  private static final String ELEMENT_ACTUAL_DURATION = "actual_duration";



  /**
   * The name of the encoded element that holds the job statistics.
   */
  private static final String ELEMENT_STATS = "stats";



  /**
   * The name of the encoded element that holds legacy resource monitor
   * statistical data (just stat trackers, not resource monitor stats).
   */
  private static final String ELEMENT_LEGACY_MONITOR_STATS = "monitor_stats";



  /**
   * The name of the encoded element that holds resource monitor statistics.
   */
  private static final String ELEMENT_RESOURCE_MONITOR_STATS = "resource_stats";



  /**
   * The name of the encoded element that holds the log messages.
   */
  private static final String ELEMENT_LOG_MESSAGES = "log_messages";



  // Indicates whether this job should be displayed in restricted read-only
  // mode.
  private boolean displayInReadOnlyMode;

  // Indicates whether an attempt should be made to use any resource monitor
  // clients running on the same system as the client system(s) used to process
  // this job.
  private boolean monitorClientsIfAvailable;

  // Indicates whether the execution of this job will be delayed until the
  // requested number of clients are available, or if execution should be
  // cancelled if the requested number of clients are not available.
  private boolean waitForClients;

  // The time that the job actually started running.
  private Date actualStartTime;

  // The time that the job actually stopped running.
  private Date actualStopTime;

  // The time at which this job should start running.
  private Date startTime;

  // The time at which this job should stop running.
  private Date stopTime;

  // The actual length of time that the job was active.
  private int actualDuration;

  // The length of time in seconds to use as the statistics collection interval.
  private int collectionInterval;

  // The maximum length of time in seconds that the job should be allowed to
  // run.
  private int duration;

  // Indicates the current state of the job.  The value should be one of the
  // JOB_STATE_* constants.
  private int jobState;

  // The number of clients that should be used to execute this job.
  private int numClients;

  // The job state that will be assigned to the job when it has completed
  // processing.  This will default to "completed successfully", but will change
  // to reflect any other stop reason.
  private int tentativeJobState;

  // The number of concurrent job threads that should run on each client.
  private int threadsPerClient;

  // The delay in milliseconds that should be used when starting each thread on
  // the client.
  private int threadStartupDelay;

  // The set of clients that are actively processing this job.
  private List<ClientConnection> activeClients;

  // The set of resource monitor clients that are currently active.
  private List<ResourceMonitorClientConnection> activeMonitorClients;

  // The set of job IDs that must complete before this job may begin.
  private List<String> dependencies;

  // The set of messages logged during job execution.
  private List<String> logMessages;

  // The set of stat trackers maintained by the resource monitor clients.
  private List<ResourceMonitorStatTracker> resourceStatTrackers;

  // The set of stat trackers associated with this job.
  private List<StatTracker> statTrackers;

  // The mutex used to provide threadsafe access to the list of active clients.
  private final Object activeClientMutex = new Object();

  // The set of parameters that can customize the behavior of the job.
  private ParameterList parameters;

  // The set of client connections associated with the clients that are running
  // this job.
  private ClientConnection[] clientConnections;

  // The real-time stats associated with this job.
  private RealTimeJobStats realTimeStats;

  // The set of connections to the resource monitor clients for this job.
  private ResourceMonitorClientConnection[] monitorConnections;

  // The job thread that will be used to obtain information about the actual
  // work to be performed.
  private JobClass infoJobThread;

  // The SLAMD server with which this job is associated
  protected SLAMDServer slamdServer;

  // The name of the folder in which this job is located.
  private String folderName;

  // Comments about the job.
  private String jobComments;

  // A user-specified description to use for the job.
  private String jobDescription;

  // The name of the job group with which this job is associated.
  private String jobGroup;

  // The unique ID assigned to this job
  private String jobID;

  // The name of the Java class file that serves as the job thread
  private String jobThreadClassName;

  // The job ID of the optimizing job with which this job is associated.
  private String optimizingJobID;

  // The addresses of the users that should be notified when this job is
  // complete.
  private String[] notifyAddresses;

  // The set of resource monitor clients that have been requested for this job.
  private String[] monitorClients;

  // The set of clients that have been requested to run this job.
  private String[] requestedClients;



  /**
   * Creates a new instance of the job based on the specified job thread class.
   * This version of the constructor is to be used only for the purpose of
   * obtaining information about the job -- not for actually running it.
   *
   * @param  slamdServer         The SLAMD server with which this job is
   *                             associated.
   * @param  jobThreadClassName  The fully-qualified name of the Java class that
   *                             will actually be invoked to perform the work of
   *                             this job.
   *
   * @throws  SLAMDException  If it is not possible to create an instance of the
   *                          job class.
   */
  public Job(final SLAMDServer slamdServer, final String jobThreadClassName)
         throws SLAMDException
  {
    // Assign the values of the parameters to instance variables
    this.slamdServer        = slamdServer;
    this.jobThreadClassName = jobThreadClassName;


    // This is a fallback condition that will be used if this constructor can't
    // complete without an exception.  If that occurs, then the job won't be
    // able to run.
    jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;


    infoJobThread = slamdServer.getOrLoadJobClass(jobThreadClassName);


    // Indicate that enough of the constructor has completed so that we won't
    // have stopped due to error.
    jobState = Constants.JOB_STATE_UNINITIALIZED;
  }



  /**
   * Creates a new instance of the job based on the specified job thread class
   * that may be actually used to run the job.
   *
   * @param  slamdServer            The SLAMD server with which this job is
   *                                associated.
   * @param  jobThreadClassName     The fully-qualified name of the Java class
   *                                that will actually be invoked to perform the
   *                                work of this job.
   * @param  numClients             The number of clients that should be used to
   *                                execute the job.
   * @param  threadsPerClient       The number of concurrent job thread
   *                                instances that will run on each client.
   * @param  threadStartupDelay     The delay in milliseconds that should be
   *                                used when starting each thread on the client
   *                                system.
   * @param  startTime              The time at which the job should start
   *                                running.
   * @param  stopTime               The time at which the job should stop
   *                                running.
   * @param  duration               The maximum length of time in seconds that
   *                                the job should be allowed to run.
   * @param  collectionInterval     The length of time in seconds to use as the
   *                                statistics collection interval.
   * @param  parameters             The parameter list that provides the details
   *                                of how the job should run.
   * @param  displayInReadOnlyMode  Indicates whether this job should be
   *                                displayed in restricted read-only mode.
   *
   * @throws  SLAMDException  If a problem occurred while creating an instance
   *                          of the job thread class.
   */
  public Job(final SLAMDServer slamdServer, final String jobThreadClassName,
             final int numClients, final int threadsPerClient,
             final int threadStartupDelay, final Date startTime,
             final Date stopTime, int duration, final int collectionInterval,
             final ParameterList parameters,
             final boolean displayInReadOnlyMode)
         throws SLAMDException
  {
    // Invoke the first version of the constructor
    this(slamdServer, jobThreadClassName);


    // Set all of the other instance variables
    this.numClients            = numClients;
    this.threadsPerClient      = threadsPerClient;
    this.threadStartupDelay    = threadStartupDelay;
    this.startTime             = startTime;
    this.stopTime              = stopTime;
    this.duration              = duration;
    this.collectionInterval    = collectionInterval;
    this.parameters            = parameters;
    this.displayInReadOnlyMode = displayInReadOnlyMode;
    waitForClients             = false;
    optimizingJobID            = null;
    jobComments                = null;
    jobDescription             = null;
    actualStartTime            = null;
    actualStopTime             = null;
    actualDuration             = -1;
    notifyAddresses            = new String[0];
    dependencies               = new ArrayList<>();
    monitorClientsIfAvailable  = false;
    monitorClients             = null;
    requestedClients           = null;
    statTrackers               = new ArrayList<>();
    resourceStatTrackers       = new ArrayList<>();
    tentativeJobState          = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
    logMessages                = new ArrayList<>();


    // Create the variables for working with the list of clients that are
    // actively processing this job.
    activeClients        = new ArrayList<>();
    activeMonitorClients = new ArrayList<>();


    jobState = Constants.JOB_STATE_NOT_YET_STARTED;
  }



  /**
   * Retrieves an instance of the job class with which this job is associated.
   *
   * @return  An instance of the job class with which this job is associated,
   *          or {@code null} if it could not be retrieved for some reason.
   */
  @Override()
  public JobClass getJobClass()
  {
    if (infoJobThread != null)
    {
      return infoJobThread;
    }

    try
    {
      return slamdServer.getOrLoadJobClass(jobThreadClassName);
    }
    catch (SLAMDException se)
    {
      return null;
    }
  }



  /**
   * Retrieves the job ID associated with this job.
   *
   * @return  The job ID associated with this job.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Sets the job ID for this job.
   *
   * @param  jobID  The job ID to use for this job.
   */
  public void setJobID(final String jobID)
  {
    this.jobID = jobID;
  }



  /**
   * Retrieves the ID of the optimizing job with which this job is associated.
   *
   * @return  The ID of the optimizing job with which this job is associated, or
   *          {@code null} if it is not associated with an optimizing job.
   */
  public String getOptimizingJobID()
  {
    return optimizingJobID;
  }



  /**
   * Specifies the ID of the optimizing job with which this job is associated.
   *
   * @param  optimizingJobID  The ID of the optimizing job with which this job
   *                          is associated.
   */
  public void setOptimizingJobID(final String optimizingJobID)
  {
    this.optimizingJobID = optimizingJobID;
  }



  /**
   * Retrieves the name of the job group with which this job is associated.
   *
   * @return  The name of the job group with which this job is associated, or
   *          {@code null} if it was not scheduled as part of any job
   *          group.
   */
  @Override()
  public String getJobGroup()
  {
    return jobGroup;
  }



  /**
   * Specifies the name of the job group with which this job is associated.
   *
   * @param  jobGroup  The name of the job group with which this job is
   *                   associated.
   */
  public void setJobGroup(final String jobGroup)
  {
    this.jobGroup = jobGroup;
  }



  /**
   * Retrieves the name of the folder in which this job is located.
   *
   * @return  The name of the folder in which this job is located.
   */
  @Override()
  public String getFolderName()
  {
    return folderName;
  }



  /**
   * Specifies the name of the folder in which this job is located.
   *
   * @param  folderName  The name of the folder in which this job is located.
   */
  public void setFolderName(final String folderName)
  {
    this.folderName = folderName;
  }



  /**
   * Retrieves the user-specified comments for this job.
   *
   * @return  The user-specified comments for this job, or {@code null} if
   *          no comments have been made.
   */
  public String getJobComments()
  {
    return jobComments;
  }



  /**
   * Specifies a set of comments for this job.
   *
   * @param  jobComments  The set of comments to use for this job.
   */
  public void setJobComments(final String jobComments)
  {
    this.jobComments = jobComments;
  }



  /**
   * Retrieves a user-specified description for this job.
   *
   * @return  A description for this job, or {@code null} if no description
   *          has been provided.
   */
  public String getJobDescription()
  {
    return jobDescription;
  }



  /**
   * Specifies the description that should be used for this job.
   *
   * @param  jobDescription  The job description that should be used for this
   *                         job.
   */
  public void setJobDescription(final String jobDescription)
  {
    this.jobDescription = jobDescription;
  }



  /**
   * Retrieves the name of the Java class that this job will execute to perform
   * the work.
   *
   * @return  The name of the Java class that this job will execute to perform
   *          the work.
   */
  public String getJobClassName()
  {
    return jobThreadClassName;
  }



  /**
   * Retrieves the time that this job should start.
   *
   * @return  The time that this job should start.
   */
  public Date getStartTime()
  {
    return startTime;
  }



  /**
   * Specifies the start time to use for the job.
   *
   * @param  startTime  The start time to use for the job.
   */
  public void setStartTime(final Date startTime)
  {
    this.startTime = startTime;
  }



  /**
   * Retrieves the time that this job should stop.
   *
   * @return  The time that this job should start.
   */
  public Date getStopTime()
  {
    return stopTime;
  }



  /**
   * Specifies the stop time to use for this job.
   *
   * @param  stopTime  The stop time to use for this job.
   */
  public void setStopTime(final Date stopTime)
  {
    this.stopTime = stopTime;
  }



  /**
   * Retrieves the maximum amount of time that this job should run.
   *
   * @return  The maximum amount of time that this job should run.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Specifies the duration to use for the job.
   *
   * @param  duration  The duration to use for the job.
   */
  public void setDuration(final int duration)
  {
    this.duration = duration;
  }



  /**
   * Retrieves the name of this job.
   *
   * @return  The name of this job.
   */
  public String getJobName()
  {
    return infoJobThread.getJobName();
  }



  /**
   * Retrieves a description of the job class.
   *
   * @return  A description of the job class.
   */
  public String getJobClassDescription()
  {
    return infoJobThread.getShortDescription();
  }



  /**
   * Retrieves the number of clients that should be used to run this job.
   *
   * @return  The number of clients that should be used to run this job.
   */
  public int getNumberOfClients()
  {
    return numClients;
  }



  /**
   * Specifies the number of clients that should be used to run this job.
   *
   * @param  numberOfClients  The number of clients that should be used to run
   *                          this job.
   */
  public void setNumberOfClients(final int numberOfClients)
  {
    this.numClients = numberOfClients;
  }



  /**
   * Retrieves the number of threads that each client should use to run this
   * job.
   *
   * @return  The number of threads that each client should use to run this job.
   */
  public int getThreadsPerClient()
  {
    return threadsPerClient;
  }



  /**
   * Specifies the number of threads that each client should use to run this
   * job.
   *
   * @param  threadsPerClient  The number of threads that each client should use
   *                           to run this job.
   */
  public void setThreadsPerClient(final int threadsPerClient)
  {
    this.threadsPerClient = threadsPerClient;
  }



  /**
   * Retrieves the delay in milliseconds that should be used when starting each
   * thread on the client system.
   *
   * @return  The delay in milliseconds that should be used when starting each
   *          thread on the client system.
   */
  public int getThreadStartupDelay()
  {
    return threadStartupDelay;
  }



  /**
   * Specifies the delay in milliseconds that should be used when starting each
   * thread on the client system.
   *
   * @param  threadStartupDelay  The delay in milliseconds that should be used
   *                             when starting each thread on the client system.
   */
  public void setThreadStartupDelay(final int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
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
   * Specifies the length of time in seconds that should be used as the
   * statistics collection interval.
   *
   * @param  collectionInterval  The length of time in seconds that should be
   *                             used as the statistics collection interval.
   */
  public void setCollectionInterval(final int collectionInterval)
  {
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves a list of all the parameters that can be used to configure the
   * behavior of this job.
   *
   * @return  A list of all the parameters that can be used to configure the
   *          behavior of this job.
   */
  public ParameterList getParameterStubs()
  {
    return infoJobThread.getParameterStubs().clone();
  }



  /**
   * Retrieves a list of all the parameters that can be used to configure the
   * behavior of this job.
   *
   * @return  A list of all the parameters that can be used to configure the
   *          behavior of this job.
   */
  public ParameterList getClientSideParameterStubs()
  {
    return infoJobThread.getClientSideParameterStubs().clone();
  }



  /**
   * Retrieves a list of the parameters that have been configured for this job.
   * This will not be the parameter stubs, but rather the actual parameters set
   * that the user has specified.
   *
   * @return  A list of the parameters that have been configured for this job.
   */
  public ParameterList getParameterList()
  {
    return parameters;
  }



  /**
   * Specifies the list of parameters to use for this job.
   *
   * @param  parameters  The list of parameters to use for this job.
   */
  public void setParameterList(final ParameterList parameters)
  {
    this.parameters = parameters;
  }



  /**
   * Retrieves the state of the current job.  The return value should be that of
   * one of the JOB_STATE_* constants.
   *
   * @return  The state of the current job.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Specifies the state of the current job.  The provided state value should be
   * one of the JOB_STATE_* constants.
   *
   * @param  jobState  The state to use for the current job.
   */
  public void setJobState(final int jobState)
  {
    this.jobState = jobState;
  }



  /**
   * Retrieves a string description of the current job state.
   *
   * @return  A string description of the current job state.
   */
  public String getJobStateString()
  {
    return Constants.jobStateToString(jobState);
  }



  /**
   * Indicates whether this job will wait for the appropriate number of
   * available clients to start running, or whether it will be cancelled if
   * there are not enough clients available when the start time arrives.
   *
   * @return  {@code true} if this job will wait for the requested number
   *          of clients before starting, or {@code false} if not.
   */
  public boolean waitForClients()
  {
    return waitForClients;
  }



  /**
   * Specifies whether this job will wait for the appropriate number of
   * available clients to start running, or whether it will be cancelled if
   * there are not enough clients available when the start time arrives.
   *
   * @param  waitForClients  Indicates whether the job will wait for clients to
   *                         be available.
   */
  public void setWaitForClients(final boolean waitForClients)
  {
    this.waitForClients = waitForClients;
  }



  /**
   * Indicates whether this job should be displayed in restricted read-only
   * mode.
   *
   * @return  {@code true} if this job should be displayed in restricted
   *          read-only mode, or {@code false} if not.
   */
  public boolean displayInReadOnlyMode()
  {
    return displayInReadOnlyMode;
  }



  /**
   * Specifies whether this job should be displayed in restricted read-only
   * mode.
   *
   * @param  displayInReadOnlyMode  Indicates whether this job should be
   *                                displayed in restricted read-only mode.
   */
  public void setDisplayInReadOnlyMode(final boolean displayInReadOnlyMode)
  {
    this.displayInReadOnlyMode = displayInReadOnlyMode;
  }



  /**
   * Retrieves the job IDs of the jobs that must complete before this job will
   * be eligible for processing.
   *
   * @return  The job IDs of the jobs that must complete before this job will be
   *          eligible for processing, or {@code null} if there are no such
   *          dependencies.
   */
  public String[] getDependencies()
  {
    if (dependencies.isEmpty())
    {
      return null;
    }

    final String[] dependencyArray = new String[dependencies.size()];
    dependencies.toArray(dependencyArray);
    return dependencyArray;
  }



  /**
   * Specifies the job IDs of the jobs that must complete before this job will
   * be eligible for processing.
   *
   * @param  dependencies  The job IDs of the jobs that must complete before
   *                       this job will be eligible for processing.
   */
  public void setDependencies(final String[] dependencies)
  {
    this.dependencies.clear();

    for (int i=0; ((dependencies != null) && (i < dependencies.length)); i++)
    {
      if ((dependencies[i] != null) && (dependencies[i].length() > 0))
      {
        this.dependencies.add(dependencies[i]);
      }
    }
  }



  /**
   * Removes the specified job as a dependency of this job.
   *
   * @param  jobID  The job ID for the job to remove as a dependency of this
   *                job.
   */
  public void removeDependency(final String jobID)
  {
    for (int i=0; i < dependencies.size(); i++)
    {
      final String dependency = dependencies.get(i);
      if (dependency.equalsIgnoreCase(jobID))
      {
        dependencies.remove(i);
        break;
      }
    }
  }



  /**
   * Retrieves the set of clients that have been requested to run this job.
   *
   * @return  The set of clients that have been requested to run this job.
   */
  public String[] getRequestedClients()
  {
    return requestedClients;
  }



  /**
   * Specifies the set of clients that have been requested to run this job.
   *
   * @param  requestedClients  The set of clients that have been requested to
   *                           run this job.
   */
  public void setRequestedClients(final String[] requestedClients)
  {
    this.requestedClients = requestedClients;
  }



  /**
   * Retrieves the set of resource monitor clients that have been requested for
   * this job.
   *
   * @return  The set of resource monitor clients that have been requested for
   *          this job.
   */
  public String[] getResourceMonitorClients()
  {
    return monitorClients;
  }



  /**
   * Specifies the set of resource monitor clients that should be used when
   * running this job.
   *
   * @param  monitorClients  The set of resource monitor clients that should be
   *                         used when running this job.
   */
  public void setResourceMonitorClients(final String[] monitorClients)
  {
    this.monitorClients = monitorClients;
  }



  /**
   * Indicates whether an attempt will be made to use resource monitor clients
   * on the same systems as the clients used to run this job.
   *
   * @return  {@code true} if resource monitor clients on the same systems
   *          as the clients will be used, or {@code false} if not.
   */
  public boolean monitorClientsIfAvailable()
  {
    return monitorClientsIfAvailable;
  }



  /**
   * Specifies whether an attempt should be made to use resource monitor clients
   * on the same systems as the clients used to run this job.
   *
   * @param  monitorClientsIfAvailable  Specifies whether an attempt should be
   *                                    made to use resource monitor clients
   *                                    on the same systems as the clients used
   *                                    to run this job.
   */
  public void setMonitorClientsIfAvailable(
                   final boolean monitorClientsIfAvailable)
  {
    this.monitorClientsIfAvailable = monitorClientsIfAvailable;
  }



  /**
   * Retrieves the e-mail addresses of the users that should be notified when
   * this job has completed running.
   *
   * @return  The e-mail addresses of the users that should be notified when
   *          this job has completed running.
   */
  public String[] getNotifyAddresses()
  {
    return notifyAddresses;
  }



  /**
   * Specifies the e-mail addresses of the users that should be notified when
   * this job has completed running.
   *
   * @param  notifyAddresses  The e-mail addresses of the users that should be
   *                          notified when this job has completed running.
   */
  public void setNotifyAddresses(final String[] notifyAddresses)
  {
    if (notifyAddresses == null)
    {
      this.notifyAddresses = new String[0];
    }
    else
    {
      this.notifyAddresses = notifyAddresses;
    }
  }



  /**
   * Indicates whether all possible processing has been done for this job.
   *
   * @return  {@code true} if there is no more processing to be done, or
   *          {@code false} if the job is still running or pending
   *          execution.
   */
  public boolean doneRunning()
  {
    return ((jobState != Constants.JOB_STATE_NOT_YET_STARTED) &&
            (jobState != Constants.JOB_STATE_DISABLED) &&
            (jobState != Constants.JOB_STATE_RUNNING));
  }



  /**
   * Indicates whether enough information is available for this job to generate
   * graphs of the results.
   *
   * @return  {@code true} if graphs are available, or {@code false}
   *          if not.
   */
  public boolean graphsAvailable()
  {
    for (final StatTracker tracker : statTrackers)
    {
      if (tracker.getNumIntervals() > 1)
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Indicates whether enough information is available for this job to generate
   * graphs of the resource monitor statistics.
   *
   * @return  {@code true} if graphs are available, or {@code false}
   *          if not.
   */
  public boolean resourceGraphsAvailable()
  {
    for (final ResourceMonitorStatTracker monitorTracker :
         resourceStatTrackers)
    {
      if (monitorTracker.getStatTracker().getNumIntervals() > 1)
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Indicates whether this job has real-time stat data associated with it.
   *
   * @return  {@code true} if this job has real-time stat data associated
   *          with it, or {@code false} if not.
   */
  public boolean hasRealTimeStats()
  {
    return (realTimeStats != null);
  }



  /**
   * Specifies the real-time stat data associated with this job.
   *
   * @param  realTimeStats  The real-time stat data associated with this job.
   */
  public void setRealTimeStats(final RealTimeJobStats realTimeStats)
  {
    this.realTimeStats = realTimeStats;
  }



  /**
   * Retrieves the real-time stat data associated with this job.
   *
   * @return  The real-time stat data associated with this job, or
   *          {@code null} if no real-time stat data is being collected.
   */
  public RealTimeJobStats getRealTimeStats()
  {
    return realTimeStats;
  }



  /**
   * Indicates whether this job has statistical information associated with it.
   *
   * @return  {@code true} if this job has statistical information
   *          associated with it, or {@code false} if not.
   */
  public boolean hasStats()
  {
    return (! statTrackers.isEmpty());
  }



  /**
   * Indicates whether this job has resource monitor information associated with
   * it.
   *
   * @return  {@code true} if this job has resource monitor information
   *          associated with it, or {@code false} if not.
   */
  public boolean hasResourceStats()
  {
    return (! resourceStatTrackers.isEmpty());
  }



  /**
   * Retrieves the display names of all the stat trackers associated with this
   * job.
   *
   * @return  The names of all stat trackers associated with this job.
   */
  public String[] getStatTrackerNames()
  {
    // Although we could get the list of names from the job thread, it is better
    // to see the names that we actually have, in case the job thread is not
    // giving us the full or correct list (e.g., in the case of scripted jobs).
    final ArrayList<String> trackerNames = new ArrayList<>();
    for (final StatTracker statTracker : statTrackers)
    {
      String name = statTracker.getDisplayName();
      boolean matchFound = false;
      for (final String trackerName : trackerNames)
      {
        if (name.equalsIgnoreCase(trackerName))
        {
          matchFound = true;
          break;
        }
      }

      if (! matchFound)
      {
        trackerNames.add(name);
      }
    }

    final String[] names = new String[trackerNames.size()];
    trackerNames.toArray(names);
    return names;
  }



  /**
   * Retrieves the display names of all the resource monitor stat trackers
   * associated with this job.
   *
   * @return  The names of all the resource monitor stat trackers associated
   *          with this job.
   */
  public String[] getResourceStatTrackerNames()
  {
    final ArrayList<String> trackerNames = new ArrayList<>();
    for (final ResourceMonitorStatTracker monitorTracker :
        resourceStatTrackers)
    {
      String name = monitorTracker.getStatTracker().getDisplayName();
      boolean matchFound = false;
      for (final String trackerName : trackerNames)
      {
        if (name.equalsIgnoreCase(trackerName))
        {
          matchFound = true;
          break;
        }
      }

      if (! matchFound)
      {
        trackerNames.add(name);
      }
    }

    final String[] names = new String[trackerNames.size()];
    trackerNames.toArray(names);
    return names;
  }



  /**
   * Retrieves the display names of all the resource monitor stat trackers
   * associated with this job, in a map that links those names to the names of
   * the associated resource monitor class.
   *
   * @return  The display names of all the resource monitor stat trackers
   *          associated with this job, in a map that links those names to the
   *          names of the associated resource monitor class.
   */
  public Map<String,String> getResourceStatTrackerNamesAndClasses()
  {
    final Map<String,String> trackerMap = new LinkedHashMap<>();
    for (final ResourceMonitorStatTracker monitorTracker :
         resourceStatTrackers)
    {
      final String displayName =
           monitorTracker.getStatTracker().getDisplayName();
      final String className   =
           monitorTracker.getResourceMonitor().getClass().getName();

      trackerMap.put(displayName, className);
    }

    return trackerMap;
  }



  /**
   * Retrieves the set of all stat trackers associated with this job.
   *
   * @return  The set of all stat trackers associated with this job.
   */
  public StatTracker[] getStatTrackers()
  {
    final StatTracker[] trackerArray = new StatTracker[statTrackers.size()];
    statTrackers.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all resource monitor stat trackers associated with
   * this job.
   *
   * @return  The set of all resource monitor stat trackers associated with this
   *          job.
   */
  public ResourceMonitorStatTracker[] getResourceMonitorStatTrackers()
  {
    final ResourceMonitorStatTracker[] trackerArray =
         new ResourceMonitorStatTracker[resourceStatTrackers.size()];
    resourceStatTrackers.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all stat trackers associated with this job that have
   * the specified display name.
   *
   * @param  displayName  The display name for the stat trackers to be
   *                      retrieved.
   *
   * @return  The set of all stat trackers associated with this job that have
   *          the specified display name.
   */
  public StatTracker[] getStatTrackers(String displayName)
  {
    final ArrayList<StatTracker> trackerList = new ArrayList<>();
    for (final StatTracker tracker : statTrackers)
    {
      if (displayName.equals(tracker.getDisplayName()))
      {
        trackerList.add(tracker);
      }
    }

    final StatTracker[] trackerArray = new StatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all resource monitor stat trackers associated with
   * this job that have the specified display name.
   *
   * @param  displayName  The display name for the resource stat trackers to be
   *                      retrieved.
   *
   * @return  The set of all resource stat trackers associated with this job
   *          that have the specified display name.
\   */
  public StatTracker[] getResourceStatTrackers(final String displayName)
  {
    final ArrayList<StatTracker> trackerList = new ArrayList<>();
    for (final ResourceMonitorStatTracker monitorTracker : resourceStatTrackers)
    {
      final StatTracker tracker = monitorTracker.getStatTracker();
      if (displayName.equals(tracker.getDisplayName()))
      {
        trackerList.add(tracker);
      }
    }

    final StatTracker[] trackerArray = new StatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all resource monitor stat trackers associated with
   * this job that have the specified display name.
   *
   * @param  displayName  The display name for the resource stat trackers to be
   *                      retrieved.
   *
   * @return  The set of all resource stat trackers associated with this job
   *          that have the specified display name.
   */
  public ResourceMonitorStatTracker[] getResourceMonitorStatTrackers(
                                           final String displayName)
  {
    final ArrayList<ResourceMonitorStatTracker> trackerList = new ArrayList<>();
    for (final ResourceMonitorStatTracker tracker : resourceStatTrackers)
    {
      if (displayName.equals(tracker.getStatTracker().getDisplayName()))
      {
        trackerList.add(tracker);
      }
    }

    final ResourceMonitorStatTracker[] trackerArray =
         new ResourceMonitorStatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of resource monitor class instances for which statistics
   * are available with this job.
   *
   * @return  The set of resource monitor class instances for which statistics
   *          are available with this job.
   */
  public ResourceMonitor[] getResourceMonitorClasses()
  {
    final Map<String,ResourceMonitor> classMap = new LinkedHashMap<>();
    for (final ResourceMonitorStatTracker tracker : resourceStatTrackers)
    {
      final ResourceMonitor monitor = tracker.getResourceMonitor();
      final String monitorClass = monitor.getClass().getName();
      if (! classMap.containsKey(monitorClass))
      {
        classMap.put(monitorClass, monitor);
      }
    }

    final ResourceMonitor[] monitors = new ResourceMonitor[classMap.size()];
    classMap.values().toArray(monitors);
    return monitors;
  }



  /**
   * Retrieves the set of all resource monitor stat trackers associated with
   * this job that were captured using the specified resource monitor class.
   *
   * @param  className  The fully-qualified name of the resource monitor class
   *                    for which to retrieve the associated resource monitor
   *                    stat trackers.
   *
   * @return  The set of all resource monitor stat trackers associated with this
   *          job that have the specified monitor class.
   */
  public ResourceMonitorStatTracker[] getResourceMonitorStatTrackersForClass(
                                           final String className)
  {
    final ArrayList<ResourceMonitorStatTracker> trackerList = new ArrayList<>();
    for (final ResourceMonitorStatTracker tracker : resourceStatTrackers)
    {
      if (tracker.getResourceMonitor().getClass().getName().equals(className))
      {
        trackerList.add(tracker);
      }
    }

    final ResourceMonitorStatTracker[] trackerArray =
         new ResourceMonitorStatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all stat trackers associated with this job that have
   * the specified display name and client ID.
   *
   * @param  displayName  The display name for the stat trackers to be
   *                      retrieved.
   * @param  clientID     The client ID for the stat trackers to be retrieved.
   *
   * @return  The set of all stat trackers associated with this job that have
   *          the specified display name and client ID.
   */
  public StatTracker[] getStatTrackers(final String displayName,
                                       final String clientID)
  {
    final ArrayList<StatTracker> trackerList = new ArrayList<>();
    for (final StatTracker tracker : statTrackers)
    {
      if (displayName.equals(tracker.getDisplayName()) &&
          clientID.equals(tracker.getClientID()))
      {
        trackerList.add(tracker);
      }
    }

    final StatTracker[] trackerArray = new StatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all resource monitor stat trackers associated with
   * this job that have the specified display name and client ID.
   *
   * @param  displayName  The display name for the stat trackers to be
   *                      retrieved.
   * @param  clientID     The client ID for the stat trackers to be retrieved.
   *
   * @return  The set of all resource monitor stat trackers associated with this
   *          job that have the specified display name and client ID.
   */
  public StatTracker[] getResourceStatTrackers(final String displayName,
                                               final String clientID)
  {
    final ArrayList<StatTracker> trackerList = new ArrayList<>();
    for (final ResourceMonitorStatTracker monitorTracker : resourceStatTrackers)
    {
      final StatTracker tracker = monitorTracker.getStatTracker();
      if (displayName.equals(tracker.getDisplayName()) &&
          clientID.equals(tracker.getClientID()))
      {
        trackerList.add(tracker);
      }
    }

    final StatTracker[] trackerArray = new StatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the set of all resource monitor stat trackers associated with
   * this job that have the specified display name and client ID.
   *
   * @param  displayName  The display name for the stat trackers to be
   *                      retrieved.
   * @param  clientID     The client ID for the stat trackers to be retrieved.
   *
   * @return  The set of all resource monitor stat trackers associated with this
   *          job that have the specified display name and client ID.
   */
  public ResourceMonitorStatTracker[] getResourceMonitorStatTrackers(
                                           final String displayName,
                                           final String clientID)
  {
    final ArrayList<ResourceMonitorStatTracker> trackerList = new ArrayList<>();
    for (final ResourceMonitorStatTracker monitorTracker : resourceStatTrackers)
    {
      final StatTracker tracker = monitorTracker.getStatTracker();
      if (displayName.equals(tracker.getDisplayName()) &&
          clientID.equals(tracker.getClientID()))
      {
        trackerList.add(monitorTracker);
      }
    }

    final ResourceMonitorStatTracker[] trackerArray =
         new ResourceMonitorStatTracker[trackerList.size()];
    trackerList.toArray(trackerArray);
    return trackerArray;
  }



  /**
   * Retrieves the client IDs of all the clients that contributed stat tracker
   * information.
   *
   * @return  The client IDs of all the clients that contributed stat tracker
   *          information.
   */
  public String[] getStatTrackerClientIDs()
  {
    final ArrayList<String> idList = new ArrayList<>();
    for (final StatTracker tracker : statTrackers)
    {
      boolean match = false;
      for (int j=0; j < idList.size(); j++)
      {
        if (tracker.getClientID().equals(idList.get(j)))
        {
          match = true;
          break;
        }
      }
      if (! match)
      {
        idList.add(tracker.getClientID());
      }
    }

    final String[] clientIDs = new String[idList.size()];
    idList.toArray(clientIDs);
    return clientIDs;
  }



  /**
   * Retrieves the client IDs of all the clients that contributed resource
   * monitor information.
   *
   * @return  The client IDs of all the clients that contributed resource
   *          monitor information.
   */
  public String[] getResourceStatTrackerClientIDs()
  {
    final ArrayList<String> idList = new ArrayList<>();
    for (final ResourceMonitorStatTracker monitorTracker : resourceStatTrackers)
    {
      final StatTracker tracker = monitorTracker.getStatTracker();
      boolean match = false;
      for (int j=0; j < idList.size(); j++)
      {
        if (tracker.getClientID().equals(idList.get(j)))
        {
          match = true;
          break;
        }
      }
      if (! match)
      {
        idList.add(tracker.getClientID());
      }
    }

    final String[] clientIDs = new String[idList.size()];
    idList.toArray(clientIDs);
    return clientIDs;
  }



  /**
   * Specifies the set of stat trackers associated with this job.
   *
   * @param  trackerArray  The set of stat trackers to assign to this job.
   */
  public void setStatTrackers(final StatTracker[] trackerArray)
  {
    statTrackers = new ArrayList<>(trackerArray.length);
    statTrackers.addAll(Arrays.asList(trackerArray));
  }



  /**
   * Specifies the set of resource monitor stat trackers associated with this
   * job.
   *
   * @param  trackerArray  The set of resource monitor stat trackers to assign
   *                       to this job.
   */
  public void setResourceStatTrackers(final StatTracker[] trackerArray)
  {
    resourceStatTrackers = new ArrayList<>(trackerArray.length);
    for (final StatTracker tracker : trackerArray)
    {
      ResourceMonitorStatTracker monitorTracker =
           new ResourceMonitorStatTracker(
                new LegacyResourceMonitor(), tracker);
      resourceStatTrackers.add(monitorTracker);
    }
  }



  /**
   * Specifies the set of resource monitor stat trackers associated with this
   * job.
   *
   * @param  monitorTrackers  The set of resource monitor stat trackers to
   *                          assign to this job.
   */
  public void setResourceStatTrackers(
                   final ResourceMonitorStatTracker[] monitorTrackers)
  {
    resourceStatTrackers = new ArrayList<>(monitorTrackers.length);
    resourceStatTrackers.addAll(Arrays.asList(monitorTrackers));
  }



  /**
   * Retrieves the set of messages logged during the job's execution.
   *
   * @return  The set of messages logged during the job's execution.
   */
  public String[] getLogMessages()
  {
    final String[] messagesArray = new String[logMessages.size()];
    return logMessages.toArray(messagesArray);
  }



  /**
   * Adds the specified message to the set of messages logged during this job's
   * execution.
   *
   * @param  message  The message to add to the set of messages for this job.
   */
  public void addLogMessage(final String message)
  {
    logMessages.add(message);
  }



  /**
   * Specifies the set of messages logged during the job's execution.
   *
   * @param  logMessages  The set of messages logged during the job's execution.
   */
  public void setLogMessages(final String[] logMessages)
  {
    ArrayList<String> messageList = new ArrayList<>(logMessages.length);
    messageList.addAll(Arrays.asList(logMessages));
    this.logMessages = messageList;
  }



  /**
   * Specifies the set of messages logged during the job's execution.
   *
   * @param  logMessages  An array list containing the set of messages logged
   *                      during the job's execution.
   */
  public void setLogMessages(final List<String> logMessages)
  {
    this.logMessages = new ArrayList<>(logMessages);
  }



  /**
   * Retrieves the time at which this job actually started running.
   *
   * @return  The time at which this job actually started running.
   */
  public Date getActualStartTime()
  {
    return actualStartTime;
  }



  /**
   * Specifies the time that this job actually started running.
   *
   * @param  actualStartTime  The time that this job actually started running.
   */
  public void setActualStartTime(final Date actualStartTime)
  {
    this.actualStartTime = actualStartTime;
  }



  /**
   * Specifies the time that the job actually started running.
   *
   * @param  actualStartTime  The time that the job actually started running.
   */
  public void setActualStartTime(final long actualStartTime)
  {
    this.actualStartTime = new Date(actualStartTime);
  }



  /**
   * Retrieves the time that the job actually stopped running.
   *
   * @return  The time that the job actually stopped running.
   */
  public Date getActualStopTime()
  {
    return actualStopTime;
  }



  /**
   * Specifies the time that the job actually stopped running.
   *
   * @param  actualStopTime  The time that the job actually stopped running.
   */
  public void setActualStopTime(final Date actualStopTime)
  {
    this.actualStopTime = actualStopTime;
  }



  /**
   * Specifies the time that the job actually stopped running.
   *
   * @param  actualStopTime  The time that the job actually stopped running.
   */
  public void setActualStopTime(final long actualStopTime)
  {
    this.actualStopTime = new Date(actualStopTime);
  }


  /**
   * Retrieves the length of time in seconds that the job was actually running.
   *
   * @return  The length of time in seconds that the job was actually running.
   */
  public int getActualDuration()
  {
    return actualDuration;
  }



  /**
   * Specifies the length of time in seconds that the job was actually running.
   *
   * @param  actualDuration  The length of time in seconds that the job was
   *                         actually running.
   */
  public void setActualDuration(final int actualDuration)
  {
    this.actualDuration = actualDuration;
  }



  /**
   * Indicates that the job should start running on all the specified clients
   * with the appropriate number of threads per client.
   *
   * @throws  UnableToRunException  If the job is not in a state in which it can
   *                                be started.
   */
  public void startProcessing()
         throws UnableToRunException
  {
    // The job state must indicate that the job has not yet been started.  Any
    // other job state will exit with an error
    if (! ((jobState == Constants.JOB_STATE_NOT_YET_STARTED) ||
           (jobState == Constants.JOB_STATE_RUNNING)))
    {
      final String message = "The job cannot be run in its current state (" +
           Constants.jobStateToString(jobState) + ')';
      throw new UnableToRunException(message);
    }


    // Technically it's not running yet, but update the state information to
    // say that it is.  In the event that some critical error occurs while
    // starting the job, this will prevent the possibility that a change in the
    // job state is overwritten by this "running" indicator.
    jobState = Constants.JOB_STATE_RUNNING;


    // Get the appropriate number of client connections.  Make sure that it is
    // not null.  There probably should be a check to make sure we actually got
    // the right number of connections.
    clientConnections =
         slamdServer.getClientListener().getClientConnections(this);
    if (clientConnections == null)
    {
      throw new UnableToRunException(
           "Not enough clients available to run the job");
    }


    // Get the appropriate set of resource monitor client connections.
    if (monitorClientsIfAvailable ||
        ((monitorClients != null) && (monitorClients.length > 0)))
    {
      monitorConnections = slamdServer.getMonitorClientListener().
                                getMonitorClientConnections(this);
      if (monitorConnections == null)
      {
        throw new UnableToRunException("At least one in the requested set of " +
             "resource monitor clients is not available.");
      }
    }


    // Perform the job-level initialization, if any has been specified.
    try
    {
      infoJobThread.initializeJob(parameters);
    }
    catch (final UnableToRunException utre)
    {
      // This job won't be executed after all, so iterate through each
      // connection and make it available for processing again.
      for (final ClientConnection clientConnection : clientConnections)
      {
        slamdServer.getClientListener().setAvailableForProcessing(
             clientConnection);
      }

      throw utre;
    }


    // For each monitor client, send it a job request message and tell it to
    // start processing
    for (int i=0;
         ((monitorConnections != null) && (i < monitorConnections.length)); i++)
    {
      final JobResponseMessage jobResponse =
           monitorConnections[i].sendJobRequest(this, i);
      if (jobResponse.getResponseCode() ==
          Constants.MESSAGE_RESPONSE_SUCCESS)
      {
        // Add this monitor client to the list of active monitor clients.
        synchronized (activeClientMutex)
        {
          activeMonitorClients.add(monitorConnections[i]);
        }

        final JobControlResponseMessage startResponse =
             monitorConnections[i].sendJobControlRequest(this,
                  Constants.JOB_CONTROL_TYPE_START);
        if (startResponse.getResponseCode() !=
            Constants.MESSAGE_RESPONSE_SUCCESS)
        {
          logMessages.add("Unable to start resource monitoring on client " +
               monitorConnections[i].getConnectionID() + ":  " +
               startResponse.getResponseCode() + " (" +
               startResponse.getResponseMessage() + ')');
          synchronized (activeClientMutex)
          {
            removeMonitorClient(monitorConnections[i]);
          }
        }
      }
    }


    // For each client retrieved, send it a job request message and tell it to
    // start processing
    for (int i=0; i < clientConnections.length; i++)
    {
      final JobResponseMessage jobResponse =
           clientConnections[i].sendJobRequest(this, i);
      if (jobResponse.getResponseCode() ==
          Constants.MESSAGE_RESPONSE_SUCCESS)
      {
        // Add this client to the list of active clients
        synchronized (activeClientMutex)
        {
          activeClients.add(clientConnections[i]);
        }

        final JobControlResponseMessage startResponse =
             clientConnections[i].sendJobControlRequest(this,
                                       Constants.JOB_CONTROL_TYPE_START);
        final String errMsg = startResponse.getResponseMessage();
        switch (startResponse.getResponseCode())
        {
          case Constants.MESSAGE_RESPONSE_CLASS_NOT_FOUND:
            jobState = Constants.JOB_STATE_NO_SUCH_JOB;
            logMessages.add("Unable to start job on client " +
                 clientConnections[i].getClientID() + ":  No such job.");
            synchronized (activeClientMutex)
            {
              removeActiveClient(clientConnections[i]);
              slamdServer.getClientListener().setAvailableForProcessing(
                   clientConnections[i]);
            }
            break;
          case Constants.MESSAGE_RESPONSE_CLASS_NOT_VALID:
            jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
            logMessages.add("Unable to start job on client " +
                 clientConnections[i].getClientID() +
                 ":  Job class not valid.");
            synchronized (activeClientMutex)
            {
              removeActiveClient(clientConnections[i]);
              slamdServer.getClientListener().setAvailableForProcessing(
                   clientConnections[i]);
            }
            break;
          case Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE:
            logMessages.add("Unable to start job on client " +
                 clientConnections[i].getClientID() +
                 ":  Job creation failure.");
            if ((errMsg != null) && (errMsg.length() > 0))
            {
              logMessages.add(errMsg);
            }
            jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
            synchronized (activeClientMutex)
            {
              removeActiveClient(clientConnections[i]);
              slamdServer.getClientListener().setAvailableForProcessing(
                   clientConnections[i]);
            }
            break;
          case Constants.MESSAGE_RESPONSE_LOCAL_ERROR:
            logMessages.add("Unable to start job on client " +
                 clientConnections[i].getClientID() + ":  Client local error.");
            if ((errMsg != null) && (errMsg.length() > 0))
            {
              logMessages.add(errMsg);
            }
            jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
            synchronized (activeClientMutex)
            {
              removeActiveClient(clientConnections[i]);
              slamdServer.getClientListener().setAvailableForProcessing(
                   clientConnections[i]);
            }
            break;
          case Constants.MESSAGE_RESPONSE_NO_SUCH_JOB:
            logMessages.add("Unable to start job on client " +
                 clientConnections[i].getClientID() +
                 ":  Client does not know about job.");
            jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
            synchronized (activeClientMutex)
            {
              removeActiveClient(clientConnections[i]);
              slamdServer.getClientListener().setAvailableForProcessing(
                   clientConnections[i]);
            }
            break;
        }
      }
    }

    // If there are no active clients, then the job could not be started.
    if (activeClients.isEmpty())
    {
      // If there are active monitor connections, they should be stopped.
      synchronized (activeClientMutex)
      {
        for (final ResourceMonitorClientConnection conn : activeMonitorClients)
        {
          conn.sendJobControlRequest(this, Constants.JOB_CONTROL_TYPE_STOP);
        }
        activeMonitorClients.clear();
      }


      jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      throw new UnableToRunException(
           "None of the clients were able to start the job.");
    }
  }



  /**
   * Retrieves the set of client connections that have been selected to use to
   * run this job.  Note that this will only be available while the job is
   * actually running and for a very brief period of time before it starts, so
   * this method should definitely not be considered available for general use.
   *
   * @return  The set of client connections that have been selected to use to
   *          run this job.
   */
  public ClientConnection[] getClientConnections()
  {
    return clientConnections;
  }



  /**
   * Retrieves the set of clients that are currently being used to run this job.
   * The contents of this list will change as clients are started and stopped.
   * It should only be used internally while the job is running.
   *
   * @return  The set of clients that are currently being used to run this job.
   */
  public List<ClientConnection> getActiveClients()
  {
    return activeClients;
  }



  /**
   * Retrieves the set of resource monitor clients that are currently being used
   * in conjunction with this job.  The contents of this list will change as
   * resource monitor clients start and stop collecting data for this job.
   *
   * @return  The set of resource monitor clients that are currently being used
   *          to run this job.
   */
  public List<ResourceMonitorClientConnection> getActiveMonitorClients()
  {
    return activeMonitorClients;
  }



  /**
   * Removes information about the specified client connection from the list of
   * active connections associated with this job.  This will be called once the
   * job has completed, or if it is unable to start for some reason.
   *
   * @param  clientConnection  The client connection that should be removed from
   *                           the list of active connections.
   */
  public void removeActiveClient(final ClientConnection clientConnection)
  {
    for (int i=0; i < activeClients.size(); i++)
    {
      final ClientConnection conn = activeClients.get(i);
      if (conn.getClientID().equals(clientConnection.getClientID()))
      {
        activeClients.remove(i);
        break;
      }
    }
  }



  /**
   * Removes information about the specified resource monitor client connection
   * from the list of active connections associated with this job.  This will be
   * called once the job has completed, or if it is unable to start for some
   * reason.
   *
   * @param  clientConnection  The resource monitor client connection that
   *                           should be removed from the list of active
   *                           connections.
   */
  public void removeMonitorClient(
                   final ResourceMonitorClientConnection clientConnection)
  {
    for (int i=0; i < activeMonitorClients.size(); i++)
    {
      final ResourceMonitorClientConnection conn = activeMonitorClients.get(i);
      if (conn.getConnectionID().equals(clientConnection.getConnectionID()))
      {
        activeMonitorClients.remove(i);
        break;
      }
    }
  }



  /**
   * Indicates that the specified client has completed its processing for this
   * job.
   *
   * @param  clientConnection     The client connection that has completed
   *                              processing for this job.
   * @param  jobCompletedMessage  The job completed message for the client that
   *                              contains all the information we need to track.
   */
  public void clientDone(final ClientConnection clientConnection,
                         final JobCompletedMessage jobCompletedMessage)
  {
    boolean jobDone = false;

    synchronized (activeClientMutex)
    {
      // First, remove the connection from the list of active connections
      removeActiveClient(clientConnection);


      // See if we need to update the tentative job state.  It will be updated
      // as long as it currently indicates that the job has completed
      // successfully.
      if (tentativeJobState == Constants.JOB_STATE_COMPLETED_SUCCESSFULLY)
      {
        tentativeJobState = jobCompletedMessage.getJobState();
      }


      // Next, update the status counter information for this job.
      final StatTracker[] clientTrackers =
           jobCompletedMessage.getStatTrackers();
      for (int i=0; i < clientTrackers.length; i++)
      {
        statTrackers.add(clientTrackers[i]);
      }


      // Update the timing information for this job.  Pick the earliest start
      // time as the actual start time, the latest stop time as the actual
      // stop time, and the longest duration as the actual duration.  Note that
      // differences in remote clocks may skew start and end time information,
      // so duration may be the only reliable statistic unless time
      // synchronization is used.
      if ((actualStartTime == null) ||
          (jobCompletedMessage.getActualStartTime().compareTo(
               actualStartTime) < 0))
      {
        actualStartTime = jobCompletedMessage.getActualStartTime();
      }

      if ((actualStopTime == null) ||
          (jobCompletedMessage.getActualStopTime().compareTo(
               actualStopTime) > 0))
      {
        actualStopTime = jobCompletedMessage.getActualStopTime();
      }

      if ((actualDuration < 0) ||
          (jobCompletedMessage.getActualDuration() > actualDuration))
      {
        actualDuration = jobCompletedMessage.getActualDuration();
      }


      // Log any appropriate information from the job completed message and
      // store them the job's log message list.
      final String[] clientLogMessages = jobCompletedMessage.getLogMessages();
      for (final String clientLogMessage : clientLogMessages)
      {
        slamdServer.logWithoutFormatting(Constants.LOG_LEVEL_JOB_PROCESSING,
             clientLogMessage);
        logMessages.add(clientLogMessage);
      }


      // If this was the last active connection, then check to see if any
      // resource monitor clients are still active.  If so, then indicate that
      // they should stop.  If not, then do all the appropriate finalization.
      if (activeClients.isEmpty())
      {
        if (activeMonitorClients.isEmpty())
        {
          jobDone = true;
        }
        else
        {
          for (final ResourceMonitorClientConnection client :
               activeMonitorClients)
          {
            client.sendJobControlRequest(this, Constants.JOB_CONTROL_TYPE_STOP);
          }
        }
      }
    }

    if (jobDone)
    {
      // If this was the last active connection, then run the per-job
      // finalization, update the job info to indicate that the job is done, and
      // update the scheduler.
      infoJobThread.finalizeJob();
      this.jobState = tentativeJobState;
      slamdServer.getScheduler().jobDone(this);
    }
  }



  /**
   * Indicates that the specified resource monitor client has completed its
   * processing and that its resource statistics are available.
   *
   * @param  clientConnection  The resource monitor client connection that has
   *                           completed its processing for this job.
   * @param  message           The job completed message associated sent by the
   *                           resource monitor client.
   */
  public void resourceClientDone(
                   final ResourceMonitorClientConnection clientConnection,
                   final JobCompletedMessage message)
  {
    boolean jobDone = false;

    synchronized (activeClientMutex)
    {
      // First, remove the connection from the list of active connections
      removeMonitorClient(clientConnection);


      // See if we need to update the tentative job state.  It will be updated
      // as long as it currently indicates that the job has completed
      // successfully.
      if (tentativeJobState == Constants.JOB_STATE_COMPLETED_SUCCESSFULLY)
      {
        tentativeJobState = message.getJobState();
      }


      // Next, update the status counter information for this job.
      final StatTracker[] monitorTrackers = message.getStatTrackers();
      for (final StatTracker tracker : monitorTrackers)
      {
        // FIXME -- Correct this when the new protocol is in place.
        ResourceMonitorStatTracker monitorTracker =
             new ResourceMonitorStatTracker(
                  new LegacyResourceMonitor(), tracker);
        resourceStatTrackers.add(monitorTracker);
      }


      // Update the timing information for this job.  Pick the earliest start
      // time as the actual start time, the latest stop time as the actual
      // stop time, and the longest duration as the actual duration.  Note that
      // differences in remote clocks may skew start and end time information,
      // so duration may be the only reliable statistic unless time
      // synchronization is used.
      if ((actualStartTime == null) ||
          (message.getActualStartTime().compareTo(actualStartTime) < 0))
      {
        actualStartTime = message.getActualStartTime();
      }

      if ((actualStopTime == null) ||
          (message.getActualStopTime().compareTo(actualStopTime) > 0))
      {
        actualStopTime = message.getActualStopTime();
      }

      if ((actualDuration < 0) ||
          (message.getActualDuration() > actualDuration))
      {
        actualDuration = message.getActualDuration();
      }


      // Log any appropriate information from the job completed message and
      // store them the job's log message list.
      final String[] monitorLogMessages = message.getLogMessages();
      for (final String monitorLogMessage : monitorLogMessages)
      {
        slamdServer.logWithoutFormatting(Constants.LOG_LEVEL_JOB_PROCESSING,
             monitorLogMessage);
        logMessages.add(monitorLogMessage);
      }


      // If this was the last active connection, then set a flag so that we can
      // know the job was done.  Note that we can't actually run the
      // finalization here because it is possible (although remote) that a
      // deadlock could occur if we're holding the client mutex and trying to
      // get the scheduler lock, while the scheduler holds its own lock and
      // tries to get the client mutex.
      if (activeClients.isEmpty() && activeMonitorClients.isEmpty())
      {
        jobDone = true;
      }
    }

    if (jobDone)
    {
      // If this was the last active connection, then run the per-job
      // finalization, update the job info to indicate that the job is done, and
      // update the scheduler.
      infoJobThread.finalizeJob();
      this.jobState = tentativeJobState;
      slamdServer.getScheduler().jobDone(this);
    }
  }



  /**
   * Indicates that all job threads should stop running on all clients.
   */
  public void stopProcessing()
  {
    synchronized (activeClientMutex)
    {
      for (final ClientConnection client : activeClients)
      {
        client.sendJobControlRequest(this, Constants.JOB_CONTROL_TYPE_STOP);
      }

      for (final ResourceMonitorClientConnection client : activeMonitorClients)
      {
        client.sendJobControlRequest(this, Constants.JOB_CONTROL_TYPE_STOP);
      }
    }
  }



  /**
   * Indicates that all job threads should stop running on all clients, and will
   * not return until that has occurred.
   */
  public void stopAndWait()
  {
    synchronized (activeClientMutex)
    {
      for (final ClientConnection client : activeClients)
      {
        client.sendJobControlRequest(this,
             Constants.JOB_CONTROL_TYPE_STOP_AND_WAIT);
      }

      for (final ResourceMonitorClientConnection client : activeMonitorClients)
      {
        client.sendJobControlRequest(this,
             Constants.JOB_CONTROL_TYPE_STOP_AND_WAIT);
      }
    }
  }



  /**
   * Encodes this job to a byte array suitable for storage in the database.
   *
   * @return  The job encoded as a byte array.
   */
  public byte[] encode()
  {
    final ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();
    final SimpleDateFormat dateFormat  =
         new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

    elementList.add(new ASN1OctetString(ELEMENT_JOB_ID));
    elementList.add(new ASN1OctetString(jobID));
    elementList.add(new ASN1OctetString(ELEMENT_JOB_CLASS));
    elementList.add(new ASN1OctetString(jobThreadClassName));

    if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_OPTIMIZING_JOB_ID));
      elementList.add(new ASN1OctetString(optimizingJobID));
    }

    if ((jobGroup != null) && (jobGroup.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_JOB_GROUP));
      elementList.add(new ASN1OctetString(jobGroup));
    }

    if ((folderName != null) && (folderName.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_FOLDER));
      elementList.add(new ASN1OctetString(folderName));
    }

    elementList.add(new ASN1OctetString(ELEMENT_JOB_STATE));
    elementList.add(new ASN1Integer(jobState));
    elementList.add(new ASN1OctetString(ELEMENT_DISPLAY_IN_READ_ONLY));
    elementList.add(new ASN1Boolean(displayInReadOnlyMode));

    if ((jobDescription != null) && (jobDescription.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_DESCRIPTION));
      elementList.add(new ASN1OctetString(jobDescription));
    }

    elementList.add(new ASN1OctetString(ELEMENT_START_TIME));
    elementList.add(new ASN1OctetString(dateFormat.format(startTime)));

    if (stopTime != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_STOP_TIME));
      elementList.add(new ASN1OctetString(dateFormat.format(stopTime)));
    }

    if (duration > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_DURATION));
      elementList.add(new ASN1Integer(duration));
    }

    elementList.add(new ASN1OctetString(ELEMENT_NUM_CLIENTS));
    elementList.add(new ASN1Integer(numClients));

    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      final ASN1Element[] clientElements =
           new ASN1Element[requestedClients.length];
      for (int i=0; i < requestedClients.length; i++)
      {
        clientElements[i] = new ASN1OctetString(requestedClients[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_REQUESTED_CLIENTS));
      elementList.add(new ASN1Sequence(clientElements));
    }

    if ((monitorClients != null) && (monitorClients.length > 0))
    {
      final ASN1Element[] clientElements =
           new ASN1Element[monitorClients.length];
      for (int i=0; i < monitorClients.length; i++)
      {
        clientElements[i] = new ASN1OctetString(monitorClients[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_MONITOR_CLIENTS));
      elementList.add(new ASN1Sequence(clientElements));
    }

    elementList.add(new ASN1OctetString(ELEMENT_MONITOR_CLIENTS_IF_AVAILABLE));
    elementList.add(new ASN1Boolean(monitorClientsIfAvailable));
    elementList.add(new ASN1OctetString(ELEMENT_WAIT_FOR_CLIENTS));
    elementList.add(new ASN1Boolean(waitForClients));
    elementList.add(new ASN1OctetString(ELEMENT_THREADS_PER_CLIENT));
    elementList.add(new ASN1Integer(threadsPerClient));
    elementList.add(new ASN1OctetString(ELEMENT_THREAD_STARTUP_DELAY));
    elementList.add(new ASN1Integer(threadStartupDelay));

    if ((dependencies != null) && (! dependencies.isEmpty()))
    {
      final ASN1Element[] dependencyElements =
           new ASN1Element[dependencies.size()];
      for (int i=0; i < dependencyElements.length; i++)
      {
        dependencyElements[i] = new ASN1OctetString(dependencies.get(i));
      }

      elementList.add(new ASN1OctetString(ELEMENT_DEPENDENCIES));
      elementList.add(new ASN1Sequence(dependencyElements));
    }

    if ((notifyAddresses != null) && (notifyAddresses.length > 0))
    {
      final ASN1Element[] addrElements =
           new ASN1Element[notifyAddresses.length];
      for (int i=0; i < notifyAddresses.length; i++)
      {
        addrElements[i] = new ASN1OctetString(notifyAddresses[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_NOTIFY_ADDRESSES));
      elementList.add(new ASN1Sequence(addrElements));
    }

    elementList.add(new ASN1OctetString(ELEMENT_COLLECTION_INTERVAL));
    elementList.add(new ASN1Integer(collectionInterval));

    if ((jobComments != null) && (jobComments.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_COMMENTS));
      elementList.add(new ASN1OctetString(jobComments));
    }

    final Parameter[] params = parameters.getParameters();
    final ArrayList<ASN1Element> paramList = new ArrayList<>();
    for (int i=0; i < params.length; i++)
    {
      if ((params[i] instanceof PlaceholderParameter) ||
          (params[i] instanceof LabelParameter))
      {
        continue;
      }

      ASN1Element[] paramElements = new ASN1Element[]
      {
        new ASN1OctetString(params[i].getName()),
        new ASN1OctetString(params[i].getValueString())
      };

      paramList.add(new ASN1Sequence(paramElements));
    }
    final ASN1Element[] paramsElements = new ASN1Element[paramList.size()];
    paramList.toArray(paramsElements);
    elementList.add(new ASN1OctetString(ELEMENT_PARAMETERS));
    elementList.add(new ASN1Sequence(paramsElements));

    if (actualStartTime != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_ACTUAL_START_TIME));
      elementList.add(new ASN1OctetString(dateFormat.format(actualStartTime)));
    }

    if (actualStopTime != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_ACTUAL_STOP_TIME));
      elementList.add(new ASN1OctetString(dateFormat.format(actualStopTime)));
    }

    if (actualDuration >= 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_ACTUAL_DURATION));
      elementList.add(new ASN1Integer(actualDuration));
    }

    if ((statTrackers != null) && (! statTrackers.isEmpty()))
    {
      final StatTracker[] trackers = new StatTracker[statTrackers.size()];
      statTrackers.toArray(trackers);
      elementList.add(new ASN1OctetString(ELEMENT_STATS));
      elementList.add(StatEncoder.trackersToSequence(trackers));
    }

    if ((resourceStatTrackers != null) && (! resourceStatTrackers.isEmpty()))
    {
      elementList.add(new ASN1OctetString(ELEMENT_RESOURCE_MONITOR_STATS));
      elementList.add(
           ResourceMonitorStatTracker.trackersToSequence(resourceStatTrackers));
    }

    if ((logMessages != null) && (! logMessages.isEmpty()))
    {
      final ASN1Element[] messageElements = new ASN1Element[logMessages.size()];
      for (int i=0; i < messageElements.length; i++)
      {
        messageElements[i] = new ASN1OctetString(logMessages.get(i));
      }

      elementList.add(new ASN1OctetString(ELEMENT_LOG_MESSAGES));
      elementList.add(new ASN1Sequence(messageElements));
    }


    final ASN1Element[] elements = new ASN1Element[elementList.size()];
    elementList.toArray(elements);
    return new ASN1Sequence(elements).encode();
  }



  /**
   * Decodes the provided byte array as a SLAMD job.
   *
   * @param  slamdServer  The SLAMD server instance with which this job is to be
   *                      associated.
   * @param  encodedJob   The byte array containing the encoded job data.
   *
   * @return  The job decoded from the provided byte array.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           job data.
   */
  public static Job decode(final SLAMDServer slamdServer,
                           final byte[] encodedJob)
         throws DecodeException
  {
    try
    {
      boolean       displayInReadOnlyMode     = false;
      boolean       monitorClientsIfAvailable = false;
      boolean       waitForClients            = true;
      Date          actualStartTime           = null;
      Date          actualStopTime            = null;
      Date          startTime                 = null;
      Date          stopTime                  = null;
      int           actualDuration            = -1;
      int           collectionInterval        = -1;
      int           duration                  = -1;
      int           jobState                  = -1;
      int           numClients                = -1;
      int           threadsPerClient          = -1;
      int           threadStartupDelay        = 0;
      ParameterList parameters                = new ParameterList();
      StatTracker[] statTrackers              = new StatTracker[0];
      ResourceMonitorStatTracker[] monitorStatTrackers =
           new ResourceMonitorStatTracker[0];
      String        comments                  = null;
      String        folderName                = null;
      String        jobClassName              = null;
      String        jobDescription            = null;
      String        jobGroup                  = null;
      String        jobID                     = null;
      String        optimizingJobID           = null;
      String[]      dependencies              = new String[0];
      String[]      logMessages               = new String[0];
      String[]      monitorClients            = new String[0];
      String[]      notifyAddresses           = new String[0];
      String[]      requestedClients          = new String[0];

      final SimpleDateFormat dateFormat  =
           new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

      final ASN1Element element  = ASN1Element.decode(encodedJob);
      final ASN1Element[] elements = element.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        final String elementName =
             elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_JOB_ID))
        {
          jobID = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          jobClassName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_OPTIMIZING_JOB_ID))
        {
          optimizingJobID =
               elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_GROUP))
        {
          jobGroup = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_FOLDER))
        {
          folderName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_STATE))
        {
          jobState = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_DISPLAY_IN_READ_ONLY))
        {
          displayInReadOnlyMode =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          jobDescription = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_START_TIME))
        {
          final String timeStr =
               elements[i+1].decodeAsOctetString().getStringValue();
          startTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_STOP_TIME))
        {
          final String timeStr =
               elements[i+1].decodeAsOctetString().getStringValue();
          stopTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_DURATION))
        {
          duration = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_NUM_CLIENTS))
        {
          numClients = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_REQUESTED_CLIENTS))
        {
          final ASN1Element[] clientElements =
               elements[i+1].decodeAsSequence().getElements();
          requestedClients = new String[clientElements.length];
          for (int j=0; j < requestedClients.length; j++)
          {
            requestedClients[j] =
                 clientElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_MONITOR_CLIENTS))
        {
          final ASN1Element[] clientElements =
               elements[i+1].decodeAsSequence().getElements();
          monitorClients = new String[clientElements.length];
          for (int j=0; j < monitorClients.length; j++)
          {
            monitorClients[j] =
                 clientElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_MONITOR_CLIENTS_IF_AVAILABLE))
        {
          monitorClientsIfAvailable =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_WAIT_FOR_CLIENTS))
        {
          waitForClients = elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_THREADS_PER_CLIENT))
        {
          threadsPerClient = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_THREAD_STARTUP_DELAY))
        {
          threadStartupDelay = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_DEPENDENCIES))
        {
          final ASN1Element[] dependencyElements =
               elements[i+1].decodeAsSequence().getElements();
          dependencies = new String[dependencyElements.length];
          for (int j=0; j < dependencies.length; j++)
          {
            dependencies[j] =
                 dependencyElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_NOTIFY_ADDRESSES))
        {
          final ASN1Element[] addrElements =
               elements[i+1].decodeAsSequence().getElements();
          notifyAddresses = new String[addrElements.length];
          for (int j=0; j < notifyAddresses.length; j++)
          {
            notifyAddresses[j] =
                 addrElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_COLLECTION_INTERVAL))
        {
          collectionInterval = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_COMMENTS))
        {
          comments = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_PARAMETERS))
        {
          if (jobClassName == null)
          {
            for (int j=i+2; j < elements.length; j += 2)
            {
              final String name =
                   elements[j].decodeAsOctetString().getStringValue();
              if (name.equals(ELEMENT_JOB_CLASS))
              {
                jobClassName =
                     elements[j+1].decodeAsOctetString().getStringValue();
                break;
              }
            }
          }

          try
          {
            final Class<?> jobClass = Constants.classForName(jobClassName);
            final JobClass stubInstance = (JobClass) jobClass.newInstance();
            parameters = stubInstance.getClientSideParameterStubs().clone();

            final ASN1Element[] paramsElements =
                 elements[i+1].decodeAsSequence().getElements();
            for (int j=0; j < paramsElements.length; j++)
            {
              final ASN1Element[] paramElements =
                   paramsElements[j].decodeAsSequence().getElements();
              final String name =
                   paramElements[0].decodeAsOctetString().getStringValue();
              final String value =
                   paramElements[1].decodeAsOctetString().getStringValue();

              final Parameter p = parameters.getParameter(name);
              if (p != null)
              {
                p.setValueFromString(value);
              }
            }
          }
          catch (final Exception e)
          {
            parameters = new ParameterList();

            final ASN1Element[] paramsElements =
                 elements[i+1].decodeAsSequence().getElements();
            for (int j=0; j < paramsElements.length; j++)
            {
              final ASN1Element[] paramElements =
                   paramsElements[j].decodeAsSequence().getElements();
              final String name =
                   paramElements[0].decodeAsOctetString().getStringValue();
              final String value =
                   paramElements[1].decodeAsOctetString().getStringValue();

              parameters.addParameter(new StringParameter(name, value));
            }
          }
        }
        else if (elementName.equals(ELEMENT_ACTUAL_START_TIME))
        {
          final String timeStr =
               elements[i+1].decodeAsOctetString().getStringValue();
          actualStartTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_ACTUAL_STOP_TIME))
        {
          final String timeStr =
               elements[i+1].decodeAsOctetString().getStringValue();
          actualStopTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_ACTUAL_DURATION))
        {
          actualDuration = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_STATS))
        {
          statTrackers =
               StatEncoder.sequenceToTrackers(elements[i+1].decodeAsSequence());
        }
        else if (elementName.equals(ELEMENT_LEGACY_MONITOR_STATS))
        {
          final StatTracker[] trackers =
               StatEncoder.sequenceToTrackers(elements[i+1].decodeAsSequence());
          monitorStatTrackers = new ResourceMonitorStatTracker[trackers.length];
          for (int j=0; j < trackers.length; j++)
          {
            monitorStatTrackers[j] = new ResourceMonitorStatTracker(
                 new LegacyResourceMonitor(), trackers[j]);
          }
        }
        else if (elementName.equals(ELEMENT_RESOURCE_MONITOR_STATS))
        {
          monitorStatTrackers = ResourceMonitorStatTracker.sequenceToTrackers(
                                     elements[i+1].decodeAsSequence());
        }
        else if (elementName.equals(ELEMENT_LOG_MESSAGES))
        {
          // FIXME -- Encode the log messages as a single element rather than
          // multiple elements.
          final ASN1Element[] msgElements =
               elements[i+1].decodeAsSequence().getElements();
          logMessages = new String[msgElements.length];
          for (int j=0; j < logMessages.length; j++)
          {
            logMessages[j] =
                 msgElements[j].decodeAsOctetString().getStringValue();
          }
        }
      }

      final Job job = new Job(slamdServer, jobClassName, numClients,
           threadsPerClient, threadStartupDelay, startTime, stopTime, duration,
           collectionInterval, parameters, displayInReadOnlyMode);
      job.setJobID(jobID);
      job.setOptimizingJobID(optimizingJobID);
      job.setJobGroup(jobGroup);
      job.setFolderName(folderName);
      job.setJobState(jobState);
      job.setJobDescription(jobDescription);
      job.setRequestedClients(requestedClients);
      job.setResourceMonitorClients(monitorClients);
      job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
      job.setWaitForClients(waitForClients);
      job.setDependencies(dependencies);
      job.setNotifyAddresses(notifyAddresses);
      job.setJobComments(comments);
      job.setActualStartTime(actualStartTime);
      job.setActualStopTime(actualStopTime);
      job.setActualDuration(actualDuration);
      job.setStatTrackers(statTrackers);
      job.setResourceStatTrackers(monitorStatTrackers);
      job.setLogMessages(logMessages);
      return job;
    }
    catch (final Exception e)
    {
      throw new DecodeException("Unable to decode job:  " + e, e);
    }
  }



  /**
   * Decodes the provided byte array as a SLAMD job, but only decodes a minimal
   * set of data for display on summary pages.
   *
   * @param  slamdServer  The SLAMD server instance with which this job is to be
   *                      associated.
   * @param  encodedJob   The byte array containing the encoded job data.
   *
   * @return  The summary job decoded from the provided byte array.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           job data.
   */
  public static Job decodeSummaryJob(final SLAMDServer slamdServer,
                                     final byte[] encodedJob)
         throws DecodeException
  {
    try
    {
      boolean       displayInReadOnlyMode     = false;
      Date          actualStartTime           = null;
      Date          startTime                 = null;
      int           actualDuration            = -1;
      int           jobState                  = -1;
      String        jobClassName              = null;
      String        jobDescription            = null;
      String        jobID                     = null;

      final SimpleDateFormat dateFormat  =
           new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

      final ASN1Element element = ASN1Element.decode(encodedJob);
      final ASN1Element[] elements = element.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        final String elementName =
             elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_JOB_ID))
        {
          jobID = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          jobClassName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_STATE))
        {
          jobState = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_DISPLAY_IN_READ_ONLY))
        {
          displayInReadOnlyMode =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          jobDescription = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_START_TIME))
        {
          final String timeStr =
               elements[i+1].decodeAsOctetString().getStringValue();
          startTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_ACTUAL_START_TIME))
        {
          final String timeStr =
               elements[i+1].decodeAsOctetString().getStringValue();
          actualStartTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_ACTUAL_DURATION))
        {
          actualDuration = elements[i+1].decodeAsInteger().getIntValue();
        }
      }

      final Job job = new Job(slamdServer, jobClassName, 0, 0, 0, startTime,
           null, 0, 0, null, displayInReadOnlyMode);
      job.setJobID(jobID);
      job.setJobDescription(jobDescription);
      job.setJobState(jobState);
      job.setActualStartTime(actualStartTime);
      job.setActualDuration(actualDuration);
      return job;
    }
    catch (final Exception e)
    {
      throw new DecodeException("Unable to decode job:  " + e, e);
    }
  }



  /**
   * Compares this job with the provided object to determine the relative order
   * of the two in a sorted list.  The comparison will be based on the job IDs,
   * comparing first the timestamp segments, then the counter segments.  If
   * they're all the same, then the iteration and rerun segments will be taken
   * into account if appropriate.
   *
   * @param  j  The job to cmpare with this job.
   *
   * @return  A negative value if this job should be ordered before the provided
   *          object, a positive value if this job should be ordered after the
   *          provided object, or zero if there is no difference in ordering.
   *
   * @throws  ClassCastException  If the provided object is not a Job.
   */
  public int compareTo(final Job j)
          throws ClassCastException
  {
    if (j == null)
    {
      return -1;
    }

    String jobID2 = "";

    try
    {
      jobID2 = j.getJobID();

      final StringTokenizer t1 = new StringTokenizer(jobID, "-");
      final StringTokenizer t2 = new StringTokenizer(jobID2, "-");


      // Get the timestamp string.  If they differ, then use the
      // String.compareTo method.
      final String date1 = t1.nextToken();
      final String date2 = t2.nextToken();
      if (! date1.equals(date2))
      {
        return date1.compareTo(date2);
      }


      // Get the random + counter portion.  If they differ, then compare the
      // numeric counter portions.
      final String counterStr1 = t1.nextToken();
      final String counterStr2 = t2.nextToken();
      if (! counterStr1.equals(counterStr2))
      {
        final Integer counter1 = new Integer(counterStr1.substring(6));
        final Integer counter2 = new Integer(counterStr2.substring(6));
        return counter1.compareTo(counter2);
      }


      // Get the iteration portion, if they exist.
      final Integer iteration1;
      if (t1.hasMoreTokens())
      {
        iteration1 = new Integer(t1.nextToken());
      }
      else
      {
        if (t2.hasMoreTokens())
        {
          // The provided job has an iteration but this job doesn't, so this job
          // will come first.
          return -1;
        }
        else
        {
          // They must be equal.
          return 0;
        }
      }

      if (t2.hasMoreTokens())
      {
        final Integer iteration2 = new Integer(t2.nextToken());

        // It is possible that one of the jobs is a rerun iteration, which
        // should always be ordered after all other iterations of an optimizing
        // job.
        if (t1.hasMoreTokens())
        {
          if (t2.hasMoreTokens())
          {
            // They both still have stuff to come, so we'll order them by name.
            return jobID.compareTo(jobID2);
          }
          else
          {
            // This is almost certainly a rerun iteration.
            return 1;
          }
        }
        else if (t2.hasMoreTokens())
        {
          // The provided job is a rerun iteration.
          return -1;
        }
        else
        {
          // Neither is a rerun iteration so compare the iteration numbers.
          return iteration1.compareTo(iteration2);
        }
      }
      else
      {
        // The provided job does not have an iteration but this job does, so the
        // provided job will come first.
        return 1;
      }
    }
    catch (final Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(e));

      return jobID.compareTo(jobID2);
    }
  }
}

