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
package com.slamd.tftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.StatTracker;
import com.slamd.stat.TimeTracker;



/**
 * Defines a client that can connect with a TFTP server and retreive files.
 * Provides features including the ability to set custom timeouts and number of
 * retries as well as the download method.  Only error messages are stored.  No
 * data is kept (only the total ammount of bytes is stored).
 *
 * @author  2X Geoffrey Said
 */
public class TFTPClient
{
    // Static public variables declaration.
    /**
     * Standard TFTP port to use when intializing communication with the
     * TFTP server.
     */
    public static final int DEFAULT_TFTP_SERVER_PORT = 69;

    /**
     * Data terminator to include in TFTP packet.
     */
    public static final byte PACKET_DATA_TERMINATOR = 00;

    /**
     * Maximum TFTP packet size to be used in creating byte buffers.
     */
    public static final int MAX_BUFFER_SIZE = 516;

    /**
     * Default client UDP port to use if no random one is used.
     */
    public static final int DEFAULT_UDP_PORT = 6254;

    /**
     * Default timeout, in milliseconds, to wait before the client retransmits
     * last packet.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 3000;

    /**
     * Default number of retries before transmitting an error TFTP packet to
     * the server.
     */
    public static final int DEFAULT_NUMBER_OF_RETRIES = 3;

    /**
     * Default download mode when building a data TFTP packet.
     */
    public static final String DEFAULT_MODE = "octet";

    /**
     * Default TFTP server IP.
     */
    public static final String DEFAULT_TFTP_SERVER_IP = "127.0.0.1";

    /**
     * Default filename to fetch from the TFTP server.
     */
    public static final String DEFAULT_FILENAME = "filename";

    /**
     * Value that defines a read request TFTP packet.
     */
    public static final byte PACKET_RRQ = 01;

    /**
     * Value that defines a write request TFTP packet.
     */
    public static final byte PACKET_WRQ = 02;

    /**
     * Value that defines a data TFTP packet.
     */
    public static final byte PACKET_DATA = 03;

    /**
     * Value that defines an acknowledge TFTP packet.
     */
    public static final byte PACKET_ACK = 04;

    /**
     * Value that defines an error TFTP packet.
     */
    public static final byte PACKET_ERROR = 05;

    /**
     * Ascii mode description string used in building a data TFTP packet
     */
    public static final String DOWNLOAD_NETASCII = "netascii";

    /**
     * Binary mode description string used in building a data TFTP packet
     */
    public static final String DOWNLOAD_OCTET = "octet";

    /**
     * Mail mode description string used in building a data TFTP packet.
     */
    public static final String DOWNLOAD_MAIL = "mail";

    /**
     * Value that defines a <B>NOT DEFINED</B> error TFTP packet.
     */
    public static final byte ERROR_NOT_DEFINED = 00;

    /**
     * Value that defines a <B>FILE NOT FOUND</B> error TFTP packet.
     */
    public static final byte ERROR_FILE_NOT_FOUND = 01;

    /**
     * Value that defines an <B>ACCESS VIOLATION</B> error TFTP packet.
     */
    public static final byte ERROR_ACCESS_VIOLATION = 02;

    /**
     * Value that defines a <B>DISK FULL</B> error TFTP packet.
     */
    public static final byte ERROR_DISK_FULL = 03;

    /**
     * Value that defines an <B>ILLEGAL TFTP OPERATION</B> error TFTP packet.
     */
    public static final byte ERROR_ILLEGAL_TFTP_OPERATION = 04;

    /**
     * Value that defines an <B>UNKNOWN TRANSFER ID</B> error TFTP packet.
     */
    public static final byte ERROR_UNKNOWN_TRANSFER_ID = 05;

    /**
     * Value that defines a <B>FILE ALREADY EXISTS</B> error TFTP packet.
     */
    public static final byte ERROR_FILE_ALREADY_EXISTS = 06;

    /**
     * Value that defines a <B>NO SUCH USER</B> error TFTP packet.
     */
    public static final byte ERROR_NO_SUCH_USER = 07;

