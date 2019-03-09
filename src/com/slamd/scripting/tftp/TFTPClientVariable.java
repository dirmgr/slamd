/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Geoffrey Said.
 * Portions created by Geoffrey Said are Copyright (C) 2006.
 * All Rights Reserved.
 *
 * Contributor(s):  Geoffrey Said
 */
package com.slamd.scripting.tftp;

import com.slamd.job.JobClass;
import com.slamd.tftp.TFTPClient;
import com.slamd.scripting.engine.Argument;
import com.slamd.scripting.engine.Method;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.Variable;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.StringVariable;
import com.slamd.stat.StatTracker;



/**
 * Defines a SLAMD variable that can be included in scripts to simulate a TFTP
 * client.  The TFTP client provides the following methods:
 *
 * <UL>
 * <LI>    setServerIP(String serverIP):    Sets the TFTP Server IP.  This method
 *         those not return anything.
 * <LI>    setFileName(String fileName):    Assigns the file name to download from
 *         the TFTP server.  This method those not return anything.
 * <LI>    setDataTransferMode(String mode):    Indicates how the data should be
 *         transfered ie Binary, ASCII, and mail.  This method those not return
 *         anything.
 * <LI>    setNumberOfRetries(int numberOfRetries):    Affects how many
 *         times the client will try to retransmit the current packet.  This
 *         method those not return anything.
 * <LI>    setTimeout(int timeout):    Sets the timeout, in milliseconds, to wait
 *         for a packet to arrive over the network.  This method those not return
 *         anything.
 * <LI>    getServerIP():    Returns the server IP as a string.  The default is
 *         <I>"127.0.0.1"</I>.
 * <LI>    getFileName():    Returns a string containing the file name to fetch.
 *         The default is <I>"filename"</I>.
 * <LI>    getDataTransferMode():    Returns the current transfer mode stored as a
 *         string.  The default is <I>"octet"</I>.
 * <LI>    getNumberOfRetries():    Returns the number of retries as an integer.
 *         The default is <I>3</I>.
 * <LI>    getTimeout():    Returns an integer with the timeout, in    milliseconds.
 *         The default is <I>3000</I> which is equivalent to 3 seconds.
 * <LI>    getErrorMessage():    When an error message is received by the client, the
 *         text portion is stored in the <CODE>errorMessage</CODE> variable. This
 *         method returns the content of the variable as a string.  If no error
 *         packet has been received, the message <I>"No error message"</I> is
 *         returned.
 * <LI>    getFetchedDataLength():    Returns the total length of the downloaded file.
 *         It returns 0 if nothing has been fetched yet.
 * <LI>    getFile():    Fetches a file from the TFTP server.  Returns 0 if
 *         everything worked fine, 1 if an error packet was received, 2 if the
 *         maximum number of retries has been exceeded, and 3 if a
 *         TFTPClientException has been thrown by the TFTP client.
 * <LI>    enableStatisticsCollection():    Enables the gathering of statistical
 *         information.
 * <LI>    disableStatisticsCollection():    Disables the gathering of statistical
 *         information.
 * <LI>    isStatisticsEnabled():    Returns true if statistics collection is
 *         enabled and false if not.
 * <LI>    areWeCollectingNow():    Returns true if the client is currently
 *         collecting statistical information and false if not.
 * </UL>
 *
 * @author    2X Geoffrey Said
 */
public class TFTPClientVariable extends Variable
{
    // Static public variables declaration
    /**
     * The name that will be used to identify the tftp client variable.
     */
    public static final String TFTP_CLIENT_VARIABLE_TYPE = "tftpclient";

    /**
     * The name of the method that is used to set the TFTP server IP address.
     */
    public static final String SET_SERVER_IP_METHOD_NAME = "setserverip";

    /**
     * The number of the <I>setServerIP</I> method.
     */
    public static final int SET_SERVER_IP_METHOD_NUMBER = 0;

