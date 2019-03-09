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



import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;

import com.lowagie.text.Anchor;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfWriter;

import com.slamd.admin.RequestInfo;
import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;
import com.slamd.job.Job;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class provides an implementation of a SLAMD report generator that will
 * write the report information to a PDF document.
 *
 *
 * @author   Neil A. Wilson
 */
public class PDFReportGenerator
       implements ReportGenerator, PdfPageEvent
{
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
   * include the individual iterations of an optimizing job.
   */
  public static final String PARAM_INCLUDE_OPTIMIZING_ITERATIONS =
       "include_optimizing_iterations";



  /**
   * The name of the configuration parameter that indicates whether to view the
   * resulting PDF document in the browser (if supported) or save to disk.
   */
  public static final String PARAM_VIEW_IN_BROWSER = "view_in_browser";



  // The list used to hold the jobs that will be included in the generated
  // report.
  private ArrayList<Job> jobList;

  // The list used to hold the optimizing jobs that will be included in the
  // generated report.
  private ArrayList<OptimizingJob> optimizingJobList;

  // Indicates whether to include graphs in the generated results.
  private boolean includeGraphs;

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

  // Indicates whether to include information for individual iterations of an
  // optimizing job.
  private boolean includeOptimizingIterations;

  // Indicates whether to view the resulting PDF in a browser or save it to
  // disk.
  private boolean viewInBrowser;

  // The decimal format that will be used to format floating-point values.
  private DecimalFormat decimalFormat;

  // The set of jobs that will actually be included in the report.
  private Job[] reportJobs;

  // The set of optimizing jobs that will actually be included in the report.
  private OptimizingJob[] reportOptimizingJobs;

  // The date formatter that will be used when writing out dates.
  private SimpleDateFormat dateFormat;



  /**
   * Creates a new text report generator.
   */
  public PDFReportGenerator()
  {
    jobList           = new ArrayList<Job>();
    optimizingJobList = new ArrayList<OptimizingJob>();
    dateFormat        = new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    decimalFormat     = new DecimalFormat("0.000");

    includeScheduleConfig       = true;
    includeJobConfig            = true;
    includeStats                = true;
    includeMonitorStats         = true;
    includeGraphs               = true;
    requireStats                = true;
    includeOptimizingIterations = true;
    viewInBrowser               = false;
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
    return "PDF Report Generator";
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
    return new PDFReportGenerator();
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
    BooleanParameter requireStatsParameter =
         new BooleanParameter(PARAM_REQUIRE_STATS,
                              "Only Include Jobs with Statistics",
                              "Indicates whether to only include jobs that " +
                              "have statistics available.", requireStats);
    BooleanParameter includeOptimizingParameter =
         new BooleanParameter(PARAM_INCLUDE_OPTIMIZING_ITERATIONS,
                              "Include Optimizing Job Iterations",
                              "Indicates whether to include data for  " +
                              "optimizing job iterations.",
                              includeOptimizingIterations);
    BooleanParameter viewInBrowserParameter =
         new BooleanParameter(PARAM_VIEW_IN_BROWSER,
                              "View PDF in Browser",
                              "Indicates whether the resulting PDF file " +
                              "should be viewed in the browser or save it to " +
                              "disk.", viewInBrowser);

    Parameter[] parameters = new Parameter[]
    {
      includeScheduleConfigParameter,
      includeJobConfigParameter,
      includeStatsParameter,
      includeMonitorParameter,
      includeGraphsParameter,
      requireStatsParameter,
      includeOptimizingParameter,
      viewInBrowserParameter
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

    bp = reportParameters.getBooleanParameter(PARAM_VIEW_IN_BROWSER);
    if (bp != null)
    {
      viewInBrowser = bp.getBooleanValue();
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


    // Prepare to actually generate the report and send it to the user.
    HttpServletResponse response = requestInfo.getResponse();
    if (viewInBrowser)
    {
      response.setContentType("application/pdf");
    }
    else
    {
      response.setContentType("application/x-slamd-report-pdf");
    }
    response.addHeader("Content-Disposition",
                       "filename=\"slamd_data_report.pdf\"");


    try
    {
      // Create the PDF document and associate it with the response to send to
      // the client.
      Document  document = new Document(PageSize.LETTER);
      PdfWriter writer   = PdfWriter.getInstance(document,
                                                 response.getOutputStream());
      document.addTitle("SLAMD Generated Report");
      document.addCreationDate();
      document.addCreator("SLAMD Distributed Load Generator");
      writer.setPageEvent(this);

      // Open the document and add the table of contents.
      document.open();

      boolean needNewPage = writeContents(document);

      // Write the regular job information.
      for (int i=0; i < reportJobs.length; i++)
      {
        if (needNewPage)
        {
          document.newPage();
        }
        writeJob(document, reportJobs[i]);
        needNewPage = true;
      }

      // Write the optimizing job information.
      for (int i=0; i < reportOptimizingJobs.length; i++)
      {
        if (needNewPage)
        {
          document.newPage();
        }
        writeOptimizingJob(document, reportOptimizingJobs[i]);
        needNewPage = true;
      }

      // Close the document.
      document.close();
    }
    catch (Exception e)
    {
      // Not much we can do about this.
      e.printStackTrace();
      return;
    }
  }



  /**
   * Writes the table of contents to the document.
   *
   * @param  document  The document to which the contents are to be written.
   *
   * @return  <CODE>true</CODE> if the contents information was written to the
   *          PDF document, or <CODE>false</CODE> if not.
   *
   * @throws  DocumentException  If a problem occurs while writing the contents.
   */
  private boolean writeContents(Document document)
          throws DocumentException
  {
    // First, make sure that there is actually something to write.  If we're
    // only going to write information for a single job or optimizing job, then
    // there is no reason to have a contents section.
    if (((reportJobs.length == 1) && (reportOptimizingJobs.length == 0)) ||
        ((reportJobs.length == 0) && (reportOptimizingJobs.length == 1)))
    {
      return false;
    }


    if (reportJobs.length > 0)
    {
      // Write the job data header.
      Paragraph p = new Paragraph("Job Data",
                                  FontFactory.getFont(FontFactory.HELVETICA, 18,
                                                      Font.BOLD, Color.BLACK));
      document.add(p);


      // Create a table with the list of jobs.
      PdfPTable table = new PdfPTable(3);
      table.setWidthPercentage(100);
      writeTableHeaderCell(table, "Job ID");
      writeTableHeaderCell(table, "Description");
      writeTableHeaderCell(table, "Job Type");

      for (int i=0; i < reportJobs.length; i++)
      {
        Job job = reportJobs[i];
        Anchor anchor =
             new Anchor(job.getJobID(),
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.UNDERLINE, Color.BLUE));
        anchor.setReference('#' + job.getJobID());
        table.addCell(new PdfPCell(anchor));

        String descriptionStr = job.getJobDescription();
        if ((descriptionStr == null) || (descriptionStr.length() == 0))
        {
          descriptionStr = "(Not Specified)";
        }
        writeTableCell(table, descriptionStr);

        writeTableCell(table, job.getJobClass().getJobName());
      }
      document.add(table);


      // Write a blank line between the job data and optimizing job data.
      document.add(new Paragraph(" "));
    }


    if (reportOptimizingJobs.length > 0)
    {
      // Write the optimizing job data header.
      Paragraph p = new Paragraph("Optimizing Job Data",
                                  FontFactory.getFont(FontFactory.HELVETICA, 18,
                                                      Font.BOLD, Color.BLACK));
      document.add(p);


      // Create a table with the list of jobs.
      PdfPTable table = new PdfPTable(3);
      table.setWidthPercentage(100);
      writeTableHeaderCell(table, "Optimizing Job ID");
      writeTableHeaderCell(table, "Description");
      writeTableHeaderCell(table, "Job Type");

      for (int i=0; i < reportOptimizingJobs.length; i++)
      {
        OptimizingJob optimizingJob = reportOptimizingJobs[i];
        Anchor anchor =
             new Anchor(optimizingJob.getOptimizingJobID(),
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.UNDERLINE, Color.BLUE));
        anchor.setReference('#' + optimizingJob.getOptimizingJobID());
        table.addCell(new PdfPCell(anchor));

        String descriptionStr = optimizingJob.getDescription();
        if ((descriptionStr == null) || (descriptionStr.length() == 0))
        {
          descriptionStr = "(Not Specified)";
        }
        writeTableCell(table, descriptionStr);

        writeTableCell(table, optimizingJob.getJobClass().getJobName());
      }
      document.add(table);
    }

    return true;
  }



  /**
   * Writes information about the provided job to the document.
   *
   * @param  document  The document to which the job information should be
   *                   written.
   * @param  job       The job to include in the document.
   *
   * @throws  DocumentException  If a problem occurs while writing the contents.
   */
  private void writeJob(Document document, Job job)
          throws DocumentException
  {
    Anchor anchor = new Anchor("Job " + job.getJobID(),
                               FontFactory.getFont(FontFactory.HELVETICA, 18,
                                                   Font.BOLD, Color.BLACK));
    anchor.setName(job.getJobID());
    Paragraph p = new Paragraph(anchor);
    document.add(p);

    // Write the general information to the document.
    p = new Paragraph("General Information",
                      FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD,
                                          Color.BLACK));
    document.add(p);

    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setWidths(new int[] { 30, 70 });
    writeTableCell(table, "Job ID");
    writeTableCell(table, job.getJobID());

    String optimizingJobID = job.getOptimizingJobID();
    if ((optimizingJobID != null) && (optimizingJobID.length() > 0))
    {
      writeTableCell(table, "Optimizing Job ID");
      writeTableCell(table, optimizingJobID);
    }

    String descriptionStr = job.getJobDescription();
    if ((descriptionStr == null) || (descriptionStr.length() == 0))
    {
      descriptionStr = "(Not Specified)";
    }
    writeTableCell(table, "Job Description");
    writeTableCell(table, descriptionStr);

    writeTableCell(table, "Job Type");
    writeTableCell(table, job.getJobClassName());

    writeTableCell(table, "Job Class");
    writeTableCell(table, job.getJobClass().getClass().getName());

    writeTableCell(table, "Job State");
    writeTableCell(table, job.getJobStateString());
    document.add(table);


    // Write the schedule config if appropriate.
    if (includeScheduleConfig)
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("Schedule Information",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

      Date startTime = job.getStartTime();
      String startStr;
      if (startTime == null)
      {
        startStr = "(Not Available)";
      }
      else
      {
        startStr = dateFormat.format(startTime);
      }
      writeTableCell(table, "Scheduled Start Time");
      writeTableCell(table, startStr);

      Date stopTime = job.getStopTime();
      String stopStr;
      if (stopTime == null)
      {
        stopStr = "(Not Specified)";
      }
      else
      {
        stopStr = dateFormat.format(stopTime);
      }
      writeTableCell(table, "Scheduled Stop Time");
      writeTableCell(table, stopStr);

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
      writeTableCell(table, "Scheduled Duration");
      writeTableCell(table, durationStr);

      writeTableCell(table, "Number of Clients");
      writeTableCell(table, String.valueOf(job.getNumberOfClients()));

      String[] requestedClients = job.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        PdfPTable clientTable = new PdfPTable(1);
        for (int i=0; i < requestedClients.length; i++)
        {
          PdfPCell clientCell = new PdfPCell(new Phrase(requestedClients[i]));
          clientCell.setBorder(0);
          clientTable.addCell(clientCell);
        }

        writeTableCell(table, "Requested Clients");
        table.addCell(clientTable);
      }

      String[] monitorClients = job.getResourceMonitorClients();
      if ((monitorClients != null) && (monitorClients.length > 0))
      {
        PdfPTable clientTable = new PdfPTable(1);
        for (int i=0; i < monitorClients.length; i++)
        {
          PdfPCell clientCell = new PdfPCell(new Phrase(monitorClients[i]));
          clientCell.setBorder(0);
          clientTable.addCell(clientCell);
        }

        writeTableCell(table, "Resource Monitor Clients");
        table.addCell(clientTable);
      }

      writeTableCell(table, "Threads per Client");
      writeTableCell(table, String.valueOf(job.getThreadsPerClient()));

      writeTableCell(table, "Thread Startup Delay");
      writeTableCell(table, job.getThreadStartupDelay() + " milliseconds");

      writeTableCell(table, "Statistics Collection Interval");
      writeTableCell(table, job.getCollectionInterval() + " seconds");

      document.add(table);
    }


    // Write the job-specific parameter information if appropriate.
    if (includeJobConfig)
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("Parameter Information",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

      Parameter[] params = job.getParameterList().getParameters();
      for (int i=0; i < params.length; i++)
      {
        writeTableCell(table, params[i].getDisplayName());
        writeTableCell(table, params[i].getDisplayValue());
      }

      document.add(table);
    }


    // Write the statistical data if appropriate.
    if (includeStats && job.hasStats())
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("General Execution Data",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

      Date actualStartTime = job.getActualStartTime();
      String startStr;
      if (actualStartTime == null)
      {
        startStr = "(Not Available)";
      }
      else
      {
        startStr = dateFormat.format(actualStartTime);
      }
      writeTableCell(table, "Actual Start Time");
      writeTableCell(table, startStr);

      Date actualStopTime = job.getActualStopTime();
      String stopStr;
      if (actualStopTime == null)
      {
        stopStr = "(Not Available)";
      }
      else
      {
        stopStr = dateFormat.format(actualStopTime);
      }
      writeTableCell(table, "Actual Stop Time");
      writeTableCell(table, stopStr);

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
      writeTableCell(table, "Actual Duration");
      writeTableCell(table, durationStr);

      String[] clients = job.getStatTrackerClientIDs();
      if ((clients != null) && (clients.length > 0))
      {
        PdfPTable clientTable = new PdfPTable(1);
        for (int i=0; i < clients.length; i++)
        {
          PdfPCell clientCell = new PdfPCell(new Phrase(clients[i]));
          clientCell.setBorder(0);
          clientTable.addCell(clientCell);
        }

        writeTableCell(table, "Clients Used");
        table.addCell(clientTable);
      }

      document.add(table);

      String[] trackerNames = job.getStatTrackerNames();
      for (int i=0; i < trackerNames.length; i++)
      {
        StatTracker[] trackers = job.getStatTrackers(trackerNames[i]);
        if ((trackers != null) && (trackers.length > 0))
        {
          document.newPage();
          StatTracker tracker = trackers[0].newInstance();
          tracker.aggregate(trackers);

          document.add(new Paragraph(" "));
          document.add(new Paragraph(trackerNames[i],
                                FontFactory.getFont(FontFactory.HELVETICA, 12,
                                                    Font.BOLD, Color.BLACK)));

          String[] summaryNames  = tracker.getSummaryLabels();
          String[] summaryValues = tracker.getSummaryData();
          table = new PdfPTable(2);
          table.setWidthPercentage(100);
          table.setWidths(new int[] { 50, 50 });
          for (int j=0; j < summaryNames.length; j++)
          {
            writeTableCell(table, summaryNames[j]);
            writeTableCell(table, summaryValues[j]);
          }
          document.add(table);

          if (includeGraphs)
          {
            try
            {
              ParameterList params = tracker.getGraphParameterStubs(job);
              BufferedImage graphImage =
                   tracker.createGraph(job, Constants.DEFAULT_GRAPH_WIDTH,
                                       Constants.DEFAULT_GRAPH_HEIGHT, params);
              Image image = Image.getInstance(imageToByteArray(graphImage));
              image.scaleToFit(inchesToPoints(5.5), inchesToPoints(4.5));
              document.add(image);
            } catch (Exception e) {}
          }
        }
      }
    }


    // Write the resource monitor data if appropriate.
    if (includeMonitorStats && job.hasResourceStats())
    {
      String[] trackerNames = job.getResourceStatTrackerNames();
      for (int i=0; i < trackerNames.length; i++)
      {
        StatTracker[] trackers = job.getResourceStatTrackers(trackerNames[i]);
        if ((trackers != null) && (trackers.length > 0))
        {
          document.newPage();
          StatTracker tracker = trackers[0].newInstance();
          tracker.aggregate(trackers);

          document.add(new Paragraph(" "));
          document.add(new Paragraph(trackerNames[i],
                                FontFactory.getFont(FontFactory.HELVETICA, 12,
                                                    Font.BOLD, Color.BLACK)));

          String[] summaryNames  = tracker.getSummaryLabels();
          String[] summaryValues = tracker.getSummaryData();
          table = new PdfPTable(2);
          table.setWidthPercentage(100);
          table.setWidths(new int[] { 50, 50 });
          for (int j=0; j < summaryNames.length; j++)
          {
            writeTableCell(table, summaryNames[j]);
            writeTableCell(table, summaryValues[j]);
          }
          document.add(table);

          if (includeGraphs)
          {
            try
            {
              ParameterList params = tracker.getGraphParameterStubs(job);
              BufferedImage graphImage =
                   tracker.createMonitorGraph(job,
                        Constants.DEFAULT_GRAPH_WIDTH,
                        Constants.DEFAULT_MONITOR_GRAPH_HEIGHT, params);
              Image image = Image.getInstance(imageToByteArray(graphImage));
              image.scaleToFit(inchesToPoints(5.5), inchesToPoints(4.5));
              document.add(image);
            } catch (Exception e) {}
          }
        }
      }
    }
  }



  /**
   * Writes information about the provided optimizing job to the document.
   *
   * @param  document       The document to which the job information should be
   *                        written.
   * @param  optimizingJob  The optimizing job to include in the document.
   *
   * @throws  DocumentException  If a problem occurs while writing the contents.
   */
  private void writeOptimizingJob(Document document,
                                  OptimizingJob optimizingJob)
          throws DocumentException
  {
    Anchor anchor = new Anchor("Optimizing Job " +
                               optimizingJob.getOptimizingJobID(),
                               FontFactory.getFont(FontFactory.HELVETICA, 18,
                                                   Font.BOLD, Color.BLACK));
    anchor.setName(optimizingJob.getOptimizingJobID());
    Paragraph p = new Paragraph(anchor);
    document.add(p);

    // Write the general information to the document.
    p = new Paragraph("General Information",
                      FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD,
                                          Color.BLACK));
    document.add(p);

    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setWidths(new int[] { 30, 70 });
    writeTableCell(table, "Optimizing Job ID");
    writeTableCell(table, optimizingJob.getOptimizingJobID());

    writeTableCell(table, "Job Type");
    writeTableCell(table, optimizingJob.getJobClassName());

    String descriptionStr = optimizingJob.getDescription();
    if ((descriptionStr == null) || (descriptionStr.length() == 0))
    {
      descriptionStr = "(Not Specified)";
    }
    writeTableCell(table, "Base Description");
    writeTableCell(table, descriptionStr);

    writeTableCell(table, "Include Thread Count in Description");
    writeTableCell(table,
                   String.valueOf(optimizingJob.includeThreadsInDescription()));

    writeTableCell(table, "Job State");
    writeTableCell(table,
                   Constants.jobStateToString(optimizingJob.getJobState()));
    document.add(table);


    // Write the schedule config to the document if appropriate.
    if (includeScheduleConfig)
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("Schedule Information",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

      Date startTime = optimizingJob.getStartTime();
      String startStr;
      if (startTime == null)
      {
        startStr = "(Not Available)";
      }
      else
      {
        startStr = dateFormat.format(startTime);
      }
      writeTableCell(table, "Scheduled Start Time");
      writeTableCell(table, startStr);

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
      writeTableCell(table, "Job Duration");
      writeTableCell(table, durationStr);

      writeTableCell(table, "Delay Between Iterations");
      writeTableCell(table,
                     optimizingJob.getDelayBetweenIterations() + " seconds");

      writeTableCell(table, "Number of Clients");
      writeTableCell(table, String.valueOf(optimizingJob.getNumClients()));

      String[] requestedClients = optimizingJob.getRequestedClients();
      if ((requestedClients != null) && (requestedClients.length > 0))
      {
        PdfPTable clientTable = new PdfPTable(1);
        for (int i=0; i < requestedClients.length; i++)
        {
          PdfPCell clientCell = new PdfPCell(new Phrase(requestedClients[i]));
          clientCell.setBorder(0);
          clientTable.addCell(clientCell);
        }

        writeTableCell(table, "Requested Clients");
        table.addCell(clientTable);
      }

      String[] monitorClients = optimizingJob.getResourceMonitorClients();
      if ((monitorClients != null) && (monitorClients.length > 0))
      {
        PdfPTable clientTable = new PdfPTable(1);
        for (int i=0; i < monitorClients.length; i++)
        {
          PdfPCell clientCell = new PdfPCell(new Phrase(monitorClients[i]));
          clientCell.setBorder(0);
          clientTable.addCell(clientCell);
        }

        writeTableCell(table, "Resource Monitor Clients");
        table.addCell(clientTable);
      }

      writeTableCell(table, "Minimum Number of Threads");
      writeTableCell(table, String.valueOf(optimizingJob.getMinThreads()));

      int maxThreads = optimizingJob.getMaxThreads();
      String maxThreadsStr;
      if (maxThreads > 0)
      {
        maxThreadsStr = String.valueOf(maxThreads);
      }
      else
      {
        maxThreadsStr = "(Not Specified)";
      }
      writeTableCell(table, "Maximum Number of Threads");
      writeTableCell(table, maxThreadsStr);

      writeTableCell(table, "Thread Increment Between Iterations");
      writeTableCell(table, String.valueOf(optimizingJob.getThreadIncrement()));

      writeTableCell(table, "Statistics Collection Interval");
      writeTableCell(table,
                     optimizingJob.getCollectionInterval() + " seconds");
      document.add(table);
    }


    // Get the optimization algorithm used.
    OptimizationAlgorithm optimizationAlgorithm =
         optimizingJob.getOptimizationAlgorithm();
    ParameterList paramList =
         optimizationAlgorithm.getOptimizationAlgorithmParameters();
    Parameter[] optimizationParams = paramList.getParameters();


    // Write the optimizing config to the document if appropriate.
    if (includeScheduleConfig)
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("Optimization Settings",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

      for (int i=0; i < optimizationParams.length; i++)
      {
        writeTableCell(table, optimizationParams[i].getDisplayName());
        writeTableCell(table, optimizationParams[i].getDisplayValue());
      }

      writeTableCell(table, "Maximum Consecutive Non-Improving Iterations");
      writeTableCell(table, String.valueOf(optimizingJob.getMaxNonImproving()));

      writeTableCell(table, "Re-Run Best Iteration");
      writeTableCell(table, String.valueOf(optimizingJob.reRunBestIteration()));

      int reRunDuration = optimizingJob.getReRunDuration();
      String durationStr;
      if (reRunDuration > 0)
      {
        durationStr = reRunDuration + " seconds";
      }
      else
      {
        durationStr = "(Not Specified)";
      }
      writeTableCell(table, "Re-Run Duration");
      writeTableCell(table, durationStr);

      document.add(table);
    }


    // Write the job-specific config to the document if appropriate.
    if (includeJobConfig)
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("Parameter Information",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

      Parameter[] params = optimizingJob.getParameters().getParameters();
      for (int i=0; i < params.length; i++)
      {
        writeTableCell(table, params[i].getDisplayName());
        writeTableCell(table, params[i].getDisplayValue());
      }

      document.add(table);
    }


    // Write the statistical data to the document if appropriate.
    if (includeStats && optimizingJob.hasStats())
    {
      document.add(new Paragraph(" "));
      p = new Paragraph("Execution Data",
                        FontFactory.getFont(FontFactory.HELVETICA, 12,
                                            Font.BOLD, Color.BLACK));
      document.add(p);

      table = new PdfPTable(2);
      table.setWidthPercentage(100);
      table.setWidths(new int[] { 30, 70 });

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
      writeTableCell(table, "Actual Start Time");
      writeTableCell(table, startTimeStr);

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
      writeTableCell(table, "Actual Stop Time");
      writeTableCell(table, stopTimeStr);

      Job[] iterations = optimizingJob.getAssociatedJobs();
      if ((iterations != null) && (iterations.length > 0))
      {
        writeTableCell(table, "Job Iterations Completed");
        writeTableCell(table, String.valueOf(iterations.length));

        int optimalThreadCount = optimizingJob.getOptimalThreadCount();
        String threadStr;
        if (optimalThreadCount > 0)
        {
          threadStr = String.valueOf(optimalThreadCount);
        }
        else
        {
          threadStr = "(Not Available)";
        }
        writeTableCell(table, "Optimal Thread Count");
        writeTableCell(table, threadStr);

        double optimalValue = optimizingJob.getOptimalValue();
        String valueStr;
        if (optimalThreadCount > 0)
        {
          valueStr = decimalFormat.format(optimalValue);
        }
        else
        {
          valueStr = "(Not Available)";
        }
        writeTableCell(table, "Optimal Value");
        writeTableCell(table, valueStr);

        String optimalID = optimizingJob.getOptimalJobID();
        writeTableCell(table, "Optimal Job Iteration");
        if ((optimalID == null) || (optimalID.length() == 0))
        {
          writeTableCell(table, "(Not Available)");
        }
        else if (includeOptimizingIterations)
        {
          anchor = new Anchor(optimalID,
                              FontFactory.getFont(FontFactory.HELVETICA, 12,
                                                  Font.UNDERLINE, Color.BLUE));
          anchor.setReference('#' + optimalID);
          table.addCell(new PdfPCell(anchor));
        }
        else
        {
          writeTableCell(table, optimalID);
        }
      }

      Job reRunIteration = optimizingJob.getReRunIteration();
      if (reRunIteration != null)
      {
        writeTableCell(table, "Re-Run Iteration");
        if (includeOptimizingIterations)
        {
          anchor = new Anchor(reRunIteration.getJobID(),
                              FontFactory.getFont(FontFactory.HELVETICA, 12,
                                                  Font.UNDERLINE, Color.BLUE));
          anchor.setReference('#' + reRunIteration.getJobID());
          table.addCell(new PdfPCell(anchor));
        }
        else
        {
          writeTableCell(table, reRunIteration.getJobID());
        }


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

        writeTableCell(table, "Re-Run Iteration Value");
        writeTableCell(table, valueStr);
      }

      document.add(table);

      if (includeOptimizingIterations && (iterations != null) &&
          (iterations.length > 0))
      {
        document.add(new Paragraph(" "));
        p = new Paragraph("Job Iterations",
                          FontFactory.getFont(FontFactory.HELVETICA, 12,
                                              Font.BOLD, Color.BLACK));
        document.add(p);

        table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 50, 50 });

        for (int i=0; i < iterations.length; i++)
        {
          String valueStr;
          try
          {
            double iterationValue =
                 optimizationAlgorithm.getIterationOptimizationValue(
                                            iterations[i]);
            valueStr = decimalFormat.format(iterationValue);
          }
          catch (Exception e)
          {
            valueStr = "N/A";
          }

          anchor = new Anchor(iterations[i].getJobID(),
                              FontFactory.getFont(FontFactory.HELVETICA, 12,
                                                  Font.UNDERLINE,
                                                  Color.BLUE));
          anchor.setReference('#' + iterations[i].getJobID());
          table.addCell(new PdfPCell(anchor));
          writeTableCell(table, valueStr);
        }

        document.add(table);
      }

      if (includeGraphs && (iterations != null) && (iterations.length > 0))
      {
        String[] statNames = iterations[0].getStatTrackerNames();
        for (int j=0; j < statNames.length; j++)
        {
          StatTracker[] trackers =
               iterations[0].getStatTrackers(statNames[j]);
          if ((trackers != null) && (trackers.length > 0))
          {
            StatTracker tracker = trackers[0].newInstance();
            tracker.aggregate(trackers);

            try
            {
              document.newPage();
              ParameterList params = tracker.getGraphParameterStubs(iterations);
              BufferedImage graphImage =
                   tracker.createGraph(iterations,
                                       Constants.DEFAULT_GRAPH_WIDTH,
                                       Constants.DEFAULT_GRAPH_HEIGHT, params);
              Image image = Image.getInstance(imageToByteArray(graphImage));
              image.scaleToFit(inchesToPoints(5.5), inchesToPoints(4.5));
              document.add(image);
            } catch (Exception e) {}
          }
        }
      }

      if (includeOptimizingIterations && (iterations != null) &&
          (iterations.length > 0))
      {
        for (int i=0; i < iterations.length; i++)
        {
          document.newPage();
          writeJob(document, iterations[i]);
        }
      }
      if (includeOptimizingIterations && (reRunIteration != null))
      {
        document.newPage();
        writeJob(document, reRunIteration);
      }
    }
  }



  /**
   * Writes the specified text to the provided table as a header cell.
   *
   * @param  table  The table to which the header cell should be written.
   * @param  text   The text to write to the header cell.
   */
  private void writeTableHeaderCell(PdfPTable table, String text)
  {
    Phrase phrase = new Phrase(text,
                               FontFactory.getFont(FontFactory.HELVETICA, 12,
                                                   Font.BOLD, Color.BLACK));
    table.addCell(new PdfPCell(phrase));
  }



  /**
   * Writes the specified text to the provided table as a normal cell.
   *
   * @param  table  The table to which the cell should be written.
   * @param  text   The text to write to the cell.
   */
  private void writeTableCell(PdfPTable table, String text)
  {
    table.addCell(new PdfPCell(new Phrase(text)));
  }



  /**
   * Converts the specified number of inches into points (there are 72 points
   * per inch).  The number of inches provided does not need to be an integer.
   *
   * @param  numInches  The number of inches to be converted to points.
   *
   * @return  The number of points corresponding to the provided number of
   *          inches.
   */
  public static int inchesToPoints(double numInches)
  {
    return (int) Math.round(numInches * 72);
  }



  /**
   * Converts the provided image to a byte array containing data for the PNG
   * representation of the image.
   *
   * @param  image  The image to be converted to a byte array.
   *
   * @return  The byte array containing the image data.
   *
   * @throws  IOException  If a problem occurs while creating the image array.
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



  /**
   * Performs the appropriate action necessary when starting a new page.  In
   * this case, we will write the SLAMD header to the top of the page.
   *
   * @param  writer    The writer used to write the PDF document.
   * @param  document  The PDF document being written.
   */
  public void onStartPage(PdfWriter writer, Document document)
  {
    try
    {
      PdfPTable table = new PdfPTable(3);
      table.setWidthPercentage(100);

      PdfPCell blueCell = new PdfPCell(new Phrase(" \n "));
      blueCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
      blueCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
      blueCell.setBackgroundColor(new Color(0x59, 0x4F, 0xBF));
      blueCell.setBorderWidth(inchesToPoints(1.0/16));
      blueCell.setBorderColor(new Color(0xFF, 0xFF, 0xFF));
      blueCell.setPadding(inchesToPoints(1.0/16));
      table.addCell(blueCell);

      Phrase titlePhrase =
           new Phrase("SLAMD Generated Report",
                      FontFactory.getFont(FontFactory.HELVETICA, 12,
                                          Font.BOLD,
                                          new Color(0x59, 0x4F, 0xBF)));
      PdfPCell yellowCell = new PdfPCell(titlePhrase);
      yellowCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
      yellowCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
      yellowCell.setBackgroundColor(new Color(0xFB, 0xE2, 0x49));
      yellowCell.setBorderWidth(inchesToPoints(1.0/16));
      yellowCell.setBorderColor(new Color(0xFF, 0xFF, 0xFF));
      yellowCell.setPadding(inchesToPoints(1.0/16));
      table.addCell(yellowCell);

      Phrase versionPhrase =
           new Phrase("Version " + DynamicConstants.SLAMD_VERSION,
                      FontFactory.getFont(FontFactory.HELVETICA, 12,
                                          Font.BOLD,
                                          new Color(0xFF, 0xFF, 0xFF)));
      PdfPCell redCell = new PdfPCell(versionPhrase);
      redCell.setHorizontalAlignment(Cell.ALIGN_RIGHT);
      redCell.setVerticalAlignment(Cell.ALIGN_MIDDLE);
      redCell.setBackgroundColor(new Color(0xD1, 0x21, 0x24));
      redCell.setBorderWidth(inchesToPoints(1.0/16));
      redCell.setBorderColor(new Color(0xFF, 0xFF, 0xFF));
      redCell.setPadding(inchesToPoints(1.0/16));
      table.addCell(redCell);

      document.add(table);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }



  /**
   * Performs the appropriate action necessary when ending a page.  In this
   * case, no action is required.
   *
   * @param  writer    The writer used to write the PDF document.
   * @param  document  The PDF document being written.
   */
  public void onEndPage(PdfWriter writer, Document document)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when opening a document.  In this
   * case, no action is required.
   *
   * @param  writer    The writer used to write the PDF document.
   * @param  document  The PDF document being written.
   */
  public void onOpenDocument(PdfWriter writer, Document document)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when opening a document.  In this
   * case, no action is required.
   *
   * @param  writer    The writer used to write the PDF document.
   * @param  document  The PDF document being written.
   */
  public void onCloseDocument(PdfWriter writer, Document document)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when starting a new paragraph.
   * In this case, no action is required.
   *
   * @param  writer        The writer used to write the PDF document.
   * @param  document      The PDF document being written.
   * @param  paragraphPos  The position of the beginning of the paragraph.
   */
  public void onParagraph(PdfWriter writer, Document document,
                          float paragraphPos)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when ending a paragraph.  In this
   * case, no action is required.
   *
   * @param  writer           The writer used to write the PDF document.
   * @param  document         The PDF document being written.
   * @param  paragraphEndPos  The position of the end of the paragraph.
   */
  public void onParagraphEnd(PdfWriter writer, Document document,
                             float paragraphEndPos)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when starting a new chapter.  In
   * this case, no action is required.
   *
   * @param  writer      The writer used to write the PDF document.
   * @param  document    The PDF document being written.
   * @param  chapterPos  The position at which the beginning of the chapter will
   *                     be written.
   * @param  title       The title to use for the chapter.
   */
  public void onChapter(PdfWriter writer, Document document, float chapterPos,
                        Paragraph title)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when ending a chapter.  In this
   * case, no action is required.
   *
   * @param  writer         The writer used to write the PDF document.
   * @param  document       The PDF document being written.
   * @param  chapterEndPos  The position at which the end of the chapter will be
   *                        written.
   */
  public void onChapterEnd(PdfWriter writer, Document document,
                           float chapterEndPos)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when beginning a new section.  In
   * this case, no action is required.
   *
   * @param  writer        The writer used to write the PDF document.
   * @param  document      The PDF document being written.
   * @param  sectionPos    The position at which the beginning of the section
   *                       will be written.
   * @param  depth         The depth for the section.
   * @param  sectionTitle  The title to use for the section.
   */
  public void onSection(PdfWriter writer, Document document, float sectionPos,
                        int depth, Paragraph sectionTitle)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when ending a section.  In this
   * case, no action is required.
   *
   * @param  writer         The writer used to write the PDF document.
   * @param  document       The PDF document being written.
   * @param  sectionEndPos  The position at which the end of the section will be
   *                        written.
   */
  public void onSectionEnd(PdfWriter writer, Document document,
                           float sectionEndPos)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }



  /**
   * Performs the appropriate action necessary when writing a generic tag.  In
   * this case, no action is required.
   *
   * @param  writer     The writer used to write the PDF document.
   * @param  document   The PDF document being written.
   * @param  rectangle  The rectangle containing the chunk with the generic tag.
   * @param  text       The text of the tag.
   */
  public void onGenericTag(PdfWriter writer, Document document,
                           Rectangle rectangle, String text)
  {
    // No action necessary, but this method is required by the PdfPageEvent
    // interface.
  }
}

