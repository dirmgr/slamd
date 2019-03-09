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
package com.slamd.scripting.mail;



import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.slamd.asn1.ASN1Element;

import com.unboundid.util.Base64;



/**
 * This class provides a set of methods that can be used in the process of
 * authenticating to a server using CRAM-MD5 as defined in RFC 2195, which
 * itself is based on HMAC as defined in RFC 2104.
 *
 *
 * @author   Neil A. Wilson
 */
public class CRAMMD5Handler
{
  /**
   * The block length in characters used in generating an HMAC-MD5 digest.
   */
  public static final int BLOCK_LENGTH = 64;



  /**
   * The number of bytes contained in an MD5 digest.
   */
  public static final int MD5_DIGEST_LENGTH = 16;



  /**
   * The inner pad byte, which will be XORed with the shared secret for the
   * first digest.
   */
  public static final byte IPAD_BYTE = 0x36;



  /**
   * The outer pad byte, which will be XORed with the shared secret for the
   * second digest.
   */
  public static final byte OPAD_BYTE = 0x5c;



  // An array filled with the iPad byte.
  private byte[] iPadArray;

  // An array filled with the oPad byte.
  private byte[] oPadArray;

  // The message digest that will be used to create MD5 hashes.
  private MessageDigest md5Digest;



  /**
   * Provides a means of testing this CRAM-MD5 handler by generating a CRAM-MD5
   * response for the given information.  The data to use to generate the
   * response must be provided as arguments and the response will be written to
   * standard output.
   *
   * @param  args  The command line arguments provided to this program.  There
   *               must be exactly three arguments, and they must be the
   *               username, password, and challenge (in that order).
   */
  public static void main(String[] args)
  {
    if (args.length != 3)
    {
      System.err.println("ERROR:  There must be exactly 3 arguments " +
                         "(username, password, challenge)");
      System.exit(1);
    }


    try
    {
      CRAMMD5Handler crammer = new CRAMMD5Handler();
      System.out.println(crammer.generateCRAMMD5Response(args[0], args[1],
                                                         args[2]));
    }
    catch (Exception e)
    {
      System.err.println("Caught an exception during processing:");
      e.printStackTrace();
    }
  }



  /**
   * Creates a new instance of this CRAM-MD5 handler.
   *
   * @throws  NoSuchAlgorithmException  If a problem occurs while trying to
   *                                    initialize the MD5 digest handler.
   */
  public CRAMMD5Handler()
         throws NoSuchAlgorithmException
  {
    md5Digest = MessageDigest.getInstance("MD5");

    iPadArray = new byte[BLOCK_LENGTH];
    oPadArray = new byte[BLOCK_LENGTH];
    for (int i=0; i < BLOCK_LENGTH; i++)
    {
      iPadArray[i] = IPAD_BYTE;
      oPadArray[i] = OPAD_BYTE;
    }
  }



  /**
   * Generates the CRAM-MD5 response that should be used for the provided
   * information.
   *
   * @param  username   The username to use in the authentication process.
   * @param  password   The password to use in the authentication process.
   * @param  challenge  The challenge provided by the server to which
   *                    authentication is to be performed.
   *
   * @return  The string containing the CRAM-MD5 response.
   */
  public String generateCRAMMD5Response(String username, String password,
                                        String challenge)
  {
    // The resulting response will be the concatenation of the username, a
    // space, and a hex string representation of the HMAC-MD5 hash of the
    // password and the challenge.  First, create a string buffer long enough to
    // hold everything.
    StringBuilder buffer = new StringBuilder(username.length() + 1 +
                                           (2*MD5_DIGEST_LENGTH));


    // Next, append the username and the space.
    buffer.append(username);
    buffer.append(' ');


    // Finally, generate the HMAC-MD5 digest of the password and challenge,
    // convert it to a string, and return it.
    byte[] challengeBytes = ASN1Element.getBytes(challenge);
    byte[] pwBytes        = ASN1Element.getBytes(password);
    byte[] hmacMD5Bytes   = generateHMACMD5(challengeBytes, pwBytes);
    writeToHexString(hmacMD5Bytes, buffer);
    return Base64.encode(ASN1Element.getBytes(buffer.toString()));
  }



