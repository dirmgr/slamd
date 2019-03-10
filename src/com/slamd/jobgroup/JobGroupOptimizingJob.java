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
package com.slamd.jobgroup;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.unboundid.asn1.ASN1Boolean;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1Integer;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;

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
public final class JobGroupOptimizingJob
       implements JobGroupItem
{
  /**
   * The name of the encoded element that holds the optimizing job name.
   */
  private static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that holds the job class name.
   */
  private static final String ELEMENT_JOB_CLASS = "job_class";



  /**
   * The name of the encoded element that holds the duration.
   */
  private static final String ELEMENT_DURATION = "duration";



  /**
   * The name of the encoded element that holds the delay between iterations.
   */
  private static final String ELEMENT_DELAY_BETWEEN_ITERATIONS =
       "delay_between_iterations";



  /**
   * The name of the encoded element that holds the number of clients.
   */
  private static final String ELEMENT_NUM_CLIENTS = "num_clients";



  /**
   * The name of the encoded element that holds the minimum number of threads
   * per client.
   */
  private static final String ELEMENT_MIN_THREADS = "min_threads";



  /**
   * The name of the encoded element that holds the maximum number of threads
   * per client.
   */
  private static final String ELEMENT_MAX_THREADS = "max_threads";



  /**
   * The name of the encoded element that holds the thread increment.
   */
  private static final String ELEMENT_THREAD_INCREMENT = "thread_increment";



  /**
   * The name of the encoded element that holds the statistics collection
   * interval.
   */
  private static final String ELEMENT_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the encoded element that holds the maximum number of
   * non-improving iterations.
   */
  private static final String ELEMENT_MAX_NONIMPROVING = "max_nonimproving";



  /**
   * The name of the encoded element that holds the thread startup delay.
   */
  private static final String ELEMENT_THREAD_STARTUP_DELAY =
       "thread_startup_delay";



  /**
   * The name of the encoded element that holds the flag indicating whether to
   * re-run the best iteration.
   */
  private static final String ELEMENT_RERUN_BEST_ITERATION =
       "rerun_best_iteration";



  /**
   * The name of the encoded element that holds the re-run duration.
   */
  private static final String ELEMENT_RERUN_DURATION = "rerun_duration";



  /**
   * The name of the encoded element that holds the set of dependencies for the
   * optimizing job.
   */
  private static final String ELEMENT_DEPENDENCIES = "dependencies";



  /**
   * The name of the encoded element that holds the optimization algorithm.
   */
  private static final String ELEMENT_OPTIMIZATION_ALGORITHM =
       "optimization_algorithm";



  /**
   * The name of the encoded element that holds the set of mapped parameters.
   */
  private static final String ELEMENT_MAPPED_PARAMS = "mapped_parameters";



  /**
   * The name of the encoded element that holds the set of fixed parameters.
   */
  private static final String ELEMENT_FIXED_PARAMS = "fixed_parameters";



  // Indicates whether to re-run the best iteration for this optimizing job.
  private boolean reRunBestIteration;

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

  // The set of dependencies for this optimizing job.  They will be the names of
  // other jobs or optimizing jobs in the job group on which this job is
  // dependent.
  private List<String> dependencies;

  // The optimizing job parameters that will be requested from the user, mapped
  // from the names used in the job group to the parameter stub for this
  // optimizing job.
  private Map<String,String> mappedParameters;

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
  public JobGroupOptimizingJob(final String name, final JobGroup jobGroup,
                               final JobClass jobClass, final int duration,
                               final int delayBetweenIterations,
                               final int numClients, final int minThreads,
                               final int maxThreads, final int threadIncrement,
                               final int collectionInterval,
                               final int maxNonImproving,
                               final int threadStartupDelay,
                               final boolean reRunBestIteration,
                               final int reRunDuration,
                               final List<String> dependencies,
                               final OptimizationAlgorithm optimizationAlgorithm,
                               final ParameterList optimizationParameters,
                               final Map<String,String> mappedParameters,
                               final ParameterList fixedParameters)
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
  @Override()
  public String getName()
  {
    return name;
  }



  /**
   * Specifies the human-readable name for this optimizing job.
   *
   * @param  name  The human-readable name for this optimizing job.
   */
  public void setName(final String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the job group for this optimizing job.
   *
   * @return  The job group for this optimizing job.
   */
  @Override()
  public JobGroup getJobGroup()
  {
    return jobGroup;
  }



  /**
   * Specifies the job group for this optimizing job.
   *
   * @param  jobGroup  The job group for this optimizing job.
   */
  public void setJobGroup(final JobGroup jobGroup)
  {
    this.jobGroup = jobGroup;
  }



  /**
   * Retrieves the job class for this optimizing job.
   *
   * @return  The job class for this optimizing job.
   */
  @Override()
  public JobClass getJobClass()
  {
    return jobClass;
  }



  /**
   * Specifies the job class for this optimizing job.
   *
   * @param  jobClass  The job class for this optimizing job.
   */
  public void setJobClass(final JobClass jobClass)
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
  public void setDuration(final int duration)
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
  public void setDelayBetweenIterations(final int delayBetweenIterations)
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
  public void setNumClients(final int numClients)
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
  public void setMinThreads(final int minThreads)
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
  public void setMaxThreads(final int maxThreads)
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
  public void setThreadIncrement(final int threadIncrement)
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
  public void setCollectionInterval(final int collectionInterval)
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
  public void setMaxNonImprovingIterations(final int maxNonImproving)
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
  public void setThreadStartupDelay(final int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
  }



  /**
   * Indicates whether to re-run the best iteration of this optimizing job.
   *
   * @return  {@code true} if the best iteration of this optimizing job
   *          should be re-run, or {@code false} if not.
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
  public void setReRunBestIteration(final boolean reRunBestIteration)
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
  public void setReRunDuration(final int reRunDuration)
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
  public List<String> getDependencies()
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
  public void setOptimizationAlgorithm(
                   final OptimizationAlgorithm optimizationAlgorithm)
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
  public void setOptimizationParameters(
                   final ParameterList optimizationParameters)
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
  public Map<String,String> getMappedParameters()
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
  public void schedule(final SLAMDServer slamdServer, final Date startTime,
                       final String folderName, final String[] requestedClients,
                       final String[] requestedMonitorClients,
                       final boolean monitorClientsIfAvailable,
                       final String[] externalDependencies,
                       final ParameterList parameters,
                       final Map<String,JobItem> scheduledJobs,
                       final List<String> messages)
         throws SLAMDException
  {
    // Create a new parameter list that combines the mapped parameters and the
    // fixed parameters.
    final Parameter[] params =
         jobClass.getParameterStubs().clone().getParameters();
    for (final Parameter param : params)
    {
      final String paramName = param.getName();
      final String mappedName = mappedParameters.get(paramName);
      if (mappedName == null)
      {
        final Parameter p = fixedParameters.getParameter(paramName);
        if (p != null)
        {
          param.setValueFrom(p);
        }
      }
      else
      {
        Parameter p = parameters.getParameter(mappedName);
        if (p != null)
        {
          param.setValueFrom(p);
        }
      }
    }

    final ParameterList jobParameters = new ParameterList(params);


    // Create the set of dependencies for the optimizing job.
    final ArrayList<String> depList = new ArrayList<>(dependencies.size());
    if ((externalDependencies != null) && (externalDependencies.length > 0))
    {
      depList.addAll(Arrays.asList(externalDependencies));
    }

    for (final String dependencyName : dependencies)
    {
      final Object o = scheduledJobs.get(dependencyName);
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

    final String[] dependencyArray = new String[depList.size()];
    depList.toArray(dependencyArray);


    // Create the requested client array.
    String[] clientArray = null;
    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      // FIXME -- Do we need to worry about the possibility of jobs running in
      // parallel within this job group?

      final ArrayList<String> clientList = new ArrayList<>(numClients);
      clientList.addAll(Arrays.asList(requestedClients));

      clientArray = new String[clientList.size()];
      clientList.toArray(clientArray);
    }


    // Get a reference to the scheduler and create the optimizing job ID.
    final Scheduler scheduler = slamdServer.getScheduler();
    final String optimizingJobID = scheduler.generateUniqueID();


    // Create the optimizing job using the information available.
    final OptimizingJob optimizingJob = new OptimizingJob(slamdServer,
         optimizingJobID, optimizationAlgorithm, jobClass, folderName, name,
         true, startTime, duration, delayBetweenIterations, numClients,
         clientArray, requestedMonitorClients, monitorClientsIfAvailable,
         minThreads, maxThreads, threadIncrement, collectionInterval,
         maxNonImproving, null, reRunBestIteration, reRunDuration,
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
           "optimization algorithm for optimizing job " + name + ":  " +
           ive.getMessage());
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
  @Override()
  public ASN1Element encode()
  {
    final ArrayList<ASN1Element> elementList = new ArrayList<>();

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
      final ArrayList<ASN1Element> depElements =
           new ArrayList<ASN1Element>(dependencies.size());
      for (final String dependency : dependencies)
      {
        depElements.add(new ASN1OctetString(dependency));
      }

      elementList.add(new ASN1OctetString(ELEMENT_DEPENDENCIES));
      elementList.add(new ASN1Sequence(depElements));
    }

    if (jobClass instanceof UnknownJobClass)
    {
      final ASN1Element[] optimizationAlgorithmElements = new ASN1Element[]
      {
        new ASN1OctetString(optimizationAlgorithm.getClass().getName()),
        new ASN1Sequence()
      };
      elementList.add(new ASN1OctetString(ELEMENT_OPTIMIZATION_ALGORITHM));
      elementList.add(new ASN1Sequence(optimizationAlgorithmElements));
    }
    else
    {
      final Parameter[] optimizationParams =
           optimizationParameters.getParameters();
      final ASN1Element[] optParamsElements =
           new ASN1Element[optimizationParams.length];
      for (int i=0; i < optimizationParams.length; i++)
      {
        final ASN1Element[] optParamElements = new ASN1Element[]
        {
          new ASN1OctetString(optimizationParams[i].getName()),
          new ASN1OctetString(optimizationParams[i].getValueString())
        };

        optParamsElements[i] = new ASN1Sequence(optParamElements);
      }
      final ASN1Element[] optimizationAlgorithmElements = new ASN1Element[]
      {
        new ASN1OctetString(optimizationAlgorithm.getClass().getName()),
        new ASN1Sequence(optParamsElements)
      };
      elementList.add(new ASN1OctetString(ELEMENT_OPTIMIZATION_ALGORITHM));
      elementList.add(new ASN1Sequence(optimizationAlgorithmElements));
    }

    if ((mappedParameters != null) && (! mappedParameters.isEmpty()))
    {
      final ArrayList<ASN1Element> paramElements = new ArrayList<>();
      for (final String jobName : mappedParameters.keySet())
      {
        final String groupName = mappedParameters.get(jobName);

        final ASN1Element[] paramElementArray =
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
  public static JobGroupOptimizingJob decode(final SLAMDServer slamdServer,
                     final JobGroup jobGroup,
                     final ASN1Element encodedOptimizingJob)
         throws DecodeException
  {
    try
    {
      final ArrayList<String> dependencies = new ArrayList<>();
      final LinkedHashMap<String,String> mappedParameters =
           new LinkedHashMap<>();
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

      final ASN1Element[] elements =
           encodedOptimizingJob.decodeAsSequence().elements();

      for (int i=0; i < elements.length; i += 2)
      {
        final String elementName =
             elements[i].decodeAsOctetString().stringValue();

        if (elementName.equals(ELEMENT_NAME))
        {
          name = elements[i+1].decodeAsOctetString().stringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          // FIXME -- Does this need to be able to handle classes that aren't
          // registered?
          final String jobClassName =
               elements[i+1].decodeAsOctetString().stringValue();
          jobClass = slamdServer.getJobClass(jobClassName);
        }
        else if (elementName.equals(ELEMENT_DURATION))
        {
          duration = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_DELAY_BETWEEN_ITERATIONS))
        {
          delayBetweenIterations =
               elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_NUM_CLIENTS))
        {
          numClients = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_MIN_THREADS))
        {
          minThreads = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_MAX_THREADS))
        {
          maxThreads = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_THREAD_INCREMENT))
        {
          threadIncrement = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_COLLECTION_INTERVAL))
        {
          collectionInterval = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_MAX_NONIMPROVING))
        {
          maxNonImproving = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_THREAD_STARTUP_DELAY))
        {
          threadStartupDelay = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_RERUN_BEST_ITERATION))
        {
          reRunBestIteration =
               elements[i+1].decodeAsBoolean().booleanValue();
        }
        else if (elementName.equals(ELEMENT_RERUN_DURATION))
        {
          reRunDuration = elements[i+1].decodeAsInteger().intValue();
        }
        else if (elementName.equals(ELEMENT_DEPENDENCIES))
        {
          final ASN1Element[] depElements =
               elements[i+1].decodeAsSequence().elements();
          for (final ASN1Element depElement : depElements)
          {
            dependencies.add(depElement.decodeAsOctetString().stringValue());
          }
        }
        else if (elementName.equals(ELEMENT_OPTIMIZATION_ALGORITHM))
        {
          if (jobClass == null)
          {
            for (int j=i+2; j < elements.length; j += 2)
            {
              final String elementName2 =
                   elements[j].decodeAsOctetString().stringValue();
              if (elementName2.equals(ELEMENT_JOB_CLASS))
              {
                final String className =
                     elements[j+1].decodeAsOctetString().stringValue();
                jobClass = slamdServer.getOrLoadJobClass(className);
                break;
              }
            }
          }

          final ASN1Element[] algorithmElements =
               elements[i+1].decodeAsSequence().elements();
          final String algorithmName =
               algorithmElements[0].decodeAsOctetString().stringValue();
          optimizationAlgorithm = (OptimizationAlgorithm)
               Constants.classForName(algorithmName).newInstance();

          optimizationParameters = optimizationAlgorithm.
               getOptimizationAlgorithmParameterStubs(jobClass).clone();
          final ASN1Element[] paramsElements =
               algorithmElements[1].decodeAsSequence().elements();
          for (final ASN1Element paramsElement : paramsElements)
          {
            final ASN1Element[] paramElements =
                 paramsElement.decodeAsSequence().elements();
            final String paramName =
                 paramElements[0].decodeAsOctetString().stringValue();
            final String paramValue =
                 paramElements[1].decodeAsOctetString().stringValue();

            final Parameter p = optimizationParameters.getParameter(paramName);
            if (p != null)
            {
              p.setValueFromString(paramValue);
            }
          }
        }
        else if (elementName.equals(ELEMENT_MAPPED_PARAMS))
        {
          final ASN1Element[] paramElements =
               elements[i+1].decodeAsSequence().elements();
          for (final ASN1Element paramElement : paramElements)
          {
            final ASN1Element[] pElements =
                 paramElement.decodeAsSequence().elements();
            mappedParameters.put(
                 pElements[0].decodeAsOctetString().stringValue(),
                 pElements[1].decodeAsOctetString().stringValue());
          }
        }
        else if (elementName.equals(ELEMENT_FIXED_PARAMS))
        {
          fixedParameters = ParameterList.decode(elements[i+1]);
        }
      }

      return new JobGroupOptimizingJob(name, jobGroup, jobClass, duration,
           delayBetweenIterations, numClients, minThreads, maxThreads,
           threadIncrement, collectionInterval, maxNonImproving,
           threadStartupDelay, reRunBestIteration, reRunDuration, dependencies,
           optimizationAlgorithm, optimizationParameters, mappedParameters,
           fixedParameters);
    }
    catch (final Exception e)
    {
      throw new DecodeException(
           "Unable to decode the job group optimizing " + "job:  " + e, e);
    }
  }
}

