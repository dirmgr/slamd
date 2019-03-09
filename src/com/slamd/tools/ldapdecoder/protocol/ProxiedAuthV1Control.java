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



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the first version of the proxied authorization control,
 * which is used to perform an operation under the authority of one user while
 * authenticated as another.  Although it was replaced by a second version, the
 * original version is still in use in some cases.
 *
 *
 * @author   Neil A. Wilson
 */
public class ProxiedAuthV1Control
       extends LDAPControl
{
  /**
   * The OID of the proxied auth v1 control.
   */
  public static final String PROXIED_AUTH_V1_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.12";



  // The DN of the user whose authority the requested operation should be
  // performed.
  private String proxyDN;



  /**
   * Creates a new proxied auth v1 control with the provided information.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   * @param  proxyDN     The DN of the user under whose authority the requested
   *                     operation should be performed.
   */
  public ProxiedAuthV1Control(boolean isCritical, String proxyDN)
  {
    super(PROXIED_AUTH_V1_CONTROL_OID, isCritical, encodeValue(proxyDN));

    this.proxyDN = proxyDN;
  }



  /**
   * Creates a new proxied auth v1 control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public ProxiedAuthV1Control(boolean isCritical, ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(PROXIED_AUTH_V1_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] elements;
    try
    {
      byte[] valueBytes = controlValue.getValue();
      elements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode proxied auth v1 value " +
                                  "sequence", e);
    }


    if (elements.length != 1)
    {
      throw new ProtocolException("There must be exactly one element in a " +
                                  "proxied auth v1 value sequence");
    }


    try
    {
      proxyDN = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode the proxy DN from the " +
                                  "proxied authorization control", e);
    }
  }



  /**
   * Retrieves the proxy DN for this proxied authorization control.
   *
   * @return  The proxy DN for this proxied authorization control.
   */
  public String getProxyDN()
  {
    return proxyDN;
  }



  /**
   * Encodes the provided proxy DN into an octet string suitable for use as the
   * value of this control.
   *
   * @param  proxyDN  The DN of the user under whose authority the requested
   *                  operation should be performed.
   *
   * @return  The octet string containing the encoded proxy DN.
   */
  public static ASN1OctetString encodeValue(String proxyDN)
  {
    ASN1Element[] elements = new ASN1Element[]
    {
      new ASN1OctetString(proxyDN)
    };


    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Retrieves a string representation of this control with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this control with the specified indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("LDAP Proxied Authorization (v1) Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Proxy DN:  ").append(proxyDN).
           append(LDAPMessage.EOL);

    return buffer.toString();
  }
}

