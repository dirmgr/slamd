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
package com.slamd.scripting.jdbc;



import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that provides a set of methods for interacting
 * with a JDBC prepared statement.  Those methods are:
 *
 * <UL>
 *   <LI>executeQuery() -- Executes the associated query and returns the result
 *       set of matching records.</LI>
 *   <LI>executeUpdate() -- Executes the associated update and returns an
 *       integer value that contains the row count for the update.</LI>
 *   <LI>getFailureReason() -- Retrieves a string containing the reason that the
 *       last operation failed.</LI>
 *   <LI>isNull() -- Retrieves a Boolean value that indicates whether this is a
 *       null prepared statement.</LI>
 *   <LI>setBoolean(int index, Boolean value) -- Updates the SQL for this
 *       prepared statement to place the provided Boolean value in the position
 *       of the specified placeholder.  This method returns a Boolean value that
 *       indicates whether the update was successful.</LI>
 *   <LI>setInteger(int index, int value) -- Updates the SQL for this prepared
 *       statement to place the provided integer value in the position of the
 *       specified placeholder.    This method returns a Boolean value that
 *       indicates whether the update was successful.</LI>
 *   <LI>setString(int index, string value) -- Updates the SQL for this prepared
 *       statement to place the provided string value in the position of the
 *       specified placeholder.    This method returns a Boolean value that
 *       indicates whether the update was successful.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class JDBCPreparedStatementVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of JDBC prepared statement
   *  variables.
   */
  public static final String JDBC_PREPARED_STATEMENT_VARIABLE_TYPE =
       "jdbcpreparedstatement";



  /**
   * The name of the method that can be used to execute a query using this
   * prepared statement.
   */
  public static final String EXECUTE_QUERY_METHOD_NAME = "executequery";



  /**
   * The method number for the "executeQuery" method.
   */
  public static final int EXECUTE_QUERY_METHOD_NUMBER = 0;



  /**
   * The name of the method that can be used to execute an update using this
   * prepared statement.
   */
  public static final String EXECUTE_UPDATE_METHOD_NAME = "executeupdate";



  /**
   * The method number for the "executeUpdate" method.
   */
  public static final int EXECUTE_UPDATE_METHOD_NUMBER = 1;



  /**
   * The name of the method that can be used to determine the reason for the
   * last failure.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to determine whether this is a null
   * prepared statement.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to specify the boolean value to use
   * in place of a given placeholder.
   */
  public static final String SET_BOOLEAN_METHOD_NAME = "setboolean";



  /**
   * The method number for the "setBoolean" method.
   */
  public static final int SET_BOOLEAN_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to specify the integer value to use
   * in place of a given placeholder.
   */
  public static final String SET_INTEGER_METHOD_NAME = "setinteger";



  /**
   * The method number for the "setInteger" method.
   */
  public static final int SET_INTEGER_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to specify the string value to use
   * in place of a given placeholder.
   */
  public static final String SET_STRING_METHOD_NAME = "setstring";



  /**
   * The method number for the "setString" method.
   */
  public static final int SET_STRING_METHOD_NUMBER = 6;



  /**
   * The set of methods associated with JDBC prepared statement variables.
   */
  public static final Method[] JDBC_PREPARED_STATEMENT_VARIABLE_METHODS =
       new Method[]
  {
    new Method(EXECUTE_QUERY_METHOD_NAME, new String[0],
               JDBCResultSetVariable.JDBC_RESULT_SET_VARIABLE_TYPE),
    new Method(EXECUTE_UPDATE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_BOOLEAN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_INTEGER_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_STRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };



  // The actual JDBC prepared statement that we will use to perform all
  // processing.
  private PreparedStatement preparedStatement;

  // The variable that holds the reason for the last failure.
  private String failureReason;

  // The SQL statement associated with this prepared statement variable.
  private String sql;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public JDBCPreparedStatementVariable()
         throws ScriptException
  {
    preparedStatement = null;
    sql               = null;
    failureReason     = null;
  }



  /**
   * Creates a new JDBC prepared statement variable using the provided
   * information.
   *
   * @param  sql                The SQL statement to use for this prepared
   *                            statement.
   * @param  preparedStatement  The prepared statement to use for this
   *                            prepared statement.
   */
  public JDBCPreparedStatementVariable(String sql,
                                       PreparedStatement preparedStatement)
  {
    this.sql               = sql;
    this.preparedStatement = preparedStatement;

    failureReason = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return JDBC_PREPARED_STATEMENT_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return JDBC_PREPARED_STATEMENT_VARIABLE_METHODS;
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
    for (int i=0; i < JDBC_PREPARED_STATEMENT_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_PREPARED_STATEMENT_VARIABLE_METHODS[i].getName().equals(
                                                           methodName))
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
    for (int i=0; i < JDBC_PREPARED_STATEMENT_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_PREPARED_STATEMENT_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < JDBC_PREPARED_STATEMENT_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_PREPARED_STATEMENT_VARIABLE_METHODS[i].hasSignature(methodName,
                                                           argumentTypes))
      {
        return JDBC_PREPARED_STATEMENT_VARIABLE_METHODS[i].getReturnType();
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
      case EXECUTE_QUERY_METHOD_NUMBER:
        if (preparedStatement == null)
        {
          failureReason = "The prepared statement has not been initialized.";
          return new JDBCResultSetVariable(sql, null);
        }

        try
        {
          ResultSet resultSet = preparedStatement.executeQuery();
          failureReason = null;
          return new JDBCResultSetVariable(sql, resultSet);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new JDBCResultSetVariable(sql, null);
        }
      case EXECUTE_UPDATE_METHOD_NUMBER:
        if (preparedStatement == null)
        {
          failureReason = "The prepared statement has not been initialized.";
          return new IntegerVariable(-1);
        }

        try
        {
          int rowCount = preparedStatement.executeUpdate();
          failureReason = null;
          return new IntegerVariable(rowCount);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new IntegerVariable(-1);
        }
      case GET_FAILURE_REASON_METHOD_NUMBER:
        return new StringVariable(failureReason);
      case IS_NULL_METHOD_NUMBER:
        failureReason = null;
        return new BooleanVariable(preparedStatement == null);
      case SET_BOOLEAN_METHOD_NUMBER:
        IntegerVariable iv = (IntegerVariable) arguments[0].getArgumentValue();
        BooleanVariable bv = (BooleanVariable) arguments[1].getArgumentValue();

        if (preparedStatement == null)
        {
          failureReason = "The prepared statement has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          preparedStatement.setBoolean(iv.getIntValue(), bv.getBooleanValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_INTEGER_METHOD_NUMBER:
        IntegerVariable iv1 = (IntegerVariable) arguments[0].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        if (preparedStatement == null)
        {
          failureReason = "The prepared statement has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          preparedStatement.setInt(iv1.getIntValue(), iv2.getIntValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_STRING_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        StringVariable sv = (StringVariable) arguments[1].getArgumentValue();

        if (preparedStatement == null)
        {
          failureReason = "The prepared statement has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          preparedStatement.setString(iv.getIntValue(), sv.getStringValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
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
               JDBC_PREPARED_STATEMENT_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                JDBC_PREPARED_STATEMENT_VARIABLE_TYPE +
                                " rejected.");
    }


    JDBCPreparedStatementVariable jpsv =
         (JDBCPreparedStatementVariable) argument.getArgumentValue();
    preparedStatement = jpsv.preparedStatement;
    sql               = jpsv.sql;
    failureReason     = jpsv.failureReason;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (sql == null)
    {
      return "JDBC Prepared Statement";
    }
    else
    {
      return "JDBC Prepared Statement " + sql;
    }
  }
}

