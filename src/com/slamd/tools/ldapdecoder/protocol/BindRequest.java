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
package com.slamd.tools.ldapdecoder.protocol;



import java.io.PrintStream;
import java.util.Date;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP bind request, which is used to authenticate to a
 * directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class BindRequest
       extends ProtocolOp
{
  /**
   * The authentication type that indicates that simple authentication is to be
   * performed.
   */
  public static final byte AUTH_TYPE_SIMPLE = (byte) 0x80;



  /**
   * The authentication type that indicates that SASL authentication is to be
   * performed.
   */
  public static final byte AUTH_TYPE_SASL = (byte) 0xA3;



  // The SASL credentials for this bind request.
  private ASN1OctetString saslCredentials;

  // The authentication type contained in this bind request.
  private byte authType;

  // The LDAP protocol version contained in this bind request.
  private int protocolVersion;

  // The DN of the user performing the bind.
  private String bindDN;

  // The password used for simple authentication.
  private String bindPassword;

  // The SASL mechanism used for this bind request.
  private String saslMechanism;



  /**
   * Creates a new bind request using simple authentication with the provided
   * information.
   *
   * @param  protocolVersion  The LDAP protocol version to use in the bind
   *                          request.
   * @param  bindDN           The DN of the user performing the bind.
   * @param  bindPassword     The password of the user performing the bind.
   */
  public BindRequest(int protocolVersion, String bindDN, String bindPassword)
  {
    this.protocolVersion = protocolVersion;
    this.bindDN          = bindDN;
    this.bindPassword    = bindPassword;

    authType = AUTH_TYPE_SIMPLE;
  }



  /**
   * Creates a new bind request using SASL authentication with the provided
   * information.
   *
   * @param  protocolVersion  The LDAP protocol version to use in the bind
   *                          request.
   * @param  bindDN           The DN of the user performing the bind.
   * @param  saslMechanism    The SASL mechanism used to perform the bind.
   * @param  saslCredentials  The SASL credentials to use in the bind.
   */
  public BindRequest(int protocolVersion, String bindDN, String saslMechanism,
                     ASN1OctetString saslCredentials)
  {
    this.protocolVersion = protocolVersion;
    this.bindDN          = bindDN;
    this.saslMechanism   = saslMechanism;
    this.saslCredentials = saslCredentials;

    authType = AUTH_TYPE_SASL;
  }



  /**
   * Retrieves the LDAP protocol version used in this bind request.
   *
   * @return  The LDAP protocol version used in this bind request.
   */
  public int getProtocolVersion()
  {
    return protocolVersion;
  }



  /**
   * Retrieves the DN of the user performing the bind.
   *
   * @return  The DN of the user performing the bind.
   */
  public String getBindDN()
  {
    return bindDN;
  }



  /**
   * Retrieves the type of authentication contained in this bind request.
   *
   * @return  <CODE>AUTH_TYPE_SIMPLE</CODE> if simple authentication is to be
   *          performed, or <CODE>AUTH_TYPE_SASL</CODE> if SASL authentication
   *          should be used.
   */
  public byte getAuthType()
  {
    return authType;
  }



  /**
   * Retrieves the password used for simple authentication.
   *
   * @return  The password used for simple authentication.
   */
  public String getBindPassword()
  {
    return bindPassword;
  }



  /**
   * Retrieves the mechanism used for SASL authentication.
   *
   * @return  The mechanism used for SASL authentication.
   */
  public String getSASLMechanism()
  {
    return saslMechanism;
  }



  /**
   * Retrieves the credentials used for SASL authentication.
   *
   * @return  The credentials used for SASL authentication.
   */
  public ASN1OctetString getSASLCredentials()
  {
    return saslCredentials;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element authenticationElement;

    if (authType == AUTH_TYPE_SIMPLE)
    {
      authenticationElement = new ASN1OctetString(AUTH_TYPE_SIMPLE,
                                                  bindPassword);
    }
    else
    {
      ASN1Element[] saslElements;
      if (saslCredentials == null)
      {
        saslElements = new ASN1Element[]
        {
          new ASN1OctetString(saslMechanism)
        };
      }
      else
      {
        saslElements = new ASN1Element[]
        {
          new ASN1OctetString(saslMechanism),
          saslCredentials
        };
      }

      authenticationElement = new ASN1Sequence(AUTH_TYPE_SASL, saslElements);
    }


    ASN1Element[] bindElements = new ASN1Element[]
    {
      new ASN1Integer(protocolVersion),
      new ASN1OctetString(bindDN),
      authenticationElement
    };


    return new ASN1Sequence(BIND_REQUEST_TYPE, bindElements);
  }



  /**
   * Decodes the provided ASN.1 element as a bind request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded bind request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a bind request.
   */
  public static BindRequest decodeBindRequest(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] bindElements;
    try
    {
      bindElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode bind request protocol op " +
                                  "as an ASN.1 sequence", e);
    }


    if (bindElements.length != 3)
    {
      throw new ProtocolException("There must be three elements in a bind " +
                                  "request sequence");
    }


    int protocolVersion;
    try
    {
      protocolVersion = bindElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode protocol version", e);
    }


    String bindDN;
    try
    {
      bindDN = bindElements[1].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode bind DN", e);
    }


    switch (bindElements[2].getType())
    {
      case AUTH_TYPE_SIMPLE:
        try
        {
          String bindPassword =
                      bindElements[2].decodeAsOctetString().getStringValue();
          return new BindRequest(protocolVersion, bindDN, bindPassword);
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode bind password", e);
        }
      case AUTH_TYPE_SASL:
        ASN1Element[] saslElements;
        try
        {
          saslElements = bindElements[2].decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode SASL authentication " +
                                      "sequence", e);
        }

        if ((saslElements.length < 1) || (saslElements.length > 2))
        {
          throw new ProtocolException("A SASL authentication sequence must " +
                                      "have one or two elements");
        }

        String saslMechanism;
        try
        {
          saslMechanism =
               saslElements[0].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode the SASL mechanism", e);
        }

        ASN1OctetString saslCredentials = null;
        if (saslElements.length == 2)
        {
          try
          {
            saslCredentials = saslElements[1].decodeAsOctetString();
          }
          catch (Exception e)
          {
            throw new ProtocolException("Unable to decode the SASL credentials",
                                        e);
          }
        }

        // FIXME -- Add support for specific SASL mechanisms.
        return new BindRequest(protocolVersion, bindDN, saslMechanism,
                               saslCredentials);
      default:
        throw new ProtocolException("Unknown authentication type " +
                                    bindElements[2].getType());
    }
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Bind Request";
  }



  /**
   * Retrieves a string representation of this protocol op with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this protocol op with the specified
   *          indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("LDAP Version:  ").
           append(protocolVersion).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("Bind DN:  ").
           append(bindDN).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("Authentication Data:").
           append(LDAPMessage.EOL);

    if (authType == AUTH_TYPE_SIMPLE)
    {
      buffer.append(indentBuf).append("    Authentication Type:  Simple").
             append(LDAPMessage.EOL);
      buffer.append(indentBuf).append("    Bind Password:  ").
             append(bindPassword).append(LDAPMessage.EOL);
    }
    else if (authType == AUTH_TYPE_SASL)
    {
      buffer.append(indentBuf).append("    Authentication Type:  SASL").
             append(LDAPMessage.EOL);
      buffer.append(indentBuf).append("    SASL Mechanism:  ").
             append(saslMechanism).append(LDAPMessage.EOL);

      if (saslCredentials != null)
      {
        byte[] credentialBytes = saslCredentials.getValue();
        buffer.append(indentBuf).append("    SASL Credentials:").
               append(LDAPMessage.EOL);
        buffer.append(LDAPMessage.byteArrayToString(credentialBytes,
                                                    (indent+8)));
      }
    }

    return buffer.toString();
  }



  /**
   * Constructs a string representation of this LDAP message in a form that can
   * be written to a SLAMD script.  It may be empty if this message isn't one
   * that would be generated as part of a client request.
   *
   * @param  scriptWriter  The print stream to which the script contents should
   *                       be written.
   */
  public void toSLAMDScript(PrintStream scriptWriter)
  {
    scriptWriter.println("#### Bind request captured at " + new Date());
    scriptWriter.println("# Bind DN:  " + bindDN);

    if (authType == AUTH_TYPE_SIMPLE)
    {
      scriptWriter.println("# Authentication Type:  Simple");
      scriptWriter.println("# Authentication Password:  " + bindPassword);
      scriptWriter.println("resultCode = conn.bind(\"" + bindDN + "\", \"" +
                           bindPassword + "\");");
    }
    else if (authType == AUTH_TYPE_SASL)
    {
      scriptWriter.println("# Authentication Type:  SASL");
      scriptWriter.println("# SASL Mechanism:  " + saslMechanism);
      scriptWriter.println("# NOTE:  The SLAMD scripting language does not " +
                           "currently support SASL binds.");
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

