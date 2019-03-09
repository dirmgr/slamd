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



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;



/**
 * This class defines a thread that will be used to actually replay data against
 * a target server.  It works independently of any other capture thread that
 * might be running, and will reconnect as often as necessary to deal with cases
 * in which the server has closed the connection (potentially in response to a
 * request from the client to do so).
 *
 *
 * @author   Neil A. Wilson
 */
public class ReplayThread
       extends Thread
{
  // The number of times the connection was unexpectedly closed during
  // processing.
  protected int disconnects;

  // The number of iterations through the entire data set that were completed.
  protected int iterationsCompleted;

  // The number of individual capture packets that were replayed.
  protected int packetsReplayed;

  // The replay utility with which this thread is associated.
  private ReplayCapture replayCapture;



  /**
   * Creates a new thread that may be used to replay captured traffic against a
   * target server.
   *
   * @param  replayCapture  The capture utility with which this thread is
   *                        associated.
   */
  public ReplayThread(ReplayCapture replayCapture)
  {
    this.replayCapture = replayCapture;

    disconnects         = 0;
    iterationsCompleted = 0;
    packetsReplayed     = 0;
  }



  /**
   * Loops, replaying traffic until it is time to stop for some reason.
   */
  @Override()
  public void run()
  {
    boolean           connected     = false;
    boolean           infinite      = (replayCapture.numIterations <= 0);
    SocketChannel     socketChannel = null;
    InetSocketAddress socketAddress =
         new InetSocketAddress(replayCapture.targetHost,
                               replayCapture.targetPort);

    try
    {
outerLoop:
      for (int i=0; (infinite || (i < replayCapture.numIterations)); i++)
      {
        if (replayCapture.stopRequested)
        {
          break;
        }

        for (int j=0; j < replayCapture.captureData.length; j++)
        {
          if (replayCapture.stopRequested)
          {
            break outerLoop;
          }

          if (! connected)
          {
            try
            {
              socketChannel = SocketChannel.open(socketAddress);
              socketChannel.configureBlocking(false);
              socketChannel.finishConnect();
              replayCapture.registerSocketChannel(socketChannel);
              connected = true;
            }
            catch (IOException ioe)
            {
              System.err.println("ERROR:  Unable to connect to " +
                                 socketAddress);
              break outerLoop;
            }
          }

          if (replayCapture.sleepTimes[j] > 0)
          {
            try
            {
              Thread.sleep(replayCapture.sleepTimes[j]);
            } catch (InterruptedException ie) {}
          }

          try
          {
            replayCapture.captureData[j].replayTo(socketChannel);
            packetsReplayed++;
          }
          catch (IOException ioe)
          {
            try
            {
              socketChannel.close();
            } catch (Exception e) {}

            connected = false;
            disconnects++;
          }
        }

        if (! replayCapture.stopRequested)
        {
          iterationsCompleted++;
        }
      }
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  Uncaught exception while processing");
      e.printStackTrace();
    }

    try
    {
      socketChannel.close();
    } catch (Exception e) {}

    replayCapture.threadDone(this);
  }
}

