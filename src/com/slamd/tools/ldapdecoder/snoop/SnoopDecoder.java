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
package com.slamd.tools.ldapdecoder.snoop;



import java.io.IOException;
import java.io.InputStream;



/**
 * This class defines a utility for decoding Snoop version 2 capture files as
 * defined in RFC 1761, such as the Solaris snoop utility.
 *
 *
 * @author   Neil A. Wilson
 */
public class SnoopDecoder
{
  /**
  * The magic bytes that must appear at the beginning of a snoop capture file.
  * They spell the word "snoop" followed by three null bytes.
  */
  public static final byte[] SNOOP_HEADER_BYTES =
       new byte[] { 0x73, 0x6e, 0x6f, 0x6f, 0x70, 0x00, 0x00, 0x00 };



  /**
   * The data link type that indicates the capture was obtained from an IEEE
   * 802.3 data link.
   */
  public static final int DATA_LINK_TYPE_IEEE_802_3 = 0;



  /**
   * The data link type that indicates the capture was obtained from an IEEE
   * 802.4 token bus data link.
   */
  public static final int DATA_LINK_TYPE_IEEE_802_4 = 1;



  /**
   * The data link type that indicates the capture was obtained from an IEEE
   * 802.5 token ring data link.
   */
  public static final int DATA_LINK_TYPE_IEEE_802_5 = 2;



  /**
   * The data link type that indicates the capture was obtained from an IEEE
   * 802.6 metro net data link.
   */
  public static final int DATA_LINK_TYPE_IEEE_802_6 = 3;



  /**
   * The data link type that indicates the capture was obtained from an
   * Ethernet data link.
   */
  public static final int DATA_LINK_TYPE_ETHERNET = 4;



  /**
   * The data link type that indicates the capture was obtained from an HDLC
   * data link.
   */
  public static final int DATA_LINK_TYPE_HDLC = 5;



  /**
   * The data link type that indicates the capture was obtained from a
   * character synchronous data link.
   */
  public static final int DATA_LINK_TYPE_CHARACTER_SYNCHRONOUS = 6;



  /**
   * The data link type that indicates the capture was obtained from an IBM
   * channel-to-channel data link.
   */
  public static final int DATA_LINK_TYPE_CHANNEL_TO_CHANNEL = 7;



  /**
   * The data link type that indicates the capture was obtained from an FDDI
   * data link.
   */
  public static final int DATA_LINK_TYPE_FDDI = 8;



  /**
   * The data link type that indicates the capture was obtained from some other
   * kind of data link.
   */
  public static final int DATA_LINK_TYPE_OTHER = 9;



  // The input stream from which the snoop data will be read.
  private InputStream inputStream;

  // The datalink type from which this capture was obtained.
  private int dataLinkType;



  /**
   * Creates a new snoop decoder that will read data from the provided input
   * stream.  The snoop file header will be read from the input stream to
   * verify that it is a valid snoop v2 capture.
   *
   * @param  inputStream  The input stream from which the snoop data is to be
   *                      read.
   *
   * @throws  IOException     If a problem occurs while attempting to read
   *                          data from the provided input stream.
   * @throws  SnoopException  If a problem occurs while attempting to decode
   *                          the snoop file header.
   */
  public SnoopDecoder(InputStream inputStream)
         throws IOException, SnoopException
  {
    this.inputStream = inputStream;


    // The first eight bytes must be "snoop" followed by three null bytes.
    byte[] idBytes = readBytes(inputStream, 8);
    if (! byteArraysAreEqual(idBytes, SNOOP_HEADER_BYTES))
    {
      throw new SnoopException("Input does not begin with the appropriate " +
                               "snoop header");
    }


    // The next 4 bytes must contain an integer that specifies the snoop file
    // format.  This decoder only handles snoop version 2 files.
    int snoopVersion = byteArrayToInt(readBytes(inputStream, 4));
    if (snoopVersion != 2)
    {
      throw new SnoopException("Only snoop version 2 capture files are " +
                               "supported (detected snoop version " +
                               snoopVersion + " capture)");
    }


    // The next four bytes specify the data link type.
    dataLinkType = byteArrayToInt(readBytes(inputStream, 4));
    if ((dataLinkType < 0) || (dataLinkType >= 10))
    {
      throw new SnoopException("Invalid data link type (" + dataLinkType + ')');
    }
  }



