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
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import com.slamd.client.ClientException;
import com.slamd.client.ClientMessageWriter;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.resourcemonitor.ResourceMonitorClient;



/**
 * This class defines a command-line application that may serve as a resource
 * monitor client for use with SLAMD.  All of the configuration is done through
 * command-line options.
 *
 *
 * @author   Neil A. Wilson
 */
public class CommandLineResourceMonitorClient
       implements ClientMessageWriter
{
  /**
   * The name of the configuration property that specifies the address of the
   * SLAMD server.
   */
  public static final String PROPERTY_SLAMD_ADDRESS = "SLAMD_ADDRESS";



  /**
   * The name of the configuration property that specifies the resource monitor
   * client port for the SLAMD server.
   */
  public static final String PROPERTY_SLAMD_MONITOR_PORT = "SLAMD_MONITOR_PORT";



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
   * The name of the configuration property that indicates whether to
   * automatically reconnect to the server if the connection is lost.
   */
  public static final String PROPERTY_AUTO_RECONNECT = "AUTO_RECONNECT";



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
   * The name of the configuration property that specifies whether to enable
   * verbose mode.
   */
  public static final String PROPERTY_VERBOSE = "VERBOSE_MODE";



  /**
   * The length of time that the monitor client should sleep between attempts to
   * reconnect to the SLAMD server if it is supposed to automatically reconnect.
   */
  public static final int SERVER_DOWN_RECONNECT_TIME = 30000;



  // Indicates whether the client should automatically attempt to reconnect to
  // the SLAMD server after it has disconnected.
  private boolean autoReconnect = false;

  // Indicates whether the client should enable reporting statistics to the
  // SLAMD server while a job is in progress.
  private boolean enableRealTimeStats = false;

  // Indicates whether the client should blindly trust the server's SSL cert.
  private boolean blindTrust = false;

  // Indicates whether the client should use SSL to communicate with the SLAMD
  // server.
  private boolean useSSL = false;

  // Indicates whether the client should use time synchronization with the SLAMD
  // server.
  private boolean useTimeSync = true;

  // Indicates whether this client is operating in verbose mode.
  private boolean verboseMode = false;

  // The type of authentication to use.
  private int authType = Constants.AUTH_TYPE_NONE;

  // The port number to use when communicating with the SLAMD server.
  private int slamdPort = Constants.DEFAULT_MONITOR_LISTENER_PORT_NUMBER;

  // The port number to use when communicating with the SLAMD server's stat
  // listener.
  private int slamdStatPort = Constants.DEFAULT_STAT_LISTENER_PORT_NUMBER;

  // The interval to use when reporting real-time statistics to the SLAMD
  // server.
  private int statReportInterval = Constants.DEFAULT_STAT_REPORT_INTERVAL;

  // The authentication ID to use when connecting to the SLAMD server.
  private String authID = null;

  // The password to use when connecting to the SLAMD server.
  private String authPW = null;

  // The local address to use for the client.
  private String clientAddress = null;

  // The path to the directory containing the resource monitor configuration
  // files.
  private String configDirectory = "config";

  // The address of the SLAMD server.
  private String slamdHost = "127.0.0.1";

  // The location of the JSSE key store that will be used if the communication
  // between the client and the server is SSL-based.
  private String sslKeyStore = null;

  // The password needed to access the JSSE key store.
  private String sslKeyStorePassword = null;

  // The location of the JSSE trust store that will be used if the communication
  // between the client and the server is SSL-based.
  private String sslTrustStore = null;

  // The password needed to access the JSSE trust store.
  private String sslTrustStorePassword = null;



  /**
   * Passes off all the work to the constructor so that we can pass in a
   * reference to this class to the client.
   *
   * @param  args  The command-line arguments provided to this application.
   */
  public static void main(String[] args)
  {
    new CommandLineResourceMonitorClient(args);
  }



  /**
   * Parses the command line parameters and connects to the SLAMD server to
   * accept and process resource monitor requests.
   *
   * @param  args  The command-line arguments provided to this application.
   */
  public CommandLineResourceMonitorClient(String[] args)
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


    // Parse the command-line parameters
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        slamdHost = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        slamdPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-P"))
      {
        slamdStatPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-C"))
      {
        clientAddress = args[++i];
      }
      else if (args[i].equals("-D"))
      {
        authID = args[++i];
      }
      else if (args[i].equals("-w"))
      {
        authPW = args[++i];
      }
      else if (args[i].equals("-c"))
      {
        configDirectory = args[++i];
      }
      else if (args[i].equals("-S"))
      {
        useSSL = true;
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
      else if (args[i].equals("-r"))
      {
        autoReconnect = true;
      }
      else if (args[i].equals("-s"))
      {
        enableRealTimeStats = true;
      }
      else if (args[i].equals("-I"))
      {
        statReportInterval = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-Y"))
      {
        useTimeSync = false;
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
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
        System.err.println("ERROR:  Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that the configuration directory was specified.
    if (configDirectory == null)
    {
      System.err.println("ERROR:  No configuration directory provided (use " +
                         "-c)");
      displayUsage();
      System.exit(1);
    }


    // Determine if we should use authentication.
    if ((authID != null) && (authPW != null))
    {
      authType = Constants.AUTH_TYPE_SIMPLE;
    }
    else if (authID != null)
    {
      System.err.println("WARNING:  Auth ID provided but no password");
      System.err.println("          No authentication will be performed.");
    }
    else if (authPW != null)
    {
      System.err.println("WARNING:  Auth password provided but no auth ID");
      System.err.println("          No authentication will be performed.");
    }


    // Instantiate the resource monitor client and use it to process requests.
    ResourceMonitorClient monitorClient = null;
    while (true)
    {
      try
      {
        monitorClient = new ResourceMonitorClient(clientAddress, slamdHost,
             slamdPort, useSSL, blindTrust, sslKeyStore, sslKeyStorePassword,
             sslTrustStore, sslTrustStorePassword, authType, authID, authPW,
             useTimeSync, this, configDirectory, enableRealTimeStats,
             statReportInterval, slamdStatPort);
        monitorClient.handleRequests();
      }
      catch (ClientException ce)
      {
        System.err.println(ce.getMessage());
        if (! ce.stillAvailable())
        {
          System.err.println("This resource monitor client will shut down.");
          break;
        }
      }
      catch (SLAMDException se)
      {
        System.err.println(se.getMessage());
      }

      System.err.println("Disconnected from the SLAMD server.");

      if (autoReconnect)
      {
        System.err.println("Sleeping before attempt to reconnect...");

        try
        {
          Thread.sleep(SERVER_DOWN_RECONNECT_TIME);
        } catch (InterruptedException ie) {}
        continue;
      }
      else
      {
        break;
      }
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

      if (value == null)
      {
        continue;
      }

      if (name.equals(PROPERTY_SLAMD_ADDRESS))
      {
        slamdHost = value;
      }
      else if (name.equals(PROPERTY_SLAMD_MONITOR_PORT))
      {
        slamdPort = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_SLAMD_STAT_PORT))
      {
        slamdStatPort = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_CLIENT_ADDRESS))
      {
        clientAddress = value;
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
      else if (name.equals(PROPERTY_AUTO_RECONNECT))
      {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("1"))
        {
          autoReconnect = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          autoReconnect = false;
        }
        else
        {
          System.err.println("ERROR:  Cannot interpret the value of the " +
                             PROPERTY_AUTO_RECONNECT +
                             " property as a Boolean.");
          System.exit(1);
        }
      }
      else if (name.equals(PROPERTY_AUTH_ID))
      {
        authID = value;
      }
      else if (name.equals(PROPERTY_AUTH_PW))
      {
        authPW = value;
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
"-f {file}    --  The path to the monitor client configuration file" + eol +
"-h {host}    --  The address of the SLAMD server" + eol +
"-p {port}    --  The port number of the SLAMD server" + eol +
"-P {port}    --  The port number that the SLAMD server uses for " + eol +
"                 collecting real-time statistics. " + eol +
"-C {addr}    --  The local source address to use for the client." + eol +
"-D {authid}  --  The ID to use to authenticate to the SLAMD server" + eol +
"-w {authpw}  --  The password for the authentication ID" + eol +
"-c {dir}     --  The name of the directory containing the monitor " + eol +
"                 configuration files." + eol +
"-S           --  Indicates that the client should communicate with the" + eol +
"                 SLAMD server over SSL." + eol +
"-B           --  Indicates that the client blindly trust any SSL" + eol +
"                 certificate presented by the SLAMD server." + eol +
"-k {file}    --  The location of the JSSE key store." + eol +
"-K {pw}      --  The password needed to access the JSSE key store." + eol +
"-t {file}    --  The location of the JSSE trust store." + eol +
"-T {pw}      --  The password needed to access the JSSE trust store." + eol +
"-r           --  Automatically attempt to reconnect to the SLAMD " + eol +
"                 server if the connection is closed due to shutdown" + eol +
"-s           --  Indicates that the client should enable real-time " + eol +
"                 statistics reporting to the SLAMD server." + eol +
"-I {value}   --  Specifies the interval (in seconds) to use when " + eol +
"                 reporting real-time stats to the SLAMD server." + eol +
"-L           --  Disable time synchronization with the SLAMD server" + eol +
"-v           --  Operate in verbose mode" + eol +
"-H           --  Show this usage information" + eol
                      );
  }



  /**
   * Writes the specified message to standard output.
   *
   * @param  message  The message to be written.
   */
  public void writeMessage(String message)
  {
    System.out.println(message);
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
      System.out.println(message);
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

