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
package com.slamd.job;



import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;

import com.sleepycat.je.DatabaseException;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.db.DecodeException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.server.SLAMDServer;
import com.slamd.server.SLAMDServerException;
import com.slamd.server.SMTPMailer;
import com.slamd.server.Scheduler;



/**
 * This class defines an optimizing job, which is a special kind of job that
 * has the ability to execute the same job multiple times varying only the
 * number of threads per client in an attempt to automatically determine the
 * highest or lowest value for a particular statistic.
 *
 *
 * @author   Neil A. Wilson
 */
public class OptimizingJob
       implements Comparable, JobItem
{
  /**
   * The name of the encoded element that holds the optimizing job ID.
   */
  public static final String ELEMENT_OPTIMIZING_JOB_ID = "optimizing_job_id";



  /**
   * The name of the encoded element that holds the job IDs of the standard
   * iterations.
   */
  public static final String ELEMENT_ITERATION_IDS = "iteration_ids";



  /**
   * The name of the encoded element that holds the job IDs of the rerun
   * iteration.
   */
  public static final String ELEMENT_RERUN_ID = "rerun_id";



  /**
   * The name of the encoded element that holds the job class name.
   */
  public static final String ELEMENT_JOB_CLASS = "job_class";



  /**
   * The name of the encoded element that holds the job group name.
   */
  public static final String ELEMENT_JOB_GROUP = "job_group";



  /**
   * The name of the encoded element that holds the job state.
   */
  public static final String ELEMENT_JOB_STATE = "job_state";



  /**
   * The name of the encoded element that holds the job description.
   */
  public static final String ELEMENT_DESCRIPTION = "description";



  /**
   * The name of the encoded element that indicates whether to include the
   * number of threads in the job descriptions.
   */
  public static final String ELEMENT_INCLUDE_THREADS_IN_DESCRIPTION =
       "include_threads_in_description";



  /**
   * The name of the encoded element that indicates whether the job should be
   * displayed in restricted read-only mode.
   */
  public static final String ELEMENT_DISPLAY_IN_READ_ONLY =
       "display_in_read_only";



  /**
   * The name of the encoded element that holds the scheduled start time.
   */
  public static final String ELEMENT_START_TIME = "start_time";



  /**
   * The name of the encoded element that holds the scheduled duration.
   */
  public static final String ELEMENT_DURATION = "duration";



  /**
   * The name of the encoded element that holds the delay between iterations.
   */
  public static final String ELEMENT_DELAY_BETWEEN_ITERATIONS =
       "delay_between_iterations";



  /**
   * The name of the encoded element that holds the number of clients.
   */
  public static final String ELEMENT_NUM_CLIENTS = "num_clients";



  /**
   * The name of the encoded element that holds the requested clients.
   */
  public static final String ELEMENT_REQUESTED_CLIENTS = "requested_clients";



  /**
   * The name of the encoded element that holds the requested monitor clients.
   */
  public static final String ELEMENT_MONITOR_CLIENTS = "monitor_clients";



  /**
   * The name of the encoded element that indicates whether to monitor clients
   * if they are also running resource monitor clients.
   */
  public static final String ELEMENT_MONITOR_CLIENTS_IF_AVAILABLE =
       "monitor_clients_if_available";



  /**
   * The name of the encoded element that holds the minimum number of threads to
   * use.
   */
  public static final String ELEMENT_MIN_THREADS = "min_threads";



  /**
   * The name of the encoded element that holds the maximum number of threads to
   * use.
   */
  public static final String ELEMENT_MAX_THREADS = "max_threads";



  /**
   * The name of the encoded element that holds the thread increment.
   */
  public static final String ELEMENT_THREAD_INCREMENT = "thread_increment";



  /**
   * The name of the encoded element that holds the thread startup delay.
   */
  public static final String ELEMENT_THREAD_STARTUP_DELAY =
       "thread_startup_delay";



  /**
   * The name of the encoded element that holds the collection interval.
   */
  public static final String ELEMENT_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the encoded element that holds the job folder name.
   */
  public static final String ELEMENT_FOLDER_NAME = "folder_name";



  /**
   * The name of the encoded element that holds the maximum allowed nonimproving
   * iterations.
   */
  public static final String ELEMENT_MAX_NONIMPROVING = "max_nonimproving";



  /**
   * The name of the encoded element that indicates whether to rerun the best
   * iteration.
   */
  public static final String ELEMENT_RERUN_BEST_ITERATION =
       "rerun_best_iteration";



  /**
   * The name of the encoded element that holds the scheduled duration for the
   * rerun iteration.
   */
  public static final String ELEMENT_RERUN_DURATION = "rerun_duration";



  /**
   * The name of the encoded element that holds the addresses of the users to
   * notify when the optimizing job is done running.
   */
  public static final String ELEMENT_NOTIFY_ADDRESSES = "notify_addresses";



  /**
   * The name of the encoded element that holds the dependencies.
   */
  public static final String ELEMENT_DEPENDENCIES = "dependencies";



  /**
   * The name of the encoded element that holds the information about the
   * optimization algorithm.
   */
  public static final String ELEMENT_OPTIMIZATION_ALGORITHM =
       "optimization_algorithm";



  /**
   * The name of the encoded element that holds the job parameters.
   */
  public static final String ELEMENT_PARAMETERS = "parameters";



  /**
   * The name of the encoded element that holds the actual start time.
   */
  public static final String ELEMENT_ACTUAL_START_TIME = "actual_start_time";



  /**
   * The name of the encoded element that holds the actual stop time.
   */
  public static final String ELEMENT_ACTUAL_STOP_TIME = "actual_stop_time";



  /**
   * The name of the encoded element that holds the stop reason.
   */
  public static final String ELEMENT_STOP_REASON = "stop_reason";



  /**
   * The name of the encoded element that indicates whether the optimizing job
   * should make the next iteration disabled when it is scheduled.
   */
  public static final String ELEMENT_PAUSE_REQUESTED = "pause_requested";



  /**
   * The name of the encoded element that holds the set of comments for an
   * optimizing job.
   */
  public static final String ELEMENT_COMMENTS = "comments";



  // The set of job iterations associated with this optimizing job.
  private ArrayList<Job> jobList;

  // Indicates whether a request has been received to cancel this job.
  private boolean cancelRequested;

  // Indicates whether this optimizing job should be displayed in restricted
  // read-only mode.
  private boolean displayInReadOnlyMode;

  // Indicates whether to include the number of threads in the description of
  // the job for each iteration.
  private boolean includeThreadsInDescription;

  // Indicates whether an attempt should be made to use any resource monitor
  // clients running on the same system as the client system(s) used to process
  // this job.
  private boolean monitorClientsIfAvailable;

  // Indicates whether a request has been received to pause this job.
  private boolean pauseRequested;

  // Indicates whether the best iteration should be rerun after the job has
  // otherwise completed.
  private boolean reRunBestIteration;

  // The time at which the first iteration of the optimizing job actually
  // started.
  private Date actualStartTime;

  // The time at which the last iteration of the optimizing job completed.
  private Date actualStopTime;

  // The initial start time for the optimizing job.
  private Date startTime;

  // The statistics collection interval for the jobs.
  private int collectionInterval;

  // The current number of non-improving iterations that have been encountered.
  private int currentNonImproving;

  // The number of threads that has yielded the best result so far.
  private int currentOptimalThreads;

  // The length of time in seconds between individual job iterations.
  private int delayBetweenIterations;

  // The maximum length of time for the individual job iterations.
  private int duration;

  // The current state of this optimizing job.
  private int jobState;

  // The maximum number of consecutive non-improving iterations that should be
  // allowed before ending the job.
  private int maxNonImproving;

  // The maximum number of threads that will be allowed for an iteration.
  private int maxThreads;

  // The minimum number of threads that will be allowed for an iteration.
  private int minThreads;

  // The number of clients that will be used for each iteration.
  private int numClients;

  // The duration that should be used to re-run the best iteration of this
  // optimizing job.
  private int reRunDuration;

  // The thread increment that will be used between iterations.
  private int threadIncrement;

  // The thread startup delay that will be used for each iteration.
  private int threadStartupDelay;

  // The iteration of this job that was a re-run of the best iteration,
  // potentially with a different duration.
  private Job reRunIteration;

  // The job class associated with this optimizing job.
  private JobClass jobClass;

  // The optimization algorithm to use for this optimizing job.
  private OptimizationAlgorithm optimizationAlgorithm;

  // The set of job-specific parameters associated with this optimizing job.
  private ParameterList parameters;

  // The SLAMD server with which this optimizing job is associated.
  protected SLAMDServer slamdServer;

  // The set of comments for the optimizing job.
  private String comments;

  // The job ID of the "best" iteration that has been seen so far.
  private String currentOptimalID;

  // The base description that will be used for each iteration.
  private String description;

  // The name of the job folder in which this optimizing job should be placed.
  private String folderName;

  // The name of the job group with which this optimizing job is associated.
  private String jobGroup;

  // The job ID for this optimizing job.
  private String optimizingJobID;

  // The reason that the optimization process stopped.
  private String stopReason;

  // The set of dependencies associated with this optimizing job.
  private String[] dependencies;

  // The set of resource monitor clients that have been requested for executing
  // the jobs.
  private String[] monitorClients;

  // The e-mail addresses of the users to notify when the optimization process
  // is complete.
  private String[] notifyAddresses;

  // The set of clients that have been requested for executing the jobs.
  private String[] requestedClients;



  /**
   * Creates a new instance of an optimizing job based on the provided
   * information.
   *
   * @param  slamdServer                  The SLAMD server with which this
   *                                      optimizing job is associated.
   * @param  optimizingJobID              The unique ID for this optimizing job.
   * @param  optimizationAlgorithm        The optimization algorithm to use for
   *                                      this optimizing job.
   * @param  baseJob                      The base job that should be used to
   *                                      provide parameter information for this
   *                                      optimizing job.
   * @param  folderName                   The name of the job folder in which
   *                                      the optimizing job should be placed.
   * @param  description                  The base description for each job
   *                                      iteration.
   * @param  includeThreadsInDescription  Indicates whether the thread count
   *                                      should be included in the description
   *                                      for each job iteration.
   * @param  startTime                    The time that the first iteration
   *                                      should start running.
   * @param  duration                     The maximum length of time any job
   *                                      iteration should be allowed to run.
   * @param  delayBetweenIterations       The length of time in seconds that
   *                                      should be left between job iterations.
   * @param  numClients                   The number of clients that should be
   *                                      used to run each iteration.
   * @param  requestedClients             The set of clients that have been
   *                                      requested for each iteration.
   * @param  monitorClients               The set of resource monitor clients
   *                                      that have been requested for this
   *                                      optimizing job.
   * @param  monitorClientsIfAvailable    Indicates whether any resource monitor
   *                                      clients on the same system(s) as the
   *                                      job clients should automatically be
   *                                      used.
   * @param  minThreads                   The minimum number of threads that
   *                                      should be used on each client.
   * @param  maxThreads                   The maximum number of threads that
   *                                      should be used for each client.  If
   *                                      there is no maximum, this should be
   *                                      negative.
   * @param  threadIncrement              The increase in threads that should be
   *                                      used between iterations.
   * @param  collectionInterval           The statistics collection interval
   *                                      that should be used for the job
   *                                      iterations.
   * @param  maxNonImproving              The maximum number of consecutive
   *                                      non-improving iterations that will be
   *                                      allowed before ending the job.
   * @param  notifyAddresses              The e-mail addresses of the users that
   *                                      should be notified when the
   *                                      optimization process is complete.
   * @param  reRunBestIteration           Indicates whether the best iteration
   *                                      should be re-run once the optimizing
   *                                      job is otherwise complete.
   * @param  reRunDuration                The duration that should be used when
   *                                      re-running the best iteration.
   * @param  displayInReadOnlyMode        Indicates whether this optimizing job
   *                                      should be displayed in restricted
   *                                      read-only mode.
   */
  public OptimizingJob(SLAMDServer slamdServer, String optimizingJobID,
                       OptimizationAlgorithm optimizationAlgorithm, Job baseJob,
                       String folderName, String description,
                       boolean includeThreadsInDescription, Date startTime,
                       int duration, int delayBetweenIterations,
                       int numClients, String[] requestedClients,
                       String[] monitorClients,
                       boolean monitorClientsIfAvailable, int minThreads,
                       int maxThreads, int threadIncrement,
                       int collectionInterval, int maxNonImproving,
                       String[] notifyAddresses, boolean reRunBestIteration,
                       int reRunDuration, boolean displayInReadOnlyMode)
  {
    this.slamdServer                 = slamdServer;
    this.optimizingJobID             = optimizingJobID;
    this.optimizationAlgorithm       = optimizationAlgorithm;
    this.folderName                  = folderName;
    this.description                 = description;
    this.includeThreadsInDescription = includeThreadsInDescription;
    this.startTime                   = startTime;
    this.duration                    = duration;
    this.delayBetweenIterations      = delayBetweenIterations;
    this.numClients                  = numClients;
    this.requestedClients            = requestedClients;
    this.monitorClients              = monitorClients;
    this.monitorClientsIfAvailable   = monitorClientsIfAvailable;
    this.minThreads                  = minThreads;
    this.maxThreads                  = maxThreads;
    this.threadIncrement             = threadIncrement;
    this.collectionInterval          = collectionInterval;
    this.maxNonImproving             = maxNonImproving;
    this.notifyAddresses             = notifyAddresses;
    this.reRunBestIteration          = reRunBestIteration;
    this.reRunDuration               = reRunDuration;
    this.displayInReadOnlyMode       = displayInReadOnlyMode;

    cancelRequested       = false;
    jobClass              = baseJob.getJobClass();
    parameters            = baseJob.getParameterList();
    currentOptimalID      = null;
    currentOptimalThreads = 0;
    currentNonImproving   = 0;
    actualStartTime       = null;
    actualStopTime        = null;
    stopReason            = null;
    jobState              = Constants.JOB_STATE_NOT_YET_STARTED;
    reRunIteration        = null;
    jobList               = new ArrayList<Job>();
    dependencies          = new String[0];
  }



  /**
   * Creates a new instance of an optimizing job based on the provided
   * information.
   *
   * @param  slamdServer                  The SLAMD server with which this
   *                                      optimizing job is associated.
   * @param  optimizingJobID              The unique ID for this optimizing job.
   * @param  optimizationAlgorithm        The optimization algorithm to use for
   *                                      this optimizing job.
   * @param  jobClass                     The job class associated with this
   *                                      optimizing job.
   * @param  folderName                   The name of the folder in which this
   *                                      optimizing job should be placed.
   * @param  description                  The base description for each job
   *                                      iteration.
   * @param  includeThreadsInDescription  Indicates whether the thread count
   *                                      should be included in the description
   *                                      for each job iteration.
   * @param  startTime                    The time that the first iteration
   *                                      should start running.
   * @param  duration                     The maximum length of time any job
   *                                      iteration should be allowed to run.
   * @param  delayBetweenIterations       The length of time in seconds that
   *                                      should be left between job iterations.
   * @param  numClients                   The number of clients that should be
   *                                      used to run each iteration.
   * @param  requestedClients             The set of clients that have been
   *                                      requested for each iteration.
   * @param  monitorClients               The set of resource monitor clients
   *                                      that have been requested for this
   *                                      optimizing job.
   * @param  monitorClientsIfAvailable    Indicates whether any resource monitor
   *                                      clients on the same system(s) as the
   *                                      job clients should automatically be
   *                                      used.
   * @param  minThreads                   The minimum number of threads that
   *                                      should be used on each client.
   * @param  maxThreads                   The maximum number of threads that
   *                                      should be used for each client.  If
   *                                      there is no maximum, this should be
   *                                      negative.
   * @param  threadIncrement              The increase in threads that should be
   *                                      used between iterations.
   * @param  collectionInterval           The statistics collection interval
   *                                      that should be used for the job
   *                                      iterations.
   * @param  maxNonImproving              The maximum number of consecutive
   *                                      non-improving iterations that will be
   *                                      allowed before ending the job.
   * @param  notifyAddresses              The e-mail addresses of the users that
   *                                      should be notified when the
   *                                      optimization process is complete.
   * @param  reRunBestIteration           Indicates whether the best iteration
   *                                      should be re-run once the optimizing
   *                                      job is otherwise complete.
   * @param  reRunDuration                The duration that should be used when
   *                                      re-running the best iteration.
   * @param  parameters                   The parameter list for this optimizing
   *                                      job.
   * @param  displayInReadOnlyMode        Indicates whether this optimizing job
   *                                      should be displayed in restricted
   *                                      read-only mode.
   */
  public OptimizingJob(SLAMDServer slamdServer, String optimizingJobID,
                       OptimizationAlgorithm optimizationAlgorithm,
                       JobClass jobClass, String folderName, String description,
                       boolean includeThreadsInDescription, Date startTime,
                       int duration, int delayBetweenIterations,
                       int numClients, String[] requestedClients,
                       String[] monitorClients,
                       boolean monitorClientsIfAvailable, int minThreads,
                       int maxThreads, int threadIncrement,
                       int collectionInterval, int maxNonImproving,
                       String[] notifyAddresses, boolean reRunBestIteration,
                       int reRunDuration, ParameterList parameters,
                       boolean displayInReadOnlyMode)
  {
    this.slamdServer                 = slamdServer;
    this.optimizingJobID             = optimizingJobID;
    this.optimizationAlgorithm       = optimizationAlgorithm;
    this.jobClass                    = jobClass;
    this.folderName                  = folderName;
    this.description                 = description;
    this.includeThreadsInDescription = includeThreadsInDescription;
    this.startTime                   = startTime;
    this.duration                    = duration;
    this.delayBetweenIterations      = delayBetweenIterations;
    this.numClients                  = numClients;
    this.requestedClients            = requestedClients;
    this.monitorClients              = monitorClients;
    this.monitorClientsIfAvailable   = monitorClientsIfAvailable;
    this.minThreads                  = minThreads;
    this.maxThreads                  = maxThreads;
    this.threadIncrement             = threadIncrement;
    this.collectionInterval          = collectionInterval;
    this.maxNonImproving             = maxNonImproving;
    this.notifyAddresses             = notifyAddresses;
    this.reRunBestIteration          = reRunBestIteration;
    this.reRunDuration               = reRunDuration;
    this.parameters                  = parameters;
    this.displayInReadOnlyMode       = displayInReadOnlyMode;

    cancelRequested       = false;
    currentOptimalID      = null;
    currentOptimalThreads = 0;
    currentNonImproving   = 0;
    actualStartTime       = null;
    actualStopTime        = null;
    stopReason            = null;
    jobState              = Constants.JOB_STATE_NOT_YET_STARTED;
    reRunIteration        = null;
    jobList               = new ArrayList<Job>();
    dependencies          = new String[0];
  }



  /**
   * Retrieves the unique ID associated with this optimizing job.
   *
   * @return  The unique ID associated with this optimizing job.
   */
  public String getOptimizingJobID()
  {
    return optimizingJobID;
  }



  /**
   * Retrieves the optimization algorithm associated with this optimizing job.
   *
   * @return  The optimization algorithm associated with this optimizing job.
   */
  public OptimizationAlgorithm getOptimizationAlgorithm()
  {
    return optimizationAlgorithm;
  }



  /**
   * Retrieves the job class associated with this optimizing job.
   *
   * @return  The job class associated with this optimizing job.
   */
  public JobClass getJobClass()
  {
    return jobClass;
  }



  /**
   * Retrieves the name of the job class associated with this optimizing job.
   *
   * @return  The name of the job class associated with this optimizing job.
   */
  public String getJobClassName()
  {
    return jobClass.getClass().getName();
  }



  /**
   * Retrieves the job name for the job class associated with this optimizing
   * job.
   *
   * @return  The job name for the job class associated with this optimizing
   *          job.
   */
  public String getJobName()
  {
    return jobClass.getJobName();
  }



  /**
   * Retrieves the description for the job class associated with this optimizing
   * job.
   *
   * @return  The description for the job class associated with this optimizing
   *          job.
   */
  public String getJobClassDescription()
  {
    return jobClass.getShortDescription();
  }



  /**
   * Retrieves the name of the job group with which this optimizing job is
   * associated.
   *
   * @return  The name of the job group with which this optimizing job is
   *          associated, or <CODE>null</CODE> if it was not scheduled as part
   *          of a job group.
   */
  public String getJobGroup()
  {
    return jobGroup;
  }



  /**
   * Specifies the name of the job group with which this optimizing job is
   * associated.
   *
   * @param  jobGroup  The name of the job group with which this optimizing job
   *                   is associated.
   */
  public void setJobGroup(String jobGroup)
  {
    this.jobGroup = jobGroup;
  }



  /**
   * Indicates whether this optimizing job should be displayed in restricted
   * read-only mode.
   *
   * @return  <CODE>true</CODE> if this optimizing job should be displayed in
   *          restricted read-only mode, or <CODE>false</CODE> if not.
   */
  public boolean displayInReadOnlyMode()
  {
    return displayInReadOnlyMode;
  }



  /**
   * Specifies whether this optimizing job should be displayed in restricted
   * read-only mode.
   *
   * @param  displayInReadOnlyMode  Indicates whether this optimizing job should
   *                                be displayed in restricted read-only mode.
   */
  public void setDisplayInReadOnlyMode(boolean displayInReadOnlyMode)
  {
    this.displayInReadOnlyMode = displayInReadOnlyMode;
  }



  /**
   * Retrieves the name of the job folder in which this optimizing job is
   * located.
   *
   * @return  The name of the job folder in which this optimizing job is
   *          located.
   */
  public String getFolderName()
  {
    return folderName;
  }



  /**
   * Specifies the name of the job folder in which this optimizing job is
   * located.
   *
   * @param  folderName  The name of the job folder in which this optimizing job
   *                     is located.
   */
  public void setFolderName(String folderName)
  {
    this.folderName = folderName;
  }



  /**
   * Retrieves the set of parameter stubs associated with this optimizing job.
   *
   * @return  The set of parameter stubs associated with this optimizing job.
   */
  public ParameterList getParameterStubs()
  {
    return jobClass.getParameterStubs();
  }



  /**
   * Retrieves the set of parameter stubs associated with the optimization
   * algorithm.
   *
   * @return  The set of parameter stubs associated with the optimization
   *          algorithm.
   */
  public ParameterList getOptimizationParameterStubs()
  {
    return
         optimizationAlgorithm.getOptimizationAlgorithmParameterStubs(jobClass);
  }



  /**
   * Retrieves the parameter list associated with this optimizing job.
   *
   * @return  The parameter list associated with this optimizing job.
   */
  public ParameterList getParameters()
  {
    return parameters;
  }



  /**
   * Retrieves the set of parameters associated with the optimization algorithm.
   *
   * @return  The set of parameters associated with the optimization algorithm.
   */
  public ParameterList getOptimizationParameters()
  {
    return optimizationAlgorithm.getOptimizationAlgorithmParameters();
  }



  /**
   * Retrieves the base description for this optimizing job.
   *
   * @return  The base description for this optimizing job.
   */
  public String getDescription()
  {
    return description;
  }



  /**
   * Retrieves the comments for this optimizing job.
   *
   * @return  The comments for this optimizing job, or <CODE>null</CODE> if
   *          there are none.
   */
  public String getComments()
  {
    return comments;
  }



  /**
   * Specifies the set of comments for this optimizing job.
   *
   * @param  comments  The set of comments for this optimizing job.
   */
  public void setComments(String comments)
  {
    this.comments = comments;
  }



  /**
   * Indicates whether the number of threads should be included in the
   * description for each iteration of this optimizing job.
   *
   * @return  <CODE>true</CODE> if the number of threads should be included
   *          in the description for this optimizing job, or <CODE>false</CODE>
   *          if not.
   */
  public boolean includeThreadsInDescription()
  {
    return includeThreadsInDescription;
  }



  /**
   * Retrieves the time at which the first iteration of this optimizing job
   * should start running.
   *
   * @return  The time at which the first iteration of this optimizing job.
   */
  public Date getStartTime()
  {
    return startTime;
  }



  /**
   * Retrieves the maximum length of time that any iteration of this optimizing
   * job should be allowed to run.
   *
   * @return  The maximum length of time that any iteration of this optimizing
   *          job should be allowed to run.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Retrieves the length of time in seconds that should be scheduled between
   * iterations of this optimizing job.
   *
   * @return  The length of time in seconds that should be scheduled between
   *          iterations of this optimizing job.
   */
  public int getDelayBetweenIterations()
  {
    return delayBetweenIterations;
  }



  /**
   * Indicates whether the best iteration should be re-run once the job has
   * completed.
   *
   * @return  <CODE>true</CODE> if the best iteration should be re-run after the
   *          job has completed, or <CODE>false</CODE> if not.
   */
  public boolean reRunBestIteration()
  {
    return reRunBestIteration;
  }



  /**
   * Retrieves the duration that should be used when re-running the best
   * iteration of the optimizing job.
   *
   * @return  The duration that should be used when re-running the best
   *          iteration of the optimizing job.
   */
  public int getReRunDuration()
  {
    return reRunDuration;
  }



  /**
   * Retrieves the job iteration that was a re-run of the best iteration for
   * this optimizing job.
   *
   * @return  The job iteration that was a re-run of the best iteration for this
   *          optimizing job, or <CODE>null</CODE> if there is none or it hasn't
   *          run yet.
   */
  public Job getReRunIteration()
  {
    return reRunIteration;
  }



  /**
   * Specifies the job that is a re-run of the best iteration of this optimizing
   * job.
   *
   * @param  reRunIteration  The job that is a re-run of the best iteration of
   *                         this optimizing job.
   */
  public void setReRunIteration(Job reRunIteration)
  {
    this.reRunIteration = reRunIteration;
  }



  /**
   * Retrieves the number of clients that should be used to run each iteration
   * of this optimizing job.
   *
   * @return  The number of clients that should be used to run each iteration
   *          of this optimizing job.
   */
  public int getNumClients()
  {
    return numClients;
  }



  /**
   * Retrieves the set of clients that have been requested to run each iteration
   * of this optimizing job.
   *
   * @return  The number of clients that should be used to run each iteration of
   *          this optimizing job.
   */
  public String[] getRequestedClients()
  {
    return requestedClients;
  }



  /**
   * Retrieves the set of resource monitor clients that have been requested for
   * this optimizing job.
   *
   * @return  The set of resource monitor clients that have been requested for
   *          this optimizing job.
   */
  public String[] getResourceMonitorClients()
  {
    return monitorClients;
  }



  /**
   * Indicates whether an attempt will be made to use resource monitor clients
   * on the same systems as the clients used to run this job.
   *
   * @return  <CODE>true</CODE> if resource monitor clients on the same systems
   *          as the clients will be used, or <CODE>false</CODE> if not.
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
  public void setMonitorClientsIfAvailable(boolean monitorClientsIfAvailable)
  {
    this.monitorClientsIfAvailable = monitorClientsIfAvailable;
  }



  /**
   * Retrieves the minimum number of threads that should be used in an iteration
   * of this optimizing job.
   *
   * @return  The minimum number of threads that should be used in an iteration
   *          of this optimizing job.
   */
  public int getMinThreads()
  {
    return minThreads;
  }



  /**
   * Retrieves the maximum number of threads that should be used in an iteration
   * of this optimizing job.
   *
   * @return  The maximum number of threads that should be used in an iteration
   *          of this optimizing job.
   */
  public int getMaxThreads()
  {
    return maxThreads;
  }



  /**
   * Retrieves the increment that should be used when increasing the number of
   * threads used between iterations of this optimizing job.
   *
   * @return  The increment that should be used when increasing the number of
   *          threads used between iterations of this optimizing job.
   */
  public int getThreadIncrement()
  {
    return threadIncrement;
  }



  /**
   * Retrieves the thread startup delay for iterations of this optimizing job.
   *
   * @return  The thread startup delay for iterations of this optimizing job.
   */
  public int getThreadStartupDelay()
  {
    return threadStartupDelay;
  }



  /**
   * Specifies the thread startup delay for iterations of this optimizing job.
   *
   * @param  threadStartupDelay  The thread startup delay for iterations of this
   *                             optimizing job.
   */
  public void setThreadStartupDelay(int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
  }



  /**
   * Retrieves the statistics collection interval that should be used for each
   * iteration of this optimizing job.
   *
   * @return  The statistics collection interval that should be used for each
   *          iteration of this optimizing job.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Retrieves the maximum number of consecutive non-improving iterations that
   * will be allowed before the job is determined to have found the optimum
   * value.
   *
   * @return  The maximum number of consecutive non-improving iterations that
   *          will be allowed before the job is determined to have found the
   *          optimum value.
   */
  public int getMaxNonImproving()
  {
    return maxNonImproving;
  }



  /**
   * Retrieves the e-mail address(es) of the user(s) that should be notified
   * whenever the optimization process has completed.
   *
   * @return  The e-mail address(es) of the user(s) that should be notified
   *          whenever the optimization process has completed.
   */
  public String[] getNotifyAddresses()
  {
    return notifyAddresses;
  }



  /**
   * Retrieves the time at which the first iteration of this job actually
   * started running.
   *
   * @return  The time at which the first iteration of this job actually started
   *          running.
   */
  public Date getActualStartTime()
  {
    return actualStartTime;
  }



  /**
   * Specifies the time at which the first iteration of this job actually
   * started running.
   *
   * @param  actualStartTime  The time at which the first iteration of this job
   *                          actually started running.
   */
  public void setActualStartTime(Date actualStartTime)
  {
    this.actualStartTime = actualStartTime;
  }



  /**
   * Retrieves the time at which the last iteration of this job actually
   * completed running.
   *
   * @return  The time at which the last iteration of this job actually
   *          completed running.
   */
  public Date getActualStopTime()
  {
    return actualStopTime;
  }



  /**
   * Specifies the time at which the last iteration of this job actually
   * completed running.
   *
   * @param  actualStopTime  The time at which the last iteration of this job
   *                         actually completed running.
   */
  public void setActualStopTime(Date actualStopTime)
  {
    this.actualStopTime = actualStopTime;
  }



  /**
   * Retrieves the current state of this optimizing job.
   *
   * @return  The current state of this optimizing job.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Specifies the current state of this optimizing job.
   *
   * @param  jobState  The current state of this optimizing job.
   */
  public void setJobState(int jobState)
  {
    this.jobState = jobState;
  }



  /**
   * Retrieves the string representation of the current state for this
   * optimizing job.
   *
   * @return  The string representation of the current state for this optimizing
   *          job.
   */
  public String getJobStateString()
  {
    return Constants.jobStateToString(jobState);
  }



  /**
   * Retrieves the reason that the job stopped running.
   *
   * @return  The reason that the job stopped running.
   */
  public String getStopReason()
  {
    return stopReason;
  }



  /**
   * Specifies the reason that the job stopped running.
   *
   * @param  stopReason  The reason that the job stopped running.
   */
  public void setStopReason(String stopReason)
  {
    this.stopReason = stopReason;
  }



  /**
   * Retrieves the set of jobs scheduled as iterations of this optimizing job.
   *
   * @return  The set of jobs scheduled as iterations of this optimizing job.
   */
  public Job[] getAssociatedJobs()
  {
    if ((jobList == null) || jobList.isEmpty())
    {
      return new Job[0];
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    return jobs;
  }



  /**
   * Retrieves the set of jobs scheduled as iterations of this optimizing job,
   * including the re-run iteration if there is one.
   *
   * @return  The set of jobs scheduled as iterations of this optimizing job,
   *          including the re-run iteration if there is one.
   */
  public Job[] getAssociatedJobsIncludingReRun()
  {
    ArrayList<Job> list = new ArrayList<Job>();
    if (jobList != null)
    {
      list.addAll(jobList);
    }

    if (reRunIteration != null)
    {
      list.add(reRunIteration);
    }

    Job[] jobs = new Job[list.size()];
    list.toArray(jobs);
    return jobs;
  }



  /**
   * Specifies the set of jobs scheduled as iterations of this optimizing job.
   *
   * @param  associatedJobs  The set of jobs scheduled as iterations of this
   *                         optimizing job.
   */
  public void setAssociatedJobs(Job[] associatedJobs)
  {
    // If the provided list of jobs is empty or null, then just clear the
    // list and reset the optimum values.
    if ((associatedJobs == null) || (associatedJobs.length == 0))
    {
      jobList.clear();
      currentOptimalID      = null;
      currentOptimalThreads = 0;
      currentNonImproving   = 0;
      return;
    }


    // First, sort the jobs in the order that they were started.
    Arrays.sort(associatedJobs);

    // Add the jobs into the job list.
    jobList.clear();
    for (int i=0; i < associatedJobs.length; i++)
    {
      jobList.add(associatedJobs[i]);
    }


    // Set the first job to be the optimum so far.
    currentOptimalID      = null;
    currentOptimalThreads = 0;
    currentNonImproving   = 0;
    optimizationAlgorithm.reInitializeOptimizationAlgorithm();


    // Iterate through the list of remaining jobs and extract the optimum values
    // from them.
    for (int i=0; i < associatedJobs.length; i++)
    {
      if (! associatedJobs[i].doneRunning())
      {
        break;
      }

      try
      {
        if (optimizationAlgorithm.isBestIterationSoFar(associatedJobs[i]))
        {
          currentOptimalID      = associatedJobs[i].getJobID();
          currentOptimalThreads = associatedJobs[i].getThreadsPerClient();
          currentNonImproving   = 0;
        }
        else
        {
          currentNonImproving++;
        }
      }
      catch (Exception e)
      {
        // There's really not much that can be done about this.  However, it
        // should never happen for any case except the last iteration.
        currentNonImproving++;
      }
    }
  }



  /**
   * Retrieves the set of dependencies associated with this optimizing job.
   *
   * @return  The set of dependencies associated with this optimizing job.
   */
  public String[] getDependencies()
  {
    return dependencies;
  }



  /**
   * Specifies the set of dependencies for this optimizing job.
   *
   * @param  dependencies  The set of dependencies for this optimizing job.
   */
  public void setDependencies(String[] dependencies)
  {
    if (dependencies == null)
    {
      this.dependencies = new String[0];
    }
    else
    {
      this.dependencies = dependencies;
    }
  }



  /**
   * Indicates whether the optimization process has completed (i.e., no
   * additional iterations will be performed, for whatever reason).
   *
   * @return  <CODE>true</CODE> if the optimization process has completed, or
   *          <CODE>false</CODE> if not.
   */
  public boolean doneRunning()
  {
    switch (jobState)
    {
      case Constants.JOB_STATE_NOT_YET_STARTED:
      case Constants.JOB_STATE_RUNNING:
      case Constants.JOB_STATE_DISABLED:
      case Constants.JOB_STATE_UNINITIALIZED:
        return false;
      default:
        return true;
    }
  }



  /**
   * Indicates whether at least some statistical information is available for
   * this optimizing job.
   *
   * @return  <CODE>true</CODE> if there is statistical information available
   *          for this optimizing job, or <CODE>false</CODE> if not.
   */
  public boolean hasStats()
  {
    if (jobList.isEmpty())
    {
      return false;
    }

    for (int i=0; i < jobList.size(); i++)
    {
      Job job = jobList.get(i);
      if (job.hasStats())
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the optimal thread count that has been identified.  Note that
   * this may not be accurate if the job has not yet completed.
   *
   * @return  The optimal thread count that has been identified, or -1 if no
   *          optimal thread count has been determined.
   */
  public int getOptimalThreadCount()
  {
    return currentOptimalThreads;
  }



  /**
   * Retrieves the optimal value that has been identified.  Note that this
   * may not be accurate if the job has not yet completed.
   *
   * @return  The optimal value that has been identified, or -1.0 if no
   *          optimal value has been determined.
   */
  public double getOptimalValue()
  {
    if (currentOptimalID != null)
    {
      for (int i=0; i < jobList.size(); i++)
      {
        Job iteration = jobList.get(i);
        if (iteration.getJobID().equals(currentOptimalID))
        {
          try
          {
            return
                 optimizationAlgorithm.getIterationOptimizationValue(iteration);
          }
          catch (Exception e)
          {
            return -1.0;
          }
        }
      }
    }

    return -1.0;
  }



  /**
   * Retrieves the job ID of the optimal iteration.  Note that this may not be
   * accurate if the job has not yet completed.
   *
   * @return  The job ID of the optimal iteration, or <CODE>null</CODE> if no
   *          optimal iteration has been determined.
   */
  public String getOptimalJobID()
  {
    return currentOptimalID;
  }



  /**
   * Specifies the optimal iteration for this optimizing job.  This method
   * should only be used by optimization algorithms.
   *
   * @param  optimalIteration  The job that is the optimal iteration for this
   *                           optimizing job.
   */
  public void setOptimalIteration(Job optimalIteration)
  {
    currentOptimalID      = optimalIteration.getJobID();
    currentOptimalThreads = optimalIteration.getThreadsPerClient();
  }



  /**
   * Retrieves the job that was the optimal iteration for this optimizing job.
   * Note that this may not be accurate if the job has not yet completed.
   *
   * @return  The job that was the optimal iteration for this optimizing job.
   */
  public Job getOptimalIteration()
  {
    if (currentOptimalID == null)
    {
      return null;
    }

    for (int i=0; i < jobList.size(); i++)
    {
      Job job = jobList.get(i);
      if (job.getJobID().equals(currentOptimalID))
      {
        return job;
      }
    }

    return null;
  }



  /**
   * Causes the first iteration of this optimizing job to be scheduled for
   * execution in the SLAMD server.
   *
   * @throws  SLAMDServerException  If a problem occurs while scheduling the
   *                                optimizing job.
   */
  public void schedule()
         throws SLAMDServerException
  {
    try
    {
      Job job = new Job(slamdServer, jobClass.getClass().getName(),
                        numClients, minThreads, 0, startTime, null, duration,
                        collectionInterval, parameters, displayInReadOnlyMode);

      job.setOptimizingJobID(optimizingJobID);
      job.setJobID(optimizingJobID + '-' + minThreads);
      job.setFolderName(folderName);
      job.setRequestedClients(requestedClients);
      job.setResourceMonitorClients(monitorClients);
      job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
      job.setWaitForClients(true);
      job.setDependencies(dependencies);
      job.setThreadStartupDelay(threadStartupDelay);
      job.setJobComments(comments);

      String threadStr = minThreads + " Thread";
      if (minThreads > 1)
      {
        threadStr += "s";
      }
      if ((description == null) || (description.length() == 0))
      {
        if (includeThreadsInDescription)
        {
          job.setJobDescription(threadStr);
        }
      }
      else
      {
        if (includeThreadsInDescription)
        {
          job.setJobDescription(description + " (" + threadStr + ')');
        }
        else
        {
          job.setJobDescription(description);
        }
      }

      jobList.add(job);
      slamdServer.getConfigDB().writeOptimizingJob(this);
      slamdServer.getScheduler().scheduleJob(job, folderName);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new SLAMDServerException("Unable to schedule optimizing job " +
                                     optimizingJobID + ":  " + e, e);
    }
  }



  /**
   * Handles the work of cancelling this optimizing job.
   *
   * @throws  SLAMDServerException  If a problem occurs while cancelling the
   *                                job.
   */
  public void cancel()
         throws SLAMDServerException
  {
    // If this job is done running, then we don't need to do anything.
    if (doneRunning())
    {
      return;
    }


    // Indicate that a cancel has been requested and cancel any iteration that
    // may be pending or running.
    cancelRequested = true;
    boolean runningFound = slamdServer.getScheduler().cancelOptimizingJob(this);

    // Update the job state to indicate that it has been cancelled.
    jobState = Constants.JOB_STATE_CANCELLED;
    stopReason = "The optimizing job was cancelled by administrative " +
                 "request.";

    try
    {
      slamdServer.getConfigDB().writeOptimizingJob(this);
    }
    catch (DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                             "Unable to update optimizing job information " +
                             "for optimizing job " + optimizingJobID +
                             " to indicate the job was cancelled:" + de);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(de));
    }


    // If a running iteration was found, then we shouldn't send the notification
    // yet.  Let that be done by the jobIterationComplete when the job is
    // actually done.
    if (! runningFound)
    {
      sendJobCompleteNotification();
    }
  }



  /**
   * Indicates whether a request has been submitted to pause this optimizing
   * job.
   *
   * @return  <CODE>true</CODE> if a request has been made to pause this
   *          optimizing job, or <CODE>false</CODE> if not.
   */
  public boolean pauseRequested()
  {
    return pauseRequested;
  }



  /**
   * Requests that this optimizing job be paused before its next iteration.  If
   * this is done, the next iteration will be disabled when it is scheduled so
   * that it will not automatically start running when the appropriate start
   * time arrives.
   */
  public void pauseBeforeNextIteration()
  {
    pauseRequested = true;

    try
    {
      slamdServer.getConfigDB().writeOptimizingJob(this);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
    }
  }



  /**
   * Indicates that this optimizing job does not need to be paused before the
   * next iteration.
   */
  public void cancelPause()
  {
    pauseRequested = false;

    try
    {
      slamdServer.getConfigDB().writeOptimizingJob(this);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
    }
  }



  /**
   * Indicates that one iteration of this optimizing job is complete and that
   * any appropriate action should be taken.  If necessary, it will schedule
   * the next iteration.  Otherwise, it will indicate that processing is
   * complete.
   *
   * @param  jobIteration  The job iteration that has completed.
   */
  public void jobIterationComplete(Job jobIteration)
  {
    // Get a reference to the scheduler.
    Scheduler scheduler = slamdServer.getScheduler();


    // First, make sure the job is not null.  That should never happen.
    if (jobIteration == null)
    {
      jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      stopReason = "Reported complete iteration was NULL.";
      try
      {
        slamdServer.getConfigDB().writeOptimizingJob(this);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to update optimizing job information " +
                               "for optimizing job " + optimizingJobID +
                               " to indicate stopped due to null " +
                               "iteration:  " + de);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
      }

      scheduler.decacheOptimizingJob(optimizingJobID);
      sendJobCompleteNotification();
      return;
    }


    // Now check to see if a request has been submitted to cancel this job.  If
    // so, then we're done.
    if (cancelRequested)
    {
      jobState = Constants.JOB_STATE_CANCELLED;
      stopReason = "The optimizing job was cancelled by administrative " +
                   "request.";

      try
      {
        slamdServer.getConfigDB().writeOptimizingJob(this);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to update optimizing job information " +
                               "for optimizing job " + optimizingJobID +
                               " to indicate the job was cancelled:" + de);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
      }

      scheduler.decacheOptimizingJob(optimizingJobID);
      sendJobCompleteNotification();
      return;
    }

      scheduler.decacheOptimizingJob(optimizingJobID);

    // Next, check to see if the job stopped for an acceptable reason.
    boolean acceptableStopReason = false;
    switch (jobIteration.getJobState())
    {
      case Constants.JOB_STATE_COMPLETED_SUCCESSFULLY:
      case Constants.JOB_STATE_STOPPED_DUE_TO_DURATION:
      case Constants.JOB_STATE_STOPPED_DUE_TO_STOP_TIME:
        acceptableStopReason = true;
    }

    if (! acceptableStopReason)
    {
      jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      stopReason = "Job \"" + jobIteration.getJobID() +
                    "\" stopped with a stop reason that was not acceptable " +
                    "for continuing (" +
                    Constants.jobStateToString(jobIteration.getJobState()) +
                    ").";
      try
      {
        slamdServer.getConfigDB().writeOptimizingJob(this);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to update optimizing job information " +
                               "for optimizing job " + optimizingJobID +
                               " to indicate stopped due to unacceptable job " +
                               "iteration state:" + de);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
      }

      scheduler.decacheOptimizingJob(optimizingJobID);
      sendJobCompleteNotification();
      return;
    }


    // See if this was the re-run of the best iteration.  If so, then we know
    // that we won't be running any more.
    if ((reRunIteration != null) &&
        reRunIteration.getJobID().equals(jobIteration.getJobID()))
    {
      jobState = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
      stopReason = "The optimizing job completed successfully after " +
                   "re-running the best iteration.";
      try
      {
        slamdServer.getConfigDB().writeOptimizingJob(this);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to update optimizing job information " +
                               "for optimizing job " + optimizingJobID +
                               " to indicate stopped successfully after " +
                               "re-running the best iteration:  " + de);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
      }

      scheduler.decacheOptimizingJob(optimizingJobID);
      sendJobCompleteNotification();
      return;
    }


    // Check to see if the job had reached the maximum number of threads.
    if ((maxThreads > 0) && (jobIteration.getThreadsPerClient() >= maxThreads))
    {
      // If we should re-run the best iteration, then do so now.
      if (reRunBestIteration && (currentOptimalThreads > 0))
      {
        scheduleReRunOfBestIteration();
        return;
      }
      else
      {
        jobState = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
        stopReason = "The maximum number of threads per client was reached.";
        try
        {
          slamdServer.getConfigDB().writeOptimizingJob(this);
        }
        catch (DatabaseException de)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                                 "Unable to update optimizing job " +
                                 "information for optimizing job " +
                                 optimizingJobID + " to indicate stopped due " +
                                 "to maximum number of threads per client:  " +
                                 de);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(de));
        }

        scheduler.decacheOptimizingJob(optimizingJobID);
        sendJobCompleteNotification();
        return;
      }
    }


    // Check to see if the maximum number of consecutive non-improving
    // iterations has been reached.
    if (currentNonImproving >= maxNonImproving)
    {
      if (reRunBestIteration && (currentOptimalThreads > 0))
      {
        scheduleReRunOfBestIteration();
        return;
      }
      else
      {
        jobState = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
        stopReason = "The maximum number of consecutive non-improving " +
                     "iterations was reached.";
        try
        {
          slamdServer.getConfigDB().writeOptimizingJob(this);
        }
        catch (DatabaseException de)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                                 "Unable to update optimizing job " +
                                 "information for optimizing job " +
                                 optimizingJobID + " to indicate stopped due " +
                                 "to maximum number of non-improving " +
                                 "iterations:  " + de);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(de));
        }

        scheduler.decacheOptimizingJob(optimizingJobID);
        sendJobCompleteNotification();
        return;
      }
    }


    // If we have gotten here, then the optimization process is not yet
    // complete.  Therefore, we need to schedule the next iteration.
    try
    {
      int numThreads = jobIteration.getThreadsPerClient() + threadIncrement;
      if ((maxThreads > 0) && (numThreads > maxThreads))
      {
        numThreads = maxThreads;
      }

      long nextStartTimeMillis = System.currentTimeMillis() +
                                 (1000 * delayBetweenIterations);
      Date nextStartTime = new Date(nextStartTimeMillis);

      Job job = new Job(slamdServer, jobClass.getClass().getName(),
                        numClients, numThreads, 0, nextStartTime, null,
                        duration, collectionInterval,
                        parameters, displayInReadOnlyMode);

      job.setOptimizingJobID(optimizingJobID);
      job.setJobID(optimizingJobID + '-' + numThreads);
      job.setFolderName(folderName);
      job.setRequestedClients(requestedClients);
      job.setResourceMonitorClients(monitorClients);
      job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
      job.setWaitForClients(true);
      job.setJobComments(comments);

      // If this optimizing job should be paused, then do so now.  We will also
      // unset the pause requested flag.
      if (pauseRequested)
      {
        job.setJobState(Constants.JOB_STATE_DISABLED);
        pauseRequested = false;
      }

      String threadStr = numThreads + " Threads";
      if ((description == null) || (description.length() == 0))
      {
        if (includeThreadsInDescription)
        {
          job.setJobDescription(threadStr);
        }
      }
      else
      {
        if (includeThreadsInDescription)
        {
          job.setJobDescription(description + " (" + threadStr + ')');
        }
        else
        {
          job.setJobDescription(description);
        }
      }

      jobList.add(job);
      slamdServer.getConfigDB().writeOptimizingJob(this);
      slamdServer.getScheduler().scheduleJob(job, folderName);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      stopReason = "Unable to schedule a new iteration:  " + e;
      try
      {
        slamdServer.getConfigDB().writeOptimizingJob(this);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to update optimizing job information " +
                               "for optimizing job " + optimizingJobID +
                               " to indicate stopped due to scheduling " +
                               "failure:  " + de);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
      }

      scheduler.decacheOptimizingJob(optimizingJobID);
      sendJobCompleteNotification();
      return;
    }
  }



  /**
   * This schedules a second run of the iteration that yielded the best result.
   */
  public void scheduleReRunOfBestIteration()
  {
    try
    {
      long nextStartTimeMillis = System.currentTimeMillis() +
                                 (1000 * delayBetweenIterations);
      Date nextStartTime = new Date(nextStartTimeMillis);

      Job job = new Job(slamdServer, jobClass.getClass().getName(),
                        numClients, currentOptimalThreads, 0, nextStartTime,
                        null, reRunDuration, collectionInterval, parameters,
                        displayInReadOnlyMode);

      job.setOptimizingJobID(optimizingJobID);
      job.setJobID(optimizingJobID + '-' + currentOptimalThreads + "-rerun");
      job.setFolderName(folderName);
      job.setRequestedClients(requestedClients);
      job.setResourceMonitorClients(monitorClients);
      job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
      job.setWaitForClients(true);
      job.setThreadStartupDelay(threadStartupDelay);
      job.setJobComments(comments);

      // If this optimizing job should be paused, then do so now.  We will also
      // unset the pause requested flag.
      if (pauseRequested)
      {
        job.setJobState(Constants.JOB_STATE_DISABLED);
        pauseRequested = false;
      }

      String threadStr = currentOptimalThreads + " Threads";
      if ((description == null) || (description.length() == 0))
      {
        if (includeThreadsInDescription)
        {
          job.setJobDescription(threadStr + ", re-run best iteration");
        }
      }
      else
      {
        if (includeThreadsInDescription)
        {
          job.setJobDescription(description + " (" + threadStr +
                                ", re-run best iteration)");
        }
        else
        {
          job.setJobDescription(description + " (re-run best iteration)");
        }
      }

      reRunIteration = job;
      slamdServer.getConfigDB().writeOptimizingJob(this);
      slamdServer.getScheduler().scheduleJob(job, folderName);
    }
    catch (Exception e)
    {
      jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      stopReason = "Unable to schedule a re-run of the best iteration:  " + e;
      try
      {
        slamdServer.getConfigDB().writeOptimizingJob(this);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Unable to update optimizing job information " +
                               "for optimizing job " + optimizingJobID +
                               " to indicate stopped due to scheduling " +
                               "failure:  " + de);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
      }

      slamdServer.getScheduler().decacheOptimizingJob(optimizingJobID);
      sendJobCompleteNotification();
      return;
    }
  }



  /**
   * Sends an e-mail message to any configured recipients indicating that the
   * optimization process has completed.
   */
  public void sendJobCompleteNotification()
  {
    // If there is no one to notify, then don't do anything.
    if ((notifyAddresses == null) || (notifyAddresses.length == 0))
    {
      return;
    }


    // Initialize variables we will use later.
    SMTPMailer mailer = slamdServer.getMailer();
    SimpleDateFormat dateFormat =
         new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);


    // Construct an e-mail message with information about the optimizing job.
    String EOL = Constants.SMTP_EOL;
    String subject = "SLAMD optimizing job " + optimizingJobID + " completed";
    StringBuilder message = new StringBuilder();
    message.append("The optimization process for the SLAMD optimizing job " +
                   optimizingJobID + " has completed." + EOL + EOL);

    String baseURI = mailer.getServletBaseURI();
    if ((baseURI != null) && (baseURI.length() > 0))
    {
      message.append("For more detailed information about this job, go to" +
                     EOL);
      message.append(baseURI + '?' + Constants.SERVLET_PARAM_SECTION + '=' +
                     Constants.SERVLET_SECTION_JOB + '&' +
                     Constants.SERVLET_PARAM_SUBSECTION + '=' +
                     Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING + '&' +
                     Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID + '=' +
                     optimizingJobID + EOL + EOL);
    }

    message.append("Optimizing job ID:  " + optimizingJobID + EOL);

    if (actualStartTime != null)
    {
      message.append("Actual Start Time:  " +
                     dateFormat.format(actualStartTime) + EOL);
    }
    if (actualStopTime != null)
    {
      message.append("Actual Stop Time:  " + dateFormat.format(actualStopTime) +
                     EOL);
    }

    message.append("Current State:  " + Constants.jobStateToString(jobState) +
                   EOL);
    message.append("Stop Reason:  " + stopReason + EOL);

    DecimalFormat decimalFormat = new DecimalFormat("0.000");
    if (currentOptimalThreads > 0)
    {
      message.append(EOL);

      ParameterList paramList =
           optimizationAlgorithm.getOptimizationAlgorithmParameters();
      Parameter[] params = paramList.getParameters();
      for (int i=0; i < params.length; i++)
      {
        message.append(params[i].getDisplayName() + ":  " +
                       params[i].getDisplayValue() + EOL);
      }

      message.append("Optimal Number of Threads:  " + currentOptimalThreads +
                     EOL);
      message.append("Optimal Value :  " +
                     decimalFormat.format(getOptimalValue())  + EOL);
      message.append(EOL);
    }

    if (reRunIteration != null)
    {
      try
      {
        double reRunValue =
             optimizationAlgorithm.getIterationOptimizationValue(
                  reRunIteration);
        message.append("Re-Run Value:  " + decimalFormat.format(reRunValue) +
                       EOL);
        message.append(EOL);
      } catch (Exception e) {}
    }

    mailer.sendMessage(notifyAddresses, subject, message.toString());
  }



  /**
   * Encodes information about this optimizing job to a byte array suitable for
   * storage in the configuration database.
   *
   * @return  The byte array containing the encoded optimizing job.
   */
  public byte[] encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();
    SimpleDateFormat dateFormat  =
         new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

    elementList.add(new ASN1OctetString(ELEMENT_OPTIMIZING_JOB_ID));
    elementList.add(new ASN1OctetString(optimizingJobID));
    elementList.add(new ASN1OctetString(ELEMENT_FOLDER_NAME));
    elementList.add(new ASN1OctetString(folderName));
    elementList.add(new ASN1OctetString(ELEMENT_PAUSE_REQUESTED));
    elementList.add(new ASN1Boolean(pauseRequested));

    if ((jobList != null) && (! jobList.isEmpty()))
    {
      ASN1Element[] idElements = new ASN1Element[jobList.size()];
      for (int i=0; i < idElements.length; i++)
      {
        Job job = jobList.get(i);
        idElements[i] = new ASN1OctetString(job.getJobID());
      }

      elementList.add(new ASN1OctetString(ELEMENT_ITERATION_IDS));
      elementList.add(new ASN1Sequence(idElements));
    }

    if (reRunIteration != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_RERUN_ID));
      elementList.add(new ASN1OctetString(reRunIteration.getJobID()));
    }

    elementList.add(new ASN1OctetString(ELEMENT_JOB_CLASS));
    elementList.add(new ASN1OctetString(jobClass.getClass().getName()));
    elementList.add(new ASN1OctetString(ELEMENT_JOB_STATE));
    elementList.add(new ASN1Integer(jobState));

    if ((jobGroup != null) && (jobGroup.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_JOB_GROUP));
      elementList.add(new ASN1OctetString(jobGroup));
    }

    if ((description != null) && (description.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_DESCRIPTION));
      elementList.add(new ASN1OctetString(description));
    }

    elementList.add(new ASN1OctetString(
                             ELEMENT_INCLUDE_THREADS_IN_DESCRIPTION));
    elementList.add(new ASN1Boolean(includeThreadsInDescription));
    elementList.add(new ASN1OctetString(ELEMENT_DISPLAY_IN_READ_ONLY));
    elementList.add(new ASN1Boolean(displayInReadOnlyMode));

    if (startTime != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_START_TIME));
      elementList.add(new ASN1OctetString(dateFormat.format(startTime)));
    }

    elementList.add(new ASN1OctetString(ELEMENT_DURATION));
    elementList.add(new ASN1Integer(duration));
    elementList.add(new ASN1OctetString(ELEMENT_DELAY_BETWEEN_ITERATIONS));
    elementList.add(new ASN1Integer(delayBetweenIterations));
    elementList.add(new ASN1OctetString(ELEMENT_NUM_CLIENTS));
    elementList.add(new ASN1Integer(numClients));

    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      ASN1Element[] clientElements = new ASN1Element[requestedClients.length];
      for (int i=0; i < requestedClients.length; i++)
      {
        clientElements[i] = new ASN1OctetString(requestedClients[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_REQUESTED_CLIENTS));
      elementList.add(new ASN1Sequence(clientElements));
    }

    if ((monitorClients != null) && (monitorClients.length > 0))
    {
      ASN1Element[] clientElements = new ASN1Element[monitorClients.length];
      for (int i=0; i < monitorClients.length; i++)
      {
        clientElements[i] = new ASN1OctetString(monitorClients[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_MONITOR_CLIENTS));
      elementList.add(new ASN1Sequence(clientElements));
    }

    elementList.add(new ASN1OctetString(ELEMENT_MONITOR_CLIENTS_IF_AVAILABLE));
    elementList.add(new ASN1Boolean(monitorClientsIfAvailable));
    elementList.add(new ASN1OctetString(ELEMENT_MIN_THREADS));
    elementList.add(new ASN1Integer(minThreads));
    elementList.add(new ASN1OctetString(ELEMENT_MAX_THREADS));
    elementList.add(new ASN1Integer(maxThreads));
    elementList.add(new ASN1OctetString(ELEMENT_THREAD_INCREMENT));
    elementList.add(new ASN1Integer(threadIncrement));
    elementList.add(new ASN1OctetString(ELEMENT_COLLECTION_INTERVAL));
    elementList.add(new ASN1Integer(collectionInterval));
    elementList.add(new ASN1OctetString(ELEMENT_MAX_NONIMPROVING));
    elementList.add(new ASN1Integer(maxNonImproving));
    elementList.add(new ASN1OctetString(ELEMENT_RERUN_BEST_ITERATION));
    elementList.add(new ASN1Boolean(reRunBestIteration));
    elementList.add(new ASN1OctetString(ELEMENT_RERUN_DURATION));
    elementList.add(new ASN1Integer(reRunDuration));

    if (threadStartupDelay > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_THREAD_STARTUP_DELAY));
      elementList.add(new ASN1Integer(threadStartupDelay));
    }

    if ((notifyAddresses != null) && (notifyAddresses.length > 0))
    {
      ASN1Element[] addrElements = new ASN1Element[notifyAddresses.length];
      for (int i=0; i < notifyAddresses.length; i++)
      {
        addrElements[i] = new ASN1OctetString(notifyAddresses[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_NOTIFY_ADDRESSES));
      elementList.add(new ASN1Sequence(addrElements));
    }

    if ((dependencies != null) && (dependencies.length > 0))
    {
      ASN1Element[] dependencyElements = new ASN1Element[dependencies.length];
      for (int i=0; i < dependencies.length; i++)
      {
        dependencyElements[i] = new ASN1OctetString(dependencies[i]);
      }

      elementList.add(new ASN1OctetString(ELEMENT_DEPENDENCIES));
      elementList.add(new ASN1Sequence(dependencyElements));
    }


    if (jobClass instanceof UnknownJobClass)
    {
      ASN1Element[] optimizationAlgorithmElements = new ASN1Element[]
      {
        new ASN1OctetString(optimizationAlgorithm.getClass().getName()),
        new ASN1Sequence()
      };
      elementList.add(new ASN1OctetString(ELEMENT_OPTIMIZATION_ALGORITHM));
      elementList.add(new ASN1Sequence(optimizationAlgorithmElements));
    }
    else
    {
      Parameter[] optimizationParams =
           optimizationAlgorithm.getOptimizationAlgorithmParameters().
                getParameters();
      ASN1Element[] optParamsElements =
           new ASN1Element[optimizationParams.length];
      for (int i=0; i < optimizationParams.length; i++)
      {
        ASN1Element[] optParamElements = new ASN1Element[]
        {
          new ASN1OctetString(optimizationParams[i].getName()),
          new ASN1OctetString(optimizationParams[i].getValueString())
        };

        optParamsElements[i] = new ASN1Sequence(optParamElements);
      }
      ASN1Element[] optimizationAlgorithmElements = new ASN1Element[]
      {
        new ASN1OctetString(optimizationAlgorithm.getClass().getName()),
        new ASN1Sequence(optParamsElements)
      };
      elementList.add(new ASN1OctetString(ELEMENT_OPTIMIZATION_ALGORITHM));
      elementList.add(new ASN1Sequence(optimizationAlgorithmElements));
    }

    Parameter[] params = parameters.getParameters();
    ArrayList<ASN1Element> paramList = new ArrayList<ASN1Element>();
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
    ASN1Element[] paramsElements = new ASN1Element[paramList.size()];
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

    if ((stopReason != null) && (stopReason.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_STOP_REASON));
      elementList.add(new ASN1OctetString(stopReason));
    }

    if ((comments != null) && (comments.length() > 0))
    {
      elementList.add(new ASN1OctetString(ELEMENT_COMMENTS));
      elementList.add(new ASN1OctetString(comments));
    }


    ASN1Element[] elements = new ASN1Element[elementList.size()];
    elementList.toArray(elements);
    return new ASN1Sequence(elements).encode();
  }



  /**
   * Decodes the provided byte array as an optimizing job.
   *
   * @param  slamdServer           The SLAMD server with which the optimizing
   *                               job is associated.
   * @param  encodedOptimizingJob  The byte array containing the encoded
   *                               optimizing job.
   *
   * @return  The optimizing job decoded from the provided byte array.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the provided byte array as an optimizing job.
   */
  public static OptimizingJob decode(SLAMDServer slamdServer,
                                     byte[] encodedOptimizingJob)
         throws DecodeException
  {
    try
    {
      boolean               displayInReadOnlyMode       = false;
      boolean               includeThreadsInDescription = false;
      boolean               monitorClientsIfAvailable   = false;
      boolean               pauseRequested              = false;
      boolean               reRunBestIteration          = false;
      Date                  actualStartTime             = null;
      Date                  actualStopTime              = null;
      Date                  startTime                   = null;
      int                   collectionInterval          = -1;
      int                   delayBetweenIterations      = 0;
      int                   duration                    = -1;
      int                   jobState                    = -1;
      int                   maxNonImproving             = 1;
      int                   maxThreads                  = 1;
      int                   minThreads                  = 1;
      int                   numClients                  = -1;
      int                   reRunDuration               = -1;
      int                   threadIncrement             = 1;
      int                   threadStartupDelay          = 0;
      Job                   reRunIteration              = null;
      Job[]                 iterations                  = new Job[0];
      JobClass              jobClass                    = null;
      OptimizationAlgorithm optimizationAlgorithm       = null;
      ParameterList         parameters                  = new ParameterList();
      ParameterList         optimizationParameters      = new ParameterList();
      String                comments                    = null;
      String                description                 = null;
      String                folderName                  = null;
      String                jobGroup                    = null;
      String                optimizingJobID             = null;
      String                stopReason                  = null;
      String[]              dependencies                = new String[0];
      String[]              monitorClients              = new String[0];
      String[]              notifyAddresses             = new String[0];
      String[]              requestedClients            = new String[0];

      SimpleDateFormat dateFormat  =
           new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

      ASN1Element   element  = ASN1Element.decode(encodedOptimizingJob);
      ASN1Element[] elements = element.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_OPTIMIZING_JOB_ID))
        {
          optimizingJobID =
               elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_FOLDER_NAME))
        {
          folderName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_PAUSE_REQUESTED))
        {
          pauseRequested = elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_ITERATION_IDS))
        {
          ASN1Element[] idElements =
               elements[i+1].decodeAsSequence().getElements();
          ArrayList<Job> jobList = new ArrayList<Job>(idElements.length);
          for (int j=0; j < idElements.length; j++)
          {
            String jobID = idElements[j].decodeAsOctetString().getStringValue();
            Job job = slamdServer.getConfigDB().getJob(jobID);
            if (job != null)
            {
              jobList.add(job);
            }
          }

          iterations = new Job[jobList.size()];
          jobList.toArray(iterations);
        }
        else if (elementName.equals(ELEMENT_RERUN_ID))
        {
          String jobID = elements[i+1].decodeAsOctetString().getStringValue();
          reRunIteration = slamdServer.getConfigDB().getJob(jobID);
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          String jobClassName =
               elements[i+1].decodeAsOctetString().getStringValue();
          jobClass = slamdServer.getOrLoadJobClass(jobClassName);
        }
        else if (elementName.equals(ELEMENT_JOB_STATE))
        {
          jobState = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_JOB_GROUP))
        {
          jobGroup = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          description = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_INCLUDE_THREADS_IN_DESCRIPTION))
        {
          includeThreadsInDescription =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_DISPLAY_IN_READ_ONLY))
        {
          displayInReadOnlyMode =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_START_TIME))
        {
          String timeStr = elements[i+1].decodeAsOctetString().getStringValue();
          startTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_DURATION))
        {
          duration = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_DELAY_BETWEEN_ITERATIONS))
        {
          delayBetweenIterations =
               elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_NUM_CLIENTS))
        {
          numClients = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_REQUESTED_CLIENTS))
        {
          ASN1Element[] clientElements =
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
          ASN1Element[] clientElements =
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
        else if (elementName.equals(ELEMENT_MIN_THREADS))
        {
          minThreads = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_MAX_THREADS))
        {
          maxThreads = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_THREAD_INCREMENT))
        {
          threadIncrement = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_THREAD_STARTUP_DELAY))
        {
          threadStartupDelay = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_COLLECTION_INTERVAL))
        {
          collectionInterval = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_MAX_NONIMPROVING))
        {
          maxNonImproving = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_RERUN_BEST_ITERATION))
        {
          reRunBestIteration =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_RERUN_DURATION))
        {
          reRunDuration = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_NOTIFY_ADDRESSES))
        {
          ASN1Element[] addrElements =
               elements[i+1].decodeAsSequence().getElements();
          notifyAddresses = new String[addrElements.length];
          for (int j=0; j < notifyAddresses.length; j++)
          {
            notifyAddresses[j] =
                 addrElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_DEPENDENCIES))
        {
          ASN1Element[] dependencyElements =
               elements[i+1].decodeAsSequence().getElements();
          dependencies = new String[dependencyElements.length];
          for (int j=0; j < dependencies.length; j++)
          {
            dependencies[j] =
                 dependencyElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_OPTIMIZATION_ALGORITHM))
        {
          if (jobClass == null)
          {
            for (int j=i+2; j < elements.length; j += 2)
            {
              String name = elements[j].decodeAsOctetString().getStringValue();
              if (name.equals(ELEMENT_JOB_CLASS))
              {
                String className =
                     elements[j+1].decodeAsOctetString().getStringValue();
                jobClass = slamdServer.getOrLoadJobClass(className);
                break;
              }
            }
          }

          ASN1Element[] algorithmElements =
               elements[i+1].decodeAsSequence().getElements();
          String algorithmName =
               algorithmElements[0].decodeAsOctetString().getStringValue();
          optimizationAlgorithm = (OptimizationAlgorithm)
               Constants.classForName(algorithmName).newInstance();

          optimizationParameters = optimizationAlgorithm.
               getOptimizationAlgorithmParameterStubs(jobClass).clone();
          ASN1Element[] paramsElements =
               algorithmElements[1].decodeAsSequence().getElements();
          for (int j=0; j < paramsElements.length; j++)
          {
            ASN1Element[] paramElements =
                 paramsElements[j].decodeAsSequence().getElements();
            String name =
                 paramElements[0].decodeAsOctetString().getStringValue();
            String value =
                 paramElements[1].decodeAsOctetString().getStringValue();

            Parameter p = optimizationParameters.getParameter(name);
            if (p != null)
            {
              p.setValueFromString(value);
            }
          }
        }
        else if (elementName.equals(ELEMENT_PARAMETERS))
        {
          if (jobClass == null)
          {
            for (int j=i+2; j < elements.length; j += 2)
            {
              String name = elements[j].decodeAsOctetString().getStringValue();
              if (name.equals(ELEMENT_JOB_CLASS))
              {
                String jobClassName =
                     elements[j+1].decodeAsOctetString().getStringValue();
                jobClass = slamdServer.getOrLoadJobClass(jobClassName);
                break;
              }
            }
          }

          parameters = jobClass.getClientSideParameterStubs().clone();
          ASN1Element[] paramsElements =
               elements[i+1].decodeAsSequence().getElements();
          for (int j=0; j < paramsElements.length; j++)
          {
            ASN1Element[] paramElements =
                 paramsElements[j].decodeAsSequence().getElements();
            String name =
                 paramElements[0].decodeAsOctetString().getStringValue();
            String value =
                 paramElements[1].decodeAsOctetString().getStringValue();

            Parameter p = parameters.getParameter(name);
            if (p != null)
            {
              p.setValueFromString(value);
            }
          }
        }
        else if (elementName.equals(ELEMENT_ACTUAL_START_TIME))
        {
          String timeStr = elements[i+1].decodeAsOctetString().getStringValue();
          actualStartTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_ACTUAL_STOP_TIME))
        {
          String timeStr = elements[i+1].decodeAsOctetString().getStringValue();
          actualStopTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_STOP_REASON))
        {
          stopReason = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_COMMENTS))
        {
          comments = elements[i+1].decodeAsOctetString().getStringValue();
        }
      }

      OptimizingJob optimizingJob =
           new OptimizingJob(slamdServer, optimizingJobID,
                             optimizationAlgorithm, jobClass, folderName,
                             description, includeThreadsInDescription,
                             startTime, duration, delayBetweenIterations,
                             numClients, requestedClients, monitorClients,
                             monitorClientsIfAvailable, minThreads, maxThreads,
                             threadIncrement, collectionInterval,
                             maxNonImproving, notifyAddresses,
                             reRunBestIteration, reRunDuration, parameters,
                             displayInReadOnlyMode);

      if (! (jobClass instanceof UnknownJobClass))
      {
        optimizationAlgorithm.initializeOptimizationAlgorithm(optimizingJob,
                                   optimizationParameters);
      }

      optimizingJob.setActualStartTime(actualStartTime);
      optimizingJob.setActualStopTime(actualStopTime);
      optimizingJob.setAssociatedJobs(iterations);
      optimizingJob.setDependencies(dependencies);
      optimizingJob.setJobState(jobState);
      optimizingJob.setJobGroup(jobGroup);
      optimizingJob.setReRunIteration(reRunIteration);
      optimizingJob.setStopReason(stopReason);
      optimizingJob.setThreadStartupDelay(threadStartupDelay);
      optimizingJob.setComments(comments);

      optimizingJob.pauseRequested = pauseRequested;

      return optimizingJob;
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode optimizing job:  " + e, e);
    }
  }



  /**
   * Decodes the provided byte array as an optimizing job, but only including
   * summary information.
   *
   * @param  slamdServer           The SLAMD server with which the optimizing
   *                               job is associated.
   * @param  encodedOptimizingJob  The byte array containing the encoded
   *                               optimizing job.
   *
   * @return  The summary optimizing job decoded from the provided byte array.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the provided byte array as an optimizing job.
   */
  public static OptimizingJob decodeSummary(SLAMDServer slamdServer,
                                            byte[] encodedOptimizingJob)
         throws DecodeException
  {
    try
    {
      boolean               displayInReadOnlyMode       = false;
      Date                  actualStartTime             = null;
      Date                  startTime                   = null;
      int                   jobState                    = -1;
      JobClass              jobClass                    = null;
      String                description                 = null;
      String                folderName                  = null;
      String                optimizingJobID             = null;

      SimpleDateFormat dateFormat  =
           new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

      ASN1Element   element  = ASN1Element.decode(encodedOptimizingJob);
      ASN1Element[] elements = element.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_OPTIMIZING_JOB_ID))
        {
          optimizingJobID =
               elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_FOLDER_NAME))
        {
          folderName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          String jobClassName =
               elements[i+1].decodeAsOctetString().getStringValue();
          jobClass = slamdServer.getOrLoadJobClass(jobClassName);
        }
        else if (elementName.equals(ELEMENT_JOB_STATE))
        {
          jobState = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          description = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DISPLAY_IN_READ_ONLY))
        {
          displayInReadOnlyMode =
               elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_START_TIME))
        {
          String timeStr = elements[i+1].decodeAsOctetString().getStringValue();
          startTime = dateFormat.parse(timeStr);
        }
        else if (elementName.equals(ELEMENT_ACTUAL_START_TIME))
        {
          String timeStr = elements[i+1].decodeAsOctetString().getStringValue();
          actualStartTime = dateFormat.parse(timeStr);
        }
      }

      OptimizingJob optimizingJob =
           new OptimizingJob(slamdServer, optimizingJobID, null, jobClass,
                             folderName, description, false, startTime, -1, 0,
                             -1, null, null, false, 1, -1, 1, 1, 1, null, false,
                             -1, null, displayInReadOnlyMode);
      optimizingJob.setActualStartTime(actualStartTime);
      optimizingJob.setJobState(jobState);
      return optimizingJob;
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode optimizing job:  " + e, e);
    }
  }



  /**
   * Compares this optimizing job with the provided object to determine the
   * relative order of the two in a sorted list.  The comparison will be based
   * first by start time, then by optimizing job ID.
   *
   * @param  o  The object to compare with this optimizing job.  It must be an
   *            OptimizingJob.
   *
   * @return  A negative value if this optimizing job should be ordered before
   *          the provided object, a positive value if this optimizing job
   *          should be ordered after the provided object, or zero if there is
   *          no difference in ordering.
   *
   * @throws  ClassCastException  If the provided object is not an
   *                              OptimizingJob.
   */
  public int compareTo(Object o)
          throws ClassCastException
  {
    if (o == null)
    {
      return -1;
    }

    String optimizingJobID2 = "";

    try
    {
      OptimizingJob oj = (OptimizingJob) o;
      optimizingJobID2 = oj.getOptimizingJobID();

      StringTokenizer t1 = new StringTokenizer(optimizingJobID, "-");
      StringTokenizer t2 = new StringTokenizer(optimizingJobID2, "-");


      // Get the timestamp string.  If they differ, then use the
      // String.compareTo method.
      String date1 = t1.nextToken();
      String date2 = t2.nextToken();
      if (! date1.equals(date2))
      {
        return date1.compareTo(date2);
      }


      // Get the random + counter portion.  If they differ, then compare the
      // numeric counter portions.
      String counterStr1 = t1.nextToken();
      String counterStr2 = t2.nextToken();
      if (! counterStr1.equals(counterStr2))
      {
        Integer counter1 = new Integer(counterStr1.substring(6));
        Integer counter2 = new Integer(counterStr2.substring(6));
        return counter1.compareTo(counter2);
      }


      // We shouldn't get this far for an optimizing job, so just use a string
      // comparison.
      return optimizingJobID.compareTo(optimizingJobID2);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      return optimizingJobID.compareTo(optimizingJobID2);
    }
  }
}

