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
package com.slamd.db;



import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.OptimizingJob;
import com.slamd.job.SingleStatisticOptimizationAlgorithm;
import com.slamd.job.UnknownJobClass;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.server.SLAMDServer;
import com.slamd.server.UploadedFile;
import com.slamd.stat.StatEncoder;
import com.slamd.stat.StatTracker;



/**
 * This class provides a utility that can be used to migrate data from a SLAMD
 * 1.x-style configuration directory server to the new embedded configuration
 * database.  It can migrate all of the data, or just a selected portion of it
 * (at the folder level).
 *
 *
 * @author   Neil A. Wilson
 */
public class DSMigrator
{
  // Indicates whether to use SSL to communicate with the directory server.
  boolean useSSL;

  // The port to use to communicate with the directory server.
  int serverPort;

  // The connection to use to communicate with the directory server.  Note that
  // it may not actually be connected.
  LDAPConnection conn;

  // The date formatter used to parse date information.
  SimpleDateFormat dateFormat;

  // The configuration database used by the SLAMD server.
  SLAMDDB configDB;

  // The SLAMD server instance into which the data will be imported.
  SLAMDServer slamdServer;

  // The base DN for the SLAMD configuration information in the directory
  // server.
  String baseDN;

  // The DN to use to bind to the directory server.
  String bindDN;

  // The password to use to bind to the directory server.
  String bindPW;

  // The address of the directory server.
  String serverAddress;



  /**
   * Creates a new instance of this DS migrator with the specified
   * configuration.
   *
   * @param  slamdServer    The SLAMD server instance into which the data is to
   *                        be imported.
   * @param  serverAddress  The address of the directory server containing the
   *                        data to migrate.
   * @param  serverPort     The port of the directory server.
   * @param  useSSL         Indicates whether to use SSL to communicate with the
   *                        directory server.
   * @param  bindDN         The DN to use to bind to the directory server.
   * @param  bindPW         The password to use to bind to the directory server.
   * @param  baseDN         The base DN for the SLAMD data in the directory
   *                        server.
   *
   * @throws  LDAPException  If the connection is configured to use SSL but a
   *                         problem occurs while trying to perform the SSL
   *                         initialization.
   */
  public DSMigrator(SLAMDServer slamdServer, String serverAddress,
                    int serverPort, boolean useSSL, String bindDN,
                    String bindPW, String baseDN)
         throws LDAPException
  {
    this.slamdServer   = slamdServer;
    this.serverAddress = serverAddress;
    this.serverPort    = serverPort;
    this.useSSL        = useSSL;
    this.bindDN        = bindDN;
    this.bindPW        = bindPW;
    this.baseDN        = baseDN;

    configDB = slamdServer.getConfigDB();

    dateFormat = new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);

