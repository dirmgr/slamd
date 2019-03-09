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



import java.util.ArrayList;
import java.util.HashMap;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPDN;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;
import netscape.ldap.LDAPUrl;
import netscape.ldap.factory.JSSESocketFactory;

import com.slamd.common.Constants;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;
import com.slamd.server.SLAMDServer;



/**
 * This class provides a mechanism for restricting access to components of the
 * administrative interface.  It caches user access information for better
 * performance, but also provides a mechanism for flushing that cache so that
 * access can be re-evaluated.
 *
 * Note that this class does not actually restrict access or require
 * authentication, because that must be configured in the Web server itself.
 * Rather, this class will determine whether the authenticated user may access
 * a particular resource, as well as which resources a given user may access
 * (which are two different things).
 *
 *
 * @author   Neil A. Wilson
 */
public class AccessManager
{
  /**
   * The set of attributes to return if you don't want any attributes returned.
   */
  private static final String[] noAttrs =
       new String[] { LDAPConnection.NO_ATTRS };



  /**
   * The set of attributes to return if you want the member URL for a dynamic
   * group.
   */
  private static final String[] memberURLAttrs =
       new String[] { Constants.MEMBER_URL_AT };



  /**
   * The set of attributes to return if you want the list of roles from a user
   * entry.
   */
  private static final String[] roleAttrs =
       new String[] { Constants.ROLE_DN_AT };



  // Indicates whether the access manager has been stopped.
  private boolean managerStopped;

  // Indicates whether communication with the user directory should use SSL.
  private boolean useSSL;

  // Indicates whether the SLAMD server should blindly trust any certificate
  // presented by the user directory.
  private boolean userDirBlindTrust;

  // A cache of information about what a particular user may access.
  private HashMap<String,String[]> userInfoCache;

  // The port number for the user directory.
  private int userDirectoryPort;

  // The connection to the user directory.
  private LDAPConnection userDirConn;

  // The mutex used to provide threadsafe access to the user directory
  // connection.
  private final Object connMutex;

  // The mutex used to provide threadsafe access to the list of protected
  // resources.
  private final Object protectedResourceMutex;

  // The mutex used to provide threadsafe access to the user info cache.
  private final Object userCacheMutex;

  // The SLAMD server with which this access manager is associated.
  private SLAMDServer slamdServer;

  // The location of the JSSE key store.
  private String sslKeyStore;

  // The password for the JSSE key store.
  private String sslKeyPassword;

  // The location of the JSSE trust store.
  private String sslTrustStore;

  // The password for the JSSE trust store.
  private String sslTrustPassword;

  // The location in the user directory under which user accounts may be found.
  private String userBaseDN;

  // The DN to use to bind to the user directory.
  private String userDirectoryBindDN;

  // The password for the user directory bind DN.
  private String userDirectoryBindPW;

  // The hostname / IP address of the user directory.
  private String userDirectoryHost;

  // The name of the LDAP attribute used as the login ID.
  private String userIDAttribute;

  // The set of resources to which this access manager can protect access.
  private String[][] protectedResources;



  /**
   * Creates a new access manager to use for protecting access to the
   * administrative interface.  Note that the manager will not be started, so
   * it is necessary to call the <CODE>startAccessMananager</CODE> to start it.
   *
   * @param  userDirectoryHost    The address of the user directory.
   * @param  userDirectoryPort    The port number of the user directory.
   * @param  userDirectoryBindDN  The DN to use to bind to the user directory.
   * @param  userDirectoryBindPW  The password for the user directory bind DN.
   * @param  userBaseDN           The location in the user directory under which
   *                              user entries may be found.
   * @param  userIDAttribute      The name of the LDAP attribute that is used as
   *                              the login ID.
   */
  public AccessManager(String userDirectoryHost, int userDirectoryPort,
                       String userDirectoryBindDN, String userDirectoryBindPW,
                       String userBaseDN, String userIDAttribute)
  {
    this(userDirectoryHost, userDirectoryPort, userDirectoryBindDN,
         userDirectoryBindPW, userBaseDN, userIDAttribute, false, true, null,
         null, null, null);
  }



