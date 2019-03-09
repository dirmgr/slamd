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



import java.util.HashMap;
import java.util.TreeSet;



/**
 * This class maintains up to four lists of string values (one each for the
 * equality, subInitial, subAny, and subFinal values).  The values will be
 * sorted automatically by the underlying implementation.
 *
 *
 * @author   Neil A. Wilson
 */
public class UniqueSortedList
{
  // Indicates whether an equality list will be maintained.
  private boolean maintainEqualityList;

  // Indicates whether the list will need to keep track of the number of
  // entries that match a given filter.
  private boolean maintainMatchCount;

  // Indicates whether a subAny list will be maintained.
  private boolean maintainSubAnyList;

  // Indicates whether a subInitial list will be maintained.
  private boolean maintainSubInitialList;

  // Indicates whether a subFinal list will be maintained.
  private boolean maintainSubFinalList;

  // Indicates whether all substring types will be maintained.
  private boolean maintainSubstringList;

  // The hash map used to count the number of entries matching each equality
  // filter.
  private HashMap<String,Integer> eqMatchCount;

  // The hash map used to count the number of entries matching each subAny
  // filter.
  private HashMap<String,Integer> subAnyMatchCount;

  // The hash map used to count the number of entries matching each subInitial
  // filter.
  private HashMap<String,Integer> subInitialMatchCount;

  // The hash map used to count the number of entries matching each subFinal
  // filter.
  private HashMap<String,Integer> subFinalMatchCount;

  // The maximum number of matches required before a filter will be included in
  // the filter list.
  private int maxMatches;

  // The minimum number of matches required before a filter will be included in
  // the filter list.
  private int minMatches;

  // The number of characters to include in substring filters.
  private int substringLength;

  // The set that will be used to hold the equality values.
  private TreeSet<String> eqValues;

  // The set that will be used to hold the subAny values.
  private TreeSet<String> subAnyValues;

  // The set that will be used to hold the subInitial values.
  private TreeSet<String> subInitialValues;

  // The set that will be used to hold the subFinal values.
  private TreeSet<String> subFinalValues;



  /**
   * Creates a new unique sorted list with no elements.
   *
   * @param  maintainEqualityList    Indicates whether the equality list will be
   *                                 maintained.
   * @param  maintainSubstringList   Indicates whether the all substring types
   *                                 will be maintained.
   * @param  maintainSubInitialList  Indicates whether the subInitial list will
   *                                 be maintained.
   * @param  maintainSubAnyList      Indicates whether the subAny list will be
   *                                 maintained.
   * @param  maintainSubFinalList    Indicates whether the subFinal list will be
   *                                 maintained.
   */
  public UniqueSortedList(boolean maintainEqualityList,
                          boolean maintainSubstringList,
                          boolean maintainSubInitialList,
                          boolean maintainSubAnyList,
                          boolean maintainSubFinalList)
  {
    this.maintainEqualityList   = maintainEqualityList;
    this.maintainSubstringList  = maintainSubstringList;
    this.maintainSubInitialList = maintainSubInitialList;
    this.maintainSubAnyList     = maintainSubAnyList;
    this.maintainSubFinalList   = maintainSubFinalList;
    this.eqValues               = new TreeSet<String>();
    this.subInitialValues       = new TreeSet<String>();
    this.subAnyValues           = new TreeSet<String>();
    this.subFinalValues         = new TreeSet<String>();
  }



  /**
   * Specifies the criteria to use when determining how to create the filters.
   *
   * @param  substringLength         The number of characters to include in
   *                                 substring filters.
   * @param  minMatches              The minimum number of entries that will
   *                                 need to match a filter before it should be
   *                                 output.
   * @param  maxMatches              The maximum number of entries that will be
   *                                 allowed to match a filter for it to be
   *                                 output.
   */
  public void setMatchCriteria(int substringLength, int minMatches,
                               int maxMatches)
  {
    if (substringLength < 1)
    {
      substringLength = 3;
    }
    this.substringLength = substringLength;

    if (minMatches < 1)
    {
      minMatches = 1;
    }
    this.minMatches = minMatches;

    if (maxMatches <= minMatches)
    {
      maxMatches = Integer.MAX_VALUE;
    }
    this.maxMatches = maxMatches;


    if ((minMatches > 1) || (maxMatches > 0))
    {
      maintainMatchCount   = true;
      eqMatchCount         = new HashMap<String,Integer>();
      subAnyMatchCount     = new HashMap<String,Integer>();
      subInitialMatchCount = new HashMap<String,Integer>();
      subFinalMatchCount   = new HashMap<String,Integer>();
    }
  }