    /**
     * The name of the method that is used to set the file name to fetch.
     */
    public static final String SET_FILE_NAME_METHOD_NAME = "setfilename";

    /**
     * The number of the <I>setFileName</I> method.
     */
    public static final int SET_FILE_NAME_METHOD_NUMBER = 1;

    /**
     * The name of the method that is used to set the data transfer mode.
     */
    public static final String SET_DATA_TRANSFER_MODE_METHOD_NAME =
                                        "setdatatransfermode";

    /**
     * The number of the <I>setDataTransferMode</I> method.
     */
    public static final int SET_DATA_TRANSFER_MODE_METHOD_NUMBER = 2;

    /**
     * The name of the method that is used to set the number of retransmissions.
     */
    public static final String SET_NUMBER_OF_RETRIES_METHOD_NAME =
                                        "setnumberofretries";

    /**
     * The number of the <I>setNumberOfRetries</I> method.
     */
    public static final int SET_NUMBER_OF_RETRIES_METHOD_NUMBER = 3;

    /**
     * The name of the method that is used to set the timeout, in milliseconds,
     * to wait for a packet on the network before a retransmission is necessary.
     */
    public static final String SET_TIMEOUT_METHOD_NAME = "settimeout";

    /**
     * The number of the <I>setTimout</I> method.
     */
    public static final int SET_TIMEOUT_METHOD_NUMBER = 4;

    /**
     * The name of the method that is used to know the, currently set, TFTP server IP.
     */
    public static final String GET_SERVER_IP_METHOD_NAME = "getserverip";

    /**
     * The number of the <I>getServerIP</I> method.
     */
    public static final int GET_SERVER_IP_METHOD_NUMBER = 5;

    /**
     * The name of the method that is used to return the file name to fetch.
     */
    public static final String GET_FILE_NAME_METHOD_NAME = "getfilename";

    /**
     * The number of the <I>getFileName</I> method.
     */
    public static final int GET_FILE_NAME_METHOD_NUMBER = 6;

    /**
     * The name of the method that is used to know the download transfer mode.
     */
    public static final String GET_DATA_TRANSFER_MODE_METHOD_NAME =
                                        "getdatatransfermode";

    /**
     * The number of the <I>getDataTransferMode</I> method.
     */
    public static final int GET_DATA_TRANSFER_MODE_METHOD_NUMBER = 7;

    /**
     * The name of the method that is used to determine the number of retries
     * that is currently set.
     */
    public static final String GET_NUMBER_OF_RETRIES_METHOD_NAME =
                                        "getnumberofretries";

    /**
     * The number of the <I>getNumberOfRetries</I> method.
     */
    public static final int GET_NUMBER_OF_RETRIES_METHOD_NUMBER = 8;

    /**
     * The name of the method that is used to determine the current timeout
     * setting.
     */
    public static final String GET_TIMEOUT_METHOD_NAME = "gettimeout";

    /**
     * The number of the <I>getTimeout</I> method.
     */
    public static final int GET_TIMEOUT_METHOD_NUMBER = 9;

    /**
     * The name of the method that is used to retreive the error message
     * when a file transfer failes.
     */
    public static final String GET_ERROR_MESSAGE_METHOD_NAME =
                                        "geterrormessage";

    /**
     * The number of the <I>getErrorMessage</I> method.
     */
    public static final int GET_ERROR_MESSAGE_METHOD_NUMBER = 10;

    /**
     * The name of the method that is used to return the length of
     * any downloaded data.
     */
    public static final String GET_FETCHED_DATA_LENGTH_METHOD_NAME =
                                        "getfetcheddatalength";

    /**
     * The number of the <I>getFetchedDataLength</I> method.
     */
    public static final int GET_FETCHED_DATA_LENGTH_METHOD_NUMBER = 11;

