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



import com.slamd.admin.RequestInfo;
import com.slamd.job.Job;
import com.slamd.job.OptimizingJob;
import com.slamd.parameter.ParameterList;



/**
 * This class defines the set of methods that must be implemented by any class
 * that provides a mechanism for generating reports of SLAMD data.
 *
 *
 * @author   Neil A. Wilson
 */
public interface ReportGenerator
{
  /**
   * Retrieves a user-friendly name that can be used to indicate the type of
   * report that will be generated.
   *
   * @return  The user-friendly name that can be used to indicate the type of
   *          report that will be generated.
   */
  public String getReportGeneratorName();



  /**
   * Retrieves a new instance of this report generator initialized with the
   * default configuration.
   *
   * @return  A new instance of this report generator initialized with the
   *          default configuration.
   */
  public ReportGenerator newInstance();



  /**
   * Retrieves a set of parameters that can be used to allow the user to
   * configure the way that the report is generated.
   *
   * @return  A set of parameters that can be used to allow the user to
   *          configure the way that the report is generated.
   */
  public ParameterList getReportParameterStubs();



  /**
   * Initializes this reporter based on the parameters customized by the end
   * user.
   *
   * @param  reportParameters  The set of parameters provided by the end user
   *                           that should be used to customize the report.
   */
  public void initializeReporter(ParameterList reportParameters);



  /**
   * Indicates that information about the provided job should be included in the
   * report.
   *
   * @param  job  The job about which to include information in the report.
   */
  public void addJobReport(Job job);



  /**
   * Indicates that information about the provided optimizing job should be
   * included in the report.
   *
   * @param  optimizingJob  The optimizing job about which to include
   *                        information in the report.
   */
  public void addOptimizingJobReport(OptimizingJob optimizingJob);



  /**
   * Generates the report and sends it to the user over the provided servlet
   * response.
   *
   * @param  requestInfo  State information about the request being processed.
   */
  public void generateReport(RequestInfo requestInfo);
}

