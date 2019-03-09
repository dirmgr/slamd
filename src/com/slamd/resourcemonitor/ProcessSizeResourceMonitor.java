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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD resource monitor that uses command-line utilities
 * to monitor the size of a specified process.  The process may be identified
 * by name or by PID, and the virtual size will be used for the process.  This
 * resource monitor will operate properly on Solaris (both SPARC and x86).  It
 * will also work for single-threaded processes on Linux with any threading
 * library, or for multi-threaded processes on Linux systems with NPTL support.
 *
 *
 * @author   Neil A. Wilson
 */
public class ProcessSizeResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker used to keep track of the process size
   * in kilobytes.
   */
  public static final String STAT_TRACKER_PROCESS_SIZE = "Process Size (KB)";



  /**
   * The name of the configuration property that specifies the process ID of the
   * process to monitor.
   */
  public static final String PROPERTY_PROCESS_ID = "process_id";



  /**
   * The default process ID that will be used if none is given.
   */
  public static final int DEFAULT_PROCESS_ID = -1;



  /**
   * The name of the configuration property that specifies a file containing
   * the process ID of the process to monitor.
   */
  public static final String PROPERTY_PROCESS_ID_FILE = "process_id_file";



  /**
   * The default process ID file that will be used if none is given.
   */
  public static final String DEFAULT_PROCESS_ID_FILE = null;



  /**
   * The name of the configuration property that specifies the name of the
   * process to monitor.
   */
  public static final String PROPERTY_PROCESS_NAME = "process_name";



  /**
   * The default process name that will be used if none is given.
   */
  public static final String DEFAULT_PROCESS_NAME = null;



  // The array list used to hold the data before we put it in the stat tracker.
  private ArrayList<Integer> sizeList;

  // A flag that indicates whether real-time statistics reporting should be
  // enabled.
  private boolean enableRealTimeStats;

  // Indicates whether the requested process has existed at any time during the
  // execution of this monitor.
  private boolean processExists;

  // The frequency that we should use when collecting statistics.
  private int collectionInterval;

  // The process ID of the process to monitor.
  private int processID;

  // The stat tracker used to keep track of the process size.
  private IntegerValueTracker processSize;

  // The path to the process ID file to use to determine the process to monitor.
  private String processIDFile;

  // The name of the process to monitor.
  private String processName;



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
    sizeList = new ArrayList<Integer>();

    processExists = false;
    processID     = getProperty(PROPERTY_PROCESS_ID, DEFAULT_PROCESS_ID);
    processIDFile = getProperty(PROPERTY_PROCESS_ID_FILE,
                                DEFAULT_PROCESS_ID_FILE);
    processName   = getProperty(PROPERTY_PROCESS_NAME, DEFAULT_PROCESS_NAME);

    if ((processIDFile == null) || (processIDFile.length() == 0))
    {
      processIDFile = null;
    }

    if ((processName == null) || (processName.length() == 0))
    {
      processName = null;
    }

    int configuredCount = 0;
    if (processID != DEFAULT_PROCESS_ID)
    {
      configuredCount++;
    }
    if ((processIDFile != null) && (processIDFile.length() > 0))
    {
      configuredCount++;
    }
    if ((processName != null) && (processName.length() > 0))
    {
      configuredCount++;
    }

    if (configuredCount == 0)
    {
      throw new SLAMDException("No process ID, PID file, or process name " +
                               "specified to monitor");
    }
    else if (configuredCount > 1)
    {
      throw new SLAMDException("Only one of the process ID, PID file, or " +
                               "process name may be specified");
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
        return true;
      case OS_TYPE_LINUX:
        // FIXME:  Should we do some kind of check here to see if NPTL is
        //         available?  Probably not, since it wouldn't matter for
        //         multithreaded applications.  Just trust the user to make the
        //         right choice.
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
    ProcessSizeResourceMonitor monitor = new ProcessSizeResourceMonitor();
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

    String procName;
    if (processName != null)
    {
      procName = processName;
    }
    else if (processIDFile != null)
    {
      procName = "PID File " + processIDFile;
    }
    else
    {
      procName = "PID " + processID;
    }

    processSize = new IntegerValueTracker(clientID, threadID,
                                          clientID + ' ' + procName + ' ' +
                                          STAT_TRACKER_PROCESS_SIZE,
                                          collectionInterval);
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "Process Size";
  }



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  @Override()
  public StatTracker[] getResourceStatistics()
  {
    if (processExists)
    {
      return new StatTracker[]
      {
        processSize
      };
    }
    else
    {
      if (processName != null)
      {
        logMessage("Process Size Resource Monitor:  The " + processName +
                   " process was not detected during the time the monitor " +
                   "was active for this job.");
      }
      else
      {
        logMessage("Process Size Resource Monitor:  The target process was " +
                   "not detected during the time the monitor was active for " +
                   "this job.");
      }

      return new StatTracker[0];
    }
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

      processSize.startTracker();
      processSize.enableRealTimeStats(statReporter, jobID);
    }


    int osType = getClientOS();

    boolean haveProcessID = ((processName == null) && (processIDFile == null));
    while (! shouldStop())
    {
      long stopSleepTime = System.currentTimeMillis() +
                           (1000 * collectionInterval);

      if (! haveProcessID)
      {
        try
        {
          if ((processName != null) && (processName.length() > 0))
          {
            processID = getPIDForName();
            if (processID < 0)
            {
              sizeList.add(0);
            }
            else
            {
              haveProcessID = true;
            }
          }
          else if ((processIDFile != null) && (processIDFile.length() > 0))
          {
            processID = getPIDFromFile();
            if (processID < 0)
            {
              sizeList.add(0);
            }
            else
            {
              haveProcessID = true;
            }
          }
        }
        catch (IOException ioe)
        {
          logMessage("Unable to determine process ID for process \"" +
                     processName + "\" -- " + ioe);
          return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
        }
      }

      if (haveProcessID)
      {
        try
        {
          switch (osType)
          {
            case OS_TYPE_SOLARIS:
              boolean pidFound = runSolaris();
              if (pidFound)
              {
                processExists = true;
              }
              else if (processName != null)
              {
                // This could mean that the process is no longer running.  If
                // so, then set a flag that we can use to try to detect when the
                // process is running again.
                logMessage("Last known process ID " + processID +
                           " could not be found.  Will start checking for a " +
                           "new process ID");
                haveProcessID = false;
              }
              break;
            case OS_TYPE_LINUX:
              pidFound = runLinux();
              if (pidFound)
              {
                processExists = true;
              }
              else if (processName != null)
              {
                // This could mean that the process is no longer running.  If
                // so, then set a flag that we can use to try to detect when the
                // process is running again.
                logMessage("Last known process ID " + processID +
                           " could not be found.  Will start checking for a " +
                           "new process ID");
                haveProcessID = false;
              }
              break;
            default:
              logMessage("Unsupported client OS (" + osType + ')');
              return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
          }
        }
        catch (Exception e)
        {
          writeVerbose("Caught an exception:  " + e);
          writeVerbose(JobClass.stackTraceToString(e));
        }
      }

      long sleepTime = stopSleepTime - System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        } catch (Exception e) {}
      }
    }


    if (enableRealTimeStats)
    {
      processSize.stopTracker();
    }


    int[] sizeArray  = new int[sizeList.size()];
    int[] countArray = new int[sizeArray.length];
    for (int i=0; i < sizeArray.length; i++)
    {
      sizeArray[i]  = sizeList.get(i);
      countArray[i] = 1;
    }
    processSize.setIntervalData(sizeArray, countArray);


    return Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
  }



  /**
   * Retrieves the process ID for the process with the specified name.
   *
   * @return  The process ID for the process with the specified name, or -1 if
   *          no such process is available.
   *
   * @throws  IOException  If a problem occurs while trying to make the
   *                       determination.
   */
  private int getPIDForName()
         throws IOException
  {
    switch (getClientOS())
    {
      // Solaris and Linux are exactly alike in this respect.
      case OS_TYPE_SOLARIS:
      case OS_TYPE_LINUX:
        Process p = Runtime.getRuntime().exec("ps -e -o comm,pid");
        BufferedReader reader =
             new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null)
        {
          StringTokenizer tokenizer = new StringTokenizer(line, " \t");
          String  command = tokenizer.nextToken();
          if (command.contains(processName))
          {
            try
            {
              reader.close();
              return Integer.parseInt(tokenizer.nextToken());
            }
            catch (Exception e)
            {
              reader.close();
              logMessage("Unable to determine PID for process \"" +
                         processName + "\" from line \"" + line + '"');
              return -1;
            }
          }
        }

        reader.close();
        return -1;
      default:
        logMessage("Unable to determine PID for process \"" +
                   processName + "\" -- unsupported client OS");
        return -1;
    }
  }



  /**
   * Retrieves the process ID to monitor from the specified file.
   *
   * @return  The process ID to monitor from the specified file, or -1 if no
   *          process ID could be read.
   *
   * @throws  IOException  If a problem occurs while trying to make the
   *                       determination.
   */
  private int getPIDFromFile()
         throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(processIDFile));
    String line = reader.readLine();
    reader.close();

    try
    {
      return Integer.parseInt(line.trim());
    }
    catch (NumberFormatException nfe)
    {
      writeVerbose("Number Format Exception reading PID from file \"" +
                   processIDFile + "\" -- " + JobClass.stackTraceToString(nfe));
      return -1;
    }
  }



  /**
   * Performs all necessary processing to determine the process size for the
   * process on a Solaris system.
   *
   * @return  <CODE>true</CODE> if the current size of the process was found, or
   *          <CODE>false</CODE> if not for some reason (e.g., the process is no
   *          longer running).
   *
   * @throws  IOException  If a problem occurs executing the appropriate command
   *                       or reading its output.
   */
  private boolean runSolaris()
          throws IOException
  {
    Process p = Runtime.getRuntime().exec("ps -p " + processID + " -o vsz");
    BufferedReader reader =
         new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;

    boolean pidFound  = false;
    boolean sizeAdded = false;
    while ((line = reader.readLine()) != null)
    {
      if (line.trim().equalsIgnoreCase("VSZ"))
      {
        continue;
      }

      try
      {
        int processSize = Integer.parseInt(line.trim());
        sizeList.add(processSize);

        if (enableRealTimeStats)
        {
          this.processSize.addValue(processSize);
        }

        pidFound  = true;
        sizeAdded = true;
        break;
      }
      catch (Exception e)
      {
        writeVerbose("Unable to determine process size:  " + e);
        sizeList.add(0);

        if (enableRealTimeStats)
        {
          processSize.addValue(0);
        }

        sizeAdded = true;
        break;
      }
    }

    if (! sizeAdded)
    {
      writeVerbose("Unable to determine process size");
      sizeList.add(0);

      if (enableRealTimeStats)
      {
        processSize.addValue(0);
      }
    }

    reader.close();
    return pidFound;
  }



  /**
   * Performs all necessary processing to determine the process size for the
   * process on a Linux system.  Note that this will only be accurate for
   * multithreaded applications on systems with NPTL support.
   *
   * @return  <CODE>true</CODE> if the current size of the process was found, or
   *          <CODE>false</CODE> if not for some reason (e.g., the process is no
   *          longer running).
   *
   * @throws  IOException  If a problem occurs executing the appropriate command
   *                       or reading its output.
   */
  private boolean runLinux()
          throws IOException
  {
    Process p = Runtime.getRuntime().exec("ps -p " + processID + " -o vsz");
    BufferedReader reader =
         new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;

    boolean pidFound  = false;
    boolean sizeAdded = false;
    while ((line = reader.readLine()) != null)
    {
      if (line.trim().equalsIgnoreCase("VSZ"))
      {
        continue;
      }

      try
      {
        int processSize = Integer.parseInt(line.trim());
        sizeList.add(processSize);

        if (enableRealTimeStats)
        {
          this.processSize.addValue(processSize);
        }

        pidFound  = true;
        sizeAdded = true;
        break;
      }
      catch (Exception e)
      {
        writeVerbose("Unable to determine process size:  " + e);
        sizeList.add(0);

        if (enableRealTimeStats)
        {
          processSize.addValue(0);
        }

        sizeAdded = true;
        break;
      }
    }

    if (! sizeAdded)
    {
      writeVerbose("Unable to determine process size");
      sizeList.add(0);

      if (enableRealTimeStats)
      {
        processSize.addValue(0);
      }
    }

    reader.close();
    return pidFound;
  }
}

