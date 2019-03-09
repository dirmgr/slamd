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



import java.io.IOException;
import java.util.ArrayList;

import com.slamd.common.Constants;
import com.slamd.db.SLAMDDB;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;



/**
 * This class implements a thread that can be used to asynchronously write
 * information to a specified log file.  It works in conjunction with the SLAMD
 * logger so that when the SLAMD logger is configured to work asynchronously it
 * will write any messages to be logged into a memory-based queue.  The logger
 * thread will then periodically poll that queue to see if there are any
 * messages to be logged.  If so, then those messages will be written into the
 * log file and the queue emptied.  In this manner, the overhead associated with
 * logging will not adversely impact the performance of the SLAMD server.  Note
 * that there will not be any debug logging performed in this class in order to
 * prevent logging loops.  However, significant events will be written to
 * standard error.
 *
 *
 * @author   Neil A. Wilson
 */
public class LoggerThread
       extends Thread
       implements ConfigSubscriber
{
  /**
   * The name used to register the logger as a subscriber to the configuration
   * handler.
   */
  public static final String CONFIG_SUBSCRIBER_NAME = "SLAMD Logger Thread";



  // Variables used to refer to the SLAMD server and its components
  private SLAMDDB     configDB;
  private Logger      logger;
  private SLAMDServer slamdServer;


  // A variable that controls how long the thread should sleep between polls of
  // the logger's queue.
  private int pollDelay;



  /**
   * Creates the logging thread and retrieves any pertinent configuration
   * information from the configuration handler.
   *
   * @param  slamdServer  The SLAMD server with which this logging thread will
   *                      be associated.
   * @param  logger       The SLAMD logger whose queue this logging thread will
   *                      watch.
   */
  public LoggerThread(SLAMDServer slamdServer, Logger logger)
  {
    setName("Logger Thread");

    // Initialize the instance variables
    this.slamdServer = slamdServer;
    this.logger      = logger;


    // Get the configuration handler and register as a subscriber
    configDB = slamdServer.getConfigDB();
    configDB.registerAsSubscriber(this);


    // Retrieve the poll delay from the configuration
    pollDelay = Constants.DEFAULT_LOG_POLL_DELAY;
    String pollDelayStr =
         configDB.getConfigParameter(Constants.PARAM_LOG_POLL_DELAY);
    if ((pollDelayStr != null) && (pollDelayStr.length() > 0))
    {
      try
      {
        pollDelay = Integer.parseInt(pollDelayStr);
      }
      catch (NumberFormatException nfe)
      {
        System.err.println(slamdServer.getTimestamp() +
                           "WARNING -- " + Constants.PARAM_LOG_POLL_DELAY +
                           " should be numeric");
      }
    }
  }



  /**
   * Performs the work of actually polling the logger's queue and writing any
   * information that it finds there into the log file.  Any problems
   * encountered in this process will be written to standard error.
   */
  @Override()
  public void run()
  {
    // Make sure that we should actually be running.  If the SLAMD server is
    // logging synchronously, then there is no need for this.
    if (! logger.logAsynchronously)
    {
      return;
    }


    // Once a request has been made to close the logger, then there is no more
    // need to continue polling.
    while (! logger.closeRequested)
    {
      // Define a list that we will use if there is actually something to be
      // logged.
      ArrayList messagesToLog = null;


      // Get the current time so that we can figure out how long to sleep when
      // the loop is over
      long startTime = System.currentTimeMillis();


      // First, check to see if there is anything to be logged.  Grab the lock
      // on buffer so that we know we have exclusive access to it, but only
      // keep it for as little time as possible so that we don't hold up
      // calls to log other messages
      synchronized (logger.loggerMutex)
      {
        if (! logger.logBuffer.isEmpty())
        {
          // There are messages to be logged, so steal that buffer away for
          // ourselves and replace it with a new empty one.
          messagesToLog = logger.logBuffer;
          logger.logBuffer = new ArrayList<String>();
        }
      }


      // If there were messages to be logged, then write them to the log file
      if (messagesToLog != null)
      {
        synchronized (logger.writerMutex)
        {
          for (int i=0; i < messagesToLog.size(); i++)
          {
            String message = (String) messagesToLog.get(i);
            try
            {
              logger.logWriter.write(message);
              logger.logWriter.newLine();
            }
            catch (IOException ioe)
            {
              System.err.println(slamdServer.getTimestamp() +
                                 "ERROR writing message \"" + message +
                                 "\" to log file:  " + ioe);
            }
          }

          try
          {
            logger.logWriter.flush();
          } catch (IOException ioe) {}
        }
      }


      // Determine how long we need to sleep until the next poll
      long sleepTime = startTime + (pollDelay*1000) -
                       System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {}
      }
    }


    // If we have gotten to this point, then a request has been made to close
    // the logger.  First, check to see if there are any more messages to be
    // logged.  If so, then write them to disk.
    synchronized (logger.loggerMutex)
    {
      for (int i=0; i < logger.logBuffer.size(); i++)
      {
        String message = logger.logBuffer.get(i);
        try
        {
          logger.logWriter.write(message);
          logger.logWriter.newLine();
        }
        catch (IOException ioe)
        {
            System.err.println(slamdServer.getTimestamp() +
                               "ERROR writing message \"" + message +
                               "\" to log file:  " + ioe);
        }
      }
    }


    // Finally, flush and close the log writer.
    synchronized (logger.writerMutex)
    {
      try
      {
        logger.logWriter.flush();
        logger.logWriter.close();
      } catch (IOException ioe) {}
    }
  }



  /**
   * Retrieves the name that the logger thread uses to subscribe to the
   * configuration handler in order to be notified of configuration changes.
   *
   * @return  The name that the logger thread uses to subscribe to the
   *          configuration handler in order to be notified of configuration
   *          changes.
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
    IntegerParameter pollDelayParameter =
         new IntegerParameter(Constants.PARAM_LOG_POLL_DELAY,
                              "Log Poll Delay",
                              "The delay in seconds between checking the log " +
                              "queue for messages to be logged (asynchronous " +
                              "logging only).", true,
                              pollDelay, true, 1, false, 0);


    Parameter[] params = new Parameter[]
    {
      pollDelayParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the logger thread.  In this
   * case, the only option is the delay to use between polls of the log buffer.
   */
  public void refreshSubscriberConfiguration()
  {
    pollDelay = Constants.DEFAULT_LOG_POLL_DELAY;
    String pollDelayStr =
         configDB.getConfigParameter(Constants.PARAM_LOG_POLL_DELAY);
    if ((pollDelayStr != null) && (pollDelayStr.length() > 0))
    {
      try
      {
        pollDelay = Integer.parseInt(pollDelayStr);
      }
      catch (NumberFormatException nfe)
      {
        System.err.println(slamdServer.getTimestamp() +
                           "WARNING -- " + Constants.PARAM_LOG_POLL_DELAY +
                           " should be numeric");
      }
    }
    else
    {
      pollDelay = Constants.DEFAULT_LOG_POLL_DELAY;
    }
  }



  /**
   * Re-reads all configuration information used by the logger thread.  In this
   * case, the only option is the delay to use between polls of the log buffer.
   *
   * @param  parameterName  The name of the parameter to be re-read from the
   *                        configuration.
   */
  public void refreshSubscriberConfiguration(String parameterName)
  {
    if (parameterName.equalsIgnoreCase(Constants.PARAM_LOG_POLL_DELAY))
    {
      pollDelay = Constants.DEFAULT_LOG_POLL_DELAY;
      String pollDelayStr =
           configDB.getConfigParameter(Constants.PARAM_LOG_POLL_DELAY);
      if ((pollDelayStr != null) && (pollDelayStr.length() > 0))
      {
        try
        {
          pollDelay = Integer.parseInt(pollDelayStr);
        }
        catch (NumberFormatException nfe)
        {
          System.err.println(slamdServer.getTimestamp() +
                             "WARNING -- " +
                             Constants.PARAM_LOG_POLL_DELAY +
                             " should be numeric");
        }
      }
      else
      {
        pollDelay = Constants.DEFAULT_LOG_POLL_DELAY;
      }
    }
  }
}

