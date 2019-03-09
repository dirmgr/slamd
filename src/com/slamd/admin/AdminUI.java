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



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import netscape.ldap.LDAPException;

import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;
import com.slamd.common.SLAMDException;
import com.slamd.db.SLAMDDB;
import com.slamd.job.Job;
import com.slamd.job.JobClass;
import com.slamd.server.ClientConnection;
import com.slamd.server.ClientManagerConnection;
import com.slamd.server.ResourceMonitorClientConnection;
import com.slamd.server.SLAMDServer;
import com.slamd.server.SLAMDServerException;

import static com.slamd.admin.AdminAccess.*;
import static com.slamd.admin.AdminConfig.*;
import static com.slamd.admin.AdminServlet.*;



/**
 * This class provides a set of methods for providing the core administrative
 * user interface.
 */
public class AdminUI
{
  /**
   * The HTML that should be displayed on the main page displayed when a user
   * initially accesses the SLAMD administrative interface.
   */
  static String defaultHTML = null;



  /**
   * Retrieves the HTML that should be used for the main page that will be
   * displayed if the user has not requested specific content.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The HTML that should be used for the main page that will be
   *          displayed if the user has not requested specific content.
   */
  static String getDefaultHTML(RequestInfo requestInfo)
  {
    if (defaultHTML == null)
    {
      defaultHTML = configDB.getConfigParameter(Constants.PARAM_DEFAULT_HTML);
      if ((defaultHTML == null) || (defaultHTML.length() == 0))
      {
        defaultHTML = Constants.DEFAULT_HTML;
      }

      defaultHTML = parseHeader(requestInfo, defaultHTML);
    }

    return defaultHTML;
  }



  /**
   * Generate the HTML content that will be displayed when the SLAMD server
   * is unavailable.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleSLAMDUnavailable(RequestInfo requestInfo)
  {
    StringBuilder htmlBody = requestInfo.htmlBody;

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">SLAMD Server Unavailable</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The SLAMD server is currently unavailable." + EOL);
    htmlBody.append(unavailableReason);

    String link = generateLink(requestInfo, Constants.SERVLET_SECTION_CONFIG,
                               Constants.SERVLET_SECTION_CONFIG_SERVLET,
                               "here");
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Click " + link + " to access the SLAMD server " +
                    "configuration.");
  }



  /**
   * Generate the HTML content that will be displayed when access control has
   * been enabled, but it was not possible to determine the identity of the end
   * user.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleUnauthenticatedUser(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleUnauthenticatedUser()");

    StringBuilder htmlBody = requestInfo.htmlBody;
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Authentication Required</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("Access control has been enabled, but your identity " +
                    "could not be determined." + EOL);
    htmlBody.append("Please re-authenticate, or contact your system " +
                    "administrator for additional information" + EOL);
  }



  /**
   * Handles the work of sending the SLAMD logo to the client.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @throws  IOException  If a problem occurs while sending the logo to the
   *                       client.
   */
  static void handleSLAMDLogo(RequestInfo requestInfo)
         throws IOException
  {
    requestInfo.generateHTML    = false;
    requestInfo.generateSidebar = false;

    HttpServletResponse response = requestInfo.response;
    response.setContentType("image/gif");
    response.setContentLength(slamdLogoBytes.length);

    OutputStream outputStream = response.getOutputStream();
    outputStream.write(slamdLogoBytes);
    outputStream.flush();
  }