  /**
   * Creates a new access manager to use for protecting access to the
   * administrative interface.  Note that the manager will not be started, so
   * it is necessary to call the <CODE>startAccessMananager</CODE> to start it.
   *
   * @param  userDirectoryHost    The address of the user directory.
   * @param  userDirectoryPort    The port number of the user directory.
   * @param  userDirectoryBindDN  The DN to use to bind to the user directory.
   * @param  userDirectoryBindPW  The password for the user directory bind DN.
   * @param  userBaseDN           The location in the user directory under which
   *                              user entries may be found.
   * @param  userIDAttribute      The name of the LDAP attribute that is used as
   *                              the login ID.
   * @param  useSSL               Indicates whether the communication with the
   *                              user directory should use SSL.
   * @param  userDirBlindTrust    Indicates whether the SLAMD server should
   *                              blindly trust any SSL certificate presented by
   *                              the user directory.
   * @param  sslKeyStore          The location of the JSSE key store to be used
   *                              for SSL communication.
   * @param  sslKeyPassword       The password to access the JSSE key store.
   * @param  sslTrustStore        The location of the JSSE trust store to be
   *                              used for SSL communication.
   * @param  sslTrustPassword     The password to access the JSSE trust store.
   */
  public AccessManager(String userDirectoryHost, int userDirectoryPort,
                       String userDirectoryBindDN, String userDirectoryBindPW,
                       String userBaseDN, String userIDAttribute,
                       boolean useSSL, boolean userDirBlindTrust,
                       String sslKeyStore, String sslKeyPassword,
                       String sslTrustStore, String sslTrustPassword)
  {
    // Set the values of the instance variables that correspond to constructor
    // arguments.
    this.userDirectoryHost   = userDirectoryHost;
    this.userDirectoryPort   = userDirectoryPort;
    this.userDirectoryBindDN = userDirectoryBindDN;
    this.userDirectoryBindPW = userDirectoryBindPW;
    this.userBaseDN          = userBaseDN;
    this.userIDAttribute     = userIDAttribute;
    this.useSSL              = useSSL;
    this.userDirBlindTrust   = userDirBlindTrust;
    this.sslKeyStore         = sslKeyStore;
    this.sslKeyPassword      = sslKeyPassword;
    this.sslTrustStore       = sslTrustStore;
    this.sslTrustPassword    = sslTrustPassword;


    // Initialize the remaining instance variables.
    this.managerStopped          = true;
    this.connMutex               = new Object();
    this.protectedResourceMutex  = new Object();
    this.userCacheMutex          = new Object();
    this.userInfoCache           = new HashMap<String,String[]>();
    this.protectedResources      = new String[0][];
  }



  /**
   * Specifies the SLAMD server that should be associated with this access
   * manager.  This is separate from the constructor because access to the admin
   * interface can be restricted even with the SLAMD server offline (e.g., to
   * restrict who can start and stop the  server).  Note that while the SLAMD
   * server is offline, certain functions like logging will not be performed.
   *
   * @param  slamdServer  The SLAMD server with which this access manager is
   *                      associated.
   */
  void setSLAMDServer(SLAMDServer slamdServer)
  {
    this.slamdServer = slamdServer;
    logMessage("Associated the access manager with the SLAMD server");
  }



  /**
   * Starts the access manager.  If it is already running, then this does
   * nothing.  Otherwise, it establishes the connection to the user directory
   * and configures recreates the user cache.
   *
   * @throws  LDAPException  If there is a problem establishing the connection
   *                         to the user directory.
   */
  public void startAccessManager()
         throws LDAPException
  {
    logMessage("Starting the access manager");

    // Grab all the locks
    synchronized (connMutex)
    {
      synchronized (protectedResourceMutex)
      {
        synchronized (userCacheMutex)
        {
          // Grab the connection to the user directory.
          if (useSSL)
          {
            if (userDirBlindTrust)
            {
              JSSEBlindTrustSocketFactory socketFactory =
                   new JSSEBlindTrustSocketFactory();
              userDirConn = new LDAPConnection(socketFactory);
            }
            else
            {
              if ((sslKeyStore != null) && (sslKeyStore.length() > 0))
              {
                System.setProperty(Constants.SSL_KEY_STORE_PROPERTY,
                                   sslKeyStore);
              }
              if ((sslKeyPassword != null) && (sslKeyPassword.length() > 0))
              {
                System.setProperty(Constants.SSL_KEY_PASSWORD_PROPERTY,
                                   sslKeyPassword);
              }
              if ((sslTrustStore != null) && (sslTrustStore.length() > 0))
              {
                System.setProperty(Constants.SSL_TRUST_STORE_PROPERTY,
                                   sslTrustStore);
              }
              if ((sslTrustPassword != null) && (sslTrustPassword.length() > 0))
              {
                System.setProperty(Constants.SSL_TRUST_PASSWORD_PROPERTY,
                                   sslTrustPassword);
              }

              userDirConn = new LDAPConnection(new JSSESocketFactory(null));
            }
          }
          else
          {
            userDirConn = new LDAPConnection();
          }
          userDirConn.connect(3, userDirectoryHost, userDirectoryPort,
                              userDirectoryBindDN, userDirectoryBindPW);
        }
      }
    }

    managerStopped = false;
    logMessage("Access manager started");
  }



