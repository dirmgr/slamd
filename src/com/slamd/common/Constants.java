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
package com.slamd.common;



/**
 * This class holds the values of constants used in various areas of the SLAMD
 * server.  It also contains a few methods for interacting with those constants.
 *
 *
 * @author  Neil A. Wilson
 */
public final class Constants
{
  /**
   * The URL that may be used to access the SLAMD home page.
   */
  public static final String SLAMD_HOME_URL = "http://www.SLAMD.com/";



  /**
   * The URL that may be used to access the SLAMD online documentation.
   */
  public static final String SLAMD_DOC_URL =
       "http://www.SLAMD.com/documentation.shtml";



  /**
   * The URI that may be used to access the SLAMD Quick Start Guide in
   * OpenDocument format.
   */
  public static final String DOC_URI_QUICK_START_ODT =
       "/documentation/slamd_quick_start_guide.odt";



  /**
   * The URI that may be used to access the SLAMD Quick Start Guide in PDF
   * format.
   */
  public static final String DOC_URI_QUICK_START_PDF =
       "/documentation/slamd_quick_start_guide.pdf";



  /**
   * The URI that may be used to access the SLAMD Tools Guide in OpenDocument
   * format.
   */
  public static final String DOC_URI_TOOLS_ODT =
       "/documentation/tools_guide.odt";



  /**
   * The URI that may be used to access the SLAMD Tools Guide in PDF format.
   */
  public static final String DOC_URI_TOOLS_PDF =
       "/documentation/tools_guide.pdf";



  /**
   * The name of the LDAP objectclass that will be used in entries that hold
   * information about SLAMD configuration parameters.
   */
  public static final String CONFIG_PARAMETER_OC = "slamdConfigParameter";



  /**
   * The name of the LDAP objectclass that will be used in entries that hold
   * information about the Java class files that can be used as SLAMD jobs.
   */
  public static final String JOB_CLASS_OC = "slamdJobClass";



  /**
   * The name of the LDAP objectclass that will be used in entries that hold
   * information about SLAMD jobs that have been scheduled for execution.
   */
  public static final String SCHEDULED_JOB_OC = "slamdScheduledJob";



  /**
   * The name of the LDAP objectclass that will be used in entries that hold
   * information about optimizing jobs.
   */
  public static final String OPTIMIZING_JOB_OC = "slamdOptimizingJob";



  /**
   * The name of the LDAP objectclass that will be used in entries that are the
   * parent entries for job folders.
   */
  public static final String JOB_FOLDER_OC = "slamdJobFolder";



  /**
   * The name of the LDAP objectclass that will be used in entries that are used
   * to hold information about uploaded files.
   */
  public static final String UPLOADED_FILE_OC = "slamdUploadedFile";



  /**
   * The name of the LDAP objectclass that will be used in entries that define
   * virtual job folders.
   */
  public static final String VIRTUAL_JOB_FOLDER_OC = "slamdVirtualJobFolder";



  /**
   * The name of the LDAP attribute that specifies the name of the job folder.
   */
  public static final String JOB_FOLDER_NAME_AT = "cn";



  /**
   * The name of the LDAP attribute that specifies the description for a job
   * folder.
   */
  public static final String JOB_FOLDER_DESCRIPTION_AT = "description";



  /**
   * The name of the LDAP attribute that specifies a filter that can be used to
   * automatically include jobs in a virtual job folder.
   */
  public static final String VIRTUAL_FOLDER_FILTER_AT = "slamdJobSearchFilter";



  /**
   * The name of the LDAP attribute that will be used to hold the unique ID
   * assigned to a scheduled job.
   */
  public static final String JOB_ID_AT = "slamdJobID";



  /**
   * The name of the LDAP attribute that will be used to hold the unique ID
   * assigned to an optimizing job.
   */
  public static final String OPTIMIZING_JOB_ID_AT = "slamdOptimizingJobID";



  /**
   * The name of the LDAP attribute that will be used to hold the name of the
   * optimization algorithm to use for a given optimizing job.
   */
  public static final String OPTIMIZATION_ALGORITHM_AT =
       "slamdOptimizationAlgorithm";



  /**
   * The name of the LDAP attribute that will be used to hold the values of
   * configuration parameters used for the optimization algorithm.
   */
  public static final String OPTIMIZATION_ALGORITHM_PARAMETER_AT =
       "slamdOptimizationAlgorithmParameter";



  /**
   * The name of the LDAP attribute that will be used to determine whether to
   * re-run the best iteration of an optimizing job.
   */
  public static final String OPTIMIZING_JOB_RERUN_BEST_ITERATION_AT =
       "slamdOptimizingJobReRunBestIteration";



  /**
   * The name of the LDAP attribute that will be used to determine the duration
   * to use when re-running the best iteration of an optimizing job.
   */
  public static final String OPTIMIZING_JOB_RERUN_DURATION_AT =
       "slamdOptimizingJobReRunDuration";



  /**
   * The name of the LDAP attribute that will be used to specify the job ID of
   * the job that was a re-run of the best iteration of the optimizing job.
   */
  public static final String OPTIMIZING_JOB_RERUN_ITERATION_AT =
       "slamdOptimizingJobReRunIteration";



  /**
   * The name of the LDAP attribute that will be used to hold the job ID of the
   * base job associated with an optimizing job.
   */
  public static final String BASE_JOB_ID_AT = "slamdBaseJobID";



  /**
   * The name of the LDAP attribute that will be used to hold the job ID(s) of
   * jobs that must be completed before the current job may be started.
   */
  public static final String JOB_DEPENDENCY_AT = "slamdJobDependency";



  /**
   * The name of the LDAP attribute that will be used to specify the number of
   * clients that should be used to process a scheduled job.
   */
  public static final String JOB_NUM_CLIENTS_AT = "slamdJobNumClients";



  /**
   * The name of the LDAP attribute that will be used to store job-specific
   * parameters.
   */
  public static final String JOB_PARAM_AT = "slamdJobParameter";



  /**
   * The name of the LDAP attribute that indicates the current state of a job.
   */
  public static final String JOB_STATE_AT = "slamdJobState";



  /**
   * The name of the LDAP attribute that will be used to specify the number of
   * threads that each client should use to process a scheduled job.
   */
  public static final String JOB_THREADS_PER_CLIENT_AT =
       "slamdJobThreadsPerClient";



  /**
   * The name of the LDAP attribute that will be used to specify the minimum
   * number of threads that should be used in an optimizing job.
   */
  public static final String JOB_MIN_THREADS_AT = "slamdJobMinThreads";



  /**
   * The name of the LDAP attribute that will be used to specify the maximum
   * number of threads that should be used in an optimizing job.
   */
  public static final String JOB_MAX_THREADS_AT = "slamdJobMaxThreads";



  /**
   * The name of the LDAP attribute that will be used to specify the increment
   * in number of threads that should be used between iterations of an
   * optimizing job.
   */
  public static final String JOB_THREAD_INCREMENT_AT =
       "slamdJobThreadIncrement";



  /**
   * The name of the LDAP attribute that will be used to specify the maximum
   * number of non-improving iterations that will be allowed in an optimizing
   * job.
   */
  public static final String JOB_MAX_NON_IMPROVING_ITERATIONS_AT =
       "slamdJobMaxNonImprovingIterations";



  /**
   * The name of the LDAP attribute that will be used to specify whether to
   * include the number of threads in the description for jobs scheduled by an
   * optimizing job.
   */
  public static final String JOB_INCLUDE_THREAD_IN_DESCRIPTION_AT =
       "slamdJobIncludeThreadCountInDescription";



  /**
   * The name of the LDAP attribute that will be used to specify the delay in
   * milliseconds between starting each thread on a client system.
   */
  public static final String JOB_THREAD_STARTUP_DELAY_AT =
       "slamdJobThreadStartupDelay";



  /**
   * The name of the LDAP attribute that will be used to specify the delay in
   * seconds between iterations of an optimizing job.
   */
  public static final String DELAY_BETWEEN_ITERATIONS_AT =
       "slamdDelayBetweenIterations";



  /**
   * The name of the LDAP attribute that will be used to specify whehter a job
   * or folder should be visible if the server is operating in restricted
   * read-only mode.
   */
  public static final String DISPLAY_IN_READ_ONLY_AT = "slamdDisplayInReadOnly";



  /**
   * The name of the LDAP attribute that will be used to specify the time at
   * which a job should start running.
   */
  public static final String JOB_START_TIME_AT = "slamdJobStartTime";



  /**
   * The name of the LDAP attribute that will be used to specify the time at
   * which a job should stop running.
   */
  public static final String JOB_STOP_TIME_AT = "slamdJobStopTime";



  /**
   * The name of the LDAP attribute that will specify the maximum length of time
   * in seconds that a job should be allowed to run.
   */
  public static final String JOB_DURATION_AT = "slamdJobDuration";



  /**
   * The name of the LDAP attribute that will be used to hold the name of a
   * SLAMD configuration parameter.
   */
  public static final String PARAMETER_NAME_AT = "slamdParameterName";



  /**
   * The name of the LDAP attribute that will be used to hold the value of a
   * SLAMD configuration parameter.
   */
  public static final String PARAMETER_VALUE_AT = "slamdParameterValue";



  /**
   * The name of the LDAP attribute that will store the name of the Java class
   * that is a job thread.
   */
  public static final String JOB_CLASS_NAME_AT = "slamdJobClassName";



  /**
   * The name of the LDAP attribute that will store the human-readable name of
   * the job thread.
   */
  public static final String JOB_NAME_AT = "cn";



  /**
   * The name of the LDAP attribute that will store comments about a job.
   */
  public static final String JOB_COMMENTS_AT = "slamdJobComments";



  /**
   * The name of the LDAP attribute that will store the description of the job
   * thread.
   */
  public static final String JOB_DESCRIPTION_AT = "description";



  /**
   * The name of the LDAP attribute that will store statistical information
   * about a job.
   */
  public static final String JOB_STAT_TRACKER_AT = "slamdJobStatistics";



  /**
   * The name of the LDAP attribute that will store statistical information
   * collected by resource monitors.
   */
  public static final String JOB_MONITOR_STAT_AT = "slamdJobMonitorStatistics";



  /**
   * The name of the LDAP attribute that will store the name of the statistic
   * targeted by an optimizing job.
   */
  public static final String JOB_OPTIMIZATION_STAT_AT =
       "slamdJobOptimizationStatistic";



  /**
   * The name of the LDAP attribute that will store the type of optimization
   * to be performed by an optimizing job.
   */
  public static final String JOB_OPTIMIZATION_TYPE_AT =
        "slamdJobOptimizationType";



  /**
   * The name of the LDAP attribute that will store the statistics collection
   * interval for a job.
   */
  public static final String JOB_COLLECTION_INTERVAL_AT =
       "slamdJobCollectionInterval";



  /**
   * The name of the LDAP attribute that will store the indication of whether
   * the job should wait for the set of clients.
   */
  public static final String JOB_WAIT_FOR_CLIENTS_AT = "slamdJobWaitForClients";



  /**
   * The name of the LDAP attribute that will store the set of clients that have
   * been requested to execute this job.
   */
  public static final String JOB_CLIENTS_AT = "slamdJobClients";



  /**
   * The name of the LDAP attribute that will store the set of clients that have
   * been requested for resource monitoring for a given job.
   */
  public static final String JOB_MONITOR_CLIENTS_AT = "slamdJobMonitorClients";



  /**
   * The name of the LDAP attribute that will be used to indicate whether
   * resource monitor clients running on the same system(s) as the job clients
   * should be automatically used.
   */
  public static final String JOB_MONITOR_CLIENTS_IF_AVAILABLE_AT =
       "slamdJobMonitorClientsIfAvailable";



  /**
   * The name of the LDAP attribute that will store the set of messages logged
   * during job processing.
   */
  public static final String JOB_LOG_MESSAGES_AT = "slamdJobLogMessages";



  /**
   * The delimiter that will be used to separate the individual log messages in
   * the single log messages attribute value.  It is not possible to make the
   * log messages attribute multivalued with a separate value per log message
   * because it is possible that some of those log messages will be duplicates.
   */
  public static final String JOB_LOG_MESSAGES_DELIMITER = "\n";



  /**
   * The name of the LDAP attribute that will store the time that the job
   * actually started running.
   */
  public static final String JOB_ACTUAL_START_TIME_AT =
       "slamdJobActualStartTime";



  /**
   * The name of the LDAP attribute that will store the time that the job
   * actually stopped running.
   */
  public static final String JOB_ACTUAL_STOP_TIME_AT = "slamdJobActualStopTime";



  /**
   * The name of the LDAP attribute that will store the length of time in
   * seconds that the job was running.
   */
  public static final String JOB_ACTUAL_DURATION_AT = "slamdJobActualDuration";



  /**
   * The name of the LDAP attribute that will store the reason that a job
   * stopped running.
   */
  public static final String JOB_STOP_REASON_AT = "slamdJobStopReason";



  /**
   * The name of the LDAP attribute that will store an address to which an
   * e-mail message should be sent whenever a job is complete.
   */
  public static final String JOB_NOTIFY_ADDRESS_AT =  "slamdJobNotifyAddress";



  /**
   * The name of the LDAP attribute that stores the LDAP URL that specifies
   * the criteria for membership in a dynamic group.
   */
  public static final String MEMBER_URL_AT = "memberURL";



  /**
   * The name of the LDAP attribute that stores the list of roles with which a
   * user entry is associated.
   */
  public static final String ROLE_DN_AT = "nsRole";



  /**
   * The name of the LDAP attribute that stores the raw data for an uploaded
   * file.
   */
  public static final String UPLOADED_FILE_DATA_AT = "slamdFileData";



  /**
   * The name of the LDAP attribute that stores the description of an uploaded
   * file.
   */
  public static final String UPLOADED_FILE_DESCRIPTION_AT =
       "description";



  /**
   * The name of the LDAP attribute that stores the name of an uploaded file.
   */
  public static final String UPLOADED_FILE_NAME_AT = "slamdFileName";



  /**
   * The name of the LDAP attribute that stores the size in bytes for an
   * uploaded file.
   */
  public static final String UPLOADED_FILE_SIZE_AT = "slamdFileSize";



  /**
   * The name of the LDAP attribute that stores the MIME type for an uploaded
   * file.
   */
  public static final String UPLOADED_FILE_TYPE_AT = "slamdFileType";



  /**
   * The set of attributes that should be requested when retrieving only summary
   * information for jobs.
   */
  public static final String[] JOB_SUMMARY_ATTRS = new String[]
  {
    JOB_ID_AT,
    JOB_CLASS_NAME_AT,
    JOB_DESCRIPTION_AT,
    JOB_NUM_CLIENTS_AT,
    JOB_THREADS_PER_CLIENT_AT,
    JOB_START_TIME_AT,
    JOB_ACTUAL_START_TIME_AT,
    DISPLAY_IN_READ_ONLY_AT,
    JOB_STATE_AT
  };



  /**
   * The set of attributes that should be requested when retrieving only summary
   * information for optimizing jobs.
   */
  public static final String[] OPTIMIZING_JOB_SUMMARY_ATTRS = new String[]
  {
    OPTIMIZING_JOB_ID_AT,
    JOB_DESCRIPTION_AT,
    BASE_JOB_ID_AT,
    JOB_CLASS_NAME_AT,
    JOB_START_TIME_AT,
    JOB_ACTUAL_START_TIME_AT,
    DISPLAY_IN_READ_ONLY_AT,
    JOB_STATE_AT
  };



  /**
   * The set of attributes that should be requested when retrieving only summary
   * information for uploaded files.
   */
  public static final String[] UPLOADED_FILE_SUMMARY_ATTRS = new String[]
  {
    UPLOADED_FILE_NAME_AT,
    UPLOADED_FILE_TYPE_AT,
    UPLOADED_FILE_SIZE_AT,
    UPLOADED_FILE_DESCRIPTION_AT
  };



  /**
   * The name of the database that is used to hold configuration information.
   */
  public static final String DB_NAME_CONFIG = "config";



  /**
   * The name of the database that is used to hold uploaded file data.
   */
  public static final String DB_NAME_FILE = "file";



  /**
   * The name of the database that is used to hold job folder information.
   */
  public static final String DB_NAME_FOLDER = "folder";



  /**
   * The name of the database that is used to hold group information.
   */
  public static final String DB_NAME_GROUP = "group";



  /**
   * The name of the database that is used to hold job information.
   */
  public static final String DB_NAME_JOB = "job";



  /**
   * The name of the database that is used to hold job group information.
   */
  public static final String DB_NAME_JOB_GROUP = "job_group";



  /**
   * The name of the database that is used to hold optimizing job information.
   */
  public static final String DB_NAME_OPTIMIZING_JOB = "optimizing_job";



  /**
   * The name of the database that is used to hold user information.
   */
  public static final String DB_NAME_USER = "user";



  /**
   * The name of the database that is used to hold virtual folder information.
   */
  public static final String DB_NAME_VIRTUAL_FOLDER = "virtual_folder";



  /**
   * The end-of-line character that is appropriate for this platform.
   */
  public static final String EOL = System.getProperty("line.separator");



  /**
   * A character array containing all the alphabetic characters.
   */
  public static final char[] ALPHABET_CHARS =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  /**
   * A character array containing all the alphabetic and numeric characters.
   */
  public static final char[] ALPHANUMERIC_CHARS =
       "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();



  /**
   * A character array containing the valid hexadecimal digits.
   */
  public static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();



  /**
   * A character array containing all the base ten digits.
   */
  public static final char[] NUMERIC_CHARS = "01234567890".toCharArray();



  /**
   * The character set to use for the random portion of the unique ID.
   */
  public static final char[] UNIQUE_ID_RANDOM_CHAR_SET = NUMERIC_CHARS;



  /**
   * The number of random characters to include in the unique ID.
   */
  public static final int UNIQUE_ID_RANDOM_CHAR_COUNT = 6;



  /**
   * The format string that will be used when writing date information into an
   * LDAP attribute.
   */
  public static final String ATTRIBUTE_DATE_FORMAT = "yyyyMMddHHmmss";



  /**
   * The format string that will be used when displaying date information to the
   * end user.
   */
  public static final String DISPLAY_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";



  /**
   * The format string that will be used when including the date in an e-mail
   * message.
   */
  public static final String MAIL_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";



  /**
   * The maximum length of time in seconds that an attempt will be made to
   * connect to the configuration directory server before initiating an attempt
   * to connect to a failover server.  If no failover server has been specified,
   * then this will do nothing.
   */
  public static final int CONNECTION_FAILOVER_DELAY = 5;



  /**
   * The string that will be used as the delimiter between the name and the
   * value for job-specific parameters stored in the configuration directory.
   * Only the first instance of this string will be seen as the delimiter --
   * subsequent occurrences will be assumed part of the value.
   */
  public static final String JOB_PARAM_DELIMITER_STRING = "=";



  /**
   * The maximum length of time in milliseconds that an attempt to read a
   * message from the client or server should wait before giving up.  This
   * should only be used for cases in which waiting on a response blocks the
   * client or server from doing other things, so blocking reads in a completely
   * separate thread should be OK.
   */
  public static final int MAX_BLOCKING_READ_TIME = 30000;



