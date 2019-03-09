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
 * This class defines an ASN.1 element that can hold a Boolean value (for
 * storing values of either true or false).
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Boolean
       extends ASN1Element
{
  /**
   * The value that will be used for Boolean values of false.  A Boolean value
   * will only be considered false if it is equal to this value.
   */
  public static final byte BOOLEAN_FALSE = 0x00;



  /**
   * The byte array that will be used as the "false" value for Boolean elements.
   */
  public static final byte[] BOOLEAN_FALSE_ARRAY = new byte[] { BOOLEAN_FALSE };



  /**
   * The value that will be used for Boolean values of true.  A Boolean value
   * will be considered true as long is it does not equal the value for false,
   * but this specific value will be used for encoding true values.
   */
  public static final byte BOOLEAN_TRUE = (byte) 0xFF;



  /**
   * The byte array that will be used as the "true" value for Boolean elements.
   */
  public static final byte[] BOOLEAN_TRUE_ARRAY = new byte[] { BOOLEAN_TRUE };



  /**
   * The ASN.1 Boolean element with a value of "false".
   */
  public static final ASN1Boolean FALSE_ELEMENT = new ASN1Boolean(false);



  /**
   * The ASN.1 Boolean element with a value of "true".
   */
  public static final ASN1Boolean TRUE_ELEMENT = new ASN1Boolean(true);



  // The Java boolean that corresponds to the value of this Boolean element.
  boolean booleanValue;



  /**
   * Creates a new ASN.1 Boolean element with the specified value.
   *
   * @param  booleanValue  The Java boolean value to use to create this ASN.1
   *                       Boolean element.
   */
  public ASN1Boolean(boolean booleanValue)
  {
    this(ASN1_BOOLEAN_TYPE, booleanValue);
  }



  /**
   * Creates a new ASN.1 Boolean element with the specified type and value.
   *
   * @param  type          The type to use for this ASN.1 Boolean element.
   * @param  booleanValue  The Java boolean value to use to create this ASN.1
   *                       Boolean element.
   */
  public ASN1Boolean(byte type, boolean booleanValue)
  {
    super(type, (booleanValue ? BOOLEAN_TRUE_ARRAY : BOOLEAN_FALSE_ARRAY));
    this.booleanValue = booleanValue;
  }



  /**
   * Retrieves the Java boolean value associated with this ASN.1 Boolean
   * element.
   *
   * @return  The Java boolean value associated with this ASN.1 Boolean element.
   */
  public boolean getBooleanValue()
  {
    return booleanValue;
  }



  /**
   * Encodes this ASN.1 element into a byte array.
   *
   * @return  This ASN.1 element encoded as a byte array.
   */
  @Override()
  public byte[] encode()
  {
    return encodedElement;
  }



  /**
   * Decodes the provided byte array as an ASN.1 Boolean element.
   *
   * @param  encodedValue  The encoded byte array to decode as an ASN.1 Boolean
   *                       element.
   *
   * @return  The decoded ASN.1 Boolean element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 Boolean element.
   */
  public static ASN1Boolean decodeAsBoolean(byte[] encodedValue)
         throws ASN1Exception
  {
    // Make sure that there was actually a value specified
    if ((encodedValue == null) || (encodedValue.length == 0))
    {
      throw new ASN1Exception("No value provided to decode");
    }


    // An encoded Boolean value should consist of three bytes (given that we
    // don't support multibyte types)
    if (encodedValue.length != 3)
    {
      throw new ASN1Exception("Expected 3 bytes in encoded value, but " +
                              encodedValue.length + " bytes exist");
    }


    // Make sure that we're not expecting a multibyte type
    if ((encodedValue[0] & 0x1F) == 0x1F)
    {
      throw new ASN1Exception("Multibyte type detected (not supported in " +
                              "this package)");
    }
    byte type = encodedValue[0];


    // Make sure that the length is only a single byte
    if (encodedValue[1] != 0x01)
    {
      throw new ASN1Exception("Length of a Boolean element is not 1");
    }


    // Determine whether the value is true or false.  Only a value of 0x00 will
    // be considered false.  All other values will be considered true.
    boolean booleanValue = (encodedValue[2] != 0x00);


    return new ASN1Boolean(type, booleanValue);
  }
}

