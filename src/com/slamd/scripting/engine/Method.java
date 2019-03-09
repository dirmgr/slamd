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
package com.slamd.scripting.engine;



/**
 * This class simply holds information about a method signature, including the
 * name of the method, the types of arguments that it accepts, and the return
 * type for the method.  Note that all matching in this method is case
 * sensitive, so it is expected that all strings will be converted to lowercase
 * before they are provided to anything in this class.
 *
 *
 * @author   Neil A. Wilson
 */
public class Method
{
  // The name to use for the method.
  private String name;

  // The types of arguments that the method accepts.
  private String[] argumentTypes;

  // The return type for the method.
  private String returnType;



  /**
   * Creates a new method with the specified name, argument types, and return
   * type.
   *
   * @param  name           The name of the method.
   * @param  argumentTypes  The names of the data types of all the arguments for
   *                        this method.
   * @param  returnType     The name of the data type that this method returns.
   */
  public Method(String name, String[] argumentTypes, String returnType)
  {
    this.name          = name;
    this.argumentTypes = argumentTypes;
    this.returnType    = returnType;
  }



  /**
   * Retrieves the name of this method.
   *
   * @return  The name of this method.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Retrieves the set of argument types for this method.
   *
   * @return  The name set of argument types for this method.
   */
  public String[] getArgumentTypes()
  {
    return argumentTypes;
  }



  /**
   * Retrieves the return type of this method.
   *
   * @return  The return type of this method.
   */
  public String getReturnType()
  {
    return returnType;
  }



  /**
   * Indicates whether the name of this method matches the specified name.
   *
   * @param  name  The name to compare against this method.
   *
   * @return  <CODE>true</CODE> if the name of this method matches the specified
   *          name, or <CODE>false</CODE> if not.
   */
  public boolean isNamed(String name)
  {
    return this.name.equals(name);
  }



  /**
   * Indicates whether this method has the specified name and set of argument
   * types.
   *
   * @param  name           The name to compare against that of this method.
   * @param  argumentTypes  The set of argument types to compare against those
   *                        of this method.
   *
   * @return  <CODE>true</CODE> if this method does have the specified name and
   *          set of argument types, or <CODE>false</CODE> if it does not.
   */
  public boolean hasSignature(String name, String[] argumentTypes)
  {
    if (! this.name.equals(name))
    {
      return false;
    }

    if (this.argumentTypes.length != argumentTypes.length)
    {
      return false;
    }

    for (int i=0; i < argumentTypes.length; i++)
    {
      if (! this.argumentTypes[i].equals(argumentTypes[i]))
      {
        return false;
      }
    }

    return true;
  }



  /**
   * Indicates whether this method has the specified name, set of argument
   * types, and return type.
   *
   * @param  name           The name to compare against that of this method.
   * @param  argumentTypes  The set of argument types to compare against those
   *                        of this method.
   * @param  returnType     The name of the return type to compare against that
   *                        of this method.
   *
   * @return  <CODE>true</CODE> if this method does have the specified name, set
   *          of argument types, and return type, or <CODE>false</CODE> if it
   *          does not.
   */
  public boolean hasSignature(String name, String[] argumentTypes,
                              String returnType)
  {
    if (! hasSignature(name, argumentTypes))
    {
      return false;
    }

    return this.returnType.equals(returnType);
  }



  /**
   * Retrieves a string representation of this method.
   *
   * @return  A string representation of this method.
   */
  @Override()
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();

    buffer.append(name);
    buffer.append('(');

    if (argumentTypes.length > 0)
    {
      buffer.append(argumentTypes[0]);

      for (int i=1; i < argumentTypes.length; i++)
      {
        buffer.append(", ");
        buffer.append(argumentTypes[i]);
      }
    }

    buffer.append(')');

    return buffer.toString();
  }
}

