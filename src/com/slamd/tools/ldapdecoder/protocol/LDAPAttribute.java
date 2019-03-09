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



import com.unboundid.util.Base64;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.asn1.ASN1Set;



/**
 * This class defines an LDAP attribute, which has a type and zero or more
 * values.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPAttribute
{
  // The set of values associated with this attribute.
  private ASN1OctetString[] values;

  // The type for this attribute.
  private String type;



  /**
   * Creates a new LDAP attribute with the provided type and values.
   *
   * @param  type    The attribute type for this attribute.
   * @param  values  The set of values to include with this attribute.
   */
  public LDAPAttribute(String type, ASN1OctetString[] values)
  {
    this.type   = type;
    this.values = values;
  }



  /**
   * Retrieves the attribute type for this attribute.
   *
   * @return  The attribute type for this attribute.
   */
  public String getType()
  {
    return type;
  }



  /**
   * Retrieves the set of values for this attribute.
   *
   * @return  The set of values for this attribute.
   */
  public ASN1OctetString[] getValues()
  {
    return values;
  }



  /**
   * Encodes this LDAP attribute to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded attribute.
   */
  public ASN1Element encode()
  {
    ASN1Set valueSet;
    if ((values == null) || (values.length == 0))
    {
      valueSet = new ASN1Set();
    }
    else
    {
      valueSet = new ASN1Set(values);
    }


    ASN1Element[] attrElements = new ASN1Element[]
    {
      new ASN1OctetString(type),
      valueSet
    };


    return new ASN1Sequence(attrElements);
  }



  /**
   * Decodes the provided ASN.1 element as an LDAP attribute.
   *
   * @param  element  The ASN.1 element to decode as an LDAP attribute.
   *
   * @return  The decoded attribute.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as an LDAP attribute.
   */
  public static LDAPAttribute decode(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] attrElements;
    try
    {
      attrElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode attribute sequence", e);
    }


    if (attrElements.length != 2)
    {
      throw new ProtocolException("There must be exactly two elements in an " +
                                  "attribute sequence");
    }


    String type;
    try
    {
      type = attrElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode attribute type", e);
    }


    ASN1OctetString[] values;
    try
    {
      ASN1Element[] valueElements = attrElements[1].decodeAsSet().getElements();
      values = new ASN1OctetString[valueElements.length];
      for (int i=0; i < values.length; i++)
      {
        values[i] = valueElements[i].decodeAsOctetString();
      }
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode attribute value set", e);
    }


    return new LDAPAttribute(type, values);
  }



  /**
   * Indicates whether the provided attribute value should be base64-encoded
   * when represented in LDIF form as per RFC 2849.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the value should be base64-encoded in LDIF
   *          form, or <CODE>false</CODE> if that is not necessary.
   */
  public static boolean valueNeedsBase64Encoding(ASN1OctetString value)
  {
    byte[] valueBytes = value.getValue();

    if (valueBytes.length == 0)
    {
      return false;
    }


    // Compare the first byte against the SAFE-INIT-CHAR list.
    byte firstByte = valueBytes[0];
    if (((firstByte & 0x7F) != firstByte) ||  // Anything > 127
        (firstByte == 0x00) ||                // NUL
        (firstByte == 0x0A) ||                // LF
        (firstByte == 0x0D) ||                // CR
        (firstByte == 0x20) ||                // SPACE
        (firstByte == 0x3A) ||                // colon
        (firstByte == 0x3C))                  // less-than
    {
      return true;
    }


    // Compare the remaining bytes against the SAFE-CHAR list.
    for (int i=1; i < valueBytes.length; i++)
    {
      if (((valueBytes[i] & 0x7F) != valueBytes[i]) ||  // Anything > 127
          (valueBytes[i] == 0x00) ||                    // NUL
          (valueBytes[i] == 0x0A) ||                    // LF
          (valueBytes[i] == 0x0D))                      // CR
      {
        return true;
      }
    }


    // We haven't found a reason to encode it, so return false.
    return false;
  }



  /**
   * Retrieves a string representation of this LDAP attribute.
   *
   * @return  A string representation of this LDAP attribute.
   */
  public String toString()
  {
    return toString(0);
  }



  /**
   * Retrieves a string representation of this LDAP attribute with the specified
   * indent.
   *
   * @param  indent  The number of spaces to the left of the value.
   *
   * @return  A string representation of this LDAP attribute.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    if ((values == null) || (values.length == 0))
    {
      buffer.append(indentBuf).append(type).append(": ").
             append(LDAPMessage.EOL);
    }
    else
    {
      for (int i=0; i < values.length; i++)
      {
        StringBuilder attrBuf = new StringBuilder();

        attrBuf.append(type);

        if (valueNeedsBase64Encoding(values[i]))
        {
          attrBuf.append(":: ").
                  append(Base64.encode(values[i].getValue()));
        }
        else
        {
          attrBuf.append(": ").append(values[i].getStringValue());
        }

        if (attrBuf.length() > 75)
        {
          buffer.append(indentBuf).append(attrBuf.substring(0, 75)).
                 append(LDAPMessage.EOL);

          int startPos = 75;
          int endPos   = Math.min(startPos+74, attrBuf.length());
          while (startPos < attrBuf.length())
          {
            buffer.append(indentBuf).append(' ').
                   append(attrBuf.substring(startPos, endPos)).
                   append(LDAPMessage.EOL);
            startPos += 74;
            endPos = Math.min(startPos+74, attrBuf.length());
          }
        }
        else
        {
          buffer.append(indentBuf).append(attrBuf).append(LDAPMessage.EOL);
        }
      }
    }

    return buffer.toString();
  }
}