    /**
     * The name of the method that is used to download a file from a particular
     * TFTP server.
     */
    public static final String GET_FILE_METHOD_NAME = "getfile";

    /**
     * The number of the <I>getFile</I> method.
     */
    public static final int GET_FILE_METHOD_NUMBER = 12;

    /**
     * The name of the method that is used to enable the automatic collection
     * of statistical data.
     */
    public static final String ENABLE_STATISTICS_COLLECTION_METHOD_NAME =
                                        "enablestatisticscollection";

    /**
     * The number of the <I>enableStatisticsCollection</I> method.
     */
    public static final int ENABLE_STATISTICS_COLLECTION_METHOD_NUMBER = 13;


    /**
     * The name of the method that is used to disable the gathering of
     * statistical information.
     */
    public static final String DISABLE_STATISTICS_COLLECTION_METHOD_NAME =
                                        "disablestatisticscollection";

    /**
     * The number of the <I>disableStatisticsCollection</I> method.
     */
    public static final int DISABLE_STATISTICS_COLLECTION_METHOD_NUMBER = 14;

    /**
     * The name of the method that is used to determine if the automatic
     * collection of statistical data is enabled.
     */
    public static final String IS_STATISTICS_ENABLED_METHOD_NAME =
                                        "isstatisticsenabled";

    /**
     * The number of the <I>isStatisticsEnabled</I> method.
     */
    public static final int IS_STATISTICS_ENABLED_METHOD_NUMBER = 15;

    /**
     * The name of the method that is used to check whether the object is
     * currently collection statistical information.
     */
    public static final String ARE_WE_COLLECTING_NOW_METHOD_NAME =
                                        "arewecollectingnow";

    /**
     * the number of the <I>areWeCollectingNow</I> method.
     */
    public static final int ARE_WE_COLLECTING_NOW_METHOD_NUMBER = 16;

    /**
     * The set of method provided by the TFTP client variable.
     */
    public static final Method[] TFTP_CLIENT_VARIABLE_METHODS = new Method[]
    {
        new Method(SET_SERVER_IP_METHOD_NAME,
                   new String[] {StringVariable.STRING_VARIABLE_TYPE}, null),
        new Method(SET_FILE_NAME_METHOD_NAME,
                             new String[] {StringVariable.STRING_VARIABLE_TYPE}, null),
        new Method(SET_DATA_TRANSFER_MODE_METHOD_NAME,
                             new String[] {StringVariable.STRING_VARIABLE_TYPE}, null),
        new Method(SET_NUMBER_OF_RETRIES_METHOD_NAME,
                             new String[] {IntegerVariable.INTEGER_VARIABLE_TYPE}, null),
        new Method(SET_TIMEOUT_METHOD_NAME,
                             new String[] {IntegerVariable.INTEGER_VARIABLE_TYPE}, null),
        new Method(GET_SERVER_IP_METHOD_NAME,
                             new String[0], StringVariable.STRING_VARIABLE_TYPE),
        new Method(GET_FILE_NAME_METHOD_NAME,
                             new String[0], StringVariable.STRING_VARIABLE_TYPE),
        new Method(GET_DATA_TRANSFER_MODE_METHOD_NAME,
                             new String[0], StringVariable.STRING_VARIABLE_TYPE),
        new Method(GET_NUMBER_OF_RETRIES_METHOD_NAME,
                             new String[0], IntegerVariable.INTEGER_VARIABLE_TYPE),
        new Method(GET_TIMEOUT_METHOD_NAME,
                             new String[0], IntegerVariable.INTEGER_VARIABLE_TYPE),
        new Method(GET_ERROR_MESSAGE_METHOD_NAME,
                             new String[0], StringVariable.STRING_VARIABLE_TYPE),
        new Method(GET_FETCHED_DATA_LENGTH_METHOD_NAME,
                             new String[0], IntegerVariable.INTEGER_VARIABLE_TYPE),
        new Method(GET_FILE_METHOD_NAME,
                             new String[0], IntegerVariable.INTEGER_VARIABLE_TYPE),
        new Method(ENABLE_STATISTICS_COLLECTION_METHOD_NAME,
                             new String[0], null),
        new Method(DISABLE_STATISTICS_COLLECTION_METHOD_NAME,    new String[0], null),
        new Method(IS_STATISTICS_ENABLED_METHOD_NAME, new String[0],
                             BooleanVariable.BOOLEAN_VARIABLE_TYPE),
        new Method(ARE_WE_COLLECTING_NOW_METHOD_NAME, new String[0],
                             BooleanVariable.BOOLEAN_VARIABLE_TYPE)
    };

