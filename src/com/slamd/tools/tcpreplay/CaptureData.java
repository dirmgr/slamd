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
package com.slamd.tools.tcpreplay;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;



/**
 * This class provides a data structure that may be used to hold data that has
 * been captured in a form that can be replayed to generate load against a
 * server.
 *
 *
 * @author   Neil A. Wilson
 */
public class CaptureData
{
  // The data that was actually captured.
  private byte[] data;

  // The version number of this capture.  The only currently-supported capture
  // version is 1.
  private int captureVersion;

  // The time that this data was captured, recorded in milliseconds since
  // January 1, 1970 (the format used by the System.currentTimeMillis() method.
  private long captureTime;



  /**
   * Creates a new capture data object with the provided information.
   *
   * @param  captureVersion  The capture version used.
   * @param  captureTime     The time that this data was captured.
   * @param  data            The actual data that was captured.
   */
  public CaptureData(int captureVersion, long captureTime, byte[] data)
  {
    this.captureVersion = captureVersion;
    this.captureTime    = captureTime;
    this.data           = data;
  }



  /**
   * Retrieves the capture version used for this capture.
   *
   * @return  The capture version used for this capture.
   */
  public int getCaptureVersion()
  {
    return captureVersion;
  }



  /**
   * Retrieves the time that the data was captured.
   *
   * @return  The time that the data was captured.
   */
  public long getCaptureTime()
  {
    return captureTime;
  }



  /**
   * Retrieves the data that was captured.
   *
   * @return  The data that was captured.
   */
  public byte[] getData()
  {
    return data;
  }



  /**
   * Reads an encoded form of this capture data from the provided input stream.
   *
   * @param  inputStream  The input stream from which to read the capture data
   *                      to decode.
   *
   * @return  The capture data decoded from the provided input stream, or
   *          <CODE>null</CODE> if there is no more data to read from the input
   *          stream.
   *
   * @throws  IOException  If a problem occurs while trying to read the data
   *                       from the input stream.
   *
   * @throws  CaptureException  If a problem occurs while attempting to decode
   *                            the data read from the input stream.
   */
  public static CaptureData decodeFrom(InputStream inputStream)
         throws IOException, CaptureException
  {
    // The first four bytes hold the capture version, which must be 1.
    int captureVersion = 0;
    for (int i=0; i < 4; i++)
    {
      int byteRead = inputStream.read();
      if (byteRead < 0)
      {
        return null;
      }

      captureVersion = (captureVersion << 8) | (byteRead & 0xFF);
    }

    if (captureVersion != 1)
    {
      throw new CaptureException("The capture version must be 1, but a value " +
                                 "of " + captureVersion + " was read.");
    }


    // The next eight bytes hold the timestamp.
    long captureTime = 0;
    for (int i=0; i < 8; i++)
    {
      captureTime= (captureTime << 8) | (inputStream.read() & 0xFF);
    }


    // The next four bytes hold the number of bytes in the capture.
    int captureLength = 0;
    for (int i=0; i < 4; i++)
    {
      captureLength = (captureLength << 8) | (inputStream.read() & 0xFF);
    }

    if ((captureLength < 0) || (captureLength > (1024*1024)))
    {
      throw new CaptureException("The capture length must not be greater " +
                                 "than one megabyte (decoded a length of " +
                                 captureLength + ").");
    }


    // Read the specified number of bytes.
    byte[] data = new byte[captureLength];
    int totalBytesRead = 0;
    while (totalBytesRead < captureLength)
    {
      int bytesRead = inputStream.read(data, totalBytesRead,
                                       (captureLength - totalBytesRead));
      if (bytesRead < 0)
      {
        throw new IOException("The input stream was unexpectedly closed " +
                              "before all data could be read.");
      }

      totalBytesRead += bytesRead;
    }


    // Create and return the capture data.
    return new CaptureData(captureVersion, captureTime, data);
  }



  /**
   * Encodes this capture data and writes it to the provided output stream.
   *
   * @param  outputStream  The output stream to which the encoded data should be
   *                       written.
   *
   * @throws  IOException  If a problem occurs while writing the data to the
   *                       provided output stream.
   */
  public void encodeTo(OutputStream outputStream)
    throws IOException
  {
    // First, four bytes will be used to encode the capture version, which is 1.
    outputStream.write((byte) 0x00);
    outputStream.write((byte) 0x00);
    outputStream.write((byte) 0x00);
    outputStream.write((byte) 0x01);


    // Next, use eight bytes for the timestamp.
    outputStream.write((byte) ((captureTime >> 56) & 0xFF));
    outputStream.write((byte) ((captureTime >> 48) & 0xFF));
    outputStream.write((byte) ((captureTime >> 40) & 0xFF));
    outputStream.write((byte) ((captureTime >> 32) & 0xFF));
    outputStream.write((byte) ((captureTime >> 24) & 0xFF));
    outputStream.write((byte) ((captureTime >> 16) & 0xFF));
    outputStream.write((byte) ((captureTime >> 8) & 0xFF));
    outputStream.write((byte) (captureTime & 0xFF));


    // Next, use four bytes for the number of bytes in the capture.
    outputStream.write((byte) ((data.length >> 24) & 0xFF));
    outputStream.write((byte) ((data.length >> 16) & 0xFF));
    outputStream.write((byte) ((data.length >> 8) & 0xFF));
    outputStream.write((byte) (data.length & 0xFF));


    // Finally, write the actual data.
    outputStream.write(data);
  }



  /**
   * Replays the captured data to the provided output stream.
   *
   * @param  outputStream  The output stream to which the data should be
   *                       written.
   *
   * @throws  IOException  If a problem occurs while writing the data to the
   *                       provided output stream.
   */
  public void replayTo(OutputStream outputStream)
         throws IOException
  {
    outputStream.write(data);
  }



  /**
   * Replays the captured data to the provided socket channel.
   *
   * @param  socketChannel  The socket channel to which the data should be
   *                        written.
   *
   * @throws  IOException  If a problem occurs while writing the data to the
   *                       provided socket channel.
   */
  public void replayTo(SocketChannel socketChannel)
         throws IOException
  {
    ByteBuffer buffer = ByteBuffer.wrap(data);

    int bytesWritten = socketChannel.write(buffer);
    while (bytesWritten < data.length)
    {
      bytesWritten += socketChannel.write(buffer);
    }
  }
}

