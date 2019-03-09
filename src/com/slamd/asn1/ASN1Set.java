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



import java.util.ArrayList;



/**
 * This class defines an ASN.1 element that serves as a set, whose value is an
 * unordered set of other ASN.1 elements.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Set
       extends ASN1Element
{
  /**
   * The set of elements that will be used if there are no elements.
   */
  public static final ASN1Element[] NO_ELEMENTS = new ASN1Element[0];



  // The set of ASN.1 elements associated with this set.
  ASN1Element[] elements;



  /**
   * Creates a new ASN.1 set with no elements encoded in the value.
   */
  public ASN1Set()
  {
    this(ASN1_SET_TYPE, NO_ELEMENTS);
  }



  /**
   * Creates a new ASN.1 set with the specified type and no elements encoded in
   * the value.
   *
   * @param  type  The type to use for this ASN.1 set.
   */
  public ASN1Set(byte type)
  {
    this(type, NO_ELEMENTS);
  }



  /**
   * Creates a new ASN.1 set to hold the specified set of ASN.1 elements.
   *
   * @param  elements  The set of ASN.1 elements to encode in the value of this
   *                   set.
   */
  public ASN1Set(ASN1Element[] elements)
  {
    this(ASN1_SET_TYPE, elements);
  }



  /**
   * Creates a new ASN.1 set with the given type to hold the specified set of
   * ASN.1 elements.
   *
   * @param  type      The type to use for this ASN.1 set.
   * @param  elements  The set of ASN.1 elements to encode in the value of this
   *                   set.
   */
  public ASN1Set(byte type, ASN1Element[] elements)
  {
    super(type);

    if (elements == null)
    {
      elements = NO_ELEMENTS;
    }
    this.elements = elements;

    int totalLength = 0;
    byte[][] encodedValues = new byte[elements.length][];
    for (int i=0; i < elements.length; i++)
    {
      encodedValues[i] = elements[i].encode();
      totalLength += encodedValues[i].length;
    }

    byte[] encodedValue = new byte[totalLength];
    int startPos = 0;
    for (int i=0; i < elements.length; i++)
    {
      System.arraycopy(encodedValues[i], 0, encodedValue, startPos,
                       encodedValues[i].length);
      startPos += encodedValues[i].length;
    }
    setValue(encodedValue);
  }



  /**
   * Creates a new ASN.1 sequence to hold the specified set of ASN.1 elements.
   *
   * @param  elements  The set of ASN.1 elements to encode in the value of this
   *                   sequence.
   */
  public ASN1Set(ArrayList<ASN1Element> elements)
  {
    this(ASN1_SEQUENCE_TYPE, elements);
  }



  /**
   * Creates a new ASN.1 sequence with the given type to hold the specified set
   * of ASN.1 elements.
   *
   * @param  type      The type to use for this ASN.1 sequence.
   * @param  elements  The set of ASN.1 elements to encode in the value of this
   *                   sequence.
   */
  public ASN1Set(byte type, ArrayList<ASN1Element> elements)
  {
    super(type);

    if ((elements == null) || elements.isEmpty())
    {
      this.elements = NO_ELEMENTS;
    }
    else
    {
      ASN1Element[] elementArray = new ASN1Element[elements.size()];
      elements.toArray(elementArray);
      replaceElements(elementArray);
    }
  }



  /**
   * Retrieves the set of elements that are encoded in this ASN.1 set.
   *
   * @return  The set of elements that are encoded in this ASN.1 set.
   */
  public ASN1Element[] getElements()
  {
    return elements;
  }



  /**
   * Adds the specified ASN.1 element to the set of elements encoded in this
   * set.
   *
   * @param  element  The ASN.1 element to include in the set of elements
   *                  encoded in this set.
   */
  public void addElement(ASN1Element element)
  {
    // Add the specified element to the current set.
    ASN1Element[] newElements = new ASN1Element[elements.length+1];
    System.arraycopy(elements, 0, newElements, 0, elements.length);
    newElements[elements.length] = element;
    this.elements = newElements;

    // Create the new encoded value by appending the bytes of the new element
    // to the existing value.
    byte[] encodedElement = element.encode();
    byte[] newSetValue = new byte[value.length + encodedElement.length];
    System.arraycopy(value, 0, newSetValue, 0, value.length);
    System.arraycopy(encodedElement, 0, newSetValue, value.length,
                     encodedElement.length);
    setValue(newSetValue);
  }



  /**
   * Removes all elements encoded in the value of this ASN.1 element.
   */
  public void removeAllElements()
  {
    setValue(EMPTY_BYTES);
    elements = NO_ELEMENTS;
  }



  /**
   * Replaces the current set of elements with the provided ASN.1 element.
   *
   * @param  element  The ASN.1 element to use to replace the existing set of
   *                  elements encoded in this set.
   */
  public void replaceElements(ASN1Element element)
  {
    replaceElements(new ASN1Element[] { element });
  }



  /**
   * Replaces the current set of elements with the provided set.
   *
   * @param  elements  The set of ASN.1 elements to use to replace the existing
   *                   set of elements encoded in this set.
   */
  public void replaceElements(ASN1Element[] elements)
  {
    if (elements == null)
    {
      elements = NO_ELEMENTS;
    }
    this.elements = elements;

    // Re-encode the data in this set.
    int totalLength = 0;
    byte[][] encodedValues = new byte[elements.length][];
    for (int i=0; i < elements.length; i++)
    {
      encodedValues[i] = elements[i].encode();
      totalLength += encodedValues[i].length;
    }

    byte[] encodedValue = new byte[totalLength];
    int startPos = 0;
    for (int i=0; i < elements.length; i++)
    {
      System.arraycopy(encodedValues[i], 0, encodedValue, startPos,
                       encodedValues[i].length);
      startPos += encodedValues[i].length;
    }
    setValue(encodedValue);
  }



  /**
   * Decodes the provided byte array as an ASN.1 set element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 set element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 set element.
   */
  public static ASN1Set decodeAsSet(byte[] encodedValue)
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
    ASN1Element[] elements = new ASN1Element[0];
    if (value.length > 0)
    {
      System.arraycopy(encodedValue, valueStartPos, value, 0, length);
      elements = ASN1Sequence.decodeSequenceElements(value);
    }


    // Finally, create the set and return it.  Because we have already done
    // all the work up front, don't do it again by using a constructor that will
    // re-encode.  Instead, make use of the fact that we have direct access to
    // instance variables.
    ASN1Set set = new ASN1Set(type);
    set.setValue(value);
    set.elements = elements;
    return set;
  }



  /**
   * Retrieves a string representation of this ASN.1 set.  It will recursively
   * display string representations for each of the elements.
   *
   * @param  indent  The number of spaces to indent the information in the
   *                 returned string.
   *
   * @return  A string representation of this ASN.1 set
   */
  @Override()
  public String toString(int indent)
  {
    String indentStr = "";
    for (int i=0; i < indent; i++)
    {
      indentStr += " ";
    }

    String elementsStr = "";
    for (int i=0; i < elements.length; i++)
    {
      ASN1Element element = elements[i];
      elementsStr += indentStr + "  Element " + i + eol +
                     element.toString(indent+2);
    }


    return indentStr + "Type:    " + type + eol +
           byteArrayToString(new byte[] { type }, 2+indent) + eol +
           indentStr + "Length:  " + value.length + eol +
           byteArrayToString(encodeLength(value.length), 2+indent) + eol +
           indentStr + "Value:   " + new String(value) + eol +
           byteArrayToString(value, 2+indent) + eol +
           "Elements:  " + eol +
           elementsStr;
  }
}

