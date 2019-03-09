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
package com.slamd.resourcemonitor;



import java.util.ArrayList;
import java.util.Properties;

import com.slamd.job.JobClass;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.StatTracker;



/**
 * This class defines a set of methods that must be implemented by any class
 * that is to be used as a SLAMD resource monitor.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class ResourceMonitor
       extends Thread
{
  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running an unknown or unrecognized operating system.
   */
  public static final int OS_TYPE_UNKNOWN = Constants.OS_TYPE_UNKNOWN;



  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running Solaris.
   */
  public static final int OS_TYPE_SOLARIS = Constants.OS_TYPE_SOLARIS;



  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running Linux.
   */
  public static final int OS_TYPE_LINUX = Constants.OS_TYPE_LINUX;



  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running HP-UX.
   */
  public static final int OS_TYPE_HPUX = Constants.OS_TYPE_HPUX;



  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running AIX.
   */
  public static final int OS_TYPE_AIX = Constants.OS_TYPE_AIX;



  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running Windows.
   */
  public static final int OS_TYPE_WINDOWS = Constants.OS_TYPE_WINDOWS;



  /**
   * The value that will be returned from the <CODE>getClientOS</CODE> method
   * if the client is running Mac OS X.
   */
  public static final int OS_TYPE_OSX = Constants.OS_TYPE_OSX;



  // The list of log messages associated with this monitor thread.
  private ArrayList<String> logMessages;

  // Indicates whether this resource monitor has stopped collecting statistics.
  private boolean hasStopped;

  // Indicates whether this resource monitor should stop collecting statistics.
  private volatile boolean shouldStop;

  // The time that this resource monitor should start collecting statistics.
  private long startTime;

  // The time that this resource monitor should stop collecting statistics.
  private long stopTime;

  // The set of properties that indicate how this monitor thread should operate.
  private Properties monitorProperties;

  // The reference to the resource monitor client being used to capture
  // statistics.
  protected ResourceMonitorClient monitorClient;

  // The resource monitor job that is currently running.
  private ResourceMonitorJob monitorJob;

  // A reference to the resource monitor thread that is currently running.
  private volatile Thread monitorThread;



  /**
   * Initializes this resource monitor implementation.  This method will be
   * called before any other methods in this interface are invoked.
   *
   * @param  monitorClient      The reference to the resource monitor client
   *                            being used to capture statistics.
   * @param  monitorProperties  A set of properties that indicate how this
   *                            monitor thread should operate.
   *
   * @throws  SLAMDException  If a problem occurs while initializing the
   *                          resource monitor.
   */
  public final void initialize(ResourceMonitorClient monitorClient,
                         Properties monitorProperties)
         throws SLAMDException
  {
    this.monitorClient     = monitorClient;
    this.monitorProperties = monitorProperties;
    this.monitorJob        = null;

    shouldStop    = false;
    hasStopped    = false;
    monitorThread = null;

    initializeMonitor();
  }



  /**
   * Performs any initialization specific to this resource monitor.
   *
   * @throws  SLAMDException  If a problem occurs while performing the
   *                          initialization.
   */
  public abstract void initializeMonitor()
         throws SLAMDException;



  /**
   * Retrieves the reference to the resource monitor client with which this
   * monitor thread is associated.
   *
   * @return  The reference to the resource monitor client with which this
   *          monitor thread is associated.
   */
  public final ResourceMonitorClient getMonitorClient()
  {
    return monitorClient;
  }



  /**
   * Retrieves the configuration properties associated with this resource
   * monitor thread.
   *
   * @return  The configuration properties associated with this resource monitor
   *          thread.
   */
  public final Properties getMonitorProperties()
  {
    return monitorProperties;
  }



  /**
   * Retrieves the resource monitor job that is currently running.
   *
   * @return  The resource monitor job that is currently running, or
   *          <CODE>null</CODE> if there is none.
   */
  public final ResourceMonitorJob getMonitorJob()
  {
    return monitorJob;
  }



  /**
   * Retrieves the configuration property with the specified name.
   *
   * @param  propertyName  The name of the property for which to retrieve the
   *                       value.
   *
   * @return  The configuration property with the specified name, or
   *          <CODE>null</CODE> if no property exists with that name.
   */
  public final String getProperty(String propertyName)
  {
    return monitorProperties.getProperty(propertyName);
  }



  /**
   * Retrieves the configuration property with the specified name.
   *
   * @param  propertyName  The name of the property for which to retrieve the
   *                       value.
   * @param  defaultValue  The value to return if no property exists with the
   *                       given name.
   *
   * @return  The configuration property with the specified name, or the given
   *          default value if no property exists with that name.
   */
  public final String getProperty(String propertyName, String defaultValue)
  {
    return monitorProperties.getProperty(propertyName, defaultValue);
  }



  /**
   * Retrieves the integer representation of the configuration property with the
   * specified name.
   *
   * @param  propertyName  The name of the property for which to retrieve the
   *                       value.
   * @param  defaultValue  The value to return if no property exists with the
   *                       given name, or if its value cannot be parsed as an
   *                       integer.
   *
   * @return  The integer representation of the configuration property with the
   *          specified name.
   */
  public final int getProperty(String propertyName, int defaultValue)
  {
    String propertyValue =
         monitorProperties.getProperty(propertyName,
                                       String.valueOf(defaultValue));

    try
    {
      return Integer.parseInt(propertyValue);
    }
    catch (NumberFormatException nfe)
    {
      return defaultValue;
    }
  }



  /**
   * Retrieves the Boolean representation of the configuration property with the
   * specified name.
   *
   * @param  propertyName  The name of the property for which to retrieve the
   *                       value.
   * @param  defaultValue  The value to return if no property exists with the
   *                       given name, or if its value cannot be parsed as a
   *                       Boolean.
   *
   * @return  The Boolean representation of the configuration property with the
   *          specified name.
   */
  public final boolean getProperty(String propertyName, boolean defaultValue)
  {
    String propertyValue =
         monitorProperties.getProperty(propertyName,
                                       String.valueOf(defaultValue));

    if (propertyValue.equalsIgnoreCase("true") ||
        propertyValue.equalsIgnoreCase("yes") ||
        propertyValue.equalsIgnoreCase("on") ||
        propertyValue.equalsIgnoreCase("1"))
    {
      return true;
    }
    else if (propertyValue.equalsIgnoreCase("false") ||
             propertyValue.equalsIgnoreCase("no") ||
             propertyValue.equalsIgnoreCase("off") ||
             propertyValue.equalsIgnoreCase("0"))
    {
      return false;
    }
    else
    {
      return defaultValue;
    }
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  public abstract String getMonitorName();



  /**
   * Indicates whether the current client system is supported for this resource
   * monitor.
   *
   * @return  <CODE>true</CODE> if the current client system is supported for
   *          this resource monitor, or <CODE>false</CODE> if not.
   */
  public abstract boolean clientSupported();



  /**
   * Creates a new instance of this resource monitor thread.  Note that the
   * <CODE>initialize()</CODE> method should have been called on the new
   * instance before it is returned.
   *
   * @return  A new instance of this resource monitor thread.
   *
   * @throws  SLAMDException  If a problem occurs while creating or initializing
   *                          the resource monitor.
   */
  public abstract ResourceMonitor newInstance()
         throws SLAMDException;



  /**
   * Initializes the stat trackers maintained by this resource monitor.
   *
   * @param  clientID            The client ID to use for the stubs.
   * @param  threadID            The thread ID to use for the stubs.
   * @param  collectionInterval  The collection interval to use for the stubs.
   */
  public abstract void initializeStatistics(String clientID, String threadID,
                                            int collectionInterval);



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  public abstract StatTracker[] getResourceStatistics();



  /**
   * Indicates that this resource monitor should start collecting statistics.
   *
   * @param  monitorJob  The resource monitor job with which the statistics will
   *                     be associated.
   */
  public final void startCollecting(ResourceMonitorJob monitorJob)
  {
    this.monitorJob = monitorJob;
    this.startTime  = monitorJob.getStartTime();
    this.stopTime   = monitorJob.getShouldStopTime();
    logMessages     = new ArrayList<String>();
    initializeStatistics(monitorClient.getClientHostname(), getMonitorName(),
                         monitorJob.getCollectionInterval());
    start();
  }



  /**
   * Indicates that this resource monitor should stop collecting statistics.
   */
  public final void stopCollecting()
  {
    shouldStop = true;
  }



  /**
   * Indicates that this resource monitor should stop collecting statistics.  It
   * will not return until the monitor has indicated that it has actually
   * stopped.
   */
  public final void stopAndWait()
  {
    shouldStop = true;

    long requestTime = System.currentTimeMillis();
    while (! hasStopped)
    {
      long now = System.currentTimeMillis();
      if (((now - requestTime) > 5000) && (monitorThread != null))
      {
        monitorThread.interrupt();
        requestTime = now;
      }

      try
      {
        Thread.sleep(10);
      } catch (InterruptedException ie) {}
    }
  }



  /**
   * Indicates whether this monitor thread should stop collecting statistics.
   *
   * @return  <CODE>true</CODE> if this monitor thread should stop collecting
   *          statistics, or <CODE>false</CODE> if not.
   */
  public final boolean shouldStop()
  {
    if ((stopTime > 0) && (System.currentTimeMillis() > stopTime))
    {
      shouldStop = true;
    }

    return shouldStop;
  }



  /**
   * Run the monitor.  Upon exiting, it will set a flag indicating that the
   * thread has stopped.
   */
  @Override()
  public final void run()
  {
    // Sleep until the start time arrives.
    long sleepTime = startTime - System.currentTimeMillis();
    while ((! shouldStop) && (sleepTime > 0))
    {
      long loopSleepTime;
      if (sleepTime > 1000)
      {
        loopSleepTime = 1000;
      }
      else
      {
        loopSleepTime = sleepTime;
      }

      try
      {
        Thread.sleep(loopSleepTime);
      } catch (InterruptedException ie) {}

      sleepTime = startTime - System.currentTimeMillis();
    }


    // Actually run the monitor thread.
    if (! shouldStop)
    {
      monitorThread = Thread.currentThread();

      try
      {
        runMonitor();
      }
      catch (Exception e)
      {
        String message = "Uncaught exception while processing resource " +
                         "monitor:  " + JobClass.stackTraceToString(e);
        monitorClient.logVerbose(message);
        logMessages.add(message);
      }


      monitorThread = null;
    }


    // Indicate that the thread is now done.
    hasStopped    = true;
    monitorJob.monitorDone(this);
    monitorJob  = null;
    logMessages = null;
  }



  /**
   * Performs the work of actually collecting resource statistics.  This method
   * should periodically call the <CODE>shouldStop()</CODE> method to determine
   * whether to stop collecting statistics.
   *
   * @return  A value that indicates the status of the monitor when it
   *          completed.
   */
  public abstract int runMonitor();



  /**
   * Retrieves the value indicating the operating system on which this resource
   * monitor is running.
   *
   * @return  The value indicating the operating system on which this resource
   *          monitor is running.
   */
  public final int getClientOS()
  {
    return monitorClient.getClientOS();
  }



  /**
   * Retrieves the hostname of the system on which this resource monitor is
   * running.
   *
   * @return  The hostname of the system on which this resource monitor is
   *          running.
   */
  public final String getClientHostname()
  {
    return monitorClient.getClientHostname();
  }



  /**
   * Writes the specified message to the log associated with this resource
   * monitor.
   *
   * @param  message  The message to be written to the log associated with this
   *                  resource monitor.
   */
  public final void logMessage(String message)
  {
    monitorClient.logVerbose(message);
    logMessages.add(message);
  }



  /**
   * Writes the specified message to the client message writer if it is
   * operating in verbose mode.
   *
   * @param  message  The message to be written.
   */
  public final void writeVerbose(String message)
  {
    monitorClient.logVerbose(message);
  }



  /**
   * Retrieves the set of messages that have been logged by this monitor.
   *
   * @return  The set of messages that have been logged by this monitor.
   */
  public final String[] getLogMessages()
  {
    String[] messages = new String[logMessages.size()];
    for (int i=0; i < messages.length; i++)
    {
      messages[i] = getMonitorName() + " -- " + logMessages.get(i);
    }
    return messages;
  }
}

