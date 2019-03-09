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



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;

import com.slamd.common.Constants;
import com.slamd.job.JobClass;
import com.slamd.server.SLAMDServer;
import com.slamd.server.SLAMDServerException;



/**
 * This class defines methods for dealing with SLAMD job packs.  A job pack is a
 * JAR file containing one or more SLAMD jobs and supporting files that may be
 * uploaded as a group and automatically registered with the SLAMD server.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobPack
{
  // The list of fields in the multipart form content.
  private List fieldList;

  // Information about the request from the client, including the file data.
  private RequestInfo requestInfo;

  // The SLAMD server with which to register the jobs.
  private SLAMDServer slamdServer;

  // The path to the directory in which the job classes should be placed.
  private String jobClassDirectory;

  // The path to the job pack file on the server's filesystem.
  private String filePath;



  /**
   * Creates a new job pack definition with the provided request.
   *
   * @param  requestInfo  Information about the request from the client,
   *                      including the file data.
   */
  public JobPack(RequestInfo requestInfo)
  {
    this.requestInfo = requestInfo;

    filePath          = null;
    fieldList         = requestInfo.multipartFieldList;
    slamdServer       = AdminServlet.slamdServer;
    jobClassDirectory = AdminServlet.classPath;
  }



  /**
   * Creates a new job pack definition from a file on the server's filesystem.
   *
   * @param  requestInfo  Information about the request from the client.
   * @param  filePath     The path to the job pack file on the server's
   *                      filesystem.
   */
  public JobPack(RequestInfo requestInfo, String filePath)
  {
    this.requestInfo = requestInfo;
    this.filePath    = filePath;

    slamdServer       = AdminServlet.slamdServer;
    jobClassDirectory = AdminServlet.classPath;
  }



  /**
   * Extracts the contents of the job pack and registers the included jobs with
   * the SLAMD server.
   *
   * @throws  SLAMDServerException  If a problem occurs while processing the job
   *                                pack JAR file.
   */
  public void processJobPack()
         throws SLAMDServerException
  {
    byte[] fileData  = null;
    File   tempFile  = null;
    String fileName  = null;
    String separator = System.getProperty("file.separator");

    if (filePath == null)
    {
      // First, get the request and ensure it is multipart content.
      HttpServletRequest request = requestInfo.request;
      if (! FileUpload.isMultipartContent(request))
      {
        throw new SLAMDServerException("Request does not contain multipart " +
                                       "content");
      }


      // Iterate through the request fields to get to the file data.
      Iterator iterator = fieldList.iterator();
      while (iterator.hasNext())
      {
        FileItem fileItem  = (FileItem) iterator.next();
        String   fieldName = fileItem.getFieldName();

        if (fieldName.equals(Constants.SERVLET_PARAM_JOB_PACK_FILE))
        {
          fileData = fileItem.get();
          fileName = fileItem.getName();
        }
      }

      // Make sure that a file was actually uploaded.
      if (fileData == null)
      {
        throw new SLAMDServerException("No file data was found in the " +
                                       "request.");
      }


      // Write the JAR file data to a temp file, since that's the only way we
      // can parse it.
      if (separator == null)
      {
        separator = "/";
      }

      tempFile = new File(jobClassDirectory + separator + fileName);
      try
      {
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        outputStream.write(fileData);
        outputStream.flush();
        outputStream.close();
      }
      catch (IOException ioe)
      {
        try
        {
          tempFile.delete();
        } catch (Exception e) {}

        ioe.printStackTrace();
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(ioe));
        throw new SLAMDServerException("I/O error writing temporary JAR " +
                                       "file:  " + ioe, ioe);
      }
    }
    else
    {
      tempFile = new File(filePath);
      if ((! tempFile.exists()) || (! tempFile.isFile()))
      {
        throw new SLAMDServerException("Specified job pack file \"" + filePath +
                                       "\" does not exist");
      }

      try
      {
        fileName = tempFile.getName();
        int fileLength = (int) tempFile.length();
        fileData = new byte[fileLength];

        FileInputStream inputStream = new FileInputStream(tempFile);
        int bytesRead = 0;
        while (bytesRead < fileLength)
        {
          bytesRead += inputStream.read(fileData, bytesRead,
                                        fileLength-bytesRead);
        }
        inputStream.close();
      }
      catch (Exception e)
      {
        slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                               JobClass.stackTraceToString(e));
        throw new SLAMDServerException("Error reading job pack file \"" +
                                       filePath + "\" -- " + e, e);
      }
    }


    StringBuilder htmlBody = requestInfo.htmlBody;


    // Parse the jar file
    JarFile     jarFile    = null;
    Manifest    manifest   = null;
    Enumeration jarEntries = null;
    try
    {
      jarFile    = new JarFile(tempFile, true);
      manifest   = jarFile.getManifest();
      jarEntries = jarFile.entries();
    }
    catch (IOException ioe)
    {
      try
      {
        if (filePath == null)
        {
          tempFile.delete();
        }
      } catch (Exception e) {}

      ioe.printStackTrace();
      slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                             JobClass.stackTraceToString(ioe));
      throw new SLAMDServerException("Unable to parse the JAR file:  " + ioe,
                                     ioe);
    }


    ArrayList<String> dirList = new ArrayList<String>();
    ArrayList<String> fileNameList = new ArrayList<String>();
    ArrayList<byte[]> fileDataList = new ArrayList<byte[]>();
    while (jarEntries.hasMoreElements())
    {
      JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
      String entryName = jarEntry.getName();
      if (jarEntry.isDirectory())
      {
        dirList.add(entryName);
      }
      else
      {
        try
        {
          int    entrySize = (int) jarEntry.getSize();
          byte[] entryData = new byte[entrySize];
          InputStream inputStream = jarFile.getInputStream(jarEntry);
          extractFileData(inputStream, entryData);
          fileNameList.add(entryName);
          fileDataList.add(entryData);
        }
        catch (IOException ioe)
        {
          try
          {
            jarFile.close();
            if (filePath == null)
            {
              tempFile.delete();
            }
          } catch (Exception e) {}

          ioe.printStackTrace();
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(ioe));
          throw new SLAMDServerException("I/O error parsing JAR entry " +
                                         entryName + " -- " + ioe, ioe);
        }
        catch (SLAMDServerException sse)
        {
          try
          {
            jarFile.close();
            if (filePath == null)
            {
              tempFile.delete();
            }
          } catch (Exception e) {}

          sse.printStackTrace();
          throw sse;
        }
      }
    }


    // If we have gotten here, then we have read all the data from the JAR file.
    // Delete the temporary file to prevent possible (although unlikely)
    // conflicts with data contained in the JAR.
    try
    {
      jarFile.close();
      if (filePath == null)
      {
        tempFile.delete();
      }
    } catch (Exception e) {}


    // Create the directory structure specified in the JAR file.
    if (! dirList.isEmpty())
    {
      htmlBody.append("<B>Created the following directories</B>" +
                      Constants.EOL);
      htmlBody.append("<BR>" + Constants.EOL);
      htmlBody.append("<UL>" + Constants.EOL);

      for (int i=0; i < dirList.size(); i++)
      {
        File dirFile = new File(jobClassDirectory + separator + dirList.get(i));
        try
        {
          dirFile.mkdirs();
          htmlBody.append("  <LI>" + dirFile.getAbsolutePath() + "</LI>" +
                          Constants.EOL);
        }
        catch (Exception e)
        {
          htmlBody.append("</UL>" + Constants.EOL);
          e.printStackTrace();
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(e));
          throw new SLAMDServerException("Unable to create directory \"" +
                                         dirFile.getAbsolutePath() + " -- " +
                                         e, e);
        }
      }

      htmlBody.append("</UL>" + Constants.EOL);
      htmlBody.append("<BR><BR>" + Constants.EOL);
    }


    // Write all the files to disk.  If we have gotten this far, then there
    // should not be any failures, but if there are, then we will have to
    // leave things in a "dirty" state.
    if (! fileNameList.isEmpty())
    {
      htmlBody.append("<B>Created the following files</B>" +
                      Constants.EOL);
      htmlBody.append("<BR>" + Constants.EOL);
      htmlBody.append("<UL>" + Constants.EOL);

      for (int i=0; i < fileNameList.size(); i++)
      {
        File dataFile = new File(jobClassDirectory + separator +
                                 fileNameList.get(i));

        try
        {
          // Make sure the parent directory exists.
          dataFile.getParentFile().mkdirs();
        } catch (Exception e) {}

        try
        {
          FileOutputStream outputStream = new FileOutputStream(dataFile);
          outputStream.write(fileDataList.get(i));
          outputStream.flush();
          outputStream.close();
          htmlBody.append("  <LI>" + dataFile.getAbsolutePath() + "</LI>" +
                          Constants.EOL);
        }
        catch (IOException ioe)
        {
          htmlBody.append("</UL>" + Constants.EOL);
          ioe.printStackTrace();
          slamdServer.logMessage(Constants.LOG_LEVEL_EXCEPTION_DEBUG,
                                 JobClass.stackTraceToString(ioe));
          throw new SLAMDServerException("Unable to write file " +
                                         dataFile.getAbsolutePath() + ioe, ioe);
        }
      }

      htmlBody.append("</UL>" + Constants.EOL);
      htmlBody.append("<BR><BR>" + Constants.EOL);
    }


    // Finally, parse the manifest to get the names of the classes that should
    // be registered with the SLAMD server.
    Attributes manifestAttributes = manifest.getMainAttributes();
    Attributes.Name key =
         new Attributes.Name(Constants.JOB_PACK_MANIFEST_REGISTER_JOBS_ATTR);
    String registerClassesStr = (String) manifestAttributes.get(key);
    if ((registerClassesStr == null) || (registerClassesStr.length() == 0))
    {
      htmlBody.append("<B>No job classes registered</B>" + Constants.EOL);
    }
    else
    {
      ArrayList<String> successList = new ArrayList<String>();
      ArrayList<String> failureList = new ArrayList<String>();

      StringTokenizer tokenizer = new StringTokenizer(registerClassesStr,
                                                      ", \t\r\n");
      while (tokenizer.hasMoreTokens())
      {
        String className = tokenizer.nextToken();

        try
        {
          JobClass jobClass = slamdServer.loadJobClass(className);
          slamdServer.addJobClass(jobClass);
          successList.add(className);
        }
        catch (Exception e)
        {
          failureList.add(className + ":  " + e);
        }
      }

      if (! successList.isEmpty())
      {
        htmlBody.append("<B>Registered Job Classes</B>" + Constants.EOL);
        htmlBody.append("<UL>" + Constants.EOL);
        for (int i=0; i < successList.size(); i++)
        {
          htmlBody.append("  <LI>" + successList.get(i) + "</LI>" +
                          Constants.EOL);
        }
        htmlBody.append("</UL>" + Constants.EOL);
        htmlBody.append("<BR><BR>" + Constants.EOL);
      }

      if (! failureList.isEmpty())
      {
        htmlBody.append("<B>Unable to Register Job Classes</B>" +
                        Constants.EOL);
        htmlBody.append("<UL>" + Constants.EOL);
        for (int i=0; i < failureList.size(); i++)
        {
          htmlBody.append("  <LI>" + failureList.get(i) + "</LI>" +
                          Constants.EOL);
        }
        htmlBody.append("</UL>" + Constants.EOL);
        htmlBody.append("<BR><BR>" + Constants.EOL);
      }
    }
  }



  /**
   * Reads the contents of the provided input stream into the given data array.
   * It will continue reading until the data array has been filled.
   *
   * @param  inputStream  The input stream from which to read the data.
   * @param  dataArray    The array into which the data should be placed after
   *                      it is read.
   *
   * @throws  IOException           If an I/O problem occurs while reading the
   *                                data from the input stream.
   * @throws  SLAMDServerException  If a problem occurs while trying to fill the
   *                                array with the data.
   */
  private void extractFileData(InputStream inputStream, byte[] dataArray)
          throws IOException, SLAMDServerException
  {
    int bytesRead = inputStream.read(dataArray);
    if (bytesRead == dataArray.length)
    {
      return;
    }
    else if (bytesRead < 0)
    {
      throw new SLAMDServerException("Unexpectedly reached the end of the " +
                                     "JAR entry input stream");
    }

    while (bytesRead < dataArray.length)
    {
      int bytesRemaining = dataArray.length - bytesRead;
      int moreBytesRead  = inputStream.read(dataArray, bytesRead,
                                            bytesRemaining);
      if (moreBytesRead < 0)
      {
        throw new SLAMDServerException("Unexpectedly reached the end of the " +
                                       "JAR entry input stream");
      }

      bytesRead += moreBytesRead;
    }
  }
}

