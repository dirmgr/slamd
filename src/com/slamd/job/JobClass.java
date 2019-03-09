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



import java.util.ArrayList;
import java.util.Date;

import com.slamd.client.ClientSideJob;
import com.slamd.common.Constants;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;

import static com.slamd.admin.AdminServlet.slamdServer;



/**
 * This class implements a thread that may be executed as a SLAMD job.
 * Depending on the job configuration, multiple instances of this thread may
 * operate concurrently on the same machine or across multiple machines.
 * Anyone that wishes to implement their own custom job for SLAMD should only
 * need to extend this class.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class JobClass
       extends Thread
{
  // Indicates whether the job is currently running
  private boolean isRunning;

  // Indicates whether a request has been made to stop the job
  private boolean stopRequested;

  // The client number associated with this job class.
  private int clientNumber;

  // The collection interval that should be used when gathering statistics.
  private int collectionInterval;

  // The length of time in seconds that this job should be allowed to run
  private int duration;

  // The job state that should be assigned to the job if shouldStop returns
  // true.
  private int shouldStopReason;

  // The thread number associated with this job thread.
  private int threadNumber;

  // Indicates the time at which the job actually started running
  private long actualStartTime;

  // Indicates the time at which the job actually stopped running.
  private long actualStopTime;

  // Indicates the time at which the job should stop running
  private long shouldStopTime;

  // The time the job should start running in milliseconds.
  private long startTimeMillis;

  // The list of parameters that customize the way that this job is to work
  private ParameterList parameters;

  // The parent job that is controlling this thread
  private ClientSideJob job;

  // The client ID that indicates the client running this job.
  private String clientID;

  // The thread ID that will be used to identify this thread to the parent job
  private String threadID;



  /**
   * Creates a new job thread that is not running and that has not been
   * requested to stop.  This should be invoked by the constructor of all
   * subclasses.
   */
  protected JobClass()
  {
    // No implementation is required.  All real work is done in the
    // initializeJobThread and initialize methods.
  }



  /**
   * Retrieves the name of the job performed by this job thread.
   *
   * @return  The name of the job performed by this job thread.
   */
  public abstract String getJobName();



  /**
   * Retrieves a short description of the job performed by this job thread.
   *
   * @return  A short description of the job performed by this job thread.
   */
  public abstract String getShortDescription();



  /**
   * Retrieves a long description of the job performed by this job thread.  Each
   * element of the array returned will be treated as a separate paragraph in
   * the administrative interface.
   *
   * @return  A long description of the job performed by this job thread.
   */
  public String[] getLongDescription()
  {
    return new String[] { getShortDescription() };
  }



  /**
   * Retrieves the name of the category in which this job class exists.  This is
   * used to help arrange the job classes in the administrative interface.
   *
   * @return  The name of the category in which this job class exists.
   */
  public String getJobCategoryName()
  {
    return null;
  }



  /**
   * Provides a means for job classes to have a level of control over the
   * number of clients that will be used to run a job.  If a job class
   * implements this method and returns a value greater than 0, then that number
   * of clients will always be used to run the job, and the form allowing the
   * user to schedule a job will not show the "Number of Clients" field.  By
   * default, the user will be allowed to choose the number of clients.
   *
   * @return  The number of clients that should be used to run this job, or -1
   *          if the user should be allowed to specify the number of clients.
   */
  public int overrideNumClients()
  {
    return -1;
  }



  /**
   * Provides a means for job classes to have a level of control over the
   * number of threads per client that will be used to run a job.  If a job
   * class implements this method and returns a value greater than 0, then that
   * number of threads per client will always be used to run the job, and the
   * form allowing the user to schedule a job will not show the "Threads per
   * Client" field.  By default, the user will be allowed to choose the number
   * of threads per client.
   *
   * @return  The number of threads per client that should be used to run this
   *          job, or -1 if the user should be allowed to specify the number
   *          of threads per client.
   */
  public int overrideThreadsPerClient()
  {
    return -1;
  }



  /**
   * Provides a means for job classes to have a level of control over the
   * statistics collection interval that will be used for a job.  If a job class
   * implements this method and returns a value greater than 0, then that
   * collection interval will always be used to run the job, and the form
   * that allows the user to schedule a job will not show the "Statistics
   * Collection Interval" field.  By default, the user will be allowed to choose
   * the statistics collection interval.
   *
   * @return  The collection interval that should be used for this job, or -1 if
   *          the user should be allowed to specify the collection interval.
   */
  public int overrideCollectionInterval()
  {
    return -1;
  }



  /**
   * Retrieves a list of parameter "stubs" that should be used to indicate
   * which parameters should be passed to the initialize method.  These stubs
   * do not need to have values (although they can be given default values).
   *
   * @return  A list of the parameter stubs that can be used to determine which
   *          parameters must/may be provided to the initialize method.
   */
  public abstract ParameterList getParameterStubs();



  /**
   * Retrieves a list of parameter "stubs" that specify all the parameters that
   * should be available to clients.  This will generally be exactly the same
   * as the list returned by the <CODE>getParameterStubs</CODE> method, but in
   * some cases the list of parameters scheduled in the configuration directory
   * may differ a little (e.g., to be able to handle dynamically-generated
   * parameters created at the time the job is scheduled).
   *
   * @return  A list of parameter "stubs" that specify all the parameters that
   *          should be available to clients.
   */
  public ParameterList getClientSideParameterStubs()
  {
    return getParameterStubs();
  }



  /**
   * Retrieves the set of stat trackers that will be maintained by this job
   * class.  The stat trackers returned by this method do not have to actually
   * contain any statistics -- the display name and stat tracker class should
   * be the only information that callers of this method should rely upon.  Note
   * that this list can be different from the list of statistics actually
   * collected by the job in some cases (e.g., if the job may not return all the
   * stat trackers it advertises in all cases, or if the job may return stat
   * trackers that it did not advertise), but it is a possibility that only the
   * stat trackers returned by this method will be accessible for some features
   * in the SLAMD server.
   *
   * @param  clientID            The client ID that should be used for the
   *                             returned stat trackers.
   * @param  threadID            The thread ID that should be used for the
   *                             returned stat trackers.
   * @param  collectionInterval  The collection interval that should be used for
   *                             the returned stat trackers.
   *
   * @return  The set of stat trackers that will be maintained by this job
   *          class.
   */
  public abstract StatTracker[] getStatTrackerStubs(String clientID,
                                                    String threadID,
                                                    int collectionInterval);



  /**
   * Retrieves the stat trackers that are maintained for this job thread.
   *
   * @return  The stat trackers that are maintained for this job thread.
   */
  public abstract StatTracker[] getStatTrackers();



  /**
   * Retrieves the client number associated with this job thread.  This provides
   * a rudimentary capability for a job to have different behaviors on different
   * clients.  The first client that receives a job request will be client 0,
   * the next will be client 1, etc.
   *
   * @return  The client number associated with this job thread.
   */
  public int getClientNumber()
  {
    return clientNumber;
  }



  /**
   * Specifies the client number to use for this job thread.
   *
   * @param  clientNumber  The client number to use for this job thread.
   */
  public void setClientNumber(int clientNumber)
  {
    this.clientNumber = clientNumber;
  }



  /**
   * Retrieves the thread number associated with this job thread.  Note that
   * this value is the thread number for this client.  Other clients will have
   * a thread with the same thread number.
   *
   * @return  The thread number associated with this job thread.
   */
  public int getThreadNumber()
  {
    return threadNumber;
  }



  /**
   * Specifies the thread number for this job thread.
   *
   * @param  threadNumber  The thread number to use for this job thread.
   */
  public void setThreadNumber(int threadNumber)
  {
    this.threadNumber = threadNumber;
  }



  /**
   * Provides a means of validating the information used to schedule the job,
   * including the scheduling information and list of parameters.
   *
   * @param  numClients          The number of clients that should be used to
   *                             run the job.
   * @param  threadsPerClient    The number of threads that should be created on
   *                             each client to run the job.
   * @param  threadStartupDelay  The delay in milliseconds that should be used
   *                             when starting the client threads.
   * @param  startTime           The time that the job should start running.
   * @param  stopTime            The time that the job should stop running.
   * @param  duration            The maximum length of time in seconds that the
   *                             job should be allowed to run.
   * @param  collectionInterval  The collection interval that should be used
   *                             when gathering statistics for the job.
   * @param  parameters          The set of parameters provided to this job that
   *                             can be used to customize its behavior.
   *
   * @throws  InvalidValueException  If the provided information is not
   *                                 appropriate for running this job.
   */
  public void validateJobInfo(int numClients, int threadsPerClient,
                              int threadStartupDelay, Date startTime,
                              Date stopTime, int duration,
                              int collectionInterval, ParameterList parameters)
         throws InvalidValueException
  {
    // No implementation required by default.
  }



  /**
   * Indicates whether this job class implements logic that makes it possible to
   * test the validity of job parameters before scheduling the job for execution
   * (e.g., to see if the server is reachable using the information provided).
   * By default, this method returns <CODE>false</CODE> to indicate that this is
   * not provided, but actual implementations may override this method to
   * return <CODE>true</CODE> and should also override the
   * <CODE>testJobParameters</CODE> method to implement the actual test.
   *
   * @return  <CODE>true</CODE> if this job provides a means of testing the job
   *          parameters, or <CODE>false</CODE> if not.
   */
  public boolean providesParameterTest()
  {
    return false;
  }



  /**
   * Provides a means of testing the provided job parameters to determine
   * whether they are valid (e.g., to see if the server is reachable) before
   * scheduling the job for execution.  This method will be executed by the
   * SLAMD server system itself and not by any of the clients.
   *
   * @param  parameters      The job parameters to be tested.
   * @param  outputMessages  The lines of output that were generated as part of
   *                         the testing process.  Each line of output should
   *                         be added to this list as a separate string, and
   *                         empty strings (but not <CODE>null</CODE> values)
   *                         are allowed to provide separation between
   *                         different messages.  No formatting should be
   *                         provided for these messages, however, since they
   *                         may be displayed in either an HTML or plain text
   *                         interface.
   *
   * @return  <CODE>true</CODE> if the test completed successfully, or
   *          <CODE>false</CODE> if not.  Note that even if the test did not
   *          complete successfully, the user will be presented with a warning
   *          but will still be allowed to schedule the job using the provided
   *          parameters.  This is necessary because the parameters may still be
   *          valid even if the server couldn't validate them at the time the
   *          job was scheduled (e.g., if the server wasn't running or could not
   *          be reached by the SLAMD server even though it could be by the
   *          clients).
   */
  public boolean testJobParameters(ParameterList parameters,
                                   ArrayList<String> outputMessages)
  {
    outputMessages.add("No parameter tests have been defined for " +
                       getJobName() + " jobs.");

    return false;
  }



  /**
   * Indicates whether this job class is deprecated.  If it is deprecated, then
   * a warning will be displayed whenever the user attempts to schedule the job.
   * A job that is deprecated will not behave any differently, but merely serves
   * as a warning that a better option may exist for the same type of test.
   *
   * @param  message  The buffer to which a message may be appended that
   *                  provides additional information about the deprecation.
   *
   * @return  <CODE>true</CODE> if this job class has been deprecated, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isDeprecated(StringBuilder message)
  {
    return false;
  }



  /**
   * Performs a one-time initialization for this job.  This initialization is
   * performed on the SLAMD server immediately before the job is sent to the
   * clients for processing.  Note that the work performed by this method should
   * only be in the form of interaction with something external.  Changes to
   * instance variables or other items internal to this method will not be
   * reflected on clients.
   *
   * @param  parameters  The set of parameters provided to this job that can be
   *                     used to customize its behavior.
   *
   * @throws  UnableToRunException  If the job initialization could not be
   *                                completed successfully and the job is unable
   *                                to run.
   */
  public void initializeJob(ParameterList parameters)
         throws UnableToRunException
  {
    // No implementation required by default.
  }



  /**
   * Performs initialization for this job on each client immediately before each
   * thread is created to actually run the job.  Note that if any changes are to
   * be made to variables that should be available to the individual threads,
   * then those variables should be declared as static.
   *
   * @param  clientID    The ID assigned to the client running this job.
   * @param  parameters  The set of parameters provided to this job that can be
   *                     used to customize its behavior.
   *
   * @throws  UnableToRunException  If the client initialization could not be
   *                                completed successfully and the job is unable
   *                                to run.
   */
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    // No implementation required by default.
  }



  /**
   * Initializes this job thread to be used to actually run the job on the
   * client.  The provided parameter list should be processed to customize the
   * behavior of this job thread, and any other initialization that needs to be
   * done in order for the job to run should be performed here as well (e.g.,
   * creating and initializing the status counters).
   *
   * @param  clientID            The client ID for this job thread.
   * @param  threadID            The thread ID for this job thread.
   * @param  collectionInterval  The length of time in seconds to use as the
   *                             statistics collection interval.
   * @param  parameters          The set of parameters provided to this job that
   *                             can be used to customize its behavior.
   *
   * @throws  UnableToRunException  If the thread initialization could not be
   *                                completed successfully and the job is unable
   *                                to run.
   */
  public abstract void initializeThread(String clientID, String threadID,
                                        int collectionInterval,
                                        ParameterList parameters)
         throws UnableToRunException;



  /**
   * The method that does all the real work for this job.  It performs the task
   * associated with the job thread based on the information provided through
   * the parameter list passed into the <CODE>initialize</CODE> method.
   */
  public abstract void runJob();



   /**
    * Retrieves the number of threads that are currently active for this job.
    *
    * @return  The number of threads that are currently active for this job.
    */
   public int getActiveThreadCount()
   {
     return job.getActiveThreadCount();
   }



  /**
   * Performs any per-thread finalization that should be done for this job.  By
   * default, no action is performed.
   */
  public void finalizeThread()
  {
    // No implementation required by default.
  }



  /**
   * Performs any per-client finalization that should be done for this job.  By
   * default, no action is performed.
   */
  public void finalizeClient()
  {
    // No implementation required by default.
  }



  /**
   * Performs any per-job finalization that should be done for this job.  By
   * default, no action is performed.
   */
  public void finalizeJob()
  {
    // No implementation required by default.
  }



  /**
   * Provides the job thread with all of the information that it needs to run.
   * This should be extended by actual job thread implementations, but the
   * version in this method should always be invoked first using
   * <CODE>super.initialize(threadID, job, parameters)</CODE> before any
   * custom code.
   *
   * @param  clientID            The client ID that will be used to identify the
   *                             client to the parent job.
   * @param  threadID            The thread ID that will be used to identify
   *                             this thread to the parent job.
   * @param  collectionInterval  The interval in seconds that should be used
   *                             when gathering statistics during job
   *                             processing.
   * @param  job                 The parent job that is controlling this thread.
   * @param  duration            The length of time in seconds that the job
   *                             should be allowed to run.  A value of -1
   *                             indicates an unlimited duration.
   * @param  stopTime            The time at which the job should stop running.
   *                             A value of <CODE>null</CODE> indicates no
   *                             specified stop time.
   * @param  startTimeMillis     The time this thread should start running in
   *                             milliseconds since January 1, 1970.
   * @param  parameters          The list of parameters (containing values) that
   *                             should be used to control the operation of this
   *                             job.
   *
   * @throws  UnableToRunException  If a problem occurs during initialization.
   */
  public final void initializeJobThread(String clientID, String threadID,
                                        int collectionInterval,
                                        ClientSideJob job, int duration,
                                        Date stopTime, long startTimeMillis,
                                        ParameterList parameters)
         throws UnableToRunException
  {
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.collectionInterval = collectionInterval;
    this.job                = job;
    this.duration           = duration;
    this.startTimeMillis    = startTimeMillis;
    this.parameters         = parameters;
    this.shouldStopReason   = Constants.JOB_STATE_STOPPED_BY_USER;

    if (stopTime == null)
    {
      shouldStopTime = -1;
    }
    else
    {
      shouldStopTime   = stopTime.getTime();
      shouldStopReason = Constants.JOB_STATE_STOPPED_DUE_TO_STOP_TIME;
    }

    isRunning     = false;
    stopRequested = false;

    try
    {
      this.initializeThread(clientID, threadID, collectionInterval, parameters);
    }
    catch (Exception e)
    {
      isRunning = false;
      stopRequested = true;
      shouldStopReason = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      logMessage("Job thread initialization failed:  " + stackTraceToString(e));
      throw new UnableToRunException("Job thread initialization failed:  " +
                                     stackTraceToString(e));
    }
  }



  /**
   * Retrieves the client side job associated with this job class.
   *
   * @return  The client side job associated with this job class.
   */
  public final ClientSideJob getClientSideJob()
  {
    return job;
  }



  /**
   * Sets the client-side job that should be associated with this job class.
   *
   * @param  job  The client-side job that should be associated with this job
   *              class.
   */
  public final void setClientSideJob(ClientSideJob job)
  {
    this.job = job;
  }



  /**
   * Retrieves the job ID of the job with which this job thread is currently
   * associated.
   *
   * @return  The job ID of the job with which this job thread is currently
   *          associated, or <CODE>null</CODE> if this thread is not associated
   *          with any job.
   */
  public String getJobID()
  {
    if (job == null)
    {
      return null;
    }

    return job.getJobID();
  }



  /**
   * Retrieves the client ID for this job thread.
   *
   * @return  The client ID for this job thread.
   */
  public final String getClientID()
  {
    return clientID;
  }



  /**
   * Retrieves the thread ID for this job thread.
   *
   * @return  The thread ID for this job thread.
   */
  public final String getThreadID()
  {
    return threadID;
  }



  /**
   * Retrieves the collection interval for this job thread.
   *
   * @return  The collection interval for this job thread.
   */
  public final int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Retrieves the scheduled duration for this job thread.
   *
   * @return  The scheduled duration for this job thread.
   */
  public final int getScheduledDuration()
  {
    return duration;
  }



  /**
   * Retrieves the time at which this job should stop, either because of the
   * duration or the stop time.
   *
   * @return  The time at which this job should stop, either because of the
   *          duration or the stop time.
   */
  public final long getShouldStopTime()
  {
    return shouldStopTime;
  }



  /**
   * Indicates that the job should start running.  It performs some necessary
   * administrative work and then starts the job thread.  This may not be
   * overridden by subclasses, so any work that is intended to be done before
   * the job actually starts should be done in the <CODE>initialize</CODE>
   * method.
   *
   * @throws  AlreadyRunningException  If this job thread is already running.
   */
  public final void startJob()
         throws AlreadyRunningException
  {
    // Make sure that the thread is not already running
    if (isRunning)
    {
      throw new AlreadyRunningException("Thread " + threadID +
                                             " is already running");
    }
    else
    {
      // If the initialization failed, then the stop reason will indicate that
      // it was stopped because of an error.  If that's the case, then don't
      // try to run.
      if (shouldStopReason != Constants.JOB_STATE_STOPPED_DUE_TO_ERROR)
      {
        start();
      }
    }
  }



  /**
   * Requests that the job stop running at the earliest convenient time.  There
   * is no guarantee that the job has actually stopped running by the time that
   * this method returns.
   *
   * @param  stopReason  The reason that the job should stop running.
   */
  public final void stopJob(int stopReason)
  {
    stopRequested = true;
    shouldStopReason = stopReason;
  }



  /**
   * Requests that the job stop running at the earliest convenient time, and
   * then waits until the job has stopped completely.  When this method returns,
   * it is safe to assume that the job is no longer running.
   *
   * @param  stopReason  The reason that the job should stop running.
   */
  public final void stopAndWait(int stopReason)
  {
    stopRequested = true;
    shouldStopReason = stopReason;

    while (isRunning)
    {
      try
      {
        Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
      }
      catch (InterruptedException ie) {}
    }
  }



  /**
   * Indicates whether the job is currently running.
   *
   * @return  <CODE>true</CODE> if the job is running, or <CODE>false</CODE> if
   *          it is not.
   */
  public final boolean isRunning()
  {
    return isRunning;
  }



  /**
   * Indicates whether a request has been made to stop the job from an external
   * source (e.g., from a call to the <CODE>stopJob</CODE> method).  Note that
   * there may be other reasons that the job should stop running (for example,
   * if a time limit has been set and has been reached).  Therefore, the
   * <CODE>shouldStop</CODE> method should be used to determine whether the job
   * should stop processing altogether.
   *
   * @return  <CODE>true</CODE> if a request has been made to stop this job, or
   *          <CODE>false</CODE> if not.
   */
  public final boolean stopRequested()
  {
    return stopRequested;
  }



  /**
   * Indicates whether the job should stop processing for any reason.  This
   * should be checked periodically from the <CODE>runJob</CODE> method to
   * determine whether it is time to stop running.
   *
   * @return  <CODE>true</CODE> if the job should stop running, or
   *          <CODE>false</CODE> if there is no reason for it to stop.
   */
  public final boolean shouldStop()
  {
    if (stopRequested)
    {
      if (shouldStopReason == Constants.JOB_STATE_STOPPED_BY_SHUTDOWN)
      {
        job.setJobState(shouldStopReason);
      }
      else
      {
        job.setJobState(Constants.JOB_STATE_STOPPED_BY_USER);
      }
      return true;
    }

    if ((shouldStopTime > 0) && (System.currentTimeMillis() > shouldStopTime))
    {
      job.setJobState(shouldStopReason);
      return true;
    }

    return false;
  }



  /**
   * Indicates whether real-time statistics collection has been enabled in the
   * client with which this job is associated.
   *
   * @return  <CODE>true</CODE> if the associated client has enabled real-time
   *          statistics collection, or <CODE>false</CODE> if not.
   */
  public final boolean enableRealTimeStats()
  {
    if (job == null)
    {
      return false;
    }
    else
    {
      return job.enableRealTimeStats();
    }
  }



  /**
   * Retrieves the real-time stat reporter that should be used for this job.
   *
   * @return  The real-time stat reporter that should be used for this job, or
   *          <CODE>null</CODE> if none should be used.
   */
  public final RealTimeStatReporter getStatReporter()
  {
    if (job == null)
    {
      return null;
    }
    else
    {
      return job.getStatReporter();
    }
  }



  /**
   * Prevent the deprecated destroy method from being used by subclasses.
   */
  @Override()
  @SuppressWarnings("deprecation")
  public final void destroy()
  {
    // No implementation required.
  }



  /**
   * Provides a means for destroying this job thread if it does not respond
   * to a graceful shutdown request.  The default implementation does nothing,
   * but an actual job class may wish to do something like close a connection to
   * a remote server that could cause the thread to terminate.
   */
  public void destroyThread()
  {
    // No implementation provided by default.
  }



  /**
   * Indicates that this job thread has completed, but there were errors that
   * may impact how the results should be interpreted.
   */
  public final void indicateCompletedWithErrors()
  {
    job.setJobState(Constants.JOB_STATE_COMPLETED_WITH_ERRORS);
  }



  /**
   * Indicates that this job thread has been stopped because of an unrecoverable
   * error.
   */
  public final void indicateStoppedDueToError()
  {
    job.setJobState(Constants.JOB_STATE_STOPPED_DUE_TO_ERROR);
  }



  /**
   * Writes a message to the client message writer so that if it is running in
   * verbose mode this information will be available to that client.  This
   * message will not be displayed if the client is not operating in verbose
   * mode, nor will it be sent back to the SLAMD server to be included in the
   * log.
   *
   * @param  message  The message to be written.
   */
  public final void writeVerbose(String message)
  {
    if (job == null)
    {
      // If the SLAMD server is available, then use it to log the message.
      // Otherwise, just print it to standard output.
      try
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG, message);
      }
      catch (Exception e)
      {
        System.err.println(message);
      }
    }
    else
    {
      job.writeVerbose(message);
    }
  }



  /**
   * Sends a status message regarding the progress of this job thread to the
   * parent job so that it can be recorded in the SLAMD logger.  It will be
   * logged using a job status log level (which is enabled by default), so only
   * significant events (e.g., exceptions) should be announced using this
   * method.  Less significant events should be logged using the
   * <CODE>logVerboseStatusMessage</CODE> method.
   *
   * @param  message  The message to be logged.
   */
  public final void logMessage(String message)
  {
    if (job == null)
    {
      // If the SLAMD server is available, then use it to log the message.
      // Otherwise, just print it to standard error.
      try
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING, message);
      }
      catch (Exception e)
      {
        System.err.println(message);
      }
    }
    else
    {
      job.logMessage(threadID, message);
    }
  }



  /**
   * A wrapper for the <CODE>runJob</CODE> method (which actually does the real
   * work for this job thread).  It takes care of setting a flag used to
   * determine if the job is currently running or not, and will properly unset
   * it when the job is no longer running, even if it stopped running because of
   * an uncaught exception.  This method may not be overridden by subclasses.
   */
  @Override()
  public final void run()
  {
    // Sleep until it is time for the job to actually start.
    while ((! shouldStop()) && (System.currentTimeMillis() < startTimeMillis))
    {
      try
      {
        Thread.sleep(10);
      } catch (InterruptedException ie) {}
    }
    writeVerbose("Starting thread " + threadID);

    // Get the current time and mark it as the start time.
    actualStartTime = System.currentTimeMillis();

    // Figure out when the job should stop.  If a duration was specified but
    // no stop time, then make the stop time (now + duration).  If a stop
    // time was specified but no duration, then just use the stop time.  If
    // both a stop time and a duration have been specified, then pick the
    // one that will come first.
    if ((duration > 0) && ((shouldStopTime <= 0) ||
                           (shouldStopTime > (actualStartTime + duration))))
    {
      shouldStopTime   = actualStartTime + (1000 * duration);
      shouldStopReason = Constants.JOB_STATE_STOPPED_DUE_TO_DURATION;
    }


    // Indicate that the job is actually running.
    isRunning = true;

    try
    {
      // Run the job.  This will not complete until the job is done or has
      // crashed and burned.
      if (! shouldStop())
      {
        runJob();
        if (job.getJobState() == Constants.JOB_STATE_RUNNING)
        {
          job.setJobState(Constants.JOB_STATE_COMPLETED_SUCCESSFULLY);
        }
      }

      // Run the per-thread finalization.
      finalizeThread();
    }
    catch (Exception e)
    {
      // If a problem occurred with the job, then this should take care of it
      // rather than passing the exception up the line and causing all kinds of
      // problems.
      job.logMessage(threadID, "Uncaught exception \"" + e +
                     "\" -- Stack Trace:  " + stackTraceToString(e));
      e.printStackTrace();
      job.setJobState(Constants.JOB_STATE_STOPPED_DUE_TO_ERROR);
    }
    finally
    {
      // Indicate that the job isn't running anymore.
      actualStopTime = System.currentTimeMillis();
      isRunning = false;
      job.threadDone(this);
    }
  }



  /**
   * Retrieves a string representation of the stack trace contained in the
   * provided exception.
   *
   * @param  t  The exception for which to retrieve the stack trace.
   *
   * @return  The string representation
   */
  public static String stackTraceToString(Throwable t)
  {
    if (t == null)
    {
      return null;
    }

    String EOL = System.getProperty("line.separator");

    StringBuilder buffer = new StringBuilder();
    buffer.append(t.toString());
    buffer.append(EOL);

    String separator = "  ";
    StackTraceElement[] elements = t.getStackTrace();
    for (int i=0; i < elements.length; i++)
    {
      buffer.append(separator);
      buffer.append(elements[i].getClassName());
      buffer.append('.');
      buffer.append(elements[i].getMethodName());
      buffer.append('(');
      buffer.append(elements[i].getFileName());
      buffer.append(':');
      buffer.append(elements[i].getLineNumber());
      buffer.append(')');
      separator = EOL + "  ";
    }

    Throwable cause = t.getCause();
    if (cause != null)
    {
      buffer.append(EOL);
      buffer.append("Caused By:  ");
      buffer.append(cause.toString());

      elements = cause.getStackTrace();
      for (int i=0; i < elements.length; i++)
      {
        buffer.append(separator);
        buffer.append(elements[i].getClassName());
        buffer.append('.');
        buffer.append(elements[i].getMethodName());
        buffer.append('(');
        buffer.append(elements[i].getFileName());
        buffer.append(':');
        buffer.append(elements[i].getLineNumber());
        buffer.append(')');
        separator = EOL + "  ";
      }
    }

    return buffer.toString();
  }
}

