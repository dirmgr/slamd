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
package com.slamd.jobs;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.StringTokenizer;

import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSocketFactory;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.asn1.ASN1Writer;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class provides an implementation of an LDAP socket factory that can be
 * used to perform authentication to the directory server using the DIGEST-MD5
 * SASL mechanism.  It is a relatively ugly hack because the LDAP SDK for Java
 * does not provide very good support for SASL authentication.
 * <BR><BR>
 * There are several things that should be noted about this implementation:
 * <UL>
 *   <LI>The <CODE>setAuthenticationInfo</CODE> method must be called to provide
 *       the identity and credentials of the user that is to be authenticated.
 *       This must be done before calling the <CODE>connect</CODE> method of the
 *       <CODE>LDAPConnection</CODE> object with which this socket factory is
 *       associated.</LI>
 *   <LI>When calling the <CODE>connect</CODE> method on the
 *       <CODE>LDAPConnection</CODE> object with which this socket factory is
 *       associated, you must only use the version that provides the host name
 *       and port number of the directory server.  Do not use any version that
 *       specifies the LDAP protocol version or bind information because that
 *       will perform a bind using simple authentication and will negate the
 *       effect of the DIGEST-MD5 bind.  Further, once the connection has
 *       been established, do not call any variants of the
 *       <CODE>authenticate</CODE> or <CODE>bind</CODE> methods.</LI>
 *   <LI>Because the DIGEST-MD5 authentication is performed outside of the LDAP
 *       SDK for Java, the SDK itself has no knowledge of that authentication.
 *       Therefore, methods like <CODE>getAuthenticationDN</CODE>,
 *       <CODE>getAuthenticationMethod</CODE>,
 *       <CODE>getAuthenticationPassword</CODE>, and
 *       <CODE>isAuthenticated</CODE> may not be used because they will provide
 *       an incorrect response.</LI>
 *   <LI>Because the authentication ID and credentials are provided outside the
 *       <CODE>makeSocket</CODE> method, this implementation is not threadsafe.
 *       Therefore, if it is expected that multiple threads may attempt to
 *       concurrently create connections using DIGEST-MD5 authentication, then
 *       they must each have their own instance of this socket factory.  It is
 *       not sufficient to use synchronization in an attempt to prevent
 *       concurrent usage of the same instance.</LI>
 *   <LI>It is possible to use this socket factory in conjunction with another
 *       socket factory for additional functionality (e.g., DIGEST-MD5
 *       authentication over an SSL-based connection).  To use this socket
 *       factory in conjunction with another socket factory, call the
 *       <CODE>setAdditionalSocketFactory</CODE> method to provide the
 *       additional socket factory.  The <CODE>makeSocket</CODE> method of that
 *       socket factory will be invoked as part of the <CODE>makeSocket</CODE>
 *       method of this socket factory.  Note that some socket factory
 *       implementations may not behave as expected when used in this
 *       manner.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPDigestMD5SocketFactory
       implements LDAPSocketFactory
{
  /**
   * The set of characters that will be used to generate the cnonce.
   */
  public static final char[] CNONCE_ALPHABET =
       ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "1234567890+/").toCharArray();



  /**
   * The algorithm used by JCE to perform MD5 hashing.
   */
  public static final String JCE_DIGEST_ALGORITHM = "MD5";



  /**
   * The ASN.1 type used to denote an LDAP bind request protocol op.
   */
  public static final byte LDAP_BIND_REQUEST_TYPE = 0x60;



  /**
   * The ASN.1 type used to denote an LDAP bind response protocol op.
   */
  public static final byte LDAP_BIND_RESPONSE_TYPE = 0x61;



  /**
   * The ASN.1 type used to denote the SASL credentials in an LDAP bind request.
   */
  public static final byte LDAP_SASL_CREDENTIALS_TYPE = (byte) 0xA3;



  /**
   * The ASN.1 type used to denote the SASL credentials in an LDAP bind
   * response.
   */
  public static final byte LDAP_SERVER_SASL_CREDENTIALS_TYPE = (byte) 0x87;



  /**
   * The quality of protection that will be used for all authentications.  This
   * implementation does not support either integrity or confidentiality.
   */
  public static final String QOP_AUTH = "auth";



  /**
   * The name of the DIGEST-MD5 SASL mechanism as it must appear in LDAP bind
   * requests.
   */
  public static final String SASL_MECHANISM_NAME = "DIGEST-MD5";



  // An additional socket factory that can be used to create the connection with
  // some additional layer of security (e.g., SSL/TLS).
  private LDAPSocketFactory socketFactory;

  // The object used to create MD5 digests.
  private MessageDigest md5Digest;

  // The random number generator used to create cnonce values.
  private SecureRandom random;

  // The authentication ID that should be used when performing the
  // authentication.
  private String authID;

  // The clear-text password that should be used when performing the
  // authentication.
  private String password;



  /**
   * Creates a new instance of this DIGEST-MD5 authenticator.  Note that
   * creating an instance of this class for the first time in the life of the
   * JVM can take a few seconds because of the time required to initialize the
   * entropy for the random number generator.
   *
   * @throws  SLAMDException  If a problem occurs while initializing this
   *                          DIGEST-MD5 authenticator.
   */
  public LDAPDigestMD5SocketFactory()
         throws SLAMDException
  {
    try
    {
      md5Digest = MessageDigest.getInstance(JCE_DIGEST_ALGORITHM);
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to initialize the MD5 digester:  " + e,
                               e);
    }

    random = new SecureRandom();

    authID        = null;
    password      = null;
    socketFactory = null;
  }



  /**
   * Specifies the authentication ID and password for use with the next
   * connection.
   *
   * @param  authID    The authentication ID for use with the next connection.
   * @param  password  The password for use with the next connection.
   */
  public void setAuthenticationInfo(String authID, String password)
  {
    this.authID   = authID;
    this.password = password;
  }



  /**
   * Specifies an additional socket factory that should be used when creating
   * connections to the directory server using this socket factory.  This makes
   * it possible to stack this socket factory on top of another one, which
   * allows for things like using DIGEST-MD5 on top of an SSL-based connection.
   *
   * @param  socketFactory  The additional socket factory that should be used
   *                        when creating connections to the directory server
   *                        using this socket factory.
   */
  public void setAdditionalSocketFactory(LDAPSocketFactory socketFactory)
  {
    this.socketFactory = socketFactory;
  }



  /**
   * Establishes a new connection to the directory server and performs a SASL
   * bind using DIGEST-MD5 before handing the socket off to the Java SDK.
   *
   * @param  host  The address of the server to which the connection should be
   *               established.
   * @param  port  The port number of the server to which the connection should
   *               be established.
   *
   * @return  The socket that may be used to communicate with the directory
   *          server.
   *
   * @throws  LDAPException  If a problem occurs while creating the socket.
   */
  public Socket makeSocket(String host, int port)
         throws LDAPException
  {
    if ((authID == null) || (password == null))
    {
      throw new LDAPException("Authentication ID and/or password has not been" +
                              "specified.", LDAPException.PARAM_ERROR);
    }


    Socket socket;
    if (socketFactory == null)
    {
      try
      {
        socket = new Socket(host, port);
      }
      catch (IOException ioe)
      {
        throw new LDAPException("Unable to connect to " + host + ':' + port +
                                " -- " + ioe, LDAPException.CONNECT_ERROR);
      }
    }
    else
    {
      socket = socketFactory.makeSocket(host, port);
    }


    // Tap into the input and output streams and use them to create an ASN.1
    // reader and writer.
    InputStream  inputStream;
    OutputStream outputStream;
    ASN1Reader   asn1Reader;
    ASN1Writer   asn1Writer;
    try
    {
      inputStream  = socket.getInputStream();
      outputStream = socket.getOutputStream();
      asn1Reader   = new ASN1Reader(inputStream);
      asn1Writer   = new ASN1Writer(outputStream);
    }
    catch (IOException ioe)
    {
      throw new LDAPException("Unable to get input and/or output stream -- " +
                              ioe, LDAPException.CONNECT_ERROR);
    }


    // Bind the connection to the directory server.
    try
    {
      doBind(asn1Reader, asn1Writer, host, authID, password);
    }
    catch (LDAPException le)
    {
      throw le;
    }
    catch (Exception e)
    {
      throw new LDAPException("Internal failure while processing the bind:   " +
                              e);
    }


    // Return the socket to the caller.
    return socket;
  }



  /**
   * Handles the process of actually performing the bind.
   *
   * @param  asn1Reader  The ASN.1 reader used top read responses from the
   *                     server.
   * @param  asn1Writer  The ASN.1 writer used to write requests to the server.
   * @param  host        The address of the directory server, used to construct
   *                     the digest-uri field for the authentication.
   * @param  authID      The authentication ID of the user that is to perform
   *                     the bind.  It is generally in the form "dn:{userdn}".
   * @param  password    The password for the user indicated in the auth ID.
   *
   * @throws  LDAPException  If any problem occurs while processing the bind.
   */
  private void doBind(ASN1Reader asn1Reader, ASN1Writer asn1Writer, String host,
                      String authID, String password)
          throws LDAPException
  {
    // First, create the LDAP message for the bind request.
    ASN1Element[] saslCredentialElements =
    {
      new ASN1OctetString(SASL_MECHANISM_NAME),
      new ASN1OctetString()
    };

    ASN1Element[] bindRequestElements =
    {
      new ASN1Integer(3),
      new ASN1OctetString(),
      new ASN1Sequence(LDAP_SASL_CREDENTIALS_TYPE, saslCredentialElements)
    };

    ASN1Element[] ldapMessageElements =
    {
      new ASN1Integer(1),
      new ASN1Sequence(LDAP_BIND_REQUEST_TYPE, bindRequestElements)
    };

    ASN1Element messageElement = new ASN1Sequence(ldapMessageElements);


    // Send the request to the server.
    try
    {
      asn1Writer.writeElement(messageElement);
    }
    catch (IOException ioe)
    {
      throw new LDAPException("Unable to send the initial bind request to " +
                              "the server:  " + ioe,
                              LDAPException.CONNECT_ERROR);
    }


    // Read the response from the server.
    ASN1Element responseElement;
    try
    {
      responseElement =
           asn1Reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
    }
    catch (ASN1Exception ae)
    {
      throw new LDAPException("Unable to decode the initial bind response " +
                              "from the server:  " + ae,
                              LDAPException.UNAVAILABLE);
    }
    catch (IOException ioe)
    {
      throw new LDAPException("Unable to read the initial bind response " +
                              "from the server:  " + ioe,
                              LDAPException.CONNECT_ERROR);
    }


    // Decode the element as a bind response.
    String responseData = null;
    try
    {
      ASN1Element[] elements = responseElement.decodeAsSequence().getElements();
      if (elements.length != 2)
      {
        throw new LDAPException("Unable to decode the initial bind response " +
                                "from the server:  response element had an " +
                                "invalid number of elements.",
                                LDAPException.UNAVAILABLE);
      }

      if (elements[1].getType() != LDAP_BIND_RESPONSE_TYPE)
      {
        throw new LDAPException("Unable to decode the initial bind response " +
                                "from the server:  response element had an " +
                                "invalid protocol op type.",
                                LDAPException.UNAVAILABLE);
      }

      elements = elements[1].decodeAsSequence().getElements();
      int resultCode = elements[0].decodeAsEnumerated().getIntValue();
      if (resultCode != LDAPException.SASL_BIND_IN_PROGRESS)
      {
        throw new LDAPException("Unable to decode the initial bind response " +
                                "from the server:  inappropriate result code.",
                                LDAPException.UNAVAILABLE);
      }

      for (int i=1; i < elements.length; i++)
      {
        if (elements[i].getType() == LDAP_SERVER_SASL_CREDENTIALS_TYPE)
        {
          responseData = elements[i].decodeAsOctetString().getStringValue();
        }
      }

      if (responseData == null)
      {
        throw new LDAPException("Unable to decode the initial bind response " +
                                "from the server:  could not obtain the " +
                                "server SASL credentials.",
                                LDAPException.UNAVAILABLE);
      }
    }
    catch (ASN1Exception ae)
    {
      throw new LDAPException("Unable to decode the initial bind response " +
                              "from the server:  " + ae,
                              LDAPException.UNAVAILABLE);
    }


    // Parse the response data.  We need to get the nonce, the realm, and the
    // character set.
    StringTokenizer tokenizer = new StringTokenizer(responseData, ",");
    String nonce   = null;
    String realm   = null;
    String charSet = "utf-8";
    while (tokenizer.hasMoreTokens())
    {
      String token      = tokenizer.nextToken();
      int    equalPos   = token.indexOf('=');
      String tokenName  = token.substring(0, equalPos).toLowerCase();
      String tokenValue = token.substring(equalPos+1);
      if (tokenValue.startsWith("\""))
      {
        tokenValue = tokenValue.substring(1, (tokenValue.length() - 1));
      }

      if (tokenName.equals("nonce"))
      {
        nonce = tokenValue;
      }
      else if (tokenName.equals("realm"))
      {
        realm = tokenValue;
      }
      else if (tokenName.equals("charset"))
      {
        charSet = tokenValue;
      }
    }


    // Make sure that at least the nonce and the realm were provided.
    if ((nonce == null) || (nonce.length() == 0))
    {
      throw new LDAPException("Unable to decode the initial bind response " +
                              "from the server:  could not extract the nonce " +
                              "from the server SASL credentials.",
                              LDAPException.UNAVAILABLE);
    }
    else if ((realm == null) || (realm.length() == 0))
    {
      throw new LDAPException("Unable to decode the initial bind response " +
                              "from the server:  could not extract the realm " +
                              "from the server SASL credentials.",
                              LDAPException.UNAVAILABLE);
    }


    // At this point, we should have enough information to generate the
    // response.  Create values for the remaining response fields.
    String cnonce     = generateCNonce(Math.max(32, nonce.length()));
    String nonceCount = "00000001";
    String qop        = "auth";
    String digestURI  = "ldap/" + host;

    String response;
    try
    {
      response = generateResponse(authID, password, realm, nonce, cnonce,
                                  nonceCount, digestURI, charSet);
    }
    catch (Exception e)
    {
      throw new LDAPException("Internal failure while generating the " +
                              "response value to send to the server:  " + e,
                              LDAPException.UNAVAILABLE);
    }


    // Assemble the full response to return to the server.
    String responseStr = "username=\"" + authID + "\",realm=\"" + realm +
                         "\",nonce=\"" + nonce + "\",cnonce=\"" + cnonce +
                         "\",nc=" + nonceCount + ",qop=" + qop +
                         ",digest-uri=\"" + digestURI + "\",response=" +
                         response;


    // Assemble the new bind request message.
    saslCredentialElements = new ASN1Element[]
    {
      new ASN1OctetString(SASL_MECHANISM_NAME),
      new ASN1OctetString(responseStr)
    };

    bindRequestElements = new ASN1Element[]
    {
      new ASN1Integer(3),
      new ASN1OctetString(),
      new ASN1Sequence(LDAP_SASL_CREDENTIALS_TYPE, saslCredentialElements)
    };

    ldapMessageElements = new ASN1Element[]
    {
      new ASN1Integer(2),
      new ASN1Sequence(LDAP_BIND_REQUEST_TYPE, bindRequestElements)
    };

    messageElement = new ASN1Sequence(ldapMessageElements);


    // Send the bind request to the directory server.
    try
    {
      asn1Writer.writeElement(messageElement);
    }
    catch (IOException ioe)
    {
      throw new LDAPException("Unable to send the subsequent bind request to " +
                              "the server:  " + ioe,
                              LDAPException.CONNECT_ERROR);
    }


    // Read the response from the server.
    try
    {
      responseElement =
           asn1Reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
    }
    catch (ASN1Exception ae)
    {
      throw new LDAPException("Unable to decode the subsequent bind response " +
                              "from the server:  " + ae,
                              LDAPException.UNAVAILABLE);
    }
    catch (IOException ioe)
    {
      throw new LDAPException("Unable to read the subsequent bind response " +
                              "from the server:  " + ioe,
                              LDAPException.CONNECT_ERROR);
    }


    // Decode the element as a bind response.
    try
    {
      ASN1Element[] elements = responseElement.decodeAsSequence().getElements();
      if (elements.length != 2)
      {
        throw new LDAPException("Unable to decode the subsequent bind " +
                                "response from the server:  response element " +
                                "had an invalid number of elements.",
                                LDAPException.UNAVAILABLE);
      }

      if (elements[1].getType() != LDAP_BIND_RESPONSE_TYPE)
      {
        throw new LDAPException("Unable to decode the subsequent bind " +
                                "response from the server:  response element " +
                                "had an invalid protocol op type.",
                                LDAPException.UNAVAILABLE);
      }

      elements = elements[1].decodeAsSequence().getElements();
      int resultCode = elements[0].decodeAsEnumerated().getIntValue();
      if (resultCode == LDAPException.SUCCESS)
      {
        return;
      }


      String matchedDN    = elements[1].decodeAsOctetString().getStringValue();
      String errorMessage = elements[2].decodeAsOctetString().getStringValue();
      throw new LDAPException("The bind attempt was not successful.",
                              resultCode, errorMessage, matchedDN);
    }
    catch (ASN1Exception ae)
    {
      throw new LDAPException("Unable to decode the subsequent bind response " +
                              "from the server:  " + ae,
                              LDAPException.UNAVAILABLE);
    }
  }



  /**
   * Generates the cnonce string that will be used for a request.
   *
   * @param  length  The number of characters to include in the cnonce.
   *
   * @return  The generated cnonce string.
   */
  private String generateCNonce(int length)
  {
    char[] cnonceChars = new char[length];

    for (int i=0; i < cnonceChars.length; i++)
    {
      cnonceChars[i] = CNONCE_ALPHABET[(random.nextInt() & 0x7FFFFFFF) %
                                       CNONCE_ALPHABET.length];
    }

    return new String(cnonceChars);
  }



  /**
   * Generates the appropriate DIGEST-MD5 response based on the provided
   * information, as per the specification in RFC 2831.
   *
   * @param  authID      The authentication ID for the user.
   * @param  password    The password for the user indicated by the auth ID.
   * @param  realm       The realm for the user indicated by the auth ID.
   * @param  nonce       The server-generated random string used in the digest.
   * @param  cnonce      The client-generated random string used in the digest.
   * @param  nonceCount  The number of times the provided nonce has been used by
   *                     the client.
   * @param  digestURI   The URI that specifies the principal name of the
   *                     service in which the authentication is being performed.
   * @param  charset     The character set to use when encoding the data.
   *
   * @return  The generated DIGEST-MD5 response.
   *
   * @throws  UnsupportedEncodingException  If the specified character set is
   *                                        unsupported.
   */
  private String generateResponse(String authID, String password, String realm,
                                  String nonce, String cnonce,
                                  String nonceCount, String digestURI,
                                  String charset)
          throws UnsupportedEncodingException
  {
    String a1Str1   = authID + ':' + realm + ':' + password;
    byte[] a1bytes1 = md5Digest.digest(a1Str1.getBytes(charset));

    String a1Str2 = ':' + nonce + ':' + cnonce;
    byte[] a1bytes2 = a1Str2.getBytes(charset);

    byte[] a1 = new byte[a1bytes1.length + a1bytes2.length];
    System.arraycopy(a1bytes1, 0, a1, 0, a1bytes1.length);
    System.arraycopy(a1bytes2, 0, a1, a1bytes1.length, a1bytes2.length);

    byte[] a2 = ("AUTHENTICATE:" + digestURI).getBytes(charset);

    String hexHashA1 = getHexString(md5Digest.digest(a1));
    String hexHashA2 = getHexString(md5Digest.digest(a2));

    String kdStr = hexHashA1 + ':' + nonce + ':' + nonceCount + ':' + cnonce +
                   ':' + QOP_AUTH + ':' + hexHashA2;
    return getHexString(md5Digest.digest(kdStr.getBytes(charset)));
  }



  /**
   * Encodes the provided byte array into a string of the hexadecimal digits
   * corresponding to the values in the array.  All the alphabetic hex digits
   * (a through f) will be return in lowercase.
   *
   * @param  bytes  The byte array to be encoded.
   *
   * @return  The hexadecimal string representation of the provided byte array.
   */
  private String getHexString(byte[] bytes)
  {
    StringBuilder buffer = new StringBuilder(2 * bytes.length);
    for (int i=0; i < bytes.length; i++)
    {
      switch ((bytes[i] >> 4) & 0x0F)
      {
        case 0x00:
          buffer.append('0');
          break;
        case 0x01:
          buffer.append('1');
          break;
        case 0x02:
          buffer.append('2');
          break;
        case 0x03:
          buffer.append('3');
          break;
        case 0x04:
          buffer.append('4');
          break;
        case 0x05:
          buffer.append('5');
          break;
        case 0x06:
          buffer.append('6');
          break;
        case 0x07:
          buffer.append('7');
          break;
        case 0x08:
          buffer.append('8');
          break;
        case 0x09:
          buffer.append('9');
          break;
        case 0x0a:
          buffer.append('a');
          break;
        case 0x0b:
          buffer.append('b');
          break;
        case 0x0c:
          buffer.append('c');
          break;
        case 0x0d:
          buffer.append('d');
          break;
        case 0x0e:
          buffer.append('e');
          break;
        case 0x0f:
          buffer.append('f');
          break;
      }

      switch (bytes[i] & 0x0F)
      {
        case 0x00:
          buffer.append('0');
          break;
        case 0x01:
          buffer.append('1');
          break;
        case 0x02:
          buffer.append('2');
          break;
        case 0x03:
          buffer.append('3');
          break;
        case 0x04:
          buffer.append('4');
          break;
        case 0x05:
          buffer.append('5');
          break;
        case 0x06:
          buffer.append('6');
          break;
        case 0x07:
          buffer.append('7');
          break;
        case 0x08:
          buffer.append('8');
          break;
        case 0x09:
          buffer.append('9');
          break;
        case 0x0a:
          buffer.append('a');
          break;
        case 0x0b:
          buffer.append('b');
          break;
        case 0x0c:
          buffer.append('c');
          break;
        case 0x0d:
          buffer.append('d');
          break;
        case 0x0e:
          buffer.append('e');
          break;
        case 0x0f:
          buffer.append('f');
          break;
      }
    }

    return buffer.toString();
  }
}

