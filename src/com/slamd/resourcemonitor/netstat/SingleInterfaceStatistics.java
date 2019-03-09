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
  void completeIteration()
  {
    // nothing to do
  }
}
