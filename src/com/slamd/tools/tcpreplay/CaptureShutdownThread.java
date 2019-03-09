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
package com.slamd.tools.tcpreplay;



/**
 * This class defines a thread that will be awoken when the JVM is in the
 * process of shutting down so that it can stop the capture process and flush
 * the capture file.
 *
 *
 * @author   Neil A. Wilson
 */
public class CaptureShutdownThread
       extends Thread
{
  // The capture daemon that is to be stopped when the JVM is shutting down.
  private CaptureDaemon captureDaemon;



  /**
   * Creates a new shutdown thread that will stop the provded capture daemon
   * when the JVM is shutting down.
   *
   * @param  captureDaemon  The capture daemon to stop when the JVM is shutting
   *                        down.
   */
  public CaptureShutdownThread(CaptureDaemon captureDaemon)
  {
    this.captureDaemon = captureDaemon;
  }



  /**
   * Signals the capture daemon to indicate that it should stop running.
   */
  @Override()
  public void run()
  {
    captureDaemon.stopCapture();
  }
}

