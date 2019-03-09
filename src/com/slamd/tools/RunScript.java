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
package com.slamd.tools;



import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import com.slamd.client.ClientMessageWriter;
import com.slamd.client.ClientSideJob;
import com.slamd.common.Constants;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.scripting.ScriptedJobClass;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.ScriptParser;
import com.slamd.stat.StatTracker;



/**
 * This class defines a specialized SLAMD client that may be used to execute
 * SLAMD scripts without the need for a SLAMD server to be running and without
 * the need for a configuration file as would be used by the standalone client.
 *
 *
 * @author   Neil A. Wilson
 */
public class RunScript
       implements ClientMessageWriter
{
  // Indicates whether the data collected by each thread should be aggregated
  // before reporting to the end user.
  private boolean aggregateThreadData;

  // Indicates whether the script should be executed in debug mode.
  private boolean debugMode;

  // Indicates whether the script file should only be validated but not actually
  // executed.
  private boolean validateOnly;

  // Indicates whether the script should be executed in verbose mode.
  private boolean verboseMode;

  // The length of time (in seconds) to use as the statistics collection
  // interval.
  private int collectionInterval;

  // The maximum length of time (in seconds) that the script should be allowed
  // to run.
  private int duration;

  // The number of threads to use when running the job.
  private int numThreads;

  // The scripted job class used to get parameter information.
  private ScriptedJobClass scriptedJob;

  // The path to the script file.
  private String scriptFile;

  // The end-of-line character for this platform.
  private String eol = Constants.EOL;

  // The set of arguments provided to the script.
  private String[] scriptArguments;



  /**
   * Create a new standalone client instance and pass all the arguments to it.
   *
   * @param  args  The set of arguments provided on the command line.
   */
  public static void main(String[] args)
  {
    new RunScript(args);
  }



  /**
   * Create a new standalone client instance, process the configuration, and
   * run the specified job.
   *
   * @param  args  The set of arguments provided on the command line.
   */
  public RunScript(String[] args)
  {
    // Set default values for all the parameters.
    aggregateThreadData  = false;
    debugMode            = false;
    validateOnly         = false;
    scriptFile           = null;
    duration             = 0;
    collectionInterval   = Constants.DEFAULT_COLLECTION_INTERVAL;
    numThreads           = 1;


    // Create an array list to hold the script arguments.
    ArrayList<String> argList = new ArrayList<String>();


    // Parse the command-line arguments
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-d"))
      {
        try
        {
          duration = Integer.parseInt(args[++i]);
        }
        catch (NumberFormatException nfe)
        {
          System.err.println("ERROR:  Duration must be an integer");
          displayUsage();
          System.exit(1);
        }
      }
      else if (args[i].equals("-i"))
      {
        try
        {
          collectionInterval = Integer.parseInt(args[++i]);
        }
        catch (NumberFormatException nfe)
        {
          System.err.println("ERROR:  Statistics collection interval must be " +
                             "an integer");
          displayUsage();
          System.exit(1);
        }
      }
      else if (args[i].equals("-t"))
      {
        try
        {
          numThreads = Integer.parseInt(args[++i]);
        }
        catch (NumberFormatException nfe)
        {
          System.err.println("ERROR:  Number of threads must be an integer");
          displayUsage();
          System.exit(1);
        }
      }
      else if (args[i].equals("-a"))
      {
        aggregateThreadData = true;
      }
      else if (args[i].equals("-D"))
      {
        debugMode   = true;
        verboseMode = true;
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
      }
      else if (args[i].equals("-V"))
      {
        validateOnly = true;
      }
      else if (args[i].equals("-h"))
      {
        displayUsage();
        System.exit(0);
      }
      else if (args[i].startsWith("-"))
      {
        System.err.println("ERROR:  Unrecognized parameter " + args[i]);
        displayUsage();
        System.exit(1);
      }
      else
      {
        if (scriptFile == null)
        {
          scriptFile = args[i];
        }
        else
        {
          argList.add(args[i]);
        }
      }
    }


    // Make sure that the required parameters were given values
    if (scriptFile == null)
    {
      System.err.println("ERROR:  No script file specified");
      displayUsage();
      System.exit(1);
    }


    // Convert the argument list to an array.
    scriptArguments = new String[argList.size()];
    argList.toArray(scriptArguments);


    // Make sure that the script file given is an absolute path.
    File scriptFileObject = new File(scriptFile);
    URL scriptFileURL = null;
    if (! (scriptFileObject.exists() && scriptFileObject.canRead()))
    {
      System.err.println("Script file \"" + scriptFile +
                         "\" does not exist or is not readable.");
      System.exit(1);
    }
    else
    {
      try
      {
        scriptFileURL = new URL("file:" + scriptFileObject.getAbsolutePath());
      }
      catch (Exception e)
      {
        System.err.println("Unable to create script file URL:  " + e);

        if (debugMode)
        {
          e.printStackTrace();
        }
      }
    }


    // Read and parse the script file.
    ScriptParser scriptParser = null;
    try
    {
      scriptParser = new ScriptParser();
    }
    catch (ScriptException se)
    {
      System.err.println("ERROR:  Unable to instantiate the script parser -- " +
                         se);
      if (debugMode)
      {
        se.printStackTrace();
      }

      System.exit(1);
    }

    try
    {
      scriptParser.read(scriptFile);
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Unable to read script file \"" + scriptFile +
                         "\" -- " + ioe);
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.exit(1);
    }

    try
    {
      scriptParser.parse();
    }
    catch (ScriptException se)
    {
      System.err.println("ERROR:  Unable to parse script file \"" + scriptFile +
                         "\" -- " + se.getMessage());

      if (debugMode)
      {
        se.printStackTrace();
      }

      System.exit(1);
    }


    if (validateOnly)
    {
      System.out.println("No errors found in script file \"" + scriptFile +
                         '"');
      System.exit(0);
    }


    // Load and verify the job class.
    scriptedJob = new ScriptedJobClass();


    // Convert the command-line arguments to a set of parameters for the job.
    BooleanParameter debugModeParameter =
         new BooleanParameter(ScriptedJobClass.DEBUG_PARAMETER_NAME, debugMode);
    FileURLParameter scriptURLParameter =
         new FileURLParameter(ScriptedJobClass.SCRIPT_FILE_PARAMETER_NAME,
                              null, scriptFileURL);
    MultiLineTextParameter scriptArgsParameter =
         new MultiLineTextParameter(
                  ScriptedJobClass.SCRIPT_ARGUMENTS_PARAMETER_NAME,
                  scriptArguments);
    Parameter[] jobParams = new Parameter[]
    {
      debugModeParameter,
      scriptURLParameter,
      scriptArgsParameter
    };
    ParameterList parameters = new ParameterList(jobParams);


    // Create a new client side job to actually do the processing.
    ClientSideJob clientJob =
         new ClientSideJob(this, null, scriptedJob.getClass().getName(),
                           numThreads, duration, collectionInterval, parameters,
                           false, false, null);


    // Start the job and wait for it to complete.
    writeMessage("Starting script execution....");
    clientJob.startAndWait();


    // Print out information about the job.
    writeMessage("Job Processing Complete");
    int jobDuration = clientJob.getActualDuration();
    writeMessage("Job Processing Time:  " + jobDuration + " seconds");
    StatTracker[] statTrackers = clientJob.getStatTrackers(aggregateThreadData);
    for (int i=0; i < statTrackers.length; i++)
    {
      System.out.println();
      System.out.println(statTrackers[i].getDisplayName() + " -- Thread " +
                         statTrackers[i].getThreadID());
      if (debugMode)
      {
        System.out.println(statTrackers[i].getDetailString());
      }
      else
      {
        System.out.println(statTrackers[i].getSummaryString());
      }
    }
  }



  /**
   * Writes usage information for this program to standard error.
   */
  public void displayUsage()
  {
    System.err.println(
"Usage:  java RunScript [options] scriptFile scriptArgs" + eol +
"        where [options] include:" + eol +
"-d {value}  -- Specifies the maximum length of time the job should run" + eol +
"-i {value}  -- Specifies the length of time in seconds that should be" + eol +
"               used as the statistics collection interval" + eol +
"-t {value}  -- Specifies the number of threads that should be used" + eol +
"-a          -- Specifies that data from each of the threads should be" + eol +
"               aggregated before displaying the results" + eol +
"-D          -- Indicates that the script should be executed in debug" + eol +
"               mode.  This also implies verbose mode" + eol +
"-v          -- Indicates that the script should be executed in verbose" + eol +
"               (but not debug) mode" + eol +
"-V          -- Indicates that the script should be validated but not" + eol +
"               executed" + eol +
"-h          -- Displays usage information for this program"
                      );
  }



  /**
   * Writes information logged during job processing to standard output.
   *
   * @param  message  The message to be written to standard output.
   */
  public void writeMessage(String message)
  {
    System.out.println(message);
    System.out.flush();
  }



  /**
   * Writes verbose information logged during job processing to standard
   * output (if verbose logging is enabled).
   *
   * @param  message  The message to be written to standard output.
   */
  public void writeVerbose(String message)
  {
    if (verboseMode)
    {
      System.out.println(message);
    }
  }



  /**
   * Indicates whether the message writer is using verbose mode and therefore
   * will display messages written with the <CODE>writeVerbose</CODE> method.
   *
   * @return  <CODE>true</CODE> if the message writer is using verbose mode, or
   *          <CODE>false</CODE> if not.
   */
  public boolean usingVerboseMode()
  {
    return verboseMode;
  }
}

