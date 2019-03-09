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
package com.slamd.jobs;



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import com.slamd.common.Constants;
import com.slamd.job.JobClass;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;

import com.unboundid.util.FixedRateBarrier;



/**
 * This class implements a SLAMD job that can be used to measure the update
 * performance of an SQL database.  It should work with any database for which a
 * JDBC driver exists.
 *
 *
 * @author   Neil A. Wilson
 */
public class SQLModRateJobClass
       extends JobClass
{
  /**
   * The display name of the stat tracker that counts exceptions caught while
   * processing queries.
   */
  public static final String STAT_TRACKER_EXCEPTIONS_CAUGHT =
       "Exceptions Caught";



  /**
   * The display name of the stat tracker that tracks the rate at which updates
   * are able to be processed.
   */
  public static  final String STAT_TRACKER_UPDATES_COMPLETED =
       "Updates Completed";



  /**
   * The display name of the stat tracker that tracks the average length of time
   * required to process an update.
   */
  public static final String STAT_TRACKER_UPDATE_DURATION =
       "Update Duration (ms)";



  /**
   * The characters that are available for use in the randomly-generated values.
   */
  public static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  // The parameter that indicates whether to always disconnect from the DB.
  private BooleanParameter disconnectParameter =
       new BooleanParameter("disconnect", "Always Disconnect",
                            "Indicates whether the connection to the " +
                            "database should be dropped after each update.",
                            false);

  // The parameter that specifies the cool down time.
  private IntegerParameter coolDownParameter =
       new IntegerParameter("cool_down", "Cool Down Time (s)",
                            "Specifies the length of time in seconds before " +
                            "the job ends that it should stop collecting " +
                            "statistics.", true, 0, true, 0, false, 0);

  // The parameter that specifies the number of iterations to perform.
  private IntegerParameter iterationsParameter =
       new IntegerParameter("iterations", "Number of Iterations",
                            "The number of updates to perform before ending " +
                            "the job.", false, -1, true, -1, false, 0);

  // The parameter that specifies the maximum request rate.
  private IntegerParameter maxRateParameter = new IntegerParameter("maxRate",
       "Max Request Rate (Requests/Second/Client)",
       "Specifies the maximum request rate (in requests per second per " +
            "client) to attempt to maintain.  If multiple clients are used, " +
            "then each client will attempt to maintain this rate.  A value " +
            "less than or equal to zero indicates that the client should " +
            "attempt to perform requests as quickly as possible.",
       true, -1);

  // The parameter that specifies the interval over which to enforce the maximum
  // request rate.
  private IntegerParameter rateLimitDurationParameter = new IntegerParameter(
       "maxRateDuration", "Max Rate Enforcement Interval (Seconds)",
       "Specifies the duration in seconds of the interval over which  to " +
            "attempt to maintain the configured maximum rate.  A value of " +
            "zero indicates that it should be equal to the statistics " +
            "collection interval.  Large values may allow more variation but " +
            "may be more accurate over time.  Small values can better " +
            "ensure that the rate doesn't exceed the requested level but may " +
            "be less able to achieve the desired rate.",
       true, 0, true,0, false, 0);

  // The parameter that specifies the time between updates.
  private IntegerParameter timeBetweenUpdatesParameter =
       new IntegerParameter("time_between_updates", "Time Between Updates (ms)",
                            "Specifies the length of time in milliseconds " +
                            "that should be allowed between updates.  Note " +
                            "that this time is measured between the " +
                            "beginning of one update and the beginning of " +
                            "the next rather than the end of one and the " +
                            "beginning of the next.", true, 0, true, 0, false,
                            0);

  // The parameter that specifies the length to use for the generated value.
  private IntegerParameter valueLengthParameter =
       new IntegerParameter("value_length", "Generated Value Length",
                            "Specifies the number of characters to include " +
                            "in the randomly-generated value to use in the " +
                            "update.", true, 20, true, 1, false, 0);

  // The parameter that specifies the warm up time.
  private IntegerParameter warmUpParameter =
       new IntegerParameter("warm_up", "Warm Up Time (s)",
                            "Specifies the length of time in seconds after " +
                            "the job starts that it should begin collecting " +
                            "statistics.", true, 0, true, 0, false, 0);

  // The parameter that specifies the password to use to connect to the DB.
  private PasswordParameter passwordParameter =
       new PasswordParameter("password", "User Password",
                             "The password for the user account to use to " +
                             "connect to the database.", false, "");

  // A placeholder used for spacing in the admin interface.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter that specifies the Java class that provides the JDBC driver.
  private StringParameter driverClassParameter =
       new StringParameter("driver_class", "JDBC Driver Class",
                           "The fully-qualified Java class that provides " +
                           "the JDBC interface to the SQL database.", true, "");

  // The parameter that specifies the JDBC URL to connect to the database.
  private StringParameter jdbcURLParameter =
       new StringParameter("jdbc_url", "JDBC URL",
                           "The URL that specifies the information to use to " +
                           "connect to the SQL database.", true, "");

  // The parameter that specifies the database column to match.
  private StringParameter matchColumnParameter =
       new StringParameter("match_column", "Column to Match",
                           "The name of the database column to query in " +
                           "order to find the records to update.", true, "");

  // The parameter that specifies the text to use to perform the match.
  private StringParameter matchValueParameter =
       new StringParameter("match_value", "Match Value",
                           "The value or value pattern to use to find the " +
                           "records to update.  It may optionally include a " +
                           "bracketed pair of integers separated by a dash " +
                           "(for random access) or a colon (for sequential " +
                           "access) to alter the query each time it is " +
                           "issued.", true, "");

  // The parameter that specifies the name of the table to use.
  private StringParameter tableNameParameter =
       new StringParameter("table_name", "Table Name",
                           "The of the table in the database in which the " +
                           "updates should be performed.", true, "");

  // The parameter that specifies the database column to update.
  private StringParameter updateColumnParameter =
       new StringParameter("update_column", "Column to Update",
                           "The name of the database column to replace with " +
                           "a randomly generated string in matching records.",
                           true, "");

  // The parameter that specifies the username to use to connect to the DB.
  private StringParameter userNameParameter =
       new StringParameter("username", "User Name",
                           "The username for the account to use to connect " +
                           "to the database.", false, "");



  // Variables that correspond to parameter values.
  private static boolean alwaysDisconnect;
  private static boolean useRange;
  private static boolean useSequential;
  private static int     coolDownTime;
  private static int     iterations;
  private static int     rangeMin;
  private static int     rangeMax;
  private static int     rangeSpan;
  private static int     sequentialCounter;
  private static int     timeBetweenUpdates;
  private static int     valueLength;
  private static int     warmUpTime;
  private static String  driverClass;
  private static String  jdbcURL;
  private static String  matchColumn;
  private static String  tableName;
  private static String  updateColumn;
  private static String  userName;
  private static String  userPassword;
  private static String  valueInitial;
  private static String  valueFinal;


  // Variables used in generating random numbers.
  private static Random parentRandom;
  private Random random;


  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;


  // A variable representing the connection to the database.
  private Connection connection;


  // Variables used for tracking statistics.
  private IncrementalTracker  exceptionsCaught;
  private IncrementalTracker  updatesCompleted;
  private TimeTracker         updateTimer;



  /**
   * Creates a new instance of this SQL SearchRate job.
   */
  public SQLModRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "SQL ModRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Repeatedly update information in an SQL database";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to repeatedly update information in an SQL " +
      "database to generate load and measure performance."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "SQL";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    Parameter[] parameters = new Parameter[]
    {
      placeholder,
      driverClassParameter,
      jdbcURLParameter,
      userNameParameter,
      passwordParameter,
      placeholder,
      tableNameParameter,
      updateColumnParameter,
      valueLengthParameter,
      matchColumnParameter,
      matchValueParameter,
      placeholder,
      warmUpParameter,
      coolDownParameter,
      timeBetweenUpdatesParameter,
      maxRateParameter,
      rateLimitDurationParameter,
      iterationsParameter,
      disconnectParameter
    };

    return new ParameterList(parameters);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[]
    {
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_UPDATES_COMPLETED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_UPDATE_DURATION,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_EXCEPTIONS_CAUGHT,
                             collectionInterval)
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    return new StatTracker[]
    {
      updatesCompleted,
      updateTimer,
      exceptionsCaught
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean providesParameterTest()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean testJobParameters(ParameterList parameters,
                                   ArrayList<String> outputMessages)
  {
    // Get the necessary parameter values.
    StringParameter driverParam =
         parameters.getStringParameter(driverClassParameter.getName());
    if ((driverParam == null) || (! driverParam.hasValue()))
    {
      outputMessages.add("ERROR:  No JDBC driver class provided.");
      return false;
    }
    String driverClass = driverParam.getStringValue();


    StringParameter urlParam =
         parameters.getStringParameter(jdbcURLParameter.getName());
    if ((urlParam == null) || (! urlParam.hasValue()))
    {
      outputMessages.add("ERROR:  No JDBC URL provided.");
      return false;
    }
    String jdbcURL = urlParam.getStringValue();


    String userName = "";
    StringParameter usernameParam =
         parameters.getStringParameter(userNameParameter.getName());
    if ((usernameParam != null) && usernameParam.hasValue())
    {
      userName = usernameParam.getStringValue();
    }


    String userPW = "";
    PasswordParameter pwParam =
         parameters.getPasswordParameter(passwordParameter.getName());
    if ((pwParam != null) && pwParam.hasValue())
    {
      userPW = pwParam.getStringValue();
    }


    // Try to load the JDBC driver.
    try
    {
      outputMessages.add("Trying to load JDBC driver class '" + driverClass +
                         "'....");
      Constants.classForName(driverClass);
      outputMessages.add("Driver class loaded successfully.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to load driver class:  " +
                         stackTraceToString(e));
      return false;
    }


    // Try to establish a connection to the database using the JDBC URL,
    // username, and password.
    try
    {
      outputMessages.add("Trying to connect to database using JDBC URL '" +
                         jdbcURL + "' as user '" + userName + "'....");

      Connection connection = DriverManager.getConnection(jdbcURL, userName,
                                                          userPW);

      outputMessages.add("Connected successfully.");
      outputMessages.add("");

      try
      {
        connection.close();
      } catch (Exception e) {}

      outputMessages.add("Connection closed.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to connect:  " +
                         stackTraceToString(e));
      return false;
    }

    outputMessages.add("All tests completed.");
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
  {
    // Get the database driver class.
    driverClass = null;
    driverClassParameter =
         parameters.getStringParameter(driverClassParameter.getName());
    if (driverClassParameter != null)
    {
      driverClass = driverClassParameter.getStringValue();
    }

    // Get the JDBC URL to use to connect to the database.
    jdbcURL = null;
    jdbcURLParameter =
         parameters.getStringParameter(jdbcURLParameter.getName());
    if (jdbcURLParameter != null)
    {
      jdbcURL = jdbcURLParameter.getStringValue();
    }

    // Get the username to use to connect to the database.
    userName = "";
    userNameParameter =
         parameters.getStringParameter(userNameParameter.getName());
    if ((userNameParameter != null) && userNameParameter.hasValue())
    {
      userName = userNameParameter.getStringValue();
    }

    // Get the password to use to connect to the database.
    userPassword = "";
    passwordParameter =
         parameters.getPasswordParameter(passwordParameter.getName());
    if ((passwordParameter != null) && passwordParameter.hasValue())
    {
      userPassword = passwordParameter.getStringValue();
    }

    // Get the name of the table in which to make the update.
    tableName = null;
    tableNameParameter =
         parameters.getStringParameter(tableNameParameter.getName());
    if (tableNameParameter != null)
    {
      tableName = tableNameParameter.getStringValue();
    }

    // Get the name of the column to update.
    updateColumn = null;
    updateColumnParameter =
         parameters.getStringParameter(updateColumnParameter.getName());
    if (updateColumnParameter != null)
    {
      updateColumn = updateColumnParameter.getStringValue();
    }

    // Get the length to use for the randomly-generated value.
    valueLength = 20;
    valueLengthParameter =
         parameters.getIntegerParameter(valueLengthParameter.getName());
    if (valueLengthParameter != null)
    {
      valueLength = valueLengthParameter.getIntValue();
    }

    // Get the name of the column to match.
    matchColumn = null;
    matchColumnParameter =
         parameters.getStringParameter(matchColumnParameter.getName());
    if (matchColumnParameter != null)
    {
      matchColumn = matchColumnParameter.getStringValue();
    }

    // Get the match value to use.
    matchValueParameter =
         parameters.getStringParameter(matchValueParameter.getName());
    if (matchValueParameter != null)
    {
      String valueStr = matchValueParameter.getStringValue();
      useRange      = false;
      useSequential = false;

      int openBracketPos = valueStr.indexOf('[');
      int dashPos = valueStr.indexOf('-', openBracketPos);
      if (dashPos < 0)
      {
        dashPos = valueStr.indexOf(':', openBracketPos);
        useSequential = true;
      }

      int closeBracketPos;
      if ((openBracketPos >= 0) && (dashPos > 0) &&
          ((closeBracketPos = valueStr.indexOf(']', dashPos)) > 0))
      {
        try
        {
          rangeMin = Integer.parseInt(valueStr.substring(openBracketPos+1,
                                                         dashPos));
          rangeMax = Integer.parseInt(valueStr.substring(dashPos+1,
                                                         closeBracketPos));
          rangeSpan = rangeMax - rangeMin + 1;

          valueInitial = valueStr.substring(0, openBracketPos);
          valueFinal   = valueStr.substring(closeBracketPos+1);
          useRange          = true;
          sequentialCounter = rangeMin;
        }
        catch (Exception e)
        {
          useRange = false;
        }
      }
      else
      {
        useRange = false;
      }
    }

    // Get the warm up time.
    warmUpTime = 0;
    warmUpParameter = parameters.getIntegerParameter(warmUpParameter.getName());
    if (warmUpParameter != null)
    {
      warmUpTime = warmUpParameter.getIntValue();
    }

    // Get the cool down time.
    coolDownTime = 0;
    coolDownParameter =
         parameters.getIntegerParameter(coolDownParameter.getName());
    if (coolDownParameter != null)
    {
      coolDownTime = coolDownParameter.getIntValue();
    }

    // Initialize the rate limiter.
    rateLimiter = null;
    maxRateParameter =
         parameters.getIntegerParameter(maxRateParameter.getName());
    if ((maxRateParameter != null) && maxRateParameter.hasValue())
    {
      int maxRate = maxRateParameter.getIntValue();
      if (maxRate > 0)
      {
        int rateIntervalSeconds = 0;
        rateLimitDurationParameter = parameters.getIntegerParameter(
             rateLimitDurationParameter.getName());
        if ((rateLimitDurationParameter != null) &&
            rateLimitDurationParameter.hasValue())
        {
          rateIntervalSeconds = rateLimitDurationParameter.getIntValue();
        }

        if (rateIntervalSeconds <= 0)
        {
          rateIntervalSeconds = getClientSideJob().getCollectionInterval();
        }

        rateLimiter = new FixedRateBarrier(rateIntervalSeconds * 1000L,
             maxRate * rateIntervalSeconds);
      }
    }

    // Get the time between updates.
    timeBetweenUpdates = 0;
    timeBetweenUpdatesParameter =
         parameters.getIntegerParameter(timeBetweenUpdatesParameter.getName());
    if (timeBetweenUpdatesParameter != null)
    {
      timeBetweenUpdates = timeBetweenUpdatesParameter.getIntValue();
    }

    // Get the number of iterations to perform.
    iterations = -1;
    iterationsParameter =
         parameters.getIntegerParameter(iterationsParameter.getName());
    if ((iterationsParameter != null) && iterationsParameter.hasValue())
    {
      iterations = iterationsParameter.getIntValue();
    }

    // Determine whether to disconnect after each query.
    alwaysDisconnect = false;
    disconnectParameter =
         parameters.getBooleanParameter(disconnectParameter.getName());
    if (disconnectParameter != null)
    {
      alwaysDisconnect = disconnectParameter.getBooleanValue();
    }


    // Initialize the parent random number generator.
    parentRandom = new Random();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
  {
    // Initialize the stat trackers.
    updatesCompleted = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_UPDATES_COMPLETED,
                                              collectionInterval);
    updateTimer = new TimeTracker(clientID, threadID,
                                  STAT_TRACKER_UPDATE_DURATION,
                                  collectionInterval);
    exceptionsCaught = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_EXCEPTIONS_CAUGHT,
                                              collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      updatesCompleted.enableRealTimeStats(statReporter, jobID);
      updateTimer.enableRealTimeStats(statReporter, jobID);
      exceptionsCaught.enableRealTimeStats(statReporter, jobID);
    }


    // Initialize the thread-specific random number generator.
    random = new Random(parentRandom.nextLong());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // First, load the driver class.  It will automatically be registered with
    // the JDBC driver manager.
    try
    {
      Constants.classForName(driverClass);
    }
    catch (Exception e)
    {
      logMessage("Unable to load the driver class \"" + driverClass + "\" -- " +
                 e);
      indicateStoppedDueToError();
      return;
    }


    // Determine the range of time for which we should collect statistics.
    long    currentTime         = System.currentTimeMillis();
    boolean collectingStats     = false;
    long    startCollectingTime = currentTime + (1000 * warmUpTime);
    long    stopCollectingTime  = Long.MAX_VALUE;
    if ((coolDownTime > 0) && (getShouldStopTime() > 0))
    {
      stopCollectingTime = getShouldStopTime() - (1000 * coolDownTime);
    }


    // Set up variables that will be used throughout the job.
    boolean           connected       = false;
    boolean           infinite        = (iterations <= 0);
    long              updateStartTime = 0;
    PreparedStatement statement       = null;
    connection = null;


    // Loop until it is determined we should stop.
    for (int i=0; ((! shouldStop()) && ((infinite || (i < iterations)))); i++)
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      currentTime = System.currentTimeMillis();
      if ((! collectingStats) && (currentTime >= startCollectingTime) &&
          (currentTime < stopCollectingTime))
      {
        // Tell the stat trackers that they should start tracking now.
        updatesCompleted.startTracker();
        updateTimer.startTracker();
        exceptionsCaught.startTracker();
        collectingStats = true;
      }
      else if (collectingStats && (currentTime >= stopCollectingTime))
      {
        // Tell the stat trackers that they should stop tracking now.
        updatesCompleted.stopTracker();
        updateTimer.stopTracker();
        exceptionsCaught.stopTracker();
        collectingStats = false;
      }


      // If the connection is not currently established, then connect it.
      if (! connected)
      {
        try
        {
          connection = DriverManager.getConnection(jdbcURL, userName,
                                                   userPassword);
          connected = true;
        }
        catch (SQLException se)
        {
          logMessage("Unable to connect to the database:  " + se);
          indicateStoppedDueToError();
          break;
        }

        String sql = "UPDATE " + tableName + " SET " + updateColumn +
                     " = ? WHERE " + matchColumn + " = ?;";
        try
        {
          statement = connection.prepareStatement(sql);
        }
        catch (SQLException se)
        {
          logMessage("Unable to parse SQL statement \"" + sql + "\".");
          indicateStoppedDueToError();
          break;
        }
      }


      // Execute the query and process through the results.
      if (timeBetweenUpdates > 0)
      {
        updateStartTime = System.currentTimeMillis();
      }
      if (collectingStats)
      {
        updateTimer.startTimer();
      }
      try
      {
        if (useRange)
        {
          statement.setString(1, getRandomValue(valueLength));
          statement.setString(2, getMatchValue());
        }
        statement.execute();
        if (collectingStats)
        {
          updateTimer.stopTimer();
          updatesCompleted.increment();
        }
      }
      catch (SQLException se)
      {
        writeVerbose("Caught SQL Exception:  " + se);
        if (collectingStats)
        {
          exceptionsCaught.increment();
        }
      }


      // If we should disconnect from the database, then do so.
      if (alwaysDisconnect)
      {
        try
        {
          statement.close();
        } catch (Exception e) {}

        try
        {
          connection.close();
        } catch (Exception e) {}
        connected = false;
      }


      // If we should sleep before the next query, then do so.
      if (timeBetweenUpdates > 0)
      {
        if (! shouldStop())
        {
          long now       = System.currentTimeMillis();
          long sleepTime = timeBetweenUpdates - (now - updateStartTime);
          if (sleepTime > 0)
          {
            try
            {
              Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {}
          }
        }
      }
    }


    // If the connection is still established, then close it.
    if (connected)
    {
      try
      {
        statement.close();
      } catch (Exception e) {}

      try
      {
        connection.close();
      } catch (Exception e) {}
    }

    if (collectingStats)
    {
      updatesCompleted.stopTracker();
      updateTimer.stopTracker();
      exceptionsCaught.stopTracker();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    if (connection != null)
    {
      try
      {
        connection.close();
      } catch (Exception e) {}

      connection = null;
    }
  }



  /**
   * Generates a string of randomly chosen alphabetic characters.
   *
   * @param  valueLength  The number of characters to include in the value.
   *
   * @return  The string of randomly chosen alphabetic characters.
   */
  private String getRandomValue(int valueLength)
  {
    char[] returnChars = new char[valueLength];

    for (int i=0; i < valueLength; i++)
    {
      returnChars[i] = ALPHABET[Math.abs((random.nextInt()) & 0x7FFFFFFF) %
                                ALPHABET.length];
    }

    return new String(returnChars);
  }



  /**
   * Retrieves a value to use to match the row(s) to update.
   *
   * @return  A value to use to match the row(s) to update.
   */
  private String getMatchValue()
  {
    if (useRange)
    {
      int value;
      if (useSequential)
      {
        value = sequentialCounter++;
        if (sequentialCounter > rangeMax)
        {
          sequentialCounter = rangeMin;
        }
      }
      else
      {
        value = ((random.nextInt() & 0x7FFFFFFF) % rangeSpan) + rangeMin;
      }

      return valueInitial + value + valueFinal;
    }
    else
    {
      return valueInitial;
    }
  }
}

