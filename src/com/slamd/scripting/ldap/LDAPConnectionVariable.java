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
package com.slamd.scripting.ldap;



import java.util.ArrayList;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPSearchConstraints;
import netscape.ldap.LDAPSearchResults;

import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.jobs.SLAMDLDAPSocketFactory;
import com.slamd.job.JobClass;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringVariable;



/**
 * This class defines a variable that maintains a connection to an LDAP
 * directory server and allows for interaction with that server.  An LDAP
 * connection has the following methods:
 *
 * <UL>
 *   <LI>add(ldapentry entry) -- Adds the specified entry to the directory
 *       server.  Returns the result code of the add operation as an integer
 *       value.</LI>
 *   <LI>bind(string dn, string password) -- Performs an LDAPv3 bind operation
 *       with the specified DN and password.  Returns the result code of the
 *       bind operation as an integer value.</LI>
 *   <LI>compare(string dn, string attribute, string value) -- Performs an LDAP
 *       compare operation on the specified entry using the provided attribute
 *       and assertion value.  Returns the result code of the compare
 *       operation as an integer value (a successful compare will return either
 *       compareFalse() or compareTrue()).</LI>
 *   <LI>compareFalse() -- Retrieves the LDAP result code that indicates a
 *       compare operation returned a result of false.</LI>
 *   <LI>compareTrue() -- Retrieves the LDAP result code that indicates a
 *       compare operation returned a result of true.</LI>
 *   <LI>connect(string host, int port) -- Establishes an anonymous LDAPv3
 *       connection to the specified directory server.  Returns the result code
 *       of the connect operation as an integer value.</LI>
 *   <LI>connect(string host, int port, boolean useSSL) -- Establishes an
 *       anonymous LDAPv3 connection to the specified directory server,
 *       optionally using SSL for the communication.  Returns the result code
 *       of the connect operation as an integer value.</LI>
 *   <LI>connect(string host, int port, string bindDN, string bindPW) --
 *       Establishes an LDAPv3 connection to the specified directory server
 *       using simple authentication.  Returns the result code of the connect
 *       operation as an integer value.</LI>
 *   <LI>connect(string host, int port, string bindDN, string bindPW,
 *               int ldapVersion) -- Establishes an LDAP connection to the
 *        specified directory server using the simple authentication with the
 *        specified LDAP version.  Returns the result code of the connect
 *        operation as an integer value.</LI>
 *   <LI>connect(string host, int port, string bindDN, string bindPW,
 *               int ldapVersion, boolean useSSL) -- Establishes an LDAP
 *        connection to the specified directory server, optionally using SSL,
 *        and using the simple authentication with the specified LDAP version.
 *        Returns the result code of the connect operation as an integer
 *        value.</LI>
 *   <LI>delete(string dn) -- Removes the entry with the specified DN from the
 *       LDAP directory server.  Returns the result code of the delete operation
 *       as an integer value.</LI>.
 *   <LI>disconnect() -- Closes the connection to the directory server.  This
 *       method does not return a value.</LI>
 *   <LI>enableAttemptedOperationCounters() -- Enables the stat trackers that
 *       count the number of times the various LDAP operations were
 *       attempted.  This should only be called at most once per LDAP connection
 *       variable instance, and should be called before any LDAP operations have
 *       been attempted.  This method does not return a value.</LI>
 *   <LI>enableFailedOperationCounters() -- Enables the stat trackers that count
 *       the number of times the various LDAP operations did not complete
 *       successfully.  This should only be called at most once per LDAP
 *       connection variable instance, and should be called before any LDAP
 *       operations have been attempted.  This method does not return a
 *       value.</LI>
 *   <LI>enableOperationTimers() -- Enables the stat trackers that track the
 *       time required to perform the various LDAP operations.  This should only
 *       be called at most once per LDAP connection variable instance, and
 *       should be called before any LDAP operations have been attempted.  This
 *       method does not return a value.</LI>
 *   <LI>enableSuccessfulOperationCounters() -- Enables the stat trackers that
 *       count the number of times the various LDAP operations completed
 *       successfully.  This should only be called at most once per LDAP
 *       connection variable instance, and should be called before any LDAP
 *       operations have been attempted.  This method does not return a
 *       value.</LI>
 *   <LI>modify(string dn, ldapmodification mod) -- Performs the specified
 *       modification in the directory server.  Returns the result code of the
 *       modify operation as an integer value.</LI>
 *   <LI>modify(string dn, ldapmodificationset modSet) -- Performs the specified
 *       modifications in the directory server.  Returns the result code of the
 *       modify operation as an integer value.</LI>
 *   <LI>modifyRDN(string dn, string newRDN, boolean deleteOldRDN) -- Performs a
 *       modify RDN operation on the specified entry in the directory server.
 *       returns the result code of the operation as an integer value.
 *   <LI>nextEntry() -- Retrieves the next entry from the previous search
 *       operation.  If there are no more entries, then a null entry will be
 *       returned.</LI>
 *   <LI>scopeBase() -- Retrieves the integer value that represents a baseObject
 *       search scope.</LI>
 *   <LI>scopeOne() -- Retrieves the integer value that represents a oneLevel
 *       search scope.</LI>
 *   <LI>scopeSub() -- Retrieves the integer value that represents a
 *       wholeSubtree search scope.</LI>
 *   <LI>search(string base, int scope, string filter) -- Performs a search
 *       operation in the directory server.  Returns the result code of the
 *       search operation as an integer value.</LI>
 *   <LI>search(string base, int scope, string filter, stringarray attributes)
 *       -- Performs a search operation in the directory server.  Returns the
 *       result code of the search operation as an integer value.</LI>
 *   <LI>search(string base, int scope, string filter, stringarray attributes,
 *              int timelimit, int sizelimit) -- Performs a search operation in
 *       the directory server.  Returns the result code of the search operation
 *       as an integer value.</LI>
 *   <LI>success() -- Retrieves the LDAP result code that indicates an operation
 *       (other than a compare) completed successfully.</LI>
 * </UL>
 *
 *
 * @author   Neil A.  Wilson
 */
