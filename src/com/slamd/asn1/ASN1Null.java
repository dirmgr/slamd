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
 * This class defines an ASN.1 element that does not hold any value.  It is
 * generally used as a placeholder in cases where there needs to be an element
 * to complete the encoding but there is no appropriate value to use in that
 * case.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Null
       extends ASN1Element
{
  /**
   * A static version of a NULL element that can be used to prevent the need to
   * create new instances whenever a null is needed with the default type.
   */
  public static final ASN1Null NULL_ELEMENT = new ASN1Null();



  /**
   * Creates a new ASN.1 null element.
   */
  public ASN1Null()
  {
    this(ASN1_NULL_TYPE);
  }



  /**
   * Creates a new ASN.1 null element with the specified type.
   *
   * @param  type  The BER type to assign to this ASN.1 element.
   */
  public ASN1Null(byte type)
  {
    super(type, EMPTY_BYTES);
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
   * Decodes the provided byte array as an ASN.1 null element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 null element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 null element.
   */
  public static ASN1Null decodeAsNull(byte[] encodedValue)
         throws ASN1Exception
  {
    // Make sure that there was a value provided
    if ((encodedValue == null) || (encodedValue.length == 0))
    {
      throw new ASN1Exception("No value to decode");
    }


    // Make sure that the length of the encoded value is two bytes (1 for type,
    // 1 for length)
    if (encodedValue.length != 2)
    {
      throw new ASN1Exception("Expected 2 bytes in encoded value, but " +
                              encodedValue.length + " bytes exist");
    }


    // Make sure that the type isn't multivalued
    if ((encodedValue[0] & 0x1F) == 0x1F)
    {
      throw new ASN1Exception("Multivalued byte detected (not supported in " +
                              "this package)");
    }


    // Make sure that the length is zero
    if (encodedValue[1] != 0x00)
    {
      throw new ASN1Exception("Length of null should be zero, not " +
                              encodedValue[1]);
    }


    return new ASN1Null(encodedValue[0]);
  }
}

