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
package com.slamd.tools;



import java.io.FileOutputStream;
import java.net.URL;

import com.slamd.common.Constants;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.http.HTTPClient;
import com.slamd.http.HTTPRequest;
import com.slamd.http.HTTPResponse;



/**
 * This class defines a utility that may be used to export information about a
 * SLAMD job to a tab-delimited text file.  It does this by communicating with
 * the administrative interface over HTTP to ensure that all appropriate
 * validity checking is performed, and that any required authentication is
 * honored.
 *
 *
 * @author   Neil A. Wilson
 */
public class ExportJob
{
  // Variables used processing.
  private boolean includeLabels         = false;
  private boolean includeOverallStats   = false;
  private boolean includePerClientStats = false;
  private boolean includePerThreadStats = false;
  private boolean useSSL                = false;
  private boolean verboseMode           = false;
  private int     slamdPort             = 8080;
  private String  authID                = null;
  private String  authPW                = null;
  private String  eol                   = Constants.EOL;
  private String  jobID                 = null;
  private String  outputFile            = null;
  private String  postURI               = "/slamd";
  private String  slamdHost             = "127.0.0.1";



  /**
   * Invokes the constructor and provides it with the command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    ExportJob exportJob = new ExportJob(args);
    if (exportJob.sendRequest())
    {
      System.out.println("The job information was exported successfully.");
    }
    else
    {
      System.out.println("An error occurred while trying to export the job " +
                         "information.");
    }
  }



  /**
   * Parses the provided command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public ExportJob(String[] args)
  {
    for (int i=0; i< args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        slamdHost = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        slamdPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-u"))
      {
        postURI = args[++i];
      }
      else if (args[i].equals("-S"))
      {
        useSSL = true;
      }
      else if (args[i].equals("-A"))
      {
        authID = args[++i];
      }
      else if (args[i].equals("-P"))
      {
        authPW = args[++i];
      }
      else if (args[i].equals("-j"))
      {
        if (jobID == null)
        {
          jobID = args[++i];
        }
        else
        {
          System.err.println("Only one job may be specified to export.");
          System.exit(1);
        }
      }
      else if (args[i].equals("-f"))
      {
        outputFile = args[++i];
      }
      else if (args[i].equals("-l"))
      {
        includeLabels = true;
      }
      else if (args[i].equals("-o"))
      {
        includeOverallStats = true;
      }
      else if (args[i].equals("-c"))
      {
        includePerClientStats = true;
      }
      else if (args[i].equals("-t"))
      {
        includePerThreadStats = true;
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that the job ID has been provided.
    if (jobID == null)
    {
      System.err.println("No job ID provided (use -j)");
      displayUsage();
      System.exit(1);
    }


    // Make sure that an output file was specified.
    if (outputFile == null)
    {
      System.err.println("No output file specified (use -f)");
      displayUsage();
      System.exit(1);
    }


    // Make sure that a detail level was provided.
    if (! (includeOverallStats || includePerClientStats ||
           includePerThreadStats))
    {
      System.err.println("You must provide at least one detail level (use " +
                         "-o, -c, and/or -t)");
      displayUsage();
      System.exit(1);
    }
  }



  /**
   * Sends the request to the SLAMD server and parses the response.
   *
   * @return  <CODE>true</CODE> if the job was cancelled successfully, or
   *          <CODE>false</CODE> if not.
   */
  public boolean sendRequest()
  {
    // Create the HTTP client to use to send the request.
    HTTPClient httpClient = new HTTPClient();
    httpClient.setFollowRedirects(true);
    httpClient.setRetrieveAssociatedFiles(false);

    if ((authID != null) && (authID.length() > 0) && (authPW != null) &&
        (authPW.length() > 0))
    {
      httpClient.enableAuthentication(authID, authPW);
    }

    if (verboseMode)
    {
      httpClient.enableDebugMode();
    }

    String protocol = "http";
    if (useSSL)
    {
      try
      {
        protocol = "https";
        httpClient.setSSLSocketFactory(new JSSEBlindTrustSocketFactory());
      }
      catch (Exception e)
      {
        System.err.println("ERROR:  Unable to initialize the SSL socket " +
                           "factory:");
        e.printStackTrace();
        return false;
      }
    }


    // Construct the URL for the request.
    String urlStr = protocol + "://" + slamdHost + ':' + slamdPort + postURI;
    URL requestURL;
    try
    {
      requestURL = new URL(urlStr);
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  The constructed URL '" + urlStr +
                         "' is invalid:");
      e.printStackTrace();
      return false;
    }


