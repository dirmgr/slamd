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
package com.slamd.dslogplay;



import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiChoiceParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PasswordParameter;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.CategoricalTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * This class defines a SLAMD job that can be used to replay requests parsed
 * from one or more Netscape / iPlanet / Sun ONE Directory Server access log
 * files.  It can be used to replay various kinds of operations either in their
 * original order or at random.
 *
 *
 * @author   Neil A. Wilson
 */
public class LogPlaybackJobClass
       extends JobClass
{
  /**
   * The display name for the stat tracker used to count the total number of
   * operations of all types replayed.
   */
  public static final String STAT_TRACKER_TOTAL_REPLAYED =
       "Total Operations Replayed";



  /**
   * The display name for the stat tracker used to count the total number of add
   * operations replayed.
   */
  public static final String STAT_TRACKER_ADDS_REPLAYED =
       "Add Operations Replayed";



  /**
   * The display name for the stat tracker used to count the total number of
   * bind operations replayed.
   */
  public static final String STAT_TRACKER_BINDS_REPLAYED =
       "Bind Operations Replayed";



  /**
   * The display name for the stat tracker used to count the total number of
   * compare operations replayed.
   */
  public static final String STAT_TRACKER_COMPARES_REPLAYED =
       "Compare Operations Replayed";



  /**
   * The display name for the stat tracker used to count the total number of
   * delete operations replayed.
   */
  public static final String STAT_TRACKER_DELETES_REPLAYED =
       "Delete Operations Replayed";



  /**
   * The display name for the stat tracker used to count the total number of
   * modify operations replayed.
   */
  public static final String STAT_TRACKER_MODS_REPLAYED =
       "Modify Operations Replayed";



  /**
   * The display name for the stat tracker used to count the total number of
   * search operations replayed.
   */
  public static final String STAT_TRACKER_SEARCHES_REPLAYED =
       "Search Operations Replayed";



  /**
   * The display name for the stat tracker used to track the average duration
   * for all operations replayed.
   */
  public static final String STAT_TRACKER_TOTAL_DURATION =
       "Overall Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the average duration
   * for add operations.
   */
  public static final String STAT_TRACKER_ADD_DURATION =
       "Add Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the average duration
   * for bind operations.
   */
  public static final String STAT_TRACKER_BIND_DURATION =
       "Bind Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the average duration
   * for compare operations.
   */
  public static final String STAT_TRACKER_COMPARE_DURATION =
       "Compare Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the average duration
   * for delete operations.
   */
  public static final String STAT_TRACKER_DELETE_DURATION =
       "Delete Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the average duration
   * for modify operations.
   */
  public static final String STAT_TRACKER_MODIFY_DURATION =
       "Modify Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the average duration
   * for search operations.
   */
  public static final String STAT_TRACKER_SEARCH_DURATION =
       "Search Operation Duration (ms)";



  /**
   * The display name for the stat tracker used to track the ratio of operations
   * replayed from the log file.
   */
  public static final String STAT_TRACKER_OPERATION_RATIOS = "Operation Ratios";



  /**
   * The display name for the stat tracker used to track the result codes for
   * the operations replayed.
   */
  public static final String STAT_TRACKER_RESULT_CODES =
       "Operation Result Codes";



  /**
   * The replay operation type that indicates that the operations should be
   * replayed in random order.
   */
  public static final String REPLAY_TYPE_RANDOM = "Replay in Random Order";



  /**
   * The replay operation type that indicates that the operations should be
   * replayed once in the order they appear in the log file.
   */
  public static final String REPLAY_TYPE_SEQUENTIAL_ONCE =
       "Replay Once in Sequential Order";



  /**
   * The replay operation type that indicates that the operations should be
   * replayed in the order they appear in the log file and that the sequence
   * should repeat when the end is reached.
   */
  public static final String REPLAY_TYPE_SEQUENTIAL_REPEATED =
       "Replay in Sequential Order Repeatedly";



  /**
   * The set of options that may be used to control how the operations are
   * replayed.
   */
  public static final String[] REPLAY_TYPES = new String[]
  {
    REPLAY_TYPE_RANDOM,
    REPLAY_TYPE_SEQUENTIAL_ONCE,
    REPLAY_TYPE_SEQUENTIAL_REPEATED
  };



  /**
   * The set of characters to include in randomly-generated values.
   */
  public static final char[] ALPHABET =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  // The parameter indicating whether to add an entry if an attempt to delete
  // it indicates that it doesn't exist.
  private BooleanParameter addMissingDeletesParameter =
       new BooleanParameter("add_missing_deletes",
                            "Add Missing Entries on Delete Attempts",
                            "Indicates whether attempts to delete an entry " +
                            "that does not exist should cause that entry to " +
                            "be added.", true);

  // The parameter indicating whether to perform a delete when attempting to add
  // an entry that already exists.
  private BooleanParameter deleteExistingAddsParameter =
       new BooleanParameter("delete_existing_add",
                            "Delete If Add Already Exists",
                            "Indicates whether an attempt to add an entry " +
                            "that already exists should delete that existing " +
                            "entry instead.", true);

  // The parameter indicating whether to replay adds.
  private BooleanParameter replayAddParameter =
       new BooleanParameter("replay_adds", "Replay Add Operations",
                            "Indicates whether the job should replay add " +
                            "operations contained in the access log.", false);

  // The parameter indicating whether to replay binds.
  private BooleanParameter replayBindParameter =
       new BooleanParameter("replay_binds", "Replay Bind Operations",
                            "Indicates whether the job should replay bind " +
                            "operations contained in the access log.", false);

  // The parameter indicating whether to replay compares.
  private BooleanParameter replayCompareParameter =
       new BooleanParameter("replay_compares", "Replay Compare Operations",
                            "Indicates whether the job should replay compare " +
                            "operations contained in the access log.", false);

  // The parameter indicating whether to replay deletes.
  private BooleanParameter replayDeleteParameter =
       new BooleanParameter("replay_deletes", "Replay Delete Operations",
                            "Indicates whether the job should replay delete " +
                            "operations contained in the access log.", false);

  // The parameter indicating whether to replay modifies.
  private BooleanParameter replayModifyParameter =
       new BooleanParameter("replay_modifies", "Replay Modify Operations",
                            "Indicates whether the job should replay modify " +
                            "operations contained in the access log.", false);

  // The parameter indicating whether to replay searches.
  private BooleanParameter replaySearchParameter =
       new BooleanParameter("replay_searches", "Replay Search Operations",
                            "Indicates whether the job should replay search " +
                            "operations contained in the access log.", false);

  // The parameter indicating whether to communicate with the server over SSL.
  private BooleanParameter useSSLParameter =
       new BooleanParameter("use_ssl", "Connect Using SSL",
                            "Indicates whether to connect to the directory " +
                            "server using SSL.", false);

  // The parameter specifying the number of operations between disconnects.
  private IntegerParameter opsBetweenDisconnectsParameter =
       new IntegerParameter("ops_between_disconnects",
                            "Operations Between Disconnects",
                            "Specifies the number of requests to process " +
                            "before disconnecting from and re-connecting to " +
                            "the directory server.  A value of zero " +
                            "indicates that all connections should be " +
                            "persistent.", true, 0, true, 0, false, 0);

  // The parameter specifying the port of the directory server.
  private IntegerParameter portParameter =
       new IntegerParameter("port", "Directory Server Port",
                            "The port of the directory server to which the " +
                            "connections should be established.", true, 389,
                            true, 1, true, 65535);

  // The parameter specifying the length of time between requests.
  private IntegerParameter timeBetweenRequestsParameter =
       new IntegerParameter("time_between_requests",
                            "Time Between Requests (ms)",
                            "The length of time in milliseconds that should " +
                            "elapse between the beginning of one request and " +
                            "the beginning of the next.  If a request takes " +
                            "longer to process than this time, then there " +
                            "will be no delay before the next request.", true,
                            0, true, 0, false, 0);

  // The parameter specifying the order for the replayed operations.
  private MultiChoiceParameter replayOrderParameter =
       new MultiChoiceParameter("replay_order", "Replay Order",
                                "The order in which to replay the operations " +
                                "from the log files.", REPLAY_TYPES,
                                REPLAY_TYPE_RANDOM);

  // The parameter specifying the paths to the log files containing the requests
  // to replay.
  private MultiLineTextParameter logFilesParameter =
       new MultiLineTextParameter("log_files", "Access Log Files to Replay",
                                  "The path(s) to the access log file(s) to " +
                                  "be replayed against the directory server.",
                                  null, true);

  // The parameter specifying the password to use to bind to the server.
  private PasswordParameter bindPasswordParameter =
       new PasswordParameter("bind_password", "Directory Server Bind Password",
                             "The password to use to bind to the directory " +
                             "server for replaying all operations other than " +
                             "binds.", false, "");

  // The parameter specifying the password to use for bind operations.
  private PasswordParameter bindOperationPasswordParameter =
       new PasswordParameter("bind_operation_password",
                             "Password for Bind Operations",
                             "The password to use when replaying bind " +
                             "operations.", false, "");

  // The placeholder used for spacing the parameters.
  private PlaceholderParameter placeholder = new PlaceholderParameter();

  // The parameter specifying the address of the directory server.
  private StringParameter addressParameter =
       new StringParameter("address", "Directory Server Address",
                           "The address of the directory server to which the " +
                           "connections should be established.", true, "");

  // The parameter specifying the DN to use to bind to the server.
  private StringParameter bindDNParameter =
       new StringParameter("bind_dn", "Directory Server Bind DN",
                           "The DN to use to bind to the directory server " +
                           "for replaying all operations other than binds.",
                           false, "");

  // The parameter specifying the attribute to replace for modify operations.
  private StringParameter modifyAttributeParameter =
       new StringParameter("modify_attr", "Attribute to Modify",
                           "The name of the attribute to replace when " +
                           "replaying modify operations.", true, "description");



  // The static class variables that correspond to the parameter values.
  protected static boolean  addMissingDeletes;
  protected static boolean  deleteExistingAdds;
  protected static boolean  doBind;
  protected static boolean  randomOrder;
  protected static boolean  repeatOrder;
  protected static boolean  replayAdds;
  protected static boolean  replayBinds;
  protected static boolean  replayCompares;
  protected static boolean  replayDeletes;
  protected static boolean  replayModifies;
  protected static boolean  replaySearches;
  protected static boolean  useSSL;
  protected static int      opsBetweenDisconnects;
  protected static int      port;
  protected static int      timeBetweenRequests;
  protected static String   address;
  protected static String   bindDN;
  protected static String   bindPassword;
  protected static String   bindOperationPassword;
  protected static String   modifyAttribute;
  protected static String[] logFiles;

  // The information about the operations to replay.
  private static int            nextOperation;
  private static LogOperation[] operationsToReplay;

  // The stat trackers used for this thread.
  protected CategoricalTracker opRatios;
  protected CategoricalTracker resultCodes;
  protected IncrementalTracker addsReplayed;
  protected IncrementalTracker bindsReplayed;
  protected IncrementalTracker comparesReplayed;
  protected IncrementalTracker deletesReplayed;
  protected IncrementalTracker modifiesReplayed;
  protected IncrementalTracker searchesReplayed;
  protected IncrementalTracker totalReplayed;
  protected TimeTracker        addTimer;
  protected TimeTracker        bindTimer;
  protected TimeTracker        compareTimer;
  protected TimeTracker        deleteTimer;
  protected TimeTracker        modifyTimer;
  protected TimeTracker        searchTimer;
  protected TimeTracker        totalTimer;

  // The connections to the directory server to use for bind operations and all
  // other operations, respectively.
  protected LDAPConnection bindConnection;
  protected LDAPConnection opConnection;

  // The random number generators used by this job.
  private static Random parentRandom;
  private Random random;

  // The thread that will be interrupted if we need to forcefully stop the job.
  private Thread jobThread;



  /**
   * The default constructor used to create a new instance of the job class.
   * The only thing it should do is to invoke the superclass constructor.  All
   * other initialization should be performed in the <CODE>initialize</CODE>
   * method.
   */
  public LogPlaybackJobClass()
  {
    super();

    logFilesParameter.setVisibleRows(5);
    logFilesParameter.setVisibleColumns(80);
    jobThread = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Directory Server Log Playback";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Replay operations from a Sun DSEE access log file";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to replay operations from one or more Sun Java " +
      "System Directory Server access log files.  It was originally written " +
      "to work with DSEE 5.2 and may or may not work with log files " +
      "generated by other server versions."
    };
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "LDAP";
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
      addressParameter,
      portParameter,
      useSSLParameter,
      bindDNParameter,
      bindPasswordParameter,
      placeholder,
      logFilesParameter,
      replayOrderParameter,
      placeholder,
      replayAddParameter,
      replayBindParameter,
      replayCompareParameter,
      replayDeleteParameter,
      replayModifyParameter,
      replaySearchParameter,
      placeholder,
      bindOperationPasswordParameter,
      modifyAttributeParameter,
      addMissingDeletesParameter,
      deleteExistingAddsParameter,
      opsBetweenDisconnectsParameter,
      timeBetweenRequestsParameter
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
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_TOTAL_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_ADDS_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_BINDS_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_COMPARES_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_DELETES_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_MODS_REPLAYED,
                             collectionInterval),
      new IncrementalTracker(clientID, threadID, STAT_TRACKER_SEARCHES_REPLAYED,
                             collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_TOTAL_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_ADD_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_BIND_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_COMPARE_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_DELETE_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_MODIFY_DURATION,
                      collectionInterval),
      new TimeTracker(clientID, threadID, STAT_TRACKER_SEARCH_DURATION,
                      collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_TRACKER_RESULT_CODES,
                             collectionInterval),
      new CategoricalTracker(clientID, threadID, STAT_TRACKER_OPERATION_RATIOS,
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
      totalReplayed,
      addsReplayed,
      bindsReplayed,
      comparesReplayed,
      deletesReplayed,
      modifiesReplayed,
      searchesReplayed,
      totalTimer,
      addTimer,
      bindTimer,
      compareTimer,
      deleteTimer,
      modifyTimer,
      searchTimer,
      resultCodes,
      opRatios
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
    // Make sure that at least one type of operation was selected.
    boolean replayEnabled = false;
    boolean replayWrites  = false;
    boolean replayBinds   = false;
    boolean replayMods    = false;
    BooleanParameter replayAddParam =
         parameters.getBooleanParameter(replayAddParameter.getName());
    if ((replayAddParam != null) && replayAddParam.getBooleanValue())
    {
      replayEnabled = true;
      replayWrites  = true;
    }

    BooleanParameter replayBindParam =
         parameters.getBooleanParameter(replayBindParameter.getName());
    if ((replayBindParam != null) && replayBindParam.getBooleanValue())
    {
      replayEnabled = true;
      replayBinds   = true;
    }

    BooleanParameter replayCompareParam =
         parameters.getBooleanParameter(replayCompareParameter.getName());
    if ((replayCompareParam != null) && replayCompareParam.getBooleanValue())
    {
      replayEnabled = true;
    }

    BooleanParameter replayDeleteParam =
         parameters.getBooleanParameter(replayDeleteParameter.getName());
    if ((replayDeleteParam != null) && replayDeleteParam.getBooleanValue())
    {
      replayEnabled = true;
      replayWrites  = true;
    }

    BooleanParameter replayModifyParam =
         parameters.getBooleanParameter(replayModifyParameter.getName());
    if ((replayModifyParam != null) && replayModifyParam.getBooleanValue())
    {
      replayEnabled = true;
      replayWrites  = true;
      replayMods    = true;
    }

    BooleanParameter replaySearchParam =
         parameters.getBooleanParameter(replaySearchParameter.getName());
    if ((replaySearchParam != null) && replaySearchParam.getBooleanValue())
    {
      replayEnabled = true;
    }

    if (! replayEnabled)
    {
      throw new InvalidValueException("At least one type of operation to " +
                                      "replay must be selected.");
    }


    // If any write operations were selected, make sure that a bind DN and
    // password were provided.
    if (replayWrites)
    {
      String bindDN = null;
      String bindPW = null;
      StringParameter bindDNParam =
           parameters.getStringParameter(bindDNParameter.getName());
      if ((bindDNParam != null) && bindDNParam.hasValue())
      {
        bindDN = bindDNParam.getStringValue();
      }

      PasswordParameter bindPWParam =
           parameters.getPasswordParameter(bindPasswordParameter.getName());
      if ((bindPWParam != null) && bindPWParam.hasValue())
      {
        bindPW = bindPWParam.getStringValue();
      }

      if ((bindDN == null) || (bindPW == null))
      {
        throw new InvalidValueException("A bind DN and password must be " +
                                        "provided if any write operations " +
                                        "are to be replayed.");
      }
    }


    // If bind operations were selected, then make sure that a bind operation
    // password was provided.
    if (replayBinds)
    {
      PasswordParameter bindOpPWParam =
           parameters.getPasswordParameter(
                bindOperationPasswordParameter.getName());
      if ((bindOpPWParam == null) || (! bindOpPWParam.hasValue()))
      {
        throw new InvalidValueException("A bind operation password must be " +
                                        "provided if bind operations are to " +
                                        "be replayed.");
      }
    }


    // If modify operations were selected, then make sure that a modify
    // attribute was provided.
    if (replayMods)
    {
      StringParameter modAttrParam =
           parameters.getStringParameter(modifyAttributeParameter.getName());
      if ((modAttrParam == null) || (! modAttrParam.hasValue()))
      {
        throw new InvalidValueException("A modify attribute must be provided " +
                                        "if modify operations are to be " +
                                        "replayed.");
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
    // Get the necessary parameter values to connect to the server.
    boolean useSSL  = false;
    int     port    = 389;
    String  address = null;
    String  bindDN  = null;
    String  bindPW  = null;

    StringParameter addressParam =
         parameters.getStringParameter(addressParameter.getName());
    if ((addressParam != null) && addressParam.hasValue())
    {
      address = addressParam.getStringValue();
    }
    else
    {
      outputMessages.add("ERROR:  No directory server address was specified.");
      return false;
    }

    IntegerParameter portParam =
         parameters.getIntegerParameter(portParameter.getName());
    if ((portParam != null) && portParam.hasValue())
    {
      port = portParam.getIntValue();
    }
    else
    {
      outputMessages.add("ERROR:  No directory server port was specified.");
      return false;
    }

    BooleanParameter sslParam =
         parameters.getBooleanParameter(useSSLParameter.getName());
    if (sslParam != null)
    {
      useSSL = sslParam.getBooleanValue();
    }

    StringParameter dnParam =
         parameters.getStringParameter(bindDNParameter.getName());
    if ((dnParam != null) && (dnParam.hasValue()))
    {
      bindDN = dnParam.getStringValue();
    }

    PasswordParameter pwParam =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((pwParam != null) && (pwParam.hasValue()))
    {
      bindPW = pwParam.getStringValue();
    }


    boolean doBind = false;
    if (bindDN == null)
    {
      if (bindPW != null)
      {
        outputMessages.add("WARNING:  A bind password was provided but no " +
                           "bind DN.  No authentication will be performed.");
        outputMessages.add("");
      }
    }
    else
    {
      if (bindPW == null)
      {
        outputMessages.add("WARNING:  A bind DN was provided but no " +
                           "password.  No authentication will be performed.");
        outputMessages.add("");
      }
      else
      {
        doBind = true;
      }
    }


    // Initialize the LDAP connection.
    LDAPConnection conn;
    if (useSSL)
    {
      try
      {
        outputMessages.add("Going to perform SSL initialization.");

        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        conn = new LDAPConnection(sslUtil.createSSLSocketFactory());

        outputMessages.add("SSL initialization completed successfully.");
        outputMessages.add("");
      }
      catch (Exception e)
      {
        outputMessages.add("ERROR:  SSL initialization failed:  " + e);
        return false;
      }
    }
    else
    {
      conn = new LDAPConnection();
    }


    // Connect to the directory server and optionally bind.
    if (doBind)
    {
      try
      {
        outputMessages.add("Going to connect to " + address + ':' + port +
                           " as " + bindDN);
        conn.connect(address, port, 10000);
        conn.bind(bindDN, bindPW);
        outputMessages.add("Connected successfully.");
        outputMessages.add("");
      }
      catch (Exception e)
      {
        outputMessages.add("Connection attempt failed:  " + e);
        return false;
      }
    }
    else
    {
      try
      {
        outputMessages.add("Going to connect to " + address + ':' + port);
        conn.connect(address, port, 10000);
        outputMessages.add("Connected successfully.");
        outputMessages.add("");
      }
      catch (Exception e)
      {
        outputMessages.add("Connection attempt failed:  " + e);
        return false;
      }
    }


    // Disconnect from the server and indicate that everything was successful.
    try
    {
      conn.close();
    } catch (Exception e) {}

    outputMessages.add("All tests completed successfully.");
    return true;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    // Initialize the parent random-number generator.
    parentRandom = new Random();


    // Get the information needed to connect to the server.
    addressParameter =
         parameters.getStringParameter(addressParameter.getName());
    if (addressParameter != null)
    {
      address = addressParameter.getStringValue();
    }

    portParameter = parameters.getIntegerParameter(portParameter.getName());
    if (portParameter != null)
    {
      port = portParameter.getIntValue();
    }

    useSSL = false;
    useSSLParameter = parameters.getBooleanParameter(useSSLParameter.getName());
    if (useSSLParameter != null)
    {
      useSSL = useSSLParameter.getBooleanValue();
    }

    bindDN = null;
    bindDNParameter = parameters.getStringParameter(bindDNParameter.getName());
    if ((bindDNParameter != null) && bindDNParameter.hasValue())
    {
      bindDN = bindDNParameter.getStringValue();
    }

    bindPassword = null;
    bindPasswordParameter =
         parameters.getPasswordParameter(bindPasswordParameter.getName());
    if ((bindPasswordParameter != null) && bindPasswordParameter.hasValue())
    {
      bindPassword = bindPasswordParameter.getStringValue();
    }
    doBind = ((bindDN != null) && (bindPassword != null));


    // Get the information about the log files to replay.
    logFiles = new String[0];
    logFilesParameter =
         parameters.getMultiLineTextParameter(logFilesParameter.getName());
    if (logFilesParameter != null)
    {
      logFiles = logFilesParameter.getNonBlankLines();
    }

    randomOrder   = true;
    repeatOrder   = true;
    nextOperation = -1;
    replayOrderParameter =
         parameters.getMultiChoiceParameter(replayOrderParameter.getName());
    if (replayOrderParameter != null)
    {
      String orderStr = replayOrderParameter.getStringValue();
      if (orderStr.equals(REPLAY_TYPE_SEQUENTIAL_ONCE))
      {
        randomOrder = false;
        repeatOrder = false;
      }
      else if (orderStr.equals(REPLAY_TYPE_SEQUENTIAL_REPEATED))
      {
        randomOrder = false;
        repeatOrder = true;
      }
    }


    // Get the information about the types of operations to replay.
    replayAdds = false;
    replayAddParameter =
         parameters.getBooleanParameter(replayAddParameter.getName());
    if (replayAddParameter != null)
    {
      replayAdds = replayAddParameter.getBooleanValue();
    }

    replayBinds = false;
    replayBindParameter =
         parameters.getBooleanParameter(replayBindParameter.getName());
    if (replayBindParameter != null)
    {
      replayBinds = replayBindParameter.getBooleanValue();
    }

    replayCompares = false;
    replayCompareParameter =
         parameters.getBooleanParameter(replayCompareParameter.getName());
    if (replayCompareParameter != null)
    {
      replayCompares = replayCompareParameter.getBooleanValue();
    }

    replayDeletes = false;
    replayDeleteParameter =
         parameters.getBooleanParameter(replayDeleteParameter.getName());
    if (replayDeleteParameter != null)
    {
      replayDeletes = replayDeleteParameter.getBooleanValue();
    }

    replayModifies = false;
    replayModifyParameter =
         parameters.getBooleanParameter(replayModifyParameter.getName());
    if (replayModifyParameter != null)
    {
      replayModifies = replayModifyParameter.getBooleanValue();
    }

    replaySearches = false;
    replaySearchParameter =
         parameters.getBooleanParameter(replaySearchParameter.getName());
    if (replaySearchParameter != null)
    {
      replaySearches = replaySearchParameter.getBooleanValue();
    }


    // Get the information needed for specific types of operations.
    bindOperationPassword = null;
    bindOperationPasswordParameter =
         parameters.getPasswordParameter(
              bindOperationPasswordParameter.getName());
    if (bindOperationPasswordParameter != null)
    {
      bindOperationPassword = bindOperationPasswordParameter.getStringValue();
    }

    modifyAttribute = "description";
    modifyAttributeParameter =
         parameters.getStringParameter(modifyAttributeParameter.getName());
    if (modifyAttributeParameter != null)
    {
      modifyAttribute = modifyAttributeParameter.getStringValue();
    }

    addMissingDeletes = false;
    addMissingDeletesParameter =
         parameters.getBooleanParameter(addMissingDeletesParameter.getName());
    if (addMissingDeletesParameter != null)
    {
      addMissingDeletes = addMissingDeletesParameter.getBooleanValue();
    }

    deleteExistingAdds = false;
    deleteExistingAddsParameter =
         parameters.getBooleanParameter(deleteExistingAddsParameter.getName());
    if (deleteExistingAddsParameter != null)
    {
      deleteExistingAdds = deleteExistingAddsParameter.getBooleanValue();
    }

    opsBetweenDisconnects = 0;
    opsBetweenDisconnectsParameter =
         parameters.getIntegerParameter(
              opsBetweenDisconnectsParameter.getName());
    if (opsBetweenDisconnectsParameter != null)
    {
      opsBetweenDisconnects = opsBetweenDisconnectsParameter.getIntValue();
    }

    timeBetweenRequests = 0;
    timeBetweenRequestsParameter =
         parameters.getIntegerParameter(timeBetweenRequestsParameter.getName());
    if (timeBetweenRequestsParameter != null)
    {
      timeBetweenRequests = timeBetweenRequestsParameter.getIntValue();
    }


    // Parse the specified log files.
    String currentLog = null;
    try
    {
      LogParser parser = new LogParser(this, replayAdds, replayBinds,
                                       replayCompares, replayDeletes,
                                       replayModifies, replaySearches);
      for (int i=0; i < logFiles.length; i++)
      {
        currentLog = logFiles[i];
        parser.parseLogFile(currentLog);
      }

      operationsToReplay = parser.getOperations();
    }
    catch (Exception e)
    {
      throw new UnableToRunException("An error occurred while trying to " +
                                     "parse the log file \"" + currentLog +
                                     "\":  " + e, e);
    }

    if ((operationsToReplay == null) || (operationsToReplay.length == 0))
    {
      throw new UnableToRunException("The provided log files did not appear " +
                                     "to contain any operations to replay.");
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
    // Initialize the child random number generator.
    random = new Random(parentRandom.nextLong());


    // Initialize all the stat trackers.
    totalReplayed = new IncrementalTracker(clientID, threadID,
                                           STAT_TRACKER_TOTAL_REPLAYED,
                                           collectionInterval);
    addsReplayed = new IncrementalTracker(clientID, threadID,
                                          STAT_TRACKER_ADDS_REPLAYED,
                                          collectionInterval);
    bindsReplayed = new IncrementalTracker(clientID, threadID,
                                           STAT_TRACKER_BINDS_REPLAYED,
                                           collectionInterval);
    comparesReplayed = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_COMPARES_REPLAYED,
                                              collectionInterval);
    deletesReplayed = new IncrementalTracker(clientID, threadID,
                                             STAT_TRACKER_DELETES_REPLAYED,
                                             collectionInterval);
    modifiesReplayed = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_MODS_REPLAYED,
                                              collectionInterval);
    searchesReplayed = new IncrementalTracker(clientID, threadID,
                                              STAT_TRACKER_SEARCHES_REPLAYED,
                                              collectionInterval);

    totalTimer = new TimeTracker(clientID, threadID,
                                 STAT_TRACKER_TOTAL_DURATION,
                                 collectionInterval);
    addTimer = new TimeTracker(clientID, threadID, STAT_TRACKER_ADD_DURATION,
                               collectionInterval);
    bindTimer = new TimeTracker(clientID, threadID, STAT_TRACKER_BIND_DURATION,
                                collectionInterval);
    compareTimer = new TimeTracker(clientID, threadID,
                                   STAT_TRACKER_COMPARE_DURATION,
                                   collectionInterval);
    deleteTimer = new TimeTracker(clientID, threadID,
                                  STAT_TRACKER_DELETE_DURATION,
                                  collectionInterval);
    modifyTimer = new TimeTracker(clientID, threadID,
                                  STAT_TRACKER_MODIFY_DURATION,
                                  collectionInterval);
    searchTimer = new TimeTracker(clientID, threadID,
                                  STAT_TRACKER_SEARCH_DURATION,
                                  collectionInterval);

    resultCodes = new CategoricalTracker(clientID, threadID,
                                         STAT_TRACKER_RESULT_CODES,
                                         collectionInterval);
    opRatios = new CategoricalTracker(clientID, threadID,
                                      STAT_TRACKER_OPERATION_RATIOS,
                                      collectionInterval);


    // Initialize and establish the connections to the directory server.
    if (useSSL)
    {
      try
      {
        SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
        bindConnection = new LDAPConnection(sslUtil.createSSLSocketFactory());
        opConnection   = new LDAPConnection(sslUtil.createSSLSocketFactory());
      }
      catch (Exception e)
      {
        throw new UnableToRunException("Unable to perform SSL initialization " +
                                       "for LDAP connections:  " + e, e);
      }
    }
    else
    {
      bindConnection = new LDAPConnection();
      opConnection   = new LDAPConnection();
    }

    try
    {
      if (doBind)
      {
        opConnection.connect(address, port, 10000);
        opConnection.bind(bindDN, bindPassword);
      }
      else
      {
        opConnection.connect(address, port);
      }

      bindConnection.connect(address, port);
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to establish the connections " +
                                     "to the directory server:  " + e, e);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Initialize the housekeeping variables.
    boolean needReconnect = false;
    int opsPlayed = 0;
    jobThread = currentThread();


    // Start all the stat trackers.
    opRatios.startTracker();
    resultCodes.startTracker();
    addsReplayed.startTracker();
    bindsReplayed.startTracker();
    comparesReplayed.startTracker();
    deletesReplayed.startTracker();
    modifiesReplayed.startTracker();
    searchesReplayed.startTracker();
    totalReplayed.startTracker();
    addTimer.startTracker();
    bindTimer.startTracker();
    compareTimer.startTracker();
    deleteTimer.startTracker();
    modifyTimer.startTracker();
    searchTimer.startTracker();
    totalTimer.startTracker();



    // Loop until it is determined that the job should stop.
    while (! shouldStop())
    {
      long startTime = System.currentTimeMillis();

      // See if we need to reconnect to the server.
      if (needReconnect)
      {
        try
        {
          opConnection.close();
        } catch (Exception e) {}

        try
        {
          if (doBind)
          {
            opConnection.connect(address, port, 10000);
            opConnection.bind(bindDN, bindPassword);
          }
          else
          {
            opConnection.connect(address, port);
          }
        }
        catch (Exception e)
        {
          logMessage("Unable to re-establish the connection to the " +
                     "directory server:  " + e);
          indicateStoppedDueToError();
          break;
        }
      }


      // Get the next operation to perform and replay it.
      LogOperation op = getNextOperation();
      if (op == null)
      {
        break;
      }
      op.replayOperation(this);


      // Increment the counter to see if we need to reconnect the next time
      // through.
      if (opsBetweenDisconnects > 0)
      {
        opsPlayed++;
        if (opsPlayed > opsBetweenDisconnects)
        {
          needReconnect = true;
          opsPlayed = 0;
        }
      }


      // See if we need to sleep before sending the next request.
      if (timeBetweenRequests > 0)
      {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long sleepTime   = timeBetweenRequests - elapsedTime;
        if (sleepTime > 0)
        {
          try
          {
            Thread.sleep(sleepTime);
          } catch (Exception e) {}
        }
      }
    }


    // Stop all the stat trackers.
    opRatios.stopTracker();
    resultCodes.stopTracker();
    addsReplayed.stopTracker();
    bindsReplayed.stopTracker();
    comparesReplayed.stopTracker();
    deletesReplayed.stopTracker();
    modifiesReplayed.stopTracker();
    searchesReplayed.stopTracker();
    totalReplayed.stopTracker();
    addTimer.stopTracker();
    bindTimer.stopTracker();
    compareTimer.stopTracker();
    deleteTimer.stopTracker();
    modifyTimer.stopTracker();
    searchTimer.stopTracker();
    totalTimer.stopTracker();


    // Close the connections to the directory server.
    try
    {
      opConnection.close();
    } catch (Exception e) {}

    try
    {
      bindConnection.close();
    } catch (Exception e) {}
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    // Close the connections to the directory server.
    try
    {
      bindConnection.close();
    } catch (Exception e) {}

    try
    {
      opConnection.close();
    } catch (Exception e) {}


    // Interrupt the job thread.
    try
    {
      jobThread.interrupt();
    } catch (Exception e) {}
  }



  /**
   * Retrieves a string containing the specified number of randomly-chosen
   * characters.
   *
   * @param  length  The number of characters to include in the generated
   *                 string.
   *
   * @return  The generated string.
   */
  public String getRandomString(int length)
  {
    Random r;
    if (random == null)
    {
      if (parentRandom == null)
      {
        parentRandom = new Random();
      }

      r = parentRandom;
    }
    else
    {
      r = random;
    }

    char[] chars = new char[length];
    for (int i=0; i < length; i++)
    {
      chars[i] = ALPHABET[r.nextInt(ALPHABET.length)];
    }

    return new String(chars);
  }



  /**
   * Retrieves the next log operation that should be replayed against the
   * directory server.
   *
   * @return  The next log operation that should be replayed against the
   *          directory server, or <CODE>null</CODE> if there are no more
   *          operations to replay.
   */
  public LogOperation getNextOperation()
  {
    if (randomOrder)
    {
      return operationsToReplay[random.nextInt(operationsToReplay.length)];
    }
    else
    {
      synchronized (operationsToReplay)
      {
        int opPos = ++nextOperation;
        if (opPos >= operationsToReplay.length)
        {
          if (repeatOrder)
          {
            opPos = 0;
            nextOperation = 0;
          }
          else
          {
            return null;
          }
        }

        return operationsToReplay[opPos];
      }
    }
  }
}

