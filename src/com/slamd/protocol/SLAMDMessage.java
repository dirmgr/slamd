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



import java.util.HashMap;
import java.util.Iterator;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.job.JobClass;



/**
 * This class defines the basic envelope for a message used to communicate
 * between the SLAMD server and one of the client types.  A SLAMD message
 * contains a message ID (which is an integer), a message type (which is a class
 * name), and the payload (which varies based on the message type).  For the
 * purposes of extensibility, it may also contain a set of name-value pairs that
 * may be used to provide additional information about the message and/or the
 * way it should be handled.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class SLAMDMessage
{
  // The set of name-value pairs for the "extra" properties associated with this
  // message.  The name should be a string and the value an object.
  private HashMap<String,String> extraProperties;

  // The message ID for this SLAMD message.
  private int messageID;



  /**
   * Creates a new instance of this SLAMD message without any of its properties
   * set.  This is only intended for use in the process of decoding messages
   * and should only be used when the <CODE>initializeDecodedMessage</CODE>
   * method is going to be called immediately after invoking this constructor.
   */
  protected SLAMDMessage()
  {
    // No implementation is required.
  }



  /**
   * Creates a new instance of this SLAMD message with the provided message ID
   * and optional set of properties.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   */
  protected SLAMDMessage(int messageID, HashMap<String,String> extraProperties)
  {
    this.messageID = messageID;

    if (extraProperties == null)
    {
      this.extraProperties = new HashMap<String,String>();
    }
    else
    {
      this.extraProperties = extraProperties;
    }
  }



  /**
   * Initializes this decoded message with the provided message ID and extra
   * properties.  This method should only be called when decoding an encoded
   * message, and it should only be called immediately after invoking the
   * default constructor.
   *
   * @param  messageID        The message ID for this SLAMD message.
   * @param  extraProperties  The "extra" properties for this SLAMD message.
   *                          Both the names and values for the properties must
   *                          be strings.
   */
  private void initializeDecodedMessage(int messageID,
                                        HashMap<String,String> extraProperties)
  {
    this.messageID = messageID;

    if (extraProperties == null)
    {
      this.extraProperties = new HashMap<String,String>();
    }
    else
    {
      this.extraProperties = extraProperties;
    }
  }



  /**
   * Retrieves the message ID for this SLAMD message.
   *
   * @return  The message ID for this SLAMD message.
   */
  public int getMessageID()
  {
    return messageID;
  }



  /**
   * Retrieves the set of "extra" properties for this SLAMD message.  The
   * contents of the returned map may be altered by the caller.
   *
   * @return  The set of "extra" properties for this SLAMD message.
   */
  public HashMap getExtraProperties()
  {
    return extraProperties;
  }



  /**
   * Retrieves the value of the "extra" property with the specified name.
   *
   * @param  name  The name of the property to retrieve.
   *
   * @return  The value of the "extra" property with the specified name, or
   *          <CODE>null</CODE> if there is no such property.
   */
  public String getExtraProperty(String name)
  {
    Object value = extraProperties.get(name);

    if (value == null)
    {
      return null;
    }

    return value.toString();
  }



  /**
   * Encodes this SLAMD message to an ASN.1 element that may be transferred
   * between the SLAMD server and a client.
   *
   * @return  The encoded SLAMD message.
   */
  public final ASN1Element encode()
  {
    ASN1Element[] elements = new ASN1Element[4];

    elements[0] = new ASN1Integer(messageID);
    elements[1] = new ASN1OctetString(getClass().getName());
    elements[2] = encodeMessagePayload();

    if (extraProperties.isEmpty())
    {
      elements[3] = new ASN1Sequence();
    }
    else
    {
      ASN1Element[] propsElements = new ASN1Element[extraProperties.size()];

      int pos = 0;
      Iterator<String> iterator = extraProperties.keySet().iterator();

      while (iterator.hasNext())
      {
        String name = iterator.next();

        String value;
        Object valueObj = extraProperties.get(name);
        if (valueObj == null)
        {
          value = null;
        }
        else
        {
          value = valueObj.toString();
        }

        propsElements[pos++] = encodeNameValuePair(name,
                                                   new ASN1OctetString(value));
      }

      elements[3] = new ASN1Sequence(propsElements);
    }

    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 element as a SLAMD message.
   *
   * @param  messageElement  The ASN.1 element containing the encoded SLAMD
   *                         message.
   *
   * @return  The decoded SLAMD message.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided ASN.1 element as a SLAMD message.
   */
  public static SLAMDMessage decode(ASN1Element messageElement)
         throws SLAMDException
  {
    try
    {
      ASN1Element[] elements = messageElement.decodeAsSequence().getElements();

      // First, get the message ID.
      int messageID;
      try
      {
        messageID = elements[0].decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Cannot decode the first message element as " +
                                 "the message ID:  " +
                                 JobClass.stackTraceToString(e), e);
      }


      // Then, get the class name.
      String className;
      try
      {
        className = elements[1].decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Cannot decode the second message element " +
                                 "as the message class:  " +
                                 JobClass.stackTraceToString(e), e);
      }


      // If there are any "extra" message properties, then get them.
      HashMap<String,String> extraProperties;
      try
      {
        ASN1Element[] propElements =
             elements[3].decodeAsSequence().getElements();
        extraProperties = new HashMap<String,String>(propElements.length);

        for (int i=0; i < propElements.length; i++)
        {
          ASN1Element[] e = propElements[i].decodeAsSequence().getElements();
          String name     = e[0].decodeAsOctetString().getStringValue();
          String value    = e[1].decodeAsOctetString().getStringValue();
          extraProperties.put(name, value);
        }
      }
      catch (Exception e)
      {
        throw new SLAMDException("Error while attempting to parse additional " +
                                 "message properties:  " +
                                 JobClass.stackTraceToString(e), e);
      }


      // Load, instantiate, and initialize the message class.
      Class<?> messageClass;
      try
      {
        messageClass = Constants.classForName(className);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Cannot load specified message class " +
                                 className + ":  " +
                                 JobClass.stackTraceToString(e), e);
      }

      SLAMDMessage message;
      try
      {
        message = (SLAMDMessage) messageClass.newInstance();
        message.initializeDecodedMessage(messageID, extraProperties);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Cannot create an instance of class " +
                                 className + " as a SLAMD message:  " +
                                 JobClass.stackTraceToString(e), e);
      }


      // Decode the payload and return the message.
      message.decodeMessagePayload(elements[2]);
      return message;
    }
    catch (SLAMDException se)
    {
      throw se;
    }
    catch (Exception e)
    {
      throw new SLAMDException("Cannot deocde the provided ASN.1 element as " +
                               "a SLAMD message:  " +
                               JobClass.stackTraceToString(e), e);
    }
  }



  /**
   * Encodes the payload component of this SLAMD message to an ASN.1 element for
   * inclusion in the message envelope.
   *
   * @return  The ASN.1 element containing the encoded message payload.
   */
  public abstract ASN1Element encodeMessagePayload();



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
  public abstract void decodeMessagePayload(ASN1Element payloadElement)
         throws SLAMDException;



  /**
   * Appends a string representation of the payload for this SLAMD message to
   * the provided buffer.  The string representation may contain multiple lines,
   * but the last line should not end with an end-of-line marker.
   *
   * @param  buffer  The buffer to which the string representation is to be
   *                 appended.
   * @param  indent  The number of spaces to indent the payload content.
   */
  public abstract void payloadToString(StringBuilder buffer, int indent);



  /**
   * Retrieves a string representation of this SLAMD message.
   *
   * @return  A string representation of this SLAMD message.
   */
  @Override()
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this SLAMD message to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which the string representation of this SLAMD
   *                 message should be appended.
   */
  public void toString(StringBuilder buffer)
  {
    String EOL = Constants.EOL;

    buffer.append("SLAMDMessage");
    buffer.append(EOL);

    buffer.append("     Message ID:  ");
    buffer.append(messageID);
    buffer.append(EOL);

    buffer.append("     Message Type:  ");
    buffer.append(getClass().getName());
    buffer.append(EOL);

    buffer.append("     Message Payload:");
    buffer.append(EOL);
    payloadToString(buffer, 10);
    buffer.append(EOL);

    if ((extraProperties != null) && (! extraProperties.isEmpty()))
    {
      buffer.append("     Extra Properties:");
      buffer.append(EOL);

      Iterator iterator = extraProperties.keySet().iterator();
      while (iterator.hasNext())
      {
        String name = (String) iterator.next();

        String value;
        Object valueObj = extraProperties.get(name);
        if (valueObj == null)
        {
          value = "";
        }
        else
        {
          value = valueObj.toString();
        }

        buffer.append("          ");
        buffer.append(name);
        buffer.append('=');
        buffer.append(value);
        buffer.append(EOL);
      }
    }
  }



  /**
   * Encodes the provided name and value into an ASN.1 sequence.
   *
   * @param  name   The name to include in the sequence.
   * @param  value  The encoded value to place in the sequence.
   *
   * @return  The ASN.1 sequence element containing the encoded name-value pair.
   */
  public static ASN1Sequence encodeNameValuePair(String name,
                                                 ASN1Element value)
  {
    ASN1Element[] elements =
    {
      new ASN1OctetString(name),
      value
    };

    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 element as a sequence containing a name and
   * value.
   *
   * @param  sequenceElement  The ASN.1 sequence element to decode as a
   *                          name-value pair.
   * @param  nameBuffer       The buffer to which the decoded element name will
   *                          be appended.
   *
   * @return  The ASN.1 element containing the encoded value for the name-value
   *          pair.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided element as a name-value pair.
   */
  public static ASN1Element decodeNameValuePair(ASN1Element sequenceElement,
                                                StringBuilder nameBuffer)
         throws SLAMDException
  {
    ASN1Element[] elements;
    try
    {
      elements = sequenceElement.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Cannot decode the provided element as an " +
                               "ASN.1 sequence:  " + e, e);
    }

    if (elements.length != 2)
    {
      throw new SLAMDException("The provided ASN.1 sequence contained an " +
                               "invalid number of elements (expected 2, got " +
                               elements.length + ").");
    }

    String name;
    try
    {
      name = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Cannot decode the first sequence element as " +
                               "an octet string containing the property " +
                               "name:  " + e, e);
    }

    nameBuffer.append(name);
    return elements[1];
  }



  /**
   * Decodes the provided ASN.1 element as a sequence of name-value pair
   * elements.
   *
   * @param  sequenceElement  The ASN.1 element containing the encoded sequence
   *                          of name-value pair elements.
   *
   * @return  The set of decoded name-value pairs as a mapping between the
   *          property name and the encoded element.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          name-value pair elements.
   */
  public static HashMap<String,ASN1Element> decodeNameValuePairSequence(
                                                 ASN1Element sequenceElement)
         throws SLAMDException
  {
    ASN1Element[] elements;
    try
    {
      elements = sequenceElement.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to decode the provided ASN.1 element " +
                               "as a sequence of name-value pair elements:  " +
                               e, e);
    }

    HashMap<String,ASN1Element> propertyMap =
         new HashMap<String,ASN1Element>(elements.length);
    for (int i=0; i < elements.length; i++)
    {
      StringBuilder nameBuffer = new StringBuilder();
      ASN1Element valueElement = decodeNameValuePair(elements[i], nameBuffer);
      String name = nameBuffer.toString();

      if (propertyMap.containsKey(name))
      {
        throw new SLAMDException("Multiple occurrences of property " + name +
                                 "found in the provided set of name-value " +
                                 "pairs.");
      }

      propertyMap.put(name, valueElement);
    }

    return propertyMap;
  }
}

