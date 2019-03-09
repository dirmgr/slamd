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
package com.slamd.message;



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a class transfer response message that the SLAMD server
 * uses to send Java class files to clients that do not have them.  The class
 * will be saved locally on the client system so that it is will not be
 * necessary to request it again.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClassTransferResponseMessage
       extends Message
{
  // The byte array containing the encoded class data.
  private final byte[] classData;

  // The response code that indicates whether the class information has been
  // provided.
  private final int responseCode;

  // The name of the Java class specified in this message.
  private final String className;




  /**
   * Creates a new class transfer response message with the specified
   * information.
   *
   * @param  messageID     The message ID for this message.
   * @param  responseCode  The response code that indicates whether the class
   *                       information has been provided.
   * @param  className     The name of the class file associated with this class
   *                       transfer response message.
   * @param  classData     The encoded class data.
   */
  public ClassTransferResponseMessage(int messageID, int responseCode,
                                      String className, byte[] classData)
  {
    super(messageID, ASN1_TYPE_CLASS_TRANSFER_RESPONSE);

    this.responseCode = responseCode;
    this.className    = className;
    this.classData    = classData;
  }



  /**
   * Retrieves the response code associated with this message.  The value will
   * be that of one of the MESSAGE_RESPONSE_* constants.
   *
   * @return  The response code associated with this response message.
   */
  public int getResponseCode()
  {
    return responseCode;
  }



  /**
   * Retrieves the Java class name associated with this class transfer response
   * message.
   *
   * @return  The Java class name associated with this class transfer response
   *          message.
   */
  public String getClassName()
  {
    return className;
  }



  /**
   * Retrieves the encoded class data associated with this class transfer
   * response.
   *
   * @return  The encoded class data associated with this class transfer
   *          response.
   */
  public byte[] getClassData()
  {
    return classData;
  }



  /**
   * Retrieves a string representation of this message.
   *
   * @return  A string representation of this message.
   */
  @Override()
  public String toString()
  {
    String eol = System.getProperty("line.separator");

    return "Class Request Message" + eol +
           "  Message ID:  " + messageID + eol +
           "  Response Code:  " + responseCode + eol +
           "  Class Name:  " + className + eol +
           "  Class Data:  " + classData.length + " bytes" + eol;
  }



  /**
   * Decodes the provided ASN.1 element as a class transfer request message.
   *
   * @param  messageID  The message ID to use for this message.
   * @param  element    The ASN.1 element containing the encoded class name.
   *
   * @return  The class transfer request message decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a class transfer request message.
   */
  public static ClassTransferResponseMessage
                     decodeTransferResponse(int messageID, ASN1Element element)
         throws SLAMDException
  {
    // Decode the element as a sequence and get the elements from it.
    ASN1Element[] elements = null;
    try
    {
      elements = element.decodeAsSequence().getElements();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Unable to decode the transfer response " +
                               "sequence.", ae);
    }


    // There must be three elements in the response sequence.
    if ((elements == null) || (elements.length != 3))
    {
      throw new SLAMDException("Inappropriate number of elements for a " +
                               "transfer response sequence.");
    }


    // The first element is the response code.
    int responseCode = Constants.MESSAGE_RESPONSE_LOCAL_ERROR;
    try
    {
      responseCode = elements[0].decodeAsInteger().getIntValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The response code element cannot be decoded " +
                               "as an integer.", ae);
    }


    // The second element is the class name.
    String className = null;
    try
    {
      ASN1OctetString nameString = elements[1].decodeAsOctetString();
      className = nameString.getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The class name element cannot be decoded as " +
                               "an octet string", ae);
    }


    // The third element is the class data.
    byte[] classData = null;
    try
    {
      classData = elements[2].decodeAsOctetString().getValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The class data element cannot be decoded as " +
                               "an octet string", ae);
    }


    return new ClassTransferResponseMessage(messageID, responseCode, className,
                                            classData);
  }



  /**
   * Encodes this message into an ASN.1 element.  A file transfer response
   * message has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>ClassTransferResponse ::= [APPLICATION 13] SEQUENCE {</CODE>
   * <CODE>    responseCode  INTEGER,</CODE>
   * <CODE>    className     OCTET STRING,</CODE>
   * <CODE>    classData     OCTET STRING }</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Element[] responseElements = new ASN1Element[]
    {
      new ASN1Integer(responseCode),
      new ASN1OctetString(className),
      new ASN1OctetString(classData)
    };

    ASN1Element[] messageElements = new ASN1Element[]
    {
      new ASN1Integer(messageID),
      new ASN1Sequence(ASN1_TYPE_CLASS_TRANSFER_RESPONSE, responseElements)
    };

    return new ASN1Sequence(messageElements);
  }
}

