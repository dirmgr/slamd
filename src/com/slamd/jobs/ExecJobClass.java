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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.StatTracker;



/**
 * This class defines a job that has the ability to execute a command on the
 * client system.
 *
 *
 * @author   Neil A. Wilson
 */
public class ExecJobClass
       extends JobClass
{
  /**
   * The size to use for the read buffer.
   */
  public static final int READ_BUFFER_SIZE = 4096;



  // The parameter that indicates whether to log command output.
  private BooleanParameter logOutputParameter =
       new BooleanParameter("log_output", "Log Command Output",
                            "Indicates whether the output of the command " +
                            "should be logged.", false);

  // Specifies the list of environment variables to define for the job
  // execution.
  private MultiLineTextParameter environmentParameter =
       new MultiLineTextParameter("env_variables", "Environment Variables",
                                  "A set of environment variables that " +
                                  "should be defined when the job is " +
                                  "executed.  The environment variables " +
                                  "should be specified one per line, using " +
                                  "the format name=value", null, false);

  // The parameter that specifies the command to execute.
  private StringParameter commandParameter =
      new StringParameter("command", "Command to Execute",
                          "Specifies the command to execute.", true, "");

  // THe parameter that specifies the working directory.
  private StringParameter workingDirParameter =
       new StringParameter("working_dir", "Working Directory",
                           "The path to the working directory to use when " +
                           "executing the command.", false, "");

  // Indicates whether the output of the command should be captured and logged.
  private static boolean logOutput;

  // The placeholder parameter.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The command to be executed.
  private static String command;

  // The path to the working directory for the job.
  private static File workingDir;

  // The environment variables to use for the job.
  private static String[] environmentVariables;

  // The buffer used to hold data read from the process output.
  private byte[] readBuffer;



  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public ExecJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Exec";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Execute a command on client systems.";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to execute a specified command on client " +
      "systems.  If desired, the output of the command can be captured and " +
      "logged as part of the job data."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "Utility";
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
      commandParameter,
      workingDirParameter,
      environmentParameter,
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
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void validateJobInfo(int numClients, int threadsPerClient,
                              int threadStartupDelay, Date startTime,
                              Date stopTime, int duration,
                              int collectionInterval, ParameterList parameters)
         throws InvalidValueException
  {
    // See if the environment variables were specified.  If so, make sure they
    // were specified properly.
    MultiLineTextParameter envParameter =
         parameters.getMultiLineTextParameter(environmentParameter.getName());
    if (envParameter != null)
    {
      String[] lines = envParameter.getNonBlankLines();
      for (int i=0; ((lines != null) && (i < lines.length)); i++)
      {
        if (lines[i].indexOf('=') <= 0)
        {
          throw new InvalidValueException("Invalid environment variable " +
                                          "specified:  \"" + lines[i] +
                                          "\".  Expected {name}={value}");
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    command = null;
    commandParameter =
         parameters.getStringParameter(commandParameter.getName());
    if (commandParameter != null)
    {
      command = commandParameter.getStringValue();
    }


    workingDir = null;
    workingDirParameter =
         parameters.getStringParameter(workingDirParameter.getName());
    if ((workingDirParameter != null) && (workingDirParameter.hasValue()))
    {
      String workingDirStr = workingDirParameter.getStringValue();
      workingDir = new File(workingDirStr);

      try
      {
        if (! workingDir.exists())
        {
          throw new UnableToRunException("Working directory \"" +
                                         workingDirStr + "\" does not exist.");
        }

        if (! workingDir.isDirectory())
        {
          throw new UnableToRunException("Working directory \"" +
                                         workingDirStr +
                                         "\" is not a directory.");
        }
      }
      catch (UnableToRunException utre)
      {
        throw utre;
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to verify existence of " +
                                       "working directory \"" + workingDirStr +
                                       "\":  " + e, e);
      }
    }


    environmentVariables = null;
    environmentParameter =
         parameters.getMultiLineTextParameter(environmentParameter.getName());
    if (environmentParameter != null)
    {
      environmentVariables = environmentParameter.getNonBlankLines();
    }


    logOutput = false;
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
      process = runtime.exec(command, environmentVariables, workingDir);
    }
    catch (IOException ioe)
    {
      logMessage("Unable to execute command \"" + command + "\":  " + ioe);
      indicateStoppedDueToError();
      return;
    }


    BufferedInputStream stdOutStream =
         new BufferedInputStream(process.getInputStream());
    BufferedInputStream stdErrStream =
         new BufferedInputStream(process.getErrorStream());


    while (true)
    {
      try
      {
        if (logOutput)
        {
          if (stdOutStream.available() > 0)
          {
            while ((! shouldStop()) && (stdOutStream.available() > 0))
            {
              int bytesRead = stdOutStream.read(readBuffer);
              String[] outputStrs = byteArrayToStrings(readBuffer, bytesRead);
              for (int i=0; i < outputStrs.length; i++)
              {
                logMessage("STDOUT:  " + outputStrs[i]);
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
                logMessage("STDERR:  " + errorStrs[i]);
              }
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
          return;
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

          return;
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

        return;
      }
    }
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
}

