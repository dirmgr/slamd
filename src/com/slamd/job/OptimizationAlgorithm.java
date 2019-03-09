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



import com.slamd.common.SLAMDException;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.ParameterList;



/**
 * This class defines an abstract optimization algorithm, which can be used to
 * determine how an optimizing job should proceed.  In particular, an
 * optimization algorithm defines a number of user-specified parameters that
 * can be used to customize the way that it operates, and then implements the
 * logic to determine whether each iteration of an optimizing job has performed
 * better than all previous iterations based on the user-defined constraints.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class OptimizationAlgorithm
{
  /**
   * Creates a new instance of this optimization algorithm.  All subclasses must
   * define a constructor that does not take any arguments, and they must invoke
   * the constructor of this superclass as their first action.
   */
  public OptimizationAlgorithm()
  {
    // No implementation required for now, although there may be at some point
    // to support future enhancements.
  }



  /**
   * Retrieves the human-readable name that will be used for this optimization
   * algorithm.
   *
   * @return  The human-readable name that will be used for this optimization
   *          algorithm.
   */
  public abstract String getOptimizationAlgorithmName();



  /**
   * Creates a new, uninitialized instance of this optimization algorithm.  In
   * most cases, this should simply return the object created from invoking the
   * default constructor.
   *
   * @return  The new instance of this optimization algorithm.
   */
  public abstract OptimizationAlgorithm newInstance();



  /**
   * Indicates whether this optimization algorithm may be used when running the
   * specified type of job.
   *
   * @param  jobClass  The job class for which to make the determination.
   *
   * @return  <CODE>true</CODE> if this optimization algorithm may be used with
   *          the provided job class, or <CODE>false</CODE> if not.
   */
  public abstract boolean availableWithJobClass(JobClass jobClass);



  /**
   * Retrieves a set of parameter stubs that should be used to prompt the end
   * user for the settings to use when executing the optimizing job.
   *
   * @param  jobClass  The job class that will be used for the optimizing job.
   *
   * @return  A set of parameter stubs that should be used to prompt the end
   *          user for the settings to use when executing the optimizing job.
   */
  public abstract ParameterList
       getOptimizationAlgorithmParameterStubs(JobClass jobClass);



  /**
   * Retrieves the set of parameters that have been defined for this
   * optimization algorithm.
   *
   * @return  The set of parameters that have been defined for this optimization
   *          algorithm.
   */
  public abstract ParameterList getOptimizationAlgorithmParameters();



  /**
   * Initializes this optimization algorithm with the provided set of
   * parameters for the given optimizing job.
   *
   * @param  optimizingJob  The optimizing job with which this optimization
   *                        algorithm will be used.
   * @param  parameters     The parameter list containing the parameter values
   *                        provided by the end user when scheduling the
   *                        optimizing job.
   *
   * @throws  InvalidValueException  If the contents of the provided parameter
   *                                 list are not valid for use with this
   *                                 optimization algorithm.
   */
  public abstract void initializeOptimizationAlgorithm(OptimizingJob
                                                            optimizingJob,
                                                       ParameterList parameters)
         throws InvalidValueException;



  /**
   * Clears any state information currently set for this optimization algorithm
   * and restores it to the state it would have if a new instance had been
   * created and only the <CODE>initializeOptimizationAlgorithm()</CODE> method
   * had been called on that instance.
   */
  public abstract void reInitializeOptimizationAlgorithm();



  /**
   * Indicates whether the provided iteration is the best one seen so far for
   * the given optimizing job based on the constraints specified in the
   * parameters used to initialize this optimization algorithm.
   *
   * @param  iteration      The job iteration for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if the provided iteration is the best one seen
   *          so far for the optimizing job, or <CODE>false</CODE> if not.
   *
   * @throws  SLAMDException  If a problem occurs that prevents a valid
   *                          determination from being made.  If this exception
   *                          is thrown, then the optimizing job will stop
   *                          immediately with no further iterations.
   */
  public abstract boolean isBestIterationSoFar(Job iteration)
         throws SLAMDException;



  /**
   * Retrieves the value associated with the provided iteration of the given
   * optimizing job.
   *
   * @param  iteration      The job iteration for which to retrieve the value.
   *
   * @return  The value associated with the provided iteration of the given
   *          optimizing job.
   *
   * @throws  SLAMDException  If a problem occurs while trying to determine the
   *                          value for the given optimizing job iteration.
   */
  public abstract double getIterationOptimizationValue(Job iteration)
       throws SLAMDException;
}

