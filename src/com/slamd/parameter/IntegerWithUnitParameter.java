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
 * This class defines a parameter that consists of two parts -- an integer value
 * and a string value.  The string value will be presented in a drop-down list
 * form that allows the user to choose an appropriate set of units to use for
 * the integer value.
 *
 *
 * @author   Neil A. Wilson
 */
public class IntegerWithUnitParameter
       extends Parameter
{
  /**
   * The number of columns to display by default if no other value is specified.
   */
  private static final int DEFAULT_VISIBLE_COLUMNS = 10;



  // Indicates whether a lower bound should be enforced on this parameter
  private boolean hasLowerBound;

  // Indicates whether an upper bound should be enforced on this parameter
  private boolean hasUpperBound;

  // The value associated with this parameter as a Java integer
  private int intValue;

  // The lower bound that will be enforced if hasLowerBound is true
  private int lowerBound;

  // The upper bound that will be enforced if hasUpperBound is true
  private int upperBound;

  // The number of columns to display in the HTML input form.
  private int visibleColumns = DEFAULT_VISIBLE_COLUMNS;

  // The choice that is currently selected.
  private String selectedChoice;

  // The set of choices that are available for use with this parameter.
  private String[] choices;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public IntegerWithUnitParameter()
  {
    super();

    // Indicate that there are no bounds
    hasLowerBound = false;
    hasUpperBound = false;
  }



  /**
   * Creates a new integer with unit parameter with the specified name, value
   * required/optional indicator, and set of choices.  The display name will be
   * the same as the name and it will not have a description.  Upper and lower
   * bounds will not be enforced.
   *
   * @param  name     The name to use for this parameter.
   * @param  value    The value to use for this parameter.
   * @param  choices  The set of choices for this parameter.
   */
  public IntegerWithUnitParameter(String name, int value, String[] choices)
  {
    this(name, name, null, value, choices, choices[0]);
  }



  /**
   * Creates a new integer with unit parameter with the specified name,
   * display name, value, required/optional indicator, and set of choices.
   * It will not have a description, and upper and lower bounds will not be
   * enforced.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter when it is
   *                      displayed to the end user.
   * @param  value        The value to use for this parameter.
   * @param  choices      The set of choices for this parameter.
   */
  public IntegerWithUnitParameter(String name, String displayName,
                                  int value, String[] choices)
  {
    this(name, displayName, null, value, choices, choices[0]);
  }



  /**
   * Creates a new integer with unit parameter with the specified name, display
   * name, description, value, required/optional indicator, and choices.  Upper
   * and lower bounds will not be enforced.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter when it is
   *                      displayed to the end user.
   * @param  description  The description to use for this parameter.
   * @param  value        The value to use for this parameter.
   * @param  choices      The set of choices for this parameter.
   */
  public IntegerWithUnitParameter(String name, String displayName,
                                  String description, int value,
                                  String[] choices)
  {
    this(name, displayName, description, value, choices, choices[0]);
  }



  /**
   * Creates a new integer with unit parameter with the specified name, display
   * name, description, value, required/optional indicator, and choices.  Upper
   * and lower bounds will not be enforced.  This is a version of the
   * constructor intended for internal use only to allow for either null or
   * integer values.
   *
   * @param  name            The name to use for this parameter.
   * @param  displayName     The name to use for this parameter when it is
   *                         displayed to the end user.
   * @param  description     The description to use for this parameter.
   * @param  value           The value to use for this parameter.
   * @param  choices         The set of choices for this parameter.
   * @param  selectedChoice  The choice that should be selected by default.
   */
  private IntegerWithUnitParameter(String name, String displayName,
                                   String description, int value,
                                   String[] choices, String selectedChoice)
  {
    super(name, displayName, description, true, value + " " + selectedChoice);
    this.intValue       = value;
    this.choices        = choices;
    this.selectedChoice = selectedChoice;
    hasLowerBound       = false;
    lowerBound          = 0;
    hasUpperBound       = false;
    upperBound          = 0;
  }



  /**
   * Creates a new integer with unit parameter with the specified name, display
   * name, description, value, required/optional indicator, and choices as well
   * as information pertaining to upper and lower bounds.
   *
   * @param  name            The name to use for this parameter.
   * @param  displayName     The name to use for this parameter when it is
   *                         displayed to the end user.
   * @param  description     The description to use for this parameter.
   * @param  value           The value to use for this parameter.
   * @param  hasLowerBound   Indicates whether the lower bound will be enforced
   *                         for this parameter.
   * @param  lowerBound      The lower bound to use for this parameter (will
   *                         only be effective if hasLowerBound is true).
   * @param  hasUpperBound   Indicates whether the upper bound will be enforced
   *                         for this parameter.
   * @param  upperBound      The upper bound to use for this parameter (will
   *                         only be effective if hasUpperBound is true).
   * @param  choices         The set of choices for this parameter.
   * @param  selectedChoice  The choice that should be selected by default.
   */
  public IntegerWithUnitParameter(String name, String displayName,
                                  String description, int value,
                                  boolean hasLowerBound, int lowerBound,
                                  boolean hasUpperBound, int upperBound,
                                  String[] choices, String selectedChoice)
  {
    this(name, displayName, description, value, choices, selectedChoice);

    this.hasLowerBound  = hasLowerBound;
    this.lowerBound     = lowerBound;
    this.hasUpperBound  = hasUpperBound;
    this.upperBound     = upperBound;
  }



  /**
   * Retrieves the value associated with this parameter as a Java integer.  Note
   * that you should check to make sure that this parameter actually has a value
   * before calling this method, because it is possible to have an integer
   * parameter whose value is not required.  If this parameter does not have a
   * value, then the value returned from this function is unreliable.
   *
   * @return  The value associated with this parameter as a Java integer.
   */
  public int getIntValue()
  {
    return intValue;
  }



  /**
   * Retrieves the value that has been selected from the drop-down list.
   *
   * @return  The value that has been selected from the drop-down list.
   */
  public String getSelectedChoice()
  {
    return selectedChoice;
  }



  /**
   * Specifies the choice that has been selected from the drop-down list.
   *
   * @param  selectedChoice  The choice that has been selected from the
   *                         drop-down list.
   *
   * @throws  InvalidValueException  If the specified choice is not a valid
   *                                 option.
   */
  public void setSelectedChoice(String selectedChoice)
         throws InvalidValueException
  {
    if (choices == null)
    {
      this.selectedChoice = selectedChoice;
      return;
    }

    for (int i=0; i < choices.length; i++)
    {
      if (selectedChoice.equals(choices[i]))
      {
        this.selectedChoice = selectedChoice;
        return;
      }
    }

    throw new InvalidValueException('"' + selectedChoice +
                                    "\" is not a valid choice");
  }



  /**
   * Retrieves the value associated with this parameter.  If it does not have a
   * value, then <CODE>null</CODE> will be returned.  If it does have a value,
   * then that value will be returned as a Java Integer object.
   *
   * @return  The value associated with this parameter.
   */
  @Override()
  public Object getValue()
  {
    return value;
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value           The value to use for this parameter.
   * @param  selectedChoice  The selected choice indicating the unit for the
   *                         value.
   *
   * @throws  InvalidValueException  If the specified value does not meet the
   *                                 upper or lower bounds requirements (if they
   *                                 are to be enforced).
   */
  public void setValue(int value, String selectedChoice)
         throws InvalidValueException
  {
    if (hasUpperBound && (value > upperBound))
    {
      throw new InvalidValueException("Specified value of " + value +
                                      " is above upper bound of " + upperBound);
    }
    else if (hasLowerBound && (value < lowerBound))
    {
      throw new InvalidValueException("Specified value of " + value +
                                      " is below lower bound of " + lowerBound);
    }
    else if (choices != null)
    {
      boolean choiceOK = false;
      for (int i=0; i < choices.length; i++)
      {
        if (selectedChoice.equals(choices[i]))
        {
          choiceOK = true;
        }
      }

      if (! choiceOK)
      {
        throw new InvalidValueException("Specified selected choice of " +
                                        selectedChoice +
                                        " is not a valid option.");
      }
    }

    this.intValue       = value;
    this.selectedChoice = selectedChoice;
    this.value          = intValue + " " + selectedChoice;
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws InvalidValueException  If the specified value cannot be interpreted
   *                                as a Java integer, or if it does not meet
   *                                the upper or lower bounds requirements (if
   *                                they are to be enforced).
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

    if (! (value instanceof String))
    {
      throw new InvalidValueException("Only string values consisting of an " +
                                      "integer followed by a space and the " +
                                      "selected choice are acceptable.");
    }

    try
    {
      String stringValue = ((String) value).trim();
      int    spacePos    = stringValue.indexOf(' ');
      int    intValue    = Integer.parseInt(stringValue.substring(0, spacePos));
      String choice      = stringValue.substring(spacePos+1).trim();
      setValue(intValue, choice);
    }
    catch (InvalidValueException ive)
    {
      throw ive;
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Only string values consisting of an " +
                                      "integer followed by a space and the " +
                                      "selected choice are acceptable.", e);
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
    if ((parameter != null) && (parameter instanceof IntegerWithUnitParameter))
    {
      IntegerWithUnitParameter iwp = (IntegerWithUnitParameter) parameter;

      this.value          = iwp.value;
      this.intValue       = iwp.intValue;
      this.hasLowerBound  = iwp.hasLowerBound;
      this.lowerBound     = iwp.lowerBound;
      this.hasUpperBound  = iwp.hasUpperBound;
      this.upperBound     = iwp.upperBound;
      this.choices        = iwp.choices;
      this.selectedChoice = iwp.selectedChoice;
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
    try
    {
      int    spacePos    = valueString.indexOf(' ');
      int    intValue    = Integer.parseInt(valueString.substring(0, spacePos));
      String choice      = valueString.substring(spacePos+1).trim();

      setValue(intValue, choice);
    }
    catch (InvalidValueException ive)
    {
      throw ive;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new InvalidValueException("Unable to set value:  " + e, e);
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
    return (String) value;
  }



  /**
   * Retrieves the lower bound that will be used if a lower bound is to be
   * enforced.  If the lower bound is not to be enforced, then the value
   * returned from this method will be meaningless.
   *
   * @return  The lower bound that will be used if a lower bound is to be
   *          enforced.
   */
  public int getLowerBound()
  {
    return lowerBound;
  }



  /**
   * Specifies the value to use as the lower bound, and indicates that a lower
   * bound is to be enforced for this parameter.
   *
   * @param  lowerBound  The value to use as the lower bound.
   */
  public void setLowerBound(int lowerBound)
  {
    this.lowerBound = lowerBound;
    hasLowerBound   = true;
  }



  /**
   * Specifies that a lower bound should not be enforced for this parameter.
   */
  public void unsetLowerBound()
  {
    hasLowerBound = false;
  }



  /**
   * Indicates whether a lower bound will be enforced for this parameter.
   *
   * @return  <CODE>true</CODE> if a lower bound will be enforced, or
   *          <CODE>false</CODE> if not.
   */
  public boolean hasLowerBound()
  {
    return hasLowerBound;
  }



  /**
   * Retrieves the upper bound that will be used if an upper bound is to be
   * enforced.  If the upper bound is not to be enforced, then the value
   * returned from this method will be meaningless.
   *
   * @return  The lower bound that will be used if a lower bound is to be
   *          enforced.
   */
  public int getUpperBound()
  {
    return upperBound;
  }



  /**
   * Specifies the value to use as the upper bound, and indicates that an upper
   * bound is to be enforced for this parameter.
   *
   * @param  upperBound  The value to use as the upper bound.
   */
  public void setUpperBound(int upperBound)
  {
    this.upperBound = upperBound;
    hasUpperBound   = true;
  }



  /**
   * Specifies that an upper bound should not be enforced for this parameter.
   */
  public void unsetUpperBound()
  {
    hasUpperBound = false;
  }



  /**
   * Indicates whether an upper bound will be enforced for this parameter.
   *
   * @return  <CODE>true</CODE> if an upper bound will be enforced, or
   *          <CODE>false</CODE> if not.
   */
  public boolean hasUpperBound()
  {
    return hasUpperBound;
  }



  /**
   * Retrieves the list of choices available for the unit drop-down list.
   *
   * @return  The list of choices available for the unit drop-down list.
   */
  public String[] getChoices()
  {
    return choices;
  }



  /**
   * Specifies the list of choices available for the unit drop-down list.  If
   * the currently selected choice is not in the new list, then the selected
   * choice will be changed so that it is the first value of the new list.
   *
   * @param  choices  The list of choices available for the unit drop-down list.
   */
  public void setChoices(String[] choices)
  {
    this.choices = choices;

    for (int i=0; i < choices.length; i++)
    {
      if (choices[i].equals(selectedChoice))
      {
        return;
      }
    }

    selectedChoice = choices[0];
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
      return "No value specified for required parameter";
    }

    if (! (value instanceof String))
    {
      return "Value must be a string with an integer followed by the " +
             "selected choice";
    }

    String stringValue = (String) value;
    int spacePos = stringValue.indexOf(' ');
    if (spacePos <= 0)
    {
      return "Value must be a string with an integer followed by the " +
             "selected choice";
    }


    try
    {
      int intValue = Integer.parseInt(stringValue.substring(0, spacePos));
      if (hasUpperBound && (intValue > upperBound))
      {
        return "Integer portion must be below upper bound of " + upperBound;
      }
      else if (hasLowerBound && (intValue < lowerBound))
      {
        return "Integer portion must be above lower bound of " + lowerBound;
      }
    }
    catch (NumberFormatException nfe)
    {
      return "Value must be a string with an integer followed by the " +
             "selected choice";
    }

    String selectedChoice = stringValue.substring(spacePos+1);
    if (choices == null)
    {
      return null;
    }

    for (int i=0; i < choices.length; i++)
    {
      if (selectedChoice.equals(choices[i]))
      {
        return null;
      }
    }

    return "Selected choice \"" + selectedChoice + "\" is not a valid option.";
  }



  /**
   * Retrieves a String that can be used when displaying the value of this
   * parameter to the end user.
   *
   * @return  A String that can be used when displaying the value of this
   *          parameter to the end user.
   */
  @Override()
  public String getDisplayValue()
  {
    return intValue + " " + selectedChoice;
  }



  /**
   * Retrieves a String that can be used when displaying the value of this
   * parameter to the end user in the context of an HTML page.
   *
   * @return  A String that can be used when displaying the value of this
   *          parameter to the end user in the context of an HTML page.
   */
  @Override()
  public String getHTMLDisplayValue()
  {
    return getDisplayValue();
  }



  /**
   * Retrieves the number of columns that should be visible in the HTML input
   * form.
   *
   * @return  The number of columns that should be visible in the HTML input
   *          form.
   */
  public int getVisibleColumns()
  {
    return visibleColumns;
  }



  /**
   * Specifies the number of columns that should be visible in the HTML input
   * form.
   *
   * @param  visibleColumns  The number of columns that should be visible in the
   *                         HTML input form.
   */
  public void setVisibleColumns(int visibleColumns)
  {
    this.visibleColumns = visibleColumns;
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
    String returnStr = "<INPUT TYPE=\"TEXT\" NAME=\"" + prefix + name +
                       "\" VALUE=\"" + intValue + "\" SIZE=\"" +
                       visibleColumns + "\">" + Constants.EOL;
    returnStr += "<SELECT NAME=\"" + prefix + name + "\">" + Constants.EOL;
    for (int i=0; i < choices.length; i++)
    {
      if (selectedChoice.equals(choices[i]))
      {
        returnStr += "  <OPTION SELECTED VALUE=\"" + choices[i] + "\">" +
                     choices[i] + Constants.EOL;
      }
      else
      {
        returnStr += "  <OPTION VALUE=\"" + choices[i] + "\">" + choices[i] +
                     Constants.EOL;
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
    if (values.length != 2)
    {
      throw new InvalidValueException("Both an integer and a string portion " +
                                      "are required for the " + displayName +
                                      " parameter.");
    }

    try
    {
      int intValue = Integer.parseInt(values[0]);
      if (hasUpperBound && (intValue > upperBound))
      {
        throw new InvalidValueException("Integer value must be less than " +
                                        "upper bound of " + upperBound +
                                        " for parameter " + displayName);
      }
      if (hasLowerBound && (intValue < lowerBound))
      {
        throw new InvalidValueException("Integer value must be greater than " +
                                        "lower bound of " + lowerBound +
                                        " for parameter " + displayName);
      }

      String  selectedChoice = values[1];
      boolean found          = false;
      for (int i=0; i < choices.length; i++)
      {
        if (selectedChoice.equals(choices[i]))
        {
          found = true;
          break;
        }
      }

      if (! found)
      {
        throw new InvalidValueException("String value \"" + selectedChoice +
                                        "\" is not a valid option for " +
                                        "parameter " + displayName);
      }

      this.intValue       = intValue;
      this.selectedChoice = selectedChoice;
      this.value          = intValue + " " + selectedChoice;
    }
    catch (NumberFormatException nfe)
    {
      try
      {
        int intValue = Integer.parseInt(values[1]);
        if (hasUpperBound && (intValue > upperBound))
        {
          throw new InvalidValueException("Integer value must be less than " +
                                          "upper bound of " + upperBound +
                                          " for parameter " + displayName, nfe);
        }
        if (hasLowerBound && (intValue < lowerBound))
        {
          throw new InvalidValueException("Integer value must be greater " +
                                          "than lower bound of " + lowerBound +
                                          " for parameter " + displayName, nfe);
        }

        String  selectedChoice = values[0];
        boolean found          = false;
        for (int i=0; i < choices.length; i++)
        {
          if (selectedChoice.equals(choices[i]))
          {
            found = true;
            break;
          }
        }

        if (! found)
        {
          throw new InvalidValueException("String value \"" + selectedChoice +
                                          "\" is not a valid option for " +
                                          "parameter " + displayName);
        }

        this.intValue       = intValue;
        this.selectedChoice = selectedChoice;
        this.value          = intValue + " " + selectedChoice;
      }
      catch (NumberFormatException nfe2)
      {
        throw new InvalidValueException("Unable to parse either \"" +
                                        values[0] + "\" or \"" + values[1] +
                                        "\" as an integer for parameter " +
                                        displayName, nfe2);
      }
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
    if (value == null)
    {
      return "";
    }

    return "<INPUT TYPE=\"HIDDEN\" NAME=\"" + prefix + name +
           "\" VALUE=\"" + intValue + "\"><INPUT TYPE=\"HIDDEN\" NAME=\"" +
           prefix + name + "\" VALUE=\"" + selectedChoice + "\">";
  }



  /**
   * Creates a clone of this parameter.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public IntegerWithUnitParameter clone()
  {
    IntegerWithUnitParameter iwp =
         new IntegerWithUnitParameter(name, displayName, description, intValue,
                                      hasLowerBound, lowerBound, hasUpperBound,
                                      upperBound, choices, selectedChoice);
    iwp.visibleColumns = visibleColumns;
    iwp.setSensitive(isSensitive());
    return iwp;
  }
}

