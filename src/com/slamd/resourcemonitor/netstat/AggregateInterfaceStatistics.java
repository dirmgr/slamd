package com.slamd.resourcemonitor.netstat;

import com.slamd.stat.LongValueTracker;

/**
 * Statistics for multiple network interfaces are aggregated in this class.
 * Since collectors may require several parsing iterations to collect statistics
 * for the interfaces in this aggregation, this class updates the trackers only
 * when the {@link #completeIteration()} method is called.
 */
public class AggregateInterfaceStatistics extends InterfaceStatistics
{
  // Total number of bytes sent/received in a time interval.
  private long receivedTotal;
  private long sentTotal;

  // The values we got the last time we checked.
  private long lastReceivedValue;
  private long lastSentValue;



  /**
   * Constructs statistics for all interfaces.
   *
   * @param receivedBytes  received bytes tracker
   * @param sentBytes      sent bytes tracker
   */
  public AggregateInterfaceStatistics(
      LongValueTracker receivedBytes, LongValueTracker sentBytes)
  {
    super(
        InterfaceStatistics.AGGREGATE_INTERFACE_NAME, receivedBytes, sentBytes
    );
    this.receivedTotal     = 0;
    this.sentTotal         = 0;
    this.lastReceivedValue = -1;
    this.lastSentValue     = -1;

  }


  /**
   * {@inheritDoc}
   */
  void recordSentValue(long value)
  {
    this.sentTotal += value;
  }



  /**
   * {@inheritDoc}
   */
  void recordReceivedValue(long value)
  {
    this.receivedTotal += value;
  }



  /**
   * {@inheritDoc}
   */
  void completeIteration()
  {
    if (lastReceivedValue >= 0)
    {
      final Long sentDiff     = this.sentTotal - this.lastSentValue;
      final Long receivedDiff = this.receivedTotal - this.lastReceivedValue;

      // Update trackers.
      this.rawSentBytes.add(sentDiff);
      this.rawReceivedBytes.add(receivedDiff);

      if (this.enableRealTimeStats)
      {
        this.sentBytes.addValue(sentDiff);
        this.receivedBytes.addValue((receivedDiff));
      }
    }

    this.lastSentValue = this.sentTotal;
    this.lastReceivedValue = this.receivedTotal;

    // Reset the counters for the next iteration
    this.sentTotal = 0;
    this.receivedTotal = 0;
  }
}
