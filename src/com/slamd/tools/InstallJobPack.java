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



import java.net.URL;

import com.slamd.common.Constants;
import com.slamd.common.JSSEBlindTrustSocketFactory;
import com.slamd.http.HTTPClient;
import com.slamd.http.HTTPRequest;
import com.slamd.http.HTTPResponse;



/**
 * This class defines a utility that may be used to install a SLAMD job pack
 * from the command line.  It does this by communicating with the SLAMD
 * administrative interface over HTTP to ensure that all appropriate validity
 * checking is performed, and that any required authentication is honored.
 *
 *
 * @author   Neil A. Wilson
 */
public class InstallJobPack
{
  // Variables used processing.
  private boolean useSSL      = false;
  private boolean verboseMode = false;
  private int     slamdPort   = 8080;
  private String  authID      = null;
  private String  authPW      = null;
  private String  eol         = Constants.EOL;
  private String  filePath    = null;
  private String  postURI     = "/slamd";
  private String  slamdHost   = "127.0.0.1";



  /**
   * Invokes the constructor and provides it with the command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    InstallJobPack installer = new InstallJobPack(args);
    if (installer.sendRequest())
    {
      System.out.println("The job pack was installed successfully.");
    }
    else
    {
      System.out.println("An error occurred while trying to install the job " +
                         "pack.");
    }
  }



  /**
   * Parses the provided command-line arguments and sends the request to the
   * server to install the job pack.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public InstallJobPack(String[] args)
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
      else if (args[i].equals("-f"))
      {
        filePath = args[++i];
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


    // Make sure that the file path has been provided.
    if (filePath == null)
    {
      System.err.println("No job pack file path provided (use -f)");
      displayUsage();
      System.exit(1);
    }
  }



  /**
   * Sends the request to the SLAMD server and parses the response.
   *
   * @return  {@code true} if the job pack was installed properly, or
   *          {@code false} if not.
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
                         Constants.SERVLET_SECTION_JOB_INSTALL_JOB_PACK);
    request.addParameter(Constants.SERVLET_PARAM_JOB_PACK_PATH, filePath);


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
    // there was an error.  Otherwise, it was successful.
    String errorMessage =
         response.getHeader(Constants.RESPONSE_HEADER_ERROR_MESSAGE);
    if (errorMessage == null)
    {
      return true;
    }
    else
    {
      System.err.println("ERROR:  " + errorMessage);
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
"USAGE:  java -cp {classpath} InstallJobPack {options}" + eol +
"        where {options} include:" + eol +
"-f {filePath}   -- The path to the job pack file to install" + eol +
"-h {slamdHost}  -- The address of the SLAMD server" + eol +
"-p {slamdPort}  -- The port of the SLAMD server's admin interface" + eol +
"-A {authID}     -- The username to use to authenticate to the server" + eol +
"-P {authPW}     -- The password to use to authenticate to the server" + eol +
"-u {uri}        -- The URI to which the request should be sent" + eol +
"-S              -- Communicate with the SLAMD server over SSL" + eol +
"-v              -- Enable verbose mode" + eol +
"-H              -- Display usage information and exit"
                      );
  }
}

