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



import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class defines a SLAMD resource monitor that uses UDP packets to obtain
 * basic characteristics of the network connection between two systems,
 * including response time and packet loss.  The function that it performs is
 * very similar to the ping utility, but the transport is UDP rather than ICMP.
 * Communication can be with either the standard echo service (UDP port 7) if
 * available, or using a custom UDP ping server listening on any port.
 * <BR><BR>
 * Note that this monitor can achieve at best millisecond accuracy because there
 * is no pure Java way to obtain more accurate timings in Java 1.4.x.  Java 1.5
 * introduces a new <CODE>System.nanoTime()</CODE> method that can offer up to
 * nanosecond accuracy, but this monitor does not take advantage of that.  As
 * such, this monitor is best suited for WAN connections with multi-millisecond
 * response times between systems.
 *
 *
 * @author   Neil A. Wilson
 */
public class UDPPingResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker that will be used to report the
   * total number of requests sent.
   */
  public static final String STAT_TRACKER_REQUESTS_SENT = "Requests Sent";



  /**
   * The display name of the stat tracker that will be used to report the
   * response time in milliseconds.
   */
  public static final String STAT_TRACKER_RESPONSE_TIME = "Response Time (ms)";



  /**
   * The display name of the stat tracker that will be used to measure the
   * number of packets that were lost (i.e., cases in which no response was
   * obtained before a timeout occurred).
   */
  public static final String STAT_TRACKER_PACKETS_LOST = "Packets Lost";



  /**
   * The display name of the stat tracker that will be used to measure the
   * number of duplicate packets received.
   */
  public static final String STAT_TRACKER_DUPLICATE_PACKETS =
       "Duplicate Packets Received";



  /**
   * The name of the configuration property that specifies the address of the
   * system to ping.
   */
  public static final String PROPERTY_TARGET_ADDRESS = "target_address";



  /**
   * The default target address that will be used if none is specified.
   */
  public static final String DEFAULT_TARGET_ADDRESS = null;



  /**
   * The name of the configuration property that specifies the UDP port to which
   * the "ping" requests should be sent.
   */
  public static final String PROPERTY_TARGET_PORT = "target_port";



  /**
   * The default target port that will be used if none is specified.
   */
  public static final int DEFAULT_TARGET_PORT = 7;



  /**
   * The name of the configuration property that specifies the interval in
   * milliseconds between pings.
   */
  public static final String PROPERTY_PING_INTERVAL = "ping_interval";



  /**
   * The default ping interval that will be used if none is specified.
   */
  public static final int DEFAULT_PING_INTERVAL = 1000;



  /**
   * The name of the configuration property that specifies the maximum length of
   * time in milliseconds that the client will wait for a response before
   * considering a packet "lost".
   */
  public static final String PROPERTY_PING_TIMEOUT = "ping_timeout";



  /**
   * The default ping timeout that will be used if none is specified.
   */
  public static final int DEFAULT_PING_TIMEOUT = 1000;



  /**
   * The name of the configuration property that indicates the size in bytes of
   * the UDP packet to send to the remote system.
   */
  public static final String PROPERTY_PACKET_SIZE = "packet_size";



  /**
   * The default packet size that will be used if none is specified.
   */
  public static final int DEFAULT_PACKET_SIZE = 64;



  /**
   * The minimum packet size that can be used for the pings.  It is necessary to
   * have at least this many bytes to hold the sequence number and packet
   * creation timestamp.
   */
  public static final int MINIMUM_PACKET_SIZE = 12;



  // Stat trackers that will be used by this resource monitor thread.
  private IncrementalTracker packetsLost;
  private IncrementalTracker duplicatePackets;
  private TimeTracker        responseTimer;

  // The packet size to use for the UDP packets that will be sent.
  private int packetSize;

  // The interval to use when pinging the remote system.
  private int pingInterval;

  // The timeout to use when detecting lost packets.
  private int pingTimeout;

  // The UDP port to which the request packets will be sent.
  private int targetPort;

  // The address of the local system on which the client is running.
  private String localAddress;

  // The user-specified address of the target system to ping.
  private String targetAddress;

  // The IP address of the target system to ping.
  private String targetIP;



  /**
   * Performs any initialization specific to this resource monitor.
   *
   * @throws  SLAMDException  If a problem occurs while performing the
   *                          initialization.
   */
  @Override()
  public void initializeMonitor()
         throws SLAMDException
  {
    targetAddress = getProperty(PROPERTY_TARGET_ADDRESS,
                                DEFAULT_TARGET_ADDRESS);
    targetPort    = getProperty(PROPERTY_TARGET_PORT, DEFAULT_TARGET_PORT);
    pingInterval  = getProperty(PROPERTY_PING_INTERVAL, DEFAULT_PING_INTERVAL);
    pingTimeout   = getProperty(PROPERTY_PING_TIMEOUT, DEFAULT_PING_TIMEOUT);
    packetSize    = getProperty(PROPERTY_PACKET_SIZE, DEFAULT_PACKET_SIZE);


    // Make sure that a target address was specified.
    if (targetAddress == null)
    {
      throw new SLAMDException("No target address specified for the system " +
                               "to ping.");
    }


    // Make sure that the packet size is not smaller than the minimum allowed.
    if (packetSize < MINIMUM_PACKET_SIZE)
    {
      logMessage("UDP ping packet size may not be smaller than " +
                 MINIMUM_PACKET_SIZE + " -- using minimum allowable value.");
      packetSize = MINIMUM_PACKET_SIZE;
    }


    // Resolve the target address to an IP address so we don't have to worry
    // about name resolution later.
    try
    {
      targetIP = InetAddress.getByName(targetAddress).getHostAddress();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to resolve target address \"" +
                               targetAddress + " to an IP address -- " + e, e);
    }


    try
    {
      localAddress = InetAddress.getLocalHost().getHostName();
    }
    catch (Exception e)
    {
      try
      {
        localAddress = InetAddress.getLocalHost().getHostAddress();
      }
      catch (Exception e2)
      {
        throw new SLAMDException("Unable to obtain local address -- " + e2, e2);
      }
    }
  }



  /**
   * Indicates whether the current client system is supported for this resource
   * monitor.
   *
   * @return  <CODE>true</CODE> if the current client system is supported for
   *          this resource monitor, or <CODE>false</CODE> if not.
   */
  @Override()
  public boolean clientSupported()
  {
    return true;
  }



  /**
   * Creates a new instance of this resource monitor thread.  Note that the
   * <CODE>initialize()</CODE> method should have been called on the new
   * instance before it is returned.
   *
   * @return  A new instance of this resource monitor thread.
   *
   * @throws  SLAMDException  If a problem occurs while creating or initializing
   *                          the resource monitor.
   */
  @Override()
  public ResourceMonitor newInstance()
         throws SLAMDException
  {
    UDPPingResourceMonitor monitor = new UDPPingResourceMonitor();
    monitor.initialize(getMonitorClient(), getMonitorProperties());

    return monitor;
  }




  /**
   * Initializes the stat trackers maintained by this resource monitor.
   *
   * @param  clientID            The client ID to use for the stubs.
   * @param  threadID            The thread ID to use for the stubs.
   * @param  collectionInterval  The collection interval to use for the stubs.
   */
  @Override()
  public void initializeStatistics(String clientID, String threadID,
                                   int collectionInterval)
  {
    String displayNamePrefix = localAddress + " -> " + targetAddress +
                               " UDP Ping ";

    responseTimer = new TimeTracker(clientID, threadID,
                             displayNamePrefix + STAT_TRACKER_RESPONSE_TIME,
                             collectionInterval);
    packetsLost = new IncrementalTracker(clientID, threadID,
                           displayNamePrefix + STAT_TRACKER_PACKETS_LOST,
                           collectionInterval);
    duplicatePackets = new IncrementalTracker(clientID, threadID,
                                displayNamePrefix +
                                STAT_TRACKER_DUPLICATE_PACKETS,
                                collectionInterval);

    ResourceMonitorJob monitorJob = getMonitorJob();
    if (monitorJob.enableRealTimeStats())
    {
      String jobID = monitorJob.getJobID();
      RealTimeStatReporter statReporter = monitorJob.getStatReporter();

      responseTimer.enableRealTimeStats(statReporter, jobID);
      packetsLost.enableRealTimeStats(statReporter, jobID);
      duplicatePackets.enableRealTimeStats(statReporter, jobID);
    }
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "UDP Ping";
  }



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  @Override()
  public StatTracker[] getResourceStatistics()
  {
    return new StatTracker[]
    {
      responseTimer,
      packetsLost,
      duplicatePackets
    };
  }



  /**
   * Performs the work of actually collecting resource statistics.  This method
   * should periodically call the <CODE>shouldStop()</CODE> method to determine
   * whether to stop collecting statistics.
   *
   * @return  A value that indicates the status of the monitor when it
   *          completed.
   */
  @Override()
  public int runMonitor()
  {
    // First, establish a "connection" to the remote system.
    DatagramSocket pingSocket;
    try
    {
      pingSocket = new DatagramSocket();
      pingSocket.setSoTimeout(pingTimeout);
      pingSocket.connect(new InetSocketAddress(targetIP, targetPort));
    }
    catch (Exception e)
    {
      logMessage("UDP ping unable to establish a connection to " + targetIP +
                 ':' + targetPort + " -- " + JobClass.stackTraceToString(e));
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }


    // Create the packets that we will use to send and receive the pings.
    byte[] rxBytes = new byte[packetSize];
    byte[] txBytes = new byte[packetSize];
    DatagramPacket rxPacket = new DatagramPacket(rxBytes, packetSize);
    DatagramPacket txPacket = new DatagramPacket(txBytes, packetSize);


    // Start the stat trackers.
    responseTimer.startTracker();
    packetsLost.startTracker();
    duplicatePackets.startTracker();


    // Operate in a loop that will run until it needs to stop.
    int sequenceNumber = 0;
    int returnCode     = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
    while (! shouldStop())
    {
      // Get the current time, since we will need it to construct the request.
      // The request will contain at least 12 bytes (four for the sequence
      // number and eight for the start time).
      long startTime = System.currentTimeMillis();
      txBytes[0] = (byte) ((sequenceNumber >>> 24) & 0x000000FF);
      txBytes[1] = (byte) ((sequenceNumber >>> 16) & 0x000000FF);
      txBytes[2] = (byte) ((sequenceNumber >>>  8) & 0x000000FF);
      txBytes[3] = (byte) (sequenceNumber & 0x000000FF);

      txBytes[4]  = (byte) ((startTime >>> 56) & 0x000000FF);
      txBytes[5]  = (byte) ((startTime >>> 48) & 0x000000FF);
      txBytes[6]  = (byte) ((startTime >>> 40) & 0x000000FF);
      txBytes[7]  = (byte) ((startTime >>> 32) & 0x000000FF);
      txBytes[8]  = (byte) ((startTime >>> 24) & 0x000000FF);
      txBytes[9]  = (byte) ((startTime >>> 16) & 0x000000FF);
      txBytes[10] = (byte) ((startTime >>>  8) & 0x000000FF);
      txBytes[11] = (byte) (startTime & 0x000000FF);


      // Actually send the packet to the remote system.
      txPacket.setData(txBytes);
      responseTimer.startTimer();
      try
      {
        pingSocket.send(txPacket);
      }
      catch (Exception e)
      {
        logMessage("UDP ping unable to send packet with sequence number " +
                   sequenceNumber + " -- " + JobClass.stackTraceToString(e));
        returnCode = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
        break;
      }


      // Wait for the response packet.
      try
      {
        pingSocket.receive(rxPacket);
        rxBytes = rxPacket.getData();
        int sequence = (((rxBytes[0] & 0x000000FF) << 24) |
                        ((rxBytes[1] & 0x000000FF) << 16) |
                        ((rxBytes[2] & 0x000000FF) << 8)  |
                        (rxBytes[3] & 0x000000FF));
        if (sequence == sequenceNumber)
        {
          responseTimer.stopTimer();
        }
        else if (sequence < sequenceNumber)
        {
          duplicatePackets.increment();
        }
      }
      catch (SocketTimeoutException ste)
      {
        packetsLost.increment();
      }
      catch (Exception e)
      {
        logMessage("UDP ping caught an unexpected exception while waiting " +
                   "for response with sequence number " + sequenceNumber +
                   " -- " + JobClass.stackTraceToString(e));
        returnCode = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
        break;
      }


      // Increment the sequence number and see if we need to sleep before the
      // next request.
      sequenceNumber++;
      long sleepTime = (startTime + pingInterval) - System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        } catch (Exception e) {}
      }
    }


    // Close the "connection" to the remote system.
    try
    {
      pingSocket.close();
    } catch (Exception e) {}


    // Stop the stat trackers.
    responseTimer.stopTracker();
    packetsLost.stopTracker();
    duplicatePackets.stopTracker();


    return returnCode;
  }
}