  /**
   * The maximum length of time in milliseconds that the getConfigDirConnection
   * method will wait before assuming that the previous holder forgot to release
   * the connection and will allow access to it again.  This will be used to
   * protect against deadlocks but should be sufficiently high that it doesn't
   * get triggered inadvertently (e.g., if a method using the connection is
   * spending a lot of time waiting for a response from the server).
   */
  public static final int MAX_CONNECTION_WAIT_TIME = 300000;



  /**
   * A loglevel that may be specified that indicates that whatever loglevel is
   * used in the configuration is the one that should be used.  This should only
   * be used when the SLAMD server is being initialized.
   */
  public static final int LOG_LEVEL_USECONFIG = -1;



  /**
   * A loglevel that is used to indicate that the associated message is of the
   * highest criticality.  These messages will always be logged regardless of
   * the loglevel set in the configuration.  If the logging system is
   * initialized, then they will be logged to both standard error and to the
   * logging system.
   */
  public static final int LOG_LEVEL_ANY = 0;



  /**
   * A loglevel that is used for tracing through code execution.  This should
   * only be used for debugging purposes.
   */
  public static final int LOG_LEVEL_TRACE = 1;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_TRACE</CODE> log level.
   */
  public static final String LOG_LEVEL_TRACE_STRING = "Trace Function Calls";



  /**
   * A loglevel that is used for messages regarding configuration file
   * processing.
   */
  public static final int LOG_LEVEL_CONFIG = 2;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_CONFIG</CODE> log level.
   */
  public static final String LOG_LEVEL_CONFIG_STRING = "Configuration Handling";



  /**
   * A loglevel that is used for messages about significant events that occur
   * while a job is being processed.
   */
  public static final int LOG_LEVEL_JOB_PROCESSING = 4;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_JOB_PROCESSING</CODE> log level.
   */
  public static final String LOG_LEVEL_JOB_PROCESSING_STRING =
       "Job Processing";



  /**
   * A loglevel that is used for messages about events that occur while the
   * server is interacting with a client.
   */
  public static final int LOG_LEVEL_CLIENT = 8;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_CLIENT</CODE> log level.
   */
  public static final String LOG_LEVEL_CLIENT_STRING =
       "Client/Server Interaction";



  /**
   * A loglevel that is used for events that occur as a result of someone using
   * the administrative interface.
   */
  public static final int LOG_LEVEL_ADMIN = 16;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_ADMIN</CODE> log level.
   */
  public static final String LOG_LEVEL_ADMIN_STRING = "Admin Interface Events";



  /**
   * A loglevel that is used for debugging operations performed in the SLAMD
   * server.
   */
  public static final int LOG_LEVEL_SERVER_DEBUG = 32;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_SERVER_DEBUG</CODE> log level.
   */
  public static final String LOG_LEVEL_SERVER_DEBUG_STRING =
       "SLAMD Server Operation Debugging";



  /**
   * A loglevel that is used for debugging job processing.
   */
  public static final int LOG_LEVEL_JOB_DEBUG = 64;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_JOB_DEBUG</CODE> log level.
   */
  public static final String LOG_LEVEL_JOB_DEBUG_STRING =
       "Job Processing Debugging";



  /**
   * A loglevel that is used for debugging client interaction.
   */
  public static final int LOG_LEVEL_CLIENT_DEBUG = 128;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_CLIENT_DEBUG</CODE> log level.
   */
  public static final String LOG_LEVEL_CLIENT_DEBUG_STRING =
       "Client Interaction Debugging";



  /**
   * A loglevel that is used for debugging scheduler operations.
   */
  public static final int LOG_LEVEL_SCHEDULER_DEBUG = 256;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_SCHEDULER_DEBUG</CODE> log level.
   */
  public static final String LOG_LEVEL_SCHEDULER_DEBUG_STRING =
       "Scheduler Debugging";



  /**
   * A loglevel that is used for debugging access manager operations.
   */
  public static final int LOG_LEVEL_ACCESS_MANAGER_DEBUG = 512;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_ACCESS_MANAGER_DEBUG</CODE> log level.
   */
  public static final String LOG_LEVEL_ACCESS_MANAGER_DEBUG_STRING =
       "Access Manager Debugging";



  /**
   * A loglevel that is used for logging exceptions that may be thrown.
   */
  public static final int LOG_LEVEL_EXCEPTION_DEBUG = 1024;



  /**
   * The user-friendly string value associated with the
   * <CODE>LOG_LEVEL_EXCEPTION_DEBUG</CODE> log level.
   */
  public static final String LOG_LEVEL_EXCEPTION_DEBUG_STRING =
       "Exception Debugging";



  /**
   * The default loglevel that will be used by the server unless overridden by
   * the configuration.
   */
  public static final int LOG_LEVEL_DEFAULT = LOG_LEVEL_ANY |
                                              LOG_LEVEL_JOB_PROCESSING |
                                              LOG_LEVEL_CLIENT |
                                              LOG_LEVEL_EXCEPTION_DEBUG;



  /**
   * Retrieves the text to include in the log file for the log level at which
   * the associated message is being logged.
   *
   * @param  logLevel  The log level to be converted to a string.
   *
   * @return  The string representation of the specified log level.
   */
  public static String logLevelToString(int logLevel)
  {
    // Always use 15 characters for these strings -- makes the logs easier
    // to read and parse.
    switch (logLevel)
    {
      case LOG_LEVEL_ANY:                   return "ANY            ";
      case LOG_LEVEL_TRACE:                 return "TRACE          ";
      case LOG_LEVEL_CONFIG:                return "CONFIG         ";
      case LOG_LEVEL_JOB_PROCESSING:        return "JOB            ";
      case LOG_LEVEL_CLIENT:                return "CLIENT         ";
      case LOG_LEVEL_ADMIN:                 return "ADMIN          ";
      case LOG_LEVEL_SERVER_DEBUG:          return "SERVER_DEBUG   ";
      case LOG_LEVEL_JOB_DEBUG:             return "JOB_DEBUG      ";
      case LOG_LEVEL_CLIENT_DEBUG:          return "CLIENT_DEBUG   ";
      case LOG_LEVEL_SCHEDULER_DEBUG:       return "SCHEDULER_DEBUG";
      case LOG_LEVEL_ACCESS_MANAGER_DEBUG:  return "ACCESS_DEBUG   ";
      case LOG_LEVEL_EXCEPTION_DEBUG:       return "EXCEPTION      ";
      default:                              return "UNKNOWN        ";
    }
  }



  /**
   * Retrieves the string values associated with the various log levels that
   * may be enabled for use in the SLAMD server.
   *
   * @return  The string values associated with the available log levels.
   */
  public static String[] getLogLevelStrings()
  {
    return new String[]
    {
      LOG_LEVEL_TRACE_STRING,
      LOG_LEVEL_CONFIG_STRING,
      LOG_LEVEL_JOB_PROCESSING_STRING,
      LOG_LEVEL_CLIENT_STRING,
      LOG_LEVEL_ADMIN_STRING,
      LOG_LEVEL_SERVER_DEBUG_STRING,
      LOG_LEVEL_JOB_DEBUG_STRING,
      LOG_LEVEL_CLIENT_DEBUG_STRING,
      LOG_LEVEL_SCHEDULER_DEBUG_STRING,
      LOG_LEVEL_ACCESS_MANAGER_DEBUG_STRING,
      LOG_LEVEL_EXCEPTION_DEBUG_STRING
    };
  }



  /**
   * The value that should be given to a Boolean configuration parameter to
   * indicate that it is "on" or "true".
   */
  public static final String CONFIG_VALUE_TRUE = "true";



  /**
   * The value that should be given to a Boolean configuration parameter to
   * indicate that it is "off" or "false".
   */
  public static final String CONFIG_VALUE_FALSE = "false";



  /**
   * The default length of time in seconds between iterations of the SLAMD
   * scheduler loop.
   */
  public static final int DEFAULT_SCHEDULER_DELAY = 5;



  /**
   * The default time in seconds before a job's start time that the job request
   * should be sent to clients.
   */
  public static final int DEFAULT_SCHEDULER_START_BUFFER = 30;



  /**
   * The default length of time in seconds to use as the statistics collection
   * interval.
   */
  public static final int DEFAULT_COLLECTION_INTERVAL = 60;



  /**
   * The default interval in seconds between keepalive messages.
   */
  public static final int DEFAULT_LISTENER_KEEPALIVE_INTERVAL = 0;



  /**
   * The default port on which the SLAMD server will listen for client
   * connections.
   */
  public static final int DEFAULT_LISTENER_PORT_NUMBER = 3000;



  /**
   * The default port on which the SLAMD server will listen for client manager
   * connections.
   */
  public static final int DEFAULT_CLIENT_MANAGER_LISTENER_PORT_NUMBER = 3001;



  /**
   * The default port on which the SLAMD server will listen for resource monitor
   * client connections.
   */
  public static final int DEFAULT_MONITOR_LISTENER_PORT_NUMBER = 3002;



  /**
   * The default port on which the SLAMD server will listen for real-time
   * statistics reporting.
   */
  public static final int DEFAULT_STAT_LISTENER_PORT_NUMBER = 3003;



  /**
   * The default length of time in seconds between periodic saves of statistical
   * data on the client.
   */
  public static final int DEFAULT_STAT_PERSISTENCE_INTERVAL = 300;



  /**
   * The default length of time in seconds to use when reporting statistics to
   * the SLAMD server.
   */
  public static final int DEFAULT_STAT_REPORT_INTERVAL = 5;



  /**
   * The default length of time in seconds between iterations of the logging
   * thread's poll loop.
   */
  public static final int DEFAULT_LOG_POLL_DELAY = 10;



  /**
   * The default name to use for the SLAMD log file.
   */
  public static final String DEFAULT_LOG_FILENAME = "slamd.log";



  /**
   * The default content type that will be used for returning non-HTML data.
   */
  public static final String DEFAULT_FILE_CONTENT_TYPE =
       "application/x-slamd-file";



  /**
   * The default path to use for the location of the Web application files.
   */
  public static final String DEFAULT_WEB_APP_PATH = "/WEB-INF";



  /**
   * The default maximum length of time in seconds that a client connection will
   * wait for a response to a solicited request before returning an error.
   */
  public static final int DEFAULT_MAX_RESPONSE_WAIT_TIME = 5;



  /**
   * The default maximum length of time in seconds that the SLAMD server will
   * wait for a response to a solicited request from a client manager before
   * returning an error.
   */
  public static final int DEFAULT_CLIENT_MANAGER_MAX_WAIT_TIME = 10;



  /**
   * The default value that indicates whether the client listener will require
   * clients to authenticate.
   */
  public static final boolean DEFAULT_REQUIRE_AUTHENTICATION = false;



  /**
   * The default value that indicates whether the client listener will use SSL.
   */
  public static final boolean DEFAULT_LISTENER_USE_SSL = false;



  /**
   * The full name of the Java class that is the superclass for all job thread
   * implementations.
   */
  public static final String JOB_THREAD_SUPERCLASS_NAME =
       "com.slamd.job.JobClass";



  /**
   * The authentication type that indicates no authentication will be performed.
   */
  public static final int AUTH_TYPE_NONE = 0;



  /**
   * The authentication type that indicates simple authentication will be
   * performed (i.e., password-based).
   */
  public static final int AUTH_TYPE_SIMPLE = 1;



  /**
   * The client state that will be used if the current state does not fit into
   * any of the other categories.
   */
  public static final int CLIENT_STATE_UNKNOWN = 0;



  /**
   * The client state that indicates the client is not connected to the SLAMD
   * server.  This state should never be included in a status response message
   * because status response messages are only sent to the server.
   */
  public static final int CLIENT_STATE_NOT_CONNECTED = 1;



  /**
   * The client state that indicates the client is idle and will accept new
   * work.
   */
  public static final int CLIENT_STATE_IDLE = 2;



  /**
   * The client state that indicates that a job has been defined but is not yet
   * started.  That is, the client is not actively processing, but will not
   * accept any new jobs.
   */
  public static final int CLIENT_STATE_JOB_NOT_YET_STARTED = 3;



  /**
   * The client state that indicates the client is currently running a job.
   */
  public static final int CLIENT_STATE_RUNNING_JOB = 4;



  /**
   * The client state that indicates the client is in the process of shutting
   * down.
   */
  public static final int CLIENT_STATE_SHUTTING_DOWN = 5;



  /**
   * Retrieves a string representation of the provided client state code.
   *
   * @param  clientState  The client state code for which to retrieve a string
   *                      representation.
   *
   * @return  A string representation of the provided client state code.
   */
  public static String clientStateToString(int clientState)
  {
    switch (clientState)
    {
      case CLIENT_STATE_NOT_CONNECTED:
        return "Not Connected";
      case CLIENT_STATE_IDLE:
        return "Idle";
      case CLIENT_STATE_JOB_NOT_YET_STARTED:
        return "Waiting to Start Job";
      case CLIENT_STATE_RUNNING_JOB:
        return "Processing Job";
      case CLIENT_STATE_SHUTTING_DOWN:
        return "Shutting Down";
      default:
        return "Unknown";
    }
  }



  /**
   * A job control type that indicates the specified job should start running.
   * This can be used to override the start time and start a job immediately.
   */
  public static final int JOB_CONTROL_TYPE_START = 0;



  /**
   * A job control type that indicates the specified job should be signaled to
   * stop running.  The response message from the client will be sent back once
   * the stop request has been issued, but the job may continue to run for some
   * unspecified period of time after that until the stop request is noticed.
   * If the job has not yet started, then it will be cancelled.  If the job has
   * started, then the client may send a status response message providing
   * information about the job at the time it was cancelled.  If the job has
   * already completed, then no action will be taken.
   */
  public static final int JOB_CONTROL_TYPE_STOP = 1;



  /**
   * A job control type that indicates the specified job should be signaled to
   * stop running.  The response message from the client will not be sent back
   * until the job has actually stopped running on that system.
   */
  public static final int JOB_CONTROL_TYPE_STOP_AND_WAIT = 2;



  /**
   * A job control type that indicates the specified job should be signaled to
   * stop running because the SLAMD server is shutting down.  However, the
   * client should not actually close the connection until it receives the
   * server shutdown message.
   */
  public static final int JOB_CONTROL_TYPE_STOP_DUE_TO_SHUTDOWN = 3;



  /**
   * The response code that indicates a successful operation.
   */
  public static final int MESSAGE_RESPONSE_SUCCESS = 0;



  /**
   * The response code that indicates an authentication failed because the
   * authentication ID could not be found.
   */
  public static final int MESSAGE_RESPONSE_UNKNOWN_AUTH_ID = 1;



  /**
   * The response code that indicates an authentication failed because the
   * credentials provided were not valid for the authentication ID.
   */
  public static final int MESSAGE_RESPONSE_INVALID_CREDENTIALS = 2;



  /**
   * The response code that indicates an authentication failed because the
   * requested authentication type is known but not supported by the server.
   */
  public static final int MESSAGE_RESPONSE_UNSUPPORTED_AUTH_TYPE = 3;



  /**
   * The response code that indicates the version of the client software is not
   * one that may be used with the version of the server.
   */
  public static final int MESSAGE_RESPONSE_UNSUPPORTED_CLIENT_VERSION = 4;



  /**
   * The response code that indicates the version of the server software is not
   * one that may be used with the version of the client.
   */
  public static final int MESSAGE_RESPONSE_UNSUPPORTED_SERVER_VERSION = 5;



  /**
   * The response code that indicates there are too many connections to the
   * server to allow a new one.
   */
  public static final int MESSAGE_RESPONSE_CONNECTION_LIMIT_REACHED = 6;



  /**
   * The response code that indicates that an operation was requested without
   * first providing a valid hello message.
   */
  public static final int MESSAGE_RESPONSE_HELLO_REQUIRED = 7;



  /**
   * The response code that indicates that a request from the server referenced
   * a job about which the client has no knowledge.
   */
  public static final int MESSAGE_RESPONSE_NO_SUCH_JOB = 8;



  /**
   * The response code that indicates that a request was made to start a job
   * that was already running.
   */
  public static final int MESSAGE_RESPONSE_JOB_ALREADY_STARTED = 9;



  /**
   * The response code that indicates that a job control request was made with
   * an unknown or unsupported job control type.
   */
  public static final int MESSAGE_RESPONSE_UNSUPPORTED_CONTROL_TYPE = 10;



  /**
   * The response code that indicates that a Java class specified in a job
   * request did not exist on the client system.
   */
  public static final int MESSAGE_RESPONSE_CLASS_NOT_FOUND = 11;



  /**
   * The response code that indicates that a Java class specified in a job
   * request exists but is not a valid job thread class.
   */
  public static final int MESSAGE_RESPONSE_CLASS_NOT_VALID = 12;



  /**
   * The response code that indicates that a client was not able to create an
   * instance of the job thread to execute the specified job.
   */
  public static final int MESSAGE_RESPONSE_JOB_CREATION_FAILURE = 13;



  /**
   * The response code that indicates that a client has refused a job request.
   */
  public static final int MESSAGE_RESPONSE_JOB_REQUEST_REFUSED = 14;



  /**
   * The response code that indicates that an expected response was not received
   * within the expected amount of time.
   */
  public static final int MESSAGE_RESPONSE_NO_RESPONSE = 15;



  /**
   * The response code that indicates a problem occurred that prevented a
   * message from being sent or received.
   */
  public static final int MESSAGE_RESPONSE_LOCAL_ERROR = 16;



  /**
   * The response code that indicates that the client is shutting down.
   */
  public static final int MESSAGE_RESPONSE_CLIENT_SHUTDOWN = 17;



  /**
   * The response code that indicates an error occurred in the server that
   * prevented it from handling a client request properly.
   */
  public static final int MESSAGE_RESPONSE_SERVER_ERROR = 18;



  /**
   * The response code that indicates a start client request could not be
   * processed because there were not enough clients available.
   */
  public static final int MESSAGE_RESPONSE_INSUFFICIENT_CLIENTS = 19;



  /**
   * The response code that indicates that a connection from a client has been
   * rejected by the SLAMD server.
   */
  public static final int MESSAGE_RESPONSE_CLIENT_REJECTED = 20;



