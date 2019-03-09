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
package com.slamd.tools.ldapdecoder;



/**
 * This class provides a data structure which holds information about the source
 * and destination addresses and ports for a connection.
 */
final class ConnectionIdentifier
{
  // The destination port number.
  private final int destinationPort;

  // The source port number.
  private final int sourcePort;

  // The destination IP address.
  private final String destinationIP;

  // The source IP address.
  private final String sourceIP;



  /**
   * Creates a new connection identifier with the following information.
   *
   * @param  sourceIP         The source IP address for the connection.
   * @param  sourcePort       The source port number for the connection.
   * @param  destinationIP    The destination IP address for the connection.
   * @param  destinationPort  The destination port number for the connection.
   */
  ConnectionIdentifier(final String sourceIP, final int sourcePort,
                       final String destinationIP, final int destinationPort)
  {
    this.sourceIP        = sourceIP;
    this.sourcePort      = sourcePort;
    this.destinationIP   = destinationIP;
    this.destinationPort = destinationPort;
  }



  /**
   * Retrieves the source IP address for this connection.
   *
   * @return  The source IP address for this connection.
   */
  String getSourceIP()
  {
    return sourceIP;
  }



  /**
   * Retrieves the source port number for this connection.
   *
   * @return  The source port number for this connection.
   */
  int getSourcePort()
  {
    return sourcePort;
  }



  /**
   * Retrieves the destination IP address for this connection.
   *
   * @return  The destination IP address for this connection.
   */
  String getDestinationIP()
  {
    return destinationIP;
  }



  /**
   * Retrieves the destination port number for this connection.
   *
   * @return  The destination port number for this connection.
   */
  int getDestinationPort()
  {
    return destinationPort;
  }



  /**
   * Retrieves a hash code for this object.
   *
   * @return  A hash code for this object.
   */
  @Override()
  public int hashCode()
  {
    return sourceIP.hashCode() + destinationIP.hashCode() + sourcePort +
           destinationPort;
  }



  /**
   * Indicates whether the provided object is equal to this connection
   * identifier.
   *
   * @param  o  The object for which to make the determination.
   *
   * @return  {@code true} if the provided object is equal to this connection
   *          identifier, or {@code false} if not.
   */
  @Override()
  public boolean equals(final Object o)
  {
    if (o == null)
    {
      return false;
    }
    if (o == this)
    {
      return true;
    }
    if (o instanceof ConnectionIdentifier)
    {
      final ConnectionIdentifier i = (ConnectionIdentifier) o;
      return (sourceIP.equals(i.sourceIP) && (sourcePort == i.sourcePort) &&
               destinationIP.equals(i.destinationIP) &&
               (destinationPort == i.destinationPort));
    }
    else
    {
      return false;
    }
  }



  /**
   * Retrieves a string representation of this connection identifier.
   *
   * @return  A string representation of this connection identifier.
   */
  @Override()
  public String toString()
  {
    return sourceIP + ':' + sourcePort + " to " + destinationIP + ':' +
         destinationPort;
  }
}
