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



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the virtual list view (VLV) response control, which is
 * returned by the server after processing a search with the VLV request control
 * to provide information on what was actually returned.
 *
 *
 * @author   Neil A. Wilson
 */
public class VLVResponseControl
       extends LDAPControl
{
  /**
   * The OID of the VLV response control.
   */
  public static final String VLV_RESPONSE_CONTROL_OID =
                                  "2.16.840.1.113730.3.4.10";



  // The total number of entries in the result set.
  private int contentCount;

  // The result code for the VLV operation.
  private int resultCode;

  // The position of the target entry in the overall result set.
  private int targetPosition;



  /**
   * Creates a VLV response control.
   *
   * @param  isCritical      Indicates whether this control should be marked
   *                         critical.
   * @param  targetPosition  The position of the target entry in the result set.
   * @param  contentCount    The total number of entries in the result set.
   * @param  resultCode      The result code for the VLV operation.
   */
  public VLVResponseControl(boolean isCritical, int targetPosition,
                            int contentCount, int resultCode)
  {
    super(VLV_RESPONSE_CONTROL_OID, isCritical,
          encodeValue(targetPosition, contentCount, resultCode));

    this.targetPosition = targetPosition;
    this.contentCount   = contentCount;
    this.resultCode     = resultCode;
  }



  /**
   * Creates a new VLV response control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public VLVResponseControl(boolean isCritical,
                            ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(VLV_RESPONSE_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] sequenceElements;
    try
    {
      byte[] valueBytes = controlValue.getValue();
      sequenceElements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode VLV response control " +
                                  "sequence", e);
    }


    if (sequenceElements.length != 3)
    {
      throw new ProtocolException("There must be exactly 3 elements in a VLV " +
                                  "response control sequence");
    }


    try
    {
      targetPosition = sequenceElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode target position from VLV " +
                                  "response control", e);
    }


    try
    {
      contentCount = sequenceElements[1].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode content count from VLV " +
                                  "response control", e);
    }


    try
    {
      resultCode = sequenceElements[2].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode result code from VLV " +
                                  "response control", e);
    }
  }



  /**
   * Encodes the provided information into an octet string suitable for use as
   * the value of a VLV response control.
   *
   * @param  targetPosition  The position of the target entry in the result set.
   * @param  contentCount    The total number of entries in the result set.
   * @param  resultCode      The result code for the VLV operation.
   *
   * @return  The encoded control value.
   */
  public static ASN1OctetString encodeValue(int targetPosition,
                                            int contentCount, int resultCode)
  {
    ASN1Element[] sequenceElements = new ASN1Element[]
    {
      new ASN1Integer(targetPosition),
      new ASN1Integer(contentCount),
      new ASN1Enumerated(resultCode)
    };


    return new ASN1OctetString(new ASN1Sequence(sequenceElements).encode());
  }



  /**
   * Retrieves the position of the target entry in the result set.
   *
   * @return  The position of the target entry in the result set.
   */
  public int getTargetPosition()
  {
    return targetPosition;
  }



  /**
   * Retrieves the total number of entries in the result set.
   *
   * @return  The total number of entries in the result set.
   */
  public int getContentCount()
  {
    return contentCount;
  }



  /**
   * Retrieves the result code for the VLV operation.
   *
   * @return  The result code for the VLV operation.
   */
  public int getResultCode()
  {
    return resultCode;
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
    buffer.append(indentBuf).append("LDAP VLV Response Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Target Position:  ").
           append(targetPosition).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Content Count:  ").
           append(contentCount).append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    VLV Result Code:  ").
           append(resultCode).append(" (").
           append(LDAPResultCode.resultCodeToString(resultCode)).append(')').
           append(LDAPMessage.EOL);

    return buffer.toString();
  }
}

