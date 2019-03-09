package com.slamd.resourcemonitor.netstat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Set;

/**
 * Linux network interface statistics collector.
 */
public class LinuxNetStatCollector extends NetStatCollector
{
  // Should only be instantiated by the builder
  LinuxNetStatCollector()
  {
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize()
  {
    if (!getInterfaceNames().isEmpty())
    {
      // If the monitor was configured to track certain interfaces only, then
      // check to see if all those interface names are present in the system.

      final Set<String> interfaceNames = new HashSet<String>();
      try
      {
        String line;

        final BufferedReader reader =
            new BufferedReader(new FileReader("/proc/net/dev"));

        while ((line = reader.readLine()) != null)
        {
          if (line.startsWith("Inter-") || line.startsWith(" face"))
          {
            continue;
          }
          final StringTokenizer tokenizer = new StringTokenizer(line, ": \t");
          interfaceNames.add(tokenizer.nextToken());
        }
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }

      if (!interfaceNames.containsAll(getInterfaceNames()))
      {
        logMessage("One or more monitored interfaces (" + getInterfaceNames() +
            ") do not exist in the system. Available interfaces are: " +
            interfaceNames);

        throw new IllegalArgumentException("one or more monitored interfaces" +
            " do not exist");
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  public void collect() throws IOException
  {
    String line;
    final BufferedReader reader =
        new BufferedReader(new FileReader("/proc/net/dev"));

    InterfaceStatistics stats = null;

    while ((line = reader.readLine()) != null)
    {
      if (line.startsWith("Inter-") || line.startsWith(" face"))
      {
        continue;
      }

      final StringTokenizer tokenizer = new StringTokenizer(line, ": \t");
      final String interfaceName = tokenizer.nextToken();

      // Do not collect statistics for the loopback interface unless it is
      // explicitly specified in the configuration.
      if (!isMonitorAllInterfaces() &&
          !getInterfaceNames().contains("lo"))
      {
        continue;
      }

      if (!isMonitorAllInterfaces() &&
          !getInterfaceNames().contains(interfaceName))
      {
        // Ignore this line if this interface is not tracked
        continue;
      }

      stats = getInterfaceStatistics(interfaceName);
      stats.recordReceivedValue(Long.parseLong(tokenizer.nextToken()));

      // Ignore the first few columns.
      for (int i=0; i < 7; i++)
      {
        tokenizer.nextToken();
      }

      stats.recordSentValue(Long.parseLong(tokenizer.nextToken()));
    }
    reader.close();

    if (stats != null)
    {
      stats.completeIteration();
    }
  }

}
