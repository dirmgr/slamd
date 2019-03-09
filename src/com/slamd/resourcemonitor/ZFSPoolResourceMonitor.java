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
 * Contributor(s):  Bertold Kolics, Neil A. Wilson
 */
package com.slamd.resourcemonitor;

import com.slamd.common.SLAMDException;
import com.slamd.common.Constants;
import com.slamd.stat.StatTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.StackedValueTracker;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * The ZFS pool resource monitor allows monitoring ZFS pools as exposed by the
 * {@code zpool iostat} command.
 */
public class ZFSPoolResourceMonitor
  extends ResourceMonitor
{
  /**
   * The name of the configuration property that specifies the name of the
   * monitored ZFS pools.
   */
  public static final String PROPERTY_POOL="monitor_pools";



  /**
   * The name of the boolean configuration property that specifies whether
   * read and write operations should be displayed stacked on the same graph.
   */
  public static final String PROPERTY_STACKED_GRAPH="display_stacked";



  /**
   * The name of the zpool command. Defaults to: zpool.
   */
  public static final String PROPERTY_ZPOOL_COMMAND = "zpool_command";



  /**
   * The display name of the stacked-value stat tracker that monitors the
   * number of operations.
   */
  public static final String STAT_TRACKER_OPERATIONS = "Operations";



  /**
   * The display name of the stacked-value stat tracker that monitors the
   * disk bandwidth.
   */
  public static final String STAT_TRACKER_BANDWIDTH = "Bandwidth";



  /**
   * The display name of the stat tracker that monitors the number of read
   * operations.
   */
  public static final String STAT_TRACKER_READS = "Reads";



  /**
   * The display name of the stat tracker that monitors the number of write
   * operations.
   */
  public static final String STAT_TRACKER_WRITES = "Writes";



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



  // The default zpool command
  private static final String DEFAULT_ZPOOL_COMMAND = "zpool";

  // Flags used to help skip the first set of data since it will be aggregate
  // information since boot which is completely irrelevant to what we actually
  // want to see.
  private boolean firstIterationSkipped;
  private boolean skipThisIteration;

  // The information to use when collecting statistics.
  private int    collectionInterval;
  private String clientID;
  private String threadID;

  // The map that will be used to hold the data read from zpool iostat.
  private Map<String,List<PoolData>> data;

  // The zpool command used for gathering statistics that includes all
  // parameters except the collection interval.
  private String zpoolCommand;


  /**
   * {@inheritDoc}
   */
  public String getMonitorName() {
    return "ZFS Pool Resource Monitor";
  }



  /**
   * Returns true if the operating system is Solaris, false otherwise.
   *
   * @return true if the operating system is Solaris, false otherwise.
   */
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
   * {@inheritDoc}
   */
  public void initializeStatistics(String clientID, String threadID,
                                   int collectionInterval)
  {
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.collectionInterval = collectionInterval;

  }



  /**
   * {@inheritDoc}
   */
  public ResourceMonitor newInstance() throws SLAMDException
  {
    final ResourceMonitor monitor = new ZFSPoolResourceMonitor();
    monitor.initialize(getMonitorClient(), getMonitorProperties());

    return monitor;
  }



  /**
   * {@inheritDoc}
   */
  public void initializeMonitor() throws SLAMDException
  {
    firstIterationSkipped = false;
    skipThisIteration     = false;
    data                  = new LinkedHashMap<String,List<PoolData>>();

    final String zpoolCmd =
        getProperty(PROPERTY_ZPOOL_COMMAND, DEFAULT_ZPOOL_COMMAND);

    final StringBuilder ioStatBuilder = new StringBuilder();
    ioStatBuilder.append(zpoolCmd).append(" iostat");

    final Collection<String> availablePools = getOnlinePoolNames();
    if (availablePools.isEmpty())
    {
      throw new SLAMDException("No pools found on this systems");
    }

    final String poolStr = getProperty(PROPERTY_POOL);
    if ((poolStr == null) || (poolStr.length() == 0))
    {
      for (String poolName : availablePools)
      {
        data.put(poolName, new ArrayList<PoolData>());
        ioStatBuilder.append(' ').append(poolName);
      }
    }
    else
    {
      StringTokenizer tokenizer = new StringTokenizer(poolStr, ",");
      while (tokenizer.hasMoreTokens())
      {
        String poolName = tokenizer.nextToken().trim();

        if (!availablePools.contains(poolName))
        {
          throw new SLAMDException("Pool '" + poolName + " either does not " +
              "exist or is offline");
        }

        data.put(poolName, new ArrayList<PoolData>());
        ioStatBuilder.append(' ').append(poolName);
      }
    }

    zpoolCommand = ioStatBuilder.toString();
    writeVerbose("zpool iostat command: " + zpoolCommand);
  }



  /**
   * {@inheritDoc}
   */
  public StatTracker[] getResourceStatistics()
  {
    final List<StatTracker> stats = new ArrayList<StatTracker>();
    final boolean asStackedValues =
        getProperty(PROPERTY_STACKED_GRAPH, false);

    for (Map.Entry<String,List<PoolData>> pool : data.entrySet())
    {
      final String poolName = pool.getKey();
      final List<PoolData> poolData = pool.getValue();

      if (asStackedValues)
      {
        // reads/writes share the same stacked value tracker

        final double[][] ops       = new double[poolData.size()][2];
        final double[][] bandwidth = new double[poolData.size()][2];
        final int[] countArray     = new int[poolData.size()];

        int i=0;
        for (PoolData data : poolData)
        {
          ops[i][0]       = data.reads;
          ops[i][1]       = data.writes;
          bandwidth[i][0] = data.kbRead;
          bandwidth[i][1] = data.kbWritten;
          countArray[i]   = 1;
          i++;
        }

        final StackedValueTracker opCountTracker =
            new StackedValueTracker(clientID, threadID,
                clientID + ' ' + poolName + ' ' + STAT_TRACKER_OPERATIONS,
                collectionInterval,
                new String[] { STAT_TRACKER_READS, STAT_TRACKER_WRITES});
        opCountTracker.setDrawAsStackedGraph(true);
        opCountTracker.setIntervalTotals(ops, countArray);
        opCountTracker.setIncludeHorizontalGrid(true);
        opCountTracker.setIncludeVerticalGrid(true);
        opCountTracker.setIncludeLegend(true);
        stats.add(opCountTracker);


        final StackedValueTracker bandwidthTracker =
            new StackedValueTracker(clientID, threadID,
                clientID + ' ' + poolName + ' ' + STAT_TRACKER_BANDWIDTH,
                collectionInterval,
                new String[] { STAT_TRACKER_KB_READ, STAT_TRACKER_KB_WRITTEN});
        bandwidthTracker.setDrawAsStackedGraph(true);
        bandwidthTracker.setIntervalTotals(bandwidth, countArray);
        bandwidthTracker.setIncludeHorizontalGrid(true);
        bandwidthTracker.setIncludeVerticalGrid(true);
        bandwidthTracker.setIncludeLegend(true);
        stats.add(bandwidthTracker);
      }
      else
      {
        final int[] reads      = new int[poolData.size()];
        final int[] writes     = new int[poolData.size()];
        final int[] kbRead     = new int[poolData.size()];
        final int[] kbWritten  = new int[poolData.size()];
        final int[] countArray = new int[poolData.size()];

        int i=0;
        for (PoolData data : poolData)
        {
          reads[i]      = data.reads;
          writes[i]     = data.writes;
          kbRead[i]     = data.kbRead;
          kbWritten[i]  = data.kbWritten;
          countArray[i] = 1;
          i++;
        }

        final IntegerValueTracker readTracker =
            new IntegerValueTracker(clientID, threadID,
                clientID + ' ' + poolName + ' ' + STAT_TRACKER_READS,
                collectionInterval);
        readTracker.setIntervalData(reads, countArray);
        stats.add(readTracker);

        final IntegerValueTracker writeTracker =
            new IntegerValueTracker(clientID, threadID,
                clientID + ' ' + poolName + ' ' + STAT_TRACKER_WRITES,
                collectionInterval);
        writeTracker.setIntervalData(writes, countArray);
        stats.add(writeTracker);

        final IntegerValueTracker kbReadTracker =
            new IntegerValueTracker(clientID, threadID,
                clientID + ' ' + poolName + ' ' + STAT_TRACKER_KB_READ,
                collectionInterval);
        kbReadTracker.setIntervalData(kbRead, countArray);
        stats.add(kbReadTracker);

        final IntegerValueTracker kbWrittenTracker =
            new IntegerValueTracker(clientID, threadID,
                clientID + ' ' + poolName + ' ' + STAT_TRACKER_KB_WRITTEN,
                collectionInterval);
        kbWrittenTracker.setIntervalData(kbWritten, countArray);
        stats.add(kbWrittenTracker);
      }
    }

    return stats.toArray(new StatTracker[stats.size()]);
  }



  /**
   * {@inheritDoc}
   */
  public int runMonitor()
  {
    final Process p;
    final InputStream inputStream;
    final BufferedReader reader;
    try
    {
      p = Runtime.getRuntime().exec(zpoolCommand + ' ' + collectionInterval);
      inputStream = p.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
    }
    catch (Exception e)
    {
      logMessage("Unable to execute \"" + zpoolCommand + ' ' +
                 collectionInterval + "\" -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    while (! shouldStop())
    {
      try
      {
        if (inputStream.available() <= 0)
        {
          try
          {
            Thread.sleep(10);
          }
          catch (InterruptedException ie)
          {
            // ignore
          }
          continue;
        }

        String line = reader.readLine();
        if (line == null)
        {
          logMessage("zpool iostat command stopped producing output");
          return Constants.JOB_STATE_COMPLETED_WITH_ERRORS;
        }
        else if (line.length() == 0)
        {
          continue;
        }

        line = line.trim();
        if (line.startsWith("  ")
            || line.startsWith("pool")
            || line.startsWith("----"))
        {
          continue;
        }

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

        if (skipThisIteration)
        {
          // We're in the first round of output, and we want to skip it.
          continue;
        }

        String[] fields = line.split("\\s+");
        if (fields.length != 7)
        {
          logMessage("Malformed zpool iostat output line: "  + line);
          continue;
        }
        String pool = fields[0];

        List<PoolData> poolData = data.get(pool);
        if (poolData == null)
        {
          logMessage("Internal error, missing pool data for pool " + pool);
          continue;
        }

        int readBytes = parseBandwidth(fields[5]);
        int writtenBytes = parseBandwidth(fields[6]);
        final PoolData stats = new PoolData(
            Integer.parseInt(fields[3]), Integer.parseInt(fields[4]),
            readBytes, writtenBytes);

        poolData.add(stats);
      }
      catch (Exception e)
      {
        try
        {
          reader.close();
          inputStream.close();
          p.destroy();
        }
        catch (Exception e2)
        {
          // ignore
        }

        logMessage("Error parsing zpool iostat output:  " + e);
        return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      }
    }

    if (p != null)
    {
      try
      {
        p.destroy();
      }
      catch (Exception e)
      {
        // ignore
      }
    }

    try
    {
      reader.close();
      inputStream.close();
    }
    catch (Exception e)
    {
      // ignore
    }

    return Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
  }



  /**
   * Parses the bandwidth values from the {@code zpool iostat} command and
   * returns the value expressed in kilobytes.
   *
   * @param field raw field value
   * @return the value in kilobytes
   */
  private int parseBandwidth(final String field)
  {
    int bandwidth = 0;

    if (field != null && field.length() > 0)
    {
      switch (field.charAt(field.length() - 1))
      {
        case '0':
          // fall through
        case '1':
          // fall through
        case '2':
          // fall through
        case '3':
          // fall through
        case '4':
          // fall through
        case '5':
          // fall through
        case '6':
          // fall through
        case '7':
          // fall through
        case '8':
          // fall through
        case '9':
          // divide by 1024
          bandwidth = Integer.parseInt(field) >> 10;
          break;
        case 'T':
          // terrabyte/s range: can't possibly happen in current systems.
          bandwidth = Integer.MAX_VALUE;
          break;
        case 'G':
          bandwidth =
              (int)(1024 * 1024 *
                  Double.parseDouble(field.substring(0, field.length() - 1)));
          break;
        case 'M':
          bandwidth =
              (int)(1024 *
                  Double.parseDouble(field.substring(0, field.length() - 1)));
          break;
        case 'K':
          bandwidth =
              (int)(Double.parseDouble(field.substring(0, field.length() - 1)));
          break;
        default:
          bandwidth = 0;
      }
    }

    return bandwidth;
  }


  /**
   * Returns the online ZFS pools available on the system.
   *
   * @return the online ZFS pools available on the system.
   */
  private Collection<String> getOnlinePoolNames()
  {
    final Collection<String> availablePoolList = new ArrayList<String>();
    BufferedReader reader = null;

    final String command;
    switch(getClientOS())
    {
      case Constants.OS_TYPE_SOLARIS:
        command = DEFAULT_ZPOOL_COMMAND + " list -H -o name,health";
        break;
      default:
        throw new IllegalArgumentException("Unsupported OS");
    }

    try
    {
      final Process p = Runtime.getRuntime().exec(command);
      reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

      String line;
      while ((line = reader.readLine()) != null)
      {
        line = line.trim();

        if (line.length() == 0)
        {
          continue;
        }

        StringTokenizer tokenizer = new StringTokenizer(line, "\t");
        String poolName = tokenizer.nextToken();
        boolean online = "ONLINE".equalsIgnoreCase(tokenizer.nextToken());

        if (online)
        {
          availablePoolList.add(poolName);
        }
      }
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(
          "Unable to read or parse output from iostat command -- " + e);
    }

    finally
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }
        catch (Exception e)
        {
          // ignore
        }

      }
    }

    return availablePoolList;
  }


  /**
   * Helper class to capture statistics for a ZFS pool.
   */
  private class PoolData
  {
    final int reads;
    final int writes;
    final int kbRead;
    final int kbWritten;

    private PoolData(int reads, int writes, int readKilobytes,
                     int writeKilobytes)
    {
      this.reads     = reads;
      this.writes    = writes;
      this.kbRead    = readKilobytes;
      this.kbWritten = writeKilobytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return String.format("reads=%d, writes=%d, kbRead=%d, kbWritten=%d",
          this.reads, this.writes, this.kbRead, this.kbWritten);
    }
  }

}
