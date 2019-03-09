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

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP search result entry, which holds an entry matching
 * a search request.
 *
 *
 * @author   Neil A. Wilson
 */
public class SearchResultEntry
       extends ProtocolOp
{
  // The set of attributes included in this entry.
  private LDAPAttribute[] attributes;

  // The DN of this entry.
  private String dn;



  /**
   * Creates a new search result entry with the provided information.
   *
   * @param  dn          The DN of this entry.
   * @param  attributes  The set of attributes included in this entry.
   */
  public SearchResultEntry(String dn, LDAPAttribute[] attributes)
  {
    this.dn         = dn;
    this.attributes = attributes;
  }



  /**
   * Retrieves the DN of this entry.
   *
   * @return  The DN of this entry.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Retrieves the set of attributes included in this entry.
   *
   * @return  The set of attributes included in this entry.
   */
  public LDAPAttribute[] getAttributes()
  {
    return attributes;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] attrElements;
    if ((attributes == null) || (attributes.length == 0))
    {
      attrElements = new ASN1Element[0];
    }
    else
    {
      attrElements = new ASN1Element[attributes.length];
      for (int i=0; i < attributes.length; i++)
      {
        attrElements[i] = attributes[i].encode();
      }
    }


    ASN1Element[] entryElements = new ASN1Element[]
    {
      new ASN1OctetString(dn),
      new ASN1Sequence(attrElements)
    };


    return new ASN1Sequence(SEARCH_RESULT_ENTRY_TYPE, entryElements);
  }



  /**
   * Decodes the provided ASN.1 element as a search result entry protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded search result entry.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a search result entry.
   */
  public static SearchResultEntry decodeSearchResultEntry(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] entryElements;
    try
    {
      entryElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search result entry " +
                                  "sequence", e);
    }


    if (entryElements.length != 2)
    {
      throw new ProtocolException("There must be exactly two elements in a " +
                                  "search result entry sequence");
    }


    String dn;
    try
    {
      dn = entryElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search result entry DN", e);
    }


    LDAPAttribute[] attributes;
    try
    {
      ASN1Element[] attrElements =
                         entryElements[1].decodeAsSequence().getElements();
      attributes = new LDAPAttribute[attrElements.length];
      for (int i=0; i < attrElements.length; i++)
      {
        attributes[i] = LDAPAttribute.decode(attrElements[i]);
      }
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search result entry " +
                                  "attributes", e);
    }


    return new SearchResultEntry(dn, attributes);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Search Result Entry";
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
    buffer.append(indentBuf).append("dn: ").append(dn).
           append(LDAPMessage.EOL);

    for (int i=0; ((attributes != null) && (i < attributes.length)); i++)
    {
      buffer.append(attributes[i].toString(indent));
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
    // This is not something that would be generated by a client and therefore
    // no operation needs to be performed.  However, we can write a comment
    // to the script indicating that an add response was received.
    scriptWriter.println("#### Search result entry captured at " + new Date());
    scriptWriter.println("# dn: " + dn);
    for (int i=0; ((attributes != null) && (i < attributes.length)); i++)
    {
      ASN1OctetString[] values = attributes[i].getValues();
      for (int j=0; ((values != null) && (j < values.length)); j++)
      {
        scriptWriter.println("# " + attributes[i].getType() + ": " +
                             values[j].getStringValue());
      }
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

