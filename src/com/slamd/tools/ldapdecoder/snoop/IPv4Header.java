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
 * This class defines a data structure for storing information about an Internet
 * Protocol header as defined in RFC 791.
 *
 *
 * @author   Neil A. Wilson
 */
public class IPv4Header
{
  /**
   * The ethertype used in the Ethernet header to indicate that the packet
   * contains IPv4 data.
   */
  public static final int ETHERTYPE_IPV4 = 0x0800;



  // Indicates whether fragmented communication will be accepted.
  private boolean mayFragment;

  // Indicates whether this is the last fragment.
  private boolean lastFragment;

  // The specific IP protocol for this datagram.
  private byte protocol;

  // The time to live (TTL) for this packet.
  private byte timeToLive;

  // The byte defining the type of service bitmask.
  private byte typeOfService;

  // The option data associated with this IP datagram.
  private byte[] optionBytes;

  // The destination IP address for this IP packet.
  private int destinationIP;

  // The checksum for the IP header.
  private int headerChecksum;

  // The IP version for this header.
  private byte ipVersion;

  // The number of 32-bit words contained in the IP header.
  private int ipHeaderWords;

  // The source IP address for this IP packet.
  private int sourceIP;

  // The total length in bytes of this IP datagram, including the IP header and
  // data.
  private int totalLength;

  // The identification value for this IP datagram.
  private int ipID;

  // The fragment offset for this IP datagram.
  private int fragmentOffset;



  /**
   * Creates a new IPv4 header data structure with the provided information.
   *
   * @param  ipVersion       The IP version for this IP datagram.
   * @param  ipHeaderWords   The number of 32-bit words contained in this IP
   *                         header.
   * @param  typeOfService   The type of service for this IP datagram.
   * @param  totalLength     The total length in bytes of this IP packet,
   *                         including the header and data.
   * @param  ipID            The identifier for this IP datagram.
   * @param  mayFragment     Indicates whether packets may be fragmented.
   * @param  lastFragment    Indicates whether this is the last fragment in a
   *                         set.
   * @param  fragmentOffset  The offset in 64-bit words of this fragment in the
   *                         overall data stream.
   * @param  timeToLive      The time to live for this IP datagram.
   * @param  protocol        The specific IP protocol represented by this
   *                         packet.
   * @param  headerChecksum  The checksum for the IP header.
   * @param  sourceIP        The source IP address for this IP datagram.
   * @param  destinationIP   The destination IP address for this IP datagram.
   * @param  optionBytes     The set of data associated with any options that
   *                         may be contained in this IP header.
   */
  public IPv4Header(byte ipVersion, int ipHeaderWords, byte typeOfService,
                    int totalLength, int ipID, boolean mayFragment,
                    boolean lastFragment, int fragmentOffset, byte timeToLive,
                    byte protocol, int headerChecksum, int sourceIP,
                    int destinationIP, byte[] optionBytes)
  {
    this.ipVersion      = ipVersion;
    this.ipHeaderWords  = ipHeaderWords;
    this.typeOfService  = typeOfService;
    this.totalLength    = totalLength;
    this.ipID           = ipID;
    this.mayFragment    = mayFragment;
    this.lastFragment   = lastFragment;
    this.fragmentOffset = fragmentOffset;
    this.timeToLive     = timeToLive;
    this.protocol       = protocol;
    this.headerChecksum = headerChecksum;
    this.sourceIP       = sourceIP;
    this.destinationIP  = destinationIP;
    this.optionBytes    = optionBytes;
  }



  /**
   * Decodes data in the provided byte array as an IPv4 header.
   *
   * @param  headerBytes  The byte array containing the data to decode.
   * @param  offset       The position in the byte array at which the IPv4
   *                      header starts.
   *
   * @return  The decoded IPv4 header.
   *
   * @throws  SnoopException  If there is a problem decoding the IPv4 header.
   */
  public static IPv4Header decodeIPv4Header(byte[] headerBytes, int offset)
         throws SnoopException
  {
    // First, make sure that the provided array is long enough to hold the
    // minimum IPv4 header of 20 bytes.
    if ((offset+20) > headerBytes.length)
    {
      throw new SnoopException("There are not enough bytes in the provided " +
                               "array to hold the minimum IPv4 header length " +
                               "of 20 bytes");
    }


    byte ipVersion = (byte) ((headerBytes[offset] >>> 4) & 0xFF);
    if (ipVersion != 0x04)
    {
      throw new SnoopException("IP version is not 4");
    }


    int ipHeaderWords = (headerBytes[offset] & 0x0F);
    if ((offset + (ipHeaderWords*4)) > headerBytes.length)
    {
      throw new SnoopException("There are not enough bytes in the provided " +
                               "array to hold the indicated IPv4 header " +
                               "header length of " + (ipHeaderWords*4) +
                               " bytes");
    }


    byte typeOfService = headerBytes[offset+1];
    int  totalLength   = SnoopDecoder.byteArrayToInt(headerBytes, offset+2, 2);
    int  ipID          = SnoopDecoder.byteArrayToInt(headerBytes, offset+4, 2);

    int fragmentBits = SnoopDecoder.byteArrayToInt(headerBytes, offset+6, 2);
    boolean mayFragment    = ((fragmentBits & 0x4000) == 0x0000);
    boolean lastFragment   = ((fragmentBits & 0x2000) == 0x0000);
    int     fragmentOffset = (fragmentBits & 0x1FFF);

    byte timeToLive = headerBytes[offset+8];
    byte protocol   = headerBytes[offset+9];
    int headerChecksum = SnoopDecoder.byteArrayToInt(headerBytes, offset+10, 2);
    int sourceIP       = SnoopDecoder.byteArrayToInt(headerBytes, offset+12, 4);
    int destinationIP  = SnoopDecoder.byteArrayToInt(headerBytes, offset+16, 4);

    byte[] optionBytes;
    if (ipHeaderWords == 5)
    {
      optionBytes = new byte[0];
    }
    else
    {
      optionBytes = new byte[(ipHeaderWords-5) * 4];
      System.arraycopy(headerBytes, offset+20, optionBytes, 0,
                       optionBytes.length);
    }

    return new IPv4Header(ipVersion, ipHeaderWords, typeOfService, totalLength,
                          ipID, mayFragment, lastFragment, fragmentOffset,
                          timeToLive, protocol, headerChecksum, sourceIP,
                          destinationIP, optionBytes);
  }



