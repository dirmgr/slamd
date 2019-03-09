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
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP extended response, which provides information
 * about the result of processing an extended request.
 *
 *
 * @author   Neil A. Wilson
 */
public class ExtendedResponse
       extends ProtocolOp
{
  /**
   * The ASN.1 type that should be used to encode a request OID.
   */
  public static final byte RESPONSE_OID_TYPE = (byte) 0x8A;



  /**
   * The ASN.1 type that should be used to encode a request value.
   */
  public static final byte RESPONSE_VALUE_TYPE = (byte) 0x8B;



  // The value for the extended response.
  private ASN1OctetString responseValue;

  // The result code for the operation.
  private int resultCode;

  // The error message associated with this result.
  private String errorMessage;

  // The matched DN for this result.
  private String matchedDN;

  // The OID for the extended response.
  private String responseOID;

  // The set of referrals for this result.
  private String[] referralURLs;



  /**
   * Creates a new extended response with the provided information.
   *
   * @param  resultCode    The result code for this extended response.
   * @param  matchedDN     The matched DN for this extended response.
   * @param  errorMessage  The error message for this extended response.
   */
  public ExtendedResponse(int resultCode, String matchedDN, String errorMessage)
  {
    this.resultCode   = resultCode;
    this.matchedDN    = matchedDN;
    this.errorMessage = errorMessage;

    referralURLs  = null;
    responseOID   = null;
    responseValue = null;
  }



  /**
   * Creates a new extended response with the provided information.
   *
   * @param  resultCode    The result code for this extended response.
   * @param  matchedDN     The matched DN for this extended response.
   * @param  errorMessage  The error message for this extended response.
   * @param  referralURLs  The set of referral URLs for this extended response.
   */
  public ExtendedResponse(int resultCode, String matchedDN, String errorMessage,
                          String[] referralURLs)
  {
    this.resultCode   = resultCode;
    this.matchedDN    = matchedDN;
    this.errorMessage = errorMessage;
    this.referralURLs = referralURLs;

    responseOID   = null;
    responseValue = null;
  }



  /**
   * Creates a new extended response with the provided information.
   *
   * @param  resultCode     The result code for this extended response.
   * @param  matchedDN      The matched DN for this extended response.
   * @param  errorMessage   The error message for this extended response.
   * @param  referralURLs   The set of referral URLs for this extended response.
   * @param  responseOID    The OID for this extended response.
   * @param  responseValue  The value for this extended response.
   */
  public ExtendedResponse(int resultCode, String matchedDN, String errorMessage,
                          String[] referralURLs, String responseOID,
                          ASN1OctetString responseValue)
  {
    this.resultCode    = resultCode;
    this.matchedDN     = matchedDN;
    this.errorMessage  = errorMessage;
    this.referralURLs  = referralURLs;
    this.responseOID   = responseOID;
    this.responseValue = responseValue;
  }



  /**
   * Retrieves the result code for the operation.
   *
   * @return  The result code for the operation.
   */
  public int getResultCode()
  {
    return resultCode;
  }



  /**
   * Retrieves the error message for this result.
   *
   * @return  The error message for this result.
   */
  public String getErrorMessage()
  {
    return errorMessage;
  }



  /**
   * Retrieves the matched DN for this result.
   *
   * @return  The matched DN for this result.
   */
  public String getMatchedDN()
  {
    return matchedDN;
  }



  /**
   * Retrieves the set of referrals for this result.
   *
   * @return  The set of referrals for this result, or <CODE>null</CODE> if
   *          there were no referrals contained in the result.
   */
  public String[] getReferrals()
  {
    return referralURLs;
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
    elementList.add(new ASN1Enumerated(resultCode));
    elementList.add(new ASN1OctetString(matchedDN));
    elementList.add(new ASN1OctetString(errorMessage));


    if ((referralURLs != null) && (referralURLs.length > 0))
    {
      ASN1Element[] referralElements = new ASN1Element[referralURLs.length];
      for (int i=0; i < referralURLs.length; i++)
      {
        referralElements[i] = new ASN1OctetString(referralURLs[i]);
      }

      elementList.add(new ASN1Sequence(REFERRAL_TYPE, referralElements));
    }


    if (responseOID != null)
    {
      elementList.add(new ASN1OctetString(RESPONSE_OID_TYPE, responseOID));
    }


    if (responseValue != null)
    {
      responseValue.setType(RESPONSE_VALUE_TYPE);
      elementList.add(responseValue);
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
  public static ExtendedResponse decodeExtendedResponse(ASN1Element element)
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


    if ((responseElements.length < 3) || (responseElements.length > 6))
    {
      throw new ProtocolException("There must be between 3 and 6 elements in " +
                                  "an extended response sequence");
    }


    int resultCode;
    try
    {
      resultCode = responseElements[0].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode extended result code", e);
    }


    String matchedDN;
    try
    {
      matchedDN = responseElements[1].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode extended response " +
                                  "matched DN", e);
    }


    String errorMessage;
    try
    {
      errorMessage = responseElements[2].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode extended response " +
                                  "error message", e);
    }


    String[]        referralURLs  = null;
    String          responseOID   = null;
    ASN1OctetString responseValue = null;
    for (int i=3; i < responseElements.length; i++)
    {
      switch (responseElements[i].getType())
      {
        case REFERRAL_TYPE:
          try
          {
            ASN1Element[] referralElements =
                 responseElements[i].decodeAsSequence().getElements();
            referralURLs = new String[referralElements.length];
            for (int j=0; j < referralURLs.length; j++)
            {
              referralURLs[j] =
                   referralElements[j].decodeAsOctetString().getStringValue();
            }
          }
          catch (Exception e)
          {
            throw new ProtocolException("Unable to decode extended response " +
                                        "referrals", e);
          }
          break;
        case RESPONSE_OID_TYPE:
          try
          {
            responseOID =
                 responseElements[i].decodeAsOctetString().getStringValue();
          }
          catch (Exception e)
          {
            throw new ProtocolException("Unable to decode extended response " +
                                        "OID", e);
          }
          break;
        case RESPONSE_VALUE_TYPE:
          try
          {
            responseValue = responseElements[i].decodeAsOctetString();
          }
          catch (Exception e)
          {
            throw new ProtocolException("Unable to decode extended response " +
                                        "value", e);
          }
          break;
        default:
          throw new ProtocolException("Unknown extended response element " +
                                      "type " + responseElements[i].getType());
      }
    }


    // FIXME:  Add specific decoding for special extended response types
    return new ExtendedResponse(resultCode, matchedDN, errorMessage,
                                referralURLs, responseOID, responseValue);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Extended Response";
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
    buffer.append(indentBuf).append("Result Code:  ").
           append(resultCode).append(" (").
           append(LDAPResultCode.resultCodeToString(resultCode)).
           append(')').append(LDAPMessage.EOL);


    if ((matchedDN != null) && (matchedDN.length() > 0))
    {
      buffer.append(indentBuf).append("Matched DN:  ").
             append(matchedDN).append(LDAPMessage.EOL);
    }


    if ((errorMessage != null) && (errorMessage.length() > 0))
    {
      buffer.append(indentBuf).append("Error Message:  ").
             append(errorMessage).append(LDAPMessage.EOL);
    }


    if ((referralURLs != null) && (referralURLs.length > 0))
    {
      buffer.append(indentBuf).append("Referrals:").append(LDAPMessage.EOL);
      for (int i=0; i < referralURLs.length; i++)
      {
        buffer.append(indentBuf).append("    ").append(referralURLs[i]).
               append(LDAPMessage.EOL);
      }
    }


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
    scriptWriter.println("#### Extended response captured at " + new Date());
    scriptWriter.println("# Result code:  " + resultCode);

    if ((matchedDN != null) && (matchedDN.length() > 0))
    {
      scriptWriter.println("# Matched DN:  " + matchedDN);
    }

    if ((errorMessage != null) && (errorMessage.length() > 0))
    {
      scriptWriter.println("# Error message:  " + errorMessage);
    }

    if ((referralURLs != null) && (referralURLs.length > 0))
    {
      scriptWriter.println("# Referral(s):");
      for (int i=0; i < referralURLs.length; i++)
      {
        scriptWriter.println("#   " + referralURLs[i]);
      }
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

