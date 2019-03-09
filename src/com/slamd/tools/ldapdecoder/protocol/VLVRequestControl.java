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



import com.unboundid.util.Base64;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the virtual list view (VLV) request control, which is used
 * to indicate that only a particular range of the results should be returned
 * and that potentially other ranges may be requested.  Note that if this
 * control is included in a request, then the server-side sort control must also
 * be included.
 *
 *
 * @author   Neil A. Wilson
 */
public class VLVRequestControl
       extends LDAPControl
{
  /**
   * The OID of the VLV request control.
   */
  public static final String VLV_REQUEST_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.9";



  /**
   * The selection type that indicates that the target entry will be selected by
   * its offset in the list of search results.
   */
  public static final byte SELECT_TYPE_BY_OFFSET = (byte) 0xA0;



  /**
   * The selection type that indicates that the target entry will be selected as
   * the first entry greater than or equal to the provided assertion value.  The
   * comparison will be made against the primary sort attribute defined in the
   * server-side sort control.
   */
  public static final byte SELECT_TYPE_BY_ASSERTION_VALUE = (byte) 0x81;



  // The assertion value to use to find the target entry.
  private ASN1OctetString assertionValue;

  // The method that will be used to select the entry that is the starting point
  // for the results.
  private byte selectType;

  // The number of entries after the requested entry that should be returned.
  private int afterCount;

  // The number of entries before the requested entry that should be returned.
  private int beforeCount;

  // The total number of entries matching the search criteria.
  private int contentCount;

  // The offset of the target entry in the list of matches.
  private int entryOffset;



  /**
   * Creates a new virtual list view request control.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  beforeCount   The number of entries before the requested target
   *                       entry that should be returned.
   * @param  afterCount    The number of entries after the requested target
   *                       entry that should be returned.
   * @param  entryOffset   The offset of the target entry in the list of search
   *                       results.
   * @param  contentCount  The total number of entries that the client believes
   *                       match the search criteria.  If this is not known
   *                       (e.g., if this is the first request in a series of
   *                       VLV retrievals), then this should be zero.
   */
  public VLVRequestControl(boolean isCritical, int beforeCount, int afterCount,
                           int entryOffset, int contentCount)
  {
    super(VLV_REQUEST_CONTROL_OID, isCritical,
          encodeValue(beforeCount, afterCount, entryOffset, contentCount));

    this.selectType   = SELECT_TYPE_BY_OFFSET;
    this.beforeCount  = beforeCount;
    this.afterCount   = afterCount;
    this.entryOffset  = entryOffset;
    this.contentCount = contentCount;
  }



  /**
   * Creates a new virtual list view request control.
   *
   * @param  isCritical      Indicates whether this control should be marked
   *                         critical.
   * @param  beforeCount     The number of entries before the requested target
   *                         entry that should be returned.
   * @param  afterCount      The number of entries after the requested target
   *                         entry that should be returned.
   * @param  assertionValue  The assertion value that should be used to locate
   *                         the target entry.
   */
  public VLVRequestControl(boolean isCritical, int beforeCount, int afterCount,
                           ASN1OctetString assertionValue)
  {
    super(VLV_REQUEST_CONTROL_OID, isCritical,
          encodeValue(beforeCount, afterCount, assertionValue));

    this.selectType     = SELECT_TYPE_BY_ASSERTION_VALUE;
    this.beforeCount    = beforeCount;
    this.afterCount     = afterCount;
    this.assertionValue = assertionValue;
  }



  /**
   * Creates a new virtual list view request control.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for the VLV request control.
   *
   * @throws  ProtocolException  If the provided control value cannot be
   *                             decoded appropriately for a VLV request
   *                             control.
   */
  public VLVRequestControl(boolean isCritical, ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(VLV_REQUEST_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] sequenceElements;
    try
    {
      byte[] valueBytes = controlValue.getValue();
      sequenceElements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode VLV request control " +
                                  "sequence", e);
    }


    if (sequenceElements.length != 3)
    {
      throw new ProtocolException("There must be exactly 3 elements in a VLV " +
                                  "request control sequence");
    }


    try
    {
      beforeCount = sequenceElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode beforeCount from VLV " +
                                  "request control sequence", e);
    }


    try
    {
      afterCount = sequenceElements[1].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode afterCount from VLV " +
                                  "request control sequence", e);
    }


    selectType = sequenceElements[2].getType();
    if (selectType == SELECT_TYPE_BY_OFFSET)
    {
      ASN1Element[] offsetElements;
      try
      {
        offsetElements = sequenceElements[2].decodeAsSequence().getElements();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode byOffset sequence " +
                                    "elements in VLV request control", e);
      }


      if (offsetElements.length != 2)
      {
        throw new ProtocolException("There must be exactly 2 elements in the " +
                                    "byOffset sequence of a VLV request " +
                                    "control");
      }


      try
      {
        entryOffset = offsetElements[0].decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode entry offset from the " +
                                    "VLV request control", e);
      }


      try
      {
        contentCount = offsetElements[1].decodeAsInteger().getIntValue();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode content count from the " +
                                    "VLV request control", e);
      }
    }
    else if (selectType == SELECT_TYPE_BY_ASSERTION_VALUE)
    {
      try
      {
        assertionValue = sequenceElements[2].decodeAsOctetString();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode greaterThanOrEqual " +
                                    "assertion value from VLV request control",
                                    e);
      }
    }
    else
    {
      throw new ProtocolException("Invalid entry selection type (" +
                                  selectType + ')');
    }
  }



  /**
   * Encodes the provided information into a VLV request control value.
   *
   * @param  beforeCount   The number of entries before the requested target
   *                       entry that should be returned.
   * @param  afterCount    The number of entries after the requested target
   *                       entry that should be returned.
   * @param  entryOffset   The offset of the target entry in the list of search
   *                       results.
   * @param  contentCount  The total number of entries that the client believes
   *                       match the search criteria.  If this is not known
   *                       (e.g., if this is the first request in a series of
   *                       VLV retrievals), then this should be zero.
   *
   * @return  The encoded VLV request control value.
   */
  public static ASN1OctetString encodeValue(int beforeCount, int afterCount,
                                            int entryOffset, int contentCount)
  {
    ASN1Element[] offsetElements = new ASN1Element[]
    {
      new ASN1Integer(entryOffset),
      new ASN1Integer(contentCount)
    };


    ASN1Element[] sequenceElements = new ASN1Element[]
    {
      new ASN1Integer(beforeCount),
      new ASN1Integer(afterCount),
      new ASN1Sequence(SELECT_TYPE_BY_OFFSET, offsetElements)
    };


    return new ASN1OctetString(new ASN1Sequence(sequenceElements).encode());
  }



  /**
   * Encodes the provided information into a VLV request control value.
   *
   * @param  beforeCount     The number of entries before the requested target
   *                         entry that should be returned.
   * @param  afterCount      The number of entries after the requested target
   *                         entry that should be returned.
   * @param  assertionValue  The assertion value that should be used to locate
   *                         the target entry.
   *
   * @return  The encoded VLV request control value.
   */
  public static ASN1OctetString encodeValue(int beforeCount, int afterCount,
                                            ASN1OctetString assertionValue)
  {
    assertionValue.setType(SELECT_TYPE_BY_ASSERTION_VALUE);
    ASN1Element[] sequenceElements = new ASN1Element[]
    {
      new ASN1Integer(beforeCount),
      new ASN1Integer(afterCount),
      assertionValue
    };


    return new ASN1OctetString(new ASN1Sequence(sequenceElements).encode());
  }



  /**
   * Retrieves the number of entries before the target entry that should be
   * retrieved.
   *
   * @return  The number of entries before the target entry that should be
   *          retrieved.
   */
  public int getBeforeCount()
  {
    return beforeCount;
  }



  /**
   * Retrieves the number of entries after the target entry that should be
   * retrieved.
   *
   * @return  The number of entries after the target entry that should be
   *          retrieved.
   */
  public int getAfterCount()
  {
    return afterCount;
  }



  /**
   * Retrieves the flag that indicates the means of selecting the target entry.
   *
   * @return  The flag that indicates the means of selecting the target entry.
   */
  public byte getSelectionType()
  {
    return selectType;
  }



  /**
   * Retrieves the offset of the target entry in the result set.
   *
   * @return  The offset of the target entry in the result set.
   */
  public int getEntryOffset()
  {
    return entryOffset;
  }



  /**
   * Retrieves the number of entries the client believes are in the result set.
   *
   * @return  The number of entries the client believes are in the result set.
   */
  public int getContentCount()
  {
    return contentCount;
  }



  /**
   * Retrieves the assertion value that should be used to locate the target
   * entry in the result set.
   *
   * @return  The assertion value that should be used to locate the target entry
   *          in the result set.
   */
  public ASN1OctetString getAssertionValue()
  {
    return assertionValue;
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
    buffer.append(indentBuf).append("LDAP VLV Request Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Before Count:  ").
           append(beforeCount).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    After Count:  ").
           append(afterCount).append(LDAPMessage.EOL);

    if (selectType == SELECT_TYPE_BY_OFFSET)
    {
      buffer.append(indentBuf).append("    Selection Type:  By Entry Offset").
             append(LDAPMessage.EOL);
      buffer.append(indentBuf).append("        Entry Offset:  ").
             append(entryOffset).append(LDAPMessage.EOL);
      buffer.append(indentBuf).append("        Content Count:  ").
             append(contentCount).append(LDAPMessage.EOL);
    }
    else if (selectType == SELECT_TYPE_BY_ASSERTION_VALUE)
    {
      buffer.append(indentBuf).
             append("    Selection Type:  By Assertion Value").
             append(LDAPMessage.EOL);

      if (LDAPAttribute.valueNeedsBase64Encoding(assertionValue))
      {
        buffer.append(indentBuf).append("        Assertion Value::  ").
               append(Base64.encode(assertionValue.getValue())).
               append(LDAPMessage.EOL);
      }
      else
      {
        buffer.append(indentBuf).append("        Assertion Value:  ").
               append(assertionValue.getStringValue()).append(LDAPMessage.EOL);
      }
    }
    else
    {
      buffer.append(indentBuf).
             append("    Selection Type:  Invalid Type (").append(selectType).
             append(')').append(LDAPMessage.EOL);
    }

    return buffer.toString();
  }
}

