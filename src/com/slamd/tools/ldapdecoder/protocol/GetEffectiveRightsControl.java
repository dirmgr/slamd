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
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the LDAP get effective rights control, which may be used
 * to make determinations about the access permissions for a given user.
 *
 *
 * @author   Neil A. Wilson
 */
public class GetEffectiveRightsControl
       extends LDAPControl
{
  /**
   * The OID of the get effective rights control.
   */
  public static final String GET_EFFECTIVE_RIGHTS_CONTROL_OID =
                                  "1.3.6.1.4.1.42.2.27.9.5.2";



  // The authorization ID that specifies the user for which to make the
  // determination.
  private String authzID;

  // The set of attribute types for which to make the determination.
  private String[] attributeTypes;



  /**
   * Creates a new get effective rights control.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   */
  public GetEffectiveRightsControl(boolean isCritical)
  {
    super(GET_EFFECTIVE_RIGHTS_CONTROL_OID, isCritical, null);
  }



  /**
   * Creates a new get effective rights control using the provided information.
   *
   * @param  isCritical      Indicates whether this control should be marked
   *                         critical.
   * @param  authzID         The authorization ID that specifies the user for
   *                         which to determine the effective rights.
   * @param  attributeTypes  The set of attributes for which to retrieve the
   *                         effective rights.
   */
  public GetEffectiveRightsControl(boolean isCritical, String authzID,
                                   String[] attributeTypes)
  {
    super(GET_EFFECTIVE_RIGHTS_CONTROL_OID, isCritical,
          encodeValue(authzID, attributeTypes));

    this.authzID        = authzID;
    this.attributeTypes = attributeTypes;
  }



  /**
   * Creates a new get effective rights control by decoding the provided control
   * value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The control value that may be decoded to obtain
   *                       additional information about the way the request
   *                       should be processed.
   *
   * @throws  ProtocolException  If a problem occurs while attempting to decode
   *                             the control value.
   */
  public GetEffectiveRightsControl(boolean isCritical,
                                   ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(GET_EFFECTIVE_RIGHTS_CONTROL_OID, isCritical, controlValue);


    if (controlValue == null)
    {
      authzID        = null;
      attributeTypes = null;
    }
    else
    {
      ASN1Element[] elements;
      try
      {
        byte[] valueBytes = controlValue.getValue();
        elements = ASN1Element.decodeAsSequence(valueBytes).getElements();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode get effective rights " +
                                    "control sequence", e);
      }


      if (elements.length != 2)
      {
        throw new ProtocolException("There must be exactly 2 elements in a " +
                                    "get effective rights value sequence");
      }


      try
      {
        authzID = elements[0].decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode the authzID from the " +
                                    "get effective rights control value", e);
      }


      try
      {
        ASN1Element[] attrElements =
             elements[1].decodeAsSequence().getElements();
        attributeTypes = new String[attrElements.length];
        for (int i=0; i < attrElements.length; i++)
        {
          attributeTypes[i] =
               attrElements[i].decodeAsOctetString().getStringValue();
        }
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode attribute types from " +
                                    "the get effective rights control value",
                                    e);
      }
    }
  }



  /**
   * Retrieves the authzID that specifies the user for which to make the
   * effective rights determination.
   *
   * @return  The authzID that specifies the user for which to make the
   *          effective rights determination.
   */
  public String getAuthzID()
  {
    return authzID;
  }



  /**
   * Retrieves the attribute types for which to make the effective rights
   * determination.
   *
   * @return  The attribute types for which to make the effective rights
   *          determination.
   */
  public String[] getAttributeTypes()
  {
    return attributeTypes;
  }



  /**
   * Encodes the provided information into an octet string that may be used as
   * the value for the get effective rights control.
   *
   * @param  authzID         The authzID that specifies the user for which to
   *                         make the effective rights determination.
   * @param  attributeTypes  The attribute types for which to make the effective
   *                         rights determination.
   *
   * @return  The octet string containing the encoded control value.
   */
  public static ASN1OctetString encodeValue(String authzID,
                                            String[] attributeTypes)
  {
    if ((authzID == null) || (attributeTypes == null))
    {
      return null;
    }


    ASN1Element[] attrElements = new ASN1Element[attributeTypes.length];
    for (int i=0; i < attributeTypes.length; i++)
    {
      attrElements[i] = new ASN1OctetString(attributeTypes[i]);
    }


    ASN1Element[] valueElements = new ASN1Element[]
    {
      new ASN1OctetString(authzID),
      new ASN1Sequence(attrElements)
    };


    return new ASN1OctetString(new ASN1Sequence(valueElements).encode());
  }



  /**
   * Retrieves a string representation of this control with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this control with the specified indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("LDAP Get Effective Rights Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);


    if (authzID != null)
    {
      buffer.append(indentBuf).append("    Authorization ID:  ").
             append(authzID).append(LDAPMessage.EOL);
    }


    if ((attributeTypes != null) && (attributeTypes.length > 0))
    {
      buffer.append(indentBuf).append("    Attribute Types:").
             append(LDAPMessage.EOL);
      for (int i=0; i < attributeTypes.length; i++)
      {
        buffer.append(indentBuf).append("        ").append(attributeTypes[i]).
               append(LDAPMessage.EOL);
      }
    }

    return buffer.toString();
  }
}

