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



import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;



/**
 * This class defines an LDAP delete operation parsed from an access log and
 * implements the necessary logic to parse the log file line and to replay the
 * operation against a Directory Server.
 *
 *
 * @author   Neil A. Wilson
 */
public class DeleteOperation
       extends LogOperation
{
  // The DN of the entry to delete.
  private final String dn;



  /**
   * Creates a new delete operation with the provided information.
   *
   * @param  dn  The DN of the entry to delete.
   */
  public DeleteOperation(String dn)
  {
    this.dn = dn;
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
      int deletePos = line.indexOf("DEL dn=\"");
      if (deletePos < 0)
      {
        return null;
      }

      int closePos = line.indexOf('"', deletePos+8);
      String dn = line.substring(deletePos+8, closePos);
      return new DeleteOperation(dn);
    }
    catch (Exception e)
    {
      jobThread.writeVerbose("Unable to parse delete line \"" + line + "\":  " +
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
    boolean tryAdd     = false;
    String  resultCode = DEFAULT_RESULT_CODE;

    try
    {
      jobThread.totalTimer.startTimer();
      jobThread.deleteTimer.startTimer();
      jobThread.opConnection.delete(dn);
    }
    catch (LDAPException le)
    {
      ResultCode rc = le.getResultCode();
      if (LogPlaybackJobClass.addMissingDeletes &&
          (rc == ResultCode.NO_SUCH_OBJECT))
      {
        tryAdd = true;
      }
      else
      {
        resultCode = rc.intValue() + " (" + rc.getName() + ')';
      }
    }
    catch (Exception e)
    {
      resultCode = ResultCode.OTHER_INT_VALUE + " (" + e + ')';
    }
    finally
    {
      jobThread.totalTimer.stopTimer();
      jobThread.deleteTimer.stopTimer();
      jobThread.totalReplayed.increment();
      jobThread.deletesReplayed.increment();
      jobThread.opRatios.increment("Delete");
      jobThread.resultCodes.increment(resultCode);
    }


    if (tryAdd)
    {
      try
      {
        resultCode = DEFAULT_RESULT_CODE;

        String firstString = jobThread.getRandomString(8);
        String lastString  = jobThread.getRandomString(8);

        Entry entry = new Entry(dn,
             new Attribute("objectClass", "top", "person", "organizationalPerson",
                  "inetOrgPerson", "extensibleObject"),
             new Attribute("givenName", firstString),
             new Attribute("sn", lastString),
             new Attribute("cn", firstString + ' ' + lastString));

        jobThread.totalTimer.startTimer();
        jobThread.addTimer.startTimer();
        jobThread.opConnection.add(entry);
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
        jobThread.addTimer.stopTimer();
        jobThread.totalReplayed.increment();
        jobThread.addsReplayed.increment();
        jobThread.opRatios.increment("Add");
        jobThread.resultCodes.increment(resultCode);
      }
    }
  }



  /**
   * Retrieves a string representation of this log operation.
   *
   * @return  A string representation of this log operation.
   */
  public String toString()
  {
    return "DEL dn=\"" + dn + '"';
  }
}

