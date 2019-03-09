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
import com.slamd.asn1.ASN1Null;



/**
 * This class defines an LDAP unbind request, which is used to disconnect from a
 * directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class UnbindRequest
       extends ProtocolOp
{
  /**
   * Creates a new unbind request.
   */
  public UnbindRequest()
  {
    // No action required;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    return new ASN1Null(UNBIND_REQUEST_TYPE);
  }



  /**
   * Decodes the provided ASN.1 element as an unbind request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded unbind request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as an unbind request.
   */
  public static UnbindRequest decodeUnbindRequest(ASN1Element element)
         throws ProtocolException
  {
    try
    {
      element.decodeAsNull();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode unbind request element", e);
    }

    return new UnbindRequest();
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Unbind Request";
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
    return "";
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
    // We won't replay unbind requests because then any further requests on the
    // connection would fail.  However, note that the unbind occurred so that
    // the user editing the script can decide whether to allow it.
    scriptWriter.println("#### Unbind request captured at " + new Date());
    scriptWriter.println("# Not actually going to unbind to prevent problems " +
                         "with future operations.");
    scriptWriter.println("# If you actually want the unbind processed, then " +
                         "uncomment the next line:");
    scriptWriter.println("# conn.disconnect();");
    scriptWriter.println();
    scriptWriter.println();
  }
}

