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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;

import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class defines a SLAMD job that interacts with a POP3 mail server.  It
 * does so by performing the following operations:
 *
 * <OL>
 *   <LI>Send the user name (<CODE>USER </CODE><I>{user}</I>).</LI>
 *   <LI>Send the password (<CODE>PASS </CODE><I>{password}</I>).</LI>
 *   <LI>Retrieve a list of all messages (<CODE>LIST</CODE>).</LI>
 *   <LI>Log out (<CODE>QUIT</CODE>).</LI>
 * </OL>
 *
 *
 * @author  Neil A. Wilson
 */
public class POPCheckRateJobClass
       extends JobClass
{
  /**
   * The display name of the stat tracker used to count the number of POP
   * sessions established.
   */
  public static final String STAT_TRACKER_POP_SESSIONS = "POP Sessions";



  /**
   * The display name of the stat tracker used to count the number of failed
   * POP logins.
   */
  public static final String STAT_TRACKER_FAILURE_COUNT = "Failed Logins";



  /**
   * The display name of the stat tracker used to keep track of the number of
   * messages contained in the inbox for each POP session.
   */
  public static final String STAT_TRACKER_MESSAGE_COUNT = "Message Count";



  /**
   * The display name of the stat tracker used to time the process of
   * authenticating and retrieving the list of messages.
   */
  public static final String STAT_TRACKER_SESSION_DURATION =
       "Session Duration (ms)";



  /**
   * The display name of the stat tracker used to count the number of successful
   * POP logins.
   */
  public static final String STAT_TRACKER_SUCCESS_COUNT = "Successful Logins";



  // The port number of the POP server.
  private IntegerParameter portParameter =
       new IntegerParameter("pop_port", "POP Server Port",
                            "The port number on which the POP server is " +
                            "listening for requests.", true, 110, true, 1, true,
                            65535);

  // The length of time between initial requests.
  private IntegerParameter delayParameter =
       new IntegerParameter("delay", "Time Between POP Sessions (ms)",
                            "The length of time in milliseconds between " +
                            "attempts to access the POP server.",
                            true, 0, true, 0, false, 0);

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

  // The password to use when authenticating.
  private PasswordParameter passwordParameter =
       new PasswordParameter("user_pw", "User Password",
                             "The password that will be used to authenticate " +
                             "to the POP server.", true, "");

  // A placeholder parameter that is only used for formatting.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The address of the POP server.
  private StringParameter hostParameter =
       new StringParameter("pop_host", "POP Server Address",
                           "The fully-qualified domain name or IP address of " +
                           "the system running the POP server.", true, "");

  // The user ID to use when authenticating.
  private StringParameter userIDParameter =
       new StringParameter("user_id", "User ID",
                           "The user ID that will be used to authenticate to " +
                           "the POP server.  It may include a range of " +
                           "numeric values chosen randomly by including " +
                           "that range in brackets with the values separated " +
                           "by a dash (i.e., \"user.[1-1000]\"), or a range " +
                           "of sequentially-incrementing numeric values by " +
                           "including that range in brackets with the values " +
                           "separated by a colon (i.e., \"user.[1:1000]\").",
                           true, "");



  // Static variables used to hold parameter values.
  private static int          popPort;
  private static int          delay;
  private static String       popAddress;
  private static String       password;
  private static ValuePattern userIDPattern;


  // The random number generator for the job.
  private static Random parentRandom;
  private Random random;


  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;



  // The stat trackers for the job.
  private IncrementalTracker  failureCounter;
  private IncrementalTracker  sessionCounter;
  private IncrementalTracker  successCounter;
  private IntegerValueTracker messageCountTracker;
  private TimeTracker         sessionTimer;




  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public POPCheckRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "POP CheckRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Repeatedly retrieve information about messages in an POP3 inbox";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to repeatedly establish sessions with an POP3 " +
      "mail server and retrieve information about messages in the user's inbox."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "Mail";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    Parameter[] parameters =
    {
      placeholder,
      hostParameter,
      portParameter,
      userIDParameter,
      passwordParameter,
      delayParameter,
      maxRateParameter,
      rateLimitDurationParameter
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
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_POP_SESSIONS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SESSION_DURATION,
                      collectionInterval),
      new IntegerValueTracker(clientID, threadID, STAT_TRACKER_MESSAGE_COUNT,
                              collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SUCCESS_COUNT,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_FAILURE_COUNT,
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
      sessionCounter,
      sessionTimer,
      messageCountTracker,
      successCounter,
      failureCounter
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void validateJobInfo(int numClients, int threadsPerClient,
                              int threadStartupDelay, Date startTime,
                              Date stopTime, int duration,
                              int collectionInterval, ParameterList parameters)
         throws InvalidValueException
  {
    // The user ID parameter must be parseable as a value pattern.
    StringParameter p =
         parameters.getStringParameter(userIDParameter.getName());
    if ((p != null) && p.hasValue())
    {
      try
      {
        new ValuePattern(p.getValue());
      }
      catch (ParseException pe)
      {
        throw new InvalidValueException("The value provided for the '" +
             p.getDisplayName() + "' parameter is not a valid value " +
             "pattern:  " + pe.getMessage(), pe);
      }
    }
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
    // Get the parameters necessary to perform the test.
    StringParameter hostParam =
         parameters.getStringParameter(hostParameter.getName());
    if ((hostParam == null) || (! hostParam.hasValue()))
    {
      outputMessages.add("ERROR:  No POP server address was provided.");
      return false;
    }
    String host = hostParam.getStringValue();


    IntegerParameter portParam =
         parameters.getIntegerParameter(portParameter.getName());
    if ((portParam == null) || (! portParam.hasValue()))
    {
      outputMessages.add("ERROR:  No POP server port was provided.");
      return false;
    }
    int port = portParam.getIntValue();


    StringParameter userIDParam =
         parameters.getStringParameter(userIDParameter.getName());
    if ((userIDParam == null) || (! userIDParam.hasValue()))
    {
      outputMessages.add("ERROR:  No user ID was provided.");
      return false;
    }
    String userID = userIDParam.getStringValue();


    PasswordParameter pwParam =
         parameters.getPasswordParameter(passwordParameter.getName());
    if ((pwParam == null) || (! pwParam.hasValue()))
    {
      outputMessages.add("ERROR:  No user password was provided.");
      return false;
    }
    String userPW = pwParam.getStringValue();


    // Try to establish a connection to the POP server.
    Socket         socket;
    BufferedReader reader;
    BufferedWriter writer;
    try
    {
      outputMessages.add("Trying to establish a connection to POP server " +
                         host + ':' + port + "....");

      socket = new Socket(host, port);
      reader = new BufferedReader(new InputStreamReader(
                                           socket.getInputStream()));
      writer = new BufferedWriter(new OutputStreamWriter(
                                           socket.getOutputStream()));

      outputMessages.add("Connected successfully.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to connect:  " +
                         stackTraceToString(e));
      return false;
    }


    // Read the initial response line from the server.
    try
    {
      outputMessages.add("Trying to read the hello string from the server....");

      String line = reader.readLine();

      outputMessages.add("Hello string was '" + line + "'.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to read the hello string:  " +
                         stackTraceToString(e));

      try
      {
        reader.close();
      } catch (Exception e2) {}

      try
      {
        writer.close();
      } catch (Exception e2) {}

      try
      {
        socket.close();
      } catch (Exception e2) {}

      return false;
    }


    // Send the user request.
    try
    {
      outputMessages.add("Trying to send the USER request to the server....");

      writer.write("USER " + userID);
      writer.newLine();
      writer.flush();

      outputMessages.add("Successfully sent the USER request.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to send the USER request:  " +
                         stackTraceToString(e));

      try
      {
        reader.close();
      } catch (Exception e2) {}

      try
      {
        writer.close();
      } catch (Exception e2) {}

      try
      {
        socket.close();
      } catch (Exception e2) {}

      return false;
    }


    // Read the user response.
    boolean loginSuccessful = false;
    try
    {
      outputMessages.add("Trying to read the USER response from the " +
                         "server....");

      String line = reader.readLine();
      if (line.startsWith("+"))
      {
        loginSuccessful = true;
      }

      outputMessages.add("Read a USER response of '" + line + "'.");
      outputMessages.add("");
    }
    catch (Exception e)
    {
      outputMessages.add("ERROR:  Unable to read the USER response:  " +
                         stackTraceToString(e));

      try
      {
        reader.close();
      } catch (Exception e2) {}

      try
      {
        writer.close();
      } catch (Exception e2) {}

      try
      {
        socket.close();
      } catch (Exception e2) {}

      return false;
    }


    // Send the pass request.
    if (loginSuccessful)
    {
      try
      {
        outputMessages.add("Trying to send the PASS request to the server....");

        writer.write("PASS " + userPW);
        writer.newLine();
        writer.flush();

        outputMessages.add("Successfully sent the PASS request.");
        outputMessages.add("");
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to send the PASS request:  " +
                           stackTraceToString(e));

        try
        {
          reader.close();
        } catch (Exception e2) {}

        try
        {
          writer.close();
        } catch (Exception e2) {}

        try
        {
          socket.close();
        } catch (Exception e2) {}

        return false;
      }


      // Read the pass response.
      loginSuccessful = false;
      try
      {
        outputMessages.add("Trying to read the PASS response from the " +
                           "server....");

        String line = reader.readLine();
        if (line.startsWith("+"))
        {
          loginSuccessful = true;
        }

        outputMessages.add("Read a PASS response of '" + line + "'.");
        outputMessages.add("");
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  Unable to read the PASS response:  " +
                           stackTraceToString(e));

        try
        {
          reader.close();
        } catch (Exception e2) {}

        try
        {
          writer.close();
        } catch (Exception e2) {}

        try
        {
          socket.close();
        } catch (Exception e2) {}

        return false;
      }
    }


    // If we've gotten here, then everything seems to be OK.  Close the
    // connection and return whether the login was successful.
    try
    {
      outputMessages.add("Sending the QUIT request to the server.");
      outputMessages.add("");

      writer.write("QUIT");
      writer.newLine();
      writer.flush();
    } catch (Exception e) {}

    try
    {
      reader.close();
    } catch (Exception e) {}

    try
    {
      writer.close();
    } catch (Exception e) {}

    try
    {
      socket.close();
    } catch (Exception e) {}


    outputMessages.add("All tests completed.");
    return loginSuccessful;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    // Seed the parent random number generator.
    parentRandom = new Random();


    // Get the address of the POP server.
    hostParameter = parameters.getStringParameter(hostParameter.getName());
    if (hostParameter != null)
    {
      popAddress = hostParameter.getStringValue();
    }

    // Get the port for the POP server.
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      popPort = portParameter.getIntValue();
    }

    // Get the user ID.  See if it should be a range of values.
    userIDParameter = parameters.getStringParameter(userIDParameter.getName());
    if (userIDParameter != null)
    {
      try
      {
        userIDPattern = new ValuePattern(userIDParameter.getStringValue());
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to parse user ID pattern:  " +
                                       stackTraceToString(e), e);
      }
    }

    // Get the password.
    passwordParameter =
         parameters.getPasswordParameter(passwordParameter.getName());
    if (passwordParameter != null)
    {
      password = passwordParameter.getStringValue();
    }


    // Get the delay between requests.
    delayParameter = parameters.getIntegerParameter(delayParameter.getName());
    if (delayParameter != null)
    {
      delay = delayParameter.getIntValue();
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
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // Create the stat trackers for this thread.
    sessionCounter = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_POP_SESSIONS,
                                            collectionInterval);
    sessionTimer = new TimeTracker(clientID, threadID,
                                   STAT_TRACKER_SESSION_DURATION,
                                   collectionInterval);
    messageCountTracker = new IntegerValueTracker(clientID, threadID,
                                                  STAT_TRACKER_MESSAGE_COUNT,
                                                  collectionInterval);
    successCounter = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SUCCESS_COUNT,
                                            collectionInterval);
    failureCounter = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_FAILURE_COUNT,
                                            collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      sessionCounter.enableRealTimeStats(statReporter, jobID);
      sessionTimer.enableRealTimeStats(statReporter, jobID);
      messageCountTracker.enableRealTimeStats(statReporter, jobID);
      successCounter.enableRealTimeStats(statReporter, jobID);
      failureCounter.enableRealTimeStats(statReporter, jobID);
    }


    // Seed the random number generator for this thread.
    random = new Random(parentRandom.nextLong());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Define variables that will be used throughout this method.
    boolean        keepReading;
    BufferedReader reader;
    BufferedWriter writer;
    int            highestUID;
    long           lastStartTime  = 0;
    Socket         socket;
    String         line;
    String         lastLine;
    String         request;
    String         userID;


    // Start the stat trackers.
    sessionCounter.startTracker();
    sessionTimer.startTracker();
    messageCountTracker.startTracker();
    successCounter.startTracker();
    failureCounter.startTracker();


    // Loop until it is determined that the job should stop.
