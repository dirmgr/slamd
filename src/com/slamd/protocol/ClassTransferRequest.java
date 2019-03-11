/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2019.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.protocol;



import java.util.ArrayList;
import java.util.Map;

import com.unboundid.asn1.ASN1Boolean;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;

import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a SLAMD message that will be sent from the client to the
 * server whenever the client receives a request to process a job using a class
 * that has not been defined on the client.  It can include multiple classes.
 *
 *
 * @author   Neil A. Wilson
 */
public final class ClassTransferRequest
       extends SLAMDMessage
{
  // Indicates whether the server should also include any dependencies
  // associated with the requested classes.
  private boolean includeDependencies;

  // The names of the classes that have been requested by the client.
  private String[] classNames;



  /**
   * Creates a new instance of this class transfer request message which is
   * intended for use in decoding a message transmitted between the server and
   * the client.  It is not intended for general use.
   */
  public ClassTransferRequest()
  {
    super();

    classNames          = null;
    includeDependencies = false;
  }



  /**
   * Creates a new instance of this class transfer request message with the
   * provided information.
   *
   * @param  messageID            The message ID for this SLAMD message.
   * @param  extraProperties      The "extra" properties for this SLAMD message.
   *                              Both the names and values for the properties
   *                              must be strings.
   * @param  classNames           The names of the classes that have been
   *                              requested by the client.
   * @param  includeDependencies  Indicates whether the server should also
   *                              include any dependencies associated with the
   *                              requested classes.
   */
  public ClassTransferRequest(final int messageID,
                              final Map<String,String> extraProperties,
                              final String[] classNames,
                              final boolean includeDependencies)
  {
    super(messageID, extraProperties);

    this.classNames          = classNames;
    this.includeDependencies = includeDependencies;
  }



  /**
   * Retrieves the names of the classes that have been requested by the client.
   *
   * @return  The names of the classes that have been requested by the client.
   */
  public String[] getClassNames()
  {
    return classNames;
  }



  /**
   * Specifies the names of the classes that have been requested by the client.
   *
   * @param  classNames  The names of the classes that have been requested by
   *                     the client.
   */
  public void setClassNames(final String[] classNames)
  {
    this.classNames = classNames;
  }



  /**
   * Indicates whether the server should include any dependencies associated
   * with the requested classes.
   *
   * @return  {@code true} if the server should include any dependencies
   *          associated with the requested classes, or {@code false} if
   *          not.
   */
  public boolean getIncludeDependencies()
  {
    return includeDependencies;
  }



  /**
   * Specifies whether the server should include any dependencies associated
   * with the requested classes.
   *
   * @param  includeDependencies  Specifies whether the server should include
   *                              any dependencies with the requested classes.
   */
  public void setIncludeDependencies(final boolean includeDependencies)
  {
    this.includeDependencies = includeDependencies;
  }



  /**
   * Encodes the payload component of this SLAMD message to an ASN.1 element for
   * inclusion in the message envelope.
   *
   * @return  The ASN.1 element containing the encoded message payload.
   */
  @Override()
  public ASN1Element encodeMessagePayload()
  {
    final ArrayList<ASN1Element> elementList = new ArrayList<>();

    final ASN1Element[] classNameElements = new ASN1Element[classNames.length];
    for (int i=0; i < classNames.length; i++)
    {
      classNameElements[i] = new ASN1OctetString(classNames[i]);
    }
    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_REQUESTED_CLASS_NAMES,
                         new ASN1Sequence(classNameElements)));

    elementList.add(encodeNameValuePair(
                         ProtocolConstants.PROPERTY_INCLUDE_DEPENDENCIES,
                         new ASN1Boolean(includeDependencies)));

    return new ASN1Sequence(elementList);
  }



  /**
   * Decodes the provided ASN.1 element and uses it as the payload for this
   * SLAMD message.
   *
   * @param  payloadElement  The ASN.1 element to decode as the payload for this
   *                         SLAMD message.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided ASN.1 element as the payload for this
   *                          SLAMD message.
   */
  @Override()
  public void decodeMessagePayload(final ASN1Element payloadElement)
         throws SLAMDException
  {
    final Map<String,ASN1Element> propertyMap =
         decodeNameValuePairSequence(payloadElement);

    ASN1Element valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_REQUESTED_CLASS_NAMES);
    if (valueElement == null)
    {
      throw new SLAMDException("Class transfer request message does not " +
           "include any class names.");
    }
    else
    {
      try
      {
        final ASN1Element[] classNameElements =
             valueElement.decodeAsSequence().elements();
        classNames = new String[classNameElements.length];
        for (int i=0; i < classNameElements.length; i++)
        {
          classNames[i] =
               classNameElements[i].decodeAsOctetString().stringValue();
        }
      }
      catch (final Exception e)
      {
        throw new SLAMDException(
             "Unable to decode the requested class names:  " + e, e);
      }
    }


    valueElement =
         propertyMap.get(ProtocolConstants.PROPERTY_INCLUDE_DEPENDENCIES);
    if (valueElement != null)
    {
      try
      {
        includeDependencies = valueElement.decodeAsBoolean().booleanValue();
      }
      catch (final Exception e)
      {
        throw new SLAMDException(
             "Unable to decode the includeDependencies flag:  " + e, e);
      }
    }
  }



  /**
   * Appends a string representation of the payload for this SLAMD message to
   * the provided buffer.  The string representation may contain multiple lines,
   * but the last line should not end with an end-of-line marker.
   *
   * @param  buffer  The buffer to which the string representation is to be
   *                 appended.
   * @param  indent  The number of spaces to indent the payload content.
   */
  @Override()
  public void payloadToString(final StringBuilder buffer, final int indent)
  {
    final StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    buffer.append(indentBuf);
    buffer.append("classNames =");
    buffer.append(Constants.EOL);
    for (final String className : classNames)
    {
      buffer.append(indentBuf);
      buffer.append("     ");
      buffer.append(className);
      buffer.append(Constants.EOL);
    }

    buffer.append(indentBuf);
    buffer.append("includeDependencies = ");
    buffer.append(includeDependencies);
  }
}

