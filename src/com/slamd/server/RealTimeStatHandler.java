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
package com.slamd.server;



import java.util.HashMap;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.db.SLAMDDB;
import com.slamd.job.JobClass;
import com.slamd.message.RegisterStatisticMessage;
import com.slamd.message.ReportStatisticMessage;



/**
 * This class implements a mechanism for handling statistical data reported to
 * the SLAMD server in real time.  It aggregates the data reported by all the
 * clients and can make it available for display to end users on request.
 *
 *
 * @author   Neil A. Wilson
 */
public class RealTimeStatHandler
{
  // The maximum number of collection intervals that should be maintained.
  protected int maxIntervals;

  // The hash map that associates statistical data with the corresponding job.
  private HashMap<String,RealTimeJobStats> statHash;

  // The mutex used to provide threadsafe access to the stat hash.
  private final Object statHashMutex;

  // The configuration database that stores the configuration for this stat
  // handler.
  private SLAMDDB configDB;


  // The SLAMD server with which this stat handler is associated.
  private SLAMDServer slamdServer;



  /**
   * Creates a new instance of this real-time stat handler.
   *
   * @param  slamdServer  The SLAMD server with which this stat handler is
   *                      associated.
   */
  public RealTimeStatHandler(SLAMDServer slamdServer)
  {
    this.slamdServer = slamdServer;

    statHash      = new HashMap<String,RealTimeJobStats>();
    statHashMutex = new Object();
    maxIntervals  = Constants.DEFAULT_MAX_STAT_INTERVALS;

    configDB = slamdServer.getConfigDB();

    String intervalsStr =
         configDB.getConfigParameter(Constants.PARAM_MAX_STAT_INTERVALS);
    if ((intervalsStr != null) && (intervalsStr.length() > 0))
    {
      try
      {
        maxIntervals = Integer.parseInt(intervalsStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                               "Invalid value for config parameter " +
                               Constants.PARAM_MAX_STAT_INTERVALS + ":  " +
                               intervalsStr + " -- " + e);
      }
    }
  }



  /**
   * Retrieves the reference to the SLAMD server with which this stat handler
   * is associated.
   *
   * @return  The reference to the SLAMD server with which this stat handler is
   *          associated.
   */
  public SLAMDServer getSLAMDServer()
  {
    return slamdServer;
  }



  /**
   * Handles the work of registering a client with the SLAMD server for the
   * purpose of real-time statistics reporting.
   *
   * @param  message  The message containing the information about the client
   *                  being registered.
   */
  public void handleRegisterStatMessage(RegisterStatisticMessage message)
  {
    String jobID    = message.getJobID();
    String statName = message.getDisplayName();

    synchronized (statHashMutex)
    {
      RealTimeJobStats jobStats = statHash.get(jobID);
      if (jobStats == null)
      {
        try
        {
          jobStats = new RealTimeJobStats(this, jobID, maxIntervals);
          statHash.put(jobID, jobStats);
        }
        catch (SLAMDServerException sse)
        {
          // What to do here?
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(sse));
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                                 "Stat handler asked to register statistics " +
                                 "for unknown job " + jobID);
          return;
        }
      }

      jobStats.registerStatistic(statName);
    }
  }



  /**
   * Handles the work of processing the statistical data contained in the
   * provided message and making it available to the server.
   *
   * @param  message  The message containing the statistical data to be
   *                  reported.
   */
  public void handleReportStatMessage(ReportStatisticMessage message)
  {
    String jobID = message.getJobID();
    ASN1Sequence[] dataSequences = message.getDataSequences();

    synchronized (statHashMutex)
    {
      RealTimeJobStats jobStats = statHash.get(jobID);
      if (jobStats == null)
      {
        // What to do here?
        slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                               "Stat handler asked to report statistics " +
                               "for unregistered job " + jobID);
        return;
      }

      for (int i=0; i < dataSequences.length; i++)
      {
        try
        {
          ASN1Element[] dataElements = dataSequences[i].getElements();
// These are currently unused.
//          String clientID =
//               dataElements[0].decodeAsOctetString().getStringValue();
//          String threadID =
//               dataElements[1].decodeAsOctetString().getStringValue();
          String statName =
               dataElements[2].decodeAsOctetString().getStringValue();


          // Oops.  There is a bug in the way that stat done messages are
          // encoded, in that they do not include an interval number.  This
          // screws up parsing, so it is necessary to detect that (because the
          // interval number and stat type have different ASN.1 types) and work
          // around the problem.
          int intervalNum = -1;
          int statType;
          if (dataElements[3].getType() == ASN1Element.ASN1_ENUMERATED_TYPE)
          {
            statType = dataElements[3].decodeAsEnumerated().getIntValue();
          }
          else
          {
            intervalNum = dataElements[3].decodeAsInteger().getIntValue();
            statType    = dataElements[4].decodeAsEnumerated().getIntValue();
          }

          switch (statType)
          {
            case Constants.STAT_REPORT_TYPE_ADD:
              double statValue =
                   Double.parseDouble(dataElements[5].decodeAsOctetString().
                                      getStringValue());
              jobStats.updateStatToAdd(statName, intervalNum, statValue);
              break;
            case Constants.STAT_REPORT_TYPE_AVERAGE:
              statValue =
                   Double.parseDouble(dataElements[5].decodeAsOctetString().
                                      getStringValue());
              jobStats.updateStatToAverage(statName, intervalNum, statValue);
              break;
            case Constants.STAT_REPORT_TYPE_DONE:
              jobStats.deregisterStatistic();
              break;
          }

        }
        catch (Exception e)
        {
          // What to do here?
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                                 "Stat handler encountered error while " +
                                 "processing data:  " + e);
        }
      }

      jobStats.setLastUpdateTime();
    }
  }



  /**
   * Removes the real-time stat data for the specified job from this stat
   * handler.  Note that this method does not do any locking and assumes that
   * the stat hash lock is already held by the thread calling this method.
   *
   * @param  jobID  The job ID of the job for which to remove the data from this
   *                stat handler.
   */
  public void removeJobStatsUnlocked(String jobID)
  {
    statHash.remove(jobID);
  }
}

