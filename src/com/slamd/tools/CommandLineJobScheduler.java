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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import com.slamd.job.JobClass;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.SingleStatisticOptimizationAlgorithm;
import com.slamd.common.Constants;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FloatParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.http.HTTPClient;
import com.slamd.http.HTTPRequest;
import com.slamd.http.HTTPResponse;



/**
 * This class defines a utility that may be used to schedule SLAMD jobs from the
 * command line.  It does this by communicating with the SLAMD administrative
 * interface over HTTP to ensure that the scheduler is properly updated, that
 * all appropriate validity checking is performed, and that any required
 * authentication is honored.
 *
 *
 * @author   Neil A. Wilson
 */
public class CommandLineJobScheduler
{
  // The set of parameters that define information about the way in which the
  // job should be scheduled.
  private BooleanParameter disabledParameter =
       new BooleanParameter("create_disabled", "Create Job As Disabled",
                            "Indicates whether the job should be disabled " +
                            "when it is scheduled.", false);

  private BooleanParameter monitorClientsIfAvailableParameter =
       new BooleanParameter("monitor_clients_if_available",
                            "Monitor Clients if Available",
                            "Indicates whether the SLAMD server should " +
                            "automatically request resource monitoring for " +
                            "client systems used for a job if they also have " +
                            "active resource monitor clients.", false);

  private BooleanParameter optimizeParameter =
       new BooleanParameter("optimize_results", "Optimize Results",
                            "Indicates whether to create an optimizing job.",
                            true);

  private BooleanParameter rerunParameter =
       new BooleanParameter("rerun", "Re-Run Best Iteration",
                            "Indicates whether the best iteration of the " +
                            "optimizing job should be re-run.", false);

  private BooleanParameter waitParameter =
       new BooleanParameter("wait_for_clients", "Wait for Available Clients",
                            "Indicates whether the job should wait for a " +
                            "sufficient set of clients to be available if " +
                            "they are not available when the job start time " +
                            "arrives.", true);

  private IntegerParameter clientsParameter =
       new IntegerParameter("num_clients", "Number of Clients",
                            "The number of clients to use to run the job.",
                            true, 1, true, 1, false, 0);

  private IntegerParameter collectionIntervalParameter =
       new IntegerParameter("collection_interval",
                            "Statistics Collection Interval",
                            "The collection interval to use when capturing " +
                            "statistics.", true, 60, true, 1, false, 0);

  private IntegerParameter delayParameter =
       new IntegerParameter("delay_between_iterations",
                            "Delay Between Iterations",
                            "The length of time in seconds that should be " +
                            "allowed between iterations of an optimizing job.",
                            true, 0, true, 0, false, 0);

  private IntegerParameter durationParameter =
       new IntegerParameter("duration", "Job Duration",
                            "The maximum length of time in seconds that the " +
                            "job should be allowed to run.", false, -1, false,
                            0, false, 0);

  private IntegerParameter maxNonImprovingParameter =
       new IntegerParameter("max_nonimproving",
                            "Maximum Consecutive Non-Improving Iterations",
                            "The maximum number of iterations that should " +
                            "be allowed to run that do not produce results " +
                            "better than the best iteration so far.", true, 1,
                            true, 1, false, 1);

  private IntegerParameter maxThreadsParameter =
       new IntegerParameter("max_threads", "Maximum Number of Threads",
                            "The maximum number of threads per client to use " +
                            "for this optimizing job.", false, -1, false, 0,
                            false, 0);

  private IntegerParameter minThreadsParameter =
       new IntegerParameter("min_threads", "Minimum Number of Threads",
                            "The minimum number of threads per client to use " +
                            "for this optimizing job.", true, 1, true, 1, false,
                            0);

  private IntegerParameter rerunDurationParameter =
       new IntegerParameter("rerun_duration", "Re-Run Duration",
                            "The length of time in seconds to use when " +
                            "re-running the best iteration of this " +
                            "optimizing job.", false, -1, false, 0, false, 0);

  private IntegerParameter threadIncrementParameter =
       new IntegerParameter("thread_increment",
                            "Thread Increment Between Iterations",
                            "The increment to use when increasing the number " +
                            "of threads per client between optimizing job " +
                            "iterations.", true, 1, true, 1, false, 0);

  private IntegerParameter threadsParameter =
       new IntegerParameter("num_threads", "Number of Threads per Client",
                            "The number of threads per client to use to run " +
                            "the job.", true, 1, true, 1, false, 0);

  private IntegerParameter threadStartupDelayParameter =
       new IntegerParameter("thread_startup_delay", "Thread Startup Delay (ms)",
                            "The delay in milliseconds that will be used " +
                            "when starting the individual threads on the " +
                            "client.  If no value is specified, then there " +
                            "will not be any delay between client thread " +
                            "startup.", false, 0, true, 0, false, 0);

  private StringParameter algorithmParameter =
       new StringParameter("optimization_algorithm", "Optimization Algorithm",
                           "The fully-qualified name of the Java class that " +
                           "provides the optimization algorithm to use for " +
                           "the optimizing job.", true, "");

  private StringParameter classParameter =
       new StringParameter("job_class", "Job Class Name",
                           "The fully-qualified name of the Java class that " +
                           "provides an implementation of the job class to " +
                           "execute.", true, "");

  private StringParameter descriptionParameter =
       new StringParameter("description", "Job Description",
                           "The description for the job.", false, "");

  private StringParameter folderParameter =
       new StringParameter("job_folder", "Place in Folder",
                           "The name of the job folder in which the job " +
                           "should be placed when it is scheduled.", false, "");

  private StringParameter monitorClientsParameter =
       new StringParameter("monitor_clients", "Resource Monitor Clients",
                           "The address(es) of the resource monitor " +
                           "client(s) that should be used to collect " +
                           "information while the job is running.", false, "");

  private StringParameter notifyAddrsParameter =
       new StringParameter("notify_addresses", "Notify Addresses",
                           "The e-mail address(es) of the user(s) to notify " +
                           "when the job is complete.  If multiple addresses " +
                           "are provided, they should be separated by commas.",
                           false, "");

  private StringParameter requestedClientsParameter =
       new StringParameter("requested_clients", "Requested Clients",
                           "The address(es) of the client(s) that should be " +
                           "used to run the job.  If multiple clients are to " +
                           "be requested, they should be separated by commas.",
                           false, "");

  private StringParameter startTimeParameter =
       new StringParameter("start_time", "Job Start Time",
                           "The time that the job should be considered " +
                           "eligible for execution, in the format " +
                           "YYYYMMDDhhmmss.", true, "");

  private StringParameter stopTimeParameter =
       new StringParameter("stop_time", "Job Stop Time",
                           "The time that the job should stop running, in " +
                           "the format YYYYMMDDhhmmss.", false, "");


  // Parameter arrays to use when constructing the parameters to use for the
  // configuration file.
  private Parameter[] jobParameters = new Parameter[]
  {
    classParameter,
    folderParameter,
    disabledParameter,
    descriptionParameter,
    startTimeParameter,
    stopTimeParameter,
    durationParameter,
    clientsParameter,
    waitParameter,
    requestedClientsParameter,
    monitorClientsParameter,
    monitorClientsIfAvailableParameter,
    threadsParameter,
    threadStartupDelayParameter,
    collectionIntervalParameter,
    notifyAddrsParameter
  };