  /**
   * Retrieves the IP protocol version specified in this datagram.  The version
   * will be encoded in the lower 4 bits of the returned byte.
   *
   * @return  The IP protocol version specified in this datagram.
   */
  public byte getIPVersion()
  {
    return ipVersion;
  }



  /**
   * Retrieves the length in bytes of the IPv4 header.
   *
   * @return  The length in bytes of the IPv4 header.
   */
  public int getIPHeaderLength()
  {
    return (ipHeaderWords * 4);
  }



  /**
   * Retrieves the byte containing the type of service flags for this IP
   * datagram.
   *
   * @return  The byte containing the type of service flags for this IP
   *          datagram.
   */
  public byte getTypeOfService()
  {
    return typeOfService;
  }



  /**
   * Retrieves the total length of this IPv4 datagram, including the header and
   * any data.
   *
   * @return  The total length of this IPv4 datagram, including the header and
   *          any data.
   */
  public int getIPDatagramLength()
  {
    return totalLength;
  }



  /**
   * Retrieves the identifier for this IPv4 datagram.  The identifier will be
   * encoded in the lower 16 bits of the returned int value.
   *
   * @return  The identifier for this IPv4 datagram.
   */
  public int getIPID()
  {
    return ipID;
  }



  /**
   * Indicates whether the originator will accept fragmented packets.
   *
   * @return  <CODE>true</CODE> if the originator will accept fragmented
   *          packets, or <CODE>false</CODE> if not.
   */
  public boolean mayFragment()
  {
    return mayFragment;
  }



  /**
   * Indicates whether this packet contains the last fragment in a fragmented
   * data stream.
   *
   * @return  <CODE>true</CODE> if this packet contains the last fragment in a
   *          fragmented data stream, or <CODE>false</CODE> if there are more
   *          fragments.
   */
  public boolean lastFragment()
  {
    return lastFragment;
  }



  /**
   * Retrieves the offset of this fragment in the overall data stream.  The
   * value will be encoded in the lower 13 bits of the returned int value, and
   * it specifies the number of 64-bit datagrams into the overall data stream
   * this IPv4 packet lies.
   *
   * @return  The offset of this fragment in the overall data stream.
   */
  public int getFragmentOffset()
  {
    return fragmentOffset;
  }



  /**
   * Retrieves the time to live (TTL) for this IP datagram.
   *
   * @return  The time to live for this IP datagram.
   */
  public byte getTimeToLive()
  {
    return timeToLive;
  }



  /**
   * Retrieves the protocol for this IP datagram.
   *
   * @return  The protocol for this IP datagram.
   */
  public byte getProtocol()
  {
    return protocol;
  }



  /**
   * Retrieves the checksum for this IP datagram.  The checksum will be encoded
   * in the lower 16 bits of the returned int value.
   *
   * @return  The checksum for this IP datagram.
   */
  public int getHeaderChecksum()
  {
    return headerChecksum;
  }



  /**
   * Retrieves the source IP address for this datagram, as a 32-bit value.
   *
   * @return  The source IP address for this datagram as a 32-bit value.
   */
  public int getSourceIP()
  {
    return sourceIP;
  }



  /**
   * Retrieves the destination IP address for this datagram, as a 32-bit value.
   *
   * @return  The destination IP address for this datagram as a 32-bit value.
   */
  public int getDestinationIP()
  {
    return destinationIP;
  }



  /**
   * Converts the provided 32-bit integer value to an IP address in dotted quad
   * format.
   *
   * @param  addressInt  The 32-bit integer value containing the encoded IP
   *                     address.
   *
   * @return  The String representation of the provided IP address.
   */
  public static String intToIPAddress(int addressInt)
  {
    StringBuilder buffer = new StringBuilder(16);
    buffer.append((addressInt >>> 24) & 0x000000FF).append('.').
           append((addressInt >>> 16) & 0x000000FF).append('.').
           append((addressInt >>> 8) & 0x000000FF).append('.').
           append(addressInt & 0x000000FF);

    return buffer.toString();
  }



  /**
   * Retrieves the encoded IP option data for this header.
   *
   * @return  The encoded IP option data for this header.
   */
  public byte[] getOptionBytes()
  {
    return optionBytes;
  }
}