    // The actual TFTP client that is going to handle all requests
    private TFTPClient tftpClient;

    // The variables associated with statistical information gathering
    private int collectionInterval;
    private String clientID;
    private String threadID;
    private StatTracker[] statTrackers;

    // A String to hold the last failure message
    private String failureMessage;

    /**
     * Creates a new TFTP client variable with no name, to be used only when
     * creating a variable with <CODE>Class.newInstance()</CODE>, and only when
     * <CODE>setName()</CODE> is called after that to set the name.
     *
     * @throws ScriptException    if a problem occurs while initializing the new
     *                             variable.
     */
    public TFTPClientVariable() throws ScriptException
    {
        tftpClient = new TFTPClient();
        failureMessage = null;
        statTrackers = new StatTracker[0];
    }

    /**
     * Creates a new TFTP client variable that is set to fetch files from
     * the specified TFTP server IP.
     *
     * @param serverIP            a string containing the TFTP server IP.
     *
     * @throws ScriptException    if a problem occurs while initializing the new
     *                             variable.
     */
    public TFTPClientVariable(String serverIP) throws ScriptException
    {
        tftpClient = new TFTPClient(serverIP);
        failureMessage = null;
        statTrackers = new StatTracker[0];
    }

    /**
     * Creates a new TFTP client variable that is set to fetch the file stored
     * in the <CODE>filename</CODE> variable from the TFTP server IP specified
     * in the <CODE>serverIP</CODE> variable.
     *
     * @param serverIP            a string containing the TFTP server IP.
     * @param fileName            a string containing the file name.
     * @throws ScriptException    if a problem occurs while initializing the new
     *                             variable.
     */
    public TFTPClientVariable(String serverIP, String fileName)
        throws ScriptException
    {
        tftpClient = new TFTPClient(serverIP, fileName);
        failureMessage = null;
        statTrackers = new StatTracker[0];
    }

    /*
     * Stores the passed message in the failureMessage variable
     */
    private void setFailureMessage(String message)
    {
        this.failureMessage = message;
    }

    /**
     * Returns any failure messages stored in the <CODE>failureMessage</CODE> variable.
     *
     * @return    a string containing the error message.
     */
    public String getFailureMessage()
    {
        return failureMessage;
    }

    /**
     * Returns the type of the current variable.
     *
     * @return    a string with the variable type.
     */
    @Override()
    public String getVariableTypeName()
    {
        return TFTP_CLIENT_VARIABLE_TYPE;
    }

    /**
     * Returns the set of methods that this client offers.
     *
     * @return    a method array with all the method signitures.
     */
    @Override()
    public Method[] getMethods()
    {
        return TFTP_CLIENT_VARIABLE_METHODS;
    }

    /**
     * Checks for the passed method name in the methods array.
     *
     * @param    methodName    the name of the method.
     *
     * @return    true if the name is in the methods array,
     *             false if the name those not match any method.
     */
    @Override()
    public boolean hasMethod(String methodName)
    {
        for (int x = 0; x < TFTP_CLIENT_VARIABLE_METHODS.length; x++)
        {
            if (TFTP_CLIENT_VARIABLE_METHODS[x].getName().equals(methodName))
                return true;
        }
        return false;
    }

