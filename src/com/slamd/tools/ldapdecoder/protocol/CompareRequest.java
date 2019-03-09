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



import java.io.PrintStream;
import java.util.Date;

import com.unboundid.util.Base64;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP compare request, which is used to determine
 * whether an entry has a given attribute name and value.
 *
 *
 * @author   Neil A. Wilson
 */
public class CompareRequest
       extends ProtocolOp
{
  // The assertion value for the compare request.
  private ASN1OctetString assertionValue;

  // The attribute type for the compare request.
  private String attributeType;

  // The DN of the entry to compare.
  private String dn;



  /**
   * Creates a new compare request with the provided information.
   *
   * @param  dn              The DN of the entry for which to perform the
   *                         comparison.
   * @param  attributeType   The attribute type for this compare request.
   * @param  assertionValue  The assertion value for this compare request.
   */
  public CompareRequest(String dn, String attributeType,
                        ASN1OctetString assertionValue)
  {
    this.dn             = dn;
    this.attributeType  = attributeType;
    this.assertionValue = assertionValue;
  }



  /**
   * Retrieves the DN of the entry for which to perform the comparison.
   *
   * @return  The DN of the entry for which to perform the comparison.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Retrieves the attribute type for this compare request.
   *
   * @return  The attribute type for this compare request.
   */
  public String getAttributeType()
  {
    return attributeType;
  }



  /**
   * Retrieves the assertion value for this compare request.
   *
   * @return  The assertion value for this compare request.
   */
  public ASN1OctetString getAssertionValue()
  {
    return assertionValue;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] avaElements = new ASN1Element[]
    {
      new ASN1OctetString(attributeType),
      assertionValue
    };


    ASN1Element[] compareElements = new ASN1Element[]
    {
      new ASN1OctetString(dn),
      new ASN1Sequence(avaElements)
    };


    return new ASN1Sequence(COMPARE_REQUEST_TYPE, compareElements);
  }



  /**
   * Decodes the provided ASN.1 element as a compare request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded compare request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a compare request.
   */
  public static CompareRequest decodeCompareRequest(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] compareElements;
    try
    {
      compareElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode compare request sequence",
                                  e);
    }


    if (compareElements.length != 2)
    {
      throw new ProtocolException("There must be exactly 2 elements in a " +
                                  "compare request sequence");
    }


    String dn;
    try
    {
      dn = compareElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode DN from compare request",
                                  e);
    }


    String          attributeType;
    ASN1OctetString assertionValue;
    try
    {
      ASN1Element[] avaElements =
                         compareElements[1].decodeAsSequence().getElements();
      attributeType  = avaElements[0].decodeAsOctetString().getStringValue();
      assertionValue = avaElements[1].decodeAsOctetString();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode attribute-value " +
                                  "assertion from compare request", e);
    }


    return new CompareRequest(dn, attributeType, assertionValue);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Compare Request";
  }



  /**
   * Retrieves a string representation of this protocol op with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this protocol op with the specified
   *          indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("Entry DN: ").append(dn).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("Attribute Type: ").
           append(attributeType).append(LDAPMessage.EOL);

    if (LDAPAttribute.valueNeedsBase64Encoding(assertionValue))
    {
      buffer.append(indentBuf).append("Assertion Value:: ").
             append(Base64.encode(assertionValue.getValue())).
             append(LDAPMessage.EOL);
    }
    else
    {
      buffer.append(indentBuf).append("Assertion Value: ").
             append(assertionValue.getStringValue()).append(LDAPMessage.EOL);
    }

    return buffer.toString();
  }



  /**
   * Constructs a string representation of this LDAP message in a form that can
   * be written to a SLAMD script.  It may be empty if this message isn't one
   * that would be generated as part of a client request.
   *
   * @param  scriptWriter  The print stream to which the script contents should
   *                       be written.
   */
  public void toSLAMDScript(PrintStream scriptWriter)
  {
    scriptWriter.println("#### Compare request captured at " + new Date());
    scriptWriter.println("# Entry DN:  " + dn);
    scriptWriter.println("# Attribute Type:  " + attributeType);
    scriptWriter.println("# Assertion Value:  " +
                         assertionValue.getStringValue());
    scriptWriter.println("resultCode = conn.compare(\"" + dn + "\", \"" +
                         attributeType + "\", \"" +
                         assertionValue.getStringValue() + "\");");
    scriptWriter.println();
    scriptWriter.println();
  }
}

