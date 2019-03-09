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
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP modify response, which provides information about
 * the result of processing a modify request.
 *
 *
 * @author   Neil A. Wilson
 */
public class ModifyResponse
       extends ProtocolOp
{
  // The result code for the operation.
  private int resultCode;

  // The error message associated with this result.
  private String errorMessage;

  // The matched DN for this result.
  private String matchedDN;

  // The set of referrals for this result.
  private String[] referrals;



  /**
   * Creates a new modify response protocol op with the provided information.
   *
   * @param  resultCode    The result code for this response.
   * @param  matchedDN     The matched DN for this response.
   * @param  errorMessage  The error message for this response.
   */
  public ModifyResponse(int resultCode, String matchedDN, String errorMessage)
  {
    this.resultCode   = resultCode;
    this.matchedDN    = matchedDN;
    this.errorMessage = errorMessage;

    referrals = null;
  }



  /**
   * Creates a new modify response protocol op with the provided information.
   *
   * @param  resultCode    The result code for this response.
   * @param  matchedDN     The matched DN for this response.
   * @param  errorMessage  The error message for this response.
   * @param  referrals     The set of referrals for this response.
   */
  public ModifyResponse(int resultCode, String matchedDN, String errorMessage,
                        String[] referrals)
  {
    this.resultCode   = resultCode;
    this.matchedDN    = matchedDN;
    this.errorMessage = errorMessage;
    this.referrals    = referrals;
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
    return referrals;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] resultElements;
    if ((referrals == null) || (referrals.length == 0))
    {
      resultElements = new ASN1Element[]
      {
        new ASN1Enumerated(resultCode),
        new ASN1OctetString(matchedDN),
        new ASN1OctetString(errorMessage)
      };
    }
    else
    {
      ASN1Element[] referralElements = new ASN1Element[referrals.length];
      for (int i=0; i < referralElements.length; i++)
      {
        referralElements[i] = new ASN1OctetString(referrals[i]);
      }

      resultElements = new ASN1Element[]
      {
        new ASN1Enumerated(resultCode),
        new ASN1OctetString(matchedDN),
        new ASN1OctetString(errorMessage),
        new ASN1Sequence(REFERRAL_TYPE, referralElements)
      };
    }


    return new ASN1Sequence(MODIFY_RESPONSE_TYPE, resultElements);
  }



  /**
   * Decodes the provided ASN.1 element as a modify response protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded modify response.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a modify response.
   */
  public static ModifyResponse decodeModifyResponse(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] resultElements;
    try
    {
      resultElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode result sequence", e);
    }


    if ((resultElements.length < 3) || (resultElements.length > 4))
    {
      throw new ProtocolException("There must be either 3 or 4 elements in " +
                                  "an LDAP result sequence");
    }


    int resultCode;
    try
    {
      resultCode = resultElements[0].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode result code", e);
    }


    String matchedDN;
    try
    {
      matchedDN = resultElements[1].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode matched DN for LDAP result",
                                  e);
    }


    String errorMessage;
    try
    {
      errorMessage = resultElements[2].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode error message for LDAP " +
                                  "result", e);
    }


    String[] referrals = null;
    if (resultElements.length == 4)
    {
      try
      {
        ASN1Element[] referralElements =
                           resultElements[3].decodeAsSequence().getElements();
        referrals = new String[referralElements.length];
        for (int i=0; i < referrals.length; i++)
        {
          referrals[i] =
               referralElements[i].decodeAsOctetString().getStringValue();
        }
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode referral sequence", e);
      }
    }


    return new ModifyResponse(resultCode, matchedDN, errorMessage, referrals);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Modify Response";
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

    if ((referrals != null) && (referrals.length > 0))
    {
      buffer.append(indentBuf).append("Referrals:").append(LDAPMessage.EOL);
      for (int i=0; i < referrals.length; i++)
      {
        buffer.append(indentBuf).append("    ").append(referrals[i]).
               append(LDAPMessage.EOL);
      }
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
    scriptWriter.println("#### Modify response captured at " + new Date());
    scriptWriter.println("# Result code:  " + resultCode);

    if ((matchedDN != null) && (matchedDN.length() > 0))
    {
      scriptWriter.println("# Matched DN:  " + matchedDN);
    }

    if ((errorMessage != null) && (errorMessage.length() > 0))
    {
      scriptWriter.println("# Error message:  " + errorMessage);
    }

    if ((referrals != null) && (referrals.length > 0))
    {
      scriptWriter.println("# Referral(s):");
      for (int i=0; i < referrals.length; i++)
      {
        scriptWriter.println("#   " + referrals[i]);
      }
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