  /**
   * Generates a page that may be used to access the SLAMD documentation.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDocumentation(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDocumentation()");

    StringBuilder htmlBody = requestInfo.htmlBody;
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">SLAMD Documentation</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("The following documentation is available through this " +
                    "administrative interface." + EOL);
    htmlBody.append("The complete set of SLAMD documentation may be found " +
                    "online at <A HREF=\"" + Constants.SLAMD_DOC_URL +
                    "\" TARGET=\"_BLANK\">" + Constants.SLAMD_DOC_URL +
                    "</A>." + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<B>Quick Start Guide</B><BR>" + EOL);
    htmlBody.append("Format:  " + EOL);
    htmlBody.append("<A HREF=\"" + Constants.DOC_URI_QUICK_START_ODT +
                    "\">OpenDocument</A>" + EOL);
    htmlBody.append("&nbsp;|&nbsp;" + EOL);
    htmlBody.append("<A HREF=\"" + Constants.DOC_URI_QUICK_START_PDF +
                    "\">PDF</A>" + EOL);
    htmlBody.append("<BR>" + EOL);
    htmlBody.append("Provides a basic set of instructions for getting the " +
                    "SLAMD server and client software installed and running."  +
                    EOL);
    htmlBody.append("It is less complete than the Administration and Usage " +
                    "Guide, but provides quick access to the information " +
                    "that should be the most helpful when trying to use " +
                    "SLAMD." + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("<B>Tools Guide</B><BR>" + EOL);
    htmlBody.append("Format:  " + EOL);
    htmlBody.append("<A HREF=\"" + Constants.DOC_URI_TOOLS_ODT +
                    "\">OpenDocument</A>" + EOL);
    htmlBody.append("&nbsp;|&nbsp;" + EOL);
    htmlBody.append("<A HREF=\"" + Constants.DOC_URI_TOOLS_PDF +
                    "\">PDF</A>" + EOL);
    htmlBody.append("<BR>" + EOL);
    htmlBody.append("Provides information about many of the tools provided " +
                    "with SLAMD."  + EOL);
    htmlBody.append("<BR><BR>" + EOL);
  }



  /**
   * Generates an HTML-formatted page that can be used to view the SLAMD
   * license.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleHTMLLicense(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleHTMLLicense()");

    StringBuilder htmlBody = requestInfo.htmlBody;
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">SLAMD License</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    htmlBody.append("The SLAMD Distributed Load Generation Engine (SLAMD) is " +
                    "licensed under the Sun Public License version 1.0, " +
                    "which is an " +
                    "<A HREF=\"http://www.opensource.org/licenses/index.php\"" +
                    " TARGET=\"_BLANK\">OSI-approved Open Source license</A>." +
                    EOL);

    String link = generateNewWindowLink(requestInfo,
                                        Constants.SERVLET_SECTION_LICENSE_TEXT,
                                        null, null, null, "here");
    htmlBody.append("The full text of this license is provided below in HTML " +
                    "form, or you can click " + link + " for a plain-text " +
                    "version." + EOL);
    htmlBody.append("<BR><BR>" + EOL);

    try
    {
      ClassLoader loader = requestInfo.getClass().getClassLoader();
      InputStream inputStream =
           loader.getResourceAsStream(Constants.RESOURCE_LICENSE_HTML);
      BufferedReader reader =
           new BufferedReader(new InputStreamReader(inputStream));

      String line;
      while ((line = reader.readLine()) != null)
      {
        htmlBody.append(line + EOL);
      }

      reader.close();
    }
    catch (Exception e)
    {
      htmlBody.append("An error occurred while trying to read the SLAMD " +
                      "license file:  " + e + "<BR><BR>" + EOL);
      htmlBody.append("Please see <A HREF=\"http://www.opensource.org/" +
                      "licenses/sunpublic.php\" TARGET=\"BLANK\">" +
                      "http://www.opensource.org/licenses/sunpublic.php</A> " +
                      "for the text of the Sun Public License." + EOL);
    }
  }



  /**
   * Retrieves the contents of the SLAMD license in plain text form.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleTextLicense(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleTextLicense()");


    // Prepare to send the entire response to the client.
    HttpServletResponse response = requestInfo.response;
    PrintWriter writer;
    try
    {
      writer = response.getWriter();
      response.setContentType("text/plain");
      requestInfo.generateHTML = false;
    }
    catch (IOException ioe)
    {
      // What can we do here?
      ioe.printStackTrace();
      return;
    }


    try
    {
      ClassLoader loader = requestInfo.getClass().getClassLoader();
      InputStream inputStream =
           loader.getResourceAsStream(Constants.RESOURCE_LICENSE_TEXT);
      BufferedReader reader =
           new BufferedReader(new InputStreamReader(inputStream));

      String line;
      while ((line = reader.readLine()) != null)
      {
        writer.println(line);
      }

      reader.close();
    }
    catch (Exception e)
    {
      writer.println("An error occurred while trying to read the SLAMD " +
                     "license file:  " + e);
      writer.println();
      writer.println("See http://www.opensource.org/licenses/sunpublic.php " +
                     "for the full text of the Sun Public License." + EOL);
    }
  }



  /**
   * Generate the HTML content that will be displayed when the SLAMD
   * configuration database does not yet exist.
   *
   * @param  requestInfo  The sate information for this request.
   */
  static void handleNoDB(RequestInfo requestInfo)
  {
    // Get the important state information for this request.
    HttpServletRequest request     = requestInfo.request;
    StringBuilder       htmlBody    = requestInfo.htmlBody;
    StringBuilder       infoMessage = requestInfo.infoMessage;


    // First, see if the server is operating in read-only mode.  If so, then we
    // won't try to create the database and will report an error instead.
    if (readOnlyMode)
    {
      infoMessage.append("ERROR:  The SLAMD configuration database does not " +
                         "exist in the configured location (" +
                         configDBDirectory + ") and the server is configured " +
                         "to operate in read-only mode.<BR>" + EOL);
      infoMessage.append("The SLAMD server will not be able to start with " +
                         "this configuration.<BR>" + EOL);

      htmlBody.append("<H2>Unable to start the SLAMD Server</H2>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The SLAMD server will not be able to start until the " +
                      "above error is corrected." + EOL);
      return;
    }


    // See if the user has granted permission for the database to be created.
    String confirmStr = request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
    if ((confirmStr != null) &&
        confirmStr.equals(Constants.SUBMIT_STRING_CREATE_DB))
    {
      try
      {
        // The user said we can create the database, so try to do so.
        SLAMDDB.createDB(configDBDirectory);
        infoMessage.append("Successfully created the SLAMD configuration " +
                           "database.<BR>" + EOL);
        configDBExists = true;

        // Start the SLAMD server.
        slamdServer = new SLAMDServer(adminServlet, readOnlyMode,
                                      configDBDirectory, sslKeyStore,
                                      sslKeyStorePassword, sslTrustStore,
                                      sslTrustStorePassword);
        scheduler     = slamdServer.getScheduler();
        configDB      = slamdServer.getConfigDB();
        configDB.registerAsSubscriber(ADMIN_CONFIG);
        ADMIN_CONFIG.refreshSubscriberConfiguration();
        slamdRunning  = true;
        infoMessage.append("Successfully started the SLAMD server with this " +
                           "new database.<BR>" + EOL);
        htmlBody.append(getDefaultHTML(requestInfo));
        return;
      }
      catch (Exception e)
      {
        e.printStackTrace();
        infoMessage.append("Unable to create the SLAMD configuration " +
                           "database and start the SLAMD server:  " + e +
                           "<BR>" + EOL);
        htmlBody.append("<H2>Unable to Start the SLAMD Server</H2>" + EOL);
        htmlBody.append("<BR><BR>");
        htmlBody.append("An error occurred that prevented the SLAMD server " +
                        "from creating the configuration database in the " +
                        configDBDirectory + " directory and starting up." +
                        EOL);
        htmlBody.append("The stack trace from the exception that occurred " +
                        "is:" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append(JobClass.stackTraceToString(e));
        return;
      }
    }
    else if ((confirmStr != null) &&
             confirmStr.equals(Constants.SUBMIT_STRING_CANCEL))
    {
      infoMessage.append("The configuration database was not created.<BR>" +
                           EOL);
      htmlBody.append("<H2>Database Creation Cancelled</H2>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The SLAMD configuration database was not created " +
                      "because the operation was cancelled at the request " +
                      "of the end user." + EOL);
      return;
    }
    else
    {
      infoMessage.append("WARNING:  The SLAMD configuration database does " +
                         "not exist.<BR>" + EOL);
      htmlBody.append("<H2>Create the SLAMD Configuration Database</H2>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The SLAMD configuration database could not be found " +
                      "at " + configDBDirectory +
                      " on the server's filesystem." + EOL);
      htmlBody.append("If this is a fresh SLAMD server installation, then " +
                      "this is normal and you can create the database in " +
                      "this location by clicking the \"" +
                      Constants.SUBMIT_STRING_CREATE_DB +
                      "\" button below.<BR><BR>" + EOL);
      htmlBody.append("If the configuration database should already exist " +
                      "in some other location, or if you wish to create a " +
                      "new database elsewhere, then stop the SLAMD server " +
                      "and specify that location in the value of the \"" +
                      Constants.SERVLET_INIT_PARAM_CONFIG_DB_DIRECTORY +
                      "\" servlet initialization parameter.");
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\">" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_CREATE_DB + "\">" + EOL);
      htmlBody.append("  &nbsp;&nbsp;" + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                      Constants.SERVLET_PARAM_CONFIRMED + "\" VALUE=\"" +
                      Constants.SUBMIT_STRING_CANCEL + "\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
      return;
    }
  }



  /**
   * Handle all processing related to the operations that may be performed in
   * the status section of the administrative interface.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleStatus(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleStatus()");

    // The user must have at least view status permission to access anything in
    // this section.
    if (! requestInfo.mayViewStatus)
    {
      logMessage(requestInfo, "No mayViewStatus permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "access SLAMD server status information");
      return;
    }


    // Get the important state information from the request info.
    HttpServletRequest  request        = requestInfo.request;
    HttpServletResponse response       = requestInfo.response;
    String              servletBaseURI = requestInfo.servletBaseURI;
    String              subsection     = requestInfo.subsection;
    StringBuilder        htmlBody       = requestInfo.htmlBody;
    StringBuilder        infoMessage    = requestInfo.infoMessage;


    // See if there was a request to do anything, or if the user wants to view
    // status information.
    if (subsection.equals(Constants.SERVLET_SECTION_STATUS_START_SLAMD))
    {
      // If the user doesn't have permission to start SLAMD, then don't let them
      if (! requestInfo.mayStartStopSLAMD)
      {
        logMessage(requestInfo, "No mayStartStopSLAMD permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "start the SLAMD server");
        response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                           "You do not have permission to start the SLAMD " +
                           "server");
        return;
      }

      // If the SLAMD server is already running, then there is nothing to do
      if (slamdRunning)
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">SLAMD Already Running</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The SLAMD server is already running." + EOL);
        htmlBody.append("Your request to start it has been ignored." + EOL);
        response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                           "The SLAMD server is already running.");
      }
      else
      {
        // If confirmation has not been provided, then get it now
        String confirmStr =
                    request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
        if ((confirmStr == null) ||
            ((! confirmStr.equalsIgnoreCase("yes")) &&
             (! confirmStr.equalsIgnoreCase("no"))))
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Start SLAMD Server</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Are you sure that you want to start the SLAMD " +
                          "server?" + EOL);
          htmlBody.append("<BR>");
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_STATUS) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_STATUS_START_SLAMD) +
                          EOL);
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
            slamdServer = new SLAMDServer(adminServlet, readOnlyMode,
                 configDBDirectory, sslKeyStore, sslKeyStorePassword,
                 sslTrustStore, sslTrustStorePassword);

            scheduler     = slamdServer.getScheduler();
            configDB      = slamdServer.getConfigDB();
            configDB.registerAsSubscriber(ADMIN_CONFIG);
            ADMIN_CONFIG.refreshSubscriberConfiguration();
            slamdRunning  = true;

            htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                            "\">SLAMD Server Started</SPAN>" + EOL);
            htmlBody.append("<BR><BR>" + EOL);
            htmlBody.append("The SLAMD server has been started successfully." +
                            EOL);

            htmlBody.append("<BR><BR>" + EOL);
            String link = generateLink(requestInfo,
                                       Constants.SERVLET_SECTION_STATUS, null,
                                       "Return to the status page.");
            htmlBody.append(link + EOL);
          }
          catch (Exception e)
          {
            slamdServer       = null;
            scheduler         = null;
            configDB          = null;
            slamdRunning      = false;
            infoMessage.append("ERROR:  Could not start SLAMD:  " + e +
                               "<BR>" + EOL);
            unavailableReason = "Could not create the SLAMD server " +
                                "instance:  " + e.getMessage();
            response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                               "Could not create the SLAMD server instance:  " +
                               e.getMessage());

            htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                            "\">SLAMD Server Startup Failed</SPAN>" + EOL);
            htmlBody.append("<BR><BR>" + EOL);
            htmlBody.append("The SLAMD server could not be started." +
                            EOL);
            htmlBody.append("See the error message above for additional " +
                            "information on the cause of the error message");
          }
        }
        else
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">SLAMD Startup Cancelled</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server was not started." + EOL);

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
        }
      }
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_STOP_SLAMD))
    {
      // If the user doesn't have permission to stop SLAMD, then don't let them
      if (! requestInfo.mayStartStopSLAMD)
      {
        logMessage(requestInfo, "No mayStartStopSLAMD permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "stop the SLAMD server");
        response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                           "You do not have permission to stop the SLAMD " +
                           "server");
        return;
      }

      // If the SLAMD server is already stopped, then there is nothing to do
      if (! slamdRunning)
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">SLAMD Already Stopped</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The SLAMD server was already stopped." + EOL);
        htmlBody.append("Your request to stop it has been ignored." + EOL);
        response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                           "The SLAMD server was already stopped.");

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else
      {
        // If confirmation has not been provided, then get it now
        String confirmStr =
                    request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
        if ((confirmStr == null) ||
            ((! confirmStr.equalsIgnoreCase("yes")) &&
             (! confirmStr.equalsIgnoreCase("no"))))
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Stop SLAMD Server</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Warning:  Stopping the SLAMD server will cancel " +
                          "any jobs that are currently running and " +
                          "disconnect any clients that are connected." + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("Are you sure that you want to stop the SLAMD " +
                          "server?" + EOL);
          htmlBody.append("<BR>");
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                          "\">" + EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_STATUS) +
                          EOL);
          htmlBody.append("  " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_STATUS_STOP_SLAMD) +
                          EOL);
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
          slamdServer.stopSLAMD();
          slamdServer       = null;
          slamdRunning      = false;
          unavailableReason = "The SLAMD server has been stopped.";
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">SLAMD Server Stopped</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server has successfully completed its " +
                          "shutdown process and is no longer running." + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_STATUS) +
                          EOL);
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_STATUS_START_SLAMD) +
                          EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                          "VALUE=\"Start SLAMD\">" + EOL);
          htmlBody.append("</FORM>" + EOL);
        }
        else
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">SLAMD Shutdown Cancelled</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server was not stopped." + EOL);

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
        }
      }
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_RESTART_SLAMD))
    {
      // If the user doesn't have permission to restart SLAMD, then don't let
      // them
      if (! requestInfo.mayStartStopSLAMD)
      {
        logMessage(requestInfo, "No mayStartStopSLAMD permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "restart the SLAMD server");
        response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                           "You do not have permission to restart the SLAMD " +
                           "server");
        return;
      }

      // If confirmation has not been provided, then get it now
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Restart SLAMD Server</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Warning:  Restarting the SLAMD server will cancel " +
                        "any jobs that are currently running and " +
                        "disconnect any clients that are connected." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Are you sure that you want to restart the SLAMD " +
                        "server?" + EOL);
        htmlBody.append("<BR>");
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_RESTART_SLAMD) +
                        EOL);
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
          if (slamdRunning)
          {
            slamdRunning = false;
            slamdServer.stopSLAMD();
            slamdServer = null;
          }

          // Buy a little time to help make sure that all the old connections
          // get cleaned up properly so that the new listener can bind to the
          // port.
          Thread.sleep(3000);

          slamdServer = new SLAMDServer(adminServlet, readOnlyMode,
               configDBDirectory, sslKeyStore, sslKeyStorePassword,
               sslTrustStore, sslTrustStorePassword);

          scheduler     = slamdServer.getScheduler();
          configDB      = slamdServer.getConfigDB();
          configDB.registerAsSubscriber(ADMIN_CONFIG);
          ADMIN_CONFIG.refreshSubscriberConfiguration();
          slamdRunning  = true;

          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">SLAMD Server Restarted</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server has been restarted successfully." +
                          EOL);

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
        }
        catch (Exception e)
        {
          slamdServer       = null;
          scheduler         = null;
          configDB          = null;
          slamdRunning      = false;
          infoMessage.append("ERROR:  Could not start SLAMD:  " + e +
                             "<BR>" + EOL);
          unavailableReason = "Could not create the SLAMD server " +
                              "instance:  " + e.getMessage();
          response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                             "Could not create the SLAMD server instance:  " +
                             e.getMessage());

          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">SLAMD Server Startup Failed</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server could not be started." +
                          EOL);
          htmlBody.append("See the error message above for additional " +
                          "information on the cause of the error message");

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
        }
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">SLAMD Restart Cancelled</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The SLAMD server was not restarted." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_RELOAD_JOBS))
    {
      try
      {
        slamdServer.reloadJobClasses();
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Job Definitions Reloaded</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The job class definitions have been successfully " +
                        "reloaded from the configuration directory." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      catch (SLAMDServerException sse)
      {
        infoMessage.append("Unable to reload job class definitions:  " +
                           sse.getMessage() + "<BR>" + EOL);
        response.addHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE,
                           "Unable to reload job class definitions:  " +
                           sse.getMessage());

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Job Definitions Not Reloaded</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The job class definitions could not be reloaded " +
                        "from the configuration directory." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_RESTART_ACL))
    {
      // If the user doesn't have permission to restart the ACL manager, then
      // don't let them
      if (! requestInfo.mayStartStopAccessManager)
      {
        logMessage(requestInfo,
                   "No mayStartStopAccessManager permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "restart the access control manager.");
        return;
      }

      // If confirmation has not been provided, then get it now
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr == null) ||
          ((! confirmStr.equalsIgnoreCase("yes")) &&
           (! confirmStr.equalsIgnoreCase("no"))))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Restart Access Manager</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Are you sure that you want to restart the access  " +
                        "control manager?" + EOL);
        htmlBody.append("<BR>");
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_RESTART_ACL) +
                        EOL);
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
        accessManager.stopAccessManager();
        accessManager.flushUserCache();
        accessManager = new AccessManager(userDirHost, userDirPort,
                                          userDirBindDN, userDirBindPW,
                                          userDirBase, userIDAttribute,
                                          userDirUseSSL, userDirBlindTrust,
                                          sslKeyStore, sslKeyStorePassword,
                                          sslTrustStore, sslTrustStorePassword);

        registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_FULL,
                    resourceDNFullAccess, false);
        registerACL(requestInfo,
                    Constants.SERVLET_INIT_PARAM_ACCESS_RESTART_SLAMD,
                    resourceDNRestartSLAMD, false);
        registerACL(requestInfo,
                    Constants.SERVLET_INIT_PARAM_ACCESS_RESTART_ACL,
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
        registerACL(requestInfo,
                    Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_STATUS,
                    resourceDNViewStatus, false);
        registerACL(requestInfo,
                    Constants.SERVLET_INIT_PARAM_ACCESS_DISCONNECT_CLIENT,
                    resourceDNDisconnectClient, false);
        registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_JOB,
                    resourceDNViewJob, false);
        registerACL(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_EXPORT_JOB,
                    resourceDNExportJob, false);
        registerACL(requestInfo,
                    Constants.SERVLET_INIT_PARAM_ACCESS_SCHEDULE_JOB,
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

        try
        {
          accessManager.startAccessManager();

          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Access Manager Restarted</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The access control manager has been restarted " +
                          "successfully." + EOL);
        }
        catch (LDAPException le)
        {
          infoMessage.append("ERROR:  Could not start access control " +
                             "manager:  " + le + "<BR>" + EOL);

          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Access Manager Not Restarted</SPAN>" + EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The access control manager could not be restarted." +
                          EOL);
          htmlBody.append("See the error message above for additional " +
                          "information." + EOL);
        }

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Access Manager Restart Cancelled</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The access control manager was not restarted." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
    }
    else if (subsection.equals(
                             Constants.SERVLET_SECTION_STATUS_FLUSH_ACL_CACHE))
    {
      // If the user doesn't have permission to flush the cache, then don't let
      // them
      if (! requestInfo.mayStartStopAccessManager)
      {
        logMessage(requestInfo,
                   "No mayStartStopAccessManager permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "flush the access control cache.");
        return;
      }

      // Flushing the ACL cache is a minimal risk process, so there's no need
      // for confirmation
      accessManager.flushUserCache();
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">ACL Cache Flushed</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("The access control cache has been flushed." + EOL);

      htmlBody.append("<BR><BR>" + EOL);
      String link = generateLink(requestInfo,
                                 Constants.SERVLET_SECTION_STATUS, null,
                                 "Return to the status page.");
      htmlBody.append(link + EOL);
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_DISCONNECT))
    {
      // If the user doesn't have permission to disconnect a client, then
      // don't let them.
      if (! requestInfo.mayDisconnectClients)
      {
        logMessage(requestInfo,
                   "No mayDisconnectClients permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "disconnect clients from the SLAMD server.");
        return;
      }


      // Get the client ID of the client to disconnect.  If none was provided,
      // generate an error.
      String clientID = request.getParameter(Constants.SERVLET_PARAM_CLIENT_ID);
      if ((clientID == null) || (clientID.length() == 0))
      {
        infoMessage.append("ERROR:  No client ID specified.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">No Client ID Specified</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Unable to disconnect the client because no client " +
                        "ID has been specified."  + EOL);
        return;
      }


      // Get the value of the submit string.  If none was specified, then
      // generate a form to display it.
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr != null) &&
          submitStr.equalsIgnoreCase(
                         Constants.SUBMIT_STRING_GRACEFUL_DISCONNECT))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Client " + clientID + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        if (slamdServer.getClientListener().requestDisconnect(clientID))
        {
          htmlBody.append("A request has been sent to disconnect client " +
                          clientID + '.' + EOL);
          htmlBody.append("It may take a little time for the client to " +
                          "respond to the request.");
          htmlBody.append("If the client was busy processing a job, then " +
                          "an attempt will be made to obtain at least " +
                          "partial statistics from that job." + EOL);
        }
        else
        {
          htmlBody.append("Client " + clientID + " could not be disconnected " +
                          "from the SLAMD server." + EOL);
          htmlBody.append("The client listener was unaware of any client " +
                          "with the specified client ID." + EOL);
        }

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(
                              Constants.SUBMIT_STRING_FORCEFUL_DISCONNECT))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Client " + clientID + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        if (slamdServer.getClientListener().forceDisconnect(clientID))
        {
          htmlBody.append("Client " + clientID + " has been disconnected " +
                          "from the SLAMD server." + EOL);
          htmlBody.append("Note that if the client was busy processing a " +
                          "job, then any statistical information associated " +
                          "with the execution of that job on the client will " +
                          "be lost." + EOL);
        }
        else
        {
          htmlBody.append("Client " + clientID + " could not be disconnected " +
                          "from the SLAMD server." + EOL);
          htmlBody.append("The client listener was unaware of any client " +
                          "with the specified client ID." + EOL);
        }

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(Constants.SUBMIT_STRING_CANCEL))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Cancelled</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Client " + clientID + " was not disconnected." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Client " + clientID + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Specify the manner in which the client should be " +
                        "disconnected." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_DISCONNECT) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CLIENT_ID,
                                              clientID) + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_GRACEFUL_DISCONNECT + "\">" +
                        EOL);
        htmlBody.append("  &nbsp;&nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_FORCEFUL_DISCONNECT + "\">" +
                        EOL);
        htmlBody.append("  &nbsp;&nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_CANCEL + "\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
    }
    else if (subsection.equals(
                  Constants.SERVLET_SECTION_STATUS_DISCONNECT_MONITOR))
    {
      // If the user doesn't have permission to disconnect a client, then
      // don't let them.
      if (! requestInfo.mayDisconnectClients)
      {
        logMessage(requestInfo,
                   "No mayDisconnectClients permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
             "disconnect resource monitor clients from the SLAMD server.");
        return;
      }


      // Get the client ID of the client to disconnect.  If none was provided,
      // generate an error.
      String clientID = request.getParameter(Constants.SERVLET_PARAM_CLIENT_ID);
      if ((clientID == null) || (clientID.length() == 0))
      {
        infoMessage.append("ERROR:  No client ID specified.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">No Client ID Specified</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Unable to disconnect the resource monitor client " +
             "because no client ID has been specified."  + EOL);
        return;
      }


      // Get the value of the submit string.  If none was specified, then
      // generate a form to display it.
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr != null) &&
          submitStr.equalsIgnoreCase(
                         Constants.SUBMIT_STRING_GRACEFUL_DISCONNECT))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Client " + clientID + "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        if (slamdServer.getMonitorClientListener().requestDisconnect(clientID))
        {
          htmlBody.append("A request has been sent to disconnect resource " +
               "monitor client " + clientID + '.' + EOL);
          htmlBody.append("It may take a little time for the client to " +
               "respond to the request.");
          htmlBody.append("If the client was busy processing a job, then " +
               "an attempt will be made to obtain at least partial " +
               "statistics from that job." + EOL);
          htmlBody.append("The resource monitor client may automatically " +
               "attempt to re-connect to the server after some period of " +
               "time." + EOL);
        }
        else
        {
          htmlBody.append("Resource monitor client " + clientID +
               " could not be disconnected from the SLAMD server." + EOL);
          htmlBody.append("The client listener was unaware of any monitor " +
               "client with the specified client ID." + EOL);
        }

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(
                              Constants.SUBMIT_STRING_FORCEFUL_DISCONNECT))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
             "\">Disconnect Resource Monitor Client " + clientID + "</SPAN>" +
             EOL);
        htmlBody.append("<BR><BR>" + EOL);
        if (slamdServer.getMonitorClientListener().forceDisconnect(clientID))
        {
          htmlBody.append("Resource monitor client " + clientID +
               " has been disconnected from the SLAMD server." + EOL);
          htmlBody.append("Note that if the monitor client was busy " +
               "processing a job, then any statistical information " +
               "associated with the execution of that job on the client will " +
               "be lost." + EOL);
          htmlBody.append("The resource monitor client may automatically " +
               "attempt to re-connect to the server after some period of " +
               "time." + EOL);
        }
        else
        {
          htmlBody.append("Resource monitor client " + clientID +
               " could not be disconnected from the SLAMD server." + EOL);
          htmlBody.append("The monitor client listener was unaware of any " +
               "resource monitor client with the specified client ID." + EOL);
        }

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(Constants.SUBMIT_STRING_CANCEL))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Cancelled</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Resource monitor client " + clientID + " was not " +
             "disconnected." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
             "\">Disconnect Resource Monitor Client " + clientID + "</SPAN>" +
             EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Specify the manner in which the resource monitor " +
             "client should be disconnected." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
             generateHidden(Constants.SERVLET_PARAM_SECTION,
                  Constants.SERVLET_SECTION_STATUS) + EOL);
        htmlBody.append("  " +
             generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                  Constants.SERVLET_SECTION_STATUS_DISCONNECT_MONITOR) + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CLIENT_ID,
             clientID) + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
             Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
             Constants.SUBMIT_STRING_GRACEFUL_DISCONNECT + "\">" + EOL);
        htmlBody.append("  &nbsp;&nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
             Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
             Constants.SUBMIT_STRING_FORCEFUL_DISCONNECT + "\">" +
             EOL);
        htmlBody.append("  &nbsp;&nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
             Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
             Constants.SUBMIT_STRING_CANCEL + "\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_CONNECT))
    {
      // The user must have permission to disconnect clients in order to do
      // anything here.
      if (! requestInfo.mayDisconnectClients)
      {
        logMessage(requestInfo, "No mayDisconnectClients permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "create or disconnect client connections");
        return;
      }


      // The user wants to send a request to a client manager to have one or
      // more connections started.  First, get the client ID of the client
      // manager to use.
      String clientID = request.getParameter(Constants.SERVLET_PARAM_CLIENT_ID);
      if ((clientID == null) || (clientID.length() == 0))
      {
        infoMessage.append("ERROR:  No client manager specified.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Establish Client Connections</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The SLAMD server cannot request that additional " +
                        "clients be created because no client manager ID was " +
                        "specified.");

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
        return;
      }

      // Get the number of clients that should be created.  If a number was
      // provided, then send a request to create that number of clients.
      // Otherwise, display a form that will allow the user to specify the
      // number of clients.
      int numClients = 0;
      String numClientsStr =
           request.getParameter(Constants.SERVLET_PARAM_JOB_NUM_CLIENTS);
      if ((numClientsStr != null) && (numClientsStr.length() > 0))
      {
        try
        {
          numClients = Integer.parseInt(numClientsStr);
          if (numClients <= 0)
          {
            infoMessage.append("ERROR:  Number of clients must be a positive " +
                               "integer value.<BR>" + EOL);
          }
        }
        catch (NumberFormatException nfe)
        {
          infoMessage.append("ERROR:  Could not interpret \"" + numClientsStr +
                             "\" as a numeric value.<BR>" + EOL);
        }
      }

      if (numClients > 0)
      {
        // Get the appropriate client manager connection.
        ClientManagerConnection cmConn =
             slamdServer.getClientManagerListener().getClientManager(clientID);
        if (cmConn == null)
        {
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Unable to Create Client Connections</SPAN>" +
                          EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server was unable to request that the " +
                          "client connections be created because no client " +
                          "manager was found with client ID \"" + clientID +
                          "\"." + EOL);

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
          return;
        }

        // Send the create client request to the client manager.
        try
        {
          cmConn.startClients(numClients);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Client Connections Created</SPAN>" +
                          EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server has received confirmation that " +
                          "the client manager on system \"" + clientID +
                          "\" has accepted the request and is in the process " +
                          "of creating " + numClients +
                          " new client connections.");

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
          return;
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  Could not create the requested number " +
                             "of client connections -- " + se.getMessage() +
                             "<BR>" + EOL);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Client Connections Not Created</SPAN>" +
                          EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The request to create " + numClients +
                          " client connections on client system \"" + clientID +
                          "\" was rejected by the client manager.");

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
          return;
        }
      }
      else
      {
        // Display the form to the end user.
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Request Client Connections</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Enter the number of clients that should be created " +
                        "on client system " + clientID + '.' + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM CLASS=\"" + Constants.STYLE_MAIN_FORM +
                        "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_CONNECT) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_CLIENT_ID,
                                       clientID) + EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("  Number of Clients:  " + EOL);
        htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                        Constants.SERVLET_PARAM_JOB_NUM_CLIENTS +
                        "\" SIZE=\"40\">" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Create Clients\">" +
                        EOL);
        htmlBody.append("</FORM>" + EOL);
      }
    }
    else if (subsection.equals(
                             Constants.SERVLET_SECTION_STATUS_CONNECT_CLIENTS))
    {
      // The user must have permission to disconnect clients in order to do
      // anything here.
      if (! requestInfo.mayDisconnectClients)
      {
        logMessage(requestInfo, "No mayDisconnectClients permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "create or disconnect client connections");
        return;
      }


      // Get the set of client managers that are currently connected.
      ClientManagerConnection[] cmConns =
           slamdServer.getClientManagerListener().getSortedClientManagers();
      if ((cmConns == null) || (cmConns.length == 0))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Request Client Connections</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("There are currently no client managers connected." +
                        EOL);
        return;
      }


      // The user wants to connect clients across multiple client managers.
      // See if the user has specified the distribution for those new
      // connections.
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr != null) &&
          submitStr.equalsIgnoreCase(Constants.SUBMIT_STRING_CONNECT))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Request Client Connections</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        int totalConns = 0;
        for (int i=0; i < cmConns.length; i++)
        {
          int numConns = 0;
          String numConnsStr =
               request.getParameter(Constants.SERVLET_PARAM_NUM_CLIENTS_PREFIX +
                                    cmConns[i].getClientID());
          if ((numConnsStr != null) && (numConnsStr.length() > 0))
          {
            try
            {
              numConns = Integer.parseInt(numConnsStr);
            }
            catch (Exception e)
            {
              infoMessage.append("ERROR:  Unable to determine the number " +
                                 "of client connections to establish for " +
                                 "client " + cmConns[i].getClientID() +
                                 ".<BR>" + EOL);
            }
          }

          if (numConns > 0)
          {
            try
            {
              cmConns[i].startClients(numConns);
              totalConns += numConns;
              infoMessage.append("Requested " + numConns + " connections for " +
                                 cmConns[i].getClientID() + ".<BR>" + EOL);
            }
            catch (Exception e)
            {
              infoMessage.append("Unable to request " + numConns +
                                 " connections for " +
                                 cmConns[i].getClientID() + ":  " + e +
                                 ".<BR>" + EOL);
            }
          }
        }

        htmlBody.append("Requests have been sent to create " + totalConns +
                        " connections across one or more client managers." +
                        EOL);
        htmlBody.append("It may take some time for those client connections " +
                        "to actually be established." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(Constants.SUBMIT_STRING_CANCEL))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Connect Cancelled</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("No clients connections have been requested." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Request Client Connections</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        // Get the total number of clients to be created.
        int numClients = 0;
        try
        {
          numClients = Integer.parseInt(request.getParameter(
                                    Constants.SERVLET_PARAM_JOB_NUM_CLIENTS));
          if (numClients <= 0)
          {
            infoMessage.append("ERROR:  Number of clients to connect must " +
                               "be greater than zero.<BR>" + EOL);
          }
        }
        catch (Exception e)
        {
          infoMessage.append("ERROR:  Unable to determine the number of " +
                             "client connections to request.<BR>" + EOL);
        }

        // Figure out how many clients should be requested for each manager.
        // First, create an integer array with all values of zero.
        int[] numConns = new int[cmConns.length];
        for (int i=0; i < numConns.length; i++)
        {
          numConns[i] = 0;
        }

        boolean keepGoing        = true;
        int     clientsRemaining = numClients;
        while (keepGoing && (clientsRemaining > 0))
        {
          keepGoing = false;
          for (int i=0; ((clientsRemaining > 0) && (i < cmConns.length)); i++)
          {
            int maxClients = cmConns[i].getMaxClients();
            if (maxClients <= 0)
            {
              keepGoing = true;
              numConns[i]++;
              clientsRemaining--;
            }
            else
            {
              int currentClients = cmConns[i].getStartedClients();
              if ((currentClients + numConns[i] + 1) <= maxClients)
              {
                keepGoing = true;
                numConns[i]++;
                clientsRemaining--;
              }
            }
          }
        }

        if (! keepGoing)
        {
          infoMessage.append("WARNING:  The number of clients requested (" +
                             numClients + ") was greater than the total " +
                             "number of clients that may be handled by the " +
                             "set of client managers that are currently " +
                             "connected.<BR>" + EOL);
        }

        // Generate the form that allows the user to specify how the requests
        // should be distributed.
        htmlBody.append("Specify the number of client connections that " +
                        "should be established for each client system." + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        htmlBody.append("<FORM CLASS=\"" +
                        Constants.STYLE_MAIN_FORM +
                        "\" METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                    Constants.SERVLET_SECTION_STATUS) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_CONNECT_CLIENTS) +
                        EOL);

        htmlBody.append("<TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD><B>Client ID</B>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD><B>Current Connections</B></TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD><B>Max Connections</B></TD>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD><B>Connections to Create</B></TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        for (int i=0; i < cmConns.length; i++)
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>" + cmConns[i].getClientID() + "</TD>" +
                          EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + cmConns[i].getStartedClients() +
                          "</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + cmConns[i].getMaxClients() + "</TD>" +
                          EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);

          if ((cmConns[i].getMaxClients() > 0) &&
              (cmConns[i].getStartedClients() >= cmConns[i].getMaxClients()))
          {
            htmlBody.append("    <TD>N/A</TD>" + EOL);
          }
          else
          {
            htmlBody.append("    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                            Constants.SERVLET_PARAM_NUM_CLIENTS_PREFIX +
                            cmConns[i].getClientID() + "\" VALUE=\"" +
                            String.valueOf(numConns[i]) + "\" SIZE=\"5\">" +
                            "</TD>" + EOL);
          }

          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("</TABLE>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_CONNECT + "\">" + EOL);
        htmlBody.append("  &nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_CANCEL + "\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
    }
    else if (subsection.equals(Constants.SERVLET_SECTION_STATUS_DISCONNECT_ALL))
    {
      // The user must have permission to disconnect clients in order to do
      // anything here.
      if (! requestInfo.mayDisconnectClients)
      {
        logMessage(requestInfo, "No mayDisconnectClients permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "create or disconnect client connections");
        return;
      }


      // The user wants to disconnect all clients associated with a particular
      // client manager.  First, get the client ID of the client manager to use.
      String clientID = request.getParameter(Constants.SERVLET_PARAM_CLIENT_ID);
      if ((clientID == null) || (clientID.length() == 0))
      {
        infoMessage.append("ERROR:  No client manager specified.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Clients</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The SLAMD server cannot request that the clients be " +
                        "disconnected because no client manager ID was " +
                        "specified.");

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
        return;
      }


      // Get the appropriate client manager connection.
      ClientManagerConnection cmConn =
           slamdServer.getClientManagerListener().getClientManager(clientID);
      if (cmConn == null)
      {
        infoMessage.append("ERROR:  No client manager specified.<BR>" + EOL);
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Clients</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("The SLAMD server cannot request that the clients be " +
                        "disconnected because no client manager was found " +
                        "with client ID \"" + clientID + "\".");

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
        return;
      }


      // See if confirmation has been provided.  If so, then act on it
      // accordingly.  If not, then obtain that confirmation from the end user.
      String confirmStr =
                  request.getParameter(Constants.SERVLET_PARAM_CONFIRMED);
      if ((confirmStr != null) && confirmStr.equalsIgnoreCase("yes"))
      {
        // Send a request to stop all the clients.
        try
        {
          cmConn.stopClients(-1);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Client Connections Disconnected</SPAN>" +
                          EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The SLAMD server has received confirmation that " +
                          "the client manager on system \"" + clientID +
                          "\" has accepted the request and is in the process " +
                          "of terminating all its client connections.");

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
          return;
        }
        catch (SLAMDException se)
        {
          infoMessage.append("ERROR:  Could not terminate the client " +
                             "connections -- " + se.getMessage() + "<BR>" +
                             EOL);
          htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                          "\">Client Connections Not Disconnected</SPAN>" +
                          EOL);
          htmlBody.append("<BR><BR>" + EOL);
          htmlBody.append("The request to terminate the client connections " +
                          "on client system \"" + clientID +
                          "\" was rejected by the client manager.");

          htmlBody.append("<BR><BR>" + EOL);
          String link = generateLink(requestInfo,
                                     Constants.SERVLET_SECTION_STATUS, null,
                                     "Return to the status page.");
          htmlBody.append(link + EOL);
          return;
        }
      }
      else if ((confirmStr != null) && confirmStr.equalsIgnoreCase("no"))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnected Cancelled</SPAN>" +
                        EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("No client connections have been terminated.");

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
        return;
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Clients for " + clientID + "</SPAN>" +
                        EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Client manager " + clientID + " reports that it has " +
                        cmConn.getStartedClients() +
                        " clients currently running." + EOL);
        htmlBody.append("Disconnecting these clients will cause them to " +
                        "immediately stop working on any active jobs, and " +
                        "any statistics they may have for that job will be " +
                        "lost." + EOL);
        htmlBody.append("Are you sure that you want to disconnect these " +
                        "clients?" + EOL);
        htmlBody.append("<BR>");
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_DISCONNECT_ALL) +
                        EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_CLIENT_ID,
                                              clientID) + EOL);
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
    else if (subsection.equals(
                  Constants.SERVLET_SECTION_STATUS_DISCONNECT_ALL_CLIENTS))
    {
      // The user must have permission to disconnect clients in order to do
      // anything here.
      if (! requestInfo.mayDisconnectClients)
      {
        logMessage(requestInfo, "No mayDisconnectClients permission granted");
        generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                                 "create or disconnect client connections");
        return;
      }


      // The user wants to disconnect all client connections registered with the
      // SLAMD server.  See if the disconnect should be graceful or forceful, or
      // if that has not yet been specified.
      String submitStr = request.getParameter(Constants.SERVLET_PARAM_SUBMIT);
      if ((submitStr != null) &&
          submitStr.equalsIgnoreCase(
                         Constants.SUBMIT_STRING_GRACEFUL_DISCONNECT))
      {
        slamdServer.getClientListener().requestDisconnectAll();

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect All Clients</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("A request has been sent to disconnect all clients." +
                        EOL);
        htmlBody.append("It may take a little time for the clients to " +
                        "respond to the request.");
        htmlBody.append("If any clients were busy processing a job, then " +
                        "an attempt will be made to obtain at least " +
                        "partial statistics from that job." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(
                              Constants.SUBMIT_STRING_FORCEFUL_DISCONNECT))
      {
        slamdServer.getClientListener().forcefullyDisconnectAll();

        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect All Clients</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("All clients have been disconnected from the SLAMD " +
                        "server." + EOL);
        htmlBody.append("Note that if the client was busy processing a " +
                        "job, then any statistical information associated " +
                        "with the execution of that job on the client will " +
                        "be lost." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else if ((submitStr != null) &&
               submitStr.equalsIgnoreCase(Constants.SUBMIT_STRING_CANCEL))
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect Cancelled</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("No clients have been disconnected." + EOL);

        htmlBody.append("<BR><BR>" + EOL);
        String link = generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS, null,
                                   "Return to the status page.");
        htmlBody.append(link + EOL);
      }
      else
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">Disconnect All Clients</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("Specify the manner in which the clients should be " +
                        "disconnected." + EOL);
        htmlBody.append("<BR><BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) + EOL);
        htmlBody.append("  " +
             generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                  Constants.SERVLET_SECTION_STATUS_DISCONNECT_ALL_CLIENTS) +
                        EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_GRACEFUL_DISCONNECT + "\">" +
                        EOL);
        htmlBody.append("  &nbsp;&nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_FORCEFUL_DISCONNECT + "\">" +
                        EOL);
        htmlBody.append("  &nbsp;&nbsp;" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                        Constants.SERVLET_PARAM_SUBMIT + "\" VALUE=\"" +
                        Constants.SUBMIT_STRING_CANCEL + "\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
    }
    else if (subsection.equals(
                  Constants.SERVLET_SECTION_STATUS_GET_STATUS_AS_TEXT))
    {
      response.setContentType("text/plain");
      requestInfo.generateHTML = false;

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


      // Print the SLAMD server version information.
      String buildDate;
      try
      {
        Date d = dateFormat.parse(DynamicConstants.BUILD_DATE);
        buildDate = displayDateFormat.format(d);
      }
      catch (Exception e)
      {
        buildDate = DynamicConstants.BUILD_DATE;
      }

      writer.println("SLAMD Server Version:  " +
                     DynamicConstants.SLAMD_VERSION +
                     (DynamicConstants.OFFICIAL_BUILD
                      ? "" : " (Unofficial Build)"));
      writer.println("SLAMD Server Build Date:  " + buildDate);
      writer.println();


      // Print the current state of the SLAMD server.
      if (slamdRunning)
      {
        writer.println("SLAMD State:  ONLINE");
        writer.println("Startup Time:  " +
                       displayDateFormat.format(slamdServer.getStartupTime()));
        writer.println("Current Time:  " +
                       displayDateFormat.format(new Date()));

        int upSecs = (int) ((System.currentTimeMillis() -
                             slamdServer.getStartupTime().getTime()) / 1000);
        int upDays = (upSecs / 86400);
        upSecs -= (86400 * upDays);
        int upHours = (upSecs / 3600);
        upSecs -= (3600 * upHours);
        int upMins = (upSecs / 60);
        upSecs -= (60 * upMins);
        writer.println("Uptime:  " + upDays + "d " + upHours + "h " + upMins +
                       "m " + upSecs + 's');

        writer.println();
        writer.println("Jobs Scheduled Since Startup:  " +
                       scheduler.getTotalScheduledJobCount());
        writer.println("Jobs Completed Since Startup:  " +
                       scheduler.getCompletedJobCount());
        writer.println("Jobs Cancelled Since Startup:  " +
                       scheduler.getCancelledJobCount());
        writer.println("Jobs Currently Pending Execution:  " +
                       scheduler.getPendingJobCount());
        writer.println("Jobs Currently Running:  " +
                       scheduler.getRunningJobCount());
        writer.println();

        ClientManagerConnection[] managerConns =
             slamdServer.getClientManagerListener().getSortedClientManagers();
        for (int i=0; i < managerConns.length; i++)
        {
          writer.println("Client Manager:  " + managerConns[i].getClientID());
        }

        if (managerConns.length > 0)
        {
          writer.println();
        }

        ClientConnection[] clientConns =
             slamdServer.getClientListener().getSortedConnectionList();
        for (int i=0; i < clientConns.length; i++)
        {
          writer.println("Client:  " + clientConns[i].getClientID());
        }

        if (clientConns.length > 0)
        {
          writer.println();
        }

        ResourceMonitorClientConnection[] monitorConns =
             slamdServer.getMonitorClientListener().
                              getSortedMonitorClientList();
        for (int i=0; i < monitorConns.length; i++)
        {
          writer.println("Resource Monitor Client:  " +
                         monitorConns[i].getClientID());
        }

        if (monitorConns.length > 0)
        {
          writer.println();
        }
      }
      else
      {
        writer.println("SLAMD State:  OFFLINE");
        writer.println("Current Time:  " +
                       displayDateFormat.format(new Date()));
        writer.println();
      }


      // Print JVM statistics
      Runtime runtime = Runtime.getRuntime();
      writer.println("Java Version:  " + System.getProperty("java.version"));
      writer.println("Java Installation:  " + System.getProperty("java.home"));
      writer.println("Operating System Name:  " +
                     System.getProperty("os.name"));
      writer.println("Operating System Version:  " +
                     System.getProperty("os.version"));
      writer.println("Total CPUs Available to JVM:  " +
                     runtime.availableProcessors());
      writer.println("Total Memory Available to JVM:  " + runtime.maxMemory());
      writer.println("Memory Currently Held by JVM:  " + runtime.totalMemory());
      writer.println("Unused Memory Held by JVM:  " + runtime.freeMemory());
    }
    else
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">SLAMD Distributed Load Generation Engine " +
                      "Status</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append(EOL);

      // The refresh button
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_STATUS) +
                      EOL);
      if (requestInfo.debugHTML)
      {
        htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
      }
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
      htmlBody.append("</FORM>" + EOL);

      // Show the SLAMD server status
      htmlBody.append("<B>Server Status</B>" + EOL);
      htmlBody.append("<BR>" + EOL);
      if (slamdRunning)
      {
        htmlBody.append("<TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>SLAMD Server Version</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      " + DynamicConstants.SLAMD_VERSION + EOL);
        if (! DynamicConstants.OFFICIAL_BUILD)
        {
          htmlBody.append("      (Unofficial Build)" + EOL);
        }
        htmlBody.append("    </TD>" + EOL);

        String buildDate;
        try
        {
          Date d = dateFormat.parse(DynamicConstants.BUILD_DATE);
          buildDate = displayDateFormat.format(d);
        }
        catch (Exception e)
        {
          buildDate = DynamicConstants.BUILD_DATE;
        }
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>SLAMD Server Build Date</TD>" + EOL);
        htmlBody.append("    <TD>" + buildDate + "</TD>" +
                        EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>SLAMD Server is </TD>" + EOL);
        htmlBody.append("    <TD><B>ONLINE</B></TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>Online Since</TD>" + EOL);
        htmlBody.append("    <TD>" +
                        displayDateFormat.format(slamdServer.getStartupTime()) +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>Current Time</TD>" + EOL);
        htmlBody.append("    <TD>" +
                        displayDateFormat.format(new Date()) +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        int upSecs = (int) ((System.currentTimeMillis() -
                             slamdServer.getStartupTime().getTime()) / 1000);
        int upDays = (upSecs / 86400);
        upSecs -= (86400 * upDays);
        int upHours = (upSecs / 3600);
        upSecs -= (3600 * upHours);
        int upMins = (upSecs / 60);
        upSecs -= (60 * upMins);

        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>Uptime</TD>" + EOL);

        htmlBody.append("    <TD>" + upDays + "d " + upHours + "h " + upMins +
                        "m " + upSecs + "s</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        // If the user is an administrative user, then allow them to enable or
        // disable HTML debugging.
        if (requestInfo.hasFullAccess)
        {
          String link;
          if (requestInfo.debugHTML)
          {
            link = "<A HREF=\"" +  requestInfo.servletBaseURI + '?' +
                   Constants.SERVLET_PARAM_SECTION + '=' +
                   Constants.SERVLET_SECTION_STATUS + "\">Enabled</A>";
          }
          else
          {
            link = "<A HREF=\"" +  requestInfo.servletBaseURI + '?' +
                   Constants.SERVLET_PARAM_SECTION + '=' +
                   Constants.SERVLET_SECTION_STATUS + '&' +
                   Constants.SERVLET_PARAM_HTML_DEBUG + "=1\">Disabled</A>";
          }

          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>HTML Debugging</TD>" + EOL);
          htmlBody.append("    <TD>" + link + "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
          htmlBody.append("<BR><HR><BR>" + EOL);
        }

        if (requestInfo.mayStartStopSLAMD)
        {
          // The "Stop SLAMD" button
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_STATUS) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_STATUS_STOP_SLAMD) +
                          EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                          "VALUE=\"Stop SLAMD\">" + EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);

          // The "Restart SLAMD" button
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_STATUS) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_STATUS_RESTART_SLAMD) +
                          EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                          "VALUE=\"Restart SLAMD\">" + EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);

          // The "Reload Job Classes" button
          if (slamdServer.useCustomClassLoader())
          {
            htmlBody.append("  <TR>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + EOL);
            htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                            servletBaseURI + "\">" + EOL);
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_SECTION,
                                           Constants.SERVLET_SECTION_STATUS) +
                            EOL);
            htmlBody.append("        " +
                            generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                 Constants.SERVLET_SECTION_STATUS_RELOAD_JOBS) +
                            EOL);
            if (requestInfo.debugHTML)
            {
              htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                             "1") + EOL);
            }
            htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                            "VALUE=\"Reload Job Classes\">" + EOL);
            htmlBody.append("      </FORM>" + EOL);
            htmlBody.append("    </TD>" + EOL);
            htmlBody.append("  </TR>" + EOL);
          }
        }

        // The "View SLAMD Log" button
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_VIEW_LOG) +
                        EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"View SLAMD Log\">" + EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        htmlBody.append("</TABLE>" + EOL);
      }
      else
      {
        htmlBody.append("<TABLE BORDER=\"0\">" + EOL);
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>SLAMD Server is </TD>" + EOL);
        htmlBody.append("    <TD><B>OFFLINE</B></TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        if (requestInfo.mayStartStopSLAMD)
        {
          // The "Start SLAMD" button
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + EOL);
          htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                          servletBaseURI + "\">" + EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SECTION,
                                         Constants.SERVLET_SECTION_STATUS) +
                          EOL);
          htmlBody.append("        " +
                          generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                               Constants.SERVLET_SECTION_STATUS_START_SLAMD) +
                          EOL);
          if (requestInfo.debugHTML)
          {
            htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                           "1") + EOL);
          }
          htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                          "VALUE=\"Start SLAMD\">" + EOL);
          htmlBody.append("      </FORM>" + EOL);
          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("</TABLE>" + EOL);
      }


      if (slamdRunning && (! readOnlyMode))
      {
        // Show the scheduler status
        htmlBody.append("<BR><HR><BR>" + EOL);
        htmlBody.append("<B>Scheduler Status</B>" + EOL);
        htmlBody.append("<BR>" + EOL);
        htmlBody.append("<TABLE CELLSPACING=\"0\" BORDER=\"0\">" + EOL);
        htmlBody.append("  <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        htmlBody.append("    <TD>Jobs scheduled since SLAMD startup</TD>" +
                        EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + scheduler.getTotalScheduledJobCount() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        htmlBody.append("    <TD>Jobs completed since SLAMD startup</TD>" +
                        EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + scheduler.getCompletedJobCount() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        htmlBody.append("    <TD>Jobs cancelled since SLAMD startup</TD>" +
                        EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + scheduler.getCancelledJobCount() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
        if (requestInfo.mayViewJob)
        {
          String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                     Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                                     "pending execution");
          htmlBody.append("    <TD>Jobs currently " + link + "</TD>" + EOL);
        }
        else
        {
          htmlBody.append("    <TD>Jobs currently pending execution</TD>" +
                          EOL);
        }
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + scheduler.getPendingJobCount() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
        htmlBody.append("  <TR CLASS=\"" +
                        Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
        if (requestInfo.mayViewJob)
        {
          String link = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                     Constants.SERVLET_SECTION_JOB_VIEW_RUNNING,
                                     "running");
          htmlBody.append("    <TD>Jobs currently " + link + "</TD>" + EOL);
        }
        else
        {
          htmlBody.append("    <TD>Jobs currently running</TD>" + EOL);
        }
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + scheduler.getRunningJobCount() +
                        "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        Job[] recentlyCompletedJobs = scheduler.getRecentlyCompletedJobs();
        if ((recentlyCompletedJobs != null) &&
            (recentlyCompletedJobs.length > 0))
        {
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
          htmlBody.append("    <TD VALIGN=\"TOP\">Most Recently Completed " +
                          "Jobs</TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD>" + EOL);

          for (int i=0; i < recentlyCompletedJobs.length; i++)
          {
            String jobID;
            if (requestInfo.mayViewJob)
            {
              jobID = generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_GENERIC,
                                   Constants.SERVLET_PARAM_JOB_ID,
                                   recentlyCompletedJobs[i].getJobID(),
                                   recentlyCompletedJobs[i].getJobID());
            }
            else
            {
              jobID = recentlyCompletedJobs[i].getJobID();
            }

            String description = recentlyCompletedJobs[i].getJobDescription();
            if (description == null)
            {
              description = recentlyCompletedJobs[i].getJobClassName();
            }

            htmlBody.append("      " + jobID + " - " + description + "<BR>" +
                            EOL);
          }

          htmlBody.append("    </TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }

        htmlBody.append("</TABLE>" + EOL);


        // Show the client manager listener status
        htmlBody.append("<BR><HR><BR>" + EOL);
        ClientManagerConnection[] cmConns =
          slamdServer.getClientManagerListener().getSortedClientManagers();
        if ((cmConns == null) || (cmConns.length == 0))
        {
          htmlBody.append("<B>Client Manager Listener Status</B>" + EOL);
          htmlBody.append("<BR>" + EOL);
          htmlBody.append("There are currently no client managers connected." +
                          EOL);
        }
        else
        {
          htmlBody.append("<B>Client Manager Listener Status (" +
                          cmConns.length + " connected)</B>" + EOL);
          htmlBody.append("<TABLE CELLSPACING=\"0\" BORDER=\"0\">" + EOL);
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          htmlBody.append("    <TD><B>Client ID</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Client Address</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Time Established</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Active Clients</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Max Clients</B></TD>" + EOL);
          if (requestInfo.mayDisconnectClients)
          {
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          }
          htmlBody.append("  </TR>" + EOL);

          for (int i=0; i < cmConns.length; i++)
          {
            if ((i % 2) == 0)
            {
              htmlBody.append("  <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("  <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }

            htmlBody.append("    <TD>" + cmConns[i].getClientID() + "</TD>" +
                            EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + cmConns[i].getClientIPAddress() +
                            "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" +
                 displayDateFormat.format(cmConns[i].getEstablishedTime()) +
                            "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + cmConns[i].getStartedClients() +
                            "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);

            int maxClients = cmConns[i].getMaxClients();
            if (maxClients > 0)
            {
              htmlBody.append("    <TD>" + maxClients + "</TD>" + EOL);
            }
            else
            {
              htmlBody.append("    <TD>unlimited</TD>" + EOL);
            }

            if (requestInfo.mayDisconnectClients)
            {
              htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
              String link =
                   generateLink(requestInfo, Constants.SERVLET_SECTION_STATUS,
                                Constants.SERVLET_SECTION_STATUS_CONNECT,
                                Constants.SERVLET_PARAM_CLIENT_ID,
                                cmConns[i].getClientID(), "Request Clients");
              htmlBody.append("    <TD>" + link + "</TD>" + EOL);

              htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
              if (cmConns[i].getStartedClients() > 0)
              {
                link =
                     generateLink(requestInfo, Constants.SERVLET_SECTION_STATUS,
                          Constants.SERVLET_SECTION_STATUS_DISCONNECT_ALL,
                          Constants.SERVLET_PARAM_CLIENT_ID,
                          cmConns[i].getClientID(), "Disconnect All");
                htmlBody.append("    <TD>" + link + "</TD>" + EOL);
              }
              else
              {
                htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
              }
            }
            htmlBody.append("  </TR>" + EOL);
          }

          htmlBody.append("</TABLE>" + EOL);

          if (requestInfo.mayDisconnectClients)
          {
            htmlBody.append("<BR>" + EOL);
            htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                            "\">" + EOL);
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_SECTION,
                                           Constants.SERVLET_SECTION_STATUS) +
                            EOL);
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                            Constants.SERVLET_SECTION_STATUS_CONNECT_CLIENTS) +
                            EOL);
            htmlBody.append("  Request &nbsp;" + EOL);
            htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                            Constants.SERVLET_PARAM_JOB_NUM_CLIENTS +
                            "\" SIZE=\"5\">" + EOL);
            htmlBody.append("  &nbsp; clients." + EOL);
            htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Submit\">" + EOL);
            htmlBody.append("</FORM>" + EOL);
          }
        }


        // Show the client listener status
        htmlBody.append("<BR><HR><BR>" + EOL);
        ClientConnection[] conns =
             slamdServer.getClientListener().getSortedConnectionList();
        if ((conns == null) || (conns.length == 0))
        {
          htmlBody.append("<B>Client Listener Status</B>" + EOL);
          htmlBody.append("<BR>" + EOL);
          htmlBody.append("There are currently no clients connected." + EOL);
        }
        else
        {
          int numIdle = 0;
          for (int i=0; i < conns.length; i++)
          {
            if (conns[i].getJob() == null)
            {
              numIdle++;
            }
          }
          htmlBody.append("<B>Client Listener Status (Total=" + conns.length +
                          ", Idle=" + numIdle + ", Busy=" +
                          (conns.length-numIdle) + ")</B>" + EOL);
          htmlBody.append("<BR>" + EOL);

          htmlBody.append("<TABLE CELLSPACING=\"0\" BORDER=\"0\">" + EOL);
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          htmlBody.append("    <TD><B>Connection ID</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Client Address</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Time Established</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Current State</B></TD>" + EOL);
          if (requestInfo.mayDisconnectClients)
          {
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          }
          htmlBody.append("  </TR>" + EOL);
          for (int i=0; i < conns.length; i++)
          {
            if (i % 2 == 0)
            {
              htmlBody.append("  <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("  <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }

            htmlBody.append("    <TD>" + conns[i].getClientID() + "</TD>" +
                            EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + conns[i].getClientIPAddress() +
                            "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" +
                 displayDateFormat.format(conns[i].getEstablishedTime()) +
                            "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + conns[i].getStatusString() + "</TD>" +
                            EOL);
            if (requestInfo.mayDisconnectClients)
            {
              htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
              htmlBody.append("    <TD>" +
                              generateLink(requestInfo,
                                   Constants.SERVLET_SECTION_STATUS,
                                   Constants.SERVLET_SECTION_STATUS_DISCONNECT,
                                   Constants.SERVLET_PARAM_CLIENT_ID,
                                   conns[i].getClientID(), "Disconnect") +
                              "</TD>" + EOL);
            }
            htmlBody.append("  </TR>" + EOL);
          }
          htmlBody.append("</TABLE>" + EOL);

          if (requestInfo.mayDisconnectClients)
          {
            htmlBody.append("<BR>" + EOL);
            htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                            "\">" + EOL);
            htmlBody.append("  " +
                            generateHidden(Constants.SERVLET_PARAM_SECTION,
                                           Constants.SERVLET_SECTION_STATUS) +
                            EOL);
            htmlBody.append("  " +
                 generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                      Constants.SERVLET_SECTION_STATUS_DISCONNECT_ALL_CLIENTS) +
                 EOL);
            htmlBody.append("  <INPUT TYPE=\"SUBMIT\" " +
                            "VALUE=\"Disconnect All\">" + EOL);
            htmlBody.append("</FORM>" + EOL);
          }
        }


        // Show the resource monitor client listener status
        htmlBody.append("<BR><HR><BR>" + EOL);
        ResourceMonitorClientConnection[] monitorConns =
             slamdServer.getMonitorClientListener().
                              getSortedMonitorClientList();
        if ((monitorConns == null) || (monitorConns.length == 0))
        {
          htmlBody.append("<B>Resource Monitor Listener Status</B>" + EOL);
          htmlBody.append("<BR>" + EOL);
          htmlBody.append("There are currently no resource monitor clients " +
                          "connected." + EOL);
        }
        else
        {
          int numIdle = 0;
          for (int i=0; i < monitorConns.length; i++)
          {
            if ((monitorConns[i].getJobIDsInProgress() == null) ||
                (monitorConns[i].getJobIDsInProgress().length == 0))
            {
              numIdle++;
            }
          }
          htmlBody.append("<B>Resource Monitor Listener Status (Total=" +
                          monitorConns.length + ", Idle=" + numIdle +
                          ", Busy=" + (monitorConns.length-numIdle) + ")</B>" +
                          EOL);
          htmlBody.append("<BR>" + EOL);

          htmlBody.append("<TABLE CELLSPACING=\"0\" BORDER=\"0\">" + EOL);
          htmlBody.append("  <TR CLASS=\"" +
                          Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
          htmlBody.append("    <TD><B>Connection ID</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Client Address</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Time Established</B></TD>" + EOL);
          htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          htmlBody.append("    <TD><B>Current State</B></TD>" + EOL);
          if (requestInfo.mayDisconnectClients)
          {
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
          }
          htmlBody.append("  </TR>" + EOL);
          for (int i=0; i < monitorConns.length; i++)
          {
            if (i % 2 == 0)
            {
              htmlBody.append("  <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
            }
            else
            {
              htmlBody.append("  <TR CLASS=\"" +
                              Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
            }

            htmlBody.append("    <TD>" + monitorConns[i].getClientID() +
                            "</TD>" +
                            EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + monitorConns[i].getClientIPAddress() +
                            "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" +
                 displayDateFormat.format(
                      monitorConns[i].getEstablishedTime()) + "</TD>" + EOL);
            htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
            htmlBody.append("    <TD>" + monitorConns[i].getStatusString() +
                            "</TD>" + EOL);
            if (requestInfo.mayDisconnectClients)
            {
              htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
              htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
              htmlBody.append("    <TD>" + generateLink(requestInfo,
                   Constants.SERVLET_SECTION_STATUS,
                   Constants.SERVLET_SECTION_STATUS_DISCONNECT_MONITOR,
                   Constants.SERVLET_PARAM_CLIENT_ID,
                   monitorConns[i].getClientID(), "Disconnect") +
                   "</TD>" + EOL);
            }
            htmlBody.append("  </TR>" + EOL);
          }
          htmlBody.append("</TABLE>" + EOL);
        }
      }


      // Show the access control manager section
      if ((useAccessControl) && (requestInfo.mayStartStopAccessManager))
      {
        htmlBody.append("<BR><HR><BR>" + EOL);
        htmlBody.append("<B>Access Control Manager</B>" + EOL);
        htmlBody.append("<BR>" + EOL);
        htmlBody.append("<TABLE CELLSPACING=\"0\" BORDER=\"0\">" + EOL);

        // The "Restart ACL Manager" Button
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_RESTART_ACL) +
                        EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Restart ACL Manager\">" + EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        // The "Flush ACL Cache" Button
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>&nbsp;</TD>" + EOL);
        htmlBody.append("    <TD>" + EOL);
        htmlBody.append("      <FORM METHOD=\"POST\" ACTION=\"" +
                        servletBaseURI + "\">" + EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SECTION,
                                       Constants.SERVLET_SECTION_STATUS) +
                        EOL);
        htmlBody.append("        " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_STATUS_FLUSH_ACL_CACHE) +
                        EOL);
        if (requestInfo.debugHTML)
        {
          htmlBody.append(generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                         "1") + EOL);
        }
        htmlBody.append("        <INPUT TYPE=\"SUBMIT\" " +
                        "VALUE=\"Flush ACL Cache\">" + EOL);
        htmlBody.append("      </FORM>" + EOL);
        htmlBody.append("    </TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);

        htmlBody.append("</TABLE>" + EOL);
      }


      // Show the JVM statistical information.
      Runtime runtime = Runtime.getRuntime();
      htmlBody.append("<BR><HR><BR>" + EOL);
      htmlBody.append("<B>SLAMD Server JVM Statistics</B>" + EOL);
      htmlBody.append("<BR>" + EOL);
      htmlBody.append("<TABLE CELLSPACING=\"0\" BORDER=\"0\">" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Java Version</TD>" + EOL);
      htmlBody.append("    <TD>" + System.getProperty("java.version") +
                      "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
      htmlBody.append("    <TD>Java Installation</TD>" + EOL);
      htmlBody.append("    <TD>" + System.getProperty("java.home") + "</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Operating System Name</TD>" + EOL);
      htmlBody.append("    <TD>" + System.getProperty("os.name") + "</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
      htmlBody.append("    <TD>Operating System Version</TD>" + EOL);
      htmlBody.append("    <TD>" + System.getProperty("os.version") + "</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Total CPUs Available to JVM</TD>" + EOL);
      htmlBody.append("    <TD>" + runtime.availableProcessors() + "</TD>" +
                      EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
      htmlBody.append("    <TD>Total Memory Available to JVM</TD>" + EOL);
      htmlBody.append("    <TD>" + runtime.maxMemory() + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_A + "\">" + EOL);
      htmlBody.append("    <TD>Memory Currently Held by JVM</TD>" + EOL);
      htmlBody.append("    <TD>" + runtime.totalMemory() + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("  <TR CLASS=\"" +
                      Constants.STYLE_JOB_SUMMARY_LINE_B + "\">" + EOL);
      htmlBody.append("    <TD>Unused Memory Currently Held by JVM</TD>" + EOL);
      htmlBody.append("    <TD>" + runtime.freeMemory() + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
      htmlBody.append("</TABLE>" + EOL);
    }
  }



  /**
   * Handle all processing required to display information from the SLAMD log
   * file to the user.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleViewLog(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleViewLog()");

    // The user must have at least view status permission to access anything in
    // this section.
    if (! requestInfo.mayViewStatus)
    {
      logMessage(requestInfo, "No mayViewStatus permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view the SLAMD log file.");
      return;
    }


    // Get the important state variables.
    HttpServletRequest request     = requestInfo.request;
    String             baseURI     = requestInfo.servletBaseURI;
    StringBuilder       htmlBody    = requestInfo.htmlBody;
    StringBuilder       infoMessage = requestInfo.infoMessage;


    // Determine how much of the log to show.
    boolean showAll  = false;
    int     numLines = Constants.DEFAULT_LOG_VIEW_LINES;

    String showAllStr =
               request.getParameter(Constants.SERVLET_PARAM_LOG_VIEW_ALL);
    if (showAllStr != null)
    {
      showAll = (showAllStr.equalsIgnoreCase("true") ||
                 showAllStr.equalsIgnoreCase("yes") ||
                 showAllStr.equalsIgnoreCase("on") ||
                 showAllStr.equalsIgnoreCase("1"));
    }

    String linesStr =
                request.getParameter(Constants.SERVLET_PARAM_LOG_VIEW_LINES);
    if (linesStr != null)
    {
      try
      {
        numLines = Integer.parseInt(linesStr);
      } catch (Exception e) {}
    }


    // Add the page header.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">SLAMD Server Status</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append(EOL);


    // Add the form that allows the user to select how much to display.
    htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + baseURI + "\">" + EOL);
    htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                          Constants.SERVLET_SECTION_STATUS) +
                    EOL);
    htmlBody.append("  " +
                    generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                                   Constants.SERVLET_SECTION_STATUS_VIEW_LOG) +
                    EOL);
    if (requestInfo.debugHTML)
    {
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                            "1") + EOL);
    }
    htmlBody.append("  Number of lines to display:  " + EOL);
    htmlBody.append("  <INPUT TYPE=\"TEXT\" NAME=\"" +
                    Constants.SERVLET_PARAM_LOG_VIEW_LINES + "\" VALUE=\"" +
                    numLines + "\" SIZE=\"5\">" + EOL);
    htmlBody.append("  <BR>" + EOL);
    htmlBody.append("  <INPUT TYPE=\"CHECKBOX\" NAME=\"" +
                    Constants.SERVLET_PARAM_LOG_VIEW_ALL +
                    (showAll ? "\" CHECKED>" : "\">") +
                    "Show all lines of the log file" + EOL);
    htmlBody.append("  <BR>" + EOL);
    htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Refresh\">" + EOL);
    htmlBody.append("</FORM>" + EOL);
    htmlBody.append("<BR><HR><BR>");
    htmlBody.append(EOL);


    // Add the appropriate data from the log file.
    try
    {
      String logFile =
           configDB.getConfigParameter(Constants.PARAM_LOG_FILENAME);
      if (logFile == null)
      {
        logFile = Constants.DEFAULT_LOG_FILENAME;
      }

      BufferedReader reader = new BufferedReader(new FileReader(logFile));
      String line;
      LinkedList<String> lineList = new LinkedList<String>();
      int        listSize = 0;
      htmlBody.append(Constants.HTML_COMMENT_LOG_START + EOL);
      while ((line = reader.readLine()) != null)
      {
        if (showAll)
        {
          htmlBody.append(line + "<BR>" + EOL);
        }
        else
        {
          lineList.add(line + "<BR>" + EOL);
          if (listSize >= numLines)
          {
            lineList.removeFirst();
          }
          else
          {
            listSize++;
          }
        }
      }

      if (! showAll)
      {
        Iterator iterator = lineList.iterator();
        while (iterator.hasNext())
        {
          htmlBody.append((String) iterator.next());
        }
      }

      reader.close();
      htmlBody.append(Constants.HTML_COMMENT_LOG_END + EOL);
    }
    catch (Exception e)
    {
      infoMessage.append("Error reading log file:  " + e + "<BR>" + EOL);
    }
  }



  /**
   * Generates the navigation sidebar that will appear on the left side of the
   * page for pages generated in the administrative interface.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The HTML for the navigation sidebar.
   */
  static String generateSideBar(RequestInfo requestInfo)
  {
    StringBuilder html = new StringBuilder();

    html.append("<TABLE CLASS=\"" + Constants.STYLE_NAV_BAR +
                "\" BORDER=\"0\" WIDTH=\"100%\">" + EOL);
    html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                EOL);
    html.append("    <TD COLSPAN=\"2\">" + EOL);
    html.append("      <A HREF=\"" + Constants.SLAMD_HOME_URL +
                "\" TARGET=\"_BLANK\"><IMG SRC=\"" +
                requestInfo.servletBaseURI + '?' +
                Constants.SERVLET_PARAM_SECTION + '=' +
                Constants.SERVLET_SECTION_SLAMD_LOGO + "\" WIDTH=\"178\" " +
                "HEIGHT=\"128\" BORDER=\"0\" ALT=\"SLAMD Distributed Load " +
                "Generation Engine\"></A>" + EOL);
    html.append("      <FONT SIZE=\"-2\">&nbsp;<BR></FONT>" + EOL);
    html.append("    </TD>" + EOL);
    html.append("  </TR>" + EOL);

    if (showTimeInSidebar)
    {
      String timeStr = displayDateFormat.format(new Date());
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\"><A HREF=\"" + requestInfo.getGETURL() +
           "\">" + timeStr + "</A></TD>" + EOL);
      html.append("  </TR>" + EOL);
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
      html.append("  </TR>" + EOL);
    }

