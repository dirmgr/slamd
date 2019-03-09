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
 * This class defines the entry change notification control, which may be used
 * in conjunction with the persistent search control to provide details on the
 * type of change that occurred with an entry.
 *
 *
 * @author   Neil A. Wilson
 */
public class EntryChangeNotificationControl
       extends LDAPControl
{
  /**
   * The OID of the entry change notification control.
   */
  public static final String ENTRY_CHANGE_NOTIFICATION_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.7";



  // The change number associated with this change, if applicable.
  private int changeNumber;

  // The type of change that occurred to the entry.
  private int changeType;

  // The previous DN for this entry if the change was a modify DN.
  private String previousDN;



  /**
   * Creates a new entry change notification control.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  changeType    The type of change that occurred to the entry.
   * @param  previousDN    The previous DN of the entry if the change was a
   *                       modify DN operation.
   * @param  changeNumber  The change number associated with this change, if
   *                       applicable.  A negative value should be used to
   *                       indicate that no change number is available.
   */
  public EntryChangeNotificationControl(boolean isCritical, int changeType,
                                        String previousDN, int changeNumber)
  {
    super(ENTRY_CHANGE_NOTIFICATION_CONTROL_OID, isCritical,
          encodeValue(changeType, previousDN, changeNumber));

    this.changeType   = changeType;
    this.previousDN   = previousDN;
    this.changeNumber = changeNumber;
  }



  /**
   * Creates a new entry change notification control by decoding the provided
   * value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the control
   *                             value.
   */
  public EntryChangeNotificationControl(boolean isCritical,
                                        ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(ENTRY_CHANGE_NOTIFICATION_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] sequenceElements;
    try
    {
      sequenceElements =
           ASN1Element.decodeAsSequence(controlValue.getValue()).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode entry change " +
                                  "notification control value sequence", e);
    }


    if ((sequenceElements.length < 1) || (sequenceElements.length > 3))
    {
      throw new ProtocolException("There must be between 1 and 3 elements in " +
                                  "an entry change notification value " +
                                  "sequence");
    }


    try
    {
      changeType = sequenceElements[0].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode change type from " +
                                  "entry change notification control", e);
    }


    previousDN   = null;
    changeNumber = -1;
    if (changeType == PersistentSearchControl.CHANGE_TYPE_MODIFY_DN)
    {
      try
      {
        previousDN = sequenceElements[1].decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode previous DN from " +
                                    "entry change notification control", e);
      }

      if (sequenceElements.length == 3)
      {
        try
        {
          changeNumber = sequenceElements[2].decodeAsInteger().getIntValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode change number from " +
                                      "entry change notification control", e);
        }
      }
    }
    else
    {
      if (sequenceElements.length == 2)
      {
        try
        {
          changeNumber = sequenceElements[1].decodeAsInteger().getIntValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode change number from " +
                                      "entry change notification control", e);
        }
      }
    }
  }



  /**
   * Encodes the provided information into an octet string suitable for use as
   * the value of this control.
   *
   * @param  changeType    The type of change that occurred to the entry.
   * @param  previousDN    The previous DN of the entry if the change was a
   *                       modify DN operation.
   * @param  changeNumber  The change number associated with this change, if
   *                       applicable.
   *
   * @return  An octet string containing the encoded control value.
   */
  public static ASN1OctetString encodeValue(int changeType, String previousDN,
                                            int changeNumber)
  {
    ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>(3);
    elementList.add(new ASN1Enumerated(changeType));

    if (changeType == PersistentSearchControl.CHANGE_TYPE_MODIFY_DN)
    {
      elementList.add(new ASN1OctetString(previousDN));
    }

    if (changeNumber >= 0)
    {
      elementList.add(new ASN1Integer(changeNumber));
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
    buffer.append(indentBuf).append("LDAP Entry Change Notification Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);

    String changeTypeStr;
    switch (changeType)
    {
      case PersistentSearchControl.CHANGE_TYPE_ADD:
        changeTypeStr = "add";
        break;
      case PersistentSearchControl.CHANGE_TYPE_DELETE:
        changeTypeStr = "delete";
        break;
      case PersistentSearchControl.CHANGE_TYPE_MODIFY:
        changeTypeStr = "modify";
        break;
      case PersistentSearchControl.CHANGE_TYPE_MODIFY_DN:
        changeTypeStr = "modify DN";
        break;
      default:
        changeTypeStr = "invalid change type (" + changeType + ')';
        break;
    }
    buffer.append(indentBuf).append("    Change Type:  ").
           append(changeType).append(" (").append(changeTypeStr).append(')').
           append(LDAPMessage.EOL);

    if (changeType == PersistentSearchControl.CHANGE_TYPE_MODIFY_DN)
    {
      buffer.append(indentBuf).append("    Previous DN:  ").append(previousDN).
             append(LDAPMessage.EOL);
    }

    if (changeNumber >= 0)
    {
      buffer.append(indentBuf).append("    Change Number:  ").
             append(changeNumber).append(LDAPMessage.EOL);
    }

    return buffer.toString();
  }
}

