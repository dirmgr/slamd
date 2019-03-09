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
package com.slamd.scripting.ldap;



import java.util.ArrayList;
import java.util.HashMap;



/**
 * This class defines a template that contains the information used to generate
 * LDAP entries.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPEntryTemplate
       implements Cloneable
{
  // The names of the attributes in this template.  Note that because there can
  // be multiple values for a single attribute, there can be multiple duplicate
  // attribute names.
  private ArrayList<String> attrNames;

  // The separators between the attribute names and values.  This will be
  // either ": " or ":: ".  The order of the elements in this list corresponds
  // to the order of the attribute names and values.
  private ArrayList<String> attrSeparators;

  // The values for the attributes in this template.  The order of the values
  // corresponds to the order of the names and separators.
  private ArrayList<String> attrValues;

  // The map containing the sequential value counters.
  private HashMap<String,SequentialValueCounter> counterHash;

  // The name of the parent template.  This name is case-insensitive
  protected String parentTemplateName;

  // The name of the attribute that is to be used as the RDN component for
  // entries of this type.  Multivalued RDNs are not supported.
  protected String rdnAttribute;

  // The name of this template.  This name is case-insensitive.
  private String templateName;

  // The array that will contain all the attribute information once the
  // template entry has been finalized.
  private String[][] attrComponents;



  /**
   * Creates a new template with the specified name and RDN attribute.
   *
   * @param  templateName  The name of this template.
   * @param  rdnAttribute  The name of the attribute that will be used for the
   *                       RDN component of entries created from this template.
   */
  public LDAPEntryTemplate(String templateName, String rdnAttribute)
  {
    this(templateName, rdnAttribute, null);
  }



  /**
   * Creates a new template with the specified name and RDN attribute that
   * extends the given parent template.
   *
   * @param  templateName        The name of this template.
   * @param  rdnAttribute        The name of the attribute that will be used for
   *                             the RDN component of entries created from this
   *                             template.
   * @param  parentTemplateName  The name of the template that this template
   *                             should extend.
   */
  public LDAPEntryTemplate(String templateName, String rdnAttribute,
                           String parentTemplateName)
  {
    this.templateName       = templateName;
    this.rdnAttribute       = rdnAttribute;
    this.parentTemplateName = parentTemplateName;

    counterHash    = new HashMap<String,SequentialValueCounter>();
    attrComponents = new String[0][0];
    attrNames      = new ArrayList<String>();
    attrSeparators = new ArrayList<String>();
    attrValues     = new ArrayList<String>();
  }



  /**
   * Retrieves the name of this template.
   *
   * @return  The name of this template.
   */
  public String getName()
  {
    return templateName;
  }



  /**
   * Retrieves the value for this template.
   *
   * @return  The value for this template.
   */
  public String getRDNAttribute()
  {
    return rdnAttribute;
  }



  /**
   * Retrieves the name of the parent template for this template.
   *
   * @return  The name of the parent template for this template.
   */
  public String getParentTemplateName()
  {
    return parentTemplateName;
  }



  /**
   * Adds a new attribute definition to this template.
   *
   * @param  name       The name of the attribute.
   * @param  separator  The separator that should appear between the attribute
   *                    name and value.
   * @param  value      The value (or template to use for creating the value).
   */
  public void addAttribute(String name, String separator, String value)
  {
    attrNames.add(name);
    attrSeparators.add(separator);
    attrValues.add(value);
  }



  /**
   * Converts the information read into lists into a two-dimensional array for
   * more efficient processing.  Note that the attribute name is stored twice
   * (once in the user-specified case and once in lowercase) for faster
   * processing later.
   */
  public void completeInitialization()
  {
    attrComponents = new String[attrNames.size()][5];
    for (int i=0; i < attrComponents.length; i++)
    {
      attrComponents[i][0] = attrNames.get(i);
      attrComponents[i][1] = attrSeparators.get(i);
      attrComponents[i][2] = attrValues.get(i);
      attrComponents[i][3] = attrComponents[i][0].toLowerCase();
    }
  }



  /**
   * Retrieves the attribute components of this template in the form of a
   * two-dimensional array.
   *
   * @return  The attribute components of this template in the form of a
   *          two-dimensional array.
   */
  public String[][] getAttributeComponents()
  {
    return attrComponents;
  }



  /**
   * Creates a clone of this template.
   *
   * @return  The clone of this template.
   */
  @Override()
  public LDAPEntryTemplate clone()
  {
    LDAPEntryTemplate t = new LDAPEntryTemplate(templateName, rdnAttribute,
                                                parentTemplateName);

    t.attrNames = new ArrayList<String>(attrNames);
    t.attrSeparators = new ArrayList<String>(attrSeparators);
    t.attrComponents = getAttributeComponents();

    return t;
  }



  /**
   * Retrieves the next value that should be used for the specified sequential
   * value counter.  If there is no value counter defined for the specified
   * attribute, then a value of <CODE>Integer.MIN_VALUE</CODE> will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        next counter value.
   *
   * @return  The next counter value, or <CODE>Integer.MIN_VALUE</CODE> if the
   *          specified attribute does not have a counter associated with it.
   */
  public int getCounterValue(String attributeName)
  {
    SequentialValueCounter counter = counterHash.get(attributeName);
    if (counter == null)
    {
      return Integer.MIN_VALUE;
    }
    else
    {
      return counter.getNext();
    }
  }



  /**
   * Adds the specified sequential value counter for use with this template.
   *
   * @param  attributeName  The name of the attribute for which this counter is
   *                        to be used.
   * @param  counter        The counter to associated with the attribute.
   */
  public void addCounter(String attributeName, SequentialValueCounter counter)
  {
    counterHash.put(attributeName, counter);
  }
}

