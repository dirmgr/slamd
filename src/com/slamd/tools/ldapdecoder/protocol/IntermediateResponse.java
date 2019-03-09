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
import java.util.ArrayList;
import java.util.Date;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP extended response, which provides information
 * about the result of processing an intermediate response.
 *
 *
 * @author   Neil A. Wilson
 */
public class IntermediateResponse
       extends ProtocolOp
{
  /**
   * The ASN.1 type that should be used to encode a response OID.
   */
  public static final byte RESPONSE_OID_TYPE = (byte) 0x80;



  /**
   * The ASN.1 type that should be used to encode a response value.
   */
  public static final byte RESPONSE_VALUE_TYPE = (byte) 0x81;



  // The value for the extended response.
  private ASN1OctetString responseValue;

  // The OID for the extended response.
  private String responseOID;



  /**
   * Creates a new intermediate response with the provided information.
   *
   * @param  responseOID    The OID for this intermediate response.
   * @param  responseValue  The value for this intermediate response.
   */
  public IntermediateResponse(String responseOID, ASN1OctetString responseValue)
  {
    this.responseOID   = responseOID;
    this.responseValue = responseValue;
  }



  /**
   * Retrieves the OID for this extended response.
   *
   * @return  The OID for this extended response.
   */
  public String getReponseOID()
  {
    return responseOID;
  }



  /**
   * Retrieves the value for this extended response.
   *
   * @return  The value for this extended response.
   */
  public ASN1OctetString getResponseValue()
  {
    return responseValue;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>();

    if (responseOID != null)
    {
      elementList.add(new ASN1OctetString(RESPONSE_OID_TYPE, responseOID));
    }

    if (responseValue != null)
    {
      elementList.add(new ASN1OctetString(RESPONSE_VALUE_TYPE,
                                          responseValue.getValue()));
    }


    ASN1Element[] responseElements = new ASN1Element[elementList.size()];
    elementList.toArray(responseElements);
    return new ASN1Sequence(EXTENDED_RESPONSE_TYPE, responseElements);
  }



  /**
   * Decodes the provided ASN.1 element as an extended response protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded extended response.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as an extended response.
   */
  public static IntermediateResponse decodeIntermediateResponse(
                                          ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] responseElements;
    try
    {
      responseElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode extended response sequence",
                                  e);
    }


    if (responseElements.length > 2)
    {
      throw new ProtocolException("There must be no more than 2 elements in " +
                                  "an intermediate response sequence");
    }


    String          responseOID = null;
    ASN1OctetString responseValue = null;
    for (ASN1Element e : responseElements)
    {
      switch (e.getType())
      {
        case RESPONSE_OID_TYPE:
          try
          {
            responseOID = e.decodeAsOctetString().getStringValue();
          }
          catch (Exception ex)
          {
            throw new ProtocolException("Unable to decode the intermediate "  +
                                        "response OID", ex);
          }
          break;

        case RESPONSE_VALUE_TYPE:
          try
          {
            responseValue = e.decodeAsOctetString();
          }
          catch (Exception ex)
          {
            throw new ProtocolException("Unable to decode the intermediate " +
                                        "response value", ex);
          }
          break;

        default:
          throw new ProtocolException("Unknown intermediate response element " +
                                      "type " + e.getType());
      }
    }


    // FIXME:  Add specific decoding for special intermediate response types
    return new IntermediateResponse(responseOID, responseValue);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Intermediate Response";
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
    if (responseOID != null)
    {
      buffer.append(indentBuf).append("Response OID: ").append(responseOID).
             append(LDAPMessage.EOL);
    }


    if (responseValue!= null)
    {
      byte[] valueBytes = responseValue.getValue();
      buffer.append(indentBuf).append("Response Value:").
             append(LDAPMessage.EOL);
      buffer.append(LDAPMessage.byteArrayToString(valueBytes, (indent+4)));
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
    scriptWriter.println("#### Intermediate response captured at " +
                         new Date());

    if (responseOID != null)
    {
      scriptWriter.println("# Response OID:  " + responseOID);
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

