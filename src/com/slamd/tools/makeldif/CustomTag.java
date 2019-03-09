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
 * This class defines the framework that should be used to develop custom tags
 * for use with MakeLDIF.
 *
 *
 * @author   Neil A. Wilson
 */
public abstract class CustomTag
{
  /**
   * Performs any necessary one-time initialization that should be performed
   * when this custom tag is first created.  This is optional, and by default
   * no initialization is performed.
   */
  public void initialize()
  {
    // No implementation required.
  }



  /**
   * Performs any initialization that should be performed each time the LDIF
   * generation starts working on a new branch (e.g., to reset any internal
   * variables that might have been in use).  This is optional, and by default
   * no reinitialization is performed.
   */
  public void reinitialize()
  {
    // No implementation required by default.
  }



  /**
   * Generates the appropriate output that should be included whenever this tag
   * is encountered in the template.
   *
   * @param  tagArguments  The set of arguments provided to the tag.  Because
   *                       custom tags are the last
   *
   * @return  The appropriate output that should be included whenever this tag
   *          is encountered in the template.
   */
  public abstract String generateOutput(String[] tagArguments);
}

