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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.FloatValueTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD resource monitor that uses command-line utilities
 * to monitor I/O utilization.  Note that this monitor only fully supports
 * Solaris systems.  It provides some support for Linux systems with 4.1.x
 * versions of iostat, since earlier iostat versions don't provide the ability
 * to determine the amount of data read/written per second.  The iostat commands
 * on HP-UX and AIX are so crippled as to not provide any ability to get
 * separate read and write statistics and as such are basically worthless.
 *
 *
 * @author   Neil A. Wilson
 */
public class IOStatResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker that monitors the amount of data
   * read in kilobytes.
   */
  public static final String STAT_TRACKER_KB_READ = "Kilobytes Read";



  /**
   * The display name of the stat tracker that monitors the amount of data
   * written in kilobytes.
   */
  public static final String STAT_TRACKER_KB_WRITTEN = "Kilobytes Written";



  /**
   * The display name of the stat tracker that monitors the device percent busy.
   */
  public static final String STAT_TRACKER_PCT_BUSY = "Percent Busy";



  /**
   * The configuration property that specifies the path to the iostat command
   * to use.
   */
  public static final String PROPERTY_IOSTAT_COMMAND = "iostat_command";



  /**
   * The default iostat command that will be used if no other value is given.
   */
  public static final String DEFAULT_IOSTAT_COMMAND = "iostat";



  /**
   * The configuration property that specifies which disks to monitor.  If
   * specified, it should be a comma-delimited list of disk names as they are
   * output by iostat.  If not specified, information about all disks will be
   * captured.  It may optionally also include a set of alternate names to use
   * for those disks by specifying the values in the comma-delimited list in
   * the form "name=altname" (e.g., "ssd1=Database,ssd2=Logs").
   */
  public static final String PROPERTY_MONITOR_DISKS = "monitor_disks";



  // Flags used to help skip the first set of data since it will be aggregate
  // information since boot which is completely irrelevant to what we actually
  // want to see.
  private boolean firstIterationSkipped;
  private boolean skipThisIteration;

  // The hash map that specifies alternate names to use for the disks to
  // monitor.
  private HashMap<String,String> altNameMap;

  // The information to use when collecting statistics.
  private int    collectionInterval;
  private String clientID;
  private String threadID;

  // The hash maps that will be used to hold the data read from iostat.
  private LinkedHashMap<String,ArrayList<Double>> readData;
  private LinkedHashMap<String,ArrayList<Double>> writeData;
  private LinkedHashMap<String,ArrayList<Integer>> busyData;

  // The iostat command to execute.
  private String iostatCommand;

  // The set of disks for which monitoring has been requested.
  private String[] requestedDisks;



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
    firstIterationSkipped = false;
    skipThisIteration     = false;
    altNameMap            = new HashMap<String,String>();
    readData              = new LinkedHashMap<String,ArrayList<Double>>();
    writeData             = new LinkedHashMap<String,ArrayList<Double>>();
    busyData              = new LinkedHashMap<String,ArrayList<Integer>>();

    iostatCommand = getProperty(PROPERTY_IOSTAT_COMMAND,
                                DEFAULT_IOSTAT_COMMAND);
    switch (getClientOS())
    {
      case OS_TYPE_SOLARIS:
        iostatCommand += " -x -I";
        break;
      case OS_TYPE_LINUX:
        iostatCommand += " -d -k";
        break;
    }

    String diskStr = getProperty(PROPERTY_MONITOR_DISKS);
    if ((diskStr == null) || (diskStr.length() == 0))
    {
      requestedDisks = null;
    }
    else
    {
      ArrayList<String> diskList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(diskStr, ",");
      while (tokenizer.hasMoreTokens())
      {
        String diskName = null;
        String token = tokenizer.nextToken().trim();
        int equalPos = token.indexOf('=');
        if (equalPos > 0)
        {
          diskName       = token.substring(0, equalPos);
          String altName = token.substring(equalPos+1);
          diskList.add(diskName);
          altNameMap.put(diskName, altName);
        }
        else
        {
          diskName = token;
          diskList.add(diskName);
        }

        readData.put(diskName, new ArrayList<Double>());
        writeData.put(diskName, new ArrayList<Double>());
        busyData.put(diskName, new ArrayList<Integer>());
      }

      requestedDisks = new String[diskList.size()];
      diskList.toArray(requestedDisks);
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
        return solarisSupported();
      case OS_TYPE_LINUX:
        return linuxSupported();
      default:
        return false;
    }
  }



  /**
   * Determines whether a Solaris client system should be supported.  It does
   * this by verifying that the iostat command exists and reports data in the
   * expected format.  In addition, if a list of requested disks has been
   * provided, it will verify that they all exist.  If no set of requested
   * disks has been given, then this will set the requested disks to all disks
   * in the system.
   *
   * @return  <CODE>true</CODE> if the Solaris client system is supported, or
   *          <CODE>false</CODE> if it is not.
   */
  private boolean solarisSupported()
  {
    Process        p      = null;
    BufferedReader reader = null;

    try
    {
      p = Runtime.getRuntime().exec(iostatCommand);
      reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    }
    catch (Exception e)
    {
      writeVerbose("Unable to execute \"" + iostatCommand + "\" -- " + e);
      return false;
    }

    ArrayList<String> availableDiskList = new ArrayList<String>();
    try
    {
      String line;
      while ((line = reader.readLine()) != null)
      {
        line = line.trim();
        if ((line.length() == 0) ||
            line.startsWith("extended") ||
            line.startsWith("device"))
        {
          continue;
        }

        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        String diskName = tokenizer.nextToken();
        availableDiskList.add(diskName);
        if ((requestedDisks == null) || (requestedDisks.length == 0))
        {
          readData.put(diskName, new ArrayList<Double>());
          writeData.put(diskName, new ArrayList<Double>());
          busyData.put(diskName, new ArrayList<Integer>());
        }
      }
    }
    catch (Exception e)
    {
      writeVerbose("Unable to parse output from iostat command -- " + e);
      return false;
    }

    try
    {
      reader.close();
    } catch (Exception e) {}

    if ((requestedDisks != null) && (requestedDisks.length > 0))
    {
      for (int i=0; i < requestedDisks.length; i++)
      {
        if (! availableDiskList.contains(requestedDisks[i]))
        {
          writeVerbose("Requested disk " + requestedDisks[i] +
                       " not available on client system");
          return false;
        }
      }
    }

    if (readData.isEmpty())
    {
      writeVerbose("No disks available to be monitored.");
      return false;
    }

    return true;
  }



  /**
   * Determines whether a Linux client system should be supported.  It does
   * this by verifying that the iostat command exists and reports data in the
   * expected format.  In addition, if a list of requested disks has been
   * provided, it will verify that they all exist.  If no set of requested
   * disks has been given, then this will set the requested disks to all disks
   * in the system.
   *
   * @return  <CODE>true</CODE> if the Linux client system is supported, or
   *          <CODE>false</CODE> if it is not.
   */
  private boolean linuxSupported()
  {
    Process        p      = null;
    BufferedReader reader = null;
    try
    {
      p = Runtime.getRuntime().exec(iostatCommand);
      reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    }
    catch (Exception e)
    {
      writeVerbose("Unable to execute \"" + iostatCommand + "\" -- " + e);
      return false;
    }

    ArrayList<String> availableDiskList = new ArrayList<String>();
    try
    {
      String line;
      while ((line = reader.readLine()) != null)
      {
        if ((line.length() == 0) || line.startsWith("Linux ") ||
            line.startsWith("Device:"))
        {
          continue;
        }

        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        String diskName = tokenizer.nextToken();
        availableDiskList.add(diskName);
        if ((requestedDisks == null) || (requestedDisks.length == 0))
        {
          readData.put(diskName, new ArrayList<Double>());
          writeData.put(diskName, new ArrayList<Double>());
        }
      }
    }
    catch (Exception e)
    {
      writeVerbose("Unable to parse output from iostat command -- " + e);
      return false;
    }

    try
    {
      reader.close();
    } catch (Exception e) {}

    if ((requestedDisks != null) && (requestedDisks.length > 0))
    {
      for (int i=0; i < requestedDisks.length; i++)
      {
        if (! availableDiskList.contains(requestedDisks[i]))
        {
          writeVerbose("Requested disk " + requestedDisks[i] +
                       " not available on client system");
          return false;
        }
      }
    }

    if (readData.isEmpty())
    {
      writeVerbose("No disks available to be monitored.");
      return false;
    }

    return true;
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
    IOStatResourceMonitor monitor = new IOStatResourceMonitor();
    monitor.initialize(getMonitorClient(), getMonitorProperties());

    Iterator<String> iterator = readData.keySet().iterator();
    while (iterator.hasNext())
    {
      String key = iterator.next();
      monitor.readData.put(key, new ArrayList<Double>());
      monitor.writeData.put(key, new ArrayList<Double>());
      monitor.busyData.put(key, new ArrayList<Integer>());
    }

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
    // Just capture the information we need.  We will actually create the
    // trackers later.
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "IOStat";
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

    Iterator<String> diskNames = readData.keySet().iterator();
    while (diskNames.hasNext())
    {
      String diskName = diskNames.next();
      String altName  = altNameMap.get(diskName);
      if ((altName == null) || (altName.length() == 0))
      {
        altName = diskName;
      }

      ArrayList<Double> readList  = readData.get(diskName);
      ArrayList<Double> writeList = writeData.get(diskName);
      double[] readArray  = new double[readList.size()];
      double[] writeArray = new double[writeList.size()];
      int[]    countArray = new int[readList.size()];
      for (int i=0; i < readArray.length; i++)
      {
        readArray[i]  = readList.get(i);
        writeArray[i] = writeList.get(i);
        countArray[i] = 1;
      }

      FloatValueTracker readTracker =
           new FloatValueTracker(clientID, threadID,
                                 clientID + ' ' + altName + ' ' +
                                 STAT_TRACKER_KB_READ, collectionInterval);
      readTracker.setIntervalData(readArray, countArray);
      statList.add(readTracker);

      FloatValueTracker writeTracker =
           new FloatValueTracker(clientID, threadID,
                                 clientID + ' ' + altName + ' ' +
                                 STAT_TRACKER_KB_WRITTEN, collectionInterval);
      writeTracker.setIntervalData(writeArray, countArray);
      statList.add(writeTracker);

      ArrayList<Integer> busyList = busyData.get(diskName);
      if ((busyList != null) && (! busyList.isEmpty()))
      {
        int[] busyArray = new int[busyList.size()];
        for (int i=0; i < busyArray.length; i++)
        {
          busyArray[i] = busyList.get(i);
        }

        IntegerValueTracker busyTracker =
             new IntegerValueTracker(clientID, threadID,
                                     clientID + ' ' + altName + ' ' +
                                     STAT_TRACKER_PCT_BUSY, collectionInterval);
        busyTracker.setIntervalData(busyArray, countArray);
        statList.add(busyTracker);
      }
    }

    StatTracker[] trackers = new StatTracker[statList.size()];
    statList.toArray(trackers);
    return trackers;
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
    Process        p           = null;
    InputStream    inputStream = null;
    BufferedReader reader      = null;
    try
    {
      p = Runtime.getRuntime().exec(iostatCommand + ' ' + collectionInterval);
      inputStream = p.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
    }
    catch (Exception e)
    {
      logMessage("Unable to execute \"" + iostatCommand + ' ' +
                 collectionInterval + "\" -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    int clientOS = getClientOS();

    while (! shouldStop())
    {
      try
      {
        if (inputStream.available() <= 0)
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
          logMessage("iostat command stopped producing output");
          return Constants.JOB_STATE_COMPLETED_WITH_ERRORS;
        }
        else if (line.length() == 0)
        {
          continue;
        }

        switch (clientOS)
        {
          case OS_TYPE_SOLARIS:
            parseSolarisLine(line);
            break;
          case OS_TYPE_LINUX:
            parseLinuxLine(line);
            break;
        }
      }
      catch (Exception e)
      {
        try
        {
          reader.close();
          inputStream.close();
          p.destroy();
        }
        catch (Exception e2) {}

        logMessage("Error parsing iostat output:  " + e);
        return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      }
    }

    if (p != null)
    {
      try
      {
        p.destroy();
      } catch (Exception e) {}
    }

    try
    {
      reader.close();
      inputStream.close();
    }
    catch (Exception e) {}

    return Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
  }



  /**
   * Parses the provided line of iostat output as it would be generated on a
   * Solaris system.
   *
   * @param  line  The iostat output line to parse.
   */
  private void parseSolarisLine(String line)
  {
    line = line.trim();
    if (line.startsWith("extended"))
    {
      if (! firstIterationSkipped)
      {
        // This will be the first line of output.
        if (skipThisIteration)
        {
          firstIterationSkipped = true;
          skipThisIteration     = false;
        }
        else
        {
          skipThisIteration = true;
        }
      }

      return;
    }

    if (skipThisIteration)
    {
      // We're in the first round of output, and we want to skip it.
      return;
    }

    if (line.startsWith("device"))
    {
      // This is a header line -- skip it.
      return;
    }

    // This should be actual output.  Parse and handle it appropriately.
    StringTokenizer tokenizer = new StringTokenizer(line, " \t");
    String diskName = tokenizer.nextToken();
    ArrayList<Double> readList  = readData.get(diskName);
    if (readList == null)
    {
      // This isn't a disk we're monitoring.
      return;
    }

    ArrayList<Double> writeList = writeData.get(diskName);
    ArrayList<Integer> busyList = busyData.get(diskName);

    tokenizer.nextToken(); // Skip the number of reads per interval.
    tokenizer.nextToken(); // Skip the number of writes per interval.

    Double kbRead    = new Double(tokenizer.nextToken());
    Double kbWritten = new Double(tokenizer.nextToken());

    tokenizer.nextToken(); // Skip the wait queue length.
    tokenizer.nextToken(); // Skip the number of active transactions.
    tokenizer.nextToken(); // Skip the average service response time.
    tokenizer.nextToken(); // Skip the percent wait time.

    Integer pctBusy = new Integer(tokenizer.nextToken());

    readList.add(kbRead);
    writeList.add(kbWritten);
    busyList.add(pctBusy);
  }



  /**
   * Parses the provided line of iostat output as it would be generated on a
   * Linux system.
   *
   * @param  line  The iostat output line to parse.
   */
  private void parseLinuxLine(String line)
  {
    // This will be the first line of output with uname information.
    if (line.startsWith("Linux "))
    {
      return;
    }

    // This indicates that we're starting a new round of output.
    if (line.startsWith("Device:"))
    {
      if (! firstIterationSkipped)
      {
        if (skipThisIteration)
        {
          firstIterationSkipped = true;
          skipThisIteration     = false;
        }
        else
        {
          skipThisIteration = true;
        }
      }

      return;
    }

    // We're in the first round of output, and we want to skip it.
    if (skipThisIteration)
    {
      return;
    }

    // This should be actual output.  Parse and handle it appropriately.
    StringTokenizer tokenizer = new StringTokenizer(line, " \t");
    String diskName = tokenizer.nextToken();
    ArrayList<Double> readList  = readData.get(diskName);
    if (readList == null)
    {
      // This isn't a disk we're monitoring.
      return;
    }

    ArrayList<Double> writeList = writeData.get(diskName);

    tokenizer.nextToken(); // Skip the number of transactions per second.
    tokenizer.nextToken(); // Skip the KB read per second
    tokenizer.nextToken(); // Skip the KB written per second.

    Double kbRead    = new Double(tokenizer.nextToken());
    Double kbWritten = new Double(tokenizer.nextToken());

    readList.add(kbRead);
    writeList.add(kbWritten);
  }
}

