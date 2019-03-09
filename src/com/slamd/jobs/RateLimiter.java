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
package com.slamd.jobs;



/**
 * This class defines a utility that can be used to limit the rate at which
 * operations are performed.  It works by setting a countdown timer to a given
 * length of time in milliseconds, starting the timer, allowing the caller to
 * perform an operation, and then sleeping for any remaining length of time.  If
 * the operation performed takes longer than the specified duration, then it
 * will not sleep at all.
 *
 *
 * @author   Neil A. Wilson
 */
public class RateLimiter
{
  // The length of time in milliseconds that this timer should be active.
  private int duration;

  // The time that this timer should stop sleeping.
  private long stopSleepTime;



  /**
   * Creates the rate limiter with the specified duration.
   *
   * @param  duration  The total length of time in milliseconds that should
   *                   elapse between the start of the timer and the limiter
   *                   should stop sleeping.
   */
  public RateLimiter(int duration)
  {
    this.duration = duration;

    stopSleepTime = 0;
  }



  /**
   * Retrieves the configured duration for this rate limiter.
   *
   * @return  The configured duration for this rate limiter.
   */
  public int getDuration()
  {
    return duration;
  }



  /**
   * Specifies the duration for this rate limiter.
   *
   * @param  duration  The duration for this rate limiter.
   */
  public void setDuration(int duration)
  {
    this.duration = duration;
  }



  /**
   * Resets and starts the timer for this rate limiter.
   */
  public void startTimer()
  {
    stopSleepTime = System.currentTimeMillis() + duration;
  }



  /**
   * Sleeps for whatever time is remaining on the timer.
   */
  public void sleepForRemainingTime()
  {
    long sleepTime = stopSleepTime - System.currentTimeMillis();
    if (sleepTime > 0)
    {
      try
      {
        Thread.sleep(sleepTime);
      } catch (Exception e) {}
    }
  }
}

