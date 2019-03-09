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



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.PreloadConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;

import com.slamd.admin.AdminServlet;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.JobClassLoader;
import com.slamd.dslogplay.LogPlaybackJobClass;
import com.slamd.jobs.BurnCPUJobClass;
import com.slamd.jobs.DSADMImportRateJobClass;
import com.slamd.jobs.DSCFGImportRateJobClass;
import com.slamd.jobs.ExecJobClass;
import com.slamd.jobs.HTTPGetRateJobClass;
import com.slamd.jobs.IMAPCheckRateJobClass;
import com.slamd.jobs.LDAPAddAndDeleteRateJobClass;
import com.slamd.jobs.LDAPAsynchronousModRateJobClass;
import com.slamd.jobs.LDAPAsynchronousSearchRateJobClass;
import com.slamd.jobs.LDAPAuthRateJobClass;
import com.slamd.jobs.LDAPCompareRateJobClass;
import com.slamd.jobs.LDAPFileBasedModRateJobClass;
import com.slamd.jobs.LDAPFileBasedSearchRateJobClass;
import com.slamd.jobs.LDAPMixedLoadJobClass;
import com.slamd.jobs.LDAPModDNRateJobClass;
import com.slamd.jobs.LDAPModRateJobClass;
import com.slamd.jobs.LDAPMultiConnectionSearchRateJobClass;
import com.slamd.jobs.LDAPPrimeJobClass;
import com.slamd.jobs.LDAPSearchRateJobClass;
import com.slamd.jobs.LDAPSearchAndModRateJobClass;
import com.slamd.jobs.LDAPWaitForDirectoryJobClass;
import com.slamd.jobs.LDIF2DBImportRateJobClass;
import com.slamd.jobs.MultiSearchLDAPLoadJobClass;
import com.slamd.jobs.NullJobClass;
import com.slamd.jobs.POPCheckRateJobClass;
import com.slamd.jobs.SMTPSendRateJobClass;
import com.slamd.jobs.SQLModRateJobClass;
import com.slamd.jobs.SQLSearchRateJobClass;
import com.slamd.jobs.SiteMinderJobClass;
import com.slamd.jobs.SolarisLDAPAuthRateJobClass;
import com.slamd.jobs.TCPReplayJobClass;
import com.slamd.jobs.ThroughputTestJobClass;
import com.slamd.jobs.WeightedSiteMinderJobClass;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizingJob;
import com.slamd.job.SingleStatisticOptimizationAlgorithm;
import com.slamd.job.SingleStatisticWithCPUUtilizationOptimizationAlgorithm;
import com.slamd.job.SingleStatisticWithConstraintOptimizationAlgorithm;
import com.slamd.job.SingleStatisticWithReplicationLatencyOptimizationAlgorithm;
import com.slamd.jobgroup.JobGroup;
import com.slamd.loadvariance.LoadVarianceTestJobClass;
import com.slamd.report.HTMLReportGenerator;
import com.slamd.report.PDFReportGenerator;
import com.slamd.report.TextReportGenerator;
import com.slamd.scripting.BSFJobClass;
import com.slamd.scripting.ScriptedJobClass;
import com.slamd.server.ConfigSubscriber;
import com.slamd.server.SLAMDServer;
import com.slamd.server.SLAMDServerException;
import com.slamd.server.UploadedFile;



/**
 * This class provides the main interface to the SLAMD database used for storing
 * user accounts, configuration information, and job data.
 *
 *
 * @author  Neil A. Wilson
 */
public class SLAMDDB
{
  // The set of transactions that are currently active in the database.
  ArrayList<Transaction> activeTransactions;

  // The list of configuration subscribers that have been registered with this
  // configuration database.
  ArrayList<ConfigSubscriber> configSubscribers;

  // Indicates whether the databases are currently open.
  boolean dbsOpen;

  // Indicates whether the database environment is currently open.
  boolean environmentOpen;

  // Indicates whether the database is currently operating in read-only mode.
  boolean readOnly;

  // The handle to the configuration database.
  Database configDB;

  // The handle to the uploaded file database.
  Database fileDB;

  // The handle to the folder database.
  Database folderDB;

  // The handle to the group database.
  Database groupDB;

  // The handle to the job database.
  Database jobDB;

  // The handle to the job group database.
  Database jobGroupDB;

  // The handle to the optimizing job database.
  Database optimizingJobDB;

  // The handle to the user database.
  Database userDB;

  // The handle to the virtual folder database.
  Database virtualFolderDB;

  // The database environment with which all the databases are associated.
  Environment dbEnv;

  // The path to the directory containing the database files.
  File dbDir;

  // The hash map that is used to hold information about the configuration
  // parameters.
  HashMap<String,String> configHash;

  // The set of jobs that are currently disabled and not eligible for execution.
  HashSet<String> disabledJobs;

  // The set of jobs that have been scheduled but have not yet started running
  // and are not disabled.
  HashSet<String> pendingJobs;

  // The set of jobs that are currently running.
  HashSet<String> runningJobs;

  // The mutex used to provide threadsafe access to the database.
  private final Object dbMutex;

  // The SLAMD server instance with which this database is associated.
  SLAMDServer slamdServer;



