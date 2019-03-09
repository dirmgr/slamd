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



import com.slamd.asn1.ASN1Element;
import com.slamd.job.JobClass;



/**
 * This class defines a common interface that is implemented by both job group
 * jobs and job group optimizing jobs.
 *
 *
 * @author   Neil A. Wilson
 */
public interface JobGroupItem
{
  /**
   * Retrieves the job group with which this job is associated.
   *
   * @return  The job group with which this job is associated.
   */
  public JobGroup getJobGroup();



  /**
   * Retrieves the human-readable name for this job.
   *
   * @return  The human-readable name for this job.
   */
  public String getName();



  /**
   * Retrieves the job class for this job.
   *
   * @return  The job class for this job.
   */
  public JobClass getJobClass();



  /**
   * Encodes the information in this job group job to an ASN.1 element suitable
   * for use in an encoded job group.
   *
   * @return  The ASN.1 element containing the encoded job information.
   */
  public ASN1Element encode();
}

