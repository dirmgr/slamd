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
import java.util.StringTokenizer;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;



/**
 * This class defines an LDAP search operation parsed from an access log and
 * implements the necessary logic to parse the log file line and to replay the
 * operation against a Directory Server.
 *
 *
 * @author   Neil A. Wilson
 */
public class SearchOperation
       extends LogOperation
{
  // The scope to use for the search request.
  private final SearchScope scope;

  // The base DN for the search request.
  private final String baseDN;

  // The filter for the search request.
  private final String filter;

  // The set of attributes to return for the search request.
  private final String[] attributes;



  /**
   * Creates a new search operation with the provided information.
   *
   * @param  baseDN      The base DN to use for the search request.
   * @param  scope       The scope to use for the search request.
   * @param  filter      The filter to use for the search request.
   * @param  attributes  The set of attributes to include in matching entries.
   */
  public SearchOperation(String baseDN, SearchScope scope, String filter,
                         String[] attributes)
  {
    this.baseDN     = baseDN;
    this.scope      = scope;
    this.filter     = filter;
    this.attributes = attributes;
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
      int basePos = line.indexOf("SRCH base=\"");
      if (basePos < 0)
      {
        return null;
      }

      int scopePos = line.indexOf("\" scope=", basePos+11);
      if (scopePos < 0)
      {
        return null;
      }

      String baseDN = line.substring(basePos+11, scopePos);
      char scopeChar = line.charAt(scopePos+8);
      SearchScope scope;
      switch (scopeChar)
      {
        case '0':
          scope = SearchScope.BASE;
          break;
        case '1':
          scope = SearchScope.ONE;
          break;
        case '2':
          scope = SearchScope.SUB;
          break;
        default:
          scope = SearchScope.BASE;
          break;
      }

      int filterPos = line.indexOf(scopeChar + " filter=\"", scopePos+8);
      if (filterPos < 0)
      {
        return null;
      }

      String filter = null;
      String[] attrs = null;
      int attrsPos = line.indexOf("\" attrs=", filterPos+10);
      if (attrsPos < 0)
      {
        int filterClosePos = line.indexOf('"', filterPos+10);
        if (filterClosePos < 0)
        {
          return null;
        }
        else
        {
          filter = line.substring(filterPos+10, filterClosePos);
        }
      }
      else
      {
        filter = line.substring(filterPos+10, attrsPos);

        char attrsChar = line.charAt(attrsPos+8);
        if (attrsChar == '\"')
        {
          int attrsClosePos = line.indexOf('"', attrsPos+9);
          if (attrsClosePos > 0)
          {
            String attrsStr = line.substring(attrsPos+9, attrsClosePos);
            ArrayList<String> attrList = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(attrsStr, " ");
            while (tokenizer.hasMoreTokens())
            {
              attrList.add(tokenizer.nextToken());
            }
            attrs = new String[attrList.size()];
            attrList.toArray(attrs);
          }
        }
      }

      return new SearchOperation(baseDN, scope, filter, attrs);
    }
    catch (Exception e)
    {
      jobThread.writeVerbose("Unable to parse search line \"" + line +
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
  @Override()
  public void replayOperation(LogPlaybackJobClass jobThread)
  {
    String        resultCode     = DEFAULT_RESULT_CODE;

    try
    {
      jobThread.totalTimer.startTimer();
      jobThread.searchTimer.startTimer();

      jobThread.opConnection.search(baseDN, scope, filter, attributes);
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
      jobThread.searchTimer.stopTimer();
      jobThread.totalReplayed.increment();
      jobThread.searchesReplayed.increment();
      jobThread.opRatios.increment("Search");
      jobThread.resultCodes.increment(resultCode);
    }
  }



  /**
   * Retrieves a string representation of this log operation.
   *
   * @return  A string representation of this log operation.
   */
  @Override()
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    buffer.append("SRCH base=\"");
    buffer.append(baseDN);
    buffer.append("\" scope=");
    buffer.append(scope);
    buffer.append(" filter=\"");
    buffer.append(filter);
    buffer.append("\" attrs=");

    if ((attributes == null) || (attributes.length == 0))
    {
      buffer.append("ALL");
    }
    else
    {
      buffer.append('"');
      buffer.append(attributes[0]);

      for (int i=1; i < attributes.length; i++)
      {
        buffer.append(' ');
        buffer.append(attributes[i]);
      }

      buffer.append('"');
    }

    return buffer.toString();
  }
}

