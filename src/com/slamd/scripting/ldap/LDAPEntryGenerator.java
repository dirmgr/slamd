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
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import com.unboundid.util.Base64;



/**
 * This class handles the work of generating LDAP entries based on information
 * in a template file.  The template files may contain various kinds of tokens
 * that allow the entries generated to be highly customizable.  The tokens that
 * are supported are:
 *
 * <UL>
 *   <LI>&lt;presence:<I>percent</I>&gt; -- Indicates that the associated
 *       attribute/value should be present in approximately <I>percent</I>
 *       percent of the entries generated.</LI>
 *   <LI>&lt;parentdn&gt; -- Will be replaced by the DN of the parent
 *       entry.</LI>
 *   <LI>&lt;random:alpha:<I>length</I>&gt; -- Will be replaced by a random
 *       string of <I>length</I> alphabetic characters.</LI>
 *   <LI>&lt;random:numeric:<I>length</I>&gt; -- Will be replaced by a random
 *       string of <I>length</I> numeric digits.</LI>
 *   <LI>&lt;random:alphanumeric:<I>length</I>&gt; -- Will be replaced by a
 *       random string of <I>length</I> alphanumeric characters.</LI>
 *   <LI>&lt;random:hex:<I>length</I>&gt; -- Will be replaced by a random
 *       string of <I>length</I> hexadecimal digits.</LI>
 *   <LI>&lt;random:base64:<I>length</I>&gt; -- Will be replaced by a random
 *       string of <I>length</I> base64-encoded characters.</LI>
 *   <LI>&lt;random:telephone&gt; -- Will be replaced with a randomly-generated
 *       telephone number in the format 123-456-7890.</LI>
 *   <LI>&lt;sequential&gt; -- Will be replaced by a sequentially-increasing
 *       numeric value with an initial value of zero.</LI>
 *   <LI>&lt;sequential:<I>first-value</I>&gt; -- Will be replaced by a
 *       sequentially-increasing numeric value with an initial value of
 *       <I>first-value</I>.</LI>
 *   <LI>&lt;guid&gt; -- Will be replaced by a number in GUID (globally-unique
 *       identifier) format.  The value generated is not guaranteed to be
 *       globally unique, but will be unique among any entries created by this
 *       generator.</LI>
 *   <LI>&lt;list:<I>value1</I>,<I>value2</I>,...,<I>valueN</I>&gt; --
 *       Will be replaced by a value chosen at random from the list of provided
 *       values.  Each value will have an equal chance of being chosen.</LI>
 *   <LI>&lt;list:<I>value1</I>:<I>weight1</I>,<I>value2</I>:<I>weight2</I>,...,
 *       <I>valueN</I>:<I>weightN</I>&gt; -- Will be replaced by a value
 *       chosen at random from the list of provided values.  The weight
 *       associated with each value determines the likelihood of that value
 *       being chosen.</LI>
 *   <LI>{<I>attr</I>} -- Will be replaced with the value of the attribute
 *       <I>attr</I>.  Note that the specified attribute must be assigned a
 *       value in the template file before any line that references its value in
 *       this manner.</LI>
 *   <LI>{<I>attr</I>:<I>length</I>} -- Will be replaced with the first
 *       <I>length</I> characters from the value of the attribute <I>attr</I>.
 *       Note that the specified attribute must be assigned a value in the
 *       template file before any line that references its value in this
 *       manner.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPEntryGenerator
{
  /**
   * The end of line character that should be used on the current platform.
   */
  public static final String EOL = System.getProperty("line.separator");



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



  // A map containing the templates defined in the template file.
  private HashMap<String,LDAPEntryTemplate> templateHash;

  // A map containing value lists
  private HashMap<String,AttributeValueList> valueLists;

  // The random number generator being used.
  private Random random;

  // The base that we will use for GUIDs instead of the MAC address of the
  // network interface (since that may not exist, and Java can't get to it
  // without native calls anyway).
  private String guidBase;



  /**
   * Creates a new instance of the LDIF generator that parses the command-line
   * parameters and coordinates the LDIF creation.
   */
  public LDAPEntryGenerator()
  {
    // Set the values of the instance variables
    random                = new Random();
    templateHash          = new HashMap<String,LDAPEntryTemplate>();
    valueLists            = new HashMap<String,AttributeValueList>();
    guidBase              = generateRandomValue(HEX_CHARS, 12);
  }



  /**
   * Adds the specified entry template for use with this entry generator.
   *
   * @param  template  The template to use for this entry generator.
   */
  public void addTemplate(LDAPEntryTemplate template)
  {
    templateHash.put(template.getName(), template);
  }



  /**
   * Reads the template file and extracts the branch and template definitions
   * from it.
   *
   * @param  templateFileLines  The lines from the template file that will be
   *                            used for the template generation.
   */
  public void parseTemplateFile(String[] templateFileLines)
  {
    // The lines contained in the current definition
    ArrayList<String> currentDefinition = new ArrayList<String>();

    // The name of the branch or template we are working on
    String name = null;

    // Read through the template file an entry at a time (an entry is a set of
    // consecutive non-blank lines) and process it.  This code is pretty
    // inefficient, but it works and is only called when reading the template
    // information and not when actually creating the entries.
    int lineNumber = 0;
    while (lineNumber < templateFileLines.length)
    {
      String line = templateFileLines[lineNumber];

      if ((line.length() > 0) && (lineNumber < (templateFileLines.length-1)))
      {
        currentDefinition.add(line);
        if (line.toLowerCase().startsWith("template: "))
        {
          name = line.substring(10);
        }
      }
      else
      {
        if ((line.length() > 0) && (lineNumber >= (templateFileLines.length-1)))
        {
          currentDefinition.add(line);
        }

        // Only try to process it if there is something to process
        if (! currentDefinition.isEmpty())
        {
          // It's a template, so determine which RDN attribute to use and
          // parse out the template and subtemplate names.  Everything else
          // will go into the attribute list.
          LDAPEntryTemplate template = new LDAPEntryTemplate(name, "cn");
          for (int i=0; i < currentDefinition.size(); i++)
          {
            line = currentDefinition.get(i);
            if (line.toLowerCase().startsWith("template: "))
            {
              // ignore this because we already have the template name and
              // don't want to make it an attribute
            }
            else if (line.toLowerCase().startsWith("rdnattr: "))
            {
              template.rdnAttribute = line.substring(9);
            }
            else if (line.toLowerCase().startsWith("extends: "))
            {
              template.parentTemplateName = line.substring(9);
            }
            else if (line.indexOf(": ") > 0)
            {
              if ((line.indexOf(":: ") > 0) &&
                  (line.indexOf(":: ") < line.indexOf(": ")))
              {
                String attrName = line.substring(0, line.indexOf(":: "));
                String value = line.substring(line.indexOf(":: ") + 3);
                template.addAttribute(attrName, ":: ", value);
              }
              else
              {
                String attrName = line.substring(0, line.indexOf(": "));
                String value = line.substring(line.indexOf(": ") + 2);
                template.addAttribute(attrName, ": ", value);
              }
            }
          }


          // Finalize this template to configure it for more efficient
          // processing later.
          template.completeInitialization();
          templateHash.put(name.toLowerCase(), template);

          currentDefinition.clear();
          name = null;
        }
      }

      lineNumber++;
    }
  }



  /**
   * Creates a subordinate entry for the specified branch.  Any translation that
   * needs to be done on the attribute values will be taken care of here.
   *
   * @param  parentDN      The DN under which to create the entry.
   * @param  templateName  The name of the template to use to generate the
   *                       entry.
   *
   * @return  The LDAP entry that was created, or a null entry if a problem
   *          occurred.
   */
  public LDAPEntryVariable createTemplateEntry(String parentDN,
                                               String templateName)

  {
    LDAPEntryTemplate template = templateHash.get(templateName.toLowerCase());
    if (template == null)
    {
      return new LDAPEntryVariable();
    }


    String rdnAttr            = template.getRDNAttribute();
    String[][] attrComponents = template.getAttributeComponents();
    LDAPEntryVariable entry   = new LDAPEntryVariable();


    // Set a flag that indicates whether this value needs to be reprocessed or
    // not.  If any changes were made to a value in one iteration, then it
    // should be reprocessed so that additional processing can be performed if
    // necessary.  Another flag should be used to determine if we're in a
    // reprocess in order to know
    boolean needReprocess = false;
    String  value         = null;
    String  rdnValue      = null;


    // Iterate through all the attributes and do any processing that needs to be
    // done.
    for (int i=0; i < attrComponents.length; i++)
    {
      value = attrComponents[i][2];
      needReprocess = true;
      int pos;

      // If the value contains "<presence:", then determine if it should
      // actually be included in this entry.  If not, then just go to the next
      // attribute
      if ((pos = value.indexOf("<presence:")) >= 0)
      {
        int closePos = value.indexOf('>', pos);
        if (closePos > pos)
        {
          String numStr = value.substring(pos+10, closePos);
          try
          {
            int percentage = Integer.parseInt(numStr);
            int randomValue = (Math.abs(random.nextInt()) % 100) + 1;
            if (randomValue <= percentage)
            {
              // We have determined that this value should be included in the
              // LDIF output, so remove the "<presence:x>" tag and let it go on
              // to do the rest of the processing on this entry
              value = value.substring(0, pos) + value.substring(closePos+1);
            }
            else
            {
              // We have determined that this value should not be included in
              // the LDIF output, so just go on to the next one.
              continue;
            }
          } catch (NumberFormatException nfe) {}
        }
      }

      while (needReprocess && (value.indexOf('<') >= 0))
      {
        needReprocess = false;


        // If the value contains "<parentdn>" then replace that with the DN of
        // the parent entry
        if ((pos = value.indexOf("<parentdn>")) >= 0)
        {
          value = value.substring(0, pos) + parentDN +
                  value.substring(pos + 10);
        }


        // If the value contains "<random:alpha:num>" then generate a random
        // alphabetic value and use it.
        if ((pos = value.indexOf("<random:alpha:")) >= 0)
        {
          // Get the number of characters to include in the value
          int numPos = pos + 14;
          int count = Integer.parseInt(
                           value.substring(numPos, value.indexOf('>', numPos)));
          String randVal = generateRandomValue(ALPHA_CHARS, count);
          value = value.substring(0, pos) + randVal +
                  value.substring(value.indexOf('>', numPos) + 1);
          needReprocess = true;
        }

        // If the value contains "<random:numeric:num>" then generate a random
        // numeric value and use it
        if ((pos = value.indexOf("<random:numeric:")) >= 0)
        {
          // Get the number of characters to include in the value
          int numPos = pos + 16;
          int count = Integer.parseInt(
                           value.substring(numPos, value.indexOf('>', numPos)));
          String randVal = generateRandomValue(NUMERIC_CHARS, count);
          value = value.substring(0, pos) + randVal +
                  value.substring(value.indexOf('>', numPos) + 1);
          needReprocess = true;
        }

        // If the value contains "<random:alphanumeric:num>" then generate a
        // random alphanumeric value and use it
        if ((pos = value.indexOf("<random:alphanumeric:")) >= 0)
        {
          // Get the number of characters to include in the value
          int numPos = pos + 21;
          int count = Integer.parseInt(
                           value.substring(numPos,value.indexOf('>', numPos)));
          String randVal = generateRandomValue(ALPHANUMERIC_CHARS, count);
          value = value.substring(0, pos) + randVal +
                  value.substring(value.indexOf('>', numPos) + 1);
          needReprocess = true;
        }

        // If the value contains "<random:hex:num>" then generate a random
        // hexadecimal value and use it
        if ((pos = value.indexOf("<random:hex:")) >= 0)
        {
          // Get the number of characters to include in the value
          int numPos = pos + 12;
          int count = Integer.parseInt(
                           value.substring(numPos, value.indexOf('>', numPos)));
          String randVal = generateRandomValue(HEX_CHARS, count);
          value = value.substring(0, pos) + randVal +
                  value.substring(value.indexOf('>', numPos) + 1);
          needReprocess = true;
        }

        // If the value contains "<random:base64:num>" then generate a random
        // base64 value and use it
        if ((pos = value.indexOf("<random:base64:")) >= 0)
        {
          // Get the number of characters to include in the value
          int numPos = pos + 15;
          int count = Integer.parseInt(
                           value.substring(numPos, value.indexOf('>', numPos)));
          String randVal = generateRandomValue(BASE64_CHARS, count);
          switch (count % 4)
          {
            case 1:  randVal += "===";
                     break;
            case 2:  randVal += "==";
                     break;
            case 3:  randVal += "=";
                     break;
          }

          // Convert the base64-encoded data to a byte array and then convert
          // the binary value back to a string.  It may not be readable, but it
          // should be enough to get the  binary data in the entry.
          try
          {
            byte[] binaryValue = Base64.decode(randVal);
            randVal = new String(binaryValue);
          } catch (Exception e) {}


          value = value.substring(0, pos) + randVal +
                  value.substring(value.indexOf('>', numPos) + 1);
          needReprocess = true;
        }

        // If the value contains "<random:telephone>" then generate a random
        // telephone number and use it
        if ((pos = value.indexOf("<random:telephone>")) >= 0)
        {
          // Get the number of characters to include in the value
          String randVal = generateRandomValue(NUMERIC_CHARS, 10);
          value = value.substring(0, pos) + randVal.substring(0, 3) + '-' +
                  randVal.substring(3, 6) + '-' + randVal.substring(6) +
                  value.substring(pos + 18);
          needReprocess = true;
        }

        // If the value contains "<guid>" then generate a GUID and use it
        if ((pos = value.indexOf("<guid>")) >= 0)
        {
          // Get the number of characters to include in the value
          value = value.substring(0, pos) + generateGUID() +
                  value.substring(pos + 6);
          needReprocess = true;
        }

        // If the value contains "<sequential>" then use the next sequential
        // value for that attribute
        if ((pos = value.indexOf("<sequential")) >= 0)
        {
          int closePos = value.indexOf('>', pos);

          // Get the sequential counter for that attribute
          int counterValue = template.getCounterValue(attrComponents[i][3]);
          if (counterValue == Integer.MIN_VALUE)
          {
            int colonPos = value.indexOf(':', pos);
            int firstValue = 0;
            if ((colonPos > pos) && (colonPos < closePos))
            {
              firstValue = Integer.parseInt(value.substring(colonPos+1,
                                                            closePos));
            }
            SequentialValueCounter c = new SequentialValueCounter(firstValue);
            template.addCounter(attrComponents[i][3], c);
            counterValue = c.getNext();
          }

          value = value.substring(0, pos) + counterValue +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<list:" then treat it as a value list and get
        // the value from that
        if ((pos = value.indexOf("<list:")) >= 0)
        {
          int closePos = value.indexOf('>', pos);

          // See if a value list already exists for the specified attribute.  If
          // not, then create one
          AttributeValueList avl = valueLists.get(attrComponents[i][3]);
          if (avl == null)
          {
            avl = new AttributeValueList();
            String listVals = value.substring(pos+6, closePos);
            StringTokenizer tokenizer = new StringTokenizer(listVals, ",");
            while (tokenizer.hasMoreElements())
            {
              String listValue = tokenizer.nextToken();
              int colonPos = listValue.indexOf(':');
              if (colonPos > 0)
              {
                String val = listValue.substring(0, colonPos);
                int weight = Integer.parseInt(listValue.substring(colonPos+1));
                avl.addValue(val, weight);
              }
              else
              {
                avl.addValue(listValue);
              }
            }
            avl.completeInitialization();
            valueLists.put(attrComponents[i][3], avl);
          }

          // Get the list value and use it
          value = value.substring(0, pos) + avl.nextValue() +
                  value.substring(closePos+1);
          needReprocess = true;
        }
      }

      needReprocess = true;
      while (needReprocess && ((pos = value.indexOf('{')) >= 0))
      {
        needReprocess = false;
        // If the value has "{attr}", then try to replace it with the value of
        // that attribute.  Note that attribute replacement will only work
        // properly for attributes that are defined in the template before the
        // attribute that attempts to use its value.  If the specified attribute
        // has more than one value, then the first value found will be used.
        int closePos = value.indexOf('}', pos);
        if (closePos > 0)
        {
          int colonPos = value.indexOf(':', pos);
          int substringChars = -1;
          String attrName = null;
          if ((colonPos > 0) && (colonPos < closePos))
          {
            attrName = value.substring(pos+1, colonPos).toLowerCase();
            String numStr = value.substring(colonPos+1, closePos);
            try
            {
              substringChars = Integer.parseInt(numStr);
            } catch (NumberFormatException nfe) {}
          }
          else
          {
            attrName = value.substring(pos+1, closePos).toLowerCase();
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
          value = value.substring(0, pos) + attrValue +
                  value.substring(closePos+1);
          needReprocess = true;
        }
      }

      entry.addAttribute(new LDAPAttributeVariable(attrComponents[i][0],
                                                   value));
      if ((rdnValue == null) && (attrComponents[i][3].equals(rdnAttr)))
      {
        rdnValue = value;
      }
    }


    // All of the values have been processed.  Now try to calculate the DN for
    // the entry.
    if (rdnValue == null)
    {
System.err.println("Returning NULL entry -- no RDN value");
      return null;
    }


    // Create the DN for the entry and return it.
    String entryDN = rdnAttr + '=' + rdnValue + ',' + parentDN;
    entry.setDN(entryDN);
    return entry;
  }



  /**
   * Generates a random value of the indicated length from the specified
   * character set.
   *
   * @param  charSet  The character set from which the random characters are to
   *                  be taken.
   * @param  length   The length of the random value to generate.
   *
   * @return  The random value that was generated.
   */
  public String generateRandomValue(char[] charSet, int length)
  {
    char[] retArray = new char[length];

    for (int i=0; i < retArray.length; i++)
    {
      retArray[i] = charSet[Math.abs(random.nextInt()) % charSet.length];
    }

    return new String(retArray);
  }



  /**
   * Generates a globally-unique identifier.  Technically speaking, it's not
   * guaranteed to be globally unique, but this should be good enough for most
   * purposes.
   *
   * @return  The GUID that was generated.
   */
  public String generateGUID()
  {
    String timeStr = Long.toHexString(System.currentTimeMillis());
    String tmpStr = timeStr +
                    generateRandomValue(HEX_CHARS, 20-timeStr.length());
    return tmpStr.substring(0, 8) + '-' + tmpStr.substring(8, 12) + '-' +
           tmpStr.substring(12,16) + '-' + tmpStr.substring(16) + '-' +
           guidBase;
  }
}

