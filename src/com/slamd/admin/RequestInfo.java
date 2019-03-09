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



import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;

import com.slamd.common.Constants;



/**
 * This class defines a set of variables that are used to hold variables used
 * in the process of handling an individual request in the admin interface.
 *
 *
 * @author   Neil A. Wilson
 */
public class RequestInfo
{
  // Indicates whether the servlet is currently should generate extra HTML that
  // can be used for debugging purposes.
  protected boolean debugHTML;

  // Indicates whether the doPost method should generate the response that gets
  // sent back to the client or if it has already been sent in the course of
  // processing.
  protected boolean generateHTML;

  // Indicates whether the sidebar containing link information should be
  // generated.
  protected boolean generateSidebar;

  // Indicates whether the user has full access to the SLAMD admin interface.
  protected boolean hasFullAccess;

  // Indicates whether the user may add job class definitions.
  protected boolean mayAddJobClass;

  // Indicates whether the user may cancel scheduled jobs.
  protected boolean mayCancelJob;

  // Indicates whether the user may delete completed job info.
  protected boolean mayDeleteJob;

  // Indicates whether the user may delete job class definitions.
  protected boolean mayDeleteJobClass;

  // Indicates whether the user may disconnect clients.
  protected boolean mayDisconnectClients;

  // Indicates whether the user may edit servlet config info.
  protected boolean mayEditServletConfig;

  // Indicates whether the user may edit SLAMD config info.
  protected boolean mayEditSLAMDConfig;

  // Indicates whether the user may export job data.
  protected boolean mayExportJobData;

  // Indicates whether the user may manage real and virtual job folders.
  protected boolean mayManageFolders;

  // Indicates whether the user may schedule jobs for execution.
  protected boolean mayScheduleJob;

  // Indicates whether the user may start or stop the access control manager.
  protected boolean mayStartStopAccessManager;

  // Indicates whether the user may start or stop SLAMD.
  protected boolean mayStartStopSLAMD;

  // Indicates whether the user may view job class definitions.
  protected boolean mayViewJobClass;

  // Indicates whether the user may view job information.
  protected boolean mayViewJob;

  // Indicates whether the user may view the servlet config info.
  protected boolean mayViewServletConfig;

  // Indicates whether the user may view the SLAMD server config info.
  protected boolean mayViewSLAMDConfig;

  // Indicates whether the user may view the server status info.
  protected boolean mayViewStatus;

  // The structure with information regarding the servlet request.
  protected final HttpServletRequest request;

  // The structure with information regarding the servlet response.
  protected final HttpServletResponse response;

  // The unique ID assigned to this request.  It will appear as an HTML comment
  // at the top of the response page generated, and will also appear at the
  // beginning of any messages logged using the admin interface debugging log
  // level.
  protected int requestID;

  // The list of fields provided in the multipart request form, if it was a
  // multipart request.
  protected List multipartFieldList;

  // The string representation of the URL that may be used to generate the
  // current page using an HTTP GET.
  protected String getURL;

  // The name of the administration section with which this request is
  // associated.
  protected String section;

  // The base URI for the servlet request.
  protected String servletBaseURI;

  // The name of the administrative subsection with which this request is
  // associated.
  protected String subsection;

  // The user ID
  protected String userIdentifier;

  // A set of debug information that will be included as comments in the HTML
  // that is generated.  Any message that would be logged to the SLAMD server
  // log file will also be written here.
  protected StringBuilder debugInfo;

  // The main body of the HTML page to generate.
  protected StringBuilder htmlBody;

  // A message that should be displayed at the top of the main frame.
  protected StringBuilder infoMessage;



  /**
   * Creates a new set of request state information using the provided request
   * and response.
   *
   * @param  request   Information about the HTTP request issued by the client.
   * @param  response  Information about the HTTP response that will be returned
   *                   to the client.
   */
  public RequestInfo(HttpServletRequest request, HttpServletResponse response)
  {
    this.request  = request;
    this.response = response;

    generateHTML    = true;
    debugInfo       = new StringBuilder();
    htmlBody        = new StringBuilder();
    infoMessage     = new StringBuilder();

    if (request != null)
    {
      servletBaseURI = request.getRequestURI();
      userIdentifier = request.getRemoteUser();

      if (FileUpload.isMultipartContent(request))
      {
        try
        {
          FileUpload fileUpload = new FileUpload(new DefaultFileItemFactory());
          multipartFieldList = fileUpload.parseRequest(request);
          Iterator iterator = multipartFieldList.iterator();

          while (iterator.hasNext())
          {
            FileItem fileItem = (FileItem) iterator.next();
            String name = fileItem.getFieldName();
            if (name.equals(Constants.SERVLET_PARAM_SECTION))
            {
              section = new String(fileItem.get());
            }
            else if (name.equals(Constants.SERVLET_PARAM_SUBSECTION))
            {
              subsection = new String(fileItem.get());
            }
          }
        } catch (FileUploadException fue) { fue.printStackTrace(); }
      }
      else
      {
        section    = request.getParameter(Constants.SERVLET_PARAM_SECTION);
        subsection = request.getParameter(Constants.SERVLET_PARAM_SUBSECTION);
      }
    }

    if (section == null)
    {
      section = "";
    }

    if (subsection == null)
    {
      subsection = "";
    }
  }



