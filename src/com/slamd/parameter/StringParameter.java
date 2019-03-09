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
 * This class defines a String parameter whose value is simple text.  The HTML
 * input mechanism for this parameter is an edit field.
 *
 *
 * @author   Neil A. Wilson
 */
public class StringParameter
       extends Parameter
{
  /**
   * The number of columns to display by default if no other value is specified.
   */
  private static final int DEFAULT_VISIBLE_COLUMNS = 40;



  // The number of columns to display in the HTML input form.
  private int visibleColumns = DEFAULT_VISIBLE_COLUMNS;

  // The Java String associated with this parameter.
  private String stringValue;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public StringParameter()
  {
    super();

    // No additional implementation required.
  }



  /**
   * Creates a new String parameter with the specified name.  The display name
   * will be the same as the name, it will not have a description, it will not
   * have a value, and it will not be required.
   *
   * @param  name  The name to use for this parameter.
   */
  public StringParameter(String name)
  {
    this(name, name, null, false, "");
  }



  /**
   * Creates a new String parameter with the specified name and value.  The
   * display name will be the same as the name, it will not have a description,
   * and it will not be required.
   *
   * @param  name   The name to use for this parameter.
   * @param  value  The value to use for this parameter.
   */
  public StringParameter(String name, String value)
  {
    this(name, name, null, false, value);
  }



  /**
   * Creates a new String parameter with the specified name and
   * required/optional indicator.  The display name will be the same as the
   * name, and it will not have a description or value.
   *
   * @param  name        The name to use for this parameter.
   * @param  isRequired  Indicates whether this parameter is required to have a
   *                     value.
   */
  public StringParameter(String name, boolean isRequired)
  {
    this(name, name, null, isRequired, "");
  }



  /**
   * Creates a new String parameter with the specified name, value, and
   * required/optional indicator.  The display name will be the same as the name
   * and it will not have a description.
   *
   * @param  name        The name to use for this parameter.
   * @param  isRequired  Indicates whether this parameter is required to have a
   *                     value.
   * @param  value       The value to use for this parameter.
   */
  public StringParameter(String name, boolean isRequired, String value)
  {
    this(name, name, null, isRequired, value);
  }



  /**
   * Creates a new String parameter with the specified name, display name,
   * required/optional indicator, and value.  It will not have a description.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter if it is to be
   *                      displayed to the end user.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   * @param  value        The value to use for this parameter.
   */
  public StringParameter(String name, String displayName, boolean isRequired,
                         String value)
  {
    this(name, displayName, null, isRequired, value);
  }



  /**
   * Creates a new String parameter with the specified name, display name,
   * description, required/optional indicator, and value.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter if it is to be
   *                      displayed to the end user.
   * @param  description  The description to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   * @param  value        The value to use for this parameter.
   */
  public StringParameter(String name, String displayName, String description,
                         boolean isRequired, String value)
  {
    super(name, displayName, description, isRequired, value);

    if (value == null)
    {
      stringValue = "";
    }
    else
    {
      stringValue = value;
    }
  }



  /**
   * Retrieves the value of this parameter as a Java String.
   *
   * @return  The value of this parameter as a Java String.
   */
  public String getStringValue()
  {
    return stringValue;
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
    stringValue = valueString;
    value = valueString;
  }



  /**
   * Retrieves the value of this parameter as a Java String.
   *
   * @return  The value of this parameter as a Java String.
   */
  @Override()
  public String getValue()
  {
    return stringValue;
  }



  /**
   * Indicates whether this parameter has a value.
   *
   * @return  <CODE>true</CODE> if it does have a value, or <CODE>false</CODE>
   *          if it does not.
   */
  @Override()
  public boolean hasValue()
  {
    return ((stringValue != null) && (stringValue.length() > 0));
  }



  /**
   * Specifies the value to use for this string parameter.  Only values of type
   * string will be accepted.
   *
   * @param  value  The value to use for this string parameter.
   *
   * @throws  InvalidValueException  If the provided object is not a string, or
   *                                 it is a required parameter but no value was
   *                                 specified.
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
    stringValue = (String) value;
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
    if ((parameter != null) && (parameter instanceof StringParameter))
    {
      StringParameter sp = (StringParameter) parameter;
      this.value       = sp.value;
      this.stringValue = sp.stringValue;
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
      return stringValue;
    }
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
      if (isRequired)
      {
        return "No value provided for required parameter";
      }
      else
      {
        return null;
      }
    }

    if (value instanceof String)
    {
      if ((String.valueOf(value).length() == 0) && isRequired)
      {
        return "No value provided for required parameter";
      }

      return null;
    }
    else
    {
      return (value.getClass().getName() + " is not a supported " +
              "String object type");
    }
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
    return stringValue;
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
    String returnStr = "<INPUT TYPE=\"TEXT\" NAME=\"" + prefix + name +
                       "\" VALUE=\"" + stringValue +
                       "\" SIZE=\"" + visibleColumns + "\">";
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
    String valueStr = stringValue;
    if (stringValue == null)
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
  public StringParameter clone()
  {
    StringParameter sp = new StringParameter(name, displayName, description,
                                             isRequired, stringValue);
    sp.visibleColumns = visibleColumns;
    sp.setSensitive(isSensitive());
    return sp;
  }
}