  /**
   * Adds the specified string value to this list if it is not already present.
   *
   * @param  stringValue  The string value to be added to this list.
   */
  public void addString(String stringValue)
  {
    if (maintainEqualityList)
    {
      if (maintainMatchCount)
      {
        Integer currentCount = eqMatchCount.get(stringValue);
        int count;
        if (currentCount == null)
        {
          count = 1;
        }
        else
        {
          count = currentCount + 1;
        }
        eqMatchCount.put(stringValue, count);
        if ((count >= minMatches) && (count <= maxMatches))
        {
          eqValues.add(stringValue);
        }
        else
        {
          eqValues.remove(stringValue);
        }
      }
      else
      {
        eqValues.add(stringValue);
      }
    }


    if (stringValue.length() >= substringLength)
    {
      if (maintainSubInitialList)
      {
        if (maintainMatchCount)
        {
          String subInitialValue = stringValue.substring(0, substringLength);
          Integer currentCount = subInitialMatchCount.get(subInitialValue);
          int count;
          if (currentCount == null)
          {
            count = 1;
          }
          else
          {
            count = currentCount + 1;
          }
          subInitialMatchCount.put(subInitialValue, count);
          if ((count >= minMatches) && (count <= maxMatches))
          {
            subInitialValues.add(subInitialValue);
          }
          else
          {
            subInitialValues.remove(subInitialValue);
          }
        }
        else
        {
          subInitialValues.add(stringValue.substring(0, substringLength));
        }
      }

      if (maintainSubFinalList)
      {
        if (maintainMatchCount)
        {
          String subFinalValue = stringValue.substring(stringValue.length() -
                                                       substringLength,
                                                       stringValue.length());
          Integer currentCount = subFinalMatchCount.get(subFinalValue);
          int count;
          if (currentCount == null)
          {
            count = 1;
          }
          else
          {
            count = currentCount + 1;
          }
          subFinalMatchCount.put(subFinalValue, count);
          if ((count >= minMatches) && (count <= maxMatches))
          {
            subFinalValues.add(subFinalValue);
          }
          else
          {
            subFinalValues.remove(subFinalValue);
          }
        }
        else
        {
          subFinalValues.add(stringValue.substring(stringValue.length() -
                                                   substringLength,
                                                   stringValue.length()));
        }
      }

      if (maintainSubAnyList)
      {
        int endPos = stringValue.length() - substringLength;
        for (int j=0,k=substringLength; j <= endPos; j++,k++)
        {
          if (maintainMatchCount)
          {
            String subString = stringValue.substring(j, k);
            Integer currentCount = subAnyMatchCount.get(subString);
            int count;
            if (currentCount == null)
            {
              count = 1;
            }
            else
            {
              count = currentCount + 1;
            }
            subAnyMatchCount.put(subString, count);
            if ((count >= minMatches) && (count <= maxMatches))
            {
              subAnyValues.add(subString);
            }
            else
            {
              subAnyValues.remove(subString);
            }
          }
          else
          {
            subAnyValues.add(stringValue.substring(j, k));
          }
        }
      }
    }
  }



  /**
   * Indicates whether the equality list is maintained.
   *
   * @return  <CODE>true</CODE> if the equality list is maintained, or
   *          <CODE>false</CODE> if not.
   */
  public boolean maintainEqualityList()
  {
    return maintainEqualityList;
  }



  /**
   * Indicates whether the lists for all substring types are maintained.
   *
   * @return  <CODE>true</CODE> if the lists for all substring types are
   *          maintained, or <CODE>false</CODE> if not.
   */
  public boolean maintainSubstringList()
  {
    return maintainSubstringList;
  }



  /**
   * Indicates whether the subInitial list is maintained.
   *
   * @return  <CODE>true</CODE> if the subInitial list is maintained, or
   *          <CODE>false</CODE> if not.
   */
  public boolean maintainSubInitialList()
  {
    return maintainSubInitialList;
  }



  /**
   * Indicates whether the subAny list is maintained.
   *
   * @return  <CODE>true</CODE> if the subAny list is maintained, or
   *          <CODE>false</CODE> if not.
   */
  public boolean maintainSubAnyList()
  {
    return maintainSubAnyList;
  }



  /**
   * Indicates whether the subFinal list is maintained.
   *
   * @return  <CODE>true</CODE> if the subFinal list is maintained, or
   *          <CODE>false</CODE> if not.
   */
  public boolean maintainSubFinalList()
  {
    return maintainSubFinalList;
  }



  /**
   * Retrieves the string values contained in the equality list.
   *
   * @return  The string values contained in the equality list.
   */
  public String[] getEqualityValues()
  {
    String[] values = new String[eqValues.size()];
    eqValues.toArray(values);
    return values;
  }



  /**
   * Retrieves the string values contained in the subInitial list.
   *
   * @return  The string values contained in the subInitial list.
   */
  public String[] getSubInitialValues()
  {
    String[] values = new String[subInitialValues.size()];
    subInitialValues.toArray(values);
    return values;
  }



  /**
   * Retrieves the string values contained in the subAny list.
   *
   * @return  The string values contained in the subAny list.
   */
  public String[] getSubAnyValues()
  {
    String[] values = new String[subAnyValues.size()];
    subAnyValues.toArray(values);
    return values;
  }



  /**
   * Retrieves the string values contained in the subFinal list.
   *
   * @return  The string values contained in the subFinal list.
   */
  public String[] getSubFinalValues()
  {
    String[] values = new String[subFinalValues.size()];
    subFinalValues.toArray(values);
    return values;
  }
}

