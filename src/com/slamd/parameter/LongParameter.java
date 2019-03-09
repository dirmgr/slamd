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
 * This class defines a parameter whose value is a 64-bit mathematical integer
 * (no decimal values) that supports a much wider range of values than the
 * standard integer parameter type.  It can also support upper and lower bounds,
 * although they are not required to be enforced.
 *
 *
 * @author   Neil A. Wilson
 */
public class LongParameter
       extends Parameter
{
  /**
   * The number of columns to display by default if no other value is specified.
   */
  private static final int DEFAULT_VISIBLE_COLUMNS = 40;



  // Indicates whether a lower bound should be enforced on this parameter
  private boolean hasLowerBound;

  // Indicates whether an upper bound should be enforced on this parameter
  private boolean hasUpperBound;

  // The number of columns to display in the HTML input form.
  private int visibleColumns = DEFAULT_VISIBLE_COLUMNS;

  // The value associated with this parameter as a Java long
  private long longValue;

  // The lower bound that will be enforced if hasLowerBound is true
  private long lowerBound;

  // The upper bound that will be enforced if hasUpperBound is true
  private long upperBound;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public LongParameter()
  {
    super();

    // Indicate that there are no bounds
    hasLowerBound = false;
    hasUpperBound = false;
  }



  /**
   * Creates a new long parameter with the specified name.  The display name
   * will be the same as the name, it will not have a description or a value,
   * and it will not be required to have a value.  Upper and lower bounds will
   * not be enforced.
   *
   * @param  name  The name to use for this parameter.
   */
  public LongParameter(String name)
  {
    this(name, name, null, false, null);
  }



  /**
   * Creates a new long parameter with the specified name and value.  The
   * display name will be the same as the name, and it will not have a
   * description.  It will not be required, nor will upper or lower bounds be
   * enforced.
   *
   * @param  name   The name to use for this parameter.
   * @param  value  The value to use for this parameter.
   */
  public LongParameter(String name, long value)
  {
    this(name, name, null, false, Long.valueOf(value));
  }



  /**
   * Creates a new long parameter with the specified name, value, and
   * required/optional indicator.  The display name will be the same as the name
   * and it will not have a description.  Upper and lower bounds will not be
   * enforced.
   *
   * @param  name        The name to use for this parameter.
   * @param  isRequired  Indicates whether this parameter is required to have a
   *                     value.
   * @param  value       The value to use for this parameter.
   */
  public LongParameter(String name, boolean isRequired, long value)
  {
    this(name, name, null, isRequired, Long.valueOf(value));
  }



  /**
   * Creates a new long parameter with the specified name, display name,
   * value, and required/optional indicator.  It will not have a description,
   * and upper and lower bounds will not be enforced.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter when it is
   *                      displayed to the end user.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   * @param  value        The value to use for this parameter.
   */
  public LongParameter(String name, String displayName, boolean isRequired,
                       long value)
  {
    this(name, displayName, null, isRequired, Long.valueOf(value));
  }



  /**
   * Creates a new long parameter with the specified name, display name,
   * description, value, and required/optional indicator.  Upper and lower
   * bounds will not be enforced.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter when it is
   *                      displayed to the end user.
   * @param  description  The description to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   * @param  value        The value to use for this parameter.
   */
  public LongParameter(String name, String displayName, String description,
                       boolean isRequired, long value)
  {
    this(name, displayName, description, isRequired, Long.valueOf(value));
  }



  /**
   * Creates a new long parameter with the specified name, display name,
   * description, value, and required/optional indicator.  Upper and lower
   * bounds will not be enforced.  This is a version of the constructor intended
   * for internal use only to allow for either null or long values.
   *
   * @param  name         The name to use for this parameter.
   * @param  displayName  The name to use for this parameter when it is
   *                      displayed to the end user.
   * @param  description  The description to use for this parameter.
   * @param  isRequired   Indicates whether this parameter is required to have a
   *                      value.
   * @param  value        The value to use for this parameter.
   */
  private LongParameter(String name, String displayName, String description,
                        boolean isRequired, Long value)
  {
    super(name, displayName, description, isRequired, value);
    hasLowerBound  = false;
    lowerBound     = 0;
    hasUpperBound  = false;
    upperBound     = 0;


    if (value == null)
    {
      longValue = 0;
    }
    else
    {
      longValue = value;
    }
  }



  /**
   * Creates a new long parameter with the specified name, display name,
   * description, value, and required/optional indicator, as well as information
   * pertaining to upper and lower bounds.
   *
   * @param  name           The name to use for this parameter.
   * @param  displayName    The name to use for this parameter when it is
   *                        displayed to the end user.
   * @param  description    The description to use for this parameter.
   * @param  isRequired     Indicates whether this parameter is required to have
   *                        a value.
   * @param  value          The value to use for this parameter.
   * @param  hasLowerBound  Indicates whether the lower bound will be enforced
   *                        for this parameter.
   * @param  lowerBound     The lower bound to use for this parameter (will only
   *                        be effective if hasLowerBound is true).
   * @param  hasUpperBound  Indicates whether the upper bound will be enforced
   *                        for this parameter.
   * @param  upperBound     The upper bound to use for this parameter (will only
   *                        be effective if hasUpperBound is true).
   */
  public LongParameter(String name, String displayName, String description,
                       boolean isRequired, long value, boolean hasLowerBound,
                       long lowerBound, boolean hasUpperBound, long upperBound)
  {
    this(name, displayName, description, isRequired, Long.valueOf(value));

    this.hasLowerBound  = hasLowerBound;
    this.lowerBound     = lowerBound;
    this.hasUpperBound  = hasUpperBound;
    this.upperBound     = upperBound;
  }



  /**
   * Retrieves the value associated with this parameter as a Java long.  Note
   * that you should check to make sure that this parameter actually has a value
   * before calling this method, because it is possible to have a long parameter
   * whose value is not required.  If this parameter does not have a value, then
   * the value returned from this function is unreliable.
   *
   * @return  The value associated with this parameter as a Java long.
   */
  public long getLongValue()
  {
    return longValue;
  }



  /**
   * Retrieves the value associated with this parameter.  If it does not have a
   * value, then <CODE>null</CODE> will be returned.  If it does have a value,
   * then that value will be returned as a Java Long object.
   *
   * @return  The value associated with this parameter.
   */
  @Override()
  public Long getValue()
  {
    if (value == null)
    {
      return null;
    }
    else
    {
      return longValue;
    }
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws  InvalidValueException  If the specified value does not meet the
   *                                 upper or lower bounds requirements (if they
   *                                 are to be enforced).
   */
  public void setValue(long value)
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
    else
    {
      this.value     = value;
      this.longValue = value;
    }
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws InvalidValueException  If the specified value cannot be interpreted
   *                                as a Java long, or if it does not meet
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


    if (value == null)
    {
      return;
    }
    else if (value instanceof Long)
    {
      setValue(((Long) value).longValue());
    }
    else if (value instanceof String)
    {
      String valueStr = (String) value;

      if (valueStr.length() == 0)
      {
        return;
      }

      try
      {
        long longValue = Long.parseLong(valueStr);
        setValue(longValue);
      }
      catch (NumberFormatException nfe)
      {
        throw new InvalidValueException('"' + valueStr +
                                         "\" is not a valid long value",
                                        nfe);
      }
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
    if ((parameter != null) && (parameter instanceof LongParameter))
    {
      LongParameter lp   = (LongParameter) parameter;
      this.value         = lp.value;
      this.longValue     = lp.longValue;
      this.hasLowerBound = lp.hasLowerBound;
      this.lowerBound    = lp.lowerBound;
      this.hasUpperBound = lp.hasUpperBound;
      this.upperBound    = lp.upperBound;
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
      longValue = Long.parseLong(valueString);
      value = longValue;
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Unable to set long value:  " + e, e);
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
      return String.valueOf(longValue);
    }
  }



  /**
   * Retrieves the lower bound that will be used if a lower bound is to be
   * enforced.  If the lower bound is not to be enforced, then the value
   * returned from this method will be meaningless.
   *
   * @return  The lower bound that will be used if a lower bound is to be
   *          enforced.
   */
  public long getLowerBound()
  {
    return lowerBound;
  }



  /**
   * Specifies the value to use as the lower bound, and indicates that a lower
   * bound is to be enforced for this parameter.
   *
   * @param  lowerBound  The value to use as the lower bound.
   */
  public void setLowerBound(long lowerBound)
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
  public long getUpperBound()
  {
    return upperBound;
  }



  /**
   * Specifies the value to use as the upper bound, and indicates that an upper
   * bound is to be enforced for this parameter.
   *
   * @param  upperBound  The value to use as the upper bound.
   */
  public void setUpperBound(long upperBound)
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
        return "No value specified for required parameter";
      }
      else
      {
        return null;
      }
    }
    else if (value instanceof Long)
    {
      long longValue = ((Long) value);
      if (hasLowerBound && (longValue < lowerBound))
      {
        return "Specified value of " + longValue + " is below lower bound of " +
               lowerBound;
      }
      else if (hasUpperBound && (longValue > upperBound))
      {
        return "Specified value of " + longValue + " is above upper bound of " +
               upperBound;
      }
      else
      {
        return null;
      }
    }
    else if (value instanceof String)
    {
      String valueStr = (String) value;
      if (valueStr.length() == 0)
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


      try
      {
        long longValue = Long.parseLong(valueStr);
        if (hasLowerBound && (longValue < lowerBound))
        {
          return "Specified value of " + longValue +
                 " is below lower bound of " + lowerBound;
        }
        else if (hasUpperBound && (longValue > upperBound))
        {
          return "Specified value of " + longValue +
                 " is above upper bound of " + upperBound;
        }
        else
        {
          return null;
        }
      }
      catch (NumberFormatException nfe)
      {
        return '"' + valueStr + "\" is not a valid long value";
      }
    }
    else
    {
      return (value.getClass().getName() + " is not a supported Long " +
              "object type");
    }
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
    return String.valueOf(longValue);
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
                       "\" VALUE=\"" + ((value == null)
                                        ? ""
                                        : String.valueOf(longValue)) +
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
    String valueStr;
    if (value == null)
    {
      valueStr = "";
    }
    else
    {
      valueStr = String.valueOf(longValue);
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
  public LongParameter clone()
  {
    LongParameter lp = new LongParameter(name, displayName, description,
                                         isRequired, longValue, hasLowerBound,
                                         lowerBound, hasUpperBound, upperBound);
    lp.visibleColumns = visibleColumns;
    lp.setSensitive(isSensitive());
    return lp;
  }
}