  /**
   * Retrieves the data link type for this snoop capture.
   *
   * @return  The data link type for this snoop capture.
   */
  public int getDataLinkType()
  {
    return dataLinkType;
  }



  /**
   * Reads the next packet record from the input stream.
   *
   * @return  The next packet record from the input stream, or
   *          <CODE>null</CODE> if the end of the stream has been reached.
   *
   * @throws  IOException     If a problem occurs while reading data from the
   *                          input stream.
   * @throws  SnoopException  If a problem occurs while trying to decode the
   *                          packet record.
   */
  public SnoopPacketRecord nextPacketRecord()
         throws IOException, SnoopException
  {
    return SnoopPacketRecord.readPacketRecord(inputStream);
  }



  /**
   * Reads the specified number of bytes from the given input stream.
   *
   * @param  inputStream  The input stream from which to read the data.
   * @param  numBytes     The number of bytes to read from the input stream.
   *
   * @return  A byte array containing the requested number of bytes, or
   *          <CODE>null</CODE> if the end of the input stream has been
   *          reached.
   *
   * @throws  IOException  If a problem occurs while trying to read the
   *                       requested number of bytes.
   */
  public static byte[] readBytes(InputStream inputStream, int numBytes)
         throws IOException
  {
    byte[] returnArray = new byte[numBytes];
    int    bytesRead   = inputStream.read(returnArray);
    if (bytesRead < 0)
    {
      return null;
    }

    while (bytesRead < numBytes)
    {
      int moreBytesRead = inputStream.read(returnArray, bytesRead,
                                           (numBytes - bytesRead));
      if (moreBytesRead < 0)
      {
        return null;
      }

      bytesRead += moreBytesRead;
    }

    return returnArray;
  }



  /**
   * Converts the provided byte array to an integer.
   *
   * @param  byteArray  The byte array containing the data to convert to an
   *                    integer.
   *
   * @return  The decoded integer.
   */
  public static int byteArrayToInt(byte[] byteArray)
  {
    return byteArrayToInt(byteArray, 0, byteArray.length);
  }



  /**
   * Converts the specified data from the provided byte array to an integer.
   *
   * @param  byteArray  The byte array containing the data to convert to an
   *                    integer.
   * @param  startPos   The position in the byte array to start decoding.
   * @param  length     The number of bytes to decode.
   *
   * @return  The decoded integer.
   */
  public static int byteArrayToInt(byte[] byteArray, int startPos, int length)
  {
    int value = 0x00000000;
    switch (length)
    {
      case 1:   value |= (0x000000FF & byteArray[startPos]);
                break;
      case 2:   value |= ((0x000000FF & byteArray[startPos]) << 8) |
                         (0x000000FF & byteArray[startPos+1]);
                break;
      case 3:   value |= ((0x000000FF & byteArray[startPos]) << 16) |
                         ((0x000000FF & byteArray[startPos+1]) << 8) |
                         (0x000000FF & byteArray[startPos+2]);
                break;
      case 4:   value |= ((0x000000FF & byteArray[startPos]) << 24) |
                         ((0x000000FF & byteArray[startPos+1]) << 16) |
                         ((0x000000FF & byteArray[startPos+2]) << 8) |
                         (0x000000FF & byteArray[startPos+3]);
                break;
    }

    return value;
  }



