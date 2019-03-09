/*
 * Copyright 2008-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2008-2010.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.jobs;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.StatTracker;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;



/**
 * This class provides a simple SLAMD job class that may be used to wait until
 * one or more Directory Server instances are available and respond to LDAP
 * requests.
 */
public class LDAPWaitForDirectoryJobClass
       extends LDAPJobClass
{
  // The DN of the entry to retrieve.
  private static String entryDN;

  // The parameter to use to specify the target entry DN.
  private StringParameter entryDNParameter = new StringParameter("entryDN",
       "Entry to Retrieve",
       "The DN of the entry to retrieve from the server.  If no value is " +
            "provided, then the root DSE will be retrieved.",
       false, "");



  /**
   * Creates a new instance of this job class.
   */
  public LDAPWaitForDirectoryJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "LDAP Wait for Directory";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Wait for an LDAP directory server to become available";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to wait for an LDAP directory server to become " +
      "available and respond to requests.  It will not end until it is able " +
      "to successfully connect to the directory server and retrieve the " +
      "specified entry.  If multiple servers are specified, then it will not " +
      "complete until all servers are found to be available."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int overrideNumClients()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int overrideThreadsPerClient()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected List<Parameter> getNonLDAPParameterStubs()
  {
    return Arrays.asList(
         new PlaceholderParameter(),
         entryDNParameter);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(final String clientID,
                                           final String threadID,
                                           final int collectionInterval)
  {
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean testNonLDAPJobParameters(final ParameterList parameters,
                         final LDAPConnection connection,
                         final ArrayList<String> outputMessages)
  {
    boolean successful = true;

    // Ensure that the target entry DN exists.
    StringParameter entryDNParam =
         parameters.getStringParameter(entryDNParameter.getName());
    if ((entryDNParam != null) && entryDNParam.hasValue())
    {
      try
      {
        String dn = entryDNParam.getStringValue();
        outputMessages.add("Ensuring that entry '" + dn +
                           "' exists....");
        SearchResultEntry e = connection.getEntry(dn);
        if (e == null)
        {
          outputMessages.add("ERROR:  The base entry does not exist.");
          successful = false;
        }
        else
        {
          outputMessages.add("The base entry exists.");
        }
      }
      catch (Exception e)
      {
        successful = false;
        outputMessages.add("Unable to perform the search:  " +
                           stackTraceToString(e));
      }

      outputMessages.add("");
    }

    return successful;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected void initializeClientNonLDAP(final String clientID,
                                         final ParameterList parameters)
  {
    entryDN = "";
    entryDNParameter =
         parameters.getStringParameter(entryDNParameter.getName());
    if ((entryDNParameter != null) && entryDNParameter.hasValue())
    {
      entryDN = entryDNParameter.getStringValue();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(final String clientID, final String threadID,
                               final int collectionInterval,
                               final ParameterList parameters)
  {
    // No implementation is required.
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    SearchRequest searchRequest =
         new SearchRequest(entryDN, SearchScope.BASE,
                           Filter.createPresenceFilter("objectClass"), "1.1");
    searchRequest.setTimeLimitSeconds(5);
    searchRequest.setResponseTimeoutMillis(5000L);


    RoundRobinServerSet serverSet = getServerSet();
    String[] addresses = serverSet.getAddresses();
    int[]    ports     = serverSet.getPorts();

    while (! shouldStop())
    {
      boolean failed = false;
      for (int i=0; i < addresses.length; i++)
      {
        LDAPConnection c;

        try
        {
          c = createConnection(addresses[i], ports[i]);
        }
        catch (LDAPException le)
        {
          failed = true;
          break;
        }

        try
        {
          SearchResult searchResult = c.search(searchRequest);
          if ((! searchResult.getResultCode().equals(ResultCode.SUCCESS)) ||
              (searchResult.getEntryCount() == 0))
          {
            failed = true;
            break;
          }
        }
        catch (LDAPException le)
        {
          failed = true;
          break;
        }
        finally
        {
          c.close();
        }
      }

      if (failed)
      {
        // The server doesn't look like it's ready yet, so wait for five seconds
        // before trying again.
        try
        {
          Thread.sleep(5000L);
        } catch (Exception e) {}
      }
      else
      {
        // There weren't any failures, so the server must be up and the entry
        // exists.
        return;
      }
    }
  }
}
