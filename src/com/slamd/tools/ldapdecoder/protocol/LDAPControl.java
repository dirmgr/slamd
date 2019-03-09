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



import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP control, which provides additional information
 * that may be used when processing an LDAP operation.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPControl
{
  /**
   * The ASN.1 type that should be used for a sequence of controls.
   */
  public static final byte CONTROL_SEQUENCE_TYPE = (byte) 0xA0;



  // The value for this control.
  private ASN1OctetString controlValue;

  // Indicates whether this control should be considered critical.
  private boolean isCritical;

  // The OID for this LDAP control.
  private String controlOID;



  /**
   * Creates a new LDAP control with the specified OID.  It will not be critical
   * and will not have a value.
   *
   * @param  controlOID  The OID for this control.
   */
  public LDAPControl(String controlOID)
  {
    this.controlOID   = controlOID;
    this.isCritical   = false;
    this.controlValue = null;
  }



  /**
   * Creates a new LDAP control with the specified OID, criticality, and value.
   *
   * @param  controlOID    The OID for this control.
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The value for this control.
   */
  public LDAPControl(String controlOID, boolean isCritical,
                     ASN1OctetString controlValue)
  {
    this.controlOID   = controlOID;
    this.isCritical   = isCritical;
    this.controlValue = controlValue;
  }



  /**
   * Retrieves the OID for this control.
   *
   * @return  The OID for this control.
   */
  public String getControlOID()
  {
    return controlOID;
  }



  /**
   * Indicates whether this control is marked critical.
   *
   * @return  <CODE>true</CODE> if this control is marked critical, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isCritical()
  {
    return isCritical;
  }



  /**
   * Retrieves the value for this control.
   *
   * @return  The value for this control, or <CODE>null</CODE> if it does not
   *          have a value.
   */
  public ASN1OctetString getValue()
  {
    return controlValue;
  }



  /**
   * Encodes this control to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded control.
   */
  public ASN1Element encode()
  {
    ASN1Element[] controlElements;
    if (controlValue == null)
    {
      if (isCritical)
      {
        controlElements = new ASN1Element[]
        {
          new ASN1OctetString(controlOID),
          new ASN1Boolean(isCritical)
        };
      }
      else
      {
        controlElements = new ASN1Element[]
        {
          new ASN1OctetString(controlOID)
        };
      }
    }
    else
    {
      controlElements = new ASN1Element[]
      {
        new ASN1OctetString(controlOID),
        new ASN1Boolean(isCritical),
        controlValue
      };
    }


    return new ASN1Sequence(controlElements);
  }



  /**
   * Encodes the provided array of controls to an ASN.1 sequence suitable for
   * including in an LDAP message.
   *
   * @param  controls  The set of controls to be encoded.
   *
   * @return  The ASN.1 element containing the encoded controls.
   */
  public static ASN1Element encode(LDAPControl[] controls)
  {
    ASN1Element[] controlElements = new ASN1Element[controls.length];
    for (int i=0; i < controlElements.length; i++)
    {
      controlElements[i] = controls[i].encode();
    }

    return new ASN1Sequence(CONTROL_SEQUENCE_TYPE, controlElements);
  }



  /**
   * Decodes the provided ASN.1 element as an LDAP control.
   *
   * @param  element  The ASN.1 element to decode as an LDAP control.
   *
   * @return  The decoded LDAP control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the ASN.1
   *                             element as an LDAP control.
   */
  public static LDAPControl decode(ASN1Element element)
         throws ProtocolException
  {
    String          controlOID;
    boolean         isCritical   = false;
    ASN1OctetString controlValue = null;


    ASN1Element[] controlElements;
    try
    {
      controlElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode ASN.1 element as a " +
                                  "control sequence", e);
    }


    if ((controlElements.length == 0) || (controlElements.length > 3))
    {
      throw new ProtocolException("There must be between 1 and 3 elements in " +
                                  "an LDAP control sequence");
    }


    try
    {
      controlOID = controlElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode control OID", e);
    }


    if (controlElements.length >= 2)
    {
      if ((controlElements.length == 2) &&
          (controlElements[1].getType() == ASN1Element.ASN1_OCTET_STRING_TYPE))
      {
        try
        {
          controlValue = controlElements[1].decodeAsOctetString();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode control value", e);
        }
      }
      else
      {
        try
        {
          isCritical = controlElements[1].decodeAsBoolean().getBooleanValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode control criticality",
                                      e);
        }
      }
    }


    if (controlElements.length == 3)
    {
      try
      {
        controlValue = controlElements[2].decodeAsOctetString();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode control value", e);
      }
    }


    try
    {
      if (controlOID.equals(ManageDSAITControl.MANAGE_DSA_IT_CONTROL_OID))
      {
        return new ManageDSAITControl(isCritical);
      }
      else if (controlOID.equals(
                    PersistentSearchControl.PERSISTENT_SEARCH_CONTROL_OID))
      {
        return new PersistentSearchControl(isCritical, controlValue);
      }
      else if (controlOID.equals(EntryChangeNotificationControl.
                                      ENTRY_CHANGE_NOTIFICATION_CONTROL_OID))
      {
        return new EntryChangeNotificationControl(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    PasswordExpiredControl.PASSWORD_EXPIRED_CONTROL_OID))
      {
        return new PasswordExpiredControl(isCritical);
      }
      else if (controlOID.equals(
                    PasswordExpiringControl.PASSWORD_EXPIRING_CONTROL_OID))
      {
        return new PasswordExpiringControl(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    PasswordPolicyControl.PASSWORD_POLICY_CONTROL_OID))
      {
        return new PasswordPolicyControl(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    ServerSortRequestControl.SERVER_SORT_REQUEST_CONTROL_OID))
      {
        return new ServerSortRequestControl(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    ServerSortResponseControl.SERVER_SORT_RESPONSE_CONTROL_OID))
      {
        return new ServerSortResponseControl(isCritical, controlValue);
      }
      else if (controlOID.equals(VLVRequestControl.VLV_REQUEST_CONTROL_OID))
      {
        return new VLVRequestControl(isCritical, controlValue);
      }
      else if (controlOID.equals(VLVResponseControl.VLV_RESPONSE_CONTROL_OID))
      {
        return new VLVResponseControl(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    ProxiedAuthV1Control.PROXIED_AUTH_V1_CONTROL_OID))
      {
        return new ProxiedAuthV1Control(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    ProxiedAuthV2Control.PROXIED_AUTH_V2_CONTROL_OID))
      {
        return new ProxiedAuthV2Control(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    RealAttributesOnlyControl.REAL_ATTRIBUTES_ONLY_CONTROL_OID))
      {
        return new RealAttributesOnlyControl(isCritical);
      }
      else if (controlOID.equals(
                    GetEffectiveRightsControl.GET_EFFECTIVE_RIGHTS_CONTROL_OID))
      {
        return new GetEffectiveRightsControl(isCritical, controlValue);
      }
      else if (controlOID.equals(
                    AuthorizationIDRequestControl.AUTHORIZATION_ID_REQUEST_OID))
      {
        return new AuthorizationIDRequestControl(isCritical);
      }
      else if (controlOID.equals(
               AuthorizationIDResponseControl.AUTHORIZATION_ID_RESPONSE_OID))
      {
        return new AuthorizationIDResponseControl(isCritical, controlValue);
      }
      else if (controlOID.equals(PagedResultsControl.PAGED_RESULTS_CONTROL_OID))
      {
        return new PagedResultsControl(isCritical, controlValue);
      }
    } catch (Exception e) {}


    return new LDAPControl(controlOID, isCritical, controlValue);
  }



  /**
   * Decodes the provided ASN.1 element as a set of LDAP controls.
   *
   * @param  element  The ASN.1 element to decode as a set of LDAP controls.
   *
   * @return  The decoded array of controls.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the set of
   *                             controls.
   */
  public static LDAPControl[] decodeControls(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] controlElements;
    try
    {
      controlElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode ASN.1 element as a " +
                                  "sequence of controls", e);
    }


    LDAPControl[] controls = new LDAPControl[controlElements.length];
    for (int i=0; i < controlElements.length; i++)
    {
      controls[i] = decode(controlElements[i]);
    }


    return controls;
  }



  /**
   * Retrieves a string representation of this control.
   *
   * @return  A string representation of this control.
   */
  public String toString()
  {
    return toString(0);
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
    buffer.append(indentBuf).append("LDAP Control").append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(controlOID).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical).append(LDAPMessage.EOL);

    if (controlValue != null)
    {
      byte[] valueBytes = controlValue.getValue();
      buffer.append(indentBuf).append("    Value:  ").append(LDAPMessage.EOL);
      buffer.append(LDAPMessage.byteArrayToString(valueBytes, (indent+8)));
    }

    return buffer.toString();
  }
}

