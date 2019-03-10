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
import com.slamd.job.OptimizingJob;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.server.SLAMDServer;



/**
 * This class defines a data structure for holding information about a job that
 * is scheduled as part of a job group.
 *
 *
 * @author   Neil A. Wilson
 */
public final class JobGroupJob
       implements JobGroupItem
{
  /**
   * The name of the encoded element that holds the job name.
   */
  private static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that holds the job class name.
   */
  private static final String ELEMENT_JOB_CLASS = "job_class";



  /**
   * The name of the encoded element that holds the statistics collection
   * interval.
   */
  private static final String ELEMENT_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the encoded element that holds the job duration.
   */
  private static final String ELEMENT_DURATION = "duration";



  /**
   * The name of the encoded element that holds the number of clients.
   */
  private static final String ELEMENT_NUM_CLIENTS = "num_clients";



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
   * The name of the encoded element that holds the job dependencies.
   */
  private static final String ELEMENT_DEPENDENCIES = "dependencies";



  /**
   * The name of the encoded element that holds the set of mapped parameters.
   */
  private static final String ELEMENT_MAPPED_PARAMS = "mapped_parameters";



  /**
   * The name of the encoded element that holds the set of fixed parameters.
   */
  private static final String ELEMENT_FIXED_PARAMS = "fixed_parameters";



  // The set of dependencies for this job.  They will be the names of other jobs
  // or optimizing jobs in the job group on which this job is dependent.
  private List<String> dependencies;

  // The job parameters that will be requested from the user, mapped from the
  // names used in this job class to their names in the job group.
  private Map<String,String> mappedParameters;

  // The statistics collection interval for this job.
  private int collectionInterval;

  // The maximum length of time in seconds that this job should run.
  private int duration;

  // The number of clients to use to execute the job.
  private int numClients;

  // The number of threads per client to use to execute the job.
  private int threadsPerClient;

  // The thread startup delay in milliseconds for this job.
  private int threadStartupDelay;

  // The job class for this job.
  private JobClass jobClass;

  // The job group with which this job is associated.
  private JobGroup jobGroup;

  // The fixed-value parameters that will always be used for this job in the job
  // group.
  private ParameterList fixedParameters;

  // A name used to identify this job in the job group.
  private String name;



  /**
   * Creates a new job group job with the provided information.
   *
   * @param  jobGroup            The job group with which this job is
   *                             associated.
   * @param  name                The human-readable name for this job.
   * @param  jobClass            The job class for this job.
   * @param  duration            The duration for this job.
   * @param  collectionInterval  The collection interval for this job.
   * @param  numClients          The number of clients to use to run this job.
   * @param  threadsPerClient    The number of threads per client to use to run
   *                             this job.
   * @param  threadStartupDelay  The thread startup delay in milliseconds for
   *                             this job.
   * @param  dependencies        The set of dependencies for this job.
   * @param  mappedParameters    The set of mapped parameters for this job,
   *                             mapped from the names used in the associated
   *                             job class to the names used in the job group.
   * @param  fixedParameters     The set of fixed parameters for this job.
   */
  public JobGroupJob(final JobGroup jobGroup, final String name,
                     final JobClass jobClass, final int duration,
                     final int collectionInterval, final int numClients,
                     final int threadsPerClient, final int threadStartupDelay,
                     final List<String> dependencies,
                     final Map<String,String> mappedParameters,
                     final ParameterList fixedParameters)
  {
    this.jobGroup           = jobGroup;
    this.name               = name;
    this.jobClass           = jobClass;
    this.duration           = duration;
    this.collectionInterval = collectionInterval;
    this.numClients         = numClients;
    this.threadsPerClient   = threadsPerClient;
    this.threadStartupDelay = threadStartupDelay;
    this.dependencies       = dependencies;
    this.mappedParameters   = mappedParameters;
    this.fixedParameters    = fixedParameters;
  }



  /**
   * Retrieves the job group with which this job is associated.
   *
   * @return  The job group with which this job is associated.
   */
  @Override()
  public JobGroup getJobGroup()
  {
    return jobGroup;
  }



  /**
   * Retrieves the human-readable name for this job.
   *
   * @return  The human-readable name for this job.
   */
  @Override()
  public String getName()
  {
    return name;
  }



  /**
   * Specifies the human-readable name for this job.
   *
   * @param  name  The human-readable name for this job.
   */
  public void setName(final String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the job class for this job.
   *
   * @return  The job class for this job.
   */
  @Override()
  public JobClass getJobClass()
  {
    return jobClass;
  }



  /**
   * Specifies the job class for this job.
   *
   * @param  jobClass  The job class for this job.
   */
  public void setJobClass(final JobClass jobClass)
  {
    this.jobClass = jobClass;
  }



  /**
   * Retrieves the duration for this job.
   *
   * @return  The duration for this job.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Specifies the duration for this job.
   *
   * @param  duration  The duration for this job.
   */
  public void setDuration(final int duration)
  {
    this.duration = duration;
  }



  /**
   * Retrieves the collection interval for this job.
   *
   * @return  The collection interval for this job.
   */
  public int getCollectionInterval()
  {
    return collectionInterval;
  }



  /**
   * Specifies the collection interval for this job.
   *
   * @param  collectionInterval  The collection interval for this job.
   */
  public void setCollectionInterval(final int collectionInterval)
  {
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves the number of clients for this job.
   *
   * @return  The number of clients for this job.
   */
  public int getNumClients()
  {
    return numClients;
  }



  /**
   * Specifies the number of clients for this job.
   *
   * @param  numClients  The number of clients for this job.
   */
  public void setNumClients(final int numClients)
  {
    this.numClients = numClients;
  }



  /**
   * Retrieves the number of threads per client for this job.
   *
   * @return  The number of threads per client for this job.
   */
  public int getThreadsPerClient()
  {
    return threadsPerClient;
  }



  /**
   * Specifies the number of threads per client for this job.
   *
   * @param  threadsPerClient  The number of threads per client for this job.
   */
  public void setThreadsPerClient(final int threadsPerClient)
  {
    this.threadsPerClient = threadsPerClient;
  }



  /**
   * Retrieves the thread startup delay for this job.
   *
   * @return  The thread startup delay for this job.
   */
  public int getThreadStartupDelay()
  {
    return threadStartupDelay;
  }



  /**
   * Specifies the thread startup delay for this job.
   *
   * @param  threadStartupDelay  The thread startup delay for this job.
   */
  public void setThreadStartupDelay(final int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
  }



  /**
   * Retrieves the set of dependencies for this job.  The contents of the
   * returned list may be altered by the caller.
   *
   * @return  The set of dependencies for this job.
   */
  public List<String> getDependencies()
  {
    return dependencies;
  }



  /**
   * Retrieves the set of mapped parameters for this job, mapped from the names
   * used in the associated job class to the names used in the job group.  The
   * contents of the returned map may be altered by the caller.
   *
   * @return  The set of mapped parameters for this job.
   */
  public Map<String,String> getMappedParameters()
  {
    return mappedParameters;
  }



  /**
   * Retrieves the set of fixed parameters for this job.  The contents of the
   * returned parameter list may be altered by the caller.
   *
   * @return  The set of fixed parameters for this job.
   */
  public ParameterList getFixedParameters()
  {
    return fixedParameters;
  }




  /**
   * Schedules this job for execution by the SLAMD server using the provided
   * information.
   *
   * @param  slamdServer                The reference to the SLAMD server to use
   *                                    when scheduling the job.
   * @param  startTime                  The start time to use for the job.
   * @param  folderName                 The name of the folder into which the
   *                                    job should be placed.
   * @param  requestedClients           The set of clients that have been
   *                                    requested for this job group.
   * @param  requestedMonitorClients    the set of resource monitor clients that
   *                                    have been requested for this job group.
   * @param  monitorClientsIfAvailable  Indicates whether the clients used to
   *                                    run the job should be monitored if there
   *                                    are also resource monitor clients
   *                                    running on the same system.
   * @param  externalDependencies       A set of jobs outside of this job group
   *                                    on which the scheduled job should be
   *                                    dependent.
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
   *                                    scheduling the job.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to schedule
   *                          the job.
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
      final String paramName  = param.getName();
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
        final Parameter p = parameters.getParameter(mappedName);
        if (p != null)
        {
          param.setValueFrom(p);
        }
      }
    }

    final ParameterList jobParameters = new ParameterList(params);


    // Create the set of dependencies for the job.
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


    // Execute the validateJobInfo method for the job class.  This is necessary
    // to ensure that certain background processing that might be required gets
    // done.
    try
    {
      jobClass.validateJobInfo(numClients, threadsPerClient, threadStartupDelay,
           startTime, null, duration, collectionInterval, jobParameters);
    }
    catch (InvalidValueException ive)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
           JobClass.stackTraceToString(ive));

      messages.add("WARNING:  validateJobInfo for job " + name +
           " failed with message:  " + ive.getMessage());
    }


    // Create the job using the information available.
    final Job job = new Job(slamdServer, jobClass.getClass().getName(),
         numClients, threadsPerClient, threadStartupDelay, startTime, null,
         duration, collectionInterval, jobParameters, false);

    job.setJobDescription(name);
    job.setJobGroup(jobGroup.getName());
    job.setDependencies(dependencyArray);
    job.setFolderName(folderName);
    job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
    job.setWaitForClients(true);

    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      // FIXME -- Do we need to worry about the possibility of jobs running in
      // parallel within this job group?

      final ArrayList<String> clientList = new ArrayList<>(numClients);
      for (int i=0; ((i < numClients) && (i < requestedClients.length)); i++)
      {
        clientList.add(requestedClients[i]);
      }

      final String[] clientArray = new String[clientList.size()];
      clientList.toArray(clientArray);
      job.setRequestedClients(clientArray);
    }

    if ((requestedMonitorClients != null) &&
        (requestedMonitorClients.length > 0))
    {
      job.setResourceMonitorClients(requestedMonitorClients);
    }


    // Schedule the job for execution.
    slamdServer.getScheduler().scheduleJob(job, folderName);
    messages.add("Successfully scheduled job " + name + " with job ID " +
         job.getJobID());


    // Add the job to the job hash so it can be used as a dependency for other
    // jobs.
    scheduledJobs.put(name, job);
  }



  /**
   * Encodes the information in this job group job to an ASN.1 element suitable
   * for use in an encoded job group.
   *
   * @return  The ASN.1 element containing the encoded job information.
   */
  @Override()
  public ASN1Element encode()
  {
    final ArrayList<ASN1Element> elementList = new ArrayList<>();

    elementList.add(new ASN1OctetString(ELEMENT_NAME));
    elementList.add(new ASN1OctetString(name));

    elementList.add(new ASN1OctetString(ELEMENT_JOB_CLASS));
    elementList.add(new ASN1OctetString(jobClass.getClass().getName()));

    elementList.add(new ASN1OctetString(ELEMENT_COLLECTION_INTERVAL));
    elementList.add(new ASN1Integer(collectionInterval));

    if (duration > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_DURATION));
      elementList.add(new ASN1Integer(duration));
    }

    elementList.add(new ASN1OctetString(ELEMENT_NUM_CLIENTS));
    elementList.add(new ASN1Integer(numClients));

    elementList.add(new ASN1OctetString(ELEMENT_THREADS_PER_CLIENT));
    elementList.add(new ASN1Integer(threadsPerClient));

    if (threadStartupDelay > 0)
    {
      elementList.add(new ASN1OctetString(ELEMENT_THREAD_STARTUP_DELAY));
      elementList.add(new ASN1Integer(threadStartupDelay));
    }

    if ((dependencies != null) && (! dependencies.isEmpty()))
    {
      final ArrayList<ASN1Element> depElements =
           new ArrayList<>(dependencies.size());
      for (int i=0; i < dependencies.size(); i++)
      {
        depElements.add(new ASN1OctetString(dependencies.get(i)));
      }

      elementList.add(new ASN1OctetString(ELEMENT_DEPENDENCIES));
      elementList.add(new ASN1Sequence(depElements));
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
   * Decodes the information in the provided element as a job group job.
   *
   * @param  slamdServer  The SLAMD server instance to use in the decoding
   *                      process.
   * @param  jobGroup     The job group with which this job is associated.
   * @param  encodedJob   The encoded job information to decode.
   *
   * @return  The decoded job group job.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the job information.
   */
  public static JobGroupJob decode(final SLAMDServer slamdServer,
                                   final JobGroup jobGroup,
                                   final ASN1Element encodedJob)
         throws DecodeException
  {
    try
    {
      final ArrayList<String> dependencies = new ArrayList<>();
      final LinkedHashMap<String,String> mappedParameters =
           new LinkedHashMap<>();
      int           collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
      int           duration           = -1;
      int           numClients         = -1;
      int           threadsPerClient   = -1;
      int           threadStartupDelay = 0;
      JobClass      jobClass           = null;
      ParameterList fixedParameters    = new ParameterList();
      String        name               = null;

      final ASN1Element[] elements =
           encodedJob.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        final String elementName =
             elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_NAME))
        {
          name = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_CLASS))
        {
          // FIXME -- Does this need to be able to handle classes that aren't
          // registered?
          final String jobClassName =
               elements[i+1].decodeAsOctetString().getStringValue();
          jobClass = slamdServer.getJobClass(jobClassName);
        }
        else if (elementName.equals(ELEMENT_COLLECTION_INTERVAL))
        {
          collectionInterval = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_DURATION))
        {
          duration = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_NUM_CLIENTS))
        {
          numClients = elements[i+1].decodeAsInteger().getIntValue();
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
          final ASN1Element[] depElements =
               elements[i+1].decodeAsSequence().getElements();
          for (final ASN1Element depElement : depElements)
          {
            dependencies.add(depElement.decodeAsOctetString().getStringValue());
          }
        }
        else if (elementName.equals(ELEMENT_MAPPED_PARAMS))
        {
          final ASN1Element[] paramElements =
               elements[i+1].decodeAsSequence().getElements();
          for (final ASN1Element paramElement : paramElements)
          {
            final ASN1Element[] pElements =
                 paramElement.decodeAsSequence().getElements();
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

      return new JobGroupJob(jobGroup, name, jobClass, duration,
           collectionInterval, numClients, threadsPerClient, threadStartupDelay,
           dependencies, mappedParameters, fixedParameters);
    }
    catch (final Exception e)
    {
      throw new DecodeException("Unable to decode the job group job:  " + e, e);
    }
  }
}

