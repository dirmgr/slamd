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
 * decrease the number of active threads using curves based on various sections
 * of the sine wave.
 * <BR><BR>
 * Each invocation of this load variance algorithm should consist of two
 * tab-delimited components.  The first should be one of the following strings:
 * <UL>
 *   <LI>"concave" -- this indicates that the curve should open downward.</LI>
 *   <LI>"convex" -- this indicates that the curve should open upward.</LI>
 * </UL>
 * The second argument should be one of the following strings:
 * <UL>
 *   <LI>"+N" -- this indicates that the actual number of active threads should
 *       be increased by "N".</LI>
 *   <LI>"-N" -- this indicates that the actual number of active threads should
 *        be decreased by "N".</LI>
 *   <LI>"+N%" -- this indicates that the actual number of active threads should
 *       be increased by "N" percent of the total number of threads.</LI>
 *   <LI>"-N%" -- this indicates that the actual number of active threads should
 *       be decreased by "N" percent of the total number of threads.</LI>
 *   <LI>"=N" -- this indicates that the actual number of active threads should
 *       be increased or decreased to "N".</LI>
 *   <LI>"=N%" -- this indicates that the actual number of active threads should
 *       be increased or decreased to "N" percent of the total number of
 *       threads.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class SineLoadVarianceAlgorithm
       extends LoadVarianceAlgorithm
{
  /**
   * A predefined constant that can make some of the formulas a little shorter.
   */
  public static final double PI = Math.PI;



  /**
   * The shape value that indicates that the resulting curve should open
   * downward.
   */
  public static final int SHAPE_CONCAVE = 1;



  /**
   * The shape value that indicates that the resulting curve should open upward.
   */
  public static final int SHAPE_CONVEX = 2;




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

  // The shape of the variation that should be used by this algorithm.
  private int variationShape;

  // The value associated with the variation.
  private int variationValue;



  /**
   * This constructor is used to create a new instance of this load variation
   * algorithm through reflection.  A default constructor must be provided in
   * all subclasses, but the only thing that it needs to do is call
   * <CODE>super()</CODE>.
   */
  public SineLoadVarianceAlgorithm()
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
    if ((arguments == null) || (arguments.length != 2))
    {
      throw new SLAMDException("There must be exactly two arguments provided " +
                               "for the sine load variance algorithm.");
    }


    String shapeStr = arguments[0];
    if (shapeStr.equalsIgnoreCase("concave"))
    {
      variationShape = SHAPE_CONCAVE;
    }
    else if (shapeStr.equalsIgnoreCase("convex"))
    {
      variationShape = SHAPE_CONVEX;
    }
    else
    {
      throw new SLAMDException("Invalid shape \"" + shapeStr +
                               "\" -- should be concave or convex.");
    }


    String value = arguments[1];
    if ((value == null) || (value.length() == 0))
    {
      throw new SLAMDException("The second argument to the sine load " +
                               "variance algorithm may not be blank.");
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
        throw new SLAMDException("The second argument to the sine load " +
                                 "variance algorithm must start with '+', " +
                                 "'-', or '='.");
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
    // Figure out the change in the number of threads and whether there will be
    // an increase or decrease.
    boolean increase;
    int     numThreads;
    switch (variationMethod)
    {
      case METHOD_INCREASE_BY_NUMBER:
        increase   = true;
        numThreads = variationValue;
        break;
      case METHOD_DECREASE_BY_NUMBER:
        increase   = false;
        numThreads = variationValue;
        break;
      case METHOD_INCREASE_BY_PERCENT:
        increase   = true;
        numThreads = (totalThreads * variationValue / 100);
        break;
      case METHOD_DECREASE_BY_PERCENT:
        increase   = false;
        numThreads = (totalThreads * variationValue / 100);
        break;
      case METHOD_SET_TO_NUMBER:
        numThreads = variationValue - activeThreads;
        if (numThreads > 0)
        {
          increase = true;
        }
        else
        {
          increase   = false;
          numThreads = Math.abs(numThreads);
        }
        break;
      case METHOD_SET_TO_PERCENT:
        numThreads = (totalThreads * variationValue / 100) - activeThreads;
        if (numThreads > 0)
        {
          increase = true;
        }
        else
        {
          increase   = false;
          numThreads = Math.abs(numThreads);
        }
        break;
      default:
        throw new SLAMDException("Invalid load variance method " +
                                 variationMethod);
    }


    // If there is no change in the number of threads, then we don't need to do
    // anything.
    if (numThreads == 0)
    {
      return new int[0][];
    }


    // If the duration is zero, then just make the change immediately.
    if (duration == 0)
    {
      if (increase)
      {
        return new int[][]
        {
          new int[] { 0, numThreads }
        };
      }
      else
      {
        return new int[][]
        {
          new int[] { 0, -numThreads }
        };
      }
    }


    // Make sure that the specified increase or decrease is still within the
    // bounds of the number of available threads.
    if (increase && ((activeThreads + numThreads) > totalThreads))
    {
      numThreads = totalThreads - activeThreads;
    }
    else if ((! increase) && ((activeThreads - numThreads) < 0))
    {
      numThreads = activeThreads;
    }


    // Call the appropriate method to make the calculation based on the variance
    // shape.
    switch (variationShape)
    {
      case SHAPE_CONCAVE:
        if (increase)
        {
          return handleConcaveUp(numThreads, duration);
        }
        else
        {
          return handleConcaveDown(numThreads, duration);
        }
      case SHAPE_CONVEX:
        if (increase)
        {
          return handleConvexUp(numThreads, duration);
        }
        else
        {
          return handleConvexDown(numThreads, duration);
        }
      default:
        throw new SLAMDException("Invalid shape value " + variationShape);
    }
  }



  /**
   * Handle an increase in the number of active threads using a concave up
   * shape.  The shape will be based on the function f(x) = sin(x).
   *
   * @param  numThreads  The actual number of threads that should be activated
   *                     during this process.
   * @param  duration    The length of time in seconds over which the activation
   *                     should be processed.
   *
   * @return  The load variation data that indicates the number of threads to
   *          activate or deactivate over time.
   */
  private int[][] handleConcaveUp(int numThreads, int duration)
  {
    // The equation solved for y is:  y = H*sin((PI*x)/(2*W)).
    // Solved for x is:  x = (2*W*asin(y/H))/PI.
    int     durationMillis = duration * 1000;
    int[][] returnArray    = new int[numThreads][2];
    for (int i=0,y=1; i < numThreads; i++,y++)
    {
      double x = (2.0*durationMillis*Math.asin(1.0*y/numThreads))/PI;
      returnArray[i][0] = (int) x;
      returnArray[i][1] = 1;
    }

    return returnArray;
  }



  /**
   * Handle a decrease in the number of active threads using a concave down
   * shape.  The shape will be based on the function f(x) = sin(x + pi/2).
   *
   * @param  numThreads  The actual number of threads that should be deactivated
   *                     during this process.
   * @param  duration    The length of time in seconds over which the
   *                     deactivation should be processed.
   *
   * @return  The load variation data that indicates the number of threads to
   *          activate or deactivate over time.
   */
  private int[][] handleConcaveDown(int numThreads, int duration)
  {
    // In this case, we'll use cosine which has a nicer period than sine for
    // this particular calculation.
    // The equation solved for y is:  y = H*cos((PI*x)/(2*W)).
    // Solved for x is:  x = (2*W*acos(y/H))/PI.
    int     durationMillis = duration * 1000;
    int[][] returnArray    = new int[numThreads][2];
    for (int i=0,y=numThreads; i < numThreads; i++,y--)
    {
      double x = (2.0*durationMillis*Math.acos(1.0*y/numThreads))/PI;
      returnArray[i][0] = (int) x;
      returnArray[i][1] = -1;
    }

    return returnArray;
  }



  /**
   * Handle an increase int the number of active threads using a convex up
   * shape.  The shape will be based on the function f(x) = 1 + sin(x - pi/2).
   *
   * @param  numThreads  The actual number of threads that should be activated
   *                     during this process.
   * @param  duration    The length of time in seconds over which the activation
   *                     should be processed.
   *
   * @return  The load variation data that indicates the number of threads to
   *          activate or deactivate over time.
   */
  private int[][] handleConvexUp(int numThreads, int duration)
  {
    // In this case, we'll use cosine which has a nicer period than sine for
    // this particular calculation.
    // The equation solved for y is:  y = H*(1-cos((PI*x)/(2*W))).
    // Solved for x is:  x = (2*W*acos(1-(y/H)))/PI.
    int     durationMillis = duration * 1000;
    int[][] returnArray    = new int[numThreads][2];
    for (int i=0,y=1; i < numThreads; i++,y++)
    {
      double x = (2.0*durationMillis*Math.acos(1.0-(1.0*y/numThreads)))/PI;
      returnArray[i][0] = (int) x;
      returnArray[i][1] = 1;
    }

    return returnArray;
  }



  /**
   * Handle a decrease int the number of active threads using a convex down
   * shape.  The shape will be based on the function f(x) = 1 - sin(x).
   *
   * @param  numThreads  The actual number of threads that should be deactivated
   *                     during this process.
   * @param  duration    The length of time in seconds over which the
   *                     deactivation should be processed.
   *
   * @return  The load variation data that indicates the number of threads to
   *          activate or deactivate over time.
   */
  private int[][] handleConvexDown(int numThreads, int duration)
  {
    // The equation solved for y is:  y = H*(1-sin((PI*x)/(2*W))).
    // Solved for x is:  x = (2*W*asin(1-(y/H)))/PI.
    int     durationMillis = duration * 1000;
    int[][] returnArray    = new int[numThreads][2];
    for (int i=0,y=numThreads; i < numThreads; i++,y--)
    {
      double x = (2.0*durationMillis*Math.asin(1.0-(1.0*y/numThreads)))/PI;
      returnArray[i][0] = (int) x;
      returnArray[i][1] = -1;
    }

    return returnArray;
  }
}

