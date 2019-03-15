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
package com.slamd.jobs.utility;



import java.util.Date;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD job that does nothing.  It simply provides the
 * capability to insert a delay between jobs.
 *
 *
 * @author   Neil A. Wilson
 */
public final class NoOpJob
       extends JobClass
{
  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the {@code initialize} method.
   */
  public NoOpJob()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "No-Op";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "This job does nothing.  It can be used to inject a delay between " +
         "jobs.";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to insert a delay between other jobs.  It does " +
      "not perform any processing, and it does not collect any statistics."
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
  public StatTracker[] getStatTrackerStubs(final String clientID,
                                           final String threadID,
                                           final int collectionInterval)
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
  public void validateJobInfo(final int numClients,
                              final int threadsPerClient,
                              final int threadStartupDelay,
                              final Date startTime, final Date stopTime,
                              final int duration, final int collectionInterval,
                              final ParameterList parameters)
         throws InvalidValueException
  {
    // Either a duration or a stop time must have been specified.
    if ((duration <= 0) && (stopTime == null))
    {
      throw new InvalidValueException("Null jobs will never stop on their " +
           "own and must be scheduled with a duration or stop time.");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(final String clientID, final String threadID,
                               final int collectionInterval,
                               final ParameterList parameters)
         throws UnableToRunException
  {
    // No implementation necessary
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Loop until it is determined that the job should stop.
    while (! shouldStop())
    {
      try
      {
        Thread.sleep(100);
      } catch (InterruptedException ie) {}
    }
  }
}

