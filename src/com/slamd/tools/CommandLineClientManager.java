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

import com.slamd.client.ClientMessageWriter;
import com.slamd.clientmanager.ClientManager;



/**
 * This class provides an implementation of a client manager that may be used to
 * automate the process of connecting and disconnecting clients to and from the
 * SLAMD server.
 *
 *
 * @author   Neil A. Wilson
 */
public class CommandLineClientManager
       implements ClientMessageWriter
{
  /**
   * The name of the configuration property that specifies the address of the
   * SLAMD server.
   */
  public static final String PROPERTY_SLAMD_ADDRESS = "SLAMD_ADDRESS";



  /**
   * The name of the configuration property that specifies the client manager
   * port for the SLAMD server.
   */
  public static final String PROPERTY_SLAMD_MANAGER_PORT = "SLAMD_MANAGER_PORT";



  /**
   * The name of the configuration property that specifies the client address.
   */
  public static final String PROPERTY_CLIENT_ADDRESS = "CLIENT_ADDRESS";



  /**
   * The name of the configuration property that specifies the client ID.
   */
  public static final String PROPERTY_CLIENT_ID = "CLIENT_ID";



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
   * The name of the configuration property that specifies how many clients to
   * automatically create when connecting to the SLAMD server.
   */
  public static final String PROPERTY_AUTO_CREATE_CLIENTS =
       "AUTO_CREATE_CLIENTS";



  /**
   * The name of the configuration property that specifies the maximum number of
   * concurrent client instances that should be allowed by the client manager.
   */
  public static final String PROPERTY_MAX_CLIENTS = "MAX_CLIENTS";



  /**
   * The default command that will be used to start the client manager if none
   * is provided.  Note that this does not apply to Windows systems, as they
   * require a different default command.
   */
  public static final String DEFAULT_START_COMMAND = "./start_client.sh";



  /**
   * The default command that will be used to start the client manager on
   * Windows systems if none is provided.
   */
  public static final String DEFAULT_START_COMMAND_WINDOWS = "start_client.bat";



  // Indicates whether the client manager should blindly trust any certificate
  // provided by the SLAMD server.
  private boolean blindTrust = false;

  // Indicates whether this client manager should communicate over SSL.
  private boolean useSSL = false;

  // Indicates whether this client manager is operating in verbose mode.
  private boolean verboseMode = false;

  // The number of client instances that should be automatically created
  // whenever the client manager establishes a connection to the SLAMD server.
  private int autoCreateClients = 0;

  // The maximum number of clients that may be started using this client
  // manager.
  private int maxClients = 0;

  // The port number to use to contact the SLAMD server.
  private int port = 3001;

  // The local address that the client should use.
  private String clientAddress = null;

  // The client ID for this client.
  private String clientID = null;

  // The address of the SLAMD server.
  private String host = null;

  // The location of the JSSE key store.
  private String sslKeyStore = null;

  // The password required to access the JSSE key store.
  private String sslKeyStorePassword = null;

  // The location of the JSSE trust store.
  private String sslTrustStore = null;

  // The password required to access the JSSE trust store.
  private String sslTrustStorePassword = null;

  // The command to use to start the SLAMD client application.
  private String startCommand = DEFAULT_START_COMMAND;



  /**
   * Creates a new instance of this client manager.
   *
   * @param  args  The command-line arguments provided to this client manager.
   */
  public static void main(String[] args)
  {
    new CommandLineClientManager(args);
  }



  /**
   * Creates a new instance of this client manager.
   *
   * @param  args  The command-line arguments provided to this client manager.
   */
  public CommandLineClientManager(String[] args)
  {
    if (System.getProperty("os.name").toLowerCase().contains("windows"))
    {
      startCommand = DEFAULT_START_COMMAND_WINDOWS;
    }


    // See if a configuration file was specified.  If so, then process it.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-f"))
      {
        processConfigFile(args[++i]);
        break;
      }
    }


    // Iterate through the command-line arguments and use them to set values
    // for the configuration parameters.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        host = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        port = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-C"))
      {
        clientAddress = args[++i];
      }
      else if (args[i].equals("-n"))
      {
        clientID = args[++i];
      }
      else if (args[i].equals("-c"))
      {
        startCommand = args[++i];
      }
      else if (args[i].equals("-A"))
      {
        autoCreateClients = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-m"))
      {
        maxClients = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-v"))
      {
        verboseMode = true;
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
      else if (args[i].equals("-H"))
      {
        displayUsage();
        return;
      }
      else if (args[i].equals("-f"))
      {
        // Already handled this.
        i++;
      }
      else
      {
        System.err.println("ERROR:  Unrecognized parameter \"" + args[i] + '"');
        displayUsage();
        return;
      }
    }


    // Make sure that at least the host was specified.
    if (host == null)
    {
      System.err.println("ERROR:  No host specified (use \"-h\")");
      displayUsage();
      return;
    }


    // Create the new client manager instance.
    ClientManager clientManager =
         new ClientManager(clientID, clientAddress, host, port, useSSL,
                           blindTrust, sslKeyStore, sslKeyStorePassword,
                           sslTrustStore, sslTrustStorePassword, maxClients,
                           startCommand, this);
    clientManager.setAutoCreateClients(autoCreateClients);
    clientManager.run();
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
        host = value;
      }
      else if (name.equals(PROPERTY_SLAMD_MANAGER_PORT))
      {
        port = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_CLIENT_ADDRESS))
      {
        clientAddress = value;
      }
      else if (name.equals(PROPERTY_CLIENT_ID))
      {
        clientID = value;
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
      else if (name.equals(PROPERTY_AUTO_CREATE_CLIENTS))
      {
        autoCreateClients = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_MAX_CLIENTS))
      {
        maxClients = Integer.parseInt(value);
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
   * Displays the usage information for this program on standard error.
   */
  public static void displayUsage()
  {
    System.err.println("USAGE:  java CommandLineClientManager [options]");
    System.err.println("        where [options] include:");
    System.err.println("-h {host} -- The address of the SLAMD server");
    System.err.println("-p {port} -- The port of the client manager listener");
    System.err.println("-C {addr} --  The local source address to use for " +
                       "the client");
    System.err.println("-n {id}   --  The client ID to use for the client");
    System.err.println("-m {num}  -- The maximum number of clients to allow");
    System.err.println("-c {cmd}  -- The command to start the SLAMD client");
    System.err.println("-S        -- Indicates that SSL should be enabled");
    System.err.println("-B        -- Indicates that the client should " +
                       "blindly trust any certificate");
    System.err.println("-t {file} -- The path to the certificate trust store");
    System.err.println("-T {pw}   -- The password for the certificate trust " +
                       "store");
    System.err.println("-k {file} -- The path to the certificate key store");
    System.err.println("-K {pw}   -- The password for the certificate key " +
                       "store");
    System.err.println("-v        -- Use verbose mode");
    System.err.println("-H        -- Display this usage information");
  }



  /**
   * Writes the provided message to this message writer.
   *
   * @param  message  The message to be written.
   */
  public void writeMessage(String message)
  {
    System.out.println(message);
  }



  /**
   * Writes the provided message if this message writer is operating in verbose
   * mode.
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
   * Indicates whether this message writer is operating in verbose mode.
   *
   * @return  <CODE>true</CODE> if this message writer is operating in verbose
   *          mode, or <CODE>false</CODE> if not.
   */
  public boolean usingVerboseMode()
  {
    return verboseMode;
  }
}

