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
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the server-side sort response control, which is included
 * in the search result done message that the server sends to the client after
 * performing a search that included sorting.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerSortResponseControl
       extends LDAPControl
{
  /**
   * The OID of the server sort response control.
   */
  public static final String SERVER_SORT_RESPONSE_CONTROL_OID =
                                  "1.2.840.113556.1.4.474";



  /**
   * The ASN.1 type that will be used if an attribute type is included in the
   * sort response control.
   */
  public static final byte TYPE_ATTRIBUTE_TYPE = (byte) 0x80;



  // The result code for the sort operation.
  private int resultCode;

  // The attribute type associated with the sort key that caused the first
  // error in the sorting process.
  private String attributeType;



  /**
   * Creates a new server sort response control with the provided information.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   * @param  resultCode  The result code for the sort operation.
   */
  public ServerSortResponseControl(boolean isCritical, int resultCode)
  {
    super(SERVER_SORT_RESPONSE_CONTROL_OID, isCritical,
          encodeValue(resultCode, null));

    this.resultCode    = resultCode;
    this.attributeType = null;
  }



  /**
   * Creates a new server sort response control with the provided information.
   *
   * @param  isCritical     Indicates whether this control should be marked
   *                        critical.
   * @param  resultCode     The result code for the sort operation.
   * @param  attributeType  The attribute associated with the sort key that
   *                        caused the first error in the sorting process.
   */
  public ServerSortResponseControl(boolean isCritical, int resultCode,
                                   String attributeType)
  {
    super(SERVER_SORT_RESPONSE_CONTROL_OID, isCritical,
          encodeValue(resultCode, attributeType));

    this.resultCode    = resultCode;
    this.attributeType = attributeType;
  }



  /**
   * Creates a new server sort response control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public ServerSortResponseControl(boolean isCritical,
                                   ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(SERVER_SORT_RESPONSE_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] elements;
    try
    {
      byte[] valueBytes = controlValue.getValue();
      elements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode the server sort response " +
                                  "control sequence", e);
    }


    if ((elements.length < 1) || (elements.length > 2))
    {
      throw new ProtocolException("There must be either 1 or 2 elements in " +
                                  "a server sort response control sequence");
    }


    try
    {
      resultCode = elements[0].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode result code for server " +
                                  "sort response control");
    }


    if (elements.length == 2)
    {
      try
      {
        attributeType = elements[1].decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode attribute type for " +
                                    "server sort response control");
      }
    }
    else
    {
      attributeType = null;
    }
  }



  /**
   * Retrieves the result code for the server-side sort operation.
   *
   * @return  The result code for the server-side sort operation.
   */
  public int getResultCode()
  {
    return resultCode;
  }



  /**
   * Retrieves the attribute type for the server-side sort operation.
   *
   * @return  The attribute type for the server-side sort operation.
   */
  public String getAttributeType()
  {
    return attributeType;
  }



  /**
   * Encodes the provided information into an octet string suitable for use as
   * the value of this control.
   *
   * @param  resultCode     The result code for this server-side sort response.
   * @param  attributeType  The attribute type for this server-side sort
   *                        response.
   *
   * @return  The octet string containing the encoded control value.
   */
  public static ASN1OctetString encodeValue(int resultCode,
                                            String attributeType)
  {
    ASN1Element[] elements;
    if (attributeType == null)
    {
      elements = new ASN1Element[]
      {
        new ASN1Enumerated(resultCode)
      };
    }
    else
    {
      elements = new ASN1Element[]
      {
        new ASN1Enumerated(resultCode),
        new ASN1OctetString(TYPE_ATTRIBUTE_TYPE, attributeType)
      };
    }


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
    buffer.append(indentBuf).append("LDAP Server-Side Sort Response Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Sort Result Code:  ").
           append(resultCode).append(LDAPMessage.EOL);


    if (attributeType != null)
    {
      buffer.append(indentBuf).append("    Attribute for First Error:  ").
             append(attributeType).append(LDAPMessage.EOL);
    }


    return buffer.toString();
  }
}

