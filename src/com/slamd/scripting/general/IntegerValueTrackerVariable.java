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



import com.slamd.job.JobClass;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;



/**
 * This class defines a variable that may be added to a script to act as an
 * integer value tracker.  It also defines a set of methods for dealing with
 * integer value trackers:
 *
 * <UL>
 *   <LI>addValue(int intValue) -- Increments the value of the integer value
 *       tracker.</LI>
 *   <LI>setDisplayName(string name) -- Specifies the display name to use for
 *       the integer value tracker.</LI>
 *   <LI>startTracker() -- Specifies that the integer value tracker should start
 *       collecting statistics.</LI>
 *   <LI>stopTracker() -- Specifies that the integer value tracker should stop
 *       collecting statistics.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class IntegerValueTrackerVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of integer value tracker
   * variables.
   */
  public static final String INTEGER_VALUE_TRACKER_VARIABLE_TYPE =
       "integervaluetracker";



  /**
   * The name of the method that will be used to update the value of the
   * tracker.
   */
  public static final String ADD_VALUE_METHOD_NAME = "addvalue";



  /**
   * The method number for the "addvalue" method.
   */
  public static final int ADD_VALUE_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to set the display name of the
   * integer value tracker.
   */
  public static final String SET_DISPLAY_NAME_METHOD_NAME = "setdisplayname";



  /**
   * The method number for the "setDisplayName" method.
   */
  public static final int SET_DISPLAY_NAME_METHOD_NUMBER = 1;



  /**
   * The name of the method that will be used to tell the tracker to start
   * collecting statistics.
   */
  public static final String START_TRACKER_METHOD_NAME = "starttracker";



  /**
   * The method number for the "startTracker" method.
   */
  public static final int START_TRACKER_METHOD_NUMBER = 2;



  /**
   * The name of the method that will be used to tell the tracker to stop
   * collecting statistics.
   */
  public static final String STOP_TRACKER_METHOD_NAME = "stoptracker";



  /**
   * The method number for the "stopTracker" method.
   */
  public static final int STOP_TRACKER_METHOD_NUMBER = 3;



  /**
   * The set of methods associated with integer value tracker variables.
   */
  public static final Method[] INTEGER_VALUE_TRACKER_VARIABLE_METHODS =
       new Method[]
  {
    new Method(ADD_VALUE_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE } , null),
    new Method(SET_DISPLAY_NAME_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(START_TRACKER_METHOD_NAME, new String[0], null),
    new Method(STOP_TRACKER_METHOD_NAME, new String[0], null)
  };



  // Indicates whether the integer value tracker is currently collecting
  // statistics.
  private boolean collectingStatistics;

  // The job thread with which this variable is associated.
  private JobClass jobThread;

  // The integer value tracker associated with this variable.
  private IntegerValueTracker integerValueTracker;

  // The display name to use for this stat tracker.
  private String displayName;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   */
  public IntegerValueTrackerVariable()
  {
    collectingStatistics = false;
    displayName          = null;
    integerValueTracker  = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return INTEGER_VALUE_TRACKER_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return INTEGER_VALUE_TRACKER_VARIABLE_METHODS;
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
    for (int i=0; i < INTEGER_VALUE_TRACKER_VARIABLE_METHODS.length; i++)
    {
      if (INTEGER_VALUE_TRACKER_VARIABLE_METHODS[i].
               getName().equals(methodName))
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
    for (int i=0; i < INTEGER_VALUE_TRACKER_VARIABLE_METHODS.length; i++)
    {
      if (INTEGER_VALUE_TRACKER_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < INTEGER_VALUE_TRACKER_VARIABLE_METHODS.length; i++)
    {
      if (INTEGER_VALUE_TRACKER_VARIABLE_METHODS[i].hasSignature(methodName,
                                                                 argumentTypes))
      {
        return INTEGER_VALUE_TRACKER_VARIABLE_METHODS[i].getReturnType();
      }
    }

    return null;
  }



  /**
   * Retrieves the integer value tracker associated with this integer value
   * tracker variable.
   *
   * @return  The integer value tracker associated with this integer value
   *          tracker variable.
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    if (integerValueTracker == null)
    {
      return new StatTracker[0];
    }
    else
    {
      return new StatTracker[] { integerValueTracker };
    }
  }



  /**
   * Starts the stat trackers associated with this variable.
   *
   * @param  jobThread  The job thread with which the stat trackers are
   *                    associated.
   */
  @Override()
  public void startStatTrackers(JobClass jobThread)
  {
    this.jobThread = jobThread;

    // The functionality of this method will be handled by the startTracker
    // method, but we need to create the tracker now while we have the job
    // thread information.
    if (displayName == null)
    {
      displayName = getName();
    }

    integerValueTracker =
        new IntegerValueTracker(jobThread.getClientID(),
                                jobThread.getThreadID(), displayName,
                                jobThread.getCollectionInterval());
  }



  /**
   * Stops the stat trackers associated with this variable.
   */
  @Override()
  public void stopStatTrackers()
  {
    if (collectingStatistics)
    {
      integerValueTracker.stopTracker();
    }
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
      case ADD_VALUE_METHOD_NUMBER:
           // Add the value provided as an integer argument to the  stat tracker
           // and don't return a value.
           if (collectingStatistics)
           {
             IntegerVariable iv =
                  (IntegerVariable) arguments[0].getArgumentValue();
             integerValueTracker.addValue(iv.getIntValue());
           }
           return null;
      case SET_DISPLAY_NAME_METHOD_NUMBER:
           // Get the display name to use, set it, and don't return a value.
           StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
           displayName = sv.getStringValue();
           integerValueTracker.setDisplayName(displayName);
           return null;
      case START_TRACKER_METHOD_NUMBER:
           // Start the stat tracker and don't return a value.
           RealTimeStatReporter statReporter = jobThread.getStatReporter();
           if (statReporter != null)
           {
             integerValueTracker.enableRealTimeStats(statReporter,
                                                     jobThread.getJobID());
           }

           integerValueTracker.startTracker();
           collectingStatistics = true;
           return null;
      case STOP_TRACKER_METHOD_NUMBER:
           // Stop the stat tracker and don't return a value.
           integerValueTracker.stopTracker();
           collectingStatistics = false;
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
    if (! argument.getArgumentType().equals(
               INTEGER_VALUE_TRACKER_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                INTEGER_VALUE_TRACKER_VARIABLE_TYPE +
                                " rejected.");
    }

    IntegerValueTrackerVariable ivtv = (IntegerValueTrackerVariable)
                                       argument.getArgumentValue();
    collectingStatistics = ivtv.collectingStatistics;
    displayName          = ivtv.displayName;
    integerValueTracker  = ivtv.integerValueTracker;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return "Integer Value Tracker " + displayName;
  }
}

