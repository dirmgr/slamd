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
 * This class provides a data structure that can hold information about the
 * result of attempting to decode a byte array as an ASN.1 element.  If the
 * array contained at least a complete ASN.1 element, then that element will be
 * available, along with any remaining data (if there was more data beyond the
 * end of the element).  It is also possible to use this result to determine
 * if there was not enough data in the original array to hold a complete
 * element.
 */
public final class ASN1DecodeResult
{
  // The ASN.1 element that was decoded.
  private final ASN1Element decodedElement;

  // The data left over after decoding the element.
  private final byte[] remainingData;




  /**
   * Creates a new ASN.1 decode result with the provided information.
   *
   * @param  decodedElement  The ASN.1 element that was decoded.  This should be
   *                         {@code null} if the associated byte array did not
   *                         contain a complete element.
   * @param  remainingData   A byte array containing any data from the
   *                         associated byte array that were left over after
   *                         decoding the element.  This should be {@code null}
   *                         if the associated byte array did not contain a
   *                         complete element, or if there were no bytes left
   *                         over.
   */
  ASN1DecodeResult(final ASN1Element decodedElement, final byte[] remainingData)
  {
    this.decodedElement = decodedElement;
    this.remainingData  = remainingData;
  }



  /**
   * Retrieves the ASN.1 element decoded from the original byte array, if
   * available.
   *
   * @return  The ASN.1 element decoded from the original byte array, or
   *          {@code null} if the array did not contain a complete ASN.1
   *          element.
   */
  public ASN1Element getDecodedElement()
  {
    return decodedElement;
  }



  /**
   * Retrieves a byte array containing data that was left over after decoding
   * the ASN.1 element, if any.
   *
   * @return  A byte array containing data that was left over after decoding the
   *          ASN.1 element, or {@code null} if the array did not contain a
   *          complete ASN.1 element, or if there were no bytes left over after
   *          decoding the element.
   */
  public byte[] getRemainingData()
  {
    return remainingData;
  }
}
