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
package com.slamd.loadvariance;



import com.slamd.common.SLAMDException;



/**
 * This class defines an abstract algorithm that may be used to vary the load
 * over time while a job is running by controlling the number of threads that
 * are active at any given time.  Actual subclasses must implement any abstract
 * methods defined in this class.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class LoadVarianceAlgorithm
{
  /**
   * This constructor is used to create a new instance of this load variation
   * algorithm through reflection.  A default constructor must be provided in
   * all subclasses, but the only thing that it needs to do is call
   * <CODE>super()</CODE>.
   */
  public LoadVarianceAlgorithm()
  {
    // No implementation required.
  }



  /**
   * Initializes this load variation algorithm based on the provided list of
   * arguments.
   *
   * @param  arguments  The arguments that may be used to customize the behavior
   *                    of this load variation algorithm.
   *
   * @throws  SLAMDException  If a problem occurs while trying to initialize
   *                          this load variation algorithm.
   */
  public abstract void initializeVariationAlgorithm(String[] arguments)
         throws SLAMDException;



  /**
   * Retrieves a two-dimensional array that provides information about the
   * increase or decrease in active job threads that should be applied over
   * time.  Each element of the array returned should itself be a two-element
   * array with the first element being the number of milliseconds since the
   * start of this load variance instruction that the increase or decrease
   * should occur, and the second is an integer value that indicates the number
   * of threads that should be added at that time (may be negative if threads
   * are to be removed).
   *
   * @param  duration       The length of time in seconds over which this load
   *                        variance algorithm should operate.
   * @param  totalThreads   The total number of threads that have been scheduled
   *                        for the job with which this algorithm is to be used.
   * @param  activeThreads  The number of threads that are already active at
   *                        the time that this method is called.
   *
   * @return  A two-dimensional array that provides information about the
   *          increase or decrease in active job threads that should be applied
   *          over time.
   *
   * @throws  SLAMDException  If a problem occurs while calculating the variance
   *                          array.
   */
  public abstract int[][] calculateVariance(int duration, int totalThreads,
                                            int activeThreads)
         throws SLAMDException;
}

