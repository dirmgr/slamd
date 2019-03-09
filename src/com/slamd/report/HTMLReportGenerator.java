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



import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;

import com.slamd.admin.RequestInfo;
import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;
import com.slamd.job.Job;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class provides an implementation of a SLAMD report generator that will
 * write the report information in HTML form.  The resulting files will be
 * packaged together in a zip archive.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTMLReportGenerator
       implements ReportGenerator
{
  /**
   * The end-of-line string that will be used.
   */
  public static final String EOL = Constants.EOL;



  /**
   * The name of the configuration parameter that specifies the level of
   * compression to use when generating the zip archive.
   */
  public static final String PARAM_COMPRESSION_LEVEL = "compression_level";



  /**
   * The name of the configuration parameter that indicates whether to include
   * detailed statistical information in the report.
   */
  public static final String PARAM_INCLUDE_DETAILED_STATISTICS =
       "include_detailed_stats";



  /**
   * The name of the configuration parameter that indicates whether to include
   * graphs in the report.
   */
  public static final String PARAM_INCLUDE_GRAPHS = "include_graphs";



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
   * The name of the configuration parameter that indicates whether to
   * include the individual iterations of an optimizing job.
   */
  public static final String PARAM_INCLUDE_OPTIMIZING_ITERATIONS =
       "include_optimizing_iterations";



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
   * The compression options that will be presented to the end user.
   */
  public static final String[] COMPRESSION_OPTIONS = new String[]
  {
    "Default Compression Level",
    "No Compression",
    "Fastest Compression",
    "Best Compression"
  };



  // The list used to hold the jobs that will be included in the generated
  // report.
  private ArrayList<Job> jobList;

  // The list used to hold the optimizing jobs that will be included in the
  // generated report.
  private ArrayList<OptimizingJob> optimizingJobList;

  // Indicates whether to include detailed statistics for the jobs included in
  // the report.
  private boolean includeDetailedStats;

  // Indicates whether to include graphs in the generated results.
  private boolean includeGraphs;

  // Indicates whether to include the job-specific configuration in the
  // generated report.
  private boolean includeJobConfig;

  // Indicates whether to include resource monitor statistics in the generated
  // report.
  private boolean includeMonitorStats;

  // Indicates whether to only provide information for individual iterations of
  // an optimizing job.
  private boolean includeOptimizingIterations;

  // Indicates whether to include the schedule configuration in the generated
  // report.
  private boolean includeScheduleConfig;

  // Indicates whether to include job statistics in the generated report.
  private boolean includeStats;

  // Indicates whether to only include jobs that have statistics.
  private boolean requireStats;

  // The decimal format that will be used to format floating-point values.
  private DecimalFormat decimalFormat;

  // The level of compression to use for the generated zip archive.
  private int compressionLevel;

  // The set of jobs that will actually be included in the report.
  private Job[] reportJobs;

  // The set of optimizing jobs that will actually be included in the report.
  private OptimizingJob[] reportOptimizingJobs;

  // The date formatter that will be used when writing out dates.
  private SimpleDateFormat dateFormat;



  /**
   * Creates a new text report generator.
   */
  public HTMLReportGenerator()
  {
    jobList           = new ArrayList<Job>();
    optimizingJobList = new ArrayList<OptimizingJob>();
    dateFormat        = new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    decimalFormat     = new DecimalFormat("0.000");
    compressionLevel  = Deflater.DEFAULT_COMPRESSION;

    includeScheduleConfig       = true;
    includeJobConfig            = true;
    includeStats                = true;
    includeMonitorStats         = true;
    includeDetailedStats        = false;
    includeGraphs               = true;
    requireStats                = true;
    includeOptimizingIterations = true;
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
    return "HTML Report Generator";
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
    return new HTMLReportGenerator();
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
    BooleanParameter includeGraphsParameter =
         new BooleanParameter(PARAM_INCLUDE_GRAPHS,
                              "Include Graphs of Statistics",
                              "Indicates whether graphs of statistical " +
                              "information should be included in the report.",
                              includeGraphs);
    BooleanParameter includeDetailParameter =
         new BooleanParameter(PARAM_INCLUDE_DETAILED_STATISTICS,
                              "Include Detailed Statistical Information",
                              "Indicates whether to include a detailed " +
                              "report of the statistics collected.",
                              includeDetailedStats);
    BooleanParameter requireStatsParameter =
         new BooleanParameter(PARAM_REQUIRE_STATS,
                              "Only Include Jobs with Statistics",
                              "Indicates whether to only include jobs that " +
                              "have statistics available.", requireStats);
    BooleanParameter includeOptimizingParameter =
         new BooleanParameter(PARAM_INCLUDE_OPTIMIZING_ITERATIONS,
                              "Include Optimizing Job Iterations",
                              "Indicates whether to include data for the " +
                              "individual iterations of an optimizing job.",
                              includeOptimizingIterations);

    String compressionLevelStr = null;
    switch (compressionLevel)
    {
      case Deflater.NO_COMPRESSION:
        compressionLevelStr = COMPRESSION_OPTIONS[1];
        break;
      case Deflater.BEST_SPEED:
        compressionLevelStr = COMPRESSION_OPTIONS[2];
        break;
      case Deflater.BEST_COMPRESSION:
        compressionLevelStr = COMPRESSION_OPTIONS[3];
        break;
      default:
        compressionLevelStr = COMPRESSION_OPTIONS[0];
        break;
    }

    MultiChoiceParameter compressionLevelParameter =
         new MultiChoiceParameter(PARAM_COMPRESSION_LEVEL,
                                  "Level of Compression",
                                  "The level of compression to use when " +
                                  "generating the zip archive",
                                  COMPRESSION_OPTIONS, compressionLevelStr);

    Parameter[] parameters = new Parameter[]
    {
      includeScheduleConfigParameter,
      includeJobConfigParameter,
      includeStatsParameter,
      includeMonitorParameter,
      includeGraphsParameter,
      includeDetailParameter,
      requireStatsParameter,
      includeOptimizingParameter,
      compressionLevelParameter
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

    bp = reportParameters.getBooleanParameter(PARAM_INCLUDE_GRAPHS);
    if (bp != null)
    {
      includeGraphs = bp.getBooleanValue();
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
                               PARAM_INCLUDE_OPTIMIZING_ITERATIONS);
    if (bp != null)
    {
      includeOptimizingIterations = bp.getBooleanValue();
    }

    MultiChoiceParameter mp =
         reportParameters.getMultiChoiceParameter(PARAM_COMPRESSION_LEVEL);
    if (mp != null)
    {
      String levelStr = mp.getStringValue();
      if (levelStr.equals(COMPRESSION_OPTIONS[0]))
      {
        compressionLevel = Deflater.DEFAULT_COMPRESSION;
      }
      else if (levelStr.equals(COMPRESSION_OPTIONS[1]))
      {
        compressionLevel = Deflater.NO_COMPRESSION;
      }
      else if (levelStr.equals(COMPRESSION_OPTIONS[2]))
      {
        compressionLevel = Deflater.BEST_SPEED;
      }
      else if (levelStr.equals(COMPRESSION_OPTIONS[3]))
      {
        compressionLevel = Deflater.BEST_COMPRESSION;
      }
      else
      {
        compressionLevel = Deflater.DEFAULT_COMPRESSION;
      }
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

    optimizingJobList.add(optimizingJob);
  }



  /**
   * Generates the report and sends it to the user over the provided servlet
   * response.
   *
   * @param  requestInfo  State information about the request being processed.
   */
  public void generateReport(RequestInfo requestInfo)
  {
    // Determine exactly what to include in the report.  We will want to strip
    // out any individual jobs that are part of an optimizing job that is also
    // to be included in the report.
    reportOptimizingJobs = new OptimizingJob[optimizingJobList.size()];
    optimizingJobList.toArray(reportOptimizingJobs);

    ArrayList<Job> tmpList = new ArrayList<Job>(jobList.size());
    for (int i=0; i < jobList.size(); i++)
    {
      Job job = jobList.get(i);
      String optimizingJobID = job.getOptimizingJobID();
      if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
      {
        boolean matchFound = false;

        for (int j=0; j < reportOptimizingJobs.length; j++)
        {
          if (optimizingJobID.equalsIgnoreCase(
                   reportOptimizingJobs[j].getOptimizingJobID()))
          {
            matchFound = true;
            break;
          }
        }

        if (matchFound)
        {
          continue;
        }
      }

      tmpList.add(job);
    }
    reportJobs = new Job[tmpList.size()];
    tmpList.toArray(reportJobs);


    // Actually generate the report and send it to the user.
    HttpServletResponse response = requestInfo.getResponse();
    response.setContentType("application/zip");
    response.addHeader("Content-Disposition",
                       "filename=\"slamd_data_report.zip\"");

    try
    {
      ZipOutputStream zipStream =
           new ZipOutputStream(response.getOutputStream());
      zipStream.setLevel(compressionLevel);
      createIndexPage(requestInfo, zipStream);

      for (int i=0; i < reportJobs.length; i++)
      {
        try
        {
          createJobPage(requestInfo, reportJobs[i], zipStream);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }

      for (int i=0; i < reportOptimizingJobs.length; i++)
      {
        try
        {
          createOptimizingJobPage(requestInfo, reportOptimizingJobs[i],
                                  zipStream);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }

      zipStream.flush();
      zipStream.close();
    }
    catch (IOException ioe)
    {
      // Not much we can do about this.
      System.err.println("Unable to generate report:  " + ioe);
    }
  }



  /**
   * Writes the index page into the provided zip output stream.
   *
   * @param  requestInfo  State information about the request being processed.
   * @param  zipStream    The zip output stream to which the data should be
   *                      written.
   *
   * @throws  IOException  If a problem occurs while writing to the zip output
   *                       stream.
   */
  private void createIndexPage(RequestInfo requestInfo,
                               ZipOutputStream zipStream)
          throws IOException
  {
    StringBuilder buffer = new StringBuilder();
    writePageHeader(requestInfo, buffer, true);

    buffer.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                  "\">SLAMD Generated Report</SPAN>" + EOL);
    buffer.append("<BR><BR>" + EOL);
    buffer.append("<TABLE BORDER=\"0\">" + EOL);
    buffer.append("  <TR>" + EOL);
    buffer.append("    <TD>Generation Date</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + dateFormat.format(new Date()) + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    HttpServletRequest request = requestInfo.getRequest();
    String serverURL = request.getScheme() + "://" + request.getServerName() +
                       ':' + request.getServerPort() +
                       requestInfo.getServletBaseURI();
    buffer.append("  <TR>" + EOL);
    buffer.append("    <TD>SLAMD Server URL</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD><A HREF=\"" + serverURL + "\">" + serverURL +
                  "</A></TD>" + EOL);
    buffer.append("  </TR>" + EOL);
    buffer.append("</TABLE>" + EOL);
    buffer.append(EOL);

    if (reportJobs.length > 0)
    {
      buffer.append("<BR><BR>" + EOL);
      buffer.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Job Data</SPAN>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
      buffer.append("  <TR>" + EOL);
      buffer.append("    <TD><B>Job ID</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD><B>Description</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD><B>Job Type</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      for (int i=0; i < reportJobs.length; i++)
      {
        Job job = reportJobs[i];
        if ((i % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }

        String id = job.getJobID();
        buffer.append("    <TD><A HREF=\"jobs/job_" + id + ".html\">" +
                      id + "</A></TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);


        String description = job.getJobDescription();
        if ((description == null) || (description.length() == 0))
        {
          description = "&nbsp;";
        }
        buffer.append("    <TD>" + description + "</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);

        buffer.append("    <TD>" + job.getJobClass().getJobName() + "</TD>" +
                      EOL);
        buffer.append("  </TR>" + EOL);
      }

      buffer.append("</TABLE>" + EOL);
      buffer.append(EOL);
    }


    if (reportOptimizingJobs.length > 0)
    {
      buffer.append("<BR><BR>" + EOL);
      buffer.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Optimizing Job Data</SPAN>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
      buffer.append("  <TR>" + EOL);
      buffer.append("    <TD><B>Optimizing Job ID</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD><B>Description</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD><B>Job Type</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      for (int i=0; i < reportOptimizingJobs.length; i++)
      {
        OptimizingJob job = reportOptimizingJobs[i];
        if ((i % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }

        String id = job.getOptimizingJobID();
        buffer.append("    <TD><A HREF=\"jobs/optimizing_job_" + id +
                      ".html\">" +
                      id + "</A></TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);


        String description = job.getDescription();
        if ((description == null) || (description.length() == 0))
        {
          description = "&nbsp;";
        }
        buffer.append("    <TD>" + description + "</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);

        buffer.append("    <TD>" + job.getJobClass().getJobName() + "</TD>" +
                      EOL);
        buffer.append("  </TR>" + EOL);
      }

      buffer.append("</TABLE>" + EOL);
      buffer.append(EOL);
    }

    writePageFooter(requestInfo, buffer);

    zipStream.putNextEntry(new ZipEntry("index.html"));
    zipStream.write(buffer.toString().getBytes());
    zipStream.closeEntry();
  }



  /**
   * Writes a page for the given job to the provided zip output stream.
   *
   * @param  requestInfo  State information about the request being processed.
   * @param  job          The job to be written to the zip output stream.
   * @param  zipStream    The zip output stream to which the job data should be
   *                      written.
   *
   * @throws  IOException  If a problem occurs while writing to the zip output
   *                       stream.
   */
  private void createJobPage(RequestInfo requestInfo, Job job,
                             ZipOutputStream zipStream)
          throws IOException
  {
    StringBuilder buffer = new StringBuilder();
    writePageHeader(requestInfo, buffer, false);

    buffer.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                  "\">Information for Job " + job.getJobID() + "</SPAN>" + EOL);
    buffer.append("<BR><BR>" + EOL);

    int i=0;

    buffer.append("<B>General Information</B>" + EOL);
    buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Job ID</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + job.getJobID() + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    String optimizingJobID = job.getOptimizingJobID();
    if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
    {
      // See if we have information about the optimizing job for this job.
      boolean generateLink = false;
      for (int j=0; j < reportOptimizingJobs.length; j++)
      {
        if (optimizingJobID.equalsIgnoreCase(
                 reportOptimizingJobs[j].getOptimizingJobID()))
        {
          generateLink = true;
          break;
        }
      }

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Optimizing Job ID</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);

      if (generateLink)
      {
        buffer.append("    <TD><A HREF=\"optimizing_job_" + optimizingJobID +
                      ".html\">" + optimizingJobID + "</A></TD>" + EOL);
      }
      else
      {
        buffer.append("    <TD>" + optimizingJobID + "</TD>" + EOL);
      }

      buffer.append("  </TR>" + EOL);
    }

    String description = job.getJobDescription();
    if ((description == null) || (description.length() == 0))
    {
      description = "(Not Specified)";
    }
    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Job Description</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + description + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Job Type</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + job.getJobClass().getJobName() + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Job Class</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + job.getJobClassName() + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Job State</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + Constants.jobStateToString(job.getJobState()) +
                  "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);
    buffer.append("</TABLE>" + EOL);


    // Write information to the buffer about the schedule configuration.
    if (includeScheduleConfig)
    {
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>Schedule Information</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

      Date startTime = job.getStartTime();
      String startTimeStr;
      if (startTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(startTime);
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Scheduled Start Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + startTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      Date stopTime = job.getStopTime();
      String stopTimeStr;
      if (stopTime == null)
      {
        stopTimeStr = "(Not Specified)";
      }
      else
      {
        stopTimeStr = dateFormat.format(stopTime);
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Scheduled Stop Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + stopTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      int duration = job.getDuration();
      String durationStr;
      if (duration > 0)
      {
        durationStr = duration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Scheduled Duration</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + durationStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Number of Clients</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + job.getNumberOfClients() + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      String[] requestedClients = job.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>Requested Clients</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + EOL);
        buffer.append("      " + requestedClients[0]);
        for (int j=1; j < requestedClients.length; j++)
        {
          buffer.append("<BR>" + EOL);
          buffer.append("      " + requestedClients[j]);
        }
        buffer.append(EOL);
        buffer.append("    </TD>" + EOL);
        buffer.append("  </TR>" + EOL);
      }

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Threads Per Client</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + job.getThreadsPerClient() + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Thread Startup Delay</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + job.getThreadStartupDelay() +
                    " milliseconds</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Statistics Collection Interval</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + job.getCollectionInterval() + " seconds</TD>" +
                    EOL);
      buffer.append("  </TR>" + EOL);
      buffer.append("</TABLE>" + EOL);
    }


    // Write information to the buffer about the job-specific configuration.
    if (includeJobConfig)
    {
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>Parameter Information</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

      Parameter[] params = job.getParameterList().getParameters();
      for (int j=0; j < params.length; j++)
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>" + params[j].getDisplayName() + "</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + params[j].getHTMLDisplayValue() + "</TD>" +
                      EOL);
        buffer.append("  </TR>" + EOL);
      }

      buffer.append("</TABLE>" + EOL);
    }


    // Write information to the buffer about the job statistics.
    LinkedHashMap<String,BufferedImage> graphMap =
         new LinkedHashMap<String,BufferedImage>();
    if (includeStats && job.hasStats())
    {
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>General Execution Data</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

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
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Actual Start Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + startTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

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
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Actual Stop Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + stopTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

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
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Actual Duration</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + durationStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      String[] clientsUsed = job.getStatTrackerClientIDs();
      if ((clientsUsed != null) && (clientsUsed.length > 0))
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("      <TD>Clients Used</TD>" + EOL);
        buffer.append("      <TD>&nbsp;</TD>" + EOL);
        buffer.append("      <TD>" + EOL);
        buffer.append("        " + clientsUsed[0]);
        for (int j=1; j < clientsUsed.length; j++)
        {
          buffer.append("<BR>" + EOL);
          buffer.append("        " + clientsUsed[j]);
        }
        buffer.append(EOL);
        buffer.append("      </TD>" + EOL);
      }

      buffer.append("</TABLE>" + EOL);

      String[] statNames = job.getStatTrackerNames();
      for (int j=0; j < statNames.length; j++)
      {
        StatTracker[] trackers = job.getStatTrackers(statNames[j]);
        if ((trackers != null) && (trackers.length > 0))
        {
          StatTracker tracker = trackers[0].newInstance();
          tracker.aggregate(trackers);
          buffer.append("<BR><BR>" + EOL);
          buffer.append("<B>" + tracker.getDisplayName() + "</B>" + EOL);
          buffer.append("<BR>" + EOL);
          if (includeDetailedStats)
          {
            buffer.append(tracker.getDetailHTML());
          }
          else
          {
            buffer.append(tracker.getSummaryHTML());
          }

          if (includeGraphs)
          {
            String filename = "images/job_" + job.getJobID() + "_graph_" + j +
                              ".png";
            buffer.append("<IMG SRC=\"../" + filename +
                          "\" ALT=\"Graph of Results for " + statNames[j] +
                          "\">" + EOL);
            buffer.append("<BR>" + EOL);

            ParameterList params = tracker.getGraphParameterStubs(job);
            BufferedImage image =
                 tracker.createGraph(job, Constants.DEFAULT_GRAPH_WIDTH,
                                     Constants.DEFAULT_GRAPH_HEIGHT, params);
            graphMap.put(filename, image);
          }
        }
      }
    }


    // Write information about the resource monitor statistics.
    if (includeMonitorStats && job.hasResourceStats())
    {
      String[] trackerNames = job.getResourceStatTrackerNames();
      for (int j=0; ((trackerNames != null) && (j < trackerNames.length)); j++)
      {
        StatTracker[] trackers = job.getResourceStatTrackers(trackerNames[j]);
        if ((trackers == null) || (trackers.length == 0))
        {
          continue;
        }

        StatTracker tracker = trackers[0].newInstance();
        tracker.aggregate(trackers);

        buffer.append("<BR><BR>" + EOL);
        buffer.append("<B>" + tracker.getDisplayName() + "</B>" + EOL);
        buffer.append("<BR>" + EOL);

        if (includeDetailedStats)
        {
          buffer.append(tracker.getDetailHTML());
        }
        else
        {
          buffer.append(tracker.getSummaryHTML());
        }

        if (includeGraphs)
        {
          String filename = "images/job_" + job.getJobID() +
                            "_monitor_graph_" + j + ".png";
          buffer.append("<IMG SRC=\"../" + filename +
                        "\" ALT=\"Graph of Results for " +
                        tracker.getDisplayName() + "\">" + EOL);
          buffer.append("<BR>" + EOL);

          ParameterList params = tracker.getMonitorGraphParameterStubs(job);
          BufferedImage image =
               tracker.createMonitorGraph(job, Constants.DEFAULT_GRAPH_WIDTH,
                    Constants.DEFAULT_MONITOR_GRAPH_HEIGHT, params);
          graphMap.put(filename, image);
        }
      }
    }


    writePageFooter(requestInfo, buffer);

    String filename = "jobs/job_" + job.getJobID() + ".html";
    zipStream.putNextEntry(new ZipEntry(filename));
    zipStream.write(buffer.toString().getBytes());
    zipStream.closeEntry();

    Iterator iterator = graphMap.keySet().iterator();
    while (iterator.hasNext())
    {
      String        imageName = (String) iterator.next();
      BufferedImage graph     = graphMap.get(imageName);

      zipStream.putNextEntry(new ZipEntry(imageName));
      try
      {
        zipStream.write(imageToByteArray(graph));
      } catch (IOException ioe) {}
      zipStream.closeEntry();
    }
  }



  /**
   * Writes a page for the given optimizing job to the provided zip output
   * stream.
   *
   * @param  requestInfo    State information about the request being processed.
   * @param  optimizingJob  The optimizing job to be written to the zip output
   *                        stream.
   * @param  zipStream      The zip output stream to which the job data should
   *                        be written.
   *
   * @throws  IOException  If a problem occurs while writing to the zip output
   *                       stream.
   */
  private void createOptimizingJobPage(RequestInfo requestInfo,
                                       OptimizingJob optimizingJob,
                                       ZipOutputStream zipStream)
          throws IOException
  {
    // Get the optimization algorithm and set of parameters.
    OptimizationAlgorithm optimizationAlgorithm =
         optimizingJob.getOptimizationAlgorithm();
    ParameterList paramList =
         optimizationAlgorithm.getOptimizationAlgorithmParameters();
    Parameter[] optimizationParams = paramList.getParameters();

    StringBuilder buffer = new StringBuilder();
    writePageHeader(requestInfo, buffer, false);

    buffer.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                  "\">Information for Optimizing Job " +
                  optimizingJob.getOptimizingJobID() + "</SPAN>" + EOL);
    buffer.append("<BR><BR>" + EOL);

    int i=0;

    buffer.append("<B>General Information</B>" + EOL);
    buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Optimizing Job ID</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + optimizingJob.getOptimizingJobID() + "</TD>" +
                  EOL);
    buffer.append("  </TR>" + EOL);

    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Job Type</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + optimizingJob.getJobClass().getJobName() +
                  "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    String description = optimizingJob.getDescription();
    if ((description == null) || (description.length() == 0))
    {
      description = "(Not Specified)";
    }
    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Base Description</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + description + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    boolean includeThreadCount = optimizingJob.includeThreadsInDescription();
    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Include Thread Count in Description</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + String.valueOf(includeThreadCount) + "</TD>" +
                  EOL);
    buffer.append("  </TR>" + EOL);

    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Optimizing Job State</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" +
                  Constants.jobStateToString(optimizingJob.getJobState()) +
                  "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);

    String stopReason = optimizingJob.getStopReason();
    if ((stopReason == null) || (stopReason.length() == 0))
    {
      stopReason = "(Not Available)";
    }
    if ((i++ % 2) == 0)
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                    "\">" + EOL);
    }
    else
    {
      buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                    "\">" + EOL);
    }
    buffer.append("    <TD>Stop Reason</TD>" + EOL);
    buffer.append("    <TD>&nbsp;</TD>" + EOL);
    buffer.append("    <TD>" + stopReason + "</TD>" + EOL);
    buffer.append("  </TR>" + EOL);
    buffer.append("</TABLE>" + EOL);


    // Write the schedule configuration to the report.
    if (includeScheduleConfig)
    {
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>Schedule Information</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

      Date startTime = optimizingJob.getStartTime();
      String startTimeStr;
      if (startTime == null)
      {
        startTimeStr = "(Not Available)";
      }
      else
      {
        startTimeStr = dateFormat.format(startTime);
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Scheduled Start Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + startTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      int duration = optimizingJob.getDuration();
      String durationStr;
      if (duration > 0)
      {
        durationStr = duration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Job Duration</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + durationStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Delay Between Iterations</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + optimizingJob.getDelayBetweenIterations() +
                    " seconds</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Number of Clients</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + optimizingJob.getNumClients() +
                    "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      String[] requestedClients = optimizingJob.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>Requested Clients</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + EOL);
        buffer.append("      " + requestedClients[0]);
        for (int j=1; j < requestedClients.length; j++)
        {
          buffer.append("<BR>" + EOL);
          buffer.append("      " + requestedClients[j]);
        }
        buffer.append(EOL);
        buffer.append("    </TD>" + EOL);
        buffer.append("  </TR>" + EOL);
      }

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Minimum Number of Threads</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + optimizingJob.getMinThreads() +
                    "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      int maxThreads = optimizingJob.getMaxThreads();
      String maxThreadStr;
      if (maxThreads > 0)
      {
        maxThreadStr = String.valueOf(maxThreads);
      }
      else
      {
        maxThreadStr = "(Not Specified)";
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Maximum Number of Threads</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + maxThreadStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Thread Increment Between Iterations</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + optimizingJob.getThreadIncrement() +
                    "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Statistics Collection Interval</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + optimizingJob.getCollectionInterval() +
                    " seconds</TD>" + EOL);
      buffer.append("  </TR>" + EOL);
      buffer.append("</TABLE>" + EOL);


      // Write the optimization settings to the report.
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>Optimization Settings</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

      for (int j=0; j < optimizationParams.length; j++)
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>" + optimizationParams[j].getDisplayName() +
                      "</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + optimizationParams[j].getHTMLDisplayValue() +
                      "</TD>" + EOL);
        buffer.append("  </TR>" + EOL);
      }

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Maximum Consecutive Non-Improving " +
                    "Iterations</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + optimizingJob.getMaxNonImproving() + "</TD>" +
                    EOL);
      buffer.append("  </TR>" + EOL);

      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Re-Run Best Iteration</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" +
                    String.valueOf(optimizingJob.reRunBestIteration()) +
                    "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      int reRunDuration = optimizingJob.getReRunDuration();
      if (reRunDuration > 0)
      {
        durationStr = reRunDuration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Re-Run Duration</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + durationStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);
      buffer.append("</TABLE>" + EOL);
    }


    // Write the job-specific configuration to the report.
    if (includeJobConfig)
    {
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>Parameter Information</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

      Parameter[] params = optimizingJob.getParameters().getParameters();
      for (int j=0; j < params.length; j++)
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>" + params[j].getDisplayName() + "</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + params[j].getHTMLDisplayValue() + "</TD>" +
                      EOL);
        buffer.append("  </TR>" + EOL);
      }

      buffer.append("</TABLE>" + EOL);
    }


    // Write the statistical information to the report.
    LinkedHashMap<String,BufferedImage> graphMap =
         new LinkedHashMap<String,BufferedImage>();
    if (includeStats && optimizingJob.hasStats())
    {
      i=0;
      buffer.append("<BR>" + EOL);
      buffer.append("<B>Execution Data</B>" + EOL);
      buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

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
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Actual Start Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + startTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

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
      if ((i++ % 2) == 0)
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                      "\">" + EOL);
      }
      else
      {
        buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                      "\">" + EOL);
      }
      buffer.append("    <TD>Actual Stop Time</TD>" + EOL);
      buffer.append("    <TD>&nbsp;</TD>" + EOL);
      buffer.append("    <TD>" + stopTimeStr + "</TD>" + EOL);
      buffer.append("  </TR>" + EOL);

      Job[] iterations = optimizingJob.getAssociatedJobs();
      if ((iterations != null) && (iterations.length > 0))
      {
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>Job Iterations Completed</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + iterations.length + "</TD>" + EOL);
        buffer.append("  </TR>" + EOL);

        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>Optimal Thread Count</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + optimizingJob.getOptimalThreadCount() +
                      "</TD>" + EOL);
        buffer.append("  </TR>" + EOL);

        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>Optimal Value</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" +
                      decimalFormat.format(optimizingJob.getOptimalValue()) +
                      "</TD>" + EOL);
        buffer.append("  </TR>" + EOL);

        String optimalID = optimizingJob.getOptimalJobID();
        if ((optimalID == null) || (optimalID.length() == 0))
        {
          optimalID = "(Not Available)";
        }
        else if (includeOptimizingIterations)
        {
          optimalID = "<A HREF=\"job_" + optimalID + ".html\">" + optimalID +
                      "</A>";
        }
        if ((i++ % 2) == 0)
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_A +
                        "\">" + EOL);
        }
        else
        {
          buffer.append("  <TR CLASS=\"" + Constants.STYLE_JOB_SUMMARY_LINE_B +
                        "\">" + EOL);
        }
        buffer.append("    <TD>Optimal Job Iteration</TD>" + EOL);
        buffer.append("    <TD>&nbsp;</TD>" + EOL);
        buffer.append("    <TD>" + optimalID + "</TD>" + EOL);
        buffer.append("  </TR>" + EOL);

        Job reRunIteration = optimizingJob.getReRunIteration();
        if (reRunIteration != null)
        {
          if ((i++ % 2) == 0)
          {
            buffer.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          }
          else
          {
            buffer.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          }
          buffer.append("    <TD>Re-Run Iteration</TD>" + EOL);
          buffer.append("    <TD>&nbsp;</TD>" + EOL);
          if (includeOptimizingIterations)
          {
            buffer.append("    <TD><A HREF=\"job_" + reRunIteration.getJobID() +
                          ".html\">" + reRunIteration.getJobID() + "</A></TD>" +
                          EOL);
          }
          else
          {
            buffer.append("    <TD>" + reRunIteration.getJobID() + "</TD>" +
                          EOL);
          }
          buffer.append("  </TR>" + EOL);

          String valueStr;
          try
          {
            double iterationValue =
                 optimizationAlgorithm.getIterationOptimizationValue(
                                            reRunIteration);
            valueStr = decimalFormat.format(iterationValue);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          buffer.append("    <TD>Re-Run Iteration Value</TD>" + EOL);
          buffer.append("    <TD>&nbsp;</TD>" + EOL);
          buffer.append("    <TD>" + valueStr + "</TD>" + EOL);
          buffer.append("  </TR>" + EOL);

          if (includeOptimizingIterations)
          {
            createJobPage(requestInfo, reRunIteration, zipStream);
          }
        }
      }

      buffer.append("</TABLE>" + EOL);

      if ((iterations != null) && (iterations.length > 0))
      {
        i=0;
        buffer.append("<BR>" + EOL);
        buffer.append("<B>Job Iterations</B>" + EOL);
        buffer.append("<TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);

        for (int j=0; j < iterations.length; j++)
        {
          if ((i++ % 2) == 0)
          {
            buffer.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          }
          else
          {
            buffer.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          }
          if (includeOptimizingIterations)
          {
            buffer.append("    <TD><A HREF=\"job_" + iterations[j].getJobID() +
                          ".html\">" + iterations[j].getJobID() + "</A></TD>" +
                          EOL);
          }
          else
          {
            buffer.append("    <TD>" + iterations[j].getJobID() + "</TD>" +
                          EOL);
          }
          buffer.append("    <TD>&nbsp;</TD>" + EOL);

          String valueStr;
          try
          {
            double value =
                 optimizationAlgorithm.getIterationOptimizationValue(
                                            iterations[j]);
            valueStr = decimalFormat.format(value);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          buffer.append("    <TD>" + valueStr + "</TD>" + EOL);
          buffer.append("  </TR>" + EOL);

          if (includeOptimizingIterations)
          {
            createJobPage(requestInfo, iterations[j], zipStream);
          }
        }

        buffer.append("</TABLE>" + EOL);

        if (includeGraphs)
        {
          String[] statNames = iterations[0].getStatTrackerNames();
          for (int j=0; j < statNames.length; j++)
          {
            buffer.append("<BR><BR>" + EOL);
            StatTracker[] trackers =
                 iterations[0].getStatTrackers(statNames[j]);
            if ((trackers != null) && (trackers.length > 0))
            {
              StatTracker tracker = trackers[0].newInstance();
              tracker.aggregate(trackers);

              String filename = "images/optimizing_job_" +
                                optimizingJob.getOptimizingJobID() + "_graph_" +
                                j + ".png";
              buffer.append("<IMG SRC=\"../" + filename +
                            "\" ALT=\"Comparison of Results for " +
                            tracker.getDisplayName() + "\">" + EOL);

              ParameterList params = tracker.getGraphParameterStubs(iterations);
              BufferedImage image =
                   tracker.createGraph(iterations,
                                       Constants.DEFAULT_GRAPH_WIDTH,
                                       Constants.DEFAULT_GRAPH_HEIGHT, params);
              graphMap.put(filename, image);
            }
          }
        }
      }
    }


    writePageFooter(requestInfo, buffer);

    String filename = "jobs/optimizing_job_" +
                      optimizingJob.getOptimizingJobID() + ".html";
    zipStream.putNextEntry(new ZipEntry(filename));
    zipStream.write(buffer.toString().getBytes());
    zipStream.closeEntry();

    Iterator<String> iterator = graphMap.keySet().iterator();
    while (iterator.hasNext())
    {
      String        imageName = iterator.next();
      BufferedImage graph     = graphMap.get(imageName);

      zipStream.putNextEntry(new ZipEntry(imageName));
      try
      {
        zipStream.write(imageToByteArray(graph));
      } catch (IOException ioe) {}
      zipStream.closeEntry();
    }
  }



  /**
   * Writes the standard HTML page header into the provided buffer.
   *
   * @param  requestInfo  State information about the request being processed.
   * @param  buffer       The buffer into which the header should be written.
   * @param  indexPage    Indicates whether the header is being written for the
   *                      index page or some other page.
   */
  private void writePageHeader(RequestInfo requestInfo, StringBuilder buffer,
                               boolean indexPage)
  {
    buffer.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 " +
                  "Transitional//EN\">" + EOL);
    buffer.append(EOL);
    buffer.append("<HTML>" + EOL);
    buffer.append("  <HEAD>" + EOL);
    buffer.append("    <TITLE>SLAMD Generated Report</TITLE>" + EOL);
    buffer.append("    <META HTTP-EQUIV=\"Content-Type\" " +
                  "CONTENT=\"text/html; charset=utf-8\">" + EOL);
    buffer.append(Constants.STYLE_SHEET_DATA);
    buffer.append("  </HEAD>" + EOL);
    buffer.append(EOL);
    buffer.append("  <BODY>" + EOL);
    buffer.append("    <TABLE WIDTH=\"100%\" BORDER=\"0\" CELLSPACING=\"10\">" +
                  EOL);
    buffer.append("      <TR>" + EOL);
    buffer.append("        <TD CLASS=\"blue_background\" ALIGN=\"LEFT\" " +
                  "WIDTH=\"33%\">&nbsp;<BR>&nbsp;</TD>" + EOL);
    buffer.append("        <TD CLASS=\"yellow_background\" ALIGN=\"CENTER\" " +
                  "WIDTH=\"34%\">" + EOL);
    buffer.append("          SLAMD Generated Report" + EOL);
    buffer.append("        </TD>" + EOL);
    buffer.append("        <TD CLASS=\"red_background\" ALIGN=\"RIGHT\" " +
                  "WIDTH=\"33%\">" + EOL);
    buffer.append("          Version " + DynamicConstants.SLAMD_VERSION + EOL);
    if (! DynamicConstants.OFFICIAL_BUILD)
    {
      buffer.append("          <BR>");
      buffer.append("          <FONT SIZE=\"-2\">Unofficial Build ID " +
                    DynamicConstants.BUILD_DATE + "</FONT>" + EOL);
    }
    buffer.append("        </TD>" + EOL);
    buffer.append("      </TR>" + EOL);
    buffer.append("    </TABLE>" + EOL);

    if (indexPage)
    {
      buffer.append("    <BR>" + EOL);
    }
    else
    {
      buffer.append("    <A HREF=\"../index.html\">Index Page</A>" + EOL);
      buffer.append("    <BR><BR>" + EOL);
    }

    buffer.append(EOL);
  }



  /**
   * Writes the standard HTML page footer into the provided buffer.
   *
   * @param  requestInfo  State information about the request being processed.
   * @param  buffer       The buffer into which the footer should be written.
   */
  private void writePageFooter(RequestInfo requestInfo, StringBuilder buffer)
  {
    buffer.append(EOL);
    buffer.append("  </BODY>" + EOL);
    buffer.append("</HTML" + EOL);
  }



  /**
   * Converts the provided image to a byte array containing data for the PNG
   * representation of the image.
   *
   * @param  image  The image to be converted to a byte array.
   *
   * @return  The byte array containing the image data.
   *
   * @throws  IOException  If a problem occurs converting the image to a byte
   *                       array.
   */
  private byte[] imageToByteArray(BufferedImage image)
          throws IOException
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
    ImageEncoder encoder = ImageCodec.createImageEncoder("png", outputStream,
                                                         null);
    encoder.encode(image);
    return outputStream.toByteArray();
  }
}

