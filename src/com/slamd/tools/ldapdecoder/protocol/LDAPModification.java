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



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP modification, which describes a change that should
 * be made to an entry in a directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPModification
{
  /**
   * The modification type that indicates that the provided attribute value(s)
   * should be added to the entry.
   */
  public static final int MOD_TYPE_ADD = 0;



  /**
   * The modification type that indicates that the provided attribute value(s)
   * should be removed from the entry.
   */
  public static final int MOD_TYPE_DELETE = 1;



  /**
   * The modification type that indicates that any existing values for the
   * specified attribute should be replaced with the given values.
   */
  public static final int MOD_TYPE_REPLACE = 2;



  /**
   * The modification type that indicates that an existing integer value should
   * be incremented by a given amount.
   */
  public static final int MOD_TYPE_INCREMENT = 3;



  // The type of modification that should be made.
  private int modType;

  // The attribute on which the modification should be made.
  private LDAPAttribute attribute;



  /**
   * Creates a new LDAP modification with the provided information.
   *
   * @param  modType    The type of modification that should be made.
   * @param  attribute  The attribute on which the change should be made.
   */
  public LDAPModification(int modType, LDAPAttribute attribute)
  {
    this.modType   = modType;
    this.attribute = attribute;
  }



  /**
   * Retrieves the modification type for this LDAP modification.
   *
   * @return  The modification type for this LDAP modification.
   */
  public int getModType()
  {
    return modType;
  }



  /**
   * Retrieves the attribute for this LDAP modification.
   *
   * @return  The attribute for this LDAP modification.
   */
  public LDAPAttribute getAttribute()
  {
    return attribute;
  }



  /**
   * Encodes this LDAP modification to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded LDAP modification.
   */
  public ASN1Element encode()
  {
    ASN1Element[] modElements = new ASN1Element[]
    {
      new ASN1Enumerated(modType),
      attribute.encode()
    };


    return new ASN1Sequence(modElements);
  }



  /**
   * Decodes the provided ASN.1 element as an LDAP modification.
   *
   * @param  element  The ASN.1 element containing the encoded LDAP
   *                  modification.
   *
   * @return  The decoded LDAP modification.
   *
   * @throws  ProtocolException  If a problem occurs while attempting to decode
   *                             the modification.
   */
  public static LDAPModification decode(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] modElements;
    try
    {
      modElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modification sequence", e);
    }


    if (modElements.length != 2)
    {
      throw new ProtocolException("There must be exactly 2 elements in a " +
                                  "modification sequence");
    }


    int modType;
    try
    {
      modType = modElements[0].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modification change type",
                                  e);
    }


    LDAPAttribute attribute;
    try
    {
      attribute = LDAPAttribute.decode(modElements[1]);
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modification attribute", e);
    }


    return new LDAPModification(modType, attribute);
  }



  /**
   * Retrieves a string representation of this modification.
   *
   * @return  A string representation of this modification.
   */
  public String toString()
  {
    return toString(0);
  }



  /**
   * Retrieves a string representation of this modification using the specified
   * indent.
   *
   * @param  indent  The number of spaces that the modification should be
   *                 indented.
   *
   * @return  The string representation of this modification.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(" ");
    }


    StringBuilder buffer = new StringBuilder();


    String modTypeStr;
    switch (modType)
    {
      case MOD_TYPE_ADD:
        modTypeStr = "add";
        break;
      case MOD_TYPE_DELETE:
        modTypeStr = "delete";
        break;
      case MOD_TYPE_REPLACE:
        modTypeStr = "replace";
        break;
      case MOD_TYPE_INCREMENT:
        modTypeStr = "increment";
        break;
      default:
        modTypeStr = "invalid change type (" + modType + ")";
        break;
    }
    buffer.append(indentBuf).append(modTypeStr).append(": ").
           append(attribute.getType()).append(LDAPMessage.EOL);


    ASN1OctetString[] values = attribute.getValues();
    if ((values != null) && (values.length > 0))
    {
      buffer.append(attribute.toString(indent));
    }


    return buffer.toString();
  }
}

