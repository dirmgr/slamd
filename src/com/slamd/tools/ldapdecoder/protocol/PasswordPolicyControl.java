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



import java.util.ArrayList;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the password policy control, which is defined in
 * draft-behera-ldap-password-policy.
 *
 *
 * @author   Neil A. Wilson
 */
public class PasswordPolicyControl
       extends LDAPControl
{
  /**
   * The OID of the password policy control.
   */
  public static final String PASSWORD_POLICY_CONTROL_OID =
                                  "1.3.6.1.4.1.42.2.27.8.5.1";



  /**
   * The BER type for the warning value element.
   */
  public static final byte TYPE_WARNING = (byte) 0x80;



  /**
   * The BER type for the error value element.
   */
  public static final byte TYPE_ERROR = (byte) 0x81;



  /**
   * The warning type that will be used to specify the length of time in seconds
   * until expiration.
   */
  public static final byte WARNING_TYPE_TIME_BEFORE_EXPIRATION = (byte) 0x80;



  /**
   * The warning type that will be used to specify the number of grace logins
   * remaining.
   */
  public static final byte WARNING_TYPE_GRACE_AUTHS_REMAINING = (byte) 0x81;



  /**
   * The error type that will be used to indicate that the password is expired.
   */
  public static final int ERROR_TYPE_PASSWORD_EXPIRED = 0;



  /**
   * The error type that will be used to indicate that the account is locked.
   */
  public static final int ERROR_TYPE_ACCOUNT_LOCKED = 1;



  /**
   * The error type that will be used to indicate that the password has been
   * reset and must be changed.
   */
  public static final int ERROR_TYPE_CHANGE_AFTER_RESET = 2;



  /**
   * The error type that will be used to indicate that password changes are not
   * allowed.
   */
  public static final int ERROR_TYPE_PASSWORD_MOD_NOT_ALLOWED = 3;



  /**
   * The error type that will be used to indicate that the current password must
   * be provided when choosing a new password.
   */
  public static final int ERROR_TYPE_MUST_SUPPLY_OLD_PASSWORD = 4;



  /**
   * The error type that will be used to indicate that the provided password is
   * unacceptable.
   */
  public static final int ERROR_TYPE_INSUFFICIENT_PASSWORD_QUALITY = 5;



  /**
   * The error type that will be used to indicate that the provided password is
   * too short.
   */
  public static final int ERROR_TYPE_PASSWORD_TOO_SHORT = 6;



  /**
   * The error type that will be used to indicate that the password is too
   * young.
   */
  public static final int ERROR_TYPE_PASSWORD_TOO_YOUNG = 7;



  /**
   * The error type that will be used to indicate that the password is in the
   * password history.
   */
  public static final int ERROR_TYPE_PASSWORD_IN_HISTORY = 8;



  // The warning type for this password policy control.
  private byte warningType;

  // The error type for this password policy control.
  private int errorType;

  // The warning value for this password policy control.
  private int warningValue;



  /**
   * Creates a new password policy control with no value, which will be suitable
   * for use in a request.
   *
   * @param  isCritical  Indicates whether the control should be marked
   *                     critical.
   */
  public PasswordPolicyControl(boolean isCritical)
  {
    super(PASSWORD_POLICY_CONTROL_OID, isCritical, null);

    warningType  = (byte) -1;
    errorType    = -1;
    warningValue = -1;
  }



  /**
   * Creates a new password policy control with a value containing possible
   * warning and/or error components.
   *
   * @param  isCritical    Indicates whether the controls should be marked
   *                       critical.
   * @param  warningType   The warning type for this control, or -1 if there
   *                       should not be a warning type.
   * @param  warningValue  The value associated with the warning, if any.
   * @param  errorType     The error type for this control, or -1 if there
   *                       should not be an error type.
   */
  public PasswordPolicyControl(boolean isCritical, byte warningType,
                               int warningValue, int errorType)
  {
    super(PASSWORD_POLICY_CONTROL_OID, isCritical,
          encodeValue(warningType, warningValue, errorType));

    this.warningType  = warningType;
    this.warningValue = warningValue;
    this.errorType    = errorType;
  }



  /**
   * Creates a new password policy control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the control
   *                             value.
   */
  public PasswordPolicyControl(boolean isCritical, ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(PASSWORD_POLICY_CONTROL_OID, isCritical, controlValue);


    warningType  = (byte) -1;
    warningValue = -1;
    errorType    = -1;


    ASN1Element[] sequenceElements;
    try
    {
      sequenceElements =
           ASN1Element.decodeAsSequence(controlValue.getValue()).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode password policy control " +
                                  "value sequence", e);
    }

    if (sequenceElements.length > 2)
    {
      throw new ProtocolException("There must be no more than two elements " +
                                  "in a password policy control value " +
                                  "sequence.");
    }


    for (int i=0; i < sequenceElements.length; i++)
    {
      if (sequenceElements[i].getType() == TYPE_WARNING)
      {
        try
        {
          ASN1Integer intElement =
               ASN1Element.decodeAsInteger(sequenceElements[i].getValue());
          if ((intElement.getType() == WARNING_TYPE_TIME_BEFORE_EXPIRATION) ||
              (intElement.getType() == WARNING_TYPE_GRACE_AUTHS_REMAINING))
          {
            warningType  = intElement.getType();
            warningValue = intElement.getIntValue();
          }
          else
          {
            throw new ProtocolException("Unable to decode the password " +
                                        "policy control value because the " +
                                        "warning element had an invalid type " +
                                        "of " + intElement.getType());
          }
        }
        catch (ProtocolException pe)
        {
          throw pe;
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode the password policy " +
                                      "control value sequence because an " +
                                      "error occurred while parsing the " +
                                      "warning element:  " + e);
        }
      }
      else if (sequenceElements[i].getType() == TYPE_ERROR)
      {
        try
        {
          ASN1Enumerated enumeratedElement =
               sequenceElements[i].decodeAsEnumerated();
          switch (enumeratedElement.getIntValue())
          {
            case ERROR_TYPE_PASSWORD_EXPIRED:
            case ERROR_TYPE_ACCOUNT_LOCKED:
            case ERROR_TYPE_CHANGE_AFTER_RESET:
            case ERROR_TYPE_PASSWORD_MOD_NOT_ALLOWED:
            case ERROR_TYPE_MUST_SUPPLY_OLD_PASSWORD:
            case ERROR_TYPE_INSUFFICIENT_PASSWORD_QUALITY:
            case ERROR_TYPE_PASSWORD_TOO_SHORT:
            case ERROR_TYPE_PASSWORD_TOO_YOUNG:
            case ERROR_TYPE_PASSWORD_IN_HISTORY:
              errorType = enumeratedElement.getIntValue();
              break;
            default:
              throw new ProtocolException("Unable to decode the password " +
                                          "policy control value because the " +
                                          "error element had an invalid " +
                                          "value of " +
                                          enumeratedElement.getIntValue());
          }
        }
        catch (ProtocolException pe)
        {
          throw pe;
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode the password policy " +
                                      "control value sequence because an " +
                                      "error occurred while parsing the " +
                                      "error element:  " + e);
        }
      }
      else
      {
        throw new ProtocolException("Unable to decode the password policy " +
                                    "control value sequence because it had " +
                                    "an element with an invalid type of " +
                                    sequenceElements[i].getType());
      }
    }
  }



  /**
   * Encodes the provided information into an octet string suitable for use as
   * the value of this control.
   *
   * @param  warningType   The warning type for this control, or -1 if there
   *                       should not be a warning type.
   * @param  warningValue  The value associated with the warning, if any.
   * @param  errorType     The error type for this control, or -1 if there
   *                       should not be an error type.
   *
   * @return  An octet string containing the encoded control value.
   */
  public static ASN1OctetString encodeValue(byte warningType, int warningValue,
                                            int errorType)
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>(2);

    if (warningType != (byte) -1)
    {
      ASN1Integer warningInteger = new ASN1Integer(warningType, warningValue);
      elementList.add(new ASN1Element(TYPE_WARNING, warningInteger.encode()));
    }

    if (errorType != -1)
    {
      elementList.add(new ASN1Enumerated(TYPE_ERROR, errorType));
    }

    ASN1Element[] sequenceElements = new ASN1Element[elementList.size()];
    elementList.toArray(sequenceElements);
    ASN1Sequence valueSequence = new ASN1Sequence(sequenceElements);
    return new ASN1OctetString(valueSequence.encode());
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

    ASN1OctetString controlValue = getValue();
    if (controlValue == null)
    {
      buffer.append(indentBuf).append("LDAP Password Policy Request Control").
             append(LDAPMessage.EOL);
    }
    else
    {
      buffer.append(indentBuf).append("LDAP Password Policy Response Control").
             append(LDAPMessage.EOL);
    }

    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);

    if (controlValue != null)
    {
      buffer.append(indentBuf).append("    Value:").append(LDAPMessage.EOL);

      switch (warningType)
      {
        case WARNING_TYPE_TIME_BEFORE_EXPIRATION:
          buffer.append(indentBuf).append("        Time Before Expiration:  ").
               append(warningValue).append(" seconds").append(LDAPMessage.EOL);
          break;
        case WARNING_TYPE_GRACE_AUTHS_REMAINING:
          buffer.append(indentBuf).append("        Grace Logins Remaining:  ").
               append(warningValue).append(LDAPMessage.EOL);
          break;
        case (byte) -1:
          break;
        default:
          buffer.append(indentBuf).append("        Unknown Warining (").
               append(warningType).append("):  ").append(warningValue).
               append(LDAPMessage.EOL);
          break;
      }

      switch (errorType)
      {
        case ERROR_TYPE_PASSWORD_EXPIRED:
          buffer.append(indentBuf).append("        Password Expired").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_ACCOUNT_LOCKED:
          buffer.append(indentBuf).append("        Account Locked").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_CHANGE_AFTER_RESET:
          buffer.append(indentBuf).append("        Change After Reset").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_PASSWORD_MOD_NOT_ALLOWED:
          buffer.append(indentBuf).append("        Password Mod Not Allowed").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_MUST_SUPPLY_OLD_PASSWORD:
          buffer.append(indentBuf).append("        Must Supply Old Password").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_INSUFFICIENT_PASSWORD_QUALITY:
          buffer.append(indentBuf).
               append("        Insufficient Password Quality").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_PASSWORD_TOO_SHORT:
          buffer.append(indentBuf).append("        Password Too Short").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_PASSWORD_TOO_YOUNG:
          buffer.append(indentBuf).append("        Password Too Young").
               append(LDAPMessage.EOL);
          break;
        case ERROR_TYPE_PASSWORD_IN_HISTORY:
          buffer.append(indentBuf).append("        Password In History").
               append(LDAPMessage.EOL);
          break;
        case (byte) -1:
          break;
        default:
          buffer.append(indentBuf).append("        Unknown Errpr (").
               append(errorType).append(LDAPMessage.EOL);
          break;
      }
    }

    return buffer.toString();
  }
}