  /**
   * Closes the connection to the user directory and stops the access manager.
   * Note that when the access manager is stopped, any requests to the access
   * manager will indicate that no access is allowed to anything.
   */
  public void stopAccessManager()
  {
    logMessage("Stopping the access manager");
    managerStopped = true;

    // Grab all the locks
    synchronized (connMutex)
    {
      synchronized (protectedResourceMutex)
      {
        synchronized (userCacheMutex)
        {
          // Close the connection to the user directory
          try
          {
            synchronized (connMutex)
            {
              userDirConn.disconnect();
            }
          }
          catch (LDAPException le)
          {
            logMessage("Exception while closing the user directory " +
                       "connection:  " + le);
          }

          // Flush the user cache
          userInfoCache.clear();
        }
      }
    }
  }



  /**
   * Indicates whether this access manager has been stopped.
   *
   * @return  <CODE>true</CODE> if the access manager is stopped, or
   *          <CODE>false</CODE> if it is running.
   */
  public boolean managerIsStopped()
  {
    return managerStopped;
  }



  /**
   * Registers the specified resource as one that is protected by this access
   * manager.  If the specified resource is already defined, then this will
   * assign it to the new resource DN.  If specified, it will also flush the
   * user cache (which is necessary for users whose information is cached to be
   * able to access this new protected resource).
   *
   * @param  resourceName    The name associated with this protected resource.
   * @param  resourceDN      The DN of the entry in the user directory that will
   *                         be used to determine whether users may access this
   *                         resource.
   * @param  flushUserCache  Indicates whether the user cache should be flushed
   *                         in the process of adding this controlled resource.
   */
  public void register(String resourceName, String resourceDN,
                       boolean flushUserCache)
  {
    logMessage("In registerProtectedResource(" + resourceName + ", " +
               resourceDN + ", " + flushUserCache + ')');

    resourceDN = LDAPDN.normalize(resourceDN.toLowerCase());
    synchronized (protectedResourceMutex)
    {
      // If it's already there, then replace it.
      boolean added = false;
      for (int i=0; i < protectedResources.length; i++)
      {
        if (protectedResources[i][0].equals(resourceName))
        {
          protectedResources[i][1] = resourceDN;
          added = true;
          break;
        }
      }


      // If it was not already in the list of protected resources, then add it.
      if (! added)
      {
        String[][] newResourceList = new String[protectedResources.length+1][];
        System.arraycopy(protectedResources, 0, newResourceList, 0,
                         protectedResources.length);
        newResourceList[protectedResources.length] =
             new String[] { resourceName, resourceDN };
        protectedResources = newResourceList;
      }


      // Flush the user cache if appropriate.
      if (flushUserCache)
      {
        flushUserCache();
      }
    }
  }



  /**
   * Removes the specified resource from the set of protected resources for this
   * access manager, optionally flushing the user cache in the process.
   *
   * @param  resourceName    The name of the protected resource to be
   * @param  flushUserCache  Indicates whether the user cache should be flushed
   *                         in the process of removing the controlled resource.
   */
  public void deregister(String resourceName, boolean flushUserCache)
  {
    logMessage("In deregisterProtectedResource(" + resourceName + ", " +
               flushUserCache + ')');

    synchronized (protectedResourceMutex)
    {
      // Iterate through the list.  If the specified resource isn't in the list,
      // then there's no need to do anything.
      for (int i=0; i < protectedResources.length; i++)
      {
        if (protectedResources[i][0].equals(resourceName))
        {
          String[][] newList = new String[protectedResources.length-1][];
          System.arraycopy(protectedResources, 0, newList, 0, i);
          System.arraycopy(protectedResources, i+1, newList, i,
                           newList.length-i);
          protectedResources = newList;


          // Flush the user cache if necessary.
          if (flushUserCache)
          {
            flushUserCache();
          }

          return;
        }
      }
    }
  }



