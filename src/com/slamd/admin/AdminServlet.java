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
package com.slamd.admin;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import netscape.ldap.LDAPException;

import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;

import com.sleepycat.je.DatabaseException;

import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;
import com.slamd.common.SLAMDException;
import com.slamd.db.DSMigrator;
import com.slamd.db.JobFolder;
import com.slamd.db.SLAMDDB;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.job.OptimizingJob;
import com.slamd.job.UnknownJobClass;
import com.slamd.jobgroup.JobGroup;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.report.ReportGenerator;
import com.slamd.server.SLAMDServer;
import com.slamd.server.SLAMDServerException;
import com.slamd.server.Scheduler;
import com.slamd.server.UploadedFile;
import com.slamd.stat.StatTracker;

import com.unboundid.util.Base64;

import static com.slamd.admin.AdminAccess.*;
import static com.slamd.admin.AdminConfig.*;
import static com.slamd.admin.AdminDebug.*;
import static com.slamd.admin.AdminJob.*;
import static com.slamd.admin.AdminJobGroup.*;
import static com.slamd.admin.AdminUI.*;



/**
 * This class serves as the single point of entry for the Web-based SLAMD
 * administration interface.  It provides a user-friendly mechanism for the end
 * user to access the SLAMD server.
 *
 *
 * @author   Neil A. Wilson
 */
