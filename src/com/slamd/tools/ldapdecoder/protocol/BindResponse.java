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
 * This class defines an LDAP bind response, which provides the result of
 * processing a bind request.
 *
 *
 * @author   Neil A. Wilson
 */
public class BindResponse
       extends ProtocolOp
{
  /**
   * The ASN.1 type used to hold the referral component of the response.
   */
  public static final byte SERVER_SASL_CREDENTIALS_TYPE = (byte) 0x87;



  // The set of SASL credentials provided by the server for use in the bind
  // process.
  private ASN1OctetString serverSASLCredentials;

  // The result code for the operation.
  private int resultCode;

  // The error message associated with this result.
  private String errorMessage;

  // The matched DN for this result.
  private String matchedDN;

  // The set of referrals for this result.
  private String[] referrals;



  /**
   * Creates a new bind response with the provided information.
   *
   * @param  resultCode    The result code for this response.
   * @param  matchedDN     The matched DN for this response.
   * @param  errorMessage  The error message for this response.
   */
  public BindResponse(int resultCode, String matchedDN, String errorMessage)
  {
    this.resultCode   = resultCode;
    this.matchedDN    = matchedDN;
    this.errorMessage = errorMessage;

    referrals             = null;
    serverSASLCredentials = null;
  }



  /**
   * Creates a new bind response with the provided information.
   *
   * @param  resultCode    The result code for this response.
   * @param  matchedDN     The matched DN for this response.
   * @param  errorMessage  The error message for this response.
   * @param  referrals     The set of referrals for this response.
   */
  public BindResponse(int resultCode, String matchedDN, String errorMessage,
                      String[] referrals)
  {
    this.resultCode   = resultCode;
    this.matchedDN    = matchedDN;
    this.errorMessage = errorMessage;
    this.referrals    = referrals;

    serverSASLCredentials = null;
  }



  /**
   * Creates a new bind response with the provided information.
   *
   * @param  resultCode             The result code for this response.
   * @param  matchedDN              The matched DN for this response.
   * @param  errorMessage           The error message for this response.
   * @param  referrals              The set of referrals for this response.
   * @param  serverSASLCredentials  The set of credentials returned by the
   *                                server for use in the SASL authentication
   *                                process.
   */
  public BindResponse(int resultCode, String matchedDN, String errorMessage,
                      String[] referrals, ASN1OctetString serverSASLCredentials)
  {
    this.resultCode            = resultCode;
    this.matchedDN             = matchedDN;
    this.errorMessage          = errorMessage;
    this.referrals             = referrals;
    this.serverSASLCredentials = serverSASLCredentials;
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
   * Retrieves the set of credentials returned by the server for use in the SASL
   * authentication process.
   *
   * @return  The set of credentials returned by the server for use in the SASL
   *          authentication process.
   */
  public ASN1OctetString getServerSASLCredentials()
  {
    return serverSASLCredentials;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>(5);
    elementList.add(new ASN1Enumerated(resultCode));

    if (matchedDN == null)
    {
      elementList.add(new ASN1OctetString());
    }
    else
    {
      elementList.add(new ASN1OctetString(matchedDN));
    }

    if (errorMessage == null)
    {
      elementList.add(new ASN1OctetString());
    }
    else
    {
      elementList.add(new ASN1OctetString(errorMessage));
    }

    if ((referrals != null) && (referrals.length > 0))
    {
      ASN1Element[] referralElements = new ASN1Element[referrals.length];
      for (int i=0; i < referrals.length; i++)
      {
        referralElements[i] = new ASN1OctetString(referrals[i]);
      }

      elementList.add(new ASN1Sequence(REFERRAL_TYPE, referralElements));
    }

    if (serverSASLCredentials != null)
    {
      serverSASLCredentials.setType(SERVER_SASL_CREDENTIALS_TYPE);
      elementList.add(serverSASLCredentials);
    }


    ASN1Element[] bindResponseElements = new ASN1Element[elementList.size()];
    elementList.toArray(bindResponseElements);
    return new ASN1Sequence(BIND_RESPONSE_TYPE, bindResponseElements);
  }



  /**
   * Decodes the provided ASN.1 element as a bind response protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded bind response.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a bind response.
   */
  public static BindResponse decodeBindResponse(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] bindResponseElements;
    try
    {
      bindResponseElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode bind response sequence", e);
    }


    if ((bindResponseElements.length < 3) || (bindResponseElements.length > 5))
    {
      throw new ProtocolException("There must be between 3 and 5 elements in " +
                                  "a bind response sequence");
    }


    int resultCode;
    try
    {
      resultCode = bindResponseElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode bind result code", e);
    }


    String matchedDN;
    try
    {
      matchedDN =
           bindResponseElements[1].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode bind result matched DN", e);
    }


    String errorMessage;
    try
    {
      errorMessage =
           bindResponseElements[2].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode bind result error message",
                                  e);
    }


    String[]        referrals             = null;
    ASN1OctetString serverSASLCredentials = null;
    if (bindResponseElements.length >= 4)
    {
      byte type = bindResponseElements[3].getType();
      if (type == REFERRAL_TYPE)
      {
        try
        {
          ASN1Element[] referralElements =
               bindResponseElements[3].decodeAsSequence().getElements();
          referrals = new String[referralElements.length];
          for (int i=0; i < referrals.length; i++)
          {
            referrals[i] =
                 referralElements[i].decodeAsOctetString().getStringValue();
          }
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode referral elements", e);
        }
      }
      else if (type == SERVER_SASL_CREDENTIALS_TYPE)
      {
        try
        {
          serverSASLCredentials = bindResponseElements[3].decodeAsOctetString();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode server SASL " +
                                      "credentials", e);
        }
      }
      else
      {
        throw new ProtocolException("Unsupported bind response element type " +
                                    type);
      }
    }


    if (bindResponseElements.length == 5)
    {
      try
      {
        serverSASLCredentials = bindResponseElements[4].decodeAsOctetString();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode server SASL credentials",
                                    e);
      }
    }


    return new BindResponse(resultCode, matchedDN, errorMessage, referrals,
                            serverSASLCredentials);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Bind Response";
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

    if (serverSASLCredentials != null)
    {
      byte[] credentialsBytes = serverSASLCredentials.getValue();
      buffer.append(indentBuf).append("Server SASL Credentials:").
             append(LDAPMessage.EOL);
      buffer.append(LDAPMessage.byteArrayToString(credentialsBytes,
                                                  (indent+4)));
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
    scriptWriter.println("#### Bind response captured at " + new Date());
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

