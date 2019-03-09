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
 * Contributor(s):  Bertold Kolics, Neil A. Wilson
 */
package com.slamd.resourcemonitor;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.stat.StatTracker;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;



/**
 * The LDAP resource monitor allows a resource with statistics exposed over
 * LDAP.
 */
public class LDAPResourceMonitor extends ResourceMonitor {

  /**
   * The name of the configuration property that specifies the address of the
   * directory server.
   */
  public static final String PROPERTY_LDAP_HOST = "ldap_address";

  /**
   * The default address to use for the LDAP server.
   */
  public static final String DEFAULT_LDAP_HOST = "127.0.0.1";

  /**
   * The name of the configuration property that specifies the port of the
   * LDAP server.
   */
  public static final String PROPERTY_LDAP_PORT = "ldap_port";

  /**
   * The default port to use for the LDAP server.
   */
  public static final int DEFAULT_LDAP_PORT = 389;

  /**
   * The name of the configuration property that specifies the DN to use when
   * binding to the server.
   */
  public static final String PROPERTY_LDAP_BIND_DN = "ldap_bind_dn";

  /**
   * The default DN to use when binding to the LDAP server.
   */
  public static final String DEFAULT_LDAP_BIND_DN = "";

  /**
   * The name of the configuration property that specifies the password to use
   * when binding to the server.
   */
  public static final String PROPERTY_LDAP_BIND_PW = "ldap_bind_pw";

  /**
   * The default password to use when binding to the LDAP server.
   */
  public static final String DEFAULT_LDAP_BIND_PW = "";

  // The information we will use to create the stat trackers later.
  private int collectionInterval;

  // The information to use to connect to the directory server.
  private String ldapHost;
  private int ldapPort;
  private String bindDN;
  private String bindPW;

  // A flag that indicates whether the monitor information was retrieved during
  // initialization, and a variable used to hold the reason for the failure if
  // it was not successful.
  private boolean monitorRetrievedInInit = false;
  private String failureReason = null;

  // Array of monitored entries tracked by this resource monitor
  private LDAPMonitoredEntry[] monitoredEntries;

  /**
   * Utility method to allow parsing an LDIF file and print out the
   * corresponding configuration file.
   * @param args argument array
   * @throws IOException if the provided file cannot be opened for some reason
   */
  public static void main(String args[]) throws IOException
  {
    if (args.length != 1)
    {
      System.err.println("Usage: java " +
          "com.slamd.resourcemonitor.LDAPResourceMonitor " +
          "<ldif file>");
      return;
    }

    int counter = 0;

    LDIFReader ldifReader = new LDIFReader(args[0]);
    while (true)
    {
      Entry entry;
      try
      {
        entry = ldifReader.readEntry();
        if (entry == null)
        {
          // All entries have been processed
          break;
        }
      }
      catch (LDIFException le)
      {
        if (le.mayContinueReading())
        {
          System.err.println("A recoverable occurred while attempting to " +
              "read an entry at or near line number " + le.getLineNumber() +
              ":  " + le.getMessage());
          System.err.println("The entry will be skipped.");
          continue;
        }
        else
        {
          System.err.println("An unrecoverable occurred while attempting to " +
              "read an entry at or near line number " + le.getLineNumber() +
              ":  " + le.getMessage());
          System.err.println("LDIF processing will be aborted.");
          break;
        }
      }
      catch (IOException ioe)
      {
        System.err.println("An I/O error occurred while attempting to read " +
            "from the LDIF file:  " + ioe.getMessage());
        System.err.println("LDIF processing will be aborted.");
        break;
      }

      String propBase = String.format("entry%d.", counter);
      System.out.print(propBase);
      System.out.print("dn=");
      System.out.println(entry.getDN());

      for (Attribute a : entry.getAttributes())
      {
        String attrName = a.getBaseName();
        String attrValue = a.getValue();
        String tracker = "LongValueTracker";
        try {
          double d = Double.parseDouble(attrValue);
          if (d != (long)d)
          {
            tracker = "FloatValueTracker";
          }
        } catch (NumberFormatException e) {
          // it's not a number, skip this attribute
          continue;
        }

        String attrBase = String.format("%sattr.%s.", propBase, attrName);

        System.out.print(attrBase);
        System.out.print("display=");
        System.out.println(attrName);

        System.out.print(attrBase);
        System.out.print("tracker=");
        System.out.println(tracker);
      }
      System.out.println();

      counter++;
    }

    ldifReader.close();
  }

  /**
   * {@inheritDoc}
   */
  public String getMonitorName() {
    return "LDAP Resource Monitor";
  }

