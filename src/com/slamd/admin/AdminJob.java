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
package com.slamd.admin;



import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;

import com.sleepycat.je.DatabaseException;

import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Sequence;
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
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.MultiValuedParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.resourcemonitor.ResourceMonitor;
import com.slamd.server.ClientConnection;
import com.slamd.server.RealTimeJobStats;
import com.slamd.server.ResourceMonitorClientConnection;
import com.slamd.server.SLAMDServerException;
import com.slamd.server.UploadedFile;
import com.slamd.stat.ResourceMonitorStatTracker;
import com.slamd.stat.StatEncoder;
import com.slamd.stat.StatGrapher;
import com.slamd.stat.StatTracker;

import static com.unboundid.util.StaticUtils.secondsToHumanReadableDuration;

import static com.slamd.admin.AdminServlet.*;
import static com.slamd.admin.AdminUI.*;



/**
 * This class provides a set of methods for providing logic for managing jobs.
 */
public class AdminJob
{
  /**
   * Handle all processing related to viewing summary for information for a set
   * of jobs in a particular category (pending, running, or completed), or for
   * viewing detailed information about a specific job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJob(RequestInfo requestInfo)
  {
    String subsection = requestInfo.subsection;
    String folderName = null;
    String jobID = null;
    String[] jobIDValues =
         requestInfo.request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);

    if ((jobIDValues != null) && (jobIDValues.length == 1))
    {
      jobID = jobIDValues[0];
    }

    handleViewJob(requestInfo, subsection, folderName, jobID);
  }



  /**
   * Handle all processing related to viewing summary for information for a set
   * of jobs in the specified category, or for viewing detailed information
   * about a specific job.
   *
   * @param  requestInfo  The state information for this request.
   * @param  subsection   The subsection that specifies which category of jobs
   *                      to view.
   * @param  folderName   The name of the folder to display.
   * @param  jobID        The job ID of the job to display.
   */
  static void handleViewJob(RequestInfo requestInfo, String subsection,
                            String folderName, String jobID)
  {
    logMessage(requestInfo, "In handleViewJob()");

    // The user must have at least view job permission to access anything in
    // this section.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    if (subsection == null)
    {
      subsection = "";
    }


    // Get the current folder name because it may be needed later.
    if ((folderName == null) || (folderName.length() == 0))
    {
      folderName = request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if ((folderName == null) || (folderName.length() == 0))
      {
        folderName = Constants.FOLDER_NAME_UNCLASSIFIED;
      }
    }
    String vFolderName =
         request.getParameter(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER);


    // See if a job ID has been specified.  If so, then display that job.
    if ((jobID != null) && (jobID.length() > 0))
    {
      // The user wants to view a specific job, so retrieve it.
      Job job = null;
      try
      {
        if (slamdRunning && (! readOnlyMode))
        {
          job = scheduler.getJob(jobID);
        }
        else
        {
          job = configDB.getJob(jobID);
        }

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">View Job " + jobID + "</SPAN>" + EOL);
      }
      catch (Exception e)
      {
        infoMessage.append("Error retrieving job " + jobID + ":  " +
                           e.getMessage() + "<BR>" + EOL);
      }

      if (job == null)
      {
        htmlBody.append("<BR><BR>");
        htmlBody.append("No information is available for job " + jobID + '.' +
                        EOL);
      }
      else
      {
        generateViewJobBody(requestInfo, job);
      }
    }
    else
    {
      // No specific job was specified, so determine which category to use.
      String category = null;
      Job[] jobs = null;

      if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL))
      {
        if ((vFolderName == null) || (vFolderName.length() == 0))
        {
          handleViewVirtualFolderList(requestInfo);
        }
        else
        {
          handleViewVirtualFolder(requestInfo, vFolderName);
        }
        return;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_PENDING))
      {
        category = "pending";
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">View Pending Jobs</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        htmlBody.append("<TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       subsection) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" +
                        EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("</TABLE>" + EOL);

        jobs = scheduler.getPendingJobs();
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_RUNNING))
      {
        category = "running";
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">View Running Jobs</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       subsection) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        jobs = scheduler.getRunningJobs();
      }
      else
      {
        category = "completed";
        try
        {
          jobs = configDB.getCompletedSummaryJobs(folderName);
          if (jobs == null)
          {
            jobs = new Job[0];
            infoMessage.append("Unknown folder \"" + folderName + '"');
          }

          if (hideOptimizingIterations)
          {
            ArrayList<Job> jobList = new ArrayList<Job>(jobs.length);
            for (int i=0; i < jobs.length; i++)
            {
              if (jobs[i].getOptimizingJobID() == null)
              {
                jobList.add(jobs[i]);
              }
            }

            jobs = new Job[jobList.size()];
            jobList.toArray(jobs);
          }
        }
        catch (Exception e)
        {
          infoMessage.append("Unable to retrieve job information:  " +
                             e.getMessage() + "<BR>" + EOL);
        }

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">View Completed Jobs</SPAN>" + EOL);
        htmlBody.append("<BR>" + EOL);

        String link = generateLink(requestInfo,
                           Constants.SERVLET_SECTION_JOB,
                           Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                           Constants.SERVLET_PARAM_JOB_FOLDER, folderName,
                           "Switch to optimizing jobs for this folder");
        htmlBody.append(link + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        if ((folderName == null) || (folderName.length() == 0))
        {
          folderName = Constants.FOLDER_NAME_UNCLASSIFIED;
        }

        try
        {
          JobFolder folder = null;
          JobFolder[] folders = configDB.getFolders();
          if (folders == null)
          {
            folders = new JobFolder[0];
          }

          if ((folders != null) && (folders.length > 0))
          {
            htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                            "\">" + EOL);
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_SECTION,
                                           Constants.SERVLET_SECTION_JOB) +
                            EOL);
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                           subsection) + EOL);
            if (requestInfo.debugHTML)
            {
              htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                             "1") + EOL);
            }
            htmlBody.append("  View jobs in folder " + EOL);
            htmlBody.append("  <SELECT NAME=\"" +
                            Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
            for (int i=0; i < folders.length; i++)
            {
              String selectedStr = "";
              if (folderName.equalsIgnoreCase(folders[i].getFolderName()))
              {
                folder = folders[i];
                selectedStr = " SELECTED";
              }

              htmlBody.append("    <OPTION VALUE=\"" +
                              folders[i].getFolderName() + '"' + selectedStr +
                              '>' + folders[i].getFolderName() + EOL);
            }

            htmlBody.append("  </SELECT>" + EOL);
            htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" +
                            EOL);
            htmlBody.append("</FORM>" + EOL);
            htmlBody.append("<BR>" + EOL);

            if (folder != null)
            {
              htmlBody.append("<B>Jobs for Folder " + folder.getFolderName() +
                              "</B>" + EOL);
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
                          Constants.SERVLET_SECTION_JOB_FOLDER_DESCRIPTION) +
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
                            Constants.SERVLET_SECTION_JOB_FOLDER_PUBLISH) +
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
                            Constants.SERVLET_SECTION_JOB_FOLDER_PUBLISH) +
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
          }
        }
        catch (DatabaseException de)
        {
          infoMessage.append("ERROR:  Unable to obtain a list of the job " +
                             "folders defined in the configuration " +
                             "database -- " + de);
          return;
        }
      }


      if ((jobs == null) || (jobs.length == 0))
      {
        if (category.equals("completed"))
        {
          if ((folderName == null) || (folderName.length() == 0))
          {
            folderName = Constants.FOLDER_NAME_UNCLASSIFIED;
          }
          htmlBody.append("There are currently no " + category +
                          " jobs in the " + folderName +
                          " folder.<BR><BR>" + EOL);
        }
        else
        {
          htmlBody.append("There are currently no " + category +
                          " jobs.<BR><BR>" + EOL);
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
                                       Constants.SERVLET_SECTION_JOB_MASS_OP) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                       subsection) + EOL);
        String jobFolder =
             request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
        if ((jobFolder != null) && (jobFolder.length() > 0))
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                         jobFolder) + EOL);
        }
        else
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                         Constants.FOLDER_NAME_UNCLASSIFIED) +
                          EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
        htmlBody.append("    <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A +"\">" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><B>Job ID</B></TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><B>Description</B></TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><B>Job Type</B></TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);

        if (category.equals("running") || category.equals("completed"))
        {
          htmlBody.append("      <TD><B>Actual Start Time</B></TD>" + EOL);
        }
        else
        {
          htmlBody.append("      <TD><B>Start Time</B></TD>" + EOL);
        }

        if (category.equals("completed"))
        {
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD><B>Actual Duration</B></TD>" + EOL);

          if (enableReadOnlyManagement && (! readOnlyMode))
          {
            htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("      <TD><B>Read-Only Status</B></TD>" +
                 EOL);
          }
        }

        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><B>Current State</B></TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);

        boolean selectAll   = false;
        boolean deselectAll = false;

        String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
        if (submitStr != null)
        {
          if (submitStr.equals(Constants.SUBMIT_STRING_SELECT_ALL))
          {
            selectAll = true;
          }
          else if (submitStr.equals(Constants.SUBMIT_STRING_DESELECT_ALL))
          {
            deselectAll = true;
          }
        }

        String[] selectedJobIDs =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
        if (selectedJobIDs == null)
        {
          selectedJobIDs = new String[0];
        }


        for (int i=0; i < jobs.length; i++)
        {
          if (i % 2 == 0)
          {
            htmlBody.append("    <TR CLASS=\"" +
                            Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          }
          else
          {
            htmlBody.append("    <TR CLASS=\"" +
                            Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          }

          String description = (jobs[i].getJobDescription() == null)
                                    ? ""
                                    : jobs[i].getJobDescription();
          String stateStr;
          switch (jobs[i].getJobState())
          {
            case Constants.JOB_STATE_NOT_YET_STARTED:
              stateStr = "Pending";
              break;
            case Constants.JOB_STATE_DISABLED:
              stateStr = "Disabled";
              break;
            case Constants.JOB_STATE_RUNNING:
              stateStr = "Running";
              break;
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
            default:
              stateStr = "Stopped";
              break;
          }

          boolean selected = false;
          if (selectAll)
          {
            selected = true;
          }
          else if (! deselectAll)
          {
            for (int j=0; j < selectedJobIDs.length; j++)
            {
              if (selectedJobIDs[j].equals(jobs[i].getJobID()))
              {
                selected = true;
                break;
              }
            }
          }

          String fParam = null;
          String fValue = null;
          if ((vFolderName != null) && (vFolderName.length() > 0))
          {
            fParam = Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER;
            fValue = vFolderName;
          }
          else if ((folderName != null) && (folderName.length() > 0))
          {
            fParam = Constants.SERVLET_PARAM_JOB_FOLDER;
            fValue = folderName;
          }

          String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                     Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                     Constants.SERVLET_PARAM_JOB_ID,
                                     jobs[i].getJobID(), fParam, fValue,
                                     jobs[i].getJobID());
          htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                               Constants.SERVLET_PARAM_JOB_ID + "\" VALUE=\"" +
                               jobs[i].getJobID() + '"' +
                               (selected ? " CHECKED" : "") + "></TD>" + EOL);
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD>" + link + "</TD>" + EOL);
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD>" + description + "</TD>" + EOL);
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD>" + jobs[i].getJobName() + "</TD>" + EOL);
          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);

          if (category.equals("running") || category.equals("completed"))
          {
            Date startTime = jobs[i].getActualStartTime();
            if (startTime == null)
            {
              startTime = jobs[i].getStartTime();
            }
            htmlBody.append("      <TD>" +
                            displayDateFormat.format(startTime) +
                            "</TD>" + EOL);
          }
          else
          {
            htmlBody.append("      <TD>" +
                            displayDateFormat.format(jobs[i].getStartTime()) +
                            "</TD>" + EOL);
          }

          if (category.equals("completed"))
          {
            htmlBody.append("      <TD>&nbsp;</TD>" + EOL);

            final int actualDuration = jobs[i].getActualDuration();
            if (actualDuration < 0)
            {
              htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
            }
            else
            {
              htmlBody.append("      <TD>" +
                   secondsToHumanReadableDuration(actualDuration) +
                   "</TD>" + EOL);
            }
          }

          if (category.equals("completed") && enableReadOnlyManagement &&
              (! readOnlyMode))
          {
            htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
            if (jobs[i].displayInReadOnlyMode())
            {
              htmlBody.append("      <TD>Published</TD>" +  EOL);
            }
            else
            {
              htmlBody.append("      <TD>Not Published</TD>" +  EOL);
            }
          }

          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD>" + stateStr + "</TD>" +
                          EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("  </TABLE>" + EOL);
        htmlBody.append("  <BR><BR>" + EOL);

        ArrayList<String> actions = new ArrayList<String>();
        actions.add(Constants.SUBMIT_STRING_SELECT_ALL);
        actions.add(Constants.SUBMIT_STRING_DESELECT_ALL);
        if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_PENDING))
        {
          if (requestInfo.mayCancelJob)
          {
            actions.add(Constants.SUBMIT_STRING_CANCEL);

            if (requestInfo.mayDeleteJob)
            {
              actions.add(Constants.SUBMIT_STRING_CANCEL_AND_DELETE);
            }
          }

          if (requestInfo.mayScheduleJob)
          {
            actions.add(Constants.SUBMIT_STRING_CLONE);
            actions.add(Constants.SUBMIT_STRING_DISABLE);
            actions.add(Constants.SUBMIT_STRING_ENABLE);
          }
        }
        else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_RUNNING))
        {
          if (requestInfo.mayCancelJob)
          {
            actions.add(Constants.SUBMIT_STRING_CANCEL);
          }

          if (requestInfo.mayScheduleJob)
          {
            actions.add(Constants.SUBMIT_STRING_CLONE);
          }
        }
        else
        {
          if (requestInfo.mayScheduleJob)
          {
            actions.add(Constants.SUBMIT_STRING_CLONE);
          }

          if (requestInfo.mayViewJob && (! disableGraphs))
          {
            actions.add(Constants.SUBMIT_STRING_COMPARE);
          }

          if (requestInfo.mayDeleteJob)
          {
            actions.add(Constants.SUBMIT_STRING_DELETE);
          }

          if (requestInfo.mayExportJobData)
          {
            actions.add(Constants.SUBMIT_STRING_EXPORT);
          }

          if (requestInfo.mayManageFolders)
          {
            actions.add(Constants.SUBMIT_STRING_CREATE_FOLDER);

            try
            {
              JobFolder[] folders = configDB.getFolders();
              if ((folders != null) && (folders.length > 0))
              {
                actions.add(Constants.SUBMIT_STRING_DELETE_FOLDER);
                actions.add(Constants.SUBMIT_STRING_MOVE);
              }
            }
            catch (DatabaseException de) {}
          }
        }

        if (requestInfo.mayManageFolders)
        {
          actions.add(Constants.SUBMIT_STRING_ADD_TO_VIRTUAL_FOLDER);
        }

        if (requestInfo.mayManageFolders && enableReadOnlyManagement &&
            subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED))
        {
          actions.add(Constants.SUBMIT_STRING_PUBLISH_JOBS);
          actions.add(Constants.SUBMIT_STRING_DEPUBLISH_JOBS);
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

          if (((i % 5) == 4) && ((i+1) < actions.size()))
          {
            htmlBody.append("    </TR>" + EOL);
            htmlBody.append("    <TR>" + EOL);
          }
        }

        if ((actions.size() % 5) != 0)
        {
          for (int i=0; i < (5 - (actions.size() % 5)); i++)
          {
            htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          }
        }

        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("  </TABLE>" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }


      // The remainder of this section deals with file uploads.  If the file
      // upload capability has been disabled, then don't add that content to
      // the page.  Note that it is also only available for completed jobs, so
      // it should not be displayed for pending or running jobs.
      if (disableUploads || (! category.equals("completed")))
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
  }



  /**
   * Handles the work of retrieving information about the specified job as a
   * plain text document.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJobAsText(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewJobAsText()");


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
    HttpServletRequest  request  = requestInfo.request;
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


    // Get the job ID of the job to retrieve.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      writer.println("ERROR:  No job ID specified in the request.");
      return;
    }


    // Get the job for the requested job ID.
    Job job;
    try
    {
      job = configDB.getJob(jobID);
      if (job == null)
      {
        if (onlyState)
        {
          writer.println(Constants.JOB_STATE_NO_SUCH_JOB);
        }
        else
        {
          writer.println("ERROR:  Could not retrieve job \"" + jobID + '"');
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
        writer.println("ERROR:  Could not retrieve job \"" + jobID + '"');
      }
      return;
    }


    // Determine whether we should only return the job state.
    if (onlyState)
    {
      writer.println(job.getJobState());
      return;
    }


    // Determine whether the resulting information should include detailed
    // statistics.
    boolean viewDetailedStats = false;
    String detailStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_VIEW_DETAILED_STATS);
    if (detailStr != null)
    {
      viewDetailedStats = (detailStr.equalsIgnoreCase("true") ||
                           detailStr.equalsIgnoreCase("yes") ||
                           detailStr.equalsIgnoreCase("on") ||
                           detailStr.equalsIgnoreCase("1"));
    }

    writer.println(job.getJobClass().getJobName() + " Job " + job.getJobID());
    writer.println("Job ID:  " + job.getJobID());

    String description = job.getJobDescription();
    if ((description != null) && (description.length() > 0))
    {
      writer.println("Job Description:  " + description);
    }

    String optimizingJobID = job.getOptimizingJobID();
    if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
    {
      writer.println("Optimizing Job ID:  " + optimizingJobID);
    }

    writer.println("Job Type:  " + job.getJobClass().getJobName());
    writer.println("Job Class:  " + job.getJobClassName());
    writer.println("Job State:  " + job.getJobState() + " (" +
                   Constants.jobStateToString(job.getJobState()) + ')');


    writer.println("----- Job Schedule Information -----");
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
    writer.println("Scheduled Start Time:  " + startTimeStr);

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
    writer.println("Scheduled Stop Time:  " + stopTimeStr);

    int scheduledDuration = job.getDuration();
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

    int numClients = job.getNumberOfClients();
    writer.println("Number of Clients:  " + numClients);

    String[] requestedClients = job.getRequestedClients();
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

    int numThreads = job.getThreadsPerClient();
    writer.println("Number of Threads per Client:  " + numThreads);

    int startupDelay = job.getThreadStartupDelay();
    writer.println("Thread Startup Delay:  " + startupDelay);

    int collectionInterval = job.getCollectionInterval();
    writer.println("Statistics Collection Interval:  " +
         secondsToHumanReadableDuration(collectionInterval));


    // Write information to the buffer about the job-specific configuration.
    writer.println("----- Job-Specific Configuration -----");
    Parameter[] params = job.getParameterList().getParameters();
    for (int i=0; i < params.length; i++)
    {
      writer.println(params[i].getDisplayName() + ":  " +
                     params[i].getDisplayValue());
    }


    // Write information to the buffer about the job statistics.
    if (job.hasStats())
    {
      writer.println("----- General Statistical Information -----");

      Date actualStartTime = job.getActualStartTime();
      if (actualStartTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(actualStartTime);
      }
      writer.println("Actual Start Time:  " + startTimeStr);

      Date actualStopTime = job.getActualStopTime();
      if (actualStopTime == null)
      {
        stopTimeStr = "(Not Available)";
      }
      else
      {
        stopTimeStr = dateFormat.format(actualStopTime);
      }
      writer.println("Actual Stop Time:  " + stopTimeStr);

      int actualDuration = job.getActualDuration();
      if (actualDuration > 0)
      {
        durationStr = secondsToHumanReadableDuration(actualDuration);
      }
      else
      {
        durationStr = "(Not Available)";
      }
      writer.println("Actual Duration:  " + durationStr);

      String[] clients = job.getStatTrackerClientIDs();
      if ((clients != null) && (clients.length > 0))
      {
        writer.print("Clients Used:  " + clients[0]);
        for (int i=1; i < clients.length; i++)
        {
          writer.println(",");
          writer.print("               " + clients[i]);
        }
        writer.println();
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

        writer.println("----- " + statNames[i] + " Statistics -----");
        if (viewDetailedStats)
        {
          writer.println(tracker.getDetailString());
        }
        else
        {
          writer.println(tracker.getSummaryString());
        }
      }
    }


    // Write information to the buffer about the resource monitor statistics.
    if (job.hasResourceStats())
    {
      writer.println("----- Resource Monitor Statistics -----");
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

        writer.println("----- " + statNames[i] +
                      " Resource Monitor Statistics -----");
        if (viewDetailedStats)
        {
          writer.println(tracker.getDetailString());
        }
        else
        {
          writer.println(tracker.getSummaryString());
        }
      }
    }
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
    HttpServletRequest  request  = requestInfo.request;
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
   * Generate the HTML code that can be used to display detailed information
   * about a particular job.
   *
   * @param  requestInfo  The state information for this request.
   * @param  job          The job whose information is to be displayed.
   */
  static void generateViewJobBody(RequestInfo requestInfo, Job job)
  {
    logMessage(requestInfo, "In generateViewJobBody(" + job.getJobID() + ')');

    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    int     buttonsAdded = 0;
    boolean isUnknownJob = (job.getJobClass() instanceof UnknownJobClass);

    htmlBody.append("<TABLE BORDER=\"0\" WIDTH=\"100%\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          job.getJobID()) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
    htmlBody.append("</FORM>" + EOL);
    htmlBody.append("    </TD>" + EOL);
    buttonsAdded++;
    if ((buttonsAdded % 5) == 0)
    {
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
    }

    if (requestInfo.mayScheduleJob)
    {
      if ((job.getJobState() == Constants.JOB_STATE_NOT_YET_STARTED) ||
          (job.getJobState() == Constants.JOB_STATE_DISABLED))
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_EDIT) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }

        if (job.getJobState() == Constants.JOB_STATE_NOT_YET_STARTED)
        {
          htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                                Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_DISABLE) +
                          EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                                job.getJobID()) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Disable\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          buttonsAdded++;
          if ((buttonsAdded % 5) == 0)
          {
            htmlBody.append("  </TR>" + EOL);
            htmlBody.append("  <TR>" + EOL);
          }
        }
        else
        {
          htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                                Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                         Constants.SERVLET_SECTION_JOB_ENABLE) +
                          EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                                job.getJobID()) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Enable\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
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
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_COMMENTS) +
                             EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit Comments\">" +
                        EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      if (! isUnknownJob)
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_CLONE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Clone\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }
    }

    if (job.doneRunning())
    {
      if (requestInfo.mayScheduleJob)
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        '"' + blankTarget + '>' + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_IMPORT_PERSISTENT) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Import Persistent Stats\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      if ((! disableGraphs) && (job.graphsAvailable()))
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        '"' + blankTarget + '>' + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_VIEW_GRAPH) + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Graph\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
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
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_DELETE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Delete\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      if (requestInfo.mayManageFolders)
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_MASS_OP) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_MOVE + "\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }

        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_MASS_OP) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_ADD_TO_VIRTUAL_FOLDER + "\">" +
                        EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      StatTracker[] stubs = job.getJobClass().getStatTrackerStubs("", "", 1);
      if (requestInfo.mayScheduleJob && (! isUnknownJob) && (stubs != null) &&
          (stubs.length > 0))
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_OPTIMIZE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Optimize Results\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
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
      if ((job.getJobState() == Constants.JOB_STATE_RUNNING) &&
          job.hasRealTimeStats() && (! disableGraphs))
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        '"' + blankTarget + '>' + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                        Constants.SERVLET_SECTION_JOB_VIEW_GRAPH_REAL_TIME) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"In-Progress Results\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }
      }

      if (requestInfo.mayCancelJob)
      {
        htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_CANCEL) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              job.getJobID()) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        buttonsAdded++;
        if ((buttonsAdded % 5) == 0)
        {
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
        }


        if (requestInfo.mayDeleteJob &&
            scheduler.isJobPending(job.getJobID()) &&
            (job.getOptimizingJobID() == null))
        {
          htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                                Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_CANCEL_AND_DELETE) +
                          EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                                job.getJobID()) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel and " +
                          "Delete\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          buttonsAdded++;
          if ((buttonsAdded % 5) == 0)
          {
            htmlBody.append("  </TR>" + EOL);
            htmlBody.append("  <TR>" + EOL);
          }
        }
      }
    }

    if ((reportGenerators != null) && (reportGenerators.length > 0))
    {
      htmlBody.append("    <TD ALIGN=\"CENTER\">" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                      Constants.SERVLET_SECTION_JOB_GENERATE_REPORT) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                            job.getJobID()) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Generate Report\">" +
                      EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("    </TD>" + EOL);
      buttonsAdded++;
    }

    while ((buttonsAdded % 5) != 0)
    {
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      buttonsAdded++;
    }

    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("</TABLE>" + EOL);


    // See if a real or virtual folder name has been specified as a parameter.
    // If so, then provide a link to return to that folder.  Start with the
    // virtual folder first.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER);
    if ((folderName != null) && (folderName.length() > 0))
    {
      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL,
                                 Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                                 folderName, folderName);
      htmlBody.append("Return to virtual job folder " + link + '.' + EOL);
    }
    else
    {
      // No virtual folder, so try a real folder.
      folderName = request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if ((folderName == null) || (folderName.length() == 0))
      {
        folderName = job.getFolderName();
      }

      if ((folderName != null) && (folderName.length() > 0))
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                                   Constants.SERVLET_PARAM_JOB_FOLDER,
                                   folderName, folderName);
        htmlBody.append("Return to job folder " + link + '.' + EOL);
      }
      else
      {
        // No real folder, so display either the completed, running, or pending
        // jobs.
        if (job.doneRunning())
        {
          String link =
               generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                            Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                            "completed jobs");
          htmlBody.append("Return to the " + link + " page." + EOL);
        }
        else if (job.getJobState() == Constants.JOB_STATE_RUNNING)
        {
          String link =
               generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                            Constants.SERVLET_SECTION_JOB_VIEW_RUNNING,
                            "running jobs");
          htmlBody.append("Return to the " + link + " page." + EOL);
        }
        else
        {
          String link =
               generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                            Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                            "pending jobs");
          htmlBody.append("Return to the " + link + " page." + EOL);
        }
      }
    }


    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>General " +
                    "Information</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job ID
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Job ID</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getJobID() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The optimizing job ID
    String optimizingJobID = job.getOptimizingJobID();
    if (optimizingJobID == null)
    {
      optimizingJobID = "(none specified)";
    }
    else
    {
      optimizingJobID =
           generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                        Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                        Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                        optimizingJobID, optimizingJobID);
    }
    htmlBody.append("      <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Optimizing Job ID</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJobID + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job group name
    String jobGroupName = job.getJobGroup();
    if (jobGroupName == null)
    {
      jobGroupName = "(none specified)";
    }
    else
    {
      jobGroupName = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                          Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                          Constants.SERVLET_PARAM_JOB_GROUP_NAME, jobGroupName,
                          jobGroupName);
    }
    htmlBody.append("      <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Job Group</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + jobGroupName + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job description
    String descriptionStr = job.getJobDescription();
    if (descriptionStr == null)
    {
      descriptionStr = "(not specified)";
    }
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Job Description</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + descriptionStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job type
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Job Type</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getJobName() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job class
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Job Class</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    if (requestInfo.mayViewJobClass)
    {
      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_CLASSES,
                                 Constants.SERVLET_PARAM_JOB_CLASS,
                                 job.getJobClassName(),
                                 job.getJobClassName());
      htmlBody.append("    <TD>" + link + "</TD>" + EOL);
    }
    else
    {
      htmlBody.append("    <TD>" + job.getJobClassName() + "</TD>" + EOL);
    }
    htmlBody.append("  </TR>" + EOL);

    // The current job state
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Current State</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getJobStateString() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // If the job is currently pending, try to figure out why it hasn't started
    // running.
    if ((scheduler != null) && scheduler.isJobPending(job.getJobID()))
    {
      String pendingReason = scheduler.getPendingReason(job.getJobID());
      if (pendingReason != null)
      {
        htmlBody.append("        <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        htmlBody.append("    <TD>Pending Reason</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + pendingReason + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    // If the job is running, try to figure out what clients it is using and
    // how long it might have left.
    if (job.getJobState() == Constants.JOB_STATE_RUNNING)
    {
      int styleValue = 0;

      ArrayList clientList = job.getActiveClients();
      if ((clientList != null) && (! clientList.isEmpty()))
      {
        String style = ((styleValue++ % 2) == 0)
                       ?  Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        htmlBody.append("        <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>Active Clients</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);

        for (int i=0; i < clientList.size(); i++)
        {
          ClientConnection conn = (ClientConnection) clientList.get(i);
          htmlBody.append("      " + conn.getClientID() + "<BR>" + EOL);
        }

        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }

      ArrayList monitorClientList = job.getActiveMonitorClients();
      if ((monitorClientList != null) && (! monitorClientList.isEmpty()))
      {
        String style = ((styleValue++ % 2) == 0)
                       ?  Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        htmlBody.append("        <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>Active Monitor Clients</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);

        for (int i=0; i < monitorClientList.size(); i++)
        {
          ResourceMonitorClientConnection conn =
               (ResourceMonitorClientConnection) monitorClientList.get(i);
          htmlBody.append("      " + conn.getClientID() + "<BR>" + EOL);
        }

        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }

      Date startDate = job.getActualStartTime();
      if (startDate != null)
      {
        long startTime = startDate.getTime();

        long shouldStopTime = -1;
        if (job.getDuration() > 0)
        {
          shouldStopTime = startTime + (job.getDuration() * 1000);
        }

        Date stopDate = job.getStopTime();
        if (stopDate != null)
        {
          long tmpShouldStopTime = stopDate.getTime();
          if ((shouldStopTime > 0) && (tmpShouldStopTime < shouldStopTime))
          {
            shouldStopTime = tmpShouldStopTime;
          }
        }

        if (shouldStopTime > 0)
        {
          Date shouldStopDate = new Date(shouldStopTime);
          String stopTimeStr = displayDateFormat.format(shouldStopDate);

          String style = ((styleValue++ % 2) == 0)
                         ?  Constants.STYLE_JOB_SUMMARY_LINE_A
                         : Constants.STYLE_JOB_SUMMARY_LINE_B;

          htmlBody.append("        <TR CLASS=\"" + style + "\">" + EOL);
          htmlBody.append("    <TD>Estimated Completion Time</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + stopTimeStr + "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);

          long timeLeft = shouldStopTime - System.currentTimeMillis();
          if (timeLeft > 0)
          {
            timeLeft /= 1000;
            String remainingStr = "";

            long numDays = timeLeft / 86400;
            if (numDays > 0)
            {
              timeLeft %= 86400;
              remainingStr += numDays + " day(s) ";
            }

            long numHours = timeLeft / 3600;
            if ((remainingStr.length() > 0) || (numHours > 0))
            {
              timeLeft %= 3600;
              remainingStr += numHours + " hour(s) ";
            }

            long numMinutes = timeLeft / 60;
            if ((remainingStr.length() > 0) || (numMinutes > 0))
            {
              timeLeft %= 60;
              remainingStr += numMinutes + " minute(s) ";
            }

            remainingStr += timeLeft + " second(s)";

            style = ((styleValue++ % 2) == 0)
                    ?  Constants.STYLE_JOB_SUMMARY_LINE_A
                    : Constants.STYLE_JOB_SUMMARY_LINE_B;

            htmlBody.append("        <TR CLASS=\"" + style + "\">" + EOL);
            htmlBody.append("    <TD>Estimated Time Remaining</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + remainingStr + "</TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
        }
      }
    }

    // Job execution data
    if (job.doneRunning())
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Job Execution " +
                      "Data</B></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // The actual start time
      Date actualStartTime = job.getActualStartTime();
      String actualStartTimeStr = "(not available)";
      if (actualStartTime != null)
      {
        actualStartTimeStr = displayDateFormat.format(actualStartTime);
      }
      htmlBody.append("        <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Actual Start Time</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + actualStartTimeStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // The actual stop time
      Date actualStopTime = job.getActualStopTime();
      String actualStopTimeStr = "(not available)";
      if (actualStopTime != null)
      {
        actualStopTimeStr = displayDateFormat.format(actualStopTime);
      }
      htmlBody.append("        <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
      htmlBody.append("    <TD>Actual Stop Time</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + actualStopTimeStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // The actual duration
      int actualDuration = job.getActualDuration();
      String actualDurationStr;
      if (actualDuration > 0)
      {
        actualDurationStr = secondsToHumanReadableDuration(actualDuration);
      }
      else
      {
        actualDurationStr = "(not available)";
      }
      htmlBody.append("        <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Actual Duration</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + actualDurationStr + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      if (job.hasStats())
      {
        // The stat trackers
        String[] trackerNames = job.getStatTrackerNames();
        for (int i=0; i < trackerNames.length; i++)
        {
          StatTracker[] statTrackers = job.getStatTrackers(trackerNames[i]);
          if (statTrackers.length > 0)
          {
            String valueStr = "(no data available)";
            try
            {
              StatTracker tracker = statTrackers[0].newInstance();
              tracker.aggregate(statTrackers);
              valueStr = tracker.getSummaryHTML();
            }
            catch (Exception e)
            {
              e.printStackTrace();
              infoMessage.append("ERROR creating aggregate tracker " +
                                 trackerNames[i] + ":  " + e + "<BR>" + EOL);
            }


            if (i % 2 == 1)
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }

            htmlBody.append("    <TD>" + trackerNames[i] + "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + valueStr + "</TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
          else
          {
            if (i % 2 == 1)
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }
            htmlBody.append("    <TD>" + trackerNames[i] + "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>(no data available)</TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
        }
      }

      if (job.hasResourceStats())
      {
        // The stat trackers
        String[] trackerNames = job.getResourceStatTrackerNames();
        for (int i=0; i < trackerNames.length; i++)
        {
          StatTracker[] statTrackers =
               job.getResourceStatTrackers(trackerNames[i]);
          if (statTrackers.length > 0)
          {
            String valueStr = "(no data available)";
            try
            {
              StatTracker tracker = statTrackers[0].newInstance();
              tracker.aggregate(statTrackers);
              valueStr = tracker.getSummaryHTML();
            }
            catch (Exception e)
            {
              e.printStackTrace();
              infoMessage.append("ERROR creating aggregate tracker " +
                                 trackerNames[i] + ":  " + e + "<BR>" + EOL);
            }


            if (i % 2 == 1)
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }

            htmlBody.append("    <TD>" + trackerNames[i] + "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + valueStr + "</TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
          else
          {
            if (i % 2 == 1)
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }
            htmlBody.append("    <TD>" + trackerNames[i] + "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>(no data available)</TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
        }
      }

      if (job.hasStats() || job.hasResourceStats())
      {
          ArrayList<String> subsectionList = new ArrayList<String>();
          ArrayList<String> nameList = new ArrayList<String>();
          ArrayList<String> targetList = new ArrayList<String>();

          subsectionList.add(Constants.SERVLET_SECTION_JOB_VIEW_STATS);
          nameList.add("Detailed Statistics");
          targetList.add(" TARGET=\"_BLANK\"");

          if ((! disableGraphs) && job.graphsAvailable())
          {
            subsectionList.add(Constants.SERVLET_SECTION_JOB_VIEW_GRAPH);
            nameList.add("Graph Statistics");
            targetList.add(blankTarget);

            subsectionList.add(Constants.SERVLET_SECTION_JOB_VIEW_OVERLAY);
            nameList.add("Graph Overlaid Statistics");
            targetList.add(blankTarget);
          }

          if ((! disableGraphs) && (job.resourceGraphsAvailable()))
          {
            subsectionList.add(
                 Constants.SERVLET_SECTION_JOB_VIEW_MONITOR_GRAPH);
            nameList.add("Graph Resource Statistics");
            targetList.add(blankTarget);
          }

          subsectionList.add(Constants.SERVLET_SECTION_JOB_SAVE_STATS);
          nameList.add("Save Statistics");
          targetList.add(" TARGET=\"_BLANK\"");

          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
          htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
          htmlBody.append("        <TR>" + EOL);

          for (int i=0; i < subsectionList.size(); i++)
          {
            String subsec     = subsectionList.get(i);
            String subsecName = nameList.get(i);
            String target     = targetList.get(i);

            htmlBody.append("          <TD>" + EOL);
            htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                            servletBaseURI + '"' + target + '>' + EOL);
            htmlBody.append("              " +
                            generateHidden(Constants.SERVLET_PARAM_SECTION,
                                           Constants.SERVLET_SECTION_JOB) +
                            EOL);
            htmlBody.append("              " +
                            generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                           subsec) + EOL);
            htmlBody.append("              " +
                            generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                           job.getJobID()) + EOL);

            if (requestInfo.debugHTML)
            {
              htmlBody.append("              " +
                              generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                             "1") + EOL);
            }

            htmlBody.append("              <INPUT TYPE=\"SUBMIT\" VALUE=\"" +
                            subsecName + "\">" + EOL);
            htmlBody.append("            </FORM>" + EOL);
            htmlBody.append("          </TD>" + EOL);
          }

          htmlBody.append("        </TR>" + EOL);
          htmlBody.append("      </TABLE>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>");
      }
    }

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Schedule " +
                    "Information</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The scheduled start time
    Date startTime = job.getStartTime();
    String startTimeStr = "(not specified)";
    if (startTime != null)
    {
      startTimeStr = displayDateFormat.format(startTime);
    }
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Scheduled Start Time</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + startTimeStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The scheduled stop time
    Date stopTime = job.getStopTime();
    String stopTimeStr = "(not specified)";
    if (stopTime != null)
    {
      stopTimeStr = displayDateFormat.format(stopTime);
    }
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Scheduled Stop Time</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + stopTimeStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The scheduled duration
    String durationStr = (job.getDuration() > 0)
         ? secondsToHumanReadableDuration(job.getDuration())
         : "(not specified)";
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Scheduled Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + durationStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of clients
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Number of Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getNumberOfClients() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The indication of whether to wait for clients
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Wait for Available Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + (job.waitForClients() ? "true" : "false") +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    if (! (readOnlyMode && hideSensitiveInformation))
    {
      // The set of clients to use
      String[] requestedClients = job.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        htmlBody.append("        <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        htmlBody.append("    <TD>Requested Clients</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        String separator = "";
        for (int i=0; i < requestedClients.length; i++)
        {
          htmlBody.append(separator);
          htmlBody.append(requestedClients[i]);
          separator = "<BR>" + EOL;
        }
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }

      // The set of resource monitor clients to use
      String[] monitorClients = job.getResourceMonitorClients();
      if ((monitorClients != null) && (monitorClients.length > 0))
      {
        htmlBody.append("        <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        htmlBody.append("    <TD>Resource Monitor Clients</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        String separator = "";
        for (int i=0; i < monitorClients.length; i++)
        {
          htmlBody.append(separator);
          htmlBody.append(monitorClients[i]);
          separator = "<BR>" + EOL;
        }
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    // The indication of whether to automatically monitor client systems
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" +
                    (job.monitorClientsIfAvailable() ? "true" : "false") +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of threads per client
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Threads per Client</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getThreadsPerClient() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The thread startup delay
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
    htmlBody.append("    <TD>Thread Startup Delay</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getThreadStartupDelay() +
                    " milliseconds</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The collection interval
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
    htmlBody.append("    <TD>Statistics Collection Interval</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" +
         secondsToHumanReadableDuration(job.getCollectionInterval()) + "</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    // The set of scheduled job dependencies.
    String[] dependencies = job.getDependencies();
    htmlBody.append("        <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
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
            String link =
                 generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
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
              String link =
                   generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
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

    if (! (readOnlyMode && hideSensitiveInformation))
    {
      // The set of addresses to notify when the job has completed.
      htmlBody.append("        <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Notify on Completion</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);

      String[] notifyAddresses = job.getNotifyAddresses();
      if ((notifyAddresses == null) || (notifyAddresses.length == 0))
      {
        htmlBody.append("  <TD>(none specified)</TD>" + EOL);
      }
      else
      {
        htmlBody.append("  <TD><A HREF=\"mailto:" + notifyAddresses[0] + "\">" +
                        notifyAddresses[0] + "</A>");
        for (int i=1; i < notifyAddresses.length; i++)
        {
          htmlBody.append(", <A HREF=\"mailto:" + notifyAddresses[i] + "\">" +
                          notifyAddresses[i] + "</A>");
        }
        htmlBody.append("</TD>" + EOL);
      }

      htmlBody.append("  </TR>" + EOL);
    }


    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Parameter " +
                    "Information</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Parameter information
    Parameter[] params = job.getParameterStubs().clone().getParameters();
    if ((params != null) && (params.length > 0))
    {
      for (int i=0, j=0; i < params.length; i++,j++)
      {
        Parameter p =
            job.getParameterList().getParameter(params[i].getName());
        if (readOnlyMode && hideSensitiveInformation && p.isSensitive())
        {
          continue;
        }

        String valueStr = "(not specified)";
        if (p != null)
        {
          valueStr = p.getHTMLDisplayValue();
        }
        else if ((params[i] instanceof PlaceholderParameter) ||
                 (params[i] instanceof LabelParameter))
        {
          j--;
          continue;
        }

        if (j % 2 == 1)
        {
          htmlBody.append("        <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        }
        else
        {
          htmlBody.append("        <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        }

        htmlBody.append("    <TD>" + params[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + valueStr + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }
    else
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">No parameters defined or " +
                      "parameter information is not available</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // Comments about the job
    String comments = job.getJobComments();
    if ((comments != null) && (comments.length() > 0))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Job Comments</B></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">" + comments + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    if (job.doneRunning())
    {
      if (! (readOnlyMode && hideSensitiveInformation))
      {
        // The list of clients used.
        String[] clientIDs = job.getStatTrackerClientIDs();
        if ((clientIDs != null) && (clientIDs.length > 0))
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\"><B>Clients Used</B></TD>" +
                          EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
          htmlBody.append("      <TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
          for (int i=0; i < clientIDs.length; i++)
          {
            if (i % 2 == 1)
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }
            else
            {
              htmlBody.append("        <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }

            htmlBody.append("          <TD>" + clientIDs[i] + "</TD>" + EOL);
            htmlBody.append("        </TR>" + EOL);
          }
          htmlBody.append("      </TABLE>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
      }

      // The log messages
      String[] logMessages = job.getLogMessages();
      if ((logMessages != null) && (logMessages.length > 0))
      {
        String splitReason = null;
        boolean split = (logMessages.length > 20);
        if (split)
        {
          splitReason = "There were more than 20 messages logged.";
        }
        else
        {
          for (int i=0; ((! split) && (i < logMessages.length)); i++)
          {
            split = logMessages[i].length() > 100;
            splitReason = "One or messages are too wide to be displayed here.";
          }
        }

        if (split)
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\"><B>Messages Logged</B></TD>" +
                          EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);

          String link = generateNewWindowLink(requestInfo,
                             Constants.SERVLET_SECTION_JOB,
                             Constants.SERVLET_SECTION_JOB_VIEW_LOG_MESSAGES,
                             Constants.SERVLET_PARAM_JOB_ID, job.getJobID(),
                             "here");

          htmlBody.append("      " + splitReason + EOL);
          htmlBody.append("      Click " + link + " to view the full set of " +
                          "log messages for this job in a separate window." +
                          EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
        else
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\"><B>Messages Logged</B></TD>" +
                          EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
          htmlBody.append("      <TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
          htmlBody.append("        <PRE>");
          for (int i=0; i < logMessages.length; i++)
          {
            if (i > 0)
            {
              htmlBody.append(EOL);
            }
            htmlBody.append(logMessages[i]);
          }
          htmlBody.append("</PRE>" + EOL);
          htmlBody.append("      </TABLE>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
      }
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work of viewing the full set of log messages associated with a
   * given job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJobLogMessages(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewJobLogMessages()");

    // The user must have at least view job permission to access anything in
    // this section.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    StringBuilder       htmlBody       = requestInfo.htmlBody;


    // Get the job ID of the requested job.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">View Job Log Messages</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job ID was provided to indicate the job for which " +
                      "to view the log messages.");
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">View Log Messages for Job " + jobID + "</SPAN>" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }


    // Get the job from the job cache.
    Job job;
    try
    {
      job = configDB.getJob(jobID);
      if (job == null)
      {
        htmlBody.append("ERROR:  Job " + jobID + " could not be found." + EOL);
        return;
      }
    }
    catch (Exception e)
    {
      htmlBody.append("ERROR:  Unable to retrieve job from the job cache:  " +
                      e + EOL);
      return;
    }


    // Get the log messages for the specified job.
    String[] logMessages = job.getLogMessages();
    if ((logMessages == null) || (logMessages.length == 0))
    {
      htmlBody.append("Job " + jobID + " does not have any log messages." +
                      EOL);
    }
    else
    {
      htmlBody.append("The following messages were logged for job " + jobID +
                      ':' + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<PRE>");
      for (int i=0; i < logMessages.length; i++)
      {
        htmlBody.append(logMessages[i] + EOL);
      }
      htmlBody.append("</PRE>" + EOL);
    }
  }



  /**
   * Handles all processing related to viewing detailed statistical information
   * about a job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJobStatistics(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewJobStatistics()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    // The user must be able to view job information to do anything here
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the job ID.  If there is no job ID, then just display an error.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">View Job Statistics</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job was specified for which to view statistical " +
                      "information." + EOL);
      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                                 "here");
      htmlBody.append("Click " + link + " to view the set of completed " +
                      "jobs" + EOL);
      return;
    }


    // Get the job with the specified job ID.
    Job job = null;
    try
    {
      job = configDB.getJob(jobID);
      if (job == null)
      {
        throw new SLAMDServerException("Could not retrieve information for " +
                                       "job from the configuration directory");
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  " + e.getMessage() + "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">View Job Statistics</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Information about job " + jobID +
                      "is not available." + EOL);
      htmlBody.append("See the error message above for more information." +
                      EOL);
      return;
    }


    // Determine what exactly should be shown.
    boolean showJobStats    = true;
    boolean showClientStats = false;
    boolean showThreadStats = false;


    // Show the header at the top of the page
    requestInfo.generateSidebar = false;
    htmlBody.append("<A NAME=\"top\">" + EOL);
    htmlBody.append("<H1>View Statistics for Job " + jobID + "</H1>" + EOL);
    htmlBody.append("<BR>" + EOL);


    // Create all the variables we'll need to show the form.
    String[] trackerNames = job.getStatTrackerNames();
    String[] displayTrackerNames = new String[0];
    int defaultStatTypes = 1;
    for (int i=0; i < trackerNames.length; i++)
    {
      defaultStatTypes *= 2;
    }
    defaultStatTypes -= 1;

    String[] monitorTrackerNames = job.getResourceStatTrackerNames();
    String[] displayMonitorTrackerNames = new String[0];
    int defaultMonitorStatTypes = 1;
    for (int i=0; i < monitorTrackerNames.length; i++)
    {
      defaultMonitorStatTypes *= 2;
    }
    defaultMonitorStatTypes -= 1;

    MultiValuedParameter trackersParameter =
         new MultiValuedParameter(Constants.SERVLET_PARAM_STAT_TRACKER,
                                  "Statistic Types",
                                  "The types of statistics that are " +
                                  "available for this job", trackerNames, 0,
                                  true);
    MultiValuedParameter monitorTrackersParameter =
         new MultiValuedParameter(Constants.SERVLET_PARAM_MONITOR_STAT,
                                  "Resource Monitor Statistic Types",
                                  "The types of resource monitor statistics " +
                                  "that are available for this job",
                                  monitorTrackerNames, 0, true);
    MultiValuedParameter detailLevelParameter =
         new MultiValuedParameter(Constants.SERVLET_PARAM_DETAIL_LEVEL,
                                  "Level of Detail",
                                  "The amount of detail to display for each " +
                                  "statistic type",
                                  Constants.STAT_CATEGORY_NAMES,
                                  Constants.STAT_CATEGORY_JOB_STATS, true);


    // Figure out what the user has actually requested (if anything)
    if (request.getParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                             Constants.SERVLET_PARAM_STAT_TRACKER) != null)
    {
      try
      {
        trackersParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  trackersParameter.getName()));
        displayTrackerNames = intToTrackerNames(trackersParameter.getIntValue(),
                                                trackerNames);
      } catch (Exception e) {}
    }

    if (request.getParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                             Constants.SERVLET_PARAM_MONITOR_STAT) != null)
    {
      try
      {
        monitorTrackersParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  monitorTrackersParameter.getName()));
        displayMonitorTrackerNames =
             intToTrackerNames(monitorTrackersParameter.getIntValue(),
                               monitorTrackerNames);
      } catch (Exception e) {}
    }

    if (request.getParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                             Constants.SERVLET_PARAM_DETAIL_LEVEL) != null)
    {
      try
      {
        detailLevelParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  detailLevelParameter.getName()));
        int intValue = detailLevelParameter.getIntValue();
        showJobStats    = (intValue & Constants.STAT_CATEGORY_JOB_STATS) ==
                          Constants.STAT_CATEGORY_JOB_STATS;
        showClientStats = (intValue & Constants.STAT_CATEGORY_CLIENT_STATS) ==
                          Constants.STAT_CATEGORY_CLIENT_STATS;
        showThreadStats = (intValue & Constants.STAT_CATEGORY_THREAD_STATS) ==
                          Constants.STAT_CATEGORY_THREAD_STATS;
      } catch (Exception e) {}
    }


    // Generate the form to allow the user choose what they want to see.
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                   Constants.SERVLET_SECTION_JOB_VIEW_STATS) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          jobID) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }
    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD><B>" + trackersParameter.getDisplayName() +
                    "</B></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    trackersParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    if (monitorTrackerNames.length > 0)
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><B>" +
                      monitorTrackersParameter.getDisplayName() + "</B></TD>" +
                      EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      monitorTrackersParameter.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD><B>" + detailLevelParameter.getDisplayName() +
                    "</B></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    detailLevelParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);


    // Since this page can get kind of long, generate anchor links
    for (int i=0; i < displayTrackerNames.length; i++)
    {
      htmlBody.append("<A HREF=\"#stat" + i + "\">Jump to statistics for " +
                      displayTrackerNames[i] + "</A><BR>" + EOL);
    }
    for (int i=0; i < displayMonitorTrackerNames.length; i++)
    {
      htmlBody.append("<A HREF=\"#stat" + (i+displayTrackerNames.length) +
                      "\">Jump to statistics for " +
                      displayMonitorTrackerNames[i] + "</A><BR>" + EOL);
    }


    // Actually generate the detail information.
    String separator = "<BR><HR><BR>";
    for (int i=0; i < displayTrackerNames.length; i++)
    {
      htmlBody.append(separator + EOL);
      htmlBody.append("<A NAME=\"stat" + i + "\">" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">" + displayTrackerNames[i] + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      if (showJobStats)
      {
        StatTracker[] statTrackers =
             job.getStatTrackers(displayTrackerNames[i]);

        try
        {
          StatTracker tracker = statTrackers[0].newInstance();
          tracker.aggregate(statTrackers);
          htmlBody.append("<B>Combined Data for All Threads</B>" + EOL);
          htmlBody.append("<BR>" + EOL);
          htmlBody.append(tracker.getDetailHTML() + EOL);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }

      if (showClientStats || showThreadStats)
      {
        String[] clientIDs = job.getStatTrackerClientIDs();
        for (int j=0; j < clientIDs.length; j++)
        {
          StatTracker[] statTrackers =
               job.getStatTrackers(displayTrackerNames[i], clientIDs[j]);

          if (showClientStats)
          {
            try
            {
              StatTracker tracker = statTrackers[0].newInstance();
              tracker.aggregate(statTrackers);
              htmlBody.append("<BLOCKQUOTE>" + EOL);
              htmlBody.append("<B>Summary Data for Client " + clientIDs[j] +
                              "</B>" + EOL);
              htmlBody.append("<BR>" + EOL);
              htmlBody.append(tracker.getDetailHTML() + EOL);

              if (showThreadStats)
              {
                htmlBody.append("<BLOCKQUOTE>" + EOL);
                for (int k=0; k < statTrackers.length; k++)
                {
                  htmlBody.append("<B>Data for Client " + clientIDs[j] +
                                  " Thread " + statTrackers[k].getThreadID() +
                                  "</B>" + EOL);
                  htmlBody.append("<BR>" + EOL);
                  htmlBody.append(statTrackers[k].getDetailHTML() + EOL);
                  htmlBody.append("<BR>" + EOL);
                }
                htmlBody.append("</BLOCKQUOTE>" + EOL);
              }

              htmlBody.append("</BLOCKQUOTE>" + EOL);
            }
            catch (Exception e)
            {
              e.printStackTrace();
            }
          }
          else
          {
            htmlBody.append("<BLOCKQUOTE>" + EOL);
            for (int k=0; k < statTrackers.length; k++)
            {
              htmlBody.append("<B>Data for Client " + clientIDs[j] +
                              " Thread " + statTrackers[k].getThreadID() +
                              "</B>" + EOL);
              htmlBody.append("<BR>" + EOL);
              htmlBody.append(statTrackers[k].getDetailHTML() + EOL);
              htmlBody.append("<BR>" + EOL);
            }
            htmlBody.append("</BLOCKQUOTE>" + EOL);
          }
        }
      }

      htmlBody.append("<A HREF=\"#top\">Return to Top</A><BR>" + EOL);
    }

    for (int i=0; i < displayMonitorTrackerNames.length; i++)
    {
      htmlBody.append(separator + EOL);
      htmlBody.append("<A NAME=\"stat" + (i+displayTrackerNames.length) +
                      "\">" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">" + displayMonitorTrackerNames[i] + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      StatTracker[] statTrackers =
           job.getResourceStatTrackers(displayMonitorTrackerNames[i]);

      try
      {
        StatTracker tracker = statTrackers[0].newInstance();
        tracker.aggregate(statTrackers);
        htmlBody.append("<B>Combined Data for All Threads</B>" + EOL);
        htmlBody.append("<BR>" + EOL);
        htmlBody.append(tracker.getDetailHTML() + EOL);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }

      htmlBody.append("<A HREF=\"#top\">Return to Top</A><BR>" + EOL);
    }
  }



  /**
   * Handles all processing related to saving job statistics to an external
   * file.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleSaveJobStatistics(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleSaveJobStatistics()");

    // The user must be able to export job information to do anything here
    if (! requestInfo.mayExportJobData)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "save job information");
      return;
    }


    // Get important state variables for this request.
    HttpServletRequest request     = requestInfo.request;
    StringBuilder       htmlBody    = requestInfo.htmlBody;
    StringBuilder       infoMessage = requestInfo.infoMessage;


    // Get the job ID.  If there is no job ID, then just display an error.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Save Job Statistics</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job was specified for which to save statistical " +
                      "information." + EOL);
      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                                 "here");
      htmlBody.append("Click " + link + " to view the set of completed " +
                      "jobs" + EOL);
      return;
    }


    // Get the job with the specified job ID.
    Job job = null;
    try
    {
      job = configDB.getJob(jobID);
      if (job == null)
      {
        throw new SLAMDServerException("Could not retrieve information for " +
                                       "job from the configuration directory");
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  " + e.getMessage() + "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Save Job Statistics</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Information about job " + jobID +
                      "is not available." + EOL);
      htmlBody.append("See the error message above for more information." +
                      EOL);
      return;
    }


    // Determine what exactly should be saved.
    boolean saveJobStats     = true;
    boolean saveClientStats  = true;
    boolean saveThreadStats  = true;
    boolean saveWithLabels   = true;
    String[] trackerNames = job.getStatTrackerNames();
    String[] monitorTrackerNames = job.getResourceStatTrackerNames();
    String[] saveTrackerNames = trackerNames;
    String[] saveMonitorTrackerNames = monitorTrackerNames;

    int defaultStatTypes = 1;
    for (int i=0; i < trackerNames.length; i++)
    {
      defaultStatTypes *= 2;
    }
    defaultStatTypes -= 1;

    int defaultMonitorStatTypes = 1;
    for (int i=0; i < monitorTrackerNames.length; i++)
    {
      defaultMonitorStatTypes *= 2;
    }
    defaultMonitorStatTypes -= 1;

    int detailLevel = Constants.STAT_CATEGORY_JOB_STATS |
                      Constants.STAT_CATEGORY_CLIENT_STATS |
                      Constants.STAT_CATEGORY_THREAD_STATS;
    BooleanParameter includeLabelsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Row/Column Labels",
                              "Indicates whether to include row and column " +
                              "labels in the data to be saved", true);
    MultiValuedParameter trackersParameter =
         new MultiValuedParameter(Constants.SERVLET_PARAM_STAT_TRACKER,
                                  "Statistic Types",
                                  "The types of statistics that are " +
                                  "available for this job", trackerNames,
                                  defaultStatTypes, true);
    MultiValuedParameter monitorTrackersParameter =
         new MultiValuedParameter(Constants.SERVLET_PARAM_MONITOR_STAT,
                                  "Resource Monitor Statistic Types",
                                  "The types of resource monitor statistics " +
                                  "that are available for this job",
                                  monitorTrackerNames, defaultMonitorStatTypes,
                                  true);
    MultiValuedParameter detailLevelParameter =
         new MultiValuedParameter(Constants.SERVLET_PARAM_DETAIL_LEVEL,
                                  "Level of Detail",
                                  "The amount of detail to include for each " +
                                  "statistic type",
                                  Constants.STAT_CATEGORY_NAMES, detailLevel,
                                  true);


    // See whether we need to actually save the data or if we should display the
    // form that allows the user to choose what they want to save.  This is done
    // by looking for the presence of the confirmed parameter.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) || (confirmStr.length() == 0))
    {
      // The user hasn't gotten to choose what he/she wants to see yet, so
      // show them the form.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Save Data for Job " + jobID + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("This function allows you to save the statistical " +
                      "information gathered while processing this job to a " +
                      "tab-delimited text file that may be imported into " +
                      "a spreadsheet or other application." + EOL);
      htmlBody.append("Choose the kinds of data and the level of detail that " +
                      "you would like to include in the output." + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" +
                      requestInfo.servletBaseURI + "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_SAVE_STATS) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                            jobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><B>" + trackersParameter.getDisplayName() +
                      "</B></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      trackersParameter.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      "</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      if (monitorTrackerNames.length > 0)
      {
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD><B>" +
                        monitorTrackersParameter.getDisplayName() +
                        "</B></TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        monitorTrackersParameter.getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><B>" + detailLevelParameter.getDisplayName() +
                      "</B></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      detailLevelParameter.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      "</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><B>" +
                      includeLabelsParameter.getDisplayName() + "</B></TD>" +
                      EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      includeLabelsParameter.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      "</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Save the Data\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);

      htmlBody.append("<BR><BR>" + EOL);
      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                                 Constants.SERVLET_PARAM_JOB_ID, jobID,
                                 "Return to Job Information Page");
      htmlBody.append(link + EOL);
    }
    else
    {
      // The form has been submitted, so we're going to write out the
      // information that they have requested.  First, get all the request
      // parameters.
      try
      {
        trackersParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  trackersParameter.getName()));
        saveTrackerNames = intToTrackerNames(trackersParameter.getIntValue(),
                                             trackerNames);
      } catch (Exception e) {}
      try
      {
        monitorTrackersParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  monitorTrackersParameter.getName()));
        saveMonitorTrackerNames =
            intToTrackerNames(monitorTrackersParameter.getIntValue(),
                              monitorTrackerNames);
      } catch (Exception e) {}
      try
      {
        detailLevelParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  detailLevelParameter.getName()));
        int intValue = detailLevelParameter.getIntValue();
        saveJobStats    = (intValue & Constants.STAT_CATEGORY_JOB_STATS) ==
                          Constants.STAT_CATEGORY_JOB_STATS;
        saveClientStats = (intValue & Constants.STAT_CATEGORY_CLIENT_STATS) ==
                          Constants.STAT_CATEGORY_CLIENT_STATS;
        saveThreadStats = (intValue & Constants.STAT_CATEGORY_THREAD_STATS) ==
                          Constants.STAT_CATEGORY_THREAD_STATS;
      } catch (Exception e) {}
      try
      {
        includeLabelsParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  includeLabelsParameter.getName()));
        saveWithLabels = includeLabelsParameter.getBooleanValue();
      } catch (Exception e) {}


      // Get the print writer for this servlet.  This is the only thing that
      // should throw an exception, so it is the only way we can bail out
      // prematurely.  Of course, if this fails, then it's very likely that
      // the attempt to display the page back to the client will fail too.
      PrintWriter writer;
      try
      {
        writer = requestInfo.response.getWriter();
      }
      catch (IOException ioe)
      {
        infoMessage.append("ERROR:  Unable to write the data -- " + ioe +
                           "<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Error Saving Data</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The attempt to save the data failed." + EOL);
        htmlBody.append("See the error message above for additional " +
                        "information");
        return;
      }


      // OK.  We're ready to go.  We want this information to get saved to a
      // file rather than displayed by the browser.  To do this, we need to
      // set a content type that the browser doesn't recognize.  Also, we want
      // to make sure that doPost doesn't try to generate any additional HTML
      // because otherwise that would get saved in the file too.
      requestInfo.response.setContentType("application/x-slamd-statistics");
      requestInfo.response.addHeader("Content-Disposition",
                                     "filename=\"slamd_job_data_" + jobID +
                                     ".txt\"");
      requestInfo.generateHTML = false;


      // Now iterate through each type of statistic and save the appropriate
      // set of values.
      for (int i=0; i < saveTrackerNames.length; i++)
      {
        if (saveJobStats)
        {
          StatTracker[] statTrackers =
               job.getStatTrackers(saveTrackerNames[i]);

          try
          {
            StatTracker tracker = statTrackers[0].newInstance();
            tracker.aggregate(statTrackers);
            if (saveWithLabels)
            {
              writer.println("Combined " + saveTrackerNames[i] +
                             " Data for All Threads");
            }
            String[][] values = tracker.getDataForExport(saveWithLabels);
            for (int j=0; j < values.length; j++)
            {
              String tab = "";
              for (int k=0; k < values[j].length; k++)
              {
                writer.print(tab + values[j][k]);
                tab = "\t";
              }
              writer.println();
            }

            writer.println();
            writer.println();
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }

        if (saveClientStats || saveThreadStats)
        {
          String[] clientIDs = job.getStatTrackerClientIDs();
          for (int j=0; j < clientIDs.length; j++)
          {
            StatTracker[] statTrackers =
                 job.getStatTrackers(saveTrackerNames[i], clientIDs[j]);

            if (saveClientStats)
            {
              try
              {
                StatTracker tracker = statTrackers[0].newInstance();
                tracker.aggregate(statTrackers);
                if (saveWithLabels)
                {
                  writer.println("Combined " + saveTrackerNames[i] +
                                 " Data for Client " + clientIDs[j]);
                }
                String[][] values = tracker.getDataForExport(saveWithLabels);
                for (int k=0; k < values.length; k++)
                {
                  String tab = "";
                  for (int l=0; l < values[k].length; l++)
                  {
                    writer.print(tab + values[k][l]);
                    tab = "\t";
                  }
                  writer.println();
                }

                writer.println();
                writer.println();
              }
              catch (Exception e)
              {
                e.printStackTrace();
              }
            }

            if (saveThreadStats)
            {
              for (int k=0; k < statTrackers.length; k++)
              {
                if (saveWithLabels)
                {
                  writer.println(saveTrackerNames[i] + " Data for Client " +
                                 clientIDs[j] + " Thread " +
                                 statTrackers[k].getThreadID());
                }
                String[][] values =
                     statTrackers[k].getDataForExport(saveWithLabels);
                for (int l=0; l < values.length; l++)
                {
                  String tab = "";
                  for (int m=0; m < values[l].length; m++)
                  {
                    writer.print(tab + values[l][m]);
                    tab = "\t";
                  }
                  writer.println();
                }

                writer.println();
                writer.println();
              }
            }
          }
        }
      }


      // Now iterate through each type of resource monitor statistic and save
      // the appropriate set of values.
      for (int i=0; i < saveMonitorTrackerNames.length; i++)
      {
        StatTracker[] statTrackers =
             job.getResourceStatTrackers(saveMonitorTrackerNames[i]);
        try
        {
          StatTracker tracker = statTrackers[0].newInstance();
          tracker.aggregate(statTrackers);
          if (saveWithLabels)
          {
            writer.println(saveMonitorTrackerNames[i]);
          }
          String[][] values = tracker.getDataForExport(saveWithLabels);
          for (int j=0; j < values.length; j++)
          {
            String tab = "";
            for (int k=0; k < values[j].length; k++)
            {
              writer.print(tab + values[j][k]);
              tab = "\t";
            }
            writer.println();
          }

          writer.println();
          writer.println();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }



  /**
   * Converts the provided int value into a list of the stat tracker names to
   * which that number should correspond.  This is used to convert the value
   * of the multivalued parameter for statistic types into a list of the stat
   * trackers for which data is to be provided.
   *
   * @param  intValue      The int value to be converted into a set of tracker
   *                       names.
   * @param  trackerNames  The set of all stat tracker names available for the
   *                       job to be displayed.
   *
   * @return  The set of stat tracker names that the user has chosen.
   */
  static String[] intToTrackerNames(int intValue, String[] trackerNames)
  {
    ArrayList<String> nameList = new ArrayList<String>();

    for (int i=0; i < trackerNames.length; i++)
    {
      // Calculate 2^i
      int val = 1;
      for (int j=0; j < i; j++)
      {
        val *= 2;
      }

      if ((intValue & val) == val)
      {
        nameList.add(trackerNames[i]);
      }
    }

    String[] nameArray = new String[nameList.size()];
    nameList.toArray(nameArray);
    return nameArray;
  }



  /**
   * Handles the processing required to display graphs of statistical
   * information gathered during job processing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewGraph(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewGraph()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.htmlBody;


    // See if information has been posted for this graph.
    boolean posted = false;
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      posted = true;
    }


    // Get the job ID and the job that goes along with it
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Job Results</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job ID was specified." + EOL);
      htmlBody.append("A job ID is required to indicate which job " +
                      "contains the data to be graphed." + EOL);
      return;
    }

    Job job;
    try
    {
      job = configDB.getJob(jobID);
      if (job == null)
      {
        throw new SLAMDServerException("No information is know about job " +
                                       jobID);
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  " + e.getMessage() + "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Results for Job " + jobID + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Unable to retrieve job " + jobID +
                      " from the configuration directory." + EOL);
      htmlBody.append("See the error message above for additional " +
                      "information." + EOL);
      return;
    }


    // Get information about the kinds of statistics available for this job.
    // If there is no statistical data available, then abort.
    String[] trackerNames = job.getStatTrackerNames();
    String[] clientIDs    = job.getStatTrackerClientIDs();
    if ((trackerNames == null) || (trackerNames.length == 0) ||
        (clientIDs == null) || (clientIDs.length == 0))
    {
      trackerNames = job.getResourceStatTrackerNames();
      clientIDs = job.getResourceStatTrackerClientIDs();
      if ((trackerNames == null) || (trackerNames.length == 0) ||
          (clientIDs == null) || (clientIDs.length == 0))
      {
        String link = generateGetJobLink(requestInfo, jobID, jobID);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Graph Results for Job " + link + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The specified job does not contain any statistical " +
                        "data from which to generate a graph.");
        return;
      }
      else
      {
        // This job does not have any job statistics, but it does have
        // monitor statistics, so show the user the page to display them.
        handleViewMonitorGraph(requestInfo);
        return;
      }
    }


    // Generate the common parameters that will be used for user input.
    IntegerParameter widthParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_WIDTH,
                              "Graph Width",
                              "The width in pixels of the graph to create",
                              true, defaultGraphWidth, true, 0, false, 0);
    IntegerParameter heightParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_HEIGHT,
                              "Graph Height",
                              "The height in pixels of the graph to create",
                              true, defaultGraphHeight, true, 0, false, 0);
    MultiChoiceParameter trackersParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_STAT_TRACKER,
                                  "Statistic Types",
                                  "The types of statistics that are " +
                                  "maintained for this job.", trackerNames,
                                  trackerNames[0]);
    IntegerParameter monitorHeightParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_MONITOR_GRAPH_HEIGHT,
                              "Resource Monitor Graph Height",
                              "The height in pixels for the resource monitor " +
                              "graphs", true, defaultMonitorGraphHeight, true,
                              0, false, 0);
    BooleanParameter graphAllMonitorsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_MONITOR_GRAPH_ALL,
                              "Graph All Resource Monitors",
                              "Indicates whether all resource monitor " +
                              "statistics should be graphed.", false);

    // Obtain the values that should be used for the common parameters.
    if (posted)
    {
      try
      {
        widthParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  widthParameter.getName()));
      } catch (Exception e) {}

      try
      {
        heightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  heightParameter.getName()));
      } catch (Exception e) {}

      try
      {
        trackersParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  trackersParameter.getName()));
      } catch (Exception e) {}
    }

    // Get the tracker-specific parameters.
    Parameter[] trackerParameters = new Parameter[0];
    StatTracker[] selectedTrackers =
         job.getStatTrackers(trackersParameter.getStringValue());
    if ((selectedTrackers != null) && (selectedTrackers.length > 0))
    {
      trackerParameters =
           selectedTrackers[0].getGraphParameterStubs(job).
                 clone().getParameters();

      for (int i=0; i < trackerParameters.length; i++)
      {
        if (posted)
        {
          try
          {
            trackerParameters[i].htmlInputFormToValue(
                 request.getParameterValues(
                      Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                      trackerParameters[i].getName()));
          } catch (Exception e) {}
        }
      }
    }


    // Get the height for the monitor tracker graphs.
    try
    {
      monitorHeightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  monitorHeightParameter.getName()));
    } catch (Exception e) {}


    // Get the set of all resource monitor statistics collected for the job.
    ResourceMonitorStatTracker[] monitorTrackers =
         job.getResourceMonitorStatTrackers();


    // Determine whether to graph all monitor trackers.
    try
    {
      graphAllMonitorsParameter.htmlInputFormToValue(
           request.getParameterValues(
                Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                graphAllMonitorsParameter.getName()));
    } catch (Exception e) {}
    boolean graphAllMonitors = graphAllMonitorsParameter.getBooleanValue();


    // Determine whether to graph monitor trackers by client.
    String[] monitorClientIDs = job.getResourceStatTrackerClientIDs();
    BooleanParameter[] monitorClientParameters =
         new BooleanParameter[monitorClientIDs.length];
    HashSet<String> monitorClientSet =
         new HashSet<String>(monitorClientIDs.length);
    for (int i=0; i < monitorClientIDs.length; i++)
    {
      String clientID    = monitorClientIDs[i];
      String safeName    = Constants.SERVLET_PARAM_MONITOR_CLIENT + '_' +
                           clientID;
      String displayName = "Graph All " + clientID + " Resource Monitors";
      String description = "Indicates whether all resource statistics " +
                           "collected by the " + clientID +
                           " resource monitor client should be graphed.";

      monitorClientParameters[i] =
           new BooleanParameter(safeName, displayName, description, false);

      try
      {
        if (posted)
        {
          if (graphAllMonitors)
          {
            monitorClientParameters[i].setValue(true);
          }
          else
          {
            monitorClientParameters[i].htmlInputFormToValue(
                 request.getParameterValues(
                      Constants.SERVLET_PARAM_JOB_PARAM_PREFIX + safeName));
          }
        }
      } catch (Exception e) {}

      if (monitorClientParameters[i].getBooleanValue())
      {
        monitorClientSet.add(clientID);
      }
    }


    // Determine whether to graph monitor trackers by monitor class.
    ResourceMonitor[] monitorClasses = job.getResourceMonitorClasses();
    BooleanParameter[] monitorClassParameters =
         new BooleanParameter[monitorClasses.length];
    HashSet<String> monitorClassSet =
         new HashSet<String>(monitorClasses.length);
    for (int i=0; i < monitorClassParameters.length; i++)
    {
      String className   = monitorClasses[i].getClass().getName();
      String safeName    = Constants.SERVLET_PARAM_MONITOR_CLASS + '_' +
                           className;
      String displayName = "Graph All " + monitorClasses[i].getMonitorName() +
                           " Resource Monitors";
      String description = "Indicates whether all resource statistics " +
                           "collected by instances of the " + className +
                           " resource monitor should be graphed.";

      monitorClassParameters[i] =
           new BooleanParameter(safeName, displayName, description, false);

      try
      {
        if (posted)
        {
          if (graphAllMonitors)
          {
            monitorClassParameters[i].setValue(true);
          }
          else
          {
            monitorClassParameters[i].htmlInputFormToValue(
                 request.getParameterValues(
                      Constants.SERVLET_PARAM_JOB_PARAM_PREFIX + safeName));
          }
        }
      } catch (Exception e) {}

      if (monitorClassParameters[i].getBooleanValue())
      {
        monitorClassSet.add(className);
      }
    }


    // Generate the page header.
    String link = generateGetJobLink(requestInfo, jobID, jobID);
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Graph Results for Job " + link + "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Choose the type of information and level of detail to " +
                    "display." + EOL);
    htmlBody.append("<BR>" + EOL);


    // Generate the form that allows the user to specify the information
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GRAPH) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          jobID) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + widthParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    widthParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + heightParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    heightParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + trackersParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    trackersParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    // Generate the tracker-specific parameter input form
    for (int i=0; i < trackerParameters.length; i++)
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + trackerParameters[i].getDisplayName() +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      trackerParameters[i].getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      "</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    // Generate the element to allow the user to specify the height for system
    // resource graphs.
    if (monitorTrackers.length > 0)
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + monitorHeightParameter.getDisplayName() +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      monitorHeightParameter.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      "</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" +
                      graphAllMonitorsParameter.getDisplayName() + "</TD>" +
                      EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      graphAllMonitorsParameter.getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);

      for (int i=0; i < monitorClientParameters.length; i++)
      {
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>" +
                        monitorClientParameters[i].getDisplayName() + "</TD>" +
                        EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        monitorClientParameters[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }

      for (int i=0; i < monitorClassParameters.length; i++)
      {
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>" +
                        monitorClassParameters[i].getDisplayName() + "</TD>" +
                        EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        monitorClassParameters[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                    "VALUE=\"View Graph\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>");


    // Generate the image tag.  Replace any spaces with the '+' sign.
    String imageURI;
    try
    {
      htmlBody.append("<BR><BR>" + EOL);
      imageURI = servletBaseURI + '?' + Constants.SERVLET_PARAM_SECTION + '=' +
                 Constants.SERVLET_SECTION_JOB + '&' +
                 Constants.SERVLET_PARAM_SUBSECTION + '=' +
                 Constants.SERVLET_SECTION_JOB_GRAPH + '&' +
                 Constants.SERVLET_PARAM_JOB_ID + '=' + jobID + '&' +
                 Constants.SERVLET_PARAM_GRAPH_WIDTH +'=' +
                 widthParameter.getIntValue() + '&' +
                 Constants.SERVLET_PARAM_GRAPH_HEIGHT + '=' +
                 heightParameter.getIntValue() + '&' +
                 Constants.SERVLET_PARAM_STAT_TRACKER + '=' +
                 URLEncoder.encode(trackersParameter.getStringValue(), "UTF-8");
      for (int i=0; i < trackerParameters.length; i++)
      {
        imageURI += '&' + Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                    trackerParameters[i].getName() + '=' +
                    trackerParameters[i].getValueString();
      }

      htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                      "\" WIDTH=\"" + widthParameter.getIntValue() +
                      "\" HEIGHT=\"" + heightParameter.getIntValue() +
                      "\" ALT=\"Graph of Results for Job " + jobID + "\">" +
                      EOL);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
    }


    // Graph any appropriate resource monitor statistics.
    for (int i=0; i < monitorTrackers.length; i++)
    {
      try
      {
        String monitorClass =
             monitorTrackers[i].getResourceMonitor().getClass().getName();
        String monitorClientID =
             monitorTrackers[i].getStatTracker().getClientID();

        if ((! monitorClassSet.contains(monitorClass)) &&
            (! monitorClientSet.contains(monitorClientID)))
        {
          continue;
        }

        String displayName =
             monitorTrackers[i].getStatTracker().getDisplayName();

        htmlBody.append("<BR><BR>" + EOL);
        imageURI = servletBaseURI + '?' + Constants.SERVLET_PARAM_SECTION +
                   '=' + Constants.SERVLET_SECTION_JOB + '&' +
                   Constants.SERVLET_PARAM_SUBSECTION + '=' +
                   Constants.SERVLET_SECTION_JOB_GRAPH_MONITOR + '&' +
                   Constants.SERVLET_PARAM_JOB_ID + '=' + jobID + '&' +
                   Constants.SERVLET_PARAM_GRAPH_WIDTH + '=' +
                   widthParameter.getIntValue() + '&' +
                   Constants.SERVLET_PARAM_MONITOR_GRAPH_HEIGHT + '=' +
                   monitorHeightParameter.getIntValue() + '&' +
                   Constants.SERVLET_PARAM_STAT_TRACKER + '=' +
                   URLEncoder.encode(displayName, "UTF-8");


        htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                        "\" WIDTH=\"" + widthParameter.getIntValue() +
                        "\" HEIGHT=\"" + monitorHeightParameter.getIntValue() +
                        "\" ALT=\"" + displayName + "\">" +  EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
      }
    }
  }



  /**
   * Handles the processing required to display graphs of resource monitor
   * information gathered during job processing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewMonitorGraph(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewGraph()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.htmlBody;


    // See if information has been posted for this graph.
    boolean posted = false;
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      posted = true;
    }


    // Get the job ID and the job that goes along with it
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Job Resource Monitors</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job ID was specified." + EOL);
      htmlBody.append("A job ID is required to indicate which job " +
                      "contains the data to be graphed." + EOL);
      return;
    }

    Job job;
    try
    {
      job = configDB.getJob(jobID);
      if (job == null)
      {
        throw new SLAMDServerException("No information is know about job " +
                                       jobID);
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  " + e.getMessage() + "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Resource Monitors for Job " + jobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Unable to retrieve job " + jobID +
                      " from the configuration directory." + EOL);
      htmlBody.append("See the error message above for additional " +
                      "information." + EOL);
      return;
    }


    // Get information about the kinds of statistics available for this job.
    // If there is no statistical data available, then abort.
    String[] trackerNames = job.getResourceStatTrackerNames();
    String[] clientIDs    = job.getResourceStatTrackerClientIDs();
    if ((trackerNames == null) || (trackerNames.length == 0) ||
        (clientIDs == null) || (clientIDs.length == 0))
    {
      String link = generateGetJobLink(requestInfo, jobID, jobID);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Resource Monitors for Job " + link +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The specified job does not contain any resource " +
                      "monitor data from which to generate a graph.");
      return;
    }


    // Generate the common parameters that will be used for user input.
    IntegerParameter widthParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_WIDTH,
                              "Graph Width",
                              "The width in pixels of the graph to create",
                              true, defaultGraphWidth, true, 0, false, 0);
    IntegerParameter heightParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_HEIGHT,
                              "Graph Height",
                              "The height in pixels of the graph to create",
                              true, defaultGraphHeight, true, 0, false, 0);
    MultiChoiceParameter trackersParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_STAT_TRACKER,
                                  "Resource Monitor Types",
                                  "The types of statistics that are " +
                                  "maintained for this job.", trackerNames,
                                  trackerNames[0]);
    IntegerParameter monitorHeightParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_MONITOR_GRAPH_HEIGHT,
                              "Resource Monitor Graph Height",
                              "The height in pixels for the resource monitor " +
                              "graphs", true, defaultMonitorGraphHeight, true,
                              0, false, 0);
    BooleanParameter graphAllMonitorsParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_MONITOR_GRAPH_ALL,
                              "Graph All Resource Monitors",
                              "Indicates whether all resource monitor " +
                              "statistics should be graphed.", false);

    // Obtain the values that should be used for the common parameters.
    if (posted)
    {
      try
      {
        widthParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  widthParameter.getName()));
      } catch (Exception e) {}

      try
      {
        heightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  heightParameter.getName()));
      } catch (Exception e) {}

      try
      {
        trackersParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  trackersParameter.getName()));
      } catch (Exception e) {}

      try
      {
        monitorHeightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  monitorHeightParameter.getName()));
      } catch (Exception e) {}

      try
      {
        graphAllMonitorsParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  graphAllMonitorsParameter.getName()));
      } catch (Exception e) {}
    }

    // Get the tracker-specific parameters.
    Parameter[] trackerParameters = new Parameter[0];
    StatTracker[] selectedTrackers =
         job.getResourceStatTrackers(trackersParameter.getStringValue());
    if ((selectedTrackers != null) && (selectedTrackers.length > 0))
    {
      trackerParameters =
           selectedTrackers[0].getMonitorGraphParameterStubs(job).
                 clone().getParameters();

      for (int i=0; i < trackerParameters.length; i++)
      {
        if (posted)
        {
          try
          {
            trackerParameters[i].htmlInputFormToValue(
                 request.getParameterValues(
                      Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                      trackerParameters[i].getName()));
          } catch (Exception e) {}
        }
      }
    }


    // Generate the page header.
    String link = generateGetJobLink(requestInfo, jobID, jobID);
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Graph Resource Monitors for Job " + link + "</SPAN>" +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Choose the resource monitor to graph." + EOL);
    htmlBody.append("<BR>" + EOL);


    // Generate the form that allows the user to specify the information
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_VIEW_MONITOR_GRAPH) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          jobID) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + widthParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    widthParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + heightParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    heightParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + trackersParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    trackersParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    // Generate the tracker-specific parameter input form
    for (int i=0; i < trackerParameters.length; i++)
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + trackerParameters[i].getDisplayName() +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      trackerParameters[i].getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      "</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + monitorHeightParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    monitorHeightParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + graphAllMonitorsParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    graphAllMonitorsParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                    "VALUE=\"View Graph\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>");


    try
    {
      // Generate the image tag.  Replace any spaces with the '+' sign.
      htmlBody.append("<BR><BR>" + EOL);
      String imageURI = servletBaseURI + '?' + Constants.SERVLET_PARAM_SECTION +
                        '=' + Constants.SERVLET_SECTION_JOB + '&' +
                        Constants.SERVLET_PARAM_SUBSECTION + '=' +
                        Constants.SERVLET_SECTION_JOB_GRAPH_MONITOR + '&' +
                        Constants.SERVLET_PARAM_JOB_ID + '=' + jobID + '&' +
                        Constants.SERVLET_PARAM_GRAPH_WIDTH + '=' +
                        widthParameter.getIntValue() + '&' +
                        Constants.SERVLET_PARAM_MONITOR_GRAPH_HEIGHT + '=' +
                        heightParameter.getIntValue() + '&' +
                        Constants.SERVLET_PARAM_STAT_TRACKER + '=' +
                        URLEncoder.encode(trackersParameter.getStringValue(),
                                          "UTF-8");
      for (int i=0; i < trackerParameters.length; i++)
      {
        imageURI += '&' + Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                    trackerParameters[i].getName() + '=' +
                    trackerParameters[i].getValueString();
      }

      htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                      "\" WIDTH=\"" + widthParameter.getIntValue() +
                      "\" HEIGHT=\"" + heightParameter.getIntValue() +
                      "\" ALT=\"Graph of Results for Job " + jobID + "\">" +
                      EOL);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
    }


    // If we should graph all monitor stats, then do so.
    if (graphAllMonitorsParameter.getBooleanValue())
    {
      String[] monitorTrackerNames = job.getResourceStatTrackerNames();
      for (int i=0; i < monitorTrackerNames.length; i++)
      {
        try
        {
          htmlBody.append("<BR><BR>" + EOL);
          String imageURI = servletBaseURI + '?' +
                            Constants.SERVLET_PARAM_SECTION + '=' +
                            Constants.SERVLET_SECTION_JOB + '&' +
                            Constants.SERVLET_PARAM_SUBSECTION + '=' +
                            Constants.SERVLET_SECTION_JOB_GRAPH_MONITOR + '&' +
                            Constants.SERVLET_PARAM_JOB_ID + '=' + jobID + '&' +
                            Constants.SERVLET_PARAM_GRAPH_WIDTH + '=' +
                            widthParameter.getIntValue() + '&' +
                            Constants.SERVLET_PARAM_MONITOR_GRAPH_HEIGHT + '=' +
                            monitorHeightParameter.getIntValue() + '&' +
                            Constants.SERVLET_PARAM_STAT_TRACKER + '=' +
                            URLEncoder.encode(monitorTrackerNames[i], "UTF-8");


          htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                          "\" WIDTH=\"" + widthParameter.getIntValue() +
                          "\" HEIGHT=\"" +
                          monitorHeightParameter.getIntValue() + "\" ALT=\"" +
                          monitorTrackerNames[i] + "\">" +  EOL);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
        }
      }
    }
  }



  /**
   * Handles the processing required to actually serve up the dynamically
   * generated images depicting information gathered during job processing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleGraph(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleGraph()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      try
      {
        requestInfo.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      } catch (Exception e) { e.printStackTrace(); }
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest  request  = requestInfo.request;
    HttpServletResponse response = requestInfo.response;


    // Get the job IDs.  If none were specified, then send back an error
    // response.  If one was provided, then generate a regular graph.  If
    // multiple job IDs were provided, then generate a graph comparing the
    // results of those jobs.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e) {}
      return;
    }
    else if (jobIDs.length == 1)
    {
      String jobID = jobIDs[0];
      Job job = null;
      try
      {
        job = configDB.getJob(jobID);
      } catch (Exception e) { e.printStackTrace(); }
      if (job == null)
      {
        try
        {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) { e.printStackTrace(); }
        return;
      }

      // Get the graph parameters.
      int width  = defaultGraphWidth;
      try
      {
        width = Integer.parseInt(request.getParameter(
                                      Constants.SERVLET_PARAM_GRAPH_WIDTH));
      } catch (Exception e) { e.printStackTrace(); }

      int height = defaultGraphHeight;
      try
      {
        height = Integer.parseInt(request.getParameter(
                                       Constants.SERVLET_PARAM_GRAPH_HEIGHT));
      } catch (Exception e) { e.printStackTrace(); }


      // Get the type of statistic that is to be graphed.
      String trackerName =
           request.getParameter(Constants.SERVLET_PARAM_STAT_TRACKER);
      StatTracker[] selectedTrackers = job.getStatTrackers(trackerName);
      if ((selectedTrackers == null) || (selectedTrackers.length == 0))
      {
        return;
      }


      // Get the tracker-specific parameters.
      Parameter[] trackerParams = new Parameter[0];
      if ((selectedTrackers != null) && (selectedTrackers.length > 0))
      {
        trackerParams = selectedTrackers[0].getGraphParameterStubs(job).
             clone().getParameters();

        for (int i=0; i < trackerParams.length; i++)
        {
          if (request.getParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                                   trackerParams[i].getName()) != null)
          {
            try
            {
              trackerParams[i].htmlInputFormToValue(
                   request.getParameterValues(
                        Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                        trackerParams[i].getName()));
            } catch (Exception e) {}
          }
        }
      }


      // Generate the graph.
      try
      {
        BufferedImage image =
             selectedTrackers[0].createGraph(job, width, height,
                                             new ParameterList(trackerParams));
        response.setContentType("image/png");
        response.addHeader("Content-Disposition", "filename=\"" +
                           generateGraphFilename(job, selectedTrackers[0]) +
                           '"');
        ImageEncoder encoder =
             ImageCodec.createImageEncoder("png", response.getOutputStream(),
                                           null);
        encoder.encode(image);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    else
    {
      Job[] jobs = new Job[jobIDs.length];
      for (int i=0; i < jobs.length; i++)
      {
        try
        {
          jobs[i] = configDB.getJob(jobIDs[i]);
        } catch (Exception e) { e.printStackTrace(); }
        if (jobs[i] == null)
        {
          try
          {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
          } catch (Exception e) { e.printStackTrace(); }
          return;
        }
      }

      // Get the graph parameters.
      int width  = Constants.DEFAULT_GRAPH_WIDTH;
      try
      {
        width = Integer.parseInt(request.getParameter(
                                      Constants.SERVLET_PARAM_GRAPH_WIDTH));
      } catch (Exception e) { e.printStackTrace(); }

      int height = Constants.DEFAULT_GRAPH_HEIGHT;
      try
      {
        height = Integer.parseInt(request.getParameter(
                                       Constants.SERVLET_PARAM_GRAPH_HEIGHT));
      } catch (Exception e) { e.printStackTrace(); }


      // Get the type of statistic that is to be graphed.
      String trackerName =
           request.getParameter(Constants.SERVLET_PARAM_STAT_TRACKER);
      StatTracker[] selectedTrackers = jobs[0].getStatTrackers(trackerName);
      if ((selectedTrackers == null) || (selectedTrackers.length == 0))
      {
        return;
      }


      // Get the tracker-specific parameters.
      Parameter[] trackerParams = new Parameter[0];
      if ((selectedTrackers != null) && (selectedTrackers.length > 0))
      {
        trackerParams = selectedTrackers[0].getGraphParameterStubs(jobs).
             clone().getParameters();

        for (int i=0; i < trackerParams.length; i++)
        {
          if (request.getParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                                   trackerParams[i].getName()) != null)
          {
            try
            {
              trackerParams[i].htmlInputFormToValue(
                   request.getParameterValues(
                        Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                        trackerParams[i].getName()));
            } catch (Exception e) {}
          }
        }
      }


      // Generate the graph.
      try
      {
        BufferedImage image =
             selectedTrackers[0].createGraph(jobs, width, height,
                                             new ParameterList(trackerParams));
        response.setContentType("image/png");
        response.addHeader("Content-Disposition", "filename=\"" +
                           generateGraphFilename(jobs, selectedTrackers[0]) +
                           '"');
        ImageEncoder encoder =
             ImageCodec.createImageEncoder("png", response.getOutputStream(),
                                           null);
        encoder.encode(image);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }



  /**
   * Handles the processing required to generate graphs from resource monitor
   * statistics.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleMonitorGraph(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleMonitorGraph()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      try
      {
        requestInfo.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      } catch (Exception e) { e.printStackTrace(); }
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest  request  = requestInfo.request;
    HttpServletResponse response = requestInfo.response;


    // Get the job ID.  If none was specified, then send back an error
    // response.  If one was provided, then generate a regular graph.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e) {}
      return;
    }

    Job job = null;
    try
    {
      job = configDB.getJob(jobID);
    } catch (Exception e) { e.printStackTrace(); }
    if (job == null)
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e) { e.printStackTrace(); }
      return;
    }

    // Get the graph parameters.
    int width  = defaultGraphWidth;
    try
    {
      width = Integer.parseInt(request.getParameter(
                                    Constants.SERVLET_PARAM_GRAPH_WIDTH));
    } catch (Exception e) { e.printStackTrace(); }

    int height = defaultMonitorGraphHeight;
    try
    {
      height =
           Integer.parseInt(request.getParameter(
                                 Constants.SERVLET_PARAM_MONITOR_GRAPH_HEIGHT));
    } catch (Exception e) { e.printStackTrace(); }


    // Get the type of statistic that is to be graphed.
    String trackerName =
         request.getParameter(Constants.SERVLET_PARAM_STAT_TRACKER);
    StatTracker[] selectedTrackers = job.getResourceStatTrackers(trackerName);
    if ((selectedTrackers == null) || (selectedTrackers.length == 0))
    {
      return;
    }


    // Get the tracker-specific parameters.
    Parameter[] trackerParams = new Parameter[0];
    if ((selectedTrackers != null) && (selectedTrackers.length > 0))
    {
      trackerParams = selectedTrackers[0].getMonitorGraphParameterStubs(job).
           clone().getParameters();

      for (int i=0; i < trackerParams.length; i++)
      {
        if (request.getParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                                 trackerParams[i].getName()) != null)
        {
          try
          {
            trackerParams[i].htmlInputFormToValue(
                 request.getParameterValues(
                      Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                      trackerParams[i].getName()));
          } catch (Exception e) {}
        }
      }
    }


    // Generate the graph.
    try
    {
      BufferedImage image =
           selectedTrackers[0].createMonitorGraph(job, width, height,
                                    new ParameterList(trackerParams));
      response.setContentType("image/png");
      response.addHeader("Content-Disposition", "filename=\"" +
                         generateGraphFilename(job, selectedTrackers[0]) + '"');
      ImageEncoder encoder =
           ImageCodec.createImageEncoder("png", response.getOutputStream(),
                                         null);
      encoder.encode(image);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }



  /**
   * Handles the processing required to display graphs of statistical
   * information gathered in real time.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewRealTimeGraph(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewRealTimeGraph()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // See if information has been posted for this graph.
    boolean posted = false;
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      posted = true;
    }


    // Get the job ID and the job that goes along with it
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph In-Progress Results</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job ID was specified." + EOL);
      htmlBody.append("A job ID is required to indicate which job " +
                      "contains the data to be graphed." + EOL);
      return;
    }

    Job job = null;
    RealTimeJobStats jobStats = null;
    try
    {
      job = scheduler.getJob(jobID);
      if (job != null)
      {
        jobStats = job.getRealTimeStats();
      }
    }
    catch (SLAMDServerException sse)
    {
      infoMessage.append("ERROR:  " + sse.getMessage() + "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph In-Progress Results for Job " + jobID +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Unable to retrieve job " + jobID +
                      "from the configuration directory." + EOL);
      htmlBody.append("See the error message above for additional " +
                      "information." + EOL);
      return;
    }

    if (jobStats == null)
    {
      if (job.doneRunning())
      {
        String link = generateGetJobLink(requestInfo, jobID, jobID);
        infoMessage.append("Job Completed.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Graph In-Progress Results for Job " + link +
                        "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Job " + jobID + " is no longer running." + EOL);
        htmlBody.append("The in-progress statistics associated with it are " +
                        "no longer available." + EOL);

        link = generateGetJobLink(requestInfo, jobID, "completed job page");
        htmlBody.append("See the " + link + " for complete details about the " +
                        "job.");
        return;
      }
      else
      {
        String link = generateGetJobLink(requestInfo, jobID, jobID);
        infoMessage.append("ERROR:  No data available<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Graph In-Progress Results for Job " + link +
                        "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Job " + jobID +
                        " does not have any in-progress data available." + EOL);
        return;
      }
    }


    // Get information about the kinds of statistics available for this job.
    // If there is no statistical data available, then abort.
    String[] statNames = jobStats.getStatNames();
    if ((statNames == null) || (statNames.length == 0))
    {
      String link = generateGetJobLink(requestInfo, jobID, jobID);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph In-Progress Results for Job " + link +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The specified job does not contain any statistical " +
                      "data from which to generate a graph.");
      return;
    }


    // Generate the common parameters that will be used for user input.
    IntegerParameter widthParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_WIDTH,
                              "Graph Width",
                              "The width in pixels of the graph to create",
                              true, defaultGraphWidth, true, 0, false, 0);
    IntegerParameter heightParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_HEIGHT,
                              "Graph Height",
                              "The height in pixels of the graph to create",
                              true, defaultGraphHeight, true, 0, false, 0);
    MultiChoiceParameter statsParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_STAT_TRACKER,
                                  "Statistic Types",
                                  "The types of statistics that are " +
                                  "maintained for this job.", statNames,
                                  statNames[0]);

    // Obtain the values that should be used for the common parameters.
    if (posted)
    {
      try
      {
        widthParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  widthParameter.getName()));
      } catch (Exception e) {}

      try
      {
        heightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  heightParameter.getName()));
      } catch (Exception e) {}

      try
      {
        statsParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  statsParameter.getName()));
      } catch (Exception e) {}
    }


    // Get the data for the requested statistic.
    String   statName   = statsParameter.getStringValue();
    double[] statValues = jobStats.getStatValues(statName);
    long     updateTime = jobStats.getLastUpdateTime();


    // Generate the page header.
    String link = generateGetJobLink(requestInfo, jobID, jobID);
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Graph In-Progress Results for Job " + link +
                    "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Choose the statistic to graph." + EOL);
    htmlBody.append("<BR>" + EOL);


    // Generate the form that allows the user to specify the information
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_VIEW_GRAPH_REAL_TIME) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          jobID) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + widthParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    widthParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + heightParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    heightParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + statsParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    statsParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                    "VALUE=\"View Graph\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>");
    htmlBody.append("<BR><BR>" + EOL);


    if ((statValues == null) || (statValues.length == 0))
    {
      htmlBody.append("Job " + jobID +
                      " does not have any in-progress data available for " +
                      statName + EOL);
    }
    else if (statValues.length < 2)
    {
      htmlBody.append("Job " + jobID + " does not yet have enough data " +
                      "available for " + statName + " to generate a graph" +
                      EOL);
    }
    else
    {
      try
      {
        // Generate the image tag.  Replace any spaces with the '+' sign.
        String imageURI = servletBaseURI + '?' +
                          Constants.SERVLET_PARAM_SECTION +
                          '=' + Constants.SERVLET_SECTION_JOB + '&' +
                          Constants.SERVLET_PARAM_SUBSECTION + '=' +
                          Constants.SERVLET_SECTION_JOB_GRAPH_REAL_TIME + '&' +
                          Constants.SERVLET_PARAM_JOB_ID + '=' + jobID + '&' +
                          Constants.SERVLET_PARAM_GRAPH_WIDTH + '=' +
                          widthParameter.getIntValue() + '&' +
                          Constants.SERVLET_PARAM_GRAPH_HEIGHT + '=' +
                          heightParameter.getIntValue() + '&' +
                          Constants.SERVLET_PARAM_STAT_TRACKER + '=' +
                          URLEncoder.encode(statsParameter.getStringValue(),
                                            "UTF-8");

        htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                        "\" WIDTH=\"" + widthParameter.getIntValue() +
                        "\" HEIGHT=\"" + heightParameter.getIntValue() +
                        "\" ALT=\"Graph of In-Progress Results for Job " +
                        jobID + "\">" + EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
      }
    }

    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("In-progress statistical data for this job was last " +
                    "updated at " +
                    displayDateFormat.format(new Date(updateTime)) + EOL);
  }



  /**
   * Handles the processing required to actually serve up the dynamically
   * generated images depicting information gathered in real-time.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleRealTimeGraph(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleRealTimeGraph()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      try
      {
        requestInfo.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      } catch (Exception e) { e.printStackTrace(); }
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest  request  = requestInfo.request;
    HttpServletResponse response = requestInfo.response;


    // Get the job ID.  If none was specified, then send back an error response.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e) {}
      return;
    }


    // Get the name of the stat for which to retrieve the data.
    String statName =
         request.getParameter(Constants.SERVLET_PARAM_STAT_TRACKER);
    if ((statName == null) || (statName.length() == 0))
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e) {}
      return;
    }


    // Get the data for the requested statistic.  If an problem occurs, then
    // send back an error response.
    Job      job;
    double[] statData;
    int      startSeconds;
    try
    {
      job = scheduler.getJob(jobID);
      RealTimeJobStats stats = job.getRealTimeStats();
      statData = stats.getStatValues(statName);
      startSeconds = stats.getFirstInterval(statName) *
                     job.getCollectionInterval();
      if ((statData == null) || (statData.length < 2))
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
    }
    catch (Exception e)
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e2) { e2.printStackTrace(); }
      return;
    }


    // Get the graph dimensions.
    int width  = defaultGraphWidth;
    try
    {
      width = Integer.parseInt(request.getParameter(
                                    Constants.SERVLET_PARAM_GRAPH_WIDTH));
    } catch (Exception e) { e.printStackTrace(); }

    int height = defaultGraphHeight;
    try
    {
      height = Integer.parseInt(request.getParameter(
                                     Constants.SERVLET_PARAM_GRAPH_HEIGHT));
    } catch (Exception e) { e.printStackTrace(); }


    // Create the graph.
    StatGrapher grapher = new StatGrapher(width, height,
                                          "In-Progress Results for " +
                                          statName);
    grapher.addDataSet(statData, job.getCollectionInterval(), jobID);
    grapher.setBaseAtZero(true);
    grapher.setIncludeAverage(false);
    grapher.setIncludeRegression(false);
    grapher.setIncludeHorizontalGrid(true);
    grapher.setIncludeVerticalGrid(true);
    grapher.setVerticalAxisTitle("");
    grapher.setStartSeconds(startSeconds);

    try
    {
      BufferedImage image = grapher.generateLineGraph();
      response.setContentType("image/png");
      response.addHeader("Content-Disposition", "filename=\"" +
                         generateGraphFilename(job, statName) + '"');
      ImageEncoder encoder =
           ImageCodec.createImageEncoder("png", response.getOutputStream(),
                                         null);
      encoder.encode(image);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }



  /**
   * Handles the processing required to display graphs that overlay two
   * different stat trackers.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewOverlay(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewOverlay()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.htmlBody;


    // See if information has been posted for this graph.
    boolean posted = false;
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      posted = true;
    }


    // Get the set of job IDs.  There could be multiple, but we don't care
    // about that yet.  If there are multiple, then just deal with the first
    // one.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Job Results</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("No job ID was specified." + EOL);
      htmlBody.append("A job ID is required to indicate which job " +
                      "contains the data to be graphed." + EOL);
      return;
    }


    String jobID;
    String link;
    if (jobIDs.length == 1)
    {
      jobID = "Job " + jobIDs[0];
      link  = "Job " + generateGetJobLink(requestInfo, jobIDs[0], jobIDs[0]);
    }
    else
    {
      jobID = "Multiple Jobs";
      link  = "Multiple Jobs";
    }

    Job job;
    try
    {
      job = configDB.getJob(jobIDs[0]);
      if (job == null)
      {
        throw new SLAMDServerException("No information is known about job " +
                                       jobIDs[0]);
      }
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  " + e.getMessage() + "<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Results for " + jobID + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Unable to retrieve job " + jobIDs[0] +
                      "from the configuration directory." + EOL);
      htmlBody.append("See the error message above for additional " +
                      "information." + EOL);
      return;
    }


    // Get the set of searchable statistics available for this job.  Only
    // searchable statistics may be used in an overlay graph.
    ArrayList<String> nameList = new ArrayList<String>();
    String[] trackerNames = job.getStatTrackerNames();
    if ((trackerNames == null) || (trackerNames.length < 2))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Results for " + link + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The specified job does not contain enough statistical " +
                      "data from which to generate a graph.");
      return;
    }
    for (int i=0; i < trackerNames.length; i++)
    {
      StatTracker[] trackers = job.getStatTrackers(trackerNames[i]);
      if ((trackers != null) && (trackers.length > 0) &&
          trackers[0].isSearchable())
      {
        nameList.add(trackerNames[i]);
      }
    }
    if (nameList.size() < 2)
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Graph Results for " + link + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The specified job does not contain enough statistical " +
                      "data from which to generate a graph.");
      return;
    }
    String[] graphableNames = new String[nameList.size()];
    nameList.toArray(graphableNames);


    // Generate the common parameters that will be used for user input.
    BooleanParameter baseAtZeroParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO,
                              "Base at Zero",
                              "Indicates whether the lower bound for the " +
                              "graph should be based at zero rather than " +
                              "dynamically calculated from the information " +
                              "contained in the data provided.", true);
    BooleanParameter includeLegendParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS,
                              "Include Legend",
                              "Indicates whether the graph generated should " +
                              "include a legend.", false);
    BooleanParameter sameAxisParameter =
         new BooleanParameter(Constants.SERVLET_PARAM_GRAPH_USE_SAME_AXIS,
                              "Graph on Same Axis",
                              "Indicates whether the two statistics should " +
                              "graphed on the same axis rather than separate " +
                              "axes.", false);
    IntegerParameter widthParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_WIDTH,
                              "Graph Width",
                              "The width in pixels of the graph to create",
                              true, defaultGraphWidth, true, 0, false, 0);
    IntegerParameter heightParameter =
         new IntegerParameter(Constants.SERVLET_PARAM_GRAPH_HEIGHT,
                              "Graph Height",
                              "The height in pixels of the graph to create",
                              true, defaultGraphHeight, true, 0, false, 0);
    MultiChoiceParameter leftParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_LEFT_TRACKER,
                                  "Left Axis Statistic",
                                  "The name of the stat tracker to graph " +
                                  "along the left axis.", graphableNames,
                                  graphableNames[0]);
    MultiChoiceParameter rightParameter =
         new MultiChoiceParameter(Constants.SERVLET_PARAM_RIGHT_TRACKER,
                                  "Right Axis Statistic",
                                  "The name of the stat tracker to graph " +
                                  "along the right axis.", graphableNames,
                                  graphableNames[1]);

    // Obtain the values that should be used for the common parameters.
    if (posted)
    {
      try
      {
        baseAtZeroParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  baseAtZeroParameter.getName()));
      } catch (Exception e) {}

      try
      {
        includeLegendParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  includeLegendParameter.getName()));
      } catch (Exception e) {}

      try
      {
        sameAxisParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  sameAxisParameter.getName()));
      } catch (Exception e) {}

      try
      {
        widthParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  widthParameter.getName()));
      } catch (Exception e) {}

      try
      {
        heightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  heightParameter.getName()));
      } catch (Exception e) {}

      try
      {
        leftParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  leftParameter.getName()));
      } catch (Exception e) {}

      try
      {
        rightParameter.htmlInputFormToValue(
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  rightParameter.getName()));
      } catch (Exception e) {}
    }


    // Generate the page header.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Graph Results for " + link + "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Choose the type of information to display." + EOL);
    htmlBody.append("<BR>" + EOL);


    // Generate the form that allows the user to specify the information
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                   Constants.SERVLET_SECTION_JOB_VIEW_OVERLAY) +
                    EOL);

    for (int i=0; i < jobIDs.length; i++)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                            jobIDs[i]) + EOL);
    }

    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + widthParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    widthParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + heightParameter.getDisplayName() + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    heightParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + leftParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    leftParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + rightParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    rightParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + sameAxisParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    sameAxisParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + includeLegendParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    includeLegendParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>" + baseAtZeroParameter.getDisplayName() +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>" +
                    baseAtZeroParameter.getHTMLInputForm(
                         Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                    "</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                    "VALUE=\"View Graph\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>");


    // Generate the image tag.  Replace any spaces with the '+' sign.
    try
    {
      htmlBody.append("<BR><BR>" + EOL);
      String imageURI = servletBaseURI + '?' + Constants.SERVLET_PARAM_SECTION +
                        '=' + Constants.SERVLET_SECTION_JOB + '&' +
                        Constants.SERVLET_PARAM_SUBSECTION + '=' +
                        Constants.SERVLET_SECTION_JOB_OVERLAY + '&';

      for (int i=0; i < jobIDs.length; i++)
      {
        imageURI += Constants.SERVLET_PARAM_JOB_ID + '=' + jobIDs[i] + '&';
      }

      imageURI += Constants.SERVLET_PARAM_GRAPH_WIDTH + '=' +
                  widthParameter.getIntValue() + '&' +
                  Constants.SERVLET_PARAM_GRAPH_HEIGHT + '=' +
                  heightParameter.getIntValue() + '&' +
                  Constants.SERVLET_PARAM_LEFT_TRACKER + '=' +
                  URLEncoder.encode(leftParameter.getStringValue(), "UTF-8") +
                  '&' + Constants.SERVLET_PARAM_RIGHT_TRACKER + '=' +
                  URLEncoder.encode(rightParameter.getStringValue(), "UTF-8") +
                  '&' + Constants.SERVLET_PARAM_GRAPH_USE_SAME_AXIS + '=' +
                  sameAxisParameter.getValueString() + '&' +
                  Constants.SERVLET_PARAM_INCLUDE_LABELS + '=' +
                  includeLegendParameter.getValueString() + '&' +
                  Constants.SERVLET_PARAM_BASE_AT_ZERO + '=' +
                  baseAtZeroParameter.getValueString();

      htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                      "\" WIDTH=\"" + widthParameter.getIntValue() +
                      "\" HEIGHT=\"" + heightParameter.getIntValue() +
                      "\" ALT=\"Graph of Results for " + jobID + "\">" + EOL);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
    }
  }



  /**
   * Handles the processing required to actually serve up the dynamically
   * generated overlay graphs depicting information gathered during job
   * processing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleOverlay(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleOverlay()");

    // The user must be able to view job information in order to see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      try
      {
        requestInfo.response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      } catch (Exception e) { e.printStackTrace(); }
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest  request  = requestInfo.request;
    HttpServletResponse response = requestInfo.response;


    // Get the graph parameters.
    int width  = Constants.DEFAULT_GRAPH_WIDTH;
    try
    {
      width = Integer.parseInt(request.getParameter(
                                    Constants.SERVLET_PARAM_GRAPH_WIDTH));
    } catch (Exception e) { e.printStackTrace(); }

    int height = Constants.DEFAULT_GRAPH_HEIGHT;
    try
    {
      height = Integer.parseInt(request.getParameter(
                                     Constants.SERVLET_PARAM_GRAPH_HEIGHT));
    } catch (Exception e) { e.printStackTrace(); }

    boolean useSameAxis = false;
    try
    {
      String sameAxisStr =
           request.getParameter(Constants.SERVLET_PARAM_GRAPH_USE_SAME_AXIS);
      sameAxisStr = sameAxisStr.toLowerCase();
      useSameAxis = (sameAxisStr.equals("true") ||
                     sameAxisStr.equals("yes") || sameAxisStr.equals("on") ||
                     sameAxisStr.equals("1"));
    } catch (Exception e) { e.printStackTrace(); }

    boolean includeLegend = false;
    try
    {
      String legendStr =
           request.getParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
      legendStr = legendStr.toLowerCase();
      includeLegend = (legendStr.equals("true") || legendStr.equals("yes") ||
                       legendStr.equals("on") || legendStr.equals("1"));
    } catch (Exception e) { e.printStackTrace(); }

    boolean baseAtZero = true;
    try
    {
      String baseAtZeroStr =
           request.getParameter(Constants.SERVLET_PARAM_BASE_AT_ZERO);
      baseAtZeroStr = baseAtZeroStr.toLowerCase();
      baseAtZero = (! (baseAtZeroStr.equals("false") ||
                       baseAtZeroStr.equals("no") ||
                       baseAtZeroStr.equals("off") ||
                       baseAtZeroStr.equals("0")));
    } catch (Exception e) { e.printStackTrace(); }

    String leftTrackerName =
                request.getParameter(Constants.SERVLET_PARAM_LEFT_TRACKER);
    String rightTrackerName =
                request.getParameter(Constants.SERVLET_PARAM_RIGHT_TRACKER);


    // Get the job IDs.  If none were specified, then send back an error
    // response.  If one was provided, then generate a regular graph.  If
    // multiple job IDs were provided, then generate a graph comparing the
    // results of those jobs.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      try
      {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (Exception e) {}
      return;
    }
    else if (jobIDs.length == 1)
    {
      String jobID = jobIDs[0];
      Job job = null;
      try
      {
        job = configDB.getJob(jobID);
      } catch (Exception e) { e.printStackTrace(); }
      if (job == null)
      {
        try
        {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) { e.printStackTrace(); }
        return;
      }


      // Get the statistics to include on the left and right axes.
      StatTracker[] leftTrackers  = job.getStatTrackers(leftTrackerName);
      StatTracker[] rightTrackers = job.getStatTrackers(rightTrackerName);
      if ((leftTrackers == null) || (leftTrackers.length == 0) ||
          (rightTrackers == null) || (rightTrackers.length == 0))
      {
        return;
      }


      // Aggregate the tracker data for the left and right trackers.
      StatTracker leftTracker = leftTrackers[0].newInstance();
      leftTracker.aggregate(leftTrackers);
      StatTracker rightTracker = rightTrackers[0].newInstance();
      rightTracker.aggregate(rightTrackers);


      // Get the values to include in the graph for the left and right
      // trackers.
      double[] leftData  = leftTracker.getGraphData();
      double[] rightData = rightTracker.getGraphData();


      // Create and initialize the stat tracker.
      String caption = leftTracker.getDisplayName() + " and " +
                       rightTracker.getDisplayName() + " for Job " +
                       job.getJobID();
      StatGrapher grapher = new StatGrapher(width, height, caption);
      grapher.setBaseAtZero(baseAtZero);
      grapher.setIncludeLegend(includeLegend, "Statistic");


      // Generate the graph.
      try
      {
        BufferedImage image = grapher.generateDualLineGraph(
                                   leftTracker.getDisplayName(),
                                   leftTracker.getAxisLabel(), leftData,
                                   leftTracker.getCollectionInterval(),
                                   rightTracker.getDisplayName(),
                                   rightTracker.getAxisLabel(), rightData,
                                   rightTracker.getCollectionInterval(),
                                   useSameAxis, "Elapsed Time (seconds)", null);
        response.setContentType("image/png");
        response.addHeader("Content-Disposition", "filename=\"" +
                           generateGraphFilename(job, leftTracker,
                                                 rightTracker) + '"');
        ImageEncoder encoder =
             ImageCodec.createImageEncoder("png", response.getOutputStream(),
                                           null);
        encoder.encode(image);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    else
    {
      Job[] jobs = new Job[jobIDs.length];
      for (int i=0; i < jobs.length; i++)
      {
        try
        {
          jobs[i] = configDB.getJob(jobIDs[i]);
        } catch (Exception e) { e.printStackTrace(); }
        if (jobs[i] == null)
        {
          try
          {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
          } catch (Exception e) { e.printStackTrace(); }
          return;
        }
      }


      // Get the data to be graphed for each job.
      double[] leftData       = new double[jobs.length];
      double[] rightData      = new double[jobs.length];
      String   leftAxisTitle  = "";
      String   rightAxisTitle = "";
      for (int i=0; i < jobs.length; i++)
      {
        StatTracker[] trackers = jobs[i].getStatTrackers(leftTrackerName);
        if ((trackers == null) || (trackers.length == 0))
        {
          leftData[i] = Double.NaN;
        }
        else
        {
          StatTracker tracker = trackers[0].newInstance();
          tracker.aggregate(trackers);
          leftData[i] = tracker.getSummaryValue();
          leftAxisTitle = tracker.getAxisLabel();
        }

        trackers = jobs[i].getStatTrackers(rightTrackerName);
        if ((trackers == null) || (trackers.length == 0))
        {
          rightData[i] = Double.NaN;
        }
        else
        {
          StatTracker tracker = trackers[0].newInstance();
          tracker.aggregate(trackers);
          rightData[i] = tracker.getSummaryValue();
          rightAxisTitle = tracker.getAxisLabel();
        }
      }


      // Create and initialize the stat tracker.
      String caption = leftTrackerName + " and " + rightTrackerName;
      StatGrapher grapher = new StatGrapher(width, height, caption);
      grapher.setBaseAtZero(baseAtZero);
      grapher.setIncludeLegend(includeLegend, "Statistic");
      grapher.setIgnoreZeroValues(true);


      // Generate the graph.
      try
      {
        BufferedImage image = grapher.generateDualLineGraph(
                                   leftTrackerName, leftAxisTitle, leftData, 1,
                                   rightTrackerName, rightAxisTitle, rightData,
                                   1, useSameAxis, "Job Number", jobIDs);
        response.setContentType("image/png");
        response.addHeader("Content-Disposition", "filename=\"" +
                           generateGraphFilename(jobs, leftTrackerName,
                                                 rightTrackerName) + '"');
        ImageEncoder encoder =
             ImageCodec.createImageEncoder("png", response.getOutputStream(),
                                           null);
        encoder.encode(image);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }



  /**
   * Constructs a filename that can be used for the name of a graph generated
   * from the provided job and stat tracker.
   *
   * @param  job      The job from which the graph is being generated.
   * @param  tracker  A stat tracker that provides information about the
   *                  statistic being graphed.
   *
   * @return  The filename that can be used for the generated graph.
   */
  static String generateGraphFilename(Job job, StatTracker tracker)
  {
    String nameStr = job.getJobName() + '_' + tracker.getDisplayName() + '_' +
                     job.getJobID() + ".png";
    nameStr = nameStr.replace(' ', '_').replace('/', '_').replace(',', '_');
    return nameStr;
  }



  /**
   * Constructs a filename that can be used for the name of a graph generated
   * from the provided set of jobs and stat tracker.
   *
   * @param  jobs      The jobs from which the graph is being generated.
   * @param  tracker   A stat tracker that provides information about the
   *                   statistic being graphed.
   *
   * @return  The filename that can be used for the generated graph.
   */
  static String generateGraphFilename(Job[] jobs, StatTracker tracker)
  {
    String nameStr = jobs[0].getJobName() + "_comparison_of_" +
                     tracker.getDisplayName() + ".png";
    nameStr = nameStr.replace(' ', '_').replace('/', '_').replace(',', '_');
    return nameStr;
  }



  /**
   * Constructs a filename that can be used for the name of a graph generated
   * from the provided job and stat trackers.
   *
   * @param  job       The job from which the graph is being generated.
   * @param  tracker1  The first stat tracker included in the overlay graph.
   * @param  tracker2  The second stat tracker included in the overlay graph.
   *
   * @return  The filename that can be used for the generated graph.
   */
  static String generateGraphFilename(Job job, StatTracker tracker1,
                                       StatTracker tracker2)
  {
    String nameStr = job.getJobName() + "_comparison_of_" +
                     tracker1.getDisplayName() + "_and_" +
                     tracker2.getDisplayName() + '_' + job.getJobID() + ".png";
    nameStr = nameStr.replace(' ', '_').replace('/', '_').replace(',', '_');
    return nameStr;
  }



  /**
   * Constructs a filename that can be used for the name of a graph generated
   * from the provided job and stat tracker names.
   *
   * @param  jobs          The jobs from which the graph is being generated.
   * @param  trackerName1  The name of the first stat tracker included in the
   *                       overlay graph.
   * @param  trackerName2  The name of the second stat tracker included in the
   *                       overlay graph.
   *
   * @return  The filename that can be used for the generated graph.
   */
  static String generateGraphFilename(Job[] jobs, String trackerName1,
                                       String trackerName2)
  {
    String nameStr = "multiple_" + jobs[0].getJobName() +
                     "_jobs_comparison_of_" + trackerName1 + "_and_" +
                     trackerName2 + ".png";
    nameStr = nameStr.replace(' ', '_').replace('/', '_').replace(',', '_');
    return nameStr;
  }



  /**
   * Constructs a filename that can be used for the name of a graph generated
   * from the provided job and stat name.
   *
   * @param  job       The job from which the graph is being generated.
   * @param  statName  The name of the statistic being graphed.
   *
   * @return  The filename that can be used for the generated graph.
   */
  static String generateGraphFilename(Job job, String statName)
  {
    String nameStr = job.getJobName() + '_' + statName + '_' + job.getJobID() +
                     ".png";
    nameStr = nameStr.replace(' ', '_').replace('/', '_').replace(',', '_');
    return nameStr;
  }




  /**
   * Handles all processing related to scheduling a new job for execution.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleScheduleJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleScheduleJob()");

    // The user must be able to schedule new jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "schedule jobs for execution");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request     = requestInfo.request;
    StringBuilder       htmlBody    = requestInfo.htmlBody;


    // See if a job class name has been provided.  If so, then generate a form
    // to schedule that job.  If not, then allow the user to choose the job
    // class.
    String jobClassName =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if ((jobClassName == null) || (jobClassName.length() == 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Schedule a New Job</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      JobClass[][] categorizedClasses = slamdServer.getCategorizedJobClasses();
      if ((categorizedClasses != null) && (categorizedClasses.length > 0))
      {
        htmlBody.append("Choose the type of job to schedule:" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        for (int i=0; i < categorizedClasses.length; i++)
        {
          String categoryName = categorizedClasses[i][0].getJobCategoryName();
          if (categoryName == null)
          {
            categoryName = "Unclassified";
          }

          htmlBody.append("<B>" + categoryName + " Job Classes</B><BR>" + EOL);
          htmlBody.append("<UL>" + EOL);

          for (int j=0; j < categorizedClasses[i].length; j++)
          {
            String[] names =
            {
              Constants.SERVLET_PARAM_JOB_CLASS
            };

            String[] values =
            {
              categorizedClasses[i][j].getClass().getName()
            };

            String link = generateLink(requestInfo,
                 Constants.SERVLET_SECTION_JOB,
                 Constants.SERVLET_SECTION_JOB_SCHEDULE, names, values,
                 categorizedClasses[i][j].getShortDescription(),
                 categorizedClasses[i][j].getJobName());
            htmlBody.append("  <LI>" + link + " (" +
                 categorizedClasses[i][j].getClass().getName() + ")</LI>" +
                 EOL);
          }

          htmlBody.append("</UL>" + EOL);
          htmlBody.append("<BR>" + EOL);
        }
      }
      else
      {
        htmlBody.append("No job classes have been defined in the SLAMD " +
                        "server." + EOL);
      }
    }
    else
    {
      JobClass jobClass = slamdServer.getJobClass(jobClassName);
      if (jobClass == null)
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Schedule a New Job</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Job class " + jobClassName +
                        " is not defined in the SLAMD server." + EOL);
      }
      else
      {
        boolean showAdvanced    = alwaysShowAdvancedOptions;
        boolean advancedClicked = false;
        String advStr =
             request.getParameter(Constants.SERVLET_PARAM_SHOW_ADVANCED);
        if ((advStr != null) && (advStr.length() > 0))
        {
          advancedClicked = true;
          showAdvanced    = true;
        }
        else if (alwaysShowAdvancedOptions)
        {
          advStr = "1";
        }

        String validateStr =
                    request.getParameter(
                         Constants.SERVLET_PARAM_JOB_VALIDATE_SCHEDULE);
        if ((validateStr == null) || (validateStr.length() == 0) ||
            ((advStr != null) && (! advStr.equals("1"))))
        {
          int numCopies = 1;
          String copyStr =
               request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_COPIES);
          if (copyStr != null)
          {
            try
            {
              numCopies = Integer.parseInt(copyStr);
            } catch (Exception e) {}
          }

          boolean makeInterDependent = false;
          String interDependStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT);
          if (interDependStr != null)
          {
            makeInterDependent = (interDependStr.equalsIgnoreCase("true") ||
                                  interDependStr.equalsIgnoreCase("yes") ||
                                  interDependStr.equalsIgnoreCase("on") ||
                                  interDependStr.equalsIgnoreCase("1"));
          }

          int delay = 0;
          String delayStr = request.getParameter(
                                 Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
          if (delayStr != null)
          {
            try
            {
              delay = Integer.parseInt(delayStr);
            } catch (Exception e) {}
          }

          String descStr = request.getParameter(
                                Constants.SERVLET_PARAM_JOB_DESCRIPTION);

          Date startTime = null;
          String startTimeStr = request.getParameter(
                                     Constants.SERVLET_PARAM_JOB_START_TIME);
          if (startTimeStr != null)
          {
            try
            {
              startTime = dateFormat.parse(startTimeStr);
            } catch (Exception e) {}
          }

          Date stopTime = null;
          String stopTimeStr = request.getParameter(
                                    Constants.SERVLET_PARAM_JOB_STOP_TIME);
          if (stopTimeStr != null)
          {
            try
            {
              stopTime = dateFormat.parse(stopTimeStr);
            } catch (Exception e) {}
          }

          int duration = -1;
          String durationStr = request.getParameter(
                                    Constants.SERVLET_PARAM_JOB_DURATION);
          if (durationStr != null)
          {
            try
            {
              duration = DurationParser.parse(durationStr);
            } catch (Exception e) {}
          }

          int numClients = -1;
          String numClientsStr = request.getParameter(
                                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
          if (numClientsStr != null)
          {
            try
            {
              numClients = Integer.parseInt(numClientsStr);
            } catch (Exception e) {}
          }

          int numThreads = -1;
          String threadStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
          if (threadStr != null)
          {
            try
            {
              numThreads = Integer.parseInt(threadStr);
            } catch (Exception e) {}
          }

          int threadStartupDelay = 0;
          threadStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
          if (threadStr != null)
          {
            try
            {
              threadStartupDelay = Integer.parseInt(threadStr);
            } catch (Exception e) {}
          }

          String clientsStr =
               request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
          String[] clients;
          if ((clientsStr == null) || (clientsStr.length() == 0))
          {
            clients = new String[0];
          }
          else
          {
            ArrayList<String> clientList = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(clientsStr);
            while (tokenizer.hasMoreTokens())
            {
              clientList.add(tokenizer.nextToken());
            }
            clients = new String[clientList.size()];
            clientList.toArray(clients);
          }

          clientsStr = request.getParameter(
                            Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
          String[] monitorClients;
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
              clientList.add(tokenizer.nextToken());
            }
            monitorClients = new String[clientList.size()];
            clientList.toArray(monitorClients);
          }

          boolean monitorClientsIfAvailable = false;
          String monitorStr = request.getParameter(
               Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
          if (monitorStr != null)
          {
            monitorClientsIfAvailable = (monitorStr.equals("1") ||
                                         monitorStr.equalsIgnoreCase("true") ||
                                         monitorStr.equalsIgnoreCase("yes") ||
                                         monitorStr.equalsIgnoreCase("on"));
          }

          boolean waitForClients;
          if (validateStr == null)
          {
            waitForClients = true;
            String waitStr = request.getParameter(
                                  Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS);
            if (waitStr != null)
            {
              waitForClients = (! (waitStr.equals("0") ||
                                   waitStr.equalsIgnoreCase("false") ||
                                   waitStr.equalsIgnoreCase("off") ||
                                   waitStr.equalsIgnoreCase("no")));
            }
            else
            {
              waitForClients = true;
            }
          }
          else
          {
            waitForClients = false;
            String waitStr = request.getParameter(
                                  Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS);
            if (waitStr != null)
            {
              waitForClients = (waitStr.equals("1") ||
                                waitStr.equalsIgnoreCase("true") ||
                                waitStr.equalsIgnoreCase("on") ||
                                waitStr.equalsIgnoreCase("yes"));
            }
            else
            {
              waitForClients = false;
            }
          }

          String[] dependencyIDs = request.getParameterValues(
                                        Constants.SERVLET_PARAM_JOB_DEPENDENCY);

          String[] notifyAddresses;
          String notifyAddressStr =
               request.getParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS);
          if ((notifyAddressStr == null) || (notifyAddressStr.length() == 0))
          {
            notifyAddresses = new String[0];
          }
          else
          {
            ArrayList<String> addressList = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(notifyAddressStr, ", ");
            while (st.hasMoreTokens())
            {
              addressList.add(st.nextToken());
            }
            notifyAddresses = new String[addressList.size()];
            addressList.toArray(notifyAddresses);
          }

          int collectionInterval = -1;
          String intervalStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
          if (intervalStr != null)
          {
            try
            {
              collectionInterval = DurationParser.parse(intervalStr);
            } catch (Exception e) {}
          }

          String disabledStr =
               request.getParameter(Constants.SERVLET_PARAM_JOB_DISABLED);
          boolean jobDisabled = ((disabledStr != null) &&
                                 (disabledStr.equalsIgnoreCase("true") ||
                                  disabledStr.equalsIgnoreCase("on") ||
                                  disabledStr.equalsIgnoreCase("yes") ||
                                  disabledStr.equalsIgnoreCase("1")));

          String displayStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
          boolean displayInReadOnlyMode = false;
          if (displayStr != null)
          {
            displayInReadOnlyMode = displayStr.equalsIgnoreCase("true") ||
                                    displayStr.equalsIgnoreCase("yes") ||
                                    displayStr.equalsIgnoreCase("on") ||
                                    displayStr.equalsIgnoreCase("1");
          }

          String comments =
               request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);

          Parameter[] jobParams =
               jobClass.getParameterStubs().clone().getParameters();
          for (int i=0; i < jobParams.length; i++)
          {
            try
            {
              String[] values = request.getParameterValues(
                                     Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                                     jobParams[i].getName());
              if (values != null)
              {
                jobParams[i].htmlInputFormToValue(values);
              }
              else if (advancedClicked &&
                       (jobParams[i] instanceof BooleanParameter))
              {
                // This means that the form was posted and the box was not
                // checked, so make sure the parameter has a value of "false".
                ((BooleanParameter) jobParams[i]).setValue(false);
              }
            } catch (Exception e) {}
          }


          generateScheduleJobForm(requestInfo, jobClass, numCopies,
                                  makeInterDependent, delay, null, descStr,
                                  startTime, stopTime, duration, numClients,
                                  numThreads, threadStartupDelay, clients,
                                  monitorClients, monitorClientsIfAvailable,
                                  waitForClients, dependencyIDs,
                                  notifyAddresses, collectionInterval, comments,
                                  jobDisabled, displayInReadOnlyMode,
                                  new ParameterList(jobParams));
        }
        else
        {
          validateJobInfo(requestInfo);
        }
      }
    }
  }



  /**
   * Handles all processing required to be able to clone a job (i.e., schedule
   * a new job using the settings taken from an existing job).
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleCloneJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleCloneJob()");

    // The user must be able to schedule new jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "schedule jobs for execution");
      return;
    }


    // Get the important state variables.
    HttpServletRequest request     = requestInfo.request;
    StringBuilder       infoMessage = requestInfo.infoMessage;


    // See if a job ID has been specified.  If not, then just treat it like
    // scheduling a new job.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      handleScheduleJob(requestInfo);
    }
    else
    {
      // See if the specified job exists.  If so, then use information from
      // it.  If not, then set an info message and schedule a new job.
      try
      {
        Job job = scheduler.getJob(jobID);
        if (job == null)
        {
          infoMessage.append("Unable to retrieve information about job " +
                             jobID + "<BR>" + EOL);
          handleScheduleJob(requestInfo);
        }
        else
        {
          generateScheduleJobForm(requestInfo, job.getJobClass(), 1, false, 0,
                                  job.getFolderName(), job.getJobDescription(),
                                  null, null, job.getDuration(),
                                  job.getNumberOfClients(),
                                  job.getThreadsPerClient(),
                                  job.getThreadStartupDelay(),
                                  job.getRequestedClients(),
                                  job.getResourceMonitorClients(),
                                  job.monitorClientsIfAvailable(),
                                  job.waitForClients(), null,
                                  job.getNotifyAddresses(),
                                  job.getCollectionInterval(),
                                  job.getJobComments(), false,
                                  job.displayInReadOnlyMode(),
                                  job.getParameterList());
        }
      }
      catch (SLAMDServerException sse)
      {
        infoMessage.append("Unable to retrieve information about job " +
                           jobID + ":  " + sse.getMessage() + "<BR>" + EOL);
        handleScheduleJob(requestInfo);
      }
    }
  }



  /**
   * Generates the HTML form that may be used to specify all of the necessary
   * information required to schedule a new job for execution.  If any
   * information is provided through the arguments to this method, then the
   * form fields will be populated with the relevant information.
   *
   * @param  requestInfo                The state information for this request.
   * @param  jobClass                   The Java class that is to be used to run
   *                                    the job.
   * @param  numCopies                  The number of copies to make of the
   *                                    scheduled job.
   * @param  makeInterDependent         Indicates whether to make multiple
   *                                    copies of the job interdependent.
   * @param  delayBetweenStarts         The delay in seconds between the start
   *                                    of one job from the start of the next
   *                                    copy.
   * @param  folderName                 The name of the folder in which this job
   *                                    should be placed by default.
   * @param  jobDescription             A freeform text description that can
   *                                    provide additional information about the
   *                                    purpose of this job.
   * @param  startTime                  The time at which the job should start
   *                                    running.
   * @param  stopTime                   The time at which the job should stop
   *                                    running.
   * @param  duration                   The maximum length of time in seconds
   *                                    that the job should be allowed to run.
   * @param  numClients                 The number of clients on which the job
   *                                    should be run.
   * @param  threadsPerClient           The number of threads to create on each
   *                                    client to process the job.
   * @param  threadStartupDelay         The delay in milliseconds that should be
   *                                    used between starting the individual
   *                                    threads on the client.
   * @param  clients                    The set of clients that have been
   *                                    requested for the job.
   * @param  monitorClients             The set of resource monitor clients that
   *                                    have been requested for the job.
   * @param  monitorClientsIfAvailable  Indicates whether to automatically
   *                                    monitor systems used to run job clients
   *                                    if resource monitor clients are
   *                                    available.
   * @param  waitForClients             Indicates whether the scheduler should
   *                                    wait for a sufficient set of clients to
   *                                    become available before trying to start
   *                                    the job.
   * @param  dependencyIDs              The job IDs of any jobs that must be
   *                                    completed before this job may be
   *                                    started.
   * @param  notifyAddresses            The e-mail addresses of the users to
   *                                    notify when the job has been completed.
   * @param  collectionInterval         The length of time in seconds that will
   *                                    be used as the statistics collection
   *                                    interval.
   * @param  comments                   A set of comments to use for the job.
   * @param  jobDisabled                Indicates whether the job should be
   *                                    disabled when it is scheduled.
   * @param  displayInReadOnlyMode      Indicates whether the job should be made
   *                                    visible if the server is operating in
   *                                    restricted read-only mode.
   * @param  parameters                 The set of parameters with information
   *                                    that is to be used during job
   *                                    processing.
   */
  static void generateScheduleJobForm(RequestInfo requestInfo,
                                      JobClass jobClass, int numCopies,
                                      boolean makeInterDependent,
                                      int delayBetweenStarts, String folderName,
                                      String jobDescription, Date startTime,
                                      Date stopTime, int duration,
                                      int numClients, int threadsPerClient,
                                      int threadStartupDelay, String[] clients,
                                      String[] monitorClients,
                                      boolean monitorClientsIfAvailable,
                                      boolean waitForClients,
                                      String[] dependencyIDs,
                                      String[] notifyAddresses,
                                      int collectionInterval, String comments,
                                      boolean jobDisabled,
                                      boolean displayInReadOnlyMode,
                                      ParameterList parameters)
  {
    logMessage(requestInfo, "In generateScheduleJobForm(" +
               jobClass.getJobName() + ')');


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;


    // See if the job class is deprecated.  If so, then add a warning message at
    // the top of the page.
    StringBuilder deprecatedMessage = new StringBuilder();
    if (jobClass.isDeprecated(deprecatedMessage))
    {
      StringBuilder infoMessage = requestInfo.infoMessage;
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
                    "\">Schedule a New \"" + jobClass.getJobName() +
                    "\" Job</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    for (String s : jobClass.getLongDescription())
    {
      htmlBody.append(s + EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }
    htmlBody.append("Enter the following information about the " +
                    jobClass.getJobName() + " job." + EOL);
    htmlBody.append("Note that parameters marked with an asterisk (" +
                    star + ") are required to have a value." + EOL);
    String link = generateNewWindowLink(requestInfo,
                       Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_SCHEDULE_HELP,
                       Constants.SERVLET_PARAM_JOB_CLASS,
                       jobClass.getClass().getName(),
                       "Click here for help regarding these parameters");
    htmlBody.append(link + '.' + EOL);

    htmlBody.append("<BR><BR>" + EOL);


    // See if we should show the advanced scheduling info.
    boolean showAdvanced = alwaysShowAdvancedOptions;
    String advancedStr =
         request.getParameter(Constants.SERVLET_PARAM_SHOW_ADVANCED);
    if ((advancedStr != null) && (advancedStr.length() > 0))
    {
      showAdvanced = true;
    }


    int jobNumClients = jobClass.overrideNumClients();
    int jobNumThreads = jobClass.overrideThreadsPerClient();
    int jobInterval   = jobClass.overrideCollectionInterval();


    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_SCHEDULE) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                jobClass.getClass().getName()) + EOL);
    htmlBody.append("  " + generateHidden(
                                Constants.SERVLET_PARAM_JOB_VALIDATE_SCHEDULE,
                                "1") + EOL);

    if (jobNumClients > 0)
    {
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                                  String.valueOf(jobNumClients)) + EOL);
    }

    if (jobNumThreads > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT,
                           String.valueOf(jobNumThreads)) + EOL);
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

    if (showAdvanced)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SHOW_ADVANCED,
                                     "1") + EOL);
    }
    else
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_NUM_COPIES,
                                     String.valueOf(numCopies)) + EOL);
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT,
                           String.valueOf(makeInterDependent)) + EOL);

      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS,
                           String.valueOf(delayBetweenStarts)) + EOL);

      StringBuilder clientBuffer = new StringBuilder();
      String       separator = "";
      for (int i=0; ((clients != null) && (i < clients.length)); i++)
      {
        clientBuffer.append(separator);
        clientBuffer.append(clients[i]);
        separator = " ";
      }
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLIENTS,
                                            clientBuffer.toString()) + EOL);

      StringBuilder monitorClientBuffer = new StringBuilder();
      separator    = "";
      for (int i=0; ((monitorClients != null) && (i < monitorClients.length));
           i++)
      {
        monitorClientBuffer.append(separator);
        monitorClientBuffer.append(monitorClients[i]);
        separator = " ";
      }
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS,
                                  monitorClientBuffer.toString()) + EOL);

      if ((notifyAddresses != null) && (notifyAddresses.length > 0))
      {
        String notifyStr = notifyAddresses[0];
        for (int i=1; i < notifyAddresses.length; i++)
        {
          notifyStr += ", " + notifyAddresses[i];
        }

        htmlBody.append("  " + generateHidden(
                                    Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS,
                                    notifyStr) + EOL);
      }

      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY,
                           String.valueOf(threadStartupDelay)) + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                           Constants.SERVLET_PARAM_SHOW_ADVANCED +
                           "\" VALUE=\"Show Advanced Scheduling Options\">" +
                           "<BR>" + EOL);
    }

    htmlBody.append("<TABLE BORDER=\"0\">" + EOL);

    if (showAdvanced)
    {
      // Indicate whether the job should be disabled.
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Job Is Disabled</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DISABLED + '"' +
                      (jobDisabled ? " CHECKED" : "") + "></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // Indicate whether the job should be displayed in restricted read-only
      // mode.
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Display In Read-Only Mode</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY + '"' +
                      (displayInReadOnlyMode ? " CHECKED" : "") + "></TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);


      // The number of copies.
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Number of Copies</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_COPIES + "\" VALUE=\"" +
                      numCopies + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // Whether to make copies interdependent.
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Make Copies Interdependent</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT + '"' +
                      (makeInterDependent ? " CHECKED" : "") + "></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // The delay between the start of each copy.
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Time Between Copy Startups</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS +
                      "\" VALUE=\"" + delayBetweenStarts +
                      "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // The name of the folder in which the job should be placed.
    JobFolder[] folders = null;
    try
    {
      folders = configDB.getFolders();
    } catch (DatabaseException de) {}
    if ((folders != null) && (folders.length > 0))
    {
      if (folderName == null)
      {
        folderName = request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      }

      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Place in Folder</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + EOL);
      htmlBody.append("      <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        if ((folderName != null) &&
            folderName.equalsIgnoreCase(folders[i].getFolderName()))
        {
          htmlBody.append("        <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\" SELECTED>" +
                          folders[i].getFolderName() + EOL);
        }
        else
        {
          htmlBody.append("        <OPTION VALUE=\"" +
                          folders[i].getFolderName() + "\">" +
                          folders[i].getFolderName() + EOL);
        }
      }
      htmlBody.append("      </SELECT>");
      htmlBody.append("    </TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // The job description
    String value = jobDescription;
    if (value == null)
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Description</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    value + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The start time
    if (startTime != null)
    {
      value = dateFormat.format(startTime);
    }
    else
    {
      if (populateStartTime)
      {
        value = dateFormat.format(new Date());
      }
      else
      {
        value = "";
      }
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Start Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_START_TIME + "\" VALUE=\"" +
                    value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The stop time
    if (stopTime != null)
    {
      value = dateFormat.format(stopTime);
    }
    else
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Stop Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_STOP_TIME + "\" VALUE=\"" +
                    value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The duration
    if (duration > 0)
    {
      value = secondsToHumanReadableDuration(duration);
    }
    else
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of clients
    if (jobNumClients <= 0)
    {
      if (numClients > 0)
      {
        value = String.valueOf(numClients);
      }
      else
      {
        value = "";
      }
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      value + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // Specific clients to use.
    if (showAdvanced)
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Use Specific Clients</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_CLIENTS + "\" ROWS=\"5\"" +
                      " COLS=\"40\">");
      String separator = "";
      for (int i=0; ((clients != null) && (i < clients.length)); i++)
      {
        htmlBody.append(separator);
        htmlBody.append(clients[i]);
        separator = EOL;
      }
      htmlBody.append("</TEXTAREA></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);


      // Resource monitor clients to use.
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Resource Monitor Clients</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS +
                      "\" ROWS=\"5\"" + " COLS=\"40\">");
      separator = "";
      for (int i=0; ((monitorClients != null) && (i < monitorClients.length));
           i++)
      {
        htmlBody.append(separator);
        htmlBody.append(monitorClients[i]);
        separator = EOL;
      }
      htmlBody.append("</TEXTAREA></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // Whether to automatically monitor client systems if available.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE +
                    '"' + (monitorClientsIfAvailable ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Whether to wait for clients to be available.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Wait for Available Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS +
                    '"' + (waitForClients ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of threads per client
    if (jobNumThreads <= 0)
    {
      if (threadsPerClient > 0)
      {
        value = String.valueOf(threadsPerClient);
      }
      else
      {
        value = "";
      }
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Threads per Client " + star + "</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT +
                      "\" VALUE=\"" + value + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    if (showAdvanced)
    {
      // The thread startup delay
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Thread Startup Delay (ms)</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                      "\" VALUE=\"" + threadStartupDelay +
                      "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      // The job dependencies
      Job[] pendingJobs    = scheduler.getPendingJobs();
      Job[] runningJobs    = scheduler.getRunningJobs();

      OptimizingJob[] optimizingJobs = new OptimizingJob[0];
      try
      {
        optimizingJobs = scheduler.getUncompletedOptimizingJobs();
      }
      catch (SLAMDServerException sse)
      {
        requestInfo.infoMessage.append("ERROR:  Unable to retrieve the list " +
                                       "of uncompleted optimizing jobs -- " +
                                       sse + "<BR>" + EOL);
      }

      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + EOL);

      for (int i=0; ((dependencyIDs != null) &&
                     (i < dependencyIDs.length)); i++)
      {
        if ((dependencyIDs[i] == null) || (dependencyIDs[i].length() == 0))
        {
          continue;
        }

        htmlBody.append("      <SELECT NAME=\"" +
                        Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);

        Job dependentJob = null;
        try
        {
          dependentJob = configDB.getJob(dependencyIDs[i]);
        } catch (Exception e) {}

        if (dependentJob == null)
        {
          htmlBody.append("        <OPTION VALUE=\"" + dependencyIDs[i] +
                          "\">" + dependencyIDs[i] + " -- Unknown Job" + EOL);
        }
        else
        {
          String description = dependentJob.getJobDescription();
          if (description == null)
          {
            description = "";
          }
          else if (description.length() > 0)
          {
            description = " -- " + description;
          }
          htmlBody.append("        <OPTION VALUE=\"" + dependencyIDs[i] +
                          "\">" + dependencyIDs[i] + " -- " +
                          dependentJob.getJobName() + description + EOL);
        }

        htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);

        for (int j=0; j < pendingJobs.length; j++)
        {
          String description = pendingJobs[j].getJobDescription();
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
          String description = runningJobs[j].getJobDescription();
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
          String description = optimizingJobs[j].getDescription();
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
        htmlBody.append("      <BR>" + EOL);
      }

      htmlBody.append("      <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);
      htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);
      for (int j=0; j < pendingJobs.length; j++)
      {
        String description = pendingJobs[j].getJobDescription();
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
        String description = runningJobs[j].getJobDescription();
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
        String description = optimizingJobs[j].getDescription();
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


      if (slamdServer.getMailer().isEnabled())
      {
        // The e-mail addresses of the users to notify upon completion.
        if ((notifyAddresses == null) || (notifyAddresses.length == 0))
        {
          value = "";
        }
        else
        {
          value = notifyAddresses[0];
          for (int i=1; i < notifyAddresses.length; i++)
          {
            value += ", " + notifyAddresses[i];
          }
        }
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>Notify on Completion</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS +
                        "\" VALUE=\"" + value + "\" SIZE=\"40\"></TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    // The collection interval
    if (jobInterval <= 0)
    {
      if (collectionInterval > 0)
      {
        value = secondsToHumanReadableDuration(collectionInterval);
      }
      else
      {
        value = secondsToHumanReadableDuration(
             Constants.DEFAULT_COLLECTION_INTERVAL);
      }
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Statistics Collection Interval</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + value + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // Get the job-specific parameters.  Note that the provided set of
    // parameters may not be the entire set that can be used, so use the
    // parameter stubs as the base
    Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
    for (int i=0; i < stubs.length; i++)
    {
      if (parameters != null)
      {
        Parameter p = parameters.getParameter(stubs[i].getName());
        if (p != null)
        {
          stubs[i].setValueFrom(p);
        }
      }

      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD><A CLASS=\"" + Constants.STYLE_FORM_CAPTION +
                      "\" TITLE=\"" + stubs[i].getDescription() + "\">" +
                      stubs[i].getDisplayName() +
                      (stubs[i].isRequired() ? ' ' + star : "") + "</A></TD>" +
                      EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" +
                      stubs[i].getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // The job comments
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

    // The "Test Job Parameters" and "Schedule Job" buttons
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);

    if (jobClass.providesParameterTest())
    {
      htmlBody.append("    <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_TEST_PARAMS + "\">" + EOL);
      htmlBody.append("    &nbsp; &nbsp;" + EOL);
    }

    htmlBody.append("    <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                    Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                    Constants.SUBMIT_STRING_SCHEDULE_JOB + "\">" + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("</TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Parses the request parameters provided from submitting the "schedule a new
   * job" form and verifies that all the necessary information has been provided
   * and that it is valid.  If there are any problems, then it will provide
   * information on what those problems are and will display the form again to
   * allow the user to correct these problems.  If everything is OK, then the
   * job will be scheduled for execution.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void validateJobInfo(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In validateJobInfo()");

    // If the user doesn't have schedule permission, then they can't see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo,
                 "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "schedule jobs for execution.");
      return;
    }

    HttpServletRequest request     = requestInfo.request;
    StringBuilder       infoMessage = requestInfo.infoMessage;

    boolean  displayInReadOnlyMode     = false;
    boolean  jobDisabled               = false;
    boolean  jobIsValid                = true;
    boolean  makeInterDependent        = false;
    boolean  monitorClientsIfAvailable = false;
    boolean  waitForClients            = false;
    Date     startTime                 = null;
    Date     stopTime                  = null;
    int      delayBetweenStarts        = 0;
    int      duration                  = -1;
    int      numCopies                 = 1;
    int      numClients                = -1;
    int      threadsPerClient          = -1;
    int      threadStartupDelay        = 0;
    int      collectionInterval        = Constants.DEFAULT_COLLECTION_INTERVAL;
    String   folderName                = null;
    String   jobComments               = null;
    String[] dependencyIDs             = null;
    String[] notifyAddresses           = null;
    String[] requestedClients          = null;
    String[] monitorClients            = null;
    JobClass jobClass                  = null;


    // Handle the job class
    String jobClassName =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if ((jobClassName == null) || (jobClassName.length() == 0))
    {
      infoMessage.append("ERROR:  No job class specified.<BR>" + EOL);
      handleScheduleJob(requestInfo);
      return;
    }
    jobClass = slamdServer.getJobClass(jobClassName);
    if (jobClass == null)
    {
      infoMessage.append("ERROR:  Could not find job class " + jobClassName +
                         ".<BR>" + EOL);
      handleScheduleJob(requestInfo);
      return;
    }

    // See if the job is disabled.
    String disabledStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DISABLED);
    jobDisabled = ((disabledStr != null) &&
                   (disabledStr.equalsIgnoreCase("true") ||
                    disabledStr.equalsIgnoreCase("on") ||
                    disabledStr.equalsIgnoreCase("yes") ||
                    disabledStr.equalsIgnoreCase("1")));

    // See if the job should be displayed in restricted read-only mode.
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if (displayStr != null)
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }

    // Handle the number of copies of the job to create.
    String numCopiesStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_COPIES);
    if (numCopiesStr != null)
    {
      try
      {
        numCopies = Integer.parseInt(numCopiesStr);
      }
      catch (NumberFormatException nfe)
      {
        infoMessage.append("ERROR:  Number of copies must be an integer<BR>" +
                           EOL);
        jobIsValid = false;
      }
    }

    // Determine whether to make the copies interdependent.
    String interDependStr =
         request.getParameter(
              Constants.SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT);
    if (interDependStr != null)
    {
      makeInterDependent = (interDependStr.equalsIgnoreCase("true") ||
                            interDependStr.equalsIgnoreCase("yes") ||
                            interDependStr.equalsIgnoreCase("on") ||
                            interDependStr.equalsIgnoreCase("1"));
    }

    // Handle the time between job starts.
    String timeStr =
         request.getParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
    if (timeStr != null)
    {
      try
      {
        delayBetweenStarts = Integer.parseInt(timeStr);
      }
      catch (NumberFormatException nfe)
      {
        infoMessage.append("ERROR:  Time between job startups must be an " +
                           "integer<BR>" + EOL);
        jobIsValid = false;
      }
    }

    // Handle the job folder name.  No need to validate here -- if it doesn't
    // exist, then it will fail later.
    folderName = request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);

    // Handle the job description.  No validation is required for this one.
    String jobDescription =
                request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);

    // Handle the start time
    String startTimeStr =
                request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
    if ((startTimeStr == null) || (startTimeStr.length() == 0))
    {
      startTime = new Date();
    }
    else
    {
      if (startTimeStr.length() != Constants.ATTRIBUTE_DATE_FORMAT.length())
      {
        infoMessage.append("ERROR:  Start time string must be " +
                           Constants.ATTRIBUTE_DATE_FORMAT.length() +
                           " digits in length (YYYYMMDDhhmmss).<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          startTime = dateFormat.parse(startTimeStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Start time string could not be " +
                             "interpreted as a timestamp.  It must be in the " +
                             "form YYYYMMDDhhmmss.<BR>" + EOL);
          jobIsValid = false;
        }
      }
    }

    // Handle the stop time
    String stopTimeStr =
                request.getParameter(Constants.SERVLET_PARAM_JOB_STOP_TIME);
    if ((stopTimeStr != null) && (stopTimeStr.length() > 0))
    {
      if (stopTimeStr.length() != Constants.ATTRIBUTE_DATE_FORMAT.length())
      {
        infoMessage.append("ERROR:  Stop time string must be " +
                           Constants.ATTRIBUTE_DATE_FORMAT.length() +
                           " digits in length (YYYYMMDDhhmmss).<BR>" + EOL);
        jobIsValid = false;
      }
      else
      {
        try
        {
          stopTime = dateFormat.parse(stopTimeStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Stop time string could not be " +
                             "interpreted as a timestamp.  It must be in the " +
                             "form YYYYMMDDhhmmss.<BR>" + EOL);
          jobIsValid = false;
        }
      }
    }

    // Handle the duration
    String durationStr =
                request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
    if ((durationStr != null) && (durationStr.length() > 0))
    {
      try
      {
        duration = DurationParser.parse(durationStr);
      }
      catch (SLAMDException se)
      {
        infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        jobIsValid = false;
      }
    }

    // Handle the number of clients.
    String numClientsStr =
                request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
    if ((numClientsStr != null) && (numClientsStr.length() > 0))
    {
      try
      {
        numClients = Integer.parseInt(numClientsStr);
      }
      catch (NumberFormatException nfe)
      {
        infoMessage.append("ERROR:  Number of clients string could not be " +
                           "interpreted as an integer.<BR>" + EOL);
        jobIsValid = false;
      }
    }
    else
    {
      infoMessage.append("ERROR:  Number of clients not specified.<BR>" + EOL);
      jobIsValid = false;
    }

    // Handle the set of requested clients.
    String requestedClientsStr =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
    if ((requestedClientsStr != null) && (requestedClientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(requestedClientsStr);
      while (tokenizer.hasMoreTokens())
      {
        // Convert each client address to an IP address.  This makes it possible
        // to allow for multiple names to refer to a single client.  If a client
        // address can't be resolved, then report an error back to the client.
        String token = tokenizer.nextToken();
        try
        {
          InetAddress clientAddress = InetAddress.getByName(token);
          clientList.add(clientAddress.getHostAddress());
        }
        catch (UnknownHostException uhe)
        {
          infoMessage.append("ERROR:  Could not resolve \"" + token +
                             "\" as a valid address.<BR>" + EOL);
          jobIsValid = false;
        }
      }

      if (! clientList.isEmpty())
      {
        requestedClients = new String[clientList.size()];
        clientList.toArray(requestedClients);

        if (requestedClients.length > numClients)
        {
          infoMessage.append("ERROR:  Requested set of clients contains more " +
                             "than the specified number of clients.<BR>" + EOL);
          jobIsValid = false;
        }
      }
    }

    // Handle the set of resource monitor clients.
    String monitorClientsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
    if ((monitorClientsStr != null) && (monitorClientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(monitorClientsStr);
      while (tokenizer.hasMoreTokens())
      {
        // Convert each client address to an IP address.  This makes it possible
        // to allow for multiple names to refer to a single client.  If a client
        // address can't be resolved, then report an error back to the client.
        String token = tokenizer.nextToken();
        try
        {
          InetAddress clientAddress = InetAddress.getByName(token);

          // See if the address is already present in the list.  if so, then
          // don't add it a second time and display a warning to the user.
          String ipAddress = clientAddress.getHostAddress();
          if (clientList.contains(ipAddress))
          {
            infoMessage.append("WARNING:  Ignoring duplicate reference to " +
                               "resource monitor client " + token + "<BR>." +
                               EOL);
            continue;
          }

          clientList.add(ipAddress);
        }
        catch (UnknownHostException uhe)
        {
          infoMessage.append("ERROR:  Could not resolve \"" + token +
                             "\" as a valid address<BR>." + EOL);
          jobIsValid = false;
        }
      }

      if (! clientList.isEmpty())
      {
        monitorClients = new String[clientList.size()];
        clientList.toArray(monitorClients);
      }
    }

    // Handle the number of threads per client
    String threadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
    if ((threadsStr != null) && (threadsStr.length() > 0))
    {
      try
      {
        threadsPerClient = Integer.parseInt(threadsStr);
      }
      catch (NumberFormatException nfe)
      {
        infoMessage.append("ERROR:  Number of threads per client could not " +
                           "be interpreted as an integer.<BR>" + EOL);
        jobIsValid = false;
      }
    }
    else
    {
      infoMessage.append("ERROR:  Number of threads per client not " +
                         "specified.<BR>" + EOL);
      jobIsValid = false;
    }

    // Handle the thread startup delay
    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if ((delayStr != null) && (delayStr.length() > 0))
    {
      try
      {
        threadStartupDelay = Integer.parseInt(delayStr);
      }
      catch (NumberFormatException nfe)
      {
        infoMessage.append("ERROR:  Thread startup delay must be an " +
                           "integer.<BR>" + EOL);
        jobIsValid = false;
      }
    }

    // Handle the determination to wait for available clients.
    String waitStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS);
    if ((waitStr != null) && (waitStr.length() > 0))
    {
      waitForClients = (waitStr.equalsIgnoreCase("true") ||
                        waitStr.equalsIgnoreCase("on") ||
                        waitStr.equalsIgnoreCase("yes") ||
                        waitStr.equals("1"));
    }

    // Handle the determination to automatically monitor available clients.
    String monitorStr = request.getParameter(
              Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
    if ((monitorStr != null) && (monitorStr.length() > 0))
    {
      monitorClientsIfAvailable = (monitorStr.equalsIgnoreCase("true") ||
                                   monitorStr.equalsIgnoreCase("on") ||
                                   monitorStr.equalsIgnoreCase("yes") ||
                                   monitorStr.equals("1"));
    }

    // Handle the list of dependency IDs.
    dependencyIDs = request.getParameterValues(
                         Constants.SERVLET_PARAM_JOB_DEPENDENCY);

    // Handle the notify addresses.
    String notifyAddressStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS);
    if ((notifyAddressStr == null) || (notifyAddressStr.length() == 0))
    {
      notifyAddresses = new String[0];
    }
    else
    {
      ArrayList<String> addressList = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(notifyAddressStr, ", ");
      while (st.hasMoreTokens())
      {
        addressList.add(st.nextToken());
      }
      notifyAddresses = new String[addressList.size()];
      addressList.toArray(notifyAddresses);
    }

    // Handle the collection interval.
    String intervalStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
    if ((intervalStr != null) && (intervalStr.length() > 0))
    {
      try
      {
        collectionInterval = DurationParser.parse(intervalStr);
        if (collectionInterval <= 0)
        {
          infoMessage.append("ERROR:  Statistics collection interval must be " +
                             "greater than zero.<BR>" + EOL);
          jobIsValid = false;
        }
      }
      catch (SLAMDException se)
      {
        infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        jobIsValid = false;
      }
    }

    // Figure out how long the job is scheduled to run.  If it is less than
    // or equal to the collection interval, then complain about it.
    if ((duration > 0) && (duration <= collectionInterval))
    {
      infoMessage.append("ERROR:  Statistics collection interval must be " +
                         "less than the scheduled job duration.<BR>" + EOL);
      jobIsValid = false;
    }
    else if (stopTime != null)
    {
      long startTimeMillis = startTime.getTime();
      long stopTimeMillis  = stopTime.getTime();
      long durationMillis  = (stopTimeMillis - startTimeMillis);
      long durationSecs    = durationMillis / 1000;

      if (durationSecs <= collectionInterval)
      {
        infoMessage.append("ERROR:  The difference between the scheduled " +
                           "start and stop times may not be less than the " +
                           "statistics collection interval.<BR>" + EOL);
        jobIsValid = false;
      }
    }

    // Handle the job comments
    jobComments = request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);

    // Handle the job-specific parameters
    Parameter[] params = jobClass.getParameterStubs().clone().getParameters();
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
    ParameterList parameters = new ParameterList(params);


    // Execute the validation routine of the job class itself.
    try
    {
      jobClass.validateJobInfo(numClients, threadsPerClient, threadStartupDelay,
                               startTime, stopTime, duration,
                               collectionInterval, parameters);
    }
    catch (InvalidValueException ive)
    {
      infoMessage.append("ERROR:  " + ive.getMessage() + "<BR>" + EOL);
      jobIsValid = false;
    }


    // At this point, all the parameters have been read in and verified.  If the
    // job is valid, then see if we need to test the parameters.  If so, then do
    // that.  Otherwise, schedule the job for execution.
    if (jobIsValid)
    {
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if (submitStr.equals(Constants.SUBMIT_STRING_TEST_PARAMS))
      {
        ArrayList<String> messageList = new ArrayList<String>();
        boolean testSuccessful = false;

        try
        {
          testSuccessful = jobClass.testJobParameters(parameters, messageList);
        }
        catch (Exception e)
        {
          testSuccessful = false;
          messageList.add("ERROR:  Exception caught while testing job " +
                          "parameters:  " + JobClass.stackTraceToString(e));
        }

        infoMessage.append("Results of testing job parameters:<BR><BR>" + EOL);
        for (int i=0; i < messageList.size(); i++)
        {
          Object o = messageList.get(i);
          if (o != null)
          {
            infoMessage.append(o.toString() + "<BR>" + EOL);
          }
        }
      }
      else
      {
        try
        {
          long thisStartTime = System.currentTimeMillis();
          if (startTime != null)
          {
            thisStartTime = startTime.getTime();
          }

          long thisDuration  = duration;
          if (stopTime != null)
          {
            long stopTimeDuration = stopTime.getTime() - thisStartTime;
            thisDuration = Math.max(thisDuration, stopTimeDuration);
          }

          Job job = null;
          String jobID = null;
          for (int i=0; i < numCopies; i++)
          {
            Date jobStartTime = new Date(thisStartTime);
            Date jobStopTime  = null;
            if (stopTime != null)
            {
              jobStopTime = new Date(thisStartTime + thisDuration);
            }

            job = new Job(slamdServer, jobClass.getClass().getName(),
                          numClients, threadsPerClient, threadStartupDelay,
                          jobStartTime, jobStopTime, duration,
                          collectionInterval, parameters,
                          displayInReadOnlyMode);
            job.setFolderName(folderName);
            job.setJobDescription(jobDescription);
            job.setWaitForClients(waitForClients);
            job.setRequestedClients(requestedClients);
            job.setResourceMonitorClients(monitorClients);
            job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
            job.setDependencies(dependencyIDs);
            job.setNotifyAddresses(notifyAddresses);
            job.setJobComments(jobComments);
            if (jobDisabled)
            {
              job.setJobState(Constants.JOB_STATE_DISABLED);
            }


            try
            {
              jobID = scheduler.scheduleJob(job, folderName);
              logMessage(requestInfo, "Successfully scheduled job " + jobID);
              infoMessage.append("Successfully scheduled job " + jobID +
                                 " for execution.<BR>" + EOL);


              // Update the HTTP header of the response to include the job ID.
              HttpServletResponse response = requestInfo.response;
              response.addHeader(Constants.SERVLET_PARAM_JOB_ID, jobID);


              // Perform a simple calculation to see how much data this job will
              // send back to client.  If (numIntervals*numClients*numThreads)
              // is greater than 1 million, then display a warning message.
              int numIntervals = 0;
              if (duration > 0)
              {
                numIntervals = duration / collectionInterval;
              }
              else if (jobStopTime != null)
              {
                long stopTimeVal = jobStopTime.getTime();
                long startTimeVal;
                if (jobStartTime == null)
                {
                  startTimeVal = System.currentTimeMillis();
                }
                else
                {
                  startTimeVal = jobStartTime.getTime();
                }

                numIntervals = (int) ((stopTimeVal - startTimeVal) /
                                      collectionInterval);
              }

              if ((numIntervals * numClients * threadsPerClient) > 1000000)
              {
                infoMessage.append("WARNING:  This job has the potential to " +
                                   "return a large amount of data to the " +
                                   "SLAMD server.  If you do not think that " +
                                   "the SLAMD server has adequate memory to " +
                                   "handle the result set returned, then you " +
                                   "may wish to cancel or edit this job and " +
                                   "re-schedule it with a larger collection " +
                                   "interval.<BR>" + EOL);
              }

              if (makeInterDependent)
              {
                dependencyIDs = new String[] { jobID };
              }
            }
            catch (SLAMDServerException sse)
            {
              sse.printStackTrace();
              infoMessage.append("ERROR:  Unable to schedule job for " +
                                 "execution -- " + sse.getMessage() + "<BR>" +
                                   EOL);
            }

            thisStartTime += (delayBetweenStarts * 1000);
          }

          if (numCopies == 1)
          {
            generateViewJobBody(requestInfo, job);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_PENDING, null,
                          null);
          }

          return;
        }
        catch (Exception e)
        {
          e.printStackTrace();
          infoMessage.append("ERROR:  Unable to create the job -- " +
                             e.getMessage() + "<BR>" + EOL);
        }
      }
    }

    generateScheduleJobForm(requestInfo, jobClass, numCopies,
                            makeInterDependent, delayBetweenStarts, folderName,
                            jobDescription, startTime, stopTime, duration,
                            numClients, threadsPerClient, threadStartupDelay,
                            requestedClients, monitorClients,
                            monitorClientsIfAvailable, waitForClients,
                            dependencyIDs, notifyAddresses, collectionInterval,
                            jobComments, jobDisabled, displayInReadOnlyMode,
                            parameters);
  }



  /**
   * Generates an HTML page that may be used to display information that can
   * help a user understand the kinds of information that should be provided
   * when scheduling a new job for execution.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void generateScheduleHelpPage(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In generateScheduleHelpPage()");

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


    String jobClassName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if ((jobClassName == null) || (jobClassName.length() == 0))
    {
      htmlBody.append("ERROR:  No job class name specified." + EOL);
      return;
    }

    JobClass jobClass = slamdServer.getJobClass(jobClassName);
    if (jobClass == null)
    {
      htmlBody.append("ERROR:  No information is known about job class " +
                      jobClass + '.' + EOL);
      return;
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Help for Scheduling a \"" + jobClass.getJobName() +
                    "\" Job</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The following parameters may be specified when " +
                    "scheduling a " + jobClass.getJobName() + " job class:" +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);

    // The disable checkbox.
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Is Disabled</TD>" + EOL);
    htmlBody.append("   <TD>" + EOL);
    htmlBody.append("     Indicates whether the job should be designated as " +
                    "disabled when it is initially scheduled." + EOL);
    htmlBody.append("     This will prevent the job from starting until it " +
                    "is manually re-enabled." + EOL);
    htmlBody.append("   </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of copies
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Number of Copies</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The number of copies to make for this job." + EOL);
    htmlBody.append("      When scheduling the job for execution, this " +
                    "parameter specifies how many copies to schedule, which " +
                    "allows you to create multiple jobs using the same " +
                    "base configuration and then edit each one to customize " +
                    "its settings." + EOL);
    htmlBody.append("      If no value is specified, then only one copy will " +
                    "be created." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The delay between startups
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Time between Copy Startups</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The length of time in seconds that should be left " +
                    "between the start times for each copy of this job." + EOL);
    htmlBody.append("      Note that this delay is between start times, not " +
                    "between the time that one job completes and the next " +
                    "begins." + EOL);
    htmlBody.append("      If no value is specified, then all jobs will be " +
                    "eligible for immediate execution." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job folder
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Place in Folder</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The name of the job folder in which the job " +
                    "be placed when it is created." + EOL);
    htmlBody.append("      Note that this will only appear if one or more " +
                    "job folders have been created." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

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

    // The start time
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Start Time</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The time at which the job should start running." +
                    EOL);
    htmlBody.append("      The value should be in the form " +
                    "\"YYYYMMDDhhmmss\"." + EOL);
    htmlBody.append("      If you specify time that has already passed, then " +
                    "the job will be immediately eligible for execution." +
                    EOL);
    htmlBody.append("      If you leave the value unspecified, then the " +
                    "current time will be used." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The stop time
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Stop Time</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The time at which the job should stop running if " +
                    "it has not already stopped running." + EOL);
    htmlBody.append("      The value should be in the form " +
                    "\"YYYYMMDDhhmmss\"." + EOL);
    htmlBody.append("      If you leave the value unspecified, then the " +
                    "job will continue running for the maximum allowed " +
                    "duration, until it has completed, or until it is " +
                    "stopped by an administrator." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The duration
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Duration</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The maximum length of time in seconds that the " +
                    "job should be allowed to run before it is stopped." + EOL);
    htmlBody.append("      If you leave the value unspecified, then the " +
                    "job will continue running until the stop time has been " +
                    "reached, until it has completed, or until it is stopped " +
                    "by an administrator." + EOL);
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

    // Whether to wait until clients are available
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Wait for Available Clients</TD>" + EOL);
    htmlBody.append("    <TD>");
    htmlBody.append("      Indicates whether the job should wait for the " +
                    "appropriate number and/or specified clients to become " +
                    "available before it will be eligible to start running." +
                    EOL);
    htmlBody.append("      If the time arrives for the job to start, this " +
                    "parameter indicates whether the job will be cancelled, " +
                    "or if the start will be delayed until the appropriate " +
                    "clients are available." + EOL);
    htmlBody.append("    </TD>");
    htmlBody.append("  </TR>" + EOL);

    // The number of threads per client
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Threads per Client</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The number of threads that should be created on " +
                    "each client to run the job." + EOL);
    htmlBody.append("      This is a required parameter." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The thread startup delay
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Thread Startup Delay</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      The delay in milliseconds that will be used when " +
                    "starting the individual threads on the client." + EOL);
    htmlBody.append("      If no value is specified, then there will not be " +
                    "any delay between client thread startup." + EOL);
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
                    "be sent an e-mail message when the job has completed " +
                    "execution." + EOL);
    htmlBody.append("      This message will contain a summary of the " +
                    "job execution results and may optionally include a " +
                    "link to view more information about the job through the " +
                    "SLAMD administrative interface." + EOL);
    htmlBody.append("      If multiple addresses should be notified, then " +
                    "they should be separated with spaces and/or commas." +
                    EOL);
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

    // The job-specific parameters
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
                         "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
      htmlBody.append("      " + description + EOL);
      if (stubs[i].isRequired())
      {
        htmlBody.append("      <BR>" + EOL);
        htmlBody.append("      This is a required parameter." + EOL);
      }
      htmlBody.append("    </TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // The job comments
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Comments</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);
    htmlBody.append("      Free-form comments about this job that may be " +
                    "used to provide more descriptive information than can " +
                    "be seen through the other parameters." + EOL);
    htmlBody.append("    </TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work necessary to edit a job that has been scheduled but not
   * yet started.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleEditJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEditJob()");

    // If the user doesn't have schedule permission, then they can't see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit job information.");
      return;
    }


    // Get the important state variables.
    HttpServletRequest request     = requestInfo.request;
    StringBuilder       htmlBody    = requestInfo.htmlBody;
    StringBuilder       infoMessage = requestInfo.infoMessage;


    // Make sure that a job ID has been specified.  If not, then just show the
    // default HTML.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append(getDefaultHTML(requestInfo));
      return;
    }


    // Make sure that we can retrieve the specified job from the pending queue.
    Job job = scheduler.getPendingJob(jobID);
    if (job == null)
    {
      infoMessage.append("Unable to retrieve job " + jobID +
                         " for editing." + EOL);
      infoMessage.append("It may no longer be in the pending jobs queue.<BR>" +
                         EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                    null, null);
      return;
    }


    // See if the user has performed the edit.  If so, make sure everything is
    // valid and perform the update.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      boolean updateValid = true;

      // Get the disabled flag.
      String disabledStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DISABLED);
      boolean jobDisabled = ((disabledStr != null) &&
                             (disabledStr.equalsIgnoreCase("true") ||
                              disabledStr.equalsIgnoreCase("on") ||
                              disabledStr.equalsIgnoreCase("yes") ||
                              disabledStr.equalsIgnoreCase("1")));

      boolean displayInReadOnlyMode = false;
      String displayStr =
           request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
      if ((displayStr == null) || (displayStr.length() == 0))
      {
        displayInReadOnlyMode = false;
      }
      else
      {
        displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                                 displayStr.equalsIgnoreCase("yes") ||
                                 displayStr.equalsIgnoreCase("on") ||
                                 displayStr.equalsIgnoreCase("1"));
      }

      // Get the description -- no verification required.
      String description =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);

      // Get the start time.  If it's specified, then it must be a valid date.
      Date startTime = null;
      String startTimeStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
      if ((startTimeStr != null) && (startTimeStr.length() > 0))
      {
        if (startTimeStr.length() == 14)
        {
          try
          {
            startTime = dateFormat.parse(startTimeStr);
          }
          catch (Exception e)
          {
            infoMessage.append("ERROR:  Start time is not a valid date.<BR>" +
                               EOL);
            updateValid = false;
          }
        }
        else
        {
          infoMessage.append("ERROR:  Start time is not a valid date.<BR>" +
                             EOL);
          updateValid = false;
        }
      }

      // Get the stop time.  If it's specified, then it must be a valid date.
      Date stopTime = null;
      String stopTimeStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_STOP_TIME);
      if ((stopTimeStr != null) && (stopTimeStr.length() > 0))
      {
        if (stopTimeStr.length() == 14)
        {
          try
          {
            stopTime = dateFormat.parse(stopTimeStr);
          }
          catch (Exception e)
          {
            infoMessage.append("ERROR:  Stop time is not a valid date.<BR>" +
                               EOL);
            updateValid = false;
          }
        }
        else
        {
          infoMessage.append("ERROR:  Stop time is not a valid date.<BR>" +
                             EOL);
          updateValid = false;
        }
      }

      // Get the duration.  If it's specified, then it must be an integer;
      int duration = -1;
      String durationStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
      if ((durationStr != null) && (durationStr.length() > 0))
      {
        try
        {
          duration = DurationParser.parse(durationStr);
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
          updateValid = false;
        }
      }

      // Get the number of clients.  It must be an integer.
      int numClients = -1;
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((numClientsStr == null) || (numClientsStr.length() == 0))
      {
        infoMessage.append("ERROR:  The number of clients to use must be " +
                           "specified.<BR>" + EOL);
        updateValid = false;
      }
      else
      {
        try
        {
          numClients = Integer.parseInt(numClientsStr);
          if (numClients <= 0)
          {
            infoMessage.append("ERROR:  The number of clients must be " +
                               "greater than zero.<BR>" + EOL);
            updateValid = false;
          }
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  The number of clients must be an " +
                             "integer.<BR>" + EOL);
          updateValid = false;
        }
      }

      // Get the specific clients to use.  If specified, they must all be
      // resolvable to IP addresses.
      String[] clients = null;
      String clientStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
      if ((clientStr != null) && (clientStr.length() > 0))
      {
        ArrayList<String> clientAddressList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(clientStr, " \t\r\n");
        while (tokenizer.hasMoreTokens())
        {
          String address = tokenizer.nextToken();
          try
          {
            InetAddress clientAddress = InetAddress.getByName(address);
            clientAddressList.add(clientAddress.getHostAddress());
          }
          catch (Exception e)
          {
            infoMessage.append("ERROR:  The client address " + address +
                               " could not be resolved.<BR>" + EOL);
            updateValid = false;
          }
        }
        clients = new String[clientAddressList.size()];
        clientAddressList.toArray(clients);
      }

      // Get the resource monitor clients to use.  If specified, they must all
      // be resolvable to IP addresses.
      String[] monitorClients = null;
      clientStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
      if ((clientStr != null) && (clientStr.length() > 0))
      {
        ArrayList<String> clientAddressList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(clientStr, " \t\r\n");
        while (tokenizer.hasMoreTokens())
        {
          String address = tokenizer.nextToken();
          try
          {
            InetAddress clientAddress = InetAddress.getByName(address);

            // See if the address is already present in the list.  if so, then
            // don't add it a second time and display a warning to the user.
            String ipAddress = clientAddress.getHostAddress();
            if (clientAddressList.contains(ipAddress))
            {
              infoMessage.append("WARNING:  Ignoring duplicate reference to " +
                                 "resource monitor client " + address +
                                 "<BR>." + EOL);
              continue;
            }

            clientAddressList.add(ipAddress);
          }
          catch (Exception e)
          {
            infoMessage.append("ERROR:  The resource monitor client address " +
                               address + " could not be resolved.<BR>" + EOL);
            updateValid = false;
          }
        }
        monitorClients = new String[clientAddressList.size()];
        clientAddressList.toArray(monitorClients);
      }

      // Get the indicator that specifies whether to automatically monitor
      // client systems if they are available.
      String monitorStr = request.getParameter(
           Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
      boolean monitorClientsIfAvailable =
           ((monitorStr != null) && (monitorStr.equalsIgnoreCase("true") ||
                                     monitorStr.equalsIgnoreCase("yes") ||
                                     monitorStr.equalsIgnoreCase("on") ||
                                     monitorStr.equalsIgnoreCase("1")));

      // Get the indicator that specifies whether to wait for clients to be
      // available.
      String waitStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS);
      boolean waitForClients = ((waitStr != null) &&
                                (waitStr.equalsIgnoreCase("true") ||
                                 waitStr.equalsIgnoreCase("yes") ||
                                 waitStr.equalsIgnoreCase("on") ||
                                 waitStr.equalsIgnoreCase("1")));

      // Get the number of threads per client.  It must be a positive integer.
      int threadsPerClient = -1;
      String threadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
      if ((threadsStr == null) || (threadsStr.length() == 0))
      {
        infoMessage.append("ERROR:  The number of threads per client must be " +
                           "specified.<BR>" + EOL);
        updateValid = false;
      }
      else
      {
        try
        {
          threadsPerClient = Integer.parseInt(threadsStr);
          if (threadsPerClient <= 0)
          {
            infoMessage.append("ERROR:  The number of threads per client " +
                               "must be greater than zero.<BR>" + EOL);
            updateValid = false;
          }
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  The number of threads per client must " +
                             "be an integer.<BR>" + EOL);
          updateValid = false;
        }
      }

      // Get the thread startup delay.  If it is specified, then it must be
      // an integer.
      int threadStartupDelay = 0;
      String threadDelayStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
      if ((threadDelayStr != null) && (threadDelayStr.length() > 0))
      {
        try
        {
          threadStartupDelay = Integer.parseInt(threadDelayStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  The thread startup delay must be an " +
                             "integer.<BR>" + EOL);
          updateValid = false;
        }
      }

      // Get the job dependencies.
      String[] dependencyIDs =
           request.getParameterValues(Constants.SERVLET_PARAM_JOB_DEPENDENCY);

      // Get the statistics collection interval.  If it is specified, then it
      // must be an integer.
      int collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr != null) && (intervalStr.length() > 0))
      {
        try
        {
          collectionInterval = DurationParser.parse(intervalStr);
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
          updateValid = false;
        }
      }


      // Get the notify addresses.
      String[] notifyAddresses = new String[0];
      String addressStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS);
      if ((addressStr != null) && (addressStr.length() > 0))
      {
        ArrayList<String> addressList = new ArrayList<String>();
        StringTokenizer tokenizer   = new StringTokenizer(addressStr, ", ");
        while (tokenizer.hasMoreTokens())
        {
          addressList.add(tokenizer.nextToken());
        }

        notifyAddresses = new String[addressList.size()];
        addressList.toArray(notifyAddresses);
      }


      // Get the job comments.
      String comments =
           request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);


      // Get all of the job-specific parameters.
      Parameter[] params = job.getParameterStubs().clone().getParameters();
      for (int i=0; i < params.length; i++)
      {
        String[] values =
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  params[i].getName());
        try
        {
          params[i].htmlInputFormToValue(values);
        }
        catch (InvalidValueException ive)
        {
          infoMessage.append("ERROR:  Invalid value specified for " +
                             params[i].getDisplayName() + ":  " +
                             ive.getMessage() + "<BR>" + EOL);
          updateValid = false;
        }
      }


      // If all the new values are acceptable, then write the changes into the
      // config directory.
      if (updateValid)
      {
        try
        {
          job.setJobState(jobDisabled ?
                          Constants.JOB_STATE_DISABLED :
                          Constants.JOB_STATE_NOT_YET_STARTED);
          job.setJobDescription(description);
          job.setStartTime(startTime);
          job.setStopTime(stopTime);
          job.setDuration(duration);
          job.setNumberOfClients(numClients);
          job.setRequestedClients(clients);
          job.setResourceMonitorClients(monitorClients);
          job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
          job.setWaitForClients(waitForClients);
          job.setThreadsPerClient(threadsPerClient);
          job.setThreadStartupDelay(threadStartupDelay);
          job.setCollectionInterval(collectionInterval);
          job.setDependencies(dependencyIDs);
          job.setNotifyAddresses(notifyAddresses);
          job.setJobComments(comments);
          job.setDisplayInReadOnlyMode(displayInReadOnlyMode);
          job.setParameterList(new ParameterList(params));

          configDB.writeJob(job);
          infoMessage.append("Successfully updated job " + job.getJobID() +
                             ".<BR>" + EOL);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Unable to update job information:  " +
                             e + "<BR>" + EOL);
        }

        generateViewJobBody(requestInfo, job);
        return;
      }
    }

    int jobNumClients = job.getJobClass().overrideNumClients();
    int jobNumThreads = job.getJobClass().overrideThreadsPerClient();
    int jobInterval   = job.getJobClass().overrideCollectionInterval();

    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Edit Job " + jobID + "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    String link = generateNewWindowLink(requestInfo,
                       Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_SCHEDULE_HELP,
                       Constants.SERVLET_PARAM_JOB_CLASS,
                       job.getJobClass().getClass().getName(),
                       "Click here for help regarding these parameters");
    htmlBody.append(link + '.' + EOL);

    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" +
                    requestInfo.servletBaseURI + "\">" + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_EDIT) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          jobID) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    if (jobNumClients > 0)
    {
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                                  String.valueOf(jobNumClients)) + EOL);
    }

    if (jobNumThreads > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT,
                           String.valueOf(jobNumThreads)) + EOL);
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

    htmlBody.append("<TABLE BORDER=\"0\">" + EOL);

    // Whether the job is disabled
    boolean jobDisabled = (job.getJobState() == Constants.JOB_STATE_DISABLED);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Disabled</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DISABLED + '"' +
                    (jobDisabled ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Whether the job should be displayed in read-only mode
    boolean displayInReadOnlyMode = job.displayInReadOnlyMode();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Display in Read-Only Mode</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY + '"' +
                    (displayInReadOnlyMode ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job description
    String value = job.getJobDescription();
    if (value == null)
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Description</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    value + "\" SIZE=\"80\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The start time
    Date startTime = job.getStartTime();
    if (startTime != null)
    {
      value = dateFormat.format(startTime);
    }
    else
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Start Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_START_TIME + "\" VALUE=\"" +
                    value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The stop time
    Date stopTime = job.getStopTime();
    if (stopTime != null)
    {
      value = dateFormat.format(stopTime);
    }
    else
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Stop Time <FONT SIZE=\"-1\">(YYYYMMDDhhmmss)" +
                    "</FONT></TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_STOP_TIME + "\" VALUE=\"" +
                    value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The duration
    int duration = job.getDuration();
    if (duration > 0)
    {
      value = secondsToHumanReadableDuration(duration);
    }
    else
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of clients
    if (jobNumClients <= 0)
    {
      int numClients = job.getNumberOfClients();
      if (numClients > 0)
      {
        value = String.valueOf(numClients);
      }
      else
      {
        value = "";
      }
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      value + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // Specific clients to use.
    String[] clients = job.getRequestedClients();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Use Specific Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_CLIENTS + "\" ROWS=\"5\"" +
                    " COLS=\"40\">");
    String separator = "";
    for (int i=0; ((clients != null) && (i < clients.length)); i++)
    {
      htmlBody.append(separator);
      htmlBody.append(clients[i]);
      separator = EOL;
    }
    htmlBody.append("</TEXTAREA></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);


    // Resource monitor clients to use.
    String[] monitorClients = job.getResourceMonitorClients();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Resource Monitor Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS +
                    "\" ROWS=\"5\"" + " COLS=\"40\">");
    separator = "";
    for (int i=0; ((monitorClients != null) && (i < monitorClients.length));
         i++)
    {
      htmlBody.append(separator);
      htmlBody.append(monitorClients[i]);
      separator = EOL;
    }
    htmlBody.append("</TEXTAREA></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Whether to automatically monitor client systems.
    boolean monitorClientsIfAvailable = job.monitorClientsIfAvailable();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE +
                    '"' + (monitorClientsIfAvailable ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // Whether to wait for clients to be available.
    boolean waitForClients = job.waitForClients();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Wait for Available Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS +
                    '"' + (waitForClients ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The number of threads per client
    if (jobNumThreads <= 0)
    {
      int threadsPerClient = job.getThreadsPerClient();
      if (threadsPerClient > 0)
      {
        value = String.valueOf(threadsPerClient);
      }
      else
      {
        value = "";
      }
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Threads per Client " + star + "</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT +
                      "\" VALUE=\"" + value + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // The thread startup delay
    int threadStartupDelay = job.getThreadStartupDelay();
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + threadStartupDelay +
                    "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The job dependencies
    String[] dependencyIDs = job.getDependencies();
    Job[] pendingJobs = scheduler.getPendingJobs();
    Job[] runningJobs = scheduler.getRunningJobs();

    OptimizingJob[] optimizingJobs = new OptimizingJob[0];
    try
    {
      optimizingJobs = scheduler.getUncompletedOptimizingJobs();
    }
    catch (SLAMDServerException sse)
    {
      requestInfo.infoMessage.append("ERROR:  Unable to retrieve the list " +
                                     "of uncompleted optimizing jobs -- " +
                                     sse + "<BR>" + EOL);
    }

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + EOL);

    for (int i=0; ((dependencyIDs != null) &&
                   (i < dependencyIDs.length)); i++)
    {
      if ((dependencyIDs[i] == null) || (dependencyIDs[i].length() == 0))
      {
        continue;
      }

      htmlBody.append("      <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);

      Job dependentJob = null;
      try
      {
        dependentJob = configDB.getJob(dependencyIDs[i]);
      } catch (Exception e) {}

      if (dependentJob == null)
      {
        htmlBody.append("        <OPTION VALUE=\"" + dependencyIDs[i] +
                        "\">" + dependencyIDs[i] + " -- Unknown Job" + EOL);
      }
      else
      {
        String description = dependentJob.getJobDescription();
        if (description == null)
        {
          description = "";
        }
        else if (description.length() > 0)
        {
          description = " -- " + description;
        }
        htmlBody.append("        <OPTION VALUE=\"" + dependencyIDs[i] +
                        "\">" + dependencyIDs[i] + " -- " +
                        dependentJob.getJobName() + description + EOL);
      }

      htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);

      for (int j=0; j < pendingJobs.length; j++)
      {
        if (pendingJobs[j].getJobID().equals(jobID))
        {
          // Don't allow the user to specify this job as its own dependency.
          // That would prevent the job from starting at all.
          continue;
        }

        String description = pendingJobs[j].getJobDescription();
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
                        pendingJobs[j].getJobID() + " -- " +
                        pendingJobs[j].getJobName() + description + EOL);
      }

      for (int j=0; j < runningJobs.length; j++)
      {
        String description = runningJobs[j].getJobDescription();
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
                        runningJobs[j].getJobID() + " -- " +
                        runningJobs[j].getJobName() + description + EOL);
      }

      for (int j=0; j < optimizingJobs.length; j++)
      {
        String description = optimizingJobs[j].getDescription();
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
      htmlBody.append("      <BR>" + EOL);
    }

    htmlBody.append("      <SELECT NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);
    htmlBody.append("        <OPTION VALUE=\"\">No Dependency" + EOL);
    for (int j=0; j < pendingJobs.length; j++)
    {
      if (pendingJobs[j].getJobID().equals(jobID))
      {
        // Don't allow the user to specify this job as its own dependency.
        // That would prevent the job from starting at all.
        continue;
      }

      String description = pendingJobs[j].getJobDescription();
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
                      pendingJobs[j].getJobID() + " -- " +
                      pendingJobs[j].getJobName() + description + EOL);
    }

    for (int j=0; j < runningJobs.length; j++)
    {
      String description = runningJobs[j].getJobDescription();
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
                      runningJobs[j].getJobID() + " -- " +
                      runningJobs[j].getJobName() + description + EOL);
    }

    for (int j=0; j < optimizingJobs.length; j++)
    {
      String description = optimizingJobs[j].getDescription();
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

    // The notify addresses
    String[] notifyAddresses = job.getNotifyAddresses();
    if ((notifyAddresses != null) && (notifyAddresses.length > 0))
    {
      value = notifyAddresses[0];

      for (int i=1; i < notifyAddresses.length; i++)
      {
        value += ", " + notifyAddresses[i];
      }
    }
    else
    {
      value = "";
    }
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Notify on Completion</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS +
                    "\" VALUE=\"" + value + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    // The collection interval
    if (jobInterval <= 0)
    {
      int collectionInterval = job.getCollectionInterval();
      if (collectionInterval > 0)
      {
        value = secondsToHumanReadableDuration(collectionInterval);
      }
      else
      {
        value = "";
      }
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>Statistics Collection Interval</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + value + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // Get the job-specific parameters.  Note that the provided set of
    // parameters may not be the entire set that can be used, so use the
    // parameter stubs as the base
    Parameter[] stubs = job.getParameterStubs().clone().getParameters();
    ParameterList parameters = job.getParameterList();
    for (int i=0; i < stubs.length; i++)
    {
      if (parameters != null)
      {
        Parameter p = parameters.getParameter(stubs[i].getName());
        if (p != null)
        {
          stubs[i].setValueFrom(p);
        }
      }

      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>" + stubs[i].getDisplayName() +
                      (stubs[i].isRequired() ? ' ' + star : "") + "</TD>" +
                      EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" +
                      stubs[i].getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    // The job comments
    String comments = job.getJobComments();
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

    // The "Schedule Job" button
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Update " +
                    "Job\"></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("</TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles the work necessary to edit the set of comments and/or description
   * for a job.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleEditJobComments(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEditJobComments()");

    // If the user doesn't have schedule permission, then they can't see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit job information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Make sure that a job ID has been specified.  If not, then just show the
    // default HTML.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      htmlBody.append(getDefaultHTML(requestInfo));
      return;
    }


    // Make sure that we can retrieve the specified job.
    Job job = null;
    try
    {
      job = configDB.getJob(jobID);
    }
    catch (Exception e)
    {
      infoMessage.append("Error retrieving job " + jobID + ":  " +
                         e.getMessage() + "<BR>" + EOL);
    }
    if (job == null)
    {
      infoMessage.append("Unable to retrieve job " + jobID +
                         " for editing.<BR>" + EOL);
      return;
    }

    boolean displayInReadOnlyMode = false;
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if ((displayStr == null) || (displayStr.length() == 0))
    {
      displayInReadOnlyMode = false;
    }
    else
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }

    // See if we need to actually process the update or if we need to show the
    // form to the user.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      String comments =
           request.getParameter(Constants.SERVLET_PARAM_JOB_COMMENTS);
      String description =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);

      try
      {
        job.setJobDescription(description);
        job.setJobComments(comments);
        job.setDisplayInReadOnlyMode(displayInReadOnlyMode);
        configDB.writeJob(job);
        infoMessage.append("Successfully updated job comments.<BR>" + EOL);
      }
      catch (DatabaseException de)
      {
        infoMessage.append("Unable to update job comments:  " + de +
                           "<BR>" + EOL);
      }

      generateViewJobBody(requestInfo, job);
    }
    else
    {
      String comments    = job.getJobComments();
      String description = job.getJobDescription();

      if (comments == null)
      {
        comments = "";
      }
      if (description == null)
      {
        description = "";
      }

      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Edit Comments for Job " + jobID + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Edit the set of comments for the job and click the " +
                      "update button when finished." + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                  Constants.SERVLET_SECTION_JOB_EDIT_COMMENTS) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                            jobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Job Description</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                      description + "\" SIZE=\"80\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Job Comments</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COMMENTS + "\" ROWS=\"10\" " +
                      "COLS=\"80\">" + comments + "</TEXTAREA></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Display in Restricted Read-Only Mode</TD>" +
                      EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY + '"' +
                      (displayInReadOnlyMode ? " CHECKED" : "") + "></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);


      htmlBody.append("<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" " +
                      "VALUE=\"Update\">" + EOL);
      htmlBody.append("</FORM>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                     EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                        Constants.SERVLET_SECTION_JOB_VIEW_GENERIC) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobID) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing necessary to import persistent statistical data for
   * a job into the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleImportPersistentStats(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleImportPersistentStats()");

    // If the user doesn't have schedule permission, then they can't see this.
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "import persistent job statistics.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // There should have been a job ID specified.  If not, then report an error.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      String message = "Unable to determine the job ID of the job for which " +
                       "to import persistent statistics.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      return;
    }


    // Make sure that we can retrieve the specified job.
    Job   job      = null;
    String message = null;
    try
    {
      job = configDB.getJob(jobID);
    }
    catch (Exception e)
    {
      message = "Unable to retrieve information about job " + jobID + ":  " + e;
    }
    if (job == null)
    {
      if (message == null)
      {
        message = "Unable to retrieve information about job " + jobID;
      }
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      return;
    }


    // Make sure that the job is done running.
    if (! job.doneRunning())
    {
      message = "Unable to import persistent statistics for job " + jobID +
                " -- the job is not yet done running.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
      return;
    }


    // See if information was provided about the file to import.  If so, then
    // try to import that data.  Otherwise, provide a form to get that file.
    String importFile =
         request.getParameter(Constants.SERVLET_PARAM_DATA_IMPORT_FILE);
    if ((importFile != null) && (importFile.length() > 0))
    {
      try
      {
        File f = new File(importFile);
        if (! (f.exists() || f.isFile()))
        {
          message = "The specified input file " + importFile +
                    " does not exist or is not a file";
          infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
          requestInfo.response.addHeader(
                                    Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                    message);
        }
        else
        {
          FileInputStream inputStream     = new FileInputStream(f);
          ASN1Reader      reader          = new ASN1Reader(inputStream);
          ASN1Sequence    trackerSequence = null;

          try
          {
            trackerSequence = reader.readElement().decodeAsSequence();
          }
          catch (Exception e)
          {
            inputStream.close();
            throw e;
          }

          StatTracker[] importTrackers =
               StatEncoder.sequenceToTrackers(trackerSequence);
          if (importTrackers.length == 0)
          {
            message = "No statistical information was found in the import " +
                      "file";
            infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
            requestInfo.response.addHeader(
                                      Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                      message);
          }
          else
          {
            String clientID = importTrackers[0].getClientID();
            ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
            StatTracker[] jobTrackers = job.getStatTrackers();

            for (int i=0; i < jobTrackers.length; i++)
            {
              if (! jobTrackers[i].getClientID().equalsIgnoreCase(clientID))
              {
                trackerList.add(jobTrackers[i]);
              }
            }

            for (int i=0; i < importTrackers.length; i++)
            {
              trackerList.add(importTrackers[i]);
            }

            StatTracker[] newTrackers = new StatTracker[trackerList.size()];
            trackerList.toArray(newTrackers);
            job.setStatTrackers(newTrackers);
            configDB.writeJob(job);

            infoMessage.append("The job was successfully updated with the " +
                               "information from the provided persistent " +
                               "stat data file.<BR>");
            generateViewJobBody(requestInfo, job);
            return;
          }
        }
      }
      catch (Exception e)
      {
        message = "An error occurred while attempting to import the " +
                  "persistent statistical data:  " +
                  JobClass.stackTraceToString(e);
        infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
        requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                       message);
        return;
      }
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Import Persistent Statistics for Job " + jobID +
                    "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Specify the path on the SLAMD server system to the " +
                    "file containing the persistent statistical data." + EOL);
    htmlBody.append("<BR>");
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_IMPORT_PERSISTENT) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                          jobID) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <BR>" + EOL);
    htmlBody.append("  Data File Path:  " + EOL);
    htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_DATA_IMPORT_FILE +
                    "\" SIZE=\"80\">" + EOL);
    htmlBody.append("  <BR><BR>" + EOL);
    htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing necessary to cancel a pending or running job,
   * including first obtaining confirmation from the user so that a job is not
   * inadvertently cancelled.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleCancelJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleCancelJob()");

    // If the user doesn't have cancel permission, then they can't see this
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "cancel jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // There should have been a job ID specified.  If not, then there's not
    // really much that can be done, so just print an info message and leave
    // the rest of the page blank.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      String message = "Unable to determine the job ID of the job to cancel.";
      infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
      requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                     message);
    }
    else
    {
      // Cancelling a job requires confirmation.  If confirmation has not
      // yet been obtained, then display a form that allows the user to
      // provide this confirmation.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Cancel Job " + jobID + "</SPAN>" + EOL);
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
                                       Constants.SERVLET_SECTION_JOB_CANCEL) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobID) + EOL);
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
      else if (confirmStr.equalsIgnoreCase("yes"))
      {
        scheduler.cancelJob(jobID, false);
        infoMessage.append("A request has been sent to cancel job " + jobID +
                           ".  It may take a short amount of time for the " +
                           "job to actually be stopped.<BR>" + EOL);
        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
      else
      {
        infoMessage.append("Job " + jobID + " was not cancelled.<BR>" + EOL);
        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
    }
  }



  /**
   * Handles all processing necessary to cancel a pending job and remove it from
   * SLAMD entirely, rather than cancelling it and moving it to the completed
   * jobs page.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleCancelAndDelete(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleCancelAndDelete()");

    // If the user doesn't have cancel or delete permission, then they can't
    // see this
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "cancel jobs.");
      return;
    }
    if (! requestInfo.mayDeleteJob)
    {
      logMessage(requestInfo, "No mayDeleteJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "delete jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // There should have been a job ID specified.  If not, then there's not
    // really much that can be done, so just print an info message and leave
    // the rest of the page blank.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      infoMessage.append("ERROR:  Unable to determine the job ID of the " +
                         "job to cancel.<BR>" + EOL);
    }
    else
    {
      // Retrieve the job from the scheduler to see if it is actually in the
      // pending jobs queue, and that it has not yet started running.
      Job job = scheduler.getPendingJob(jobID);
      if (job == null)
      {
        infoMessage.append("ERROR:  Job " + jobID + " is not contained in " +
                           "the pending jobs queue.<BR>" + EOL);
        handleViewJob(requestInfo, null, null, jobID);
        return;
      }
      else if (job.getOptimizingJobID() != null)
      {
        infoMessage.append("ERROR:  Job " + jobID + " is associated with an " +
                           "optimizing job and may not be deleted<BR>." + EOL);
        handleViewJob(requestInfo, null, null, jobID);
        return;
      }


      // Cancelling a job requires confirmation.  If confirmation has not
      // yet been obtained, then display a form that allows the user to
      // provide this confirmation.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Cancel Job " + jobID + "</SPAN>" + EOL);
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
                             Constants.SERVLET_SECTION_JOB_CANCEL_AND_DELETE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobID) + EOL);
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
      else if (confirmStr.equalsIgnoreCase("yes"))
      {
        scheduler.cancelAndDeleteJob(jobID);
        infoMessage.append("Job " + jobID + " has been cancelled and removed " +
                           "from SLAMD.<BR>" + EOL);
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                      null, null);
        return;
      }
      else
      {
        infoMessage.append("Job " + jobID + " was not cancelled.<BR>" + EOL);
        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
    }
  }



  /**
   * Handles all processing necessary to delete information from the
   * configuration directory about a job that has been completed.  It can be
   * used to delete information about a single job, or about a range of jobs
   * completed before a specified time.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDeleteJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDeleteJob()");

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


    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID != null) && (jobID.length() > 0))
    {
      // Deleting a job requires confirmation.  If it hasn't been provided, then
      // request it.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        // First, retrieve the job and make sure that it is not associated with
        // an optimizing job.
        Job job;
        try
        {
          job = configDB.getJob(jobID);
        }
        catch (Exception e)
        {
          infoMessage.append("Unable to retrieve job " + jobID +
                             " from the configuration directory:  " + e +
                             "<BR>" + EOL);
          return;
        }

        String optimizingJobID = job.getOptimizingJobID();
        if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
        {
          OptimizingJob optimizingJob = null;
          try
          {
            optimizingJob = getOptimizingJob(optimizingJobID);
          } catch (Exception e) {}

          if (optimizingJob != null)
          {
            infoMessage.append("ERROR:  You cannot remove a job iteration " +
                               "that is part of an optimizing job without " +
                               "first removing the optimizing job definition." +
                               "<BR>" + EOL);
            generateViewJobBody(requestInfo, job);
            return;
          }
        }

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Delete Job " + jobID + "</SPAN>" + EOL);
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
                                       Constants.SERVLET_SECTION_JOB_DELETE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobID) + EOL);
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
      else if (confirmStr.equalsIgnoreCase("yes"))
      {
        try
        {
          configDB.removeJob(jobID);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Delete Successful</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Information about job " + jobID +
                          " has been removed from the configuration " +
                          "directory." + EOL);
          htmlBody.append("<BR><BR>" + EOL);

          String link =
                      generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                                   "here");
          htmlBody.append("Click " + link + " to go to the completed jobs " +
                          "page." + EOL);

        }
        catch (DatabaseException de)
        {
          infoMessage.append("ERROR:  " + de.getMessage() + "<BR>" + EOL);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Delete Failed</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Information about job " + jobID +
                          " could not be removed from the configuration " +
                          "directory." + EOL);
          htmlBody.append("See the error message above for additional " +
                          "information about the failure."  + EOL);
        }
      }
      else
      {
        infoMessage.append("Job " + jobID + " was not deleted.<BR>" + EOL);
        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
    }
  }



  /**
   * Handles all processing necessary to temporarily disable a pending job,
   * including first obtaining confirmation from the user so that a job is not
   * inadvertently disabled.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDisableJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDisableJob()");

    // If the user doesn't have permission to schedule jobs, then they can't
    // see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "disable jobs.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // There should have been a job ID specified.  If not, then there's not
    // really much that can be done, so just print an info message and leave
    // the rest of the page blank.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      infoMessage.append("ERROR:  Unable to determine the job ID of the " +
                         "job to disable.<BR>" + EOL);
    }
    else
    {
      // Disabling a job requires confirmation.  If confirmation has not
      // yet been obtained, then display a form that allows the user to
      // provide this confirmation.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disable Job " + jobID + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Are you sure that you want to disable this job?" +
                        EOL);
        htmlBody.append("<BR>");
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_DISABLE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobID) + EOL);
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
      else if (confirmStr.equalsIgnoreCase("yes"))
      {
        try
        {
          scheduler.disableJob(jobID);
          infoMessage.append("Job " + jobID +
                             " has been temporarily disabled.<BR>" + EOL);
        }
        catch (SLAMDServerException sse)
        {
          infoMessage.append("ERROR:  Unable to disable job " + jobID +
                             " -- " + sse + "<BR>" + EOL);
        }

        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
      else
      {
        infoMessage.append("Job " + jobID + " was not disabled.<BR>" + EOL);
        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
    }
  }



  /**
   * Handles all processing necessary to re-enable a disabled job, including
   * first obtaining confirmation from the user so that a job is not
   * inadvertently re-enabled.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleEnableJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEnableJob()");

    // If the user doesn't have permission to schedule jobs, then they can't
    // see this
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "enable jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // There should have been a job ID specified.  If not, then there's not
    // really much that can be done, so just print an info message and leave
    // the rest of the page blank.
    String jobID = request.getParameter(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobID == null) || (jobID.length() == 0))
    {
      infoMessage.append("ERROR:  Unable to determine the job ID of the " +
                         "job to enable.<BR>" + EOL);
    }
    else
    {
      // Enabling a job requires confirmation.  If confirmation has not
      // yet been obtained, then display a form that allows the user to
      // provide this confirmation.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Enable Job " + jobID + "</SPAN>" + EOL);
        htmlBody.append("<BR>" + EOL);
        htmlBody.append("Are you sure that you want to enable this job?" +
                        EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_ENABLE) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobID) + EOL);
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
      else if (confirmStr.equalsIgnoreCase("yes"))
      {
        try
        {
          scheduler.enableJob(jobID);
          infoMessage.append("Job " + jobID +
                             " has been re-enabled.<BR>" + EOL);
        }
        catch (SLAMDServerException sse)
        {
          infoMessage.append("ERROR:  Unable to re-enable job " + jobID +
                             " -- " + sse + "<BR>" + EOL);
        }

        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
      }
      else
      {
        infoMessage.append("Job " + jobID + " was not re-enabled.<BR>" + EOL);
        try
        {
          generateViewJobBody(requestInfo, scheduler.getJob(jobID));
        } catch (SLAMDServerException sse) {}
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
    JobClass      jobClass               = null;
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
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED, null,
                        jobID);
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
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED, null,
                        jobID);
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
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED, null,
                        jobID);
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
                    description + "\" SIZE=\"40\"></TD>" + EOL);
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
                    startTimeStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    durationStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                      clientsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "COLS=\"40\">" + clientsStr + "</TEXTAREA></TD>" + EOL);
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
                    "\" ROWS=\"5\" COLS=\"40\">" + clientsStr +
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
                    minThreadsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    maxThreadsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    incrementStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "\" VALUE=\"" + startupDelayStr + "\" SIZE=\"40\"></TD>" +
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
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
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
                    "\" SIZE=\"40\"></TD>" + EOL);
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
                    reRunDurationStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                      "\" VALUE=\"" + mailStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    startTimeStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    durationStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                      clientsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "COLS=\"40\">" + clientsStr + "</TEXTAREA></TD>" + EOL);
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
                    "\" ROWS=\"5\" COLS=\"40\">" + clientsStr +
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
                    minThreadsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    maxThreadsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    incrementStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "\" VALUE=\"" + startupDelayStr + "\" SIZE=\"40\"></TD>" +
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
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
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
                    "\" SIZE=\"40\"></TD>" + EOL);
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
                    reRunDurationStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                      "\" VALUE=\"" + mailStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    startTimeStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    durationStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                      clientsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    "COLS=\"40\">" + clientsStr + "</TEXTAREA></TD>" + EOL);
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
                    "\" ROWS=\"5\" COLS=\"40\">" + clientsStr +
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
                    minThreadsStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                    maxThreadsStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread increment to use.
    String incrementStr = String.valueOf(optimizingJob.getThreadIncrement());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Increment Between Iterations " + star +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_THREAD_INCREMENT + "\" VALUE=\"" +
                    incrementStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    // The thread startup delay to use.
    String startupDelayStr =
                String.valueOf(optimizingJob.getThreadStartupDelay());
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + startupDelayStr + "\" SIZE=\"40\"></TD>" +
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
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
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
                    "\" SIZE=\"40\"></TD>" + EOL);
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
                    reRunDurationStr + "\" SIZE=\"40\"></TD>" + EOL);
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
                      "\" VALUE=\"" + mailStr + "\" SIZE=\"40\"></TD>" + EOL);
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
      for (int i=0; i < parameters.length; i++)
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
          htmlBody.append("    <TD>" + parameters[i].getDisplayName() +
                          "</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + parameters[i].getHTMLDisplayValue() +
                          "</TD>" + EOL);
        }

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
   * @return  The requested optimizing job, or <CODE>null</CODE> if the
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
}

