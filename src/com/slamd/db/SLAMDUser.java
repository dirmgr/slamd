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



import java.security.MessageDigest;
import java.util.Arrays;

import com.slamd.asn1.ASN1Boolean;
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
public class SLAMDUser
{
  /**
   * The name of the encoded element that holds the name of the default folder
   * for this user.
   */
  public static final String ELEMENT_DEFAULT_FOLDER = "folder";



  /**
   * The name of the encoded element that holds the groups names associated with
   * this user.
   */
  public static final String ELEMENT_GROUPS = "groups";



  /**
   * The name of the encoded element that holds the hashed password for this
   * user.
   */
  public static final String ELEMENT_HASHED_PASSWORD = "password";



  /**
   * The name of the encoded element that indicates whether this user is an
   * administrator.
   */
  public static final String ELEMENT_IS_ADMIN = "isadmin";



  /**
   * The name of the encoded element that holds the user name for this user.
   */
  public static final String ELEMENT_USER_NAME = "username";



  // The SHA-1 digest used to hash passwords.
  static MessageDigest shaDigest;

  // Indicates whether this user is an administrator.
  boolean isAdmin;

  // The hashed password for this user.
  byte[] hashedPassword;

  // The default folder for this user.
  String defaultFolder;

  // The username for this user.
  String userName;

  // The set of groups in which this user is a member.
  String[] groupNames;



  static
  {
    try
    {
      shaDigest = MessageDigest.getInstance("SHA");
    } catch (Exception e) {}
  }



  /**
   * Creates a new user with the provided information.
   *
   * @param  userName        The user name for this user.
   * @param  hashedPassword  The SHA-1 hash of the password for this user.
   * @param  groupNames      The names of the groups in which this user is a
   *                         member.
   * @param  isAdmin         Indicates whether this user is an administrator.
   * @param  defaultFolder   The default folder for this user.
   */
  public SLAMDUser(String userName, byte[] hashedPassword, String[] groupNames,
                   boolean isAdmin, String defaultFolder)
  {
    this.userName       = userName;
    this.hashedPassword = hashedPassword;
    this.isAdmin        = isAdmin;
    this.defaultFolder  = defaultFolder;

    if (groupNames == null)
    {
      this.groupNames = new String[0];
    }
    else
    {
      this.groupNames = groupNames;
    }
  }



  /**
   * Retrieves the user name for this user.
   *
   * @return  The user name for this user.
   */
  public String getUserName()
  {
    return userName;
  }



  /**
   * Determines whether the provided clear-text password is the correct password
   * for this user.
   *
   * @param  password  The clear-text password to validate for this user.
   *
   * @return  <CODE>true</CODE> if the provided password is correct, or
   *          <CODE>false</CODE> if not.
   */
  public boolean checkPassword(String password)
  {
    byte[] pwHash = shaDigest.digest(ASN1Element.getBytes(password));
    if (pwHash.length != hashedPassword.length)
    {
      return false;
    }

    for (int i=0; i < pwHash.length; i++)
    {
      if (pwHash[i] != hashedPassword[i])
      {
        return false;
      }
    }

    return true;
  }



  /**
   * Specifies the password to use for the user.
   *
   * @param  password  The password to use for the user.
   */
  public void setPassword(String password)
  {
    hashedPassword = shaDigest.digest(ASN1Element.getBytes(password));
  }



  /**
   * Retrieves the names of the groups in which this user is a member.
   *
   * @return  The names of the groups in which this user is a member.
   */
  public String[] getGroupNames()
  {
    return groupNames;
  }



