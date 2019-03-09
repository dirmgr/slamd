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
package com.slamd.client;



import com.slamd.common.SLAMDException;



/**
 * This class defines an exception that may be thrown if a problem is
 * encountered while performing some operation in the client.
 *
 *
 * @author   Neil A. Wilson
 */
public final class ClientException
       extends SLAMDException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -7618061048848245382L;



  // Indicates whether the client may continue to be used.
  private final boolean stillAvailable;



  /**
   * Creates a new SLAMD client exception with the specified message.
   *
   * @param  message         A message providing more information about the
   *                         exception.
   * @param  stillAvailable  Indicate whether the client may continue to be
   *                         used.
   */
  public ClientException(final String message, final boolean stillAvailable)
  {
    super(message);

    this.stillAvailable = stillAvailable;
  }



  /**
   * Creates a new SLAMD client exception with the specified message and parent
   * exception.
   *
   * @param  message         A message providing more information about the
   *                         exception.
   * @param  stillAvailable  Indicate whether the client may continue to be
   *                         used.
   * @param  cause           The parent exception that triggered this client
   *                         exception.
   */
  public ClientException(final String message, final boolean stillAvailable,
                         final Throwable cause)
  {
    super(message, cause);

    this.stillAvailable = stillAvailable;
  }



  /**
   * Indicates whether the client may continue to be used.
   *
   * @return  {@code true} if the client may continue to be used, or
   *          {@code false} if not.
   */
  public boolean stillAvailable()
  {
    return stillAvailable;
  }
}