    /**
     * String array that defines all error messages to write in an error TFTP
     * packet.
     */
    public static final String[] ERROR_MESSAGES = { "Timeout error occurred",
                                                    "File not found",
                                                    "Access violation",
                                                    "Disk is full",
                                                    "Illegal tftp operation",
                                                    "Unknown transfer ID",
                                                    "File already exists",
                                                    "No such user",
                                                    "No error message" };
    /**
     * The display name of the stat tracker that stores the ammount of time to
     * service a file download request.
     */
    public static final String STAT_TRACKER_TFTP_DOWNLOAD_TIME =
                               "TFTP Client Download Time";

    /**
     * The display name of the stat tracker that stores the ammount of file
     * download requests.
     */
    public static final String STAT_TRACKER_TFTP_FILE_DOWNLOADS =
                               "TFTP Client File Downloads";

    /**
     * The display name of the stat tracker that stores the number of TFTP
     * packet retransmittions.  This happens when UDP packets get lost or the
     * server is not responding.
     */
    public static final String STAT_TRACKER_TFTP_RETRIES =
                               "TFTP Packet Retransmittions";

    /**
     * The display name of the stat tracker that stores the ammount of bytes
     * downloaded by the TFTP client.
     */
    public static final String STAT_TRACKER_TFTP_DOWNLOADED_BYTES =
                              "TFTP Data Bytes Downloaded";

    /**
     * The display name of the stat tracker that stores the actual ammount of
     * bytes sent by the TFTP server to service the request.
     */
    public static final String STAT_TRACKER_TFTP_ACTUAL_DOWNLOADED_BYTES =
                               "TFTP Actual Data Bytes Downloaded";

      /**
     * The display name of the stat tracker that stores the total number of
     * failed downloads.
     */
    public static final String STAT_TRACKER_TFTP_FAILED_DOWNLOADS =
                               "TFTP Client Failed Downloads";

    // Private instance variables.
    // Byte value that defines a no error situation.
    private static final byte ERROR_NO_ERROR = 10;

    // UDP socket and packet objects.
    private DatagramSocket socket;
    private DatagramPacket sendPacket, receivePacket;

    // Byte array stream objects.  Used to store all data sent/received.
    private ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
    private ByteArrayOutputStream tftpSendPacket = new ByteArrayOutputStream();
    private ByteArrayInputStream tftpReceivedPacket;

    // Object instance status variables.
    // Byte arrays
    private byte[] opCode = new byte[2];
    private byte[] errorCode = new byte[2];
    private byte[] blockNumber = new byte[2];
    private byte[] data;

    // Integers that store communication ports, the length of the last piece of
    // data received, and the total data received
    private int localPort;
    private int remotePort;
    private int dataLength;
    private int totalDataLength;
    private int totalActualDataLength;

    // Integers that store UDP socket timeout and number of retries
    // before client sends error packet.
    private int timeout;
    private int retries;

    // Strings storing fileName to retrieve, data transfer mode, error messages,
    // and server IP address.
    private String fileName;
    private String mode;
    private String errorMessage;
    private String serverIP;

    // Flags that store the status of statistics collection
    private boolean collectStatistics;
    private boolean collectingNow;

    // Statistics Trackers that are maintained by this TFTP client
    private TimeTracker tftpDownloadTime;
    private IncrementalTracker tftpFileDownloads;
    private IncrementalTracker tftpRetries;
    private IncrementalTracker tftpFailedDownloads;
    private IntegerValueTracker tftpDownloadBytes;
    private IntegerValueTracker    tftpActualDownloadedBytes;

    // End of variable declarations.

    /**
     * Class constructor which sets every instance variable to its default value.
     * <P>
     * In particular:
     * <UL>
     * <LI>Server IP is set to <I>127.0.0.1</I>.
     * <LI>File name is set to the file <I>filename</I>.
     * </UL>
     */
    public TFTPClient()
    {
        setVariables(DEFAULT_TFTP_SERVER_IP, DEFAULT_FILENAME, DEFAULT_TFTP_SERVER_PORT, PACKET_RRQ,
                DEFAULT_MODE, DEFAULT_SOCKET_TIMEOUT, DEFAULT_NUMBER_OF_RETRIES);
    }

    /**
     * Class constructur that initialises the TFTP server IP to the
     * <CODE>serverIP</CODE> variable. All other variables are set to default.
     * <P>
     * In particular:
     * <UL>
     * <LI>File name is set to the file <I>filename</I>.
     * </UL>
     *
     * @param serverIP    the TFTP server IP address to connect to when retrieving
     *                                    files.
     */
    public TFTPClient(String serverIP)
    {
        setVariables(serverIP, DEFAULT_FILENAME, DEFAULT_TFTP_SERVER_PORT, PACKET_RRQ,
                DEFAULT_MODE, DEFAULT_SOCKET_TIMEOUT, DEFAULT_NUMBER_OF_RETRIES);
    }

