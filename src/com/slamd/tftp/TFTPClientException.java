/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Geoffrey Said.
 * Portions created by Geoffrey Said are Copyright (C) 2006.
 * All Rights Reserved.
 *
 * Contributor(s):  Geoffrey Said
 */
package com.slamd.tftp;



import java.io.IOException;



/**
 * Defines a custom exception that can be thrown by the TFTP client when
 * various I/O errors are encountered.
 *
 * @author    2X Geoffrey Said
 */
public class TFTPClientException extends IOException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 6041674093035366586L;



  /**
   * Default constructor that takes an error message as a parameter.
   * It calls the super class constructor and passes the message to it.
   *
   * @param  message  an error message to display when the exception is
   *                  handled.
   */
  public TFTPClientException(String message)
  {
      super(message);
  }
}

