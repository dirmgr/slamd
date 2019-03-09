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



/**
 * This class defines a thread that works in conjunction with the replication
 * latency resource monitor to ensure that it stops in a timely manner.  Under
 * certain conditions (e.g., if replication has broken or is lagging very far
 * behind), the replication latency resource monitor could potentially take a
 * very long time to receive notification of a change that occurred, which
 * would delay any further processing until that notification is received.
 *
 *
 * @author   Neil A. Wilson
 */
public class ReplicationLatencyResourceMonitorThread
       extends Thread
{
  // Indicates whether the client has requested that this thread be stopped.
  private boolean threadStopRequested;

  // The replication latency resource monitor that this thread is watching.
  private ReplicationLatencyResourceMonitor latencyMonitor;



  /**
   * Creates a new instance of this thread that will monitor the provided
   * replication latency resource monitor.
   *
   * @param  latencyMonitor  The replication latency resource monitor that will
   *                         be watched.
   */
  public ReplicationLatencyResourceMonitorThread(
              ReplicationLatencyResourceMonitor latencyMonitor)
  {
    this.latencyMonitor = latencyMonitor;

    threadStopRequested = false;
  }



  /**
   * Runs this thread, watching the provided latency check resource monitor and
   * stopping it if necessary.
   */
  @Override()
  public void run()
  {
    while (! (latencyMonitor.isStopped || threadStopRequested))
    {
      if (latencyMonitor.shouldStop() && latencyMonitor.waitingOnPSearch)
      {
        try
        {
          latencyMonitor.interrupt();
        }
        catch (Exception e) {}
      }

      try
      {
        Thread.sleep(1000);
      }
      catch (Exception e) {}
    }
  }



  /**
   * Requests that this thread stop watching the associated replication latency
   * resource monitor.
   */
  public void requestStop()
  {
    threadStopRequested = true;
  }
}