  /**
   * Retrieves information about the request received from the end user.
   *
   * @return  Information about the request received from the end user.
   */
  public HttpServletRequest getRequest()
  {
    return request;
  }



  /**
   * Retrieves information about the response to send to the end user.
   *
   * @return  Information about the response to send to the end user.
   */
  public HttpServletResponse getResponse()
  {
    return response;
  }



  /**
   * Retrieves the base URI for the request.
   *
   * @return  The base URI for the request.
   */
  public String getServletBaseURI()
  {
    return servletBaseURI;
  }



  /**
   * Retrieves the HTTP GET URL that could be used to reproduce the current
   * page.
   *
   * @return  The HTTP GET URL that could be used to reproduce the current page.
   */
  public String getGETURL()
  {
    return getURL;
  }



  /**
   * Specifies the HTTP GET URL that could be used to reproduce the current
   * page.
   *
   * @param  getURL  The HTTP GET URL that could be used to reproduce the
   *                 current page.
   */
  public void setGETURL(final String getURL)
  {
    this.getURL = getURL;
  }



  /**
   * Retrieves the user identifier associated with the current user.
   *
   * @return  The user identifier associated with the current user, or
   *          <CODE>null</CODE> if that is not available.
   */
  public String getUserIdentifier()
  {
    return userIdentifier;
  }



  /**
   * Indicates whether the response to the end user will be generated from the
   * information in the HTML body string buffer.
   *
   * @return  <CODE>true</CODE> if the response to the end user will be
   *          generated from the information in the HTML body string buffer, or
   *          <CODE>false</CODE> if the response will be generated by directly
   *          accessing the servlet response.
   */
  public boolean generateHTML()
  {
    return generateHTML;
  }



  /**
   * Specifies whether the response to the end user should be generated from the
   * information in the HTML body string buffer.
   *
   * @param  generateHTML  Specifies whether the response to the end user should
   *                       be generated from the information in the HTML body
   *                       string buffer.
   */
  public void setGenerateHTML(boolean generateHTML)
  {
    this.generateHTML = generateHTML;
  }



  /**
   * Indicates whether the response generated based on the information in the
   * HTML body string buffer will include the navigation sidebar.  This is only
   * applicable if <CODE>generateHTML()</CODE> returns <CODE>true</CODE>.
   *
   * @return  <CODE>true</CODE> if the response generated will include the
   *          navigation sidebar, or <CODE>false</CODE> if it will not.
   */
  public boolean generateSidebar()
  {
    return generateSidebar;
  }



  /**
   * Specifies whether the response generated based on the information in the
   * HTML body string buffer should include the navigation sidebar.
   *
   * @param  generateSidebar  Specifies whether the response generated should
   *                          include the navigation sidebar.
   */
  public void setGenerateSidebar(boolean generateSidebar)
  {
    this.generateSidebar = generateSidebar;
  }



  /**
   * Retrieves the string buffer containing the main content that will be
   * included in the HTML page generated and returned to the end user.
   * Additional content may be included by appending to this string buffer.
   *
   * @return  The string buffer containing the main content that will be
   *          included in the HTML page generated and returned to the end user.
   */
  public StringBuilder getHTMLBody()
  {
    return htmlBody;
  }



  /**
   * Retrieves the string buffer containing the informational message that will
   * be displayed at the top of the page to provide a warning, error, or notice
   * to the end user in a prominent form.  Additional content may be included by
   * appending to this string buffer.
   *
   * @return  The string buffer containing the informational message that will
   *          be displayed at the top of the page.
   */
  public StringBuilder getInfoMessage()
  {
    return infoMessage;
  }
}

