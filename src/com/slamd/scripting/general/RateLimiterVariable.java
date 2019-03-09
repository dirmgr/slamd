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
package com.slamd.scripting.general;



import com.slamd.jobs.RateLimiter;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a variable that can be used to control the rate at which
 * operations occur.  It starts a timer, allows the caller to perform an
 * operation, and then sleeps for the remainder of that timer.  It provides the
 * following methods:
 *
 * <UL>
 *   <LI>getDuration() -- Retrieves the length of time in milliseconds that
 *       should elapse between the start of the timer and the end of the
 *       sleep.  It will be returned as an integer variable.</LI>
 *   <LI>setDuration(int duration) -- Specifies the total length of time in
 *       milliseconds that should elapse between the start of the timer and the
 *       end of the sleep.  This method does not return a value.</LI>
 *   <LI>sleepForRemainingTime() -- Sleeps for whatever time is remaining on
 *       the timer after the last time it was started.  This method does not
 *       return a value.</LI>
 *   <LI>startTimer() -- Resets and starts the countdown timer.  This method
 *       does not return a value.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class RateLimiterVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of rate limiter variables.
   */
  public static final String RATE_LIMITER_VARIABLE_TYPE = "ratelimiter";



  /**
   * The name of the method that will be used to get the total duration for the
   * rate limiter.
   */
  public static final String GET_DURATION_METHOD_NAME = "getduration";



  /**
   * The method number for the "getDuration" method.
   */
  public static final int GET_DURATION_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to set the total duration for the
   * rate limiter.
   */
  public static final String SET_DURATION_METHOD_NAME = "setduration";



  /**
   * The method number for the "setDuration" method.
   */
  public static final int SET_DURATION_METHOD_NUMBER = 1;



  /**
   * The name of the method that will be used to sleep for whatever time is
   * remaining on the timer.
   */
  public static final String SLEEP_FOR_REMAINING_TIME_METHOD_NAME =
                                  "sleepforremainingtime";



  /**
   * The method number for the "sleepForRemainingTime" method.
   */
  public static final int SLEEP_FOR_REMAINING_TIME_METHOD_NUMBER = 2;



  /**
   * The name of the method that will be used to reset and start the countdown
   * timer.
   */
  public static final String START_TIMER_METHOD_NAME = "starttimer";



  /**
   * The method number for the "startTimer" method.
   */
  public static final int START_TIMER_METHOD_NUMBER = 3;



  /**
   * The set of methods associated with file URL variables.
   */
  public static final Method[] RATE_LIMITER_VARIABLE_METHODS = new Method[]
  {
    new Method(GET_DURATION_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SET_DURATION_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(SLEEP_FOR_REMAINING_TIME_METHOD_NAME, new String[0], null),
    new Method(START_TIMER_METHOD_NAME, new String[0], null)
  };



  // The rate limiter used to do the work.
  private RateLimiter rateLimiter;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public RateLimiterVariable()
         throws ScriptException
  {
    rateLimiter = new RateLimiter(0);
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return RATE_LIMITER_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return RATE_LIMITER_VARIABLE_METHODS;
  }



  /**
   * Indicates whether this variable type has a method with the specified name.
   *
   * @param  methodName  The name of the method.
   *
   * @return  <CODE>true</CODE> if this variable has a method with the specified
   *          name, or <CODE>false</CODE> if it does not.
   */
  @Override()
  public boolean hasMethod(String methodName)
  {
    for (int i=0; i < RATE_LIMITER_VARIABLE_METHODS.length; i++)
    {
      if (RATE_LIMITER_VARIABLE_METHODS[i].getName().equals(methodName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the method number for the method that has the specified name and
   * argument types, or -1 if there is no such method.
   *
   * @param  methodName     The name of the method.
   * @param  argumentTypes  The list of argument types for the method.
   *
   * @return  The method number for the method that has the specified name and
   *          argument types.
   */
  @Override()
  public int getMethodNumber(String methodName, String[] argumentTypes)
  {
    for (int i=0; i < RATE_LIMITER_VARIABLE_METHODS.length; i++)
    {
      if (RATE_LIMITER_VARIABLE_METHODS[i].hasSignature(methodName,
                                                        argumentTypes))
      {
        return i;
      }
    }

    return -1;
  }



  /**
   * Retrieves the return type for the method with the specified name and
   * argument types.
   *
   * @param  methodName     The name of the method.
   * @param  argumentTypes  The set of argument types for the method.
   *
   * @return  The return type for the method, or <CODE>null</CODE> if there is
   *          no such method defined.
   */
  @Override()
  public String getReturnTypeForMethod(String methodName,
                                       String[] argumentTypes)
  {
    for (int i=0; i < RATE_LIMITER_VARIABLE_METHODS.length; i++)
    {
      if (RATE_LIMITER_VARIABLE_METHODS[i].hasSignature(methodName,
                                                         argumentTypes))
      {
        return RATE_LIMITER_VARIABLE_METHODS[i].getReturnType();
      }
    }

    return null;
  }



  /**
   * Executes the specified method, using the provided variables as arguments
   * to the method, and makes the return value available to the caller.
   *
   * @param  lineNumber    The line number of the script in which the method
   *                       call occurs.
   * @param  methodNumber  The method number of the method to execute.
   * @param  arguments     The set of arguments to use for the method.
   *
   * @return  The value returned from the method, or <CODE>null</CODE> if it
   *          does not return a value.
   *
   * @throws  ScriptException  If the specified method does not exist, or if a
   *                           problem occurs while attempting to execute it.
   */
  @Override()
  public Variable executeMethod(int lineNumber, int methodNumber,
                                Argument[] arguments)
         throws ScriptException
  {
    switch (methodNumber)
    {
      case GET_DURATION_METHOD_NUMBER:
        return new IntegerVariable(rateLimiter.getDuration());
      case SET_DURATION_METHOD_NUMBER:
        IntegerVariable iv = (IntegerVariable) arguments[0].getArgumentValue();
        rateLimiter.setDuration(iv.getIntValue());
        return null;
      case SLEEP_FOR_REMAINING_TIME_METHOD_NUMBER:
        rateLimiter.sleepForRemainingTime();
        return null;
      case START_TIMER_METHOD_NUMBER:
        rateLimiter.startTimer();
        return null;
      default:
        throw new ScriptException(lineNumber,
                                  "There is no method " + methodNumber +
                                  " defined for " + getArgumentType() +
                                  " variables.");
    }
  }



  /**
   * Assigns the value of the provided argument to this variable.  The value of
   * the provided argument must be of the same type as this variable.
   *
   * @param  argument  The argument whose value should be assigned to this
   *                   variable.
   *
   * @throws  ScriptException  If a problem occurs while performing the
   *                           assignment.
   */
  @Override()
  public void assign(Argument argument)
         throws ScriptException
  {
    if (! argument.getArgumentType().equals(RATE_LIMITER_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                RATE_LIMITER_VARIABLE_TYPE + " rejected.");
    }

    RateLimiterVariable rlv = (RateLimiterVariable) argument.getArgumentValue();
    rateLimiter = rlv.rateLimiter;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return "RateLimiter(" + rateLimiter.getDuration() + ')';
  }
}

