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
 * This class defines a generic snoop packet record, which holds information
 * about a packet that has been captured.
 *
 *
 * @author   Neil A. Wilson
 */
public class SnoopPacketRecord
{
  // The actual data associated with this packet record.
  private byte[] packetData;

  // The original length of the packet that was captured.
  private int originalLength;

  // The total length of this packet record, including the 24-byte header and
  // any pad bytes that it may have.
  private int packetRecordLength;

  // The number of packets dropped by the packet capture software between the
  // first packet captured and this packet.
  private int cumulativeDrops;

  // The time this packet was captured, measured in seconds since January 1,
  // 1970.
  private long timestampSeconds;

  // The number of microseconds after the indicated timestamp in seconds that
  // the packet was captured.
  private long timestampMicroseconds;



  /**
   * Creates a new snoop packet record with the provided information.
   *
   * @param  originalLength         The original length of the data that was
   *                                captured.
   * @param  packetRecordLength     The total length of this packet record,
   *                                including the header and padding.
   * @param  cumulativeDrops        The total number of packets dropped since
   *                                the first packet captured.
   * @param  timestampSeconds       The time this packet was captured, measured
   *                                in seconds since January 1, 1970.
   * @param  timestampMicroseconds  The number of microseconds after the
   *                                indicated timestamp in seconds that the
   *                                packet was captured.
   * @param  packetData             The actual data of the packet that was
   *                                captured.
   */
  public SnoopPacketRecord(int originalLength, int packetRecordLength,
                           int cumulativeDrops, long timestampSeconds,
                           long timestampMicroseconds, byte[] packetData)
  {
    this.originalLength        = originalLength;
    this.packetRecordLength    = packetRecordLength;
    this.cumulativeDrops       = cumulativeDrops;
    this.timestampSeconds      = timestampSeconds;
    this.timestampMicroseconds = timestampMicroseconds;
    this.packetData            = packetData;
  }



  /**
   * Reads a snoop packet record from the provided input stream.
   *
   * @param  inputStream  The input stream from which to read the snoop packet
   *                      record.
   *
   * @return  The snoop packet record that was captured.
   *
   * @throws  IOException     If a problem occurs while reading data from the
   *                          provided input stream.
   * @throws  SnoopException  If a problem occurs while attempting to decode the
   *                          snoop data.
   */
  public static SnoopPacketRecord readPacketRecord(InputStream inputStream)
         throws IOException, SnoopException
  {
    // Read and decode the snoop packet record header.
    byte[] headerBytes = SnoopDecoder.readBytes(inputStream, 24);
    if (headerBytes == null)
    {
      return null;
    }

    int  originalLength     = SnoopDecoder.byteArrayToInt(headerBytes, 0, 4);
    int  includedLength     = SnoopDecoder.byteArrayToInt(headerBytes, 4, 4);
    int  packetRecordLength = SnoopDecoder.byteArrayToInt(headerBytes, 8, 4);
    int  cumulativeDrops    = SnoopDecoder.byteArrayToInt(headerBytes, 12, 4);
    long timestampSeconds   = SnoopDecoder.byteArrayToLong(headerBytes, 16, 4);
    long timestampMicros    = SnoopDecoder.byteArrayToLong(headerBytes, 20, 4);


    // Read the actual packet data.
    byte[] packetData = SnoopDecoder.readBytes(inputStream, includedLength);
    if (packetData == null)
    {
      throw new SnoopException("End of input stream reached before packet " +
                               "data could be read");
    }


    // Read any pad bytes that might have been included.
    int numPadBytes = packetRecordLength - includedLength - 24;
    if (numPadBytes < 0)
    {
      throw new SnoopException("Unable to determine number of pad bytes in " +
                               "the snoop packet record");
    }
    else if (numPadBytes > 0)
    {
      byte[] padBytes = SnoopDecoder.readBytes(inputStream, numPadBytes);
      if (padBytes == null)
      {
        throw new SnoopException("End of input stream reached before pad " +
                                 "bytes could be read");
      }
    }


    // Create and return the packet record.
    return new SnoopPacketRecord(originalLength, packetRecordLength,
                                 cumulativeDrops, timestampSeconds,
                                 timestampMicros, packetData);
  }



  /**
   * Retrieves the number of bytes in the original data packet.
   *
   * @return  The number of bytes in the original data packet.
   */
  public int getOriginalLength()
  {
    return originalLength;
  }



  /**
   * Retrieves the number of bytes actually captured from the data packet.
   *
   * @return  The number of bytes actually captured from the data packet.
   */
  public int getIncludedLength()
  {
    return packetData.length;
  }



  /**
   * Indicates whether this packet is truncated (i.e., the amount of data
   * captured is less than the amount of data in the original packet).
   *
   * @return  <CODE>true</CODE> if this packet is truncated, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isTruncated()
  {
    return (originalLength != packetData.length);
  }



  /**
   * Retrieves the total number of bytes in this packet record, including the
   * packet record header and any pad bytes.
   *
   * @return  The total number of bytes in this packet record.
   */
  public int getPacketRecordLength()
  {
    return packetRecordLength;
  }



  /**
   * Retrieves the total number of packets that have been dropped between the
   * first packet captured and this packet.
   *
   * @return  The total number of packets that have been dropped between the
   *          first packet captured and this packet.
   */
  public int getCumulativeDrops()
  {
    return cumulativeDrops;
  }



  /**
   * Retrieves the time this packet was captured, measured in number of seconds
   * since January 1, 1970.
   *
   * @return  The time this packet was captured, measured in number of seconds
   *          since January 1, 1970.
   */
  public long getTimestampSeconds()
  {
    return timestampSeconds;
  }



  /**
   * Retrieves the number of microseconds after the specified number of seconds
   * that this packet was captured.
   *
   * @return  The number of microseconds after the specified number of seconds
   *          that this packet was captured.
   */
  public long getTimestampMicroseconds()
  {
    return timestampMicroseconds;
  }



  /**
   * Retrieves the actual data contained in the packet.
   *
   * @return  The actual data contained in the packet.
   */
  public byte[] getPacketData()
  {
    return packetData;
  }



  /**
   * Retrieves the number of bytes of padding included at the end of this packet
   * record.
   *
   * @return  The number of bytes of padding included at the end of this packet
   *          record.
   */
  public int getPadBytes()
  {
    return (packetRecordLength - 24 - packetData.length);
  }
}

