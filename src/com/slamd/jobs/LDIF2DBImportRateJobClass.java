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
package com.slamd.jobs;



import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.PeriodicEventTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD job that has the ability to perform an ldif2db
 * import for the Sun ONE Directory Server and extract statistics from the
 * progress of the import, including the total number of entries processed,
 * the average and recent import rates, and the hit ratio.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIF2DBImportRateJobClass
       extends JobClass
{
  /**
   * The display name of the stat tracker used to keep track of the average
   * import rate.
   */
  public static final String STAT_TRACKER_AVERAGE_RATE =
       "Average Import Rate (Entries/Second)";



  /**
   * The display name of the stat tracker used to keep track of the recent
   * import rate.
   */
  public static final String STAT_TRACKER_RECENT_RATE =
       "Recent Import Rate (Entries/Second)";



  /**
   * The display name of the stat tracker used to keep track of the number of
   * entries processed.
   */
  public static final String STAT_TRACKER_ENTRIES_PROCESSED =
       "Total Entries Processed";



  /**
   * The display name of the stat tracker used to keep track of the hit ratio.
   */
  public static final String STAT_TRACKER_HIT_RATIO = "Hit Ratio (Percent)";



  /**
   * The size to use for the read buffer.
   */
  public static final int READ_BUFFER_SIZE = 4096;



  // The parameter that indicates whether to log command output.
  private BooleanParameter logOutputParameter =
       new BooleanParameter("log_output", "Log Command Output",
                            "Indicates whether the ldif2db output should be " +
                            "logged.", true);

  // The parameter that specifies the name of the database into which the data
  // is to be imported.
  private StringParameter dbNameParameter =
       new StringParameter("db_name", "Database Name",
                           "The name of the database into which the data is " +
                           "to be imported.", true, "userRoot");

  // The parameter that specifies the path to the ldif2db script.
  private StringParameter ldif2dbCommandParmeter =
       new StringParameter("ldif2db_command", "ldif2db Command",
                           "The path to the ldif2db command to be executed.",
                           true, "");

  // The parameter that specifies the path to the LDIF file to import.
  private StringParameter ldifFileParameter =
       new StringParameter("ldif_file", "LDIF File",
                           "The path to the LDIF file to be imported.", true,
                           "");

  // Indicates whether the output of the command should be captured and logged.
  private static boolean logOutput;

  // The placeholder parameter.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The name of the database into which to import the LDIF file.
  private static String dbName;

  // The command to be executed.
  private static String ldif2dbCommand;

  // The LDIF file containing the data to import.
  private static String ldifFile;

  // The buffer used to hold data read from the process output.
  private byte[] readBuffer;


  // The stat trackers maintained by this job.
  private PeriodicEventTracker entriesProcessed;
  private PeriodicEventTracker averageRate;
  private PeriodicEventTracker recentRate;
  private PeriodicEventTracker hitRatio;


  // The date format object that will be used to parse the date as it is written
  // to the log file.
  private SimpleDateFormat dateFormat;




  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public LDIF2DBImportRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "ldif2db Import Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Import LDIF data into Sun DSEE with the ldif2db utility";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to import data from an LDIF file into a Sun " +
      "Java System Directory Server instance using the ldif2db utility."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "LDAP";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int overrideNumClients()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int overrideThreadsPerClient()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    Parameter[] parameters = new Parameter[]
    {
      placeholder,
      ldif2dbCommandParmeter,
      dbNameParameter,
      ldifFileParameter,
      logOutputParameter
    };

    return new ParameterList(parameters);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[]
    {
      new PeriodicEventTracker(clientID, threadID, STAT_TRACKER_AVERAGE_RATE,
                               collectionInterval),
      new PeriodicEventTracker(clientID, threadID, STAT_TRACKER_RECENT_RATE,
                               collectionInterval),
      new PeriodicEventTracker(clientID, threadID,
                               STAT_TRACKER_ENTRIES_PROCESSED,
                               collectionInterval),
      new PeriodicEventTracker(clientID, threadID, STAT_TRACKER_HIT_RATIO,
                               collectionInterval),
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[]
    {
      averageRate,
      recentRate,
      entriesProcessed,
      hitRatio
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    ldif2dbCommand = null;
    ldif2dbCommandParmeter =
         parameters.getStringParameter(ldif2dbCommandParmeter.getName());
    if (ldif2dbCommandParmeter != null)
    {
      ldif2dbCommand = ldif2dbCommandParmeter.getStringValue();
    }


    ldifFile = null;
    ldifFileParameter =
         parameters.getStringParameter(ldifFileParameter.getName());
    if (ldifFileParameter != null)
    {
      ldifFile = ldifFileParameter.getStringValue();
    }


    dbName = null;
    dbNameParameter = parameters.getStringParameter(dbNameParameter.getName());
    if (dbNameParameter != null)
    {
      dbName = dbNameParameter.getStringValue();
    }


    logOutput = true;
    logOutputParameter =
         parameters.getBooleanParameter(logOutputParameter.getName());
    if (logOutputParameter != null)
    {
      logOutput = logOutputParameter.getBooleanValue();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // Create the stat trackers.
    averageRate      = new PeriodicEventTracker(clientID, threadID,
                                                STAT_TRACKER_AVERAGE_RATE,
                                                collectionInterval);
    recentRate       = new PeriodicEventTracker(clientID, threadID,
                                                STAT_TRACKER_RECENT_RATE,
                                                collectionInterval);
    entriesProcessed = new PeriodicEventTracker(clientID, threadID,
                                                STAT_TRACKER_ENTRIES_PROCESSED,
                                                collectionInterval);
    hitRatio         = new PeriodicEventTracker(clientID, threadID,
                                                STAT_TRACKER_HIT_RATIO,
                                                collectionInterval);

    averageRate.setFlatBetweenPoints(false);
    recentRate.setFlatBetweenPoints(false);
    entriesProcessed.setFlatBetweenPoints(false);
    hitRatio.setFlatBetweenPoints(false);


    // Initialize the read buffer.
    readBuffer = new byte[READ_BUFFER_SIZE];


    // Initialize the date format.
    dateFormat = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]");
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    Runtime runtime = Runtime.getRuntime();
    Process process = null;

    try
    {
      String[] commandArray = { ldif2dbCommand, "-n", dbName, "-i", ldifFile };
      process = runtime.exec(commandArray);
    }
    catch (IOException ioe)
    {
      logMessage("Unable to execute ldif2db command \"" + ldif2dbCommand +
                 " -n " + dbName + " -i " + ldifFile + "\":  " + ioe);
      indicateStoppedDueToError();
      return;
    }


    BufferedInputStream stdOutStream =
         new BufferedInputStream(process.getInputStream());
    BufferedInputStream stdErrStream =
         new BufferedInputStream(process.getErrorStream());

    averageRate.startTracker();
    recentRate.startTracker();
    entriesProcessed.startTracker();
    hitRatio.startTracker();


    while (true)
    {
      try
      {
        if (stdOutStream.available() > 0)
        {
          while ((! shouldStop()) && (stdOutStream.available() > 0))
          {
            int bytesRead = stdOutStream.read(readBuffer);
            String[] outputStrs = byteArrayToStrings(readBuffer, bytesRead);
            for (int i=0; i < outputStrs.length; i++)
            {
              if (logOutput)
              {
                logMessage("STDOUT:  " + outputStrs[i]);
              }

              processLine(outputStrs[i]);
            }
          }
        }

        if (stdErrStream.available() > 0)
        {
          while ((! shouldStop()) && (stdErrStream.available() > 0))
          {
            int bytesRead = stdErrStream.read(readBuffer);
            String[] errorStrs = byteArrayToStrings(readBuffer, bytesRead);
            for (int i=0; i < errorStrs.length; i++)
            {
              if (logOutput)
              {
                logMessage("STDERR:  " + errorStrs[i]);
              }

              processLine(errorStrs[i]);
            }
          }
        }

        if (shouldStop())
        {
          try
          {
            stdOutStream.close();
            stdErrStream.close();
          } catch (Exception e) {}

          process.destroy();
          logMessage("Terminated process because the client determined it " +
                     "should stop running.");
          break;
        }

        try
        {
          int returnCode = process.exitValue();
          if (returnCode == 0)
          {
            logMessage("Command completed successfully (exit code 0)");
          }
          else
          {
            logMessage("Command completed abnormally (exit code " +
                       returnCode + ')');
            indicateCompletedWithErrors();
          }

          try
          {
            stdOutStream.close();
            stdErrStream.close();
          } catch (Exception e) {}

          break;
        } catch (IllegalThreadStateException itse) {}

        try
        {
          Thread.sleep(100);
        } catch (InterruptedException ie) {}
      }
      catch (IOException ioe)
      {
        // This could mean that the command is done or that some other error
        // occurred.  Try to get the return code to see if it completed.
        boolean completedSuccessfully = false;
        try
        {
          int returnCode = process.exitValue();
          completedSuccessfully = (returnCode == 0);
          if (completedSuccessfully)
          {
            logMessage("Command completed successfully (exit code 0)");
          }
          else
          {
            logMessage("Command completed abnormally (exit code " + returnCode +
                       ')');
            indicateCompletedWithErrors();
          }
        }
        catch (IllegalThreadStateException itse)
        {
          logMessage("Attempt to read process output failed:  " + ioe);
          indicateCompletedWithErrors();
        }

        break;
      }
    }


    averageRate.stopTracker();
    recentRate.stopTracker();
    entriesProcessed.stopTracker();
    hitRatio.stopTracker();
  }



  /**
   * Converts the provided byte array into an array of strings, with one string
   * per line.
   *
   * @param  byteArray  The byte array containing the data to convert to an
   *                    array of strings.
   * @param  length     The number of bytes to actually use in the byte array.
   *
   * @return  The array of strings containing the data from the provided byte
   *          array.
   */
  private static String[] byteArrayToStrings(byte[] byteArray, int length)
  {
    ArrayList<String> stringList = new ArrayList<String>();

    String byteStr = new String(byteArray, 0, length);
    StringTokenizer tokenizer = new StringTokenizer(byteStr, "\r\n");
    while (tokenizer.hasMoreTokens())
    {
      stringList.add(tokenizer.nextToken());
    }

    String[] returnStrings = new String[stringList.size()];
    stringList.toArray(returnStrings);
    return returnStrings;
  }



  /**
   * Processes the provided line of output from the ldif2db command and extracts
   * information about the progress of the import, if the line contains the
   * appropriate information.
   *
   * @param  line  The line of output captured from ldif2db.
   */
  private void processLine(String line)
  {
    try
    {
      if (! line.contains(" -- average rate "))
      {
        return;
      }

      int    closeBracketPos = line.indexOf(']');
      String dateStr         = line.substring(0, closeBracketPos+1);
      Date   logDate         = dateFormat.parse(dateStr);
      long   logTime         = logDate.getTime();


      int entriesStartPos = line.indexOf(": Processed ", closeBracketPos) + 12;
      int entriesEndPos   = line.indexOf(" entries", entriesStartPos);
      int numEntries      = Integer.parseInt(line.substring(entriesStartPos,
                                                            entriesEndPos));

      int    avgStartPos = line.indexOf(" -- average rate ", entriesEndPos) +
                           17;
      int    avgEndPos   = line.indexOf("/sec", avgStartPos);
      double avgRate     = Double.parseDouble(line.substring(avgStartPos,
                                                             avgEndPos));

      int    rctStartPos = line.indexOf(", recent rate ", avgEndPos) + 14;
      int    rctEndPos   = line.indexOf("/sec", rctStartPos);
      double rctRate     = Double.parseDouble(line.substring(rctStartPos,
                                                             rctEndPos));

      int    hitStartPos = line.indexOf(", hit ratio ", rctEndPos) + 12;
      int    hitEndPos   = line.indexOf('%', hitStartPos);
      double hitRate     = Double.parseDouble(line.substring(hitStartPos,
                                                             hitEndPos));

      writeVerbose("Line is \"" + line + '"');
      writeVerbose("Timestamp is " + logDate);
      writeVerbose("Average rate is " + avgRate);
      writeVerbose("Recent rate is " + rctRate);
      writeVerbose("Entries processed is " + numEntries);
      writeVerbose("Hit ratio is " + hitRate);
      averageRate.update(logTime, avgRate);
      recentRate.update(logTime, rctRate);
      entriesProcessed.update(logTime, numEntries);
      hitRatio.update(logTime, hitRate);
    }
    catch (Exception e)
    {
      writeVerbose("Unable to parse line \"" + line + "\" -- " +
                   stackTraceToString(e));
    }
  }
}

