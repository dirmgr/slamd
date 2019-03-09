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
 * This class defines an ASN.1 element that can hold an integer value.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Integer
       extends ASN1Element
{
  /**
   * The integer value 0 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_0 = new byte [] { 0x00 };



  /**
   * The integer value 1 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_1 = new byte [] { 0x01 };



  /**
   * The integer value 2 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_2 = new byte [] { 0x02 };



  /**
   * The integer value 3 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_3 = new byte [] { 0x03 };



  /**
   * The integer value 4 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_4 = new byte [] { 0x04 };



  /**
   * The integer value 5 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_5 = new byte [] { 0x05 };



  /**
   * The integer value 6 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_6 = new byte [] { 0x06 };



  /**
   * The integer value 7 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_7 = new byte [] { 0x07 };



  /**
   * The integer value 8 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_8 = new byte [] { 0x08 };



  /**
   * The integer value 9 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_9 = new byte [] { 0x09 };



  /**
   * The integer value 10 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_10 = new byte [] { 0x0A };



  /**
   * The integer value 11 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_11 = new byte [] { 0x0B };



  /**
   * The integer value 12 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_12 = new byte [] { 0x0C };



  /**
   * The integer value 13 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_13 = new byte [] { 0x0D };



  /**
   * The integer value 14 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_14 = new byte [] { 0x0E };



  /**
   * The integer value 15 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_15 = new byte [] { 0x0F };



  /**
   * The integer value 16 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_16 = new byte [] { 0x10 };



  /**
   * The integer value 17 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_17 = new byte [] { 0x11 };



  /**
   * The integer value 18 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_18 = new byte [] { 0x12 };



  /**
   * The integer value 19 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_19 = new byte [] { 0x13 };



  /**
   * The integer value 20 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_20 = new byte [] { 0x14 };



  /**
   * The integer value 21 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_21 = new byte [] { 0x15 };



  /**
   * The integer value 22 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_22 = new byte [] { 0x16 };



  /**
   * The integer value 23 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_23 = new byte [] { 0x17 };



  /**
   * The integer value 24 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_24 = new byte [] { 0x18 };



  /**
   * The integer value 25 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_25 = new byte [] { 0x19 };



  /**
   * The integer value 26 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_26 = new byte [] { 0x1A };



  /**
   * The integer value 27 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_27 = new byte [] { 0x1B };



  /**
   * The integer value 28 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_28 = new byte [] { 0x1C };



  /**
   * The integer value 29 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_29 = new byte [] { 0x1D };



  /**
   * The integer value 30 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_30 = new byte [] { 0x1E };



  /**
   * The integer value 31 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_31 = new byte [] { 0x1F };



  /**
   * The integer value 32 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_32 = new byte [] { 0x20 };



  /**
   * The integer value 33 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_33 = new byte [] { 0x21 };



  /**
   * The integer value 34 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_34 = new byte [] { 0x22 };



  /**
   * The integer value 35 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_35 = new byte [] { 0x23 };



  /**
   * The integer value 36 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_36 = new byte [] { 0x24 };



  /**
   * The integer value 37 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_37 = new byte [] { 0x25 };



  /**
   * The integer value 38 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_38 = new byte [] { 0x26 };



  /**
   * The integer value 39 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_39 = new byte [] { 0x27 };



  /**
   * The integer value 40 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_40 = new byte [] { 0x28 };



  /**
   * The integer value 41 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_41 = new byte [] { 0x29 };



  /**
   * The integer value 42 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_42 = new byte [] { 0x2A };



  /**
   * The integer value 43 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_43 = new byte [] { 0x2B };



  /**
   * The integer value 44 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_44 = new byte [] { 0x2C };



  /**
   * The integer value 45 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_45 = new byte [] { 0x2D };



  /**
   * The integer value 46 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_46 = new byte [] { 0x2E };



  /**
   * The integer value 47 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_47 = new byte [] { 0x2F };



  /**
   * The integer value 48 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_48 = new byte [] { 0x30 };



  /**
   * The integer value 49 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_49 = new byte [] { 0x31 };



  /**
   * The integer value 50 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_50 = new byte [] { 0x32 };



  /**
   * The integer value 51 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_51 = new byte [] { 0x33 };



  /**
   * The integer value 52 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_52 = new byte [] { 0x34 };



  /**
   * The integer value 53 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_53 = new byte [] { 0x35 };



  /**
   * The integer value 54 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_54 = new byte [] { 0x36 };



  /**
   * The integer value 55 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_55 = new byte [] { 0x37 };



  /**
   * The integer value 56 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_56 = new byte [] { 0x38 };



  /**
   * The integer value 57 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_57 = new byte [] { 0x39 };



  /**
   * The integer value 58 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_58 = new byte [] { 0x3A };



  /**
   * The integer value 59 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_59 = new byte [] { 0x3B };



  /**
   * The integer value 60 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_60 = new byte [] { 0x3C };



  /**
   * The integer value 61 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_61 = new byte [] { 0x3D };



  /**
   * The integer value 62 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_62 = new byte [] { 0x3E };



  /**
   * The integer value 63 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_63 = new byte [] { 0x3F };



  /**
   * The integer value 64 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_64 = new byte [] { 0x40 };



  /**
   * The integer value 65 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_65 = new byte [] { 0x41 };



  /**
   * The integer value 66 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_66 = new byte [] { 0x42 };



  /**
   * The integer value 67 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_67 = new byte [] { 0x43 };



  /**
   * The integer value 68 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_68 = new byte [] { 0x44 };



  /**
   * The integer value 69 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_69 = new byte [] { 0x45 };



  /**
   * The integer value 70 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_70 = new byte [] { 0x46 };



  /**
   * The integer value 71 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_71 = new byte [] { 0x47 };



  /**
   * The integer value 72 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_72 = new byte [] { 0x48 };



  /**
   * The integer value 73 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_73 = new byte [] { 0x49 };



  /**
   * The integer value 74 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_74 = new byte [] { 0x4A };



  /**
   * The integer value 75 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_75 = new byte [] { 0x4B };



  /**
   * The integer value 76 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_76 = new byte [] { 0x4C };



  /**
   * The integer value 77 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_77 = new byte [] { 0x4D };



  /**
   * The integer value 78 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_78 = new byte [] { 0x4E };



  /**
   * The integer value 79 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_79 = new byte [] { 0x4F };



  /**
   * The integer value 80 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_80 = new byte [] { 0x50 };



  /**
   * The integer value 81 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_81 = new byte [] { 0x51 };



  /**
   * The integer value 82 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_82 = new byte [] { 0x52 };



  /**
   * The integer value 83 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_83 = new byte [] { 0x53 };



  /**
   * The integer value 84 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_84 = new byte [] { 0x54 };



  /**
   * The integer value 85 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_85 = new byte [] { 0x55 };



  /**
   * The integer value 86 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_86 = new byte [] { 0x56 };



  /**
   * The integer value 87 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_87 = new byte [] { 0x57 };



  /**
   * The integer value 88 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_88 = new byte [] { 0x58 };



  /**
   * The integer value 89 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_89 = new byte [] { 0x59 };



  /**
   * The integer value 90 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_90 = new byte [] { 0x5A };



  /**
   * The integer value 91 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_91 = new byte [] { 0x5B };



  /**
   * The integer value 92 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_92 = new byte [] { 0x5C };



  /**
   * The integer value 93 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_93 = new byte [] { 0x5D };



  /**
   * The integer value 94 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_94 = new byte [] { 0x5E };



  /**
   * The integer value 95 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_95 = new byte [] { 0x5F };



  /**
   * The integer value 96 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_96 = new byte [] { 0x60 };



  /**
   * The integer value 97 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_97 = new byte [] { 0x61 };



  /**
   * The integer value 98 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_98 = new byte [] { 0x62 };



  /**
   * The integer value 99 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_99 = new byte [] { 0x63 };



  /**
   * The integer value 100 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_100 = new byte [] { 0x64 };



  /**
   * The integer value 101 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_101 = new byte [] { 0x65 };



  /**
   * The integer value 102 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_102 = new byte [] { 0x66 };



  /**
   * The integer value 103 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_103 = new byte [] { 0x67 };



  /**
   * The integer value 104 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_104 = new byte [] { 0x68 };



  /**
   * The integer value 105 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_105 = new byte [] { 0x69 };



  /**
   * The integer value 106 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_106 = new byte [] { 0x6A };



  /**
   * The integer value 107 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_107 = new byte [] { 0x6B };



  /**
   * The integer value 108 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_108 = new byte [] { 0x6C };



  /**
   * The integer value 109 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_109 = new byte [] { 0x6D };



  /**
   * The integer value 110 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_110 = new byte [] { 0x6E };



  /**
   * The integer value 111 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_111 = new byte [] { 0x6F };



  /**
   * The integer value 112 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_112 = new byte [] { 0x70 };



  /**
   * The integer value 113 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_113 = new byte [] { 0x71 };



  /**
   * The integer value 114 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_114 = new byte [] { 0x72 };



  /**
   * The integer value 115 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_115 = new byte [] { 0x73 };



  /**
   * The integer value 116 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_116 = new byte [] { 0x74 };



  /**
   * The integer value 117 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_117 = new byte [] { 0x75 };



  /**
   * The integer value 118 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_118 = new byte [] { 0x76 };



  /**
   * The integer value 119 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_119 = new byte [] { 0x77 };



  /**
   * The integer value 120 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_120 = new byte [] { 0x78 };



  /**
   * The integer value 121 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_121 = new byte [] { 0x79 };



  /**
   * The integer value 122 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_122 = new byte [] { 0x7A };



  /**
   * The integer value 123 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_123 = new byte [] { 0x7B };



  /**
   * The integer value 124 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_124 = new byte [] { 0x7C };



  /**
   * The integer value 125 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_125 = new byte [] { 0x7D };



  /**
   * The integer value 126 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_126 = new byte [] { 0x7E };



  /**
   * The integer value 127 encoded as a byte array in the appropriate ASN.1
   * format.
   */
  public static final byte[] INT_VALUE_127 = new byte [] { 0x7F };



  // The Java int value that corresponds to the value of this ASN.1 integer
  // element
  int intValue;



  /**
   * Creates a new ASN.1 integer element with the specified value.
   *
   * @param  intValue  The Java int value to use in creating this ASN.1 integer
   *                   element.
   */
  public ASN1Integer(int intValue)
  {
    this(ASN1_INTEGER_TYPE, intValue);
  }



  /**
   * Creates a new ASN.1 integer element with the specified type and value.
   *
   * @param  type      The type to use for this ASN.1 integer value.
   * @param  intValue  The Java int value to use in creating this ASN.1 integer
   *                   element.
   */
  public ASN1Integer(byte type, int intValue)
  {
    super(type);

    setValue(encodeIntValue(intValue));
    this.intValue = intValue;
  }



  /**
   * Encodes the provided int value in the appropriate manner for an ASN.1
   * integer value.
   *
   * @param  intValue  The Java int value to encode as an ASN.1 integer value.
   *
   * @return  A byte array that contains the encoded integer value.
   */
  public static byte[] encodeIntValue(int intValue)
  {
    // First, see if the int value is within the first 127 values.  If so, then
    // just return the pre-encoded version.
    switch (intValue)
    {
      case 0:   return INT_VALUE_0;
      case 1:   return INT_VALUE_1;
      case 2:   return INT_VALUE_2;
      case 3:   return INT_VALUE_3;
      case 4:   return INT_VALUE_4;
      case 5:   return INT_VALUE_5;
      case 6:   return INT_VALUE_6;
      case 7:   return INT_VALUE_7;
      case 8:   return INT_VALUE_8;
      case 9:   return INT_VALUE_9;
      case 10:  return INT_VALUE_10;
      case 11:  return INT_VALUE_11;
      case 12:  return INT_VALUE_12;
      case 13:  return INT_VALUE_13;
      case 14:  return INT_VALUE_14;
      case 15:  return INT_VALUE_15;
      case 16:  return INT_VALUE_16;
      case 17:  return INT_VALUE_17;
      case 18:  return INT_VALUE_18;
      case 19:  return INT_VALUE_19;
      case 20:  return INT_VALUE_20;
      case 21:  return INT_VALUE_21;
      case 22:  return INT_VALUE_22;
      case 23:  return INT_VALUE_23;
      case 24:  return INT_VALUE_24;
      case 25:  return INT_VALUE_25;
      case 26:  return INT_VALUE_26;
      case 27:  return INT_VALUE_27;
      case 28:  return INT_VALUE_28;
      case 29:  return INT_VALUE_29;
      case 30:  return INT_VALUE_30;
      case 31:  return INT_VALUE_31;
      case 32:  return INT_VALUE_32;
      case 33:  return INT_VALUE_33;
      case 34:  return INT_VALUE_34;
      case 35:  return INT_VALUE_35;
      case 36:  return INT_VALUE_36;
      case 37:  return INT_VALUE_37;
      case 38:  return INT_VALUE_38;
      case 39:  return INT_VALUE_39;
      case 40:  return INT_VALUE_40;
      case 41:  return INT_VALUE_41;
      case 42:  return INT_VALUE_42;
      case 43:  return INT_VALUE_43;
      case 44:  return INT_VALUE_44;
      case 45:  return INT_VALUE_45;
      case 46:  return INT_VALUE_46;
      case 47:  return INT_VALUE_47;
      case 48:  return INT_VALUE_48;
      case 49:  return INT_VALUE_49;
      case 50:  return INT_VALUE_50;
      case 51:  return INT_VALUE_51;
      case 52:  return INT_VALUE_52;
      case 53:  return INT_VALUE_53;
      case 54:  return INT_VALUE_54;
      case 55:  return INT_VALUE_55;
      case 56:  return INT_VALUE_56;
      case 57:  return INT_VALUE_57;
      case 58:  return INT_VALUE_58;
      case 59:  return INT_VALUE_59;
      case 60:  return INT_VALUE_60;
      case 61:  return INT_VALUE_61;
      case 62:  return INT_VALUE_62;
      case 63:  return INT_VALUE_63;
      case 64:  return INT_VALUE_64;
      case 65:  return INT_VALUE_65;
      case 66:  return INT_VALUE_66;
      case 67:  return INT_VALUE_67;
      case 68:  return INT_VALUE_68;
      case 69:  return INT_VALUE_69;
      case 70:  return INT_VALUE_70;
      case 71:  return INT_VALUE_71;
      case 72:  return INT_VALUE_72;
      case 73:  return INT_VALUE_73;
      case 74:  return INT_VALUE_74;
      case 75:  return INT_VALUE_75;
      case 76:  return INT_VALUE_76;
      case 77:  return INT_VALUE_77;
      case 78:  return INT_VALUE_78;
      case 79:  return INT_VALUE_79;
      case 80:  return INT_VALUE_80;
      case 81:  return INT_VALUE_81;
      case 82:  return INT_VALUE_82;
      case 83:  return INT_VALUE_83;
      case 84:  return INT_VALUE_84;
      case 85:  return INT_VALUE_85;
      case 86:  return INT_VALUE_86;
      case 87:  return INT_VALUE_87;
      case 88:  return INT_VALUE_88;
      case 89:  return INT_VALUE_89;
      case 90:  return INT_VALUE_90;
      case 91:  return INT_VALUE_91;
      case 92:  return INT_VALUE_92;
      case 93:  return INT_VALUE_93;
      case 94:  return INT_VALUE_94;
      case 95:  return INT_VALUE_95;
      case 96:  return INT_VALUE_96;
      case 97:  return INT_VALUE_97;
      case 98:  return INT_VALUE_98;
      case 99:  return INT_VALUE_99;
      case 100: return INT_VALUE_100;
      case 101: return INT_VALUE_101;
      case 102: return INT_VALUE_102;
      case 103: return INT_VALUE_103;
      case 104: return INT_VALUE_104;
      case 105: return INT_VALUE_105;
      case 106: return INT_VALUE_106;
      case 107: return INT_VALUE_107;
      case 108: return INT_VALUE_108;
      case 109: return INT_VALUE_109;
      case 110: return INT_VALUE_110;
      case 111: return INT_VALUE_111;
      case 112: return INT_VALUE_112;
      case 113: return INT_VALUE_113;
      case 114: return INT_VALUE_114;
      case 115: return INT_VALUE_115;
      case 116: return INT_VALUE_116;
      case 117: return INT_VALUE_117;
      case 118: return INT_VALUE_118;
      case 119: return INT_VALUE_119;
      case 120: return INT_VALUE_120;
      case 121: return INT_VALUE_121;
      case 122: return INT_VALUE_122;
      case 123: return INT_VALUE_123;
      case 124: return INT_VALUE_124;
      case 125: return INT_VALUE_125;
      case 126: return INT_VALUE_126;
      case 127: return INT_VALUE_127;
    }


    // It's greater than 127, so do it the "long" way.
    if ((intValue & 0xFF800000) != 0)
    {
      byte[] returnValue = new byte[4];
      returnValue[0] = (byte) ((intValue & 0xFF000000) >>> 24);
      returnValue[1] = (byte) ((intValue & 0x00FF0000) >>> 16);
      returnValue[2] = (byte) ((intValue & 0x0000FF00) >>> 8);
      returnValue[3] = (byte) (intValue & 0x000000FF);
      return returnValue;
    }
    else if ((intValue & 0x00FF8000) != 0)
    {
      byte[] returnValue = new byte[3];
      returnValue[0] = (byte) ((intValue & 0x00FF0000) >>> 16);
      returnValue[1] = (byte) ((intValue & 0x0000FF00) >>> 8);
      returnValue[2] = (byte) (intValue & 0x000000FF);
      return returnValue;
    }
    else
    {
      byte[] returnValue = new byte[2];
      returnValue[0] = (byte) ((intValue & 0x0000FF00) >>> 8);
      returnValue[1] = (byte) (intValue & 0x000000FF);
      return returnValue;
    }
  }



  /**
   * This method converts the provided byte array into a Java int.
   *
   * @param  encodedValue  The byte array containing the value to decode as an
   *                       integer.
   *
   * @return  The Java int decoded from the provided byte array.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be converted into
   *                         a Java int.
   */
  public static int decodeIntValue(byte[] encodedValue)
         throws ASN1Exception
  {
    if ((encodedValue == null) || (encodedValue.length == 0))
    {
      throw new ASN1Exception("No value to decode");
    }

    int value = 0x00000000;
    switch (encodedValue.length)
    {
      case 1:   value |= (0x000000FF & encodedValue[0]);
                break;
      case 2:   value |= ((0x000000FF & encodedValue[0]) << 8) |
                         (0x000000FF & encodedValue[1]);
                break;
      case 3:   value |= ((0x000000FF & encodedValue[0]) << 16) |
                         ((0x000000FF & encodedValue[1]) << 8) |
                         (0x000000FF & encodedValue[2]);
                break;
      case 4:   value |= ((0x000000FF & encodedValue[0]) << 24) |
                         ((0x000000FF & encodedValue[1]) << 16) |
                         ((0x000000FF & encodedValue[2]) << 8) |
                         (0x000000FF & encodedValue[3]);
                break;
      default:  throw new ASN1Exception("The provided value cannot be " +
                                        "represented as a Java int");
    }

    return value;
  }



  /**
   * Retrieves the Java int that corresponds to the value of this ASN.1 integer
   * value.
   *
   * @return  The Java int that corresponds to the value of this ASN.1 integer
   *          value.
   */
  public int getIntValue()
  {
    return intValue;
  }



  /**
   * Decodes the provided byte array as an ASN.1 Integer element.
   *
   * @param  encodedValue  The encoded ASN.1 element.
   *
   * @return  The decoded ASN.1 Integer element.
   *
   * @throws  ASN1Exception  If the provided byte array cannot be decoded as an
   *                         ASN.1 integer element.
   */
  public static ASN1Integer decodeAsInteger(byte[] encodedValue)
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
    int intValue = decodeIntValue(value);

    return new ASN1Integer(type, intValue);
  }
}

