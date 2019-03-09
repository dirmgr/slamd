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



/**
 * This class defines a parameter that provides the user with a list of options
 * from which the value should be chosen.  Parameters of this type will always
 * be required to have a value.
 *
 *
 * @author   Neil A. Wilson
 */
public class MultiChoiceParameter
       extends Parameter
{
  // The set of valid values that may be used for this parameter.
  private String[] choices;

  // The set of display values that may be used for this parameter.
  private String[] displayValues;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public MultiChoiceParameter()
  {
    super();

    // Create an empty set of choices.
    choices       = new String[0];
    displayValues = new String[0];
  }



  /**
   * Creates a new multichoice parameter with the specified name and set of
   * choices.  The display name will be the same as the name, there will be no
   * description, and the value will default to the first item in the list of
   * choices (unless choices is null or empty).
   *
   * @param  name     The name to use for this parameter.
   * @param  choices  The set of values that may be used for this parameter.
   */
  public MultiChoiceParameter(String name, String[] choices)
  {
    this(name, name, null, choices, choices, null);
  }



  /**
   * Creates a new multichoice parameter with the specified name, value, and
   * set of choices.  The display name will be the same as the name, and there
   * will be no description.
   *
   * @param  name     The name to use for this parameter.
   * @param  choices  The set of values that may be used for this parameter.
   * @param  value    The value to use for this parameter.
   */
  public MultiChoiceParameter(String name, String[] choices, String value)
  {
    this(name, name, null, choices, choices, value);
  }



  /**
   * Creates a new multichoice parameter with the specified name, display name,
   * value, and set of choices.  There will be no description.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter if it to be
   *                      displayed to the end user.
   * @param  choices      The set of values that may be used for this parameter.
   * @param  value        The value to use for this parameter.
   */
  public MultiChoiceParameter(String name, String displayName, String[] choices,
                              String value)
  {
    this(name, displayName, null, choices, choices, value);
  }



  /**
   * Creates a new multichoice parameter with the specified name, display name,
   * description, value, and set of choices.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter if it to be
   *                      displayed to the end user.
   * @param  description  The description to use for this parameter.
   * @param  choices      The set of values that may be used for this parameter.
   * @param  value        The value to use for this parameter.
   */
  public MultiChoiceParameter(String name, String displayName,
                              String description, String[] choices,
                              String value)
  {
    this(name, displayName, description, choices, choices, value);
  }



  /**
   * Creates a new multichoice parameter with the specified name, display name,
   * description, value, and set of choices.
   *
   * @param  name           The name to use for this parameter.
   * @param  displayName    The name to use for this parameter if it to be
   *                        displayed to the end user.
   * @param  description    The description to use for this parameter.
   * @param  choices        The set of values that may be used for this
   *                        parameter.
   * @param  displayValues  The set of values that should be displayed in the
   *                        drop-down list box associated with this parameter.
   * @param  value          The value to use for this parameter.
   */
  public MultiChoiceParameter(String name, String displayName,
                              String description, String[] choices,
                              String[] displayValues, String value)
  {
    super(name, displayName, description, true, value);

    // Make sure that choices is not empty or null.  If it is, and there is a
    // value, then include that value in the set of choices.
    this.choices = choices;
    if ((choices == null) || (choices.length == 0))
    {
      if ((value == null) || (value.length() == 0))
      {
        this.choices = new String[0];
        this.value   = null;
      }
      else
      {
        this.choices = new String[] {value};
      }
    }


    // If the value is not specified, then make it the first option in the
    // list of choices
    else if ((value == null) || (value.length() == 0))
    {
      value = choices[0];
    }


    // If there is a list of choices and a value, then make sure that value is
    // given as an option in the list of choices.
    else
    {
      boolean foundValue = false;

      for (int i=0; i < choices.length; i++)
      {
        if (choices[i].equals(value))
        {
          foundValue = true;
          break;
        }
      }

      if (! foundValue)
      {
        String[] tempChoices = new String[choices.length + 1];
        tempChoices[0] = value;
        System.arraycopy(choices, 0, tempChoices, 1, choices.length);
        this.choices = tempChoices;
      }
    }


    // Set the display values for this parameter.  If they are empty, null, or
    // have a different length than the choices, then just use the choices.
    this.displayValues = displayValues;
    if ((displayValues == null) ||
        (displayValues.length != this.choices.length))
    {
      this.displayValues = new String[this.choices.length];
      System.arraycopy(this.choices, 0, this.displayValues, 0,
                       this.choices.length);
    }
  }



  /**
   * Creates a new multichoice parameter with the specified information.  This
   * is to be used when decoding a multichoice parameter from an ASN.1 element.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use when displaying the parameter to the
   *                      end user.
   * @param  description  A description of this parameter.
   * @param  valueString  The value of this parameter represented as a string.
   */
  MultiChoiceParameter(String name, String displayName, String description,
                       String valueString)
  {
    super(name, displayName, description, true, valueString);
    choices       = new String[] { valueString };
    displayValues = new String[] { valueString };
  }



  /**
   * Retrieves the set of acceptable values for this parameter.
   *
   * @return  The set of acceptable values for this parameter.
   */
  public String[] getChoices()
  {
    return choices;
  }



  /**
   * Specify the set of acceptable values for this parameter.  Note that this
   * will overwrite any set of values that may be currently associated with this
   * parameter.
   *
   * @param  choices  The set of acceptable choices for this parameter.
   */
  public void setChoices(String[] choices)
  {
    if (choices == null)
    {
      this.choices = new String[0];
    }
    else
    {
      this.choices = choices;
    }
  }



  /**
   * Retrieves the set of display values to use for this parameter.  These will
   * be displayed to the end user in the drop-down list box.
   *
   * @return  The set of display values to use for this parameter.
   */
  public String[] getDisplayValues()
  {
    return displayValues;
  }



  /**
   * Specifies the set of display values to use for this parameter.  Note that
   * the elements in this list must always correspond to the order and number of
   * elements in the list of choices.
   *
   * @param  displayValues  The set of display values to use for this parameter.
   */
  public void setDisplayValues(String[] displayValues)
  {
    if (displayValues == null)
    {
      this.displayValues = new String[0];
    }
    else
    {
      this.displayValues = displayValues;
    }
  }



  /**
   * Adds the specified choice to the list of options.  It will be added to the
   * end of the current list.  If the specified choice already exists in the
   * list of options, then no action will be taken.
   *
   * @param  choice  The new choice to add to the list of options.
   */
  public void addChoice(String choice)
  {
    addChoice(choice, choice);
  }



  /**
   * Adds the specified choice to the list of options.  It will be added to the
   * end of the current list.  If the specified choice already exists in the
   * list of options, then no action will be taken.
   *
   * @param  choice        The new choice to add to the list of options.
   * @param  displayValue  The value to display for this choice in the drop-down
   *                       list box.
   */
  public void addChoice(String choice, String displayValue)
  {
    for (int i=0; i < choices.length; i++)
    {
      if (choices[i].equals(choice))
      {
        return;
      }
    }

    String[] tempChoices = new String[choices.length + 1];
    System.arraycopy(choices, 0, tempChoices, 0, choices.length);
    tempChoices[choices.length] = choice;
    choices = tempChoices;

    String[] tempDisplayValues = new String[displayValues.length + 1];
    System.arraycopy(displayValues, 0, tempDisplayValues, 0,
                     displayValues.length);
    tempDisplayValues[displayValues.length] = displayValue;
    displayValues = tempDisplayValues;
  }



  /**
   * Removes the specified choice from the list of options.  If the specified
   * choice is not an option, then no action will be taken.
   *
   * @param  choice  The choice to be removed from the list of options.
   */
  public void removeChoice(String choice)
  {
    for (int i=0; i < choices.length; i++)
    {
      if (choices[i].equals(choice))
      {
        String[] tempChoices = new String[choices.length - 1];

        for (int j=0; j < i; j++)
        {
          tempChoices[j] = choices[j];
        }

        for (int j=i+1; j < choices.length; j++)
        {
          tempChoices[j-1] = choices[i];
        }

        return;
      }
    }
  }



  /**
   * Retrieves the value of this parameter as a Java string.
   *
   * @return  the value of this parameter as a Java string.
   */
  public String getStringValue()
  {
    return (String) value;
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws  InvalidValueException  If the specified value is not in the list
   *                                 of choices.
   */
  @Override()
  public void setValue(Object value)
         throws InvalidValueException
  {
    String invalidReason = getInvalidReason(value);
    if (invalidReason == null)
    {
      this.value = value;
    }
    else
    {
      throw new InvalidValueException(invalidReason);
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
    if ((parameter != null) && (parameter instanceof MultiChoiceParameter))
    {
      MultiChoiceParameter mcp = (MultiChoiceParameter) parameter;
      this.value = mcp.value;
    }
  }



  /**
   * Retrieves a string representation of the value of this parameter.
   *
   * @return  A string representation of the value of this parameter.
   */
  @Override()
  public String getValueString()
  {
    if (value == null)
    {
      return "";
    }
    else
    {
      return (String) value;
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
    value = valueString;
  }



  /**
   * Retrieves the reason that the specified value is not valid.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return  The reason that the specified value is not valid, or
   *          <CODE>null</CODE> if it is valid.
   */
  @Override()
  public String getInvalidReason(Object value)
  {
    if (value == null)
    {
      return "No value provided for required parameter.";
    }


    if (value instanceof String)
    {
      String stringValue = (String) value;

      for (int i=0; i < choices.length; i++)
      {
        if (choices[i].equals(stringValue))
        {
          return null;
        }
      }

      return stringValue + " is not in the list of valid choices for " + name;
    }

    return value.getClass().getName() + " is not a valid object type for a " +
           "multichoice parameter";
  }



  /**
   * Retrieves the value of this parameter in a form that can be displayed to
   * the end user.
   *
   * @return  The value of this parameter in a form that can be displayed to
   *          the end user.
   */
  @Override()
  public String getDisplayValue()
  {
    return (String) value;
  }



  /**
   * Retrieves the text necessary to request the value of this Boolean parameter
   * from an HTML form.
   *
   * @param  prefix  The prefix that should be placed in front of the parameter
   *                 name as the name of the form element.
   *
   * @return  The text necessary to request the value of this Boolean parameter
   *          from an HTML form.
   */
  @Override()
  public String getHTMLInputForm(String prefix)
  {
    String returnStr = "<SELECT NAME=\"" + prefix + name + "\">\n";

    for (int i=0; i < choices.length; i++)
    {
      if (choices[i].equals(value))
      {
        returnStr += "  <OPTION SELECTED VALUE=\"" + choices[i] +
                     "\">" + displayValues[i] + '\n';
      }
      else
      {
        returnStr += "  <OPTION VALUE=\"" + choices[i] + "\">" +
                     displayValues[i] + '\n';
      }
    }

    returnStr += "</SELECT>";


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
      setValue(values[0]);
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
    String valueStr = (String) value;
    if (value == null)
    {
      valueStr = "";
    }

    return "<INPUT TYPE=\"HIDDEN\" NAME=\"" + prefix + name +
           "\" VALUE=\"" + valueStr + "\">";
  }



  /**
   * Creates a clone of this parameter.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public MultiChoiceParameter clone()
  {
    MultiChoiceParameter mcp =
         new MultiChoiceParameter(name, displayName, description, choices,
                                  displayValues, (String) value);
    mcp.setSensitive(isSensitive());

    return mcp;
  }
}