    // Construct the request to send to the server.
    HTTPRequest request = new HTTPRequest(false, requestURL);
    request.addParameter(Constants.SERVLET_PARAM_SECTION,
                         Constants.SERVLET_SECTION_JOB);
    request.addParameter(Constants.SERVLET_PARAM_SUBSECTION,
                         Constants.SERVLET_SECTION_JOB_SAVE_STATS);
    request.addParameter(Constants.SERVLET_PARAM_JOB_ID, jobID);
    request.addParameter(Constants.SERVLET_PARAM_CONFIRMED, "Yes");

    if (includeLabels)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                           Constants.SERVLET_PARAM_INCLUDE_LABELS, "on");
    }

    if (includeOverallStats)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                           Constants.SERVLET_PARAM_DETAIL_LEVEL,
                           Constants.STAT_CATEGORY_JOB_STATS_STR);
    }

    if (includePerClientStats)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                           Constants.SERVLET_PARAM_DETAIL_LEVEL,
                           Constants.STAT_CATEGORY_CLIENT_STATS_STR);
    }

    if (includePerThreadStats)
    {
      request.addParameter(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                           Constants.SERVLET_PARAM_DETAIL_LEVEL,
                           Constants.STAT_CATEGORY_THREAD_STATS_STR);
    }


    // Send the request and read the response.
    HTTPResponse response;
    try
    {
      response = httpClient.sendRequest(request);
      httpClient.closeAll();
    }
    catch (Exception e)
    {
      System.err.println("Error communicating with SLAMD server:");
      e.printStackTrace();
      return false;
    }


    // Make sure that the response status code was "200".
    if (response.getStatusCode() != 200)
    {
      System.err.println("ERROR:  Unexpected response status code (" +
                         response.getStatusCode() + ' ' +
                         response.getResponseMessage() + ')');
      return false;
    }


    // See if the response returned included an error header.  If it did, then
    // there was an error.
    String errorMessage =
         response.getHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE);
    if (errorMessage != null)
    {
      System.err.println("ERROR:  " + errorMessage);
      return false;
    }


    // Get the response data and write it to the output file.
    try
    {
      FileOutputStream outputStream = new FileOutputStream(outputFile);
      outputStream.write(response.getResponseData());
      outputStream.flush();
      outputStream.close();
      return true;
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  Unable to save data to the output file:");
      e.printStackTrace();
      return false;
    }
  }



  /**
   * Prints the provided message if the program is operating in verbose mode.
   *
   * @param  message  The message to be printed.
   */
  public void debug(String message)
  {
    if (verboseMode)
    {
      System.out.println(message);
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String eol = Constants.EOL;

    System.out.println(
"USAGE:  java -cp {classpath} ExportJob {options}" + eol +
"        where {options} include:" + eol +
"-j {jobID}        -- The job ID of the job to export" + eol +
"-f {filename}     -- The output file to which to send the export" + eol +
"-l                -- Include labels in the exported data" + eol +
"-o                -- Include overall job statistics in the export" + eol +
"-c                -- Include per-client statistics in the export" + eol +
"-t                -- Include per-thread statistics in the export" + eol +
"-h {slamdHost}    -- The address of the SLAMD server" + eol +
"-p {slamdPort}    -- The port of the SLAMD server's admin interface" + eol +
"-A {authID}       -- The username to use to authenticate to the server" + eol +
"-P {authPW}       -- The password to use to authenticate to the server" + eol +
"-u {uri}          -- The URI to which the request should be sent" + eol +
"-S                -- Communicate with the SLAMD server over SSL" + eol +
"-v                -- Enable verbose mode" + eol +
"-H                -- Display usage information and exit"
                      );
  }
}

