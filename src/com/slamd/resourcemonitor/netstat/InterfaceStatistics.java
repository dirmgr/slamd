package com.slamd.resourcemonitor.netstat;

import com.slamd.stat.LongValueTracker;

import java.util.List;
import java.util.ArrayList;



/**
 * Abstract class that encapsulates most of the common data processing for
 * network interface statistics.
 */
public abstract class InterfaceStatistics
{
  // Name used to refer to an "aggragete" interface that captures statistics
  // for multiple network interfaces.
  public static final String AGGREGATE_INTERFACE_NAME = "aggregate";

  // Network interface name.
  private final String name;

  // Tracker for number of bytes received by the interface.
  protected final LongValueTracker receivedBytes;

  // Tracker for number of bytes sent by the interface.
  protected final LongValueTracker sentBytes;

  // Collected received bytes values.
  protected final List<Long> rawReceivedBytes = new ArrayList<Long>();

  // Collected sent bytes values.
  protected final List<Long> rawSentBytes = new ArrayList<Long>();

  // A flag that indicates whether we will try to report real-time statistics.
  protected boolean enableRealTimeStats = false;


  /**
   * Constructs statistics for an interface.
   *
   * @param name          interface name
   * @param receivedBytes received bytes tracker
   * @param sentBytes     sent bytes tracker
   */
  public InterfaceStatistics(
      String name, LongValueTracker receivedBytes, LongValueTracker sentBytes)
  {
    this.name = name;
    this.receivedBytes = receivedBytes;
    this.sentBytes = sentBytes;
  }



  /**
   * Returns the name of the interface.
   *
   * @return the name of the interface.
   */
  final String getName()
  {
    return name;
  }



  /**
   * Returns the received bytes tracker.
   *
   * @return the received bytes tracker.
   */
  final public LongValueTracker getReceivedBytes()
  {
    return receivedBytes;
  }


  /**
   * Returns the sent bytes tracker.
   *
   * @return the sent bytes tracker.
   */
  final public LongValueTracker getSentBytes()
  {
    return sentBytes;
  }



  /**
   * Enables real time statistics collection.
   */
  final public void enableRealTimeStats()
  {
    this.enableRealTimeStats = true;
  }



  /**
   * Completes the processing of network interface statistics. Called at the
   * end of the monitoring cycle.
   */
  final void completeCollection()
  {
    final long[] rxArray    = new long[this.rawReceivedBytes.size()];
    final long[] txArray    = new long[this.rawSentBytes.size()];
    final int[] countArray  = new int[this.rawReceivedBytes.size()];

    for (int i=0; i < rxArray.length; i++)
    {
      long bytesReceived = this.rawReceivedBytes.get(i);
      if (bytesReceived < 0)
      {
        // We got a negative value, which could be the result of an internal
        // counter rolling over.  Just take the previous value if possible, or
        // zero if there is no previous value.
        if (i > 0)
        {
          bytesReceived = rxArray[i-1];
        }
        else
        {
          bytesReceived = 0;
        }
      }
      rxArray[i] = bytesReceived;

      long bytesTransmitted = this.rawSentBytes.get(i);
      if (bytesTransmitted < 0)
      {
        // We got a negative value, which could be the result of an internal
        // counter rolling over.  Just take the previous value if possible, or
        // zero if there is no previous value.
        if (i > 0)
        {
          bytesTransmitted = txArray[i-1];
        }
        else
        {
          bytesTransmitted = 0;
        }
      }
      txArray[i] = bytesTransmitted;

      countArray[i] = 1;
    }

    this.receivedBytes.setIntervalData(rxArray, countArray);
    this.sentBytes.setIntervalData(txArray, countArray);

  }



  /**
   * Records the number of sent bytes to the interface statistics. The
   * statistics may not be applied until {@link #completeIteration()} is
   * called.
   *
   * @param value   number of bytes sent
   */
  abstract void recordSentValue(long value);



  /**
   * Records the number of received bytes to the interface statistics. The
   * statistics may not be applied until {@link #completeIteration()} is
   * called.
   *
   * @param value   number of bytes received
   */
  abstract void recordReceivedValue(long value);



  /**
   * Completes any processing necessary to complete an iteration. An iteration
   * ends after both {@link #recordSentValue(long)} and
   * {@link #recordReceivedValue(long)} have been invoked.
   */
  abstract void completeIteration();
}