  /**
   * Retrieves the string representation of the provided response code.
   *
   * @param  responseCode  The response code for which to retrieve the string
   *                       representation.
   *
   * @return  The string representation of the provided response code.
   */
  public static String responseCodeToString(int responseCode)
  {
    switch (responseCode)
    {
      case MESSAGE_RESPONSE_SUCCESS:
        return "Success";
      case MESSAGE_RESPONSE_UNKNOWN_AUTH_ID:
        return "Unknown auth ID";
      case MESSAGE_RESPONSE_INVALID_CREDENTIALS:
        return "Invalid credentials";
      case MESSAGE_RESPONSE_UNSUPPORTED_AUTH_TYPE:
        return "Unsupported auth type";
      case MESSAGE_RESPONSE_UNSUPPORTED_CLIENT_VERSION:
        return "Unsupported client version";
      case MESSAGE_RESPONSE_UNSUPPORTED_SERVER_VERSION:
        return "Unsupported server version";
      case MESSAGE_RESPONSE_CONNECTION_LIMIT_REACHED:
        return "Connection limit reached";
      case MESSAGE_RESPONSE_HELLO_REQUIRED:
        return "Hello required";
      case MESSAGE_RESPONSE_NO_SUCH_JOB:
        return "No such job";
      case MESSAGE_RESPONSE_JOB_ALREADY_STARTED:
        return "Job already started";
      case MESSAGE_RESPONSE_UNSUPPORTED_CONTROL_TYPE:
        return "Unsupported control type";
      case MESSAGE_RESPONSE_CLASS_NOT_FOUND:
        return "Class not found";
      case MESSAGE_RESPONSE_CLASS_NOT_VALID:
        return "Class not valid";
      case MESSAGE_RESPONSE_JOB_CREATION_FAILURE:
        return "Job creation failure";
      case MESSAGE_RESPONSE_JOB_REQUEST_REFUSED:
        return "Job request refused";
      case MESSAGE_RESPONSE_NO_RESPONSE:
        return "No response received";
      case MESSAGE_RESPONSE_LOCAL_ERROR:
        return "Local error";
      case MESSAGE_RESPONSE_CLIENT_SHUTDOWN:
        return "Client shutdown";
      case MESSAGE_RESPONSE_SERVER_ERROR:
        return "Server error";
      case MESSAGE_RESPONSE_INSUFFICIENT_CLIENTS:
        return "Insufficient clients";
      case MESSAGE_RESPONSE_CLIENT_REJECTED:
        return "Client rejected";
      default:
        return "Unknown";
    }
  }



  /**
   * The message type that the client can use to identify itself to the server.
   */
  public static final int MESSAGE_TYPE_CLIENT_HELLO = 1;



  /**
   * The message type that the server can use to identify itself to the client.
   */
  public static final int MESSAGE_TYPE_SERVER_HELLO = 2;



  /**
   * The message type that is used to provide information about an
   * authentication request.
   */
  public static final int MESSAGE_TYPE_HELLO_RESPONSE = 3;



  /**
   * The message type that is simply used to keep the connection alive between
   * the client and the server.
   */
  public static final int MESSAGE_TYPE_KEEPALIVE = 4;



  /**
   * The message type that the server uses to request information about the
   * status of the client.
   */
  public static final int MESSAGE_TYPE_STATUS_REQUEST = 5;



  /**
   * The message type that the client uses to provide status information to the
   * server.
   */
  public static final int MESSAGE_TYPE_STATUS_RESPONSE = 6;



  /**
   * The message type that the server uses to request that the client begin
   * processing a new job.
   */
  public static final int MESSAGE_TYPE_JOB_REQUEST = 7;



  /**
   * The message type that the client uses to respond to a server's job request.
   */
  public static final int MESSAGE_TYPE_JOB_RESPONSE = 8;



  /**
   * The message type that the client or server may use to indicate that the
   * job status should or will change (e.g., stop requested by user, client
   * shutting down, server shutting down, etc.).
   */
  public static final int MESSAGE_TYPE_JOB_CONTROL_REQUEST = 9;



  /**
   * The message type that is used to provide a response to a job control
   * request.
   */
  public static final int MESSAGE_TYPE_JOB_CONTROL_RESPONSE = 10;



  /**
   * The message type that is used to indicate that job processing has been
   * completed and provide final status information back to the server.
   */
  public static final int MESSAGE_TYPE_JOB_COMPLETED = 11;



  /**
   * The message type that is used to indicate that the SLAMD server is shutting
   * down and therefore the connection to the client is being terminated.
   */
  public static final int MESSAGE_TYPE_SERVER_SHUTDOWN = 12;



  /**
   * The message type that is used by a client manager to identify itself to the
   * SLAMD server.
   */
  public static final int MESSAGE_TYPE_CLIENT_MANAGER_HELLO = 14;



  /**
   * The message type that is used to request that the client manager start one
   * or more clients.
   */
  public static final int MESSAGE_TYPE_START_CLIENT_REQUEST = 15;



  /**
   * The message type that is used to provide a response to a start client
   * request.
   */
  public static final int MESSAGE_TYPE_START_CLIENT_RESPONSE = 16;



  /**
   * The message type that is used to request that the client manager stop
   * one or more clients.
   */
  public static final int MESSAGE_TYPE_STOP_CLIENT_REQUEST = 17;



  /**
   * The message type that is used to provide a response to a stop client
   * request.
   */
  public static final int MESSAGE_TYPE_STOP_CLIENT_RESPONSE = 18;



  /**
   * The message type that is used to register a stat tracker with the SLAMD
   * server's real-time stat reporting subsystem.
   */
  public static final int MESSAGE_TYPE_REGISTER_STATISTIC = 19;



  /**
   * The message type that is used to provide statistical data to the SLAMD
   * server's real-time stat reporting subsystem.
   */
  public static final int MESSAGE_TYPE_REAL_TIME_STAT_DATA = 20;



  /**
   * The name of the configuration parameter that specifies the location of
   * configuration parameters in the configuration directory.
   */
  public static final String PARAM_CONFIG_PARAMETER_BASE =
       "config_parameter_base_dn";



  /**
   * The name of the configuration parameter that specifies the location of
   * information about scheduled jobs in the configuration directory.
   */
  public static final String PARAM_CONFIG_SCHEDULED_JOB_BASE =
      "config_scheduled_job_base_dn";



  /**
   * The name of the configuration parameter that specifies the location of
   * information about the job classes defined in the configuration directory.
   */
  public static final String PARAM_CONFIG_JOB_CLASS_BASE =
       "config_job_class_base_dn";



  /**
   * The name of the configuration parameter that specifies a URL that can be
   * used to access the SLAMD admin interface.
   */
  public static final String PARAM_SERVLET_BASE_URI = "servlet_base_uri";



  /**
   * The name of the configuration parameter that specifies the port number on
   * which the client manager listener will listen for connections from client
   * managers.
   */
  public static final String PARAM_CLIENT_MANAGER_LISTENER_PORT =
       "client_manager_listen_port";



  /**
   * The name of the configuration parameter that specifies the maximum length
   * of time in seconds that the SLAMD server will wait for a response to a
   * request sent to a client manager.
   */
  public static final String PARAM_CLIENT_MANAGER_MAX_WAIT_TIME =
       "client_manager_max_wait_time";



  /**
   * The name of the configuration parameter that specifies how frequently
   * keepalive messages will be sent between the client and the server to
   * ensure that the connection does not get closed (and to detect failed
   * connections on either end).
   */
  public static final String PARAM_LISTENER_KEEPALIVE_INTERVAL =
       "listener_keepalive_interval";



  /**
   * The keepalive interval value that indicates no keepalive is to be used.
   */
  public static final int NO_KEEPALIVE_INTERVAL = 0;



  /**
   * The name of the configuration parameter that specifies the maximum number
   * of clients that may be connected to the SLAMD server at any given time.
   */
  public static final String PARAM_LISTENER_MAX_CLIENTS =
       "listener_max_clients";



  /**
   * The value that indicates there will not be a maximum number of concurrent
   * clients that may be connected at any given time.
   */
  public static final int NO_MAX_CLIENTS = 0;



  /**
   * The name of the configuration parameter that specifies the maximum number
   * of collection intervals that the real-time stat manager should retain for
   * each job.
   */
  public static final String PARAM_MAX_STAT_INTERVALS = "max_stat_intervals";



  /**
   * The default number of collection intervals that should be retained by the
   * real-time stat reporter.
   */
  public static final int DEFAULT_MAX_STAT_INTERVALS = 60;



  /**
   * The name of the configuration parameter that specifies the port number on
   * which the SLAMD server will listen for client connections.
   */
  public static final String PARAM_LISTENER_PORT = "listener_port";



  /**
   * The name of the configuration parameter that specifies the port number on
   * which the SLAMD server will listen for resource monitor client connections.
   */
  public static final String PARAM_MONITOR_LISTENER_PORT =
       "monitor_listener_port";



  /**
   * The name of the configuration parameter that specifies the log level that
   * should be used by the server to determine which messages get logged.
   */
  public static final String PARAM_LOG_LEVEL = "log_level";



  /**
   * The name of the configuration parameter that specifies the delay in seconds
   * between iterations of the logging thread's poll loop.
   */
  public static final String PARAM_LOG_POLL_DELAY = "log_poll_delay";



  /**
   * The name of the configuration parameter that specifies the delay in seconds
   * between iterations of the SLAMD scheduler loop.
   */
  public static final String PARAM_SCHEDULER_DELAY = "scheduler_delay";



  /**
   * The name of the configuration parameter that specifies the number of
   * seconds before the job's actual start time that the scheduler should start
   * sending the request information to clients.
   */
  public static final String PARAM_SCHEDULER_START_BUFFER =
       "scheduler_request_buffer";



  /**
   * The name of the configuration parameter that holds a list of all jobs that
   * have been marked disabled.
   */
  public static final String PARAM_DISABLED_JOBS = "disabled_jobs";



  /**
   * The name of the configuration parameter that holds a list of all jobs that
   * have been scheduled but have not yet started running and are not disabled.
   */
  public static final String PARAM_PENDING_JOBS = "pending_jobs";



  /**
   * The name of the configuration parameter that holds a list of all jobs that
   * are currently running.
   */
  public static final String PARAM_RUNNING_JOBS = "running_jobs";



  /**
   * The name of the configuration parameter that specifies the port number on
   * which the stat listener will listen for connections from clients.
   */
  public static final String PARAM_STAT_LISTENER_PORT =
       "stat_listen_port";



  /**
   * The name of the configuration parameter that specifies whether the logger
   * will always flush the log information to disk as soon as it is written.
   * This is only used if synchronous logging is used.
   */
  public static final String PARAM_LOG_ALWAYS_FLUSH = "log_always_flush";



  /**
   * The default value that will be used for the "always flush" parameter if no
   * value is specified.
   */
  public static final boolean DEFAULT_LOG_ALWAYS_FLUSH = true;



  /**
   * The name of the configuration parameter that specifies whether the logger
   * should be used.
   */
  public static final String PARAM_LOGGER_ENABLED = "logger_enabled";



  /**
   * The default value that will be used for the "logger enabled" parameter if
   * no value is specified.
   */
  public static final boolean DEFAULT_LOGGER_ENABLED = true;



  /**
   * The name of the configuration parameter that specifies the name of the file
   * to which log messages will be written.
   */
  public static final String PARAM_LOG_FILENAME = "log_filename";



  /**
   * The name of the configuration parameter that specifies whether logging is
   * to be done synchronously (when the call is made to logMessage) or
   * asynchronously (in a separate thread).
   */
  public static final String PARAM_LOG_ASYNCHRONOUSLY = "log_asynchronously";



  /**
   * The default value that will be used for the "log asynchronously" parameter
   * if no value is specified.
   */
  public static final boolean DEFAULT_LOG_ASYNCHRONOUSLY = false;



  /**
   * The name of the configuration parameter that specifies the maximum length
   * of time in seconds to wait for a client response for a solicited request.
   */
  public static final String PARAM_MAX_RESPONSE_WAIT_TIME =
       "max_response_wait_time";



  /**
   * The name of the configuration parameter that specifies whether the SLAMD
   * server should allow e-mail messages to be sent when certain kinds of
   * events occur.
   */
  public static final String PARAM_ENABLE_MAIL_ALERTS = "enable_mail_alerts";



  /**
   * The name of the configuration parameter that specifies the address of the
   * mail server to use for sending alert messages.
   */
  public static final String PARAM_SMTP_SERVER = "smtp_server";



  /**
   * The name of the configuration parameter that specifies the port of the
   * mail server to use for sending alert messages.
   */
  public static final String PARAM_SMTP_PORT = "smtp_port";



  /**
   * The default port number that will be used to contact the SMTP server.
   */
  public static final int DEFAULT_SMTP_PORT = 25;



  /**
   * The name of the configuration parameter that specifies the e-mail address
   * that should be used for the sender of alert messages.
   */
  public static final String PARAM_MAIL_FROM_ADDRESS = "mail_from_address";



  /**
   * The name of the configuration parameter that specifies whether to always
   * show advanced scheduling options when scheduling a job.
   */
  public static final String PARAM_ALWAYS_SHOW_ADVANCED_OPTIONS =
       "always_show_advanced_options";



  /**
   * The name of the configuration parameter that specifies whether the graphing
   * capability of SLAMD should be disabled (e.g., if no appropriate graphics
   * environment is available for use by the server).
   */
  public static final String PARAM_DISABLE_GRAPHS = "disable_graphs";



  /**
   * The name of the configuration parameter that specifies whether graphing
   * should be performed in the same or a separate window.
   */
  public static final String PARAM_GRAPH_IN_NEW_WINDOW = "graph_in_new_window";



  /**
   * Indicates whether a new window should be created for graphs by default.
   */
  public static final boolean DEFAULT_GRAPH_IN_NEW_WINDOW = true;



  /**
   * The name of the configuration parameter that specifies whether the
   * file upload capability should be disabled.
   */
  public static final String PARAM_DISABLE_UPLOADS = "disable_uploads";



  /**
   * The name of the configuration parameter that specifies whether the server
   * should hide the individual iterations of an optimizing job when viewing the
   * list of completed jobs.
   */
  public static final String PARAM_HIDE_OPTIMIZING_ITERATIONS =
       "hide_optimizing_iterations";



  /**
   * The name of the configuration parameter that specifies whether to include
   * the address of the server in the title of the generated HTML pages.
   */
  public static final String PARAM_INCLUDE_SERVER_IN_TITLE =
       "include_server_in_title";



  /**
   * The name of the configuration parameter that specifies whether to enable
   * the management of options only used when operating in restricted
   * read-only mode.
   */
  public static final String PARAM_MANAGE_READ_ONLY = "manage_read_only";



  /**
   * The name of the configuration parameter that specifies the maximum file
   * size that may be uploaded.
   */
  public static final String PARAM_MAX_UPLOAD_SIZE = "max_upload_size";



  /**
   * The default value that will be used for the maximum file upload size if
   * none is specified.
   */
  public static final int DEFAULT_MAX_UPLOAD_SIZE = -1;



  /**
   * The name of the configuration parameter that specifies the set of
   * optimization algorithms that have been defined for use in the SLAMD server.
   */
  public static final String PARAM_OPTIMIZATION_ALGORITHMS =
       "optimization_algorithms";



  /**
   * The name of the configuration parameter that specifies whether a default
   * value (set to the current time) should be displayed for the start time if
   * none has been provided.
   */
  public static final String PARAM_POPULATE_START_TIME = "populate_start_time";



  /**
   * The name of the configuration parameter that specifies the Java classes
   * that may be used for generating reports of SLAMD data.
   */
  public static final String PARAM_REPORT_GENERATOR_CLASSES =
       "report_generator_classes";



  /**
   * The name of the configuration parameter that specifies whether the client
   * listener should require clients to authenticate.
   */
  public static final String PARAM_REQUIRE_AUTHENTICATION =
       "require_authentication";



  /**
   * The name of the configuration parameter that specifies whether the client
   * listener should use SSL.
   */
  public static final String PARAM_LISTENER_USE_SSL = "listener_use_ssl";



  /**
   * The name of the configuration parameter that specifies whether the login ID
   * for the currently-authenticated user will be displayed in the
   * administrative interface if access control is enabled.
   */
  public static final String PARAM_SHOW_LOGIN_ID = "ui_show_login_id";



  /**
   * The name of the configuration parameter that specifies whether the time
   * should be displayed in the navigation sidebar.
   */
  public static final String PARAM_SHOW_TIME = "ui_show_time";



  /**
   * The name of the configuration parameter that can be used to add content to
   * the HTML header for pages displayed through the admin interface.
   */
  public static final String PARAM_ADD_TO_HTML_HEADER = "add_to_html_header";



  /**
   * The name of the configuration parameter that can be used to override the
   * default header placed at the top of most pages in the administrative
   * interface.
   */
  public static final String PARAM_PAGE_HEADER = "page_header";



  /**
   * The default page header that will be used if no other value is specified.
   */
  public static final String DEFAULT_PAGE_HEADER =
       "          <TABLE WIDTH=\"100%\" BORDER=\"0\" CELLPADDING=\"8\" " +
            "CELLSPACING=\"0\">" + EOL +
       "            <TR>" + EOL +
       "              <TD CLASS=\"yellow_background\" ALIGN=\"LEFT\" " +
           "WIDTH=\"50%\">" + EOL +
       "                SLAMD Distributed<BR>" + EOL +
       "                Load Generation Engine" + EOL +
       "              </TD>" + EOL +
       "              <TD WIDTH=\"0%\"></TD>" + EOL +
       "              <TD CLASS=\"red_background\" ALIGN=\"RIGHT\" " +
            "WIDTH=\"50%\">" + EOL +
       "                " + Constants.HEADER_TAG_SLAMD_VERSION + EOL +
       "                <BR>" + EOL +
       "                <FONT SIZE=\"-2\">" +
           Constants.HEADER_TAG_SLAMD_UNOFFICIAL_BUILD + "</FONT>" + EOL +
       "              </TD>" + EOL +
       "            </TR>" + EOL +
       "          </TABLE>" + EOL +
       "          <FONT SIZE=\"-2\">&nbsp;<BR></FONT>" + EOL;



  /**
   * The name of the configuration parameter that can be used to override the
   * default footer placed at the bottom of every page in the administrative
   * interface.
   */
  public static final String PARAM_PAGE_FOOTER = "page_footer";



  /**
   * The default page footer that will be used if no other value is specified.
   */
  public static final String DEFAULT_PAGE_FOOTER = EOL;



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD build date.
   */
  public static final String HEADER_TAG_SLAMD_BUILD_DATE =
       "####SLAMD_BUILD_DATE####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD logo.
   */
  public static final String HEADER_TAG_SLAMD_LOGO = "####SLAMD_LOGO####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD major version.
   */
  public static final String HEADER_TAG_SLAMD_MAJOR_VERSION =
       "####SLAMD_MAJOR_VERSION####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD minor version.
   */
  public static final String HEADER_TAG_SLAMD_MINOR_VERSION =
       "####SLAMD_MINOR_VERSION####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD point version.
   */
  public static final String HEADER_TAG_SLAMD_POINT_VERSION =
       "####SLAMD_POINT_VERSION####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD version.
   */
  public static final String HEADER_TAG_SLAMD_VERSION = "####SLAMD_VERSION####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the SLAMD unofficial build ID if this is an unofficial build, or nothing if
   * it is an official build.
   */
  public static final String HEADER_TAG_SLAMD_UNOFFICIAL_BUILD_ID =
       "####SLAMD_UNOFFICIAL_BUILD_ID####";



  /**
   * The tag that may be used in the header or footer that will be replaced with
   * the text "Unofficial Build" if this is an unofficial build, or nothing if
   * it is an official build.
   */
  public static final String HEADER_TAG_SLAMD_UNOFFICIAL_BUILD =
       "####SLAMD_UNOFFICIAL_BUILD####";



