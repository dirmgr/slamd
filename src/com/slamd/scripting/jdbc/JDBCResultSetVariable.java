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
 * with the results of a query executed using a JDBC connection.  The methods
 * available for use with a result set are:
 *
 * <UL>
 *   <LI>cancelRowUpdates() -- Indicates that any updates to the current row
 *       should be discarded.  This method returns a Boolean value that
 *       indicates whether the cancel was processed successfully.</LI>
 *   <LI>deleteRow() -- Deletes the current row from the result set and the
 *       underlying database.  This method returns a Boolean value that
 *       indicates whether the delete was successful.</LI>
 *   <LI>firstRow() -- Moves the cursor to the first row in the result set.
 *       This method returns a Boolean value that indicates whether the move was
 *       successful.</LI>
 *   <LI>getBoolean(int column) -- Retrieves the Boolean value from the
 *       specified column of the current row.</LI>
 *   <LI>getBoolean(string columnName) -- Retrieves the Boolean value from the
 *       specified column of the current row.
 *   <LI>getFailureReason() -- Retrieves the a string containing the reason that
 *       the last operation failed.</LI>
 *   <LI>getInteger(int column) -- Retrieves the Boolean value from the
 *       specified column of the current row.</LI>
 *   <LI>getInteger(string columnName) -- Retrieves the Boolean value from the
 *       specified column of the current row.</LI>
 *   <LI>getRow() -- Retrieves the row number of the currently selected
 *       row.</LI>
 *   <LI>getString(int column) -- Retrieves the string value from the specified
 *       column of the current row.</LI>
 *   <LI>getString(string columnName) -- Retrieves the string value from the
 *       specified column of the current row.</LI>
 *   <LI>insertRow() -- Inserts the current insert row into the result set and
 *       the database, provided that values have been given for all columns.
 *       This method returns a Boolean value that indicates whether the insert
 *       was successful.</LI>
 *   <LI>isNull() -- Indicates whether this is a null result set, which can
 *       result from a failure during an <CODE>executeQuery</CODE> method.</LI>
 *   <LI>lastRow() -- Moves the cursor to the last row in the result set.  This
 *       This method returns a Boolean value that indicates whether the move was
 *       successful.</LI>
 *   <LI>moveToCurrentRow() -- Returns to the row that was currently selected
 *       before moving to the insert row.  This method returns a Boolean value
 *       that indicates whether the move was successful.</LI>
 *   <LI>moveToInsertRow() -- Moves to a special row that may be used to insert
 *       a new record into the result set and underlying database.  This method
 *       returns a Boolean value that indicates whether the move was
 *       successful.</LI>
 *   <LI>nextRow() -- Moves the cursor to the next row in the result set.  This
 *       method returns a Boolean value that indicates whether the move was
 *       successful.</LI>
 *   <LI>setBoolean(int column, Boolean value) -- Specifies the Boolean value
 *       that should be used for the specified column of the current row.  This
 *       method returns a Boolean value that indicates whether the update was
 *       successful.</LI>
 *   <LI>setBoolean(string columnName, Boolean value) -- Specifies the Boolean
 *       value that should be used for the specified column of the current
 *       row.  This method returns a Boolean value that indicates whether the
 *       update was successful.</LI>
 *   <LI>setInteger(int column, int value) -- Specifies the integer value
 *       that should be used for the specified column of the current row.  This
 *       method returns a Boolean value that indicates whether the update was
 *       successful.</LI>
 *   <LI>setInteger(string columnName, int value) -- Specifies the integer value
 *       that should be used for the specified column of the current row.  This
 *       method returns a Boolean value that indicates whether the update was
 *       successful.</LI>
 *   <LI>setRow(int row) -- Moves to the specified row in the result set.  This
 *       method returns a Boolean value that indicates whether the move was
 *       successful.</LI>
 *   <LI>setString(int column, string value) -- Specifies the string value that
 *       should be used for the specified column of the current row.  This
 *       method returns a Boolean value that indicates whether the update was
 *       successful.</LI>
 *   <LI>setString(string columnName, string value) -- Specifies the string
 *       value that should be used for the specified column of the current row.
 *       This method returns a Boolean value that indicates whether the update
 *       was successful.</LI>
 *   <LI>updateRow() -- Writes any updates that have been made to the current
 *       row to the database.  This method returns a Boolean value that
 *       indicates whether the update was successful.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class JDBCResultSetVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of JDBC result set variables.
   */
  public static final String JDBC_RESULT_SET_VARIABLE_TYPE = "jdbcresultset";



  /**
   * The name of the method that can be used to cancel any outstanding updates
   * to the current row of the result set.
   */
  public static final String CANCEL_ROW_UPDATES_METHOD_NAME =
       "cancelrowupdates";



  /**
   * The method number for the "cancelRowUpdates" method.
   */
  public static final int CANCEL_ROW_UPDATES_METHOD_NUMBER = 0;



  /**
   * The name of the method that can be used to remove the current row from the
   * result set and database.
   */
  public static final String DELETE_ROW_METHOD_NAME = "deleterow";



  /**
   * The method number for the "deleteRow" method.
   */
  public static final int DELETE_ROW_METHOD_NUMBER = 1;



  /**
   * The name of the method that can be used to move to the first row in the
   * result set.
   */
  public static final String FIRST_ROW_METHOD_NAME = "firstrow";



  /**
   * The method number for the "firstRow" method.
   */
  public static final int FIRST_ROW_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to retrieve the Boolean value of a
   * specified column.
   */
  public static final String GET_BOOLEAN_METHOD_NAME = "getboolean";



  /**
   * The method number for the first "getBoolean" method.
   */
  public static final int GET_BOOLEAN_1_METHOD_NUMBER = 3;



  /**
   * The method number for the second "getBoolean" method.
   */
  public static final int GET_BOOLEAN_2_METHOD_NUMBER = 4;



  /**
   * The name of the method that can retrieve the reason for the last failure.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to retrieve the integer value of a
   * specified column.
   */
  public static final String GET_INTEGER_METHOD_NAME = "getinteger";



  /**
   * The method number for the first "getInteger" method.
   */
  public static final int GET_INTEGER_1_METHOD_NUMBER = 6;



  /**
   * The method number for the second "getInteger" method.
   */
  public static final int GET_INTEGER_2_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to get the row number for the
   * current row.
   */
  public static final String GET_ROW_METHOD_NAME = "getrow";



  /**
   * The method number for the "getRow" method.
   */
  public static final int GET_ROW_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to retrieve the string value of a
   * specified column.
   */
  public static final String GET_STRING_METHOD_NAME = "getstring";



  /**
   * The method number for the first "getString" method.
   */
  public static final int GET_STRING_1_METHOD_NUMBER = 9;



  /**
   * The method number for the second "getString" method.
   */
  public static final int GET_STRING_2_METHOD_NUMBER = 10;



  /**
   * The name of the method that can be used to insert a row into the row set
   * and database.
   */
  public static final String INSERT_ROW_METHOD_NAME = "insertrow";



  /**
   * The method number for the "insertRow" method.
   */
  public static final int INSERT_ROW_METHOD_NUMBER = 11;



  /**
   * The name of the method that can be used to determine whether this is a null
   * result set.
   */
  public static final String IS_NULL_METHOD_NAME = "isnull";



  /**
   * The method number for the "isNull" method.
   */
  public static final int IS_NULL_METHOD_NUMBER = 12;



  /**
   * The name of the method that can be used to move to the last row in the
   * result set.
   */
  public static final String LAST_ROW_METHOD_NAME = "lasttrow";



  /**
   * The method number for the "lastRow" method.
   */
  public static final int LAST_ROW_METHOD_NUMBER = 13;



  /**
   * The name of the method that can be used to return to the row that was
   * selected before moving to the insert row.
   */
  public static final String MOVE_TO_CURRENT_ROW_METHOD_NAME =
       "movetocurrentrow";



  /**
   * The method number for the "moveToCurrentRow" method.
   */
  public static final int MOVE_TO_CURRENT_ROW_METHOD_NUMBER = 14;



  /**
   * The name of the method that can be used to move to a special row that can
   * be used to insert a new row into the result set and database.
   */
  public static final String MOVE_TO_INSERT_ROW_METHOD_NAME = "movetoinsertrow";



  /**
   * The method number for the "moveToInsertRow" method.
   */
  public static final int MOVE_TO_INSERT_ROW_METHOD_NUMBER = 15;



  /**
   * The name of the method that can be used to move to the next row in the
   * result set.
   */
  public static final String NEXT_ROW_METHOD_NAME = "nextrow";



  /**
   * The method number for the "nextRow" method.
   */
  public static final int NEXT_ROW_METHOD_NUMBER = 16;



  /**
   * The name of the method that can be used to specify the Boolean value to use
   * for a given column of the current row.
   */
  public static final String SET_BOOLEAN_METHOD_NAME = "setboolean";



  /**
   * The method number for the first "setBoolean" method.
   */
  public static final int SET_BOOLEAN_1_METHOD_NUMBER = 17;



  /**
   * The method number for the second "setBoolean" method.
   */
  public static final int SET_BOOLEAN_2_METHOD_NUMBER = 18;



  /**
   * The name of the method that can be used to specify the integer value to use
   * for a given column of the current row.
   */
  public static final String SET_INTEGER_METHOD_NAME = "setinteger";



  /**
   * The method number for the first "setInteger" method.
   */
  public static final int SET_INTEGER_1_METHOD_NUMBER = 19;



  /**
   * The method number for the second "setInteger" method.
   */
  public static final int SET_INTEGER_2_METHOD_NUMBER = 20;



  /**
   * The name of the method that can be used to specify the current row in the
   * result set.
   */
  public static final String SET_ROW_METHOD_NAME = "setrow";



  /**
   * The method number for the "setRow" method.
   */
  public static final int SET_ROW_METHOD_NUMBER = 21;



  /**
   * The name of the method that can be used to specify the string value to use
   * for a given column of the current row.
   */
  public static final String SET_STRING_METHOD_NAME = "setstring";



  /**
   * The method number for the first "setString" method.
   */
  public static final int SET_STRING_1_METHOD_NUMBER = 22;



  /**
   * The method number for the second "setString" method.
   */
  public static final int SET_STRING_2_METHOD_NUMBER = 23;



  /**
   * The name of the method that can be used to update the current row in the
   * underlying database.
   */
  public static final String UPDATE_ROW_METHOD_NAME = "updaterow";



  /**
   * The method number for the "updateRow" method.
   */
  public static final int UPDATE_ROW_METHOD_NUMBER = 24;



  /**
   * The set of methods associated with JDBC result set variables.
   */
  public static final Method[] JDBC_RESULT_SET_VARIABLE_METHODS = new Method[]
  {
    new Method(CANCEL_ROW_UPDATES_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DELETE_ROW_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(FIRST_ROW_METHOD_NAME, new String[0],
             BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_BOOLEAN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_BOOLEAN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_INTEGER_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_INTEGER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_ROW_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_STRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_STRING_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(INSERT_ROW_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(IS_NULL_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(LAST_ROW_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(MOVE_TO_CURRENT_ROW_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(MOVE_TO_INSERT_ROW_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(NEXT_ROW_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_BOOLEAN_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_BOOLEAN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_INTEGER_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_INTEGER_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_ROW_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_STRING_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_STRING_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(UPDATE_ROW_METHOD_NAME, new String[0],
              BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };



  // The actual JDBC result set that we will use to perform all processing.
  private ResultSet resultSet;

  // The variable that holds the reason for the last failure.
  private String failureReason;

  // The SQL query that generated this result set.
  private String sqlQuery;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public JDBCResultSetVariable()
         throws ScriptException
  {
    resultSet     = null;
    sqlQuery      = null;
    failureReason = null;
  }



  /**
   * Creates a new JDBC result set variable using the provided result set.
   *
   * @param  sqlQuery   The SQL query to use for this result set variable.
   * @param  resultSet  The JDBC result set to use for this result set variable.
   */
  public JDBCResultSetVariable(String sqlQuery, ResultSet resultSet)
  {
    this.sqlQuery  = sqlQuery;
    this.resultSet = resultSet;

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
    return JDBC_RESULT_SET_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return JDBC_RESULT_SET_VARIABLE_METHODS;
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
    for (int i=0; i < JDBC_RESULT_SET_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_RESULT_SET_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < JDBC_RESULT_SET_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_RESULT_SET_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < JDBC_RESULT_SET_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_RESULT_SET_VARIABLE_METHODS[i].hasSignature(methodName,
                                                           argumentTypes))
      {
        return JDBC_RESULT_SET_VARIABLE_METHODS[i].getReturnType();
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
      case CANCEL_ROW_UPDATES_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.cancelRowUpdates();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case DELETE_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.deleteRow();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case FIRST_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(resultSet.first());
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case GET_BOOLEAN_1_METHOD_NUMBER:
        IntegerVariable iv = (IntegerVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(resultSet.getBoolean(iv.getIntValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case GET_BOOLEAN_2_METHOD_NUMBER:
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(resultSet.getBoolean(sv.getStringValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case GET_FAILURE_REASON_METHOD_NUMBER:
        return new StringVariable(failureReason);
      case GET_INTEGER_1_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new IntegerVariable(-1);
        }

        try
        {
          failureReason = null;
          return new IntegerVariable(resultSet.getInt(iv.getIntValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new IntegerVariable(-1);
        }
      case GET_INTEGER_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new IntegerVariable(-1);
        }

        try
        {
          failureReason = null;
          return new IntegerVariable(resultSet.getInt(sv.getStringValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new IntegerVariable(-1);
        }
      case GET_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new IntegerVariable(-1);
        }

        try
        {
          failureReason = null;
          return new IntegerVariable(resultSet.getRow());
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new IntegerVariable(-1);
        }
      case GET_STRING_1_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new StringVariable(null);
        }

        try
        {
          failureReason = null;
          return new StringVariable(resultSet.getString(iv.getIntValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new StringVariable(null);
        }
      case GET_STRING_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new StringVariable(null);
        }

        try
        {
          failureReason = null;
          return new StringVariable(resultSet.getString(sv.getStringValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new StringVariable(null);
        }
      case INSERT_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.insertRow();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case IS_NULL_METHOD_NUMBER:
        return new BooleanVariable(resultSet == null);
      case LAST_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(resultSet.last());
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case MOVE_TO_CURRENT_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.moveToCurrentRow();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case MOVE_TO_INSERT_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.moveToInsertRow();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case NEXT_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(resultSet.next());
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_BOOLEAN_1_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        BooleanVariable bv = (BooleanVariable) arguments[1].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateBoolean(iv.getIntValue(), bv.getBooleanValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_BOOLEAN_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        bv = (BooleanVariable) arguments[1].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateBoolean(sv.getStringValue(), bv.getBooleanValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_INTEGER_1_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[1].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateInt(iv.getIntValue(), iv2.getIntValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_INTEGER_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        iv = (IntegerVariable) arguments[1].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateInt(sv.getStringValue(), iv.getIntValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_ROW_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(resultSet.absolute(iv.getIntValue()));
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_STRING_1_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();
        sv = (StringVariable) arguments[1].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateString(iv.getIntValue(), sv.getStringValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_STRING_2_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();

        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateString(sv.getStringValue(), sv2.getStringValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case UPDATE_ROW_METHOD_NUMBER:
        if (resultSet == null)
        {
          failureReason = "The result set has not been initialized.";
          return new BooleanVariable(false);
        }

        try
        {
          resultSet.updateRow();
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
    if (! argument.getArgumentType().equals(JDBC_RESULT_SET_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                JDBC_RESULT_SET_VARIABLE_TYPE + " rejected.");
    }


    JDBCResultSetVariable jrsv =
         (JDBCResultSetVariable) argument.getArgumentValue();
    resultSet     = jrsv.resultSet;
    sqlQuery      = jrsv.sqlQuery;
    failureReason = jrsv.failureReason;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (sqlQuery == null)
    {
      return "JDBC Result Set";
    }
    else
    {
      return "JDBC Result Set for Query " + sqlQuery;
    }
  }
}

