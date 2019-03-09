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
 * This class defines a load variance algorithm that may be used to increase or
 * decrease the number of active threads in spurts rather than gradually over
 * time.  That is, all the threads to enable or disable will be notified at the
 * very beginning of the interval with no change over the duration of that
 * interval.  That is, the graph will appear to have an initial jump up or down
 * but then will remain level for the duration of the interval associated with
 * that line.  The change in the number of active threads may be specified in
 * one of the following ways:
 * <UL>
 *   <LI>"+N", where N is a fixed number of threads to start at the beginning of
 *       this load variation.</LI>
 *   <LI>"-N", where N is a fixed number of threads to stop at the beginning of
 *       this load variation.</LI>
 *   <LI>"+N%", where N% is a percentage of the overall number of threads per
 *       client that should be started at the beginning of this load
 *       variation.</LI>
 *   <LI>"-N%", where N% is a percentage of the overall number of threads per
 *       client that should be stopped at the beginning of this load
 *       variation.</LI>
 *   <LI>"=N", where N is the fixed number of threads that should be running
 *       once this load variation completes.</LI>
 *   <LI>"=N%", where N% is a percentage of the overall number of threads per
 *       client that should be running once this load variation completes.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class StairStepLoadVarianceAlgorithm
       extends LoadVarianceAlgorithm
{
  /**
   * The variation method that indicates that the number of active threads
   * should be increased by a fixed amount.
   */
  public static final int METHOD_INCREASE_BY_NUMBER = 1;



  /**
   * The variation method that indicates that the number of active threads
   * should be decreased by a fixed amount.
   */
  public static final int METHOD_DECREASE_BY_NUMBER = 2;



  /**
   * The variation method that indicates that the number of active threads
   * should be increased by a percentage of the total number of threads.
   */
  public static final int METHOD_INCREASE_BY_PERCENT = 3;



  /**
   * The variation method that indicates that the number of active threads
   * should be decreased by a percentage of the total number of threads.
   */
  public static final int METHOD_DECREASE_BY_PERCENT = 4;



  /**
   * The variation method that indicates that the number of active threads
   * should be set to a fixed number.
   */
  public static final int METHOD_SET_TO_NUMBER = 5;



  /**
   * The variation method that indicates that the number of active threads
   * should be set to a percentage of the total number of threads.
   */
  public static final int METHOD_SET_TO_PERCENT = 6;



  // The variation method that should be used by this algorithm.
  private int variationMethod;

  // The value associated with the variation.
  private int variationValue;



  /**
   * This constructor is used to create a new instance of this load variation
   * algorithm through reflection.  A default constructor must be provided in
   * all subclasses, but the only thing that it needs to do is call
   * <CODE>super()</CODE>.
   */
  public StairStepLoadVarianceAlgorithm()
  {
    super();
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
  @Override()
  public void initializeVariationAlgorithm(String[] arguments)
         throws SLAMDException
  {
    if ((arguments == null) || (arguments.length != 1))
    {
      throw new SLAMDException("There must be exactly one argument provided " +
                               "for the linear load variance algorithm.");
    }

    String value = arguments[0];
    if ((value == null) || (value.length() == 0))
    {
      throw new SLAMDException("The argument to the linear load variance " +
                               "algorithm may not be blank.");
    }

    boolean endsWithPercent = value.endsWith("%");
    String  valueString     = null;

    switch (value.charAt(0))
    {
      case '+':
        if (endsWithPercent)
        {
          variationMethod = METHOD_INCREASE_BY_PERCENT;
          valueString     = value.substring(1, value.length() - 1);
        }
        else
        {
          variationMethod = METHOD_INCREASE_BY_NUMBER;
          valueString     = value.substring(1);
        }
        break;
      case '-':
        if (endsWithPercent)
        {
          variationMethod = METHOD_DECREASE_BY_PERCENT;
          valueString     = value.substring(1, value.length() - 1);
        }
        else
        {
          variationMethod = METHOD_DECREASE_BY_NUMBER;
          valueString     = value.substring(1);
        }
        break;
      case '=':
        if (endsWithPercent)
        {
          variationMethod = METHOD_SET_TO_PERCENT;
          valueString     = value.substring(1, value.length() - 1);
        }
        else
        {
          variationMethod = METHOD_SET_TO_NUMBER;
          valueString     = value.substring(1);
        }
        break;
      default:
        throw new SLAMDException("The argument to the linear load variance " +
                                 "algorithm must start with '+', '-', or '='.");
    }


    try
    {
      variationValue = Integer.parseInt(valueString);
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to parse '" + valueString +
                               "' as an integer.");
    }


    if (variationValue < 0)
    {
      throw new SLAMDException("The load variance value may not be negative.");
    }

    if (endsWithPercent && (variationValue > 100))
    {
      throw new SLAMDException("Percentage values given may not exceed 100.");
    }
  }



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
  @Override()
  public int[][] calculateVariance(int duration, int totalThreads,
                                   int activeThreads)
         throws SLAMDException
  {
    switch (variationMethod)
    {
      case METHOD_INCREASE_BY_NUMBER:
        return new int[][]
        {
          new int[]
          {
            0, Math.min(variationValue, (totalThreads-activeThreads))
          }
        };
      case METHOD_DECREASE_BY_NUMBER:
        return new int[][]
        {
          new int[]
          {
            0, -(Math.min(variationValue, activeThreads))
          }
        };
      case METHOD_INCREASE_BY_PERCENT:
        int numThreads = totalThreads * variationValue / 100;
        return new int[][]
        {
          new int[]
          {
            0, Math.min(numThreads, (totalThreads-activeThreads))
          }
        };
      case METHOD_DECREASE_BY_PERCENT:
        numThreads = totalThreads * variationValue / 100;
        return new int[][]
        {
          new int[]
          {
            0, -(Math.min(numThreads, activeThreads))
          }
        };
      case METHOD_SET_TO_NUMBER:
        numThreads = Math.min(variationValue, totalThreads);
        if (numThreads == activeThreads)
        {
          return new int[0][];
        }
        else if (numThreads > activeThreads)
        {
          return new int[][]
          {
            new int[]
            {
              0, Math.min(numThreads, (totalThreads-activeThreads))
            }
          };
        }
        else
        {
          return new int[][]
          {
            new int[]
            {
              0, -(Math.min(numThreads, activeThreads))
            }
          };
        }
      case METHOD_SET_TO_PERCENT:
        numThreads = totalThreads * variationValue / 100;
        if (numThreads == activeThreads)
        {
          return new int[0][];
        }
        else if (numThreads > activeThreads)
        {
          return new int[][]
          {
            new int[]
            {
              0, Math.min(numThreads, (totalThreads-activeThreads))
            }
          };
        }
        else
        {
          return new int[][]
          {
            new int[]
            {
              0, -(Math.min(numThreads, activeThreads))
            }
          };
        }
      default:
        throw new SLAMDException("Invalid variation method:  " +
                                 variationMethod);
    }
  }
}

