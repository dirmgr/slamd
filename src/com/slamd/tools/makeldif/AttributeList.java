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
package com.slamd.tools.makeldif;



import java.util.ArrayList;



/**
 * This class defines an attribute list that holds attribute names and values,
 * as well as the separators between them in LDIF output (can be either ": " or
 * ":: " depending on whether the value is base64-encoded or not).
 *
 *
 * @author   Neil A. Wilson
 */
public class AttributeList
{
  // The list of attribute names in all lowercase (for faster matching)
  private ArrayList<String> lowerNames;

  // The list of attribute names in the specified in the template
  private ArrayList<String> names;

  // The list of separators to use between the names and values
  private ArrayList<String> separators;

  // The list of attribute values.
  private ArrayList<String> values;



  /**
   * Creates a new attribute list with no attribute information.
   */
  public AttributeList()
  {
    lowerNames = new ArrayList<String>();
    names      = new ArrayList<String>();
    separators = new ArrayList<String>();
    values     = new ArrayList<String>();
  }



  /**
   * Adds a new attribute definition to this attribute list.
   *
   * @param  name       The name of the attribute to add.
   * @param  lowerName  The name of the attribute to add, converted to
   *                    lowercase.
   * @param  separator  The separator to place between the name and the value
   *                    in the LDIF output.
   * @param  value      The value of the attribute to add.
   */
  public void addAttribute(String name, String lowerName, String separator,
                           String value)
  {
    names.add(name);
    lowerNames.add(lowerName);
    separators.add(separator);
    values.add(value);
  }



  /**
   * Retrieves the value of the specified attribute in this attribute list.
   * This is used by the attribute replacement code.  If the specified attribute
   * has multiple values, then the first value added to the list will be
   * returned.  If the specified attribute is not contained in the attribute
   * list, then an empty string will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.
   *
   * @return  The value of the specified attribute.
   */
  public String getValue(String attributeName)
  {
    String lowerName = attributeName.toLowerCase();

    for (int i=0; i < lowerNames.size(); i++)
    {
      if (lowerName.equals(lowerNames.get(i)))
      {
        return values.get(i);
      }
    }

    return "";
  }



  /**
   * Retrieves the value of the specified attribute in this attribute list.
   * This is used by the attribute replacement code.  If the specified attribute
   * has multiple values, then the first value added to the list will be
   * returned.  If the specified attribute is not contained in the attribute
   * list, then an empty string will be returned.
   *
   * @param  lowerName  The name of the attribute for which to retrieve the
   *                    value, already represented as a lowercase string.
   *
   * @return  The value of the specified attribute.
   */
  public String getValueForLowerName(String lowerName)
  {
    for (int i=0; i < lowerNames.size(); i++)
    {
      if (lowerName.equals(lowerNames.get(i)))
      {
        return values.get(i);
      }
    }

    return "";
  }



  /**
   * Retrieves the set of values for the specified attribute in this attribute
   * list.  If the specified attribute is not contained in the attribute list,
   * then an empty array will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        set of values.
   *
   * @return  The set of values for the specified attribute.
   */
  public String[] getValues(String attributeName)
  {
    ArrayList<String> valueList = new ArrayList<String>();

    String lowerName = attributeName.toLowerCase();

    for (int i=0; i < lowerNames.size(); i++)
    {
      if (lowerName.equals(lowerNames.get(i)))
      {
        valueList.add(values.get(i));
      }
    }

    String[] values = new String[valueList.size()];
    valueList.toArray(values);
    return values;
  }



  /**
   * Returns a string representation of this attribute list.  The output will be
   * in LDIF format (but will not have a DN).
   *
   * @return  A string representation of this attribute list.
   */
  public String toString()
  {
    StringBuilder returnStr = new StringBuilder();

    for (int i=0; i < names.size(); i++)
    {
      returnStr.append(names.get(i));
      returnStr.append(separators.get(i));
      returnStr.append(values.get(i));
      returnStr.append(MakeLDIF.EOL);
    }

    returnStr.append(MakeLDIF.EOL);


    return returnStr.toString();
  }
}