  /**
   * Converts the provided byte array to a long.
   *
   * @param  byteArray  The byte array containing the data to convert to a long.
   *
   * @return  The decoded long.
   */
  public static long byteArrayToLong(byte[] byteArray)
  {
    return byteArrayToLong(byteArray, 0, byteArray.length);
  }



  /**
   * Converts the specified data from the provided byte array to a long.
   *
   * @param  byteArray  The byte array containing the data to convert to a long.
   * @param  startPos   The position in the byte array to start decoding.
   * @param  length     The number of bytes to decode.
   *
   * @return  The decoded long.
   */
  public static long byteArrayToLong(byte[] byteArray, int startPos, int length)
  {
    long value = 0x0000000000000000;
    switch (length)
    {
      case 1:   value |= (0xFFL & byteArray[startPos]);
                break;
      case 2:   value |= ((0xFFL & byteArray[startPos]) << 8) |
                         (0xFFL & byteArray[startPos+1]);
                break;
      case 3:   value |= ((0xFFL & byteArray[startPos]) << 16) |
                         ((0xFFL & byteArray[startPos+1]) << 8) |
                         (0xFFL & byteArray[startPos+2]);
                break;
      case 4:   value |= ((0xFFL & byteArray[startPos]) << 24) |
                         ((0xFFL & byteArray[startPos+1]) << 16) |
                         ((0xFFL & byteArray[startPos+2]) << 8) |
                         (0xFFL & byteArray[startPos+3]);
                break;
      case 5:   value |= ((0xFFL & byteArray[startPos]) << 32) |
                         ((0xFFL & byteArray[startPos+1]) << 24) |
                         ((0xFFL & byteArray[startPos+2]) << 16) |
                         ((0xFFL & byteArray[startPos+3]) << 8) |
                         (0xFFL & byteArray[startPos+4]);
                break;
      case 6:   value |= ((0xFFL & byteArray[startPos]) << 40) |
                         ((0xFFL & byteArray[startPos+1]) << 32) |
                         ((0xFFL & byteArray[startPos+2]) << 24) |
                         ((0xFFL & byteArray[startPos+3]) << 16) |
                         ((0xFFL & byteArray[startPos+4]) << 8) |
                         (0xFFL & byteArray[startPos+5]);
                break;
      case 7:   value |= ((0xFFL & byteArray[startPos]) << 48) |
                         ((0xFFL & byteArray[startPos+1]) << 40) |
                         ((0xFFL & byteArray[startPos+2]) << 32) |
                         ((0xFFL & byteArray[startPos+3]) << 24) |
                         ((0xFFL & byteArray[startPos+4]) << 16) |
                         ((0xFFL & byteArray[startPos+5]) << 8) |
                         (0xFFL & byteArray[startPos+6]);
                break;
      case 8:   value |= ((0xFFL & byteArray[startPos]) << 56) |
                         ((0xFFL & byteArray[startPos+1]) << 48) |
                         ((0xFFL & byteArray[startPos+2]) << 40) |
                         ((0xFFL & byteArray[startPos+3]) << 32) |
                         ((0xFFL & byteArray[startPos+4]) << 24) |
                         ((0xFFL & byteArray[startPos+5]) << 16) |
                         ((0xFFL & byteArray[startPos+6]) << 8) |
                         (0xFFL & byteArray[startPos+7]);
                break;
    }

    return value;
  }



  /**
   * Indicates whether the contents of the two byte arrays are equal.
   *
   * @param  array1  The first byte array to compare.
   * @param  array2  The second byte array to compare.
   *
   * @return  <CODE>true</CODE> if the byte arrays are equal, or
   *          <CODE>false</CODE> if not.
   */
  public static boolean byteArraysAreEqual(byte[] array1, byte[] array2)
  {
    if (array1.length != array2.length)
    {
      return false;
    }

    for (int i=0; i < array1.length; i++)
    {
      if (array1[i] != array2[i])
      {
        return false;
      }
    }

    return true;
  }
}

