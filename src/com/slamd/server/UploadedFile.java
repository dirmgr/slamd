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
package com.slamd.server;



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.db.DecodeException;



/**
 * This class defines a data type to hold information about a file that has been
 * uploaded and stored in the SLAMD configuration directory.
 *
 *
 * @author   Neil A. Wilson
 */
public class UploadedFile
{
  /**
   * The name of the encoded element that holds the name of the uploaded file.
   */
  public static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that holds the size of the uploaded file.
   */
  public static final String ELEMENT_SIZE = "size";



  /**
   * The name of the encoded element that holds the type of the uploaded file.
   */
  public static final String ELEMENT_TYPE = "type";



  /**
   * The name of the encoded element that holds the description for the uploaded
   * file.
   */
  public static final String ELEMENT_DESCRIPTION = "description";



  /**
   * The name of the encoded element that holds the uploaded file data.
   */
  public static final String ELEMENT_DATA = "data";



  private byte[] fileData;
  private int    fileSize;
  private String fileDescription;
  private String fileName;
  private String fileType;



  /**
   * Creates a new uploaded file with just summary information (no actual file
   * data).
   *
   * @param  fileName         The name of the file.
   * @param  fileType         The MIME type for the file.
   * @param  fileSize         The size of the file in bytes.
   * @param  fileDescription  A brief description of the file.
   */
  public UploadedFile(String fileName, String fileType, int fileSize,
                      String fileDescription)
  {
    this.fileName        = fileName;
    this.fileType        = fileType;
    this.fileSize        = fileSize;
    this.fileDescription = fileDescription;
    this.fileData        = null;
  }



  /**
   * Creates a new uploaded file including the file data.
   *
   * @param  fileName         The name of the file.
   * @param  fileType         The MIME type for the file.
   * @param  fileSize         The size of the file in bytes.
   * @param  fileDescription  A brief description of the file.
   * @param  fileData         The actual data contained in the file.
   */
  public UploadedFile(String fileName, String fileType, int fileSize,
                      String fileDescription, byte[] fileData)
  {
    this.fileName        = fileName;
    this.fileType        = fileType;
    this.fileSize        = fileSize;
    this.fileDescription = fileDescription;
    this.fileData        = fileData;
  }



  /**
   * Retrieves the name of the uploaded file.
   *
   * @return  The name of the uploaded file.
   */
  public String getFileName()
  {
    return fileName;
  }



  /**
   * Retrieves the MIME type for the uploaded file.
   *
   * @return  The MIME type for the uploaded file.
   */
  public String getFileType()
  {
    return fileType;
  }



  /**
   * Specifies the MIME type to use for the uploaded file.
   *
   * @param  fileType  The MIME type to use for the uploaded file.
   */
  public void setFileType(String fileType)
  {
    this.fileType = fileType;
  }



  /**
   * Retrieves the size of the file in bytes.
   *
   * @return  The size of the file in bytes.
   */
  public int getFileSize()
  {
    return fileSize;
  }



  /**
   * Retrieves the description for the file.
   *
   * @return  The description for the file.
   */
  public String getFileDescription()
  {
    return fileDescription;
  }



  /**
   * Specifies the description to use for the uploaded file.
   *
   * @param  fileDescription  The description to use for the uploaded file.
   */
  public void setFileDescription(String fileDescription)
  {
    this.fileDescription = fileDescription;
  }



  /**
   * Retrieves the actual file data.
   *
   * @return  The actual file data, or <CODE>null</CODE> if no file data has
   *          been provided.
   */
  public byte[] getFileData()
  {
    return fileData;
  }



  /**
   * Encodes information about this uploaded file into a byte array.
   *
   * @return  The byte array containing the encoded file data.
   */
  public byte[] encode()
  {
    ASN1Element[] elements = new ASN1Element[]
    {
      new ASN1OctetString(ELEMENT_NAME),
      new ASN1OctetString(fileName),
      new ASN1OctetString(ELEMENT_SIZE),
      new ASN1Integer(fileSize),
      new ASN1OctetString(ELEMENT_TYPE),
      new ASN1OctetString(fileType),
      new ASN1OctetString(ELEMENT_DESCRIPTION),
      new ASN1OctetString(fileDescription),
      new ASN1OctetString(ELEMENT_DATA),
      new ASN1OctetString(fileData)
    };

    return new ASN1Sequence(elements).encode();
  }



  /**
   * Decodes the provided byte array as an uploaded file.
   *
   * @param  encodedFile  The byte array containing the data to decode.
   *
   * @return  The decoded file.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           provided byte array as an uploaded file.
   */
  public static UploadedFile decode(byte[] encodedFile)
         throws DecodeException
  {
    try
    {
      byte[] fileData        = null;
      int    fileSize        = 0;
      String fileDescription = null;
      String fileName        = null;
      String fileType        = null;

      ASN1Element   element  = ASN1Element.decode(encodedFile);
      ASN1Element[] elements = element.decodeAsSequence().getElements();
      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();
        if (elementName.equals(ELEMENT_NAME))
        {
          fileName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_SIZE))
        {
          fileSize = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_TYPE))
        {
          fileType = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          fileDescription =
               elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DATA))
        {
          fileData = elements[i+1].decodeAsOctetString().getValue();
        }
      }

      return new UploadedFile(fileName, fileType, fileSize, fileDescription,
                              fileData);
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode uploaded file:  " + e, e);
    }
  }



  /**
   * Decodes the provided byte array as an uploaded file, but excluding the
   * actual file data.
   *
   * @param  encodedFile  The byte array containing the data to decode.
   *
   * @return  The decoded file, excluding the file data.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           provided byte array as an uploaded file.
   */
  public static UploadedFile decodeWithoutData(byte[] encodedFile)
         throws DecodeException
  {
    try
    {
      int    fileSize        = 0;
      String fileDescription = null;
      String fileName        = null;
      String fileType        = null;

      ASN1Element   element  = ASN1Element.decode(encodedFile);
      ASN1Element[] elements = element.decodeAsSequence().getElements();
      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();
        if (elementName.equals(ELEMENT_NAME))
        {
          fileName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_SIZE))
        {
          fileSize = elements[i+1].decodeAsInteger().getIntValue();
        }
        else if (elementName.equals(ELEMENT_TYPE))
        {
          fileType = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          fileDescription =
               elements[i+1].decodeAsOctetString().getStringValue();
        }
      }

      return new UploadedFile(fileName, fileType, fileSize, fileDescription);
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode uploaded file:  " + e, e);
    }
  }
}

