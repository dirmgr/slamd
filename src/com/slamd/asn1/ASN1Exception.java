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
package com.slamd.asn1;



/**
 * This class defines an exception that may be thrown if there is a problem
 * working with an ASN.1 element.
 *
 *
 * @author   Neil A. Wilson
 */
public class ASN1Exception
       extends Exception
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -5504616318565952905L;



  /**
   * Creates a new ASN.1 exception with the specified message.
   *
   * @param  message  The message associated with this exception.
   */
  public ASN1Exception(String message)
  {
    super(message);
  }



  /**
   * Creates a new ASN.1 exception with the specified message and parent
   * exception.
   *
   * @param  message  The message associated with this exception.
   * @param  cause    The parent exception that triggered this ASN.1 exception.
   */
  public ASN1Exception(String message, Throwable cause)
  {
    super(message, cause);
  }
}