  /**
   * Determines whether this user is a member of the specified group.
   *
   * @param  groupName  The name of the group for which to make the
   *                    determination.
   *
   * @return  <CODE>true</CODE> if this user is a member of the specified group,
   *          or <CODE>false</CODE> if not.
   */
  public boolean memberOf(String groupName)
  {
    for (int i=0; i < groupNames.length; i++)
    {
      if (groupNames[i].equals(groupName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Specifies the names of the groups in which this user is a member.
   *
   * @param  groupNames  The names of the groups in which this user is a member.
   */
  public void setGroupNames(String[] groupNames)
  {
    if (groupNames == null)
    {
      groupNames = new String[0];
    }

    Arrays.sort(groupNames);
    this.groupNames = groupNames;
  }



  /**
   * Adds the provided group name to the set of groups associated with this
   * user.
   *
   * @param  groupName  The name of the group to add to the set of groups for
   *                    this user.
   */
  public void addGroupName(String groupName)
  {
    String[] newGroups = new String[groupNames.length+1];
    for (int i=0; i < groupNames.length; i++)
    {
      if (groupNames[i].equals(groupName))
      {
        return;
      }
      else
      {
        newGroups[i] = groupNames[i];
      }
    }

    newGroups[groupNames.length] = groupName;
    Arrays.sort(newGroups);
    groupNames = newGroups;
  }



  /**
   * Removes the provided group name from the set of groups associated with this
   * user.
   *
   * @param  groupName  The name of the group to remove from the set of groups
   *                    for this user.
   */
  public void removeGroupName(String groupName)
  {
    int pos = -1;
    for (int i=0; i < groupNames.length; i++)
    {
      if (groupNames[i].equals(groupName))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    String[] newGroupNames = new String[groupNames.length-1];
    System.arraycopy(groupNames, 0, newGroupNames, 0, pos);
    System.arraycopy(groupNames, pos+1, newGroupNames, pos,
                     (newGroupNames.length - pos));
    groupNames = newGroupNames;
  }



  /**
   * Indicates whether this user is an administrator with full rights.
   *
   * @return  <CODE>true</CODE> if this user is an administrator, or
   *          <CODE>false</CODE> if not.
   */
  public boolean isAdmin()
  {
    return isAdmin;
  }



  /**
   * Specifies whether this user is an administrator.
   *
   * @param  isAdmin  Specifies whether this user is an administrator.
   */
  public void setAdmin(boolean isAdmin)
  {
    this.isAdmin = isAdmin;
  }



  /**
   * Retrieves the name of the default folder for this user.
   *
   * @return  The name of the default folder for this user.
   */
  public String getDefaultFolder()
  {
    return defaultFolder;
  }



  /**
   * Specifies the name of the default folder for this user.
   *
   * @param  defaultFolder  The name of the default folder for this user.
   */
  public void setDefaultFolder(String defaultFolder)
  {
    this.defaultFolder = defaultFolder;
  }



  /**
   * Encodes the information for this user into a byte array.
   *
   * @return  The byte array containing the encoded user information.
   */
  public byte[] encode()
  {
    ASN1Element[] groupElements = new ASN1Element[groupNames.length];
    for (int i=0; i < groupNames.length; i++)
    {
      groupElements[i] = new ASN1OctetString(groupNames[i]);
    }

    ASN1Element[] userElements = new ASN1Element[]
    {
      new ASN1OctetString(ELEMENT_USER_NAME),
      new ASN1OctetString(userName),
      new ASN1OctetString(ELEMENT_HASHED_PASSWORD),
      new ASN1OctetString(hashedPassword),
      new ASN1OctetString(ELEMENT_GROUPS),
      new ASN1Sequence(groupElements),
      new ASN1OctetString(ELEMENT_IS_ADMIN),
      new ASN1Boolean(isAdmin),
      new ASN1OctetString(ELEMENT_DEFAULT_FOLDER),
      new ASN1OctetString(defaultFolder)
    };

    return new ASN1Sequence(userElements).encode();
  }



  /**
   * Decodes the provided byte array as information about a user.
   *
   * @param  encodedUser  The byte array containing the encoded user
   *                      information.
   *
   * @return  The user decoded from the provided byte array.
   *
   * @throws  DecodeException  If a problem occurs while trying to decode the
   *                           provided byte array.
   */
  public static SLAMDUser decode(byte[] encodedUser)
         throws DecodeException
  {
    try
    {
      boolean  isAdmin        = false;
      byte[]   hashedPassword = null;
      String   defaultFolder  = null;
      String   userName       = null;
      String[] groupNames     = new String[0];

      ASN1Element   element  = ASN1Element.decode(encodedUser);
      ASN1Element[] elements = element.decodeAsSequence().getElements();
      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();
        if (elementName.equals(ELEMENT_USER_NAME))
        {
          userName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_HASHED_PASSWORD))
        {
          hashedPassword = elements[i+1].decodeAsOctetString().getValue();
        }
        else if (elementName.equals(ELEMENT_GROUPS))
        {
          ASN1Element[] groupElements =
               elements[i+1].decodeAsSequence().getElements();
          groupNames = new String[groupElements.length];
          for (int j=0; j < groupNames.length; j++)
          {
            groupNames[j] =
                 groupElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_IS_ADMIN))
        {
          isAdmin = elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_DEFAULT_FOLDER))
        {
          defaultFolder = elements[i+1].decodeAsOctetString().getStringValue();
        }
      }

      return new SLAMDUser(userName, hashedPassword, groupNames, isAdmin,
                           defaultFolder);
    }
    catch (Exception e)
    {
      throw new DecodeException("Unable to decode user information:  " + e, e);
    }
  }
}

