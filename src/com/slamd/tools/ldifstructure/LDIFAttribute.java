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



import java.util.ArrayList;



/**
 * This class defines a data structure that holds information about an attribute
 * of an LDIF entry.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFAttribute
{
  // The set of values for this attribute.
  private ArrayList<String> values;

  // The name for this attribute converted to lowercase.
  private String lowerName;

  // The user-provided name for this attribute.
  private String name;



  /**
   * Creates a new LDIF attribute with the provided information.
   *
   * @param  name   The name of this attribute.
   * @param  value  The value for this attribute.
   */
  public LDIFAttribute(String name, String value)
  {
    this(name, LDIFReader.toLowerCase(name), value);
  }



  /**
   * Creates a new LDIF attribute with the provided information.
   *
   * @param  name       The name of this attribute.
   * @param  lowerName  The name of the attribute converted to lowercase.
   * @param  value      The value for this attribute.
   */
  public LDIFAttribute(String name, String lowerName, String value)
  {
    this.name      = name;
    this.lowerName = lowerName;

    values = new ArrayList<String>();
    values.add(value);
  }



  /**
   * Retrieves the name of this attribute.
   *
   * @return  the name of this attribute.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Retrieves the lowercase representation of the name for this attribute.
   *
   * @return  The lowercase representation of the name for this attribute.
   */
  public String getLowerName()
  {
    return lowerName;
  }



  /**
   * Retrieves the set of values for this attribute.
   *
   * @return  The set of values for this attribute.
   */
  public ArrayList getValues()
  {
    return values;
  }



  /**
   * Adds the provided value to this attribute.
   *
   * @param  value  The value to add to this attribute.
   */
  public void addValue(String value)
  {
    for (int i=0; i < values.size(); i++)
    {
      if (value.equalsIgnoreCase(values.get(i)))
      {
        return;
      }
    }

    values.add(value);
  }
}

