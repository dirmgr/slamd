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



import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.StringTokenizer;



/**
 * This class defines a cookie that may be used to help retain state information
 * between requests.
 *
 *
 * @author   Neil A. Wilson
 */
public class HTTPCookie
{
  /**
   * The date formatter that will be used to parse dates from cookies.
   */
  public static final SimpleDateFormat EXPIRATION_DATE_FORMAT =
       new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");



  // Indicates whether this cookie should only be returned for secure
  // connections.
  private boolean secure;

  // The hash map of all extra (unrecognized) properties included in the cookie.
  private HashMap<String,String> extraProperties;

  // The version for this cookie.
  private int version;

  // The time that this cookie should expire.
  private long expirationDate;

  // The comment for this cookie;
  private String comment;

  // The domain with which this cookie should be used.
  private String domain;

  // The name of the data associated with this cookie.
  private String name;

  // The path of the request document with which this cookie should be used.
  private String path;

  // The value of the data associated with this cookie.
  private String value;



  /**
   * Creates a new cookie by parsing it from the provided string.
   *
   * @param  requestURL    The URL of the request with which the cookie is
   *                       associated.
   * @param  cookieString  The string to be parsed to create the cookie.
   *
   * @throws  HTTPException  If a problem occurs while attempting to parse the
   *                         provided string as a cookie.
   */
  public HTTPCookie(URL requestURL, String cookieString)
         throws HTTPException
  {
    // Set default values for all the elements.
    name            = null;
    value           = null;
    domain          = null;
    path            = null;
    expirationDate  = -1;
    secure          = false;
    version         = -1;
    comment         = null;
    extraProperties = new HashMap<String,String>();


    // Parse the cookie string into the individual elements.
    StringTokenizer tokenizer = new StringTokenizer(cookieString, ";");
    while (tokenizer.hasMoreTokens())
    {
      String token      = tokenizer.nextToken().trim();

      String tokenName;
      String lowerName;
      String tokenValue;
      int    equalPos = token.indexOf('=');
      if (equalPos < 0)
      {
        tokenName  = token;
        lowerName  = tokenName.toLowerCase();
        tokenValue = "";
      }
      else
      {
        tokenName  = token.substring(0, equalPos);
        lowerName  = tokenName.toLowerCase();
        tokenValue = token.substring(equalPos+1);
      }

      if (name == null)
      {
        // This must be the first element in the cookie, which must be the name.
        name  = tokenName;
        value = tokenValue;
      }
      else if (lowerName.equals("secure"))
      {
        secure = true;
      }
      else if (lowerName.equals("domain"))
      {
        domain = tokenValue.toLowerCase();
      }
      else if (lowerName.equals("path"))
      {
        path = tokenValue;
      }
      else if (lowerName.equals("expires"))
      {
        try
        {
          expirationDate =
               EXPIRATION_DATE_FORMAT.parse(token.substring(8)).getTime();
        }
        catch (Exception e)
        {
          throw new HTTPException("Unable to parse cookie expiration date:  " +
                                  e, e);
        }
      }
      else if (lowerName.equals("max-age"))
      {
        try
        {
          expirationDate = System.currentTimeMillis() +
                           (1000 * Integer.parseInt(tokenValue));
        }
        catch (Exception e)
        {
          throw new HTTPException("Unable to parse cookie max-age:  " + e, e);
        }
      }
      else if (lowerName.equals("comment"))
      {
        comment = tokenValue;
      }
      else if (lowerName.equals("version"))
      {
        try
        {
          version = Integer.parseInt(token.substring(8));
        }
        catch (Exception e)
        {
          throw new HTTPException("Unable to parse cookie version:  " + e, e);
        }
      }
      else
      {
        extraProperties.put(lowerName, tokenValue);
      }
    }


    // Make sure that a name and value were available.  If not, then this was an
    // invalid cookie.
    if (name == null)
    {
      throw new HTTPException("Unable to parse the cookie:  no name provided");
    }
    else if (value == null)
    {
      throw new HTTPException("Unable to parse the cookie:  no value provided");
    }


    // See if a domain was provided.  If not, then set it to the address used
    // for the request.
    if (domain == null)
    {
      domain = requestURL.getHost();
    }


    // See if a path was provided.  If not, then set it to the path for the
    // request.
    if (path == null)
    {
      path = requestURL.getPath();
    }
  }



