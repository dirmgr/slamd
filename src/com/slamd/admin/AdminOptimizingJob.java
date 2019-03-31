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
package com.slamd.admin;



import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sleepycat.je.DatabaseException;

import com.slamd.common.Constants;
import com.slamd.common.DurationParser;
import com.slamd.common.SLAMDException;
import com.slamd.db.DecodeException;
import com.slamd.db.JobFolder;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.job.SingleStatisticOptimizationAlgorithm;
import com.slamd.job.UnknownJobClass;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.server.SLAMDServerException;
import com.slamd.server.UploadedFile;

import static com.unboundid.util.StaticUtils.secondsToHumanReadableDuration;

import static com.slamd.admin.AdminJob.*;
import static com.slamd.admin.AdminServlet.*;
import static com.slamd.admin.AdminUI.*;



/**
 * This class provides a set of methods for providing logic for managing
 * optimizing jobs.
 */
public final class AdminOptimizingJob
{
  /**
   * Prevent this utility class from being instantiated.
   */
  private AdminOptimizingJob()
  {
    // No implementation required.
  }



  /**
   * Handles the work of retrieving information about the specified optimizing
   * job as a plain text document.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewOptimizingJobAsText(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewOptimizingJobAsText()");


    // The user must have permission to view job information in order to see
    // this section.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state information for the request.
    HttpServletRequest request  = requestInfo.request;
    HttpServletResponse response = requestInfo.response;


    // Prepare to send the entire response to the client.
    PrintWriter writer;
    try
    {
      writer = response.getWriter();
      response.setContentType("text/plain");
      requestInfo.generateHTML = false;
    }
    catch (IOException ioe)
    {
      // What can we do here?
      ioe.printStackTrace();
      return;
    }


    boolean onlyState = false;
    String stateStr = request.getParameter(Constants.SERVLET_PARAM_ONLY_STATE);
    if (stateStr != null)
    {
      onlyState = (stateStr.equalsIgnoreCase("true") ||
                   stateStr.equalsIgnoreCase("yes") ||
                   stateStr.equalsIgnoreCase("on") ||
                   stateStr.equalsIgnoreCase("1"));
    }


    // Get the ID of the optimizing job to retrieve.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      writer.println("ERROR:  No optimizing job ID specified in the request.");
      return;
    }


    // Get the optimizing job with the requested ID.
    OptimizingJob optimizingJob;
    try
    {
      optimizingJob = getOptimizingJob(optimizingJobID);
      if (optimizingJob == null)
      {
        if (onlyState)
        {
          writer.println(Constants.JOB_STATE_NO_SUCH_JOB);
        }
        else
        {
          writer.println("ERROR:  Could not retrieve optimizing job \"" +
                         optimizingJobID + '"');
        }

        return;
      }
    }
    catch (Exception e)
    {
      if (onlyState)
      {
        writer.println(Constants.JOB_STATE_NO_SUCH_JOB);
      }
      else
      {
        writer.println("ERROR:  Could not retrieve optimizing job \"" +
                       optimizingJobID + '"');
      }

      return;
    }


    // Determine whether we should only return the job state.
    if (onlyState)
    {
      writer.println(optimizingJob.getJobState());
      return;
    }


    // Write information to the buffer that will always be included.
    writer.println("Optimizing " + optimizingJob.getJobClass().getJobName() +
                   " Job " + optimizingJob.getOptimizingJobID());
    writer.println("Optimizing Job ID:  " + optimizingJob.getOptimizingJobID());

    String description = optimizingJob.getDescription();
    if ((description != null) && (description.length() > 0))
    {
      writer.println("Job Description:  " + description);
    }

    writer.println("Job Type:  " + optimizingJob.getJobClass().getJobName());
    writer.println("Job Class:  " + optimizingJob.getJobClassName());
    writer.println("Job State:  " +
                   Constants.jobStateToString(optimizingJob.getJobState()));


    // Write information to the buffer about the schedule configuration.
    writer.println("----- Job Schedule Information -----");

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
    writer.println("Scheduled Start Time:  " + startTimeStr);

    int scheduledDuration = optimizingJob.getDuration();
    String durationStr;
    if (scheduledDuration > 0)
    {
      durationStr = secondsToHumanReadableDuration(scheduledDuration);
    }
    else
    {
      durationStr = "(Not Specified)";
    }
    writer.println("Scheduled Duration:  " + durationStr);

    int numClients = optimizingJob.getNumClients();
    writer.println("Number of Clients:  " + numClients);

    String[] requestedClients = optimizingJob.getRequestedClients();
    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      writer.print("Requested Clients:  " + requestedClients[0]);
      for (int i=1; i < requestedClients.length; i++)
      {
        writer.println(",");
        writer.print("                    " + requestedClients[i]);
      }
      writer.println();
    }

    int minThreads = optimizingJob.getMinThreads();
    writer.println("Minimum Number of Threads:  " + minThreads);

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
    writer.println("Maximum Number of Threads:  " + maxStr);

    int threadIncrement = optimizingJob.getThreadIncrement();
    writer.println("Thread Increment Between Iterations:  " + threadIncrement);

    int collectionInterval = optimizingJob.getCollectionInterval();
    writer.println("Statistics Collection Interval:  " +
         secondsToHumanReadableDuration(collectionInterval));

    OptimizationAlgorithm optimizationAlgorithm =
         optimizingJob.getOptimizationAlgorithm();
    Parameter[] params =
         optimizationAlgorithm.getOptimizationAlgorithmParameters().
                                    getParameters();
    for (int i=0; i < params.length; i++)
    {
      writer.println(params[i].getDisplayName() + ":  " +
                     params[i].getDisplayValue());
    }

    int maxNonImproving = optimizingJob.getMaxNonImproving();
    writer.println("Max. Consecutive Non-Improving Iterations:  " +
                   maxNonImproving);

    boolean reRun = optimizingJob.reRunBestIteration();
    writer.println("Re-Run Best Iteration:  " + String.valueOf(reRun));

    int reRunDuration = optimizingJob.getReRunDuration();
    if (reRunDuration > 0)
    {
      durationStr = secondsToHumanReadableDuration(reRunDuration);
    }
    else
    {
      durationStr = "(Not Specified)";
    }
    writer.println("Re-Run Duration:  " + durationStr);


    // Write information to the buffer about the job-specific configuration.
    writer.println("----- Job-Specific Configuration -----");
    params = optimizingJob.getParameters().getParameters();
    for (int i=0; i < params.length; i++)
    {
      writer.println(params[i].getDisplayName() + ":  " +
                     params[i].getDisplayValue());
    }


    // Write information to the buffer about the job statistics.
    if (optimizingJob.hasStats())
    {
      writer.println("----- General Statistical Information -----");

      Date actualStartTime = optimizingJob.getActualStartTime();
      if (actualStartTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(actualStartTime);
      }
      writer.println("Actual Start Time:  " + startTimeStr);

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
      writer.println("Actual Stop Time:  " + stopTimeStr);

      Job[] iterations = optimizingJob.getAssociatedJobs();
      if ((iterations != null) && (iterations.length > 0))
      {
        writer.println("Job Iterations Completed:  " + iterations.length);

        int optimalThreads = optimizingJob.getOptimalThreadCount();
        writer.println("Optimal Thread Count:  " + optimalThreads);

        double optimalValue = optimizingJob.getOptimalValue();
        writer.println("Optimal Value:  " + decimalFormat.format(optimalValue));

        Job reRunIteration = optimizingJob.getReRunIteration();
        if (reRunIteration != null)
        {
          String valueStr;

          try
          {
            double value = optimizationAlgorithm.getIterationOptimizationValue(
                                                      reRunIteration);
            valueStr = decimalFormat.format(value);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          writer.println("Re-Run Value:  " + valueStr);
        }


        writer.println("----- Optimizing Job Iterations -----");
        for (int i=0; i < iterations.length; i++)
        {
          String valueStr;

          try
          {
            double value = optimizationAlgorithm.getIterationOptimizationValue(
                                                      iterations[i]);
            valueStr = decimalFormat.format(value);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          writer.println(iterations[i].getJobID() + ":  " + valueStr);
        }
      }
    }
  }



  /**
   * Handles all processing associated with scheduling a self-optimizing job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleOptimizeJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleOptimizeJob()");

    // If the user doesn't have permission to schedule jobs, then they can't
    // see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "schedule jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // There should have been either a job ID specified or a job class.  Look
    // for a job ID first and if that is not present, then get the job class
    // name.
    Job           job                    = null;
    JobClass jobClass               = null;
    ParameterList optimizationParameters = null;
    ParameterList parameters             = null;
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID != null) && (jobID.length() > 0))
    {
      try
      {
        job = configDB.getJob(jobID);
      }
      catch (Exception e)
      {
        infoMessage.append("ERROR:  Unable to retrieve job \"" + jobID +
                           "\" -- " + e.getMessage() + "<BR>" + EOL);
        return;
      }

      if (job == null)
      {
        infoMessage.append("ERROR:  Unable to retrieve job \"" + jobID +
                           "\" from the configuration directory.<BR>" + EOL);
        return;
      }

      jobClass = job.getJobClass();
      parameters = job.getParameterList().clone();
    }
    else
    {
      String jobClassName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
      if ((jobClassName == null) || (jobClassName.length() == 0))
      {
        infoMessage.append("ERROR:  Neither a job ID nor a job class name " +
                           "was provided to use for the optimizing job.<BR>" +
                           EOL);
        return;
      }

      jobClass = slamdServer.getJobClass(jobClassName);
      if (jobClass == null)
      {
        infoMessage.append("ERROR:  Unknown job class \"" + jobClassName +
                           "\" specified for the optimizing job.<BR>" + EOL);
        return;
      }

      // This is a temporary placeholder.  We'll fill in the values later.
      parameters = jobClass.getParameterStubs().clone();
    }


    // Figure out which optimization algorithm should be used.  If one was
    // specified in the request, then assume it has already been provided.
    // Otherwise, show the user a set of the options available to them.
    OptimizationAlgorithm optimizationAlgorithm;
    String optimizationAlgorithmClass =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM);
    if ((optimizationAlgorithmClass == null) ||
        (optimizationAlgorithmClass.length() == 0))
    {
      // Get a list of the available optimization algorithms that can be used
      // for this job and allow the user to choose which one he/she wants.
      // If none are available, display an error message.
      ArrayList<OptimizationAlgorithm> availableAlgorithmList =
           new ArrayList<OptimizationAlgorithm>();
      OptimizationAlgorithm[] algorithms =
           slamdServer.getOptimizationAlgorithms();
      if ((algorithms == null) || (algorithms.length == 0))
      {
        infoMessage.append("WARNING:  No optimization algorithms have been " +
                           "configured in the server.  Using the default.<BR>" +
                           EOL);
        algorithms = new OptimizationAlgorithm[]
        {
          new SingleStatisticOptimizationAlgorithm()
        };
      }

      for (int i=0; i < algorithms.length; i++)
      {
        if (algorithms[i].availableWithJobClass(jobClass))
        {
          availableAlgorithmList.add(algorithms[i]);
        }
      }

      if (availableAlgorithmList.isEmpty())
      {
        infoMessage.append("ERROR:  There are no optimization algorithms " +
                           "configured in the server that may be used with " +
                           jobClass.getJobName() + " jobs.<BR>" + EOL);
        if (job == null)
        {
          handleViewOptimizing(requestInfo, false);
        }
        else
        {
          handleViewJob(requestInfo,
               Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED, null, jobID);
        }
        return;
      }
      else if (availableAlgorithmList.size() == 1)
      {
        optimizationAlgorithm = availableAlgorithmList.get(0);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Select an Optimization Algorithm</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Select the optimization algorithm that you wish to " +
                        "use for this optimizing " + jobClass.getJobName() +
                        " job." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" +
                        requestInfo.servletBaseURI + "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              requestInfo.section) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       requestInfo.subsection) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                       jobClass.getClass().getName()) + EOL);
        if (jobID != null)
        {
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                                jobID) + EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <SELECT NAME=\"" +
                        Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM + "\">" +
                        EOL);
        for (int i=0; i < availableAlgorithmList.size(); i++)
        {
          OptimizationAlgorithm algorithm = availableAlgorithmList.get(i);
          htmlBody.append("    <OPTION VALUE=\"" +
                          algorithm.getClass().getName() + "\">" +
                          algorithm.getOptimizationAlgorithmName() + EOL);
        }
        htmlBody.append("  </SELECT>" + EOL);
        htmlBody.append("  <BR><BR>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Continue\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        return;
      }
    }
    else
    {
      optimizationAlgorithm =
           slamdServer.getOptimizationAlgorithm(optimizationAlgorithmClass);
      if (optimizationAlgorithm == null)
      {
        infoMessage.append("ERROR:  Undefined optimization algorithm class \"" +
                           optimizationAlgorithmClass +
                           "\" specified for the optimizing job.<BR>" + EOL);
        if (job == null)
        {
          handleViewOptimizing(requestInfo, false);
        }
        else
        {
          handleViewJob(requestInfo,
               Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED, null, jobID);
        }
        return;
      }
    }


    // See if the user has submitted the form that should allow them to specify
    // the information needed for generating the optimizing job.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      if (confirmStr.equals(Constants.SUBMIT_STRING_CANCEL))
      {
        infoMessage.append("The self-optimizing job was not scheduled.<BR>" +
                           EOL);
        if (job == null)
        {
          handleViewOptimizing(requestInfo, false);
        }
        else
        {
          handleViewJob(requestInfo,
               Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED, null, jobID);
        }
        return;
      }


      // The user wants to schedule the job.  Set up the variables to do this.
      boolean  displayInReadOnlyMode     = false;
      boolean  jobIsValid                = true;
      boolean  includeThreadCount        = false;
      boolean  monitorClientsIfAvailable = false;
      boolean  reRunBestIteration        = false;
      Date     startTime                 = null;
      int      collectionInterval        = 60;
      int      delayBetweenJobs          = 0;
      int      duration                  = -1;
      int      numClients                = -1;
      int      maxNonImproving           = -1;
      int      maxThreads                = -1;
      int      minThreads                = -1;
      int      reRunDuration             = -1;
      int      threadIncrement           = -1;
      int      threadStartupDelay        = 0;
      String   description               = null;
      String   folderName                = null;
      String[] notifyAddresses           = null;
      String[] requestedClients          = null;
      String[] monitorClients            = null;


      // Get the description specified by the user.  We don't require any
      // validation for this.
      description =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);


      // Get the flag that specifies whether to include the thread count in the
      // description.
      includeThreadCount = false;
      String includeThreadStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION);
      if (includeThreadStr != null)
      {
        includeThreadCount = (includeThreadStr.equalsIgnoreCase("true") ||
                              includeThreadStr.equalsIgnoreCase("yes") ||
                              includeThreadStr.equalsIgnoreCase("on") ||
                              includeThreadStr.equalsIgnoreCase("1"));
      }


      // Get the flag that specifies whether to display the optimizing job in
      // read-only mode.
      displayInReadOnlyMode = false;
      String displayStr =
           request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
      if (displayStr != null)
      {
        displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                                 displayStr.equalsIgnoreCase("yes") ||
                                 displayStr.equalsIgnoreCase("on") ||
                                 displayStr.equalsIgnoreCase("1"));
      }


      // Get the start time for the first iteration.  If it is specified, then
      // it must be 14 digits in the form "YYYYMMDDhhmmss".
      String startTimeStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
      if ((startTimeStr == null) || (startTimeStr.length() == 0))
      {
        startTime = new Date();
      }
      else
      {
        if (startTimeStr.length() != 14)
        {
          infoMessage.append("ERROR:  The job start time must be in the " +
                             "form YYYYMMDDhhmmss (14 digits).<BR>" + EOL);
          jobIsValid = false;
        }
        else
        {
          try
          {
            startTime = dateFormat.parse(startTimeStr);
          }
          catch (ParseException pe)
          {
            infoMessage.append("ERROR:  Unable to parse the job start " +
                               "time \"" + startTimeStr +
                               "\" as a date in the form YYYYMMDDhhmmss.<BR>" +
                               EOL);
            jobIsValid = false;
          }
        }
      }


      // Get the duration for the job.  If it is specified, it must be a
      // positive integer.
      String durationStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
      if ((durationStr == null) || (durationStr.length() == 0))
      {
        duration = -1;
      }
      else
      {
        try
        {
          duration = DurationParser.parse(durationStr);
          if (duration <= 0)
          {
            infoMessage.append("ERROR:  The job duration must be positive." +
                               "<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the delay between iterations.  This must be specified and must be
      // an integer greater than or equal to zero.
      String delayStr =
           request.getParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
      if ((delayStr == null) || (delayStr.length() == 0))
      {
        infoMessage.append("ERROR:  No value specified for the delay between " +
                           "iterations.<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          delayBetweenJobs = Integer.parseInt(delayStr);
          if (delayBetweenJobs < 0)
          {
            infoMessage.append("ERROR:  The delay between job iterations " +
                               "must be greater than or equal to zero.<BR>" +
                               EOL);
            jobIsValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the delay between job " +
                             "iterations \"" + delayStr +
                             "\" as an integer value.<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the number of clients.  This must be specified and must be a
      // positive integer.
      String clientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((clientsStr == null) || (clientsStr.length() == 0))
      {
        infoMessage.append("ERROR:  No value specified for the number of " +
                           "clients to use.<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          numClients = Integer.parseInt(clientsStr);
          if (numClients <= 0)
          {
            infoMessage.append("ERROR:  The number of clients must be " +
                               "positive.<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the number of clients " +
                             '"' + clientsStr + "\" as an integer value.<BR>" +
                             EOL);
          jobIsValid = false;
        }
      }


      // Get the addresses of any specific clients that should be used to run
      // the job.  Make sure that all client addresses are resolvable.
      clientsStr = request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
      if ((clientsStr == null) || (clientsStr.length() == 0))
      {
        requestedClients = new String[0];
      }
      else
      {
        ArrayList<String> clientList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(clientsStr);
        while (tokenizer.hasMoreTokens())
        {
          String address = tokenizer.nextToken();
          try
          {
            InetAddress clientAddress = InetAddress.getByName(address);
            clientList.add(clientAddress.getHostAddress());
          }
          catch (UnknownHostException uhe)
          {
            infoMessage.append("ERROR:  Unable to resolve client address \"" +
                               address + "\".<BR>" + EOL);
            jobIsValid = false;
          }
        }
        requestedClients = new String[clientList.size()];
        clientList.toArray(requestedClients);

        if (requestedClients.length > numClients)
        {
          infoMessage.append("ERROR:  Specific set of requested clients may " +
                             "not contain more than the specified number of " +
                             "clients.<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the addresses of any resource monitor clients that should be used.
      // Make sure that all addresses are resolvable.
      clientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
      if ((clientsStr == null) || (clientsStr.length() == 0))
      {
        monitorClients = new String[0];
      }
      else
      {
        ArrayList<String> clientList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(clientsStr);
        while (tokenizer.hasMoreTokens())
        {
          String address = tokenizer.nextToken();
          try
          {
            InetAddress clientAddress = InetAddress.getByName(address);
            clientList.add(clientAddress.getHostAddress());
          }
          catch (UnknownHostException uhe)
          {
            infoMessage.append("ERROR:  Unable to resolve client address \"" +
                               address + "\".<BR>" + EOL);
            jobIsValid = false;
          }
        }
        monitorClients = new String[clientList.size()];
        clientList.toArray(monitorClients);
      }


      // Determine whether to automatically monitor any client systems that may
      // be used.
      String monitorStr = request.getParameter(
           Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
      if ((monitorStr != null) && (monitorStr.length() > 0))
      {
        monitorClientsIfAvailable = (monitorStr.equalsIgnoreCase("true") ||
                                     monitorStr.equalsIgnoreCase("yes") ||
                                     monitorStr.equalsIgnoreCase("on") ||
                                     monitorStr.equalsIgnoreCase("1"));
      }


      // Get the minimum number of threads per client.  This must be specified
      // and must be a positive integer.
      String minThreadStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
      if ((minThreadStr == null) || (minThreadStr.length() == 0))
      {
        infoMessage.append("ERROR:  No value specified for the minimum " +
                           "number of threads to use.<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          minThreads = Integer.parseInt(minThreadStr);
          if (minThreads <= 0)
          {
            infoMessage.append("ERROR:  The minimum number of threads must " +
                               "be positive.<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the minimum number of " +
                             "threads \"" + minThreadStr +
                             "\" as an integer value.<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the maximum number of threads per client.  This does not have to
      // be specified, but if it is then it must be a positive integer value
      // greater than the minimum number of threads.
      String maxThreadStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX);
      if ((maxThreadStr == null) || (maxThreadStr.length() == 0))
      {
        maxThreads = -1;
      }
      else
      {
        try
        {
          maxThreads = Integer.parseInt(maxThreadStr);
          if ((minThreads > 0) && (maxThreads < minThreads))
          {
            infoMessage.append("ERROR:  The maximum number of threads must " +
                               "be greater than or equal to the minimum " +
                               "number of threads.<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the maximum number of " +
                             "threads \"" + maxThreadStr +
                             "\" as an integer value.<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the increment to use when increasing the number of threads between
      // iterations.  It must be specified and it must be a positive integer.
      String incrementStr =
           request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
      if ((incrementStr == null) || (incrementStr.length() == 0))
      {
        infoMessage.append("ERROR:  No value specified for the thread " +
                           "increment.<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          threadIncrement = Integer.parseInt(incrementStr);
          if (threadIncrement <= 0)
          {
            infoMessage.append("ERROR:  The thread increment must be greater " +
                               "than zero.<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the thread increment \"" +
                             incrementStr + "\" as an integer value.<BR>" +
                             EOL);
          jobIsValid = false;
        }
      }



      // Get the thread startup delay.  It is optional.
      String startupDelayStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
      if ((startupDelayStr != null) && (startupDelayStr.length() > 0))
      {
        try
        {
          threadStartupDelay = Integer.parseInt(startupDelayStr);
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the thread startup " +
                             "delay \"" + startupDelayStr +
                             "\" as an integer value.<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the statistics collection interval.  If it is specified, then it
      // must be positive and should not be greater than or equal to the
      // duration.
      String intervalStr =
           request.getParameter(
                        Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr != null) && (intervalStr.length() > 0))
      {
        try
        {
          collectionInterval = DurationParser.parse(intervalStr);
          if (collectionInterval <= 0)
          {
            infoMessage.append("ERROR:  Statistics collection interval must " +
                               "be greater than zero.<BR>" + EOL);
            jobIsValid = false;
          }
          else if ((duration > 0) && (collectionInterval >= duration))
          {
            infoMessage.append("ERROR:  Statistics collection interval must " +
                               "be less than the maximum duration.<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
          jobIsValid = false;
        }
      }


      // Get the maximum number of non-improving iterations.  It must be
      // specified and must be an integer value greater than or equal to zero.
      String nonImprovingStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
      if ((nonImprovingStr == null) || (nonImprovingStr.length() == 0))
      {
        infoMessage.append("ERROR:  No value specified for the maximum " +
                           "number of consecutive non-improving " +
                           "iterations.<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          maxNonImproving = Integer.parseInt(nonImprovingStr);
          if (maxNonImproving < 0)
          {
            infoMessage.append("ERROR:  Maximum number of consecutive " +
                               "non-improving iterations must be greater " +
                               "than or equal to zero.<BR>" + EOL);
            jobIsValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Unable to parse the maximum number of " +
                             "consecutive non-improving iterations \"" +
                             nonImprovingStr + "\" as an integer value.<BR>" +
                             EOL);
          jobIsValid = false;
        }
      }


      // Determine whether to re-run the best iteration.
      String reRunStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
      if (reRunStr != null)
      {
        reRunBestIteration = (reRunStr.equalsIgnoreCase("true") ||
                              reRunStr.equalsIgnoreCase("yes") ||
                              reRunStr.equalsIgnoreCase("on") ||
                              reRunStr.equalsIgnoreCase("1"));
      }


      // Determine the duration to use when re-running the best iteration.
      String reRunDurationStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_DURATION);
      if (reRunDurationStr != null)
      {
        try
        {
          reRunDuration = DurationParser.parse(reRunDurationStr);
        } catch (SLAMDException se) {}
      }



      // Get the e-mail address(es) to be notified when the optimization process
      // is complete.
      String addressStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS);
      if ((addressStr == null) || (addressStr.length() == 0))
      {
        notifyAddresses = new String[0];
      }
      else
      {
        ArrayList<String> addressList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(addressStr, ", ");
        while (tokenizer.hasMoreTokens())
        {
          addressList.add(tokenizer.nextToken());
        }
        notifyAddresses = new String[addressList.size()];
        addressList.toArray(notifyAddresses);
      }


      // Get the optimizing job comments.
      String comments =
           request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);
      if ((comments == null) || (comments.length() == 0))
      {
        comments = null;
      }


      // Get the dependencies to use for the optimizing job.
      String[] dependencies =
           request.getParameterValues(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
      if (dependencies == null)
      {
        dependencies = new String[0];
      }


      // Get the folder name to use when creating the job.
      folderName = request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);


      // Get the optimization algorithm configuration parameters.
      Parameter[] params = optimizationAlgorithm.
           getOptimizationAlgorithmParameterStubs(jobClass).clone().
                 getParameters();
      for (int i=0; i < params.length; i++)
      {
        try
        {
          params[i].htmlInputFormToValue(
               request.getParameterValues(
                    Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                    params[i].getName()));
        }
        catch (InvalidValueException ive)
        {
          infoMessage.append("ERROR:  The value for " +
                             params[i].getDisplayName() + " is invalid -- " +
                             ive.getMessage() + "<BR>" + EOL);
          jobIsValid = false;
        }
      }
      optimizationParameters = new ParameterList(params);


      // Get the job-specific parameters.
      params = jobClass.getParameterStubs().clone().getParameters();
      for (int i=0; i < params.length; i++)
      {
        try
        {
          params[i].htmlInputFormToValue(
               request.getParameterValues(
                    Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                    params[i].getName()));
        }
        catch (InvalidValueException ive)
        {
          infoMessage.append("ERROR:  The value for " +
                             params[i].getDisplayName() + " is invalid -- " +
                             ive.getMessage() + "<BR>" + EOL);
          jobIsValid = false;
        }
      }
      parameters.setParameters(params);


      // Validate the configuration of the job as a whole.
      if (jobIsValid)
      {
        try
        {
          jobClass.validateJobInfo(numClients, minThreads, 0, startTime, null,
                                   duration, collectionInterval, parameters);
        }
        catch (InvalidValueException ive)
        {
          infoMessage.append("ERROR:  The information provided is not valid " +
                             "for this job:  " + ive.getMessage() + "<BR>" +
                             EOL);
          jobIsValid = false;
        }
      }


      // If the job is valid, then determine if we should test the job
      // parameters or schedule it for execution.  If it is not valid, then
      // return the user to the schedule form.
      if (jobIsValid)
      {
        if ((confirmStr != null) &&
            (confirmStr.equals(Constants.SUBMIT_STRING_TEST_PARAMS)))
        {
          ArrayList<String> messageList = new ArrayList<String>();

          try
          {
            jobClass.testJobParameters(parameters, messageList);
          }
          catch (Exception e)
          {
            messageList.add("ERROR:  Exception caught while testing job " +
                            "parameters:  " + JobClass.stackTraceToString(e));
          }

          infoMessage.append("Results of testing job parameters:<BR><BR>" +
                             EOL);
          for (int i=0; i < messageList.size(); i++)
          {
            Object o = messageList.get(i);
            if (o != null)
            {
              infoMessage.append(o.toString() + "<BR>" + EOL);
            }
          }

          if (job == null)
          {
            generateOptimizeJobForm(requestInfo, jobClass,
                                    optimizationAlgorithm);
          }
          else
          {
            generateOptimizeJobForm(requestInfo, job, optimizationAlgorithm);
          }
          return;
        }
        else
        {
          String optimizingJobID = scheduler.generateUniqueID();
          OptimizingJob optimizingJob =
               new OptimizingJob(slamdServer, optimizingJobID,
                                 optimizationAlgorithm, jobClass, folderName,
                                 description, includeThreadCount,
                                 startTime, duration, delayBetweenJobs,
                                 numClients, requestedClients, monitorClients,
                                 monitorClientsIfAvailable, minThreads,
                                 maxThreads, threadIncrement,
                                 collectionInterval, maxNonImproving,
                                 notifyAddresses, reRunBestIteration,
                                 reRunDuration, parameters,
                                 displayInReadOnlyMode);
          optimizingJob.setDependencies(dependencies);
          optimizingJob.setThreadStartupDelay(threadStartupDelay);
          optimizingJob.setComments(comments);

          try
          {
            optimizationAlgorithm.initializeOptimizationAlgorithm(optimizingJob,
                                       optimizationParameters);
          }
          catch (InvalidValueException ive)
          {
            infoMessage.append("ERROR:  Invalid value for one or more " +
                               "optimization settings:  " + ive.getMessage() +
                               "<BR>" + EOL);
            if (job == null)
            {
              generateOptimizeJobForm(requestInfo, jobClass,
                                      optimizationAlgorithm);
            }
            else
            {
              generateOptimizeJobForm(requestInfo, job, optimizationAlgorithm);
            }
            return;
          }

          try
          {
            scheduler.scheduleOptimizingJob(optimizingJob, folderName);

            // Update the HTTP header of the response to include the job ID.
            HttpServletResponse response = requestInfo.response;
            response.addHeader(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                               optimizingJobID);

            generateViewOptimizingJobBody(requestInfo, optimizingJobID);
          }
          catch (SLAMDServerException sse)
          {
            infoMessage.append("ERROR:  Unable to schedule the optimizing " +
                               "job -- " + sse.getMessage() + "<BR>" + EOL);
            if (job == null)
            {
              generateOptimizeJobForm(requestInfo, jobClass,
                                      optimizationAlgorithm);
            }
            else
            {
              generateOptimizeJobForm(requestInfo, job, optimizationAlgorithm);
            }
          }
        }
      }
      else
      {
        generateOptimizeJobForm(requestInfo, jobClass, optimizationAlgorithm);
      }
    }
    else
    {
      if (job == null)
      {
        generateOptimizeJobForm(requestInfo, jobClass, optimizationAlgorithm);
      }
      else
      {
        generateOptimizeJobForm(requestInfo, job, optimizationAlgorithm);
      }
    }
  }



  /**
   * Generates a form that allows the user to schedule a self-optimizing job.
   *
   * @param  requestInfo            The state information for this request.
   * @param  jobClass               The job class that should be used to obtain
   *                                information for scheduling the optimizing
   *                                job.
   * @param  optimizationAlgorithm  The optimization algorithm to use for the
   *                                optimizing job.
   */
  static void generateOptimizeJobForm(RequestInfo requestInfo,
                                      JobClass jobClass,
                                      OptimizationAlgorithm
                                           optimizationAlgorithm)
  {
    logMessage(requestInfo,
               "In generateOptimizeJobForm(" + jobClass.getJobName() + ')');


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    int jobNumClients = jobClass.overrideNumClients();
    int jobInterval   = jobClass.overrideCollectionInterval();


    // See if the job class is deprecated.  If so, then add a warning message at
    // the top of the page.
    StringBuilder deprecatedMessage = new StringBuilder();
    if (jobClass.isDeprecated(deprecatedMessage))
    {
      infoMessage.append("WARNING:  This job class has been deprecated.");
      if (deprecatedMessage.length() > 0)
      {
        infoMessage.append("  ");
        infoMessage.append(deprecatedMessage);
      }

      infoMessage.append("<BR><BR>" + EOL);
    }


    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Optimize Results for a \"" + jobClass.getJobName() +
                    "\" Job</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("Enter the following information about the way in which " +
                    "the optimization should proceed." + EOL);
    htmlBody.append("Note that parameters marked with an asterisk (" +
                    star + ") are required to have a value." + EOL);

