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



import java.util.Date;

import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a very simple SLAMD job that can be used to test load
 * variance algorithms.  It does not generate any actual load, but rather simply
 * has each thread increment a counter at regular intervals so that the graph
 * created from that statistic should have the desired appearance.
 *
 *
 * @author   Neil A. Wilson
 */
public class LoadVarianceTestJobClass
       extends LoadVarianceJobClass
{
  /**
   * The display name of the stat tracker that will be incremented to define the
   * overall shape of the load definition.
   */
  public static final String STAT_TRACKER_COUNTER_INCREMENTS =
       "Counter Increments";



  // The parameter that will be used to indicate whether to loop the load
  // variance definition if the end is reached.
  private BooleanParameter loopParameter =
       new BooleanParameter("loop_variance", "Loop Load Variance Definition",
                            "Indicates whether the job should loop the load " +
                            "variance definition when the end is reached or " +
                            "simply keep processing with the number of " +
                            "threads active at the end of the definition.",
                            false);

  // The parameter that will be used to obtain the length of time between
  // counter updates.
  private IntegerParameter updateParameter =
       new IntegerParameter("update_interval", "Update Interval (ms)",
                            "Specifies the length of time in milliseconds " +
                            "that should be allowed between updates of the " +
                            "internal counter.", true, 100, true, 0, false, 0);


  // The length of time in milliseconds that should be allowed between counter
  // increments.
  private static int updateInterval;


  // The thread number for this thread, which will be used for debugging
  // purposes.
  private int threadNumber;


  // The stat tracker that will be used to keep track of the total number of
  // threads active at any given time.
  private IncrementalTracker counter;



  /**
   * The default constructor used to create a new instance of the search thread.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public LoadVarianceTestJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Load Variance Test";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Test whether a load variation definition is configured properly";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to test whether a load variation definition is " +
      "configured properly to obtain the desired output."
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
      getVariableLoadParameterStub(),
      loopParameter,
      updateParameter
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
      new IncrementalTracker(clientID, threadID,
                             STAT_TRACKER_COUNTER_INCREMENTS,
                             collectionInterval)
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
      counter
    };
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
    // Make sure the load variance definition provided by the end user is
    // acceptable.
    validateLoadDefinition(threadsPerClient, startTime, stopTime, duration,
                           parameters);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    // Make sure to initialize the load variation algorithm.
    initializeVariableLoad(parameters);


    // Determine whether to loop the load variance definition.
    loopVarianceDefinition = false;
    loopParameter = parameters.getBooleanParameter(loopParameter.getName());
    if (loopParameter != null)
    {
      loopVarianceDefinition = loopParameter.getBooleanValue();
    }
    setLoopVarianceDefinition(loopVarianceDefinition);


    // Get the length of time between updates.
    updateParameter = parameters.getIntegerParameter(updateParameter.getName());
    if ((updateParameter != null) && updateParameter.hasValue())
    {
      updateInterval = updateParameter.getIntValue();
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
    // Create the stat tracker.
    counter = new IncrementalTracker(clientID, threadID,
                                     STAT_TRACKER_COUNTER_INCREMENTS,
                                     collectionInterval);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void doStartup()
  {
    threadNumber = getThreadNumber();
    writeVerbose("Initial startup for thread " + threadNumber);

    counter.startTracker();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void doShutdown()
  {
    counter.stopTracker();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void doProcessing()
  {
    writeVerbose("Starting processing on thread " + threadNumber);

    while (! shouldPauseOrStop())
    {
      counter.increment();

      try
      {
        Thread.sleep(updateInterval);
      } catch (Exception e) {}
    }

    writeVerbose("Stopped processing on thread " + threadNumber);
  }
}