  /**
   * Creates a new cookie with the provided information.
   *
   * @param  name            The name of the data associated with this cookie.
   * @param  value           The value of the data associated with this cookie.
   * @param  domain          The domain with which this cookie should be used.
   * @param  path            The path for which this cookie should be used.
   * @param  expirationDate  The time that this cookie should expire, or -1 if
   *                         there should be no expiration date.
   * @param  secure          Indicates whether this cookie should only be
   *                         provided over a secure connection.
   */
  public HTTPCookie(String name, String value, String domain, String path,
                    long expirationDate, boolean secure)
  {
    this.name           = name;
    this.value          = value;
    this.domain         = domain;
    this.path           = path;
    this.expirationDate = expirationDate;
    this.secure         = secure;

    extraProperties = new HashMap<String,String>(0);
    comment         = null;
    version         = -1;
  }



  /**
   * Creates a new cookie with the provided information.
   *
   * @param  name            The name of the data associated with this cookie.
   * @param  value           The value of the data associated with this cookie.
   * @param  domain          The domain with which this cookie should be used.
   * @param  path            The path for which this cookie should be used.
   * @param  expirationDate  The time that this cookie should expire, or -1 if
   *                         there should be no expiration date.
   * @param  secure          Indicates whether this cookie should only be
   *                         provided over a secure connection.
   * @param  comment         The comment for this cookie.
   * @param  version         The version for this cookie, or -1 if there is no
   *                         version.
   */
  public HTTPCookie(String name, String value, String domain, String path,
                    long expirationDate, boolean secure, String comment,
                    int version)
  {
    this.name           = name;
    this.value          = value;
    this.domain         = domain;
    this.path           = path;
    this.expirationDate = expirationDate;
    this.secure         = secure;
    this.comment        = comment;
    this.version        = version;

    extraProperties = new HashMap<String,String>(0);
  }



  /**
   * Retrieves the name for this cookie.
   *
   * @return  The name for this cookie.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Retrieves the value for this cookie.
   *
   * @return  The value for this cookie.
   */
  public String getValue()
  {
    return value;
  }



  /**
   * Retrieves the domain associated with this cookie.
   *
   * @return  The domain associated with this cookie.
   */
  public String getDomain()
  {
    return domain;
  }



  /**
   * Retrieves the path associated with this cookie.
   *
   * @return  The path associated with this cookie.
   */
  public String getPath()
  {
    return path;
  }



  /**
   * Retrieves the expiration date for this cookie.
   *
   * @return  The expiration date for this cookie.
   */
  public long getExpirationDate()
  {
    return expirationDate;
  }



  /**
   * Indicates whether this cookie should be only provided over secure
   * connections.
   *
   * @return  <CODE>true</CODE> if this cookie should only be provided over
   *          secure connections, or <CODE>false</CODE> if not.
   */
  public boolean getSecure()
  {
    return secure;
  }



  /**
   * Retrieves the comment for this cookie.
   *
   * @return  The comment for this cookie, or <CODE>null</CODE> if there is
   *          none.
   */
  public String getComment()
  {
    return comment;
  }



  /**
   * Retrieves the version for this cookie.
   *
   * @return  The version for this cookie, or -1 if there is none.
   */
  public int getVersion()
  {
    return version;
  }



  /**
   * Retrieves the set of all "extra" unrecognized properties that have been
   * set for this cookie, mapped from the property name (in all lowercase
   * characters) to the property value.
   *
   * @return  The set of "extra" unrecognized properties for this cookie.
   */
  public HashMap getProperties()
  {
    return extraProperties;
  }



  /**
   * Retrieves the value of the requested "extra" unrecognized property from
   * this cookie.
   *
   * @param  lowerName  The name of the property to retrieve, in all lowercase
   *                    characters.
   *
   * @return  The value for the requested "extra" property, or <CODE>null</CODE>
   *          if no such property has been set.
   */
  public String getProperty(String lowerName)
  {
    return extraProperties.get(lowerName);
  }



  /**
   * Indicates whether this cookie applies to the provided request.
   *
   * @param  requestURL   The request for which to make the determination.
   * @param  currentTime  The time to use when determining whether the cookie
   *                      is expired.
   *
   * @return  <CODE>true</CODE> if this cookie should be included in the given
   *          request, or <CODE>false</CODE> if not.
   */
  public boolean appliesToRequest(URL requestURL, long currentTime)
  {
    // See if the cookie has expired.
    if ((expirationDate > 0) && (expirationDate < currentTime))
    {
      return false;
    }


    // If this cookie requires a secure connection, verify that the request uses
    // such a connection.
    if (secure)
    {
      if (! requestURL.getProtocol().equalsIgnoreCase("https"))
      {
        return false;
      }
    }


    // See if the domain is applicable to the request.
    String host = requestURL.getHost().toLowerCase();
    if (! host.endsWith(domain))
    {
      return false;
    }


    // See if the path is applicable to the request.
    if (! requestURL.getPath().startsWith(path))
    {
      return false;
    }


    // If we've gotten here, then the cookie should be included in the request.
    return true;
  }
}

