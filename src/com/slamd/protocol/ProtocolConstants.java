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
package com.slamd.protocol;



/**
 * This class defines a set of constants used by SLAMD protocol elements.
 *
 *
 * @author   Neil A. Wilson
 */
public class ProtocolConstants
{
  /**
   * The name of the property that holds the actual duration for a job.
   */
  public static final String PROPERTY_ACTUAL_DURATION = "actual_duration";



  /**
   * The name of the property that holds the actual start time for a job.
   */
  public static final String PROPERTY_ACTUAL_START_TIME = "actual_start_time";



  /**
   * The name of the property that holds the actual stop time for a job.
   */
  public static final String PROPERTY_ACTUAL_STOP_TIME = "actual_stop_time";



  /**
   * The name of the property that holds the authentication credentials.
   */
  public static final String PROPERTY_AUTH_CREDENTIALS = "auth_credentials";



  /**
   * The name of the property that holds the authentication ID.
   */
  public static final String PROPERTY_AUTH_ID = "auth_id";



  /**
   * The name of the property that holds the authentication method.
   */
  public static final String PROPERTY_AUTH_METHOD = "auth_method";



  /**
   * The name of the property that holds the actual bytes that comprise a class
   * file.
   */
  public static final String PROPERTY_CLASS_BYTES = "class_bytes";



  /**
   * The name of the property that holds the class data structures for a class
   * transfer response.
   */
  public static final String PROPERTY_CLASS_DATA = "class_data";



  /**
   * The name of the property that holds a fully-qualified class name.
   */
  public static final String PROPERTY_CLASS_NAME = "class_name";



  /**
   * The name of the property that holds the client ID.
   */
  public static final String PROPERTY_CLIENT_ID = "client_id";



  /**
   * The name of the property that holds the IP address of the client.
   */
  public static final String PROPERTY_CLIENT_IP = "client_ip";



  /**
   * The name of the property that holds the client manager ID.
   */
  public static final String PROPERTY_CLIENT_MANAGER_ID = "client_manager_id";



  /**
   * The name of the property that holds the client number for a job request.
   */
  public static final String PROPERTY_CLIENT_NUMBER = "client_number";



  /**
   * The name of the property that holds the port number of the client.
   */
  public static final String PROPERTY_CLIENT_PORT = "client_port";



  /**
   * The name of the property that indicates whether the client should close the
   * connection to the server.
   */
  public static final String PROPERTY_CLIENT_SHOULD_CLOSE =
       "client_should_close";



  /**
   * The name of the property that holds the statistics collection interval for
   * a job.
   */
  public static final String PROPERTY_COLLECTION_INTERVAL =
       "collection_interval";



  /**
   * The name of the property that indicates whether the disconnect is transient
   * or permanent.
   */
  public static final String PROPERTY_DISCONNECT_IS_TRANSIENT = "is_transient";



  /**
   * The name of the property that holds the reason that the connection is
   * being closed.
   */
  public static final String PROPERTY_DISCONNECT_REASON = "disconnect_reason";



  /**
   * The name of the property that holds the display name for an element.
   */
  public static final String PROPERTY_DISPLAY_NAME = "display_name";



  /**
   * The name of the property that holds the duration for a job.
   */
  public static final String PROPERTY_DURATION = "duration";



  /**
   * The name of the property that holds the data for a job file.
   */
  public static final String PROPERTY_FILE_DATA = "file_data";



  /**
   * The name of the property that holds the description for a job file.
   */
  public static final String PROPERTY_FILE_DESCRIPTION = "file_description";



  /**
   * The name of the property that holds the name for a job file.
   */
  public static final String PROPERTY_FILE_NAME = "file_name";



  /**
   * The name of the property that holds the MIME type for a job file.
   */
  public static final String PROPERTY_FILE_TYPE = "file_type";



  /**
   * The name of the property that holds the in-progress data reported by a
   * client.
   */
  public static final String PROPERTY_IN_PROGRESS_DATA = "in_progress_data";



  /**
   * The name of the property that holds the in-progress report interval for a
   * job.
   */
  public static final String PROPERTY_IN_PROGRESS_REPORT_INTERVAL =
       "in_progress_report_interval";



  /**
   * The name of the property that indicates whether the server should include
   * any dependencies when sending a class to the client.
   */
  public static final String PROPERTY_INCLUDE_DEPENDENCIES =
       "include_dependencies";



  /**
   * The name of the property that holds the fully-qualified name of the job
   * class for a job.
   */
  public static final String PROPERTY_JOB_CLASS_NAME = "job_class_name";



