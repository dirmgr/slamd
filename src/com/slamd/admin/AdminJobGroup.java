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



import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

import com.sleepycat.je.DatabaseException;

import com.slamd.common.Constants;
import com.slamd.common.DurationParser;
import com.slamd.common.SLAMDException;
import com.slamd.db.JobFolder;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.job.SingleStatisticOptimizationAlgorithm;
import com.slamd.jobgroup.JobGroup;
import com.slamd.jobgroup.JobGroupItem;
import com.slamd.jobgroup.JobGroupJob;
import com.slamd.jobgroup.JobGroupOptimizingJob;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.server.SLAMDServerException;

import static com.unboundid.util.StaticUtils.secondsToHumanReadableDuration;

import static com.slamd.admin.AdminJob.*;
import static com.slamd.admin.AdminServlet.*;
import static com.slamd.admin.AdminUI.*;



/**
 * This class provides a set of methods for providing logic for managing job
 * groups.
 */
public class AdminJobGroup
{
  /**
   * Handles all processing related to viewing information about the job groups
   * defined in the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJobGroups(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewJobGroups()");


    // Get the important state information for this request.
    String        servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder htmlBody       = requestInfo.htmlBody;
    StringBuilder infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job group information");
      return;
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Manage Job Groups</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);


    // Get a list of the available job groups.
    JobGroup[] jobGroups;
    try
    {
      jobGroups = configDB.getSummaryJobGroups();
    }
    catch (DatabaseException de)
    {
      infoMessage.append("ERROR -- Unable to retrieve the list of job groups " +
                         "defined in the configuration:  " + de.getMessage() +
                         EOL);
      return;
    }


    if ((jobGroups == null) || (jobGroups.length == 0))
    {
      htmlBody.append("No job groups have been defined in the SLAMD server." +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }
    else
    {
      htmlBody.append("The following job groups have been defined in the " +
                      "SLAMD server:" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("<UL>" + EOL);
      for (int i=0; i < jobGroups.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                                   Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroups[i].getName(),
                                   jobGroups[i].getName());

        String description = jobGroups[i].getDescription();

        htmlBody.append("  <LI>" + link);

        if ((description != null) && (description.length() > 0))
        {
          htmlBody.append(" -- " + description);
        }

        htmlBody.append("</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }


    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                  Constants.SERVLET_SECTION_JOB_ADD_GROUP) +
                      EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Create a New Job " +
                      "Group\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing related to viewing information about a job group
   * defined in the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJobGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job group information");
      return;
    }


    // Get the name of the job group to view.
    String groupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    handleViewJobGroup(requestInfo, groupName);
  }



  /**
   * Handles all processing related to viewing information about a job group
   * defined in the SLAMD server.
   *
   * @param  requestInfo   The state information for this request.
   * @param  jobGroupName  The name of the job group to view.
   */
  static void handleViewJobGroup(RequestInfo requestInfo, String jobGroupName)
  {
    logMessage(requestInfo, "In handleViewJobGroup()");


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
                               "view job group information");
      return;
    }


    if ((jobGroupName == null) || (jobGroupName.length() == 0))
    {
      infoMessage.append("ERROR:  No job group name was specified.<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }


    // Get the requested job group from the configuration database.
    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);

      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database.");
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Could not decode job group \"" +
                         jobGroupName + "\":  " + e.getMessage() + ".<BR>" +
                         EOL);
      return;
    }


    // See if a particular job or optimizing job has been selected.  If so, then
    // display information about it, but only for the "view job group"
    // subsection.
    if (requestInfo.subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GROUP))
    {
      String jobName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME);
      if (jobName != null)
      {
        handleViewJobGroupJob(requestInfo, jobGroup, jobName);
        return;
      }

      String optimizingJobName =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME);
      if (optimizingJobName != null)
      {
        handleViewJobGroupOptimizingJob(requestInfo, jobGroup,
                                        optimizingJobName);
        return;
      }
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">View Job Group " + jobGroupName + "</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    ArrayList jobList = jobGroup.getJobList();
    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("<TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("  <TR>" + EOL);

      if (! jobList.isEmpty())
      {
        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_SCHEDULE_GROUP) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Schedule " +
                        "This Job Group\">" + EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
      }

      htmlBody.append("    <TD>" + EOL);
      htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.
                                SERVLET_SECTION_JOB_CLONE_GROUP)
                      + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroupName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Clone This " +
                      "Job Group\">" + EOL);
      htmlBody.append("      </FORM>" + EOL);
      htmlBody.append("    </TD>" + EOL);

      htmlBody.append("    <TD>" + EOL);
      htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.
                                SERVLET_SECTION_JOB_REMOVE_GROUP)
                      + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroupName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Delete This " +
                      "Job Group\">" + EOL);
      htmlBody.append("      </FORM>" + EOL);
      htmlBody.append("    </TD>" + EOL);

      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("</TABLE>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }


    // If there is a description, show it.
    String description = jobGroup.getDescription();
    if ((description != null) && (description.length() > 0))
    {
      htmlBody.append("<B>Job Group Description</B><BR>" + EOL);
      htmlBody.append(description + "<BR><BR>" + EOL);
    }

    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                      Constants.SERVLET_SECTION_JOB_EDIT_GROUP_DESCRIPTION) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroup.getName()) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit Job Group " +
                      "Name/Description\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
    }


    htmlBody.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Jobs Contained in This " +
                    "Group</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    if (jobList.isEmpty())
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">There are currently no jobs in " +
                      "this job group.</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      if (requestInfo.mayScheduleJob)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_ADD_JOB_TO_GROUP) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" VALUE=\"Add a " +
                        "Job\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);

        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                        Constants.
                             SERVLET_SECTION_JOB_ADD_OPTIMIZING_JOB_TO_GROUP)
                        + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" VALUE=\"Add an " +
                        "Optimizing Job\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("        </TR>" + EOL);
        htmlBody.append("      </TABLE>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }
    else
    {
      String style    = Constants.STYLE_JOB_SUMMARY_LINE_A;
      String lastName = null;
      for (int i=0; i < jobList.size(); i++)
      {
        style = ((i % 2) == 0)
                ? Constants.STYLE_JOB_SUMMARY_LINE_A
                : Constants.STYLE_JOB_SUMMARY_LINE_B;

        Object o = jobList.get(i);
        if (o instanceof JobGroupJob)
        {
          JobGroupJob job = (JobGroupJob) o;
          lastName        = job.getName();
          String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                     Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                                     Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroup.getName(),
                                     Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME,
                                     lastName, lastName);

          htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
          htmlBody.append("    <TD>" + link + "</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + job.getJobClass().getJobName() +
                          "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
        else if (o instanceof JobGroupOptimizingJob)
        {
          JobGroupOptimizingJob optimizingJob = (JobGroupOptimizingJob) o;
          lastName = optimizingJob.getName();
          String link =
               generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                    Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                    Constants.SERVLET_PARAM_JOB_GROUP_NAME, jobGroup.getName(),
                    Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME,
                    lastName, lastName);

          htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
          htmlBody.append("    <TD>" + link + "</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>Optimizing " +
                          optimizingJob.getJobClass().getJobName() + "</TD>" +
                          EOL);
          htmlBody.append("  </TR>" + EOL);
        }
      }

      if (requestInfo.mayScheduleJob)
      {
        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_ADD_JOB_TO_GROUP) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" VALUE=\"Add a " +
                        "Job\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);

        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                        Constants.
                             SERVLET_SECTION_JOB_ADD_OPTIMIZING_JOB_TO_GROUP)
                        + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" VALUE=\"Add an " +
                        "Optimizing Job\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                        Constants.SERVLET_SECTION_JOB_REMOVE_JOB_FROM_GROUP)
                        + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroup.getName()) + EOL);
        htmlBody.append("              " +
                        generateHidden(
                             Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME,
                             lastName) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" VALUE=\"Remove " +
                        "Last Job from Group\">" + EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("        </TR>" + EOL);
        htmlBody.append("      </TABLE>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>Common Parameters</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    Parameter[] groupParameters = jobGroup.getParameters().getParameters();
    if ((groupParameters == null) || (groupParameters.length == 0))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">No common parameters have been " +
                      "defined.</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
    }
    else
    {
      for (int i=0; i < groupParameters.length; i++)
      {
        Parameter p = groupParameters[i];
        String style = ((i % 2) == 0)
                       ? Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        String displayName = p.getDisplayName();
        if (displayName == null)
        {
          displayName = p.getName();
        }

        description = p.getDescription();
        if (description == null)
        {
          description = "";
        }

        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>" + displayName + "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + description + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }

      if (requestInfo.mayScheduleJob)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroup.getName()) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit Job " +
                        "Group Parameters\">" + EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles all processing related to viewing an individual job in a job group.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobGroup     The job group containing the job to view.
   * @param  jobName      The name of the job to view.
   */
  static void handleViewJobGroupJob(RequestInfo requestInfo, JobGroup jobGroup,
                                    String jobName)
  {
    logMessage(requestInfo, "In handleViewJobGroupJob()");


    // Get the important state information for this request.
    String        servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder htmlBody       = requestInfo.htmlBody;
    StringBuilder infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job group information.");
      return;
    }


    // Get the job to view from the job group.
    JobGroupJob job = null;
    ArrayList<JobGroupItem> jobList = jobGroup.getJobList();
    for (int i=0; i < jobList.size(); i++)
    {
      Object o = jobList.get(i);
      if (o instanceof JobGroupJob)
      {
        if (((JobGroupJob) o).getName().equals(jobName))
        {
          job = (JobGroupJob) o;
          break;
        }
      }
    }

    if (job == null)
    {
      infoMessage.append("ERROR:  Job \"" + jobName + "\" does not exist in " +
                         "job group \"" + jobGroup.getName() + "\".<BR>" + EOL);
      return;
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">View Job Group Job \"" + jobName + "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_EDIT_GROUP_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroup.getName()) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME,
                                     jobName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit Job\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR>" + EOL);
    }

    htmlBody.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>General Parameters</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Name</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + jobName + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroup.getName(), jobGroup.getName());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Group</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + link + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Type</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getJobClass().getJobName() + "</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                        Constants.SERVLET_SECTION_JOB_VIEW_CLASSES,
                        Constants.SERVLET_PARAM_JOB_CLASS,
                        job.getJobClass().getClass().getName(),
                        job.getJobClass().getClass().getName());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Class</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + link + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String durationStr;
    if (job.getDuration() > 0)
    {
      durationStr = secondsToHumanReadableDuration(job.getDuration());
    }
    else
    {
      durationStr = "";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + durationStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Number of Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getNumClients() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Threads per Client</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getThreadsPerClient() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + job.getThreadStartupDelay() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Statistics Collection Interval</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" +
         secondsToHumanReadableDuration(job.getCollectionInterval()) + "</TD>" +
         EOL);
    htmlBody.append("  </TR>" + EOL);

    ArrayList dependencies = job.getDependencies();
    if ((dependencies != null) && (! dependencies.isEmpty()))
    {
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + EOL);

      for (int i=0; i < dependencies.size(); i++)
      {
        htmlBody.append("      " + dependencies.get(i) + "<BR>" + EOL);
      }

      htmlBody.append("    </TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }


    LinkedHashMap mappedParameters = job.getMappedParameters();
    if ((mappedParameters != null) && (! mappedParameters.isEmpty()))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Mapped Job " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      int i=0;
      Iterator iterator = mappedParameters.keySet().iterator();
      while (iterator.hasNext())
      {
        String jobParamName   = (String) iterator.next();
        String groupParamName = (String) mappedParameters.get(jobParamName);

        Parameter jobParam =
             job.getJobClass().getParameterStubs().getParameter(jobParamName);
        Parameter groupParam =
             jobGroup.getParameters().getParameter(groupParamName);

        String jobParamDisplayName;
        if ((jobParam == null) || (jobParam.getDisplayName() == null))
        {
          jobParamDisplayName = jobParamName;
        }
        else
        {
          jobParamDisplayName = jobParam.getDisplayName();
        }

        String groupParamDisplayName;
        if ((groupParam == null) || (groupParam.getDisplayName() == null))
        {
          groupParamDisplayName = groupParamName;
        }
        else
        {
          groupParamDisplayName = groupParam.getDisplayName();
        }

        String style = ((i++ % 2) == 0)
                       ? Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>" + jobParamDisplayName + "</TD>" + EOL);
        htmlBody.append("    <TD> --&gt; </TD>" + EOL);
        htmlBody.append("    <TD>" + groupParamDisplayName + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    Parameter[] fixedParameters = job.getFixedParameters().getParameters();
    if ((fixedParameters != null) && (fixedParameters.length > 0))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Fixed Job Parameters</B></TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);

      for (int i=0; i < fixedParameters.length; i++)
      {
        String style = ((i % 2) == 0)
                       ? Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        String displayName = fixedParameters[i].getDisplayName();
        if (displayName == null)
        {
          displayName = fixedParameters[i].getName();
        }

        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>" + displayName + "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + fixedParameters[i].getHTMLDisplayValue() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles all processing related to viewing an individual optimizing job in a
   * job group.
   *
   * @param  requestInfo        The state information for this request.
   * @param  jobGroup           The job group containing the job to view.
   * @param  optimizingJobName  The name of the optimizing job to view.
   */
  static void handleViewJobGroupOptimizingJob(RequestInfo requestInfo,
                                              JobGroup jobGroup,
                                              String optimizingJobName)
  {
    logMessage(requestInfo, "In handleViewJobGroupJob()");


    // Get the important state information for this request.
    String        servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder htmlBody       = requestInfo.htmlBody;
    StringBuilder infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job group information.");
      return;
    }


    // Get the optimizing job to view from the job group.
    JobGroupOptimizingJob optimizingJob = null;
    ArrayList<JobGroupItem> jobList = jobGroup.getJobList();
    for (int i=0; i < jobList.size(); i++)
    {
      Object o = jobList.get(i);
      if (o instanceof JobGroupOptimizingJob)
      {
        if (((JobGroupOptimizingJob) o).getName().equals(optimizingJobName))
        {
          optimizingJob = (JobGroupOptimizingJob) o;
          break;
        }
      }
    }

    if (optimizingJob == null)
    {
      infoMessage.append("ERROR:  Optimizing job \"" + optimizingJobName +
                         "\" does not exist in job group \"" +
                         jobGroup.getName() + "\".<BR>" + EOL);
      return;
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">View Job Group Optimizing Job \"" + optimizingJobName +
                    "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    if (requestInfo.mayScheduleJob)
    {
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                      Constants.SERVLET_SECTION_JOB_EDIT_GROUP_OPTIMIZING_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroup.getName()) + EOL);
      htmlBody.append("  " +
                      generateHidden(
                      Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME,
                      optimizingJobName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Edit Optimizing " +
                      "Job\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR>" + EOL);
    }

    htmlBody.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD COLSPAN=\"3\"><B>General Parameters</B></TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Optimizing Job Name</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJobName + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroup.getName(), jobGroup.getName());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Group</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + link + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Type</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getJobClass().getJobName() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                        Constants.SERVLET_SECTION_JOB_VIEW_CLASSES,
                        Constants.SERVLET_PARAM_JOB_CLASS,
                        optimizingJob.getJobClass().getClass().getName(),
                        optimizingJob.getJobClass().getClass().getName());
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Job Class</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + link + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String durationStr;
    if (optimizingJob.getDuration() > 0)
    {
      durationStr = secondsToHumanReadableDuration(optimizingJob.getDuration());
    }
    else
    {
      durationStr = "";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + durationStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Delay Between Iterations</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getDelayBetweenIterations() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Number of Clients</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getNumClients() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Minimum Number of Threads</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getMinThreads() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    String maxThreadsStr;
    if (optimizingJob.getMaxThreads() > 0)
    {
      maxThreadsStr = String.valueOf(optimizingJob.getMaxThreads());
    }
    else
    {
      maxThreadsStr = "(not specified)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + maxThreadsStr + "</TD>" + EOL);
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

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Max. Consecutive Non-Improving Iterations</TD>" +
                    EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.getMaxNonImprovingIterations() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    htmlBody.append("    <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + optimizingJob.reRunBestIteration() +
                    "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    if (optimizingJob.getReRunDuration() > 0)
    {
      durationStr = secondsToHumanReadableDuration(
           optimizingJob.getReRunDuration());
    }
    else
    {
      durationStr = "(not specified)";
    }
    htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    htmlBody.append("    <TD>Re-Run Duration</TD>" + EOL);
    htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("    <TD>" + durationStr + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    ArrayList dependencies = optimizingJob.getDependencies();
    if ((dependencies != null) && (! dependencies.isEmpty()))
    {
      htmlBody.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      htmlBody.append("    <TD>Job Dependencies</TD>" + EOL);
      htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("    <TD>" + EOL);

      for (int i=0; i < dependencies.size(); i++)
      {
        htmlBody.append("      " + dependencies.get(i) + "<BR>" + EOL);
      }

      htmlBody.append("    </TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }


    Parameter[] optimizationParameters =
         optimizingJob.getOptimizationParameters().getParameters();
    if ((optimizationParameters != null) && (optimizationParameters.length > 0))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Optimization Algorithm "+
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      for (int i=0; i < optimizationParameters.length; i++)
      {
        String style = ((i % 2) == 0)
                       ? Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        String displayName = optimizationParameters[i].getDisplayName();
        if (displayName == null)
        {
          displayName = optimizationParameters[i].getName();
        }

        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>" + displayName + "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" +
                        optimizationParameters[i].getHTMLDisplayValue() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    LinkedHashMap mappedParameters = optimizingJob.getMappedParameters();
    if ((mappedParameters != null) && (! mappedParameters.isEmpty()))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Mapped Job " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);

      int i=0;
      Iterator iterator = mappedParameters.keySet().iterator();
      while (iterator.hasNext())
      {
        String jobParamName   = (String) iterator.next();
        String groupParamName = (String) mappedParameters.get(jobParamName);

        Parameter jobParam =
             optimizingJob.getJobClass().getParameterStubs().getParameter(
                                                                  jobParamName);
        Parameter groupParam =
             jobGroup.getParameters().getParameter(groupParamName);

        String jobParamDisplayName;
        if ((jobParam == null) || (jobParam.getDisplayName() == null))
        {
          jobParamDisplayName = jobParamName;
        }
        else
        {
          jobParamDisplayName = jobParam.getDisplayName();
        }

        String groupParamDisplayName;
        if ((groupParam == null) || (groupParam.getDisplayName() == null))
        {
          groupParamDisplayName = groupParamName;
        }
        else
        {
          groupParamDisplayName = groupParam.getDisplayName();
        }

        String style = ((i++ % 2) == 0)
                       ? Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>" + jobParamDisplayName + "</TD>" + EOL);
        htmlBody.append("    <TD> --&gt; </TD>" + EOL);
        htmlBody.append("    <TD>" + groupParamDisplayName + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    Parameter[] fixedParameters =
         optimizingJob.getFixedParameters().getParameters();
    if ((fixedParameters != null) && (fixedParameters.length > 0))
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD COLSPAN=\"3\"><B>Fixed Job Parameters</B></TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);

      for (int i=0; i < fixedParameters.length; i++)
      {
        String style = ((i % 2) == 0)
                       ? Constants.STYLE_JOB_SUMMARY_LINE_A
                       : Constants.STYLE_JOB_SUMMARY_LINE_B;

        String displayName = fixedParameters[i].getDisplayName();
        if (displayName == null)
        {
          displayName = fixedParameters[i].getName();
        }

        htmlBody.append("  <TR CLASS=\"" + style + "\">" + EOL);
        htmlBody.append("    <TD>" + displayName + "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + fixedParameters[i].getHTMLDisplayValue() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles all processing related to editing the name and/or description of a
   * job group.
   *
   * @param  requestInfo  The state information for this request
   */
  static void handleEditJobGroupDescription(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEditJobGroupDescription()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit a job group.");
      return;
    }


    // Get the job group to edit.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if ((jobGroupName == null) || (jobGroupName.length() == 0))
    {
      infoMessage.append("ERROR:  No job group was specified for which to " +
                         "edit the name and/or description.<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }

    JobGroup jobGroup = null;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration " +
                           "database.<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Could not retrieve job group \"" +
                         jobGroupName + "\" from the configuration " +
                         "database:  " + e + ".<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }


    // See if the user submitted the form.  If so, then set the corresponding
    // parameters.  Otherwise, display the form to retrieve them.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      boolean jobValid = true;

      // There must be a name for the job group, and it must not conflict with
      // the name of any other job group.
      String newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
      if ((newName == null) || (newName.length() == 0))
      {
        infoMessage.append("ERROR:  No new name was provided for the job " +
                           "group.<BR>" + EOL);
        jobValid = false;
      }
      else if (! newName.equals(jobGroup.getName()))
      {
        try
        {
          JobGroup existingGroup = configDB.getJobGroup(newName);
          if (existingGroup != null)
          {
            infoMessage.append("ERROR:  The new name provided for the job " +
                               "group conflicts with the name of an existing " +
                               "job group.<BR>" + EOL);
            jobValid = false;
          }
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Could not determine if the new name " +
                             "conflicted with the name of any other job " +
                             "groups in the configuration database:  " + e +
                             ".<BR>" + EOL);
          jobValid = false;
        }
      }


      // Get the new description for the job group.  We don't care what it's
      // value is.
      String newDescription =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);


      // If everything looks OK, then apply the update.
      if (jobValid)
      {
        jobGroup.setName(newName);
        jobGroup.setDescription(newDescription);

        try
        {
          configDB.writeJobGroup(jobGroup);

          // If the job group was renamed, then get rid of the record for the
          // old name.
          if (! jobGroupName.equals(newName))
          {
            configDB.removeJobGroup(jobGroupName);
          }

          infoMessage.append("Successfully updated the name and/or " +
                             "description for the job group.<BR>" + EOL);
          handleViewJobGroup(requestInfo, newName);
          return;
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Could not update the job group in the " +
                             "configuration database:  " + e + ".<BR>" + EOL);
          handleViewJobGroup(requestInfo);
          return;
        }
      }
    }


    // Display the form that allows the user to edit the job group name and/or
    // description.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Edit Name/Description for Job Group \"" + jobGroupName +
                    "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Please provide the new name and/or description for the " +
                    "job group:" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_EDIT_GROUP_DESCRIPTION) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }

    String newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
    if ((newName == null) || (newName.length() == 0))
    {
      newName = jobGroup.getName();
    }

    String newDescription =
                request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);
    if ((newDescription == null) || (newDescription.length() == 0))
    {
      newDescription = jobGroup.getDescription();
    }


    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Job Group Name</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_NEW_NAME + "\" VALUE=\"" + newName +
                    "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Job Group Description</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    newDescription + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Update " +
                    "Name/Description\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to editing the parameters in a job group.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleEditJobGroupParams(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEditJobGroupParams()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit a job group.");
      return;
    }


    // Get the job group to edit.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if ((jobGroupName == null) || (jobGroupName.length() == 0))
    {
      infoMessage.append("ERROR:  No job group name was given for which to " +
                         "edit the parameters.");
      handleViewJobGroups(requestInfo);
      return;
    }

    JobGroup jobGroup = null;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);

      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  No job group \"" + jobGroupName +
                           "\" exists in the configuration database.");
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Could not retrieve job group \"" +
                         jobGroupName +
                         "\" from the configuration database:  " + e);
      handleViewJobGroups(requestInfo);
      return;
    }


    // Get the sets of jobs and optimizing jobs associated with this group.
    ArrayList<JobGroupItem> jobList = jobGroup.getJobList();
    ArrayList<JobGroupJob> jobs = new ArrayList<JobGroupJob>(jobList.size());
    ArrayList<JobGroupOptimizingJob> optimizingJobs =
         new ArrayList<JobGroupOptimizingJob>(jobList.size());
    for (int i=0; i < jobList.size(); i++)
    {
      Object o = jobList.get(i);
      if (o instanceof JobGroupJob)
      {
        jobs.add((JobGroupJob) o);
      }
      else if (o instanceof JobGroupOptimizingJob)
      {
        optimizingJobs.add((JobGroupOptimizingJob) o);
      }
    }


    // Get the parameters for this job group.
    Parameter[] params = jobGroup.getParameters().clone().getParameters();


    // Get the index of the selected parameter, if there is one.
    int selectedIndex = -1;
    String indexStr = request.getParameter(Constants.SERVLET_PARAM_INDEX);
    if (indexStr != null)
    {
      try
      {
        selectedIndex = Integer.parseInt(indexStr);
      } catch (Exception e) {}
    }


    // See if the user submitted the form.  If so, then figure out what to do
    // from there.
    String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if ((submitStr == null) || (selectedIndex < 0))
    {
      // We'll drop through to the form below.
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_MOVE_GROUP_PARAM))
    {
      // Get the new index for the parameter and make the move.
      try
      {
        int newIndex = Integer.parseInt(request.getParameter(
                                        Constants.SERVLET_PARAM_NEW_INDEX));

        ArrayList<Parameter> paramList =
             new ArrayList<Parameter>(params.length);
        for (int i=0; i < params.length; i++)
        {
          paramList.add(params[i]);
        }

        Parameter p = paramList.remove(selectedIndex);
        paramList.add(newIndex, p);
        paramList.toArray(params);

        jobGroup.setParameters(new ParameterList(params));
        configDB.writeJobGroup(jobGroup);
        infoMessage.append("Successfully moved the parameter in the " +
                           "parameter list.<BR>" + EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not move the parameter in the " +
                           "parameter list:  " + e + ".<BR>" + EOL);
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_INSERT_PARAM_SPACER))
    {
      // We want to insert a spacer before the selected element.  Grow the
      // parameter array by one and insert a placeholder parameter in the new
      // location.
      try
      {
        Parameter[] newParams = new Parameter[params.length+1];
        System.arraycopy(params, 0, newParams, 0, selectedIndex);
        System.arraycopy(params, selectedIndex, newParams, selectedIndex+1,
                         (params.length-selectedIndex));
        newParams[selectedIndex] = new PlaceholderParameter();
        params = newParams;

        jobGroup.setParameters(new ParameterList(params));
        configDB.writeJobGroup(jobGroup);
        infoMessage.append("Successfully inserted a spacer into the " +
                           "parameter list.<BR>" + EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not insert a spacer into the " +
                           "parameter list:  " + e + ".<BR>" + EOL);
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_REMOVE_PARAM_SPACER))
    {
      // We want to remove the selected element if it is a spacer.
      try
      {
        Parameter p = params[selectedIndex];
        if (p instanceof PlaceholderParameter)
        {
          Parameter[] newParams = new Parameter[params.length-1];
          System.arraycopy(params, 0, newParams, 0, selectedIndex);
          System.arraycopy(params, selectedIndex+1, newParams, selectedIndex,
                           (params.length-selectedIndex-1));
          params = newParams;

          jobGroup.setParameters(new ParameterList(params));
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully removed the spacer from the " +
                             "parameter list.<BR>" + EOL);
        }
        else
        {
          infoMessage.append("ERROR:  The specified parameter was not " +
                             "a spacer.<BR>" + EOL);
        }
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not remove the spacer from the " +
                           "parameter list:  " + e + ".<BR>" + EOL);
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_INSERT_PARAM_LABEL))
    {
      // We want to insert a label before the selected element.  First, see if
      // we have the text for the label.  If so, then grow the parameter array
      // by one and insert a label parameter in the new location.  Otherwise,
      // prompt the user for the new label.
      String label = request.getParameter(Constants.SERVLET_PARAM_LABEL_TEXT);
      if ((label == null) || (label.length() == 0))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Insert Label For Job Group \"" + jobGroupName +
                        "\"</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Enter the text for the new label parameter:");
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                             Constants.SUBMIT_STRING_INSERT_PARAM_LABEL) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(selectedIndex)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_LABEL_TEXT + "\" SIZE=\"40\">" +
                        EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        return;
      }
      else
      {
        try
        {
          Parameter[] newParams = new Parameter[params.length+1];
          System.arraycopy(params, 0, newParams, 0, selectedIndex);
          System.arraycopy(params, selectedIndex, newParams, selectedIndex+1,
                           (params.length-selectedIndex));
          newParams[selectedIndex] = new LabelParameter(label);
          params = newParams;

          jobGroup.setParameters(new ParameterList(params));
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully inserted a label into the " +
                             "parameter list.<BR>" + EOL);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Could not insert a label into the " +
                             "parameter list:  " + e + ".<BR>" + EOL);
        }
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_REMOVE_PARAM_LABEL))
    {
      // We want to remove the selected element if it is a label.
      try
      {
        Parameter p = params[selectedIndex];
        if (p instanceof LabelParameter)
        {
          Parameter[] newParams = new Parameter[params.length-1];
          System.arraycopy(params, 0, newParams, 0, selectedIndex);
          System.arraycopy(params, selectedIndex+1, newParams, selectedIndex,
                           (params.length-selectedIndex-1));
          params = newParams;

          jobGroup.setParameters(new ParameterList(params));
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully removed the label from the " +
                             "parameter list.<BR>" + EOL);
        }
        else
        {
          infoMessage.append("ERROR:  The specified parameter was not " +
                             "a label.<BR>" + EOL);
        }
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not remove the label from the " +
                           "parameter list:  " + e + ".<BR>" + EOL);
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_EDIT_LABEL_TEXT))
    {
      // Get the label text.
      String label = request.getParameter(Constants.SERVLET_PARAM_LABEL_TEXT);

      // See if the user submitted the form.  If not, then show it to them to
      // let them edit the label.  Otherwise, update the job group.
      String confirmStr =
           request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) || (confirmStr.length() == 0))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Edit Parameter Label For Job Group \"" +
                        jobGroupName + "\"</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Enter the new text for the label parameter:");
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                        Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                             Constants.SUBMIT_STRING_EDIT_LABEL_TEXT) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(selectedIndex)) +  EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_CONFIRMED, "1") +
                        EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_LABEL_TEXT +
                      "\" VALUE=\"" + label + "\" SIZE=\"40\">" +  EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        return;
      }
      else
      {
        try
        {
          Parameter p = params[selectedIndex];
          if (p instanceof LabelParameter)
          {
            params[selectedIndex] = new LabelParameter(label);
            jobGroup.setParameters(new ParameterList(params));
            configDB.writeJobGroup(jobGroup);
            infoMessage.append("Successfully edited the label text.<BR>" + EOL);
          }
          else
          {
            infoMessage.append("ERROR:  The specified parameter was not " +
                               "a label.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Could not edit the label text:  " + e +
                             ".<BR>" + EOL);
        }
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_REMOVE_GROUP_PARAM))
    {
      // We want to remove the selected parameter.
      try
      {
        Parameter p = params[selectedIndex];

        Parameter[] newParams = new Parameter[params.length-1];
        System.arraycopy(params, 0, newParams, 0, selectedIndex);
        System.arraycopy(params, selectedIndex+1, newParams, selectedIndex,
                         (params.length-selectedIndex-1));
        params = newParams;

        jobGroup.setParameters(new ParameterList(params));
        configDB.writeJobGroup(jobGroup);
        infoMessage.append("Successfully removed parameter \"" +
                           p.getDisplayName() +
                           " from the parameter list.<BR>" + EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not remove the specified parameter " +
                           "from the list:  " + e + ".<BR>" + EOL);
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_RENAME_GROUP_PARAM))
    {
      // See if a new name was given.  If so, then set it.  If not, then prompt
      // the user for it.
      try
      {
        String newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
        if ((newName == null) || (newName.length() == 0))
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Edit Parameter Name for Job Group \"" +
                          jobGroupName + "\"</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Enter the new display name for the \"" +
                          params[selectedIndex].getDisplayName() +
                          "\" parameter:");
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                         jobGroupName) + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                               Constants.SUBMIT_STRING_RENAME_GROUP_PARAM) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_INDEX,
                                         String.valueOf(selectedIndex)) +  EOL);

          if (requestInfo.debugHTML)
          {
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                          Constants.SERVLET_PARAM_NEW_NAME + "\" SIZE=\"40\"" +
                          " VALUE=\"" + params[selectedIndex].getDisplayName() +
                          "\">" + EOL);
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
          return;
        }


        // Make sure that the new name doesn't conflict with any other
        // parameters in the job group.
        boolean conflict = false;
        for (int i=0; i < params.length; i++)
        {
          if (i == selectedIndex)
          {
            continue;
          }
          else if (newName.equals(params[i].getDisplayName()))
          {
            infoMessage.append("ERROR:  The new display name for the " +
                               "parameter conflicts with the display name of " +
                               "another parameter in the list.<BR>" + EOL);
            conflict = true;
            break;
          }
        }
        if (! conflict)
        {
          params[selectedIndex].setDisplayName(newName);
          jobGroup.setParameters(new ParameterList(params));
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully updated the display name of the " +
                             "job group parameter.<BR>" + EOL);
        }
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not rename the job parameter:  " + e +
                           ".<BR>" + EOL);
      }
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_SET_PARAM_DEFAULT))
    {
      // See if the form was submitted.  If so, then process it.  Otherwise,
      // display it to the user.
      try
      {
        String confirmStr =
             request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
        if ((confirmStr == null) || (confirmStr.length() == 0))
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Edit Default Value for Job Group \"" +
                          jobGroupName + "\" Parameter \"" +
                          params[selectedIndex].getDisplayName() + "\"</SPAN>" +
                          EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Please choose the new default value for the \"" +
                          params[selectedIndex].getDisplayName() +
                          "\" parameter:");
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                         jobGroupName) + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                               Constants.SUBMIT_STRING_SET_PARAM_DEFAULT) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_INDEX,
                                         String.valueOf(selectedIndex)) +  EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                         "1") + EOL);

          if (requestInfo.debugHTML)
          {
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          htmlBody.append("  " +
                          params[selectedIndex].getHTMLInputForm(
                               Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + EOL);
          htmlBody.append("  <BR><BR>" + EOL);
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
          return;
        }


        // Try to set the new value for the parameter.
        params[selectedIndex].htmlInputFormToValue(request);
        jobGroup.setParameters(new ParameterList(params));
        configDB.writeJobGroup(jobGroup);
        infoMessage.append("Successfully updated the default value for the " +
                           "job group parameter.<BR>" + EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not update the default value:  " + e +
                           ".<BR>" + EOL);
      }
    }


    // Display the form allowing the user to edit the job group parameters.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Edit Parameters for Job Group \"" + jobGroupName +
                    "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroupName, jobGroupName);
    htmlBody.append("Edit the parameter names, default values, ordering, and " +
                    "spacing for mapped parameters in job group " + link +
                    '.' + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<TABLE BORDER=\"0\">" + EOL);

    for (int i=0; i < params.length; i++)
    {
      if (i > 0)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\"><HR></TD>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
        htmlBody.append("  <TR>" + EOL);
      }


      if (params[i] instanceof PlaceholderParameter)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">----- Spacer -----</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        if (params.length > 1)
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI +  "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                         jobGroupName) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_INDEX,
                                         String.valueOf(i)) +  EOL);

          if (requestInfo.debugHTML)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          htmlBody.append("        <SELECT NAME=\"" +
                          Constants.SERVLET_PARAM_NEW_INDEX + "\">" + EOL);

          for (int j=0; j < params.length; j++)
          {
            String name;
            if (params[j] instanceof PlaceholderParameter)
            {
              name = "----- Spacer -----";
            }
            else if (params[j] instanceof LabelParameter)
            {
              name = "----- Label:  " + params[j].getDisplayValue() + "-----";
            }
            else
            {
              name = params[j].getDisplayName();
            }

            if (j < i)
            {
              htmlBody.append("          <OPTION VALUE=\"" + j + "\">Before " +
                              name + EOL);
            }
            else if (j > i)
            {
              htmlBody.append("          <OPTION VALUE=\"" + j + "\">After " +
                              name + EOL);
            }
          }

          htmlBody.append("        </SELECT>" + EOL);
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_MOVE_GROUP_PARAM + "\">" +
                          EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_REMOVE_PARAM_SPACER + "\">" +
                        EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
      else if (params[i] instanceof LabelParameter)
      {
        String labelText = params[i].getDisplayValue();

        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">----- Label:  " + labelText +
                        " -----</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        if (params.length > 1)
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI +  "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                         jobGroupName) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_INDEX,
                                         String.valueOf(i)) +  EOL);

          if (requestInfo.debugHTML)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          htmlBody.append("        <SELECT NAME=\"" +
                          Constants.SERVLET_PARAM_NEW_INDEX + "\">" + EOL);

          for (int j=0; j < params.length; j++)
          {
            String name;
            if (params[j] instanceof PlaceholderParameter)
            {
              name = "----- Spacer -----";
            }
            else if (params[j] instanceof LabelParameter)
            {
              name = "----- Label:  " + params[j].getDisplayValue() + "-----";
            }
            else
            {
              name = params[j].getDisplayName();
            }

            if (j < i)
            {
              htmlBody.append("          <OPTION VALUE=\"" + j + "\">Before " +
                              name + EOL);
            }
            else if (j > i)
            {
              htmlBody.append("          <OPTION VALUE=\"" + j + "\">After " +
                              name + EOL);
            }
          }

          htmlBody.append("        </SELECT>" + EOL);
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_MOVE_GROUP_PARAM + "\">" +
                          EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_LABEL_TEXT,
                                       labelText) + EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_EDIT_LABEL_TEXT + "\">" +
                        EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_REMOVE_PARAM_LABEL + "\">" +
                        EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("        </TR>" + EOL);
        htmlBody.append("      </TABLE>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
      else
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_INSERT_PARAM_SPACER + "\">" +
                        EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_INSERT_PARAM_LABEL + "\">" +
                        EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);
        htmlBody.append("        </TR>" + EOL);
        htmlBody.append("      </TABLE>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);


        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD><B>Parameter Name</B></TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + params[i].getDisplayName() + "</TD>" +
                        EOL);
        htmlBody.append("  </TR>" + EOL);


        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD><B>Default Value</B></TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + params[i].getHTMLDisplayValue() + "</TD>" +
                        EOL);
        htmlBody.append("  </TR>" + EOL);


        if (params.length > 1)
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI +  "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                         jobGroupName) + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_INDEX,
                                         String.valueOf(i)) +  EOL);

          if (requestInfo.debugHTML)
          {
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          htmlBody.append("        <SELECT NAME=\"" +
                          Constants.SERVLET_PARAM_NEW_INDEX + "\">" + EOL);

          for (int j=0; j < params.length; j++)
          {
            String name;
            if (params[j] instanceof PlaceholderParameter)
            {
              name = "----- Spacer -----";
            }
            else if (params[j] instanceof LabelParameter)
            {
              name = "----- Label:  " + params[j].getDisplayValue() + "-----";
            }
            else
            {
              name = params[j].getDisplayName();
            }

            if (j < i)
            {
              htmlBody.append("          <OPTION VALUE=\"" + j + "\">Before " +
                              name + EOL);
            }
            else if (j > i)
            {
              htmlBody.append("          <OPTION VALUE=\"" + j + "\">After " +
                              name + EOL);
            }
          }

          htmlBody.append("        </SELECT>" + EOL);
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_MOVE_GROUP_PARAM + "\">" +
                          EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }


        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD COLSPAN=\"3\">" + EOL);
        htmlBody.append("      <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR>" + EOL);
        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_RENAME_GROUP_PARAM + "\">" +
                        EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);


        htmlBody.append("          <TD>" + EOL);
        htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI +  "\">" + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_JOB) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                        EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroupName) + EOL);
        htmlBody.append("              " +
                        generateHidden(Constants.SERVLET_PARAM_INDEX,
                                       String.valueOf(i)) +  EOL);

        if (requestInfo.debugHTML)
        {
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_SET_PARAM_DEFAULT + "\">" +
                        EOL);
        htmlBody.append("            </FORM>" + EOL);
        htmlBody.append("          </TD>" + EOL);


        boolean unused = true;
        for (int j=0; (unused && (j < jobs.size())); j++)
        {
          JobGroupJob job = jobs.get(j);
          Iterator iterator = job.getMappedParameters().values().iterator();
          while (iterator.hasNext())
          {
            if (params[i].getName().equals(iterator.next()))
            {
              unused = false;
              break;
            }
          }
        }
        for (int j=0; (unused && (j < optimizingJobs.size())); j++)
        {
          JobGroupOptimizingJob optimizingJob = optimizingJobs.get(j);
          Iterator iterator =
               optimizingJob.getMappedParameters().values().iterator();
          while (iterator.hasNext())
          {
            if (params[i].getName().equals(iterator.next()))
            {
              unused = false;
              break;
            }
          }
        }
        if (unused)
        {
          htmlBody.append("          <TD>" + EOL);
          htmlBody.append("            <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI +  "\">" + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                          Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS) +
                          EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                         jobGroupName) + EOL);
          htmlBody.append("              " +
                          generateHidden(Constants.SERVLET_PARAM_INDEX,
                                         String.valueOf(i)) +  EOL);

          if (requestInfo.debugHTML)
          {
            htmlBody.append("              " +
                            generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          htmlBody.append("              <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                          Constants.SUBMIT_STRING_REMOVE_GROUP_PARAM + "\">" +
                          EOL);
          htmlBody.append("            </FORM>" + EOL);
          htmlBody.append("          </TD>" + EOL);
        }
        htmlBody.append("        </TR>" + EOL);
        htmlBody.append("      </TABLE>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }


    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles all processing related to editing a job in a job group.
   *
   * @param  requestInfo  The state information for this request
   */
  static void handleEditJobGroupJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEditJobGroupJob()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit a job group.");
      return;
    }


    // Get the job group to edit.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if ((jobGroupName == null) || (jobGroupName.length() == 0))
    {
      infoMessage.append("ERROR:  No job group was specified for which to " +
                         "edit the job.<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }

    JobGroup jobGroup = null;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration " +
                           "database.<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Could not retrieve job group \"" +
                         jobGroupName + "\" from the configuration " +
                         "database:  " + e + ".<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }


    // Get the job group job to edit.
    String jobName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME);
    if ((jobName == null) || (jobName.length() == 0))
    {
      infoMessage.append("ERROR:  No job was selected for editing in the " +
                         "specified job group.<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }

    JobGroupJob job = null;
    ArrayList jobList = jobGroup.getJobList();
    for (int i=0; i < jobList.size(); i++)
    {
      Object o = jobList.get(i);
      if (o instanceof JobGroupJob)
      {
        JobGroupJob currentJob = (JobGroupJob) o;
        if (currentJob.getName().equals(jobName))
        {
          job = currentJob;
          break;
        }
      }
    }

    if (job == null)
    {
      infoMessage.append("ERROR:  There is no job named \"" + jobName +
                         "\" in job group \"" + jobGroupName + "\".<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }


    boolean submitted = false;
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      boolean jobValid = true;
      submitted = true;


      String newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
      if ((newName == null) || (newName.length() == 0))
      {
        infoMessage.append("ERROR:  The job must have a name.<BR>" + EOL);
        jobValid = false;
      }
      else if (! newName.equals(jobName))
      {
        boolean conflict = false;

        for (int i=0; i < jobList.size(); i++)
        {
          Object o = jobList.get(i);
          if (o instanceof JobGroupJob)
          {
            JobGroupJob j = (JobGroupJob) o;
            if (j.getName().equals(newName))
            {
              conflict = true;
              break;
            }
          }
          else if (o instanceof JobGroupOptimizingJob)
          {
            JobGroupOptimizingJob oj = (JobGroupOptimizingJob) o;
            if (oj.getName().equals(newName))
            {
              conflict = true;
              break;
            }
          }
        }

        if (conflict)
        {
          infoMessage.append("ERROR:  The new name provided for the job " +
                             "conflicts with the name of another job or " +
                             "optimizing job in the job group.<BR>" + EOL);
          jobValid = false;
        }
      }


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
          jobValid = false;
        }
      }


      int numClients = -1;
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((numClientsStr == null) || (numClientsStr.length() == 0))
      {
        infoMessage.append("ERROR:  No number of clients was provided.<BR>" +
                           EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          numClients = Integer.parseInt(numClientsStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided number of " +
                             "clients as an integer.<BR>" + EOL);
          jobValid = false;
        }
      }


      int threadsPerClient = -1;
      String numThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
      if ((numThreadsStr == null) || (numThreadsStr.length() == 0))
      {
        infoMessage.append("ERROR:  No number of threads per client was " +
                           "provided.<BR>" + EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          threadsPerClient = Integer.parseInt(numThreadsStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided number of " +
                             "threads per client as an integer.<BR>" + EOL);
          jobValid = false;
        }
      }


      int threadStartupDelay = 0;
      String delayStr = request.getParameter(
                             Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
      if ((delayStr != null) && (delayStr.length() > 0))
      {
        try
        {
          threadStartupDelay = Integer.parseInt(delayStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided thread " +
                             "startup delay as an integer.<BR>" + EOL);
          jobValid = false;
        }
      }


      int collectionInterval = -1;
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr == null) || (intervalStr.length() == 0))
      {
        infoMessage.append("ERROR:  No statistics collection interval was " +
                           "provided.<BR>" + EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          collectionInterval = DurationParser.parse(intervalStr);
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" +EOL);
          jobValid = false;
        }
      }


      JobClass      jobClass               = job.getJobClass();
      ParameterList newFixedParameters     = new ParameterList();
      ParameterList newJobGroupParameters  = new ParameterList();
      LinkedHashMap<String,String> newMappedParameters =
           new LinkedHashMap<String,String>();
      ArrayList<String> removeMappedParameters = new ArrayList<String>();
      ArrayList<String> removeFixedParameters = new ArrayList<String>();

      Parameter[] stubs = jobClass.getParameterStubs().getParameters();
      if ((stubs != null) && (stubs.length > 0))
      {
        for (int i=0; i < stubs.length; i++)
        {
          if ((stubs[i] instanceof PlaceholderParameter) ||
              (stubs[i] instanceof LabelParameter))
          {
            continue;
          }

          // Determine how this parameter should be treated.
          String stubName = stubs[i].getName();
          String pName    = Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                            stubName;
          String pValue   = request.getParameter(pName);

          if (pValue == null)
          {
            // This is fine -- we'll just keep treating it like before.
          }
          else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING))
          {
            // Determine the parameter to which it should be mapped.
            String mapToName  = Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                                stubName;
            String mapToValue = request.getParameter(mapToName);

            if ((mapToValue != null) && (mapToValue.length() > 0))
            {
              removeFixedParameters.add(stubName);
              removeMappedParameters.add(stubName);
              newMappedParameters.put(stubName, mapToValue);
            }
          }
          else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW))
          {
            String mapToName =
                 Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX + stubName;
            String mapToValue = request.getParameter(mapToName);

            if ((mapToValue != null) && (mapToValue.length() > 0))
            {
              // Make sure that the new mapping doesn't conflict with an
              // existing one.
              boolean conflict = false;
              Parameter[] groupParams =
                   jobGroup.getParameters().getParameters();
              for (int j=0; j < groupParams.length; j++)
              {
                if (stubName.equals(groupParams[j].getName()))
                {
                  // There is a conflict in the parameter names, which we can
                  // work around by changing the name of the new parameter.
                  conflict = true;
                  int k = 1;
                  String newStubName = stubName + k;
                  while (conflict)
                  {
                    k++;
                    conflict = false;
                    newStubName = stubName + k;
                    for (int l=0; l < groupParams.length; l++)
                    {
                      if (newStubName.equals(groupParams[l].getName()))
                      {
                        conflict = true;
                        break;
                      }
                    }
                  }
                }
                else if (stubs[i].getDisplayName().equalsIgnoreCase(
                              groupParams[j].getDisplayName()))
                {
                  conflict = true;
                  infoMessage.append("ERROR:  Job parameter \"" +
                                     stubs[i].getDisplayName() +
                                     "\" was configured to be a new mapped " +
                                     "parameter for the job group, but its " +
                                     "display name conflicts with that of " +
                                     "another parameter already in the " +
                                     "job group.<BR>" + EOL);
                  break;
                }
              }

              if (conflict)
              {
                jobValid = false;
              }
              else
              {
                removeFixedParameters.add(stubName);
                removeMappedParameters.add(stubName);
                newMappedParameters.put(stubName, stubs[i].getName());
                newJobGroupParameters.addParameter(stubs[i]);
              }
            }
          }
          else if (pValue.equals(Constants.GROUP_PARAM_TYPE_FIXED))
          {
            try
            {
              stubs[i].htmlInputFormToValue(request);

              removeFixedParameters.add(stubName);
              removeMappedParameters.add(stubName);
              newFixedParameters.addParameter(stubs[i]);
            }
            catch (InvalidValueException ive)
            {
              infoMessage.append("ERROR:  The specified fixed value for " +
                                 "parameter \"" + stubs[i].getDisplayName() +
                                 "\" is invalid:  " + ive.getMessage() +
                                 ".<BR>" + EOL);
              jobValid = false;
            }
          }
        }
      }


      if (jobValid)
      {
        // Everything looks OK, so update the job and then write the job group
        // to the configuration database.
        job.setName(newName);
        job.setDuration(duration);
        job.setNumClients(numClients);
        job.setThreadsPerClient(threadsPerClient);
        job.setThreadStartupDelay(threadStartupDelay);
        job.setCollectionInterval(collectionInterval);

        for (int i=0; i < removeMappedParameters.size(); i++)
        {
          job.getMappedParameters().remove(removeMappedParameters.get(i));
        }

        for (int i=0; i < removeFixedParameters.size(); i++)
        {
          job.getFixedParameters().removeParameter(
               removeFixedParameters.get(i));
        }

        Iterator<String> iterator = newMappedParameters.keySet().iterator();
        while (iterator.hasNext())
        {
          String n = iterator.next();
          String v = newMappedParameters.get(n);
          job.getMappedParameters().put(n, v);
        }

        Parameter[] newFixed = newFixedParameters.getParameters();
        for (int i=0; i < newFixed.length; i++)
        {
          job.getFixedParameters().addParameter(newFixed[i]);
        }

        Parameter[] newGroup = newJobGroupParameters.getParameters();
        for (int i=0; i < newGroup.length; i++)
        {
          jobGroup.getParameters().addParameter(newGroup[i]);
        }

        try
        {
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully updated the job group in the " +
                             "configuration database.<BR>" + EOL);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Could not update the job group in the " +
                             "configuration database:  " + e + ".<BR>" + EOL);
        }

        handleViewJobGroupJob(requestInfo, jobGroup, job.getName());
        return;
      }
    }


    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Edit Job \"" + jobName + "\" in Job Group \"" +
                    jobGroupName + "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroupName,
                               Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME,
                               jobName, jobName);
    htmlBody.append("Update the following \"" + link + "\" job information." +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Fields marked with an asterisk (" + star +
                    ") are required to have a value.");
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_EDIT_GROUP_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME,
                                   jobName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    JobClass jobClass = job.getJobClass();
    if (jobClass.overrideNumClients() > 0)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                           String.valueOf(jobClass.overrideNumClients())) +
                      EOL);
    }

    if (jobClass.overrideThreadsPerClient() > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT,
                           String.valueOf(jobClass.overrideThreadsPerClient()))
                      + EOL);
    }

    if (jobClass.overrideCollectionInterval() > 0)
    {
      htmlBody.append("  " +
           generateHidden(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                secondsToHumanReadableDuration(
                     jobClass.overrideCollectionInterval())) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\"><B>General Parameters</B></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String newName = jobName;
    if (submitted)
    {
      newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
      if ((newName == null) || (newName.length() == 0))
      {
        newName = jobName;
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Job Name " + star +"</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_NEW_NAME + "\" VALUE=\"" + newName +
                    "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    String durationStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
    if ((durationStr == null) || (durationStr.length() == 0))
    {
      if (submitted)
      {
        durationStr = "";
      }
      else
      {
        if (job.getDuration() > 0)
        {
          durationStr = secondsToHumanReadableDuration(job.getDuration());
        }
        else
        {
          durationStr = "";
        }
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    durationStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideNumClients() <= 0)
    {
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((numClientsStr == null) || (numClientsStr.length() == 0))
      {
        numClientsStr = String.valueOf(job.getNumClients());
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      numClientsStr + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    if (jobClass.overrideThreadsPerClient() <= 0)
    {
      String numThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
      if ((numThreadsStr == null) || (numThreadsStr.length() == 0))
      {
        numThreadsStr = String.valueOf(job.getThreadsPerClient());
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Threads per Client " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT +
                      "\" VALUE=\"" + numThreadsStr + "\" SIZE=\"40\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if ((delayStr == null) || (delayStr.length() == 0))
    {
      if (submitted)
      {
        delayStr = "0";
      }
      else
      {
        delayStr = String.valueOf(job.getThreadStartupDelay());
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideCollectionInterval() <= 0)
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
      htmlBody.append("      <TD>Statistics Collection Interval " + star +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
    Parameter[] groupParameters = jobGroup.getParameters().getParameters();
    if ((stubs != null) && (stubs.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><B>Job-Specific " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      boolean skippedFirstSpace = false;
      for (int i=0; i < stubs.length; i++)
      {
        if ((stubs[i] instanceof PlaceholderParameter) ||
            (stubs[i] instanceof LabelParameter))
        {
          continue;
        }

        if (! skippedFirstSpace)
        {
          skippedFirstSpace = true;
        }
        else
        {
          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD COLSPAN=\"3\">" + stubs[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);

        String mapParam  = Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                           stubs[i].getName();
        String mapType   = null;
        String mapToName = stubs[i].getName();

        if (submitted)
        {
          mapType = request.getParameter(mapParam);

          try
          {
            stubs[i].htmlInputFormToValue(request);
          } catch (InvalidValueException ive) {}
        }
        if (mapType == null)
        {
          if (job.getMappedParameters().containsKey(stubs[i].getName()))
          {
            mapType = Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING;

            String mappedName =
                 job.getMappedParameters().get(stubs[i].getName());
            Parameter mappedParam =
                 jobGroup.getParameters().getParameter(mappedName);
            if (mappedParam != null)
            {
              mapToName = mappedParam.getName();
              stubs[i].setValueFrom(mappedParam);
            }
          }
          else if (job.getFixedParameters().hasParameter(stubs[i].getName()))
          {
            mapType = Constants.GROUP_PARAM_TYPE_FIXED;

            Parameter fixedParam =
                 job.getFixedParameters().getParameter(stubs[i].getName());
            if (fixedParam != null)
            {
              stubs[i].setValueFrom(fixedParam);
            }
          }
          else
          {
            mapType = Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW;
          }
        }


        htmlBody.append("    <TR>" + EOL);
        if (mapType.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING))
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          mapParam + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING +
                          "\" CHECKED>Map to Existing Job Group " +
                          "Parameter</TD>" + EOL);
        }
        else
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          mapParam + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING +
                          "\">Map to Existing Job Group Parameter</TD>" + EOL);
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" + EOL);
        htmlBody.append("        <SELECT NAME=\"" +
                        Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                        stubs[i].getName() + "\">" + EOL);
        for (int j=0; j < groupParameters.length; j++)
        {
          if ((groupParameters[j] instanceof PlaceholderParameter) ||
              (groupParameters[j] instanceof LabelParameter))
          {
            continue;
          }

          String selectedStr = "";
          if (groupParameters[j].getName().equals(mapToName))
          {
            selectedStr = " SELECTED";
          }

          htmlBody.append("          <OPTION VALUE=\"" +
                          groupParameters[j].getName() + '"' + selectedStr +
                          '>' + groupParameters[j].getDisplayName() + EOL);
        }
        htmlBody.append("        </SELECT>" + EOL);
        htmlBody.append("      </TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);


        htmlBody.append("    <TR>" + EOL);
        if (mapType.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW))
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW +
                          "\" CHECKED>Create a New Job Group Parameter</TD>" +
                          EOL);
        }
        else
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW +
                          "\">Create a New Job Group Parameter</TD>" + EOL);
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        stubs[i].getDisplayName() + "\" SIZE=\"40\"></TD>" +
                        EOL);
        htmlBody.append("    </TR>" + EOL);


        htmlBody.append("    <TR>" + EOL);
        if (mapType.equals(Constants.GROUP_PARAM_TYPE_FIXED))
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_FIXED +
                          "\" CHECKED>Always Use a Fixed Value</TD>" + EOL);
        }
        else
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_FIXED +
                          "\">Always Use a Fixed Value</TD>" + EOL);
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        stubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Update " +
                    "Job\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to editing an optimizing job in a job group.
   *
   * @param  requestInfo  The state information for this request
   */
  static void handleEditJobGroupOptimizingJob(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleEditJobGroupOptimizingJob()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "edit a job group.");
      return;
    }


    // Get the job group to edit.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if ((jobGroupName == null) || (jobGroupName.length() == 0))
    {
      infoMessage.append("ERROR:  No job group was specified for which to " +
                         "edit the job.<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }

    JobGroup jobGroup = null;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration " +
                           "database.<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Could not retrieve job group \"" +
                         jobGroupName + "\" from the configuration " +
                         "database:  " + e + ".<BR>" + EOL);
      handleViewJobGroups(requestInfo);
      return;
    }


    // Get the job group optimizing job to edit.
    String optimizingJobName =
         request.getParameter(
              Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME);
    if ((optimizingJobName == null) || (optimizingJobName.length() == 0))
    {
      infoMessage.append("ERROR:  No optimizing job was selected for " +
                         "editing in the specified job group.<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }

    JobGroupOptimizingJob optimizingJob = null;
    ArrayList jobList = jobGroup.getJobList();
    for (int i=0; i < jobList.size(); i++)
    {
      Object o = jobList.get(i);
      if (o instanceof JobGroupOptimizingJob)
      {
        JobGroupOptimizingJob currentOptimizingJob = (JobGroupOptimizingJob) o;
        if (currentOptimizingJob.getName().equals(optimizingJobName))
        {
          optimizingJob = currentOptimizingJob;
          break;
        }
      }
    }

    if (optimizingJob == null)
    {
      infoMessage.append("ERROR:  There is no job named \"" +
                         optimizingJobName + "\" in job group \"" +
                         jobGroupName + "\".<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }


    boolean submitted = false;
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      boolean jobValid = true;
      submitted = true;


      String newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
      if ((newName == null) || (newName.length() == 0))
      {
        infoMessage.append("ERROR:  The optimizing job must have a name.<BR>" +
                           EOL);
        jobValid = false;
      }
      else if (! newName.equals(optimizingJobName))
      {
        boolean conflict = false;

        for (int i=0; i < jobList.size(); i++)
        {
          Object o = jobList.get(i);
          if (o instanceof JobGroupJob)
          {
            JobGroupJob j = (JobGroupJob) o;
            if (j.getName().equals(newName))
            {
              conflict = true;
              break;
            }
          }
          else if (o instanceof JobGroupOptimizingJob)
          {
            JobGroupOptimizingJob oj = (JobGroupOptimizingJob) o;
            if (oj.getName().equals(newName))
            {
              conflict = true;
              break;
            }
          }
        }

        if (conflict)
        {
          infoMessage.append("ERROR:  The new name provided for the " +
                             "optimizing job conflicts with the name of " +
                             "another job or optimizing job in the job " +
                             "group.<BR>" + EOL);
          jobValid = false;
        }
      }


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
          jobValid = false;
        }
      }


      int numClients = -1;
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((numClientsStr == null) || (numClientsStr.length() == 0))
      {
        infoMessage.append("ERROR:  No number of clients was provided.<BR>" +
                           EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          numClients = Integer.parseInt(numClientsStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided number of " +
                             "clients as an integer.<BR>" + EOL);
          jobValid = false;
        }
      }


      int minThreads = -1;
      String minThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
      if ((minThreadsStr == null) || (minThreadsStr.length() == 0))
      {
        infoMessage.append("ERROR:  No minimum number of threads per client " +
                           "was provided.<BR>" + EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          minThreads = Integer.parseInt(minThreadsStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided minimum " +
                             "number of threads per client as an integer.<BR>" +
                             EOL);
          jobValid = false;
        }
      }


      int maxThreads = -1;
      String maxThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX);
      if ((maxThreadsStr != null) && (maxThreadsStr.length() > 0))
      {
        try
        {
          maxThreads = Integer.parseInt(maxThreadsStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided maximum " +
                             "number of threads per client as an integer.<BR>" +
                             EOL);
          jobValid = false;
        }
      }


      int threadIncrement = -1;
      String incrementStr =
           request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
      if ((incrementStr == null) || (incrementStr.length() == 0))
      {
        infoMessage.append("ERROR:  No thread increment was provided.<BR>" +
                           EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          threadIncrement = Integer.parseInt(incrementStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided thread " +
                             "increment as an integer.<BR>" + EOL);
          jobValid = false;
        }
      }


      int threadStartupDelay = 0;
      String delayStr = request.getParameter(
                             Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
      if ((delayStr != null) && (delayStr.length() > 0))
      {
        try
        {
          threadStartupDelay = Integer.parseInt(delayStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided thread " +
                             "startup delay as an integer.<BR>" + EOL);
          jobValid = false;
        }
      }


      int collectionInterval = -1;
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr == null) || (intervalStr.length() == 0))
      {
        infoMessage.append("ERROR:  No statistics collection interval was " +
                           "provided.<BR>" + EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          collectionInterval = DurationParser.parse(intervalStr);
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" +EOL);
          jobValid = false;
        }
      }


      int maxNonImproving = -1;
      String nonImprovingStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
      if ((nonImprovingStr == null) || (nonImprovingStr.length() == 0))
      {
        infoMessage.append("ERROR:  No maximum number of non-improving " +
                           "iterations was provided.<BR>" + EOL);
        jobValid = false;
      }
      else
      {
        try
        {
          maxNonImproving = Integer.parseInt(nonImprovingStr);
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Cannot parse the provided maximum " +
                             "number of non-improving iterations as an " +
                             "integer.<BR>" +EOL);
          jobValid = false;
        }
      }


      boolean reRunBestIteration = false;
      String reRunStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
      if ((reRunStr != null) && (reRunStr.length() > 0))
      {
        reRunBestIteration = (reRunStr.equalsIgnoreCase("true") ||
                              reRunStr.equalsIgnoreCase("yes") ||
                              reRunStr.equalsIgnoreCase("on") ||
                              reRunStr.equalsIgnoreCase("1"));
      }


      int reRunDuration = -1;
      String reRunDurationStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_DURATION);
      if ((reRunDurationStr != null) && (reRunDurationStr.length() > 0))
      {
        try
        {
          reRunDuration = DurationParser.parse(reRunDurationStr);
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
          jobValid = false;
        }
      }


      JobClass jobClass = optimizingJob.getJobClass();
      OptimizationAlgorithm optimizationAlgorithm =
           optimizingJob.getOptimizationAlgorithm();
      Parameter[] optimizationStubs = optimizationAlgorithm.
           getOptimizationAlgorithmParameterStubs(jobClass).
                getParameters().clone();
      ParameterList optimizationParameters = new ParameterList();
      for (int i=0; i < optimizationStubs.length; i++)
      {
        if ((optimizationStubs[i] instanceof PlaceholderParameter) ||
            (optimizationStubs[i] instanceof LabelParameter))
        {
          continue;
        }

        try
        {
          String paramName = Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                             optimizationStubs[i].getName();
          String[] values = request.getParameterValues(paramName);
          optimizationStubs[i].htmlInputFormToValue(values);
          optimizationParameters.addParameter(optimizationStubs[i]);
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The provided value(s) for optimization " +
                             "algorithm parameter \"" +
                             optimizationStubs[i].getDisplayName() +
                             "\" were invalid:  " + e.getMessage() + ".<BR>" +
                             EOL);
        }
      }


      ParameterList newFixedParameters     = new ParameterList();
      ParameterList newJobGroupParameters  = new ParameterList();
      LinkedHashMap<String,String> newMappedParameters =
           new LinkedHashMap<String,String>();
      ArrayList<String> removeMappedParameters = new ArrayList<String>();
      ArrayList<String> removeFixedParameters  = new ArrayList<String>();

      Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
      if ((stubs != null) && (stubs.length > 0))
      {
        for (int i=0; i < stubs.length; i++)
        {
          if ((stubs[i] instanceof PlaceholderParameter) ||
              (stubs[i] instanceof LabelParameter))
          {
            continue;
          }

          // Determine how this parameter should be treated.
          String stubName = stubs[i].getName();
          String pName    = Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                            stubName;
          String pValue   = request.getParameter(pName);

          if (pValue == null)
          {
            // This is fine -- we'll just keep treating it like before.
          }
          else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING))
          {
            // Determine the parameter to which it should be mapped.
            String mapToName  = Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                                stubName;
            String mapToValue = request.getParameter(mapToName);

            if ((mapToValue != null) && (mapToValue.length() > 0))
            {
              removeFixedParameters.add(stubName);
              removeMappedParameters.add(stubName);
              newMappedParameters.put(stubName, mapToValue);
            }
          }
          else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW))
          {
            String mapToName =
                 Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX + stubName;
            String mapToValue = request.getParameter(mapToName);

            if ((mapToValue != null) && (mapToValue.length() > 0))
            {
              // Make sure that the new mapping doesn't conflict with an
              // existing one.
              boolean conflict = false;
              Parameter[] groupParams =
                   jobGroup.getParameters().getParameters();
              for (int j=0; j < groupParams.length; j++)
              {
                if (stubName.equals(groupParams[j].getName()))
                {
                  // There is a conflict in the parameter names, which we can
                  // work around by changing the name of the new parameter.
                  conflict = true;
                  int k = 1;
                  String newStubName = stubName + k;
                  while (conflict)
                  {
                    k++;
                    conflict = false;
                    newStubName = stubName + k;
                    for (int l=0; l < groupParams.length; l++)
                    {
                      if (newStubName.equals(groupParams[l].getName()))
                      {
                        conflict = true;
                        break;
                      }
                    }
                  }
                }
                else if (stubs[i].getDisplayName().equalsIgnoreCase(
                              groupParams[j].getDisplayName()))
                {
                  conflict = true;
                  infoMessage.append("ERROR:  Job parameter \"" +
                                     stubs[i].getDisplayName() +
                                     "\" was configured to be a new mapped " +
                                     "parameter for the job group, but its " +
                                     "display name conflicts with that of " +
                                     "another parameter already in the " +
                                     "job group.<BR>" + EOL);
                  break;
                }
              }

              if (conflict)
              {
                jobValid = false;
              }
              else
              {
                removeFixedParameters.add(stubName);
                removeMappedParameters.add(stubName);
                newMappedParameters.put(stubName, stubs[i].getName());
                newJobGroupParameters.addParameter(stubs[i]);
              }
            }
          }
          else if (pValue.equals(Constants.GROUP_PARAM_TYPE_FIXED))
          {
            try
            {
              stubs[i].htmlInputFormToValue(request);

              removeFixedParameters.add(stubName);
              removeMappedParameters.add(stubName);
              newFixedParameters.addParameter(stubs[i]);
            }
            catch (InvalidValueException ive)
            {
              infoMessage.append("ERROR:  The specified fixed value for " +
                                 "parameter \"" + stubs[i].getDisplayName() +
                                 "\" is invalid:  " + ive.getMessage() +
                                 ".<BR>" + EOL);
              jobValid = false;
            }
          }
        }
      }


      if (jobValid)
      {
        // Everything looks OK, so update the job and then write the job group
        // to the configuration database.
        optimizingJob.setName(newName);
        optimizingJob.setDuration(duration);
        optimizingJob.setNumClients(numClients);
        optimizingJob.setMinThreads(minThreads);
        optimizingJob.setMaxThreads(maxThreads);
        optimizingJob.setThreadIncrement(threadIncrement);
        optimizingJob.setThreadStartupDelay(threadStartupDelay);
        optimizingJob.setCollectionInterval(collectionInterval);
        optimizingJob.setMaxNonImprovingIterations(maxNonImproving);
        optimizingJob.setReRunBestIteration(reRunBestIteration);
        optimizingJob.setReRunDuration(reRunDuration);
        optimizingJob.setOptimizationParameters(optimizationParameters);

        for (int i=0; i < removeMappedParameters.size(); i++)
        {
          optimizingJob.getMappedParameters().remove(
               removeMappedParameters.get(i));
        }

        for (int i=0; i < removeFixedParameters.size(); i++)
        {
          optimizingJob.getFixedParameters().removeParameter(
               removeFixedParameters.get(i));
        }

        Iterator<String> iterator = newMappedParameters.keySet().iterator();
        while (iterator.hasNext())
        {
          String n = iterator.next();
          String v = newMappedParameters.get(n);
          optimizingJob.getMappedParameters().put(n, v);
        }

        Parameter[] newFixed = newFixedParameters.getParameters();
        for (int i=0; i < newFixed.length; i++)
        {
          optimizingJob.getFixedParameters().addParameter(newFixed[i]);
        }

        Parameter[] newGroup = newJobGroupParameters.getParameters();
        for (int i=0; i < newGroup.length; i++)
        {
          jobGroup.getParameters().addParameter(newGroup[i]);
        }

        try
        {
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully updated the job group in the " +
                             "configuration database.<BR>" + EOL);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Could not update the job group in the " +
                             "configuration database:  " + e + ".<BR>" + EOL);
        }

        handleViewJobGroupOptimizingJob(requestInfo, jobGroup,
                                        optimizingJob.getName());
        return;
      }
    }


    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Edit Optimizing Job \"" + optimizingJobName +
                    "\" in Job Group \"" + jobGroupName + "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                       Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                       Constants.SERVLET_PARAM_JOB_GROUP_NAME, jobGroupName,
                       Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME,
                       optimizingJobName, optimizingJobName);
    htmlBody.append("Update the following \"" + link + "\" optimizing job " +
                    "information." + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Fields marked with an asterisk (" + star +
                    ") are required to have a value.");
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                    Constants.SERVLET_SECTION_JOB_EDIT_GROUP_OPTIMIZING_JOB) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " +
                    generateHidden(
                         Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME,
                         optimizingJobName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    JobClass jobClass = optimizingJob.getJobClass();
    if (jobClass.overrideNumClients() > 0)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                           String.valueOf(jobClass.overrideNumClients())) +
                      EOL);
    }

    if (jobClass.overrideCollectionInterval() > 0)
    {
      htmlBody.append("  " +
           generateHidden(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                secondsToHumanReadableDuration(
                     jobClass.overrideCollectionInterval())) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\"><B>General Parameters</B></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String newName = optimizingJobName;
    if (submitted)
    {
      newName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
      if ((newName == null) || (newName.length() == 0))
      {
        newName = optimizingJobName;
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Optimizing Job Name " + star +"</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_NEW_NAME + "\" VALUE=\"" + newName +
                    "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    String durationStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_DURATION);
    if ((durationStr == null) || (durationStr.length() == 0))
    {
      if (submitted)
      {
        durationStr = "";
      }
      else
      {
        if (optimizingJob.getDuration() > 0)
        {
          durationStr =
               secondsToHumanReadableDuration(optimizingJob.getDuration());
        }
        else
        {
          durationStr = "";
        }
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DURATION + "\" VALUE=\"" +
                    durationStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideNumClients() <= 0)
    {
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((numClientsStr == null) || (numClientsStr.length() == 0))
      {
        numClientsStr = String.valueOf(optimizingJob.getNumClients());
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      numClientsStr + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    String minThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
    if ((minThreadsStr == null) || (minThreadsStr.length() == 0))
    {
      minThreadsStr = String.valueOf(optimizingJob.getMinThreads());
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Minimum Number of Threads " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MIN +
                    "\" VALUE=\"" + minThreadsStr + "\" SIZE=\"40\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String maxThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX);
    if ((maxThreadsStr == null) || (maxThreadsStr.length() == 0))
    {
      if (optimizingJob.getMaxThreads() > 0)
      {
        maxThreadsStr = String.valueOf(optimizingJob.getMaxThreads());
      }
      else
      {
        maxThreadsStr = "";
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Maximum Number of Threads</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MAX +
                    "\" VALUE=\"" + maxThreadsStr + "\" SIZE=\"40\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String incrementStr =
         request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
    if ((incrementStr == null) || (incrementStr.length() == 0))
    {
      incrementStr = String.valueOf(optimizingJob.getThreadIncrement());
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Increment Between Iterations " + star +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_THREAD_INCREMENT +
                    "\" VALUE=\"" + incrementStr + "\" SIZE=\"40\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if ((delayStr == null) || (delayStr.length() == 0))
    {
      if (submitted)
      {
        delayStr = "0";
      }
      else
      {
        delayStr = String.valueOf(optimizingJob.getThreadStartupDelay());
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideCollectionInterval() <= 0)
    {
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr == null) || (intervalStr.length() == 0))
      {
        intervalStr = secondsToHumanReadableDuration(
             optimizingJob.getCollectionInterval());
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistics Collection Interval " + star +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    String nonImprovingStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
    if ((nonImprovingStr == null) || (nonImprovingStr.length() == 0))
    {
      nonImprovingStr =
           String.valueOf(optimizingJob.getMaxNonImprovingIterations());
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Max. Consecutive Non-Improving Iterations " +
                    star + "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING +
                    "\" VALUE=\"" + nonImprovingStr +
                    "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    boolean reRunBestIteration;
    if (submitted)
    {
      reRunBestIteration = false;
      String reRunStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
      if (reRunStr != null)
      {
        reRunBestIteration = (reRunStr.equalsIgnoreCase("true") ||
                              reRunStr.equalsIgnoreCase("yes") ||
                              reRunStr.equalsIgnoreCase("on") ||
                              reRunStr.equalsIgnoreCase("1"));
      }
    }
    else
    {
      reRunBestIteration = optimizingJob.reRunBestIteration();
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_BEST_ITERATION + '"' +
                    (reRunBestIteration ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    String reRunDurationStr =
         request.getParameter(Constants.SERVLET_PARAM_RERUN_DURATION);
    if ((reRunDurationStr == null) || (reRunDurationStr.length() == 0))
    {
      if (submitted)
      {
        reRunDurationStr = "";
      }
      else
      {
        if (optimizingJob.getReRunDuration() > 0)
        {
          reRunDurationStr = secondsToHumanReadableDuration(
               optimizingJob.getReRunDuration());
        }
        else
        {
          reRunDurationStr = "";
        }
      }
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Duration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_DURATION + "\" VALUE=\"" +
                    reRunDurationStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    Parameter[] optimizationStubs = optimizingJob.getOptimizationAlgorithm().
         getOptimizationAlgorithmParameterStubs(jobClass).clone().
              getParameters();
    if ((optimizationStubs != null) && (optimizationStubs.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><B>Optimization Algorithm " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      for (int i=0; i < optimizationStubs.length; i++)
      {
        if ((optimizationStubs[i] instanceof PlaceholderParameter) ||
            (optimizationStubs[i] instanceof LabelParameter))
        {
          continue;
        }

        if (submitted)
        {
          try
          {
            optimizationStubs[i].htmlInputFormToValue(request);
          } catch (InvalidValueException ive) {}
        }
        else
        {
          Parameter p =
            optimizingJob.getOptimizationParameters().getParameter(
                 optimizationStubs[i].getName());
          if (p != null)
          {
            optimizationStubs[i].setValueFrom(p);
          }
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>" + optimizationStubs[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        optimizationStubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX)
                        + "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }


    Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
    Parameter[] groupParameters = jobGroup.getParameters().getParameters();
    if ((stubs != null) && (stubs.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><B>Job-Specific " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      boolean skippedFirstSpace = false;
      for (int i=0; i < stubs.length; i++)
      {
        if ((stubs[i] instanceof PlaceholderParameter) ||
            (stubs[i] instanceof LabelParameter))
        {
          continue;
        }

        if (! skippedFirstSpace)
        {
          skippedFirstSpace = true;
        }
        else
        {
          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD COLSPAN=\"3\">" + stubs[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);

        String mapParam  = Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                           stubs[i].getName();
        String mapType   = null;
        String mapToName = stubs[i].getName();

        if (submitted)
        {
          mapType = request.getParameter(mapParam);

          try
          {
            stubs[i].htmlInputFormToValue(request);
          } catch (InvalidValueException ive) {}
        }
        if (mapType == null)
        {
          if (optimizingJob.getMappedParameters().containsKey(
                                                       stubs[i].getName()))
          {
            mapType = Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING;

            String mappedName =
                 optimizingJob.getMappedParameters().get(stubs[i].getName());
            Parameter mappedParam =
                 jobGroup.getParameters().getParameter(mappedName);
            if (mappedParam != null)
            {
              mapToName = mappedParam.getName();
              stubs[i].setValueFrom(mappedParam);
            }
          }
          else if (optimizingJob.getFixedParameters().hasParameter(
                                                           stubs[i].getName()))
          {
            mapType = Constants.GROUP_PARAM_TYPE_FIXED;

            Parameter fixedParam =
                 optimizingJob.getFixedParameters().getParameter(
                                                         stubs[i].getName());
            if (fixedParam != null)
            {
              stubs[i].setValueFrom(fixedParam);
            }
          }
          else
          {
            mapType = Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW;
          }
        }


        htmlBody.append("    <TR>" + EOL);
        if (mapType.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING))
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          mapParam + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING +
                          "\" CHECKED>Map to Existing Job Group " +
                          "Parameter</TD>" + EOL);
        }
        else
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          mapParam + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING +
                          "\">Map to Existing Job Group Parameter</TD>" + EOL);
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" + EOL);
        htmlBody.append("        <SELECT NAME=\"" +
                        Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                        stubs[i].getName() + "\">" + EOL);
        for (int j=0; j < groupParameters.length; j++)
        {
          if ((groupParameters[j] instanceof PlaceholderParameter) ||
              (groupParameters[j] instanceof LabelParameter))
          {
            continue;
          }

          String selectedStr = "";
          if (groupParameters[j].getName().equals(mapToName))
          {
            selectedStr = " SELECTED";
          }

          htmlBody.append("          <OPTION VALUE=\"" +
                          groupParameters[j].getName() + '"' + selectedStr +
                          '>' + groupParameters[j].getDisplayName() + EOL);
        }
        htmlBody.append("        </SELECT>" + EOL);
        htmlBody.append("      </TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);


        htmlBody.append("    <TR>" + EOL);
        if (mapType.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW))
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW +
                          "\" CHECKED>Create a New Job Group Parameter</TD>" +
                          EOL);
        }
        else
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW +
                          "\">Create a New Job Group Parameter</TD>" + EOL);
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        stubs[i].getDisplayName() + "\" SIZE=\"40\"></TD>" +
                        EOL);
        htmlBody.append("    </TR>" + EOL);


        htmlBody.append("    <TR>" + EOL);
        if (mapType.equals(Constants.GROUP_PARAM_TYPE_FIXED))
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_FIXED +
                          "\" CHECKED>Always Use a Fixed Value</TD>" + EOL);
        }
        else
        {
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_FIXED +
                          "\">Always Use a Fixed Value</TD>" + EOL);
        }
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        stubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Update " +
                    "Optimizing Job\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to creating a new job group in the SLAMD
   * server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleAddJobGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleAddJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule jobs to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "create a job group.");
      return;
    }


    // Get any parameters that may have been specified.
    String name = request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    String description =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_DESCRIPTION);


    // If a job group name was specified, then create the job group.
    if (name != null)
    {
      // Make sure that the specified name isn't already taken.
      JobGroup jobGroup;
      try
      {
        jobGroup = configDB.getJobGroup(name);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Unable to determine whether the job " +
                           "group already exists " + e.getMessage() + "<BR>" +
                           EOL);
        return;
      }

      if (jobGroup == null)
      {
        try
        {
          jobGroup = new JobGroup(name, description);
          configDB.writeJobGroup(jobGroup);
          handleViewJobGroup(requestInfo);
          return;
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  Unable to add the job group to the " +
                             "configuration database:  " + e.getMessage() +
                             "<BR>" + EOL);
          return;
        }
      }
      else
      {
        infoMessage.append("ERROR:  A job group already exists with the " +
                           "name \"" + name + "\".<BR>" + EOL);
      }
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Create a New Job Group</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);

    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_ADD_GROUP) +
                    EOL);

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    if (name == null)
    {
      name = "";
    }

    if (description == null)
    {
      description = "";
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD><SPAN CLASS=\"" +
                    Constants.STYLE_FORM_CAPTION +
                    "\">Job Group Name</SPAN></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_GROUP_NAME +
                    "\" VALUE=\"" + name + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD><SPAN CLASS=\"" +
                    Constants.STYLE_FORM_CAPTION +
                    "\">Job Group Description</SPAN></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_GROUP_DESCRIPTION +
                    "\" VALUE=\"" + description + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Create Job " +
                    "Group\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);

    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to adding a job to a job group.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleAddJobToGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleAddJobToJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "update a job group.");
      return;
    }


    // Get the job group to which a new job should be added.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if (jobGroupName == null)
    {
      infoMessage.append("ERROR:  No job group specified.<BR>" + EOL);
      return;
    }

    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database." +
                           "<BR>" + EOL);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Cannot retrieve job group \"" + jobGroupName +
                         "\":  " + e.getMessage() + "<BR>" + EOL);
      return;
    }

    ArrayList jobList = jobGroup.getJobList();
    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroupName, jobGroupName);


    // See if the user has specified the job type.  If not, then display a form
    // to allow the user to select it.
    JobClass jobClass = null;
    String jobClassName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if (jobClassName != null)
    {
      jobClass = slamdServer.getJobClass(jobClassName);
      if (jobClass == null)
      {
        infoMessage.append("WARNING:  Job class \"" + jobClassName +
                           "\" is not defined in the SLAMD server.");
      }
      else
      {
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
      }
    }

    if (jobClass == null)
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Add a New Job to Job Group " + jobGroupName +
                      "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Choose the type of job to add to the \"" + link +
                      "\" job group:" + EOL);
      htmlBody.append("<BR><BR>");

      htmlBody.append("<FORM METHOD=\"SUBMIT\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_ADD_JOB_TO_GROUP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroupName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <SELECT NAME=\"" + Constants.SERVLET_PARAM_JOB_CLASS +
                      "\">");

      String[] jobCategories = slamdServer.getJobClassCategories();
      Arrays.sort(jobCategories);

      for (int i=0; i < jobCategories.length; i++)
      {
        JobClass[] jobClasses = slamdServer.getJobClasses(jobCategories[i]);
        for (int j=0; ((jobClasses != null) && (j < jobClasses.length)); j++)
        {
          htmlBody.append("    <OPTION VALUE=\"" +
                          jobClasses[j].getClass().getName() + "\">" +
                          jobCategories[i] + " -- " +
                          jobClasses[j].getJobName() + EOL);
        }
      }

      htmlBody.append("  </SELECT>" + EOL);

      htmlBody.append("  <BR><BR>");
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Continue\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      return;
    }


    // See if the user submitted the form.  If so, then make sure that the
    // provided values are acceptable and add the job to the group.
    String confirmed = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    boolean submitted = ((confirmed != null) && (confirmed.length() > 0));
    if (submitted)
    {
      boolean jobValid = true;

      // Get the job name.  It is required, and it must not duplicate the name
      // of any existing job or optimizing job in the group.
      String jobName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME);
      if ((jobName == null) || (jobName.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  No job name was given.  This is a " +
                           "required parameter.<BR>" + EOL);
      }
      else
      {
        for (int i=0; i < jobList.size(); i++)
        {
          Object o = jobList.get(i);
          if (o instanceof JobGroupJob)
          {
            if (((JobGroupJob) o).getName().equalsIgnoreCase(jobName))
            {
              jobValid = false;
              infoMessage.append("ERROR:  Job name \"" + jobName +
                                 "\" is already in use by another job in the " +
                                 "job group.<BR>" + EOL);
            }
          }
          else if (o instanceof JobGroupOptimizingJob)
          {
            if (((JobGroupOptimizingJob) o).getName().equalsIgnoreCase(jobName))
            {
              jobValid = false;
              infoMessage.append("ERROR:  Job name \"" + jobName +
                                 "\" is already in use by an optimizing job " +
                                 "in the job group.<BR>" + EOL);
            }
          }
        }
      }


      // Get the duration.  It is optional.
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
          jobValid = false;
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        }
      }

      // Get the number of clients.  It is required.
      int numClients = -1;
      String clientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((clientsStr == null) || (clientsStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The number of clients was not provided.  " +
                           "This is a required parameter.<BR>" + EOL);
      }
      else
      {
        try
        {
          numClients = Integer.parseInt(clientsStr);
          if (numClients < 1)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The number of clients must be " +
                               "greater than or equal to 1.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The number of clients must be an " +
                             "integer.<BR>" + EOL);
        }
      }

      // Get the number of threads per client.  It is required.
      int threadsPerClient = -1;
      String threadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
      if ((threadsStr == null) || (threadsStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The number of threads per client was not " +
                           "provided.  This is a required parameter.<BR>" +
                           EOL);
      }
      else
      {
        try
        {
          threadsPerClient = Integer.parseInt(threadsStr);
          if (threadsPerClient < 1)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The number of threads per client " +
                               "must be greater than or equal to 1.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The number of threads per client must " +
                             "be an integer.<BR>" + EOL);
        }
      }

      // Get the thread startup delay.  It is optional.
      int threadStartupDelay = 0;
      String delayStr = request.getParameter(
                             Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
      if ((delayStr != null) && (delayStr.length() > 0))
      {
        try
        {
          threadStartupDelay = Integer.parseInt(delayStr);
          if (threadStartupDelay < 0)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The thread startup delay must be " +
                               "greater than or equal to 0.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The thread startup delay must be an " +
                             "integer.<BR>" + EOL);
        }
      }

      // Get the statistics collection interval.  It is required.
      int collectionInterval = -1;
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr == null) || (intervalStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The statistics collection interval was " +
                           "not provided.  This is a required parameter.<BR>" +
                           EOL);
      }
      else
      {
        try
        {
          collectionInterval = DurationParser.parse(intervalStr);
          if (collectionInterval <= 0)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The statistics collection interval " +
                               "must be greater than zero.<BR>" + EOL);
          }
          else if ((duration > 0) && (collectionInterval >= duration))
          {
            jobValid = false;
            infoMessage.append("ERROR:  The statistics collection interval " +
                               "must be less than the job duration.<BR>" + EOL);
          }
        }
        catch (SLAMDException se)
        {
          jobValid = false;
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        }
      }

      // Get the set of dependencies.  It is optional.
      ArrayList<String> dependencies = new ArrayList<String>();
      String[] dependencyStrs =
           request.getParameterValues(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
      if ((dependencyStrs != null) && (dependencyStrs.length > 0))
      {
        for (int i=0; i < dependencyStrs.length; i++)
        {
          boolean found = false;
          for (int j=0; j < jobList.size(); j++)
          {
            Object o = jobList.get(j);
            if (o instanceof JobGroupJob)
            {
              if (((JobGroupJob) o).getName().equals(dependencyStrs[i]))
              {
                found = true;
                break;
              }
            }
            else if (o instanceof JobGroupOptimizingJob)
            {
              if (((JobGroupOptimizingJob)
                   o).getName().equals(dependencyStrs[i]))
              {
                found = true;
                break;
              }
            }
          }

          if (found)
          {
            dependencies.add(dependencyStrs[i]);
          }
          else
          {
            jobValid = false;
            infoMessage.append("ERROR:  A dependency was defined for job \"" +
                               dependencyStrs[i] + "\" but no such job was " +
                               "found in the job group.<BR>" + EOL);
          }
        }
      }

      // Process all of the job-specific parameters.
      LinkedHashMap<String,String> mappedParameters =
           new LinkedHashMap<String,String>();
      ParameterList newMappedParameters = new ParameterList();
      ParameterList fixedParameters     = new ParameterList();
      ParameterList jobGroupParameters  = jobGroup.getParameters();

      Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
      for (int i=0; i < stubs.length; i++)
      {
        // Determine whether this parameter should be mapped to an existing
        // parameter, mapped as a new parameter, or hold a fixed value.
        String pName  = Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                        stubs[i].getName();
        String pValue = request.getParameter(pName);
        if ((pValue == null) || (pValue.length() == 0))
        {
          // This shouldn't happen, but we'll let it slide if it's not a
          // required parameter.
          if (stubs[i].isRequired())
          {
            jobValid = false;
            infoMessage.append("ERROR:  Unable to determine how job " +
                               "parameter \"" + stubs[i].getDisplayName() +
                               "\" should be handled.<BR>" + EOL);
          }
        }
        else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING))
        {
          // Determine which existing parameter it should be mapped to.
          String mapToName  = Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                              stubs[i].getName();
          String mapToValue = request.getParameter(mapToName);
          if ((mapToValue == null) || (mapToValue.length() == 0))
          {
            jobValid = false;
            infoMessage.append("ERROR:  Job parameter \"" +
                               stubs[i].getDisplayName() + " was configured " +
                               "to be mapped to a job group parameter, but " +
                               "no target parameter was specified.<BR>" + EOL);
          }
          else
          {
            Parameter p = jobGroupParameters.getParameter(mapToValue);
            if (p == null)
            {
              jobValid = false;
              infoMessage.append("ERROR:  Job parameter \"" +
                                 stubs[i].getDisplayName() + "\" was " +
                                 "configured to be mapped to job group " +
                                 "parameter \"" + mapToValue + "\" but no " +
                                 "such parameter is defined in the job " +
                                 "group.<BR>" + EOL);
            }
            else
            {
              mappedParameters.put(stubs[i].getName(), mapToValue);
            }
          }
        }
        else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW))
        {
          // Determine the display name to use for the new mapped parameter.
          String mapToName  =
               Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX +
               stubs[i].getName();
          String mapToValue = request.getParameter(mapToName);
          if ((mapToValue != null) && (mapToValue.length() > 0))
          {
            stubs[i].setDisplayName(mapToValue);
          }

          // Make sure that the new parameter doesn't conflict with an
          // existing job group parameter.
          boolean conflict = false;
          Parameter[] groupParams = jobGroupParameters.getParameters();
          String originalName = stubs[i].getName();
          for (int j=0; j < groupParams.length; j++)
          {
            if (stubs[i].getName().equals(groupParams[j].getName()))
            {
              // There is a conflict in the parameter names, which we can work
              // around by changing the name of the new parameter.
              conflict = true;
              int k = 1;
              String baseName = stubs[i].getName();
              String newName = baseName + k;
              while (conflict)
              {
                k++;
                conflict = false;
                newName = baseName + k;
                for (int l=0; l < groupParams.length; l++)
                {
                  if (newName.equals(groupParams[l].getName()))
                  {
                    conflict = true;
                    break;
                  }
                }
              }

              stubs[i].setName(newName);
            }
            else if (stubs[i].getDisplayName().equalsIgnoreCase(
                          groupParams[j].getDisplayName()))
            {
              // There is a conflict in the display names, which we can't work
              // around and will therefore propagate back to the user.
              conflict = true;
              infoMessage.append("ERROR:  Job parameter \"" +
                                 stubs[i].getDisplayName() + "\" was " +
                                 "configured to be a new mapped parameter " +
                                 "for the job group, but its display name " +
                                 "conflicts with the display name of another " +
                                 "parameter already in the job group.<BR>" +
                                 EOL);
              break;
            }
          }

          if (conflict)
          {
            jobValid = false;
          }
          else
          {
            mappedParameters.put(originalName, stubs[i].getName());
            newMappedParameters.addParameter(stubs[i]);
          }
        }
        else if (pValue.equals(Constants.GROUP_PARAM_TYPE_FIXED))
        {
          // Determine the fixed value to use for the parameter.
          try
          {
            stubs[i].htmlInputFormToValue(request);
            fixedParameters.addParameter(stubs[i]);
          }
          catch (InvalidValueException ive)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The specified fixed value for " +
                               "parameter \"" + stubs[i].getDisplayName() +
                               "\" is invalid:  " + ive.getMessage() + "<BR>" +
                               EOL);
          }
        }
        else
        {
          jobValid = false;
          infoMessage.append("ERROR:  Unable to determine how job " +
                             "parameter \"" + stubs[i].getDisplayName() +
                             "\" should be handled.<BR>" + EOL);
        }
      }


      // If everything looks good, then create the job and add it to the group.
      // Also, if there are any new mapped parameters, then add them to the job
      // group.
      if (jobValid)
      {
        JobGroupJob job = new JobGroupJob(jobGroup, jobName, jobClass, duration,
                                          collectionInterval, numClients,
                                          threadsPerClient, threadStartupDelay,
                                          dependencies, mappedParameters,
                                          fixedParameters);
        jobGroup.getJobList().add(job);

        Parameter[] newMapped = newMappedParameters.getParameters();
        for (int i=0; i < newMapped.length; i++)
        {
          jobGroupParameters.addParameter(newMapped[i]);
        }

        // Write the updated job group to the configuration database.
        try
        {
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully added job \"" + jobName +
                             "\" to the job group.<BR>" + EOL);
          handleViewJobGroup(requestInfo);
          return;
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          infoMessage.append("ERROR:  Could not update the job group in the " +
                             "configuration database:  " + e.getMessage() +
                             ".<BR>" + EOL);
        }
      }
    }

    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Add a New \"" + jobClass.getJobName() +
                    "\" Job to Job Group \"" + jobGroupName + "\"</SPAN>" +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("Enter the following information about the " +
                    jobClass.getJobName() + " job to add to the \"" +
                    link + "\" job group." + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("For the job-specific parameters, you may either " +
                    "specify hard-coded values or allow the user to " +
                    "specify the value as part of the job group parameters." +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Fields marked with an asterisk (" + star +
                    ") are required to have a value.");
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_ADD_JOB_TO_GROUP) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                          jobClass.getClass().getName()) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    if (jobClass.overrideNumClients() > 0)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                           String.valueOf(jobClass.overrideNumClients())) +
                      EOL);
    }

    if (jobClass.overrideThreadsPerClient() > 0)
    {
      htmlBody.append("  " +
                      generateHidden(
                           Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT,
                           String.valueOf(jobClass.overrideThreadsPerClient()))
                      + EOL);
    }

    if (jobClass.overrideCollectionInterval() > 0)
    {
      htmlBody.append("  " +
           generateHidden(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                secondsToHumanReadableDuration(
                     jobClass.overrideCollectionInterval())) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\"><B>General Parameters</B></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String jobName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME);
    if (jobName == null)
    {
      jobName = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Job Name " + star +"</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME + "\" VALUE=\"" +
                    jobName + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

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

    if (jobClass.overrideNumClients() <= 0)
    {
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if (numClientsStr == null)
      {
        numClientsStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      numClientsStr + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    if (jobClass.overrideThreadsPerClient() <= 0)
    {
      String numThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT);
      if (numThreadsStr == null)
      {
        numThreadsStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Threads per Client " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT +
                      "\" VALUE=\"" + numThreadsStr + "\" SIZE=\"40\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if (delayStr == null)
    {
      delayStr = "0";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideCollectionInterval() <= 0)
    {
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if (intervalStr == null)
      {
        intervalStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistics Collection Interval " + star +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    if (! jobList.isEmpty())
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Job Dependencies</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);

      for (int i=0; i < jobList.size(); i++)
      {
        String depName;
        Object o = jobList.get(i);
        if (o instanceof JobGroupJob)
        {
          depName = ((JobGroupJob) o).getName();
        }
        else if (o instanceof JobGroupOptimizingJob)
        {
          depName = ((JobGroupOptimizingJob) o).getName();
        }
        else
        {
          continue;
        }

        String checkedStr = "";
        String[] depValues =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
        for (int j=0; ((depValues != null) && (j < depValues.length)); j++)
        {
          if (depValues[j].equals(depName))
          {
            checkedStr = " CHECKED";
            break;
          }
        }

        if ((! submitted) && (i == (jobList.size() - 1)))
        {
          checkedStr = " CHECKED";
        }

        htmlBody.append("        <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                        Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\" VALUE=\"" +
                        depName + '"' + checkedStr + '>' + depName + "<BR>" +
                        EOL);
      }

      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
    Parameter[] groupParameters = jobGroup.getParameters().getParameters();
    if ((stubs != null) && (stubs.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><B>Job-Specific " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      boolean skippedFirstSpace = false;
      for (int i=0; i < stubs.length; i++)
      {
        if ((stubs[i] instanceof PlaceholderParameter) ||
            (stubs[i] instanceof LabelParameter))
        {
          continue;
        }

        if (! skippedFirstSpace)
        {
          skippedFirstSpace = true;
        }
        else
        {
          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD COLSPAN=\"3\">" + stubs[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);

        if (submitted)
        {
          try
          {
            stubs[i].htmlInputFormToValue(request);
          } catch (InvalidValueException ive) {}
        }

        boolean selected = false;
        if (groupParameters.length > 0)
        {
          int selectedIndex = -1;
          for (int j=0; ((! selected) && (j < groupParameters.length)); j++)
          {
            if ((groupParameters[j] instanceof PlaceholderParameter) ||
                (groupParameters[j] instanceof LabelParameter))
            {
              continue;
            }

            if (stubs[i].getName().equalsIgnoreCase(
                     groupParameters[j].getName()) ||
                stubs[i].getDisplayName().equalsIgnoreCase(
                     groupParameters[j].getDisplayName()))
            {
              selected = true;
              selectedIndex = j;
            }
          }

          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING + '"');
          if (selected)
          {
            htmlBody.append(" CHECKED");
          }
          htmlBody.append(">Map to Existing Job Group Parameter</TD>" + EOL);

          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD>" + EOL);
          htmlBody.append("        <SELECT NAME=\"" +
                          Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                          stubs[i].getName() + "\">" + EOL);
          for (int j=0; j < groupParameters.length; j++)
          {
            if ((groupParameters[j] instanceof PlaceholderParameter) ||
                (groupParameters[j] instanceof LabelParameter))
            {
              continue;
            }

            htmlBody.append("          <OPTION VALUE=\"" +
                            groupParameters[j].getName());
            if (j == selectedIndex)
            {
              htmlBody.append("\" SELECTED>");
            }
            else
            {
              htmlBody.append("\">");
            }

            String displayName = groupParameters[j].getDisplayName();
            if (displayName == null)
            {
              displayName = groupParameters[j].getName();
            }

            htmlBody.append(displayName + EOL);
          }
          htmlBody.append("        </SELECT>" + EOL);
          htmlBody.append("      </TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                        Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW + '"');
        if (! selected)
        {
          htmlBody.append(" CHECKED");
          selected = true;
        }
        htmlBody.append(">Create a New Job Group Parameter</TD>" + EOL);

        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        stubs[i].getDisplayName() + "\" SIZE=\"40\"></TD>" +
                        EOL);
        htmlBody.append("    </TR>" + EOL);

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                        Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        Constants.GROUP_PARAM_TYPE_FIXED + "\">Always Use a " +
                        "Fixed Value</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        stubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Add Job to Job " +
                    "Group\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to adding an optimizing job to a job group.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleAddOptimizingJobToGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleAddOptimizingJobToJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "update a job group.");
      return;
    }


    // Get the job group to which a new job should be added.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if (jobGroupName == null)
    {
      infoMessage.append("ERROR:  No job group specified.<BR>" + EOL);
      return;
    }

    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database." +
                           "<BR>" + EOL);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Cannot retrieve job group \"" + jobGroupName +
                         "\":  " + e.getMessage() + "<BR>" + EOL);
      return;
    }

    ArrayList jobList = jobGroup.getJobList();
    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroupName, jobGroupName);


    // See if the user has specified the job type.  If not, then display a form
    // to allow the user to select it.
    JobClass jobClass = null;
    String jobClassName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if (jobClassName != null)
    {
      jobClass = slamdServer.getJobClass(jobClassName);
      if (jobClass == null)
      {
        infoMessage.append("WARNING:  Job class \"" + jobClassName +
                           "\" is not defined in the SLAMD server.");
      }
      else
      {
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
      }
    }

    if (jobClass == null)
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Add a New Optimizing Job to Job Group " +
                      jobGroupName + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Choose the type of optimizing job to add to the \"" +
                      link + "\" job group:" + EOL);
      htmlBody.append("<BR><BR>");

      htmlBody.append("<FORM METHOD=\"SUBMIT\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
           generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                Constants.SERVLET_SECTION_JOB_ADD_OPTIMIZING_JOB_TO_GROUP) +
                EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroupName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <SELECT NAME=\"" + Constants.SERVLET_PARAM_JOB_CLASS +
                      "\">");

      String[] jobCategories = slamdServer.getJobClassCategories();
      Arrays.sort(jobCategories);

      for (int i=0; i < jobCategories.length; i++)
      {
        JobClass[] jobClasses = slamdServer.getJobClasses(jobCategories[i]);
        for (int j=0; ((jobClasses != null) && (j < jobClasses.length)); j++)
        {
          if (jobClasses[j].overrideThreadsPerClient() < 0)
          {
            htmlBody.append("    <OPTION VALUE=\"" +
                            jobClasses[j].getClass().getName() + "\">" +
                            jobCategories[i] + " -- " +
                            jobClasses[j].getJobName() + EOL);
          }
        }
      }

      htmlBody.append("  </SELECT>" + EOL);

      htmlBody.append("  <BR><BR>");
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Continue\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      return;
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
        handleViewJobGroup(requestInfo);
        return;
      }
      else if (availableAlgorithmList.size() == 1)
      {
        optimizationAlgorithm = availableAlgorithmList.get(0);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Add an Optimizing Job to Job Group " +
                        jobGroupName + "</SPAN>" + EOL);
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
                        generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                       jobGroup.getName()) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                       jobClass.getClass().getName()) + EOL);

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
        handleViewJobGroup(requestInfo);
        return;
      }
    }


    // See if the user submitted the form.  If so, then make sure that the
    // provided values are acceptable and add the job to the group.
    String confirmed = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    boolean submitted = ((confirmed != null) && (confirmed.length() > 0));
    if (submitted)
    {
      boolean jobValid = true;

      // Get the optimizing job name.  It is required, and it must not duplicate
      // the name of any existing job or optimizing job in the group.
      String jobName =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME);
      if ((jobName == null) || (jobName.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  No optimizing job name was given.  This " +
                           "is a required parameter.<BR>" + EOL);
      }
      else
      {
        for (int i=0; i < jobList.size(); i++)
        {
          Object o = jobList.get(i);
          if (o instanceof JobGroupJob)
          {
            if (((JobGroupJob) o).getName().equalsIgnoreCase(jobName))
            {
              jobValid = false;
              infoMessage.append("ERROR:  Job name \"" + jobName +
                                 "\" is already in use by a job in the job " +
                                 "group.<BR>" + EOL);
            }
          }
          else if (o instanceof JobGroupOptimizingJob)
          {
            if (((JobGroupOptimizingJob) o).getName().equalsIgnoreCase(jobName))
            {
              jobValid = false;
              infoMessage.append("ERROR:  Job name \"" + jobName +
                                 "\" is already in use by another optimizing " +
                                 "job in the job group.<BR>" + EOL);
            }
          }
        }
      }


      // Get the duration.  It is optional.
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
          jobValid = false;
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        }
      }

      // Get the delay between iterations.  It is optional.
      int delayBetweenIterations = 0;
      String delayStr =
           request.getParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
      if ((delayStr != null) && (delayStr.length() > 0))
      {
        try
        {
          delayBetweenIterations = Integer.parseInt(delayStr);
          if (delayBetweenIterations < 0)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The delay between iterations must " +
                               "be greater than or equal to 0.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The delay between iterations must be " +
                             "an integer.<BR>" + EOL);
        }
      }

      // Get the number of clients.  It is required.
      int numClients = -1;
      String clientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((clientsStr == null) || (clientsStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The number of clients was not provided.  " +
                           "This is a required parameter.<BR>" + EOL);
      }
      else
      {
        try
        {
          numClients = Integer.parseInt(clientsStr);
          if (numClients < 1)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The number of clients must be " +
                               "greater than or equal to 1.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The number of clients must be an " +
                             "integer.<BR>" + EOL);
        }
      }

      // Get the minimum number of threads per client.  It is required.
      int minThreads = 1;
      String minThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
      if ((minThreadsStr == null) || (minThreadsStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The minimum number of threads per client " +
                           "was not provided.  This is a required parameter." +
                           "<BR>" + EOL);
      }
      else
      {
        try
        {
          minThreads = Integer.parseInt(minThreadsStr);
          if (minThreads < 1)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The minimum number of threads per " +
                               "client must be greater than or equal to " +
                               "1.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The minimum number of threads per " +
                             "client must be an integer.<BR>" + EOL);
        }
      }

      // Get the maximum number of threads per client.  It is optional.
      int maxThreads = -1;
      String maxThreadsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX);
      if ((maxThreadsStr != null) && (maxThreadsStr.length() > 0))
      {
        try
        {
          maxThreads = Integer.parseInt(maxThreadsStr);
          if ((maxThreads > 0) && (maxThreads < minThreads))
          {
            jobValid = false;
            infoMessage.append("ERROR:  The maximum number of threads per " +
                               "client must be greater than or equal to the " +
                               "minimum number of threads per client.<BR>" +
                               EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The maximum number of threads per " +
                             "client must be an integer.<BR>" + EOL);
        }
      }

      // Get the thread increment between iterations.  It is required.
      int threadIncrement = 1;
      String incrementStr =
           request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
      if ((incrementStr == null) || (incrementStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The thread increment between iterations " +
                           "was not provided.  It is a required " +
                           "parameter.<BR>" + EOL);
      }
      else
      {
        try
        {
          threadIncrement = Integer.parseInt(incrementStr);
          if (threadIncrement < 1)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The thread increment between " +
                               "iterations must be greater than or equal to " +
                               "1.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The thread increment between " +
                             "iterations must be an integer.<BR>" + EOL);
        }
      }

      // Get the thread startup delay.  It is optional.
      int threadStartupDelay = 0;
      delayStr = request.getParameter(
                      Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
      if ((delayStr != null) && (delayStr.length() > 0))
      {
        try
        {
          threadStartupDelay = Integer.parseInt(delayStr);
          if (threadStartupDelay < 0)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The thread startup delay must be " +
                               "greater than or equal to 0.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The thread startup delay must be an " +
                             "integer.<BR>" + EOL);
        }
      }

      // Get the statistics collection interval.  It is required.
      int collectionInterval = -1;
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if ((intervalStr == null) || (intervalStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The statistics collection interval was " +
                           "not provided.  This is a required parameter.<BR>" +
                           EOL);
      }
      else
      {
        try
        {
          collectionInterval = DurationParser.parse(intervalStr);
          if (collectionInterval <= 0)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The statistics collection interval " +
                               "must be greater than zero.<BR>" + EOL);
          }
          else if ((duration > 0) && (collectionInterval >= duration))
          {
            jobValid = false;
            infoMessage.append("ERROR:  The statistics collection interval " +
                               "must be less than the job duration.<BR>" + EOL);
          }
        }
        catch (SLAMDException se)
        {
          jobValid = false;
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        }
      }

      // Get the maximum number of non-improving iterations.  It is required.
      int maxNonImproving = 1;
      String maxNonImprovingStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
      if ((maxNonImprovingStr == null) || (maxNonImprovingStr.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  The maximum number of consecutive " +
                           "non-improving iterations was not provided.  This " +
                           "a required parameter.<BR>" + EOL);
      }
      else
      {
        try
        {
          maxNonImproving = Integer.parseInt(maxNonImprovingStr);
          if (maxNonImproving < 1)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The maximum number of consecutive " +
                               "non-improving iterations must be greater " +
                               "than or equal to 1.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The maximum number of consecutive " +
                             "non-improving iterations must be an " +
                             "integer.<BR>" + EOL);
        }
      }

      // Get the flag indicating whether to re-run the best iteration.
      boolean reRunBestIteration = false;
      String reRunStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
      if (reRunStr != null)
      {
        reRunBestIteration = (reRunStr.equalsIgnoreCase("true") ||
                              reRunStr.equalsIgnoreCase("yes") ||
                              reRunStr.equalsIgnoreCase("on") ||
                              reRunStr.equalsIgnoreCase("1"));
      }

      // Get the re-run duration.
      int reRunDuration = -1;
      String reRunDurationStr =
           request.getParameter(Constants.SERVLET_PARAM_RERUN_DURATION);
      if ((reRunDurationStr != null) && (reRunDurationStr.length() > 0))
      {
        try
        {
          reRunDuration = DurationParser.parse(reRunDurationStr);
        }
        catch (SLAMDException se)
        {
          jobValid = false;
          infoMessage.append("ERROR:  " + se.getMessage() + "<BR>" + EOL);
        }
      }

      // Get the set of dependencies.  It is optional.
      ArrayList<String> dependencies = new ArrayList<String>();
      String[] dependencyStrs =
           request.getParameterValues(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
      if ((dependencyStrs != null) && (dependencyStrs.length > 0))
      {
        for (int i=0; i < dependencyStrs.length; i++)
        {
          boolean found = false;
          for (int j=0; j < jobList.size(); j++)
          {
            Object o = jobList.get(j);
            if (o instanceof JobGroupJob)
            {
              if (((JobGroupJob) o).getName().equals(dependencyStrs[i]))
              {
                found = true;
                break;
              }
            }
            else if (o instanceof JobGroupOptimizingJob)
            {
              if (((JobGroupOptimizingJob)
                   o).getName().equals(dependencyStrs[i]))
              {
                found = true;
                break;
              }
            }
          }

          if (found)
          {
            dependencies.add(dependencyStrs[i]);
          }
          else
          {
            jobValid = false;
            infoMessage.append("ERROR:  A dependency was defined for " +
                               "optimizing job \"" + dependencyStrs[i] +
                               "\" but no such job or optimizing job was " +
                               "found in the job group.<BR>" + EOL);
          }
        }
      }

      // Process all of the optimization algorithm parameters.
      Parameter[] optimizationStubs =
           optimizationAlgorithm.getOptimizationAlgorithmParameterStubs(
                 jobClass).clone().getParameters();
      ParameterList optimizationParameters = new ParameterList();
      for (int i=0; i < optimizationStubs.length; i++)
      {
        if ((optimizationStubs[i] instanceof PlaceholderParameter) ||
            (optimizationStubs[i] instanceof LabelParameter))
        {
          continue;
        }

        try
        {
          String paramName = Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                             optimizationStubs[i].getName();
          String[] values = request.getParameterValues(paramName);
          optimizationStubs[i].htmlInputFormToValue(values);
          optimizationParameters.addParameter(optimizationStubs[i]);
        }
        catch (Exception e)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The provided value(s) for optimization " +
                             "algorithm parameter \"" +
                             optimizationStubs[i].getDisplayName() +
                             "\" were invalid:  " + e.getMessage() + ".<BR>" +
                             EOL);
        }
      }


      // Process all of the job-specific parameters.
      LinkedHashMap<String,String> mappedParameters =
           new LinkedHashMap<String,String>();
      ParameterList newMappedParameters = new ParameterList();
      ParameterList fixedParameters     = new ParameterList();
      ParameterList jobGroupParameters  = jobGroup.getParameters();
      Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
      for (int i=0; i < stubs.length; i++)
      {
        // Determine whether this parameter should be mapped to an existing
        // parameter, mapped as a new parameter, or hold a fixed value.
        String pName  = Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                        stubs[i].getName();
        String pValue = request.getParameter(pName);
        if ((pValue == null) || (pValue.length() == 0))
        {
          // This shouldn't happen, but we'll let it slide if it's not a
          // required parameter.
          if (stubs[i].isRequired())
          {
            jobValid = false;
            infoMessage.append("ERROR:  Unable to determine how optimizing " +
                               "job parameter \"" + stubs[i].getDisplayName() +
                               "\" should be handled.<BR>" + EOL);
          }
        }
        else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING))
        {
          // Determine which existing parameter it should be mapped to.
          String mapToName  = Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                              stubs[i].getName();
          String mapToValue = request.getParameter(mapToName);
          if ((mapToValue == null) || (mapToValue.length() == 0))
          {
            jobValid = false;
            infoMessage.append("ERROR:  Optimizing job ob parameter \"" +
                               stubs[i].getDisplayName() + " was configured " +
                               "to be mapped to a job group parameter, but " +
                               "no target parameter was specified.<BR>" + EOL);
          }
          else
          {
            Parameter p = jobGroupParameters.getParameter(mapToValue);
            if (p == null)
            {
              jobValid = false;
              infoMessage.append("ERROR:  Optimizing job parameter \"" +
                                 stubs[i].getDisplayName() + "\" was " +
                                 "configured to be mapped to job group " +
                                 "parameter \"" + mapToValue + "\" but no " +
                                 "such parameter is defined in the job " +
                                 "group.<BR>" + EOL);
            }
            else
            {
              mappedParameters.put(stubs[i].getName(), mapToValue);
            }
          }
        }
        else if (pValue.equals(Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW))
        {
          // Determine the display name to use for the new mapped parameter.
          String mapToName  =
               Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX +
               stubs[i].getName();
          String mapToValue = request.getParameter(mapToName);
          if ((mapToValue != null) && (mapToValue.length() > 0))
          {
            stubs[i].setDisplayName(mapToValue);
          }

          // Make sure that the new parameter doesn't conflict with an
          // existing job group parameter.
          boolean conflict = false;
          String originalName = stubs[i].getName();
          Parameter[] groupParams = jobGroupParameters.getParameters();
          for (int j=0; j < groupParams.length; j++)
          {
            if (stubs[i].getName().equals(groupParams[j].getName()))
            {
              // There is a conflict in the parameter names, which we can work
              // around by changing the name of the new parameter.
              conflict = true;
              int k = 1;
              String baseName = stubs[i].getName();
              String newName = baseName + k;
              while (conflict)
              {
                k++;
                conflict = false;
                newName = baseName + k;
                for (int l=0; l < groupParams.length; l++)
                {
                  if (newName.equals(groupParams[l].getName()))
                  {
                    conflict = true;
                    break;
                  }
                }
              }

              stubs[i].setName(newName);
            }
            else if (stubs[i].getDisplayName().equalsIgnoreCase(
                          groupParams[j].getDisplayName()))
            {
              conflict = true;
              infoMessage.append("ERROR:  Optimizing job parameter \"" +
                                 stubs[i].getDisplayName() + "\" was " +
                                 "configured to be a new mapped parameter " +
                                 "for the job group, but its display name " +
                                 "conflicts with the display name of another " +
                                 "parameter already in the job group.<BR>" +
                                 EOL);
              break;
            }
          }

          if (conflict)
          {
            jobValid = false;
          }
          else
          {
            mappedParameters.put(originalName, stubs[i].getName());
            newMappedParameters.addParameter(stubs[i]);
          }
        }
        else if (pValue.equals(Constants.GROUP_PARAM_TYPE_FIXED))
        {
          // Determine the fixed value to use for the parameter.
          try
          {
            stubs[i].htmlInputFormToValue(request);
            fixedParameters.addParameter(stubs[i]);
          }
          catch (InvalidValueException ive)
          {
            jobValid = false;
            infoMessage.append("ERROR:  The specified fixed value for " +
                               "parameter \"" + stubs[i].getDisplayName() +
                               "\" is invalid:  " + ive.getMessage() + "<BR>" +
                               EOL);
          }
        }
        else
        {
          jobValid = false;
          infoMessage.append("ERROR:  Unable to determine how optimizing job " +
                             "parameter \"" + stubs[i].getDisplayName() +
                             "\" should be handled.<BR>" + EOL);
        }
      }


      // If everything looks good, then create the job and add it to the group.
      // Also, if there are any new mapped parameters, then add them to the job
      // group.
      if (jobValid)
      {
        JobGroupOptimizingJob optimizingJob =
             new JobGroupOptimizingJob(jobName, jobGroup, jobClass, duration,
                                       delayBetweenIterations, numClients,
                                       minThreads, maxThreads, threadIncrement,
                                       collectionInterval, maxNonImproving,
                                       threadStartupDelay, reRunBestIteration,
                                       reRunDuration, dependencies,
                                       optimizationAlgorithm,
                                       optimizationParameters, mappedParameters,
                                       fixedParameters);
        jobGroup.getJobList().add(optimizingJob);

        Parameter[] newMapped = newMappedParameters.getParameters();
        for (int i=0; i < newMapped.length; i++)
        {
          jobGroupParameters.addParameter(newMapped[i]);
        }

        // Write the updated job group to the configuration database.
        try
        {
          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully added optimizing job \"" + jobName +
                             "\" to the job group.<BR>" + EOL);
          handleViewJobGroup(requestInfo);
          return;
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          infoMessage.append("ERROR:  Could not update the job group in the " +
                             "configuration database:  " + e.getMessage() +
                             ".<BR>" + EOL);
        }
      }
    }

    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Add a New Optimizing \"" + jobClass.getJobName() +
                    "\" Job to Job Group \"" + jobGroupName + "\"</SPAN>" +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("Enter the following information about the optimizing " +
                    jobClass.getJobName() + " job to add to the \"" + link +
                    "\" job group." + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The optimization parameters must be given hard-coded " +
                    "values." + EOL);
    htmlBody.append("For the job-specific parameters, you may either " +
                    "specify hard-coded values or allow the user to " +
                    "specify the value as part of the job group parameters." +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Fields marked with an asterisk (" + star +
                    ") are required to have a value.");
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
         generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
              Constants.SERVLET_SECTION_JOB_ADD_OPTIMIZING_JOB_TO_GROUP) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                          jobClass.getClass().getName()) + EOL);
    htmlBody.append("  " +
         generateHidden(Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                        optimizationAlgorithm.getClass().getName()) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    if (jobClass.overrideNumClients() > 0)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                           String.valueOf(jobClass.overrideNumClients())) +
                      EOL);
    }

    if (jobClass.overrideCollectionInterval() > 0)
    {
      htmlBody.append("  " +
           generateHidden(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                secondsToHumanReadableDuration(
                     jobClass.overrideCollectionInterval())) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\"><B>General Parameters</B></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String jobName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME);
    if (jobName == null)
    {
      jobName = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Job Name " + star +"</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME +
                    "\" VALUE=\"" + jobName + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

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

    String delayStr =
         request.getParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS);
    if (delayStr == null)
    {
      delayStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Delay Between Iterations</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideNumClients() <= 0)
    {
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if (numClientsStr == null)
      {
        numClientsStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Number of Clients " + star + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_NUM_CLIENTS + "\" VALUE=\"" +
                      numClientsStr + "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    String minThreadsStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN);
    if (minThreadsStr == null)
    {
      minThreadsStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Minimum Number of Threads " + star + "</TD>" +
                    EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREADS_MIN + "\" VALUE=\"" +
                    minThreadsStr + "\" SIZE=\"40\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

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
                    maxThreadsStr + "\" SIZE=\"40\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    String incrementStr =
         request.getParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT);
    if (incrementStr == null)
    {
      incrementStr = "";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Increment Between Iterations " + star +
                    "</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_THREAD_INCREMENT + "\" VALUE=\"" +
                    incrementStr + "\" SIZE=\"40\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);

    delayStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY);
    if (delayStr == null)
    {
      delayStr = "0";
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Thread Startup Delay (ms)</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY +
                    "\" VALUE=\"" + delayStr + "\" SIZE=\"40\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    if (jobClass.overrideCollectionInterval() <= 0)
    {
      String intervalStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL);
      if (intervalStr == null)
      {
        intervalStr = "";
      }
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistics Collection Interval " + star +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL +
                      "\" VALUE=\"" + intervalStr + "\" SIZE=\"40\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    String maxNonImprovingStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING);
    if (maxNonImprovingStr == null)
    {
      maxNonImprovingStr = "";
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

    boolean reRunBestIteration = false;
    String reRunStr =
         request.getParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION);
    if (reRunStr != null)
    {
      reRunBestIteration = (reRunStr.equalsIgnoreCase("true") ||
                            reRunStr.equalsIgnoreCase("yes") ||
                            reRunStr.equalsIgnoreCase("on") ||
                            reRunStr.equalsIgnoreCase("1"));
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Re-Run Best Iteration</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_RERUN_BEST_ITERATION + '"' +
                    (reRunBestIteration ? " CHECKED" : "") + "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

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

    if (! jobList.isEmpty())
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Job Dependencies</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);

      for (int i=0; i < jobList.size(); i++)
      {
        String depName;
        Object o = jobList.get(i);
        if (o instanceof JobGroupJob)
        {
          depName = ((JobGroupJob) o).getName();
        }
        else if (o instanceof JobGroupOptimizingJob)
        {
          depName = ((JobGroupOptimizingJob) o).getName();
        }
        else
        {
          continue;
        }

        String checkedStr = "";
        String[] depValues =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
        for (int j=0; ((depValues != null) && (j < depValues.length)); j++)
        {
          if (depValues[j].equals(depName))
          {
            checkedStr = " CHECKED";
            break;
          }
        }

        if ((! submitted) && (i == (jobList.size() - 1)))
        {
          checkedStr = " CHECKED";
        }

        htmlBody.append("        <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                        Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\" VALUE=\"" +
                        depName + '"' + checkedStr + '>' + depName + "<BR>" +
                        EOL);
      }

      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    Parameter[] optimizationStubs = optimizationAlgorithm.
         getOptimizationAlgorithmParameterStubs(jobClass).clone().
              getParameters();
    if ((optimizationStubs != null) && (optimizationStubs.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><B>Optimization Algorithm " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      for (int i=0; i < optimizationStubs.length; i++)
      {
        if ((optimizationStubs[i] instanceof PlaceholderParameter) ||
            (optimizationStubs[i] instanceof LabelParameter))
        {
          continue;
        }

        if (submitted)
        {
          try
          {
            optimizationStubs[i].htmlInputFormToValue(request);
          } catch (InvalidValueException ive) {}
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>" + optimizationStubs[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        optimizationStubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX)
                        + "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }

    Parameter[] stubs = jobClass.getParameterStubs().clone().getParameters();
    Parameter[] groupParameters = jobGroup.getParameters().getParameters();
    if ((stubs != null) && (stubs.length > 0))
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><B>Job-Specific " +
                      "Parameters</B></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      boolean skippedFirstSpace = false;
      for (int i=0; i < stubs.length; i++)
      {
        if ((stubs[i] instanceof PlaceholderParameter) ||
            (stubs[i] instanceof LabelParameter))
        {
          continue;
        }

        if (! skippedFirstSpace)
        {
          skippedFirstSpace = true;
        }
        else
        {
          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD COLSPAN=\"3\">" + stubs[i].getDisplayName() +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);

        if (submitted)
        {
          try
          {
            stubs[i].htmlInputFormToValue(request);
          } catch (InvalidValueException ive) {}
        }

        boolean selected = false;
        if (groupParameters.length > 0)
        {
          int selectedIndex = -1;
          for (int j=0; ((! selected) && (j < groupParameters.length)); j++)
          {
            if ((groupParameters[j] instanceof PlaceholderParameter) ||
                (groupParameters[j] instanceof LabelParameter))
            {
              continue;
            }

            if (stubs[i].getName().equalsIgnoreCase(
                     groupParameters[j].getName()) ||
                stubs[i].getDisplayName().equalsIgnoreCase(
                     groupParameters[j].getDisplayName()))
            {
              selected = true;
              selectedIndex = j;
            }
          }

          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                          Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                          stubs[i].getName() + "\" VALUE=\"" +
                          Constants.GROUP_PARAM_TYPE_MAPPED_TO_EXISTING + '"');
          if (selected)
          {
            htmlBody.append(" CHECKED");
          }
          htmlBody.append(">Map to Existing Job Group Parameter</TD>" + EOL);

          htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("      <TD>" + EOL);
          htmlBody.append("        <SELECT NAME=\"" +
                          Constants.SERVLET_PARAM_MAP_TO_NAME_PREFIX +
                          stubs[i].getName() + "\">" + EOL);
          for (int j=0; j < groupParameters.length; j++)
          {
            if ((groupParameters[j] instanceof PlaceholderParameter) ||
                (groupParameters[j] instanceof LabelParameter))
            {
              continue;
            }

            htmlBody.append("          <OPTION VALUE=\"" +
                            groupParameters[j].getName());
            if (j == selectedIndex)
            {
              htmlBody.append("\" SELECTED>");
            }
            else
            {
              htmlBody.append("\">");
            }

            String displayName = groupParameters[j].getDisplayName();
            if (displayName == null)
            {
              displayName = groupParameters[j].getName();
            }

            htmlBody.append(displayName + EOL);
          }
          htmlBody.append("        </SELECT>" + EOL);
          htmlBody.append("      </TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                        Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        Constants.GROUP_PARAM_TYPE_MAPPED_TO_NEW + '"');
        if (! selected)
        {
          htmlBody.append(" CHECKED");
          selected = true;
        }
        htmlBody.append(">Create a New Job Group Parameter</TD>" + EOL);

        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        stubs[i].getDisplayName() + "\" SIZE=\"40\"></TD>" +
                        EOL);
        htmlBody.append("    </TR>" + EOL);

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"RADIO\" NAME=\"" +
                        Constants.SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX +
                        stubs[i].getName() + "\" VALUE=\"" +
                        Constants.GROUP_PARAM_TYPE_FIXED + "\">Always Use a " +
                        "Fixed Value</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        stubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Add Optimizing " +
                    "Job to Job Group\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to removing a job from a job group.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleRemoveJobFromGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleRemoveJobFromGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "update a job group.");
      return;
    }


    // Get the job group from which the job should be removed.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if (jobGroupName == null)
    {
      infoMessage.append("ERROR:  No job group specified.<BR>" + EOL);
      return;
    }

    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database." +
                           "<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      handleViewJobGroups(requestInfo);
      infoMessage.append("ERROR:  Cannot retrieve job group \"" + jobGroupName +
                         "\":  " + e.getMessage() + "<BR>" + EOL);
      return;
    }


    // Get the name of the job to remove from the job group (it may be a normal
    // job or an optimizing job).
    String jobName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME);
    if ((jobName == null) || (jobName.length() == 0))
    {
      infoMessage.append("ERROR:  No job was specified to remove from the " +
                         "job group.");
      handleViewJobGroup(requestInfo);
      return;
    }


    // Right now, we only support removing the last job in the group so that we
    // don't cause problems with dependencies.  Make sure the specified job is
    // the last one.
    ArrayList jobList = jobGroup.getJobList();
    if (jobList.isEmpty())
    {
      infoMessage.append("ERROR:  There are no jobs or optimizing jobs " +
                         "defined in job group " + jobGroupName + ".<BR>" +
                         EOL);
      handleViewJobGroup(requestInfo);
      return;
    }

    Object o = jobList.get(jobList.size() - 1);
    if (o instanceof JobGroupJob)
    {
      if (! ((JobGroupJob) o).getName().equals(jobName))
      {
        infoMessage.append("ERROR:  The specified job is not the last job " +
                           "in the list.  Only the last job in a job group " +
                           "may be removed to ensure that dependencies are " +
                           "properly maintained.<BR>" + EOL);
        handleViewJobGroup(requestInfo);
        return;
      }
    }
    else if (o instanceof JobGroupOptimizingJob)
    {
      if (! ((JobGroupOptimizingJob) o).getName().equals(jobName))
      {
        infoMessage.append("ERROR:  The specified job is not the last job " +
                           "in the list.  Only the last job in a job group " +
                           "may be removed to ensure that dependencies are " +
                           "properly maintained.<BR>" + EOL);
        handleViewJobGroup(requestInfo);
        return;
      }
    }
    else
    {
      infoMessage.append("ERROR:  Unrecognized object \"" + String.valueOf(o) +
                         "\" as the last element of the job group.  It must " +
                         "be either a job or an optimizing job.<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }


    // See if the user provided confirmation that the group should be updated.
    // If so, then remove the job and update the list.  Otherwise, display the
    // form to request confirmation.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.equalsIgnoreCase("yes")))
    {
      try
      {
        jobList.remove(o);
        configDB.writeJobGroup(jobGroup);
        infoMessage.append("Successfully updated job group \"" + jobGroupName +
                           "\" in the configuration database.<BR>" + EOL);
        handleViewJobGroup(requestInfo);
        return;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not update job group\"" +
                           jobGroupName + "\" in the configuration " +
                           "database:  " + e.getMessage() + ".<BR>" + EOL);
        handleViewJobGroup(requestInfo);
        return;
      }
    }
    else if ((confirmStr != null) && (confirmStr.equalsIgnoreCase("no")))
    {
      infoMessage.append("Job Group \"" + jobGroupName + "\" was not " +
                         "updated.<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Update Job Group \"" + jobGroupName + "\"</SPAN>" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);

      if (o instanceof JobGroupJob)
      {
        htmlBody.append("Are you sure that you want to remove job \"" +
                        jobName + "\" from job group \"" + jobGroupName +
                        "\"?" + EOL);
      }
      else if (o instanceof JobGroupOptimizingJob)
      {
        htmlBody.append("Are you sure that you want to remove optimizing " +
                        "job \"" + jobName +  "\" from job group \"" +
                        jobGroupName + "\"?" + EOL);
      }

      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_REMOVE_JOB_FROM_GROUP)
                      + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroupName) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_JOB_NAME,
                                     jobName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"Yes\">" +
                      EOL);
      htmlBody.append("  &nbsp;&nbsp;" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"No\">" +
                      EOL);

      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing related to removing a job group from the SLAMD
   * server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleRemoveJobGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleRemoveJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "remove a job group.");
      return;
    }


    // Get the job group that should be removed.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if (jobGroupName == null)
    {
      infoMessage.append("ERROR:  No job group specified.<BR>" + EOL);
      return;
    }

    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database." +
                           "<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Cannot retrieve job group \"" + jobGroupName +
                         "\":  " + e.getMessage() + "<BR>" + EOL);
      return;
    }


    // See if the user provided confirmation.  If so, then remove the job
    // group.  If not, then get it.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.equalsIgnoreCase("yes")))
    {
      try
      {
        configDB.removeJobGroup(jobGroupName);
        infoMessage.append("Successfully removed job group \"" + jobGroupName +
                           "\" from the configuration database.<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Could not remove job group\"" +
                           jobGroupName + "\" from the configuration " +
                           "database:  " + e.getMessage() + ".<BR>" + EOL);
        return;
      }
    }
    else if ((confirmStr != null) && (confirmStr.equalsIgnoreCase("no")))
    {
      infoMessage.append("Job Group \"" + jobGroupName + "\" was not removed " +
                         "from the configuration database.<BR>" + EOL);
      handleViewJobGroup(requestInfo);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Remove Job Group \"" + jobGroupName + "\"</SPAN>" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to remove this job group?" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("        " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.
                                SERVLET_SECTION_JOB_REMOVE_GROUP)
                      + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                     jobGroupName) + EOL);

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"Yes\">" +
                      EOL);
      htmlBody.append("  &nbsp;&nbsp;" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"No\">" +
                      EOL);

      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing related to scheduling a job group.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleScheduleJobGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleScheduleJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule a job to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "schedule a job group.");
      return;
    }


    // Get the job group that should be scheduled.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if (jobGroupName == null)
    {
      infoMessage.append("ERROR:  No job group specified.<BR>" + EOL);
      return;
    }

    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database." +
                           "<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Cannot retrieve job group \"" + jobGroupName +
                         "\":  " + e.getMessage() + "<BR>" + EOL);
      return;
    }


    // See if the user has submitted the form and wants to schedule the job.
    String confirmed = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    boolean submitted = ((confirmed != null) && (confirmed.length() > 0));
    if (submitted)
    {
      boolean jobValid = true;

      String folderName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if ((folderName == null) || (folderName.length() == 0))
      {
        jobValid = false;
        infoMessage.append("ERROR:  No job folder name was provided.<BR>" +
                           EOL);
      }
      else
      {
        try
        {
          JobFolder folder = configDB.getFolder(folderName);
          if (folder == null)
          {
            jobValid = false;
            infoMessage.append("ERROR:  Job folder \"" + folderName +
                               "\" does not exist in the configuration " +
                               "database.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          jobValid = false;
          infoMessage.append("ERROR:  Could not retrieve job folder \"" +
                             folderName + "\":  " + e.getMessage() + ".<BR>" +
                             EOL);
        }
      }


      Date startTime;
      String startTimeStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
      if ((startTimeStr == null) || (startTimeStr.length() == 0))
      {
        startTime = new Date();
      }
      else
      {
        try
        {
          startTime = dateFormat.parse(startTimeStr);
        }
        catch (Exception e)
        {
          startTime = new Date();

          jobValid = false;
          infoMessage.append("ERROR:  Could not parse the job start time " +
                             "value \"" + startTimeStr + "\" as a valid " +
                             "timestamp in the form YYYYMMDDhhmmss.<BR>" + EOL);
        }
      }


      String[] requestedClients = new String[0];
      String requestedClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_CLIENTS);
      if ((requestedClientsStr !=  null) && (requestedClientsStr.length() > 0))
      {
        ArrayList<String> clientList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(requestedClientsStr);
        while (tokenizer.hasMoreTokens())
        {
          String token = tokenizer.nextToken();
          try
          {
            InetAddress clientAddress = InetAddress.getByName(token);
            clientList.add(clientAddress.getHostAddress());
          }
          catch (Exception e)
          {
            jobValid = false;
            infoMessage.append("ERROR:  Could not resolve client address \"" +
                               token + "\" to an IP address.<BR>" + EOL);
          }
        }

        if (! clientList.isEmpty())
        {
          requestedClients = new String[clientList.size()];
          clientList.toArray(requestedClients);
        }
      }


      String[] requestedMonitorClients = new String[0];
      String requestedMonitorClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
      if ((requestedMonitorClientsStr !=  null) &&
          (requestedMonitorClientsStr.length() > 0))
      {
        ArrayList<String> monitorClientList = new ArrayList<String>();
        StringTokenizer tokenizer =
             new StringTokenizer(requestedMonitorClientsStr);
        while (tokenizer.hasMoreTokens())
        {
          String token = tokenizer.nextToken();
          try
          {
            InetAddress clientAddress = InetAddress.getByName(token);
            monitorClientList.add(clientAddress.getHostAddress());
          }
          catch (Exception e)
          {
            jobValid = false;
            infoMessage.append("ERROR:  Could not resolve monitor client " +
                               "address \"" + token +
                               "\" to an IP address.<BR>" + EOL);
          }
        }

        if (! monitorClientList.isEmpty())
        {
          requestedMonitorClients = new String[monitorClientList.size()];
          monitorClientList.toArray(requestedMonitorClients);
        }
      }


      // Monitor Clients if Available
      boolean monitorClientsIfAvailable = false;
      String monitorIfAvailableStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
      if (monitorIfAvailableStr != null)
      {
        monitorClientsIfAvailable =
             (monitorIfAvailableStr.equalsIgnoreCase("true") ||
              monitorIfAvailableStr.equalsIgnoreCase("yes") ||
              monitorIfAvailableStr.equalsIgnoreCase("on") ||
              monitorIfAvailableStr.equalsIgnoreCase("1"));
      }


      // See if a dependency was specified.
      String[] dependencies;
      String dependency =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
      if ((dependency == null) || (dependency.length() == 0))
      {
        dependencies = new String[0];
      }
      else
      {
        dependencies = new String[] { dependency };
      }


      Parameter[] groupParams =
           jobGroup.getParameters().clone().getParameters();
      for (int i=0; i < groupParams.length; i++)
      {
        try
        {
          groupParams[i].htmlInputFormToValue(request);
        }
        catch (InvalidValueException ive)
        {
          jobValid = false;
          infoMessage.append("ERROR:  The provided value for job parameter " +
                             groupParams[i].getDisplayName() +
                             " is invalid:  " + ive.getMessage() + ".<BR>" +
                             EOL);
        }
      }
      ParameterList parameters = new ParameterList(groupParams);


      if (jobValid)
      {
        try
        {
          ArrayList<String> messages = new ArrayList<String>();
          jobGroup.schedule(slamdServer, startTime, folderName,
                            requestedClients, requestedMonitorClients,
                            monitorClientsIfAvailable, dependencies, parameters,
                            messages);

          for (int i=0; i < messages.size(); i++)
          {
            infoMessage.append(messages.get(i) + "<BR>" + EOL);
          }

          infoMessage.append("Successfully scheduled all jobs in job group " +
                             jobGroup.getName() + ".<BR>" + EOL);

          handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                        null, null);
          return;
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          infoMessage.append("ERROR:  An error occurred while attepting to " +
                             "schedule one of the jobs in job group " +
                             jobGroup.getName() + ":  " + e.getMessage() +
                             ".  Only some of the jobs may have been " +
                             "scheduled.<BR>" + EOL);
          handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                        null, null);
          return;
        }
      }
    }


    // Generate the form that allows the user to schedule the job group.
    String star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                  "\">*</SPAN>";

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Schedule the \"" + jobGroupName +
                    "\" Job Group</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroupName, jobGroupName);
    htmlBody.append("Enter the following information to use to schedule " +
                    "the \"" + link + "\" job group." + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Fields marked with an asterisk (" + star +
                    ") are required to have a value.");
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_SCHEDULE_GROUP) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);


    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
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

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Place in Folder</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        if ((folderName != null) &&
            folderName.equalsIgnoreCase(folders[i].getFolderName()))
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
      htmlBody.append("        </SELECT>");
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    String startTimeStr =
         request.getParameter(Constants.SERVLET_PARAM_JOB_START_TIME);
    if (startTimeStr == null)
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


    String[] requestedClients =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_CLIENTS);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Use Specific Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_CLIENTS + "\" ROWS=\"5\"" +
                    " COLS=\"40\">");
    String separator = "";
    for (int i=0; ((requestedClients != null) &&
                   (i < requestedClients.length)); i++)
    {
      htmlBody.append(separator);
      htmlBody.append(requestedClients[i]);
      separator = EOL;
    }
    htmlBody.append("</TEXTAREA></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);


    String[] monitorClients = request.getParameterValues(
                                   Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Resource Monitor Clients</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><TEXTAREA NAME=\"" +
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
    htmlBody.append("    </TR>" + EOL);


    boolean monitorClientsIfAvailable;
    String monitorStr =
         request.getParameter(
              Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE);
    if ((monitorStr != null) &&
        (monitorStr.equalsIgnoreCase("true") ||
         monitorStr.equalsIgnoreCase("yes") ||
         monitorStr.equalsIgnoreCase("on") || monitorStr.equalsIgnoreCase("1")))
    {
      monitorClientsIfAvailable = true;
    }
    else
    {
      monitorClientsIfAvailable = false;
    }
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Monitor Clients if Available</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE +
                    '"' + (monitorClientsIfAvailable ? " CHECKED" : "") +
                    "></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

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

    if ((pendingJobs.length > 0) || (runningJobs.length > 0) ||
        (optimizingJobs.length > 0))
    {
      String dependencyStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DEPENDENCY);
      if (dependencyStr == null)
      {
        dependencyStr = "";
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Job Group Dependency</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DEPENDENCY + "\">" + EOL);
      htmlBody.append("          <OPTION VALUE=\"\">No Dependency" + EOL);
      for (int i=0; i < pendingJobs.length; i++)
      {
        String description = pendingJobs[i].getJobDescription();
        if (description == null)
        {
          description = "";
        }
        else if (description.length() > 0)
        {
          description = " -- " + description;
        }

        String selectedStr;
        if (pendingJobs[i].getJobID().equals(dependencyStr))
        {
          selectedStr = "\" SELECTED>";
        }
        else
        {
          selectedStr = "\">";
        }

        htmlBody.append("          <OPTION VALUE=\"" +
                        pendingJobs[i].getJobID() + selectedStr +
                        pendingJobs[i].getJobID() + " -- Pending " +
                        pendingJobs[i].getJobName() + description + EOL);
      }

      for (int i=0; i < runningJobs.length; i++)
      {
        String description = runningJobs[i].getJobDescription();
        if (description == null)
        {
          description = "";
        }
        else if (description.length() > 0)
        {
          description = " -- " + description;
        }

        String selectedStr;
        if (runningJobs[i].getJobID().equals(dependencyStr))
        {
          selectedStr = "\" SELECTED>";
        }
        else
        {
          selectedStr = "\">";
        }

        htmlBody.append("          <OPTION VALUE=\"" +
                        runningJobs[i].getJobID() + selectedStr +
                        runningJobs[i].getJobID() + " -- Running " +
                        runningJobs[i].getJobName() + description + EOL);
      }

      for (int i=0; i < optimizingJobs.length; i++)
      {
        String description = optimizingJobs[i].getDescription();
        if (description == null)
        {
          description = "";
        }
        else
        {
          description = " -- " + description;
        }

        String selectedStr;
        if (optimizingJobs[i].getOptimizingJobID().equals(dependencyStr))
        {
          selectedStr = "\" SELECTED>";
        }
        else
        {
          selectedStr = "\">";
        }

        htmlBody.append("          <OPTION VALUE=\"" +
                        optimizingJobs[i].getOptimizingJobID() + selectedStr +
                        optimizingJobs[i].getOptimizingJobID() +
                        " -- Optimizing " +
                        optimizingJobs[i].getJobClass().getJobName() +
                        description + EOL);
      }

      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
    }


    Parameter[] stubs = jobGroup.getParameters().clone().getParameters();
    if (stubs.length > 0)
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      for (int i=0; i < stubs.length; i++)
      {
        String displayName = stubs[i].getDisplayName();
        if (displayName == null)
        {
          displayName = stubs[i].getName();
        }

        if (stubs[i].isRequired())
        {
          displayName += ' ' + star;
        }

        if (submitted)
        {
          try
          {
            stubs[i].htmlInputFormToValue(request);
          } catch (Exception e) {}
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>" + displayName + "</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        stubs[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }
    }


    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Schedule Job " +
                    "Group\"></TD>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles the work of cloning an existing job group.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleCloneJobGroup(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleScheduleJobGroup()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to schedule a job to do anything here
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "clone a job group.");
      return;
    }


    // Get the job group that should be cloned.
    String jobGroupName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_GROUP_NAME);
    if (jobGroupName == null)
    {
      infoMessage.append("ERROR:  No job group specified.<BR>" + EOL);
      return;
    }

    JobGroup jobGroup;
    try
    {
      jobGroup = configDB.getJobGroup(jobGroupName);
      if (jobGroup == null)
      {
        infoMessage.append("ERROR:  Job group \"" + jobGroupName +
                           "\" does not exist in the configuration database." +
                           "<BR>" + EOL);
        handleViewJobGroups(requestInfo);
        return;
      }
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));

      infoMessage.append("ERROR:  Cannot retrieve job group \"" + jobGroupName +
                         "\":  " + e.getMessage() + "<BR>" + EOL);
      return;
    }


    // See if a new name has been specified.  If so, then perform the clone.
    // Otherwise, show the form allowing the user to choose the name.
    String cloneName = request.getParameter(Constants.SERVLET_PARAM_NEW_NAME);
    if ((cloneName != null) && (cloneName.length() > 0) &&
        (! cloneName.equalsIgnoreCase(jobGroupName)))
    {
      try
      {
        String description =
             request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);


        // Make sure that the new name does not conflict with the name of an
        // existing job group.
        if (configDB.getJobGroup(cloneName) == null)
        {
          jobGroup.setName(cloneName);
          jobGroup.setDescription(description);

          configDB.writeJobGroup(jobGroup);
          infoMessage.append("Successfully cloned job group \"" + jobGroupName +
                             "\" as \"" + cloneName + "\".<BR>" + EOL);
          handleViewJobGroup(requestInfo, cloneName);
          return;
        }
        else
        {
          infoMessage.append("ERROR:  A job group already exists with the " +
                             "specified name.<BR>" + EOL);
        }
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Unable to clone the job:  " + e + ".<BR>" +
                           EOL);
        handleViewJobGroup(requestInfo);
        return;
      }
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Clone Job Group \"" + jobGroupName + "\"</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                               Constants.SERVLET_SECTION_JOB_VIEW_GROUP,
                               Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                               jobGroupName, jobGroupName);
    htmlBody.append("Enter the following information to use to schedule " +
                    "the \"" + link + "\" job group." + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SECTION,
                                   Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_CLONE_GROUP) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_JOB_GROUP_NAME,
                                   jobGroupName) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    if ((cloneName == null) || (cloneName.length() == 0))
    {
      cloneName = jobGroup.getName();
    }

    String description =
                request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);
    if ((description == null) || (description.length() == 0))
    {
      description = jobGroup.getDescription();
      if (description == null)
      {
        description = "";
      }
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>New Job Group Name:</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_NEW_NAME + "\" VALUE=\"" +
                    cloneName + "\" SIZE=\"40\">" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>Job Group Description:</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_DESCRIPTION + "\" VALUE=\"" +
                    description + "\" SIZE=\"40\">" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\"></TD>" +
                    EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }
}
