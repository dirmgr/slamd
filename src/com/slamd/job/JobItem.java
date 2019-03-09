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



/**
 * This class provides a common interface implemented by both jobs and
 * optimizing jobs.
 *
 *
 * @author   Neil A. Wilson
 */
public interface JobItem
{
  /**
   * Retrieves an instance of the job class with which this job is associated.
   *
   * @return  An instance of the job class with which this job is associated,
   *          or <CODE>null</CODE> if it could not be retrieved for some reason.
   */
  public JobClass getJobClass();



  /**
   * Retrieves the name of the job group with which this job is associated.
   *
   * @return  The name of the job group with which this job is associated, or
   *          <CODE>null</CODE> if it was not scheduled as part of any job
   *          group.
   */
  public String getJobGroup();



  /**
   * Retrieves the name of the folder in which this job is located.
   *
   * @return  The name of the folder in which this job is located.
   */
  public String getFolderName();
}

