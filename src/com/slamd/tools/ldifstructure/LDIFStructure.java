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



import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;



/**
 * This program defines a utility that can examine an LDIF file and gather
 * summary information about the structure of the associated directory.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDIFStructure
       implements TreeSelectionListener, ListSelectionListener
{
  /**
   * The end-of-line character for this operating system.
   */
  public static final String EOL = System.getProperty("line.separator");



  // A list of the root nodes read from the LDIF.
  private ArrayList<LDIFNode> rootNodes;

  // Indicates whether we should ignore hierarchy when examining the data.
  private boolean ignoreHierarchy;

  // The decimal format used to format numeric values.
  private DecimalFormat decimalFormat;

  // The mapping to hold all nodes read from the LDIF.
  private HashMap<String,LDIFNode> nodeMap;

  // The maximum number of entries to process.
  private int maxEntries;

  // The main window used for the application.
  private JFrame appWindow;

  // The label used to display the total number of descendants.
  private JLabel descendantCountLabel;

  // The label used to display the number of direct descendants.
  private JLabel childCountLabel;

  // The label used to display the DN of the current entry.
  private JLabel dnLabel;

  // The list box used to display the entry types for the children immediately
  // below a given entry.
  private JList childTypeList;

  // The text area used to display information about a particular entry type.
  private JTextArea entryTypeArea;

  // The tree used to graphically represent the hierarchy of the LDIF.
  private JTree ditTree;

  // The currently selected LDIF node.
  private LDIFNode selectedNode;

  // The LDIF file from which the information has been read.
  private String ldifFile;



  /**
   * Parses the command-line arguments and takes the appropriate action.
   *
   * @param  args  The command-line arguments provided to this program.
   *
   * @throws  Exception  If a problem occurs while examining the data.
   */
  public static void main(String[] args)
         throws Exception
  {
    boolean aggregateOnly   = false;
    boolean ignoreHierarchy = false;
    int     maxEntries      = 0;
    String  ldifFile        = null;
    String  outputFile      = null;

    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-l"))
      {
        ldifFile = args[++i];
      }
      else if (args[i].equals("-o"))
      {
        outputFile = args[++i];
      }
      else if (args[i].equals("-x"))
      {
        maxEntries = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-i"))
      {
        ignoreHierarchy = true;
      }
      else if (args[i].equals("-a"))
      {
        aggregateOnly = true;
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("ERROR:  Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        System.exit(1);
      }
    }


    if (ldifFile == null)
    {
      System.err.println("ERROR:  No LDIF file specified (use -l)");
      displayUsage();
      System.exit(1);
    }


    LDIFStructure ldifStructure = new LDIFStructure(ldifFile, ignoreHierarchy,
                                                    maxEntries);
    if (outputFile == null)
    {
      ldifStructure.displayGUI();
    }
    else
    {
      ldifStructure.writeOutputFile(outputFile, aggregateOnly);
      System.out.println("Wrote LDIF structure information to " + outputFile);
    }
  }



  /**
   * Processes the provided LDIF file and populates the appropriate internal
   * structures based on its contents.
   *
   * @param  ldifFile         The path to the LDIF file to process.
   * @param  ignoreHierarchy  Indicates whether to ignore hierarchy and just
   *                          look at unique objectclass combinations.
   * @param  maxEntries       The maximum number of entries to process, or -1 if
   *                          there should be no maximum.
   *
   * @throws  IOException  If a problem occurs while reading from the LDIF.
   *
   * @throws  ParseException  If a problem occurs while parsing an entry from
   *                          the LDIF.
   */
  public LDIFStructure(String ldifFile, boolean ignoreHierarchy, int maxEntries)
         throws IOException, ParseException
  {
    // Initialize the appropriate instance variables.
    this.ldifFile        = ldifFile;
    this.ignoreHierarchy = ignoreHierarchy;
    decimalFormat        = new DecimalFormat("0.00");
    rootNodes            = new ArrayList<LDIFNode>();
    nodeMap              = new HashMap<String,LDIFNode>();

    String baseDN = null;


    // Create the LDIF reader and use it to start reading entries.
    LDIFReader reader = new LDIFReader(ldifFile);
    LDIFEntry entry = reader.nextEntry();
    int entriesRead = 0;
    while (entry != null)
    {
      if ((maxEntries > 0) && (entriesRead >= maxEntries))
      {
        break;
      }
      else
      {
        entriesRead++;
      }


      // If we are to ignore hierarchy, then we need to set the DN to a
      // consistent value so that all entries will be treated the same.  The
      // base DN will be the DN of the first entry we read from the file, and
      // then we'll prepend the entry's actual RDN to it.
      if (ignoreHierarchy)
      {
        if (baseDN == null)
        {
          baseDN = entry.getNormalizedDN();

          LDIFNode node = new LDIFNode(baseDN, null);
          rootNodes.add(node);
          nodeMap.put(baseDN, node);
          entry = reader.nextEntry();
          continue;
        }

        entry.setDN("rdn=x," + baseDN);
      }


      // Get the parent DN for the entry.  If it doesn't have a parent, then it
      // must be a root node.
      String parentDN = entry.getParentDN();
      if (parentDN == null)
      {
        String normalizedDN = entry.getNormalizedDN();
        LDIFNode node = new LDIFNode(normalizedDN, null);
        rootNodes.add(node);
        nodeMap.put(normalizedDN, node);
      }
      else
      {
        // See if the parent node is already present.  If so, then just
        // increment its child count.  Otherwise, create a new node for it.
        LDIFNode parentNode = nodeMap.get(parentDN);
        if (parentNode == null)
        {
          String grandparentDN = entry.getGrandparentDN();
          if (grandparentDN == null)
          {
            // This could be a multi-level suffix, so make it a root node.
            String normalizedDN = entry.getNormalizedDN();
            LDIFNode node = new LDIFNode(normalizedDN, null);
            rootNodes.add(node);
            nodeMap.put(normalizedDN, node);
          }
          else
          {
            // See if we have a node for the grandparent DN.  If we do, then
            // Add a new node for the parent DN and make it a child of the
            // grandparent.  If not, then it must be an out-of-order LDIF.
            LDIFNode grandparentNode = nodeMap.get(grandparentDN);
            if (grandparentNode == null)
            {
              // This could be a multi-level suffix, so make it a root node.
              String normalizedDN = entry.getNormalizedDN();
              LDIFNode node = new LDIFNode(normalizedDN, null);
              rootNodes.add(node);
              nodeMap.put(normalizedDN, node);
            }
            else
            {
              parentNode = new LDIFNode(parentDN, grandparentNode);
              parentNode.addChild(entry);
              grandparentNode.addChildNode(parentNode);
              nodeMap.put(parentDN, parentNode);
            }
          }
        }
        else
        {
          parentNode.addChild(entry);
        }
      }



      // Print out a status message if appropriate and then read the next entry.
      if ((entriesRead % 1000) == 0)
      {
        System.out.println("Processed " + entriesRead + " entries.");
      }
      entry = reader.nextEntry();
    }


    // We should be done processing the LDIF.  Close the file and print out the
    // resulting tree.
    reader.close();
    System.out.println("End of LDIF reached.  Processed " + entriesRead +
                       " entries");
  }



  /**
   * Writes an output file with summary information collected from the LDIF.
   *
   * @param  outputFile     The path to the output file to write.
   * @param  aggregateOnly  Indicates whether to only write aggregate
   *                        information for each node rather than separate
   *                        output for each unique objectclass combination.
   *
   * @throws  IOException  If a problem occurs while trying to write the output
   *                       file.
   */
  public void writeOutputFile(String outputFile, boolean aggregateOnly)
         throws IOException
  {
    // Open the output file for writing.
    PrintWriter writer = new PrintWriter(new FileWriter(outputFile));


    // Iterate through the root nodes and write information about them and their
    // subordinates to the the output file.
    for (int i=0; i < rootNodes.size(); i++)
    {
      LDIFNode n = rootNodes.get(i);
      writeNode(n, writer, aggregateOnly);
    }


    // Close the output file.
    writer.flush();
    writer.close();
  }



  /**
   * Writes information about the provided node to the given writer.
   *
   * @param  node           The LDIF node to be written.
   * @param  writer         The writer to which the information should be
   *                        written.
   * @param  aggregateOnly  Indicates whether to only write aggregate
   *                        information for each node rather than separate
   *                        output for each unique objectclass combination.
   */
  private void writeNode(LDIFNode node, PrintWriter writer,
                         boolean aggregateOnly)
  {
    writer.println("Entry DN: " + node.getNormalizedDN());
    writer.println("Immediate Children: " + node.getNumChildren());
    writer.println("Total Descendants: " + node.getNumDescendants());

    LDIFEntryType[] entryTypes = node.getSortedChildTypes();
    if (entryTypes.length > 1)
    {
      LDIFEntryType t = node.getAggregateChildEntryType();
      writer.println("    Entry Type: Aggregate");

      LinkedHashMap<String,Integer> objectClassCounts =
           t.getAggregateObjectClassCounts();
      Iterator iterator = objectClassCounts.keySet().iterator();
      while (iterator.hasNext())
      {
        String s = (String) iterator.next();
        int count = objectClassCounts.get(s);
        double percent = 100.0 * count / node.getNumChildren();

        writer.println("    objectClass: " + s + " (" +
                       decimalFormat.format(percent) +
                       "% of matching entries)");
      }

      iterator = t.getAttributes().keySet().iterator();
      while (iterator.hasNext())
      {
        String s = (String) iterator.next();
        LDIFAttributeInfo i = t.getAttributes().get(s);

        double percentOfEntries = 100.0 * i.getNumEntries() /
                                  t.getNumEntries();
        double valuesPerEntry   = i.getAverageValuesPerEntry();
        double charsPerValue    = i.getAverageCharactersPerValue();
        writer.println("    " + s + ": " +
                       decimalFormat.format(percentOfEntries) +
                       "% of entries, " +
                       decimalFormat.format(valuesPerEntry) +
                       " values per entry, " +
                       decimalFormat.format(charsPerValue) +
                       " characters per value");

        if (i.getNumUniqueValues() > 0)
        {
          TreeMap valueCounts = i.getUniqueValues();
          String separator="";

          writer.print("        <");

          Iterator iterator2 = valueCounts.keySet().iterator();
          while (iterator2.hasNext())
          {
            String value = (String) iterator2.next();
            writer.print(separator + value + ':' + valueCounts.get(value));
            separator = ",";
          }

          writer.println(">");
        }
      }

      writer.println();
    }

    if ((! aggregateOnly) || (entryTypes.length == 1))
    {
      for (int i=0; i < entryTypes.length; i++)
      {
        String[] objectClasses = entryTypes[i].getObjectClasses();
        String label = objectClasses[0];
        for (int j=1; j < objectClasses.length; j++)
        {
          label = label + ' ' + objectClasses[j];
        }

        double pct =
             100.0 * entryTypes[i].getNumEntries() / node.getNumChildren();

        writer.println("    Entry Type: " + label);
        writer.println("    Matching Entries: " +
                       entryTypes[i].getNumEntries() + '(' +
                       decimalFormat.format(pct) + " percent of entries " +
                       "immediately below " + node.getNormalizedDN() + ')');

        for (int j=0; j < objectClasses.length; j++)
        {
          writer.println("    objectClass: " + objectClasses[j]);
        }

        Iterator iterator = entryTypes[i].getAttributes().keySet().iterator();
        while (iterator.hasNext())
        {
          String s = (String) iterator.next();
          LDIFAttributeInfo ai = entryTypes[i].getAttributes().get(s);

          double percentOfEntries = 100.0 * ai.getNumEntries() /
                                    entryTypes[i].getNumEntries();
          double valuesPerEntry   = ai.getAverageValuesPerEntry();
          double charsPerValue    = ai.getAverageCharactersPerValue();
          writer.println("    " + s + ": " +
                         decimalFormat.format(percentOfEntries) +
                         "% of entries, " +
                         decimalFormat.format(valuesPerEntry) +
                         " values per entry, " +
                         decimalFormat.format(charsPerValue) +
                         " characters per value");

          if (ai.getNumUniqueValues() > 0)
          {
            TreeMap valueCounts = ai.getUniqueValues();
            String separator="";

            writer.print("        <");

            Iterator iterator2 = valueCounts.keySet().iterator();
            while (iterator2.hasNext())
            {
              String value = (String) iterator2.next();
              writer.print(separator + value + ':' + valueCounts.get(value));
              separator = ",";
            }

            writer.println(">");
          }
        }

        writer.println();
      }
    }

    writer.println();
    writer.println("--------------------------------------------------");
    writer.println();


    for (int i=0; i < node.getChildNodes().size(); i++)
    {
      LDIFNode n = (LDIFNode) node.getChildNodes().get(i);
      writeNode(n, writer, aggregateOnly);
    }
  }



  /**
   * Generates and displays the GUI that may be used to browse the contents of
   * the LDIF.
   */
  public void displayGUI()
  {
    // Now create the GUI to display to the end user.  Start with the main
    // window.
    appWindow = new JFrame("LDIF Browser:  " + ldifFile);
    appWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    appWindow.getContentPane().setLayout(new BorderLayout());


    // Create a split pane that will be used to split the DIT tree from
    // the rest of the data.
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


    // Create the tree and put it on the left side of the window.
    DefaultMutableTreeNode topNode = new DefaultMutableTreeNode("Root Nodes");
    for (int i=0; i < rootNodes.size(); i++)
    {
      addChildNode(topNode, rootNodes.get(i));
    }

    ditTree = new JTree(topNode);
    ditTree.setRootVisible(false);
    ditTree.setShowsRootHandles(true);
    ditTree.addTreeSelectionListener(this);
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.getViewport().setView(ditTree);
    splitPane.add(scrollPane, JSplitPane.LEFT);


    // Create the DN and count labels and add them to a panel using a grid
    // layout.
    dnLabel              = new JLabel("Entry DN:  {none selected}");
    childCountLabel      = new JLabel("Immediate Children:  N/A");
    descendantCountLabel = new JLabel("Total Descendants:  N/A");

    JPanel labelPanel = new JPanel(new GridLayout(3, 1));
    labelPanel.add(dnLabel, 0);
    labelPanel.add(childCountLabel, 1);
    labelPanel.add(descendantCountLabel, 2);


    // Create the child node list and put it and the label panel on another
    // panel with a border layout.  Then add that panel to the main window.
    childTypeList = new JList();
    childTypeList.setVisibleRowCount(10);
    childTypeList.addListSelectionListener(this);
    scrollPane = new JScrollPane();
    scrollPane.getViewport().setView(childTypeList);

    JPanel listAndLabelPanel = new JPanel(new BorderLayout());
    listAndLabelPanel.add(labelPanel, BorderLayout.NORTH);
    listAndLabelPanel.add(scrollPane, BorderLayout.CENTER);


    // Create a new panel to hold the label, list, and entry type information
    // and then add those elements.
    JPanel rightPanel = new JPanel(new BorderLayout());
    entryTypeArea = new JTextArea("No entry has been selected.  Please " +
                                  "choose a node from the tree to the left",
                                  30, 80);
    entryTypeArea.setEditable(false);
    entryTypeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    scrollPane = new JScrollPane();
    scrollPane.getViewport().setView(entryTypeArea);
    rightPanel.add(listAndLabelPanel, BorderLayout.NORTH);
    rightPanel.add(scrollPane, BorderLayout.CENTER);
    splitPane.add(rightPanel, JSplitPane.RIGHT);
    appWindow.getContentPane().add(splitPane, BorderLayout.CENTER);


    // Size the window properly and make it visible.
    appWindow.pack();
    appWindow.setVisible(true);
  }



  /**
   * Adds the provided LDIF node into the DIT tree as a child of the provided
   * parent node.  This will also recursively add all descendants of the
   * provided child node.
   *
   * @param  parentTreeNode  The parent node below which the child should be
   *                         added.
   * @param  childLDIFNode   The child LDIF node to add below the parent.
   */
  public void addChildNode(DefaultMutableTreeNode parentTreeNode,
                           LDIFNode childLDIFNode)
  {
    DefaultMutableTreeNode childTreeNode =
         new DefaultMutableTreeNode(childLDIFNode.getNormalizedDN());

    for (int i=0; i < childLDIFNode.getChildNodes().size(); i++)
    {
      LDIFNode n = (LDIFNode) childLDIFNode.getChildNodes().get(i);
      addChildNode(childTreeNode, n);
    }

    parentTreeNode.add(childTreeNode);
  }



  /**
   * Indicates that the selected node of the tree has changed and that the
   * other components should be updated to reflect that change.
   *
   * @param  selectionEvent  The selection even with information on the change.
   */
  public void valueChanged(TreeSelectionEvent selectionEvent)
  {
    // Get the currently selected node from the DIT tree.  Then convert it to
    // an LDIF node.
    TreePath selectionPath = ditTree.getSelectionPath();
    if (selectionPath == null)
    {
      return;
    }

    DefaultMutableTreeNode treeNode =
         (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
    String entryDN = (String) treeNode.getUserObject();
    if (entryDN == null)
    {
      return;
    }

    selectedNode = nodeMap.get(entryDN);
    if (selectedNode == null)
    {
      return;
    }


    // At this point, we have the node, so use it to update the display.
    dnLabel.setText("Entry DN:  " + entryDN);
    childCountLabel.setText("Immediate Children:  " +
                            selectedNode.getNumChildren());
    descendantCountLabel.setText("Total Descendants:  " +
                                 selectedNode.getNumDescendants());

    LDIFEntryType[] childTypes = selectedNode.getSortedChildTypes();
    if (childTypes.length > 1)
    {
      String[] typeStrings = new String[childTypes.length+1];
      typeStrings[0] = selectedNode.getNumChildren() + ":  Aggregate";
      for (int i=0; i < childTypes.length; i++)
      {
        StringBuilder displayStr = new StringBuilder();
        displayStr.append(childTypes[i].getNumEntries());
        displayStr.append(": ");

        for (int j=0; j < childTypes[i].getObjectClasses().length; j++)
        {
          displayStr.append(' ');
          displayStr.append(childTypes[i].getObjectClasses()[j]);
        }
        typeStrings[i+1] = displayStr.toString();
      }
      childTypeList.setListData(typeStrings);
    }
    else
    {
      String[] typeStrings = new String[childTypes.length];
      for (int i=0; i < childTypes.length; i++)
      {
        StringBuilder displayStr = new StringBuilder();
        displayStr.append(childTypes[i].getNumEntries());
        displayStr.append(": ");

        for (int j=0; j < childTypes[i].getObjectClasses().length; j++)
        {
          displayStr.append(' ');
          displayStr.append(childTypes[i].getObjectClasses()[j]);
        }
        typeStrings[i] = displayStr.toString();
      }
      childTypeList.setListData(typeStrings);
    }

    appWindow.repaint();
  }



  /**
   * Indicates that the selected item in the entry type list has changed and
   * that the other components should be updated to reflect that change.
   *
   * @param  selectionEvent  The selection even with information on the change.
   */
  public void valueChanged(ListSelectionEvent selectionEvent)
  {
    entryTypeArea.setText("");
    if (selectedNode == null)
    {
      return;
    }

    String valueStr = (String) childTypeList.getSelectedValue();
    if (valueStr == null)
    {
      return;
    }

    int pos = valueStr.indexOf(":  ");
    if (pos < 0)
    {
      return;
    }

    String key = valueStr.substring(pos+3);
    boolean isAggregate;
    LDIFEntryType entryType;
    if (key.equals("Aggregate"))
    {
      isAggregate = true;
      entryType = selectedNode.getAggregateChildEntryType();
    }
    else
    {
      isAggregate = false;
      entryType = (LDIFEntryType) selectedNode.getChildEntryTypes().get(key);
    }

    if (entryType == null)
    {
      return;
    }

    StringBuilder b = new StringBuilder();
    int matchingEntries = entryType.getNumEntries();
    double percentOfEntries =
         100.0 * matchingEntries / selectedNode.getNumChildren();
    b.append("Matching Entries:  ");
    b.append(matchingEntries);
    b.append(" (");
    b.append(decimalFormat.format(percentOfEntries));
    b.append("% of child entries below ");
    b.append(selectedNode.getNormalizedDN());
    b.append(')');
    b.append(EOL);

    if (isAggregate)
    {
      LinkedHashMap<String,Integer> objectClassCounts =
           entryType.getAggregateObjectClassCounts();

      Iterator iterator = objectClassCounts.keySet().iterator();
      while (iterator.hasNext())
      {
        String s = (String) iterator.next();
        int count = objectClassCounts.get(s);
        percentOfEntries = 100.0 * count / matchingEntries;

        b.append("objectClass: ");
        b.append(s);
        b.append(" (");
        b.append(decimalFormat.format(percentOfEntries));
        b.append("% of matching entries)");
        b.append(EOL);
      }
    }
    else
    {
      for (int i=0; i < entryType.getObjectClasses().length; i++)
      {
        b.append("objectClass: ");
        b.append(entryType.getObjectClasses()[i]);
        b.append(EOL);
      }
    }

    Iterator iterator = entryType.getAttributes().values().iterator();
    while (iterator.hasNext())
    {
      LDIFAttributeInfo ai = (LDIFAttributeInfo) iterator.next();
      percentOfEntries = 100.0 * ai.getNumEntries() / matchingEntries;

      b.append(ai.getAttributeName());
      b.append(": ");
      b.append(decimalFormat.format(percentOfEntries));
      b.append("% of entries, ");
      b.append(decimalFormat.format(ai.getAverageValuesPerEntry()));
      b.append(" values per entry, ");
      b.append(decimalFormat.format(ai.getAverageCharactersPerValue()));
      b.append(" characters per value");
      b.append(EOL);

      if (ai.getNumUniqueValues() > 0)
      {
        String separator = "";

        b.append("     <");

        Iterator iterator2 = ai.getUniqueValues().keySet().iterator();
        while (iterator2.hasNext())
        {
          String s = (String) iterator2.next();
          b.append(separator);
          b.append(s);
          b.append(':');
          b.append(ai.getUniqueValues().get(s));

          separator = ",";
        }

        b.append('>');
        b.append(EOL);
      }
    }

    entryTypeArea.setText(b.toString());
    entryTypeArea.setSelectionStart(0);
    entryTypeArea.setSelectionEnd(0);
    appWindow.repaint();
  }



  /**
   * Writes usage information for this program to standard error.
   */
  public static void displayUsage()
  {
    System.out.println(
"USAGE:  java LDIFStructure {options}" + EOL +
"        where {options} include:" + EOL +
"-l {ldifFile} -- The LDIF file to process" + EOL +
"-o {outFile}  -- The output file to create (instead of showing a GUI)" + EOL +
"-x {maxCount} -- Process at most this number of entries" + EOL +
"-i            -- Ignore hierarchy and only focus on objectclass sets" + EOL +
"-a            -- Only show aggregate data (ingored in GUI mode)" + EOL +
"-H            -- Display this usage information"
                      );
  }
}

