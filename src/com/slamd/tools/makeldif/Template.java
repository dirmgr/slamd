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
import java.util.HashMap;



/**
 * This class defines a template that contains the information used to create
 * entries in the LDIF file.
 *
 *
 * @author   Neil A. Wilson
 */
public class Template
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

  // The number of entries to create for each subtemplate type.
  private ArrayList<Integer> subtemplCounts;

  // The names of subtemplates for this template (templates to be used to create
  // entries below those created with this template)
  private ArrayList<String> subtemplNames;

  // The set of sequential value counters that are maintained for this template.
  private HashMap<String,Counter> counters;

  // The set of custom tags that are maintained for this template.
  private HashMap<String,CustomTag> customTags;

  // The array that will contain all the subtemplate count information once the
  // template entry has been finalized.
  private int[] subtemplateCounts;

  // The name of the parent template.  This name is case-insensitive
  protected String parentTemplateName;

  // The name(s) of the attribute(s) that is to be used as the RDN component for
  // entries of this type, converted to lowercase.
  private String[] lowerRDNAttributes;

  // The name(s) of the attribute(s) that is to be used as the RDN component for
  // entries of this type.
  private String[] rdnAttributes;

  // The name of this template.  This name is case-insensitive.
  private String templateName;

  // The array that will contain all the subtemplate names once the template
  // entry has been finalized.
  private String[] subtemplateNames;

  // The array that will contain all the attribute information once the
  // template entry has been finalized.
  protected String[][] attrComponents;



  /**
   * Creates a new template with the specified name and RDN attribute.
   *
   * @param  templateName   The name of this template.
   * @param  rdnAttributes  The name of the attribute(s) that will be used for
   *                        the RDN component(s) of entries created from this
   *                        template.
   */
  public Template(String templateName, String[] rdnAttributes)
  {
    this(templateName, rdnAttributes, null);
  }



  /**
   * Creates a new template with the specified name and RDN attribute that
   * extends the given parent template.
   *
   * @param  templateName        The name of this template.
   * @param  rdnAttributes       The name of the attribute(s) that will be used
   *                             for the RDN component(s) of entries created
   *                             from this template.
   * @param  parentTemplateName  The name of the template that this template
   *                             should extend.
   */
  public Template(String templateName, String[] rdnAttributes,
                  String parentTemplateName)
  {
    this.templateName       = templateName;
    this.rdnAttributes      = rdnAttributes;
    this.parentTemplateName = parentTemplateName;

    attrComponents = new String[0][0];
    attrNames      = new ArrayList<String>();
    attrSeparators = new ArrayList<String>();
    attrValues     = new ArrayList<String>();
    subtemplNames  = new ArrayList<String>();
    subtemplCounts = new ArrayList<Integer>();
    counters       = new HashMap<String,Counter>();
    customTags     = new HashMap<String,CustomTag>();

    lowerRDNAttributes = new String[rdnAttributes.length];
    for (int i=0; i < rdnAttributes.length; i++)
    {
      lowerRDNAttributes[i] = rdnAttributes[i].toLowerCase();
    }
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
   * Retrieves the names of the RDN attributes for this template.
   *
   * @return  The names of the RDN attributes for this template.
   */
  public String[] getRDNAttributes()
  {
    return rdnAttributes;
  }



  /**
   * Sets the RDN attributes to use for this template.
   *
   * @param  rdnAttributes  The RDN attributes to use for this template.
   */
  public void setRDNAttributes(String[] rdnAttributes)
  {
    this.rdnAttributes = rdnAttributes;

    lowerRDNAttributes = new String[rdnAttributes.length];
    for (int i=0; i < rdnAttributes.length; i++)
    {
      lowerRDNAttributes[i] = rdnAttributes[i].toLowerCase();
    }
  }



  /**
   * Retrieves the names of the RDN attributes for this template in lowercase.
   *
   * @return  The names of the RDN attributes for this template in lowercase.
   */
  public String[] getLowerRDNAttributes()
  {
    return lowerRDNAttributes;
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
   * Adds a subtemplate definition to this template.  A subtemplate definition
   * specifies a type of entry that should be generated below each entry
   * generated using this template.  Multiple subtemplates can be associated
   * with a single template.
   *
   * @param  name   The name of the template to use to generate the specified
   *                attributes.
   * @param  count  The number of entries of the specified type to create below
   *                each entry generated using this template.
   */
  public void addSubtemplate(String name, int count)
  {
    subtemplNames.add(name);
    subtemplCounts.add(count);
  }



  /**
   * Converts the information read into lists into a two-dimensional array for
   * more efficient processing.  Note that the attribute name is stored twice
   * (once in the user-specified case and once in lowercase) for faster
   * processing later.
   */
  public void completeInitialization()
  {
    attrComponents = new String[attrNames.size()][4];
    for (int i=0; i < attrComponents.length; i++)
    {
      attrComponents[i][0] = attrNames.get(i);
      attrComponents[i][1] = attrSeparators.get(i);
      attrComponents[i][2] = attrValues.get(i);
      attrComponents[i][3] = attrComponents[i][0].toLowerCase();
    }

    subtemplateNames = new String[subtemplNames.size()];
    subtemplateCounts = new int[subtemplateNames.length];
    for (int i=0; i < subtemplateNames.length; i++)
    {
      subtemplateNames[i]  = subtemplNames.get(i);
      subtemplateCounts[i] = subtemplCounts.get(i);
    }
  }



  /**
   * Clears the counter hashtable, which will cause all counters for this
   * template to be reset the next time it is used.
   */
  public void resetCounters()
  {
    counters.clear();
  }



  /**
   * Reinitializes all of the custom tags for this template.
   */
  public void reinitializeCustomTags()
  {
    for (CustomTag tag : customTags.values())
    {
      tag.reinitialize();
    }
  }



  /**
   * Adds the provided sequential value counter to this template.
   *
   * @param  counterName  The name to use for the counter.
   * @param  counter      The sequential value counter to add to this template.
   */
  public void addCounter(String counterName, Counter counter)
  {
    counters.put(counterName, counter);
  }



  /**
   * Retrieves the sequential value counter with the specified name from this
   * template.
   *
   * @param  counterName  The name of the counter to retrieve.
   *
   * @return  The counter with the specified name, or <CODE>null</CODE> if that
   *          counter has not yet been created for this template.
   */
  public Counter getCounter(String counterName)
  {
    return counters.get(counterName);
  }



  /**
   * Adds the provided custom tag to this template.
   *
   * @param  tagName    The name to use for the tag.
   * @param  customTag  The custom tag to add to this template.
   */
  public void addCustomTag(String tagName, CustomTag customTag)
  {
    customTags.put(tagName, customTag);
  }



  /**
   * Retrieves the custom tag with the specified name from this template.
   *
   * @param  tagName  The name of the custom tag to retrieve.
   *
   * @return  The custom tag with the specified name, or <CODE>null</CODE> if
   *          that tag has not been created for this template.
   */
  public CustomTag getCustomTag(String tagName)
  {
    return customTags.get(tagName);
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
   * Indicates whether this template entry has subtemplate definitions and
   * therefore should have entries created beneath it.
   *
   * @return  <CODE>true</CODE> if this template does have subordinate entries,
   *          or <CODE>false</CODE> if not.
   */
  public boolean hasSubordinates()
  {
    return (subtemplateNames.length > 0);
  }



  /**
   * Retrieves the names of the templates that should be used to create entries
   * below entries created using this template.
   *
   * @return  The names of the templates that should be used to create entries
   *          below entries created using this template.
   */
  public String[] getSubtemplateNames()
  {
    return subtemplateNames;
  }



  /**
   * Retrieves the number of entries of each subtemplate type that should be
   * created below entries created using this template.  Note that the order of
   * elements in this array corresponds to the order of elements retrieved from
   * <CODE>getSubtemplateNames</CODE>.
   *
   * @return  The number of entries of each subtemplate type that should be
   *          created below entries created using this template.
   */
  public int[] getSubtemplateCounts()
  {
    return subtemplateCounts;
  }



  /**
   * Creates a clone of this template.
   *
   * @return  The clone of this template.
   */
  public Object clone()
  {
    Template t = new Template(templateName, rdnAttributes, parentTemplateName);

    t.subtemplateCounts = new int[subtemplateCounts.length];
    System.arraycopy(subtemplateCounts, 0, t.subtemplateCounts, 0,
                     subtemplateCounts.length);

    t.subtemplateNames = new String[subtemplateNames.length];
    System.arraycopy(subtemplateNames, 0, t.subtemplateNames, 0,
                     subtemplateNames.length);

    t.attrNames      = new ArrayList<String>(attrNames);
    t.attrSeparators = new ArrayList<String>(attrSeparators);
    t.subtemplCounts = new ArrayList<Integer>(subtemplCounts);
    t.subtemplNames  = new ArrayList<String>(subtemplNames);

    t.attrComponents = getAttributeComponents();

    return t;
  }
}

