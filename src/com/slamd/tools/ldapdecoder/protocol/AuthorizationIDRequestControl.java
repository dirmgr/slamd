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



/**
 * This class defines the LDAP authorization identity request control, which may
 * be included in a bind request to ask that the server include the
 * authorization ID in the bind response.
 *
 *
 * @author   Neil A. Wilson
 */
public class AuthorizationIDRequestControl
       extends LDAPControl
{
  /**
   * The OID of the authorization identity request control.
   */
  public static final String AUTHORIZATION_ID_REQUEST_OID =
                                  "2.16.840.1.113730.3.4.16";



  /**
   * Creates a new authorization ID request control.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   */
  public AuthorizationIDRequestControl(boolean isCritical)
  {
    super(AUTHORIZATION_ID_REQUEST_OID, isCritical, null);
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
    buffer.append(indentBuf).append("LDAP Authorization Identity Request " +
                                    "Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);

    return buffer.toString();
  }
}

