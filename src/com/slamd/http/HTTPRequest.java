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



import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.slamd.asn1.ASN1Element;

import com.unboundid.util.Base64;



/**
 * This class defines a means of encapsulating an HTTP request to send to a
 * remote server.  The request may use either the GET or POST method.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTTPRequest
{
  /**
   * The HTTP method for GET requests.
   */
  public static final String HTTP_METHOD_GET = "GET";



  /**
   * The HTTP method for POST requests.
   */
  public static final String HTTP_METHOD_POST = "POST";



  // The list of parameter names that should be used for this request.
  private ArrayList<String> parameterNames;

  // The list of parameter values that should be used for this request.
  private ArrayList<String> parameterValues;

  // Indicates whether this is a GET or POST request.
  private boolean isGet;

  // The mapping of the header information for this request.
  private LinkedHashMap<String,String> headerMap;

  // The request body.
  private String body;

  // The base URL for this request.
  URL baseURL;



  /**
   * Creates a new HTTP request with the provided method and base URL.  It will
   * not have any request headers or parameters.
   *
   * @param  isGet    Indicates whether this request should use the HTTP GET
   *                  method (if not, POST will be used).
   * @param  baseURL  The base URL to use for the request.
   */
  public HTTPRequest(boolean isGet, URL baseURL)
  {
    this.isGet   = isGet;
    this.baseURL = baseURL;

    if ((baseURL.getPath() == null) || (baseURL.getPath().length() == 0))
    {
      try
      {
        baseURL = new URL(baseURL.toExternalForm() + '/');
      } catch (Exception e) {}
    }

    headerMap       = new LinkedHashMap<String,String>();
    parameterNames  = new ArrayList<String>();
    parameterValues = new ArrayList<String>();
  }



  /**
   * Indicates whether this is an HTTP GET request.
   *
   * @return  <CODE>true</CODE> if this represents a GET request, or
   *          <CODE>false</CODE> if it represents a POST request.
   */
  public boolean isGet()
  {
    return isGet;
  }



  /**
   * Retrieves the HTTP method associated with this request.
   *
   * @return  The HTTP method associated with this request.
   */
  public String getRequestMethod()
  {
    if (isGet)
    {
      return HTTP_METHOD_GET;
    }
    else
    {
      return HTTP_METHOD_POST;
    }
  }



  /**
   * Retrieves the base URL for this request.
   *
   * @return  The base URL for this request.
   */
  public URL getBaseURL()
  {
    return baseURL;
  }



  /**
   * Retrieves the value of the requested header for this request.
   *
   * @param  name  The name of the header to retrieve.
   *
   * @return  The value of the requested header for this request, or
   *          <CODE>null</CODE> if no such header has been defined.
   */
  public String getHeader(String name)
  {
    return headerMap.get(name);
  }



  /**
   * Retrieves the map containing information about the headers for this
   * request.
   *
   * @return  The map containing information about the headers for this request.
   */
  public LinkedHashMap<String,String> getHeaderMap()
  {
    return headerMap;
  }



  /**
   * Adds the provided name and value to the set of headers for this request.
   * If a header already exists with the given name, it will be replaced with
   * the provided value.
   *
   * @param  name   The name of the header to add to the request.
   * @param  value  The value of the header to add.
   */
  public void setHeader(String name, String value)
  {
    headerMap.put(name, value);
  }



  /**
   * Removes the header with the specified name from this request.
   *
   * @param  name  The name of the header to remove from this request.
   */
  public void removeHeader(String name)
  {
    headerMap.remove(name);
  }



  /**
   * Removes all header information for this request.
   */
  public void clearHeaders()
  {
    headerMap.clear();
  }



  /**
   * Retrieves an array list whose string elements are the names of the
   * parameters associated with this request.
   *
   * @return  An array list whose string elements are the names of the
   *          parameters associated with this request.
   */
  public ArrayList getParameterNameList()
  {
    return parameterNames;
  }



  /**
   * Retrieves the names of the parameters for this request.
   *
   * @return  The names of the parameters for this request.
   */
  public String[] getParameterNames()
  {
    String[] nameArray = new String[parameterNames.size()];
    parameterNames.toArray(nameArray);
    return nameArray;
  }



  /**
   * Retrieves an array list whose string elements are the values of the
   * parameters associated with this request.  The order of the values will
   * correspond to the order in which the names are provided.
   *
   * @return  An array list whose string elements are the values of the
   *          parameters associated with this request.
   */
  public ArrayList getParameterValueList()
  {
    return parameterValues;
  }



  /**
   * Retrieves the values of the parameters for this request.  The order of the
   * values will correspond to the order in which the names are provided.
   *
   * @return  The values of the parameters for this request.
   */
  public String[] getParameterValues()
  {
    String[] valueArray = new String[parameterValues.size()];
    parameterValues.toArray(valueArray);
    return valueArray;
  }



  /**
   * Retrieves the value of the parameter with the specified name.  If the
   * given parameter has multiple values, then the first value will be returned.
   * If there is no such parameter for this entry, then <CODE>null</CODE> will
   * be returned.
   *
   * @param  parameterName  The name of the parameter for which to retrieve the
   *                        value.
   *
   * @return  The value for the requested parameter.
   */
  public String getParameterValue(String parameterName)
  {
    for (int i=0; i < parameterNames.size(); i++)
    {
      if (parameterNames.get(i).equalsIgnoreCase(parameterName))
      {
        return parameterValues.get(i);
      }
    }

    return null;
  }



  /**
   * Retrieves the set of values for the parameter with the specified name.  If
   * there is no such parameter for this entry, then an empty array will be
   * returned.
   *
   * @param  parameterName  The name of the parameter for which to retrieve the
   *                        set of values.
   *
   * @return  The set of values for the requested parameter.
   */
  public String[] getParameterValues(String parameterName)
  {
    ArrayList<String> valueList = new ArrayList<String>();

    for (int i=0; i < parameterNames.size(); i++)
    {
      if (parameterNames.get(i).equalsIgnoreCase(parameterName))
      {
        valueList.add(parameterValues.get(i));
      }
    }

    String[] valueArray = new String[valueList.size()];
    valueList.toArray(valueArray);
    return valueArray;
  }



  /**
   * Adds the specified parameter to this request.  If a parameter already
   * exists with the given name, then another value will be added.
   *
   * @param  name   The name of the value to add to this request.
   * @param  value  The value of the parameter to add to this request.
   */
  public void addParameter(String name, String value)
  {
    parameterNames.add(name);
    parameterValues.add(value);
  }



  /**
   * Adds the provided parameter to this request with a value that is already
   * properly URL-encoded.
   *
   * @param  name   The name to use for the parameter.
   * @param  value  The pre-encoded value to for the parameter.
   *
   * @throws  UnsupportedEncodingException  If a problem occurs while attempting
   *                                        to decode the value as UTF-8.
   */
  public void addEncodedParameter(String name, String value)
         throws UnsupportedEncodingException
  {
    // This is not very efficient, since we're decoding the value now only to
    // re-encode it later, but this method should not be used very frequently
    // anyway so we can take the hit to avoid having to write a lot of code to
    // make it more efficient.
    parameterNames.add(name);
    parameterValues.add(URLDecoder.decode(value, "UTF-8"));
  }



  /**
   * Adds the specified parameter to this request.  If a parameter already
   * exists with the given name, then another value will be added.
   *
   * @param  name    The name of the value to add to this request.
   * @param  values  The set of value for the parameter to add to this request.
   */
  public void addParameter(String name, String[] values)
  {
    for (int i=0; ((values != null) && (i < values.length)); i++)
    {
      parameterNames.add(name);
      parameterValues.add(values[i]);
    }
  }



  /**
   * Replaces any existing values for the specified parameter in this request
   * with the provided value.  If no values exist with the specified name, a new
   * parameter will be added.
   *
   * @param  name      The name of the parameter for which to replace any
   *                   existing values.
   * @param  newValue  The new value to use in place of any existing value(s).
   */
  public void replaceParameter(String name, String newValue)
  {
    for (int i=parameterNames.size()-1; i >= 0; i--)
    {
      if (parameterNames.get(i).equalsIgnoreCase(name))
      {
        parameterNames.remove(i);
        parameterValues.remove(i);
      }
    }

    parameterNames.add(name);
    parameterValues.add(newValue);
  }



  /**
   * Replaces any existing values for the specified parameter in this request
   * with the provided set of values.  If no values exist with the specified
   * name, a new parameter will be added.
   *
   * @param  name       The name of the parameter for which to replace any
   *                    existing values.
   * @param  newValues  The set of values to use in place of any existing
   *                    value(s).
   */
  public void replaceParameter(String name, String[] newValues)
  {
    for (int i=parameterNames.size()-1; i >= 0; i--)
    {
      if (parameterNames.get(i).equalsIgnoreCase(name))
      {
        parameterNames.remove(i);
        parameterValues.remove(i);
      }
    }

    for (int i=0; ((newValues != null) && (i < newValues.length)); i++)
    {
      parameterNames.add(name);
      parameterValues.add(newValues[i]);
    }
  }



  /**
   * Removes all values for the parameter with the given name.  If no values for
   * the specified parameter exist in the entry, then no action will be
   * performed.
   *
   * @param  name  The name of the header for which to remove any values from
   *               the request.
   */
  public void removeParameter(String name)
  {
    for (int i=parameterNames.size()-1; i >= 0; i--)
    {
      if (parameterNames.get(i).equalsIgnoreCase(name))
      {
        parameterNames.remove(i);
        parameterValues.remove(i);
      }
    }
  }



  /**
   * Removes the parameter with the specified name and value from this request.
   * If the specified parameter does not exist with the given value, then no
   * action will be performed.
   *
   * @param  name   The name of the parameter from which to remove the specified
   *                value.
   * @param  value  The value of the parameter to remove.
   */
  public void removeParameter(String name, String value)
  {
    for (int i=parameterNames.size()-1; i >= 0; i--)
    {
      if (parameterNames.get(i).equalsIgnoreCase(name) &&
          parameterValues.get(i).equalsIgnoreCase(value))
      {
        parameterNames.remove(i);
        parameterValues.remove(i);
        return;
      }
    }
  }



  /**
   * Removes all parameter information from this request.
   */
  public void removeAllParameters()
  {
    parameterNames.clear();
    parameterValues.clear();
  }



  /**
   * Retrieves the body for this request.  This will only be used for POST
   * operations, and only if no parameters have been provided.  It will only be
   * available if a body has been provided using the {@link #setBody} method.
   *
   * @return  The body to use for this request.
   */
  public String getBody()
  {
    return body;
  }



  /**
   * Sets the request body.  This is only available for POST operations, and if
   * any parameters have been provided, then the body will be ignored.
   *
   * @param  body  The body to use for the request.
   */
  public void setBody(String body)
  {
    this.body = body;
  }



  /**
   * Creates a string that is suitable for sending to an HTTP server or proxy.
   *
   * @param  client  The client that will be sending the request.
   *
   * @return  The string for sending to the HTTP server or proxy.
   */
  public String generateHTTPRequest(HTTPClient client)
  {
    StringBuilder buffer      = new StringBuilder();
    StringBuilder paramBuffer = null;

    String baseURLStr;
    if (client.proxyHost == null)
    {
      baseURLStr = baseURL.getFile();
    }
    else
    {
      baseURLStr = baseURL.toExternalForm();
    }

    if (isGet)
    {
      buffer.append("GET ");
      buffer.append(baseURLStr);

      if (! parameterNames.isEmpty())
      {
        if (baseURL.getQuery() == null)
        {
          buffer.append('?');
          buffer.append(parameterNames.get(0));
          buffer.append('=');
          buffer.append(encodeValue(parameterValues.get(0)));

          for (int i=1; i < parameterNames.size(); i++)
          {
            buffer.append('&');
            buffer.append(parameterNames.get(i));
            buffer.append('=');
            buffer.append(encodeValue(parameterValues.get(i)));
          }
        }
        else
        {
          for (int i=0; i < parameterNames.size(); i++)
          {
            buffer.append('&');
            buffer.append(parameterNames.get(i));
            buffer.append('=');
            buffer.append(encodeValue(parameterValues.get(i)));
          }
        }
      }

      buffer.append(" HTTP/1.1\r\n");
    }
    else
    {
      buffer.append("POST ");
      buffer.append(baseURLStr);
      buffer.append(" HTTP/1.1\r\n");

      if (! parameterNames.isEmpty())
      {
        String name = parameterNames.get(0);
        if ((name == null) || (name.length() == 0))
        {
          paramBuffer = new StringBuilder();
          paramBuffer.append(parameterValues.get(0));
        }
        else
        {
          paramBuffer = new StringBuilder();
          paramBuffer.append(parameterNames.get(0));
          paramBuffer.append('=');
          paramBuffer.append(encodeValue(parameterValues.get(0)));
        }

        for (int i=1; i < parameterNames.size(); i++)
        {
          name = parameterNames.get(i);
          if ((name == null) || (name.length() == 0))
          {
            paramBuffer.append('&');
            paramBuffer.append(parameterValues.get(i));
          }
          else
          {
            paramBuffer.append('&');
            paramBuffer.append(parameterNames.get(i));
            paramBuffer.append('=');
            paramBuffer.append(encodeValue(parameterValues.get(i)));
          }
        }
      }
    }

    buffer.append("Host: ");
    buffer.append(baseURL.getHost());
    if (baseURL.getPort() > 0)
    {
      buffer.append(':');
      buffer.append(baseURL.getPort());
    }
    buffer.append("\r\n");

    buffer.append("Connection: ");
    if (client.useKeepAlive)
    {
      buffer.append("Keep-Alive\r\n");
    }
    else
    {
      buffer.append("Close\r\n");
    }

    if (client.enableGZIP)
    {
      buffer.append("Accept-Encoding: gzip\r\n");
    }

    if (paramBuffer != null)
    {
      buffer.append("Content-Length: ");
      buffer.append(paramBuffer.length());
      buffer.append("\r\n");
    }
    else if (body != null)
    {
      buffer.append("Content-Length: ");
      buffer.append(body.length());
      buffer.append("\r\n");
    }

    boolean hasContentType = false;

    Iterator iterator = client.commonHeaderMap.keySet().iterator();
    while (iterator.hasNext())
    {
      String headerName = (String) iterator.next();
      if (headerName.equalsIgnoreCase("content-type"))
      {
        hasContentType = true;
      }

      if (headerMap.get(headerName) == null)
      {
        buffer.append(headerName);
        buffer.append(": ");
        buffer.append(client.commonHeaderMap.get(headerName));
        buffer.append("\r\n");
      }
    }

    iterator = headerMap.keySet().iterator();
    while (iterator.hasNext())
    {
      String headerName = (String) iterator.next();
      if (headerName.equalsIgnoreCase("content-type"))
      {
        hasContentType = true;
      }

      buffer.append(headerName);
      buffer.append(": ");
      buffer.append(headerMap.get(headerName));
      buffer.append("\r\n");
    }


    // If this is a POST request and the user didn't provide a content type,
    // then automatically include a default content type.
    if ((! isGet) && (! hasContentType))
    {
      buffer.append("Content-Type: application/x-www-form-urlencoded\r\n");
    }


    if ((client.proxyAuthID != null) && (client.proxyAuthPW != null))
    {
      String authStr = client.proxyAuthID + ':' + client.proxyAuthPW;
      buffer.append(HTTPClient.PROXY_AUTH_HEADER_PREFIX);
      buffer.append(Base64.encode(ASN1Element.getBytes(authStr)));
      buffer.append("\r\n");
    }

    if ((client.authID != null) && (client.authPW != null))
    {
      String authStr = client.authID + ':' + client.authPW;
      buffer.append(HTTPClient.AUTH_HEADER_PREFIX);
      buffer.append(Base64.encode(ASN1Element.getBytes(authStr)));
      buffer.append("\r\n");
    }

    HTTPCookie[] cookies = client.getCookies(baseURL);
    if (cookies.length > 0)
    {
      buffer.append("Cookie: ");
      buffer.append(cookies[0].getName());
      buffer.append('=');
      buffer.append(cookies[0].getValue());

      for (int i=1; i < cookies.length; i++)
      {
        buffer.append("; ");
        buffer.append(cookies[i].getName());
        buffer.append('=');
        buffer.append(cookies[i].getValue());
      }

      buffer.append("\r\n");
    }

    buffer.append("\r\n");

    if (paramBuffer != null)
    {
      buffer.append(paramBuffer);
    }
    else
    {
      buffer.append(body);
    }

    return buffer.toString();
  }



  /**
   * Encodes the provided value in a form suitable for including in an HTTP
   * request.  Any unsafe characters will be escaped.
   *
   * @param  parameterValue  The value to be encoded.
   *
   * @return  The properly encoded value.
   */
  public static String encodeValue(String parameterValue)
  {
    try
    {
      return URLEncoder.encode(parameterValue, "UTF-8");
    }
    catch (UnsupportedEncodingException uee)
    {
      // This should never happen.
      return parameterValue;
    }
  }



  /**
   * Creates a new HTTP request that is a copy of this request, optionally using
   * a different base URL.  This is particularly useful when following redirects
   * generated in response to POST requests.
   *
   * @return  The generated HTTP request.
   *
   * @param  baseURL  The base URL to use for the new request.  If this is null
   *                  then the base URL from this request will be used.
   */
  public HTTPRequest clone(URL baseURL)
  {
    HTTPRequest newRequest;
    if (baseURL == null)
    {
      newRequest = new HTTPRequest(isGet, this.baseURL);
    }
    else
    {
      newRequest = new HTTPRequest(isGet, baseURL);
    }

    newRequest.parameterNames.addAll(parameterNames);
    newRequest.parameterValues.addAll(parameterValues);
    newRequest.headerMap = new LinkedHashMap<String,String>(headerMap);

    return newRequest;
  }
}

