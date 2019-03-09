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
package com.slamd.protocol;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;



/**
 * This class defines a data structure for holding information about a class
 * transferred from the server to a client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClassData
{
  // The bytes that comprise the class file.
  private byte[] classBytes;

  // The fully-qualified name of the class.
  private String className;



  /**
   * Creates a new class data object with the provided information.
   *
   * @param  className   The fully-qualified name of the class.
   * @param  classBytes  The bytes that comprise the class file.
   */
  public ClassData(String className, byte[] classBytes)
  {
    this.className  = className;
    this.classBytes = classBytes;
  }



  /**
   * Retrieves the fully-qualified name of the class.
   *
   * @return  The fully-qualified name of the class.
   */
  public String getClassName()
  {
    return className;
  }



  /**
   * Specifies the fully-qualified name of the class.
   *
   * @param  className  The fully-qualified name of the class.
   */
  public void setClassName(String className)
  {
    this.className = className;
  }



  /**
   * Retrieves the bytes that comprise the class file.
   *
   * @return  The bytes that comprise the class file.
   */
  public byte[] getClassBytes()
  {
    return classBytes;
  }



  /**
   * Specifies the bytes that comprise the class file.
   *
   * @param  classBytes  The bytes that comprise the class file.
   */
  public void setClassBytes(byte[] classBytes)
  {
    this.classBytes = classBytes;
  }



  /**
   * Writes the contents of the class to an appropriate file below the given
   * directory.
   *
   * @param  directory  The path to the directory below which the class file
   *                    should be written.
   *
   * @throws  IOException  If an I/O problem occurs while attempting to write
   *                       the class file.
   *
   * @throws  SecurityException  If the security manager will not allow the file
   *                             to be written.
   */
  public void writeClassFile(String directory)
         throws IOException, SecurityException
  {
    String fileName = directory + File.separatorChar +
                      className.replace('.', File.separatorChar) + ".class";

    FileOutputStream outputStream = new FileOutputStream(fileName);
    outputStream.write(classBytes);
    outputStream.flush();
    outputStream.close();
  }



  /**
   * Encodes this class data structure to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded class data structure.
   */
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    elementList.add(SLAMDMessage.encodeNameValuePair(
                         ProtocolConstants.PROPERTY_CLASS_NAME,
                         new ASN1OctetString(className)));
    elementList.add(SLAMDMessage.encodeNameValuePair(
                         ProtocolConstants.PROPERTY_CLASS_BYTES,
                         new ASN1OctetString(classBytes)));

    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the provided ASN.1 element as a set of class data information.
   *
   * @param  encodedData  The ASN.1 element containing the data to be decoded.
   *
   * @return  The decoded class data information.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided element.
   */
  public static ClassData decode(ASN1Element encodedData)
         throws SLAMDException
  {
    HashMap<String,ASN1Element> propertyMap =
         SLAMDMessage.decodeNameValuePairSequence(encodedData);

    String className;
    ASN1Element valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_CLASS_NAME);
    if (valueElement == null)
    {
      throw new SLAMDException("Class data sequence does not include the " +
                               "class name element.");
    }
    else
    {
      try
      {
        className = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the class name property:  " +
                                 e, e);
      }
    }


    byte[] classBytes;
    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_CLASS_BYTES);
    if (valueElement == null)
    {
      throw new SLAMDException("Class data sequence does not include the " +
                               "class bytes.");
    }
    else
    {
      try
      {
        classBytes = valueElement.decodeAsOctetString().getValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the class bytes " +
                                 "property:  " + e, e);
      }
    }


    return new ClassData(className, classBytes);
  }



  /**
   * Retrieves a string representation of this class data structure.
   *
   * @return  A string representation of this class data structure.
   */
  @Override()
  public String toString()
  {
    return className + " (" + classBytes.length + " bytes)";
  }
}

