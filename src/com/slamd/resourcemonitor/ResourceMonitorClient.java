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
package com.slamd.resourcemonitor;



import java.io.File;
import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import javax.net.ssl.SSLSocketFactory;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Writer;
import com.slamd.client.Client;
import com.slamd.client.ClientException;
import com.slamd.client.ClientMessageWriter;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.job.JobClass;
import com.slamd.message.ClientHelloMessage;
import com.slamd.message.HelloResponseMessage;
import com.slamd.message.JobCompletedMessage;
import com.slamd.message.JobControlRequestMessage;
import com.slamd.message.JobControlResponseMessage;
import com.slamd.message.JobRequestMessage;
import com.slamd.message.JobResponseMessage;
import com.slamd.message.KeepAliveMessage;
import com.slamd.message.Message;
import com.slamd.message.ServerShutdownMessage;
import com.slamd.message.StatusRequestMessage;
import com.slamd.message.StatusResponseMessage;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;



/**
 * This class defines a SLAMD client that can be used to monitor system
 * resources and report them to the SLAMD server.
 *
 *
 * @author   Neil A. Wilson
 */
public class ResourceMonitorClient
{
  /**
   * The name that will be assigned to the main resource monitor thread.
   */
  public static final String MAIN_THREAD_NAME = "Main Resource Monitor Thread";



  /**
   * The name of the configuration property that indicates whether a particular
   * resource monitor is enabled.
   */
  public static final String CONFIG_PROPERTY_ENABLED = "monitor_enabled";



  /**
   * The name of the configuration property that indicates the fully-qualified
   * class name of the resource monitor being defined.
   */
  public static final String CONFIG_PROPERTY_MONITOR_CLASS = "monitor_class";



  // The ASN.1 reader used to read messages from the SLAMD server.
  private ASN1Reader reader;

  // The ASN.1 writer used to send messages to the SLAMD server.
  private ASN1Writer writer;

  // Indicates whether the client should blindly trust the server's SSL cert.
  private boolean blindTrust;

  // Indicates whether the client should report statistical results to the SLAMD
  // server while a job is in progress.
  private boolean enableRealTimeStats;

  // Indicates whether a shutdown has been requested.
  private boolean shutdownRequested;

  // Indicates whether this client should support time synchronization with the
  // SLAMD server.
  private boolean supportsTimeSync;

  // Indicates whether to use SSL for communicating with the SLAMD server.
  private boolean useSSL;

  // The message writer associated with this resource monitor client.
  private ClientMessageWriter messageWriter;

  // The type of authentication that the client should perform when connecting
  // to the SLAMD server.
  private int authType;

  // An value that indicates the operating system on which this client is
  // running.
  private int clientOS;

  // The next message ID that should be included in a request to the SLAMD
  // server.
  private int messageID;

  // The port on which the SLAMD server is listening for monitor connections.
  private int slamdPort;

  // The port on which the SLAMD server is listening for connections from
  // real-time stat reporters.
  private int slamdStatPort;

  // The interval that should be used when reporting real-time stats to the
  // SLAMD server.
  private int statReportInterval;

  // A hash used to keep track of the monitor jobs currently running on this
  // client.
  private LinkedHashMap<String,ResourceMonitorJob> jobHash;

  // The difference in milliseconds between the clock on the client system and
  // the clock on the server system.
  private long serverTimeOffset;

  // A mutex used to provide threadsafe access to the job hash.
  private final Object jobHashMutex;

  // The stat reporter used to send results to the SLAMD server while a job is
  // in progress.
  private RealTimeStatReporter statReporter;

  // The resource monitors defined for use with this client.
  private ResourceMonitor[] resourceMonitors;

  // The socket used to connect to the SLAMD server.
  private Socket monitorSocket;

  // The ID that should be used to authenticate to the SLAMD server.
  private String authID;

  // The password that should be used to authenticate to the SLAMD server.
  private String authPW;

  // The hostname for this client.
  private String clientHostname;

