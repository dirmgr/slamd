package com.slamd.resourcemonitor.netstat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * AIX network interface statistics collector.
 */
public class AIXNetStatCollector extends NetStatCollector
{

  // Should only be instantiated by the builder
  AIXNetStatCollector()
  {
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize()
  {
    if (!isMonitorAllInterfaces())
    {
      throw new IllegalArgumentException("Per-interface collection is not" +
          " supported on this OS");
    }
  }



  /**
   * {@inheritDoc}
   */
  public void collect() throws IOException
  {
    // We can get information about TCP network traffic by executing the command
    // "netstat -ss -p tcp" and looking for the lines
    // "# data packets (# bytes)", "# packets (# bytes) received in-sequence",
    // and "# out-of-order packets (# bytes)".  We need to combine the
    // in-sequence and out-of-order bytes to get the bytes received.

    final Process p = Runtime.getRuntime().exec("netstat -ss -p tcp");
    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream()));

    int    countersSeen = 0;
    long   rxTotal      = 0;
    long   txTotal      = 0;
    String line;

    while ((countersSeen < 3) && ((line = reader.readLine()) != null))
    {
      int pos = line.indexOf("data packets (");
      if (pos > 0)
      {
        int startPos = pos + 14;
        int endPos   = line.indexOf(' ', startPos);
        txTotal += Long.parseLong(line.substring(startPos, endPos));
        countersSeen++;
      }
      else if (line.indexOf("received in-sequence") > 0)
      {
        int startPos = line.indexOf('(') + 1;
        int endPos   = line.indexOf(' ', startPos);
        rxTotal += Long.parseLong(line.substring(startPos, endPos));
        countersSeen++;
      }
      else if (line.indexOf("out-of-order packets") > 0)
      {
        int startPos = line.indexOf('(') + 1;
        int endPos   = line.indexOf(' ', startPos);
        rxTotal += Long.parseLong(line.substring(startPos, endPos));
        countersSeen++;
      }
    }

    reader.close();
    p.destroy();

    final InterfaceStatistics stats =
        getInterfaceStatistics(InterfaceStatistics.AGGREGATE_INTERFACE_NAME);

    stats.recordReceivedValue(txTotal);
    stats.recordSentValue(rxTotal);
    stats.completeIteration();
  }

}
