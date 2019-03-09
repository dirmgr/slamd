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
package com.slamd.misc;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * This class defines a servlet that makes it possible for a client to request
 * a file from the Web server, and have the Web server distribute a different
 * file for each request.  The individual files to distribute should exist in
 * a specified location on the filesystem and should be named with the same base
 * name followed by a period and a sequentially-incrementing number starting at
 * 1.  For example, if a request for the file "myfile" should return one of five
 * files, then the files to be retrieved should be named "myfile.1", "myfile.2",
 * "myfile.3", "myfile.4", and "myfile.5".
 *
 *
 * @author   Neil A. Wilson
 */
public class GetFile
       extends HttpServlet
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -8475709072466939471L;



  /**
   * The name of the servlet initialization parameter that specifies the
   * directory in which to look for the requested files.
   */
  public static final String SERVLET_INIT_PARAM_FILE_DIRECTORY =
       "file_directory";



  /**
   * The name of the servlet parameter that specifies the file to retrieve.
   */
  public static final String SERVLET_PARAM_FILENAME = "file";



  /**
   * The name of the servlet parameter that specifies the access mode that
   * should be used when retrieving the files.
   */
  public static final String SERVLET_PARAM_ACCESS_MODE = "mode";



  /**
   * The access mode that indicates that the files should be retrieved in
   * sequential order.
   */
  public static final String ACCESS_MODE_SEQUENTIAL = "sequential";



  /**
   * The access mode that indicates that the files should be retrieved in
   * random order.
   */
  public static final String ACCESS_MODE_RANDOM = "random";



  /**
   * The access mode that indicates that information about the specified file
   * should be removed from the file data cache.
   */
  public static final String ACCESS_MODE_REMOVE = "remove";



  // Indicates whether this servlet has been initialized.
  private static boolean initialized;

  // The cache used for this servlet.
  private static HashMap<String,GetFileCacheItem> fileDataCache;

  // The mutex used to provide threadsafe access to the cache.
  private static final Object cacheMutex = new Object();

  // The path to the directory in which the files served by this servlet reside.
  private String fileDirectory;

  // The reason that this servlet is unavailable.
  private static String unavailableReason;



  /**
   * Performs the necessary initialization for this servlet.
   */
  @Override()
  public void init()
  {
    // Create the file data cache.
    fileDataCache = new HashMap<String,GetFileCacheItem>();

    // Prepare for the worst, but hope for the best
    initialized       = false;
    unavailableReason = "Unknown error during servlet initialization.";

    // See if a file directory has been specified.  If so, then use it.
    fileDirectory = "";
    ServletConfig servletConfig = getServletConfig();
    if (servletConfig != null)
    {
      String dir =
           servletConfig.getInitParameter(SERVLET_INIT_PARAM_FILE_DIRECTORY);
      if (dir == null)
      {
        unavailableReason = "No file directory has been specified.  Please " +
                            "specify a value for the " +
                            SERVLET_INIT_PARAM_FILE_DIRECTORY +
                            " servlet initialization parameter.";
        return;
      }
      else
      {
        try
        {
          File dirFile = new File(dir);
          if (dirFile.exists())
          {
            if (dirFile.isDirectory())
            {
              fileDirectory = dir;
              initialized   = true;
              return;
            }
            else
            {
              unavailableReason = "Specified file directory \"" + dir +
                                  "\" exists but is not a directory";
              return;
            }
          }
          else
          {
            unavailableReason = "Specified file directory \"" + dir +
                                "\" does not exist or could not be accessed.";
            return;
          }
        }
        catch (Exception e)
        {
          unavailableReason = "Unable to find or access the file directory \"" +
                              dir + "\" -- " + e;
          return;
        }
      }
    }
  }



  /**
   * Processes the servlet request and returns the appropriate file.
   *
   * @param  request   Information about the request received from the client.
   * @param  response  Information about the response to send back to the
   *                   client.
   *
   * @throws  IOException  If a problem occurs while sending the response back
   *                       to the client.
   */
  @Override()
  public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException
  {
    // If the servlet was not properly initialized, then return an error.
    if (! initialized)
    {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("The servlet initialization did not " +
                                 "complete properly:  " + unavailableReason);
      return;
    }


    // Get the base name of the file to retrieve.
    String filename = request.getParameter(SERVLET_PARAM_FILENAME);
    if ((filename == null) || (filename.length() == 0))
    {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("Missing required parameter " +
                                 SERVLET_PARAM_FILENAME);
      return;
    }


    // Determine the access mode to use for the file.
    boolean sequential = true;
    String  modeStr    = request.getParameter(SERVLET_PARAM_ACCESS_MODE);
    if ((modeStr != null) && (modeStr.length() > 0))
    {
      if (modeStr.equalsIgnoreCase(ACCESS_MODE_RANDOM))
      {
        sequential = false;
      }
      else if (modeStr.equalsIgnoreCase(ACCESS_MODE_REMOVE))
      {
        synchronized (cacheMutex)
        {
          Object removedObject = fileDataCache.remove(filename);
          if (removedObject == null)
          {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No information about file \"" +
                                       filename +
                                       "\" exists in the file data cache.");
          }
          else
          {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Information about file \"" + filename +
                                       "\" has been removed from the cache.");
          }

          return;
        }
      }
      else if (! modeStr.equalsIgnoreCase(ACCESS_MODE_SEQUENTIAL))
      {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Invalid access mode specified -- must be " +
                                   "either " + ACCESS_MODE_SEQUENTIAL +
                                   " or " + ACCESS_MODE_RANDOM);
        return;
      }
    }


    // See if the file exists in the cache. If so, then get the cached info.  If
    // not, then see if the file actually exists on the filesystem.
    GetFileCacheItem cacheItem;
    synchronized (cacheMutex)
    {
      cacheItem = fileDataCache.get(filename);
      if (cacheItem == null)
      {
        cacheItem = createCacheItem(filename);
      }
    }


    // If the cache item is null, then return a "not found" response.
    if (cacheItem == null)
    {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("Could not find file \"" + filename + '"');
      return;
    }


    // Determine the actual filename to use and then try to get the file data.
    String fileToRetrieve;
    if (sequential)
    {
      fileToRetrieve = cacheItem.nextFileName();
    }
    else
    {
      fileToRetrieve = cacheItem.randomFileName();
    }
    sendFile(response, fileToRetrieve);
  }



  /**
   * Creates a cache item with information about the specified file.  If the
   * file does not exist, then it will return null.
   *
   * @param  filename  The name of the file to be cached.
   *
   * @return  The cache item with information about the specified file, or
   *          <CODE>null</CODE> if the file does not exist.
   */
  public GetFileCacheItem createCacheItem(String filename)
  {
    // First, see if the filename has any forward or backward slashes.  If so,
    // then reject it without checking, because it's an attempt to go outside
    // the file directory.
    if ((filename.indexOf('/') >= 0) || (filename.indexOf('\\') >= 0))
    {
      return null;
    }


    // Next, see if the file exists with a name of ".1".  If it does not exist
    // or is not accessible, then return null;
    File actualFile = new File(fileDirectory + '/' + filename + ".1");
    if ((! actualFile.exists()) || (! actualFile.isFile()))
    {
      return null;
    }


    // OK.  At least one file exists.  Find out how many there are.
    int numFiles = 1;
    while (true)
    {
      File testFile = new File(fileDirectory + '/' + filename + '.' +
                               (numFiles + 1));
      if (testFile.exists() && testFile.isFile())
      {
        numFiles++;
      }
      else
      {
        break;
      }
    }


    // Now we know how many files there are, so create a cache item, put it in
    // the cache, and return it to the caller.
    GetFileCacheItem cacheItem = new GetFileCacheItem(filename, numFiles);
    fileDataCache.put(filename, cacheItem);
    return cacheItem;
  }



  /**
   * Sends the contents of the specified file back to the client.
   *
   * @param  response        The response object to use to send the data back
   *                         to the client.
   * @param  fileToRetrieve  The name of the actual file to retrieve.
   *
   * @throws  IOException  If a problem occurs while trying to send the file
   *                       data to the client.
   */
  public void sendFile(HttpServletResponse response, String fileToRetrieve)
         throws IOException
  {
    byte[] buffer = new byte[4096];
    BufferedInputStream inputStream =
         new BufferedInputStream(new FileInputStream(fileDirectory + '/' +
                                                     fileToRetrieve));
    BufferedOutputStream outputStream =
         new BufferedOutputStream(response.getOutputStream());

    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) >= 0)
    {
      outputStream.write(buffer, 0, bytesRead);
    }

    inputStream.close();
    outputStream.flush();
    outputStream.close();
  }
}

