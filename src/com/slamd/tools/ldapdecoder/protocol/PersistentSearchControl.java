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
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the LDAP persistent search control, which is used to
 * provide notification of changes to entries meeting certain criteria.
 *
 *
 * @author   Neil A. Wilson
 */
public class PersistentSearchControl
       extends LDAPControl
{
  /**
   * The OID of the persistent search control.
   */
  public static final String PERSISTENT_SEARCH_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.3";



  /**
   * The change type that indicates that notification should be provided for
   * add operations.
   */
  public static final int CHANGE_TYPE_ADD = 1;



  /**
   * The change type that indicates that notification should be provided for
   * delete operations.
   */
  public static final int CHANGE_TYPE_DELETE = 2;



  /**
   * The change type that indicates that notification should be provided for
   * modify operations.
   */
  public static final int CHANGE_TYPE_MODIFY = 4;



  /**
   * The change type that indicates that notification should be provided for
   * modify DN operations.
   */
  public static final int CHANGE_TYPE_MODIFY_DN = 8;



  // Indicates whether only changes should be returned.
  private boolean changesOnly;

  // Indicates whether change notification controls should be included in
  // matching entries.
  private boolean returnChangeControls;

  // The types of changes for which notification should be provided.
  private int changeTypes;



  /**
   * Creates a new persistent search control with the provided information.
   *
   * @param  isCritical            Indicates whether this control should be
   *                               marked critical.
   * @param  changeTypes           The types of changes for which to provide
   *                               notification.
   * @param  changesOnly           Indicates whether only changes should be
   *                               returned, or both changes and any existing
   *                               matches.
   * @param  returnChangeControls  Indicates whether entry change notification
   *                               controls should be returned in matching
   *                               entries.
   */
  public PersistentSearchControl(boolean isCritical, int changeTypes,
                                 boolean changesOnly,
                                 boolean returnChangeControls)
  {
    super(PERSISTENT_SEARCH_CONTROL_OID, isCritical,
          encodeValue(changeTypes, changesOnly, returnChangeControls));

    this.changeTypes          = changeTypes;
    this.changesOnly          = changesOnly;
    this.returnChangeControls = returnChangeControls;
  }



  /**
   * Creates a new persistent search control with the provided information.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this persistent search control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the control
   *                             value.
   */
  public PersistentSearchControl(boolean isCritical,
                                 ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(PERSISTENT_SEARCH_CONTROL_OID, isCritical, controlValue);

    decodeValue();
  }



  /**
   * Retrieves the types of changes for which to provide notification.
   *
   * @return  The types of changes for which to provide notification.
   */
  public int getChangeTypes()
  {
    return changeTypes;
  }



  /**
   * Indicates whether only changes matching the search criteria should be
   * returned, or whether existing entries matching the criteria should also be
   * included.
   *
   * @return  <CODE>false</CODE> if existing entries matching the search
   *          criteria should be returned as well as changes, or
   *          <CODE>true</CODE> if only changes should be returned.
   */
  public boolean changesOnly()
  {
    return changesOnly;
  }



  /**
   * Indicates whether entry change notification controls should be included
   * with the change notifications.
   *
   * @return  <CODE>true</CODE> if change notification controls should be
   *          included with the change notifications, or <CODE>false</CODE> if
   *          not.
   */
  public boolean returnChangeControls()
  {
    return returnChangeControls;
  }



  /**
   * Encodes the provided information to an ASN.1 element suitable for the value
   * of this control.
   *
   * @param  changeTypes           The change types for which notification
   *                               should be provided.
   * @param  changesOnly           Indicates whether to only return changes
   *                               matching the criteria.
   * @param  returnChangeControls  Indicates whether entry change notification
   *                               controls should be returned.
   *
   * @return  The octet string containing the encoded control value.
   */
  public static ASN1OctetString encodeValue(int changeTypes,
                                            boolean changesOnly,
                                            boolean returnChangeControls)
  {
    ASN1Element[] sequenceElements = new ASN1Element[]
    {
      new ASN1Integer(changeTypes),
      new ASN1Boolean(changesOnly),
      new ASN1Boolean(returnChangeControls)
    };

    ASN1Sequence valueSequence = new ASN1Sequence(sequenceElements);
    return new ASN1OctetString(valueSequence.encode());
  }



  /**
   * Decodes the value for this control to extract the appropriate information
   * from it.
   *
   * @throws  ProtocolException  If the control value cannot be decoded
   *                             appropriately for a persistent search.
   */
  private void decodeValue()
          throws ProtocolException
  {
    ASN1Element[] sequenceElements;
    try
    {
      byte[] valueBytes = getValue().getValue();
      sequenceElements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode persistent search value",
                                  e);
    }


    if (sequenceElements.length != 3)
    {
      throw new ProtocolException("There must be exactly 3 elements in a " +
                                  "persistent search control value sequence");
    }


    try
    {
      changeTypes = sequenceElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode changeTypes from " +
                                  "persistent search control", e);
    }


    try
    {
      changesOnly = sequenceElements[1].decodeAsBoolean().getBooleanValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode changesOnly from " +
                                  "persistent search control", e);
    }


    try
    {
      returnChangeControls =
           sequenceElements[2].decodeAsBoolean().getBooleanValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode returnECs from " +
                                  "persistent search control", e);
    }
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
    buffer.append(indentBuf).append("LDAP Persistent Search Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);

    buffer.append(indentBuf).append("    Change Types:  ").
           append(changeTypes).append(" (");
    String separator = "";
    if (changeTypes == 0)
    {
      buffer.append("none").append(LDAPMessage.EOL);
    }
    else
    {
      if ((changeTypes & CHANGE_TYPE_ADD) == CHANGE_TYPE_ADD)
      {
        buffer.append("add");
        separator = ", ";
      }
      if ((changeTypes & CHANGE_TYPE_DELETE) == CHANGE_TYPE_DELETE)
      {
        buffer.append(separator).append("delete");
        separator = ", ";
      }
      if ((changeTypes & CHANGE_TYPE_MODIFY) == CHANGE_TYPE_MODIFY)
      {
        buffer.append(separator).append("modify");
        separator = ", ";
      }
      if ((changeTypes & CHANGE_TYPE_MODIFY_DN) == CHANGE_TYPE_MODIFY_DN)
      {
        buffer.append(separator).append("modify DN");
        separator = ", ";
      }

      buffer.append(')').append(LDAPMessage.EOL);
    }


    buffer.append(indentBuf).append("    Changes Only:  ").
           append(changesOnly).append(LDAPMessage.EOL);
    buffer.append(indentBuf).
           append("    Return Change Notification Controls:  ").
           append(returnChangeControls).append(LDAPMessage.EOL);

    return buffer.toString();
  }
}

