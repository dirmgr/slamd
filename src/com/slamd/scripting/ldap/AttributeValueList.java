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
import java.util.Random;



/**
 * This class defines a list of explicit values that an attribute may have.  The
 * values may be weighted so that some are more likely to occur than others.
 * Values with a larger weight are more likely to occur than values with a
 * smaller weight.
 *
 *
 * @author   Neil A. Wilson
 */
public class AttributeValueList
{
  // The set of values that have been defined before the call to
  // completeInitialization
  private ArrayList<String> valueList;

  // The set of weights that have been defined before the call to
  // completeInitialization
  private ArrayList<Integer> weightList;

  // The total of all the value weights.
  private int weightTotal;

  // The weights associated with each value (the order of the weights matches
  // the order of the values).
  private int[] weights;

  // A random number generator used to choose the values.
  private Random random;

  // The set of values that may be used.
  private String[] values;



  /**
   * Creates a new value list with no values.
   */
  public AttributeValueList()
  {
    random      = new Random();
    valueList   = new ArrayList<String>();
    weightList  = new ArrayList<Integer>();
    weightTotal = 0;
  }



  /**
   * Adds the specified value to the value list.  It will be given a weight of
   * 1.
   *
   * @param  value  The value to add to the value list.
   */
  public void addValue(String value)
  {
    addValue(value, 1);
  }



  /**
   * Adds the specified value to the value list with the indicated weight.
   *
   * @param  value   The value to add to the value list.
   * @param  weight  The weight to use for the value.
   */
  public void addValue(String value, int weight)
  {
    valueList.add(value);
    weightList.add(weight);
    weightTotal += weight;
  }



  /**
   * Indicates that all of the values have been added to the value list and
   * that the value list should be optimized for better performance when
   * retrieving values from it.
   */
  public void completeInitialization()
  {
    weights = new int[weightList.size()];
    values  = new String[weights.length];

    int accumulatedWeight = 0;
    for (int i=0; i < weights.length; i++)
    {
      accumulatedWeight += weightList.get(i);
      values[i] = valueList.get(i);
      weights[i] = accumulatedWeight;
    }
  }



  /**
   * Retrieves a value from the value list.  The value to retrieve will be
   * chosen at random, but will be somewhat based on the weights associated with
   * each value.
   *
   * @return  A value from the value list.
   */
  public String nextValue()
  {
    int val = Math.abs(random.nextInt()) % weightTotal;
    for (int i=0; i < weights.length; i++)
    {
      if (val < weights[i])
      {
        return values[i];
      }
    }

    // This should never occur, but if it does, then return a valid value
    return values[0];
  }
}
