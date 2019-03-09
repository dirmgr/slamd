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
 * This class defines an LDAP extended request, which may be used to request
 * some kind of operation in the directory.
 *
 *
 * @author   Neil A. Wilson
 */
public class ExtendedRequest
       extends ProtocolOp
{
  /**
   * The ASN.1 type that should be used to encode a request OID.
   */
  public static final byte REQUEST_OID_TYPE = (byte) 0x80;



  /**
   * The ASN.1 type that should be used to encode a request value.
   */
  public static final byte REQUEST_VALUE_TYPE = (byte) 0x81;



  // The value associated with this extended request.
  private ASN1OctetString requestValue;

  // The OID for this extended request.
  private String requestOID;



  /**
   * Creates a new extended request with the provided OID and no value.
   *
   * @param  requestOID  The OID for the extended request.
   */
  public ExtendedRequest(String requestOID)
  {
    this.requestOID = requestOID;

    requestValue = null;
  }



  /**
   * Creates a new extended request with the provided OID and value.
   *
   * @param  requestOID    The OID for the extended request.
   * @param  requestValue  The value to use for the extended request.
   */
  public ExtendedRequest(String requestOID, ASN1OctetString requestValue)
  {
    this.requestOID   = requestOID;
    this.requestValue = requestValue;
  }



  /**
   * Retrieves the OID for this extended request.
   *
   * @return  The OID for this extended request.
   */
  public String getRequestOID()
  {
    return requestOID;
  }



  /**
   * Retrieves the value for this extended request.
   *
   * @return  The value for this extended request.
   */
  public ASN1OctetString getRequestValue()
  {
    return requestValue;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] requestElements;
    if (requestValue == null)
    {
      requestElements = new ASN1Element[]
      {
        new ASN1OctetString(REQUEST_OID_TYPE, requestOID)
      };
    }
    else
    {
      requestValue.setType(REQUEST_VALUE_TYPE);

      requestElements = new ASN1Element[]
      {
        new ASN1OctetString(REQUEST_OID_TYPE, requestOID),
        requestValue
      };
    }


    return new ASN1Sequence(EXTENDED_REQUEST_TYPE, requestElements);
  }



  /**
   * Decodes the provided ASN.1 element as an extended request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded extended request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as an extended request.
   */
  public static ExtendedRequest decodeExtendedRequest(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] requestElements;
    try
    {
      requestElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode extended request sequence",
                                  e);
    }


    if ((requestElements.length < 1) || (requestElements.length > 2))
    {
      throw new ProtocolException("There must be 1 or 2 elements in an " +
                                  "extended request sequence");
    }


    String requestOID;
    try
    {
      requestOID = requestElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode extended request OID", e);
    }


    ASN1OctetString requestValue = null;
    if (requestElements.length == 2)
    {
      try
      {
        requestValue = requestElements[1].decodeAsOctetString();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode extended request value",
                                    e);
      }
    }


    // FIXME -- Add custom decoding for special kinds of extended requests.
    return new ExtendedRequest(requestOID, requestValue);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Extended Request";
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
    buffer.append(indentBuf).append("Request OID: ").append(requestOID).
           append(LDAPMessage.EOL);


    if (requestValue != null)
    {
      byte[] valueBytes = requestValue.getValue();
      buffer.append(indentBuf).append("Request Value:").
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
    scriptWriter.println("#### Extended request captured at " + new Date());
    scriptWriter.println("# The SLAMD scripting language does not currently " +
                         "extended abandons.");
    scriptWriter.println();
    scriptWriter.println();
  }
}

