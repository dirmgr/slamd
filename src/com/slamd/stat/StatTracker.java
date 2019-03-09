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
package com.slamd.stat;



import java.awt.image.BufferedImage;

import com.slamd.common.SLAMDException;
import com.slamd.job.Job;
import com.slamd.parameter.ParameterList;



/**
 * This interface defines all the methods that must be implemented by a class
 * intended to collect and manage statistical information during the course of
 * job processing.  Note that this class does not place any limitations on the
 * kind of data that it manages or the way that it collects it.  Rather, it
 * simply defines a common set of methods that allows the SLAMD server to deal
 * with it in a generic manner.
 *
 * Note that stat trackers are not threadsafe and therefore a stat tracker
 * should only be used by a single thread.  If you need to use multiple threads,
 * then have a separate stat tracker per thread and use the
 * <CODE>aggregate</CODE> method to combine the data from those trackers into a
 * single tracker.
 *
 *
 * @author   Neil A. Wilson
 */
public interface StatTracker
{
  /**
   * Creates a new instance of this stat tracker.  The new instance should have
   * the same type, display name, client ID, thread ID, and collection interval
   * as the stat tracker used to create it.
   *
   * @return  The new instance of this stat tracker.
   */
  public StatTracker newInstance();



  /**
   * Indicates that the stat tracker is to start maintaining statistics and that
   * it should start its internal timer.
   */
  public void startTracker();



  /**
   * Indicates that the stat tracker that there will not be any more statistics
   * collection done and that it should stop its internal timer.
   */
  public void stopTracker();



  /**
   * Indicates whether this stat tracker has been started, regardless of whether
   * it is currently running.
   *
   * @return  <CODE>true</CODE> if this stat tracker has been started, or
   *          <CODE>false</CODE> if it has not yet been started.
   */
  public boolean hasBeenStarted();



  /**
   * Indicates whether this stat tracker is currently running.
   *
   * @return  <CODE>true</CODE> if this stat tracker is currently running, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isRunning();



  /**
   * Indicates that the stat tracker should enable real-time statistics
   * collection.  Note that some stat trackers may not support real-time
   * statistics collection, in which case this method may be ignored.
   *
   * @param  statReporter  The stat-reporter that should be used to report
   *                       real-time statistics to the SLAMD server.
   * @param  jobID         The job ID of the job that will be reporting the
   *                       data.
   */
  public void enableRealTimeStats(RealTimeStatReporter statReporter,
                                  String jobID);



  /**
   * Retrieves the client ID of the client that used this stat tracker.
   *
   * @return  The client ID of the client that used this stat tracker.
   */
  public String getClientID();



  /**
   * Specifies the client ID of the client that used this stat tracker.  Note
   * that this should only be used when creating a new stat tracker based on
   * encoded data and not when using it to collect statistics.
   *
   * @param  clientID  The client ID of the client that used this stat tracker.
   */
  public void setClientID(String clientID);



  /**
   * Retrieves the thread ID of the client thread that used this stat tracker.
   *
   * @return  The thread ID of the client thread that used this stat tracker.
   */
  public String getThreadID();



  /**
   * Specifies the thread ID of the client thread that used this stat tracker.
   * Note that this should only be used when creating a new stat tracker based
   * on encoded data and not when using it to collect statistics.
   *
   * @param  threadID  The thread ID of the client thread that used this stat
   *                   tracker.
   */
  public void setThreadID(String threadID);



  /**
   * Retrieves the user-friendly name associated with this stat tracker.
   *
   * @return  The user-friendly name associated with this stat tracker.
   */
  public String getDisplayName();



  /**
   * Specifies the display name for this stat tracker.  Note that this should
   * only be used when creating a new stat tracker based on encoded data and not
   * when using it to collect statistics.
   *
   * @param  displayName The display name for this stat tracker.
   */
  public void setDisplayName(String displayName);



  /**
   * Retrieves the collection interval (in seconds) that will be used for this
   * stat tracker.
   *
   * @return  The collection interval (in seconds) that will be used for this
   *          stat tracker.
   */
  public int getCollectionInterval();



  /**
   * Specifies the collection interval for this stat tracker.  Note that this
   * should only be used when creating a new stat tracker based on encoded data
   * and not when using it to collect statistics.
   *
   * @param  collectionInterval  The collection interval in seconds to use for
   *                             this stat tracker.
   */
  public void setCollectionInterval(int collectionInterval);



