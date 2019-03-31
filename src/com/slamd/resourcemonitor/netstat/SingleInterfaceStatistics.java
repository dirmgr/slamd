/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.resourcemonitor.netstat;

import com.slamd.stat.LongValueTracker;

/**
 * Statistics for a single network interface is tracked in this class.
 */
public class SingleInterfaceStatistics extends InterfaceStatistics
{
  // The values we got the last time we checked.
  private long lastReceivedValue;
  private long lastSentValue;



  /**
   * Constructs statistics for a single network interface.
   *
   * @param name          interface name
   * @param receivedBytes received bytes tracker
   * @param sentBytes     sent bytes tracker
   */
  public SingleInterfaceStatistics(
      String name, LongValueTracker receivedBytes, LongValueTracker sentBytes)
  {
    super(name, receivedBytes, sentBytes);
    this.lastReceivedValue = -1;
    this.lastSentValue     = -1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  void recordSentValue(long value)
  {
    if (lastSentValue >= 0)
    {
      final Long diff = value - lastSentValue;

      rawSentBytes.add(diff);
      if (enableRealTimeStats)
      {
        sentBytes.addValue(diff);
      }
    }

    lastSentValue = value;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  void recordReceivedValue(long value)
  {
    if (lastReceivedValue >= 0)
    {
      final Long diff = value - lastReceivedValue;

      rawReceivedBytes.add(diff);
      if (enableRealTimeStats)
      {
        receivedBytes.addValue(diff);
      }
    }

    lastReceivedValue = value;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  void completeIteration()
  {
    // nothing to do
  }
}