  private Parameter[] optimizingParameters = new Parameter[]
  {
    classParameter,
    optimizeParameter,
    algorithmParameter,
    folderParameter,
    descriptionParameter,
    startTimeParameter,
    durationParameter,
    delayParameter,
    clientsParameter,
    requestedClientsParameter,
    monitorClientsParameter,
    monitorClientsIfAvailableParameter,
    minThreadsParameter,
    maxThreadsParameter,
    threadIncrementParameter,
    collectionIntervalParameter,
    notifyAddrsParameter,
    maxNonImprovingParameter,
    rerunParameter,
    rerunDurationParameter
  };


  // Variables used processing.
  private boolean               createDisabled            = false;
  private boolean               monitorClientsIfAvailable = false;
  private boolean               rerunBestIteration        = false;
  private boolean               optimizingJob             = false;
  private boolean               useSSL                    = false;
  private boolean               verboseMode               = false;
  private boolean               waitForClients            = true;
  private Date                  startDate                 = null;
  private Date                  stopDate                  = null;
  private int                   collectionInterval        = 60;
  private int                   delayBetweenIterations    = 0;
  private int                   duration                  = -1;
  private int                   maxThreads                = -1;
  private int                   minThreads                = 1;
  private int                   nonImprovingIterations    = 1;
  private int                   numClients                = -1;
  private int                   rerunDuration             = -1;
  private int                   slamdPort                 = 8080;
  private int                   threadIncrement           = 1;
  private int                   threadsPerClient          = -1;
  private int                   threadStartupDelay        = 0;
  private JobClass              jobInstance               = null;
  private OptimizationAlgorithm optimizationAlgorithm     =
       new SingleStatisticOptimizationAlgorithm();
  private Parameter[]           jobSpecificParameters     = null;
  private Parameter[]           optimizationParameters    = null;
  private String                authID                    = null;
  private String                authPW                    = null;
  private String                configFile                = null;
  private String                dependencyID              = null;
  private String                description               = null;
  private String                eol                       = Constants.EOL;
  private String                folderName                = null;
  private String                jobClassName              = null;
  private String                postURI                   = "/slamd";
  private String                slamdHost                 = "127.0.0.1";
  private String                startTime                 = null;
  private String                stopTime                  = null;
  private String[]              monitorClients            = null;
  private String[]              notifyAddresses           = null;
  private String[]              requestedClients          = null;



  /**
   * Invokes the constructor and provides it with the command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    ArrayList<String>     configFileList        = new ArrayList<String>();
    boolean               generateConfig        = false;
    boolean               makeInterDependent    = false;
    boolean               optimizingJob         = false;
    boolean               useSSL                = false;
    boolean               verboseMode           = false;
    int                   slamdPort             = 8080;
    OptimizationAlgorithm optimizationAlgorithm =
         new SingleStatisticOptimizationAlgorithm();
    String    authID                            = null;
    String    authPW                            = null;
    String    dependencyID                      = null;
    String    jobClassName                      = null;
    String    postURI                           = "/slamd";
    String    slamdHost                         = "127.0.0.1";

    // Iterate through the command line arguments.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-f"))
      {
        configFileList.add(args[++i]);
      }
      else if (args[i].equals("-h"))
      {
        slamdHost = args[++i];
      }
      else if(args[i].equals("-p"))
      {
        slamdPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-S"))
      {
        useSSL = true;
      }
      else if (args[i].equals("-A"))
      {
        authID = args[++i];
      }
      else if (args[i].equals("-P"))
      {
        authPW = args[++i];
      }
      else if (args[i].equals("-D"))
      {
        dependencyID = args[++i];
      }
      else if (args[i].equals("-u"))
      {
        postURI = args[++i];
      }
      else if (args[i].equals("-d"))
      {
        makeInterDependent = true;
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
      }
      else if (args[i].equals("-g"))
      {
        generateConfig = true;
        jobClassName = args[++i];
      }
      else if (args[i].equals("-O"))
      {
        optimizingJob = true;
      }
      else if (args[i].equals("-o"))
      {
        String algorithmClassName = args[++i];
        try
        {
          Class algorithmClass = Constants.classForName(algorithmClassName);
          optimizationAlgorithm =
               (OptimizationAlgorithm) algorithmClass.newInstance();
        }
        catch (Exception e)
        {
          System.err.println("Unable to load optimization algorithm class \"" +
                             algorithmClassName + "\" -- " + e);
          return;
        }
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        return;
      }
      else
      {
        System.err.println("ERROR:  Invalid argument \"" + args[i] + '"');
        displayUsage();
        return;
      }
    }


    if (configFileList.isEmpty())
    {
      System.err.println("ERROR:  At least one configuration file must be " +
                         "specified.");
      displayUsage();
      return;
    }


    if (generateConfig)
    {
      if (configFileList.size() > 1)
      {
        System.err.println("ERROR:  Only one file may be specified when " +
                           "generating a configuration file.");
        displayUsage();
        return;
      }

      String configFile = configFileList.get(0);
      CommandLineJobScheduler scheduler =
           new CommandLineJobScheduler(configFile, slamdHost, slamdPort, useSSL,
                                       authID, authPW, postURI, dependencyID,
                                       jobClassName, optimizingJob,
                                       optimizationAlgorithm, verboseMode);
      scheduler.generateConfigFile();
      return;
    }


    for (int i=0; i < configFileList.size(); i++)
    {
      String configFile = configFileList.get(i);
      CommandLineJobScheduler scheduler =
           new CommandLineJobScheduler(configFile, slamdHost, slamdPort, useSSL,
                                       authID, authPW, postURI, dependencyID,
                                       jobClassName, optimizingJob,
                                       optimizationAlgorithm, verboseMode);

      if (! scheduler.parseConfigFile())
      {
        System.err.println("An error occurred while parsing configuration " +
                           "file \"" + configFile + '"');
        return;
      }

      String scheduledID;
      if (scheduler.optimizingJob)
      {
        scheduledID = scheduler.scheduleOptimizingJob();
      }
      else
      {
        scheduledID = scheduler.scheduleJob();
      }

      if (scheduledID == null)
      {
        System.err.println("An error occurred while attempting to schedule " +
                           "the job defined in configuration file \"" +
                           configFile + '"');
        return;
      }

      if (makeInterDependent)
      {
        dependencyID = scheduledID;
      }
    }
  }



  /**
   * Creates a new instance of this command-line scheduler with the provided
   * information.
   *
   * @param  configFile             The path to the configuration file to use.
   * @param  slamdHost              The address to use for the SLAMD server.
   * @param  slamdPort              The port of the SLAMD server's HTTP
   *                                administration interface.
   * @param  useSSL                 Indicates whether to use SSL to communicate
   *                                with the SLAMD server.
   * @param  authID                 The user ID to use to authenticate to the
   *                                server.
   * @param  authPW                 The password to use to authenticate to the
   *                                server.
   * @param  postURI                The URI in the SLAMD server to which the
   *                                request should be posted.
   * @param  dependencyID           The job ID of the job on which to make the
   *                                new job dependent.
   * @param  jobClassName           The name of the job class for which to
   *                                generate the configuration.
   * @param  optimizingJob          Indicates whether the configuration
   *                                generated should be an optimizing job.
   * @param  optimizationAlgorithm  The optimization algorithm that should be
   *                                used to create the optimizing job.
   * @param  verboseMode            Indicates whether the scheduler should
   *                                operate in verbose mode.
   */
  public CommandLineJobScheduler(String configFile, String slamdHost,
                                 int slamdPort, boolean useSSL, String authID,
                                 String authPW, String postURI,
                                 String dependencyID, String jobClassName,
                                 boolean optimizingJob,
                                 OptimizationAlgorithm optimizationAlgorithm,
                                 boolean verboseMode)
  {
    this.configFile            = configFile;
    this.slamdHost             = slamdHost;
    this.slamdPort             = slamdPort;
    this.useSSL                = useSSL;
    this.authID                = authID;
    this.authPW                = authPW;
    this.postURI               = postURI;
    this.dependencyID          = dependencyID;
    this.jobClassName          = jobClassName;
    this.optimizingJob         = optimizingJob;
    this.optimizationAlgorithm = optimizationAlgorithm;
    this.verboseMode           = verboseMode;
  }



