/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2019.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.stat;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 * This class provides a data structure that holds information about the results
 * for a specific category within a categorical tracker.  It can be used to help
 * sort the results of a categorical tracker by category.
 */
final class CategoricalTrackerCategoryData
      implements Comparable<CategoricalTrackerCategoryData>
{
  // The total number of occurrences for this category.
  private final int totalCount;

  // The name of the associated category.
  private final String categoryName;

  // The list of occurrences for this category broken up by interval.
  private final List<Integer> countPerInterval;



  /**
   * Creates a new categorical tracker category data object with the provided
   * information.
   *
   * @param  categoryName      The name of this category.
   * @param  totalCount        The total number of occurrences of this category.
   * @param  countPerInterval  The number of occurrences of this category broken
   *                           up by interval.
   */
  CategoricalTrackerCategoryData(final String categoryName,
                                 final int totalCount,
                                 final List<Integer> countPerInterval)
  {
    this.categoryName = categoryName;
    this.totalCount = totalCount;
    this.countPerInterval =
         Collections.unmodifiableList(new ArrayList<>(countPerInterval));
  }



  /**
   * Retrieves the name for this category.
   *
   * @return  The name for this category.
   */
  String getCategoryName()
  {
    return categoryName;
  }



  /**
   * Retrieves the total count for this category.
   *
   * @return  The total count for this category.
   */
  int getTotalCount()
  {
    return totalCount;
  }



  /**
   * Retrieves the number of occurrences for this category in each interval.
   *
   * @return  The number of occurrences for this category in each interval.
   */
  List<Integer> getCountPerInterval()
  {
    return countPerInterval;
  }



  /**
   * Retrieves an integer value that indicates the relative order of this
   * category data to the provided data in a sorted list.
   *
   * @param  d  The category data to compare against this category data.
   *
   * @return  A positive integer if this category data should be ordered after
   *          the provided data in a sorted list, a negative integer if this
   *          category data should be ordered before the provided data in a
   *          sorted list, or zero if the order does not matter.
   */
  @Override()
  public int compareTo(final CategoricalTrackerCategoryData d)
  {
    // Sort based on the total count first, with bigger counts ordered before
    // smaller counts.
    if (totalCount > d.totalCount)
    {
      return -1;
    }
    else if (totalCount < d.totalCount)
    {
      return 1;
    }

    // If the counts are the same, then sort lexicographically by category name.
    return categoryName.compareTo(d.categoryName);
  }



  /**
   * Retrieves a string representation of this category data object.
   *
   * @return  A string representation of this category data object.
   */
  public String toString()
  {
    return "CategoryData(name='" + categoryName + ", totalCount=" +
         totalCount + ')';
  }
}
