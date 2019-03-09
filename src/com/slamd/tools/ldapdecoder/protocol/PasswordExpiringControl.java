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
 * This class defines the password expiring control, which is returned whenever
 * a user attempts to bind with a password that will expire in the near future.
 *
 *
 * @author   Neil A. Wilson
 */
public class PasswordExpiringControl
       extends LDAPControl
{
  /**
   * The OID of the password expiring control.
   */
  public static final String PASSWORD_EXPIRING_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.5";



  // The length of time in seconds until the password expires.
  private int secondsUntilExpiration;



  /**
   * Creates a new password expiring control.
   *
   * @param  isCritical              Indicates whether this control should be
   *                                 marked critical.
   * @param  secondsUntilExpiration  Specifies the number of seconds until the
   *                                 password is actually expired.
   */
  public PasswordExpiringControl(boolean isCritical, int secondsUntilExpiration)
  {
    super(PASSWORD_EXPIRING_CONTROL_OID, isCritical,
          new ASN1OctetString(String.valueOf(secondsUntilExpiration)));

    this.secondsUntilExpiration = secondsUntilExpiration;
  }



  /**
   * Creates a new password expiring control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public PasswordExpiringControl(boolean isCritical,
                                 ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(PASSWORD_EXPIRING_CONTROL_OID, isCritical, controlValue);

    try
    {
      secondsUntilExpiration = Integer.parseInt(controlValue.getStringValue());
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode control value to " +
                                  "determine length of time until expiration",
                                  e);
    }
  }



  /**
   * Retrieves the length of time in seconds until the password actually
   * expires.
   *
   * @return  The length of time in seconds until the password actually expires.
   */
  public int getSecondsUntilExpiration()
  {
    return secondsUntilExpiration;
  }



  /**
   * Retrieves a human-readable string containing the length of time until the
   * password expires.
   *
   * @return  A human-readable string containing the length of time until the
   *          password expires.
   */
  public String getTimeToExpiration()
  {
    StringBuilder buffer = new StringBuilder();

    boolean printOutput         = false;
    int     secondsRemaining    = secondsUntilExpiration;
    int     daysUntilExpiration = secondsRemaining / 86400;
    secondsRemaining -= (daysUntilExpiration * 86400);
    if (daysUntilExpiration > 0)
    {
      printOutput = true;
      buffer.append(daysUntilExpiration).append(" days, ");
    }

    int hoursRemaining = secondsRemaining / 3600;
    secondsRemaining -= (hoursRemaining * 3600);
    if (printOutput || (hoursRemaining > 0))
    {
      printOutput = true;
      buffer.append(hoursRemaining).append(" hours, ");
    }

    int minutesRemaining = secondsRemaining / 60;
    secondsRemaining -= (minutesRemaining * 60);
    if (printOutput || (minutesRemaining > 0))
    {
      printOutput = true;
      buffer.append(minutesRemaining).append(" minutes, ");
    }

    buffer.append(secondsRemaining).append(" seconds");
    return buffer.toString();
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
    buffer.append(indentBuf).append("LDAP Password Expiring Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Time Until Expiration:  ").
           append(getTimeToExpiration()).append(LDAPMessage.EOL);

    return buffer.toString();
  }
}

