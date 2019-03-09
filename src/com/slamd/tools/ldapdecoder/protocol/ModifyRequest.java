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

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP modify request, which is used to alter information
 * in a directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class ModifyRequest
       extends ProtocolOp
{
  // The set of modifications to perform on the entry.
  private LDAPModification[] modifications;

  // The DN of the entry in which to perform the modifications.
  private String dn;



  /**
   * Creates a new modify request with the provided information.
   *
   * @param  dn             The DN of the entry in which to perform the
   *                        modifications.
   * @param  modifications  The set of modifications to perform on the entry.
   */
  public ModifyRequest(String dn, LDAPModification[] modifications)
  {
    this.dn            = dn;
    this.modifications = modifications;
  }



  /**
   * Retrieves the DN of the entry in which to perform the modifications.
   *
   * @return  The DN of the entry in which to perform the modifications.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Retrieves the set of modifications to perform on the entry.
   *
   * @return  The set of modifications to perform on the entry.
   */
  public LDAPModification[] getModifiations()
  {
    return modifications;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element[] modElements;
    if ((modifications == null) || (modifications.length == 0))
    {
      modElements = new ASN1Element[0];
    }
    else
    {
      modElements = new ASN1Element[modifications.length];
      for (int i=0; i < modElements.length; i++)
      {
        modElements[i] = modifications[i].encode();
      }
    }


    ASN1Element[] modifyRequestElements = new ASN1Element[]
    {
      new ASN1OctetString(dn),
      new ASN1Sequence(modElements)
    };


    return new ASN1Sequence(MODIFY_REQUEST_TYPE, modifyRequestElements);
  }



  /**
   * Decodes the provided ASN.1 element as a modify request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded modify request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a modify request.
   */
  public static ModifyRequest decodeModifyRequest(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] modifyRequestElements;
    try
    {
      modifyRequestElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modify request sequence",
                                  e);
    }


    if (modifyRequestElements.length != 2)
    {
      throw new ProtocolException("There must be exactly 2 elements in a " +
                                  "modify request sequence");
    }


    String dn;
    try
    {
      dn = modifyRequestElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modify entry DN", e);
    }


    LDAPModification[] modifications;
    try
    {
      ASN1Element[] modElements =
           modifyRequestElements[1].decodeAsSequence().getElements();
      modifications = new LDAPModification[modElements.length];
      for (int i=0; i < modifications.length; i++)
      {
        modifications[i] = LDAPModification.decode(modElements[i]);
      }
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode modifications", e);
    }


    return new ModifyRequest(dn, modifications);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Modify Request";
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
    buffer.append(indentBuf).append("dn: ").append(dn).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("changetype: modify").
           append(LDAPMessage.EOL);

    if ((modifications != null) && (modifications.length > 0))
    {
      buffer.append(modifications[0].toString(indent));

      for (int i=1; i < modifications.length; i++)
      {
        buffer.append(indentBuf).append('-').append(LDAPMessage.EOL);
        buffer.append(modifications[i].toString(indent));
      }
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
    scriptWriter.println("#### Modify request captured at " + new Date());
    scriptWriter.println("# dn: " + dn);
    scriptWriter.println("# changetype: modify");

    for (int i=0; ((modifications != null) && (i < modifications.length)); i++)
    {
      if (i > 0)
      {
        scriptWriter.println("# -");
      }

      LDAPAttribute attr = modifications[i].getAttribute();
      switch (modifications[i].getModType())
      {
        case LDAPModification.MOD_TYPE_ADD:
          scriptWriter.println("# add: " + attr.getType());
          break;
        case LDAPModification.MOD_TYPE_DELETE:
          scriptWriter.println("# delete: " + attr.getType());
          break;
        case LDAPModification.MOD_TYPE_REPLACE:
          scriptWriter.println("# replace: " + attr.getType());
          break;
      }

      ASN1OctetString[] values = attr.getValues();
      for (int j=0; ((values != null) && (j < values.length)); j++)
      {
        scriptWriter.println("# " + attr.getType() + ": " +
                             values[j].getStringValue());
      }
    }

    scriptWriter.println("modSet.removeAll();");

    for (int i=0; ((modifications != null) && (i < modifications.length)); i++)
    {
      LDAPAttribute attr = modifications[i].getAttribute();
      switch (modifications[i].getModType())
      {
        case LDAPModification.MOD_TYPE_ADD:
          scriptWriter.println("mod.assign(mod.modTypeAdd(), \"" +
                               attr.getType() + "\");");
          break;
        case LDAPModification.MOD_TYPE_DELETE:
          scriptWriter.println("mod.assign(mod.modTypeDelete(), \"" +
                               attr.getType() + "\");");
          break;
        case LDAPModification.MOD_TYPE_REPLACE:
          scriptWriter.println("mod.assign(mod.modTypeReplace(), \"" +
                               attr.getType() + "\");");
          break;
      }

      ASN1OctetString[] values = attr.getValues();
      for (int j=0; ((values != null) && (j < values.length)); j++)
      {
        scriptWriter.println("mod.addValue(\"" + values[j].getStringValue() +
                             "\");");
      }

      scriptWriter.println("modSet.addModification(mod);");
    }

    scriptWriter.println("resultCode = conn.modify(\"" + dn + "\", modSet);");
    scriptWriter.println();
    scriptWriter.println();
  }
}

