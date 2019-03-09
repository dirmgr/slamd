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
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the simple paged results control, as defined in RFC 2696.
 * This control is used to retrieve the results of a search operation a "page"
 * at a time.
 *
 *
 * @author   Neil A. Wilson
 */
public class PagedResultsControl
       extends LDAPControl
{
  /**
   * The OID of the paged results control.
   */
  public static final String PAGED_RESULTS_CONTROL_OID =
                                  "1.2.840.113556.1.4.319";



  // The opaque cookie value for this control.
  private ASN1OctetString cookie;

  // The page size or estimated result set size for this control.
  private int size;



  /**
   * Creates a new simple paged results control.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   * @param  size        The size for this control.
   * @param  cookie      The opaque cookie for this control.
   */
  public PagedResultsControl(boolean isCritical, int size,
                             ASN1OctetString cookie)
  {
    super(PAGED_RESULTS_CONTROL_OID, isCritical, encodeValue(size, cookie));

    this.size   = size;
    this.cookie = cookie;
  }



  /**
   * Creates a new simple paged results control.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for the paged results control.
   *
   * @throws  ProtocolException  If the provided control value cannot be
   *                             decoded appropriately for a paged results
   *                             control.
   */
  public PagedResultsControl(boolean isCritical, ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(PAGED_RESULTS_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] sequenceElements;
    try
    {
      byte[] valueBytes = controlValue.getValue();
      sequenceElements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode paged results control " +
                                  "sequence", e);
    }


    if (sequenceElements.length != 2)
    {
      throw new ProtocolException("There must be exactly 2 elements in a " +
                                  "paged results control sequence");
    }


    try
    {
      size = sequenceElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode size from paged results " +
                                  "control sequence", e);
    }


    try
    {
      cookie = sequenceElements[1].decodeAsOctetString();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode cookie from paged " +
                                  "results control sequence", e);
    }
  }



  /**
   * Encodes the provided information into a paged results control value.
   *
   * @param  size    The size for this paged results control.
   * @param  cookie  The ASN.1 octet string forming the opaque cookie for this
   *                 paged results control.
   *
   * @return  The encoded VLV request control value.
   */
  public static ASN1OctetString encodeValue(int size, ASN1OctetString cookie)
  {
    ASN1Element[] elements = new ASN1Element[]
    {
      new ASN1Integer(size),
      cookie
    };

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Retrieves the size for this paged results control, which is either the
   * requested page size or the estimated result set size.
   *
   * @return  The size for this paged results control.
   */
  public int getSize()
  {
    return size;
  }



  /**
   * Retrieves the opaque cookie for this paged results control.
   *
   * @return  The opaque cookie for this paged results control.
   */
  public ASN1OctetString getCookie()
  {
    return cookie;
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
    buffer.append(indentBuf).append("LDAP Simple Paged Results Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Size:  ").
           append(size).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Cookie:  ").append(LDAPMessage.EOL);

    if (cookie.getValue().length > 0)
    {
      buffer.append(LDAPMessage.byteArrayToString(cookie.getValue(),
                                                  (indent+8)));
    }

    return buffer.toString();
  }
}

