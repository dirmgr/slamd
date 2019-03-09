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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.slamd.common.Constants;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;

import com.unboundid.util.Base64;



/**
 * This class defines a command-line utility that may be used to export job data
 * from the SLAMD server.  It does this by communicating with the SLAMD
 * administrative interface over HTTP to ensure that all appropriate validity
 * checking is performed, and that any required authentication is honored.
 *
 *
 * @author   Neil A. Wilson
 */
public class ExportData
{
  // Variables used processing.
  private boolean  exportAll            = false;
  private boolean  exportUnclassified   = false;
  private boolean  useSSL               = false;
  private boolean  verboseMode          = false;
  private int      slamdPort            = 8080;
  private String   authID               = null;
  private String   authPW               = null;
  private String   eol                  = Constants.EOL;
  private String   filePath             = null;
  private String   postURI              = "/slamd";
  private String   slamdHost            = "127.0.0.1";
  private String[] realFolderExports    = new String[0];
  private String[] virtualFolderExports = new String[0];



  /**
   * Invokes the constructor and provides it with the command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    ExportData exporter = new ExportData(args);
    if (exporter.sendRequest())
    {
      System.out.println("The export completed successfully.");
    }
    else
    {
      System.out.println("An error occurred that prevented the export from " +
                         "completing successfully.");
    }
  }



  /**
   * Parses the provided command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public ExportData(String[] args)
  {
    ArrayList<String> realList = new ArrayList<String>();
    ArrayList<String> virtualList = new ArrayList<String>();

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
      else if (args[i].equals("-a"))
      {
        exportAll = true;
      }
      else if (args[i].equals("-U"))
      {
        exportUnclassified = true;
      }
      else if (args[i].equals("-R"))
      {
        realList.add(args[++i]);
      }
      else if (args[i].equals("-V"))
      {
        virtualList.add(args[++i]);
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
      System.err.println("No export file path provided (use -f)");
      displayUsage();
      System.exit(1);
    }


    // Convert the folder lists into arrays.
    realFolderExports    = new String[realList.size()];
    virtualFolderExports = new String[virtualList.size()];
    realList.toArray(realFolderExports);
    virtualList.toArray(virtualFolderExports);


    // Make sure that at the user specified something to export
    if ((! exportAll) && (! exportUnclassified) &&
        (realFolderExports.length == 0) && (virtualFolderExports.length == 0))
    {
      System.err.println("No data specified to include in the export");
      displayUsage();
      System.exit(1);
    }
  }



  /**
   * Sends the request to the SLAMD server and parses the response.
   *
   * @return  <CODE>true</CODE> if the data was exported properly, or
   *          <CODE>false</CODE> if not.
   */
  public boolean sendRequest()
  {
    // Construct the data string to POST to the server.
    StringBuilder buf = new StringBuilder();
    addParameter(buf, Constants.SERVLET_PARAM_SECTION,
                 Constants.SERVLET_SECTION_JOB);
    addParameter(buf, Constants.SERVLET_PARAM_SUBSECTION,
                 Constants.SERVLET_SECTION_JOB_EXPORT_JOB_DATA);
    if (exportAll)
    {
      addParameter(buf, Constants.SERVLET_PARAM_EXPORT_CHOICE,
                   Constants.EXPORT_CHOICE_ALL);
    }
    else
    {
      addParameter(buf, Constants.SERVLET_PARAM_EXPORT_CHOICE,
                   Constants.EXPORT_CHOICE_SELECTED);

      if (exportUnclassified)
      {
        addParameter(buf, Constants.SERVLET_PARAM_EXPORT_UNCLASSIFIED,
                     Constants.SERVLET_PARAM_EXPORT_UNCLASSIFIED);
      }

      for (int i=0; i < realFolderExports.length; i++)
      {
        addParameter(buf, Constants.SERVLET_PARAM_EXPORT_REAL_FOLDER,
                     realFolderExports[i]);
      }

      for (int i=0; i < virtualFolderExports.length; i++)
      {
        addParameter(buf, Constants.SERVLET_PARAM_EXPORT_VIRTUAL_FOLDER,
                     virtualFolderExports[i]);
      }
    }
    addParameter(buf, Constants.SERVLET_PARAM_SUBMIT,
                 Constants.SUBMIT_STRING_EXPORT);


    // Establish a connection to the SLAMD server's admin interface.
    Socket         socket = null;
    BufferedReader reader = null;
    BufferedWriter writer = null;
    try
    {
      if (useSSL)
      {
        debug("Establishing an SSL-based connection to " + slamdHost + ':' +
              slamdPort);
        JSSEBlindTrustSocketFactory socketFactory =
             new JSSEBlindTrustSocketFactory();
        socket = socketFactory.makeSocket(slamdHost, slamdPort);
      }
      else
      {
        debug("Establishing a connection to " + slamdHost + ':' +
              slamdPort);
        socket = new Socket(slamdHost, slamdPort);
      }

      reader =
           new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer =
           new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
    catch (Exception e)
    {
      if (verboseMode)
      {
        e.printStackTrace();
      }
      System.err.println("ERROR:  Unable to connect to SLAMD server's " +
                         "admin interface:  " + e);
      return false;
    }


    // Write the HTTP request to the server.
    try
    {
      writeLine(writer, "POST " + postURI + " HTTP/1.1");
      writeLine(writer, "Host: " + slamdHost);
      writeLine(writer, "Connection: close");
      writeLine(writer, "Content-Type: application/x-www-form-urlencoded");

      if ((authID != null) && (authID.length() > 0) &&
          (authPW != null) && (authPW.length() > 0))
      {
        String authStr   = authID + ':' + authPW;
        byte[] authBytes = authStr.getBytes("UTF-8");
        writeLine(writer,
                  "Authorization: Basic " + Base64.encode(authBytes));
      }

      writeLine(writer, "Content-Length: " + buf.length());
      writeLine(writer, "");
      writeLine(writer, buf.toString());
      writer.flush();
    }
    catch (IOException ioe)
    {
      if (verboseMode)
      {
        ioe.printStackTrace();
      }
      System.err.println("ERROR: Unable to POST to server -- " + ioe);
      return false;
    }


    // Read the HTTP response header from the server.
    try
    {
      String line;
      while (((line = readLine(reader)) != null) && (line.length() > 0))
      {
        String lowerLine = line.toLowerCase();
        if (lowerLine.startsWith("http/1.1"))
        {
          StringTokenizer st = new StringTokenizer(line, " ");
          String protocol   = st.nextToken();
          String resultCode = st.nextToken();
          String message = line.substring(line.indexOf(resultCode) +
                                          resultCode.length()).trim();
          if (! resultCode.equals("200"))
          {
            System.err.println("ERROR:  Unexpected HTTP result code " +
                               resultCode);
            System.err.println("Message was " + message);
            return false;
          }
        }
      }
    }
    catch (Exception e)
    {
      if (verboseMode)
      {
        e.printStackTrace();
      }
      System.err.println("ERROR:  Unable to read response header -- " + e);
      return false;
    }

    try
    {
      BufferedWriter exportWriter =
           new BufferedWriter(new FileWriter(filePath, false));
      int    bytesRead  = 0;
      int    eolLength  = System.getProperty("line.separator").length();
      int    nextTarget = 1024 * 1024;
      String line;
      while ((line = reader.readLine()) != null)
      {
        exportWriter.write(line);
        exportWriter.newLine();
        bytesRead += line.length() + eolLength;
        if (bytesRead >= nextTarget)
        {
          System.out.println(bytesRead + " bytes exported");
          nextTarget += (1024 * 1024);
        }
      }

      System.out.println("Export complete -- " + bytesRead + " bytes exported");
      exportWriter.close();
      reader.close();
      writer.close();
    }
    catch (IOException ioe)
    {
      if (verboseMode)
      {
        ioe.printStackTrace();
      }
    }

    return true;
  }



  /**
   * Adds information about the specified parameter to the list of parameters in
   * a form that may be submitted in an HTTP POST.
   *
   * @param  buffer  The string buffer to which the parameter information should
   *                 be written.
   * @param  name    The name of the parameter to add.
   * @param  value   The value of the parameter to add.
   */
  private void addParameter(StringBuilder buffer, String name, String value)
  {
    if (buffer.length() > 0)
    {
      buffer.append('&');
    }

    buffer.append(name);
    buffer.append('=');

    try
    {
      buffer.append(URLEncoder.encode(value, "UTF-8"));
    }
    catch (UnsupportedEncodingException uee)
    {
      if (verboseMode)
      {
        uee.printStackTrace();
      }
      System.err.println("ERROR:  Unable to encode \"" + value +
                         "\" as UTF-8 data.");
    }
  }



  /**
   * Writes the provided line to the provided writer.
   *
   * @param  writer  The writer to which the line should be written.
   * @param  line    The line to be written.
   *
   * @throws  IOException  If a problem occurs while writing the line.
   */
  private void writeLine(BufferedWriter writer, String line)
          throws IOException
  {
    writer.write(line + eol);
    debug("CLIENT:  " + line);
  }



  /**
   * Reads a line of output from the provided reader.
   *
   * @param  reader  The reader from which to read the output.
   *
   * @return  The line read from the reader.
   *
   * @throws  IOException  If a problem occurs while reading the line.
   */
  private String readLine(BufferedReader reader)
          throws IOException
  {
    String line = reader.readLine();
    if (line != null)
    {
      debug("SERVER:  " + line);
    }

    return line;
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
"USAGE:  java -cp {classpath} ExportData {options}" + eol +
"        where {options} include:" + eol +
"-f {filePath}    -- The path to the data file to be created" + eol +
"-a               -- Indicates that all data in the server should be" + eol +
"                    included in the export" + eol +
"-U               -- Indicates that unclassified job data should be" + eol +
"                    included in the export" + eol +
"-R {folderName}  -- The name of a real job folder to include in the" + eol +
"                    export (may be used multiple times)" + eol +
"-V {folderName}  -- The name of a virtual job folder to include in the" + eol +
"                    export (may be used multiple times)" + eol +
"-F {folderName}  -- The name of a folder to include in the export" + eol +
"-h {slamdHost}   -- The address of the SLAMD server" + eol +
"-p {slamdPort}   -- The port of the SLAMD server's admin interface" + eol +
"-A {authID}      -- The username to use to authenticate to the server" + eol +
"-P {authPW}      -- The password to use to authenticate to the server" + eol +
"-u {uri}         -- The URI to which the request should be sent" + eol +
"-S               -- Communicate with the SLAMD server over SSL" + eol +
"-v               -- Enable verbose mode" + eol +
"-H               -- Display usage information and exit"
                      );
  }
}