    /**
     * Class constructur that initialises the TFTP server IP to the
     * <CODE>serverIP</CODE> variable and the file name to the string stored in
     * the <CODE>filename</CODE> variable.  All other instance variables are set
     * to their defaults.
     *
     * @param serverIP    the TFTP server IP address to connect to when retrieving
     *                                    files.
     * @param filename    the name of the file to fetch from the TFTP server.
     */
    public TFTPClient(String serverIP, String filename)
    {
        setVariables(serverIP, filename, DEFAULT_TFTP_SERVER_PORT, PACKET_RRQ,
                DEFAULT_MODE, DEFAULT_SOCKET_TIMEOUT, DEFAULT_NUMBER_OF_RETRIES);
    }

    /**
     * Class constructor that initialises the TFTP server IP to the
     * <CODE>serverIP</CODE> variable and the packet time out to the
     * <CODE>timeout</CODE> variable.  All other instance variables are set to
     * their defaults.
     * <P>
     * In particular:
     * <UL>
     * <LI> File name is set to the file <I>filename</I>.
     * </UL>
     *
     * @param serverIP    the TFTP server IP address to connect to when retrieving
     *                                    files.
     * @param timeout        the time out, in milliseconds, before a packet is
     *                                    re-tansmitted.
     */
    public TFTPClient(String serverIP, int timeout)
    {
        setVariables(serverIP, DEFAULT_FILENAME, DEFAULT_TFTP_SERVER_PORT, PACKET_RRQ,
                DEFAULT_MODE, timeout, DEFAULT_NUMBER_OF_RETRIES);
    }

    /**
     * Class constructor that intialises the TFTP server IP to the
     * <CODE>serverIP</CODE> variable, the packet time out to the
     * <CODE>timeout</CODE>, and the maximum number of packet retransmitions to
     * <CODE>retries</CODE>.
     * <P>
     * In particular:
     * <UL>
     * <LI> File name is set to the file <I>filename</I>.
     * </UL>
     *
     * @param serverIP    the TFTP server IP address to connect to when retrieving
     *                                    files.
     * @param timeout        the time out, in milliseconds, before a packet is
     *                                    re-tansmitted.
     * @param retries        the maximum number of packet retransmitions before
     *                                    sending an error TFTP packet.
     */
    public TFTPClient(String serverIP, int timeout, int retries)
    {
        setVariables(serverIP, DEFAULT_FILENAME, DEFAULT_TFTP_SERVER_PORT, PACKET_RRQ,
                DEFAULT_MODE, timeout, retries);
    }

    /*
     * The setVariables method calls other set methods to correctly build an
     * instance.  Used by the constructors to initialise TFTPClient objects
     */
    private void setVariables(String serverIP, String filename, int remotePort,
                    byte opCode, String mode, int timeout, int retries)
    {
        byte[] blockNumber = {0,0};

        setServerIP(serverIP);
        setRemotePort(remotePort);
        setFileName(filename);
        setOpCode(opCode);
        setDataTransferMode(mode);
        setErrorCode(ERROR_NO_ERROR);
        setBlockNumber(blockNumber);
        setTimeout(timeout);
        setNumberOfRetries(retries);
        setDataLength(0);
        setTotalDataLength(0);
        setLocalPort(DEFAULT_UDP_PORT);
        setErrorMessage(ERROR_MESSAGES[8]);
        disableStatisticsCollection();
    }

    // Public set methods.
    /**
     * Assigns the passed filename to the object's <CODE>fileName</CODE> field.
     *
     * @param fileName    the file name to retreive from the TFTP server.
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * Assigns the download mode according to the passed parameter.  This affects
     * the <CODE>mode</CODE> field.  Contains checks to avoid illegal options.
     * If an illegal mode value is passed, the value in <CODE>DEFAULT_MODE</CODE>
     * is used.
     *
     * @param mode    a string containing the download mode.
     */
    public void setDataTransferMode(String mode)
    {
        boolean defaultValue = false;

        // Checks to detect correct values
        if (! mode.equalsIgnoreCase(DOWNLOAD_NETASCII))
            if (! mode.equalsIgnoreCase(DOWNLOAD_OCTET))
                if (! mode.equalsIgnoreCase(DOWNLOAD_MAIL))
                    defaultValue = true;
        // Accept the passed value according to the boolean variable defaultValue
        if (defaultValue)
            this.mode = DEFAULT_MODE;
        else
            this.mode = mode;
    }