mainLoop:
    while (! shouldStop())
    {
      if (rateLimiter != null)
      {
        if (rateLimiter.await())
        {
          continue;
        }
      }

      // If we need to sleep, then do so.
      if (delay > 0)
      {
        long now          = System.currentTimeMillis();
        long prevTestTime = now - lastStartTime;
        if (prevTestTime < delay)
        {
          try
          {
            Thread.sleep(delay - prevTestTime);
          } catch (Exception e) {}
        }
      }

      lastStartTime = System.currentTimeMillis();

      // Start the attempt timer and indicate the beginning of a new attempt.
      sessionCounter.increment();
      sessionTimer.startTimer();


      // Get the user ID to use in the next request.
      userID = userIDPattern.nextValue();


      // Open a connection to the POP server.
      try
      {
        socket = new Socket(popAddress, popPort);
        reader = new BufferedReader(new InputStreamReader(
                                             socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(
                                             socket.getOutputStream()));
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();
        continue;
      }


      // Read the initial hello line from the server.
      try
      {
        line = reader.readLine();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }

      // Send the user name.
      request = "USER " + userID;
      try
      {
        writer.write(request);
        writer.newLine();
        writer.flush();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }


      // Read the USER response.  It must be a single line.
      try
      {
        line = reader.readLine();
        if (line == null)
        {
          // The server must have closed the connection.
          sessionTimer.stopTimer();
          failureCounter.increment();

          try
          {
            reader.close();
            writer.close();
            socket.close();
          } catch (Exception e) {}

          continue mainLoop;
        }
        if (! line.startsWith("+"))
        {
          sessionTimer.stopTimer();
          failureCounter.increment();

          try
          {
            reader.close();
            writer.close();
            socket.close();
          } catch (Exception e) {}

          continue;
        }
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }


      // Send the password.
      request = "PASS " + password;
      try
      {
        writer.write(request);
        writer.newLine();
        writer.flush();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }


      // Read the PASS response.  It must be a single line.
      try
      {
        line = reader.readLine();
        if (line == null)
        {
          // The server must have closed the connection.
          sessionTimer.stopTimer();
          failureCounter.increment();

          try
          {
            reader.close();
            writer.close();
            socket.close();
          } catch (Exception e) {}

          continue mainLoop;
        }
        else if (! line.startsWith("+"))
        {
          sessionTimer.stopTimer();
          failureCounter.increment();

          try
          {
            reader.close();
            writer.close();
            socket.close();
          } catch (Exception e) {}

          continue;
        }
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }


      // Send the request to get the list of messages.
      request = "LIST";
      try
      {
        writer.write(request);
        writer.newLine();
        writer.flush();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }


      // Retrieve the list of messages.  It will be terminated with a "." on a
      // line by itself.  The line immediately before it should contain the UID
      // of the highest message number.
      highestUID  = 0;
      lastLine    = null;
      keepReading = true;
      while (keepReading)
      {
        try
        {
          line = reader.readLine();
          if (line == null)
          {
            // The server must have closed the connection.
            keepReading = false;
            sessionTimer.stopTimer();
            failureCounter.increment();

            try
            {
              reader.close();
              writer.close();
              socket.close();
            } catch (Exception e) {}

            continue mainLoop;
          }
          else if (line.startsWith("-"))
          {
            sessionTimer.stopTimer();
            failureCounter.increment();

            try
            {
              reader.close();
              writer.close();
              socket.close();
            } catch (Exception e) {}

            continue mainLoop;
          }
          else if (line.startsWith("."))
          {
            keepReading = false;
          }
          else
          {
            lastLine = line;
          }
        }
        catch (IOException ioe)
        {
          sessionTimer.stopTimer();
          failureCounter.increment();

          try
          {
            reader.close();
            writer.close();
            socket.close();
          } catch (Exception e) {}

          continue;
        }
      }


      // The line immediately before the period should have the highest message
      // number.  However, if it starts with a "+", then that means there are
      // no messages in the mailbox.
      if (lastLine != null)
      {
        if (lastLine.startsWith("+"))
        {
          messageCountTracker.addValue(0);
        }
        else
        {
          try
          {
            StringTokenizer tokenizer = new StringTokenizer(lastLine, " ");
            highestUID = Integer.parseInt(tokenizer.nextToken());
            messageCountTracker.addValue(highestUID);
          }
          catch (Exception e)
          {
            sessionTimer.stopTimer();
            failureCounter.increment();

            try
            {
              reader.close();
              writer.close();
              socket.close();
            } catch (Exception e2) {}

            continue;
          }
        }
      }


      // Log out from the server.
      request = "QUIT";
      try
      {
        writer.write(request);
        writer.newLine();
        writer.flush();

        reader.close();
        writer.close();
        socket.close();

        sessionTimer.stopTimer();
        successCounter.increment();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          reader.close();
          writer.close();
          socket.close();
        } catch (Exception e) {}

        continue;
      }
    }


    // Stop the stat trackers.
    sessionCounter.stopTracker();
    sessionTimer.stopTracker();
    messageCountTracker.stopTracker();
    successCounter.stopTracker();
    failureCounter.stopTracker();
  }
}

