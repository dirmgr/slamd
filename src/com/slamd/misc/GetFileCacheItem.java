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
package com.slamd.misc;



import java.util.Random;



/**
 * This class defines a state item that is used to store information about a
 * file that can be retrieved by the <CODE>GetFile</CODE> servlet.
 *
 *
 * @author   Neil A. Wilson
 */
public class GetFileCacheItem
{
  // The current counter for this cache item.
  private int counter;

  // The total number of files in this cache set.
  private final int numFiles;

  // The random number generator that will be used to choose a file at random.
  private final Random random;

  // The base name of this file to which the counter will be applied.
  private final String baseName;



  /**
   * Creates a new cache item for a file with the specified base name and total
   * number of files.
   *
   * @param  baseName  The base name for the file with which this cache item is
   *                   associated.
   * @param  numFiles  The total number of files available with the specified
   *                   base name.
   */
  public GetFileCacheItem(String baseName, int numFiles)
  {
    this.baseName = baseName;
    this.numFiles = numFiles;

    counter = 1;
    random  = new Random();
  }



  /**
   * Retrieves the name of the next file that should be retrieved.  It will be
   * the base name followed by a period and a number.  In addition, the counter
   * will be incremented.
   *
   * @return  The name of the next file that should be retrieved.
   */
  public synchronized String nextFileName()
  {
    String returnStr = baseName + '.' + counter++;

    if (counter > numFiles)
    {
      counter = 1;
    }

    return returnStr;
  }



  /**
   * Retrieves the name of a random file that should be retrieved.  It will be
   * the base name followed by a period and a randomly-chosen number (between
   * 1 and numFiles, inclusive).
   *
   * @return  The name of the randomly-chosen file that should be retrieved.
   */
  public String randomFileName()
  {
    return baseName + '.' + (random.nextInt(numFiles) + 1);
  }
}

