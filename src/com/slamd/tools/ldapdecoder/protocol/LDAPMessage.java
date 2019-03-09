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

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP message, which is the envelope that encompasses
 * all communication using the LDAP protocol.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPMessage
{
  /**
   * The end-of-line character for this platform.
   */
  public static final String EOL = System.getProperty("line.separator");



  // The message ID for this LDAP message.
  private int messageID;

  // The set of controls associated with this LDAP message.
  private LDAPControl[] controls;

  // The protocol op for this LDAP message.
  private ProtocolOp protocolOp;



  /**
   * Creates a new LDAP message with the provided message ID, protocol op, and
   * no controls.
   *
   * @param  messageID   The message ID for this LDAP message.
   * @param  protocolOp  The protocol op for this LDAP message.
   */
  public LDAPMessage(int messageID, ProtocolOp protocolOp)
  {
    this.messageID  = messageID;
    this.protocolOp = protocolOp;
    this.controls   = null;
  }



  /**
   * Creates a new LDAP message with the provided message ID, protocol op, and
   * set of controls.
   *
   * @param  messageID   The message ID for this LDAP message.
   * @param  protocolOp  The protocol op for this LDAP message.
   * @param  controls    The set of controls associated with this LDAP message.
   */
  public LDAPMessage(int messageID, ProtocolOp protocolOp,
                     LDAPControl[] controls)
  {
    this.messageID  = messageID;
    this.protocolOp = protocolOp;

    if ((controls == null) || (controls.length == 0))
    {
      this.controls = null;
    }
    else
    {
      this.controls = controls;
    }
  }



  /**
   * Retrieves the message ID for this LDAP message.
   *
   * @return  The message ID for this LDAP message.
   */
  public int getMessageID()
  {
    return messageID;
  }



  /**
   * Retrieves the protocol op for this LDAP message.
   *
   * @return  The protocol op for this LDAP message.
   */
  public ProtocolOp getProtocolOp()
  {
    return protocolOp;
  }



  /**
   * Retrieves the set of controls for this LDAP message.
   *
   * @return  The set of controls for this LDAP message, or <CODE>null</CODE>
   *          if there are no controls.
   */
  public LDAPControl[] getControls()
  {
    return controls;
  }



  /**
   * Encodes this LDAP message to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded LDAP message.
   */
  public ASN1Element encode()
  {
    ASN1Element[] messageElements;

    if ((controls == null) || (controls.length == 0))
    {
      messageElements = new ASN1Element[]
      {
        new ASN1Integer(messageID),
        protocolOp.encode()
      };
    }
    else
    {
      messageElements = new ASN1Element[]
      {
        new ASN1Integer(messageID),
        protocolOp.encode(),
        LDAPControl.encode(controls)
      };
    }

    return new ASN1Sequence(messageElements);
  }



  /**
   * Decodes the provided ASN.1 element as an LDAP message.
   *
   * @param  element  The ASN.1 element to be decoded as an LDAP message.
   *
   * @return  The decoded LDAP message.
   *
   * @throws  ProtocolException  If a problem occurs while attempting to decode
   *                             the ASN.1 element to an LDAP message.
   */
  public static LDAPMessage decode(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] messageElements;
    try
    {
      messageElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode ASN.1 element as a " +
                                  "sequence.", e);
    }


    if ((messageElements.length < 2) || (messageElements.length > 3))
    {
      throw new ProtocolException("There must be either 2 or 3 elements in " +
                                  "an LDAP message sequence.");
    }


    int messageID;
    try
    {
      messageID = messageElements[0].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode the message ID", e);

    }


    ProtocolOp protocolOp = ProtocolOp.decode(messageElements[1]);


    LDAPControl[] controls = null;
    if (messageElements.length == 3)
    {
      controls = LDAPControl.decodeControls(messageElements[2]);
    }


    return new LDAPMessage(messageID, protocolOp, controls);
  }



  /**
   * Retrieves a string representation of this LDAP message.
   *
   * @return  A string representation of this LDAP message.
   */
  public String toString()
  {
    return toString(0);
  }



  /**
   * Retrieves a string representation of this LDAP message using the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the message output.
   *
   * @return  A string representation of this LDAP message.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append(protocolOp.getProtocolOpType()).append(EOL);
    buffer.append(indentBuf).append("    Message ID:  ").append(messageID).
           append(EOL);
    buffer.append(indentBuf).append("    ").
           append(protocolOp.getProtocolOpType()).append(" Protocol Op").
           append(EOL);
    buffer.append(protocolOp.toString(indent+8));

    if ((controls != null) && (controls.length > 0))
    {
      for (int i=0; i < controls.length; i++)
      {
        buffer.append(controls[i].toString(indent+4));
      }
    }

    return buffer.toString();
  }



  /**
   * Writes this LDAP message to the provided print stream in a form that is
   * suitable for inclusion in a SLAMD script.  It is acceptable for nothing to
   * be written if this message isn't one that would be associated with a client
   * request.
   *
   * @param  scriptWriter  The script writer to which the generated script
   *                       should be written.
   */
  public void toSLAMDScript(PrintStream scriptWriter)
  {
    protocolOp.toSLAMDScript(scriptWriter);
  }



  /**
   * Retrieves a string representation of the provided byte array.
   *
   * @param  byteArray  The byte array to be displayed as a string.
   *
   * @return  A string representation of the provided byte array.
   */
  public static String byteArrayToString(byte[] byteArray)
  {
    return byteArrayToString(byteArray, 0);
  }



  /**
   * Retrieves a string representation of the provided byte array using the
   * specified indent.
   *
   * @param  byteArray  The byte array to be displayed as a string.
   * @param  indent     The number of spaces to indent the output.
   *
   * @return  A string representation of the provided byte array.
   */
  public static String byteArrayToString(byte[] byteArray, int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    StringBuilder buffer   = new StringBuilder();
    StringBuilder hexBuf   = new StringBuilder();
    StringBuilder asciiBuf = new StringBuilder();
    for (int i=0; i < byteArray.length; i++)
    {
      switch (byteArray[i])
      {
        case 0x00:
          hexBuf.append("00 ");
          asciiBuf.append('.');
          break;
        case 0x01:
          hexBuf.append("01 ");
          asciiBuf.append('.');
          break;
        case 0x02:
          hexBuf.append("02 ");
          asciiBuf.append('.');
          break;
        case 0x03:
          hexBuf.append("03 ");
          asciiBuf.append('.');
          break;
        case 0x04:
          hexBuf.append("04 ");
          asciiBuf.append('.');
          break;
        case 0x05:
          hexBuf.append("05 ");
          asciiBuf.append('.');
          break;
        case 0x06:
          hexBuf.append("06 ");
          asciiBuf.append('.');
          break;
        case 0x07:
          hexBuf.append("07 ");
          asciiBuf.append('.');
          break;
        case 0x08:
          hexBuf.append("08 ");
          asciiBuf.append('.');
          break;
        case 0x09:
          hexBuf.append("09 ");
          asciiBuf.append('.');
          break;
        case 0x0A:
          hexBuf.append("0A ");
          asciiBuf.append('.');
          break;
        case 0x0B:
          hexBuf.append("0B ");
          asciiBuf.append('.');
          break;
        case 0x0C:
          hexBuf.append("0C ");
          asciiBuf.append('.');
          break;
        case 0x0D:
          hexBuf.append("0D ");
          asciiBuf.append('.');
          break;
        case 0x0E:
          hexBuf.append("0E ");
          asciiBuf.append('.');
          break;
        case 0x0F:
          hexBuf.append("0F ");
          asciiBuf.append('.');
          break;
        case 0x10:
          hexBuf.append("10 ");
          asciiBuf.append('.');
          break;
        case 0x11:
          hexBuf.append("11 ");
          asciiBuf.append('.');
          break;
        case 0x12:
          hexBuf.append("12 ");
          asciiBuf.append('.');
          break;
        case 0x13:
          hexBuf.append("13 ");
          asciiBuf.append('.');
          break;
        case 0x14:
          hexBuf.append("14 ");
          asciiBuf.append('.');
          break;
        case 0x15:
          hexBuf.append("15 ");
          asciiBuf.append('.');
          break;
        case 0x16:
          hexBuf.append("16 ");
          asciiBuf.append('.');
          break;
        case 0x17:
          hexBuf.append("17 ");
          asciiBuf.append('.');
          break;
        case 0x18:
          hexBuf.append("18 ");
          asciiBuf.append('.');
          break;
        case 0x19:
          hexBuf.append("19 ");
          asciiBuf.append('.');
          break;
        case 0x1A:
          hexBuf.append("1A ");
          asciiBuf.append('.');
          break;
        case 0x1B:
          hexBuf.append("1B ");
          asciiBuf.append('.');
          break;
        case 0x1C:
          hexBuf.append("1C ");
          asciiBuf.append('.');
          break;
        case 0x1D:
          hexBuf.append("1D ");
          asciiBuf.append('.');
          break;
        case 0x1E:
          hexBuf.append("1E ");
          asciiBuf.append('.');
          break;
        case 0x1F:
          hexBuf.append("1F ");
          asciiBuf.append('.');
          break;
        case 0x20:
          hexBuf.append("20 ");
          asciiBuf.append(' ');
          break;
        case 0x21:
          hexBuf.append("21 ");
          asciiBuf.append('!');
          break;
        case 0x22:
          hexBuf.append("22 ");
          asciiBuf.append('"');
          break;
        case 0x23:
          hexBuf.append("23 ");
          asciiBuf.append('#');
          break;
        case 0x24:
          hexBuf.append("24 ");
          asciiBuf.append('$');
          break;
        case 0x25:
          hexBuf.append("25 ");
          asciiBuf.append('%');
          break;
        case 0x26:
          hexBuf.append("26 ");
          asciiBuf.append('&');
          break;
        case 0x27:
          hexBuf.append("27 ");
          asciiBuf.append('\'');
          break;
        case 0x28:
          hexBuf.append("28 ");
          asciiBuf.append('(');
          break;
        case 0x29:
          hexBuf.append("29 ");
          asciiBuf.append(')');
          break;
        case 0x2A:
          hexBuf.append("2A ");
          asciiBuf.append('*');
          break;
        case 0x2B:
          hexBuf.append("2B ");
          asciiBuf.append('+');
          break;
        case 0x2C:
          hexBuf.append("2C ");
          asciiBuf.append(',');
          break;
        case 0x2D:
          hexBuf.append("2D ");
          asciiBuf.append('-');
          break;
        case 0x2E:
          hexBuf.append("2E ");
          asciiBuf.append('.');
          break;
        case 0x2F:
          hexBuf.append("2F ");
          asciiBuf.append('/');
          break;
        case 0x30:
          hexBuf.append("30 ");
          asciiBuf.append('0');
          break;
        case 0x31:
          hexBuf.append("31 ");
          asciiBuf.append('1');
          break;
        case 0x32:
          hexBuf.append("32 ");
          asciiBuf.append('2');
          break;
        case 0x33:
          hexBuf.append("33 ");
          asciiBuf.append('3');
          break;
        case 0x34:
          hexBuf.append("34 ");
          asciiBuf.append('4');
          break;
        case 0x35:
          hexBuf.append("35 ");
          asciiBuf.append('5');
          break;
        case 0x36:
          hexBuf.append("36 ");
          asciiBuf.append('6');
          break;
        case 0x37:
          hexBuf.append("37 ");
          asciiBuf.append('7');
          break;
        case 0x38:
          hexBuf.append("38 ");
          asciiBuf.append('8');
          break;
        case 0x39:
          hexBuf.append("39 ");
          asciiBuf.append('9');
          break;
        case 0x3A:
          hexBuf.append("3A ");
          asciiBuf.append(':');
          break;
        case 0x3B:
          hexBuf.append("3B ");
          asciiBuf.append(';');
          break;
        case 0x3C:
          hexBuf.append("3C ");
          asciiBuf.append('<');
          break;
        case 0x3D:
          hexBuf.append("3D ");
          asciiBuf.append('=');
          break;
        case 0x3E:
          hexBuf.append("3E ");
          asciiBuf.append('>');
          break;
        case 0x3F:
          hexBuf.append("3F ");
          asciiBuf.append('?');
          break;
        case 0x40:
          hexBuf.append("40 ");
          asciiBuf.append('@');
          break;
        case 0x41:
          hexBuf.append("41 ");
          asciiBuf.append('A');
          break;
        case 0x42:
          hexBuf.append("42 ");
          asciiBuf.append('B');
          break;
        case 0x43:
          hexBuf.append("43 ");
          asciiBuf.append('C');
          break;
        case 0x44:
          hexBuf.append("44 ");
          asciiBuf.append('D');
          break;
        case 0x45:
          hexBuf.append("45 ");
          asciiBuf.append('E');
          break;
        case 0x46:
          hexBuf.append("46 ");
          asciiBuf.append('F');
          break;
        case 0x47:
          hexBuf.append("47 ");
          asciiBuf.append('G');
          break;
        case 0x48:
          hexBuf.append("48 ");
          asciiBuf.append('H');
          break;
        case 0x49:
          hexBuf.append("49 ");
          asciiBuf.append('I');
          break;
        case 0x4A:
          hexBuf.append("4A ");
          asciiBuf.append('J');
          break;
        case 0x4B:
          hexBuf.append("4B ");
          asciiBuf.append('K');
          break;
        case 0x4C:
          hexBuf.append("4C ");
          asciiBuf.append('L');
          break;
        case 0x4D:
          hexBuf.append("4D ");
          asciiBuf.append('M');
          break;
        case 0x4E:
          hexBuf.append("4E ");
          asciiBuf.append('N');
          break;
        case 0x4F:
          hexBuf.append("4F ");
          asciiBuf.append('O');
          break;
        case 0x50:
          hexBuf.append("50 ");
          asciiBuf.append('P');
          break;
        case 0x51:
          hexBuf.append("51 ");
          asciiBuf.append('Q');
          break;
        case 0x52:
          hexBuf.append("52 ");
          asciiBuf.append('R');
          break;
        case 0x53:
          hexBuf.append("53 ");
          asciiBuf.append('S');
          break;
        case 0x54:
          hexBuf.append("54 ");
          asciiBuf.append('T');
          break;
        case 0x55:
          hexBuf.append("55 ");
          asciiBuf.append('U');
          break;
        case 0x56:
          hexBuf.append("56 ");
          asciiBuf.append('V');
          break;
        case 0x57:
          hexBuf.append("57 ");
          asciiBuf.append('W');
          break;
        case 0x58:
          hexBuf.append("58 ");
          asciiBuf.append('X');
          break;
        case 0x59:
          hexBuf.append("59 ");
          asciiBuf.append('Y');
          break;
        case 0x5A:
          hexBuf.append("5A ");
          asciiBuf.append('Z');
          break;
        case 0x5B:
          hexBuf.append("5B ");
          asciiBuf.append('[');
          break;
        case 0x5C:
          hexBuf.append("5C ");
          asciiBuf.append('\\');
          break;
        case 0x5D:
          hexBuf.append("5D ");
          asciiBuf.append(']');
          break;
        case 0x5E:
          hexBuf.append("5E ");
          asciiBuf.append('^');
          break;
        case 0x5F:
          hexBuf.append("5F ");
          asciiBuf.append('_');
          break;
        case 0x60:
          hexBuf.append("60 ");
          asciiBuf.append('`');
          break;
        case 0x61:
          hexBuf.append("61 ");
          asciiBuf.append('a');
          break;
        case 0x62:
          hexBuf.append("62 ");
          asciiBuf.append('b');
          break;
        case 0x63:
          hexBuf.append("63 ");
          asciiBuf.append('c');
          break;
        case 0x64:
          hexBuf.append("64 ");
          asciiBuf.append('d');
          break;
        case 0x65:
          hexBuf.append("65 ");
          asciiBuf.append('e');
          break;
        case 0x66:
          hexBuf.append("66 ");
          asciiBuf.append('f');
          break;
        case 0x67:
          hexBuf.append("67 ");
          asciiBuf.append('g');
          break;
        case 0x68:
          hexBuf.append("68 ");
          asciiBuf.append('h');
          break;
        case 0x69:
          hexBuf.append("69 ");
          asciiBuf.append('i');
          break;
        case 0x6A:
          hexBuf.append("6A ");
          asciiBuf.append('j');
          break;
        case 0x6B:
          hexBuf.append("6B ");
          asciiBuf.append('k');
          break;
        case 0x6C:
          hexBuf.append("6C ");
          asciiBuf.append('l');
          break;
        case 0x6D:
          hexBuf.append("6D ");
          asciiBuf.append('m');
          break;
        case 0x6E:
          hexBuf.append("6E ");
          asciiBuf.append('n');
          break;
        case 0x6F:
          hexBuf.append("6F ");
          asciiBuf.append('o');
          break;
        case 0x70:
          hexBuf.append("70 ");
          asciiBuf.append('p');
          break;
        case 0x71:
          hexBuf.append("71 ");
          asciiBuf.append('q');
          break;
        case 0x72:
          hexBuf.append("72 ");
          asciiBuf.append('r');
          break;
        case 0x73:
          hexBuf.append("73 ");
          asciiBuf.append('s');
          break;
        case 0x74:
          hexBuf.append("74 ");
          asciiBuf.append('t');
          break;
        case 0x75:
          hexBuf.append("75 ");
          asciiBuf.append('u');
          break;
        case 0x76:
          hexBuf.append("76 ");
          asciiBuf.append('v');
          break;
        case 0x77:
          hexBuf.append("77 ");
          asciiBuf.append('w');
          break;
        case 0x78:
          hexBuf.append("78 ");
          asciiBuf.append('x');
          break;
        case 0x79:
          hexBuf.append("79 ");
          asciiBuf.append('y');
          break;
        case 0x7A:
          hexBuf.append("7A ");
          asciiBuf.append('z');
          break;
        case 0x7B:
          hexBuf.append("7B ");
          asciiBuf.append('{');
          break;
        case 0x7C:
          hexBuf.append("7C ");
          asciiBuf.append('|');
          break;
        case 0x7D:
          hexBuf.append("7D ");
          asciiBuf.append('}');
          break;
        case 0x7E:
          hexBuf.append("7E ");
          asciiBuf.append('~');
          break;
        case 0x7F:
          hexBuf.append("7F ");
          asciiBuf.append('.');
          break;
        case (byte) 0x80:
          hexBuf.append("80 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x81:
          hexBuf.append("81 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x82:
          hexBuf.append("82 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x83:
          hexBuf.append("83 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x84:
          hexBuf.append("84 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x85:
          hexBuf.append("85 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x86:
          hexBuf.append("86 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x87:
          hexBuf.append("87 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x88:
          hexBuf.append("88 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x89:
          hexBuf.append("89 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8A:
          hexBuf.append("8A ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8B:
          hexBuf.append("8B ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8C:
          hexBuf.append("8C ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8D:
          hexBuf.append("8D ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8E:
          hexBuf.append("8E ");
          asciiBuf.append('.');
          break;
        case (byte) 0x8F:
          hexBuf.append("8F ");
          asciiBuf.append('.');
          break;
        case (byte) 0x90:
          hexBuf.append("90 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x91:
          hexBuf.append("91 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x92:
          hexBuf.append("92 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x93:
          hexBuf.append("93 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x94:
          hexBuf.append("94 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x95:
          hexBuf.append("95 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x96:
          hexBuf.append("96 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x97:
          hexBuf.append("97 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x98:
          hexBuf.append("98 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x99:
          hexBuf.append("99 ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9A:
          hexBuf.append("9A ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9B:
          hexBuf.append("9B ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9C:
          hexBuf.append("9C ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9D:
          hexBuf.append("9D ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9E:
          hexBuf.append("9E ");
          asciiBuf.append('.');
          break;
        case (byte) 0x9F:
          hexBuf.append("9F ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA0:
          hexBuf.append("A0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA1:
          hexBuf.append("A1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA2:
          hexBuf.append("A2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA3:
          hexBuf.append("A3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA4:
          hexBuf.append("A4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA5:
          hexBuf.append("A5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA6:
          hexBuf.append("A6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA7:
          hexBuf.append("A7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA8:
          hexBuf.append("A8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xA9:
          hexBuf.append("A9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAA:
          hexBuf.append("AA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAB:
          hexBuf.append("AB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAC:
          hexBuf.append("AC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAD:
          hexBuf.append("AD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAE:
          hexBuf.append("AE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xAF:
          hexBuf.append("AF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB0:
          hexBuf.append("B0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB1:
          hexBuf.append("B1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB2:
          hexBuf.append("B2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB3:
          hexBuf.append("B3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB4:
          hexBuf.append("B4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB5:
          hexBuf.append("B5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB6:
          hexBuf.append("B6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB7:
          hexBuf.append("B7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB8:
          hexBuf.append("B8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xB9:
          hexBuf.append("B9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBA:
          hexBuf.append("BA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBB:
          hexBuf.append("BB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBC:
          hexBuf.append("BC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBD:
          hexBuf.append("BD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBE:
          hexBuf.append("BE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xBF:
          hexBuf.append("BF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC0:
          hexBuf.append("C0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC1:
          hexBuf.append("C1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC2:
          hexBuf.append("C2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC3:
          hexBuf.append("C3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC4:
          hexBuf.append("C4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC5:
          hexBuf.append("C5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC6:
          hexBuf.append("C6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC7:
          hexBuf.append("C7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC8:
          hexBuf.append("C8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xC9:
          hexBuf.append("C9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCA:
          hexBuf.append("CA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCB:
          hexBuf.append("CB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCC:
          hexBuf.append("CC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCD:
          hexBuf.append("CD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCE:
          hexBuf.append("CE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xCF:
          hexBuf.append("CF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD0:
          hexBuf.append("D0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD1:
          hexBuf.append("D1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD2:
          hexBuf.append("D2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD3:
          hexBuf.append("D3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD4:
          hexBuf.append("D4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD5:
          hexBuf.append("D5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD6:
          hexBuf.append("D6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD7:
          hexBuf.append("D7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD8:
          hexBuf.append("D8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xD9:
          hexBuf.append("D9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDA:
          hexBuf.append("DA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDB:
          hexBuf.append("DB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDC:
          hexBuf.append("DC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDD:
          hexBuf.append("DD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDE:
          hexBuf.append("DE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xDF:
          hexBuf.append("DF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE0:
          hexBuf.append("E0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE1:
          hexBuf.append("E1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE2:
          hexBuf.append("E2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE3:
          hexBuf.append("E3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE4:
          hexBuf.append("E4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE5:
          hexBuf.append("E5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE6:
          hexBuf.append("E6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE7:
          hexBuf.append("E7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE8:
          hexBuf.append("E8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xE9:
          hexBuf.append("E9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEA:
          hexBuf.append("EA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEB:
          hexBuf.append("EB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEC:
          hexBuf.append("EC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xED:
          hexBuf.append("ED ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEE:
          hexBuf.append("EE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xEF:
          hexBuf.append("EF ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF0:
          hexBuf.append("F0 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF1:
          hexBuf.append("F1 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF2:
          hexBuf.append("F2 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF3:
          hexBuf.append("F3 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF4:
          hexBuf.append("F4 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF5:
          hexBuf.append("F5 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF6:
          hexBuf.append("F6 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF7:
          hexBuf.append("F7 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF8:
          hexBuf.append("F8 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xF9:
          hexBuf.append("F9 ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFA:
          hexBuf.append("FA ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFB:
          hexBuf.append("FB ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFC:
          hexBuf.append("FC ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFD:
          hexBuf.append("FD ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFE:
          hexBuf.append("FE ");
          asciiBuf.append('.');
          break;
        case (byte) 0xFF:
          hexBuf.append("FF ");
          asciiBuf.append('.');
          break;
      }

      if ((i % 16) == 15)
      {
        buffer.append(indentBuf).append(hexBuf).append(' ').append(asciiBuf).
               append(EOL);

        hexBuf   = new StringBuilder();
        asciiBuf = new StringBuilder();
      }
      else if ((i % 8) == 7)
      {
        hexBuf.append(' ');
        asciiBuf.append(' ');
      }
    }

    int charsLeft = 16 - (byteArray.length % 16);
    if (charsLeft < 16)
    {
      for (int i=0; i < charsLeft; i++)
      {
        hexBuf.append("   ");
      }
      if (charsLeft > 8)
      {
        hexBuf.append(' ');
      }
    }

    buffer.append(indentBuf).append(hexBuf).append(' ').append(asciiBuf).
           append(EOL);
    return buffer.toString();
  }
}

