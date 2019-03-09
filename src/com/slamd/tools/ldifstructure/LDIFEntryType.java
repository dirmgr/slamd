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



import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;



/**
 * This class defines an LDIF entry type, which is used as a means of
 * categorizing the kinds of entries that may be found in an LDIF file.  The
 * categorization is based on the set of objectclasses contained in an entry.
 * Each set of unique objectclasses will constitute its own entry type.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFEntryType
{
  // The total number of entries of this type that have been processed.
  private int numEntries;

  // The association between the names of the objectclasses and the number of
  // entries that contain them, for aggregate entry types.
  private LinkedHashMap<String,Integer> objectClassCounts;

  // A mapping between the names of the attributes that appear in entries of
  // this type and a set of information about that attribute.
  private TreeMap<String,LDIFAttributeInfo> attributes;

  // The names of the objectclasses for this entry type.
  private String[] objectClasses;



  /**
   * Creates a new shell of an entry type that will be used for aggregating
   * information from multiple entry types.
   */
  LDIFEntryType()
  {
    objectClasses     = new String[] { "Aggregate" };
    numEntries        = 0;
    objectClassCounts = new LinkedHashMap<String,Integer>();
    attributes        = new TreeMap<String,LDIFAttributeInfo>();
  }



  /**
   * Creates a new LDIF entry type structure based on information in the
   * provided entry.
   *
   * @param  entry          The entry to use to create this LDIF entry type
   *                        structure.
   * @param  objectClasses  The set of objectclasses to use for this entry type.
   */
  public LDIFEntryType(LDIFEntry entry, String[] objectClasses)
  {
    this.objectClasses = objectClasses;

    numEntries        = 1;
    objectClassCounts = new LinkedHashMap<String,Integer>();

    attributes = new TreeMap<String,LDIFAttributeInfo>();
    Iterator iterator = entry.getAttributes().values().iterator();
    while (iterator.hasNext())
    {
      LDIFAttribute a = (LDIFAttribute) iterator.next();
      String lowerName = a.getLowerName();
      if (lowerName.equals("objectclass"))
      {
        continue;
      }

      LDIFAttributeInfo attrInfo = attributes.get(lowerName);
      if (attrInfo == null)
      {
        attrInfo = new LDIFAttributeInfo(a);
        attributes.put(lowerName, attrInfo);
      }
      else
      {
        attrInfo.update(a);
      }
    }
  }



  /**
   * Updates the information in this entry type with data from the provided
   * entry.
   *
   * @param  entry  The entry to use to update this entry type structure.
   */
  public void update(LDIFEntry entry)
  {
    numEntries++;

    Iterator iterator = entry.getAttributes().values().iterator();
    while (iterator.hasNext())
    {
      LDIFAttribute a = (LDIFAttribute) iterator.next();
      String lowerName = a.getLowerName();
      if (lowerName.equals("objectclass"))
      {
        continue;
      }

      LDIFAttributeInfo attrInfo = attributes.get(lowerName);
      if (attrInfo == null)
      {
        attrInfo = new LDIFAttributeInfo(a);
        attributes.put(lowerName, attrInfo);
      }
      else
      {
        attrInfo.update(a);
      }
    }
  }



  /**
   * Retrieves the total number of entries of this type that have been
   * processed.
   *
   * @return  The total number of entries of this type that have been processed.
   */
  public int getNumEntries()
  {
    return numEntries;
  }



  /**
   * Retrieves the set of objectclasses that are associated with this entry
   * type.
   *
   * @return  The set of objectclasses that are associated with this entry type.
   */
  public String[] getObjectClasses()
  {
    return objectClasses;
  }



  /**
   * Retrieves the set of attribute information that has been accumulated for
   * the entries of this type that have been processed.
   *
   * @return  The set of attribute information that has been accumulated for the
   *          entries of this type that have been processed.
   */
  public TreeMap<String,LDIFAttributeInfo> getAttributes()
  {
    return attributes;
  }



  /**
   * Aggregates the information from the provided enty type into this entry
   * type.
   *
   * @param  entryType  The entry type with the information to aggregate.
   */
  void aggregate(LDIFEntryType entryType)
  {
    numEntries += entryType.numEntries;

    for(int i=0; i < entryType.objectClasses.length; i++)
    {
      String s = entryType.objectClasses[i];
      Integer count = objectClassCounts.get(s);
      if (count == null)
      {
        objectClassCounts.put(s, entryType.numEntries);
      }
      else
      {
        objectClassCounts.put(s, (entryType.numEntries + count));
      }
    }

    Iterator iterator = entryType.attributes.keySet().iterator();
    while (iterator.hasNext())
    {
      String s = (String) iterator.next();
      LDIFAttributeInfo currentInfo  = attributes.get(s);
      LDIFAttributeInfo providedInfo = entryType.attributes.get(s);

      if (currentInfo == null)
      {
        attributes.put(s, (LDIFAttributeInfo) providedInfo.clone());
      }
      else
      {
        currentInfo.aggregate(providedInfo);
      }
    }
  }



  /**
   * Retrieves the mapping between the names of the objectclasses and the
   * number of entries in which they appear.  This should only be used for
   * aggregate entry types.
   *
   * @return  The mapping between the names of the objectclasses and the number
   *          of entries in which they appear.
   */
  public LinkedHashMap<String,Integer> getAggregateObjectClassCounts()
  {
    return objectClassCounts;
  }
}

