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
package com.slamd.loadvariance;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;

import com.slamd.common.Constants;
import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class defines a SLAMD job that varies the load that it generates over
 * time based on input read from a file that controls the number of threads that
 * should be active at any given time.  It works by creating and starting all
 * threads at the beginning of the job but making them remain inactive until
 * they are needed.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class LoadVarianceJobClass
       extends JobClass
{
  /**
   * The default length of time in milliseconds that an idle thread should sleep
   * between checks to determine whether it is time to start running.
   */
  public static final int DEFAULT_IDLE_SLEEP_DURATION = 100;



  /**
   * The name of the parameter that will be used to specify the URL to the file
   * containing the load definition.
   */
  public static final String PARAM_LOAD_DEFINITION = "load_definition";



  // Indicates whether the job should loop through the load variance definition
  // once the end has been reached.
  public static boolean loopVarianceDefinition;

  // An array that is used to indicate whether each of the threads should
  // currently be active or inactive.
  public static boolean[] threadsActive;

  // The length of time in milliseconds that each thread should sleep while it
  // is idle before waking up to check to see if it should start running again.
  public static int idleSleepDuration;

  // An array that stores the duration between changes in the number of active
  // threads and the increase or decrease to make at those times.
  public static int[][] varianceData;


  // The reference to the thread that is used to keep track of starting and
  // stopping each of the individual job threads.
  public static LoadVarianceControlThread controlThread;



  /**
   * The default constructor used to create a new instance of the search thread.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public LoadVarianceJobClass()
  {
    super();
  }



  /**
   * Retrieves the parameter that is used to specify the URL to the data file
   * containing the variable load definition.
   *
   * @return  The parameter that is used to specify the URL to the data file
   *          containing the variable load definition.
   */
  public final Parameter getVariableLoadParameterStub()
  {
    return new FileURLParameter(PARAM_LOAD_DEFINITION, "Load Definition URL",
                                "The URL to the file containing the load " +
                                "definition to use for the job.", null, true);
  }



  /**
   * Validates the information in the load definition file both for its syntax
   * and for plausibility when used with the other provided information.
   *
   * @param  threadsPerClient  The number of threads per client that have been
   *                           scheduled for this job.
   * @param  startTime         The time that this job should start running.
   * @param  stopTime          The time that this job should stop running.
   * @param  duration          The maximum length of time in seconds that the
   *                           job should be allowed to run.
   * @param  parameters        The set of parameters associated with this job,
   *                           including the load definition URL parameter.
   *
   * @throws  InvalidValueException  If an error occurs during validation.
   */
  public final void validateLoadDefinition(int threadsPerClient, Date startTime,
                                           Date stopTime, int duration,
                                           ParameterList parameters)
         throws InvalidValueException
  {
    // Get the contents of the load definition file.
    FileURLParameter loadFileParameter =
         parameters.getFileURLParameter(PARAM_LOAD_DEFINITION);
    if ((loadFileParameter == null) || (! loadFileParameter.hasValue()))
    {
      throw new InvalidValueException("No load definition URL was provided.");
    }

    String[] loadDefinitionLines;
    try
    {
      loadDefinitionLines = loadFileParameter.getNonBlankFileLines();
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Unable to retrieve the contents of " +
                                      "load definition file:  " + e, e);
    }

    if ((loadDefinitionLines == null) || (loadDefinitionLines.length == 0))
    {
      throw new InvalidValueException("No data was found in the load " +
                                      "definition file.");
    }


    // Parse each line to ensure that it is valid and that it does not attempt
    // to create a total number of threads greater than what has been scheduled.
    for (int i=0; i < loadDefinitionLines.length; i++)
    {
      // Each line should be tab-delimited using the following format:
      // 1.  The length of time in seconds that should elapse between the end
      //     of the previous instruction and the beginning of this one.
      // 2.  The length of time over which this instruction should be carried
      //     out.
      // 3.  The fully-qualified name of the Java class that defines the
      //     variation algorithm.
      // 4+  Any arguments that should be provided to the variation algorithm,
      //     including the change in threads over time.
      try
      {
        StringTokenizer tokenizer = new StringTokenizer(loadDefinitionLines[i],
                                                        "\t");

        int    delayBeforeExecution  = Integer.parseInt(tokenizer.nextToken());
        int    durationOfVariance    = Integer.parseInt(tokenizer.nextToken());
        String algorithmName         = tokenizer.nextToken();

        ArrayList<String> argumentList = new ArrayList<String>();
        while (tokenizer.hasMoreTokens())
        {
          argumentList.add(tokenizer.nextToken());
        }
        String[] arguments = new String[argumentList.size()];
        argumentList.toArray(arguments);

        Class<?> algorithmClass = Constants.classForName(algorithmName);
        LoadVarianceAlgorithm loadVariationAlgorithm =
             (LoadVarianceAlgorithm) algorithmClass.newInstance();
        loadVariationAlgorithm.initializeVariationAlgorithm(arguments);
      }
      catch (Exception e)
      {
        throw new InvalidValueException("Error while trying to parse line " +
                                        (i+1) + " of load definition file:  " +
                                        e, e);
      }
    }
  }



  /**
   * Initializes the logic that will be used to generate load for this thread.
   * This should be called by the <CODE>initializeClient</CODE> method, as it
   * uses static variables to determine the logic to use for all threads.  The
   * <CODE>runJob</CODE> method will be used to apply this definition on a
   * per-thread basis.
   *
   * @param  parameters  The parameter list containing the parameters for this
   *                     job, including the load definition URL parameter.
   *
   * @throws  UnableToRunException  If a problem occurs while parsing the load
   *                                definition file that would keep this job
   *                                from running properly.
   */
  public final void initializeVariableLoad(ParameterList parameters)
         throws UnableToRunException
  {
    // Initialize variables that we will need to convert the contents of the
    // load definition file into actual numbers.
    ArrayList<int[]> varianceList  = new ArrayList<int[]>();
    int       activeThreads = 0;
    int       currentOffset = 0;
    int       totalThreads  = getClientSideJob().getThreadsPerClient();


    // Set the default idle sleep time.
    idleSleepDuration = DEFAULT_IDLE_SLEEP_DURATION;


    // Indicate that the load variance definition should not be looped by
    // default.
    loopVarianceDefinition = false;


    // Get the contents of the load definition file.
    FileURLParameter loadFileParameter =
         parameters.getFileURLParameter(PARAM_LOAD_DEFINITION);
    if ((loadFileParameter == null) || (! loadFileParameter.hasValue()))
    {
      throw new UnableToRunException("No load definition URL was provided.");
    }

    String[] loadDefinitionLines;
    try
    {
      loadDefinitionLines = loadFileParameter.getNonBlankFileLines();
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to retrieve the contents of " +
                                     "load definition file:  " + e, e);
    }

    if ((loadDefinitionLines == null) || (loadDefinitionLines.length == 0))
    {
      throw new UnableToRunException("No data was found in the load " +
                                     "definition file.");
    }


    // Parse each line to ensure that it is valid and that it does not attempt
    // to create a total number of threads greater than what has been scheduled.
    for (int i=0; i < loadDefinitionLines.length; i++)
    {
      // Each line should be tab-delimited using the following format:
      // 1.  The length of time in seconds that should elapse between the end
      //     of the previous instruction and the beginning of this one.
      // 2.  The length of time over which this instruction should be carried
      //     out.
      // 3.  The fully-qualified name of the Java class that defines the
      //     variation algorithm.
      // 4+  Any arguments that should be provided to the variation algorithm,
      //     including the change in threads over time.
      try
      {
        StringTokenizer tokenizer = new StringTokenizer(loadDefinitionLines[i],
                                                        "\t");

        int    delayBeforeExecution  = Integer.parseInt(tokenizer.nextToken());
        int    durationOfVariance    = Integer.parseInt(tokenizer.nextToken());
        String algorithmName         = tokenizer.nextToken();

        ArrayList<String> argumentList = new ArrayList<String>();
        while (tokenizer.hasMoreTokens())
        {
          argumentList.add(tokenizer.nextToken());
        }
        String[] arguments = new String[argumentList.size()];
        argumentList.toArray(arguments);

        Class<?> algorithmClass = Constants.classForName(algorithmName);
        LoadVarianceAlgorithm varianceAlgorithm =
             (LoadVarianceAlgorithm) algorithmClass.newInstance();
        varianceAlgorithm.initializeVariationAlgorithm(arguments);
        int[][] varianceInfo =
             varianceAlgorithm.calculateVariance(durationOfVariance,
                                                 totalThreads, activeThreads);

        currentOffset += (1000 * delayBeforeExecution);
        for (int j=0; j < varianceInfo.length; j++)
        {
          int[] varianceElement = varianceInfo[j];
          varianceElement[0] += currentOffset;
          activeThreads      += varianceElement[1];
          varianceList.add(varianceElement);
        }

        currentOffset += (1000 * durationOfVariance);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        throw new UnableToRunException("Error while trying to parse line " +
                                       (i+1) + " of load definition file:  " +
                                       e, e);
      }
    }


    // Update the array containing the variance data.
    varianceData = new int[varianceList.size()][];
    for (int i=0; i < varianceData.length; i++)
    {
      varianceData[i] = varianceList.get(i);
    }


    // Create the array that will be used to control the actions of each of the
    // threads.
    threadsActive = new boolean[totalThreads];
    Arrays.fill(threadsActive, false);


    // Create the thread that will be used to keep track of all the actual job
    // threads.
    controlThread = new LoadVarianceControlThread(this);
    controlThread.start();
  }



  /**
   * Retrieves the length of time in milliseconds that each thread will sleep
   * while it is idle before checking to determine whether it is time to start
   * running.
   *
   * @return  The length of time in milliseconds that each thread should sleep
   *          while it is idle before checking to determine if it is time to
   *          start running.
   */
  public int getIdleSleepDuration()
  {
    return idleSleepDuration;
  }



  /**
   * Specifies the length of time in milliseconds that each thread should sleep
   * while it is idle before checking to determine whether it is time to start
   * running.  Note that this method should always be called after the call to
   * <CODE>initializeVariableLoad</CODE> or it will be overridden.
   *
   * @param  idleSleepDuration  The length of time in milliseconds that each
   *                            thread should sleep while it is idle before
   *                            checking to determine whether it is time to
   *                            start running.
   */
  public void setIdleSleepDuration(int idleSleepDuration)
  {
    this.idleSleepDuration = idleSleepDuration;
  }



  /**
   * Indicates whether the job should loop back through the load variance
   * definition when the end is reached.  If not, then only those threads that
   * were active at the end of the load variance definition will continue to be
   * used for the remainder of the job.
   *
   * @return  <CODE>true</CODE> if the job should loop back through the load
   *          variance definition when the end is reached, or <CODE>false</CODE>
   *          if not.
   */
  public boolean loopVarianceDefinition()
  {
    return loopVarianceDefinition;
  }



  /**
   * Specifies whether the job should loop back through the load variance
   * definition when the end is reached.
   *
   * @param  loopVarianceDefinition  Indicates whether the job should loop back
   *                                 through the load variance definition when
   *                                 the end is reached.
   */
  public void setLoopVarianceDefinition(boolean loopVarianceDefinition)
  {
    this.loopVarianceDefinition = loopVarianceDefinition;
  }



  /**
   * Performs the processing associated with this job.  In the generic case, it
   * completes the initialization and then enters a loop that waits until an
   * indication is received that the thread should start processing.  If it is
   * notified to temporarily stop, then it will do so and wait for either a
   * permanent stop request or notification that it should start again.  Once a
   * permanent stop request is received, then this method will perform
   * finalization and exit.
   */
  @Override()
  public final void runJob()
  {
    doStartup();

    controlThread.startRunning();

    while (true)
    {
      if (threadsActive[getThreadNumber()] && (! shouldStop()))
      {
        doPreProcessing();
        doProcessing();
        doPostProcessing();
      }
      else
      {
        if (shouldStop())
        {
          break;
        }
        else
        {
          try
          {
            Thread.sleep(idleSleepDuration);
          } catch (Exception e) {}
        }
      }
    }

    controlThread.stopRunning();

    doShutdown();
  }



  /**
   * Indicates whether this thread should temporarily pause its execution or
   * stop altogether.  This method should be periodically called by the
   * <CODE>doProcessing</CODE> method, and if it returns <CODE>true</CODE> then
   * <CODE>doProcessing</CODE> should exit.
   *
   * @return  <CODE>true</CODE> if the thread should pause or stop its
   *          execution, or <CODE>false</CODE> if not.
   */
  public final boolean shouldPauseOrStop()
  {
    if (threadsActive[getThreadNumber()])
    {
      if (shouldStop())
      {
        threadsActive[getThreadNumber()] = false;
        return true;
      }
      else
      {
        return false;
      }
    }
    else
    {
      return true;
    }
  }



  /**
   * Performs any processing that should be done immediately before each call to
   * <CODE>doProcessing()</CODE>.  By default, no action is performed.
   */
  public void doPreProcessing()
  {
    // No implementation required by default.
  }



  /**
   * Performs any processing that should be done immediately after each call to
   * <CODE>doProcessing()</CODE>.  By default, no action is performed.
   */
  public void doPostProcessing()
  {
    // No implementation required by default.
  }



  /**
   * Performs any processing that should be done at the very beginning of
   * execution before any actual processing is performed.  This should include
   * starting all stat trackers for the job.
   */
  public abstract void doStartup();



  /**
   * Performs any processing that should be done at the very end of execution
   * after all actual processing has completed.  This should include stopping
   * all stat trackers for the job.
   */
  public abstract void doShutdown();



  /**
   * Performs the actual processing for this job.  It should periodically call
   * the <CODE>shouldPauseOrStop</CODE> method and if it returns
   * <CODE>true</CODE> then this method should exit.
   */
  public abstract void doProcessing();
}

