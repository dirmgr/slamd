package com.slamd.resourcemonitor.netstat;

import com.slamd.job.JobClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;


/**
 * Solaris network interface statistics collector.
 */
public class SolarisNetStatCollector extends NetStatCollector
{
  // Pattern used to split the kstat output
  private static final Pattern LINE_PATTERN = Pattern.compile("[ \t:]");

  // kstat collector script
  private Process collector;

  // Reader for the kstat output
  private BufferedReader outputReader;

  // Thread executing the data collection
  private Thread collectorThread;

  // Exception caught while reading the script input
  private volatile IOException exception;

  // Flag to indicate that the collector thread should stop
  private volatile boolean shouldStop = false;

  // Number of lines expected every time kstat outputs statistics for a
  // time interval
  private int lineCount;

  // Number of lines read from kstat for the current interval.
  private int currentLineCount = 0;



  // Should only be instantiated by the builder
  SolarisNetStatCollector()
  {
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize()
  {
    try
    {
      final StringBuilder buf = new StringBuilder("/usr/bin/kstat -p -c net ");

      // If all interfaces are monitored or
      // more than one interface is monitored, then kstat will collect stats
      // from all interfaces. In the latter case, it will ignore stats from
      // non-monitored interfaces.
      if (isMonitorAllInterfaces() || getInterfaceNames().size() > 1)
      {
        buf.append("::mac:[or]bytes64");
      }
      else
      {
        // Otherwise, just tell kstat to return the stats for a single
        // interface.
        buf.append("::").
            append(getInterfaceNames().iterator().next()).
            append(":[or]bytes64");
      }

      // Find out how many lines kstat would produce without the interval arg.
      populateLineCount(buf.toString());

      // Append the interval argument
      final String kstatCommand =
          buf.append(' ').append(getCollectionIntervalSecs()).toString();


      collector = Runtime.getRuntime().exec(kstatCommand);

    }
    catch (IOException e)
    {
      logMessage("Failed to initialize collector, reason: " +
          e.getLocalizedMessage());
      throw new RuntimeException(e);
    }

    outputReader = new BufferedReader(
        new InputStreamReader(
            this.collector.getInputStream()
        )
    );


    final Thread thread = new Thread(
        new Runnable()
        {
          public void run() {
            String line;

            try
            {
              while (!shouldStop()
                  && (line = outputReader.readLine()) != null)
              {
                parseLine(line);
              }
            }
            catch (IOException e)
            {
              handleException(e);
            }
          }
        }
    );

    thread.setDaemon(true);
    thread.setName("Solaris network statistics collector");
    thread.start();
    collectorThread = thread;
  }



  /**
   * {@inheritDoc}
   */
  public void collect() throws IOException
  {
    //
    // Nothing to do here, since the collection is done in a separate thread.
    //
    // In case the collector thread exited, the code rethrows the exception
    // so that the resource monitor handles this accordingly.
    //
    if (exception != null && !shouldStop())
    {
      final IOException e = exception;
      exception = null;
      throw e;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void stop()
  {
    shouldStop = true;

    // Wait for up to 5 seconds for the collector to stop
    try
    {
      collectorThread.join(5000);
    }
    catch (InterruptedException e)
    {
      // ignore
    }

    if (collectorThread.isAlive())
    {
      collectorThread.interrupt();
    }

    try
    {
      outputReader.close();
    }
    catch (IOException e) {
      // ignore
    }

    collector.destroy();
  }



  /**
   * @return true if the collector thread should stop.
   */
  private boolean shouldStop()
  {
    return shouldStop;
  }



  /**
   * Helper method for the collector thread to save the exception caught
   * while reading the collector script output.
   *
   * @param e   exception caught.
   */
  private void handleException(final IOException e)
  {
    exception = e;
  }



  /**
   * Executes the kstat utility before collection starts in order to determine
   * the number of lines expected in each iteration.
   *
   * @param command      the kstat command to execute.
   *
   * @throws IOException if an error occurs while interacting with the kstat
   *                     utility.
   */
  private void populateLineCount(final String command) throws IOException
  {
    final Process p =
        Runtime.getRuntime().exec(command);

    final BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                p.getInputStream()
            )
        );

    int lineCount = 0;
    while (reader.readLine() != null)
    {
      lineCount++;
    }

    reader.close();
    p.destroy();

    this.lineCount = lineCount;
  }



  /**
   * Parses the kstat output and saves the collected statistic.
   *
   * @param line   line to parse
   */
  private void parseLine(final String line)
  {
    // kstat puts an empty line between iterations
    if (line.length() == 0)
    {
      return;
    }

    // each line is expected to be in the form of
    //
    // moduleName:instanceName:name:statistic value
    //
    // e.g.
    // e1000g:0:e1000g0:obytes64       2070825856
    //

    final String[] fields = LINE_PATTERN.split(line);
    assert fields.length == 5;

    final String moduleName   = fields[0];
    final String instanceName = fields[1];
    final String name         = fields[2];
    final String statistic    = fields[3];
    final String value        = fields[4];

    final String interfaceName;
    if ("mac".equals(name))
    {
      interfaceName = moduleName + instanceName;
    }
    else
    {
      interfaceName = name;
    }

    boolean skipParsing = false;

    if (!isMonitorAllInterfaces() &&
        !getInterfaceNames().contains(interfaceName))
    {
      // Ignore this line if this interface is not tracked
      skipParsing = true;
    }

    InterfaceStatistics stats = getInterfaceStatistics(interfaceName);
    if (stats == null)
    {
      return;
    }

    if (!skipParsing)
    {
      if ("obytes64".equals(statistic))
      {
        try
        {
          stats.recordSentValue(Long.parseLong(value));
        }
        catch (Exception e)
        {
          // This should never happen.
          writeVerbose("Unable to determine obytes64 for interface " +
              interfaceName + " from kstat output:  " +
              JobClass.stackTraceToString(e));
        }


      }
      else if ("rbytes64".equals(statistic))
      {
        try
        {
          stats.recordReceivedValue(Long.parseLong(value));
        }
        catch (Exception e)
        {
          // This should never happen.
          writeVerbose("Unable to determine rbytes64 for interface " +
              interfaceName + " from kstat output:  " +
              JobClass.stackTraceToString(e));
        }
      }
    }

    if (++currentLineCount == lineCount)
    {
      // All output lines for this time interval have been processed
      stats.completeIteration();
      currentLineCount = 0;
    }
  }

}
