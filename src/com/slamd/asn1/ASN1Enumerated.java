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
 * This class defines an ASN.1 element that can hold an enumerated value.  It
 * behaves in all respects like an integer, with the exception that each value
 * of an enumerated element has a particular meaning (application-specific or
 * context-specific) associated with it.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Enumerated
       extends ASN1Element
{
  // The Java int value that corresponds to the value of this ASN.1 enumerated
  // element
  int intValue;



  /**
   * Creates a new ASN.1 enumerated element with the specified value.
   *
   * @param  intValue  The Java int value to use in creating the ASN.1
   *                   enumerated element.
   */
  public ASN1Enumerated(int intValue)
  {
    this(ASN1_ENUMERATED_TYPE, intValue);
  }



  /**
   * Creates a new ASN.1 enumerated element with the specified type and value.
   *
   * @param  type      The type to use for this enumerated value.
   * @param  intValue  The Java int value to use in creating the ASN.1
   *                   enumerated value.
   */
  public ASN1Enumerated(byte type, int intValue)
  {
    super(type);

    setValue(encodeIntValue(intValue));
    this.intValue = intValue;
  }



  /**
   * Encodes the provided int value in the appropriate manner for an ASN.1
   * enumerated value.
   *
   * @param  intValue  The Java int value to encode as an ASN.1 enumerated
   *                   value.
   *
   * @return  A byte array that contains the encoded int value.
   */
  public static byte[] encodeIntValue(int intValue)
  {
    return ASN1Integer.encodeIntValue(intValue);
  }



  /**
   * Retrieves the Java int value that corresponds to the value of this ASN.1
   * enumerated element.
   *
   * @return  The Java int value that corresponds to the value of this ASN.1
   *          enumerated element.
   */
  public int getIntValue()
  {
    return intValue;
  }



  /**
   * Decodes the provided byte array as an ASN.1 enumerated element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 enumerated element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 enumerated element.
   */
  public static ASN1Enumerated decodeAsEnumerated(byte[] encodedValue)
         throws ASN1Exception
  {
    // First make sure that there actually was a value provided
    if ((encodedValue == null) || (encodedValue.length == 0))
    {
      throw new ASN1Exception("No data to decode");
    }


    // Make sure that the encoded value is at least three bytes.  Otherwise,
    // there can't be a type, length, and value.
    if (encodedValue.length < 3)
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
    int intValue = ASN1Integer.decodeIntValue(value);

    ASN1Enumerated enumeratedElement = new ASN1Enumerated(type);
    enumeratedElement.intValue = intValue;
    enumeratedElement.value = value;

    return enumeratedElement;
  }
}

