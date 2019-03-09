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



import netscape.ldap.LDAPException;

import com.slamd.common.Constants;

import static com.slamd.admin.AdminServlet.*;



/**
 * This class provides a set of methods for providing access control for the
 * administrative user interface.
 */
public class AdminAccess
{
  /**
   * Retrieves the access manager associated with this admin servlet.
   *
   * @return  The access manager associated with this admin servlet.
   */
  public static AccessManager getAccessManager()
  {
    return accessManager;
  }



  /**
   * Registers the specified protected resource with the access control manager,
   * if appropriate.
   *
   * @param  requestInfo     The state information for this request.
   * @param  resourceName    The name to use for the protected resource.
   * @param  resourceDN      The DN of the group or role that specifies which
   *                         users have access to content associated with the
   *                         resource name.
   * @param  flushUserCache  Indicates whether the access manager's user info
   *                         cache should be flushed after registering the
   *                         new protected resource.
   */
  static void registerACL(RequestInfo requestInfo, String resourceName,
                          String resourceDN, boolean flushUserCache)
  {
    logMessage(requestInfo, "In registerACL(" + resourceName + ')');
    if ((resourceDN != null) && (resourceDN.length() > 0))
    {
      accessManager.register(resourceName, resourceDN, flushUserCache);
    }
  }



  /**
   * Sets the values of the non-static instance variables related to access
   * control management.  These instance variables are used to make the access
   * control processing code in other sections of this class much simpler.
   *
   * @param  requestInfo  The state information for this request.
   */
  static void setAccessControlVariables(RequestInfo requestInfo)
  {
    logMessage(requestInfo, "In setAccessControlVariables()");

    if (readOnlyMode)
    {
      requestInfo.hasFullAccess             = false;
      requestInfo.mayStartStopSLAMD         = false;
      requestInfo.mayStartStopAccessManager = false;
      requestInfo.mayViewStatus             = false;
      requestInfo.mayDisconnectClients      = false;
      requestInfo.mayViewServletConfig      = false;
      requestInfo.mayEditServletConfig      = false;
      requestInfo.mayViewSLAMDConfig        = false;
      requestInfo.mayEditSLAMDConfig        = false;
      requestInfo.mayManageFolders          = false;
      requestInfo.mayViewJob                = true;
      requestInfo.mayExportJobData          = false;
      requestInfo.mayScheduleJob            = false;
      requestInfo.mayCancelJob              = false;
      requestInfo.mayDeleteJob              = false;
      requestInfo.mayViewJobClass           = false;
      requestInfo.mayAddJobClass            = false;
      requestInfo.mayDeleteJobClass         = false;
      return;
    }
    else if (! useAccessControl)
    {
      requestInfo.hasFullAccess             = true;
      requestInfo.mayStartStopSLAMD         = true;
      requestInfo.mayStartStopAccessManager = true;
      requestInfo.mayViewStatus             = true;
      requestInfo.mayDisconnectClients      = true;
      requestInfo.mayViewServletConfig      = true;
      requestInfo.mayEditServletConfig      = true;
      requestInfo.mayViewSLAMDConfig        = true;
      requestInfo.mayEditSLAMDConfig        = true;
      requestInfo.mayManageFolders          = true;
      requestInfo.mayViewJob                = true;
      requestInfo.mayExportJobData          = true;
      requestInfo.mayScheduleJob            = true;
      requestInfo.mayCancelJob              = true;
      requestInfo.mayDeleteJob              = true;
      requestInfo.mayViewJobClass           = true;
      requestInfo.mayAddJobClass            = true;
      requestInfo.mayDeleteJobClass         = true;
      return;
    }


    if ((resourceDNFullAccess == null) || (resourceDNFullAccess.length() == 0))
    {
      requestInfo.hasFullAccess = true;
    }
    else
    {
      requestInfo.hasFullAccess =
           mayAccess(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_FULL);
    }

    if ((resourceDNRestartSLAMD == null) ||
        (resourceDNRestartSLAMD.length() == 0))
    {
      requestInfo.mayStartStopSLAMD = true;
    }
    else
    {
      requestInfo.mayStartStopSLAMD =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_RESTART_SLAMD);
    }

