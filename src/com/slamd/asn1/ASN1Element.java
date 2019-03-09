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
 * This class defines a generic ASN.1 element and a set of core methods for
 * dealing with them.  Subclasses may deal with more specific kinds of ASN.1
 * elements.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Element
{
  /**
   * The standard ASN.1 type for boolean values.
   */
  public static final byte ASN1_BOOLEAN_TYPE = 0x01;



  /**
   * The standard ASN.1 type for integer values.
   */
  public static final byte ASN1_INTEGER_TYPE = 0x02;



  /**
   * The standard ASN.1 type for octet string values.
   */
  public static final byte ASN1_OCTET_STRING_TYPE = 0x04;



  /**
   * The standard ASN.1 type for null values.
   */
  public static final byte ASN1_NULL_TYPE = 0x05;



  /**
   * The standard ASN.1 type for enumerated values.
   */
  public static final byte ASN1_ENUMERATED_TYPE = 0x0A;



  /**
   * The standard ASN.1 type for sequence values.
   */
  public static final byte ASN1_SEQUENCE_TYPE = 0x30;



  /**
   * The standard ASN.1 type for set values.
   */
  public static final byte ASN1_SET_TYPE = 0x31;



  /**
   * An empty byte array, used to prevent multiple allocations for empty arrays.
   */
  public static final byte[] EMPTY_BYTES = new byte[0];



  // The type of this ASN.1 element.  This implementation only supports
  // single-byte type values (up through "APPLICATION 31").
  byte   type;

  // The encoded form of this ASN.1 element.
  byte[] encodedElement;

  // The encoded value for this ASN.1 element.
  byte[] value;


  // The end of line character(s) for this platform.
  static String eol = System.getProperty("line.separator");



  /**
   * Creates a new ASN.1 element with the specified type and no value.
   *
   * @param  type   The type of this ASN.1 element.
   */
  public ASN1Element(byte type)
  {
    this(type, EMPTY_BYTES);
  }



  /**
   * Creates a new ASN.1 element with the specified type and value.
   *
   * @param  type   The type of this ASN.1 element.
   * @param  value  The encoded value for this ASN.1 element.
   */
  public ASN1Element(byte type, byte[] value)
  {
    this.type = type;

    if (value == null)
    {
      value = EMPTY_BYTES;
    }
    this.value = value;

    byte[] encodedLength = encodeLength(value.length);
    encodedElement = new byte[1 + encodedLength.length + value.length];
    encodedElement[0] = type;
    System.arraycopy(encodedLength, 0, encodedElement, 1, encodedLength.length);
    System.arraycopy(value, 0, encodedElement, 1+encodedLength.length,
                     value.length);
  }



  /**
   * Creates a new ASN.1 element with the specified information.
   *
   * @param  type            The type of this ASN.1 element.
   * @param  value           The value for this ASN.1 element.
   * @param  encodedElement  The ASN.1 element encoded as a byte array.
   */
  public ASN1Element(byte type, byte[] value, byte[] encodedElement)
  {
    this.type           = type;
    this.value          = value;
    this.encodedElement = encodedElement;
  }



  /**
   * Gets the type of this ASN.1 element.
   *
   * @return  The type of this ASN.1 element.
   */
  public byte getType()
  {
    return type;
  }



  /**
   * Gets the type of this ASN.1 element without either of the class bits or the
   * primitive/constructed bit set (i.e., the least significant five bits).
   *
   * @return  The type of this ASN.1 element without any of the flag bits set.
   */
  public byte getTypeWithoutFlags()
  {
    return (byte) (type & 0x1F);
  }



  /**
   * Specifies the type for this ASN.1 element.
   *
   * @param  type  The type for this ASN.1 element.
   */
  public void setType(byte type)
  {
    this.type = type;
  }



  /**
   * Gets the encoded value for this ASN.1 element.
   *
   * @return  The encoded value for this ASN.1 element.
   */
  public byte[] getValue()
  {
    return value;
  }



  /**
   * Specifies the value for this ASN.1 element.
   *
   * @param  value  The value for this ASN.1 element.
   */
  public void setValue(byte[] value)
  {
    if (value == null)
    {
      value = EMPTY_BYTES;
    }

    this.value = value;
    byte[] encodedLength = encodeLength(value.length);
    encodedElement = new byte[1 + encodedLength.length + value.length];
    encodedElement[0] = type;
    System.arraycopy(encodedLength, 0, encodedElement, 1,
                     encodedLength.length);
    System.arraycopy(value, 0, encodedElement, 1+encodedLength.length,
                     value.length);
  }



  /**
   * Encodes the specified length as a byte array as it should appear in an
   * ASN.1 element.
   *
   * @param  length  The length value to be encoded.
   *
   * @return  The encoded length as a byte array.
   */
  protected static byte[] encodeLength(int length)
  {
    // First, see if the int value is within the first 128 values.  If so, then
    // just return the pre-encoded version.
    switch (length)
    {
      case 0:   return ASN1Integer.INT_VALUE_0;
      case 1:   return ASN1Integer.INT_VALUE_1;
      case 2:   return ASN1Integer.INT_VALUE_2;
      case 3:   return ASN1Integer.INT_VALUE_3;
      case 4:   return ASN1Integer.INT_VALUE_4;
      case 5:   return ASN1Integer.INT_VALUE_5;
      case 6:   return ASN1Integer.INT_VALUE_6;
      case 7:   return ASN1Integer.INT_VALUE_7;
      case 8:   return ASN1Integer.INT_VALUE_8;
      case 9:   return ASN1Integer.INT_VALUE_9;
      case 10:  return ASN1Integer.INT_VALUE_10;
      case 11:  return ASN1Integer.INT_VALUE_11;
      case 12:  return ASN1Integer.INT_VALUE_12;
      case 13:  return ASN1Integer.INT_VALUE_13;
      case 14:  return ASN1Integer.INT_VALUE_14;
      case 15:  return ASN1Integer.INT_VALUE_15;
      case 16:  return ASN1Integer.INT_VALUE_16;
      case 17:  return ASN1Integer.INT_VALUE_17;
      case 18:  return ASN1Integer.INT_VALUE_18;
      case 19:  return ASN1Integer.INT_VALUE_19;
      case 20:  return ASN1Integer.INT_VALUE_20;
      case 21:  return ASN1Integer.INT_VALUE_21;
      case 22:  return ASN1Integer.INT_VALUE_22;
      case 23:  return ASN1Integer.INT_VALUE_23;
      case 24:  return ASN1Integer.INT_VALUE_24;
      case 25:  return ASN1Integer.INT_VALUE_25;
      case 26:  return ASN1Integer.INT_VALUE_26;
      case 27:  return ASN1Integer.INT_VALUE_27;
      case 28:  return ASN1Integer.INT_VALUE_28;
      case 29:  return ASN1Integer.INT_VALUE_29;
      case 30:  return ASN1Integer.INT_VALUE_30;
      case 31:  return ASN1Integer.INT_VALUE_31;
      case 32:  return ASN1Integer.INT_VALUE_32;
      case 33:  return ASN1Integer.INT_VALUE_33;
      case 34:  return ASN1Integer.INT_VALUE_34;
      case 35:  return ASN1Integer.INT_VALUE_35;
      case 36:  return ASN1Integer.INT_VALUE_36;
      case 37:  return ASN1Integer.INT_VALUE_37;
      case 38:  return ASN1Integer.INT_VALUE_38;
      case 39:  return ASN1Integer.INT_VALUE_39;
      case 40:  return ASN1Integer.INT_VALUE_40;
      case 41:  return ASN1Integer.INT_VALUE_41;
      case 42:  return ASN1Integer.INT_VALUE_42;
      case 43:  return ASN1Integer.INT_VALUE_43;
      case 44:  return ASN1Integer.INT_VALUE_44;
      case 45:  return ASN1Integer.INT_VALUE_45;
      case 46:  return ASN1Integer.INT_VALUE_46;
      case 47:  return ASN1Integer.INT_VALUE_47;
      case 48:  return ASN1Integer.INT_VALUE_48;
      case 49:  return ASN1Integer.INT_VALUE_49;
      case 50:  return ASN1Integer.INT_VALUE_50;
      case 51:  return ASN1Integer.INT_VALUE_51;
      case 52:  return ASN1Integer.INT_VALUE_52;
      case 53:  return ASN1Integer.INT_VALUE_53;
      case 54:  return ASN1Integer.INT_VALUE_54;
      case 55:  return ASN1Integer.INT_VALUE_55;
      case 56:  return ASN1Integer.INT_VALUE_56;
      case 57:  return ASN1Integer.INT_VALUE_57;
      case 58:  return ASN1Integer.INT_VALUE_58;
      case 59:  return ASN1Integer.INT_VALUE_59;
      case 60:  return ASN1Integer.INT_VALUE_60;
      case 61:  return ASN1Integer.INT_VALUE_61;
      case 62:  return ASN1Integer.INT_VALUE_62;
      case 63:  return ASN1Integer.INT_VALUE_63;
      case 64:  return ASN1Integer.INT_VALUE_64;
      case 65:  return ASN1Integer.INT_VALUE_65;
      case 66:  return ASN1Integer.INT_VALUE_66;
      case 67:  return ASN1Integer.INT_VALUE_67;
      case 68:  return ASN1Integer.INT_VALUE_68;
      case 69:  return ASN1Integer.INT_VALUE_69;
      case 70:  return ASN1Integer.INT_VALUE_70;
      case 71:  return ASN1Integer.INT_VALUE_71;
      case 72:  return ASN1Integer.INT_VALUE_72;
      case 73:  return ASN1Integer.INT_VALUE_73;
      case 74:  return ASN1Integer.INT_VALUE_74;
      case 75:  return ASN1Integer.INT_VALUE_75;
      case 76:  return ASN1Integer.INT_VALUE_76;
      case 77:  return ASN1Integer.INT_VALUE_77;
      case 78:  return ASN1Integer.INT_VALUE_78;
      case 79:  return ASN1Integer.INT_VALUE_79;
      case 80:  return ASN1Integer.INT_VALUE_80;
      case 81:  return ASN1Integer.INT_VALUE_81;
      case 82:  return ASN1Integer.INT_VALUE_82;
      case 83:  return ASN1Integer.INT_VALUE_83;
      case 84:  return ASN1Integer.INT_VALUE_84;
      case 85:  return ASN1Integer.INT_VALUE_85;
      case 86:  return ASN1Integer.INT_VALUE_86;
      case 87:  return ASN1Integer.INT_VALUE_87;
      case 88:  return ASN1Integer.INT_VALUE_88;
      case 89:  return ASN1Integer.INT_VALUE_89;
      case 90:  return ASN1Integer.INT_VALUE_90;
      case 91:  return ASN1Integer.INT_VALUE_91;
      case 92:  return ASN1Integer.INT_VALUE_92;
      case 93:  return ASN1Integer.INT_VALUE_93;
      case 94:  return ASN1Integer.INT_VALUE_94;
      case 95:  return ASN1Integer.INT_VALUE_95;
      case 96:  return ASN1Integer.INT_VALUE_96;
      case 97:  return ASN1Integer.INT_VALUE_97;
      case 98:  return ASN1Integer.INT_VALUE_98;
      case 99:  return ASN1Integer.INT_VALUE_99;
      case 100: return ASN1Integer.INT_VALUE_100;
      case 101: return ASN1Integer.INT_VALUE_101;
      case 102: return ASN1Integer.INT_VALUE_102;
      case 103: return ASN1Integer.INT_VALUE_103;
      case 104: return ASN1Integer.INT_VALUE_104;
      case 105: return ASN1Integer.INT_VALUE_105;
      case 106: return ASN1Integer.INT_VALUE_106;
      case 107: return ASN1Integer.INT_VALUE_107;
      case 108: return ASN1Integer.INT_VALUE_108;
      case 109: return ASN1Integer.INT_VALUE_109;
      case 110: return ASN1Integer.INT_VALUE_110;
      case 111: return ASN1Integer.INT_VALUE_111;
      case 112: return ASN1Integer.INT_VALUE_112;
      case 113: return ASN1Integer.INT_VALUE_113;
      case 114: return ASN1Integer.INT_VALUE_114;
      case 115: return ASN1Integer.INT_VALUE_115;
      case 116: return ASN1Integer.INT_VALUE_116;
      case 117: return ASN1Integer.INT_VALUE_117;
      case 118: return ASN1Integer.INT_VALUE_118;
      case 119: return ASN1Integer.INT_VALUE_119;
      case 120: return ASN1Integer.INT_VALUE_120;
      case 121: return ASN1Integer.INT_VALUE_121;
      case 122: return ASN1Integer.INT_VALUE_122;
      case 123: return ASN1Integer.INT_VALUE_123;
      case 124: return ASN1Integer.INT_VALUE_124;
      case 125: return ASN1Integer.INT_VALUE_125;
      case 126: return ASN1Integer.INT_VALUE_126;
      case 127: return ASN1Integer.INT_VALUE_127;
    }


    // It was not less than or equal to 128, so do it the "long" way.
    if ((length & 0xFF000000) != 0)
    {
      byte[] returnArray = new byte[5];
      returnArray[0] = (byte) 0x84;
      returnArray[1] = (byte) ((length & 0xFF000000) >>> 24);
      returnArray[2] = (byte) ((length & 0x00FF0000) >>> 16);
      returnArray[3] = (byte) ((length & 0x0000FF00) >>> 8);
      returnArray[4] = (byte) (length & 0x000000FF);
      return returnArray;
    }
    else if ((length & 0x00FF0000) != 0)
    {
      byte[] returnArray = new byte[4];
      returnArray[0] = (byte) 0x83;
      returnArray[1] = (byte) ((length & 0x00FF0000) >>> 16);
      returnArray[2] = (byte) ((length & 0x0000FF00) >>> 8);
      returnArray[3] = (byte) (length & 0x000000FF);
      return returnArray;
    }
    else if ((length & 0x0000FF00) != 0)
    {
      byte[] returnArray = new byte[3];
      returnArray[0] = (byte) 0x82;
      returnArray[1] = (byte) ((length & 0x0000FF00) >>> 8);
      returnArray[2] = (byte) (length & 0x000000FF);
      return returnArray;
    }
    else
    {
      byte[] returnArray = new byte[2];
      returnArray[0] = (byte) 0x81;
      returnArray[1] = (byte) (length & 0x000000FF);
      return returnArray;
    }
  }



  /**
   * Decodes the provided byte array as a length.
   *
   * @param  encodedLength  The encoded value to decode as a length.
   *
   * @return  The length decoded from the provided byte array.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 length.
   */
  public static int decodeLength(byte[] encodedLength)
         throws ASN1Exception
  {
    // First, make sure that an encoded value was actually provided.
    if ((encodedLength == null) || (encodedLength.length == 0))
    {
      throw new ASN1Exception("No length provided to decode");
    }

    if (encodedLength.length == 1)
    {
      if ((encodedLength[0] & 0x7F) == encodedLength[0])
      {
        // There is only a single byte and its value is less than 128, so just
        // return it as is.
        return encodedLength[0];
      }
      else if ((encodedLength[0] & 0x7F) == 0)
      {
        // There is only a single byte and its value is 0x80, so just return
        // 128.
        return 128;
      }
      else
      {
        // There is only a single byte, but the 0x80 bit is set, which requires
        // that multiple bytes actually be provided, so throw an exception
        throw new ASN1Exception("Only one byte in length, but it is an " +
                                "invalid value");
      }
    }
    else
    {
      if ((encodedLength[0] & 0x7F) != encodedLength[0])
      {
        // There are multiple bytes in this length and the first byte appears to
        // be set properly.  Put together the appropriate value if it can be
        // encoded as a Java integer, or throw an exception if it cannot.
        int numLengthBytes = (encodedLength[0] & 0x7F);
        if (numLengthBytes == (encodedLength.length - 1))
        {
          if (numLengthBytes <= 4)
          {
            byte[] byteArray = new byte[numLengthBytes];
            System.arraycopy(encodedLength, 1, byteArray, 0, byteArray.length);
            return byteArrayToInt(byteArray);
          }

           throw new ASN1Exception("Unable to represent length " +
                                   "as a Java int");
        }
        else
        {
          // The length of the array does not equal the expected length.
          throw new ASN1Exception("Encoded length indicates " + numLengthBytes +
                                  " bytes in length, but " +
                                  (encodedLength.length - 1) +
                                  " bytes were provided");
        }
      }
      else
      {
        // There are multiple bytes in the value, but the first byte does not
        // have the 0x80 bit set to specify the number of bytes in the length.
        throw new ASN1Exception("Multiple bytes in length, but first byte " +
                                "does not have 0x80 bit set");
      }
    }
  }



  /**
   * Encodes this ASN.1 element into a byte array.
   *
   * @return  This ASN.1 element encoded as a byte array.
   */
  public byte[] encode()
  {
    return encodedElement;
  }



  /**
   * Encodes this ASN.1 element into the provided byte array at the indicated
   * position.
   *
   * @param  byteArray  The byte array into which the value is to be encoded.
   * @param  startPos   The position in the byte array at which to start writing
   *                    the encoded value.
   *
   * @return  The number of bytes written into the array, or -1 if the array was
   *          not big enough to hold the encoded value.
   */
  public int encode(byte[] byteArray, int startPos)
  {
    // First, make sure there is enough space in the provided byte array.
    int length = encodedElement.length;
    if (startPos + length > byteArray.length)
    {
      return -1;
    }


    // Encode the value into the array.
    System.arraycopy(encodedElement, 0, byteArray, startPos,
                     encodedElement.length);
    return length;
  }



  /**
   * Decodes the provided byte array as a generic ASN.1 element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 element.
   */
  public static ASN1Element decode(byte[] encodedValue)
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
                                   numLengthBytes + " bytes, but only " +
                                   encodedValue.length +
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


    return new ASN1Element(type, value, encodedValue);
  }



  /**
   * Decodes the provided byte array as a generic ASN.1 element.  The provided
   * array may contain a partial element, exactly a complete element, or
   * a complete element plus some additional data.  The beginning of the data
   * in the array must be the beginning of an ASN.1 element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The result of attempting to decode the element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 element.
   */
  public static ASN1DecodeResult decodePartial(byte[] encodedValue)
         throws ASN1Exception
  {
    // First make sure that there actually was a value provided and that there
    // are enough bytes to contain a valid element.
    if ((encodedValue == null) || (encodedValue.length < 2))
    {
      return new ASN1DecodeResult(null, null);
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
          return new ASN1DecodeResult(null, null);
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
    int numExtraBytes = (encodedValue.length - valueStartPos) - length;
    if (numExtraBytes < 0)
    {
      return new ASN1DecodeResult(null, null);
    }

    byte[] value = new byte[length];
    System.arraycopy(encodedValue, valueStartPos, value, 0, length);
    ASN1Element element = new ASN1Element(type, value, encodedValue);

    byte[] remainingData;
    if (numExtraBytes > 0)
    {
      remainingData = new byte[numExtraBytes];
      System.arraycopy(encodedValue, valueStartPos+length, remainingData, 0,
           numExtraBytes);
    }
    else
    {
      remainingData = null;
    }

    return new ASN1DecodeResult(element, remainingData);
  }



  /**
   * Decodes this element as an ASN.1 Boolean element.
   *
   * @return  The decoded ASN.1 Boolean element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1
   *                         Boolean element.
   */
  public ASN1Boolean decodeAsBoolean()
         throws ASN1Exception
  {
    return ASN1Boolean.decodeAsBoolean(encode());
  }



  /**
   * Decodes the provided byte array as an ASN.1 Boolean element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 Boolean element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 Boolean element.
   */
  public static ASN1Boolean decodeAsBoolean(byte[] encodedValue)
         throws ASN1Exception
  {
    return ASN1Boolean.decodeAsBoolean(encodedValue);
  }



  /**
   * Decodes this element as an ASN.1 integer element.
   *
   * @return  The decoded ASN.1 integer element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1
   *                         integer element.
   */
  public ASN1Integer decodeAsInteger()
         throws ASN1Exception
  {
    return ASN1Integer.decodeAsInteger(encodedElement);
  }



  /**
   * Decodes the provided byte array as an ASN.1 integer element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 integer element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 integer element.
   */
  public static ASN1Integer decodeAsInteger(byte[] encodedValue)
         throws ASN1Exception
  {
    return ASN1Integer.decodeAsInteger(encodedValue);
  }



  /**
   * Decodes this element as an ASN.1 octet string element.
   *
   * @return  The decoded ASN.1 octet string element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1
   *                         octet string element.
   */
  public ASN1OctetString decodeAsOctetString()
         throws ASN1Exception
  {
    return ASN1OctetString.decodeAsOctetString(encodedElement);
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
    return ASN1OctetString.decodeAsOctetString(encodedValue);
  }



  /**
   * Decodes this element as an ASN.1 null element.
   *
   * @return  The decoded ASN.1 null element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1 null
   *                         element.
   */
  public ASN1Null decodeAsNull()
         throws ASN1Exception
  {
    return ASN1Null.decodeAsNull(encodedElement);
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
    return ASN1Null.decodeAsNull(encodedValue);
  }



  /**
   * Decodes this element as an ASN.1 enumerated element.
   *
   * @return  The decoded ASN.1 enumerated element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1
   *                         enumerated element.
   */
  public ASN1Enumerated decodeAsEnumerated()
         throws ASN1Exception
  {
    return ASN1Enumerated.decodeAsEnumerated(encodedElement);
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
    return ASN1Enumerated.decodeAsEnumerated(encodedValue);
  }



  /**
   * Decodes this element as an ASN.1 sequence element.
   *
   * @return  The decoded ASN.1 sequence element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1
   *                         sequence element.
   */
  public ASN1Sequence decodeAsSequence()
         throws ASN1Exception
  {
    return ASN1Sequence.decodeAsSequence(encodedElement);
  }



  /**
   * Decodes the provided byte array as an ASN.1 sequence element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 sequence element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 sequence element.
   */
  public static ASN1Sequence decodeAsSequence(byte[] encodedValue)
         throws ASN1Exception
  {
    return ASN1Sequence.decodeAsSequence(encodedValue);
  }



  /**
   * Decodes this element as an ASN.1 set element.
   *
   * @return  The decoded ASN.1 set element.
   *
   * @throws  ASN1Exception  If this element cannot be decoded as an ASN.1 set
   *                         element.
   */
  public ASN1Set decodeAsSet()
         throws ASN1Exception
  {
    return ASN1Set.decodeAsSet(encodedElement);
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
    return ASN1Set.decodeAsSet(encodedValue);
  }



  /**
   * Converts the provided byte array into a Java int value.  Note that this
   * method assumes that the provided array contains no more than four bytes.
   *
   * @param  byteArray  The byte array containing the encoded integer value.
   *
   * @return  The Java int value decoded from the byte array.
   */
  protected static int byteArrayToInt(byte[] byteArray)
  {
    int value = 0x00000000;
    switch (byteArray.length)
    {
      case 1:   value |= (0x000000FF & byteArray[0]);
                break;
      case 2:   value |= ((0x000000FF & byteArray[0]) << 8) |
                         (0x000000FF & byteArray[1]);
                break;
      case 3:   value |= ((0x000000FF & byteArray[0]) << 16) |
                         ((0x000000FF & byteArray[1]) << 8) |
                         (0x000000FF & byteArray[2]);
                break;
      case 4:   value |= ((0x000000FF & byteArray[0]) << 24) |
                         ((0x000000FF & byteArray[1]) << 16) |
                         ((0x000000FF & byteArray[2]) << 8) |
                         (0x000000FF & byteArray[3]);
                break;
    }

    return value;
  }



  /**
   * Retrieves a string representation of this ASN.1 element.
   *
   * @return  A string representation of this ASN.1 element.
   */
  @Override()
  public String toString()
  {
    return toString(0);
  }



  /**
   * Retrieves a string representation of this ASN.1 element.  The information
   * will be indented the specified number of spaces.
   *
   * @param  indent  The number of spaces to indent each line of the output.
   *
   * @return  A string representation of this ASN.1 element.
   */
  public String toString(int indent)
  {
    String indentStr = "";
    for (int i=0; i < indent; i++)
    {
      indentStr += " ";
    }

    return indentStr + "Type:    " + type + eol +
           byteArrayToString(new byte[] { type }, 2+indent) + eol +
           indentStr + "Length:  " + value.length + eol +
           byteArrayToString(encodeLength(value.length), 2+indent) + eol +
           indentStr + "Value:   " + new String(value) + eol +
           byteArrayToString(value, 2+indent) + eol;
  }



  /**
   * Retrieves a string containing the hexadecimal digits contained in the
   * provided byte array.
   *
   * @param  byteArray  The byte array containing the information to be output.
   *
   * @return  A string containing the hexadecimal digits contained in the
   *          provided byte array.
   */
  public static String byteArrayToString(byte[] byteArray)
  {
    return byteArrayToString(byteArray, 0);
  }



  /**
   * Retrieves a string containing the hexadecimal digits contained in the
   * provided byte array.  Each line will be indented the specified number of
   * spaces.
   *
   * @param  byteArray  The byte array containing the information to be output.
   * @param  indent     The number of spaces to indent each line of the output.
   *
   * @return  A string containing the hexadecimal digits contained in the
   *          provided byte array.
   */
  public static String byteArrayToString(byte[] byteArray, int indent)
  {
    String indentStr = "";
    for (int i=0; i < indent; i++)
    {
      indentStr += " ";
    }

    String returnStr = indentStr;

    for (int i=0; i < byteArray.length; i++)
    {
      String hexStr = Integer.toHexString((0x000000FF & byteArray[i]));
      if (hexStr.length() == 1)
      {
        returnStr += "0";
      }

      returnStr += hexStr;
      if ((i+1) % 16 == 0)
      {
        returnStr += eol + indentStr;
      }
      else
      {
        returnStr += " ";
      }
    }

    if (byteArray.length % 16 != 0)
    {
      returnStr += eol;
    }

    return returnStr;
  }



  /**
   * Retrieves a string representation of the provided byte array (including the
   * ASCII equivalent).
   *
   * @param  byteArray  The byte array to be displayed as a string.
   *
   * @return  A string representation of the provided byte array with the ASCII
   *          equivalent.
   */
  public static String byteArrayToStringWithASCII(byte[] byteArray)
  {
    return byteArrayToStringWithASCII(byteArray, 0);
  }



  /**
   * Retrieves a string representation of the provided byte array (including the
   * ASCII equivalent) using the specified indent.
   *
   * @param  byteArray  The byte array to be displayed as a string.
   * @param  indent     The number of spaces to indent the output.
   *
   * @return  A string representation of the provided byte array with the ASCII
   *          equivalent.
   */
  public static String byteArrayToStringWithASCII(byte[] byteArray, int indent)
  {
    String EOL = System.getProperty("line.separator");

    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    StringBuilder buffer   = new StringBuilder();
    StringBuilder hexBuf   = new StringBuilder();
    StringBuilder asciiBuf = new StringBuilder();
    for (int i=0; i < byteArray.length; i++)
    {
      switch (byteArray[i])
      {
        case 0x00:
          hexBuf.append("00 ");
          asciiBuf.append('.');
          break;
        case 0x01:
          hexBuf.append("01 ");
          asciiBuf.append('.');
          break;
        case 0x02:
          hexBuf.append("02 ");
          asciiBuf.append('.');
          break;
        case 0x03:
          hexBuf.append("03 ");
          asciiBuf.append('.');
          break;
        case 0x04:
          hexBuf.append("04 ");
          asciiBuf.append('.');
          break;
        case 0x05:
          hexBuf.append("05 ");
          asciiBuf.append('.');
          break;
        case 0x06:
          hexBuf.append("06 ");
          asciiBuf.append('.');
          break;
        case 0x07:
          hexBuf.append("07 ");
          asciiBuf.append('.');
          break;
        case 0x08:
          hexBuf.append("08 ");
          asciiBuf.append('.');
          break;
        case 0x09:
          hexBuf.append("09 ");
          asciiBuf.append('.');
          break;
        case 0x0A:
          hexBuf.append("0A ");
          asciiBuf.append('.');
          break;
        case 0x0B:
          hexBuf.append("0B ");
          asciiBuf.append('.');
          break;
        case 0x0C:
          hexBuf.append("0C ");
          asciiBuf.append('.');
          break;
        case 0x0D:
          hexBuf.append("0D ");
          asciiBuf.append('.');
          break;
        case 0x0E:
          hexBuf.append("0E ");
          asciiBuf.append('.');
          break;
        case 0x0F:
          hexBuf.append("0F ");
          asciiBuf.append('.');
          break;
        case 0x10:
          hexBuf.append("10 ");
          asciiBuf.append('.');
          break;
        case 0x11:
          hexBuf.append("11 ");
          asciiBuf.append('.');
          break;
        case 0x12:
          hexBuf.append("12 ");
          asciiBuf.append('.');
          break;
        case 0x13:
          hexBuf.append("13 ");
          asciiBuf.append('.');
          break;
        case 0x14:
          hexBuf.append("14 ");
          asciiBuf.append('.');
          break;
        case 0x15:
          hexBuf.append("15 ");
          asciiBuf.append('.');
          break;
        case 0x16:
          hexBuf.append("16 ");
          asciiBuf.append('.');
          break;
        case 0x17:
          hexBuf.append("17 ");
          asciiBuf.append('.');
          break;
        case 0x18:
          hexBuf.append("18 ");
          asciiBuf.append('.');
          break;
        case 0x19:
          hexBuf.append("19 ");
          asciiBuf.append('.');
          break;
        case 0x1A:
          hexBuf.append("1A ");
          asciiBuf.append('.');
          break;
        case 0x1B:
          hexBuf.append("1B ");
          asciiBuf.append('.');
          break;
        case 0x1C:
          hexBuf.append("1C ");
          asciiBuf.append('.');
          break;
        case 0x1D:
          hexBuf.append("1D ");
          asciiBuf.append('.');
          break;
        case 0x1E:
          hexBuf.append("1E ");
          asciiBuf.append('.');
          break;
        case 0x1F:
          hexBuf.append("1F ");
          asciiBuf.append('.');
          break;
        case 0x20:
          hexBuf.append("20 ");
          asciiBuf.append(' ');
          break;
        case 0x21:
          hexBuf.append("21 ");
          asciiBuf.append('!');
          break;
        case 0x22:
          hexBuf.append("22 ");
          asciiBuf.append('"');
          break;
        case 0x23:
          hexBuf.append("23 ");
          asciiBuf.append('#');
          break;
        case 0x24:
          hexBuf.append("24 ");
          asciiBuf.append('$');
          break;
        case 0x25:
          hexBuf.append("25 ");
          asciiBuf.append('%');
          break;
        case 0x26:
          hexBuf.append("26 ");
          asciiBuf.append('&');
          break;
        case 0x27:
          hexBuf.append("27");
          asciiBuf.append('\'');
          break;
        case 0x28:
          hexBuf.append("28 ");
          asciiBuf.append('(');
          break;
        case 0x29:
          hexBuf.append("29 ");
          asciiBuf.append(')');
          break;
        case 0x2A:
          hexBuf.append("2A ");
          asciiBuf.append('*');
          break;
        case 0x2B:
          hexBuf.append("2B ");
          asciiBuf.append('+');
          break;
        case 0x2C:
          hexBuf.append("2C ");
          asciiBuf.append(',');
          break;
        case 0x2D:
          hexBuf.append("2D ");
          asciiBuf.append('-');
          break;
        case 0x2E:
          hexBuf.append("2E ");
          asciiBuf.append('.');
          break;
        case 0x2F:
          hexBuf.append("2F ");
          asciiBuf.append('/');
          break;
        case 0x30:
          hexBuf.append("30 ");
          asciiBuf.append('0');
          break;
        case 0x31:
          hexBuf.append("31 ");
          asciiBuf.append('1');
          break;
        case 0x32:
          hexBuf.append("32 ");
          asciiBuf.append('2');
          break;
        case 0x33:
          hexBuf.append("33 ");
          asciiBuf.append('3');
          break;
        case 0x34:
          hexBuf.append("34 ");
          asciiBuf.append('4');
          break;
        case 0x35:
          hexBuf.append("35 ");
          asciiBuf.append('5');
          break;
        case 0x36:
          hexBuf.append("36 ");
          asciiBuf.append('6');
          break;
        case 0x37:
          hexBuf.append("37 ");
          asciiBuf.append('7');
          break;
        case 0x38:
          hexBuf.append("38 ");
          asciiBuf.append('8');
          break;
        case 0x39:
          hexBuf.append("39 ");
          asciiBuf.append('9');
          break;
        case 0x3A:
          hexBuf.append("3A ");
          asciiBuf.append(':');
          break;
        case 0x3B:
          hexBuf.append("3B ");
          asciiBuf.append(';');
          break;
        case 0x3C:
          hexBuf.append("3C ");
          asciiBuf.append('<');
          break;
        case 0x3D:
          hexBuf.append("3D ");
          asciiBuf.append('=');
          break;
        case 0x3E:
          hexBuf.append("3E ");
          asciiBuf.append('>');
          break;
        case 0x3F:
          hexBuf.append("3F ");
          asciiBuf.append('?');
          break;
        case 0x40:
          hexBuf.append("40 ");
          asciiBuf.append('@');
          break;
        case 0x41:
          hexBuf.append("41 ");
          asciiBuf.append('A');
          break;
        case 0x42:
          hexBuf.append("42 ");
          asciiBuf.append('B');
          break;
        case 0x43:
          hexBuf.append("43 ");
          asciiBuf.append('C');
          break;
        case 0x44:
          hexBuf.append("44 ");
          asciiBuf.append('D');
          break;
        case 0x45:
          hexBuf.append("45 ");
          asciiBuf.append('E');
          break;
        case 0x46:
          hexBuf.append("46 ");
          asciiBuf.append('F');
          break;
        case 0x47:
          hexBuf.append("47 ");
          asciiBuf.append('G');
          break;
        case 0x48:
          hexBuf.append("48 ");
          asciiBuf.append('H');
          break;
        case 0x49:
          hexBuf.append("49 ");
          asciiBuf.append('I');
          break;
        case 0x4A:
          hexBuf.append("4A ");
          asciiBuf.append('J');
          break;
        case 0x4B:
          hexBuf.append("4B ");
          asciiBuf.append('K');
          break;
        case 0x4C:
          hexBuf.append("4C ");
          asciiBuf.append('L');
          break;
        case 0x4D:
          hexBuf.append("4D ");
          asciiBuf.append('M');
          break;
        case 0x4E:
          hexBuf.append("4E ");
          asciiBuf.append('N');
          break;
        case 0x4F:
          hexBuf.append("4F ");
          asciiBuf.append('O');
          break;
        case 0x50:
          hexBuf.append("50 ");
          asciiBuf.append('P');
          break;
        case 0x51:
          hexBuf.append("51 ");
          asciiBuf.append('Q');
          break;
        case 0x52:
          hexBuf.append("52 ");
          asciiBuf.append('R');
          break;
        case 0x53:
          hexBuf.append("53 ");
          asciiBuf.append('S');
          break;
        case 0x54:
          hexBuf.append("54 ");
          asciiBuf.append('T');
          break;
        case 0x55:
          hexBuf.append("55 ");
          asciiBuf.append('U');
          break;
        case 0x56:
          hexBuf.append("56 ");
          asciiBuf.append('V');
          break;
        case 0x57:
          hexBuf.append("57 ");
          asciiBuf.append('W');
          break;
        case 0x58:
          hexBuf.append("58 ");
          asciiBuf.append('X');
          break;
        case 0x59:
          hexBuf.append("59 ");
          asciiBuf.append('Y');
          break;
        case 0x5A:
          hexBuf.append("5A ");
          asciiBuf.append('Z');
          break;
        case 0x5B:
          hexBuf.append("5B ");
          asciiBuf.append('[');
          break;
        case 0x5C:
          hexBuf.append("5C ");
          asciiBuf.append('\\');
          break;
        case 0x5D:
          hexBuf.append("5D ");
          asciiBuf.append(']');
          break;
        case 0x5E:
          hexBuf.append("5E ");
          asciiBuf.append('^');
          break;
        case 0x5F:
          hexBuf.append("5F ");
          asciiBuf.append('_');
          break;
        case 0x60:
          hexBuf.append("60 ");
          asciiBuf.append('`');
          break;
        case 0x61:
          hexBuf.append("61 ");
          asciiBuf.append('a');
          break;
        case 0x62:
          hexBuf.append("62 ");
          asciiBuf.append('b');
          break;
        case 0x63:
          hexBuf.append("63 ");
          asciiBuf.append('c');
          break;
        case 0x64:
          hexBuf.append("64 ");
          asciiBuf.append('d');
          break;
        case 0x65:
          hexBuf.append("65 ");
          asciiBuf.append('e');
          break;
        case 0x66:
          hexBuf.append("66 ");
          asciiBuf.append('f');
          break;
        case 0x67:
          hexBuf.append("67 ");
          asciiBuf.append('g');
          break;
        case 0x68:
          hexBuf.append("68 ");
          asciiBuf.append('h');
          break;
        case 0x69:
          hexBuf.append("69 ");
          asciiBuf.append('i');
          break;
        case 0x6A:
          hexBuf.append("6A ");
          asciiBuf.append('j');
          break;
        case 0x6B:
          hexBuf.append("6B ");
          asciiBuf.append('k');
          break;
        case 0x6C:
          hexBuf.append("6C ");
          asciiBuf.append('l');
          break;
        case 0x6D:
          hexBuf.append("6D ");
          asciiBuf.append('m');
          break;
        case 0x6E:
          hexBuf.append("6E ");
          asciiBuf.append('n');
          break;
        case 0x6F:
          hexBuf.append("6F ");
          asciiBuf.append('o');
          break;
        case 0x70:
          hexBuf.append("70 ");
          asciiBuf.append('p');
          break;
        case 0x71:
          hexBuf.append("71 ");
          asciiBuf.append('q');
          break;
        case 0x72:
          hexBuf.append("72 ");
          asciiBuf.append('r');
          break;
        case 0x73:
          hexBuf.append("73 ");
          asciiBuf.append('s');
          break;
        case 0x74:
          hexBuf.append("74 ");
          asciiBuf.append('t');
          break;
        case 0x75:
          hexBuf.append("75 ");
          asciiBuf.append('u');
          break;
        case 0x76:
          hexBuf.append("76 ");
          asciiBuf.append('v');
          break;
        case 0x77:
          hexBuf.append("77 ");
          asciiBuf.append('w');
          break;
        case 0x78:
          hexBuf.append("78 ");
          asciiBuf.append('x');
          break;
        case 0x79:
          hexBuf.append("79 ");
          asciiBuf.append('y');
          break;
        case 0x7A:
          hexBuf.append("7A ");
          asciiBuf.append('z');
          break;
        case 0x7B:
          hexBuf.append("7B ");
          asciiBuf.append('{');
          break;
        case 0x7C:
          hexBuf.append("7C ");
          asciiBuf.append('|');
          break;
        case 0x7D:
          hexBuf.append("7D ");
          asciiBuf.append('}');
          break;
        case 0x7E:
          hexBuf.append("7E ");
          asciiBuf.append('~');
          break;
        case 0x7F:
          hexBuf.append("7F ");
          asciiBuf.append('.');
          break;
        case (byte) 0x80:
          hexBuf.append("80 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x81:
          hexBuf.append("81 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x82:
          hexBuf.append("82 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x83:
          hexBuf.append("83 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x84:
          hexBuf.append("84 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x85:
          hexBuf.append("85 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x86:
          hexBuf.append("86 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x87:
          hexBuf.append("87 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x88:
          hexBuf.append("88 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x89:
          hexBuf.append("89 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8A:
          hexBuf.append("8A ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8B:
          hexBuf.append("8B ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8C:
          hexBuf.append("8C ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8D:
          hexBuf.append("8D ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8E:
          hexBuf.append("8E ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8F:
          hexBuf.append("8F ");
          asciiBuf.append('.');
          break;
        case (byte) 0x90:
          hexBuf.append("90 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x91:
          hexBuf.append("91 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x92:
          hexBuf.append("92 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x93:
          hexBuf.append("93 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x94:
          hexBuf.append("94 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x95:
          hexBuf.append("95 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x96:
          hexBuf.append("96 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x97:
          hexBuf.append("97 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x98:
          hexBuf.append("98 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x99:
          hexBuf.append("99 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9A:
          hexBuf.append("9A ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9B:
          hexBuf.append("9B ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9C:
          hexBuf.append("9C ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9D:
          hexBuf.append("9D ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9E:
          hexBuf.append("9E ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9F:
          hexBuf.append("9F ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA0:
          hexBuf.append("A0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA1:
          hexBuf.append("A1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA2:
          hexBuf.append("A2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA3:
          hexBuf.append("A3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA4:
          hexBuf.append("A4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA5:
          hexBuf.append("A5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA6:
          hexBuf.append("A6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA7:
          hexBuf.append("A7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA8:
          hexBuf.append("A8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA9:
          hexBuf.append("A9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAA:
          hexBuf.append("AA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAB:
          hexBuf.append("AB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAC:
          hexBuf.append("AC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAD:
          hexBuf.append("AD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAE:
          hexBuf.append("AE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAF:
          hexBuf.append("AF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB0:
          hexBuf.append("B0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB1:
          hexBuf.append("B1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB2:
          hexBuf.append("B2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB3:
          hexBuf.append("B3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB4:
          hexBuf.append("B4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB5:
          hexBuf.append("B5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB6:
          hexBuf.append("B6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB7:
          hexBuf.append("B7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB8:
          hexBuf.append("B8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB9:
          hexBuf.append("B9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBA:
          hexBuf.append("BA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBB:
          hexBuf.append("BB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBC:
          hexBuf.append("BC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBD:
          hexBuf.append("BD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBE:
          hexBuf.append("BE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBF:
          hexBuf.append("BF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC0:
          hexBuf.append("C0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC1:
          hexBuf.append("C1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC2:
          hexBuf.append("C2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC3:
          hexBuf.append("C3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC4:
          hexBuf.append("C4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC5:
          hexBuf.append("C5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC6:
          hexBuf.append("C6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC7:
          hexBuf.append("C7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC8:
          hexBuf.append("C8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC9:
          hexBuf.append("C9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCA:
          hexBuf.append("CA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCB:
          hexBuf.append("CB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCC:
          hexBuf.append("CC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCD:
          hexBuf.append("CD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCE:
          hexBuf.append("CE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCF:
          hexBuf.append("CF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD0:
          hexBuf.append("D0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD1:
          hexBuf.append("D1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD2:
          hexBuf.append("D2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD3:
          hexBuf.append("D3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD4:
          hexBuf.append("D4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD5:
          hexBuf.append("D5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD6:
          hexBuf.append("D6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD7:
          hexBuf.append("D7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD8:
          hexBuf.append("D8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD9:
          hexBuf.append("D9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDA:
          hexBuf.append("DA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDB:
          hexBuf.append("DB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDC:
          hexBuf.append("DC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDD:
          hexBuf.append("DD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDE:
          hexBuf.append("DE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDF:
          hexBuf.append("DF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE0:
          hexBuf.append("E0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE1:
          hexBuf.append("E1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE2:
          hexBuf.append("E2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE3:
          hexBuf.append("E3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE4:
          hexBuf.append("E4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE5:
          hexBuf.append("E5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE6:
          hexBuf.append("E6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE7:
          hexBuf.append("E7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE8:
          hexBuf.append("E8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE9:
          hexBuf.append("E9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEA:
          hexBuf.append("EA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEB:
          hexBuf.append("EB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEC:
          hexBuf.append("EC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xED:
          hexBuf.append("ED ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEE:
          hexBuf.append("EE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEF:
          hexBuf.append("EF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF0:
          hexBuf.append("F0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF1:
          hexBuf.append("F1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF2:
          hexBuf.append("F2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF3:
          hexBuf.append("F3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF4:
          hexBuf.append("F4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF5:
          hexBuf.append("F5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF6:
          hexBuf.append("F6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF7:
          hexBuf.append("F7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF8:
          hexBuf.append("F8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF9:
          hexBuf.append("F9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFA:
          hexBuf.append("FA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFB:
          hexBuf.append("FB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFC:
          hexBuf.append("FC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFD:
          hexBuf.append("FD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFE:
          hexBuf.append("FE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFF:
          hexBuf.append("FF ");
          asciiBuf.append('.');
          break;
      }

      if ((i % 16) == 15)
      {
        buffer.append(indentBuf).append(hexBuf).append(' ').append(asciiBuf).
               append(EOL);

        hexBuf   = new StringBuilder();
        asciiBuf = new StringBuilder();
      }
      else if ((i % 8) == 7)
      {
        hexBuf.append(' ');
        asciiBuf.append(' ');
      }
    }

    int charsLeft = 16 - (byteArray.length % 16);
    if (charsLeft < 16)
    {
      for (int i=0; i < charsLeft; i++)
      {
        hexBuf.append("   ");
      }
      if (charsLeft > 8)
      {
        hexBuf.append(' ');
      }
    }

    buffer.append(indentBuf).append(hexBuf).append(' ').append(asciiBuf).
           append(EOL);
    return buffer.toString();
  }



  /**
   * Retrieves a byte array containing the binary representation of the
   * provided string.  If the provided string is 7-bit clean, then this method
   * is about 5 times faster than the standard Java
   * <CODE>String.getBytes()</CODE> method.
   *
   * @param  stringValue  The string for which to retrieve the binary
   *                      representation.
   *
   * @return  A byte array containing the binary representation of the provided
   *          string.
   */
  public static byte[] getBytes(String stringValue)
  {
    if (stringValue == null)
    {
      return new byte[0];
    }

    byte b;
    char c;
    byte[] returnArray = new byte[stringValue.length()];
    for (int i=0; i < returnArray.length; i++)
    {
      c = stringValue.charAt(i);
      b = (byte) (c & 0x0000007F);
      if (b == c)
      {
        returnArray[i] = b;
      }
      else
      {
        return stringValue.getBytes();
      }
    }

    return returnArray;
  }
}

