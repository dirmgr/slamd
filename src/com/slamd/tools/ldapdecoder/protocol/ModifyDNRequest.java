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



import java.io.PrintStream;
import java.util.Date;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP modify DN request, which is used to alter the DN
 * of an entry in a directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class ModifyDNRequest
       extends ProtocolOp
{
  /**
   * The ASN.1 type that should be used for the newSuperior portion of the
   * modify DN request if it is provided.
   */
  public static final byte NEW_SUPERIOR_TYPE = (byte) 0x80;



  // Indicates whether the old RDN value should be removed from the entry.
  private boolean deleteOldRDN;

  // The current DN of the entry to rename.
  private String dn;

  // The new RDN to use for the entry.
  private String newRDN;

  // The new parent to use for the entry.
  private String newSuperior;



  /**
   * Creates a new modify DN request with the provided information.
   *
   * @param  dn            The current DN for the entry.
   * @param  newRDN        The new RDN to use for the entry.
   * @param  deleteOldRDN  Indicates whether the old RDN value should be removed
   *                       from the entry.
   */
  public ModifyDNRequest(String dn, String newRDN, boolean deleteOldRDN)
  {
    this.dn           = dn;
    this.newRDN       = newRDN;
    this.deleteOldRDN = deleteOldRDN;

    newSuperior = null;
  }



  /**
   * Creates a new modify DN request with the provided information.
   *
   * @param  dn            The current DN for the entry.
   * @param  newRDN        The new RDN to use for the entry.
   * @param  deleteOldRDN  Indicates whether the old RDN value should be removed
   *                       from the entry.
   * @param  newSuperior   The new parent to use for the entry.
   */
  public ModifyDNRequest(String dn, String newRDN, boolean deleteOldRDN,
                         String newSuperior)
  {
    this.dn           = dn;
    this.newRDN       = newRDN;
    this.deleteOldRDN = deleteOldRDN;
    this.newSuperior  = newSuperior;
  }



  /**
   * Retrieves the current DN of the entry to rename.
   *
   * @return  The current DN of the entry to rename.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Retrieves the new RDN to use for the entry.
   *
   * @return  The new RDN to use for the entry.
   */
  public String getNewRDN()
  {
    return newRDN;
  }



  /**
   * Indicates whether the old RDN value should be removed from the entry when
   * it is renamed.
   *
   * @return  <CODE>true</CODE> if the old RDN value should be removed from the
   *          entry, or <CODE>false</CODE> if not.
   */
  public boolean deleteOldRDN()
  {
    return deleteOldRDN;
  }



  /**
   * Retrieves the new parent to use for the entry.
   *
   * @return  The new parent to use for the entry.
   */
  public String getNewSuperior()
  {
    return newSuperior;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] modifyDNElements;
    if (newSuperior == null)
    {
      modifyDNElements = new ASN1Element[]
      {
        new ASN1OctetString(dn),
        new ASN1OctetString(newRDN),
        new ASN1Boolean(deleteOldRDN)
      };
    }
    else
    {
      modifyDNElements = new ASN1Element[]
      {
        new ASN1OctetString(dn),
        new ASN1OctetString(newRDN),
        new ASN1Boolean(deleteOldRDN),
        new ASN1OctetString(NEW_SUPERIOR_TYPE, newSuperior)
      };
    }


    return new ASN1Sequence(MODIFY_DN_REQUEST_TYPE, modifyDNElements);
  }



  /**
   * Decodes the provided ASN.1 element as a modify DN request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded modify DN request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a modify DN request.
   */
  public static ModifyDNRequest decodeModifyDNRequest(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] modifyDNElements;
    try
    {
      modifyDNElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modify DN request sequence",
                                  e);
    }


    if ((modifyDNElements.length < 3) || (modifyDNElements.length > 4))
    {
      throw new ProtocolException("There must be either 3 or 4 elements in a " +
                                  "modify DN request sequence");
    }


    String dn;
    try
    {
      dn = modifyDNElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode entry DN for modify DN " +
                                  "request", e);
    }


    String newRDN;
    try
    {
      newRDN = modifyDNElements[1].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode new RDN for modify DN " +
                                  "request", e);
    }


    boolean deleteOldRDN;
    try
    {
      deleteOldRDN = modifyDNElements[2].decodeAsBoolean().getBooleanValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode deleteOldRDN for modify " +
                                  "DN request", e);
    }


    String newSuperior = null;
    if (modifyDNElements.length == 4)
    {
      try
      {
        newSuperior =
             modifyDNElements[3].decodeAsOctetString().getStringValue();
      }
      catch (Exception e)
      {
        throw new ProtocolException("Unable to decode newSuperior for modify " +
                                    "DN request", e);
      }
    }


    return new ModifyDNRequest(dn, newRDN, deleteOldRDN, newSuperior);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Modify DN Request";
  }



  /**
   * Retrieves a string representation of this protocol op with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this protocol op with the specified
   *          indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("Current Entry DN: ").append(dn).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("New RDN: ").append(newRDN).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("Delete Old RDN: ").
           append(deleteOldRDN).append(LDAPMessage.EOL);


    if (newSuperior != null)
    {
      buffer.append(indentBuf).append("New Superior: ").append(newSuperior).
             append(LDAPMessage.EOL);
    }


    return buffer.toString();
  }



  /**
   * Constructs a string representation of this LDAP message in a form that can
   * be written to a SLAMD script.  It may be empty if this message isn't one
   * that would be generated as part of a client request.
   *
   * @param  scriptWriter  The print stream to which the script contents should
   *                       be written.
   */
  public void toSLAMDScript(PrintStream scriptWriter)
  {
    scriptWriter.println("#### Modify DN request captured at " + new Date());
    scriptWriter.println("# Entry DN:  " + dn);
    scriptWriter.println("# New RDN:  " + newRDN);
    scriptWriter.println("# Delete Old RDN:  " + deleteOldRDN);
    if ((newSuperior == null) || (newSuperior.length() == 0))
    {
      scriptWriter.println("resultCode = conn.modifyRDN(\"" + dn + "\", \"" +
                           newRDN + "\", " + deleteOldRDN + ");");
    }
    else
    {
      scriptWriter.println("# New Superior:  " + newSuperior);
      scriptWriter.println("# NOTE:  The SLAMD scripting language does not " +
                           "currently support modify DN with newSuperior.");
    }

    scriptWriter.println();
    scriptWriter.println();
  }
}