  /**
   * Retrieves the set of protected resources for this access manager.  The
   * first element of each set will be the resource name and the second will be
   * the associated entry DN.
   *
   * @return  The set of protected resources for this access manager.
   */
  public String[][] getProtectedResources()
  {
    synchronized (protectedResourceMutex)
    {
      logMessage("getControlledResources() -- returning " +
                 protectedResources.length + " items");
      return protectedResources;
    }
  }



  /**
   * Flushes the user cache, which means that information about a user and what
   * he/she may access will be re-read from the user directory the next time the
   * user accesses a protected area of the administrative interface.
   */
  public void flushUserCache()
  {
    logMessage("Flushing the user info cache");

    synchronized (userCacheMutex)
    {
      userInfoCache.clear();
    }
  }



  /**
   * Retrieves the names of all the protected resources that the specified user
   * may access.
   *
   * @param  userIdentifier  The ID of the user for which to retrieve the names
   *                         of the accessible resources.
   *
   * @return  The names of all the protected resources that the specified user
   *          may access.  If no information is known about the specified user,
   *          then <CODE>null</CODE> is returned.
   *
   * @throws  AccessDeniedException  If it is necessary to go to the user
   *                                 directory to retrieve information for this
   *                                 user but the user entry could not be found.
   *
   * @throws  LDAPException  If it is necessary to go to the user directory to
   *                         retrieve the information for this user and a
   *                         problem occurs while doing so.
   */
  public String[] getAccessibleResources(String userIdentifier)
         throws AccessDeniedException, LDAPException
  {
    logMessage("In getAccessibleResources(" + userIdentifier + ')');


    // If the access manager is stopped, then no access is granted to anyone.
    if (managerStopped)
    {
      logMessage("getAccessibleResources(" + userIdentifier +
                 ") -- manager stopped; returning null");
    }


    String[] accessibleResources = null;
    synchronized (userCacheMutex)
    {
      // First, check the user cache to see if we already have this information.
      accessibleResources = userInfoCache.get(userIdentifier);

      if (accessibleResources == null)
      {
        accessibleResources = getUserInfoFromDirectory(userIdentifier);
        if (accessibleResources != null)
        {
          userInfoCache.put(userIdentifier, accessibleResources);
        }
      }
    }

    return accessibleResources;
  }



  /**
   * Indicates whether the specified user may access the indicated resource.
   *
   * @param  userIdentifier  The ID of the user for which to make the
   *                         determination.
   * @param  resourceName    The name of the resource for which to check access
   *                         permissions.
   *
   * @return  <CODE>true</CODE> if the user may access the indicated resource,
   *          or <CODE>false</CODE> if not or if the manager is stopped.
   *
   * @throws  AccessDeniedException  If it is necessary to go to the user
   *                                 directory to retrieve information for this
   *                                 user but the user entry could not be found.
   *
   * @throws  LDAPException  If it is necessary to go to the user directory to
   *                         retrieve the information for this user and a
   *                         problem occurs while doing so.
   */
  public boolean mayAccess(String userIdentifier, String resourceName)
         throws AccessDeniedException, LDAPException
  {
    // If the access manager is stopped, then no access is granted to anyone.
    if (managerStopped)
    {
      logMessage("mayAccess(" + userIdentifier + ", " + resourceName +
                 ") -- manager stopped; returning false");
    }

    String[] resources = getAccessibleResources(userIdentifier);
    if (resources == null)
    {
      logMessage("mayAccess(" + userIdentifier + ", " + resourceName +
                 ") -- accessible resources is null; returning false");
      return false;
    }

    for (int i=0; i < resources.length; i++)
    {
      if (resources[i].equals(resourceName))
      {
        logMessage("mayAccess(" + userIdentifier + ", " + resourceName +
                   ") returning true");
        return true;
      }
    }


  logMessage("mayAccess(" + userIdentifier + ", " + resourceName +
             ") returning false");
    return false;
  }



