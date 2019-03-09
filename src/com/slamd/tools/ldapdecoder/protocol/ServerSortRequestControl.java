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
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines the server-side sort request control, which requests that
 * the server sort the results before returning them to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class ServerSortRequestControl
       extends LDAPControl
{
  /**
   * The OID of the server sort request control.
   */
  public static final String SERVER_SORT_REQUEST_CONTROL_OID =
                                  "1.2.840.113556.1.4.473";



  // The sort keys that define the sort order for the matching entries.
  private ServerSortKey[] sortKeys;



  /**
   * Creates a new server sort request control with the provided information.
   *
   * @param  isCritical  Indicates whether this control should be marked
   *                     critical.
   * @param  sortKeys    The sort keys that define the sort order.
   */
  public ServerSortRequestControl(boolean isCritical, ServerSortKey[] sortKeys)
  {
    super(SERVER_SORT_REQUEST_CONTROL_OID, isCritical, encodeValue(sortKeys));

    this.sortKeys = sortKeys;
  }



  /**
   * Creates a new server sort request control by decoding the provided value.
   *
   * @param  isCritical    Indicates whether this control should be marked
   *                       critical.
   * @param  controlValue  The encoded value for this control.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the value
   *                             for the control.
   */
  public ServerSortRequestControl(boolean isCritical,
                                  ASN1OctetString controlValue)
         throws ProtocolException
  {
    super(SERVER_SORT_REQUEST_CONTROL_OID, isCritical, controlValue);


    ASN1Element[] keyElements;
    try
    {
      byte[] valueBytes = controlValue.getValue();
      keyElements = ASN1Element.decodeAsSequence(valueBytes).getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode control value as a " +
                                  "sequence of sort keys", e);
    }


    sortKeys = new ServerSortKey[keyElements.length];
    for (int i=0; i < keyElements.length; i++)
    {
      try
      {
        sortKeys[i] = ServerSortKey.decode(keyElements[i]);
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode server sort key", e);
      }
    }
  }



  /**
   * Retrieves the sort keys that define the sort order.
   *
   * @return  The sort keys that define the sort order.
   */
  public ServerSortKey[] getSortKeys()
  {
    return sortKeys;
  }



  /**
   * Encodes the provided set of sort keys into an octet string suitable for use
   * as the value of this control.
   *
   * @param  sortKeys  The sort keys that define the sort order.
   *
   * @return  The octet string containing the encoded sort keys.
   */
  public static ASN1OctetString encodeValue(ServerSortKey[] sortKeys)
  {
    ASN1Element[] keyElements = new ASN1Element[sortKeys.length];
    for (int i=0; i < keyElements.length; i++)
    {
      keyElements[i] = sortKeys[i].encode();
    }

    return new ASN1OctetString(new ASN1Sequence(keyElements).encode());
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
    buffer.append(indentBuf).append("LDAP Server-Side Sort Request Control").
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    OID:  ").append(getControlOID()).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("    Criticality:  ").
           append(isCritical()).append(LDAPMessage.EOL);
    for (int i=0; i < sortKeys.length; i++)
    {
      buffer.append(sortKeys[i].toString(indent+4));
    }

    return buffer.toString();
  }
}

