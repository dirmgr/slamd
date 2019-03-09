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
import java.util.TreeMap;



/**
 * This class defines a data structure that holds information about a given
 * attribute that may be associated with an LDIF entry type.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFAttributeInfo
{
  /**
   * The maximum number of characters that will be allowed for a unique value.
   */
  public static final int MAX_UNIQUE_VALUE_LENGTH = 25;



  /**
   * The maximum number of unique values that should be maintained.
   */
  public static final int MAX_UNIQUE_VALUES = 100;



  // The number of times that at least one value for this attribute was present
  // in an entry of the associated type.
  private int numEntries;

  // The number of unique values for this attribute across all entries.
  private int numUniqueValues;

  // The total number of values for this attribute across all entries.
  private int numValues;

  // The total number of characters across all values in all entries.
  private long numCharacters;

  // The name of the attribute for this attribute info structure.
  private String attributeName;

  // A mapping between the individual values for this attribute and the number
  // of occurrences for each.
  private TreeMap<String,Integer> valueCounts;



  /**
   * Creates a new LDIF attribute info structure based on the information in
   * the provided attribute.
   *
   * @param  attribute  The LDIF attribute to use to create this attribute info
   *                    structure.
   */
  public LDIFAttributeInfo(LDIFAttribute attribute)
  {
    attributeName   = attribute.getLowerName();
    valueCounts     = new TreeMap<String,Integer>();
    numEntries      = 1;
    numValues       = 0;
    numUniqueValues = 0;
    numCharacters   = 0;

    Iterator iterator = attribute.getValues().iterator();
    while (iterator.hasNext())
    {
      String s = (String) iterator.next();
      String lowerValue = LDIFReader.toLowerCase(s);

      numValues++;
      numCharacters += s.length();

      if (s.length() > MAX_UNIQUE_VALUE_LENGTH)
      {
        numUniqueValues = -1;
        continue;
      }

      if (numUniqueValues >= 0)
      {
        Integer count = valueCounts.get(lowerValue);
        if (count == null)
        {
          numUniqueValues++;
          valueCounts.put(lowerValue, 1);
        }
        else
        {
          valueCounts.put(lowerValue, (count + 1));
        }
      }
    }
  }



  /**
   * Creates a new LDIF attribute info structure with the provided information.
   *
   * @param  attributeName    The name of the attribute.
   * @param  numEntries       The number of entries in which this attribute
   *                          exists.
   * @param  numValues        The total number of values for this attribute
   *                          across all entries.
   * @param  numCharacters    The total number of characters across all values
   *                          in all entries.
   * @param  numUniqueValues  The number of unique values for this attribute.
   * @param  valueCounts      The map correlating the unique values for this
   *                          attribute with the number of occurrences for each.
   */
  LDIFAttributeInfo(String attributeName, int numEntries, int numValues,
                    long numCharacters, int numUniqueValues,
                    TreeMap<String,Integer> valueCounts)
  {
    this.attributeName   = attributeName;
    this.numEntries      = numEntries;
    this.numValues       = numValues;
    this.numCharacters   = numCharacters;
    this.numUniqueValues = numUniqueValues;
    this.valueCounts     = valueCounts;
  }



  /**
   * Updates this attribute info structure with information from the provided
   * attribute.
   *
   * @param  attribute  The attribute to use to create this attribute info
   *                    structure.
   */
  public void update(LDIFAttribute attribute)
  {
    numEntries++;

    Iterator iterator = attribute.getValues().iterator();
    while (iterator.hasNext())
    {
      String s = (String) iterator.next();
      if (s.length() > MAX_UNIQUE_VALUE_LENGTH)
      {
        numUniqueValues = -1;
      }

      if (numUniqueValues >= 0)
      {
        String lowerValue = LDIFReader.toLowerCase(s);
        Integer count = valueCounts.get(lowerValue);
        if (count == null)
        {
          numUniqueValues++;
          if (numUniqueValues > MAX_UNIQUE_VALUES)
          {
            numUniqueValues = -1;
          }
          else
          {
            valueCounts.put(lowerValue, 1);
          }
        }
        else
        {
          valueCounts.put(lowerValue, (count + 1));
        }
      }

      numValues++;
      numCharacters += s.length();
    }
  }



  /**
   * Retrieves the name of the attribute with which this attribute info
   * structure is associated.
   *
   * @return  The name of the attribute with which this attribute info structure
   *          is associated.
   */
  public String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Retrieves the number of entries processed that have at least one value for
   * this attribute.
   *
   * @return  The number of entries processed that have at least one value for
   *          this attribute.
   */
  public int getNumEntries()
  {
    return numEntries;
  }



  /**
   * Retrieves the total number of values for the specified attribute across all
   * entries processed.
   *
   * @return  The total number of values for the specified attribute across all
   *          entries processed.
   */
  public int getNumValues()
  {
    return numValues;
  }



  /**
   * Retrieves the average number of values for this attribute in entries that
   * contain at least one value (i.e., it does not take into account entries
   * that do not have any values for this attribute).
   *
   * @return  The average number of values for this attribute in entries that
   *          contain at least one value.
   */
  public double getAverageValuesPerEntry()
  {
    return (1.0 * numValues / numEntries);
  }



  /**
   * Retrieves the total number of characters contained in all values for the
   * associated attribute.
   *
   * @return  The total number of characters contained in all values for the
   *          associated attribute.
   */
  public long getNumCharacters()
  {
    return numCharacters;
  }



  /**
   * Retrieves the average number of characters contained in each value for the
   * associated attribute.
   *
   * @return  The average number of characters contained in each value for the
   *          associated attribute.
   */
  public double getAverageCharactersPerValue()
  {
    return (1.0 * numCharacters / numValues);
  }



  /**
   * Retrieves the number of unique values for this attribute.
   *
   * @return  The number of unique values for this attribute, or -1 if the
   *          maximum number of allowed unique values has been exceeded.
   */
  public double getNumUniqueValues()
  {
    return numUniqueValues;
  }



  /**
   * Retrieves a map correlating the unique values for this attribute and the
   * number of entries in which each of those values appear.  The contents of
   * the returned map must not be considered reliable if
   * <CODE>getNumUniqueValues</CODE> returns a negative value.
   *
   * @return  A map correlating the unique values for this attribute and the
   *          number of entries in which each of those values appear.
   */
  public TreeMap<String,Integer> getUniqueValues()
  {
    return valueCounts;
  }



  /**
   * Creates a copy of this LDIF attribute info structure.
   *
   * @return  A copy of this LDIF attribute info struture.
   */
  public Object clone()
  {
    TreeMap<String,Integer> mapClone = new TreeMap<String,Integer>(valueCounts);
    return new LDIFAttributeInfo(attributeName, numEntries, numValues,
                                 numCharacters, numUniqueValues, mapClone);
  }



  /**
   * Aggregates the information in the provided attribute info structure with
   * this version.
   *
   * @param  attributeInfo  The attribute info structure containing the data to
   *                        aggregate.
   */
  public void aggregate(LDIFAttributeInfo attributeInfo)
  {
    numEntries    += attributeInfo.numEntries;
    numValues     += attributeInfo.numValues;
    numCharacters += attributeInfo.numCharacters;

    if (attributeInfo.numUniqueValues < 0)
    {
      numUniqueValues = -1;
    }

    if (numUniqueValues > 0)
    {
      Iterator iterator = attributeInfo.valueCounts.keySet().iterator();
      while (iterator.hasNext())
      {
        String  s             = (String) iterator.next();
        Integer currentCount  = valueCounts.get(s);
        Integer providedCount = attributeInfo.valueCounts.get(s);

        if (currentCount != null)
        {
          valueCounts.put(s, (currentCount + providedCount));
        }
        else
        {
          numUniqueValues++;
          if (numUniqueValues > MAX_UNIQUE_VALUES)
          {
            numUniqueValues = -1;
          }
          else
          {
            valueCounts.put(s, providedCount);
          }
        }
      }
    }
  }
}

