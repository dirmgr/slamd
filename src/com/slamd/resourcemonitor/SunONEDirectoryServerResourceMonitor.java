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



import java.util.ArrayList;
import java.util.StringTokenizer;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD resource monitor that has the ability to
 * communicate with an instance of the Sun ONE Directory Server and periodically
 * capture monitor information from it.  In particular, it will retrieve the
 * "cn=monitor" entry and parse its contents, and it may also parse the
 * "cn=monitor,cn={dbname},cn=ldbm database,cn=plugins,cn=config" entries to
 * retrieve database-specific information.
 *
 *
 * @author   Neil A. Wilson
 */
public class SunONEDirectoryServerResourceMonitor
       extends ResourceMonitor
{
  /**
   * The display name for the stat tracker that monitors the number of
   * connections currently established to the directory server.
   */
  public static final String STAT_TRACKER_CURRENT_CONNECTIONS =
       "Current Connections";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the number of connections currently established to the
   * directory server.
   */
  public static final String PROPERTY_CAPTURE_CURRENT_CONNECTIONS =
       "capture_current_connections";



  /**
   * The flag that indicates whether the number of current connections will be
   * captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_CURRENT_CONNECTIONS = true;



  /**
   * The display name for the stat tracker that monitors the number of requests
   * requests that have been received but not yet completed.
   */
  public static final String STAT_TRACKER_REQUESTS_IN_PROGRESS =
      "Requests In Progress";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the number of requests in progress.
   */
  public static final String PROPERTY_CAPTURE_REQUESTS_IN_PROGRESS =
       "capture_requests_in_progress";



  /**
   * The flag that indicates whether the number of requests in progress will be
   * captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_REQUESTS_IN_PROGRESS = true;



  /**
   * The display name for the stat tracker that monitors the total number of
   * requests that have been fully processed by the server.
   */
  public static final String STAT_TRACKER_REQUESTS_COMPLETED =
       "Requests Completed";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the number of requests that have been completed.
   */
  public static final String PROPERTY_CAPTURE_REQUESTS_COMPLETED =
       "capture_requests_completed";



  /**
   * The flag that indicates whether the number of completed requests will be
   * captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_REQUESTS_COMPLETED = true;



  /**
   * The display name for the stat tracker that monitors the number of
   * records in the backend database.
   */
  public static final String STAT_TRACKER_DB_ENTRY_COUNT =
      "Total Database Records";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the number of records in the backend database.
   */
  public static final String PROPERTY_CAPTURE_DB_ENTRY_COUNT =
       "capture_db_entry_count";



  /**
   * The flag that indicates whether the number of records in the database will
   * be captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_DB_ENTRY_COUNT = true;



  /**
   * The display name for the stat tracker that monitors the number of
   * entries in the backend database.
   */
  public static final String STAT_TRACKER_LDAP_ENTRY_COUNT =
      "Total LDAP Entries";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the number of entries in the backend database.
   */
  public static final String PROPERTY_CAPTURE_LDAP_ENTRY_COUNT =
       "capture_ldap_entry_count";



  /**
   * The flag that indicates whether the number of entries in the database will
   * be captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_LDAP_ENTRY_COUNT = true;



  /**
   * The display name for the stat tracker that monitors the current entry cache
   * size.
   */
  public static final String STAT_TRACKER_ENTRY_CACHE_SIZE =
      "Entry Cache Size (MB)";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the size of the entry cache.
   */
  public static final String PROPERTY_CAPTURE_ENTRY_CACHE_SIZE =
       "capture_entry_cache_size";



  /**
   * The flag that indicates whether the entry cache size will be captured by
   * default.
   */
  public static final boolean DEFAULT_CAPTURE_ENTRY_CACHE_SIZE = true;



  /**
   * The display name for the stat tracker that monitors the number of entries
   * currently in the entry cache.
   */
  public static final String STAT_TRACKER_ENTRY_CACHE_COUNT =
      "Entry Cache Count";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the number of entries in the entry cache.
   */
  public static final String PROPERTY_CAPTURE_ENTRY_CACHE_COUNT =
       "capture_entry_cache_count";



  /**
   * The flag that indicates whether the number of entries in the entry cache
   * will be captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_ENTRY_CACHE_COUNT = true;



  /**
   * The display name for the stat tracker that monitors the percent full for
   * the entry cache.
   */
  public static final String STAT_TRACKER_ENTRY_CACHE_PCT_FULL =
      "Entry Cache Percent Full";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the entry cache percent full.
   */
  public static final String PROPERTY_CAPTURE_ENTRY_CACHE_PCT_FULL =
       "capture_entry_cache_percent_full";



  /**
   * The flag that indicates whether the entry cache percent full will be
   * captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_ENTRY_CACHE_PCT_FULL = true;



  /**
   * The display name for the stat tracker that monitors the percent of the
   * total entries that are cached.
   */
  public static final String STAT_TRACKER_ENTRY_CACHE_PCT_CACHED =
      "Percent of Entries Cached";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the entry cache percent of entries cached.
   */
  public static final String PROPERTY_CAPTURE_ENTRY_CACHE_PCT_CACHED =
       "capture_entry_cache_percent_entries_cached";



  /**
   * The flag that indicates whether the entry cache percent of entries cached
   * will be captured by default.
   */
  public static final boolean DEFAULT_CAPTURE_ENTRY_CACHE_PCT_CACHED = true;



  /**
   * The display name for the stat tracker that monitors the current entry cache
   * hit ratio.
   */
  public static final String STAT_TRACKER_ENTRY_CACHE_HIT_RATIO =
      "Entry Cache Hit Ratio";



  /**
   * The name of the configuration property that indicates whether to capture
   * information about the entry cache hit ratio.
   */
  public static final String PROPERTY_CAPTURE_ENTRY_CACHE_HIT_RATIO =
       "capture_entry_cache_hit_ratio";



  /**
   * The flag that indicates whether the entry cache hit ratio will be captured
   * by default.
   */
  public static final boolean DEFAULT_CAPTURE_ENTRY_CACHE_HIT_RATIO = true;



  /**
   * The name of the configuration parameter that specifies which backend(s) to
   * monitor for statistics.
   */
  public static final String PROPERTY_MONITOR_DBS = "monitor_dbs";



  /**
   * The name of the configuration property that specifies the address of the
   * directory server.
   */
  public static final String PROPERTY_DS_HOST = "ds_address";



  /**
   * The default address to use for the directory server.
   */
  public static final String DEFAULT_DS_HOST = "127.0.0.1";



  /**
   * The name of the configuration property that specifies the port of the
   * directory server.
   */
  public static final String PROPERTY_DS_PORT = "ds_port";



  /**
   * The default port to use for the directory server.
   */
  public static final int DEFAULT_DS_PORT = 389;



  /**
   * The name of the configuration property that specifies the DN to use when
   * binding to the server.
   */
  public static final String PROPERTY_DS_BIND_DN = "ds_bind_dn";



  /**
   * The default DN to use when binding to the directory server.
   */
  public static final String DEFAULT_DS_BIND_DN = "";



  /**
   * The name of the configuration property that specifies the password to use
   * when binding to the server.
   */
  public static final String PROPERTY_DS_BIND_PW = "ds_bind_pw";



  /**
   * The default password to use when binding to the directory server.
   */
  public static final String DEFAULT_DS_BIND_PW = "";



  /**
   * The name of the LDAP attribute that specifies the total number of
   * connections currently established to the server.
   */
  public static final String ATTR_CURRENT_CONNECTIONS = "currentConnections";



  /**
   * The name of the LDAP attribute that specifies the total number of
   * operations that have been requested of the server.
   */
  public static final String ATTR_OPS_INITIATED = "opsInitiated";



  /**
   * The name of the LDAP attribute that specifies the total number of
   * operations that have been processed by the server.
   */
  public static final String ATTR_OPS_COMPLETED = "opsCompleted";



  /**
   * The name of the LDAP attribute that specifies the DNs of the entries
   * serving as monitors for the backend databases.
   */
  public static final String ATTR_BACKEND_MONITOR_DN = "backendMonitorDN";



  /**
   * The name of the LDAP attribute that specifies the total number of keys
   * in the backend database.
   */
  public static final String ATTR_DB_ENTRY_COUNT = "dbEntryCount";



  /**
   * The name of the LDAP attribute that specifies the number of entries in the
   * backend database.
   */
  public static final String ATTR_LDAP_ENTRY_COUNT = "ldapEntryCount";



  /**
   * The name of the LDAP attribute that specifies the current size of the entry
   * cache in bytes.
   */
  public static final String ATTR_CURRENT_EC_SIZE = "currentEntryCacheSize";



  /**
   * The name of the LDAP attribute that specifies the total number of entries
   * currently in the entry cache.
   */
  public static final String ATTR_CURRENT_EC_ENTRIES = "currentEntryCacheCount";



  /**
   * The name of the LDAP attribute that specifies the maximum entry cache size
   * in bytes.
   */
  public static final String ATTR_MAX_EC_SIZE = "maxEntryCacheSize";



  /**
   * The name of the LDAP attribute that specifies the entry cache hit ratio.
   */
  public static final String ATTR_EC_HIT_RATIO = "entryCacheHitRatio";



  /**
   * The names of the attributes that should be retrieved when reading the
   * primary monitor entry.
   */
  public static final String[] MONITOR_ATTRS =
  {
    ATTR_CURRENT_CONNECTIONS,
    ATTR_OPS_INITIATED,
    ATTR_OPS_COMPLETED,
    ATTR_BACKEND_MONITOR_DN
  };



  /**
   * The names of the attributes that should be retrieved when reading a
   * database monitor entry.
   */
  public static final String[] DB_MONITOR_ATTRS =
  {
    ATTR_DB_ENTRY_COUNT,
    ATTR_LDAP_ENTRY_COUNT,
    ATTR_CURRENT_EC_SIZE,
    ATTR_MAX_EC_SIZE,
    ATTR_CURRENT_EC_ENTRIES,
    ATTR_EC_HIT_RATIO
  };



  // The data collected by this resource monitor.
  private ArrayList<Integer>   connList;
  private ArrayList<Integer>   opsCompletedList;
  private ArrayList<Integer>   opsInitiatedList;
  private ArrayList<Integer>[] dbCountList;
  private ArrayList<Integer>[] entryCountList;
  private ArrayList<Integer>[] cacheEntriesList;
  private ArrayList<Integer>[] cachePercentFullList;
  private ArrayList<Integer>[] cachePercentEntriesList;
  private ArrayList<Integer>[] cacheHitRatioList;
  private ArrayList<Long>[]    cacheSizeList;

  // The arrays of the DNs of backend monitors and their database names.
  private String[] backendMonitorDNs;
  private String[] backendDBNames;

  // The maximum entry cache size for each database.
  private long[] maxEntryCacheSizes;

  // The information to use to connect to the directory server.
  private int    dsPort;
  private String dsHost;
  private String bindDN;
  private String bindPW;

  // The information we will use to create the stat trackers later.
  private int    collectionInterval;
  private String clientID;
  private String threadID;

  // Information about which statistics we want to capture.
  private boolean captureBackendStats;
  private boolean captureCurrentConnections;
  private boolean captureRequestsInProgress;
  private boolean captureRequestsCompleted;
  private boolean captureDBEntryCount;
  private boolean captureLDAPEntryCount;
  private boolean captureEntryCacheSize;
  private boolean captureEntryCacheCount;
  private boolean captureEntryCachePercentFull;
  private boolean captureEntryCachePercentEntries;
  private boolean captureEntryCacheHitRatio;

  // A flag that indicates whether the monitor information was retrieved during
  // initialization, and a variable used to hold the reason for the failure if
  // it was not successful.
  private boolean monitorRetrievedInInit;
  private String  failureReason;



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
    captureCurrentConnections =
         getProperty(PROPERTY_CAPTURE_CURRENT_CONNECTIONS,
                     DEFAULT_CAPTURE_CURRENT_CONNECTIONS);
    captureRequestsInProgress =
         getProperty(PROPERTY_CAPTURE_REQUESTS_IN_PROGRESS,
                     DEFAULT_CAPTURE_REQUESTS_IN_PROGRESS);
    captureRequestsCompleted = getProperty(PROPERTY_CAPTURE_REQUESTS_COMPLETED,
                                           DEFAULT_CAPTURE_REQUESTS_COMPLETED);
    captureDBEntryCount = getProperty(PROPERTY_CAPTURE_DB_ENTRY_COUNT,
                                      DEFAULT_CAPTURE_DB_ENTRY_COUNT);
    captureLDAPEntryCount = getProperty(PROPERTY_CAPTURE_LDAP_ENTRY_COUNT,
                                        DEFAULT_CAPTURE_LDAP_ENTRY_COUNT);
    captureEntryCacheSize = getProperty(PROPERTY_CAPTURE_ENTRY_CACHE_SIZE,
                                        DEFAULT_CAPTURE_ENTRY_CACHE_SIZE);
    captureEntryCacheCount = getProperty(PROPERTY_CAPTURE_ENTRY_CACHE_COUNT,
                                         DEFAULT_CAPTURE_ENTRY_CACHE_COUNT);
    captureEntryCachePercentFull =
         getProperty(PROPERTY_CAPTURE_ENTRY_CACHE_PCT_FULL,
                     DEFAULT_CAPTURE_ENTRY_CACHE_PCT_FULL);
    captureEntryCachePercentEntries =
         getProperty(PROPERTY_CAPTURE_ENTRY_CACHE_PCT_CACHED,
                     DEFAULT_CAPTURE_ENTRY_CACHE_PCT_CACHED);
    captureEntryCacheHitRatio =
         getProperty(PROPERTY_CAPTURE_ENTRY_CACHE_HIT_RATIO,
                     DEFAULT_CAPTURE_ENTRY_CACHE_HIT_RATIO);

    String monitorDBStr = getProperty(PROPERTY_MONITOR_DBS);
    if ((monitorDBStr == null) || (monitorDBStr.length() == 0))
    {
      backendMonitorDNs  = null;
      backendDBNames     = null;
      maxEntryCacheSizes = null;
    }
    else
    {
      ArrayList<String> nameList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(monitorDBStr, ",");
      while (tokenizer.hasMoreTokens())
      {
        nameList.add(tokenizer.nextToken().trim());
      }

      backendDBNames = new String[nameList.size()];
      nameList.toArray(backendDBNames);

      backendMonitorDNs  = new String[backendDBNames.length];
      maxEntryCacheSizes = new long[backendDBNames.length];
    }

    dsHost = getProperty(PROPERTY_DS_HOST, DEFAULT_DS_HOST);
    dsPort = getProperty(PROPERTY_DS_PORT, DEFAULT_DS_PORT);
    bindDN = getProperty(PROPERTY_DS_BIND_DN, DEFAULT_DS_BIND_DN);
    bindPW = getProperty(PROPERTY_DS_BIND_PW, DEFAULT_DS_BIND_PW);


    // Make sure that we can actually read the cn=monitor entry
    boolean connFailure = false;
    failureReason = null;
    monitorRetrievedInInit = false;
    LDAPConnection conn = new LDAPConnection();
    try
    {
      conn.connect(3, dsHost, dsPort, bindDN, bindPW);
    }
    catch (LDAPException le)
    {
      connFailure = true;
      failureReason = "Unable to establish an LDAP connection to " + dsHost +
                      ':' + dsPort + " -- " + le.getLDAPResultCode() + " (" +
                      le.getLDAPErrorMessage() + ')';
    }

    if (! connFailure)
    {
      LDAPSearchResults results = null;
      try
      {
        results = conn.search("cn=monitor", LDAPConnection.SCOPE_BASE,
                              "(objectClass=*)", MONITOR_ATTRS, false);
      }
      catch (LDAPException le)
      {
        try
        {
          conn.disconnect();
        } catch (Exception e) {}
        connFailure = true;
        failureReason = "Unable to search the directory " + dsHost + ':' +
                        dsPort + " for the cn=monitor entry:  " +
                        le.getLDAPResultCode() + '(' +
                        le.getLDAPErrorMessage() + ')';
      }

      LDAPEntry monitorEntry = null;
      while ((! connFailure) && results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          monitorEntry = (LDAPEntry) element;
        }
      }

      if ((! connFailure) && (monitorEntry == null))
      {
        connFailure = true;
        failureReason = "Could not read cn=monitor entry from " + dsHost + ':' +
                        dsPort + " -- no entries returned from search";
      }


      // If a set of DB names was specified, then make sure we can read the
      // monitor entries for each.  If no names were specified, then use the
      // values of the backendMonitorDN attribute.
      if ((! connFailure) &&
          ((backendDBNames == null) || (backendDBNames.length == 0)))
      {
        ArrayList<String> dnList    = new ArrayList<String>();
        ArrayList<String> nameList  = new ArrayList<String>();
        ArrayList<Long> maxSizeList = new ArrayList<Long>();

        LDAPAttribute attr = monitorEntry.getAttribute(ATTR_BACKEND_MONITOR_DN);
        String[] values = null;
        if ((attr == null) || ((values = attr.getStringValueArray()) == null) ||
            (values.length == 0))
        {
          writeVerbose("No backends found -- will not monitor backend " +
                       "information");
          captureDBEntryCount             = false;
          captureLDAPEntryCount           = false;
          captureEntryCacheHitRatio       = false;
          captureEntryCacheCount          = false;
          captureEntryCacheSize           = false;
          captureEntryCachePercentFull    = false;
          captureEntryCachePercentEntries = false;
        }
        else
        {
          for (int i=0; i < values.length; i++)
          {
            String dn = values[i];

            try
            {
              results = conn.search(dn, LDAPConnection.SCOPE_BASE,
                                    "(objectClass=*)", DB_MONITOR_ATTRS, false);
              LDAPEntry dbEntry = null;
              while (results.hasMoreElements())
              {
                Object element = results.nextElement();
                if (element instanceof LDAPEntry)
                {
                  dbEntry = (LDAPEntry) element;
                }
              }
              if (dbEntry == null)
              {
                writeVerbose("Unable to read backend monitor " + dn);
                continue;
              }

              LDAPAttribute maxSizeAttr =
                   dbEntry.getAttribute(ATTR_MAX_EC_SIZE);
              String[] sizeValues = null;
              if ((maxSizeAttr != null) &&
                  ((sizeValues = maxSizeAttr.getStringValueArray()) != null) &&
                  (sizeValues.length > 0))
              {
                maxSizeList.add(new Long(sizeValues[0]));
              }
            }
            catch (Exception e)
            {
              writeVerbose("Exception reading backend monitor " + dn + " -- " +
                           e);
              continue;
            }

            dnList.add(dn);
            int commaPos = dn.indexOf(',');
            int equalPos = dn.indexOf('=', commaPos+1);
            commaPos = dn.indexOf(',', equalPos+1);
            nameList.add(dn.substring(equalPos+1, commaPos));
          }
        }

        backendDBNames     = new String[nameList.size()];
        backendMonitorDNs  = new String[dnList.size()];
        nameList.toArray(backendDBNames);
        dnList.toArray(backendMonitorDNs);

        maxEntryCacheSizes = new long[maxSizeList.size()];
        for (int i=0; i < maxEntryCacheSizes.length; i++)
        {
          maxEntryCacheSizes[i] = maxSizeList.get(i);
        }
      }
      else if (! connFailure)
      {
        for (int i=0; i < backendDBNames.length; i++)
        {
          String dn = "cn=monitor,cn=" + backendDBNames[i] +
                      ",cn=ldbm database,cn=plugins,cn=config";
          try
          {
            results = conn.search(dn, LDAPConnection.SCOPE_BASE,
                                  "(objectClass=*)", DB_MONITOR_ATTRS, false);

            LDAPEntry backendMonitorEntry = null;
            while (results.hasMoreElements())
            {
              Object element = results.nextElement();
              if (element instanceof LDAPEntry)
              {
                backendMonitorEntry = (LDAPEntry) element;
              }
            }

            if (backendMonitorEntry == null)
            {
              connFailure = true;
              failureReason = "Unable to read monitor entry " + dn + " from " +
                              dsHost + ':' + dsPort +
                              " -- no entries returned from search.";
            }

            if (! connFailure)
            {
              LDAPAttribute maxSizeAttr =
                   backendMonitorEntry.getAttribute(ATTR_MAX_EC_SIZE);
              String[] values = null;
              if ((maxSizeAttr == null) ||
                  ((values = maxSizeAttr.getStringValueArray()) == null) ||
                  (values.length == 0))
              {
                connFailure = true;
                failureReason = "Unable to read data from backend monitor " +
                                "entry " + dn + " from " + dsHost + ':' +
                                dsPort;
              }

              if (! connFailure)
              {
                maxEntryCacheSizes[i] = Long.parseLong(values[0]);
                backendMonitorDNs[i]  = dn;
              }
            }
          }
          catch (LDAPException le)
          {
            connFailure = true;
            failureReason = "Unable to search the directory " + dsHost + ':' +
                            dsPort + " for the " + dn + " entry:  " +
                            le.getLDAPResultCode() + '(' +
                            le.getLDAPErrorMessage() + ')';
          }
        }
      }

      try
      {
        conn.disconnect();
      } catch (Exception e) {}
    }

    if (! connFailure)
    {
      captureBackendStats =
           (((! connFailure) && (backendMonitorDNs.length > 0)) &&
            (captureDBEntryCount || captureLDAPEntryCount ||
             captureEntryCacheCount ||
             captureEntryCacheHitRatio ||
             captureEntryCachePercentFull ||
             captureEntryCachePercentEntries ||
             captureEntryCacheSize));
      monitorRetrievedInInit = (! connFailure);

      opsCompletedList        = new ArrayList<Integer>();
      connList                = new ArrayList<Integer>();
      opsInitiatedList        = new ArrayList<Integer>();

      initializeListArrays();
    }
  }



  /**
   * Initializses arrays of lists.  Suppress warnings since an array cannot be
   * created with a type.
   */
  @SuppressWarnings("unchecked")
  private void initializeListArrays()
  {
    dbCountList             = new ArrayList[backendMonitorDNs.length];
    entryCountList          = new ArrayList[backendMonitorDNs.length];
    cacheSizeList           = new ArrayList[backendMonitorDNs.length];
    cacheEntriesList        = new ArrayList[backendMonitorDNs.length];
    cachePercentFullList    = new ArrayList[backendMonitorDNs.length];
    cachePercentEntriesList = new ArrayList[backendMonitorDNs.length];
    cacheHitRatioList       = new ArrayList[backendMonitorDNs.length];

    for (int i=0; i < backendMonitorDNs.length; i++)
    {
      dbCountList[i]             = new ArrayList<Integer>();
      entryCountList[i]          = new ArrayList<Integer>();
      cacheSizeList[i]           = new ArrayList<Long>();
      cacheEntriesList[i]        = new ArrayList<Integer>();
      cachePercentFullList[i]    = new ArrayList<Integer>();
      cachePercentEntriesList[i] = new ArrayList<Integer>();
      cacheHitRatioList[i]       = new ArrayList<Integer>();
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
    SunONEDirectoryServerResourceMonitor monitor =
         new SunONEDirectoryServerResourceMonitor();
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
    // Capture the information now so that we can use it later.
    this.clientID           = clientID;
    this.threadID           = threadID;
    this.collectionInterval = collectionInterval;
  }



  /**
   * Retrieves the name to use for this resource monitor.
   *
   * @return  The name to use for this resource monitor.
   */
  @Override()
  public String getMonitorName()
  {
    return "Sun ONE Directory Monitor";
  }



  /**
   * Retrieves the statistical data collected by this resource monitor.
   *
   * @return  The statistical data collected by this resource monitor.
   */
  @Override()
  public StatTracker[] getResourceStatistics()
  {
    if (! monitorRetrievedInInit)
    {
      return new StatTracker[0];
    }

    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
    String    prefix      = "ldap://" + dsHost + ':' + dsPort + "/ ";

    if (captureCurrentConnections)
    {
      int[] connArray  = new int[connList.size()];
      int[] countArray = new int[connList.size()];
      for (int i=0; i < connArray.length; i++)
      {
        connArray[i]  = connList.get(i);
        countArray[i] = 1;
      }

      IntegerValueTracker tracker =
           new IntegerValueTracker(clientID, threadID,
                                   prefix + STAT_TRACKER_CURRENT_CONNECTIONS,
                                   collectionInterval);
      tracker.setIntervalData(connArray, countArray);
      trackerList.add(tracker);
    }

    if (captureRequestsInProgress || captureRequestsCompleted)
    {
      int[] inProgressArray   = new int[opsInitiatedList.size()];
      int[] opsCompletedArray = new int[opsCompletedList.size()];
      int[] countArray        = new int[opsInitiatedList.size()];
      for (int i=0; i < inProgressArray.length; i++)
      {
        int opsInitiated = opsInitiatedList.get(i);
        int opsCompleted = opsCompletedList.get(i);

        opsCompletedArray[i] = opsCompleted;
        inProgressArray[i]   = opsInitiated - opsCompleted;
        countArray[i]        = 1;
      }

      if (captureRequestsInProgress)
      {
        IntegerValueTracker tracker =
             new IntegerValueTracker(clientID, threadID,
                                     prefix +
                                     STAT_TRACKER_REQUESTS_IN_PROGRESS,
                                     collectionInterval);
        tracker.setIntervalData(inProgressArray, countArray);
        trackerList.add(tracker);
      }

      if (captureRequestsCompleted)
      {
        IntegerValueTracker tracker =
             new IntegerValueTracker(clientID, threadID,
                                     prefix + STAT_TRACKER_REQUESTS_COMPLETED,
                                     collectionInterval);
        tracker.setIntervalData(opsCompletedArray, countArray);
        trackerList.add(tracker);
      }
    }

    if (captureBackendStats)
    {
      for (int i=0; i < backendDBNames.length; i++)
      {
        prefix = "ldap://" + dsHost + ':' + dsPort + "/ " + backendDBNames[i] +
                 ' ';

        if (captureDBEntryCount)
        {
          int[] dbCountArray = new int[dbCountList[i].size()];
          int[] countArray   = new int[dbCountList[i].size()];
          for (int j=0; j < dbCountArray.length; j++)
          {
            dbCountArray[j] = dbCountList[i].get(j);
            countArray[j]   = 1;
          }

          IntegerValueTracker tracker =
               new IntegerValueTracker(clientID, threadID,
                                       prefix + STAT_TRACKER_DB_ENTRY_COUNT,
                                       collectionInterval);
          tracker.setIntervalData(dbCountArray, countArray);
          trackerList.add(tracker);
        }

        if (captureLDAPEntryCount)
        {
          int[] entryCountArray = new int[entryCountList[i].size()];
          int[] countArray      = new int[entryCountList[i].size()];
          for (int j=0; j < entryCountArray.length; j++)
          {
            entryCountArray[j] = entryCountList[i].get(j);
            countArray[j]      = 1;
          }

          IntegerValueTracker tracker =
               new IntegerValueTracker(clientID, threadID,
                                       prefix + STAT_TRACKER_LDAP_ENTRY_COUNT,
                                       collectionInterval);
          tracker.setIntervalData(entryCountArray, countArray);
          trackerList.add(tracker);
        }

        if (captureEntryCacheCount)
        {
          int[] cacheCountArray = new int[cacheEntriesList[i].size()];
          int[] countArray      = new int[cacheEntriesList[i].size()];
          for (int j=0; j < cacheCountArray.length; j++)
          {
            cacheCountArray[j] = cacheEntriesList[i].get(j);
            countArray[j]      = 1;
          }

          IntegerValueTracker tracker =
               new IntegerValueTracker(clientID, threadID,
                                       prefix + STAT_TRACKER_ENTRY_CACHE_COUNT,
                                       collectionInterval);
          tracker.setIntervalData(cacheCountArray, countArray);
          trackerList.add(tracker);
        }

        if (captureEntryCachePercentEntries)
        {
          int[] cachePctArray = new int[entryCountList[i].size()];
          int[] countArray    = new int[entryCountList[i].size()];
          for (int j=0; j < cachePctArray.length; j++)
          {
            int entryCount = entryCountList[i].get(j);
            int cacheCount = cacheEntriesList[i].get(j);
            cachePctArray[j] =
                 (int) Math.round(1.0 * cacheCount * 100 / entryCount);
            countArray[j] = 1;
          }

          IntegerValueTracker tracker =
               new IntegerValueTracker(clientID, threadID,
                                       prefix +
                                       STAT_TRACKER_ENTRY_CACHE_PCT_CACHED,
                                       collectionInterval);
          tracker.setIntervalData(cachePctArray, countArray);
          trackerList.add(tracker);
        }

        if (captureEntryCacheSize || captureEntryCachePercentFull)
        {
          int[] sizeArray    = new int[cacheSizeList[i].size()];
          int[] pctFullArray = new int[cacheSizeList[i].size()];
          int[] countArray   = new int[cacheSizeList[i].size()];
          for (int j=0; j < sizeArray.length; j++)
          {
            long size       = cacheSizeList[i].get(j);
            sizeArray[j]    = (int) Math.round(1.0 * size / 1024 / 1024);
            pctFullArray[j] = (int) Math.round(1.0 * size * 100 /
                                               maxEntryCacheSizes[i]);
            countArray[j]   = 1;
          }

          if (captureEntryCacheSize)
          {
            IntegerValueTracker tracker =
                 new IntegerValueTracker(clientID, threadID,
                                         prefix + STAT_TRACKER_ENTRY_CACHE_SIZE,
                                         collectionInterval);
            tracker.setIntervalData(sizeArray, countArray);
            trackerList.add(tracker);
          }

          if (captureEntryCachePercentFull)
          {
            IntegerValueTracker tracker =
                 new IntegerValueTracker(clientID, threadID,
                                         prefix +
                                         STAT_TRACKER_ENTRY_CACHE_PCT_FULL,
                                         collectionInterval);
            tracker.setIntervalData(pctFullArray, countArray);
            trackerList.add(tracker);
          }
        }

        if (captureEntryCacheHitRatio)
        {
          int[] hitRatioArray = new int[cacheHitRatioList[i].size()];
          int[] countArray    = new int[cacheHitRatioList[i].size()];
          for (int j=0; j < hitRatioArray.length; j++)
          {
            hitRatioArray[j] = cacheHitRatioList[i].get(j);
            countArray[j]    = 1;
          }

          IntegerValueTracker tracker =
               new IntegerValueTracker(clientID, threadID,
                                       prefix +
                                       STAT_TRACKER_ENTRY_CACHE_HIT_RATIO,
                                       collectionInterval);
          tracker.setIntervalData(hitRatioArray, countArray);
          trackerList.add(tracker);
        }
      }
    }

    StatTracker[] returnTrackers = new StatTracker[trackerList.size()];
    trackerList.toArray(returnTrackers);
    return returnTrackers;
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
    if (! monitorRetrievedInInit)
    {
      if (failureReason == null)
      {
        logMessage("Unable to retrieve directory server monitor information " +
                   "during initialization");
      }
      else
      {
        logMessage("Unable to retrieve directory server monitor information " +
                   "during initialization:  " + failureReason);
      }

      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }


    LDAPConnection conn = new LDAPConnection();
    try
    {
      conn.connect(3, dsHost, dsPort, bindDN, bindPW);
    }
    catch (LDAPException le)
    {
      logMessage("Unable to connect to directory server " + dsHost + ':' +
                 dsPort + " -- " + le.getLDAPResultCode() + " (" +
                 le.getLDAPErrorMessage() + ')');
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    while (! shouldStop())
    {
      long nextStartTime = System.currentTimeMillis() +
                           (1000 * collectionInterval);

      try
      {
        captureData(conn);
      }
      catch (LDAPException le)
      {
        try
        {
          conn.disconnect();
        } catch (Exception e) {}

        logMessage("Exception encountered while trying to process monitor " +
                   "data:  " + le.getLDAPResultCode() + " (" +
                   le.getLDAPErrorMessage() + ')');
        return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      }

      long sleepTime = nextStartTime - System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {}
      }
    }

    try
    {
      conn.disconnect();
    }
    catch (Exception e) {}

    return Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
  }



  /**
   * Retrieves the monitor information and processes it as necessary.
   *
   * @param  conn  The connection to the directory server to use when reading
   *               the information.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   */
  private void captureData(LDAPConnection conn)
          throws LDAPException
  {
    LDAPEntry entry = conn.read("cn=monitor", MONITOR_ATTRS);
    if (entry == null)
    {
      throw new LDAPException("Unable to read monitor entry",
                              LDAPException.NO_RESULTS_RETURNED,
                              "Unable to read monitor entry");
    }

    LDAPAttribute attr   = null;
    String[]      values = null;

    if (captureCurrentConnections)
    {
      attr = entry.getAttribute(ATTR_CURRENT_CONNECTIONS);
      if ((attr == null) || ((values = attr.getStringValueArray()) == null) ||
          (values.length == 0))
      {
        throw new LDAPException("Unable to read current connection count",
                                LDAPException.NO_RESULTS_RETURNED,
                                "Unable to read current connection count");
      }

      try
      {
        connList.add(new Integer(values[0]));
      }
      catch (NumberFormatException nfe)
      {
        throw new LDAPException("Unable to parse current connection count",
                                LDAPException.NO_RESULTS_RETURNED,
                                "Unable to parse current connection count");
      }
    }


    if (captureRequestsInProgress || captureRequestsCompleted)
    {
      Integer opsInitiated;
      attr = entry.getAttribute(ATTR_OPS_INITIATED);
      if ((attr == null) || ((values = attr.getStringValueArray()) == null) ||
          (values.length == 0))
      {
        throw new LDAPException("Unable to read opsInitiated",
                                LDAPException.NO_RESULTS_RETURNED,
                                "Unable to read opsInitiated");
      }

      try
      {
        opsInitiated = new Integer(values[0]);
      }
      catch (NumberFormatException nfe)
      {
        throw new LDAPException("Unable to parse opsInitiated",
                                LDAPException.NO_RESULTS_RETURNED,
                                "Unable to parse opsInitiated");
      }


      Integer opsCompleted;
      attr = entry.getAttribute(ATTR_OPS_COMPLETED);
      if ((attr == null) || ((values = attr.getStringValueArray()) == null) ||
          (values.length == 0))
      {
        throw new LDAPException("Unable to read opsCompleted",
                                LDAPException.NO_RESULTS_RETURNED,
                                "Unable to read opsCompleted");
      }

      try
      {
        opsCompleted = new Integer(values[0]);
      }
      catch (NumberFormatException nfe)
      {
        throw new LDAPException("Unable to parse opsCompleted",
                                LDAPException.NO_RESULTS_RETURNED,
                                "Unable to parse opsCompleted");
      }

      opsInitiatedList.add(opsInitiated);
      opsCompletedList.add(opsCompleted);
    }


    if (captureBackendStats)
    {
      for (int i=0; i < backendMonitorDNs.length; i++)
      {
        entry = conn.read(backendMonitorDNs[i], DB_MONITOR_ATTRS);
        if (entry == null)
        {
          throw new LDAPException("Unable to read backend monitor entry " +
                                  backendMonitorDNs[i],
                                  LDAPException.NO_RESULTS_RETURNED,
                                  "Unable to read monitor entry " +
                                  backendMonitorDNs[i]);
        }

        if (captureDBEntryCount)
        {
          attr = entry.getAttribute(ATTR_DB_ENTRY_COUNT);
          if ((attr == null) ||
              ((values = attr.getStringValueArray()) == null) ||
              (values.length == 0))
          {
            throw new LDAPException("Unable to read dbEntryCount from " +
                                    backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to read dbEntryCount from " +
                                    backendMonitorDNs[i]);
          }

          try
          {
            dbCountList[i].add(new Integer(values[0]));
          }
          catch (NumberFormatException nfe)
          {
            throw new LDAPException("Unable to parse dbEntryCount from " +
                                    backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to parse dbEntryCount from " +
                                    backendMonitorDNs[i]);
          }
        }

        if (captureLDAPEntryCount || captureEntryCachePercentEntries)
        {
          attr = entry.getAttribute(ATTR_LDAP_ENTRY_COUNT);
          if ((attr == null) ||
              ((values = attr.getStringValueArray()) == null) ||
              (values.length == 0))
          {
            throw new LDAPException("Unable to read ldapEntryCount from " +
                                    backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to read ldapEntryCount from " +
                                    backendMonitorDNs[i]);
          }

          try
          {
            entryCountList[i].add(new Integer(values[0]));
          }
          catch (NumberFormatException nfe)
          {
            throw new LDAPException("Unable to parse ldapEntryCount from " +
                                    backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to parse ldapEntryCount from " +
                                    backendMonitorDNs[i]);
          }
        }

        if (captureEntryCacheCount || captureEntryCachePercentEntries)
        {
          attr = entry.getAttribute(ATTR_CURRENT_EC_ENTRIES);
          if ((attr == null) ||
              ((values = attr.getStringValueArray()) == null) ||
              (values.length == 0))
          {
            throw new LDAPException("Unable to read currentEntryCacheCount " +
                                    "from " + backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to read currentEntryCacheCount " +
                                    "from " + backendMonitorDNs[i]);
          }

          try
          {
            cacheEntriesList[i].add(new Integer(values[0]));
          }
          catch (NumberFormatException nfe)
          {
            throw new LDAPException("Unable to parse currentEntryCacheCount " +
                                    "from " + backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to parse currentEntryCacheCount " +
                                    "from " + backendMonitorDNs[i]);
          }
        }

        if (captureEntryCacheSize || captureEntryCachePercentFull)
        {
          attr = entry.getAttribute(ATTR_CURRENT_EC_SIZE);
          if ((attr == null) ||
              ((values = attr.getStringValueArray()) == null) ||
              (values.length == 0))
          {
            throw new LDAPException("Unable to read currentEntryCacheSize " +
                                    "from " + backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to read currentEntryCacheSize " +
                                    "from " + backendMonitorDNs[i]);
          }

          try
          {
            cacheSizeList[i].add(new Long(values[0]));
          }
          catch (NumberFormatException nfe)
          {
            throw new LDAPException("Unable to parse currentEntryCacheSize " +
                                    "from " + backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to parse currentEntryCacheSize " +
                                    "from " + backendMonitorDNs[i]);
          }
        }

        if (captureEntryCacheHitRatio)
        {
          attr = entry.getAttribute(ATTR_EC_HIT_RATIO);
          if ((attr == null) ||
              ((values = attr.getStringValueArray()) == null) ||
              (values.length == 0))
          {
            throw new LDAPException("Unable to read entryCacheHitRatio " +
                                    "from " + backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to read entryCacheHitRatio " +
                                    "from " + backendMonitorDNs[i]);
          }

          try
          {
            cacheHitRatioList[i].add(new Integer(values[0]));
          }
          catch (NumberFormatException nfe)
          {
            throw new LDAPException("Unable to parse entryCacheHitRatio " +
                                    "from " + backendMonitorDNs[i],
                                    LDAPException.NO_RESULTS_RETURNED,
                                    "Unable to parse entryCacheHitRatio " +
                                    "from " + backendMonitorDNs[i]);
          }
        }
      }
    }
  }
}

