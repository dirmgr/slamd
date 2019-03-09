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
 * This class defines a special type of parameter that is not a real parameter
 * in the sense that the user can change its value, but rather is simply static
 * text that will appear in the list of parameters to provide a type of section
 * header.
 *
 *
 * @author   Neil A. Wilson
 */
public class LabelParameter
       extends Parameter
{
  // The text that will appear in the label.
  private String labelText;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public LabelParameter()
  {
    super();

    // No additional implementation required.
  }



  /**
   * Creates a new label parameter with the specified text.
   *
   * @param  labelText  The text to use for this label parameter.
   */
  public LabelParameter(String labelText)
  {
    super("", "", "", false, labelText);
    this.labelText = labelText;
  }


  /**
   * Retrieves the value for this parameter.
   *
   * @return  The value for this parameter.
   */
  @Override()
  public String getValue()
  {
    return labelText;
  }



  /**
   * Sets the value for this parameter.
   *
   * @param  value  The value for this parameter.
   */
  @Override()
  public void setValue(Object value)
  {
    labelText = String.valueOf(value);
    value = labelText;
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
    if (parameter instanceof LabelParameter)
    {
      labelText = ((LabelParameter) parameter).labelText;
      value = labelText;
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
    return labelText;
  }






  /**
   * Specifies the value to use for this parameter from the provided String.
   *
   * @param  valueString  The string representation of the value to use for this
   *                      parameter.
   */
  @Override()
  public void setValueFromString(String valueString)
  {
    labelText = valueString;
    value = labelText;
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
    return null;
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
    return labelText;
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
    return "<B>" + labelText + "</B>";
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
    return "<B>" + labelText + "</B>";
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
    // No implementation required.
  }



  /**
   * Retrieves the value of this parameter as it would be submitted by a browser
   * posting the request.
   *
   * @return  The value of this parameter as it would be submitted by a browser
   *          posting the request.
   */
  @Override()
  public String getHTMLPostValue()
  {
    return null;
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
    return "";
  }



  /**
   * Retrieves a string representation of this parameter.
   *
   * @return  A string representation of this parameter.
   */
  @Override()
  public String toString()
  {
    return labelText;
  }



  /**
   * Creates a clone of this parameter.  In this case, because this is an
   * abstract class, we won't return anything.  However, subclasses will return
   * actual copies of themselves.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public LabelParameter clone()
  {
    return new LabelParameter(labelText);
  }
}

