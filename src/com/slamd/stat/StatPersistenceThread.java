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
package com.slamd.stat;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.slamd.asn1.ASN1Writer;
import com.slamd.client.ClientSideJob;



/**
 * This class defines a thread that may be used to periodically save statistical
 * information on the client system while a job is in progress so that any
 * data it has collected so far will be preserved so that at least some results
 * may be provided.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatPersistenceThread
       extends Thread
{
  /**
   * The length of time in milliseconds to sleep between polls to determine if
   * it is time to write the data out.
   */
  public static final int POLL_INTERVAL = 5000;



  // The list of statistics being captured by this thread.
  private ArrayList<StatTracker> statList;

  // The job with which this persistence thread is currently associated.
  private ClientSideJob job;

  // The interval in seconds between saves of the job data.
  private int saveInterval;

  // The mutex used to provide threadsafe access to the stat list.
  private final Object statListMutex;

  // The unique ID assigned to the client with which this stat persistence
  // thread is associated.
  private String clientID;

  // The path to the directory into which the persistence data should be saved.
  private String saveDirectory;



  /**
   * Creates a new instance of this stat persistence thread with the provided
   * information.
   *
   * @param  clientID       The unique ID assigned to the client with which this
   *                        stat persistence thread is associated.  It should
   *                        be unique among multiple clients on the same system.
   * @param  saveDirectory  The path to the directory into which the persistence
   *                        data should be saved.
   * @param  saveInterval   The interval in seconds between saves of job data.
   *
   * @throws  IOException  If the save directory does not exist and cannot be
   *                       created, or if it exists but is not a directory.
   */
  public StatPersistenceThread(String clientID, String saveDirectory,
                               int saveInterval)
         throws IOException
  {
    this.clientID      = clientID;
    this.saveDirectory = saveDirectory;
    this.saveInterval  = saveInterval;

    statList      = new ArrayList<StatTracker>();
    statListMutex = new Object();
    job           = null;

    setName("Stat Persistence Thread");
    setDaemon(true);

    try
    {
      File saveDirFile = new File(saveDirectory);
      if (! saveDirFile.exists())
      {
        if (! saveDirFile.mkdirs())
        {
          throw new IOException("Save directory \"" + saveDirectory +
                                "\" does not exist and could not be created.");
        }
      }
      else
      {
        if (! saveDirFile.isDirectory())
        {
          throw new IOException("Save directory \"" + saveDirectory +
                                "\" exists but is not a directory.");
        }
      }
    }
    catch (Exception e)
    {
      throw new IOException("Unable to determine whether save directory " +
                            "exists:  " + e);
    }
  }



  /**
   * Specifies the job with which this persistence thread should be used.
   *
   * @param  job  The job with which this persistence thread should be used.
   */
  public void setJob(ClientSideJob job)
  {
    this.job = job;
    statList.clear();
  }



  /**
   * Registers the provided stat tracker with the persistence thread.
   *
   * @param  tracker  The stat tracker to register.
   */
  public void registerTracker(StatTracker tracker)
  {
    synchronized (statListMutex)
    {
      statList.add(tracker);
    }
  }



  /**
   * Indicates that the job has completed and that any remaining data should be
   * written to disk and the associated stat trackers discarded.
   *
   * @throws  IOException  If a problem occurs while writing the data to disk.
   */
  public void jobDone()
         throws IOException
  {
    synchronized (statListMutex)
    {
      String jobID = job.getJobID();
      job = null;

      if (statList.isEmpty())
      {
        return;
      }

      StatTracker[] trackers = new StatTracker[statList.size()];
      statList.toArray(trackers);
      statList.clear();

      String fileName = saveDirectory + File.separator + clientID + "." +
                        jobID;
      fileName = fileName.replace(':', '_');
      File statFile = new File(fileName + ".temp");
      OutputStream outputStream = new FileOutputStream(statFile, false);
      ASN1Writer writer = new ASN1Writer(outputStream);
      writer.writeElement(StatEncoder.trackersToSequence(trackers));
      writer.close();
      outputStream.close();

      File origFile = new File(fileName);
      if (origFile.exists() && (! origFile.delete()))
      {
        job.logMessage("Stat Persistence Thread",
                       "Unable to delete current persistent stat file " +
                       fileName);
      }

      if (! statFile.renameTo(new File(fileName)))
      {
        throw new IOException("Unable to rename temporary file \"" +
                              statFile.getAbsolutePath() +
                              "\" to desired name");
      }
    }
  }



  /**
   * Periodically writes any statistics collected for the active job to disk.
   */
  public void run()
  {
    boolean jobActive       = false;
    long    nextCaptureTime = 0;


    // Create a loop that will wait for a job to become available, and then
    // once it does will periodically write statistical data for that job to
    // disk.
    while (true)
    {
      if (job == null)
      {
        // There is no job running right now, so we don't need to do anything.
        jobActive = false;
      }
      else
      {
        if (! jobActive)
        {
          // We didn't think that there was a job running, which means that we
          // need to reset the next capture time.
          jobActive = true;
          nextCaptureTime = System.currentTimeMillis() + (1000 * saveInterval);
        }
        else
        {
          // Check to see if it's time to save the data yet.
          if (System.currentTimeMillis() >= nextCaptureTime)
          {
            synchronized (statListMutex)
            {
              if (! statList.isEmpty())
              {
                StatTracker[] trackers = new StatTracker[statList.size()];
                statList.toArray(trackers);

                try
                {
                  String fileName = saveDirectory + File.separator +
                                    clientID + "." + job.getJobID();
                  fileName = fileName.replace(':', '_');
                  File statFile = new File(fileName + ".temp");
                  OutputStream outputStream = new FileOutputStream(statFile,
                                                                   false);
                  ASN1Writer writer = new ASN1Writer(outputStream);
                  writer.writeElement(StatEncoder.trackersToSequence(trackers));
                  writer.close();
                  outputStream.close();

                  File origFile = new File(fileName);
                  if (origFile.exists() && (! origFile.delete()))
                  {
                    job.logMessage("Stat Persistence Thread",
                                   "Unable to delete current persistent " +
                                   "stat file " + fileName);
                  }

                  if (! statFile.renameTo(new File(fileName)))
                  {
                    job.logMessage("Stat Persistence Thread",
                                   "Unable to rename temporary file \"" +
                                   statFile.getAbsolutePath() +
                                   "\" to desired name");
                  }
                }
                catch (Exception e)
                {
                  job.logMessage("Stat Persistence Thread",
                                 "Unable to write statistical data to a " +
                                 "temporary file -- " + e);
                }
              }
            }

            nextCaptureTime = System.currentTimeMillis() +
                              (1000 * saveInterval);
          }
        }
      }


      // At this point, we just a little bit and check again.
      try
      {
        Thread.sleep(POLL_INTERVAL);
      } catch (Exception e) {}
    }
  }
}

