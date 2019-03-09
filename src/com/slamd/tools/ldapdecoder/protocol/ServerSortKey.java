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
package com.slamd.tools.ldapdecoder.protocol;



import java.util.ArrayList;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines a sort key that may be provided in a server sort request
 * control to specify how the server should sort the results.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerSortKey
{
  /**
   * The ASN.1 type that should be used to encode the matching rule ID in the
   * sort key.
   */
  public static final byte MATCHING_RULE_ID_TYPE = (byte) 0x80;



  /**
   * The ASN.1 type that should be used to encode the reverse order flag in the
   * sort key.
   */
  public static final byte REVERSE_ORDER_TYPE = (byte) 0x81;



  // Indicates whether the entries should be sorted in reverse order.
  private boolean reverseOrder;

  // Specifies the attribute type to be sorted.
  private String attributeType;

  // The matching rule ID that should be used to define the ordering.
  private String matchingRuleID;



  /**
   * Creates a new server sort key with the provided information.
   *
   * @param  attributeType   The attribute type for which the sorting should be
   *                         performed.
   * @param  matchingRuleID  The OID of the matching rule that should be used to
   *                         define the ordering.
   * @param  reverseOrder    Indicates whether the entries should be returned in
   *                         reverse order.
   */
  public ServerSortKey(String attributeType, String matchingRuleID,
                       boolean reverseOrder)
  {
    this.attributeType  = attributeType;
    this.matchingRuleID = matchingRuleID;
    this.reverseOrder   = reverseOrder;
  }



  /**
   * Retrieves the attribute type for which the sorting should be performed.
   *
   * @return  The attribute type for which the sorting should be performed.
   */
  public String getAttributeType()
  {
    return attributeType;
  }



  /**
   * Retrieves the OID of the matching rule that should be used to define the
   * ordering.
   *
   * @return  The OID of the matching rule that should be used to define the
   *          ordering.
   */
  public String getMatchingRuleID()
  {
    return matchingRuleID;
  }



  /**
   * Indicates whether the entries should be returned in reverse order.
   *
   * @return  <CODE>true</CODE> if the entries should be returned in reverse
   *          order, or <CODE>false</CODE> if not.
   */
  public boolean reverseOrder()
  {
    return reverseOrder;
  }



  /**
   * Encodes this sort key to an ASN.1 element suitable for including in a
   * server sort request control.
   *
   * @return  The ASN.1 element containing the encoded sort key.
   */
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();
    elementList.add(new ASN1OctetString(attributeType));

    if ((matchingRuleID != null) && (matchingRuleID.length() > 0))
    {
      elementList.add(new ASN1OctetString(MATCHING_RULE_ID_TYPE,
                                          matchingRuleID));
    }

    if (reverseOrder)
    {
      elementList.add(new ASN1Boolean(REVERSE_ORDER_TYPE, reverseOrder));
    }


    ASN1Element[] elements = new ASN1Element[elementList.size()];
    elementList.toArray(elements);
    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 element as a server sort key.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded server sort key.
   *
   * @throws  ProtocolException  If a problem is encountered while decoding the
   *                             sort key.
   */
  public static ServerSortKey decode(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] elements;
    try
    {
      elements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode server sort key sequence",
                                  e);
    }


    if ((elements.length < 1) || (elements.length > 3))
    {
      throw new ProtocolException("There must be between 1 and 3 elements in " +
                                  "a sort key sequence");
    }


    String attributeType;
    try
    {
      attributeType = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode attribute type from " +
                                  "sort key sequence", e);
    }


    String  matchingRuleID = null;
    boolean reverseOrder   = false;
    for (int i=1; i < elements.length; i++)
    {
      byte type = elements[i].getType();
      if (type == MATCHING_RULE_ID_TYPE)
      {
        try
        {
          matchingRuleID = elements[i].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode matching rule ID " +
                                      "from sort key sequence", e);
        }
      }
      else if (type == REVERSE_ORDER_TYPE)
      {
        try
        {
          reverseOrder = elements[i].decodeAsBoolean().getBooleanValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode reverseOrder flag " +
                                      "from sort key sequence", e);
        }
      }
      else
      {
        throw new ProtocolException("Invalid type for sort key element (" +
                                    type + ')');
      }
    }


    return new ServerSortKey(attributeType, matchingRuleID, reverseOrder);
  }



  /**
   * Retrieves a string representation of this server sort key.
   *
   * @return  A string representation of this server sort key.
   */
  public String toString()
  {
    return toString(0);
  }



  /**
   * Retrieves a string representation of this server sort key using the
   * specified indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this server sort key using the
   *          specified indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("Server Sort Key").append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Attribute Type:  ").
           append(attributeType).append(LDAPMessage.EOL);


    if ((matchingRuleID != null) && (matchingRuleID.length() > 0))
    {
      buffer.append(indentBuf).append("    Matching Rule ID:  ").
             append(matchingRuleID).append(LDAPMessage.EOL);
    }



    buffer.append(indentBuf).append("    Reverse Order:  ").
           append(reverseOrder).append(LDAPMessage.EOL);


    return buffer.toString();
  }
}

