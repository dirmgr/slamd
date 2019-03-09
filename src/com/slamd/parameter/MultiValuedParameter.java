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



import com.slamd.common.Constants;



/**
 * This class defines a parameter that may have multiple concurrent values.
 * The HTML input form will be a list box that allows multiple values to be
 * selected at the same time.  The internal representation of the value of this
 * parameter will be an integer, with the individual values each associated with
 * an increasing power of 2 (the first item will be associated with a value of
 * 1, the second with a value of 2, the third with 4, the fourth with 8, etc.).
 * Anything that uses this parameter type must maintain that mapping externally
 * because the value strings are not included in the encoded version of this
 * parameter that is sent between the client and server.
 *
 *
 * @author   Neil A. Wilson
 */
public class MultiValuedParameter
       extends Parameter
{
  // The numeric value associated with this parameter.
  private int intValue;

  // The number of rows to display at any given time.
  private int visibleRows = 10;

  // The set of potential values that this parameter may have.
  private String[] valueStrings;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public MultiValuedParameter()
  {
    super();


    // Create an empty set of values.
    valueStrings = new String[0];
  }



  /**
   * Creates a new multivalued parameter with the specified name and set of
   * potential values.  The display name will be the same as the name, it will
   * not have a description, no values will be selected, and it will not be
   * required.
   *
   * @param  name          The name to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   */
  public MultiValuedParameter(String name, String[] valueStrings)
  {
    this(name, name, null, valueStrings, 0, false);
  }



  /**
   * Creates a new multivalued parameter with the specified name, set of
   * potential values, and required/optional indicator.  The display name will
   * be the same as the name, it will not have a description, and there will
   * not be any values selected.
   *
   * @param  name          The name to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   * @param  isRequired    Indicates whether this parameter is required to
   *                       have a value.
   */
  public MultiValuedParameter(String name, String[] valueStrings,
                              boolean isRequired)
  {
    this(name, name, null, valueStrings, 0, isRequired);
  }



  /**
   * Creates a new multivalued parameter with the specified name, display name,
   * and set of potential values.  It will not have a description, there will
   * not be any values selected, and it will not be required.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The display name to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   */
  public MultiValuedParameter(String name, String displayName,
                              String[] valueStrings)
  {
    this(name, displayName, null, valueStrings, 0, false);
  }



  /**
   * Creates a new multivalued parameter with the specified name, display name,
   * set of potential values, and required/optional indicator.  It will not have
   * a description and there will not be any values selected.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The display name to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   * @param  isRequired    Indicates whether this parameter is required to
   *                       have a value.
   */
  public MultiValuedParameter(String name, String displayName,
                              String[] valueStrings, boolean isRequired)
  {
    this(name, displayName, null, valueStrings, 0, isRequired);
  }



  /**
   * Creates a new multivalued parameter with the specified name, display name,
   * description, and set of potential values.  There will not be any values
   * selected and it will not be required.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The display name to use for this parameter.
   * @param  description   The description to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   */
  public MultiValuedParameter(String name, String displayName,
                              String description, String[] valueStrings)
  {
    this(name, displayName, description, valueStrings, 0, false);
  }



  /**
   * Creates a new multivalued parameter with the specified name, display name,
   * description, set of potential values, and required/optional indicator.
   * There will not be any values selected.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The display name to use for this parameter.
   * @param  description   The description to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   * @param  isRequired    Indicates whether this parameter is required to
   *                       have a value.
   */
  public MultiValuedParameter(String name, String displayName,
                              String description, String[] valueStrings,
                              boolean isRequired)
  {
    this(name, displayName, description, valueStrings, 0, isRequired);
  }



  /**
   * Creates a new multivalued parameter with the specified name, display name,
   * description, set of potential values, and set of selected values.  It will
   * not be required.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The display name to use for this parameter.
   * @param  description   The description to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   * @param  intValue      The numeric representation that indicates which of
   *                       the values are selected.
   */
  public MultiValuedParameter(String name, String displayName,
                              String description, String[] valueStrings,
                              int intValue)
  {
    this(name, displayName, description, valueStrings, intValue, false);
  }



  /**
   * Creates a new multivalued parameter with the specified name, display name,
   * description, set of potential values, set of selected values, and
   * required/optional indicator.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The display name to use for this parameter.
   * @param  description   The description to use for this parameter.
   * @param  valueStrings  The text associated with the potential values of this
   *                       parameter.
   * @param  intValue      The numeric representation that indicates which of
   *                       the values are selected.
   * @param  isRequired    Indicates whether this parameter is required to
   *                       have a value.
   */
  public MultiValuedParameter(String name, String displayName,
                              String description, String[] valueStrings,
                              int intValue, boolean isRequired)
  {
    super(name, displayName, description, isRequired, intValue);

    this.valueStrings = valueStrings;
    this.intValue     = intValue;
  }



  /**
   * Creates a new multivalued parameter with the specified information.  This
   * version is only intended for internal use, and only for decoding the value
   * of the parameter transported between the client and the server.  The value
   * string provided should be the string representation of the numeric value
   * associated with this parameter.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The display name to use for this parameter.
   * @param  description  The description to use for this parameter.
   * @param  valueString  The string representation of the numeric value for
   *                      this parameter.
   * @param  isRequired   Indicates whether this parameter is required to
   *                      have a value.
   */
  MultiValuedParameter(String name, String displayName, String description,
                       String valueString, boolean isRequired)
  {
    super(name, displayName, description, isRequired, null);

    int intValue = 0;
    try
    {
      intValue = Integer.parseInt(valueString);
    } catch (NumberFormatException nfe) {}

    this.intValue = intValue;
    this.value = intValue;
    this.valueStrings = new String[0];
  }



  /**
   * Retrieves the set of value strings that may be used with this parameter
   * (i.e., the potential values).
   *
   * @return  The set of value strings that may be used with this parameter.
   */
  public String[] getValueStrings()
  {
    return valueStrings;
  }



  /**
   * Retrieves the integer value that specifies which of the potential values
   * are actually selected.
   *
   * @return  The integer value that specifies which of the potential values are
   *          actually selected.
   */
  public int getIntValue()
  {
    return intValue;
  }



  /**
   * Indicates whether the option associated with the specified value is
   * selected.
   *
   * @param  optionValue  The numeric value of the option for which to make the
   *                      determination.
   *
   * @return  <CODE>true</CODE> if the specified value is selected, or
   *          <CODE>false</CODE> if it is not.
   */
  public boolean isSelected(int optionValue)
  {
    return ((optionValue & intValue) == optionValue);
  }



  /**
   * Specifies the integer value to use for this parameter.
   *
   * @param  intValue  The integer value to use for this parameter.
   *
   * @throws  InvalidValueException  If there is a problem with the value
   *                                 information provided.
   */
  public void setValue(int intValue)
         throws InvalidValueException
  {
    setValue(Integer.valueOf(intValue));
  }



  /**
   * Specifies the value to use for this parameter.  The value should be a Java
   * Integer.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws  InvalidValueException  If the value provided is not valid.
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

    this.value = value;
    if (value == null)
    {
      this.intValue = 0;
    }
    else if (value instanceof Integer)
    {
      intValue = ((Integer) value);
    }
    else if (value instanceof String)
    {
      intValue = Integer.parseInt((String) value);
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
    if ((parameter != null) && (parameter instanceof MultiValuedParameter))
    {
      MultiValuedParameter mvp = (MultiValuedParameter) parameter;
      this.value    = mvp.value;
      this.intValue = mvp.intValue;
    }
  }



  /**
   * Retrieves the maximum number of rows that will be visible when displaying
   * the HTML input form.
   *
   * @return  The maximum number of rows that will be visible when displaying
   *          the HTML input form.
   */
  public int getVisibleRows()
  {
    return visibleRows;
  }



  /**
   * Specifies the maximum number of rows that will be visible when displaying
   * the HTML input form.
   *
   * @param  visibleRows  The maximum number of rows that will be visible when
   *                      displaying the HTML input form.
   */
  public void setVisibleRows(int visibleRows)
  {
    this.visibleRows = visibleRows;
  }



  /**
   * Retrieves a string representation of the value of this parameter.
   *
   * @return  A string representation of the value of this parameter.
   */
  @Override()
  public String getValueString()
  {
    return String.valueOf(intValue);
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
    try
    {
      intValue = Integer.parseInt(valueString);
      value = intValue;
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Unable to set int value:  " + e, e);
    }
  }



  /**
   * Retrieves a string that indicates why the provided value is invalid.
   *
   * @param  value  The value for which to obtain the invalid reason.
   *
   * @return  The reason that the specified value is invalid, or
   *          <CODE>null</CODE> if the value is valid.
   */
  @Override()
  public String getInvalidReason(Object value)
  {
    if (value == null)
    {
      if (isRequired)
      {
        return "No value(s) provided for required parameter";
      }
      else
      {
        return null;
      }
    }

    if (value instanceof Integer)
    {
      int intValue = ((Integer) value);
      if (intValue == 0)
      {
        if (isRequired)
        {
          return "No value(s) provided for required parameter";
        }
        else
        {
          return null;
        }
      }

      if (intValue < 0)
      {
        return "Integer value may not be negative";
      }

      if (intValue > getMaxValue())
      {
        return "Integer value may not be greater than " + getMaxValue() +
               " (" + intValue + " was provided)";
      }

      return null;
    }

    if (value instanceof String)
    {
      try
      {
        int intValue = Integer.parseInt((String) value);
        if (intValue == 0)
        {
          if (isRequired)
          {
            return "No value(s) provided for required parameter";
          }
          else
          {
            return null;
          }
        }

        if (intValue < 0)
        {
          return "Integer value may not be negative";
        }

        if (intValue > getMaxValue())
        {
          return "Integer value may not be greater than " + getMaxValue() +
                 " (" + intValue + " was provided)";
        }

        return null;
      }
      catch (NumberFormatException nfe)
      {
        return "Value " + String.valueOf(value) +
               " cannot be converted to an integer";
      }
    }

    return (value.getClass().getName() + " is not a valid object type for " +
            "the set of values for a multivalued parameter");
  }



  /**
   * Indicates whether this parameter has one or more values.
   *
   * @return  <CODE>true</CODE> if this parameter has one or more values, or
   *          <CODE>false</CODE> if not.
   */
  @Override()
  public boolean hasValue()
  {
    return (intValue > 0);
  }



  /**
   * Retrieves a string representation of the value of this parameter.
   *
   * @return  A string representation of the value of this parameter.
   */
  @Override()
  public String getDisplayValue()
  {
    if ((valueStrings == null) || (valueStrings.length == 0))
    {
      return "";
    }
    else
    {
      StringBuilder returnBuffer = new StringBuilder();

      String eol = "";
      for (int i=0; i < valueStrings.length; i++)
      {
        returnBuffer.append(eol);
        if (isSelected(indexToIntValue(i)))
        {
          returnBuffer.append("X ");
        }
        else
        {
          returnBuffer.append("  ");
        }
        returnBuffer.append(valueStrings[i]);
        eol = Constants.EOL;
      }

      return returnBuffer.toString();
    }
  }



  /**
   * Retrieves the value of this parameter in a form that can be displayed in
   * an HTML document.
   *
   * @return  The value of this parameter in a form that can be displayed in an
   *          HTML document.
   */
  @Override()
  public String getHTMLDisplayValue()
  {
    if ((valueStrings == null) || (valueStrings.length == 0))
    {
      return "";
    }
    else
    {
      StringBuilder returnBuffer = new StringBuilder();

      returnBuffer.append("<TABLE BORDER=\"0\">\n");
      returnBuffer.append("  <TR>\n");
      returnBuffer.append("    <TD><B>Selected</B></TD>\n");
      returnBuffer.append("    <TD><B>Value</B></TD>\n");
      returnBuffer.append("  </TR>\n");

      for (int i=0; i < valueStrings.length; i++)
      {
        returnBuffer.append("  <TR>\n");
        if (isSelected(indexToIntValue(i)))
        {
          returnBuffer.append("    <TD>Y</TD>\n");
        }
        else
        {
          returnBuffer.append("    <TD>N</TD>\n");
        }

        returnBuffer.append("    <TD>" + valueStrings[i] + "</TD>\n");
        returnBuffer.append("  </TR>\n");
      }

      returnBuffer.append("</TABLE>\n");

      return returnBuffer.toString();
    }
  }



  /**
   * Retrieves an HTML string that can be used to specify the value(s) for this
   * parameter.
   *
   * @param  prefix  The prefix that should be placed in front of the parameter
   *                 name as the name of the form element.
   *
   * @return  An HTML string that can be used to specify the value(s) for this
   *          parameter.
   */
  @Override()
  public String getHTMLInputForm(String prefix)
  {
    String returnStr = "<SELECT MULTIPLE NAME=\"" + prefix + name +
                       "\" SIZE=\"" +
                       Math.min(visibleRows, valueStrings.length) + "\">\n";

    for (int i=0; i < valueStrings.length; i++)
    {
      String selectedStr = (isSelected(indexToIntValue(i))) ? "SELECTED " : "";
      returnStr += "  <OPTION " + selectedStr + "VALUE=\"" + valueStrings[i] +
                   "\">" + valueStrings[i] + '\n';
    }

    returnStr += "</SELECT>\n";
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
    int returnValue = 0;

    if (values == null)
    {
      setValue(0);
      return;
    }

    for (int i=0; i < valueStrings.length; i++)
    {
      boolean match = false;
      for (int j=0; j < values.length; j++)
      {
        if (values[j].equalsIgnoreCase(valueStrings[i]))
        {
          match = true;
          break;
        }
      }

      if (match)
      {
        returnValue += indexToIntValue(i);
      }
    }

    setValue(returnValue);
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
    if ((valueStrings == null) || (valueStrings.length == 0) || (intValue <= 0))
    {
      return "";
    }

    StringBuilder buffer = new StringBuilder();
    for (int i=0; i < valueStrings.length; i++)
    {
      int twoToTheI = 1;
      for (int j=0; j < i; j++)
      {
        twoToTheI *= 2;
      }

      if ((twoToTheI & intValue) == twoToTheI)
      {
        buffer.append("<INPUT TYPE=\"HIDDEN\" NAME=\"" + prefix + name +
                      "\" VALUE=\"" + valueStrings[i] + "\">");
      }
    }

    return buffer.toString();
  }



  /**
   * Creates a new multivalued parameter that is a duplicate of this parameter.
   *
   * @return  A new multivalued parameter that is a duplicate of this parameter.
   */
  @Override()
  public MultiValuedParameter clone()
  {
    MultiValuedParameter mvp = new MultiValuedParameter(name, displayName,
                                                        description,
                                                        valueStrings, intValue,
                                                        isRequired);
    mvp.visibleRows = visibleRows;
    mvp.setSensitive(isSensitive());
    return mvp;
  }



  /**
   * Provides a mapping between the position of an element in the set of value
   * strings to the numeric value associated with the element in that position.
   *
   * @param  valueIndex  The position of the item in the set of value strings.
   *
   * @return  The integer value associated with the item in the specified
   *          position.
   */
  public int indexToIntValue(int valueIndex)
  {
    if (valueIndex < 0)
    {
      return 0;
    }

    int returnValue = 1;

    for (int i=0; i < valueIndex; i++)
    {
      returnValue *= 2;
    }

    return returnValue;
  }



  /**
   * Retrieves the maximum allowed value for this parameter based on the number
   * of items in the set of potential values.
   *
   * @return  The maximum allowed value for this parameter.
   */
  public int getMaxValue()
  {
    return indexToIntValue(valueStrings.length) - 1;
  }
}

