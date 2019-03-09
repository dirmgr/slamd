/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.tools.ldapdecoder.snoop;



import java.io.IOException;
import java.io.InputStream;



/**
 * This class defines a utility for decoding libpcap capture files such as those
 * generated by tcpdump.
 *
 *
 * @author   Neil A. Wilson
 */
public class TCPDumpDecoder
{
  /**
   * The 32-bit value that must appear at the beginning of a pcap capture file
   * using big endian byte ordering.
   */
  public static final int PCAP_MAGIC_NUMBER_BIG_ENDIAN = 0xa1b2c3d4;



  /**
   * The 32-bit value that must appear at the beginning of a pcap capture file
   * using little endian byte ordering.
   */
  public static final int PCAP_MAGIC_NUMBER_LITTLE_ENDIAN = 0xd4c3b2a1;



  /**
   * The data link type that indicates the capture was obtained from a BSD
   * loopback device.
   */
  public static final int DATA_LINK_TYPE_BSD_LOOPBACK = 0;



  /**
   * The data link type that indicates the capture was obtained from Ethernet or
   * a Linux loopback device.
   */
  public static final int DATA_LINK_TYPE_ETHERNET = 1;



  /**
   * The data link type that indicates the capture was obtained from an 802.5
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_802_5 = 6;



  /**
   * The data link type that indicates the capture was obtained from an ARCnet
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_ARCNET = 7;



  /**
   * The data link type that indicates the capture was obtained from a SLIP
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_SLIP = 8;



  /**
   * The data link type that indicates the capture was obtained from a PPP
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_PPP = 9;



  /**
   * The data link type that indicates the capture was obtained from a FDDI
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_FDDI = 10;



  /**
   * The data link type that indicates the capture was obtained from an LLC or
   * SNAP-encapsulated ATM datalink device.
   */
  public static final int DATA_LINK_TYPE_LLC = 100;



  /**
   * The data link type that indicates the capture was obtained from a raw IP
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_RAW_IP = 101;



  /**
   * The data link type that indicates the capture was obtained from a BSD SLIP
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_BSD_SLIP = 102;



  /**
   * The data link type that indicates the capture was obtained from a BSD PPP
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_BSD_PPP = 103;



  /**
   * The data link type that indicates the capture was obtained from an HDLC
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_HDLC = 104;



  /**
   * The data link type that indicates the capture was obtained from an 802.11
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_802_11 = 105;



  /**
   * The data link type that indicates the capture was obtained from a Linux
   * "cooked" capture datalink device.
   */
  public static final int DATA_LINK_TYPE_LINUX_COOKED_CAPTURE = 113;



  /**
   * The data link type that indicates the capture was obtained from a LocalTalk
   * datalink device.
   */
  public static final int DATA_LINK_TYPE_LOCALTALK = 114;



  // Indicates whether the data in this dump is big endian or little endian, as
  // the specification allows for either ordering.
  private boolean bigEndian;

  // The input stream from which the snoop data will be read.
  private InputStream inputStream;

  // The datalink type from which this capture was obtained.
  private int dataLinkType;



