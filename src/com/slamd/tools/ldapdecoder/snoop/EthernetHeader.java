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
 * This class defines a data structure for storing information about an Ethernet
 * header.
 *
 *
 * @author   Neil A. Wilson
 */
public class EthernetHeader
{
  // The MAC address of the system for which this Ethernet packet is destined.
  private byte[] destinationMAC;

  // The MAC address of the system from which this Ethernet packet came.
  private byte[] sourceMAC;

  // The type of traffic contained in this packet.
  private int ethertype;



  /**
   * Creates an Ethernet header data structure with the provided information.
   *
   * @param  destinationMAC  The MAC address of the system for which this
   *                         Ethernet packet is destined.
   * @param  sourceMAC       The MAC address of the system from which this
   *                         Ethernet packet was sent.
   * @param  ethertype       The type of traffic contained in this packet.
   */
  public EthernetHeader(byte[] destinationMAC, byte[] sourceMAC,
                        int ethertype)
  {
    this.destinationMAC = destinationMAC;
    this.sourceMAC      = sourceMAC;
    this.ethertype      = ethertype;
  }



  /**
   * Decodes an Ethernet header from the provided byte array.
   *
   * @param  packetBytes  The byte array containing the data to be decoded.
   * @param  offset       The position in the byte array at which to start
   *                      reading the Ethernet header.
   *
   * @return  The decoded Ethernet header.
   *
   * @throws  SnoopException  If a problem occurs while attempting to decode the
   *                          Ethernet header.
   */
  public static EthernetHeader decodeEthernetHeader(byte[] packetBytes,
                                                    int offset)
         throws SnoopException
  {
    if ((offset + 14) > packetBytes.length)
    {
      throw new SnoopException("Insufficient data available for an Ethernet " +
                               "packet header");
    }


    byte[] destinationMAC = new byte[6];
    byte[] sourceMAC      = new byte[6];
    System.arraycopy(packetBytes, offset, destinationMAC, 0, 6);
    System.arraycopy(packetBytes, offset+6, sourceMAC, 0, 6);


    int ethertype = (packetBytes[offset+12] << 8) | (packetBytes[offset+13]);
    return new EthernetHeader(destinationMAC, sourceMAC, ethertype);
  }



  /**
   * Retrieves the MAC address of the system to which this Ethernet packet was
   * sent.
   *
   * @return  The MAC address of the system to which this Ethernet packet was
   *          sent.
   */
  public byte[] getDestinationMAC()
  {
    return destinationMAC;
  }



  /**
   * Retrieves the MAC address of the system that sent this Ethernet packet.
   *
   * @return  The MAC address of the system that sent this Ethernet packet.
   */
  public byte[] getSourceMAC()
  {
    return sourceMAC;
  }



  /**
   * Retrieves the ethertype for this packet, which indicates the type of
   * information that it contains.  A list of defined ethertypes may be obtained
   * from IANA (http://www.iana.org/assignments/ethernet-numbers).
   *
   * @return  The ethertype for this packet.
   */
  public int getEthertype()
  {
    return ethertype;
  }



  /**
   * Retrieves the number of bytes contained in this Ethernet header.
   *
   * @return  The number of bytes contained in this Ethernet header.
   */
  public int getHeaderLength()
  {
    // There will always be 14 bytes in an Ethernet header.
    return 14;
  }
}

