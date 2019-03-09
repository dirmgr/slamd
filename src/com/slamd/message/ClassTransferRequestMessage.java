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
import com.slamd.common.SLAMDException;



/**
 * This class defines a class transfer request message that the client uses to
 * request job classes from the server that the client does not currently have
 * in its classpath.  This class will be saved locally on the client system so
 * that it is will not be necessary to request it again.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClassTransferRequestMessage
       extends Message
{
  // The name of the Java class specified in this message.
  private final String className;



  /**
   * Creates a new class transfer request message with the specified message ID
   * and class name.
   *
   * @param  messageID  The message ID for this message.
   * @param  className  The name of the Java class specified in this message.
   */
  public ClassTransferRequestMessage(int messageID, String className)
  {
    super(messageID, ASN1_TYPE_CLASS_TRANSFER_REQUEST);
    this.className = className;
  }



  /**
   * Retrieves the Java class name associated with this class transfer request
   * message.
   *
   * @return  The Java class name associated with this class transfer request
   *          message.
   */
  public String getClassName()
  {
    return className;
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
           "  Class Name:  " + className + eol;
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
  public static ClassTransferRequestMessage
                     decodeTransferRequest(int messageID, ASN1Element element)
         throws SLAMDException
  {
    String className = null;

    try
    {
      ASN1OctetString nameString = element.decodeAsOctetString();
      className = nameString.getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The class name element cannot be decoded as " +
                               "an octet string", ae);
    }

    return new ClassTransferRequestMessage(messageID, className);
  }



  /**
   * Encodes this message into an ASN.1 element.  A file transfer request
   * message has the following ASN.1 syntax:
   * <BR><BR>
   * <CODE>ClassTransferRequest ::= [APPLICATION 12] OCTET STRING</CODE>
   * <BR>
   *
   * @return  An ASN.1 encoded representation of this message.
   */
  @Override()
  public ASN1Element encode()
  {
    ASN1Integer messageIDElement = new ASN1Integer(messageID);

    ASN1OctetString classNameString =
         new ASN1OctetString(ASN1_TYPE_CLASS_TRANSFER_REQUEST, className);

    ASN1Element[] messageElements = new ASN1Element[]
    {
      messageIDElement,
      classNameString
    };

    return new ASN1Sequence(messageElements);
  }
}

