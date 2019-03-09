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



/**
 * This class defines a data structure for storing information about the
 * Transmission Control Protocol (TCP) header as defined in RFC 793.
 *
 *
 * @author   Neil A. Wilson
 */
public class TCPHeader
{
  /**
   * The IP protocol value that indicates the data contains TCP information.
   */
  public static final byte IP_PROTOCOL_TCP = 0x06;



  /**
   * The bitmask that may be applied to the TCP flags to determine whether the
   * urgent pointer field should be considered significant.
   */
  public static final byte TCP_FLAG_MASK_URG = 0x20;



  /**
   * The bitmask that may be applied to the TCP flags to determine whether the
   * acknowledgement field should be considered significant.
   */
  public static final byte TCP_FLAG_MASK_ACK = 0x10;



  /**
   * The bitmask that may be applied to the TCP flags to determine whether the
   * push flag is set.
   */
  public static final byte TCP_FLAG_MASK_PSH = 0x08;



  /**
   * The bitmask that may be applied to the TCP flags to determine whether the
   * reset flag is set, indicating the forceful destruction of a connection.
   */
  public static final byte TCP_FLAG_MASK_RST = 0x04;



  /**
   * The bitmask that may be applied to the TCP flags to determine whether the
   * SYN flag is set, indicating a new connection.
   */
  public static final byte TCP_FLAG_MASK_SYN = 0x02;



  /**
   * The bitmask that may be applied to the TCP flags to determine whether the
   * FIN flag is set, indicating the connection is being closed.
   */
  public static final byte TCP_FLAG_MASK_FIN = 0x01;



  // Any data associated with options in this TCP header.
  private byte[] optionBytes;

  // The acknowledgement number for this TCP header.
  private int ackNumber;

  // The checksum for this TCP header.
  private int checksum;

  // The position of the end of this TCP header and the beginning of the data.
  private int dataOffset;

  // The destination port for this TCP header.
  private int destinationPort;

  // The sequence number for this TCP header.
  private int sequenceNumber;

  // The source port for this TCP header.
  private int sourcePort;

  // The set of flags for this TCP header.
  private int tcpFlags;

  // The urgent pointer for this TCP header.
  private int urgentPointer;

  // The window for this TCP header.
  private int window;



  /**
   * Creates a new TCP header with the provided information.
   *
   * @param  sourcePort       The source port for this TCP header.
   * @param  destinationPort  The destination port for this TCP header.
   * @param  sequenceNumber   The sequence number for this TCP header.
   * @param  ackNumber        The acknowledgement number for this TCP header.
   * @param  dataOffset       The data offset for this TCP header, measured in
   *                          32-bit words.
   * @param  tcpFlags         The set of flags associated with this TCP header.
   * @param  window           The window for this TCP header.
   * @param  checksum         The checksum for this TCP header.
   * @param  urgentPointer    The urgent pointer for this TCP header.
   * @param  optionBytes      The raw data associated with any options in this
   *                          TCP header.
   */
  public TCPHeader(int sourcePort, int destinationPort, int sequenceNumber,
                   int ackNumber, int dataOffset, int tcpFlags, int window,
                   int checksum, int urgentPointer, byte[] optionBytes)
  {
    this.sourcePort      = sourcePort;
    this.destinationPort = destinationPort;
    this.sequenceNumber  = sequenceNumber;
    this.ackNumber       = ackNumber;
    this.dataOffset      = dataOffset;
    this.tcpFlags        = tcpFlags;
    this.window          = window;
    this.checksum        = checksum;
    this.urgentPointer   = urgentPointer;
    this.optionBytes     = optionBytes;
  }