  /**
   * Generates an HMAC-MD5 response based on the provided key and data.
   *
   * @param  data  The plain-text data to include in the response.
   * @param  key   The secret key to use in the response.
   *
   * @return  A byte array containing the HMAC-MD5 response.
   */
  public byte[] generateHMACMD5(byte[] data, byte[] key)
  {
    // First, if the key is longer than BLOCK_LENGTH, then use the MD5 digest of
    // the key instead of the actual key.
    byte[] k;
    if (key.length > BLOCK_LENGTH)
    {
      k = md5Digest.digest(key);
    }
    else
    {
      k = key;
    }


    // Create byte arrays that will hold the data we need to use in this
    // process.  Place the appropriate data in each array.
    byte[] iPadAndData = new byte[BLOCK_LENGTH + data.length];
    System.arraycopy(iPadArray, 0, iPadAndData, 0, BLOCK_LENGTH);
    System.arraycopy(data, 0, iPadAndData, BLOCK_LENGTH, data.length);

    byte[] oPadAndHash = new byte[BLOCK_LENGTH + MD5_DIGEST_LENGTH];
    System.arraycopy(oPadArray, 0, oPadAndHash, 0, BLOCK_LENGTH);


    // Iterate through the bytes in the key and XOR them with iPad and oPad as
    // appropriate.
    for (int i=0; i < k.length; i++)
    {
      iPadAndData[i] ^= k[i];
      oPadAndHash[i] ^= k[i];
    }


    // Copy an MD5 digest of the iPad XORed key and the data into the array to
    // be hashed.
    System.arraycopy(md5Digest.digest(iPadAndData), 0, oPadAndHash,
                     BLOCK_LENGTH, MD5_DIGEST_LENGTH);


    // Return an MD5 hash of the resulting combination of the iPadHash and the
    // oPad XOR.
    return md5Digest.digest(oPadAndHash);
  }



  /**
   * Writes a hexadecimal representation of the contents of the provided byte
   * array into the given string buffer.  All hexadecimal digits greater than
   * nine will use the lowercase alphabetic representation.
   *
   * @param  byteArray  The byte array to be written as a hex string.
   * @param  buffer     The buffer to which the data should be written.
   */
  public static void writeToHexString(byte[] byteArray, StringBuilder buffer)
  {
    for (int i=0; i < byteArray.length; i++)
    {
      switch ((byteArray[i] >>> 4) & 0x0F)
      {
        case 0x00:
          buffer.append('0');
          break;
        case 0x01:
          buffer.append('1');
          break;
        case 0x02:
          buffer.append('2');
          break;
        case 0x03:
          buffer.append('3');
          break;
        case 0x04:
          buffer.append('4');
          break;
        case 0x05:
          buffer.append('5');
          break;
        case 0x06:
          buffer.append('6');
          break;
        case 0x07:
          buffer.append('7');
          break;
        case 0x08:
          buffer.append('8');
          break;
        case 0x09:
          buffer.append('9');
          break;
        case 0x0a:
          buffer.append('a');
          break;
        case 0x0b:
          buffer.append('b');
          break;
        case 0x0c:
          buffer.append('c');
          break;
        case 0x0d:
          buffer.append('d');
          break;
        case 0x0e:
          buffer.append('e');
          break;
        case 0x0f:
          buffer.append('f');
          break;
      }

      switch (byteArray[i] & 0x0F)
      {
        case 0x00:
          buffer.append('0');
          break;
        case 0x01:
          buffer.append('1');
          break;
        case 0x02:
          buffer.append('2');
          break;
        case 0x03:
          buffer.append('3');
          break;
        case 0x04:
          buffer.append('4');
          break;
        case 0x05:
          buffer.append('5');
          break;
        case 0x06:
          buffer.append('6');
          break;
        case 0x07:
          buffer.append('7');
          break;
        case 0x08:
          buffer.append('8');
          break;
        case 0x09:
          buffer.append('9');
          break;
        case 0x0a:
          buffer.append('a');
          break;
        case 0x0b:
          buffer.append('b');
          break;
        case 0x0c:
          buffer.append('c');
          break;
        case 0x0d:
          buffer.append('d');
          break;
        case 0x0e:
          buffer.append('e');
          break;
        case 0x0f:
          buffer.append('f');
          break;
      }
    }
  }
}

