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
import java.text.DecimalFormat;



/**
 * This program can be used to replay captured communication to a target server
 * to generate load.
 *
 *
 * @author   Neil A. Wilson
 */
public class TCPReplay
{
  /**
   * Parses the command-line arguments, creates the TCP capture daemon, and
   * starts capturing data.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // Specify default values for all the command-line arguments.
    boolean preserveTiming         = false;
    float   timingMultiplier       = (float) 1.0;
    int     delayBetweenIterations = 0;
    int     delayBetweenPackets    = 0;
    int     maxDuration            = -1;
    int     maxIterations          = 1;
    int     numThreads             = 1;
    int     targetPort             = -1;
    String  captureFile            = null;
    String  targetHost             = null;


    // Process the command-line arguments.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        targetHost = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        targetPort = Integer.parseInt(args[++i]);
        if ((targetPort < 1) || (targetPort > 65535))
        {
          System.err.println("ERROR:  The target port must be between 1 and " +
                             "65535");
          System.exit(1);
        }
      }
      else if (args[i].equals("-i"))
      {
        captureFile = args[++i];
      }
      else if (args[i].equals("-P"))
      {
        preserveTiming = true;
      }
      else if (args[i].equals("-m"))
      {
        timingMultiplier = Float.parseFloat(args[++i]);
      }
      else if (args[i].equals("-d"))
      {
        delayBetweenPackets = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-I"))
      {
        maxIterations = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-D"))
      {
        delayBetweenIterations = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-T"))
      {
        maxDuration = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-t"))
      {
        numThreads = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("ERROR:  Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }



    // Verify that all the required parameters were provided.
    if (targetHost == null)
    {
      System.err.println("ERROR:  No target server host provided (use -h)");
      displayUsage();
      System.exit(1);
    }

    if (targetPort < 0)
    {
      System.err.println("ERROR:  No target server port provided (use -p)");
      displayUsage();
      System.exit(1);
    }

    if (captureFile == null)
    {
      System.err.println("ERROR:  No capture file provided (use -i)");
      displayUsage();
      System.exit(1);
    }


    // Create the replay utility.
    ReplayCapture replayCapture = null;
    try
    {
      replayCapture = new ReplayCapture(targetHost, targetPort, captureFile,
                                        preserveTiming, timingMultiplier,
                                        delayBetweenPackets, maxIterations,
                                        delayBetweenIterations, maxDuration,
                                        numThreads);
      System.out.println("Read " + replayCapture.getNumPackets() +
                         " packets from capture file \"" + captureFile + "\".");
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Unable to read capture file \"" +
                         captureFile + "\":  " + ioe);
    }
    catch (CaptureException ce)
    {
      System.err.println("ERROR:  Unable to parse capture file \"" +
                         captureFile + "\":  " + ce);
    }


    // Start replaying the data, and wait for all threads to finish before
    // continuing.
    long startTime = System.currentTimeMillis();
    replayCapture.replayData();
    replayCapture.waitForReplayThreads();
    long stopTime = System.currentTimeMillis();


    // Print out some basic stats about the replay.
    long   totalDisconnects = replayCapture.getTotalDisconnects();
    long   totalIterations  = replayCapture.getTotalIterationsCompleted();
    long   totalReplayed    = replayCapture.getTotalPacketsReplayed();
    long   totalDuration    = stopTime - startTime;
    double packetsPerSecond = 1000.0 * totalReplayed / totalDuration;

    System.out.println("Processing complete.");
    System.out.println("Total Active Duration (ms):     " + totalDuration);
    System.out.println("Total Iterations Completed:     " + totalIterations);
    System.out.println("Total Packets Replayed:         " + totalReplayed);
    System.out.println("Total Disconnects Encountered:  " + totalDisconnects);
    System.out.println("Packets Replayed per Second:    " +
                       new DecimalFormat("0.000").format(packetsPerSecond));
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String EOL = System.getProperty("line.separator");
    System.out.println(
"USAGE:  java TCPReplay {options}" + EOL +
"        where {options} include" + EOL +
"-h {address}  -- Specifies the address of the server to which the data" + EOL +
"                 should be replayed" + EOL +
"-p {port}     -- Specifies the port of the server to which the data" + EOL +
"                 should be replayed" + EOL +
"-i {file}     -- Specifies the input file containing the captured data" + EOL +
"                 to be replayed" + EOL +
"-P            -- Indicates that an attempt should be made to preserve" + EOL +
"                 original timing between requests" + EOL +
"-m {value}    -- Specifies a multipler that may applied to the" + EOL +
"                 original time between requests" + EOL +
"-d {value}    -- Specifies the delay in milliseconds to insert between" + EOL +
"                 each packet replayed if the original timing is not to" + EOL +
"                 be preserved" + EOL +
"-I {value}    -- Specifies the maximum number of iterations each" + EOL +
"                 thread should make through the entire data set" + EOL +
"-D {value}    -- Specifies the delay in milliseconds to insert between" + EOL +
"                 individual iterations through the entire data set" + EOL +
"-T {value}    -- Specifies the maximum length of time in seconds that" + EOL +
"                 the replay should be allowed to process" + EOL +
"-t {value}    -- Specifies the number of threads that should replay" + EOL +
"                 the data set concurrently" + EOL +
"-H            -- Displays this usage information"
                      );
  }
}

