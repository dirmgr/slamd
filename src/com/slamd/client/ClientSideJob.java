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
package com.slamd.client;



import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.slamd.job.AlreadyRunningException;
import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.common.Constants;
import com.slamd.common.JobClassLoader;
import com.slamd.common.SLAMDException;
import com.slamd.message.ClassTransferRequestMessage;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatPersistenceThread;
import com.slamd.stat.StatTracker;



/**
 * This class defines a "client-side" job.  It is a representation of a job that
 * is held by the client, having no knowledge of anything on the server side).
 * All methods implemented in this class operate on the local version of the
 * job only and have no direct impact on the execution of the job that may be
 * occurring concurrently on other clients.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientSideJob
{
  /**
   * The date formatter used to generate the timestamps in messages to be
   * logged.
   */
  static SimpleDateFormat dateFormat =
              new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);


  // The set of active job threads.
  ArrayList<JobClass> activeThreads;

  // The set of messages that have been logged during the execution of the job.
  ArrayList<String> logMessages;

  // Indicates whether real-time statistics collection should be used.
  boolean enableRealTimeStats;

  // Indicates whether the job is done processing for some reason
  boolean isDone;

  // Indicates whether the custom job class loader should be used.
  boolean useCustomClassLoader;

  // The client message writer that will be used to write messages to the
  // client.
  ClientMessageWriter messageWriter;

  // The time at which this job should start running.
  Date startTime;

  // The time at which this job should stop running.
  Date stopTime;

  // The client number associated with this job.
  int clientNumber;

  // The length of time in seconds to use as the statistics collection interval.
  int collectionInterval;

  // The maximum length of time in seconds that the job should be allowed to
  // run.
  int duration;

  // The state of the job as it currently exists.
  int jobState;

  // The number of concurrent job threads that should run on each client.
  int threadsPerClient;

  // The delay in milliseconds between the time that each thread should be
  // started.
  int threadStartupDelay;

  // The time that the job actually started running.
  long actualStartTime;

  // The time that the job actually stopped running.
  long actualStopTime;

  // The time (ms since January 1, 1970) that the job was scheduled to start.
  long scheduledStartTime;

  // A mutex used to protect multithreaded access to job threads.
  private final Object jobThreadMutex;

  // A mutex used to protect multithreaded access to the log list.
  private final Object logMutex;

  // The set of parameters that can customize the behavior of the job.
  ParameterList parameters;

  // The stat reporter that should be used for collecting statistics
  RealTimeStatReporter statReporter;

  // The client with which this job is associated.
  Client client;

  // The set of job threads that are associated with this job.
  JobClass[] jobThreads;

  // The location in which job class files may be found.
  String classPath;

  // The ID of the client that is being used to run this job.
  String clientID;

  // The unique ID assigned to this job
  String jobID;

  // The name of the Java class file that serves as the job thread
  String jobClass;



  /**
   * Creates a new client-side job with the provided information.
   *
   * @param  client                The client with which this job is associated.
   * @param  jobID                 The job ID for this job.
   * @param  jobClass              The name of the Java class that should be
   *                               executed to run this job.
   * @param  threadsPerClient      The number of threads that should be used to
   *                               run this job.
   * @param  startTime             The time at which this job is supposed to
   *                               start.
   * @param  stopTime              The time at which this job is supposed to
   *                               stop.
   * @param  clientNumber          The client number associated with this job.
   * @param  duration              The maximum length of time in seconds that
   *                               this job should run.
   * @param  collectionInterval    The length of time in seconds that should be
   *                               used for the statistics collection interval.
   * @param  threadStartupDelay    The delay in milliseconds that should be used
   *                               when creating the individual client threads.
   * @param  parameters            The list of job-specific parameters that
   *                               control how this job should operate.
   * @param  useCustomClassLoader  Indicates whether the custom job class loader
   *                               should be used to load job classes.
   * @param  enableRealTimeStats   Indicates whether this job should report
   *                               statistics in real-time.
   * @param  statReporter          The stat reporter that should be used to
   *                               report real-time statistics.
   */
  public ClientSideJob(Client client, String jobID, String jobClass,
                       int threadsPerClient, Date startTime, Date stopTime,
                       int clientNumber, int duration, int collectionInterval,
                       int threadStartupDelay, ParameterList parameters,
                       boolean useCustomClassLoader,
                       boolean enableRealTimeStats,
                       RealTimeStatReporter statReporter)
  {
    this.client               = client;
    this.classPath            = client.getClassPath();
    this.clientID             = client.getClientID();
    this.messageWriter        = client.getMessageWriter();
    this.jobID                = jobID;
    this.jobClass             = jobClass;
    this.threadsPerClient     = threadsPerClient;
    this.startTime            = startTime;
    this.stopTime             = stopTime;
    this.clientNumber         = clientNumber;
    this.duration             = duration;
    this.collectionInterval   = collectionInterval;
    this.threadStartupDelay   = threadStartupDelay;
    this.parameters           = parameters;
    this.useCustomClassLoader = useCustomClassLoader;
    this.enableRealTimeStats  = enableRealTimeStats;
    this.statReporter         = statReporter;

    scheduledStartTime = startTime.getTime();
    isDone             = false;
    jobState           = Constants.JOB_STATE_NOT_YET_STARTED;
    jobThreadMutex     = new Object();
    logMutex           = new Object();
    jobThreads         = new JobClass[threadsPerClient];
    activeThreads      = new ArrayList<JobClass>(threadsPerClient);
    logMessages        = new ArrayList<String>();

    checkForJobClass();
  }



  /**
   * Creates a new client-side job that is only to be used as a standalone job
   * (i.e., to run without a server).
   *
   * @param  messageWriter         The message writer that will be used to write
   *                               messages regarding the progress of job
   *                               execution.
   * @param  classPath             The directory on the filesystem in which job
   *                               class files may be found.
   * @param  jobClass              The name of the Java class that should be
   *                               used to run this job.
   * @param  threadsPerClient      The number of threads to use to run the job.
   * @param  duration              The maximum length of time in seconds that
   *                               the job should be allowed to run.
   * @param  collectionInterval    The length of time in seconds that should be
   *                               used for the statistics collection interval.
   * @param  parameters            The list of job-specific parameters that
   *                               control how this job should operate.
   * @param  useCustomClassLoader  Indicates whether the custom job class loader
   *                               should be used to load job classes.
   * @param  enableRealTimeStats   Indicates whether this job should report
   *                               statistics in real-time.
   * @param  statReporter          The stat reporter that should be used to
   *                               report real-time statistics.
   */
  public ClientSideJob(ClientMessageWriter messageWriter, String classPath,
                       String jobClass, int threadsPerClient, int duration,
                       int collectionInterval, ParameterList parameters,
                       boolean useCustomClassLoader,
                       boolean enableRealTimeStats,
                       RealTimeStatReporter statReporter)
  {
    this.client               = null;
    this.classPath            = classPath;
    this.clientID             = null;
    this.jobID                = null;
    this.messageWriter        = messageWriter;
    this.jobClass             = jobClass;
    this.threadsPerClient     = threadsPerClient;
    this.startTime            = null;
    this.stopTime             = null;
    this.clientNumber         = 0;
    this.duration             = duration;
    this.collectionInterval   = collectionInterval;
    this.parameters           = parameters;
    this.useCustomClassLoader = useCustomClassLoader;
    this.enableRealTimeStats  = enableRealTimeStats;
    this.statReporter         = statReporter;

    scheduledStartTime = System.currentTimeMillis();
    isDone             = false;
    jobState           = Constants.JOB_STATE_NOT_YET_STARTED;
    jobThreadMutex     = new Object();
    logMutex           = new Object();
    jobThreads         = new JobClass[threadsPerClient];
    activeThreads      = new ArrayList<JobClass>(threadsPerClient);
    logMessages        = new ArrayList<String>();
  }



  /**
   * Checks to see if the client has the specified class in its classpath.  If
   * not, then request it from the server.
   */
  private void checkForJobClass()
  {
    // See if we can load the specified job class.  If so, then return
    // "success".  Otherwise, request it from the server.
    try
    {
      if (useCustomClassLoader)
      {
        JobClassLoader jobClassLoader =
             new JobClassLoader(getClass().getClassLoader(), classPath);
        jobClassLoader.getJobClass(jobClass);
        return;
      }
      else
      {
        Class<?> jobClass = Constants.classForName(this.jobClass);
        JobClass instance = (JobClass) jobClass.newInstance();
        return;
      }
    }
    catch (Exception e)
    {
      // We couldn't find the job class, so request it from the server.
      ClassTransferRequestMessage request =
           new ClassTransferRequestMessage(client.getMessageID(), jobClass);
      try
      {
        client.sendMessage(request);
      }
      catch (IOException ioe)
      {
        writeVerbose("Unable to send class transfer request for " +
                     jobClass + ":  " + ioe);
      }
    }
  }



  /**
   * Retrieves the job ID for this job.
   *
   * @return  The job ID for this job.
   */
  public String getJobID()
  {
    return jobID;
  }



  /**
   * Retrieves the name of the Java class that should be invoked to run this
   * job.
   *
   * @return  The name of the Java class that should be invoked to run this job.
   */
  public String getJobClass()
  {
    return jobClass;
  }



  /**
   * Retrieves the number of threads that should be started on each client
   * running this job.
   *
   * @return  The number of threads that should be started on each client
   *          running this job.
   */
  public int getThreadsPerClient()
  {
    return threadsPerClient;
  }



  /**
   * Retrieves the time at which this job should start running.
   *
   * @return  The time at which this job should start running.
   */
  public Date getStartTime()
  {
    return startTime;
  }



  /**
   * Retrieves the time at which this job has been scheduled to start.
   *
   * @return  The time at which this job has been scheduled to start.
   */
  public long getScheduledStartTime()
  {
    return scheduledStartTime;
  }



  /**
   * Retrieves the time at which this job actually started running.
   *
   * @return  The time at which this job actually started running.
   */
  public long getActualStartTime()
  {
    return actualStartTime;
  }



  /**
   * Retrieves the time at which this job should stop running.
   *
   * @return  The time at which this job should stop running.
   */
  public Date getStopTime()
  {
    return stopTime;
  }



  /**
   * Retrieves the time at which this job actually stopped running.
   *
   * @return  The time at which this job actually stopped running.
   */
  public long getActualStopTime()
  {
    return actualStopTime;
  }



  /**
   * Retrieves the maximum length of time in seconds that this job should be
   * allowed to run.
   *
   * @return  The maximum length of time in seconds that this job should be
   *          allowed to run.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Retrieves the total length of time in seconds that the job was running.
   *
   * @return  The total length of time in seconds that the job was running.
   */
  public int getActualDuration()
  {
    return (int) ((actualStopTime - actualStartTime) / 1000);
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
   * Indicates whether this job should collect statistical data in real time.
   *
   * @return  <CODE>true</CODE> if the client should collect statistical data in
   *          real time, or <CODE>false</CODE> if not.
   */
  public boolean enableRealTimeStats()
  {
    return enableRealTimeStats;
  }



  /**
   * Retrieves the stat reporter that should be used to report real-time
   * statistical data.
   *
   * @return  The stat reporter that should be used to report real-time
   *          statistical data, or <CODE>null</CODE> if no reporting should be
   *          done.
   */
  public RealTimeStatReporter getStatReporter()
  {
    return statReporter;
  }



  /**
   * Retrieves the list of job-specific parameters associated with this job.
   *
   * @return  The list of job-specific parameters associated with this job.
   */
  public ParameterList getParameters()
  {
    return parameters;
  }



  /**
   * Starts processing on the job and waits until that processing is complete.
   * This method is intended for use by standalone clients that do not
   * actually communicate with a SLAMD server.
   *
   * @return  The response code from the start operation.
   */
  public int startAndWait()
  {
    int startResult = start();

    if (startResult != Constants.MESSAGE_RESPONSE_SUCCESS)
    {
      return startResult;
    }

    while (! isDone)
    {
      try
      {
        Thread.sleep(100);
      } catch (InterruptedException ie) {}
    }
    return startResult;
  }



  /**
   * Attempts to start processing on the job.
   *
   * @return  The result code from the attempted start operation.
   */
  public synchronized int start()
  {
    // First, make sure that the job has not yet been started.  If it has, then
    // exit.
    if (jobState != Constants.JOB_STATE_NOT_YET_STARTED)
    {
      return Constants.MESSAGE_RESPONSE_JOB_ALREADY_STARTED;
    }


    // Set the job state value to indicate that the job has stopped due to an
    // error.  This isn't true, but if we abort the startup for some reason,
    // then it will be.  If the startup succeeds, then this will be changed to
    // the appropriate value.
    jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;


    // Get an instance of the job class.
    JobClass jobInstance;
    if (useCustomClassLoader)
    {
      JobClassLoader jobClassLoader =
           new JobClassLoader(getClass().getClassLoader(), classPath);
      try
      {
        jobInstance = jobClassLoader.getJobClass(jobClass);
      }
      catch (SLAMDException se)
      {
        logMessage(se.getMessage());
        return Constants.MESSAGE_RESPONSE_CLASS_NOT_FOUND;
      }
    }
    else
    {
      try
      {
        Class<?> jobClass = Constants.classForName(this.jobClass);
        jobInstance = (JobClass) jobClass.newInstance();
      }
      catch (Exception e)
      {
        logMessage(e.getMessage());
        return Constants.MESSAGE_RESPONSE_CLASS_NOT_FOUND;
      }
    }


    // Perform client-level initialization for the job class.
    try
    {
      String clientID = "";
      if (client != null)
      {
        clientID = client.getClientID();
      }

      jobInstance.setClientNumber(clientNumber);
      jobInstance.setClientSideJob(this);

      StatPersistenceThread statPersistenceThread =
           Client.getStatPersistenceThread();
      if (statPersistenceThread != null)
      {
        statPersistenceThread.setJob(this);
      }

      jobInstance.initializeClient(clientID, parameters);
    }
    catch (Exception e)
    {
      logMessage("Client-level initialization failed:  " +
                 JobClass.stackTraceToString(e));
      return Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE;
    }


    // The job is ready to be started, so create the appropriate number of
    // threads and start them.  It is safe to assume that if an illegal access
    // or instantiation exception occurs as a result of this, it will be on the
    // first one and therefore there will be no cleanup necessary.
    jobThreads = new JobClass[threadsPerClient];
    long threadStartTime = scheduledStartTime;

    for (int i=0; i < threadsPerClient; i++)
    {
      try
      {
        JobClass jobThread = jobInstance.getClass().newInstance();
        String threadID = null;
        if (client != null)
        {
          threadID = client.getClientID() + '-' + i;
        }
        else
        {
          threadID = String.valueOf(i);
        }
        jobThread.setName("Job Thread " + jobID + ':' + threadID);
        jobThread.setClientNumber(clientNumber);
        jobThread.setThreadNumber(i);
        jobThread.initializeJobThread(clientID, threadID, collectionInterval,
                                      this, duration, stopTime, threadStartTime,
                                      parameters);
        jobThreads[i] = jobThread;
        activeThreads.add(jobThreads[i]);
        threadStartTime += threadStartupDelay;
      }
      catch (UnableToRunException utre)
      {
        logMessage("Unable to run exception while initializing job thread:  " +
                   JobClass.stackTraceToString(utre));
        return Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE;
      }
      catch (IllegalAccessException iae)
      {
        logMessage("Illegal access exception while initializing job thread:  " +
                   JobClass.stackTraceToString(iae));
        return Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE;
      }
      catch (InstantiationException ie)
      {
        logMessage("Instantiation exception while initializing job thread:  " +
                   JobClass.stackTraceToString(ie));
        return Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE;
      }
      catch (Exception e)
      {
        logMessage("Uncaught exception while initializing job thread:  " +
                   JobClass.stackTraceToString(e));
        return Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE;
      }
    }


    // Update the job state to indicate that it is running.  Technically, it
    // isn't running yet, but this is close enough, and we'll change the state
    // if a failure occurs later in this method.
    jobState = Constants.JOB_STATE_RUNNING;
    actualStartTime = new Date().getTime();


    // Iterate through all of the job threads and signal them to start
    for (int i=0; i < jobThreads.length; i++)
    {
      try
      {
        messageWriter.writeVerbose("Adding job thread " +
                                   jobThreads[i].getThreadID() +
                                   " to active list");
        jobThreads[i].startJob();
      }
      catch (AlreadyRunningException sare)
      {
        // This should never happen, but we still need to catch it.
        logMessage("Caught an already running exception:  " +
                   JobClass.stackTraceToString(sare));
        jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
        return Constants.MESSAGE_RESPONSE_JOB_CREATION_FAILURE;
      }

      if (threadStartupDelay > 0)
      {
        try
        {
          Thread.sleep(threadStartupDelay);
        } catch (InterruptedException ie) {}
      }
    }


    // No problems encountered -- the jobs are starting up
    return Constants.MESSAGE_RESPONSE_SUCCESS;
  }



  /**
   * Attempts to stop processing on the job.
   *
   * @param  stopReason  The reason that the job is to be stopped.
   *
   * @return  The result code from the attempted stop operation.
   */
  public int stop(int stopReason)
  {
    synchronized (jobThreadMutex)
    {
      // If the job hasn't been started yet, then cancel it.  This will prevent
      // it from being started.
      if (jobState == Constants.JOB_STATE_NOT_YET_STARTED)
      {
        jobState = Constants.JOB_STATE_CANCELLED;
      }

      // Is the job running?  If so, then try to stop it.
      if (jobState == Constants.JOB_STATE_RUNNING)
      {
        for (int i=0; i < activeThreads.size(); i++)
        {
          JobClass jobThread = activeThreads.get(i);
          jobThread.stopJob(stopReason);
          jobState = stopReason;
        }
      }
    }

    return Constants.MESSAGE_RESPONSE_SUCCESS;
  }



  /**
   * Attempts to stop processing on the job and will not return until the
   * job state indicates that it is no longer running.
   *
   * @param  stopReason  The reason that the job is to be stopped.
   *
   * @return  The result code from the attempted stop operation.
   */
  public synchronized int stopAndWait(int stopReason)
  {
    // First, send all threads the stop signal
    stop(stopReason);

    // Now loop until the job state shows the job is no longer running
    while (jobState == Constants.JOB_STATE_RUNNING)
    {
      try
      {
        Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
      } catch (InterruptedException ie) {}
    }

    jobState = stopReason;

    return Constants.MESSAGE_RESPONSE_SUCCESS;
  }




  /**
   * Attempts to forcefully stop execution of the job by interrupting the
   * thread.  This should only be used if the job is not responding to normal
   * stop requests, most likely because it is blocked.
   *
   * @param  stopReason  The stop reason that should be used for the job.
   */
  public final void forcefullyStop(int stopReason)
  {
    while (! activeThreads.isEmpty())
    {
      JobClass jobThread = activeThreads.remove(0);
      messageWriter.writeVerbose("Forcefully removing thread " +
                                 jobThread.getThreadID() +
                                 " from active list");
      try
      {
        jobThread.stopJob(stopReason);
        jobThread.interrupt();
        Thread.sleep(100);
      } catch (Exception e) {}


      // Hopefully, the above interrupt worked.  However, if it did not, then
      // try waiting a little while longer and interrupting again.  We'll only
      // try to interrupt a thread twice before trying the job thread's destroy
      // method.  If the job thread still won't die after the call to destroy
      // (which is possible, since destroy does nothing by default), then it
      // could be hung and require administrative action.
      if (jobThread.isAlive())
      {
        try
        {
          Thread.sleep(1000);

          if (jobThread.isAlive())
          {
            jobThread.interrupt();

            Thread.sleep(100);
            if (jobThread.isAlive())
            {
              jobThread.destroyThread();
            }
          }
        } catch (Exception e) {}
      }
    }
  }



  /**
   * Retrieves the state of this job.
   *
   * @return  The state of this job.
   */
  public int getJobState()
  {
    return jobState;
  }



  /**
   * Specifies the job state to use for this job.
   *
   * @param  jobState  The job state to use for this job.
   */
  public void setJobState(int jobState)
  {
    this.jobState = jobState;
  }



  /**
   * Retrieves the number of threads that are currently active.
   *
   * @return  The number of threads that are currently active.
   */
  public int getActiveThreadCount()
  {
    return activeThreads.size();
  }



  /**
   * Retrieves the stat tracker information for this job.
   *
   * @param  aggregateThreadData  Indicates whether the data collected by each
   *                              thread should be aggregated before returning
   *                              the results.
   *
   * @return  The stat tracker information for this job.
   */
  public StatTracker[] getStatTrackers(boolean aggregateThreadData)
  {
    // If there are no job threads defined, then return an empty array
    if ((jobThreads == null) || (jobThreads.length == 0))
    {
      return new StatTracker[0];
    }

    if (aggregateThreadData)
    {
      // Create a linked hash map to hold named lists of the different kinds
      // of stat trackers while preserving the original order of the trackers.
      LinkedHashMap<String,ArrayList<StatTracker>> hashMap =
           new LinkedHashMap<String,ArrayList<StatTracker>>();

      for (int i=0; i < jobThreads.length; i++)
      {
        StatTracker[] threadTrackers = jobThreads[i].getStatTrackers();
        for (int j=0; j < threadTrackers.length; j++)
        {
          ArrayList<StatTracker> trackerList =
               hashMap.get(threadTrackers[j].getDisplayName());
          if (trackerList == null)
          {
            trackerList = new ArrayList<StatTracker>();
            trackerList.add(threadTrackers[j]);
            hashMap.put(threadTrackers[j].getDisplayName(), trackerList);
          }
          else
          {
            trackerList.add(threadTrackers[j]);
          }
        }
      }


      // Now get the values from the hash map and aggregate all the data into
      // a single array of stat trackers.
      Collection<ArrayList<StatTracker>> values = hashMap.values();
      StatTracker[] aggregateTrackers = new StatTracker[values.size()];
      Iterator<ArrayList<StatTracker>> iterator = values.iterator();
      int i = 0;
      while (iterator.hasNext())
      {
        ArrayList<StatTracker> trackerList = iterator.next();
        StatTracker[] trackerArray = new StatTracker[trackerList.size()];
        trackerList.toArray(trackerArray);
        aggregateTrackers[i] = trackerArray[0].newInstance();
        aggregateTrackers[i].aggregate(trackerArray);
        aggregateTrackers[i].setThreadID("aggregated");
        i++;
      }

      return aggregateTrackers;
    }
    else
    {
      // Create the array to return
      ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
      for (int i=0; i < jobThreads.length; i++)
      {
        StatTracker[] threadTrackers = jobThreads[i].getStatTrackers();
        for (int j=0; j < threadTrackers.length; j++)
        {
          trackerList.add(threadTrackers[j]);
        }
      }


      // Return the set of status counters
      StatTracker[] trackers = new StatTracker[trackerList.size()];
      trackerList.toArray(trackers);
      return trackers;
    }
  }



  /**
   * Retrieves an array containing all of the messages that have been logged
   * for this job.
   *
   * @return  An array containing all of the messages that have been logged for
   *          this job.
   */
  public String[] getLogMessages()
  {
    ArrayList clone = (ArrayList) logMessages.clone();
    String[] messageArray = new String[clone.size()];

    for (int i=0; i < messageArray.length; i++)
    {
      messageArray[i] = (String) clone.get(i);
    }

    return messageArray;
  }



  /**
   * Writes a new message about this job into the list of messages to log.  Note
   * that the messages will not actually be logged into the SLAMD server's log
   * until they are sent back to the server in a status response or job
   * completed message in order to cut down on the network traffic and reduce
   * latency associated with job processing.
   *
   * @param  message  The message to be logged.
   */
  private void logMessage(String message)
  {
    synchronized (logMutex)
    {
      logMessages.add(message);
    }

    messageWriter.writeMessage(message);
  }



  /**
   * Writes the provided message to the client message writer using the verbose
   * setting (so the message will not be displayed if the client is not
   * operating in verbose mode).  The specified message will not be written to
   * the SLAMD log.
   *
   * @param  message  The message to be written.
   */
  public void writeVerbose(String message)
  {
    messageWriter.writeVerbose(message);
  }



  /**
   * Writes a new message from the specified thread into the list of messages to
   * log.  Note that the messages will not actually be logged into the SLAMD
   * server's log until they are sent back to the server in a status response or
   * job completed message in order to cut down on the network traffic and
   * reduce latency associated with job processing.
   *
   * @param  threadID  The ID of the thread logging this message.
   * @param  message   The message to be logged.
   */
  public void logMessage(String threadID, String message)
  {
    StringBuilder messageBuffer = new StringBuilder();

    // The date formatter is not threadsafe, so we need to protect access to it.
    synchronized (logMutex)
    {
      messageBuffer.append('[');
      messageBuffer.append(dateFormat.format(new Date()));
      messageBuffer.append(']');
    }

    messageBuffer.append(" - ");
    messageBuffer.append(
         Constants.logLevelToString(Constants.LOG_LEVEL_JOB_PROCESSING));
    messageBuffer.append(" - ");

    if (client != null)
    {
      messageBuffer.append("client=");
      messageBuffer.append(client.clientID);
    }

    if (jobID != null)
    {
      messageBuffer.append(" job=");
      messageBuffer.append(jobID);
    }

    messageBuffer.append(" - ");
    messageBuffer.append(message);

    synchronized (logMutex)
    {
      logMessages.add(messageBuffer.toString());
    }
    messageWriter.writeMessage(message);
  }



  /**
   * Indicates that the specified thread is no longer running.  This removes the
   * thread from the active thread list and once all threads have completed
   * performs the appropriate job cleanup work.
   *
   * @param  jobThread  The job thread that has finished.
   */
  public void threadDone(JobClass jobThread)
  {
    synchronized (jobThreadMutex)
    {
      // Remove it from the list of active threads
      for (int i=0; i < activeThreads.size(); i++)
      {
        if (jobThread.getThreadID().equals(activeThreads.get(i).getThreadID()))
        {
          messageWriter.writeVerbose("Removing thread " +
                                     jobThread.getThreadID() +
                                     " from active list");
          activeThreads.remove(i);
          break;
        }
      }


      // See if the active thread list is empty.  If so, then the whole job is
      // done.
      if (activeThreads.isEmpty())
      {
        messageWriter.writeVerbose("All job threads have completed");
        isDone = true;
        actualStopTime = new Date().getTime();

        // Run the per-client finalization
        try
        {
          jobThread.finalizeClient();
        }
        catch (Exception e)
        {
          messageWriter.writeVerbose("ERROR running per-client " +
                                     "finalization:  " + e);
        }

        if (client != null)
        {
          client.jobDone();
        }
      }
    }
  }



  /**
   * Indicates whether this job has been completed or is otherwise stopped for
   * some reason.  A job will not be considered done if it was never started.
   *
   * @return  <CODE>true</CODE> if the job is done, or <CODE>false</CODE> if
   *          not.
   */
  public boolean isDone()
  {
    return isDone;
  }
}

