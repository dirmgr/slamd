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
public class JobGroupJob
       implements JobGroupItem
{
  /**
   * The name of the encoded element that holds the job name.
   */
  public static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that holds the job class name.
   */
  public static final String ELEMENT_JOB_CLASS = "job_class";



  /**
   * The name of the encoded element that holds the statistics collection
   * interval.
   */
  public static final String ELEMENT_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the encoded element that holds the job duration.
   */
  public static final String ELEMENT_DURATION = "duration";



  /**
   * The name of the encoded element that holds the number of clients.
   */
  public static final String ELEMENT_NUM_CLIENTS = "num_clients";



  /**
   * The name of the encoded element that holds the number of threads per
   * client.
   */
  public static final String ELEMENT_THREADS_PER_CLIENT = "threads_per_client";



  /**
   * The name of the encoded element that holds the thread startup delay.
   */
  public static final String ELEMENT_THREAD_STARTUP_DELAY =
       "thread_startup_delay";



  /**
   * The name of the encoded element that holds the job dependencies.
   */
  public static final String ELEMENT_DEPENDENCIES = "dependencies";



  /**
   * The name of the encoded element that holds the set of mapped parameters.
   */
  public static final String ELEMENT_MAPPED_PARAMS = "mapped_parameters";



  /**
   * The name of the encoded element that holds the set of fixed parameters.
   */
  public static final String ELEMENT_FIXED_PARAMS = "fixed_parameters";



  // The set of dependencies for this job.  They will be the names of other jobs
  // or optimizing jobs in the job group on which this job is dependent.
  private ArrayList<String> dependencies;

  // The job parameters that will be requested from the user, mapped from the
  // names used in this job class to their names in the job group.
  private LinkedHashMap<String,String> mappedParameters;

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
  public JobGroupJob(JobGroup jobGroup, String name, JobClass jobClass,
                     int duration, int collectionInterval, int numClients,
                     int threadsPerClient, int threadStartupDelay,
                     ArrayList<String> dependencies,
                     LinkedHashMap<String,String> mappedParameters,
                     ParameterList fixedParameters)
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
  public JobGroup getJobGroup()
  {
    return jobGroup;
  }



  /**
   * Retrieves the human-readable name for this job.
   *
   * @return  The human-readable name for this job.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Specifies the human-readable name for this job.
   *
   * @param  name  The human-readable name for this job.
   */
  public void setName(String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the job class for this job.
   *
   * @return  The job class for this job.
   */
  public JobClass getJobClass()
  {
    return jobClass;
  }



  /**
   * Specifies the job class for this job.
   *
   * @param  jobClass  The job class for this job.
   */
  public void setJobClass(JobClass jobClass)
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
  public void setDuration(int duration)
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
  public void setCollectionInterval(int collectionInterval)
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
  public void setNumClients(int numClients)
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
  public void setThreadsPerClient(int threadsPerClient)
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
  public void setThreadStartupDelay(int threadStartupDelay)
  {
    this.threadStartupDelay = threadStartupDelay;
  }



  /**
   * Retrieves the set of dependencies for this job.  The contents of the
   * returned list may be altered by the caller.
   *
   * @return  The set of dependencies for this job.
   */
  public ArrayList<String> getDependencies()
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
  public LinkedHashMap<String,String> getMappedParameters()
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


    // Create the set of dependencies for the job.
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


    // Execute the validateJobInfo method for the job class.  This is necessary
    // to ensure that certain background processing that might be required gets
    // done.
    try
    {
      jobClass.validateJobInfo(numClients, threadsPerClient, threadStartupDelay,
                               startTime, null, duration, collectionInterval,
                               jobParameters);
    }
    catch (InvalidValueException ive)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ive));

      messages.add("WARNING:  validateJobInfo for job " + name +
                   " failed with message:  " + ive.getMessage());
    }


    // Create the job using the information available.
    Job job = new Job(slamdServer, jobClass.getClass().getName(), numClients,
                      threadsPerClient, threadStartupDelay, startTime, null,
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

      ArrayList<String> clientList = new ArrayList<String>(numClients);
      for (int i=0; ((i < numClients) && (i < requestedClients.length)); i++)
      {
        clientList.add(requestedClients[i]);
      }

      String[] clientArray = new String[clientList.size()];
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
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

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
      ArrayList<ASN1Element> depElements =
           new ArrayList<ASN1Element>(dependencies.size());
      for (int i=0; i < dependencies.size(); i++)
      {
        depElements.add(new ASN1OctetString(dependencies.get(i)));
      }

      elementList.add(new ASN1OctetString(ELEMENT_DEPENDENCIES));
      elementList.add(new ASN1Sequence(depElements));
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
  public static JobGroupJob decode(SLAMDServer slamdServer, JobGroup jobGroup,
                                   ASN1Element encodedJob)
         throws DecodeException
  {
    try
    {
      ArrayList<String> dependencies = new ArrayList<String>();
      LinkedHashMap<String,String> mappedParameters =
           new LinkedHashMap<String,String>();
      int           collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
      int           duration           = -1;
      int           numClients         = -1;
      int           threadsPerClient   = -1;
      int           threadStartupDelay = 0;
      JobClass      jobClass           = null;
      ParameterList fixedParameters    = new ParameterList();
      String        name               = null;

      ASN1Element[] elements = encodedJob.decodeAsSequence().getElements();

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
          ASN1Element[] depElements =
               elements[i+1].decodeAsSequence().getElements();
          for (int j=0; j < depElements.length; j++)
          {
            dependencies.add(
                 depElements[j].decodeAsOctetString().getStringValue());
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

      return new JobGroupJob(jobGroup, name, jobClass, duration,
                             collectionInterval, numClients, threadsPerClient,
                             threadStartupDelay, dependencies, mappedParameters,
                             fixedParameters);
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode the job group job:  " + e, e);
    }
  }
}