  /**
   * Generates a configuration file to use to schedule the job.
   */
  public void generateConfigFile()
  {
    // First, verify that the specified job class exists and that it is a valid
    // job class.
    try
    {
      Class<?> jobClass = Constants.classForName(jobClassName);
      Class<?> jobSuperClass =
           Constants.classForName(Constants.JOB_THREAD_SUPERCLASS_NAME);
      if (jobSuperClass.isAssignableFrom(jobClass))
      {
        try
        {
          jobInstance = (JobClass) jobClass.newInstance();
        }
        catch (Exception e)
        {
          if (verboseMode)
          {
            e.printStackTrace();
          }
          System.err.println("ERROR:  Could not create an instance of the " +
                             "job class " + jobClassName + ":  " + e);
          return;
        }
      }
      else
      {
        System.err.println("ERROR:  Class " + jobClassName +
                      " is not a valid SLAMD job class");
        return;
      }
    }
    catch (ClassNotFoundException cnfe)
    {
      if (verboseMode)
      {
        cnfe.printStackTrace();
      }
      System.err.println("ERROR:  Could not find job class " + jobClassName +
                         ".  Is it in the classpath?");
      return;
    }


    // Set the name of the job class in the config file.
    try
    {
      classParameter.setValue(jobClassName);
    }
    catch (InvalidValueException ive)
    {
      // This should never happen.
      if (verboseMode)
      {
        ive.printStackTrace();
      }
      System.err.println("ERROR:  Unable to set job class name -- " + ive);
      return;
    }


    // Set the name of the optimization algorithm in the config file.
    try
    {
      algorithmParameter.setValue(optimizationAlgorithm.getClass().getName());
    }
    catch (InvalidValueException ive)
    {
      // This should never happen.
      if (verboseMode)
      {
        ive.printStackTrace();
      }
      System.err.println("ERROR:  Unable to set optimization algorithm -- " +
                         ive);
      return;
    }


    // Set the start time to the current time.
    try
    {
      SimpleDateFormat dateFormat =
           new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);
      startTimeParameter.setValue(dateFormat.format(new Date()));
    }
    catch (InvalidValueException ive)
    {
      // This also should never happen.
      if (verboseMode)
      {
        ive.printStackTrace();
      }
      System.err.println("ERROR:  Unable to set job start time -- " + ive);
      return;
    }


    // If this is to be an optimizing job, then make sure it is optimizable with
    // the specified algorithm.
    if (optimizingJob)
    {
      if (! optimizationAlgorithm.availableWithJobClass(jobInstance))
      {
        System.err.println("ERROR:  Job class \"" + jobClassName +
                           "\" may not be used with optimization algorithm \"" +
                           optimizationAlgorithm.getClass().getName() + '"');
        return;
      }
    }


