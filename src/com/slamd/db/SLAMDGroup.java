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
package com.slamd.db;



import java.util.Arrays;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines a set of information about a user defined in the SLAMD
 * database, including the username and hashed password, the default folder,
 * and the set of groups in which the user is a member.
 *
 *
 * @author  Neil A. Wilson
 */
public class SLAMDGroup
{
  /**
   * The name of the encoded element that holds the name of this group.
   */
  public static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that holds the member names for this group.
   */
  public static final String ELEMENT_MEMBERS = "members";



  // The name of this group.
  String groupName;

  // The user names of the members of this group.
  String[] memberNames;



  /**
   * Creates a new SLAMD group with the provided information.
   *
   * @param  groupName    The name of this group.
   * @param  memberNames  The user names of the users that are members of this
   *                      group.
   */
  public SLAMDGroup(String groupName, String[] memberNames)
  {
    this.groupName   = groupName;

    if (memberNames == null)
    {
      this.memberNames = new String[0];
    }
    else
    {
      this.memberNames = memberNames;
    }
  }



  /**
   * Retrieves the name of this group.
   *
   * @return  The name of this group.
   */
  public String getGroupName()
  {
    return groupName;
  }



  /**
   * Retrieves the names of the members of this group.
   *
   * @return  The names of the members of this group.
   */
  public String[] getMemberNames()
  {
    return memberNames;
  }



  /**
   * Determines whether the specified user is a member of this group.
   *
   * @param  userName  The user name of the user for which to make the
   *                   determination.
   *
   * @return  <CODE>true</CODE> if the user is a member of this group, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isMember(String userName)
  {
    for (int i=0; i < memberNames.length; i++)
    {
      if (memberNames[i].equals(userName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Specifies the set of members to use for this group.
   *
   * @param  memberNames  The user names of the users that should be members of
   *                      this group.
   */
  public void setMemberNames(String[] memberNames)
  {
    if (memberNames == null)
    {
      memberNames = new String[0];
    }

    Arrays.sort(memberNames);
    this.memberNames = memberNames;
  }



  /**
   * Adds the specified user as a member of this group.
   *
   * @param  userName  The user name of the user to add to this group.
   */
  public void addMember(String userName)
  {
    String[] newMemberNames = new String[memberNames.length+1];
    for (int i=0; i < memberNames.length; i++)
    {
      if (memberNames[i].equals(userName))
      {
        return;
      }
      else
      {
        newMemberNames[i] = userName;
      }
    }

    newMemberNames[memberNames.length] = userName;
    Arrays.sort(newMemberNames);
    memberNames = newMemberNames;
  }



  /**
   * Removes the specified user from this group.
   *
   * @param  userName  The user name of the user to remove from this group.
   */
  public void removeMember(String userName)
  {
    int pos = -1;
    for (int i=0; i < memberNames.length; i++)
    {
      if (memberNames[i].equals(userName))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    String[] newUserNames = new String[memberNames.length-1];
    System.arraycopy(memberNames, 0, newUserNames, 0, pos);
    System.arraycopy(memberNames, pos+1, newUserNames, pos,
                     (newUserNames.length - pos));
    memberNames = newUserNames;
  }



  /**
   * Encodes this group into a byte array.
   *
   * @return  The byte array containing the encoded representation of this
   *          group.
   */
  public byte[] encode()
  {
    ASN1Element[] memberElements = new ASN1Element[memberNames.length];
    for (int i=0; i < memberNames.length; i++)
    {
      memberElements[i] = new ASN1OctetString(memberNames[i]);
    }


    ASN1Element[] groupElements = new ASN1Element[]
    {
      new ASN1OctetString(ELEMENT_NAME),
      new ASN1OctetString(groupName),
      new ASN1OctetString(ELEMENT_MEMBERS),
      new ASN1Sequence(memberElements)
    };

    return new ASN1Sequence(groupElements).encode();
  }



  /**
   * Decodes the provided byte array as a SLAMD group.
   *
   * @param  encodedGroup  The byte array containing the encoded group
   *                       information.
   *
   * @return  The decoded SLAMD group.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           provided byte array as a SLAMD group.
   */
  public static SLAMDGroup decode(byte[] encodedGroup)
         throws DecodeException
  {
    try
    {
      String   groupName   = null;
      String[] memberNames = new String[0];

      ASN1Element   element  = ASN1Element.decode(encodedGroup);
      ASN1Element[] elements = element.decodeAsSequence().getElements();
      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();

        if (elementName.equals(ELEMENT_NAME))
        {
          groupName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_MEMBERS))
        {
          ASN1Element[] memberElements =
               elements[i+1].decodeAsSequence().getElements();
          memberNames = new String[memberElements.length];
          for (int j=0; j < memberNames.length; j++)
          {
            memberNames[j] =
                 memberElements[j].decodeAsOctetString().getStringValue();
          }
        }
      }

      return new SLAMDGroup(groupName, memberNames);
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode the SLAMD group:  " + e, e);
    }
  }
}