public class AdminServlet
       extends HttpServlet
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 5218965023006249116L;



  /**
   * The name that will be used to identify the admin interface as a
   * configuration subscriber.
   */
  public static final String CONFIG_SUBSCRIBER_NAME = "User Interface";



  /**
   * The end of line character that will be used for the generated HTML content.
   */
  public static final String EOL = "\n";



  /**
   * The reference to this admin servlet.
   */
  public static AdminServlet adminServlet = null;



  /**
   * The reference to the SLAMD server with which this admin servlet is
   * associated, if there is one.
   */
  public static SLAMDServer slamdServer = null;



  /**
   * The access control manager for the administrative interface.
   */
  static AccessManager accessManager;



  /**
   * Indicates whether to always show advanced scheduling options when
   * scheduling a job.
   */
  static boolean alwaysShowAdvancedOptions;



  /**
   * Indicates whether the configuration database has been created and exists on
   * the disk.
   */
  static boolean configDBExists;



  /**
   * Indicates whether the graphing capability of SLAMD should be disabled.
   */
  static boolean disableGraphs;



  /**
   * Indicates whether the file upload capability of SLAMD should be disabled.
   */
  static boolean disableUploads;



  /**
   * Indicates whether to enable management of options that are only applicable
   * if the server will be run in restricted read-only mode.
   */
  static boolean enableReadOnlyManagement;



  /**
   * Indicates whether graphs should be generated in a new window.
   */
  static boolean graphInNewWindow;



  /**
   * Indicates whether the individual iterations of an optimizing job should be
   * hidden when viewing a list of completed jobs.
   */
  static boolean hideOptimizingIterations;



  /**
   * Indicates whether the SLAMD server should attempt to hide sensitive
   * information when displaying a job in read-only mode.  This sensitive
   * information can include the addresses of client systems, e-mail addresses
   * for job notification, and the values of any parameters that are marked
   * sensitive in the associated job.
   */
  static boolean hideSensitiveInformation;



  /**
   * Indicates whether to include the address of the SLAMD server in the title
   * of the generated HTML pages.
   */
  static boolean includeAddressInPageTitle;



  /**
   * Indicates whether the admin interface should automatically place the
   * current time in the start time text field when scheduling a new job.
   */
  static boolean populateStartTime;



  /**
   * Indicates whether the admin interface should operate in read-only mode.  In
   * read-only mode, users will only be able to view job information -- they
   * will not be allowed to schedule jobs or alter the configuration.
   */
  static boolean readOnlyMode;



  /**
   * Indicates whether the admin interface should restrict the set of jobs and
   * folders that are displayed when the server is operating in read-only mode.
   */
  static boolean restrictedReadOnlyMode;



  /**
   * Indicates whether the admin interface should allow users to search for
   * jobs when operating in read-only mode.
   */
  static boolean searchReadOnly;



  /**
   * Indicates whether the login ID of the currently-authenticated user will be
   * displayed in the navigation bar.
   */
  static boolean showLoginID;



  /**
   * Indicates whether the SLAMD Server Status link should be shown first or
   * last in the navigation sidebar.  By default it will be shown last.
   */
  static boolean showStatusFirstInSidebar;



  /**
   * Indicates whether the current time should be displayed in the sidebar.
   */
  static boolean showTimeInSidebar;



  /**
   * Indicates whether the SLAMD server is currently running.
   */
  static boolean slamdRunning;



  /**
   * Indicates whether access control should be used when determining whether to
   * allow a given operation.
   */
  static boolean useAccessControl;



  /**
   * Indicates whether the SLAMD server should blindly trust any SSL certificate
   * presented by the user directory.
   */
  static boolean userDirBlindTrust;



  /**
   * Indicates whether the connection to the user directory should be encrypted
   * using SSL.
   */
  static boolean userDirUseSSL;



  /**
   * The byte array containing the encoded SLAMD logo.
   */
  static byte[] slamdLogoBytes;



  /**
   * The configuration database for the SLAMD server.
   */
  static SLAMDDB configDB;



  /**
   * The decimal formatter used by the admin servlet.
   */
  static DecimalFormat decimalFormat;



  /**
   * The default width to use for generated graphs.
   */
  static int defaultGraphWidth;



  /**
   * The default height to use for generated graphs.
   */
  static int defaultGraphHeight;



  /**
   * The default height to use for resource monitor graphs.
   */
  static int defaultMonitorGraphHeight;



  /**
   * The port number to use to connect to the user directory.
   */
  static int userDirPort;



  /**
   * The maximum file size that will be allowed via upload.  A value of -1
   * indicates no limit.
   */
  static int maxUploadSize;



  /**
   * The request ID that will be used for the next request.
   */
  static int nextID;



  /**
   * The set of properties associated with the servlet configuration file.
   */
  static Properties configProperties;



  /**
   * The set of report generators that have been configured in the SLAMD server.
   */
  static ReportGenerator[] reportGenerators;



  /**
   * The SLAMD server scheduler.
   */
  static Scheduler scheduler;



  /**
   * The date formatter that will be used to format dates in a form that will be
   * stored in the directory server and also when users enter date information
   * via a form.
   */
  static SimpleDateFormat dateFormat;



  /**
   * The date formatter that will be used to format dates that will be displayed
   * to the end user.
   */
  static SimpleDateFormat displayDateFormat;



  /**
   * The lines that should be added to the HTML header for generated pages.
   */
  static String addedHeaderLines = null;



  /**
   * The HTML target that will be used to determine whether to create a new
   * window for the specified content.
   */
  static String blankTarget;



  /**
   * The location on the filesystem under which the job classes may be found.
   */
  static String classPath;



  /**
   * The location of the configuration database files.
   */
  static String configDBDirectory;



  /**
   * The servlet configuration file that may be an alternate source for init
   * parameters.
   */
  static String configFile;



  /**
   * The HTML that should be displayed on the main page displayed when a user
   * initially accesses the SLAMD administrative interface.
   */
  static String defaultHTML = null;



  /**
   * The HTML that should be displayed at the bottom of every page generated.
   */
  static String pageFooter = null;



  /**
   * The HTML that should be displayed at the top of every page generated.
   */
  static String pageHeader = null;



  /**
   * The DN of the group/role that specifies who can add job classes.
   */
  static String resourceDNAddJobClass;



  /**
   * The DN of the group/role that specifies which users can be used by clients
   * to authenticate.
   */
  static String resourceDNAuthenticateClient;



  /**
   * The DN of the group/role that specifies who can cancel jobs.
   */
  static String resourceDNCancelJob;



  /**
   * The DN of the group/role that specifies who can delete jobs.
   */
  static String resourceDNDeleteJob;



  /**
   * The DN of the group/role that specifies who can delete job classes.
   */
  static String resourceDNDeleteJobClass;



  /**
   * The DN of the group/role that specifies who can disconnect clients.
   */
  static String resourceDNDisconnectClient;



  /**
   * The DN of the group/role that specifies who can edit the servlet config.
   */
  static String resourceDNEditServletConfig;



  /**
   * The DN of the group/role that specifies who can edit the SLAMD config.
   */
  static String resourceDNEditSLAMDConfig;



  /**
   * The DN of the group/role that specifies who can export job information.
   */
  static String resourceDNExportJob;



  /**
   * The DN of the group/role that specifies who has full access to anything.
   */
  static String resourceDNFullAccess;



  /**
   * The DN of the group/role that specifies who can manage real and virtual
   * job folders.
   */
  static String resourceDNManageJobFolders;



  /**
   * The DN of the group/role that specifies who can start and stop the SLAMD
   * server.
   */
  static String resourceDNRestartSLAMD;



  /**
   * The DN of the group/role that specifies who can start and stop the access
   * control manager.
   */
  static String resourceDNRestartACL;



  /**
   * The DN of the group/role that specifies who can view job information.
   */
  static String resourceDNViewJob;



  /**
   * The DN of the group/role that specifies who can view job class definitions.
   */
  static String resourceDNViewJobClass;



  /**
   * The DN of the group/role that specifies who can schedule jobs.
   */
  static String resourceDNScheduleJob;



  /**
   * The DN of the group/role that specifies who can view the servlet config.
   */
  static String resourceDNViewServletConfig;



  /**
   * The DN of the group/role that specifies who can view the SLAMD config.
   */
  static String resourceDNViewSLAMDConfig;



  /**
   * The DN of the group/role that specifies who can view SLAMD status info.
   */
  static String resourceDNViewStatus;



  /**
   * The style sheet that should be used for generated HTML pages.
   */
  static String styleSheet = null;



  /**
   * The location of the JSSE key store to use for establishing SSL-based
   * connections.
   */
  static String sslKeyStore;



  /**
   * The password for the JSSE key store.
   */
  static String sslKeyStorePassword;



  /**
   * The location of the JSSE trust store to use for establishing SSL-based
   * connections.
   */
  static String sslTrustStore;



  /**
   * The password for the JSSE trust store.
   */
  static String sslTrustStorePassword;



  /**
   * The reason that the administrative interface is unavailable.
   */
  static String unavailableReason;



  /**
   * The base DN under which the user accounts are located in the user
   * directory.
   */
  static String userDirBase;



  /**
   * The DN to use to bind to the user directory server.
   */
  static String userDirBindDN;



  /**
   * The password for the user directory bind DN.
   */
  static String userDirBindPW;



  /**
   * The host name or IP address for the user directory server.
   */
  static String userDirHost;



  /**
   * The name of the LDAP attribute that will be used to find user entries in
   * the user directory from the login ID that has been provided.
   */
  static String userIDAttribute;



  /**
   * The absolute path to the WEB-INF directory for the admin interface.
   */
  static String webInfBasePath;



  /**
   * Perform the one-time initialization for the servlet that is done when the
   * servlet is first loaded.
   */
  @Override()
  public void init()
  {
    adminServlet = this;

    // Initialize the static variables that won't get caught elsewhere
    slamdRunning       = false;
    nextID             = 0;
    dateFormat         = new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);
    displayDateFormat  = new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT);
    unavailableReason  = "The servlet initialization did not complete " +
                         "properly.";
    defaultGraphWidth  = Constants.DEFAULT_GRAPH_WIDTH;
    defaultGraphHeight = Constants.DEFAULT_GRAPH_HEIGHT;
    defaultMonitorGraphHeight = Constants.DEFAULT_MONITOR_GRAPH_HEIGHT;
    decimalFormat      = new DecimalFormat("0.000");
    webInfBasePath     =  getServletContext().getRealPath(
                               Constants.DEFAULT_WEB_APP_PATH);
    showStatusFirstInSidebar = false;


    // Read the SLAMD logo into the corresponding byte array.
    slamdLogoBytes = new byte[0];
    try
    {
      InputStream inputStream = getClass().getClassLoader().
                                     getResourceAsStream("slamd_logo.gif");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);

      int bytesRead;
      byte[] buffer = new byte[8192];
      while ((bytesRead = inputStream.read(buffer)) > 0)
      {
        outputStream.write(buffer, 0, bytesRead);
      }

      slamdLogoBytes = outputStream.toByteArray();
    } catch (Exception e) {}


    // First, read the servlet configuration
    try
    {
      if (! readServletConfig(getServletConfig()))
      {
        // We don't need to set the unavailable reason here because
        // readServletConfig() will do that for us.
        return;
      }
    }
    catch (IOException ioe)
    {
      if ((configFile != null) && (configFile.length() > 0))
      {
        unavailableReason = "Unable to read the servlet configuration file " +
                            configFile + ":  " + ioe;
      }
      else
      {
        unavailableReason = "Unable to obtain the servlet configuration.";
      }

      return;
    }


    // Configure the access control manager if access control checking will be
    // used
    if ((! readOnlyMode) && useAccessControl)
    {
      accessManager = new AccessManager(userDirHost, userDirPort, userDirBindDN,
                                        userDirBindPW, userDirBase,
                                        userIDAttribute, userDirUseSSL,
                                        userDirBlindTrust, sslKeyStore,
                                        sslKeyStorePassword, sslTrustStore,
                                        sslTrustStorePassword);


      // Register all of the protected resources
      RequestInfo requestInfo = new RequestInfo(null, null);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_FULL,
                  resourceDNFullAccess, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_RESTART_SLAMD,
                  resourceDNRestartSLAMD, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_RESTART_ACL,
                  resourceDNRestartACL, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_SERVLET_CONFIG,
                  resourceDNViewServletConfig, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_EDIT_SERVLET_CONFIG,
                  resourceDNEditServletConfig, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_SLAMD_CONFIG,
                  resourceDNViewSLAMDConfig, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_EDIT_SLAMD_CONFIG,
                  resourceDNEditSLAMDConfig, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_STATUS,
                  resourceDNViewStatus, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_DISCONNECT_CLIENT,
                  resourceDNDisconnectClient, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_JOB,
                  resourceDNViewJob, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_EXPORT_JOB,
                  resourceDNExportJob, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_SCHEDULE_JOB,
                  resourceDNScheduleJob, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_CANCEL_JOB,
                  resourceDNCancelJob, false);
      registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_DELETE_JOB,
                  resourceDNDeleteJob, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_MANAGE_JOB_FOLDERS,
                  resourceDNManageJobFolders, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_JOB_CLASS,
                  resourceDNViewJobClass, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_ADD_JOB_CLASS,
                  resourceDNAddJobClass, false);
      registerACL(requestInfo,
                  Constants.SERVLET_INIT_PARAM_ACCESS_DELETE_JOB_CLASS,
                  resourceDNDeleteJobClass, false);


      // Start the access control manager.
      try
      {
        accessManager.startAccessManager();
      }
      catch (LDAPException le)
      {
        unavailableReason = "The access control manager was not able to be "
                          + "started because the connection to the "
                          + "configuration directory could not be "
                          + "established:  " + le.getMessage();
        return;
      }
    }


    // Determine whether the configuration database has been created yet.  If
    // not, then set a flag so that the admin interface will prompt the user to
    // create it.
    configDBExists = false;
    try
    {
      configDBExists = SLAMDDB.dbExists(configDBDirectory);
      if (! configDBExists)
      {
        slamdServer       = null;
        scheduler         = null;
        configDB          = null;
        unavailableReason = "The configuration database has not yet been " +
                            "created.";
        return;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      slamdServer       = null;
      scheduler         = null;
      configDB          = null;
      unavailableReason = "Could not determine if the configuration " +
                          "database already exists:  " + e;
      return;
    }


    // Create the SLAMD server instance.
    try
    {
      slamdServer = new SLAMDServer(this, readOnlyMode, configDBDirectory,
                                    sslKeyStore, sslKeyStorePassword,
                                    sslTrustStore, sslTrustStorePassword);

      scheduler = slamdServer.getScheduler();
      configDB  = slamdServer.getConfigDB();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      slamdServer       = null;
      scheduler         = null;
      configDB          = null;
      unavailableReason = "Could not create the SLAMD server instance:  " +
                          e.getMessage();
      return;
    }


    // Register as a configuration subscriber
    configDB.registerAsSubscriber(ADMIN_CONFIG);
    ADMIN_CONFIG.refreshSubscriberConfiguration();


    // If we have made it to this point, then the server has started up and
    // should be ready to handle requests and for administrative interaction.
    slamdRunning = true;
  }



  /**
   * Indicates that the servlet engine is shutting down and that the appropriate
   * action should be taken to ensure that SLAMD is properly stopped.
   */
  @Override()
  public void destroy()
  {
    if (slamdServer != null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                             "SLAMD servlet engine is shutting down.");

      if (slamdRunning)
      {
        slamdServer.stopSLAMD();
        slamdRunning = false;
      }
    }
  }



  /**
   * Receives an HTTP GET request from a client, interprets the request, and
   * generates the appropriate response.
   *
   * @param  request   Information about the HTTP GET request issued by the
   *                   client.
   * @param  response  Information about the HTTP response that will be
   *                   returned to the client.
   *
   * @throws  IOException  If a problem is encountered while reading from or
   *                       writing to the client.
   */
  @Override()
  public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException
  {
    // We don't need to distinguish between requests made via GET and those
    // made via POST, so just treat a GET the same as a POST.
    doPost(request, response);
  }



  /**
   * Receives an HTTP POST request from a client, interprets the request, and
   * generates the appropriate response.
   *
   * @param  request   Information about the HTTP POST request issued by the
   *                   client.
   * @param  response  Information about the HTTP response that will be
   *                   returned to the client.
   *
   * @throws  IOException  If a problem is encountered while reading from or
   *                       writing to the client.
   */
  @Override()
  public void doPost(HttpServletRequest request, HttpServletResponse response)
         throws IOException
  {
    // First, create the request info variable.
    RequestInfo requestInfo = new RequestInfo(request, response);
    requestInfo.requestID = getNextID();


    String section    = requestInfo.section;
    String subsection = requestInfo.subsection;

    String dbgStr = request.getParameter(Constants.SERVLET_PARAM_HTML_DEBUG);
    requestInfo.debugHTML = ((dbgStr != null) && (dbgStr.length() > 0));

    requestInfo.generateSidebar = true;
    String param = request.getParameter(Constants.SERVLET_PARAM_HIDE_SIDEBAR);
    if ((param != null) &&
        (param.equalsIgnoreCase(Constants.CONFIG_VALUE_TRUE)))
    {
      requestInfo.generateSidebar = false;
    }

    if (useAccessControl &&
        ((requestInfo.userIdentifier == null) ||
         (requestInfo.userIdentifier.length() == 0)))
    {
      handleUnauthenticatedUser(requestInfo);
    }
    else
    {
      // Initialize the variables used for access control.
      setAccessControlVariables(requestInfo);


      // Check to see if the SLAMD server is running.  If not, then display the
      // appropriate page to the user.
      if ((! configDBExists) &&
          (! section.equals(Constants.SERVLET_SECTION_DOCUMENTATION)) &&
          (! section.equals(Constants.SERVLET_SECTION_LICENSE_HTML)) &&
          (! section.equals(Constants.SERVLET_SECTION_LICENSE_TEXT)) &&
          (! section.equals(Constants.SERVLET_SECTION_SLAMD_LOGO)))
      {
        handleNoDB(requestInfo);
      }
      else if ((! slamdRunning) &&
          (! section.equals(Constants.SERVLET_SECTION_CONFIG)) &&
          (! section.equals(Constants.SERVLET_SECTION_STATUS)) &&
          (! section.equals(Constants.SERVLET_SECTION_DOCUMENTATION)) &&
          (! section.equals(Constants.SERVLET_SECTION_LICENSE_HTML)) &&
          (! section.equals(Constants.SERVLET_SECTION_LICENSE_TEXT)) &&
          (! section.equals(Constants.SERVLET_SECTION_SLAMD_LOGO)))
      {
        handleSLAMDUnavailable(requestInfo);
      }
      else
      {
        // See if the user just wants to view a job.  If so, then take them to
        // it.  This can make for shorter URLs if you are only interested in
        // viewing job information.
        String getJobID = request.getParameter(Constants.SERVLET_PARAM_GET_JOB);
        if ((getJobID != null) && (getJobID.length() > 0))
        {
          handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                        null, getJobID);
        }


        // Determine what to do from the parameters that have been provided
        else if (section.equals(Constants.SERVLET_SECTION_SLAMD_LOGO))
        {
          handleSLAMDLogo(requestInfo);
        }
        else if (section.equals(Constants.SERVLET_SECTION_CONFIG))
        {
          if (subsection.equals(Constants.SERVLET_SECTION_CONFIG_SERVLET))
          {
            handleServletConfig(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_CONFIG_ACCESS))
          {
            handleAccessControlConfig(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_CONFIG_SLAMD))
          {
            handleSLAMDConfig(requestInfo);
          }
          else
          {
            handleConfig(requestInfo);
          }
        }
        else if (section.equals(Constants.SERVLET_SECTION_DOCUMENTATION))
        {
          handleDocumentation(requestInfo);
        }
        else if (section.equals(Constants.SERVLET_SECTION_LICENSE_HTML))
        {
          handleHTMLLicense(requestInfo);
        }
        else if (section.equals(Constants.SERVLET_SECTION_LICENSE_TEXT))
        {
          handleTextLicense(requestInfo);
        }
        else if (section.equals(Constants.SERVLET_SECTION_STATUS))
        {
          if (subsection.equals(Constants.SERVLET_SECTION_STATUS_VIEW_LOG))
          {
            handleViewLog(requestInfo);
          }
          else
          {
            handleStatus(requestInfo);
          }
        }
        else if (section.equals(Constants.SERVLET_SECTION_JOB))
        {
          if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_PENDING) ||
              subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_RUNNING) ||
              subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED) ||
              subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GENERIC))
          {
            handleViewJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_AS_TEXT))
          {
            handleViewJobAsText(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING_AS_TEXT))
          {
            handleViewOptimizingJobAsText(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_LOG_MESSAGES))
          {
            handleViewJobLogMessages(requestInfo);
            requestInfo.generateSidebar = false;
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GROUPS))
          {
            handleViewJobGroups(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GROUP))
          {
            handleViewJobGroup(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_EDIT_GROUP_DESCRIPTION))
          {
            handleEditJobGroupDescription(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_EDIT_GROUP_PARAMS))
          {
            handleEditJobGroupParams(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_EDIT_GROUP_JOB))
          {
            handleEditJobGroupJob(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_JOB_EDIT_GROUP_OPTIMIZING_JOB))
          {
            handleEditJobGroupOptimizingJob(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_ADD_GROUP))
          {
            handleAddJobGroup(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_ADD_JOB_TO_GROUP))
          {
            handleAddJobToGroup(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_JOB_ADD_OPTIMIZING_JOB_TO_GROUP))
          {
            handleAddOptimizingJobToGroup(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_REMOVE_JOB_FROM_GROUP))
          {
            handleRemoveJobFromGroup(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_REMOVE_GROUP))
          {
            handleRemoveJobGroup(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_SCHEDULE_GROUP))
          {
            handleScheduleJobGroup(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_CLONE_GROUP))
          {
            handleCloneJobGroup(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_REAL))
          {
            handleViewRealFolderList(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_LIST_REAL_FOLDERS))
          {
            handleListRealFolders(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_LIST_VIRTUAL_FOLDERS))
          {
            handleListVirtualFolders(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_FOLDER_DESCRIPTION))
          {
            handleEditFolderDescription(requestInfo, false);
          }
          else if
               (subsection.equals(
                     Constants.SERVLET_SECTION_OPTIMIZING_FOLDER_DESCRIPTION))
          {
            handleEditFolderDescription(requestInfo, true);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_FOLDER_PUBLISH))
          {
            handlePublishFolder(requestInfo, false);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_OPTIMIZING_FOLDER_PUBLISH))
          {
            handlePublishFolder(requestInfo, true);
          }
          else if (subsection.equals(
                                   Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL))
          {
            handleVirtualJobFolders(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_STATS))
          {
            handleViewJobStatistics(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_SAVE_STATS))
          {
            handleSaveJobStatistics(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GRAPH))
          {
            if (! disableGraphs)
            {
              handleViewGraph(requestInfo);
              if (graphInNewWindow)
              {
                requestInfo.generateSidebar = false;
              }
            }
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_MONITOR_GRAPH))
          {
            if (! disableGraphs)
            {
              handleViewMonitorGraph(requestInfo);
              if (graphInNewWindow)
              {
                requestInfo.generateSidebar = false;
              }
            }
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_GRAPH))
          {
            if (! disableGraphs)
            {
              handleGraph(requestInfo);
              requestInfo.generateHTML = false;
            }
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_GRAPH_REAL_TIME))
          {
            if (! disableGraphs)
            {
              handleViewRealTimeGraph(requestInfo);
              if (graphInNewWindow)
              {
                requestInfo.generateSidebar = false;
              }
            }
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_GRAPH_REAL_TIME))
          {
            if (! disableGraphs)
            {
              handleRealTimeGraph(requestInfo);
              requestInfo.generateHTML = false;
            }
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_GRAPH_MONITOR))
          {
            if (! disableGraphs)
            {
              handleMonitorGraph(requestInfo);
              requestInfo.generateHTML = false;
            }
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_OVERLAY))
          {
            if (! disableGraphs)
            {
              handleViewOverlay(requestInfo);
              if (graphInNewWindow)
              {
                requestInfo.generateSidebar = false;
              }
            }
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_OVERLAY))
          {
            if (! disableGraphs)
            {
              handleOverlay(requestInfo);
              requestInfo.generateHTML = false;
            }
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_UPLOAD))
          {
            handleUploadedFile(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_SCHEDULE))
          {
            handleScheduleJob(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_CLONE))
          {
            handleCloneJob(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_EDIT))
          {
            handleEditJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_EDIT_COMMENTS))
          {
            handleEditJobComments(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_IMPORT_PERSISTENT))
          {
            handleImportPersistentStats(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_CANCEL))
          {
            handleCancelJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_CANCEL_AND_DELETE))
          {
            handleCancelAndDelete(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_DELETE))
          {
            handleDeleteJob(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_DISABLE))
          {
            handleDisableJob(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_ENABLE))
          {
            handleEnableJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_GENERATE_REPORT))
          {
            handleGenerateReport(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_MASS_OP))
          {
            handleMassOperation(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_MASS_OPTIMIZING))
          {
            handleMassOptimizing(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_IMPORT_JOB_DATA))
          {
            handleDataImport(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_EXPORT_JOB_DATA))
          {
            handleDataExport(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_MIGRATE))
          {
            handleMigrateData(requestInfo);
          }
          else if (subsection.equals(
                                   Constants.SERVLET_SECTION_JOB_VIEW_CLASSES))
          {
            handleViewJobClass(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_ADD_CLASS))
          {
            handleAddJobClass(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_INSTALL_JOB_PACK))
          {
            handleInstallJobPack(requestInfo);
          }
          else if (subsection.equals(
                                   Constants.SERVLET_SECTION_JOB_DELETE_CLASS))
          {
            handleDeleteJobClass(requestInfo);
          }
          else if (subsection.equals(
                                   Constants.SERVLET_SECTION_JOB_SCHEDULE_HELP))
          {
            generateScheduleHelpPage(requestInfo);
            requestInfo.generateSidebar = false;
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_JOB_OPTIMIZE))
          {
            handleOptimizeJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_CANCEL_OPTIMIZING))
          {
            handleCancelOptimizingJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_PAUSE_OPTIMIZING))
          {
            handlePauseOptimizingJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_UNPAUSE_OPTIMIZING))
          {
            handleUnpauseOptimizingJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_DELETE_OPTIMIZING))
          {
            handleDeleteOptimizingJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING))
          {
            handleViewOptimizing(requestInfo, false);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_MOVE_OPTIMIZING))
          {
            handleMoveOptimizingJob(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_CLONE_OPTIMIZING))
          {
            generateCloneOptimizingJobForm(requestInfo);
          }
          else if (subsection.equals(
                        Constants.SERVLET_SECTION_JOB_EDIT_OPTIMIZING_COMMENTS))
          {
            handleEditOptimizingComments(requestInfo);
          }
          else if (subsection.equals(
                                   Constants.SERVLET_SECTION_JOB_OPTIMIZE_HELP))
          {
            generateOptimizeHelpPage(requestInfo);
            requestInfo.generateSidebar = false;
          }
          else
          {
            if (readOnlyMode)
            {
              handleViewJob(requestInfo,
                            Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                            null, null);
            }
            else
            {
              requestInfo.htmlBody.append(getDefaultHTML(requestInfo));
            }
          }
        }
        else if (section.equals(Constants.SERVLET_SECTION_DEBUG))
        {
          if (subsection.equals(Constants.SERVLET_SECTION_DEBUG_THREADS))
          {
            handleDebugThreads(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_DEBUG_SYSPROPS))
          {
            handleDebugSystemProperties(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_DEBUG_REQUEST))
          {
            handleDebugRequest(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_DEBUG_DB))
          {
            handleDebugDatabase(requestInfo);
          }
          else if (subsection.equals(Constants.SERVLET_SECTION_DEBUG_GC))
          {
            handleDebugGC(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_DEBUG_ENABLE_INSTRUCTION_TRACE))
          {
            handleDebugEnableInstructionTrace(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_DEBUG_DISABLE_INSTRUCTION_TRACE))
          {
            handleDebugDisableInstructionTrace(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_DEBUG_ENABLE_METHOD_TRACE))
          {
            handleDebugEnableMethodTrace(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_DEBUG_DISABLE_METHOD_TRACE))
          {
            handleDebugDisableMethodTrace(requestInfo);
          }
          else if (subsection.equals(
                   Constants.SERVLET_SECTION_DEBUG_STACK_TRACE))
          {
            handleDebugStackTrace(requestInfo);
          }
        }
        else
        {
          boolean pageGenerated = false;
          String queryString = request.getQueryString();
          if ((queryString != null) && (queryString.length() > 0))
          {
            try
            {
              MessageDigest md5Digest = MessageDigest.getInstance("MD5");
              byte[] queryBytes = queryString.getBytes("UTF-8");
              String queryDigest = Base64.encode(md5Digest.digest(queryBytes));
              for (int i=0; i < Constants.QUERY_STRING_MD5.length; i++)
              {
                if (queryDigest.equals(Constants.QUERY_STRING_MD5[i]))
                {
                  generatePageFromMD5(requestInfo, queryDigest);
                  pageGenerated = true;
                  break;
                }
              }
            }
            catch (Exception e) {}
          }

          if (! pageGenerated)
          {
            if (readOnlyMode)
            {
              handleViewJob(requestInfo,
                            Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                            null, null);
            }
            else
            {
              requestInfo.htmlBody.append(getDefaultHTML(requestInfo));
            }
          }
        }
      }
    }


    // If the response hasn't been sent back yet, then do it now.
    if (requestInfo.generateHTML)
    {
      response.setContentType("text/html");
      PrintWriter writer = response.getWriter();
      if (requestInfo.generateSidebar)
      {
        writer.println(generateHTMLHeader(requestInfo));
        writer.println("<!-- START INFO MESSAGE -->" + EOL);
        writer.println(generateWarning(requestInfo.infoMessage.toString()));
        writer.println("<!-- END INFO MESSAGE -->" + EOL + EOL);
      }
      else
      {
        writer.println(generateHTMLHeaderWithoutSidebar(requestInfo));
        writer.println("<!-- START INFO MESSAGE -->" + EOL);
        writer.println(generateWarning(requestInfo.infoMessage.toString()));
        writer.println("<!-- END INFO MESSAGE -->" + EOL + EOL);
      }

      writer.println("<!-- START MAIN BODY -->" + EOL);
      writer.println(requestInfo.htmlBody.toString());
      writer.println("<!-- END MAIN BODY -->" + EOL + EOL);
      writer.println(generateHTMLFooter(requestInfo));
      writer.flush();
    }
  }



  /**
   * Handles the work of processing requests related to files that have been
   * uploaded to the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleUploadedFile(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleUploadedFile()");

    // If file uploads have been disabled, then no one can see this section.
    if (disableUploads)
    {
      logMessage(requestInfo, "File uploads disabled");
      generateAccessDeniedBody(requestInfo,
                               "Access to file uploads has been disabled.");
      return;
    }

    // The user must have at least view status permission to access anything in
    // this section.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // First, see if the request is multi-part content.  If it is, then we can
    // be pretty confident that the user is uploading a file.
    if (FileUpload.isMultipartContent(requestInfo.request))
    {
      handleFileUpload(requestInfo);
      return;
    }


    // Get the important state information for the request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    // See if the user is doing this in the context of optimizing jobs.
    boolean inOptimizing = false;
    String optStr = request.getParameter(Constants.SERVLET_PARAM_IN_OPTIMIZING);
    if (optStr != null)
    {
      inOptimizing = optStr.equalsIgnoreCase("true");
    }

    // Get the file action to determine what we want to do.
    String fileAction =
         request.getParameter(Constants.SERVLET_PARAM_FILE_ACTION);
    if (fileAction == null)
    {
      infoMessage.append("ERROR:  No action specified.<BR>" + EOL);
      if (inOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
      return;
    }
    else if (fileAction.equals(Constants.FILE_ACTION_VIEW))
    {
      handleRetrieveFile(requestInfo, inOptimizing, true);
    }
    else if (fileAction.equals(Constants.FILE_ACTION_SAVE))
    {
      handleRetrieveFile(requestInfo, inOptimizing, false);
    }
    else if (fileAction.equals(Constants.FILE_ACTION_EDIT_TYPE))
    {
      // No matter what, we need the folder name and file name.
      String folderName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      String fileName = request.getParameter(Constants.SERVLET_PARAM_FILE_NAME);
      if ((fileName == null) || (fileName.length() == 0))
      {
        infoMessage.append("The name of the file to delete was not " +
                           "provided.<BR>" + EOL);
        if (inOptimizing)
        {
          handleViewOptimizing(requestInfo, true);
        }
        else
        {
          handleViewJob(requestInfo,
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                        folderName, null);
        }
        return;
      }

      UploadedFile file = null;
      try
      {
        file = configDB.getUploadedFile(folderName, fileName);
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to retrieve the requested file -- " + e +
                           ".<BR>" + EOL);
        if (inOptimizing)
        {
          handleViewOptimizing(requestInfo, true);
        }
        else
        {
          handleViewJob(requestInfo,
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                        folderName, null);
        }
        return;
      }


      // See if the user has submitted the new MIME type to use.
      String newType = request.getParameter(Constants.SERVLET_PARAM_FILE_TYPE);
      if ((newType != null) && (newType.length() > 0))
      {
        try
        {
          file.setFileType(newType);
          configDB.writeUploadedFile(file, folderName);
          infoMessage.append("Successfully updated MIME type for file \"" +
                             fileName + "\"<BR>" + EOL);
        }
        catch (DatabaseException de)
        {
          infoMessage.append("Unable to update MIME type:  " +
                             de.getMessage() + "<BR>" + EOL);
        }

        if (inOptimizing)
        {
          handleViewOptimizing(requestInfo, true);
        }
        else
        {
          handleViewJob(requestInfo,
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                        folderName, null);
        }
        return;
      }
      else
      {
        // Display a form that allows the user to specify the new MIME type.
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Update MIME Type for File " + fileName + "</SPAN>" +
                        EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Please enter the new MIME type for the file" + EOL);
        htmlBody.append("<BR>");
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_UPLOAD) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                       Constants.FILE_ACTION_EDIT_TYPE) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_FILE_NAME,
                                              fileName) + EOL);
        if (inOptimizing)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                         "true") + EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_FILE_TYPE + "\" VALUE=\"" +
                        file.getFileType() + "\"><BR><BR>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Update MIME Type\">" +
                        EOL);
        htmlBody.append("</FORM>" + EOL);
      }
    }
    else if (fileAction.equals(Constants.FILE_ACTION_DELETE))
    {
      // We need the name of the folder and the name of the file.  The folder
      // can be null, but the file cannot.
      String folderName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      String fileName = request.getParameter(Constants.SERVLET_PARAM_FILE_NAME);
      if ((fileName == null) || (fileName.length() == 0))
      {
        infoMessage.append("The name of the file to delete was not " +
                           "provided.<BR>" + EOL);
        if (inOptimizing)
        {
          handleViewOptimizing(requestInfo, true);
        }
        else
        {
          handleViewJob(requestInfo,
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                        folderName, null);
        }
        return;
      }


      // Deleting a file requires confirmation.  If it hasn't been provided,
      // then request it.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Delete File " + fileName + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Are you sure that you want to delete this file?" +
                        EOL);
        htmlBody.append("<BR>");
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_UPLOAD) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                       Constants.FILE_ACTION_DELETE) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_FILE_NAME,
                                              fileName) + EOL);
        if (inOptimizing)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                         "true") + EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_CONFIRMED +
                        "\" VALUE=\"Yes\"></TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_CONFIRMED +
                        "\" VALUE=\"No\"></TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("  </TABLE>" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
      else if (confirmStr.equalsIgnoreCase("yes"))
      {
        try
        {
          configDB.removeUploadedFile(folderName, fileName);
          infoMessage.append("Successfully removed " + fileName +
                             " from the configuration directory.<BR>" + EOL);
          if (inOptimizing)
          {
            handleViewOptimizing(requestInfo, true);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          folderName, null);
          }
        }
        catch (DatabaseException de)
        {
          infoMessage.append("Unable to remove " + fileName +
                             " from the configuration directory -- " +
                             de.getMessage() + "<BR>" + EOL);
          if (inOptimizing)
          {
            handleViewOptimizing(requestInfo, true);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          folderName, null);
          }
        }
      }
      else
      {
        infoMessage.append("File " + fileName +
                           " was not removed from the configuration " +
                           "directory.<BR>" + EOL);
        if (inOptimizing)
        {
          handleViewOptimizing(requestInfo, true);
        }
        else
        {
          handleViewJob(requestInfo,
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                        folderName, null);
        }
      }
    }
    else if (fileAction.equals(Constants.FILE_ACTION_UPLOAD))
    {
      if (! requestInfo.mayManageFolders)
      {
        logMessage(requestInfo, "No mayManageFolders permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "remove files from job folders.");
        return;
      }

      // See if the user wants to upload file from the server's filesystem.
      String filePath =
           request.getParameter(Constants.SERVLET_PARAM_UPLOAD_FILE_PATH);
      if ((filePath == null) || (filePath.length() == 0))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Upload a File</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        htmlBody.append("Upload a file through the browser." + EOL);
        htmlBody.append("<BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" " +
                        "ENCTYPE=\"multipart/form-data\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_UPLOAD) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                       Constants.FILE_ACTION_UPLOAD) +
                        EOL);

        String folderName =
             request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
        if (folderName != null)
        {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
        }

        if (inOptimizing)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                         "true") + EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>File to Upload</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"FILE\" NAME=\"" +
                        Constants.SERVLET_PARAM_UPLOAD_FILE + "\">" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>File Description</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_FILE_DESCRIPTION + "\">" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Upload File\"</TD>" + EOL);
        htmlBody.append("  </TABLE>" + EOL);

        htmlBody.append("</FORM>" + EOL);

        htmlBody.append("<BR><BR><HR><BR>" + EOL);

        htmlBody.append("Upload a file on the server's filesystem." + EOL);
        htmlBody.append("<BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                       Constants.SERVLET_SECTION_JOB_UPLOAD) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_FILE_ACTION,
                                       Constants.FILE_ACTION_UPLOAD) +
                        EOL);

        if (folderName != null)
        {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
        }

        if (inOptimizing)
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_IN_OPTIMIZING,
                                         "true") + EOL);
        }

        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>File Path</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_UPLOAD_FILE_PATH +
                        "\" SIZE=\"40\">" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>File TYPE</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_FILE_TYPE + "\">" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>File Description</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_FILE_DESCRIPTION + "\">" + EOL);
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Upload File\"</TD>" + EOL);
        htmlBody.append("  </TABLE>" + EOL);

        htmlBody.append("</FORM>" + EOL);
      }
      else
      {
        // The user wants to upload a local file.  Read the rest of the request
        // parameters that may have been specified.
        String folderName =
             request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);

        String fileType =
             request.getParameter(Constants.SERVLET_PARAM_FILE_TYPE);
        if ((fileType == null) || (fileType.length() == 0))
        {
          fileType = "application/octet-stream";
        }

        String fileDescription =
             request.getParameter(Constants.SERVLET_PARAM_FILE_DESCRIPTION);
        if ((fileDescription == null) || (fileDescription.length() == 0))
        {
          fileDescription = null;
        }


        // Make sure that the file exists on the local filesystem and that it
        // is not larger than the maximum upload file size.
        HttpServletResponse response = requestInfo.response;
        File uploadFile = new File(filePath);
        if ((! uploadFile.exists()) || (! uploadFile.isFile()))
        {
          String message = "File \"" + filePath +
                           "\" does not exist on the SLAMD server system";
          infoMessage.append("ERROR:  " + message);
          response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE, message);
          if (inOptimizing)
          {
            handleViewOptimizing(requestInfo, true, folderName);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          folderName, null);
          }
          return;
        }


        // Read in the file data.
        byte[] fileData;
        int    fileSize;
        String fileName;
        try
        {
          fileName = uploadFile.getName();
          fileSize = (int) uploadFile.length();
          fileData = new byte[fileSize];

          int bytesRead = 0;
          FileInputStream inputStream = new FileInputStream(uploadFile);
          while (bytesRead < fileSize)
          {
            bytesRead += inputStream.read(fileData, bytesRead,
                                          (fileSize - bytesRead));
          }
          inputStream.close();
        }
        catch (Exception e)
        {
          String message = "Unable to read data from \"" + filePath +
                           "\" -- " + e;
          infoMessage.append("ERROR:  " + message);
          response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE, message);
          if (inOptimizing)
          {
            handleViewOptimizing(requestInfo, true, folderName);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          folderName, null);
          }
          return;
        }


        // Create the upload file and store it in the config directory.
        UploadedFile uploadedFile = new UploadedFile(fileName, fileType,
                                                     fileSize, fileDescription,
                                                     fileData);
        try
        {
          configDB.writeUploadedFile(uploadedFile, folderName);
          infoMessage.append("The file was uploaded successfully.");
          if (inOptimizing)
          {
            handleViewOptimizing(requestInfo, true, folderName);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          folderName, null);
          }
          return;
        }
        catch (DatabaseException de)
        {
          String message = de.getMessage();
          infoMessage.append("ERROR:  " + message);
          response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE, message);
          if (inOptimizing)
          {
            handleViewOptimizing(requestInfo, true, folderName);
          }
          else
          {
            handleViewJob(requestInfo,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          folderName, null);
          }
          return;
        }
      }
    }
    else
    {
      infoMessage.append("ERROR:  Invalid file action \"" + fileAction +
                         "\".<BR>" + EOL);
      if (inOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
      return;
    }
  }



  /**
   * Handles the work of retrieving the file and sending it back to the client.
   *
   * @param  requestInfo         The state information for this request.
   * @param  inOptimizing        Indicates whether this should be done in the
   *                             context of the optimizing jobs.
   * @param  useRealContentType  Indicates whether the real content type for the
   *                             file should be provided when returning the
   *                             file.
   */
  static void handleRetrieveFile(RequestInfo requestInfo, boolean inOptimizing,
                                boolean useRealContentType)
  {
    // Get the important state variables for this request.
    HttpServletRequest  request     = requestInfo.request;
    HttpServletResponse response    = requestInfo.response;
    StringBuilder        infoMessage = requestInfo.infoMessage;


    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    String fileName = request.getParameter(Constants.SERVLET_PARAM_FILE_NAME);

    UploadedFile file = null;
    String message = "Unable to retrieve information about file \"" + fileName +
                     "\" from folder \"" + folderName + "\" -- ";
    try
    {
      file = configDB.getUploadedFile(folderName, fileName);
      if (file == null)
      {
        message += " no uploaded file found matching that criteria.";
      }
    }
    catch (Exception e)
    {
      message += e.getMessage();
    }

    if (file == null)
    {
      infoMessage.append(message + "<BR>" + EOL);
      if (inOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
      return;
    }

    requestInfo.generateHTML = false;
    if (useRealContentType)
    {
      response.setContentType(file.getFileType());
    }
    else
    {
      response.setContentType(Constants.DEFAULT_FILE_CONTENT_TYPE);
    }

    response.addHeader("Content-Disposition",
                       "filename=\"" + file.getFileName() + '"');

    try
    {
      OutputStream outputStream = response.getOutputStream();
      outputStream.write(file.getFileData());
      outputStream.flush();
    } catch (IOException ioe) {}
  }



  /**
   * Handles the work of actually accepting an uploaded file and storing it in
   * the configuration directory.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleFileUpload(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleFileUpload()");

    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "upload files into job folders.");
      return;
    }

    StringBuilder infoMessage = requestInfo.infoMessage;

    FileUpload fileUpload = new FileUpload(new DefaultFileItemFactory());
    fileUpload.setSizeMax(maxUploadSize);

    boolean inOptimizing = false;
    String  folderName   = null;

    try
    {
      String  fileName     = null;
      String  fileType     = null;
      String  fileDesc     = null;
      int     fileSize     = -1;
      byte[]  fileData     = null;

      Iterator iterator = requestInfo.multipartFieldList.iterator();
      while (iterator.hasNext())
      {
        FileItem fileItem = (FileItem) iterator.next();
        String fieldName = fileItem.getFieldName();

        if (fieldName.equals(Constants.SERVLET_PARAM_FILE_DESCRIPTION))
        {
          fileDesc = new String(fileItem.get());
        }
        else if (fieldName.equals(Constants.SERVLET_PARAM_JOB_FOLDER))
        {
          folderName = new String(fileItem.get());
        }
        else if (fieldName.equals(Constants.SERVLET_PARAM_UPLOAD_FILE))
        {
          fileData = fileItem.get();
          fileSize = fileData.length;
          fileType = fileItem.getContentType();
          fileName = fileItem.getName();
        }
        else if (fieldName.equals(Constants.SERVLET_PARAM_IN_OPTIMIZING))
        {
          String optStr = new String(fileItem.get());
          inOptimizing  = optStr.equalsIgnoreCase("true");
        }
      }

      if (fileName == null)
      {
        infoMessage.append("Unable to process file upload:  did not receive " +
                           "any actual file data.<BR>" + EOL);
        if (inOptimizing)
        {
          handleViewOptimizing(requestInfo, true, folderName);
        }
        else
        {
          handleViewJob(requestInfo,
                        Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                        folderName, null);
        }
        return;
      }

      UploadedFile file = new UploadedFile(fileName, fileType, fileSize,
                                           fileDesc, fileData);
      configDB.writeUploadedFile(file, folderName);
      infoMessage.append("Successfully uploaded file \"" + fileName + "\"<BR>" +
                         EOL);
    }
    catch (Exception e)
    {
      infoMessage.append("Unable to process file upload:  " + e + "<BR>" + EOL);
    }

    if (inOptimizing)
    {
      handleViewOptimizing(requestInfo, true, folderName);
    }
    else
    {
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    folderName, null);
    }
  }



  /**
   * Handles the work of publishing or de-publishing a job folder (and
   * optionally the jobs contained in it) for display in restricted read-only
   * mode.
   *
   * @param  requestInfo    The state information for this request.
   * @param  forOptimizing  Indicates whether the request was issued in the
   *                        context of viewing optimizing jobs or regular
   *                        jobs.
   */
  static void handlePublishFolder(RequestInfo requestInfo,
                                  boolean forOptimizing)
  {
    logMessage(requestInfo, "In handlePublishFolder()");

    // The user must have at least manage folder permission to do anything in
    // this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "manage job folders");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the name of the folder to update.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    if ((folderName == null) || (folderName.length() == 0))
    {
      infoMessage.append("ERROR:  No folder name provided to publish or " +
                         "de-publish<BR>" + EOL);
      if (forOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
      return;
    }


    // Get the submit string to use when determining which action to perform.
    String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if ((submitStr == null) || (submitStr.length() == 0))
    {
      infoMessage.append("ERROR:  Unable to determine the action to take on " +
                         "the job folder<BR>" + EOL);
      if (forOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
      return;
    }


    // Determine what action to perform.
    boolean displayInReadOnlyMode;
    boolean updateJobs;
    if (submitStr.equals(Constants.SUBMIT_STRING_PUBLISH_FOLDER))
    {
      displayInReadOnlyMode = true;
      updateJobs            = false;
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_PUBLISH_FOLDER_JOBS))
    {
      displayInReadOnlyMode = true;
      updateJobs            = true;
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_DEPUBLISH_FOLDER))
    {
      displayInReadOnlyMode = false;
      updateJobs            = false;
    }
    else if (submitStr.equals(Constants.SUBMIT_STRING_DEPUBLISH_FOLDER_JOBS))
    {
      displayInReadOnlyMode = false;
      updateJobs            = true;
    }
    else
    {
      infoMessage.append("ERROR:  Unable to determine the action to take on " +
                         "the job folder<BR>" + EOL);
      if (forOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
      return;
    }


    // Perform the update.
    try
    {
      JobFolder folder = configDB.getFolder(folderName);
      folder.setDisplayInReadOnlyMode(displayInReadOnlyMode);

      if (updateJobs)
      {
        Job[] jobs = configDB.getJobs(folderName);
        OptimizingJob[] optimizingJobs = configDB.getOptimizingJobs(folderName);

        for (int i=0; i < jobs.length; i++)
        {
          jobs[i].setDisplayInReadOnlyMode(displayInReadOnlyMode);
          configDB.writeJob(jobs[i]);
        }

        for (int i=0; i < optimizingJobs.length; i++)
        {
          optimizingJobs[i].setDisplayInReadOnlyMode(displayInReadOnlyMode);
          configDB.writeOptimizingJob(optimizingJobs[i]);
        }
      }

      configDB.writeFolder(folder);
      infoMessage.append("Successfully updated publishing information.<BR>" +
                         EOL);
    }
    catch (Exception e)
    {
      infoMessage.append("Unable to perform the update:  " + e.getMessage() +
                         "<BR>" + EOL);
    }

    if (forOptimizing)
    {
      handleViewOptimizing(requestInfo, true);
    }
    else
    {
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
    }
  }



  /**
   * Handles all processing related to editing the description of a real job
   * folder.
   *
   * @param  requestInfo    The state information for this request.
   * @param  forOptimizing  Indicates whether the request was made when the
   *                        user was viewing optimizing jobs rather than regular
   *                        jobs.
   */
  static void handleEditFolderDescription(RequestInfo requestInfo,
                                          boolean forOptimizing)
  {
    logMessage(requestInfo, "In handleEditFolderDescription()");

    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "manage job folders.");
      return;
    }


    // Get the folder for which to edit the description.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    if ((folderName == null) || (folderName.length() == 0))
    {
      infoMessage.append("ERROR:  No job folder name provided for which to " +
                         "edit the description.<BR>" + EOL);
      handleViewRealFolderList(requestInfo);
      return;
    }
    JobFolder folder = null;
    try
    {
      folder = configDB.getFolder(folderName);
    }
    catch (Exception e)
    {
      infoMessage.append("ERROR:  Could not retrieve job folder \"" +
                         folderName + " from the configuration directory -- " +
                         e + "<BR>" + EOL);
      handleViewRealFolderList(requestInfo);
      return;
    }


    // See if the form has been submitted.  If so, then make the change and
    // display the folder.  If not, then display the form to allow them to
    // specify a new filter.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      String description =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);

      boolean displayInReadOnlyMode = false;
      String displayStr =
           request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
      if ((displayStr == null) || (displayStr.length() == 0))
      {
        displayInReadOnlyMode = false;
      }
      else
      {
        displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                                 displayStr.equalsIgnoreCase("yes") ||
                                 displayStr.equalsIgnoreCase("on") ||
                                 displayStr.equalsIgnoreCase("1"));
      }


      try
      {
        folder.setDescription(description);
        folder.setDisplayInReadOnlyMode(displayInReadOnlyMode);
        configDB.writeFolder(folder);
        infoMessage.append("Successfully updated the job folder " +
                           "description.<BR>" + EOL);
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Unable to update folder description -- " +
                           de + "<BR>" + EOL);
      }

      if (forOptimizing)
      {
        handleViewOptimizing(requestInfo, true);
      }
      else
      {
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      folderName, null);
      }
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Update Description for Job Folder \"" + folderName +
                      "\"</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please provide the new description for the " +
                      "job folder." + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);

      if (forOptimizing)
      {
        htmlBody.append("  " +
             generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                  Constants.SERVLET_SECTION_OPTIMIZING_FOLDER_DESCRIPTION) +
                  EOL);
      }
      else
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_FOLDER_DESCRIPTION) +
                        EOL);
      }

      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                            folderName) + EOL);


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      String description = folder.getDescription();
      if (description == null)
      {
        description = "";
      }
      htmlBody.append("  <TEXTAREA NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DESCRIPTION +
                      "\" ROWS=\"5\" COLS=\"80\">" + description +
                      "</TEXTAREA>" + EOL);
      htmlBody.append("   <BR><BR>");

      boolean displayInReadOnlyMode = folder.displayInReadOnlyMode();
      String displayStr =
           request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
      if (displayStr != null)
      {
        displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                                 displayStr.equalsIgnoreCase("yes") ||
                                 displayStr.equalsIgnoreCase("on") ||
                                 displayStr.equalsIgnoreCase("1"));
      }
      String checkedStr = (displayInReadOnlyMode ? " CHECKED" : "");

      htmlBody.append("   <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY + '"' +
                      checkedStr + '>');
      htmlBody.append("   Display In Restricted Read-Only Mode");
      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Update Description\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Writes the list of real job folders to the client as simple text output.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleListRealFolders(RequestInfo requestInfo)
  {
    // Indicate that we will be generating all our output here.
    HttpServletResponse response = requestInfo.response;
    requestInfo.generateHTML = false;
    response.setContentType("text/plain");


    // Retrieve the set of job folders defined in the config directory.
    JobFolder[] folders;
    try
    {
      folders = configDB.getFolders();
    }
    catch (DatabaseException de)
    {
      response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                         "Unable to retrieve folder list -- " + de);
      try
      {
        PrintWriter writer = response.getWriter();
        writer.println("Unable to retrieve folder list -- " + de);
        writer.flush();
      } catch (IOException ioe) {}

      return;
    }


    // Get the writer that will be used for sending data to the client.
    PrintWriter writer;
    try
    {
      writer = response.getWriter();
    }
    catch (IOException ioe)
    {
      // Not much that can be done about this.
      ioe.printStackTrace();
      return;
    }


    // Write the folder list to the client.
    for (int i=0; i < folders.length; i++)
    {
      writer.println(folders[i].getFolderName());
    }
    writer.flush();
  }



  /**
   * Writes the list of virtual job folders to the client as simple text output.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleListVirtualFolders(RequestInfo requestInfo)
  {
    // Indicate that we will be generating all our output here.
    HttpServletResponse response = requestInfo.response;
    requestInfo.generateHTML = false;
    response.setContentType("text/plain");


    // Retrieve the set of job folders defined in the config directory.
    JobFolder[] folders;
    try
    {
      folders = configDB.getVirtualFolders();
    }
    catch (DatabaseException de)
    {
      response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                         "Unable to retrieve folder list -- " + de);
      try
      {
        PrintWriter writer = response.getWriter();
        writer.println("Unable to retrieve folder list -- " + de);
        writer.flush();
      } catch (IOException ioe) {}

      return;
    }


    // Get the writer that will be used for sending data to the client.
    PrintWriter writer;
    try
    {
      writer = response.getWriter();
    }
    catch (IOException ioe)
    {
      // Not much that can be done about this.
      ioe.printStackTrace();
      return;
    }


    // Write the folder list to the client.
    for (int i=0; i < folders.length; i++)
    {
      writer.println(folders[i].getFolderName());
    }
    writer.flush();
  }



  /**
   * Handles all processing related to viewing and managing virtual job folders.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleVirtualJobFolders(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleVirtualJobFolders()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // See if a folder name has been specified.  If so, then work with that
    // folder.  Otherwise, show the set of folders that have been defined.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER);

    boolean displayInReadOnlyMode = false;
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if ((displayStr == null) || (displayStr.length() == 0))
    {
      displayInReadOnlyMode = false;
    }
    else
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }

    if ((folderName != null) && (folderName.length() > 0))
    {
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr == null) || (submitStr.length() == 0) ||
          submitStr.equals(Constants.SUBMIT_STRING_SELECT_ALL) ||
          submitStr.equals(Constants.SUBMIT_STRING_DESELECT_ALL))
      {
        handleViewVirtualFolder(requestInfo, folderName);
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_CREATE_VIRTUAL_FOLDER))
      {
        if (! requestInfo.mayManageFolders)
        {
          logMessage(requestInfo, "No mayManageFolders permission granted");
          generateAccessDeniedBody(requestInfo, "You do not have permission " +
                                   "to manage job folders.");
        }

        try
        {
          JobFolder folder = new JobFolder(folderName, displayInReadOnlyMode,
                                           true, null, null, null, null,
                                           null, null, null);
          configDB.writeVirtualFolder(folder);
        }
        catch (DatabaseException de)
        {
          infoMessage.append(de.getMessage() + "<BR>" + EOL);
        }

        handleViewVirtualFolderList(requestInfo);
        return;
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_DELETE_VIRTUAL_FOLDER))
      {
        if (! requestInfo.mayManageFolders)
        {
          logMessage(requestInfo, "No mayManageFolders permission granted");
          generateAccessDeniedBody(requestInfo, "You do not have permission " +
                                   "to manage job folders.");
        }

        String confirmStr =
             request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
        if ((confirmStr != null) && confirmStr.equals("Yes"))
        {
          try
          {
            configDB.removeVirtualFolder(folderName);
            infoMessage.append("Virtual job folder \"" + folderName +
                               "\" has been removed from the configuration " +
                               "directory.<BR>" + EOL);
          }
          catch (DatabaseException de)
          {
            infoMessage.append("Unable to remove virtual job folder \"" +
                               folderName + "\" from the configuration " +
                               "directory -- " + de.getMessage() + "<BR>" +
                               EOL);
          }

          handleViewVirtualFolderList(requestInfo);
        }
        else if ((confirmStr != null) && confirmStr.equals("No"))
        {
          infoMessage.append("Virtual job folder \"" + folderName +
                             "\" was not removed from the configuration " +
                             "directory.<BR>" + EOL);
          handleViewVirtualFolder(requestInfo, folderName);
        }
        else
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Delete Virtual Job Folder \"" + folderName +
                          "\"</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Are you sure that you want to delete this virtual " +
                          "job folder?" + EOL);
          htmlBody.append("<BR>" + EOL);
          htmlBody.append("<UL>" + EOL);

          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                               Constants.SUBMIT_STRING_DELETE_VIRTUAL_FOLDER) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(
                               Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                               folderName) + EOL);


          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_CONFIRMED +
                          "\" VALUE=\"Yes\"></TD>" + EOL);
          htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_CONFIRMED +
                          "\" VALUE=\"No\"></TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
          htmlBody.append("  </TABLE>" + EOL);
          htmlBody.append("</FORM>" + EOL);
        }
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_EDIT_DESCRIPTION))
      {
        // The user must have permission to manage job folders to be able to
        // do anything here.
        if (! requestInfo.mayManageFolders)
        {
          logMessage(requestInfo, "No mayManageFolders permission granted");
          generateAccessDeniedBody(requestInfo, "You do not have permission " +
                                   "to manage job folders.");
        }


        // See if the operation has been confirmed.  If so, then make the
        // requested change.  If not, then request a new description from the
        // user.
        String confirmStr =
             request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
        if ((confirmStr != null) && (confirmStr.length() > 0))
        {
          // Get the new description to use and update the job information in
          // the configuration directory.
          String description =
               request.getParameter(Constants.SERVLET_PARAM_JOB_DESCRIPTION);
          try
          {
            JobFolder folder = configDB.getVirtualFolder(folderName);
            folder.setDescription(description);
            folder.setDisplayInReadOnlyMode(displayInReadOnlyMode);
            configDB.writeVirtualFolder(folder);
            infoMessage.append("Successfully updated the folder description." +
                               "<BR>" + EOL);
          }
          catch (Exception e)
          {
            infoMessage.append("Unable to update the folder description:  " +
                               e.getMessage() + "<BR>" + EOL);
          }

          handleViewVirtualFolder(requestInfo, folderName);
          return;
        }
        else
        {
          // Get the job folder with which we will be working.
          JobFolder folder = null;
          try
          {
            folder = configDB.getVirtualFolder(folderName);
          }
          catch (Exception e)
          {
            infoMessage.append("Unable to retrieve virtual job folder \"" +
                               folderName + "\" -- " + e.getMessage() +
                               "<BR>" + EOL);
            handleViewVirtualFolderList(requestInfo);
            return;
          }

          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Update Description for Virtual Job Folder \"" +
                          folderName + "\"</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Please provide the new description for the " +
                          "virtual job folder." + EOL);
          htmlBody.append("<BR><BR>" + EOL);

          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                               Constants.SUBMIT_STRING_EDIT_DESCRIPTION) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(
                               Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                               folderName) + EOL);


          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }

          String description = folder.getDescription();
          if (description == null)
          {
            description = "";
          }
          htmlBody.append("  <TEXTAREA NAME=\"" +
                          Constants.SERVLET_PARAM_JOB_DESCRIPTION +
                          "\" ROWS=\"5\" COLS=\"80\">" + description +
                          "</TEXTAREA>" + EOL);
          htmlBody.append("  <BR><BR>" + EOL);
          htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                          Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY +
                          '"' + (displayInReadOnlyMode ? " CHECKED" : "") +
                          '>' + EOL);
          htmlBody.append("  Display In Restricted Read-Only Mode" + EOL);
          htmlBody.append("  <BR><BR>" + EOL);
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_CONFIRMED +
                          "\" VALUE=\"Update Description\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
        }
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_CLONE))
      {
        String[] jobIDs =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
        if ((jobIDs == null) || (jobIDs.length == 0))
        {
          infoMessage.append("ERROR:  No job IDs specified to clone.<BR>" +
                             EOL);
          handleViewVirtualFolder(requestInfo, folderName);
          return;
        }

        handleMassClone(requestInfo, jobIDs);
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_COMPARE))
      {
        String[] jobIDs =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
        if ((jobIDs == null) || (jobIDs.length == 0))
        {
          infoMessage.append("ERROR:  No job IDs specified to compare.<BR>" +
                             EOL);
          handleViewVirtualFolder(requestInfo, folderName);
          return;
        }

        handleMassCompare(requestInfo, jobIDs);
      }
      else if (submitStr.equals(
                    Constants.SUBMIT_STRING_REMOVE_FROM_VIRTUAL_FOLDER))
      {
        if (! requestInfo.mayManageFolders)
        {
          logMessage(requestInfo, "No mayManageFolders permission granted");
          generateAccessDeniedBody(requestInfo, "You do not have permission " +
                                   "to manage job folders.");
        }

        String[] jobIDs =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
        if ((jobIDs == null) || (jobIDs.length == 0))
        {
          infoMessage.append("ERROR:  No job IDs specified to remove from " +
                             "this virtual job folder.<BR>" + EOL);
          handleViewVirtualFolder(requestInfo, folderName);
          return;
        }

        String confirmStr =
             request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
        if ((confirmStr != null) && confirmStr.equals("Yes"))
        {
          try
          {
            JobFolder folder = configDB.getVirtualFolder(folderName);

            for (int i=0; i < jobIDs.length; i++)
            {
              folder.removeJobID(jobIDs[i]);
            }

            configDB.writeVirtualFolder(folder);
            infoMessage.append("Successfully removed the selected jobs from " +
                               "virtual job folder \"" + folderName +
                               "\".<BR>" + EOL);
          }
          catch (Exception e)
          {
            infoMessage.append("Unable to remove jobs from virtual job " +
                               "folder \"" + folderName +
                               "\" from the configuration directory -- " +
                               e.getMessage() + "<BR>" + EOL);
          }

          handleViewVirtualFolder(requestInfo, folderName);
        }
        else if ((confirmStr != null) && confirmStr.equals("No"))
        {
          infoMessage.append("No jobs were removed from virtual job folder \"" +
                             folderName + "\".<BR>" + EOL);
          handleViewVirtualFolder(requestInfo, folderName);
        }
        else
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Remove Jobs from Virtual Job Folder \"" +
                          folderName + "\"</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Are you sure that you want to remove the selected " +
                          "jobs from this virtual job folder?" + EOL);
          htmlBody.append("<BR>" + EOL);
          htmlBody.append("<UL>" + EOL);

          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                          EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                          Constants.SUBMIT_STRING_REMOVE_FROM_VIRTUAL_FOLDER) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(
                               Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                               folderName) + EOL);
          for (int i=0; i < jobIDs.length; i++)
          {
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                           jobIDs[i]) + EOL);
          }


          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
          htmlBody.append("    <TR>" + EOL);
          htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_CONFIRMED +
                          "\" VALUE=\"Yes\"></TD>" + EOL);
          htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                          Constants.SERVLET_PARAM_CONFIRMED +
                          "\" VALUE=\"No\"></TD>" + EOL);
          htmlBody.append("    </TR>" + EOL);
          htmlBody.append("  </TABLE>" + EOL);
          htmlBody.append("</FORM>" + EOL);
        }
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_EXPORT))
      {
        String[] jobIDs =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
        if ((jobIDs == null) || (jobIDs.length == 0))
        {
          infoMessage.append("ERROR:  No job IDs specified to export.<BR>" +
                             EOL);
          handleViewVirtualFolder(requestInfo, folderName);
          return;
        }

        handleMassExport(requestInfo, jobIDs);
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_GENERATE_REPORT))
      {
        handleGenerateReport(requestInfo);
      }
    }
    else
    {
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr != null) &&
          submitStr.equals(Constants.SUBMIT_STRING_CREATE_VIRTUAL_FOLDER))
      {
        if (! requestInfo.mayManageFolders)
        {
          logMessage(requestInfo, "No mayManageFolders permission granted");
          generateAccessDeniedBody(requestInfo, "You do not have permission " +
                                   "to manage job folders.");
        }

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Create a New Virtual Job Folder</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Enter the name to use for the new virtual job " +
                        "folder." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                        "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                             Constants.SUBMIT_STRING_CREATE_VIRTUAL_FOLDER) +
                        EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") +
                          EOL);
        }
        htmlBody.append("  Virtual Folder Name:  " + EOL);
        htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER +
                        "\" SIZE=\"40\">" + EOL);
        htmlBody.append("  <BR><BR>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                        Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY +
                        '"' + (displayInReadOnlyMode ? " CHECKED" : "") + '>' +
                        EOL);
        htmlBody.append("  Display In Restricted Read-Only Mode" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Create Virtual " +
                        "Folder\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
      else
      {
        handleViewVirtualFolderList(requestInfo);
      }
    }
  }



  /**
   * Handles all processing related to viewing the list of real job folders
   * that have been defined in the configuration directory.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewRealFolderList(RequestInfo requestInfo)
  {
    // Get the important state information for the request.
    String       servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder htmlBody       = requestInfo.htmlBody;
    StringBuilder infoMessage    = requestInfo.infoMessage;


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Manage Real Job Folders</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    JobFolder[] folders;
    try
    {
      folders = configDB.getFolders();
    }
    catch (DatabaseException de)
    {
      infoMessage.append("ERROR:  Unable to retrieve real job folders -- " +
                         de.getMessage() + "<BR>" + EOL);
      htmlBody.append("Unable to retrieve real job folders." + EOL);
      return;
    }

    if ((folders == null) || (folders.length == 0))
    {
      htmlBody.append("No real job folders have been defined." + EOL);
    }
    else
    {
      htmlBody.append("The following real job folders have been defined " +
                      "in the SLAMD server:" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; ((folders != null) && (i < folders.length)); i++)
      {
        String link =
             generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                          Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                          Constants.SERVLET_PARAM_JOB_FOLDER,
                          folders[i].getFolderName(),
                          folders[i].getFolderName());
        String description = folders[i].getDescription();
        if ((description == null) || (description.length() == 0))
        {
          htmlBody.append("  <LI>" + link + "</LI>" + EOL);
        }
        else
        {
        htmlBody.append("  <LI>" + link + " -- " + description + "</LI>" + EOL);
        }
      }

      htmlBody.append("</UL>" + EOL);
    }

    htmlBody.append("<BR>" + EOL);
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_MASS_OP) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }

    if (requestInfo.mayManageFolders)
    {
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_CREATE_FOLDER + "\">" +
                      EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_DELETE_FOLDER + "\">" +
                      EOL);
    }
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to viewing the list of virtual job folders
   * that have been defined in the configuration directory.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewVirtualFolderList(RequestInfo requestInfo)
  {
    // Get the important state information for the request.
    String       servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder htmlBody       = requestInfo.htmlBody;
    StringBuilder infoMessage    = requestInfo.infoMessage;


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Manage Virtual Job Folders</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    JobFolder[] folders;
    try
    {
      folders = configDB.getVirtualFolders();
    }
    catch (DatabaseException de)
    {
      infoMessage.append("ERROR:  Unable to retrieve virtual job folders -- " +
                         de.getMessage() + "<BR>" + EOL);
      htmlBody.append("Unable to retrieve virtual job folders." + EOL);
      return;
    }

    if ((folders == null) || (folders.length == 0))
    {
      htmlBody.append("No virtual job folders have been defined." + EOL);
    }
    else
    {
      htmlBody.append("The following virtual job folders have been defined " +
                      "in the SLAMD server:" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; ((folders != null) && (i < folders.length)); i++)
      {
        String link =
             generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                          Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL,
                          Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                          folders[i].getFolderName(),
                          folders[i].getFolderName());
        String description = folders[i].getDescription();
        if ((description == null) || (description.length() == 0))
        {
          htmlBody.append("  <LI>" + link + "</LI>" + EOL);
        }
        else
        {
        htmlBody.append("  <LI>" + link + " -- " + description + "</LI>" + EOL);
        }
      }

      htmlBody.append("</UL>" + EOL);
    }

    htmlBody.append("<BR>" + EOL);
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }

    if (requestInfo.mayManageFolders)
    {
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_CREATE_VIRTUAL_FOLDER + "\">" +
                      EOL);
    }
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing related to viewing the list of jobs contained in a
   * virtual folder.
   *
   * @param  requestInfo  The state information for this request.
   * @param  folderName   The name of the virtual folder to view.
   */
  static void handleViewVirtualFolder(RequestInfo requestInfo,
                                      String folderName)
  {
    logMessage(requestInfo, "In handleVirtualJobFolders()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // The user must be able to view job information to do anything here.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information");
      return;
    }


    // Get the requested virtual folder.
    JobFolder folder = null;
    try
    {
      folder = configDB.getVirtualFolder(folderName);
    }
    catch (Exception e)
    {
      infoMessage.append("Unable to retrieve jobs for folder \"" +
                         folderName + "\" -- " + e.getMessage() + "<BR>" +
                         EOL);
      handleViewVirtualFolderList(requestInfo);
      return;
    }


    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Virtual Job Folder \"" + folderName + "\"</SPAN>" +
                    EOL);
    htmlBody.append("<BR>" + EOL);


    if (folder != null)
    {
      String description = folder.getDescription();
      if ((description != null) && (description.length() > 0))
      {
        htmlBody.append("<BLOCKQUOTE>" + EOL);
        htmlBody.append("  " + description + EOL);
        htmlBody.append("</BLOCKQUOTE>" + EOL);
        htmlBody.append("<BR>" + EOL);
      }
    }


    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                                   folderName) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    if (requestInfo.mayManageFolders)
    {
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_DELETE_VIRTUAL_FOLDER + "\">" +
                      EOL);
      htmlBody.append("  &nbsp; &nbsp;" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_EDIT_DESCRIPTION + "\">" + EOL);
    }
    htmlBody.append("</FORM>" + EOL);

    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                                   folderName) + EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    Job[] jobs;
    try
    {
      jobs = configDB.getVirtualJobs(folderName);
    }
    catch (Exception e)
    {
      infoMessage.append("Unable to retrieve jobs for folder \"" +
                         folderName + "\" -- " + e.getMessage() + "<BR>" + EOL);
      return;
    }

    if (jobs.length == 0)
    {
      htmlBody.append("There are no jobs contained in this virtual folder." +
                      EOL);
      return;
    }


    htmlBody.append("  <TABLE BORDER=\"0\" CELLSPACING=\"0\">" + EOL);
    htmlBody.append("    <TR CLASS=\"" +
                    Constants.STYLE_JOB_SUMMARY_LINE_A +"\">" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><B>Job ID</B></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><B>Description</B></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><B>Job Type</B></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><B>Start Time</B></TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><B>Current State</B></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);

    boolean selectAll   = false;
    boolean deselectAll = false;

    String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if (submitStr != null)
    {
      if (submitStr.equals(Constants.SUBMIT_STRING_SELECT_ALL))
      {
        selectAll = true;
      }
      else if (submitStr.equals(Constants.SUBMIT_STRING_DESELECT_ALL))
      {
        deselectAll = true;
      }
    }

    String[] selectedJobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if (selectedJobIDs == null)
    {
      selectedJobIDs = new String[0];
    }


    for (int i=0; i < jobs.length; i++)
    {
      if (i % 2 == 0)
      {
        htmlBody.append("    <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
      }
      else
      {
        htmlBody.append("    <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      }

      String description = (jobs[i].getJobDescription() == null)
                                ? ""
                                : jobs[i].getJobDescription();
      String stateStr;
      switch (jobs[i].getJobState())
      {
        case Constants.JOB_STATE_NOT_YET_STARTED:
          stateStr = "Pending";
          break;
        case Constants.JOB_STATE_DISABLED:
          stateStr = "Disabled";
          break;
        case Constants.JOB_STATE_RUNNING:
          stateStr = "Running";
          break;
        case Constants.JOB_STATE_CANCELLED:
        case Constants.JOB_STATE_STOPPED_BY_USER:
          stateStr = "Cancelled";
          break;
        case Constants.JOB_STATE_COMPLETED_SUCCESSFULLY:
        case Constants.JOB_STATE_COMPLETED_WITH_ERRORS:
        case Constants.JOB_STATE_STOPPED_DUE_TO_DURATION:
        case Constants.JOB_STATE_STOPPED_DUE_TO_STOP_TIME:
          stateStr = "Completed";
          break;
        default:
          stateStr = "Stopped";
          break;
      }

      boolean selected = false;
      if (selectAll)
      {
        selected = true;
      }
      else if (! deselectAll)
      {
        for (int j=0; j < selectedJobIDs.length; j++)
        {
          if (selectedJobIDs[j].equals(jobs[i].getJobID()))
          {
            selected = true;
            break;
          }
        }
      }

      String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                 Constants.SERVLET_PARAM_JOB_ID,
                                 jobs[i].getJobID(),
                                 Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                                 folderName, jobs[i].getJobID());
      htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                           Constants.SERVLET_PARAM_JOB_ID + "\" VALUE=\"" +
                           jobs[i].getJobID() + '"' +
                           (selected ? " CHECKED" : "") + "></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + link + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + description + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + jobs[i].getJobName() + "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      displayDateFormat.format(jobs[i].getStartTime()) +
                      "</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + stateStr + "</TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("  <BR><BR>" + EOL);


    ArrayList<String> actions = new ArrayList<String>();
    actions.add(Constants.SUBMIT_STRING_SELECT_ALL);
    actions.add(Constants.SUBMIT_STRING_DESELECT_ALL);
    if (requestInfo.mayScheduleJob)
    {
      actions.add(Constants.SUBMIT_STRING_CLONE);
    }

    if (requestInfo.mayViewJob && (! disableGraphs))
    {
      actions.add(Constants.SUBMIT_STRING_COMPARE);
    }

    if (requestInfo.mayExportJobData)
    {
      actions.add(Constants.SUBMIT_STRING_EXPORT);
    }

    if (requestInfo.mayManageFolders)
    {
      actions.add(Constants.SUBMIT_STRING_REMOVE_FROM_VIRTUAL_FOLDER);
    }

    if ((reportGenerators != null) && (reportGenerators.length > 0))
    {
      actions.add(Constants.SUBMIT_STRING_GENERATE_REPORT);
    }


    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    htmlBody.append("    <TR>" + EOL);

    for (int i=0; i < actions.size(); i++)
    {
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      actions.get(i) + "\">" + EOL);

      if (((i % 5) == 4) && (i < (actions.size() - 1)))
      {
        htmlBody.append("    </TR>" + EOL);
        htmlBody.append("    <TR>" + EOL);
      }
    }

    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);

    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing necessary to generate a report of job results.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleGenerateReport(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleGenerateReport()");

    // If the user doesn't have view job permission, then they can't see this
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Make sure that at least one report generator has been defined.
    if ((reportGenerators == null) || (reportGenerators.length == 0))
    {
      infoMessage.append("ERROR:  No report generators defined<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Generate Job Data Report</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Job reporting is unavailable because no report " +
                      "generators have been defined in the configuration." +
                      EOL);
      return;
    }


    // Determine which jobs should be included in the report.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if (jobIDs == null)
    {
      jobIDs = new String[0];
    }

    String[] optimizingJobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);
    if (optimizingJobIDs == null)
    {
      optimizingJobIDs = new String[0];
    }

    if ((jobIDs.length == 0) && (optimizingJobIDs.length == 0))
    {
      boolean virtualFolder = false;
      String folderName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if ((folderName == null) || (folderName.length() == 0))
      {
        folderName =
             request.getParameter(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER);
        if (folderName != null)
        {
          virtualFolder = true;
        }
      }

      try
      {
        Job[] jobs;
        OptimizingJob[] optimizingJobs;
        if (virtualFolder)
        {
          jobs           = configDB.getSummaryVirtualJobs(folderName);
          optimizingJobs = new OptimizingJob[0];
        }
        else
        {
          jobs           = configDB.getSummaryJobs(folderName);
          optimizingJobs = configDB.getSummaryOptimizingJobs(folderName);
        }

        jobIDs = new String[jobs.length];
        for (int i=0; i < jobs.length; i++)
        {
          jobIDs[i] = jobs[i].getJobID();
        }

        optimizingJobIDs = new String[optimizingJobs.length];
        for (int i=0; i < optimizingJobs.length; i++)
        {
          optimizingJobIDs[i] = optimizingJobs[i].getOptimizingJobID();
        }
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));

        infoMessage.append("ERROR:  Unable to retrieve job information -- " +
                           e.getMessage() + "<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Generate Job Data Report</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("An error occurred while trying to retrieve " +
                        "information about the jobs and/or optimizing jobs " +
                        "in folder " + folderName + '.' + EOL);
        return;
      }

      if ((jobIDs.length == 0) && (optimizingJobIDs.length == 0))
      {
        infoMessage.append("ERROR:  No job information found<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Generate Job Data Report</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("No job or optimizing job data was found in folder " +
                        folderName + '.' + EOL);
        return;
      }
    }


    // Determine whether we know which report generator to use.
    ReportGenerator reportGenerator = null;
    if (reportGenerators.length == 1)
    {
      reportGenerator = reportGenerators[0].newInstance();
    }
    else
    {
      String generatorName =
           request.getParameter(Constants.SERVLET_PARAM_REPORT_GENERATOR);
      if ((generatorName != null) && (generatorName.length() > 0))
      {
        for (int i=0; i < reportGenerators.length; i++)
        {
          if (generatorName.equals(reportGenerators[i].getClass().getName()))
          {
            reportGenerator = reportGenerators[i].newInstance();
            break;
          }
        }
      }
    }

    if (reportGenerator == null)
    {
      // We need to provide a form that allows the user to select the report
      // generator.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Generate Job Data Report</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please select the report generator to use." + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">");
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_GENERATE_REPORT) +
                      EOL);
      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }
      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(
                                    Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                    optimizingJobIDs[i]) + EOL);
      }

      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_REPORT_GENERATOR + "\">");
      for (int i=0; i < reportGenerators.length; i++)
      {
        htmlBody.append("    <OPTION VALUE=\"" +
                        reportGenerators[i].getClass().getName() + "\">" +
                        reportGenerators[i].getReportGeneratorName() + EOL);
      }
      htmlBody.append("  </SELECT>");
      htmlBody.append("  <BR><BR>");
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Continue\">" + EOL);
      htmlBody.append("</FORM>");
      return;
    }


    // Determine whether the user has submitted the form to configure the
    // report parameters.
    Parameter[] params = reportGenerator.newInstance().
                              getReportParameterStubs().getParameters();
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) && (confirmStr.length() > 0))
    {
      boolean configValid = true;
      for (int i=0; i < params.length; i++)
      {
        String[] values =
             request.getParameterValues(
                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                  params[i].getName());
        try
        {
          params[i].htmlInputFormToValue(values);
        }
        catch (InvalidValueException ive)
        {
          infoMessage.append("Invalid value for parameter \"" +
                             params[i].getDisplayName() + "\" -- " +
                             ive.getMessage() + "<BR>" + EOL);
          configValid = false;
        }
      }

      if (configValid)
      {
        reportGenerator.initializeReporter(new ParameterList(params));

        for (int i=0; i < jobIDs.length; i++)
        {
          try
          {
            Job job = configDB.getJob(jobIDs[i]);
            reportGenerator.addJobReport(job);
          }
          catch (Exception e)
          {
            // We should probably display an error page here, but since it
            // should be very rare, just log a message and continue.
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                                   "Unable to retrieve job " + jobIDs[i] +
                                   " to include in generated report");
          }
        }

        for (int i=0; i < optimizingJobIDs.length; i++)
        {
          try
          {
            OptimizingJob optimizingJob = getOptimizingJob(optimizingJobIDs[i]);
            reportGenerator.addOptimizingJobReport(optimizingJob);
          }
          catch (Exception e)
          {
            // We should probably display an error page here, but since it
            // should be very rare, just log a message and continue.
            slamdServer.logMessage(Constants.LOG_LEVEL_JOB_PROCESSING,
                                   "Unable to retrieve optimizing job " +
                                   optimizingJobIDs[i] +
                                   " to include in generated report");
          }
        }

        requestInfo.generateHTML = false;
        reportGenerator.generateReport(requestInfo);
        return;
      }
    }

    // Display the form that allows the user to customize the report
    // that will be generated.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Generate Job Data Report</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Please indicate how the report should be generated." +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                    "\">");
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_GENERATE_REPORT) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_REPORT_GENERATOR,
                                   reportGenerator.getClass().getName()) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                          "1") + EOL);
    for (int i=0; i < jobIDs.length; i++)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                            jobIDs[i]) + EOL);
    }
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                  optimizingJobIDs[i]) + EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
    for (int i=0; i < params.length; i++)
    {
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>" + params[i].getDisplayName() + "</TD>" +
                      EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" +
                      params[i].getHTMLInputForm(
                           Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) + "</TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
    }

    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("    <TR>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
    htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" " +
                    "VALUE=\"Continue\"></TD>" + EOL);
    htmlBody.append("    </TR>" + EOL);
    htmlBody.append("  </TABLE>" + EOL);
    htmlBody.append("</FORM>");
  }




  /**
   * Handles all processing necessary to perform operations on multiple jobs at
   * the same time.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleMassOperation(RequestInfo requestInfo)
  {
    HttpServletRequest request = requestInfo.request;


    // Get the job IDs of the jobs on which to perform the operation.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);

    // Determine which mass operation the user wishes to perform.
    String opString = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if (opString == null)
    {
      return;
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_SELECT_ALL) ||
             opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DESELECT_ALL))
    {
      handleViewJob(requestInfo,
                    request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY),
                    null, null);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_CANCEL))
    {
      handleMassCancel(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(
                  Constants.SUBMIT_STRING_CANCEL_AND_DELETE))
    {
      handleMassCancelAndDelete(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_CLONE))
    {
      handleMassClone(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_COMPARE))
    {
      handleMassCompare(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DELETE))
    {
      handleMassDelete(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DISABLE))
    {
      handleMassDisable(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_ENABLE))
    {
      handleMassEnable(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_EXPORT))
    {
      handleMassExport(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_CREATE_FOLDER))
    {
      handleCreateFolder(requestInfo);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DELETE_FOLDER))
    {
      handleDeleteFolder(requestInfo);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_MOVE))
    {
      handleMoveJobs(requestInfo);
    }
    else if (opString.equalsIgnoreCase(
                           Constants.SUBMIT_STRING_ADD_TO_VIRTUAL_FOLDER))
    {
      handleAddToVirtualFolder(requestInfo);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_PUBLISH_JOBS))
    {
      handleMassPublish(requestInfo, true);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DEPUBLISH_JOBS))
    {
      handleMassPublish(requestInfo, false);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_GENERATE_REPORT))
    {
      handleGenerateReport(requestInfo);
    }
  }



  /**
   * Handles the work of cancelling multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to cancel.
   */
  static void handleMassCancel(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassCancel()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    // The user must have permission to cancel jobs to access this section.
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "cancel jobs.");
      return;
    }

    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No jobs specified to cancel.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }

    // See if the user has provided confirmation.  If so, then cancel the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Cancel Selected Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to cancel the selected " +
                      "jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID, jobIDs[i],
                                   jobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_CANCEL) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                     category) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      for (int i=0; i < jobIDs.length; i++)
      {
        scheduler.cancelJob(jobIDs[i], false);
        infoMessage.append("Requested cancel for job " + jobIDs[i] + ".<BR>" +
                           EOL);
      }

      infoMessage.append("It may take several seconds for all jobs to be " +
                         "cancelled.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
    else
    {
      infoMessage.append("No jobs were cancelled.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
  }



  /**
   * Handles the work of cancelling multiple jobs and removing them from SLAMD
   * entirely.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to cancel.
   */
  static void handleMassCancelAndDelete(RequestInfo requestInfo,
                                        String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassCancelAndDelete()");


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    // The user must have permission to cancel and delete jobs to access this
    // section.
    if (! requestInfo.mayCancelJob)
    {
      logMessage(requestInfo, "No mayCancelJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "cancel jobs.");
      return;
    }
    else if (! requestInfo.mayDeleteJob)
    {
      logMessage(requestInfo, "No mayDeleteJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "delete jobs.");
      return;
    }

    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No jobs specified to cancel and delete.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }


    // See if any of the jobs are no longer running or are associated with
    // optimizing jobs.  If so, then remove them from the list of jobs on which
    // we will operate.
    ArrayList<String> idList = new ArrayList<String>(jobIDs.length);
    for (int i=0; i < jobIDs.length; i++)
    {
      Job job = scheduler.getPendingJob(jobIDs[i]);
      if (job == null)
      {
        infoMessage.append("Not going to cancel and delete job " + jobIDs[i] +
                           " because it is no longer in the pending jobs " +
                           "queue.<BR>" + EOL);
      }
      else if (job.getOptimizingJobID() != null)
      {
        infoMessage.append("Not going to cancel and delete job " + jobIDs[i] +
                           " because it is associated with an optimizing " +
                           "job.<BR>" + EOL);
      }
      else
      {
        idList.add(jobIDs[i]);
      }
    }

    jobIDs = new String[idList.size()];
    idList.toArray(jobIDs);
    if (jobIDs.length == 0)
    {
      infoMessage.append("No jobs left to cancel and delete.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }



    // See if the user has provided confirmation.  If so, then cancel the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Cancel and Delete Selected Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to cancel and delete the " +
                      "selected jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID, jobIDs[i],
                                   jobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                           Constants.SUBMIT_STRING_CANCEL_AND_DELETE) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                     category) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      for (int i=0; i < jobIDs.length; i++)
      {
        scheduler.cancelAndDeleteJob(jobIDs[i]);
        infoMessage.append("Requested cancel and delete for job " + jobIDs[i] +
                           ".<BR>" + EOL);
      }

      handleViewJob(requestInfo, category, null, null);
    }
    else
    {
      infoMessage.append("No jobs were cancelled.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
  }



  /**
   * Handles the work of cloning multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to clone.
   */
  static void handleMassClone(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassClone()");

    // The user must have permission to schedule jobs to access this section.
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "clone jobs.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No jobs specified to clone.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }

    // See if the user has provided confirmation.  If so, then clone the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Clone Selected Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to clone the selected " +
                      "jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID, jobIDs[i],
                                   jobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_CLONE) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                     category) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      try
      {
        JobFolder[] folders = configDB.getFolders();
        if ((folders != null) && (folders.length > 0))
        {
          String folderName =
               request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
          if (folderName == null)
          {
            folderName = "";
          }

          htmlBody.append("  Place jobs in folder " + EOL);
          htmlBody.append("  <SELECT NAME=\"" +
                          Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
          htmlBody.append("    <OPTION VALUE=\"\">Unclassified" + EOL);
          for (int i=0; i < folders.length; i++)
          {
            htmlBody.append("    <OPTION VALUE=\"" +
                            folders[i].getFolderName() + '"');
            if (folderName.equals(folders[i].getFolderName()))
            {
              htmlBody.append(" SELECTED");
            }

            htmlBody.append("> " + folders[i].getFolderName() + EOL);
          }
          htmlBody.append("  </SELECT>" + EOL);
          htmlBody.append("  <BR>" + EOL);
        }
        else
        {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                         "") + EOL);
        }
      }
      catch (DatabaseException de)
      {
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                         "") + EOL);
      }

      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT +
                      "\">" + EOL);
      htmlBody.append("  Make Jobs Interdependent" + EOL);
      htmlBody.append("  <BR>" + EOL);

      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_DISABLED +
                      "\" CHECKED>" + EOL);
      htmlBody.append("  Make Jobs Disabled" + EOL);
      htmlBody.append("  <BR>" + EOL);

      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      boolean makeInterDependent = false;
      boolean makeDisabled       = false;
      String  folderName         = null;

      String dependentID = null;
      String dependStr =
           request.getParameter(
                Constants.SERVLET_PARAM_JOB_MAKE_INTERDEPENDENT);
      if (dependStr != null)
      {
        makeInterDependent = (dependStr.equalsIgnoreCase("true") ||
                              dependStr.equalsIgnoreCase("yes") ||
                              dependStr.equalsIgnoreCase("on") ||
                              dependStr.equalsIgnoreCase("1"));
      }

      String disabledStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_DISABLED);
      if (disabledStr != null)
      {
        makeDisabled = (disabledStr.equalsIgnoreCase("true") ||
                        disabledStr.equalsIgnoreCase("yes") ||
                        disabledStr.equalsIgnoreCase("on") ||
                        disabledStr.equalsIgnoreCase("1"));
      }

      folderName = request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if ((folderName == null) || (folderName.length() == 0))
      {
        folderName = null;
      }

      for (int i=0; i < jobIDs.length; i++)
      {
        Job j = null;
        try
        {
          j = configDB.getJob(jobIDs[i]);
          if (j == null)
          {
            infoMessage.append("Unable to clone job " + jobIDs[i] +
                               ":  Job not found.<BR>" + EOL);
            continue;
          }
        }
        catch (Exception e)
        {
          infoMessage.append("Unable to clone job " + jobIDs[i] + ":  " + e +
                             "<BR>" + EOL);
          continue;
        }

        try
        {
          Job newJob = new Job(slamdServer, j.getJobClassName(),
                               j.getNumberOfClients(), j.getThreadsPerClient(),
                               j.getThreadStartupDelay(), new Date(), null,
                               j.getDuration(), j.getCollectionInterval(),
                               j.getParameterList(), j.displayInReadOnlyMode());

          if (makeDisabled)
          {
            newJob.setJobState(Constants.JOB_STATE_DISABLED);
          }

          newJob.setJobDescription(j.getJobDescription());
          newJob.setWaitForClients(j.waitForClients());
          newJob.setRequestedClients(j.getRequestedClients());
          newJob.setResourceMonitorClients(j.getResourceMonitorClients());
          newJob.setJobComments(j.getJobComments());

          if (dependentID != null)
          {
            newJob.setDependencies(new String[] { dependentID });
          }


          // Normally, this wouldn't be required, but we'll do it anyway because
          // some jobs can do sneaky things in the validateJobInfo() method.
          try
          {
            JobClass jobClass = newJob.getJobClass();
            jobClass.validateJobInfo(newJob.getNumberOfClients(),
                                     newJob.getThreadsPerClient(),
                                     newJob.getThreadStartupDelay(),
                                     newJob.getStartTime(),
                                     newJob.getStopTime(), newJob.getDuration(),
                                     newJob.getCollectionInterval(),
                                     newJob.getParameterList());
          }
          catch (InvalidValueException ive)
          {
            infoMessage.append("WARNING:  validateJobInfo failed when " +
                               "cloning job " + j.getJobID() + ":  " +
                               ive.getMessage()  + "<BR>" + EOL);
          }

          String newJobID = scheduler.scheduleJob(newJob, folderName);
          infoMessage.append("Successfully scheduled job " + newJobID +
                             " for execution.<BR>" + EOL);

          if (makeInterDependent)
          {
            dependentID = newJobID;
          }
        }
        catch (Exception e)
        {
          infoMessage.append("Unable to clone job " + jobIDs[i] + ":  " +
                             e + "<BR>" + EOL);
        }
      }

      if (makeDisabled)
      {
        infoMessage.append("Note that all cloned jobs are currently disabled " +
                           "so that they may be edited as necessary.<BR>" +
                           EOL);
      }
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                    null, null);
    }
    else
    {
      infoMessage.append("No jobs were cloned.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
  }



  /**
   * Handles the work of comparing multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to compare.
   */
  static void handleMassCompare(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassCompare()");

    // The user must have permission to view jobs to access this section.
    if (! requestInfo.mayViewJob)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least two job IDs were specified.  If not, then print
    // an appropriate error message and go back to viewing the completed jobs.
    if ((jobIDs == null) || (jobIDs.length < 2))
    {
      infoMessage.append("You must specify at least two jobs of the same " +
                         "type to be compared.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }


    // Retrieve all of the jobs and make sure that they are all of the same
    // type.
    ArrayList<Job> jobList = new ArrayList<Job>();
    String jobClass  = null;
    for (int i=0; i < jobIDs.length; i++)
    {
      Job job = null;
      try
      {
        job = configDB.getJob(jobIDs[i]);
        if (job == null)
        {
          infoMessage.append("Unable to retrieve job " + jobIDs[i] +
                             " -- job not found.<BR>" + EOL);
        }
        else
        {
          // Make sure that the job is of the same type as all other jobs in the
          // selection.
          if (jobClass == null)
          {
            jobClass = job.getJobClassName();
          }
          else
          {
            if (! jobClass.equals(job.getJobClassName()))
            {
              infoMessage.append("Skipping job " + jobIDs[i] +
                                 " because it is not of the same type as the " +
                                 " first job selected (" + jobClass + ").<BR>" +
                                 EOL);
              continue;
            }
          }

          // Make sure that the job has statistics available.
          if (! job.hasStats())
          {
              infoMessage.append("Skipping job " + jobIDs[i] +
                                 " because it does not have any statistics " +
                                 "available.<BR>" + EOL);
              continue;
          }

          jobList.add(job);
        }
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to retrieve job " + jobIDs[i] + " -- " + e +
                           "<BR>" + EOL);
      }
    }


    // Make sure that at least two jobs were placed in the job list.
    if (jobList.size() < 2)
    {
      infoMessage.append("Unable to perform the requested comparison because " +
                         "there were not at least two jobs meeting the " +
                         "necessary criteria.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }


    // Sort the job information based on the actual start times for the jobs.
    // Since there should not be that many jobs to compare, and since it is
    // likely that they will already be sorted anyway, then a selection sort
    // should be the fastest and simplest way to do it.
    long[] startTimes = new long[jobList.size()];
    Job[] jobs = new Job[jobList.size()];
    for (int i=0; i < jobs.length; i++)
    {
      jobs[i] = jobList.get(i);
      startTimes[i] = jobs[i].getActualStartTime().getTime();
    }
    for (int i=0; i < jobs.length; i++)
    {
      int  slot         = -1;
      long minStartTime = startTimes[i];

      for (int j=i+1; j < jobs.length; j++)
      {
        if (startTimes[j] < minStartTime)
        {
          slot         = j;
          minStartTime = startTimes[j];
        }
      }

      if (slot > 0)
      {
        Job  tempJob       = jobs[slot];
        long tempStartTime = startTimes[slot];
        jobs[slot]         = jobs[i];
        startTimes[slot]   = startTimes[i];
        jobs[i]            = tempJob;
        startTimes[i]      = tempStartTime;
      }
    }


    // Get the names of the stat trackers that will be available for comparison.
    // For now, this will only be the stat trackers available in the first job
    // in the list of selected jobs.  Although it is possible that the different
    // jobs selected will have different sets of statistics, that introduces too
    // much complexity into this code for the current version.
    String[] trackerNames = jobs[0].getStatTrackerNames();


    // Determine how the comparison should be done.  It can either be a trend
    // comparison (where each job counts as one data point) or a side-by-side
    // comparison (where the jobs are compared across their entire durations).
    String confirmedStr =
         request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);

    if ((confirmedStr != null) &&
        confirmedStr.equals(Constants.CONFIG_VALUE_FALSE))
    {
      String trackerName =
           request.getParameter(Constants.SERVLET_PARAM_STAT_TRACKER);
      StatTracker infoTracker = jobs[0].getStatTrackers(trackerName)[0];

      int graphWidth  = defaultGraphWidth;
      try
      {
        graphWidth =
             Integer.parseInt(request.getParameter(
                                   Constants.SERVLET_PARAM_GRAPH_WIDTH));
      } catch (Exception e) {}

      int graphHeight = defaultGraphHeight;
      try
      {
        graphHeight =
             Integer.parseInt(request.getParameter(
                                   Constants.SERVLET_PARAM_GRAPH_HEIGHT));
      } catch (Exception e) {}

      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Compare " + trackerName + " for Multiple \"" +
                      jobs[0].getJobName() + "\" Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                     Constants.SUBMIT_STRING_COMPARE) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                     Constants.CONFIG_VALUE_FALSE) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_USE_REQUEST_PARAMS,
                                     Constants.CONFIG_VALUE_TRUE) + EOL);

      String hideStr = (graphInNewWindow
                        ? Constants.CONFIG_VALUE_TRUE
                        : Constants.CONFIG_VALUE_FALSE);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HIDE_SIDEBAR,
                                     hideStr) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      for (int i=0; i < jobs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobs[i].getJobID()) + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Graph Width</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_GRAPH_WIDTH + "\" VALUE=\"" +
                      graphWidth + "\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Graph Height</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_GRAPH_HEIGHT + "\" VALUE=\"" +
                      graphHeight + "\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistic to Compare</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_STAT_TRACKER + "\">" + EOL);
      for (int i=0; i < trackerNames.length; i++)
      {
        if (trackerName.equalsIgnoreCase(trackerNames[i]))
        {
          htmlBody.append("          <OPTION SELECTED VALUE=\"" +
                          trackerNames[i] + "\">" + trackerNames[i] + EOL);
        }
        else
        {
          htmlBody.append("          <OPTION VALUE=\"" + trackerNames[i] +
                          "\">" + trackerNames[i] + EOL);
        }
      }
      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      Parameter[] params =
           infoTracker.getGraphParameterStubs(jobs).clone().getParameters();
      String useRequestValuesStr =
           request.getParameter(Constants.SERVLET_PARAM_USE_REQUEST_PARAMS);
      boolean useRequestValues =
           ((useRequestValuesStr != null) &&
            (useRequestValuesStr.equals(Constants.CONFIG_VALUE_TRUE)));
      for (int i=0; i < params.length; i++)
      {
        if (useRequestValues)
        {
          try
          {
            String[] values = request.getParameterValues(
                                   Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                                   params[i].getName());
            params[i].htmlInputFormToValue(values);
          }
          catch (InvalidValueException ive) {}
        }

        htmlBody.append("    <TR>" + EOL);
        htmlBody.append("      <TD>" + params[i].getDisplayName() + "</TD>" +
                        EOL);
        htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("      <TD>" +
                        params[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("    </TR>" + EOL);
      }

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\">&nbsp;</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD COLSPAN=\"3\"><INPUT TYPE=\"SUBMIT\" " +
                      "VALUE=\"Compare\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);

      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);

      // Generate the image tag.  Replace any spaces with the '+' sign.
      try
      {
        htmlBody.append("<BR><BR>" + EOL);
        String imageURI = servletBaseURI + '?' +
                          Constants.SERVLET_PARAM_SECTION + '=' +
                          Constants.SERVLET_SECTION_JOB + '&' +
                          Constants.SERVLET_PARAM_SUBSECTION + '=' +
                          Constants.SERVLET_SECTION_JOB_GRAPH + '&' +
                          Constants.SERVLET_PARAM_GRAPH_WIDTH + '=' +
                          graphWidth + '&' +
                          Constants.SERVLET_PARAM_GRAPH_HEIGHT + '=' +
                          graphHeight + '&' +
                          Constants.SERVLET_PARAM_STAT_TRACKER + '=' +
                          URLEncoder.encode(trackerName, "UTF-8");
        for (int i=0; i < jobIDs.length; i++)
        {
          imageURI += '&' + Constants.SERVLET_PARAM_JOB_ID + '=' + jobIDs[i];
        }
        for (int i=0; i < params.length; i++)
        {
          imageURI += '&' + Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                      params[i].getName() + '=' + params[i].getValueString();
        }

        htmlBody.append("<IMG SRC=\"" + imageURI.replace(' ', '+') +
                        "\" WIDTH=\"" + graphWidth + "\" HEIGHT=\"" +
                        graphHeight + "\" ALT=\"Graph of Results for Job " +
                        "Comparison\">" + EOL);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
      }
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Compare Multiple \"" + jobs[0].getJobName() +
                      "\" Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      '"' + blankTarget + '>' + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                     Constants.SUBMIT_STRING_COMPARE) + EOL);

      // Why is this set to "false"?  It's a subtle difference, but it's used
      // in the previous section when assigning values to the parameters for the
      // stat tracker.  If the value is set to "true", then the values of those
      // parameters will be set from the request parameters.  If it's set to
      // something else, then the default values will be used.
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                     Constants.CONFIG_VALUE_FALSE) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_USE_REQUEST_PARAMS,
                                     Constants.CONFIG_VALUE_FALSE) + EOL);

      String hideStr = (graphInNewWindow
                        ? Constants.CONFIG_VALUE_TRUE
                        : Constants.CONFIG_VALUE_FALSE);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HIDE_SIDEBAR,
                                     hideStr) + EOL);

      for (int i=0; i < jobs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobs[i].getJobID()) + EOL);
      }
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Statistic to Graph</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_STAT_TRACKER + "\">" + EOL);
      for (int i=0; i < trackerNames.length; i++)
      {
        htmlBody.append("          <OPTION VALUE=\"" + trackerNames[i] + "\">" +
                        trackerNames[i] + EOL);
      }
      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Graph\"></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);

      htmlBody.append("</FORM>" + EOL);
      htmlBody.append("<BR>" + EOL);


      boolean oneFound = false;
      boolean twoFound = false;
      for (int i=0; i < trackerNames.length; i++)
      {
        StatTracker[] trackers = jobs[0].getStatTrackers(trackerNames[i]);
        if ((trackers != null) && (trackers.length > 0) &&
            trackers[0].isSearchable())
        {
          if (oneFound)
          {
            twoFound = true;
            break;
          }
          else
          {
            oneFound = true;
          }
        }
      }

      if (twoFound)
      {
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        '"' + blankTarget + '>' + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_JOB) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_JOB_VIEW_OVERLAY) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HIDE_SIDEBAR,
                                       hideStr) + EOL);

        for (int i=0; i < jobs.length; i++)
        {
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                                jobs[i].getJobID()) + EOL);
        }
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }

        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Graph Overlaid Statistics\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
        htmlBody.append("<BR>" + EOL);
      }

      for (int i=0; i < trackerNames.length; i++)
      {
        htmlBody.append("<B>" + trackerNames[i] + "</B><BR>" + EOL);
        htmlBody.append("<TABLE BORDER=\"1\">" + EOL);

        boolean labelsDisplayed = false;
        for (int j=0; j < jobs.length; j++)
        {
          StatTracker[] trackers = jobs[j].getStatTrackers(trackerNames[i]);
          if ((trackers != null) && (trackers.length > 0))
          {
            StatTracker tracker = trackers[0].newInstance();
            tracker.aggregate(trackers);

            if (! labelsDisplayed)
            {
              String[] labels = tracker.getSummaryLabels();
              htmlBody.append("  <TR>" + EOL);
              htmlBody.append("    <TD><B>Job ID</B></TD>" + EOL);
              htmlBody.append("    <TD><B>Job Description</B></TD>" + EOL);

              for (int k=0; k < labels.length; k++)
              {
                htmlBody.append("    <TD><B>" + labels[k] + "</B></TD>" + EOL);
              }

              htmlBody.append("  </TR>" + EOL);
              labelsDisplayed = true;
            }

            htmlBody.append("  <TR>" + EOL);

            String link =
                 generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                              Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                              Constants.SERVLET_PARAM_JOB_ID,
                              jobs[j].getJobID(), jobs[j].getJobID());
            htmlBody.append("    <TD>" + link + "</TD>" + EOL);
            htmlBody.append("    <TD>" + jobs[j].getJobDescription() + "</TD>" +
                            EOL);

            String[] data = tracker.getSummaryData();
            for (int k=0; k < data.length; k++)
            {
              htmlBody.append("    <TD>" + data[k] + "</TD>" + EOL);
            }

            htmlBody.append("  </TR>" + EOL);
          }
        }

        htmlBody.append("</TABLE>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
      }
    }
  }



  /**
   * Handles the work of deleting multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to delete.
   */
  static void handleMassDelete(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassDelete()");

    // The user must have permission to delete jobs to access this section.
    if (! requestInfo.mayDeleteJob)
    {
      logMessage(requestInfo, "No mayDeleteJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "delete jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       infoMessage    = requestInfo.infoMessage;
    StringBuilder       htmlBody       = requestInfo.htmlBody;


    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No jobs specified to delete.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }

    // See if the user has provided confirmation.  If so, then delete the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Delete Selected Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to delete the selected " +
                      "jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID, jobIDs[i],
                                   jobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_DELETE) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                     category) + EOL);

      String folderName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if ((folderName != null) && (folderName.length() > 0))
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
      }

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      for (int i=0; i < jobIDs.length; i++)
      {
        Job job = null;
        try
        {
          job = configDB.getJob(jobIDs[i]);
        } catch (Exception e) {}
        if ((job != null) && (job.getOptimizingJobID() != null))
        {
          OptimizingJob optimizingJob = null;
          try
          {
            optimizingJob = getOptimizingJob(job.getOptimizingJobID());
            if (optimizingJob != null)
            {
              infoMessage.append("Not removing job " + jobIDs[i] +
                                 " because it is associated with optimizing " +
                                 "job " + job.getOptimizingJobID() + "<BR>" +
                                 EOL);
              continue;
            }
          } catch (Exception e) {}
        }

        try
        {
          configDB.removeJob(jobIDs[i]);
          infoMessage.append("Deleted job " + jobIDs[i] + ".<BR>" +
                             EOL);
        }
        catch (DatabaseException de)
        {
          infoMessage.append("Unable to delete job " + jobIDs[i] + ":  " +
                             de + "<BR>" + EOL);
        }
      }

      handleViewJob(requestInfo, category, null, null);
    }
    else
    {
      infoMessage.append("No jobs were deleted.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
  }



  /**
   * Handles the work of disabling multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to disable.
   */
  static void handleMassDisable(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassDisable()");

    // The user must have permission to schedule jobs to access this section.
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "disable jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No jobs specified to disable.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }

    // See if the user has provided confirmation.  If so, then disable the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Disable Selected Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to disable the selected " +
                      "jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID, jobIDs[i],
                                   jobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_DISABLE) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                     category) + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      for (int i=0; i < jobIDs.length; i++)
      {
        try
        {
          scheduler.disableJob(jobIDs[i]);
          infoMessage.append("Disabled job " + jobIDs[i] + ".<BR>" +
                             EOL);
        }
        catch (SLAMDServerException sse)
        {
          infoMessage.append("Unable to disable job " + jobIDs[i] + ":  " +
                             sse + "<BR>" + EOL);
        }
      }

      handleViewJob(requestInfo, category, null, null);
    }
    else
    {
      infoMessage.append("No jobs were disabled.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
  }



  /**
   * Handles the work of enabling multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to enable.
   */
  static void handleMassEnable(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassEnable()");

    // The user must have permission to schedule jobs to access this section.
    if (! requestInfo.mayScheduleJob)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "enable jobs.");
      return;
    }


    // Get the important state information for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No jobs specified to enable.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }

    // See if the user has provided confirmation.  If so, then enable the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Enable Selected Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to enable the selected " +
                      "jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID, jobIDs[i],
                                   jobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_ENABLE) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_VIEW_CATEGORY,
                                     category) + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      for (int i=0; i < jobIDs.length; i++)
      {
        try
        {
          scheduler.enableJob(jobIDs[i]);
          infoMessage.append("Enabled job " + jobIDs[i] + ".<BR>" +
                             EOL);
        }
        catch (SLAMDServerException sse)
        {
          infoMessage.append("Unable to enable job " + jobIDs[i] + ":  " +
                             sse + "<BR>" + EOL);
        }
      }

      handleViewJob(requestInfo, category, null, null);
    }
    else
    {
      infoMessage.append("No jobs were enabled.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
    }
  }



  /**
   * Handles the work of exporting information about multiple jobs.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobIDs       The job IDs of the jobs to export.
   */
  static void handleMassExport(RequestInfo requestInfo, String[] jobIDs)
  {
    logMessage(requestInfo, "In handleMassExport()");

    // The user must have permission to export jobs to access this section.
    if (! requestInfo.mayExportJobData)
    {
      logMessage(requestInfo, "No mayViewJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "export job information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    String category =
         request.getParameter(Constants.SERVLET_PARAM_VIEW_CATEGORY);


    // Make sure that at least one job ID was specified.  If not, then print
    // an appropriate error message and go back to viewing the completed jobs.
    if ((jobIDs == null) || (jobIDs.length < 1))
    {
      infoMessage.append("You must specify at least one job to export.<BR>" +
                         EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }


    // Retrieve all of the jobs and see if they are all of the same type.  If
    // so, then we'll be able to be pretty specific about the kinds of data
    // that can be exported.  Otherwise, it will only be possible to choose
    // what gets exported in a more generic manner.
    ArrayList<ArrayList<Job>> jobTypeList = new ArrayList<ArrayList<Job>>();
    ArrayList<String> jobIDList = new ArrayList<String>();
    for (int i=0; i < jobIDs.length; i++)
    {
      Job job = null;
      try
      {
        job = configDB.getJob(jobIDs[i]);
        if (job == null)
        {
          infoMessage.append("Unable to retrieve job " + jobIDs[i] +
                             " -- job not found.<BR>" + EOL);
        }
        else
        {
          // Make sure that the job has statistics available.
          if (! job.hasStats())
          {
              infoMessage.append("Skipping job " + jobIDs[i] +
                                 " because it does not have any statistics " +
                                 "available.<BR>" + EOL);
              continue;
          }

          // Get the job type for this job and see if it is the same as any
          // of the other jobs that we have already seen.
          jobIDList.add(jobIDs[i]);
          boolean categorized = false;
          for (int j=0; j < jobTypeList.size(); j++)
          {
            ArrayList<Job> jobList = jobTypeList.get(j);
            Job job2 = jobList.get(0);
            if (job2.getJobClassName().equals(job.getJobClassName()))
            {
              jobList.add(job);
              categorized = true;
              break;
            }
          }
          if (! categorized)
          {
            ArrayList<Job> jobList = new ArrayList<Job>();
            jobList.add(job);
            jobTypeList.add(jobList);
          }
        }
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to retrieve job " + jobIDs[i] + " -- " + e +
                           "<BR>" + EOL);
      }
    }

    // Make sure that at least one job was placed in the job list.
    if (jobTypeList.isEmpty())
    {
      infoMessage.append("Unable to perform the requested comparison because " +
                         "there was not at least one job meeting the " +
                         "necessary criteria.<BR>" + EOL);
      handleViewJob(requestInfo, category, null, null);
      return;
    }


    jobIDs = new String[jobIDList.size()];
    jobIDList.toArray(jobIDs);


    // Sort the job information based on the actual start times for the jobs.
    // Since there should not be that many jobs to compare, and since it is
    // likely that they will already be sorted anyway, then a selection sort
    // should be the fastest and simplest way to do it.
    for (int i=0; i < jobTypeList.size(); i++)
    {
      ArrayList<Job> jobList = jobTypeList.get(i);
      long[]    startTimes = new long[jobList.size()];
      Job[]     jobs       = new Job[jobList.size()];
      for (int j=0; j < jobs.length; j++)
      {
        jobs[j]       = jobList.get(j);
        startTimes[j] = jobs[j].getActualStartTime().getTime();
      }
      for (int j=0; j < jobs.length; j++)
      {
        int  slot         = -1;
        long minStartTime = startTimes[j];

        for (int k=j+1; k < jobs.length; k++)
        {
          if (startTimes[k] < minStartTime)
          {
            slot         = k;
            minStartTime = startTimes[k];
          }
        }

        if (slot > 0)
        {
          Job  tempJob       = jobs[slot];
          long tempStartTime = startTimes[slot];

          jobs[slot]       = jobs[j];
          startTimes[slot] = startTimes[j];
          jobs[j]          = tempJob;
          startTimes[j]    = tempStartTime;
        }
      }

      jobList.clear();
      for (int j=0; j < jobs.length; j++)
      {
        jobList.add(jobs[j]);
      }
    }


    // Determine whether the user has chosen the kinds of information to be
    // exported.
    String confirmedStr =
         request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);

    if ((confirmedStr != null) &&
        confirmedStr.equals(Constants.CONFIG_VALUE_TRUE))
    {
      // Determine the kinds of information that will be included in the output.
      String value =
           request.getParameter(Constants.SERVLET_PARAM_EXPORT_JOB_ID);
      boolean includeJobID = (! ((value == null) || value.equals("0") ||
                                 value.equalsIgnoreCase("false") ||
                                 value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_INCLUDE_LABELS);
      boolean includeLabels = (! ((value == null) || value.equals("0") ||
                                  value.equalsIgnoreCase("false") ||
                                  value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_DESCRIPTION);
      boolean includeDescription = (! ((value == null) || value.equals("0") ||
                                       value.equalsIgnoreCase("false") ||
                                       value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_START_TIME);
      boolean includeStartTime = (! ((value == null) || value.equals("0") ||
                                     value.equalsIgnoreCase("false") ||
                                     value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_STOP_TIME);
      boolean includeStopTime = (! ((value == null) || value.equals("0") ||
                                    value.equalsIgnoreCase("false") ||
                                    value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_DURATION);
      boolean includeDuration = (! ((value == null) || value.equals("0") ||
                                    value.equalsIgnoreCase("false") ||
                                    value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_CLIENTS);
      boolean includeClients = (! ((value == null) || value.equals("0") ||
                                   value.equalsIgnoreCase("false") ||
                                   value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_THREADS);
      boolean includeThreads = (! ((value == null) || value.equals("0") ||
                                   value.equalsIgnoreCase("false") ||
                                   value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_INTERVAL);
      boolean includeInterval = (! ((value == null) || value.equals("0") ||
                                    value.equalsIgnoreCase("false") ||
                                    value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_PARAMETERS);
      boolean includeAllParams = (! ((value == null) || value.equals("0") ||
                                     value.equalsIgnoreCase("false") ||
                                     value.equalsIgnoreCase("off")));

      value = request.getParameter(Constants.SERVLET_PARAM_EXPORT_STATISTICS);
      boolean includeAllStats = (! ((value == null) || value.equals("0") ||
                                     value.equalsIgnoreCase("false") ||
                                     value.equalsIgnoreCase("off")));

      ArrayList<Parameter> includeParameters = new ArrayList<Parameter>();
      ArrayList<StatTracker> includeStats = new ArrayList<StatTracker>();
      if (jobTypeList.size() == 1)
      {
        ArrayList<Job> jobList = jobTypeList.get(0);
        Job         job     = jobList.get(0);
        Parameter[] stubs   = job.getParameterStubs().getParameters();
        for (int i=0; i < stubs.length; i++)
        {
          value =
               request.getParameter(
                    Constants.SERVLET_PARAM_EXPORT_PARAM_PREFIX +
                    stubs[i].getName());
          if (! ((value == null) || value.equals("0") ||
                 value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("off")))
          {
            includeParameters.add(stubs[i]);
          }
        }

        String[] trackerNames = job.getStatTrackerNames();
        for (int i=0; i < trackerNames.length; i++)
        {
          value =
               request.getParameter(Constants.SERVLET_PARAM_EXPORT_STAT_PREFIX +
                                    trackerNames[i]);
          if (! ((value == null) || value.equals("0") ||
                 value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("off")))
          {
            // There must be at least one job with this stat tracker in order to
            // export this information.
            for (int j=0; j < jobList.size(); j++)
            {
              StatTracker[] trackers =
                   jobList.get(j).getStatTrackers(trackerNames[i]);
              if ((trackers != null) && (trackers.length > 0))
              {
                includeStats.add(trackers[0]);
                break;
              }
            }
          }
        }
      }


      // Get the writer to use to send data to the client.
      PrintWriter writer = null;
      try
      {
        writer = requestInfo.response.getWriter();
      }
      catch (IOException ioe)
      {
        infoMessage.append("ERROR:  Unable to write the data -- " + ioe +
                           "<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Error Saving Data</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The attempt to save the data failed." + EOL);
        htmlBody.append("See the error message above for additional " +
                        "information");
        return;
      }

      // Indicate that the output from this will not be HTML.
      requestInfo.generateHTML = false;
      requestInfo.response.setContentType("application/x-slamd-job-export");
      requestInfo.response.addHeader("Content-Disposition",
                                     "filename=\"slamd_job_export_data.txt\"");


      // Iterate through all the different kinds of jobs to include in the
      // export.
      for (int i=0; i < jobTypeList.size(); i++)
      {
        // Get the job information as an array.
        ArrayList<Job> jobList = jobTypeList.get(i);
        Job[]     jobs    = new Job[jobList.size()];
        jobList.toArray(jobs);

        // If there are multiple types of jobs to retrieve, then get the types
        // of parameters to include for this particular job type.
        if (includeAllParams && (jobTypeList.size() > 1))
        {
          includeParameters.clear();
          Parameter[] stubs = jobs[0].getParameterStubs().getParameters();
          for (int j=0; j < stubs.length; j++)
          {
            if (! (stubs[j] instanceof PlaceholderParameter))
            {
              includeParameters.add(stubs[j]);
            }
          }
        }

        // If there are multiple types of jobs to retrieve, then get the types
        // of statistics to include for this particular job type.
        if (includeAllStats && (jobTypeList.size() > 1))
        {
          includeStats.clear();
          String[] trackerNames = jobs[0].getStatTrackerNames();
          for (int j=0; j < trackerNames.length; j++)
          {
            for (int k=0; k < jobs.length; k++)
            {
              StatTracker[] trackers = jobs[k].getStatTrackers(trackerNames[j]);
              if ((trackers != null) && (trackers.length > 0))
              {
                includeStats.add(trackers[0]);
                break;
              }
            }
          }
        }

        // If labels are to be included in the export, then send them out.
        if (includeLabels)
        {
          writer.println(jobs[0].getJobName() + " Job Data");

          if (includeJobID)
          {
            writer.print("Job ID\t");
          }

          if (includeDescription)
          {
            writer.print("Description\t");
          }

          if (includeStartTime)
          {
            writer.print("Start Time\t");
          }

          if (includeStopTime)
          {
            writer.print("Stop Time\t");
          }

          if (includeDuration)
          {
            writer.print("Duration\t");
          }

          if (includeClients)
          {
            writer.print("Number of Clients\t");
          }

          if (includeThreads)
          {
            writer.print("Threads per Client\t");
          }

          if (includeInterval)
          {
            writer.print("Collection Interval\t");
          }

          for (int j=0; j < includeParameters.size(); j++)
          {
            Parameter p = includeParameters.get(i);
            writer.print(p.getDisplayName() + '\t');
          }

          for (int j=0; j < includeStats.size(); j++)
          {
            StatTracker tracker = includeStats.get(j);
            String[] trackerLabels = tracker.getSummaryLabels();
            for (int k=0; k < trackerLabels.length; k++)
            {
              writer.print(trackerLabels[k] + '\t');
            }
          }

          writer.println();
        }


        // Write out the requested information for each job.
        for (int j=0; j < jobs.length; j++)
        {
          if (includeJobID)
          {
            writer.print(jobs[j].getJobID() + '\t');
          }

          if (includeDescription)
          {
            writer.print(jobs[j].getJobDescription() + '\t');
          }

          if (includeStartTime)
          {
            String formattedTime =
                 displayDateFormat.format(jobs[j].getActualStartTime());
            writer.print(formattedTime + '\t');
          }

          if (includeStopTime)
          {
            String formattedTime =
                 displayDateFormat.format(jobs[j].getActualStopTime());
            writer.print(formattedTime + '\t');
          }

          if (includeDuration)
          {
            writer.print(jobs[j].getActualDuration() + "\t");
          }

          if (includeClients)
          {
            writer.print(jobs[j].getNumberOfClients() + "\t");
          }

          if (includeThreads)
          {
            writer.print(jobs[j].getThreadsPerClient() + "\t");
          }

          if (includeInterval)
          {
            writer.print(jobs[j].getCollectionInterval() + "\t");
          }

          for (int k=0; k < includeParameters.size(); k++)
          {
            Parameter p = includeParameters.get(k);
            Parameter q = jobs[i].getParameterList().getParameter(p.getName());
            if (q == null)
            {
              writer.print("\t");
            }
            else
            {
              writer.print(q.getValueString() + '\t');
            }
          }

          for (int k=0; k < includeStats.size(); k++)
          {
            StatTracker tracker = includeStats.get(k);

            StatTracker[] trackers =
                 jobs[j].getStatTrackers(tracker.getDisplayName());
            if ((trackers == null) || (trackers.length == 0))
            {
              for (int l=0; l < tracker.getSummaryLabels().length; l++)
              {
                writer.print("\t");
              }
            }
            else
            {
              try
              {
                StatTracker t = trackers[0].newInstance();
                t.aggregate(trackers);
                String[] values = t.getSummaryData();
                for (int l=0; l < values.length; l++)
                {
                  writer.print(values[l] + '\t');
                }
              }
              catch (Exception e)
              {
                for (int l=0; l < tracker.getSummaryLabels().length; l++)
                {
                  writer.print("\t");
                }
              }
            }
          }

          writer.println();
        }

        writer.println();
      }
    }
    else
    {
      if (jobTypeList.size() == 1)
      {
        ArrayList<Job> jobList = jobTypeList.get(0);
        Job job = jobList.get(0);

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Export Multiple \"" + job.getJobName() +
                        "\" Jobs</SPAN>" + EOL);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Export Multiple SLAMD Jobs</SPAN>" + EOL);
      }

      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please select the information that should be included " +
                      "in the job data export." + EOL);

      if (jobTypeList.size() > 1)
      {
        htmlBody.append("Note that because multiple job types were selected, " +
                        "it is not possible to choose the individual " +
                        "parameter and statistic types that can be exported." +
                        EOL);
        htmlBody.append("Therefore, it is only possible to indicate whether " +
                        "to include all or no parameter information, and all " +
                        "or no statistical information." + EOL);
      }

      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\" CLASS=\"" + Constants.STYLE_MAIN_FORM + "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                     Constants.SUBMIT_STRING_EXPORT) + EOL);

      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_CONFIRMED,
                                     Constants.CONFIG_VALUE_TRUE) + EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <B>Job Schedule Information to Export</B><BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_JOB_ID +
                      "\" CHECKED>Job ID<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_DESCRIPTION +
                      "\" CHECKED>Job Description<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_START_TIME +
                      "\">Job Start Time<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_STOP_TIME +
                      "\">Job Stop Time<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_DURATION +
                      "\">Job Duration<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_CLIENTS +
                      "\">Number of Clients<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_THREADS +
                      "\">Number of Threads per Client<BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_INTERVAL +
                      "\">Statistics Collection Interval<BR>" + EOL);

      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <B>Job Parameter Information to Export</B><BR>" + EOL);
      if (jobTypeList.size() == 1)
      {
        ArrayList<Job> jobList = jobTypeList.get(0);
        Job job = jobList.get(0);
        Parameter[] stubs   = job.getParameterStubs().getParameters();
        for (int i=0; i < stubs.length; i++)
        {
          if (! (stubs[i] instanceof PlaceholderParameter))
          {
            htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                            Constants.SERVLET_PARAM_EXPORT_PARAM_PREFIX +
                            stubs[i].getName() + "\">" +
                            stubs[i].getDisplayName() + "<BR>" + EOL);
          }
        }
      }
      else
      {
        htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                        Constants.SERVLET_PARAM_EXPORT_PARAMETERS +
                        "\">Export all parameters<BR>" + EOL);
      }

      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <B>Statistical Information to Export</B><BR>" + EOL);
      if (jobTypeList.size() == 1)
      {
        ArrayList<Job> jobList = jobTypeList.get(0);
        Job job = jobList.get(0);
        String[]  trackerNames = job.getStatTrackerNames();
        for (int i=0; i < trackerNames.length; i++)
        {
          htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                          Constants.SERVLET_PARAM_EXPORT_STAT_PREFIX +
                          trackerNames[i] + "\" CHECKED>" + trackerNames[i] +
                          "<BR>" + EOL);
        }
      }
      else
      {
        htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                        Constants.SERVLET_PARAM_EXPORT_STATISTICS +
                        "\" CHECKED>Export all statistics<BR>" + EOL);
      }


      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_INCLUDE_LABELS +
                      "\" CHECKED>Include Labels in Exported Data<BR>" + EOL);
      htmlBody.append("  <BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Export Data\"><BR>" +
                      EOL);

      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of publishing or de-publishing one or more jobs for
   * display in restricted read-only mode.
   *
   * @param  requestInfo            The state information for this request.
   * @param  displayInReadOnlyMode  Indicates whether the specified jobs should
   *                                be published or de-published.
   */
  static void handleMassPublish(RequestInfo requestInfo,
                                boolean displayInReadOnlyMode)
  {
    logMessage(requestInfo, "In handleMassPublish()");

    // The user must have permission to manage folders to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "manage job folders.");
      return;
    }

    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    // Get the job IDs of the jobs to publish or de-publish.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("No action was taken because no jobs were selected." +
                         "<BR>" + EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
      return;
    }

    // Perform the update and create an info message with the updated status.
    for (int i=0; i < jobIDs.length; i++)
    {
      try
      {
        Job job = configDB.getJob(jobIDs[i]);
        job.setDisplayInReadOnlyMode(displayInReadOnlyMode);
        configDB.writeJob(job);
        infoMessage.append("Successfully updated job " + jobIDs[i] + "<BR>" +
                           EOL);
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to update job " + jobIDs[i] + ":  " +
                           e.getMessage() + "<BR>" + EOL);
      }
    }

    handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                  null, null);
  }



  /**
   * Handles all processing required to create a new job folder.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleCreateFolder(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleCreateFolder()");

    // The user must have permission to manage folders to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "create a new folder.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the folder name.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    if (((folderName != null) && (folderName.length() == 0)))
    {
      folderName = null;
    }

    // Determine whether to publish this folder in restricted read-only mode.
    boolean displayInReadOnlyMode = false;
    String displayStr =
         request.getParameter(Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY);
    if ((displayStr == null) || (displayStr.length() == 0))
    {
      displayInReadOnlyMode = false;
    }
    else
    {
      displayInReadOnlyMode = (displayStr.equalsIgnoreCase("true") ||
                               displayStr.equalsIgnoreCase("yes") ||
                               displayStr.equalsIgnoreCase("on") ||
                               displayStr.equalsIgnoreCase("1"));
    }

    String createString = "Create Folder";
    String cancelString = "Cancel";

    // See if the user has actually made a selection.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((folderName != null) && (confirmStr != null) &&
        (confirmStr.equalsIgnoreCase(createString)))
    {
      // The user has confirmed that the new folder should be created.
      try
      {
        JobFolder folder = new JobFolder(folderName, displayInReadOnlyMode,
                                         false, null, null, null, null,
                                         null, null, null);
        configDB.writeFolder(folder);
        infoMessage.append("Successfully created job folder " + folderName +
                           ".<BR>" + EOL);
      }
      catch (DatabaseException de)
      {
        requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                       de.getMessage());
        infoMessage.append("ERROR:  Unable to create job folder " + folderName +
                           " -- " + de + "<BR>" + EOL);
      }

      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
    }
    else if ((confirmStr != null) && confirmStr.equalsIgnoreCase(cancelString))
    {
      // The folder creation has been cancelled.
      infoMessage.append("No job folder was created.<BR>" + EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
    }
    else
    {
      // Display a form that allows the user to create a new job folder.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Create a New Job Folder</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                     Constants.SUBMIT_STRING_CREATE_FOLDER) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><SPAN CLASS=\"" +
                      Constants.STYLE_FORM_CAPTION +
                      "\">New Folder Name</SPAN></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER +
                      "\" SIZE=\"40\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Display In Restricted Read-Only Mode</TD>" +
                      EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_DISPLAY_IN_READ_ONLY + '"' +
                      (displayInReadOnlyMode ? " CHECKED" : "") + "></TD>" +
                      EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      createString + "\">" + EOL);
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      cancelString + "\">" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing required to remove a job folder.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDeleteFolder(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDeleteFolder()");

    // The user must have permission to schedule jobs to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "delete job folders.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the folder name.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    if (((folderName != null) && (folderName.length() == 0)))
    {
      folderName = null;
    }
    else if ((folderName != null) &&
             folderName.equals(Constants.FOLDER_NAME_UNCLASSIFIED))
    {
      infoMessage.append("You cannot delete the " +
                         Constants.FOLDER_NAME_UNCLASSIFIED +
                         " job folder.<BR>" + EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    Constants.FOLDER_NAME_UNCLASSIFIED, null);
      return;
    }

    String deleteString = "Delete Folder";
    String cancelString = "Cancel";

    // See if the user has actually made a selection.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((folderName != null) && (confirmStr != null) &&
        (confirmStr.equalsIgnoreCase(deleteString)))
    {
      // The user has confirmed that the new folder should be created.
      // Determine whether to delete the folder contents as well.
      boolean deleteContents = false;
      String deleteContentsStr =
           request.getParameter(Constants.SERVLET_PARAM_DELETE_FOLDER_CONTENTS);
      if (deleteContentsStr != null)
      {
        deleteContents = (deleteContentsStr.equalsIgnoreCase("true") ||
                          deleteContentsStr.equalsIgnoreCase("yes") ||
                          deleteContentsStr.equalsIgnoreCase("on") ||
                          deleteContentsStr.equalsIgnoreCase("1"));
      }

      try
      {
        configDB.removeFolder(folderName, deleteContents);
        infoMessage.append("Successfully deleted job folder " + folderName +
                           ".<BR>" + EOL);
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      Constants.FOLDER_NAME_UNCLASSIFIED, null);
      }
      catch (DatabaseException de)
      {
        String message = "Unable to delete job folder " + folderName + " -- " +
                         de.getMessage();
        infoMessage.append("ERROR:  " + message + "<BR>" + EOL);
        requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                       message);
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
      }
    }
    else if ((folderName != null) && (confirmStr != null) &&
             (confirmStr.equalsIgnoreCase(cancelString)))
    {
      // The folder creation has been cancelled.
      infoMessage.append("No job folder was deleted.<BR>" + EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
    }
    else
    {
      // Get the list of available job folders.
      JobFolder[] folders = null;
      try
      {
        folders = configDB.getFolders();
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Could not retrieve the list of job " +
                           "folders -- " + de + "<BR>" + EOL);
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
        return;
      }

      if ((folders == null) || (folders.length <= 1))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Delete a Job Folder</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("No custom job folders have been defined in the " +
                        "SLAMD server." + EOL);
        return;
      }

      // Display a form that allows the user to delete a job folder.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Delete a Job Folder</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please choose the job folder to delete." + EOL);
      htmlBody.append("Note that only empty folders may be removed." + EOL);
      htmlBody.append("To remove a folder that is not empty, first delete " +
                      "or move any jobs contained in that folder." + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                     Constants.SUBMIT_STRING_DELETE_FOLDER) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Folder to Delete</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        if (folders[i].getFolderName().equals(
                                            Constants.FOLDER_NAME_UNCLASSIFIED))
        {
          continue;
        }

        htmlBody.append("          <OPTION VALUE=\"" +
                        folders[i].getFolderName() + "\">" +
                        folders[i].getFolderName() + EOL);
      }

      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Delete Folder Contents</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_DELETE_FOLDER_CONTENTS +
                      "\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      deleteString + "\">" + EOL);
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      cancelString + "\">" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing required to move one or more jobs to a specified
   * job folder.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleMoveJobs(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleMoveJobs()");

    // The user must have permission to schedule jobs to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "move jobs.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Get the job IDs of the jobs to move.  If there were none, then display
    // an error message.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("ERROR:  No jobs selected to move.<BR>" + EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
      return;
    }

    // Get the name of the folder into which the jobs are to be moved.
    String folderName =
        request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
    if ((folderName != null) && (folderName.length() == 0))
    {
      folderName = null;
    }

    String moveStr   = "Move Jobs";
    String cancelStr = "Cancel";

    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);

    // See if the user has confirmed that the job information should be moved.
    if ((folderName != null) && (confirmStr != null) &&
        (confirmStr.equalsIgnoreCase(moveStr)))
    {
      // The user wants to move the jobs, so do it.
      for (int i=0; i < jobIDs.length; i++)
      {
        try
        {
          configDB.moveJob(jobIDs[i], folderName);
          infoMessage.append("Successfully moved job " + jobIDs[i] + "<BR>" +
                             EOL);
        }
        catch (DatabaseException de)
        {
          infoMessage.append("ERROR while moving job " + jobIDs[i] + ":  " +
                             de + "<BR>" + EOL);
        }
      }

      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
    }
    else if ((folderName != null) && (confirmStr != null) &&
             (confirmStr.equalsIgnoreCase(cancelStr)))
    {
      infoMessage.append("The selected jobs were not moved.<BR>" + EOL);
      handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                    null, null);
    }
    else
    {
      // Get the list of available job folders.
      JobFolder[] folders = null;
      try
      {
        folders = configDB.getFolders();
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Could not retrieve the list of job " +
                           "folders -- " + de + "<BR>" + EOL);
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
        return;
      }

      if (folders.length == 0)
      {
        infoMessage.append("ERROR:  There are no custom folders defined.<BR>" +
                           EOL);
        handleViewJob(requestInfo, Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                      null, null);
        return;
      }

      // Display a form that allows the user to choose the new folder.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Move Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please specify the new folder for the following jobs." +
                      EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);
      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  <LI>" + jobIDs[i] + "</LI>" + EOL);
      }
      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                     Constants.SERVLET_SECTION_JOB_MASS_OP) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                     Constants.SUBMIT_STRING_MOVE) +
                      EOL);
      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append(' ' +
                        generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                       jobIDs[i]) + EOL);
      }
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><SPAN CLASS=\"" +
                      Constants.STYLE_FORM_CAPTION +
                      "\">New Folder Name</SPAN></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);

      for (int i=0; i < folders.length; i++)
      {
        htmlBody.append("          <OPTION VALUE=\"" +
                        folders[i].getFolderName() + "\">" +
                        folders[i].getFolderName() + EOL);
      }

      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      moveStr + "\">" + EOL);
      htmlBody.append("        <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      cancelStr + "\">" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of adding a set of jobs to a virtual job folder.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleAddToVirtualFolder(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleAddToVirtualFolder()");

    // The user must have permission to schedule jobs to do anything in this
    // section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayScheduleJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "modify virtual job folders");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Make sure that at least one job ID was provided.  If not, then display
    // an error.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_ID);
    if ((jobIDs == null) || (jobIDs.length == 0))
    {
      infoMessage.append("ERROR:  No jobs specified<BR>" + EOL);
      htmlBody.append("You must specify at least one job to be added to a " +
                      "virtual folder." + EOL);
      return;
    }


    // See if a folder name was provided.  If so, then try to add the specified
    // jobs to the folder.  If not, then prompt the user for a folder name.
    String folderName =
         request.getParameter(Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER);
    if ((folderName != null) &&
        folderName.equals(Constants.FOLDER_NAME_CREATE_NEW_FOLDER))
    {
      folderName =
           request.getParameter(Constants.SERVLET_PARAM_NEW_FOLDER_NAME);
    }

    if ((folderName != null) && (folderName.length() > 0))
    {
      // Add the specified list of job IDs to the virtual folder, or create a
      // new folder if necessary.
      try
      {
        boolean created = false;
        JobFolder folder = configDB.getVirtualFolder(folderName);
        if (folder == null)
        {
          folder = new JobFolder(folderName, false, true, null, null, null,
                                 jobIDs, null, null, null);
          created = true;
        }
        else
        {
          for (int i=0; i < jobIDs.length; i++)
          {
            folder.addJobID(jobIDs[i]);
          }
        }

        configDB.writeVirtualFolder(folder);

        if (created)
        {
          infoMessage.append("Successfully created virtual job folder \"" +
                             folderName + "\"<BR>" + EOL);
        }
        else
        {
          infoMessage.append("Successfully updated virtual job folder \"" +
                             folderName + "\"<BR>" +  EOL);
        }
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to update virtual job folder \"" +
                           folderName + "\" -- " + e.getMessage() + "<BR>" +
                           EOL);
      }

      handleViewVirtualFolder(requestInfo, folderName);
      return;
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Add Jobs to a Virtual Folder</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);


      // Get the list of virtual job folders that are defined in the SLAMD
      // server.
      JobFolder[] folders;
      try
      {
        folders = configDB.getVirtualFolders();
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Unable to retrieve the list of virtual " +
                           "job folders -- " + de.getMessage() + "<BR>" + EOL);
        htmlBody.append("An error occurred while attempting to retrieve the " +
                        "list of virtual job folders that have been defined " +
                        "in the SLAMD server." + EOL);
        return;
      }


      htmlBody.append("Choose the virtual folder to which the jobs are to be " +
                      "added, or choose to create a new virtual job folder" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OP) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                           Constants.SUBMIT_STRING_ADD_TO_VIRTUAL_FOLDER) +
                      EOL);

      for (int i=0; i < jobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_ID,
                                              jobIDs[i]) + EOL);
      }

      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") +
                        EOL);
      }


      if ((folders != null) && (folders.length > 0))
      {
        htmlBody.append("  <SELECT NAME=\"" +
                        Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER + "\">" +
                        EOL);
        htmlBody.append("    <OPTION VALUE=\"" +
                        Constants.FOLDER_NAME_CREATE_NEW_FOLDER + "\">" +
                        Constants.FOLDER_NAME_CREATE_NEW_FOLDER + EOL);
        for (int i=0; i < folders.length; i++)
        {
          htmlBody.append("    <OPTION VALUE=\"" + folders[i].getFolderName() +
                          "\">" + folders[i].getFolderName() + EOL);
        }
        htmlBody.append("  </SELECT>" + EOL);
        htmlBody.append("  <BR>" + EOL);
      }
      else
      {
        htmlBody.append("  " +
                        generateHidden(
                             Constants.SERVLET_PARAM_VIRTUAL_JOB_FOLDER,
                             Constants.FOLDER_NAME_CREATE_NEW_FOLDER) + EOL);
        htmlBody.append("  No virtual job folders have been defined in " +
                        "the SLAMD server.<BR>" + EOL);
      }


      htmlBody.append("  New Virtual Folder Name:  " + EOL);
      htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_NEW_FOLDER_NAME +
                      "\" SIZE=\"40\">" + EOL);
      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Add Jobs\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work of performing some operation on multiple optimizing jobs
   * at the same time.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleMassOptimizing(RequestInfo requestInfo)
  {
    HttpServletRequest request = requestInfo.request;


    // Get the job IDs of the optimizing jobs on which to perform the operation.
    String[] jobIDs =
         request.getParameterValues(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID);

    // Determine which mass operation the user wishes to perform.
    String opString = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if (opString == null)
    {
      return;
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_SELECT_ALL) ||
             opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DESELECT_ALL))
    {
      handleViewOptimizing(requestInfo, true);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DELETE))
    {
      handleMassOptimizingDelete(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_MOVE))
    {
      handleMassOptimizingMove(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_PUBLISH_JOBS))
    {
      handleMassOptimizingPublish(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_DEPUBLISH_JOBS))
    {
      handleMassOptimizingDepublish(requestInfo, jobIDs);
    }
    else if (opString.equalsIgnoreCase(Constants.SUBMIT_STRING_GENERATE_REPORT))
    {
      handleGenerateReport(requestInfo);
    }
  }



  /**
   * Handles the work of deleting multiple optimizing jobs at the same time.
   *
   * @param  requestInfo       The state information for this request.
   * @param  optimizingJobIDs  The job IDs of the optimizing jobs to be
   *                           deleted.
   */
  static void handleMassOptimizingDelete(RequestInfo requestInfo,
                                         String[] optimizingJobIDs)
  {
    logMessage(requestInfo, "In handleMassOptimizingDelete()");

    // The user must have permission to delete jobs to access this section.
    if (! requestInfo.mayDeleteJob)
    {
      logMessage(requestInfo, "No mayDeleteJob permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "delete job information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       infoMessage    = requestInfo.infoMessage;
    StringBuilder       htmlBody       = requestInfo.htmlBody;


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((optimizingJobIDs == null) || (optimizingJobIDs.length == 0))
    {
      infoMessage.append("No optimizing jobs specified to delete.<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }

    // See if the user has provided confirmation.  If so, then delete the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Delete Selected Optimizing Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure that you want to delete the selected " +
                      "optimizing jobs?" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        String link =
             generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                          Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                          Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                          optimizingJobIDs[i], optimizingJobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_DELETE) +
                      EOL);

      String folderName =
           request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);
      if (folderName != null)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_JOB_FOLDER,
                                       folderName) + EOL);
      }

      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(
                                    Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                    optimizingJobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
           Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS +
           "\" CHECKED>Delete all iterations of the optimizing jobs" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"5\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        try
        {
          boolean includeIterations = false;
          String includeStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS);
          includeIterations = ((includeStr != null) &&
                               (includeStr.equalsIgnoreCase("true") ||
                                includeStr.equalsIgnoreCase("yes") ||
                                includeStr.equalsIgnoreCase("on") ||
                                includeStr.equalsIgnoreCase("1")));
          if (includeIterations)
          {
            OptimizingJob optimizingJob = getOptimizingJob(optimizingJobIDs[i]);
            if (optimizingJob == null)
            {
              infoMessage.append("ERROR:  Unable to retrieve optimizing job " +
                                 optimizingJobIDs[i] + "<BR>" + EOL);
              handleViewOptimizing(requestInfo, true);
              return;
            }

            Job[] iterations = optimizingJob.getAssociatedJobs();
            if ((iterations != null) && (iterations.length > 0))
            {
              for (int j=0; j < iterations.length; j++)
              {
                try
                {
                  configDB.removeJob(iterations[j].getJobID());
                }
                catch (Exception e)
                {
                  infoMessage.append("Unable to remove optimizing job " +
                                     "iteration " + iterations[j].getJobID() +
                                     " -- " + e);
                }
              }
            }

            Job reRunIteration = optimizingJob.getReRunIteration();
            if (reRunIteration != null)
            {
              try
              {
                configDB.removeJob(reRunIteration.getJobID());
              }
              catch (Exception e)
              {
                infoMessage.append("Unable to remove optimizing job re-run " +
                                   "iteration " + reRunIteration.getJobID() +
                                   " -- " + e);
              }
            }
          }

          configDB.removeOptimizingJob(optimizingJobIDs[i]);
          infoMessage.append("Deleted optimizing job " + optimizingJobIDs[i] +
                             ".<BR>" + EOL);
        }
        catch (Exception e)
        {
          infoMessage.append("Unable to delete optimizing job " +
                             optimizingJobIDs[i] + ":  " + e + "<BR>" + EOL);
        }
      }

      handleViewOptimizing(requestInfo, true);
    }
    else
    {
      infoMessage.append("No optimizing jobs were deleted.<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
    }
  }



  /**
   * Handles the work of moving multiple optimizing jobs.
   *
   * @param  requestInfo       The state information for this request.
   * @param  optimizingJobIDs  The job IDs of the optimizing jobs to be moved.
   */
  static void handleMassOptimizingMove(RequestInfo requestInfo,
                                       String[] optimizingJobIDs)
  {
    logMessage(requestInfo, "In handleMassOptimizingMove()");

    // The user must have permission to manage folders to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "move job information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       infoMessage    = requestInfo.infoMessage;
    StringBuilder       htmlBody       = requestInfo.htmlBody;


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((optimizingJobIDs == null) || (optimizingJobIDs.length == 0))
    {
      infoMessage.append("No optimizing jobs specified to move.<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }

    // See if the user has provided confirmation.  If so, then delete the jobs
    // or don't based on the results of the confirmation.  If no confirmation
    // has been provided, then request it.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("Move Jobs")) &&
         (! confirmStr.equalsIgnoreCase("Cancel"))))
    {
      // Get the set of job folders defined in the configuration directory.  If
      // there are none, then return an error.
      JobFolder[] folders    = null;
      String      failReason = "No job folders have been created.";
      try
      {
        folders = configDB.getFolders();
      }
      catch (DatabaseException de)
      {
        failReason = de.getMessage();
      }

      if ((folders == null) || (folders.length == 0))
      {
        infoMessage.append("Unable to move the selected optimizing jobs:  " +
                           failReason + "<BR>" + EOL);
        handleViewOptimizing(requestInfo, true);
        return;
      }

      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Move Selected Optimizing Jobs</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please specify the destination folder for the " +
                      "optimizing jobs" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<UL>" + EOL);

      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        String link =
             generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                          Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                          Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                          optimizingJobIDs[i], optimizingJobIDs[i]);
        htmlBody.append("  <LI>" + link + "</LI>" + EOL);
      }

      htmlBody.append("</UL>" + EOL);
      htmlBody.append("<BR>" + EOL);

      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SECTION,
                                     Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_MASS_OPTIMIZING) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBMIT,
                                            Constants.SUBMIT_STRING_MOVE) +
                      EOL);

      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        htmlBody.append("  " + generateHidden(
                                    Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                                    optimizingJobIDs[i]) + EOL);
      }


      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"5\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD>Destination Folder</TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>" + EOL);
      htmlBody.append("        <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\">" + EOL);
      for (int i=0; i < folders.length; i++)
      {
        htmlBody.append("          <OPTION VALUE=\"" +
                        folders[i].getFolderName() + "\">" +
                        folders[i].getFolderName() + EOL);
      }
      htmlBody.append("        </SELECT>" + EOL);
      htmlBody.append("      </TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" +
           Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS +
           "\" CHECKED></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD>Move all iterations of the optimizing " +
                      "jobs</TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Move Jobs\"></TD>" + EOL);
      htmlBody.append("      <TD>&nbsp;</TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Cancel\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("Move Jobs"))
    {
      String folderName =
                  request.getParameter(Constants.SERVLET_PARAM_JOB_FOLDER);

      for (int i=0; i < optimizingJobIDs.length; i++)
      {
        try
        {
          boolean includeIterations = false;
          String includeStr =
               request.getParameter(
                    Constants.SERVLET_PARAM_OPTIMIZING_JOB_INCLUDE_ITERATIONS);
          includeIterations = ((includeStr != null) &&
                               (includeStr.equalsIgnoreCase("true") ||
                                includeStr.equalsIgnoreCase("yes") ||
                                includeStr.equalsIgnoreCase("on") ||
                                includeStr.equalsIgnoreCase("1")));
          if (includeIterations)
          {
            OptimizingJob optimizingJob = getOptimizingJob(optimizingJobIDs[i]);
            if (optimizingJob == null)
            {
              infoMessage.append("ERROR:  Unable to retrieve optimizing job " +
                                 optimizingJobIDs[i] + "<BR>" + EOL);
              handleViewOptimizing(requestInfo, true);
              return;
            }

            Job[] iterations = optimizingJob.getAssociatedJobs();
            if ((iterations != null) && (iterations.length > 0))
            {
              for (int j=0; j < iterations.length; j++)
              {
                try
                {
                  configDB.moveJob(iterations[j].getJobID(), folderName);
                }
                catch (Exception e)
                {
                  infoMessage.append("Unable to move optimizing job " +
                                     "iteration " + iterations[j].getJobID() +
                                     " -- " + e);
                }
              }
            }

            Job reRunIteration = optimizingJob.getReRunIteration();
            if (reRunIteration != null)
            {
              try
              {
                configDB.moveJob(reRunIteration.getJobID(), folderName);
              }
              catch (Exception e)
              {
                infoMessage.append("Unable to move optimizing job re-run " +
                                   "iteration " + reRunIteration.getJobID() +
                                   " -- " + e);
              }
            }
          }

          configDB.moveOptimizingJob(optimizingJobIDs[i], folderName);
          infoMessage.append("Moved optimizing job " + optimizingJobIDs[i] +
                             ".<BR>" + EOL);
        }
        catch (Exception e)
        {
          infoMessage.append("Unable to move optimizing job " +
                             optimizingJobIDs[i] + ":  " + e + "<BR>" + EOL);
        }
      }

      handleViewOptimizing(requestInfo, true);
    }
    else
    {
      infoMessage.append("No optimizing jobs were moved.<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
    }
  }



  /**
   * Handles the work of publishing information about multiple optimizing jobs.
   *
   * @param  requestInfo       The state information for this request.
   * @param  optimizingJobIDs  The job IDs of the optimizing jobs to be
   *                           published.
   */
  static void handleMassOptimizingPublish(RequestInfo requestInfo,
                                          String[] optimizingJobIDs)
  {
    logMessage(requestInfo, "In handleMassOptimizingPublish()");

    // The user must have permission to manage folders to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "publish job information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder infoMessage = requestInfo.infoMessage;


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((optimizingJobIDs == null) || (optimizingJobIDs.length == 0))
    {
      infoMessage.append("No optimizing jobs specified to publish.<BR>" + EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }

    // Iterate through the jobs and mark each for publication.
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      try
      {
        OptimizingJob optimizingJob = getOptimizingJob(optimizingJobIDs[i]);
        optimizingJob.setDisplayInReadOnlyMode(true);
        configDB.writeOptimizingJob(optimizingJob);
        infoMessage.append("Marked optimizing job " + optimizingJobIDs[i] +
                           " for publication.<BR>" + EOL);
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to publish optimizing job " +
                           optimizingJobIDs[i] + ":  " + e + "<BR>" + EOL);
      }
    }

    handleViewOptimizing(requestInfo, true);
  }



  /**
   * Handles the work of de-publishing information about multiple optimizing
   * jobs.
   *
   * @param  requestInfo       The state information for this request.
   * @param  optimizingJobIDs  The job IDs of the optimizing jobs to be
   *                           de-published.
   */
  static void handleMassOptimizingDepublish(RequestInfo requestInfo,
                                            String[] optimizingJobIDs)
  {
    logMessage(requestInfo, "In handleMassOptimizingPublish()");

    // The user must have permission to manage folders to access this section.
    if (! requestInfo.mayManageFolders)
    {
      logMessage(requestInfo, "No mayManageFolders permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "de-publish job information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // Make sure that at least one job ID was specified.  If not, then print an
    // error message and view the appropriate set of jobs.
    if ((optimizingJobIDs == null) || (optimizingJobIDs.length == 0))
    {
      infoMessage.append("No optimizing jobs specified to de-publish.<BR>" +
                         EOL);
      handleViewOptimizing(requestInfo, true);
      return;
    }

    // Iterate through the jobs and mark each for de-publication.
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      try
      {
        OptimizingJob optimizingJob = getOptimizingJob(optimizingJobIDs[i]);
        optimizingJob.setDisplayInReadOnlyMode(false);
        configDB.writeOptimizingJob(optimizingJob);
        infoMessage.append("Marked optimizing job " + optimizingJobIDs[i] +
                           " for de-publication.<BR>" + EOL);
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to de-publish optimizing job " +
                           optimizingJobIDs[i] + ":  " + e + "<BR>" + EOL);
      }
    }

    handleViewOptimizing(requestInfo, true);
  }



  /**
   * Handles all processing required to import data into the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDataImport(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDataImport()");

    // The user must have export job data permission to see anything here.
    if (! requestInfo.mayExportJobData)
    {
      logMessage(requestInfo, "No mayExportJobData permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "import job data");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest  request        = requestInfo.request;
    HttpServletResponse response       = requestInfo.response;
    String              servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder        htmlBody       = requestInfo.htmlBody;
    StringBuilder        infoMessage    = requestInfo.infoMessage;


    // See if the user has told us where the import file is.  If so, then
    // process it.  If not, then ask them for the location.
    String importFile =
         request.getParameter(Constants.SERVLET_PARAM_DATA_IMPORT_FILE);
    if ((importFile == null) || (importFile.length() == 0))
    {
      // We don't know where the import file is.  Ask the user.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Import Job Data</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Please provide the absolute path (on the SLAMD server " +
                      "system) to the location of the file containing the " +
                      "data to import." + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_IMPORT_JOB_DATA) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  Import File:  "  + EOL);
      htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_DATA_IMPORT_FILE +
                      "\" SIZE=\"80\">" + EOL);
      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Import Data\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else
    {
      // We have the import file, so we can process it.  However, we have to do
      // some trickery here in order to give the user feedback in real-time so
      // they can see that the import actually is going on.  Therefore, we will
      // generate all the HTML ourselves, including the header, the sidebar, and
      // the copyright notice.
      PrintWriter writer = null;
      try
      {
        writer = response.getWriter();
      }
      catch (IOException ioe)
      {
        infoMessage.append("Unable to obtain output writer -- " + ioe +
                           "<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Import Failed</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("An error occurred that prevented the import from " +
                        "being processed successfully.");
        return;
      }

      requestInfo.generateHTML = false;
      response.setContentType("text/html");


      // Generate the HTML header and print it out.  Then replace the HTML
      // body with an empty string buffer.
      writer.println(generateHTMLHeader(requestInfo));
      writer.flush();


      // Hand the import off to the config handler to process it.
      FileInputStream inputStream = null;
      try
      {
        writer.println("<!-- START IMPORT -->");
        inputStream = new FileInputStream(importFile);
        configDB.importFolderData(inputStream, writer, true);
        writer.println("<!-- END IMPORT -->");
      }
      catch (Exception e)
      {
        writer.println("<!-- END IMPORT -->");
        writer.println("<!-- IMPORT ERROR:  " +
                       JobClass.stackTraceToString(e) + " -->");
        writer.println("Unexpected error processing import file:  " +
                       JobClass.stackTraceToString(e));
        writer.flush();

        try
        {
          inputStream.close();
        } catch (Exception e2) {}
      }


      // Generate the copyright notice and print it out.
      writer.println(generateHTMLFooter(requestInfo));
      writer.flush();
    }
  }



  /**
   * Handles all processing required to export data from the SLAMD server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDataExport(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDataExport()");

    // The user must have export job data permission to see anything here.
    if (! requestInfo.mayExportJobData)
    {
      logMessage(requestInfo, "No mayExportJobData permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "export job data");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest  request        = requestInfo.request;
    HttpServletResponse response       = requestInfo.response;
    String              servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder        htmlBody       = requestInfo.htmlBody;
    StringBuilder        infoMessage    = requestInfo.infoMessage;


    // See if the user has selected which information to export.
    String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
    if ((submitStr != null) && submitStr.equals(Constants.SUBMIT_STRING_EXPORT))
    {
      // The user wants to actually export the data.  Figure out what that
      // includes and do the export.
      boolean exportAll = false;
      String exportChoice =
           request.getParameter(Constants.SERVLET_PARAM_EXPORT_CHOICE);
      if ((exportChoice != null) &&
          exportChoice.equals(Constants.EXPORT_CHOICE_ALL))
      {
        exportAll = true;
      }

      ArrayList<String> realList = new ArrayList<String>();
      ArrayList<String> virtualList  = new ArrayList<String>();
      ArrayList<String> jobGroupList = new ArrayList<String>();
      if (! exportAll)
      {
        String[] realArray = request.getParameterValues(
                                  Constants.SERVLET_PARAM_EXPORT_REAL_FOLDER);
        for (int i=0; ((realArray != null) && (i < realArray.length)); i++)
        {
          realList.add(realArray[i]);
        }

        String[] virtualArray =
             request.getParameterValues(
                  Constants.SERVLET_PARAM_EXPORT_VIRTUAL_FOLDER);
        for (int i=0; ((virtualArray != null) && (i < virtualArray.length));
             i++)
        {
          virtualList.add(virtualArray[i]);
        }

        String[] jobGroupArray =
             request.getParameterValues(
                  Constants.SERVLET_PARAM_EXPORT_JOB_GROUP);
        for (int i=0; ((jobGroupArray != null) && (i < jobGroupArray.length));
             i++)
        {
          jobGroupList.add(jobGroupArray[i]);
        }
      }
      else
      {
        try
        {
          JobFolder[] realFolders    = configDB.getFolders();
          JobFolder[] virtualFolders = configDB.getVirtualFolders();
          JobGroup[]  jobGroups      = configDB.getSummaryJobGroups();
          for (int i=0; i < realFolders.length; i++)
          {
            realList.add(realFolders[i].getFolderName());
          }
          for (int i=0; i < virtualFolders.length; i++)
          {
            virtualList.add(virtualFolders[i].getFolderName());
          }
          for (int i=0; i < jobGroups.length; i++)
          {
            jobGroupList.add(jobGroups[i].getName());
          }
        }
        catch (DatabaseException de)
        {
          infoMessage.append("Unable to retrieve the list of available " +
                             "folders -- " + de + "<BR>" + EOL);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Export Failed</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("An error occurred that prevented the export from " +
                          "being processed successfully.");
          return;
        }
      }


      OutputStream outputStream;
      try
      {
        outputStream = response.getOutputStream();
      }
      catch (IOException ioe)
      {
        infoMessage.append("Unable to obtain output writer -- " + ioe +
                           "<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Export Failed</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("An error occurred that prevented the export from " +
                        "being processed successfully.");
        return;
      }


      // From this point on, we are committed to actually generating the output
      // file.  If an error occurs, then there's not much that can be done
      // about it.  We'll log it to the SLAMD log but we don't really get
      // much
      requestInfo.generateHTML = false;
      requestInfo.response.setContentType("application/x-slamd-data-export");
      requestInfo.response.addHeader("Content-Disposition",
                                     "filename=\"slamd_data_export_data." +
                                     dateFormat.format(new Date()) + '"');

      String[] realFolderNames = new String[realList.size()];
      realList.toArray(realFolderNames);

      String[] virtualFolderNames = new String[virtualList.size()];
      virtualList.toArray(virtualFolderNames);

      String[] jobGroupNames = new String[jobGroupList.size()];
      jobGroupList.toArray(jobGroupNames);

      try
      {
        configDB.exportFolderData(realFolderNames, virtualFolderNames,
                                  jobGroupNames, outputStream);
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_ANY,
                               "Export of SLAMD server data failed:  " + e);
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
      }
    }
    else if ((submitStr != null) &&
             submitStr.equals(Constants.SUBMIT_STRING_CANCEL))
    {
      // The user cancelled the export.
      infoMessage.append("Job data export cancelled<BR>" + EOL);
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Export Cancelled</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The job data export was cancelled.");
    }
    else
    {
      // The user has not yet specified what to export.  Show them a form to
      // allow them to choose what they want included.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Export Job Data</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Choose which information you wish to export");
      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_EXPORT_JOB_DATA) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <SELECT NAME=\"" +
                      Constants.SERVLET_PARAM_EXPORT_CHOICE + "\">" + EOL);
      htmlBody.append("    <OPTION VALUE=\"" + Constants.EXPORT_CHOICE_ALL +
                      "\">" + Constants.EXPORT_CHOICE_ALL + EOL);
      htmlBody.append("    <OPTION VALUE=\"" +
                      Constants.EXPORT_CHOICE_SELECTED + "\">" +
                      Constants.EXPORT_CHOICE_SELECTED + EOL);
      htmlBody.append("  </SELECT>" + EOL);
      htmlBody.append("  <BR><BR>" + EOL);

      try
      {
        JobFolder[] realFolders = configDB.getFolders();
        for (int i=0; i < realFolders.length; i++)
        {
          htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                          Constants.SERVLET_PARAM_EXPORT_REAL_FOLDER +
                          "\" VALUE=\"" + realFolders[i].getFolderName() +
                          "\">Real Job Folder \"" +
                          realFolders[i].getFolderName() + "\"<BR>" + EOL);
        }
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Unable to retrieve the list of defined " +
                           "real job folders -- " + de + "<BR>" + EOL);
      }

      try
      {
        JobFolder[] virtualFolders = configDB.getVirtualFolders();
        for (int i=0; i < virtualFolders.length; i++)
        {
          htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                          Constants.SERVLET_PARAM_EXPORT_VIRTUAL_FOLDER +
                          "\" VALUE=\"" + virtualFolders[i].getFolderName() +
                          "\">Virtual Job Folder \"" +
                          virtualFolders[i].getFolderName() + "\"<BR>" + EOL);
        }
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Unable to retrieve the list of defined " +
                           "virtual job folders -- " + de + "<BR>" + EOL);
      }

      try
      {
        JobGroup[] jobGroups = configDB.getSummaryJobGroups();
        for (int i=0; i < jobGroups.length; i++)
        {
          htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                          Constants.SERVLET_PARAM_EXPORT_JOB_GROUP +
                          "\" VALUE=\"" + jobGroups[i].getName() +
                          "\">Job Group \"" + jobGroups[i].getName() +
                          "\"<BR>" + EOL);
        }
      }
      catch (DatabaseException de)
      {
        infoMessage.append("ERROR:  Unable to retrieve the list of defined " +
                           "job groups -- " + de + "<BR>" + EOL);
      }

      htmlBody.append("  <BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_EXPORT + "\">" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_CANCEL + "\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles all processing require to migrate job data from a SLAMD 1.x-style
   * configuration directory into the new configuration database.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleMigrateData(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleMigrateData()");

    // The user must have export permission to see anything here.
    if (! requestInfo.mayExportJobData)
    {
      logMessage(requestInfo, "No mayExportJobData permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "migrate job data.");
    }


    // Get the important state variables for this request.
    HttpServletRequest  request        = requestInfo.request;
    HttpServletResponse response       = requestInfo.response;
    String              servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder        htmlBody       = requestInfo.htmlBody;
    StringBuilder        infoMessage    = requestInfo.infoMessage;


    // Create parameters that may be used to specify information about the
    // directory server from which to migrate the data.
    BooleanParameter useSSLParameter =
         new BooleanParameter("use_ssl", "Use SSL",
                              "Indicates whether to use SSL to communicate " +
                              "with the directory server.", false);
    IntegerParameter portParameter =
         new IntegerParameter("ldap_port", "Directory Server Port",
                              "The port of the directory server containing " +
                              "the data to be migrated.", true, 389, true, 1,
                              true, 65535);
    PasswordParameter bindPWParameter =
         new PasswordParameter("bind_pw", "Bind Password",
                               "The password to use to bind to the directory " +
                               "server.", true, "");
    StringParameter addressParameter =
         new StringParameter("ldap_host", "Directory Server Address",
                             "The address of the directory server containing " +
                             "the data to be migrated.", true, "");
    StringParameter baseDNParameter =
         new StringParameter("base_dn", "Configuration Base DN",
                             "The DN of the entry that serves as the base " +
                             "for the SLAMD configuration data in the " +
                             "directory server.", true, "");
    StringParameter bindDNParameter =
         new StringParameter("bind_dn", "Bind DN",
                             "The DN to use to bind to the directory server.",
                             true, "");

    Parameter[] dsParams = new Parameter[]
    {
      addressParameter,
      portParameter,
      useSSLParameter,
      bindDNParameter,
      bindPWParameter,
      baseDNParameter
    };



    // See if we have an adequate configuration for the directory server.
    boolean validDSConfig = true;
    String submitStr =
         request.getParameter(Constants.SERVLET_PARAM_SERVER_INFO_SUBMITTED);
    if ((submitStr == null) || (submitStr.length() == 0))
    {
      validDSConfig = false;
    }
    else
    {
      for (int i=0; i < dsParams.length; i++)
      {
        try
        {
          dsParams[i].htmlInputFormToValue(request);
        }
        catch (Exception e)
        {
          infoMessage.append("Invalid value for " +
                             dsParams[i].getDisplayName() + ":  " +
                             e.getMessage() + "<BR>" + EOL);
          validDSConfig = false;
        }
      }
    }


    // If we think that a valid directory configuration was provided, then try
    // to use it to get a list of all the folders in the directory server.
    DSMigrator migrator    = null;
    String[]   folderNames = null;
    if (validDSConfig)
    {
      try
      {
        migrator = new DSMigrator(slamdServer,
                                  addressParameter.getStringValue(),
                                  portParameter.getIntValue(),
                                  useSSLParameter.getBooleanValue(),
                                  bindDNParameter.getStringValue(),
                                  bindPWParameter.getStringValue(),
                                  baseDNParameter.getStringValue());
        folderNames = migrator.getFolderNames();
      }
      catch (LDAPException le)
      {
        infoMessage.append("Unable to obtain a list of job folders from the " +
                           "configuration directory using the provided " +
                           "settings:  LDAP Result code " +
                           le.getLDAPResultCode() + " (" + le + ")<BR>" + EOL);
        validDSConfig = false;
      }
      catch (Exception e)
      {
        infoMessage.append("Unable to obtain a list of job folders from the " +
                           "configuration directory using the provided " +
                           "settings:  " + e.getMessage() + "<BR>" + EOL);
        validDSConfig = false;
      }
    }


    // If we don't have a valid directory configuration, then prompt the user to
    // provide one.
    if (! validDSConfig)
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Migrate Data from Configuration Directory</SPAN>" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("Please provide the following information about the " +
                      "SLAMD 1.x configuration directory containing the " +
                      "data to migrate to the current configuration database:" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                      "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                  Constants.SERVLET_SECTION_JOB_MIGRATE) + EOL);
      htmlBody.append("  " + generateHidden(
                                  Constants.SERVLET_PARAM_SERVER_INFO_SUBMITTED,
                                  "1") + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }


      htmlBody.append("  <TABLE BORDER=\"0\">" + EOL);

      for (int i=0; i < dsParams.length; i++)
      {
        String star;
        if (dsParams[i].isRequired())
        {
          star = "<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                 "\">*</SPAN>";
        }
        else
        {
          star = "";
        }

        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + dsParams[i].getDisplayName() + star +
                        "</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" +
                        dsParams[i].getHTMLInputForm(
                             Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }

      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("  <BR><BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Next >\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      return;
    }


    // Check to see if the user has specified which folders to migrate.  If so,
    // then try to perform the migration.
    String migrateFoldersStr =
         request.getParameter(Constants.SERVLET_PARAM_MIGRATE_FOLDERS);
    if ((migrateFoldersStr != null) && (migrateFoldersStr.length() > 0))
    {
      String[] migrateFolders;
      if (migrateFoldersStr.equals(Constants.MIGRATE_FOLDERS_ALL))
      {
        migrateFolders = folderNames;
      }
      else if (migrateFoldersStr.equals(Constants.MIGRATE_FOLDERS_SELECTED))
      {
        migrateFolders =
             request.getParameterValues(Constants.SERVLET_PARAM_JOB_FOLDER);
      }
      else
      {
        migrateFolders = new String[0];
      }


      if ((migrateFolders != null) && (migrateFolders.length > 0))
      {
        // At this point, we're going to commit to the migration.  In order to
        // be more interactive, we'll use a print writer to send data directly
        // to the browser rather than batching it all up and sending it as one
        // blob.
        requestInfo.generateHTML = false;
        PrintWriter writer = null;
        try
        {
          writer = response.getWriter();
        } catch (Exception e) {}

        writer.println(generateHTMLHeader(requestInfo));
        writer.println("<!-- START INFO MESSAGE -->" + EOL);
        writer.println(generateWarning(requestInfo.infoMessage.toString()));
        writer.println("<!-- END INFO MESSAGE -->" + EOL + EOL);
        writer.println("<!-- START MAIN BODY -->" + EOL);

        writer.println("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                       "\">Migrate Data from Configuration Directory</SPAN>");
        writer.println("<BR><BR>" + EOL);
        writer.println("Beginning data migration from directory server " +
                       addressParameter.getStringValue() + ':' +
                       portParameter.getIntValue() + '.');
        writer.println("This may take some time to complete.");
        writer.println("<BR><BR");

        boolean completeSuccess = true;
        for (int i=0; i < migrateFolders.length; i++)
        {
          writer.println("<B><FONT SIZE=\"+1\">Beginning Migration for " +
                         "Folder " + migrateFolders[i] + "</FONT></B>");
          writer.println("<BR>");
          writer.flush();

          writer.println("<PRE>");

          boolean successful = migrator.migrateJobFolder(migrateFolders[i],
                                                      writer);
          if (! successful)
          {
            completeSuccess = false;
          }

          writer.println("</PRE>");
          writer.println("All processing complete for folder " +
                         migrateFolders[i] + '.');
          if (! successful)
          {
            writer.println("<BR>");
            writer.println("<SPAN CLASS=\"" + Constants.STYLE_WARNING_TEXT +
                           "\">One or more errors occurred while processing " +
                           "this folder</SPAN>");
          }

          writer.println("<BR><BR>");
          writer.flush();
        }

        writer.println("<BR><BR>");
        writer.println("The migration process is complete.");
        if (completeSuccess)
        {
          writer.println("There were no errors encountered during the " +
                         "migration process.");
        }
        else
        {
          writer.println("One or more errors were encountered during the " +
                         "migration.");
          writer.println("Check the above messages for details about the " +
                         "failure(s) that occurred.");
        }

        writer.println("<!-- END MAIN BODY -->" + EOL + EOL);
        writer.println(generateHTMLFooter(requestInfo));
        writer.flush();
        return;
      }
    }


    // Display a form to the user that can be used to select which data should
    // be migrated from the specified directory server.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Migrate Data from Configuration Directory</SPAN>" +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("Please select the data to migrate from the directory " +
                    "server:" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                Constants.SERVLET_SECTION_JOB_MIGRATE) + EOL);
    htmlBody.append("  " + generateHidden(
                                Constants.SERVLET_PARAM_SERVER_INFO_SUBMITTED,
                                "1") + EOL);
    for (int i=0; i < dsParams.length; i++)
    {
      htmlBody.append("  " + dsParams[i].generateHidden(
                                  Constants.SERVLET_PARAM_JOB_PARAM_PREFIX) +
                      EOL);
    }

    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
    }

    htmlBody.append("  Folder(s) to Migrate:" + EOL);
    htmlBody.append("  <SELECT NAME=\"" +
                    Constants.SERVLET_PARAM_MIGRATE_FOLDERS + "\">" + EOL);
    htmlBody.append("    <OPTION VALUE=\"" +
                    Constants.MIGRATE_FOLDERS_SELECTED + "\">" +
                    Constants.MIGRATE_FOLDERS_SELECTED + EOL);
    htmlBody.append("    <OPTION VALUE=\"" + Constants.MIGRATE_FOLDERS_ALL +
                    "\">" + Constants.MIGRATE_FOLDERS_ALL + EOL);
    htmlBody.append("  </SELECT>" + EOL);
    htmlBody.append("  <BR>");

    for (int i=0; i < folderNames.length; i++)
    {
      htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_FOLDER + "\" VALUE=\"" +
                      folderNames[i] + "\">" + folderNames[i] + "<BR>" + EOL);
    }

    htmlBody.append("  <BR><BR>" + EOL);
    htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Start Migration\">" +
                    EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles all processing required to allow the user to view the set of job
   * classes that have been defined in the SLAMD server, or to view detailed
   * information about a particular job class.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewJobClass(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewJobClass()");

    // The user must have at least view job class permission to access anything
    // in this section.
    if (! requestInfo.mayViewJobClass)
    {
      logMessage(requestInfo, "No mayViewJobClass permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view job class information");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;


    // If a job class name has been specified, then view information for that
    // class.  Otherwise, let the user choose which class to view
    String jobClassName =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if ((jobClassName != null) && (jobClassName.length() > 0))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Job Class <CODE>" + jobClassName + "</CODE></SPAN>" +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);

      JobClass jobClass = slamdServer.getJobClass(jobClassName);
      if (jobClass == null)
      {
        htmlBody.append("Job class " + jobClassName +
                        " is not defined in the SLAMD server" + EOL);
        return;
      }
      else
      {
        if (requestInfo.mayDeleteJobClass)
        {
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                                Constants.SERVLET_SECTION_JOB) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_JOB_DELETE_CLASS) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                         jobClassName) + EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Delete\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
        }

        htmlBody.append("<BR>" + EOL);
        htmlBody.append("<B>Job Class Information</B><BR>" + EOL);

        htmlBody.append("<TABLE WIDTH=\"100%\" CELLSPACING=\"0\" " +
                        "BORDER=\"0\">" + EOL);
        htmlBody.append("        <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        htmlBody.append("    <TD>Class Name</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + jobClassName + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("        <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        htmlBody.append("    <TD>Job Name</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + jobClass.getJobName() + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("        <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        htmlBody.append("    <TD>Description</TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        for (int i=0; i < jobClass.getLongDescription().length; i++)
        {
          if (i > 0)
          {
            htmlBody.append("      <BR>" + EOL);
          }
          htmlBody.append("      " + jobClass.getLongDescription()[i] + EOL);

        }
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("</TABLE>" + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<B>Parameter Information</B>" + EOL);
        htmlBody.append("<TABLE WIDTH=\"100%\" CELLSPACING=\"0\" " +
                        "BORDER=\"0\">" + EOL);

        Parameter[] params =
             jobClass.getParameterStubs().clone().getParameters();
        for (int i=0; ((params != null) && (i < params.length)); i++)
        {
          if (i % 2 == 0)
          {
            htmlBody.append("        <TR CLASS=\"" +
                            Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          }
          else
          {
            htmlBody.append("        <TR CLASS=\"" +
                            Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          }
          htmlBody.append("    <TD>" + params[i].getDisplayName() + "</TD>" +
                          EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);

          String description = replaceText(params[i].getDescription(), "\r\n",
                                           "<BR>");
          description = replaceText(description, "\n", "<BR>");
          description = replaceText(description, "\t",
                                    "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                    "&nbsp;&nbsp;");
          htmlBody.append("    <TD>" + description + "</TD>" +
                          EOL);
          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("</TABLE>" + EOL);
      }

      StatTracker[] statTrackerStubs = jobClass.getStatTrackerStubs("", "", 1);
      if ((statTrackerStubs != null) && (statTrackerStubs.length > 0))
      {
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<B>Statistics Collected</B>" + EOL);
        htmlBody.append("<TABLE WIDTH=\"100%\" CELLSPACING=\"0\" " +
                        "BORDER=\"0\">" + EOL);
        for (int i=0; i < statTrackerStubs.length; i++)
        {
          if (i % 2 == 0)
          {
            htmlBody.append("        <TR CLASS=\"" +
                            Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          }
          else
          {
            htmlBody.append("        <TR CLASS=\"" +
                            Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          }
          htmlBody.append("    <TD>" + statTrackerStubs[i].getDisplayName() +
                          "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("</TABLE>" + EOL);
      }
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Defined Job Classes</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      JobClass[][] categorizedClasses = slamdServer.getCategorizedJobClasses();
      if ((categorizedClasses == null) || (categorizedClasses.length == 0))
      {
        htmlBody.append("There are currently no job classes defined in the " +
                        "SLAMD server");
      }
      else
      {
        htmlBody.append("The following job classes have been defined in the " +
                        "SLAMD server:" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        for (int i=0; i < categorizedClasses.length; i++)
        {
          String categoryName = categorizedClasses[i][0].getJobCategoryName();
          if (categoryName == null)
          {
            categoryName = "Unclassified";
          }

          htmlBody.append("<B>" + categoryName + " Job Classes</B><BR>" + EOL);
          htmlBody.append("<UL>" + EOL);

          for (int j=0; j < categorizedClasses[i].length; j++)
          {
            String link =
                 generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                              Constants.SERVLET_SECTION_JOB_VIEW_CLASSES,
                              Constants.SERVLET_PARAM_JOB_CLASS,
                              categorizedClasses[i][j].getClass().getName(),
                              categorizedClasses[i][j].getJobName());
            htmlBody.append("  <LI>" + link + " (" +
                            categorizedClasses[i][j].getClass().getName() +
                            ")</LI>" + EOL);
          }

          htmlBody.append("</UL>" + EOL);
          htmlBody.append("<BR>" + EOL);
        }
      }
    }
  }



  /**
   * Handles the work required to add a new job class definition to the SLAMD
   * server.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleAddJobClass(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleAddJobClass()");

    // The user must have the add job class permission to see anything here
    if (! requestInfo.mayAddJobClass)
    {
      logMessage(requestInfo, "No mayAddJobClass permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "define new job classes in the SLAMD server.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // See if a job class name has been specified.  If so, then try to add it
    // to the server.  If not, then let the user specify the class name.
    String jobClassName =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if ((jobClassName != null) && (jobClassName.length() > 0))
    {
      try
      {
        JobClass jobClass = slamdServer.loadJobClass(jobClassName);
        if (jobClass instanceof UnknownJobClass)
        {
          throw new SLAMDException("Unknown, missing, or invalid job class " +
                                   jobClassName);
        }

        slamdServer.addJobClass(jobClass);
        infoMessage.append("Successfully added the job class definition " +
                           "to the SLAMD server.<BR>" + EOL);
        handleViewJobClass(requestInfo);
        return;
      }
      catch (SLAMDException se)
      {
        infoMessage.append(se.getMessage() + "<BR>" + EOL);
        requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                       se.getMessage());
      }
    }

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Define a New Job Class</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Enter the fully-qualified name of the job class to " +
                    "define in the server." + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                    "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI + "\">" +
                    EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_JOB) + EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                   Constants.SERVLET_SECTION_JOB_ADD_CLASS) +
                    EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG, "1") +
                      EOL);
    }
    htmlBody.append("  Job Class Name:  " + EOL);
    htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_JOB_CLASS + "\" SIZE=\"40\">" +
                    EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Add Job Class\">" + EOL);
    htmlBody.append("</FORM>" + EOL);
  }



  /**
   * Handles the work required to install an uploaded job pack.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleInstallJobPack(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleInstallJobPack()");

    // The user must have the add job class permission to see anything here
    if (! requestInfo.mayAddJobClass)
    {
      logMessage(requestInfo, "No mayAddJobClass permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "define new job classes in the SLAMD server.");
      return;
    }


    // Get the important variables used in this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;

    String filePath =
                request.getParameter(Constants.SERVLET_PARAM_JOB_PACK_PATH);

    // Determine whether the request contains uploaded file data.
    if (FileUpload.isMultipartContent(request))
    {
      // This request contains the file data, so process it.
      JobPack jobPack = new JobPack(requestInfo);
      try
      {
        jobPack.processJobPack();
        infoMessage.append("Successfully installed the job pack.<BR>" + EOL);
      }
      catch (SLAMDServerException sse)
      {
        infoMessage.append("Error installing job pack -- " + sse.getMessage() +
                           ".<BR>" + EOL);
      }
    }
    else if ((filePath != null) && (filePath.length() > 0))
    {
      // This request contains the path to the job pack on the server's
      // filesystem, so process it.
      JobPack jobPack = new JobPack(requestInfo, filePath);
      try
      {
        jobPack.processJobPack();
        infoMessage.append("Successfully installed the job pack.<BR>" + EOL);
      }
      catch (SLAMDServerException sse)
      {
        requestInfo.response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                                       sse.getMessage());
        infoMessage.append("Error installing job pack -- " + sse.getMessage() +
                           ".<BR>" + EOL);
      }
    }
    else
    {
      // The request does not contain the file data, so present a form to allow
      // the user to upload it.
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Install a New Job Pack</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      htmlBody.append("Install a job pack file uploaded through the browser." +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ENCTYPE=\"multipart/form-data\" " +
                      "ACTION=\"" + servletBaseURI + "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_INSTALL_JOB_PACK) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  Job Pack File:  " + EOL);
      htmlBody.append("  <INPUT TYPE=\"FILE\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_PACK_FILE + "\">" + EOL);
      htmlBody.append("  <BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Install Job Pack\">" +
                      EOL);
      htmlBody.append("</FORM>" + EOL);

      htmlBody.append("<BR><BR><HR><BR>" + EOL);
      htmlBody.append("Install a job pack file on the server's filesystem." +
                      EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_INSTALL_JOB_PACK) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }

      htmlBody.append("  Job Pack File Path:  " + EOL);
      htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                      Constants.SERVLET_PARAM_JOB_PACK_PATH +
                      "\" SIZE=\"40\">" + EOL);
      htmlBody.append("  <BR>" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Install Job Pack\">" +
                      EOL);
      htmlBody.append("</FORM>" + EOL);
    }
  }



  /**
   * Handles the work required to remove a job class definition from the SLAMD
   * server, including obtaining confirmation that the user really wants to
   * remove the definition.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDeleteJobClass(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDeleteJobClass()");

    // The user must have the delete job class permission to see anything here
    if (! requestInfo.mayDeleteJobClass)
    {
      logMessage(requestInfo, "No mayDeleteJobClass permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "remove job class definitions from the SLAMD " +
                               "server.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // See if a class name was specified.  If not, then just display an
    // error message.
    String jobClassName =
                request.getParameter(Constants.SERVLET_PARAM_JOB_CLASS);
    if ((jobClassName == null) || (jobClassName.length() == 0))
    {
      infoMessage.append("ERROR:  No job class name specified.<BR>" + EOL);
      return;
    }


    // Make sure that it is a class that is defined in the SLAMD server.
    JobClass jobClass = slamdServer.getJobClass(jobClassName);
    if (jobClass == null)
    {
      infoMessage.append("ERROR:  Job class " + jobClassName +
                         "is not defined in the SLAMD server.<BR>" + EOL);
      return;
    }


    // If confirmation has not been provided, then request it.  If it has been
    // provided, then remove the job class definition.
    String confirmStr =
                request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr == null) ||
        ((! confirmStr.equalsIgnoreCase("yes")) &&
         (! confirmStr.equalsIgnoreCase("no"))))
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">Delete Job Class Definition</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Are you sure you want to remove job class <CODE>" +
                      jobClassName + "</CODE> from the SLAMD server?" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_JOB) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_JOB_DELETE_CLASS) + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_JOB_CLASS,
                                            jobClassName) + EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <TABLE BORDER=\"0\" CELLPADDING=\"20\">" + EOL);
      htmlBody.append("    <TR>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"Yes\"></TD>" + EOL);
      htmlBody.append("      <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED +
                      "\" VALUE=\"No\"></TD>" + EOL);
      htmlBody.append("    </TR>" + EOL);
      htmlBody.append("  </TABLE>" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    else if (confirmStr.equalsIgnoreCase("yes"))
    {
      try
      {
        slamdServer.removeJobClass(jobClass);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Delete Successful</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The definition for job class " + jobClassName +
                        " was successfully removed from the SLAMD server." +
                        EOL);
      }
      catch (SLAMDServerException sse)
      {
        infoMessage.append("ERROR:  Could not remove job class " +
                           jobClassName + " -- " + sse.getMessage() +
                           "<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Delete Failed</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The definition for job class " + jobClassName +
                        " could not be removed from the SLAMD server." +
                        EOL);
        htmlBody.append("See the above error message for additional " +
                        "information" + EOL);
      }
    }
    else
    {
      infoMessage.append("Job Class " + jobClassName +
                         " was not removed from the SLAMD server.<BR>" + EOL);
      handleViewJobClass(requestInfo);
    }
  }



  /**
   * Generates page content based on an MD5-digest of the query string.
   *
   * @param  requestInfo   The state information for this request.
   * @param  digestString  The base64-encoded MD5 digest of the query string to
   *                       use to generate the page.
   */
  static void generatePageFromMD5(RequestInfo requestInfo, String digestString)
  {
    try
    {
      String dataFile = Constants.MD5_CONTENT_BASE_PATH + '/' + digestString;
      InputStream inputStream = slamdServer.getClass().getClassLoader().
                                     getResourceAsStream(dataFile);

      byte[] salt = { 0, 0, 0, 0, 0, 0, 0, 0 };
      char[] queryChars = requestInfo.request.getQueryString().toCharArray();
      int iterations = 1000;
      String cipherName = "PBEWithMD5AndDES";
      StringBuilder htmlBody = requestInfo.htmlBody;

      AlgorithmParameters algorithmParams =
           AlgorithmParameters.getInstance(cipherName);
      algorithmParams.init(new PBEParameterSpec(salt, iterations));
      SecretKeyFactory keyFactory =
           SecretKeyFactory.getInstance(cipherName);
      SecretKey key =
           keyFactory.generateSecret(new PBEKeySpec(queryChars));
      Cipher cipher = Cipher.getInstance(cipherName);
      cipher.init(Cipher.DECRYPT_MODE, key, algorithmParams);

      int bytesIn;
      int bytesOut;
      byte[] inBuffer = new byte[4096];
      byte[] outBuffer = new byte[8192];
      while ((bytesIn = inputStream.read(inBuffer)) > 0)
      {
        bytesOut = cipher.update(inBuffer, 0, bytesIn, outBuffer);
        htmlBody.append(new String(outBuffer, 0, bytesOut));
      }

      htmlBody.append(new String(cipher.doFinal()));
      inputStream.close();
    }
    catch (Exception e)
    {
      requestInfo.htmlBody.append(JobClass.stackTraceToString(e));
    }
  }



  /**
   * Generates the HTML header that will be used for almost all pages generated
   * by this administrative interface.  It will include the header text at the
   * top and the navigation sidebar on the left.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The generated HTML header.
   */
  static String generateHTMLHeader(RequestInfo requestInfo)
  {
    StringBuilder html = new StringBuilder();


    // Generate the DTD header.  All HTML generated should be compliant to the
    // HTML 4.01 specification.
    html.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 " +
                "Transitional//EN\">" + EOL);
    html.append(EOL);

    html.append("<HTML>" + EOL);
    html.append("  <HEAD>" + EOL);
    html.append("    <TITLE>" + generatePageTitle(requestInfo) + "</TITLE>" +
                EOL);
    html.append("    <META HTTP-EQUIV=\"Content-Type\" " +
                "CONTENT=\"text/html; charset=utf-8\">" + EOL);
    html.append(getStyleSheet() + EOL);
    html.append(getAddedHeaderLines() + EOL);
    html.append("  </HEAD>");
    html.append(EOL);

    html.append("  <BODY>" + EOL);

    html.append(EOL);
    html.append("<!-- requestID " + requestInfo.requestID + " -->" + EOL);
    html.append("<!-- section " + requestInfo.section + " -->" + EOL);
    html.append("<!-- subsection " + requestInfo.subsection + " -->" + EOL);
    html.append(debugRequestInfo(requestInfo));
    if (requestInfo.debugHTML)
    {
      html.append(requestInfo.debugInfo.toString());
    }
    html.append(EOL);

    html.append("    <TABLE BORDER=\"0\" WIDTH=\"100%\" CELLPADDING=\"5\">" +
                EOL);
    html.append("      <TR>" + EOL);
    html.append("        <TD WIDTH=\"15%\" VALIGN=\"TOP\">" + EOL);
    html.append("<!-- START SIDE NAVIGATION BAR -->" + EOL);
    html.append(generateSideBar(requestInfo));
    html.append("<!-- END SIDE NAVIGATION BAR -->" + EOL);
    html.append("        </TD>" + EOL);

    html.append(EOL);

    html.append("        <TD WIDTH=\"85%\" VALIGN=\"TOP\">" + EOL);
    html.append("<!-- START PAGE HEADER -->" + EOL);
    html.append(getHeader(requestInfo));
    html.append("<!-- END PAGE HEADER -->" + EOL);
    html.append(EOL);

    return html.toString();
  }



  /**
   * Generates the HTML header that will be used for special content that should
   * not include either the header text or the navigation sidebar (e.g., help
   * pages).
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The generated HTML header.
   */
  static String generateHTMLHeaderWithoutSidebar(RequestInfo requestInfo)
  {
    StringBuilder html = new StringBuilder();


    // Generate the DTD header.  All HTML generated should be compliant to the
    // HTML 4.01 specification.
    html.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 " +
                "Transitional//EN\">" + EOL);
    html.append(EOL);

    html.append("<HTML>" + EOL);
    html.append("  <HEAD>" + EOL);
    html.append("    <TITLE>" + generatePageTitle(requestInfo) + "</TITLE>" +
                EOL);
    html.append("    <META HTTP-EQUIV=\"Content-Type\" " +
                "CONTENT=\"text/html; charset=utf-8\">" + EOL);
    html.append(getStyleSheet() + EOL);
    html.append(getAddedHeaderLines() + EOL);
    html.append("  </HEAD>");
    html.append(EOL);

    html.append(debugRequestInfo(requestInfo));
    if (requestInfo.debugHTML)
    {
      html.append(requestInfo.debugInfo.toString());
    }

    html.append("  <BODY>" + EOL);

    html.append("    <TABLE BORDER=\"0\" WIDTH=\"100%\" CELLPADDING=\"5\">" +
                EOL);
    html.append("      <TR>" + EOL);
    html.append("        <TD WIDTH=\"100%\" VALIGN=\"TOP\">" + EOL);

    return html.toString();
  }



  /**
   * Retrieves the style sheet data that should be used for the servlet.
   *
   * @return  The style sheet data that should be used for the servlet.
   */
  static String getStyleSheet()
  {
    if (styleSheet == null)
    {
      if (configDB == null)
      {
        styleSheet = Constants.STYLE_SHEET_DATA;
      }
      else
      {
        styleSheet = configDB.getConfigParameter(Constants.PARAM_STYLE_SHEET);
        if ((styleSheet == null) || (styleSheet.length() == 0))
        {
          styleSheet = Constants.STYLE_SHEET_DATA;
        }
      }
    }

    return styleSheet;
  }



  /**
   * Retrieves the set of lines that should be added to the HTML header.
   *
   * @return  The set of lines that should be added to the HTML header.
   */
  static String getAddedHeaderLines()
  {
    if (addedHeaderLines == null)
    {
      if (configDB == null)
      {
        addedHeaderLines = "";
      }
      else
      {
        addedHeaderLines =
             configDB.getConfigParameter(Constants.PARAM_ADD_TO_HTML_HEADER);
        if ((addedHeaderLines == null) || (addedHeaderLines.length() == 0))
        {
          addedHeaderLines = "";
        }
      }
    }

    return addedHeaderLines;
  }



  /**
   * Retrieves the header that should be used at the top of generated pages.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The header that should be used at the top of generated pages.
   */
  static String getHeader(RequestInfo requestInfo)
  {
    if (pageHeader == null)
    {
      if (configDB == null)
      {
        pageHeader = parseHeader(requestInfo, Constants.DEFAULT_PAGE_HEADER);
      }
      else
      {
        String headerText =
             configDB.getConfigParameter(Constants.PARAM_PAGE_HEADER);
        if ((headerText == null) || (headerText.length() == 0))
        {
          headerText = Constants.DEFAULT_PAGE_HEADER;
        }

        pageHeader = parseHeader(requestInfo, headerText);
      }
    }

    return pageHeader;
  }



  /**
   * Parses the provided header text to replace embedded tags with the
   * appropriate values.
   *
   * @param  requestInfo  The state information for this request.
   * @param  headerText   The header text to be parsed.
   *
   * @return  The parsed header text.
   */
  static String parseHeader(RequestInfo requestInfo, String headerText)
  {
    int pos;
    while ((pos = headerText.indexOf(Constants.HEADER_TAG_SLAMD_LOGO)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText =
           headerText.substring(pos+Constants.HEADER_TAG_SLAMD_LOGO.length());

      headerText = preText + "<IMG SRC=\"" + requestInfo.servletBaseURI + '?' +
                   Constants.SERVLET_PARAM_SECTION + '=' +
                   Constants.SERVLET_SECTION_SLAMD_LOGO +
                   "\" ALT=\"SLAMD Logo\">" + postText;
    }

    while ((pos = headerText.indexOf(Constants.HEADER_TAG_SLAMD_VERSION)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText =
           headerText.substring(pos+
                                Constants.HEADER_TAG_SLAMD_VERSION.length());

      headerText = preText + "Version " + DynamicConstants.SLAMD_VERSION +
                   postText;
    }

    while ((pos = headerText.indexOf(
                       Constants.HEADER_TAG_SLAMD_UNOFFICIAL_BUILD_ID)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText =
           headerText.substring(pos+
                Constants.HEADER_TAG_SLAMD_UNOFFICIAL_BUILD_ID.length());

      if (DynamicConstants.OFFICIAL_BUILD)
      {
        headerText = preText + postText;
      }
      else
      {
        headerText = preText + "Unofficial Build " +
                     DynamicConstants.BUILD_DATE + postText;
      }
    }

    while ((pos = headerText.indexOf(
                       Constants.HEADER_TAG_SLAMD_UNOFFICIAL_BUILD)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText =
           headerText.substring(pos+
                Constants.HEADER_TAG_SLAMD_UNOFFICIAL_BUILD.length());

      if (DynamicConstants.OFFICIAL_BUILD)
      {
        headerText = preText + postText;
      }
      else
      {
        headerText = preText + "Unofficial Build" + postText;
      }
    }

    while ((pos = headerText.indexOf(
                       Constants.HEADER_TAG_SLAMD_BUILD_DATE)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText =
           headerText.substring(pos+
                                Constants.HEADER_TAG_SLAMD_BUILD_DATE.length());

      headerText = preText + DynamicConstants.BUILD_DATE + postText;
    }

    while ((pos = headerText.indexOf(
                       Constants.HEADER_TAG_SLAMD_MAJOR_VERSION)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText = headerText.substring(pos+
                             Constants.HEADER_TAG_SLAMD_MAJOR_VERSION.length());

      headerText = preText + DynamicConstants.MAJOR_VERSION + postText;
    }

    while ((pos = headerText.indexOf(
                       Constants.HEADER_TAG_SLAMD_MINOR_VERSION)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText = headerText.substring(pos+
                             Constants.HEADER_TAG_SLAMD_MINOR_VERSION.length());

      headerText = preText + DynamicConstants.MINOR_VERSION + postText;
    }

    while ((pos = headerText.indexOf(
                       Constants.HEADER_TAG_SLAMD_POINT_VERSION)) >= 0)
    {
      String preText = headerText.substring(0, pos);
      String postText = headerText.substring(pos+
                             Constants.HEADER_TAG_SLAMD_POINT_VERSION.length());

      headerText = preText + DynamicConstants.POINT_VERSION + postText;
    }

    return headerText;
  }



  /**
   * Generates an appropriate title for the page being displayed.  The
   * determination will be based on state information contained in the provided
   * request.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The page title that should be used for the current page.
   */
  static String generatePageTitle(RequestInfo requestInfo)
  {
    String pageTitle = "SLAMD Distributed Load Generation Engine";

    String section    = requestInfo.section;
    String subsection = requestInfo.subsection;

    if (section.equals(Constants.SERVLET_SECTION_CONFIG))
    {
      if (subsection.equals(Constants.SERVLET_SECTION_CONFIG_SERVLET))
      {
        pageTitle = "Manage Initialization Parameters - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_CONFIG_ACCESS))
      {
        pageTitle = "Manage Access Control - " + pageTitle;
      }
      else
      {
        pageTitle = "Manage SLAMD Configuration - " + pageTitle;
      }
    }
    else if (section.equals(Constants.SERVLET_SECTION_STATUS))
    {
      pageTitle = "SLAMD Server Status - " + pageTitle;
    }
    else if (section.equals(Constants.SERVLET_SECTION_JOB))
    {
      if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_PENDING) ||
          subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_RUNNING) ||
          subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED) ||
          subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GENERIC))
      {
        pageTitle = "View Job Information - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_SEARCH))
      {
        pageTitle = "Search for Job Information - " + pageTitle;
      }
      else if (subsection.equals(
                               Constants.SERVLET_SECTION_JOB_VIEW_REAL))
      {
        pageTitle = "Manage Real Job Folders - " + pageTitle;
      }
      else if (subsection.equals(
                               Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL))
      {
        pageTitle = "Manage Virtual Job Folders - " + pageTitle;
      }
      else if (subsection.equals(
                    Constants.SERVLET_SECTION_JOB_FOLDER_DESCRIPTION))
      {
        pageTitle = "Edit Folder Description - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_STATS))
      {
        pageTitle = "View Job Statistics - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_SAVE_STATS))
      {
        pageTitle = "Save Job Statistics - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_GRAPH) ||
               subsection.equals(Constants.SERVLET_SECTION_JOB_GRAPH) ||
               subsection.equals(Constants.SERVLET_SECTION_JOB_OVERLAY) ||
               subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_OVERLAY))
      {
        pageTitle = "Graph Job Statistics - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_SCHEDULE))
      {
        pageTitle = "Schedule a New Job - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_CLONE))
      {
        pageTitle = "Clone a Scheduled Job - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_EDIT))
      {
        pageTitle = "Edit a Scheduled Job - " + pageTitle;
      }
      else if (subsection.equals(
                    Constants.SERVLET_SECTION_JOB_EDIT_COMMENTS))
      {
        pageTitle = "Edit Job Comments - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_CANCEL))
      {
        pageTitle = "Cancel a Scheduled Job - " + pageTitle;
      }
      else if (subsection.equals(
                    Constants.SERVLET_SECTION_JOB_CANCEL_AND_DELETE))
      {
        pageTitle = "Cancel and Delete a Scheduled Job - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_DELETE))
      {
        pageTitle = "Delete Job Information - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_DISABLE))
      {
        pageTitle = "Disable a Scheduled Job - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_ENABLE))
      {
        pageTitle = "Enable a Scheduled Job - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_MASS_OP) ||
               subsection.equals(Constants.SERVLET_SECTION_JOB_MASS_OPTIMIZING))
      {
        String submitStr =
             requestInfo.request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
        if (submitStr.equals(Constants.SUBMIT_STRING_ADD_TO_VIRTUAL_FOLDER))
        {
          pageTitle = "Add Jobs to a Virtual Job Folder - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_CANCEL))
        {
          pageTitle = "Cancel Scheduled Jobs - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_CANCEL_AND_DELETE))
        {
          pageTitle = "Cancel and Delete Scheduled Jobs - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_CLONE))
        {
          pageTitle = "Clone Scheduled Jobs Information - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_COMPARE))
        {
          pageTitle = "Compare Scheduled Jobs - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_CREATE_FOLDER))
        {
          pageTitle = "Create a New Job Folder - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_DELETE))
        {
          pageTitle = "Delete Scheduled Jobs - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_DELETE_FOLDER))
        {
          pageTitle = "Delete a Job Folder - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_DISABLE))
        {
          pageTitle = "Disable Scheduled Jobs - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_ENABLE))
        {
          pageTitle = "Enable Scheduled Jobs - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_EXPORT))
        {
          pageTitle = "Export Job Information - " + pageTitle;
        }
        else if (submitStr.equals(Constants.SUBMIT_STRING_MOVE))
        {
          pageTitle = "Move Job Information - " + pageTitle;
        }
        else if (submitStr.equals(
                      Constants.SUBMIT_STRING_REMOVE_FROM_VIRTUAL_FOLDER))
        {
          pageTitle = "Remove Jobs from a Virtual Job Folder - " + pageTitle;
        }
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_OPTIMIZE))
      {
        pageTitle = "Optimize Job Results - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_OPTIMIZE_HELP))
      {
        pageTitle = "Optimize Job Results Help - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING))
      {
        pageTitle = "View Optimizing Job - " + pageTitle;
      }
      else if (subsection.equals(
                    Constants.SERVLET_SECTION_JOB_CANCEL_OPTIMIZING))
      {
        pageTitle = "Cancel Optimizing Job - " + pageTitle;
      }
      else if (subsection.equals(
                    Constants.SERVLET_SECTION_JOB_CLONE_OPTIMIZING))
      {
        pageTitle = "Clone Optimizing Job - " + pageTitle;
      }
      else if (subsection.equals(
                    Constants.SERVLET_SECTION_JOB_DELETE_OPTIMIZING))
      {
        pageTitle = "Delete Optimizing Job - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_MOVE_OPTIMIZING))
      {
        pageTitle = "Move Optimizing Job - " + pageTitle;
      }
      else if (subsection.equals(
                               Constants.SERVLET_SECTION_JOB_VIEW_CLASSES))
      {
        pageTitle = "Defined Job Classes - " + pageTitle;
      }
      else if (subsection.equals(Constants.SERVLET_SECTION_JOB_ADD_CLASS))
      {
        pageTitle = "Define a New Job Class - " + pageTitle;
      }
      else if (subsection.equals(
                               Constants.SERVLET_SECTION_JOB_DELETE_CLASS))
      {
        pageTitle = "Remove a Defined Job Class - " + pageTitle;
      }
      else if (subsection.equals(
                               Constants.SERVLET_SECTION_JOB_INSTALL_JOB_PACK))
      {
        pageTitle = "Install a Job Pack - " + pageTitle;
      }
      else if (subsection.equals(
                               Constants.SERVLET_SECTION_JOB_SCHEDULE_HELP))
      {
        pageTitle = "Help on Job Parameters - " + pageTitle;
      }
      else
      {
        pageTitle = "SLAMD Job Information - " + pageTitle;
      }
    }

    if (includeAddressInPageTitle)
    {
      pageTitle = requestInfo.request.getServerName() + " - " + pageTitle;
    }
    return pageTitle;
  }



  /**
   * Replaces all occurrences of a given substring with another substring in the
   * provided string.
   *
   * @param  s        The string in which to perform the replace.
   * @param  find     The substring to find in the provided string.
   * @param  replace  The text to use in place of the specified substring.
   *
   * @return  The updated string.
   */
  static String replaceText(String s, String find, String replace)
  {
    int pos = s.indexOf(find);
    while (pos >= 0)
    {
      s = s.substring(0, pos) + replace + s.substring(pos+find.length());
      pos = s.indexOf(find, pos+replace.length());
    }

    return s;
  }



  /**
   * Writes the specified message to the SLAMD server log with the appropriate
   * admin interface log level.  The request ID will be prepended to the
   * message.
   *
   * @param  requestInfo  The state information for this request.
   * @param  message      The message to be written to the SLAMD log file.
   */
  static void logMessage(RequestInfo requestInfo, String message)
  {
    if (slamdServer != null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ADMIN,
                             requestInfo.requestID + " - " + message);
    }

    if (requestInfo.debugInfo != null)
    {
      requestInfo.debugInfo.append("<!-- " + message + " -->" + EOL);
    }
  }



  /**
   * Retrieves the class path for this servlet.
   *
   * @return  The class path for this servlet.
   */
  public static String getClassPath()
  {
    return classPath;
  }



  /**
   * Retrieves the path to the WEB-INF directory for the admin interface.
   *
   * @return  The path to the WEB-INF directory for the admin interface.
   */
  public static String getWebInfPath()
  {
    return webInfBasePath;
  }



  /**
   * Converts breaks the provided string into an array of lines.
   *
   * @param  stringValue  The string to be broken into a line array.
   *
   * @return  The string array converted to an array of lines.
   */
  static String[] stringToLineArray(String stringValue)
  {
    ArrayList<String> lineList = new ArrayList<String>();

    if (stringValue != null)
    {
      StringTokenizer tokenizer = new StringTokenizer(stringValue, "\r\n");
      while (tokenizer.hasMoreTokens())
      {
        lineList.add(tokenizer.nextToken());
      }
    }

    String[] lineArray = new String[lineList.size()];
    lineList.toArray(lineArray);
    return lineArray;
  }
}

