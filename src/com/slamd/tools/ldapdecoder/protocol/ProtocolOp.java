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

import com.slamd.asn1.ASN1Element;



/**
 * This class defines an LDAP protocol op, which is used to encapsulate the
 * main part of an LDAP request.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class ProtocolOp
{
  /**
   * The ASN.1 type for bind request protocol ops.
   */
  public static final byte BIND_REQUEST_TYPE = 0x60;



  /**
   * The ASN.1 type for bind response protocol ops.
   */
  public static final byte BIND_RESPONSE_TYPE = 0x61;



  /**
   * The ASN.1 type for unbind request protocol ops.
   */
  public static final byte UNBIND_REQUEST_TYPE = 0x42;



  /**
   * The ASN.1 type for search request protocol ops.
   */
  public static final byte SEARCH_REQUEST_TYPE = 0x63;



  /**
   * The ASN.1 type for search result entry protocol ops.
   */
  public static final byte SEARCH_RESULT_ENTRY_TYPE = 0x64;



  /**
   * The ASN.1 type for search result reference protocol ops.
   */
  public static final byte SEARCH_RESULT_REFERENCE_TYPE = 0x73;



  /**
   * The ASN.1 type for search result done protocol ops.
   */
  public static final byte SEARCH_RESULT_DONE_TYPE = 0x65;



  /**
   * The ASN.1 type for modify request protocol ops.
   */
  public static final byte MODIFY_REQUEST_TYPE = 0x66;



  /**
   * The ASN.1 type for modify response protocol ops.
   */
  public static final byte MODIFY_RESPONSE_TYPE = 0x67;



  /**
   * The ASN.1 type for add request protocol ops.
   */
  public static final byte ADD_REQUEST_TYPE = 0x68;



  /**
   * The ASN.1 type for add response protocol ops.
   */
  public static final byte ADD_RESPONSE_TYPE = 0x69;



  /**
   * The ASN.1 type for delete request protocol ops.
   */
  public static final byte DELETE_REQUEST_TYPE = 0x4A;



  /**
   * The ASN.1 type for delete response protocol ops.
   */
  public static final byte DELETE_RESPONSE_TYPE = 0x6B;



  /**
   * The ASN.1 type for modify DN request protocol ops.
   */
  public static final byte MODIFY_DN_REQUEST_TYPE = 0x6C;



  /**
   * The ASN.1 type for modify DN response protocol ops.
   */
  public static final byte MODIFY_DN_RESPONSE_TYPE = 0x6D;



  /**
   * The ASN.1 type for compare request protocol ops.
   */
  public static final byte COMPARE_REQUEST_TYPE = 0x6E;



  /**
   * The ASN.1 type for compare response protocol ops.
   */
  public static final byte COMPARE_RESPONSE_TYPE = 0x6F;



  /**
   * The ASN.1 type for abandon request protocol ops.
   */
  public static final byte ABANDON_REQUEST_TYPE = 0x50;



  /**
   * The ASN.1 type for extended request protocol ops.
   */
  public static final byte EXTENDED_REQUEST_TYPE = 0x77;



  /**
   * The ASN.1 type for extended response protocol ops.
   */
  public static final byte EXTENDED_RESPONSE_TYPE = 0x78;



  /**
   * The ASN.1 type for intermediate response protocol ops.
   */
  public static final byte INTERMEDIATE_RESPONSE_TYPE = 0x79;



  /**
   * The ASN.1 type used to hold the referral component of the response.
   */
  public static final byte REFERRAL_TYPE = (byte) 0xA3;



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public abstract ASN1Element encode();



  /**
   * Decodes the provided ASN.1 element as an LDAP protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded LDAP protocol op.
   *
   * @throws  ProtocolException  If a problem occurs while attempting to decode
   *                             the protocol op.
   */
  public static ProtocolOp decode(ASN1Element element)
         throws ProtocolException
  {
    switch (element.getType())
    {
      case BIND_REQUEST_TYPE:
        return BindRequest.decodeBindRequest(element);
      case BIND_RESPONSE_TYPE:
        return BindResponse.decodeBindResponse(element);
      case UNBIND_REQUEST_TYPE:
        return UnbindRequest.decodeUnbindRequest(element);
      case SEARCH_REQUEST_TYPE:
        return SearchRequest.decodeSearchRequest(element);
      case SEARCH_RESULT_ENTRY_TYPE:
        return SearchResultEntry.decodeSearchResultEntry(element);
      case SEARCH_RESULT_REFERENCE_TYPE:
        return SearchResultReference.decodeSearchResultReference(element);
      case SEARCH_RESULT_DONE_TYPE:
        return SearchResultDone.decodeSearchResultDone(element);
      case MODIFY_REQUEST_TYPE:
        return ModifyRequest.decodeModifyRequest(element);
      case MODIFY_RESPONSE_TYPE:
        return ModifyResponse.decodeModifyResponse(element);
      case ADD_REQUEST_TYPE:
        return AddRequest.decodeAddRequest(element);
      case ADD_RESPONSE_TYPE:
        return AddResponse.decodeAddResponse(element);
      case DELETE_REQUEST_TYPE:
        return DeleteRequest.decodeDeleteRequest(element);
      case DELETE_RESPONSE_TYPE:
        return DeleteResponse.decodeDeleteResponse(element);
      case MODIFY_DN_REQUEST_TYPE:
        return ModifyDNRequest.decodeModifyDNRequest(element);
      case MODIFY_DN_RESPONSE_TYPE:
        return ModifyDNResponse.decodeModifyDNResponse(element);
      case COMPARE_REQUEST_TYPE:
        return CompareRequest.decodeCompareRequest(element);
      case COMPARE_RESPONSE_TYPE:
        return CompareResponse.decodeCompareResponse(element);
      case ABANDON_REQUEST_TYPE:
        return AbandonRequest.decodeAbandonRequest(element);
      case EXTENDED_REQUEST_TYPE:
        return ExtendedRequest.decodeExtendedRequest(element);
      case EXTENDED_RESPONSE_TYPE:
        return ExtendedResponse.decodeExtendedResponse(element);
      case INTERMEDIATE_RESPONSE_TYPE:
        return IntermediateResponse.decodeIntermediateResponse(element);
      default:
        throw new ProtocolException("Unrecognized protocol op type " +
                                    element.getType());
    }
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public abstract String getProtocolOpType();



  /**
   * Retrieves a string representation of this protocol op.
   *
   * @return  A string representation of this protocol op.
   */
  public String toString()
  {
    return toString(0);
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
  public abstract String toString(int indent);



  /**
   * Writes this LDAP message to the provided print stream in a form that is
   * suitable for inclusion in a SLAMD script.  It is acceptable for nothing to
   * be written if this message isn't one that would be associated with a client
   * request.
   *
   * @param  scriptWriter  The script writer to which the generated script
   *                       should be written.
   */
  public abstract void toSLAMDScript(PrintStream scriptWriter);
}

