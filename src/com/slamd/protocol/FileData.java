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



import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a data structure for holding information about a file
 * generated during job processing that should be uploaded into the SLAMD
 * server along with the job results.
 *
 *
 * @author   Neil A. Wilson
 */
public class FileData
{
  // The actual data for the file.
  private byte[] fileData;

  // A description for the file.
  private String fileDescription;

  // The name of the file, without any path information.
  private String fileName;

  // The MIME type for the file.
  private String fileType;



  /**
   * Creates a new file data object with the provided information.
   *
   * @param  fileName         The name of the file, without any path
   *                          information.
   * @param  fileData         The actual data for the file.
   * @param  fileType         The MIME type for the file.
   * @param  fileDescription  A description for the file.
   */
  public FileData(String fileName, byte[] fileData, String fileType,
                  String fileDescription)
  {
    this.fileName        = fileName;
    this.fileData        = fileData;
    this.fileType        = fileType;
    this.fileDescription = fileDescription;
  }



  /**
   * Retrieves the name of the file, without any path information.
   *
   * @return  The name of the file, without any path information.
   */
  public String getFileName()
  {
    return fileName;
  }



  /**
   * Specifies the name of the file, without any path information.
   *
   * @param  fileName  The name of the file without any path information.
   */
  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }



  /**
   * Retrieves actual data for the file.
   *
   * @return  The actual data for the file.
   */
  public byte[] getFileData()
  {
    return fileData;
  }



  /**
   * Specifies the actual data for the file.
   *
   * @param  fileData  The actual data for the file.
   */
  public void setFileData(byte[] fileData)
  {
    this.fileData = fileData;
  }



  /**
   * Retrieves the MIME type for the file.
   *
   * @return  The MIME type for the file.
   */
  public String getFileType()
  {
    return fileType;
  }



  /**
   * Specifies the MIME type for the file.
   *
   * @param  fileType  The MIME type for the file.
   */
  public void setFileType(String fileType)
  {
    this.fileType = fileType;
  }



  /**
   * Retrieves the description for the file.
   *
   * @return  The description for the file, or <CODE>null</CODE> if none was
   *          provided.
   */
  public String getFileDescription()
  {
    return fileDescription;
  }



  /**
   * Specifies the description for the file.
   *
   * @param  fileDescription  The description for the file.
   */
  public void setFileDescription(String fileDescription)
  {
    this.fileDescription = fileDescription;
  }



  /**
   * Encodes this file data structure to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded file data structure.
   */
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    elementList.add(SLAMDMessage.encodeNameValuePair(
                         ProtocolConstants.PROPERTY_FILE_NAME,
                         new ASN1OctetString(fileName)));
    elementList.add(SLAMDMessage.encodeNameValuePair(
                         ProtocolConstants.PROPERTY_FILE_DATA,
                         new ASN1OctetString(fileData)));
    elementList.add(SLAMDMessage.encodeNameValuePair(
                         ProtocolConstants.PROPERTY_FILE_TYPE,
                         new ASN1OctetString(fileType)));

    if (fileDescription != null)
    {
      elementList.add(SLAMDMessage.encodeNameValuePair(
                           ProtocolConstants.PROPERTY_FILE_DESCRIPTION,
                           new ASN1OctetString(fileDescription)));
    }

    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the provided ASN.1 element as a set of file data information.
   *
   * @param  encodedData  The ASN.1 element containing the data to be decoded.
   *
   * @return  The decoded file data information.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided element.
   */
  public static FileData decode(ASN1Element encodedData)
         throws SLAMDException
  {
    HashMap<String,ASN1Element> propertyMap =
         SLAMDMessage.decodeNameValuePairSequence(encodedData);

    String fileName;
    ASN1Element valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_FILE_NAME);
    if (valueElement == null)
    {
      throw new SLAMDException("File data sequence does not include the " +
                               "file name element.");
    }
    else
    {
      try
      {
        fileName = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the file name property:  " +
                                 e, e);
      }
    }


    byte[] fileData;
    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_FILE_DATA);
    if (valueElement == null)
    {
      throw new SLAMDException("File data sequence does not include the " +
                               "file data.");
    }
    else
    {
      try
      {
        fileData = valueElement.decodeAsOctetString().getValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the file data property:  " +
                                 e, e);
      }
    }


    String fileType;
    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_FILE_TYPE);
    if (valueElement == null)
    {
      throw new SLAMDException("File data sequence does not include the " +
                               "file type.");
    }
    else
    {
      try
      {
        fileType = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the file type property:  " +
                                 e, e);
      }
    }


    String fileDescription = null;
    valueElement = propertyMap.get(ProtocolConstants.PROPERTY_FILE_DESCRIPTION);
    if (valueElement != null)
    {
      try
      {
        fileDescription = valueElement.decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the file description " +
                                 "property:  " + e, e);
      }
    }


    return new FileData(fileName, fileData, fileType, fileDescription);
  }



  /**
   * Encodes the provided array of file data objects to an ASN.1 element.
   *
   * @param  fileData  The set of file data objects to be encoded.
   *
   * @return  The ASN.1 element containing the encoded file data information.
   */
  public static ASN1Element encodeArray(FileData[] fileData)
  {
    ASN1Element[] elements = new ASN1Element[fileData.length];

    for (int i=0; i < elements.length; i++)
    {
      elements[i] = fileData[i].encode();
    }

    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 element as an array of file data objects.
   *
   * @param  encodedData  The ASN.1 element containing the encoded file data
   *                      objects.
   *
   * @return  The decoded array of file data objects.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided element.
   */
  public static FileData[] decodeArray(ASN1Element encodedData)
         throws SLAMDException
  {
    ASN1Element[] elements;
    try
    {
      elements = encodedData.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to decode the provided ASN.1 element " +
                               "as a sequence:  " + e, e);
    }

    FileData[] fileData = new FileData[elements.length];
    for (int i=0; i < fileData.length; i++)
    {
      fileData[i] = decode(elements[i]);
    }

    return fileData;
  }



  /**
   * Retrieves a string representation of this class data structure.
   *
   * @return  A string representation of this class data structure.
   */
  @Override()
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    toString(buffer, 0);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this file data structure to the provided
   * string buffer, using the specified indent.
   *
   * @param  buffer  The buffer to which the string representation should be
   *                 appended.
   * @param  indent  The number of spaces to indent the output.
   */
  public void toString(StringBuilder buffer, int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    buffer.append(indentBuf);
    buffer.append("fileName = ");
    buffer.append(fileName);
    buffer.append(Constants.EOL);

    buffer.append(indentBuf);
    buffer.append("     fileType = ");
    buffer.append(fileType);

    if (fileDescription != null)
    {
      buffer.append(Constants.EOL);
      buffer.append(indentBuf);
      buffer.append("     fileDescription = ");
      buffer.append(fileDescription);
    }

    buffer.append(Constants.EOL);
    buffer.append(indentBuf);
    buffer.append("     fileData = byte[");
    buffer.append(fileData.length);
    buffer.append(']');
  }
}

