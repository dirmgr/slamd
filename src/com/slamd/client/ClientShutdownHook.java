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



import com.slamd.common.Constants;
import com.slamd.message.StatusResponseMessage;



/**
 * This class defines a shutdown hook that will be registered with the SLAMD
 * client once it has been successfully created.  This shutdown hook will be
 * invoked whenever the client is shutting down in an attempt to notify the
 * server of the shutdown and close the connection gracefully.  Note that the
 * use of shutdown hooks requires a Java version of at least 1.3.
 *
 *
 * @author   Neil A. Wilson
 */
public class ClientShutdownHook
       extends Thread
{
  // The SLAMD client with which this shutdown hook is registered.
  private final Client client;



  /**
   * Creates a client shutdown hook that is associated with the provided SLAMD
   * client.
   *
   * @param  client  The SLAMD client with which this shutdown hook is
   *                 associated.
   */
  public ClientShutdownHook(Client client)
  {
    setName("Client Shutdown Hook");
    this.client = client;
  }



  /**
   * Sends a status response message to the server that indicates the SLAMD
   * client is shutting down.  If an instance of this class is registered as a
   * shutdown hook for the client, then this method will be invoked as part of
   * the shutdown process, in which case it will attempt to notify the server of
   * the shutdown and close the connection.
   */
  @Override()
  public void run()
  {
    try
    {
      StatusResponseMessage msg =
           new StatusResponseMessage(client.getMessageID(),
                    Constants.MESSAGE_RESPONSE_CLIENT_SHUTDOWN,
                    Constants.CLIENT_STATE_SHUTTING_DOWN,
                    "The SLAMD client is shutting down");
      client.writer.writeElement(msg.encode());
      client.clientSocket.close();
    }
    catch (Exception e)
    {
      // We're already shutting down.  Nothing we can really do about an
      // exception here.
    }
  }
}

