/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.tools;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import com.slamd.common.Constants;
import com.slamd.http.HTTPClient;
import com.slamd.http.HTTPRequest;
import com.slamd.http.HTTPResponse;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;



/**
 * This class defines a utility that may be used to retrieve information about a
 * job or optimizing job in the SLAMD server.  It does this by communicating
 * with the administrative interface over HTTP to ensure that all appropriate
 * validity checking is performed, and that any required authentication is
 * honored.
 *
 *
 * @author   Neil A. Wilson
 */
public class GetJob
{
  // Variables used processing.
  private boolean[] isOptimizing = null;
  private boolean   onlyState    = false;
  private boolean   useSSL       = false;
  private boolean   verboseMode  = false;
  private int       slamdPort    = 8080;
  private String    authID       = null;
  private String    authPW       = null;
  private String    eol          = Constants.EOL;
  private String    postURI      = "/slamd";
  private String    slamdHost    = "127.0.0.1";
  private String[]  jobIDs       = null;



  /**
   * Invokes the constructor and provides it with the command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    GetJob getJob = new GetJob(args);
    getJob.sendRequest();
  }



  /**
   * Parses the provided command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public GetJob(String[] args)
  {
    ArrayList<Boolean> isOptimizingList = new ArrayList<Boolean>();
    ArrayList<String>  jobIDList        = new ArrayList<String>();

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
        jobIDList.add(args[++i]);
        isOptimizingList.add(Boolean.FALSE);
      }
      else if (args[i].equals("-o"))
      {
        jobIDList.add(args[++i]);
        isOptimizingList.add(Boolean.TRUE);
      }
      else if (args[i].equals("-O"))
      {
        onlyState = true;
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


    // Make sure that at least one job ID was provided.
    int listSize = jobIDList.size();
    if (listSize == 0)
    {
      System.err.println("At least one job ID or optimizing job ID must be " +
                         "provided");
      displayUsage();
      System.exit(1);
    }


    jobIDs       = new String[listSize];
    isOptimizing = new boolean[listSize];
    for (int i=0; i < listSize; i++)
    {
      jobIDs[i]       = jobIDList.get(i);
      isOptimizing[i] = isOptimizingList.get(i);
    }
  }



  /**
   * Sends the request to the SLAMD server and parses the response.
   *
   * @return  {@code true} if the job information was retrieved
   *          successfully, or {@code false} if not.
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


    // Iterate through the jobs, retrieve them, and print them to standard out.
    for (int i=0; i < jobIDs.length; i++)
    {
      // Construct the request to send to the server.
      HTTPRequest request = new HTTPRequest(false, requestURL);
      request.addParameter(Constants.SERVLET_PARAM_SECTION,
                           Constants.SERVLET_SECTION_JOB);
      if (isOptimizing[i])
      {
        request.addParameter(Constants.SERVLET_PARAM_SUBSECTION,
             Constants.SERVLET_SECTION_JOB_VIEW_OPTIMIZING_AS_TEXT);
        request.addParameter(Constants.SERVLET_PARAM_OPTIMIZING_JOB_ID,
                             jobIDs[i]);
      }
      else
      {
        request.addParameter(Constants.SERVLET_PARAM_SUBSECTION,
             Constants.SERVLET_SECTION_JOB_VIEW_AS_TEXT);
        request.addParameter(Constants.SERVLET_PARAM_JOB_ID, jobIDs[i]);
      }

      if (onlyState)
      {
        request.addParameter(Constants.SERVLET_PARAM_ONLY_STATE, "true");
      }


      // Send the request and read the response.
      HTTPResponse response;
      try
      {
        response = httpClient.sendRequest(request);
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


      // Convert the response data from a byte array to a string and print it to
      // standard out.
      try
      {
        byte[] responseData = response.getResponseData();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                      new ByteArrayInputStream(responseData)));
        String line = reader.readLine();
        while (line != null)
        {
          System.out.println(line);
          line = reader.readLine();
        }

        reader.close();
      }
      catch (Exception e)
      {
        System.err.println("Error decoding response data:");
        e.printStackTrace();
        return false;
      }
    }


    httpClient.closeAll();
    return true;
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
"USAGE:  java -cp {classpath} GetJob {options}" + eol +
"        where {options} include:" + eol +
"-j {jobID}        -- The ID of a job to retrieve" + eol +
"-o {optimizingID} -- The ID of an optimizing job to retrieve" + eol +
"-O                -- Only display the current state of the job(s)" + eol +
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

