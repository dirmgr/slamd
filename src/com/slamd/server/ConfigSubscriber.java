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
package com.slamd.server;



import com.slamd.parameter.ParameterList;



/**
 * This interface specifies a method that can be used to dynamically cause SLAMD
 * components to refresh their configuration.  It should be implemented by any
 * SLAMD component that obtains configuration information through the
 * configuration database so that there is a way to make configuration changes
 * on the fly.  It is up to each component implementing this interface to decide
 * how best to handle any configuration changes.  Any component that is a
 * ConfigSubscriber also needs to register with the configuration database using
 * the <CODE>registerAsSubscriber</CODE> method.
 *
 *
 * @author   Neil A. Wilson
 */
public interface ConfigSubscriber
{
  /**
   * Retrieves the name that this configuration subscriber uses to identify
   * itself to the configuration handler.
   *
   * @return  The name that this configuration subscriber uses to identify
   *          itself to the configuration handler.
   */
  public String getSubscriberName();



  /**
   * Retrieves the set of configuration parameters associated with this
   * configuration subscriber.
   *
   * @return  The set of configuration parameters associated with this
   *          configuration subscriber.
   */
  public ParameterList getSubscriberParameters();



  /**
   * Indicates that the implementer should re-read all of its configuration
   * information from the configuration handler and make any necessary changes
   * to its behavior.
   *
   * @throws  SLAMDServerException  If there is a problem while making a
   *                                configuration change.
   */
  public void refreshSubscriberConfiguration()
         throws SLAMDServerException;



  /**
   * Indicates that the implementer should re-read the specified configuration
   * parameter from the configuration handler and make any necessary changes to
   * its behavior.
   *
   * @param  parameterName  The name of the configuration parameter for which
   *                        the configuration should be re-read.
   *
   * @throws  SLAMDServerException  If any of the configuration parameters do
   *                                not have acceptable values, or if a problem
   *                                occurs while making a configuration change.
   */
  public void refreshSubscriberConfiguration(String parameterName)
         throws SLAMDServerException;
}

