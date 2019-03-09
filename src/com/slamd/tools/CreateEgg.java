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
package com.slamd.tools;



import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.unboundid.util.Base64;



/**
 * This program can be used to generate Easter eggs for inclusion in software
 * that is based on HTML pages.  It works by accepting the query string that is
 * to be used to access the easter egg, generating an MD5 hash of the query
 * string that should be used to determine when the user has entered that query
 * string in the URL (so that the clear-text query string does not appear in the
 * application source), and then reading a file with the data that should be
 * displayed when this egg is invoked and re-writing it to a DES-encrypted file
 * with a key of the clear-text query string.
 *
 *
 * @author   Neil A. Wilson
 */
public class CreateEgg
{
  /**
   * The salt that will always be used for encryption.  Since we don't care that
   * much about security in this case, the salt can be weak for added
   * convenience.
   */
  public static final byte[] SALT = { 0, 0, 0, 0, 0, 0, 0, 0 };



  /**
   * The number of iterations to apply to the encryption algorithm.
   */
  public static final int ITERATIONS = 1000;



  /**
   * The name of the cipher to use for the encryption.
   */
  public static final String CIPHER_NAME = "PBEWithMD5AndDES";



  /**
   * Parses the command-line arguments and performs the appropriate processing.
   *
   * @param  args  The command-line arguments provided to this program.
   *
   * @throws  Exception  If a problem occurs while performing any processing.
   */
  public static void main(String[] args)
         throws Exception
  {
    String  queryString = null;
    String  inputFile   = null;
    String  outputFile  = null;
    boolean decrypt     = false;


    // Parse the command-line arguments provided to the program.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-q"))
      {
        queryString = args[++i];
      }
      else if (args[i].equals("-i"))
      {
        inputFile = args[++i];
      }
      else if (args[i].equals("-o"))
      {
        outputFile = args[++i];
      }
      else if (args[i].equals("-d"))
      {
        decrypt = true;
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("ERROR:  Unrecognized argument\"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    // Validate the parameters provided.
    if (queryString == null)
    {
      System.err.println("ERROR:  No query string provided (use -q)");
      displayUsage();
      System.exit(1);
    }

    if (inputFile == null)
    {
      System.err.println("ERROR:  No input file provided (use -i)");
      displayUsage();
      System.exit(1);
    }

    if (outputFile == null)
    {
      if (decrypt)
      {
        outputFile = inputFile + ".decrypted";
      }
      else
      {
        outputFile = inputFile + ".encrypted";
      }
    }


    // Perform the appropriate encryption or decryption based on the provided
    // command-line arguments.
    if (decrypt)
    {
      // Initialize the decryption mechanism.
      AlgorithmParameters algorithmParams =
           AlgorithmParameters.getInstance(CIPHER_NAME);
      algorithmParams.init(new PBEParameterSpec(SALT, ITERATIONS));
      SecretKeyFactory keyFactory =
           SecretKeyFactory.getInstance(CIPHER_NAME);
      SecretKey key =
           keyFactory.generateSecret(new PBEKeySpec(queryString.toCharArray()));
      Cipher cipher = Cipher.getInstance(CIPHER_NAME);
      cipher.init(Cipher.DECRYPT_MODE, key, algorithmParams);


      // Open the files and do the decryption.
      int bytesIn;
      int bytesOut;
      int totalBytesRead = 0;
      int totalBytesWritten = 0;
      byte[] inBuffer = new byte[4096];
      byte[] outBuffer = new byte[8192];
      FileInputStream inputStream = new FileInputStream(inputFile);
      FileOutputStream outputStream = new FileOutputStream(outputFile);
      while ((bytesIn = inputStream.read(inBuffer)) > 0)
      {
        bytesOut = cipher.update(inBuffer, 0, bytesIn, outBuffer);
        outputStream.write(outBuffer, 0, bytesOut);
        totalBytesRead += bytesIn;
        totalBytesWritten += bytesOut;
      }
      outputStream.write(cipher.doFinal());
      inputStream.close();
      outputStream.flush();
      outputStream.close();
      System.out.println("Read " + totalBytesRead + " bytes from " + inputFile);
      System.out.println("Wrote " + totalBytesWritten + " bytes to " +
                         outputFile);
    }
    else
    {
      // Create an MD5 hash of the query string.
      MessageDigest md5Digest = MessageDigest.getInstance("MD5");
      byte[] queryHashBytes   = md5Digest.digest(queryString.getBytes("UTF-8"));
      String queryHashStr     = Base64.encode(queryHashBytes);
      System.out.println("MD5 hash of query string is " + queryHashStr);


      // Initialize the encryption mechanism.
      AlgorithmParameters algorithmParams =
           AlgorithmParameters.getInstance(CIPHER_NAME);
      algorithmParams.init(new PBEParameterSpec(SALT, ITERATIONS));
      SecretKeyFactory keyFactory =
           SecretKeyFactory.getInstance(CIPHER_NAME);
      SecretKey key =
           keyFactory.generateSecret(new PBEKeySpec(queryString.toCharArray()));
      Cipher cipher = Cipher.getInstance(CIPHER_NAME);
      cipher.init(Cipher.ENCRYPT_MODE, key, algorithmParams);


      // Open the files and do the encryption.
      int bytesIn;
      int bytesOut;
      int totalBytesRead = 0;
      int totalBytesWritten = 0;
      byte[] inBuffer = new byte[4096];
      byte[] outBuffer = new byte[8192];
      FileInputStream inputStream = new FileInputStream(inputFile);
      FileOutputStream outputStream = new FileOutputStream(outputFile);
      while ((bytesIn = inputStream.read(inBuffer)) > 0)
      {
        bytesOut = cipher.update(inBuffer, 0, bytesIn, outBuffer);
        outputStream.write(outBuffer, 0, bytesOut);
        totalBytesRead += bytesIn;
        totalBytesWritten += bytesOut;
      }
      outputStream.write(cipher.doFinal());
      inputStream.close();
      outputStream.flush();
      outputStream.close();
      System.out.println("Read " + totalBytesRead + " bytes from " + inputFile);
      System.out.println("Wrote " + totalBytesWritten + " bytes to " +
                         outputFile);
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    System.out.println("USAGE:  java CreateEgg -q {queryString} " +
                       "-i {inputFile} -o {outputFile} [-d]");
  }
}

