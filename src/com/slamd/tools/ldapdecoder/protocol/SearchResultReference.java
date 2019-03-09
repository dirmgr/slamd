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
 * This class defines an LDAP search result reference, which provides a link to
 * one or more other locations in which the search should be performed.
 *
 *
 * @author   Neil A. Wilson
 */
public class SearchResultReference
       extends ProtocolOp
{
  // The set of referral URLs for this search result reference.
  private String[] referralURLs;



  /**
   * Creates a new search result reference with the provided referral URLs.
   *
   * @param  referralURLs  The referral URLs for use in this search result
   *                       reference.
   */
  public SearchResultReference(String[] referralURLs)
  {
    this.referralURLs = referralURLs;
  }



  /**
   * Retrieves the set of referral URLs associated with this search result
   * reference.
   *
   * @return  The set of referral URLs associated with this search result
   *          reference.
   */
  public String[] getReferralURLs()
  {
    return referralURLs;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] referralElements;
    if ((referralURLs == null) || (referralURLs.length == 0))
    {
      referralElements = new ASN1Element[0];
    }
    else
    {
      referralElements = new ASN1Element[referralURLs.length];
      for (int i=0; i < referralElements.length; i++)
      {
        referralElements[i] = new ASN1OctetString(referralURLs[i]);
      }
    }


    return new ASN1Sequence(SEARCH_RESULT_REFERENCE_TYPE, referralElements);
  }



  /**
   * Decodes the provided ASN.1 element as a search result reference protocol
   * op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded search result reference.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a search result reference.
   */
  public static SearchResultReference
                     decodeSearchResultReference(ASN1Element element)
         throws ProtocolException
  {
    try
    {
      ASN1Element[] referralElements = element.decodeAsSequence().getElements();
      String[] referralURLs = new String[referralElements.length];
      for (int i=0; i < referralElements.length; i++)
      {
        referralURLs[i] =
             referralElements[i].decodeAsOctetString().getStringValue();
      }

      return new SearchResultReference(referralURLs);
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search result reference " +
                                  "sequence", e);
    }
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP SearchResultReference";
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
    buffer.append(indentBuf).append("Referral URLs: ").
           append(LDAPMessage.EOL);

    for (int i=0; ((referralURLs != null) && (i < referralURLs.length)); i++)
    {
      buffer.append(indentBuf).append("    ").append(referralURLs[i]).
             append(LDAPMessage.EOL);
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
    scriptWriter.println("#### Search result reference captured at " +
                         new Date());
    scriptWriter.println("# Referral URLs: ");
    for (int i=0; ((referralURLs != null) && (i < referralURLs.length)); i++)
    {
      scriptWriter.println("#   " + referralURLs[i]);
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

