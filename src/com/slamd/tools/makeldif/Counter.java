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
 * This class defines a numeric counter that can be used to retrieve sequential
 * values through subsequent calls to the <CODE>getNext</CODE> method.
 *
 *
 * @author   Neil A. Wilson
 */
public class Counter
{
  // The next numeric value that will be retrieved by getNext
  private int numericValue;



  /**
   * Creates a new counter with a starting point of zero.
   */
  public Counter()
  {
    this(0);
  }



  /**
   * Creates a new counter with the specified starting point.
   *
   * @param  startingPoint  The first value that will be retrieved by this
   *                        counter.  Additional values retrieved will increase
   *                        sequentially.
   */
  public Counter(int startingPoint)
  {
    numericValue = startingPoint;
  }



  /**
   * Retrieves the next value to use for this counter.
   *
   * @return  The next value to use for this counter.
   */
  public int getNext()
  {
    return numericValue++;
  }
}

