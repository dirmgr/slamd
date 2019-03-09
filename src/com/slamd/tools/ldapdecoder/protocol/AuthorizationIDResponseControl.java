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



import com.slamd.asn1.ASN1OctetString;



/**
 * This class defines the LDAP authorization identity response control, which
 * may be included in a bind response to indicate the authorization ID for the
 * bind.
 *
 *
 * @author   Neil A. Wilson
 */
public class AuthorizationIDResponseControl
       extends LDAPControl
{
  /**
   * The OID of the authorization identity response control.
   */
  public static final String AUTHORIZATION_ID_RESPONSE_OID =
                                  "2.16.840.1.113730.3.4.15";



  // The authorization ID included in this control.
  private String authzID;



  /**
   * Creates a new authorization ID response control with the provided
   * information.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   * @param  authzID     The authorization ID included in this control.
   */
  public AuthorizationIDResponseControl(boolean isCritical, String authzID)
  {
    super(AUTHORIZATION_ID_RESPONSE_OID, isCritical,
          ((authzID == null)
           ? new ASN1OctetString()
           : new ASN1OctetString(authzID)));

    this.authzID = authzID;
  }



  /**
   * Creates a new authorization ID response control by decoding the provided
   * value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public AuthorizationIDResponseControl(boolean isCritical,
                                        ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(AUTHORIZATION_ID_RESPONSE_OID, isCritical, controlValue);

    authzID = controlValue.getStringValue();
  }



  /**
   * Retrieves the authorization ID included in this control.
   *
   * @return  The authorization ID included in this control.
   */
  public String getAuthzID()
  {
    return authzID;
  }



  /**
   * Retrieves a string representation of this control with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this control with the specified indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("LDAP Authorization Identity Response " +
                                    "Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Authorization ID:  ").append(authzID).
           append(LDAPMessage.EOL);

    return buffer.toString();
  }
}

