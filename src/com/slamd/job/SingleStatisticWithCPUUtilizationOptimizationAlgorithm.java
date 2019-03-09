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



import java.util.ArrayList;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.parameter.FloatParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.resourcemonitor.VMStatResourceMonitor;
import com.slamd.server.SLAMDServer;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.ResourceMonitorStatTracker;
import com.slamd.stat.StackedValueTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD optimization algorithm that looks at a single
 * statistic within the job and finds the iteration with the highest or lowest
 * value for a given stat tracker that also has a CPU utilization less than or
 * equal to a given value.
 * <BR><BR>
 * For this optimization algorithm to be used, an optimizing job must include
 * the appropriate data from at least one VMStat resource monitor.  The data to
 * examine can come from either a stat tracker providing only the specific
 * utilization component to examine, or from the stacked value tracker that
 * reports all three (user, system, and idle times).  If there are multiple
 * reports of CPU utilization, then they will all be taken into consideration.
 *
 *
 * @author   Neil A. Wilson
 */
public class SingleStatisticWithCPUUtilizationOptimizationAlgorithm
       extends OptimizationAlgorithm
{
  /**
   * The name of the parameter that is used to specify the minimum required
   * percent improvement needed for a new best iteration.
   */
  public static final String PARAM_MIN_PCT_IMPROVEMENT = "min_pct_improvement";



  /**
   * The name of the parameter that is used to specify the statistic to
   * optimize.
   */
  public static final String PARAM_OPTIMIZE_STAT = "optimize_stat";



  /**
   * The name of the parameter that is used to specify the type of optimization
   * to perform.
   */
  public static final String PARAM_OPTIMIZE_TYPE = "optimize_type";



  /**
   * The name of the parameter that is used to specify the maximum CPU
   * utilization that will be acceptable.
   */
  public static final String PARAM_MAX_CPU_UTILIZATION = "max_cpu_utilization";



  /**
   * The name of the parameter that is used to specify the component of CPU
   * utilization that should be examined.
   */
  public static final String PARAM_UTILIZATION_COMPONENT =
       "utilization_component";



  /**
   * The optimization type value that indicates that we should try to find the
   * highest value for the statistic to optimize.
   */
  public static final int OPTIMIZE_TYPE_MAXIMIZE = 1;



  /**
   * The optimization type value that indicates that we should try to find the
   * lowest value for the statistic to optimize.
   */
  public static final int OPTIMIZE_TYPE_MINIMIZE = 2;



  /**
   * The utilization component that indicates that only user time should be
   * considered.
   */
  public static final int UTILIZATION_COMPONENT_USER_TIME = 1;



  /**
   * The utilization component that indicates that only system time should be
   * considered.
   */
  public static final int UTILIZATION_COMPONENT_SYSTEM_TIME = 2;



  /**
   * The utilization component that indicates that both user and system time
   * should be considered.
   */
  public static final int UTILIZATION_COMPONENT_BUSY_TIME = 3;



  /**
   * The string that will be displayed for the user time utilization component.
   */
  public static final String UTILIZATION_COMPONENT_USER_STRING = "User Time";



  /**
   * The string that will be displayed for the system time utilization
   * component.
   */
  public static final String UTILIZATION_COMPONENT_SYSTEM_STRING =
       "System Time";



  /**
   * The string that will be displayed for the busy time utilization component.
   */
  public static final String UTILIZATION_COMPONENT_BUSY_STRING =
       "Busy Time (User + System)";




  // The best value seen so far for this algorithm.
  private double bestValueSoFar;

  // The maximum acceptable CPU utilization.
  private double maxUtilization;

  // The minimum percent improvement that must be seen to consider a higher
  // value the new best iteration.
  private float minPctImprovement;

  // The parameter used to specify the maximum acceptable CPU utilization.
  private FloatParameter maxUtilizationParameter;

  // The parameter used to specify the minimum percent improvement.
  private FloatParameter minPctImprovementParameter;

  // The type of optimization to perform.
  private int optimizeType;

  // The utilization component to consider.
  private int utilizationComponent;

  // The parameter used to specify the statistic to optimize.
  private MultiChoiceParameter optimizeStatParameter;

  // The parameter used to specify the type of optimization to perform.
  private MultiChoiceParameter optimizeTypeParameter;

  // The parameter used to specify the utilization component to consider.
  private MultiChoiceParameter utilizationComponentParameter;

  // The optimizing job with which this optimization algorithm is associated.
  private OptimizingJob optimizingJob;

  // The display name of the statistic to optimize.
  private String optimizeStat;



  /**
   * Creates a new instance of this optimization algorithm.  All subclasses must
   * define a constructor that does not take any arguments, and they must invoke
   * the constructor of this superclass as their first action.
   */
  public SingleStatisticWithCPUUtilizationOptimizationAlgorithm()
  {
    super();

    minPctImprovementParameter    = null;
    optimizeStatParameter         = null;
    optimizeTypeParameter         = null;
    utilizationComponentParameter = null;
    maxUtilizationParameter       = null;
    bestValueSoFar                = Double.NaN;
    minPctImprovement             = 0.0F;
    optimizingJob                 = null;
    optimizeStat                  = null;
    optimizeType                  = -1;
    maxUtilization                = 100.0;
    utilizationComponent          = -1;
  }



  /**
   * Retrieves the human-readable name that will be used for this optimization
   * algorithm.
   *
   * @return  The human-readable name that will be used for this optimization
   *          algorithm.
   */
  @Override()
  public String getOptimizationAlgorithmName()
  {
    return "Optimize a Single Job Statistic with a CPU Utilization Constraint";
  }



  /**
   * Creates a new, uninitialized instance of this optimization algorithm.  In
   * most cases, this should simply return the object created from invoking the
   * default constructor.
   *
   * @return  The new instance of this optimization algorithm.
   */
  @Override()
  public OptimizationAlgorithm newInstance()
  {
    return new SingleStatisticWithCPUUtilizationOptimizationAlgorithm();
  }



  /**
   * Indicates whether this optimization algorithm may be used when running the
   * specified type of job.  This algorithm is only available for jobs that
   * report at least one "searchable" stat tracker.
   *
   * @param  jobClass  The job class for which to make the determination.
   *
   * @return  <CODE>true</CODE> if this optimization algorithm may be used with
   *          the provided job class, or <CODE>false</CODE> if not.
   */
  @Override()
  public boolean availableWithJobClass(JobClass jobClass)
  {
    StatTracker[] jobStats = jobClass.getStatTrackerStubs("", "", 1);
    if ((jobStats == null) || (jobStats.length == 0))
    {
      return false;
    }

    for (int i=0; i < jobStats.length; i++)
    {
      if (jobStats[i].isSearchable())
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Clears any state information currently set for this optimization algorithm
   * and restores it to the state it would have if a new instance had been
   * created and only the <CODE>initializeOptimizationAlgorithm()</CODE> method
   * had been called on that instance.  In this case, all that is necessary is
   * to forget about the best value seen so far.
   */
  @Override()
  public void reInitializeOptimizationAlgorithm()
  {
    bestValueSoFar = Double.NaN;
  }



  /**
   * Retrieves a set of parameter stubs that should be used to prompt the end
   * user for the settings to use when executing the optimizing job.
   *
   * @param  jobClass  The job class that will be used for the optimizing job.
   *
   * @return  A set of parameter stubs that should be used to prompt the end
   *          user for the settings to use when executing the optimizing job.
   */
  @Override()
  public ParameterList getOptimizationAlgorithmParameterStubs(JobClass jobClass)
  {
    // First, compile a list of all the "searchable" statistics that this job
    // reports it collects.
    ArrayList<String> availableStatList = new ArrayList<String>();
    StatTracker[] jobStats = jobClass.getStatTrackerStubs("", "", 1);
    for (int i=0; i < jobStats.length; i++)
    {
      if (jobStats[i].isSearchable())
      {
        availableStatList.add(jobStats[i].getDisplayName());
      }
    }

    int numAvailable = availableStatList.size();
    if (numAvailable == 0)
    {
      return new ParameterList();
    }

    String[] searchableStatNames = new String[numAvailable];
    availableStatList.toArray(searchableStatNames);
    if (optimizeStat == null)
    {
      optimizeStat = searchableStatNames[0];
    }

    String[] optimizationTypes =
    {
      Constants.OPTIMIZE_TYPE_MAXIMIZE,
      Constants.OPTIMIZE_TYPE_MINIMIZE
    };
    String optimizeTypeStr;
    switch (optimizeType)
    {
      case OPTIMIZE_TYPE_MAXIMIZE:
        optimizeTypeStr = Constants.OPTIMIZE_TYPE_MAXIMIZE;
        break;
      case OPTIMIZE_TYPE_MINIMIZE:
        optimizeTypeStr = Constants.OPTIMIZE_TYPE_MINIMIZE;
        break;
      default:
        optimizeTypeStr = Constants.OPTIMIZE_TYPE_MAXIMIZE;
        break;
    }

    String[] utilizationComponents =
    {
      UTILIZATION_COMPONENT_USER_STRING,
      UTILIZATION_COMPONENT_SYSTEM_STRING,
      UTILIZATION_COMPONENT_BUSY_STRING
    };

    String componentStr;
    switch (utilizationComponent)
    {
      case UTILIZATION_COMPONENT_USER_TIME:
        componentStr = UTILIZATION_COMPONENT_USER_STRING;
        break;
      case UTILIZATION_COMPONENT_SYSTEM_TIME:
        componentStr = UTILIZATION_COMPONENT_SYSTEM_STRING;
        break;
      case UTILIZATION_COMPONENT_BUSY_TIME:
        componentStr = UTILIZATION_COMPONENT_BUSY_STRING;
        break;
      default:
        componentStr = UTILIZATION_COMPONENT_BUSY_STRING;
        break;
    }


    optimizeStatParameter =
         new MultiChoiceParameter(PARAM_OPTIMIZE_STAT, "Statistic to Optimize",
                                  "The name of the statistic for which to " +
                                  "try to find the optimal value.",
                                  searchableStatNames, optimizeStat);
    optimizeTypeParameter =
         new MultiChoiceParameter(PARAM_OPTIMIZE_TYPE, "Optimization Type",
                                  "The type of optimization to perform for " +
                                  "the statistic to optimize.",
                                  optimizationTypes, optimizeTypeStr);
    maxUtilizationParameter =
         new FloatParameter(PARAM_MAX_CPU_UTILIZATION,
                            "Maximum Acceptable CPU Utilization",
                            "The maximum CPU utilization that will be " +
                            "allowed for the specified CPU component for " +
                            "an iteration to be considered acceptable.", true,
                            (float) maxUtilization, true, (float) 0.0, true,
                            (float) 100.0);
    utilizationComponentParameter =
         new MultiChoiceParameter(PARAM_UTILIZATION_COMPONENT,
                                  "Utilization Component to Consider",
                                  "The CPU utilization component to consider " +
                                  "when deciding whether an iteration is " +
                                  "acceptable.", utilizationComponents,
                                  componentStr);

    minPctImprovementParameter =
         new FloatParameter(PARAM_MIN_PCT_IMPROVEMENT,
                            "Min. % Improvement for New Best Iteration",
                            "The minimum percentage improvement in " +
                            "performance that an iteration must have over " +
                            "the previous best to be considered the new best " +
                            "iteration.", false, minPctImprovement, true, 0.0F,
                            false, 0.0F);


    Parameter[] algorithmParams =
    {
      new PlaceholderParameter(),
      optimizeStatParameter,
      optimizeTypeParameter,
      maxUtilizationParameter,
      utilizationComponentParameter,
      minPctImprovementParameter
    };

    return new ParameterList(algorithmParams);
  }



  /**
   * Retrieves the set of parameters that have been defined for this
   * optimization algorithm.
   *
   * @return  The set of parameters that have been defined for this optimization
   *          algorithm.
   */
  @Override()
  public ParameterList getOptimizationAlgorithmParameters()
  {
    Parameter[] algorithmParams =
    {
      optimizeStatParameter,
      optimizeTypeParameter,
      maxUtilizationParameter,
      utilizationComponentParameter,
      minPctImprovementParameter
    };

    return new ParameterList(algorithmParams);
  }



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
  @Override()
  public void initializeOptimizationAlgorithm(OptimizingJob optimizingJob,
                                              ParameterList parameters)
         throws InvalidValueException
  {
    this.optimizingJob = optimizingJob;

    String[] monitorClients = optimizingJob.getResourceMonitorClients();
    if ((monitorClients == null) || (monitorClients.length == 0))
    {
      throw new InvalidValueException("No resource monitor clients have been " +
                                      "requested for this optimizing job.  " +
                                      "At least one is required to provide " +
                                      "CPU utilization data.");
    }


    // Get the optimization statistic parameter and name.
    optimizeStatParameter =
         parameters.getMultiChoiceParameter(PARAM_OPTIMIZE_STAT);
    if ((optimizeStatParameter == null) || (! optimizeStatParameter.hasValue()))
    {
      throw new InvalidValueException("No value provided for the statistic " +
                                      "to optimize");
    }
    optimizeStat = optimizeStatParameter.getStringValue();


    // Get the optimization type parameter and value.
    optimizeTypeParameter =
         parameters.getMultiChoiceParameter(PARAM_OPTIMIZE_TYPE);
    if ((optimizeTypeParameter == null) || (! optimizeTypeParameter.hasValue()))
    {
      throw new InvalidValueException("No value provided for the " +
                                      "optimization type");
    }
    String optimizeTypeStr = optimizeTypeParameter.getStringValue();
    if (optimizeTypeStr.equalsIgnoreCase(Constants.OPTIMIZE_TYPE_MAXIMIZE))
    {
      optimizeType = OPTIMIZE_TYPE_MAXIMIZE;
    }
    else  if (optimizeTypeStr.equalsIgnoreCase(
                                   Constants.OPTIMIZE_TYPE_MINIMIZE))
    {
      optimizeType = OPTIMIZE_TYPE_MINIMIZE;
    }
    else
    {
      throw new InvalidValueException("Invalid value \"" + optimizeTypeStr +
                                      "\" for optimization type.");
    }


    // Get the maximum acceptable CPU utilization.
    maxUtilizationParameter =
         parameters.getFloatParameter(PARAM_MAX_CPU_UTILIZATION);
    if ((maxUtilizationParameter == null) ||
        (! maxUtilizationParameter.hasValue()))
    {
      throw new InvalidValueException("No value provided for the maximum " +
                                      "acceptable CPU utilization.");
    }
    maxUtilization = maxUtilizationParameter.getFloatValue();


    // Get the CPU utilization component to consider.
    utilizationComponentParameter =
         parameters.getMultiChoiceParameter(PARAM_UTILIZATION_COMPONENT);
    if ((utilizationComponentParameter == null) ||
        (! utilizationComponentParameter.hasValue()))
    {
      throw new InvalidValueException("No value provided for the CPU " +
                                      "utilization component.");
    }
    String componentStr = utilizationComponentParameter.getStringValue();
    if (componentStr.equalsIgnoreCase(UTILIZATION_COMPONENT_USER_STRING))
    {
      utilizationComponent = UTILIZATION_COMPONENT_USER_TIME;
    }
    else if (componentStr.equalsIgnoreCase(UTILIZATION_COMPONENT_SYSTEM_STRING))
    {
      utilizationComponent = UTILIZATION_COMPONENT_SYSTEM_TIME;
    }
    else if (componentStr.equalsIgnoreCase(UTILIZATION_COMPONENT_BUSY_STRING))
    {
      utilizationComponent = UTILIZATION_COMPONENT_BUSY_TIME;
    }
    else
    {
      throw new InvalidValueException("Invalid value \"" + componentStr +
                                      "\" for CPU utilization component.");
    }


    // Get the minimum percent improvement required for a new best iteration.
    minPctImprovement = 0.0F;
    minPctImprovementParameter =
         parameters.getFloatParameter(PARAM_MIN_PCT_IMPROVEMENT);
    if ((minPctImprovementParameter != null) &&
        minPctImprovementParameter.hasValue())
    {
      minPctImprovement = minPctImprovementParameter.getFloatValue();
    }


    // See If the provided optimizing job has run any iterations so far.  If so,
    // then look through them to determine the best value so far.
    bestValueSoFar = Double.NaN;
    Job[] iterations = optimizingJob.getAssociatedJobs();
    if (iterations != null)
    {
      for (int i=0; i <iterations.length; i++)
      {
        try
        {
          if (! isAcceptableCPUUtilization(iterations[i]))
          {
            continue;
          }
        } catch (Exception e) {}


        StatTracker[] trackers = iterations[i].getStatTrackers(optimizeStat);
        if ((trackers != null) && (trackers.length > 0))
        {
          StatTracker tracker = trackers[0].newInstance();
          tracker.aggregate(trackers);
          double value = tracker.getSummaryValue();
          if (Double.isNaN(bestValueSoFar))
          {
            bestValueSoFar = value;
          }
          else if ((optimizeType == OPTIMIZE_TYPE_MAXIMIZE) &&
                   (value > bestValueSoFar) &&
                   (value >= (bestValueSoFar+bestValueSoFar*minPctImprovement)))
          {
            bestValueSoFar = value;
          }
          else if ((optimizeType == OPTIMIZE_TYPE_MINIMIZE) &&
                   (value < bestValueSoFar) &&
                   (value <= (bestValueSoFar-bestValueSoFar*minPctImprovement)))
          {
            bestValueSoFar = value;
          }
        }
      }
    }

    SLAMDServer slamdServer = optimizingJob.slamdServer;
    slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                           "SingleStatisticWithCPUUtilizationOptimization" +
                           "Algorithm.initializeOptimizationAlgorith(" +
                           optimizingJob.getOptimizingJobID() +
                           ") best so far is " +
                           String.valueOf(bestValueSoFar));
  }



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
  @Override()
  public boolean isBestIterationSoFar(Job iteration)
         throws SLAMDException
  {
    SLAMDServer slamdServer = iteration.slamdServer;

    if (! isAcceptableCPUUtilization(iteration))
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                             "SingleStatisticWithCPUUtilizationOptimization" +
                             "Algorithm.isBestIterationSoFar(" +
                             iteration.getJobID() + ") returning false " +
                             "because the iteration does not have acceptable " +
                             "CPU utilization data.");
      return false;
    }

    double iterationValue = getIterationOptimizationValue(iteration);

    if (Double.isNaN(bestValueSoFar) && (! Double.isNaN(iterationValue)))
    {
      bestValueSoFar = iterationValue;
      slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                             "SingleStatisticWithCPUUtilizationOptimization" +
                             "Algorithm.isBestIterationSoFar(" +
                             iteration.getJobID() + ") returning true " +
                             "because iteration value " + iterationValue +
                             " is not NaN but current best is NaN.");
      return true;
    }

    switch (optimizeType)
    {
      case OPTIMIZE_TYPE_MAXIMIZE:
        if (iterationValue > bestValueSoFar)
        {
          if (iterationValue > bestValueSoFar+bestValueSoFar*minPctImprovement)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                   "SingleStatisticWithCPUUtilization" +
                                   "OptimizationAlgorithm." +
                                   "isBestIterationSoFar(" +
                                   iteration.getJobID() + ") returning true " +
                                   "because iteration value " + iterationValue +
                                   " is greater than previous best value " +
                                   bestValueSoFar + " by at least " +
                                   (minPctImprovement*100) + "%.");
            bestValueSoFar = iterationValue;
            return true;
          }
          else
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                   "SingleStatisticWithCPUUtilization" +
                                   "OptimizationAlgorithm." +
                                   "isBestIterationSoFar(" +
                                   iteration.getJobID() + ") returning false " +
                                   "because iteration value " + iterationValue +
                                   " is greater than previous best value " +
                                   bestValueSoFar + " but the margin of " +
                                   "improvement is less than " +
                                   (minPctImprovement*100) + "%.");
            return false;
          }
        }
        else
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                 "SingleStatisticWithCPUUtilization" +
                                 "OptimizationAlgorithm.isBestIterationSoFar(" +
                                 iteration.getJobID() + ") returning false " +
                                 "because iteration value " + iterationValue +
                                 " is less than previous best value " +
                                 bestValueSoFar);
          return false;
        }
      case OPTIMIZE_TYPE_MINIMIZE:
        if (iterationValue < bestValueSoFar)
        {
          if (iterationValue < bestValueSoFar-bestValueSoFar*minPctImprovement)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                   "SingleStatisticWithCPUUtilization" +
                                   "OptimizationAlgorithm." +
                                   "isBestIterationSoFar(" +
                                   iteration.getJobID() + ") returning true " +
                                   "because iteration value " + iterationValue +
                                   " is less than previous best value " +
                                   bestValueSoFar + " by at least " +
                                   (minPctImprovement*100) + "%.");
            bestValueSoFar = iterationValue;
            return true;
          }
          else
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                   "SingleStatisticWithCPUUtilization" +
                                   "OptimizationAlgorithm." +
                                   "isBestIterationSoFar(" +
                                   iteration.getJobID() + ") returning false " +
                                   "because iteration value " + iterationValue +
                                   " is less than previous best value " +
                                   bestValueSoFar + " but the margin of " +
                                   "improvement is less than " +
                                   (minPctImprovement*100) + "%.");
            return false;
          }
        }
        else
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                 "SingleStatisticWithCPUUtilization" +
                                 "OptimizationAlgorithm.isBestIterationSoFar(" +
                                 iteration.getJobID() + ") returning false " +
                                 "because iteration value " + iterationValue +
                                 " is greater than previous best value " +
                                 bestValueSoFar);
          return false;
        }
      default:
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                               "SingleStatisticWithCPUUtilization" +
                               "OptimizationAlgorithm.isBestIterationSoFar(" +
                               iteration.getJobID() + ") returning false " +
                               "because an unknown optimization type of " +
                               optimizeType + " is being used.");
        return false;
    }
  }



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
  @Override()
  public double getIterationOptimizationValue(Job iteration)
       throws SLAMDException
  {
    StatTracker[] trackers = iteration.getStatTrackers(optimizeStat);
    if ((trackers == null) || (trackers.length == 0))
    {
      throw new SLAMDException("The provided optimizing job iteration did " +
                               "not include any values for the statistic to " +
                               "optimize, \"" + optimizeStat + "\".");
    }

    StatTracker tracker = trackers[0].newInstance();
    tracker.aggregate(trackers);

    double summaryValue = tracker.getSummaryValue();
    iteration.slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                     "SingleStatisticWithCPUUtilization" +
                                     "OptimizationAlgorithm." +
                                     "getIterationOptimizationValue(" +
                                     iteration.getJobID() + ") returning " +
                                     summaryValue);

    return summaryValue;
  }



  /**
   * Indicates whether the provided job iteration has an acceptable CPU
   * utilization.
   *
   * @param  iteration  The iteration for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the CPU utilization for the provided
   *          iteration is acceptable, or <CODE>false</CODE> if not.
   *
   * @throws  SLAMDException  If the provided iteration does not include
   *                          sufficient CPU utilization data to make the
   *                          determination.
   */
  private boolean isAcceptableCPUUtilization(Job iteration)
          throws SLAMDException
  {
    SLAMDServer slamdServer      = iteration.slamdServer;
    boolean     utilizationFound = false;

    String className = VMStatResourceMonitor.class.getName();
    ResourceMonitorStatTracker[] monitorTrackers =
         iteration.getResourceMonitorStatTrackersForClass(className);
    for (int i=0; i < monitorTrackers.length; i++)
    {
      StatTracker tracker = monitorTrackers[i].getStatTracker();
      String      name    = tracker.getDisplayName();

      if ((tracker instanceof StackedValueTracker) &&
          name.endsWith(VMStatResourceMonitor.STAT_TRACKER_CPU_UTILIZATION))
      {
        utilizationFound = true;
        StackedValueTracker utilizationTracker = (StackedValueTracker) tracker;
        double userTime =
             utilizationTracker.getAverageValue(
                  VMStatResourceMonitor.UTILIZATION_CATEGORY_USER);
        double systemTime =
             utilizationTracker.getAverageValue(
                  VMStatResourceMonitor.UTILIZATION_CATEGORY_SYSTEM);
        double busyTime = userTime + systemTime;

        switch (utilizationComponent)
        {
          case UTILIZATION_COMPONENT_USER_TIME:
            if (userTime > maxUtilization)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                     "SingleStatisticWithCPUUtilization" +
                                     "OptimizationAlgorithm.isAcceptableCPU" +
                                     "Utilization(" + iteration.getJobID() +
                                     ") returning false because user time of " +
                                     userTime + " for stat " +
                                     tracker.getDisplayName() +
                                     " exceeded the maximum allowed of " +
                                     maxUtilization);
              return false;
            }
            break;
          case UTILIZATION_COMPONENT_SYSTEM_TIME:
            if (systemTime > maxUtilization)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                     "SingleStatisticWithCPUUtilization" +
                                     "OptimizationAlgorithm.isAcceptableCPU" +
                                     "Utilization(" + iteration.getJobID() +
                                     ") returning false because system time " +
                                     "of " + systemTime + " for stat " +
                                     tracker.getDisplayName() +
                                     " exceeded the maximum allowed of " +
                                     maxUtilization);
              return false;
            }
            break;
          case UTILIZATION_COMPONENT_BUSY_TIME:
            if (busyTime > maxUtilization)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                     "SingleStatisticWithCPUUtilization" +
                                     "OptimizationAlgorithm.isAcceptableCPU" +
                                     "Utilization(" + iteration.getJobID() +
                                     ") returning false because busy time of " +
                                     busyTime + " for stat " +
                                     tracker.getDisplayName() +
                                     " exceeded the maximum allowed of " +
                                     maxUtilization);
              return false;
            }
            break;
          default:
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                   "SingleStatisticWithCPUUtilization" +
                                   "OptimizationAlgorithm.isAcceptableCPU" +
                                   "Utilization(" + iteration.getJobID() +
                                   ") returning false because an unknown " +
                                   "utilization component of " +
                                   utilizationComponent + " is in use.");
            return false;
        }
      }
      else if ((tracker instanceof IntegerValueTracker) &&
               ((utilizationComponent == UTILIZATION_COMPONENT_USER_TIME) &&
                name.endsWith(VMStatResourceMonitor.STAT_TRACKER_CPU_USER)) ||
               ((utilizationComponent == UTILIZATION_COMPONENT_SYSTEM_TIME) &&
                name.endsWith(VMStatResourceMonitor.STAT_TRACKER_CPU_SYSTEM)) ||
               ((utilizationComponent == UTILIZATION_COMPONENT_BUSY_TIME) &&
                name.endsWith(VMStatResourceMonitor.STAT_TRACKER_CPU_BUSY)))
      {
        utilizationFound = true;
        double value = ((IntegerValueTracker) tracker).getAverageValue();
        if (value > maxUtilization)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                                 "SingleStatisticWithCPUUtilization" +
                                 "OptimizationAlgorithm.isAcceptableCPU" +
                                 "Utilization(" + iteration.getJobID() +
                                 ") returning false because value of " +
                                 value + " for stat " +
                                 tracker.getDisplayName() +
                                 " exceeded the maximum allowed of " +
                                 maxUtilization);
          return false;
        }
      }
    }

    if (! utilizationFound)
    {
      throw new SLAMDException("The provided job iteration did not include " +
                               "any CPU utilization data.");
    }

    slamdServer.logMessage(Constants.LOG_LEVEL_JOB_DEBUG,
                           "SingleStatisticWithCPUUtilizationOptimization" +
                           "Algorithm.isAcceptableCPUUtilization(" +
                           iteration.getJobID() + ") returning true.");
    return true;
  }
}