  /**
   * Initializes the SLAMD database and opens the environment, creating a new
   * one if necessary.
   *
   * @param  slamdServer        The SLAMD server instance with which this
   *                            database is associated.
   * @param  dbDirectory        The path to the directory in which the database
   *                            files should be held.
   * @param  readOnly           Indicates whether the database should be opened
   *                            in read-only mode.
   * @param  createIfNecessary  Indicates whether a new database environment
   *                            should be created if one does not already
   *                            exist.
   *
   * @throws  DatabaseException  If a problem occurs while initializing the
   *                             database environment.
   */
  public SLAMDDB(SLAMDServer slamdServer, String dbDirectory, boolean readOnly,
                 boolean createIfNecessary)
         throws DatabaseException
  {
    this.readOnly    = readOnly;
    this.slamdServer = slamdServer;


    // First make sure that the specified directory exists and is actually a
    // directory.  If it does not exist, then create it if appropriate.
    dbDir = new File(dbDirectory);
    if (dbDir.exists())
    {
      if (! dbDir.isDirectory())
      {
        String message = "Specified database directory \"" + dbDirectory +
                         "\" exists but is not a directory.";
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY, message);
        throw new DatabaseException(message);
      }
    }
    else
    {
      if (createIfNecessary)
      {
        try
        {
          dbDir.mkdirs();
        }
        catch (Exception e)
        {
          String message = "Unable to create database directory \"" +
                           dbDirectory + "\" -- " + e;
          slamdServer.logMessage(Constants.LOG_LEVEL_ANY, message);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          throw new DatabaseException(message, e);
        }
      }
      else
      {
        String message = "Database directory \"" + dbDirectory +
                         "\" does not exist and the SLAMD server has been " +
                         "configured to not create it.";
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY, message);
        throw new DatabaseException(message);
      }
    }


    // Define the properties that should be used to open the DB environment.
    EnvironmentConfig config = new EnvironmentConfig();
    config.setAllowCreate(createIfNecessary);
    config.setReadOnly(readOnly);
    config.setTransactional(true);
    config.setTxnNoSync(readOnly);
    config.setCachePercent(25);


    // Open the database environment with the specified configuration.
    dbEnv = new Environment(dbDir, config);
    slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                           "Opened the configuration database environment.");


    // Initialize the remaining instance variables.
    dbMutex            = new Object();
    activeTransactions = new ArrayList<Transaction>();
    environmentOpen    = true;
    dbsOpen            = false;
    configSubscribers  = new ArrayList<ConfigSubscriber>();


    // Open the individual databases.
    try
    {
      openDatabases(! readOnly);
    }
    catch (DatabaseException de)
    {
      try
      {
        dbEnv.close();
      } catch (Exception e) {}

      throw de;
    }
    catch (Exception e)
    {
      try
      {
        dbEnv.close();
      } catch (Exception e2) {}

      throw new DatabaseException("Unable to open the SLAMD databases:  " + e,
                                  e);
    }


    // Get the lists of disabled, pending, and running jobs.
    disabledJobs = new HashSet<String>();
    byte[] disabledBytes = get(null, configDB, Constants.PARAM_DISABLED_JOBS,
                               false);
    if ((disabledBytes != null) && (disabledBytes.length > 0))
    {
      String disabledStr;
      try
      {
        disabledStr = new String(disabledBytes, "UTF-8");
      }
      catch (UnsupportedEncodingException uee)
      {
        disabledStr = new String(disabledBytes);
      }

      StringTokenizer tokenizer = new StringTokenizer(disabledStr, "\n");
      while (tokenizer.hasMoreTokens())
      {
        disabledJobs.add(tokenizer.nextToken());
      }
    }

    pendingJobs = new HashSet<String>();
    byte[] pendingBytes = get(null, configDB, Constants.PARAM_PENDING_JOBS,
                              false);
    if ((pendingBytes != null) && (pendingBytes.length > 0))
    {
      String pendingStr;
      try
      {
        pendingStr = new String(pendingBytes, "UTF-8");
      }
      catch (UnsupportedEncodingException uee)
      {
        pendingStr = new String(pendingBytes);
      }

      StringTokenizer tokenizer = new StringTokenizer(pendingStr, "\n");
      while (tokenizer.hasMoreTokens())
      {
        pendingJobs.add(tokenizer.nextToken());
      }
    }

    runningJobs = new HashSet<String>();
    byte[] runningBytes = get(null, configDB, Constants.PARAM_RUNNING_JOBS,
                              false);
    if ((runningBytes != null) && (runningBytes.length > 0))
    {
      String runningStr;
      try
      {
        runningStr = new String(runningBytes, "UTF-8");
      }
      catch (UnsupportedEncodingException uee)
      {
        runningStr = new String(runningBytes);
      }

      StringTokenizer tokenizer = new StringTokenizer(runningStr, "\n");
      while (tokenizer.hasMoreTokens())
      {
        runningJobs.add(tokenizer.nextToken());
      }
    }
  }



  /**
   * Checks to see if a valid SLAMD database exists in the specified location.
   *
   * @param  dbDirectory  The path to the directory containing the database
   *                      files.
   *
   * @return  <CODE>true</CODE> if the database exists, or <CODE>false</CODE>
   *          false if not.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to make
   *                             the determination.
   */
  public static boolean dbExists(String dbDirectory)
         throws DatabaseException
  {
    // First make sure that the specified directory exists and is actually a
    // directory.  If it does not exist, then create it if appropriate.
    File dbDir = new File(dbDirectory);
    if (dbDir.exists())
    {
      if (! dbDir.isDirectory())
      {
        throw new DatabaseException("Specified database directory \"" +
                                    dbDirectory +
                                    "\" exists but is not a directory.");
      }
    }
    else
    {
      return false;
    }


    // Define the properties that should be used to open the DB environment.
    EnvironmentConfig config = new EnvironmentConfig();
    config.setAllowCreate(false);
    config.setReadOnly(true);
    config.setTransactional(true);


    // Open the database environment with the specified configuration.
    Environment dbEnv = new Environment(dbDir, config);


    // Try to open the configuration database.
    try
    {
      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setAllowCreate(false);
      dbConfig.setReadOnly(true);

      Database configDB = dbEnv.openDatabase(null, Constants.DB_NAME_CONFIG,
                                             dbConfig);
      configDB.close();
      dbEnv.close();
      return true;
    }
    catch (DatabaseNotFoundException dnfe)
    {
      try
      {
        dbEnv.close();
      } catch (Exception e) {}

      return false;
    }
    catch (DatabaseException de)
    {
      try
      {
        dbEnv.close();
      } catch (Exception e) {}

      throw de;
    }
  }



  /**
   * Creates a new SLAMD database if one does not already exist.
   *
   * @param  dbDirectory  The path to the directory in which to create the
   *                      database.
   *
   * @throws  DatabaseException  If a problem occurs while trying to create the
   *                             database.
   */
  public static void createDB(String dbDirectory)
         throws DatabaseException
  {
    // First make sure that the specified directory exists and is actually a
    // directory.  If it does not exist, then create it if appropriate.
    File dbDir = new File(dbDirectory);
    if (dbDir.exists())
    {
      if (! dbDir.isDirectory())
      {
        throw new DatabaseException("Specified database directory \"" +
                                    dbDirectory +
                                    "\" exists but is not a directory.");
      }
    }
    else
    {
      try
      {
        dbDir.mkdirs();
      }
      catch (Exception e)
      {
        throw new DatabaseException("Unable to create database directory \"" +
                                    dbDirectory + "\" -- " + e, e);
      }
    }


    // Define the properties that should be used to open the DB environment.
    EnvironmentConfig config = new EnvironmentConfig();
    config.setAllowCreate(true);
    config.setReadOnly(false);
    config.setTransactional(true);
    config.setTxnNoSync(true);


    // Open the database environment with the specified configuration.
    Environment dbEnv = new Environment(dbDir, config);


    // Open and close the individual DBs, creating them if necessary.
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    dbConfig.setReadOnly(false);
    dbConfig.setSortedDuplicates(false);
    dbConfig.setTransactional(true);

    try
    {
      dbEnv.openDatabase(null, Constants.DB_NAME_CONFIG, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_FILE, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_FOLDER, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_GROUP, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_JOB, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_JOB_GROUP, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_OPTIMIZING_JOB,
                         dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_USER, dbConfig).close();
      dbEnv.openDatabase(null, Constants.DB_NAME_VIRTUAL_FOLDER,
                         dbConfig).close();
    }
    catch (DatabaseException de)
    {
      try
      {
        dbEnv.close();
      } catch (Exception e) {}

      throw de;
    }


    // Re-open the config DB and Populate it with the most important initial
    // configuration.
    Database configDB = dbEnv.openDatabase(null, Constants.DB_NAME_CONFIG,
                                           dbConfig);
    String[] jobClasses =
    {
      BurnCPUJobClass.class.getName(),
      DSADMImportRateJobClass.class.getName(),
      DSCFGImportRateJobClass.class.getName(),
      ExecJobClass.class.getName(),
      HTTPGetRateJobClass.class.getName(),
      IMAPCheckRateJobClass.class.getName(),
      LDAPAddAndDeleteRateJobClass.class.getName(),
      LDAPAsynchronousModRateJobClass.class.getName(),
      LDAPAsynchronousSearchRateJobClass.class.getName(),
      LDAPAuthRateJobClass.class.getName(),
      LDAPCompareRateJobClass.class.getName(),
      LDAPFileBasedModRateJobClass.class.getName(),
      LDAPFileBasedSearchRateJobClass.class.getName(),
      LDAPMixedLoadJobClass.class.getName(),
      LDAPModDNRateJobClass.class.getName(),
      LDAPModRateJobClass.class.getName(),
      LDAPMultiConnectionSearchRateJobClass.class.getName(),
      LDAPPrimeJobClass.class.getName(),
      LDAPSearchRateJobClass.class.getName(),
      LDAPSearchAndModRateJobClass.class.getName(),
      LDAPWaitForDirectoryJobClass.class.getName(),
      LDIF2DBImportRateJobClass.class.getName(),
      MultiSearchLDAPLoadJobClass.class.getName(),
      NullJobClass.class.getName(),
      POPCheckRateJobClass.class.getName(),
      SiteMinderJobClass.class.getName(),
      SMTPSendRateJobClass.class.getName(),
      SolarisLDAPAuthRateJobClass.class.getName(),
      SQLModRateJobClass.class.getName(),
      SQLSearchRateJobClass.class.getName(),
      TCPReplayJobClass.class.getName(),
      ThroughputTestJobClass.class.getName(),
      WeightedSiteMinderJobClass.class.getName(),
      LoadVarianceTestJobClass.class.getName(),
      BSFJobClass.class.getName(),
      ScriptedJobClass.class.getName(),
      LogPlaybackJobClass.class.getName()
    };

    String[] optimizationAlgorithms =
    {
      SingleStatisticOptimizationAlgorithm.class.getName(),
      SingleStatisticWithConstraintOptimizationAlgorithm.class.getName(),
      SingleStatisticWithCPUUtilizationOptimizationAlgorithm.class.getName(),
      SingleStatisticWithReplicationLatencyOptimizationAlgorithm.class.getName()
    };

    String[] reportGenerators =
    {
      HTMLReportGenerator.class.getName(),
      PDFReportGenerator.class.getName(),
      TextReportGenerator.class.getName()
    };


    try
    {
      StringBuilder buffer = new StringBuilder();
      for (int i=0; i < jobClasses.length; i++)
      {
        if (i > 0)
        {
          buffer.append('\n');
        }

        buffer.append(jobClasses[i]);
      }
      put(configDB, Constants.PARAM_JOB_CLASSES, buffer.toString());


      buffer = new StringBuilder();
      for (int i=0; i < optimizationAlgorithms.length; i++)
      {
        if (i > 0)
        {
          buffer.append('\n');
        }

        buffer.append(optimizationAlgorithms[i]);
      }
      put(configDB, Constants.PARAM_OPTIMIZATION_ALGORITHMS, buffer.toString());


      buffer = new StringBuilder();
      for (int i=0; i < reportGenerators.length; i++)
      {
        if (i > 0)
        {
          buffer.append('\n');
        }

        buffer.append(reportGenerators[i]);
      }
      put(configDB, Constants.PARAM_REPORT_GENERATOR_CLASSES,
          buffer.toString());


      String logFilename = AdminServlet.getWebInfPath() + '/' +
                           Constants.DEFAULT_LOG_FILENAME;
      put(configDB, Constants.PARAM_LOG_FILENAME, logFilename);
    }
    catch (DatabaseException de)
    {
      try
      {
        configDB.close();
        dbEnv.close();
      } catch (Exception e) {}

      throw de;
    }


    // Create an "Unclassified" job folder.
    Database folderDB = dbEnv.openDatabase(null, Constants.DB_NAME_FOLDER,
                                           dbConfig);
    JobFolder folder = new JobFolder(Constants.FOLDER_NAME_UNCLASSIFIED, false,
                                     false, null, null, "Unclassified jobs.",
                                     null, null, null, null);
    try
    {
      put(folderDB, Constants.FOLDER_NAME_UNCLASSIFIED, folder.encode());
    }
    catch (DatabaseException de)
    {
      try
      {
        folderDB.close();
        dbEnv.close();
      } catch (Exception e) {}

      throw de;
    }
    folderDB.close();



    // Close the database environment.
    try
    {
      configDB.close();
    } catch (Exception e) {}

    dbEnv.close();
  }



  /**
   * Opens all the databases used in the SLAMD database environment.  If the
   * databases are already open, then no action will be taken.
   *
   * @param  createIfNecessary  Indicates whether any database(s) that do not
   *                            exist should be created.
   *
   * @throws  DatabaseException  If a problem occurs while opening the
   *                             databases. Note that if an exception is thrown,
   *                             then all databases will be closed but the
   *                             environment will still be open.
   */
  public void openDatabases(boolean createIfNecessary)
         throws DatabaseException
  {
    // See if the database is already open.  If so, then don't do anything.
    synchronized (dbMutex)
    {
      if (dbsOpen)
      {
        return;
      }


      // The database environment must be open in order to proceed.
      if (! environmentOpen)
      {
        String message = "Unable to open SLAMD databases because the " +
                         "database environment is not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY, message);
        throw new DatabaseException(message);
      }


      // Create the database configuration that will be used to open the
      // databases.
      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setAllowCreate(createIfNecessary);
      dbConfig.setReadOnly(readOnly);
      dbConfig.setSortedDuplicates(false);
      dbConfig.setTransactional(true);


      // Iterate through each of the databases and open them.
      try
      {
        configDB        = openDB(Constants.DB_NAME_CONFIG, dbConfig);
        fileDB          = openDB(Constants.DB_NAME_FILE, dbConfig);
        folderDB        = openDB(Constants.DB_NAME_FOLDER, dbConfig);
        groupDB         = openDB(Constants.DB_NAME_GROUP, dbConfig);
        jobDB           = openDB(Constants.DB_NAME_JOB, dbConfig);
        jobGroupDB      = openDB(Constants.DB_NAME_JOB_GROUP, dbConfig);
        optimizingJobDB = openDB(Constants.DB_NAME_OPTIMIZING_JOB, dbConfig);
        userDB          = openDB(Constants.DB_NAME_USER, dbConfig);
        virtualFolderDB = openDB(Constants.DB_NAME_VIRTUAL_FOLDER, dbConfig);
      }
      catch (DatabaseException dbe)
      {
        closeDatabases(true);
        throw dbe;
      }


      // Populate the configuration map.
      configHash = new HashMap<String,String>();
      Cursor configCursor = configDB.openCursor(null, new CursorConfig());
      DatabaseEntry keyEntry = new DatabaseEntry();
      DatabaseEntry valueEntry = new DatabaseEntry();
      OperationStatus status = configCursor.getFirst(keyEntry, valueEntry,
                                                     LockMode.DEFAULT);
      while (status == OperationStatus.SUCCESS)
      {
        try
        {
          String key   = new String(keyEntry.getData(), "UTF-8");
          String value = new String(valueEntry.getData(), "UTF-8");
          configHash.put(key, value);
        }
        catch (UnsupportedEncodingException uee)
        {
          String key   = new String(keyEntry.getData());
          String value = new String(valueEntry.getData());
          configHash.put(key, value);
        }

        status = configCursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT);
      }
      configCursor.close();


      // At this point, all the databases should be open.
      dbsOpen = true;
    }
  }



  /**
   * Opens the specified database with the given configuration.
   *
   * @param  dbName    The name of the database to open.
   * @param  dbConfig  The configuration to use to open the database.
   *
   * @return  The database that was opened.
   *
   * @throws  DatabaseException  If a problem occurs while opening the database.
   */
  private Database openDB(String dbName, DatabaseConfig dbConfig)
          throws DatabaseException
  {
    Database db = dbEnv.openDatabase(null, dbName, dbConfig);

    PreloadConfig preloadConfig = new PreloadConfig();
    preloadConfig.setLoadLNs(false);
    db.preload(preloadConfig);

    return db;
  }



  /**
   * Closes all databases in the environment.  If the databases are already
   * closed then this method will do anything.
   *
   * @param  abortTransactions  Indicates whether any transactions in progress
   *                            should be aborted.  If not and there are active
   *                            transactions, then this method will fail.
   *
   * @throws  DatabaseException  If there are active transactions and this
   *                             method is configured to not abort them.
   */
  public void closeDatabases(boolean abortTransactions)
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // If the databases aren't open then don't do anything.
      if (! dbsOpen)
      {
        return;
      }


      // If there are active transactions, then either abort them or fail.
      if (! activeTransactions.isEmpty())
      {
        if (abortTransactions)
        {
          for (Transaction txn : activeTransactions)
          {
            try
            {
              txn.abort();
            } catch (Exception e) {}
          }

          activeTransactions.clear();
        }
        else
        {
          String message = "Not closing databases because there are active " +
                           "transactions and this method has been configured " +
                           "to not abort them.";
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
          throw new DatabaseException(message);
        }
      }


      // Close all the databases.
      closeDB(configDB);
      closeDB(fileDB);
      closeDB(folderDB);
      closeDB(groupDB);
      closeDB(jobDB);
      closeDB(jobGroupDB);
      closeDB(optimizingJobDB);
      closeDB(userDB);
      closeDB(virtualFolderDB);
      dbsOpen = false;
    }
  }



  /**
   * Closes the provided database.
   *
   * @param  db  The database to be closed.
   */
  private void closeDB(Database db)
  {
    if (db != null)
    {
      try
      {
        db.close();
      } catch (Exception e) {}
    }
  }



  /**
   * Closes the database environment.  This will only be allowed if the
   * databases have already been closed.
   *
   * @throws  DatabaseException  If the databases are still open.
   */
  public void closeEnvironment()
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that there are no databases open.
      if (dbsOpen)
      {
        String message = "Unable to close the environment because the " +
                         "databases are still open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // Ensure that the environment is synchronized.
      try
      {
        dbEnv.sync();
      } catch (Exception e) {}


      // Close the database environment.
      try
      {
        dbEnv.close();
      } catch (Exception e) {}

      environmentOpen = false;
    }
  }



  /**
   * Writes data from the specified job folder(s) in a form that is suitable for
   * importing into another SLAMD server instance.
   *
   * @param  realFolderNames     The names of the real job folders to include in
   *                             the export.
   * @param  virtualFolderNames  The names of the virtual job folders to include
   *                             in the export.
   * @param  jobGroupNames       The names of the job groups to include in the
   *                             export.
   * @param  outputStream        The output stream to which the export data
   *                             should be written.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             database.
   *
   * @throws  IOException  If a problem occurs while writing to the provided
   *                       output stream.
   */
  public void exportFolderData(String[] realFolderNames,
                               String[] virtualFolderNames,
                               String[] jobGroupNames,
                               OutputStream outputStream)
         throws DatabaseException, IOException
  {
    if ((realFolderNames.length == 0) && (virtualFolderNames.length == 0) &&
        (jobGroupNames.length == 0))
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                             "No folders or job groups specified to include " +
                             "in the export.");
      return;
    }


    ASN1Writer asn1Writer = new ASN1Writer(outputStream);


    // First, the real job folders.  We have to keep everything consistent
    // within a folder, so we'll use a transaction to protect it.
    for (int i=0; i < realFolderNames.length; i++)
    {
      Transaction txn = getTransaction();
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                             "Beginning export for folder \"" +
                             realFolderNames[i] + '"');

      try
      {
        // Retrieve the entry for the folder itself.
        byte[] folderBytes = get(txn, folderDB, realFolderNames[i], false);
        if (folderBytes == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Could not find job folder " +
                                 realFolderNames[i] +
                                 " in the configuration database.");
          continue;
        }

        JobFolder folder = JobFolder.decode(folderBytes);
        ASN1Element[] elements =
        {
          new ASN1OctetString(Constants.DB_NAME_FOLDER),
          new ASN1OctetString(realFolderNames[i]),
          new ASN1OctetString(folderBytes)
        };
        asn1Writer.writeElement(new ASN1Sequence(elements));


        // Get the set of jobs contained in that folder.
        String[] jobIDs = folder.getJobIDs();
        for (int j=0; j < jobIDs.length; j++)
        {
          try
          {
            byte[] jobBytes = get(txn, jobDB, jobIDs[j], false);
            if (jobBytes == null)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                     "Could not find job " + jobIDs[j] +
                                     " referenced in folder " +
                                     realFolderNames[i]);
              continue;
            }

            elements = new ASN1Element[]
            {
              new ASN1OctetString(Constants.DB_NAME_JOB),
              new ASN1OctetString(jobIDs[j]),
              new ASN1OctetString(jobBytes)
            };
            asn1Writer.writeElement(new ASN1Sequence(elements));
          }
          catch (IOException ioe)
          {
            throw ioe;
          }
          catch (Exception e)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                   "Unable to retrieve information about job " +
                                   jobIDs[j] + " from the configuration " +
                                   "database:  " + e);
          }
        }


        // Get the set of optimizing jobs contained in the folder.
        String[] optimizingJobIDs = folder.getOptimizingJobIDs();
        for (int j=0; j < optimizingJobIDs.length; j++)
        {
          try
          {
            byte[] optimizingJobBytes = get(txn, optimizingJobDB,
                                            optimizingJobIDs[j], false);
            if (optimizingJobBytes == null)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                     "Could not find optimizing job " +
                                     optimizingJobIDs[j] + " referenced in " +
                                     "folder " + realFolderNames[i]);
              continue;
            }

            elements = new ASN1Element[]
            {
              new ASN1OctetString(Constants.DB_NAME_OPTIMIZING_JOB),
              new ASN1OctetString(optimizingJobIDs[j]),
              new ASN1OctetString(optimizingJobBytes)
            };
            asn1Writer.writeElement(new ASN1Sequence(elements));
          }
          catch (IOException ioe)
          {
            throw ioe;
          }
          catch (Exception e)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                   "Unable to retrieve information about " +
                                   "optimizing job " + optimizingJobIDs[j] +
                                   " from the configuration database:  " + e);
          }
        }


        // Get the set of uploaded files contained in the folder.
        String[] fileNames = folder.getFileNames();
        for (int j=0; j < fileNames.length; j++)
        {
          try
          {
            String key = realFolderNames[i] + '\t' + fileNames[j];
            byte[] fileBytes = get(txn, fileDB, key, false);
            if (fileBytes == null)
            {
              slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                     "Could not find uploaded file " +
                                     fileNames[j] + " referenced in folder " +
                                     realFolderNames[i]);
              continue;
            }

            elements = new ASN1Element[]
            {
              new ASN1OctetString(Constants.DB_NAME_FILE),
              new ASN1OctetString(fileNames[j]),
              new ASN1OctetString(fileBytes)
            };
            asn1Writer.writeElement(new ASN1Sequence(elements));
          }
          catch (IOException ioe)
          {
            throw ioe;
          }
          catch (Exception e)
          {
            slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                   "Unable to retrieve information about " +
                                   "uploaded " + "file " + fileNames[j] +
                                   " from the configuration database:  " + e);
          }
        }
      }
      catch (DatabaseException de)
      {
        throw de;
      }
      catch (IOException ioe)
      {
        throw ioe;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DatabaseException("Unexpected error occurred while " +
                                    "performing the export:  " + e, e);
      }
      finally
      {
        abortTransaction(txn);
      }

      outputStream.flush();
    }


    // Next, iterate through the virtual folders.  In this case, we just need
    // the folder itself since the jobs are contained elsewhere and were
    // hopefully included in the export of the real folders, so there is no need
    // for a transaction.
    for (int i=0; i < virtualFolderNames.length; i++)
    {
      try
      {
        // Retrieve the entry for the virtual folder itself.
        byte[] virtualFolderBytes = get(null, virtualFolderDB,
                                        virtualFolderNames[i], false);
        if (virtualFolderBytes == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Could not find virtual job folder " +
                                 virtualFolderNames[i] +
                                 " in the configuration database.");
          continue;
        }

        ASN1Element[] elements =
        {
          new ASN1OctetString(Constants.DB_NAME_VIRTUAL_FOLDER),
          new ASN1OctetString(virtualFolderNames[i]),
          new ASN1OctetString(virtualFolderBytes)
        };
        asn1Writer.writeElement(new ASN1Sequence(elements));
      }
      catch (DatabaseException de)
      {
        throw de;
      }
      catch (IOException ioe)
      {
        throw ioe;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DatabaseException("Unexpected error occurred while " +
                                    "performing the export:  " + e, e);
      }

      outputStream.flush();
    }


    // Next, iterate through the job groups.  In this case, a job group is a
    // single entity in the database, so there is no need to protect this with a
    // transaction.
    for (int i=0; i < jobGroupNames.length; i++)
    {
      try
      {
        // Retrieve the entry for the job group itself.
        byte[] jobGroupBytes = get(null, jobGroupDB, jobGroupNames[i], false);

        if (jobGroupBytes == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Could not find job group " +
                                 jobGroupNames[i] +
                                 " in the configuration database.");

          continue;
        }

        ASN1Element[] elements =
        {
          new ASN1OctetString(Constants.DB_NAME_JOB_GROUP),
          new ASN1OctetString(jobGroupNames[i]),
          new ASN1OctetString(jobGroupBytes)
        };
        asn1Writer.writeElement(new ASN1Sequence(elements));
      }
      catch (DatabaseException de)
      {
        throw de;
      }
      catch (IOException ioe)
      {
        throw ioe;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DatabaseException("Unexpected error occurred while " +
                                    "performing the export:  " + e, e);
      }

      outputStream.flush();
    }
  }



  /**
   * Reads import data from the provided input stream and imports it into the
   * database.
   *
   * @param  inputStream     The input stream from which to read the data to
   *                         import.
   * @param  progressWriter  The print writer that should be used to write
   *                         progress about the import to the end user.
   * @param  writeHTML       Indicates whether the progress information should
   *                         be written in HTML format.  If not, then it will be
   *                         plain text.
   *
   * @return  <CODE>true</CODE> if the import was completely successful, or
   *          <CODE>false</CODE> if one or more problems were encountered.
   */
  public boolean importFolderData(InputStream inputStream,
                                  PrintWriter progressWriter, boolean writeHTML)
  {
    ASN1Reader asn1Reader = new ASN1Reader(inputStream);

    ASN1Element element;
    try
    {
      element = nextElement(asn1Reader, progressWriter, writeHTML);
    }
    catch (Exception e)
    {
      return false;
    }

    boolean completeSuccess = true;
    int     numRecords      = 0;
    long    startTime       = System.currentTimeMillis();
    while (element != null)
    {
      numRecords++;

      byte[] data;
      String dbName;
      String keyName;
      try
      {
        ASN1Element[] elements = element.decodeAsSequence().getElements();
        dbName  = elements[0].decodeAsOctetString().getStringValue();
        keyName = elements[1].decodeAsOctetString().getStringValue();
        data    = elements[2].decodeAsOctetString().getValue();
      }
      catch (Exception e)
      {
        progressWriter.println("Unable to decode ASN.1 element read from the " +
                               "input stream as a sequence of three " +
                               "elements:");

        if (writeHTML)
        {
          progressWriter.println("<BR>");
          progressWriter.println("<PRE>");
        }

        progressWriter.println(JobClass.stackTraceToString(e));

        if (writeHTML)
        {
          progressWriter.println("</PRE>");
          progressWriter.println("<BR>");
        }

        try
        {
          element = nextElement(asn1Reader, progressWriter, writeHTML);
        }
        catch (Exception e2)
        {
          return false;
        }

        completeSuccess = false;
        continue;
      }


      Database db = getDB(dbName);
      if (db == null)
      {
        progressWriter.println("Unable to retrieve a reference to database " +
                               dbName + " from the DB environment.");

        if (writeHTML)
        {
          progressWriter.println("<BR><BR>");
        }
        else
        {
          progressWriter.println();
        }

        try
        {
          element = nextElement(asn1Reader, progressWriter, writeHTML);
        }
        catch (Exception e)
        {
          return false;
        }

        completeSuccess = false;
        continue;
      }


      try
      {
        // See if the specified record exists.  If so, then see if it's a folder
        // and merge the contents together.  Otherwise, refuse to overwrite the
        // existing record.
        byte[] existingData;
        if ((existingData = get(null, db, keyName, false)) != null)
        {
          if (dbName.equals(Constants.DB_NAME_FOLDER))
          {
            JobFolder existingFolder = getFolder(keyName);
            JobFolder newFolder      = JobFolder.decode(existingData);

            String[] newJobIDs = newFolder.getJobIDs();
            for (int i=0; i < newJobIDs.length; i++)
            {
              if (! existingFolder.containsJobID(newJobIDs[i]))
              {
                existingFolder.addJobID(newJobIDs[i]);
              }
            }

            String[] newOptimizingJobIDs = newFolder.getOptimizingJobIDs();
            for (int i=0; i < newOptimizingJobIDs.length; i++)
            {
              if (! existingFolder.containsOptimizingJobID(
                                        newOptimizingJobIDs[i]))
              {
                existingFolder.addOptimizingJobID(newOptimizingJobIDs[i]);
              }
            }

            String[] newChildNames = newFolder.getChildNames();
            for (int i=0; i < newChildNames.length; i++)
            {
              if (! existingFolder.containsChildName(newChildNames[i]))
              {
                existingFolder.addChildName(newChildNames[i]);
              }
            }

            String[] newFileNames = newFolder.getFileNames();
            for (int i=0; i < newFileNames.length; i++)
            {
              if (! existingFolder.containsFileName(newFileNames[i]))
              {
                existingFolder.addFileName(newFileNames[i]);
              }
            }

            byte[] updatedEntry = existingFolder.encode();
            put(null, db, keyName, updatedEntry);
            if (writeHTML)
            {
              progressWriter.println("<SPAN CLASS=\"" +
                                     Constants.STYLE_WARNING_TEXT +
                                     "\">Updated existing job folder \"" +
                                     keyName + "\" in database \"" + dbName +
                                     "\".</SPAN>");
            }
            else
            {
              progressWriter.println("Updated existing job folder \"" +
                                     keyName + "\" in database \"" + dbName +
                                     "\".");
            }
          }
          else
          {
            if (writeHTML)
            {
              progressWriter.println("<SPAN CLASS=\"" +
                                     Constants.STYLE_WARNING_TEXT +
                                     "\">Refusing to overwrite existing " +
                                     "record with key \"" + keyName +
                                     "\" in database \"" + dbName +
                                     "\".</SPAN>");
            }
            else
            {
              progressWriter.println("Refusing to overwrite existing record " +
                                     "with key \"" + keyName +
                                     "\" in database \"" + dbName + "\".");
            }
          }
        }
        else
        {
          put(null, db, keyName, data);
          progressWriter.println("Successfully wrote record with key \"" +
                                 keyName + "\" to database \"" + dbName +
                                 "\".");
        }

        if (writeHTML)
        {
          progressWriter.println("<BR><BR>");
        }
        else
        {
          progressWriter.println();
        }
      }
      catch (Exception e)
      {
        progressWriter.println("Unable to write entry with key \"" + keyName +
                               "\" to database \"" + dbName + "\":");

        if (writeHTML)
        {
          progressWriter.println("<BR>");
          progressWriter.println("<PRE>");
        }

        progressWriter.println(JobClass.stackTraceToString(e));

        if (writeHTML)
        {
          progressWriter.println("</PRE>");
          progressWriter.println("<BR>");
        }

        try
        {
          element = nextElement(asn1Reader, progressWriter, writeHTML);
        }
        catch (Exception e2)
        {
          return false;
        }

        completeSuccess = false;
        continue;
      }


      try
      {
        element = nextElement(asn1Reader, progressWriter, writeHTML);
      }
      catch (Exception e)
      {
        return false;
      }

      completeSuccess = false;
      continue;
    }

    long endTime = System.currentTimeMillis();
    progressWriter.println("<BR><BR>");
    progressWriter.println("Import complete.<BR><BR>");
    progressWriter.println("Processed " + numRecords + " records in " +
                           (endTime - startTime) + " milliseconds.<BR>");

    return completeSuccess;
  }



  /**
   * Retrieves the next ASN.1 element from the provided reader.
   *
   * @param  reader          The ASN.1 reader from which to read the next
   *                         element.
   * @param  progressWriter  The print writer to which progress information may
   *                         be written.
   * @param  writeHTML       Indicates whether information written to the print
   *                         writer should be in HTML form.
   *
   * @return  The next ASN.1 element read from the provided reader.
   *
   * @throws  IOException  If a problem occurs while reading from the provided
   *                       reader.
   *
   * @throws  ASN1Exception  If a problem occurs while trying to decode the data
   *                         read as an ASN.1 element.
   */
  private static ASN1Element nextElement(ASN1Reader reader,
                                         PrintWriter progressWriter,
                                         boolean writeHTML)
          throws IOException, ASN1Exception
  {
    try
    {
      return reader.readElement();
    }
    catch (IOException ioe)
    {
      progressWriter.println("I/O error encountered while attempting to read " +
                             "from the provided input stream:");

      if (writeHTML)
      {
        progressWriter.println("<BR>");
        progressWriter.println("<PRE>");
      }

      progressWriter.println(JobClass.stackTraceToString(ioe));

      if (writeHTML)
      {
        progressWriter.println("</PRE>");
        progressWriter.println("<BR>");
      }

      throw ioe;
    }
    catch (ASN1Exception ae)
    {
      progressWriter.println("Unable to decode data read from input stream " +
                             "as an ASN.1 element:");

      if (writeHTML)
      {
        progressWriter.println("<BR>");
        progressWriter.println("<PRE>");
      }

      progressWriter.println(JobClass.stackTraceToString(ae));

      if (writeHTML)
      {
        progressWriter.println("</PRE>");
        progressWriter.println("<BR>");
      }

      throw ae;
    }
    catch (Exception e)
    {
      String message = "Unexpected error while reading ASN.1 element from " +
                       "input stream:";
      progressWriter.println(message);

      if (writeHTML)
      {
        progressWriter.println("<BR>");
        progressWriter.println("<PRE>");
      }

      progressWriter.println(JobClass.stackTraceToString(e));

      if (writeHTML)
      {
        progressWriter.println("</PRE>");
        progressWriter.println("<BR>");
      }

      throw new ASN1Exception(message + ":  " + e, e);
    }
  }



  /**
   * Retrieves the names of the databases contained in the DB environment.
   *
   * @return  The names of the databases contained in the DB environment.
   *
   * @throws  DatabaseException  If a problem occurs while obtaining the list of
   *                             database names.
   */
  public String[] getDBNames()
         throws DatabaseException
  {
    List<String> list = dbEnv.getDatabaseNames();
    String[] dbNames = new String[list.size()];
    list.toArray(dbNames);
    return dbNames;
  }



  /**
   * Retrieves a handle to the database with the specified name.
   *
   * @param  dbName  The name of the database to retrieve.
   *
   * @return  The requested database, or <CODE>null</CODE> if there is no such
   *          database.
   */
  private Database getDB(String dbName)
  {
    Database db = null;

    if (dbName.equals(Constants.DB_NAME_CONFIG))
    {
      db = configDB;
    }
    else if (dbName.equals(Constants.DB_NAME_FILE))
    {
      db = fileDB;
    }
    else if (dbName.equals(Constants.DB_NAME_FOLDER))
    {
      db = folderDB;
    }
    else if (dbName.equals(Constants.DB_NAME_GROUP))
    {
      db = groupDB;
    }
    else if (dbName.equals(Constants.DB_NAME_JOB))
    {
      db = jobDB;
    }
    else if (dbName.equals(Constants.DB_NAME_JOB_GROUP))
    {
      db = jobGroupDB;
    }
    else if (dbName.equals(Constants.DB_NAME_OPTIMIZING_JOB))
    {
      db = optimizingJobDB;
    }
    else if (dbName.equals(Constants.DB_NAME_USER))
    {
      db = userDB;
    }
    else if (dbName.equals(Constants.DB_NAME_VIRTUAL_FOLDER))
    {
      db = virtualFolderDB;
    }

    return db;
  }



  /**
   * Retrieves a list of all the keys in the specified database.
   *
   * @param  dbName  The name of the database for which to retrieve the list of
   *                 keys.
   *
   * @return  A list of all the keys in the specified database.
   *
   * @throws  DatabaseException  If a problem occurs while obtaining the list of
   *                             database keys.
   */
  public String[] getDBKeys(String dbName)
         throws DatabaseException
  {
    Database db = getDB(dbName);
    if (db == null)
    {
      throw new DatabaseException("No database found with a name of \"" +
                                  dbName + '"');
    }

    CursorConfig cursorConfig = new CursorConfig();
    cursorConfig.setReadUncommitted(true);
    Cursor cursor = db.openCursor(null, cursorConfig);

    ArrayList<String> keyList = new ArrayList<String>();
    try
    {
      DatabaseEntry keyEntry  = new DatabaseEntry();
      DatabaseEntry dataEntry = new DatabaseEntry();

      OperationStatus status = cursor.getFirst(keyEntry, dataEntry,
                                               LockMode.READ_UNCOMMITTED);
      while (status == OperationStatus.SUCCESS)
      {
        keyList.add(new String(keyEntry.getData(), "UTF-8"));
        status = cursor.getNext(keyEntry, dataEntry, LockMode.READ_UNCOMMITTED);
      }

      String[] keys = new String[keyList.size()];
      keyList.toArray(keys);
      return keys;
    }
    catch (DatabaseException de)
    {
      throw de;
    }
    catch (Exception e)
    {
      throw new DatabaseException("Unexpected exception caught while " +
                                  "retrieving DB keys:  " + e, e);
    }
    finally
    {
      cursor.close();
    }
  }



  /**
   * Retrieves the data associated with the specified key in the given database.
   *
   * @param  dbName  The name of the database from which the data should be
   *                 retrieved.
   * @param  dbKey   The key associated with the data to retrieve.
   *
   * @return  The data associated with the specified key in the given database,
   *          or <CODE>null</CODE> if there is no such database key.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             database.
   */
  public byte[] getDBData(String dbName, String dbKey)
         throws DatabaseException
  {
    Database db = getDB(dbName);
    if (db == null)
    {
      return null;
    }

    return get(null, db, dbKey, false);
  }



  /**
   * Retrieves the value of the specified configuration parameter from the
   * configuration database.
   *
   * @param  parameterName  The name of the configuration parameter to
   *                        retrieve.
   *
   * @return  The value of the specified configuration parameter, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public String getConfigParameter(String parameterName)
  {
    return configHash.get(parameterName);
  }



  /**
   * Retrieves the value of the specified configuration parameter from the
   * configuration database.
   *
   * @param  parameterName  The name of the configuration parameter to
   *                        retrieve.
   * @param  defaultValue   The value to use for the parameter if the specified
   *                        key is not present in the database.
   *
   * @return  The value of the specified configuration parameter, or the
   *          provided default value if there is no such parameter.
   */
  public String getConfigParameter(String parameterName, String defaultValue)
  {
    String value = configHash.get(parameterName);
    if (value == null)
    {
      return defaultValue;
    }

    return value;
  }



  /**
   * Retrieves the value of the specified configuration parameter from the
   * configuration database.
   *
   * @param  parameterName  The name of the configuration parameter to
   *                        retrieve.
   * @param  defaultValue   The value to use for the parameter if the specified
   *                        key is not present in the database.
   *
   * @return  The value of the specified configuration parameter, or the
   *          provided default value if there is no such parameter.
   */
  public boolean getConfigParameter(String parameterName, boolean defaultValue)
  {
    String value = configHash.get(parameterName);
    if (value == null)
    {
      return defaultValue;
    }

    if (value.equals("true") || value.equals("yes") || value.equals("on") ||
        value.equals("1"))
    {
      return true;
    }
    else if (value.equals("false") || value.equals("no") ||
             value.equals("off") || value.equals("0"))
    {
      return false;
    }

    return defaultValue;
  }



  /**
   * Retrieves the value of the specified configuration parameter from the
   * configuration database.
   *
   * @param  parameterName  The name of the configuration parameter to
   *                        retrieve.
   * @param  defaultValue   The value to use for the parameter if the specified
   *                        key is not present in the database.
   *
   * @return  The value of the specified configuration parameter, or the
   *          provided default value if there is no such parameter.
   */
  public int getConfigParameter(String parameterName, int defaultValue)
  {
    String value = configHash.get(parameterName);
    if (value == null)
    {
      return defaultValue;
    }

    try
    {
      return Integer.parseInt(value);
    }
    catch (Exception e)
    {
      return defaultValue;
    }
  }



  /**
   * Sets the value of the specified parameter in the configuration database.
   *
   * @param  parameterName      The name of the parameter to set.
   * @param  parameterValue     The value to use for the parameter.
   * @param  notifySubscribers  Indicates whether the configuration subscribers
   *                            registered with the SLAMD server should be
   *                            notified of the change.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to store
   *                             the configuration parameter.
   */
  public void putConfigParameter(String parameterName, String parameterValue,
                                 boolean notifySubscribers)
         throws DatabaseException
  {
    OperationStatus status = put(null, configDB, parameterName,
                                 ASN1Element.getBytes(parameterValue));
    if (status != OperationStatus.SUCCESS)
    {
      throw new DatabaseException("Unexpected status returned from put:  " +
                                  status);
    }

    configHash.put(parameterName, parameterValue);

    if (! notifySubscribers)
    {
      return;
    }

    synchronized (configSubscribers)
    {
      for (int i=0; i < configSubscribers.size(); i++)
      {
        ConfigSubscriber s = configSubscribers.get(i);
        try
        {
          s.refreshSubscriberConfiguration(parameterName);
        }
        catch (SLAMDServerException sse)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Error while notifying subscriber " +
                                 s.getSubscriberName() + " of change to " +
                                 " configuration parameter " + parameterName +
                                 " with value " + parameterValue + " -- " +
                                 sse);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(sse));
        }
      }
    }
  }



  /**
   * Sets the value of the specified parameter in the configuration database.
   *
   * @param  parameterName      The name of the parameter to set.
   * @param  parameterValue     The value to use for the parameter.
   * @param  notifySubscribers  Indicates whether the configuration subscribers
   *                            registered with the SLAMD server should be
   *                            notified of the change.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to store
   *                             the configuration parameter.
   */
  public void putConfigParameter(String parameterName, boolean parameterValue,
                                 boolean notifySubscribers)
         throws DatabaseException
  {
    OperationStatus status =
         put(null, configDB, parameterName,
             ASN1Element.getBytes(String.valueOf(parameterValue)));
    if (status != OperationStatus.SUCCESS)
    {
      throw new DatabaseException("Unexpected status returned from put:  " +
                                  status);
    }

    configHash.put(parameterName, String.valueOf(parameterValue));

    if (! notifySubscribers)
    {
      return;
    }

    synchronized (configSubscribers)
    {
      for (int i=0; i < configSubscribers.size(); i++)
      {
        ConfigSubscriber s = configSubscribers.get(i);
        try
        {
          s.refreshSubscriberConfiguration(parameterName);
        }
        catch (SLAMDServerException sse)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Error while notifying subscriber " +
                                 s.getSubscriberName() + " of change to " +
                                 " configuration parameter " + parameterName +
                                 " with value " + parameterValue + " -- " +
                                 sse);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(sse));
        }
      }
    }
  }



  /**
   * Sets the value of the specified parameter in the configuration database.
   *
   * @param  parameterName      The name of the parameter to set.
   * @param  parameterValue     The value to use for the parameter.
   * @param  notifySubscribers  Indicates whether the configuration subscribers
   *                            registered with the SLAMD server should be
   *                            notified of the change.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to store
   *                             the configuration parameter.
   */
  public void putConfigParameter(String parameterName, int parameterValue,
                                 boolean notifySubscribers)
         throws DatabaseException
  {
    OperationStatus status =
         put(null, configDB, parameterName,
             ASN1Element.getBytes(String.valueOf(parameterValue)));
    if (status != OperationStatus.SUCCESS)
    {
      throw new DatabaseException("Unexpected status returned from put:  " +
                                  status);
    }

    configHash.put(parameterName, String.valueOf(parameterValue));

    if (! notifySubscribers)
    {
      return;
    }

    synchronized (configSubscribers)
    {
      for (int i=0; i < configSubscribers.size(); i++)
      {
        ConfigSubscriber s = configSubscribers.get(i);
        try
        {
          s.refreshSubscriberConfiguration(parameterName);
        }
        catch (SLAMDServerException sse)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Error while notifying subscriber " +
                                 s.getSubscriberName() + " of change to " +
                                 " configuration parameter " + parameterName +
                                 " with value " + parameterValue + " -- " +
                                 sse);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(sse));
        }
      }
    }
  }



  /**
   * Removes information about the specified parameter from the configuration
   * database.
   *
   * @param  parameterName  The name of the configuration parameter to remove
   *                        from the configuration database.
   *
   * @throws  DatabaseException  If  problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeConfigParameter(String parameterName)
         throws DatabaseException
  {
    delete(null, configDB, parameterName);
    configHash.remove(parameterName);

    synchronized (configSubscribers)
    {
      for (int i=0; i < configSubscribers.size(); i++)
      {
        ConfigSubscriber s = configSubscribers.get(i);
        try
        {
          s.refreshSubscriberConfiguration(parameterName);
        }
        catch (SLAMDServerException sse)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Error while notifying subscriber " +
                                 s.getSubscriberName() + " of removal of " +
                                 " configuration parameter " + parameterName +
                                 " -- " +  sse);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(sse));
        }
      }
    }
  }



  /**
   * Retrieves the specified folder from the configuration database.
   *
   * @param  folderName  The name of the folder to retrieve.
   *
   * @return  The requested folder, or <CODE>null</CODE> if no such folder
   *          exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           folder from the configuration database.
   */
  public JobFolder getFolder(String folderName)
         throws DatabaseException, DecodeException
  {
    byte[] folderBytes = get(null, folderDB, folderName, false);
    if (folderBytes == null)
    {
      return null;
    }

    return JobFolder.decode(folderBytes);
  }



  /**
   * Retrieves a list of all job folders defined in the configuration database.
   *
   * @return  A list of all job folders defined in the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public JobFolder[] getFolders()
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      ArrayList<JobFolder> folderList = new ArrayList<JobFolder>();

      CursorConfig cursorConfig = new CursorConfig();
      cursorConfig.setReadUncommitted(false);

      Cursor          cursor = folderDB.openCursor(null, cursorConfig);
      DatabaseEntry   key    = new DatabaseEntry();
      DatabaseEntry   value  = new DatabaseEntry();
      OperationStatus status = cursor.getFirst(key, value, LockMode.DEFAULT);
      while (status == OperationStatus.SUCCESS)
      {
        try
        {
          JobFolder folder = JobFolder.decode(value.getData());
          if (folder.getFolderName().equals(Constants.FOLDER_NAME_UNCLASSIFIED))
          {
            folderList.add(0, folder);
          }
          else
          {
            folderList.add(folder);
          }
        } catch (Exception e) {}

        status = cursor.getNext(key, value, LockMode.DEFAULT);
      }

      cursor.close();

      JobFolder[] folders = new JobFolder[folderList.size()];
      folderList.toArray(folders);
      return folders;
    }
  }



  /**
   * Writes information about the provided job folder into the configuration
   * database.  If the specified folder already exists, then it will be
   * replaced.
   *
   * @param  jobFolder  The folder to write to the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void writeFolder(JobFolder jobFolder)
         throws DatabaseException
  {
    put(null, folderDB, jobFolder.getFolderName(), jobFolder.encode());
  }



  /**
   * Removes information about the specified job folder from the configuration
   * database.  If the specified folder does not exist, then no action will be
   * taken.
   *
   * @param  folderName      The name of the job folder to remove from the
   *                         configuration database.
   * @param  deleteContents  Indicates whether to remove the jobs, optimizing
   *                         jobs, and uploaded files associated with the
   *                         folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeFolder(String folderName, boolean deleteContents)
         throws DatabaseException
  {
    Transaction txn = getTransaction();

    JobFolder folder;
    try
    {
      byte[] folderBytes = get(txn, folderDB, folderName, true);
      folder = JobFolder.decode(folderBytes);
    }
    catch (Exception e)
    {
      txn.abort();
      throw new DatabaseException("Unable to retrieve job folder " +
                                  folderName + " -- " + e, e);
    }


    String[] childNames = folder.getChildNames();
    if ((childNames != null) && (childNames.length > 0))
    {
      abortTransaction(txn);
      throw new DatabaseException("Unable to delete job folder " + folderName +
                                  " because it contains one or more child " +
                                  "folders.");
    }


    String[] jobIDs = folder.getJobIDs();
    if ((jobIDs != null) && (jobIDs.length > 0))
    {
      if (deleteContents)
      {
        try
        {
          for (int i=0; i < jobIDs.length; i++)
          {
            delete(txn, jobDB, jobIDs[i]);
          }
        }
        catch (Exception e)
        {
          abortTransaction(txn);
          throw new DatabaseException("Unable to delete jobs contained in " +
                                      "folder " + folderName + " -- " + e, e);
        }
      }
      else
      {
        abortTransaction(txn);
        throw new DatabaseException("Cannot delete job folder " + folderName +
                                    " because it still contains one or more " +
                                    "jobs.");
      }
    }


    String[] optimizingJobIDs = folder.getOptimizingJobIDs();
    if ((optimizingJobIDs != null) && (optimizingJobIDs.length > 0))
    {
      if (deleteContents)
      {
        try
        {
          for (int i=0; i < optimizingJobIDs.length; i++)
          {
            delete(txn, optimizingJobDB, optimizingJobIDs[i]);
          }
        }
        catch (Exception e)
        {
          abortTransaction(txn);
          throw new DatabaseException("Unable to delete optimizing jobs " +
                                      "contained in folder " + folderName +
                                      " -- " + e, e);
        }
      }
      else
      {
        abortTransaction(txn);
        throw new DatabaseException("Cannot delete job folder " + folderName +
                                    " because it still contains one or more " +
                                    "optimizing jobs.");
      }
    }


    String[] fileNames = folder.getFileNames();
    if ((fileNames != null) && (fileNames.length > 0))
    {
      if (deleteContents)
      {
        try
        {
          for (int i=0; i < fileNames.length; i++)
          {
            delete(txn, fileDB, folderName + '\t' + fileNames[i]);
          }
        }
        catch (Exception e)
        {
          abortTransaction(txn);
          throw new DatabaseException("Unable to delete uploaded files " +
                                      "contained in job folder " + folderName +
                                      " -- " + e, e);
        }
      }
      else
      {
        abortTransaction(txn);
        throw new DatabaseException("Cannot delete job folder " + folderName +
                                    " because it still contains one or more " +
                                    "uploaded files.");
      }
    }


    try
    {
      delete(txn, folderDB, folderName);
      commitTransaction(txn);
    }
    catch (Exception e)
    {
      abortTransaction(txn);
      throw new DatabaseException("Cannot delete job folder " + folderName +
                                  " -- " + e, e);
    }
  }



  /**
   * Retrieves the specified virtual folder from the configuration database.
   *
   * @param  folderName  The name of the virtual folder to retrieve.
   *
   * @return  The requested virtual folder, or <CODE>null</CODE> if no such
   *          folder exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           virtual folder from the configuration database.
   */
  public JobFolder getVirtualFolder(String folderName)
         throws DatabaseException, DecodeException
  {
    byte[] folderBytes = get(null, virtualFolderDB, folderName, false);
    if (folderBytes == null)
    {
      return null;
    }

    return JobFolder.decode(folderBytes);
  }



  /**
   * Retrieves a list of all virtual job folders defined in the configuration
   * database.
   *
   * @return  A list of all virtual job folders defined in the configuration
   *          database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public JobFolder[] getVirtualFolders()
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      ArrayList<JobFolder> folderList = new ArrayList<JobFolder>();

      CursorConfig cursorConfig = new CursorConfig();
      cursorConfig.setReadUncommitted(false);

      Cursor          cursor = virtualFolderDB.openCursor(null, cursorConfig);
      DatabaseEntry   key    = new DatabaseEntry();
      DatabaseEntry   value  = new DatabaseEntry();
      OperationStatus status = cursor.getFirst(key, value, LockMode.DEFAULT);
      while (status == OperationStatus.SUCCESS)
      {
        try
        {
          folderList.add(JobFolder.decode(value.getData()));
        } catch (Exception e) {}

        status = cursor.getNext(key, value, LockMode.DEFAULT);
      }
      cursor.close();

      JobFolder[] folders = new JobFolder[folderList.size()];
      folderList.toArray(folders);
      return folders;
    }
  }



  /**
   * Writes information about the provided virtual job folder into the
   * configuration database.  If the specified folder already exists, then it
   * will be replaced.
   *
   * @param  jobFolder  The virtual folder to write to the configuration
   *                    database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void writeVirtualFolder(JobFolder jobFolder)
         throws DatabaseException
  {
    put(null, virtualFolderDB, jobFolder.getFolderName(), jobFolder.encode());
  }



  /**
   * Removes information about the specified virtual job folder from the
   * configuration database.  If the specified folder does not exist, then no
   * action will be taken.
   *
   * @param  folderName  The name of the virtual job folder to remove from the
   *                     configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeVirtualFolder(String folderName)
         throws DatabaseException
  {
    delete(null, virtualFolderDB, folderName);
  }



  /**
   * Retrieves the specified job from the configuration database.
   *
   * @param  jobID  The job ID of the job to retrieve from the database.
   *
   * @return  The requested job from the configuration database, or
   *          <CODE>null</CODE> if no such job exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while decoding the job
   *                           information.
   */
  public Job getJob(String jobID)
         throws DatabaseException, DecodeException
  {
    byte[] jobBytes = get(null, jobDB, jobID, false);
    if (jobBytes == null)
    {
      return null;
    }

    return Job.decode(slamdServer, jobBytes);
  }



  /**
   * Retrieves summary information for the specified job from the configuration
   * database.
   *
   * @param  jobID  The job ID of the job to retrieve from the database.
   *
   * @return  Summary information for the requested job from the configuration
   *          database, or <CODE>null</CODE> if no such job exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while decoding the job
   *                           information.
   */
  public Job getSummaryJob(String jobID)
         throws DatabaseException, DecodeException
  {
    byte[] jobBytes = get(null, jobDB, jobID, false);
    if (jobBytes == null)
    {
      return null;
    }

    return Job.decodeSummaryJob(slamdServer, jobBytes);
  }



  /**
   * Retrieves the set of jobs contained in the specified folder of the
   * configuration database.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     associated jobs.
   *
   * @return  The set of jobs contained in the specified folder of the
   *          configuration database, or <CODE>null</CODE> if there is no such
   *          folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified folder.
   */
  public Job[] getJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  jobIDs  = folder.getJobIDs();
    ArrayList<Job> jobList = new ArrayList<Job>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        Job job = getJob(jobIDs[i]);
        if (job != null)
        {
          jobList.add(job);
        }
      } catch (Exception e) {}
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    Arrays.sort(jobs);
    return jobs;
  }



  /**
   * Retrieves the set of completed jobs contained in the specified folder of
   * the configuration database.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     associated jobs.
   *
   * @return  The set of completed jobs contained in the specified folder of the
   *          configuration database, or <CODE>null</CODE> if there is no such
   *          folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified folder.
   */
  public Job[] getCompletedJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  jobIDs  = folder.getJobIDs();
    ArrayList<Job> jobList = new ArrayList<Job>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        Job job = getJob(jobIDs[i]);
        if (job.doneRunning())
        {
          jobList.add(job);
        }
      } catch (Exception e) {}
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    Arrays.sort(jobs);
    return jobs;
  }



  /**
   * Retrieves the set of jobs contained in the specified virtual folder of the
   * configuration database.
   *
   * @param  folderName  The name of the virtual folder for which to retrieve
   *                     the associated jobs.
   *
   * @return  The set of jobs contained in the specified virtual folder of the
   *          configuration database, or <CODE>null</CODE> if there is no such
   *          folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified virtual folder.
   */
  public Job[] getVirtualJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getVirtualFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  jobIDs  = folder.getJobIDs();
    ArrayList<Job> jobList = new ArrayList<Job>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        Job job = getJob(jobIDs[i]);
        if (job != null)
        {
          jobList.add(job);
        }
      } catch (Exception e) {}
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    Arrays.sort(jobs);
    return jobs;
  }



  /**
   * Retrieves summary information for the set of jobs contained in the
   * specified folder of the configuration database.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     associated jobs.
   *
   * @return  Summary information about the set of jobs contained in the
   *          specified folder of the configuration database, or
   *          <CODE>null</CODE> if there is no such folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified folder.
   */
  public Job[] getSummaryJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  jobIDs  = folder.getJobIDs();
    ArrayList<Job> jobList = new ArrayList<Job>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        Job job = getSummaryJob(jobIDs[i]);
        if (job != null)
        {
          jobList.add(job);
        }
      } catch (Exception e) {}
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    Arrays.sort(jobs);
    return jobs;
  }



  /**
   * Retrieves summary information for the set of completed jobs contained in
   * the specified folder of the configuration database.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     associated jobs.
   *
   * @return  Summary information about the set of completed jobs contained in
   *          the specified folder of the configuration database, or
   *          <CODE>null</CODE> if there is no such folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified folder.
   */
  public Job[] getCompletedSummaryJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  jobIDs  = folder.getJobIDs();
    ArrayList<Job> jobList = new ArrayList<Job>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        Job job = getSummaryJob(jobIDs[i]);
        if (job.doneRunning())
        {
          jobList.add(job);
        }
      } catch (Exception e) {}
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    Arrays.sort(jobs);
    return jobs;
  }



  /**
   * Retrieves summary information for the set of jobs contained in the
   * specified virtual folder of the configuration database.
   *
   * @param  folderName  The name of the virtual folder for which to retrieve
   *                     the associated jobs.
   *
   * @return  Summary information about the set of jobs contained in the
   *          specified virtual folder of the configuration database, or
   *          <CODE>null</CODE> if there is no such folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified virtual folder.
   */
  public Job[] getSummaryVirtualJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getVirtualFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  jobIDs  = folder.getJobIDs();
    ArrayList<Job> jobList = new ArrayList<Job>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        jobList.add(getSummaryJob(jobIDs[i]));
      } catch (Exception e) {}
    }

    Job[] jobs = new Job[jobList.size()];
    jobList.toArray(jobs);
    Arrays.sort(jobs);
    return jobs;
  }



  /**
   * Writes information about the provided job into the configuration database.
   * If the specified job already exists, it will be overwritten.  Otherwise, it
   * will be added.
   *
   * @param  job  The job to write to the database.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to write
   *                             the job information.
   */
  public void writeJob(Job job)
         throws DatabaseException
  {
    // First, see if the job already exists in the configuration database.
    Job j = null;
    try
    {
      j = getJob(job.getJobID());
    }
    catch (DecodeException de)
    {
      // The job exists but could not be decoded for some reason.  That's fine,
      // since we're going to overwrite it anyway.
      j = null;
    }


    // If the job exists, then just overwrite it.  If not, then store it and
    // update the folder in which the job is stored.
    if (j != null)
    {
      put(null, jobDB, job.getJobID(), job.encode());
    }
    else
    {
      Transaction txn = getTransaction();

      try
      {
        byte[] folderBytes = get(txn, folderDB, job.getFolderName(), true);
        JobFolder folder = JobFolder.decode(folderBytes);
        folder.addJobID(job.getJobID());
        put(txn, folderDB, job.getFolderName(), folder.encode());
        put(txn, jobDB, job.getJobID(), job.encode());
        commitTransaction(txn);
      }
      catch (DatabaseException de)
      {
        abortTransaction(txn);
        throw de;
      }
      catch (Exception e)
      {
        abortTransaction(txn);
        String message = "Unexpected exception caught while adding job " +
                         "data:  " + e;
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY, message);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DatabaseException(message, e);
      }
    }


    // Check to see if the job is in the disabled, pending, or running job lists
    // and update them as necessary.
    boolean isDisabled = (job.getJobState() == Constants.JOB_STATE_DISABLED);
    boolean isPending =
         (job.getJobState() == Constants.JOB_STATE_NOT_YET_STARTED);
    boolean isRunning = (job.getJobState() == Constants.JOB_STATE_RUNNING);

    if (disabledJobs.contains(job.getJobID()))
    {
      if (! isDisabled)
      {
        synchronized (disabledJobs)
        {
          disabledJobs.remove(job.getJobID());
          writeDisabledJobList();
        }
      }
    }
    else if (isDisabled)
    {
      synchronized (disabledJobs)
      {
        disabledJobs.add(job.getJobID());
        writeDisabledJobList();
      }
    }

    if (pendingJobs.contains(job.getJobID()))
    {
      if (! isPending)
      {
        synchronized (pendingJobs)
        {
          pendingJobs.remove(job.getJobID());
          writePendingJobList();
        }
      }
    }
    else if (isPending)
    {
      synchronized (pendingJobs)
      {
        pendingJobs.add(job.getJobID());
        writePendingJobList();
      }
    }

    if (runningJobs.contains(job.getJobID()))
    {
      if (! isRunning)
      {
        synchronized (runningJobs)
        {
          runningJobs.remove(job.getJobID());
          writeRunningJobList();
        }
      }
    }
    else if (isRunning)
    {
      synchronized (runningJobs)
      {
        runningJobs.add(job.getJobID());
        writeRunningJobList();
      }
    }
  }



  /**
   * Writes the list of disabled jobs to the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to write
   *                             the disabled job list.
   */
  private void writeDisabledJobList()
          throws DatabaseException
  {
    StringBuilder buffer = new StringBuilder();
    if (! disabledJobs.isEmpty())
    {
      Iterator iterator = disabledJobs.iterator();
      buffer.append((String) iterator.next());

      while (iterator.hasNext())
      {
        buffer.append('\n');
        buffer.append((String) iterator.next());
      }
    }

    byte[] disabledJobBytes = ASN1Element.getBytes(buffer.toString());
    put(null, configDB, Constants.PARAM_DISABLED_JOBS, disabledJobBytes);
  }



  /**
   * Retrieves an array containing the job IDs of the jobs that are currently
   * disabled.
   *
   * @return  An array containing the job IDs of the jobs that are currently
   *          disabled.
   */
  public String[] getDisabledJobIDs()
  {
    synchronized (disabledJobs)
    {
      String[] disabledJobIDs = new String[disabledJobs.size()];
      return disabledJobs.toArray(disabledJobIDs);
    }
  }



  /**
   * Retrieves an array containing the set of jobs that are currently disabled.
   *
   * @return  An array containing the set of jobs that are currently disabled.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             database.
   */
  public Job[] getDisabledJobs()
         throws DatabaseException
  {
    synchronized (disabledJobs)
    {
      ArrayList<Job> disabledJobList = new ArrayList<Job>();
      Iterator iterator = disabledJobs.iterator();
      while (iterator.hasNext())
      {
        String jobID = (String) iterator.next();
        byte[] jobBytes = get(null, jobDB, jobID, false);
        if (jobBytes == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Could not find disabled job " + jobID);
          continue;
        }

        try
        {
          disabledJobList.add(Job.decode(slamdServer, jobBytes));
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to decode job " + jobID + " -- " + e);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
        }
      }

      Job[] disabledJobs = new Job[disabledJobList.size()];
      disabledJobList.toArray(disabledJobs);
      Arrays.sort(disabledJobs);
      return disabledJobs;
    }
  }



  /**
   * Writes the list of pending jobs to the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to write
   *                             the pending job list.
   */
  private void writePendingJobList()
          throws DatabaseException
  {
    StringBuilder buffer = new StringBuilder();
    if (! pendingJobs.isEmpty())
    {
      Iterator<String> iterator = pendingJobs.iterator();
      buffer.append(iterator.next());

      while (iterator.hasNext())
      {
        buffer.append('\n');
        buffer.append(iterator.next());
      }
    }

    byte[] pendingJobBytes = ASN1Element.getBytes(buffer.toString());
    put(null, configDB, Constants.PARAM_PENDING_JOBS, pendingJobBytes);
  }



  /**
   * Retrieves an array containing the job IDs of the jobs that are currently
   * pending execution.
   *
   * @return  An array containing the job IDs of the jobs that are currently
   *          pending execution.
   */
  public String[] getPendingJobIDs()
  {
    synchronized (pendingJobs)
    {
      String[] pendingJobIDs = new String[pendingJobs.size()];
      return pendingJobs.toArray(pendingJobIDs);
    }
  }



  /**
   * Retrieves an array containing the set of jobs that are currently pending
   * execution.
   *
   * @return  An array containing the set of jobs that are currently pending
   *          execution.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             database.
   */
  public Job[] getPendingJobs()
         throws DatabaseException
  {
    synchronized (pendingJobs)
    {
      ArrayList<Job> pendingJobList = new ArrayList<Job>();
      Iterator<String> iterator = pendingJobs.iterator();
      while (iterator.hasNext())
      {
        String jobID = iterator.next();
        byte[] jobBytes = get(null, jobDB, jobID, false);
        if (jobBytes == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Could not find pending job " + jobID);
          continue;
        }

        try
        {
          pendingJobList.add(Job.decode(slamdServer, jobBytes));
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to decode job " + jobID + " -- " + e);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
        }
      }

      Job[] pendingJobs = new Job[pendingJobList.size()];
      pendingJobList.toArray(pendingJobs);
      Arrays.sort(pendingJobs);
      return pendingJobs;
    }
  }



  /**
   * Writes the list of running jobs to the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to write
   *                             the running job list.
   */
  private void writeRunningJobList()
          throws DatabaseException
  {
    StringBuilder buffer = new StringBuilder();
    if (! runningJobs.isEmpty())
    {
      Iterator<String> iterator = runningJobs.iterator();
      buffer.append(iterator.next());

      while (iterator.hasNext())
      {
        buffer.append('\n');
        buffer.append(iterator.next());
      }
    }

    byte[] runningJobBytes = ASN1Element.getBytes(buffer.toString());
    put(null, configDB, Constants.PARAM_RUNNING_JOBS, runningJobBytes);
  }



  /**
   * Retrieves an array containing the job IDs of the jobs that are currently
   * running.
   *
   * @return  An array containing the job IDs of the jobs that are currently
   *          running.
   */
  public String[] getRunningJobIDs()
  {
    synchronized (runningJobs)
    {
      String[] runningJobIDs = new String[runningJobs.size()];
      return runningJobs.toArray(runningJobIDs);
    }
  }



  /**
   * Retrieves an array containing the set of jobs that are currently running.
   *
   * @return  An array containing the set of jobs that are currently running.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             database.
   */
  public Job[] getRunningJobs()
         throws DatabaseException
  {
    synchronized (runningJobs)
    {
      ArrayList<Job> runningJobList = new ArrayList<Job>();
      Iterator<String> iterator = runningJobs.iterator();
      while (iterator.hasNext())
      {
        String jobID = iterator.next();
        byte[] jobBytes = get(null, jobDB, jobID, false);
        if (jobBytes == null)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Could not find running job " + jobID);
          continue;
        }

        try
        {
          runningJobList.add(Job.decode(slamdServer, jobBytes));
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to decode job " + jobID + " -- " + e);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
        }
      }

      Job[] runningJobs = new Job[runningJobList.size()];
      runningJobList.toArray(runningJobs);
      Arrays.sort(runningJobs);
      return runningJobs;
    }
  }



  /**
   * Removes information about the specified job from the configuration
   * database.
   *
   * @param  jobID  The job ID of the job to remove from the configuration
   *                database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeJob(String jobID)
         throws DatabaseException
  {
    Job j = null;
    try
    {
      j = getJob(jobID);
    }
    catch (DecodeException de)
    {
      // This means that the job exists but can't be decoded for some reason.
      // In this case, just remove the job.
      delete(null, jobDB, jobID);
      return;
    }


    // At this point, we need to remove the job as well as the reference to the
    // job in its corresponding folder.  First, create a transaction to use to
    // protect the entire operation.
    Transaction txn = getTransaction();

    try
    {
      byte[] folderBytes = get(txn, folderDB, j.getFolderName(), true);
      JobFolder folder = JobFolder.decode(folderBytes);
      folder.removeJobID(jobID);
      put(txn, folderDB, j.getFolderName(), folder.encode());
      delete(txn, jobDB, jobID);
      commitTransaction(txn);
    }
    catch (DatabaseException de)
    {
      abortTransaction(txn);
      throw de;
    }
    catch (Exception e)
    {
      abortTransaction(txn);

      String message = "Unexpected exception caught while removing job:  " + e;
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DatabaseException(message, e);
    }
  }



  /**
   * Moves the specified job from its current folder to the new folder.
   *
   * @param  jobID       The job ID of the job to move.
   * @param  folderName  The name of the new folder in which to place the job.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void moveJob(String jobID, String folderName)
         throws DatabaseException
  {
    Transaction txn = getTransaction();

    try
    {
      Job j = getJob(jobID);

      byte[] currentFolderBytes = get(txn, folderDB, j.getFolderName(), true);
      JobFolder currentFolder = JobFolder.decode(currentFolderBytes);
      currentFolder.removeJobID(jobID);
      put(txn, folderDB, j.getFolderName(), currentFolder.encode());

      byte[] newFolderBytes = get(txn, folderDB, folderName, true);
      JobFolder newFolder = JobFolder.decode(newFolderBytes);
      newFolder.addJobID(jobID);
      put(txn, folderDB, folderName, newFolder.encode());

      j.setFolderName(folderName);
      put(txn, jobDB, jobID, j.encode());

      commitTransaction(txn);
    }
    catch (DatabaseException de)
    {
      abortTransaction(txn);
      throw de;
    }
    catch (Exception e)
    {
      abortTransaction(txn);

      String message = "Unexpected exception caught while attempting to move " +
                       "job:  " + e;
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DatabaseException(message, e);
    }
  }



  /**
   * Retrieves the specified optimizing job from the configuration database.
   *
   * @param  optimizingJobID  The ID of the optimizing job to retrieve from the
   *                          database.
   *
   * @return  The requested optimizing job from the configuration database, or
   *          <CODE>null</CODE> if no such job exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while decoding the optimizing
   *                           job information.
   */
  public OptimizingJob getOptimizingJob(String optimizingJobID)
         throws DatabaseException, DecodeException
  {
    byte[] jobBytes = get(null, optimizingJobDB, optimizingJobID, false);
    if (jobBytes == null)
    {
      return null;
    }

    return OptimizingJob.decode(slamdServer, jobBytes);
  }



  /**
   * Retrieves summary information for the specified optimizing job from the
   * configuration database.
   *
   * @param  optimizingJobID  The ID of the optimizing job to retrieve from the
   *                          database.
   *
   * @return  Summary information for the requested optimizing job from the
   *          configuration database, or <CODE>null</CODE> if no such job
   *          exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while decoding the optimizing
   *                           job information.
   */
  public OptimizingJob getSummaryOptimizingJob(String optimizingJobID)
         throws DatabaseException, DecodeException
  {
    byte[] jobBytes = get(null, optimizingJobDB, optimizingJobID, false);
    if (jobBytes == null)
    {
      return null;
    }

    return OptimizingJob.decodeSummary(slamdServer, jobBytes);
  }



  /**
   * Retrieves the set of optimizing jobs contained in the specified folder of
   * the configuration database.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     associated optimizing jobs.
   *
   * @return  The set of optimizing jobs contained in the specified folder of
   *          the configuration database, or <CODE>null</CODE> if there is no
   *          such folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified folder.
   */
  public OptimizingJob[] getOptimizingJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  optimizingJobIDs  = folder.getOptimizingJobIDs();
    ArrayList<OptimizingJob> optimizingJobList =
         new ArrayList<OptimizingJob>(optimizingJobIDs.length);
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      try
      {
        OptimizingJob oj = getOptimizingJob(optimizingJobIDs[i]);
        if (oj != null)
        {
          optimizingJobList.add(oj);
        }
      } catch (Exception e) {}
    }

    OptimizingJob[] optimizingJobs =
         new OptimizingJob[optimizingJobList.size()];
    optimizingJobList.toArray(optimizingJobs);
    Arrays.sort(optimizingJobs);
    return optimizingJobs;
  }



  /**
   * Retrieves summary information for the set of optimizing jobs contained in
   * the specified folder of the configuration database.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     associated optimizing jobs.
   *
   * @return  Summary information for the set of optimizing jobs contained in
   *          the specified folder of the configuration database, or
   *          <CODE>null</CODE> if there is no such folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the specified folder.
   */
  public OptimizingJob[] getSummaryOptimizingJobs(String folderName)
         throws DatabaseException, DecodeException
  {
    JobFolder folder = getFolder(folderName);
    if (folder == null)
    {
      return null;
    }

    String[]  optimizingJobIDs  = folder.getOptimizingJobIDs();
    ArrayList<OptimizingJob> optimizingJobList =
         new ArrayList<OptimizingJob>(optimizingJobIDs.length);
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      try
      {
        OptimizingJob oj = getSummaryOptimizingJob(optimizingJobIDs[i]);
        if (oj != null)
        {
          optimizingJobList.add(oj);
        }
      } catch (Exception e) {}
    }

    OptimizingJob[] optimizingJobs =
         new OptimizingJob[optimizingJobList.size()];
    optimizingJobList.toArray(optimizingJobs);
    Arrays.sort(optimizingJobs);
    return optimizingJobs;
  }



  /**
   * Writes information about the provided optimizing job into the configuration
   * database.  If the specified optimizing job already exists, it will be
   * overwritten.  Otherwise, it will be added.
   *
   * @param  optimizingJob  The optimizing job to write to the database.
   *
   * @throws  DatabaseException  If a problem occurs while attempting to write
   *                             the optimizing job information.
   */
  public void writeOptimizingJob(OptimizingJob optimizingJob)
         throws DatabaseException
  {
    // First, see if the optimizing job already exists in the configuration
    // database.
    OptimizingJob oj = null;
    try
    {
      oj = getOptimizingJob(optimizingJob.getOptimizingJobID());
    }
    catch (DecodeException de)
    {
      // The job exists but could not be decoded for some reason.  That's fine,
      // since we're going to overwrite it anyway.
      oj = null;
    }


    // If the job exists, then just overwrite it.  If not, then store it and
    // update the folder in which the job is stored.
    if (oj != null)
    {
      put(null, optimizingJobDB, optimizingJob.getOptimizingJobID(),
          optimizingJob.encode());
    }
    else
    {
      Transaction txn = getTransaction();

      try
      {
        byte[] folderBytes = get(txn, folderDB, optimizingJob.getFolderName(),
                                 true);
        JobFolder folder = JobFolder.decode(folderBytes);
        folder.addOptimizingJobID(optimizingJob.getOptimizingJobID());
        put(txn, folderDB, optimizingJob.getFolderName(), folder.encode());
        put(txn, optimizingJobDB, optimizingJob.getOptimizingJobID(),
            optimizingJob.encode());
        commitTransaction(txn);
      }
      catch (DatabaseException de)
      {
        abortTransaction(txn);
        throw de;
      }
      catch (Exception e)
      {
        abortTransaction(txn);

        String message = "Unexpected exception caught while adding " +
                         "optimizing job data:  " + e;
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY, message);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DatabaseException(message, e);
      }
    }
  }



  /**
   * Removes information about the specified optimizing job from the
   * configuration database.
   *
   * @param  optimizingJobID  The ID of the optimizing job to remove from the
   *                          configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeOptimizingJob(String optimizingJobID)
         throws DatabaseException
  {
    OptimizingJob oj = null;
    try
    {
      oj = getOptimizingJob(optimizingJobID);
    }
    catch (DecodeException de)
    {
      // This means that the job exists but can't be decoded for some reason.
      // In this case, just remove the job.
      delete(null, optimizingJobDB, optimizingJobID);
      return;
    }


    // At this point, we need to remove the job as well as the reference to the
    // job in its corresponding folder.  First, create a transaction to use to
    // protect the entire operation.
    Transaction txn = getTransaction();

    try
    {
      byte[] folderBytes = get(txn, folderDB, oj.getFolderName(), true);
      JobFolder folder = JobFolder.decode(folderBytes);
      folder.removeOptimizingJobID(optimizingJobID);
      put(txn, folderDB, oj.getFolderName(), folder.encode());
      delete(txn, optimizingJobDB, optimizingJobID);
      commitTransaction(txn);
    }
    catch (DatabaseException de)
    {
      abortTransaction(txn);
      throw de;
    }
    catch (Exception e)
    {
      abortTransaction(txn);

      String message = "Unexpected exception caught while removing " +
                       "optimizing job:  " + e;
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DatabaseException(message, e);
    }
  }



  /**
   * Moves the specified optimizing job from its current folder to the new
   * folder.
   *
   * @param  optimizingJobID  The job ID of the job to move.
   * @param  folderName       The name of the new folder in which to place the
   *                          optimizing job.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void moveOptimizingJob(String optimizingJobID, String folderName)
         throws DatabaseException
  {
    Transaction txn = getTransaction();

    try
    {
      OptimizingJob oj = getOptimizingJob(optimizingJobID);

      byte[] currentFolderBytes = get(txn, folderDB, oj.getFolderName(), true);
      JobFolder currentFolder = JobFolder.decode(currentFolderBytes);
      currentFolder.removeOptimizingJobID(optimizingJobID);
      put(txn, folderDB, oj.getFolderName(), currentFolder.encode());

      byte[] newFolderBytes = get(txn, folderDB, folderName, true);
      JobFolder newFolder = JobFolder.decode(newFolderBytes);
      newFolder.addOptimizingJobID(optimizingJobID);
      put(txn, folderDB, folderName, newFolder.encode());

      oj.setFolderName(folderName);
      put(txn, optimizingJobDB, optimizingJobID, oj.encode());

      commitTransaction(txn);
    }
    catch (DatabaseException de)
    {
      abortTransaction(txn);
      throw de;
    }
    catch (Exception e)
    {
      abortTransaction(txn);

      String message = "Unexpected exception caught while attempting to move " +
                       "optimizing job:  " + e;
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DatabaseException(message, e);
    }
  }



  /**
   * Retrieves a list of all job groups defined in the configuration database.
   *
   * @return  A list of all job groups defined in the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public JobGroup[] getJobGroups()
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      ArrayList<JobGroup> jobGroupList = new ArrayList<JobGroup>();

      CursorConfig cursorConfig = new CursorConfig();
      cursorConfig.setReadUncommitted(false);

      Cursor          cursor = jobGroupDB.openCursor(null, cursorConfig);
      DatabaseEntry   key    = new DatabaseEntry();
      DatabaseEntry   value  = new DatabaseEntry();
      OperationStatus status = cursor.getFirst(key, value, LockMode.DEFAULT);
      while (status == OperationStatus.SUCCESS)
      {
        try
        {
          jobGroupList.add(JobGroup.decode(slamdServer, value.getData()));
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to decode job the job group with " +
                                 "name " + (new String(key.getData())) + ":  " +
                                 e.getMessage());
        }

        status = cursor.getNext(key, value, LockMode.DEFAULT);
      }

      cursor.close();

      JobGroup[] jobGroups = new JobGroup[jobGroupList.size()];
      jobGroupList.toArray(jobGroups);
      return jobGroups;
    }
  }



  /**
   * Retrieves a list of all job groups defined in the configuration database.
   * The returned list will only contain summary information for each group.
   *
   * @return  A list of all job groups defined in the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public JobGroup[] getSummaryJobGroups()
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      ArrayList<JobGroup> jobGroupList = new ArrayList<JobGroup>();

      CursorConfig cursorConfig = new CursorConfig();
      cursorConfig.setReadUncommitted(false);

      Cursor          cursor = jobGroupDB.openCursor(null, cursorConfig);
      DatabaseEntry   key    = new DatabaseEntry();
      DatabaseEntry   value  = new DatabaseEntry();
      OperationStatus status = cursor.getFirst(key, value, LockMode.DEFAULT);
      while (status == OperationStatus.SUCCESS)
      {
        try
        {
          jobGroupList.add(JobGroup.decodeSummary(value.getData()));
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));

          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to decode job the job group with " +
                                 "name " + (new String(key.getData())) + ":  " +
                                 e.getMessage());
        }

        status = cursor.getNext(key, value, LockMode.DEFAULT);
      }

      cursor.close();

      JobGroup[] jobGroups = new JobGroup[jobGroupList.size()];
      jobGroupList.toArray(jobGroups);
      return jobGroups;
    }
  }



  /**
   * Retrieves the requested job group from the configuration database.
   *
   * @param  jobGroupName  The name of the job group to retrieve.
   *
   * @return  The requested job group, or <CODE>null</CODE> if no such job group
   *          exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           requested job group information.
   */
  public JobGroup getJobGroup(String jobGroupName)
         throws DatabaseException, DecodeException
  {
    byte[] jobGroupBytes = get(null, jobGroupDB, jobGroupName, false);
    if (jobGroupBytes == null)
    {
      return null;
    }

    return JobGroup.decode(slamdServer, jobGroupBytes);
  }



  /**
   * Writes information about the provided job group to the configuration
   * database.  If the specified job group already exists, it will be
   * overwritten.  Otherwise, a new record will be created.
   *
   * @param  jobGroup  The job group to be written to the configuration
   *                   database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void writeJobGroup(JobGroup jobGroup)
         throws DatabaseException
  {
    put(null, jobGroupDB, jobGroup.getName(), jobGroup.encode());
  }



  /**
   * Removes information about the specified job group from the configuration
   * database.  If it does not exist, then no action will be taken.
   *
   * @param  jobGroupName  The name of the job group to remove from the
   *                       configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeJobGroup(String jobGroupName)
         throws DatabaseException
  {
    delete(null, jobGroupDB, jobGroupName);
  }



  /**
   * Retrieves the specified uploaded file from the configuration database.
   *
   * @param  folderName  The name of the folder in which the specified file is
   *                     located.
   * @param  fileName    The name of the uploaded file to retrieve.
   *
   * @return  The requested uploaded file, or <CODE>null</CODE> if no such file
   *          exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If the uploaded file cannot be decoded for some
   *                           reason.
   */
  public UploadedFile getUploadedFile(String folderName, String fileName)
         throws DatabaseException, DecodeException
  {
    String key = folderName + '\t' + fileName;
    byte[] fileBytes = get(null, fileDB, key, false);
    if (fileBytes == null)
    {
      return null;
    }

    return UploadedFile.decode(fileBytes);
  }



  /**
   * Retrieves the specified uploaded file from the configuration database.  The
   * file returned will not include the actual file data.
   *
   * @param  folderName  The name of the folder in which the specified file is
   *                     located.
   * @param  fileName    The name of the uploaded file to retrieve.
   *
   * @return  The requested uploaded file, or <CODE>null</CODE> if no such file
   *          exists.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   *
   * @throws  DecodeException  If the uploaded file cannot be decoded for some
   *                           reason.
   */
  public UploadedFile getUploadedFileWithoutData(String folderName,
                                                 String fileName)
         throws DatabaseException, DecodeException
  {
    String key = folderName + '\t' + fileName;
    byte[] fileBytes = get(null, fileDB, key, false);
    if (fileBytes == null)
    {
      return null;
    }

    return UploadedFile.decodeWithoutData(fileBytes);
  }



  /**
   * Retrieves the set of uploaded files associated with the specified folder.
   * The files returned will not include the actual file data.
   *
   * @param  folderName  The name of the folder for which to retrieve the
   *                     uploaded files.
   *
   * @return  The set of uploaded files associated with the specified folder, or
   *          <CODE>null</CODE> if there is no such folder.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public UploadedFile[] getUploadedFiles(String folderName)
         throws DatabaseException
  {
    JobFolder folder = null;
    try
    {
      folder = getFolder(folderName);
    }
    catch (DecodeException de)
    {
      String message = "Unable to decode job folder:  " + de;
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(de));
      throw new DatabaseException(message, de);
    }


    if (folder == null)
    {
      return null;
    }


    String[] fileNames = folder.getFileNames();
    ArrayList<UploadedFile> fileList = new ArrayList<UploadedFile>();
    for (int i=0; i < fileNames.length; i++)
    {
      try
      {
        fileList.add(getUploadedFileWithoutData(folderName, fileNames[i]));
      } catch (Exception e) {}
    }


    UploadedFile[] uploadedFiles = new UploadedFile[fileList.size()];
    fileList.toArray(uploadedFiles);
    return uploadedFiles;
  }



  /**
   * Writes information about the provided uploaded file into the configuration
   * database.  If the file already exists, then it will be overwritten.
   * Otherwise, it will be added and the corresponding folder will be updated.
   *
   * @param  uploadedFile  The uploaded file to be written to the configuration
   *                       database.
   * @param  folderName    The name of the folder in which the specified file
   *                       should be placed.
   *
   * @throws  DatabaseException  If a problem occurs while writing information
   *                             about the specified file to the configuration
   *                             database.
   */
  public void writeUploadedFile(UploadedFile uploadedFile, String folderName)
         throws DatabaseException
  {
    String key = folderName + '\t' + uploadedFile.getFileName();

    UploadedFile currentFile;
    try
    {
      currentFile = getUploadedFileWithoutData(folderName,
                                               uploadedFile.getFileName());
    }
    catch (DecodeException de)
    {
      // This means that the file exists but cannot be decoded for some reason.
      // In this case, just overwrite that file.
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(de));
      put(null, fileDB, key, uploadedFile.encode());
      return;
    }


    if (currentFile == null)
    {
      // The file does not exist, so add it and also update the corresponding
      // folder.  Use a transaction to wrap all these operations.
      Transaction txn = getTransaction();

      try
      {
        byte[] folderBytes = get(txn, folderDB, folderName, true);
        JobFolder folder = JobFolder.decode(folderBytes);
        folder.addFileName(uploadedFile.getFileName());
        put(txn, folderDB, folderName, folder.encode());
        put(txn, fileDB, key, uploadedFile.encode());
        commitTransaction(txn);
      }
      catch (DatabaseException de)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(de));
        abortTransaction(txn);
        throw de;
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        abortTransaction(txn);

        String message = "Unexpected exception caught while adding uploaded " +
                         "file to DB:  " + e;
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new DatabaseException(message, e);
      }
    }
    else
    {
      // The file already exists, so just update it in the file database.
      put(null, fileDB, key, uploadedFile.encode());
    }
  }



  /**
   * Removes information about the specified uploaded file from the
   * configuration database.
   *
   * @param  folderName  The name of the folder from which to remove the file.
   * @param  fileName    The name of the uploaded file to remove.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeUploadedFile(String folderName, String fileName)
         throws DatabaseException
  {
    // We need to both remove the file and update the associated folder.  Do
    // this under a transaction.
    Transaction txn = getTransaction();

    try
    {
      byte[] folderBytes = get(txn, folderDB, folderName, true);
      JobFolder folder = JobFolder.decode(folderBytes);
      folder.removeFileName(fileName);
      put(txn, folderDB, folderName, folder.encode());
      delete(txn, fileDB, folderName + '\t' + fileName);
      commitTransaction(txn);
    }
    catch (DatabaseException de)
    {
      abortTransaction(txn);
      throw de;
    }
    catch (Exception e)
    {
      abortTransaction(txn);

      String message = "Unexpected exception caught while removing uploaded " +
                       "file:  " + e;
      slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(e));
      throw new DatabaseException(message, e);
    }
  }



  /**
   * Retrieves the set of job classes defined in the configuration database.
   *
   * @return  The set of job classes defined in the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while obtaining the set of
   *                             job classes.
   */
  public JobClass[] getJobClasses()
         throws DatabaseException
  {
    boolean useCustomClassLoader =
         getConfigParameter(Constants.PARAM_USE_CUSTOM_CLASS_LOADER, false);
    JobClassLoader classLoader = null;
    if (useCustomClassLoader)
    {
      classLoader = new JobClassLoader(getClass().getClassLoader(),
                                       slamdServer.getClassPath());
    }

    String classListStr = getConfigParameter(Constants.PARAM_JOB_CLASSES, "");
    StringTokenizer tokenizer = new StringTokenizer(classListStr, "\n");
    ArrayList<JobClass> jobClassList = new ArrayList<JobClass>();
    while (tokenizer.hasMoreTokens())
    {
      String className = tokenizer.nextToken();

      JobClass jobClass;
      if (useCustomClassLoader)
      {
        try
        {
          jobClassList.add(classLoader.getJobClass(className));
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to load job class " + className +
                                 ":  " + e);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
        }
      }
      else
      {
        try
        {
          jobClass = (JobClass) Constants.classForName(className).newInstance();
          jobClassList.add(jobClass);
        }
        catch (Exception e)
        {
          slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG,
                                 "Unable to load job class " + className +
                                 ":  " + e);
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
        }
      }
    }

    JobClass[] jobClasses = new JobClass[jobClassList.size()];
    jobClassList.toArray(jobClasses);
    return jobClasses;
  }



  /**
   * Adds information about the specified job class to the configuration
   * database.
   *
   * @param  className  The name of the job class to add to the configuration
   *                    database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void addJobClass(String className)
         throws DatabaseException
  {
    String classListStr = getConfigParameter(Constants.PARAM_JOB_CLASSES, "");
    putConfigParameter(Constants.PARAM_JOB_CLASSES,
                       classListStr + '\n' + className, false);
  }



  /**
   * Removes information about the specified job class from the configuration
   * database.
   *
   * @param  className  The job class to remove from the configuration database.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             configuration database.
   */
  public void removeJobClass(String className)
         throws DatabaseException
  {
    String classListStr = getConfigParameter(Constants.PARAM_JOB_CLASSES, "");
    StringTokenizer tokenizer = new StringTokenizer(classListStr, "\n");
    StringBuilder classNameBuffer = new StringBuilder();
    while (tokenizer.hasMoreTokens())
    {
      String jobClassName = tokenizer.nextToken();
      if (! jobClassName.equals(className))
      {
        if (classNameBuffer.length() > 0)
        {
          classNameBuffer.append('\n');
        }

        classNameBuffer.append(className);
      }
    }

    putConfigParameter(Constants.PARAM_JOB_CLASSES, classNameBuffer.toString(),
                       false);
  }



  /**
   * Retrieves the set of config subscribers that have been registered with the
   * configuration database.
   *
   * @return  The set of config subscribers that have been registered with the
   *          configuration database.
   */
  public ConfigSubscriber[] getConfigSubscribers()
  {
    synchronized (configSubscribers)
    {
      ConfigSubscriber[] subscribers =
           new ConfigSubscriber[configSubscribers.size()];
      configSubscribers.toArray(subscribers);
      return subscribers;
    }
  }



  /**
   * Retrieves a "safe" name for the provided configuration subscriber, which is
   * suitable for submission in an HTML form.
   *
   * @param  subscriber  The configuration subscriber for which to retrieve the
   *                     safe name.
   *
   * @return  The safe name for the provided configuration subscriber.
   */
  public static String getSafeName(ConfigSubscriber subscriber)
  {
    return subscriber.getSubscriberName().toLowerCase().replace(' ', '_');
  }



  /**
   * Retrieves the configuration subscriber that corresponds to the provided
   * "safe" name.  The safe name will be generated by converting the subscriber
   * name to all lowercase characters and replacing any spaces with underscores.
   *
   * @param  name  The safe name for which to retrieve the corresponding config
   *               subscriber.
   *
   * @return  The config subscriber for the provided safe name, or
   *          <CODE>null</CODE> if there is no such subscriber.
   */
  public ConfigSubscriber subscriberForSafeName(String name)
  {
    synchronized (configSubscribers)
    {
      Iterator<ConfigSubscriber> iterator = configSubscribers.iterator();
      while (iterator.hasNext())
      {
        ConfigSubscriber subscriber = iterator.next();
        if (subscriber.getSubscriberName().toLowerCase().replace(' ', '_').
                 equals(name))
        {
          return subscriber;
        }
      }
    }

    return null;
  }



  /**
   * Registers the provided configuration subscriber to be notified of changes
   * to configuration parameters in the database.
   *
   * @param  subscriber  The configuration subscriber with which to register.
   */
  public void registerAsSubscriber(ConfigSubscriber subscriber)
  {
    synchronized (configSubscribers)
    {
      for (int i=0; i < configSubscribers.size(); i++)
      {
        ConfigSubscriber s = configSubscribers.get(i);
        if (s.getSubscriberName().equals(subscriber.getSubscriberName()))
        {
          return;
        }
      }

      configSubscribers.add(subscriber);
    }
  }



  /**
   * Creates a new transaction that may be used to protect operations impacting
   * multiple databases.
   *
   * @return  The transaction that has been created.
   *
   * @throws  DatabaseException  If a problem occurs while trying to create the
   *                             transaction.
   */
  private Transaction getTransaction()
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that the environment is open before trying to create the
      // transaction.
      if (! environmentOpen)
      {
        String message = "Cannot create a transaction when the database " +
                         "environment is not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // Specify the configuration to use for the transaction.
      TransactionConfig txnConfig = new TransactionConfig();
      txnConfig.setReadUncommitted(false);
      txnConfig.setNoWait(false);


      // Try to create the transaction.
      Transaction txn = dbEnv.beginTransaction(null, txnConfig);
      activeTransactions.add(txn);
      return txn;
    }
  }



  /**
   * Commits the provided transaction.
   *
   * @param  txn  The transaction to commit.
   *
   * @throws  DatabaseException  If a problem occurs while trying to commit the
   *                             transaction.
   */
  private void commitTransaction(Transaction txn)
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that the environment is open before trying to perform the
      // commit.
      if (! environmentOpen)
      {
        String message = "Cannot commit a transaction when the database " +
                         "environment is not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // Commit the transaction.
      txn.commit();


      // Remove the transaction from the list of active transactions.
      activeTransactions.remove(txn);
    }
  }



  /**
   * Aborts the provided transaction.
   *
   * @param  txn  The transaction to abort.
   *
   * @throws  DatabaseException  If a problem occurs while trying to abort the
   *                             transaction.
   */
  private void abortTransaction(Transaction txn)
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that the environment is open before trying to abort the
      // transaction.
      if (! environmentOpen)
      {
        String message = "Cannot abort a transaction when the database " +
                         "environment is not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // No matter what, we will remove the transaction from the list of active
      // transactions.
      activeTransactions.remove(txn);


      // Abort the transaction.
      txn.abort();
    }
  }



  /**
   * Retrieves a byte array containing the contents of the record with the
   * provided key from the specified database.
   *
   * @param  txn        The transaction to use to protect the read.  This may be
   *                    <CODE>null</CODE> if no transaction is needed.
   * @param  db         The database in which to perform the get.
   * @param  key        The key for the record to retrieve.
   * @param  writeLock  Indicates whether to acquire a write lock on the
   *                    specified key.
   *
   * @return  A byte array containing the contents of the record with the
   *          provided key from the specified database, or <CODE>null</CODE> if
   *          the specified key does not exist.
   *
   * @throws  DatabaseException  If a problem occurs while trying to perform
   *                             the get.
   */
  private byte[] get(Transaction txn, Database db, String key,
                     boolean writeLock)
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that the databases are open before trying to perform the get.
      if (! dbsOpen)
      {
        String message = "Cannot perform the get because the databases are " +
                         "not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // Perform the get and return the associated data.
      DatabaseEntry keyEntry  = new DatabaseEntry(ASN1Element.getBytes(key));
      DatabaseEntry dataEntry = new DatabaseEntry();
      LockMode      lockMode  = (writeLock ? LockMode.RMW : LockMode.DEFAULT);

      if (db.get(txn, keyEntry, dataEntry, lockMode) == OperationStatus.SUCCESS)
      {
        return dataEntry.getData();
      }
      else
      {
        return null;
      }
    }
  }



  /**
   * Updates the specified database with the provided information.  If a record
   * exists with the specified key, then it will be overwritten.  Otherwise, a
   * new record will be created.
   *
   * @param  txn   The transaction to use to protect the update.  This may be
   *               <CODE>null</CODE> if no transaction is needed.
   * @param  db    The database into which to perform the update.
   * @param  key   The key to use for the provided data.
   * @param  data  The byte array containing the data to use for the record.
   *
   * @return  An <CODE>OperationStatus</CODE> instance providing information
   *          about the status of the update.
   *
   * @throws  DatabaseException  If a problem occurs while trying to update the
   *                             database.
   */
  private OperationStatus put(Transaction txn, Database db, String key,
                              byte[] data)
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that the databases are open before trying to perform the put.
      if (! dbsOpen)
      {
        String message = "Cannot perform the put because the databases are " +
                         "not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // Perform the put and return the status.
      DatabaseEntry keyEntry  = new DatabaseEntry(ASN1Element.getBytes(key));
      DatabaseEntry dataEntry = new DatabaseEntry(data);
      return db.put(txn, keyEntry, dataEntry);
    }
  }



  /**
   * Updates the provided database with the given information.  If a record
   * exists with the specified key, then it will be overwritten.  Otherwise, a
   * new record will be created.  Note that this should only be used in a
   * single-threaded mode when this update will be the only interaction with
   * the database at that time.
   *
   * @param  db    The database into which to perform the update.
   * @param  key   The key to use for the provided data.
   * @param  data  The data to use for the record.
   *
   * @return  An <CODE>OperationStatus</CODE> instance providing information
   *          about the status of the update.
   *
   * @throws  DatabaseException  If a problem occurs while trying to update the
   *                             database.
   */
  private static OperationStatus put(Database db, String key, String data)
         throws DatabaseException
  {
    try
    {
      DatabaseEntry keyEntry  = new DatabaseEntry(ASN1Element.getBytes(key));
      DatabaseEntry dataEntry = new DatabaseEntry(ASN1Element.getBytes(data));
      return db.put(null, keyEntry, dataEntry);
    }
    catch (DatabaseException de)
    {
      throw de;
    }
    catch (Exception e)
    {
      throw new DatabaseException("Unable to update the database:  " + e, e);
    }
  }



  /**
   * Updates the provided database with the given information.  If a record
   * exists with the specified key, then it will be overwritten.  Otherwise, a
   * new record will be created.  Note that this should only be used in a
   * single-threaded mode when this update will be the only interaction with
   * the database at that time.
   *
   * @param  db    The database into which to perform the update.
   * @param  key   The key to use for the provided data.
   * @param  data  The data to use for the record.
   *
   * @return  An <CODE>OperationStatus</CODE> instance providing information
   *          about the status of the update.
   *
   * @throws  DatabaseException  If a problem occurs while trying to update the
   *                             database.
   */
  private static OperationStatus put(Database db, String key, byte[] data)
         throws DatabaseException
  {
    try
    {
      DatabaseEntry keyEntry  = new DatabaseEntry(ASN1Element.getBytes(key));
      DatabaseEntry dataEntry = new DatabaseEntry(data);
      return db.put(null, keyEntry, dataEntry);
    }
    catch (DatabaseException de)
    {
      throw de;
    }
    catch (Exception e)
    {
      throw new DatabaseException("Unable to update the database:  " + e, e);
    }
  }



  /**
   * Removes the specified key from the given database.
   *
   * @param  txn  The transaction to use to protect the delete.
   * @param  db   The database from which to remove the specified key.
   * @param  key  The key to remove from the database.
   *
   * @return  An <CODE>OperationStatus</CODE> instance providing information
   *          about the status of the delete.
   *
   * @throws  DatabaseException  If a problem occurs while interacting with the
   *                             database.
   */
  private OperationStatus delete(Transaction txn, Database db, String key)
         throws DatabaseException
  {
    synchronized (dbMutex)
    {
      // Make sure that the databases are open before trying to perform the
      // delete.
      if (! dbsOpen)
      {
        String message = "Cannot perform the delete because the databases " +
                         "are not open.";
        slamdServer.logMessage(Constants.LOG_LEVEL_CONFIG, message);
        throw new DatabaseException(message);
      }


      // Perform the put and return the status.
      DatabaseEntry keyEntry  = new DatabaseEntry(ASN1Element.getBytes(key));
      return db.delete(txn, keyEntry);
    }
  }
}