    String link = generateNewWindowLink(requestInfo,
                       Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_OPTIMIZE_HELP,
                       Constants.SERVLET_PARAM_JOB_CLASS,
                       jobClass.getClass().getName(),
                       Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                       optimizationAlgorithm.getClass().getName(),
                       "Click here for help regarding these parameters");
    htmlBody.append(link + '.' + EOL);

    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_OPTIMIZE) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                          jobClass.getClass().getName()) + EOL);
    htmlBody.append("  " +
                    generateHidden(
                         Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                         optimizationAlgorithm.getClass().getName()) + EOL);

    if (jobNumClients > 0)
    {
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                                  String.valueOf(jobNumClients)) + EOL);
    }

    if (jobInterval > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                           secondsToHumanReadableDuration(jobInterval)) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);


    // The description for the job.
    String description =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);
    if ((description == null) || (description.length() == 0))
    {
      description = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    description + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to include the number of threads in the description.
    boolean includeThreadInDescription = false;
    String includeThreadStr =
         request.getParameter(
              Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION);
    if (includeThreadStr != null)
    {
      includeThreadInDescription = (includeThreadStr.equalsIgnoreCase("true") ||
                                    includeThreadStr.equalsIgnoreCase("yes") ||
                                    includeThreadStr.equalsIgnoreCase("on") ||
                                    includeThreadStr.equalsIgnoreCase("1"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Include Thread Count in Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION +
                    '"' + (includeThreadInDescription ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to display the job in restricted read-only mode.
    boolean displayInReadOnlyMode = false;
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if (displayStr != null)
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Display In Read-Only Mode</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY +
                    '"' + (displayInReadOnlyMode ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The name of the folder to use for the job.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if ((folders != null) && (folders.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Place in Folder</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        if ((folderName != null) &&
            folderName.equals(folders[i].getFolderName()))
        {
          htmlBody.append("          <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\" SELECTED>" +
                          folders[i].getFolderName() + EOL);
        }
        else
        {
          htmlBody.append("          <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\">" +
                          folders[i].getFolderName() + EOL);
        }
      }
      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The start time for the job.
    String startTimeStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
    if ((startTimeStr == null) || (startTimeStr.length() == 0))
    {
      if (populateStartTime)
      {
        startTimeStr = dateFormat.format(new Date());
      }
      else
      {
        startTimeStr = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Start Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_START_TIME + "\" VALUE=\"" +
                    startTimeStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The duration for the job.
    String durationStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
    if (durationStr == null)
    {
      durationStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    durationStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The delay that should be included between the individual copies of the
    // job.
    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
    if ((delayStr == null) || (delayStr.length() == 0))
    {
      delayStr = "0";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Delay Between Iterations " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The number of clients to use.
    if (jobNumClients <= 0)
    {
      String clientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if (clientsStr == null)
      {
        clientsStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      clientsStr + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The specific set of clients to use.
    String clientsStr =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
    if (clientsStr == null)
    {
      clientsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Use Specific Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_CLIENTS + "\" ROWS=\"5\" " +
                    "COLS=\"80\">" + clientsStr + "</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The set of resource monitor clients to use.
    clientsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
    if (clientsStr == null)
    {
      clientsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Resource Monitor Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS +
                    "\" ROWS=\"5\" COLS=\"80\">" + clientsStr +
                    "</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to automatically monitor client systems if available.
    boolean monitorClientsIfAvailable = false;
    String monitorStr = request.getParameter(
         Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
    if (monitorStr != null)
    {
      monitorClientsIfAvailable = (monitorStr.equalsIgnoreCase("true") ||
                                   monitorStr.equalsIgnoreCase("yes") ||
                                   monitorStr.equalsIgnoreCase("on") ||
                                   monitorStr.equalsIgnoreCase("1"));
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE +
                    '"' + (monitorClientsIfAvailable ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The minimum number of threads to use.
    String minThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
    if ((minThreadsStr == null) || (minThreadsStr.length() == 0))
    {
      minThreadsStr = "1";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Minimum Number of Threads " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MIN + "\" VALUE=\"" +
                    minThreadsStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The maximum number of threads to use.
    String maxThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX);
    if (maxThreadsStr == null)
    {
      maxThreadsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MAX + "\" VALUE=\"" +
                    maxThreadsStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread increment to use.
    String incrementStr =
         request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
    if ((incrementStr == null) || (incrementStr.length() == 0))
    {
      incrementStr = "1";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Increment Between Iterations " + star +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_THREAD_INCREMENT + "\" VALUE=\"" +
                    incrementStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread startup delay to use.
    String startupDelayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if ((startupDelayStr == null) || (startupDelayStr.length() == 0))
    {
      startupDelayStr = "0";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + startupDelayStr + "\" SIZE=\"80\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);


    // The statistics collection interval.
    if (jobInterval <= 0)
    {
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if (intervalStr == null)
      {
        intervalStr = secondsToHumanReadableDuration(
             Constants.DEFAULT_COLLECTION_INTERVAL);
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistics Collection Interval</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"80\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The number of consecutive non-improving results before ending testing.
    String maxNonImprovingStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
    if ((maxNonImprovingStr == null) || (maxNonImprovingStr.length() == 0))
    {
      maxNonImprovingStr = "1";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Max. Consecutive Non-Improving Iterations " +
                    star + "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING +
                    "\" VALUE=\"" + maxNonImprovingStr +
                    "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to re-run the best iteration of the optimizing job once it is
    // found.
    boolean reRunBestIteration = false;
    String reRunBestStr =
         request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
    if (reRunBestStr != null)
    {
      reRunBestStr = reRunBestStr.toLowerCase();
      reRunBestIteration = (reRunBestStr.equals("true") ||
                            reRunBestStr.equals("yes") ||
                            reRunBestStr.equals("on") ||
                            reRunBestStr.equals("1"));
    }
    String checkedStr = (reRunBestIteration ? " CHECKED" : "");
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_BEST_ITERATION + '"' +
                    checkedStr + "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The duration to use when re-running the best iteration.
    String reRunDurationStr =
         request.getParameter(Constants.SERVLET_PARAM_RERUN_DURATION);
    if (reRunDurationStr == null)
    {
      reRunDurationStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_DURATION + "\" VALUE=\"" +
                    reRunDurationStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The dependencies for this job.
    String dependencyID =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);

    htmlBody.append("      <SELECT NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);
    htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);

    Job[] pendingJobs = scheduler.getPendingJobs();
    Job[] runningJobs = scheduler.getRunningJobs();
    OptimizingJob[] optimizingJobs;
    try
    {
      optimizingJobs = scheduler.getUncompletedOptimizingJobs();
      if (optimizingJobs == null)
      {
        optimizingJobs = new OptimizingJob[0];
      }
    }
    catch (SLAMDServerException sse)
    {
      infoMessage.append("ERROR:  Unable to get the list of uncompleted " +
                         "optimizing jobs to use in the dependency list -- " +
                         sse + "<BR>" + EOL);
      optimizingJobs = new OptimizingJob[0];
    }

    for (int j=0; j < pendingJobs.length; j++)
    {
      description = pendingJobs[j].getJobDescription();
      if (description == null)
      {
        description = "";
      }
      else if (description.length() > 0)
      {
        description = " -- " + description;
      }
      if ((dependencyID != null) &&
          dependencyID.equals(pendingJobs[j].getJobID()))
      {
        htmlBody.append("        <OPTION VALUE=\"" +
                        pendingJobs[j].getJobID() + "\" SELECTED>" +
                        pendingJobs[j].getJobID() + " -- Pending " +
                        pendingJobs[j].getJobName() + description + EOL);
      }
      else
      {
        htmlBody.append("        <OPTION VALUE=\"" +
                        pendingJobs[j].getJobID() + "\">" +
                        pendingJobs[j].getJobID() + " -- Pending " +
                        pendingJobs[j].getJobName() + description + EOL);
      }
    }

    for (int j=0; j < runningJobs.length; j++)
    {
      description = runningJobs[j].getJobDescription();
      if (description == null)
      {
        description = "";
      }
      else if (description.length() > 0)
      {
        description = " -- " + description;
      }
      if ((dependencyID != null) &&
          dependencyID.equals(runningJobs[j].getJobID()))
      {
        htmlBody.append("        <OPTION VALUE=\"" +
                        runningJobs[j].getJobID() + "\" SELECTED>" +
                        runningJobs[j].getJobID() + " -- Running " +
                        runningJobs[j].getJobName() + description + EOL);
      }
      else
      {
        htmlBody.append("        <OPTION VALUE=\"" +
                        runningJobs[j].getJobID() + "\">" +
                        runningJobs[j].getJobID() + " -- Running " +
                        runningJobs[j].getJobName() + description + EOL);
      }
    }

    for (int j=0; j < optimizingJobs.length; j++)
    {
      description = optimizingJobs[j].getDescription();
      if (description == null)
      {
        description = "";
      }
      else
      {
        description = " -- " + description;
      }

      if ((dependencyID != null) &&
          dependencyID.equals(optimizingJobs[j].getOptimizingJobID()))
      {
        htmlBody.append("        <OPTION VALUE=\"" +
                        optimizingJobs[j].getOptimizingJobID() +
                        "\" SELECTED>" +
                        optimizingJobs[j].getOptimizingJobID() +
                        " -- Optimizing " +
                        optimizingJobs[j].getJobClass().getJobName() +
                        description + EOL);
      }
      else
      {
        htmlBody.append("        <OPTION VALUE=\"" +
                        optimizingJobs[j].getOptimizingJobID() + "\">" +
                        optimizingJobs[j].getOptimizingJobID() +
                        " -- Optimizing " +
                        optimizingJobs[j].getJobClass().getJobName() +
                        description + EOL);
      }
    }


    htmlBody.append("      </SELECT>" + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The e-mail address(es) to notify when the job is complete.
    if (slamdServer.getMailer().isEnabled())
    {
      String mailStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS);
      if (mailStr == null)
      {
        mailStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Notify on Completion</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS +
                      "\" VALUE=\"" + mailStr + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The list of optimization-algorithm specific parameters.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    boolean confirmed = ((confirmStr != null) && (confirmStr.length() > 0));
    Parameter[] params = optimizationAlgorithm.
          getOptimizationAlgorithmParameterStubs(jobClass).clone().
               getParameters();
    for (int i=0; i < params.length; i++)
    {
      Parameter stub = params[i];
      if (confirmed)
      {
        String[] values =
             request.getParameterValues(
                  Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                  stub.getName());
        try
        {
          stub.htmlInputFormToValue(values);
        } catch (InvalidValueException ive) {}
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + stub.getDisplayName() +
                      (stub.isRequired() ? star : "") + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        " +
                      stub.getHTMLInputForm(
                           Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX) +
                      EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The list of job-specific parameters.
    params = jobClass.getParameterStubs().clone().getParameters();
    for (int i=0; i < params.length; i++)
    {
      Parameter stub = params[i];
      if (confirmed)
      {
        String[] values = request.getParameterValues(
                               Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                              stub.getName());
        try
        {
          stub.htmlInputFormToValue(values);
        } catch (InvalidValueException ive) {}
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + stub.getDisplayName() +
                      (stub.isRequired() ? star : "") + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        " +
                      stub.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The comments field.
    String comments =
         request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);
    if (comments == null)
    {
      comments = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Comments</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_COMMENTS + "\" ROWS=\"5\" " +
                    "COLS=\"80\">" + comments + "</TEXTAREA></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The submit button.
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" + EOL);
    if (jobClass.providesParameterTest())
    {
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"" + Constants.SUBMIT_STRING_TEST_PARAMS +
                      "\">" + EOL);
      htmlBody.append("        &nbsp; &nbsp;" + EOL);
    }
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_CONFIRMED +
                    "\" VALUE=\"Schedule\">" + EOL);
    htmlBody.append("        &nbsp; &nbsp;" + EOL);
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_CONFIRMED +
                    "\" VALUE=\"" + Constants.SUBMIT_STRING_CANCEL + "\">" +
                    EOL);
    htmlBody.append("      </TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Generates a form that allows the user to schedule a self-optimizing job.
   *
   * @param  requestInfo            The state information for this request.
   * @param  job                    The job that will be used as the basis for
   *                                the self-optimizing job.
   * @param  optimizationAlgorithm  The optimization algorithm to use for the
   *                                optimizing job.
   */
  static void generateOptimizeJobForm(RequestInfo requestInfo, Job job,
                                      OptimizationAlgorithm
                                           optimizationAlgorithm)
  {
    logMessage(requestInfo, "In generateOptimizeJobForm(" + job.getJobID() +
               ')');


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    int jobNumClients = job.getJobClass().overrideNumClients();
    int jobInterval   = job.getJobClass().overrideCollectionInterval();


    // See if the job class is deprecated.  If so, then add a warning message at
    // the top of the page.
    StringBuilder deprecatedMessage = new StringBuilder();
    if (job.getJobClass().isDeprecated(deprecatedMessage))
    {
      infoMessage.append("WARNING:  This job class has been deprecated.");
      if (deprecatedMessage.length() > 0)
      {
        infoMessage.append("  ");
        infoMessage.append(deprecatedMessage);
      }

      infoMessage.append("<BR><BR>" + EOL);
    }


    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Optimize Results for Job " + job.getJobID() +
                    "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("Enter the following information about the way in which " +
                    "the optimization should proceed." + EOL);
    htmlBody.append("Note that parameters marked with an asterisk (" +
                    star + ") are required to have a value." + EOL);

    String link = generateNewWindowLink(requestInfo,
                       Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_OPTIMIZE_HELP,
                       Constants.SERVLET_PARAM_JOB_CLASS,
                       job.getJobClassName(),
                       Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                       optimizationAlgorithm.getClass().getName(),
                       "Click here for help regarding these parameters");
    htmlBody.append(link + '.' + EOL);

    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_OPTIMIZE) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                          job.getJobClassName()) + EOL);
    htmlBody.append("  " +
                    generateHidden(
                         Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                         optimizationAlgorithm.getClass().getName()) + EOL);

    if (jobNumClients > 0)
    {
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                                  String.valueOf(jobNumClients)) + EOL);
    }

    if (jobInterval > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                           String.valueOf(jobInterval)) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);


    // The description for the job.
    String description =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);
    if ((description == null) || (description.length() == 0))
    {
      description = job.getJobDescription();
      if (description == null)
      {
        description = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    description + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to include the number of threads in the description.
    boolean includeThreadInDescription = false;
    String includeThreadStr =
         request.getParameter(
              Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION);
    if (includeThreadStr != null)
    {
      includeThreadInDescription = (includeThreadStr.equalsIgnoreCase("true") ||
                                    includeThreadStr.equalsIgnoreCase("yes") ||
                                    includeThreadStr.equalsIgnoreCase("on") ||
                                    includeThreadStr.equalsIgnoreCase("one"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Include Thread Count in Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION +
                    '"' + (includeThreadInDescription ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to display the job in restricted read-only mode.
    boolean displayInReadOnlyMode = false;
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if (displayStr != null)
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Display In Read-Only Mode</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY +
                    '"' + (displayInReadOnlyMode ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The name of the folder to use for the job.
    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if ((folders != null) && (folders.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Place in Folder</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        if ((job.getFolderName() != null) &&
            job.getFolderName().equalsIgnoreCase(folders[i].getFolderName()))
        {
          htmlBody.append("          <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\" SELECTED>" +
                          folders[i].getFolderName() + EOL);
        }
        else
        {
          htmlBody.append("          <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\">" +
                          folders[i].getFolderName() + EOL);
        }
      }
      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The start time for the job.
    String startTimeStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
    if ((startTimeStr == null) || (startTimeStr.length() == 0))
    {
      if (populateStartTime)
      {
        startTimeStr = dateFormat.format(new Date());
      }
      else
      {
        startTimeStr = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Start Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_START_TIME + "\" VALUE=\"" +
                    startTimeStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The duration for the job.
    String durationStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
    if ((durationStr == null) || (durationStr.length() == 0))
    {
      int duration = job.getDuration();
      if (duration > 0)
      {
        durationStr = secondsToHumanReadableDuration(duration);
      }
      else
      {
        durationStr = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    durationStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The delay that should be included between the individual copies of the
    // job.
    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
    if ((delayStr == null) || (delayStr.length() == 0))
    {
      delayStr = "0";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Delay Between Iterations " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The number of clients to use.
    if (jobNumClients <= 0)
    {
      String clientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((clientsStr == null) || (clientsStr.length() == 0))
      {
        clientsStr = String.valueOf(job.getNumberOfClients());
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      clientsStr + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The specific set of clients to use.
    String clientsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
    if (clientsStr == null)
    {
      clientsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Use Specific Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_CLIENTS + "\" ROWS=\"5\" " +
                    "COLS=\"80\">" + clientsStr + "</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The set of resource monitor clients to use.
    clientsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
    if (clientsStr == null)
    {
      clientsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Resource Monitor Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS +
                    "\" ROWS=\"5\" COLS=\"80\">" + clientsStr +
                    "</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to automatically monitor client systems if available.
    boolean monitorClientsIfAvailable = job.monitorClientsIfAvailable();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE +
                    '"' + (monitorClientsIfAvailable ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The minimum number of threads to use.
    String minThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
    if ((minThreadsStr == null) || (minThreadsStr.length() == 0))
    {
      minThreadsStr = "1";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Minimum Number of Threads " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MIN + "\" VALUE=\"" +
                    minThreadsStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The maximum number of threads to use.
    String maxThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX);
    if ((maxThreadsStr == null) || (maxThreadsStr.length() == 0))
    {
      maxThreadsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MAX + "\" VALUE=\"" +
                    maxThreadsStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread increment to use.
    String incrementStr =
         request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
    if ((incrementStr == null) || (incrementStr.length() == 0))
    {
      incrementStr = "1";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Increment Between Iterations " + star +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_THREAD_INCREMENT + "\" VALUE=\"" +
                    incrementStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread startup delay to use.
    String startupDelayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if ((startupDelayStr == null) || (startupDelayStr.length() == 0))
    {
      startupDelayStr = "0";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + startupDelayStr + "\" SIZE=\"80\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);


    // The statistics collection interval.
    if (jobInterval <= 0)
    {
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr == null) || (intervalStr.length() == 0))
      {
        intervalStr =
             secondsToHumanReadableDuration(job.getCollectionInterval());
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistics Collection Interval</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"80\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The number of consecutive non-improving results before ending testing.
    String maxNonImprovingStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
    if ((maxNonImprovingStr == null) || (maxNonImprovingStr.length() == 0))
    {
      maxNonImprovingStr = "1";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Max. Consecutive Non-Improving Iterations " +
                    star + "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING +
                    "\" VALUE=\"" + maxNonImprovingStr +
                    "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to re-run the best iteration of the optimizing job once it is
    // found.
    boolean reRunBestIteration = false;
    String reRunBestStr =
         request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
    if (reRunBestStr != null)
    {
      reRunBestStr = reRunBestStr.toLowerCase();
      reRunBestIteration = (reRunBestStr.equals("true") ||
                            reRunBestStr.equals("yes") ||
                            reRunBestStr.equals("on") ||
                            reRunBestStr.equals("1"));
    }
    String checkedStr = (reRunBestIteration ? " CHECKED" : "");
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_BEST_ITERATION + '"' +
                    checkedStr + "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The duration to use when re-running the best iteration.
    String reRunDurationStr =
         request.getParameter(Constants.SERVLET_PARAM_RERUN_DURATION);
    if (reRunDurationStr == null)
    {
      reRunDurationStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_DURATION + "\" VALUE=\"" +
                    reRunDurationStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The dependencies for this job.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);

    htmlBody.append("      <SELECT NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);
    htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);

    Job[] pendingJobs = scheduler.getPendingJobs();
    Job[] runningJobs = scheduler.getRunningJobs();
    OptimizingJob[] optimizingJobs;
    try
    {
      optimizingJobs = scheduler.getUncompletedOptimizingJobs();
      if (optimizingJobs == null)
      {
        optimizingJobs = new OptimizingJob[0];
      }
    }
    catch (SLAMDServerException sse)
    {
      infoMessage.append("ERROR:  Unable to get the list of uncompleted " +
                         "optimizing jobs to use in the dependency list -- " +
                         sse + "<BR>" + EOL);
      optimizingJobs = new OptimizingJob[0];
    }

    for (int j=0; j < pendingJobs.length; j++)
    {
      description = pendingJobs[j].getJobDescription();
      if (description == null)
      {
        description = "";
      }
      else if (description.length() > 0)
      {
        description = " -- " + description;
      }
      htmlBody.append("        <OPTION VALUE=\"" +
                      pendingJobs[j].getJobID() + "\">" +
                      pendingJobs[j].getJobID() + " -- Pending " +
                      pendingJobs[j].getJobName() + description + EOL);
    }

    for (int j=0; j < runningJobs.length; j++)
    {
      description = runningJobs[j].getJobDescription();
      if (description == null)
      {
        description = "";
      }
      else if (description.length() > 0)
      {
        description = " -- " + description;
      }
      htmlBody.append("        <OPTION VALUE=\"" +
                      runningJobs[j].getJobID() + "\">" +
                      runningJobs[j].getJobID() + " -- Running " +
                      runningJobs[j].getJobName() + description + EOL);
    }

    for (int j=0; j < optimizingJobs.length; j++)
    {
      description = optimizingJobs[j].getDescription();
      if (description == null)
      {
        description = "";
      }
      else
      {
        description = " -- " + description;
      }
      htmlBody.append("        <OPTION VALUE=\"" +
                      optimizingJobs[j].getOptimizingJobID() + "\">" +
                      optimizingJobs[j].getOptimizingJobID() +
                      " -- Optimizing " +
                      optimizingJobs[j].getJobClass().getJobName() +
                      description + EOL);
    }


    htmlBody.append("      </SELECT>" + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The e-mail address(es) to notify when the job is complete.
    if (slamdServer.getMailer().isEnabled())
    {
      String mailStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS);
      if (mailStr == null)
      {
        mailStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Notify on Completion</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS +
                      "\" VALUE=\"" + mailStr + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The list of optimization-algorithm specific parameters.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    boolean confirmed = ((confirmStr != null) && (confirmStr.length() > 0));
    Parameter[] params = optimizationAlgorithm.
          getOptimizationAlgorithmParameterStubs(job.getJobClass()).clone().
               getParameters();
    for (int i=0; i < params.length; i++)
    {
      Parameter stub = params[i];
      if (confirmed)
      {
        String[] values =
             request.getParameterValues(
                  Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                  stub.getName());
        try
        {
          stub.htmlInputFormToValue(values);
        } catch (InvalidValueException ive) {}
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + stub.getDisplayName() +
                      (stub.isRequired() ? star : "") + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        " +
                      stub.getHTMLInputForm(
                           Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX) +
                      EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The list of job-specific parameters.
    params = job.getParameterStubs().clone().getParameters();
    for (int i=0; i < params.length; i++)
    {
      Parameter stub = params[i];
      if (confirmed)
      {
        String[] values = request.getParameterValues(
                               Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                              stub.getName());
        try
        {
          stub.htmlInputFormToValue(values);
        }
        catch (InvalidValueException ive)
        {
          Parameter p = job.getParameterList().getParameter(stub.getName());
          if (p != null)
          {
            stub.setValueFrom(p);
          }
        }
      }
      else
      {
        Parameter p = job.getParameterList().getParameter(stub.getName());
        if (p != null)
        {
          stub.setValueFrom(p);
        }
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + stub.getDisplayName() +
                      (stub.isRequired() ? star : "") + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        " +
                      stub.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The comments field.
    String comments =
         request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);
    if (comments == null)
    {
      comments = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Comments</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_COMMENTS + "\" ROWS=\"5\" " +
                    "COLS=\"80\">" + comments + "</TEXTAREA></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The submit button.
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" + EOL);
    if (job.getJobClass().providesParameterTest())
    {
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"" + Constants.SUBMIT_STRING_TEST_PARAMS +
                      "\">" + EOL);
      htmlBody.append("        &nbsp; &nbsp;" + EOL);
    }
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_CONFIRMED +
                    "\" VALUE=\"Schedule\">" + EOL);
    htmlBody.append("        &nbsp; &nbsp;" + EOL);
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_CONFIRMED +
                    "\" VALUE=\"" + Constants.SUBMIT_STRING_CANCEL + "\">" +
                    EOL);
    htmlBody.append("      </TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Generates a form that allows the user to schedule a self-optimizing job
   * based on a previous optimizing job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void generateCloneOptimizingJobForm(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In generateCloneOptimizeJobForm()");


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID for the optimizing job.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if (optimizingJobID == null)
    {
      infoMessage.append("ERROR:  Unable to determine the job ID for the " +
                         "optimizing job to clone<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }


    // Get the optimizing job with the provided ID.
    OptimizingJob optimizingJob;
    try
    {
      optimizingJob = getOptimizingJob(optimizingJobID);
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  Unable to retrieve optimizing job " +
                         optimizingJobID + " from the configuration " +
                         "directory -- " + e + "<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }
    if (optimizingJob == null)
    {
      infoMessage.append("ERROR:  Unable to retrieve optimizing job " +
                         optimizingJobID + " from the configuration " +
                         "directory<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }

    int jobNumClients = optimizingJob.getJobClass().overrideNumClients();
    int jobInterval = optimizingJob.getJobClass().overrideCollectionInterval();


    // See if the job class is deprecated.  If so, then add a warning message at
    // the top of the page.
    StringBuilder deprecatedMessage = new StringBuilder();
    if (optimizingJob.getJobClass().isDeprecated(deprecatedMessage))
    {
      infoMessage.append("WARNING:  This job class has been deprecated.");
      if (deprecatedMessage.length() > 0)
      {
        infoMessage.append("  ");
        infoMessage.append(deprecatedMessage);
      }

      infoMessage.append("<BR><BR>" + EOL);
    }


    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Clone Optimizing Job " + optimizingJobID +
                    "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("Enter the following information about the way in which " +
                    "the optimization should proceed." + EOL);
    htmlBody.append("Note that parameters marked with an asterisk (" +
                    star + ") are required to have a value." + EOL);

    String link = generateNewWindowLink(requestInfo,
                       Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_OPTIMIZE_HELP,
                       Constants.SERVLET_PARAM_JOB_CLASS,
                       optimizingJob.getJobClassName(),
                       Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                       optimizingJob.getOptimizationAlgorithm().getClass().
                            getName(),
                       "Click here for help regarding these parameters");
    htmlBody.append(link + '.' + EOL);

    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_OPTIMIZE) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                          optimizingJob.getJobClassName()) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(
                         Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                         optimizingJob.getOptimizationAlgorithm().getClass().
                              getName()) + EOL);

    if (jobNumClients > 0)
    {
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                                  String.valueOf(jobNumClients)) + EOL);
    }

    if (jobInterval > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                           secondsToHumanReadableDuration(jobInterval)) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);


    // The description for the job.
    String description =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);
    if ((description == null) || (description.length() == 0))
    {
      description = optimizingJob.getDescription();
      if (description == null)
      {
        description = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    description + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to include the number of threads in the description.
    boolean includeThreadInDescription =
         optimizingJob.includeThreadsInDescription();
    String includeThreadStr =
         request.getParameter(
              Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION);
    if (includeThreadStr != null)
    {
      includeThreadInDescription = (includeThreadStr.equalsIgnoreCase("true") ||
                                    includeThreadStr.equalsIgnoreCase("yes") ||
                                    includeThreadStr.equalsIgnoreCase("on") ||
                                    includeThreadStr.equalsIgnoreCase("one"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Include Thread Count in Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION +
                    '"' + (includeThreadInDescription ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to display the job in restricted read-only mode.
    boolean displayInReadOnlyMode = false;
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if (displayStr != null)
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Display In Read-Only Mode</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY +
                    '"' + (displayInReadOnlyMode ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The name of the folder to use for the job.
    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if ((folders != null) && (folders.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Place in Folder</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        if ((optimizingJob.getFolderName() != null) &&
            optimizingJob.getFolderName().equalsIgnoreCase(
                                               folders[i].getFolderName()))
        {
          htmlBody.append("          <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\" SELECTED>" +
                          folders[i].getFolderName() + EOL);
        }
        else
        {
          htmlBody.append("          <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\">" +
                          folders[i].getFolderName() + EOL);
        }
      }
      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The start time for the job.  This will not be set from the provided job.
    String startTimeStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
    if ((startTimeStr == null) || (startTimeStr.length() == 0))
    {
      if (populateStartTime)
      {
        startTimeStr = dateFormat.format(new Date());
      }
      else
      {
        startTimeStr = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Start Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_START_TIME + "\" VALUE=\"" +
                    startTimeStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The duration for the job.
    int duration = optimizingJob.getDuration();
    String durationStr;
    if (duration > 0)
    {
      durationStr = secondsToHumanReadableDuration(duration);
    }
    else
    {
      durationStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    durationStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The delay that should be included between the individual copies of the
    // job.
    String delayStr = String.valueOf(optimizingJob.getDelayBetweenIterations());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Delay Between Iterations " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The number of clients to use.
    if (jobNumClients <= 0)
    {
      String clientsStr = String.valueOf(optimizingJob.getNumClients());
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      clientsStr + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The specific set of clients to use.
    String clientsStr;
    String[] clients = optimizingJob.getRequestedClients();
    if ((clients == null) || (clients.length == 0))
    {
      clientsStr = "";
    }
    else
    {
      clientsStr = clients[0];
      for (int i=1; i < clients.length; i++)
      {
        clientsStr += '\n' + clients[i];
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Use Specific Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_CLIENTS + "\" ROWS=\"5\" " +
                    "COLS=\"80\">" + clientsStr + "</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The set of resource monitor clients to use.
    String[] monitorClients = optimizingJob.getResourceMonitorClients();
    if ((monitorClients == null) || (monitorClients.length == 0))
    {
      clientsStr = "";
    }
    else
    {
      clientsStr = monitorClients[0];
      for (int i=1; i < monitorClients.length; i++)
      {
        clientsStr += '\n' + monitorClients[i];
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Resource Monitor Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS +
                    "\" ROWS=\"5\" COLS=\"80\">" + clientsStr +
                    "</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to automatically monitor client systems if available.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE +
                    '"' +  (optimizingJob.monitorClientsIfAvailable()
                             ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The minimum number of threads to use.
    String minThreadsStr = String.valueOf(optimizingJob.getMinThreads());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Minimum Number of Threads " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MIN + "\" VALUE=\"" +
                    minThreadsStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The maximum number of threads to use.
    int maxThreads = optimizingJob.getMaxThreads();
    String maxThreadsStr;
    if (maxThreads > 0)
    {
      maxThreadsStr = String.valueOf(maxThreads);
    }
    else
    {
      maxThreadsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MAX + "\" VALUE=\"" +
                    maxThreadsStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread increment to use.
    String incrementStr = String.valueOf(optimizingJob.getThreadIncrement());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Increment Between Iterations " + star +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_THREAD_INCREMENT + "\" VALUE=\"" +
                    incrementStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread startup delay to use.
    String startupDelayStr =
                String.valueOf(optimizingJob.getThreadStartupDelay());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + startupDelayStr + "\" SIZE=\"80\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);


    // The statistics collection interval.
    if (jobInterval <= 0)
    {
      String intervalStr = secondsToHumanReadableDuration(
           optimizingJob.getCollectionInterval());
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistics Collection Interval</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"80\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The number of consecutive non-improving results before ending testing.
    String maxNonImprovingStr =
                String.valueOf(optimizingJob.getMaxNonImproving());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Max. Consecutive Non-Improving Iterations " +
                    star + "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING +
                    "\" VALUE=\"" + maxNonImprovingStr +
                    "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // Whether to re-run the best iteration of the optimizing job once it is
    // found.
    boolean reRunBestIteration = optimizingJob.reRunBestIteration();
    String checkedStr = (reRunBestIteration ? " CHECKED" : "");
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_BEST_ITERATION + '"' +
                    checkedStr + "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The duration to use when re-running the best iteration.
    String reRunDurationStr = "";
    if (optimizingJob.getReRunDuration() > 0)
    {
      reRunDurationStr =
           secondsToHumanReadableDuration(optimizingJob.getReRunDuration());
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_DURATION + "\" VALUE=\"" +
                    reRunDurationStr + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The dependencies for this job.  This will not be set from the provided
    // optimizing job.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);

    htmlBody.append("      <SELECT NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);
    htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);

    Job[] pendingJobs = scheduler.getPendingJobs();
    Job[] runningJobs = scheduler.getRunningJobs();
    OptimizingJob[] optimizingJobs;
    try
    {
      optimizingJobs = scheduler.getUncompletedOptimizingJobs();
      if (optimizingJobs == null)
      {
        optimizingJobs = new OptimizingJob[0];
      }
    }
    catch (SLAMDServerException sse)
    {
      infoMessage.append("ERROR:  Unable to get the list of uncompleted " +
                         "optimizing jobs to use in the dependency list -- " +
                         sse + "<BR>" + EOL);
      optimizingJobs = new OptimizingJob[0];
    }

    for (int j=0; j < pendingJobs.length; j++)
    {
      description = pendingJobs[j].getJobDescription();
      if (description == null)
      {
        description = "";
      }
      else if (description.length() > 0)
      {
        description = " -- " + description;
      }
      htmlBody.append("        <OPTION VALUE=\"" +
                      pendingJobs[j].getJobID() + "\">" +
                      pendingJobs[j].getJobID() + " -- Pending " +
                      pendingJobs[j].getJobName() + description + EOL);
    }

    for (int j=0; j < runningJobs.length; j++)
    {
      description = runningJobs[j].getJobDescription();
      if (description == null)
      {
        description = "";
      }
      else if (description.length() > 0)
      {
        description = " -- " + description;
      }
      htmlBody.append("        <OPTION VALUE=\"" +
                      runningJobs[j].getJobID() + "\">" +
                      runningJobs[j].getJobID() + " -- Running " +
                      runningJobs[j].getJobName() + description + EOL);
    }

    for (int j=0; j < optimizingJobs.length; j++)
    {
      description = optimizingJobs[j].getDescription();
      if (description == null)
      {
        description = "";
      }
      else
      {
        description = " -- " + description;
      }
      htmlBody.append("        <OPTION VALUE=\"" +
                      optimizingJobs[j].getOptimizingJobID() + "\">" +
                      optimizingJobs[j].getOptimizingJobID() +
                      " -- Optimizing " +
                      optimizingJobs[j].getJobClass().getJobName() +
                      description + EOL);
    }


    htmlBody.append("      </SELECT>" + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The e-mail address(es) to notify when the job is complete.
    if (slamdServer.getMailer().isEnabled())
    {
      String[] notifyAddresses = optimizingJob.getNotifyAddresses();
      String mailStr;
      if ((notifyAddresses == null) || (notifyAddresses.length == 0))
      {
        mailStr = "";
      }
      else
      {
        mailStr = notifyAddresses[0];
        for (int i=1; i < notifyAddresses.length; i++)
        {
          mailStr += ", " + notifyAddresses[i];
        }
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Notify on Completion</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS +
                      "\" VALUE=\"" + mailStr + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The list of optimization-algorithm specific parameters.
    Parameter[] stubs = optimizingJob.getOptimizationParameterStubs().clone().
         getParameters();
    ParameterList paramList = optimizingJob.getOptimizationParameters();
    for (int i=0; i < stubs.length; i++)
    {
      Parameter p = paramList.getParameter(stubs[i].getName());
      if (p != null)
      {
        try
        {
          stubs[i].setValueFrom(p);
        } catch (Exception e) { e.printStackTrace(); }
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + stubs[i].getDisplayName() +
                      (stubs[i].isRequired() ? star : "") + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        " +
                      stubs[i].getHTMLInputForm(
                           Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX) +
                      EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The list of job-specific parameters.
    Parameter[] params = optimizingJob.getJobClass().getParameterStubs().
         clone().getParameters();
    for (int i=0; i < params.length; i++)
    {
      Parameter stub = params[i];
      Parameter p = optimizingJob.getParameters().getParameter(stub.getName());
      if (p != null)
      {
        stub.setValueFrom(p);
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + stub.getDisplayName() +
                      (stub.isRequired() ? star : "") + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        " +
                      stub.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    // The comments field.
    String comments =
         request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);
    if (comments == null)
    {
      comments = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Comments</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_COMMENTS + "\" ROWS=\"5\" " +
                    "COLS=\"80\">" + comments + "</TEXTAREA></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // The submit button.
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" + EOL);
    if (optimizingJob.getJobClass().providesParameterTest())
    {
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"" + Constants.SUBMIT_STRING_TEST_PARAMS +
                      "\">" + EOL);
      htmlBody.append("        &nbsp; &nbsp;" + EOL);
    }
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_CONFIRMED +
                    "\" VALUE=\"Schedule\">" + EOL);
    htmlBody.append("        &nbsp; &nbsp;" + EOL);
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_CONFIRMED +
                    "\" VALUE=\"" + Constants.SUBMIT_STRING_CANCEL + "\">" +
                    EOL);
    htmlBody.append("      </TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Generates an HTML page that may be used to display information that can
   * help a user understand the kinds of information that should be provided
   * when scheduling an optimizing job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void generateOptimizeHelpPage(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In generateOptimizeHelpPage()");

    // If the user doesn't have schedule permission, then they can't see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "schedule jobs for execution.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request  = requestInfo.request;
    StringBuilder       htmlBody = requestInfo.htmlBody;


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Help for Scheduling an Optimizing Job</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The following parameters may be specified when " +
                    "scheduling an optimizing job:" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);

    // The job description
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Description</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      A string that can contain any kind of " +
                    "information about this job." + EOL);
    htmlBody.append("      It is only used for display in the " +
                    "administrative interface to help users more quickly " +
                    "determine the purpose of a particular job." + EOL);
    htmlBody.append("      This parameter is optional." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Whether to include thread count in description
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Include Thread Count in Description</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      Indicates whether the description for each " +
                    "individual job in this set should include the number " +
                    "of threads used for that job." + EOL);
    htmlBody.append("      This is a convenience feature for use when " +
                    "comparing or graphing the jobs run as part of an " +
                    "optimizing job." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The start time
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Start Time</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The time at which the first iteration of this " +
                    "optimizing job should start running." + EOL);
    htmlBody.append("      It should be specified using the 14-digit form " +
                    "\"YYYYMMDDhhmmss\"." + EOL);
    htmlBody.append("      If it is not provided, then the current time will " +
                    "be used and the job will be considered immediately " +
                    "eligible for execution." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The duration
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Duration</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The maximum length of time in seconds that each " +
                    "iteration should be allowed to run before it is stopped." +
                    EOL);
    htmlBody.append("      If you leave the value unspecified, then the " +
                    "job will continue running until it has completed, or " +
                    "until it is stopped by an administrator." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The delay between iterations.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Delay Between Iterations</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The length of time in seconds that should elapse " +
                    "between the end of one job iteration and the beginning " +
                    "of the next." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of clients
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Number of Clients</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The number of clients that should be used to run " +
                    "this job." + EOL);
    htmlBody.append("      Note that the specified number of clients must be " +
                    "available when the start time is reached or the job " +
                    "will not be able to run." + EOL);
    htmlBody.append("      This is a required parameter." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The requested clients
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Use Specific Clients</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The addresses of specific client systems that " +
                    "should be used to run the job." + EOL);
    htmlBody.append("      The addresses may be either DNS-resolvable host " +
                    "names or IP addresses." + EOL);
    htmlBody.append("      A separate address should be used per line." + EOL);
    htmlBody.append("      If no set of clients is specified, then any " +
                    "available clients will be used, up to the specified " +
                    "number of clients." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The resource monitor clients
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Resource Monitor Clients</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The addresses of the resource monitor client " +
                    "systems that should be used for the job." + EOL);
    htmlBody.append("      The addresses may be either DNS-resolvable host " +
                    "names or IP addresses." + EOL);
    htmlBody.append("      A separate address should be used per line." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Whether to automatically monitor client systems
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>");
    htmlBody.append("      Indicates whether the job should automatically " +
                    "attempt to use any resource monitor clients that are " +
                    "running on client system(s) when the job is running." +
                    EOL);
    htmlBody.append("      If so, then any clients used that also have " +
                    "resource monitors running will report resource monitor " +
                    "statistics for the job." + EOL);
    htmlBody.append("      If no resource monitor client is available on one " +
                    "or more client systems, then the job will still be " +
                    "processed by those clients but no monitor information " +
                    "will be collected for those systems." + EOL);
    htmlBody.append("    </TD>");
    htmlBody.append("  </TR>" + EOL);

    // The minimum number of threads
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Minimum Number of Threads</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The minimum number of threads that should be used " +
                    "by each client when trying to determine the optimal " +
                    "performance for the specified statistic." + EOL);
    htmlBody.append("      The first iteration of this optimizing job will " +
                    "use this number of threads, and then each iteration " +
                    "thereafter will use an increasingly greater number of " +
                    "threads per client." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The maximum number of threads
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The maximum number of threads per client that " +
                    "should be used when trying to determine the optimal " +
                    "performance for the specified statistic." + EOL);
    htmlBody.append("      Note that if this is too low, then it may cause " +
                    "the optimization process to stop before it has " +
                    "identified the optimum value for the specified " +
                    "statistic." + EOL);
    htmlBody.append("      Also note that the optimization process may stop " +
                    "before this number of threads has been reached if " +
                    "the maximum number of consecutive non-improving " +
                    "iterations has been reached." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The thread increment between iterations
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Thread Increment Between Iterations</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The increment by which the number of threads per " +
                    "client should be raised between iterations of this " +
                    "optimizing job." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The collection interval
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Statistics Collection Interval</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The length of time in seconds for a statistics " +
                    "collection interval." + EOL);
    htmlBody.append("      In addition to the summary statistics gathered " +
                    "during job processing, the time that a job is running " +
                    "will be broken up into intervals of this length and " +
                    "statistics will be made available for each of those " +
                    "intervals." + EOL);
    htmlBody.append("      If no value is specified, a default of " +
                    Constants.DEFAULT_COLLECTION_INTERVAL +
                    " seconds will be used." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The maximum number of non-improving iterations
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Max. Consecutive Non-Improving " +
                    "Iterations</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      Specifies the maximum number of consecutive " +
                    "non-improving job iterations that will be allowed " +
                    "before the optimization process completes." + EOL);
    htmlBody.append("      A non-improving job iteration is any job " +
                    "iteration in which the value for the specified statistic" +
                    "is not closer to the optimum value than any of the " +
                    "previous iterations." + EOL);
    htmlBody.append("      For an optimization type of \"maximize\", this " +
                    "would be any job that does not yield a higher value " +
                    "for the tracked statistic than all previous iterations." +
                    EOL);
    htmlBody.append("      For an optimization type of \"minimize\", this " +
                    "would be any job that does not yield a lower value " +
                    "for the tracked statistic than all previous iterations." +
                    EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job dependencies
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The set of jobs that must complete running before " +
                    "this job will be considered eligible to run." + EOL);
    htmlBody.append("      If dependencies are specified and at least one " +
                    "on which this job is dependent has not yet completed " +
                    "running when the start time for this job arrives, then " +
                    "the execution of this job will be delayed until all " +
                    "dependencies have been fulfilled." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The e-mail address(es) to notify on completion of this job.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Notify on Completion</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The e-mail address(es) of the user(s) that should " +
                    "be sent an e-mail message when the optimization process " +
                    "has completed." + EOL);
    htmlBody.append("      This message will contain a summary of the " +
                    "job execution results and may optionally include a " +
                    "link to view more information about the job through the " +
                    "SLAMD administrative interface." + EOL);
    htmlBody.append("      If multiple addresses should be notified, then " +
                    "they should be separated with spaces and/or commas." +
                    EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // See if a job class has been provided.  We will need that for both the
    // optimization algorithm and the job-specific parameters.
    String jobClassName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    JobClass jobClass = null;
    if ((jobClassName != null) && (jobClassName.length() > 0) &&
        ((jobClass = slamdServer.getJobClass(jobClassName)) != null))
    {
      // See if an optimization algorithm has been provided. If so, then provide
      // help information for those parameters as well.
      String optimizationAlgorithmName =
           request.getParameter(Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM);
      if ((optimizationAlgorithmName != null) &&
          (optimizationAlgorithmName.length() > 0))
      {
        OptimizationAlgorithm optimizationAlgorithm =
          slamdServer.getOptimizationAlgorithm(optimizationAlgorithmName);
        if (optimizationAlgorithm != null)
        {
          Parameter[] stubs = optimizationAlgorithm.
                    getOptimizationAlgorithmParameterStubs(jobClass).clone().
                         getParameters();
          for (int i=0; i < stubs.length; i++)
          {
            htmlBody.append("  <TR>" + EOL);
            htmlBody.append("    <TD>" + stubs[i].getDisplayName() + "</TD>" +
                            EOL);
            htmlBody.append("    <TD>" + EOL);

            String description = replaceText(stubs[i].getDescription(), "\r\n",
                                             "<BR>");
            description = replaceText(description, "\n", "<BR>");
            description = replaceText(description, "\t",
                                      "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                      "&nbsp;&nbsp;");
            htmlBody.append("      " + description + EOL);
            if (stubs[i].isRequired())
            {
              htmlBody.append("      <BR>" + EOL);
              htmlBody.append("      This is a required parameter." + EOL);
            }
            htmlBody.append("    </TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
        }
      }

      Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
      for (int i=0; i < stubs.length; i++)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + stubs[i].getDisplayName() + "</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);

        String description = replaceText(stubs[i].getDescription(), "\r\n",
                                         "<BR>");
        description = replaceText(description, "\n", "<BR>");
        description = replaceText(description, "\t",
                                  "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                  "&nbsp;&nbsp;");
        htmlBody.append("      " + description + EOL);
        if (stubs[i].isRequired())
        {
          htmlBody.append("      <BR>" + EOL);
          htmlBody.append("      This is a required parameter." + EOL);
        }
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work of viewing information about one or multiple optimizing
   * jobs.
   *
   * @param  requestInfo      The state information for this request.
   * @param  showAll          Indicates whether information about all optimizing
   *                          jobs should be shown even if an optimizing job ID
   *                          has been provided.
   */
  static void handleViewOptimizing(RequestInfo requestInfo, boolean showAll)
  {
    HttpServletRequest request = requestInfo.request;

    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    handleViewOptimizing(requestInfo, showAll, folderName);
  }



  /**
   * Handles the work of viewing information about one or multiple optimizing
   * jobs.
   *
   * @param  requestInfo      The state information for this request.
   * @param  showAll          Indicates whether information about all optimizing
   *                          jobs should be shown even if an optimizing job ID
   *                          has been provided.
   * @param  folderName       The name of the job folder that should be
   *                          displayed.
   */
  static void handleViewOptimizing(RequestInfo requestInfo, boolean showAll,
                                   String folderName)
  {
    logMessage(requestInfo, "In generateOptimizeHelpPage()");

    // If the user doesn't have permission to view jobs, then they can't see
    // this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information.");
      return;
    }

    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // See if an optimizing job ID has been specified.  If so, then show that
    // specific job.  If not, then show the list of all optimizing jobs.
    if (! showAll)
    {
      String optimizingJobID =
           request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
      if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
      {
        generateViewOptimizingJobBody(requestInfo, optimizingJobID);
        return;
      }

    }

    if ((folderName == null) || (folderName.length() == 0))
    {
      folderName = Constants.FOLDER_NAME_UNCLASSIFIED;
    }

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">View Optimizing Jobs</SPAN>" + EOL);
    htmlBody.append("<BR>" + EOL);

    String link = generateLink(requestInfo,
                       Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                       Constants.SERVLET_PARAM_JOB_FOLDER, folderName,
                       "Switch to completed jobs for this folder");
    htmlBody.append(link + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    JobFolder   folder  = null;
    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if ((folders != null) && (folders.length > 0))
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  View Optimizing Jobs in Folder" + EOL);
      htmlBody.append("  <SELECT NAME=\"" + Constants.SERVLET_PARAM_JOB_FOLDER +
                      "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        String selectedStr;
        if ((folderName != null) &&
            folderName.equals(folders[i].getFolderName()))
        {
          selectedStr = "SELECTED ";
          folder = folders[i];
        }
        else
        {
          selectedStr = "";
        }
        htmlBody.append("    <OPTION " + selectedStr + "VALUE=\"" +
                        folders[i].getFolderName() + "\">" +
                        folders[i].getFolderName() + EOL);
      }
      htmlBody.append("  </SELECT>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR>" + EOL);
    }
    else
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR>" + EOL);
    }

    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_OPTIMIZE) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  Schedule an optimizing " + EOL);
      htmlBody.append("  <SELECT NAME=\"" + Constants.SERVLET_PARAM_JOB_CLASS +
                      "\">" + EOL);

      JobClass[][] jobClasses = slamdServer.getCategorizedJobClasses();
      for (int i=0; i < jobClasses.length; i++)
      {
        for (int j=0; j < jobClasses[i].length; j++)
        {
          String category = jobClasses[i][j].getJobCategoryName();
          if ((category == null) || (category.length() == 0))
          {
            category = "Unclassified";
          }
          htmlBody.append("    <OPTION VALUE=\"" +
                          jobClasses[i][j].getClass().getName() +
                          "\">" + category + " -- " +
                          jobClasses[i][j].getJobName() + EOL);
        }
      }

      htmlBody.append("  </SELECT>" + EOL);
      htmlBody.append("  job." + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }

    if (folder != null)
    {
      htmlBody.append("<B>Jobs for Folder " + folder.getFolderName() + "</B>" +
                      EOL);
      htmlBody.append("<BR>" + EOL);
      String description = folder.getDescription();
      if ((description != null) && (description.length() > 0))
      {
        htmlBody.append("<BLOCKQUOTE>" + description + "</BLOCKQUOTE>" +
                        EOL);
      }

      if (requestInfo.mayManageFolders)
      {
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
             generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                  Constants.SERVLET_SECTION_OPTIMIZING_FOLDER_DESCRIPTION) +
             EOL);
        htmlBody.append("  " +
             generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                            folderName) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(
               generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
               EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Edit Description\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }

      htmlBody.append("<BR>" + EOL);


      if (requestInfo.mayManageFolders && enableReadOnlyManagement)
      {
        if (folder.displayInReadOnlyMode())
        {
          htmlBody.append("This folder is currently visible in " +
                          "restricted read-only mode." + EOL);
          htmlBody.append("<BR>");
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("  " +
               generateHidden(Constants.SERVLET_PARAM_SECTION,
                              Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("  " +
               generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                    Constants.SERVLET_SECTION_OPTIMIZING_FOLDER_PUBLISH) +
               EOL);
          htmlBody.append("  " +
               generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                              folderName) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(
                 generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                "1") +
                 EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT +
                          "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_DEPUBLISH_FOLDER +
                          "\">" + EOL);
          htmlBody.append("  &nbsp;" + EOL);
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_DEPUBLISH_FOLDER_JOBS +
                        "\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
          htmlBody.append("<BR>" + EOL);
        }
        else
        {
          htmlBody.append("This folder is currently not visible in " +
                          "restricted read-only mode." + EOL);
          htmlBody.append("<BR>");
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
               generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                    Constants.SERVLET_SECTION_OPTIMIZING_FOLDER_PUBLISH) +
               EOL);
          htmlBody.append("  " +
               generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                              folderName) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(
                 generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                "1") +
                 EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT +
                          "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_PUBLISH_FOLDER +
                          "\">" + EOL);
          htmlBody.append("  &nbsp;" + EOL);
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT +
                          "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_PUBLISH_FOLDER_JOBS +
                          "\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
          htmlBody.append("<BR>" + EOL);
        }
      }
    }


    // Get the list of optimizing jobs that have been defined in the config
    // directory.
    OptimizingJob[] optimizingJobs = null;
    try
    {
      optimizingJobs = configDB.getSummaryOptimizingJobs(folderName);
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  Unable to retrieve the set of optimizing " +
                         "jobs -- " + e.getMessage() + "<BR>" + EOL);
      htmlBody.append("Unable to retrieve the set of optimizing jobs." + EOL);
      return;
    }

    if ((optimizingJobs == null) || (optimizingJobs.length == 0))
    {
      if ((folders == null) || (folders.length == 0))
      {
        htmlBody.append("There are no optimizing jobs in the " +
                        "configuration directory.<BR><BR>" + EOL);
      }
      else
      {
        if ((folderName == null) || (folderName.length() == 0))
        {
          htmlBody.append("There are no optimizing jobs in the " +
                          Constants.FOLDER_NAME_UNCLASSIFIED +
                          "\" job folder.<BR><BR>" + EOL);
        }
        else
        {
          htmlBody.append("There are no optimizing jobs in the " + folderName +
                          "\" job folder.<BR><BR>" + EOL);
        }
      }
    }
    else
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OPTIMIZING) +
                      EOL);
      if (folderName != null)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
      }

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      if (! readOnlyMode)
      {
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      }
      htmlBody.append("      <TD><B>Job ID</B></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><B>Description</B></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><B>Job Type</B></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><B>Actual Start Time</B></TD>" + EOL);

      if (enableReadOnlyManagement && (! readOnlyMode))
      {
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><B>Read-Only Status</B></TD>" + EOL);
      }

      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><B>Current State</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      boolean selectAll   = false;
      boolean deselectAll = false;
      String  submitStr   =
           request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr != null) &&
          submitStr.equals(Constants.SUBMIT_STRING_SELECT_ALL))
      {
        selectAll = true;
      }
      else if ((submitStr != null) &&
               submitStr.equals(Constants.SUBMIT_STRING_DESELECT_ALL))
      {
        deselectAll = true;
      }

      String[] selectedJobs = request.getParameterValues(
                                   Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);

      for (int i=0; i < optimizingJobs.length; i++)
      {
        if ((i % 2) == 0)
        {
          htmlBody.append("    <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        }
        else
        {
          htmlBody.append("    <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        }

        String checkedStr = "";
        String jobID      = optimizingJobs[i].getOptimizingJobID();
        if (! readOnlyMode)
        {
          if (selectAll)
          {
            checkedStr = " CHECKED";
          }
          else if (! deselectAll)
          {
            if (selectedJobs != null)
            {
              for (int j=0; j < selectedJobs.length; j++)
              {
                if (selectedJobs[j].equals(jobID))
                {
                  checkedStr = " CHECKED";
                  break;
                }
              }
            }
          }
          htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                          Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID +
                          "\" VALUE=\"" + jobID + '"' + checkedStr + "></TD>" +
                          EOL);
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        }

        link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                            Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                            Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID, jobID,
                            jobID);
        htmlBody.append("      <TD>" + link + "</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);

        String description = optimizingJobs[i].getDescription();
        if (description == null)
        {
          description = "";
        }
        htmlBody.append("      <TD>" + description + "</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);

        String jobName =
             optimizingJobs[i].getJobClass().getJobName();
        htmlBody.append("      <TD>" + jobName + "</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);

        String startTimeStr;
        Date startTime = optimizingJobs[i].getActualStartTime();
        if (startTime == null)
        {
          startTime = optimizingJobs[i].getStartTime();
        }
        if (startTime == null)
        {
          startTimeStr = "";
        }
        else
        {
          startTimeStr = displayDateFormat.format(startTime);
        }
        htmlBody.append("      <TD>" + startTimeStr + "</TD>" + EOL);

        if (enableReadOnlyManagement && (! readOnlyMode))
        {
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          if (optimizingJobs[i].displayInReadOnlyMode())
          {
            htmlBody.append("      <TD>Published</TD>" + EOL);
          }
          else
          {
            htmlBody.append("      <TD>Not Published</TD>" + EOL);
          }
        }

        String stateStr;
        switch (optimizingJobs[i].getJobState())
        {
          case Constants.JOB_STATE_CANCELLED:
          case Constants.JOB_STATE_STOPPED_BY_USER:
            stateStr = "Cancelled";
            break;
          case Constants.JOB_STATE_COMPLETED_SUCCESSFULLY:
          case Constants.JOB_STATE_COMPLETED_WITH_ERRORS:
          case Constants.JOB_STATE_STOPPED_DUE_TO_DURATION:
          case Constants.JOB_STATE_STOPPED_DUE_TO_STOP_TIME:
            stateStr = "Completed";
            break;
          case Constants.JOB_STATE_DISABLED:
            stateStr = "Disabled";
            break;
          case Constants.JOB_STATE_NOT_YET_STARTED:
          case Constants.JOB_STATE_UNINITIALIZED:
            stateStr = "Pending";
            break;
          case Constants.JOB_STATE_RUNNING:
            stateStr = "Running";
            break;
          case Constants.JOB_STATE_STOPPED_BY_SHUTDOWN:
          case Constants.JOB_STATE_STOPPED_DUE_TO_ERROR:
            stateStr = "Stopped";
            break;
          default:
            stateStr = "Unknown";
            break;
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" + stateStr + "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
      htmlBody.append("  </TABLE>" + EOL);

      if (! readOnlyMode)
      {
        ArrayList<String> actions = new ArrayList<String>();
        actions.add(Constants.SUBMIT_STRING_SELECT_ALL);
        actions.add(Constants.SUBMIT_STRING_DESELECT_ALL);

        if (requestInfo.mayDeleteJob)
        {
          actions.add(Constants.SUBMIT_STRING_DELETE);
        }

        if (requestInfo.mayManageFolders)
        {
          if ((folders != null) && (folders.length > 0))
          {
            actions.add(Constants.SUBMIT_STRING_MOVE);
          }

          if (enableReadOnlyManagement)
          {
            actions.add(Constants.SUBMIT_STRING_PUBLISH_JOBS);
            actions.add(Constants.SUBMIT_STRING_DEPUBLISH_JOBS);
          }
        }

        if ((reportGenerators != null) && (reportGenerators.length > 0))
        {
          actions.add(Constants.SUBMIT_STRING_GENERATE_REPORT);
        }

        htmlBody.append("  <TABLE BORDER=\"0\" WIDTH=\"100%\">" + EOL);
        htmlBody.append("    <TR>" + EOL);

        for (int i=0; i < actions.size(); i++)
        {
          htmlBody.append("      <TD WIDTH=\"20%\" ALIGN=\"CENTER\"><INPUT " +
                          "TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                          actions.get(i) + "\">" + EOL);

          if (((i % 4) == 3) && ((i+1) < actions.size()))
          {
            htmlBody.append("    </TR>" + EOL);
            htmlBody.append("    <TR>" + EOL);
          }
        }

        if ((actions.size() % 4) != 0)
        {
          for (int i=0; i < (4 - (actions.size() % 4)); i++)
          {
            htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          }
        }

        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("  </TABLE>" + EOL);
      }

      htmlBody.append("</FORM>" + EOL);
    }


    // The remainder of this section deals with file uploads.  If the file
    // upload capability has been disabled, then don't add that content to
    // the page.
    if (disableUploads)
    {
      return;
    }


    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Uploaded Files</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    UploadedFile[] files = null;
    try
    {
      files = configDB.getUploadedFiles(folderName);
    }
    catch (DatabaseException de)
    {
      infoMessage.append("Unable to retrieve uploaded file list -- " +
                         de.getMessage() + "<BR>" + EOL);
    }

    if ((files == null) || (files.length == 0))
    {
      htmlBody.append("No files have been uploaded into this folder." +
                      EOL);
    }
    else
    {
      htmlBody.append("The following files have been uploaded:" + EOL);
      htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD><B>File Name</B></TD>" + EOL);
      htmlBody.append("    <TD><B>File Type</B></TD>" + EOL);
      htmlBody.append("    <TD><B>File Size (bytes)</B></TD>" + EOL);
      htmlBody.append("    <TD><B>Description</B></TD>" + EOL);
      htmlBody.append("    <TD><B>Action</B></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      for (int i=0; i < files.length; i++)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + files[i].getFileName() + "</TD>" +
                        EOL);
        htmlBody.append("    <TD>" + files[i].getFileType() + "</TD>" +
                        EOL);
        htmlBody.append("    <TD>" + files[i].getFileSize() + "</TD>" +
                        EOL);

        String desc = files[i].getFileDescription();
        if ((desc == null) || (desc.length() == 0))
        {
          desc = "&nbsp;";
        }
        htmlBody.append("    <TD>" + desc + "</TD>" + EOL);

        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\" TARGET=\"_BLANK\">" + EOL);
        htmlBody.append("              "  +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("              "  +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_UPLOAD) +  EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                       Constants.FILE_ACTION_VIEW) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_NAME,
                                       files[i].getFileName()) +EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                       "true") + EOL);
        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"View\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);

        htmlBody.append("          <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("              "  +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("              "  +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_UPLOAD) +  EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                       Constants.FILE_ACTION_SAVE) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_NAME,
                                       files[i].getFileName()) +EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                       "true") + EOL);
        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Save\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);

        if (requestInfo.mayManageFolders)
        {
          htmlBody.append("          <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("          <TD>" + EOL);
          htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("              "  +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("              "  +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_UPLOAD) +
                          EOL);
          htmlBody.append("              " +
                          generateHidden(
                               Constants.SERVLET_PARAM_FILE_ACTION,
                               Constants.FILE_ACTION_EDIT_TYPE) + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                         folderName) + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_FILE_NAME,
                                         files[i].getFileName()) +EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                         "true") + EOL);
          htmlBody.append("              <INPUT TYPE=\"SUBMIT\" " +
                          "VALUE=\"Edit MIME Type\">" + EOL);
          htmlBody.append("            </FORM>" + EOL);
          htmlBody.append("          </TD>" + EOL);

          htmlBody.append("          <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("          <TD>" + EOL);
          htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("              "  +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("              "  +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_UPLOAD) +
                          EOL);
          htmlBody.append("              " +
                          generateHidden(
                               Constants.SERVLET_PARAM_FILE_ACTION,
                               Constants.FILE_ACTION_DELETE) + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                         folderName) + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_FILE_NAME,
                                         files[i].getFileName()) +EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                         "true") + EOL);
          htmlBody.append("              <INPUT TYPE=\"SUBMIT\" " +
                          "VALUE=\"Delete\">" + EOL);
          htmlBody.append("            </FORM>" + EOL);
          htmlBody.append("          </TD>" + EOL);
        }

        htmlBody.append("        </TR>" + EOL);
        htmlBody.append("      </TABLE>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }

      htmlBody.append("</TABLE>" + EOL);
    }

    if (requestInfo.mayManageFolders)
    {
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_UPLOAD) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                     folderName) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                     Constants.FILE_ACTION_UPLOAD) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                     "true") + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Upload File\">" +
                      EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }




  /**
   * Generates a page that may be used to view information about an optimizing
   * job.
   *
   * @param  requestInfo      The state information for this request.
   * @param  optimizingJobID  The unique ID of the optimizing job to display.
   */
  static void generateViewOptimizingJobBody(RequestInfo requestInfo,
                                            String optimizingJobID)
  {
    logMessage(requestInfo, "In generateOptimizeHelpPage()");

    // If the user doesn't have permission to view jobs, then they can't see
    // this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information.");
      return;
    }

    // Get the important state information for this request.
    String       servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder htmlBody       = requestInfo.htmlBody;
    StringBuilder infoMessage    = requestInfo.infoMessage;

    // Get the optimizing job to be viewed.
    OptimizingJob optimizingJob = null;
    try
    {
      optimizingJob = getOptimizingJob(optimizingJobID);
      if (optimizingJob == null)
      {
        throw new SLAMDException("Optimizing job " + optimizingJobID +
                                 " does not exist in the SLAMD database.");
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  Unable to retrieve information for " +
                         "optimizing job " + optimizingJobID + " -- " + e +
                         "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">View Optimizing Job</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No information about optimizing job " + optimizingJobID +
                      " could be retrieved from the configuration directory." +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
      String link =
           generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                        Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                        "here");
      htmlBody.append("Click " + link + " to return to the list of " +
                      "optimizing jobs." + EOL);
      return;
    }

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">View Optimizing Job " + optimizingJobID + "</SPAN>" +
                    EOL);

    int buttonsAdded = 0;
    boolean isUnknownJob =
         ((optimizingJob == null) || (optimizingJob.getJobClass() == null) ||
          (optimizingJob.getJobClass() instanceof UnknownJobClass));

    htmlBody.append("<TABLE BORDER=\"0\" WIDTH=\"100%\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
    htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("        " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                                Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("        " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING) + EOL);
    htmlBody.append("        " +
                    generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                   optimizingJobID) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
    htmlBody.append("      </FORM>" + EOL);
    htmlBody.append("    </TD>" + EOL);
    buttonsAdded++;
    if ((buttonsAdded % 5) == 0)
    {
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
    }

    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if (requestInfo.mayManageFolders && (folders != null) &&
        (folders.length > 0))
    {
      htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
      htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MOVE_OPTIMIZING) +
                      EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Move\">" + EOL);
      htmlBody.append("      </FORM>" + EOL);
      htmlBody.append("    </TD>" + EOL);
      buttonsAdded++;
      if ((buttonsAdded % 5) == 0)
      {
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
      }
    }

    if (requestInfo.mayScheduleJob && (! isUnknownJob))
    {
      htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
      htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_CLONE_OPTIMIZING) +
                      EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Clone\">" + EOL);
      htmlBody.append("      </FORM>" + EOL);
      htmlBody.append("    </TD>" + EOL);
      buttonsAdded++;
      if ((buttonsAdded % 5) == 0)
      {
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
      }
    }

    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
      htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                      Constants.SERVLET_SECTION_JOB_EDIT_OPTIMIZING_COMMENTS) +
                      EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit " +
                      "Comments\">" + EOL);
      htmlBody.append("      </FORM>" + EOL);
      htmlBody.append("    </TD>" + EOL);
      buttonsAdded++;
      if ((buttonsAdded % 5) == 0)
      {
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
      }
    }

    if (optimizingJob.doneRunning())
    {
      if (optimizingJob.hasStats())
      {
        ArrayList<String> jobIDList = new ArrayList<String>();
        Job[] jobs = optimizingJob.getAssociatedJobsIncludingReRun();
        for (int i=0; i < jobs.length; i++)
        {
          if (jobs[i].hasStats())
          {
            jobIDList.add(jobs[i].getJobID());
          }
        }
        if (! jobIDList.isEmpty())
        {
          htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_MASS_OP) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                         Constants.SUBMIT_STRING_COMPARE) +
                          EOL);

          for (int i=0; i < jobIDList.size(); i++)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                           jobIDList.get(i)) +
                            EOL);
          }

          if (requestInfo.debugHTML)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Compare\">" +
                          EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          buttonsAdded++;
          if ((buttonsAdded % 5) == 0)
          {
            htmlBody.append("  </TR>" + EOL);
            htmlBody.append("  <TR>" + EOL);
          }
        }
      }

      Job[] jobs = optimizingJob.getAssociatedJobs();
      if ((jobs != null) && (jobs.length > 0))
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_MASS_OP) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                       Constants.SUBMIT_STRING_EXPORT) + EOL);

        for (int i=0; i < jobs.length; i++)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                         jobs[i].getJobID()) + EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Export\">" +
                        EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      if (requestInfo.mayDeleteJob)
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_DELETE_OPTIMIZING) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(
                             Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                             optimizingJobID) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Delete\">" +
                        EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      if ((reportGenerators != null) && (reportGenerators.length > 0))
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_GENERATE_REPORT) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(
                             Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                             optimizingJobID) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Generate Report\">" + EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }
    }
    else
    {
      if (requestInfo.mayCancelJob)
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_CANCEL_OPTIMIZING) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(
                             Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                             optimizingJobID) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\">" +
                        EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;


        if (optimizingJob.pauseRequested())
        {
          htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_UNPAUSE_OPTIMIZING) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(
                               Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                               optimizingJobID) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Unpause\">" +
                          EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          buttonsAdded++;
        }
        else
        {
          htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_PAUSE_OPTIMIZING) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(
                               Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                               optimizingJobID) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Pause\">" +
                          EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          buttonsAdded++;
        }
      }
    }

    while ((buttonsAdded % 5) != 0)
    {
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      buttonsAdded++;
    }

    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("</TABLE>" + EOL);

    String folderName = optimizingJob.getFolderName();
    if ((folderName != null) && (folderName.length() > 0))
    {
      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                                 Constants.SERVLET_PARAM_JOB_FOLDER,
                                 folderName, folderName);
      htmlBody.append("Return to optimizing job folder " + link + '.' + EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }

    htmlBody.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>General Information</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Optimizing Job ID</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJobID + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    JobClass jobClass = optimizingJob.getJobClass();
    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_CLASSES,
                               Constants.SERVLET_PARAM_JOB_CLASS,
                               jobClass.getClass().getName(),
                               jobClass.getJobName());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Type</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + link + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String jobGroup = optimizingJob.getJobGroup();
    if (jobGroup == null)
    {
      jobGroup = "(not specified)";
    }
    else
    {
      jobGroup = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                              Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                              Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                              jobGroup, jobGroup);
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Group</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + jobGroup + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String descriptionStr = optimizingJob.getDescription();
    if ((descriptionStr == null) || (descriptionStr.length() == 0))
    {
      descriptionStr = "(not specified)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Base Description</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + descriptionStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Include Thread Count in Description</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.includeThreadsInDescription() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String stateStr = Constants.jobStateToString(optimizingJob.getJobState());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Current State</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + stateStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String stopReason = optimizingJob.getStopReason();
    if ((stopReason == null) || (stopReason.length() == 0))
    {
      stopReason = "(not applicable)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Stop Reason</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + stopReason + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String comments = optimizingJob.getComments();
    if ((comments == null) || (comments.length() == 0))
    {
      comments = "(none)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Comments</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + comments + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Schedule Information</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    String startTimeStr =
         displayDateFormat.format(optimizingJob.getStartTime());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Start Time</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + startTimeStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String durationStr;
    if (optimizingJob.getDuration() > 0)
    {
      durationStr = secondsToHumanReadableDuration(optimizingJob.getDuration());
    }
    else
    {
      durationStr = "(not specified)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + durationStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Delay Between Iterations</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getDelayBetweenIterations() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Number of Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getNumClients() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    if (! (readOnlyMode && hideSensitiveInformation))
    {
      String[] requestedClients = optimizingJob.getRequestedClients();
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      htmlBody.append("    <TD>Requested Clients</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      if ((requestedClients == null) || (requestedClients.length == 0))
      {
        htmlBody.append("    <TD>(none specified)</TD>" + EOL);
      }
      else
      {
        htmlBody.append("    <TD>" + EOL);
        for (int i=0; i < requestedClients.length; i++)
        {
          htmlBody.append("      " + requestedClients[i] + "<BR>" + EOL);
        }
        htmlBody.append("    </TD>" + EOL);
      }
      htmlBody.append("  </TR>" + EOL);

      String[] monitorClients = optimizingJob.getResourceMonitorClients();
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      htmlBody.append("    <TD>Resource Monitor Clients</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      if ((monitorClients == null) || (monitorClients.length == 0))
      {
        htmlBody.append("    <TD>(none specified)</TD>" + EOL);
      }
      else
      {
        htmlBody.append("    <TD>" + EOL);
        for (int i=0; i < monitorClients.length; i++)
        {
          htmlBody.append("      " + monitorClients[i] + "<BR>" + EOL);
        }
        htmlBody.append("    </TD>" + EOL);
      }
      htmlBody.append("  </TR>" + EOL);
    }

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Monitor Client If Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.monitorClientsIfAvailable() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Minimum Number of Threads</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getMinThreads() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String maxThreadStr;
    if (optimizingJob.getMaxThreads() > 0)
    {
      maxThreadStr = String.valueOf(optimizingJob.getMaxThreads());
    }
    else
    {
      maxThreadStr = "(not specified)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + maxThreadStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Thread Increment Between Iterations</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getThreadIncrement() + "</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getThreadStartupDelay() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Statistics Collection Interval</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" +
         secondsToHumanReadableDuration(optimizingJob.getCollectionInterval()) +
         "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    if (! (readOnlyMode && hideSensitiveInformation))
    {
      String[] notifyAddresses = optimizingJob.getNotifyAddresses();
      String addressStr;
      if ((notifyAddresses == null) || (notifyAddresses.length == 0))
      {
        addressStr = "(none specified)";
      }
      else
      {
        addressStr = "<A HREF=\"mailto:" + notifyAddresses[0] + "\">" +
                     notifyAddresses[0] + "</A>";
        for (int i=1; i < notifyAddresses.length; i++)
        {
          addressStr += ", <A HREF=\"mailto:" + notifyAddresses[i] + "\">" +
                        notifyAddresses[i] + "</A>";
        }
      }
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      htmlBody.append("    <TD>Notify on Completion</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + addressStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    String[] dependencies = optimizingJob.getDependencies();
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    if ((dependencies == null) || (dependencies.length == 0))
    {
      htmlBody.append("        (none specified)<BR>" + EOL);
    }
    else
    {
      for (int i=0; i < dependencies.length; i++)
      {
        if ((dependencies[i] == null) || (dependencies[i].length() == 0))
        {
          // If somehow we have an empty dependency, then skip it.
          continue;
        }

        // Try to determine whether this dependency is for a normal or an
        // optimizing job.
        Job j = null;
        String dependencyStr = dependencies[i];
        try
        {
          j = configDB.getJob(dependencies[i]);
          if (j != null)
          {
            link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                Constants.SERVLET_PARAM_JOB_ID, dependencies[i],
                                dependencies[i]);
            String description = j.getJobDescription();
            if (description == null)
            {
              description = "";
            }
            else
            {
              description = " -- " + description;
            }
            dependencyStr = link + " -- " + j.getJobName() + description;
          }
        } catch (Exception e) {}

        if (j == null)
        {
          try
          {
            OptimizingJob oj = getOptimizingJob(dependencies[i]);
            if (oj != null)
            {
              link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                  Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                                  Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                  dependencies[i], dependencies[i]);
              String description = oj.getDescription();
              if (description == null)
              {
                description = "";
              }
              else
              {
                description = " -- " + description;
              }
              dependencyStr = link + " -- Optimizing " +
                              oj.getJobClass().getJobName() + description;
            }
          } catch (Exception e) {}
        }

        htmlBody.append("      " + dependencyStr + "<BR>" + EOL);
      }
    }
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Optimization Settings</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Max. Consecutive Non-Improving " +
                    "Iterations</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getMaxNonImproving() + "</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.reRunBestIteration() + "</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Re-Run Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + ((optimizingJob.getReRunDuration() > 0)
         ? secondsToHumanReadableDuration(optimizingJob.getReRunDuration())
         : "(not specified)") + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Optimization Algorithm</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" +
                    optimizingJob.getOptimizationAlgorithm().
                         getOptimizationAlgorithmName() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    if (! (jobClass instanceof UnknownJobClass))
    {
      Parameter[] parameters =
           optimizingJob.getOptimizationAlgorithm().
                getOptimizationAlgorithmParameters().getParameters();
      for (int i=0, j=0; i < parameters.length; i++,j++)
      {
        if ((parameters[i] instanceof PlaceholderParameter) ||
             (parameters[i] instanceof LabelParameter))
        {
          j--;
          continue;
        }

        if (readOnlyMode && hideSensitiveInformation &&
             parameters[i].isSensitive())
        {
          j--;
          continue;
        }

        if ((j % 2) == 0)
        {
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        }
        else
        {
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        }

        htmlBody.append("    <TD>" + parameters[i].getDisplayName() +
             "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + parameters[i].getHTMLDisplayValue() +
             "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Parameter Information</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    Parameter[] parameters = optimizingJob.getParameters().getParameters();
    for (int i=0; i < parameters.length; i++)
    {
      if ((i % 2) == 0)
      {
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
      }
      else
      {
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
      }

      if (parameters[i] instanceof PlaceholderParameter)
      {
        htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      }
      else if (readOnlyMode && hideSensitiveInformation &&
               parameters[i].isSensitive())
      {
        htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      }
      else
      {
        htmlBody.append("    <TD>" + parameters[i].getDisplayName() + "</TD>" +
                        EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + parameters[i].getHTMLDisplayValue() +
                        "</TD>" + EOL);
      }

      htmlBody.append("  </TR>" + EOL);
    }

    Job[] jobIterations = optimizingJob.getAssociatedJobs();
    if (optimizingJob.doneRunning())
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Execution Data</B></TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);

      Date actualStartTime = optimizingJob.getActualStartTime();
      if (actualStartTime == null)
      {
        startTimeStr = "(not available)";
      }
      else
      {
        startTimeStr = displayDateFormat.format(actualStartTime);
      }
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      htmlBody.append("    <TD>Actual Start Time</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + startTimeStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      Date actualStopTime = optimizingJob.getActualStopTime();
      String stopTimeStr;
      if (actualStopTime == null)
      {
        stopTimeStr = "(not available)";
      }
      else
      {
        stopTimeStr = displayDateFormat.format(actualStopTime);
      }
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      htmlBody.append("    <TD>Actual Stop Time</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + stopTimeStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      String iterationsStr;
      if (jobIterations == null)
      {
        iterationsStr = "0";
      }
      else
      {
        iterationsStr = String.valueOf(jobIterations.length);
      }
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      htmlBody.append("    <TD>Job Iterations Completed</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + iterationsStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      int    optimalThreads = optimizingJob.getOptimalThreadCount();
      double optimalValue   = optimizingJob.getOptimalValue();
      String optimalJobID   = optimizingJob.getOptimalJobID();
      if (optimalThreads > 0)
      {
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        htmlBody.append("    <TD>Optimal Thread Count</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + optimalThreads + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        htmlBody.append("    <TD>Optimal Value</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + decimalFormat.format(optimalValue) +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                            Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                            Constants.SERVLET_PARAM_JOB_ID, optimalJobID,
                            optimalJobID);
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        htmlBody.append("    <TD>Optimal Job Iteration</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + link + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        String valueStr;
        Job reRunIteration = optimizingJob.getReRunIteration();
        if (reRunIteration == null)
        {
          if (optimizingJob.reRunBestIteration())
          {
            valueStr = "(not available)";
          }
          else
          {
            valueStr = "(none specified)";
          }
        }
        else
        {
          String jobID = reRunIteration.getJobID();
          valueStr = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                  Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                  Constants.SERVLET_PARAM_JOB_ID, jobID, jobID);
        }
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        htmlBody.append("    <TD>Re-Run of Best Iteration</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);

        htmlBody.append("    <TD>" + valueStr + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        if (reRunIteration != null)
        {
          String statString = "(not available)";
          try
          {
            double value =
                 optimizingJob.getOptimizationAlgorithm().
                      getIterationOptimizationValue(reRunIteration);
            statString = decimalFormat.format(value);
          } catch (Exception e) {}

          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          htmlBody.append("    <TD>Re-Run Iteration Value</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);

          htmlBody.append("    <TD>" + statString + "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
      }
      else
      {
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        htmlBody.append("    <TD>Optimal Thread Count</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>(not available)</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        htmlBody.append("    <TD>Optimal Value</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>(not available)</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        htmlBody.append("    <TD>Optimal Job Iteration</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>(not available)</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    if ((jobIterations != null) && (jobIterations.length > 0))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Job Iterations</B></TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);

      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\" WIDTH=\"100%\">" + EOL);
      htmlBody.append("      <TABLE BORDER=\"0\" CELLSPACING=\"0\" " +
                      "WIDTH=\"100%\">" + EOL);
      for (int i=0; i < jobIterations.length; i++)
      {
        if ((i % 2) == 0)
        {
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        }
        else
        {
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        }

        link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                            Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                            Constants.SERVLET_PARAM_JOB_ID,
                            jobIterations[i].getJobID(),
                            jobIterations[i].getJobID());
        htmlBody.append("          <TD>" + link + "</TD>" + EOL);
        htmlBody.append("          <TD>&nbsp;</TD>" + EOL);

        String statString = "&nbsp;";
        try
        {
          double value =
               optimizingJob.getOptimizationAlgorithm().
                    getIterationOptimizationValue(jobIterations[i]);
          statString = decimalFormat.format(value);
        } catch (Exception e) {}

        htmlBody.append("          <TD>" + statString + "</TD>" + EOL);
        htmlBody.append("          <TD>&nbsp;</TD>" + EOL);

        stateStr = Constants.jobStateToString(jobIterations[i].getJobState());
        htmlBody.append("          <TD>" + stateStr + "</TD>" + EOL);
        htmlBody.append("        </TR>" + EOL);
      }
      htmlBody.append("      </TABLE>" + EOL);
      htmlBody.append("    </TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work of cancelling an optimizing job in the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleCancelOptimizingJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleCancelOptimizingJob()");

    // If the user doesn't have cancel permission, then they can't see this
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "cancel jobs.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID of the optimizing job to cancel.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      String message = "No optimizing job specified to cancel.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleOptimizeJob(requestInfo);
      return;
    }


    // Get the optimizing job and make sure it exists.
    OptimizingJob optimizingJob;
    try
    {
      optimizingJob = getOptimizingJob(optimizingJobID);
    }
    catch (Exception e)
    {
      String message = "Could not retrieve optimizing job " + optimizingJobID +
                       "from the configuration directory -- " + e;
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleViewOptimizing(requestInfo, true);
      return;
    }
    if (optimizingJob == null)
    {
      String message = "Could not retrieve optimizing job " + optimizingJobID +
                       "from the configuration directory.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleViewOptimizing(requestInfo, true);
      return;
    }


    // See if the user has provided confirmation for the cancel.  If not, then
    // request it.  If so, then take the appropriate action.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && confirmStr.equalsIgnoreCase("Yes"))
    {
      try
      {
        optimizingJob.cancel();
        infoMessage.append("A request has been submitted to cancel the " +
                           "optimizing job.  It may take some time for the " +
                           "cancel to occur.<BR>" + EOL);
        generateViewOptimizingJobBody(requestInfo, optimizingJobID);
        return;
      }
      catch (SLAMDServerException sse)
      {
        String message = "Unable to cancel optimizing job " + optimizingJobID +
                         " -- " + sse;
        infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
        requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                       message);
        generateViewOptimizingJobBody(requestInfo, optimizingJobID);
        return;
      }
    }
    else if ((confirmStr != null) && confirmStr.equalsIgnoreCase("No"))
    {
      infoMessage.append("Optimizing job " + optimizingJobID +
                         " was not cancelled.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Cancel Optimizing Job " + optimizingJobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to cancel this job?" + EOL);
      htmlBody.append("<BR>");
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_CANCEL_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of pausing an optimizing job in the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handlePauseOptimizingJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handlePauseOptimizingJob()");

    // If the user doesn't have cancel permission, then they can't see this
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "pause jobs.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID of the optimizing job to pause.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      String message = "No optimizing job specified to pause.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleOptimizeJob(requestInfo);
      return;
    }


    // Get the optimizing job and make sure it exists.
    OptimizingJob optimizingJob;
    try
    {
      optimizingJob = getOptimizingJob(optimizingJobID);
    }
    catch (Exception e)
    {
      String message = "Could not retrieve optimizing job " + optimizingJobID +
                       "from the configuration directory -- " + e;
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleViewOptimizing(requestInfo, true);
      return;
    }
    if (optimizingJob == null)
    {
      String message = "Could not retrieve optimizing job " + optimizingJobID +
                       "from the configuration directory.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleViewOptimizing(requestInfo, true);
      return;
    }


    // See if the user has provided confirmation for the pause.  If not, then
    // request it.  If so, then take the appropriate action.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && confirmStr.equalsIgnoreCase("Yes"))
    {
      optimizingJob.pauseBeforeNextIteration();
      infoMessage.append("A request has been submitted to pause the " +
                         "optimizing job.  The next iteration of this job " +
                         "will be created as disabled and must be " +
                         "manually enabled before it will run.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else if ((confirmStr != null) && confirmStr.equalsIgnoreCase("No"))
    {
      infoMessage.append("Optimizing job " + optimizingJobID +
                         " was not paused.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Pause Optimizing Job " + optimizingJobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to pause this optimizing " +
                      "job?" + EOL);
      htmlBody.append("<BR>");
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_PAUSE_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of unpausing an optimizing job in the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleUnpauseOptimizingJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleUnpauseOptimizingJob()");

    // If the user doesn't have cancel permission, then they can't see this
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "pause or unpause jobs.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID of the optimizing job to un-pause.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      String message = "No optimizing job specified to unpause.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleOptimizeJob(requestInfo);
      return;
    }


    // Get the optimizing job and make sure it exists.
    OptimizingJob optimizingJob;
    try
    {
      optimizingJob = getOptimizingJob(optimizingJobID);
    }
    catch (Exception e)
    {
      String message = "Could not retrieve optimizing job " + optimizingJobID +
                       "from the configuration directory -- " + e;
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleViewOptimizing(requestInfo, true);
      return;
    }
    if (optimizingJob == null)
    {
      String message = "Could not retrieve optimizing job " + optimizingJobID +
                       "from the configuration directory.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      handleViewOptimizing(requestInfo, true);
      return;
    }


    // See if the user has provided confirmation for the un-pause.  If not, then
    // request it.  If so, then take the appropriate action.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && confirmStr.equalsIgnoreCase("Yes"))
    {
      optimizingJob.cancelPause();
      infoMessage.append("A request has been submitted to unpause the " +
                         "optimizing job.  The next iteration of this job " +
                         "will not be disabled when it is scheduled.<BR>" +
                         EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else if ((confirmStr != null) && confirmStr.equalsIgnoreCase("No"))
    {
      infoMessage.append("Optimizing job " + optimizingJobID +
                         " was not unpaused.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Unpause Optimizing Job " + optimizingJobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to unpause this optimizing " +
                      "job?" + EOL);
      htmlBody.append("<BR>");
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_UNPAUSE_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of removing information about an optimizing job from the
   * SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDeleteOptimizingJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDeleteOptimizingJob()");

    // If the user doesn't have delete permission, then they can't see this
    if (! requestInfo.mayDeleteJob)
    {
      logMessage(requestInfo, "No mayDeleteJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "delete job information.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID of the optimizing job to delete.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      infoMessage.append("ERROR:  No optimizing job specified to delete.<BR>" +
                         EOL);
      handleOptimizeJob(requestInfo);
      return;
    }


    // See if the user has provided confirmation for the delete.  If not, then
    // request it.  If so, then take the appropriate action.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && confirmStr.equalsIgnoreCase("Yes"))
    {
      try
      {
        boolean includeIterations = false;
        String includeStr =
             request.getParameter(
                  Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS);
        includeIterations = ((includeStr != null) &&
                             (includeStr.equalsIgnoreCase("true") ||
                              includeStr.equalsIgnoreCase("yes") ||
                              includeStr.equalsIgnoreCase("on") ||
                              includeStr.equalsIgnoreCase("1")));
        if (includeIterations)
        {
          OptimizingJob optimizingJob = getOptimizingJob(optimizingJobID);
          if (optimizingJob == null)
          {
            infoMessage.append("ERROR:  Unable to retrieve optimizing job " +
                               optimizingJobID + "<BR>" + EOL);
            handleViewOptimizing(requestInfo, true);
            return;
          }

          Job[] iterations = optimizingJob.getAssociatedJobs();
          if ((iterations != null) && (iterations.length > 0))
          {
            for (int i=0; i < iterations.length; i++)
            {
              try
              {
                configDB.removeJob(iterations[i].getJobID());
              }
              catch (Exception e)
              {
                infoMessage.append("Unable to remove optimizing job " +
                                   "iteration " + iterations[i].getJobID() +
                                   " -- " + e);
              }
            }
          }

          Job reRunIteration = optimizingJob.getReRunIteration();
          if (reRunIteration != null)
          {
            try
            {
              configDB.removeJob(reRunIteration.getJobID());
            }
            catch (Exception e)
            {
              infoMessage.append("Unable to remove optimizing job re-run " +
                                 "iteration " + reRunIteration.getJobID() +
                                 " -- " + e);
            }
          }
        }

        configDB.removeOptimizingJob(optimizingJobID);
        infoMessage.append("Successfully removed optimizing job " +
                           optimizingJobID +
                           " from the configuration directory.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Optimizing Job " + optimizingJobID +
                        " Removed</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Information about optimizing job " + optimizingJobID +
                        " has been removed from the configuration directory." +
                        EOL);
        htmlBody.append("<BR><BR>" + EOL);
        String link =
             generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                          Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                          "here");
        htmlBody.append("Click " + link + " to return to the list of " +
                        "optimizing jobs." + EOL);
        return;
      }
      catch (Exception e)
      {
        infoMessage.append("ERROR:  Unable to remove optimizing job " +
                           optimizingJobID + " from the configuration " +
                           "directory -- " + e + "<BR>" + EOL);
        generateViewOptimizingJobBody(requestInfo, optimizingJobID);
        return;
      }
    }
    else if ((confirmStr != null) && confirmStr.equalsIgnoreCase("No"))
    {
      infoMessage.append("Optimizing job " + optimizingJobID +
                         " was not removed from the configuration " +
                         "directory.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Delete Optimizing Job " + optimizingJobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to delete this job?" + EOL);
      htmlBody.append("<BR>");
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_DELETE_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
           Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS +
           "\" CHECKED>Delete all iterations of this optimizing job" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of editing the comments for an optimizing job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleEditOptimizingComments(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleMoveOptimizingJob()");

    // If the user doesn't have the schedule job permission, then they can't see
    // this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit optimizing job information.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID of the optimizing job to edit.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      infoMessage.append("ERROR:  No optimizing job specified to edit.<BR>" +
                         EOL);
      handleOptimizeJob(requestInfo);
      return;
    }


    // Get the optimizing job from the configuration database.
    OptimizingJob optimizingJob = null;
    try
    {
      optimizingJob = configDB.getOptimizingJob(optimizingJobID);
      if (optimizingJob == null)
      {
        infoMessage.append("ERROR:  Optimizing job " + optimizingJobID +
                           " was not found in the configuration database.<BR>" +
                           EOL);
        handleOptimizeJob(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  Could not get optimizing job " +
                         optimizingJobID +  " from the configuration " +
                         "database:  " + e + ".<BR>" + EOL);
      handleOptimizeJob(requestInfo);
      return;
    }


    // Determine whether the user submitted the form to update the comments.  If
    // so, then update the job.  Otherwise, display the form.
    String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if ((submitStr != null) && (submitStr.length() > 0))
    {
      String comments =
           request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);

      try
      {
        optimizingJob.setComments(comments);
        configDB.writeOptimizingJob(optimizingJob);

        // If the optimizing job is held in memory by the scheduler, then update
        // that copy as well.
        if ((optimizingJob = scheduler.getOptimizingJob(optimizingJobID)) !=
            null)
        {
          optimizingJob.setComments(comments);
        }

        infoMessage.append("Successfully updated the optimizing job " +
                           "comments.<BR>");
      }
      catch (Exception e)
      {
        infoMessage.append("ERROR:  Unable to update optimizing job " +
                           "comments:  " + e + ".<BR>" + EOL);
      }

      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else
    {
      String comments = optimizingJob.getComments();

      if (comments == null)
      {
        comments = "";
      }

      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Edit Comments for Optimizing Job " + optimizingJobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Edit the set of comments for the optimizing job and " +
                      "click the update button when finished." + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                      Constants.SERVLET_SECTION_JOB_EDIT_OPTIMIZING_COMMENTS) +
                      EOL);
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                  optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("      <TD>Job Comments</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COMMENTS + "\" ROWS=\"10\" " +
                      "COLS=\"80\">" + comments + "</TEXTAREA></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);


      htmlBody.append("<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" " +
                      "VALUE=\"Update\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of moving an optimizing job from one folder to another.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleMoveOptimizingJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleMoveOptimizingJob()");

    // If the user doesn't have manage folders permission, then they can't see
    // this
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "move job information.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job ID of the optimizing job to delete.
    String optimizingJobID =
         request.getParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if ((optimizingJobID == null) || (optimizingJobID.length() == 0))
    {
      infoMessage.append("ERROR:  No optimizing job specified to move.<BR>" +
                         EOL);
      handleOptimizeJob(requestInfo);
      return;
    }


    // Make sure that one or more folders exists in the configuration directory.
    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if ((folders == null) || (folders.length == 0))
    {
      infoMessage.append("ERROR:  No job folders have been defined in the " +
                         "configuration directory.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }


    // See if the user has provided confirmation for the move.  If not, then
    // request it.  If so, then take the appropriate action.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && confirmStr.equalsIgnoreCase("Move"))
    {
      try
      {
        String folderName =
             request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);

        boolean includeIterations = false;
        String includeStr =
             request.getParameter(
                  Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS);
        includeIterations = ((includeStr != null) &&
                             (includeStr.equalsIgnoreCase("true") ||
                              includeStr.equalsIgnoreCase("yes") ||
                              includeStr.equalsIgnoreCase("on") ||
                              includeStr.equalsIgnoreCase("1")));
        if (includeIterations)
        {
          OptimizingJob optimizingJob = getOptimizingJob(optimizingJobID);
          if (optimizingJob == null)
          {
            infoMessage.append("ERROR:  Unable to retrieve optimizing job " +
                               optimizingJobID + "<BR>" + EOL);
            handleViewOptimizing(requestInfo, true);
            return;
          }

          Job[] iterations = optimizingJob.getAssociatedJobs();
          if ((iterations != null) && (iterations.length > 0))
          {
            for (int i=0; i < iterations.length; i++)
            {
              try
              {
                configDB.moveJob(iterations[i].getJobID(), folderName);
              }
              catch (Exception e)
              {
                infoMessage.append("Unable to move optimizing job iteration " +
                                   iterations[i].getJobID() + " -- " + e);
              }
            }
          }

          Job reRunIteration = optimizingJob.getReRunIteration();
          if (reRunIteration != null)
          {
            try
            {
              configDB.moveJob(reRunIteration.getJobID(), folderName);
            }
            catch (Exception e)
            {
              infoMessage.append("Unable to move optimizing job re-run " +
                                 "iteration " + reRunIteration.getJobID() +
                                 " -- " + e);
            }
          }
        }


        configDB.moveOptimizingJob(optimizingJobID, folderName);
        infoMessage.append("Successfully moved optimizing job " +
                           optimizingJobID + ".<BR>" + EOL);
        generateViewOptimizingJobBody(requestInfo, optimizingJobID);
        return;
      }
      catch (Exception e)
      {
        infoMessage.append("ERROR:  Unable to move optimizing job " +
                           optimizingJobID + " -- " + e + "<BR>" + EOL);
        generateViewOptimizingJobBody(requestInfo, optimizingJobID);
        return;
      }
    }
    else if ((confirmStr != null) && confirmStr.equalsIgnoreCase("Cancel"))
    {
      infoMessage.append("Optimizing job " + optimizingJobID +
                         " was not moved.<BR>" + EOL);
      generateViewOptimizingJobBody(requestInfo, optimizingJobID);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Move Optimizing Job " + optimizingJobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MOVE_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                     optimizingJobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  Move to folder:" + EOL);
      htmlBody.append("  <SELECT NAME=\"" + Constants.SERVLET_PARAM_JOB_FOLDER +
                      "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        htmlBody.append("    <OPTION VALUE=\"" + folders[i].getFolderName() +
                        "\">" + folders[i].getFolderName() +  EOL);
      }
      htmlBody.append("  </SELECT>" + EOL);
      htmlBody.append("<BR>");

      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
           Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS +
           "\" CHECKED>Move all iterations of this optimizing job" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Move\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Cancel\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Retrieves the requested optimizing job from the scheduler if possible, or
   * from the config DB if necessary.
   *
   * @param  optimizingJobID  The ID of the optimizing job to retrieve.
   *
   * @return  The requested optimizing job, or {@code null} if the
   *          requested optimizing job does not exist.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             SLAMD database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the optimizing job information from the DB.
   *
   * @throws  SLAMDServerException  If some other problem occurs while
   *                                attempting to retrieve the optimizing job.
   */
  static OptimizingJob getOptimizingJob(String optimizingJobID)
         throws DatabaseException, DecodeException, SLAMDServerException
  {
    if (scheduler == null)
    {
      return configDB.getOptimizingJob(optimizingJobID);
    }
    else
    {
      return scheduler.getOptimizingJob(optimizingJobID);
    }
  }



  /**
   * Writes the specified message to the SLAMD server log with the appropriate
   * admin interface log level.  The request ID will be prepended to the
   * message.
   *
   * @param  requestInfo  The state information for this request.
   * @param  message      The message to be written to the SLAMD log file.
   */
  static void logMessage(RequestInfo requestInfo, String message)
  {
    if (slamdServer != null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ADMIN,
                             requestInfo.requestID + " - " + message);
    }

    if (requestInfo.debugInfo != null)
    {
      requestInfo.debugInfo.append("<!-- " + message + " -->" + EOL);
    }
  }
}
