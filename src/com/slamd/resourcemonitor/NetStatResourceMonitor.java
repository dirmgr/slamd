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
 * Contributor(s):  Neil A. Wilson, Geoffrey Said
 */
package com.slamd.resourcemonitor;



import java.util.*;
import com.slamd.common.*;
import com.slamd.job.*;
import com.slamd.stat.*;
import com.slamd.resourcemonitor.netstat.NetStatCollector;
import com.slamd.resourcemonitor.netstat.InterfaceStatistics;
import com.slamd.resourcemonitor.netstat.SingleInterfaceStatistics;
import com.slamd.resourcemonitor.netstat.AggregateInterfaceStatistics;


/**
 * This class defines a SLAMD resource monitor that uses command-line utilities
 * to monitor network traffic volumes.
 *
 *
 * @author   Neil A. Wilson
 */
public class NetStatResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker used to keep track of the number of
   * bytes received.
   */
  public static final String STAT_TRACKER_BYTES_RECEIVED =
       "Network Bytes Received";



  /**
   * The display name of the stat tracker used to keep track of the number of
   * bytes transmitted.
   */
  public static final String STAT_TRACKER_BYTES_TRANSMITTED =
       "Network Bytes Transmitted";

  /**
   * The configuration property name used to specify the list of monitored
   * network interfaces.
   */
  public static final String PROPERTY_MONITOR_INTERFACES =
      "monitor_interfaces";

  /**
   * The configuration property name used to specify if the network interface
   * statistics should be aggregated.
   */
  public static final String PROPERTY_MONITOR_AGGREGATE_STATISTICS =
      "monitor_aggregate_statistics";

  // A flag that indicates whether we will try to report real-time statistics.
  boolean enableRealTimeStats;

  // The stat trackers used to keep track of the amount of traffic transferred.
  private InterfaceStatistics[] interfaceStatistics;

  // The frequency that we should use when collecting statistics.
  int collectionInterval;

  // Names of the monitored interfaces.
  private String[] monitoredInterfaces;

  // Flag to indicate if the network interface statistics are aggregated.
  private boolean aggregateStats;

  /**
   * Performs any initialization specific to this resource monitor.
   *
   * @throws  SLAMDException  If a problem occurs while performing the
   *                          initialization.
   */
  @Override()
  public void initializeMonitor()
         throws SLAMDException
  {
    String monitorInterfacesProp = getProperty(PROPERTY_MONITOR_INTERFACES);
    if (monitorInterfacesProp != null && monitorInterfacesProp.length() > 0)
    {
      monitoredInterfaces =
          monitorInterfacesProp.replaceAll("[ \t]*", "").split(",");

    }

    aggregateStats = getProperty(PROPERTY_MONITOR_AGGREGATE_STATISTICS, false);
    if ((monitoredInterfaces == null) || (monitoredInterfaces.length == 0))
    {
      // We will always aggregate stats if no specific interfaces are defined.
      monitoredInterfaces = null;
      aggregateStats = true;
    }
  }



  /**
   * Indicates whether the current client system is supported for this resource
   * monitor.
   *
   * @return  <CODE>true</CODE> if the current client system is supported for
   *          this resource monitor, or <CODE>false</CODE> if not.
   */
  @Override()
  public boolean clientSupported()
  {
    int osType = getClientOS();

    switch (osType)
    {
      case OS_TYPE_SOLARIS:
      case OS_TYPE_LINUX:
      case OS_TYPE_HPUX:
      case OS_TYPE_AIX:
      case OS_TYPE_WINDOWS:
        return true;
      default:
        return false;
    }
  }



  /**
   * Creates a new instance of this resource monitor thread.  Note that the
   * <CODE>initialize()</CODE> method should have been called on the new
   * instance before it is returned.
   *
   * @return  A new instance of this resource monitor thread.
   *
   * @throws  SLAMDException  If a problem occurs while creating or initializing
   *                          the resource monitor.
   */
  @Override()
  public ResourceMonitor newInstance()
         throws SLAMDException
  {
    NetStatResourceMonitor monitor = new NetStatResourceMonitor();
    monitor.initialize(getMonitorClient(), getMonitorProperties());

    return monitor;
  }




  /**
   * Initializes the stat trackers maintained by this resource monitor.
   *
   * @param  clientID            The client ID to use for the stubs.
   * @param  threadID            The thread ID to use for the stubs.
   * @param  collectionInterval  The collection interval to use for the stubs.
   */
  @Override()
  public void initializeStatistics(String clientID, String threadID,
                                   int collectionInterval)
  {
    this.collectionInterval = collectionInterval;

    if (aggregateStats)
    {
      interfaceStatistics = new InterfaceStatistics[1];
      interfaceStatistics[0]=
          new AggregateInterfaceStatistics(
              new LongValueTracker(clientID, threadID, clientID + ' ' +
                  STAT_TRACKER_BYTES_RECEIVED, collectionInterval
              ),
              new LongValueTracker(clientID, threadID, clientID + ' ' +
                  STAT_TRACKER_BYTES_TRANSMITTED, collectionInterval
              )
          );
    }
    else
    {
      interfaceStatistics = new InterfaceStatistics[monitoredInterfaces.length];
      int i = 0;
      for (String nic : monitoredInterfaces)
      {
        interfaceStatistics[i++] =
            new SingleInterfaceStatistics(
                nic,
                new LongValueTracker(clientID, threadID, clientID + ' ' +
                    ' ' + nic + ' ' + STAT_TRACKER_BYTES_RECEIVED,
                    collectionInterval
                ),
                new LongValueTracker(clientID, threadID, clientID + ' ' +
                    ' ' + nic + ' ' + STAT_TRACKER_BYTES_TRANSMITTED,
                    collectionInterval
                )
            );
      }
    }
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "NetStat";
  }



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  @Override()
  public StatTracker[] getResourceStatistics()
  {
    // Sort the trackers by their display name
    final List<LongValueTracker> trackers = new ArrayList<LongValueTracker>();
    for (InterfaceStatistics stats : this.interfaceStatistics)
    {
      trackers.add(stats.getReceivedBytes());
      trackers.add(stats.getSentBytes());
    }

    return trackers.toArray(new StatTracker[trackers.size()]);
  }



  /**
   * Performs the work of actually collecting resource statistics.  This method
   * should periodically call the <CODE>shouldStop()</CODE> method to determine
   * whether to stop collecting statistics.
   *
   * @return  A value that indicates the status of the monitor when it
   *          completed.
   */
  @Override()
  public int runMonitor()
  {
    // Determine whether to enable real-time statistics collection.  If so, then
    // we'll actually capture the data twice -- once for real-time reporting and
    // then again for what we actually report back to the server.
    ResourceMonitorJob monitorJob = getMonitorJob();
    if ((monitorJob != null) && (monitorJob.enableRealTimeStats()))
    {
      String jobID = monitorJob.getJobID();
      RealTimeStatReporter statReporter = monitorJob.getStatReporter();

      enableRealTimeStats = true;

      for (InterfaceStatistics stats : interfaceStatistics)
      {
        stats.enableRealTimeStats();

        final LongValueTracker sentBytesTracker = stats.getSentBytes();
        sentBytesTracker.startTracker();
        sentBytesTracker.enableRealTimeStats(statReporter, jobID);

        final LongValueTracker receivedBytesTracker = stats.getReceivedBytes();
        receivedBytesTracker.startTracker();
        receivedBytesTracker.enableRealTimeStats(statReporter, jobID);
      }
    }


    if (!clientSupported())
    {
      logMessage("Unsupported client OS (" + getClientOS() + ')');
            return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    final NetStatCollector.Builder builder = new NetStatCollector.Builder();
    builder.setAggregateStatistics(this.aggregateStats);
    builder.setCollectionIntervalSecs(this.collectionInterval);
    builder.setInterfaceStatistics(this.interfaceStatistics);
    builder.setMonitorAllInterfaces(this.monitoredInterfaces == null);
    builder.setInterfaceNames(this.monitoredInterfaces);
    builder.setMonitor(this);
    builder.setOSType(getClientOS());

    final NetStatCollector collector = builder.build();

    try
    {
      collector.initialize();
    }
    catch (Exception e)
    {
      logMessage(
          "Failed to initialize network statistics collection, reason: "
              + e.getLocalizedMessage()
      );
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }


    while (! shouldStop())
    {
      long stopSleepTime = System.currentTimeMillis() +
                           (1000 * collectionInterval);

      try
      {
        collector.collect();
      }
      catch (Exception e)
      {
        writeVerbose("Caught an exception:  " + e);
        writeVerbose(JobClass.stackTraceToString(e));
      }

      long sleepTime = stopSleepTime - System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        }
        catch (Exception e)
        {
          // ignore
        }
      }
    }


    // If we were capturing real-time statistics, then stop so that we can
    // replace the data with what will actually be reported.
    if (enableRealTimeStats)
    {
      for (InterfaceStatistics stats : interfaceStatistics)
      {
        final LongValueTracker sentBytesTracker = stats.getSentBytes();
        sentBytesTracker.stopTracker();

        final LongValueTracker receivedBytesTracker = stats.getReceivedBytes();
        receivedBytesTracker.stopTracker();
      }

    }

    collector.finalizeCollection();

    return Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
  }

}

