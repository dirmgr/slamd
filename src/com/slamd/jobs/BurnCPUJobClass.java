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
package com.slamd.jobs;



import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class defines a job that can attempt to drive one or more CPUs to 100%
 * utilization.  It does this by simply executing an infinite loop in each
 * thread.
 *
 *
 * @author   Neil A. Wilson
 */
public class BurnCPUJobClass
       extends JobClass
{
  // The value updated in the runJob method.
  private Double d;



  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public BurnCPUJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Burn CPU";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Generate high CPU utilization on client systems";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to generate high CPU utilization on client " +
      "systems.  It will do this by creating the specified number of threads " +
      "running tight loops.  On most systems, specifying a number of threads " +
      "greater than or equal to the number of available CPUs will cause CPU " +
      "utilization for that system to reach near 100%."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "Utility";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    return new ParameterList();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // No implementation is required.
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    while (! shouldStop())
    {
      d = 12345.0D;
      for (int i=0; i < 100000; i++)
      {
        d *= i;
      }
    }
  }
}

