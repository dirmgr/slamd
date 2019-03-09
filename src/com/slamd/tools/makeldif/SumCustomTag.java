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
package com.slamd.tools.makeldif;



/**
 * This class provides a very simple implementation of a custom tag that
 * calculates the sum of all the arguments provided to it.
 *
 *
 * @author   Neil A. Wilson
 */
public class SumCustomTag
       extends CustomTag
{
  /**
   * Performs any necessary one-time initialization that should be performed
   * when this custom tag is first created.  In this case, no initialization is
   * performed.
   */
  public void initialize()
  {
    // No implementation required.
  }



  /**
   * Performs any initialization that should be performed each time the LDIF
   * generation starts working on a new branch (e.g., to reset any internal
   * variables that might have been in use).  In this case, no reinitialization
   * is performed.
   */
  public void reinitialize()
  {
    // No implementation required.
  }



  /**
   * Parses the list of arguments, converts the values to integers, and totals
   * those values.
   *
   * @param  tagArguments  The arguments containing the numeric values to be
   *                       totaled.
   *
   * @return  The string representation of the total of all the argument values.
   */
  public String generateOutput(String[] tagArguments)
  {
    int sum = 0;

    for (int i=0; i < tagArguments.length; i++)
    {
      sum += Integer.parseInt(tagArguments[i]);
    }

    return String.valueOf(sum);
  }
}

