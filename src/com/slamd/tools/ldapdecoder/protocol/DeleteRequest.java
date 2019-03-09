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



/**
 * This class defines an LDAP delete request, which is used to remove an entry
 * from a directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class DeleteRequest
       extends ProtocolOp
{
  // The DN of the entry to delete.
  private String dn;



  /**
   * Creates a new delete request to delete the entry with the given DN.
   *
   * @param  dn  The DN of the entry to delete.
   */
  public DeleteRequest(String dn)
  {
    this.dn = dn;
  }



  /**
   * Retrieves the DN of the entry to delete.
   *
   * @return  The DN of the entry to delete.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    return new ASN1OctetString(DELETE_REQUEST_TYPE, dn);
  }



  /**
   * Decodes the provided ASN.1 element as a delete request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded delete request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a delete request.
   */
  public static DeleteRequest decodeDeleteRequest(ASN1Element element)
         throws ProtocolException
  {
    try
    {
      String dn = element.decodeAsOctetString().getStringValue();
      return new DeleteRequest(dn);
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode delete request", e);
    }
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Delete Request";
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
    buffer.append(indentBuf).append("dn: ").append(dn).append(LDAPMessage.EOL);

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
    scriptWriter.println("#### Delete request captured at " + new Date());
    scriptWriter.println("# Entry DN:  " + dn);
    scriptWriter.println("resultCode = conn.delete(\"" + dn + "\");");
    scriptWriter.println();
    scriptWriter.println();
  }
}