  /**
   * {@inheritDoc}
   */
  public boolean clientSupported() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public void initializeStatistics(String clientID, String threadID,
                                   int collectionInterval)
  {
    // Capture the information now so that we can use it later.
    this.collectionInterval = collectionInterval;
    if (monitoredEntries != null)
    {
      final String prefix =
          String.format("ldap://%s:%d/: ", this.ldapHost, this.ldapPort);

      for (LDAPMonitoredEntry e : monitoredEntries)
      {
        Collection<StatTracker> trackers = e.getStatTrackers();
        for (StatTracker t : trackers)
        {
          t.setClientID(clientID);
          t.setThreadID(threadID);
          t.setCollectionInterval(collectionInterval);
          t.setDisplayName(prefix + t.getDisplayName());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public ResourceMonitor newInstance() throws SLAMDException {
    final ResourceMonitor monitor = new LDAPResourceMonitor();
    monitor.initialize(getMonitorClient(), getMonitorProperties());

    return monitor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeMonitor() throws SLAMDException
  {
    this.ldapHost = getProperty(PROPERTY_LDAP_HOST, DEFAULT_LDAP_HOST);
    this.ldapPort = getProperty(PROPERTY_LDAP_PORT, DEFAULT_LDAP_PORT);
    this.bindDN = getProperty(PROPERTY_LDAP_BIND_DN, DEFAULT_LDAP_BIND_DN);
    this.bindPW = getProperty(PROPERTY_LDAP_BIND_PW, DEFAULT_LDAP_BIND_PW);

    // flag to indicate that error occured during initialization
    boolean failure = false;

    failureReason = null;
    monitorRetrievedInInit = false;
    final LDAPConnection conn = new LDAPConnection();
    try
    {
      conn.connect(ldapHost, ldapPort);
      conn.bind(bindDN, bindPW);
    }
    catch (LDAPException le)
    {
      failure = true;
      failureReason = String.format(
          "Unable to establish an LDAP connection to %s:%d -- %s (%s)",
          this.ldapHost, this.ldapPort, le.getResultCode(), le.getMessage());
    }

    if (! failure)
    {
      try {
        parseConf();
      } catch (IllegalArgumentException e) {
        failure = true;
      }
    }

    monitorRetrievedInInit = (! failure);
  }

  /**
   * Parses the resource monitor configuration from the monitor properties.
   */
  @SuppressWarnings("unchecked")
  private void parseConf()
  {
    // expand variables in property values
    Map<String,String> preProcessedProps =
        LDAPResourceMonitor.preProcess(getMonitorProperties());

    // this will hold all properties under the same property base
    // key: property base
    // value: properties under this property base
    Map<String,Map<String,String>> entryProps =
        new HashMap<String,Map<String,String>>();

    // walk over all the properties once
   for (Map.Entry<String,String> e : preProcessedProps.entrySet())
    {
      final String key = e.getKey();
      final String value = e.getValue();

      int dot = key.indexOf(".");
      if (dot <= 0)
      {
        // configuration keys have _ in their name
        if (key.indexOf("_") < 0)
        {
          // neither a configuration key nor a property related to monitoring
          throw new IllegalArgumentException("Invalid property name: " +
              key);
        }
        continue;
      }

      String propertyBase = key.substring(0, dot);
      Map<String,String> monitoredEntry = entryProps.get(propertyBase);
      if (monitoredEntry == null)
      {
        monitoredEntry = new HashMap<String,String>();
        entryProps.put(propertyBase, monitoredEntry);
      }

      monitoredEntry.put(
          key.substring(dot + 1), // remove the property base from the key
          value
      );
    }

    List<LDAPMonitoredEntry> entries = new ArrayList<LDAPMonitoredEntry>();
    for (Map.Entry<String,Map<String,String>> entry : entryProps.entrySet())
    {
      entries.add(new LDAPMonitoredEntry(
          entry.getKey(),  // property base
          entry.getValue() // properties under the base
      ));
    }

    this.monitoredEntries =
        entries.toArray(new LDAPMonitoredEntry[entries.size()]);
  }

  /**
   * Preprocesses the monitor properties and executes variable substitution.
   * Variable names are enclosed within '@' characters.
   * @param props properties to process
   * @return map of key, value pairs with expanded variables
   */
  private static Map<String, String> preProcess(Properties props)
  {
    // pattern matches property names enclosed within '@' characters
    // no space and no '@' is allowed in property names
    final Pattern varPattern = Pattern.compile("@([^@ ]+)@");
    final Map<String,String> preProcessedMap = new HashMap<String,String>();

    for (Map.Entry<Object,Object> e : props.entrySet())
    {
      final String key = e.getKey().toString();
      String value = e.getValue().toString();
      Matcher varPatternMatcher = varPattern.matcher(value);

      // loop until matches found
      while (varPatternMatcher.find())
      {

        String varName = varPatternMatcher.group(1);
        if (key.equals(varName))
        {
          // circular reference found, stop expansion
          break;
        }

        // get the value of the string enclosed within the '@'-s (e.g.
        // ldap_port)
        String replacement = props.get(varName).toString();
        if (replacement != null)
        {
          // replace first occurrence
          value = varPatternMatcher.replaceFirst(replacement);
          // create a new matcher for the new string
          // there could be other variables in the same value
          varPatternMatcher = varPattern.matcher(value);
        }
      }

      preProcessedMap.put(key, value);
    }

    return preProcessedMap;
  }

  /**
   * {@inheritDoc}
   */
  public StatTracker[] getResourceStatistics() {
    // this set will sort the trackers according to their display names
    Set<StatTracker> stats = new TreeSet<StatTracker>(
        new Comparator<StatTracker>()
        {
          public int compare(StatTracker t1, StatTracker t2)
          {
            // display names are always set in the LDAP Resource Monitor
            return t1.getDisplayName().compareTo(t2.getDisplayName());
          }
        }
    );

    if (monitoredEntries != null)
    {
      for (LDAPMonitoredEntry e : monitoredEntries)
      {
        // populate stat trackers first
        for (LDAPMonitoredAttr a : e.getMonitoredAttrs())
        {
          a.collectCapturedData();
        }

        // retrive the stat trackers
        stats.addAll(e.getStatTrackers());
      }
    }

    return stats.toArray(new StatTracker[stats.size()]);
  }

  /**
   * {@inheritDoc}
   */
  public int runMonitor() {
    if (! monitorRetrievedInInit)
    {
      if (failureReason == null)
      {
        logMessage("Unable to retrieve LDAP server monitor information " +
                   "during initialization");
      }
      else
      {
        logMessage("Unable to retrieve LDAP server monitor information " +
                   "during initialization:  " + failureReason);
      }

      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    if (monitoredEntries == null)
    {
      logMessage("Resource monitor has nothing to monitor");
      return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
    }

    LDAPConnection conn = new LDAPConnection();
    try
    {
      conn.connect(this.ldapHost, this.ldapPort);
      conn.bind(this.bindDN, this.bindPW);
    }
    catch (LDAPException le)
    {
      logMessage(
          String.format(
              "Unable to connect to directory server %s:%d -- %s (%s)",
              this.ldapHost, this.ldapPort, le.getResultCode(), le.getMessage()
          )
      );
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
          conn.close();
        }
        catch (Exception e)
        {
          // ignore
        }

        logMessage(
            String.format(
                "Exception encountered while trying to process monitor " +
                    "data: %s (%s) ", le.getResultCode(), le.getMessage()
            )
        );
        return Constants.JOB_STATE_STOPPED_DUE_TO_ERROR;
      }

      long sleepTime = nextStartTime - System.currentTimeMillis();
      if (sleepTime > 0)
      {
        try
        {
          Thread.sleep(sleepTime);
        }
        catch (InterruptedException ie)
        {
          // ignore
        }
      }
    }

    try
    {
      conn.close();
    }
    catch (Exception e)
    {
      // ignore
    }

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

    for (LDAPMonitoredEntry e : this.monitoredEntries)
    {
      final String[] monitoredAttrNames = e.getMonitoredAttrNames();
      // fetch the monitored LDAP entry
      Entry ldapEntry = conn.getEntry(e.getDN(), e.getMonitoredAttrNames());
      if (ldapEntry == null)
      {
        // skip missing entries
        continue;
      }

      // capture data for all monitored attributes in the monitored entry
      for (String monitoredAttrName : monitoredAttrNames)
      {
        String ldapValue =
            ldapEntry.getAttributeValue(monitoredAttrName);
        if (ldapValue == null)
        {
          // attribute is missing, set value to 0
          ldapValue = "0";
        }

        final LDAPMonitoredAttr monitoredAttr =
            e.getMonitoredAttr(monitoredAttrName);
        try
        {
          monitoredAttr.addCapturedData(ldapValue);
        }
        catch (NumberFormatException e1)
        {
          throw new LDAPException(ResultCode.NO_RESULTS_RETURNED,
              String.format("Unable to parse the value of %s in %s as %s.",
                  monitoredAttrName, e.getDN(), monitoredAttr.getAttrType())
          );
        }
      }
    }
  }
}
