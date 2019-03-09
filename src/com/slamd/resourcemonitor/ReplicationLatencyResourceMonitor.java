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



import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPInterruptedException;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPSearchConstraints;
import netscape.ldap.LDAPSearchResults;
import netscape.ldap.controls.LDAPPersistSearchControl;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class defines a SLAMD resource monitor that can be used to monitor the
 * latency associated with replication between two instances of the Sun ONE
 * Directory Server.  It operates by periodically performing a modify operation
 * on a master directory and using a persistent search on a replica to detect
 * that change.
 *
 *
 * @author   Neil A. Wilson
 */
public class ReplicationLatencyResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name of the stat tracker used to keep track of replication
   * latency.
   */
  public static final String STAT_TRACKER_REPLICATION_LATENCY =
       "Replication Latency (ms)";



  /**
   * The name of the configuration property that specifies the address of the
   * master directory server.
   */
  public static final String PROPERTY_MASTER_HOST = "master_host";



  /**
   * The name of the configuration property that specifies the port of the
   * master directory server.
   */
  public static final String PROPERTY_MASTER_PORT = "master_port";



  /**
   * The name of the configuration property that specifies the DN to use to bind
   * to the master directory server.
   */
  public static final String PROPERTY_MASTER_BIND_DN = "master_bind_dn";



  /**
   * The name of the configuration property that specifies the password to use
   * to bind to the master directory server.
   */
  public static final String PROPERTY_MASTER_BIND_PW = "master_bind_pw";



  /**
   * The name of the configuration property that specifies the address of the
   * replica directory server.
   */
  public static final String PROPERTY_REPLICA_HOST = "replica_host";



  /**
   * The name of the configuration property that specifies the port of the
   * replica directory server.
   */
  public static final String PROPERTY_REPLICA_PORT = "replica_port";



  /**
   * The name of the configuration property that specifies the DN to use to bind
   * to the replica directory server.
   */
  public static final String PROPERTY_REPLICA_BIND_DN = "replica_bind_dn";



  /**
   * The name of the configuration property that specifies the password to use
   * to bind to the replica directory server.
   */
  public static final String PROPERTY_REPLICA_BIND_PW = "replica_bind_pw";



  /**
   * The name of the configuration property that specifies the DN of the entry
   * to modify on the master and to monitor on the replica.
   */
  public static final String PROPERTY_LATENCY_CHECK_ENTRY_DN =
       "latency_check_entry_dn";



  /**
   * The name of the configuration property that specifies the name of the
   * attribute that should be modified on the master directory to trigger
   * notification on the replica.
   */
  public static final String PROPERTY_LATENCY_CHECK_MOD_ATTR = "attr_to_modify";



  /**
   * The name of the configuration property that specifies the minimum delay in
   * milliseconds between modifications to the latency check entry DN on the
   * master server.
   */
  public static final String PROPERTY_LATENCY_CHECK_DELAY =
       "latency_check_delay";



  // Information used to connect to the servers.
  private int    masterPort;
  private int    replicaPort;
  private String masterBindDN;
  private String masterBindPW;
  private String masterHost;
  private String replicaBindDN;
  private String replicaBindPW;
  private String replicaHost;


  // Information about the modifications that should be made for latency
  // checking.
  private int    latencyCheckDelay;
  private String attrToModify;
  private String latencyCheckEntryDN;


  // Variables used by the monitor thread to help ensure that this thread stops
  // in a timely manner when appropriate.
  protected boolean isStopped;
  protected boolean waitingOnPSearch;


  // The stat tracker used to measure replication latency.
  private TimeTracker latencyTimer;



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
    // Get the information needed to connect to the master server.
    masterHost = getProperty(PROPERTY_MASTER_HOST, null);
    if ((masterHost == null) || (masterHost.length() == 0))
    {
      throw new SLAMDException("ERROR:  No value provided for \"" +
                               PROPERTY_MASTER_HOST +
                               "\" configuration property");
    }

    masterPort = getProperty(PROPERTY_MASTER_PORT, -1);
    if ((masterPort <= 0) || (masterPort > 65535))
    {
      throw new SLAMDException("ERROR:  Missing or invalid value for \"" +
                               PROPERTY_MASTER_PORT +
                               "\" configuration property");
    }

    masterBindDN = getProperty(PROPERTY_MASTER_BIND_DN);
    masterBindPW = getProperty(PROPERTY_MASTER_BIND_PW);



    // Get the information needed to connect to the replica server.
    replicaHost = getProperty(PROPERTY_REPLICA_HOST, null);
    if ((replicaHost == null) || (replicaHost.length() == 0))
    {
      throw new SLAMDException("ERROR:  No value provided for \"" +
                               PROPERTY_REPLICA_HOST +
                               "\" configuration property");
    }

    replicaPort = getProperty(PROPERTY_REPLICA_PORT, -1);
    if ((replicaPort <= 0) || (replicaPort > 65535))
    {
      throw new SLAMDException("ERROR:  Missing or invalid value for \"" +
                               PROPERTY_REPLICA_PORT +
                               "\" configuration property");
    }

    replicaBindDN = getProperty(PROPERTY_REPLICA_BIND_DN);
    replicaBindPW = getProperty(PROPERTY_REPLICA_BIND_PW);


    // Get the other information needed for the latency checking process.
    latencyCheckEntryDN = getProperty(PROPERTY_LATENCY_CHECK_ENTRY_DN, null);
    if ((latencyCheckEntryDN == null) || (latencyCheckEntryDN.length() == 0))
    {
      throw new SLAMDException("ERROR:  No value provided for \"" +
                               PROPERTY_LATENCY_CHECK_ENTRY_DN +
                               "\" configuration property");
    }

    attrToModify = getProperty(PROPERTY_LATENCY_CHECK_MOD_ATTR, null);
    if ((attrToModify == null) || (attrToModify.length() == 0))
    {
      throw new SLAMDException("ERROR:  No value provided for \"" +
                               PROPERTY_LATENCY_CHECK_MOD_ATTR +
                               "\" configuration property");
    }

    latencyCheckDelay = getProperty(PROPERTY_LATENCY_CHECK_DELAY, -1);
    if (latencyCheckDelay < 0)
    {
      throw new SLAMDException("ERROR:  Missing or invalid value for \"" +
                               PROPERTY_LATENCY_CHECK_DELAY +
                               "\" configuration property");
    }

    isStopped        = true;
    waitingOnPSearch = false;
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
    ReplicationLatencyResourceMonitor monitor =
         new ReplicationLatencyResourceMonitor();
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
    String displayName = masterHost + ':' + masterPort + "->" + replicaHost +
                         ':' + replicaPort + ' ' +
                         STAT_TRACKER_REPLICATION_LATENCY;
    latencyTimer = new TimeTracker(clientID, threadID, displayName,
                                   collectionInterval);

    ResourceMonitorJob monitorJob = getMonitorJob();
    if (monitorJob.enableRealTimeStats())
    {
      String jobID = monitorJob.getJobID();
      RealTimeStatReporter statReporter = monitorJob.getStatReporter();
      latencyTimer.enableRealTimeStats(statReporter, jobID);
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
    return "LDAP Replication Latency";
  }



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  @Override()
  public StatTracker[] getResourceStatistics()
  {
    StatTracker[] trackers = new StatTracker[]
    {
      latencyTimer
    };

    return trackers;
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
    // Establish the connection to the master directory server.
    LDAPConnection masterConn = new LDAPConnection();
    try
    {
      masterConn.connect(3, masterHost, masterPort, masterBindDN, masterBindPW);
    }
    catch (Exception e)
    {
      logMessage("Unable to connect to master directory server " + masterHost +
                 ':' + masterPort + " -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }


    // Establish the connection to the replica directory server.
    LDAPConnection replicaConn = new LDAPConnection();
    try
    {
      replicaConn.connect(3, replicaHost, replicaPort, replicaBindDN,
                          replicaBindPW);
    }
    catch (Exception e)
    {
      logMessage("Unable to connect to replica directory server " +
                 replicaHost + ':' + masterPort + " -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }


    // Register the persistent search against the replica directory.
    LDAPSearchResults psearchResults;
    try
    {
      LDAPPersistSearchControl psearchControl =
           new LDAPPersistSearchControl(LDAPPersistSearchControl.MODIFY, true,
                                        false, true);
      LDAPSearchConstraints searchConstraints =
           replicaConn.getSearchConstraints();
      searchConstraints.setMaxResults(0);
      searchConstraints.setServerControls(psearchControl);
      psearchResults = replicaConn.search(latencyCheckEntryDN,
                            LDAPConnection.SCOPE_BASE,
                            "(|(objectClass=*)(objectClass=ldapSubentry))",
                            null, false, searchConstraints);
    }
    catch (Exception e)
    {
      logMessage("Unable to register a persistent search with replica " +
                 "directory server " + replicaHost + ':' + replicaPort +
                 " -- " + e);
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }


    // Create a thread that will be used to watch this resource monitor and
    // stop it as appropriate.
    isStopped        = false;
    waitingOnPSearch = false;
    ReplicationLatencyResourceMonitorThread monitorThread =
         new ReplicationLatencyResourceMonitorThread(this);
    monitorThread.start();


    // Start the latency checking stat tracker.
    latencyTimer.startTracker();


    // Loop, making a change on the master server and then detecting it on the
    // replica.
    int jobState = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
    while (! shouldStop())
    {
      // Get the current time so that we can decide how long to sleep between
      // modifications of the latency check entry.
      long latencyCheckStartTime = System.currentTimeMillis();


      // Modify the latency check entry.
      try
      {
        LDAPAttribute attr =
             new LDAPAttribute(attrToModify,
                               String.valueOf(System.currentTimeMillis()));
        masterConn.modify(latencyCheckEntryDN,
                          new LDAPModification(LDAPModification.REPLACE, attr));
      }
      catch (Exception e)
      {
        logMessage("Unable to modify entry \"" + latencyCheckEntryDN +
                   "\" on master server " + masterHost + ':' + masterPort +
                   " -- " + e);
        jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
        break;
      }


      // Start the timer, since we are only interested in how long it took to
      // replicate the change, not in how long it takes to perform the change
      // and replicate it.
      latencyTimer.startTimer();



      // Wait for the change to occur on the replica.
      waitingOnPSearch = true;
      if (psearchResults.hasMoreElements())
      {
        try
        {
          Object element = psearchResults.nextElement();

          if (element instanceof LDAPEntry)
          {
            // We detected a change to the entry, so stop the timer.
            latencyTimer.stopTimer();
          }
          else if (element instanceof LDAPInterruptedException)
          {
            // This means that the user either cancelled the job, or that the
            // monitor thread interrupted the persistent search when it was time
            // to stop for one reason or another.
            break;
          }
          else
          {
            logMessage("Unexpected object returned by the persistent " +
                       "search:  " + element);
            jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
            break;
          }
        }
        catch (Exception e)
        {
          logMessage("An error occurred while trying to detect the change on " +
                     "replica " + replicaHost + ':' + replicaPort + " -- " + e);
          jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
          break;
        }
      }
      else
      {
        logMessage("Unexpected end of persistent search results on replica " +
                   replicaHost + ':' + replicaPort);
        jobState = Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
        break;
      }
      waitingOnPSearch = false;


      // Determine if we should sleep before the next check.
      if ((latencyCheckDelay > 0) && (! shouldStop()))
      {
        long elapsedTime = System.currentTimeMillis() - latencyCheckStartTime;
        if (latencyCheckDelay > elapsedTime)
        {
          try
          {
            Thread.sleep(latencyCheckDelay - elapsedTime);
          } catch (InterruptedException ie) {}
        }
      }
    }



    // Stop the tracker, close the connections to the server, and return.
    latencyTimer.stopTracker();
    isStopped        = true;
    waitingOnPSearch = false;
    monitorThread.requestStop();

    try
    {
      masterConn.disconnect();
    } catch (Exception e) {}

    try
    {
      replicaConn.disconnect();
    } catch (Exception e) {}

    return jobState;
  }
}

