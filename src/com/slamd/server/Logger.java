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
package com.slamd.server;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.slamd.common.Constants;
import com.slamd.db.SLAMDDB;
import com.slamd.job.JobClass;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.StringParameter;



/**
 * This class handles all logging performed by the SLAMD server.  It is a
 * multithreaded component to allow changes to be logged with minimal impact on
 * the performance of other components in the SLAMD server that use the logger
 * (like the scheduler).  However, configuration parameters make it possible to
 * customize this behavior so that logging can be done synchronously if this is
 * desired for some reason (e.g., debugging purposes).  Note that minimal
 * logging will be performed in this class to prevent logging loops, but any
 * significant problems will be logged to standard error.
 *
 *
 * @author   Neil A. Wilson
 */
public class Logger
       implements ConfigSubscriber
{
  /**
   * The name used to register the logger as a subscriber to the configuration
   * handler.
   */
  public static final String CONFIG_SUBSCRIBER_NAME = "SLAMD Logger";



  // Variables that are used in the actual logging process
  private   boolean        alwaysFlush;
  protected boolean        closeRequested;
  protected boolean        logAsynchronously;
  private   boolean        loggerEnabled;
  protected BufferedWriter logWriter;
  private   String         logFilename;

  protected final Object loggerMutex;
  protected final Object writerMutex;


  // The configuration database for the SLAMD server.
  private SLAMDDB configDB;


  // Variables related to asynchronous logging
  private   LoggerThread      loggerThread;
  protected ArrayList<String> logBuffer;


  // The SLAMD server with which this logger is associated
  private SLAMDServer slamdServer;



  /**
   * Creates a new instance of a logger to work with the provided SLAMD server.
   *
   * @param  slamdServer  The SLAMD server instance with which this logger is to
   *                      be associated.
   *
   * @throws  SLAMDServerException  If a problem is encountered while creating
   *                                the logger.
   */
  public Logger(SLAMDServer slamdServer)
         throws SLAMDServerException
  {
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Entering Logger constructor");


    // Initialize all of the appropriate instance variables
    this.slamdServer  = slamdServer;
    alwaysFlush       = Constants.DEFAULT_LOG_ALWAYS_FLUSH;
    closeRequested    = false;
    logAsynchronously = Constants.DEFAULT_LOG_ASYNCHRONOUSLY;
    loggerEnabled     = Constants.DEFAULT_LOGGER_ENABLED;
    logFilename       = Constants.DEFAULT_LOG_FILENAME;
    loggerMutex       = new Object();
    writerMutex       = new Object();


    // Retrieve the configuration handler and register as a subscriber
    configDB = slamdServer.getConfigDB();
    configDB.registerAsSubscriber(this);


    // Read the values of the appropriate configuration parameters
    String paramValue =
         configDB.getConfigParameter(Constants.PARAM_LOGGER_ENABLED);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      loggerEnabled =
           (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
    }

    paramValue = configDB.getConfigParameter(Constants.PARAM_LOG_FILENAME);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      logFilename = paramValue;
    }

    paramValue = configDB.getConfigParameter(Constants.PARAM_LOG_ALWAYS_FLUSH);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      alwaysFlush =
           (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
    }

    paramValue =
         configDB.getConfigParameter(Constants.PARAM_LOG_ASYNCHRONOUSLY);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      logAsynchronously =
           (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
    }


    // Create the log writer for logging to the appropriate file
    try
    {
      logWriter = new BufferedWriter(new FileWriter(logFilename, true));
      slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                             "Opened log file " + logFilename);
    }
    catch (IOException ioe)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDServerException("Error opening log file " + logFilename +
                                     " -- " + ioe, ioe);
    }


    // If the logger is to operate asynchronously, then create the log buffer,
    // logger mutex, and logging thread
    if (logAsynchronously)
    {
      logBuffer      = new ArrayList<String>();
      loggerThread   = new LoggerThread(slamdServer, this);
      loggerThread.start();
      slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                             "Configured asynchronous logging");
    }
    else
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                             "Using synchronous logging");
    }


    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Leaving Logger constructor");
    slamdServer.setLoggerInitialized(true);
  }



  /**
   * Performs the work of actually logging the specified message.  If the logger
   * is configured to operate synchronously, then it is written immediately.  If
   * the logger is configured to operate asynchronously, then the message is
   * written to the log buffer to be picked up by the logger thread.
   *
   * @param  message  The message to be written to the log.
   */
  public void logMessage(String message)
  {
    // If the logger is disabled, then do nothing.
    if (! loggerEnabled)
    {
      return;
    }

    // No logging in this method (for obvious reasons)
    synchronized (loggerMutex)
    {
      if (closeRequested)
      {
        return;
      }


      if (logAsynchronously)
      {
        logBuffer.add(message);
      }
      else
      {
        try
        {
          synchronized (writerMutex)
          {
            logWriter.write(message);
            logWriter.newLine();

            if (alwaysFlush)
            {
              logWriter.flush();
            }
          }
        }
        catch (IOException ioe)
        {
          System.err.println(slamdServer.getTimestamp() +
                             "Error writing log message \"" + message + "\"--" +
                             ioe);
        }
      }
    }
  }



  /**
   * Sets a flag that indicates that the logger should stop operating.  If the
   * logger is working synchronously, then it actually closes the log file.
   * otherwise, it waits for the logging thread to complete.  This function will
   * not return until the logging subsystem has completely shut down.
   */
  public void closeLogger()
  {
    synchronized (loggerMutex)
    {
      // Set the flag indicating that the logger is to be shut down
      closeRequested = true;


      // If logging is done synchronously, then flush and close the log file and
      // exit from this method
      if (! logAsynchronously)
      {
        try
        {
          synchronized (writerMutex)
          {
            logWriter.flush();
            logWriter.close();
          }
        } catch (IOException ioe) {}
      }
    }


    // If we get here, then the logger is operating asynchronously.  Join the
    // logging thread to wait for it to finish.  We can't hold the logger mutex
    // at this time, because the logger thread needs to get it.
    if (logAsynchronously)
    {
      try
      {
        loggerThread.interrupt();
        loggerThread.join();
      } catch (InterruptedException ie) {}
    }


    // Indicate to the SLAMD server that the logger is no longer active.  We can
    // log a message indicating that the logger is shut down because it will now
    // be written to standard error if applicable
    slamdServer.setLoggerInitialized(false);
    slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                           "Logging system has been shut down");
  }



  /**
   * Retrieves the name that the logger uses to subscribe to the configuration
   * handler in order to be notified of configuration changes.
   *
   * @return  The name that the logger uses to subscribe to the configuration
   *          handler in order to be notified of configuration changes.
   */
  public String getSubscriberName()
  {
    return CONFIG_SUBSCRIBER_NAME;
  }



  /**
   * Retrieves the set of configuration parameters associated with this
   * configuration subscriber.
   *
   * @return  The set of configuration parameters associated with this
   *          configuration subscriber.
   */
  public ParameterList getSubscriberParameters()
  {
    BooleanParameter loggerEnabledParameter =
         new BooleanParameter(Constants.PARAM_LOGGER_ENABLED, "Logger Enabled",
                             "Indicates whether the logger will be used to " +
                             "record events in a log file.", loggerEnabled);
    StringParameter logFileParameter =
         new StringParameter(Constants.PARAM_LOG_FILENAME, "Log File Name",
                             "The absolute path to the SLAMD log file.",
                             true, logFilename);
    BooleanParameter logFlushParameter =
         new BooleanParameter(Constants.PARAM_LOG_ALWAYS_FLUSH,
                              "Always Flush to Disk",
                              "Indicates whether information written to the " +
                              "log file should be immediately flushed to disk.",
                              alwaysFlush);
    BooleanParameter logAsynchParameter =
         new BooleanParameter(Constants.PARAM_LOG_ASYNCHRONOUSLY,
                              "Log Asynchronously",
                              "Indicates whether information logged will be " +
                              "immediately written to the file or put into " +
                              "to be logged later for better performance " +
                              "(changes require a restart to take effect).",
                              logAsynchronously);


    Parameter[] params = new Parameter[]
    {
      loggerEnabledParameter,
      logFileParameter,
      logFlushParameter,
      logAsynchParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the logger.  In this
   * case, this is the name of the file to which to log and the flag indicating
   * whether synchronous logging should always flush after writing.  It is not
   * possible to dynamically switch between synchronous and asynchronous
   * logging.
   * <p>
   * If the name of the log file changes, then the logger writer will be closed
   * and a new one opened with the specified name.  There is no guarantee that
   * any messages currently in the log buffer will be written to the original
   * log file before it is closed (they may be written to the new log file
   *  instead).
   *
   * @throws  SLAMDServerException  If a new log filename is detected and there
   *                                is a problem while closing the existing log
   *                                file and/or opening the new one.
   */
  public void refreshSubscriberConfiguration()
         throws SLAMDServerException
  {
    // Determine whether the logger should be enabled.
    String paramValue =
         configDB.getConfigParameter(Constants.PARAM_LOGGER_ENABLED);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      loggerEnabled =
           (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
    }
    else
    {
      loggerEnabled = Constants.DEFAULT_LOGGER_ENABLED;
    }

    // Read the name of the log file to use.  If it is different than the
    // current log file, then switch the logger to use the new file.
    String origLogFilename = logFilename;
    paramValue = configDB.getConfigParameter(Constants.PARAM_LOG_FILENAME);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      logFilename = paramValue;
      if ((logFilename == null) || (logFilename.length() == 0))
      {
        logFilename = origLogFilename;
      }
      if (! origLogFilename.equals(logFilename))
      {
        synchronized (writerMutex)
        {
          // It is possible that the logger has not yet been opened, so don't
          // log any errors if this fails.
          try
          {
            logWriter.flush();
            logWriter.close();
          } catch (IOException ioe) {}


          // Open the new log file so it will be used for future messages
          try
          {
            logWriter = new BufferedWriter(new FileWriter(logFilename, true));
            slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                                   "Opened log file " + logFilename);
          }
          catch (IOException ioe)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                   JobClass.stackTraceToString(ioe));
            throw new SLAMDServerException("Error opening log file " +
                                           logFilename + " -- " + ioe, ioe);
          }
        }
      }
    }
    else
    {
      logFilename = Constants.DEFAULT_LOG_FILENAME;
    }


    // Read the indicator that determines whether to always flush after
    // synchronous writes
    paramValue = configDB.getConfigParameter(Constants.PARAM_LOG_ALWAYS_FLUSH);
    if ((paramValue != null) && (paramValue.length() > 0))
    {
      alwaysFlush = ! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE);
    }
    else
    {
      alwaysFlush = Constants.DEFAULT_LOG_ALWAYS_FLUSH;
    }
  }



  /**
   * Re-reads the configuration for the specified parameter, if it is applicable
   * to the logger.  Only the name of the log file and the flag indicating
   * whether synchronous logging should always flush after writing may be
   * dynamically reconfigured.  It is not possible to dynamically switch between
   * synchronous and asynchronous logging.
   * <p>
   * If the name of the log file changes, then the logger writer will be closed
   * and a new one opened with the specified name.  There is no guarantee that
   * any messages currently in the log buffer will be written to the original
   * log file before it is closed (they may be written to the new log file
   *  instead).
   *
   * @param  parameterName  The name of the parameter to be re-read from the
   *                        configuration.
   *
   * @throws  SLAMDServerException  If a new log filename is detected and there
   *                                is a problem while closing the existing log
   *                                file and/or opening the new one.
   */
  public void refreshSubscriberConfiguration(String parameterName)
         throws SLAMDServerException
  {
    // Read the indicator that specifies whether logging should be used.
    if (parameterName.equalsIgnoreCase(Constants.PARAM_LOGGER_ENABLED))
    {
      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_LOGGER_ENABLED);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        loggerEnabled =
             (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
      }
      else
      {
        loggerEnabled = Constants.DEFAULT_LOGGER_ENABLED;
      }
    }


    // Read the name of the log file to use.  If it is different than the
    // current log file, then switch the logger to use the new file.
    if (parameterName.equalsIgnoreCase(Constants.PARAM_LOG_FILENAME))
    {
      String origLogFilename = logFilename;
      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_LOG_FILENAME);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        logFilename = paramValue;
        if ((logFilename == null) || (logFilename.length() == 0))
        {
          logFilename = origLogFilename;
        }
        if (! origLogFilename.equals(logFilename))
        {
          synchronized (writerMutex)
          {
            // It is possible that the logger has not yet been opened, so don't
            // log any errors if this fails.
            try
            {
              logWriter.flush();
              logWriter.close();
            } catch (Exception ioe) {}


            // Open the new log file so it will be used for future messages
            try
            {
              logWriter = new BufferedWriter(new FileWriter(logFilename, true));
              slamdServer.logMessage(Constants.LOG_LEVEL_TRACE,
                                     "Opened log file " + logFilename);
            }
            catch (IOException ioe)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                     JobClass.stackTraceToString(ioe));
              throw new SLAMDServerException("Error opening log file " +
                                             logFilename + " -- " + ioe, ioe);
            }
          }
        }
      }
      else
      {
        logFilename = Constants.DEFAULT_LOG_FILENAME;
      }
    }


    // Read the indicator that determines whether to always flush after
    // synchronous writes
    if (parameterName.equalsIgnoreCase(Constants.PARAM_LOG_ALWAYS_FLUSH))
    {
      String paramValue =
           configDB.getConfigParameter(Constants.PARAM_LOG_ALWAYS_FLUSH);
      if ((paramValue != null) && (paramValue.length() > 0))
      {
        alwaysFlush =
             (! paramValue.equalsIgnoreCase(Constants.CONFIG_VALUE_FALSE));
      }
      else
      {
        alwaysFlush = Constants.DEFAULT_LOG_ALWAYS_FLUSH;
      }
    }
  }
}