  /**
   * Decodes information in the provided byte array as a TCP header.
   *
   * @param  headerBytes  The byte array containing the header information to
   *                      decode.
   * @param  offset       The position in the byte array at which to begin
   *                      decoding.
   *
   * @return  The decoded TCP header.
   *
   * @throws  SnoopException  If a problem occurs while trying to decode the
   *                          TCP header.
   */
  public static TCPHeader decodeTCPHeader(byte[] headerBytes, int offset)
         throws SnoopException
  {
    // First, make sure the provided byte array is long enough to hold the
    // minimum TCP header length of 20 bytes.
    if ((offset+20) > headerBytes.length)
    {
      throw new SnoopException("Provided byte array is not large enough to " +
                               "hold the minimum TCP header size of 20 bytes.");
    }


    int sourcePort      = SnoopDecoder.byteArrayToInt(headerBytes, offset, 2);
    int destinationPort = SnoopDecoder.byteArrayToInt(headerBytes, offset+2, 2);
    int sequenceNumber  = SnoopDecoder.byteArrayToInt(headerBytes, offset+4, 4);
    int ackNumber       = SnoopDecoder.byteArrayToInt(headerBytes, offset+8, 4);


    int dataOffset = ((headerBytes[offset+12] >>> 4) & 0x0F);
    if ((offset+(dataOffset*4)) > headerBytes.length)
    {
      throw new SnoopException("Provided byte array is not large enough to " +
                               "hold the decoded TCP header size of " +
                               (dataOffset*4) + " bytes.");
    }


    int tcpFlags      = (headerBytes[offset+13] & 0x3F);
    int window        = SnoopDecoder.byteArrayToInt(headerBytes, offset+14, 2);
    int checksum      = SnoopDecoder.byteArrayToInt(headerBytes, offset+16, 2);
    int urgentPointer = SnoopDecoder.byteArrayToInt(headerBytes, offset+18, 2);


    byte[] optionBytes;
    if (dataOffset > 5)
    {
      optionBytes = new byte[(dataOffset - 5) * 4];
      System.arraycopy(headerBytes, offset+20, optionBytes, 0,
                       optionBytes.length);
    }
    else
    {
      optionBytes = new byte[0];
    }


    return new TCPHeader(sourcePort, destinationPort, sequenceNumber, ackNumber,
                         dataOffset, tcpFlags, window, checksum, urgentPointer,
                         optionBytes);
  }



  /**
   * Retrieves the source port for this TCP packet.  The port will be encoded in
   * the lower 16 bits of the returned int value.
   *
   * @return  The source port for this TCP packet.
   */
  public int getSourcePort()
  {
    return sourcePort;
  }



  /**
   * Retrieves the destination port for this TCP packet.  The port will be
   * encoded in the lower 16 bits of the returned int value.
   *
   * @return  The destination port for this TCP packet.
   */
  public int getDestinationPort()
  {
    return destinationPort;
  }



  /**
   * Retrieves the sequence number for this TCP packet.  The value will use all
   * 32 bits of the returned int value.
   *
   * @return  The sequence number for this TCP packet.
   */
  public int getSequenceNumber()
  {
    return sequenceNumber;
  }



  /**
   * Retrieves the acknowledgement number for this TCP packet.  The value will
   * use all 32 bits of the returned int value.
   *
   * @return  The acknowledgement number for this TCP packet.
   */
  public int getAcknowledgementNumber()
  {
    return ackNumber;
  }



  /**
   * Retrieves the data offset for this TCP header.  The value will be encoded
   * in the lower 4 bits of the provided value and will indicate the number of
   * 32-bit words contained in the header.
   *
   * @return  The data offset for this TCP header.
   */
  public int getDataOffset()
  {
    return dataOffset;
  }



  /**
   * Retrieves the length in bytes of this TCP header.
   *
   * @return  The length in bytes of this TCP header.
   */
  public int getHeaderLength()
  {
    return (dataOffset * 4);
  }



  /**
   * Retrieves the set of flags for this TCP header.  The flags will be encoded
   * in the lower 6 bits of the returned int value, and individual flags may
   * be checked by ANDing them with the values of the
   * <CODE>TCP_FLAG_MASK_*</CODE> constants.
   *
   * @return  The set of flags for this TCP header.
   */
  public int getTCPFlags()
  {
    return tcpFlags;
  }



  /**
   * Retrieves the window for this TCP header.  The value will be encoded in
   * the lower 16 bits of the returned int value.
   *
   * @return  The window for this TCP header.
   */
  public int getWindow()
  {
    return window;
  }



  /**
   * Retrieves the checksum for this TCP header.  The value will be encoded in
   * the lower 16 bits of the returned int value.
   *
   * @return  The checksum for this TCP header.
   */
  public int getChecksum()
  {
    return checksum;
  }



  /**
   * Retrieves the urgent pointer for this TCP header.  The window will be
   * encoded in the lower 16 bits of the returned int value.
   *
   * @return  The urgent pointer for this TCP header.
   */
  public int getUrgentPointer()
  {
    return urgentPointer;
  }



  /**
   * Retrieves the data for any options associated with this TCP header.
   *
   * @return  The data for any options associated with this TCP header.
   */
  public byte[] getOptionBytes()
  {
    return optionBytes;
  }
}

