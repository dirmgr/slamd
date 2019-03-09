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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.StackedValueTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD resource monitor that uses the mpstat utility to
 * measure CPU utilization data on a per-CPU basis.  Note that the definition of
 * "CPU" is controlled by the mpstat utility, so on multi-core systems (e.g.,
 * those using UltraSPARC-IV processors) each core will appear as a separate
 * CPU, and on CMT systems (e.g., those using UltraSPARC T1 processors), each
 * strand will appear as a separate CPU.
 * <BR><BR>
 * This resource monitor is currently only supported on Solaris systems (using
 * either SPARC or x86/x64 processors).
 *
 *
 * @author   Neil A. Wilson
 */
public class MPStatResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker that will be used to report the CPU
   * busy time (user time + system time).
   */
  public static final String STAT_TRACKER_CPU_BUSY = "MPStat CPU Busy Time";



  /**
   * The display name of the stat tracker that will be used to report the CPU
   * idle time.
   */
  public static final String STAT_TRACKER_CPU_IDLE = "MPStat CPU Idle Time";



  /**
   * The display name of the stat tracker that will be used to report the CPU
   * system time.
   */
  public static final String STAT_TRACKER_CPU_SYSTEM = "MPStat CPU System Time";



  /**
   * The display name of the stat tracker that will be used to report the CPU
   * user time.
   */
  public static final String STAT_TRACKER_CPU_USER = "MPStat CPU User Time";



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



  // Stat trackers that will be used by this resource monitor thread.
  private StackedValueTracker cpuBusyTime;
  private StackedValueTracker cpuIdleTime;
  private StackedValueTracker cpuSystemTime;
  private StackedValueTracker cpuUserTime;

  // Flags that indicate what should be captured
  private boolean captureCPUBusy;
  private boolean captureCPUIdle;
  private boolean captureCPUSystem;
  private boolean captureCPUUser;

  // The maps that will be used to hold the data collected while mpstat
  // is running.
  private LinkedHashMap<Integer,ArrayList<Integer>> idleMap;
  private LinkedHashMap<Integer,ArrayList<Integer>> systemMap;
  private LinkedHashMap<Integer,ArrayList<Integer>> userMap;

  // The statistics collection interval that should be used.
  private int collectionInterval;

  // The data to use when initializing the stat trackers.
  private String clientID;
  private String threadID;



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
    captureCPUBusy   = getProperty(PROPERTY_CAPTURE_CPU_BUSY,
                                   DEFAULT_CAPTURE_CPU_BUSY);
    captureCPUUser   = getProperty(PROPERTY_CAPTURE_CPU_USER,
                                   DEFAULT_CAPTURE_CPU_USER);
    captureCPUSystem = getProperty(PROPERTY_CAPTURE_CPU_SYSTEM,
                                   DEFAULT_CAPTURE_CPU_SYSTEM);
    captureCPUIdle   = getProperty(PROPERTY_CAPTURE_CPU_IDLE,
                                   DEFAULT_CAPTURE_CPU_IDLE);

    userMap   = new LinkedHashMap<Integer,ArrayList<Integer>>();
    systemMap = new LinkedHashMap<Integer,ArrayList<Integer>>();
    idleMap   = new LinkedHashMap<Integer,ArrayList<Integer>>();
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
    MPStatResourceMonitor monitor = new MPStatResourceMonitor();
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
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.collectionInterval = collectionInterval;

    cpuUserTime   = null;
    cpuSystemTime = null;
    cpuIdleTime   = null;
    cpuBusyTime   = null;
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "MPStat";
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

    if (captureCPUBusy && (cpuBusyTime != null))
    {
      statList.add(cpuBusyTime);
    }

    if (captureCPUUser && (cpuUserTime != null))
    {
      statList.add(cpuUserTime);
    }

    if (captureCPUSystem && (cpuSystemTime != null))
    {
      statList.add(cpuSystemTime);
    }

    if (captureCPUIdle && (cpuIdleTime != null))
    {
      statList.add(cpuIdleTime);
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
    // First execute the mpstat command and collect its output.
    Process        p;
    InputStream    inputStream;
    BufferedReader reader;

    try
    {
      String[] commandArray =
      {
        "mpstat",
        String.valueOf(collectionInterval)
      };
      p           = Runtime.getRuntime().exec(commandArray);
      inputStream = p.getInputStream();
      reader      = new BufferedReader(new InputStreamReader(inputStream));
    }
    catch (Exception e)
    {
      logMessage("Error executing command \"mpstat " + collectionInterval +
                 "\" -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    boolean firstSetSeen    = false;
    boolean firstSetSkipped = false;
    int     numIterations   = -1;
    int     stopReason      = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
    while (! shouldStop())
    {
      try
      {
        if (inputStream.available() == 0)
        {
          try
          {
            Thread.sleep(100);
            continue;
          } catch (InterruptedException ie) {}
        }


        // Read the next line of output and figure out what to do with it.
        String line = reader.readLine();
        if (line.startsWith("CPU"))
        {
          // This is a header line.  If it's the first one, then indicate that
          // we've seen it.  If it's a subsequent one, then indicate that we
          // have skipped the first set.
          if (firstSetSeen)
          {
            firstSetSkipped = true;
            numIterations++;
          }
          else
          {
            firstSetSeen = true;
          }
        }
        else
        {
          // This is a data line.  If we're in the first set of output, then
          // simply parse out the CPU ID and store it in the hash with an empty
          // list.  Otherwise, also grab the percent user, system, wait, and
          // idle times and append them to the lists already in the hash.
          StringTokenizer tokenizer = new StringTokenizer(line, " \t");
          if (firstSetSkipped)
          {
            Integer cpuID = new Integer(tokenizer.nextToken());

            ArrayList<Integer> userList   = userMap.get(cpuID);
            ArrayList<Integer> systemList = systemMap.get(cpuID);
            ArrayList<Integer> idleList   = idleMap.get(cpuID);

            if (userList == null)
            {
              // This is a CPU we haven't seen before.  This could happen if the
              // CPU was dynamically enabled in the middle of the job.  In this
              // case, create a new list and fill it with zeros up to this
              // point.
              userList   = new ArrayList<Integer>();
              systemList = new ArrayList<Integer>();
              idleList   = new ArrayList<Integer>();

              for (int i=0; i < numIterations; i++)
              {
                userList.add(0);
                systemList.add(0);
                idleList.add(0);
              }

              userMap.put(cpuID, userList);
              systemMap.put(cpuID, systemList);
              idleMap.put(cpuID, idleList);
            }
            else if (userList.size() < numIterations)
            {
              // We must be missing some data for this CPU for one or more
              // iterations.  This can happen if the CPU is dynamically taken
              // offline and then re-enabled in the middle of a job.  In this
              // case, fill in the gap with zeros.
              while (userList.size() < numIterations)
              {
                userList.add(0);
                systemList.add(0);
                idleList.add(0);
              }
            }

            // Skip over all the data that we don't care about for this tracker.
            tokenizer.nextToken(); // minf
            tokenizer.nextToken(); // mjf
            tokenizer.nextToken(); // xcal
            tokenizer.nextToken(); // intr
            tokenizer.nextToken(); // ithr
            tokenizer.nextToken(); // csw
            tokenizer.nextToken(); // icsw
            tokenizer.nextToken(); // migr
            tokenizer.nextToken(); // smtx
            tokenizer.nextToken(); // srw
            tokenizer.nextToken(); // syscl

            // Parse out the user, system, wait, and idle times.
            Integer userTime   = new Integer(tokenizer.nextToken());
            Integer systemTime = new Integer(tokenizer.nextToken());
            Integer waitTime   = new Integer(tokenizer.nextToken());
            Integer idleTime   = new Integer(tokenizer.nextToken());

            if (waitTime > 0)
            {
              systemTime = (systemTime + waitTime);
            }

            userList.add(userTime);
            systemList.add(systemTime);
            idleList.add(idleTime);
          }
          else
          {
            Integer cpuID = new Integer(tokenizer.nextToken());
            userMap.put(cpuID, new ArrayList<Integer>());
            systemMap.put(cpuID, new ArrayList<Integer>());
            idleMap.put(cpuID, new ArrayList<Integer>());
          }
        }
      }
      catch (Exception e)
      {
        logMessage("Error while parsing mpstat command output:  " + e);
        stopReason = Constants.JOB_STATE_COMPLETED_WITH_ERRORS;
      }
    }

    try
    {
      reader.close();
      p.destroy();
    } catch (Exception e) {}


    // If the number of iterations is negative, then we didn't capture any data.
    if (numIterations <= 0)
    {
      cpuBusyTime   = null;
      cpuUserTime   = null;
      cpuSystemTime = null;
      cpuIdleTime   = null;

      return stopReason;
    }


    // Iterate through the maps and make sure that all the data for each of the
    // CPUs has the right number of iterations.  This could be off if a CPU was
    // dynamically offlined and not re-enabled while the job was running, in
    // which case we'll pad out the lists with zeros.  It could also occur if
    // the monitor was in the middle of parsing a set of output and only some of
    // the CPUs had been handled, in which case we'll drop the data for that
    // interval for those CPUs that had been captured.  Then convert each data
    // set to arrays to use when initializing the stat trackers.
    int        numCPUs         = userMap.size();
    String[]   categoryNames   = new String[numCPUs+1];
    double[][] userTimeArray   = new double[numIterations][numCPUs+1];
    double[][] systemTimeArray = new double[numIterations][numCPUs+1];
    double[][] idleTimeArray   = new double[numIterations][numCPUs+1];
    double[][] busyTimeArray   = new double[numIterations][numCPUs+1];
    int[]      categoryCounts  = new int[numIterations];
    int        categorySlot    = 0;

    Iterator iterator = userMap.keySet().iterator();
    while (iterator.hasNext())
    {
      int slot= categorySlot++;

      Integer cpuID = (Integer) iterator.next();

      ArrayList<Integer> userList   = userMap.get(cpuID);
      ArrayList<Integer> systemList = systemMap.get(cpuID);
      ArrayList<Integer> idleList   = idleMap.get(cpuID);

      if (userList.size() != numIterations)
      {
        while (userList.size() < numIterations)
        {
          userList.add(0);
          systemList.add(0);
          idleList.add(0);
        }

        while (userList.size() > numIterations)
        {
          userList.remove(numIterations);
          systemList.remove(numIterations);
          idleList.remove(numIterations);
        }
      }

      categoryNames[slot]  = "CPU " + cpuID;
      for (int i=0; i < numIterations; i++)
      {
        Integer userTime   = userList.get(i);
        Integer systemTime = systemList.get(i);
        Integer idleTime   = idleList.get(i);

        userTimeArray[i][slot]   = userTime.doubleValue();
        systemTimeArray[i][slot] = systemTime.doubleValue();
        idleTimeArray[i][slot]   = idleTime.doubleValue();
        busyTimeArray[i][slot]   =
             userTime.doubleValue() + systemTime.doubleValue();
      }
    }

    categoryNames[numCPUs] = "Idle Time";
    for (int i=0; i < numIterations; i++)
    {
      userTimeArray[i][numCPUs]   = (100 * numCPUs);
      systemTimeArray[i][numCPUs] = (100 * numCPUs);
      idleTimeArray[i][numCPUs]   = (100 * numCPUs);
      busyTimeArray[i][numCPUs]   = (100 * numCPUs);

      for (int j=0; j < numCPUs; j++)
      {
        userTimeArray[i][numCPUs]   -= userTimeArray[i][j];
        systemTimeArray[i][numCPUs] -= systemTimeArray[i][j];
        idleTimeArray[i][numCPUs]   -= idleTimeArray[i][j];
        busyTimeArray[i][numCPUs]   -= busyTimeArray[i][j];
      }
    }

    Arrays.fill(categoryCounts, 1);


    // Create the stat trackers and populate them with the captured data.
    cpuBusyTime = new StackedValueTracker(clientID, threadID,
                           clientID + ' ' + STAT_TRACKER_CPU_BUSY,
                           collectionInterval, categoryNames);
    cpuBusyTime.setIntervalTotals(busyTimeArray, categoryCounts);
    cpuBusyTime.setDrawAsStackedGraph(false);

    cpuUserTime = new StackedValueTracker(clientID, threadID,
                           clientID + ' ' + STAT_TRACKER_CPU_USER,
                           collectionInterval, categoryNames);
    cpuUserTime.setIntervalTotals(userTimeArray, categoryCounts);
    cpuUserTime.setDrawAsStackedGraph(false);

    cpuSystemTime = new StackedValueTracker(clientID, threadID,
                             clientID + ' ' + STAT_TRACKER_CPU_SYSTEM,
                             collectionInterval, categoryNames);
    cpuSystemTime.setIntervalTotals(systemTimeArray, categoryCounts);
    cpuSystemTime.setDrawAsStackedGraph(false);

    cpuIdleTime = new StackedValueTracker(clientID, threadID,
                           clientID + ' ' + STAT_TRACKER_CPU_IDLE,
                           collectionInterval, categoryNames);
    cpuIdleTime.setIntervalTotals(idleTimeArray, categoryCounts);
    cpuIdleTime.setDrawAsStackedGraph(false);

    return stopReason;
  }
}