    /**
     * Returns the method number as found in the methods array.
     *
     * @param    methodName        the name of the method.
     * @param    argumentTypes    the method's arguments.
     *
     * @return    -1 if the passed method's signiture those not match any stored
     *             in the methods array.
     *             the index position of the matched method.
     */
    @Override()
    public int getMethodNumber(String methodName, String[] argumentTypes)
    {
        for (int x = 0; x < TFTP_CLIENT_VARIABLE_METHODS.length; x++)
        {
            if (TFTP_CLIENT_VARIABLE_METHODS[x].hasSignature(methodName,
                                                             argumentTypes))
                return x;
        }
        return -1;
    }

    /**
     * Returns the return type of the specified method.
     *
     * @param    methodName        the name of the method.
     * @param    argumentTypes    the method's arguments.
     *
     * @return    null if the method is not found,
     *             the return type as a string.
     */
    @Override()
    public String getReturnTypeForMethod(String methodName,
                                         String[] argumentTypes)
    {
        int position = 0;

        position = getMethodNumber(methodName, argumentTypes);
        if (position >= 0)
            return TFTP_CLIENT_VARIABLE_METHODS[position].getReturnType();
        else
            return null;
    }

    /**
     * Executes the specified method, using the provided variables as arguments
     * to the method, and makes the return value available to the caller.
     *
     * @param    lineNumber        the line number in the script where the method call
     *                             occurred.
     * @param    methodNumber    the method number of the method to execute.
     * @param    arguments        the set of arguments to use for the method.
     *
     * @return    the value returned from the method, or null if it does not
     *             return anything.
     *
     * @throws    ScriptException    if the specified method does not exist, or if a
     *                             problem occurs while attempting to execute it.
     */
    @Override()
    public Variable executeMethod(int lineNumber, int methodNumber,
           Argument[] arguments) throws ScriptException
    {
        StringVariable sv;
        IntegerVariable iv;
        int returnCode;

        switch (methodNumber)
        {
            case SET_SERVER_IP_METHOD_NUMBER:
                sv = (StringVariable) arguments[0].getArgumentValue();
                tftpClient.setServerIP(sv.getStringValue());
                return null;
            case SET_FILE_NAME_METHOD_NUMBER:
                sv = (StringVariable) arguments[0].getArgumentValue();
                tftpClient.setFileName(sv.getStringValue());
                return null;
            case SET_DATA_TRANSFER_MODE_METHOD_NUMBER:
                sv = (StringVariable) arguments[0].getArgumentValue();
                tftpClient.setDataTransferMode(sv.getStringValue());
                return null;
            case SET_NUMBER_OF_RETRIES_METHOD_NUMBER:
                iv = (IntegerVariable) arguments[0].getArgumentValue();
                tftpClient.setNumberOfRetries(iv.getIntValue());
                return null;
            case SET_TIMEOUT_METHOD_NUMBER:
                iv = (IntegerVariable) arguments[0].getArgumentValue();
                tftpClient.setTimeout(iv.getIntValue());
                return null;
            case GET_SERVER_IP_METHOD_NUMBER:
                return new StringVariable(tftpClient.getServerIP());
            case GET_FILE_NAME_METHOD_NUMBER:
                return new StringVariable(tftpClient.getFileName());
            case GET_DATA_TRANSFER_MODE_METHOD_NUMBER:
                return new StringVariable(tftpClient.getDataTransferMode());
            case GET_NUMBER_OF_RETRIES_METHOD_NUMBER:
                return new IntegerVariable(tftpClient.getNumberOfRetries());
            case GET_TIMEOUT_METHOD_NUMBER:
                return new IntegerVariable(tftpClient.getTimeout());
            case GET_ERROR_MESSAGE_METHOD_NUMBER:
                return new StringVariable(getFailureMessage());
            case GET_FETCHED_DATA_LENGTH_METHOD_NUMBER:
                return new IntegerVariable(tftpClient.getFetchedDataLength());
            case GET_FILE_METHOD_NUMBER:
                try
                {
                    returnCode = tftpClient.getFile();
                    switch (returnCode)
                    {
                        case 1:
                            setFailureMessage(tftpClient.getErrorMessage());
                            return new IntegerVariable(1);
                        case 2:
                            setFailureMessage("Client: timeout occured!");
                            return new IntegerVariable(2);
                        default:
                            return new IntegerVariable(0);
                    }
                }
                catch(Exception exception)
                {
                    setFailureMessage(exception.toString());
                    return new IntegerVariable(3);
                }
            case ENABLE_STATISTICS_COLLECTION_METHOD_NUMBER:
                tftpClient.enableStatisticsCollection(clientID, threadID,
                                              collectionInterval);
                return null;
            case DISABLE_STATISTICS_COLLECTION_METHOD_NUMBER:
                tftpClient.disableStatisticsCollection();
                return null;
            case IS_STATISTICS_ENABLED_METHOD_NUMBER:
                return new BooleanVariable(tftpClient.isStatisticsEnabled());
            case ARE_WE_COLLECTING_NOW_METHOD_NUMBER:
                return new BooleanVariable(tftpClient.areWeCollectingNow());
            default:
                throw new ScriptException(lineNumber, "There is no method "
                                          + methodNumber + " defined for "
                                          + getArgumentType() + " variables.");
        }
    }

