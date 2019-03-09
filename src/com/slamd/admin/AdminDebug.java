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
package com.slamd.admin;



import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.slamd.asn1.ASN1Element;
import com.slamd.common.Constants;
import com.slamd.job.JobClass;

import static com.slamd.admin.AdminServlet.*;
import static com.slamd.admin.AdminUI.*;



/**
 * This class provides a set of methods for providing debug access to the
 * administrative interface.
 */
public class AdminDebug
{
  /**
   * Handles the work of generating debug information about all thread groups
   * and threads defined in the JVM.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugThreads(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugThreads()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Find the root thread group for the JVM.
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup = threadGroup.getParent();
    while (parentGroup != null)
    {
      threadGroup = parentGroup;
      parentGroup = threadGroup.getParent();
    }


    // Next, get all the threads defined in the JVM.
    int      numThreads    = threadGroup.activeCount();
    Thread[] activeThreads = new Thread[numThreads];
    threadGroup.enumerate(activeThreads);


    // Display all this information to the user.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM Thread Debug Information</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The following threads are currently defined in the JVM:" +
                    EOL);

    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD><B>Thread Group</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Thread Name</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Priority</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Is Alive</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Is Interrupted</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Is Daemon</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    for (int i=0; i < numThreads; i++)
    {
      if (activeThreads[i] == null)
      {
        continue;
      }

      ThreadGroup group = activeThreads[i].getThreadGroup();
      String groupName  = (group == null) ? "N/A" : group.getName();


      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>" + groupName + "</TD>" + EOL);
      htmlBody.append("    <TD>" + activeThreads[i].getName() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + activeThreads[i].getPriority() + "</TD>" +
                      EOL);
      htmlBody.append("    <TD>" + activeThreads[i].isAlive() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + activeThreads[i].isInterrupted() + "</TD>" +
                      EOL);
      htmlBody.append("    <TD>" + activeThreads[i].isDaemon() + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work of generating debug information including a full stack
   * trace for all threads in the JVM.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugStackTrace(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugStackTrace()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody    = requestInfo.htmlBody;
    StringBuilder infoMessage = requestInfo.infoMessage;

    Map threadMap = null;
    try
    {
      Class<?> threadClass = Constants.classForName("java.lang.Thread");
      Method stackTracesMethod = threadClass.getMethod("getAllStackTraces");
      threadMap = (Map) stackTracesMethod.invoke(null);

      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">SLAMD Server JVM Stack Trace</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      Iterator iterator = threadMap.entrySet().iterator();
      while (iterator.hasNext())
      {
        Map.Entry entry = (Map.Entry) iterator.next();

        Thread              thread = (Thread) entry.getKey();
        StackTraceElement[] stack  = (StackTraceElement[]) entry.getValue();

        htmlBody.append("Thread \"" + thread.getName() + "\" Stack:" + EOL);
        htmlBody.append("<PRE>" + EOL);

        for (int i=0; i < stack.length; i++)
        {
          htmlBody.append("     " + stack[i].getClassName() + '.' +
                          stack[i].getMethodName() + '(');

          if (stack[i].isNativeMethod())
          {
            htmlBody.append("native method");
          }
          else
          {
            htmlBody.append(stack[i].getFileName() + ':' +
                            stack[i].getLineNumber());
          }

          htmlBody.append(')' + EOL);
        }

        htmlBody.append("</PRE>" + EOL);

        if (iterator.hasNext())
        {
          htmlBody.append("<BR><HR><BR>" + EOL);
        }
      }
    }
    catch (Exception e)
    {
      infoMessage.append("Stack trace information is not available.<BR>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("An error occurred that prevented the thread " +
                      "stack trace information from being retrieved:  " +
                      e + EOL);
      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("Note that this capability is only allowed for Java 5 " +
                      "and higher versions of the JVM." + EOL);
      return;
    }
  }



  /**
   * Handles the work of generating debug information about all system
   * properties defined in the JVM.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugSystemProperties(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugSystemProperties()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Get the set of system properties defined in the JVM.
    Properties  systemProperties = System.getProperties();
    Enumeration propertyNames    = systemProperties.propertyNames();


    // Display this information to the user.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM System Properties Debug Information</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The following system properties are currently defined " +
                    "in the JVM:" + EOL);

    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD><B>Property Name</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Property Value</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    while (propertyNames.hasMoreElements())
    {
      String name  = (String) propertyNames.nextElement();
      String value = System.getProperty(name, "&nbsp;");

      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>" + name + "</TD>" + EOL);
      htmlBody.append("    <TD>" + value + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work of generating debug information about the request
   * information provided by the client.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugRequest(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugRequest()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request  = requestInfo.request;
    StringBuilder       htmlBody = requestInfo.htmlBody;


    // Display information about the request to the user.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Servlet Request Information</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("The following information is available about the " +
                    "request sent to SLAMD:" + EOL);


    // General request information.
    htmlBody.append("<BR><BR>");
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">General Request Information</SPAN>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Request URL</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getRequestURL() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Request Method</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getMethod() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Request Protocol</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getProtocol() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Authentication Type</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getAuthType() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Remote User</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getRemoteUser() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Remote Host</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getRemoteHost() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Server Host</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getServerName() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Server Port</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getServerPort() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Content Type</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getContentType() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Content Length</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getContentLength() + "</TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD>Character Encoding</TD>" + EOL);
    htmlBody.append("    <TD>" + request.getCharacterEncoding() + "</TD>" +
                    EOL);
    htmlBody.append("  </TR>" + EOL);
    htmlBody.append("</TABLE>" + EOL);


    // Request header information.
    htmlBody.append("<BR><BR>");
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Request Headers</SPAN>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD><B>Header Name</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Header Value</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements())
    {
      String      name   = (String) headerNames.nextElement();
      Enumeration values = request.getHeaders(name);

      if ((values != null) && (values.hasMoreElements()))
      {
        while (values.hasMoreElements())
        {
          String value = (String) values.nextElement();
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>" + name + "</TD>" + EOL);
          htmlBody.append("    <TD>" + value + "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
      }
      else
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + name + "</TD>" + EOL);
        htmlBody.append("    <TD>N/A</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }
    htmlBody.append("</TABLE>" + EOL);


    // Request parameter information.
    htmlBody.append("<BR><BR>");
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Request Parameters</SPAN>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD><B>Parameter Name</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Parameter Value</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    Enumeration parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements())
    {
      String   name   = (String) parameterNames.nextElement();
      String[] values = request.getParameterValues(name);

      if ((values == null) || (values.length == 0))
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + name + "</TD>" + EOL);
        htmlBody.append("    <TD>N/A</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
      else
      {
        for (int i=0; i < values.length; i++)
        {
          htmlBody.append("  <TR>" + EOL);
          htmlBody.append("    <TD>" + name + "</TD>" + EOL);
          htmlBody.append("    <TD>" + values[i] + "</TD>" + EOL);
          htmlBody.append("  </TR>" + EOL);
        }
      }
    }

    htmlBody.append("</TABLE>" + EOL);


    // Request attribute information.
    htmlBody.append("<BR><BR>");
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Request Attributes</SPAN>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD><B>Attribute Name</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Attribute Type</B></TD>" + EOL);
    htmlBody.append("    <TD><B>String Representation</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    Enumeration attributeNames = request.getAttributeNames();
    while (attributeNames.hasMoreElements())
    {
      String name  = (String) attributeNames.nextElement();
      Object value = request.getAttribute(name);

      if (value == null)
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + name + "</TD>" + EOL);
        htmlBody.append("    <TD>N/A</TD>" + EOL);
        htmlBody.append("    <TD>N/A</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
      else
      {
        htmlBody.append("  <TR>" + EOL);
        htmlBody.append("    <TD>" + name + "</TD>" + EOL);
        htmlBody.append("    <TD>" + value.getClass().getName() + "</TD>" +
                        EOL);
        htmlBody.append("    <TD>" + value.toString() + "</TD>" + EOL);
        htmlBody.append("  </TR>" + EOL);
      }
    }

    htmlBody.append("</TABLE>" + EOL);


    // Request cookie information.
    htmlBody.append("<BR><BR>");
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">Request Cookies</SPAN>" + EOL);
    htmlBody.append("<TABLE BORDER=\"1\">" + EOL);
    htmlBody.append("  <TR>" + EOL);
    htmlBody.append("    <TD><B>Cookie Name</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Cookie Domain</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Cookie Path</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Cookie Max Age</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Cookie Version</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Is Secure</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Cookie Value</B></TD>" + EOL);
    htmlBody.append("    <TD><B>Cookie Comment</B></TD>" + EOL);
    htmlBody.append("  </TR>" + EOL);

    Cookie[] cookies = request.getCookies();
    for (int i=0; ((cookies != null) && (i < cookies.length)); i++)
    {
      htmlBody.append("  <TR>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getName() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getDomain() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getPath() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getVersion() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getMaxAge() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getSecure() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getValue() + "</TD>" + EOL);
      htmlBody.append("    <TD>" + cookies[i].getComment() + "</TD>" + EOL);
      htmlBody.append("  </TR>" + EOL);
    }

    htmlBody.append("</TABLE>" + EOL);
  }



  /**
   * Handles the work of displaying debug information from the SLAMD
   * configuration database.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugDatabase(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugDatabase()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    HttpServletRequest request        = requestInfo.request;
    String             servletBaseURI = requestInfo.servletBaseURI;
    StringBuilder       htmlBody       = requestInfo.htmlBody;
    StringBuilder       infoMessage    = requestInfo.infoMessage;


    // First, print out a really hideous warning message to the user.
    infoMessage.append("<B>WARNING:  This information is provided for " +
                       "debugging purposes only, and any changes made " +
                       "through this interface can cause serious damage to " +
                       "the database.  Use it at your own risk.</B>" + EOL);


    // See if the user has specified a database name.  If not, then present a
    // form to request one.
    String dbName = request.getParameter(Constants.SERVLET_PARAM_DB_NAME);
    if ((dbName == null) || (dbName.length() == 0))
    {
      try
      {
        String[] dbNames = configDB.getDBNames();
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">SLAMD Database Debug Information</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_DEBUG) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_DEBUG_DB) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);

        htmlBody.append("  Choose the database you wish to access:" + EOL);
        htmlBody.append("  <SELECT NAME=\"" + Constants.SERVLET_PARAM_DB_NAME +
                        "\">" + EOL);

        for (int i=0; i < dbNames.length; i++)
        {
          htmlBody.append("    <OPTION VALUE=\"" + dbNames[i] + "\">" +
                          dbNames[i] + EOL);
        }

        htmlBody.append("  </SELECT>" + EOL);
        htmlBody.append("  <BR><BR>" + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Next >\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
      catch (Exception e)
      {
        htmlBody.append("<PRE>" + EOL);
        htmlBody.append(JobClass.stackTraceToString(e) + EOL);
        htmlBody.append("</PRE>" + EOL);
      }

      return;
    }


    // The user has specified a database name, so see if they have given a key
    // name.  If not, then present a list of the keys in that database.
    String dbKey = request.getParameter(Constants.SERVLET_PARAM_DB_KEY);
    if ((dbKey == null) || (dbKey.length() == 0))
    {
      try
      {
        htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                        "\">SLAMD Debug Information for Database " + dbName +
                        "</SPAN>" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        String[] keyNames = configDB.getDBKeys(dbName);

        htmlBody.append("The following keys have been defined in the " +
                        dbName + " database:" + EOL);
        htmlBody.append("<BR><BR>" + EOL);

        for (int i=0; i < keyNames.length; i++)
        {
          htmlBody.append(generateLink(requestInfo,
                                       Constants.SERVLET_SECTION_DEBUG,
                                       Constants.SERVLET_SECTION_DEBUG_DB,
                                       Constants.SERVLET_PARAM_DB_NAME, dbName,
                                       Constants.SERVLET_PARAM_DB_KEY,
                                       keyNames[i], keyNames[i]));
          htmlBody.append("<BR>" + EOL);
        }

        htmlBody.append("<BR>" + EOL);
        htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                        "\">" + EOL);
        htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                              Constants.SERVLET_SECTION_DEBUG) +
                        EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                             Constants.SERVLET_SECTION_DEBUG_DB) + EOL);
        htmlBody.append("  " +
                        generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                       "1") + EOL);
        htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"< Back\">" + EOL);
        htmlBody.append("</FORM>" + EOL);
      }
      catch (Exception e)
      {
        htmlBody.append("<PRE>" + EOL);
        htmlBody.append(JobClass.stackTraceToString(e) + EOL);
        htmlBody.append("</PRE>" + EOL);
      }

      return;
    }


    // The user has specified a specific key.  Display the data associated with
    // that key in both hex and ASCII forms.
    try
    {
      htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                      "\">SLAMD Debug Information for Database " + dbName +
                      " Key " + dbKey + "</SPAN>" + EOL);
      htmlBody.append("<BR><BR>" + EOL);

      byte[] data = configDB.getDBData(dbName, dbKey);
      String hexData = ASN1Element.byteArrayToStringWithASCII(data);
      hexData = replaceText(hexData, "<", "&lt;");
      hexData = replaceText(hexData, ">", "&gt;");

      htmlBody.append("<B>Hexadecimal Representation</B>" + EOL);
      htmlBody.append("<PRE>" + EOL);
      htmlBody.append(hexData + EOL);
      htmlBody.append("</PRE>" + EOL);

      String dataString = new String(data, "UTF-8");
      dataString = replaceText(dataString, "<", "&lt;");
      dataString = replaceText(dataString, ">", "&gt;");

      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<B>String Representation</B>" + EOL);
      htmlBody.append("<PRE>" + EOL);
      htmlBody.append(dataString + EOL);
      htmlBody.append("</PRE>" + EOL);

      htmlBody.append("<BR><BR>" + EOL);
      htmlBody.append("<FORM METHOD=\"POST\" ACTION=\"" + servletBaseURI +
                      "\">" + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_SECTION,
                                            Constants.SERVLET_SECTION_DEBUG) +
                      EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_SUBSECTION,
                           Constants.SERVLET_SECTION_DEBUG_DB) + EOL);
      htmlBody.append("  " + generateHidden(Constants.SERVLET_PARAM_DB_NAME,
                                            dbName) + EOL);
      htmlBody.append("  " +
                      generateHidden(Constants.SERVLET_PARAM_HTML_DEBUG,
                                     "1") + EOL);
      htmlBody.append("  <INPUT TYPE=\"SUBMIT\" VALUE=\"< Back\">" + EOL);
      htmlBody.append("</FORM>" + EOL);
    }
    catch (Exception e)
    {
      htmlBody.append("<PRE>" + EOL);
      htmlBody.append(JobClass.stackTraceToString(e) + EOL);
      htmlBody.append("</PRE>" + EOL);
    }

    return;
  }



  /**
   * Handles the work of requesting that the JVM perform garbage collection.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugGC(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugGC()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Request that the JVM perform garbage collection.
    System.gc();


    // Display a message to the user indicating that the request was performed.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM Garbage Collection Requested</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("A request has been sent to the JVM to perform garbage " +
                    "collection." + EOL);
    htmlBody.append("Note that this request will not have any effect if the " +
                    "JVM has been configured to ignore garbage collection " +
                    "requests." + EOL);
  }



  /**
   * Handles the work of requesting that the JVM enable instruction tracing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugEnableInstructionTrace(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugEnableInstructionTrace()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Request that the JVM enable instruction tracing.
    Runtime.getRuntime().traceInstructions(true);


    // Display a message to the user indicating that the request was performed.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM Instruction Tracing Requested</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("A request has been sent to the JVM to enable " +
                    "instruction tracing." + EOL);
    htmlBody.append("A few things to note about this tracing:" + EOL);

    htmlBody.append("<UL>" + EOL);
    htmlBody.append("  <LI>Tracing may not be supported on all platforms " +
                    "or JVMs.</LI>" + EOL);
    htmlBody.append("  <LI>Tracing may require the use of a debug version of " +
                    "the JVM or invoking the JVM with debugging " +
                    "parameters.</LI>" + EOL);
    htmlBody.append("  <LI>If tracing is supported, then the format, output " +
                    "location, and other details may vary by " +
                    "implementation.</LI>" + EOL);
    htmlBody.append("  <LI>Tracing may impose a severe performance penalty " +
                    "and may generate a great deal of output.  It is " +
                    "strongly recommended that it be used only when " +
                    "necessary for debugging purposes.</LI>" + EOL);
    htmlBody.append("</UL>" + EOL);
  }



  /**
   * Handles the work of requesting that the JVM disable instruction tracing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugDisableInstructionTrace(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugDisableInstructionTrace()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Request that the JVM disable instruction tracing.
    Runtime.getRuntime().traceInstructions(false);


    // Display a message to the user indicating that the request was performed.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM Instruction Tracing Stopped</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("A request has been sent to the JVM to disable " +
                    "instruction tracing." + EOL);
  }



  /**
   * Handles the work of requesting that the JVM enable method call tracing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugEnableMethodTrace(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugEnableMethodTrace()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Request that the JVM enable method call tracing.
    Runtime.getRuntime().traceMethodCalls(true);


    // Display a message to the user indicating that the request was performed.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM Method Call Tracing Requested</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("A request has been sent to the JVM to enable method " +
                    "call tracing." + EOL);
    htmlBody.append("A few things to note about this tracing:" + EOL);

    htmlBody.append("<UL>" + EOL);
    htmlBody.append("  <LI>Tracing may not be supported on all platforms " +
                    "or JVMs.</LI>" + EOL);
    htmlBody.append("  <LI>Tracing may require the use of a debug version of " +
                    "the JVM or invoking the JVM with debugging " +
                    "parameters.</LI>" + EOL);
    htmlBody.append("  <LI>If tracing is supported, then the format, output " +
                    "location, and other details may vary by " +
                    "implementation.</LI>" + EOL);
    htmlBody.append("  <LI>Tracing may impose a severe performance penalty " +
                    "and may generate a great deal of output.  It is " +
                    "strongly recommended that it be used only when " +
                    "necessary for debugging purposes.</LI>" + EOL);
    htmlBody.append("</UL>" + EOL);
  }



  /**
   * Handles the work of requesting that the JVM disable method call tracing.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void handleDebugDisableMethodTrace(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In handleDebugDisableMethodTrace()");

    // The user must have full administrative rights to see anything here.
    if (! requestInfo.hasFullAccess)
    {
      logMessage(requestInfo, "No hasFullAccess permission granted");
      generateAccessDeniedBody(requestInfo, "You do not have permission to " +
                               "view debug information.");
      return;
    }


    // Get the important state variables for this request.
    StringBuilder htmlBody = requestInfo.htmlBody;


    // Request that the JVM disable method call tracing.
    Runtime.getRuntime().traceMethodCalls(false);


    // Display a message to the user indicating that the request was performed.
    htmlBody.append("<SPAN CLASS=\"" + Constants.STYLE_MAIN_HEADER +
                    "\">JVM Method Call Tracing Stopped</SPAN>" + EOL);
    htmlBody.append("<BR><BR>" + EOL);
    htmlBody.append("A request has been sent to the JVM to disable " +
                    "method call tracing." + EOL);
  }



  /**
   * Extracts information from the HTTP request that may be used for debugging
   * purposes.
   *
   * @param  requestInfo  The state information for this request.
   *
   * @return  The debug information that may be added to the page.
   */
  static String debugRequestInfo(RequestInfo requestInfo)
  {
    StringBuilder       buf     = new StringBuilder();
    StringBuilder       urlBuf  = new StringBuilder();
    HttpServletRequest request = requestInfo.request;

    buf.append(EOL);
    buf.append("<!-- Request URI:     ");
    buf.append(requestInfo.servletBaseURI);
    buf.append(" -->");
    buf.append(EOL);

    buf.append("<!-- Request Method:  ");
    buf.append(request.getMethod());
    buf.append(" -->");
    buf.append(EOL);

    buf.append("<!-- Remote User:     ");
    buf.append(request.getRemoteUser());
    buf.append(" -->");
    buf.append(EOL);

    buf.append("<!-- Remote Addr:     ");
    buf.append(request.getRemoteAddr());
    buf.append(" -->");
    buf.append(EOL);

    buf.append("<!-- Remote Host:     ");
    buf.append(request.getRemoteHost());
    buf.append(" -->");
    buf.append(EOL);

    buf.append("<!-- Begin Request Headers -->");
    buf.append(EOL);
    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements())
    {
      String headerName = (String) headerNames.nextElement();
      buf.append("<!--   ");
      buf.append(headerName);
      buf.append(" -->");
      buf.append(EOL);
      Enumeration headerValues = request.getHeaders(headerName);
      while (headerValues.hasMoreElements())
      {
        buf.append("<!--     ");
        buf.append((String) headerValues.nextElement());
        buf.append(" -->");
        buf.append(EOL);
      }
    }
    buf.append("<!-- End Request Headers -->");
    buf.append(EOL);

