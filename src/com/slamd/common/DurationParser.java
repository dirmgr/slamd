/*
 * Copyright 2010 UnboundID Corp.
 * All Rights Reserved.
 */
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
package com.slamd.common;



/**
 * This class provides a utility that may be used to parse string
 * representations of durations.  A duration string may be an integer, in which
 * case it will be interpreted as seconds.  Otherwise, it may be specified as a
 * sequence of integers followed by units, delimited by spaces or commas.
 * Supported units include:
 * <UL>
 *   <LI>s, sec, secs, second, seconds -- Indicates that the preceding integer
 *       value represents a number of seconds.</LI>
 *   <LI>m, min, mins, minute, minutes -- Indicates that the preceding integer
 *       value represents a number of minutes.</LI>
 *   <LI>h, hr, hrs, hour, hours -- Indicates that the preceding integer value
 *       represents a number of hours.</LI>
 *   <LI>d, day, days -- Indicates that the preceding integer value represents a
 *       number of days.</LI>
 * </UL>
 * <BR>
 * The following are examples of valid ways to specify a duration of "123456"
 * seconds:
 * <UL>
 *   <LI>123456</LI>
 *   <LI>123456s</LI>
 *   <LI>123456 seconds</LI>
 *   <LI>1d 10h 17m 36s</LI>
 *   <LI>1d 10h 17m 36s</LI>
 *   <LI>1 day 10 hours 17 minutes 36 seconds</LI>
 *   <LI>1 day, 10 hours, 17 minutes, 36 seconds</LI>
 * </UL>
 */
public final class DurationParser
{
  /**
   * Parses the provided string as a duration.
   *
   * @param  s  The string to be parsed.
   *
   * @return  The number of seconds represented by the provided duration.
   *
   * @throws  SLAMDException  If the provided string cannot be parsed as a
   *                          valid duration.
   */
  public static int parse(final String s)
         throws SLAMDException
  {
    if (s == null)
    {
      throw new SLAMDException("Unable to parse a duration value because the " +
           "provided string is null.");
    }

    final StringBuilder b = new StringBuilder(s.trim().toLowerCase());
    if (b.length() == 0)
    {
      throw new SLAMDException("Unable to parse a duration value because the " +
           "provided string is empty.");
    }

    int numSeconds = 0;
    while (b.length() > 0)
    {
      final int constantValue = extractNumeric(s, b);
      if (b.length() == 0)
      {
        numSeconds += constantValue;
      }
      else
      {
        final int unitValue = extractUnit(s, b);
        numSeconds += (constantValue * unitValue);
      }
    }

    return numSeconds;
  }



  /**
   * Extracts a numeric value from the provided buffer, removing digits as they
   * are encountered.
   *
   * @param  s  The original string value that was provided.
   * @param  b  The buffer to be processed.
   *
   * @return  The numeric value that has been extracted.
   *
   * @throws  SLAMDException  If a problem is encountered.
   */
  private static int extractNumeric(final String s, final StringBuilder b)
          throws SLAMDException
  {
    int value = 0;

    boolean digitFound = false;
    while (b.length() > 0)
    {
      final char c = b.charAt(0);
      switch (c)
      {
        case '0':
          value *= 10L;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '1':
          value = (value * 10) + 1;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '2':
          value = (value * 10) + 2;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '3':
          value = (value * 10) + 3;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '4':
          value = (value * 10) + 4;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '5':
          value = (value * 10) + 5;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '6':
          value = (value * 10) + 6;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '7':
          value = (value * 10) + 7;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '8':
          value = (value * 10) + 8;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case '9':
          value = (value * 10) + 9;
          b.deleteCharAt(0);
          digitFound = true;
          break;

        case ' ':
        case ',':
          b.deleteCharAt(0);
          break;

        default:
          if (! digitFound)
          {
            throw new SLAMDException("Unable to parse the provided string '" +
                 s + "' as a duration because it was missing a numeric " +
                 "element where one was expected.");
          }

          return value;
      }
    }

    return value;
  }



  /**
   * Extracts a unit from the provided buffer, removing the associated
   * characters as they are processed.
   *
   * @param  s  The original string that was provided.
   * @param  b  The buffer to be processed.
   *
   * @return  The numeric value representing the number of seconds in that unit.
   *
   * @throws  SLAMDException  If a problem is encountered.
   */
  private static int extractUnit(final String s, final StringBuilder b)
          throws SLAMDException
  {
    final StringBuilder unitBuffer = new StringBuilder();

    while (b.length() > 0)
    {
      final char c = b.charAt(0);
      if (Character.isDigit(c))
      {
        break;
      }

      if ((c == ' ') || (c == ','))
      {
        b.deleteCharAt(0);
        break;
      }

      unitBuffer.append(c);
      b.deleteCharAt(0);
    }

    final String unit = unitBuffer.toString();
    if (unit.equals("s") || unit.equals("sec") || unit.equals("secs") ||
         unit.equals("second") || unit.equals("seconds"))
    {
      return 1;
    }
    else if (unit.equals("m") || unit.equals("min") || unit.equals("mins") ||
         unit.equals("minute") || unit.equals("minutes"))
    {
      return 60;
    }
    else if (unit.equals("h") || unit.equals("hr") || unit.equals("hrs") ||
         unit.equals("hour") || unit.equals("hours"))
    {
      return 3600;
    }
    else if (unit.equals("d") || unit.equals("day") || unit.equals("days"))
    {
      return 86400;
    }
    else
    {
      throw new SLAMDException("Unable to parse the provided string '" + s +
           "' as a duration because '" + unit + "' is not a supported unit.");
    }
  }



  /**
   * Provides a number of test cases for the duration parser.
   *
   * @param  args  The command-line arguments.
   */
  public static void main(final String[] args)
         throws Exception
  {
    final String[] values =
    {
      "123456",
      "123456s",
      "123456sec",
      "123456secs",
      "123456second",
      "123456seconds",
      " 123456 s ",
      " 123456 sec ",
      " 123456 secs ",
      " 123456 second ",
      " 123456 seconds ",
      " 1 2 3 4 5 6 ",
      " 123,456 ",
      " 123,456 seconds ",
      " 1d37056s",
      " 1 d, 37056s",
      " 1 d , 37056s",
      "1 d 10 h 1056s",
      "1d10h17m36s",
      "1 d 10 h 17 m 36 s",
      "1 day 10 h 17 m 36 s",
      "1 days 10 h 17 m 36 s",
      "1 d 10 hr 17 m 36 s",
      "1 d 10 hrs 17 m 36 s",
      "1 d 10 hour 17 m 36 s",
      "1 d 10 hours 17 m 36 s",
      "1 d 10 h 17 min 36 s",
      "1 d 10 h 17 mins 36 s",
      "1 d 10 h 17 minute 36 s",
      "1 d 10 h 17 minutes 36 s",
      "1 d 10 h 17 m 36 sec",
      "1 d 10 h 17 m 36 secs",
      "1 d 10 h 17 m 36 second",
      "1 d 10 h 17 m 36 seconds",
      "1 day, 10 hours, 17 minutes, 36 seconds"
    };

    for (final String s : values)
    {
      final int numSeconds = parse(s);
      if (numSeconds != 123456)
      {
        System.err.println("ERROR:  String '" + s + "' was parsed as " +
             numSeconds);
      }
      else
      {
        System.out.println("String '" + s + "' was correctly parsed as " +
             numSeconds);
      }
    }
  }
}
