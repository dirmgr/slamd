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



import java.util.ArrayList;



/**
 * This class defines a branch of the directory tree that should be included in
 * the resulting LDIF file and may or may not have subordinate entries.
 *
 *
 * @author   Neil A. Wilson
 */
public class Branch
{
  // A list whose order corresponds with that of the subordinateTemplates that
  // tells how many entries of that type should be created under this branch
  private ArrayList<Integer> entriesPerTemplate;

  // Extra lines of data in LDIF form to be written into the branch entry
  private ArrayList<String> extraLines;

  // The list of subordinate templates that have been defined for use with this
  // branch
  private ArrayList<String> subordinateTemplates;

  // The DN of this branch entry
  private String branchDN;



  /**
   * Creates a new branch with no subordinate entries.
   *
   * @param  branchDN  The DN of the branch to create.
   */
  public Branch(String branchDN)
  {
    this.branchDN        = branchDN;
    extraLines           = new ArrayList<String>();
    entriesPerTemplate   = new ArrayList<Integer>();
    subordinateTemplates = new ArrayList<String>();
  }



  /**
   * Retrieves the DN of the branch to create.
   *
   * @return  The DN of the branch to create.
   */
  public String getDN()
  {
    return branchDN;
  }



  /**
   * Adds a new template for use with this branch.
   *
   * @param  templateName  The name of the template to use when creating the
   *                       entries.
   * @param  numEntries    The number of entries of this type to create.
   */
  public void addTemplate(String templateName, int numEntries)
  {
    subordinateTemplates.add(templateName);
    entriesPerTemplate.add(numEntries);
  }



  /**
   * Indicates whether this branch has subordinate entries.
   *
   * @return  <CODE>true</CODE> if this branch does have subordinate entries, or
   *          <CODE>false</CODE> if not.
   */
  public boolean hasSubordinates()
  {
    return (! subordinateTemplates.isEmpty());
  }



  /**
   * Retrieves the names of the templates to use for creating subordinate
   * entries.
   *
   * @return  The names of the templates to use for creating subordinate
   *          entries.
   */
  public String[] getSubordinateTemplates()
  {
    String[] templates = new String[subordinateTemplates.size()];
    for (int i=0; i < templates.length; i++)
    {
      templates[i] = subordinateTemplates.get(i);
    }

    return templates;
  }



  /**
   * Retrieves the number of subordinate entries that should be created for
   * the specified template.
   *
   * @param  templateName  The name of the template for which to create the
   *                       subordinate entries.
   *
   * @return  The number of subordinate entries that should be created for the
   *          specified template.
   */
  public int numEntriesForTemplate(String templateName)
  {
    for (int i=0; i < subordinateTemplates.size(); i++)
    {
      if (templateName.equalsIgnoreCase(subordinateTemplates.get(i)))
      {
        return entriesPerTemplate.get(i);
      }
    }


    return 0;
  }



  /**
   * Adds the provided extra line to this branch.
   *
   * @param  line  The extra line to add to this branch.
   */
  public void addExtraLine(String line)
  {
    extraLines.add(line);
  }



  /**
   * Retrieves the set of extra lines for this branch.
   *
   * @return  The set of extra lines for this branch.
   */
  public String[] getExtraLines()
  {
    String[] extraLineArray = new String[extraLines.size()];

    for (int i=0; i < extraLineArray.length; i++)
    {
      extraLineArray[i] = extraLines.get(i);
    }

    return extraLineArray;
  }
}