    urlBuf.append(request.getRequestURL());
    String separator = "?";

    buf.append("<!-- Begin Request Parameters -->");
    buf.append(EOL);
    Enumeration parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements())
    {
      String parameterName = (String) parameterNames.nextElement();
      if (parameterName.equals(Constants.SERVLET_PARAM_HTML_DEBUG))
      {
        continue;
      }

      buf.append("<!--   ");
      buf.append(parameterName);
      buf.append(" -->");
      buf.append(EOL);

      String[] parameterValues = request.getParameterValues(parameterName);
      for (int i=0; ((parameterValues != null) && (i < parameterValues.length));
           i++)
      {
        buf.append("<!--     ");
        buf.append(parameterValues[i]);
        buf.append(" -->");
        buf.append(EOL);

        urlBuf.append(separator);
        urlBuf.append(parameterName);
        urlBuf.append('=');
        urlBuf.append(parameterValues[i]);
      }

      if ((parameterValues == null) || (parameterValues.length == 0))
      {
        urlBuf.append(separator);
        urlBuf.append(parameterName);
        urlBuf.append('=');
      }

      separator = "&";
    }
    buf.append("<!-- End Request Parameters -->");
    buf.append(EOL);

    buf.append("<!-- HTTP GET URL:  ");

    final String getURL = replaceText(urlBuf.toString(), " ", "%20");
    requestInfo.setGETURL(getURL);

    buf.append(getURL);
    buf.append(" -->" + EOL);
    buf.append(EOL);
    buf.append(EOL);

    return buf.toString();
  }
}
