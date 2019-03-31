/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.resourcemonitor.netstat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Windows network statistics collector.
 */
public class WindowsNetStatCollector extends NetStatCollector
{
  /**
   * Creates a new instance of this collector.  This should only be called by
   * the builder.
   */
  WindowsNetStatCollector()
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
  @Override()
  public void collect() throws IOException
  {

    // We can get statistics on a Windows machine by running command
    // "netstat -e" and get the ethernet statistics. We then capture the line
    // containing "Bytes" and split it into substrings
    final Process p = Runtime.getRuntime().exec("netstat -e");
    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream()));

    int    countersSeen  = 0;
    int    x             = 0;
    long   rxTotal       = 0;
    long   txTotal       = 0;
    String line;
    final String[] finalresult = {" "," "," "};

    while ((countersSeen < 1) && ((line = reader.readLine()) != null))
    {
      if (line.startsWith("Bytes"))
      {
        String[] result = line.split("\\s");
        for (int i = 0; i < result.length; i++)
        {
          if ( ! result[i].equals("") )
          {
            finalresult[x] = result[i];
            x++;
          }
        }
        rxTotal += Long.parseLong(finalresult[1]);
        txTotal += Long.parseLong(finalresult[2]);
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
