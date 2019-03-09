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
 * This interface provides a mechanism by which various kinds of clients (e.g.,
 * command line clients, GUI clients, etc.) can display information about what
 * a client is doing in an appropriate manner.
 *
 *
 * @author   Neil A. Wilson
 */
public interface ClientMessageWriter
{
  /**
   * Writes the specified message to the appropriate location.
   *
   * @param  message  The message to be written.
   */
  public void writeMessage(String message);



  /**
   * Writes the specified message to the appropriate location, provided that
   * the client is operating in verbose mode.
   *
   * @param  message  The message to be written.
   */
  public void writeVerbose(String message);



  /**
   * Indicates whether the message writer is using verbose mode and therefore
   * will display messages written with the <CODE>writeVerbose</CODE> method.
   *
   * @return  <CODE>true</CODE> if the message writer is using verbose mode, or
   *          <CODE>false</CODE> if not.
   */
  public boolean usingVerboseMode();
}

