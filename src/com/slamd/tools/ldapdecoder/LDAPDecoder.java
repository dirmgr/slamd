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
package com.slamd.tools.ldapdecoder;



import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.net.ssl.SSLServerSocketFactory;

import com.slamd.asn1.ASN1DecodeResult;
import com.slamd.asn1.ASN1Element;
import com.slamd.tools.ldapdecoder.protocol.LDAPMessage;
import com.slamd.tools.ldapdecoder.snoop.EthernetHeader;
import com.slamd.tools.ldapdecoder.snoop.IPv4Header;
import com.slamd.tools.ldapdecoder.snoop.SnoopDecoder;
import com.slamd.tools.ldapdecoder.snoop.SnoopException;
import com.slamd.tools.ldapdecoder.snoop.SnoopPacketRecord;
import com.slamd.tools.ldapdecoder.snoop.TCPDumpDecoder;
import com.slamd.tools.ldapdecoder.snoop.TCPDumpPacketRecord;
import com.slamd.tools.ldapdecoder.snoop.TCPHeader;



/**
 * This program defines a utility that can be used to decode LDAP traffic and
 * display it in human-readable form.  It acts as a very simple proxy server,
 * in which LDAP clients communicate with this program which decodes the request
 * and displays it to the end user, as well as forwarding the request on to the
 * directory server.  It also accepts and decodes the response from the server
 * as well as forwarding it back to the client.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPDecoder
       extends Thread
{
  // Indicates whether the raw bytes of the LDAP communication should be
  // displayed in addition to the decoded human-readable output.
  protected boolean displayRawBytes = false;

  // Indicates whether to exclude responses from the server in the generated
  // script.
  private boolean excludeResponses = false;

  // Indicates whether communication from multiple clients should be written to
  // separate files.
  private boolean separateFilePerConnection = false;

  // Indicates whether a specific server address was specified on the command
  // line (necessary for working in offline mode).
  private boolean serverAddressSpecified = false;

  // Indicates whether to use SSL when communicating with LDAP clients.
  private boolean useSSLForClients = false;

  // Indicates whether to use SSL when communicating with the directory server.
  protected boolean useSSLForServer = false;

  // Indicates whether to operate in verbose mode.
  protected boolean verboseMode = false;

  // Indicates whether to write the output as a SLAMD script.
  protected boolean writeJobScript = false;

  // The map of partial data read for connections.
  private HashMap<ConnectionIdentifier,byte[]> partialDataMap =
       new HashMap<ConnectionIdentifier,byte[]>();

  // The port on which to listen for LDAP requests from clients.
  private int listenPort = -1;

  // The port on which the directory server is listening for LDAP requests.
  protected int serverPort = 389;

  // The common output writer that will be used for logging all communication
  // unless a separate output file should be created per client.
  private PrintStream commonOutputWriter = System.out;

  // The output writer that will be used for writing a SLAMD job script based on
  // the LDAP communication.
  protected PrintStream scriptWriter = null;

  // The input file that contains the network packet capture data to use rather
  // that obtaining it while operating in proxy mode.
  private String inputFile = null;

  // The address on which to listen for LDAP requests from clients.
  private String listenAddress = "0.0.0.0";

  // The file to which the decoded LDAP communication is to be written.
  private String outputFile = null;

  // The file to which the SLAMD job script should be written.
  private String scriptFile = null;

  // The address on which the directory server is listening for LDAP requests.
  protected String serverAddress = "127.0.0.1";



  /**
   * Invokes the constructor using the provided command-line arguments and
   * creates a new instance of this LDAP decoder.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    LDAPDecoder decoder = new LDAPDecoder(args);

    if (decoder.offlineMode())
    {
      decoder.runInOfflineMode();
    }
    else
    {
      decoder.setName("LDAPDecoder Main Thread");
      decoder.start();
    }
  }



  /**
   * Creates a new instance of this LDAP decoder based on the information
   * contained in the provided command-line arguments.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public LDAPDecoder(String[] args)
  {
    // Iterate through the arguments and assign their values to instance
    // variables.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-h"))
      {
        serverAddress = args[++i];
        serverAddressSpecified = true;
      }
      else if (args[i].equals("-p"))
      {
        serverPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-l"))
      {
        listenAddress = args[++i];
      }
      else if (args[i].equals("-L"))
      {
        listenPort = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-i"))
      {
        inputFile = args[++i];
      }
      else if (args[i].equals("-f"))
      {
        outputFile = args[++i];
      }
      else if (args[i].equals("-F"))
      {
        writeJobScript = true;
        scriptFile     = args[++i];
      }
      else if (args[i].equals("-x"))
      {
        excludeResponses = true;
      }
      else if (args[i].equals("-m"))
      {
        separateFilePerConnection = true;
      }
      else if (args[i].equals("-s"))
      {
        useSSLForServer = true;
      }
      else if (args[i].equals("-S"))
      {
        useSSLForClients = true;
      }
      else if (args[i].equals("-b"))
      {
        displayRawBytes = true;
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
      else
      {
        System.err.println("Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that either an input file or a valid listen port was specified.
    if ((inputFile == null) || (inputFile.length() == 0))
    {
      if ((listenPort < 1) || (listenPort > 65535))
      {
        System.err.println("Invalid or unspecified listen port (use -L)");
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that there were not any conflicting arguments.
    if (separateFilePerConnection && (inputFile != null))
    {
      System.err.println("Only a single output file may be specified when " +
                         "parsing an input file rather than running in proxy " +
                         "mode");
      System.exit(1);
    }

    if (separateFilePerConnection &&
        ((outputFile == null) || (outputFile.length() == 0) ||
         outputFile.equals("-")))
    {
      System.err.println("An output file must be specified when the -m " +
                         "option is used");
      displayUsage();
      System.exit(1);
    }


    // If a single output file is to be used, create it.
    if ((outputFile != null) && (outputFile.length() > 0) &&
        (! outputFile.equals("-")) && (! separateFilePerConnection))
    {
      try
      {
        commonOutputWriter = new PrintStream(new FileOutputStream(outputFile,
                                                                  true));
      }
      catch (IOException ioe)
      {
        System.err.println("ERROR:  Unable to open output file \"" +
                           outputFile + "\" for writing");
        ioe.printStackTrace();
        System.exit(1);
      }
    }


    // If a script file is to be written, then create the file and write the
    // header to it.
    if (writeJobScript)
    {
      try
      {
        scriptWriter = new PrintStream(new FileOutputStream(scriptFile, false));
      }
      catch (IOException ioe)
      {
        System.err.println("ERROR:  Unable to open script file \"" +
                           scriptFile + "\" for writing");
        ioe.printStackTrace();
        System.exit(1);
      }


      scriptWriter.println("# This script was dynamically generated by the " +
                           "the SLAMD LDAPDecoder tool.");
      scriptWriter.println("# Generation Date:  " + new Date());
      scriptWriter.println();
      scriptWriter.println();

      scriptWriter.println("# Make the LDAP data types available for use.");
      scriptWriter.println("use com.slamd.scripting.ldap." +
                           "LDAPAttributeVariable;");
      scriptWriter.println("use com.slamd.scripting.ldap." +
                           "LDAPConnectionVariable;");
      scriptWriter.println("use com.slamd.scripting.ldap." +
                           "LDAPEntryVariable;");
      scriptWriter.println("use com.slamd.scripting.ldap." +
                           "LDAPModificationVariable;");
      scriptWriter.println("use com.slamd.scripting.ldap." +
                           "LDAPModificationSetVariable;");
      scriptWriter.println();
      scriptWriter.println();

      scriptWriter.println("# Define the variables that we will use.");
      scriptWriter.println("variable boolean             useSSL;");
      scriptWriter.println("variable int                 resultCode;");
      scriptWriter.println("variable int                 port;");
      scriptWriter.println("variable LDAPConnection      conn;");
      scriptWriter.println("variable LDAPEntry           entry;");
      scriptWriter.println("variable LDAPModification    mod;");
      scriptWriter.println("variable LDAPModificationSet modSet;");
      scriptWriter.println("variable string              bindDN;");
      scriptWriter.println("variable string              bindPW;");
      scriptWriter.println("variable string              host;");
      scriptWriter.println("variable string              message;");
      scriptWriter.println("variable StringArray         searchAttrs;");
      scriptWriter.println();
      scriptWriter.println();

      scriptWriter.println("# Read the values of all the configuration " +
                           "arguments.");
      scriptWriter.println("host   = script.getScriptArgument(\"host\", \"" +
                           serverAddress + "\");");
      scriptWriter.println("port   = script.getScriptIntArgument(\"port\", " +
                           serverPort + ");");
      scriptWriter.println("useSSL = script.getScriptBooleanArgument(" +
                           "\"useSSL\", " +
                           (useSSLForServer ? "true" : "false") + ");");
      scriptWriter.println("bindDN = script.getScriptArgument(\"bindDN\", " +
                           "\"\");");
      scriptWriter.println("bindPW = script.getScriptArgument(\"bindPW\", " +
                           "\"\");");
      scriptWriter.println();
      scriptWriter.println();

      scriptWriter.println("# Indicate that the connection should collect " +
                           "and report statistics.");
      scriptWriter.println("conn.enableAttemptedOperationCounters();");
      scriptWriter.println("conn.enableSuccessfulOperationCounters();");
      scriptWriter.println("conn.enableFailedOperationCounters();");
      scriptWriter.println("conn.enableOperationTimers();");
      scriptWriter.println();
      scriptWriter.println();

      scriptWriter.println("# Establish the connection that will be used for " +
                           "all the work.  If the");
      scriptWriter.println("# connection attempt fails, then exit with an " +
                           "error.");
      scriptWriter.println("resultCode = conn.connect(host, port, bindDN, " +
                           "bindPW, 3, useSSL);");
      scriptWriter.println("if resultCode.notEqual(conn.success())");
      scriptWriter.println("begin");
      scriptWriter.println("  message = \"Unable to connect.  Result code " +
                           "was:  \";");
      scriptWriter.println("  message = " +
                           "message.append(resultCode.toString());");
      scriptWriter.println("  script.logMessage(message);");
      scriptWriter.println("  script.exitWithError();");
      scriptWriter.println("end;");
      scriptWriter.println();
      scriptWriter.println();
      scriptWriter.flush();

      Runtime runtime = Runtime.getRuntime();
      runtime.addShutdownHook(new LDAPDecoderShutdownHook(this));
    }
  }



  /**
   * Operates in a loop, accepting new client connections and handing them off
   * to worker threads to actually handle decoding the communication between
   * them.
   */
  public void run()
  {
    // Create the server socket to accept connections from clients.
    ServerSocket serverSocket;
    if (useSSLForClients)
    {
      try
      {
        InetAddress listenInetAddress = InetAddress.getByName(listenAddress);
        SSLServerSocketFactory socketFactory =
             (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = socketFactory.createServerSocket(listenPort, 128,
                                                        listenInetAddress);
      }
      catch (Exception e)
      {
        System.err.println("Unable to create SSL listener to accept client " +
                           "connections:");
        e.printStackTrace();
        return;
      }
    }
    else
    {
      try
      {
        InetAddress listenInetAddress = InetAddress.getByName(listenAddress);
        serverSocket = new ServerSocket(listenPort, 128, listenInetAddress);
      }
      catch (Exception e)
      {
        System.err.println("Unable to create listener to accept client " +
                           "connections:");
        e.printStackTrace();
        return;
      }
    }


    // Print out a message so the user knows the decoder is ready.
    System.err.println("Listening on " + listenAddress + ':' + listenPort +
                       " for client connections");


    // Operate in a loop accepting connections and handing them off to be
    // handled by other threads.
    boolean failedLastTime = false;
    while (true)
    {
      try
      {
        // Accept a new connection from the client.
        Socket clientSocket = serverSocket.accept();

        // Prepare the output writer to use for the connection.
        PrintStream outputWriter;
        if (separateFilePerConnection)
        {
          String fileName = outputFile + '.' +
                            clientSocket.getInetAddress().getHostAddress() +
                            '.' + clientSocket.getPort();
          outputWriter = new PrintStream(new FileOutputStream(fileName, true));
        }
        else
        {
          outputWriter = commonOutputWriter;
        }


        // Create a new client connection object and use it to handle the
        // communication between the client and the server.
        new LDAPClientConnection(this, clientSocket, outputWriter);

        failedLastTime = false;
      }
      catch (Exception e)
      {
        if (failedLastTime)
        {
          System.err.println("Disabling the listener due to consecutive " +
                             "failures while accepting connections");
          return;
        }
        else
        {
          failedLastTime = true;
        }
      }
    }
  }



  /**
   * Indicates whether the LDAPDecoder is configured to operate in offline mode.
   *
   * @return  <CODE>true</CODE> if it is configured to operate in offline mode,
   *          or <CODE>false</CODE> if it is to operate in proxy mode.
   */
  public boolean offlineMode()
  {
    return (inputFile != null);
  }



  /**
   * Runs the LDAP decoder in offline mode, reading the data from the specified
   * input file.
   */
  public void runInOfflineMode()
  {
    // If a server address was specified, then resolve it to an IP address if
    // necessary.
    if (serverAddressSpecified)
    {
      try
      {
        serverAddress = InetAddress.getByName(serverAddress).getHostAddress();
      }
      catch (Exception e)
      {
        System.err.println("ERROR:  Unable to resolve \"" + serverAddress +
                           "\" -- " + e);

        if (verboseMode)
        {
          e.printStackTrace();
        }

        System.exit(1);
      }
    }


    // Open an input stream to the specified file.
    FileInputStream inputStream = null;
    try
    {
      inputStream = new FileInputStream(inputFile);
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Unable to open the input file \"" +
                         inputFile + "\" -- " + ioe);

      if (verboseMode)
      {
        ioe.printStackTrace();
      }

      System.exit(1);
    }


    // Peek at the beginning of the file to determine whether it is a snoop or
    // libpcap dump.
    try
    {
      byte initialByte = (byte) (inputStream.read() & 0x000000FF);

      if (inputStream.markSupported())
      {
        inputStream.reset();
      }
      else
      {
        inputStream.close();
        inputStream = new FileInputStream(inputFile);
      }

      if (initialByte == SnoopDecoder.SNOOP_HEADER_BYTES[0])
      {
        handleSnoopCapture(inputStream);
      }
      else if ((initialByte == (byte) 0xa1) || (initialByte == (byte) 0xd4))
      {
        handleTCPDumpCapture(inputStream);
      }
      else
      {
        System.err.println("Unable to determine the capture file format");
        System.exit(1);
      }
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Unable to read first byte from the capture " +
                         "file -- " + ioe);

      if (verboseMode)
      {
        ioe.printStackTrace();
      }

      System.exit(1);
    }
  }



  /**
   * Handles the work of parsing and interpreting a snoop capture.
   *
   * @param  inputStream   The input stream from which to read the snoop
   *                       capture.
   */
  private void handleSnoopCapture(InputStream inputStream)
  {
    // Create the decoder that we will use to do the real work.
    SnoopDecoder snoopDecoder = null;
    try
    {
      snoopDecoder = new SnoopDecoder(inputStream);

      if (verboseMode)
      {
        System.err.println("Initialized the snoop decoder");
      }
    }
    catch (SnoopException se)
    {
      System.err.println("ERROR:  " + se.getMessage());

      if (verboseMode)
      {
        se.printStackTrace();
      }

      System.exit(1);
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  Unable to initialize the snoop decoder -- " +
                         e);

      if (verboseMode)
      {
        e.printStackTrace();
      }

      System.exit(1);
    }


    // Make sure that the data captured was from an Ethernet datalink type.  If
    // not, then we don't know how to handle it.
    if (snoopDecoder.getDataLinkType() != SnoopDecoder.DATA_LINK_TYPE_ETHERNET)
    {
      System.err.println("ERROR:  Unsupported datalink type for snoop " +
                         "capture");
      System.err.println("Only Ethernet captures are currently supported");
      System.exit(1);
    }


    // Iterate through the snoop packet records and evaluate them to decide
    // whether they contain LDAP traffic.
    boolean errorOccurred   = false;
    int     packetNumber    = 0;
    int     cumulativeDrops = 0;
    int     ldapPackets     = 0;
    int     errorPackets    = 0;
    int     skippedPackets  = 0;
    SimpleDateFormat dateFormat =
         new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss.SSS Z]");
    while (true)
    {
      SnoopPacketRecord packetRecord = null;

      try
      {
        packetRecord = snoopDecoder.nextPacketRecord();
        if (packetRecord == null)
        {
          if (verboseMode)
          {
            System.err.println("Reached the end of the capture file");
          }

          break;
        }

        packetNumber++;
      }
      catch (SnoopException se)
      {
        System.err.println("ERROR:  Unable to decode snoop packet record -- " +
                           se.getMessage());

        if (verboseMode)
        {
          se.printStackTrace();
        }

        errorOccurred = true;
        break;
      }
      catch (IOException ioe)
      {
        System.err.println("ERROR:  Unable to read next packet record from " +
                           "input file -- " + ioe);

        if (verboseMode)
        {
          ioe.printStackTrace();
        }

        errorOccurred = true;
        break;
      }


      // See if any packets may have been dropped and if so print a warning.
      if (packetRecord.getCumulativeDrops() > cumulativeDrops)
      {
        int numDrops    = packetRecord.getCumulativeDrops() - cumulativeDrops;
        cumulativeDrops += numDrops;

        if (verboseMode)
        {
          System.err.println("WARNING:  Detected " + numDrops +
                             " dropped packet(s) between packets " +
                             (packetNumber-1) + " and " + packetNumber);
        }
      }


      // Determine if the packet record was truncated.  If so, then we can't do
      // anything with it.
      if (packetRecord.isTruncated())
      {
        if (verboseMode)
        {
          System.err.println("WARNING:  Skipping truncated packet " +
                             packetNumber);
        }

        continue;
      }


      // Get the timestamp for this packet
      long captureTime = (packetRecord.getTimestampSeconds() * 1000) +
                         (packetRecord.getTimestampMicroseconds() / 1000);
      String packetTimestamp = dateFormat.format(new Date(captureTime));


      // Get the data for the packet and decode the Ethernet header.
      byte[] packetData = packetRecord.getPacketData();
      int    resultCode = decodePacketData(packetData, packetNumber,
                                           packetTimestamp);
      switch (resultCode)
      {
        case -1:
          errorPackets++;
          break;
        case 0:
          skippedPackets++;
          break;
        case 1:
          ldapPackets++;
          break;
      }
    }


    // Close the input and output streams.
    try
    {
      inputStream.close();
    } catch (Exception e) {}

    try
    {
      commonOutputWriter.flush();
      commonOutputWriter.close();
    } catch (Exception e) {}


    // Print a summary of the results.
    System.out.println("Processed " + packetNumber + " total packets");
    System.out.println("Processed " + ldapPackets + " LDAP packets");
    System.out.println("Skipped " + skippedPackets + " non-LDAP packets");
    System.out.println("Encountered " + errorPackets + " decoding errors");


    // Exit gracefully or with an error, depending on whether any fatal decoding
    // errors were encountered.
    if (errorOccurred)
    {
      System.exit(1);
    }
    else
    {
      System.exit(0);
    }
  }



  /**
   * Handles the work of parsing and interpreting a tcpdump capture.
   *
   * @param  inputStream   The input stream from which to read the tcpdump
   *                       capture.
   */
  private void handleTCPDumpCapture(InputStream inputStream)
  {
    // Create the decoder that we will use to do the real work.
    TCPDumpDecoder tcpDumpDecoder = null;
    try
    {
      tcpDumpDecoder = new TCPDumpDecoder(inputStream);

      if (verboseMode)
      {
        System.err.println("Initialized the tcpdump decoder");
      }
    }
    catch (SnoopException se)
    {
      System.err.println("ERROR:  " + se.getMessage());

      if (verboseMode)
      {
        se.printStackTrace();
      }

      System.exit(1);
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  Unable to initialize the tcpdump decoder " +
                         "-- " + e);

      if (verboseMode)
      {
        e.printStackTrace();
      }

      System.exit(1);
    }


    // Make sure that the data captured was from an Ethernet datalink type.  If
    // not, then we don't know how to handle it.
    if (tcpDumpDecoder.getDataLinkType() !=
        TCPDumpDecoder.DATA_LINK_TYPE_ETHERNET)
    {
      System.err.println("ERROR:  Unsupported datalink type for tcpdump " +
                         "capture");
      System.err.println("Only Ethernet captures are currently supported");
      System.exit(1);
    }


    // Iterate through the snoop packet records and evaluate them to decide
    // whether they contain LDAP traffic.
    boolean errorOccurred   = false;
    int     packetNumber    = 0;
    int     ldapPackets     = 0;
    int     errorPackets    = 0;
    int     skippedPackets  = 0;
    SimpleDateFormat dateFormat =
         new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss.SSS Z]");
    while (true)
    {
      TCPDumpPacketRecord packetRecord = null;

      try
      {
        packetRecord = tcpDumpDecoder.nextPacketRecord();
        if (packetRecord == null)
        {
          if (verboseMode)
          {
            System.err.println("Reached the end of the capture file");
          }

          break;
        }

        packetNumber++;
      }
      catch (SnoopException se)
      {
        System.err.println("ERROR:  Unable to decode snoop packet record -- " +
                           se.getMessage());

        if (verboseMode)
        {
          se.printStackTrace();
        }

        errorOccurred = true;
        break;
      }
      catch (IOException ioe)
      {
        System.err.println("ERROR:  Unable to read next packet record from " +
                           "input file -- " + ioe);

        if (verboseMode)
        {
          ioe.printStackTrace();
        }

        errorOccurred = true;
        break;
      }


      // Determine if the packet record was truncated.  If so, then we can't do
      // anything with it.
      if (packetRecord.isTruncated())
      {
        if (verboseMode)
        {
          System.err.println("WARNING:  Skipping truncated packet " +
                             packetNumber);
        }

        continue;
      }


      // Get the timestamp for this packet
      long captureTime = (packetRecord.getTimestampSeconds() * 1000) +
                         (packetRecord.getTimestampMicroseconds() / 1000);
      String packetTimestamp = dateFormat.format(new Date(captureTime));


      // Get the data for the packet and decode the Ethernet header.
      byte[] packetData = packetRecord.getPacketData();
      int    resultCode = decodePacketData(packetData, packetNumber,
                                           packetTimestamp);
      switch (resultCode)
      {
        case -1:
          errorPackets++;
          break;
        case 0:
          skippedPackets++;
          break;
        case 1:
          ldapPackets++;
          break;
      }
    }


    // Close the input and output streams.
    try
    {
      inputStream.close();
    } catch (Exception e) {}

    try
    {
      commonOutputWriter.flush();
      commonOutputWriter.close();
    } catch (Exception e) {}


    // Print a summary of the results.
    System.out.println("Processed " + packetNumber + " total packets");
    System.out.println("Processed " + ldapPackets + " LDAP packets");
    System.out.println("Skipped " + skippedPackets + " non-LDAP packets");
    System.out.println("Encountered " + errorPackets + " decoding errors");


    // Exit gracefully or with an error, depending on whether any fatal decoding
    // errors were encountered.
    if (errorOccurred)
    {
      System.exit(1);
    }
    else
    {
      System.exit(0);
    }
  }



  /**
   * Decodes the actual Ethernet packet as LDAP data.
   *
   * @param  packetData       The actual data contained in this packet.
   * @param  packetNumber     The packet number for this packet.
   * @param  packetTimestamp  The time that the packet was captured.
   *
   * @return  A numeric value that indicates the state of the decoding.  A value
   *          of -1 indicates that an error occurred while processing the data.
   *          A value of 0 indicates that the packet was processed properly but
   *          did not contain LDAP data.  A value of 1 indicates that the packet
   *          was processed properly and did contain LDAP data.
   */
  private int decodePacketData(byte[] packetData, int packetNumber,
                               String packetTimestamp)
  {
    EthernetHeader ethernetHeader = null;
    try
    {
      ethernetHeader = EthernetHeader.decodeEthernetHeader(packetData, 0);
    }
    catch (SnoopException se)
    {
      System.err.println("ERROR:  Unable to decode Ethernet header for " +
                         "packet " + packetNumber + " -- " + se.getMessage());

      if (verboseMode)
      {
        se.printStackTrace();
      }

      return -1;
    }


    // Make sure the Ethernet header contains IP data.
    if (ethernetHeader.getEthertype() != IPv4Header.ETHERTYPE_IPV4)
    {
      if (verboseMode)
      {
        System.err.println("NOTICE:  Skipping packet " + packetNumber +
                           " because it does not contain IPv4 data");
      }

      return 0;
    }


    // Decode the IPv4 header for the packet.
    int ipHeaderStart = ethernetHeader.getHeaderLength();
    IPv4Header ipHeader = null;
    try
    {
      ipHeader = IPv4Header.decodeIPv4Header(packetData, ipHeaderStart);
    }
    catch (SnoopException se)
    {
      System.err.println("ERROR:  Unable to decode IPv4 header for " +
                         "packet " + packetNumber + " -- " + se.getMessage());

      if (verboseMode)
      {
        se.printStackTrace();
      }

      return -1;
    }


    // Make sure the IP header contains TCP data.
    if (ipHeader.getProtocol() != TCPHeader.IP_PROTOCOL_TCP)
    {
      if (verboseMode)
      {
        System.err.println("NOTICE:  Skipping packet " + packetNumber +
                           " because it does not contain TCP data");
      }

      return 0;
    }


    // If a server address was specified, then see if this packet has a source
    // or destination of that address.
    String sourceIP = IPv4Header.intToIPAddress(ipHeader.getSourceIP());
    String destIP   = IPv4Header.intToIPAddress(ipHeader.getDestinationIP());
    if (serverAddressSpecified)
    {
      if (! (serverAddress.equals(sourceIP) || serverAddress.equals(destIP)))
      {
        if (verboseMode)
        {
          System.err.println("NOTICE:  Skipping packet " + packetNumber +
                             " because neither the source nor destination " +
                             "address matched the provided server address");
        }

        return 0;
      }
    }


    // Decode the TCP header for the packet.
    int tcpHeaderStart = ipHeaderStart + ipHeader.getIPHeaderLength();
    TCPHeader tcpHeader = null;
    try
    {
      tcpHeader = TCPHeader.decodeTCPHeader(packetData, tcpHeaderStart);
    }
    catch (SnoopException se)
    {
      System.err.println("ERROR:  Unable to decode TCP header for " +
                         "packet " + packetNumber + " -- " + se.getMessage());

      if (verboseMode)
      {
        se.printStackTrace();
      }

      return -1;
    }


    // See whether the source or destination port from the TCP header matches
    // the server port.
    int sourcePort = tcpHeader.getSourcePort();
    int destPort   = tcpHeader.getDestinationPort();
    if (! ((serverPort == sourcePort) || (serverPort == destPort)))
    {
      if (verboseMode)
      {
        System.err.println("NOTICE:  Skipping packet " + packetNumber +
                           " because neither the source nor destination " +
                           "port matched the provided server port");
      }

      return 0;
    }


    // At this point, we can be reasonably confident that we have a packet
    // going to or from the directory server.  Get the data (if any) and try
    // to decode it as LDAP.
    int dataStart = tcpHeaderStart + tcpHeader.getHeaderLength();
    if (dataStart == packetData.length)
    {
      if (verboseMode)
      {
        System.err.println("NOTICE:  Skipping packet " + packetNumber +
                           " because it does not contain any TCP data");
      }

      return 0;
    }


    // There is what we believe to be LDAP data in this packet, so extract it
    // and try to interpret it.
    byte[] ldapData = new byte[packetData.length - dataStart];
    System.arraycopy(packetData, dataStart, ldapData, 0, ldapData.length);
    if (displayRawBytes)
    {
      commonOutputWriter.println("Raw Data from Client:");
      commonOutputWriter.println(LDAPMessage.byteArrayToString(ldapData, 4));
    }


    // Create a ConnectionIdentifier object and see if we have any partial data
    // from an earlier packet on this connection.
    ConnectionIdentifier connIdentifier =
         new ConnectionIdentifier(sourceIP, sourcePort, destIP, destPort);
    byte[] existingData = partialDataMap.remove(connIdentifier);
    if (existingData != null)
    {
      if (verboseMode)
      {
        System.err.println("NOTE:  Combining data read in packet " +
             packetNumber + " with data read earlier on the same connection.");
      }
      byte[] newLDAPData = new byte[ldapData.length + existingData.length];
      System.arraycopy(existingData, 0, newLDAPData, 0, existingData.length);
      System.arraycopy(ldapData, 0, newLDAPData, existingData.length,
           ldapData.length);
      ldapData = newLDAPData;
    }

    if (ldapData[0] != (byte) 0x30)
    {
      // We will ignore this packet because it doesn't contain any LDAP data.
      if (verboseMode)
      {
        System.err.println("NOTICE:  Skipping packet " + packetNumber +
             " because it does not start with the expected ASN.1 sequence " +
             "BER type.");
      }

      return -1;
    }

    ASN1DecodeResult decodeResult;
    try
    {
      decodeResult = ASN1Element.decodePartial(ldapData);
    }
    catch (Exception e)
    {
      if (verboseMode)
      {
        System.err.println("WARNING:  Skipping packet " + packetNumber +
                           " because TCP data could not be decoded as an " +
                           "ASN.1 element -- " + e);
        e.printStackTrace();
      }

      return -1;
    }

    boolean fullDecoded = false;
    while (true)
    {
      ASN1Element asn1Element = decodeResult.getDecodedElement();
      if (asn1Element == null)
      {
        if (ldapData.length > (20 * 1024 * 1024))
        {
          // For safety purposes, we won't attempt to handle any packets
          // larger than 20MB.
          System.err.println("ERROR:  Packet " + packetNumber +
               ", combined with data from earlier packets, does not contain " +
               "a complete element even after more than 100MB of data has " +
               "been processed.  Aborting the attempt to decode this data.");
          return -1;
        }
        else
        {
          if (verboseMode)
          {
            System.err.println("NOTICE:  Packet " + packetNumber +
                 " contains LDAP data, but does not have enough to form a " +
                 "complete ASN.1 element.  Saving the partial data to add " +
                 "to data from subsequent packets on the connection.");
          }
          partialDataMap.put(connIdentifier, ldapData);
          if (fullDecoded)
          {
            return 1;
          }
          else
          {
            return 0;
          }
        }
      }

      fullDecoded = true;
      commonOutputWriter.println(packetTimestamp + " Data From " + sourceIP +
                                 ':' + sourcePort + " to " + destIP + ':' +
                                 destPort);


      LDAPMessage message = null;
      try
      {
        message = LDAPMessage.decode(asn1Element);
      }
      catch (Exception e)
      {
        if (verboseMode)
        {
          System.err.println("WARNING:  Skipping packet " + packetNumber +
                             " because ASN.1 element could not be decoded as " +
                             "an LDAP message -- " + e);
          e.printStackTrace();
        }

        commonOutputWriter.println("Unable to decode data from client:  " +
                                   e.getMessage());
        commonOutputWriter.println();
        commonOutputWriter.println();
        return -1;
      }


      commonOutputWriter.println("Decoded Data from Client:");
      commonOutputWriter.println(message.toString(4));
      commonOutputWriter.println();
      commonOutputWriter.println();

      if (writeJobScript)
      {
        message.toSLAMDScript(scriptWriter);
      }

      if (verboseMode)
      {
        System.err.println("NOTICE:  Decoded packet " + packetNumber +
             " as an " + message.getProtocolOp().getProtocolOpType() +
             " message");
      }

      byte[] remainingData = decodeResult.getRemainingData();
      if (remainingData == null)
      {
        return 1;
      }

      try
      {
        ldapData = remainingData;
        decodeResult = ASN1Element.decodePartial(ldapData);
      }
      catch (Exception e)
      {
        if (verboseMode)
        {
          System.err.println("WARNING:  Remaining TCP data in packet " +
               packetNumber + " could not be decoded as an ASN.1 element -- " +
               e);
          e.printStackTrace();
        }

        return -1;
      }
    }
  }



  /**
   * Indicates whether comments providing information about server responses
   * should be excluded from the resulting SLAMD job script.
   *
   * @return  {@code true} if information about server responses should be
   *          excluded, or {@code false} if not.
   */
  public boolean excludeResponses()
  {
    return excludeResponses;
  }



  /**
   * Displays usage information for this LDAP decoder.
   */
  public static void displayUsage()
  {
    String eol = System.getProperty("line.separator");

    System.err.println(
"Usage:  java -jar LDAPDecoder.jar {options}" + eol +
"        where {options} include:" + eol +
"-h {serverAddress}  -- Specifies the address of the directory server" + eol +
"                       to which client requests should be forwarded" + eol +
"-p {serverPort}     -- Specifies the port of the directory server to" + eol +
"                       which client requests should be forwarded" + eol +
"-l {listenAddress}  -- Specifies the address on which the LDAP decoder" + eol +
"                       should accept connections from LDAP clients" + eol +
"-L {listenPort}     -- Specifies the port on which the LDAP decoder" + eol +
"                       should accept connections from LDAP clients" + eol +
"-i {inputFile}      -- Specifies the input file containing the capture" + eol +
"                       data to use when operating in offline mode" + eol +
"-f {outputFile}     -- Specifies the path to the output file to which" + eol +
"                       decoded communication will be written in " + eol +
"                       human-readable form" + eol +
"-F {outputFile}     -- Specifies the path to the output file to which" + eol +
"                       decoded communication will be written as a" + eol +
"                       SLAMD job script" + eol +
"-x                  -- Indicates that server responses should be" + eol +
"                    -- excluded from the generated SLAMD job script" + eol +
"-m                  -- Indicates that a separate file output file" + eol +
"                       should be used for each client connection" + eol +
"-s                  -- Indicates that communication with the directory" + eol +
"                       server should be encrypted using SSL" + eol +
"-S                  -- Indicates that communication with clients" + eol +
"                       be encrypted using SSL" + eol +
"-b                  -- Indicates that the raw bytes of the LDAP" + eol +
"                       communication should be included in the output" + eol +
"-v                  -- Enables verbose mode (useful for debugging)" + eol +
"-H                  -- Displays this usage information"
                      );
  }
}