    try
    {
      // Create the file writer
      BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));


      // Write the header
      writer.write("#################################################" +
                   "##############################" + eol);
      writeComment(writer, "SLAMD Command-Line Job Scheduler Config File");
      writeComment(writer, "");
      writeComment(writer, "Job Name:               " +
                   jobInstance.getJobName());
      writeComment(writer, "");
      writeComment(writer, "Job Class:              " + jobClassName);
      writeComment(writer, "");
      writeComment(writer, "Job Class Description:  " +
                   jobInstance.getShortDescription());
      writeComment(writer, "");
      writer.write("#################################################" +
                   "##############################" + eol);
      writer.write(eol+eol);


      if (optimizingJob)
      {
        for (int i=0; i < optimizingParameters.length; i++)
        {
          writeParam(writer, optimizingParameters[i], null);
        }

        // Write all the optimization algorithm parameters.
        Parameter[] optimizationParams =
             optimizationAlgorithm.
                  getOptimizationAlgorithmParameterStubs(jobInstance).
                       getParameters();
        for (int i=0; i < optimizationParams.length; i++)
        {
          writeParam(writer, optimizationParams[i],
                     Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX);
        }
      }
      else
      {
        for (int i=0; i < jobParameters.length; i++)
        {
          writeParam(writer, jobParameters[i], null);
        }
      }


      // Iterate through each of the job-specific parameters and write out the
      // comments and configuration info for each
      Parameter[] jobParams = jobInstance.getParameterStubs().getParameters();
      for (int i=0; i < jobParams.length; i++)
      {
        writeParam(writer, jobParams[i],
                   Constants.SERVLET_PARAM_JOB_PARAM_PREFIX);
      }


      // Close the file and be done.
      writer.flush();
      writer.close();

      System.out.println("The configuration file was written to " + configFile +
                         '.');
      System.out.println("You may need to edit the file before it can be " +
                         "used to run the job.");
    }
    catch (IOException ioe)
    {
      if (verboseMode)
      {
        ioe.printStackTrace();
      }
      System.err.println("ERROR writing sample configuration file " +
                         configFile + ":  " + ioe);
    }
  }



  /**
   * Writes information about the specified parameter to the configuration file
   * using the given writer.
   *
   * @param  writer     The writer to use to send data to the config file.
   * @param  parameter  The parameter to write to the config file.
   * @param  prefix     The prefix to use for the parameter when writing it to
   *                    the config file.  If no prefix should be used, this may
   *                    be either null or an empty string.
   *
   * @throws  IOException  If a problem occurs while writing to the
   *                       configuration file.
   */
  private void writeParam(BufferedWriter writer, Parameter parameter,
                          String prefix)
          throws IOException
  {
    // If the parameter is a placeholder, then don't do anything with it.
    if ((parameter instanceof PlaceholderParameter) ||
        (parameter instanceof LabelParameter))
    {
      return;
    }

    // Write the display name and description.
    writeComment(writer, parameter.getDisplayName());
    writeComment(writer, parameter.getDescription());

    // Indicate whether the parameter is required or optional.
    if (parameter.isRequired())
    {
      writeComment(writer, "This parameter is required.");
    }
    else
    {
      writeComment(writer, "This parameter is optional.");
    }

    // If there are any constraints on the value, then state what they are.
    if (parameter instanceof BooleanParameter)
    {
      writeComment(writer,
                   "The value must be either \"true\" or \"false\".");
    }
    else if (parameter instanceof FloatParameter)
    {
      FloatParameter fp = (FloatParameter) parameter;
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
          writeComment(writer, "The value must be greater than or equal to " +
                       fp.getLowerBound() + '.');
        }
      }
      else if (fp.hasUpperBound())
      {
        writeComment(writer, "The value must be less than or equal to " +
                     fp.getUpperBound() + '.');
      }
    }
    else if (parameter instanceof IntegerParameter)
    {
      IntegerParameter ip = (IntegerParameter) parameter;
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
          writeComment(writer, "The value must be greater than or equal to " +
                       ip.getLowerBound() + '.');
        }
      }
      else if (ip.hasUpperBound())
      {
        writeComment(writer, "The value must be less than or equal to " +
                     ip.getUpperBound() + '.');
      }
    }
    else if (parameter instanceof MultiChoiceParameter)
    {
      writeComment(writer, "The value must be one of the following:");
      String[] choices = ((MultiChoiceParameter) parameter).getChoices();
      for (int j=0; j < choices.length; j++)
      {
        writeComment(writer, "  - \"" + choices[j] + '"');
      }
    }

    // Finally, write the parameter name and possibly a value
    if ((prefix != null) && (prefix.length() > 0))
    {
      writer.write(prefix);
    }
    writer.write(parameter.getName() + '=' + parameter.getValueString() + eol +
                 eol + eol);
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
  private void writeComment(BufferedWriter writer, String comment)
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
   * Parses the configuration file and assigns its contents to instance
   * variables.
   *
   * @return  <CODE>true</CODE> if the file was parsed successfully and the job
   *          configuration is valid, or <CODE>false</CODE> if there was a
   *          problem of some kind.
   */
  public boolean parseConfigFile()
  {
    boolean configValid = true;
    debug("Parsing configuration file " + configFile);


    // Read the configuration file
    Properties configProperties = new Properties();
    try
    {
      configProperties.load(new FileInputStream(configFile));
    }
    catch (IOException ioe)
    {
      if (verboseMode)
      {
        ioe.printStackTrace();
      }
      System.err.println("ERROR:  Unable to open configuration file " +
                         configFile);
      return false;
    }


    // Get the name of the job class and create the job instance.
    jobClassName = configProperties.getProperty(classParameter.getName());
    if (jobClassName == null)
    {
      System.err.println("ERROR:  No job class name specified.");
      return false;
    }

    try
    {
      debug("Loading and instantiating job class \"" + jobClassName + '"');
      Class<?> jobClass = Constants.classForName(jobClassName);
      Class<?> jobSuperClass =
           Constants.classForName(Constants.JOB_THREAD_SUPERCLASS_NAME);
      if (jobSuperClass.isAssignableFrom(jobClass))
      {
        try
        {
          jobInstance = (JobClass) jobClass.newInstance();
        }
        catch (Exception e)
        {
          if (verboseMode)
          {
            e.printStackTrace();
          }
          System.err.println("ERROR:  Could not create an instance of the " +
                             "job class " + jobClassName + ":  " + e);
          return false;
        }
      }
      else
      {
        System.err.println("ERROR:  Class " + jobClassName +
                      " is not a valid SLAMD job class");
        return false;
      }
    }
    catch (ClassNotFoundException cnfe)
    {
      if (verboseMode)
      {
        cnfe.printStackTrace();
      }
      System.err.println("ERROR:  Could not find job class " + jobClassName +
                         ".  Is it in the classpath?");
      return false;
    }


    // See if this is to be an optimizing job.
    String optimizingStr =
         configProperties.getProperty(optimizeParameter.getName());
    if ((optimizingStr != null) && optimizingStr.equalsIgnoreCase("true"))
    {
      optimizingJob = true;
    }


    // Get the optimization algorithm to use.
    String algorithmStr =
         configProperties.getProperty(algorithmParameter.getName());
    if (optimizingJob)
    {
      try
      {
        Class algorithmClass = Constants.classForName(algorithmStr);
        optimizationAlgorithm =
             (OptimizationAlgorithm) algorithmClass.newInstance();
      }
      catch (Exception e)
      {
        System.err.println("ERROR:  Unable to use optimization algorithm \"" +
                           algorithmStr + "\" -- " + e);
        configValid = false;
      }
    }


    // Get the folder name.  It can be blank and needs no validation, but an
    // invalid folder name can prevent the job from being scheduled properly.
    folderName  = configProperties.getProperty(folderParameter.getName());


    // Get the job description.  We don't really care about this.
    description = configProperties.getProperty(descriptionParameter.getName());


    // Get the job start time.  It must be present and properly formatted.
    startTime = configProperties.getProperty(startTimeParameter.getName());
    if ((startTime == null) || (startTime.length() == 0))
    {
      System.err.println("ERROR:  No start time specified.");
      configValid = false;
    }
    else if (startTime.length() != 14)
    {
      System.err.println("ERROR:  Invalid start time.  It must be in the " +
                         "form YYYYMMDDhhmmss");
      configValid = false;
    }
    else
    {
      SimpleDateFormat dateFormat =
           new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);
      try
      {
        startDate = dateFormat.parse(startTime);
      }
      catch (ParseException pe)
      {
        if (verboseMode)
        {
          pe.printStackTrace();
        }
        System.err.println("ERROR:  Invalid start time.  It must be in the " +
                           "form YYYYMMDDhhmmss");
        configValid = false;
      }
    }


    // Get the job stop time.  It is optional, but if provided must be properly
    // formatted.
    stopTime = configProperties.getProperty(stopTimeParameter.getName());
    if ((stopTime != null) && (stopTime.length() > 0))
    {
      if (stopTime.length() != 14)
      {
        System.err.println("ERROR:  Invalid stop time.  It must be in the " +
                           "form YYYYMMDDhhmmss");
        configValid = false;
      }
      else
      {
        SimpleDateFormat dateFormat =
             new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);
        try
        {
          stopDate = dateFormat.parse(stopTime);
        }
        catch (ParseException pe)
        {
          if (verboseMode)
          {
            pe.printStackTrace();
          }
          System.err.println("ERROR:  Invalid stop time.  It must be in the " +
                             "form YYYYMMDDhhmmss");
          configValid = false;
        }
      }
    }


    // Get the job duration.  It is optional, but if provided must be an
    // integer.
    String durationStr =
         configProperties.getProperty(durationParameter.getName());
    if ((durationStr != null) && (durationStr.length() > 0))
    {
      try
      {
        duration = Integer.parseInt(durationStr);
      }
      catch (NumberFormatException nfe)
      {
        if (verboseMode)
        {
          nfe.printStackTrace();
        }
        System.err.println("ERROR:  Job duration must be an integer.");
        configValid = false;
      }
    }


    // Get the number of clients.  It must be specified and must be a positive
    // integer.
    String clientsStr =
         configProperties.getProperty(clientsParameter.getName());
    if ((clientsStr == null) || (clientsStr.length() == 0))
    {
      System.err.println("ERROR:  Number of clients must be specified.");
      configValid = false;
    }
    else
    {
      try
      {
        numClients = Integer.parseInt(clientsStr);
        if (numClients <= 0)
        {
          System.err.println("ERROR:  Number of clients must be greater than " +
                             "zero.");
          configValid = false;
        }
      }
      catch (NumberFormatException nfe)
      {
        if (verboseMode)
        {
          nfe.printStackTrace();
        }
        System.err.println("ERROR:  Number of clients must be an integer.");
        configValid = false;
      }
    }


    // Get the set of requested clients.  This is optional.
    String requestedClientsStr =
         configProperties.getProperty(requestedClientsParameter.getName());
    if ((requestedClientsStr != null) && (requestedClientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(requestedClientsStr, ", \t");
      while (st.hasMoreTokens())
      {
        clientList.add(st.nextToken());
      }
      requestedClients = new String[clientList.size()];
      clientList.toArray(requestedClients);
    }


    // Get the set of resource monitor clients.  This is optional.
    String monitorClientsStr =
         configProperties.getProperty(monitorClientsParameter.getName());
    if ((monitorClientsStr != null) && (monitorClientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(monitorClientsStr, ", \t");
      while (st.hasMoreTokens())
      {
        clientList.add(st.nextToken());
      }
      monitorClients = new String[clientList.size()];
      clientList.toArray(monitorClients);
    }


    // Get the statistics collection interval.  It must be specified and must be
    // a positive integer.
    String intervalStr =
         configProperties.getProperty(collectionIntervalParameter.getName());
    if ((intervalStr == null) || (intervalStr.length() == 0))
    {
      System.err.println("ERROR:  Statistics collection interval must be " +
                         "specified.");
      configValid = false;
    }
    else
    {
      try
      {
        collectionInterval = Integer.parseInt(intervalStr);
        if (collectionInterval <= 0)
        {
          System.err.println("ERROR:  Statistics collection interval must be " +
                             "greater than zero.");
          configValid = false;
        }
      }
      catch (NumberFormatException nfe)
      {
        if (verboseMode)
        {
          nfe.printStackTrace();
        }
        System.err.println("ERROR:  Statistics collection interval must be " +
                           "an integer.");
        configValid = false;
      }
    }


    // Get the set of e-mail addresses of the users to notify when the job is
    // complete.  This is optional.
    String notifyStr =
         configProperties.getProperty(notifyAddrsParameter.getName());
    if ((notifyStr != null) && (notifyStr.length() > 0))
    {
      ArrayList<String> addressList = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(notifyStr, ", \t");
      while (st.hasMoreTokens())
      {
        addressList.add(st.nextToken());
      }
      notifyAddresses = new String[addressList.size()];
      addressList.toArray(notifyAddresses);
    }


    // Get values of parameters that differ based on whether this is an
    // optimizing job.
    if (optimizingJob)
    {
      // Get the delay between iterations.  It must be specified and must be
      // an integer greater than or equal to zero.
      String delayStr = configProperties.getProperty(delayParameter.getName());
      if ((delayStr == null) || (delayStr.length() == 0))
      {
        System.err.println("ERROR:  Delay between iterations must be " +
                           "specified.");
        configValid = false;
      }
      else
      {
        try
        {
          delayBetweenIterations = Integer.parseInt(delayStr);
          if (delayBetweenIterations < 0)
          {
            System.err.println("ERROR:  Delay between iterations must be " +
                               "greater than or equal to zero.");
            configValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Delay between iterations must be an " +
                             "integer.");
          configValid = false;
        }
      }


      // Get the minimum number of threads per client.  It must be specified and
      // must be a positive integer.
      String minThreadsStr =
           configProperties.getProperty(minThreadsParameter.getName());
      if ((minThreadsStr == null) || (minThreadsStr.length() == 0))
      {
        System.err.println("ERROR:  Minimum number of threads must be " +
                           "specified.");
        configValid = false;
      }
      else
      {
        try
        {
          minThreads = Integer.parseInt(minThreadsStr);
          if (minThreads <= 0)
          {
            System.err.println("ERROR:  Minimum number of threads must be " +
                               "greater than zero.");
            configValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Minimum number of threads must be an " +
                             "integer.");
          configValid = false;
        }
      }


      // Get the maximum number of threads per client.  It is optional, but if
      // specified must be an integer.
      String maxThreadsStr =
           configProperties.getProperty(maxThreadsParameter.getName());
      if ((maxThreadsStr != null) && (maxThreadsStr.length() > 0))
      {
        try
        {
          maxThreads = Integer.parseInt(maxThreadsStr);
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Maximum number of threads must be an " +
                             "integer.");
          configValid = false;
        }
      }


      // Get the thread increment.  It must be specified and must be a positive
      // integer.
      String incrementStr =
           configProperties.getProperty(threadIncrementParameter.getName());
      if ((incrementStr == null) || (incrementStr.length() == 0))
      {
        System.err.println("ERROR:  Thread increment must be specified.");
        configValid = false;
      }
      else
      {
        try
        {
          threadIncrement = Integer.parseInt(incrementStr);
          if (threadIncrement <= 0)
          {
            System.err.println("ERROR:  Thread increment must be greater " +
                               "than zero.");
            configValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Thread increment must be an integer.");
          configValid = false;
        }
      }


      // Get the maximum number of non-improving iterations.  It must be
      // specified and must be a positive integer.
      String nonImprovingStr =
           configProperties.getProperty(maxNonImprovingParameter.getName());
      if ((nonImprovingStr == null) || (nonImprovingStr.length() == 0))
      {
        System.err.println("ERROR:  Maximum number of non-improving " +
                           "iterations must be specified.");
        configValid = false;
      }
      else
      {
        try
        {
          nonImprovingIterations = Integer.parseInt(nonImprovingStr);
          if (nonImprovingIterations <= 0)
          {
            System.err.println("ERROR:  Maximum number of non-improving " +
                               "iterations must be greater than zero.");
            configValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Maximum number of non-improving " +
                             "iterations must be an integer.");
          configValid = false;
        }
      }


      // Determine whether to re-run the best iteration.  It is optional.
      String reRunStr = configProperties.getProperty(rerunParameter.getName());
      if ((reRunStr != null) && reRunStr.equalsIgnoreCase("true"))
      {
        rerunBestIteration = true;
      }


      // Determine the duration to use for the re-run iteration.  It is
      // optional, but if specified must be an integer.
      String reRunDurationStr =
           configProperties.getProperty(rerunDurationParameter.getName());
      if ((reRunDurationStr != null) && (reRunDurationStr.length() > 0))
      {
        try
        {
          rerunDuration = Integer.parseInt(reRunDurationStr);
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Re-run duration must be an integer.");
          configValid = false;
        }
      }
    }
    else
    {
      // Determine whether the job should be disabled when it is created.
      String disabledStr =
           configProperties.getProperty(disabledParameter.getName());
      if ((disabledStr != null) && disabledStr.equalsIgnoreCase("true"))
      {
        createDisabled = true;
      }


      // Determine whether the job should wait for clients to be available.
      String waitStr = configProperties.getProperty(waitParameter.getName());
      if ((waitStr != null) && waitStr.equalsIgnoreCase("false"))
      {
        waitForClients = false;
      }


      // Determine whether to monitor clients if they are available.
      String monitorStr = configProperties.getProperty(
                               monitorClientsIfAvailableParameter.getName());
      if ((monitorStr != null) && monitorStr.equalsIgnoreCase("true"))
      {
        monitorClientsIfAvailable = true;
      }


      // Get the number of threads per client.  It must be specified and must be
      // a positive integer.
      String threadsStr =
           configProperties.getProperty(threadsParameter.getName());
      if ((threadsStr == null) || (threadsStr.length() == 0))
      {
        System.err.println("ERROR:  Number of threads per client must be " +
                           "specified.");
        configValid = false;
      }
      else
      {
        try
        {
          threadsPerClient = Integer.parseInt(threadsStr);
          if (threadsPerClient <= 0)
          {
            System.err.println("ERROR:  Number of threads per client must be " +
                               "greater than zero.");
            configValid = false;
          }
        }
        catch (NumberFormatException nfe)
        {
          if (verboseMode)
          {
            nfe.printStackTrace();
          }
          System.err.println("ERROR:  Number of threads per client must be " +
                             "an integer.");
          configValid = false;
        }
      }
    }


    // Get the thread startup delay.  It is optional, but if provided must be
    // an integer greater than or equal to zero.
    String delayStr =
         configProperties.getProperty(threadStartupDelayParameter.getName());
    if ((delayStr == null) || (delayStr.length() == 0))
    {
      threadStartupDelay = 0;
    }
    else
    {
      try
      {
        threadStartupDelay = Integer.parseInt(delayStr);
        if (threadStartupDelay < 0)
        {
          System.err.println("ERROR:  Thread startup delay must be greater " +
                             "than or equal to zero.");
          configValid = false;
        }
      }
      catch (NumberFormatException nfe)
      {
        if (verboseMode)
        {
          nfe.printStackTrace();
        }
        System.err.println("ERROR:  Thread startup delay must be an integer.");
        configValid = false;
      }
    }


    // Validate the values of all the optimization algorithm parameters.
    if (optimizingJob)
    {
      debug("Validating optimization algorithm parameters");
      optimizationParameters =
           optimizationAlgorithm.
                getOptimizationAlgorithmParameterStubs(jobInstance).
                     getParameters();
      for (int i=0; i < optimizationParameters.length; i++)
      {
        if ((optimizationParameters[i] instanceof PlaceholderParameter) ||
            (optimizationParameters[i] instanceof LabelParameter))
        {
          continue;
        }

        String valueStr =
             configProperties.getProperty(
                  Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                  optimizationParameters[i].getName());
        if (valueStr == null)
        {
          valueStr = "";
        }

        try
        {
          optimizationParameters[i].setValueFromString(valueStr);
        }
        catch (InvalidValueException ive)
        {
          if (verboseMode)
          {
            ive.printStackTrace();
          }

          System.err.println("ERROR:  Invalid value for optimization " +
                             "algorithm parameter \"" +
                             optimizationParameters[i].getDisplayName() +
                             "\":  " + ive.getMessage());
          configValid = false;
        }
      }
    }


    // Validate the values of all the job-specific parameters.
    debug("Validating job-specific parameters");
    jobSpecificParameters =
         jobInstance.getParameterStubs().getParameters();
    for (int i=0; i < jobSpecificParameters.length; i++)
    {
      if ((jobSpecificParameters[i] instanceof PlaceholderParameter) ||
          (jobSpecificParameters[i] instanceof LabelParameter))
      {
        continue;
      }

      String valueStr = configProperties.getProperty(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                             jobSpecificParameters[i].getName());
      if (valueStr == null)
      {
        valueStr = "";
      }


      // Replace any occurrences of "\n" (with the exception of "\\n") with line
      // breaks.
      int pos = 0;
      while ((pos < valueStr.length()) &&
             ((pos = valueStr.indexOf('\\', pos)) >= 0))
      {
        if (pos == (valueStr.length() - 1))
        {
          break;
        }

        switch (valueStr.charAt(pos+1))
        {
          case 'n':
            valueStr = valueStr.substring(0, pos) + '\n' +
                       valueStr.substring(pos+2);
            pos++;
            break;
          case '\\':
            valueStr = valueStr.substring(0, pos) + valueStr.substring(pos+1);
            pos++;
            break;
          default:
            pos += 2;
            break;
        }
      }

      try
      {
        jobSpecificParameters[i].setValueFromString(valueStr);
      }
      catch (InvalidValueException ive)
      {
        if (verboseMode)
        {
          ive.printStackTrace();
        }
        System.err.println("ERROR:  Invalid value for job-specific " +
                           "parameter \"" +
                           jobSpecificParameters[i].getDisplayName() + "\":  " +
                           ive.getMessage());
        configValid = false;
      }
    }


    // Validate the job as a whole.
    if (configValid)
    {
      debug("Validating job configuration");
      ParameterList paramList = new ParameterList(jobSpecificParameters);
      try
      {
        jobInstance.validateJobInfo(numClients, threadsPerClient, 0,
                                    startDate, stopDate, duration,
                                    collectionInterval, paramList);
      }
      catch (InvalidValueException ive)
      {
        if (verboseMode)
        {
          ive.printStackTrace();
        }
      }
    }

    debug("Done parsing job configuration -- job is " +
          (configValid ? "" : "not ") + "acceptable");
    return configValid;
  }



  /**
   * Handles all processing required to schedule a job for execution.
   *
   * @return  The job ID of the job that was scheduled, or <CODE>null</CODE> if
   *          an error occurred to prevent the job from being scheduled
   *          properly.
   */
  public String scheduleJob()
  {
    // Create the HTTP client to use to send the request.
    HTTPClient httpClient = new HTTPClient();
    httpClient.setFollowRedirects(true);
    httpClient.setRetrieveAssociatedFiles(false);

    if ((authID != null) && (authID.length() > 0) && (authPW != null) &&
        (authPW.length() > 0))
    {
      httpClient.enableAuthentication(authID, authPW);
    }

    if (verboseMode)
    {
      httpClient.enableDebugMode();
    }

    String protocol = "http";
    if (useSSL)
    {
      try
      {
        protocol = "https";
        httpClient.setSSLSocketFactory(new JSSEBlindTrustSocketFactory());
      }
      catch (Exception e)
      {
        System.err.println("ERROR:  Unable to initialize the SSL socket " +
                           "factory:");
        e.printStackTrace();
        return null;
      }
    }


    // Construct the URL for the request.
    String urlStr = protocol + "://" + slamdHost + ':' + slamdPort + postURI;
    URL requestURL;
    try
    {
      requestURL = new URL(urlStr);
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  The constructed URL '" + urlStr +
                         "' is invalid:");
      e.printStackTrace();
      return null;
    }


    // Construct the request to send to the server.
    HTTPRequest request = new HTTPRequest(false, requestURL);
    request.addParameter(Constants.SERVLET_PARAM_SECTION,
                         Constants.SERVLET_SECTION_JOB);
    request.addParameter(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_SCHEDULE);
    request.addParameter(Constants.SERVLET_PARAM_JOB_CLASS, jobClassName);
    request.addParameter(Constants.SERVLET_PARAM_JOB_VALIDATE_SCHEDULE, "1");
    request.addParameter(Constants.SERVLET_PARAM_SUBMIT,
                         Constants.SUBMIT_STRING_SCHEDULE_JOB);

    if (createDisabled)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DISABLED, "on");
    }

    if ((folderName == null) || (folderName.length() == 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_FOLDER,
                           Constants.FOLDER_NAME_UNCLASSIFIED);
    }
    else
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_FOLDER, folderName);
    }

    if ((description != null) && (description.length() > 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION,
                           description);
    }

    request.addParameter(Constants.SERVLET_PARAM_JOB_START_TIME, startTime);

    if ((stopTime != null) && (stopTime.length() > 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_STOP_TIME, stopTime);
    }

    if (duration > 0)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DURATION,
                           String.valueOf(duration));
    }

    request.addParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                         String.valueOf(numClients));

    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      String requestedClientsStr = requestedClients[0];
      for (int i=1; i < requestedClients.length; i++)
      {
        requestedClientsStr += '\n' + requestedClients[i];
      }
      request.addParameter(Constants.SERVLET_PARAM_JOB_CLIENTS,
                           requestedClientsStr);
    }

    if ((monitorClients != null) && (monitorClients.length > 0))
    {
      String monitorClientsStr = monitorClients[0];
      for (int i=1; i < monitorClients.length; i++)
      {
        monitorClientsStr += '\n' + monitorClients[i];
      }
      request.addParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS,
                           monitorClientsStr);
    }

    if (monitorClientsIfAvailable)
    {
      request.addParameter(
           Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE, "on");
    }

    if (waitForClients)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS, "on");
    }

    request.addParameter(Constants.SERVLET_PARAM_JOB_THREADS_PER_CLIENT,
                         String.valueOf(threadsPerClient));
    request.addParameter(Constants.SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY,
                         String.valueOf(threadStartupDelay));

    request.addParameter(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                         String.valueOf(collectionInterval));

    if ((dependencyID != null) && (dependencyID.length() > 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DEPENDENCY,
                           dependencyID);
    }

    if ((notifyAddresses != null) && (notifyAddresses.length > 0))
    {
      String notifyStr = notifyAddresses[0];
      for (int i=1; i < notifyAddresses.length; i++)
      {
        notifyStr += ',' + notifyAddresses[i];
      }
      request.addParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS,
                           notifyStr);
    }

    for (int i=0; i < jobSpecificParameters.length; i++)
    {
      String valueStr = jobSpecificParameters[i].getHTMLPostValue();
      if (valueStr != null)
      {
        // The value is already encoded, and doing it again would cause
        // problems. Therefore, manually, construct the post string.
        String name = Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                      jobSpecificParameters[i].getName();
        try
        {
          request.addEncodedParameter(name, valueStr);
        }
        catch (UnsupportedEncodingException uee)
        {
          request.addParameter(name, valueStr);
        }
      }
    }


    // Send the request and read the response.
    HTTPResponse response;
    try
    {
      response = httpClient.sendRequest(request);
      httpClient.closeAll();
    }
    catch (Exception e)
    {
      System.err.println("Error communicating with SLAMD server:");
      e.printStackTrace();
      return null;
    }


    // Make sure that the response status code was "200".
    if (response.getStatusCode() != 200)
    {
      System.err.println("ERROR:  Unexpected response status code (" +
                         response.getStatusCode() + ' ' +
                         response.getResponseMessage() + ')');
      return null;
    }


    // See if the response header included a job ID.  If so, then the job was
    // scheduled successfully.  If not, then there was a problem to prevent it
    // from being scheduled.
    String jobID = response.getHeader(Constants.SERVLET_PARAM_JOB_ID);
    if (jobID == null)
    {
      System.err.println("ERROR:  Unable to determine the job ID from the " +
                         "response.");
      System.err.println("This probably means that the job was not scheduled " +
                         "properly.");

      if (verboseMode)
      {
        System.err.println("The body of the HTTP response was:");
        try
        {
          byte[] responseData = response.getResponseData();
          BufferedReader reader =
               new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(responseData)));
          String line = reader.readLine();
          while (line != null)
          {
            System.out.println(line);
            line = reader.readLine();
          }

          reader.close();
        }
        catch (Exception e)
        {
          System.err.println("Error decoding response data:");
          e.printStackTrace();
        }
      }

      return null;
    }
    else
    {
      System.out.println("Successfully scheduled job " + jobID +
                         " using config file \"" + configFile + "\".");
      return jobID;
    }
  }



  /**
   * Handles all processing required to schedule an optimizing job for
   * execution.
   *
   * @return  The job ID of the optimizing job that was scheduled, or
   *          <CODE>null</CODE> if an error occurred to prevent the job from
   *          being scheduled properly.
   */
  public String scheduleOptimizingJob()
  {
    // Create the HTTP client to use to send the request.
    HTTPClient httpClient = new HTTPClient();
    httpClient.setFollowRedirects(true);
    httpClient.setRetrieveAssociatedFiles(false);

    if ((authID != null) && (authID.length() > 0) && (authPW != null) &&
        (authPW.length() > 0))
    {
      httpClient.enableAuthentication(authID, authPW);
    }

    if (verboseMode)
    {
      httpClient.enableDebugMode();
    }

    String protocol = "http";
    if (useSSL)
    {
      try
      {
        protocol = "https";
        httpClient.setSSLSocketFactory(new JSSEBlindTrustSocketFactory());
      }
      catch (Exception e)
      {
        System.err.println("ERROR:  Unable to initialize the SSL socket " +
                           "factory:");
        e.printStackTrace();
        return null;
      }
    }


    // Construct the URL for the request.
    String urlStr = protocol + "://" + slamdHost + ':' + slamdPort + postURI;
    URL requestURL;
    try
    {
      requestURL = new URL(urlStr);
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  The constructed URL '" + urlStr +
                         "' is invalid:");
      e.printStackTrace();
      return null;
    }


    // Construct the request to send to the server.
    HTTPRequest request = new HTTPRequest(false, requestURL);
    request.addParameter(Constants.SERVLET_PARAM_SECTION,
                         Constants.SERVLET_SECTION_JOB);
    request.addParameter(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_OPTIMIZE);
    request.addParameter(Constants.SERVLET_PARAM_JOB_CLASS, jobClassName);
    request.addParameter(Constants.SERVLET_PARAM_OPTIMIZATION_ALGORITHM,
                         optimizationAlgorithm.getClass().getName());
    request.addParameter(
         Constants.SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION, "on");
    request.addParameter(Constants.SERVLET_PARAM_CONFIRMED, "Schedule");

    if ((folderName == null) || (folderName.length() == 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_FOLDER,
                           Constants.FOLDER_NAME_UNCLASSIFIED);
    }
    else
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_FOLDER, folderName);
    }

    if ((description != null) && (description.length() > 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION,
                           description);
    }

    request.addParameter(Constants.SERVLET_PARAM_JOB_START_TIME, startTime);

    if (duration > 0)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DURATION,
                           String.valueOf(duration));
    }

    request.addParameter(Constants.SERVLET_PARAM_TIME_BETWEEN_STARTUPS,
                         String.valueOf(delayBetweenIterations));

    request.addParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS,
                         String.valueOf(numClients));

    if ((requestedClients != null) && (requestedClients.length > 0))
    {
      String requestedClientsStr = requestedClients[0];
      for (int i=1; i < requestedClients.length; i++)
      {
        requestedClientsStr += '\n' + requestedClients[i];
      }
      request.addParameter(Constants.SERVLET_PARAM_JOB_CLIENTS,
                           requestedClientsStr);
    }

    if ((monitorClients != null) && (monitorClients.length > 0))
    {
      String monitorClientsStr = monitorClients[0];
      for (int i=1; i < monitorClients.length; i++)
      {
        monitorClientsStr += '\n' + monitorClients[i];
      }
      request.addParameter(Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS,
                           monitorClientsStr);
    }

    if (monitorClientsIfAvailable)
    {
      request.addParameter(
           Constants.SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE, "on");
    }

    request.addParameter(Constants.SERVLET_PARAM_JOB_THREADS_MIN,
                         String.valueOf(minThreads));

    if (maxThreads > 0)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_THREADS_MAX,
                           String.valueOf(maxThreads));
    }

    request.addParameter(Constants.SERVLET_PARAM_THREAD_INCREMENT,
                         String.valueOf(threadIncrement));

    request.addParameter(Constants.SERVLET_PARAM_JOB_COLLECTION_INTERVAL,
                         String.valueOf(collectionInterval));

    for (int i=0; i < optimizationParameters.length; i++)
    {
      if ((optimizationParameters[i] instanceof PlaceholderParameter) ||
          (optimizationParameters[i] instanceof LabelParameter))
      {
        continue;
      }

      String name = Constants.SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX +
                    optimizationParameters[i].getName();
      try
      {
        request.addEncodedParameter(name,
             optimizationParameters[i].getHTMLPostValue());
      }
      catch (UnsupportedEncodingException uee)
      {
        request.addParameter(name,
                             optimizationParameters[i].getHTMLPostValue());
      }
    }

    request.addParameter(Constants.SERVLET_PARAM_JOB_MAX_NON_IMPROVING,
                         String.valueOf(nonImprovingIterations));

    if (rerunBestIteration)
    {
      request.addParameter(Constants.SERVLET_PARAM_RERUN_BEST_ITERATION, "on");
    }

    if (rerunDuration > 0)
    {
      request.addParameter(Constants.SERVLET_PARAM_RERUN_DURATION,
                   String.valueOf(rerunDuration));
    }

    if ((dependencyID != null) && (dependencyID.length() > 0))
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_DEPENDENCY,
                           dependencyID);
    }

    if ((notifyAddresses != null) && (notifyAddresses.length > 0))
    {
      String notifyStr = notifyAddresses[0];
      for (int i=1; i < notifyAddresses.length; i++)
      {
        notifyStr += ',' + notifyAddresses[i];
      }
      request.addParameter(Constants.SERVLET_PARAM_JOB_NOTIFY_ADDRESS,
                           notifyStr);
    }

    for (int i=0; i < jobSpecificParameters.length; i++)
    {
      String valueStr = jobSpecificParameters[i].getHTMLPostValue();
      if (valueStr != null)
      {
        // The value is already encoded, and doing it again would cause
        // problems. Therefore, manually, construct the post string.
        String name = Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                      jobSpecificParameters[i].getName();
        try
        {
          request.addEncodedParameter(name, valueStr);
        }
        catch (UnsupportedEncodingException uee)
        {
          request.addParameter(name, valueStr);
        }
      }
    }


    // Send the request and read the response.
    HTTPResponse response;
    try
    {
      response = httpClient.sendRequest(request);
      httpClient.closeAll();
    }
    catch (Exception e)
    {
      System.err.println("Error communicating with SLAMD server:");
      e.printStackTrace();
      return null;
    }


    // Make sure that the response status code was "200".
    if (response.getStatusCode() != 200)
    {
      System.err.println("ERROR:  Unexpected response status code (" +
                         response.getStatusCode() + ' ' +
                         response.getResponseMessage() + ')');
      return null;
    }


    // See if the response header included an optimizing job ID.  If so, then
    // the job was scheduled successfully.  If not, then there was a problem to
    // prevent it from being scheduled.
    String optimizingJobID =
         response.getHeader(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if (optimizingJobID == null)
    {
      System.err.println("ERROR:  Unable to determine the optimizing job ID " +
                         "from the response.");
      System.err.println("This probably means that the job was not scheduled " +
                         "properly.");

      if (verboseMode)
      {
        System.err.println("The body of the HTTP response was:");
        try
        {
          byte[] responseData = response.getResponseData();
          BufferedReader reader =
               new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(responseData)));
          String line = reader.readLine();
          while (line != null)
          {
            System.out.println(line);
            line = reader.readLine();
          }

          reader.close();
        }
        catch (Exception e)
        {
          System.err.println("Error decoding response data:");
          e.printStackTrace();
        }
      }

      return null;
    }
    else
    {
      System.out.println("Successfully scheduled optimizing job " +
                         optimizingJobID + " using config file \"" +
                         configFile + "\".");
      return optimizingJobID;
    }
  }



  /**
   * Prints the provided message if the program is operating in verbose mode.
   *
   * @param  message  The message to be printed.
   */
  public void debug(String message)
  {
    if (verboseMode)
    {
      System.out.println(message);
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String eol = Constants.EOL;

    System.out.println(
"USAGE:  java -cp {classpath} CommandLineJobScheduler {options}" + eol +
"        where {options} for running a job include:" + eol +
"-f {configFile} -- The configuration file to use to schedule the job" + eol +
"-h {slamdHost}  -- The address of the SLAMD server" + eol +
"-p {slamdPort}  -- The port of the SLAMD server's admin interface" + eol +
"-A {authID}     -- The username to use to authenticate to the server" + eol +
"-P {authPW}     -- The password to use to authenticate to the server" + eol +
"-D {jobID}      -- The job on which this job should be dependent" + eol +
"-u {uri}        -- The URI to which the request should be sent" + eol +
"-S              -- Communicate with the SLAMD server over SSL" + eol +
"-d              -- Make scheduled jobs interdependent" + eol +
"-v              -- Enable verbose mode" + eol + eol +
"       where {options} for generating a config file include:" + eol +
"-g {className}  -- Generate a config file for the specified job class" + eol +
"-f {configFile} -- The configuration file to use to schedule the job" + eol +
"-O              -- Generate a config file for an optimizing job" + eol +
"-o {className}  -- Use the specified optimization algorithm" + eol + eol +
"       where {options} for displaying usage include:" + eol +
"-H              -- Display usage information and exit"
                      );
  }
}

