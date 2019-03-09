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
package com.slamd.stat;



import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Reader;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.SLAMDException;



/**
 * This class defines a program that may be used to extract and view information
 * contained in encoded persistent stat files.
 *
 *
 * @author   Neil A. Wilson
 */
public class PersistentStatViewer
       implements ItemListener
{
  // The stat trackers with the information to view.
  protected StatTracker[] statTrackers;

  // The variables related to the GUI.
  protected JComboBox statListBox;
  private   JFrame    appWindow;
  private   JPanel    graphPanel;



  /**
   * Parses the command line arguments to figure out what the user wants to do
   * and then attempts to carry that out.
   *
   * @param  args  The command-line arguments provided to this program.
   *
   * @throws  Exception  If a problem occurs.
   */
  public static void main(String[] args)
         throws Exception
  {
    // Set default values for the configurable options.
    boolean aggregateThreads = false;
    boolean guiMode          = false;
    boolean verboseOutput    = false;
    String  filename         = null;


    // Iterate through the command-line parameters and process them.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-a"))
      {
        aggregateThreads = true;
      }
      else if (args[i].equals("-g"))
      {
        guiMode = true;
      }
      else if (args[i].equals("-v"))
      {
        verboseOutput = true;
      }
      else if (args[i].equals("-f"))
      {
        filename = args[++i];
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


    // Make sure that a filename was provided.
    if (filename == null)
    {
      System.err.println("ERROR:  No stat file was provided (use -f)");
      displayUsage();
      System.exit(1);
    }


    // Get the stat trackers from that file.
    StatTracker[] trackers = decodeStats(filename);
    if ((trackers == null) || (trackers.length == 0))
    {
      System.err.println("ERROR:  No stat tracker data was found in the " +
                         "specified file.");
      System.exit(1);
    }


    if (guiMode)
    {
      PersistentStatViewer statViewer = new PersistentStatViewer(trackers);
      return;
    }


    // Create a list of the unique display names for all the stats.
    ArrayList<String> statNames = new ArrayList<String>();
    for (int i=0; i < trackers.length; i++)
    {
      if (! statNames.contains(trackers[i].getDisplayName()))
      {
        statNames.add(trackers[i].getDisplayName());
      }
    }


    // Iterate through the stat names and display information for each.
    for (int i=0; i < statNames.size(); i++)
    {
      String name = statNames.get(i);

      ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
      for (int j=0; j < trackers.length; j++)
      {
        if (trackers[j].getDisplayName().equals(name))
        {
          trackerList.add(trackers[j]);
        }
      }

      StatTracker[] trackersToDisplay;
      if (aggregateThreads)
      {
        StatTracker[] trackersToAggregate = new StatTracker[trackerList.size()];
        trackerList.toArray(trackersToAggregate);

        if (trackersToAggregate.length <= 1)
        {
          trackersToDisplay = trackersToAggregate;
        }
        else
        {
          StatTracker aggregateTracker = trackersToAggregate[0].newInstance();
          aggregateTracker.aggregate(trackersToAggregate);
          trackersToDisplay = new StatTracker[] { aggregateTracker };
        }
      }
      else
      {
        trackersToDisplay = new StatTracker[trackerList.size()];
        trackerList.toArray(trackersToDisplay);
      }

      for (int j=0; j < trackersToDisplay.length; j++)
      {
        if (verboseOutput)
        {
          System.out.println(trackersToDisplay[j].getDetailString());
        }
        else
        {
          System.out.println(trackersToDisplay[j].getSummaryString());
        }

        System.out.println();
      }
    }
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String EOL = System.getProperty("line.separator");

    System.err.println(
"USAGE:  java com.slamd.stat.PersistentStatViewer {options}" + EOL +
"        where {options} include:" + EOL +
"-f {filename} -- Specifies the path to the persistent stat file to use" + EOL +
"-a            -- Indicates that all threads should be aggregated" + EOL +
"-v            -- Display verbose statistics" + EOL +
"-g            -- Display graphs (in a GUI) rather than values" + EOL +
"-H            -- Displays usage information"
                      );
  }



  /**
   * Creates a new instance of the graphical persistent stat viewer that will be
   * used to display information about the provided stat trackers.
   *
   * @param  statTrackers The stat trackers to view.
   */
  public PersistentStatViewer(StatTracker[] statTrackers)
  {
    // Set the stat trackers for this viewer and get the unique names.
    this.statTrackers = statTrackers;


    // Get the unique names of the stat trackers.
    ArrayList<String> nameList = new ArrayList<String>();
    for (int i=0; i < statTrackers.length; i++)
    {
      if (! nameList.contains(statTrackers[i].getDisplayName()))
      {
        nameList.add(statTrackers[i].getDisplayName());
      }
    }

    String[] statNames = new String[nameList.size()];
    nameList.toArray(statNames);


    // Create the GUI.
    appWindow = new JFrame("SLAMD Persistent Stat Viewer");
    appWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    appWindow.getContentPane().setLayout(new BorderLayout());
    appWindow.setResizable(false);

    statListBox = new JComboBox(statNames);
    statListBox.setEditable(false);
    statListBox.addItemListener(this);
    appWindow.getContentPane().add(statListBox, BorderLayout.NORTH);

    graphPanel = new PersistentStatGraphPanel(this);
    graphPanel.setPreferredSize(new Dimension(640, 480));
    appWindow.getContentPane().add(graphPanel, BorderLayout.CENTER);

    appWindow.pack();
    appWindow.setVisible(true);
    graphPanel.repaint();
  }



  /**
   * Indicates that an item change event has occurred and should be processed.
   * In this case, it will simply trigger the graph to be redrawn.
   *
   * @param  event  The item change event that occurred.
   */
  public void itemStateChanged(ItemEvent event)
  {
    graphPanel.repaint();
  }



  /**
   * Reads the provided data file and decodes it as a set of stat trackers.
   *
   * @param  filename  The path to the file containing the encoded stat tracker
   *                   information.
   *
   * @return  The set of stat trackers that have been decoded from the specified
   *          file.
   *
   * @throws  IOException  If a problem occurs while attempting to read from the
   *                       specified file.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          information as a set of stat trackers.
   */
  public static StatTracker[] decodeStats(String filename)
         throws IOException, SLAMDException
  {
    File statFile = new File(filename);
    if (! (statFile.exists() && statFile.isFile()))
    {
      throw new IOException('"' + filename + "\" either does not exist or " +
                            "is not a regular file.");
    }

    FileInputStream inputStream = new FileInputStream(filename);
    ASN1Reader      asn1Reader  = new ASN1Reader(inputStream);
    ASN1Element     element;

    try
    {
      element = asn1Reader.readElement();
    }
    catch (IOException ioe)
    {
      try
      {
        inputStream.close();
      } catch (Exception e2) {}

      throw ioe;
    }
    catch (Exception e)
    {
      try
      {
        inputStream.close();
      } catch (Exception e2) {}

      throw new SLAMDException("Unable to decode the contents of the file \"" +
                               filename + "\" as an ASN.1 element -- " + e, e);
    }


    asn1Reader.close();
    inputStream.close();


    ASN1Sequence sequence;
    try
    {
      sequence = element.decodeAsSequence();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Unable to decode the ASN.1 element read from " +
                               "the input file as a sequence -- " + e, e);
    }


    return StatEncoder.sequenceToTrackers(sequence);
  }
}





