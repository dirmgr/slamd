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



import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.slamd.client.Client;
import com.slamd.client.ClientException;
import com.slamd.client.ClientMessageWriter;
import com.slamd.client.ClientShutdownListener;
import com.slamd.common.Constants;
import com.slamd.common.DynamicConstants;



/**
 * This class provides a GUI front-end to the SLAMD client interface using the
 * Java Swing interface.
 *
 *
 * @author   Neil A. Wilson
 */
public class SwingClient
       implements ActionListener, ChangeListener, ClientMessageWriter,
                  ClientShutdownListener
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
  public static final String PROPERTY_SLAMD_PORT = "SLAMD_PORT";



  /**
   * The name of the configuration property that specifies the stat port for the
   * SLAMD server.
   */
  public static final String PROPERTY_SLAMD_STAT_PORT = "SLAMD_STAT_PORT";



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
   * The end-of-line marker that should be used on this platform.
   */
  public static String EOL = System.getProperty("line.separator");



  // The reference to the SLAMD client actually doing the work.
  private Client client;

  // Variables pertaining to the client configuration.
  private boolean aggregateThreadData   = false;
  private boolean enableRealTimeStats   = false;
  private boolean persistStats          = false;
  private boolean restrictedMode        = false;
  private boolean sslBlindTrust         = true;
  private boolean useCustomClassLoader  = true;
  private boolean useSSL                = false;
  private boolean useTimeSync           = true;
  private boolean verboseMode           = false;
  private int     persistenceInterval   = 300;
  private int     serverClientPort      = 3000;
  private int     serverStatPort        = 3003;
  private int     statReportInterval    = 5;
  private String  authID                = null;
  private String  authPW                = null;
  private String  classDirectory        = "classes";
  private String  persistenceDirectory  = "statpersistence";
  private String  serverAddress         = "127.0.0.1";
  private String  sslKeyStore           = null;
  private String  sslKeyStorePassword   = null;
  private String  sslTrustStore         = null;
  private String  sslTrustStorePassword = null;

  // References to GUI components pertaining to the client configuration.
  private JButton        copyAllButton;
  private JButton        copySelectedButton;
  private JButton        connectButton;
  private JButton        dialogCancelButton;
  private JButton        dialogConnectButton;
  private JCheckBox      aggregateCheckbox;
  private JCheckBox      blindTrustCheckbox;
  private JCheckBox      enableRealTimeCheckbox;
  private JCheckBox      persistStatsCheckbox;
  private JCheckBox      restrictedModeCheckbox;
  private JCheckBox      useSSLCheckbox;
  private JCheckBox      verboseCheckbox;
  private JDialog        connectDialog;
  private JFrame         appWindow;
  private JLabel         persistDirLabel;
  private JLabel         persistIntervalLabel;
  private JLabel         statIntervalLabel;
  private JLabel         statPortLabel;
  private JLabel         sslKeyStoreLabel;
  private JLabel         sslKeyStorePasswordLabel;
  private JLabel         sslTrustStoreLabel;
  private JLabel         sslTrustStorePasswordLabel;
  private JPasswordField authPWField;
  private JPasswordField sslKeyStorePasswordField;
  private JPasswordField sslTrustStorePasswordField;
  private JTextArea      messageArea;
  private JTextField     authIDField;
  private JTextField     hostField;
  private JTextField     persistDirField;
  private JTextField     persistIntervalField;
  private JTextField     portField;
  private JTextField     statPortField;
  private JTextField     statIntervalField;
  private JTextField     sslKeyStoreField;
  private JTextField     sslTrustStoreField;



  /**
   * Launches the swing client with the provided set of command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    new SwingClient(args);
  }



  /**
   * Creates a new instance of this swing client using the provided set of
   * command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public SwingClient(String[] args)
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


    // Parse the command-line arguments and assign their values to the
    // appropriate variables.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        serverAddress = args[++i];
      }
      else if (args[i].equals("-p"))
      {
        serverClientPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-P"))
      {
        serverStatPort = Integer.parseInt(args[++i]);
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
        classDirectory = args[++i];
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
        sslBlindTrust = true;
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
        System.err.println("ERROR:  Unrecognized argument \"" + args[i] +
                           '"');
        return;
      }
    }


    // Create the GUI layout for this client.
    appWindow = new JFrame("SLAMD Client - " + DynamicConstants.SLAMD_VERSION);
    appWindow.getContentPane().setLayout(new BorderLayout(5, 5));

    JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    copySelectedButton = new JButton("Copy Selection to Clipboard");
    copySelectedButton.addActionListener(this);
    northPanel.add(copySelectedButton);
    copyAllButton = new JButton("Copy All to Clipboard");
    copyAllButton.addActionListener(this);
    northPanel.add(copyAllButton);
    connectButton = new JButton("Disconnect");
    connectButton.addActionListener(this);
    northPanel.add(connectButton);

    messageArea = new JTextArea(30, 80);
    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setEditable(false);
    messageArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
    JScrollPane centerPane = new JScrollPane(messageArea);

    appWindow.getContentPane().add(northPanel, BorderLayout.NORTH);
    appWindow.getContentPane().add(centerPane, BorderLayout.CENTER);
    appWindow.pack();
    appWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    appWindow.setVisible(true);
    connectButton.setText("Connect");
    connectButton.requestFocus();
  }



  /**
   * Detects an action event (i.e., button click) and performs the appropriate
   * task.
   *
   * @param  event  The action event that occurred.
   */
  public void actionPerformed(ActionEvent event)
  {
    Object source = event.getSource();

    if (source == connectButton)
    {
      if (client == null)
      {
        if (connectDialog == null)
        {
          createConnectDialog();
        }
        else
        {
          connectDialog.setVisible(true);
        }
      }
      else
      {
        client.disconnect();
        client = null;
        connectButton.setText("Connect");
      }
    }
    else if (source == dialogConnectButton)
    {
      try
      {
        serverAddress         = hostField.getText();
        serverClientPort      = Integer.parseInt(portField.getText());
        useSSL                = useSSLCheckbox.isSelected();
        sslBlindTrust         = blindTrustCheckbox.isSelected();
        sslKeyStore           = sslKeyStoreField.getText();
        sslKeyStorePassword   =
             new String(sslKeyStorePasswordField.getPassword());
        sslTrustStore         = sslTrustStoreField.getText();
        sslTrustStorePassword =
             new String(sslTrustStorePasswordField.getPassword());
        authID                = authIDField.getText();
        authPW                = new String(authPWField.getPassword());
        enableRealTimeStats   = enableRealTimeCheckbox.isSelected();
        serverStatPort        = Integer.parseInt(statPortField.getText());
        statReportInterval    = Integer.parseInt(statIntervalField.getText());
        persistStats          = persistStatsCheckbox.isSelected();
        persistenceDirectory  = persistDirField.getText();
        persistenceInterval   =
             Integer.parseInt(persistIntervalField.getText());
        aggregateThreadData   = aggregateCheckbox.isSelected();
        restrictedMode        = restrictedModeCheckbox.isSelected();
        verboseMode           = verboseCheckbox.isSelected();

        int authType = Constants.AUTH_TYPE_NONE;
        if ((authID != null) && (authID.length() > 0) &&
            (authPW != null) && (authPW.length() > 0))
        {
          authType = Constants.AUTH_TYPE_SIMPLE;
        }

        client = new Client(null, null, serverAddress, serverClientPort,
                            serverStatPort, useTimeSync, enableRealTimeStats,
                            statReportInterval, persistStats,
                            persistenceDirectory, persistenceInterval, authType,
                            authID, authPW, restrictedMode,
                            useCustomClassLoader, classDirectory, useSSL,
                            sslBlindTrust, sslKeyStore, sslKeyStorePassword,
                            sslTrustStore, sslTrustStorePassword, this);
        client.setShutdownListener(this);
        connectButton.setText("Disconnect");
        client.start();
        connectDialog.setVisible(false);
      }
      catch (ClientException ce)
      {
        messageArea.append("Unable to connect:  " + ce);
        connectDialog.setVisible(false);
      }
    }
    else if (source == dialogCancelButton)
    {
      connectDialog.setVisible(false);
    }
    else if (source == copyAllButton)
    {
      int selectionStart = messageArea.getSelectionStart();
      int selectionEnd   = messageArea.getSelectionEnd();
      messageArea.selectAll();
      messageArea.copy();
      messageArea.setSelectionStart(selectionStart);
      messageArea.setSelectionEnd(selectionEnd);
    }
    else if (source == copySelectedButton)
    {
      messageArea.copy();
    }
  }



  /**
   * Detects a change event (i.e., checkbox selection/deselection) and performs
   * the appropriate task.
   *
   * @param  event  The change event that occurred.
   */
  public void stateChanged(ChangeEvent event)
  {
    Object source = event.getSource();

    if (source == useSSLCheckbox)
    {
      useSSL = useSSLCheckbox.isSelected();
      blindTrustCheckbox.setEnabled(useSSL);
      sslKeyStoreLabel.setEnabled(useSSL);
      sslKeyStoreField.setEnabled(useSSL);
      sslKeyStorePasswordLabel.setEnabled(useSSL);
      sslKeyStorePasswordField.setEnabled(useSSL);
      sslTrustStoreLabel.setEnabled(useSSL);
      sslTrustStoreField.setEnabled(useSSL);
      sslTrustStorePasswordLabel.setEnabled(useSSL);
      sslTrustStorePasswordField.setEnabled(useSSL);
    }
    else if (source == enableRealTimeCheckbox)
    {
      enableRealTimeStats = enableRealTimeCheckbox.isSelected();
      statPortLabel.setEnabled(enableRealTimeStats);
      statPortField.setEnabled(enableRealTimeStats);
      statIntervalLabel.setEnabled(enableRealTimeStats);
      statIntervalField.setEnabled(enableRealTimeStats);
    }
    else if (source == persistStatsCheckbox)
    {
      persistStats = persistStatsCheckbox.isSelected();
      persistDirLabel.setEnabled(persistStats);
      persistDirField.setEnabled(persistStats);
      persistIntervalLabel.setEnabled(persistStats);
      persistIntervalField.setEnabled(persistStats);
    }
  }



  /**
   * Creates the dialog that collects the information necessary to connect to
   * the SLAMD server.
   */
  public void createConnectDialog()
  {
    connectDialog = new JDialog(appWindow, "SLAMD Server Information", true);
    connectDialog.getContentPane().setLayout(new BorderLayout());

    JPanel             centerPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc         = new GridBagConstraints();

    gbc.gridx      = 0;
    gbc.gridy      = 0;
    gbc.gridheight = 1;
    gbc.gridwidth  = 2;
    gbc.weightx    = 1.0;
    gbc.weighty    = 1.0;
    gbc.anchor     = GridBagConstraints.WEST;
    centerPanel.add(new JLabel("Server Address"), gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    hostField = new JTextField(20);
    if (serverAddress != null)
    {
      hostField.setText(serverAddress);
    }
    centerPanel.add(hostField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    centerPanel.add(new JLabel("Server Port"), gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    portField = new JTextField(5);
    portField.setText(String.valueOf(serverClientPort));
    centerPanel.add(portField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    centerPanel.add(new JPanel());

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    useSSLCheckbox = new JCheckBox("Connect Using SSL");
    useSSLCheckbox.setSelected(useSSL);
    useSSLCheckbox.addChangeListener(this);
    centerPanel.add(useSSLCheckbox, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    blindTrustCheckbox = new JCheckBox("Blindly Trust any Certificate");
    blindTrustCheckbox.setSelected(sslBlindTrust);
    blindTrustCheckbox.setEnabled(useSSL);
    centerPanel.add(blindTrustCheckbox, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    sslKeyStoreLabel = new JLabel("SSL Key Store Location");
    sslKeyStoreLabel.setEnabled(useSSL);
    centerPanel.add(sslKeyStoreLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    sslKeyStoreField = new JTextField(20);
    if (sslKeyStore != null)
    {
      sslKeyStoreField.setText(sslKeyStore);
    }
    sslKeyStoreField.setEnabled(useSSL);
    centerPanel.add(sslKeyStoreField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    sslKeyStorePasswordLabel = new JLabel("SSL Key Store Password");
    sslKeyStorePasswordLabel.setEnabled(useSSL);
    centerPanel.add(sslKeyStorePasswordLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    sslKeyStorePasswordField = new JPasswordField(20);
    if (sslKeyStorePassword != null)
    {
      sslKeyStorePasswordField.setText(sslKeyStorePassword);
    }
    sslKeyStorePasswordField.setEnabled(useSSL);
    centerPanel.add(sslKeyStorePasswordField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    sslTrustStoreLabel = new JLabel("SSL Trust Store Location");
    sslTrustStoreLabel.setEnabled(useSSL);
    centerPanel.add(sslTrustStoreLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    sslTrustStoreField = new JTextField(20);
    if (sslTrustStore != null)
    {
      sslTrustStoreField.setText(sslTrustStore);
    }
    sslTrustStoreField.setEnabled(useSSL);
    centerPanel.add(sslTrustStoreField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    sslTrustStorePasswordLabel = new JLabel("SSL Trust Store Password");
    sslTrustStorePasswordLabel.setEnabled(useSSL);
    centerPanel.add(sslTrustStorePasswordLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    sslTrustStorePasswordField = new JPasswordField(20);
    if (sslTrustStorePassword != null)
    {
      sslTrustStorePasswordField.setText(sslTrustStorePassword);
    }
    sslTrustStorePasswordField.setEnabled(useSSL);
    centerPanel.add(sslTrustStorePasswordField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    centerPanel.add(new JPanel());

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    centerPanel.add(new JLabel("Authentication ID"), gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    authIDField = new JTextField(20);
    if (authID != null)
    {
      authIDField.setText(authID);
    }
    centerPanel.add(authIDField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    centerPanel.add(new JLabel("Authentication Password"), gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    authPWField = new JPasswordField(20);
    if (authPW != null)
    {
      authPWField.setText(authID);
    }
    centerPanel.add(authPWField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    centerPanel.add(new JPanel());

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    enableRealTimeCheckbox =
         new JCheckBox("Enable Real-Time Statistics Reporting");
    enableRealTimeCheckbox.setSelected(enableRealTimeStats);
    enableRealTimeCheckbox.addChangeListener(this);
    centerPanel.add(enableRealTimeCheckbox, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    statPortLabel = new JLabel("SLAMD Server Stat Port");
    statPortLabel.setEnabled(enableRealTimeStats);
    centerPanel.add(statPortLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    statPortField = new JTextField(5);
    statPortField.setText(String.valueOf(serverStatPort));
    statPortField.setEnabled(enableRealTimeStats);
    centerPanel.add(statPortField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    statIntervalLabel = new JLabel("Stat Report Interval");
    statIntervalLabel.setEnabled(enableRealTimeStats);
    centerPanel.add(statIntervalLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    statIntervalField = new JTextField(5);
    statIntervalField.setText(String.valueOf(statReportInterval));
    statIntervalField.setEnabled(enableRealTimeStats);
    centerPanel.add(statIntervalField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    persistStatsCheckbox = new JCheckBox("Enable Client-Side Stat Persistence");
    persistStatsCheckbox.setSelected(persistStats);
    persistStatsCheckbox.addChangeListener(this);
    centerPanel.add(persistStatsCheckbox, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    persistDirLabel = new JLabel("Stat Persistence Directory");
    persistDirLabel.setEnabled(persistStats);
    centerPanel.add(persistDirLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    persistDirField = new JTextField(20);
    persistDirField.setText(persistenceDirectory);
    persistDirField.setEnabled(persistStats);
    centerPanel.add(persistDirField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    persistIntervalLabel = new JLabel("Persistence Collection Interval");
    persistIntervalLabel.setEnabled(persistStats);
    centerPanel.add(persistIntervalLabel, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    persistIntervalField = new JTextField(5);
    persistIntervalField.setText(String.valueOf(persistenceInterval));
    persistIntervalField.setEnabled(persistStats);
    centerPanel.add(persistIntervalField, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    centerPanel.add(new JPanel());

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    aggregateCheckbox = new JCheckBox("Aggregate Thread Data");
    aggregateCheckbox.setSelected(aggregateThreadData);
    centerPanel.add(aggregateCheckbox, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    restrictedModeCheckbox = new JCheckBox("Use Restricted Mode");
    restrictedModeCheckbox.setSelected(restrictedMode);
    centerPanel.add(restrictedModeCheckbox, gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    verboseCheckbox = new JCheckBox("Use Verbose Mode");
    verboseCheckbox.setSelected(verboseMode);
    centerPanel.add(verboseCheckbox, gbc);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    dialogConnectButton = new JButton("Connect");
    dialogConnectButton.addActionListener(this);
    buttonPanel.add(dialogConnectButton);

    dialogCancelButton = new JButton("Cancel");
    dialogCancelButton.addActionListener(this);
    buttonPanel.add(dialogCancelButton);

    connectDialog.getContentPane().add(centerPanel, BorderLayout.CENTER);
    connectDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    connectDialog.pack();
    connectDialog.setVisible(true);
  }



  /**
   * Writes the provided message to the message text area.
   *
   * @param  message  The message to be written.
   */
  public void writeMessage(String message)
  {
    messageArea.append(message + EOL);
    messageArea.setCaretPosition(messageArea.getText().length());
  }



  /**
   * Writes the provided message to the message text area if verbose mode is
   * enabled.
   *
   * @param  message  The message to be written.
   */
  public void writeVerbose(String message)
  {
    if (verboseCheckbox.isSelected())
    {
      messageArea.append(message + EOL);
      messageArea.setCaretPosition(messageArea.getText().length());
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
    return verboseCheckbox.isSelected();
  }



  /**
   * Indicates that the client has disconnected from the server and that the
   * client may wish to take whatever action is appropriate.
   */
  public void clientDisconnected()
  {
    writeMessage("Disconnected from the SLAMD server");
    connectButton.setText("Connect");
    client = null;
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
        serverAddress = value;
      }
      else if (name.equals(PROPERTY_SLAMD_PORT))
      {
        serverClientPort = Integer.parseInt(value);
      }
      else if (name.equals(PROPERTY_SLAMD_STAT_PORT))
      {
        serverStatPort = Integer.parseInt(value);
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
          sslBlindTrust = true;
        }
        else if (value.equalsIgnoreCase("false") ||
                 value.equalsIgnoreCase("no") ||
                 value.equalsIgnoreCase("off") || value.equalsIgnoreCase("0"))
        {
          sslBlindTrust = false;
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
"-h {host}    --  The address of the SLAMD server." + eol +
"-p {port}    --  The port number of the SLAMD server." + eol +
"-P {port}    --  The port number that the SLAMD server uses for " + eol +
"                 collecting real-time statistics. " + eol +
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
"-L           --  Disable the custom class loader." + eol +
"-Y           --  Disable time synchronization with the SLAMD server." + eol +
"-v           --  Operate in verbose mode." + eol +
"-H           --  Show this usage information." + eol
                      );
  }
}