  /**
   * Retrieves the total length of time in seconds that this stat tracker was
   * capturing statistics.
   *
   * @return  The total length of time in seconds that this stat tracker was
   *          capturing statistics.
   */
  public int getDuration();



  /**
   * Specifies the duration for this stat tracker.  Note that this should only
   * be used when creating a new stat tracker based on encoded data and not when
   * using it to collect statistics.
   *
   * @param  duration  The duration for this stat tracker.
   */
  public void setDuration(int duration);



  /**
   * Indicates whether the user may search for jobs with statistics collected by
   * this stat tracker.  The search will be "greater than" and "less than" some
   * user-specified value.
   *
   * @return  <CODE>true</CODE> if statistics collected by this stat tracker
   *          should be searchable, or <CODE>false</CODE> if not.
   */
  public boolean isSearchable();



  /**
   * Indicates whether the value associated with this stat tracker is greater
   * than or equal to the provided value.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what exactly
   * "the value of this stat tracker" means will be left up to those stat
   * trackers that are searchable.
   *
   * @param  value  The value against which the value of this stat tracker is to
   *                be compared.
   *
   * @return  <CODE>true</CODE> if the value of this stat tracker is greater
   *          than or equal to the provided value, or <CODE>false</CODE> if not.
   */
  public boolean isAtLeast(double value);



  /**
   * Indicates whether the value associated with this stat tracker is less than
   * or equal to the provided value.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what exactly
   * "the value of this stat tracker" means will be left up to those stat
   * trackers that are searchable.
   *
   * @param  value  The value against which the value of this stat tracker is to
   *                be compared.
   *
   * @return  <CODE>true</CODE> if the value of this stat tracker is less than
   *          or equal to the provided value, or <CODE>false</CODE> if not.
   */
  public boolean isAtMost(double value);



  /**
   * Retrieves the number of intervals for which data is available for this stat
   * tracker.
   *
   * @return  The number of intervals for which data is available for this stat
   *          tracker.
   */
  public int getNumIntervals();



  /**
   * Aggregates the information collected by the provided set of stat trackers
   * into a single tracker that represents the information gathered from the
   * entire set of data.  All of the stat trackers in the provided array must be
   * of the same type as the instance into which the information will be
   * aggregated.
   *
   * @param  trackers  The set of stat trackers whose data is to be aggregated.
   */
  public void aggregate(StatTracker[] trackers);



  /**
   * Retrieves brief one-line summary string with cumulative information about
   * this stat tracker.
   *
   * @return  A brief one-line summary string containing cumulative information
   *          about this stat tracker.
   */
  public String getSummaryString();



  /**
   * Retrieves the value associated with this stat tracker.  This is only
   * applicable if <CODE>isSearchable</CODE> returns <CODE>true</CODE>, and what
   * exactly "the value associated with this stat tracker" means will be left up
   * to those stat trackers that are searchable.
   *
   * @return  The value associated with this stat tracker.
   */
  public double getSummaryValue();



  /**
   * Retrieves a detailed (potentially multi-line) string with verbose
   * information about the data collected by this stat tracker.
   *
   * @return  A detailed string with verbose information about the data
   *          collected by this stat tracker.
   */
  public String getDetailString();



  /**
   * Retrieves a version of the summary information for this stat tracker
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the summary data for this stat tracker.
   */
  public String getSummaryHTML();



  /**
   * Retrieves a version of the verbose information for this stat tracker,
   * formatted for display in an HTML document.
   *
   * @return  An HTML version of the verbose data for this stat tracker.
   */
  public String getDetailHTML();



  /**
   * Retrieves a string array with the labels corresponding to the values
   * returned from the <CODE>getSummaryData</CODE> method.
   *
   * @return  A string array with the labels corresponding to the values
   *          returned from the <CODE>getSummaryData</CODE> method.
   */
  public String[] getSummaryLabels();



  /**
   * Retrieves the summary string data for this stat tracker as separate values.
   *
   * @return  The summary string data for this stat tracker as separate values.
   */
  public String[] getSummaryData();



  /**
   * Retrieves the raw data associated with this stat tracker in a form that
   * can be easily converted for export to CSV, tab-delimited text, or some
   * other format for use in an external application.  There should be one value
   * per "cell".
   *
   *
   * @param  includeLabels  Indicates whether the information being exported
   *                        should contain labels.
   *
   * @return  The raw data associated with this stat tracker in a form that can
   *           be exported to some external form.
   */
  public String[][] getDataForExport(boolean includeLabels);