  /**
   * Retrieves the set of all accessible resources for the specified user from
   * the user directory.
   *
   * @param  userIdentifier  The identifier used to find the user entry in the
   *                         user directory.
   *
   * @return  The names of all the resources that the user may access, or
   *          <CODE>null</CODE> if the user could not be found in the
   *          directory.
   *
   * @throws  AccessDeniedException  If the user entry could not be found in the
   *                                 user directory.
   *
   * @throws  LDAPException  If there is a problem retrieving information from
   *                         the user directory.
   */
  private String[] getUserInfoFromDirectory(String userIdentifier)
          throws AccessDeniedException, LDAPException
  {
    logMessage("In getUserInfoFromDirectory(" + userIdentifier + ')');

    ArrayList<String> resourceList = new ArrayList<String>();
    String[][] resources = getProtectedResources();

    // First, retrieve the user entry from the directory.
    synchronized (connMutex)
    {
      String userDN = null;
      String filter = '(' + userIDAttribute + '=' + userIdentifier + ')';
      LDAPSearchResults results = userDirConn.search(userBaseDN,
                                                     LDAPConnection.SCOPE_SUB,
                                                     filter, roleAttrs, false);
      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          // We have an entry, so first get the user's DN
          LDAPEntry entry = (LDAPEntry) element;
          userDN = LDAPDN.normalize(entry.getDN().toLowerCase());


          // Next, check to see if this user is associated with any roles.
          LDAPAttribute attr = entry.getAttribute(Constants.ROLE_DN_AT);
          if (attr != null)
          {
            String[] values = attr.getStringValueArray();
            if ((values != null) && (values.length > 0))
            {
              for (int i=0; i < values.length; i++)
              {
                String roleDN = LDAPDN.normalize(values[i].toLowerCase());
                for (int j=0; j < resources.length; j++)
                {
                  if (roleDN.equals(resources[j][1]))
                  {
                    resourceList.add(resources[j][0]);
                    logMessage(userIdentifier + " allowed for role resource " +
                               resources[i][0]);
                    break;
                  }
                }
              }
            }
          }

          break;
        }
      }


      // If the user DN is null, then we couldn't find the user.
      if (userDN == null)
      {
        logMessage("Could not find user " + userIdentifier + " in directory");
        throw new AccessDeniedException("Could not find user " +
                                        userIdentifier +
                                        " in the user directory");
      }


