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



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;

import com.unboundid.util.Base64;



/**
 * This class defines a utility for reading entries from an LDIF file.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFReader
{
  // The buffered reader that will read lines from the file.
  private BufferedReader reader;

  // The line number of the last line read from the LDIF.
  private int lineNumber;



  /**
   * Creates a new LDIF reader that will read data from the specified file.
   *
   * @param  fileName  The name of the LDIF file from which to read the data.
   *
   * @throws  IOException  If a problem occurs while opening the LDIF file for
   *                       reading.
   */
  public LDIFReader(String fileName)
         throws IOException
  {
    this(new BufferedReader(new FileReader(fileName)));
  }



  /**
   * Creates a new LDIF reader that will read data from provided input stream.
   *
   * @param  inputStream  The input stream from which to read the data.
   */
  public LDIFReader(InputStream inputStream)
  {
    this(new BufferedReader(new InputStreamReader(inputStream)));
  }



  /**
   * Creates a new LDIF reader that will read data from the provided buffered
   * reader.
   *
   * @param  reader  The buffered reader from which to read the data.
   */
  public LDIFReader(BufferedReader reader)
  {
    this.reader = reader;

    lineNumber    = 0;
  }



  /**
   * Reads the next entry from the LDIF file.
   *
   * @return  The next entry from the LDIF file, or <CODE>null</CODE> if there
   *          is no more data to read.
   *
   * @throws  IOException  If a problem occurs while reading data from the file.
   *
   * @throws  ParseException  If a problem occurs while trying to parse an entry
   *                          from the file.
   */
  public LDIFEntry nextEntry()
         throws IOException, ParseException
  {
    ArrayList<StringBuilder> entryLines = new ArrayList<StringBuilder>();
    String line;


    // Read until we find the beginning of the next entry.
    while (true)
    {
      line = reader.readLine();
      if (line == null)
      {
        return null;
      }

      lineNumber++;

      int length = line.length();
      if (length == 0)
      {
        continue;
      }

      if (line.charAt(0) == '#')
      {
        // It's a comment.  Ignore it.
        continue;
      }

      if (line.startsWith("version:"))
      {
        // It's the LDIF version.  Ignore it.
        continue;
      }

      if (! line.startsWith("dn:"))
      {
        throw new ParseException("Invalid entry starting at line " +
                                 lineNumber + ":  expected a DN line but got " +
                                 line, lineNumber);
      }

      entryLines.add(new StringBuilder(line));
      break;
    }


    // Read the rest of the entry.
    char firstChar;
    while (true)
    {
      line = reader.readLine();
      if (line == null)
      {
        break;
      }

      lineNumber++;
      if (line.length() == 0)
      {
        break;
      }

      firstChar = line.charAt(0);
      if (firstChar == '#')
      {
        continue;
      }
      else if (firstChar == ' ')
      {
        entryLines.get(entryLines.size()-1).append(line.substring(1));
      }
      else
      {
        entryLines.add(new StringBuilder(line));
      }
    }


    // Convert the data to an LDIF entry.
    StringBuilder dnLine = entryLines.get(0);
    dnLine.delete(0, 3);
    boolean isBase64 = false;
    while (((firstChar = dnLine.charAt(0)) == ' ') || (firstChar == ':'))
    {
      if (firstChar == ':')
      {
        isBase64 = true;
      }

      dnLine.deleteCharAt(0);
    }

    LDIFEntry entry;
    if (isBase64)
    {
      String dnStr =
           new String(Base64.decode(dnLine.toString()), "UTF-8").trim();
      entry = new LDIFEntry(dnStr);
    }
    else
    {
      entry = new LDIFEntry(dnLine.toString().trim());
    }

attrLoop:
    for (int i=1; i < entryLines.size(); i++)
    {
      StringBuilder attrLine = entryLines.get(i);
      int colonPos = attrLine.indexOf(":");
      if (colonPos <= 0)
      {
        throw new ParseException("Invalid attribute definition " + attrLine +
                                 " in entry ending at line " + lineNumber,
                                 lineNumber);
      }

      String attrName = attrLine.substring(0, colonPos);
      attrLine.delete(0, colonPos+1);
      if (attrLine.length() > 0)
      {
        isBase64 = false;
        while (((firstChar = attrLine.charAt(0)) == ' ') || (firstChar == ':'))
        {
          attrLine.deleteCharAt(0);
          if (attrLine.length() == 0)
          {
            continue attrLoop;
          }
          if (firstChar == ':')
          {
            isBase64 = true;
          }
        }

        if (isBase64)
        {
          String valueStr =
               new String(Base64.decode(attrLine.toString()), "UTF-8").trim();
          entry.addAttribute(attrName, toLowerCase(attrName), valueStr);
        }
        else
        {
          entry.addAttribute(attrName, toLowerCase(attrName),
                             attrLine.toString().trim());
        }
      }
    }

    return entry;
  }



  /**
   * Closes this LDIF reader and the handle to the underlying file or input
   * stream.
   *
   * @throws  IOException  If a problem occurs while attempting to close this
   *                       reader.
   */
  public void close()
         throws IOException
  {
    reader.close();
  }



  /**
   * Retrieves an all-lowercase version of the provided string.  This is much
   * faster than <CODE>String.toLowerCase()</CODE> for strings that contain only
   * ASCII characters.
   *
   * @param  s  The string to convert to lowercase.
   *
   * @return  The lowercase representation of the provided string.
   */
  public static String toLowerCase(String s)
  {
    int length = s.length();

    StringBuilder buffer = new StringBuilder(length);
    for (int i=0; i < length; i++)
    {
      char c = s.charAt(i);
      if ((c & 0x7F) != c)
      {
        buffer.append(s.substring(i).toLowerCase());
        return buffer.toString();
      }

      switch (c)
      {
        case 'A':
          buffer.append('a');
          break;
        case 'B':
          buffer.append('b');
          break;
        case 'C':
          buffer.append('c');
          break;
        case 'D':
          buffer.append('d');
          break;
        case 'E':
          buffer.append('e');
          break;
        case 'F':
          buffer.append('f');
          break;
        case 'G':
          buffer.append('g');
          break;
        case 'H':
          buffer.append('h');
          break;
        case 'I':
          buffer.append('i');
          break;
        case 'J':
          buffer.append('j');
          break;
        case 'K':
          buffer.append('k');
          break;
        case 'L':
          buffer.append('l');
          break;
        case 'M':
          buffer.append('m');
          break;
        case 'N':
          buffer.append('n');
          break;
        case 'O':
          buffer.append('o');
          break;
        case 'P':
          buffer.append('p');
          break;
        case 'Q':
          buffer.append('q');
          break;
        case 'R':
          buffer.append('r');
          break;
        case 'S':
          buffer.append('s');
          break;
        case 'T':
          buffer.append('t');
          break;
        case 'U':
          buffer.append('u');
          break;
        case 'V':
          buffer.append('v');
          break;
        case 'W':
          buffer.append('w');
          break;
        case 'X':
          buffer.append('x');
          break;
        case 'Y':
          buffer.append('y');
          break;
        case 'Z':
          buffer.append('z');
          break;
        default:
          buffer.append(c);
          break;
      }
    }

    return buffer.toString();
  }
}

