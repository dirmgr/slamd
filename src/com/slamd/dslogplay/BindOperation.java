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



import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;



/**
 * This class defines an LDAP bind operation parsed from an access log and
 * implements the necessary logic to parse the log file line and to replay the
 * operation against a Directory Server.
 *
 *
 * @author   Neil A. Wilson
 */
public class BindOperation
       extends LogOperation
{
  // The DN of the user performing the bind.
  private final String bindDN;



  /**
   * Creates a new bind operation with the provided information.
   *
   * @param  bindDN  The DN of the user as whom to bind.
   */
  public BindOperation(String bindDN)
  {
    this.bindDN = bindDN;
  }



  /**
   * Parses the provided line as appropriate for this type of operation and
   * constructs a new log operation based on the information it contains.
   *
   * @param  jobThread  The job thread with which this parser is associated.
   * @param  line       The line to be parsed and converted to a log operation.
   *
   * @return  The log operation created from the provided log line, or
   *          <CODE>null</CODE> if a problem prevented the line from being
   *          parsed properly.
   */
  public static LogOperation parseLogLine(LogPlaybackJobClass jobThread,
                                          String line)
  {
    try
    {
      int bindPos = line.indexOf("BIND dn=\"");
      if (bindPos < 0)
      {
        return null;
      }

      int closePos = line.indexOf("\" method=", bindPos+9);
      String bindDN = line.substring(bindPos+9, closePos);
      return new BindOperation(bindDN);
    }
    catch (Exception e)
    {
      jobThread.writeVerbose("Unable to parse bind line \"" + line + "\":  " +
                             e);
      return null;
    }
  }



  /**
   * Replays this operation against the directory server using the information
   * contained in the provided job thread.
   *
   * @param  jobThread  The job thread to use when replaying this operation.
   */
  public void replayOperation(LogPlaybackJobClass jobThread)
  {
    String resultCode = DEFAULT_RESULT_CODE;

    try
    {
      jobThread.totalTimer.startTimer();
      jobThread.bindTimer.startTimer();

      jobThread.bindConnection.bind(bindDN,
           LogPlaybackJobClass.bindOperationPassword);
    }
    catch (LDAPException le)
    {
      ResultCode rc = le.getResultCode();

      resultCode = rc.intValue() + " (" + rc.getName() + ')';
    }
    catch (Exception e)
    {
      resultCode = ResultCode.OTHER + " (" + e + ')';
    }
    finally
    {
      jobThread.totalTimer.stopTimer();
      jobThread.bindTimer.stopTimer();
      jobThread.totalReplayed.increment();
      jobThread.bindsReplayed.increment();
      jobThread.opRatios.increment("Bind");
      jobThread.resultCodes.increment(resultCode);
    }
  }



  /**
   * Retrieves a string representation of this log operation.
   *
   * @return  A string representation of this log operation.
   */
  public String toString()
  {
    return "BIND dn=\"" + bindDN + '"';
  }
}