public class LDAPConnectionVariable
       extends Variable
{
  /**
   * The name that will be used for the data type of LDAP connection variables.
   */
  public static final String LDAP_CONNECTION_VARIABLE_TYPE = "ldapconnection";



  /**
   * The name of the method that performs an LDAP add operation.
   */
  public static final String ADD_METHOD_NAME = "add";



  /**
   * The method number for the "add" method.
   */
  public static final int ADD_METHOD_NUMBER = 0;



  /**
   * The name of the method that performs an LDAP bind operation.
   */
  public static final String BIND_METHOD_NAME = "bind";



  /**
   * The method number for the "bind" method.
   */
  public static final int BIND_METHOD_NUMBER = 1;



  /**
   * The name of the method that performs an LDAP compare operation.
   */
  public static final String COMPARE_METHOD_NAME = "compare";



  /**
   * The method number for the "compare" method.
   */
  public static final int COMPARE_METHOD_NUMBER = 2;



  /**
   * The name of the method that retrieves the result code associated with a
   * compare result of "false".
   */
  public static final String COMPARE_FALSE_METHOD_NAME = "comparefalse";



  /**
   * The method number for the "compareFalse" method.
   */
  public static final int COMPARE_FALSE_METHOD_NUMBER = 3;



  /**
   * The name of the method that retrieves the result code associated with a
   * compare result of "true".
   */
  public static final String COMPARE_TRUE_METHOD_NAME = "comparetrue";



  /**
   * The method number for the "compareTrue" method.
   */
  public static final int COMPARE_TRUE_METHOD_NUMBER = 4;



  /**
   * The name of the method that establishes a connection to an LDAP directory
   * server.
   */
  public static final String CONNECT_METHOD_NAME = "connect";



  /**
   * The method number for the first "connect" method.
   */
  public static final int CONNECT_1_METHOD_NUMBER = 5;



  /**
   * The method number for the second "connect" method.
   */
  public static final int CONNECT_2_METHOD_NUMBER = 6;



  /**
   * The method number for the third "connect" method.
   */
  public static final int CONNECT_3_METHOD_NUMBER = 7;



  /**
   * The method number for the fourth "connect" method.
   */
  public static final int CONNECT_4_METHOD_NUMBER = 8;



  /**
   * The method number for the fifth "connect" method.
   */
  public static final int CONNECT_5_METHOD_NUMBER = 9;



  /**
   * The name of the method that performs an LDAP delete operation.
   */
  public static final String DELETE_METHOD_NAME = "delete";



  /**
   * The method number for the "delete" method.
   */
  public static final int DELETE_METHOD_NUMBER = 10;



  /**
   * The name of the method that performs an LDAP disconnect operation.
   */
  public static final String DISCONNECT_METHOD_NAME = "disconnect";



  /**
   * The method number for the "disconnect" method.
   */
  public static final int DISCONNECT_METHOD_NUMBER = 11;



  /**
   * The name of the method that enables the stat trackers that count LDAP
   * operation attempts.
   */
  public static final String ENABLE_ATTEMPT_COUNTERS_METHOD_NAME =
       "enableattemptedoperationcounters";



  /**
   * The method number for the "enableAttemptedOperationCounters" method.
   */
  public static final int ENABLE_ATTEMPT_COUNTERS_METHOD_NUMBER = 12;



  /**
   * The name of the method that enables the stat trackers that count failed
   * LDAP operations.
   */
  public static final String ENABLE_FAILED_COUNTERS_METHOD_NAME =
       "enablefailedoperationcounters";



  /**
   * The method number for the "enableFailedOperationCounters" method.
   */
  public static final int ENABLE_FAILED_COUNTERS_METHOD_NUMBER = 13;



  /**
   * The name of the method that enables the stat trackers that time LDAP
   * operations.
   */
  public static final String ENABLE_OPERATION_TIMERS_METHOD_NAME =
       "enableoperationtimers";



  /**
   * The method number for the "enableOperationTimers" method.
   */
  public static final int ENABLE_OPERATION_TIMERS_METHOD_NUMBER = 14;



  /**
   * The name of the method that enables the stat trackers that count successful
   * LDAP operations.
   */
  public static final String ENABLE_SUCCESS_COUNTERS_METHOD_NAME =
       "enablesuccessfuloperationcounters";



  /**
   * The method number for the "enableSuccessfulOperationCounters" method.
   */
  public static final int ENABLE_SUCCESS_COUNTERS_METHOD_NUMBER = 15;



  /**
   * The name of the method that performs an LDAP modify operation.
   */
  public static final String MODIFY_METHOD_NAME = "modify";



  /**
   * The method number for the first "modify" method.
   */
  public static final int MODIFY_1_METHOD_NUMBER = 16;



  /**
   * The method number for the second "modify" method.
   */
  public static final int MODIFY_2_METHOD_NUMBER = 17;



  /**
   * The name of the method that performs an LDAP modify RDN operation.
   */
  public static final String MODIFY_RDN_METHOD_NAME = "modifyrdn";



  /**
   * The method number for the "modifyRDN" method.
   */
  public static final int MODIFY_RDN_METHOD_NUMBER = 18;



  /**
   * The name of the method that retrieves the next entry in the set of search
   * results.
   */
  public static final String NEXT_ENTRY_METHOD_NAME = "nextentry";



  /**
   * The method number for the "nextEntry" method.
   */
  public static final int NEXT_ENTRY_METHOD_NUMBER = 19;


  /**
   * The name of the method that retrieves the search scope to use for
   * base-level searches.
   */
  public static final String SCOPE_BASE_METHOD_NAME = "scopebase";



  /**
   * The method number for the "scopeBase" method.
   */
  public static final int SCOPE_BASE_METHOD_NUMBER = 20;



  /**
   * The name of the method that retrieves the search scope to use for onelevel
   * searches.
   */
  public static final String SCOPE_ONE_METHOD_NAME = "scopeone";



  /**
   * The method number for the "scopeOne" method.
   */
  public static final int SCOPE_ONE_METHOD_NUMBER = 21;



  /**
   * The name of the method that retrieves the search scope to use for subtree
   * searches.
   */
  public static final String SCOPE_SUB_METHOD_NAME = "scopesub";



  /**
   * The method number for the "scopeSub" method.
   */
  public static final int SCOPE_SUB_METHOD_NUMBER = 22;



  /**
   * The name of the method that performs an LDAP search operation.
   */
  public static final String SEARCH_METHOD_NAME = "search";



  /**
   * The method number for the first "search" method.
   */
  public static final int SEARCH_1_METHOD_NUMBER = 23;



  /**
   * The method number for the second "search" method.
   */
  public static final int SEARCH_2_METHOD_NUMBER = 24;



  /**
   * The method number for the third "search" method.
   */
  public static final int SEARCH_3_METHOD_NUMBER = 25;



  /**
   * The name of the method that retrieves the result code associated with a
   * successful LDAP operation.
   */
  public static final String SUCCESS_METHOD_NAME = "success";



  /**
   * The method number for the "success" method.
   */
  public static final int SUCCESS_METHOD_NUMBER = 26;



  /**
   * The set of methods associated with LDAP connection variables.
   */
  public static final Method[] LDAP_CONNECTION_VARIABLE_METHODS = new Method[]
  {
    new Method(ADD_METHOD_NAME,
               new String[] { LDAPEntryVariable.LDAP_ENTRY_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(BIND_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(COMPARE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(COMPARE_FALSE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(COMPARE_TRUE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(CONNECT_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(DELETE_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(DISCONNECT_METHOD_NAME, new String[0], null),
    new Method(ENABLE_ATTEMPT_COUNTERS_METHOD_NAME, new String[0], null),
    new Method(ENABLE_FAILED_COUNTERS_METHOD_NAME, new String[0], null),
    new Method(ENABLE_OPERATION_TIMERS_METHOD_NAME, new String[0], null),
    new Method(ENABLE_SUCCESS_COUNTERS_METHOD_NAME, new String[0], null),
    new Method(MODIFY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              LDAPModificationVariable.
                                   LDAP_MODIFICATION_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(MODIFY_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              LDAPModificationSetVariable.
                                   LDAP_MODIFICATION_SET_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(MODIFY_RDN_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              BooleanVariable.BOOLEAN_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(NEXT_ENTRY_METHOD_NAME, new String[0],
               LDAPEntryVariable.LDAP_ENTRY_VARIABLE_TYPE),
    new Method(SCOPE_BASE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SCOPE_ONE_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SCOPE_SUB_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SEARCH_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SEARCH_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SEARCH_METHOD_NAME,
               new String[] { StringVariable.STRING_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              StringVariable.STRING_VARIABLE_TYPE,
                              StringArrayVariable.STRING_ARRAY_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE,
                              IntegerVariable.INTEGER_VARIABLE_TYPE },
               IntegerVariable.INTEGER_VARIABLE_TYPE),
    new Method(SUCCESS_METHOD_NAME, new String[0],
               IntegerVariable.INTEGER_VARIABLE_TYPE)
  };



  // The job thread being used in conjunction with this variable.
  private JobClass jobThread;

  // The connection to the LDAP directory server.
  private LDAPConnection conn;

  // The set of LDAP search results from the last search operation.
  private LDAPSearchResults results;


  // Create stat tracker variables to track information about the various LDAP
  // operations.
  private boolean            enableAttemptCounters;
  private boolean            enableFailedCounters;
  private boolean            enableOperationTimers;
  private boolean            enableSuccessCounters;
  private IncrementalTracker attemptedAddCounter;
  private IncrementalTracker attemptedBindCounter;
  private IncrementalTracker attemptedCompareCounter;
  private IncrementalTracker attemptedConnectCounter;
  private IncrementalTracker attemptedDeleteCounter;
  private IncrementalTracker attemptedModifyCounter;
  private IncrementalTracker attemptedModifyRDNCounter;
  private IncrementalTracker attemptedSearchCounter;
  private IncrementalTracker failedAddCounter;
  private IncrementalTracker failedBindCounter;
  private IncrementalTracker failedCompareCounter;
  private IncrementalTracker failedConnectCounter;
  private IncrementalTracker failedDeleteCounter;
  private IncrementalTracker failedModifyCounter;
  private IncrementalTracker failedModifyRDNCounter;
  private IncrementalTracker failedSearchCounter;
  private IncrementalTracker successfulAddCounter;
  private IncrementalTracker successfulBindCounter;
  private IncrementalTracker successfulCompareCounter;
  private IncrementalTracker successfulConnectCounter;
  private IncrementalTracker successfulDeleteCounter;
  private IncrementalTracker successfulModifyCounter;
  private IncrementalTracker successfulModifyRDNCounter;
  private IncrementalTracker successfulSearchCounter;
  private TimeTracker        addTimer;
  private TimeTracker        bindTimer;
  private TimeTracker        compareTimer;
  private TimeTracker        connectTimer;
  private TimeTracker        deleteTimer;
  private TimeTracker        modifyTimer;
  private TimeTracker        modifyRDNTimer;
  private TimeTracker        searchTimer;



  /**
   * Creates a new variable with no name, to be used only when creating a
   * variable with <CODE>Class.newInstance()</CODE>, and only when
   * <CODE>setName()</CODE> is called after that to set the name.
   *
   * @throws  ScriptException  If a problem occurs while initializing the new
   *                           variable.
   */
  public LDAPConnectionVariable()
         throws ScriptException
  {
    // Create the connection.
    conn = new LDAPConnection(new SLAMDLDAPSocketFactory());

    // Indicate that no statistics will be tracked by default.
    enableAttemptCounters = false;
    enableSuccessCounters = false;
    enableFailedCounters  = false;
    enableOperationTimers = false;
  }



  /**
   * Retrieves the name of the variable type for this variable.
   *
   * @return  The name of the variable type for this variable.
   */
  @Override()
  public String getVariableTypeName()
  {
    return LDAP_CONNECTION_VARIABLE_TYPE;
  }



  /**
   * Enables the stat trackers that track the number of times the various LDAP
   * operations were attempted.
   *
   * @param  clientID            The client ID to use for the stat trackers.
   * @param  threadID            The thread ID to use for the stat trackers.
   * @param  collectionInterval  The collection interval to use for the stat
   *                             trackers.
   */
  public void enableAttemptCounters(String clientID, String threadID,
                                    int collectionInterval)
  {
    enableAttemptCounters = true;
    attemptedAddCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Add Operations",
                                collectionInterval);
    attemptedBindCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Bind Operations",
                                collectionInterval);
    attemptedCompareCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Compare Operations",
                                collectionInterval);
    attemptedConnectCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Connect Operations",
                                collectionInterval);
    attemptedDeleteCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Delete Operations",
                                collectionInterval);
    attemptedModifyCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Modify Operations",
                                collectionInterval);
    attemptedModifyRDNCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() +
                                " -- Attempted Modify RDN Operations",
                                collectionInterval);
    attemptedSearchCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Attempted Search Operations",
                                collectionInterval);

    RealTimeStatReporter statReporter = jobThread.getStatReporter();
    if (statReporter != null)
    {
      String jobID = jobThread.getJobID();

      attemptedAddCounter.enableRealTimeStats(statReporter, jobID);
      attemptedBindCounter.enableRealTimeStats(statReporter, jobID);
      attemptedCompareCounter.enableRealTimeStats(statReporter, jobID);
      attemptedConnectCounter.enableRealTimeStats(statReporter, jobID);
      attemptedDeleteCounter.enableRealTimeStats(statReporter, jobID);
      attemptedModifyCounter.enableRealTimeStats(statReporter, jobID);
      attemptedModifyRDNCounter.enableRealTimeStats(statReporter, jobID);
      attemptedSearchCounter.enableRealTimeStats(statReporter, jobID);
    }

    attemptedAddCounter.startTracker();
    attemptedBindCounter.startTracker();
    attemptedCompareCounter.startTracker();
    attemptedConnectCounter.startTracker();
    attemptedDeleteCounter.startTracker();
    attemptedModifyCounter.startTracker();
    attemptedModifyRDNCounter.startTracker();
    attemptedSearchCounter.startTracker();
  }



  /**
   * Enables the stat trackers that track the number of times the various LDAP
   * operations were completed successfully.
   *
   * @param  clientID            The client ID to use for the stat trackers.
   * @param  threadID            The thread ID to use for the stat trackers.
   * @param  collectionInterval  The collection interval to use for the stat
   *                             trackers.
   */
  public void enableSuccessCounters(String clientID, String threadID,
                                    int collectionInterval)
  {
    enableSuccessCounters = true;
    successfulAddCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Add Operations",
                                collectionInterval);
    successfulBindCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Bind Operations",
                                collectionInterval);
    successfulCompareCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Compare Operations",
                                collectionInterval);
    successfulConnectCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Connect Operations",
                                collectionInterval);
    successfulDeleteCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Delete Operations",
                                collectionInterval);
    successfulModifyCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Modify Operations",
                                collectionInterval);
    successfulModifyRDNCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() +
                                " -- Successful Modify RDN Operations",
                                collectionInterval);
    successfulSearchCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Successful Search Operations",
                                collectionInterval);

    RealTimeStatReporter statReporter = jobThread.getStatReporter();
    if (statReporter != null)
    {
      String jobID = jobThread.getJobID();

      successfulAddCounter.enableRealTimeStats(statReporter, jobID);
      successfulBindCounter.enableRealTimeStats(statReporter, jobID);
      successfulCompareCounter.enableRealTimeStats(statReporter, jobID);
      successfulConnectCounter.enableRealTimeStats(statReporter, jobID);
      successfulDeleteCounter.enableRealTimeStats(statReporter, jobID);
      successfulModifyCounter.enableRealTimeStats(statReporter, jobID);
      successfulModifyRDNCounter.enableRealTimeStats(statReporter, jobID);
      successfulSearchCounter.enableRealTimeStats(statReporter, jobID);
    }

    successfulAddCounter.startTracker();
    successfulBindCounter.startTracker();
    successfulCompareCounter.startTracker();
    successfulConnectCounter.startTracker();
    successfulDeleteCounter.startTracker();
    successfulModifyCounter.startTracker();
    successfulModifyRDNCounter.startTracker();
    successfulSearchCounter.startTracker();
  }



  /**
   * Enables the stat trackers that track the number of times the various LDAP
   * operations were unable to complete successfully.
   *
   * @param  clientID            The client ID to use for the stat trackers.
   * @param  threadID            The thread ID to use for the stat trackers.
   * @param  collectionInterval  The collection interval to use for the stat
   *                             trackers.
   */
  public void enableFailedCounters(String clientID, String threadID,
                                   int collectionInterval)
  {
    enableFailedCounters = true;
    failedAddCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Add Operations",
                                collectionInterval);
    failedBindCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Bind Operations",
                                collectionInterval);
    failedCompareCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Compare Operations",
                                collectionInterval);
    failedConnectCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Connect Operations",
                                collectionInterval);
    failedDeleteCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Delete Operations",
                                collectionInterval);
    failedModifyCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Modify Operations",
                                collectionInterval);
    failedModifyRDNCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() +
                                " -- Failed Modify RDN Operations",
                                collectionInterval);
    failedSearchCounter =
         new IncrementalTracker(clientID, threadID,
                                getName() + " -- Failed Search Operations",
                                collectionInterval);

    RealTimeStatReporter statReporter = jobThread.getStatReporter();
    if (statReporter != null)
    {
      String jobID = jobThread.getJobID();

      failedAddCounter.enableRealTimeStats(statReporter, jobID);
      failedBindCounter.enableRealTimeStats(statReporter, jobID);
      failedCompareCounter.enableRealTimeStats(statReporter, jobID);
      failedConnectCounter.enableRealTimeStats(statReporter, jobID);
      failedDeleteCounter.enableRealTimeStats(statReporter, jobID);
      failedModifyCounter.enableRealTimeStats(statReporter, jobID);
      failedModifyRDNCounter.enableRealTimeStats(statReporter, jobID);
      failedSearchCounter.enableRealTimeStats(statReporter, jobID);
    }

    failedAddCounter.startTracker();
    failedBindCounter.startTracker();
    failedCompareCounter.startTracker();
    failedConnectCounter.startTracker();
    failedDeleteCounter.startTracker();
    failedModifyCounter.startTracker();
    failedModifyRDNCounter.startTracker();
    failedSearchCounter.startTracker();
  }



  /**
   * Enables the stat trackers that track the length of time required to perform
   * the various LDAP operations.
   *
   * @param  clientID            The client ID to use for the stat trackers.
   * @param  threadID            The thread ID to use for the stat trackers.
   * @param  collectionInterval  The collection interval to use for the stat
   *                             trackers.
   */
  public void enableOperationTimers(String clientID, String threadID,
                                    int collectionInterval)
  {
    enableOperationTimers = true;
    addTimer = new TimeTracker(clientID, threadID,
                               getName() + " -- Add Time (ms)",
                               collectionInterval);
    bindTimer = new TimeTracker(clientID, threadID,
                                getName() + " -- Bind Time (ms)",
                                collectionInterval);
    compareTimer = new TimeTracker(clientID, threadID,
                                   getName() + " -- Compare Time (ms)",
                                   collectionInterval);
    connectTimer = new TimeTracker(clientID, threadID,
                                   getName() + " -- Connect Time (ms)",
                                   collectionInterval);
    deleteTimer = new TimeTracker(clientID, threadID,
                                  getName() + " -- Delete Time (ms)",
                                  collectionInterval);
    modifyTimer = new TimeTracker(clientID, threadID,
                                  getName() + " -- Modify Time (ms)",
                                  collectionInterval);
    modifyRDNTimer = new TimeTracker(clientID, threadID,
                                     getName() + " -- Modify RDN Time (ms)",
                                     collectionInterval);
    searchTimer = new TimeTracker(clientID, threadID,
                                  getName() + " -- Search Time (ms)",
                                  collectionInterval);

    RealTimeStatReporter statReporter = jobThread.getStatReporter();
    if (statReporter != null)
    {
      String jobID = jobThread.getJobID();

      addTimer.enableRealTimeStats(statReporter, jobID);
      bindTimer.enableRealTimeStats(statReporter, jobID);
      compareTimer.enableRealTimeStats(statReporter, jobID);
      connectTimer.enableRealTimeStats(statReporter, jobID);
      deleteTimer.enableRealTimeStats(statReporter, jobID);
      modifyTimer.enableRealTimeStats(statReporter, jobID);
      modifyRDNTimer.enableRealTimeStats(statReporter, jobID);
      searchTimer.enableRealTimeStats(statReporter, jobID);
    }

    addTimer.startTracker();
    bindTimer.startTracker();
    compareTimer.startTracker();
    connectTimer.startTracker();
    deleteTimer.startTracker();
    modifyTimer.startTracker();
    modifyRDNTimer.startTracker();
    searchTimer.startTracker();
  }



  /**
   * Retrieves the stat trackers that have been maintained for this LDAP
   * connection variable.
   *
   * @return  The stat trackers that have been maintained for this LDAP
   *          connection variable.
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
    if (enableAttemptCounters)
    {
      if ((attemptedAddCounter != null) &&
          (attemptedAddCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedAddCounter);
      }

      if ((attemptedBindCounter != null) &&
          (attemptedBindCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedBindCounter);
      }

      if ((attemptedCompareCounter != null) &&
          (attemptedCompareCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedCompareCounter);
      }

      if ((attemptedConnectCounter != null) &&
          (attemptedConnectCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedConnectCounter);
      }

      if ((attemptedDeleteCounter != null) &&
          (attemptedDeleteCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedDeleteCounter);
      }

      if ((attemptedModifyCounter != null) &&
          (attemptedModifyCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedModifyCounter);
      }

      if ((attemptedModifyRDNCounter != null) &&
          (attemptedModifyRDNCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedModifyRDNCounter);
      }

      if ((attemptedSearchCounter != null) &&
          (attemptedSearchCounter.getTotalCount() > 0))
      {
        trackerList.add(attemptedSearchCounter);
      }
    }


    if (enableSuccessCounters)
    {
      if ((successfulAddCounter != null) &&
          (successfulAddCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulAddCounter);
      }

      if ((successfulBindCounter != null) &&
          (successfulBindCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulBindCounter);
      }

      if ((successfulCompareCounter != null) &&
          (successfulCompareCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulCompareCounter);
      }

      if ((successfulConnectCounter != null) &&
          (successfulConnectCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulConnectCounter);
      }

      if ((successfulDeleteCounter != null) &&
          (successfulDeleteCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulDeleteCounter);
      }

      if ((successfulModifyCounter != null) &&
          (successfulModifyCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulModifyCounter);
      }

      if ((successfulModifyRDNCounter != null) &&
          (successfulModifyRDNCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulModifyRDNCounter);
      }

      if ((successfulSearchCounter != null) &&
          (successfulSearchCounter.getTotalCount() > 0))
      {
        trackerList.add(successfulSearchCounter);
      }
    }


    if (enableFailedCounters)
    {
      if ((failedAddCounter != null) &&
          (failedAddCounter.getTotalCount() > 0))
      {
        trackerList.add(failedAddCounter);
      }

      if ((failedBindCounter != null) &&
          (failedBindCounter.getTotalCount() > 0))
      {
        trackerList.add(failedBindCounter);
      }

      if ((failedCompareCounter != null) &&
          (failedCompareCounter.getTotalCount() > 0))
      {
        trackerList.add(failedCompareCounter);
      }

      if ((failedConnectCounter != null) &&
          (failedConnectCounter.getTotalCount() > 0))
      {
        trackerList.add(failedConnectCounter);
      }

      if ((failedDeleteCounter != null) &&
          (failedDeleteCounter.getTotalCount() > 0))
      {
        trackerList.add(failedDeleteCounter);
      }

      if ((failedModifyCounter != null) &&
          (failedModifyCounter.getTotalCount() > 0))
      {
        trackerList.add(failedModifyCounter);
      }

      if ((failedModifyRDNCounter != null) &&
          (failedModifyRDNCounter.getTotalCount() > 0))
      {
        trackerList.add(failedModifyRDNCounter);
      }

      if ((failedSearchCounter != null) &&
          (failedSearchCounter.getTotalCount() > 0))
      {
        trackerList.add(failedSearchCounter);
      }
    }


    if (enableOperationTimers)
    {
      if ((addTimer != null) &&
          (addTimer.getTotalCount() > 0))
      {
        trackerList.add(addTimer);
      }

      if ((bindTimer != null) &&
          (bindTimer.getTotalCount() > 0))
      {
        trackerList.add(bindTimer);
      }

      if ((compareTimer != null) &&
          (compareTimer.getTotalCount() > 0))
      {
        trackerList.add(compareTimer);
      }

      if ((connectTimer != null) &&
          (connectTimer.getTotalCount() > 0))
      {
        trackerList.add(connectTimer);
      }

      if ((deleteTimer != null) &&
          (deleteTimer.getTotalCount() > 0))
      {
        trackerList.add(deleteTimer);
      }

      if ((modifyTimer != null) &&
          (modifyTimer.getTotalCount() > 0))
      {
        trackerList.add(modifyTimer);
      }

      if ((modifyRDNTimer != null) &&
          (modifyRDNTimer.getTotalCount() > 0))
      {
        trackerList.add(modifyRDNTimer);
      }

      if ((searchTimer != null) &&
          (searchTimer.getTotalCount() > 0))
      {
        trackerList.add(searchTimer);
      }
    }

    StatTracker[] trackers = new StatTracker[trackerList.size()];
    trackerList.toArray(trackers);
    return trackers;
  }



  /**
   * Starts the stat trackers associated with this variable.
   *
   * @param  jobThread  The job thread with which the stat trackers are
   *                    associated.
   */
  @Override()
  public void startStatTrackers(JobClass jobThread)
  {
    // The functionality of this method will be handled by the enableXXX
    // methods, but we want to grab the job thread now so we can use its
    // information for the stat counters.
    this.jobThread = jobThread;
  }



  /**
   * Stops the stat trackers associated with this variable.
   */
  @Override()
  public void stopStatTrackers()
  {
    if (attemptedAddCounter != null)
    {
      attemptedAddCounter.stopTracker();
    }

    if (attemptedBindCounter != null)
    {
      attemptedBindCounter.stopTracker();
    }

    if (attemptedCompareCounter != null)
    {
      attemptedCompareCounter.stopTracker();
    }

    if (attemptedConnectCounter != null)
    {
      attemptedConnectCounter.stopTracker();
    }

    if (attemptedDeleteCounter != null)
    {
      attemptedDeleteCounter.stopTracker();
    }

    if (attemptedModifyCounter != null)
    {
      attemptedModifyCounter.stopTracker();
    }

    if (attemptedModifyRDNCounter != null)
    {
      attemptedModifyRDNCounter.stopTracker();
    }

    if (attemptedSearchCounter != null)
    {
      attemptedSearchCounter.stopTracker();
    }

    if (successfulAddCounter != null)
    {
      successfulAddCounter.stopTracker();
    }

    if (successfulBindCounter != null)
    {
      successfulBindCounter.stopTracker();
    }

    if (successfulCompareCounter != null)
    {
      successfulCompareCounter.stopTracker();
    }

    if (successfulConnectCounter != null)
    {
      successfulConnectCounter.stopTracker();
    }

    if (successfulDeleteCounter != null)
    {
      successfulDeleteCounter.stopTracker();
    }

    if (successfulModifyCounter != null)
    {
      successfulModifyCounter.stopTracker();
    }

    if (successfulModifyRDNCounter != null)
    {
      successfulModifyRDNCounter.stopTracker();
    }

    if (successfulSearchCounter != null)
    {
      successfulSearchCounter.stopTracker();
    }

    if (failedAddCounter != null)
    {
      failedAddCounter.stopTracker();
    }

    if (failedBindCounter != null)
    {
      failedBindCounter.stopTracker();
    }

    if (failedCompareCounter != null)
    {
      failedCompareCounter.stopTracker();
    }

    if (failedConnectCounter != null)
    {
      failedConnectCounter.stopTracker();
    }

    if (failedDeleteCounter != null)
    {
      failedDeleteCounter.stopTracker();
    }

    if (failedModifyCounter != null)
    {
      failedModifyCounter.stopTracker();
    }

    if (failedModifyRDNCounter != null)
    {
      failedModifyRDNCounter.stopTracker();
    }

    if (failedSearchCounter != null)
    {
      failedSearchCounter.stopTracker();
    }

    if (addTimer != null)
    {
      addTimer.stopTracker();
    }

    if (bindTimer != null)
    {
      bindTimer.stopTracker();
    }

    if (compareTimer != null)
    {
      compareTimer.stopTracker();
    }

    if (connectTimer != null)
    {
      connectTimer.stopTracker();
    }

    if (deleteTimer != null)
    {
      deleteTimer.stopTracker();
    }

    if (modifyTimer != null)
    {
      modifyTimer.stopTracker();
    }

    if (modifyRDNTimer != null)
    {
      modifyRDNTimer.stopTracker();
    }

    if (searchTimer != null)
    {
      searchTimer.stopTracker();
    }
  }



  /**
   * Retrieves a list of all methods defined for this variable.
   *
   * @return  A list of all methods defined for this variable.
   */
  @Override()
  public Method[] getMethods()
  {
    return LDAP_CONNECTION_VARIABLE_METHODS;
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
    for (int i=0; i < LDAP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_CONNECTION_VARIABLE_METHODS[i].getName().equals(methodName))
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
    for (int i=0; i < LDAP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
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
    for (int i=0; i < LDAP_CONNECTION_VARIABLE_METHODS.length; i++)
    {
      if (LDAP_CONNECTION_VARIABLE_METHODS[i].hasSignature(methodName,
                                                           argumentTypes))
      {
        return LDAP_CONNECTION_VARIABLE_METHODS[i].getReturnType();
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
      case ADD_METHOD_NUMBER:
        // Retrieve the entry to add.
        LDAPEntryVariable lev = (LDAPEntryVariable)
                                arguments[0].getArgumentValue();

        // Perform the add operation and get the result code.
        if (enableAttemptCounters) { attemptedAddCounter.increment(); }
        if (enableOperationTimers) { addTimer.startTimer(); }
        int resultCode = LDAPException.SUCCESS;
        try
        {
          conn.add(lev.toLDAPEntry());
          if (enableSuccessCounters) { successfulAddCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedAddCounter.increment(); }
        }
        if (enableOperationTimers) { addTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case BIND_METHOD_NUMBER:
        // Retrieve the bind DN and password.
        StringVariable sv1 = (StringVariable) arguments[0].getArgumentValue();
        StringVariable sv2 = (StringVariable) arguments[1].getArgumentValue();
        String bindDN = sv1.getStringValue();
        String bindPW = sv2.getStringValue();

        // Perform the bind operation and get the result code.
        if (enableAttemptCounters) { attemptedBindCounter.increment(); }
        if (enableOperationTimers) { bindTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.bind(3, bindDN, bindPW);
          if (enableSuccessCounters) { successfulBindCounter.increment(); }
        }
        catch (LDAPException le)
        {
          if (enableFailedCounters) { failedBindCounter.increment(); }
          resultCode = le.getLDAPResultCode();
        }
        if (enableOperationTimers) { bindTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case COMPARE_METHOD_NUMBER:
        // Retrieve the string argument values.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        sv2 = (StringVariable) arguments[1].getArgumentValue();
        StringVariable sv3 = (StringVariable) arguments[2].getArgumentValue();
        String dn    = sv1.getStringValue();
        String name  = sv2.getStringValue();
        String value = sv3.getStringValue();
        LDAPAttribute attr = new LDAPAttribute(name, value);

        // Perform the compare operation and get the result code.
        if (enableAttemptCounters) { attemptedCompareCounter.increment(); }
        if (enableOperationTimers) { compareTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          boolean match = conn.compare(dn, attr);
          resultCode = match
                       ? LDAPException.COMPARE_TRUE
                       : LDAPException.COMPARE_FALSE;
          if (enableSuccessCounters) { successfulCompareCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedCompareCounter.increment(); }
        }
        if (enableOperationTimers) { compareTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case COMPARE_FALSE_METHOD_NUMBER:
        // Return the result code as an integer value.
        return new IntegerVariable(LDAPException.COMPARE_FALSE);
      case COMPARE_TRUE_METHOD_NUMBER:
        // Return the result code as an integer value.
        return new IntegerVariable(LDAPException.COMPARE_TRUE);
      case CONNECT_1_METHOD_NUMBER:
        // Get the host and port.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        IntegerVariable iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        String host = sv1.getStringValue();
        int    port = iv1.getIntValue();

        // Perform the connect operation and get the result code.
        if (enableAttemptCounters) { attemptedConnectCounter.increment(); }
        if (enableOperationTimers) { connectTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.setSocketFactory(null);
          conn.connect(3, host, port, "", "");
          if (enableSuccessCounters) { successfulConnectCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedConnectCounter.increment(); }
        }
        if (enableOperationTimers) { connectTimer.stopTimer(); }

        // Return the result code as an integer value
        return new IntegerVariable(resultCode);
      case CONNECT_2_METHOD_NUMBER:
        // Get the host and port.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        BooleanVariable bv1 = (BooleanVariable) arguments[2].getArgumentValue();

        host           = sv1.getStringValue();
        port           = iv1.getIntValue();
        boolean useSSL = bv1.getBooleanValue();

        // Perform the connect operation and get the result code.
        if (enableAttemptCounters) { attemptedConnectCounter.increment(); }
        if (enableOperationTimers) { connectTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          if (useSSL)
          {
            conn.setSocketFactory(new JSSEBlindTrustSocketFactory());
          }
          else
          {
            conn.setSocketFactory(null);
          }

          conn.connect(3, host, port, "", "");
          if (enableSuccessCounters) { successfulConnectCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedConnectCounter.increment(); }
        }
        if (enableOperationTimers) { connectTimer.stopTimer(); }

        // Return the result code as an integer value
        return new IntegerVariable(resultCode);
      case CONNECT_3_METHOD_NUMBER:
        // Get the host, port, bind DN, and password.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        sv2 = (StringVariable)  arguments[2].getArgumentValue();
        sv3 = (StringVariable)  arguments[3].getArgumentValue();
        host = sv1.getStringValue();
        port = iv1.getIntValue();
        bindDN = sv2.getStringValue();
        bindPW = sv3.getStringValue();

        // Perform the connect operation and get the result code.
        if (enableAttemptCounters) { attemptedConnectCounter.increment(); }
        if (enableOperationTimers) { connectTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.setSocketFactory(null);
          conn.connect(3, host, port, bindDN, bindPW);
          if (enableSuccessCounters) { successfulConnectCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedConnectCounter.increment(); }
        }
        if (enableOperationTimers) { connectTimer.stopTimer(); }

        // Return the result code as an integer value
        return new IntegerVariable(resultCode);
      case CONNECT_4_METHOD_NUMBER:
        // Get the host, port, bind DN, password, and LDAP version.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        sv2 = (StringVariable)  arguments[2].getArgumentValue();
        sv3 = (StringVariable)  arguments[3].getArgumentValue();
        IntegerVariable iv2 = (IntegerVariable) arguments[4].getArgumentValue();
        host = sv1.getStringValue();
        port = iv1.getIntValue();
        bindDN = sv2.getStringValue();
        bindPW = sv3.getStringValue();
        int ldapVersion = iv2.getIntValue();

        // Perform the connect operation and get the result code.
        if (enableAttemptCounters) { attemptedConnectCounter.increment(); }
        if (enableOperationTimers) { connectTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.setSocketFactory(null);
          conn.connect(ldapVersion, host, port, bindDN, bindPW);
          if (enableSuccessCounters) { successfulConnectCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedConnectCounter.increment(); }
        }
        if (enableOperationTimers) { connectTimer.stopTimer(); }

        // Return the result code as an integer value
        return new IntegerVariable(resultCode);
      case CONNECT_5_METHOD_NUMBER:
        // Get the host, port, bind DN, password, and LDAP version.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        sv2 = (StringVariable)  arguments[2].getArgumentValue();
        sv3 = (StringVariable)  arguments[3].getArgumentValue();
        iv2 = (IntegerVariable) arguments[4].getArgumentValue();
        bv1 = (BooleanVariable) arguments[5].getArgumentValue();
        host = sv1.getStringValue();
        port = iv1.getIntValue();
        bindDN = sv2.getStringValue();
        bindPW = sv3.getStringValue();
        ldapVersion = iv2.getIntValue();
        useSSL = bv1.getBooleanValue();

        // Perform the connect operation and get the result code.
        if (enableAttemptCounters) { attemptedConnectCounter.increment(); }
        if (enableOperationTimers) { connectTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          if (useSSL)
          {
            conn.setSocketFactory(new JSSEBlindTrustSocketFactory());
          }
          else
          {
            conn.setSocketFactory(null);
          }

          conn.connect(ldapVersion, host, port, bindDN, bindPW);
          if (enableSuccessCounters) { successfulConnectCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedConnectCounter.increment(); }
        }
        if (enableOperationTimers) { connectTimer.stopTimer(); }

        // Return the result code as an integer value
        return new IntegerVariable(resultCode);
      case DELETE_METHOD_NUMBER:
        // Get the value of the string argument
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        dn = sv1.getStringValue();

        // Perform the delete operation and get the result code.
        if (enableAttemptCounters) { attemptedDeleteCounter.increment(); }
        if (enableOperationTimers) { deleteTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.delete(dn);
          if (enableSuccessCounters) { successfulDeleteCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedDeleteCounter.increment(); }
        }
        if (enableOperationTimers) { deleteTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case DISCONNECT_METHOD_NUMBER:
        // Disconnect and don't return a value.
        try
        {
          conn.disconnect();
        } catch (LDAPException le) {}
        return null;
      case ENABLE_ATTEMPT_COUNTERS_METHOD_NUMBER:
        // Enable the counters and don't return a value.
        enableAttemptCounters(jobThread.getClientID(), jobThread.getThreadID(),
                              jobThread.getCollectionInterval());
        return null;
      case ENABLE_FAILED_COUNTERS_METHOD_NUMBER:
        // Enable the counters and don't return a value.
        enableFailedCounters(jobThread.getClientID(), jobThread.getThreadID(),
                             jobThread.getCollectionInterval());
        return null;
      case ENABLE_OPERATION_TIMERS_METHOD_NUMBER:
        // Enable the counters and don't return a value.
        enableOperationTimers(jobThread.getClientID(), jobThread.getThreadID(),
                              jobThread.getCollectionInterval());
        return null;
      case ENABLE_SUCCESS_COUNTERS_METHOD_NUMBER:
        // Enable the counters and don't return a value.
        enableSuccessCounters(jobThread.getClientID(), jobThread.getThreadID(),
                              jobThread.getCollectionInterval());
        return null;
      case MODIFY_1_METHOD_NUMBER:
        // Get the entry DN and modification.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        dn = sv1.getStringValue();
        LDAPModificationVariable lmv = (LDAPModificationVariable)
                                       arguments[1].getArgumentValue();
        LDAPModification mod = lmv.toLDAPModification();

        // Perform the modify operation and get the result code.
        if (enableAttemptCounters) { attemptedModifyCounter.increment(); }
        if (enableOperationTimers) { modifyTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.modify(dn, mod);
          if (enableSuccessCounters) { successfulModifyCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedModifyCounter.increment(); }
        }
        if (enableOperationTimers) { modifyTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case MODIFY_2_METHOD_NUMBER:
        // Get the entry DN and modification.
        sv1 = (StringVariable) arguments[0].getArgumentValue();
        dn = sv1.getStringValue();
        LDAPModificationSetVariable lmsv = (LDAPModificationSetVariable)
                                           arguments[1].getArgumentValue();
        LDAPModification[] mods = lmsv.toLDAPModifications();

        // Perform the modify operation and get the result code.
        if (enableAttemptCounters) { attemptedModifyCounter.increment(); }
        if (enableOperationTimers) { modifyTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.modify(dn, mods);
          if (enableSuccessCounters) { successfulModifyCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedModifyCounter.increment(); }
        }
        if (enableOperationTimers) { modifyTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case MODIFY_RDN_METHOD_NUMBER:
        // Get the argument values.
        sv1  = (StringVariable) arguments[0].getArgumentValue();
        sv2  = (StringVariable) arguments[1].getArgumentValue();
        BooleanVariable bv = (BooleanVariable) arguments[2].getArgumentValue();
        dn = sv1.getStringValue();
        String  newRDN = sv2.getStringValue();
        boolean deleteOldRDN = bv.getBooleanValue();

        // Perform the modify RDN operation and get the result code.
        if (enableAttemptCounters) { attemptedModifyRDNCounter.increment(); }
        if (enableOperationTimers) { modifyRDNTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          conn.rename(dn, newRDN, deleteOldRDN);
          if (enableSuccessCounters) { successfulModifyRDNCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedModifyRDNCounter.increment(); }
        }
        if (enableOperationTimers) { modifyRDNTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case NEXT_ENTRY_METHOD_NUMBER:
        // If the search result set is null or empty, then return a null entry.
        // Otherwise, return the next entry from the result set.
        if (results != null)
        {
          while (results.hasMoreElements())
          {
            Object element = results.nextElement();
            if (element instanceof LDAPEntry)
            {
              return new LDAPEntryVariable((LDAPEntry) element);
            }
          }
        }

        // We didn't return a real entry, so return a null entry.
        return new LDAPEntryVariable();
      case SCOPE_BASE_METHOD_NUMBER:
        // Return the scope value as an integer.
        return new IntegerVariable(LDAPConnection.SCOPE_BASE);
      case SCOPE_ONE_METHOD_NUMBER:
        // Return the scope value as an integer.
        return new IntegerVariable(LDAPConnection.SCOPE_ONE);
      case SCOPE_SUB_METHOD_NUMBER:
        // Return the scope value as an integer.
        return new IntegerVariable(LDAPConnection.SCOPE_SUB);
      case SEARCH_1_METHOD_NUMBER:
        // Get the base, scope, and filter.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        sv2 = (StringVariable)  arguments[2].getArgumentValue();
        String base         = sv1.getStringValue();
        int    scope        = iv1.getIntValue();
        String filter       = sv2.getStringValue();

        // Perform the search operation and get the result code.
        LDAPSearchConstraints constraints = conn.getSearchConstraints();
        constraints.setMaxResults(0);
        constraints.setTimeLimit(0);
        constraints.setServerTimeLimit(0);
        if (enableAttemptCounters) { attemptedSearchCounter.increment(); }
        if (enableOperationTimers) { searchTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          results = conn.search(base, scope, filter, null, false, constraints);
          if (enableSuccessCounters) { successfulSearchCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedSearchCounter.increment(); }
        }
        if (enableOperationTimers) { searchTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case SEARCH_2_METHOD_NUMBER:
        // Get the base, scope, filter, and attributes to return.
        sv1 = (StringVariable)  arguments[0].getArgumentValue();
        iv1 = (IntegerVariable) arguments[1].getArgumentValue();
        sv2 = (StringVariable)  arguments[2].getArgumentValue();
        StringArrayVariable sav = (StringArrayVariable)
                                  arguments[3].getArgumentValue();
        base   = sv1.getStringValue();
        scope  = iv1.getIntValue();
        filter = sv2.getStringValue();
        String[] attrs = sav.getStringValues();

        // Perform the search operation and get the result code.
        constraints = conn.getSearchConstraints();
        constraints.setMaxResults(0);
        constraints.setTimeLimit(0);
        constraints.setServerTimeLimit(0);
        if (enableAttemptCounters) { attemptedSearchCounter.increment(); }
        if (enableOperationTimers) { searchTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          results = conn.search(base, scope, filter, attrs, false, constraints);
          if (enableSuccessCounters) { successfulSearchCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedSearchCounter.increment(); }
        }
        if (enableOperationTimers) { searchTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case SEARCH_3_METHOD_NUMBER:
        // Get the base, scope, filter, attributes to return, timelimit, and
        // sizelimit.
        sv1 = (StringVariable)      arguments[0].getArgumentValue();
        iv1 = (IntegerVariable)     arguments[1].getArgumentValue();
        sv2 = (StringVariable)      arguments[2].getArgumentValue();
        sav = (StringArrayVariable) arguments[3].getArgumentValue();
        iv2 = (IntegerVariable)     arguments[4].getArgumentValue();
        IntegerVariable iv3 = (IntegerVariable) arguments[5].getArgumentValue();
        base   = sv1.getStringValue();
        scope  = iv1.getIntValue();
        filter = sv2.getStringValue();
        attrs  = sav.getStringValues();
        int timelimit = iv2.getIntValue();
        int sizelimit = iv3.getIntValue();

        // Perform the search operation and get the result code.
        constraints = conn.getSearchConstraints();
        constraints.setMaxResults(sizelimit);
        constraints.setTimeLimit(1000*timelimit);
        constraints.setServerTimeLimit(timelimit);
        if (enableAttemptCounters) { attemptedSearchCounter.increment(); }
        if (enableOperationTimers) { searchTimer.startTimer(); }
        resultCode = LDAPException.SUCCESS;
        try
        {
          results = conn.search(base, scope, filter, attrs, false, constraints);
          if (enableSuccessCounters) { successfulSearchCounter.increment(); }
        }
        catch (LDAPException le)
        {
          resultCode = le.getLDAPResultCode();
          if (enableFailedCounters) { failedSearchCounter.increment(); }
        }
        if (enableOperationTimers) { searchTimer.stopTimer(); }

        // Return the result code as an integer value.
        return new IntegerVariable(resultCode);
      case SUCCESS_METHOD_NUMBER:
        // Return the result code as an integer.
        return new IntegerVariable(LDAPException.SUCCESS);
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
    if (! argument.getArgumentType().equals(LDAP_CONNECTION_VARIABLE_TYPE))
    {
      throw new ScriptException("Attempt to assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                LDAP_CONNECTION_VARIABLE_TYPE + " rejected.");
    }

    LDAPConnectionVariable lcv = (LDAPConnectionVariable)
                                 argument.getArgumentValue();
    jobThread                  = lcv.jobThread;
    conn                       = lcv.conn;
    results                    = lcv.results;
    enableAttemptCounters      = lcv.enableAttemptCounters;
    enableFailedCounters       = lcv.enableFailedCounters;
    enableOperationTimers      = lcv.enableOperationTimers;
    enableSuccessCounters      = lcv.enableSuccessCounters;
    attemptedAddCounter        = lcv.attemptedAddCounter;
    attemptedBindCounter       = lcv.attemptedBindCounter;
    attemptedCompareCounter    = lcv.attemptedCompareCounter;
    attemptedConnectCounter    = lcv.attemptedConnectCounter;
    attemptedDeleteCounter     = lcv.attemptedDeleteCounter;
    attemptedModifyCounter     = lcv.attemptedModifyCounter;
    attemptedModifyRDNCounter  = lcv.attemptedModifyRDNCounter;
    attemptedSearchCounter     = lcv.attemptedSearchCounter;
    failedAddCounter           = lcv.failedAddCounter;
    failedBindCounter          = lcv.failedBindCounter;
    failedCompareCounter       = lcv.failedCompareCounter;
    failedConnectCounter       = lcv.failedConnectCounter;
    failedDeleteCounter        = lcv.failedDeleteCounter;
    failedModifyCounter        = lcv.failedModifyCounter;
    failedModifyRDNCounter     = lcv.failedModifyRDNCounter;
    failedSearchCounter        = lcv.failedSearchCounter;
    successfulAddCounter       = lcv.successfulAddCounter;
    successfulBindCounter      = lcv.successfulBindCounter;
    successfulCompareCounter   = lcv.successfulCompareCounter;
    successfulConnectCounter   = lcv.successfulConnectCounter;
    successfulDeleteCounter    = lcv.successfulDeleteCounter;
    successfulModifyCounter    = lcv.successfulModifyCounter;
    successfulModifyRDNCounter = lcv.successfulModifyRDNCounter;
    successfulSearchCounter    = lcv.successfulSearchCounter;
    addTimer                   = lcv.addTimer;
    bindTimer                  = lcv.bindTimer;
    compareTimer               = lcv.compareTimer;
    connectTimer               = lcv.connectTimer;
    deleteTimer                = lcv.deleteTimer;
    modifyTimer                = lcv.modifyTimer;
    modifyRDNTimer             = lcv.modifyRDNTimer;
    searchTimer                = lcv.searchTimer;
  }



  /**
   * Retrieves a string representation of the value of this argument.
   *
   * @return  A string representation of the value of this argument.
   */
  public String getValueAsString()
  {
    if (conn == null)
    {
      return "null";
    }
    else
    {
      boolean connected = conn.isConnected();
      if (! connected)
      {
        return "(not connected)";
      }


      String host = conn.getHost();
      int    port = conn.getPort();

      String protocol;
      if (conn.getSocketFactory() == null)
      {
        protocol = "ldap";
      }
      else
      {
        protocol = "ldaps";
      }

      if (conn.isAuthenticated())
      {
        String authDN = conn.getAuthenticationDN();
        return protocol + "://" + host + ':' + port + "/ authenticated as \"" +
               authDN + '"';
      }
      else
      {
        return protocol + "://" + host + ':' + port + '/';
      }
    }
  }
}

