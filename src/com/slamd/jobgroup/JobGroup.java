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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;
import com.slamd.db.DecodeException;
import com.slamd.job.JobItem;
import com.slamd.parameter.ParameterList;
import com.slamd.server.SLAMDServer;



/**
 * This class defines a data structure for representing a job group.  A job
 * group is a collection of jobs that may be scheduled as a single entity.  In
 * particular, a job group consists of the following:
 * <UL>
 *   <LI>A human-readable name.</LI>
 *   <LI>A human-readable description.</LI>
 *   <LI>A set of parameters that will be requested when scheduling the job
 *       group.</LI>
 *   <LI>A list of jobs and optimizing jobs contained in this group.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public final class JobGroup
{
  /**
   * The name of the encoded element that holds the job group name.
   */
  private static final String ELEMENT_NAME = "job_group_name";



  /**
   * The name of the encoded element that holds the job group description.
   */
  private static final String ELEMENT_DESCRIPTION = "job_group_description";



  /**
   * The name of the encoded element that holds the job group parameter list.
   */
  private static final String ELEMENT_PARAMETERS = "job_group_parameters";



  /**
   * The name of the encoded element that holds the job list.
   */
  private static final String ELEMENT_JOB_LIST = "job_list";



  /**
   * The name of the encoded element that holds a job in the job list.
   */
  private static final String ELEMENT_JOB_GROUP_JOB = "job_group_job";



  /**
   * The name of the encoded element that holds an optimizing job in the job
   * list.
   */
  private static final String ELEMENT_JOB_GROUP_OPTIMIZING_JOB =
       "job_group_optimizing_job";



  // The set of jobs and optimizing jobs contained in this job group.
  private List<JobGroupItem> jobList;

  // The set of parameters associated with this job group.
  private ParameterList parameters;

  // The human-readable description for this job group.
  private String description;

  // The human-readable name for this job group.
  private String name;



  /**
   * Creates a new job group with the provided information.
   *
   * @param  name         The human-readable name for this job group.
   * @param  description  The human-readable description for this job group.
   */
  public JobGroup(final String name, final String description)
  {
    this.name        = name;
    this.description = description;

    jobList    = new ArrayList<>();
    parameters = new ParameterList();
  }



  /**
   * Retrieves the human-readable name for this job group.
   *
   * @return  The human-readable name for this job group.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Specifies the human-readable name for this job group.
   *
   * @param  name  The human-readable name for this job group.
   */
  public void setName(final String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the human-readable description for this job group.
   *
   * @return  The human-readable description for this job group.
   */
  public String getDescription()
  {
    return description;
  }



  /**
   * Specifies the human-readable description for this job group.
   *
   * @param  description  The human-readable description for this job group.
   */
  public void setDescription(final String description)
  {
    this.description = description;
  }



  /**
   * Retrieves the set of parameters for this job group.  The resulting set may
   * be altered by the caller.
   *
   * @return  The set of parameters for this job group.
   */
  public ParameterList getParameters()
  {
    return parameters;
  }



  /**
   * Specifies the set of parameters for this job group.  This method must be
   * called with care because leaving out required parameters could leave the
   * group unusable.
   *
   * @param  parameters  The set of parameters for this job group.
   */
  public void setParameters(final ParameterList parameters)
  {
    this.parameters = parameters;
  }



  /**
   * Retrieves the set of jobs and optimizing jobs for this job group.  The
   * elements of this list may be either <CODE>JobGroupJob</CODE> or
   * <CODE>JobGroupOptimizingJob</CODE> objects.  The contents of this list may
   * be altered by the caller.
   *
   * @return  The set of jobs and optimizing jobs for this job group.
   */
  public List<JobGroupItem> getJobList()
  {
    return jobList;
  }



  /**
   * Schedules all of the jobs and optimizing jobs that are part of this job
   * group for execution using the provided information.
   *
   * @param  slamdServer                The reference to the SLAMD server to use
   *                                    when scheduling the associated jobs and
   *                                    optimizing jobs.
   * @param  startTime                  The start time to use for the jobs and
   *                                    optimizing jobs that are scheduled.
   * @param  folderName                 The name of the folder into which the
   *                                    scheduled jobs and optimizing jobs
   *                                    should be placed.
   * @param  requestedClients           The set of clients that have been
   *                                    requested for this job group.
   * @param  requestedMonitorClients    the set of resource monitor clients that
   *                                    have been requested for this job group.
   * @param  monitorClientsIfAvailable  Indicates whether the clients used to
   *                                    run the job should be monitored if there
   *                                    are also resource monitor clients
   *                                    running on the same system.
   * @param  dependencies               A set of IDs of jobs or optimizing jobs
   *                                    that should be used as dependencies for
   *                                    all jobs and optimizing jobs scheduled
   *                                    as part of this job group.
   * @param  parameters                 The set of parameters provided for this
   *                                    job group.
   * @param  messages                   A list for use in holding any messages
   *                                    that should be provided to the user as a
   *                                    result of scheduling this job group.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to schedule
   *                          any of the jobs or optimizing jobs in this job
   *                          group.  Any jobs or optimizing jobs that had been
   *                          scheduled will remain scheduled.
   */
  public void schedule(final SLAMDServer slamdServer, final Date startTime,
                       final String folderName,
                       final String[] requestedClients,
                       final String[] requestedMonitorClients,
                       final boolean monitorClientsIfAvailable,
                       final String[] dependencies,
                       final ParameterList parameters,
                       final List<String> messages)
         throws SLAMDException
  {
    Map<String,JobItem> scheduledJobs = new LinkedHashMap<>();
    for (final JobGroupItem item : jobList)
    {
      if (item instanceof JobGroupJob)
      {
        ((JobGroupJob) item).schedule(slamdServer, startTime, folderName,
             requestedClients, requestedMonitorClients,
             monitorClientsIfAvailable, dependencies, parameters, scheduledJobs,
             messages);
      }
      else if (item instanceof JobGroupOptimizingJob)
      {
        ((JobGroupOptimizingJob) item).schedule(slamdServer, startTime,
             folderName, requestedClients, requestedMonitorClients,
             monitorClientsIfAvailable, dependencies, parameters, scheduledJobs,
             messages);
      }
    }
  }



  /**
   * Encodes this job group to a byte array suitable for storing in the
   * configuration repository.
   *
   * @return  The byte array containing the encoded job group data.
   */
  public byte[] encode()
  {
    final ArrayList<ASN1Element> elementList = new ArrayList<>();

    elementList.add(new ASN1OctetString(ELEMENT_NAME));
    elementList.add(new ASN1OctetString(name));

    if (description != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_DESCRIPTION));
      elementList.add(new ASN1OctetString(description));
    }

    if (parameters != null)
    {
      elementList.add(new ASN1OctetString(ELEMENT_PARAMETERS));
      elementList.add(parameters.encode());
    }

    if ((jobList != null) && (! jobList.isEmpty()))
    {
      final ArrayList<ASN1Element> jobElements = new ArrayList<>();
      for (final JobGroupItem item : jobList)
      {
        if (item instanceof JobGroupJob)
        {
          jobElements.add(new ASN1OctetString(ELEMENT_JOB_GROUP_JOB));
          jobElements.add(item.encode());
        }
        else if (item instanceof JobGroupOptimizingJob)
        {
          jobElements.add(new ASN1OctetString(
                                   ELEMENT_JOB_GROUP_OPTIMIZING_JOB));
          jobElements.add(item.encode());
        }
      }

      elementList.add(new ASN1OctetString(ELEMENT_JOB_LIST));
      elementList.add(new ASN1Sequence(jobElements));
    }


    final ASN1Element[] elements = new ASN1Element[elementList.size()];
    elementList.toArray(elements);
    return new ASN1Sequence(elements).encode();
  }



  /**
   * Decodes the contents of the provided byte array as a job group.
   *
   * @param  slamdServer      The reference to the SLAMD server to use in the
   *                          decoding process.
   * @param  encodedJobGroup  The byte array containing the encoded job group
   *                          data.
   *
   * @return  The decoded job group.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the job group data.
   */
  public static JobGroup decode(final SLAMDServer slamdServer,
                                final byte[] encodedJobGroup)
         throws DecodeException
  {
    try
    {
      final JobGroup jobGroup = new JobGroup(null, null);
      final ArrayList<JobGroupItem> jobList = new ArrayList<>();

      final ASN1Element   element  = ASN1Element.decode(encodedJobGroup);
      final ASN1Element[] elements = element.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        final String elementName =
             elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_NAME))
        {
          jobGroup.setName(
               elements[i+1].decodeAsOctetString().getStringValue());
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          jobGroup.setDescription(
               elements[i+1].decodeAsOctetString().getStringValue());
        }
        else if (elementName.equals(ELEMENT_PARAMETERS))
        {
          jobGroup.parameters = ParameterList.decode(elements[i+1]);
        }
        else if (elementName.equals(ELEMENT_JOB_LIST))
        {
          final ASN1Element[] listElements =
               elements[i+1].decodeAsSequence().getElements();
          for (int j=0; j < listElements.length; j += 2)
          {
            final String elementName2 =
                 listElements[j].decodeAsOctetString().getStringValue();

            if (elementName2.equals(ELEMENT_JOB_GROUP_JOB))
            {
              jobList.add(JobGroupJob.decode(slamdServer, jobGroup,
                   listElements[j+1]));
            }
            else if (elementName2.equals(ELEMENT_JOB_GROUP_OPTIMIZING_JOB))
            {
              jobList.add(JobGroupOptimizingJob.decode(slamdServer, jobGroup,
                   listElements[j+1]));
            }
          }
        }
      }

      jobGroup.jobList = jobList;

      return jobGroup;
    }
    catch (final Exception e)
    {
      throw new DecodeException("Unable to decode job group:  " + e, e);
    }
  }



  /**
   * Decodes the contents of the provided byte array as a job group.  Only the
   * name and description will actually be decoded.
   *
   * @param  encodedJobGroup  The byte array containing the encoded job group
   *                          data.
   *
   * @return  The decoded summary job group.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the job group summary data.
   */
  public static JobGroup decodeSummary(final byte[] encodedJobGroup)
         throws DecodeException
  {
    try
    {
      String name = null;
      String description = null;

      final ASN1Element element = ASN1Element.decode(encodedJobGroup);
      final ASN1Element[] elements = element.decodeAsSequence().getElements();

      for (int i=0; i < elements.length; i += 2)
      {
        final String elementName =
             elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_NAME))
        {
          name = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          description = elements[i+1].decodeAsOctetString().getStringValue();
        }
      }

      return new JobGroup(name, description);
    }
    catch (final Exception e)
    {
      throw new DecodeException("Unable to decode job group:  " + e, e);
    }
  }
}

