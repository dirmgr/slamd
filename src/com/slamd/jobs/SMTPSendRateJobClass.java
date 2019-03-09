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
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class defines a SLAMD job that interacts with an SMTP mail server by
 * sending messages of a fixed or randomly-chosen size to one or more recipients
 * at a rate that is as high as possible.
 *
 *
 * @author  Neil A. Wilson
 */
public class SMTPSendRateJobClass
       extends JobClass
{
  /**
   * The set of characters that will be used when forming random "words".
   */
  public static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();


  /**
   * The end-of-line character that is required for SMTP messages.
   */
  public static final String EOL = "\r\n";



  /**
   * The display name of the stat tracker used to count the number of SMTP
   * sessions established.
   */
  public static final String STAT_TRACKER_SMTP_SESSIONS = "SMTP Sessions";



  /**
   * The display name of the stat tracker used to count the total number of
   * SMTP sessions in which at least one message was sent successfully.
   */
  public static final String STAT_TRACKER_SUCCESS_COUNT = "Successful Sessions";



  /**
   * The display name of the stat tracker used to count the total number of
   * SMTP sessions in which no messages were sent successfully.
   */
  public static final String STAT_TRACKER_FAILURE_COUNT = "Failed Sessions";



  /**
   * The display name of the stat tracker used to keep track of the total number
   * of recipients specified for each message.
   */
  public static final String STAT_TRACKER_TOTAL_RECIPIENTS = "Total Recipients";



  /**
   * The display name of the stat tracker used to keep track of the number of
   * recipient addresses that were accepted by the mail server for each message.
   */
  public static final String STAT_TRACKER_ACCEPTED_RECIPIENTS =
       "Accepted Recipients";



  /**
   * The display name of the stat tracker used to keep track of the number of
   * recipient addresses that were rejected by the mail server for each message.
   */
  public static final String STAT_TRACKER_REJECTED_RECIPIENTS =
       "Rejected Recipients";



  /**
   * The display name of the stat tracker used to time the process of
   * authenticating and retrieving the list of messages.
   */
  public static final String STAT_TRACKER_SESSION_DURATION =
       "Session Duration (ms)";



  // The length of time between initial requests.
  private IntegerParameter delayParameter =
       new IntegerParameter("delay", "Time Between SMTP Sessions (ms)",
                            "The length of time in milliseconds between " +
                            "attempts to access the SMTP server.",
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

  // The maximum number of recipients to include in the message.
  private IntegerParameter maxRecipientsParameter =
    new IntegerParameter("max_recipients", "Maximum Number of Recipients",
                         "The maximum number of recipients that should be " +
                         "used for any single message.", true, 1, true, 1,
                         false, 0);

  // The minimum number of recipients to include in the message.
  private IntegerParameter minRecipientsParameter =
    new IntegerParameter("min_recipients", "Minimum Number of Recipients",
                         "The minimum number of recipients that should be " +
                         "used for any single message.", true, 1, true, 1,
                         false, 0);

  // The port number of the SMTP server.
  private IntegerParameter portParameter =
       new IntegerParameter("smtp_port", "SMTP Server Port",
                            "The port number on which the SMTP server is " +
                            "listening for requests.", true, 25, true, 1, true,
                            65535);

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

  // The size in bytes that should be used for the message body.
  private IntegerParameter sizeParameter =
       new IntegerParameter("size", "Message Body Size (bytes)",
                            "The size in bytes that should be used for the " +
                            "body of the SMTP message.  Note that this will " +
                            "be used as an approximation -- the actual " +
                            "message size may deviate by a few bytes.  It " +
                            "should also be noted that this does not " +
                            "include the SMTP message headers.", true, 1024,
                            true, 1, false, 0);

  // A placeholder parameter that is only used for formatting.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The address from which messages will originate.
  private StringParameter fromParameter =
       new StringParameter("from_address", "From Address",
                           "The e-mail address from which messages sent by " +
                           "this job will originate.", true, "");


  // The address of the SMTP server.
  private StringParameter hostParameter =
       new StringParameter("smtp_host", "SMTP Server Address",
                           "The fully-qualified domain name or IP address of " +
                           "the system running the SMTP server.", true, "");

  // The recipient(s) to use for the messages.
  private StringParameter recipientParameter =
       new StringParameter("recipient", "Recipient Address",
                           "The e-mail address of the recipient that should " +
                           "be used for each mail message.  A range of " +
                           "values may be specified by enclosing the range " +
                           "in brackets and separating the minimum and " +
                           "maximum values with a dash (e.g., [1-1000]), or " +
                           "a sequential range may be specified by " +
                           "separating the minimum and maximum values with a " +
                           "colon (e.g., [1:1000]).", true, "");



  // Static variables used to hold parameter values.
  private static int          delay;
  private static int          messageSize;
  private static int          maxRecipients;
  private static int          minRecipients;
  private static int          recipientSpan;
  private static int          smtpPort;
  private static String       fromAddress;
  private static String       messageBody;
  private static String       smtpAddress;
  private static String       subject;
  private static ValuePattern recipientPattern;


  // The random number generator for the job.
  private static Random parentRandom;
  private Random random;


  // The rate limiter for this job.
  private static FixedRateBarrier rateLimiter;


  // The local address associated with this client system.
  private String localAddress;



  // The stat trackers for the job.
  private IncrementalTracker  failureCounter;
  private IncrementalTracker  sessionCounter;
  private IncrementalTracker  successCounter;
  private IntegerValueTracker acceptedRecipientTracker;
  private IntegerValueTracker rejectedRecipientTracker;
  private IntegerValueTracker totalRecipientTracker;
  private TimeTracker         sessionTimer;




  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public SMTPSendRateJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "SMTP SendRate";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Repeatedly send e-mail messages using an SMTP mail server";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to repeatedly establish sessions with an SMTP " +
      "mail server and send messages to one or more recipients."
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
      fromParameter,
      recipientParameter,
      minRecipientsParameter,
      maxRecipientsParameter,
      sizeParameter,
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
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SMTP_SESSIONS,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SESSION_DURATION,
                      collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SUCCESS_COUNT,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_FAILURE_COUNT,
                             collectionInterval),
      new IntegerValueTracker(clientID, threadID, STAT_TRACKER_TOTAL_RECIPIENTS,
                              collectionInterval),
      new IntegerValueTracker(clientID, threadID,
                              STAT_TRACKER_ACCEPTED_RECIPIENTS,
                              collectionInterval),
      new IntegerValueTracker(clientID, threadID,
                              STAT_TRACKER_REJECTED_RECIPIENTS,
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
      successCounter,
      failureCounter,
      totalRecipientTracker,
      acceptedRecipientTracker,
      rejectedRecipientTracker
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
    // The recipient parameter must be parseable as a value pattern.
    StringParameter p =
         parameters.getStringParameter(recipientParameter.getName());
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

    IntegerParameter minParam =
         parameters.getIntegerParameter(minRecipientsParameter.getName());
    if (minParam == null)
    {
      throw new InvalidValueException("No value provided for required " +
                                      "parameter " +
                                      minRecipientsParameter.getDisplayName());
    }

    IntegerParameter maxParam =
         parameters.getIntegerParameter(maxRecipientsParameter.getName());
    if (maxParam == null)
    {
      throw new InvalidValueException("No value provided for required " +
                                      "parameter " +
                                      maxRecipientsParameter.getDisplayName());
    }

    int minRecip = minParam.getIntValue();
    int maxRecip = maxParam.getIntValue();
    if ((minRecip <= 0) || (maxRecip <= 0))
    {
      throw new InvalidValueException("Minimum and maximum number of " +
                                      "recipients must be greater than zero.");
    }
    else if (minRecip > maxRecip)
    {
      throw new InvalidValueException("Maximum number of recipients must be " +
                                      "greater than or equal to the minimum " +
                                      "number of recipients.");
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
      outputMessages.add("ERROR:  No SMTP server address was provided.");
      return false;
    }
    String host = hostParam.getStringValue();


    IntegerParameter portParam =
         parameters.getIntegerParameter(portParameter.getName());
    if ((portParam == null) || (! portParam.hasValue()))
    {
      outputMessages.add("ERROR:  No SMTP server port was provided.");
      return false;
    }
    int port = portParam.getIntValue();


    // Try to establish a connection to the SMTP server.
    Socket         socket;
    BufferedReader reader;
    BufferedWriter writer;
    try
    {
      outputMessages.add("Trying to establish a connection to SMTP server " +
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


    // If we've gotten here, then everything seems to be OK.  Close the
    // connection and return true.
    try
    {
      outputMessages.add("Sending the QUIT request to the server.");
      outputMessages.add("");

      writer.write("QUIT" + EOL);
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
    return true;
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


    // Get the address of the SMTP server.
    hostParameter = parameters.getStringParameter(hostParameter.getName());
    if (hostParameter != null)
    {
      smtpAddress = hostParameter.getStringValue();
    }

    // Get the port for the SMTP server.
    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      smtpPort = portParameter.getIntValue();
    }

    // Get the from address.
    fromParameter = parameters.getStringParameter(fromParameter.getName());
    if (fromParameter != null)
    {
      fromAddress = fromParameter.getStringValue();
    }


    // Get the recipient pattern.
    recipientParameter  =
         parameters.getStringParameter(recipientParameter.getName());
    if (recipientParameter != null)
    {
      try
      {
        recipientPattern =
             new ValuePattern(recipientParameter.getStringValue());
      }
      catch (Exception e)
      {
        throw new UnableToRunException(
             "Unable to parse the recipient pattern:  " + stackTraceToString(e),
             e);
      }
    }


    // Get the minimum number of recipients.
    minRecipientsParameter =
         parameters.getIntegerParameter(minRecipientsParameter.getName());
    if (minRecipientsParameter != null)
    {
      minRecipients = minRecipientsParameter.getIntValue();
    }


    // Get the maximum number of recipients.
    maxRecipientsParameter =
         parameters.getIntegerParameter(maxRecipientsParameter.getName());
    if (maxRecipientsParameter != null)
    {
      maxRecipients = maxRecipientsParameter.getIntValue();
    }
    recipientSpan = maxRecipients - minRecipients + 1;


    // Get the message size.
    sizeParameter = parameters.getIntegerParameter(sizeParameter.getName());
    if (sizeParameter != null)
    {
      messageSize = sizeParameter.getIntValue();
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


    // Create the message that will be used for all the SMTP sessions.
    generateSubject();
    generateMessage();
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
                                            STAT_TRACKER_SMTP_SESSIONS,
                                            collectionInterval);
    sessionTimer = new TimeTracker(clientID, threadID,
                                   STAT_TRACKER_SESSION_DURATION,
                                   collectionInterval);
    successCounter = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_SUCCESS_COUNT,
                                            collectionInterval);
    failureCounter = new IncrementalTracker(clientID, threadID,
                                            STAT_TRACKER_FAILURE_COUNT,
                                            collectionInterval);
    totalRecipientTracker = new IntegerValueTracker(clientID, threadID,
                                     STAT_TRACKER_TOTAL_RECIPIENTS,
                                     collectionInterval);
    acceptedRecipientTracker = new IntegerValueTracker(clientID, threadID,
                                        STAT_TRACKER_ACCEPTED_RECIPIENTS,
                                        collectionInterval);
    rejectedRecipientTracker = new IntegerValueTracker(clientID, threadID,
                                        STAT_TRACKER_REJECTED_RECIPIENTS,
                                        collectionInterval);


    // Enable real-time reporting of the data for these stat trackers.
    RealTimeStatReporter statReporter = getStatReporter();
    if (statReporter != null)
    {
      String jobID = getJobID();
      sessionCounter.enableRealTimeStats(statReporter, jobID);
      sessionTimer.enableRealTimeStats(statReporter, jobID);
      successCounter.enableRealTimeStats(statReporter, jobID);
      failureCounter.enableRealTimeStats(statReporter, jobID);
      totalRecipientTracker.enableRealTimeStats(statReporter, jobID);
      acceptedRecipientTracker.enableRealTimeStats(statReporter, jobID);
      rejectedRecipientTracker.enableRealTimeStats(statReporter, jobID);
    }


    // Get the local address associated with this client.
    try
    {
      localAddress = InetAddress.getLocalHost().getHostName();
    }
    catch (IOException ioe)
    {
      try
      {
        localAddress = InetAddress.getLocalHost().getHostAddress();
      }
      catch (IOException ioe2)
      {
        localAddress = clientID;
      }
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
    BufferedReader   reader;
    BufferedWriter   writer;
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy " +
                                                       "HH:mm:ss Z");
    int              numAccepted;
    int              numRejected;
    int              numRecipients;
    long             lastStartTime  = 0;
    Socket           socket;
    String           serverResponse = null;
    String[]         recipients;


    // Start the stat trackers.
    sessionCounter.startTracker();
    sessionTimer.startTracker();
    successCounter.startTracker();
    failureCounter.startTracker();
    totalRecipientTracker.startTracker();
    acceptedRecipientTracker.startTracker();
    rejectedRecipientTracker.startTracker();


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


      // Put together the list of recipients.
      numRecipients = ((random.nextInt() & 0x7FFFFFFF) % recipientSpan) +
                      minRecipients;
      recipients = new String[numRecipients];
      for (int i=0; i < numRecipients; i++)
      {
        recipients[i] = recipientPattern.nextValue();
      }


      // Start the attempt timer and indicate the beginning of a new attempt.
      sessionCounter.increment();
      sessionTimer.startTimer();


      // Open the connection to the SMTP server.
      try
      {
        socket = new Socket(smtpAddress, smtpPort);
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


      // The SMTP server should first introduce itself to the client.  Make sure
      // that the introduction is acceptable -- if so then it should start with
      // the number "220".
      try
      {
        serverResponse = reader.readLine();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();
        continue;
      }

      if (! serverResponse.startsWith("220"))
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe) {}

        continue;
      }


      // Send a "HELO" request to the server and read the response.  Make sure
      // that the response starts with a "250".
      try
      {
        sendLine(writer, "HELO " + localAddress);
        serverResponse = reader.readLine();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe2) {}

        continue;
      }

      if (! serverResponse.startsWith("250"))
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe) {}

        continue;
      }


      // Specify the from address.  The server must provide a response starting
      // with "250" for this to be acceptable.
      try
      {
        sendLine(writer, "MAIL FROM:<" + fromAddress + '>');
        serverResponse = reader.readLine();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe2) {}

        continue;
      }


      if (! serverResponse.startsWith("250"))
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe) {}

        continue;
      }

      // Specify the recipients.  The server should provide a response starting
      // with "250" or "251" for each of them.
      numAccepted = 0;
      numRejected = 0;
      for (int i=0; i < recipients.length; i++)
      {
        try
        {
          sendLine(writer, "RCPT TO:<" + recipients[i] + '>');
          serverResponse = reader.readLine();
        }
        catch (IOException ioe)
        {
          sessionTimer.stopTimer();
          failureCounter.increment();

          try
          {
            sendLine(writer, "QUIT");
            writer.close();
            reader.close();
            socket.close();
          } catch (IOException ioe2) {}

          continue mainLoop;
        }

        if (serverResponse.startsWith("25"))
        {
          numAccepted++;
        }
        else
        {
          numRejected++;
        }
      }

      totalRecipientTracker.addValue(numRecipients);
      acceptedRecipientTracker.addValue(numAccepted);
      rejectedRecipientTracker.addValue(numRejected);
      if (numAccepted == 0)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe) {}

        continue;
      }


      // Send the "DATA" header to the server.  The server must provide a
      // response starting with "354".
      try
      {
        sendLine(writer, "DATA");
        serverResponse = reader.readLine();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe2) {}

        continue;
      }

      if (! serverResponse.startsWith("354"))
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe) {}

        continue;
      }


      // Send the message header.  The server will not provide a response to
      // this.  Also, since we're sending multiple lines at once, there is no
      // reason to flush after each one so don't do that.
      try
      {
        writer.write("From: <" + fromAddress + '>' + EOL);
        writer.write("MIME-Version: 1.0" + EOL);
        writer.write("Content-type: text/plain; charset=us-ascii" + EOL);
        writer.write("Date: " + dateFormat.format(new Date()) + EOL);
        writer.write("Subject: " + subject + EOL);
        for (int i=0; i < recipients.length; i++)
        {
          writer.write("To: <" + recipients[i] + '>' + EOL);
        }
        writer.write(EOL);
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe2) {}

        continue;
      }

      // Send the message itself followed by a line containing only a period.
      // The server should provide a response starting with "250".
      try
      {
        writer.write(messageBody + EOL);
        sendLine(writer, ".");
        serverResponse = reader.readLine();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe2) {}

        continue;
      }

      if (! serverResponse.startsWith("250"))
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe) {}

        continue;
      }


      // The message is complete, so end the session with a "QUIT".
      try
      {
        sendLine(writer, "QUIT");
        writer.close();
        reader.close();
        socket.close();
      }
      catch (IOException ioe)
      {
        sessionTimer.stopTimer();
        failureCounter.increment();

        try
        {
          sendLine(writer, "QUIT");
          writer.close();
          reader.close();
          socket.close();
        } catch (IOException ioe2) {}

        continue;
      }


      // If we made it here, then everything was successful.
      sessionTimer.stopTimer();
      successCounter.increment();
    }

    sessionCounter.stopTracker();
    sessionTimer.stopTracker();
    successCounter.stopTracker();
    failureCounter.stopTracker();
    totalRecipientTracker.stopTracker();
    acceptedRecipientTracker.stopTracker();
    rejectedRecipientTracker.stopTracker();
  }



  /**
   * Writes the provided line of text to the SMTP server in the appropriate
   * format.
   *
   * @param  writer  The writer that can be used to communicate with the SMTP
   *                 server.
   * @param  line    The line of text to be written to the server.
   *
   * @throws  IOException  If a problem occurs while writing the data to the
   *                       SMTP server.
   */
  private static void sendLine(BufferedWriter writer, String line)
          throws IOException
  {
    writer.write(line);
    writer.write(EOL);
    writer.flush();
  }



  /**
   * Generates a subject for this mail message.  It will be between 3 and 7
   * "words" in length.
   */
  private static void generateSubject()
  {
    int          numWords      = (parentRandom.nextInt() & 0x7FFFFFFF) % 5 + 3;
    StringBuilder subjectBuffer = new StringBuilder();

    String separator = "";
    for (int i=0; i < numWords; i++)
    {
      int wordLength = (parentRandom.nextInt() & 0x7FFFFFFF) % 10 + 3;
      subjectBuffer.append(generateWord(wordLength));
      subjectBuffer.append(separator);
      separator = " ";
    }

    subject = subjectBuffer.toString();
  }



  /**
   * Creates the e-mail message that will be sent.  Although it will not
   * contain actual words, it will at least look realistic in terms of
   * spacing, word size, punctuation, etc.
   */
  private static void generateMessage()
  {
    int totalSize              = 0;
    int wordsThisSentence      = 0;
    int charsThisLine          = 0;
    StringBuilder messageBuffer = new StringBuilder();
    int sentenceSize           = (parentRandom.nextInt() & 0x7FFFFFFF) % 11 + 5;
    while (totalSize < messageSize)
    {
      int wordLength = (parentRandom.nextInt() & 0x7FFFFFFF) % 10 + 3;
      String word = generateWord(wordLength);
      messageBuffer.append(word);
      totalSize += wordLength;
      charsThisLine += wordLength;
      wordsThisSentence++;
      if ((wordsThisSentence > sentenceSize) || (totalSize > messageSize))
      {
        messageBuffer.append(".  ");
        totalSize += 3;
        charsThisLine += 3;
        wordsThisSentence = 0;
        sentenceSize = (parentRandom.nextInt() & 0x7FFFFFFF) % 11 + 5;
        if (charsThisLine > 70)
        {
          messageBuffer.append(EOL);
          totalSize += EOL.length();
          charsThisLine = 0;
        }
      }
      else if (charsThisLine > 70)
      {
        messageBuffer.append(EOL);
        totalSize += EOL.length();
        charsThisLine = 0;
      }
      else
      {
        messageBuffer.append(' ');
        totalSize++;
        charsThisLine++;
      }
    }

    messageBody = messageBuffer.toString();
  }



  /**
   * Generates a word of the specified length comprised of characters randomly
   * chosen from the provided character set.
   *
   * @param  numChars  The number of characters to include in the word.
   *
   * @return  A word of the specified length comprised of characters randomly
   *          chosen from the provided character set.
   */
  private static String generateWord(int numChars)
  {
    char[] chars = new char[numChars];
    for (int i=0; i < chars.length; i++)
    {
      chars[i] = ALPHABET[(parentRandom.nextInt() & 0x7FFFFFFF) %
                          ALPHABET.length];
    }

    return new String(chars);
  }
}