/**
 * This class defines a custom panel that will be used to display graphs of the
 * results.  This is necessary to ensure that the graphs that are displayed stay
 * displayed whenever a repaint event occurs.
 */
class PersistentStatGraphPanel
      extends JPanel
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 7284912589440159947L;



  // The stat viewer with which this panel is associated.
  PersistentStatViewer statViewer;


  /**
   * Creates a new graph panel that will be used with the provided stat viewer.
   *
   * @param  statViewer  The stat viewer with which this panel is associated.
   */
  public PersistentStatGraphPanel(PersistentStatViewer statViewer)
  {
    super(true);

    this.statViewer = statViewer;
  }



  /**
   * Draws the graph of the statistics based on the selected tracker name.
   *
   * @param  g  The graphics context to use to draw the graph.
   */
  public void paint(Graphics g)
  {
    try
    {
      // Figure out which stat is selected and get the associated trackers.
      String statName = (String) statViewer.statListBox.getSelectedItem();
      ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();
      for (int i=0; i < statViewer.statTrackers.length; i++)
      {
        if (statViewer.statTrackers[i].getDisplayName().equals(statName))
        {
          trackerList.add(statViewer.statTrackers[i]);
        }
      }

      if (trackerList.isEmpty())
      {
        return;
      }

      StatTracker[] trackers = new StatTracker[trackerList.size()];
      trackerList.toArray(trackers);
      StatTracker aggregateTracker = trackers[0].newInstance();
      aggregateTracker.aggregate(trackers);

      BufferedImage image = aggregateTracker.createGraph(640, 480);
      g.drawImage(image, 0, 0, null);
    } catch (Exception e) {}
  }
}

