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
 * This class defines the second version of the proxied authorization control,
 * which is used to perform an operation under the authority of one user while
 * authenticated as another.
 *
 *
 * @author   Neil A. Wilson
 */
public class ProxiedAuthV2Control
       extends LDAPControl
{
  /**
   * The OID of the proxied auth v2 control.
   */
  public static final String PROXIED_AUTH_V2_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.18";



  // The authorization ID that specifies the user under whose authority the
  // requested operation should be performed.
  private String authzID;



  /**
   * Creates a new proxied auth v2 control with the provided information.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   * @param  authzID     The authorization ID that specifies the user under
   *                     whose authority the requested operation should be
   *                     performed.
   */
  public ProxiedAuthV2Control(boolean isCritical, String authzID)
  {
    super(PROXIED_AUTH_V2_CONTROL_OID, isCritical,
          ((authzID == null)
           ? new ASN1OctetString(new ASN1OctetString().encode())
           : new ASN1OctetString(new ASN1OctetString(authzID).encode())));

    this.authzID = authzID;
  }



  /**
   * Creates a new proxied auth v2 control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public ProxiedAuthV2Control(boolean isCritical, ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(PROXIED_AUTH_V2_CONTROL_OID, isCritical, controlValue);

    authzID = controlValue.getStringValue();
  }



  /**
   * Retrieves the authorization ID that specifies the user under whose
   * authority the requested operation should be performed.
   *
   * @return  The authorization ID that specifies the user under whose
   *          authority the requested operation should be performed.
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
    buffer.append(indentBuf).append("LDAP Proxied Authorization (v2) Control").
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

