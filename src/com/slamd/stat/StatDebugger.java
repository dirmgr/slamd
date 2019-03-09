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



import java.util.ArrayList;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class provides a means of dumping information about the statistics
 * collected by a SLAMD job.  It is a command-line application, although it also
 * provides methods for accessing the information programmatically.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatDebugger
{
  // The port number of the SLAMD configuration directory.
  private int ldapPort;

  // The DN under which the SLAMD config data exists in the directory.
  private String baseDN;

  // The DN to use when binding to the directory server.
  private String bindDN;

  // The password to use when binding to the directory server.
  private String bindPW;

  // The job ID for the job whose statistics should be dumped.
  private String jobID;

  // The address of the SLAMD configuration directory.
  private String ldapHost;



  /**
   * Parses the provided command-line parameters, creates a new instance of the
   * stat debugger, and dumps the statistics for the specified job.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // Specify default values for the configurable parameters.
    int    ldapPort = 389;
    String baseDN   = null;
    String bindDN   = "";
    String bindPW   = "";
    String jobID    = null;
    String ldapHost = "127.0.0.1";


    // Parse the command-line arguments.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        ldapHost = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        ldapPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-D"))
      {
        bindDN = args[++i];
      }
      else if (args[i].equals("-w"))
      {
        bindPW = args[++i];
      }
      else if (args[i].equals("-b"))
      {
        baseDN = args[++i];
      }
      else if (args[i].equals("-j"))
      {
        jobID = args[++i];
      }
      else
      {
        System.err.println("Unrecognized argument " + args[i]);
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that at least the base DN and job ID were specified.
    if (baseDN == null)
    {
      System.err.println("ERROR:  No configuration base DN provided");
      displayUsage();
      System.exit(1);
    }

    if (jobID == null)
    {
      System.err.println("ERROR:  No job ID provided");
      displayUsage();
      System.exit(1);
    }


    // Create the StatDebugger and retrieve the data from it.
    StatDebugger debugger = new StatDebugger(ldapHost, ldapPort, bindDN, bindPW,
                                             baseDN);
    StatTracker[] trackers = null;
    try
    {
      trackers = debugger.getStatTrackers(jobID);
    }
    catch (SLAMDException se)
    {
      System.err.println(se.getMessage());
      System.exit(1);
    }


    // Iterate through the trackers and dump the information contained in them.
    ArrayList<String> nameList = new ArrayList<String>();
    for (int i=0; i < trackers.length; i++)
    {
      String name = trackers[i].getDisplayName();
      if (! nameList.contains(name))
      {
        nameList.add(name);
      }

      System.out.println("StatTracker " + i);
      System.out.println("Tracker Name:  " + trackers[i].getDisplayName());
      System.out.println("Client ID:  " + trackers[i].getClientID());
      System.out.println("Thread ID:  " + trackers[i].getThreadID());
      System.out.println("Collection Interval:  " +
                         trackers[i].getCollectionInterval());
      System.out.println("Number of Intervals:  " +
                         trackers[i].getNumIntervals());
      System.out.println("Duration:  " +
                         trackers[i].getDuration());
      System.out.println(trackers[i].getDetailString());
      System.out.println();
    }


    // Aggregate the values contained in the trackers and display the aggregated
    // results.
    for (int i=0; i < nameList.size(); i++)
    {
      String name = nameList.get(i);
      ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
      for (int j=0; j < trackers.length; j++)
      {
        if (trackers[j].getDisplayName().equals(name))
        {
          trackerList.add(trackers[j]);
        }
      }

      StatTracker[] trackersToAggregate = new StatTracker[trackerList.size()];
      trackerList.toArray(trackersToAggregate);
      StatTracker tracker = trackersToAggregate[0].newInstance();
      tracker.aggregate(trackersToAggregate);

      System.out.println("Aggregated Results for Stat Tracker " + name);
      System.out.println("Number of Intervals:  " +
                         tracker.getNumIntervals());
      System.out.println("Duration:  " +
                         tracker.getDuration());
      System.out.println(tracker.getDetailString());
      System.out.println();
    }
  }



  /**
   * Creates a new instance of this stat debugger using the provided
   * information.
   *
   * @param  ldapHost  The SLAMD configuration directory address.
   * @param  ldapPort  The SLAMD configuration directory port.
   * @param  bindDN    The SLAMD configuration directory bind DN.
   * @param  bindPW    The SLAMD configuration directory bind password.
   * @param  baseDN    The SLAMD configuration base DN.
   */
  public StatDebugger(String ldapHost, int ldapPort, String bindDN,
                      String bindPW, String baseDN)
  {
    this.ldapHost = ldapHost;
    this.ldapPort = ldapPort;
    this.bindDN   = bindDN;
    this.bindPW   = bindPW;
    this.baseDN   = baseDN;
  }



  /**
   * Retrieves the stat trackers associated with the specified job.
   *
   * @param  jobID  The job ID of the job for which to retrieve the stat
   *                trackers.
   *
   * @return  The set of stat trackers associated with the specified job, or
   *          <CODE>null</CODE> if it does not have any statistics.
   *
   * @throws  SLAMDException  If a problem occurs while trying to retrieve and
   *                          process the job information.
   */
  public StatTracker[] getStatTrackers(String jobID)
         throws SLAMDException
  {
    // Establish a connection to the configuration directory.
    LDAPConnection conn = new LDAPConnection();
    try
    {
      conn.connect(3, ldapHost, ldapPort, bindDN, bindPW);
    }
    catch (LDAPException le)
    {
      throw new SLAMDException("Unable to bind to the configuration " +
                               "directory:  " + le, le);
    }


    // Find the entry containing information about the specified job.
    String filter = "(&(objectClass=" + Constants.SCHEDULED_JOB_OC + ")(" +
                    Constants.JOB_ID_AT + "=" + jobID + "))";
    LDAPEntry jobEntry = null;
    try
    {
      LDAPSearchResults results = conn.search(baseDN, LDAPConnection.SCOPE_SUB,
                                              filter, null, false);
      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          if (jobEntry == null)
          {
            jobEntry = (LDAPEntry) element;
          }
          else
          {
            try
            {
              conn.disconnect();
            } catch (Exception e) {}

            throw new SLAMDException("Multiple entries found for job ID " +
                                     jobID);
          }
        }
        else if (element instanceof LDAPException)
        {
          throw (LDAPException) element;
        }
      }
    }
    catch (LDAPException le)
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}

      throw new SLAMDException("Unable to search the configuration " +
                               "directory:  " + le, le);
    }


    // Make sure that an entry was returned.
    if (jobEntry == null)
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}

      throw new SLAMDException("Unable to find any information about job " +
                               jobID + " in the configuration directory.");
    }


    // Get the statistics from the entry.
    LDAPAttribute attr = jobEntry.getAttribute(Constants.JOB_STAT_TRACKER_AT);
    if (attr == null)
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}

      return null;
    }

    byte[][] values = attr.getByteValueArray();
    if ((values == null) || (values.length == 0))
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}

      return null;
    }


    // Decode the value as an ASN.1 sequence and convert it to a stat tracker
    // array.
    StatTracker[] trackers;
    try
    {
      ASN1Sequence trackerSequence =
           ASN1Element.decode(values[0]).decodeAsSequence();
      trackers = StatEncoder.sequenceToTrackers(trackerSequence);
    }
    catch (ASN1Exception ae)
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}

      throw new SLAMDException("Unable to decode job statistics for job " +
                               jobID + ":  " + ae, ae);
    }


    // Disconnect from the directory and return the stat trackers.
    try
    {
      conn.disconnect();
    } catch (Exception e) {}

    return trackers;
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    System.err.println("Configurable options include:");
    System.err.println("-h {host} -- The SLAMD config directory address");
    System.err.println("-p {port} -- The SLAMD config directory port");
    System.err.println("-D {dn}   -- The SLAMD config directory bind DN");
    System.err.println("-w {pw}   -- The SLAMD config directory bind password");
    System.err.println("-b {dn}   -- The SLAMD config directory base DN");
    System.err.println("-j {job}  -- The job ID for which to dump stats");
  }
}

