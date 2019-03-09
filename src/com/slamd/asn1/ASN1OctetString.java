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
package com.slamd.asn1;



/**
 * This class defines an ASN.1 element that can hold an octet string value.  An
 * octet string is simply a set of bytes, and therefore any value can be encoded
 * as an octet string, although other types may be more appropriate.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1OctetString
       extends ASN1Element
{
  /**
   * Creates a new ASN.1 octet string with no value.
   */
  public ASN1OctetString()
  {
    this(ASN1_OCTET_STRING_TYPE, EMPTY_BYTES);
  }



  /**
   * Creates a new ASN.1 octet string with the specified type and no value.
   *
   * @param  type  The type to use for this octet string element.
   */
  public ASN1OctetString(byte type)
  {
    this(type, EMPTY_BYTES);
  }



  /**
   * Creates a new ASN.1 octet string with the specified value.
   *
   * @param  value  The Java string value to encode into this octet string.
   */
  public ASN1OctetString(String value)
  {
    this(ASN1_OCTET_STRING_TYPE, getBytes(value));
  }



  /**
   * Creates a new ASN.1 octet string with the specified value.
   *
   * @param  value  The byte array to use as the value for this octet string.
   */
  public ASN1OctetString(byte[] value)
  {
    this(ASN1_OCTET_STRING_TYPE, value);
  }



  /**
   * Creates a new ASN.1 octet string with the specified type and value.
   *
   * @param  type   The type to use for this octet string element.
   * @param  value  The Java string value to encode into this octet string.
   */
  public ASN1OctetString(byte type, String value)
  {
    this(type, getBytes(value));
  }



  /**
   * Creates a new ASN.1 octet string with the specified type and value.
   *
   * @param  type   The type to use for this octet string element.
   * @param  value  The byte array to use as the value for this octet string.
   */
  public ASN1OctetString(byte type, byte[] value)
  {
    super(type, value);
  }



  /**
   * Retrieves the value of this ASN.1 element as a Java string.
   *
   * @return  The value of this ASN.1 element as a Java string.
   */
  public String getStringValue()
  {
    return new String(value);
  }



  /**
   * Decodes the provided byte array as an ASN.1 octet string element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 octet string element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 octet string element.
   */
  public static ASN1OctetString decodeAsOctetString(byte[] encodedValue)
         throws ASN1Exception
  {
    // First make sure that there actually was a value provided
    if ((encodedValue == null) || (encodedValue.length == 0))
    {
      throw new ASN1Exception("No data to decode");
    }


    // Make sure that the encoded value is at least two bytes.  Otherwise, there
    // can't be both a type and a length
    if (encodedValue.length < 2)
    {
      throw new ASN1Exception("Not enough data to make a valid ASN.1 element");
    }


    // First, see if the type is supposed to be a single byte or multiple bytes.
    if ((encodedValue[0] & 0x1F) == 0x1F)
    {
      // This indicates that the type is supposed to consist of multiple bytes,
      // which we do not support, so throw an exception
      throw new ASN1Exception("Multibyte type detected (not supported in " +
                              "this package)");
    }
    byte type = encodedValue[0];


    // Next, look at the second byte to see if there is a single byte or
    // multibyte length.
    int length = 0;
    int valueStartPos = 2;
    if ((encodedValue[1] & 0x7F) != encodedValue[1])
    {
      if ((encodedValue[1] & 0x7F) == 0x00)
      {
        length = 128;
      }
      else
      {
        int numLengthBytes = (encodedValue[1] & 0x7F);
        if (encodedValue.length < (numLengthBytes + 2))
        {
          throw new ASN1Exception ("Determined the length is encoded in " +
                                   numLengthBytes + " bytes, but not enough " +
                                   "bytes exist in the encoded value");
        }
        else
        {
          byte[] lengthArray = new byte[numLengthBytes+1];
          lengthArray[0] = encodedValue[1];
          System.arraycopy(encodedValue, 2, lengthArray, 1, numLengthBytes);
          length = decodeLength(lengthArray);
          valueStartPos += numLengthBytes;
        }
      }
    }
    else
    {
      length = encodedValue[1];
    }


    // Make sure that there are the correct number of bytes in the value.  If
    // not, then throw an exception.
    if ((encodedValue.length - valueStartPos) != length)
    {
      throw new ASN1Exception("Expected a value of " + length + " bytes, but " +
                              (encodedValue.length - valueStartPos) +
                              " bytes exist");
    }
    byte[] value = new byte[length];
    System.arraycopy(encodedValue, valueStartPos, value, 0, length);


    return new ASN1OctetString(type, value);
  }
}

