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
package com.slamd.tools;



import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

import com.slamd.client.Client;
import com.slamd.client.ClientException;
import com.slamd.client.ClientMessageWriter;
import com.slamd.common.Constants;



/**
 * This class defines a command-line (non-GUI) application that may serve as a
 * client of the SLAMD server.  All of the configuration is done through
 * command-line options.  This is a good client to run as a background process.
 *
 *
 * @author   Neil A. Wilson
 */
public class CommandLineClient
       implements ClientMessageWriter
{
  /**
   * The name of the configuration property that specifies the address of the
   * SLAMD server.
   */
  public static final String PROPERTY_SLAMD_ADDRESS = "SLAMD_ADDRESS";



  /**
   * The name of the configuration property that specifies the client port for
   * the SLAMD server.
   */
  public static final String PROPERTY_SLAMD_PORT = "SLAMD_LISTEN_PORT";



  /**
   * The name of the configuration property that specifies the stat port for the
   * SLAMD server.
   */
  public static final String PROPERTY_SLAMD_STAT_PORT = "SLAMD_STAT_PORT";



  /**
   * The name of the configuration property that specifies the client address.
   */
  public static final String PROPERTY_CLIENT_ADDRESS = "CLIENT_ADDRESS";



  /**
   * The name of the configuration property that specifies the client ID.
   */
  public static final String PROPERTY_CLIENT_ID = "CLIENT_ID";



  /**
   * The name of the configuration property that specifies whether to enable
   * real-time statistics tracking.
   */
  public static final String PROPERTY_ENABLE_RT = "ENABLE_REAL_TIME_STATS";



  /**
   * The name of the configuration property that specifies the interval for
   * reporting real-time statistics.
   */
  public static final String PROPERTY_RT_INTERVAL = "REAL_TIME_REPORT_INTERVAL";



  /**
   * The name of the configuration property that specifies whether to enable
   * stat persistence.
   */
  public static final String PROPERTY_ENABLE_PERSISTENCE =
       "ENABLE_STAT_PERSISTENCE";



  /**
   * The name of the configuration property that specifies the directory for
   * stat persistence data.
   */
  public static final String PROPERTY_PERSISTENCE_DIR =
       "STAT_PERSISTENCE_DIRECTORY";



  /**
   * The name of the configuration property that specifies the stat persistence
   * interval.
   */
  public static final String PROPERTY_PERSISTENCE_INTERVAL =
       "STAT_PERSISTENCE_INTERVAL";



  /**
   * The name of the configuration property that specifies the authentication
   * ID.
   */
  public static final String PROPERTY_AUTH_ID = "AUTH_ID";



  /**
   * The name of the configuration property that specifies the authentication
   * password.
   */
  public static final String PROPERTY_AUTH_PW = "AUTH_PASS";



  /**
   * The name of the configuration property that specifies whether to use SSL.
   */
  public static final String PROPERTY_USE_SSL = "USE_SSL";



  /**
   * The name of the configuration property that specifies whether to blindly
   * trust any certificate.
   */
  public static final String PROPERTY_BLIND_TRUST = "BLIND_TRUST";



  /**
   * The name of the configuration property that specifies the path to the SSL
   * keystore.
   */
  public static final String PROPERTY_KEY_STORE = "SSL_KEY_STORE";



  /**
   * The name of the configuration property that specifies the password for the
   * SSL keystore.
   */
  public static final String PROPERTY_KEY_PASS = "SSL_KEY_PASS";



  /**
   * The name of the configuration property that specifies the path to the SSL
   * trust store.
   */
  public static final String PROPERTY_TRUST_STORE = "SSL_TRUST_PASS";



  /**
   * The name of the configuration property that specifies the password for the
   * SSL trust store.
   */
  public static final String PROPERTY_TRUST_PASS = "SSL_TRUST_PASS";



  /**
   * The name of the configuration property that specifies whether to aggregate
   * client thread data.
   */
  public static final String PROPERTY_AGGREGATE = "AGGREGATE_CLIENT_THREADS";



  /**
   * The name of the configuration property that specifies whether to operate in
   * restricted mode.
   */
  public static final String PROPERTY_RESTRICTED_MODE = "RESTRICTED_MODE";



  /**
   * The name of the configuration property that specifies whether to disable
   * the custom class loader.
   */
  public static final String PROPERTY_DISABLE_CL =
       "DISABLE_CUSTOM_CLASS_LOADER";



  /**
   * The name of the configuration property that specifies whether to enable
   * verbose mode.
   */
  public static final String PROPERTY_VERBOSE = "VERBOSE_MODE";



  /**
   * The name of the configuration property that specifies whether to enable
   * quiet mode.
   */
  public static final String PROPERTY_QUIET = "QUIET_MODE";



  /**
   * The name of the configuration property that specifies the path to the log
   * file.
   */
  public static final String PROPERTY_LOG_FILE = "LOG_FILE";



  // Indicates whether the data collected by the individual client threads
  // should be aggregated before being sent back to the SLAMD server.
  private boolean aggregateThreadData = false;

  // Indicates whether the client should blindly trust any SSL certificate
  // presented by the SLAMD server.
  private boolean blindTrust = false;

  // Indicates whether the client should enable real-time statistics collection.
  private boolean enableRealTimeStats = false;

  // Indicates whether the client should try to persist statistical data.
  private boolean persistStats;

  // Indicates whether to operate in quiet mode (no output while the client is
  // running).
  private boolean quietMode = false;

  // Indicates whether the client should operate in restricted mode.
  private boolean restrictedMode = false;

  // Indicates whether to print verbose messages.
  private boolean verboseMode = false;

  // Indicates whether to use the custom class loader when loading job classes.
  private boolean useCustomClassLoader = true;

  // Indicates whether the client should use SSL when communicating with the
  // SLAMD server.
  private boolean useSSL = false;

  // Indicates whether to enable automatic time synchronization with the SLAMD
  // server.
  private boolean useTimeSync = true;

  // The interval in seconds between saves of persistent statistical data.
  private int persistenceInterval = Constants.DEFAULT_STAT_PERSISTENCE_INTERVAL;

  // The port number to use when connecting to the SLAMD server.
  private int slamdServerPort = Constants.DEFAULT_LISTENER_PORT_NUMBER;

  // The port number to use for reporting stats to the SLAMD server.
  private int slamdStatPort = Constants.DEFAULT_STAT_LISTENER_PORT_NUMBER;

  // The interval in seconds to use when reporting stats to the server.
  private int statReportInterval = Constants.DEFAULT_STAT_REPORT_INTERVAL;

  // The client code that actually performs all the work of interacting with
  // the SLAMD server.
  private Client client;

  // The print writer that will be used to write log messages.
  private PrintWriter logWriter = null;

  // The ID that this client uses to authenticate to the SLAMD server.
  private String authenticationID = null;

  // The credentials for the provided authentication ID.
  private String authenticationCredentials = null;

  // The name of the directory to which class files may be written.
  private String classPath = null;

  // The source address to use for the client.
  private String clientAddress = null;

  // The client ID to use for the client.
  private String clientID;

  // The path to the log file to which output should be written.
  private String logFile = null;

  // The path to the directory into which the stat persistence data will be
  // written.
  private String persistenceDirectory = "statpersistence";

  // The hostname or IP address of the SLAMD server.
  private String slamdServerAddress = "127.0.0.1";

  // The location of the JSSE key store.
  private String sslKeyStore = null;

  // The password to use to access the JSSE key store.
  private String sslKeyStorePassword = null;

  // The location of the JSSE trust store.
  private String sslTrustStore = null;

  // The password to use to access the JSSE trust store.
  private String sslTrustStorePassword = null;



  /**
   * Passes off all the work to the constructor so that we can pass in a
   * reference to this class to the client.
   *
   * @param  args  The command-line arguments provided to this application.
   */
  public static void main(String[] args)
  {
    new CommandLineClient(args);
  }



  /**
   * Parses the command line parameters and connects to the SLAMD server to
   * accept and process job requests.
   *
   * @param  args  The command-line arguments provided to this application.
   */
  public CommandLineClient(String[] args)
  {
    // See if a configuration file was specified.  If so, then use it to
    // initialize the client settings.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-f"))
      {
        processConfigFile(args[++i]);
        break;
      }
    }


    // Process the command line arguments to override anything that might have
    // been specified in the configuration.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        slamdServerAddress = args[++i];
      }
      else if (args[i].equals("-C"))
      {
        clientAddress = args[++i];
      }
      else if (args[i].equals("-n"))
      {
        clientID = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        slamdServerPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-P"))
      {
        slamdStatPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-D"))
      {
        authenticationID = args[++i];
      }
      else if (args[i].equals("-w"))
      {
        authenticationCredentials = args[++i];
      }
      else if (args[i].equals("-c"))
      {
        classPath = args[++i];
      }
      else if (args[i].equals("-a"))
      {
        aggregateThreadData = true;
      }
      else if (args[i].equals("-R"))
      {
        restrictedMode = true;
      }
      else if (args[i].equals("-S"))
      {
        useSSL = true;
      }
      else if (args[i].equals("-s"))
      {
        enableRealTimeStats = true;
      }
      else if (args[i].equals("-I"))
      {
        statReportInterval = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-r"))
      {
        persistStats = true;
      }
      else if (args[i].equals("-i"))
      {
        persistenceInterval = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-d"))
      {
        persistenceDirectory = args[++i];
      }
      else if (args[i].equals("-B"))
      {
        blindTrust = true;
      }
      else if (args[i].equals("-k"))
      {
        sslKeyStore = args[++i];
      }
      else if (args[i].equals("-K"))
      {
        sslKeyStorePassword = args[++i];
      }
      else if (args[i].equals("-t"))
      {
        sslTrustStore = args[++i];
      }
      else if (args[i].equals("-T"))
      {
        sslTrustStorePassword = args[++i];
      }
      else if (args[i].equals("-l"))
      {
        logFile = args[++i];
      }
      else if (args[i].equals("-L"))
      {
        useCustomClassLoader = false;
      }
      else if (args[i].equals("-Y"))
      {
        useTimeSync = false;
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
        quietMode   = false;
      }
      else if (args[i].equals("-q"))
      {
        quietMode   = true;
        verboseMode = false;
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else if (args[i].equals("-f"))
      {
        // Already handled this.
        i++;
      }
      else
      {
        System.err.println("ERROR:  Unrecognized option \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    // If a log file has been requested, open the appropriate writer.
    if ((logFile == null) || (logFile.length() == 0))
    {
      logWriter = null;
    }
    else
    {
      try
      {
        logWriter = new PrintWriter(new FileWriter(logFile));
      }
      catch (IOException ioe)
      {
        System.err.println("ERROR:  Could not open output file \"" + logFile +
                           "\" -- " + ioe);
        System.exit(1);
      }
    }


    // Create the client and let it do all the work.
    try
    {
      int authType;
      if ((authenticationID == null) || (authenticationCredentials == null))
      {
        authType = Constants.AUTH_TYPE_NONE;
      }
      else
      {
        authType = Constants.AUTH_TYPE_SIMPLE;
      }

      client = new Client(clientID, clientAddress, slamdServerAddress,
                          slamdServerPort, slamdStatPort, useTimeSync,
                          enableRealTimeStats, statReportInterval, persistStats,
                          persistenceDirectory, persistenceInterval, authType,
                          authenticationID, authenticationCredentials,
                          restrictedMode, useCustomClassLoader, classPath,
                          useSSL, blindTrust, sslKeyStore, sslKeyStorePassword,
                          sslTrustStore, sslTrustStorePassword, this);

      client.aggregateThreadData(aggregateThreadData);
      client.start();
    }
    catch (ClientException sce)
    {
      sce.printStackTrace();
    }
  }



  /**
   * Processes the contents of the specified config file.
   *
   * @param  configFile  The path to the configuration file to process.
   */
  public void processConfigFile(String configFile)
  {
    Properties properties = new Properties();

    try
    {
      properties.load(new FileInputStream(configFile));
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Unable to load properties file \"" +
                         configFile + "\":  " + ioe);
      System.exit(1);
    }


    Iterator keys = properties.keySet().iterator();
    while (keys.hasNext())
    {
      String name  = (String) keys.next();
      String value = properties.getProperty(name, null);

      if ((value == null) || (value.length() == 0))
      {
        continue;
      }

      if (name.equals(PROPERTY_SLAMD_ADDRESS))
      {
        slamdServerAddress = value;
      }
      else if (name.equals(PROPERTY_SLAMD_PORT))
      {
        slamdServerPort = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_SLAMD_STAT_PORT))
      {
        slamdStatPort = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_CLIENT_ADDRESS))
      {
        clientAddress = value;
      }
      else if (name.equals(PROPERTY_CLIENT_ID))
      {
        clientID = value;
      }
      else if (name.equals(PROPERTY_ENABLE_RT))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          enableRealTimeStats = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          enableRealTimeStats = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_ENABLE_RT + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_RT_INTERVAL))
      {
        statReportInterval = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_ENABLE_PERSISTENCE))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          persistStats = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          persistStats = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_ENABLE_PERSISTENCE +
                             " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_PERSISTENCE_DIR))
      {
        persistenceDirectory = value;
      }
      else if (name.equals(PROPERTY_PERSISTENCE_INTERVAL))
      {
        persistenceInterval = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_AUTH_ID))
      {
        authenticationID = value;
      }
      else if (name.equals(PROPERTY_AUTH_PW))
      {
        authenticationCredentials = value;
      }
      else if (name.equals(PROPERTY_USE_SSL))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          useSSL = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          useSSL = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_USE_SSL + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_BLIND_TRUST))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          blindTrust = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          blindTrust = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_BLIND_TRUST + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_KEY_STORE))
      {
        sslKeyStore = value;
      }
      else if (name.equals(PROPERTY_KEY_PASS))
      {
        sslKeyStorePassword = value;
      }
      else if (name.equals(PROPERTY_TRUST_STORE))
      {
        sslTrustStore = value;
      }
      else if (name.equals(PROPERTY_TRUST_PASS))
      {
        sslTrustStorePassword = value;
      }
      else if (name.equals(PROPERTY_AGGREGATE))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          aggregateThreadData = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          aggregateThreadData = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_AGGREGATE + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_RESTRICTED_MODE))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          restrictedMode = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          restrictedMode = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_RESTRICTED_MODE +
                             " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_DISABLE_CL))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          useCustomClassLoader = false;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          useCustomClassLoader = true;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_DISABLE_CL + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_VERBOSE))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          verboseMode = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          verboseMode = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_VERBOSE + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_QUIET))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          quietMode = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          quietMode = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_QUIET + " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_LOG_FILE))
      {
        logFile = value;
      }
    }
  }



  /**
   * Displays usage information for this program.
   */
  public void displayUsage()
  {
    String eol = Constants.EOL;

    System.err.println(
"USAGE:  java " + getClass().getName() + " [options]" + eol +
"     where [options] include:" + eol +
"-f {file}    --  The path to the client configuration file." + eol +
"-h {host}    --  The address of the SLAMD server." + eol +
"-p {port}    --  The port number of the SLAMD server." + eol +
"-P {port}    --  The port number that the SLAMD server uses for " + eol +
"                 collecting real-time statistics. " + eol +
"-C {addr}    --  The local source address to use for the client." + eol +
"-n {id}      --  The client ID to use for the client." + eol +
"-D {authid}  --  The ID to use to authenticate to the SLAMD server." + eol +
"-w {authpw}  --  The password for the authentication ID." + eol +
"-c {dir}     --  The name of the directory in which Java class files" + eol +
"                 may be written." + eol +
"-a           --  Indicates that the data from each thread should be" + eol +
"                 aggregated before sending results to the server." + eol +
"-R           --  Indicates that the client should operate in " + eol +
"                 restricted mode." + eol +
"-S           --  Indicates that the client should communicate with the" + eol +
"                 SLAMD server over SSL." + eol +
"-s           --  Indicates that the client should enable real-time " + eol +
"                 statistics reporting to the SLAMD server." + eol +
"-I {value}   --  Specifies the interval (in seconds) to use when " + eol +
"                 reporting real-time stats to the SLAMD server." + eol +
"-r           --  Indicates that the client should retain persistent" + eol +
"                 data on disk for recovery in case of a failure" + eol +
"-i {value}   --  Specifies the interval in seconds that should be used" + eol +
"                 when periodically persisting statistics to disk" + eol +
"-d {dir}     --  Specifies the directory into which statistical data" + eol +
"                 should be written if persistence is enabled" + eol +
"-B           --  Indicates that the client blindly trust any SSL" + eol +
"                 certificate presented by the SLAMD server." + eol +
"-k {file}    --  The location of the JSSE key store." + eol +
"-K {pw}      --  The password needed to access the JSSE key store." + eol +
"-t {file}    --  The location of the JSSE trust store." + eol +
"-T {pw}      --  The password needed to access the JSSE trust store." + eol +
"-l {file}    --  The path to the output file to use rather than " + eol +
"                 standard output." + eol +
"-L           --  Disable the custom class loader." + eol +
"-Y           --  Disable time synchronization with the SLAMD server." + eol +
"-v           --  Operate in verbose mode." + eol +
"-q           --  Operate in quiet mode." + eol +
"-H           --  Show this usage information." + eol
                      );
  }



  /**
   * Writes the specified message to standard output.
   *
   * @param  message  The message to be written.
   */
  public void writeMessage(String message)
  {
    if (! quietMode)
    {
      if (logWriter == null)
      {
        System.out.println(message);
      }
      else
      {
        logWriter.println(message);
        logWriter.flush();
      }
    }
  }



  /**
   * Writes the specified message to standard output if verbose mode is enabled.
   *
   * @param  message  The message to be written.
   */
  public void writeVerbose(String message)
  {
    if (verboseMode)
    {
      if (logWriter == null)
      {
        System.out.println(message);
      }
      else
      {
        logWriter.println(message);
        logWriter.flush();
      }
    }
  }



  /**
   * Indicates whether the message writer is using verbose mode and therefore
   * will display messages written with the <CODE>writeVerbose</CODE> method.
   *
   * @return  <CODE>true</CODE> if the message writer is using verbose mode, or
   *          <CODE>false</CODE> if not.
   */
  public boolean usingVerboseMode()
  {
    return verboseMode;
  }
}

