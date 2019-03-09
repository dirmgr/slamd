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
public class TCPDumpPacketRecord
{
  // The actual data associated with this packet record.
  private byte[] packetData;

  // The original length of the packet that was captured.
  private int originalLength;

  // The time this packet was captured, measured in seconds since January 1,
  // 1970.
  private long timestampSeconds;

  // The number of microseconds after the indicated timestamp in seconds that
  // the packet was captured.
  private long timestampMicroseconds;



  /**
   * Creates a new TCPDump packet record with the provided information.
   *
   * @param  originalLength         The original length of the data that was
   *                                captured.
   * @param  timestampSeconds       The time this packet was captured, measured
   *                                in seconds since January 1, 1970.
   * @param  timestampMicroseconds  The number of microseconds after the
   *                                indicated timestamp in seconds that the
   *                                packet was captured.
   * @param  packetData             The actual data of the packet that was
   *                                captured.
   */
  public TCPDumpPacketRecord(int originalLength, long timestampSeconds,
                             long timestampMicroseconds, byte[] packetData)
  {
    this.originalLength        = originalLength;
    this.timestampSeconds      = timestampSeconds;
    this.timestampMicroseconds = timestampMicroseconds;
    this.packetData            = packetData;
  }



  /**
   * Reads a TCPDump packet record from the provided input stream.
   *
   * @param  inputStream  The input stream from which to read the packet record.
   * @param  bigEndian    Indicates whether the data in the capture file uses
   *                      big endian or little endian encoding.
   *
   * @return  The TCPDump packet record that was captured.
   *
   * @throws  IOException     If a problem occurs while reading data from the
   *                          provided input stream.
   * @throws  SnoopException  If a problem occurs while attempting to decode the
   *                          TCPDump data.
   */
  public static TCPDumpPacketRecord readPacketRecord(InputStream inputStream,
                                                     boolean bigEndian)
         throws IOException, SnoopException
  {
    // Read and decode the TCPDump packet record header, which will always be
    // 16 bytes.
    byte[] headerBytes = SnoopDecoder.readBytes(inputStream, 16);
    if (headerBytes == null)
    {
      return null;
    }

    long timestampSeconds = TCPDumpDecoder.byteArrayToLong(headerBytes, 0, 4,
                                                           bigEndian);
    long timestampMicros  = TCPDumpDecoder.byteArrayToLong(headerBytes, 4, 8,
                                                           bigEndian);
    int  includedLength   = TCPDumpDecoder.byteArrayToInt(headerBytes, 8, 4,
                                                          bigEndian);
    int  originalLength   = TCPDumpDecoder.byteArrayToInt(headerBytes, 12, 4,
                                                          bigEndian);


    // Read the actual packet data.
    byte[] packetData = SnoopDecoder.readBytes(inputStream, includedLength);
    if (packetData == null)
    {
      throw new SnoopException("End of input stream reached before packet " +
                               "data could be read");
    }


    // Create and return the packet record.
    return new TCPDumpPacketRecord(originalLength, timestampSeconds,
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
}