    if (useAccessControl && (requestInfo.userIdentifier != null) &&
        showLoginID)
    {
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">Authenticated as " +
                  requestInfo.userIdentifier + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
      html.append("  </TR>" + EOL);
    }


    if (configDBExists && showStatusFirstInSidebar && requestInfo.mayViewStatus)
    {
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_STATUS,
                               null, "SLAMD Server Status") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);

      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
      html.append("  </TR>" + EOL);
    }


    if (slamdRunning &&
        (requestInfo.mayViewJob || requestInfo.mayScheduleJob ||
         requestInfo.mayCancelJob || requestInfo.mayDeleteJob ||
         requestInfo.mayViewJobClass || requestInfo.mayAddJobClass ||
         requestInfo.mayDeleteJobClass))
    {
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">Manage Jobs</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);

      if (requestInfo.mayScheduleJob)
      {
        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_SCHEDULE,
                                 "Schedule a Job") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);
      }

      if (requestInfo.mayViewJob)
      {
        if (! readOnlyMode)
        {
          html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                      EOL);
          html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                      "</TD>" + EOL);
          html.append("    <TD VALIGN=\"TOP\">" +
                      generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_PENDING,
                                   "View Pending Jobs (" +
                                   scheduler.getPendingJobCount() + ')') +
                      "</TD>" + EOL);
          html.append("  </TR>" + EOL + EOL);

          html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                      EOL);
          html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                      "</TD>" + EOL);
          html.append("    <TD VALIGN=\"TOP\">" +
                      generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                   Constants.SERVLET_SECTION_JOB_VIEW_RUNNING,
                                   "View Running Jobs (" +
                                   scheduler.getRunningJobCount() + ')') +
                      "</TD>" + EOL);
          html.append("  </TR>" + EOL + EOL);
        }

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_COMPLETED,
                                 "View Completed Jobs") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING,
                                 "Optimizing Jobs") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_GROUPS,
                                 "Job Groups") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_REAL,
                                 "Real Job Folders") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_VIRTUAL,
                                 "Virtual Job Folders") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);
      }

      if (requestInfo.mayExportJobData)
      {
        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_IMPORT_JOB_DATA,
                                 "Import Job Data") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_EXPORT_JOB_DATA,
                                 "Export Job Data") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_MIGRATE,
                                 "Migrate SLAMD 1.x Data") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);
      }

      if (requestInfo.mayViewJobClass)
      {
        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_VIEW_CLASSES,
                                 "View Job Classes") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);
      }

      if (requestInfo.mayAddJobClass)
      {
        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_ADD_CLASS,
                                 "Add a New Job Class") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);

        html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                    EOL);
        html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                    "</TD>" + EOL);
        html.append("    <TD VALIGN=\"TOP\">" +
                    generateLink(requestInfo, Constants.SERVLET_SECTION_JOB,
                                 Constants.SERVLET_SECTION_JOB_INSTALL_JOB_PACK,
                                 "Install a Job Pack") + "</TD>" + EOL);
        html.append("  </TR>" + EOL + EOL);
      }

      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
      html.append("  </TR>" + EOL);
    }


    if (configDBExists && requestInfo.mayViewServletConfig)
    {
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_CONFIG,
                               null, "SLAMD Configuration") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);

      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
      html.append("  </TR>" + EOL);
    }


    if (configDBExists && (! showStatusFirstInSidebar) &&
        requestInfo.mayViewStatus)
    {
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_STATUS,
                               null, "SLAMD Server Status") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);
    }


    if (requestInfo.debugHTML && requestInfo.hasFullAccess)
    {
      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
      html.append("  </TR>" + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                  EOL);
      html.append("    <TD COLSPAN=\"2\">SLAMD Debug Information</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);

      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                               Constants.SERVLET_SECTION_DEBUG_THREADS,
                               "JVM Thread Data") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                               Constants.SERVLET_SECTION_DEBUG_STACK_TRACE,
                               "JVM Stack Trace") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                               Constants.SERVLET_SECTION_DEBUG_SYSPROPS,
                               "JVM System Properties") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                               Constants.SERVLET_SECTION_DEBUG_REQUEST,
                               "Client Request Data") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                               Constants.SERVLET_SECTION_DEBUG_DB,
                               "Debug Database Contents") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                               Constants.SERVLET_SECTION_DEBUG_GC,
                              "Request Garbage Collection") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                  Constants.SERVLET_SECTION_DEBUG_ENABLE_INSTRUCTION_TRACE,
                  "Enable Instruction Tracing") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                  Constants.SERVLET_SECTION_DEBUG_DISABLE_INSTRUCTION_TRACE,
                  "Disable Instruction Tracing") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                  Constants.SERVLET_SECTION_DEBUG_ENABLE_METHOD_TRACE,
                  "Enable Method Call Tracing") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);


      html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_LINK + "\">" +
                  EOL);
      html.append("    <TD VALIGN=\"TOP\">" + Constants.UI_NAV_BAR_BULLET +
                  "</TD>" + EOL);
      html.append("    <TD VALIGN=\"TOP\">" +
                  generateLink(requestInfo, Constants.SERVLET_SECTION_DEBUG,
                  Constants.SERVLET_SECTION_DEBUG_DISABLE_METHOD_TRACE,
                  "Disable Method Call Tracing") + "</TD>" + EOL);
      html.append("  </TR>" + EOL + EOL);
    }


    html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                EOL);
    html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    html.append("  </TR>" + EOL);

    html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                EOL);
    html.append("    <TD COLSPAN=\"2\">" +
                generateLink(requestInfo,
                             Constants.SERVLET_SECTION_DOCUMENTATION,
                             null, "SLAMD Documentation") + "</TD>" + EOL);
    html.append("  </TR>" + EOL + EOL);


    html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                EOL);
    html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    html.append("  </TR>" + EOL);

    html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                EOL);
    html.append("    <TD COLSPAN=\"2\">" +
                generateLink(requestInfo,
                             Constants.SERVLET_SECTION_LICENSE_HTML,
                             null, "SLAMD License") + "</TD>" + EOL);
    html.append("  </TR>" + EOL + EOL);

    html.append("  <TR CLASS=\"" + Constants.STYLE_NAV_BAR_HEADER + "\">" +
                EOL);
    html.append("    <TD COLSPAN=\"2\">&nbsp;</TD>" + EOL);
    html.append("  </TR>" + EOL);
    html.append("</TABLE>" + EOL);


    return html.toString();
  }



  /**
   * Generates the HTML page body that will be used whenever a user attempts to
   * perform a function that he/she doesn't have permission to do.
   *
   * @param  requestInfo  The state information for this request.
   * @param  message      A message with more specific information about the
   *                      access that is being denied.
   */
  static void generateAccessDeniedBody(RequestInfo requestInfo, String message)
  {
    StringBuilder htmlBody = requestInfo.htmlBody;

    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Access Denied</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The requested operation could not be performed." + EOL);
    htmlBody.append(message);
  }



  /**
   * Generates the HTML footer that will appear at the bottom of most pages
   * generated for use in the administrative interface.  It may contain some
   * special user-defined content if such content has been supplied.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The generated HTML footer.
   */
  static String generateHTMLFooter(RequestInfo requestInfo)
  {
    StringBuilder html = new StringBuilder();

    html.append("        </TD>" + EOL);
    html.append("      </TR>" + EOL);
    html.append("    </TABLE>" + EOL);
    html.append("  </BODY>" + EOL);
    html.append("</HTML>" + EOL);

    return html.toString();
  }



  /**
   * Retrieves the footer that should be used at the bottom of generated pages.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The footer that should be used at the top of generated pages.
   */
  static String getFooter(RequestInfo requestInfo)
  {
    if (pageFooter == null)
    {
      if (configDB == null)
      {
        pageFooter = parseHeader(requestInfo, Constants.DEFAULT_PAGE_FOOTER);
      }
      else
      {
        String footerText =
             configDB.getConfigParameter(Constants.PARAM_PAGE_FOOTER);
        if ((footerText == null) || (footerText.length() == 0))
        {
          footerText = Constants.DEFAULT_PAGE_FOOTER;
        }

        pageFooter = parseHeader(requestInfo, footerText);
      }
    }

    return pageFooter;
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo  The state information for this request.
   * @param  section      The section to include in the generated link.
   * @param  subsection   The subsection to include in the generated link.
   * @param  linkText     The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateLink(RequestInfo requestInfo, String section,
                             String subsection, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      if (requestInfo.debugHTML)
      {
        html.append('?');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }

    html.append("\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo      The state information for this request.
   * @param  section          The section to include in the generated link.
   * @param  subsection       The subsection to include in the generated link.
   * @param  extraParamName   The name of the "extra" parameter to include in
   *                          the generated link.
   * @param  extraParamValue  The value of the "extra" parameter to include
   *                          in the generated link.
   * @param  linkText         The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateLink(RequestInfo requestInfo, String section,
                             String subsection, String extraParamName,
                             String extraParamValue, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      if ((extraParamName != null) && (extraParamName.length() > 0))
      {
        html.append('&');
        html.append(extraParamName);
        html.append('=');

        if (extraParamValue != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraParamValue, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraParamValue);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraParamName != null) && (extraParamName.length() > 0))
    {
      html.append('?');
      html.append(extraParamName);
      html.append('=');

      if (extraParamValue != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraParamValue, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraParamValue);
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      if (requestInfo.debugHTML)
      {
        html.append('?');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }

    html.append("\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo  The state information for this request.
   * @param  section      The section to include in the generated link.
   * @param  subsection   The subsection to include in the generated link.
   * @param  extraName1   The name of the first "extra" parameter to include in
   *                      the generated link.
   * @param  extraValue1  The value of the first "extra" parameter to include in
   *                      the generated link.
   * @param  extraName2   The name of the second "extra" parameter to include in
   *                      the generated link.
   * @param  extraValue2  The value of the second "extra" parameter to include
   *                      in the generated link.
   * @param  linkText     The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateLink(RequestInfo requestInfo, String section,
                             String subsection, String extraName1,
                             String extraValue1, String extraName2,
                             String extraValue2, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      if ((extraName1 != null) && (extraName1.length() > 0))
      {
        html.append('&');
        html.append(extraName1);
        html.append('=');

        if (extraValue1 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue1, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue1);
          }
        }
      }

      if ((extraName2 != null) && (extraName2.length() > 0))
      {
        html.append('&');
        html.append(extraName2);
        html.append('=');

        if (extraValue2 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue2, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue2);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraName1 != null) && (extraName1.length() > 0))
    {
      html.append('?');
      html.append(extraName1);
      html.append('=');

      if (extraValue1 != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraValue1, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraValue1);
        }
      }

      if ((extraName2 != null) && (extraName2.length() > 0))
      {
        html.append('&');
        html.append(extraName2);
        html.append('=');

        if (extraValue2 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue2, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue2);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraName2 != null) && (extraName2.length() > 0))
    {
      html.append('?');
      html.append(extraName2);
      html.append('=');

      if (extraValue2 != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraValue2, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraValue2);
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      if (requestInfo.debugHTML)
      {
        html.append('?');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }

    html.append("\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo  The state information for this request.
   * @param  section      The section to include in the generated link.
   * @param  subsection   The subsection to include in the generated link.
   * @param  extraName1   The name of the first "extra" parameter to include in
   *                      the generated link.
   * @param  extraValue1  The value of the first "extra" parameter to include in
   *                      the generated link.
   * @param  extraName2   The name of the second "extra" parameter to include in
   *                      the generated link.
   * @param  extraValue2  The value of the second "extra" parameter to include
   *                      in the generated link.
   * @param  extraName3   The name of the third "extra" parameter to include in
   *                      the generated link.
   * @param  extraValue3  The value of the third "extra" parameter to include
   *                      in the generated link.
   * @param  linkText     The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateLink(RequestInfo requestInfo, String section,
                             String subsection, String extraName1,
                             String extraValue1, String extraName2,
                             String extraValue2, String extraName3,
                             String extraValue3, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      if ((extraName1 != null) && (extraName1.length() > 0))
      {
        html.append('&');
        html.append(extraName1);
        html.append('=');

        if (extraValue1 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue1, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue1);
          }
        }
      }

      if ((extraName2 != null) && (extraName2.length() > 0))
      {
        html.append('&');
        html.append(extraName2);
        html.append('=');

        if (extraValue2 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue2, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue2);
          }
        }
      }

      if ((extraName3 != null) && (extraName3.length() > 0))
      {
        html.append('&');
        html.append(extraName3);
        html.append('=');

        if (extraValue3 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue3, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue3);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraName1 != null) && (extraName1.length() > 0))
    {
      html.append('?');
      html.append(extraName1);
      html.append('=');

      if (extraValue1 != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraValue1, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraValue1);
        }
      }

      if ((extraName2 != null) && (extraName2.length() > 0))
      {
        html.append('&');
        html.append(extraName2);
        html.append('=');

        if (extraValue2 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue2, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue2);
          }
        }
      }
      if ((extraName3 != null) && (extraName3.length() > 0))
      {
        html.append('&');
        html.append(extraName3);
        html.append('=');

        if (extraValue3 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue3, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue3);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraName2 != null) && (extraName2.length() > 0))
    {
      html.append('?');
      html.append(extraName2);
      html.append('=');

      if (extraValue2 != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraValue2, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraValue2);
        }
      }

      if ((extraName3 != null) && (extraName3.length() > 0))
      {
        html.append('&');
        html.append(extraName3);
        html.append('=');

        if (extraValue3 != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValue3, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValue3);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraName3 != null) && (extraName3.length() > 0))
    {
      html.append('&');
      html.append(extraName3);
      html.append('=');

      if (extraValue3 != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraValue3, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraValue3);
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      if (requestInfo.debugHTML)
      {
        html.append('?');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }

    html.append("\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo  The state information for this request.
   * @param  section      The section to include in the generated link.
   * @param  subsection   The subsection to include in the generated link.
   * @param  extraNames   The names of the "extra" parameters to include in the
   *                      generated link.  It must not be {@code null}.
   * @param  extraValues  The values of the "extra" parameters to include in the
   *                      generated link.  It must not be {@code null}, and it
   *                      must have the same number of elements as the array of
   *                      extra names.
   * @param  linkText     The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateLink(RequestInfo requestInfo, String section,
                             String subsection, String[] extraNames,
                             String[] extraValues, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      for (int i=0; i < extraNames.length; i++)
      {
        html.append('&');
        html.append(extraNames[i]);
        html.append('=');

        if (extraValues[i] != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValues[i], "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValues[i]);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      for (int i=0; i < extraNames.length; i++)
      {
        if (i == 0)
        {
          html.append('?');
        }
        else
        {
          html.append('&');
        }

        html.append(extraNames[i]);
        html.append('=');

        if (extraValues[i] != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValues[i], "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValues[i]);
          }
        }
      }
    }

    html.append("\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo  The state information for this request.
   * @param  section      The section to include in the generated link.
   * @param  subsection   The subsection to include in the generated link.
   * @param  extraNames   The names of the "extra" parameters to include in the
   *                      generated link.  It must not be {@code null}.
   * @param  extraValues  The values of the "extra" parameters to include in the
   *                      generated link.  It must not be {@code null}, and it
   *                      must have the same number of elements as the array of
   *                      extra names.
   * @param  linkTitle    The title that should be displayed as a tooltip for
   *                      the link.
   * @param  linkText     The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateLink(RequestInfo requestInfo, String section,
                             String subsection, String[] extraNames,
                             String[] extraValues, String linkTitle,
                             String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      for (int i=0; i < extraNames.length; i++)
      {
        html.append('&');
        html.append(extraNames[i]);
        html.append('=');

        if (extraValues[i] != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValues[i], "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValues[i]);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      for (int i=0; i < extraNames.length; i++)
      {
        if (i == 0)
        {
          html.append('?');
        }
        else
        {
          html.append('&');
        }

        html.append(extraNames[i]);
        html.append('=');

        if (extraValues[i] != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraValues[i], "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraValues[i]);
          }
        }
      }
    }

    if (linkTitle != null)
    {
      html.append("\" TITLE=\"");
      html.append(linkTitle);
    }

    html.append("\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo      The state information for this request.
   * @param  section          The section to include in the generated link.
   * @param  subsection       The subsection to include in the generated link.
   * @param  extraParamName   The name of the "extra" parameter to include in
   *                          the generated link.
   * @param  extraParamValue  The value of the "extra" parameter to include
   *                          in the generated link.
   * @param  linkText         The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateNewWindowLink(RequestInfo requestInfo, String section,
                                      String subsection, String extraParamName,
                                      String extraParamValue, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      if ((extraParamName != null) && (extraParamName.length() > 0))
      {
        html.append('&');
        html.append(extraParamName);
        html.append('=');

        if (extraParamValue != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraParamValue, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraParamValue);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraParamName != null) && (extraParamName.length() > 0))
    {
      html.append('?');
      html.append(extraParamName);
      html.append('=');

      if (extraParamValue != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraParamValue, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraParamValue);
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      if (requestInfo.debugHTML)
      {
        html.append('?');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }

    html.append("\" TARGET=\"_BLANK\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates the HTML for a link to the specified area of the SLAMD admin
   * interface.
   *
   * @param  requestInfo       The state information for this request.
   * @param  section           The section to include in the generated link.
   * @param  subsection        The subsection to include in the generated link.
   * @param  extraParam1Name   The name of the first "extra" parameter to
   *                           include in the generated link.
   * @param  extraParam1Value  The value of the first "extra" parameter to
   *                           include in the generated link.
   * @param  extraParam2Name   The name of the first "extra" parameter to
   *                           include in the generated link.
   * @param  extraParam2Value  The value of the first "extra" parameter to
   *                           include in the generated link.
   * @param  linkText          The text to include in the generated link.
   *
   * @return  The HTML for a link to the specified area of the SLAMD admin
   *          interface.
   */
  static String generateNewWindowLink(RequestInfo requestInfo, String section,
                                      String subsection, String extraParam1Name,
                                      String extraParam1Value,
                                      String extraParam2Name,
                                      String extraParam2Value, String linkText)
  {
    StringBuilder html = new StringBuilder();

    html.append("<A HREF=\"");
    html.append(requestInfo.servletBaseURI);

    if ((section != null) && (section.length() > 0))
    {
      html.append('?');
      html.append(Constants.SERVLET_PARAM_SECTION);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(section, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(section);
      }

      if ((subsection != null) && (subsection.length() > 0))
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_SUBSECTION);
        html.append('=');
        try
        {
          html.append(URLEncoder.encode(subsection, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(subsection);
        }
      }

      if ((extraParam1Name != null) && (extraParam1Name.length() > 0))
      {
        html.append('&');
        html.append(extraParam1Name);
        html.append('=');

        if (extraParam1Value != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraParam1Value, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraParam1Value);
          }
        }
      }

      if ((extraParam2Name != null) && (extraParam2Name.length() > 0))
      {
        html.append('&');
        html.append(extraParam2Name);
        html.append('=');

        if (extraParam2Value != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraParam2Value, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraParam2Value);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraParam1Name != null) && (extraParam1Name.length() > 0))
    {
      html.append('?');
      html.append(extraParam1Name);
      html.append('=');
      try
      {
        html.append(URLEncoder.encode(extraParam1Value, "UTF-8"));
      }
      catch (UnsupportedEncodingException uee)
      {
        html.append(extraParam1Value);
      }

      if ((extraParam2Name != null) && (extraParam2Name.length() > 0))
      {
        html.append('&');
        html.append(extraParam2Name);
        html.append('=');

        if (extraParam2Value != null)
        {
          try
          {
            html.append(URLEncoder.encode(extraParam2Value, "UTF-8"));
          }
          catch (UnsupportedEncodingException uee)
          {
            html.append(extraParam2Value);
          }
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else if ((extraParam2Name != null) && (extraParam2Name.length() > 0))
    {
      html.append('?');
      html.append(extraParam2Name);
      html.append('=');

      if (extraParam2Value != null)
      {
        try
        {
          html.append(URLEncoder.encode(extraParam2Value, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee)
        {
          html.append(extraParam2Value);
        }
      }

      if (requestInfo.debugHTML)
      {
        html.append('&');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }
    else
    {
      if (requestInfo.debugHTML)
      {
        html.append('?');
        html.append(Constants.SERVLET_PARAM_HTML_DEBUG);
        html.append("=1");
      }
    }

    html.append("\" TARGET=\"_BLANK\">");
    html.append(linkText);
    html.append("</A>");

    return html.toString();
  }



  /**
   * Generates a link that can take a user directly to the page to view results
   * for a given job.
   *
   * @param  requestInfo  The state information for this request.
   * @param  jobID        The job ID to use for the generated link.
   * @param  linkText     The text to use for the generated link.
   *
   * @return  The generated link.
   */
  static String generateGetJobLink(RequestInfo requestInfo, String jobID,
                                   String linkText)
  {
    return "<A HREF=\"" + requestInfo.servletBaseURI + '?' +
           Constants.SERVLET_PARAM_GET_JOB + '=' + jobID + "\">" + linkText +
           "</A>";
  }



  /**
   * Generates the HTML for a hidden form element, which is not visible to the
   * end user in the rendered page, but provides additional information needed
   * for the server to process the form submission.
   *
   * @param  name   The name to use for the hidden parameter.
   * @param  value  The value to use for the hidden parameter.
   *
   * @return  The generated HTML for the hidden form element.
   */
  static String generateHidden(String name, String value)
  {
    return "<INPUT TYPE=\"HIDDEN\" NAME=\"" + name + "\" VALUE=\"" + value +
           "\">";
  }



  /**
   * Generates the HTML for the informational/warning/error message that may
   * appear at the top of generated pages that provides additional information
   * that would otherwise not be included on that page.
   *
   * @param  warningMessage  The message to be formatted as a warning.
   *
   * @return  The formatted warning message.
   */
  static String generateWarning(String warningMessage)
  {
    if ((warningMessage == null) || (warningMessage.length() == 0))
    {
      return "";
    }

    return "<B><SPAN CLASS=\"" +Constants.STYLE_WARNING_TEXT + "\">" +
           warningMessage + "</SPAN></B>" + EOL + "<BR><BR>" + EOL;
  }



  /**
   * Indicates whether the specified request parameter is that represents the
   * value of a checkbox that has been checked.
   *
   * @param  requestInfo    The state information for this request.
   * @param  parameterName  The name of the request parameter for which to make
   *                        the determination.
   *
   * @return  <CODE>true</CODE> if the checkbox has been checked, or
   *          <CODE>false</CODE> if not.
   */
  static boolean isChecked(RequestInfo requestInfo, String parameterName)
  {
    String valueStr = requestInfo.request.getParameter(parameterName);
    return ((valueStr != null) &&
            (valueStr.equalsIgnoreCase("true") ||
             valueStr.equalsIgnoreCase("yes") ||
             valueStr.equalsIgnoreCase("on") ||
             valueStr.equalsIgnoreCase("1")));
  }



  /**
   * Retrieves the ID to use for the next request.
   *
   * @return  The ID to use for the next request.
   */
  static synchronized int getNextID()
  {
    return nextID++;
  }



  /**
   * Indicates whether the SLAMD server is currently operating in read-only
   * mode.
   *
   * @return  <CODE>true</CODE> if the SLAMD server is operating in read-only
   *          mode, and <CODE>false</CODE> if it is not.
   */
  static boolean readOnlyMode()
  {
    return readOnlyMode;
  }



  /**
   * Indicates whether the SLAMD server is currently operating in read-only mode
   * and should restrict which jobs and folders should be published.
   *
   * @return  <CODE>true</CODE> if the SLAMD server is operating in restricted
   *          read-only mode, and <CODE>false</CODE> if it is not.
   */
  static boolean restrictedReadOnlyMode()
  {
    return (readOnlyMode && restrictedReadOnlyMode);
  }
}
