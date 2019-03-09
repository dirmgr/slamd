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



/**
 * This interface provides a mechanism by which various kinds of clients can be
 * notified when the client is disconnected from the server.
 *
 *
 * @author   Neil A. Wilson
 */
public interface ClientShutdownListener
{
  /**
   * Indicates that the client has disconnected from the server and that the
   * client may wish to take whatever action is appropriate.
   */
  public void clientDisconnected();
}