    /**
     * Assigns passed server IP to the <CODE>serverIP</CODE> field.
     * No checks are performed.
     *
     * @param serverIP    the server IP address to use.
     */
    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }

    /**
     * Assigns passed timeout, in milliseconds, to the <CODE>timeout</CODE> field.
     * If the value is negative then the number 0 is assigned.  This disables
     * socket timeout checking.
     *
     * @param timeout        an integer storing the timeout in milliseconds.
     */
    public void setTimeout(int timeout)
    {
        if (timeout >= 0)
            this.timeout = timeout;
        else
            this.timeout = 0;
    }

    /**
     * Assigns the passed number of retries to the <CODE>retries</CODE> field.
     * If the value is negative then the number 0 is assigned.  This will force
     * the client to send an error TFTP packet and close if an
     * acknowledgement/data packet is not received.
     *
     * @param retries        the maximum number of retransmissions that the client is
     *                        allowed to perform before sending an error packet.
     */
    public void setNumberOfRetries(int retries)
    {
        if (retries >= 0)
            this.retries = retries;
        else
            this.retries = 0;
    }

    /**
     * Enables the collection of statistical data by setting the
     * <CODE>collectStatistics</CODE> and <CODE>collectingNow</CODE> variables
     * to true.  Initializes all statistics trackers and calls the
     * <CODE>startTracker</CODE> method for every tracker.
     *
     * @param clientID                the Client ID to use for the trackers.
     * @param threadID                the Thread ID to use of the trackers.
     * @param collectionInterval    the collection interval to use for
     *                                 the trackers.
     */
    public void enableStatisticsCollection(String clientID, String threadID,
                                         int collectionInterval)
    {
        // If no statistics have been enabled, enabled them
        collectStatistics = true;
        // Initialise all trackers
        tftpDownloadTime = new TimeTracker(clientID, threadID,
                STAT_TRACKER_TFTP_DOWNLOAD_TIME, collectionInterval);
        tftpDownloadBytes = new IntegerValueTracker(clientID, threadID,
                STAT_TRACKER_TFTP_DOWNLOADED_BYTES, collectionInterval);
        tftpActualDownloadedBytes = new IntegerValueTracker(clientID, threadID,
                STAT_TRACKER_TFTP_ACTUAL_DOWNLOADED_BYTES, collectionInterval);
        tftpFileDownloads = new IncrementalTracker(clientID, threadID,
                STAT_TRACKER_TFTP_FILE_DOWNLOADS, collectionInterval);
        tftpFailedDownloads = new IncrementalTracker(clientID, threadID,
                STAT_TRACKER_TFTP_FAILED_DOWNLOADS, collectionInterval);
        tftpRetries = new IncrementalTracker(clientID, threadID,
                STAT_TRACKER_TFTP_RETRIES, collectionInterval);

        // Start all trackers
        tftpDownloadTime.startTracker();
        tftpDownloadBytes.startTracker();
        tftpActualDownloadedBytes.startTracker();
        tftpFileDownloads.startTracker();
        tftpFailedDownloads.startTracker();
        tftpRetries.startTracker();
        // We are now ready to collect any statistics data
        collectingNow = true;
    }

    /**
     * Sets the variables <CODE>collectStatistics</CODE> and
     * <CODE>collectingNow</CODE> to false.  This disables the collection of
     * statistical data.
     */
    public void disableStatisticsCollection()
    {
        collectStatistics = false;
        collectingNow = false;
    }

    // Private set methods.
    /*
     * Assigns the local port provided by the socket object. If passed value is
     * 0 or negative, the value of DEFAULT_UDP_PORT is used.
     */
    private void setLocalPort(int localPort)
    {
        if (localPort > 0)
            this.localPort = localPort;
        else
            this.localPort = DEFAULT_UDP_PORT;
    }

    /*
     * Assigns the remote port which is contained in the first packet sent by the
     * TFTP server.
     */
    private void setRemotePort(int remotePort)
    {
        if (remotePort > 0)
            this.remotePort = remotePort;
        else
            this.remotePort = DEFAULT_TFTP_SERVER_PORT;
    }

    /*
     * Assigns the number of bytes received to the dataLength field.  0 is used
     * if a negative value is passed.
     */
    private void setDataLength(int dataLength)
    {
        if (dataLength > 0)
            this.dataLength = dataLength;
        else
            this.dataLength = 0;
    }

    /*
     * Assigns the total number of bytes received to the totalDataLength field.
     * If a negative number is passed, 0 is stored.
     */
    private void setTotalDataLength(int totalDataLength)
    {
        if (totalDataLength > 0)
            this.totalDataLength = totalDataLength;
        else
            this.totalDataLength = 0;
    }

    /*
     * Assigns the passed byte value to the opCode byte array's second location.
     * If an illegal value is passed, the PACKET_RRQ value is set.
     */
    private void setOpCode(byte secondOpCodeByte)
    {
        this.opCode[0] = 0;

        switch (secondOpCodeByte)
        {
            case PACKET_RRQ:
            case PACKET_WRQ:
            case PACKET_DATA:
            case PACKET_ACK:
            case PACKET_ERROR:
                this.opCode[1] = secondOpCodeByte;
                break;
            default:
                this.opCode[1] = PACKET_RRQ;
        }
    }

    /*
     * Assigns the passed error code value to the errorCode byte array's second
     * location.  If an illegal value is passed, the ERROR_NO_ERROR value is set.
     */
    private void setErrorCode(byte secondErrorCodeByte)
    {
        this.errorCode[0] = 0;

        switch (secondErrorCodeByte)
        {
            case ERROR_NO_ERROR:
            case ERROR_NOT_DEFINED:
            case ERROR_FILE_NOT_FOUND:
            case ERROR_ACCESS_VIOLATION:
            case ERROR_DISK_FULL:
            case ERROR_ILLEGAL_TFTP_OPERATION:
            case ERROR_UNKNOWN_TRANSFER_ID:
            case ERROR_FILE_ALREADY_EXISTS:
            case ERROR_NO_SUCH_USER:
                this.errorCode[1] = secondErrorCodeByte;
                break;
            default:
                this.errorCode[1] = ERROR_NO_ERROR;
        }
    }

    /*
     * Initialises the blockNumber byte array.
     */
    private void setBlockNumber(byte[] blockNumber)
    {
        this.blockNumber = blockNumber;
    }

    /*
     * Assigns the passed message to the errorMessage String field.
     */
    private void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    /*
     * Assigns the integer to the totalActualDataLength field.
     */
    private void setTotalActualDataLength(int actualLength)
    {
        if (actualLength >= 0)
            this.totalActualDataLength = actualLength;
        else
            this.totalActualDataLength = 0;
    }
    // End of set methods.

    // Public get methods.
    /**
     * Returns the file name stored in the <CODE>fileName</CODE> field.
     *
     * @return    the file name to fetch from the TFTP server.
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * Returns the data download mode stored in the <CODE>mode</CODE> field.
     *
     * @return    the transfer mode to used during file transfers.
     */
    public String getDataTransferMode()
    {
        return mode;
    }

    /**
     * Returns the server IP address stored in the <CODE>serverIP</CODE> field.
     *
     * @return    the TFTP server IP address.
     */
    public String getServerIP()
    {
        return serverIP;
    }

    /**
     * Returns the error message stored in the <CODE>errorMessage</CODE> field.
     *
     * @return    the error message.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }

    /**
     * Returns the local socket port stored in the <CODE>localPort</CODE> field.
     *
     * @return    the local port number.
     */
    public int getLocalPort()
    {
        return localPort;
    }

    /**
     * Returns the remote socket port stored in the <CODE>remotePort</CODE> field.
     *
     * @return    the remote port number.
     */
    public int getRemotePort()
    {
        return remotePort;
    }

    /**
     * Returns the socket time out stored in the <CODE>timeout</CODE> field.
     *
     * @return    the time out.
     */
    public int getTimeout()
    {
        return timeout;
    }

    /**
     * Returns the number of retries stored in the <CODE>retries</CODE> field.
     *
     * @return     the maximum number of retransmissions.
     */
    public int getNumberOfRetries()
    {
        return retries;
    }

    /**
     * Returns the <CODE>errorCode</CODE> byte array.
     *
     * @return    the error code byte array.
     */
    public byte[] getErrorCode()
    {
        return errorCode;
    }

    /**
     * Returns the value stored in the <CODE>totalDataLength</CODE>.
     *
     * @return    the size of the fetched file as an integer.
     */
    public int getFetchedDataLength()
    {
        return totalDataLength;
    }

    /**
     * Returns whether statistics have been enabled.
     *
     * @return    true if statistics have been enabled,
     *             false if not.
     */
    public boolean isStatisticsEnabled()
    {
        return collectStatistics;
    }

    /**
     * Returns whether object is currently collecting statistics.
     *
     * @return    true if object is currently collecting statistics,
     *             false if it is not.
     */
    public boolean areWeCollectingNow()
    {
        return collectingNow;
    }

    /**
     * If statistics are enabled, the trackers are stopped and returned as
     * a <CODE>StatTracker</CODE> array.
     *
     * @return    all statistics trackers in a <CODE>StatTracker</CODE> array
     */
    public StatTracker[] getStatTrackers()
    {
        if (isStatisticsEnabled())
        {
            if (areWeCollectingNow())
            {
                tftpDownloadTime.stopTracker();
                tftpDownloadBytes.stopTracker();
                tftpActualDownloadedBytes.stopTracker();
                tftpFileDownloads.stopTracker();
                tftpFailedDownloads.stopTracker();
                tftpRetries.stopTracker();

                collectingNow = false;
            }
            return new StatTracker[]
            {
                tftpDownloadTime,
                tftpActualDownloadedBytes,
                tftpDownloadBytes,
                tftpFileDownloads,
                tftpFailedDownloads,
                tftpRetries
            };
        }
        else
        {
            return new StatTracker[0];
        }
    }
    // End of public get methods

    // Private get methods
    /*
     * Returns the amount of received byte data stored in the dataLength field.
     */
    private int getDataLength()
    {
        return dataLength;
    }

    /*
     * Returns the opCode byte array second location.
     */
    private byte getOpCode()
    {
        return opCode[1];
    }

    /*
     * Returns the blockNumber byte array.
     */
    private byte[] getBlockNumber()
    {
        return blockNumber;
    }

    /*
     * Returns the Actual number of bytes sent by the tftp server.
     */
    private int getActualDownloadedBytes()
    {
        return  totalActualDataLength;
    }
    // End of get methods.

    /*
     * Try to create a new socket, set the time out and return local port number.
     */
    private int initialiseSocket() throws TFTPClientException
    {
        // If there are any problems throw exception
        try
        {
            socket = new DatagramSocket();
            socket.setSoTimeout(getTimeout());
            return socket.getLocalPort();
        }
        catch(SocketException socketException)
        {
            if (socket != null && ! socket.isClosed())
                socket.close();
            throw new TFTPClientException("An error occurred while initialising the "
                                          + "UDP socket. The error is "
                                          + socketException.toString());
        }
    }

    /*
     * Create send and receive datagram packets.  Throw exception if server IP is
     * illegal.
     */
    private void initialisePackets() throws TFTPClientException
    {
        byte[] buffer = new byte[MAX_BUFFER_SIZE];

        try
        {
            sendPacket = new DatagramPacket(buffer, buffer.length,
                                                InetAddress.getByName(getServerIP()), getRemotePort());
            receivePacket = new DatagramPacket(buffer , buffer.length);
        }
        catch(UnknownHostException unknownHostException)
        {
            throw new TFTPClientException("IP address " + getServerIP()
                                          + " is invalid. The error is "
                                          + unknownHostException.toString());
        }
    }

    /*
     * This method builds a new TFTP packet by adding the appropriate fields
     * to the tftpSendPacket byte array.  Returns true if successful,
     * false if not.
     */
    private boolean buildTftpPacket()
    {
        Byte errorCode;

        tftpSendPacket.reset();

        switch (getOpCode())
        {
            case PACKET_RRQ:
            case PACKET_WRQ:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(getFileName().getBytes(), 0,
                                     getFileName().getBytes().length);
                tftpSendPacket.write(PACKET_DATA_TERMINATOR);
                tftpSendPacket.write(getDataTransferMode().getBytes(), 0,
                                     getDataTransferMode().getBytes().length);
                tftpSendPacket.write(PACKET_DATA_TERMINATOR);
                return true;
            case PACKET_ACK:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(blockNumber, 0, blockNumber.length);
                return true;
            case PACKET_DATA:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(blockNumber, 0, blockNumber.length);
                tftpSendPacket.write(data, 0, data.length);
                return true;
            case PACKET_ERROR:
                tftpSendPacket.write(opCode, 0, opCode.length);
                tftpSendPacket.write(getErrorCode(), 0, getErrorCode().length);
                errorCode = new Byte(getErrorCode()[1]);
                tftpSendPacket.write(ERROR_MESSAGES[errorCode.intValue()].getBytes(),
                          0, ERROR_MESSAGES[errorCode.intValue()].getBytes().length);
                tftpSendPacket.write(PACKET_DATA_TERMINATOR);
                return true;
            default:
                return false;
        }
    }

    /*
     * The method parses a tftp packet and disassembles it into its components
     */
    private int parseTftpPacket()
    {
        String errorMessage;
        byte[] blockNumber = new byte[2];
        byte[] oldBlockNumber;

        tftpReceivedPacket = new ByteArrayInputStream(receivePacket.getData(),
                                                      0, receivePacket.getLength());
        tftpReceivedPacket.read(opCode, 0, 2);

        switch (opCode[1])
        {
            case PACKET_ACK:
                tftpReceivedPacket.read(blockNumber, 0, 2);
                setBlockNumber(blockNumber);
                return 4;
            case PACKET_DATA:
                oldBlockNumber = getBlockNumber();
                tftpReceivedPacket.read(blockNumber, 0, 2);
                setBlockNumber(blockNumber);
                // set current packet length
                setDataLength(tftpReceivedPacket.available());
                // If block numbers are different then it is a new data packet
                if (! Arrays.equals(blockNumber, oldBlockNumber))
                {
                    // At this time these steps are unnecessary but may be useful in
                    // the future
                    data = new byte[getDataLength()];
                    tftpReceivedPacket.read(data, 0, data.length);
                    // Add new data chunk length to the old value
                    setTotalDataLength(getFetchedDataLength() + getDataLength());
                    // Add new data chunk length to the total overall download length
                    setTotalActualDataLength(getActualDownloadedBytes() + getDataLength());
                }
                else
                {
                    // If it is a retransmit just add its length to the overall download length
                    setTotalActualDataLength(getActualDownloadedBytes() + getDataLength());
                }
                return 3;
            case PACKET_ERROR:
                tftpReceivedPacket.read(errorCode, 0, 2);
                setDataLength(tftpReceivedPacket.available() - 1);
                data = new byte[getDataLength()];
                tftpReceivedPacket.read(data, 0, data.length);
                errorMessage = new String(data);
                setErrorMessage(errorMessage);
                return 5;
            case PACKET_RRQ:
                return 1;
            case PACKET_WRQ:
                return 2;
            default:
                return 6;
        }
    }

    /*
     * This method tries to send the TFTP packet over UDP.  If it cannot
     * an exception is thrown.
     */
    private void sendTftpPacket() throws TFTPClientException
    {
        byte[] data = tftpSendPacket.toByteArray();

        try
        {
            sendPacket.setData(data, 0, data.length);
            if (sendPacket.getPort() != getRemotePort())
                sendPacket.setPort(getRemotePort());
            socket.send(sendPacket);
        }
        catch(IOException exception)
        {
            if (socket != null && ! socket.isClosed())
                socket.close();
            throw new TFTPClientException("Could not send packet.  Error is "
                                          + exception.toString());
        }
    }

    /*
     * This method tries to read a TFTP packet from the network.  If a socket
     * time out occurs this method returns 1 without reading any packet
     */
    private int readTftpPacket() throws TFTPClientException
    {
        int returnValue = 0;

        try
        {
            socket.receive(receivePacket);
            if (getRemotePort() != receivePacket.getPort())
            setRemotePort(receivePacket.getPort());
        }
        catch(SocketTimeoutException socketTimeoutException)
        {
            // If there is a socket time out then return 1
            returnValue = 1;
        }
        catch(IOException exception)
        {
            // If problems raise an exception
            if (socket != null && ! socket.isClosed())
                socket.close();
            throw new TFTPClientException("Could not read packet.  Error is "
                                          + exception.toString());
        }
        return returnValue;
    }

    /**
     * Downloads the file specified by the <CODE>filename</CODE> field from the
     * TFTP server whose address is obtained from the <CODE>serverIP</CODE>
     * field.  Method returns an integer to flag success or not.  Method
     * maintaines statistics when the latter are enabled.
     *
     * @return     0 if everything was successfull, 1 if an error packet has been
     *            received, and 2 if the maximum number of retransmitts has been
     *            reached and an error packet has been sent.
     *
     * @throws    TFTPClientException        if there are errors during IO.
     */
    public int getFile() throws TFTPClientException
    {
        int returnCode;

        if (areWeCollectingNow())
        {
            tftpDownloadTime.startTimer();
        }

        returnCode = getFileInternal();

        if (areWeCollectingNow())
        {
            tftpDownloadTime.stopTimer();
            if (returnCode == 0)
            {
                tftpFileDownloads.increment();
                tftpDownloadBytes.addValue(getFetchedDataLength());
                tftpActualDownloadedBytes.addValue(getActualDownloadedBytes());
            }
            else
            {
                tftpFailedDownloads.increment();
            }
        }

        return returnCode;
    }

    /*
     * This method fetches a file from a TFTP server.
     */
    private int getFileInternal() throws TFTPClientException
    {
        boolean finished = false;
        int returnCode = 0;
        int retriesCounter = 1;
        int readTftpPacketValue = 0;

        // Initialise socket and packets
        setLocalPort(initialiseSocket());
        initialisePackets();

        System.out.println("Trying to fetch file " + getFileName() + " from server "
                           + getServerIP());
        // Clear any previous received data
        receivedData.reset();
        // Set op code to read request and set remote port to 69
        setOpCode(PACKET_RRQ);
        setRemotePort(DEFAULT_TFTP_SERVER_PORT);
        setErrorMessage(ERROR_MESSAGES[8]);
        // Build TFTP Read request packet
       if (buildTftpPacket())
         {
             // If packet has been built successfully, try to send it
             sendTftpPacket();
             // Loop until we are finished
                while (! finished)
                {
                    // Try to read packet from the network
                    readTftpPacketValue = readTftpPacket();
                    switch (readTftpPacketValue)
                    {
                        case 0:
                            // If successful, parse packet
                            retriesCounter = 1;
                            switch (parseTftpPacket())
                            {
                                case 3:
                                     // Data packet
                                     // Check that data part is 512 bytes long
                                     if (getDataLength() == 512)
                                     {
                                         // Build an acknowledgement packet and send it back
                                         setOpCode(PACKET_ACK);
                                         if (buildTftpPacket())
                                              // Send packet
                                             sendTftpPacket();
                                     }
                                     else
                                     {
                                         // If data part is less then 512 bytes long then this is
                                         // the last data packet
                                         setOpCode(PACKET_ACK);
                                         // Build the last acknowledgement packet
                                         if (buildTftpPacket())
                                         {
                                             // Send packet
                                             sendTftpPacket();
                                             finished = true;
                                         }
                                     }
                                     break;
                                case 5:
                                    // Error packet has been received
                                    finished = true;
                                    returnCode = 1;
                                    break;
                            }
                            break;
                        case 1:
                            // A socket timeout has occurred.  Need to resend last packet
                            // First check that the client has not exceeded the number of
                            // retries
                            if (retriesCounter < getNumberOfRetries())
                            {
                                // Send packet
                                sendTftpPacket();
                                retriesCounter++;
                                // If statistics are enabled increment the retries counter
                                if (collectStatistics)
                                    tftpRetries.increment();
                            }
                            else
                            {
                                // If number of retries has been exceeded send an error packet
                                setOpCode(PACKET_ERROR);
                                setErrorCode(ERROR_NOT_DEFINED);
                                if (buildTftpPacket())
                                {
                                    // Send packet
                                    sendTftpPacket();
                                    // After sending the error packet terminate
                                    finished = true;
                                    returnCode = 2;
                                }
                            }
                            break;
                     }
                 }
             }
        try
        {
            // Close socket.  Ignore any thrown exceptions
            socket.close();
        }
        catch (Exception e)
        {}
        return returnCode;
    }
}

