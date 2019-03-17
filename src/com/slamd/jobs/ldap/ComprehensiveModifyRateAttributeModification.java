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
package com.slamd.jobs.ldap;



import java.text.ParseException;
import java.util.HashSet;

import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ValuePattern;

import com.slamd.parameter.InvalidValueException;



/**
 * This class defines a data structure that holds information about an attribute
 * to modify in the {@link ComprehensiveModifyRateJob} job.
 */
final class ComprehensiveModifyRateAttributeModification
{
  // The number of values to generate for the target attribute.
  private final int numValues;

  // The name of the attribute to be modified.
  private final String attributeType;

  // The value pattern to use to generate values for the attribute.
  private final ValuePattern valuePattern;



  /**
   * Creates a new comprehensive modify rate attribute modification with the
   * provided information.
   *
   * @param  attributeType  The name of the attribute to be modified.
   * @param  numValues      The number of values to generate for the target
   *                        attribute.
   * @param valuePattern    The value pattern to use to generate values for the
   *                        attribute.
   */
  ComprehensiveModifyRateAttributeModification(final String attributeType,
                                               final int numValues,
                                               final ValuePattern valuePattern)
  {
    this.attributeType = attributeType;
    this.numValues = numValues;
    this.valuePattern = valuePattern;
  }



  /**
   * Parses the provided "attribute to modify" string as a comprehensive modify
   * rate attribute modification value.
   *
   * @param  s  The string to be parsed.
   *
   * @return  The comprehensive modify rate attribute modification value that
   *          was parsed.
   *
   * @throws  InvalidValueException  If the provided string cannot be parsed as
   *                                 a valid comprehensive modify rate attribute
   *                                 modification definition.
   */
  static ComprehensiveModifyRateAttributeModification parseAttributeToModify(
                                                           final String s)
         throws InvalidValueException
  {
    // If the value doesn't have any colons, then it must just be the attribute
    // name.  Use the default number of values and valeu pattern.
    final int firstColonPos = s.indexOf(':');
    if (firstColonPos < 0)
    {
      try
      {
        return new ComprehensiveModifyRateAttributeModification(s, 1,
             new ValuePattern("[random:80:abcdefghijklmnopqrstuvwxyz]"));
      }
      catch (final Exception e)
      {
        throw new InvalidValueException(
             "An unexpected error occurred while trying to parse attribute " +
                  "to modify value '" + s + "':  " +
                  StaticUtils.getExceptionMessage(e),
             e);
      }
    }


    // If the value has one colon, then it must have at least two.  The first
    // colon separates the attribute name from the number of values.  The second
    // separates the number of values from the value pattern.
    final String attributeType = s.substring(0, firstColonPos);

    final int secondColonPos = s.indexOf(':', firstColonPos + 1);
    if (secondColonPos < 0)
    {
      throw new InvalidValueException("Attribute to modify value '" + s +
           "' only includes a single colon.  If the value contains any " +
           "colons, then it must contain at least two (where the first is " +
           "used to specify the attribute name from the number of values to " +
           "generate for that attribute, and the second separates the number " +
           "of values from the value pattern to use to generate those " +
           "values).");
    }
    final int numValues;
    final String numValuesStr = s.substring(firstColonPos+1, secondColonPos);
    try
    {
      numValues =
           Integer.parseInt(s.substring(firstColonPos+1, secondColonPos));
    }
    catch (final Exception e)
    {
      throw new InvalidValueException(
           "Unable to parse attribute to modify value '" + s +
                "' because string '" + numValuesStr + "' between the first " +
                "two colons could not be parsed as an integer.",
           e);
    }

    if (numValues < 1)
    {
      throw new InvalidValueException("Attribute to modify value '" + s +
           "' specifies an number of values to generate for attribute '" +
           attributeType + "':  " + numValues + ".  The value must be " +
           "greater than or equal to 1.");
    }

    final String valuePatternStr = s.substring(secondColonPos+1);
    if (valuePatternStr.isEmpty())
    {
      throw new InvalidValueException("Attribute to modify value '" + s +
           "' ends with the second colon, which means that the value pattern " +
           "is empty.  The value pattern must not be empty.");
    }

    final ValuePattern valuePattern;
    try
    {
      valuePattern = new ValuePattern(valuePatternStr);
    }
    catch (final ParseException e)
    {
      throw new InvalidValueException(
           "Unable to parse attribute to modify value '" + s +
                "' because string '" + valuePatternStr + "' after the second " +
                "colon could not be parsed as a valid value pattern:  " +
                e.getMessage(),
           e);
    }

    return new ComprehensiveModifyRateAttributeModification(attributeType,
         numValues, valuePattern);
  }



  /**
   * Retrieves the name of the attribute type to be modified.
   *
   * @return  The name of the attribute type to be modified.
   */
  String getAttributeType()
  {
    return attributeType;
  }



  /**
   * Retrieves the number of values to modify for the attribute.
   *
   * @return  The number of values to generate for the attribute.
   */
  int getNumValues()
  {
    return numValues;
  }



  /**
   * Retrieves the value pattern to use to generate values for the attribute.
   *
   * @return  The value pattern to use to generate values for the attribute.
   */
  ValuePattern getValuePattern()
  {
    return valuePattern;
  }



  /**
   * Generates a modification for the target attribute.
   *
   * @return  The modification that was generated.
   */
  Modification generateModification()
  {
    final HashSet<String> values =
         new HashSet<>(StaticUtils.computeMapCapacity(numValues));
    for (int i=0; i < numValues; i++)
    {
      values.add(valuePattern.nextValue());
    }

    return new Modification(ModificationType.REPLACE, attributeType,
         values.toArray(StaticUtils.NO_STRINGS));
  }
}
