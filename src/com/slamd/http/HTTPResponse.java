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
package com.slamd.http;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;



/**
 * This class defines a means of encapsulating an HTTP response returned by a
 * server in response to an HTTP request.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTTPResponse
{
  // The set of cookie values included in this response.
  private ArrayList<String> cookieValueList;

  // The names of the headers included in this response.
  private ArrayList<String> headerNameList;

  // The values of the headers included in this response.
  private ArrayList<String> headerValueList;

  // The actual data associated with this response.
  private byte[] responseData;

  // The HTML document included in the response, if appropriate.
  private HTMLDocument htmlDocument;

  // The number of bytes contained in the content of the response.
  private int contentLength;

  // The HTTP status code for the response.
  private int statusCode;

  // The MIME type of the response.
  private String contentType;

  // The protocol version string for this response.
  private String protolVersion;

  // The response message for this response.
  private String responseMessage;

  // The URL of the request that generated this response.
  private URL requestURL;



  /**
   * Creates a new HTTP response with the provided status code.
   *
   * @param  requestURL       The URL of the request that generated this
   *                          response.
   * @param  statusCode       The HTTP status code for this response.
   * @param  protocolVersion  The protocol and version for this response.
   * @param  responseMessage  The message associated with this response.
   */
  public HTTPResponse(URL requestURL, int statusCode, String protocolVersion,
                      String responseMessage)
  {
    this.requestURL      = requestURL;
    this.statusCode      = statusCode;
    this.protolVersion   = protocolVersion;
    this.responseMessage = responseMessage;

    htmlDocument    = null;
    contentType     = null;
    contentLength   = -1;
    responseData    = new byte[0];
    cookieValueList = new ArrayList<String>();
    headerNameList  = new ArrayList<String>();
    headerValueList = new ArrayList<String>();
  }



  /**
   * Retrieves the URL of the request that generated this HTTP response.
   *
   * @return  The URL of the request that generated this HTTP response.
   */
  public URL getRequestURL()
  {
    return requestURL;
  }



  /**
   * Retrieves the status code for this HTTP response.
   *
   * @return  The status code for this HTTP response.
   */
  public int getStatusCode()
  {
    return statusCode;
  }



  /**
   * Retrieves the protocol version for this HTTP response.
   *
   * @return  The protocol version for this HTTP response.
   */
  public String getProtocolVersion()
  {
    return protolVersion;
  }



  /**
   * Retrieves the response message for this HTTP response.
   *
   * @return  The response message for this HTTP response.
   */
  public String getResponseMessage()
  {
    return responseMessage;
  }



  /**
   * Retrieves the value of the header with the specified name.  If the
   * specified header has more than one value, then only the first will be
   * retrieved.
   *
   * @param  headerName  The name of the header to retrieve.
   *
   * @return  The value of the header with the specified name, or
   *          <CODE>null</CODE> if no such header is available.
   */
  public String getHeader(String headerName)
  {
    String    lowerName = headerName.toLowerCase();

    for (int i=0; i < headerNameList.size(); i++)
    {
      if (lowerName.equals(headerNameList.get(i)))
      {
        return headerValueList.get(i);
      }
    }

    return null;
  }



  /**
   * Retrieves the set of values for the specified header.
   *
   * @param  headerName  The name of the header to retrieve.
   *
   * @return  The set of values for the specified header.
   */
  public String[] getHeaderValues(String headerName)
  {
    ArrayList<String> valueList = new ArrayList<String>();
    String    lowerName = headerName.toLowerCase();

    for (int i=0; i < headerNameList.size(); i++)
    {
      if (lowerName.equals(headerNameList.get(i)))
      {
        valueList.add(headerValueList.get(i));
      }
    }

    String[] values = new String[valueList.size()];
    valueList.toArray(values);
    return values;
  }



  /**
   * Adds a header with the given name and value to this response.
   *
   * @param  headerName   The name of the header to add to this response.
   * @param  headerValue  The value of the header to add to this response.
   */
  public void addHeader(String headerName, String headerValue)
  {
    String lowerName = headerName.toLowerCase();
    headerNameList.add(lowerName);
    headerValueList.add(headerValue);

    if (lowerName.equals("content-length"))
    {
      try
      {
        contentLength = Integer.parseInt(headerValue);
      } catch (NumberFormatException nfe) {}
    }
    else if (lowerName.equals("content-type"))
    {
      contentType = headerValue;
    }
    else if (lowerName.equals("set-cookie"))
    {
      cookieValueList.add(headerValue);
    }
  }



  /**
   * Retrieves a two-dimensional array containing the header data for this
   * response, with each element being an array containing a name/value pair.
   *
   * @return  A two-dimensional array containing the header data for this
   *          response.
   */
  public String[][] getHeaderElements()
  {
    String[][] headerElements = new String[headerNameList.size()][2];
    for (int i=0; i < headerNameList.size(); i++)
    {
      headerElements[i][0] = headerNameList.get(i);
      headerElements[i][1] = headerValueList.get(i);
    }

    return headerElements;
  }



  /**
   * Retrieves the raw data included in this HTTP response.  If the response did
   * not include any data, an empty array will be returned.
   *
   * @return  The raw data included in this HTTP response.
   */
  public byte[] getResponseData()
  {
    return responseData;
  }



  /**
   * Sets the actual data associated with this response.
   *
   * @param  responseData  The actual data associated with this response.
   *
   * @throws  IOException  If the data is GZIP-compressed and a problem occurs
   *                       during decompression.
   */
  public void setResponseData(byte[] responseData)
         throws IOException
  {
    if (responseData == null)
    {
      this.responseData = new byte[0];
    }
    else
    {
      // If the response is compressed, then decompress it.
      String contentEncoding = getHeader("content-encoding");
      if (contentEncoding != null)
      {
        contentEncoding = contentEncoding.toLowerCase();
        if (contentEncoding.contains("gzip"))
        {
          ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
          GZIPInputStream gzis = new GZIPInputStream(bais);

          ByteArrayOutputStream baos =
               new ByteArrayOutputStream(responseData.length);

          byte[] buffer = new byte[8192];
          int bytesRead = gzis.read(buffer);
          while (bytesRead > 0)
          {
            baos.write(buffer, 0, bytesRead);
            bytesRead = gzis.read(buffer);
          }

          responseData = baos.toByteArray();
        }
      }

      this.responseData = responseData;
    }
  }



  /**
   * Retrieves the content length associated with this response.
   *
   * @return  The content length associated with this response, or -1 if no
   *          content length is available.
   */
  public int getContentLength()
  {
    return contentLength;
  }



  /**
   * Retrieves the content type associated with this response.
   *
   * @return  The content type associated with this response, or
   *          <CODE>null</CODE> if no content type is available.
   */
  public String getContentType()
  {
    return contentType;
  }



  /**
   * Retrieves an array containing the values of the cookies that should be set
   * based on the information in this response.
   *
   * @return  An array containing the values of the cookies that should be set
   *          based on the information in this response.
   */
  public String[] getCookieValues()
  {
    String[] cookieValues = new String[cookieValueList.size()];
    cookieValueList.toArray(cookieValues);
    return cookieValues;
  }



  /**
   * Retrieves the HTML document associated with this response.
   *
   * @return  The HTML document associated with this response, or
   *          <CODE>null</CODE> if no HTML document is available.
   */
  public HTMLDocument getHTMLDocument()
  {
    if ((htmlDocument == null) && (contentType != null) &&
        (contentType.toLowerCase().contains("text/html")))
    {
      parseAsHTMLDocument(requestURL);
    }

    return htmlDocument;
  }



  /**
   * Parses the data associated with this response as an HTML document.
   *
   * @param  requestURL  The URL of the request that triggered this response.
   */
  public void parseAsHTMLDocument(URL requestURL)
  {
    try
    {
      String htmlString = new String(responseData);
      htmlDocument = new HTMLDocument(requestURL.toExternalForm(), htmlString);
    }
    catch (Exception e) { e.printStackTrace(); }
  }
}