  /**
   * Encodes the data collected by this tracker into a byte array that may be
   * transferred over the network or written out to persistent storage.
   *
   * @return  The data collected by this tracker encoded as a byte array.
   */
  public byte[] encode();



  /**
   * Decodes the provided data and uses it as the data for this stat tracker.
   *
   * @param  encodedData   The encoded version of the data to use for this
   *                       stat tracker.
   *
   * @throws  SLAMDException  If the provided data cannot be decoded and used
   *                          with this stat tracker.
   */
  public void decode(byte[] encodedData)
    throws SLAMDException;



  /**
   * Retrieves the set of parameters that may be specified to customize the
   * graph that is generated based on the statistical information in the stat
   * trackers.
   *
   * @param  job  The job containing the statistical information to be graphed.
   *
   * @return  The set of parameters that may be used to customize the graph that
   *          is generated.
   */
  public ParameterList getGraphParameterStubs(Job job);



  /**
   * Retrieves the set of parameters that may be specified to customize the
   * graph that is generated based on the resource monitor information in the
   * stat trackers.
   *
   * @param  job  The job containing the resource monitor information to be
   *              graphed.
   *
   * @return  The set of parameters that may be used to customize the graph that
   *          is generated.
   */
  public ParameterList getMonitorGraphParameterStubs(Job job);



  /**
   * Retrieves the set of parameters that may be specified to customize the
   * graph that is generated based on the statistical information in the stat
   * trackers.
   *
   * @param  jobs  The job containing the statistical information to be compared
   *               and graphed.
   *
   * @return  The set of parameters that may be used to customize the graph that
   *          is generated.
   */
  public ParameterList getGraphParameterStubs(Job[] jobs);



  /**
   * Retrieves the data that represents the points in a line graph for this
   * stat tracker.    This is only applicable if <CODE>isSearchable</CODE>
   * returns <CODE>true</CODE>.
   *
   * @return  The data that represents the points in a line graph for this stat
   *          tracker, or <CODE>null</CODE> if that data is not available.
   */
  public double[] getGraphData();



  /**
   * Retrieves the label that should be included along the vertical axis in a
   * line graph for this stat tracker.  This is only applicable if
   * <CODE>isSearchable</CODE> returns <CODE>true</CODE>.
   *
   * @return  The label that should be included along the vertical axis in a
   *          line graph for this stat tracker, or <CODE>null</CODE> if that
   *          data is not applicable.
   */
  public String getAxisLabel();



  /**
   * Creates a graph that visually depicts the information in the provided set
   * of stat trackers.  The provided stat trackers must be the of the same type
   * as this stat tracker.
   *
   * @param  job              The job containing the statistical information to
   *                          be graphed.
   * @param  width            The width in pixels of the graph to create.
   * @param  height           The height in pixels of the graph to create.
   * @param  graphParameters  The set of parameters that may be used to
   *                          customize the graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createGraph(Job job, int width, int height,
                                   ParameterList graphParameters);



  /**
   * Creates a graph that visually depicts the information collected by resource
   * monitors associated with the provided job.
   *
   * @param  job         The job containing the statistical information to be
   *                     graphed.
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   * @param  parameters  The set of parameters that may be used to customize
   *                     the graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createMonitorGraph(Job job, int width, int height,
                                          ParameterList parameters);



  /**
   * Creates a graph that visually depicts the information in the provided set
   * of stat trackers.  The provided stat trackers must be the of the same type
   * as this stat tracker.
   *
   * @param  jobs        The job containing the statistical information to be
   *                     compared and graphed.
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   * @param  parameters  The set of parameters that may be used to customize
   *                     the graph that is generated.
   *
   * @return  The graph created from the statistical information in the provided
   *          job.
   */
  public BufferedImage createGraph(Job[] jobs, int width, int height,
                                   ParameterList parameters);



  /**
   * Creates a graph that visually depicts the information in this stat tracker
   * using all the default settings.
   *
   * @param  width       The width in pixels of the graph to create.
   * @param  height      The height in pixels of the graph to create.
   *
   * @return  The graph created from this stat tracker.
   */
  public BufferedImage createGraph(int width, int height);
}

