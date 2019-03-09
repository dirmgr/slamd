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



import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;



/**
 * This class provides a mechanism for sending ASN.1 elements over an output
 * stream.  It is not a true <CODE>java.io.Writer</CODE>, but it should be good
 * enough for most purposes.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Writer
{
  // The output stream over which ASN.1 elements will be written.
  OutputStream outputStream;



  /**
   * Creates a new ASN.1 writer configured to use the specified output stream.
   *
   * @param  outputStream  The output stream over which the ASN.1 elements will
   *                       be written.
   */
  public ASN1Writer(OutputStream outputStream)
  {
    this.outputStream = outputStream;
  }



  /**
   * Creates a new ASN.1 writer configured to use the specified socket.
   *
   * @param  socket  The socket over which the ASN.1 elements will be written.
   *
   * @throws  IOException  If the output stream of the provided socket cannot be
   *                       obtained.
   */
  public ASN1Writer(Socket socket)
         throws IOException
  {
    this.outputStream = socket.getOutputStream();
  }



  /**
   * Writes the specified ASN.1 element over the output stream.
   *
   * @param  element  The element to be written over the output stream.
   *
   * @throws  IOException  If there is a problem writing the specified element
   *                       over the output stream.
   */
  public void writeElement(ASN1Element element)
         throws IOException
  {
    outputStream.write(element.encode());
    outputStream.flush();
  }



  /**
   * Causes the output stream to be flushed and any buffered data to be sent
   * immediately rather than waiting for the buffer to fill.
   *
   * @throws  IOException  If there is a problem flushing the output stream.
   */
  public void flush()
         throws IOException
  {
    outputStream.flush();
  }



  /**
   * Closes this writer so that it may no longer be used to send ASN.1 elements
   * over the specified output stream.
   *
   * @throws  IOException  If there is a problem encountered while closing the
   *                       writer.
   */
  public void close()
         throws IOException
  {
    // Make sure that any pending data is written immediately.
    flush();
  }
}

