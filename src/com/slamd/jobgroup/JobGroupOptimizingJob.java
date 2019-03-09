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
package com.slamd.jobgroup;



import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.db.DecodeException;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.JobItem;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.job.UnknownJobClass;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.server.SLAMDServer;
import com.slamd.server.Scheduler;



/**
 * This class defines a data structure for holding information about an
 * optimizing job that is scheduled as part of a job group.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobGroupOptimizingJob
       implements JobGroupItem
{
  /**
   * The name of the encoded element that holds the optimizing job name.
   */
  public static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that holds the job class name.
   */
  public static final String ELEMENT_JOB_CLASS = "job_class";



  /**
   * The name of the encoded element that holds the duration.
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
   * The name of the encoded element that holds the minimum number of threads
   * per client.
   */
  public static final String ELEMENT_MIN_THREADS = "min_threads";



  /**
   * The name of the encoded element that holds the maximum number of threads
   * per client.
   */
  public static final String ELEMENT_MAX_THREADS = "max_threads";



  /**
   * The name of the encoded element that holds the thread increment.
   */
  public static final String ELEMENT_THREAD_INCREMENT = "thread_increment";



  /**
   * The name of the encoded element that holds the statistics collection
   * interval.
   */
  public static final String ELEMENT_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the encoded element that holds the maximum number of
   * non-improving iterations.
   */
  public static final String ELEMENT_MAX_NONIMPROVING = "max_nonimproving";



  /**
   * The name of the encoded element that holds the thread startup delay.
   */
  public static final String ELEMENT_THREAD_STARTUP_DELAY =
       "thread_startup_delay";



  /**
   * The name of the encoded element that holds the flag indicating whether to
   * re-run the best iteration.
   */
  public static final String ELEMENT_RERUN_BEST_ITERATION =
       "rerun_best_iteration";



  /**
   * The name of the encoded element that holds the re-run duration.
   */
  public static final String ELEMENT_RERUN_DURATION = "rerun_duration";



  /**
   * The name of the encoded element that holds the set of dependencies for the
   * optimizing job.
   */
  public static final String ELEMENT_DEPENDENCIES = "dependencies";



  /**
   * The name of the encoded element that holds the optimization algorithm.
   */
  public static final String ELEMENT_OPTIMIZATION_ALGORITHM =
       "optimization_algorithm";



  /**
   * The name of the encoded element that holds the set of mapped parameters.
   */
  public static final String ELEMENT_MAPPED_PARAMS = "mapped_parameters";



  /**
   * The name of the encoded element that holds the set of fixed parameters.
   */
  public static final String ELEMENT_FIXED_PARAMS = "fixed_parameters";



  // The set of dependencies for this optimizing job.  They will be the names of
  // other jobs or optimizing jobs in the job group on which this job is
  // dependent.
  private ArrayList<String> dependencies;

  // Indicates whether to re-run the best iteration for this optimizing job.
  private boolean reRunBestIteration;

  // The optimizing job parameters that will be requested from the user, mapped
  // from the names used in the job group to the parameter stub for this
  // optimizing job.
  private LinkedHashMap<String,String> mappedParameters;

  // The statistics collection interval for this optimizing job.
  private int collectionInterval;

  // The delay in seconds between iterations of this optimizing job.
  private int delayBetweenIterations;

  // The maximum length of time in seconds that regular iterations of this
  // optimizing job should run.
  private int duration;

  // The maximum number of consecutive non-improving iterations for this
  // optimizing job.
  private int maxNonImproving;

  // The maximum number of threads to use to execute the optimizing job.
  private int maxThreads;

  // The minimum number of threads to use to execute the optimizing job.
  private int minThreads;

  // The number of clients to use to execute the optimizing job.
  private int numClients;

  // The maximum length of time in seconds that the re-run iteration should run.
  private int reRunDuration;

  // The thread increment to use for this optimizing job.
  private int threadIncrement;

  // The thread startup delay for this job.
  private int threadStartupDelay;

  // The job class for this optimizing job.
  private JobClass jobClass;

  // The job group with which this optimizing job is associated.
  private JobGroup jobGroup;

  // The optimization algorithm for this optimizing job.
  private OptimizationAlgorithm optimizationAlgorithm;

  // The fixed-value parameters that will always be used for this job in the job
  // group.
  private ParameterList fixedParameters;

  // The set of parameters for the associated optimization algorithm.
  private ParameterList optimizationParameters;

  // A name used to identify this job in the job group.
  private String name;



  /**
   * Creates a new job group optimizing job with the provided information.
   *
   * @param  name                    The name for this optimizing job.
   * @param  jobGroup                The job group with which this optimizing
   *                                 job is associated.
   * @param  jobClass                The job class for this optimizing job.
   * @param  duration                The duration in seconds for iterations of
   *                                 this optimizing job.
   * @param  delayBetweenIterations  The length of time in seconds that should
   *                                 be allowed between the end of one iteration
   *                                 and the beginning of the next.
   * @param  numClients              The number of clients to use to run this
   *                                 optimizing job.
   * @param  minThreads              The minimum number of threads per client
   *                                 for this optimizing job.
   * @param  maxThreads              The maximum number of threads per client
   *                                 for this optimizing job.
   * @param  threadIncrement         The thread increment between iterations for
   *                                 this optimizing job.
   * @param  collectionInterval      The collection interval for this optimizing
   *                                 job.
   * @param  maxNonImproving         The maximum number of non-improving
   *                                 iterations that will be allowed for this
   *                                 optimizing job.
   * @param  threadStartupDelay      The thread startup delay for iterations of
   *                                 this optimizing job.
   * @param  reRunBestIteration      Indicates whether to re-run the best
   *                                 iteration of this optimizing job.
   * @param  reRunDuration           The duration in seconds for the re-run
   *                                 iteration.
   * @param  dependencies            The names of the job group jobs and/or
   *                                 optimizing jobs on which this optimizing
   *                                 job is dependent.
   * @param  optimizationAlgorithm   The optimization algorithm for this
   *                                 optimizing job.
   * @param  optimizationParameters  The set of parameters for use with the
   *                                 optimization algorithm.
   * @param  mappedParameters        The set of mapped parameters for this
   *                                 optimizing job.
   * @param  fixedParameters         The set of fixed parameters for this
   *                                 optimizing job.
   */
  public JobGroupOptimizingJob(String name, JobGroup jobGroup,
                               JobClass jobClass, int duration,
                               int delayBetweenIterations, int numClients,
                               int minThreads, int maxThreads,
                               int threadIncrement, int collectionInterval,
                               int maxNonImproving, int threadStartupDelay,
                               boolean reRunBestIteration, int reRunDuration,
                               ArrayList<String> dependencies,
                               OptimizationAlgorithm optimizationAlgorithm,
                               ParameterList optimizationParameters,
                               LinkedHashMap<String,String> mappedParameters,
                               ParameterList fixedParameters)
  {
    this.name                   = name;
    this.jobGroup               = jobGroup;
    this.jobClass               = jobClass;
    this.duration               = duration;
    this.delayBetweenIterations = delayBetweenIterations;
    this.numClients             = numClients;
    this.minThreads             = minThreads;
    this.maxThreads             = maxThreads;
    this.threadIncrement        = threadIncrement;
    this.collectionInterval     = collectionInterval;
    this.maxNonImproving        = maxNonImproving;
    this.threadStartupDelay     = threadStartupDelay;
    this.reRunBestIteration     = reRunBestIteration;
    this.reRunDuration          = reRunDuration;
    this.dependencies           = dependencies;
    this.optimizationAlgorithm  = optimizationAlgorithm;
    this.optimizationParameters = optimizationParameters;
    this.mappedParameters       = mappedParameters;
    this.fixedParameters        = fixedParameters;
  }



  /**
   * Retrieves the human-readable name for this optimizing job.
   *
   * @return  The human-readable name for this optimizing job.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Specifies the human-readable name for this optimizing job.
   *
   * @param  name  The human-readable name for this optimizing job.
   */
  public void setName(String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the job group for this optimizing job.
   *
   * @return  The job group for this optimizing job.
   */
  public JobGroup getJobGroup()
  {
    return jobGroup;
  }



  /**
   * Specifies the job group for this optimizing job.
   *
   * @param  jobGroup  The job group for this optimizing job.
   */
  public void setJobGroup(JobGroup jobGroup)
  {
    this.jobGroup = jobGroup;
  }



  /**
   * Retrieves the job class for this optimizing job.
   *
   * @return  The job class for this optimizing job.
   */
  public JobClass getJobClass()
  {
    return jobClass;
  }



  /**
   * Specifies the job class for this optimizing job.
   *
   * @param  jobClass  The job class for this optimizing job.
   */
  public void setJobClass(JobClass jobClass)
  {
    this.jobClass = jobClass;
  }



  /**
   * Retrieves the duration for iterations of this optimizing job.
   *
   * @return  The duration for iterations of this optimizing job.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Specifies the duration for iterations of this optimizing job.
   *
   * @param  duration  The duration for iterations of this optimizing job.
   */
  public void setDuration(int duration)
  {
    this.duration = duration;
  }



  /**
   * Retrieves the delay between iterations for this optimizing job.
   *
   * @return  The delay between iterations for this optimizing job.
   */
  public int getDelayBetweenIterations()
  {
    return delayBetweenIterations;
  }



  /**
   * Specifies the delay between iterations for this optimizing job.
   *
   * @param  delayBetweenIterations  The delay between iterations for this
   *                                 optimizing job.
   */
  public void setDelayBetweenIterations(int delayBetweenIterations)
  {
    this.delayBetweenIterations = delayBetweenIterations;
  }



  /**
   * Retrieves the number of clients for this optimizing job.
   *
   * @return  The number of clients for this optimizing job.
   */
  public int getNumClients()
  {
    return numClients;
  }



  /**
   * Specifies the number of clients for this optimizing job.
   *
   * @param  numClients  The number of clients for this optimizing job.
   */
  public void setNumClients(int numClients)
  {
    this.numClients = numClients;
  }



  /**
   * Retrieves the minimum number of threads per client for this optimizing job.
   *
   * @return  The minimum number of threads per client for this optimizing job.
   */
  public int getMinThreads()
  {
    return minThreads;
  }



  /**
   * Specifies the minimum number of threads per client for this optimizing job.
   *
   * @param  minThreads  The minimum number of threads per client for this
   *                     optimizing job.
   */
  public void setMinThreads(int minThreads)
  {
    this.minThreads = minThreads;
  }



  /**
   * Retrieves the maximum number of threads per client for this optimizing job.
   *
   * @return  The maximum number of threads per client for this optimizing job.
   */
  public int getMaxThreads()
  {
    return maxThreads;
  }



  /**
   * Specifies the maximum number of threads per client for this optimizing job.
   *
   * @param  maxThreads  The maximum number of threads per client for this
   *                     optimizing job.
   */
  public void setMaxThreads(int maxThreads)
  {
    this.maxThreads = maxThreads;
  }



  /**
   * Retrieves the increment in the number of threads per client between
   * iterations of this optimizing job.
   *
   * @return  The increment in the number of threads per client between
   *          iterations of this optimizing job.
   */
  public int getThreadIncrement()
  {
    return threadIncrement;
  }



  /**
   * Specifies the increment in the number of threads per client between
   * iterations of this optimizing job.
   *
   * @param  threadIncrement  The increment in the number of threads per client
   *                          between iterations of this optimizing job.
   */
  public void setThreadIncrement(int threadIncrement)
  {
    this.threadIncrement = threadIncrement;
  }



  /**
   * Retrieves the statistics collection interval for this optimizing job.
   *
   * @return  The statistics collection interval for this optimizing job.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Specifies the statistics collection interval for this optimizing job.
   *
   * @param  collectionInterval  The statistics collection interval for this
   *                             optimizing job.
   */
  public void setCollectionInterval(int collectionInterval)
  {
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves the maximum number of non-improving iterations that will be
   * allowed for this optimizing job.
   *
   * @return  The maximum number of non-improving iterations that will be
   *          allowed for this optimizing job.
   */
  public int getMaxNonImprovingIterations()
  {
    return maxNonImproving;
  }



  /**
   * Specifies the maximum number of non-improving iterations that will be
   * allowed for this optimizing job.
   *
   * @param  maxNonImproving  The maximum number of non-improving iterations
   *                          that will be allowed for this optimizing job.
   */
  public void setMaxNonImprovingIterations(int maxNonImproving)
  {
    this.maxNonImproving = maxNonImproving;
  }



  /**
   * Retrieves the delay in milliseconds that should be used when creating
   * threads for this optimizing job.
   *
   * @return  The delay in milliseconds that should be used when creating
   *          threads for this optimizing job.
   */
  public int getThreadStartupDelay()
  {
    return threadStartupDelay;
  }



  /**
   * Specifies the delay in milliseconds that should be used when creating
   * threads for this optimizing job.
   *
   * @param  threadStartupDelay  The delay in milliseconds that should be used
   *                             when creating threads for this optimizing job.
   */
  public void setThreadStartupDelay(int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
  }



  /**
   * Indicates whether to re-run the best iteration of this optimizing job.
   *
   * @return  <CODE>true</CODE> if the best iteration of this optimizing job
   *          should be re-run, or <CODE>false</CODE> if not.
   */
  public boolean reRunBestIteration()
  {
    return reRunBestIteration;
  }



  /**
   * Specifies whether to re-run the best iteration of this optimizing job.
   *
   * @param  reRunBestIteration  Specifies whether to re-run the best iteration
   *                             of this optimizing job.
   */
  public void setReRunBestIteration(boolean reRunBestIteration)
  {
    this.reRunBestIteration = reRunBestIteration;
  }



  /**
   * Retrieves the duration in seconds for the re-run iteration of this
   * optimizing job.
   *
   * @return  The duration in seconds for the re-run iteration of this
   *          optimizing job.
   */
  public int getReRunDuration()
  {
    return reRunDuration;
  }



  /**
   * Specifies the duration in seconds for the re-run iteration of this
   * optimizing job.
   *
   * @param  reRunDuration  The duration in seconds for the re-run iteration of
   *                        this optimizing job.
   */
  public void setReRunDuration(int reRunDuration)
  {
    this.reRunDuration = reRunDuration;
  }



  /**
   * Retrieves the names of the jobs and/or optimizing jobs on which this
   * optimizing job is dependent.  The contents of the returned list may be
   * altered by the caller.
   *
   * @return  The names of the jobs and/or optimizing jobs on which this
   *          optimizing job is dependent.
   */
  public ArrayList<String> getDependencies()
  {
    return dependencies;
  }



  /**
   * Retrieves the optimization algorithm for this optimizing job.
   *
   * @return  The optimization algorithm for this optimizing job.
   */
  public OptimizationAlgorithm getOptimizationAlgorithm()
  {
    return optimizationAlgorithm;
  }



  /**
   * Specifies the optimization algorithm for this optimizing job.
   *
   * @param  optimizationAlgorithm  The optimization algorithm for this
   *                                optimizing job.
   */
  public void setOptimizationAlgorithm(OptimizationAlgorithm
                                            optimizationAlgorithm)
  {
    this.optimizationAlgorithm = optimizationAlgorithm;
  }



  /**
   * Retrieves the set of parameters for use with the optimization algorithm.
   *
   * @return  The set of parameters for use with the optimization algorithm.
   */
  public ParameterList getOptimizationParameters()
  {
    return optimizationParameters;
  }



  /**
   * Specifies the set of parameters for use with the optimization algorithm.
   *
   * @param  optimizationParameters  The set of parameters for use with the
   *                                 optimization algorithm.
   */
  public void setOptimizationParameters(ParameterList optimizationParameters)
  {
    this.optimizationParameters = optimizationParameters;
  }



  /**
   * Retrieves the set of mapped parameters for this optimizing job, mapped
   * between the name of the parameter for the job class and the name of the
   * parameter associated with the job group.  The contents of the returned map
   * may be altered by the caller.
   *
   * @return  The set of mapped parameters for this optimizing job.
   */
  public LinkedHashMap<String,String> getMappedParameters()
  {
    return mappedParameters;
  }



  /**
   * Retrieves the set of fixed parameters for this optimizing job.  The
   * returned parameter list may be altered by the caller.
   *
   * @return  The set of fixed parameters for this optimizing job.
   */
  public ParameterList getFixedParameters()
  {
    return fixedParameters;
  }



  /**
   * Schedules this optimizing job for execution by the SLAMD server using the
   * provided information.
   *
   * @param  slamdServer                The reference to the SLAMD server to use
   *                                    when scheduling the optimizing job.
   * @param  startTime                  The start time to use for the optimizing
   *                                    job.
   * @param  folderName                 The name of the folder into which the
   *                                    optimizing job should be placed.
   * @param  requestedClients           The set of clients that have been
   *                                    requested for this job group.
   * @param  requestedMonitorClients    the set of resource monitor clients that
   *                                    have been requested for this job group.
   * @param  monitorClientsIfAvailable  Indicates whether the clients used to
   *                                    run the job should be monitored if there
   *                                    are also resource monitor clients
   *                                    running on the same system.
   * @param  externalDependencies       A set of jobs outside of this job group
   *                                    on which the scheduled optimizing job
   *                                    should be dependent.
   * @param  parameters                 The set of parameters that the user
   *                                    provided for the job group.
   * @param  scheduledJobs              The set of jobs and optimizing jobs that
   *                                    have been scheduled so far as part of
   *                                    the job group.  They will be mapped from
   *                                    the name of the job or optimizing job in
   *                                    this job group to the corresponding
   *                                    object.
   * @param  messages                   A list of messages that should be
   *                                    displayed to the user as a result of
   *                                    scheduling the optimizing job.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to schedule
   *                          the optimizing job.
   */
  public void schedule(SLAMDServer slamdServer, Date startTime,
                       String folderName, String[] requestedClients,
                       String[] requestedMonitorClients,
                       boolean monitorClientsIfAvailable,
                       String[] externalDependencies, ParameterList parameters,
                       LinkedHashMap<String,JobItem> scheduledJobs,
                       ArrayList<String> messages)
         throws SLAMDException
  {
    // Create a new parameter list that combines the mapped parameters and the
    // fixed parameters.
    Parameter[] params = jobClass.getParameterStubs().clone().getParameters();
    for (int i=0; i < params.length; i++)
    {
      String paramName  = params[i].getName();
      String mappedName = mappedParameters.get(paramName);
      if (mappedName == null)
      {
        Parameter p = fixedParameters.getParameter(paramName);
        if (p != null)
        {
          params[i].setValueFrom(p);
        }
      }
      else
      {
        Parameter p = parameters.getParameter(mappedName);
        if (p != null)
        {
          params[i].setValueFrom(p);
        }
      }
    }

    ParameterList jobParameters = new ParameterList(params);


    // Create the set of dependencies for the optimizing job.
    ArrayList<String> depList = new ArrayList<String>(dependencies.size());
    if ((externalDependencies != null) && (externalDependencies.length > 0))
    {
      for (int i=0; i < externalDependencies.length; i++)
      {
        depList.add(externalDependencies[i]);
      }
    }

    for (int i=0; i < dependencies.size(); i++)
    {
      String dependencyName = dependencies.get(i);

      Object o = scheduledJobs.get(dependencyName);
      if (o == null)
      {
        continue;
      }
      else if (o instanceof Job)
      {
        depList.add(((Job) o).getJobID());
      }
      else if (o instanceof OptimizingJob)
      {
        depList.add(((OptimizingJob) o).getOptimizingJobID());
      }
    }

    String[] dependencyArray = new String[depList.size()];
    depList.toArray(dependencyArray);


    // Create the requested client array.
    String[] clientArray = null;
    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      // FIXME -- Do we need to worry about the possibility of jobs running in
      // parallel within this job group?

      ArrayList<String> clientList = new ArrayList<String>(numClients);
      for (int i=0; ((i < numClients) && (i < requestedClients.length)); i++)
      {
        clientList.add(requestedClients[i]);
      }

      clientArray = new String[clientList.size()];
      clientList.toArray(clientArray);
    }


    // Get a reference to the scheduler and create the optimizing job ID.
    Scheduler scheduler       = slamdServer.getScheduler();
    String    optimizingJobID = scheduler.generateUniqueID();


    // Create the optimizing job using the information available.
    OptimizingJob optimizingJob =
         new OptimizingJob(slamdServer, optimizingJobID, optimizationAlgorithm,
                           jobClass, folderName, name, true, startTime,
                           duration, delayBetweenIterations, numClients,
                           clientArray, requestedMonitorClients,
                           monitorClientsIfAvailable, minThreads, maxThreads,
                           threadIncrement, collectionInterval, maxNonImproving,
                           null, reRunBestIteration, reRunDuration,
                           jobParameters, false);

    optimizingJob.setJobGroup(jobGroup.getName());
    optimizingJob.setDependencies(dependencyArray);
    optimizingJob.setThreadStartupDelay(threadStartupDelay);


    // Initialize the optimization algorithm.
    try
    {
      optimizationAlgorithm.initializeOptimizationAlgorithm(optimizingJob,
                                 optimizationParameters);
    }
    catch (InvalidValueException ive)
    {
      throw new SLAMDException("ERROR:  Failure while initializing the " +
                               "optimization algorithm for optimizing job " +
                               name + ":  " +  ive.getMessage());
    }


    // Schedule the optimizing job for execution.
    scheduler.scheduleOptimizingJob(optimizingJob, folderName);
    messages.add("Successfully scheduled optimizing job " + name +
                 " with optimizing job ID " + optimizingJobID);


    // Add the optimizing job to the job hash so it can be used as a dependency
    // for other jobs.
    scheduledJobs.put(name, optimizingJob);
  }



  /**
   * Encodes the information in this job group optimizing job to an ASN.1
   * element suitable for use in an encoded job group.
   *
   * @return  The ASN.1 element containing the encoded optimizing job
   *          information.
   */
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    elementList.add(new ASN1OctetString(ELEMENT_NAME));
    elementList.add(new ASN1OctetString(name));

    elementList.add(new ASN1OctetString(ELEMENT_JOB_CLASS));
    elementList.add(new ASN1OctetString(jobClass.getClass().getName()));

    if (duration > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_DURATION));
      elementList.add(new ASN1Integer(duration));
    }

    if (delayBetweenIterations > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_DELAY_BETWEEN_ITERATIONS));
      elementList.add(new ASN1Integer(delayBetweenIterations));
    }

    elementList.add(new ASN1OctetString(ELEMENT_NUM_CLIENTS));
    elementList.add(new ASN1Integer(numClients));

    elementList.add(new ASN1OctetString(ELEMENT_MIN_THREADS));
    elementList.add(new ASN1Integer(minThreads));

    if (maxThreads > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_MAX_THREADS));
      elementList.add(new ASN1Integer(maxThreads));
    }

    elementList.add(new ASN1OctetString(ELEMENT_THREAD_INCREMENT));
    elementList.add(new ASN1Integer(threadIncrement));

    elementList.add(new ASN1OctetString(ELEMENT_COLLECTION_INTERVAL));
    elementList.add(new ASN1Integer(collectionInterval));

    elementList.add(new ASN1OctetString(ELEMENT_MAX_NONIMPROVING));
    elementList.add(new ASN1Integer(maxNonImproving));

    if (threadStartupDelay > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_THREAD_STARTUP_DELAY));
      elementList.add(new ASN1Integer(threadStartupDelay));
    }

    elementList.add(new ASN1OctetString(ELEMENT_RERUN_BEST_ITERATION));
    elementList.add(new ASN1Boolean(reRunBestIteration));

    if (reRunDuration > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_RERUN_DURATION));
      elementList.add(new ASN1Integer(reRunDuration));
    }

    if ((dependencies != null) && (! dependencies.isEmpty()))
    {
      ArrayList<ASN1Element> depElements =
           new ArrayList<ASN1Element>(dependencies.size());
      for (int i=0; i < dependencies.size(); i++)
      {
        depElements.add(new ASN1OctetString(dependencies.get(i)));
      }

      elementList.add(new ASN1OctetString(ELEMENT_DEPENDENCIES));
      elementList.add(new ASN1Sequence(depElements));
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
           optimizationParameters.getParameters();
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

    if ((mappedParameters != null) && (! mappedParameters.isEmpty()))
    {
      ArrayList<ASN1Element> paramElements = new ArrayList<ASN1Element>();
      Iterator<String> iterator = mappedParameters.keySet().iterator();
      while (iterator.hasNext())
      {
        String jobName   = iterator.next();
        String groupName = mappedParameters.get(jobName);

        ASN1Element[] paramElementArray =
        {
          new ASN1OctetString(jobName),
          new ASN1OctetString(groupName)
        };

        paramElements.add(new ASN1Sequence(paramElementArray));
      }

      elementList.add(new ASN1OctetString(ELEMENT_MAPPED_PARAMS));
      elementList.add(new ASN1Sequence(paramElements));
    }

    if (fixedParameters != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_FIXED_PARAMS));
      elementList.add(fixedParameters.encode());
    }

    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the information in the provided element as a job group optimizing
   * job.
   *
   * @param  slamdServer            The SLAMD server instance to use in the
   *                                decoding process.
   * @param  jobGroup               The job group with which this optimizing job
   *                                is associated.
   * @param  encodedOptimizingJob   The encoded optimizing job information to
   *                                decode.
   *
   * @return  The decoded job group optimizing job.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the optimizing job information.
   */
  public static JobGroupOptimizingJob decode(SLAMDServer slamdServer,
                                             JobGroup jobGroup,
                                             ASN1Element encodedOptimizingJob)
         throws DecodeException
  {
    try
    {
      ArrayList<String> dependencies = new ArrayList<String>();
      LinkedHashMap<String,String> mappedParameters =
           new LinkedHashMap<String,String>();
      boolean               reRunBestIteration     = false;
      int                   collectionInterval     =
                                 Constants.DEFAULT_COLLECTION_INTERVAL;
      int                   delayBetweenIterations = 0;
      int                   duration               = -1;
      int                   maxNonImproving        = 1;
      int                   maxThreads             = -1;
      int                   minThreads             = 1;
      int                   numClients             = -1;
      int                   reRunDuration          = -1;
      int                   threadIncrement        = 1;
      int                   threadStartupDelay     = 0;
      JobClass              jobClass               = null;
      OptimizationAlgorithm optimizationAlgorithm  = null;
      ParameterList         fixedParameters        = new ParameterList();
      ParameterList         optimizationParameters = new ParameterList();
      String                name                   = null;

      ASN1Element[] elements =
           encodedOptimizingJob.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_NAME))
        {
          name = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          // FIXME -- Does this need to be able to handle classes that aren't
          // registered?
          String jobClassName =
               elements[i+1].decodeAsOctetString().getStringValue();
          jobClass = slamdServer.getJobClass(jobClassName);
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
        else if (elementName.equals(ELEMENT_COLLECTION_INTERVAL))
        {
          collectionInterval = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_MAX_NONIMPROVING))
        {
          maxNonImproving = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_THREAD_STARTUP_DELAY))
        {
          threadStartupDelay = elements[i+1].decodeAsInteger().getIntValue();
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
        else if (elementName.equals(ELEMENT_DEPENDENCIES))
        {
          ASN1Element[] depElements =
               elements[i+1].decodeAsSequence().getElements();
          for (int j=0; j < depElements.length; j++)
          {
            dependencies.add(
                 depElements[j].decodeAsOctetString().getStringValue());
          }
        }
        else if (elementName.equals(ELEMENT_OPTIMIZATION_ALGORITHM))
        {
          if (jobClass == null)
          {
            for (int j=i+2; j < elements.length; j += 2)
            {
              String elementName2 =
                   elements[j].decodeAsOctetString().getStringValue();
              if (elementName2.equals(ELEMENT_JOB_CLASS))
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
            String paramName =
                 paramElements[0].decodeAsOctetString().getStringValue();
            String paramValue =
                 paramElements[1].decodeAsOctetString().getStringValue();

            Parameter p = optimizationParameters.getParameter(paramName);
            if (p != null)
            {
              p.setValueFromString(paramValue);
            }
          }
        }
        else if (elementName.equals(ELEMENT_MAPPED_PARAMS))
        {
          ASN1Element[] paramElements =
               elements[i+1].decodeAsSequence().getElements();
          for (int j=0; j < paramElements.length; j++)
          {
            ASN1Element[] pElements =
                 paramElements[j].decodeAsSequence().getElements();
            mappedParameters.put(
                 pElements[0].decodeAsOctetString().getStringValue(),
                 pElements[1].decodeAsOctetString().getStringValue());
          }
        }
        else if (elementName.equals(ELEMENT_FIXED_PARAMS))
        {
          fixedParameters = ParameterList.decode(elements[i+1]);
        }
      }

      return new JobGroupOptimizingJob(name, jobGroup, jobClass, duration,
                                       delayBetweenIterations, numClients,
                                       minThreads, maxThreads, threadIncrement,
                                       collectionInterval, maxNonImproving,
                                       threadStartupDelay, reRunBestIteration,
                                       reRunDuration, dependencies,
                                       optimizationAlgorithm,
                                       optimizationParameters, mappedParameters,
                                       fixedParameters);
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode the job group optimizing " +
                                "job:  " + e, e);
    }
  }
}