  /**
   * Creates a new TCPDump decoder that will read data from the provided input
   * stream.  The file header will be read from the input stream to verify that
   * it is a valid libpcap capture.
   *
   * @param  inputStream  The input stream from which the data is to be read.
   *
   * @throws  IOException     If a problem occurs while attempting to read
   *                          data from the provided input stream.
   * @throws  SnoopException  If a problem occurs while attempting to decode
   *                          the libpcap file header.
   */
  public TCPDumpDecoder(InputStream inputStream)
         throws IOException, SnoopException
  {
    this.inputStream = inputStream;


    // The first 32-bit word must be the pcap magic number.
    int magicNumber =
         SnoopDecoder.byteArrayToInt(SnoopDecoder.readBytes(inputStream, 4));
    if (magicNumber == PCAP_MAGIC_NUMBER_BIG_ENDIAN)
    {
      bigEndian = true;
    }
    else if (magicNumber == PCAP_MAGIC_NUMBER_LITTLE_ENDIAN)
    {
      bigEndian = false;
    }
    else
    {
      throw new SnoopException("Input does not begin with the appropriate " +
                               "libpcap magic number");
    }


    // The next 16-bit word must be the major version, and it must be 2.
    int majorVersion = byteArrayToInt(SnoopDecoder.readBytes(inputStream, 2),
                                      bigEndian);
    if (majorVersion != 2)
    {
      throw new SnoopException("Only libpcap capture files with a major " +
                               "version of 2 are supported (detected major " +
                               "version " + majorVersion + ')');
    }


    // The next 16-bit word must be the minor version.  We don't really care
    // what it is, although it should be 4.
    int minorVersion = byteArrayToInt(SnoopDecoder.readBytes(inputStream, 2),
                                      bigEndian);


    // The next two 32-bit words are time zone and time stamp data which are
    // actually not used, so we'll ignore them.
    SnoopDecoder.readBytes(inputStream, 8);


    // The next 32-bit value is the snapshot length.  We just want to make sure
    // that it is nonzero for now.
    int snapshotLength = byteArrayToInt(SnoopDecoder.readBytes(inputStream, 4),
                                        bigEndian);
    if (snapshotLength == 0)
    {
      throw new SnoopException("Snapshot length must be nonzero");
    }


    // The last 32-bit value in the header is the datalink type.
    dataLinkType = byteArrayToInt(SnoopDecoder.readBytes(inputStream, 4),
                                  bigEndian);
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
  public TCPDumpPacketRecord nextPacketRecord()
         throws IOException, SnoopException
  {
    return TCPDumpPacketRecord.readPacketRecord(inputStream, bigEndian);
  }



  /**
   * Converts the provided byte array to an integer.
   *
   * @param  byteArray  The byte array containing the data to convert to an
   *                    integer.
   * @param  bigEndian  Indicates whether the data in the byte array uses big
   *                    endian or little endian decoding.
   *
   * @return  The decoded integer.
   */
  public static int byteArrayToInt(byte[] byteArray, boolean bigEndian)
  {
    return byteArrayToInt(byteArray, 0, byteArray.length, bigEndian);
  }



  /**
   * Converts the specified data from the provided byte array to an integer.
   *
   * @param  byteArray  The byte array containing the data to convert to an
   *                    integer.
   * @param  startPos   The position in the byte array to start decoding.
   * @param  length     The number of bytes to decode.
   * @param  bigEndian  Indicates whether the data in the byte array uses big
   *                    endian or little endian decoding.
   *
   * @return  The decoded integer.
   */
  public static int byteArrayToInt(byte[] byteArray, int startPos, int length,
                                   boolean bigEndian)
  {
    int value = 0x00000000;
    if (bigEndian)
    {
      switch (length)
      {
        case 1:   value |= (0xFF & byteArray[startPos]);
                  break;
        case 2:   value |= ((0xFF & byteArray[startPos]) << 8) |
                           (0xFF & byteArray[startPos+1]);
                  break;
        case 3:   value |= ((0xFF & byteArray[startPos]) << 16) |
                           ((0xFF & byteArray[startPos+1]) << 8) |
                           (0xFF & byteArray[startPos+2]);
                  break;
        case 4:   value |= ((0xFF & byteArray[startPos]) << 24) |
                           ((0xFF & byteArray[startPos+1]) << 16) |
                           ((0xFF & byteArray[startPos+2]) << 8) |
                           (0xFF & byteArray[startPos+3]);
                  break;
      }
    }
    else
    {
      switch (length)
      {
        case 1:   value |= (0xFF & byteArray[startPos]);
                  break;
        case 2:   value |= ((0xFF & byteArray[startPos+1]) << 8) |
                           (0xFF & byteArray[startPos]);
                  break;
        case 3:   value |= ((0xFF & byteArray[startPos+2]) << 16) |
                           ((0xFF & byteArray[startPos+1]) << 8) |
                           (0xFF & byteArray[startPos]);
                  break;
        case 4:   value |= ((0xFF & byteArray[startPos+3]) << 24) |
                           ((0xFF & byteArray[startPos+2]) << 16) |
                           ((0xFF & byteArray[startPos+1]) << 8) |
                           (0xFF & byteArray[startPos]);
                  break;
      }
    }

    return value;
  }



  /**
   * Converts the provided byte array to a long.
   *
   * @param  byteArray  The byte array containing the data to convert to a long.
   * @param  bigEndian  Indicates whether the data should be decoded in big
   *                    endian or little endian form.
   *
   * @return  The decoded long.
   */
  public static long byteArrayToLong(byte[] byteArray, boolean bigEndian)
  {
    return byteArrayToLong(byteArray, 0, byteArray.length, bigEndian);
  }



  /**
   * Converts the specified data from the provided byte array to a long.
   *
   * @param  byteArray  The byte array containing the data to convert to a long.
   * @param  startPos   The position in the byte array to start decoding.
   * @param  length     The number of bytes to decode.
   * @param  bigEndian  Indicates whether the data should be decoded in big
   *                    endian or little endian form.
   *
   * @return  The decoded long.
   */
  public static long byteArrayToLong(byte[] byteArray, int startPos, int length,
                                     boolean bigEndian)
  {
    long value = 0x0000000000000000;
    if (bigEndian)
    {
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
    }
    else
    {
      switch (length)
      {
        case 1:   value |= (0xFFL & byteArray[startPos]);
                  break;
        case 2:   value |= ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
        case 3:   value |= ((0xFFL & byteArray[startPos+2]) << 16) |
                           ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
        case 4:   value |= ((0xFFL & byteArray[startPos+3]) << 24) |
                           ((0xFFL & byteArray[startPos+2]) << 16) |
                           ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
        case 5:   value |= ((0xFFL & byteArray[startPos+4]) << 32) |
                           ((0xFFL & byteArray[startPos+3]) << 24) |
                           ((0xFFL & byteArray[startPos+2]) << 16) |
                           ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
        case 6:   value |= ((0xFFL & byteArray[startPos+5]) << 40) |
                           ((0xFFL & byteArray[startPos+4]) << 32) |
                           ((0xFFL & byteArray[startPos+3]) << 24) |
                           ((0xFFL & byteArray[startPos+2]) << 16) |
                           ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
        case 7:   value |= ((0xFFL & byteArray[startPos+6]) << 48) |
                           ((0xFFL & byteArray[startPos+5]) << 40) |
                           ((0xFFL & byteArray[startPos+4]) << 32) |
                           ((0xFFL & byteArray[startPos+3]) << 24) |
                           ((0xFFL & byteArray[startPos+2]) << 16) |
                           ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
        case 8:   value |= ((0xFFL & byteArray[startPos+7]) << 56) |
                           ((0xFFL & byteArray[startPos+6]) << 48) |
                           ((0xFFL & byteArray[startPos+5]) << 40) |
                           ((0xFFL & byteArray[startPos+4]) << 32) |
                           ((0xFFL & byteArray[startPos+3]) << 24) |
                           ((0xFFL & byteArray[startPos+2]) << 16) |
                           ((0xFFL & byteArray[startPos+1]) << 8) |
                           (0xFFL & byteArray[startPos]);
                  break;
      }
    }

    return value;
  }
}

