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
 * This class defines a Boolean parameter that can have a value of either "true"
 * or "false".  The HTML representation of this parameter type is a checkbox.
 *
 *
 * @author   Neil A. Wilson
 */
public class BooleanParameter
       extends Parameter
{
  // The Java boolean associated with the value of this parameter
  private boolean booleanValue;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public BooleanParameter()
  {
    super();

    // No additional implementation required.
  }



  /**
   * Creates a new Boolean parameter with the specified name and boolean value.
   * The display name will be the same as the name, and it will not have a
   * description.
   *
   * @param  name          The name to use for this parameter.
   * @param  booleanValue  The Java boolean to use for the value of this
   *                       parameter.
   */
  public BooleanParameter(String name, boolean booleanValue)
  {
    super(name, name, null, false, booleanValue);
    this.booleanValue = booleanValue;
  }



  /**
   * Creates a new Boolean parameter with the specified name, display name, and
   * boolean value.  It will not have a description.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The name to use when displaying the parameter to the
   *                       end user.
   * @param  booleanValue  The Java boolean to use for the value of this
   *                       parameter.
   */
  public BooleanParameter(String name, String displayName, boolean booleanValue)
  {
    super(name, displayName, null, false, booleanValue);
    this.booleanValue = booleanValue;
  }



  /**
   * Creates a new Boolean parameter with the specified name, display name,
   * description, and boolean value.
   *
   * @param  name          The name to use for this parameter.
   * @param  displayName   The name to use when displaying the parameter to the
   *                       end user.
   * @param  description   A description of this parameter.
   * @param  booleanValue  The Java boolean to use for the value of this
   *                       parameter.
   */
  public BooleanParameter(String name, String displayName, String description,
                          boolean booleanValue)
  {
    super(name, displayName, description, false, booleanValue);
    this.booleanValue = booleanValue;
  }



  /**
   * Creates a new Boolean parameter with the specified information.  This is
   * to be used when decoding a Boolean parameter from an ASN.1 element.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use when displaying the parameter to the
   *                      end user.
   * @param  description  A description of this parameter.
   * @param  valueString  The value of this parameter represented as a string.
   */
  BooleanParameter(String name, String displayName, String description,
                   String valueString)
  {
    super(name, displayName, description, false, null);

    if (valueString.equalsIgnoreCase("false"))
    {
      booleanValue = false;
      Boolean.valueOf(false);
    }
    else
    {
      booleanValue = true;
      value = true;
    }
  }



  /**
   * Retrieves the value of this parameter as a Java boolean primitive.
   *
   * @return  The value of this parameter as a Java boolean primitive.
   */
  public boolean getBooleanValue()
  {
    return booleanValue;
  }



  /**
   * Retrieves the value of this parameter as a Java Boolean object.
   *
   * @return  The value of this parameter as a Java Boolean object.
   */
  @Override()
  public Boolean getValue()
  {
    return booleanValue;
  }



  /**
   * Specifies the value to use for this Boolean parameter.
   *
   * @param  booleanValue  The value to use for this Boolean parameter.
   */
  public void setValue(boolean booleanValue)
  {
    this.booleanValue = booleanValue;
    this.value = booleanValue;
  }



  /**
   * Specifies the value to use for this Boolean parameter.  Only values of type
   * Boolean and String will be accepted, and String values will only be
   * accepted if the value is "true" or "false" (case insensitive).
   *
   * @param  value  The value to use for this Boolean parameter.
   *
   * @throws  InvalidValueException  If the provided object is not a Boolean or
   *                                 a String, or if it is a String but the text
   *                                 is not "true" or "false".
   */
  @Override()
  public void setValue(Object value)
         throws InvalidValueException
  {
    if (value instanceof Boolean)
    {
      setValue(((Boolean) value).booleanValue());
    }
    else if (value instanceof String)
    {
      String valueStr = ((String) value).toLowerCase();
      if (valueStr.equals("true"))
      {
        setValue(true);
      }
      else if (valueStr.equals("false"))
      {
        setValue(false);
      }
      else
      {
        throw new InvalidValueException(getInvalidReason(value));
      }
    }
    else
    {
      throw new InvalidValueException(getInvalidReason(value));
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
    if ((parameter != null) && (parameter instanceof BooleanParameter))
    {
      BooleanParameter bp = (BooleanParameter) parameter;
      this.value        = bp.value;
      this.booleanValue = bp.booleanValue;
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
    return (booleanValue ? "true" : "false");
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
    setValue(valueString);
  }



  /**
   * Indicates whether this parameter has a value.  In this case, it will always
   * return true, because a Boolean parameter will always have a value.
   *
   * @return  <CODE>true</CODE>, because Boolean parameters always have a value.
   */
  @Override()
  public boolean hasValue()
  {
    return true;
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
    if (value instanceof Boolean)
    {
      return null;
    }
    else if (value instanceof String)
    {
      String valueStr = ((String) value).toLowerCase();
      if (valueStr.equals("true") || (valueStr.equals("false")))
      {
        return null;
      }

      return ('"' + String.valueOf(value) + "\" is not a valid Boolean value");
    }
    else
    {
      return (value.getClass().getName() + " is not a supported " +
              "Boolean object type");
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
    return (booleanValue ? "true" : "false");
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
    String returnStr = "<INPUT TYPE=\"CHECKBOX\" NAME=\"" + prefix + name +
                       '"' + (booleanValue ? " CHECKED>" : ">");
    return returnStr;
  }



  /**
   * Specifies the value of this parameter based on the provided text that would
   * be returned from posting an HTML form.
   *
   * @param  values  The set of values for this parameter contained in the
   *                 servlet request.
   */
  @Override()
  public void htmlInputFormToValue(String[] values)
  {
    if ((values == null) || (values.length == 0) || values[0].equals("0") ||
        values[0].equalsIgnoreCase("false") ||
        values[0].equalsIgnoreCase("off"))
    {
      setValue(false);
    }
    else
    {
      setValue(true);
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
    if (booleanValue)
    {
      return "<INPUT TYPE=\"HIDDEN\" NAME=\"" + prefix + name +
             "\" VALUE=\"ON\">";
    }
    else
    {
      return "";
    }
  }



  /**
   * Retrieves the value of this parameter as it would be submitted by a browser
   * posting the request.
   *
   * @return  The value of this parameter as it would be submitted by a browser
   *          posting the request, or <CODE>null</CODE> if this parameter should
   *          not be included in the post.
   */
  @Override()
  public String getHTMLPostValue()
  {
    if (booleanValue)
    {
      return "on";
    }
    else
    {
      return null;
    }
  }



  /**
   * Creates a clone of this parameter.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public BooleanParameter clone()
  {
    BooleanParameter bp =
         new BooleanParameter(name, displayName, description, booleanValue);
    bp.setSensitive(isSensitive());
    return bp;
  }
}

