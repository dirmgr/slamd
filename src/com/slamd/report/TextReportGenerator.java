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
package com.slamd.report;



import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.slamd.admin.RequestInfo;
import com.slamd.common.Constants;
import com.slamd.job.Job;
import com.slamd.job.JobItem;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class provides an implementation of a SLAMD report generator that will
 * write the report information to a plain text file.
 *
 *
 * @author   Neil A. Wilson
 */
public class TextReportGenerator
       implements ReportGenerator
{
  /**
   * The end-of-line string that will be used.
   */
  public static final String EOL = Constants.EOL;



  /**
   * The name of the configuration parameter that indicates whether to include
   * detailed statistical information in the report.
   */
  public static final String PARAM_INCLUDE_DETAILED_STATISTICS =
       "include_detailed_stats";



  /**
   * The name of the configuration parameter that indicates whether to include
   * resource monitor statistics in the report.
   */
  public static final String PARAM_INCLUDE_MONITOR_STATS =
       "include_monitor_config";



  /**
   * The name of the configuration parameter that indicates whether to include
   * the job-specific configuration in the report.
   */
  public static final String PARAM_INCLUDE_JOB_CONFIG = "include_job_config";



  /**
   * The name of the configuration parameter that indicates whether to include
   * the schedule configuration in the report.
   */
  public static final String PARAM_INCLUDE_SCHEDULE_CONFIG =
       "include_schedule_config";



  /**
   * The name of the configuration parameter that indicates whether to include
   * job statistics in the report.
   */
  public static final String PARAM_INCLUDE_STATS = "include_stats";



  /**
   * The name of the configuration parameter that indicates whether to only
   * include jobs that contain statistics in the generated report.
   */
  public static final String PARAM_REQUIRE_STATS = "require_stats";



  /**
   * The name of the configuration parameter that indicates whether to
   * summarize the individual iterations of an optimizing job.
   */
  public static final String PARAM_SUMMARIZE_OPTIMIZING_ITERATIONS =
       "summarize_optimizing_iterations";



  // The list used to hold the jobs and optimizing jobs that will be included
  // in the generated report.
  private ArrayList<JobItem> jobList;

  // Indicates whether to include detailed statistics for the jobs included in
  // the report.
  private boolean includeDetailedStats;

  // Indicates whether to include the job-specific configuration in the
  // generated report.
  private boolean includeJobConfig;

  // Indicates whether to include resource monitor statistics in the generated
  // report.
  private boolean includeMonitorStats;

  // Indicates whether to include the schedule configuration in the generated
  // report.
  private boolean includeScheduleConfig;

  // Indicates whether to include job statistics in the generated report.
  private boolean includeStats;

  // Indicates whether to only include jobs that have statistics.
  private boolean requireStats;

  // Indicates whether to only provide summary information for individual
  // iterations of an optimizing job.
  private boolean summarizeOptimizingIterations;

  // The decimal format that will be used to format floating-point values.
  private DecimalFormat decimalFormat;

  // The date formatter that will be used when writing out dates.
  private SimpleDateFormat dateFormat;



  /**
   * Creates a new text report generator.
   */
  public TextReportGenerator()
  {
    jobList       = new ArrayList<JobItem>();
    dateFormat    = new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    decimalFormat = new DecimalFormat("0.000");

    includeScheduleConfig         = true;
    includeJobConfig              = true;
    includeStats                  = true;
    includeMonitorStats           = true;
    includeDetailedStats          = false;
    requireStats                  = true;
    summarizeOptimizingIterations = true;
  }



  /**
   * Retrieves a user-friendly name that can be used to indicate the type of
   * report that will be generated.
   *
   * @return  The user-friendly name that can be used to indicate the type of
   *          report that will be generated.
   */
  public String getReportGeneratorName()
  {
    return "Plain Text Report Generator";
  }



  /**
   * Retrieves a new instance of this report generator initialized with the
   * default configuration.
   *
   * @return  A new instance of this report generator initialized with the
   *          default configuration.
   */
  public ReportGenerator newInstance()
  {
    return new TextReportGenerator();
  }



  /**
   * Retrieves a set of parameters that can be used to allow the user to
   * configure the way that the report is generated.
   *
   * @return  A set of parameters that can be used to allow the user to
   *          configure the way that the report is generated.
   */
  public ParameterList getReportParameterStubs()
  {
    BooleanParameter includeScheduleConfigParameter =
         new BooleanParameter(PARAM_INCLUDE_SCHEDULE_CONFIG,
                              "Include Schedule Configuration",
                              "Indicates whether the schedule configuration " +
                              "information should be included in the report.",
                              includeScheduleConfig);
    BooleanParameter includeJobConfigParameter =
         new BooleanParameter(PARAM_INCLUDE_JOB_CONFIG,
                              "Include Job-Specific Configuration",
                              "Indicates whether the job-specific " +
                              "configuration information should be included " +
                              "in the report.", includeJobConfig);
    BooleanParameter includeStatsParameter =
         new BooleanParameter(PARAM_INCLUDE_STATS, "Include Job Statistics",
                              "Indicates whether the statistics collected " +
                              "from job execution should be included in the " +
                              "report.", includeStats);
    BooleanParameter includeMonitorParameter =
         new BooleanParameter(PARAM_INCLUDE_MONITOR_STATS,
                              "Include Resource Monitor Statistics",
                              "Indicates whether the statistics collected " +
                              "from job execution should be included in the " +
                              "report.", includeMonitorStats);
    BooleanParameter includeDetailParameter =
         new BooleanParameter(PARAM_INCLUDE_DETAILED_STATISTICS,
                              "Include Detailed Statistical Information",
                              "Indicates whether to include a detailed " +
                              "report of the statistics collected",
                              includeDetailedStats);
    BooleanParameter requireStatsParameter =
         new BooleanParameter(PARAM_REQUIRE_STATS,
                              "Only Include Jobs with Statistics",
                              "Indicates whether to only include jobs that " +
                              "have statistics available.", requireStats);
    BooleanParameter summarizeOptimizingParameter =
         new BooleanParameter(PARAM_SUMMARIZE_OPTIMIZING_ITERATIONS,
                              "Summarize Optimizing Job Iterations",
                              "Indicates whether to only provide summary " +
                              "data for optimizing job iterations.",
                              summarizeOptimizingIterations);

    Parameter[] parameters = new Parameter[]
    {
      includeScheduleConfigParameter,
      includeJobConfigParameter,
      includeStatsParameter,
      includeMonitorParameter,
      includeDetailParameter,
      requireStatsParameter,
      summarizeOptimizingParameter,
    };

    return new ParameterList(parameters);
  }



  /**
   * Initializes this reporter based on the parameters customized by the end
   * user.
   *
   * @param  reportParameters  The set of parameters provided by the end user
   *                           that should be used to customize the report.
   */
  public void initializeReporter(ParameterList reportParameters)
  {
    BooleanParameter bp =
         reportParameters.getBooleanParameter(PARAM_INCLUDE_SCHEDULE_CONFIG);
    if (bp != null)
    {
      includeScheduleConfig = bp.getBooleanValue();
    }

    bp = reportParameters.getBooleanParameter(PARAM_INCLUDE_JOB_CONFIG);
    if (bp != null)
    {
      includeJobConfig = bp.getBooleanValue();
    }

    bp = reportParameters.getBooleanParameter(PARAM_INCLUDE_STATS);
    if (bp != null)
    {
      includeStats = bp.getBooleanValue();
    }

    bp = reportParameters.getBooleanParameter(PARAM_INCLUDE_MONITOR_STATS);
    if (bp != null)
    {
      includeMonitorStats = bp.getBooleanValue();
    }

    bp = reportParameters.getBooleanParameter(
                               PARAM_INCLUDE_DETAILED_STATISTICS);
    if (bp != null)
    {
      includeDetailedStats = bp.getBooleanValue();
    }

    bp = reportParameters.getBooleanParameter(PARAM_REQUIRE_STATS);
    if (bp != null)
    {
      requireStats = bp.getBooleanValue();
    }

    bp = reportParameters.getBooleanParameter(
                               PARAM_SUMMARIZE_OPTIMIZING_ITERATIONS);
    if (bp != null)
    {
      summarizeOptimizingIterations = bp.getBooleanValue();
    }
  }



  /**
   * Indicates that information about the provided job should be included in the
   * report.
   *
   * @param  job  The job about which to include information in the report.
   */
  public void addJobReport(Job job)
  {
    if (requireStats && (! job.hasStats()))
    {
      return;
    }

    jobList.add(job);
  }



  /**
   * Indicates that information about the provided optimizing job should be
   * included in the report.
   *
   * @param  optimizingJob  The optimizing job about which to include
   *                        information in the report.
   */
  public void addOptimizingJobReport(OptimizingJob optimizingJob)
  {
    if (requireStats && (! optimizingJob.hasStats()))
    {
      return;
    }

    jobList.add(optimizingJob);
  }



  /**
   * Generates the report and sends it to the user over the provided servlet
   * response.
   *
   * @param  requestInfo  State information about the request being processed.
   */
  public void generateReport(RequestInfo requestInfo)
  {
    StringBuilder buffer = new StringBuilder();
    addHeader(requestInfo, buffer);

    String separator = "----------------------------------------------------" +
                       "-----------------------" + EOL;
    boolean addSeparator = true;

    for (int i=0; i < jobList.size(); i++)
    {
      if (addSeparator)
      {
        buffer.append(EOL);
        buffer.append(separator);
        buffer.append(EOL);
      }

      Object element = jobList.get(i);
      if (element instanceof Job)
      {
        Job job = (Job) element;
        addSeparator = addJob(job, buffer);
      }
      else if (element instanceof OptimizingJob)
      {
        OptimizingJob optimizingJob = (OptimizingJob) element;
        addSeparator = addOptimizingJob(optimizingJob, buffer);
      }
    }

    HttpServletResponse response = requestInfo.getResponse();
    response.setContentType("text/plain");
    response.addHeader("Content-Disposition",
                       "filename=\"slamd_data_report.txt\"");

    try
    {
      PrintWriter writer = response.getWriter();
      writer.println(buffer.toString());
    }
    catch (IOException ioe)
    {
      // Not much we can do about this.
      System.err.println("Unable to generate report:  " + ioe);
    }
  }



  /**
   * Adds a header to the report.
   *
   * @param  requestInfo  State information for the request being processed.
   * @param  buffer       The string buffer to which the report will be added.
   */
  private void addHeader(RequestInfo requestInfo, StringBuilder buffer)
  {
    String title = "SLAMD Generated Report";
    if (jobList.size() == 1)
    {
      Object element = jobList.get(0);
      if (element instanceof Job)
      {
        Job job = (Job) element;
        title += " for Job " + job.getJobID();
      }
      else if (element instanceof OptimizingJob)
      {
        OptimizingJob optimizingJob = (OptimizingJob) element;
        title += " for Optimizing Job " + optimizingJob.getOptimizingJobID();
      }
    }

    buffer.append(title + EOL);
    buffer.append("Generated " + dateFormat.format(new Date()) + EOL);

    HttpServletRequest request = requestInfo.getRequest();


    String serverURL = request.getScheme() + "://" + request.getServerName() +
                       ':' + request.getServerPort() +
                       requestInfo.getServletBaseURI();
    buffer.append("Generated from data at " + serverURL + EOL);
  }



  /**
   * Adds information about the provided job to the report, if appropriate.
   *
   * @param  job     The job to include in the report.
   * @param  buffer  The buffer to which the job information should be written.
   *
   * @return  <CODE>true</CODE> if information about the job was actually
   *          included in the report, or <CODE>false</CODE> if not.
   */
  private boolean addJob(Job job, StringBuilder buffer)
  {
    // Check to see if this is an optimizing job iteration that we should skip.
    String optimizingJobID = job.getOptimizingJobID();
    if (summarizeOptimizingIterations && (optimizingJobID != null) &&
        (optimizingJobID.length() > 0))
    {
      for (int i=0; i < jobList.size(); i++)
      {
        Object element = jobList.get(i);
        if (element instanceof OptimizingJob)
        {
          OptimizingJob optimizingJob = (OptimizingJob) element;
          if (optimizingJob.getOptimizingJobID().equalsIgnoreCase(
                                                      optimizingJobID))
          {
            return false;
          }
        }
      }
    }


    // Write information to the buffer that will always be included.
    buffer.append(job.getJobClass().getJobName() + " Job " + job.getJobID() +
                  EOL);
    buffer.append("Job ID:  " + job.getJobID() + EOL);

    String description = job.getJobDescription();
    if ((description != null) && (description.length() > 0))
    {
      buffer.append("Job Description:  " + description + EOL);
    }

    if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
    {
      buffer.append("Optimizing Job ID:  " + optimizingJobID + EOL);
    }

    buffer.append("Job Type:  " + job.getJobClass().getJobName() + EOL);
    buffer.append("Job Class:  " + job.getJobClassName() + EOL);
    buffer.append("Job State:  " +
                  Constants.jobStateToString(job.getJobState()) + EOL);


    // Write information to the buffer about the schedule configuration.
    if (includeScheduleConfig)
    {
      buffer.append("----- Job Schedule Information -----" + EOL);

      Date scheduledStartTime = job.getStartTime();
      String startTimeStr;
      if (scheduledStartTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(scheduledStartTime);
      }
      buffer.append("Scheduled Start Time:  " + startTimeStr + EOL);

      Date scheduledStopTime = job.getStopTime();
      String stopTimeStr;
      if (scheduledStopTime == null)
      {
        stopTimeStr = "(Not Specified)";
      }
      else
      {
        stopTimeStr = dateFormat.format(scheduledStopTime);
      }
      buffer.append("Scheduled Stop Time:  " + stopTimeStr + EOL);

      int scheduledDuration = job.getDuration();
      String durationStr;
      if (scheduledDuration > 0)
      {
        durationStr = scheduledDuration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      buffer.append("Scheduled Duration:  " + durationStr + EOL);

      int numClients = job.getNumberOfClients();
      buffer.append("Number of Clients:  " + numClients + EOL);

      String[] requestedClients = job.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        buffer.append("Requested Clients:  " + requestedClients[0]);
        for (int i=1; i < requestedClients.length; i++)
        {
          buffer.append(',' + EOL);
          buffer.append("                    " + requestedClients[i] + EOL);
        }
        buffer.append(EOL);
      }

      int numThreads = job.getThreadsPerClient();
      buffer.append("Number of Threads per Client:  " + numThreads + EOL);

      int startupDelay = job.getThreadStartupDelay();
      buffer.append("Thread Startup Delay:  " + startupDelay + EOL);

      int collectionInterval = job.getCollectionInterval();
      buffer.append("Statistics Collection Interval:  " + collectionInterval +
                    " seconds" + EOL);
    }


    // Write information to the buffer about the job-specific configuration.
    if (includeJobConfig)
    {
      buffer.append("----- Job-Specific Configuration -----" + EOL);
      Parameter[] params = job.getParameterList().getParameters();
      for (int i=0; i < params.length; i++)
      {
        buffer.append(params[i].getDisplayName() + ":  " +
                      params[i].getDisplayValue() + EOL);
      }
    }


    // Write information to the buffer about the job statistics.
    if (includeStats && job.hasStats())
    {
      buffer.append("----- General Statistical Information -----" + EOL);

      Date actualStartTime = job.getActualStartTime();
      String startTimeStr;
      if (actualStartTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(actualStartTime);
      }
      buffer.append("Actual Start Time:  " + startTimeStr + EOL);

      Date actualStopTime = job.getActualStopTime();
      String stopTimeStr;
      if (actualStopTime == null)
      {
        stopTimeStr = "(Not Available)";
      }
      else
      {
        stopTimeStr = dateFormat.format(actualStopTime);
      }
      buffer.append("Actual Stop Time:  " + stopTimeStr + EOL);

      int actualDuration = job.getActualDuration();
      String durationStr;
      if (actualDuration > 0)
      {
        durationStr = actualDuration + " seconds";
      }
      else
      {
        durationStr = "(Not Available)";
      }
      buffer.append("Actual Duration:  " + durationStr + EOL);

      String[] clients = job.getStatTrackerClientIDs();
      if ((clients != null) && (clients.length > 0))
      {
        buffer.append("Clients Used:  " + clients[0]);
        for (int i=1; i < clients.length; i++)
        {
          buffer.append(',' + EOL);
          buffer.append("               " + clients[i]);
        }
        buffer.append(EOL);
      }

      String[] statNames = job.getStatTrackerNames();
      for (int i=0; i < statNames.length; i++)
      {
        StatTracker[] trackers = job.getStatTrackers(statNames[i]);
        if ((trackers == null) || (trackers.length == 0))
        {
          continue;
        }

        StatTracker tracker = trackers[0].newInstance();
        tracker.aggregate(trackers);

        buffer.append("----- " + statNames[i] + " Statistics -----" + EOL);
        if (includeDetailedStats)
        {
          buffer.append(tracker.getDetailString());
        }
        else
        {
          buffer.append(tracker.getSummaryString() + EOL);
        }
      }
    }


    // Write information to the buffer about the resource monitor statistics.
    if (includeMonitorStats && job.hasResourceStats())
    {
      buffer.append("----- Resource Monitor Statistics -----" + EOL);
      String[] statNames = job.getResourceStatTrackerNames();
      for (int i=0; i < statNames.length; i++)
      {
        StatTracker[] trackers = job.getResourceStatTrackers(statNames[i]);
        if ((trackers == null) || (trackers.length == 0))
        {
          continue;
        }

        StatTracker tracker = trackers[0].newInstance();
        tracker.aggregate(trackers);

        buffer.append("----- " + statNames[i] +
                      " Resource Monitor Statistics -----" + EOL);
        if (includeDetailedStats)
        {
          buffer.append(tracker.getDetailString());
        }
        else
        {
          buffer.append(tracker.getSummaryString() + EOL);
        }
      }
    }


    return true;
  }



  /**
   * Adds information about the provided optimizing job to the report,
   * if appropriate.
   *
   * @param  optimizingJob  The optimizing job to include in the report.
   * @param  buffer         The buffer to which the optimizing job information
   *                        should be written.
   *
   * @return  <CODE>true</CODE> if information about the optimizing job was
   *          actually included in the report, or <CODE>false</CODE> if not.
   */
  private boolean addOptimizingJob(OptimizingJob optimizingJob,
                                   StringBuilder buffer)
  {
    // Get the optimization algorithm and set of parameters.
    OptimizationAlgorithm optimizationAlgorithm =
         optimizingJob.getOptimizationAlgorithm();
    ParameterList paramList =
         optimizationAlgorithm.getOptimizationAlgorithmParameters();
    Parameter[] optimizationParams = paramList.getParameters();


    // Write information to the buffer that will always be included.
    buffer.append("Optimizing " + optimizingJob.getJobClass().getJobName() +
                  " Job " + optimizingJob.getOptimizingJobID() +
                  EOL);
    buffer.append("Optimizing Job ID:  " + optimizingJob.getOptimizingJobID() +
                  EOL);

    String description = optimizingJob.getDescription();
    if ((description != null) && (description.length() > 0))
    {
      buffer.append("Job Description:  " + description + EOL);
    }

    buffer.append("Job Type:  " + optimizingJob.getJobClass().getJobName() +
                  EOL);
    buffer.append("Job Class:  " + optimizingJob.getJobClassName() + EOL);
    buffer.append("Job State:  " +
                  Constants.jobStateToString(optimizingJob.getJobState()) +
                  EOL);


    // Write information to the buffer about the schedule configuration.
    if (includeScheduleConfig)
    {
      buffer.append("----- Job Schedule Information -----" + EOL);

      Date scheduledStartTime = optimizingJob.getStartTime();
      String startTimeStr;
      if (scheduledStartTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(scheduledStartTime);
      }
      buffer.append("Scheduled Start Time:  " + startTimeStr + EOL);

      int scheduledDuration = optimizingJob.getDuration();
      String durationStr;
      if (scheduledDuration > 0)
      {
        durationStr = scheduledDuration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      buffer.append("Scheduled Duration:  " + durationStr + EOL);

      int numClients = optimizingJob.getNumClients();
      buffer.append("Number of Clients:  " + numClients);

      String[] requestedClients = optimizingJob.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        buffer.append("Requested Clients:  " + requestedClients[0]);
        for (int i=1; i < requestedClients.length; i++)
        {
          buffer.append(',' + EOL);
          buffer.append("                    " + requestedClients[i] + EOL);
        }
        buffer.append(EOL);
      }

      int minThreads = optimizingJob.getMinThreads();
      buffer.append("Minimum Number of Threads:  " + minThreads + EOL);

      int maxThreads = optimizingJob.getMaxThreads();
      String maxStr;
      if (maxThreads > 0)
      {
        maxStr = String.valueOf(maxThreads);
      }
      else
      {
        maxStr = "(Not Specified)";
      }
      buffer.append("Maximum Number of Threads:  " + maxStr + EOL);

      int threadIncrement = optimizingJob.getThreadIncrement();
      buffer.append("Thread Increment Between Iterations:  " + threadIncrement +
                    EOL);

      int collectionInterval = optimizingJob.getCollectionInterval();
      buffer.append("Statistics Collection Interval:  " + collectionInterval +
                    " seconds" + EOL);

      for (int i=0; i < optimizationParams.length; i++)
      {
        buffer.append(optimizationParams[i].getDisplayName());
        buffer.append(":  ");
        buffer.append(optimizationParams[i].getDisplayValue());
        buffer.append(EOL);
      }

      int maxNonImproving = optimizingJob.getMaxNonImproving();
      buffer.append("Maximum Consecutive Non-Improving Iterations:  " +
                    maxNonImproving + EOL);

      boolean reRun = optimizingJob.reRunBestIteration();
      buffer.append("Re-Run Best Iteration:  " + String.valueOf(reRun) + EOL);

      int reRunDuration = optimizingJob.getReRunDuration();
      if (reRunDuration > 0)
      {
        durationStr = reRunDuration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      buffer.append("Re-Run Duration:  " + durationStr + EOL);
    }


    // Write information to the buffer about the job-specific configuration.
    if (includeJobConfig)
    {
      buffer.append("----- Job-Specific Configuration -----" + EOL);
      Parameter[] params = optimizingJob.getParameters().getParameters();
      for (int i=0; i < params.length; i++)
      {
        buffer.append(params[i].getDisplayName() + ":  " +
                      params[i].getDisplayValue() + EOL);
      }
    }


    // Write information to the buffer about the job statistics.
    if (includeStats && optimizingJob.hasStats())
    {
      buffer.append("----- General Statistical Information -----" + EOL);

      Date actualStartTime = optimizingJob.getActualStartTime();
      String startTimeStr;
      if (actualStartTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(actualStartTime);
      }
      buffer.append("Actual Start Time:  " + startTimeStr + EOL);

      Date actualStopTime = optimizingJob.getActualStopTime();
      String stopTimeStr;
      if (actualStopTime == null)
      {
        stopTimeStr = "(Not Available)";
      }
      else
      {
        stopTimeStr = dateFormat.format(actualStopTime);
      }
      buffer.append("Actual Stop Time:  " + stopTimeStr + EOL);

      Job[] iterations = optimizingJob.getAssociatedJobs();
      if ((iterations != null) && (iterations.length > 0))
      {
        buffer.append("Job Iterations Completed:  " + iterations.length + EOL);

        int optimalThreads = optimizingJob.getOptimalThreadCount();
        buffer.append("Optimal Thread Count:  " + optimalThreads + EOL);

        double optimalValue = optimizingJob.getOptimalValue();
        buffer.append("Optimal Value:  " + decimalFormat.format(optimalValue) +
                      EOL);

        Job reRunIteration = optimizingJob.getReRunIteration();
        if (reRunIteration != null)
        {
          String valueStr;

          try
          {
            double value =
                 optimizationAlgorithm.getIterationOptimizationValue(
                                            reRunIteration);
            valueStr = decimalFormat.format(value);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          buffer.append("Re-Run Value:  " + valueStr + EOL);
        }


        buffer.append("----- Optimizing Job Iterations -----" + EOL);
        for (int i=0; i < iterations.length; i++)
        {
          String valueStr;

          try
          {
            double value =
                 optimizationAlgorithm.getIterationOptimizationValue(
                                            iterations[i]);
            valueStr = decimalFormat.format(value);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          buffer.append(iterations[i].getJobID() + ":  " + valueStr + EOL);
        }
      }
    }


    return true;
  }
}