    if ((resourceDNRestartACL == null) ||
        (resourceDNRestartACL.length() == 0))
    {
      requestInfo.mayStartStopAccessManager = true;
    }
    else
    {
      requestInfo.mayStartStopAccessManager =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_RESTART_ACL);
    }

    if ((resourceDNViewStatus == null) || (resourceDNViewStatus.length() == 0))
    {
      requestInfo.mayViewStatus = true;
    }
    else
    {
      requestInfo.mayViewStatus =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_STATUS);
    }

    if ((resourceDNDisconnectClient == null) ||
        (resourceDNDisconnectClient.length() == 0))
    {
      requestInfo.mayDisconnectClients = true;
    }
    else
    {
      requestInfo.mayDisconnectClients =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_DISCONNECT_CLIENT);
    }

    if ((resourceDNViewServletConfig == null) ||
        (resourceDNViewServletConfig.length() == 0))
    {
      requestInfo.mayViewServletConfig = true;
    }
    else
    {
      requestInfo.mayViewServletConfig =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_SERVLET_CONFIG);
    }

    if ((resourceDNEditServletConfig == null) ||
        (resourceDNEditServletConfig.length() == 0))
    {
      requestInfo.mayEditServletConfig = true;
    }
    else
    {
      requestInfo.mayEditServletConfig =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_EDIT_SERVLET_CONFIG);
    }

    if ((resourceDNViewSLAMDConfig == null) ||
        (resourceDNViewSLAMDConfig.length() == 0))
    {
      requestInfo.mayViewSLAMDConfig = true;
    }
    else
    {
      requestInfo.mayViewSLAMDConfig =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_SLAMD_CONFIG);
    }

    if ((resourceDNEditSLAMDConfig == null) ||
        (resourceDNEditSLAMDConfig.length() == 0))
    {
      requestInfo.mayEditSLAMDConfig = true;
    }
    else
    {
      requestInfo.mayEditSLAMDConfig =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_EDIT_SLAMD_CONFIG);
    }

    if ((resourceDNViewJob == null) || (resourceDNViewJob.length() == 0))
    {
      requestInfo.mayViewJob = true;
    }
    else
    {
      requestInfo.mayViewJob =
           mayAccess(requestInfo, Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_JOB);
    }

    if ((resourceDNExportJob == null) || (resourceDNExportJob.length() == 0))
    {
      requestInfo.mayExportJobData = true;
    }
    else
    {
      requestInfo.mayExportJobData =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_EXPORT_JOB);
    }

    if ((resourceDNScheduleJob == null) ||
        (resourceDNScheduleJob.length() == 0))
    {
      requestInfo.mayScheduleJob = true;
    }
    else
    {
      requestInfo.mayScheduleJob =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_SCHEDULE_JOB);
    }

    if ((resourceDNCancelJob == null) || (resourceDNCancelJob.length() == 0))
    {
      requestInfo.mayCancelJob = true;
    }
    else
    {
      requestInfo.mayCancelJob =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_CANCEL_JOB);
    }

    if ((resourceDNDeleteJob == null) || (resourceDNDeleteJob.length() == 0))
    {
      requestInfo.mayDeleteJob = true;
    }
    else
    {
      requestInfo.mayDeleteJob =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_DELETE_JOB);
    }

    if ((resourceDNManageJobFolders == null) ||
        (resourceDNManageJobFolders.length() == 0))
    {
      requestInfo.mayManageFolders = true;
    }
    else
    {
      requestInfo.mayManageFolders =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_MANAGE_JOB_FOLDERS);
    }

    if ((resourceDNViewJobClass == null) ||
        (resourceDNViewJobClass.length() == 0))
    {
      requestInfo.mayViewJobClass = true;
    }
    else
    {
      requestInfo.mayViewJobClass =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_VIEW_JOB_CLASS);
    }

    if ((resourceDNAddJobClass == null) ||
        (resourceDNAddJobClass.length() == 0))
    {
      requestInfo.mayAddJobClass = true;
    }
    else
    {
      requestInfo.mayAddJobClass =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_ADD_JOB_CLASS);
    }

    if ((resourceDNDeleteJobClass == null) ||
        (resourceDNDeleteJobClass.length() == 0))
    {
      requestInfo.mayDeleteJobClass = true;
    }
    else
    {
      requestInfo.mayDeleteJobClass =
           mayAccess(requestInfo,
                     Constants.SERVLET_INIT_PARAM_ACCESS_DELETE_JOB_CLASS);
    }
  }



  /**
   * Indicate whether the currently authenticated user should be allowed to
   * access the specified resource.  If a problem occurs while attempting to
   * make the determination, then the info message will be set.
   *
   * @param  requestInfo   The state information for this request.
   * @param  resourceName  The name of the protected resource for which to make
   *                       the determination.
   *
   * @return  <CODE>true</CODE> if the user should be allowed to access the
   *          specified resource, or <CODE>false</CODE> if not.
   */
  static boolean mayAccess(RequestInfo requestInfo, String resourceName)
  {
    logMessage(requestInfo, "In mayAccess(" + resourceName + ')');

    String       userIdentifier = requestInfo.userIdentifier;
    StringBuilder infoMessage    = requestInfo.infoMessage;

    if (useAccessControl)
    {
      if (requestInfo.userIdentifier == null)
      {
        infoMessage.append("Could not determine the user identity, but " +
                           "authentication is required<BR>" + EOL);
        logMessage(requestInfo, "mayAccess(" + resourceName +
                   ") = false -- could not determine user identity");
        return false;
      }


      try
      {
        boolean returnValue =
             (accessManager.mayAccess(userIdentifier, resourceName) ||
              accessManager.mayAccess(userIdentifier,
                   Constants.SERVLET_INIT_PARAM_ACCESS_FULL));
        logMessage(requestInfo, "mayAccess(" + resourceName +
                   ") = " + returnValue);
        return returnValue;
      }
      catch (AccessDeniedException ade)
      {
        infoMessage.append("Could not retrieve access control information " +
                           "for user " + userIdentifier + ":  " +
                           ade.getMessage() + "<BR>" + EOL);
        logMessage(requestInfo, "mayAccess(" + resourceName +
                   ") = false -- LDAP exception " + ade.getMessage());
        return false;
      }
      catch (LDAPException le)
      {
        infoMessage.append("Could not retrieve access control information " +
                           "for user " + userIdentifier + ":  " + le + "<BR>" +
                           EOL);
        logMessage(requestInfo, "mayAccess(" + resourceName +
                   ") = false -- LDAP exception " + le);
        return false;
      }
    }
    else
    {
      logMessage(requestInfo, "mayAccess(" + resourceName +
                 ") = true -- access control disabled");
      return true;
    }
  }
}