  /**
   * The name of the property that holds the job class version for a job.
   */
  public static final String PROPERTY_JOB_CLASS_VERSION = "job_class_version";



  /**
   * The name of the property that holds the client state code.
   */
  public static final String PROPERTY_CLIENT_STATE = "client_state";



  /**
   * The name of the property that holds the client state message.
   */
  public static final String PROPERTY_CLIENT_STATE_MESSAGE =
       "client_state_message";



  /**
   * The name of the property that holds the operation for a job control
   * request.
   */
  public static final String PROPERTY_JOB_CONTROL_OPERATION =
       "job_control_operation";



  /**
   * The name of the property that holds the statistical data collected by a
   * job.
   */
  public static final String PROPERTY_JOB_STATISTICS = "job_stats";



  /**
   * The name of the property that holds the job ID for a job.
   */
  public static final String PROPERTY_JOB_ID = "job_id";



  /**
   * The name of the property that holds the job state code.
   */
  public static final String PROPERTY_JOB_STATE = "job_state";



  /**
   * The name of the property that holds the log messages for a job.
   */
  public static final String PROPERTY_LOG_MESSAGES = "log_messages";



  /**
   * The name of the property that holds the major version number.
   */
  public static final String PROPERTY_MAJOR_VERSION = "major_version";



  /**
   * The name of the property that holds the maximum number of concurrent
   * clients that may be created by a client manager.
   */
  public static final String PROPERTY_MAX_CLIENTS = "max_clients";



  /**
   * The name of the property that holds the minor version number.
   */
  public static final String PROPERTY_MINOR_VERSION = "minor_version";



  /**
   * The name of the property that holds the set of resource monitor classes
   * that will be used by a monitor client.
   */
  public static final String PROPERTY_MONITOR_CLASSES = "monitor_classes";



  /**
   * The name of the property that holds the set of resource monitor statistics
   * collected by a job.
   */
  public static final String PROPERTY_MONITOR_STATISTICS = "monitor_stats";



  /**
   * The name of the property that holds the number of clients to use to
   * process a job.
   */
  public static final String PROPERTY_NUM_CLIENTS = "num_clients";



  /**
   * The name of the property that holds the parameter list for a job.
   */
  public static final String PROPERTY_PARAMETER_LIST = "parameter_list";



  /**
   * The name of the property that holds the point version number.
   */
  public static final String PROPERTY_POINT_VERSION = "point_version";



  /**
   * The name of the property that indicates whether a client should report its
   * in-progress results to the server.
   */
  public static final String PROPERTY_REPORT_IN_PROGRESS_STATS =
       "report_in_progress_stats";



  /**
   * The name of the property that indicates whether a client requires the
   * server to authenticate itself.
   */
  public static final String PROPERTY_REQUIRE_SERVER_AUTH =
       "require_server_auth";



  /**
   * The name of the property that specifies the names of the classes requested
   * by the client.
   */
  public static final String PROPERTY_REQUESTED_CLASS_NAMES =
       "requested_class_names";



  /**
   * The name of the property that holds the result code for a SLAMD message.
   */
  public static final String PROPERTY_RESULT_CODE = "result_code";



  /**
   * The name of the property that holds result message for a SLAMD message.
   */
  public static final String PROPERTY_RESULT_MESSAGE = "result_message";



  /**
   * The name of the property that indicates whether a client is running in
   * restricted mode.
   */
  public static final String PROPERTY_RESTRICTED_MODE = "restricted_mode";



  /**
   * The name of the property that holds the start time for a job.
   */
  public static final String PROPERTY_START_TIME = "start_time";



  /**
   * The name of the property that holds the stop time for a job.
   */
  public static final String PROPERTY_STOP_TIME = "stop_time";



  /**
   * The name of the property that holds the thread ID.
   */
  public static final String PROPERTY_THREAD_ID = "thread_id";



  /**
   * The name of the property that holds the number of threads per client to use
   * to process a job.
   */
  public static final String PROPERTY_THREADS_PER_CLIENT = "threads_per_client";



  /**
   * The name of the property that holds the thread startup delay for a job.
   */
  public static final String PROPERTY_THREAD_STARTUP_DELAY =
       "thread_startup_delay";



  /**
   * The name of the property that contains the data for the requested upgrade
   * file.
   */
  public static final String PROPERTY_UPGRADE_FILE_DATA = "upgrade_file_data";



  /**
   * The name of the property that contains the name of the requested upgrade
   * file.
   */
  public static final String PROPERTY_UPGRADE_FILE_NAME = "upgrade_file_name";
}

