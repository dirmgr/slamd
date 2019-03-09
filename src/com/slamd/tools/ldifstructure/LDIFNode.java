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
package com.slamd.tools.ldifstructure;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;



/**
 * This class defines a data structure that holds information about a node in
 * the directory tree.  In particular, it includes the DN of the entry
 * associated with this node, a reference to the node for the parent entry, a
 * count of all immediate subordinate entries, and a list of all the child nodes
 * that themselves have one or more subordinate entries.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFNode
{
  // The list of child nodes for this node.
  private ArrayList<LDIFNode> childNodes;

  // The number of immediate children for this node.
  private int numChildren;

  // The reference to the node for the parent entry.
  private LDIFNode parentNode;

  // The normalized DN for this entry.
  private String normalizedDN;

  // The mapping used to keep track of the entry types of the direct
  // subordinates.
  private TreeMap<String,LDIFEntryType> childEntryTypes;



  /**
   * Creates a new LDIF node with the provided information.
   *
   * @param  normalizedDN  The normalized DN of the entry associated with this
   *                       LDIF node.
   * @param  parentNode    The reference to the parent node.  It may be
   *                       <CODE>null</CODE> if this node does not have a
   *                       parent.
   */
  public LDIFNode(String normalizedDN, LDIFNode parentNode)
  {
    this.normalizedDN = normalizedDN;
    this.parentNode   = parentNode;

    numChildren     = 0;
    childNodes      = new ArrayList<LDIFNode>();
    childEntryTypes = new TreeMap<String,LDIFEntryType>();
  }



  /**
   * Retrieves the normalized DN for this LDIF node.
   *
   * @return  The normalized DN for this LDIF node.
   */
  public String getNormalizedDN()
  {
    return normalizedDN;
  }



  /**
   * Retrieves the reference to the parent node for this LDIF node.
   *
   * @return  The reference to the parent node for this LDIF node, or
   *          <CODE>null</CODE> if this node does not have a parent.
   */
  public LDIFNode getParentNode()
  {
    return parentNode;
  }



  /**
   * Retrieves the number of entries that are immediate children of this node.
   *
   * @return  The number of entries that are immediate children of this node.
   */
  public int getNumChildren()
  {
    return numChildren;
  }



  /**
   * Retrieves the number of entries that are direct or indirect descendants of
   * this entry.
   *
   * @return  The number of entries that are descendants of this entry.
   */
  public int getNumDescendants()
  {
    int descendantCount = numChildren;

    for (int i=0; i < childNodes.size(); i++)
    {
      descendantCount += childNodes.get(i).getNumDescendants();
    }

    return descendantCount;
  }



  /**
   * Retrieves the set of entry types for the child entries.
   *
   * @return  The set of entry types for the child entries.
   */
  public TreeMap getChildEntryTypes()
  {
    return childEntryTypes;
  }



  /**
   * Retrieves the set of entry types for the child entries, sorted in
   * descending order of the number of entries of that type.
   *
   * @return  The set of entry types for the child entries, sorted in descending
   *          order of the number of entries of that type.
   */
  public LDIFEntryType[] getSortedChildTypes()
  {
    LDIFEntryType[] childTypes =
         new LDIFEntryType[childEntryTypes.values().size()];
    childEntryTypes.values().toArray(childTypes);

    for (int i=0; i < childTypes.length; i++)
    {
      int mostMatches = childTypes[i].getNumEntries();
      for (int j=i+1; j < childTypes.length; j++)
      {
        int currentMatches = childTypes[j].getNumEntries();
        if (currentMatches > mostMatches)
        {
          LDIFEntryType t = childTypes[i];
          childTypes[i]   = childTypes[j];
          childTypes[j]   = t;
          mostMatches     = currentMatches;
        }
      }
    }

    return childTypes;
  }



  /**
   * Retrieves an entry type definition that is constructed from all the child
   * entry types associated with this node.
   *
   * @return  An entry type definition that is constructed from all the child
   *          entry types associated with this node, or <CODE>null</CODE> if
   *          there are no child entry types.
   */
  public LDIFEntryType getAggregateChildEntryType()
  {
    if (childEntryTypes.isEmpty())
    {
      return null;
    }

    LDIFEntryType aggregateType = new LDIFEntryType();
    Iterator iterator = childEntryTypes.values().iterator();
    while (iterator.hasNext())
    {
      aggregateType.aggregate((LDIFEntryType) iterator.next());
    }

    return aggregateType;
  }



  /**
   * Increments the total number of entries that are immediate children of this
   * node.
   *
   * @param  childEntry  The entry to add as the child of this node.
   */
  public void addChild(LDIFEntry childEntry)
  {
    numChildren++;

    LDIFAttribute attr = childEntry.getAttribute("objectclass");
    if (attr == null)
    {
      return;
    }

    ArrayList values = attr.getValues();
    String[] objectClasses = new String[values.size()];
    if (objectClasses.length == 0)
    {
      return;
    }

    for (int i=0; i < objectClasses.length; i++)
    {
      objectClasses[i] = LDIFReader.toLowerCase((String) values.get(i));
    }
    Arrays.sort(objectClasses);
    StringBuilder keyStr = new StringBuilder();
    keyStr.append(objectClasses[0]);
    for (int i=1; i < objectClasses.length; i++)
    {
      keyStr.append(' ');
      keyStr.append(objectClasses[i]);
    }

    String key = keyStr.toString();
    LDIFEntryType entryType = childEntryTypes.get(key);
    if (entryType == null)
    {
      entryType = new LDIFEntryType(childEntry, objectClasses);
      childEntryTypes.put(key, entryType);
    }
    else
    {
      entryType.update(childEntry);
    }
  }



  /**
   * Retrives the set of child nodes for this LDIF node.  There should be a
   * child node for each immediate child entry that itself has one or more
   * children.
   *
   * @return  The set of child nodes for this LDIF node.
   */
  public ArrayList getChildNodes()
  {
    return childNodes;
  }



  /**
   * Adds the provided node to the set of child nodes for this LDIF node.
   *
   * @param  childNode  The node to add to the set of child nodes for this LDIF
   *                    node.
   */
  public void addChildNode(LDIFNode childNode)
  {
    childNodes.add(childNode);
  }



  /**
   * Retrieves a string representation of this LDIF node.  It will indicate
   * how many immediate children it has.  Those children that have their own
   * subordinate entries will be enumerated in an indented fashion on their
   * own line (may be several levels deep through recursion).
   *
   * @return  A string representation of this LDIF node.
   */
  public String toString()
  {
    return toString(0, 2);
  }



  /**
   * Retrieves a string representation of this LDIF node starting with the
   * specified indent.
   *
   * @param  indent     The number of spaces to indent the information for this
   *                    node.
   * @param  increment  The incremental depth to use for descendant nodes.
   *
   * @return  A string representation of this LDIF node starting with the
   *          specified indent.
   */
  public String toString(int indent, int increment)
  {
    StringBuilder buffer = new StringBuilder();
    for (int i=0; i < indent; i++)
    {
      buffer.append(' ');
    }

    buffer.append(normalizedDN);
    buffer.append(":  ");
    buffer.append(numChildren);

    if (numChildren == 1)
    {
      buffer.append(" immediate child");
    }
    else
    {
      buffer.append(" immediate children");
    }

    String EOL = System.getProperty("line.separator");
    for (int i=0; i < childNodes.size(); i++)
    {
      LDIFNode n = childNodes.get(i);
      buffer.append(EOL);
      buffer.append(n.toString(indent+increment, increment));
    }

    return buffer.toString();
  }
}

