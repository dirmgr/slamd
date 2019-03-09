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



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.slamd.common.Constants;
import com.slamd.job.JobClass;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.ScriptParser;
import com.slamd.scripting.engine.StopRunningException;
import com.slamd.scripting.engine.Variable;



/**
 * This class defines a set of methods that may be called during script
 * execution to perform various tasks.  These methods are:
 *
 * <UL>
 *   <LI>debugMessage(string message) -- Writes the specified message to the
 *       client's debug writer.  This method does not return a value.</LI>
 *   <LI>exit() -- Stops execution of the script.  This method does not return a
 *       value.</LI>
 *   <LI>exitWithError() -- Stops execution of the script and indicates that one
 *       or more errors occurred during execution that may impact the accuracy
 *       of the results collected.  This method does not return a value.</LI>
 *   <LI>getClientNumber() -- Retrieves the client number that has been assigned
 *       to the client running the job.  The first client will be client number
 *       0, the second will be client number 1, etc.</LI>
 *   <LI>getThreadNumber() -- Retrieves the thread number that has been assigned
 *       to the thread running the job on the current client.  The first thread
 *       will be thread number 0, the second will be thread number 1, etc.  Each
 *       client will have identical thread number ranges.</LI>
 *   <LI>getScriptArgument(string name) -- Retrieves the specified argument that
 *       has been passed to the script at the time that it was scheduled.  The
 *       argument value is returned as a string, and a null string is returned
 *       if the parameter was not specified.</LI>
 *   <LI>getScriptArgument(string name, string defaultValue) -- Retrieves the
 *       specified argument that has been passed to the script at the time that
 *       it was scheduled.  The argument value is returned as a string, and the
 *       default value is returned if the parameter was not specified.</LI>
 *   <LI>getScriptBooleanArgument(string name, boolean defaultValue) --
 *       Retrieves the boolean value of the specified argument that has been
 *       passed to the script at the time that it was scheduled.  The argument
 *       value is returned as a boolean, and the default value is returned if
 *       the argument was not specified or could not be converted to a
 *       boolean.</LI>
 *   <LI>getScriptIntArgument(string name, int defaultValue) -- Retrieves the
 *       integer value of the specified argument that has been passed to the
 *       script at the time that it was scheduled.  The argument value is
 *       returned as an integer, and the default value is returned if the
 *       parameter was not specified or could not be converted to an
 *       integer.</LI>
 *   <LI>logMessage(string message) -- Writes the specified message to the SLAMD
 *        log.  This method does not return a value.</LI>
 *   <LI>randomInt(int lowerBound, int upperBound) -- Retrieves a random integer
 *       value between the specified lower and upper bounds (inclusive).</LI>
 *   <LI>randomString(int length) -- Retrieves a random string value with the
 *       specified number of alphabetic characters.</LI>
 *   <LI>randomString(int length, string characterSet) -- Retrieves a random
 *       string value with the specified number of characters from the provided
 *       character set.</LI>
 *   <LI>shouldNotStop() -- Indicates whether the script has not yet been
 *       requested to stop running.</LI>
 *   <LI>shouldStop() -- Indicates whether the script should stop running.</LI>
 *   <LI>sleep(int milliseconds) -- Causes the execution of the script to pause
 *       for the specified number of milliseconds.  This method does not return
 *       a value.</LI>
 *   <LI>timestamp() -- Retrieves a string value that contains a timestamp in
 *       YYYYMMDDhhmmss format.</LI>
 *   <LI>timestamp(string format) -- Retrieves a string value contains a
 *       timestamp using the specified format.  The format used should be in the
 *       form required by the <CODE>java.text.SimpleDateFormat</CODE> class.
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class ScriptVariable
       extends Variable
{
  /**
   * The set of characters that will be used to generate random string values.
   */
  public static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  /**
   * The name of the method that can be used to write debug messages to the
   * client's debug writer.
   */
  public static final String DEBUG_MESSAGE_METHOD_NAME = "debugmessage";



  /**
   * The method number for the "debugMessage" method.
   */
  public static final int DEBUG_MESSAGE_METHOD_NUMBER = 0;



  /**
   * The name of the method that will be used to stop execution of the script.
   */
  public static final String EXIT_METHOD_NAME = "exit";



  /**
   * The method number for the "exit" method.
   */
  public static final int EXIT_METHOD_NUMBER = 1;



  /**
   * The name of the method that will be used to stop execution of the script
   * and indicate that an error occurred.
   */
  public static final String EXIT_WITH_ERROR_METHOD_NAME = "exitwitherror";



  /**
   * The method number for the "exitWithError" method.
   */
  public static final int EXIT_WITH_ERROR_METHOD_NUMBER = 2;



  /**
   * The name of the method that will be used to get argument information
   * provided at the time the job was scheduled.
   */
  public static final String GET_SCRIPT_ARGUMENT_METHOD_NAME =
       "getscriptargument";



  /**
   * The method number for the first "getScriptArgument" method.
   */
  public static final int GET_SCRIPT_ARGUMENT_1_METHOD_NUMBER = 3;



  /**
   * The method number for the second "getScriptArgument" method.
   */
  public static final int GET_SCRIPT_ARGUMENT_2_METHOD_NUMBER = 4;



  /**
   * The name of the method that will be used to get the boolean value of an
   * argument provided at the time the job was scheduled.
   */
  public static final String GET_SCRIPT_BOOLEAN_ARGUMENT_METHOD_NAME =
       "getscriptbooleanargument";



  /**
   * The method number for the "getScriptBooleanArgument" method.
   */
  public static final int GET_SCRIPT_BOOLEAN_ARGUMENT_METHOD_NUMBER = 5;



  /**
   * The name of the method that will be used to get the integer value of an
   * argument provided at the time the job was scheduled.
   */
  public static final String GET_SCRIPT_INT_ARGUMENT_METHOD_NAME =
       "getscriptintargument";



  /**
   * The method number for the "getScriptIntArgument" method.
   */
  public static final int GET_SCRIPT_INT_ARGUMENT_METHOD_NUMBER = 6;



  /**
   * The name of the method that will be used to write messages to the SLAMD
   * log.
   */
  public static final String LOG_MESSAGE_METHOD_NAME = "logmessage";



  /**
   * The method number for the "logMessage" method.
   */
  public static final int LOG_MESSAGE_METHOD_NUMBER = 7;



  /**
   * The name of the method that will be used to generate a random integer
   * value.
   */
  public static final String RANDOM_INT_METHOD_NAME = "randomint";



  /**
   * The method number for the "randomInt" method.
   */
  public static final int RANDOM_INT_METHOD_NUMBER = 8;



  /**
   * The name of the method that will be used to generate a random string
   * value.
   */
  public static final String RANDOM_STRING_METHOD_NAME = "randomstring";



  /**
   * The method number for the first "randomString" method.
   */
  public static final int RANDOM_STRING_1_METHOD_NUMBER = 9;



  /**
   * The method number for the second "randomString" method.
   */
  public static final int RANDOM_STRING_2_METHOD_NUMBER = 10;



  /**
   * The name of the method that will be used to determine whether the script
   * has not yet been requested to stop running.
   */
  public static final String SHOULD_NOT_STOP_METHOD_NAME = "shouldnotstop";



  /**
   * The method number for the "shouldNotStop" method.
   */
  public static final int SHOULD_NOT_STOP_METHOD_NUMBER = 11;



  /**
   * The name of the method that will be used to determine whether the script
   * should stop running.
   */
  public static final String SHOULD_STOP_METHOD_NAME = "shouldstop";



  /**
   * The method number for the "shouldStop" method.
   */
  public static final int SHOULD_STOP_METHOD_NUMBER = 12;



  /**
   * The name of the method that will be used to pause execution of the script.
   */
  public static final String SLEEP_METHOD_NAME = "sleep";



  /**
   * The method number for the "sleep" method.
   */
  public static final int SLEEP_METHOD_NUMBER = 13;



  /**
   * The name of the method that will be used to generate a timestamp.
   */
  public static final String TIMESTAMP_METHOD_NAME = "timestamp";



  /**
   * The method number for the first "timestamp" method.
   */
  public static final int TIMESTAMP_1_METHOD_NUMBER = 14;



  /**
   * The method number for the second "timestamp" method.
   */
  public static final int TIMESTAMP_2_METHOD_NUMBER = 15;



  /**
   * The name of the method that will be used to obtain the client number.
   */
  public static final String GET_CLIENT_NUMBER_METHOD_NAME = "getclientnumber";



  /**
   * The method number for the "getClientNumber" method.
   */
  public static final int GET_CLIENT_NUMBER_METHOD_NUMBER = 16;



  /**
   * The name of the method that will be used to obtain the thread number.
   */
  public static final String GET_THREAD_NUMBER_METHOD_NAME = "getthreadnumber";



  /**
   * The method number for the "getThreadNumber" method.
   */
  public static final int GET_THREAD_NUMBER_METHOD_NUMBER = 17;



  /**
   * The set of methods associated with script variables.
   */
  public static final Method[] SCRIPT_VARIABLE_METHODS = new Method[]
  {
    new Method(DEBUG_MESSAGE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(EXIT_METHOD_NAME, new String[0], null),
    new Method(EXIT_WITH_ERROR_METHOD_NAME, new String[0], null),
    new Method(GET_SCRIPT_ARGUMENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_SCRIPT_ARGUMENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_SCRIPT_BOOLEAN_ARGUMENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_SCRIPT_INT_ARGUMENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(LOG_MESSAGE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE }, null),
    new Method(RANDOM_INT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(RANDOM_STRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(RANDOM_STRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(SHOULD_NOT_STOP_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SHOULD_STOP_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SLEEP_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE }, null),
    new Method(TIMESTAMP_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(TIMESTAMP_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_CLIENT_NUMBER_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_THREAD_NUMBER_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE)
  };



  private static Random parentRandom = new Random();



  // The job thread that is being used to run the SLAMD script.
  private JobClass jobThread;

  // The random number generator used by this variable.
  private Random random;

  // The script parser with which this script variable is associated.
  private ScriptParser parser;

  // The date formatter that will be used to obtain timestamp values.
  private SimpleDateFormat timestampFormat;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public ScriptVariable()
         throws ScriptException
  {
    // Seed the random number generator.
    random = new Random(parentRandom.nextLong());

    // Initialize the date formatter.
    timestampFormat = new SimpleDateFormat(Constants.ATTRIBUTE_DATE_FORMAT);
  }



  /**
   * Associates this instance of the script variable with the job thread that is
   * running it.
   *
   * @param  jobThread  THe job thread that is running this script.
   */
  public void setJobThread(JobClass jobThread)
  {
    this.jobThread = jobThread;
  }



  /**
   * Associates this instance of the script variable with the script parser in
   * which it is defined.
   *
   * @param  parser  The script parser with which this script variable is being
   *                 used.
   */
  public void setParser(ScriptParser parser)
  {
    this.parser = parser;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return "script";
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return SCRIPT_VARIABLE_METHODS;
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
    for (int i=0; i < SCRIPT_VARIABLE_METHODS.length; i++)
    {
      if (SCRIPT_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < SCRIPT_VARIABLE_METHODS.length; i++)
    {
      if (SCRIPT_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
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
    for (int i=0; i < SCRIPT_VARIABLE_METHODS.length; i++)
    {
      if (SCRIPT_VARIABLE_METHODS[i].hasSignature(methodName, argumentTypes))
      {
        return SCRIPT_VARIABLE_METHODS[i].getReturnType();
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
      case DEBUG_MESSAGE_METHOD_NUMBER:
        // Get the provided string value.
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();

        // Log the message and don't return a value.
        jobThread.writeVerbose(sv.getStringValue());
        return null;
      case EXIT_METHOD_NUMBER:
        // Stop the job and don't return a value.
        jobThread.stopJob(Constants.JOB_STATE_COMPLETED_SUCCESSFULLY);
        throw new StopRunningException(lineNumber);
      case EXIT_WITH_ERROR_METHOD_NUMBER:
        // Stop the job and don't return a value.
        jobThread.stopJob(Constants.JOB_STATE_COMPLETED_WITH_ERRORS);
        jobThread.indicateStoppedDueToError();
        throw new StopRunningException(lineNumber);
      case GET_SCRIPT_ARGUMENT_1_METHOD_NUMBER:
        // Determine which parameter is being requested.
        sv = (StringVariable) arguments[0].getArgumentValue();
        String argName = sv.getStringValue().toLowerCase();

        // Retrieve the requested parameter value, or a null string.
        return new StringVariable(parser.getScriptArgument(argName));
      case GET_SCRIPT_ARGUMENT_2_METHOD_NUMBER:
        // Determine which parameter is being requested and the default value to
        // use.
        sv = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        argName = sv.getStringValue().toLowerCase();

        // Retrieve the requested parameter value, or the default string.
        String value = parser.getScriptArgument(argName);
        if (value == null)
        {
          return new StringVariable(sv2.getStringValue());
        }
        else
        {
          return new StringVariable(value);
        }
      case GET_SCRIPT_BOOLEAN_ARGUMENT_METHOD_NUMBER:
        // Determine which parameter is being requested and the default value to
        // use.
        sv = (StringVariable) arguments[0].getArgumentValue();
        BooleanVariable bv = (BooleanVariable) arguments[1].getArgumentValue();
        argName = sv.getStringValue().toLowerCase();

        // Retrieve the requested parameter value, or the default string.
        value = parser.getScriptArgument(argName);
        if (value == null)
        {
          return bv;
        }
        else
        {
          try
          {
            return new BooleanVariable(Boolean.valueOf(value));
          }
          catch (Exception e)
          {
            return bv;
          }
        }
      case GET_SCRIPT_INT_ARGUMENT_METHOD_NUMBER:
        // Determine which parameter is being requested and the default value to
        // use.
        sv = (StringVariable) arguments[0].getArgumentValue();
        IntegerVariable iv = (IntegerVariable) arguments[1].getArgumentValue();
        argName = sv.getStringValue().toLowerCase();

        // Retrieve the requested parameter value, or the default string.
        value = parser.getScriptArgument(argName);
        if (value == null)
        {
          return iv;
        }
        else
        {
          try
          {
            return new IntegerVariable(Integer.parseInt(value));
          }
          catch (Exception e)
          {
            return iv;
          }
        }
      case LOG_MESSAGE_METHOD_NUMBER:
        // Get the provided string value.
        sv = (StringVariable) arguments[0].getArgumentValue();

        // Log the message and don't return a value.
        jobThread.logMessage(sv.getStringValue());
        return null;
      case RANDOM_INT_METHOD_NUMBER:
        // Get the provided integer values.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[1].getArgumentValue();
        int lowerBound = iv.getIntValue();
        int upperBound = iv2.getIntValue();

        // Generate and return the random integer.
        int span       = upperBound - lowerBound + 1;
        return new IntegerVariable(Math.abs(random.nextInt()) % span +
                                   lowerBound);
      case RANDOM_STRING_1_METHOD_NUMBER:
        // Get the provided integer value.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Generate and return the random string.
        char[] randomChars = new char[iv.getIntValue()];
        for (int i=0; i < randomChars.length; i++)
        {
          randomChars[i] = ALPHABET[Math.abs(random.nextInt()) %
                                    ALPHABET.length];
        }
        return new StringVariable(new String(randomChars));
      case RANDOM_STRING_2_METHOD_NUMBER:
        // Get the provided integer value.
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        sv = (StringVariable) arguments[1].getArgumentValue();

        // Generate and return the random string.
        char[] charSet = sv.getStringValue().toCharArray();
        randomChars    = new char[iv.getIntValue()];
        for (int i=0; i < randomChars.length; i++)
        {
          randomChars[i] = charSet[Math.abs(random.nextInt()) % charSet.length];
        }
        return new StringVariable(new String(randomChars));
      case SHOULD_NOT_STOP_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(! jobThread.shouldStop());
      case SHOULD_STOP_METHOD_NUMBER:
        // Make the determination and return the result as a Boolean value.
        return new BooleanVariable(jobThread.shouldStop());
      case SLEEP_METHOD_NUMBER:
        // Get the provided integer value.
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        // Sleep for the specified length of time and don't return a value.
        try
        {
          Thread.sleep(iv.getIntValue());
        } catch (InterruptedException ie) {}
        return null;
      case TIMESTAMP_1_METHOD_NUMBER:
        // Generate and return the timestamp.
        return new StringVariable(timestampFormat.format(new Date()));
      case TIMESTAMP_2_METHOD_NUMBER:
        // Generate and return the timestamp.
        sv = (StringVariable) arguments[0].getArgumentValue();
        SimpleDateFormat dateFormat = new SimpleDateFormat(sv.getStringValue());
        return new StringVariable(dateFormat.format(new Date()));
      case GET_CLIENT_NUMBER_METHOD_NUMBER:
        // Return the client number.
        return new IntegerVariable(jobThread.getClientNumber());
      case GET_THREAD_NUMBER_METHOD_NUMBER:
        // Return the client number.
        return new IntegerVariable(jobThread.getThreadNumber());
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
    // Script variables cannot be assigned.
    throw new ScriptException("Attempt to assign a script variable rejected.");
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    return "script";
  }
}

