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



import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Null;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class defines a generic parameter that can be passed to a job to
 * customize the way that it operates.  It also defines functions that can be
 * used to request and accept the value in the context of an HTML form.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class Parameter
       implements Cloneable
{
  /**
   * Indicates whether this parameter is required to have a value.
   */
  protected boolean isRequired;



  /**
   * Indicates whether this parameter should be considered sensitive.
   */
  protected boolean isSensitive;



  /**
   * The value for this parameter, which is subject to validation.
   */
  protected Object value;



  /**
   * A description for this parameter that can provide additional information
   * about it than just the name.
   */
  protected String description;



  /**
   * The name that will be used to reference the parameter any time a name
   * needs to be displayed to the end user.
   */
  protected String displayName;



  /**
   * The name that will be used to reference the parameter internally and in
   * HTML forms.
   */
  protected String name;



  /**
   * Creates a new instance of the Parameter to be used when decoding values
   * transported over the network, and should not be used by jobs to create
   * parameters.  If any initialization is needed that will not be covered by
   * calls to the <CODE>setName</CODE>, <CODE>setDisplayName</CODE>,
   * <CODE>setDescription</CODE>, or <CODE>setValueFromString</CODE>, then it
   * should be done here.
   */
  public Parameter()
  {
    // No implementation required.
  }



  /**
   * Creates a new instance of the Parameter based on the provided information.
   * This is only to be called by subclasses that actually implement valid
   * Parameter objects.  Note that if a value is provided, no validation is
   * performed on it to verify that it is acceptable.
   *
   * @param  name         The name of the parameter as it is used internally.
   * @param  displayName  The name of the parameter as it is to be displayed to
   *                      the end user.
   * @param  description  A description of this parameter that provides more
   *                      information than just the name.
   * @param  isRequired   Indicates whether this parameter must have a value.
   * @param  value        The value for this parameter.
   */
  public Parameter(String name, String displayName, String description,
                   boolean isRequired, Object value)
  {
    this.name          = name;
    this.displayName   = displayName;
    this.description   = description;
    this.isRequired    = isRequired;
    this.value         = value;
    this.isSensitive   = false;
  }



  /**
   * Retrieves the name of this parameter as it is used internally.
   *
   * @return  The name of this parameter as it is used internally.
   */
  public final String getName()
  {
    return name;
  }



  /**
   * Specifies the name to use for this parameter.
   *
   * @param  name  The name to use for this parameter.
   */
  public final void setName(String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the name of this parameter as it is displayed to the end user.
   *
   * @return  The name of this parameter as it is displayed to the end user.
   */
  public final String getDisplayName()
  {
    return displayName;
  }



  /**
   * Specifies the name of this parameter as it should be displayed to the end
   * user.
   *
   * @param  displayName  The name of this parameter as it should be displayed
   *                      to the end user.
   */
  public final void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }



  /**
   * Retrieves the description for this parameter.
   *
   * @return  The description for this parameter.
   */
  public final String getDescription()
  {
    return description;
  }



  /**
   * Specifies the description for this parameter.
   *
   * @param  description  The description for this parameter.
   */
  public final void setDescription(String description)
  {
    this.description = description;
  }



  /**
   * Indicates whether this parameter is required to have a value.
   *
   * @return  <CODE>true</CODE> if this parameter is required to have a value,
   *          or <CODE>false</CODE> if it does not need a value.
   */
  public final boolean isRequired()
  {
    return isRequired;
  }



  /**
   * Specifies whether this parameter is required to have a value.
   *
   * @param  isRequired  Indicates whether this parameter is required to have
   *                     a value.
   */
  public final void setRequired(boolean isRequired)
  {
    this.isRequired = isRequired;
  }



  /**
   * Indicates whether this parameter should be considered sensitive.
   *
   * @return  <CODE>true</CODE> if this parameter should be considered
   *          sensitive, or <CODE>false</CODE> if not.
   */
  public boolean isSensitive()
  {
    return isSensitive;
  }



  /**
   * Specifies whether this parameter should be considered sensitive.
   *
   * @param  isSensitive  Specifies whether this parameter should be considered
   *                      sensitive.
   */
  public void setSensitive(boolean isSensitive)
  {
    this.isSensitive = isSensitive;
  }



  /**
   * Retrieves the value for this parameter.
   *
   * @return  The value for this parameter.
   */
  public Object getValue()
  {
    return value;
  }



  /**
   * Specifies the value to use for this parameter.
   *
   * @param  value  The value to use for this parameter.
   *
   * @throws  InvalidValueException  If the specified value is not acceptable
   *                                 for use in this parameter.
   */
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
  public abstract void setValueFrom(Parameter parameter);



  /**
   * Retrieves a string representation of the value of this parameter.
   *
   * @return  A string representation of the value of this parameter.
   */
  public abstract String getValueString();



  /**
   * Specifies the value to use for this parameter from the provided String.
   *
   * @param  valueString  The string representation of the value to use for this
   *                      parameter.
   *
   * @throws  InvalidValueException  If the provided value cannot be used to
   *                                 provide a value for this parameter.
   */
  public abstract void setValueFromString(String valueString)
         throws InvalidValueException;



  /**
   * Indicates whether a value has been specified for this parameter.
   *
   * @return  <CODE>true</CODE> if a value has been specified for this parameter
   *          or <CODE>false</CODE> if not.
   */
  public boolean hasValue()
  {
    return (value != null);
  }



  /**
   * Indicates whether the current value for this parameter is valid.
   *
   * @return  <CODE>true</CODE> if the current value for this parameter is
   *          valid, or <CODE>false</CODE> if it is not.
   */
  public final boolean isValid()
  {
    return isValid(value);
  }



  /**
   * Indicates whether the specified value would be a valid value for this
   * parameter.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the specified value would be a valid value
   *          for this parameter, or <CODE>false</CODE> if it would not be.
   */
  public final boolean isValid(Object value)
  {
    return (getInvalidReason(value) == null);
  }



  /**
   * Retrieves the reason that the current value is not valid.
   *
   * @return  The reason that the current value is not valid, or
   *           <CODE>null</CODE> if it is valid.
   */
  public final String getInvalidReason()
  {
    return getInvalidReason(value);
  }



  /**
   * Retrieves the reason that the specified value is not valid.
   *
   * @param  value  The value for which to make the determination.
   *
   * @return  The reason that the specified value is not valid, or
   *          <CODE>null</CODE> if it is valid.
   */
  public abstract String getInvalidReason(Object value);



  /**
   * Retrieves a String that can be used when displaying the value of this
   * parameter to the end user.
   *
   * @return  A String that can be used when displaying the value of this
   *          parameter to the end user.
   */
  public abstract String getDisplayValue();



  /**
   * Retrieves a String that can be used when displaying the value of this
   * parameter to the end user in the context of an HTML page.
   *
   * @return  A String that can be used when displaying the value of this
   *          parameter to the end user in the context of an HTML page.
   */
  public String getHTMLDisplayValue()
  {
    return Constants.makeHTMLSafe(getDisplayValue());
  }



  /**
   * Retrieves the value of this parameter as it would be submitted by a browser
   * posting the request.
   *
   * @return  The value of this parameter as it would be submitted by a browser
   *          posting the request.
   */
  public String getHTMLPostValue()
  {
    if (hasValue())
    {
      try
      {
        return URLEncoder.encode(getValueString(), "UTF-8");
      }
      catch (Exception e)
      {
        // This should never happen.
        return null;
      }
    }
    else
    {
      return null;
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
  public abstract String getHTMLInputForm(String prefix);



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
  public abstract void htmlInputFormToValue(String[] values)
         throws InvalidValueException;



  /**
   * Specifies the value of this parameter based on the provided servlet
   * request.
   *
   * @param  request  The servlet request from which to obtain the value(s) for
   *                  this parameter.
   *
   * @throws  InvalidValueException  If the provided request does not contain a
   *                                 valid value for this parameter.
   */
  public void htmlInputFormToValue(HttpServletRequest request)
         throws InvalidValueException
  {
    String[] values =
         request.getParameterValues(Constants.SERVLET_PARAM_JOB_PARAM_PREFIX +
                                    name);
    htmlInputFormToValue(values);
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
  public abstract String generateHidden(String prefix);



  /**
   * Retrieves a string representation of this parameter.
   *
   * @return  A string representation of this parameter.
   */
  @Override()
  public String toString()
  {
    return name + ":  " + getDisplayValue();
  }



  /**
   * Creates a clone of this parameter.
   *
   * @return  A clone of this parameter.
   */
  @Override()
  public abstract Parameter clone();



  /**
   * Decodes the provided ASN.1 element as a parameter.
   *
   * @param  element  The ASN.1 element to decode.
   *
   * @return  The parameter decoded from the ASN.1 element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a parameter.
   */
  public static Parameter decode(ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence parameterSequence = null;
    try
    {
      parameterSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("Unable to decode the ASN.1 element as a " +
                               "sequence", ae);
    }


    ASN1Element[] elements = parameterSequence.getElements();
    if (elements.length < 3)
    {
      throw new SLAMDException("The number of elements in the parameter " +
                               "sequence is not at least 3");
    }


    String className = null;
    try
    {
      className = elements[0].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The first element of the parameter sequence " +
                               "is not an enumerated value", ae);
    }


    String name = null;
    try
    {
      name = elements[1].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The second element of the parameter sequence " +
                               "is not an octet string", ae);
    }


    String valueStr = null;
    try
    {
      valueStr = elements[2].decodeAsOctetString().getStringValue();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The third element of the parameter sequence " +
                               "is not an octet string", ae);
    }


    String displayName = null;
    if (elements.length > 3)
    {
      try
      {
        displayName = elements[3].decodeAsOctetString().getStringValue();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("The fourth element of the parameter " +
                                 "sequence is not an octet string", ae);
      }
    }


    String description = null;
    if (elements.length > 4)
    {
      try
      {
        description = elements[4].decodeAsOctetString().getStringValue();
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("The fifth element of the parameter " +
                                 "sequence is not an octet string", ae);
      }
    }


    boolean isRequired = false;
    if (elements.length > 5)
    {
      try
      {
        isRequired = elements[5].decodeAsBoolean().getBooleanValue();
      }
      catch (ASN1Exception e)
      {
        throw new SLAMDException("The sixth element of the parameter " +
                                 "sequence is not a Boolean.");
      }
    }


    boolean isSensitive = false;
    if (elements.length > 6)
    {
      try
      {
        isSensitive = elements[6].decodeAsBoolean().getBooleanValue();
      }
      catch (ASN1Exception e)
      {
        throw new SLAMDException("The seventh element of the parameter " +
                                 "sequence is not a Boolean.");
      }
    }


    String[] choices = null;
    if (elements.length > 7)
    {
      try
      {
        if (elements[7].getType() == ASN1Element.ASN1_SEQUENCE_TYPE)
        {
          ASN1Element[] choiceElements =
               elements[7].decodeAsSequence().getElements();
          choices = new String[choiceElements.length];
          for (int i=0; i < choices.length; i++)
          {
            choices[i] =
                 choiceElements[i].decodeAsOctetString().getStringValue();
          }
        }
      }
      catch (ASN1Exception e)
      {
        throw new SLAMDException("The eighth element of the parameter " +
                                 "sequence is not a sequence of octet " +
                                 "strings or null element.");
      }
    }


    try
    {
      Class<?> parameterClass = Constants.classForName(className);
      Parameter p = (Parameter) parameterClass.newInstance();
      p.setName(name);
      p.setValueFromString(valueStr);

      if (displayName != null)
      {
        p.setDisplayName(displayName);
      }

      if (description != null)
      {
        p.setDescription(description);
      }

      p.setRequired(isRequired);
      p.setSensitive(isSensitive);

      if (choices != null)
      {
        if (p instanceof MultiChoiceParameter)
        {
          MultiChoiceParameter mcp = (MultiChoiceParameter) p;
          mcp.setChoices(choices);
        }
        else if (p instanceof IntegerWithUnitParameter)
        {
          IntegerWithUnitParameter iwup = (IntegerWithUnitParameter) p;
          iwup.setChoices(choices);
        }
      }

      return p;
    }
    catch (Exception e)
    {
      throw new SLAMDException("Could not create the parameter instance:  " +
                               e, e);
    }
  }



  /**
   * Encodes this parameter as an ASN.1 element.  The ASN.1 syntax for a
   * parameter is:
   * <BR><BR>
   * <CODE>Parameter ::= SEQUENCE {</CODE>
   * <CODE>    className     OCTET STRING,</CODE>
   * <CODE>    name          OCTET STRING,</CODE>
   * <CODE>    value         OCTET STRING,</CODE>
   * <CODE>    displayName   OCTET STRING OPTIONAL,</CODE>
   * <CODE>    description   OCTET STRING OPTIONAL,</CODE>
   * <CODE>    isRequired    BOOLEAN OPTIONAL,</CODE>
   * <CODE>    isSensitive   BOOLEAN OPTIONAL,</CODE>
   * <CODE>    allowedValues SEQUENCE OF OCTET STRING OPTIONAL }</CODE>
   * <BR>
   *
   * @return  The encoded ASN.1 element.
   */
  public final ASN1Element encode()
  {
    ASN1Element[] parameterElements;

    if (this instanceof MultiChoiceParameter)
    {
      MultiChoiceParameter mcp = (MultiChoiceParameter) this;

      String[] choices = mcp.getChoices();
      ASN1Element[] allowedValueElements = new ASN1Element[choices.length];
      for (int i=0; i < allowedValueElements.length; i++)
      {
        allowedValueElements[i] = new ASN1OctetString(choices[i]);
      }

      parameterElements = new ASN1Element[]
      {
        new ASN1OctetString(getClass().getName()),
        new ASN1OctetString(name),
        new ASN1OctetString(getValueString()),
        new ASN1OctetString(displayName),
        new ASN1OctetString(description),
        new ASN1Boolean(isRequired),
        new ASN1Boolean(isSensitive),
        new ASN1Sequence(allowedValueElements)
      };
    }
    else if (this instanceof IntegerWithUnitParameter)
    {
      IntegerWithUnitParameter iwup = (IntegerWithUnitParameter) this;

      String[] choices = iwup.getChoices();
      ASN1Element[] allowedValueElements = new ASN1Element[choices.length];
      for (int i=0; i < allowedValueElements.length; i++)
      {
        allowedValueElements[i] = new ASN1OctetString(choices[i]);
      }

      parameterElements = new ASN1Element[]
      {
        new ASN1OctetString(getClass().getName()),
        new ASN1OctetString(name),
        new ASN1OctetString(getValueString()),
        new ASN1OctetString(displayName),
        new ASN1OctetString(description),
        new ASN1Boolean(isRequired),
        new ASN1Boolean(isSensitive),
        new ASN1Sequence(allowedValueElements)
      };
    }
    else
    {
      parameterElements = new ASN1Element[]
      {
        new ASN1OctetString(getClass().getName()),
        new ASN1OctetString(name),
        new ASN1OctetString(getValueString()),
        new ASN1OctetString(displayName),
        new ASN1OctetString(description),
        new ASN1Boolean(isRequired),
        new ASN1Boolean(isSensitive),
        new ASN1Null()
      };
    }

    return new ASN1Sequence(parameterElements);
  }
}