    /**
     * Assigns the value of the provided argument to this variable.
     *
     * @param    argument    the argument whose value is to be assigned to this
     *                         variable.
     *
     * @throws    ScriptException    if the argument cannot be assigned to this
     *                             variable.
     */
    @Override()
    public void assign(Argument argument) throws ScriptException
    {
        if (! argument.getArgumentType().equals(TFTP_CLIENT_VARIABLE_TYPE))
        {
            throw new ScriptException("Cannot assign argument of type "
                                      + argument.getArgumentType()
                                      + " to variable of type "
                                        + TFTP_CLIENT_VARIABLE_TYPE);
        }
        TFTPClientVariable tftpcv =
             (TFTPClientVariable) argument.getArgumentValue();
        tftpClient = tftpcv.tftpClient;
        failureMessage = tftpcv.failureMessage;
    }

    /**
     * Returns a string representation of the current object.
     *
     * @return    a string with the variable's data.
     */
    public String getValueAsString()
    {
        String message;

        message = "This is the current status of the TFTP Client:\n"
                  + "\tTFTP Server IP: \t" + tftpClient.getServerIP() + '\n'
                  + "\tFile to download: \t" + tftpClient.getFileName() + '\n'
                  + "\tData transfer mode: \t"
                  + tftpClient.getDataTransferMode()
                  + '\n' + "\tTimeout of: \t" + tftpClient.getTimeout()
                  + " milliseconds\n" + "\tNumber of Retries: \t"
                  + tftpClient.getNumberOfRetries();
        return message;
    }

    /**
     * Returns the statistical information.
     *
     * @return a statistical tracker array with all the trackers.
     */
    @Override()
    public StatTracker[] getStatTrackers()
    {
        return statTrackers;
    }

    /**
     * Collects information to be able to start the statistical trackers.
     *
     * @param    jobClass    the job class that is currently associated with
     *                         the trackers.
     */
    @Override()
    public void startStatTrackers(JobClass jobClass)
    {
        clientID           = jobClass.getClientID();
        threadID           = jobClass.getThreadID();
        collectionInterval = jobClass.getCollectionInterval();
    }

    /**
     * Calls the TFTP client method <CODE>getStatTrackers</CODE> to stop the
     * trackers and stores them in the <CODE>statTrackers</CODE> array.
     */
    @Override()
    public void stopStatTrackers()
    {
        statTrackers = tftpClient.getStatTrackers();
    }
}

