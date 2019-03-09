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
package com.slamd.jobs;



import java.util.Random;
import java.util.UUID;

import com.slamd.common.SLAMDException;
import com.slamd.parameter.InvalidValueException;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.util.Base64;



/**
 * This class provides a utility that can generate entries from a template.
 *
 *
 * @author   Neil A. Wilson
 */
public class TemplateBasedEntryGenerator
{
  /**
   * The set of characters that should be included in numeric values.
   */
  public static final char[] NUMERIC_CHARS = "0123456789".toCharArray();



  /**
   * The set of characters that should be included in alphabetic values.
   */
  public static final char[] ALPHA_CHARS =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  /**
   * The set of characters that should be included in alphanumeric values.
   */
  public static final char[] ALPHANUMERIC_CHARS =
       "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();



  /**
   * The set of characters that should be included in hexadecimal values.
   */
  public static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();



  /**
   * The set of characters that should be included in base64 values.
   */
  public static final char[] BASE64_CHARS =
       ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
        "0123456789+/").toCharArray();



  /**
   * The set of months that will be used if the name of a month is required.
   */
  public static final String[] MONTH_NAMES =
  {
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
  };



  // The entry number for the first entry to be generated.
  private final int firstEntryNumber;

  // The names of the attributes used in the template.
  private final String[] attributeNames;

  // The values of the attributes used in the template.
  private final String[] attributeValues;



  /**
   * Creates a new template-based entry generator from the provided template
   * lines.
   *
   * @param  templateLines     The lines that comprise the template to use to
   *                           generate the entries.
   * @param  firstEntryNumber  The entry number for the first entry to be
   *                           generated.
   *
   * @throws  InvalidValueException  If the provided template cannot be parsed
   *                                 or there is a problem with a parameter.
   */
  public TemplateBasedEntryGenerator(final String[] templateLines,
                                     final int firstEntryNumber)
         throws InvalidValueException
  {
    this.firstEntryNumber = firstEntryNumber;

    attributeNames  = new String[templateLines.length];
    attributeValues = new String[templateLines.length];


    // Parse the template and set up for generating the entries.
    for (int i=0; i < templateLines.length; i++)
    {
      int colonPos = templateLines[i].indexOf(':');
      if (colonPos < 0)
      {
        throw new InvalidValueException("No colon found in template line \"" +
             templateLines[i] + "\" to separate the attribute name from the " +
             "value.");
      }
      else if (colonPos == 0)
      {
        throw new InvalidValueException("No attribute name found in template " +
             "line \"" + templateLines[i] + "\".");
      }
      else if (colonPos == (templateLines[i].length() - 1))
      {
        throw new InvalidValueException("No attribute value found in " +
             "template line \"" + templateLines[i] + "\".");
      }
      attributeNames[i] = templateLines[i].substring(0, colonPos);

      char nextChar = templateLines[i].charAt(colonPos+1);
      if (nextChar == ' ')
      {
        attributeValues[i] = templateLines[i].substring(colonPos+2).trim();
      }
      else if (nextChar == ':')
      {
        throw new InvalidValueException("Attribute " + attributeNames[i] +
             " uses base64-encoding which is not supported.");
      }
      else
      {
        throw new InvalidValueException("Invalid character sequence found " +
             "in template line \"" + templateLines[i] + "\" -- illegal " +
             "character '" + templateLines[i].charAt(colonPos+1) +
             "' in column " + (colonPos+1));
      }
    }
  }



  /**
   * Creates a randomly-generated LDAP entry to be added to the directory.
   *
   * @param  random       The random number generator to use for the entry.
   * @param  entryNumber  The unique entry number for the entry.
   * @param  dn           The DN to use for the entry.
   *
   * @return  The randomly-generated entry, or {@code null} if all entries have
   *          been created.
   *
   * @throws  SLAMDException  If a problem is encountered while trying to
   *                          generate the entry.
   */
  public Entry createEntry(final Random random, final int entryNumber,
                           final String dn)
         throws SLAMDException
  {
    Entry entry = new Entry(dn);

    for (int i=0; i < attributeNames.length; i++)
    {
      String value = processValue(random, attributeValues[i], entry,
                                  entryNumber,
                                  (entryNumber - firstEntryNumber));
      if (value != null)
      {
        entry.addAttribute(attributeNames[i], value);
      }
    }

    return entry;
  }



  /**
   * Generates the appropriate value from the given line in the template.
   *
   * @param  random           The random number generator to use.
   * @param  value            The value to be processed.
   * @param  entry            The entry being generated.
   * @param  entryNumber      The unique number assigned to the entry being
   *                          created.
   * @param  entryInSequence  A counter used to determine how many entries have
   *                          been created so far, not including the current
   *                          entry.
   *
   * @return  The generated value, or {@code null} if there should not be a
   *          value for the attribute.
   *
   * @throws  SLAMDException  If a problem is encountered while generating the
   *                          value.
   */
  private static String processValue(final Random random, final String value,
                                     final Entry entry, final int entryNumber,
                                     final int entryInSequence)
         throws SLAMDException
  {
    String v = value;
    boolean needReprocess = true;
    int pos;

    // If the value contains "<presence:", then determine if it should
    // actually be included in this entry.  If not, then just go to the next
    // attribute
    if ((pos = v.indexOf("<presence:")) >= 0)
    {
      int closePos = v.indexOf('>', pos);
      if (closePos > pos)
      {
        String numStr = v.substring(pos+10, closePos);
        try
        {
          int percentage = Integer.parseInt(numStr);
          int randomValue = ((random.nextInt() & 0x7FFFFFFF) % 100) + 1;
          if (randomValue <= percentage)
          {
            // We have determined that this value should be included in the
            // entry, so remove the "<presence:x>" tag and let it go on to do
            // the rest of the processing on this entry
            v = v.substring(0, pos) + v.substring(closePos+1);
          }
          else
          {
            // We have determined that this value should not be included in
            // the entry, so return null.
            return null;
          }
        }
        catch (NumberFormatException nfe)
        {
          return null;
        }
      }
    }

    // If the value contains "<ifpresent:{attrname}>", then determine if it
    // should actually be included in this entry.  If not, then just go to the
    // next attribute.
    if ((pos = v.indexOf("<ifpresent:")) >= 0)
    {
      int closePos = v.indexOf('>', pos);
      if (closePos > pos)
      {
        int colonPos = v.indexOf(':', pos+11);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          // Look for a specific value to be present
          String attrName   = v.substring(pos+11, colonPos);
          String matchValue = v.substring(colonPos+1, closePos);
          if (! entry.hasAttributeValue(attrName, matchValue))
          {
            return null;
          }
          else
          {
            v = v.substring(0, pos) + v.substring(closePos+1);
          }
        }
        else
        {
          // Just look for the attribute to be present.
          if (! entry.hasAttribute(v.substring(pos+11, closePos)))
          {
            return null;
          }
          else
          {
            v = v.substring(0, pos) + v.substring(closePos+1);
          }
        }
      }
    }

    // If the value contains "<ifabsent:{attrname}>", then determine if it
    // should actually be included in this entry.  If not, then just go to the
    // next attribute.
    if ((pos = v.indexOf("<ifabsent:")) >= 0)
    {
      int closePos = v.indexOf('>', pos);
      if (closePos > pos)
      {
        int colonPos = v.indexOf(':', pos+10);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          // Look for a specific value to be present.
          String attrName   = v.substring(pos+11, colonPos);
          String matchValue = v.substring(colonPos+1, closePos);
          if (entry.hasAttributeValue(attrName, matchValue))
          {
            return null;
          }
          else
          {
            v = v.substring(0, pos) + v.substring(closePos+1);
          }
        }
        else
        {
          // Just look for the attribute to be present.
          if (entry.hasAttribute(v.substring(pos+11, closePos)))
          {
            return null;
          }
          else
          {
            v = v.substring(0, pos) + v.substring(closePos+1);
          }
        }
      }
    }

    while (needReprocess && v.contains("<"))
    {
      needReprocess = false;


      // If the value contains "<entryNumber>" then replace that with the first
      // name
      if ((pos = v.indexOf("<entrynumber>")) >= 0)
      {
        v = v.substring(0, pos) + entryNumber + v.substring(pos + 13);
        needReprocess = true;
      }
      if ((pos = v.indexOf("<entryNumber>")) >= 0)
      {
        v = v.substring(0, pos) + entryNumber + v.substring(pos + 13);
        needReprocess = true;
      }

      // If the value contains "<random:chars:characters:length>" then
      // generate a random string of length characters from the provided
      // character set.
      if ((pos = v.indexOf("<random:chars:")) >= 0)
      {
        // Get the set of characters to use in the resulting value.
        int colonPos = v.indexOf(':', pos+14);
        int closePos = v.indexOf('>', colonPos+1);
        String charSet = v.substring(pos+14, colonPos);

        // See if there is an additional colon followed by a number.  If so,
        // then the length will be a random number between the two.
        int count;
        int colonPos2 = v.indexOf(':', colonPos+1);
        if ((colonPos2 > 0) && (colonPos2 < closePos))
        {
          int minValue = Integer.parseInt(v.substring(colonPos+1, colonPos2));
          int maxValue = Integer.parseInt(v.substring(colonPos2+1, closePos));
          int span = maxValue - minValue + 1;
          count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
        }
        else
        {
          count = Integer.parseInt(v.substring(colonPos+1, closePos));
        }

        String randVal =
             generateRandomValue(random, charSet.toCharArray(), count);
        v = v.substring(0, pos) + randVal + v.substring(closePos+1);
        needReprocess = true;
      }

      // If the value contains "<random:alpha:num>" then generate a random
      // alphabetic value and use it.
      if ((pos = v.indexOf("<random:alpha:")) >= 0)
      {
        // See if there is an additional colon followed by a number.  If so,
        // then the length will be a random number between the two.
        int count;
        int closePos = v.indexOf('>', pos+14);
        int colonPos = v.indexOf(':', pos+14);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          int minValue = Integer.parseInt(v.substring(pos+14, colonPos));
          int maxValue = Integer.parseInt(v.substring(colonPos+1, closePos));
          int span = maxValue - minValue + 1;
          count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
         }
        else
        {
          count = Integer.parseInt(v.substring(pos+14, closePos));
        }

        // Generate the new value.
        String randVal = generateRandomValue(random, ALPHA_CHARS, count);
        v = v.substring(0, pos) + randVal + v.substring(closePos + 1);
        needReprocess = true;
      }

      // If the value contains "<random:numeric:num>" then generate a random
      // numeric value and use it.  This can also take the form
      // "<random:numeric:min:max>" or "<random:numeric:min:max:length>".
      if ((pos = v.indexOf("<random:numeric:")) >= 0)
      {
        int closePos = v.indexOf('>', pos);

        // See if there is an extra colon.  If so, then generate a random
        // number between x and y.  Otherwise, generate a random number with
        // the specified number of digits.
        int extraColonPos = v.indexOf(':', pos+16);
        if ((extraColonPos > 0) && (extraColonPos < closePos))
        {
          // See if there is one more colon separating the max from the
          // length.  If so, then get it and create a padded value of at least
          // length digits.  If not, then just generate the random value.
          int extraColonPos2 = v.indexOf(':', extraColonPos+1);
          if ((extraColonPos2 > 0) && (extraColonPos2 < closePos))
          {
            String lowerBoundStr = v.substring(pos+16, extraColonPos);
            String upperBoundStr = v.substring(extraColonPos+1, extraColonPos2);
            String lengthStr = v.substring(extraColonPos2+1, closePos);
            int lowerBound = Integer.parseInt(lowerBoundStr);
            int upperBound = Integer.parseInt(upperBoundStr);
            int length = Integer.parseInt(lengthStr);
            int span = (upperBound - lowerBound + 1);
            int randomValue = (random.nextInt() & 0x7FFFFFFF) % span +
                              lowerBound;
            String valueStr = String.valueOf(randomValue);
            while (valueStr.length() < length)
            {
              valueStr = '0' + valueStr;
            }
            v = v.substring(0, pos) + valueStr + v.substring(closePos+1);
          }
          else
          {
            String lowerBoundStr = v.substring(pos+16, extraColonPos);
            String upperBoundStr = v.substring(extraColonPos+1, closePos);
            int lowerBound = Integer.parseInt(lowerBoundStr);
            int upperBound = Integer.parseInt(upperBoundStr);
            int span = (upperBound - lowerBound + 1);
            int randomValue = (random.nextInt() & 0x7FFFFFFF) % span +
                              lowerBound;
            v = v.substring(0, pos) + randomValue + v.substring(closePos+1);
          }
        }
        else
        {
          // Get the number of characters to include in the value
          int numPos = pos + 16;
          int count = Integer.parseInt(v.substring(numPos, closePos));
          String randVal = generateRandomValue(random, NUMERIC_CHARS, count);
          v = v.substring(0, pos) + randVal + v.substring(closePos+1);
        }

        needReprocess = true;
      }

      // If the value contains "<random:alphanumeric:num>" then generate a
      // random alphanumeric value and use it
      if ((pos = v.indexOf("<random:alphanumeric:")) >= 0)
      {
        // See if there is an additional colon followed by a number.  If so,
        // then the length will be a random number between the two.
        int count;
        int closePos = v.indexOf('>', pos+21);
        int colonPos = v.indexOf(':', pos+21);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          int minValue = Integer.parseInt(v.substring(pos+21, colonPos));
          int maxValue = Integer.parseInt(v.substring(colonPos+1,
                                                          closePos));
          int span = maxValue - minValue + 1;
          count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
         }
        else
        {
          count = Integer.parseInt(v.substring(pos+21, closePos));
        }

        // Generate the new value.
        String randVal = generateRandomValue(random, ALPHANUMERIC_CHARS, count);
        v = v.substring(0, pos) + randVal + v.substring(closePos + 1);
        needReprocess = true;
      }

      // If the value contains "<random:hex:num>" then generate a random
      // hexadecimal value and use it
      if ((pos = v.indexOf("<random:hex:")) >= 0)
      {
        // See if there is an additional colon followed by a number.  If so,
        // then the length will be a random number between the two.
        int count;
        int closePos = v.indexOf('>', pos+12);
        int colonPos = v.indexOf(':', pos+12);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          int minValue = Integer.parseInt(v.substring(pos+12, colonPos));
          int maxValue = Integer.parseInt(v.substring(colonPos+1, closePos));
          int span = maxValue - minValue + 1;
          count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
         }
        else
        {
          count = Integer.parseInt(v.substring(pos+12, closePos));
        }

        // Generate the new value.
        String randVal = generateRandomValue(random, HEX_CHARS, count);
        v = v.substring(0, pos) + randVal + v.substring(closePos + 1);
        needReprocess = true;
      }

      // If the value contains "<random:base64:num>" then generate a random
      // base64 value and use it
      if ((pos = v.indexOf("<random:base64:")) >= 0)
      {
        // See if there is an additional colon followed by a number.  If so,
        // then the length will be a random number between the two.
        int count;
        int closePos = v.indexOf('>', pos+15);
        int colonPos = v.indexOf(':', pos+15);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          int minValue = Integer.parseInt(v.substring(pos+15, colonPos));
          int maxValue = Integer.parseInt(v.substring(colonPos+1, closePos));
          int span = maxValue - minValue + 1;
          count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
         }
        else
        {
          count = Integer.parseInt(v.substring(pos+15, closePos));
        }

        // Generate the new value.
        String randVal = generateRandomValue(random, BASE64_CHARS, count);
        switch (count % 4)
        {
          case 1:  randVal += "===";
                   break;
          case 2:  randVal += "==";
                   break;
          case 3:  randVal += "=";
                   break;
        }
        v = v.substring(0, pos) + randVal + v .substring(closePos + 1);
        needReprocess = true;
      }

      // If the value contains "<random:telephone>" then generate a random
      // telephone number and use it
      if ((pos = v.indexOf("<random:telephone>")) >= 0)
      {
        // Get the number of characters to include in the value
        String randVal = generateRandomValue(random, NUMERIC_CHARS, 10);
        v = v.substring(0, pos) + randVal.substring(0, 3) + '-' +
                randVal.substring(3, 6) + '-' + randVal.substring(6) +
                v.substring(pos + 18);
        needReprocess = true;
      }

      // If the value contains "<random:month>" then choose a random month
      // name.  Optionally, look for "<random:month:length>" and use at most
      // length characters of the month name.
      if ((pos = v.indexOf("<random:month")) >= 0)
      {
        int closePos = v.indexOf('>', pos+13);
        String monthStr = MONTH_NAMES[(random.nextInt() & 0x7FFFFFFF) % 12];

        // See if there is another colon that specifies the length.
        int colonPos = v.indexOf(':', pos+13);
        if ((colonPos > 0) && (colonPos < closePos))
        {
          String lengthStr = v.substring(colonPos+1, closePos);
          int length = Integer.parseInt(lengthStr);
          if (monthStr.length() > length)
          {
            monthStr = monthStr.substring(0, length);
          }
        }

        v = v.substring(0, pos) + monthStr + v.substring(closePos+1);
        needReprocess = true;
      }

      // If the value contains "<guid>" then generate a GUID and use it
      if ((pos = v.indexOf("<guid>")) >= 0)
      {
        // Get the number of characters to include in the value
        v = v.substring(0, pos) + UUID.randomUUID().toString() +
            v.substring(pos + 6);
        needReprocess = true;
      }

      // If the value contains "<sequential>" then use the next sequential
      // value for that attribute
      if ((pos = v.indexOf("<sequential")) >= 0)
      {
        int closePos = v.indexOf('>', pos);

        // If a starting point was specified, then use it.  If not, then use 0.
        int colonPos = v.indexOf(':', pos);
        int startingValue = 0;
        if ((colonPos > pos) && (colonPos < closePos))
        {
          startingValue = Integer.parseInt(v.substring(colonPos+1, closePos));
        }

        v = v.substring(0, pos) + (startingValue + entryInSequence) +
            v.substring(closePos+1);
        needReprocess = true;
      }
    }

    needReprocess = true;
    while (needReprocess && ((pos = v.indexOf('{')) >= 0))
    {
      // If there is a backslash in front of the curly brace, then we don't
      // want to consider it an attribute name.
      if ((pos > 0) && (v.charAt(pos-1) == '\\'))
      {
        boolean keepGoing  = true;
        boolean nonEscaped = false;
        while (keepGoing)
        {
          v = v.substring(0, pos-1) + v.substring(pos);

          pos = v.indexOf('{', pos);
          if (pos < 0)
          {
            keepGoing = false;
          }
          else if (v.charAt(pos-1) != '\\')
          {
            nonEscaped = true;
          }
        }

        if (! nonEscaped)
        {
          break;
        }
      }


      // If the value has "{attr}", then try to replace it with the value of
      // that attribute.  Note that attribute replacement will only work
      // properly for attributes that are defined in the template before the
      // attribute that attempts to use its value.  If the specified attribute
      // has more than one value, then the first value found will be used.
      int closePos = v.indexOf('}', pos);
      if (closePos > 0)
      {
        int colonPos = v.indexOf(':', pos);
        int substringChars = -1;
        String attrName;
        if ((colonPos > 0) && (colonPos < closePos))
        {
          attrName = v.substring(pos+1, colonPos);
          String numStr = v.substring(colonPos+1, closePos);
          try
          {
            substringChars = Integer.parseInt(numStr);
          }
          catch (NumberFormatException nfe)
          {
            throw new SLAMDException("Could not parse an attribute value " +
                                     "range as an integer:  " + nfe, nfe);
          }
        }
        else
        {
          attrName = v.substring(pos+1, closePos);
        }

        String attrValue = entry.getAttributeValue(attrName);
        if (attrValue == null)
        {
          attrValue = "";
        }
        if ((colonPos > 0) && (colonPos < closePos) && (substringChars > 0) &&
            (attrValue.length() > substringChars))
        {
          attrValue = attrValue.substring(0, substringChars);
        }

        v = v.substring(0, pos) + attrValue + v.substring(closePos+1);
        needReprocess = true;
      }
    }

    if ((pos = v.indexOf("<base64:")) >= 0)
    {
      int closePos = v.indexOf('>', pos+8);
      String valueToEncode = v.substring(pos+8, closePos);
      v = v.substring(0, pos) + Base64.encode(valueToEncode) +
          v.substring(closePos+1);
    }

    return v;
  }



  /**
   * Retrieves a string containing the specified number of randomly-chosen
   * characters.
   *
   * @param  random   The random-number generator to use.
   * @param  charSet  The character set from which to take the characters to use
   *                  in the generated value.
   * @param  length   The number of characters to include in the string.
   *
   * @return  A string containing the specified number of randomly-chosen
   *          characters.
   */
  private static String generateRandomValue(final Random random,
                                            final char[] charSet,
                                            final int length)
  {
    StringBuilder buffer = new StringBuilder(length);
    for (int i=0; i < length; i++)
    {
      buffer.append(charSet[random.nextInt(charSet.length)]);
    }

    return buffer.toString();
  }
}
