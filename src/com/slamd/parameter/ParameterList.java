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

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;



/**
 * This class defines a mechanism for dealing with a number of parameters
 * associated with a job as a single unit.  There are convenience methods for
 * retrieving a particular parameter or all parameters at once, and there is a
 * method for reading the values from an HTML post into the values of the
 * parameters (doing all necessary validation).
 *
 *
 * @author   Neil A. Wilson
 */
public class ParameterList
       implements Cloneable
{
  // A mutex used for protecting multithreaded access to the parameter list
  private final Object parameterMutex;

  // The array list used to hold all of the parameters.
  private ArrayList<Parameter> parameters;



  /**
   * Creates a new parameter list with no parameters.  New parameters should be
   * added using the <CODE>addParameter</CODE> or <CODE>setParameters</CODE>
   * methods.
   */
  public ParameterList()
  {
    this(new Parameter[0]);
  }



  /**
   * Creates a new parameter list with the specified set of parameters.  Note
   * that there may not be any parameters with duplicate names.  If there are
   * multiple parameters with the same name, then the last one added will be
   * used.
   *
   * @param  parameterArray  An array of parameters to be included in the
   *                         parameter list.
   */
  public ParameterList(Parameter[] parameterArray)
  {
    parameterMutex = new Object();
    setParameters(parameterArray);
  }



  /**
   * Retrieves the parameter with the specified name from the parameter list.
   * If there is no parameter with that name, then <CODE>null</CODE> will be
   * returned.  The parameter name is case sensitive.
   *
   * @param  parameterName  The name of the parameter to retrieve from the
   *                        parameter list.
   *
   * @return  The parameter with the specified name, or <CODE>null</CODE> if
   *          there is no such parameter.
   */
  public Parameter getParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          return param;
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the Boolean parameter with the specified name.  If there is no
   * parameter with that name, or if it is not a Boolean parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the Boolean parameter to retrieve from
   *                        the parameter list.
   *
   * @return  The Boolean parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public BooleanParameter getBooleanParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof BooleanParameter)
          {
            return (BooleanParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the file parameter with the specified name.  If there is no
   * parameter with that name, or if it is not a file parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the file parameter to retrieve from the
   *                        parameter list.
   *
   * @return  The file parameter with the specified name, or <CODE>null</CODE>
   *          if there is no such parameter.
   */
  public FileURLParameter getFileURLParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof FileURLParameter)
          {
            return (FileURLParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the floating-point parameter with the specified name.  If there
   * is no parameter with that name, or if it is not a floating-point parameter,
   * then <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the floating-point parameter to retrieve
   *                        from the parameter list.
   *
   * @return  The floating-point parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public FloatParameter getFloatParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof FloatParameter)
          {
            return (FloatParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the integer parameter with the specified name.  If there is no
   * parameter with that name, or if it is not an integer parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the integer parameter to retrieve from
   *                        the parameter list.
   *
   * @return  The integer parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public IntegerParameter getIntegerParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof IntegerParameter)
          {
            return (IntegerParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the integer with unit parameter with the specified name.  If
   * there is no parameter with that name, or if it is not an integer wit unit
   * parameter, then <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the integer with unit parameter to
   *                        retrieve from the parameter list.
   *
   * @return  The integer with unit parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public IntegerWithUnitParameter
              getIntegerWithUnitParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof IntegerWithUnitParameter)
          {
            return (IntegerWithUnitParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the long parameter with the specified name.  If there is no
   * parameter with that name, or if it is not a long parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the long parameter to retrieve from the
   *                        parameter list.
   *
   * @return  The long parameter with the specified name, or <CODE>null</CODE>
   *          if there is no such parameter.
   */
  public LongParameter getLongParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof LongParameter)
          {
            return (LongParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the multichoice parameter with the specified name.  If there
   * is no parameter with that name, or if it is not a multichoice parameter,
   * then <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the multichoice parameter to retrieve
   *                        from the parameter list.
   *
   * @return  The multichoice parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public MultiChoiceParameter getMultiChoiceParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof MultiChoiceParameter)
          {
            return (MultiChoiceParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the multi-line text parameter with the specified name.  If there
   * is no parameter with that name, or if it is not a multi-line text
   * parameter, then <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the multi-line text parameter to
   *                        retrieve from the parameter list.
   *
   * @return  The multi-line text parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public MultiLineTextParameter getMultiLineTextParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof MultiLineTextParameter)
          {
            return (MultiLineTextParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the multivalued parameter with the specified name.  If there is
   * no parameter with that name, or if it is not a multivalued parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the multivalued parameter to retrieve
   *                        from the parameter list.
   *
   * @return  The multivalued parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public MultiValuedParameter getMultiValuedParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof MultiValuedParameter)
          {
            return (MultiValuedParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the password parameter with the specified name.  If there is no
   * parameter with that name, or if it is not a password parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the password parameter to retrieve from
   *                        the parameter list.
   *
   * @return  The password parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public PasswordParameter getPasswordParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof PasswordParameter)
          {
            return (PasswordParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Retrieves the string parameter with the specified name.  If there is no
   * parameter with that name, or if it is not a string parameter, then
   * <CODE>null</CODE> is returned.
   *
   * @param  parameterName  The name of the string parameter to retrieve from
   *                        the parameter list.
   *
   * @return  The string parameter with the specified name, or
   *          <CODE>null</CODE> if there is no such parameter.
   */
  public StringParameter getStringParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          if (param instanceof StringParameter)
          {
            return (StringParameter) param;
          }
          else
          {
            return null;
          }
        }
      }
    }

    return null;
  }



  /**
   * Specifies an entirely new set of parameters to be stored in this parameter
   * list.  Any existing parameter information will be lost.
   *
   * @param  parameterArray  The parameters to store in this parameter list.
   */
  public void setParameters(Parameter[] parameterArray)
  {
    // If the parameter array was provided with information in it, then use
    // those parameters to populate the list.  Otherwise, just create an
    // empty list.
    synchronized (parameterMutex)
    {
      if ((parameterArray != null) && (parameterArray.length > 0))
      {
        this.parameters = new ArrayList<Parameter>(parameterArray.length);

        for (int i=0; i < parameterArray.length; i++)
        {
          parameters.add(parameterArray[i]);
        }
      }
      else
      {
        this.parameters = new ArrayList<Parameter>();
      }
    }
  }



  /**
   * Indicates whether the parameter list currently contains a parameter with
   * the specified name.
   *
   * @param  parameterName  The name of the parameter for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if a parameter exists with the specified name,
   *          or <CODE>false</CODE> if not.
   */
  public boolean hasParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          return true;
        }
      }
    }

    return false;
  }


  /**
   * Adds the specified parameter to the parameter list.  If a parameter already
   * exists with that name, then it will be replaced with the provided
   * parameter.
   *
   * @param  parameter  The parameter to store in the parameter list.
   */
  public void addParameter(Parameter parameter)
  {
    synchronized (parameterMutex)
    {
      if (! ((parameter instanceof PlaceholderParameter) ||
             (parameter instanceof LabelParameter)))
      {
        for (int i=0; i < parameters.size(); i++)
        {
          Parameter param = parameters.get(i);
          if (param.getName().equals(parameter.getName()))
          {
            parameters.set(i, parameter);
            return;
          }
        }
      }

      parameters.add(parameter);
    }
  }



  /**
   * Removes the specified parameter from the parameter list.  If the specified
   * parameter does not exist, then no action will be taken.
   *
   * @param  parameterName  The name of the parameter to remove from the list.
   */
  public void removeParameter(String parameterName)
  {
    synchronized (parameterMutex)
    {
      for (int i=0; i < parameters.size(); i++)
      {
        Parameter param = parameters.get(i);
        if (param.getName().equals(parameterName))
        {
          parameters.remove(i);
          return;
        }
      }
    }
  }



  /**
   * Removes all parameters from the parameter list.
   */
  public void removeAllParameters()
  {
    synchronized (parameterMutex)
    {
      parameters.clear();
    }
  }



  /**
   * Retrieves the set of parameters stored in this list as an array.
   *
   * @return  The set of parameters stored in this list.
   */
  public Parameter[] getParameters()
  {
    synchronized (parameterMutex)
    {
      Parameter[] parameterArray = new Parameter[parameters.size()];

      for (int i=0; i < parameterArray.length; i++)
      {
        parameterArray[i] = parameters.get(i);
      }

      return parameterArray;
    }
  }



  /**
   * Creates a clone of this parameter list.
   *
   * @return  A clone of this parameter list.
   */
  @Override()
  public ParameterList clone()
  {
    Parameter[] paramArray = new Parameter[parameters.size()];

    for (int i=0; i < paramArray.length; i++)
    {
      paramArray[i] = parameters.get(i).clone();
    }

    return new ParameterList(paramArray);
  }



  /**
   * Decodes the provided ASN.1 element as a parameter list.
   *
   * @param  element  The ASN.1 element to decode as a parameter list.
   *
   * @return  The parameter list extracted from the provided element.
   *
   * @throws  SLAMDException  If the provided ASN.1 element cannot be decoded
   *                          as a parameter list.
   */
  public static ParameterList decode(ASN1Element element)
         throws SLAMDException
  {
    ASN1Sequence listSequence = null;
    try
    {
      listSequence = element.decodeAsSequence();
    }
    catch (ASN1Exception ae)
    {
      throw new SLAMDException("The provided ASN.1 element cannot be decoded " +
                               "as a sequence", ae);
    }

    ASN1Element[] elements = listSequence.getElements();
    Parameter[] parameters = new Parameter[elements.length];
    for (int i=0; i < elements.length; i++)
    {
      parameters[i] = Parameter.decode(elements[i]);
    }

    return new ParameterList(parameters);
  }



  /**
   * Encodes this parameter list as an ASN.1 element.  The ASN.1 syntax for a
   * parameter list is:
   * <BR><BR>
   * <CODE>Parameters ::= SEQUENCE OF Parameter</CODE>
   * <BR>
   *
   * @return  The ASN.1 element containing the encoded parameter list.
   */
  public ASN1Element encode()
  {
    Parameter[] parameters = getParameters();
    ASN1Element[] parameterElements = new ASN1Element[parameters.length];
    for (int i=0; i < parameters.length; i++)
    {
      parameterElements[i] = parameters[i].encode();
    }

    return new ASN1Sequence(parameterElements);
  }
}

