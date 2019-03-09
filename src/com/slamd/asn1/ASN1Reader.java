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
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;



/**
 * This class provides a mechanism for reading ASN.1 elements from an input
 * stream.  It is not a true <CODE>java.io.Reader</CODE>, but it should be good
 * enough for most purposes.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Reader
{
  /**
   * The size of the buffer that will be used for reading in data.
   */
  public static final int BUFFER_SIZE = 4 * 1024;


  // The input stream from which the ASN.1 elements will be read.
  InputStream inputStream;

  // The buffer that will be used to read data from the input stream.
  byte[] defaultBuffer;

  // The buffer that will be used to hold a temporary copy of buffer data.
  byte[] tmpBuffer;

  // The buffer that will be used to read additional information from the
  // input stream when the current data available is not enough.
  byte[] readMoreBuffer;

  // The buffer that will be used to hold data that has been read in but is not
  // yet needed (e.g., when multiple ASN.1 elements are sent at the same time).
  byte[] overflowBuffer;

  // The socket that provided the input stream from which the data is being
  // read.
  Socket socket;



  /**
   * Creates a new ASN.1 reader to read elements from the specified input
   * stream.
   *
   * @param  inputStream  The input stream from which ASN.1 elements are to be
   *                      read.
   */
  public ASN1Reader(InputStream inputStream)
  {
    this.defaultBuffer  = new byte[BUFFER_SIZE];
    this.tmpBuffer      = new byte[BUFFER_SIZE];
    this.readMoreBuffer = new byte[BUFFER_SIZE];
    this.inputStream    = inputStream;
    overflowBuffer      = ASN1Element.EMPTY_BYTES;
    socket              = null;
  }



  /**
   * Creates a new ASN.1 reader to read elements from the input stream of the
   * provided socket.
   *
   * @param  socket  The network socket from which the ASN.1 elements are to be
   *                 read.
   *
   * @throws  IOException  If the socket's input stream cannot be obtained.
   */
  public ASN1Reader(Socket socket)
         throws IOException
  {
    this.defaultBuffer  = new byte[BUFFER_SIZE];
    this.tmpBuffer      = new byte[BUFFER_SIZE];
    this.readMoreBuffer = new byte[BUFFER_SIZE];
    this.inputStream    = socket.getInputStream();
    this.socket         = socket;
    overflowBuffer      = ASN1Element.EMPTY_BYTES;
  }



  /**
   * Retrieves the number of bytes that can be read without blocking.
   *
   * @return  The number of bytes that can be read without blocking.
   *
   * @throws  IOException  If a problem occurs while trying to make the
   *                       determination.
   */
  public int available()
         throws IOException
  {
    return inputStream.available();
  }



  /**
   * Reads an ASN.1 element from the provided input stream.
   *
   * @return  The ASN.1 element that was read, or <CODE>null</CODE> if there was
   *          no more data to read.
   *
   * @throws  ASN1Exception  If the data read from the input stream does not
   *                         contain a valid ASN.1 element.
   *
   * @throws  IOException  If there is a problem reading the information from
   *                       the input stream.
   */
  public ASN1Element readElement()
         throws ASN1Exception, IOException
  {
    try
    {
      // Create a buffer to hold the information we will read in.
      byte[] buffer;
      int bytesRead;


      // First, see if we have information in the overflow buffer.  If so, then
      // use it.  If not, then read from the input stream.
      if (overflowBuffer.length > 0)
      {
        buffer = overflowBuffer;
        bytesRead = overflowBuffer.length;
        overflowBuffer = ASN1Element.EMPTY_BYTES;
      }
      else
      {
        buffer = defaultBuffer;
        bytesRead = inputStream.read(buffer);

        if (bytesRead < 0)
        {
          return null;
        }
      }


      // Make sure that there are at least two bytes to read from the stream.
      // If not, then we can't have a valid ASN.1 element.
      if (bytesRead < 2)
      {
        byte[] moreData = readMore();
        byte[] newBuffer = new byte[bytesRead + moreData.length];
        System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
        System.arraycopy(moreData, 0, newBuffer, bytesRead, moreData.length);
        buffer = newBuffer;
        bytesRead = newBuffer.length;
      }


      // Read the type.  Make sure that it is only a single byte
      byte type = buffer[0];
      if ((type & 0x1F) == 0x1F)
      {
        throw new ASN1Exception("Multibyte type detected (not supported in " +
                                "this package)");
      }


      // Read the first byte of the length and see if there should be more
      int length = buffer[1];
      int valueStartPos  = 2;
      if (valueStartPos >= bytesRead)
      {
        byte[] moreData = readMore();
        byte[] newBuffer = new byte[bytesRead + moreData.length];
        System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
        System.arraycopy(moreData, 0, newBuffer, bytesRead, moreData.length);
        buffer = newBuffer;
        bytesRead = newBuffer.length;
      }

      if ((length & 0x7F) == 0x00)
      {
        length = 128;
      }
      else if ((length & 0x7F) != length)
      {
        // This is a multibyte length.  Find the actual length
        int numLengthBytes = (length & 0x7F);
        valueStartPos += numLengthBytes;
        if (valueStartPos >= bytesRead)
        {
          byte[] moreData = readMore();
          byte[] newBuffer = new byte[bytesRead + moreData.length];
          System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
          System.arraycopy(moreData, 0, newBuffer, bytesRead, moreData.length);
          buffer = newBuffer;
          bytesRead = newBuffer.length;
        }

        length = 0x00000000;
        switch (numLengthBytes)
        {
          case 1:   length |= (0x000000FF & buffer[2]);
                    break;
          case 2:   length |= ((0x000000FF & buffer[2]) << 8) |
                              (0x000000FF & buffer[3]);
                    break;
          case 3:   length |= ((0x000000FF & buffer[2]) << 16) |
                              ((0x000000FF & buffer[3]) << 8) |
                              (0x000000FF & buffer[4]);
                    break;
          case 4:   length |= ((0x000000FF & buffer[2]) << 24) |
                              ((0x000000FF & buffer[3]) << 16) |
                              ((0x000000FF & buffer[4]) << 8) |
                              (0x000000FF & buffer[5]);
                    break;
          default:  throw new ASN1Exception("Length cannot be represented as " +
                                            "a Java int");
          }
      }


      // See how much information we have available relative to the amount that
      // we need.
      if ((valueStartPos + length) == bytesRead)
      {
        overflowBuffer = ASN1Element.EMPTY_BYTES;
      }
      else if ((valueStartPos + length) < bytesRead)
      {
        // Oh, no, I've read too much.
        overflowBuffer = new byte[bytesRead - (valueStartPos + length)];
        System.arraycopy(buffer, (valueStartPos+length), overflowBuffer, 0,
                         overflowBuffer.length);
        byte[] tempBuffer = new byte[valueStartPos + length];
        System.arraycopy(buffer, 0, tempBuffer, 0, (valueStartPos+length));
        buffer = tempBuffer;
        bytesRead = buffer.length;
      }
      else if ((valueStartPos + length) > bytesRead)
      {
        // I haven't read enough.
        while ((valueStartPos + length) > bytesRead)
        {
          byte[] tempReadBuffer = this.tmpBuffer;
          int tempBytesRead = inputStream.read(tempReadBuffer);
          byte[] tempBuffer = new byte[bytesRead+tempBytesRead];
          System.arraycopy(buffer, 0, tempBuffer, 0, bytesRead);
          System.arraycopy(tempReadBuffer, 0, tempBuffer, bytesRead,
                           tempBytesRead);
          buffer = tempBuffer;
          bytesRead += tempBytesRead;
        }

        // When reading in more data, we could have read too much, so stuff that
        // off into the overflow buffer.
        if ((valueStartPos + length) < bytesRead)
        {
          overflowBuffer = new byte[bytesRead - (valueStartPos + length)];
          System.arraycopy(buffer, (valueStartPos+length), overflowBuffer, 0,
                           overflowBuffer.length);
          byte[] tempBuffer = new byte[valueStartPos + length];
          System.arraycopy(buffer, 0, tempBuffer, 0, (valueStartPos+length));
          buffer = tempBuffer;
          bytesRead = buffer.length;
        }
        else
        {
          overflowBuffer = ASN1Element.EMPTY_BYTES;
        }
      }


      // Get the value of the ASN.1 element from the buffer.
      byte[] value = new byte[length];
      System.arraycopy(buffer, valueStartPos, value, 0, value.length);


      ASN1Element element = new ASN1Element(type, value);
      return element;
    }
    catch (Exception e)
    {
      if (e instanceof ASN1Exception)
      {
        throw (ASN1Exception) e;
      }
      else if (e instanceof IOException)
      {
        throw (IOException) e;
      }
      else
      {
        throw new ASN1Exception("Error while attempting to read an ASN.1 " +
                                "element:  " + e, e);
      }
    }
  }



  /**
   * Reads an ASN.1 element from the provided input stream, waiting a maximum of
   * <CODE>timeout</CODE> milliseconds for the response.  If no response has
   * been received during that time, then an <CODE>InterruptedIOException</CODE>
   * will be thrown.  Note that this is only applicable to ASN.1 readers that
   * were created using the version of the constructor that accepts a socket as
   * the argument.  If no socket has been provided, then no timeout will be
   * used.
   *
   * @param  timeout  The maximum length of time in milliseconds that the read
   *                  operation will be allowed to block while waiting for
   *                  information.
   *
   *
   * @return  The ASN.1 element that was read.
   *
   * @throws  ASN1Exception  If the data read from the input stream does not
   *                         contain a valid ASN.1 element.
   *
   * @throws  InterruptedIOException  If the read was interrupted because the
   *                                  timeout was reached.
   *
   * @throws  IOException  If there is a problem reading the information from
   *                       the input stream or setting a timeout on the socket.
   */
  public ASN1Element readElement(int timeout)
         throws ASN1Exception, InterruptedIOException, IOException
  {
    int originalTimeout = 0;

    if (socket != null)
    {
      originalTimeout = socket.getSoTimeout();
      socket.setSoTimeout(timeout);
    }

    ASN1Element returnElement = readElement();

    if (socket != null)
    {
      socket.setSoTimeout(originalTimeout);
    }

    return returnElement;
  }



  /**
   * Reads additional information from the input stream and returns it in a
   * byte array of exactly the right size.
   *
   * @return  The byte array containing the additional data read.
   *
   * @throws  IOException  If a problem occurs while reading from the input
   *                       stream.
   */
  private byte[] readMore()
          throws IOException
  {
    int moreBytesRead = inputStream.read(readMoreBuffer);
    byte[] returnArray = new byte[moreBytesRead];
    System.arraycopy(readMoreBuffer, 0, returnArray, 0, moreBytesRead);
    return returnArray;
  }



  /**
   * Closes this ASN.1 reader.
   *
   * @throws  IOException  If a problem occurs while closing the ASN.1 reader.
   */
  public void close()
         throws IOException
  {
    // No implementation necessary.
  }
}

