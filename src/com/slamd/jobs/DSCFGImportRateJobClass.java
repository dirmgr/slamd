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
 * Some preexisting portions Copyright (C) 2002-2005 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.PeriodicEventTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD job that has the ability to perform a "dscfg
 * import" operation in the Sun Java System Directory Server 6 and extract
 * statistics from the progress of the import, including the total number of
 * entries processed, the average and recent import rates, and the hit ratio.
 *
 *
 * @author   Neil A. Wilson
 */
public class DSCFGImportRateJobClass
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

  // The parameter that specifies the port for the Directory Server instance
  // into which the data is to be imported.
  private IntegerParameter portParameter =
       new IntegerParameter("port", "Directory Server Port",
                            "The port for the Directory Server instance " +
                            "into which the data should be imported", true, 389,
                            true, 1, true, 65535);

  // The parameter that specifies the password to use to bind to the Directory
  // Server.
  private PasswordParameter bindPWParameter =
       new PasswordParameter("bindpw", "Bind Password",
                             "The password to use to bind to the Directory " +
                             "Server to invoke the import", true, "");

  // The parameter that specifies the DN to use to bind to the Directory Server.
  private StringParameter bindDNParameter =
       new StringParameter("binddn", "Bind DN",
                           "The DN to use to bind to the Directory Server to " +
                           "invoke the import", true, "cn=Directory Manager");

  // The parameter that specifies the path to the dscfg utility.
  private StringParameter dscfgCommandParmeter =
       new StringParameter("dscfg_command", "dscfg Command",
                           "The path to the dscfg command to be executed.",
                           true, "");

  // The parameter that specifies the address of the Directory Server instance
  // into which the data is to be imported.
  private StringParameter hostParameter =
       new StringParameter("host", "Directory Server Address",
                           "The address of the Directory Server instance " +
                           "into which the data should be imported.", true,
                           "127.0.0.1");

  // The parameter that specifies the path to the LDIF file to import.
  private StringParameter ldifFileParameter =
       new StringParameter("ldif_file", "LDIF File",
                           "The path to the LDIF file to be imported.", true,
                           "");

  // The parameter that specifies the DN of the suffix into which the data is to
  // be imported.
  private StringParameter suffixDNParameter =
       new StringParameter("suffix_dn", "Suffix DN",
                           "The DN of the suffix into which the data is " +
                           "to be imported.", true, "");

  // Indicates whether the output of the command should be captured and logged.
  private static boolean logOutput;

  // The port of the Directory Server instance.
  private static int serverPort;

  // The placeholder parameter.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The DN to use to bind to the Directory Server.
  private static String bindDN;

  // The password to use to bind to the Directory Server.
  private static String bindPW;

  // The path to the temporary password file that we have written.
  private static String bindPWFile;

  // The path to the dscfg command to be executed.
  private static String dscfgCommand;

  // The address of the Directory Server instance.
  private static String serverHost;

  // The LDIF file containing the data to import.
  private static String ldifFile;

  // The suffix DN for the database to import.
  private static String suffixDN;

  // The buffer used to hold data read from the process output.
  private byte[] readBuffer;


  // The stat trackers maintained by this job.
  private PeriodicEventTracker entriesProcessed;
  private PeriodicEventTracker averageRate;
  private PeriodicEventTracker recentRate;
  private PeriodicEventTracker hitRatio;


  // The estimated timestamp counter that we should use for the log information,
  // since dscfg doesn't give us actual timestamps that we can use.
  private long timestampCounter;




  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public DSCFGImportRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "dscfg Import Rate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Import LDIF data into Sun DSEE 6.x with the dscfg utility";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to import data from an LDIF file into a 6.x Sun " +
      "Java System Directory Server instance using the dscfg utility."
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
      dscfgCommandParmeter,
      hostParameter,
      portParameter,
      bindDNParameter,
      bindPWParameter,
      ldifFileParameter,
      suffixDNParameter,
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
    dscfgCommand = null;
    dscfgCommandParmeter =
         parameters.getStringParameter(dscfgCommandParmeter.getName());
    if (dscfgCommandParmeter != null)
    {
      dscfgCommand = dscfgCommandParmeter.getStringValue();
    }


    serverHost = "127.0.0.1";
    hostParameter = parameters.getStringParameter(hostParameter.getName());
    if ((hostParameter != null) && hostParameter.hasValue())
    {
      serverHost = hostParameter.getStringValue();
    }


    serverPort = 389;
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if ((portParameter != null) && portParameter.hasValue())
    {
      serverPort = portParameter.getIntValue();
    }


    bindDN = "";
    bindDNParameter = parameters.getStringParameter(bindDNParameter.getName());
    if ((bindDNParameter != null) && bindDNParameter.hasValue())
    {
      bindDN = bindDNParameter.getStringValue();
    }


    bindPW = "";
    bindPWParameter =
         parameters.getPasswordParameter(bindPWParameter.getName());
    if ((bindPWParameter != null) && bindPWParameter.hasValue())
    {
      bindPW = bindPWParameter.getStringValue();
    }


    ldifFile = null;
    ldifFileParameter =
         parameters.getStringParameter(ldifFileParameter.getName());
    if (ldifFileParameter != null)
    {
      ldifFile = ldifFileParameter.getStringValue();
    }


    suffixDN = null;
    suffixDNParameter =
         parameters.getStringParameter(suffixDNParameter.getName());
    if (suffixDNParameter != null)
    {
      suffixDN = suffixDNParameter.getStringValue();
    }


    logOutput = true;
    logOutputParameter =
         parameters.getBooleanParameter(logOutputParameter.getName());
    if (logOutputParameter != null)
    {
      logOutput = logOutputParameter.getBooleanValue();
    }


    // Since we can't specify the bind password directly on the command line,
    // we'll have to write it to a file.  Create a temporary file and put the
    // password in it.  We'll remove that file as soon as the import is done,
    // but mark it "delete on exit" just in case.
    try
    {
      File pwFile = File.createTempFile("dscfg-import-bind-pw-" + getJobID(),
                                        null);
      pwFile.deleteOnExit();

      BufferedWriter writer = new BufferedWriter(new FileWriter(pwFile));
      writer.write(bindPW);
      writer.newLine();
      writer.close();

      bindPWFile = pwFile.getAbsolutePath();
    }
    catch (Exception e)
    {
      throw new UnableToRunException("An error occurred while attempting to " +
                                     "write the bind password file:  " +
                                     stackTraceToString(e), e);
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
      String[] commandArray = { dscfgCommand, "import", "-i", "-c", "-h",
                                serverHost, "-p", String.valueOf(serverPort),
                                "-u", bindDN, "-w", bindPWFile, ldifFile,
                                suffixDN };
      process = runtime.exec(commandArray);
    }
    catch (IOException ioe)
    {
      logMessage("Unable to execute dscfg command:  " + ioe);
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


    timestampCounter = System.currentTimeMillis();

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
      if (line.contains(" -- average rate "))
      {
        return;
      }

      // Since the dscfg command output doesn't contain timestamps, we'll have
      // to guess.  There should be about 20 seconds between each status update
      // line.
      timestampCounter += 20000;

      int entriesStartPos = line.indexOf(" Processed ") + 11;
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
      writeVerbose("Average rate is " + avgRate);
      writeVerbose("Recent rate is " + rctRate);
      writeVerbose("Entries processed is " + numEntries);
      writeVerbose("Hit ratio is " + hitRate);
      averageRate.update(timestampCounter, avgRate);
      recentRate.update(timestampCounter, rctRate);
      entriesProcessed.update(timestampCounter, numEntries);
      hitRatio.update(timestampCounter, hitRate);
    }
    catch (Exception e)
    {
      writeVerbose("Unable to parse line \"" + line + "\" -- " +
                   stackTraceToString(e));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void finalizeClient()
  {
    if (bindPWFile != null)
    {
      try
      {
        File pwFile = new File(bindPWFile);
        pwFile.delete();
      } catch (Exception e) {}
    }
  }
}

