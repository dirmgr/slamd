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
package com.slamd.realm;



import java.security.MessageDigest;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Hashtable;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPDN;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;
import netscape.ldap.LDAPUrl;
import netscape.ldap.factory.JSSESocketFactory;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

import com.slamd.asn1.ASN1Element;
import com.slamd.jobs.JSSEBlindTrustSocketFactory;



/**
 * This class implements a Tomcat Realm that allows authentication against an
 * LDAP directory server.  It overcomes many of the limitations of the
 * <CODE>JNDIRealm</CODE> implementation provided by default (e.g., this version
 * does not require the login ID attribute to be included in the DN), implements
 * caching (so that it's not necessary to query the directory every time a new
 * page is loaded), and provides support for verifying that the user is a member
 * of a specified static group, dynamic group, or role in addition to verifying
 * that the credentials are valid.
 * <BR><BR>
 * The configurable parameters for this realm are:
 * <UL>
 *   <LI>ldapHost -- The address of the directory server in which the user
 *       entries are stored.  This is required to have a value.</LI>
 *   <LI>ldapPort -- The port number of the directory server in which the user
 *       entries are stored.  This value must be between 1 and 65535.</LI>
 *   <LI>bindDN -- The DN that should be used to bind to the directory to find
 *       user entries and make membership determinations.  If left blank, then
 *       those operations will be performed anonymously.</LI>
 *   <LI>bindPassword -- The password for the bind DN.  If left blank, then the
 *       search and membership determination will be performed anonymously.</LI>
 *   <LI>userBase -- The DN of the base entry below which user entries exist in
 *       the directory server.  This is required to have a value.  If a
 *       membership DN is specified, then it does not need to be below this
 *       base.</LI>
 *   <LI>membershipDN -- The DN of a static group, dynamic group, or role in
 *       which a user must be a member in order to be authenticated.  If this
 *       is left blank, then no membership check will be performed (i.e., any
 *       user that is in the directory and provides a valid password will be
 *       granted access).</LI>
 *   <LI>loginIDAttribute -- The name of the LDAP attribute that will be queried
 *       to find user entries based on the value of the login ID provided.</LI>
 *   <LI>useSSL -- Indicates whether the communication with the LDAP directory
 *       server will use SSL.  The value must be either "true" or "false".</LI>
 *   <LI>blindTrust -- Indicates whether to blindly trust any SSL certificate
 *       presented by the directory server.  The value must be either "true" or
 *       "false".</LI>
 *   <LI>sslKeyStore -- The location of the JSSE key store file to use if SSL is
 *       enabled.</LI>
 *   <LI>sslKeyPassword -- The password to use when accessing the JSSE key store
 *       if SSL is enabled.</LI>
 *   <LI>sslTrustStore -- The location of the JSSE trust store file to use if
 *       SSL is enabled.</LI>
 *   <LI>sslTrustPassword -- The password to use when accessing the JSSE trust
 *       store if SSL is enabled.</LI>
 * </UL>
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPRealm
       extends RealmBase
{
  /**
   * The interval in milliseconds over which the cache needs to be cleaned.
   */
  public static final int CACHE_CLEANUP_INTERVAL = 300000;



  /**
   * The length of time in milliseconds that user information should stay in the
   * user cache before being reloaded from the directory.
   */
  public static final int CACHE_EXPIRATION_TIME = 1800000;



  /**
   * The membership type that will be used if the entry for the membership DN
   * has not yet been retrieved to determine the type of entry.
   */
  public static final int MEMBERSHIP_TYPE_UNKNOWN = -1;



  /**
   * The membership type that will be used if no membership determination is to
   * be made.
   */
  public static final int MEMBERSHIP_TYPE_NONE = 0;



  /**
   * The membership type that will be used if the user should be verified as a
   * member of a static group.
   */
  public static final int MEMBERSHIP_TYPE_STATIC = 1;



  /**
   * The membership type that will be used if the user should be verified as a
   * member of a dynamic group.
   */
  public static final int MEMBERSHIP_TYPE_DYNAMIC = 2;



  /**
   * The membership type that will be used if the user should be verified as a
   * member of a role.
   */
  public static final int MEMBERSHIP_TYPE_ROLE = 3;



  /**
   * The name of the LDAP attribute that holds the LDAP URL used to hold the
   * criteria for membership in a dynamic group.
   */
  public static final String MEMBER_URL_ATTRIBUTE = "memberURL";



  /**
   * The name of the LDAP attribute that contains the list of roles for which a
   * user is a member.
   */
  public static final String ROLE_ATTRIBUTE = "nsRole";



  /**
   * The system property used to specify the location of the JSSE key store.
   */
  public static final String SSL_KEY_STORE_PROPERTY =
       "javax.net.ssl.keyStore";



  /**
   * The system property used to specify the password for the JSSE key store.
   */
  public static final String SSL_KEY_PASSWORD_PROPERTY =
       "javax.net.ssl.keyStorePassword";



  /**
   * The system property used to specify the location of the JSSE trust store.
   */
  public static final String SSL_TRUST_STORE_PROPERTY =
       "javax.net.ssl.trustStore";



  /**
   * The system property used to specify the password for the JSSE trust store.
   */
  public static final String SSL_TRUST_PASSWORD_PROPERTY =
       "javax.net.ssl.trustStorePassword";



  /**
   * The set of attributes to return if only the role is needed from the user's
   * entry.
   */
  public static final String[] ROLE_ATTRS = new String[] { ROLE_ATTRIBUTE };



  /**
   * The set of attributes to return if no attributes are needed from the user's
   * entry.
   */
  public static final String[] NO_ATTRS = new String[] { "1.1" };



  /**
   * The flag that indicates whether SSL will be used to communicate with the
   * directory server.
   */
  protected boolean useSSL = false;



  /**
   * The flag that indicates whether to blindly trust any SSL certificate
   * presented by the directory server.
   */
  protected boolean blindTrust = true;



  /**
   * A hashtable containing cached credential information so that we don't have
   * to go to the directory server for every request.
   */
  protected Hashtable<String,CachedUser> userCache = null;



  /**
   * The DN that will be used to bind to the user directory to find user
   * accounts.
   */
  protected String bindDN = null;



  /**
   * The password for the bind DN.
   */
  protected String bindPassword = null;



  /**
   * The address to use when connecting to the user directory.
   */
  protected String ldapHost = null;



  /**
   * The port number that will be used to contact the user directory.
   */
  protected String ldapPort = "389";



  /**
   * The name of the LDAP attribute that will be used to find user entries based
   * on the provided user name.
   */
  protected String loginIDAttribute = "uid";



  /**
   * The DN of a static group, dynamic group, or role in which a user must be a
   * member in order to be successfully authenticated.
   */
  protected String membershipDN = null;



  /**
   * The password to use when accessing the JSSE key store.
   */
  protected String sslKeyPassword = null;



  /**
   * The location of the JSSE key store to use for SSL communication with the
   * directory.
   */
  protected String sslKeyStore = null;



  /**
   * The password to use when accessing the JSSE trust store.
   */
  protected String sslTrustPassword = null;



  /**
   * The location of the JSSE trust store to use for SSL communication with the
   * directory.
   */
  protected String sslTrustStore = null;



  /**
   * The DN under which user entries exist in the user directory.
   */
  protected String userBase = null;



  // The type of membership that should be checked when performing an
  // authentication.
  private int membershipType = MEMBERSHIP_TYPE_UNKNOWN;

  // The port number to use to connect to the directory server.
  private int port = 389;

  // The LDAP connection that will be used to bind to the user directory to
  // verify user credentials.
  private LDAPConnection bindConnection = null;

  // The LDAP connection that will be used to find user entries based on the
  // provided login ID.
  private LDAPConnection searchConnection = null;

  // The time at which the next cache cleanup should occur.
  private long nextCleanupTime;

  // The message digest used to generate SHA-1 hashes.
  private MessageDigest shaDigest = null;

  // The search base from the LDAP URL that will be used to verify membership in
  // a dynamic group.
  private String membershipURLBase = null;

  // The filter from the LDAP URL that will be used to verify membership in a
  // dynamic group.
  private String membershipURLFilter = null;



  /**
   * Retrieves the DN that will be used to connect to the user directory to find
   * user entries.
   *
   * @return  The DN that will be used to connect to the user directory to find
   *           user entries.
   */
  public String getBindDN()
  {
    return bindDN;
  }



  /**
   * Specifies the DN that will be used to connect to the user directory to find
   * user entries.
   *
   * @param  bindDN  The DN that will be used to connect to the user directory
   *                 to find user entries.
   */
  public void setBindDN(String bindDN)
  {
    this.bindDN = bindDN;
  }



  /**
   * Retrieves the password to use for the bind DN.
   *
   * @return  The password to use for the bind DN.
   */
  public String getBindPassword()
  {
    return bindPassword;
  }



  /**
   * Specifies the password to use for the bind DN.
   *
   * @param  bindPassword  THe password to use for the bind DN.
   */
  public void setBindPassword(String bindPassword)
  {
    this.bindPassword = bindPassword;
  }



  /**
   * Retrieves the address to use for the user directory server.
   *
   * @return  The address to use for the user directory server.
   */
  public String getLdapHost()
  {
    return ldapHost;
  }



  /**
   * Specifies the address to use for the user directory server.
   *
   * @param  ldapHost  The address to use for the user directory server.
   */
  public void setLdapHost(String ldapHost)
  {
    this.ldapHost = ldapHost;
  }



  /**
   * Retrieves the port number for the user directory server.
   *
   * @return  The port number for the user directory server.
   */
  public int getLdapPort()
  {
    return port;
  }



  /**
   * Specifies the port number for the user directory server.
   *
   * @param  ldapPort  The port number for the user directory server.
   */
  public void setLdapPort(int ldapPort)
  {
    this.port     = ldapPort;
    this.ldapPort = String.valueOf(ldapPort);
  }



  /**
   * Specifies the port number for the user directory server.
   *
   * @param  ldapPort  The port number for the user directory server.
   */
  public void setLdapPort(String ldapPort)
  {
    try
    {
      this.port     = Integer.parseInt(ldapPort);
      this.ldapPort = ldapPort;
    } catch (Exception e) {}
  }



  /**
   * Retrieves the name of the LDAP attribute that will be used to find user
   * entries based on the provided user name.
   *
   * @return  The name of the LDAP attribute that will be used to find user
   *          entries based on the provided user name.
   */
  public String getLoginIDAttribute()
  {
    return loginIDAttribute;
  }



  /**
   * Specifies the name of the LDAP attribute that will be used to find user
   * entries based on the provided user name.
   *
   * @param  loginIDAttribute  The name of the LDAP attribute that will be used
   *                           to find user entries based on the provided user
   *                           name.
   */
  public void setLoginIDAttribute(String loginIDAttribute)
  {
    this.loginIDAttribute = loginIDAttribute;
  }



  /**
   * Retrieves the location in the user directory under which user entries may
   * be found.
   *
   * @return  The location in the user directory under which user entries may be
   *          found.
   */
  public String getUserBase()
  {
    return userBase;
  }



  /**
   * Specifies the location in the user directory under which user entries may
   * be found.
   *
   * @param  userBase  The location in the user directory under which user
   *                   entries may be found.
   */
  public void setUserBase(String userBase)
  {
    this.userBase = userBase;
  }



  /**
   * Retrieves the DN of the static group, dynamic group, or role entry for
   * which a user must be a member in order to be authenticated.
   *
   * @return  The DN of the static group, dynamic group, or role entry for which
   *          a user must be a member in order to be authenticated.
   */
  public String getMembershipDN()
  {
    return membershipDN;
  }



  /**
   * Specifies the DN of the static group, dynamic group, or role entry for
   * which a user must be a member in order to be authenticated.
   *
   * @param  membershipDN  The DN of the static group, dynamic group, or role
   *                       entry for which a user must be a member in order to
   *                       be authenticated.
   */
  public void setMembershipDN(String membershipDN)
  {
    this.membershipDN = membershipDN;
    membershipType = MEMBERSHIP_TYPE_UNKNOWN;
  }



  /**
   * Indicates whether SSL will be used to communicate with the directory
   * server.
   *
   * @return  <CODE>true</CODE> if SSL will be used to communicate with the
   *          directory server, or <CODE>false</CODE> if not.
   */
  public boolean getUseSSL()
  {
    return useSSL;
  }



  /**
   * Specifies whether to use SSL to communicate with the directory server.  If
   * SSL is to be used, then the value specified must be "true".
   *
   * @param  useSSL  The string that specifies whether to use SSL to communicate
   *                 with the directory server.
   */
  public void setUseSSL(String useSSL)
  {
    this.useSSL = useSSL.equalsIgnoreCase("true");
  }



  /**
   * Indicates whether to blindly trust any SSL certificate presented by the
   * directory server.
   *
   * @return  <CODE>true</CODE> if the certificate should be blindly trusted, or
   *          <CODE>false</CODE> if not.
   */
  public boolean getBlindTrust()
  {
    return blindTrust;
  }



  /**
   * Specifies whether to blindly trust any SSL certificate presented by the
   * directory server.
   *
   * @param  blindTrust  The string that specifies whether to blindly trust any
   *                     SSL certificate.
   */
  public void setBlindTrust(String blindTrust)
  {
    this.blindTrust = blindTrust.equalsIgnoreCase("true");
  }



  /**
   * Specifies the location of the JSSE key store that is to be used for SSL
   * communication.
   *
   * @param  sslKeyStore  The location of the JSSE key store that is to be used
   *                      for SSL communication.
   */
  public void setSslKeyStore(String sslKeyStore)
  {
    this.sslKeyStore = sslKeyStore;
    if ((sslKeyStore != null) && (sslKeyStore.length() > 0))
    {
      System.setProperty(SSL_KEY_STORE_PROPERTY, sslKeyStore);
    }
  }



  /**
   * Retrieves the location of the JSSE key store that is to be used for SSL
   * communication.
   *
   * @return  The location of the JSSE key store that is to be used for SSL
   *          communication.
   */
  public String getSslKeyStore()
  {
    return sslKeyStore;
  }



  /**
   * Specifies the password used to access the JSSE key store.
   *
   * @param  sslKeyPassword  The password used to access the JSSE key store.
   */
  public void setSslKeyPassword(String sslKeyPassword)
  {
    this.sslKeyPassword = sslKeyPassword;
    if ((sslKeyPassword != null) && (sslKeyPassword.length() > 0))
    {
      System.setProperty(SSL_KEY_PASSWORD_PROPERTY, sslKeyPassword);
    }
  }



  /**
   * Retrieves the password that will be used to access the JSSE key store.
   *
   * @return  The password that will be used to access the JSSE key store.
   */
  public String getSslKeyPassword()
  {
    return sslKeyPassword;
  }



  /**
   * Specifies the location of the JSSE trust store that is to be used for SSL
   * communication.
   *
   * @param  sslTrustStore  The location of the JSSE trust store that is to be
   *                        used for SSL communication.
   */
  public void setSslTrustStore(String sslTrustStore)
  {
    this.sslTrustStore = sslTrustStore;
    if ((sslTrustStore != null) && (sslTrustStore.length() > 0))
    {
      System.setProperty(SSL_TRUST_STORE_PROPERTY, sslTrustStore);
    }
  }



  /**
   * Retrieves the location of the JSSE trust store that is to be used for SSL
   * communication.
   *
   * @return  The location of the JSSE trust store that is to be used for SSL
   *          communication.
   */
  public String getSslTrustStore()
  {
    return sslTrustStore;
  }



  /**
   * Specifies the password used to access the JSSE trust store.
   *
   * @param  sslTrustPassword  The password used to access the JSSE trust store.
   */
  public void setSslTrustPassword(String sslTrustPassword)
  {
    this.sslTrustPassword = sslTrustPassword;
    if ((sslTrustPassword != null) && (sslTrustPassword.length() > 0))
    {
      System.setProperty(SSL_TRUST_PASSWORD_PROPERTY, sslTrustPassword);
    }
  }



  /**
   * Retrieves the password that will be used to access the JSSE trust store.
   *
   * @return  The password that will be used to access the JSSE trust store.
   */
  public String getSslTrustPassword()
  {
    return sslTrustPassword;
  }



  /**
   * Retrieves the Principal associated with the specified username and
   * credentials.  If no user could be found with the specified username, or if
   * the provided credentials are invalid, then <CODE>null</CODE> will be
   * returned.
   *
   * @param  username     The provided username that will be used to find the
   *                      user entry.
   * @param  credentials  The credentials that will be used to authenticate the
   *                      user.
   *
   * @return  The Principal associated with the specified username and
   *          credentials.
   */
  @Override()
  public synchronized Principal authenticate(String username,
                                             String credentials)
  {
    // First, see if the user cache exists.  If not, then create it.  If so,
    // then see if it needs to be pruned.
    long now = System.currentTimeMillis();
    if (userCache == null)
    {
      userCache = new Hashtable<String,CachedUser>();
      nextCleanupTime = now + CACHE_CLEANUP_INTERVAL;
    }
    else
    {
      if (now > nextCleanupTime)
      {
        cleanUserCache();
      }
    }


    // First, make sure that the provided credentials are not empty.  If they
    // are, then we cannot verify the user's identity.
    if ((credentials == null) || (credentials.length() == 0))
    {
      return null;
    }


    // Generate a SHA-1 hash of the password.
    byte[] hashedPassword = hashPassword(credentials);
    if (hashedPassword == null)
    {
      return null;
    }


    // See if the cache contains information about the user.
    CachedUser cachedUser = userCache.get(username);
    if (cachedUser != null)
    {
      if (! byteArraysAreEqual(hashedPassword, cachedUser.getHashedPassword()))
      {
        return null;
      }
      else
      {
        if (now >= cachedUser.getExpirationTime())
        {
          userCache.remove(username);
        }
        else
        {
          return cachedUser.getUserPrincipal();
        }
      }
    }

    // The user wasn't in the cache, so perform a search in the directory to
    // find the user's entry.
    LDAPEntry userEntry = getUserEntry(username);
    if (userEntry == null)
    {
      return null;
    }

    // Verify the user's credentials.
    String userDN = userEntry.getDN();
    if (! credentialsAreValid(userDN, credentials))
    {
      return null;
    }

    // See if we need to check membership for the user.
    if (membershipType == MEMBERSHIP_TYPE_UNKNOWN)
    {
      determineMembershipType();
    }
    if (membershipType != MEMBERSHIP_TYPE_NONE)
    {
      if (! isMember(userEntry))
      {
        return null;
      }
    }

    // The username and credentials are valid, so return the Principal
    // associated with the user.
    Principal userPrincipal = new GenericPrincipal(this, username, credentials);
    long expirationTime = now + CACHE_EXPIRATION_TIME;
    cachedUser = new CachedUser(username, userDN, hashedPassword, userPrincipal,
                                expirationTime);
    userCache.put(username, cachedUser);
    return userPrincipal;
  }



  /**
   * Retrieves the DN of the user with the specified username.
   *
   * @param  username  The username provided by the user that is to be used to
   *                   find the user's entry.
   *
   * @return  The DN for the user with the specified user name, or
   *          <CODE>null</CODE> if the user DN could not be determined.
   */
  private LDAPEntry getUserEntry(String username)
  {
    if (searchConnection == null)
    {
      // Convert the port string to a number.
      try
      {
        port = Integer.parseInt(ldapPort);
        if ((port < 1) || (port > 65535))
        {
          log("The port number must be between 1 and 65535.");
          return null;
        }
      }
      catch (NumberFormatException nfe)
      {
        log("Cannot interpret " + ldapPort + " as an integer value.");
        return null;
      }

      // Establish the connection to use to find user entries.
      try
      {
        if (useSSL)
        {
          if (blindTrust)
          {
            JSSEBlindTrustSocketFactory socketFactory =
                 new JSSEBlindTrustSocketFactory();
            searchConnection = new LDAPConnection(socketFactory);
          }
          else
          {
            searchConnection = new LDAPConnection(new JSSESocketFactory(null));
          }
        }
        else
        {
          searchConnection = new LDAPConnection();
        }
        searchConnection.connect(3, ldapHost, port, bindDN, bindPassword);
      }
      catch (LDAPException le)
      {
        log("Could not establish the search connection:  " + le);
        searchConnection = null;
        return null;
      }
    }


    // Perform a search to find the user's entry.
    try
    {
      String filter = '(' + loginIDAttribute + '=' + username + ')';
      LDAPSearchResults results =
           searchConnection.search(userBase, LDAPConnection.SCOPE_SUB,
                                   filter, ROLE_ATTRS, false);
      while (results.hasMoreElements())
      {
        Object element = results.nextElement();
        if (element instanceof LDAPEntry)
        {
          return (LDAPEntry) element;
        }
      }
    }
    catch (LDAPException le)
    {
      log("Could not perform a search in the user directory:  " + le);
      try
      {
        searchConnection.disconnect();
      } catch (Exception e) {}
      searchConnection = null;
    }

    // If we have gotten here, then we could not determine the DN.
    return null;
  }



  /**
   * Indicates whether the provided credentials are valid for the user with the
   * specified DN.
   *
   * @param  userDN       The DN of the user for which to perform the bind.
   * @param  credentials  The password to use when binding as the specified
   *                      user.
   *
   * @return  <CODE>true</CODE> if the user's credentials are valid, or
   *          <CODE>false</CODE> if they are not or if the validity could not be
   *          verified.
   */
  private boolean credentialsAreValid(String userDN, String credentials)
  {
    if (bindConnection == null)
    {
      try
      {
        port = Integer.parseInt(ldapPort);
        if ((port < 1) || (port > 65535))
        {
          log("The port number must be between 1 and 65535.");
          return false;
        }
      }
      catch (NumberFormatException nfe)
      {
        log("Cannot interpret " + ldapPort + " as an integer value.");
        return false;
      }

      try
      {
        if (useSSL)
        {
          if (blindTrust)
          {
            JSSEBlindTrustSocketFactory socketFactory =
                 new JSSEBlindTrustSocketFactory();
            bindConnection = new LDAPConnection(socketFactory);
          }
          else
          {
            bindConnection = new LDAPConnection(new JSSESocketFactory(null));
          }
        }
        else
        {
          bindConnection = new LDAPConnection();
        }
        bindConnection.connect(ldapHost, port);
      }
      catch (LDAPException le)
      {
        log("Could not establish the bind connection:  " + le);
        bindConnection = null;
        return false;
      }
    }


    // Perform a bind as the user to verify the credentials.
    try
    {
      bindConnection.bind(3, userDN, credentials);
      return true;
    }
    catch (LDAPException le)
    {
      switch (le.getLDAPResultCode())
      {
        case LDAPException.NO_SUCH_OBJECT:
        case LDAPException.INVALID_CREDENTIALS:
        case LDAPException.INAPPROPRIATE_AUTHENTICATION:
        case LDAPException.CONSTRAINT_VIOLATION:
             break;
        default:
             log("Could not perform the bind:  " + le);
             try
             {
               bindConnection.disconnect();
             } catch (Exception e) {}
             bindConnection = null;
      }

      return false;
    }
  }



  /**
   * Generates a SHA-1 hash of the provided password.  The password will not be
   * salted, but storing hashed passwords in memory is still preferable to
   * storing them in the clear.
   *
   * @param  credentials  The password provided by the end user.
   *
   * @return  The hashed version of the password, or <CODE>null</CODE> if a
   *          problem occurs.
   */
  private byte[] hashPassword(String credentials)
  {
    if (shaDigest == null)
    {
      try
      {
        shaDigest = MessageDigest.getInstance("SHA");
      }
      catch (Exception e)
      {
        log("Unable to obtain SHA digest implementation.");
        return null;
      }
    }

    return shaDigest.digest(ASN1Element.getBytes(credentials));
  }



  /**
   * Determines whether the contents of the two provided byte arrays are
   * identical.
   *
   * @param  b1  The first byte array for which to make the determination.
   * @param  b2  The second byte array for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the contents of the provided byte arrays are
   *          equal, or <CODE>false</CODE> if not.
   */
  private static boolean byteArraysAreEqual(byte[] b1, byte[] b2)
  {
    if ((b1 == null) || (b2 == null) || (b1.length != b2.length))
    {
      return false;
    }

    for (int i=0; i < b1.length; i++)
    {
      if (b1[i] != b2[i])
      {
        return false;
      }
    }

    return true;
  }



  /**
   * Determines the type of entry that is specified in the membership DN.  That
   * is, it determines whether the membership DN refers to a static group, a
   * dynamic group, or a role.
   */
  private void determineMembershipType()
  {
    // If no membership DN was provided, then we don't need to check any kind
    // of membership.
    if ((membershipDN == null) || (membershipDN.length() == 0))
    {
      membershipType = MEMBERSHIP_TYPE_NONE;
      return;
    }


    // If a DN was provided, then retrieve the entry associated with it.
    String[] attrsToReturn = new String[]
    {
      "objectClass",
      MEMBER_URL_ATTRIBUTE
    };
    LDAPEntry membershipEntry = null;
    try
    {
      membershipEntry = searchConnection.read(membershipDN, attrsToReturn);
    }
    catch (LDAPException le)
    {
      log("Unable to retrieve membership entry " + membershipDN + ":  " +
          le);
      membershipType = MEMBERSHIP_TYPE_UNKNOWN;
      return;
    }


    // Retrieve the objectClass values from the entry.
    LDAPAttribute attr = membershipEntry.getAttribute("objectClass");
    if (attr == null)
    {
      log("Unable to retrieve objectClass values from entry " + membershipDN);
      membershipType = MEMBERSHIP_TYPE_UNKNOWN;
      return;
    }
    String[] values = attr.getStringValueArray();
    if ((values == null) || (values.length == 0))
    {
      log("Unable to retrieve objectClass values from entry " + membershipDN);
      membershipType = MEMBERSHIP_TYPE_UNKNOWN;
      return;
    }

    // Look at the objectClass values to try to determine the entry type.
    membershipType = MEMBERSHIP_TYPE_ROLE;
    for (int i=0; i < values.length; i++)
    {
      String lowerValue = values[i].toLowerCase();
      if (lowerValue.equals("groupofnames") ||
          lowerValue.equals("groupofuniquenames"))
      {
        // It is a static group.  We don't need to do anything else, so return.
        membershipType = MEMBERSHIP_TYPE_STATIC;
        return;
      }
      else if (lowerValue.equals("groupofurls"))
      {
        // It is a dynamic group.  Get the member URL and extract the search
        // base and filter from it.
        attr = membershipEntry.getAttribute(MEMBER_URL_ATTRIBUTE);
        if (attr == null)
        {
          log("Unable to retrieve " + MEMBER_URL_ATTRIBUTE +
              " attribute from groupOfURLs entry " + membershipDN);
          membershipType = MEMBERSHIP_TYPE_UNKNOWN;
          return;
        }
        values = attr.getStringValueArray();
        if ((values == null) || (values.length == 0))
        {
          log("Unable to retrieve " + MEMBER_URL_ATTRIBUTE +
              " attribute from groupOfURLs entry " + membershipDN);
          membershipType = MEMBERSHIP_TYPE_UNKNOWN;
          return;
        }

        try
        {
          LDAPUrl url = new LDAPUrl(values[1]);
          membershipURLBase   = url.getDN();
          membershipURLFilter = url.getFilter();
          membershipType = MEMBERSHIP_TYPE_DYNAMIC;
          return;
        }
        catch (Exception e)
        {
          log("Unable to parse value '" + values[1] + "' as an LDAP URL.");
          membershipType = MEMBERSHIP_TYPE_UNKNOWN;
          return;
        }
      }
    }
  }



  /**
   * Determines whether the provided user is a member of the membership DN.
   *
   * @param  userEntry  The user entry for which to make the determination.
   *
   * @return  <CODE>true</CODE> if it is determined that the user is a member,
   *          or <CODE>false</CODE> if the membership could not be determined.
   */
  private boolean isMember(LDAPEntry userEntry)
  {
    switch (membershipType)
    {
      case MEMBERSHIP_TYPE_STATIC:
        String userDN = userEntry.getDN();
        String filter = "(|(&(objectclass=groupOfNames)(member=" + userDN +
                        "))(&(objectClass=groupOfUniqueNames)(uniqueMember=" +
                        userDN + ")))";
        try
        {
          LDAPSearchResults results =
               searchConnection.search(membershipDN, LDAPConnection.SCOPE_BASE,
                                       filter, NO_ATTRS, false);
          while (results.hasMoreElements())
          {
            Object element = results.nextElement();
            if (element instanceof LDAPEntry)
            {
              return true;
            }
          }

          return false;
        }
        catch (Exception e)
        {
          return false;
        }
      case MEMBERSHIP_TYPE_DYNAMIC:
        userDN = LDAPDN.normalize(userEntry.getDN());
        if (! userDN.endsWith(membershipURLBase))
        {
          return false;
        }

        try
        {
          LDAPSearchResults results =
               searchConnection.search(userDN, LDAPConnection.SCOPE_BASE,
                                       membershipURLFilter, NO_ATTRS, false);
          while (results.hasMoreElements())
          {
            Object element = results.nextElement();
            if (element instanceof LDAPEntry)
            {
              return true;
            }
          }

          return false;
        }
        catch (Exception e)
        {
          return false;
        }
      case MEMBERSHIP_TYPE_ROLE:
        LDAPAttribute roleAttr = userEntry.getAttribute(ROLE_ATTRIBUTE);
        String[] roleValues = null;
        if ((roleAttr == null) ||
            ((roleValues = roleAttr.getStringValueArray()) == null) ||
            (roleValues.length == 0))
        {
          return false;
        }

        for (int i=0; i < roleValues.length; i++)
        {
          String roleDN = LDAPDN.normalize(roleValues[i]);
          if (roleDN.equals(membershipDN))
          {
            return true;
          }
        }

        return false;
    }


    return false;
  }



  /**
   * This method iterates through the user cache and removes all cached user
   * information that has expired.
   */
  private void cleanUserCache()
  {
    long now = System.currentTimeMillis();

    Enumeration keys = userCache.keys();
    while (keys.hasMoreElements())
    {
      String key = (String) keys.nextElement();
      CachedUser user = userCache.get(key);
      if (user.getExpirationTime() < now)
      {
        userCache.remove(key);
      }
    }

    nextCleanupTime = now + CACHE_CLEANUP_INTERVAL;
  }



  /**
   * Shuts down this realm and releases the resources associated with it.
   */
  public void stop()
  {
    // Invoke the superclass stop() method.
    try
    {
      super.stop();
    } catch (Exception e) {}

    // Close the search connection.
    try
    {
      searchConnection.disconnect();
    } catch (LDAPException le) {}

    // Close the bind connection.
    try
    {
      bindConnection.disconnect();
    } catch (LDAPException le) {}
  }



  /**
   * Retrieves a short name for this Realm implementation, for use in log
   * messages.
   *
   * @return  A short name for this Realm implementation, for use in log
   *          messages.
   */
  @Override()
  protected String getName()
  {
    return "LDAPRealm";
  }



  /**
   * Retrieves the password associated with the given principal's user name.
   *
   * @param  username  The name of the user for which to retrieve the password.
   *
   * @return  The password associated with the given principal's user name.
   */
  @Override()
  protected String getPassword(String username)
  {
    return null;
  }



  /**
   * Retrieves the Principal associated with the given user name.
   *
   * @param  username  The name of the user for which to retrieve the Principal.
   *
   * @return  The Principal associated with the given user name.
   */
  @Override()
  protected Principal getPrincipal(String username)
  {
    return null;
  }
}