  // The IP address for this client.
  private String clientIP;

  // The config directory for this client.
  private String configDirectory;

  // The address of the SLAMD server.
  private String slamdHost;

  // The location of the JSSE key store that will be used if the communication
  // between the client and the server is SSL-based.
  private String sslKeyStore;

  // The password needed to access the JSSE key store.
  private String sslKeyPass;

  // The location of the JSSE trust store that will be used if the communication
  // between the client and the server is SSL-based.
  private String sslTrustStore;

  // The password needed to access the JSSE trust store.
  private String sslTrustPass;



  /**
   * Creates a new instance of this resource monitor client.
   *
   * @param  clientAddress        The IP address on the client system from
   *                              which the connection should originate.  It
   *                              may be {@code null} if the client address
   *                              should be automatically selected.
   * @param  slamdHost            The address of the SLAMD server.
   * @param  slamdPort            The port number on which the SLAMD server is
   *                              listening for connections from monitor
   *                              clients.
   * @param  useSSL               Indicates whether to use SSL to communicate
   *                              with the SLAMD server.
   * @param  blindTrust           Indicates whether to blindly trust any SSL
   *                              certificate provided by the server.
   * @param  sslKeyStore          The location of the JSSE key store.
   * @param  sslKeyPass           The password needed to access the information
   *                              in the JSSE key store.
   * @param  sslTrustStore        The location of the JSSE trust store.
   * @param  sslTrustPass         The password needed to access the information
   *                              in the JSSE trust store.
   * @param  authType             The type of authentication to perform when
   *                              connecting to the SLAMD server.  It should be
   *                              either <CODE>Constants.AUTH_TYPE_NONE</CODE>
   *                              or <CODE>Constants.AUTH_TYPE_SIMPLE</CODE>.
   * @param  authID               The authentication ID to provide to the SLAMD
   *                              server if authentication is to be used.
   * @param  authPW               The authentication password to provide to the
   *                              SLAMD server if authentication is to be used.
   * @param  supportsTimeSync     Indicates whether the client should support
   *                              time synchronization with the server.
   * @param  messageWriter        The message writer to use for output generated
   *                              by the client.
   * @param  configDirectory      The path to the directory containing the
   *                              configuration files for the various resource
   *                              monitors defined for this client.
   * @param  enableRealTimeStats  Indicates whether this client should attempt
   *                              to report statistical information back to the
   *                              SLAMD server while a job is in progress.
   * @param  statReportInterval   The interval in seconds with which the client
   *                              should report statistical information to the
   *                              SLAMD server.
   * @param  slamdStatPort        The port number on which the SLAMD server is
   *                              listening for connections from stat clients.
   *
   * @throws  SLAMDException  If the operating system on which this client is
   *                          running is not supported.
   */
  public ResourceMonitorClient(String clientAddress,
                               String slamdHost, int slamdPort,
                               boolean useSSL, boolean blindTrust,
                               String sslKeyStore, String sslKeyPass,
                               String sslTrustStore, String sslTrustPass,
                               int authType, String authID, String authPW,
                               boolean supportsTimeSync,
                               ClientMessageWriter messageWriter,
                               String configDirectory,
                               boolean enableRealTimeStats,
                               int statReportInterval, int slamdStatPort)
         throws SLAMDException
  {
    // Initialize the instance variables corresponding to command-line
    // parameters.
    this.slamdHost           = slamdHost;
    this.slamdPort           = slamdPort;
    this.useSSL              = useSSL;
    this.blindTrust          = blindTrust;
    this.sslKeyStore         = sslKeyStore;
    this.sslKeyPass          = sslKeyPass;
    this.sslTrustStore       = sslTrustStore;
    this.sslTrustPass        = sslTrustPass;
    this.authType            = authType;
    this.authID              = authID;
    this.authPW              = authPW;
    this.supportsTimeSync    = supportsTimeSync;
    this.messageWriter       = messageWriter;
    this.configDirectory     = configDirectory;
    this.enableRealTimeStats = enableRealTimeStats;
    this.statReportInterval  = statReportInterval;
    this.slamdStatPort       = slamdStatPort;


    // Set the appropriate SSL-related environment variables
    if ((sslKeyStore != null) && (sslKeyStore.length() > 0))
    {
      System.setProperty(Constants.SSL_KEY_STORE_PROPERTY, sslKeyStore);
    }
    if ((sslKeyPass != null) && (sslKeyPass.length() > 0))
    {
      System.setProperty(Constants.SSL_KEY_PASSWORD_PROPERTY, sslKeyPass);
    }
    if ((sslTrustStore != null) && (sslTrustStore.length() > 0))
    {
      System.setProperty(Constants.SSL_TRUST_STORE_PROPERTY, sslTrustStore);
    }
    if ((sslTrustPass != null) && (sslTrustPass.length() > 0))
    {
      System.setProperty(Constants.SSL_TRUST_PASSWORD_PROPERTY, sslTrustPass);
    }


    // Create the job hash.
    jobHash      = new LinkedHashMap<String,ResourceMonitorJob>();
    jobHashMutex = new Object();


    // Initialize the message ID counter.
    messageID = 0;


    // Determine the client operating system.
    final String osName = System.getProperty("os.name");
    if ((osName == null) || (osName.length() == 0))
    {
      throw new SLAMDException("Unable to determine client operating system");
    }

    final String lowerOSName = osName.toLowerCase();
    if (lowerOSName.equals("solaris") || lowerOSName.equals("sunos"))
    {
      clientOS = Constants.OS_TYPE_SOLARIS;
    }
    else if (lowerOSName.equals("linux"))
    {
      clientOS = Constants.OS_TYPE_LINUX;
    }
    else if (lowerOSName.equals("hp-ux") || lowerOSName.equals("hpux"))
    {
      clientOS = Constants.OS_TYPE_HPUX;
    }
    else if (lowerOSName.equals("aix"))
    {
      clientOS = Constants.OS_TYPE_AIX;
    }
    else if (lowerOSName.equals("mac os x"))
    {
      clientOS = Constants.OS_TYPE_OSX;
    }
    else if (lowerOSName.contains("windows"))
    {
      clientOS = Constants.OS_TYPE_WINDOWS;
    }
    else
    {
      clientOS = Constants.OS_TYPE_UNKNOWN;
      messageWriter.writeMessage("WARNING:  Unrecognized client operating " +
           "system '" + osName + '\'');
    }


    // Determine the client hostname and IP address.
    InetAddress localAddress;
    try
    {
      if ((clientAddress == null) || (clientAddress.length() == 0))
      {
        localAddress = InetAddress.getLocalHost();
      }
      else
      {
        localAddress = InetAddress.getByName(clientAddress);
      }

      clientIP       = localAddress.getHostAddress();
      clientHostname = localAddress.getHostName();
    }
    catch (UnknownHostException uhe)
    {
      throw new SLAMDException("Unable to determine the client address.", uhe);
    }


    // Find all of the configuration files for the resource monitors and
    // instantiate them as necessary.
    resourceMonitors = new ResourceMonitor[0];
    File configDir = new File(configDirectory);
    if (! (configDir.exists() && configDir.isDirectory()))
    {
      throw new SLAMDException("Invalid configuration directory \"" +
                               configDirectory + '"');
    }
    File[] configFiles = configDir.listFiles();
    if ((configFiles == null) || (configFiles.length == 0))
    {
      throw new SLAMDException("No configuration files found in directory \"" +
                               configDirectory + '"');
    }
    for (int i=0; i < configFiles.length; i++)
    {
      parseConfigFile(configFiles[i]);
    }

    if (resourceMonitors.length == 0)
    {
      throw new SLAMDException("No resource monitors are enabled.");
    }


    Thread.currentThread().setName(MAIN_THREAD_NAME);
    messageWriter.writeVerbose("SLAMD resource monitor client starting up...");
    messageWriter.writeVerbose("");


    // Print out stats about the client system
    Runtime runtime = Runtime.getRuntime();
    messageWriter.writeVerbose("Java Version:                         " +
                               System.getProperty("java.version"));
    messageWriter.writeVerbose("Java Installation:                    " +
                               System.getProperty("java.home"));
    messageWriter.writeVerbose("Operating System Name:                " +
                               System.getProperty("os.name"));
    messageWriter.writeVerbose("Operating System Version:             " +
                               System.getProperty("os.version"));
    messageWriter.writeVerbose("Total CPUs Available to JVM:          " +
                               runtime.availableProcessors());
    messageWriter.writeVerbose("Total Memory Available to JVM:        " +
                               runtime.maxMemory());
    messageWriter.writeVerbose("Memory Currently Held by JVM:         " +
                               runtime.totalMemory());
    messageWriter.writeVerbose("Unused Memory Currently Held by JVM:  " +
                               runtime.freeMemory());
    messageWriter.writeVerbose("");


    // Establish the connection to the SLAMD server.
    try
    {
      if (useSSL)
      {
        if (blindTrust)
        {
          JSSEBlindTrustSocketFactory socketFactory =
               new JSSEBlindTrustSocketFactory();
          if ((clientAddress == null) || (clientAddress.length() == 0))
          {
            monitorSocket = socketFactory.createSocket(slamdHost, slamdPort);
          }
          else
          {
            monitorSocket = socketFactory.createSocket(slamdHost, slamdPort,
                                                       localAddress, 0);
          }
        }
        else
        {
          SSLSocketFactory socketFactory =
               (SSLSocketFactory) SSLSocketFactory.getDefault();
          if ((clientAddress == null) || (clientAddress.length() == 0))
          {
            monitorSocket = socketFactory.createSocket(slamdHost, slamdPort);
          }
          else
          {
            monitorSocket = socketFactory.createSocket(slamdHost, slamdPort,
                                                       localAddress, 0);
          }
        }
      }
      else
      {
        if ((clientAddress == null) || (clientAddress.length() == 0))
        {
          monitorSocket = new Socket(slamdHost, slamdPort);
        }
        else
        {
          monitorSocket = new Socket(slamdHost, slamdPort, localAddress, 0);
        }
      }

      reader = new ASN1Reader(monitorSocket);
      writer = new ASN1Writer(monitorSocket);
      messageWriter.writeMessage("Connected to the SLAMD server");
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to establish a connection to the " +
                               "SLAMD server:  " + e, e);
    }


