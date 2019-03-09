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



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.slamd.common.Constants;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that provides an interface to the JDBC
 * <CODE>Connection</CODE> object that can be used to interact with relational
 * databases.  A JDBC connection variable provides the following methods:
 *
 * <UL>
 *   <LI>commit() -- Attempts to perform a commit in the underlying database,
 *       provided that autocommit is disabled.
 *       This method returns a Boolean value that indicates whether the commit
 *       was successful.</LI>
 *   <LI>connect(string jdbcURL) -- Attempts to establish a connection to the
 *       database using the provided URL.  This method returns a Boolean value
 *       that indicates whether the connection was established
 *       successfully.</LI>
 *   <LI>connect(string jdbcURL, string username, string password) -- Attempts
 *       to establish a connection to the database using the provided
 *       information.  This method returns a Boolean value that indicates
 *       whether the connection was established successfully.</LI>
 *   <LI>disconnect() -- Closes the connection to the database.  This method
 *       does not return a value.</LI>
 *   <LI>executeQuery(string sql) -- Executes the query specified in the
 *       provided SQL and returns a result set containing the results of the
 *       query.</LI>
 *   <LI>executeUpdate(string sql) -- Executes the update specified in the
 *       provided SQL and returns an integer value that contains the row count
 *       for the update.</LI>
 *   <LI>getAutoCommit() -- Retrieves a Boolean value that indicates whether
 *       autocommit is currently enabled.</LI>
 *   <LI>getFailureReason() -- Retrieves the a string that provides information
 *       about the reason that the last operation failed.</LI>
 *   <LI>getLoginTimeout() -- Retrieves in integer that indicates the maximum
 *       length of time in seconds that the client will wait while trying to
 *       establish a connection to the database.</LI>
 *   <LI>getQueryTimeout() -- Retrieves an integer that indicates the maximum
 *       length of time in seconds that the client will wait while trying to
 *       execute a query or an update.</LI>
 *   <LI>isConnected() -- Retrieves a Boolean value that indicates whether the
 *       connection to the database is currently established.</LI>
 *   <LI>prepareStatement(string sql) -- Retrieves a prepared statement that
 *       may be used to perform the same kind of operation multiple times with
 *       certain fields replaced.</LI>
 *   <LI>rollBack() -- Attempts to perform a rollback to the last commit,
 *       provided that autocommit is disabled.  This method returns a Boolean
 *       value that indicates whether the rollback was successful.</LI>
 *   <LI>setAutoCommit(Boolean autoCommit) -- Specifies whether operations
 *       performed on this connection will be automatically committed.  This
 *       method returns a Boolean value that indicates whether the change
 *       was successful.</LI>
 *   <LI>setDriverClass(string class) -- Specifies the fully-qualified Java
 *       class name of the JDBC driver to use.  This method returns a Boolean
 *       value that indicates whether the driver class was loaded
 *       successfully.</LI>
 *   <LI>setLoginTimeout(int seconds) -- Specifies the maximum length of time in
 *       seconds that the client should wait while trying to establish a
 *       connection to the database.  This method returns a Boolean value that
 *       indicates whether the timeout was set successfully.</LI>
 *   <LI>setQueryTimeout(int seconds) -- Specifies the maximum length of time in
 *       seconds that the client should wait while trying to execute a query or
 *       an update.  This method returns a Boolean value that indicates whether
 *       the timeout was set successfully.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class JDBCConnectionVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of JDBC connection variables.
   */
  public static final String JDBC_CONNECTION_VARIABLE_TYPE = "jdbcconnection";



  /**
   * The name of the method that can be used to commit outstanding changes to
   * the database.
   */
  public static final String COMMIT_METHOD_NAME = "commit";



  /**
   * The method number for the "commit" method.
   */
  public static final int COMMIT_METHOD_NUMBER = 0;



  /**
   * The name of the method that can be used to establish connections to a
   * database.
   */
  public static final String CONNECT_METHOD_NAME = "connect";



  /**
   * The method number for the first "connect" method.
   */
  public static final int CONNECT_1_METHOD_NUMBER = 1;



  /**
   * The method number for the second "connect" method.
   */
  public static final int CONNECT_2_METHOD_NUMBER = 2;



  /**
   * The name of the method that can be used to disconnect from the database.
   */
  public static final String DISCONNECT_METHOD_NAME = "disconnect";



  /**
   * The method number for the "close" method.
   */
  public static final int DISCONNECT_METHOD_NUMBER = 3;



  /**
   * The name of the method that can be used to execute a query in the database.
   */
  public static final String EXECUTE_QUERY_METHOD_NAME = "executequery";



  /**
   * The method number for the "executeQuery" method.
   */
  public static final int EXECUTE_QUERY_METHOD_NUMBER = 4;



  /**
   * The name of the method that can be used to execute an update in the
   * database.
   */
  public static final String EXECUTE_UPDATE_METHOD_NAME = "executeupdate";



  /**
   * The method number for the "executeUpdate" method.
   */
  public static final int EXECUTE_UPDATE_METHOD_NUMBER = 5;



  /**
   * The name of the method that can be used to determine whether autocommit is
   * enabled in the database.
   */
  public static final String GET_AUTO_COMMIT_METHOD_NAME = "getautocommit";



  /**
   * The method number for the "getAutoCommit" method.
   */
  public static final int GET_AUTO_COMMIT_METHOD_NUMBER = 6;



  /**
   * The name of the method that can be used to retrieve information about the
   * reason that the last operation failed.
   */
  public static final String GET_FAILURE_REASON_METHOD_NAME =
       "getfailurereason";



  /**
   * The method number for the "getFailureReason" method.
   */
  public static final int GET_FAILURE_REASON_METHOD_NUMBER = 7;



  /**
   * The name of the method that can be used to retrieve the login timeout that
   * has been configured.
   */
  public static final String GET_LOGIN_TIMEOUT_METHOD_NAME = "getlogintimeout";



  /**
   * The method number for the "getLoginTimeout" method.
   */
  public static final int GET_LOGIN_TIMEOUT_METHOD_NUMBER = 8;



  /**
   * The name of the method that can be used to retrieve the query timeout that
   * has been configured.
   */
  public static final String GET_QUERY_TIMEOUT_METHOD_NAME = "getquerytimeout";



  /**
   * The method number for the "getQueryTimeout" method.
   */
  public static final int GET_QUERY_TIMEOUT_METHOD_NUMBER = 9;



  /**
   * The name of the method that can be used to determine whether this
   * connection is currently established.
   */
  public static final String IS_CONNECTED_METHOD_NAME = "isconnected";



  /**
   * The method number for the "isConnected" method.
   */
  public static final int IS_CONNECTED_METHOD_NUMBER = 10;



  /**
   * The name of the method that can be used to create a prepared statement.
   */
  public static final String PREPARE_STATEMENT_METHOD_NAME = "preparestatement";



  /**
   * The method number for the "prepareStatement" method.
   */
  public static final int PREPARE_STATEMENT_METHOD_NUMBER = 11;



  /**
   * The name of the method that can be used to roll back any outstanding
   * changes.
   */
  public static final String ROLL_BACK_METHOD_NAME = "rollback";



  /**
   * The method number for the "rollBack" method.
   */
  public static final int ROLL_BACK_METHOD_NUMBER = 12;



  /**
   * The name of the method that can be used to enable or disable autocommit in
   * the database.
   */
  public static final String SET_AUTO_COMMIT_METHOD_NAME = "setautocommit";



  /**
   * The method number for the "setAutoCommit" method.
   */
  public static final int SET_AUTO_COMMIT_METHOD_NUMBR = 13;



  /**
   * The name of the method that can be used to specify the JDBC driver class to
   * use.
   */
  public static final String SET_DRIVER_CLASS_METHOD_NAME = "setdriverclass";



  /**
   * The method number for the "setDriverClass" method.
   */
  public static final int SET_DRIVER_CLASS_METHOD_NUMBER = 14;



  /**
   * The name of the method that can be used to specify the login timeout.
   */
  public static final String SET_LOGIN_TIMEOUT_METHOD_NAME = "setlogintimeout";



  /**
   * The method number for the "setLoginTimeout" method.
   */
  public static final int SET_LOGIN_TIMEOUT_METHOD_NUMBER = 15;



  /**
   * The name of the method that can be used to specify the query timeout.
   */
  public static final String SET_QUERY_TIMEOUT_METHOD_NAME = "setquerytimeout";



  /**
   * The method number for the "setQueryTimeout" method.
   */
  public static final int SET_QUERY_TIMEOUT_METHOD_NUMBER = 16;



  /**
   * The set of methods associated with JDBC connection variables.
   */
  public static final Method[] JDBC_CONNECTION_VARIABLE_METHODS = new Method[]
  {
    new Method(COMMIT_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(DISCONNECT_METHOD_NAME, new String[0], null),
    new Method(EXECUTE_QUERY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               JDBCResultSetVariable.JDBC_RESULT_SET_VARIABLE_TYPE),
    new Method(EXECUTE_UPDATE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_AUTO_COMMIT_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(GET_FAILURE_REASON_METHOD_NAME, new String[0],
               StringVariable.STRING_VARIABLE_TYPE),
    new Method(GET_LOGIN_TIMEOUT_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(GET_QUERY_TIMEOUT_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(IS_CONNECTED_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(PREPARE_STATEMENT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               JDBCPreparedStatementVariable.
                    JDBC_PREPARED_STATEMENT_VARIABLE_TYPE),
    new Method(ROLL_BACK_METHOD_NAME, new String[0],
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_AUTO_COMMIT_METHOD_NAME,
               new String[] { BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_DRIVER_CLASS_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_LOGIN_TIMEOUT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE),
    new Method(SET_QUERY_TIMEOUT_METHOD_NAME,
               new String[] { IntegerVariable.INTEGER_VARIABLE_TYPE },
               BooleanVariable.BOOLEAN_VARIABLE_TYPE)
  };



  // The actual JDBC connection that we will use to perform all processing.
  private Connection jdbcConnection;

  // The maximum length of time to wait for a query or update.
  private int queryTimeout;

  // The URL used to establish the JDBC connection.
  private String connectionURL;

  // The variable that holds the reason for the last failure.
  private String failureReason;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public JDBCConnectionVariable()
         throws ScriptException
  {
    jdbcConnection = null;
    queryTimeout   = 0;
    connectionURL  = null;
    failureReason  = null;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return JDBC_CONNECTION_VARIABLE_TYPE;
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return JDBC_CONNECTION_VARIABLE_METHODS;
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
    for (int i=0; i < JDBC_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_CONNECTION_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < JDBC_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < JDBC_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (JDBC_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
                                                           argumentTypes))
      {
        return JDBC_CONNECTION_VARIABLE_METHODS[i].getReturnType();
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
      case COMMIT_METHOD_NUMBER:
        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new BooleanVariable(false);
        }

        try
        {
          jdbcConnection.commit();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case CONNECT_1_METHOD_NUMBER:
        StringVariable sv = (StringVariable) arguments[0].getArgumentValue();
        try
        {
          jdbcConnection = DriverManager.getConnection(sv.getStringValue());
          connectionURL  = sv.getStringValue();
          failureReason  = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason  = "Caught Exception:  " + e;
          jdbcConnection = null;
          connectionURL  = null;
          return new BooleanVariable(false);
        }
      case CONNECT_2_METHOD_NUMBER:
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        StringVariable sv3 = (StringVariable) arguments[2].getArgumentValue();

        try
        {
          jdbcConnection = DriverManager.getConnection(sv1.getStringValue(),
                                                       sv2.getStringValue(),
                                                       sv3.getStringValue());
          connectionURL  = sv1.getStringValue();
          failureReason  = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason  = "Caught Exception:  " + e;
          jdbcConnection = null;
          connectionURL  = null;
          return new BooleanVariable(false);
        }
      case DISCONNECT_METHOD_NUMBER:
        if (jdbcConnection != null)
        {
          try
          {
            jdbcConnection.close();
          } catch (Exception e) {}
        }

        jdbcConnection = null;
        connectionURL  = null;
        failureReason  = null;
        return null;
      case EXECUTE_QUERY_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new JDBCResultSetVariable(sv.getStringValue(), null);
        }

        try
        {
          Statement statement = jdbcConnection.createStatement();
          statement.setQueryTimeout(queryTimeout);
          ResultSet resultSet = statement.executeQuery(sv.getStringValue());
          statement.close();

          failureReason = null;
          return new JDBCResultSetVariable(sv.getStringValue(), resultSet);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new JDBCResultSetVariable(sv.getStringValue(), null);
        }
      case EXECUTE_UPDATE_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new IntegerVariable(-1);
        }

        try
        {
          Statement statement = jdbcConnection.createStatement();
          statement.setQueryTimeout(queryTimeout);
          int rowsUpdated = statement.executeUpdate(sv.getStringValue());
          statement.close();
          failureReason = null;
          return new IntegerVariable(rowsUpdated);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new IntegerVariable(-1);
        }
      case GET_AUTO_COMMIT_METHOD_NUMBER:
        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new BooleanVariable(false);
        }

        try
        {
          failureReason = null;
          return new BooleanVariable(jdbcConnection.getAutoCommit());
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case GET_FAILURE_REASON_METHOD_NUMBER:
        return new StringVariable(failureReason);
      case GET_LOGIN_TIMEOUT_METHOD_NUMBER:
        failureReason = null;
        return new IntegerVariable(DriverManager.getLoginTimeout());
      case GET_QUERY_TIMEOUT_METHOD_NUMBER:
        failureReason = null;
        return new IntegerVariable(queryTimeout);
      case IS_CONNECTED_METHOD_NUMBER:
        if (jdbcConnection == null)
        {
          failureReason = null;
          return new BooleanVariable(false);
        }

        try
        {
          boolean isClosed = jdbcConnection.isClosed();
          if (isClosed)
          {
            jdbcConnection = null;
            connectionURL  = null;
          }

          return new BooleanVariable(! isClosed);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case PREPARE_STATEMENT_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new JDBCPreparedStatementVariable(sv.getStringValue(), null);
        }

        try
        {
          PreparedStatement preparedStatement =
               jdbcConnection.prepareStatement(sv.getStringValue());
          preparedStatement.setQueryTimeout(queryTimeout);
          failureReason = null;
          return new JDBCPreparedStatementVariable(sv.getStringValue(),
                                                   preparedStatement);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new JDBCPreparedStatementVariable(sv.getStringValue(), null);
        }
      case ROLL_BACK_METHOD_NUMBER:
        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new BooleanVariable(false);
        }

        try
        {
          jdbcConnection.rollback();
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_AUTO_COMMIT_METHOD_NUMBR:
        BooleanVariable bv = (BooleanVariable) arguments[0].getArgumentValue();

        if (jdbcConnection == null)
        {
          failureReason = "The JDBC connection is not established.";
          return new BooleanVariable(false);
        }

        try
        {
          jdbcConnection.setAutoCommit(bv.getBooleanValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_DRIVER_CLASS_METHOD_NUMBER:
        sv = (StringVariable) arguments[0].getArgumentValue();

        try
        {
          Class<?> driverClass = Constants.classForName(sv.getStringValue());
          Class<?> driverSuperclass = Constants.classForName("java.sql.Driver");
          if (! driverSuperclass.isAssignableFrom(driverClass))
          {
            failureReason = "Class " + sv.getStringValue() +
                            " is not a valid JDBC driver class";
            return new BooleanVariable(false);
          }

          failureReason = null;
          return new BooleanVariable(true);
        }
        catch (Exception e)
        {
          failureReason = "Caught Exception:  " + e;
          return new BooleanVariable(false);
        }
      case SET_LOGIN_TIMEOUT_METHOD_NUMBER:
        IntegerVariable iv = (IntegerVariable) arguments[0].getArgumentValue();

        if (iv.getIntValue() >= 0)
        {
          DriverManager.setLoginTimeout(iv.getIntValue());
          failureReason = null;
          return new BooleanVariable(true);
        }
        else
        {
          failureReason = "The login timeout must be greater than or equal " +
                          "to zero.";
          return new BooleanVariable(false);
        }
      case SET_QUERY_TIMEOUT_METHOD_NUMBER:
        iv = (IntegerVariable) arguments[0].getArgumentValue();

        if (iv.getIntValue() >= 0)
        {
          queryTimeout = iv.getIntValue();
          failureReason = null;
          return new BooleanVariable(true);
        }
        else
        {
          failureReason = "The login timeout must be greater than or equal " +
                          "to zero.";
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
    if (! argument.getArgumentType().equals(JDBC_CONNECTION_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                JDBC_CONNECTION_VARIABLE_TYPE + " rejected.");
    }


    JDBCConnectionVariable jcv =
         (JDBCConnectionVariable) argument.getArgumentValue();
    jdbcConnection = jcv.jdbcConnection;
    queryTimeout   = jcv.queryTimeout;
    connectionURL  = jcv.connectionURL;
    failureReason  = jcv.failureReason;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (connectionURL == null)
    {
      return "JDBC Connection (not established)";
    }
    else
    {
      return "JDBC Connection " + connectionURL;
    }
  }
}