      // Now look at the remaining resource DNs and see which apply to the user.
      for (int i=0; i < resources.length; i++)
      {
        // If the resource list already contains this resource name, then we
        // can go on to the next one.
        boolean matched = false;
        for (int j=0; j < resourceList.size(); j++)
        {
          if (resourceList.get(j).equals(resources[i][0]))
          {
            matched = true;
            break;
          }
        }


        // If we found a match in the resource list, then go to the next
        // protected resource.
        if (matched)
        {
          continue;
        }


        // See if this entry is a static group and the user is a member
        filter = "(|(&(objectclass=groupOfNames)(member=" + userDN +
                 "))(&(objectclass=groupOfUniqueNames)(uniqueMember=" + userDN +
                 ")))";
        try
        {
          results = userDirConn.search(resources[i][1],
                                       LDAPConnection.SCOPE_BASE, filter,
                                       noAttrs, false);
          while (results.hasMoreElements())
          {
            Object element = results.nextElement();
            if (element instanceof LDAPEntry)
            {
              resourceList.add(resources[i][0]);
              logMessage(userIdentifier +
                         " allowed for static group resource " +
                         resources[i][0]);
              matched = true;
              break;
            }
          }
        }
        catch (LDAPException le)
        {
          if (le.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT)
          {
            // If we know the group entry doesn't exist, then there's no need
            // to try to retrieve it for dynamic groups either.
            logMessage("Resource DN entry " + resources[i][1] +
                       " not found in the user directory");
            continue;
          }
          else
          {
            throw le;
          }
        }


        // If we found a match as a static group, then go to the next protected
        // resource.
        if (matched)
        {
          continue;
        }


        // See if this entry is a dynamic group and the user matches the
        // criteria.
        filter = "(objectclass=groupOfURLs)";
        results = userDirConn.search(resources[i][1],
                                     LDAPConnection.SCOPE_BASE, filter,
                                     memberURLAttrs, false);
        while (results.hasMoreElements())
        {
          Object element = results.nextElement();
          if (element instanceof LDAPEntry)
          {
            LDAPEntry entry = (LDAPEntry) element;
            LDAPAttribute attr = entry.getAttribute(Constants.MEMBER_URL_AT);
            if (attr != null)
            {
              String[] values = attr.getStringValueArray();
              if ((values != null) && (values.length > 0))
              {
                try
                {
                  LDAPUrl url = new LDAPUrl(values[0]);
                  String base = LDAPDN.normalize(url.getDN().toLowerCase());
                  if (userDN.endsWith(base))
                  {
                    filter = url.getFilter();
                    LDAPSearchResults results2 =
                         userDirConn.search(userDN, LDAPConnection.SCOPE_BASE,
                                            filter, noAttrs, false);
                    while (results2.hasMoreElements())
                    {
                      Object element2 = results2.nextElement();
                      if (element2 instanceof LDAPEntry)
                      {
                        resourceList.add(resources[i][0]);
                        logMessage(userIdentifier + " allowed for dynamic " +
                                   "group resource " + resources[i][0]);
                        break;
                      }
                    }
                  }
                } catch (Exception e) {}
              }
            }

            break;
          }
        }
      }
    }


    // Finally, convert the resource list to an array and return it.
    String[] resourceArray = new String[resourceList.size()];
    resourceList.toArray(resourceArray);
    return resourceArray;
  }



  /**
   * Attempts to authenticate the client based on the provided information.
   *
   * @param  authID           The authentication ID provided by the client.
   * @param  authCredentials  The credentials provided by the client.
   * @param  msgBuffer        The string buffer in which an explanation will be
   *                          placed in the event of a failure.
   *
   * @return  The result code from the authentication process.
   */
  public int authenticateClient(String authID, String authCredentials,
                                StringBuilder msgBuffer)
  {
    synchronized (connMutex)
    {
      // First, find the user's entry in the directory.
      String filter = '(' + userIDAttribute + '=' + authID + ')';

      LDAPSearchResults results;
      try
      {
        results = userDirConn.search(userBaseDN, LDAPConnection.SCOPE_SUB,
                                     filter, roleAttrs, false);
      }
      catch (LDAPException le)
      {
        msgBuffer.append("Unable to search user directory:  " + le);
        return Constants.MESSAGE_RESPONSE_SERVER_ERROR;
      }

      LDAPEntry userEntry = null;
      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          if (userEntry == null)
          {
            userEntry = (LDAPEntry) element;
          }
          else
          {
            msgBuffer.append("Multiple entries found matching filter " +
                             filter);
            return Constants.MESSAGE_RESPONSE_UNKNOWN_AUTH_ID;
          }
        }
      }

      if (userEntry == null)
      {
        msgBuffer.append("Unknown user \"" + authID + '"');
        return Constants.MESSAGE_RESPONSE_UNKNOWN_AUTH_ID;
      }


      // Next, bind as the user to verify the credentials.
      String userDN = userEntry.getDN();
      LDAPConnection bindConn;
      if (useSSL)
      {
        bindConn = new LDAPConnection(new JSSESocketFactory(null));
      }
      else
      {
        bindConn = new LDAPConnection();
      }

      try
      {
        bindConn.connect(userDirectoryHost, userDirectoryPort,
                         userDN, authCredentials);
      }
      catch (LDAPException le)
      {
        try
        {
          bindConn.disconnect();
        } catch (Exception e) {}

        int resultCode = le.getLDAPResultCode();
        if (resultCode == LDAPException.INVALID_CREDENTIALS)
        {
          msgBuffer.append("Invalid credentials");
          return Constants.MESSAGE_RESPONSE_INVALID_CREDENTIALS;
        }
        else
        {
          msgBuffer.append("Unable to verify user credentials:  " + le);
          return Constants.MESSAGE_RESPONSE_SERVER_ERROR;
        }
      }

      try
      {
        bindConn.disconnect();
      } catch (Exception e) {}


      // See if a group or role has been defined that specifies which clients
      // may authenticate.  If so, then verify that the client is a member of
      // that group or role.
      String authDN = AdminServlet.resourceDNAuthenticateClient;
      if ((authDN != null) && (authDN.length() > 0))
      {
        String normDN = LDAPDN.normalize(authDN);

        // First, check to see if the it is a role to which the user belongs.
        LDAPAttribute roleAttr = userEntry.getAttribute(Constants.ROLE_DN_AT);
        if (roleAttr != null)
        {
          String[] userRoles = roleAttr.getStringValueArray();
          for (int i=0; ((userRoles != null) && (i < userRoles.length)); i++)
          {
            if (normDN.equalsIgnoreCase(LDAPDN.normalize(userRoles[i])))
            {
              return Constants.MESSAGE_RESPONSE_SUCCESS;
            }
          }


          // See if this entry is a static group and the user is a member
          filter = "(|(&(objectclass=groupOfNames)(member=" + userDN +
                   "))(&(objectclass=groupOfUniqueNames)(uniqueMember=" +
                   userDN + ")))";
          try
          {
            results = userDirConn.search(authDN, LDAPConnection.SCOPE_BASE,
                                         filter, noAttrs, false);
            while (results.hasMoreElements())
            {
              Object element = results.nextElement();
              if (element instanceof LDAPEntry)
              {
                return Constants.MESSAGE_RESPONSE_SUCCESS;
              }
            }
          }
          catch (LDAPException le)
          {
            if (le.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT)
            {
              // If we know the group entry doesn't exist, then there's no need
              // to try to retrieve it for dynamic groups either.
              msgBuffer.append("Resource DN \"" + authDN +
                               "\" that specifies the clients that may " +
                               "authenticate does not exist in the user " +
                               "directory.");
              return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
            }
            else
            {
              msgBuffer.append("Unable to search the user directory for " +
                               "resource DN \"" + authDN + "\":  " + le);
              return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
            }
          }


          // See if the DN is a dynamic group and if so whether the user is a
          // member.
          String memberURL = null;
          filter = "(objectClass=groupOfURLs)";
          try
          {
            results = userDirConn.search(authDN, LDAPConnection.SCOPE_BASE,
                                         filter, memberURLAttrs, false);
            while (results.hasMoreElements())
            {
              Object element = results.nextElement();
              if (element instanceof LDAPEntry)
              {
                LDAPEntry entry = (LDAPEntry) element;
                LDAPAttribute urlAttr =
                     entry.getAttribute(Constants.MEMBER_URL_AT);
                if (urlAttr == null)
                {
                  msgBuffer.append("Unable to verify user \"" + authID +
                                   "\" as a member of \"" + authDN + '"');
                  return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
                }

                String[] values = urlAttr.getStringValueArray();
                if ((values == null) || (values.length != 1))
                {
                  msgBuffer.append("Unable to verify user \"" + authID +
                                   "\" as a member of \"" + authDN + '"');
                  return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
                }

                memberURL = values[0];
                break;
              }
            }
          }
          catch (LDAPException le)
          {
            msgBuffer.append("Unable to search the user directory for " +
                             "resource DN \"" + authDN + "\":  " + le);
            return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
          }

          if (memberURL == null)
          {
            msgBuffer.append("Unable to verify user \"" + authID +
                             "\" as a member of \"" + authDN + '"');
            return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
          }

          try
          {
            LDAPUrl url = new LDAPUrl(memberURL);
            String base = LDAPDN.normalize(url.getDN().toLowerCase());
            userDN = LDAPDN.normalize(userDN);
            if (userDN.endsWith(base))
            {
              filter = url.getFilter();
              LDAPSearchResults results2 =
                   userDirConn.search(userDN, LDAPConnection.SCOPE_BASE,
                                      filter, noAttrs, false);
              while (results2.hasMoreElements())
              {
                Object element2 = results2.nextElement();
                if (element2 instanceof LDAPEntry)
                {
                  return Constants.MESSAGE_RESPONSE_SUCCESS;
                }
              }
            }
          }
          catch (Exception e)
          {
            msgBuffer.append("Unable to verify user \"" + authID +
                             "\" as a member of \"" + authDN +
                             "\" -- " + e);
            return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
          }
        }

        msgBuffer.append("User \"" + authID + "\" is not authorized to " +
                         "connect to the SLAMD server as a client.");
        return Constants.MESSAGE_RESPONSE_CLIENT_REJECTED;
      }

      return Constants.MESSAGE_RESPONSE_SUCCESS;
    }
  }



  /**
   * Logs the specified message to the SLAMD log file using the access manager
   * debugging log level.  Note that if the SLAMD server is not specified, then
   * the message will not be logged.
   *
   * @param  message  The message to write to the SLAMD log.
   */
  private void logMessage(String message)
  {
    if (slamdServer != null)
    {
      slamdServer.logMessage(Constants.LOG_LEVEL_ACCESS_MANAGER_DEBUG, message);
    }
  }
}