    // Create and send the client hello message.
    ClientHelloMessage helloMessage =
         new ClientHelloMessage(getMessageID(), Client.SLAMD_CLIENT_VERSION,
                                clientHostname, authType, authID, authPW,
                                supportsTimeSync);
    try
    {
      writer.writeElement(helloMessage.encode());
      messageWriter.writeVerbose("Wrote a hello request");
      messageWriter.writeVerbose(helloMessage.toString());
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to send the hello message to the " +
                               "SLAMD server:  " + e, e);
    }


    // Read the hello response from the server.
    HelloResponseMessage helloResponse;
    try
    {
      ASN1Element element =
           reader.readElement(Constants.MAX_BLOCKING_READ_TIME);
      helloResponse = (HelloResponseMessage) Message.decode(element);
      messageWriter.writeVerbose("Read a hello response");
      messageWriter.writeVerbose(helloResponse.toString());
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to read the hello response from the " +
                               "SLAMD server:  " + e, e);
    }

    if (helloResponse.getResponseCode() != Constants.MESSAGE_RESPONSE_SUCCESS)
    {
      boolean stillAvailable = true;
      switch (helloResponse.getResponseCode())
      {
        case Constants.MESSAGE_RESPONSE_UNKNOWN_AUTH_ID:
        case Constants.MESSAGE_RESPONSE_INVALID_CREDENTIALS:
        case Constants.MESSAGE_RESPONSE_UNSUPPORTED_AUTH_TYPE:
        case Constants.MESSAGE_RESPONSE_UNSUPPORTED_CLIENT_VERSION:
        case Constants.MESSAGE_RESPONSE_UNSUPPORTED_SERVER_VERSION:
        case Constants.MESSAGE_RESPONSE_CLIENT_REJECTED:
          stillAvailable = false;
          break;
      }

      int rc = helloResponse.getResponseCode();
      String message = "Unable to connect to the SLAMD server.  The result " +
                       "code was " + rc + " (" +
                       Constants.responseCodeToString(rc) + ").";
      String respMsg = helloResponse.getResponseMessage();
      if ((respMsg != null) && (respMsg.length() > 0))
      {
        message += "The response message was \"" + respMsg + "\".";
      }
      throw new ClientException(message, stillAvailable);
    }

    // Get the server's current time and use it to calculate the time difference
    // between the client and the server.
    long clientTime = System.currentTimeMillis();
    long serverTime = helloResponse.getServerTime();
    if (serverTime > 0)
    {
      serverTimeOffset = (serverTime - clientTime);
    }
    else
    {
      serverTimeOffset = 0;
    }

    // If the time skew is significant, print a warning message.
    long absOffset = Math.abs(serverTimeOffset);
    if (absOffset > 2000)
    {
      messageWriter.writeMessage("WARNING:  Time skew of " + absOffset +
                                 " milliseconds detected between the client " +
                                 "and the server.  The client will attempt " +
                                 "to correct for this.");
    }


    // If real-time statistics reporting is enabled, then create the stat
    // reporter to do so.
    if (enableRealTimeStats)
    {
      try
      {
        statReporter = new RealTimeStatReporter(slamdHost, slamdStatPort,
                                                statReportInterval,
                                                clientHostname, authID, authPW,
                                                useSSL, blindTrust, sslKeyStore,
                                                sslKeyPass, sslTrustStore,
                                                sslTrustPass);
        statReporter.start();
      }
      catch (SLAMDException se)
      {
        throw new ClientException("ERROR creating stat reporter:  " +
             se.getMessage(), false, se);
      }
    }
  }



  /**
   * Parses the provided resource monitor configuration file and if appropriate
   * makes it available for use in this resource monitor client.
   *
   * @param  configFile  The configuration file to be parsed.
   */
  private void parseConfigFile(File configFile)
  {
    logVerbose("Parsing configuration file " + configFile.getAbsolutePath());

    try
    {
      // First, make sure this monitor is enabled.
      Properties properties = new Properties();
      properties.load(new FileInputStream(configFile));
      String enabledStr = properties.getProperty(CONFIG_PROPERTY_ENABLED);
      if (enabledStr == null)
      {
        logVerbose("Configuration file \"" + configFile.getPath() +
                   "\" ignored -- no value for property \"" +
                   CONFIG_PROPERTY_ENABLED + "\".");
        return;
      }
      if (! (enabledStr.equalsIgnoreCase("true") ||
             enabledStr.equalsIgnoreCase("yes") ||
             enabledStr.equalsIgnoreCase("on") ||
             enabledStr.equalsIgnoreCase("1")))
      {
        logVerbose("Configuration file \"" + configFile.getPath() +
                   "\" ignored -- property \"" +
                   CONFIG_PROPERTY_ENABLED + "\" is not \"true\".");
        return;
      }

      String className = properties.getProperty(CONFIG_PROPERTY_MONITOR_CLASS);
      if ((className == null) || (className.length() == 0))
      {
        logVerbose("Configuration file \"" + configFile.getPath() +
                   "\" ignored -- no value for property \"" +
                   CONFIG_PROPERTY_MONITOR_CLASS + "\".");
        return;
      }

      Class<?> monitorClass = Constants.classForName(className);
      ResourceMonitor monitor = (ResourceMonitor) monitorClass.newInstance();
      monitor.initialize(this, properties);

      if (! monitor.clientSupported())
      {
        // Don't create the monitor if it isn't supported on this platform.
        logVerbose("Skipping resource monitor " + monitor.getMonitorName() +
                   " as it is not supported on this client system.");
        return;
      }

      ResourceMonitor[] newMonitors =
           new ResourceMonitor[resourceMonitors.length+1];
      System.arraycopy(resourceMonitors, 0, newMonitors, 0,
                       resourceMonitors.length);
      newMonitors[resourceMonitors.length] = monitor;
      resourceMonitors = newMonitors;
      logVerbose("Enabling resource monitor client " +
                 monitor.getMonitorName());
    }
    catch (Exception e)
    {
      e.printStackTrace();
      logVerbose("Error parsing configuration file \"" + configFile.getPath() +
                 "\" -- " + e);
    }
  }



  /**
   * Writes the provided message to the SLAMD server.
   *
   * @param  message the message to write to the SLAMD server.
   */
  public void sendMessage(Message message)
  {
    try
    {
      writer.writeElement(message.encode());
    }
    catch (Exception e)
    {
      messageWriter.writeMessage("Unable to send message to SLAMD server:  " +
                                 e);
      messageWriter.writeVerbose(JobClass.stackTraceToString(e));
    }
  }



  /**
   * Waits for requests from the SLAMD server and handles them appropriately.
   */
  public void handleRequests()
  {
    boolean consecutiveFailures = false;

    while (! shutdownRequested)
    {
      Message message = null;
      try
      {
        message = Message.decode(
                       reader.readElement(Constants.MAX_BLOCKING_READ_TIME));
        consecutiveFailures = false;
      }
      catch (InterruptedIOException ioe)
      {
        // This is fine -- it just means there was no request from the server
        // for the last several seconds.
        consecutiveFailures = false;
        continue;
      }
      catch (Exception e)
      {
        if (consecutiveFailures)
        {
          messageWriter.writeMessage("Disconnecting from SLAMD server due to " +
                                     "consecutive decoding failures.");
          try
          {
            handleShutdownMessage();
          } catch (Exception e2) {}

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
            monitorSocket.close();
          } catch (Exception e2) {}

          return;
        }
        else
        {
          messageWriter.writeMessage("Unable to decode message received from " +
                                     "the SLAMD server.");
          messageWriter.writeVerbose("Exception was " +
                                     JobClass.stackTraceToString(e));
          consecutiveFailures = true;
          continue;
        }
      }

      if (message instanceof JobRequestMessage)
      {
        messageWriter.writeMessage("Received a job request message");
        messageWriter.writeVerbose(message.toString());
        handleJobRequest((JobRequestMessage) message);
      }
      else if (message instanceof JobControlRequestMessage)
      {
        messageWriter.writeMessage("Received a job control request message");
        messageWriter.writeVerbose(message.toString());
        handleJobControlRequest((JobControlRequestMessage) message);
      }
      else if (message instanceof StatusRequestMessage)
      {
        messageWriter.writeMessage("Received a status request message");
        messageWriter.writeVerbose(message.toString());
        handleStatusRequest((StatusRequestMessage) message);
      }
      else if (message instanceof ServerShutdownMessage)
      {
        messageWriter.writeMessage("Received a server shutdown message.");
        messageWriter.writeVerbose(message.toString());
        handleShutdownMessage();

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
          monitorSocket.close();
        } catch (Exception e) {}

        return;
      }
      else if (message instanceof KeepAliveMessage)
      {
        messageWriter.writeVerbose("Received a keepalive message.");
        messageWriter.writeVerbose(message.toString());
      }
      else
      {
        messageWriter.writeMessage("Unexpected message type received from " +
                                   "the SLAMD server (" +
                                   message.getMessageType() + ')');
        messageWriter.writeVerbose(message.toString());
      }
    }
  }



  /**
   * Handles all processing required for the provided job request message.
   *
   * @param  message  The job request message to be processed.
   */
  public void handleJobRequest(JobRequestMessage message)
  {
    // Get the necessary information from the request.
    int    messageID          = message.getMessageID();
    String jobID              = message.getJobID();
    long   startTime          = message.getStartTime().getTime();
    long   stopTime           = ((message.getStopTime() == null)
                                 ? -1
                                 : message.getStopTime().getTime());
    int    duration           = message.getDuration();
    int    collectionInterval = message.getCollectionInterval();


    // Deal with the time skew if appropriate.
    if (serverTimeOffset != 0)
    {
      startTime = startTime - serverTimeOffset;

      if (stopTime > 0)
      {
        stopTime = stopTime - serverTimeOffset;
      }
    }


    // Create a resource monitor job and add it into the job hash.
    ResourceMonitorJob monitorJob =
         new ResourceMonitorJob(this, jobID, startTime, stopTime, duration,
                                collectionInterval, enableRealTimeStats,
                                statReporter);
    synchronized (jobHashMutex)
    {
      jobHash.put(jobID, monitorJob);
    }


    // Construct and send the job response.
    JobResponseMessage response =
         new JobResponseMessage(messageID, jobID,
                                Constants.MESSAGE_RESPONSE_SUCCESS);
    sendMessage(response);
    messageWriter.writeMessage("Sent a job response message");
    messageWriter.writeVerbose(response.toString());
  }



  /**
   * Handles all processing required for the provided job control request
   * message.
   *
   * @param  message  The job control request message to be processed.
   */
  public void handleJobControlRequest(JobControlRequestMessage message)
  {
    // Get the necessary information from the request.
    int    messageID   = message.getMessageID();
    String jobID       = message.getJobID();
    int    controlType = message.getJobControlOperation();


    // Determine what to do based on the control type.
    int responseCode;
    String responseMsg = null;
    ResourceMonitorJob monitorJob;
    synchronized (jobHashMutex)
    {
      monitorJob = jobHash.get(jobID);
    }

    switch (controlType)
    {
      case Constants.JOB_CONTROL_TYPE_START:
        if (monitorJob == null)
        {
          responseCode = Constants.MESSAGE_RESPONSE_NO_SUCH_JOB;
          responseMsg  = "Unknown job " + jobID;
        }
        else
        {
          monitorJob.startCollecting();
          responseCode = Constants.MESSAGE_RESPONSE_SUCCESS;
          responseMsg  = "Started statistics collection";
        }
        break;
      case Constants.JOB_CONTROL_TYPE_STOP:
      case Constants.JOB_CONTROL_TYPE_STOP_AND_WAIT:
      case Constants.JOB_CONTROL_TYPE_STOP_DUE_TO_SHUTDOWN:
        if (monitorJob == null)
        {
          responseCode = Constants.MESSAGE_RESPONSE_NO_SUCH_JOB;
          responseMsg  = "Unknown job " + jobID;
        }
        else
        {
          monitorJob.stopCollecting();
          responseCode = Constants.MESSAGE_RESPONSE_SUCCESS;
          responseMsg  = "Requested that statistics collection end";
        }
        break;
      default:
        responseCode = Constants.MESSAGE_RESPONSE_UNSUPPORTED_CONTROL_TYPE;
        responseMsg  = "Unrecognized job control type " + controlType;
        break;
    }


    // Return the response to the server.
    JobControlResponseMessage response =
         new JobControlResponseMessage(messageID, jobID, responseCode,
                                       responseMsg);
    sendMessage(response);
    messageWriter.writeMessage("Sent a job control response message");
    messageWriter.writeVerbose(response.toString());
  }



  /**
   * Handles all processing required for the provided status request message.
   *
   * @param  message  The status request message to be processed.
   */
  public void handleStatusRequest(StatusRequestMessage message)
  {
    // This implementation will only return general health responses, and always
    // indicating that everything is hunky-dory.
    StatusResponseMessage response =
         new StatusResponseMessage(message.getMessageID(),
                                   Constants.MESSAGE_RESPONSE_SUCCESS,
                                   Constants.CLIENT_STATE_IDLE,
                                   "Available to process requests");
    sendMessage(response);
    messageWriter.writeMessage("Sent a status response message");
    messageWriter.writeVerbose(response.toString());
  }



  /**
   * Handles a shutdown message from the SLAMD server.
   */
  public void handleShutdownMessage()
  {
    shutdownRequested = true;
    synchronized (jobHashMutex)
    {
      Iterator i = jobHash.values().iterator();
      while (i.hasNext())
      {
        ResourceMonitorJob monitorJob = (ResourceMonitorJob) i.next();
        monitorJob.stopCollecting();
      }
    }

    if (statReporter != null)
    {
      statReporter.stopRunning();
    }
  }



  /**
   * Retrieves the message ID that the client should use for the next message
   * that will be sent to the SLAMD server.  The client will use even-numbered
   * message IDs for messages that it originates and the server will use
   * odd-numbered message IDs for messages that it originates.  Messages sent in
   * response to a previous request will use the same message ID as the request.
   *
   * @return  The message ID that the client should use for the next message
   *          that will be sent to the SLAMD server.
   */
  public synchronized int getMessageID()
  {
    int returnID = messageID;
    messageID += 2;
    return returnID;
  }



  /**
   * Retrieves a flag that indicates the client operating system.
   *
   * @return  A flag that indicates the client operating system.
   */
  public int getClientOS()
  {
    return clientOS;
  }



  /**
   * Retrieves the hostname of the client system.
   *
   * @return  The hostname of the client system.
   */
  public String getClientHostname()
  {
    return clientHostname;
  }



  /**
   * Retrieves the IP address of the client system.
   *
   * @return  The IP address of the client system.
   */
  public String getClientIP()
  {
    return clientIP;
  }



  /**
   * Retrieves the set of resource monitors that have been defined and enabled
   * in the configuration.
   *
   * @return  The set of resource monitors that have been defined and enabled in
   *          the configuration.
   */
  public ResourceMonitor[] getDefinedMonitors()
  {
    return resourceMonitors;
  }



  /**
   * Writes the provided message to the log if verbose mode is enabled.
   *
   * @param  message  The message to be written to the log in verbose mode.
   */
  public void logVerbose(String message)
  {
    messageWriter.writeVerbose(message);
  }



  /**
   * Indicates that the provided resource monitor job has completed.
   *
   * @param  monitorJob  The resource monitor job that has completed.
   */
  public void jobDone(ResourceMonitorJob monitorJob)
  {
    // Get the necessary information from the monitor job.
    String        jobID           = monitorJob.getJobID();
    int           jobState        = Constants.JOB_STATE_COMPLETED_SUCCESSFULLY;
    long          actualStartTime = monitorJob.getActualStartTime();
    long          actualStopTime  = monitorJob.getActualStopTime();
    StatTracker[] statTrackers    = monitorJob.getStatTrackers();
    String[]      logMessages     = monitorJob.getLogMessages();


    // Handle the time skew if appropriate.
    if (serverTimeOffset != 0)
    {
      actualStartTime += serverTimeOffset;
      actualStopTime  += serverTimeOffset;
    }


    // Construct and send a job completed message to the SLAMD server.
    int actualDuration = ((int) (actualStopTime - actualStartTime)) / 1000;
    JobCompletedMessage message =
         new JobCompletedMessage(getMessageID(), jobID, jobState,
                                 actualStartTime, actualStopTime,
                                 actualDuration, statTrackers, logMessages);
    sendMessage(message);
    messageWriter.writeMessage("Sent a job completed message");
    messageWriter.writeVerbose(message.toString());


    // Remove the job from the job hash.
    synchronized (jobHashMutex)
    {
      jobHash.remove(jobID);
    }
  }
}

