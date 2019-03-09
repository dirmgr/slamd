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



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import com.sleepycat.je.DatabaseException;

import com.slamd.admin.AdminServlet;
import com.slamd.db.SLAMDDB;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizationAlgorithm;
import com.slamd.job.SingleStatisticOptimizationAlgorithm;
import com.slamd.job.UnknownJobClass;
import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;
import com.slamd.common.JobClassLoader;
import com.slamd.common.RefCountMutex;
import com.slamd.common.SLAMDException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.MultiValuedParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.stat.StatTracker;



/**
 * This class provides the entry point to the SLAMD server.  It performs all the
 * necessary bootstrapping, starts the scheduler, starts the client listener,
 * and handles all necessary coordination between all other components of SLAMD.
 *
 *
 * @author   Neil A. Wilson
 */
public class SLAMDServer
       implements ConfigSubscriber
{
  /**
   * The version of the SLAMD server software being used.
   */
  public static final String SLAMD_SERVER_VERSION =
                                  DynamicConstants.SLAMD_VERSION;



  /**
   * The name used to register the logger as a subscriber to the configuration
   * handler.
   */
  public static final String CONFIG_SUBSCRIBER_NAME = "SLAMD Server";



  // The administrative servlet used to control the SLAMD server.
  private AdminServlet adminServlet;

  // Indicates whether the server should operate in read-only mode.
  private boolean readOnlyMode;


  // Variables that hold information about the configuration database.
  private SLAMDDB configDB;
  private String  configDBDirectory;


  // Variables used for SSL communication.
  private String sslKeyStore;
  private String sslKeyPassword;
  private String sslTrustStore;
  private String sslTrustPassword;


  // The various client listeners.
  private ClientListener                clientListener;
  private ClientManagerListener         clientManagerListener;
  private ResourceMonitorClientListener monitorClientListener;
  private StatListener                  statListener;


  // The SLAMD scheduler.
  private Scheduler scheduler;


  // The SLAMD mailer.
  private SMTPMailer mailer;


  // The SLAMD logger.
  private SimpleDateFormat dateFormatter;
  private boolean          loggerInitialized;
  private boolean          overrideConfigLogLevel;
  private int              logLevel;
  private Logger           logger;


  // The real-time stat handler.
  protected RealTimeStatHandler statHandler;


  // The job classes that have been defined for use in the configuration
  // directory.
  private RefCountMutex jobClassesMutex;
  private JobClass[]    jobClasses;


  // The optimization algorithms that have been defined for use in the
  // configuration directory.
  private OptimizationAlgorithm[] optimizationAlgorithms;


  // A timestamp representing the SLAMD server startup time.
  private Date startupTime;


  // A flag that indicates whether we will use the standard or the custom class
  // loader.
  private boolean useCustomClassLoader;



  /**
   * Creates a new instance of the SLAMD server.  All of the configuration will
   * be retrieved from an LDAP directory server using the information provided
   * as parameters to this function.
   *
   * @param  adminServlet       The admin servlet used to control the SLAMD
   *                            server.
   * @param  readOnlyMode       Indicates whether the server should operate in
   *                            read-only mode.  If so, then it should not
   *                            create any listeners.
   * @param  configDBDirectory  The path to the configuration database.
   * @param  sslKeyStore        The JSSE key store file to use for SSL
   *                            connections (typically "~/.keystore").
   * @param  sslKeyPassword     The password that should be used to access the
   *                            contents of the JSSE key store.
   * @param  sslTrustStore      The JSSE trust store file to use for SSL
   *                            connections (typically
   *                            "JAVA_HOME/jre/lib/security/cacerts")
   * @param  sslTrustPassword   The password that should be used to access the
   *                            contents of the JSSE trust store.
   *
   * @throws  SLAMDServerException  If an unrecoverable error occurs that
   *                                prevents the server from operating.
   */
  public SLAMDServer(AdminServlet adminServlet, boolean readOnlyMode,
                     String configDBDirectory, String sslKeyStore,
                     String sslKeyPassword, String sslTrustStore,
                     String sslTrustPassword)
         throws SLAMDServerException
  {
    this(adminServlet, readOnlyMode, configDBDirectory, sslKeyStore,
         sslKeyPassword, sslTrustStore, sslTrustPassword,
         Constants.LOG_LEVEL_USECONFIG);
  }



  /**
   * Creates a new instance of the SLAMD server.  All of the configuration will
   * be retrieved from an LDAP directory server using the information provided
   * as parameters to this function.
   *
   * @param  adminServlet       The admin servlet used to control the SLAMD
   *                            server.
   * @param  readOnlyMode       Indicates whether the server should operate in
   *                            read-only mode.  If so, then it should not
   *                            create any listeners.
   * @param  configDBDirectory  The path to the configuration database.
   * @param  sslKeyStore        The JSSE key store file to use for SSL
   *                            connections (typically "~/.keystore").
   * @param  sslKeyPassword     The password that should be used to access the
   *                            contents of the JSSE key store.
   * @param  sslTrustStore      The JSSE trust store file to use for SSL
   *                            connections (typically
   *                            "JAVA_HOME/jre/lib/security/cacerts")
   * @param  sslTrustPassword   The password that should be used to access the
   *                            contents of the JSSE trust store.
   * @param  logLevel           The log level that should be used by the server.
   *                            If it is a valid log level, then it will
   *                            override whatever log level may be specified in
   *                            the configuration.
   *
   * @throws  SLAMDServerException  If an unrecoverable error occurs that
   *                                prevents the server from operating.
   */
  public SLAMDServer(AdminServlet adminServlet, boolean readOnlyMode,
                     String configDBDirectory, String sslKeyStore,
                     String sslKeyPassword, String sslTrustStore,
                     String sslTrustPassword, int logLevel)
         throws SLAMDServerException
  {
    // Set the admin servlet used for the SLAMD server.
    this.adminServlet = adminServlet;


    // Determine whether the server should operate in read-only mode.
    this.readOnlyMode = readOnlyMode;


    // Initialize variables related to accessing the configuration database.
    this.configDBDirectory = configDBDirectory;


    // Initialize variables related to SSL processing.
    this.sslKeyStore         = sslKeyStore;
    this.sslKeyPassword      = sslKeyPassword;
    this.sslTrustStore       = sslTrustStore;
    this.sslTrustPassword    = sslTrustPassword;


    // Initialize variables related to logging
    loggerInitialized = false;
    dateFormatter = new SimpleDateFormat("[MM/dd/yyyy:HH:mm:ss]");
    if (logLevel == Constants.LOG_LEVEL_USECONFIG)
    {
      overrideConfigLogLevel = false;
      this.logLevel          = Constants.LOG_LEVEL_DEFAULT;
    }
    else
    {
      overrideConfigLogLevel = true;
      this.logLevel          = logLevel;
      logMessage(Constants.LOG_LEVEL_TRACE,
                 "In SLAMDServer constructor");
      logMessage(Constants.LOG_LEVEL_CONFIG,
                 "Log level configured to be " + this.logLevel);
    }


    // Indicate that the server is starting up
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "SLAMD Starting up....");


    // Create the configuration handler and load the SLAMD configuration from
    // the configuration directory.  Also, register as a subscriber of the
    // configuration handler.
    try
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Opening the configuration database...");
      configDB = new SLAMDDB(this, configDBDirectory, readOnlyMode, false);
      configDB.registerAsSubscriber(this);
      refreshSubscriberConfiguration();
    }
    catch (DatabaseException de)
    {
      String message = "Unrecoverable error -- can't open the SLAMD database " +
                       "-- " + de;
      logMessage(Constants.LOG_LEVEL_ANY, message);
      logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(de));
      throw new SLAMDServerException(message);
    }


    // Create the mailer for the SLAMD server.
    mailer = new SMTPMailer(this);


    // Retrieve a list of the job classes that have been defined in the
    // configuration directory
    try
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Obtaining the set of job classes....");
      jobClassesMutex = new RefCountMutex();
      jobClasses = configDB.getJobClasses();
      sortJobClasses();
    }
    catch (DatabaseException de)
    {
      String message = "Unrecoverable error -- can't get the job class " +
                       "definitions:  " + de;
      logMessage(Constants.LOG_LEVEL_ANY, message);
      logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(de));
      throw new SLAMDServerException(message, de);
    }


    // Retrieve the list of optimization algorithms that have been defined in
    // the configuration directory.
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "Obtaining the set of optimization algorithms....");
    String algorithmStr =
         configDB.getConfigParameter(Constants.PARAM_OPTIMIZATION_ALGORITHMS);
    if ((algorithmStr == null) || (algorithmStr.length() == 0))
    {
      optimizationAlgorithms = new OptimizationAlgorithm[0];
    }
    else
    {
      ArrayList<OptimizationAlgorithm> algorithmList =
           new ArrayList<OptimizationAlgorithm>();
      StringTokenizer tokenizer = new StringTokenizer(algorithmStr,
                                                      " \t\r\n");
      while (tokenizer.hasMoreTokens())
      {
        String algorithmClassName = tokenizer.nextToken();

        try
        {
          Class<?> algorithmClass = Constants.classForName(algorithmClassName);
          OptimizationAlgorithm algorithm =
               (OptimizationAlgorithm) algorithmClass.newInstance();
          algorithmList.add(algorithm);
        }
        catch (Exception e)
        {
          logMessage(Constants.LOG_LEVEL_ANY,
                     "Unable to load optimization algorithm class \"" +
                     algorithmClassName + "\" -- " + e);
          logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                     JobClass.stackTraceToString(e));
        }
      }

      optimizationAlgorithms =
           new OptimizationAlgorithm[algorithmList.size()];
      algorithmList.toArray(optimizationAlgorithms);
    }


    // Initialize the logging subsystem
    if (! readOnlyMode)
    {
      try
      {
        logMessage(Constants.LOG_LEVEL_SERVER_DEBUG, "Starting the logger....");
        logger = new Logger(this);
        logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                   "SLAMD logger successfully configured");
      }
      catch (SLAMDServerException sse)
      {
        logMessage(Constants.LOG_LEVEL_ANY,
                   "Can't initialize the logger - " + sse.getMessage() +
                   " -- file-based logging will be disabled");
        logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                   JobClass.stackTraceToString(sse));
      }
    }


    // Create the client manager listener to start listening for new connections
    if (! readOnlyMode)
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Starting the client manager listener....");
      clientManagerListener = new ClientManagerListener(this);
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Client manager listener started");
    }


    // Create the client listener to start listening for new connections
    if (! readOnlyMode)
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Starting the client listener....");
      clientListener = new ClientListener(this);
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG, "Client listener started");
    }


    // Create the resource monitor client listener to start listening for new
    // connections
    if (! readOnlyMode)
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Starting the resource monitor client listener....");
      monitorClientListener = new ResourceMonitorClientListener(this);
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Monitor client listener started");
    }


    // Create the stat client listener to start listening for new connections
    if (! readOnlyMode)
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Starting the stat client listener....");
      statHandler = new RealTimeStatHandler(this);
      statListener = new StatListener(this);
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Stat listener started");
    }


    // Create the scheduler to handle scheduling of new jobs.  Load a job list
    // from the configuration directory to see if there are any jobs that should
    // be scheduled for execution.
    if (! readOnlyMode)
    {
      try
      {
        logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                   "Starting the scheduler....");
        scheduler = new Scheduler(this);
        logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                   "Scheduler initialized");
      }
      catch (SLAMDServerException sse)
      {
        logMessage(Constants.LOG_LEVEL_ANY,
                   "Unrecoverable error -- can't initialize the scheduler - " +
                   sse.getMessage());
        logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                   JobClass.stackTraceToString(sse));
        logger.closeLogger();

        try
        {
          configDB.closeDatabases(true);
        } catch (Exception e) {}

        try
        {
          configDB.closeEnvironment();
        } catch (Exception e) {}

        throw sse;
      }
    }


    // Start all the threads associated with SLAMD server components.
    if (clientManagerListener != null)
    {
      clientManagerListener.startListening();
    }

    if (clientListener != null)
    {
      clientListener.startListening();
    }

    if (monitorClientListener != null)
    {
      monitorClientListener.startListening();
    }

    if (statListener != null)
    {
      statListener.startListening();
    }

    if (scheduler != null)
    {
      scheduler.startScheduler();
    }


    // Set this as the current startup time
    startupTime = new Date();
    logMessage(Constants.LOG_LEVEL_ANY, "SLAMD Server Started");
    logMessage(Constants.LOG_LEVEL_TRACE, "Leaving SLAMDServer constructor");
  }



  /**
   * Stops the SLAMD server and all related components.  This method will not
   * return until the shutdown has completed.
   */
  public void stopSLAMD()
  {
    logMessage(Constants.LOG_LEVEL_ANY, "SLAMD server is shutting down....");

    // Stop the client manager listener.
    if ((! readOnlyMode) && (clientManagerListener != null))
    {
      clientManagerListener.stopListening();
    }

    // Stop the client listener to give it a chance to disconnect all
    // the clients and stop handling new connections.
    if ((! readOnlyMode) && (clientListener != null))
    {
      clientListener.stopListening();
    }

    // Stop the monitor client listener to give it a chance to disconnect.
    if ((! readOnlyMode) && (monitorClientListener != null))
    {
      monitorClientListener.stopListening();
    }

    // Stop the stat client listener to give it a chance to disconnect.
    if ((! readOnlyMode) && (statListener != null))
    {
      statListener.stopListening();
    }

    // Next, stop the scheduler
    if ((! readOnlyMode) && (scheduler != null))
    {
      scheduler.stopScheduler();
    }

    // Next, the configuration handler
    try
    {
      configDB.closeDatabases(true);
    } catch (Exception e) {}

    try
    {
      configDB.closeEnvironment();
    } catch (Exception e) {}

    // Give the monitor listener a chance to complete its shutdown.
    if ((! readOnlyMode) && (monitorClientListener != null))
    {
      monitorClientListener.waitForStop();
    }

    // Give the listener a chance to complete its shutdown
    if ((! readOnlyMode) && (clientListener != null))
    {
      clientListener.waitForStop();
    }

    // Give the stat listener a chance to complete its shutdown
    if ((! readOnlyMode) && (statListener != null))
    {
      statListener.waitForStop();
    }

    // Give the scheduler a chance to complete its shutdown
    if ((! readOnlyMode) && (scheduler != null))
    {
      scheduler.waitForStop();
    }

    // OK, so it's not really stopped, but by the time the user sees this
    // message, it will be.
    logMessage(Constants.LOG_LEVEL_ANY, "SLAMD server stopped.");

    // Finally, the logger.  This won't return until it has stopped.
    if ((! readOnlyMode) && (logger != null))
    {
      logger.closeLogger();
    }
  }



  /**
   * Retrieves the admin servlet used to manage the SLAMD server.
   *
   * @return  The admin servlet used to manage the SLAMD server.
   */
  public AdminServlet getAdminServlet()
  {
    return adminServlet;
  }



  /**
   * Retrieves the location of the job class files on the local filesystem.
   *
   * @return  The path to the job class files on the local filesystem.
   */
  public String getClassPath()
  {
    return AdminServlet.getClassPath();
  }



  /**
   * Retrieves the set of job classes defined for use in the configuration
   * directory.
   *
   * @return  The set of job classes defined for use in the configuration
   *          directory.
   */
  public JobClass[] getJobClasses()
  {
    logMessage(Constants.LOG_LEVEL_TRACE, "In SLAMDServer.getJobClasses()");
    return jobClasses;
  }



  /**
   * Retrieves the set of job classes defined for use in the configuration
   * directory that are in the specified category name.
   *
   * @param  categoryName  The name of the category for which to retrieve the
   *                       job classes.
   *
   * @return  The job classes in the specified category.
   */
  public JobClass[] getJobClasses(String categoryName)
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.getJobClasses(" + categoryName + ')');

    jobClassesMutex.getReadLock();
    ArrayList<JobClass> jobClassList = new ArrayList<JobClass>();
    for (int i=0; i < jobClasses.length; i++)
    {
      if (jobClasses[i].getJobCategoryName().equalsIgnoreCase(categoryName))
      {
        jobClassList.add(jobClasses[i]);
      }
    }
    jobClassesMutex.releaseReadLock();

    JobClass[] classes = new JobClass[jobClassList.size()];
    jobClassList.toArray(classes);
    return classes;
  }



  /**
   * Retrieves the names of the job class categories defined to the SLAMD
   * server.
   *
   * @return  The names of the job class categories defined to the SLAMD server.
   */
  public String[] getJobClassCategories()
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.getJobClassCategories()");

    jobClassesMutex.getReadLock();

    ArrayList<String> categoryList = new ArrayList<String>();

    for (int i=0; i < jobClasses.length; i++)
    {
      String categoryName = jobClasses[i].getJobCategoryName();

      boolean added = false;
      for (int j=0; j < categoryList.size(); j++)
      {
        String category = categoryList.get(j);
        if (category.equalsIgnoreCase(categoryName))
        {
          added = true;
          break;
        }
      }

      if (! added)
      {
        categoryList.add(categoryName);
      }
    }

    jobClassesMutex.releaseReadLock();

    String[] categoryNames = new String[categoryList.size()];
    categoryList.toArray(categoryNames);
    return categoryNames;
  }



  /**
   * Retrieves the set of job classes defined for use in the configuration
   * directory, separated into the individual job class categories.
   *
   * @return  The set of job classes defined for use in the configuration
   *          directory, separated into the individual job class categories.
   */
  public JobClass[][] getCategorizedJobClasses()
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.getCategorizedJobClasses()");

    jobClassesMutex.getReadLock();

    ArrayList<ArrayList<JobClass>> categoryList =
         new ArrayList<ArrayList<JobClass>>();

    for (int i=0; i < jobClasses.length; i++)
    {
      String categoryName = jobClasses[i].getJobCategoryName();

      boolean added = false;
      for (int j=0; j < categoryList.size(); j++)
      {
        ArrayList<JobClass> sublist = categoryList.get(j);
        JobClass jobClass = sublist.get(0);
        if (jobClass.getJobCategoryName() == null)
        {
          if (categoryName == null)
          {
            sublist.add(jobClasses[i]);
            added = true;
            break;
          }
        }
        else if (jobClass.getJobCategoryName().equalsIgnoreCase(categoryName))
        {
          sublist.add(jobClasses[i]);
          added = true;
          break;
        }
      }

      if (! added)
      {
        ArrayList<JobClass> sublist = new ArrayList<JobClass>();
        sublist.add(jobClasses[i]);
        categoryList.add(sublist);
      }
    }

    jobClassesMutex.releaseReadLock();

    JobClass[][] categorizedClasses = new JobClass[categoryList.size()][];
    for (int i=0; i < categorizedClasses.length; i++)
    {
      ArrayList<JobClass> sublist = categoryList.get(i);
      categorizedClasses[i] = new JobClass[sublist.size()];
      sublist.toArray(categorizedClasses[i]);
    }

    // Sort the category names alphabetically.
    for (int i=0; i < categorizedClasses.length; i++)
    {
      String categoryName = categorizedClasses[i][0].getJobCategoryName();
      if (categoryName == null)
      {
        // There is no category name.  These go at the very end.
        int lastPos = categorizedClasses.length - 1;
        if (i < lastPos)
        {
          JobClass[] tempClasses      = categorizedClasses[i];
          categorizedClasses[i]       = categorizedClasses[lastPos];
          categorizedClasses[lastPos] = tempClasses;
          i--;
        }
      }
      else
      {
        String lowerName =
             categorizedClasses[i][0].getJobCategoryName().toLowerCase();
        for (int j=i+1; j < categorizedClasses.length; j++)
        {
          categoryName = categorizedClasses[j][0].getJobCategoryName();
          if (categoryName != null)
          {
            String lowerName2 =
                 categorizedClasses[j][0].getJobCategoryName().toLowerCase();
            if (lowerName2.compareTo(lowerName) < 0)
            {
              JobClass[] tempClasses = categorizedClasses[i];
              categorizedClasses[i]  = categorizedClasses[j];
              categorizedClasses[j]  = tempClasses;
              lowerName              = lowerName2;
            }
          }
        }
      }
    }

    return categorizedClasses;
  }



  /**
   * Retrieves the SLAMD job class with the specified name.
   *
   * @param  className  The Java class name for the class to retrieve.
   *
   * @return  The SLAMD job class with the specified name, or <CODE>null</CODE>
   *          if the specified job class is not known to the SLAMD server.
   */
  public JobClass getJobClass(String className)
  {
    logMessage(Constants.LOG_LEVEL_TRACE, "In SLAMDServer.getJobClass()");
    JobClass jobClass = null;

    jobClassesMutex.getReadLock();
    for (int i=0; i < jobClasses.length; i++)
    {
      if (jobClasses[i].getClass().getName().equals(className))
      {
        jobClass = jobClasses[i];
        break;
      }
    }

    jobClassesMutex.releaseReadLock();
    return jobClass;
  }



  /**
   * Indicates whether a custom class loader should be used for loading job
   * classes.
   *
   * @return  <CODE>true</CODE> if a custom class loader should be used for
   *          loading job classes, or <CODE>false</CODE> if not.
   */
  public boolean useCustomClassLoader()
  {
    return useCustomClassLoader;
  }



  /**
   * Loads the job class with the specified name and returns it to the caller.
   *
   * @param  className  The name of the job class to be loaded.
   *
   * @return  The requested job class.
   *
   * @throws  SLAMDException  If a problem occurs while trying to load the
   *                          requested class.
   */
  public JobClass loadJobClass(String className)
         throws SLAMDException
  {
    try
    {
      if (useCustomClassLoader)
      {
        JobClassLoader classLoader =
             new JobClassLoader(this.getClass().getClassLoader(),
                                adminServlet.getClassPath());
        return classLoader.getJobClass(className);
      }
      else
      {
        try
        {
          Class<?> jobClass = Constants.classForName(className);
          return (JobClass) jobClass.newInstance();
        }
        catch (Exception e)
        {
          logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                     JobClass.stackTraceToString(e));
          throw new SLAMDException("Unable to load job class " + className +
                                   ":  " + e, e);
        }
      }
    }
    catch (Exception e)
    {
      logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(e));
      return new UnknownJobClass(className);
    }
  }



  /**
   * Retrieves a cached copy of this job class if possible.  Otherwise, attempts
   * to load the job class using the appropriate class loader.
   *
   * @param  className  The name of the Java class to be returned.
   *
   * @return  The requested job class.
   *
   * @throws  SLAMDException  If it is necessary to load the job class and a
   *                          problem occurs while doing so.
   */
  public JobClass getOrLoadJobClass(String className)
         throws SLAMDException
  {
    JobClass jobClass = getJobClass(className);
    if (jobClass == null)
    {
      jobClass = loadJobClass(className);
    }

    return jobClass;
  }



  /**
   * Refreshes the list of available job classes from the definitions stored in
   * the configuration directory.
   *
   * @throws  SLAMDServerException  If a problem is encountered while reading
   *                                from the configuration directory.
   */
  public void reloadJobClasses()
         throws SLAMDServerException
  {
    logMessage(Constants.LOG_LEVEL_TRACE, "In SLAMDServer.reloadJobClasses()");
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "In SLAMDServer.reloadJobClasses()");
    jobClassesMutex.getWriteLock();

    try
    {
      jobClasses = configDB.getJobClasses();
    }
    catch (DatabaseException de)
    {
      throw new SLAMDServerException("Unable to read job class definitions " +
                                     "from the configuration database:  " + de,
                                     de);
    }

    sortJobClasses();
    jobClassesMutex.releaseWriteLock();
  }



  /**
   * Adds the specified class as a job thread that can be used to run jobs in
   * the SLAMD server.
   *
   * @param  jobClass  The job thread class that will be used to perform the
   *                   work for this job.
   *
   * @throws  SLAMDServerException  If a problem is encountered while adding the
   *                                job class.
   */
  public void addJobClass(JobClass jobClass)
         throws SLAMDServerException
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.addJobClass(" + jobClass.getClass().getName() +
               ')');
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "In SLAMDServer.addJobClass(" + jobClass.getClass().getName() +
               ')');

    // First, make sure it's not already defined
    String className = jobClass.getClass().getName();

    jobClassesMutex.getWriteLock();
    for (int i=0; i < jobClasses.length; i++)
    {
      if (jobClasses[i].getClass().getName().equals(className))
      {
        jobClassesMutex.releaseWriteLock();
        logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                   "The job class " + className + " is already defined");
        throw new SLAMDServerException("The job class " + className +
                                       " is already defined in the " +
                                       "configuration");
      }
    }


    // Add it to the configuration directory
    try
    {
      configDB.addJobClass(className);
    }
    catch (DatabaseException de)
    {
      jobClassesMutex.releaseWriteLock();
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Unable to add job class " + className + " -- " + de);
      logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                 JobClass.stackTraceToString(de));
      throw new SLAMDServerException("Unable to add job class " + className +
                                     " -- " + de, de);
    }


    // If we got here, then it was added to the config directory successfully,
    // so update the job class array
    JobClass[] tmp = new JobClass[jobClasses.length + 1];
    System.arraycopy(jobClasses, 0, tmp, 0, jobClasses.length);
    tmp[jobClasses.length] = jobClass;
    jobClasses = tmp;
    sortJobClasses();
    jobClassesMutex.releaseWriteLock();

    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "Successfully added job class " + className);
  }



  /**
   * Removes the specified class from the list of job classes defined in the
   * SLAMD server.
   *
   * @param  jobClass  The class to remove from the set of defined job classes.
   *
   * @throws  SLAMDServerException  If a problem is encountered while removing
   *                                the job class.
   */
  public void removeJobClass(JobClass jobClass)
         throws SLAMDServerException
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.removeJobClass(" +
               jobClass.getClass().getName() + ')');
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "In SLAMDServer.removeJobClass(" +
               jobClass.getClass().getName() + ')');

    String className = jobClass.getClass().getName();
    int removePos = -1;

    jobClassesMutex.getWriteLock();
    for (int i=0; i < jobClasses.length; i++)
    {
      if (jobClasses[i].getClass().getName().equals(className))
      {
        removePos = i;
        break;
      }
    }

    if (removePos >= 0)
    {
      try
      {
        configDB.removeJobClass(className);
      }
      catch (DatabaseException de)
      {
        throw new SLAMDServerException("Unable to remove the job class " +
                                       "definition -- " + de, de);
      }

      JobClass[] tmp = new JobClass[jobClasses.length - 1];
      System.arraycopy(jobClasses, 0, tmp, 0, removePos);
      System.arraycopy(jobClasses, (removePos+1), tmp, removePos,
                       (tmp.length - removePos));
      jobClasses = tmp;
      sortJobClasses();

      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "Successfully removed job class " + className);
    }
    else
    {
      logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                 "The job class " + className + " is not defined");
    }

    jobClassesMutex.releaseWriteLock();
  }



  /**
   * Sorts the list of job classes in alphabetical order on the basis of the
   * job name.  The caller must hold the job class write lock.
   */
  private void sortJobClasses()
  {
    for (int i=0; i < jobClasses.length; i++)
    {
      String name = jobClasses[i].getJobName().toLowerCase();

      for (int j=i+1; j < jobClasses.length; j++)
      {
        String name2 = jobClasses[j].getJobName().toLowerCase();
        if (name2.compareTo(name) < 0)
        {
          JobClass tmpClass = jobClasses[i];
          jobClasses[i]     = jobClasses[j];
          jobClasses[j]     = tmpClass;
          name              = name2;
        }
      }
    }
  }



  /**
   * Retrieves the set of optimization algorithms defined for use in the SLAMD
   * server.
   *
   * @return  The set of optimization algorithms defined for use in the SLAMD
   *          server.
   */
  public OptimizationAlgorithm[] getOptimizationAlgorithms()
  {
    return optimizationAlgorithms;
  }



  /**
   * Retrieves the optimization algorithm with the specified class name.
   *
   * @param  algorithmClass  The fully-qualified name of the Java class that
   *                         provides the optimization algorithm.
   *
   * @return  The optimization algorithm with the specified class name, or
   *          <CODE>null</CODE> if no such algorithm is defined.
   */
  public OptimizationAlgorithm getOptimizationAlgorithm(String algorithmClass)
  {
    for (int i=0; i < optimizationAlgorithms.length; i++)
    {
      if (optimizationAlgorithms[i].getClass().getName().equals(algorithmClass))
      {
        return optimizationAlgorithms[i].newInstance();
      }
    }

    return null;
  }



  /**
   * Retrieves the time that the SLAMD server was started.
   *
   * @return  The time that the SLAMD server was started.
   */
  public Date getStartupTime()
  {
    return startupTime;
  }



  /**
   * Writes the specified message to the SLAMD log if appropriate (based on the
   * log level).  The message will be formatted so that it contains a timestamp
   * and a log level indicator.  If the logging system has not been initialized,
   * then the message will be logged to standard error.
   *
   * @param  messageLogLevel  The log level to use when determining whether to
   *                          actually write the message to the SLAMD log.
   * @param  logMessage       The message to be logged.
   */
  public void logMessage(int messageLogLevel, String logMessage)
  {
    // First, see if the message is applicable to our configured log level.  If
    // not, then just return immediately
    if ((logLevel & messageLogLevel) != messageLogLevel)
    {
      return;
    }


    // Prepend a timestamp and the log level onto this message
    String message;
    synchronized (this)
    {
      message = dateFormatter.format(new Date()) + " - " +
                       Constants.logLevelToString(messageLogLevel) + " - " +
                       logMessage;
    }


    // If the logger is initialized, then send the message to it.
    if (loggerInitialized)
    {
      logger.logMessage(message);
    }


    // If the logger is not yet initialized, or if it is a fatal error message,
    // then write it to standard error.
    if ((! loggerInitialized) ||
        (messageLogLevel == Constants.LOG_LEVEL_ANY))
    {
      if (! readOnlyMode)
      {
        System.err.println(message);
      }
    }
  }



  /**
   * Writes the specified message to the SLAMD log if appropriate (based on the
   * log level).  The message will be logged as-is with no formatting applied,
   * so the timestamp and log level should be already included in the message.
   * If the logging system has not been initialized, then the message will be
   * logged to standard error.
   *
   * @param  messageLogLevel  The log level to use when determining whether to
   *                          actually write the message to the SLAMD log.
   * @param  message          The message to be logged.
   */
  public void logWithoutFormatting(int messageLogLevel, String message)
  {
    // First, see if the message is applicable to our configured log level.  If
    // not, then just return immediately
    if ((logLevel & messageLogLevel) != messageLogLevel)
    {
      return;
    }


    // If the logger is initialized, then send the message to it.
    if (loggerInitialized)
    {
      logger.logMessage(message);
    }


    // If the logger is not yet initialized, or if it is a fatal error message,
    // then write it to standard error.
    if ((! loggerInitialized) ||
        (messageLogLevel == Constants.LOG_LEVEL_ANY))
    {
      if (! readOnlyMode)
      {
        System.err.println(message);
      }
    }
  }



  /**
   * Retrieves a formatted timestamp containing the current time.
   *
   * @return  A formatted timestamp containing the current time.
   */
  public synchronized String getTimestamp()
  {
    return dateFormatter.format(new Date());
  }



  /**
   * Retrieves the configuration handler associated with this SLAMD server.
   *
   * @return  The configuration handler associated with this SLAMD server.
   */
  public SLAMDDB getConfigDB()
  {
    return configDB;
  }



  /**
   * Retrieves the client listener associated with this SLAMD server.
   *
   * @return  The client listener associated with this SLAMD server.
   */
  public ClientListener getClientListener()
  {
    return clientListener;
  }



  /**
   * Retrieves the client manager listener associated with this SLAMD server.
   *
   * @return  The client manager listener associated with this SLAMD server.
   */
  public ClientManagerListener getClientManagerListener()
  {
    return clientManagerListener;
  }



  /**
   * Retrieves the resource monitor client listener associated with this SLAMD
   * server.
   *
   * @return  The resource monitor client listener associated with this SLAMD
   *          server.
   */
  public ResourceMonitorClientListener getMonitorClientListener()
  {
    return monitorClientListener;
  }



  /**
   * Retrieves the stat listener associated with this SLAMD server.
   *
   * @return  The stat listener associated with this SLAMD server.
   */
  public StatListener getStatListener()
  {
    return statListener;
  }



  /**
   * Retrieves the real-time stat handler associated with this SLAMD server.
   *
   * @return  The real-time stat handler associated with this SLAMD server.
   */
  public RealTimeStatHandler getStatHandler()
  {
    return statHandler;
  }



  /**
   * Retrieves the mailer associated with this SLAMD server.
   *
   * @return  The mailer associated with this SLAMD server.
   */
  public SMTPMailer getMailer()
  {
    return mailer;
  }



  /**
   * Checks to see if an e-mail message should be sent to one or more users to
   * tell them that the specified job is complete.
   *
   * @param  job  The job for which to provide notification.
   */
  public void sendCompletedJobNotification(Job job)
  {
    String[] notifyAddresses = job.getNotifyAddresses();
    if ((notifyAddresses == null) || (notifyAddresses.length == 0) ||
        (! mailer.isEnabled()))
    {
      return;
    }

    String EOL = Constants.SMTP_EOL;
    String subject = "SLAMD Job " + job.getJobID() + " completed";

    StringBuilder message = new StringBuilder();

    message.append("SLAMD Job " + job.getJobID() + " has completed." + EOL);

    String baseURI = mailer.getServletBaseURI();
    if ((baseURI != null) && (baseURI.length() > 0))
    {
      message.append("For full job details, go to: " + EOL);
      message.append(baseURI + '?' + Constants.SERVLET_PARAM_SECTION + '=' +
                     Constants.SERVLET_SECTION_JOB + '&' +
                     Constants.SERVLET_PARAM_SUBSECTION + '=' +
                     Constants.SERVLET_SECTION_JOB_VIEW_GENERIC + '&' +
                     Constants.SERVLET_PARAM_JOB_ID + '=' + job.getJobID() +
                     EOL);
      message.append(EOL);
    }

    message.append("Job ID:  " + job.getJobID() + EOL +
                   "Job Description:  " + job.getJobDescription() + EOL +
                   "Job Type:  " + job.getJobName() + EOL +
                   "Job Class:  " + job.getJobClass().getClass().getName() +
                        EOL +
                   "Job State:  " +
                        Constants.jobStateToString(job.getJobState()) + EOL);

    if (job.doneRunning())
    {
      message.append(EOL);

      SimpleDateFormat displayFormat =
           new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);

      Date actualStartTime = job.getActualStartTime();
      if (actualStartTime != null)
      {
        message.append("Actual Start Time:  " +
                       displayFormat.format(actualStartTime) + EOL);
      }

      Date actualStopTime = job.getActualStopTime();
      if (actualStopTime != null)
      {
        message.append("Actual Stop Time:  " +
                       displayFormat.format(actualStopTime) + EOL);
      }

      int actualDuration = job.getActualDuration();
      if (actualDuration >= 0)
      {
        message.append("Actual Duration:  " + actualDuration + EOL);
      }

      if (job.hasStats())
      {
        message.append(EOL);

        String[] trackerNames = job.getStatTrackerNames();
        for (int i=0; i< trackerNames.length; i++)
        {
          StatTracker[] trackers = job.getStatTrackers(trackerNames[i]);
          if ((trackers != null) && (trackers.length > 0))
          {
            StatTracker tracker = trackers[0].newInstance();
            tracker.aggregate(trackers);
            message.append(tracker.getSummaryString() + EOL);
          }
        }
      }
    }

    mailer.sendMessage(notifyAddresses, subject, message.toString());
  }



  /**
   * Indicates whether the logging subsystem has been initialized.
   *
   * @return  <CODE>true</CODE> if the logging system has been initialized, or
   *          <CODE>false</CODE> if not.
   */
  public boolean loggerInitialized()
  {
    return loggerInitialized;
  }



  /**
   * Specifies whether the logger is initialized or not.
   *
   * @param  loggerInitialized  Used to indicate whether the logger has been
   *                            initialized or not.
   */
  public void setLoggerInitialized(boolean loggerInitialized)
  {
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "Setting loggerInitialized to " + loggerInitialized);
    this.loggerInitialized = loggerInitialized;
  }



  /**
   * Retrieves the scheduler associated with this SLAMD server.
   *
   * @return  The scheduler associated with this SLAMD server.
   */
  public Scheduler getScheduler()
  {
    return scheduler;
  }



  /**
   * Retrieves the location of the SSL key store.
   *
   * @return  The location of the SSL key store.
   */
  public String getSSLKeyStore()
  {
    return sslKeyStore;
  }



  /**
   * Retrieves the password required to access the SSL key store.
   *
   * @return  The password required to access the SSL key store.
   */
  public String getSSLKeyStorePassword()
  {
    return sslKeyPassword;
  }



  /**
   * Retrieves the location of the SSL trust store.
   *
   * @return  The location of the SSL trust store.
   */
  public String getSSLTrustStore()
  {
    return sslTrustStore;
  }



  /**
   * Retrieves the password required to access the SSL trust store.
   *
   * @return  The password required to access the SSL trust store.
   */
  public String getSSLTrustStorePassword()
  {
    return sslTrustPassword;
  }



  /**
   * Retrieves the name that the server uses to subscribe to the configuration
   * handler in order to be notified of configuration changes.
   *
   * @return  The name that the server uses to subscribe to the configuration
   *          handler in order to be notified of configuration changes.
   */
  public String getSubscriberName()
  {
    return CONFIG_SUBSCRIBER_NAME;
  }



  /**
   * Retrieves the set of configuration parameters associated with this
   * configuration subscriber.
   *
   * @return  The set of configuration parameters associated with this
   *          configuration subscriber.
   */
  public ParameterList getSubscriberParameters()
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.getParameters())");

    String[] algorithmClasses = new String[optimizationAlgorithms.length];
    for (int i=0; i < algorithmClasses.length; i++)
    {
      algorithmClasses[i] = optimizationAlgorithms[i].getClass().getName();
    }

    BooleanParameter customClassLoaderParameter =
         new BooleanParameter(Constants.PARAM_USE_CUSTOM_CLASS_LOADER,
                              "Use Custom Class Loader",
                              "Indicates whether to use the custom class " +
                              "loader.  If this class loader is used, it "+
                              "will be possible to dynamically reload the " +
                              "job class definitions so that a job class " +
                              "can be replaced without restarting the SLAMD " +
                              "server.  However, some users have reported " +
                              "problems with this feature.  If attempts to " +
                              "interact with job classes fail, particularly " +
                              "with illegal access exceptions, try disabling " +
                              "the custom class loader.", useCustomClassLoader);

    MultiLineTextParameter optimizationAlgorithmsParameter =
         new MultiLineTextParameter(Constants.PARAM_OPTIMIZATION_ALGORITHMS,
                                    "Optimization Algorithms",
                                    "The fully-qualified names of the Java " +
                                    "classes that should serve as the " +
                                    "optimization algorithms available for " +
                                    "use in the SLAMD server.",
                                    algorithmClasses, false);
    optimizationAlgorithmsParameter.setVisibleColumns(80);

    MultiValuedParameter logLevelParameter =
         new MultiValuedParameter(Constants.PARAM_LOG_LEVEL,
                                  "Log Level",
                                  "The categories of information to include " +
                                  "in the SLAMD log.",
                                  Constants.getLogLevelStrings(), logLevel,
                                  false);



    Parameter[] params = new Parameter[]
    {
      logLevelParameter,
      customClassLoaderParameter,
      optimizationAlgorithmsParameter
    };
    return new ParameterList(params);
  }



  /**
   * Re-reads all configuration information used by the core SLAMD server.  In
   * this case, the only option is the log level to determine which messages are
   * written to the log file.  If a specific log level was specified in a
   * constructor, then that will always be used, regardless of the log level
   * specified in the configuration.
   */
  public void refreshSubscriberConfiguration()
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.refreshConfiguration()");
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "In SLAMDServer.refreshConfiguration()");

    // Read the log level from the configuration
    if (! overrideConfigLogLevel)
    {
      String logLevelStr =
          configDB.getConfigParameter(Constants.PARAM_LOG_LEVEL);
      if ((logLevelStr != null) && (logLevelStr.length() > 0))
      {
        try
        {
          logLevel = Integer.parseInt(logLevelStr);
          logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                     "Setting logLevel to " + logLevel);
        }
        catch (NumberFormatException nfe)
        {
          logMessage(Constants.LOG_LEVEL_CONFIG,
                     "Config parameter " + Constants.PARAM_LOG_LEVEL +
                     " requires a numeric value");
          logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                     JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        logLevel = Constants.LOG_LEVEL_DEFAULT;
      }
    }


    // Determine whether to use the custom class loader.
    String propertyStr =
         System.getProperty(Constants.PROPERTY_DISABLE_CUSTOM_CLASS_LOADER);
    if ((propertyStr != null) &&
        (propertyStr.equalsIgnoreCase("true") ||
         propertyStr.equalsIgnoreCase("yes") ||
         propertyStr.equalsIgnoreCase("on") ||
         propertyStr.equalsIgnoreCase("1")))
    {
      useCustomClassLoader = false;
    }
    else
    {
      useCustomClassLoader = false;
      String classLoaderStr =
           configDB.getConfigParameter(Constants.PARAM_USE_CUSTOM_CLASS_LOADER);
      if (classLoaderStr != null)
      {
        useCustomClassLoader = (classLoaderStr.equalsIgnoreCase("true") ||
                                classLoaderStr.equalsIgnoreCase("yes") ||
                                classLoaderStr.equalsIgnoreCase("on") ||
                                classLoaderStr.equalsIgnoreCase("1"));
      }
    }


    // Retrieve the set of optimization algorithms to use.
    String algorithmStr =
         configDB.getConfigParameter(Constants.PARAM_OPTIMIZATION_ALGORITHMS);
    if ((algorithmStr == null) || (algorithmStr.length() == 0))
    {
      optimizationAlgorithms = new OptimizationAlgorithm[]
      {
        new SingleStatisticOptimizationAlgorithm()
      };
    }
    else
    {
      ArrayList<OptimizationAlgorithm> algorithmList =
           new ArrayList<OptimizationAlgorithm>();

      StringTokenizer tokenizer = new StringTokenizer(algorithmStr, " \t\r\n");
      while (tokenizer.hasMoreTokens())
      {
        String className = tokenizer.nextToken();

        try
        {
          Class<?> algorithmClass = Constants.classForName(className);
          OptimizationAlgorithm algorithm =
               (OptimizationAlgorithm) algorithmClass.newInstance();
          algorithmList.add(algorithm);
        }
        catch (Exception e)
        {
          logMessage(Constants.LOG_LEVEL_CONFIG,
                     "Unable to load optimization algorithm \"" + className +
                     '"' + e);
          logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                     JobClass.stackTraceToString(e));
        }
      }

      optimizationAlgorithms = new OptimizationAlgorithm[algorithmList.size()];
      algorithmList.toArray(optimizationAlgorithms);
    }
  }



  /**
   * Re-reads the configuration information for the specified parameter.  In
   * this case, the only option is the log level to determine which messages are
   * written to the log file.  If a specific log level was specified in a
   * constructor, then that will always be used, regardless of the log level
   * specified in the configuration.
   *
   * @param  parameterName  The name of the parameter to be re-read from the
   *                        configuration.
   */
  public void refreshSubscriberConfiguration(String parameterName)
  {
    logMessage(Constants.LOG_LEVEL_TRACE,
               "In SLAMDServer.refreshConfiguration(" + parameterName + ')');
    logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
               "In SLAMDServer.refreshConfiguration(" + parameterName + ')');

    if (parameterName.equalsIgnoreCase(Constants.PARAM_LOG_LEVEL) &&
        (! overrideConfigLogLevel))
    {
      String logLevelStr =
          configDB.getConfigParameter(Constants.PARAM_LOG_LEVEL);
      if ((logLevelStr != null) && (logLevelStr.length() > 0))
      {
        try
        {
          logLevel = Integer.parseInt(logLevelStr);
          logMessage(Constants.LOG_LEVEL_SERVER_DEBUG,
                     "Setting logLevel to " + logLevel);
        }
        catch (NumberFormatException nfe)
        {
          logMessage(Constants.LOG_LEVEL_CONFIG,
                     "Config parameter " + Constants.PARAM_LOG_LEVEL +
                     " requires a numeric value");
          logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                     JobClass.stackTraceToString(nfe));
        }
      }
      else
      {
        logLevel = Constants.LOG_LEVEL_ANY;
      }
    }
    else if (parameterName.equalsIgnoreCase(
                                Constants.PARAM_USE_CUSTOM_CLASS_LOADER))
    {
      // Determine whether to use the custom class loader.
      String propertyStr =
           System.getProperty(Constants.PROPERTY_DISABLE_CUSTOM_CLASS_LOADER);
      if ((propertyStr != null) &&
          (propertyStr.equalsIgnoreCase("true") ||
           propertyStr.equalsIgnoreCase("yes") ||
           propertyStr.equalsIgnoreCase("on") ||
           propertyStr.equalsIgnoreCase("1")))
      {
        useCustomClassLoader = false;
      }
      else
      {
        useCustomClassLoader = false;
        String classLoaderStr = configDB.getConfigParameter(
                                     Constants.PARAM_USE_CUSTOM_CLASS_LOADER);
        if (classLoaderStr != null)
        {
          useCustomClassLoader = (classLoaderStr.equalsIgnoreCase("true") ||
                                  classLoaderStr.equalsIgnoreCase("yes") ||
                                  classLoaderStr.equalsIgnoreCase("on") ||
                                  classLoaderStr.equalsIgnoreCase("1"));
        }
      }
    }
    else if (parameterName.equalsIgnoreCase(
                                Constants.PARAM_OPTIMIZATION_ALGORITHMS))
    {
      // Retrieve the set of optimization algorithms to use.
      String algorithmStr = configDB.getConfigParameter(
                                 Constants.PARAM_OPTIMIZATION_ALGORITHMS);
      if ((algorithmStr == null) || (algorithmStr.length() == 0))
      {
        optimizationAlgorithms = new OptimizationAlgorithm[]
        {
          new SingleStatisticOptimizationAlgorithm()
        };
      }
      else
      {
        ArrayList<OptimizationAlgorithm> algorithmList =
             new ArrayList<OptimizationAlgorithm>();

        StringTokenizer tokenizer = new StringTokenizer(algorithmStr,
                                                        " \t\r\n");
        while (tokenizer.hasMoreTokens())
        {
          String className = tokenizer.nextToken();

          try
          {
            Class<?> algorithmClass = Constants.classForName(className);
            OptimizationAlgorithm algorithm =
                 (OptimizationAlgorithm) algorithmClass.newInstance();
            algorithmList.add(algorithm);
          }
          catch (Exception e)
          {
            logMessage(Constants.LOG_LEVEL_CONFIG,
                       "Unable to load optimization algorithm \"" + className +
                       '"' + e);
            logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                       JobClass.stackTraceToString(e));
          }
        }

        optimizationAlgorithms =
             new OptimizationAlgorithm[algorithmList.size()];
        algorithmList.toArray(optimizationAlgorithms);
      }
    }
  }
}