  /**
   * The HTML string that will be used as the default main HTML page text when
   * adding configuration data into the config directory.
   */
  public static final String DEFAULT_HTML =
       "<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
       "\">SLAMD Distributed Load Generation Engine</SPAN>\n" +
       "<BR><BR>\n" +
       "The SLAMD Distributed Load Generation Engine (SLAMD) is a Java-based " +
       "tool designed for benchmarking and performance analysis of " +
       "network-based applications.\n" +
       "<BR><BR>\n" +
       "You may use this HTML interface to configure and schedule jobs for " +
       "execution either immediately or at a future date, at which time " +
       "jobs will be distributed to remote clients for processing.\n"  +
       "Clients collect statistical information during the course of job " +
       "execution, which is provided back to the SLAMD server upon its "+
       "completion where it may be displayed in a variety of forms or " +
       "exported for external use.\n" +
       "<BR><BR>\n" +
       "To get started, click on one of the links on the left.\n";



  /**
   * The name of the configuration parameter that can specify the main page HTML
   * text to use if the default is not acceptable for some reason.
   */
  public static final String PARAM_DEFAULT_HTML = "default_html";



  /**
   * The data that will be used as the style sheet in the HTML header of the
   * administrative interface.
   */
  public static final String STYLE_SHEET_DATA =
       "    <STYLE TYPE=\"text/css\">" + EOL +
       "      BODY               { color: #000000; " +
            "background-color: #FFFFFF }" + EOL +
       "      A:link             { color: #594FBF; text-decoration: none }" +
            EOL +
       "      A:visited          { color: #594FBF; text-decoration: none }" +
            EOL +
       "      A:hover            { color: #594FBF; " +
            "text-decoration: underline }" + EOL +
       "      .blue_background   { color: #FFFFFF; " +
            "background-color: #594FBF; font-family: sans-serif; " +
            "font-weight: bold }" + EOL +
       "      .yellow_background { color: #594FBF; " +
            "background-color: #FBE249; font-family: sans-serif; " +
            "font-weight: bold }" + EOL +
       "      .red_background    { color: #FFFFFF; " +
            "background-color: #D12124; font-family: sans-serif; " +
            "font-weight: bold }" + EOL +
       "      .copyright         { font-family: sans-serif; font-size: 75% }" +
            EOL +
       "      .nav_bar           { background-color: #F1F1F1 }" + EOL +
       "      .nav_header        { color: #000000; font-family: sans-serif; " +
            "font-weight: bold; font-size: 75% }" + EOL +
       "      .nav_link          { color: #000000; font-family: sans-serif; " +
            "font-size: 75% }" + EOL +
       "      .warning           { color: #D12124; font-weight: bold }" + EOL +
       "      .main_form         { background-color: #F1F1F1 }" + EOL +
       "      .main_header       { color: #000000; font-family: sans-serif; " +
            "font-weight: bold; font-size: 125% }" + EOL +
       "      .form_caption      { color: #000000 }" + EOL +
       "      .job_summary_a     { background-color: #FFFFFF }" + EOL +
       "      .job_summary_b     { background-color: #F1F1F1 }" + EOL +
       "    </STYLE>" + EOL;



  /**
   * The name of the configuration parameter that can specify the style sheet to
   * use for the SLAMD administrative interface if the default is not
   * acceptable.
   */
  public static final String PARAM_STYLE_SHEET = "style_sheet";



  /**
   * The style sheet tag that will be used for the copyright in the page footer.
   */
  public static final String STYLE_COPYRIGHT = "copyright";



  /**
   * The style sheet tag that will be used for elements that should be displayed
   * with the "warning text" format.
   */
  public static final String STYLE_WARNING_TEXT = "warning";



  /**
   * The style sheet tag that will be used for elements that should be displayed
   * in HTML forms.
   */
  public static final String STYLE_MAIN_FORM = "main_form";



  /**
   * The style sheet tag that will be used for the captions of form elements.
   */
  public static final String STYLE_FORM_CAPTION = "form_caption";



  /**
   * The style sheet tag that will be used for elements that should be displayed
   * in the navigation bar.
   */
  public static final String STYLE_NAV_BAR = "nav_bar";



  /**
   * The style sheet tag that will be used for headers in the navigation bar.
   */
  public static final String STYLE_NAV_BAR_HEADER = "nav_header";



  /**
   * The style sheet tag that will be used for links in the navigation bar.
   */
  public static final String STYLE_NAV_BAR_LINK = "nav_link";



  /**
   * The style sheet tag that will be used for odd-numbered job summary lines.
   */
  public static final String STYLE_JOB_SUMMARY_LINE_A = "job_summary_a";



  /**
   * The style sheet tag that will be used for even-numbered job summary lines.
   */
  public static final String STYLE_JOB_SUMMARY_LINE_B = "job_summary_b";



  /**
   * The style sheet tag that will be used for the main header on the page.
   */
  public static final String STYLE_MAIN_HEADER = "main_header";



  /**
   * The character that will be used for bullets in the navigation bar.
   */
  public static final String UI_NAV_BAR_BULLET = "&raquo;";



  /**
   * The name of the configuration parameter that indicates whether to use the
   * custom job class loader.
   */
  public static final String PARAM_USE_CUSTOM_CLASS_LOADER =
       "use_custom_class_loader";



  /**
   * The name of the Java property that, if set with a value of "true" or "yes"
   * or "on" or "1", will cause the custom class loader to be disabled,
   * regardless of the setting in the configuration directory.
   */
  public static final String PROPERTY_DISABLE_CUSTOM_CLASS_LOADER =
       "com.slamd.disableCustomClassLoader";



  /**
   * The name of the configuration parameter that specifies the number of jobs
   * that should be stored in the job cache.
   */
  public static final String PARAM_JOB_CACHE_SIZE = "job_cache_size";



  /**
   * The default number of jobs to store in the in-memory job cache.
   */
  public static final int DEFAULT_JOB_CACHE_SIZE = 25;



  /**
   * The name of the configuration parameter that specifies the set of job
   * clasess that have been registered with the SLAMD server.
   */
  public static final String PARAM_JOB_CLASSES = "job_classes";



  /**
   * A job state that indicates that the state of the associated job is
   * unknown.
   */
  public static final int JOB_STATE_UNKNOWN = 0;



  /**
   * The message corresponding to the <CODE>JOB_STATE_UNKNOWN</CODE> job state.
   */
  public static final String JOB_STATE_UNKNOWN_STRING = "Unknown";



  /**
   * The job state that indicates information was requested about a job that is
   * not defined to the client.
   */
  public static final int JOB_STATE_NO_SUCH_JOB = 1;



  /**
   * The message corresponding to the <CODE>JOB_STATE_NO_SUCH_JOB</CODE> job
   * state.
   */
  public static final String JOB_STATE_NO_SUCH_JOB_STRING = "No such job";



  /**
   * A job state that indicates that the job is known but has not  yet been
   * initialized.
   */
  public static final int JOB_STATE_UNINITIALIZED = 2;



  /**
   * The message corresponding to the <CODE>JOB_STATE_UNINITIALIZED</CODE> job
   * state.
   */
  public static final String JOB_STATE_UNINITIALIZED_STRING = "Uninitialized";



  /**
   * A job state that indicates that the job has been initialized but has not
   * yet been started.
   */
  public static final int JOB_STATE_NOT_YET_STARTED = 3;



  /**
   * The message corresponding to the <CODE>JOB_STATE_NOT_YET_STARTED</CODE> job
   * state.
   */
  public static final String JOB_STATE_NOT_YET_STARTED_STRING =
       "Not yet started";



  /**
   * A job state that indicates that the job is currently running.
   */
  public static final int JOB_STATE_RUNNING = 4;



  /**
   * The message corresponding to the <CODE>JOB_STATE_RUNNING</CODE> job state.
   */
  public static final String JOB_STATE_RUNNING_STRING = "Running";



  /**
   * A job state that indicates that the job has completed successfully.
   */
  public static final int JOB_STATE_COMPLETED_SUCCESSFULLY = 5;



  /**
   * The message corresponding to the
   * <CODE>JOB_STATE_COMPLETED_SUCCESSFULLY</CODE> job state.
   */
  public static final String JOB_STATE_COMPLETED_SUCCESSFULLY_STRING =
       "Completed successfully";



  /**
   * A job state that indicates that the job has completed but there were errors
   * that may impact the results.
   */
  public static final int JOB_STATE_COMPLETED_WITH_ERRORS = 6;



  /**
   * The message corresponding to the
   * <CODE>JOB_STATE_COMPLETED_WITH_ERRORS</CODE> job state.
   */
  public static final String JOB_STATE_COMPLETED_WITH_ERRORS_STRING =
       "Completed but with one or more errors";



  /**
   * A job state that indicates that the job was stopped because a significant
   * error occurred (as opposed to an error that would have allowed the job to
   * continue running on other clients).
   */
  public static final int JOB_STATE_STOPPED_DUE_TO_ERROR = 7;



  /**
   * The message corresponding to the
   * <CODE>JOB_STATE_STOPPED_DUE_TO_ERROR</CODE> job state.
   */
  public static final String JOB_STATE_STOPPED_DUE_TO_ERROR_STRING =
       "Stopped because of an error";



  /**
   * A job state that indicates that the job was stopped because it had run for
   * the maximum length of time allowed.
   */
  public static final int JOB_STATE_STOPPED_DUE_TO_DURATION = 8;



  /**
   * The message corresponding to the
   * <CODE>JOB_STATE_STOPPED_DUE_TO_DURATION</CODE> job state.
   */
  public static final String JOB_STATE_STOPPED_DUE_TO_DURATION_STRING =
       "Stopped because the maximum duration had been reached";



  /**
   * A job state that indicates that the job was stopped because the specified
   * stop time had been reached.
   */
  public static final int JOB_STATE_STOPPED_DUE_TO_STOP_TIME = 9;



  /**
   * The message corresponding to the
   * <CODE>JOB_STATE_STOPPED_DUE_TO_STOP_TIME</CODE> job state.
   */
  public static final String JOB_STATE_STOPPED_DUE_TO_STOP_TIME_STRING =
       "Stopped because the stop time had been reached";



  /**
   * A job state that indicates that the job has been stopped by manual
   * intervention.
   */
  public static final int JOB_STATE_STOPPED_BY_USER = 10;



  /**
   * The message corresponding to the <CODE>JOB_STATE_STOPPED_BY_USER</CODE>
   * job state.
   */
  public static final String JOB_STATE_STOPPED_BY_USER_STRING =
    "Stopped by administrative request";



  /**
   * A job state that indicates that the job has been stopped as part of the
   * SLAMD server shutdown.
   */
  public static final int JOB_STATE_STOPPED_BY_SHUTDOWN = 11;



  /**
   * The message corresponding to the <CODE>JOB_STATE_STOPPED_BY_USER</CODE>
   * job state.
   */
  public static final String JOB_STATE_STOPPED_BY_SHUTDOWN_STRING =
    "Stopped by SLAMD server shutdown";



  /**
   * A job state that indicates that the job has been cancelled before it had a
   * chance to start.
   */
  public static final int JOB_STATE_CANCELLED = 12;



  /**
   * The message corresponding to the <CODE>JOB_STATE_CANCELLED</CODE> job
   * state.
   */
  public static final String JOB_STATE_CANCELLED_STRING =
       "Cancelled before job startup";



  /**
   * A job state that indicates that the job has been temporarily disabled and
   * should not be considered eligible to start.
   */
  public static final int JOB_STATE_DISABLED = 13;



  /**
   * The message corresponding to the <CODE>JOB_STATE_DISABLED</CODE> job state.
   */
  public static final String JOB_STATE_DISABLED_STRING =
       "Temporarily Disabled";



  /**
   * Retrieves the message that corresponds to the provided job state constant.
   *
   * @param  jobState  The job state value to be converted to a message.
   *
   * @return  The message that corresponds to the provided job state constant.
   */
  public static String jobStateToString(int jobState)
  {
    switch (jobState)
    {
      case JOB_STATE_UNINITIALIZED:
                return JOB_STATE_UNINITIALIZED_STRING;
      case JOB_STATE_NOT_YET_STARTED:
                return JOB_STATE_NOT_YET_STARTED_STRING;
      case JOB_STATE_RUNNING:
                return JOB_STATE_RUNNING_STRING;
      case JOB_STATE_COMPLETED_SUCCESSFULLY:
                return JOB_STATE_COMPLETED_SUCCESSFULLY_STRING;
      case JOB_STATE_COMPLETED_WITH_ERRORS:
                return JOB_STATE_COMPLETED_WITH_ERRORS_STRING;
      case JOB_STATE_STOPPED_DUE_TO_ERROR:
                return JOB_STATE_STOPPED_DUE_TO_ERROR_STRING;
      case JOB_STATE_STOPPED_DUE_TO_DURATION:
                return JOB_STATE_STOPPED_DUE_TO_DURATION_STRING;
      case JOB_STATE_STOPPED_DUE_TO_STOP_TIME:
                return JOB_STATE_STOPPED_DUE_TO_STOP_TIME_STRING;
      case JOB_STATE_STOPPED_BY_USER:
                return JOB_STATE_STOPPED_BY_USER_STRING;
      case JOB_STATE_STOPPED_BY_SHUTDOWN:
                return JOB_STATE_STOPPED_BY_SHUTDOWN_STRING;
      case JOB_STATE_CANCELLED:
                return JOB_STATE_CANCELLED_STRING;
      case JOB_STATE_DISABLED:
                return JOB_STATE_DISABLED_STRING;
      default:  return JOB_STATE_UNKNOWN_STRING;
    }
  }



  /**
   * The flag that indicates that a resource monitor client is running on an
   * unrecognized operating system.
   */
  public static final int OS_TYPE_UNKNOWN = 0;



  /**
   * The flag that indicates that a resource monitor client is running on a
   * Solaris system.
   */
  public static final int OS_TYPE_SOLARIS = 1;



  /**
   * The flag that indicates that a resource monitor client is running on a
   * Linux system.
   */
  public static final int OS_TYPE_LINUX = 2;



  /**
   * The flag that indicates that a resource monitor client is running on an
   * HP-UX system.
   */
  public static final int OS_TYPE_HPUX = 3;



  /**
   * The flag that indicates that a resource monitor client is running on an AIX
   * system.
   */
  public static final int OS_TYPE_AIX = 4;



  /**
   * The flag that indicates that a resource monitor client is running on a
   * Windows system.
   */
  public static final int OS_TYPE_WINDOWS = 5;



  /**
   * The flag that indicates that a resource monitor client is running on an
   * OS X system.
   */
  public static final int OS_TYPE_OSX = 6;



  /**
   * The option value that indicates that no resource monitor statistical
   * information should be graphed.
   */
  public static final String MONITOR_STAT_NONE =
       "No Resource Monitor Statistics";



  /**
   * The option value that indicates that all resource monitor statistical
   * information should be graphed.
   */
  public static final String MONITOR_STAT_ALL =
       "All Resource Monitor Statistics";



  /**
   * The response header that will be used to provide information about an error
   * that occurred during processing.
   */
  public static final String RESPONSE_HEADER_ERROR_MESSAGE = "error-message";



  /**
   * The base path for content generated based on MD5 digests of the query
   * string.
   */
  public static final String MD5_CONTENT_BASE_PATH = "com/slamd/md5";



  /**
   * MD5 digests of query strings that may be used to access special SLAMD
   * content.
   */
  public static final String[] QUERY_STRING_MD5 =
  {
    "SgSQJSCxuDcCZ2WzKws9Ww==",
    "hEC9A3YJaQqa46M2+KA1Cw=="
  };



  /**
   * The name of the servlet parameter that specifies which resource monitor
   * clients should have their statistics graphed.
   */
  public static final String SERVLET_PARAM_MONITOR_CLIENT = "monitor_client";



  /**
   * The name of the servlet parameter that specifies which classes of resource
   * monitor statistic(s) should be graphed.
   */
  public static final String SERVLET_PARAM_MONITOR_CLASS = "monitor_class";



  /**
   * The name of the servlet parameter that specifies which resource monitor
   * statistic(s) should be graphed.
   */
  public static final String SERVLET_PARAM_MONITOR_STAT = "monitor_stat";



