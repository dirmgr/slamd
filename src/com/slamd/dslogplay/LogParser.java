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



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



/**
 * This class defines a utility that may be used to parse Directory Server
 * access log files and convert them into operations that may be replayed
 * against the server.
 *
 *
 * @author   Neil A. Wilson
 */
public class LogParser
{
  // The set of operations parsed from the log file.
  private final ArrayList<LogOperation> operations;

  // The types of operations that should be captured.
  private final boolean captureAdds;
  private final boolean captureBinds;
  private final boolean captureCompares;
  private final boolean captureDeletes;
  private final boolean captureModifies;
  private final boolean captureSearches;

  // The job thread with which this parser is associated.
  private final LogPlaybackJobClass jobThread;



  /**
   * Creates a new log parser that will be used to capture the specified types
   * of elements.
   *
   * @param  jobThread        The job thread with which this parser is
   *                          associated.
   * @param  captureAdds      Indicates whether to capture add operations.
   * @param  captureBinds     Indicates whether to capture bind operations.
   * @param  captureCompares  Indicates whether to capture compare operations.
   * @param  captureDeletes   Indicates whether to capture delete operations.
   * @param  captureModifies  Indicates whether to capture modify operations.
   * @param  captureSearches  Indicates whether to capture search operations.
   */
  public LogParser(LogPlaybackJobClass jobThread, boolean captureAdds,
                   boolean captureBinds, boolean captureCompares,
                   boolean captureDeletes, boolean captureModifies,
                   boolean captureSearches)
  {
    this.jobThread       = jobThread;
    this.captureAdds     = captureAdds;
    this.captureBinds    = captureBinds;
    this.captureCompares = captureCompares;
    this.captureDeletes  = captureDeletes;
    this.captureModifies = captureModifies;
    this.captureSearches = captureSearches;

    operations = new ArrayList<LogOperation>();
  }



  /**
   * Parses the specified log file and captures information about the
   * appropriate operations contained in it.
   *
   * @param  logFile  The path to the log file to be parsed.
   *
   * @throws  IOException  If a problem occurs while reading the log file.
   */
  public void parseLogFile(String logFile)
         throws IOException
  {
    BufferedReader reader = new BufferedReader (new FileReader(logFile));
    String line;

    while (true)
    {
      line = reader.readLine();
      if (line == null)
      {
        break;
      }

      LogOperation parsedOperation = null;
      if (captureAdds && (line.indexOf("ADD dn=\"") > 0))
      {
        parsedOperation = AddOperation.parseLogLine(jobThread,  line);
      }
      else if (captureBinds && (line.indexOf("BIND dn=\"") > 0))
      {
        parsedOperation = BindOperation.parseLogLine(jobThread,  line);
      }
      else if (captureCompares && (line.indexOf("CMP dn=\"") > 0))
      {
        parsedOperation = CompareOperation.parseLogLine(jobThread,  line);
      }
      else if (captureDeletes && (line.indexOf("DEL dn=\"") > 0))
      {
        parsedOperation = DeleteOperation.parseLogLine(jobThread,  line);
      }
      else if (captureModifies && (line.indexOf("MOD dn=\"") > 0))
      {
        parsedOperation = ModifyOperation.parseLogLine(jobThread,  line);
      }
      else if (captureSearches && (line.indexOf("SRCH base=\"") > 0))
      {
        parsedOperation = SearchOperation.parseLogLine(jobThread,  line);
      }

      if (parsedOperation != null)
      {
        operations.add(parsedOperation);
      }
    }

    reader.close();
  }



  /**
   * Retrieves an array containing the operations that have been captured from
   * the log file(s).
   *
   * @return  An array containing the operations that have been captured from
   *          the log file(s).
   */
  public LogOperation[] getOperations()
  {
    LogOperation[] opArray = new LogOperation[operations.size()];
    operations.toArray(opArray);
    return opArray;
  }
}

