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



import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD job that serves as a placeholder.  It will be used
 * for cases in which data exists in the SLAMD server but there is no
 * corresponding job class.  This will make it possible to at least see the
 * data in a limited form.
 *
 *
 * @author   Neil A. Wilson
 */
public class UnknownJobClass
       extends JobClass
{
  // The name of the class that was supposed to have been loaded.
  private final String intendedClassName;



  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   *
   * @param  intendedClassName  The name of the class that was supposed to have
   *                            been loaded.
   */
  public UnknownJobClass(String intendedClassName)
  {
    super();

    this.intendedClassName = intendedClassName;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Unknown Job Class " + intendedClassName;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Unknown job class";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "The data for this job is associated with an unknown or unavailable " +
      "job class (" + intendedClassName + ").  Limited functionality will be " +
      "available when interacting with it."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "Unknown";
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
    // No implementation necessary
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    logMessage("This class is not intended for use when executing SLAMD jobs.");
    indicateStoppedDueToError();
    return;
  }
}