  /**
   * Retrieves the job state associated with the provided string.
   *
   * @param  stateString  The string that is to be converted to a job state.
   *
   * @return  The job state associated with the provided string.
   */
  public static int stringToJobState(String stateString)
  {
    if (stateString.equalsIgnoreCase(JOB_STATE_UNINITIALIZED_STRING))
    {
      return JOB_STATE_UNINITIALIZED;
    }
    else if (stateString.equalsIgnoreCase(JOB_STATE_NOT_YET_STARTED_STRING))
    {
      return JOB_STATE_NOT_YET_STARTED;
    }
    else if (stateString.equalsIgnoreCase(JOB_STATE_RUNNING_STRING))
    {
      return JOB_STATE_RUNNING;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_COMPLETED_SUCCESSFULLY_STRING))
    {
      return JOB_STATE_COMPLETED_SUCCESSFULLY;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_COMPLETED_WITH_ERRORS_STRING))
    {
      return JOB_STATE_COMPLETED_WITH_ERRORS;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_STOPPED_DUE_TO_ERROR_STRING))
    {
      return JOB_STATE_STOPPED_DUE_TO_ERROR;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_STOPPED_DUE_TO_DURATION_STRING))
    {
      return JOB_STATE_STOPPED_DUE_TO_DURATION;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_STOPPED_DUE_TO_STOP_TIME_STRING))
    {
      return JOB_STATE_STOPPED_DUE_TO_STOP_TIME;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_STOPPED_BY_USER_STRING))
    {
      return JOB_STATE_STOPPED_BY_USER;
    }
    else if (stateString.equalsIgnoreCase(
                  JOB_STATE_STOPPED_BY_SHUTDOWN_STRING))
    {
      return JOB_STATE_STOPPED_BY_SHUTDOWN;
    }
    else if (stateString.equalsIgnoreCase(JOB_STATE_CANCELLED_STRING))
    {
      return JOB_STATE_CANCELLED;
    }
    else if (stateString.equalsIgnoreCase(JOB_STATE_DISABLED_STRING))
    {
      return JOB_STATE_DISABLED;
    }
    else
    {
      return JOB_STATE_UNKNOWN;
    }
  }



  /**
   * The job statistics category that is used to indicate that the user wants
   * to see summary statistics for the job (aggregated from all clients and
   * threads).
   */
  public static final int STAT_CATEGORY_JOB_STATS = 1;



  /**
   * The text string that will be used for the
   * <CODE>STAT_CATEGORY_JOB_STATS</CODE> detail level.
   */
  public static final String STAT_CATEGORY_JOB_STATS_STR =
       "Overall Job Summary Statistics";



  /**
   * The job statistics category that is used to indicate that the user wants
   * to see summary statistics for each client (aggregated from all threads on
   * that client).
   */
  public static final int STAT_CATEGORY_CLIENT_STATS = 2;



  /**
   * The text string that will be used for the
   * <CODE>STAT_CATEGORY_CLIENT_STATS</CODE> detail level.
   */
  public static final String STAT_CATEGORY_CLIENT_STATS_STR =
       "Summary Statistics for Each Client";



  /**
   * The job statistics category that is used to indicate that the user wants
   * to see summary statistics for each thread.
   */
  public static final int STAT_CATEGORY_THREAD_STATS = 4;



  /**
   * The text string that will be used for the
   * <CODE>STAT_CATEGORY_THREAD_STATS</CODE> detail level.
   */
  public static final String STAT_CATEGORY_THREAD_STATS_STR =
       "Detail Statistics for Each Client Thread";



  /**
   * The category names that correspond to the various levels of detail
   * specified by the statistics categories.
   */
  public static final String[] STAT_CATEGORY_NAMES = new String[]
  {
    STAT_CATEGORY_JOB_STATS_STR,
    STAT_CATEGORY_CLIENT_STATS_STR,
    STAT_CATEGORY_THREAD_STATS_STR
  };



  /**
   * The stat report type that indicates that the provided value is to be
   * added to other values for the same interval.
   */
  public static final int STAT_REPORT_TYPE_ADD = 1;



  /**
   * The stat report type that indicates that the provided value is to be
   * averaged with other values for the same interval.
   */
  public static final int STAT_REPORT_TYPE_AVERAGE = 2;



  /**
   * The stat report type that indicates the indicated client thread is done
   * providing statistical information.
   */
  public static final int STAT_REPORT_TYPE_DONE = 0;



  /**
   * The initialization parameter that specifies whether the configuration has
   * been initialized.
   */
  public static final String SERVLET_INIT_PARAM_CONFIGURED = "configured";



  /**
   * The initialization parameter that specifies the path to the directory
   * containing the configuration database files.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_DB_DIRECTORY =
       "config_db_directory";



  /**
   * The initialization parameter that specifies the IP address or DNS hostname
   * of the configuration directory.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_DIR_HOST = "config_host";



  /**
   * The initialization parameter that specifies the port number of the
   * configuration directory.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_DIR_PORT = "config_port";



  /**
   * The initialization parameter that specifies the DN to use to bind to the
   * configuration directory.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_DIR_BIND_DN =
       "config_bind_dn";



  /**
   * The initialization parameter that specifies the password to use to bind to
   * the configuration directory.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_DIR_BIND_PW =
      "config_bind_pw";



  /**
   * The initialization parameter that specifies the location of the SLAMD
   * information in the configuration directory.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_DIR_BASE =
       "config_base_dn";



  /**
   * The initialization parameter that specifies the location of the job class
   * files.
   */
  public static final String SERVLET_INIT_PARAM_JOB_CLASS_PATH =
       "job_class_path";



  /**
   * The initialization parameter that specifies the IP address or DNS hostname
   * of the user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_DIR_HOST = "user_host";



  /**
   * The initialization parameter that specifies the port number of the
   * user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_DIR_PORT = "user_port";



  /**
   * The initialization parameter that specifies the DN to use to bind to the
   * user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_DIR_BIND_DN =
       "user_bind_dn";



  /**
   * The initialization parameter that specifies the password to use to bind to
   * the user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_DIR_BIND_PW =
       "user_bind_pw";



  /**
   * The initialization parameter that specifies the location of the user
   * account entries in the user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_DIR_BASE =
       "user_base_dn";



  /**
   * The initialization parameter that specifies the configuration file that
   * contains all the other initialization data.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_FILE =
       "config_file";



  /**
   * The header that will be written to the SLAMD configuration file whenever
   * changes are made to it.
   */
  public static final String CONFIG_FILE_HEADER =
       "SLAMD administration interface configuration file." + EOL +
       '#' + EOL +
       "#Do not edit this file while the SLAMD server is running.  For best " +
            "results," + EOL +
       "#edit these parameters through the administrative interface." + EOL +
       '#' + EOL +
       "#This file was last modified:";



  /**
   * The initialization parameter that specifies whether the communication with
   * the configuration directory should use SSL.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_USE_SSL =
       "config_use_ssl";



  /**
   * The initialization parameter that specifies whether the SLAMD server should
   * blindly trust any SSL certificate presented by the config directory.
   */
  public static final String SERVLET_INIT_PARAM_CONFIG_BLIND_TRUST =
       "config_blind_trust";



  /**
   * The initialization parameter that specifies whether the SLAMD admin
   * interface should operate in read-only mode.
   */
  public static final String SERVLET_INIT_PARAM_READ_ONLY = "read_only";



  /**
   * The initialization parameter that specifies whether the SLAMD admin
   * interface should operate in restricted read-only mode.
   */
  public static final String SERVLET_INIT_PARAM_RESTRICTED_READ_ONLY =
       "restricted_read_only";



  /**
   * The initialization parameter that specifies whether the SLAMD admin
   * interface should allow users to search for jobs when operating in
   * read-only mode.
   */
  public static final String SERVLET_INIT_PARAM_SEARCH_READ_ONLY =
       "search_in_read_only";



  /**
   * The initialization parameter that specifies whether the SLAMD admin
   * interface should hide sensitive job information when operating in
   * read-only mode.
   */
  public static final String SERVLET_INIT_PARAM_HIDE_SENSITIVE_INFO =
       "hide_sensitive_info";



  /**
   * The initialization parameter that specifies whether the SLAMD admin
   * interface should show the link to the status page at the top or bottom of
   * the navigation sidebar.
   */
  public static final String SERVLET_INIT_PARAM_SHOW_STATUS_FIRST =
       "show_status_first";



  /**
   * The initialization parameter that specifies whether the communication with
   * the user directory should use SSL.
   */
  public static final String SERVLET_INIT_PARAM_USER_USE_SSL =
       "user_user_ssl";



  /**
   * The initialization parameter that specifies whether the SLAMD server should
   * blindly trust any SSL certificate presented by the user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_BLIND_TRUST =
       "user_blind_trust";



  /**
   * The initialization parameter that specifies the location of the JSSE key
   * store for use in the SSL environment.
   */
  public static final String SERVLET_INIT_PARAM_SSL_KEY_STORE =
       "ssl_key_store";



  /**
   * The initialization parameter that specifies the password for the JSSE key
   * store.
   */
  public static final String SERVLET_INIT_PARAM_SSL_KEY_PASSWORD =
       "ssl_key_password";




  /**
   * The initialization parameter that specifies the location of the JSSE trust
   * store for use in the SSL environment.
   */
  public static final String SERVLET_INIT_PARAM_SSL_TRUST_STORE =
       "ssl_trust_store";



  /**
   * The initialization parameter that specifies the password for the JSSE key
   * store.
   */
  public static final String SERVLET_INIT_PARAM_SSL_TRUST_PASSWORD =
       "ssl_trust_password";



  /**
   * The name of the request parameter that will retrieve a job by its job ID.
   */
  public static final String SERVLET_PARAM_GET_JOB = "getjob";



  /**
   * The name of the request parameter that will cause the generated HTML to
   * exclude the navigation sidebar.
   */
  public static final String SERVLET_PARAM_HIDE_SIDEBAR = "hide_sidebar";



  /**
   * The name of the request parameter that will cause debug information to
   * be written as comments into the generated HTML.
   */
  public static final String SERVLET_PARAM_HTML_DEBUG = "html_debug";



  /**
   * The name of the request parameter that the administration servlet uses to
   * specify the section to access in the administrative interface.
   */
  public static final String SERVLET_PARAM_SECTION = "sec";



  /**
   * The name of the request parameter that the administration servlet uses to
   * specify the subsection to access in the administrative interface.
   */
  public static final String SERVLET_PARAM_SUBSECTION = "subsec";



  /**
   * The name of the request parameter that the administration servlet uses to
   * specify the configuration subscriber for which to manage the configuration.
   */
  public static final String SERVLET_PARAM_CONFIG_SUBSCRIBER = "subscriber";



  /**
   * The name of the request parameter that the administration servlet uses to
   * specify the configuration parameter whose value is to be modified.
   */
  public static final String SERVLET_PARAM_CONFIG_PARAM_NAME = "pname";



  /**
   * The name of the request parameter that the administration servlet uses to
   * specify the value to use for a configuration parameter.
   */
  public static final String SERVLET_PARAM_CONFIG_PARAM_VALUE = "pval";



  /**
   * The name of the request parameter that the administration servlet uses to
   * determine whether configuration subscribers should be notified of a change
   * to a configuration parameter.
   */
  public static final String SERVLET_PARAM_NOTIFY_SUBSCRIBERS = "notify";



  /**
   * The name of the request parameter that specifies that the operation in
   * progress should be cancelled.
   */
  public static final String SERVLET_PARAM_CANCEL = "cancel";



  /**
   * The name of the administrative section that allows the user to view and/or
   * edit the SLAMD server configuration.
   */
  public static final String SERVLET_SECTION_CONFIG = "config";



  /**
   * The name of the administrative subsection that allows the user to manage
   * the configuration for the SLAMD servlet.
   */
  public static final String SERVLET_SECTION_CONFIG_SERVLET = "servlet";



  /**
   * The name of the administrative subsection that allows the user to manage
   * settings related to access control.
   */
  public static final String SERVLET_SECTION_CONFIG_ACCESS = "acl";



  /**
   * The name of the administrative subsection that allows the user to manage
   * the configuration for a particular section of the server.
   */
  public static final String SERVLET_SECTION_CONFIG_SLAMD = "manage";



  /**
   * The name of the administrative subsection that allows the user to refresh
   * all or some of the SLAMD server configuration.
   */
  public static final String SERVLET_SECTION_CONFIG_REFRESH = "refresh";



  /**
   * The name oif the administrative section that allows the user to access
   * various kinds of debug information about SLAMD and the Java environment.
   */
  public static final String SERVLET_SECTION_DEBUG = "debug";



  /**
   * The name of the administrative subsection that allows the user to perform
   * database debugging.
   */
  public static final String SERVLET_SECTION_DEBUG_DB = "debug_db";



  /**
   * The name of the administrative subsection that allows the user to request
   * that the JVM perform garbage collection.
   */
  public static final String SERVLET_SECTION_DEBUG_GC = "debug_gc";



  /**
   * The name of the administrative subsection that allows the user to request
   * that the JVM enable instruction tracing.
   */
  public static final String SERVLET_SECTION_DEBUG_ENABLE_INSTRUCTION_TRACE =
       "debug_enable_instruction_trace";



  /**
   * The name of the administrative subsection that allows the user to request
   * that the JVM disable instruction tracing.
   */
  public static final String SERVLET_SECTION_DEBUG_DISABLE_INSTRUCTION_TRACE =
       "debug_disable_instruction_trace";



  /**
   * The name of the administrative subsection that allows the user to request
   * that the JVM enable method call tracing.
   */
  public static final String SERVLET_SECTION_DEBUG_ENABLE_METHOD_TRACE =
       "debug_enable_method_trace";



  /**
   * The name of the administrative subsection that allows the user to request
   * that the JVM disable method call tracing.
   */
  public static final String SERVLET_SECTION_DEBUG_DISABLE_METHOD_TRACE =
       "debug_disable_method_trace";



  /**
   * The name of the administrative subsection that allows the user to obtain a
   * stack trace from the server JVM.
   */
  public static final String SERVLET_SECTION_DEBUG_STACK_TRACE =
       "debug_stack_trace";



  /**
   * The name of the administrative subsection that allows the user to access
   * debug information about the system properties defined in the JVM.
   */
  public static final String SERVLET_SECTION_DEBUG_SYSPROPS = "debug_sysprops";



  /**
   * The name of the administrative subsection that allows the user to access
   * debug information about the threads and thread groups defined in the JVM.
   */
  public static final String SERVLET_SECTION_DEBUG_THREADS = "debug_threads";



  /**
   * The name of the administrative subsection that allows the user to access
   * debug information about the request from the client.
   */
  public static final String SERVLET_SECTION_DEBUG_REQUEST = "debug_request";



  /**
   * The name of the administrative subsection that allows the user to access
   * debug information about the SLAMD job cache.
   */
  public static final String SERVLET_SECTION_DEBUG_JOB_CACHE =
       "debug_job_cache";



  /**
   * The name of the request parameter that indicates that the SLAMD server
   * should flush its job cache.
   */
  public static final String SERVLET_PARAM_FLUSH_JOB_CACHE = "flush_job_cache";



  /**
   * The name of the administrative section that allows the user to view the
   * SLAMD documentation.
   */
  public static final String SERVLET_SECTION_DOCUMENTATION = "documentation";



  /**
   * The name of the administrative section that allows the user to view the
   * SLAMD license in HTML form.
   */
  public static final String SERVLET_SECTION_LICENSE_HTML = "license_html";



  /**
   * The name of the resource that can be used to retrieve an HTML-formatted
   * version of the SLAMD license.
   */
  public static final String RESOURCE_LICENSE_HTML = "SLAMD-License.html";



  /**
   * The name of the administrative section that allows the user to view the
   * SLAMD license in plain text.
   */
  public static final String SERVLET_SECTION_LICENSE_TEXT = "license_text";



  /**
   * The name of the resource that can be used to retrieve an plain text version
   * of the SLAMD license.
   */
  public static final String RESOURCE_LICENSE_TEXT = "SLAMD-License.txt";



  /**
   * The name of the administrative section that retrieves the SLAMD logo.
   */
  public static final String SERVLET_SECTION_SLAMD_LOGO = "slamd_logo";



  /**
   * The name of the administrative section that allows the user to view status
   * information about the SLAMD server and its components.
   */
  public static final String SERVLET_SECTION_STATUS = "status";



  /**
   * The name of the administrative subsection that allows the user to get the
   * current state of the SLAMD server in plain text format.
   */
  public static final String SERVLET_SECTION_STATUS_GET_STATUS_AS_TEXT =
       "status_text";



  /**
   * The name of the administrative subsection that allows the user to view the
   * SLAMD log file.
   */
  public static final String SERVLET_SECTION_STATUS_VIEW_LOG = "view_log";



  /**
   * The name of the request parameter that indicates whether all lines of the
   * SLAMD log should be displayed.
   */
  public static final String SERVLET_PARAM_LOG_VIEW_ALL = "view_all";



  /**
   * The name of the request parameter that specifies the number of lines of the
   * SLAMD log that should be displayed.
   */
  public static final String SERVLET_PARAM_LOG_VIEW_LINES = "view_lines";



  /**
   * The default number of log file lines to display if no other value is
   * specified.
   */
  public static final int DEFAULT_LOG_VIEW_LINES = 20;



  /**
   * The HTML comment that will appear on a line by itself immediately before
   * the log output starts.
   */
  public static final String HTML_COMMENT_LOG_START =
       "<!-- START LOG OUTPUT -->";



  /**
   * The HTML comment that will appear on a line by itself immediately after
   * the log output ends.
   */
  public static final String HTML_COMMENT_LOG_END =
       "<!-- END LOG OUTPUT -->";



  /**
   * The name of the administrative subsection that allows the user to start the
   * SLAMD server.
   */
  public static final String SERVLET_SECTION_STATUS_START_SLAMD = "start_slamd";



  /**
   * The name of the administrative subsection that allows the user to stop the
   * SLAMD server.
   */
  public static final String SERVLET_SECTION_STATUS_STOP_SLAMD = "stop_slamd";



  /**
   * The name of the administrative subsection that allows the user to restart
   * the SLAMD server.
   */
  public static final String SERVLET_SECTION_STATUS_RESTART_SLAMD =
       "restart_slamd";



  /**
   * The name of the administrative subsection that allows the user to reload
   * the job class definitions from the configuration directory.
   */
  public static final String SERVLET_SECTION_STATUS_RELOAD_JOBS =
       "reload_job_classes";



  /**
   * The name of the administrative subsection that allows the user to start the
   * access control manager.
   */
  public static final String SERVLET_SECTION_STATUS_START_ACL = "start_acl";



  /**
   * The name of the administrative subsection that allows the user to stop the
   * access control manager.
   */
  public static final String SERVLET_SECTION_STATUS_STOP_ACL = "stop_acl";



  /**
   * The name of the administrative subsection that allows the user to restart
   * the access control manager.
   */
  public static final String SERVLET_SECTION_STATUS_RESTART_ACL = "restart_acl";



  /**
   * The name of the administrative subsection that allows the user to flush the
   * access control manager's user info cache.
   */
  public static final String SERVLET_SECTION_STATUS_FLUSH_ACL_CACHE =
       "flush_acl_cache";



  /**
   * The name of the administrative subsection that allows the user to close a
   * client connection.
   */
  public static final String SERVLET_SECTION_STATUS_DISCONNECT = "disconnect";



  /**
   * The name of the administrative subsection that allows the user to close a
   * resource monitor client connection.
   */
  public static final String SERVLET_SECTION_STATUS_DISCONNECT_MONITOR =
       "disconnect_monitor";



  /**
   * The name of the administrative subsection that allows the user to close all
   * client connections associated with a given client manager.
   */
  public static final String SERVLET_SECTION_STATUS_DISCONNECT_ALL =
       "disconnect_all";



  /**
   * The name of the administrative subsection that allows the user to close all
   * client connections that are currently registered with the SLAMD server.
   */
  public static final String SERVLET_SECTION_STATUS_DISCONNECT_ALL_CLIENTS =
       "disconnect_all_clients";



  /**
   * The name of the administrative subsection that allows the user to request
   * that one or more client connections be established.
   */
  public static final String SERVLET_SECTION_STATUS_CONNECT = "connect";



  /**
   * The name of the administrative subsection that allows the user to request
   * that one or more client connections be established across multiple client
   * managers.
   */
  public static final String SERVLET_SECTION_STATUS_CONNECT_CLIENTS =
       "connect_clients";



  /**
   * The name of the administrative section that allows the user to work with
   * jobs in the SLAMD server, including submitting new jobs for processing,
   * un-scheduling jobs, and working with the job thread classes.
   */
  public static final String SERVLET_SECTION_JOB = "job";



  /**
   * The name of the administrative subsection that is used to edit the
   * description for a real job folder.
   */
  public static final String SERVLET_SECTION_JOB_FOLDER_DESCRIPTION =
       "job_folder_description";



  /**
   * The name of the administrative subsection that is used to edit the
   * description for a real job folder containing optimizing jobs.
   */
  public static final String SERVLET_SECTION_OPTIMIZING_FOLDER_DESCRIPTION =
       "optimizing_folder_description";



  /**
   * The name of the administrative subsection that is used to publish or
   * de-publish a job or folder for display in restricted read-only mode.
   */
  public static final String SERVLET_SECTION_JOB_FOLDER_PUBLISH =
       "job_folder_publish";



  /**
   * The name of the administrative subsection that is used to publish or
   * de-publish an optimizing job or folder for display in restricted read-only
   * mode.
   */
  public static final String SERVLET_SECTION_OPTIMIZING_FOLDER_PUBLISH =
       "optimizing_folder_publish";




  /**
   * The name of the administrative susection that is used to view information
   * about a particular job, regardless of its current state.  This is only
   * valid with the job ID.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_GENERIC = "view_job";



  /**
   * The name of the administrative susection that is used to view information
   * about a particular job with the result in plain text.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_AS_TEXT =
       "view_job_as_text";



  /**
   * The name of the administrative susection that is used to view information
   * about a particular job with the result in plain text.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_OPTIMIZING_AS_TEXT =
       "view_optimizing_job_as_text";



  /**
   * The name of the request parameter that can be used to indicate whether the
   * resulting job information should only return the state of the requested
   * job.
   */
  public static final String SERVLET_PARAM_ONLY_STATE = "only_state";



  /**
   * The name of the administrative subsection that allows the user to see
   * information about the jobs currently scheduled for execution but not yet
   * running.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_PENDING =
       "view_pending";



  /**
   * The name of the administrative subsection that allows the user to see
   * information about the jobs that are currently running.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_RUNNING = "view_running";



  /**
   * The name of the administrative subsection that allows the user to see
   * information about jobs that have completed processing for one reason or
   * another.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_COMPLETED =
       "view_completed";



  /**
   * The name of the administrative subsection that allows the user to view
   * information about the job groups defined in the server.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_GROUPS =
       "view_job_groups";



  /**
   * The name of the administrative subsection that allows the user to view a
   * job group.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_GROUP = "view_job_group";



  /**
   * The name of the administrative subsection that allows the user to edit the
   * name and description of a job group.
   */
  public static final String SERVLET_SECTION_JOB_EDIT_GROUP_DESCRIPTION =
       "edit_group_description";



  /**
   * The name of the administrative subsection that allows the user to edit the
   * parameters in a job group.
   */
  public static final String SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS =
       "edit_group_params";



  /**
   * The name of the administrative subsection that allows the user to edit a
   * job in a job group.
   */
  public static final String SERVLET_SECTION_JOB_EDIT_GROUP_JOB =
       "edit_group_job";



  /**
   * The name of the administrative subsection that allows the user to edit an
   * optimizing job in a job group.
   */
  public static final String SERVLET_SECTION_JOB_EDIT_GROUP_OPTIMIZING_JOB =
       "edit_group_optimizing_job";



  /**
   * The name of the administrative subsection that allows the user to create a
   * new job group.
   */
  public static final String SERVLET_SECTION_JOB_ADD_GROUP = "add_job_group";



  /**
   * The name of the administrative subsection that allows the user to remove a
   * job group.
   */
  public static final String SERVLET_SECTION_JOB_REMOVE_GROUP =
       "remove_job_group";



  /**
   * The name of the administrative subsection that allows a user to add a job
   * to a job group.
   */
  public static final String SERVLET_SECTION_JOB_ADD_JOB_TO_GROUP =
       "add_job_to_group";



  /**
   * The name of the administrative subsection that allows a user to add a job
   * to a job group.
   */
  public static final String SERVLET_SECTION_JOB_ADD_OPTIMIZING_JOB_TO_GROUP =
       "add_optimizing_job_to_group";



  /**
   * The name of the administrative subsection that allows a user to remove a
   * job from a job group.
   */
  public static final String SERVLET_SECTION_JOB_REMOVE_JOB_FROM_GROUP =
       "remove_job_from_group";



  /**
   * The name of the administrative subsection that allows a user to schedule a
   * job group.
   */
  public static final String SERVLET_SECTION_JOB_SCHEDULE_GROUP =
       "schedule_job_group";



  /**
   * The name of the administrative subsection that allows a user to clone a job
   * group.
   */
  public static final String SERVLET_SECTION_JOB_CLONE_GROUP =
       "clone_job_group";



  /**
   * The name of the administrative subsection that allows the user to view
   * information about an optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_OPTIMIZING =
       "view_optimizing";



  /**
   * The name of the administrative subsection that allows the user to view the
   * full set of log messages for a given job.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_LOG_MESSAGES =
       "view_log_messages";



  /**
   * The name of the administrative subsection that allows the user to see
   * information about jobs stored in a virtual job folder.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_VIRTUAL = "view_virtual";



  /**
   * The name of the administrative subsection that allows the user to see
   * information about jobs stored in a real job folder.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_REAL = "view_real";



  /**
   * The name of the administrative subsection that allows the user to list
   * the real job folders defined in the SLAMD server.
   */
  public static final String SERVLET_SECTION_LIST_REAL_FOLDERS =
       "list_real_folders";



  /**
   * The name of the administrative subsection that allows the user to list
   * the virtual job folders defined in the SLAMD server.
   */
  public static final String SERVLET_SECTION_LIST_VIRTUAL_FOLDERS =
       "list_virtual_folders";



  /**
   * The name of the administrative subsection that allows the user to create
   * a new filtered virtual job folder.
   */
  public static final String SERVLET_SECTION_JOB_CREATE_FILTERED_FOLDER =
       "create_filtered";



  /**
   * The name of the administrative subsection that allows the user to search
   * for job information.
   */
  public static final String SERVLET_SECTION_JOB_SEARCH = "search";



  /**
   * The name of the administrative subsection that allows the user to schedule
   * a self-optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_OPTIMIZE = "optimize";



  /**
   * The name of the administrative subsection that displays help information
   * for scheduling an optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_OPTIMIZE_HELP =
       "optimize_help";



  /**
   * The name of the request parameter that indicates the name of the folder in
   * which completed job information is to be viewed.
   */
  public static final String SERVLET_PARAM_JOB_FOLDER = "job_folder";



  /**
   * The name of the request parameter that indicates the name of a virtual job
   * folder that should be used.
   */
  public static final String SERVLET_PARAM_VIRTUAL_JOB_FOLDER =
       "virtual_folder";



  /**
   * The name of the request parameter that indicates the name to use when
   * creating a new job folder.
   */
  public static final String SERVLET_PARAM_NEW_FOLDER_NAME = "new_folder";



  /**
   * The folder name that will be used to indicate that a new virtual job folder
   * should be created.
   */
  public static final String FOLDER_NAME_CREATE_NEW_FOLDER =
       "Create a New Virtual Folder";



  /**
   * The name that will be used for the default job folder.
   */
  public static final String FOLDER_NAME_UNCLASSIFIED = "Unclassified";



  /**
   * The name of the request parameter that indicates the category of jobs with
   * which the user is working.
   */
  public static final String SERVLET_PARAM_VIEW_CATEGORY = "view_category";



  /**
   * The name of the request parameter that can be used to specify the name of
   * the database in which to retrieve debug information.
   */
  public static final String SERVLET_PARAM_DB_NAME = "db_name";



  /**
   * The name of the request parameter that can be used to specify the key to
   * examine in the database.
   */
  public static final String SERVLET_PARAM_DB_KEY = "db_key";



  /**
   * The name of the request parameter that indicates whether the contents of
   * a folder should be removed when the folder is deleted (i.e., whether it
   * should be allowed to remove non-empty folders).
   */
  public static final String SERVLET_PARAM_DELETE_FOLDER_CONTENTS =
       "delete_folder_contents";



  /**
   * The name of the request parameter that indicates whether the job or job
   * folder should be displayed in restricted read-only mode.
   */
  public static final String SERVLET_PARAM_DISPLAY_IN_READ_ONLY =
       "display_in_read_only";



  /**
   * The name of the administrative subsection that allows the user to view
   * detailed statistical informtion about jobs that have completed processing.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_STATS =
       "view_stats";



  /**
   * The name of the administrative subsection that allows the user to save the
   * statistical information stored for a job that has completed processing.
   */
  public static final String SERVLET_SECTION_JOB_SAVE_STATS =
       "save_stats";



  /**
   * The name of the administrative subsection that allows the user to view
   * graphs of the statistical information stored for a job that has completed
   * processing.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_GRAPH = "view_graph";



  /**
   * The name of the administrative subsection that allows the user to view
   * graphs of the resource monitor information stored for a job that has
   * completed processing.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_MONITOR_GRAPH =
       "view_monitor_graph";



  /**
   * The name of the administrative subsection that actually generates graphs of
   * the statistical information associated with a job.
   */
  public static final String SERVLET_SECTION_JOB_GRAPH = "graph";



  /**
   * The name of the administrative subsection that actually generates graphs of
   * the resource monitor statistical information associated with a job.
   */
  public static final String SERVLET_SECTION_JOB_GRAPH_MONITOR =
       "graph_monitor";



  /**
   * The name of the administrative subsection that allows the user to view
   * graphs of statistics collected while the job is running.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_GRAPH_REAL_TIME =
       "view_graph_real_time";



  /**
   * The name of the administrative subsection that actually generates graphs
   * from real-time statistical data.
   */
  public static final String SERVLET_SECTION_JOB_GRAPH_REAL_TIME =
       "graph_real_time";



  /**
   * The name of the administrative subsection that allows the user to view
   * graphs that overlay two statistics for a job.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_OVERLAY = "view_overlay";



  /**
   * The name of the administrative subsection that actually generates graphs of
   * overlayed statistics.
   */
  public static final String SERVLET_SECTION_JOB_OVERLAY = "overlay";



  /**
   * The name of the request parameter that specifies the width that should be
   * used for graphs that are generated.
   */
  public static final String SERVLET_PARAM_GRAPH_WIDTH = "width";



  /**
   * The default width to use for graphs if none is specified.
   */
  public static final int DEFAULT_GRAPH_WIDTH = 800;



  /**
   * The name of the administrative parameter that specifies the default width
   * to use for generated graphs.
   */
  public static final String PARAM_DEFAULT_GRAPH_WIDTH = "default_graph_width";



  /**
   * The name of the request parameter that specifies the height that should be
   * used for graphs that are generated.
   */
  public static final String SERVLET_PARAM_GRAPH_HEIGHT = "height";



  /**
   * The default height to use for graphs if none is specified.
   */
  public static final int DEFAULT_GRAPH_HEIGHT = 600;



  /**
   * The name of the administrative parameter that specifies the default height
   * to use for generated graphs.
   */
  public static final String PARAM_DEFAULT_GRAPH_HEIGHT =
       "default_graph_height";



  /**
   * The name of the request parameter that specifies the height that should be
   * used for resource monitor graphs that are generated.
   */
  public static final String SERVLET_PARAM_MONITOR_GRAPH_HEIGHT =
       "monitor_height";



  /**
   * The name of the request parameter that indicates whether all resource
   * monitor statistics should be graphed.
   */
  public static final String SERVLET_PARAM_MONITOR_GRAPH_ALL =
       "monitor_graph_all";



  /**
   * The default height to use for resource monitor graphs if none is specified.
   */
  public static final int DEFAULT_MONITOR_GRAPH_HEIGHT = 240;



  /**
   * The name of the administrative parameter that specifies the default height
   * to use for resource monitor graphs.
   */
  public static final String PARAM_DEFAULT_MONITOR_GRAPH_HEIGHT =
       "default_monitor_graph_height";



  /**
   * The name of the request parameter that specifies the ID of the a client
   * to target for an operation.
   */
  public static final String SERVLET_PARAM_CLIENT_ID = "client_id";



  /**
   * The name of the request parameter that specifies the search filter to use
   * when querying the configuration directory.
   */
  public static final String SERVLET_PARAM_SEARCH_FILTER = "filter";



  /**
   * The name of the administrative subsection that allows the user to schedule
   * a new job for execution.
   */
  public static final String SERVLET_SECTION_JOB_SCHEDULE =
       "schedule_new_job";



  /**
   * The name of the administrative subsection that allows the user to edit
   * recuring job definitions.
   */
  public static final String SERVLET_SECTION_JOB_RECURRING =
       "recurring_job";



  /**
   * The name for the administrative subsection that allows the user to view
   * additional information that can be helpful while scheduling a new job for
   * execution.
   */
  public static final String SERVLET_SECTION_JOB_SCHEDULE_HELP =
       "schedule_help";



  /**
   * The name of the administrative subsection that allows the user to edit a
   * job that has been scheduled but not yet started.
   */
  public static final String SERVLET_SECTION_JOB_EDIT = "edit_job";



  /**
   * The name of the administrative subsection that allows the user to edit the
   * set of comments associated with a job.
   */
  public static final String SERVLET_SECTION_JOB_EDIT_COMMENTS =
       "edit_job_comments";



  /**
   * The name of the administrative subsection that allows the user to schedule
   * a new job for execution based on parameters taken from an existing job.
   */
  public static final String SERVLET_SECTION_JOB_CLONE = "clone_job";



  /**
   * The name of the administrative subsection that allows the user to clone an
   * optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_CLONE_OPTIMIZING =
       "clone_optimizing";



  /**
   * The name of the administrative subsection that allows the user to clone an
   * optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_EDIT_OPTIMIZING_COMMENTS =
       "edit_optimizing_comments";



  /**
   * The name of the administrative subsection that allows the user to move an
   * optimizing job to a specified folder.
   */
  public static final String SERVLET_SECTION_JOB_MOVE_OPTIMIZING =
       "move_optimizing";



  /**
   * The name of the request parameter that indicates whether a requested
   * operation should be performed on the individual iterations of an optimizing
   * job as well as the optimizing job itself.
   */
  public static final String SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS =
       "include_iterations";



  /**
   * The name of the administrative subsection that allows the user to remove
   * information about a job from the configuration directory.
   */
  public static final String SERVLET_SECTION_JOB_DELETE = "delete_job";



  /**
   * The name of the administrative subsection that allows the user to remove
   * information about a job from the configuration directory.
   */
  public static final String SERVLET_SECTION_JOB_DELETE_OPTIMIZING =
       "delete_optimizing";



  /**
   * The name of the administrative subsection that allows the user to cancel
   * a pending or running job.
   */
  public static final String SERVLET_SECTION_JOB_CANCEL = "cancel_job";



  /**
   * The name of the administrative subsection that allows the user to cancel
   * an optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_CANCEL_OPTIMIZING =
       "cancel_optimizing";



  /**
   * The name of the administrative subsection that allows the user to pause
   * an optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_PAUSE_OPTIMIZING =
       "pause_optimizing";



  /**
   * The name of the administrative subsection that allows the user to unpause
   * an optimizing job.
   */
  public static final String SERVLET_SECTION_JOB_UNPAUSE_OPTIMIZING =
       "unpause_optimizing";



  /**
   * The name of the administrative subsection that allows the user to cancel
   * and delete a pending job..
   */
  public static final String SERVLET_SECTION_JOB_CANCEL_AND_DELETE =
       "cancel_and_delete";



  /**
   * The name of the administrative subsection that allows the user to
   * temporarily disable a pending job.
   */
  public static final String SERVLET_SECTION_JOB_DISABLE = "disable_job";




  /**
   * The name of the administrative subsection that allows the user to
   * re-enable a disabled job.
   */
  public static final String SERVLET_SECTION_JOB_ENABLE = "enable_job";



  /**
   * The name of the administrative subsection that allows the user to generate
   * a report of job results.
   */
  public static final String SERVLET_SECTION_JOB_GENERATE_REPORT =
       "generate_report";



  /**
   * The name of the administrative subsection that allows the user to perform
   * operations on multiple jobs.
   */
  public static final String SERVLET_SECTION_JOB_MASS_OP = "mass_op";



  /**
   * The name of the administrative subsection that allows the user to perform
   * operations on multiple optimizing jobs.
   */
  public static final String SERVLET_SECTION_JOB_MASS_OPTIMIZING =
       "mass_optimizing";



  /**
   * The name of the servlet parameter that will be used for submit buttons on
   * forms that have multiple submit buttons with different functions.
   */
  public static final String SERVLET_PARAM_SUBMIT = "submit";



  /**
   * The text that will appear on the form button used to cancel multiple jobs.
   */
  public static final String SUBMIT_STRING_CANCEL = "Cancel";



  /**
   * The text that will appear on the form button used to cancel and delete
   * multiple jobs.
   */
  public static final String SUBMIT_STRING_CANCEL_AND_DELETE =
       "Cancel and Delete";



  /**
   * The text that will appear on the form button used to clone multiple jobs.
   */
  public static final String SUBMIT_STRING_CLONE = "Clone";



  /**
   * The text that will appear on the form button used to compare multiple jobs.
   */
  public static final String SUBMIT_STRING_COMPARE = "Compare";



  /**
   * The text that will appear on the form button used to indicate that the user
   * wants to create the SLAMD database.
   */
  public static final String SUBMIT_STRING_CREATE_DB = "Create Database";



  /**
   * The text that will appear on the form button used to select all jobs
   * listed.
   */
  public static final String SUBMIT_STRING_SELECT_ALL = "Select All";



  /**
   * The text that will appear on the form button used to deselect all jobs
   * listed.
   */
  public static final String SUBMIT_STRING_DESELECT_ALL = "Deselect All";



  /**
   * The text that will appear on the form button used to export information
   * about multiple jobs.
   */
  public static final String SUBMIT_STRING_EXPORT = "Export";



  /**
   * The text that will appear on the form button used to create a new job
   * folder.
   */
  public static final String SUBMIT_STRING_CREATE_FOLDER = "Create Folder";



  /**
   * The text that will appear on the form button used to create a new virtual
   * job folder.
   */
  public static final String SUBMIT_STRING_CREATE_VIRTUAL_FOLDER =
       "Create Virtual Job Folder";



  /**
   * The text that will appear on the form button used to create a new virtual
   * job folder in which some or all of the jobs contained in it will be matched
   * based on a filter.
   */
  public static final String SUBMIT_STRING_CREATE_FILTERED_VIRTUAL_FOLDER =
       "Create Filtered Virtual Job Folder";



  /**
   * The text that will appear on the form button used to delete a virtual job
   * folder.
   */
  public static final String SUBMIT_STRING_DELETE_VIRTUAL_FOLDER =
       "Delete Virtual Job Folder";



  /**
   * The text that will appear on the form button used to edit the description
   * of a job folder.
   */
  public static final String SUBMIT_STRING_EDIT_DESCRIPTION =
       "Edit Description";



  /**
   * The text that will appear on the form button used to delete a job folder.
   */
  public static final String SUBMIT_STRING_DELETE_FOLDER = "Delete Folder";



  /**
   * The text that will appear on the form button used to move a job to a
   * different folder.
   */
  public static final String SUBMIT_STRING_MOVE = "Move";



  /**
   * The text that will appear on the form button used to add a job to a virtual
   * job folder.
   */
  public static final String SUBMIT_STRING_ADD_TO_VIRTUAL_FOLDER =
       "Add to Virtual Folder";



  /**
   * The text that will appear on the form button used to remove a job from a
   * virtual job folder.
   */
  public static final String SUBMIT_STRING_REMOVE_FROM_VIRTUAL_FOLDER =
       "Remove from Virtual Folder";



  /**
   * The text that will appear on the form button used to request connections
   * across multiple client managers.
   */
  public static final String SUBMIT_STRING_CONNECT = "Connect";



  /**
   * The text that will appear on the form button used to generate a report
   * of job data.
   */
  public static final String SUBMIT_STRING_GENERATE_REPORT = "Generate Report";



  /**
   * The text that will appear on the form button used to gracefully disconnect
   * a client connection.
   */
  public static final String SUBMIT_STRING_GRACEFUL_DISCONNECT =
       "Graceful Disconnect";



  /**
   * The text that will appear on the form button used to forcefully disconnect
   * a client connection.
   */
  public static final String SUBMIT_STRING_FORCEFUL_DISCONNECT =
       "Forceful Disconnect";



  /**
   * The text that will appear on the form button used to publish one or more
   * jobs for display in restricted read-only mode.
   */
  public static final String SUBMIT_STRING_PUBLISH_JOBS = "Publish";



  /**
   * The text that will appear on the form button used to publish a folder
   * for display in restricted read-only mode.
   */
  public static final String SUBMIT_STRING_PUBLISH_FOLDER = "Publish Folder";



  /**
   * The text that will appear on the form button used to publish a folder and
   * the jobs in that folderfor display in restricted read-only mode.
   */
  public static final String SUBMIT_STRING_PUBLISH_FOLDER_JOBS =
       "Publish Folder and Jobs";



  /**
   * The text that will appear on the form button used to de-publish one or more
   * jobs from display in restricted read-only mode.
   */
  public static final String SUBMIT_STRING_DEPUBLISH_JOBS = "De-Publish";



  /**
   * The text that will appear on the form button used to de-publish a folder
   * for display in restricted read-only mode.
   */
  public static final String SUBMIT_STRING_DEPUBLISH_FOLDER =
       "De-Publish Folder";



  /**
   * The text that will appear on the form button used to de-publish a folder
   * and the jobs in that folderfor display in restricted read-only mode.
   */
  public static final String SUBMIT_STRING_DEPUBLISH_FOLDER_JOBS =
       "De-Publish Folder and Jobs";



  /**
   * The text that will appear on the form button used to schedule a job for
   * execution.
   */
  public static final String SUBMIT_STRING_SCHEDULE_JOB = "Schedule Job";



  /**
   * The text that will appear on the form button used to test the job
   * parameters before scheduling it.
   */
  public static final String SUBMIT_STRING_TEST_PARAMS = "Test Job Parameters";



  /**
   * The text that will appear on the form button used to move a job group
   * parameter in the list.
   */
  public static final String SUBMIT_STRING_MOVE_GROUP_PARAM =
       "Move Parameter In List";



  /**
   * The text that will appear on the form button used to insert a spacer
   * between job group parameters.
   */
  public static final String SUBMIT_STRING_INSERT_PARAM_SPACER =
       "Insert Spacer";



  /**
   * The text that will appear on the form button used to remove a spacer
   * between job group parameters.
   */
  public static final String SUBMIT_STRING_REMOVE_PARAM_SPACER =
       "Remove Spacer";



  /**
   * The text that will appear on the form button used to insert a label
   * between job group parameters.
   */
  public static final String SUBMIT_STRING_INSERT_PARAM_LABEL =
       "Insert Label";



  /**
   * The text that will appear on the form button used to remove a label
   * between job group parameters.
   */
  public static final String SUBMIT_STRING_REMOVE_PARAM_LABEL =
       "Remove Label";



  /**
   * The text that will appear on the form button used to edit the text of a
   * label.
   */
  public static final String SUBMIT_STRING_EDIT_LABEL_TEXT =
       "Edit Label Text";



  /**
   * The text that will appear on the form button used to rename a job group
   * parameter.
   */
  public static final String SUBMIT_STRING_RENAME_GROUP_PARAM =
       "Edit Parameter Name";



  /**
   * The text that will appear on the form button used to set a default value
   * for the job group parameter.
   */
  public static final String SUBMIT_STRING_SET_PARAM_DEFAULT =
       "Edit Default Value";



  /**
   * The text that will appear on the form button used to remove a job group
   * parameter.
   */
  public static final String SUBMIT_STRING_REMOVE_GROUP_PARAM =
       "Remove Parameter";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * job ID should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_JOB_ID =
       "export_job_id";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * job description should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_DESCRIPTION =
       "export_description";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * job start time should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_START_TIME =
       "export_start_time";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * job stop time should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_STOP_TIME =
       "export_stop_time";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * job duration should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_DURATION = "export_duration";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * number of clients should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_CLIENTS = "export_clients";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * number of threads per client should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_THREADS = "export_threads";



  /**
   * The name of the servlet parameter that will be used to indicate that the
   * collection interval should be included in exported information.
   */
  public static final String SERVLET_PARAM_EXPORT_INTERVAL = "export_interval";



  /**
   * The name of the servlet parameter that will be used to indicate that all
   * of the parameter information should be included in the exported data.
   */
  public static final String SERVLET_PARAM_EXPORT_PARAMETERS = "export_params";



  /**
   * The prefix that will be applied to job parameter names to determine which
   * parameters should be exported.
   */
  public static final String SERVLET_PARAM_EXPORT_PARAM_PREFIX =
       "export_param_";



  /**
   * The name of the servlet parameter that will be used to indicate that all
   * of the statistical information should be included in the exported data.
   */
  public static final String SERVLET_PARAM_EXPORT_STATISTICS = "export_stats";



  /**
   * The prefix that will be applied to stat tracker names to determine which
   * statistics should be exported.
   */
  public static final String SERVLET_PARAM_EXPORT_STAT_PREFIX = "export_stat_";



  /**
   * The name of the servlet parameter that will be used to indicate what data
   * should be included in a SLAMD data export.
   */
  public static final String SERVLET_PARAM_EXPORT_CHOICE = "export_choice";



  /**
   * The string that indicates that all data in the SLAMD server should be
   * included in the export.
   */
  public static final String EXPORT_CHOICE_ALL =
       "Export all folders and job groups";



  /**
   * The string that indicates that all only data in a set of selected folders
   * should be included in the export.
   */
  public static final String EXPORT_CHOICE_SELECTED =
       "Export only selected folders and job groups";



  /**
   * The name of the servlet parameter that will be used to indicate whether the
   * unclassified jobs should be included in the export.
   */
  public static final String SERVLET_PARAM_EXPORT_UNCLASSIFIED =
       "export_unclassified";



  /**
   * The name of the servlet parameter that will be used to indicate which real
   * job folder(s) should be included in a data export.
   */
  public static final String SERVLET_PARAM_EXPORT_REAL_FOLDER =
       "export_real_folder";



  /**
   * The name of the servlet parameter that will be used to indicate which
   * virtual job folder(s) should be included in a data export.
   */
  public static final String SERVLET_PARAM_EXPORT_VIRTUAL_FOLDER =
       "export_virtual_folder";



  /**
   * The name of the servlet parameter that will be used to indicate which job
   * group(s) should be included in a data export.
   */
  public static final String SERVLET_PARAM_EXPORT_JOB_GROUP =
       "export_job_grop";



  /**
   * The name of the servlet parameter that will be used to specify the location
   * of a file containing data to be imported into the SLAMD server.
   */
  public static final String SERVLET_PARAM_DATA_IMPORT_FILE =
       "data_import_file";



  /**
   * The name of the request parameter that indicates the type of comparison to
   * be performed.
   */
  public static final String SERVLET_PARAM_COMPARE_TYPE = "compare_type";



  /**
   * The value of the compare type that indicates jobs are to be compared in a
   * trend format.
   */
  public static final String COMPARE_TYPE_TREND = "trend";



  /**
   * The value of the compare type that indicates jobs are to be compared in a
   * side-by-side format.
   */
  public static final String COMPARE_TYPE_PARALLEL = "parallel";



  /**
   * The text that will appear on the form button used to delete multiple jobs.
   */
  public static final String SUBMIT_STRING_DELETE = "Delete";



  /**
   * The text that will appear on the form button used to disable multiple jobs.
   */
  public static final String SUBMIT_STRING_DISABLE = "Disable";



  /**
   * The text that will appear on the form button used to enable multiple jobs.
   */
  public static final String SUBMIT_STRING_ENABLE = "Enable";



  /**
   * The name of the administrative subsection that allows the user to work with
   * uploaded files.
   */
  public static final String SERVLET_SECTION_JOB_UPLOAD = "job_upload";



  /**
   * The name of the request parameter that specifies the action that should be
   * taken with an uploaded file.
   */
  public static final String SERVLET_PARAM_FILE_ACTION = "file_action";



  /**
   * The file action that is used to indicate that an uploaded file should be
   * deleted.
   */
  public static final String FILE_ACTION_DELETE = "delete";



  /**
   * The file action that is used to indicate that the user wants to edit a
   * file's MIME type.
   */
  public static final String FILE_ACTION_EDIT_TYPE = "edit_type";



  /**
   * The file action that is used to indicate that an uploaded file should be
   * saved (downloaded with a bogus MIME type).
   */
  public static final String FILE_ACTION_SAVE = "save";



  /**
   * The file action that is used to indicate that an uploaded file should be
   * uploaded.
   */
  public static final String FILE_ACTION_UPLOAD = "upload";



  /**
   * The file action that is used to indicate that an uploaded file should be
   * viewed (downloaded with the correct MIME type).
   */
  public static final String FILE_ACTION_VIEW = "view";



  /**
   * The name of the request parameter that is used to hold the name of an
   * uploaded file.
   */
  public static final String SERVLET_PARAM_FILE_NAME = "file_name";



  /**
   * The name of the request parameter that is used to hold the description of
   * an uploaded file.
   */
  public static final String SERVLET_PARAM_FILE_DESCRIPTION =
       "file_description";



  /**
   * The name of the request parameter that is used to hold the MIME type of the
   * uploaded file.
   */
  public static final String SERVLET_PARAM_FILE_TYPE = "file_type";



  /**
   * The name of the request parameter that is used to hold the actual data
   * associated with a file that has been uploaded.
   */
  public static final String SERVLET_PARAM_UPLOAD_FILE = "upload_file";



  /**
   * The name of the request parameter that is used to hold the path to a file
   * that is to be uploaded into the SLAMD server.
   */
  public static final String SERVLET_PARAM_UPLOAD_FILE_PATH =
       "upload_file_path";



  /**
   * The name of the request parameter that specifies the report generator
   * class to use to generate the report.
   */
  public static final String SERVLET_PARAM_REPORT_GENERATOR =
       "report_generator";



  /**
   * The name of the request parameter that is used to hold the actual data
   * associated with a job pack file that has been uploaded.
   */
  public static final String SERVLET_PARAM_JOB_PACK_FILE = "job_pack_file";



  /**
   * The name of the request parameter that is used to hold the path to a job
   * pack contained on the server's local filesystem.
   */
  public static final String SERVLET_PARAM_JOB_PACK_PATH = "job_pack_path";



  /**
   * The name of the manifest attribute that specifies which jobs should be
   * registered with the SLAMD server when a job pack is installed.
   */
  public static final String JOB_PACK_MANIFEST_REGISTER_JOBS_ATTR =
       "Register-Jobs";



  /**
   * The name of the request parameter that indicates whether the user is
   * performing the operation in the context of the optimizing jobs.
   */
  public static final String SERVLET_PARAM_IN_OPTIMIZING = "in_optimizing";



  /**
   * The name of the administrative subsection that allows the user to work with
   * the job classes defined for use with the SLAMD server.
   */
  public static final String SERVLET_SECTION_JOB_VIEW_CLASSES =
       "view_job_classes";



  /**
   * The name of the administrative subsection that allows the user to add a new
   * job class that can be used to schedule jobs in the SLAMD server.
   */
  public static final String SERVLET_SECTION_JOB_ADD_CLASS = "add_job_class";



  /**
   * The name of the administrative subsection that allows the user to install a
   * job pack.
   */
  public static final String SERVLET_SECTION_JOB_INSTALL_JOB_PACK =
       "install_job_pack";



  /**
   * The name of the administrative subsection that allows the user to delete a
   * job class definition from the SLAMD server.
   */
  public static final String SERVLET_SECTION_JOB_DELETE_CLASS =
       "delete_job_class";



  /**
   * The name of the administrative subsection that allows the user to export
   * job data from the SLAMD server.
   */
  public static final String SERVLET_SECTION_JOB_EXPORT_JOB_DATA =
       "export_job_data";



  /**
   * The name of the administrative subsection that allows the user to import
   * job data into the SLAMD server.
   */
  public static final String SERVLET_SECTION_JOB_IMPORT_JOB_DATA =
       "import_job_data";



  /**
   * The name of the administrative subsection that allows the user to import
   * persistent statistical data into the SLAMD server.
   */
  public static final String SERVLET_SECTION_JOB_IMPORT_PERSISTENT =
       "import_persistent";



  /**
   * The name of the administrative subsection that allows the user to migrate
   * job data from a previous configuration directory to the new database.
   */
  public static final String SERVLET_SECTION_JOB_MIGRATE = "migrate_job_data";



  /**
   * The name of the request parameter that is used to indicate whether the
   * information about the migration directory server has been provided.
   */
  public static final String SERVLET_PARAM_SERVER_INFO_SUBMITTED =
       "server_info_submitted";



  /**
   * The name of the request parameter that is used for the parameter that
   * allows the user to select whether to export all folders or only a selected
   * subset.
   */
  public static final String SERVLET_PARAM_MIGRATE_FOLDERS =
       "migrate_folders";



  /**
   * The value that will be used to indicate that the user wishes to migrate
   * data from all job folders.
   */
  public static final String MIGRATE_FOLDERS_ALL =
       "Migrate Data from All Job Folders";



  /**
   * The value that will be used to indicate that the user wishes to migrate
   * data from only selected job folders.
   */
  public static final String MIGRATE_FOLDERS_SELECTED =
       "Migrate Data Only from Selected Folders";



  /**
   * The name of the request parameter that specifies wheter to show advanced
   * options for the current section.
   */
  public static final String SERVLET_PARAM_SHOW_ADVANCED = "show_advanced";



  /**
   * The name of the request parameter that allows the user to edit the set of
   * comments associated with a job.
   */
  public static final String SERVLET_PARAM_JOB_COMMENTS = "job_comments";



  /**
   * The name of the request parameter that specifies the name of a job group.
   */
  public static final String SERVLET_PARAM_JOB_GROUP_NAME = "job_group_name";



  /**
   * The name of the request parameter that specifies the index of a parameter
   * in a job group.
   */
  public static final String SERVLET_PARAM_INDEX = "param_index";



  /**
   * The name of the request parameter that specifies the new index for a
   * parameter in a job group.
   */
  public static final String SERVLET_PARAM_NEW_INDEX = "param_new_index";



  /**
   * The name of the request parameter that specifies the new name for a
   * parameter in a job group.
   */
  public static final String SERVLET_PARAM_NEW_NAME = "param_new_name";



  /**
   * The name of the request parameter that specifies the text for a label.
   */
  public static final String SERVLET_PARAM_LABEL_TEXT = "label_text";



  /**
   * The name of the request parameter that specifies the name of a job
   * associated with a job group.
   */
  public static final String SERVLET_PARAM_JOB_GROUP_JOB_NAME =
       "job_group_job_name";



  /**
   * The name of the request parameter that specifies the name of an optimizing
   * job associated with a job group.
   */
  public static final String SERVLET_PARAM_JOB_GROUP_OPTIMIZING_JOB_NAME =
       "job_group_optimizing_job_name";



  /**
   * The name of the request parameter that specifies the description of a job
   * group.
   */
  public static final String SERVLET_PARAM_JOB_GROUP_DESCRIPTION =
       "job_group_description";



  /**
   * The prefix that will be used before a job or optimizing job parameter name
   * when deciding how to treat it in a job group.
   */
  public static final String SERVLET_PARAM_GROUP_PARAM_TYPE_PREFIX =
       "group_param_";



  /**
   * The group parameter handling type that indicates that the parameter should
   * be mapped to an existing parameter that is already defined for the job
   * group.
   */
  public static final String GROUP_PARAM_TYPE_MAPPED_TO_EXISTING =
       "mapped_to_existing";



  /**
   * The group parameter handling type that indicates that the parameter should
   * become a new mapped parameter for the job group.
   */
  public static final String GROUP_PARAM_TYPE_MAPPED_TO_NEW =
       "mapped_to_new";



  /**
   * The group parameter handling type that indicates that the parameter should
   * always be assigned a fixed value.
   */
  public static final String GROUP_PARAM_TYPE_FIXED =
       "fixed_value";



  /**
   * The prefix that will be used before a job or optimizing job parameter name
   * to designate the group parameter to which the job parameter should be
   * mapped.
   */
  public static final String SERVLET_PARAM_MAP_TO_NAME_PREFIX =
       "map_to_param_name_";



  /**
   * The prefix that will be used before a job or optimizing job parameter name
   * to designate the display name of the group parameter to which the job
   * parameter should be mapped.
   */
  public static final String SERVLET_PARAM_MAP_TO_DISPLAY_NAME_PREFIX =
       "map_to_param_display_name_";



  /**
   * The name of the request parameter that specifies the job ID.
   */
  public static final String SERVLET_PARAM_JOB_ID = "job_id";



  /**
   * The name of the request parameter that specifies the job ID for an
   * optimizing job.
   */
  public static final String SERVLET_PARAM_OPTIMIZING_JOB_ID =
       "optimizing_job_id";



  /**
   * The name of the request parameter that specifies the optimization algorithm
   * to use for an optimizing job.
   */
  public static final String SERVLET_PARAM_OPTIMIZATION_ALGORITHM =
       "optimization_algorithm";



  /**
   * The name of the request parameter that specifies the job ID of a job on
   * which another job is dependent.
   */
  public static final String SERVLET_PARAM_JOB_DEPENDENCY = "job_dependency";



  /**
   * The name of the request parameter that specifies the addresses of those
   * users to notify when the job is complete.
   */
  public static final String SERVLET_PARAM_JOB_NOTIFY_ADDRESS =
       "notify_address";



  /**
   * The name of the request parameter that specifies whether the job should be
   * disabled when it is scheduled.
   */
  public static final String SERVLET_PARAM_JOB_DISABLED = "job_disabled";



  /**
   * The name of the request parameter that specifies the number of copies of
   * the job to create.
   */
  public static final String SERVLET_PARAM_JOB_NUM_COPIES = "num_copies";



  /**
   * The name of the request parameter that indicates whether multiple copies of
   * a job should be made interdependent.
   */
  public static final String SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT =
       "make_copies_interdependent";



  /**
   * The name of the request parameter that specifies the length of time in
   * seconds between the startup of job copies.
   */
  public static final String SERVLET_PARAM_TIME_BETWEEN_STARTUPS =
       "time_between_startups";



  /**
   * The name of the request parameter that specifies the job description.
   */
  public static final String SERVLET_PARAM_JOB_DESCRIPTION = "job_description";



  /**
   * The name of the request parameter that specifies whether to include the
   * number of threads in the job description.
   */
  public static final String SERVLET_PARAM_JOB_INCLUDE_THREAD_IN_DESCRIPTION =
       "include_thread";



  /**
   * The name of the request parameter that indicates whether the user has
   * confirmed that they want to perform some action.
   */
  public static final String SERVLET_PARAM_CONFIRMED = "confirmed";



  /**
   * The name of the request parameter that specifies the Java class file
   * associated with a job thread.
   */
  public static final String SERVLET_PARAM_JOB_CLASS = "job_class";



  /**
   * The name of the request parameter that specifies the statistics collection
   * interval to use for a job.
   */
  public static final String SERVLET_PARAM_JOB_COLLECTION_INTERVAL =
       "stat_interval";



  /**
   * The name of the request parameter that indicates a job schedule request has
   * been submitted and it should be validated and handled accordingly.
   */
  public static final String SERVLET_PARAM_JOB_VALIDATE_SCHEDULE =
       "validate_schedule";



  /**
   * The name of the request parameter that indicates whether a plain text job
   * listing should include detailed statistical information.
   */
  public static final String SERVLET_PARAM_JOB_VIEW_DETAILED_STATS =
       "view_detailed_stats";



  /**
   * The name of the request parameter that indicates the time that a job should
   * start being processed.
   */
  public static final String SERVLET_PARAM_JOB_START_TIME = "job_start_time";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * a scheduled start time after a given time.
   */
  public static final String SERVLET_PARAM_JOB_SCHEDULED_START_AFTER =
       "scheduled_start_after";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * a scheduled start time before a given time.
   */
  public static final String SERVLET_PARAM_JOB_SCHEDULED_START_BEFORE =
       "scheduled_start_before";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * an actual start time after a given time.
   */
  public static final String SERVLET_PARAM_JOB_ACTUAL_START_AFTER =
       "actual_start_after";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * an actual start time before a given time.
   */
  public static final String SERVLET_PARAM_JOB_ACTUAL_START_BEFORE =
       "actual_start_before";



  /**
   * The name of the request parameter that indicates the time that a job should
   * stop being processed.
   */
  public static final String SERVLET_PARAM_JOB_STOP_TIME = "job_stop_time";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * a scheduled stop time after a given time.
   */
  public static final String SERVLET_PARAM_JOB_SCHEDULED_STOP_AFTER =
       "scheduled_stop_after";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * a scheduled stop time before a given time.
   */
  public static final String SERVLET_PARAM_JOB_SCHEDULED_STOP_BEFORE =
       "scheduled_stop_before";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * an actual stop time after a given time.
   */
  public static final String SERVLET_PARAM_JOB_ACTUAL_STOP_AFTER =
       "actual_stop_after";



  /**
   * The name of the request parameter that is used when searching for jobs with
   * an actual stop time before a given time.
   */
  public static final String SERVLET_PARAM_JOB_ACTUAL_STOP_BEFORE =
       "actual_stop_before";



  /**
   * The name of the request parameter that indicates the maximum length of time
   * that should be spent processing a job.
   */
  public static final String SERVLET_PARAM_JOB_DURATION = "job_duration";



  /**
   * The name of the request parameter that is used when searching for a
   * scheduled duration greater than or equal to a given value.
   */
  public static final String SERVLET_PARAM_SCHEDULED_DURATION_MIN =
       "scheduled_duration_min";



  /**
   * The name of the request parameter that is used when searching for a
   * scheduled duration less than or equal to a given value.
   */
  public static final String SERVLET_PARAM_SCHEDULED_DURATION_MAX =
       "scheduled_duration_at_max";



  /**
   * The name of the request parameter that is used when searching for an
   * actual duration greater than or equal to a given value.
   */
  public static final String SERVLET_PARAM_ACTUAL_DURATION_MIN =
       "actual_duration_min";



  /**
   * The name of the request parameter that is used when searching for an
   * actual duration less than or equal to a given value.
   */
  public static final String SERVLET_PARAM_ACTUAL_DURATION_MAX =
       "actual_duration_max";



  /**
   * The name of the request parameter that specifies the number of clients that
   * should be used to execute a job.
   */
  public static final String SERVLET_PARAM_JOB_NUM_CLIENTS = "job_num_clients";



  /**
   * The name of the request parameter prefix that will be prepended to the ID
   * of the client manager for which to create a number of connections.
   */
  public static final String SERVLET_PARAM_NUM_CLIENTS_PREFIX = "num_clients_";



  /**
   * The name of the request parameter that is used when searching for a number
   * of clients greater than or equal to a given value.
   */
  public static final String SERVLET_PARAM_JOB_NUM_CLIENTS_MIN =
       "job_num_clients_min";



  /**
   * The name of the request parameter that is used when searching for a number
   * of clients less than or equal to a given value.
   */
  public static final String SERVLET_PARAM_JOB_NUM_CLIENTS_MAX =
       "job_num_clients_max";



  /**
   * The name of the request parameter that specifies the set of clients that
   * have been requested for a particular job.
   */
  public static final String SERVLET_PARAM_JOB_CLIENTS = "job_clients";



  /**
   * The name of the request parameter that specifies the set of resource
   * monitor clients that have been requested for a particular job.
   */
  public static final String SERVLET_PARAM_JOB_MONITOR_CLIENTS =
       "job_monitor_clients";



  /**
   * The name of the request parameter that specifies the number of threads that
   * each client should use to execute a job.
   */
  public static final String SERVLET_PARAM_JOB_THREADS_PER_CLIENT =
       "job_threads_per_client";



  /**
   * The name of the request parameter that is used when searching for a number
   * of threads per client greater than or equal to a given value.
   */
  public static final String SERVLET_PARAM_JOB_THREADS_MIN = "job_threads_min";



  /**
   * The name of the request parameter that is used when searching for a number
   * of threads per client less than or equal to a given value.
   */
  public static final String SERVLET_PARAM_JOB_THREADS_MAX = "job_threads_max";



  /**
   * The name of the request parameter that is used to specify the increment to
   * the number of threads that should be used between iterations of an
   * optimizing job.
   */
  public static final String SERVLET_PARAM_THREAD_INCREMENT =
       "thread_increment";



  /**
   * The name of the request parameter that specifies the delay in milliseconds
   * that should be used when creating threads on the client system.
   */
  public static final String SERVLET_PARAM_JOB_THREAD_STARTUP_DELAY =
       "job_thread_startup_delay";



  /**
   * The name of the request parameter that indicates whether the job should
   * wait for the number of clients to be available.
   */
  public static final String SERVLET_PARAM_JOB_WAIT_FOR_CLIENTS =
       "job_wait_for_clients";



  /**
   * The name of the request parameter that indicates whether the job should
   * automatically monitor any client systems that have resource monitor clients
   * available.
   */
  public static final String SERVLET_PARAM_JOB_MONITOR_CLIENTS_IF_AVAILABLE =
       "job_monitor_clients_if_available";



  /**
   * The name of the request parameter that indicates the name of the statistic
   * to be optimized for a self-optimizing job.
   */
  public static final String SERVLET_PARAM_JOB_OPTIMIZE_STATISTIC =
       "optimize_stat";



  /**
   * The name of the request parameter that indicates the type of optimization
   * that should be performed for a self-optimizing job.
   */
  public static final String SERVLET_PARAM_JOB_OPTIMIZE_TYPE = "optimize_type";



  /**
   * The optimization type that indicates that the optimization should try to
   * find the maximum value for the statistic.
   */
  public static final String OPTIMIZE_TYPE_MAXIMIZE = "Maximize";



  /**
   * The optimization type that indicates that the optimization should try to
   * find the minimum value for the statistic.
   */
  public static final String OPTIMIZE_TYPE_MINIMIZE = "Minimize";



  /**
   * The set of optimization types supported by the SLAMD server.
   */
  public static final String[] OPTIMIZE_TYPES =
  {
    OPTIMIZE_TYPE_MAXIMIZE,
    OPTIMIZE_TYPE_MINIMIZE
  };



  /**
   * The name of the request parameter that indicates the maximum number of
   * consecutive non-improving results that will be allowed before ending a
   * self-optimizing test.
   */
  public static final String SERVLET_PARAM_JOB_MAX_NON_IMPROVING =
       "max_non_improving";



  /**
   * The name of the request parameter that indicates whether the best iteration
   * of an optimizing job should be re-run, optionally with a different
   * duration.
   */
  public static final String SERVLET_PARAM_RERUN_BEST_ITERATION = "re_run_best";



  /**
   * The name of the request parameter that specifies the duration to use when
   * re-running the best iteration of an optimizing job.
   */
  public static final String SERVLET_PARAM_RERUN_DURATION = "re_run_duration";



  /**
   * The prefix that will be applied to all job-specific parameters used to
   * customize the way that they operate.
   */
  public static final String SERVLET_PARAM_JOB_PARAM_PREFIX = "param_";



  /**
   * The prefix that will be applied to all parameters used to configure an
   * optimization algorithm.
   */
  public static final String SERVLET_PARAM_OPTIMIZATION_PARAM_PREFIX =
       "optimization_param_";



  /**
   * The prefix that will be applied to the names of parameters that will be
   * used when searching for job information.
   */
  public static final String SERVLET_PARAM_USE_IN_SEARCH_PREFIX = "search_";



  /**
   * The name of the request parameter that specifies whether parameters should
   * be assigned their default values or given values read from the request.
   */
  public static final String SERVLET_PARAM_USE_REQUEST_PARAMS =
       "use_request_params";



  /**
   * The name of the request parameter that specifies which types of statistics
   * to display when viewing detailed statistics.
   */
  public static final String SERVLET_PARAM_STAT_TRACKER = "tracker";



  /**
   * The name of the request parameter that specifies the name of the stat
   * tracker that will be graphed on the left axis of an overlay graph.
   */
  public static final String SERVLET_PARAM_LEFT_TRACKER = "left_tracker";



  /**
   * The name of the request parameter that specifies the name of the stat
   * tracker that will be graphed on the right axis of an overlay graph.
   */
  public static final String SERVLET_PARAM_RIGHT_TRACKER = "right_tracker";



  /**
   * The name of the request parameter that specifies whether the statistics
   * graphed on an overlay graph should be graphed on the same axis.
   */
  public static final String SERVLET_PARAM_GRAPH_USE_SAME_AXIS = "same_axis";



  /**
   * The prefix that will be used when searching for jobs that have collected
   * statistics of at least a specified value.
   */
  public static final String SERVLET_PARAM_STAT_AT_LEAST = "stat_at_least_";



  /**
   * The prefix that will be used when searching for jobs that have collected
   * statistics of at most a specified value.
   */
  public static final String SERVLET_PARAM_STAT_AT_MOST = "stat_at_most_";



  /**
   * The name of the request parameter that can be used to specify whether to
   * search for job-specific parameters.
   */
  public static final String SERVLET_PARAM_SEARCH_JOB_PARAMS =
       "search_job_params";



  /**
   * The name of the request parameter that can be used to specify whether to
   * search for job-specific statistics.
   */
  public static final String SERVLET_PARAM_SEARCH_JOB_STATS =
       "search_job_stats";



  /**
   * The name of the request parameter that specifies how detailed the
   * statistics should be.
   */
  public static final String SERVLET_PARAM_DETAIL_LEVEL = "detail";



  /**
   * The name of the request parameter that specifies whether row and column
   * labels should be included in the statistical information that is being
   * output.
   */
  public static final String SERVLET_PARAM_INCLUDE_LABELS = "labels";



  /**
   * The name of the request parameter that specifies whether the legend of
   * pie graphs should include percentages by category.
   */
  public static final String SERVLET_PARAM_SHOW_PERCENTAGES = "pct";



  /**
   * The name of the request parameter that specifies whether line graphs should
   * include horizontal grids.
   */
  public static final String SERVLET_PARAM_INCLUDE_HORIZ_GRID = "includehgrid";



  /**
   * The name of the request parameter that specifies whether line graphs should
   * include vertical grids.
   */
  public static final String SERVLET_PARAM_INCLUDE_VERT_GRID = "includevgrid";



  /**
   * The name of the request parameter that indicates whether the graph
   * generated from a stacked value tracker should be a line graph or a stacked
   * area graph.
   */
  public static final String SERVLET_PARAM_DRAW_AS_STACKED_GRAPH =
       "draw_as_stacked";



  /**
   * The name of the request parameter that indicates whether to exclude
   * information from the first collection interval so that results are not
   * potentially skewed by initialization that needs to occur.
   */
  public static final String SERVLET_PARAM_EXCLUDE_FIRST_INTERVAL =
       "exclude_first";



  /**
   * The name of the request parameter that indicates whether to exclude
   * information from the last collection interval so that results are not
   * potentially skewed by only a partial interval.
   */
  public static final String SERVLET_PARAM_EXCLUDE_LAST_INTERVAL =
       "exclude_last";



  /**
   * The name of the request parameter that indicates whether the vertical
   * access of the generated graph should have a lower bound of zero rather than
   * a value that is calculated based on the data provided.
   */
  public static final String SERVLET_PARAM_BASE_AT_ZERO = "base_at_zero";



  /**
   * The name of the request parameter that indicates whether the current graph
   * should be drawn as a bar graph rather than a line graph.
   */
  public static final String SERVLET_PARAM_DRAW_AS_BAR_GRAPH =
       "draw_as_bar_graph";



  /**
   * The name of the request parameter that indicates whether the individual
   * data points on a graph should be displayed.
   */
  public static final String SERVLET_PARAM_SHOW_POINTS = "show_points";



  /**
   * The name of the request parameter that indicates whether the current line
   * graph should be flat between data points rather than directly connecting
   * the lines.
   */
  public static final String SERVLET_PARAM_FLAT_BETWEEN_POINTS =
       "flat_between_points";



  /**
   * The name of the request parameter that specifies whether the average value
   * should be included on the graph generated from statistical data.
   */
  public static final String SERVLET_PARAM_INCLUDE_AVERAGE = "average";


  /**
   * The name of the request parameter that specifies whether a regression line
   * should be included on the graph generated from statistical data.
   */
  public static final String SERVLET_PARAM_INCLUDE_REGRESSION =
       "regression";



  /**
   * The length of time in milliseconds that a thread should sleep between
   * iterations while it is blocking on some operation.
   */
  public static final int THREAD_BLOCK_SLEEP_TIME = 5;



  /**
   * The system property used to specify the location of the JSSE key store.
   */
  public static final String SSL_KEY_STORE_PROPERTY =
       "javax.net.ssl.keyStore";



  /**
   * The system property used to specify the password for the JSSE key store.
   */
  public static final String SSL_KEY_PASSWORD_PROPERTY =
       "javax.net.ssl.keyStorePassword";



  /**
   * The system property used to specify the location of the JSSE trust store.
   */
  public static final String SSL_TRUST_STORE_PROPERTY =
       "javax.net.ssl.trustStore";



  /**
   * The system property used to specify the password for the JSSE trust store.
   */
  public static final String SSL_TRUST_PASSWORD_PROPERTY =
       "javax.net.ssl.trustStorePassword";



  /**
   * The location that will be used for the configuration file if it was not
   * specified as an initialization parameter.
   */
  public static final String DEFAULT_CONFIG_FILE_PATH = "/WEB-INF/slamd.conf";



  /**
   * The name of the servlet initialization parameter that specifies the LDAP
   * attribute to use to find user entries in the user directory.
   */
  public static final String SERVLET_INIT_PARAM_USER_ID_ATTR =
       "user_id_attribute";



  /**
   * The name of the servlet initialization parameter that indicates whether
   * access control will be performed in the administrative interface.
   */
  public static final String SERVLET_INIT_PARAM_USE_ACCESS_CONTROL =
       "use_access_control";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has full access to all parts of the SLAMD admin interface.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_FULL =
       "full_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to start/stop/restart the SLAMD server.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_RESTART_SLAMD =
       "start_stop_slamd_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to start/stop/restart the access control
   * manager.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_RESTART_ACL =
       "start_stop_access_manager_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to view the servlet configuration
   * parameters.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_VIEW_SERVLET_CONFIG =
       "view_servlet_config_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to edit the servlet configuration
   * parameters.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_EDIT_SERVLET_CONFIG =
       "edit_servlet_config_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to view the SLAMD server configuration
   * parameters.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_VIEW_SLAMD_CONFIG =
       "view_slamd_config_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to edit the SLAMD server configuration
   * parameters.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_EDIT_SLAMD_CONFIG =
       "edit_slamd_config_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to view the SLAMD server status.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_VIEW_STATUS =
       "view_status_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to disconnect clients through the
   * administrative interface.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_DISCONNECT_CLIENT =
       "disconnect_client_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to view job information.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_VIEW_JOB =
       "view_job_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to export job data.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_EXPORT_JOB =
       "export_job_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to schedule a new job for execution.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_SCHEDULE_JOB =
       "schedule_job_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to cancel jobs that are pending execution
   * or actively running.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_CANCEL_JOB =
       "cancel_job_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to delete job information for completed
   * jobs.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_DELETE_JOB =
       "delete_job_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to create/delete/manage job folders.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_MANAGE_JOB_FOLDERS =
       "manage_job_folders_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to view job class information.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_VIEW_JOB_CLASS =
       "view_job_class_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to add new job classes for use by the SLAMD
   * server.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_ADD_JOB_CLASS =
       "add_job_class_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to delete job class definitions.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_DELETE_JOB_CLASS =
       "delete_job_class_access_dn";



  /**
   * The name of the servlet initialization parameter that indicates the
   * group/role that has the ability to authenticate to the SLAMD server as
   * clients.
   */
  public static final String SERVLET_INIT_PARAM_ACCESS_AUTHENTICATE_CLIENT =
       "authenticate_client_access_dn";



  /**
   * The number of spaces that will be used to indent lines when creating a
   * String representation of a SLAMD script.
   */
  public static final int SCRIPT_INDENT = 2;



  /**
   * The end-of-line character to use when sending e-mail messages.  This
   * character sequence is explicitly defined in the SMTP specification (RFC
   * 821), and some mail servers are very picky about the format.
   */
  public static final String SMTP_EOL = "\r\n";



  /**
   * Converts the provided string to a form that will be more suitable for
   * display in an HTML document.  In particular, it will convert special
   * characters like angle brackets and line breaks to
   *
   * @param  s  The string to be updated.
   *
   * @return  The updated HTML-safe version of the string.
   */
  public static String makeHTMLSafe(String s)
  {
    if (s == null)
    {
      return "";
    }

    int length = s.length();
    StringBuilder buffer = new StringBuilder(length);
    for (int i=0; i < length; i++)
    {
      char c = s.charAt(i);

      if (c == '\r')
      {
        if ((i < (length-1)) && (s.charAt(i+1) == '\n'))
        {
          i++;
        }

        buffer.append("<BR>\r\n");
      }
      else if (c == '\n')
      {
        buffer.append("<BR>\n");
      }
      else if (c == ' ')
      {
        buffer.append("&nbsp;");
      }
      else if (c == '<')
      {
        buffer.append("&lt;");
      }
      else if (c == '>')
      {
        buffer.append("&gt;");
      }
      else if (c == '&')
      {
        buffer.append("&amp;");
      }
      else if (c == '"')
      {
        buffer.append("&quot;");
      }
      else if ((c < 32) || (c > 126))
      {
        buffer.append("<!-- " + c + " -->");
      }
      else
      {
        buffer.append(c);
      }
    }

    return buffer.toString();
  }



  /**
   * Attempts to load the class with the specified name.  It will automatically
   * replace any reference to "com.sun.slamd" with "com.slamd" in an attempt to
   * account for references to class names used in older versions of SLAMD.
   *
   * @param  className  The name of the class to load.
   *
   * @return  The class with the specified name.
   *
   * @throws  ClassNotFoundException  If the specified class is not found in the
   *                                  server classpath.
   */
  public static Class<?> classForName(final String className)
         throws ClassNotFoundException
  {
    try
    {
      return Class.forName(className);
    }
    catch (ClassNotFoundException cnfe)
    {
      return Class.forName(className.replace("com.sun.slamd", "com.slamd"));
    }
  }
}

