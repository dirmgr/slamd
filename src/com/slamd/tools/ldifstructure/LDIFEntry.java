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
package com.slamd.tools.ldifstructure;



import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * This class defines a data structure for holding an entry in LDIF form.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFEntry
{
  // The regular expression pattern to use to perform DN normalization.
  private static Pattern normalizationPattern =
       Pattern.compile("[ ]*([^\\\\],)[ ]+");

  // The set of attributes defined in this entry.
  private LinkedHashMap<String,LDIFAttribute> attributes;

  // The DN of this entry in the form provided by the user.
  private String dn;

  // The normalized version of the DN for this entry.
  private String normalizedDN;



  /**
   * Creates a new LDIF entry with the provided DN.
   *
   * @param  dn  The DN for this entry.
   */
  public LDIFEntry(String dn)
  {
    this.dn = dn;

    attributes   = new LinkedHashMap<String,LDIFAttribute>();

    Matcher normalizationMatcher =
         normalizationPattern.matcher(LDIFReader.toLowerCase(dn));
    normalizedDN = normalizationMatcher.replaceAll("$1");
  }



  /**
   * Retrieves the DN for this entry as provided by the user.
   *
   * @return  The DN for this entry as provided by the user.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Specifies a new DN for this entry.
   *
   * @param  dn  The new DN to use for this entry.
   */
  public void setDN(String dn)
  {
    this.dn = dn;

    Matcher normalizationMatcher =
         normalizationPattern.matcher(LDIFReader.toLowerCase(dn));
    normalizedDN = normalizationMatcher.replaceAll("$1");
  }



  /**
   * Retrieves the normalized DN for this entry.
   *
   * @return  The normalized DN for this entry.
   */
  public String getNormalizedDN()
  {
    return normalizedDN;
  }



  /**
   * Retrieves the normalized DN of the parent for this entry.
   *
   * @return  The normalized DN of the parent for this entry, or
   *          <CODE>null</CODE> if it does not have a parent.
   */
  public String getParentDN()
  {
    return getParentDN(normalizedDN);
  }



  /**
   * Retrieves the normalized grandparent DN for this entry.
   *
   * @return  The normalized grandparent DN for this entry, or <CODE>null</CODE>
   *          if it does not have a grandparent.
   */
  public String getGrandparentDN()
  {
    String parentDN = getParentDN(normalizedDN);
    if (parentDN == null)
    {
      return null;
    }
    else
    {
      return getParentDN(parentDN);
    }
  }



  /**
   * Retrieves the DN of the parent entry for the entry with the given
   * normalized DN.
   *
   * @param  normalizedDN  The normalized DN for which to retrieve the parent
   *                       DN.
   *
   * @return  The DN of the parent entry for the entry with the given normalized
   *          DN, or <CODE>null</CODE> if it does not have a parent.
   */
  public static String getParentDN(String normalizedDN)
  {
    int commaPos = normalizedDN.indexOf(',');
    if (commaPos <= 0)
    {
      return null;
    }

    if (normalizedDN.charAt(commaPos-1) != '\\')
    {
      return normalizedDN.substring(commaPos+1);
    }

    while (true)
    {
      commaPos = normalizedDN.indexOf(',', commaPos+1);
      if (commaPos < 0)
      {
        return null;
      }

      if (normalizedDN.indexOf(commaPos-1) != '\\')
      {
        return normalizedDN.substring(commaPos+1);
      }
    }
  }



  /**
   * Indicates whether this entry is a descendant of the entry with the provided
   * normalized DN.  Note that if the provided DN is equal to the DN of this
   * entry, then it will not be considered a descendant.
   *
   * @param  normalizedDN  The normalized DN of the entry for which to make the
   *                       determination.
   *
   * @return  <CODE>true</CODE> if this entry is determined to be a descendant
   *          of the specified entry, or <CODE>false</CODE> if not.
   */
  public boolean isDescendantOf(String normalizedDN)
  {
    if (normalizedDN == null)
    {
      return false;
    }
    if (normalizedDN.equals(this.normalizedDN))
    {
      return false;
    }

    String commaDN = ',' + normalizedDN;
    if (this.normalizedDN.endsWith(commaDN))
    {
      int pos = this.normalizedDN.length() - commaDN.length() - 1;
      if ((pos > 0) && (this.normalizedDN.charAt(pos) != '\\'))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the set of attributes defined for this entry.
   *
   * @return  The set of attributes defined for this entry.
   */
  public LinkedHashMap<String,LDIFAttribute> getAttributes()
  {
    return attributes;
  }



  /**
   * Retrieves the specified attribute for this entry.
   *
   * @param  lowerName  The name of the attribute, converted to lowercase.
   *
   * @return  The requested attribute, or <CODE>null</CODE> if the specified
   *          attribute does not exist in this entry.
   */
  public LDIFAttribute getAttribute(String lowerName)
  {
    return attributes.get(lowerName);
  }



  /**
   * Adds an attribute with the specified name and value to this entry.
   *
   * @param  name       The name for the attribute to add.
   * @param  lowerName  The lowercase name for the attribute to add.
   * @param  value      The value for the attribute to add.
   */
  public void addAttribute(String name, String lowerName, String value)
  {
    LDIFAttribute attr = attributes.get(lowerName);
    if (attr == null)
    {
      attr = new LDIFAttribute(name, lowerName, value);
      attributes.put(lowerName, attr);
    }
    else
    {
      attr.addValue(value);
    }
  }
}

