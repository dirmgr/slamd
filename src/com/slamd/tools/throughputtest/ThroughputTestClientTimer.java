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
package com.slamd.tools.throughputtest;



/**
 * This class defines a simple thread that will be used to sleep for some length
 * of time before waking up and notifying the throughput test client that the
 * specified length of time has elapsed.  This is a much more efficient way of
 * measuring
 *
 *
 * @author   Neil A. Wilson
 */
public class ThroughputTestClientTimer
       extends Thread
{
  // The length of time in seconds that this thread should sleep before
  // notifying the server.
  private int sleepTime;

  // The client that created this timer thread.
  private ThroughputTestClient throughputClient;



  /**
   * Creates a new timer thread with the provided information.
   *
   * @param  throughputClient  The client that created this timer thread.
   * @param  sleepTime         The length of time in seconds to sleep before
   *                           notifying the client.
   */
  public ThroughputTestClientTimer(ThroughputTestClient throughputClient,
                                   int sleepTime)
  {
    this.throughputClient = throughputClient;
    this.sleepTime        = sleepTime;


    // Make this a daemon thread so that it won't prevent the JVM from shutting
    // down if it's the last thread running.
    setDaemon(true);
  }



  /**
   * Sleeps for the specified length of time, at which point it will set a flag
   * in the client to indicate that it should stop.
   */
  public void run()
  {
    // Sleep for the specified duration.
    try
    {
      Thread.sleep(sleepTime * 1000);
    }
    catch (InterruptedException ie) {}


    throughputClient.shouldStop = true;
  }
}

