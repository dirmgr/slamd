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
package com.slamd.resourcemonitor;



import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StackedValueTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD resource monitor that uses the vmstat utility to
 * capture information about CPU utilization.
 *
 *
 * @author   Neil A. Wilson
 */
public class VMStatResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker that will be used to report the CPU
   * busy time (user time + system time).
   */
  public static final String STAT_TRACKER_CPU_BUSY = "CPU Busy Time";



  /**
   * The display name of the stat tracker that will be used to report the CPU
   * idle time.
   */
  public static final String STAT_TRACKER_CPU_IDLE = "CPU Idle Time";



  /**
   * The display name of the stat tracker that will be used to report the CPU
   * system time.
   */
  public static final String STAT_TRACKER_CPU_SYSTEM = "CPU System Time";



  /**
   * The display name of the stat tracker that will be used to report the CPU
   * user time.
   */
  public static final String STAT_TRACKER_CPU_USER = "CPU User Time";



  /**
   * The display name of the stat tracker that will be used to report CPU
   * utilization, combining user, system, and idle times.
   */
  public static final String STAT_TRACKER_CPU_UTILIZATION = "CPU Utilization";



  /**
   * The display name of the stat tracker that will be used to report the amount
   * of free memory on the system as reported by vmstat.
   */
  public static final String STAT_TRACKER_FREE_MEMORY = "Free Memory";



  /**
   * The category name that will be used for reporting user time.
   */
  public static final String UTILIZATION_CATEGORY_USER = "User Time";



  /**
   * The category name that will be used for reporting system time.
   */
  public static final String UTILIZATION_CATEGORY_SYSTEM = "System Time";



  /**
   * The category name that will be used for reporting idle time.
   */
  public static final String UTILIZATION_CATEGORY_IDLE = "Idle Time";



  /**
   * The names of the categories into which CPU utilization will be divided.
   */
  public static final String[] CPU_UTILIZATION_CATEGORIES =
  {
    UTILIZATION_CATEGORY_USER,
    UTILIZATION_CATEGORY_SYSTEM,
    UTILIZATION_CATEGORY_IDLE
  };



  /**
   * The type of vmstat output that indicates that the client is a Linux system
   * that does not have a supported version of vmstat.
   */
  public static final int LINUX_VMSTAT_TYPE_UNSUPPORTED = 0;



  /**
   * The type of vmstat output that indicates the client is a Linux system
   * with the version of vmstat used in the 2.x version of the procps package.
   */
  public static final int LINUX_VMSTAT_TYPE_PROCPS_2 = 2;



  /**
   * The type of vmstat output that indicates the client is a Linux system with
   * the version of vmstat used in the 3.x version of the procps package.
   */
  public static final int LINUX_VMSTAT_TYPE_PROCPS_3 = 3;



  /**
   * The type of vmstat output that indicates the client is a Linux system with
   * a version of vmstat like that used on Red Hat Enterprise 3.0.
   */
  public static final int LINUX_VMSTAT_TYPE_RHEL_30 = 4;



  /**
   * The type of vmstat output that indicates the client is a Linux system with
   * a version of vmstat like that used on Red Hat Enterprise 5.0.
   */
  public static final int LINUX_VMSTAT_TYPE_RHEL_50 = 5;



  /**
   * The name of the configuration property that indicates the path of the
   * vmstat command (or the path to a command that generates output in a
   * supported format for the current OS).
   */
  public static final String PROPERTY_VMSTAT_COMMAND = "vmstat_command";



  /**
   * The default vmstat command that will be executed if no alternative is
   * provided.
   */
  public static final String DEFAULT_VMSTAT_COMMAND = "vmstat";



  /**
   * The name of the configuration property that indicates whether to skip the
   * first line of actual vmstat data.  In many cases, the fist line that vmstat
   * outputs is actually a summary of what has occurred since the system was
   * last booted, and does not actually reflect the current workload.
   */
  public static final String PROPERTY_SKIP_FIRST_LINE = "skip_first_line";



  /**
   * The default behavior that will be used with regards to skipping the first
   * line of output if no other value is provided.
   */
  public static final boolean DEFAULT_SKIP_FIRST_LINE = true;



  /**
   * The name of the configuration property that indicates whether the
   * aggregate CPU utilization should be captured.
   */
  public static final String PROPERTY_CAPTURE_CPU_UTILIZATION =
       "capture_cpu_utilization";



  /**
   * The default behavior that will be used with regards to capturing aggregate
   * CPU utilization.
   */
  public static final boolean DEFAULT_CAPTURE_CPU_UTILIZATION = true;



  /**
   * The name of the configuration property that indicates whether the CPU busy
   * (user+system) time should be captured.
   */
  public static final String PROPERTY_CAPTURE_CPU_BUSY = "capture_cpu_busy";



  /**
   * The default behavior that will be used with regards to capturing CPU busy
   * time.
   */
  public static final boolean DEFAULT_CAPTURE_CPU_BUSY = true;



  /**
   * The name of the configuration property that indicates whether the CPU user
   * time should be captured.
   */
  public static final String PROPERTY_CAPTURE_CPU_USER = "capture_cpu_user";



  /**
   * The default behavior that will be used with regards to capturing CPU user
   * time.
   */
  public static final boolean DEFAULT_CAPTURE_CPU_USER = true;



  /**
   * The name of the configuration property that indicates whether the CPU
   * system time should be captured.
   */
  public static final String PROPERTY_CAPTURE_CPU_SYSTEM = "capture_cpu_system";



  /**
   * The default behavior that will be used with regards to capturing CPU system
   * time.
   */
  public static final boolean DEFAULT_CAPTURE_CPU_SYSTEM = true;



  /**
   * The name of the configuration property that indicates whether the CPU idle
   * time should be captured.
   */
  public static final String PROPERTY_CAPTURE_CPU_IDLE = "capture_cpu_idle";



  /**
   * The default behavior that will be used with regards to capturing CPU idle
   * time.
   */
  public static final boolean DEFAULT_CAPTURE_CPU_IDLE = true;



  /**
   * The name of the configuration property that indicates whether the amount of
   * free memory should be captured.
   */
  public static final String PROPERTY_CAPTURE_FREE_MEMORY =
       "capture_free_memory";



  /**
   * The default behavior that will be used with regards to capturing the amount
   * of free memory.
   */
  public static final boolean DEFAULT_CAPTURE_FREE_MEMORY = true;



  /**
   * The name of the configuration property that specifies minimum percentage of
   * CPU busy (user + system) time that may occur at any point during processing
   * at which a log message will be written to the SLAMD server.
   */
  public static final String PROPERTY_PEAK_UTILIZATION_LOG_THRESHOLD =
       "peak_utilization_log_threshold";



  /**
   * The default value that will be used as the peak utilization log threshold.
   */
  public static final int DEFAULT_PEAK_UTILIZATION_LOG_THRESHOLD = -1;



  /**
   * The name of the configuration property that specifies minimum percentage of
   * CPU busy (user + system) time that may occur as the average over the
   * duration of the job at which a log message will be written to the SLAMD
   * server.
   */
  public static final String PROPERTY_AVERAGE_UTILIZATION_LOG_THRESHOLD =
       "average_utilization_log_threshold";



  /**
   * The default value that will be used as the average utilization log
   * threshold.
   */
  public static final int DEFAULT_AVERAGE_UTILIZATION_LOG_THRESHOLD = -1;



  // Stat trackers that will be used by this resource monitor thread.
  private IntegerValueTracker cpuBusyTime;
  private IntegerValueTracker cpuIdleTime;
  private IntegerValueTracker cpuSystemTime;
  private IntegerValueTracker cpuUserTime;
  private IntegerValueTracker freeMemory;
  private StackedValueTracker cpuUtilization;

  // Flags that indicate what should be captured
  private boolean captureCPUBusy;
  private boolean captureCPUIdle;
  private boolean captureCPUSystem;
  private boolean captureCPUUser;
  private boolean captureCPUUtilization;
  private boolean captureFreeMemory;
  private int     averageUtilizationLogThreshold;
  private int     peakUtilizationLogThreshold;

  // A flag that indicates whether real-time statistics collection should be
  // enabled.
  private boolean enableRealTimeStats;

  // The array lists that will be used to hold the data collected while vmstat
  // is running.
  private ArrayList<Integer> freeMemoryList;
  private ArrayList<Integer> systemList;
  private ArrayList<Integer> userList;

  // Flags dealing with skipping the first line of output.
  private boolean firstLineSkipped;
  private boolean skipFirstLine;

  // The statistics collection interval that should be used.
  private int collectionInterval;

  // An indicator that specifies which type of vmstat we have on a linux system.
  private static int linuxVMStatType;

  // The type of OS on which this client is running.
  private int osType;

  // The actual command to execute in order to get the vmstat output.
  private String vmstatCommand;



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
    vmstatCommand         = getProperty(PROPERTY_VMSTAT_COMMAND,
                                        DEFAULT_VMSTAT_COMMAND);
    skipFirstLine         = getProperty(PROPERTY_SKIP_FIRST_LINE,
                                        DEFAULT_SKIP_FIRST_LINE);
    captureCPUUtilization = getProperty(PROPERTY_CAPTURE_CPU_UTILIZATION,
                                        DEFAULT_CAPTURE_CPU_UTILIZATION);
    captureCPUBusy        = getProperty(PROPERTY_CAPTURE_CPU_BUSY,
                                        DEFAULT_CAPTURE_CPU_BUSY);
    captureCPUUser        = getProperty(PROPERTY_CAPTURE_CPU_USER,
                                        DEFAULT_CAPTURE_CPU_USER);
    captureCPUSystem      = getProperty(PROPERTY_CAPTURE_CPU_SYSTEM,
                                        DEFAULT_CAPTURE_CPU_SYSTEM);
    captureCPUIdle        = getProperty(PROPERTY_CAPTURE_CPU_IDLE,
                                        DEFAULT_CAPTURE_CPU_IDLE);
    captureFreeMemory     = getProperty(PROPERTY_CAPTURE_FREE_MEMORY,
                                        DEFAULT_CAPTURE_FREE_MEMORY);

    averageUtilizationLogThreshold =
         getProperty(PROPERTY_AVERAGE_UTILIZATION_LOG_THRESHOLD,
                     DEFAULT_AVERAGE_UTILIZATION_LOG_THRESHOLD);
    peakUtilizationLogThreshold =
         getProperty(PROPERTY_PEAK_UTILIZATION_LOG_THRESHOLD,
                     DEFAULT_PEAK_UTILIZATION_LOG_THRESHOLD);

    firstLineSkipped = false;
    osType           = monitorClient.getClientOS();
    userList         = new ArrayList<Integer>();
    systemList       = new ArrayList<Integer>();
    freeMemoryList   = new ArrayList<Integer>();
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
        return true;
      case OS_TYPE_LINUX:
        return linuxSupported();
      case OS_TYPE_HPUX:
        return true;
      case OS_TYPE_AIX:
        return true;
      case OS_TYPE_WINDOWS:
        File vmstatFile = new File(vmstatCommand);
        if (vmstatFile.exists())
        {
          // On Windows, the only supported version of vmstat must be provided
          // by Cygwin, and therefore will behave much like a Linux version.
          return linuxSupported();
        }
        else
        {
          writeVerbose("vmstat is unsupported on Windows without a valid " +
                       "vmstat_command configuration.");
          return false;
        }
      default:
        return false;
    }
  }



  /**
   * Indicates whether this job will be available on Linux.  This is necessary
   * because some Linux systems may not come with vmstat and of those that do
   * there may be different styles of output.
   *
   * @return  <CODE>true</CODE> if the current Linux client system has a
   *          supported vmstat, or <CODE>false</CODE> if not.
   */
  private boolean linuxSupported()
  {
    try
    {
      Process p = Runtime.getRuntime().exec(vmstatCommand);
      BufferedReader br =
          new BufferedReader(new InputStreamReader(p.getInputStream()));

      // First, see if it exited immediately with an illegal
      try
      {
        int exitValue = p.exitValue();
        if (exitValue != 0)
        {
          writeVerbose("vmstat version not supported -- nonzero exit code " +
                       exitValue);
          linuxVMStatType = LINUX_VMSTAT_TYPE_UNSUPPORTED;
          return false;
        }
      }
      catch (IllegalThreadStateException itse)
      {
        // No problem, the command hasn't exited yet.
      }

      String line = br.readLine();
      if (line.equals("procs -----------memory---------- ---swap-- " +
                      "-----io---- --system-- ----cpu----"))
      {
        linuxVMStatType = LINUX_VMSTAT_TYPE_PROCPS_3;

        try
        {
          br.close();
        } catch (Exception e) {}

        writeVerbose("vmstat appears to be from procps 3");
        return true;
      }
      else if (line.equals("   procs                      memory    swap" +
                           "          io     system         cpu"))
      {
        linuxVMStatType = LINUX_VMSTAT_TYPE_PROCPS_2;

        try
        {
          br.close();
        } catch (Exception e) {}

        writeVerbose("vmstat appears to be from procps 2");
        return true;
      }
      else if (line.equals("procs                      memory      swap" +
                           "          io     system         cpu"))
      {
        linuxVMStatType = LINUX_VMSTAT_TYPE_RHEL_30;

        try
        {
          br.close();
        } catch (Exception e) {}

        writeVerbose("vmstat appears to be from RHEL 3.0");
        return true;
      }
      else if (line.equals("procs -----------memory---------- ---swap-- " +
                           "-----io---- --system-- -----cpu------"))
      {
        linuxVMStatType = LINUX_VMSTAT_TYPE_RHEL_50;

        try
        {
          br.close();
        } catch (Exception e) {}

        writeVerbose("vmstat appears to be from RHEL 5.0");
        return true;
      }
      else
      {
        linuxVMStatType = LINUX_VMSTAT_TYPE_UNSUPPORTED;

        try
        {
          br.close();
        } catch (Exception e) {}

        writeVerbose("Unrecognized vmstat output cannot be supported");
        writeVerbose("vmstat command output was:  \"" + line + '"');
        return false;
      }
    }
    catch (Exception e)
    {
      linuxVMStatType = LINUX_VMSTAT_TYPE_UNSUPPORTED;
      writeVerbose("Exception caught while parsing vmstat output:  " +
                   JobClass.stackTraceToString(e));
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
    VMStatResourceMonitor monitor = new VMStatResourceMonitor();
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


    // We may not actually capture all this data, but it won't hurt to
    // initialize it anyway.
    cpuIdleTime = new IntegerValueTracker(clientID, threadID,
                                          clientID + ' ' +
                                          STAT_TRACKER_CPU_IDLE,
                                          collectionInterval);
    cpuSystemTime = new IntegerValueTracker(clientID, threadID,
                                            clientID + ' ' +
                                            STAT_TRACKER_CPU_SYSTEM,
                                            collectionInterval);
    cpuUserTime = new IntegerValueTracker(clientID, threadID,
                                          clientID + ' ' +
                                          STAT_TRACKER_CPU_USER,
                                          collectionInterval);
    cpuBusyTime = new IntegerValueTracker(clientID, threadID,
                                          clientID + ' ' +
                                          STAT_TRACKER_CPU_BUSY,
                                          collectionInterval);
    cpuUtilization = new StackedValueTracker(clientID, threadID,
                                             clientID + ' ' +
                                             STAT_TRACKER_CPU_UTILIZATION,
                                             collectionInterval,
                                             CPU_UTILIZATION_CATEGORIES);
    freeMemory = new IntegerValueTracker(clientID, threadID,
                                         clientID + ' ' +
                                         STAT_TRACKER_FREE_MEMORY,
                                         collectionInterval);

    cpuUtilization.setDrawAsStackedGraph(true);
    cpuUtilization.setIncludeLegend(true);
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "VMStat";
  }



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  @Override()
  public StatTracker[] getResourceStatistics()
  {
    ArrayList<StatTracker> statList = new ArrayList<StatTracker>();

    if (captureCPUUtilization)
    {
      statList.add(cpuUtilization);
    }

    if (captureCPUBusy)
    {
      statList.add(cpuBusyTime);
    }

    if (captureCPUUser)
    {
      statList.add(cpuUserTime);
    }

    if (captureCPUSystem)
    {
      statList.add(cpuSystemTime);
    }

    if (captureCPUIdle)
    {
      statList.add(cpuIdleTime);
    }

    if (captureFreeMemory)
    {
      statList.add(freeMemory);
    }

    StatTracker[] returnTrackers = new StatTracker[statList.size()];
    statList.toArray(returnTrackers);
    return returnTrackers;
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
      if (captureCPUUser)
      {
        cpuUserTime.startTracker();
        cpuUserTime.enableRealTimeStats(statReporter, jobID);
      }

      if (captureCPUSystem)
      {
        cpuSystemTime.startTracker();
        cpuSystemTime.enableRealTimeStats(statReporter, jobID);
      }

      if (captureCPUIdle)
      {
        cpuIdleTime.startTracker();
        cpuIdleTime.enableRealTimeStats(statReporter, jobID);
      }

      if (captureCPUBusy)
      {
        cpuBusyTime.startTracker();
        cpuBusyTime.enableRealTimeStats(statReporter, jobID);
      }

      if (captureFreeMemory)
      {
        freeMemory.startTracker();
        freeMemory.enableRealTimeStats(statReporter, jobID);
      }
    }


    // First execute the vmstat command and collect its output.
    Process        p;
    InputStream    inputStream;
    BufferedReader reader;

    try
    {
      String[] commandArray =
      {
        vmstatCommand,
        String.valueOf(collectionInterval)
      };
      p           = Runtime.getRuntime().exec(commandArray);
      inputStream = p.getInputStream();
      reader      = new BufferedReader(new InputStreamReader(inputStream));
    }
    catch (Exception e)
    {
      logMessage("Error executing command \"" + vmstatCommand + ' ' +
                 collectionInterval + "\" -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    int stopReason = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
    while (! shouldStop())
    {
      try
      {
        if (inputStream.available() == 0)
        {
          try
          {
            Thread.sleep(10);
          } catch (InterruptedException ie) {}
          continue;
        }

        String line = reader.readLine();
        if (line == null)
        {
          // The command stopped running for some reason.
          logMessage("Command \"" + vmstatCommand + ' ' + collectionInterval +
                     "\" stopped producing output.");
          stopReason = Constants.JOB_STATE_COMPLETED_WITH_ERRORS;
          break;
        }
        else
        {
          switch (osType)
          {
            case OS_TYPE_SOLARIS:
              parseSolarisLine(line);
              break;
            case OS_TYPE_LINUX:
              parseLinuxLine(line);
              break;
            case OS_TYPE_HPUX:
              parseHPUXLine(line);
              break;
            case OS_TYPE_AIX:
              parseAIXLine(line);
              break;
            case OS_TYPE_WINDOWS:
              // On Windows, if we're using Cygwin then the output should look
              // like it does on Linux.
              parseLinuxLine(line);
              break;
          }
        }
      }
      catch (Exception e)
      {
        logMessage("Error while parsing output of command \"" + vmstatCommand +
                   ' ' + collectionInterval + "\" -- " + e);
        stopReason = Constants.JOB_STATE_COMPLETED_WITH_ERRORS;
      }
    }

    try
    {
      reader.close();
      p.destroy();
    } catch (Exception e) {}


    // If we were capturing real-time statistics, then stop so that we can
    // replace the data with what will actually be reported.
    if (enableRealTimeStats)
    {
      if (captureCPUUser)
      {
        cpuUserTime.stopTracker();
      }

      if (captureCPUSystem)
      {
        cpuSystemTime.stopTracker();
      }

      if (captureCPUIdle)
      {
        cpuIdleTime.stopTracker();
      }

      if (captureCPUBusy)
      {
        cpuBusyTime.stopTracker();
      }

      if (captureFreeMemory)
      {
        freeMemory.stopTracker();
      }
    }


    // Convert the data collected into arrays and use it to initialize the
    // stat trackers.
    int[]      userArray   = new int[userList.size()];
    int[]      systemArray = new int[systemList.size()];
    int[]      busyArray   = new int[userList.size()];
    int[]      idleArray   = new int[userList.size()];
    int[]      memArray    = new int[freeMemoryList.size()];
    int[]      countArray  = new int[userList.size()];
    double[][] dataArray   = new double[userList.size()][];

    int peakUtilization  = 0;
    int totalUtilization = 0;
    for (int i=0; i < userArray.length; i++)
    {
      userArray[i]   = userList.get(i);
      systemArray[i] = systemList.get(i);
      idleArray[i]   = 100 - userArray[i] - systemArray[i];
      memArray[i]    = freeMemoryList.get(i);
      busyArray[i]   = userArray[i] + systemArray[i];

      totalUtilization += busyArray[i];
      if (busyArray[i] > peakUtilization)
      {
        peakUtilization = busyArray[i];
      }

      dataArray[i]   = new double[]
      {
        userArray[i],
        systemArray[i],
        idleArray[i]
      };

      countArray[i] = 1;
    }

    if ((peakUtilization > 0) && (peakUtilizationLogThreshold > 0) &&
        (peakUtilization > peakUtilizationLogThreshold))
    {
      logMessage(getClientHostname() + " peak CPU utilization of " +
                 peakUtilization + "% exceeds configured log threshold of " +
                 peakUtilizationLogThreshold + '%');
    }

    if (userArray.length > 0)
    {
      int averageUtilization = totalUtilization / userArray.length;
      if ((averageUtilization > 0) && (averageUtilizationLogThreshold > 0) &&
          (averageUtilization > averageUtilizationLogThreshold))
      {
        logMessage(getClientHostname() + " average CPU utilization of " +
                   averageUtilization + "% exceeds configured log threshold " +
                   "of " + averageUtilizationLogThreshold + '%');
      }
    }

    cpuUserTime.setIntervalData(userArray, countArray);
    cpuSystemTime.setIntervalData(systemArray, countArray);
    cpuIdleTime.setIntervalData(idleArray, countArray);
    cpuBusyTime.setIntervalData(busyArray, countArray);
    cpuUtilization.setIntervalTotals(dataArray, countArray);
    freeMemory.setIntervalData(memArray, countArray);

    return stopReason;
  }



  /**
   * Parses the provided vmstat output line as generated from the Solaris vmstat
   * and extracts the CPU utilization from it.
   *
   * @param  line  The line of output generated by the Solaris vmstat.
   */
  private void parseSolarisLine(String line)
  {
    if (line.startsWith(" procs") || line.startsWith(" r b w") ||
        line.startsWith(" kthr") || line.startsWith("<<State change>>"))
    {
      // This is a header line and doesn't need to be parsed.
      return;
    }
    else if (skipFirstLine && (! firstLineSkipped))
    {
      // This is a data line, but is the first one and we don't want to capture
      // it.
      firstLineSkipped = true;
      return;
    }
    else
    {
      try
      {
        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        for (int i=0; i < 4; i++)
        {
          tokenizer.nextToken();
        }

        Integer freeMem = new Integer(tokenizer.nextToken());
        freeMemoryList.add(freeMem);

        for (int i=0; i < 14; i++)
        {
          tokenizer.nextToken();
        }

        Integer userTime   = new Integer(tokenizer.nextToken());
        Integer systemTime = new Integer(tokenizer.nextToken());
        Integer idleTime   = new Integer(tokenizer.nextToken());

        if (enableRealTimeStats)
        {
          if (captureCPUUser)
          {
            cpuUserTime.addValue(userTime);
          }

          if (captureCPUSystem)
          {
            cpuSystemTime.addValue(systemTime);
          }

          if (captureCPUIdle)
          {
            cpuIdleTime.addValue(idleTime);
          }

          if (captureCPUBusy)
          {
            cpuBusyTime.addValue(userTime + systemTime);
          }

          if (captureFreeMemory)
          {
            freeMemory.addValue(freeMem);
          }
        }

        userList.add(userTime);
        systemList.add(systemTime);
      }
      catch (Exception e)
      {
        logMessage("Unable to parse output line \"" + line + "\" -- " + e);
      }
    }
  }



  /**
   * Parses the provided vmstat output line as generated from the Linux vmstat
   * (either the version from procps 2.x or procps 3.x) and extracts the CPU
   * utilization from it.
   *
   * @param  line  The line of output generated by the Linux vmstat.
   */
  private void parseLinuxLine(String line)
  {
    if (linuxVMStatType == LINUX_VMSTAT_TYPE_PROCPS_2)
    {
      if (line.startsWith("   procs") || line.startsWith(" r  b  w"))
      {
        // This is a header line and doesn't need to be parsed.
        return;
      }
      else if (skipFirstLine && (! firstLineSkipped))
      {
        // This is a data line, but is the first one and we don't want to
        // capture it.
        firstLineSkipped = true;
        return;
      }
      else
      {
        try
        {
          StringTokenizer tokenizer = new StringTokenizer(line, " \t");
          for (int i=0; i < 4; i++)
          {
            tokenizer.nextToken();
          }

          int free  = Integer.parseInt(tokenizer.nextToken());
          int buff  = Integer.parseInt(tokenizer.nextToken());
          int cache = Integer.parseInt(tokenizer.nextToken());
          freeMemoryList.add(free+buff+cache);

          for (int i=0; i < 6; i++)
          {
            tokenizer.nextToken();
          }

          Integer userTime   = new Integer(tokenizer.nextToken());
          Integer systemTime = new Integer(tokenizer.nextToken());
          Integer idleTime   = new Integer(tokenizer.nextToken());

          if (enableRealTimeStats)
          {
            if (captureCPUUser)
            {
              cpuUserTime.addValue(userTime);
            }

            if (captureCPUSystem)
            {
              cpuSystemTime.addValue(systemTime);
            }

            if (captureCPUIdle)
            {
              cpuIdleTime.addValue(idleTime);
            }

            if (captureCPUBusy)
            {
              cpuBusyTime.addValue(userTime + systemTime);
            }

            if (captureFreeMemory)
            {
              freeMemory.addValue(free+buff+cache);
            }
          }

          userList.add(userTime);
          systemList.add(systemTime);
        }
        catch (Exception e)
        {
          logMessage("Unable to parse output line \"" + line + "\" -- " + e);
        }
      }
    }
    else if ((linuxVMStatType == LINUX_VMSTAT_TYPE_PROCPS_3) ||
             (linuxVMStatType == LINUX_VMSTAT_TYPE_RHEL_50))
    {
      if (line.startsWith("procs -----") || line.startsWith(" r  b"))
      {
        // This is a header line and doesn't need to be parsed.
        return;
      }
      else if (skipFirstLine && (! firstLineSkipped))
      {
        // This is a data line, but is the first one and we don't want to
        // capture it.
        firstLineSkipped = true;
        return;
      }
      else
      {
        try
        {
          StringTokenizer tokenizer = new StringTokenizer(line, " \t");
          for (int i=0; i < 3; i++)
          {
            tokenizer.nextToken();
          }

          int free  = Integer.parseInt(tokenizer.nextToken());
          int buff  = Integer.parseInt(tokenizer.nextToken());
          int cache = Integer.parseInt(tokenizer.nextToken());
          freeMemoryList.add(free+buff+cache);

          for (int i=0; i < 6; i++)
          {
            tokenizer.nextToken();
          }

          Integer userTime   = new Integer(tokenizer.nextToken());
          Integer systemTime = new Integer(tokenizer.nextToken());
          Integer idleTime   = new Integer(tokenizer.nextToken());

          if (enableRealTimeStats)
          {
            if (captureCPUUser)
            {
              cpuUserTime.addValue(userTime);
            }

            if (captureCPUSystem)
            {
              cpuSystemTime.addValue(systemTime);
            }

            if (captureCPUIdle)
            {
              cpuIdleTime.addValue(idleTime);
            }

            if (captureCPUBusy)
            {
              cpuBusyTime.addValue(userTime + systemTime);
            }

            if (captureFreeMemory)
            {
              freeMemory.addValue(free+buff+cache);
            }
          }

          userList.add(userTime);
          systemList.add(systemTime);
        }
        catch (Exception e)
        {
          logMessage("Unable to parse output line \"" + line + "\" -- " + e);
        }
      }
    }
    else if (linuxVMStatType == LINUX_VMSTAT_TYPE_RHEL_30)
    {
      if (line.startsWith("procs") || line.startsWith(" r  b"))
      {
        // This is a header line and doesn't need to be parsed.
        return;
      }
      else if (skipFirstLine && (! firstLineSkipped))
      {
        // This is a data line, but is the first one and we don't want to
        // capture it.
        firstLineSkipped = true;
        return;
      }
      else
      {
        try
        {
          StringTokenizer tokenizer = new StringTokenizer(line, " \t");
          for (int i=0; i < 3; i++)
          {
            tokenizer.nextToken();
          }

          int free  = Integer.parseInt(tokenizer.nextToken());
          int buff  = Integer.parseInt(tokenizer.nextToken());
          int cache = Integer.parseInt(tokenizer.nextToken());
          freeMemoryList.add(free+buff+cache);

          for (int i=0; i < 6; i++)
          {
            tokenizer.nextToken();
          }

          Integer userTime   = new Integer(tokenizer.nextToken());
          Integer systemTime = new Integer(tokenizer.nextToken());
          Integer idleTime   = new Integer(tokenizer.nextToken());
          Integer waitTime   = new Integer(tokenizer.nextToken());

          if (waitTime > 0)
          {
            systemTime = systemTime + waitTime;
          }

          if (enableRealTimeStats)
          {
            if (captureCPUUser)
            {
              cpuUserTime.addValue(userTime);
            }

            if (captureCPUSystem)
            {
              cpuSystemTime.addValue(systemTime);
            }

            if (captureCPUIdle)
            {
              cpuIdleTime.addValue(idleTime);
            }

            if (captureCPUBusy)
            {
              cpuBusyTime.addValue(userTime + systemTime);
            }

            if (captureFreeMemory)
            {
              freeMemory.addValue(free+buff+cache);
            }
          }

          userList.add(userTime);
          systemList.add(systemTime);
        }
        catch (Exception e)
        {
          logMessage("Unable to parse output line \"" + line + "\" -- " + e);
        }
      }
    }
  }



  /**
   * Parses the provided vmstat output line as generated from the HP-UX vmstat
   * and extracts the CPU utilization from it.
   *
   * @param  line  The line of output generated by the HP-UX vmstat.
   */
  public void parseHPUXLine(String line)
  {
    if (line.startsWith("         procs") || line.startsWith("    r     b"))
    {
      // This is a header line and doesn't need to be parsed.
      return;
    }
    else if (skipFirstLine && (! firstLineSkipped))
    {
      // This is a data line, but is the first one and we don't want to capture
      // it.
      firstLineSkipped = true;
      return;
    }
    else
    {
      try
      {
        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        for (int i=0; i < 4; i++)
        {
          tokenizer.nextToken();
        }

        Integer freeMem = new Integer(tokenizer.nextToken());
        freeMemoryList.add(freeMem);

        for (int i=0; i < 10; i++)
        {
          tokenizer.nextToken();
        }

        Integer userTime   = new Integer(tokenizer.nextToken());
        Integer systemTime = new Integer(tokenizer.nextToken());
        Integer idleTime   = new Integer(tokenizer.nextToken());

        if (enableRealTimeStats)
        {
          if (captureCPUUser)
          {
            cpuUserTime.addValue(userTime);
          }

          if (captureCPUSystem)
          {
            cpuSystemTime.addValue(systemTime);
          }

          if (captureCPUIdle)
          {
            cpuIdleTime.addValue(idleTime);
          }

          if (captureCPUBusy)
          {
            cpuBusyTime.addValue(userTime + systemTime);
          }

          if (captureFreeMemory)
          {
            freeMemory.addValue(freeMem);
          }
        }

        userList.add(userTime);
        systemList.add(systemTime);
      }
      catch (Exception e)
      {
        logMessage("Unable to parse output line \"" + line + "\" -- " + e);
      }
    }
  }



  /**
   * Parses the provided vmstat output line as generated from the AIX vmstat
   * and extracts the CPU utilization from it.
   *
   * @param  line  The line of output generated by the AIX vmstat.
   */
  public void parseAIXLine(String line)
  {
    if (line.startsWith("kthr") || line.startsWith("-----") ||
        line.startsWith(" r  b"))
    {
      // This is a header line and doesn't need to be parsed.
      return;
    }
    else if (skipFirstLine && (! firstLineSkipped))
    {
      // This is a data line, but is the first one and we don't want to capture
      // it.
      firstLineSkipped = true;
      return;
    }
    else
    {
      try
      {
        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        for (int i=0; i < 3; i++)
        {
          tokenizer.nextToken();
        }

        Integer freeMem = new Integer(tokenizer.nextToken());
        freeMemoryList.add(freeMem);

        for (int i=0; i < 9; i++)
        {
          tokenizer.nextToken();
        }

        Integer userTime   = new Integer(tokenizer.nextToken());
        Integer systemTime = new Integer(tokenizer.nextToken());
        Integer idleTime   = new Integer(tokenizer.nextToken());

        if (enableRealTimeStats)
        {
          if (captureCPUUser)
          {
            cpuUserTime.addValue(userTime);
          }

          if (captureCPUSystem)
          {
            cpuSystemTime.addValue(systemTime);
          }

          if (captureCPUIdle)
          {
            cpuIdleTime.addValue(idleTime);
          }

          if (captureCPUBusy)
          {
            cpuBusyTime.addValue(userTime + systemTime);
          }

          if (captureFreeMemory)
          {
            freeMemory.addValue(freeMem);
          }
        }

        userList.add(userTime);
        systemList.add(systemTime);
      }
      catch (Exception e)
      {
        logMessage("Unable to parse output line \"" + line + "\" -- " + e);
      }
    }
  }
}

