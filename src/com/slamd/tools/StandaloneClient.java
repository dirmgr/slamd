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



import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;

import com.slamd.job.UnableToRunException;
import com.slamd.client.ClientMessageWriter;
import com.slamd.client.ClientSideJob;
import com.slamd.common.Constants;
import com.slamd.common.JobClassLoader;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FloatParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD client that may execute SLAMD jobs without the
 * need for a SLAMD server to be running.  It allows a single client to execute
 * a job from the command line with output sent to standard output and standard
 * error.  The configuration for the job should be specified in a configuration
 * file.
 *
 *
 * @author   Neil A. Wilson
 */
public class StandaloneClient
       implements ClientMessageWriter
{
  // Indicates whether the data collected by each thread should be aggregated
  // before reporting to the end user.
  private boolean aggregateThreadData;

  // Indicates whether the program is operating in a mode in which it will
  // generate a sample configuration file
  private boolean generateConfigMode;

  // Indicates whether verbose job information should be written out.
  private boolean verboseMode;

  // Indicates whether to use the custom job class loader.
  private boolean useCustomClassLoader;

  // The length of time (in seconds) to use as the statistics collection
  // interval.
  private int collectionInterval;

  // The maximum length of time (in seconds) that the job should be allowed to
  // run.
  private int duration;

  // The number of threads to use when running the job.
  private int numThreads;

  // The job class used to get parameter information.
  private JobClass jobInstance;

  // The writer to which output will be sent.
  private PrintWriter outputWriter;

  // The information read from the configuration file.
  private Properties configProperties;

  // The location of the job class files.
  private String classPath;

  // The path to the configuration file.
  private String configFile;

  // The end-of-line character for this platform.
  private String eol = Constants.EOL;

  // The name of the file to which all output should be written.
  private String outputFile;

  // The name of the job class to run.
  private String jobClassName;



  /**
   * Create a new standalone client instance and pass all the arguments to it.
   *
   * @param  args  The set of arguments provided on the command line.
   */
  public static void main(String[] args)
  {
    new StandaloneClient(args);
  }



  /**
   * Create a new standalone client instance, process the configuration, and
   * run the specified job.
   *
   * @param  args  The set of arguments provided on the command line.
   */
  public StandaloneClient(String[] args)
  {
    // Set default values for all the parameters.
    aggregateThreadData  = false;
    generateConfigMode   = false;
    verboseMode          = false;
    useCustomClassLoader = true;
    classPath            = null;
    configFile           = null;
    outputFile           = null;
    duration             = 0;
    collectionInterval   = Constants.DEFAULT_COLLECTION_INTERVAL;
    numThreads           = 1;


    // Parse the command-line arguments
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-f"))
      {
        configFile = args[++i];
      }
      else if (args[i].equals("-o"))
      {
        outputFile = args[++i];
      }
      else if (args[i].equals("-c"))
      {
        classPath = args[++i];
      }
      else if (args[i].equals("-g"))
      {
        generateConfigMode = true;
        jobClassName = args[++i];
      }
      else if (args[i].equals("-d"))
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
      else if (args[i].equals("-L"))
      {
        useCustomClassLoader = false;
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
      }
      else if (args[i].equals("-h"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("ERROR:  Unrecognized parameter " + args[i]);
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that the required parameters were given values
    if (configFile == null)
    {
      System.err.println("ERROR:  No job configuration file specified");
      displayUsage();
      System.exit(1);
    }


    // If there is an output file specified, then open it for writing.
    // Otherwise, send it to standard out.
    if (outputFile == null)
    {
      outputWriter = new PrintWriter(System.out);
    }
    else
    {
      try
      {
        outputWriter = new PrintWriter(new FileWriter(outputFile, true));
      }
      catch (IOException ioe)
      {
        System.err.println("ERROR:  Unable to open output file " + outputFile +
                           " for writing:  " + ioe);
        System.exit(1);
      }
    }


    // If the program is to run in "generate config" mode, then do that now
    if (generateConfigMode)
    {
      if (jobClassName == null)
      {
        System.err.println("ERROR:  No job class name provided");
        displayUsage();
        System.exit(1);
      }
      generateConfigFile();
      System.exit(0);
    }


    // Parse the configuration file.
    configProperties = new Properties();
    try
    {
      BufferedInputStream inputStream =
           new BufferedInputStream(new FileInputStream(configFile));
      configProperties.load(inputStream);
      inputStream.close();
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Could not read configuration file:  " + ioe);
      System.exit(1);
    }


    // Load and verify the job class.
    jobClassName =
         configProperties.getProperty(Constants.SERVLET_PARAM_JOB_CLASS);
    if (jobClassName == null)
    {
      System.err.println("ERROR:  Configuration file " + configFile +
                         "does not specify a job class using the parameter " +
                         Constants.SERVLET_PARAM_JOB_CLASS);
      System.exit(1);
    }

    if (useCustomClassLoader)
    {
      JobClassLoader jobClassLoader =
           new JobClassLoader(getClass().getClassLoader(), classPath);
      try
      {
        jobInstance = jobClassLoader.getJobClass(jobClassName);
      }
      catch (SLAMDException se)
      {
        System.err.println(se.getMessage());
        System.exit(1);
      }
    }
    else
    {
      try
      {
        Class jobClass = Constants.classForName(jobClassName);
        jobInstance = (JobClass) jobClass.newInstance();
      }
      catch (Exception e)
      {
        System.err.println("Unable to load job class " + jobClassName + ":  " +
                           e);
        System.exit(1);
      }
    }


    // Get the parameter stubs from the job class and provide them with values
    // from the config file.
    Parameter[] jobParams = jobInstance.getParameterStubs().getParameters();
    for (int i=0; i < jobParams.length; i++)
    {
      String valueString = configProperties.getProperty(jobParams[i].getName());
      try
      {
        jobParams[i].setValueFromString(valueString);
      }
      catch (InvalidValueException ive)
      {
        System.err.println("Invalid value specified for job parameter " +
                           jobParams[i].getName() + " -- " +
                           ive.getMessage());
        System.exit(1);
      }
    }
    ParameterList parameters = new ParameterList(jobParams);


    // Make sure that the job as a whole is acceptable.
    try
    {
      jobInstance.validateJobInfo(1, numThreads, 0, new Date(), null, duration,
                                  collectionInterval, parameters);
    }
    catch (InvalidValueException ive)
    {
      System.err.println("Job parameter validation failed -- " +
                         ive.getMessage());
      System.exit(1);
    }


    // Perform the job-level initialization that is normally done by the
    // server-side job.
    try
    {
      jobInstance.initializeJob(parameters);
    }
    catch (UnableToRunException utre)
    {
      System.err.println("Job initialization failed -- " + utre.getMessage());
      System.exit(1);
    }


    // Create a new client side job to actually do the processing.
    ClientSideJob clientJob = new ClientSideJob(this, classPath, jobClassName,
                                                numThreads, duration,
                                                collectionInterval, parameters,
                                                useCustomClassLoader, false,
                                                null);


    // Start the job and wait for it to complete.
    writeMessage("Starting the " + jobInstance.getJobName() + " job....");
    clientJob.startAndWait();


    // Run the per-job finalization.
    jobInstance.finalizeJob();


    // Print out information about the job.
    writeMessage("Job Processing Complete");
    int jobDuration = clientJob.getActualDuration();
    writeMessage("Job Processing Time:  " + jobDuration + " seconds");
    StatTracker[] statTrackers = clientJob.getStatTrackers(aggregateThreadData);
    for (int i=0; i < statTrackers.length; i++)
    {
      outputWriter.println();
      outputWriter.println(statTrackers[i].getDisplayName() + " -- Thread " +
                           statTrackers[i].getThreadID());
      if (verboseMode)
      {
        outputWriter.println(statTrackers[i].getDetailString());
      }
      else
      {
        outputWriter.println(statTrackers[i].getSummaryString());
      }
    }

    outputWriter.flush();
    outputWriter.close();
  }



  /**
   * Generates a configuration file that can be used to run the specified
   * job.
   */
  public void generateConfigFile()
  {
    if (useCustomClassLoader)
    {
      try
      {
        JobClassLoader classLoader =
             new JobClassLoader(getClass().getClassLoader(), classPath);
        jobInstance = classLoader.getJobClass(jobClassName);
      }
      catch (SLAMDException se)
      {
        System.err.println(se.getMessage());
        System.exit(1);
      }
    }
    else
    {
      try
      {
        Class jobClass = Constants.classForName(jobClassName);
        jobInstance = (JobClass) jobClass.newInstance();
      }
      catch (Exception e)
      {
        System.err.println("Unable to load job class " + jobClassName + ":  " +
                           e);
        System.exit(1);
      }
    }


    try
    {
      // Create the file writer
      BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));


      // Write the header
      writer.write("#################################################" +
                   "##############################" + eol);
      writeComment(writer, "SLAMD Standalone Job Configuration File");
      writeComment(writer, "");
      writeComment(writer, "Job Name:         " + jobInstance.getJobName());
      writeComment(writer, "");
      writeComment(writer, "Job Class:        " + jobClassName);
      writeComment(writer, "");
      writeComment(writer, "Job Description:  " +
                   jobInstance.getShortDescription());
      writeComment(writer, "");
      writer.write("#################################################" +
                   "##############################" + eol);
      writer.write(eol+eol);


      // Write the class name parameter
      writeComment(writer, "Job Class");
      writeComment(writer, "The Java class used to execute this job.");
      writer.write(Constants.SERVLET_PARAM_JOB_CLASS + '=' + jobClassName +
                   eol);
      writer.write(eol+eol);


      // Iterate through each of the job-specific parameters and write out the
      // comments and configuration info for each
      Parameter[] jobParams = jobInstance.getParameterStubs().getParameters();
      for (int i=0; i < jobParams.length; i++)
      {
        // If the parameter is a placeholder, then don't do anything with it.
        if ((jobParams[i] instanceof PlaceholderParameter) ||
            (jobParams[i] instanceof LabelParameter))
        {
          continue;
        }

        // Write the display name and description.
        writeComment(writer, jobParams[i].getDisplayName());
        writeComment(writer, jobParams[i].getDescription());

        // Indicate whether the parameter is required or optional.
        if (jobParams[i].isRequired())
        {
          writeComment(writer, "This parameter is required.");
        }
        else
        {
          writeComment(writer, "This parameter is optional.");
        }

        // If there are any constraints on the value, then state what they are.
        if (jobParams[i] instanceof BooleanParameter)
        {
          writeComment(writer,
                       "The value must be either \"true\" or \"false\".");
        }
        else if (jobParams[i] instanceof FloatParameter)
        {
          FloatParameter fp = (FloatParameter) jobParams[i];
          if (fp.hasLowerBound())
          {
            if (fp.hasUpperBound())
            {
              writeComment(writer, "The value must be between " +
                           fp.getLowerBound() + " and " + fp.getUpperBound() +
                           '.');
            }
            else
            {
              writeComment(writer,
                           "The value must be greater than or equal to " +
                           fp.getLowerBound() + '.');
            }
          }
          else if (fp.hasUpperBound())
          {
            writeComment(writer, "The value must be less than or equal to " +
                         fp.getUpperBound() + '.');
          }
        }
        else if (jobParams[i] instanceof IntegerParameter)
        {
          IntegerParameter ip = (IntegerParameter) jobParams[i];
          if (ip.hasLowerBound())
          {
            if (ip.hasUpperBound())
            {
              writeComment(writer, "The value must be between " +
                           ip.getLowerBound() + " and " + ip.getUpperBound() +
                           '.');
            }
            else
            {
              writeComment(writer,
                           "The value must be greater than or equal to" +
                           ip.getLowerBound() + '.');
            }
          }
          else if (ip.hasUpperBound())
          {
            writeComment(writer, "The value must be less than or equal to " +
                         ip.getUpperBound() + '.');
          }
        }
        else if (jobParams[i] instanceof MultiChoiceParameter)
        {
          writeComment(writer, "The value must be one of the following:");
          String[] choices = ((MultiChoiceParameter) jobParams[i]).getChoices();
          for (int j=0; j < choices.length; j++)
          {
            writeComment(writer, "  - \"" + choices[j] + '"');
          }
        }

        // Finally, write the parameter name and possibly a value
        writer.write(jobParams[i].getName() + '=' +
                     jobParams[i].getValueString() + eol);
        writer.write(eol+eol);
      }


      // Close the file and be done.
      writer.flush();
      writer.close();

      writeMessage("A sample configuration file was written to " + configFile +
                   '.');
      writeMessage("You may need to edit the file before it can be used to " +
                   "run the job.");
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR writing sample configuration file " +
                         configFile + ":  " + ioe);
    }
  }



  /**
   * Writes the provided comment to the generated configuration file, wrapping
   * long lines if necessary.
   *
   * @param  writer   The buffered writer used to write information to the
   *                  configuration file.
   * @param  comment  The comment to be written to the file.
   *
   * @throws  IOException  If a problem occurs while writing the comment to the
   *                       configuration file.
   */
  public void writeComment(BufferedWriter writer, String comment)
         throws IOException
  {
    if (comment.length() > 75)
    {
      String indentStr = "";
      while (comment.length() > (75-indentStr.length()))
      {
        int spacePos = comment.lastIndexOf(' ', 75);
        if (spacePos < 0)
        {
          spacePos = comment.indexOf(' ');
          if (spacePos < 0)
          {
            writer.write("# " + indentStr + comment + eol);
            return;
          }
          else
          {
            writer.write("# " + indentStr + comment.substring(0, spacePos) +
                         eol);
            comment = comment.substring(spacePos+1);
          }
        }
        else
        {
          writer.write("# " + indentStr + comment.substring(0, spacePos) + eol);
          comment = comment.substring(spacePos+1);
        }

        indentStr = "     ";
      }

      writer.write("# " + indentStr + comment + eol);
    }
    else
    {
      writer.write("# " + comment + eol);
    }
  }



  /**
   * Writes usage information for this program to standard error.
   */
  public void displayUsage()
  {
    System.err.println(
"Usage:  java StandaloneClient [options]" + eol +
"        where [options] include:" + eol +
"-f {file}   -- Specifies the configuration file for the job to run" + eol +
"-o {file}   -- Specifies the output file to use" + eol +
"-g {class}  -- Specifies that a configuration file should be created" + eol +
"               for the specified job class rather than running a job" + eol +
"-d {value}  -- Specifies the maximum length of time the job should run" + eol +
"-i {value}  -- Specifies the length of time in seconds that should be " + eol +
"               used as the statistics collection interval" + eol +
"-t {value}  -- Specifies the number of threads that should be used" + eol +
"-c {path}   -- Specifies the location of the job class files" + eol +
"-a          -- Specifies that data from each of the threads should be " + eol +
"               aggregated before displaying the results" + eol +
"-L          -- Disables the custom job class loader" + eol +
"-v          -- Specifies the verbose job information should be logged" + eol +
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
    outputWriter.println(message);
    outputWriter.flush();
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
      outputWriter.println(message);
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

