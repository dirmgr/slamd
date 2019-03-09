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
package com.slamd.protocol;



import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the resource
 * monitor client to the server when the connection is first established.  It
 * provides the server with all the necessary properties and capabilities for
 * this client.
 *
 *
 * @author   Neil A. Wilson
 */
public class MonitorClientHello
       extends SLAMDMessage
{
  // Indicates whether this monitor client requires the server to authenticate
  // itself.
  private boolean requireServerAuth;

  // The authentication credentials for this monitor client.
  private byte[] authCredentials;

  // The port number for this monitor client.
  private int clientPort;

  // The major version number for this monitor client.
  private int majorVersion;

  // The minor version number for this monitor client.
  private int minorVersion;

  // The point version number for this monitor client.
  private int pointVersion;

  // The authentication ID for this monitor client.
  private String authID;

  // The authentication method used by this monitor client.
  private String authMethod;

  // The client ID for this monitor client.
  private String clientID;

  // The IP address for this monitor client.
  private String clientIP;

  // The set of resource monitor classes that will be used by the monitor
  // client.
  private String[] monitorClasses;



  /**
   * Creates a new instance of this monitor client hello message which is
   * intended for use in decoding a message transmitted between the client and
   * the server.  It is not intended for general use.
   */
  public MonitorClientHello()
  {
    super();

    clientIP          = null;
    clientPort        = -1;
    clientID          = null;
    majorVersion      = -1;
    minorVersion      = -1;
    pointVersion      = -1;
    monitorClasses    = new String[0];
    authMethod        = null;
    authID            = null;
    authCredentials   = null;
    requireServerAuth = false;
  }



  /**
   * Creates a new instance of this monitor client hello message with the
   * provided information.
   *
   * @param  messageID       The message ID for this SLAMD message.
   * @param  extraProperties The "extra" properties for this SLAMD message.
   *                         Both the names and values for the properties must
   *                         be strings.
   * @param  clientIP        The IP address of the monitor client system.
   * @param  clientPort      The port number of the monitor client system.
   * @param  clientID        The client ID for the monitor client system.
   * @param  majorVersion    The major version number for this monitor client.
   * @param  minorVersion    The minor version number for this monitor client.
   * @param  pointVersion    The point version number for this monitor client.
   * @param  monitorClasses  The set of resource monitor classes that will be
   *                         used by the monitor client.
   */
  public MonitorClientHello(int messageID,
                            HashMap<String,String> extraProperties,
                            String clientIP, int clientPort, String clientID,
                            int majorVersion, int minorVersion,
                            int pointVersion, String[] monitorClasses)
  {
    super(messageID, extraProperties);

    this.clientIP       = clientIP;
    this.clientPort     = clientPort;
    this.clientID       = clientID;
    this.majorVersion   = majorVersion;
    this.minorVersion   = minorVersion;
    this.pointVersion   = pointVersion;
    this.monitorClasses = monitorClasses;

    authMethod        = null;
    authID            = null;
    authCredentials   = null;
    requireServerAuth = false;
  }



  /**
   * Creates a new instance of this monitor client hello message with the
   * provided information.
   *
   * @param  messageID          The message ID for this SLAMD message.
   * @param  extraProperties    The "extra" properties for this SLAMD message.
   *                            Both the names and values for the properties
   *                            must be strings.
   * @param  clientIP           The IP address of the monitor client system.
   * @param  clientPort         The port number of the monitor client system.
   * @param  clientID           The client ID for the monitor client system.
   * @param  majorVersion       The major version number for this monitor
   *                            client.
   * @param  minorVersion       The minor version number for this monitor
   *                            client.
   * @param  pointVersion       The point version number for this monitor
   *                            client.
   * @param  monitorClasses     The set of resource monitor classes that will be
   *                            used by the monitor client.
   * @param  authMethod         The authentication method used by this monitor
   *                            client.
   * @param  authID             The authentication ID for this monitor client.
   * @param  authCredentials    The authentication credentials for this monitor
   *                            client.
   * @param  requireServerAuth  Indicates whether this client requires the
   *                            server to provide its own authentication info.
   */
  public MonitorClientHello(int messageID,
                            HashMap<String,String> extraProperties,
                            String clientIP, int clientPort, String clientID,
                            int majorVersion, int minorVersion,
                            int pointVersion, String[] monitorClasses,
                            String authMethod, String authID,
                            byte[] authCredentials, boolean requireServerAuth)
  {
    super(messageID, extraProperties);

    this.clientIP          = clientIP;
    this.clientPort        = clientPort;
    this.clientID          = clientID;
    this.majorVersion      = majorVersion;
    this.minorVersion      = minorVersion;
    this.pointVersion      = pointVersion;
    this.monitorClasses    = monitorClasses;
    this.authMethod        = authMethod;
    this.authID            = authID;
    this.authCredentials   = authCredentials;
    this.requireServerAuth = requireServerAuth;
  }



  /**
   * Retrieves the client IP address for this monitor client connection.
   *
   * @return  The client IP address for this monitor client connection.
   */
  public String getClientIP()
  {
    return clientIP;
  }



  /**
   * Specifies the client IP address for this monitor client connection.
   *
   * @param  clientIP  The client IP address for this monitor client connection.
   */
  public void setClientIP(String clientIP)
  {
    this.clientIP = clientIP;
  }



  /**
   * Retrieves the client port number for this monitor client connection.
   *
   * @return  The client port number for this monitor client connection.
   */
  public int getClientPort()
  {
    return clientPort;
  }



  /**
   * Specifies the client port number for this monitor client connection.
   *
   * @param  clientPort  The client port number for this monitor client
   *                     connection.
   */
  public void setClientPort(int clientPort)
  {
    this.clientPort = clientPort;
  }



  /**
   * Retrieves the client ID for this monitor client connection.
   *
   * @return  The client ID for this monitor client connection.
   */
  public String getClientID()
  {
    return clientID;
  }



  /**
   * Specifies the client ID for this monitor client connection.
   *
   * @param  clientID  The client ID for this monitor client connection.
   */
  public void setClientID(String clientID)
  {
    this.clientID = clientID;
  }



  /**
   * Retrieves the major version number for the monitor client software.
   *
   * @return  The major version number for the monitor client software.
   */
  public int getMajorVersion()
  {
    return majorVersion;
  }



  /**
   * Specifies the major version number for the monitor client software.
   *
   * @param  majorVersion  The major version number for the monitor client
   *                       software.
   */
  public void setMajorVersion(int majorVersion)
  {
    this.majorVersion = majorVersion;
  }



  /**
   * Retrieves the minor version number for the monitor client software.
   *
   * @return  The minor version number for the monitor client software.
   */
  public int getMinorVersion()
  {
    return minorVersion;
  }



  /**
   * Specifies the minor version number for the monitor client software.
   *
   * @param  minorVersion  The minor version number for the monitor client
   *                       software.
   */
  public void setMinorVersion(int minorVersion)
  {
    this.minorVersion = minorVersion;
  }



  /**
   * Retrieves the point version number for the monitor client software.
   *
   * @return  The point version number for the monitor client software.
   */
  public int getPointVersion()
  {
    return pointVersion;
  }



  /**
   * Specifies the point version number for the monitor client software.
   *
   * @param  pointVersion  The point version number for the monitor client
   *                       software.
   */
  public void setPointVersion(int pointVersion)
  {
    this.pointVersion = pointVersion;
  }



  /**
   * Retrieves the set of resource monitor classes that will be used by this
   * monitor client.
   *
   * @return  The set of resource monitor classes that will be used by this
   *          monitor client.
   */
  public String[] getMonitorClasses()
  {
    return monitorClasses;
  }



  /**
   * Specifies the set of resource monitor classes that will be used by this
   * monitor client.
   *
   * @param  monitorClasses  The set of resource monitor classes that will be
   *                         used by this monitor client.
   */
  public void setMonitorClasses(String[] monitorClasses)
  {
    this.monitorClasses = monitorClasses;
  }



  /**
   * Retrieves the name of the method that the client wishes to use to
   * authenticate to the server.
   *
   * @return  The name of the method that the client wishes to use to
   *          authenticate to the server, or <CODE>null</CODE> if no
   *          authentication is to be performed.
   */
  public String getAuthMethod()
  {
    return authMethod;
  }



  /**
   * Specifies the name of the method that the client wishes to use to
   * authenticate to the server.
   *
   * @param  authMethod  The name of the method that the client wishes to use to
   *                     authenticate to the server.
   */
  public void setAuthMethod(String authMethod)
  {
    this.authMethod = authMethod;
  }



  /**
   * Retrieves the authentication ID that the client wishes to use to
   * authenticate to the server.
   *
   * @return  The authentication ID that the client wishes to use to
   *          authenticate to the server, or <CODE>null</CODE> if no
   *          authentication is to be performed or no auth ID is required for
   *          the selected authentication method.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Specifies the authentication ID that the client wishes to use to
   * authenticate to the server.
   *
   * @param  authID  The authentication ID that the client wishes to use to
   *                 authenticate to the server.
   */
  public void setAuthID(String authID)
  {
    this.authID = authID;
  }



  /**
   * Retrieves the credentials that the client wishes to use to authenticate to
   * the server.
   *
   * @return  The credentials that the client wishes to use to authenticate to
   *          the server, or <CODE>null</CODE> if no authentication is to be
   *          performed or no credentials are required for the selected
   *          authentication method.
   */
  public byte[] getAuthCredentials()
  {
    return authCredentials;
  }



  /**
   * Specifies the credentials that the client wishes to use to authenticate to
   * the server.
   *
   * @param  authCredentials  The credentials that the client wishes to use to
   *                          authenticate to the server.
   */
  public void setAuthCredentials(byte[] authCredentials)
  {
    this.authCredentials = authCredentials;
  }



  /**
   * Indicates whether the client requires the server to authenticate itself to
   * the client.
   *
   * @return  <CODE>true</CODE> if the client requires the server to
   *          authenticate itself to the client, or <CODE>false</CODE> if not.
   */
  public boolean getRequireServerAuth()
  {
    return requireServerAuth;
  }



  /**
   * Specifies whether the client requires the server to authenticate itself to
   * the client.
   *
   * @param  requireServerAuth  Specifies whether the client requires the server
   *                            to authenticate itself to the client.
   */
  public void setRequireServerAuth(boolean requireServerAuth)
  {
    this.requireServerAuth = requireServerAuth;
  }



  /**
   * Encodes the payload component of this SLAMD message to an ASN.1 element for
   * inclusion in the message envelope.
   *
   * @return  The ASN.1 element containing the encoded message payload.
   */
  @Override()
  public ASN1Element encodeMessagePayload()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_IP,
                                        new ASN1OctetString(clientIP)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_PORT,
                                        new ASN1Integer(clientPort)));
    elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_CLIENT_ID,
                                        new ASN1OctetString(clientID)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_MAJOR_VERSION,
                         new ASN1Integer(majorVersion)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_MINOR_VERSION,
                         new ASN1Integer(minorVersion)));
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_POINT_VERSION,
                         new ASN1Integer(pointVersion)));

    ASN1Element[] monitorClassElements = new ASN1Element[monitorClasses.length];
    for (int i=0; i < monitorClasses.length; i++)
    {
      monitorClassElements[i] = new ASN1OctetString(monitorClasses[i]);
    }
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_MONITOR_CLASSES,
                         new ASN1Sequence(monitorClassElements)));

    if (authMethod != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_AUTH_METHOD,
                           new ASN1OctetString(authMethod)));
    }

    if (authID != null)
    {
      elementList.add(encodeNameValuePair(ProtocolConstants.PROPERTY_AUTH_ID,
                                          new ASN1OctetString(authID)));
    }

    if (authCredentials != null)
    {
      elementList.add(encodeNameValuePair(
                           ProtocolConstants.PROPERTY_AUTH_CREDENTIALS,
                           new ASN1OctetString(authCredentials)));
    }

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_REQUIRE_SERVER_AUTH,
                         new ASN1Boolean(requireServerAuth)));


    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the provided ASN.1 element and uses it as the payload for this
   * SLAMD message.
   *
   * @param  payloadElement  The ASN.1 element to decode as the payload for this
   *                         SLAMD message.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided ASN.1 element as the payload for this
   *                          SLAMD message.
   */
  @Override()
  public void decodeMessagePayload(ASN1Element payloadElement)
         throws SLAMDException
  {
    HashMap<String,ASN1Element> propertyMap =
         decodeNameValuePairSequence(payloadElement);

    ASN1Element valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_IP);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "client IP address.");
    }
    else
    {
      try
      {
        clientIP = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client IP address:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_PORT);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "client port number.");
    }
    else
    {
      try
      {
        clientPort = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client port number:  " +
                                 e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLIENT_ID);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "client ID");
    }
    else
    {
      try
      {
        clientID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the client ID:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_MAJOR_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "major version number.");
    }
    else
    {
      try
      {
        majorVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the major version " +
                                 "number:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_MINOR_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "minor version number.");
    }
    else
    {
      try
      {
        minorVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the minor version " +
                                 "number:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_POINT_VERSION);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "point version number.");
    }
    else
    {
      try
      {
        pointVersion = valueElement.decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the point version " +
                                 "number:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_MONITOR_CLASSES);
    if (valueElement == null)
    {
      throw new SLAMDException("Monitor client hello message is missing the " +
                               "set of monitor classes.");
    }
    else
    {
      try
      {
        ASN1Element[] classElements =
             valueElement.decodeAsSequence().getElements();
        monitorClasses = new String[classElements.length];
        for (int i=0; i < classElements.length; i++)
        {
          monitorClasses[i] =
               classElements[i].decodeAsOctetString().getStringValue();
        }
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the set of monitor " +
                                 "classes:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_AUTH_METHOD);
    if (valueElement != null)
    {
      try
      {
        authMethod = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the auth method:  " + e, e);
      }
    }


    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_AUTH_ID);
    if (valueElement != null)
    {
      try
      {
        authID = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the auth ID:  " + e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_AUTH_CREDENTIALS);
    if (valueElement != null)
    {
      try
      {
        authCredentials = valueElement.decodeAsOctetString().getValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the auth credentials:  " +
                                 e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_REQUIRE_SERVER_AUTH);
    if (valueElement != null)
    {
      try
      {
        requireServerAuth = valueElement.decodeAsBoolean().getBooleanValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the requireServerAuth " +
                                 "flag:  " + e, e);
      }
    }
  }



  /**
   * Appends a string representation of the payload for this SLAMD message to
   * the provided buffer.  The string representation may contain multiple lines,
   * but the last line should not end with an end-of-line marker.
   *
   * @param  buffer  The buffer to which the string representation is to be
   *                 appended.
   * @param  indent  The number of spaces to indent the payload content.
   */
  @Override()
  public void payloadToString(StringBuilder buffer, int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    buffer.append(indentBuf);
    buffer.append("clientIP = ");
    buffer.append(clientIP);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("clientPort = ");
    buffer.append(clientPort);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("clientID = ");
    buffer.append(clientID);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("majorVersion = ");
    buffer.append(majorVersion);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("minorVersion = ");
    buffer.append(minorVersion);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("pointVersion = ");
    buffer.append(pointVersion);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("monitorClasses = ");
    buffer.append(Constants.EOL);
    for (int i=0; i < monitorClasses.length; i++)
    {
      buffer.append(indentBuf);
      buffer.append("     ");
      buffer.append(monitorClasses[i]);
      buffer.append(Constants.EOL);
    }

    if (authMethod != null)
    {
      buffer.append(indentBuf);
      buffer.append("authMethod = ");
      buffer.append(authMethod);
      buffer.append(Constants.EOL);
    }

    if (authID != null)
    {
      buffer.append(indentBuf);
      buffer.append("authID = ");
      buffer.append(authID);
      buffer.append(Constants.EOL);
    }

    if (authCredentials != null)
    {
      buffer.append(indentBuf);
      buffer.append("authCredentials = byte[");
      buffer.append(authCredentials.length);
      buffer.append(']');
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("requireServerAuth = ");
    buffer.append(requireServerAuth);
  }
}

