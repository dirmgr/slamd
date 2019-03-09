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
 * This class defines an LDAP compare operation parsed from an access log and
 * implements the necessary logic to parse the log file line and to replay the
 * operation against a Directory Server.
 *
 *
 * @author   Neil A. Wilson
 */
public class CompareOperation
       extends LogOperation
{
  /**
   * The result code that will be used for a successful comparison in which the
   * assertion value does not match the contents of the entry.
   */
  public static final String RESULT_CODE_COMPARE_FALSE =
       ResultCode.COMPARE_FALSE_INT_VALUE + " (Compare False)";



  /**
   * The result code that will be used for a successful comparison in which the
   * assertion value does not match the contents of the entry.
   */
  public static final String RESULT_CODE_COMPARE_TRUE =
       ResultCode.COMPARE_TRUE_INT_VALUE + " (Compare True)";



  // The name of the attribute to target.
  private final String attribute;

  // The DN of the entry to target.
  private final String dn;



  /**
   * Creates a new compare operation with the provided information.
   *
   * @param  dn         The DN of the entry to target.
   * @param  attribute  The name of the attribute to target.
   */
  public CompareOperation(String dn, String attribute)
  {
    this.dn        = dn;
    this.attribute = attribute;
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
      int cmpPos = line.indexOf("CMP dn=\"");
      if (cmpPos < 0)
      {
        return null;
      }

      int cmpClosePos = line.indexOf("\" attr=\"", cmpPos+8);
      String dn = line.substring(cmpPos+8, cmpClosePos);

      int attrClosePos = line.indexOf('"', cmpClosePos+8);
      String attribute = line.substring(cmpClosePos+8, attrClosePos);

      return new CompareOperation(dn, attribute);
    }
    catch (Exception e)
    {
      jobThread.writeVerbose("Unable to parse compare line \"" + line +
                             "\":  " + e);
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
    String resultCode     = DEFAULT_RESULT_CODE;
    String assertionValue = jobThread.getRandomString(8);

    try
    {
      jobThread.totalTimer.startTimer();
      jobThread.compareTimer.startTimer();
      if (jobThread.opConnection.compare(dn, attribute,
               assertionValue).compareMatched())
      {
        resultCode = RESULT_CODE_COMPARE_TRUE;
      }
      else
      {
        resultCode = RESULT_CODE_COMPARE_FALSE;
      }
    }
    catch (LDAPException le)
    {
      ResultCode rc = le.getResultCode();

      resultCode = rc.intValue() + " (" + rc.getName() + ')';
    }
    catch (Exception e)
    {
      resultCode = ResultCode.OTHER_INT_VALUE + " (" + e + ')';
    }
    finally
    {
      jobThread.totalTimer.stopTimer();
      jobThread.compareTimer.stopTimer();
      jobThread.totalReplayed.increment();
      jobThread.comparesReplayed.increment();
      jobThread.opRatios.increment("Compare");
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
    return "CMP dn=\"" + dn + "\" attr=\"" + attribute + '"';
  }
}

