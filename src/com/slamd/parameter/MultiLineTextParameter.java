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
package com.slamd.parameter;



import java.util.ArrayList;
import java.util.StringTokenizer;

import com.slamd.common.Constants;



/**
 * This class defines a parameter that may contain multiple lines of text.  Each
 * line may be treated as an individual string.
 *
 *
 * @author   Neil A. Wilson
 */
public class MultiLineTextParameter
       extends Parameter
{
  // The number of columns that will be visible in the text area used to
  // retrieve the value of the parameter.
  private int visibleColumns = 30;

  // The number of rows that will be visible in the text area used to retrieve
  // the value of the parameter.
  private int visibleRows = 5;

  // The lines of text associated with this parameter
  private String[] lines;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public MultiLineTextParameter()
  {
    super();

    // Create an empty set of lines.
    lines = new String[0];
  }



  /**
   * Creates a new multi-line text parameter with the specified name.  The
   * display name will be the same as the name, it will not have a description
   * or set of lines, and it will not be required.
   *
   * @param  name  The name to use for this parameter.
   */
  public MultiLineTextParameter(String name)
  {
    this(name, name, null, new String[0], false);
  }



  /**
   * Creates a new multi-line text parameter with the specified name and
   * required/optional indicator.  The display name will be the same as the
   * name, and it will not have a description or set of lines.
   *
   * @param  name        The name to use for this parameter.
   * @param  isRequired  Indicates whether this parameter is required to have a
   *                     value.
   */
  public MultiLineTextParameter(String name, boolean isRequired)
  {
    this(name, name, null, new String[0], isRequired);
  }



  /**
   * Creates a new multi-line text parameter with the specified name and set of
   * lines.  The display name will be the same as the name, it will not have a
   * description, and it will not be required.
   *
   * @param  name   The name to use for this parameter.
   * @param  lines  The set of lines associated with this parameter.
   */
  public MultiLineTextParameter(String name, String[] lines)
  {
    this(name, name, null, lines, false);
  }



  /**
   * Creates a new multi-line text parameter with the specified name, set of
   * lines, and required/optional indicator.  The display name will be the same
   * as the name and it will not have a description.
   *
   * @param  name        The name to use for this parameter.
   * @param  lines       The set of lines associated with this parameter.
   * @param  isRequired  Indicates whether this parameter is required to have a
   *                     value.
   */
  public MultiLineTextParameter(String name, String[] lines, boolean isRequired)
  {
    this(name, name, null, lines, isRequired);
  }



  /**
   * Creates a new multi-line parameter with the specified name and display
   * name.  It will not have a description or set of lines, and it will not be
   * required.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   */
  public MultiLineTextParameter(String name, String displayName)
  {
    this(name, displayName, null, new String[0], false);
  }



  /**
   * Creates a new multi-line parameter with the specified name, display name,
   * and required/optional indicator.  It will not have a description or set of
   * lines.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   */
  public MultiLineTextParameter(String name, String displayName,
                                boolean isRequired)
  {
    this(name, displayName, null, new String[0], isRequired);
  }



  /**
   * Creates a new multi-line parameter with the specified name, display name,
   * and set of lines.  It will not have a description, and it will not be
   * required.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  lines        The set of lines associated with this parameter.
   */
  public MultiLineTextParameter(String name, String displayName, String[] lines)
  {
    this(name, displayName, null, lines, false);
  }



  /**
   * Creates a new multi-line parameter with the specified name, display name,
   * set of lines, and required/optional indicator.  It will not have a
   * description.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  lines        The set of lines associated with this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   */
  public MultiLineTextParameter(String name, String displayName, String[] lines,
                                boolean isRequired)
  {
    this(name, displayName, null, lines, isRequired);
  }



  /**
   * Creates a new multi-line parameter with the specified name, display name,
   * description, and set of lines.  It will not be required.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  lines        The set of lines associated with this parameter.
   */
  public MultiLineTextParameter(String name, String displayName,
                                String description, String[] lines)
  {
    this(name, displayName, description, lines, false);
  }



  /**
   * Creates a new multi-line parameter with the specified name, display name,
   * description, set of lines, and required/optional indicator.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  lines        The set of lines associated with this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   */
  public MultiLineTextParameter(String name, String displayName,
                                String description, String[] lines,
                                boolean isRequired)
  {
    super(name, displayName, description, isRequired, lines);
    this.lines = lines;
  }



  /**
   * Retrieves the set of lines associated with this parameter.
   *
   * @return  The set of lines associated with this parameter.
   */
  public String[] getLines()
  {
    if (lines == null)
    {
      return new String[0];
    }

    return lines;
  }



  /**
   * Retrieves the set of non-blank lines associated with this parameter.
   *
   * @return  The set of non-blank lines associated with this parameter.
   */
  public String[] getNonBlankLines()
  {
    if (lines == null)
    {
      return new String[0];
    }

    ArrayList<String> lineList = new ArrayList<String>(lines.length);
    for (int i=0; i < lines.length; i++)
    {
      if (lines[i].length() > 0)
      {
        lineList.add(lines[i]);
      }
    }

    String[] nonBlankLines = new String[lineList.size()];
    lineList.toArray(nonBlankLines);
    return nonBlankLines;
  }



  /**
   * Breaks up a single string containing line breaks into an array of strings
   * separated into lines.
   *
   * @param  multilineText  The text to be broken up by line.
   *
   * @return  An array of strings representing the lines in the text provided.
   */
  public static String[] breakString(String multilineText)
  {
    // First, check to see if this text includes actual line breaks.
    ArrayList<String> stringList = new ArrayList<String>();
    StringTokenizer tokenizer = new StringTokenizer(multilineText, "\n\r");
    while (tokenizer.hasMoreTokens())
    {
      String line = tokenizer.nextToken();

      // See if this line itself has any occurrences of the string "\n", which
      // will also be interpreted as line breaks (for the benefit of the
      // standalone client).
      int breakPos = 0;
      int startPos = 0;
      if (line.contains("\\n"))
      {
        while ((breakPos = line.indexOf("\\n", startPos)) >= 0)
        {
          if (breakPos == 0)
          {
            stringList.add("");
          }
          else
          {
            // If it wasn't "\\n", then treat it as a line break.
            if (line.charAt(breakPos-1) != '\\')
            {
              stringList.add(line.substring(startPos, breakPos));
            }
            else
            {
              // If it was "\\n", then convert it to "\n".
              stringList.add(line.substring(startPos, breakPos) + 'n');
            }
          }

          startPos = breakPos + 2;
        }

        stringList.add(line.substring(startPos));
      }
      else
      {
        stringList.add(line);
      }
    }

    // Next, check to see if it contains the "\n" character, which will also be
    // interpreted as a line break, provided that the backslash is not itself
    // escaped.

    String[] stringArray = new String[stringList.size()];
    stringList.toArray(stringArray);
    return stringArray;
  }



  /**
   * Retrieves the value for this parameter.
   *
   * @return  The value for this parameter.
   */
  @Override()
  public String[] getValue()
  {
    if ((lines == null) || (lines.length == 0))
    {
      return null;
    }
    else
    {
      return lines;
    }
  }



  /**
   * Specifies the value to use for this parameter.  The provided value must
   * either be an array of strings or a single string with line breaks.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws  InvalidValueException  If the provided value is of an invalid type
   *                                 or if there is no value and the parameter
   *                                 is required.
   */
  @Override()
  public void setValue(Object value)
         throws InvalidValueException
  {
    String invalidReason = getInvalidReason(value);
    if (invalidReason != null)
    {
      throw new InvalidValueException(invalidReason);
    }

    if (value instanceof String[])
    {
      this.lines = (String[]) value;
      this.value = lines;
    }
    else if (value instanceof String)
    {
      this.lines = breakString((String) value);
      this.value = lines;
    }
  }



  /**
   * Sets the value for this parameter from the information in the provided
   * parameter.  Note that the provided parameter must be of the same type
   * as this parameter or no action will be taken.
   *
   * @param  parameter  The parameter from which to take the value for this
   *                    parameter.
   */
  @Override()
  public void setValueFrom(Parameter parameter)
  {
    if ((parameter != null) && (parameter instanceof MultiLineTextParameter))
    {
      MultiLineTextParameter mltp = (MultiLineTextParameter) parameter;
      this.value = mltp.value;
      this.lines = mltp.lines;
    }
  }



  /**
   * Retrieves the number of rows that will be visible when setting the value
   * through the HTML interface.
   *
   * @return  The number of rows that will be visible when setting the value
   *          through the HTML interface.
   */
  public int getVisibleRows()
  {
    return visibleRows;
  }



  /**
   * Specifies the number of rows that will be visible when setting the value
   * through the HTML interface.
   *
   * @param  visibleRows  The number of rows that will be visible when setting
   *                      the value through the HTML interface.
   */
  public void setVisibleRows(int visibleRows)
  {
    if (visibleRows > 0)
    {
      this.visibleRows = visibleRows;
    }
  }



  /**
   * Retrieves the number of columns that will be visible when setting the value
   * through the HTML interface.
   *
   * @return  The number of columns that will be visible when setting the value
   *          through the HTML interface.
   */
  public int getVisibleColumns()
  {
    return visibleColumns;
  }



  /**
   * Specifies the number of columns that will be visible when setting the value
   * through the HTML interface.
   *
   * @param  visibleColumns  The number of columns that will be visible when
   *                         setting the value through the HTML interface.
   */
  public void setVisibleColumns(int visibleColumns)
  {
    if (visibleColumns > 0)
    {
      this.visibleColumns = visibleColumns;
    }
  }



  /**
   * Retrieves the value of this parameter as a single string (containing line
   * breaks).
   *
   * @return  The value of this parameter as a single string.
   */
  @Override()
  public String getValueString()
  {
    if ((lines == null) || (lines.length == 0))
    {
      return "";
    }
    else
    {
      StringBuilder returnBuffer = new StringBuilder(lines[0]);

      for (int i=1; i < lines.length; i++)
      {
        returnBuffer.append(Constants.EOL);
        returnBuffer.append(lines[i]);
      }

      return returnBuffer.toString();
    }
  }



  /**
   * Specifies the value to use for this parameter from the provided String.
   * Note that no validation is performed with this method.
   *
   * @param  valueString  The string representation of the value to use for this
   *                      parameter.
   *
   * @throws  InvalidValueException  If the provided value cannot be used to
   *                                 provide a value for this parameter.
   */
  @Override()
  public void setValueFromString(String valueString)
         throws InvalidValueException
  {
    lines = breakString(valueString);
    value = lines;
  }



  /**
   * Retrieves the reason that the specified value is not valid.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return  The reason that the value is not valid, or <CODE>null</CODE> if it
   *          is valid.
   */
  @Override()
  public String getInvalidReason(Object value)
  {
    if (value == null)
    {
      if (isRequired)
      {
        return "No value specified for required parameter";
      }
      else
      {
        return null;
      }
    }
    else if (value instanceof String[])
    {
      String[] valueArr = (String[]) value;
      if (isRequired && (valueArr.length == 0))
      {
        return "No value specified for required parameter";
      }
      else
      {
        return null;
      }
    }
    else if (value instanceof String)
    {
      String[] valueArr = breakString((String) value);
      if (isRequired && (valueArr.length == 0))
      {
        return "No value specified for required parameter";
      }
      else
      {
        return null;
      }
    }
    else
    {
      return (value.getClass().getName() + " is not a supported multi-line " +
              "text object type");
    }
  }



  /**
   * Retrieves the value in a form that may be displayed to the end user.
   *
   * @return  The value in a form that may be displayed to the end user.
   */
  @Override()
  public String getDisplayValue()
  {
    if ((lines == null) || (lines.length == 0))
    {
      return "";
    }
    else
    {
      StringBuilder returnBuffer = new StringBuilder(lines[0]);

      for (int i=1; i < lines.length; i++)
      {
        returnBuffer.append(Constants.EOL);
        returnBuffer.append(lines[i]);
      }

      return returnBuffer.toString();
    }
  }



  /**
   * Retrieves the value in a form that may be displayed to the end user as part
   * of an HTML document.
   *
   * @return  The value in a form that may be displayed to the end user as part
   *          of an HTML document.
   */
  @Override()
  public String getHTMLDisplayValue()
  {
    if ((lines == null) || (lines.length == 0))
    {
      return "";
    }
    else
    {
      StringBuilder returnBuffer = new StringBuilder();
      returnBuffer.append(Constants.makeHTMLSafe(lines[0]));

      for (int i=1; i < lines.length; i++)
      {
        returnBuffer.append("<BR>");
        returnBuffer.append(Constants.EOL);
        returnBuffer.append(Constants.makeHTMLSafe(lines[i]));
      }

      return returnBuffer.toString();
    }
  }



  /**
   * Retrieves a string of text that can be used to request a value for this
   * parameter using an HTML form.  Note that this should just be for the input
   * field itself and should not use the display name or have any special marker
   * to indicate whether the value is required or not, as those are to be added
   * by whatever is generating the HTML page.
   *
   * @param  prefix  The prefix that should be placed in front of the parameter
   *                 name as the name of the form element.
   *
   * @return  A string of text that can be used to request a value for this
   *          parameter using an HTML form.
   */
  @Override()
  public String getHTMLInputForm(String prefix)
  {
    String returnStr = "<TEXTAREA NAME=\"" + prefix + name + "\" ROWS=\"" +
                       visibleRows + "\" COLS=\"" + visibleColumns + "\">" +
                       getValueString() + "</TEXTAREA>";

    return returnStr;
  }



  /**
   * Specifies the value of this parameter based on the provided text that would
   * be returned from posting an HTML form.
   *
   * @param  values  The set of values for this parameter contained in the
   *                 servlet request.
   *
   * @throws  InvalidValueException  If the specified value is not acceptable
   *                                 for this parameter.
   */
  @Override()
  public void htmlInputFormToValue(String[] values)
         throws InvalidValueException
  {
    if ((values == null) || (values.length == 0))
    {
      setValue(null);
    }
    else
    {
      setValue(breakString(values[0]));
    }
  }



  /**
   * Retrieves a string representation of the content that should be included in
   * an HTML form in which this parameter should be provided as a hidden
   * element.
   *
   * @param  prefix  The prefix to use for the parameter name.
   *
   * @return  A string representation of this parameter as a hidden element in
   *          an HTML form.
   */
  @Override()
  public String generateHidden(String prefix)
  {
    if ((lines == null) || (lines.length == 0))
    {
      return "";
    }

    StringBuilder buffer = new StringBuilder();
    for (int i=0; i < lines.length; i++)
    {
      buffer.append(lines[i] + '\n');
    }

    return "<INPUT TYPE=\"HIDDEN\" NAME=\"" + prefix + name +
           "\" VALUE=\"" + buffer.toString() + "\">";
  }



  /**
   * Creates a clone of this parameter.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public MultiLineTextParameter clone()
  {
    MultiLineTextParameter mltp = new MultiLineTextParameter(name, displayName,
                                                             description, lines,
                                                             isRequired);
    mltp.visibleRows    = visibleRows;
    mltp.visibleColumns = visibleColumns;
    mltp.setSensitive(isSensitive());
    return mltp;
  }
}

