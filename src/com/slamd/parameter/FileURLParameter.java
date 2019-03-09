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
package com.slamd.parameter;



import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import com.slamd.common.SLAMDException;



/**
 * This class defines a parameter whose value specifies a file from which
 * clients may obtain data to use during processing.  The file specified must
 * be accessible to all clients.  If a client cannot access the specified file,
 * then an attempt to obtain its contents will result in an exception.  Note
 * that a call to retrieve the information in the file will read the entire
 * contents of that file into memory, so it is best to keep the files small.
 * That should also be done if the files are to be retrieved from a remote
 * server because of the transfer involved.
 * <BR>
 * The file specified in this parameter should actually be in the form of a URL.
 * The URL may be either a file URL that points to a file on the local or
 * network-attached filesystem (e.g., "file:///tmp/file.txt" would refer to the
 * file  "/tmp/file.txt"), or may be an HTTP URL that points to a
 * publicly-readable file on a Web server (via HTTP -- not HTTPS).
 *
 *
 * @author   Neil A. Wilson
 */
public class FileURLParameter
       extends Parameter
{
  /**
   * The size of the buffer used for reading binary data from the specified
   * file.
   */
  private static final int BUFFER_SIZE = 4096;



  /**
   * The number of columns to display by default if no other value is specified.
   */
  private static final int DEFAULT_VISIBLE_COLUMNS = 40;



  /**
   * The URL type that indicates that no URL was provided.
   */
  public static final int URL_TYPE_NONE = 0;



  /**
   * The URL type that indicates the file will be retrieved from the local
   * filesystem.
   */
  public static final int URL_TYPE_FILE = 1;



  /**
   * The URL type that indicates the file will be retrieved from a remote server
   * over HTTP.
   */
  public static final int URL_TYPE_HTTP = 2;



  /**
   * The URL type that indicates the file will be retrieved from a remote server
   * over HTTPS.
   */
  public static final int URL_TYPE_HTTPS = 3;



  /**
   * The URL type that indicates the file will be retrieved from a remote server
   * over FTP.
   */
  public static final int URL_TYPE_FTP = 4;



  // The port of the server to contact to retrieve the data.
  private int serverPort;

  // The type of URL we are using.
  private int urlType;

  // The number of columns to display.
  private int visibleColumns = DEFAULT_VISIBLE_COLUMNS;

  // The user ID to use to authenticate to the server.
  private String authID;

  // The password to use to authenticate to the server.
  private String authPassword;

  // The part of the URL that specifies the file (i.e., without the protocol,
  // host, and port).
  private String fileURI;

  // The address of the server to contact to retrieve the data.
  private String serverAddress;

  // The URL that specifies the file to retrieve.
  private String urlString;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public FileURLParameter()
  {
    super();


    // No additional implementation required.
  }



  /**
   * Creates a new file parameter with the specified name.  The display name
   * will be the same as the name, it will not have a description or value, and
   * it will not be required.
   *
   * @param  name  The name to use for this parameter.
   */
  public FileURLParameter(String name)
  {
    this(name, name, null, null, false);
  }



  /**
   * Creates a new file parameter with the specified name and required/optional
   * indicator.  The display name will be the same as the name, and it will not
   * have a description or value.
   *
   * @param  name        The name to use for this parameter.
   * @param  isRequired  Indicates whether this parameter is required to have a
   *                     value.
   */
  public FileURLParameter(String name, boolean isRequired)
  {
    this(name, name, null, null, isRequired);
  }



  /**
   * Creates a new file parameter with the specified name and display name.  It
   * will not have a description or value, and it will not be required.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   */
  public FileURLParameter(String name, String displayName)
  {
    this(name, displayName, null, null, false);
  }



  /**
   * Creates a new file parameter with the specified name, display name, and
   * required/optional indicator.  It will not have a description or value.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   */
  public FileURLParameter(String name, String displayName, boolean isRequired)
  {
    this(name, displayName, null, null, isRequired);
  }



  /**
   * Creates a new file parameter with the specified name, display name, and
   * file URL.  It will not have a description, and will not be required.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  fileURL      The URL specifying the location and access method for
   *                      the file to use for this parameter.
   */
  public FileURLParameter(String name, String displayName, URL fileURL)
  {
    this(name, displayName, null, fileURL, false);
  }



  /**
   * Creates a new file parameter with the specified name, display name, file
   * URL, and required/optional indicator.  It will not have a description.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  fileURL      The URL specifying the location and access method for
   *                      the file to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   */
  public FileURLParameter(String name, String displayName, URL fileURL,
                          boolean isRequired)
  {
    this(name, displayName, null, fileURL, isRequired);
  }



  /**
   * Creates a new file parameter with the specified name, display name,
   * description, and file URL.  It will not be required.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  fileURL      The URL specifying the location and access method for
   *                      the file to use for this parameter.
   */
  public FileURLParameter(String name, String displayName, String description,
                          URL fileURL)
  {
    this(name, displayName, description, fileURL, false);
  }



  /**
   * Creates a new file parameter with the specified name, display name,
   * description and file URL, and required/optional indicator.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  fileURL      The URL specifying the location and access method for
   *                      the file to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   */
  public FileURLParameter(String name, String displayName, String description,
                          URL fileURL, boolean isRequired)
  {
    super(name, displayName, description, isRequired,
          ((fileURL == null) ? null : fileURL.toExternalForm()));
    try
    {
      parseFileURL(((fileURL == null) ? null : fileURL.toExternalForm()),
                   false);
    }
    catch (InvalidValueException ive)
    {
      // This should never happen with a URL, so we'll just ignore it for now.
    }
  }



  /**
   * Creates a new file URL parameter with the specified information.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   * @param  urlString    The string representation of the URL specifying the
   *                      location and access method for the file to use for
   *                      this parameter.
   *
   * @throws  InvalidValueException  If the provided URL string does not contain
   *                                 a valid or supported URL.
   */
  public FileURLParameter(String name, String displayName, String description,
                          boolean isRequired, String urlString)
         throws InvalidValueException
  {
    super(name, displayName, description, isRequired, urlString);
    parseFileURL(urlString, false);
  }



  /**
   * Creates a new file URL parameter based on a string representation of the
   * URL.  If the URL is not accepted, then <CODE>null</CODE> will be returned.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  urlString    The URL specifying the location and access method for
   *                      the file to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   *
   * @return  The file URL parameter that has been created.
   */
  static FileURLParameter fromURLString(String name, String displayName,
                                        String description, String urlString,
                                        boolean isRequired)
  {
    try
    {
      return new FileURLParameter(name, displayName, description, isRequired,
                                  urlString);
    }
    catch (InvalidValueException ive)
    {
      ive.printStackTrace();
      return null;
    }
  }



  /**
   * Parses the file URL to ensure that it is one that may be used in this
   * implementation.
   *
   * @param  workingURL    The URL to be parsed.
   * @param  validateOnly  Indicates whether to actually apply the contents of
   *                       the URL to this parameter, or if only validation
   *                       should be done without actually setting any
   *                       instance variables.
   *
   * @throws  InvalidValueException  If the provided URL cannot be parsed, or
   *                                 does not appear to be a supported URL type.
   */
  private void parseFileURL(String workingURL, boolean validateOnly)
          throws InvalidValueException
  {
    if ((workingURL == null) || (workingURL.length() == 0))
    {
      if (isRequired())
      {
        throw new InvalidValueException("No value provided for required " +
                                        "parameter");
      }
      else
      {
        if (! validateOnly)
        {
          urlType       = URL_TYPE_NONE;
          authID        = null;
          authPassword  = null;
          serverAddress = null;
          serverPort    = -1;
          fileURI       = null;
        }

        return;
      }
    }

    final String lowerURL   = workingURL.toLowerCase();
    final int colonSlashPos = lowerURL.indexOf(":/");
    if ((colonSlashPos < 0) && lowerURL.startsWith("/"))
    {
      // If there is no ":/" anywhere in the URL and it starts with a slash,
      // then assume that it's simply a file path.
      urlType       = URL_TYPE_FILE;
      authID        = null;
      authPassword  = null;
      serverAddress = null;
      serverPort    = -1;
      fileURI       = workingURL;
      return;
    }

    if (lowerURL.startsWith("file:/"))
    {
      if (! validateOnly)
      {
        urlType       = URL_TYPE_FILE;
        authID        = null;
        authPassword  = null;
        serverAddress = null;
        serverPort    = -1;
        fileURI       = workingURL.substring(5);
      }
    }
    else if (lowerURL.startsWith("http://"))
    {
      parseURLInternal(workingURL, URL_TYPE_HTTP, validateOnly);
    }
    else if (lowerURL.startsWith("https://"))
    {
      parseURLInternal(workingURL, URL_TYPE_HTTPS, validateOnly);
    }
    else if (lowerURL.startsWith("ftp://"))
    {
      parseURLInternal(workingURL, URL_TYPE_FTP, validateOnly);
    }
    else
    {
      throw new InvalidValueException("Not a supported protocol.");
    }
  }



  /**
   * Parses the provided URL to extract the information it contains, including
   * the host, port, auth ID, auth password, and request URI.
   *
   * @param  workingURL    The original URL provided by the end user.
   * @param  urlType       The type of URL to be parsed.
   * @param  validateOnly  Indicates whether to actually apply the contents of
   *                       the URL to this parameter, or if only validation
   *                       should be done without actually setting any
   *                       instance variables.
   *
   * @throws  InvalidValueException  If the URL is malformed.
   */
  private void parseURLInternal(String workingURL, int urlType,
                                boolean validateOnly)
          throws InvalidValueException
  {
    if (! validateOnly)
    {
      this.urlType = urlType;
    }

    int startPos;
    int defaultPort;
    switch (urlType)
    {
      case URL_TYPE_HTTP:
        startPos    = 7;
        defaultPort = 80;
        break;
      case URL_TYPE_HTTPS:
        startPos    = 8;
        defaultPort = 443;
        break;
      case URL_TYPE_FTP:
        startPos    = 6;
        defaultPort = 21;
        break;
      default:
        throw new InvalidValueException("Unsupported protocol type in URL " +
                                        workingURL);
    }

    int endPos = workingURL.indexOf('/', startPos);
    String hostPort = null;
    if (endPos < 0)
    {
      // Technically, this is not a valid URL.  However, it could be something
      // like "http://www.example.com", in which they really meant
      // "http://www.example.com/", so assume that's the case.
      hostPort = workingURL.substring(startPos);
      if (! validateOnly)
      {
        fileURI  = "/";
      }
    }
    else if (startPos == endPos)
    {
      // There was no host/port section at all.  We'll assume an address of
      // localhost and the default port.
      // FIXME -- This only works for IPv4, not for IPv6.
      hostPort = "127.0.0.1:" + defaultPort;
      if (! validateOnly)
      {
        fileURI = workingURL.substring(endPos);
      }
    }
    else
    {
      // There is an explicit host/port section and a file section.  Deal with
      // both of them.
      hostPort = workingURL.substring(startPos, endPos);
      if (! validateOnly)
      {
        fileURI = workingURL.substring(endPos);
      }
    }


    // At this point, we should be done with the file URI.  However, we still
    // need to extract the pertinent information from the host/port section.
    // First, see if there is an "@" sign to indicate authentication.
    int atPos = hostPort.indexOf('@');
    if (atPos > 0)
    {
      String authStr = hostPort.substring(0, atPos);
      hostPort = hostPort.substring(atPos+1);

      // The authStr should be in the form username:password.
      int colonPos = authStr.indexOf(':');
      if ((colonPos <= 0) || (colonPos == (authStr.length() - 1)))
      {
        throw new InvalidValueException("Unable to extract authentication ID " +
                                        "and/or password from \"" + authStr +
                                        "\" in URL \"" + workingURL + '"');
      }
      else
      {
        if (! validateOnly)
        {
          authID       = authStr.substring(0, colonPos);
          authPassword = authStr.substring(colonPos+1);
        }
      }
    }
    else
    {
      if (! validateOnly)
      {
        authID       = null;
        authPassword = null;
      }
    }


    // The only thing that should be left is the host and port.
    int colonPos = hostPort.indexOf(':');
    if (colonPos < 0)
    {
      // There was no colon at all.  So we have just been given the host but no
      // port.  Use the default port for the given protocol.
      if (! validateOnly)
      {
        serverAddress = hostPort;
        serverPort    = defaultPort;
      }
    }
    else if (colonPos == 0)
    {
      // We were only given a port.  Assume the loopback address.
      // FIXME -- This only works for IPv4, not for IPv6.
      if (! validateOnly)
      {
        serverAddress = "127.0.0.1";
      }

      try
      {
        int portValue = Integer.parseInt(hostPort.substring(1));
        if (! validateOnly)
        {
          serverPort = portValue;
        }
      }
      catch (NumberFormatException nfe)
      {
        throw new InvalidValueException("Unable to parse port number from " +
                                        "value \"" + hostPort.substring(1) +
                                        "\" in URL \"" + workingURL + '"',
                                        nfe);
      }
    }
    else
    {
      // We were given both a host and a port.  Use them separately.
      if (! validateOnly)
      {
        serverAddress = hostPort.substring(0, colonPos);
      }

      try
      {
        int portValue = Integer.parseInt(hostPort.substring(colonPos+1));
        if (! validateOnly)
        {
          serverPort = portValue;
        }
      }
      catch (NumberFormatException nfe)
      {
        throw new InvalidValueException("Unable to parse port number from " +
                                        "value \"" +
                                        hostPort.substring(colonPos+1) +
                                        "\" in URL \"" + workingURL + '"',
                                        nfe);
      }
    }
  }



  /**
   * Retrieves the URL associated with this file parameter.  Note that this may
   * not contain all information in the URL, since the <CODE>java.net.URL</CODE>
   * object does not have any way of dealing with authentication data.
   *
   * @return  The URL associated with this file parameter.
   */
  public URL getFileURL()
  {
    try
    {
      switch (urlType)
      {
        case URL_TYPE_FILE:
          return new URL("file://" + fileURI);
        case URL_TYPE_HTTP:
          return new URL("http://" + serverAddress + ':' + serverPort +
                         fileURI);
        case URL_TYPE_HTTPS:
          return new URL("https://" + serverAddress + ':' + serverPort +
                         fileURI);
        case URL_TYPE_FTP:
          return new URL("ftp://" + serverAddress + ':' + serverPort + fileURI);
        default:
          return null;
      }
    }
    catch (MalformedURLException mue)
    {
      // This should never be thrown, but just in case....
      return null;
    }
  }



  /**
   * Retrieves the contents of the file as an array of strings corresponding to
   * the lines in the file.
   *
   * @return  The contents of the file as an array of lines.
   *
   * @throws  IOException  If a problem occurs while attempting to access the
   *                       information in the specified file.
   *
   * @throws  SLAMDException  If a problem is encountered while parsing the file
   *                          URL to retrieve the appropriate information from
   *                          it.
   */
  public String[] getFileLines()
         throws IOException, SLAMDException
  {
    URLConnection conn = getConnection();
    BufferedReader reader =
         new BufferedReader(new InputStreamReader(conn.getInputStream()));

    ArrayList<String> lineList = new ArrayList<String>();
    String line;
    while ((line = reader.readLine()) != null)
    {
      lineList.add(line);
    }

    reader.close();

    String[] lines = new String[lineList.size()];
    lineList.toArray(lines);
    return lines;
  }



  /**
   * Retrieves the contents of the file as an array of strings corresponding to
   * the lines in the file, with blank lines omitted.
   *
   * @return  The contents of the file as an array of lines with blank lines
   *          omitted.
   *
   * @throws  IOException  If a problem occurs while attempting to access the
   *                       information in the specified file.
   *
   * @throws  SLAMDException  If a problem is encountered while parsing the file
   *                          URL to retrieve the appropriate information from
   *                          it.
   */
  public String[] getNonBlankFileLines()
         throws IOException, SLAMDException
  {
    URLConnection conn = getConnection();
    BufferedReader reader =
         new BufferedReader(new InputStreamReader(conn.getInputStream()));

    ArrayList<String> lineList = new ArrayList<String>();
    String line;
    while ((line = reader.readLine()) != null)
    {
      if (line.length() > 0)
      {
        lineList.add(line);
      }
    }

    reader.close();

    String[] lines = new String[lineList.size()];
    lineList.toArray(lines);
    return lines;
  }



  /**
   * Retrieves the contents of the file as an array of the bytes contained in
   * that file.
   *
   * @return  The binary contents of the specified file.
   *
   * @throws  IOException  If a problem occurs while attempting to access the
   *                       information in the specified file.
   *
   * @throws  SLAMDException  If a problem is encountered while parsing the file
   *                          URL to retrieve the appropriate information from
   *                          it.
   */
  public byte[] getRawFileData()
         throws IOException, SLAMDException
  {
    URLConnection conn = getConnection();
    BufferedInputStream inputStream =
         new BufferedInputStream(conn.getInputStream());


    byte[] buffer = new byte[BUFFER_SIZE];
    ArrayList<byte[]> readList = new ArrayList<byte[]>();
    int bytesRead = 0;
    int totalBytesRead = 0;
    while ((bytesRead = inputStream.read(buffer)) > 0)
    {
      byte[] byteArray = new byte[bytesRead];
      System.arraycopy(buffer, 0, byteArray, 0, bytesRead);
      readList.add(byteArray);
      totalBytesRead += bytesRead;
    }

    inputStream.close();


    int currentPos = 0;
    byte[] returnArray = new byte[totalBytesRead];
    for (int i=0; i < readList.size(); i++)
    {
      byte[] byteArray = readList.get(i);
      System.arraycopy(byteArray, 0, returnArray, currentPos,
                       byteArray.length);
      currentPos += byteArray.length;
    }
    return returnArray;
  }



  /**
   * Retrieves an input stream that allows the caller to get access to the
   * information in the file.
   *
   * @return  An input stream that allows the caller to get access to the
   *          information in the file.
   *
   * @throws  IOException  If a problem occurs while opening the input stream.
   *
   * @throws  SLAMDException  If a problem is encountered while parsing the file
   *                          URL to retrieve the appropriate information from
   *                          it.
   */
  public InputStream getInputStream()
         throws IOException, SLAMDException
  {
    URLConnection conn = getConnection();
    return conn.getInputStream();
  }



  /**
   * Creates a URL connection that is ready to start reading the file.
   *
   * @return  A URL connection that is ready to start reading the file.
   *
   * @throws  IOException  If a problem occurs while establishing the
   *                       connection.
   */
  private URLConnection getConnection()
          throws IOException
  {
    switch (urlType)
    {
      case URL_TYPE_NONE:
        throw new IOException("No URL provided");
      case URL_TYPE_FILE:
        return getFileURL().openConnection();
      case URL_TYPE_HTTP:
      case URL_TYPE_HTTPS:
        // HTTP and HTTPS URLs use an Authenticator
        if ((authID != null) && (authPassword != null))
        {
          Authenticator.setDefault(new FileURLAuthenticator(authID,
                                                            authPassword));
        }
        return getFileURL().openConnection();
      case URL_TYPE_FTP:
        // FTP URLs should have the username:password encoded in them, and they
        // also need a "%2F" encoded to indicate that the URL should start
        // from the root of the FTP server's filesystem.
        String ftpURLStr = "ftp://";
        if ((authID != null) && (authPassword != null))
        {
          ftpURLStr += authID + ':' + authPassword + '@';
        }
        ftpURLStr += serverAddress + ':' + serverPort + "/%2F" + fileURI;
        URL ftpURL = new URL(ftpURLStr);
        return ftpURL.openConnection();
      default:
        throw new IOException("Unknown/unsupported URL type " + urlType);
    }
  }



  /**
   * Retrieves the file URL associated with this parameter.
   *
   * @return  The file URL associated with this parameter.
   */
  @Override()
  public String getValue()
  {
    return urlString;
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws  InvalidValueException  If the provided value is not acceptable for
   *                                 use with this parameter.
   */
  @Override()
  public void setValue(Object value)
         throws InvalidValueException
  {
    String invalidReason = getInvalidReason(value);
    if (invalidReason != null)
    {
      throw new InvalidValueException(invalidReason);
    }


    if (value == null)
    {
      urlString = null;
    }
    else if (value instanceof URL)
    {
      urlString = ((URL) value).toExternalForm();
    }
    else if (value instanceof String)
    {
      String valueStr = (String) value;
      if (valueStr.length() == 0)
      {
        urlString = null;
      }
      else
      {
        urlString = (String) value;
      }
    }

    this.value = urlString;
    parseFileURL(urlString, false);
  }



  /**
   * Sets the value for this parameter from the information in the provided
   * parameter.  Note that the provided parameter must be of the same type
   * as this parameter or no action will be taken.
   *
   * @param  parameter  The parameter from which to take the value for this
   *                    parameter.
   */
  @Override()
  public void setValueFrom(Parameter parameter)
  {
    if ((parameter != null) && (parameter instanceof FileURLParameter))
    {
      FileURLParameter fup = (FileURLParameter) parameter;
      this.value         = fup.value;
      this.urlString     = fup.urlString;
      this.urlType       = fup.urlType;
      this.serverAddress = fup.serverAddress;
      this.serverPort    = fup.serverPort;
      this.fileURI       = fup.fileURI;
      this.authID        = fup.authID;
      this.authPassword  = fup.authPassword;
    }
  }



  /**
   * Retrieves the value of this parameter in text form.
   *
   * @return  The value of this parameter in text form.
   */
  @Override()
  public String getValueString()
  {
    if (urlString == null)
    {
      return "";
    }
    else
    {
      return urlString;
    }
  }



  /**
   * Specifies the value to use for this parameter from the provided String.
   * Note that no validation is performed with this method.
   *
   * @param  valueString  The string representation of the value to use for this
   *                      parameter.
   *
   * @throws  InvalidValueException  If the provided value cannot be used to
   *                                 provide a value for this parameter.
   */
  @Override()
  public void setValueFromString(String valueString)
         throws InvalidValueException
  {
    setValue(valueString);
  }



  /**
   * Retrieves the reason that the specified value is not acceptable for use
   * with this parameter.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return  The reason that the specified value is not acceptable for use
   *          with this parameter, or <CODE>null</CODE> if it is acceptable.
   */
  @Override()
  public String getInvalidReason(Object value)
  {
    if (value == null)
    {
      if (isRequired)
      {
        return "No value provided for required parameter";
      }
      else
      {
        return null;
      }
    }
    else if (value instanceof URL)
    {
      try
      {
        parseFileURL(((URL) value).toExternalForm(), true);
        return null;
      }
      catch (InvalidValueException ive)
      {
        return ive.getMessage();
      }
    }
    else if (value instanceof String)
    {
      try
      {
        parseFileURL((String) value, true);
        return null;
      }
      catch (InvalidValueException ive)
      {
        return ive.getMessage();
      }
    }
    else
    {
      return (value.getClass().getName() + " is not a supported file URL " +
              "object type");
    }
  }



  /**
   * Retrieves the value of the parameter in a form that may be displayed to the
   * end user.
   *
   * @return  The value of the parameter in a form that may be displayed to the
   *          end user.
   */
  @Override()
  public String getDisplayValue()
  {
    return getValueString();
  }



  /**
   * Retrieves the value of the parameter in a form that may be displayed to the
   * end user as part of an HTML document.
   *
   * @return  The value of the parameter in a form that may be displayed to the
   *          end user as part of an HTML document.
   */
  @Override()
  public String getHTMLDisplayValue()
  {
    String valueString = getValueString();
    if ((valueString == null) || (valueString.length() == 0))
    {
      return "";
    }
    else
    {
      return "<A HREF=\"" + valueString + "\">" + valueString + "</A>";
    }
  }



  /**
   * Retrieves the number of columns that should be visible in the HTML input
   * form.
   *
   * @return  The number of columns that should be visible in the HTML input
   *          form.
   */
  public int getVisibleColumns()
  {
    return visibleColumns;
  }



  /**
   * Specifies the number of columns that should be visible in the HTML input
   * form.
   *
   * @param  visibleColumns  The number of columns that should be visible in the
   *                         HTML input form.
   */
  public void setVisibleColumns(int visibleColumns)
  {
    this.visibleColumns = visibleColumns;
  }



  /**
   * Retrieves a string of text that can be used to request a value for this
   * parameter using an HTML form.  Note that this should just be for the input
   * field itself and should not use the display name or have any special marker
   * to indicate whether the value is required or not, as those are to be added
   * by whatever is generating the HTML page.
   *
   * @param  prefix  The prefix that should be placed in front of the parameter
   *                 name as the name of the form element.
   *
   * @return  A string of text that can be used to request a value for this
   *          parameter using an HTML form.
   */
  @Override()
  public String getHTMLInputForm(String prefix)
  {
    String returnStr = "<INPUT TYPE=\"TEXT\" NAME=\"" + prefix + name +
                       "\" VALUE=\"" + ((urlString == null) ? "" : urlString) +
                       "\" SIZE=\"" + visibleColumns + "\">";

    return returnStr;
  }



  /**
   * Specifies the value of this parameter based on the provided text that would
   * be returned from posting an HTML form.
   *
   * @param  values  The set of values for this parameter contained in the
   *                 servlet request.
   *
   * @throws  InvalidValueException  If the specified value is not acceptable
   *                                 for this parameter.
   */
  @Override()
  public void htmlInputFormToValue(String[] values)
         throws InvalidValueException
  {
    if ((values == null) || (values.length == 0))
    {
      setValue(null);
    }
    else if ((values.length == 1) && (values[0].length() == 0))
    {
      setValue(null);
    }
    else
    {
      setValue(values[0]);
    }
  }



  /**
   * Retrieves a string representation of the content that should be included in
   * an HTML form in which this parameter should be provided as a hidden
   * element.
   *
   * @param  prefix  The prefix to use for the parameter name.
   *
   * @return  A string representation of this parameter as a hidden element in
   *          an HTML form.
   */
  @Override()
  public String generateHidden(String prefix)
  {
    String urlStr = urlString;
    if (urlStr == null)
    {
      urlStr = "";
    }

    return "<INPUT TYPE=\"HIDDEN\" NAME=\"" + prefix + name +
           "\" VALUE=\"" + urlStr + "\">";
  }



  /**
   * Creates a clone of this parameter.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public FileURLParameter clone()
  {
    FileURLParameter fup = new FileURLParameter();
    fup.setName(name);
    fup.setDisplayName(displayName);
    fup.setDescription(description);
    fup.setRequired(isRequired);
    fup.setValueFrom(this);
    fup.setVisibleColumns(visibleColumns);
    fup.setSensitive(isSensitive());
    return fup;
  }
}