    if (useSSL)
    {
      conn = new LDAPConnection(new JSSEBlindTrustSocketFactory());
    }
    else
    {
      conn = new LDAPConnection();
    }
  }



  /**
   * Retrieves a list of the job folders that have been defined in the
   * configuration directory.
   *
   * @return  A list of the job folders that have been defined in the
   *         configuration directory.
   *
   * @throws  LDAPException  If a problem occurs while attempting to interact
   *                         with the configuration directory.
   */
  public String[] getFolderNames()
         throws LDAPException
  {
    try
    {
      conn.connect(3, serverAddress, serverPort, bindDN, bindPW);

      String filter = "(objectClass=" + Constants.JOB_FOLDER_OC + ')';
      String[] attrs = new String[] { Constants.JOB_FOLDER_NAME_AT };
      LDAPSearchResults results = conn.search(baseDN, LDAPConnection.SCOPE_SUB,
                                              filter, attrs, false);

      ArrayList<String> folderList = new ArrayList<String>();
      folderList.add(Constants.FOLDER_NAME_UNCLASSIFIED);

      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          LDAPEntry entry = (LDAPEntry) element;
          String folderName = getValue(entry, Constants.JOB_FOLDER_NAME_AT);
          if (folderName != null)
          {
            folderList.add(folderName);
          }
        }
      }

      String[] folderNames = new String[folderList.size()];
      folderList.toArray(folderNames);
      Arrays.sort(folderNames);
      return folderNames;
    }
    catch (LDAPException le)
    {
      throw le;
    }
    catch (Exception e)
    {
      throw new LDAPException("Unexpected exception caught while retrieving " +
                              "job folder names:  " + e);
    }
    finally
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}
    }
  }



  /**
   * Retrieves a list of the virtual job folders that have been defined in the
   * configuration directory.
   *
   * @return  A list of the virtual job folders that have been defined in the
   *         configuration directory.
   *
   * @throws  LDAPException  If a problem occurs while attempting to interact
   *                         with the configuration directory.
   */
  public String[] getVirtualFolderNames()
         throws LDAPException
  {
    try
    {
      conn.connect(3, serverAddress, serverPort, bindDN, bindPW);

      String filter = "(objectClass=" + Constants.VIRTUAL_JOB_FOLDER_OC + ')';
      String[] attrs = new String[] { Constants.JOB_FOLDER_NAME_AT };
      LDAPSearchResults results = conn.search(baseDN, LDAPConnection.SCOPE_SUB,
                                              filter, attrs, false);

      ArrayList<String> folderList = new ArrayList<String>();
      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          LDAPEntry entry = (LDAPEntry) element;
          String folderName = getValue(entry, Constants.JOB_FOLDER_NAME_AT);
          if (folderName != null)
          {
            folderList.add(folderName);
          }
        }
      }

      String[] folderNames = new String[folderList.size()];
      folderList.toArray(folderNames);
      Arrays.sort(folderNames);
      return folderNames;
    }
    catch (LDAPException le)
    {
      throw le;
    }
    catch (Exception e)
    {
      throw new LDAPException("Unexpected exception caught while retrieving " +
                              "job folder names:  " + e);
    }
    finally
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}
    }
  }



  /**
   * Attempts to migrate all the data contained in the specified folder from the
   * configuration directory into the new database.  This will migrate the
   * folder itself, along with the jobs, optimizing jobs, and uploaded files
   * that it contains.
   *
   * @param  folderName  The name of the job folder to migrate.
   * @param  writer      The print writer that should be used to output
   *                     information about the progress of the migration.
   *
   * @return  <CODE>true</CODE> if the migration was completely successful, or
   *          <CODE>false</CODE> if there may have been problems along the way
   *          that could have interfered with the migration.
   */
  public boolean migrateJobFolder(String folderName, PrintWriter writer)
  {
    try
    {
      // First, establish the connection to the directory server.
      conn.connect(3, serverAddress, serverPort, bindDN, bindPW);


      // Get the DN of the specified folder in the configuration directory.
      if ((folderName == null) || (folderName.length() == 0))
      {
        folderName = Constants.FOLDER_NAME_UNCLASSIFIED;
      }
      LDAPEntry folderEntry = getFolderEntry(folderName);
      if (folderEntry == null)
      {
        writer.println("Unable to find any information about job folder \"" +
                       folderName + "\" in the directory server.");
        return false;
      }

      boolean successful = true;
      String  folderDN   = folderEntry.getDN();


      // Next, see if there already exists a folder with the specified name in
      // the configuration database.
      JobFolder folder = configDB.getFolder(folderName);
      if (folder == null)
      {
        folder = decodeJobFolder(folderName, folderEntry);
        configDB.writeFolder(folder);
        writer.println("Successfully created job folder \"" + folderName +
                       "\".");
      }
      else
      {
        writer.println("A job folder named \"" + folderName +
                       "\" already exists in the configuration database.");
        writer.println("It will be updated to include the specified jobs " +
                       "jobs from the directory server.");
      }


      // Get the set of jobs for the folder and migrate them.
      String[] jobIDs = getJobIDs(folderDN);
      for (int i=0; i < jobIDs.length; i++)
      {
        writer.print("Migrating information for job " + jobIDs[i] + " ... ");

        try
        {
          Job job = getJob(folderName, folderDN, jobIDs[i]);
          configDB.writeJob(job);
          writer.println("Success");
        }
        catch (Exception e)
        {
          writer.println("Failed:  " + e);
          successful = false;
        }
      }


      // Get the set of optimizing jobs for the folder and migrate them.
      String[] optimizingJobIDs = getOptimizingJobIDs(folderDN);
      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        writer.print("Migrating information for optimizing job " +
                     optimizingJobIDs[i] +  " ... ");

        try
        {
          OptimizingJob optimizingJob = getOptimizingJob(folderName, folderDN,
                                                         optimizingJobIDs[i]);
          configDB.writeOptimizingJob(optimizingJob);
          writer.println("Success");
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          writer.println("Failed:  " + e);
          successful = false;
        }
      }


      // Get the set of uploaded files for the folder and migrate them.
      String[] fileNames = getUploadedFileNames(folderDN);
      for (int i=0; i < fileNames.length; i++)
      {
        writer.print("Migrating information for uploaded file " + fileNames[i] +
                     " ... ");

        try
        {
          UploadedFile file = getUploadedFile(folderDN, fileNames[i]);
          configDB.writeUploadedFile(file, folderName);
          writer.println("Success");
        }
        catch (Exception e)
        {
          writer.println("Failed:  " + e);
          successful = false;
        }
      }


      return successful;
    }
    catch (Exception e)
    {
      writer.println("Unable to complete migration of job folder " +
                     folderName + " because an unexpected error occurred:");
      writer.println(JobClass.stackTraceToString(e));
      return false;
    }
    finally
    {
      try
      {
        conn.disconnect();
      } catch (Exception e) {}
    }
  }



  /**
   * Retrieves the DN of the entry in the directory server that contains
   * information about the specified job folder.
   *
   * @param  folderName  The name of the job folder for which to obtain the DN.
   *
   * @return  The DN of the requested job folder, or <CODE>null</CODE> if it
   *          could not be found.
   *
   * @throws  LDAPException  If a problem occurs while making the determination.
   */
  private LDAPEntry getFolderEntry(String folderName)
          throws LDAPException
  {
    LDAPEntry folderEntry = null;


    // First, check to see if this should be for the base "unclassified" folder.
    // If so, then treat it specially.
    if ((folderName == null) || (folderName.length() == 0) ||
        folderName.equals(Constants.FOLDER_NAME_UNCLASSIFIED))
    {
      String filter = "(&(objectClass=" + Constants.CONFIG_PARAMETER_OC + ")(" +
                      Constants.PARAMETER_NAME_AT + '=' +
                      Constants.PARAM_CONFIG_SCHEDULED_JOB_BASE + "))";
      LDAPSearchResults results = conn.search(baseDN, LDAPConnection.SCOPE_SUB,
                                              filter, null, false);
      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          LDAPEntry entry = (LDAPEntry) element;
          String folderEntryDN = getValue(entry, Constants.PARAMETER_VALUE_AT);
          folderEntry = conn.read(folderEntryDN);
        }
      }

      if (folderEntry == null)
      {
        folderEntry = conn.read(baseDN);
      }

      return folderEntry;
    }


    // If we've gotten here, then it was not for the base folder.  Just find the
    // folder entry the easy way.
    String filter = "(&(objectClass=" + Constants.JOB_FOLDER_OC + ")(" +
                    Constants.JOB_FOLDER_NAME_AT + '=' + folderName + "))";
    LDAPSearchResults results = conn.search(baseDN, LDAPConnection.SCOPE_SUB,
                                            filter, null, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        folderEntry = ((LDAPEntry) element);
      }
    }

    return folderEntry;
  }



  /**
   * Decodes the provided LDAP entry as a job folder.
   *
   * @param  folderName   The name to use for the job folder.
   * @param  folderEntry  The entry to decode as a job folder.
   *
   * @return  The job folder decoded from the provided entry.
   *
   * @throws  DecodeException  If a problem occurred while trying to decode the
   *                           provided entry as a job folder.
   */
  private JobFolder decodeJobFolder(String folderName, LDAPEntry folderEntry)
          throws DecodeException
  {
    boolean displayInReadOnly = false;
    String valueStr = getValue(folderEntry, Constants.DISPLAY_IN_READ_ONLY_AT);
    if (valueStr != null)
    {
      displayInReadOnly = (valueStr.equalsIgnoreCase("true") ||
                           valueStr.equalsIgnoreCase("yes") ||
                           valueStr.equalsIgnoreCase("on") ||
                           valueStr.equalsIgnoreCase("1"));
    }

    String description = getValue(folderEntry,
                                  Constants.JOB_FOLDER_DESCRIPTION_AT);

    return new JobFolder(folderName, displayInReadOnly, false, null, null,
                         description, null, null, null, null);
  }



  /**
   * Retrieves the job IDs of the jobs contained in the specified folder in the
   * directory server.
   *
   * @param  folderDN  The DN of the job folder entry with which the jobs should
   *                   be associated.
   *
   * @return  The job IDs of the jobs contained in the specified folder.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   */
  private String[] getJobIDs(String folderDN)
          throws LDAPException
  {
    ArrayList<String> idList = new ArrayList<String>();

    String filter = "(objectClass=" + Constants.SCHEDULED_JOB_OC + ')';
    String[] attrs = { Constants.JOB_ID_AT };
    LDAPSearchResults results = conn.search(folderDN, LDAPConnection.SCOPE_ONE,
                                            filter, attrs, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        String jobID = getValue((LDAPEntry) element, Constants.JOB_ID_AT);
        if (jobID != null)
        {
          idList.add(jobID);
        }
      }
    }

    String[] jobIDs = new String[idList.size()];
    idList.toArray(jobIDs);
    return jobIDs;
  }



  /**
   * Retrieves information about the specified job from the directory server.
   *
   * @param  folderName  The name of the job folder with which the job is
   *                     associated.
   * @param  folderDN    The DN of the job folder with which the job is
   *                     associated.
   * @param  jobID       The job ID of the job to retrieve.
   *
   * @return  The requested job from the configuration directory.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           job data.
   */
  private Job getJob(String folderName, String folderDN, String jobID)
          throws LDAPException, DecodeException
  {
    LDAPEntry entry = null;
    String filter = "(&(objectClass=" + Constants.SCHEDULED_JOB_OC + ")(" +
                    Constants.JOB_ID_AT + '=' + jobID + "))";
    LDAPSearchResults results = conn.search(folderDN, LDAPConnection.SCOPE_SUB,
                                            filter, null, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        entry = (LDAPEntry) element;
      }
    }

    if (entry == null)
    {
      throw new DecodeException("Could not retrieve the job entry from the " +
                                "directory server.");
    }


    // Get the job class name
    String className = getValue(entry, Constants.JOB_CLASS_NAME_AT);
    if (className == null)
    {
      throw new DecodeException("Entry has no job class name");
    }

    // Get the job description
    String jobDescription = getValue(entry, Constants.JOB_DESCRIPTION_AT);

    // Get the number of clients
    int numClients = -1;
    String numClientStr = getValue(entry, Constants.JOB_NUM_CLIENTS_AT);
    if (numClientStr == null)
    {
      throw new DecodeException("Entry has no number of clients");
    }
    else
    {
      try
      {
        numClients = Integer.parseInt(numClientStr);
      }
      catch (NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
        throw new DecodeException("Invalid number of clients:  " + numClientStr,
                                  nfe);
      }
    }

    // Get whether to wait for clients to be available
    boolean waitForClients = false;
    String waitStr = getValue(entry, Constants.JOB_WAIT_FOR_CLIENTS_AT);
    if ((waitStr != null) && (waitStr.length() > 0))
    {
      waitForClients = (waitStr.equalsIgnoreCase("true") ||
                        waitStr.equalsIgnoreCase("on") ||
                        waitStr.equalsIgnoreCase("yes") ||
                        waitStr.equals("1"));
    }

    // Get whether to automatically monitor any clients used.
    boolean monitorClientsIfAvailable = false;
    String monitorStr = getValue(entry,
                             Constants.JOB_MONITOR_CLIENTS_IF_AVAILABLE_AT);
    if ((monitorStr != null) && (monitorStr.length() > 0))
    {
      monitorClientsIfAvailable = (monitorStr.equalsIgnoreCase("true") ||
                                   monitorStr.equalsIgnoreCase("on") ||
                                   monitorStr.equalsIgnoreCase("yes") ||
                                   monitorStr.equals("1"));
    }

    // Get the set of requested clients.
    String[] requestedClients = null;
    String clientsStr = getValue(entry, Constants.JOB_CLIENTS_AT);
    if ((clientsStr != null) && (clientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(clientsStr);
      while (tokenizer.hasMoreTokens())
      {
        clientList.add(tokenizer.nextToken());
      }

      if (! clientList.isEmpty())
      {
        requestedClients = new String[clientList.size()];
        clientList.toArray(requestedClients);
      }
    }

    // Get the set of resource monitor clients.
    String[] monitorClients = null;
    clientsStr = getValue(entry, Constants.JOB_MONITOR_CLIENTS_AT);
    if ((clientsStr != null) && (clientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(clientsStr);
      while (tokenizer.hasMoreTokens())
      {
        clientList.add(tokenizer.nextToken());
      }

      if (! clientList.isEmpty())
      {
        monitorClients = new String[clientList.size()];
        clientList.toArray(monitorClients);
      }
    }

    // Get the number of threads per client
    int threadsPerClient = -1;
    String numThreadStr = getValue(entry, Constants.JOB_THREADS_PER_CLIENT_AT);
    if (numThreadStr == null)
    {
      throw new DecodeException("Entry has no number of threads per client");
    }
    else
    {
      try
      {
        threadsPerClient = Integer.parseInt(numThreadStr);
      }
      catch (NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
        throw new DecodeException("Invalid number of threads per client:  " +
                                  numThreadStr, nfe);
      }
    }

    // Get the thread startup delay
    int threadStartupDelay = 0;
    String threadDelayStr =
         getValue(entry, Constants.JOB_THREAD_STARTUP_DELAY_AT);
    if (threadDelayStr != null)
    {
      try
      {
        threadStartupDelay = Integer.parseInt(threadDelayStr);
      } catch (Exception e) {}
    }

    // Get the set of dependencies.
    String[] dependencies = getValues(entry, Constants.JOB_DEPENDENCY_AT);

    // Get the set of notify addresses.
    String[] notifyAddresses = getValues(entry,
                                         Constants.JOB_NOTIFY_ADDRESS_AT);

    // Get the job state
    int jobState = Constants.JOB_STATE_UNKNOWN;
    String jobStateStr = getValue(entry, Constants.JOB_STATE_AT);
    if (jobStateStr != null)
    {
      try
      {
        jobState = Integer.parseInt(jobStateStr);
      } catch (NumberFormatException nfe) {}
    }

    // Determine whether this job should be displayed in read-only mode.
    boolean displayInReadOnlyMode = false;
    String displayStr = getValue(entry, Constants.DISPLAY_IN_READ_ONLY_AT);
    if (displayStr != null)
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }

    // Get the start time
    Date startTime = null;
    String startTimeStr = getValue(entry, Constants.JOB_START_TIME_AT);
    if (startTimeStr != null)
    {
      try
      {
        startTime = dateFormat.parse(startTimeStr);
      } catch (Exception e) {}
    }
    if (startTime == null)
    {
      throw new DecodeException("Could not retrieve job start time");
    }

    // Get the stop time
    Date stopTime = null;
    String stopTimeStr = getValue(entry, Constants.JOB_STOP_TIME_AT);
    if (stopTimeStr != null)
    {
      try
      {
        stopTime = dateFormat.parse(stopTimeStr);
      } catch (Exception e) {}
    }

    // Get the duration
    int duration = -1;
    String durationStr = getValue(entry, Constants.JOB_DURATION_AT);
    if (durationStr != null)
    {
      try
      {
        duration = Integer.parseInt(durationStr);
      } catch (NumberFormatException nfe) {}
    }

    // Get the optimizing job ID.
    String optimizingJobID = getValue(entry, Constants.OPTIMIZING_JOB_ID_AT);

    ParameterList pList = new ParameterList();
    String[] paramStrs = getValues(entry, Constants.JOB_PARAM_AT);
    if (paramStrs != null)
    {
      try
      {
        Job stub = new Job(slamdServer, className);
        ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        ParameterList stubParams = stub.getClientSideParameterStubs().clone();
        for (int i=0; i < paramStrs.length; i++)
        {
          int delimPos =
               paramStrs[i].indexOf(Constants.JOB_PARAM_DELIMITER_STRING);
          String paramName = paramStrs[i].substring(0, delimPos);
          String paramValue =
               paramStrs[i].substring(delimPos +
                    Constants.JOB_PARAM_DELIMITER_STRING.length());
          Parameter p = stubParams.getParameter(paramName);
          if (p != null)
          {
            p.setValue(paramValue);
            paramList.add(p);
          }
        }

        Parameter[] pArray = new Parameter[paramList.size()];
        for (int i=0; i < pArray.length; i++)
        {
          pArray[i] = paramList.get(i);
        }
        pList.setParameters(pArray);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Error getting job parameters:  " + e, e);
      }
    }


    // Get the stat tracker information
    StatTracker[] statTrackers = new StatTracker[0];
    LDAPAttribute attr = entry.getAttribute(Constants.JOB_STAT_TRACKER_AT);
    if (attr != null)
    {
      byte[][] values = attr.getByteValueArray();
      if ((values != null) && (values.length > 0))
      {
        try
        {
          ASN1Sequence trackerSequence =
               ASN1Element.decode(values[0]).decodeAsSequence();
          statTrackers = StatEncoder.sequenceToTrackers(trackerSequence);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          throw new DecodeException("Error getting stat trackers:  " + e, e);
        }
      }
    }

    // Get the resource monitor statistics.
    StatTracker[] monitorTrackers = new StatTracker[0];
    attr = entry.getAttribute(Constants.JOB_MONITOR_STAT_AT);
    if (attr != null)
    {
      byte[][] values = attr.getByteValueArray();
      if ((values != null) && (values.length > 0))
      {
        try
        {
          ASN1Sequence trackerSequence =
               ASN1Element.decode(values[0]).decodeAsSequence();
          monitorTrackers = StatEncoder.sequenceToTrackers(trackerSequence);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          throw new DecodeException("Error getting monitor statistics:  " + e,
                                    e);
        }
      }
    }


    // Get the collection interval information
    int collectionInterval = Constants.DEFAULT_COLLECTION_INTERVAL;
    String intervalStr = getValue(entry, Constants.JOB_COLLECTION_INTERVAL_AT);
    if (intervalStr != null)
    {
      try
      {
        collectionInterval = Integer.parseInt(intervalStr);
      }
      catch (NumberFormatException nfe)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(nfe));
        throw new DecodeException("Invalid collection interval:  " +
                                  intervalStr, nfe);
      }
    }


    // Get the log messages information
    ArrayList<String> messagesList = new ArrayList<String>();
    String logMessages = getValue(entry, Constants.JOB_LOG_MESSAGES_AT);
    if (logMessages != null)
    {
      StringTokenizer tokenizer =
           new StringTokenizer(logMessages,
                               Constants.JOB_LOG_MESSAGES_DELIMITER);
      while (tokenizer.hasMoreTokens())
      {
        messagesList.add(tokenizer.nextToken());
      }
    }


    // Get the comments information
    String comments = getValue(entry, Constants.JOB_COMMENTS_AT);


    // Get the actual start time
    Date actualStartTime = null;
    String actualStartTimeStr = getValue(entry,
                                         Constants.JOB_ACTUAL_START_TIME_AT);
    if (actualStartTimeStr != null)
    {
      try
      {
        actualStartTime = dateFormat.parse(actualStartTimeStr);
      } catch (Exception e) {}
    }

    // Get the actual stop time
    Date actualStopTime = null;
    String actualStopTimeStr = getValue(entry,
                                    Constants.JOB_ACTUAL_STOP_TIME_AT);
    if (actualStopTimeStr != null)
    {
      try
      {
        actualStopTime = dateFormat.parse(actualStopTimeStr);
      } catch (Exception e) {}
    }


    // Get the actual duration.  Try to read it from the entry first, and if
    // it's not there, then read it from the
    int actualDuration = -1;
    String actualDurationStr = getValue(entry,
                                    Constants.JOB_ACTUAL_DURATION_AT);
    if (actualDurationStr != null)
    {
      try
      {
        actualDuration = Integer.parseInt(actualDurationStr);
      } catch (Exception e) {}
    }

    if ((actualDuration == -1) && (actualStartTime != null) &&
        (actualStopTime != null))
    {
      actualDuration =
           (int) (actualStopTime.getTime() - actualStartTime.getTime()) / 1000;
    }


    // Create the job
    Job job = null;
    try
    {
      job = new Job(slamdServer, className, numClients, threadsPerClient,
                    threadStartupDelay, startTime, stopTime, duration,
                    collectionInterval, pList, displayInReadOnlyMode);
      job.setJobID(jobID);
      job.setFolderName(folderName);
      job.setWaitForClients(waitForClients);
      job.setMonitorClientsIfAvailable(monitorClientsIfAvailable);
      job.setRequestedClients(requestedClients);
      job.setResourceMonitorClients(monitorClients);
      job.setDependencies(dependencies);
      job.setJobDescription(jobDescription);
      job.setJobState(jobState);
      job.setActualStartTime(actualStartTime);
      job.setActualStopTime(actualStopTime);
      job.setActualDuration(actualDuration);
      job.setStatTrackers(statTrackers);
      job.setResourceStatTrackers(monitorTrackers);
      job.setLogMessages(messagesList);
      job.setNotifyAddresses(notifyAddresses);
      job.setJobComments(comments);
      job.setOptimizingJobID(optimizingJobID);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DecodeException("Error creating job:  " + e, e);
    }

    return job;
  }



  /**
   * Retrieves the optimizing job IDs of the optimizing jobs contained in the
   * specified folder in the directory server.
   *
   * @param  folderDN  The DN of the job folder entry with which the optimizing
   *                   jobs should be associated.
   *
   * @return  The job IDs of the optimizing jobs contained in the specified
   *          folder.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   */
  private String[] getOptimizingJobIDs(String folderDN)
          throws LDAPException
  {
    ArrayList<String> idList = new ArrayList<String>();

    String filter = "(objectClass=" + Constants.OPTIMIZING_JOB_OC + ')';
    String[] attrs = { Constants.OPTIMIZING_JOB_ID_AT };
    LDAPSearchResults results = conn.search(folderDN, LDAPConnection.SCOPE_ONE,
                                            filter, attrs, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        String optimizingJobID =
             getValue((LDAPEntry) element, Constants.OPTIMIZING_JOB_ID_AT);
        if (optimizingJobID != null)
        {
          idList.add(optimizingJobID);
        }
      }
    }

    String[] optimizingJobIDs = new String[idList.size()];
    idList.toArray(optimizingJobIDs);
    return optimizingJobIDs;
  }



  /**
   * Retrieves information about the specified optimizing job from the directory
   * server.
   *
   * @param  folderName       The name of the job folder with which the
   *                          optimizing job is associated.
   * @param  folderDN         The DN of the job folder with which the optimizing
   *                          job is associated.
   * @param  optimizingJobID  The optimizing job ID of the optimizing job to
   *                          retrieve.
   *
   * @return  The requested job from the configuration directory.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           optimizing job data.
   */
  private OptimizingJob getOptimizingJob(String folderName, String folderDN,
                                         String optimizingJobID)
          throws LDAPException, DecodeException
  {
    LDAPEntry entry = null;
    String filter = "(&(objectClass=" + Constants.OPTIMIZING_JOB_OC + ")(" +
                    Constants.OPTIMIZING_JOB_ID_AT + '=' + optimizingJobID +
                    "))";
    LDAPSearchResults results = conn.search(folderDN, LDAPConnection.SCOPE_ONE,
                                            filter, null, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        entry = (LDAPEntry) element;
      }
    }

    if (entry == null)
    {
      throw new DecodeException("Could not retrieve the optimizing job entry " +
                                "from the directory server.");
    }


    // OK.  This is kind of nasty.  The first implementation of optimizing jobs
    // was based on cloning an existing job, and as such there was a base job.
    // Subsequently, it was changed so that the optimizing job had its own
    // job class and parameter list, so the base job was no longer used.  This
    // code needs to handle either case, but since it is possible to get a job
    // class and parameter list from a base job, then we'll always use that
    // version of the constructor.
    JobClass      jobClass   = null;
    ParameterList parameters = null;
    String        baseJobID  = getValue(entry, Constants.BASE_JOB_ID_AT);
    if ((baseJobID != null) && (baseJobID.length() > 0))
    {
      Job baseJob = getJob(folderName, baseDN, baseJobID);
      jobClass    = baseJob.getJobClass();
      parameters  = baseJob.getParameterList().clone();
    }
    else
    {
      String jobClassName = getValue(entry, Constants.JOB_CLASS_NAME_AT);
      if ((jobClassName == null) || (jobClassName.length() == 0))
      {
        throw new DecodeException("Entry has neither a base job ID nor a job " +
                                  "class name.");
      }

      try
      {
        jobClass = slamdServer.getOrLoadJobClass(jobClassName);
      } catch (Exception e) {}
      if (jobClass == null)
      {
        throw new DecodeException("Entry references unknown job class \"" +
                                  jobClassName + '"');
      }

      parameters = new ParameterList();
      String[] paramStrs = getValues(entry, Constants.JOB_PARAM_AT);
      if (paramStrs != null)
      {
        try
        {
          Job stub = new Job(slamdServer, jobClassName);
          ArrayList<Parameter> paramList = new ArrayList<Parameter>();
          ParameterList stubParams = stub.getParameterStubs().clone();
          for (int i=0; i < paramStrs.length; i++)
          {
            int delimPos =
                 paramStrs[i].indexOf(Constants.JOB_PARAM_DELIMITER_STRING);
            String paramName = paramStrs[i].substring(0, delimPos);
            String paramValue =
                 paramStrs[i].substring(delimPos +
                      Constants.JOB_PARAM_DELIMITER_STRING.length());
            Parameter p = stubParams.getParameter(paramName);
            if (p != null)
            {
              p.setValue(paramValue);
              paramList.add(p);
            }
          }

          Parameter[] pArray = new Parameter[paramList.size()];
          for (int i=0; i < pArray.length; i++)
          {
            pArray[i] = paramList.get(i);
          }
          parameters.setParameters(pArray);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          throw new DecodeException("Error getting job parameters:  " + e, e);
        }
      }
    }

    boolean isUnknownJobClass = (jobClass instanceof UnknownJobClass);


    // Get the description, if there is one.
    String description = getValue(entry, Constants.JOB_DESCRIPTION_AT);


    // Get the flag that indicates whether to include the thread count in the
    // job description.
    boolean includeThreads = false;
    String includeThreadStr =
         getValue(entry, Constants.JOB_INCLUDE_THREAD_IN_DESCRIPTION_AT);
    if (includeThreadStr != null)
    {
      includeThreads = (includeThreadStr.equalsIgnoreCase("true") ||
                        includeThreadStr.equalsIgnoreCase("yes") ||
                        includeThreadStr.equalsIgnoreCase("on") ||
                        includeThreadStr.equalsIgnoreCase("1"));
    }


    // Determine whether this job should be displayed in read-only mode.
    boolean displayInReadOnlyMode = false;
    String displayStr = getValue(entry, Constants.DISPLAY_IN_READ_ONLY_AT);
    if (displayStr != null)
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }


    // Get the start time.
    Date startTime = null;
    String startTimeStr = getValue(entry, Constants.JOB_START_TIME_AT);
    if (startTimeStr == null)
    {
      throw new DecodeException("Entry has no start time.");
    }
    else
    {
      try
      {
        startTime = dateFormat.parse(startTimeStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the start time.", e);
      }
    }


    // Get the duration.
    int duration = -1;
    String durationStr = getValue(entry, Constants.JOB_DURATION_AT);
    if (durationStr != null)
    {
      try
      {
        duration = Integer.parseInt(durationStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the job duration.", e);
      }
    }


    // Get the delay between iterations.
    int delay = 0;
    String delayStr = getValue(entry, Constants.DELAY_BETWEEN_ITERATIONS_AT);
    if (delayStr != null)
    {
      try
      {
        delay = Integer.parseInt(delayStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the delay between job " +
                                  "iterations.", e);
      }
    }


    // Get the number of clients.
    int numClients = -1;
    String numClientsStr = getValue(entry, Constants.JOB_NUM_CLIENTS_AT);
    if (numClientsStr == null)
    {
      throw new DecodeException("Entry has no number of clients.");
    }
    else
    {
      try
      {
        numClients = Integer.parseInt(numClientsStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the number of clients.", e);
      }
    }


    // Get whether to automatically monitor any clients used.
    boolean monitorClientsIfAvailable = false;
    String monitorStr = getValue(entry,
                             Constants.JOB_MONITOR_CLIENTS_IF_AVAILABLE_AT);
    if ((monitorStr != null) && (monitorStr.length() > 0))
    {
      monitorClientsIfAvailable = (monitorStr.equalsIgnoreCase("true") ||
                                   monitorStr.equalsIgnoreCase("on") ||
                                   monitorStr.equalsIgnoreCase("yes") ||
                                   monitorStr.equals("1"));
    }


    // Get the set of requested clients.
    String[] requestedClients = null;
    String clientsStr = getValue(entry, Constants.JOB_CLIENTS_AT);
    if ((clientsStr != null) && (clientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(clientsStr);
      while (tokenizer.hasMoreTokens())
      {
        clientList.add(tokenizer.nextToken());
      }
      requestedClients = new String[clientList.size()];
      clientList.toArray(requestedClients);
    }


    // Get the set of resource monitor clients.
    String[] monitorClients = null;
    clientsStr = getValue(entry, Constants.JOB_MONITOR_CLIENTS_AT);
    if ((clientsStr != null) && (clientsStr.length() > 0))
    {
      ArrayList<String> clientList = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(clientsStr);
      while (tokenizer.hasMoreTokens())
      {
        clientList.add(tokenizer.nextToken());
      }
      monitorClients = new String[clientList.size()];
      clientList.toArray(monitorClients);
    }


    // Get the minimum number of threads to use.
    int minThreads = 1;
    String minThreadStr = getValue(entry, Constants.JOB_MIN_THREADS_AT);
    if (minThreadStr == null)
    {
      throw new DecodeException("Entry has no minimum number of threads.");
    }
    else
    {
      try
      {
        minThreads = Integer.parseInt(minThreadStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the minimum number of " +
                                  "threads.", e);
      }
    }


    // Get the maximum number of threads to use, if specified.
    int maxThreads = -1;
    String maxThreadStr = getValue(entry, Constants.JOB_MAX_THREADS_AT);
    if (maxThreadStr != null)
    {
      try
      {
        maxThreads = Integer.parseInt(maxThreadStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the maximum number of " +
                                  "threads.", e);
      }
    }


    // Get the thread increment.
    int threadIncrement = 0;
    String incrementStr = getValue(entry, Constants.JOB_THREAD_INCREMENT_AT);
    if (incrementStr == null)
    {
      throw new DecodeException("Entry has no thread increment.");
    }
    else
    {
      try
      {
        threadIncrement = Integer.parseInt(incrementStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the thread increment.", e);
      }
    }


    // Get the collection interval.
    int collectionInterval = -1;
    String intervalStr = getValue(entry, Constants.JOB_COLLECTION_INTERVAL_AT);
    if (intervalStr == null)
    {
      throw new DecodeException("Entry has no collection interval.");
    }
    else
    {
      try
      {
        collectionInterval = Integer.parseInt(intervalStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse collection interval.", e);
      }
    }


    // Get the maximum number of consecutive non-improving iterations.
    int maxNonImproving = 1;
    String nonImprovingStr =
         getValue(entry, Constants.JOB_MAX_NON_IMPROVING_ITERATIONS_AT);
    if (nonImprovingStr == null)
    {
      throw new DecodeException("Entry has no maximum number of " +
                                "non-improving iterations.");
    }
    else
    {
      try
      {
        maxNonImproving = Integer.parseInt(nonImprovingStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse number of non-improving " +
                                  "iterations.", e);
      }
    }


    // Get the addresses to notify.
    String[] notifyAddresses =
         getValues(entry, Constants.JOB_NOTIFY_ADDRESS_AT);


    // Determine if the best iteration should be re-run.
    boolean reRunBestIteration = false;
    String reRunBestStr =
         getValue(entry, Constants.OPTIMIZING_JOB_RERUN_BEST_ITERATION_AT);
    if ((reRunBestStr != null) && (reRunBestStr.length() > 0))
    {
      reRunBestStr = reRunBestStr.toLowerCase();
      reRunBestIteration = (reRunBestStr.equals("true") ||
                            reRunBestStr.equals("yes") ||
                            reRunBestStr.equals("on") ||
                            reRunBestStr.equals("1"));
    }


    // Determine what the duration should be when re-running the best iteration.
    int reRunDuration = -1;
    String reRunDurationStr =
         getValue(entry, Constants.OPTIMIZING_JOB_RERUN_DURATION_AT);
    if ((reRunDurationStr != null) && (reRunDurationStr.length() > 0))
    {
      try
      {
        reRunDuration = Integer.parseInt(reRunDurationStr);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse the duration to use when " +
                                  "re-running the best iteration.", e);
      }
    }


    // Get the name of the statistic to optimize and the optimization type.
    // These are for legacy purposes and are no longer required.
    String optimizeStat = getValue(entry, Constants.JOB_OPTIMIZATION_STAT_AT);
    String optimizeType = getValue(entry, Constants.JOB_OPTIMIZATION_TYPE_AT);


    // Determine the optimization algorithm to use.
    boolean legacyOptimizingJob = false;
    OptimizationAlgorithm optimizationAlgorithm;
    String optimizationAlgorithmName =
         getValue(entry, Constants.OPTIMIZATION_ALGORITHM_AT);
    if (optimizationAlgorithmName == null)
    {
      // We should assume that this is a legacy optimizing job, which uses the
      // default optimization algorithm but a different way of storing the
      // optimization settings.
      legacyOptimizingJob = true;
      optimizationAlgorithm = new SingleStatisticOptimizationAlgorithm();
    }
    else
    {
      optimizationAlgorithm =
           slamdServer.getOptimizationAlgorithm(optimizationAlgorithmName);
      if (optimizationAlgorithm == null)
      {
        // This is not necessarily fatal, since it is possible that the job
        // used an algorithm that is no longer available for users to schedule.
        try
        {
          Class<?> algorithmClass =
               Constants.classForName(optimizationAlgorithmName);
          optimizationAlgorithm =
               (OptimizationAlgorithm) algorithmClass.newInstance();
        }
        catch (Exception e)
        {
          // This is fatal, because we couldn't load the optimization algorithm
          // class anywhere.
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                                 "Unable to load the optimization algorithm " +
                                 "class \"" + optimizationAlgorithmName +
                                 "\" -- " + e);
          throw new DecodeException("Unable to load the optimization " +
                                    "algorithm class \"" +
                                    optimizationAlgorithmName + "\" -- " + e);
        }
      }
    }


    // Get the parameters for the optimization algorithm.
    ParameterList optimizationParameters = null;
    if (! legacyOptimizingJob)
    {
      String[] paramStrs =
           getValues(entry, Constants.OPTIMIZATION_ALGORITHM_PARAMETER_AT);
      optimizationParameters = optimizationAlgorithm.
           getOptimizationAlgorithmParameterStubs(jobClass).clone();
      for (int i=0; i < paramStrs.length; i++)
      {
        int delimPos =
                 paramStrs[i].indexOf(Constants.JOB_PARAM_DELIMITER_STRING);
        if (delimPos > 0)
        {
          String name = paramStrs[i].substring(0, delimPos);
          String valueStr = paramStrs[i].substring(delimPos +
                                 Constants.JOB_PARAM_DELIMITER_STRING.length());
          Parameter p = optimizationParameters.getParameter(name);
          if (p != null)
          {
            try
            {
              p.setValueFromString(valueStr);
            } catch (Exception e) {}
          }
        }
      }
    }


    // Now we have enough to create the optimizing job, so do it.
    OptimizingJob optimizingJob =
         new OptimizingJob(slamdServer, optimizingJobID, optimizationAlgorithm,
                           jobClass, folderName, description, includeThreads,
                           startTime, duration, delay, numClients,
                           requestedClients, monitorClients,
                           monitorClientsIfAvailable, minThreads, maxThreads,
                           threadIncrement, collectionInterval, maxNonImproving,
                           notifyAddresses, reRunBestIteration, reRunDuration,
                           parameters, displayInReadOnlyMode);
    if (legacyOptimizingJob)
    {
      int optType = SingleStatisticOptimizationAlgorithm.OPTIMIZE_TYPE_MAXIMIZE;
      if (optimizeType.equalsIgnoreCase(Constants.OPTIMIZE_TYPE_MINIMIZE))
      {
        optType = SingleStatisticOptimizationAlgorithm.OPTIMIZE_TYPE_MINIMIZE;
      }

      ((SingleStatisticOptimizationAlgorithm) optimizationAlgorithm).
           initializeLegacyJob(optimizingJob, optimizeStat, optType);
    }
    else
    {
      if (! isUnknownJobClass)
      {
        try
        {
          optimizationAlgorithm.initializeOptimizationAlgorithm(
               optimizingJob, optimizationParameters);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                                 "Unable to initialize optimization " +
                                 "algorithm for optimizing job \"" +
                                 optimizingJobID + "\" -- " + e);
          throw new DecodeException("Unable to initialize optimization " +
                                    "algorithm for optimizing job \"" +
                                    optimizingJobID +  "\" -- " + e);
        }
      }
    }


    // Set the actual start time, if available.
    startTimeStr = getValue(entry, Constants.JOB_ACTUAL_START_TIME_AT);
    if ((startTimeStr != null) && (startTimeStr.length() > 0))
    {
      try
      {
        optimizingJob.setActualStartTime(dateFormat.parse(startTimeStr));
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse actual start time.", e);
      }
    }


    // Set the actual stop time, if available.
    String stopTimeStr = getValue(entry, Constants.JOB_ACTUAL_STOP_TIME_AT);
    if ((stopTimeStr != null) && (stopTimeStr.length() > 0))
    {
      try
      {
        optimizingJob.setActualStopTime(dateFormat.parse(stopTimeStr));
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse actual stop time.", e);
      }
    }


    // Set the stop reason, if available.
    String stopReason = getValue(entry, Constants.JOB_STOP_REASON_AT);
    if ((stopReason != null) && (stopReason.length() > 0))
    {
      optimizingJob.setStopReason(stopReason);
    }


    // Set the job state.
    String jobStateStr = getValue(entry, Constants.JOB_STATE_AT);
    if (jobStateStr == null)
    {
      throw new DecodeException("Entry has no job state.");
    }
    else
    {
      try
      {
        optimizingJob.setJobState(Integer.parseInt(jobStateStr));
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DecodeException("Unable to parse job state.", e);
      }
    }


    // Set the list of associated jobs.
    String[] associatedJobIDs = getValues(entry, Constants.JOB_ID_AT);
    if ((associatedJobIDs != null) && (associatedJobIDs.length > 0))
    {
      Job[] associatedJobs = new Job[associatedJobIDs.length];
      for (int i=0; i < associatedJobs.length; i++)
      {
        associatedJobs[i] = getJob(folderName, baseDN, associatedJobIDs[i]);
        if (associatedJobs[i] == null)
        {
          throw new DecodeException("Unable to retrieve associated job " +
                                    associatedJobIDs[i]);
        }

        try
        {
          configDB.writeJob(associatedJobs[i]);
        } catch (Exception e) {}
      }
      optimizingJob.setAssociatedJobs(associatedJobs);
    }


    // See if a re-run iteration has been specified.
    Job reRunIteration = null;
    String reRunIterationID =
         getValue(entry, Constants.OPTIMIZING_JOB_RERUN_ITERATION_AT);
    if ((reRunIterationID != null) && (reRunIterationID.length() > 0))
    {
      try
      {
        reRunIteration = getJob(folderName, baseDN, reRunIterationID);
        optimizingJob.setReRunIteration(reRunIteration);

        try
        {
          configDB.writeJob(reRunIteration);
        } catch (Exception e) {}
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "WARNING:  Unable to retrieve job " +
                               reRunIterationID +
                               " as the best iteration of optimizing job " +
                               optimizingJobID);
      }
    }


    // Set the list of dependencies.
    String[] dependencies = getValues(entry, Constants.JOB_DEPENDENCY_AT);
    if ((dependencies != null) && (dependencies.length > 0))
    {
      optimizingJob.setDependencies(dependencies);
    }


    return optimizingJob;
  }



  /**
   * Retrieves the names of the uploaded files contained in the specified folder
   * in the directory server.
   *
   * @param  folderDN  The DN of the job folder entry with which the uploaded
   *                   files should be associated.
   *
   * @return  The names of the uploaded files contained in the specified folder.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   */
  public String[] getUploadedFileNames(String folderDN)
         throws LDAPException
  {
    ArrayList<String> fileList = new ArrayList<String>();

    String filter = "(objectClass=" + Constants.UPLOADED_FILE_OC + ')';
    String[] attrs = { Constants.UPLOADED_FILE_NAME_AT };
    LDAPSearchResults results = conn.search(folderDN, LDAPConnection.SCOPE_ONE,
                                            filter, attrs, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        String optimizingJobID =
             getValue((LDAPEntry) element, Constants.UPLOADED_FILE_NAME_AT);
        if (optimizingJobID != null)
        {
          fileList.add(optimizingJobID);
        }
      }
    }

    String[] fileNames = new String[fileList.size()];
    fileList.toArray(fileNames);
    return fileNames;
  }



  /**
   * Retrieves information about the specified uploaded file from the directory
   * server.
   *
   * @param  folderDN  The DN of the job folder with which the uploaded file is
   *                   associated.
   * @param  fileName  The name of the file to retrieve.
   *
   * @return  The requested uploaded file from the configuration directory.
   *
   * @throws  LDAPException  If a problem occurs while interacting with the
   *                         directory server.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the file information.
   */
  private UploadedFile getUploadedFile(String folderDN, String fileName)
          throws LDAPException, DecodeException
  {
    LDAPEntry entry = null;
    String filter = "(&(objectClass=" + Constants.UPLOADED_FILE_OC + ")(" +
                    Constants.UPLOADED_FILE_NAME_AT + '=' + fileName + "))";
    LDAPSearchResults results = conn.search(folderDN, LDAPConnection.SCOPE_ONE,
                                            filter, null, false);
    while (results.hasMoreElements())
    {
      Object element = results.nextElement();
      if (element instanceof LDAPEntry)
      {
        entry = (LDAPEntry) element;
      }
    }

    if (entry == null)
    {
      throw new DecodeException("Could not retrieve the uploaded file entry " +
                                "from the directory server.");
    }


    String fileType = getValue(entry, Constants.UPLOADED_FILE_TYPE_AT);
    if ((fileType == null) || (fileType.length() == 0))
    {
      throw new DecodeException("Entry \"" + entry.getDN() +
                                "\" missing value for required attribute \"" +
                                Constants.UPLOADED_FILE_TYPE_AT + '"');
    }

    int fileSize;
    String sizeStr = getValue(entry, Constants.UPLOADED_FILE_SIZE_AT);
    try
    {
      fileSize = Integer.parseInt(sizeStr);
    }
    catch (Exception e)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DecodeException("Entry \"" + entry.getDN() +
                                "\" has a missing or invalid value for " +
                                "required attribute \"" +
                                Constants.UPLOADED_FILE_SIZE_AT + '"', e);
    }

    String fileDescription =
         getValue(entry, Constants.UPLOADED_FILE_DESCRIPTION_AT);
    if ((fileDescription == null) || (fileDescription.length() == 0))
    {
      fileDescription = null;
    }

    byte[] fileData = null;
    LDAPAttribute dataAttr =
         entry.getAttribute(Constants.UPLOADED_FILE_DATA_AT);
    if (dataAttr != null)
    {
      byte[][] values = dataAttr.getByteValueArray();
      if ((values != null) && (values.length == 1))
      {
        fileData = values[0];
      }
    }

    return new UploadedFile(fileName, fileType, fileSize, fileDescription,
                            fileData);
  }



  /**
   * Retrieves the value of the requested specified attribute from the provided
   * entry.
   *
   * @param  entry          The entry from which to retrieve the value.
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.
   *
   * @return  The value of the requested attribute, or <CODE>null</CODE> if no
   *          such attribute exists in the given entry.
   */
  private String getValue(LDAPEntry entry, String attributeName)
  {
    if ((entry == null) || (attributeName == null) ||
        (attributeName.length() == 0))
    {
      return null;
    }

    LDAPAttribute attr = entry.getAttribute(attributeName);
    if (attr == null)
    {
      return null;
    }

    String[] values = attr.getStringValueArray();
    if ((values == null) || (values.length == 0))
    {
      return null;
    }

    return values[0];
  }



  /**
   * Retrieves the set of values for the requested specified attribute from the
   * provided entry.
   *
   * @param  entry          The entry from which to retrieve the values.
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        values.
   *
   * @return  The values of the requested attribute, or <CODE>null</CODE> if no
   *          such attribute exists in the given entry.
   */
  private String[] getValues(LDAPEntry entry, String attributeName)
  {
    if ((entry == null) || (attributeName == null) ||
        (attributeName.length() == 0))
    {
      return null;
    }

    LDAPAttribute attr = entry.getAttribute(attributeName);
    if (attr == null)
    {
      return null;
    }

    String[] values = attr.getStringValueArray();
    if ((values == null) || (values.length == 0))
    {
      return null;
    }

    return values;
  }
}

