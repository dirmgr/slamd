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
package com.slamd.server;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sleepycat.je.DatabaseException;

import com.slamd.db.SLAMDDB;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizingJob;
import com.slamd.job.UnableToRunException;
import com.slamd.jobs.NullJobClass;
import com.slamd.common.Constants;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class acts as the job scheduler for the SLAMD server.  It allows jobs to
 * be submitted for processing (either immediately or at some point in the
 * future) and handles all of the work necessary to make sure that they get
 * started and stopped properly.
 *
 *
 * @author   Neil A. Wilson
 */
public final class Scheduler
       extends Thread
       implements ConfigSubscriber
{
  /**
   * The name used to register the logger as a subscriber to the configuration
   * handler.
   */
  public static final String CONFIG_SUBSCRIBER_NAME = "SLAMD Scheduler";



  /**
   * The maximum number of recently completed jobs to track.
   */
  public static final int MAX_RECENTLY_COMPLETED = 5;



  /**
   * A dummy job value that will be used to signal the scheduler thread that it
   * should wake up and check the stop flag or run through the queue.
   */
  private static Job WAKEUP_JOB = null;



  // Indicators about the state of the scheduler
  private final AtomicBoolean running;
  private final AtomicBoolean stopRequested;

  // The information used to generate job IDs
  private final AtomicInteger idCounter;
  private final Random           numberGenerator;
  private final SimpleDateFormat dateFormat;

  // The delay in milliseconds between iterations of the scheduler loop
  private int schedulerDelay;

  // The time in milliseconds before the job's actual start time that the job
  // requests should be sent to the clients.
  private int startBuffer;

  // A queue that will be used to provide new jobs to be scheduled.
  private final LinkedBlockingQueue<Job> toScheduleQueue;

  // A mutex that will be used to safely provide multithreaded access to the
  // scheduler
  private final Object schedulerMutex;

  // The client listener associated with the SLAMD server.
  private final ClientListener listener;

  // The resource monitor client listener associated with the SLAMD server.
  private final ResourceMonitorClientListener monitorListener;

  // The configuration database for the SLAMD server
  private final SLAMDDB configDB;

  // The SLAMD server with which this scheduler will be associated
  private final SLAMDServer slamdServer;

  // The collections that will be used to hold pending, running, and recently
  // completed jobs
  private final LinkedHashMap<String,Job> pendingJobs;
  private final LinkedHashMap<String,Job> runningJobs;
  private final ArrayList<Job> recentlyCompletedJobs;

  // A list that will be used to hold any optimizing jobs that might be
  // associated with pending or running jobs.
  private final HashMap<String,OptimizingJob> optimizingJobs;

  // Information used in retrieving scheduler status info.
  private final AtomicInteger cancelledCount;
  private final AtomicInteger completedCount;
  private final AtomicInteger scheduledCount;

  // The thread being used to run the scheduler.
  private volatile Thread schedulerThread;



  /**
   * Creates a new instance of the SLAMD scheduler.  Once this has completed,
   * the scheduler will allow new jobs to be submitted, but nothing will
   * actually be started until the scheduler itself is started.
   *
   * @param  slamdServer  The SLAMD server with which this scheduler is
   *                      associated.
   *
   * @throws  SLAMDServerException  If a problem occurs while initializing the
   *                                scheduler.
   */
  public Scheduler(final SLAMDServer slamdServer)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler constructor");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler constructor");

    setName("SLAMD Scheduler");


    try
    {
      WAKEUP_JOB = new Job(slamdServer, NullJobClass.class.getName());
    }
    catch (final Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(e));
    }


    // Initialize all of the instance variables
    this.slamdServer      = slamdServer;
    listener              = slamdServer.getClientListener();
    monitorListener       = slamdServer.getMonitorClientListener();
    configDB              = slamdServer.getConfigDB();
    running               = new AtomicBoolean(false);
    stopRequested         = new AtomicBoolean(false);
    idCounter             = new AtomicInteger(0);
    numberGenerator       = new Random();
    dateFormat            = new SimpleDateFormat(
                                     Constants.ATTRIBUTE_DATE_FORMAT);
    startBuffer           = Constants.DEFAULT_SCHEDULER_START_BUFFER;
    schedulerDelay        = Constants.DEFAULT_SCHEDULER_DELAY;
    schedulerMutex        = new Object();
    pendingJobs           = new LinkedHashMap<String,Job>();
    runningJobs           = new LinkedHashMap<String,Job>();
    recentlyCompletedJobs = new ArrayList<Job>();
    optimizingJobs        = new HashMap<String,OptimizingJob>();
    scheduledCount        = new AtomicInteger(0);
    cancelledCount        = new AtomicInteger(0);
    completedCount        = new AtomicInteger(0);
    toScheduleQueue       = new LinkedBlockingQueue<Job>();
    schedulerThread       = null;


    // Register as a subscriber of the configuration handler
    configDB.registerAsSubscriber(this);


    // Load any jobs that are marked as running in the configuration database.
    // They must have been running when the SLAMD server was abruptly stopped,
    // so update them to indicate that they were stopped due to shutdown
    try
    {
      final Job[] runningJobArray = configDB.getRunningJobs();
      for (int i=0;
           ((runningJobArray != null) && (i < runningJobArray.length));
           i++)
      {
        try
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
               "Updating job state for " + runningJobArray[i].getJobID() +
               " to indicate stopped by shutdown");
          runningJobArray[i].setJobState(
               Constants.JOB_STATE_STOPPED_BY_SHUTDOWN);
          configDB.writeJob(runningJobArray[i]);
        }
        catch (final DatabaseException de2)
        {
          // This isn't good, but the jobs won't be scheduled anyway.  Just log
          // the failure and go on
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
               "Unable to update job " + runningJobArray[i].getJobID() +
               " to indicate stopped due to shutdown:  " + de2);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(de2));
        }

        // If this job was part of an optimizing job, then update it to indicate
        // that it has been stopped by shutdown.
        final String optimizingJobID = runningJobArray[i].getOptimizingJobID();
        if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
        {
          try
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                 "Updating job state for optimizing job " + optimizingJobID +
                 " to indicate stopped by shutdown");
            final OptimizingJob optimizingJob =
                 configDB.getOptimizingJob(optimizingJobID);
            optimizingJob.setJobState(Constants.JOB_STATE_STOPPED_BY_SHUTDOWN);
            optimizingJob.setStopReason("Stopped by SLAMD server shutdown.");
            configDB.writeOptimizingJob(optimizingJob);
          }
          catch (final Exception e)
          {
            // Log the failure and go on
            slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                 "Unable to update optimizing job " + optimizingJobID +
                 " to indicate stopped due to shutdown:  " + e);
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(e));
          }
        }
      }
    }
    catch (final DatabaseException de)
    {
      // Also not good, but not fatal either.
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
           "Unable to get running job list from config database to mark as " +
           "stopped by shutdown:  " + de);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(de));
    }


    // Load any jobs that were disabled the last time the scheduler was running.
    // These need to be read before pending jobs because pending jobs may be
    // dependent on jobs that are disabled.  Being unable to read the disabled
    // job list is not an error from which we can recover, so propagate the
    // exception to the caller.
    try
    {
      final Job[] disabledJobArray = configDB.getDisabledJobs();
      for (int i=0;
           ((disabledJobArray != null) && (i < disabledJobArray.length));
           i++)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Adding disabled job " + disabledJobArray[i].getJobID() +
             " to pending queue");
        pendingJobs.put(disabledJobArray[i].getJobID(), disabledJobArray[i]);
      }
    }
    catch (final DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
           "Unable to retrieve the list of disabled jobs from the " +
           "configuration database:  " + de);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to retrieve the list of " +
           "disabled jobs from the configuration database:  " + de, de);
    }


    // Load any jobs that may have been scheduled for execution the last time
    // the scheduler was running.  Being unable to read the pending job list
    // is not a condition from which we can recover, so propagate the exception
    // to the caller.
    try
    {
      final Job[] pendingJobArray = configDB.getPendingJobs();
      for (int i=0;
           ((pendingJobArray != null) && (i < pendingJobArray.length));
           i++)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Adding job " + pendingJobArray[i].getJobID() +
             " to pending queue");
        pendingJobs.put(pendingJobArray[i].getJobID(), pendingJobArray[i]);
      }
    }
    catch (final DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
           "Unable to retrieve the list of pending jobs from the " +
           "configuration database:  " + de);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to retrieve the list of " +
           "pending jobs from the configuration database:  " + de, de);
    }


    // Look through the pending queue and see if any of the jobs are associated
    // with optimizing jobs.  If so, then load them.
    for (final Job job : pendingJobs.values())
    {
      final String optimizingID = job.getOptimizingJobID();
      if (optimizingID != null)
      {
        try
        {
          final OptimizingJob optimizingJob =
               configDB.getOptimizingJob(optimizingID);
          if (optimizingJob != null)
          {
            optimizingJobs.put(optimizingID, optimizingJob);
          }
        }
        catch (final Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
               "Unable to load the optimizing job associated with job " +
               job.getJobID() + ":  " + e);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(e));
          throw new SLAMDServerException("Unable to load the optimizing job " +
               "associated with job " + job.getJobID() + ":  " + e, e);
        }
      }
    }


    // Determine how much before the job's start time that the job information
    // should be sent to clients.
    final String startBufferStr =
         configDB.getConfigParameter(Constants.PARAM_SCHEDULER_START_BUFFER);
    if (startBufferStr != null)
    {
      try
      {
        startBuffer = 1000 * Integer.parseInt(startBufferStr);
      }
      catch (final NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
             "Config parameter " + Constants.PARAM_SCHEDULER_START_BUFFER +
             " requires a numeric value");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(nfe));
      }
    }


    // Determine how much time in milliseconds should pass between iterations
    // through the scheduler loop
    final String delayStr =
         configDB.getConfigParameter(Constants.PARAM_SCHEDULER_DELAY);
    if (delayStr != null)
    {
      try
      {
        schedulerDelay = Integer.parseInt(delayStr);
      }
      catch (final NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
             "Config parameter " + Constants.PARAM_SCHEDULER_DELAY +
             " requires a numeric value");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(nfe));
      }
    }


    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "Leaving Scheduler constructor");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "Leaving Scheduler constructor");
  }



  /**
   * Starts the scheduler so that it can dispatch jobs to clients.
   */
  public void startScheduler()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.startScheduler()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.startScheduler()");


    if (! running.get())
    {
      start();
    }
  }



  /**
   * Stops the scheduler so that it will no longer dispatch new jobs to clients.
   * This will not stop any jobs that are currently in progress, nor will it
   * clear the queue of pending jobs.
   */
  public void stopScheduler()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.stopScheduler()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.stopScheduler()");

    stopRequested.set(true);
    toScheduleQueue.offer(WAKEUP_JOB);
  }



  /**
   * This method will not return until the scheduler has actually stopped.  Note
   * that it does not stop the scheduler -- you should first call the
   * {@code stopScheduler} method to signal the scheduler that it needs to
   * stop.
   */
  public void waitForStop()
  {
    final Thread t = schedulerThread;
    if (t != null)
    {
      try
      {
        t.join();
      }
      catch (final Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(e));
      }
    }
  }



  /**
   * Submits a new job for processing and returns the job ID associated with
   * that job.
   *
   * @param  job         The SLAMD job to be scheduled for processing.
   * @param  folderName  The name of the folder in which the job should be
   *                     placed.
   *
   * @return  The job ID associated with the scheduled job.
   *
   * @throws  SLAMDServerException  If a problem is encountered while scheduling
   *                                the job for execution.
   */
  public String scheduleJob(final Job job, final String folderName)
         throws SLAMDServerException
  {
    // First, check to see if the job has a job ID.  If not, then give it one.
    String jobID = job.getJobID();
    if ((jobID == null) || (jobID.length() == 0))
    {
      jobID = generateUniqueID();
      job.setJobID(jobID);
    }


    // Not exactly the beginning of the method, but close enough
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.scheduleJob(" + job.getJobID() + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.scheduleJob(" + job.getJobID() + ')');


    // Next, make sure the job gets recorded in the configuration database so
    // that it will be automatically re-scheduled in case the SLAMD server is
    // stopped.
    job.setFolderName(folderName);
    try
    {
      configDB.writeJob(job);
    }
    catch (final DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to add job " + jobID + " to the " +
           "configuration database:  " + de, de);
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "Added job " + jobID + " info to config database.");

    // Finally, schedule it for execution
    try
    {
      toScheduleQueue.put(job);
    }
    catch (final Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(e));

      // This should never happen, but just in case we can manually insert it
      // into the pending jobs list.
      synchronized (schedulerMutex)
      {
        pendingJobs.put(job.getJobID(), job);
      }
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
         "Scheduled new " + job.getJobName() + " job (" + jobID +
         ") for processing");

    scheduledCount.incrementAndGet();
    return jobID;
  }



  /**
   * Submits a new optimizing job for processing.
   *
   * @param  optimizingJob  The SLAMD optimizing job to be scheduled for
   *                        processing.
   * @param  folderName     The name of the folder in which the optimizing job
   *                        should be placed.
   *
   * @throws  SLAMDServerException  If a problem is encountered while scheduling
   *                                the job for execution.
   */
  public void scheduleOptimizingJob(final OptimizingJob optimizingJob,
                                    final String folderName)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.scheduleOptimizingJob(" +
         optimizingJob.getOptimizingJobID() + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.scheduleOptimizingJob(" +
         optimizingJob.getOptimizingJobID() + ')');


    // Next, make sure the job gets recorded in the configuration database so
    // that it will be automatically re-scheduled in case the SLAMD server is
    // stopped.
    optimizingJob.setFolderName(folderName);
    try
    {
      configDB.writeOptimizingJob(optimizingJob);
    }
    catch (DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to add optimizing job " +
           optimizingJob.getOptimizingJobID() +
           " to the configuration database:  " + de, de);
    }
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "Added optimizing job " + optimizingJob.getOptimizingJobID() +
         " info to config database");


    // Add the optimizing job to the hash so we can find it later.
    optimizingJobs.put(optimizingJob.getOptimizingJobID(), optimizingJob);



    // Finally, schedule it for execution
    optimizingJob.schedule();
    slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
         "Scheduled new optimizing job (" + optimizingJob.getOptimizingJobID() +
         ") for processing");
  }



  /**
   * Cancels a job that has been submitted.  If the job has not yet been
   * started, then it will simply be removed from the queue of pending jobs.  If
   * it has already started, then it will be stopped and removed from the queue
   * of running jobs.  If it has already completed, then no action will be
   * taken.
   *
   * @param  jobID        Indicates which job should be stopped.
   * @param  waitForStop  Indicates that if the job is currently running,
   *                      whether the scheduler should wait for it to stop
   *                      before returning.
   *
   * @return  The SLAMD job that was cancelled, or {@code null} if no
   *          reference to that job could be found.
   */
  public Job cancelJob(final String jobID, final boolean waitForStop)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.cancelJob(" + jobID + ", " + waitForStop + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.cancelJob(" + jobID + ", " + waitForStop + ')');

    // If the job is currently running, then we need to keep track of it so we
    // can stop it before it gets returned.  We want to do the stop outside of
    // the mutex, though, for performance reasons.
    Job runningJob = null;

    synchronized (schedulerMutex)
    {
      final Job job = pendingJobs.remove(jobID);
      if (job != null)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Removed " + jobID + " from pending queue");
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
             "Pending job " + jobID + " cancelled by user");
        cancelledCount.incrementAndGet();
        try
        {
          job.setJobState(Constants.JOB_STATE_CANCELLED);
          final Date now = new Date();
          job.setActualStartTime(now);
          job.setActualStopTime(now);
          job.setActualDuration(0);
          configDB.writeJob(job);
          slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
               "Updated config database info for job " + jobID);
        }
        catch (final DatabaseException de)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
               "Could not update config database info for job " + jobID +
               " to indicate it was cancelled:  " + de);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(de));
        }

        final String optimizingJobID = job.getOptimizingJobID();
        if (optimizingJobID != null)
        {
          try
          {
            final OptimizingJob optimizingJob =
                 getOptimizingJob(optimizingJobID);
            if (optimizingJob != null)
            {
              optimizingJob.jobIterationComplete(job);
              optimizingJobs.remove(optimizingJobID);
            }
          }
          catch (final Exception e)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                 "Unable to update optimizing job information for completed " +
                 "job " + job.getJobID() + ":  " + e);
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(e));
          }
        }
        return job;
      }

      runningJob = runningJobs.get(jobID);
    }

    if (runningJob == null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
           "Couldn't find job " + jobID + " in either pending or running " +
           "queues");
    }
    else
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
           "Removed " + jobID + " from running queue");
      if (waitForStop)
      {
        runningJob.stopAndWait();
      }
      else
      {
        runningJob.stopProcessing();
      }
      cancelledCount.incrementAndGet();
      slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
           "Running job " + jobID + " cancelled by user");
    }

    return runningJob;
  }



  /**
   * Cancels a job that has been submitted and removes it from the SLAMD server
   * entirely.  This may only be performed on jobs that have not yet started
   * running.
   *
   * @param  jobID  Indicates which job should be stopped.
   */
  public void cancelAndDeleteJob(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.cancelAndDeleteJob(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.cancelAndDeleteJob(" + jobID + ')');

    synchronized (schedulerMutex)
    {
      // See if the job is in the set of pending jobs.  If we find it there,
      // then we don't need to do anything but remove it because it hasn't yet
      // started.
      final Job pendingJob = pendingJobs.remove(jobID);
      if (pendingJob != null)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Removed " + jobID + " from pending queue");
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
             "Pending job " + jobID + " cancelled by user");
        cancelledCount.incrementAndGet();


        // Only actually delete a job if it is not part of an optimizing job.
        if (pendingJob.getOptimizingJobID() != null)
        {
          return;
        }


        try
        {
          pendingJob.setJobState(Constants.JOB_STATE_CANCELLED);
          configDB.removeJob(pendingJob.getJobID());
          slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
               "Removed config database info for job " + jobID);
        }
        catch (final DatabaseException de)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
               "Could not remove config database info for job " + jobID +
               ":  " + de);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(de));
        }

        return;
      }
    }
  }



  /**
   * Cancels any iterations of the specified optimizing job that may be pending
   * or running.
   *
   * @param  optimizingJob  The optimizing job for which to cancel any scheduled
   *                        iterations.
   *
   * @return  {@code true} if any iterations were found that are still running,
   *          or {@code false} if there were no running iterations found.  Note
   *          that this return value only accounts for running jobs and not
   *          pending jobs because it is used by the caller to determine if it
   *          should wait for running jobs to complete.
   */
  public boolean cancelOptimizingJob(final OptimizingJob optimizingJob)
  {
    boolean runningFound = false;
    final String  optimizingJobID = optimizingJob.getOptimizingJobID();

    synchronized (schedulerMutex)
    {
      // First, iterate through the pending jobs queue and remove any jobs we
      // find there that are associated with the provided optimizing job.
      final Iterator<Map.Entry<String,Job>> pendingIterator =
           pendingJobs.entrySet().iterator();
      while (pendingIterator.hasNext())
      {
        final Map.Entry<String,Job> e = pendingIterator.next();
        final Job job = e.getValue();
        if (optimizingJobID.equals(job.getOptimizingJobID()))
        {
          pendingIterator.remove();
          final String jobID = job.getJobID();

          slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
               "Removed " + jobID + " from pending queue");
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
               "Pending job " + jobID + " cancelled by user");
          cancelledCount.incrementAndGet();
          try
          {
            job.setJobState(Constants.JOB_STATE_CANCELLED);
            final Date now = new Date();
            job.setActualStartTime(now);
            job.setActualStopTime(now);
            job.setActualDuration(0);
            configDB.writeJob(job);
            slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                 "Updated config database info for job " + jobID);
          }
          catch (final DatabaseException de)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                 "Could not update config database info for job " + jobID +
                 " to indicate it was cancelled:  " + de);
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(de));
          }
        }
      }


      // Next, go through the running queue and cancel any jobs we find there
      // that are associated with the provided optimizing job.
      for (final Job job : runningJobs.values())
      {
        if (optimizingJobID.equals(job.getOptimizingJobID()))
        {
          runningFound = true;
          final String jobID = job.getJobID();
          job.stopProcessing();
          cancelledCount.incrementAndGet();
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
               "Running job " + jobID + " cancelled by user");
        }
      }


      // Make sure to remove the optimizing job from our in-memory map.
      optimizingJobs.remove(optimizingJobID);
    }

    return runningFound;
  }



  /**
   * Disables a job that is pending execution and updates the job state in the
   * config database.
   *
   * @param  jobID  The job ID of the job to be disabled.
   *
   * @throws  SLAMDServerException  If a problem occurs while trying to disable
   *                                the job.
   */
  public void disableJob(final String jobID)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.disableJob(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.disableJob(" + jobID + ')');

    // Find the job in the pending jobs queue.
    final Job pendingJob;
    synchronized (schedulerMutex)
    {
      pendingJob = pendingJobs.get(jobID);
    }

    // If the job was not found, then throw an exception.
    if (pendingJob == null)
    {
      throw new SLAMDServerException("Could not find job " + jobID +
           " in the pending job queue.");
    }

    // Update the job state in the config database to indicate it is disabled.
    pendingJob.setJobState(Constants.JOB_STATE_DISABLED);

    try
    {
      configDB.writeJob(pendingJob);
    }
    catch (final DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to update job " + jobID +
           " in the configuration database:  " + de, de);
    }
  }



  /**
   * Enables a job that is currently disabled and updates the job state in the
   * config database.
   *
   * @param  jobID  The job ID of the job to be enabled.
   *
   * @throws  SLAMDServerException  If a problem occurs while trying to enable
   *                                the job.
   */
  public void enableJob(final String jobID)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.enableJob(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.enableJob(" + jobID + ')');

    // Find the job in the pending jobs queue.
    final Job pendingJob;
    synchronized (schedulerMutex)
    {
      pendingJob = pendingJobs.get(jobID);
    }

    // If the job was not found, then throw an exception.
    if (pendingJob == null)
    {
      throw new SLAMDServerException("Could not find job " + jobID +
           " in the pending job queue.");
    }

    // Update the job state in the config database to indicate it is disabled.
    pendingJob.setJobState(Constants.JOB_STATE_NOT_YET_STARTED);

    try
    {
      configDB.writeJob(pendingJob);
    }
    catch (final DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to update job " + jobID +
           " in the configuration database:  " + de, de);
    }
  }



  /**
   * Updates the scheduler and the configuration to indicate that the job has
   * completed processing.
   *
   * @param  job  The job that the client has completed processing.
   */
  public void jobDone(final Job job)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.jobDone(" + job.getJobID() + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.jobDone(" + job.getJobID() + ')');

    // Update the job entry in the configuration database.
    try
    {
      job.setActualStopTime(new Date());
      configDB.writeJob(job);
      slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
           "Updated completed job information for " + job.getJobID() +
           " in the config database");
    }
    catch (final DatabaseException de)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
           "Unable to update information for completed job " + job.getJobID() +
           ":  " + de);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(de));
    }


    // See if anyone should be notified that the job is complete.
    slamdServer.sendCompletedJobNotification(job);


    // See if this job is associated with an optimizing job.  If so, then
    // notify it.
    final String optimizingJobID = job.getOptimizingJobID();
    if (optimizingJobID != null)
    {
      try
      {
        final OptimizingJob optimizingJob = getOptimizingJob(optimizingJobID);
        optimizingJob.jobIterationComplete(job);
      }
      catch (final Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
             "Unable to update optimizing job information for completed job " +
             job.getJobID() + ":  " + e);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(e));
      }
    }


    // Remove the job from the scheduler queue if it's there
    synchronized (schedulerMutex)
    {
      // Put the job in the recently completed list, bumping the oldest one if
      // necessary.
      recentlyCompletedJobs.add(0, job);
      while (recentlyCompletedJobs.size() > MAX_RECENTLY_COMPLETED)
      {
        recentlyCompletedJobs.remove(MAX_RECENTLY_COMPLETED);
      }

      // First look in the running job queue
      final Job runningJob = runningJobs.remove(job.getJobID());
      if (runningJob != null)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Removed " + job.getJobID() + " from running queue");
        completedCount.incrementAndGet();
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
             job.getJobName() + " job " + job.getJobID() + " completed");
        return;
      }


      // Not in the running job queue, so look in the pending job queue
      final Job pendingJob = pendingJobs.remove(job.getJobID());
      if (pendingJob != null)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Removed " + job.getJobID() + " from pending queue");
        completedCount.incrementAndGet();
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
             job.getJobName() + " job " + job.getJobID() + " completed");
        return;
      }
    }
  }



  /**
   * Retrieves the requested job from the scheduler.
   *
   * @param  jobID  The job ID of the job to be retrieved.
   *
   * @return  The requested job, or {@code null} if no reference to the
   *          specified job could be found.
   *
   * @throws  SLAMDServerException  If there is a problem while retrieving the
   *                                specified job.
   */
  public Job getJob(final String jobID)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getJob(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getJob(" + jobID + ')');


    synchronized (schedulerMutex)
    {
      final Job pendingJob = pendingJobs.get(jobID);
      if (pendingJob != null)
      {
        return pendingJob;
      }

      final Job runningJob = runningJobs.get(jobID);
      if (runningJob != null)
      {
        return runningJob;
      }
    }


    // It's not in the scheduler, so fall back on the config DB.
    try
    {
      return configDB.getJob(jobID);
    }
    catch (final Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(e));
      throw new SLAMDServerException("Unable to get job " + jobID + ":  " + e,
           e);
    }
  }



  /**
   * Retrieves the optimizing job with the specified ID from the scheduler.
   *
   * @param  optimizingJobID  The ID of the optimizing job to retrieve.
   *
   * @return  The requested optimizing job, or {@code null} if no reference to
   *          the specified optimizing job could be found.
   *
   * @throws  SLAMDServerException  If there is a problem while retrieving the
   *                                specified optimizing job.
   */
  public OptimizingJob getOptimizingJob(final String optimizingJobID)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getOptimizingJob(" + optimizingJobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getOptimizingJob(" + optimizingJobID + ')');

    final OptimizingJob optimizingJob = optimizingJobs.get(optimizingJobID);
    if (optimizingJob != null)
    {
      return optimizingJob;
    }

    try
    {
      return configDB.getOptimizingJob(optimizingJobID);
    }
    catch (final Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(e));
      throw new SLAMDServerException("Unable to get optimizing job " +
           optimizingJobID + ":  " + e, e);
    }
  }



  /**
   * Removes the specified optimizing job from the in-memory cache used by the
   * scheduler.  This has no effect if the specified job is not held in the
   * in-memory cache.
   *
   * @param  optimizingJobID  The ID of the optimizing job to remove from the
   *                          in-memory cache.
   */
  public void decacheOptimizingJob(final String optimizingJobID)
  {
    optimizingJobs.remove(optimizingJobID);
  }



  /**
   * Retrieves the set of jobs that are currently scheduled for execution but
   * not yet running.
   *
   * @return  The set of jobs that are currently scheduled for execution but not
   *          yet running.
   */
  public Job[] getPendingJobs()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getPendingJobs()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getPendingJobs()");

    final ArrayList<Job> pendingClone;
    synchronized (schedulerMutex)
    {
      pendingClone = new ArrayList<Job>(pendingJobs.values());
    }

    final Job[] jobArray = new Job[pendingClone.size()];
    pendingClone.toArray(jobArray);
    return jobArray;
  }



  /**
   * Retrieves the job with the requested ID from the pending jobs queue.
   *
   * @param  jobID  The job ID for the job to retrieve.
   *
   * @return  The requested job, or {@code null} if it does not exist in the
   *          pending jobs queue.
   */
  public Job getPendingJob(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getPendingJob(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getPendingJob(" + jobID + ')');

    synchronized (schedulerMutex)
    {
      return pendingJobs.get(jobID);
    }
  }



  /**
   * Retrieves the set of jobs that are currently running.
   *
   * @return  The set of jobs that are currently running.
   */
  public Job[] getRunningJobs()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getRunningJobs()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getRunningJobs()");

    final ArrayList<Job> runningClone;
    synchronized (schedulerMutex)
    {
      runningClone = new ArrayList<Job>(runningJobs.values());
    }

    final Job[] jobArray = new Job[runningClone.size()];
    runningClone.toArray(jobArray);
    return jobArray;
  }



  /**
   * Retrieves the job with the requested ID from the running jobs queue.
   *
   * @param  jobID  The job ID for the job to retrieve.
   *
   * @return  The requested job, or {@code null} if it does not exist in the
   *          running jobs queue.
   */
  public Job getRunningJob(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getRunningJob(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getRunningJob(" + jobID + ')');

    synchronized (schedulerMutex)
    {
      return runningJobs.get(jobID);
    }
  }



  /**
   * Retrieves the set of the most recently completed jobs.
   *
   * @return  The set of the most recently completed jobs.
   */
  public Job[] getRecentlyCompletedJobs()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getRecentlyCompletedJobs()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getRecentlyCompletedJobs()");

    final ArrayList<Job> recentlyCompletedClone;
    synchronized (schedulerMutex)
    {
      recentlyCompletedClone = new ArrayList<Job>(recentlyCompletedJobs);
    }

    final Job[] jobArray = new Job[recentlyCompletedClone.size()];
    recentlyCompletedClone.toArray(jobArray);
    return jobArray;
  }



  /**
   * Retrieves a list of the optimizing jobs for which an iteration is either
   * pending or running.
   *
   * @return  A list of the optimizing jobs for which an iteration is either
   *          pending or running.
   *
   * @throws  SLAMDServerException  If a problem occurs while retrieving the
   *                                uncompleted optimizing jobs.
   */
  public OptimizingJob[] getUncompletedOptimizingJobs()
         throws SLAMDServerException
  {
    final ArrayList<OptimizingJob> optimizingJobList;
    synchronized (schedulerMutex)
    {
      optimizingJobList = new ArrayList<OptimizingJob>(pendingJobs.size() +
           runningJobs.size());

      for (final Job job : pendingJobs.values())
      {
        final String optimizingJobID = job.getOptimizingJobID();
        if (optimizingJobID != null)
        {
          try
          {
            optimizingJobList.add(getOptimizingJob(optimizingJobID));
          }
          catch (final Exception e)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                   JobClass.stackTraceToString(e));
            throw new SLAMDServerException("Unable to retrieve optimizing " +
                 "job " + optimizingJobID + " from the configuration " +
                 "database:  " + e, e);
          }
        }
      }

      for (final Job job : runningJobs.values())
      {
        final String optimizingJobID = job.getOptimizingJobID();
        if (optimizingJobID != null)
        {
          try
          {
            optimizingJobList.add(getOptimizingJob(optimizingJobID));
          }
          catch (final Exception e)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(e));
            throw new SLAMDServerException("Unable to retrieve optimizing " +
                 "job " + optimizingJobID + " from the configuration " +
                 "database:  " + e, e);
          }
        }
      }
    }

    final OptimizingJob[] ojs = new OptimizingJob[optimizingJobList.size()];
    optimizingJobList.toArray(ojs);
    return ojs;
  }



  /**
   * Indicates whether the specified job is currently in the pending queue.
   *
   * @param  jobID  The ID of the job for which to make the determination.
   *
   * @return  {@code true} if the specified job is in the pending queue, or
   *          {@code false} if it is not.
   */
  public boolean isJobPending(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.isJobPending(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.isJobPending(" + jobID + ')');

    synchronized (schedulerMutex)
    {
      return pendingJobs.containsKey(jobID);
    }
  }



  /**
   * Indicates whether the specified job is currently in the pending queue.
   * This method expects the caller to already hold the lock on the scheduler
   * mutex and therefore does not try to grab it for itself.
   *
   * @param  jobID  The ID of the job for which to make the determination.
   *
   * @return  {@code true} if the specified job is in the pending queue, or
   *          {@code false} if it is not.
   */
  private boolean isJobPendingNoMutex(final String jobID)
  {
    return pendingJobs.containsKey(jobID);
  }



  /**
   * Retrieves information about why the specified job is currently in the
   * pending jobs queue and has not yet started running.
   *
   * @param  jobID  The job ID of the job for which to make the determination.
   *
   * @return  A human-readable string that indicates why the job has not yet
   *          started running, or {@code null} if there is no such job in the
   *          pending jobs queue or if there is no reason that it should not
   *          start running.
   */
  public String getPendingReason(final String jobID)
  {
    synchronized (schedulerMutex)
    {
      final Job job = pendingJobs.get(jobID);
      if (job != null)
      {
        if (job.getJobState() == Constants.JOB_STATE_DISABLED)
        {
          return "The job is currently disabled";
        }

        if (job.getStartTime().getTime() > System.currentTimeMillis())
        {
          return "The start time has not yet arrived";
        }

        final String[] dependencies = job.getDependencies();
        if (dependencies != null)
        {
          for (final String dep : dependencies)
          {
            if ((dep != null) && (dep.length() > 0))
            {
              if (isJobScheduledNoMutex(dep))
              {
                // The job is scheduled, so this is an unresolved dependency
                return "This job has an unresolved dependency on job " + dep;
              }
              else
              {
                // The job could be an optimizing job.  See if that is the
                // case, and if so if it may be an unresolved dependency.
                try
                {
                  final OptimizingJob optimizingJob = getOptimizingJob(dep);
                  if ((optimizingJob != null) &&
                      (! optimizingJob.doneRunning()))
                  {
                    return "This job has an unresolved dependency on " +
                        "optimizing job" + dep;
                  }
                }
                catch (final Exception e)
                {
                  slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                       JobClass.stackTraceToString(e));
                }
              }
            }
          }
        }

        if (! listener.connectionsAvailable(job))
        {
          return "There is not a sufficient set of clients available to " +
               "process the job.";
        }

        if (! monitorListener.connectionsAvailable(job))
        {
          return "There is not a sufficient set of resource monitor " +
               "clients available to process the job.";
        }

        return "There is no reason why this job has not yet started.  " +
             "It will likely be started the next time the scheduler poll " +
             "thread examines the job.";
      }
    }

    return null;
  }



  /**
   * Indicates whether the specified job is currently in the running queue.
   *
   * @param  jobID  The ID of the job for which to make the determination.
   *
   * @return  {@code true} if the specified job is in the running queue, or
   *          {@code false} if it is not.
   */
  public boolean isJobRunning(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.isJobRunning(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.isJobRunning(" + jobID + ')');

    synchronized (schedulerMutex)
    {
      return runningJobs.containsKey(jobID);
    }
  }



  /**
   * Indicates whether the specified job is currently in the running queue.
   * This method expects the caller to already hold the lock on the scheduler
   * mutex and therefore does not try to grab it for itself.
   *
   * @param  jobID  The ID of the job for which to make the determination.
   *
   * @return  {@code true} if the specified job is in the running queue, or
   *          {@code false} if it is not.
   */
  private boolean isJobRunningNoMutex(final String jobID)
  {
    return runningJobs.containsKey(jobID);
  }



  /**
   * Indicates whether the specified job is currently in either the pending or
   * running queue.
   *
   * @param  jobID  The ID of the job for which to make the determination.
   *
   * @return  {@code true} if the specified job is in either queue, or
   *          {@code false} if it is not.
   */
  public boolean isJobScheduled(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.isJobScheduled(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.isJobScheduled(" + jobID + ')');

    synchronized (schedulerMutex)
    {
      return (pendingJobs.containsKey(jobID) || runningJobs.containsKey(jobID));
    }
  }



  /**
   * Indicates whether the specified job is currently in either the pending or
   * running queue.  This method expects the caller to already hold the lock
   * on the scheduler mutex and therefore does not try to grab it for itself or
   * call other methods that try to obtain the lock.
   *
   * @param  jobID  The ID of the job for which to make the determination.
   *
   * @return  {@code true} if the specified job is in either queue, or
   *          {@code false} if it is not.
   */
  private boolean isJobScheduledNoMutex(final String jobID)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.isJobScheduledNoMutex(" + jobID + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.isJobScheduledNoMutex(" + jobID + ')');

    return (pendingJobs.containsKey(jobID) || runningJobs.containsKey(jobID));
  }



  /**
   * Indicates whether the scheduler is currently running.
   *
   * @return  {@code true} if the scheduler is running or
   *          {@code false} if it is not.
   */
  public boolean isRunning()
  {
    return running.get();
  }



  /**
   * Indicates whether a request has been made to stop the scheduler.
   *
   * @return  {@code true} if a request has been made to stop the scheduler
   *          {@code false} if not.
   */
  public boolean stopRequested()
  {
    return stopRequested.get();
  }



  /**
   * Handles all of the real work of managing the jobs that have been scheduled,
   * including making sure jobs are started at the appropriate time.
   */
  @Override()
  public void run()
  {
    schedulerThread = Thread.currentThread();
    running.set(true);

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.run()");

    // Create a loop that will continue running until a request has been made to
    // stop the scheduler
    while (! stopRequested())
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
           "Examining job queues");

      // Determine the current time.  This is used to figure out how long to
      // sleep between scheduler loop iterations
      final long startTime = System.currentTimeMillis();


      // Determine the time that we will use to compare against the job start
      // time to see if it is time to send the job to the client.
      final long compareTime = startTime + startBuffer;


      // Iterate through the list of pending jobs and see if it is time to start
      // any of them
      long earliestCompareTime = compareTime + schedulerDelay;
      synchronized (schedulerMutex)
      {
        boolean started = false;
        final Iterator<Job> pendingIterator = pendingJobs.values().iterator();
        while (pendingIterator.hasNext())
        {
          final Job job = pendingIterator.next();

          // If the job is disabled, then skip it.
          if (job.getJobState() == Constants.JOB_STATE_DISABLED)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                 "Not starting job " + job.getJobID() +
                 " because it is disabled.");
            continue;
          }

          final long jobStartTime = job.getStartTime().getTime();
          if (jobStartTime <= compareTime)
          {
            // See if this job has any unresolved dependencies.  If so, then
            // we can't start it yet.
            final String[] dependencies = job.getDependencies();
            if (dependencies != null)
            {
              boolean unresolvedDependency = false;
              for (final String dep : dependencies)
              {
                if ((dep != null) && (dep.length() > 0))
                {
                  if (isJobScheduledNoMutex(dep))
                  {
                    // The job is scheduled, so this is an unresolved dependency
                    unresolvedDependency = true;
                    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                         "Not starting job " + job.getJobID() +
                         " because it has an unresolved dependency on " + dep);
                    break;
                  }
                  else
                  {
                    // The job could be an optimizing job.  See if that is the
                    // case, and if so if it may be an unresolved dependency.
                    try
                    {
                      final OptimizingJob optimizingJob = getOptimizingJob(dep);
                      if ((optimizingJob != null) &&
                          (! optimizingJob.doneRunning()))
                      {
                        unresolvedDependency = true;
                        slamdServer.logMessage(
                             Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                             "Not starting job " + job.getJobID() +
                             " because it has an unresolved dependency on " +
                             "optimizing job " + dep);
                        break;
                      }
                    }
                    catch (Exception e)
                    {
                      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                           JobClass.stackTraceToString(e));
                    }
                  }
                }
              }

              if (unresolvedDependency)
              {
                continue;
              }
            }

            // Make sure that there are enough clients available to run the job.
            // If not, then see if we should keep waiting or if the job should
            // be cancelled
            if ((! listener.connectionsAvailable(job)) ||
                (! monitorListener.connectionsAvailable(job)))
            {
              if (job.waitForClients())
              {
                if (! listener.connectionsAvailable(job))
                {
                  slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                       "Not starting job " + job.getJobID() + " because " +
                       "there is not a sufficient set of clients available");
                }
                else
                {
                  slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                       "Not starting job " + job.getJobID() +
                       " because there is not a sufficient set of resource " +
                       "monitor clients available");
                }
                continue;
              }
              else
              {
                // The job is not able to run, and we're going to cancel it.
                pendingIterator.remove();
                final String message = "Insufficient clients available.";
                slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                     "Unable to run " + job.getJobName() + " job " +
                     job.getJobID() + " -- " + message);
                job.setJobState(Constants.JOB_STATE_STOPPED_DUE_TO_ERROR);
                final Date now = new Date();
                job.setActualStartTime(now);
                job.setActualStopTime(now);
                job.setActualDuration(0);
                job.setLogMessages(new String[] { message });

                try
                {
                  configDB.writeJob(job);
                }
                catch (final DatabaseException de)
                {
                  slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                       "Unable to update state of job " + job.getJobID() +
                       " to indicate stopped due to " + "error:  " + de);
                  slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                       JobClass.stackTraceToString(de));
                }

                continue;
              }
            }

            slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                 "Time to start " + job.getJobID());

            // It is time for this job to start, so update the state in the
            // config database, start it, move it to the running queue, and
            // decrement the loop counter so we don't skip any jobs.  Also, if
            // it is part of an optimizing job, then make sure that the
            // optimizing job has a state of "running".
            try
            {
              final Date now = new Date();
              job.setActualStartTime(now);
              job.setJobState(Constants.JOB_STATE_RUNNING);
              configDB.writeJob(job);

              final String optimizingJobID = job.getOptimizingJobID();
              if (optimizingJobID != null)
              {
                final OptimizingJob optimizingJob =
                     getOptimizingJob(optimizingJobID);
                if (optimizingJob != null)
                {
                  if (optimizingJob.getJobState() !=
                      Constants.JOB_STATE_RUNNING)
                  {
                    optimizingJob.setJobState(Constants.JOB_STATE_RUNNING);
                    optimizingJob.setActualStartTime(now);
                    configDB.writeOptimizingJob(optimizingJob);
                  }
                }
              }

              job.startProcessing();
              runningJobs.put(job.getJobID(), job);
              pendingIterator.remove();
              slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                   "Starting " + job.getJobName() + " job " + job.getJobID());
              started = true;
            }
            catch (final UnableToRunException sutre)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                   "Unable to run " + job.getJobName() + " job " +
                   job.getJobID() + ":  " + sutre);
              slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                   JobClass.stackTraceToString(sutre));

              // The job was unable to run for some reason, so update the
              // job status in the config database and remove it from the
              // job queues
              try
              {
                job.addLogMessage(sutre.getMessage());
                job.setJobState(Constants.JOB_STATE_STOPPED_DUE_TO_ERROR);
                final Date now = new Date();
                job.setActualStartTime(now);
                job.setActualStopTime(now);
                job.setActualDuration(0);
                configDB.writeJob(job);
              }
              catch (final DatabaseException de)
              {
                // We couldn't update the config database information, so just
                // log it with a fatal status
                slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                     "Unable to update state of job " + job.getJobID() +
                     " to indicate stopped due to error:  " + de);
                slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                     JobClass.stackTraceToString(de));
              }
              pendingIterator.remove();
              cancelledCount.incrementAndGet();
            }
            catch (final Exception e)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                   "Unable to update state of job " + job.getJobID() +
                   " to indicate the job is running:  " + e);
              slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                   JobClass.stackTraceToString(e));
            }
          }
          else
          {
            earliestCompareTime =
                 Math.min(earliestCompareTime, (jobStartTime - compareTime));
            slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
                 "Not starting job " + job.getJobID() + " because the start " +
                 "time has not yet arrived.");
          }
        }
      }


      // If a stop has been requested, then break out of the scheduler loop
      if (stopRequested.get())
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Scheduler detected stop requested");
        break;
      }


      // Sleep until it is time to iterate through the scheduler loop again.
      final long sleepTime = earliestCompareTime - System.currentTimeMillis();
      if ((sleepTime > 0) && (! stopRequested.get()))
      {
        try
        {
          final Job j = toScheduleQueue.poll(sleepTime, TimeUnit.MILLISECONDS);
          if (j != null)
          {
            final ArrayList<Job> toScheduleList =
                 new ArrayList<Job>(1 + toScheduleQueue.size());
            toScheduleList.add(j);
            toScheduleQueue.drainTo(toScheduleList);

            final Iterator<Job> iterator = toScheduleList.iterator();
            while (iterator.hasNext())
            {
              final Job job = iterator.next();
              if (job == WAKEUP_JOB)
              {
                iterator.remove();
              }
            }

            if (! toScheduleList.isEmpty())
            {
              synchronized (schedulerMutex)
              {
                for (final Job toSchedule : toScheduleList)
                {
                  pendingJobs.put(toSchedule.getJobID(), toSchedule);
                }
              }
            }
          }
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(e));
        }
      }
    }


    running.set(false);
    schedulerThread = null;

    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "Leaving Scheduler.run()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "Leaving Scheduler.run()");
  }



  /**
   * Generates a unique ID to use for identifying a job in the scheduler.
   *
   * @return  A unique ID to use for identifying a job in the scheduler.
   */
  public synchronized String generateUniqueID()
  {
    final char[] randomChars = new char[Constants.UNIQUE_ID_RANDOM_CHAR_COUNT];

    for (int i=0; i < randomChars.length; i++)
    {
      final int pos = Math.abs(numberGenerator.nextInt()) %
           Constants.UNIQUE_ID_RANDOM_CHAR_SET.length;
      randomChars[i] = Constants.UNIQUE_ID_RANDOM_CHAR_SET[pos];
    }

    return dateFormat.format(new Date()) + '-' + new String(randomChars) +
         idCounter.getAndIncrement();
  }



  /**
   * Retrieves the total number of jobs that have been scheduled since the SLAMD
   * server started.
   *
   * @return  The total number of jobs that have been scheduled since the SLAMD
   *          server started.
   */
  public int getTotalScheduledJobCount()
  {
    return scheduledCount.get();
  }



  /**
   * Retrieves the number of jobs that are currently scheduled (both pending
   * and running).
   *
   * @return  The total number of jobs that are currently scheduled.
   */
  public int getCurrentScheduledJobCount()
  {
    synchronized (schedulerMutex)
    {
      return (pendingJobs.size() + runningJobs.size());
    }
  }



  /**
   * Retrieves the number of jobs that are currently running.
   *
   * @return  The number of jobs that are currently running.
   */
  public int getRunningJobCount()
  {
    synchronized (schedulerMutex)
    {
      return runningJobs.size();
    }
  }



  /**
   * Retrieves the number of jobs that are currently pending (waiting to run).
   *
   * @return  The number of jobs that are currently pending.
   */
  public int getPendingJobCount()
  {
    synchronized (schedulerMutex)
    {
      return pendingJobs.size();
    }
  }



  /**
   * Retrieves the number of jobs that have been cancelled since the server
   * started.
   *
   * @return  The number of jobs that have been cancelled since the server
   *          started.
   */
  public int getCancelledJobCount()
  {
    return cancelledCount.get();
  }



  /**
   * Retrieves the number of jobs that have completed running since the server
   * started.
   *
   * @return  The number of jobs that have completed since the server started.
   */
  public int getCompletedJobCount()
  {
    return completedCount.get();
  }



  /**
   * Retrieves the name that the scheduler uses to subscribe to the
   * configuration handler in order to be notified of configuration changes.
   *
   * @return  The name that the scheduler uses to subscribe to the configuration
   *          handler in order to be notified of configuration changes.
   */
  public String getSubscriberName()
  {
    return CONFIG_SUBSCRIBER_NAME;
  }



  /**
   * Retrieves the set of configuration parameters associated with this
   * configuration subscriber.
   *
   * @return  The set of configuration parameters associated with this
   *          configuration subscriber.
   */
  public ParameterList getSubscriberParameters()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.getParameters()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.getParameters()");

    final IntegerParameter schedulerDelayParameter = new IntegerParameter(
         Constants.PARAM_SCHEDULER_DELAY, "Scheduler Poll Delay",
         "The delay in seconds between checking the scheduler queues to " +
              "determine if there is work to be done.", true, schedulerDelay,
         true, 1, false, 0);

    final IntegerParameter startBufferParameter = new IntegerParameter(
         Constants.PARAM_SCHEDULER_START_BUFFER, "Job Start Buffer",
         "The time in seconds before the job's actual start time that the " +
              "job request should be sent to clients.", true,
         (startBuffer/1000), true, 0, false, 0);


    final Parameter[] params = new Parameter[]
    {
      schedulerDelayParameter,
      startBufferParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the SLAMD scheduler.  In
   * this case, the only option is the delay between iterations of the scheduler
   * loop.
   */
  public void refreshSubscriberConfiguration()
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.refreshConfiguration()");
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.refreshConfiguration()");

    final String delayStr =
         configDB.getConfigParameter(Constants.PARAM_SCHEDULER_DELAY);
    if ((delayStr != null) && (delayStr.length() > 0))
    {
      try
      {
        schedulerDelay = Integer.parseInt(delayStr);
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Setting scheduler delay to " + schedulerDelay);
      }
      catch (final NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
             "Config parameter " + Constants.PARAM_SCHEDULER_DELAY +
             " requires a numeric value");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(nfe));
      }
    }
    else
    {
      schedulerDelay = Constants.DEFAULT_SCHEDULER_DELAY;
      slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
           "Setting scheduler delay to default of " + schedulerDelay);
    }


    final String startBufferStr =
         configDB.getConfigParameter(Constants.PARAM_SCHEDULER_START_BUFFER);
    if (startBufferStr != null)
    {
      try
      {
        startBuffer = 1000 * Integer.parseInt(startBufferStr);
      }
      catch (final NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
             "Config parameter " + Constants.PARAM_SCHEDULER_START_BUFFER +
             " requires a numeric value");
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
             JobClass.stackTraceToString(nfe));
      }
    }
    else
    {
      startBuffer = Constants.DEFAULT_SCHEDULER_START_BUFFER;
      slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
           "Setting scheduler start buffer to default of " + startBuffer);
    }
  }



  /**
   * Re-reads the configuration information for the specified parameter.  In
   * this case, the only option is the delay between iterations of the scheduler
   * loop.
   *
   * @param  parameterName  The name of the parameter to re-read from the
   *                        configuration.
   */
  public void refreshSubscriberConfiguration(final String parameterName)
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
         "In Scheduler.refreshConfiguration(" + parameterName + ')');
    slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
         "In Scheduler.refreshConfiguration(" + parameterName + ')');

    if (parameterName.equalsIgnoreCase(Constants.PARAM_SCHEDULER_DELAY))
    {
      final String delayStr =
           configDB.getConfigParameter(Constants.PARAM_SCHEDULER_DELAY);
      if ((delayStr != null) && (delayStr.length() > 0))
      {
        try
        {
          schedulerDelay = Integer.parseInt(delayStr);
          slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
               "Setting scheduler delay to " + schedulerDelay);
        }
        catch (final NumberFormatException nfe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
               "Config parameter " + Constants.PARAM_SCHEDULER_DELAY +
               " requires a numeric value");
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        schedulerDelay = Constants.DEFAULT_SCHEDULER_DELAY;
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Setting scheduler delay to default of " + schedulerDelay);
      }
    }

    else if (parameterName.equalsIgnoreCase(
                  Constants.PARAM_SCHEDULER_START_BUFFER))
    {
      final String startBufferStr =
           configDB.getConfigParameter(Constants.PARAM_SCHEDULER_START_BUFFER);
      if (startBufferStr != null)
      {
        try
        {
          startBuffer = 1000 * Integer.parseInt(startBufferStr);
        }
        catch (final NumberFormatException nfe)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
               "Config parameter " + Constants.PARAM_SCHEDULER_START_BUFFER +
               " requires a numeric value");
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
               JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        startBuffer = Constants.DEFAULT_SCHEDULER_START_BUFFER;
        slamdServer.logMessage(Constants.LOG_LEVEL_SCHEDULER_DEBUG,
             "Setting scheduler start buffer to default of " + startBuffer);
      }
    }
  }
}

