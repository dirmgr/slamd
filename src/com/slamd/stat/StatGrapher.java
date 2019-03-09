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



import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;



/**
 * This class provides a mechanism for generating graphs based on sets of data
 * obtained during job processing.  Note that a maximum of 30 data sets can be
 * represented on a single graph.
 *
 *
 * @author   Neil A. Wilson
 */
public class StatGrapher
{
  /**
   * The set of colors that will be used when producing the graphs.
   */
  public static final Color[] COLORS = new Color[]
  {
    new Color(0xFF, 0x20, 0x20), // Red
    new Color(0x20, 0xFF, 0x20), // Green
    new Color(0x00, 0x66, 0xCC), // Blue
    new Color(0xFF, 0xFF, 0x20), // Yellow
    new Color(0x20, 0xFF, 0xFF), // Cyan
    new Color(0x90, 0x80, 0x70), // Brown
    new Color(0xFF, 0x20, 0xFF), // Magenta
    new Color(0xA0, 0xA0, 0xA0), // Mid Gray
    new Color(0xFF, 0xA0, 0x40), // Orange
    new Color(0x90, 0x68, 0xC0), // Purple
    new Color(0x80, 0x00, 0x00), // Dark Red
    new Color(0x00, 0x80, 0x00), // Dark Green
    new Color(0x00, 0x00, 0x80), // Dark Blue
    new Color(0x80, 0x80, 0x00), // Dark Yellow
    new Color(0x00, 0x80, 0x80), // Dark Cyan
    new Color(0x60, 0x50, 0x40), // Dark Brown
    new Color(0x80, 0x00, 0x80), // Dark Magenta
    new Color(0x80, 0x80, 0x80), // Dark Gray
    new Color(0xC0, 0x58, 0x00), // Dark Orange
    new Color(0x60, 0x40, 0x80), // Dark Purple
    new Color(0xFF, 0x80, 0x80), // Light Red
    new Color(0x80, 0xFF, 0x80), // Light Green
    new Color(0x80, 0x80, 0XFF), // Light Blue
    new Color(0xFF, 0xFF, 0x80), // Light Yellow
    new Color(0x80, 0xFF, 0xFF), // Light Cyan
    new Color(0xC0, 0xB0, 0xA0), // Light Brown
    new Color(0xFF, 0xC0, 0xFF), // Light Magenta
    new Color(0xC3, 0xC3, 0xC3), // Light Gray
    new Color(0xFF, 0xA8, 0x58), // Light Orange
    new Color(0xC0, 0x80, 0xFF), // Light Purple
    new Color(0xFF, 0x00, 0x00), // Bright Red
    new Color(0x00, 0xFF, 0x00), // Bright Green
    new Color(0x00, 0x00, 0xFF), // Bright Blue
    new Color(0xFF, 0xFF, 0x00), // Bright Yellow
    new Color(0x00, 0xFF, 0xFF), // Bright Cyan
    new Color(0x66, 0x66, 0x00), // Bright Brown
    new Color(0xFF, 0x00, 0xFF), // Bright Magenta
    new Color(0xCC, 0XCC, 0xCC), // Bright Gray
    new Color(0x66, 0x00, 0x00), // Really Dark Red
    new Color(0x00, 0x66, 0x00), // Really Dark Green
    new Color(0x00, 0x00, 0x66), // Really Dark Blue
    new Color(0x66, 0x66, 0x00), // Really Dark Yellow
    new Color(0x00, 0x66, 0x66), // Really Dark Cyan
    new Color(0x66, 0x00, 0x66), // Really Dark Magenta
    new Color(0x33, 0x33, 0x33), // Really Dark Gray
    new Color(0xFF, 0xC0, 0xC0), // Light Red (Washed Out)
    new Color(0xC0, 0xFF, 0xC0), // Light Green (Washed Out)
    new Color(0xC0, 0xC0, 0xFF), // Light Blue (Washed Out)
    new Color(0xFF, 0xFF, 0xC0), // Light Yellow (Washed Out)
    new Color(0xC0, 0xFF, 0xFF), // Light Cyan (Washed Out)
  };



  // Indicates whether the lower bound of the graph should be zero rather than
  // calculated based on the information in the data set.
  private boolean baseAtZero;

  // Indicates whether line graphs should be flat between data points rather
  // than directly connected.
  private boolean flatBetweenPoints;

  // Indicates whether zero values present in the data set should be ignored.
  private boolean ignoreZeroValues;

  // Indicates whether a line showing the average value will be included in the
  // graph.
  private boolean includeAverage;

  // Indicates whether the graph should include horizontal grid lines.
  private boolean includeHorizontalGrid;

  // Indicates whether the graph should include a legend on the right side.
  private boolean includeLegend;

  // Indicates whether the graph should include a regression line.
  private boolean includeRegression;

  // Indicates whether the graph should include vertical grid lines.
  private boolean includeVerticalGrid;

  // Indicates whether the pie graph should show the percentage for each
  // category.
  protected boolean showPercentages;

  // The decimal format that will be used to format numeric values for display.
  private DecimalFormat decimalFormat;

  // The maximum value in the provided data sets.
  private double max;

  // The minimum value in the provided data sets.
  private double min;

  // The maximum value that will be displayed on the graph.
  private double graphMax;

  // The minimum value that will be displayed on the graph.
  private double graphMin;

  // The difference between graphMax and graphMin.
  private double graphSpan;

  // The sum of all the x values provided.
  private double sx;

  // The sum of the squares of all the x values provided.
  private double sxx;

  // The sum of the products of each x and y value provided.
  private double sxy;

  // The sum of all the y values provided.
  private double sy;

  // The sum of the squares of all the y values provided.
  private double syy;

  // The data to be graphed.
  private double[][] data;

  // The average values to use for data sets when generating stacked bar graphs.
  private double[][] dataSetAverages;

  // The number of data values provided.
  private int n;

  // The total width of the drawable area.
  private final int width;

  // The total height of the drawable area.
  private final int height;

  // The height of the graph area.
  private int graphHeight;

  // The X coordinate for the graph's origin.
  private int originX;

  // The Y coordinate for the graph's origin.
  private int originY;

  // The X coordinate for the lower right corner of the graph.
  private int lowerRightX;

  // The Y coordinate for the upper left corner of the graph.
  private int upperLeftY;

  // The number of seconds held in the data sets.
  private int numSeconds;

  // The number of seconds to use when starting the graph.
  private int startSeconds;

  // The collection interval used by the stat trackers.
  private int[] collectionIntervals;

  // The caption to include at the top of the graph.
  private final String graphTitle;

  // The caption to include along the horizontal axis.
  private String horizontalAxisTitle;

  // The caption to include at the top of the legend.
  private String legendTitle;

  // The caption to include along the vertical axis.
  private String verticalAxisTitle;

  // The set of labels to use for each data set (makes it possible to create the
  // legend along the right side.
  private String[] dataSetLabels;

  // The set of data set names for use in stacked bar graphs.
  private String[] dataSetNames;

  // The set of category names to use when generating stacked bar graphs.
  private String[][] categoryNames;



  /**
   * Creates a new stat grapher with the specified information.
   *
   * @param  width               The width of the drawable area to create.
   * @param  height              The height of the drawable area to create.
   * @param  graphTitle          The caption to include at the top of the graph.
   */
  public StatGrapher(final int width, final int height, final String graphTitle)
  {
    this.width                 = width;
    this.height                = height;
    this.graphTitle            = graphTitle;
    this.horizontalAxisTitle   = "Elapsed Time (seconds)";
    this.verticalAxisTitle     = "";
    this.includeLegend         = false;
    this.legendTitle           = "";
    this.includeHorizontalGrid = false;
    this.includeVerticalGrid   = false;
    this.flatBetweenPoints     = false;

    dataSetNames    = new String[0];
    categoryNames   = new String[0][];
    dataSetAverages = new double[0][];

    includeAverage      = false;
    decimalFormat       = new DecimalFormat("0.00");
    data                = new double[0][];
    collectionIntervals = new int[0];
    dataSetLabels       = new String[0];
    max                 = -Double.MAX_VALUE;
    min                 = Double.MAX_VALUE;
    numSeconds          = 0;
    startSeconds        = 0;
    n                   = 0;
    sx                  = 0.0;
    sy                  = 0.0;
    sxx                 = 0.0;
    syy                 = 0.0;
    sxy                 = 0.0;
  }



  /**
   * Adds the specified data to the set of information that will be graphed.
   *
   * @param  dataValues          The of data to include in the set of
   *                             information that will be graphed.
   * @param  collectionInterval  The collection interval associated with this
   *                             data set.
   * @param  dataSetLabel        The label to use for this data set in the
   *                             legend.
   */
  public void addDataSet(final double[] dataValues,
                         final int collectionInterval,
                         final String dataSetLabel)
  {
    final double[][] newData      = new double[data.length+1][];
    final int[]      newIntervals = new int[collectionIntervals.length+1];
    final String[]   newLabels    = new String[dataSetLabels.length+1];

    System.arraycopy(data, 0, newData, 0, data.length);
    System.arraycopy(collectionIntervals, 0, newIntervals, 0,
                     collectionIntervals.length);
    System.arraycopy(dataSetLabels, 0, newLabels, 0, dataSetLabels.length);
    newData[data.length]            = dataValues;
    newIntervals[collectionIntervals.length] = collectionInterval;
    newLabels[dataSetLabels.length] = dataSetLabel;
    data                = newData;
    collectionIntervals = newIntervals;
    dataSetLabels       = newLabels;

    if (((dataValues.length + 1) * collectionInterval) > numSeconds)
    {
      numSeconds = ((dataValues.length + 1) * collectionInterval);
    }

    for (int i=0; i < dataValues.length; i++)
    {
      final double value = dataValues[i];
      if (! (ignoreZeroValues && Double.isNaN(value)))
      {
        if (value > max)
        {
          max = value;
        }

        if (value < min)
        {
          min = value;
        }

        sx  += (collectionInterval*i);
        sy  += value;
        sxx += (collectionInterval*collectionInterval*i*i);
        syy += (value*value);
        sxy += (collectionInterval*i*value);
        n++;
      }
    }
  }



  /**
   * Adds the specified data for use in generating a stacked bar graph.
   *
   * @param  dataSetName       The overall name of the data set.
   * @param  categoryNames     The names of the categories in the data set.
   * @param  categoryAverages  The average values for each category of the data
   *                           set.
   */
  public void addStackedBarGraphDataSet(final String dataSetName,
                                        final String[] categoryNames,
                                        final double[] categoryAverages)
  {
    final String[] newSetNames = new String[dataSetNames.length+1];
    System.arraycopy(dataSetNames, 0, newSetNames, 0, dataSetNames.length);
    newSetNames[dataSetNames.length] = dataSetName;
    dataSetNames = newSetNames;

    final String[][] newCatNames = new String[this.categoryNames.length+1][];
    System.arraycopy(this.categoryNames, 0, newCatNames, 0,
                     this.categoryNames.length);
    newCatNames[this.categoryNames.length] = categoryNames;
    this.categoryNames = newCatNames;

    final double[][] newAverages = new double[dataSetAverages.length+1][];
    System.arraycopy(dataSetAverages, 0, newAverages, 0,
                     dataSetAverages.length);
    newAverages[dataSetAverages.length] = categoryAverages;
    dataSetAverages = newAverages;

    min = 0.0;
    double total = 0.0;
    for (int i=0; i < categoryAverages.length; i++)
    {
      total += categoryAverages[i];
    }
    if (total > max)
    {
      max = total;
    }
  }



  /**
   * Indicates whether line graphs should have a flat horizontal line followed
   * by a vertical line between data points, or if the points should be directly
   * connected.
   *
   * @param  flatBetweenPoints  Indicates whether line graphs should have a flat
   *                            horizontal line followed by a vertical line
   *                            between data points.
   */
  public void setFlatBetweenPoints(final boolean flatBetweenPoints)
  {
    this.flatBetweenPoints = flatBetweenPoints;
  }



  /**
   * Indicates whether the graph should ignore data intervals where the value
   * for that interval is zero.  Note that if this is to be used, it should be
   * set before any calls to <CODE>addDataSet</CODE> are made.
   *
   * @param  ignoreZeroValues  Indicates whether the graph should ignore data
   *                           intervals where the value for that interval is
   *                           zero.
   */
  public void setIgnoreZeroValues(final boolean ignoreZeroValues)
  {
    this.ignoreZeroValues = ignoreZeroValues;
  }



  /**
   * Indicates whether the graph should include a line that indicates the
   * average of all values provided.
   *
   * @param  includeAverage  Indicates whether the graph should include a line
   *                         that indicates the average of all values provided.
   */
  public void setIncludeAverage(final boolean includeAverage)
  {
    this.includeAverage = includeAverage;
  }



  /**
   * Indicates whether the graph should include a trend line based on a linear
   * regression calculation of all the values.
   *
   * @param  includeRegression  Indicates whether the graph should include a
   *                            regression line.
   */
  public void setIncludeRegression(final boolean includeRegression)
  {
    this.includeRegression = includeRegression;
  }



  /**
   * Indicates whether the lower bound of the graph should be at zero or should
   * be dynamically calculated based on information in the data set.
   *
   * @param  baseAtZero  Indicates whether the lower bound of the graph should
   *                     be at zero or should be dynamically calculated based on
   *                     information in the data set.
   */
  public void setBaseAtZero(final boolean baseAtZero)
  {
    this.baseAtZero = baseAtZero;
  }



  /**
   * Indicates whether the generated graph should include a legend.
   *
   * @param  includeLegend  Indicates whether the generated graph should
   *                        include a legend.
   * @param  legendTitle    The title to use for the legend if it is included.
   */
  public void setIncludeLegend(final boolean includeLegend,
                               final String legendTitle)
  {
    this.includeLegend = includeLegend;
    this.legendTitle   = legendTitle;
  }



  /**
   * Indicates whether the generated graph should include horizontal grid lines.
   *
   * @param  includeHorizontalGrid  Indicates whether the generated graph should
   *                                include horizontal grid lines.
   */
  public void setIncludeHorizontalGrid(final boolean includeHorizontalGrid)
  {
    this.includeHorizontalGrid = includeHorizontalGrid;
  }



  /**
   * Indicates whether the generated graph should include horizontal grid lines.
   *
   * @param  includeVerticalGrid  Indicates whether the generated graph should
   *                              include vertical grid lines.
   */
  public void setIncludeVerticalGrid(final boolean includeVerticalGrid)
  {
    this.includeVerticalGrid = includeVerticalGrid;
  }



  /**
   * Indicates whether the generated pie graph should show the percentages for
   * each category.
   *
   * @param  showPercentages  Indicates whether the generated pie graph should
   *                          show the percentages for each category.
   */
  public void setShowPercentages(final boolean showPercentages)
  {
    this.showPercentages = showPercentages;
  }



  /**
   * Specifies the title to be used for the horizontal axis of the generated
   * graph.
   *
   * @param  horizontalAxisTitle  The title to be used for the horizontal axis
   *                              of the generated graph.
   */
  public void setHorizontalAxisTitle(final String horizontalAxisTitle)
  {
    this.horizontalAxisTitle = horizontalAxisTitle;
  }



  /**
   * Specifies the title to be used for the vertical axis of the generated
   * graph.
   *
   * @param  verticalAxisTitle  The title to be used for the vertical axis of
   *                            the generated graph.
   */
  public void setVerticalAxisTitle(final String verticalAxisTitle)
  {
    this.verticalAxisTitle = verticalAxisTitle;
  }



  /**
   * Specifies the number of seconds into the test that the graph starts.
   *
   * @param  startSeconds  The number of seconds into the test that the graph
   *                       starts.
   */
  public void setStartSeconds(final int startSeconds)
  {
    this.startSeconds = startSeconds;
  }



  /**
   * Generates a pie graph based on the information that has been provided.
   *
   * @param  categoryNames           The names of the categories of each of the
   *                                 elements.
   * @param  occurrencesPerCategory  The number of occurrences of the tracked
   *                                 event in each category.
   *
   * @return  A buffered image containing the generated pie graph.
   */
  public BufferedImage generatePieGraph(final String[] categoryNames,
                                        final int[] occurrencesPerCategory)
  {
    // Calculate percentages based on the information provided.
    int totalOccurrences = 0;
    final String[] names = new String[categoryNames.length];
    System.arraycopy(categoryNames, 0, names, 0, categoryNames.length);
    for (int i=0; i < occurrencesPerCategory.length; i++)
    {
      totalOccurrences += occurrencesPerCategory[i];
    }
    final double[] percentages = new double[occurrencesPerCategory.length];
    for (int i=0; i < percentages.length; i++)
    {
      percentages[i] = 1.0 * occurrencesPerCategory[i] / totalOccurrences;
      if (showPercentages)
      {
        names[i] += " (" + decimalFormat.format(percentages[i] * 100) + "%)";
      }
    }


    // Convert those percentages to degrees.
    int totalDegrees = 0;
    final double[] dblDegrees = new double[percentages.length];
    final int[] degrees = new int[percentages.length];
    for (int i=0; i < percentages.length; i++)
    {
      dblDegrees[i] = 360.0 * percentages[i];
      degrees[i] = (int) Math.round(360.0 * percentages[i]);
      totalDegrees += degrees[i];
    }


    // Because the number of degrees in the arc must be an integer, rounding
    // errors can occur that make the total number of degrees in the pie greater
    // or less than 360.  If that occurs, then "play with" the numbers a little
    // until we get something that rounds to 360.  Although I haven't done the
    // math, it's probably possible for this to cause an infinite loop.  To
    // prevent that from occurring, limit the number of times the numbers will
    // be nudged.
    double fudgeFactor = 0.1;
    int    iterations  = 0;
    while ((totalDegrees != 360) && (iterations < 20))
    {
      if (totalDegrees > 360)
      {
        totalDegrees = 0;
        for (int i=0; i < degrees.length; i++)
        {
          dblDegrees[i] -= fudgeFactor;
          degrees[i] = (int) Math.round(dblDegrees[i]);
          totalDegrees += degrees[i];
        }
      }
      else
      {
        totalDegrees = 0;
        for (int i=0; i < degrees.length; i++)
        {
          dblDegrees[i] += fudgeFactor;
          degrees[i] = (int) Math.round(dblDegrees[i]);
          totalDegrees += degrees[i];
        }
      }

      fudgeFactor /= 2;
      iterations++;
    }



    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // Draw the title at the top of the graph.
    g.setFont(titleFont);
    final int captionWidth = titleMetrics.stringWidth(graphTitle);
    int x = (width - captionWidth) / 2;
    int y = titleMetrics.getHeight() + 2;
    g.drawString(graphTitle, x, y);


    // Add the legend along the side.
    int legendWidth = legendTitleMetrics.getHeight() + 2;
    if (includeLegend)
    {
      final int captionHeight = legendTitleMetrics.getHeight();
      final int captionAscent = legendItemMetrics.getAscent();

      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);
      for (int i=0; i < names.length; i++)
      {
        final int labelWidth = legendItemMetrics.stringWidth(names[i]) +
             captionHeight + 2;
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
      }

      legendWidth += maxLabelWidth + 6;
      final int legendHeight = ((names.length+1) * captionHeight) + 4;

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth+legendTitleMetrics.stringWidth(legendTitle))/2) - 6;
      int labelY = ((height - legendHeight) / 2) + captionHeight + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      g.setFont(legendItemFont);
      labelY += captionHeight;

      // Draw the legend entries using the appropriate colors.
      labelX = width - maxLabelWidth - 6;
      for (int i=0; i < names.length; i++)
      {
        g.setColor(COLORS[i % COLORS.length]);
        g.fillRect(labelX, labelY-captionAscent, captionAscent, captionAscent);

        g.setColor(Color.black);
        g.drawString(names[i], labelX + captionHeight + 2, labelY);
        labelY += captionHeight;
      }

      g.setColor(Color.black);
      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // Determine the bounds to use for the pie.
    final int usableWidth  = width - legendWidth;
    final int usableHeight = height - legendTitleMetrics.getHeight() - 2;
    final int size = Math.min(usableWidth, usableHeight) * 9 / 10;
    x = (usableWidth - size) / 2;
    y = ((usableHeight - size) / 2) + legendTitleMetrics.getHeight() + 2;


    // Draw the whole circle using the first color to hide any gaps that might
    // still remain.
    g.setColor(COLORS[0]);
    g.fillOval(x, y, size, size);


    // Generate the pie.
    int startAngle = 0;
    for (int i=0; i < degrees.length; i++)
    {
      g.setColor(COLORS[i % COLORS.length]);
      g.fillArc(x, y, size, size, startAngle, degrees[i]);
      startAngle += degrees[i];
    }


    // Return the completed image
    return image;
  }



  /**
   * Generates a line graph based on the information that has been provided.
   *
   * @return  A buffered image containing the generated line graph.
   */
  public BufferedImage generateLineGraph()
  {
    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font axisLabelFont =
         selectFont(g, "SansSerif", Font.PLAIN, 12, verticalAxisTitle, height);
    final Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 10);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics axisLabelMetrics   = g.getFontMetrics(axisLabelFont);
    final FontMetrics tickLabelMetrics   = g.getFontMetrics(tickLabelFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // Calculate the real max and min values to use for the Y axis.  There will
    // be a 5% border on the top and bottom.  Note that if all values were the
    // same for the entire span, we need to "adjust" the numbers so that the
    // graph will render.
    double span  = max - min;
    if (span < 0.00001)
    {
      span += 1.0;
      max  += 0.5;
      if (min >= 0.5)
      {
        min  -= 0.5;
      }
    }

    graphMax  = max / 0.95;
    if (baseAtZero)
    {
      graphMin = 0;
    }
    else
    {
      graphMin  = min - (graphMax * 0.05);

      // In case the graph min would be negative where the real min doesn't go
      // below zero, then make the graph min zero.
      if ((graphMin < 0) && (min >= 0))
      {
        graphMin = 0;
      }

      // Otherwise, see if we can decrease the minimum by a little to make it a
      // relatively nice number.  Do this by finding the largest power of ten
      // that is less than or equal to the span and making the minimum value in
      // the graph a multiple of that power of ten.
      graphSpan = graphMax - graphMin;
      final int largestPowerOfTen = (int) (Math.log(graphSpan) / Math.log(10));
      if (largestPowerOfTen > 0)
      {
        graphMin = ((int) (graphMin / Math.pow(10, largestPowerOfTen))) *
                   Math.pow(10, largestPowerOfTen);
      }
    }
    graphSpan = graphMax - graphMin;


    // The top of the graph will be just below the bottom of the title, and
    // the bottom of the graph will be twice the distance from the bottom of the
    // image.
    upperLeftY = titleMetrics.getHeight() + 5;
    originY = height - (2 * axisLabelMetrics.getHeight()) -
         tickLabelMetrics.getHeight();
    graphHeight = originY - upperLeftY + 1;


    // Add the legend along the side.
    int legendWidth = axisLabelMetrics.getHeight() + 2;
    if (includeLegend)
    {
      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);
      for (int i=0; i < dataSetLabels.length; i++)
      {
        final int labelWidth =
             legendItemMetrics.stringWidth(dataSetLabels[i]) +
             legendItemMetrics.getHeight() + 2;
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
      }

      legendWidth += maxLabelWidth + 6;
      final int legendHeight = 4 + legendTitleMetrics.getHeight() +
           (dataSetLabels.length * legendItemMetrics.getHeight());

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth + legendTitleMetrics.stringWidth(legendTitle)) / 2) -
           6;
      int labelY = ((height - legendHeight) / 2) +
           legendTitleMetrics.getAscent() + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      labelY += legendTitleMetrics.getHeight();
      g.setFont(legendItemFont);

      // Draw the legend entries using the appropriate colors.
      labelX = width - maxLabelWidth - 6;
      for (int i=0; i < dataSetLabels.length; i++)
      {
        final int captionAscent = legendItemMetrics.getAscent();
        final int captionHeight = legendItemMetrics.getHeight();
        g.setColor(COLORS[i % COLORS.length]);
        g.fillRect(labelX, labelY-captionAscent, captionAscent, captionAscent);
        g.setColor(Color.black);
        g.drawString(dataSetLabels[i], labelX + captionHeight + 2, labelY);
        labelY += captionHeight;
      }

      g.setColor(Color.black);
      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // The graph should be centered horizontally, so make the right border the
    // same distance from the image edge as the left border.
    lowerRightX = width - legendWidth;


    // Draw the vertical axis caption.  It should be rotated 90 degrees, which
    // makes for some funky math.  Just trust that this works.
    g.rotate(1.5*Math.PI);
    int captionWidth  = axisLabelMetrics.stringWidth(verticalAxisTitle);
    g.setFont(axisLabelFont);
    g.drawString(verticalAxisTitle, -((height+captionWidth)/2),
                 (axisLabelMetrics.getHeight()-2));
    g.rotate(Math.PI/2);


    // Figure out how many labels to draw along the vertical axis.  It will
    // basically be double-spaced, so figure out how many labels could fit along
    // the vertical axis and divide by 2.
    final int numVerticalLabels =
         graphHeight / tickLabelMetrics.getHeight() / 2;


    // If there is space for more vertical labels than there are integers, then
    // go up by integer values.  Otherwise, use the calculated number of labels.
    int labelX = 5 + axisLabelMetrics.getHeight();
    int labelY = originY;
    int maxLabelWidth = 0;
    g.setFont(tickLabelFont);
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);

          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);
        final String label = decimalFormat.format(valueY);
        final int labelWidth = tickLabelMetrics.stringWidth(label);
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
        g.drawString(label, labelX,
             (labelY + (tickLabelMetrics.getHeight()/2)));
      }
    }


    // Draw the vertical axis and add the tick marks along its side
    originX = labelX + maxLabelWidth + 5;
    g.setColor(new Color(0xF1, 0xF1, 0xF1));
    g.fillRect(originX, upperLeftY, (lowerRightX - originX),
         (originY - upperLeftY));
    g.setColor(Color.BLACK);
    g.drawLine(originX, upperLeftY, originX, originY);
    labelY = originY;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);

        if (includeHorizontalGrid)
        {
          g.setColor(Color.LIGHT_GRAY);
          g.drawLine(originX, labelY, lowerRightX, labelY);
          g.setColor(Color.BLACK);
        }
        g.drawLine((originX - 2), labelY, (originX + 2), labelY);
      }
    }


    // We know that the number of intervals will go from startSeconds to
    // startSeconds+n, so the width of startSeconds+n should be roughly equal to
    // the greatest width.
    final String label = String.valueOf(startSeconds+numSeconds);
    maxLabelWidth = tickLabelMetrics.stringWidth(label);
    final int maxHorizontalLabels = (lowerRightX - originX) / maxLabelWidth / 2;
    int secondsPerInterval;
    if ((numSeconds > maxHorizontalLabels) && (maxHorizontalLabels > 0))
    {
      secondsPerInterval = numSeconds / maxHorizontalLabels;
      if ((secondsPerInterval % collectionIntervals[0]) != 0)
      {
        secondsPerInterval = (secondsPerInterval / collectionIntervals[0] + 1) *
                             collectionIntervals[0];
      }
    }
    else
    {
      secondsPerInterval = 1;
    }



    // Draw the labels at the bottom of the horizontal axis.  The tick marks
    // can also be added at the same time.
    labelY = originY + tickLabelMetrics.getHeight() + 2;
    for (int value = 0; value < numSeconds; value += secondsPerInterval)
    {
      final String valueString = String.valueOf(startSeconds+value);
      labelX = valueXToGraphX(value);
      final int width = tickLabelMetrics.stringWidth(valueString);
      g.drawString(valueString, (labelX-(width/2)), labelY);
      if (includeVerticalGrid && (value != 0))
      {
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(labelX, originY, labelX, upperLeftY);
        g.setColor(Color.BLACK);
      }
      g.drawLine(labelX, (originY - 2), labelX, (originY + 2));
    }


    // Now draw the horizontal axis.
    g.drawLine(originX, originY, lowerRightX, originY);


    // Add the caption at the top of the image
    g.setFont(titleFont);
    captionWidth = titleMetrics.stringWidth(graphTitle);
    int captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    int captionY = titleMetrics.getAscent() + 2;
    g.drawString(graphTitle, captionX, captionY);
    g.setFont(new Font("SansSerif", Font.PLAIN, 14));


    // Add the horizontal axis caption at the bottom of the image
    captionWidth = axisLabelMetrics.stringWidth(horizontalAxisTitle);
    captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    captionY = (height - axisLabelMetrics.getDescent() - 2);
    g.setFont(axisLabelFont);
    g.drawString(horizontalAxisTitle, captionX, captionY);


    // Iterate through the data and make the line graphs.  Make sure that we
    // don't draw more data sets than we have colors available.
    final Stroke defaultStroke = g.getStroke();
    g.setStroke(new BasicStroke(2.0f));

    for (int i=0; ((i < data.length) && (i < COLORS.length)); i++)
    {
      g.setColor(COLORS[i]);

      int j, x1, x2, y1, y2;
      for (j=0; ((j < data[i].length) && ignoreZeroValues &&
                 Double.isNaN(data[i][j])); j++);
      if (j >= data[i].length)
      {
        continue;
      }

      x1 = valueXToGraphX((j+1)*collectionIntervals[i]);
      y1 = valueYToGraphY(data[i][j]);

      for (j=j+1; j < data[i].length; j++)
      {
        if (! (ignoreZeroValues && Double.isNaN(data[i][j])))
        {
          x2 = valueXToGraphX((j+1) * collectionIntervals[i]);
          y2 = valueYToGraphY(data[i][j]);

          if (flatBetweenPoints)
          {
            g.drawLine(x1, y1, x2, y1);
            g.drawLine(x2, y1, x2, y2);
          }
          else
          {
            g.drawLine(x1, y1, x2, y2);
          }

          x1 = x2;
          y1 = y2;
        }
      }
    }


    // Draw a line with the average value, if specified.
    g.setStroke(defaultStroke);
    g.setColor(Color.BLACK);
    if (includeAverage)
    {
      final int y = valueYToGraphY(sy/n);
      g.drawLine(originX, y, lowerRightX, y);
    }


    // Draw the regression line, if specified.
    if (includeRegression)
    {
      final double b = (sxy - (sx*sy)/n) / (sxx - (sx*sx)/n);
      final double a = (sy - b*sx) / n;

      final int y1 = valueYToGraphY(a);
      final int y2 = valueYToGraphY(a + b*(numSeconds-1));
      g.drawLine(originX, y1, lowerRightX, y2);
    }

    return image;
  }



  /**
   * Generates a line graph by plotting the provided data and connecting those
   * points with lines.
   *
   * @param  xValues      The sets of x coordinates for the data to graph.
   * @param  yValues      The sets of y coordinates for the data to graph.
   * @param  labels       The labels to use in the legend.
   * @param  drawPoints   Indicates whether the individual data points should be
   *                      clearly marked with dots.
   * @param  baseXAtZero  Indicates whether to start the x coordinates at zero
   *                      or the first x coordinate.
   *
   * @return  A buffered image containing the generated line graph.
   */
  public BufferedImage generateXYLineGraph(final double[][] xValues,
                                           final double[][] yValues,
                                           final String[] labels,
                                           final boolean drawPoints,
                                           final boolean baseXAtZero)
  {
    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // If there was no data provided, or if the amount of data provided isn't
    // suited for graphing, then return an empty image.
    if ((xValues == null) || (xValues.length == 0) || (yValues == null) ||
        (yValues.length == 0) || (yValues.length != xValues.length))
    {
      if ((xValues == null) || (yValues == null) || (xValues.length == 0) ||
          (yValues.length == 0))
      {
        System.err.println("Unable to generate XY line graph -- No data " +
                           "provided for x and/or y coordinates.");
      }
      else
      {
        System.err.println("Unable to generate XY line graph -- Number of X " +
                           "coordinates does not match number of Y " +
                           "coordinates.");
      }

      return image;
    }


    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font axisLabelFont =
         selectFont(g, "SansSerif", Font.PLAIN, 12, verticalAxisTitle, height);
    final Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 10);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics axisLabelMetrics   = g.getFontMetrics(axisLabelFont);
    final FontMetrics tickLabelMetrics   = g.getFontMetrics(tickLabelFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // Calculate the real max and min values to use for the Y axis.  There will
    // be a 5% border on the top and bottom.  Note that if all values were the
    // same for the entire span, we need to "adjust" the numbers so that the
    // graph will render.
    double xMax   = xValues[0][0];
    double xMin   = xValues[0][0];
    double yMax   = yValues[0][0];
    double yMin   = yValues[0][0];
    double yAvg   = 0.0;
    int    yCount = 0;
    for (int i=0; i < xValues.length; i++)
    {
      if (xValues[i].length != yValues[i].length)
      {
        System.err.println("Unable to generate XY line graph -- Number of " +
                           "x and y coordinates are not the same.");
        return image;
      }

      for (int j=0; j < xValues[i].length; j++)
      {
        if (xValues[i][j] > xMax)
        {
          xMax = xValues[i][j];
        }

        if (xValues[i][j] < xMin)
        {
          if (j == 0)
          {
            xMin = xValues[i][j];
          }
          else
          {
            System.err.println("Unable to generate XY line graph -- X " +
                               "coordinates are not in increasing order.");
            return image;
          }
        }

        if (yValues[i][j] > yMax)
        {
          yMax = yValues[i][j];
        }

        if (yValues[i][j] < yMin)
        {
          yMin = yValues[i][j];
        }

        yAvg += yValues[i][j];
        yCount++;
      }
    }

    yAvg = yAvg / yCount;
    double ySpan = (yMax - yMin);
    if (ySpan < 0.00001)
    {
      ySpan += 1.0;
      yMax  += 0.5;
      if (yMin >= 0.5)
      {
        yMin  -= 0.5;
      }
    }

    if (baseXAtZero)
    {
      if (xMin < 0)
      {
        System.err.println("Unable to generate XY line graph -- Smallest x " +
                           "coordinate is less than zero.");
        return image;
      }

      xMin = 0.0;
    }

    final double xSpan = (xMax - xMin);
    if (xSpan < 0.00001)
    {
      System.err.println("Unable to generate XY line graph -- Span of x " +
                         "values is too small.");
      return image;
    }

    graphMax  = yMax / 0.95;
    if (baseAtZero)
    {
      graphMin = 0;
    }
    else
    {
      graphMin  = yMin - (graphMax * 0.05);

      // In case the graph min would be negative where the real min doesn't go
      // below zero, then make the graph min zero.
      if ((graphMin < 0) && (yMin >= 0))
      {
        graphMin = 0;
      }

      // Otherwise, see if we can decrease the minimum by a little to make it a
      // relatively nice number.  Do this by finding the largest power of ten
      // that is less than or equal to the span and making the minimum value in
      // the graph a multiple of that power of ten.
      graphSpan = graphMax - graphMin;
      final int largestPowerOfTen = (int) (Math.log(graphSpan) / Math.log(10));
      if (largestPowerOfTen > 0)
      {
        graphMin = ((int) (graphMin / Math.pow(10, largestPowerOfTen))) *
                   Math.pow(10, largestPowerOfTen);
      }
    }
    graphSpan = graphMax - graphMin;


    // The top of the graph will be just below the bottom of the caption, and
    // the bottom of the graph will be twice the distance from the bottom of the
    // image.
    upperLeftY = titleMetrics.getHeight() + 5;
    originY = height - (2*upperLeftY);
    graphHeight = originY - upperLeftY + 1;


    int legendWidth = axisLabelMetrics.getHeight() + 2;
    if (includeLegend)
    {
      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);
      for (int i=0; i < labels.length; i++)
      {
        final int labelWidth =
             legendItemMetrics.stringWidth(labels[i]) +
             legendItemMetrics.getHeight() + 2;
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
      }

      legendWidth += maxLabelWidth + 6;
      final int legendHeight = 4 + legendTitleMetrics.getHeight() +
           (dataSetLabels.length * legendItemMetrics.getHeight());

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth+legendTitleMetrics.stringWidth(legendTitle))/2) - 6;
      int labelY = ((height - legendHeight) / 2) +
           legendTitleMetrics.getHeight() + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      g.setFont(legendItemFont);
      labelY += legendTitleMetrics.getHeight();

      // Draw the legend entries using the appropriate colors.
      labelX = width - maxLabelWidth - 6;
      for (int i=0; i < labels.length; i++)
      {
        final int captionAscent = legendItemMetrics.getAscent();
        final int captionHeight = legendItemMetrics.getHeight();
        g.setColor(COLORS[i % COLORS.length]);
        g.fillRect(labelX, labelY-captionAscent, captionAscent, captionAscent);

        g.setColor(Color.black);
        g.drawString(labels[i], labelX + captionHeight + 2, labelY);
        labelY += captionHeight;
      }

      g.setColor(Color.black);
      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // The graph should be centered horizontally, so make the right border the
    // same distance from the image edge as the left border.
    lowerRightX = width - legendWidth;


    // Draw the vertical axis caption.  It should be rotated 90 degrees, which
    // makes for some funky math.  Just trust that this works.
    g.rotate(1.5*Math.PI);
    int captionWidth  = axisLabelMetrics.stringWidth(verticalAxisTitle);
    g.setFont(axisLabelFont);
    g.drawString(verticalAxisTitle, -((height+captionWidth)/2),
                 (axisLabelMetrics.getHeight()-2));
    g.setFont(tickLabelFont);
    g.rotate(Math.PI/2);


    // Figure out how many labels to draw along the vertical axis.  It will
    // basically be double-spaced, so figure out how many labels could fit along
    // the vertical axis and divide by 2.
    final int numVerticalLabels =
         graphHeight / tickLabelMetrics.getHeight() / 2;


    // If there is space for more vertical labels than there are integers, then
    // go up by integer values.  Otherwise, use the calculated number of labels.
    int labelX = 5 + axisLabelMetrics.getHeight();
    int labelY = originY;
    int maxLabelWidth = 0;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);

          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);
        final String label = decimalFormat.format(valueY);
        final int labelWidth = tickLabelMetrics.stringWidth(label);
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
        g.drawString(label, labelX,
             (labelY + (tickLabelMetrics.getHeight()/2)));
      }
    }


    // Draw the vertical axis and add the tick marks along its side
    originX = labelX + maxLabelWidth + 5;
    g.setColor(new Color(0xF1, 0xF1, 0xF1));
    g.fillRect(originX, upperLeftY, (lowerRightX - originX),
         (originY - upperLeftY));
    g.setColor(Color.BLACK);
    g.drawLine(originX, upperLeftY, originX, originY);
    labelY = originY;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);

        if (includeHorizontalGrid)
        {
          g.setColor(Color.LIGHT_GRAY);
          g.drawLine(originX, labelY, lowerRightX, labelY);
          g.setColor(Color.BLACK);
        }
        g.drawLine((originX - 2), labelY, (originX + 2), labelY);
      }
    }


    // We know that the x values are in increasing order, so the greatest width
    // should be roughly equal to the width of the last value.  We'll use the
    // same logic to determine the label increment as we used for the vertical
    // axis.
    final String label  = decimalFormat.format(xMax);
    maxLabelWidth = tickLabelMetrics.stringWidth(label);
    final int maxHorizontalLabels = (lowerRightX - originX) / maxLabelWidth / 2;
    final double secondsPerInterval =
         chooseVerticalLabelIncrement(maxHorizontalLabels, xSpan);



    // Draw the labels at the bottom of the horizontal axis.  The tick marks
    // can also be added at the same time.
    labelY = originY + tickLabelMetrics.getHeight() + 2;
    for (double value = 0.0; value < xMax; value += secondsPerInterval)
    {
      final String valueString = decimalFormat.format(xMin+value);
      labelX = valueXToGraphX(value, xMax);
      final int width = tickLabelMetrics.stringWidth(valueString);
      g.drawString(valueString, (labelX-(width/2)), labelY);
      if (includeVerticalGrid && (value != 0))
      {
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(labelX, originY, labelX, upperLeftY);
        g.setColor(Color.BLACK);
      }
      g.drawLine(labelX, (originY - 2), labelX, (originY + 2));
    }


    // Now draw the horizontal axis.
    g.drawLine(originX, originY, lowerRightX, originY);


    // Add the caption at the top of the image
    captionWidth = titleMetrics.stringWidth(graphTitle);
    int captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    int captionY = titleMetrics.getAscent() + 2;
    g.setFont(titleFont);
    g.drawString(graphTitle, captionX, captionY);


    // Add the horizontal axis caption at the bottom of the image
    captionWidth = axisLabelMetrics.stringWidth(horizontalAxisTitle);
    captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    captionY = (height - 2);
    g.setFont(axisLabelFont);
    g.drawString(horizontalAxisTitle, captionX, captionY);


    // Iterate through the data and generate the graph.
    final Stroke defaultStroke = g.getStroke();
    g.setStroke(new BasicStroke(2.0f));
    for (int i=0; i < xValues.length; i++)
    {
      g.setColor(COLORS[i]);
      double x1 = xValues[i][0];
      double y1 = yValues[i][0];
      for (int j=1; j < xValues[i].length; j++)
      {
        final double x2 = xValues[i][j];
        final double y2 = yValues[i][j];

        final int graphX1 = valueXToGraphX(x1, xMax);
        final int graphX2 = valueXToGraphX(x2, xMax);
        final int graphY1 = valueYToGraphY(y1);
        final int graphY2 = valueYToGraphY(y2);

        if (flatBetweenPoints)
        {
          g.drawLine(graphX1, graphY1, graphX2, graphY1);
          g.drawLine(graphX2, graphY1, graphX2, graphY2);
        }
        else
        {
          g.drawLine(graphX1, graphY1, graphX2, graphY2);
        }

        if (drawPoints)
        {
          g.fillOval(graphX1-2, graphY1-2, 5, 5);
          g.fillOval(graphX2-2, graphY2-2, 5, 5);
        }

        x1 = x2;
        y1 = y2;
      }
    }


    // Draw a line with the average value, if specified.
    g.setStroke(defaultStroke);
    g.setColor(Color.BLACK);
    if (includeAverage)
    {
      final int y = valueYToGraphY(yAvg);
      g.drawLine(originX, y, lowerRightX, y);
    }

    return image;
  }



  /**
   * Generates a stacked area graph based on the information that has been
   * provided.  Note that all data points provided must have exactly the same
   * number of elements.
   *
   * @return  A buffered image containing the generated stacked area graph.
   */
  public BufferedImage generateStackedAreaGraph()
  {
    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font axisLabelFont =
         selectFont(g, "SansSerif", Font.PLAIN, 12, verticalAxisTitle, height);
    final Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 10);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics axisLabelMetrics   = g.getFontMetrics(axisLabelFont);
    final FontMetrics tickLabelMetrics   = g.getFontMetrics(tickLabelFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // Since we are doing a stacked graph, we need to add all the values
    // together for each data point and use that as the max.  The min will
    // always be zero.
    min = 0.0;
    max = 0.0;
    for (int i=0; i < data[0].length; i++)
    {
      double total = 0.0;
      for (int j=0; j < data.length; j++)
      {
        total += data[j][i];
      }
      if (total > max)
      {
        max = total;
      }
    }


    // Calculate the real max and min values to use for the Y axis.  There will
    // be a 5% border on the top and bottom.  Note that if all values were the
    // same for the entire span, we need to "adjust" the numbers so that the
    // graph will render.
    double span  = max - min;
    if (span < 0.00001)
    {
      span += 1.0;
      max  += 0.5;
      if (min >= 0.5)
      {
        min  -= 0.5;
      }
    }

    graphMax  = max / 0.95;
    if (baseAtZero)
    {
      graphMin = 0;
    }
    else
    {
      graphMin  = min - (graphMax * 0.05);

      // In case the graph min would be negative where the real min doesn't go
      // below zero, then make the graph min zero.
      if ((graphMin < 0) && (min >= 0))
      {
        graphMin = 0;
      }

      // Otherwise, see if we can decrease the minimum by a little to make it a
      // relatively nice number.  Do this by finding the largest power of ten
      // that is less than or equal to the span and making the minimum value in
      // the graph a multiple of that power of ten.
      graphSpan = graphMax - graphMin;
      final int largestPowerOfTen = (int) (Math.log(graphSpan) / Math.log(10));
      if (largestPowerOfTen > 0)
      {
        graphMin = ((int) (graphMin / Math.pow(10, largestPowerOfTen))) *
                   Math.pow(10, largestPowerOfTen);
      }
    }
    graphSpan = graphMax - graphMin;


    // The top of the graph will be just below the bottom of the caption, and
    // the bottom of the graph will be twice the distance from the bottom of the
    // image.
    upperLeftY = titleMetrics.getHeight() + 5;
    originY = height - (2 * axisLabelMetrics.getHeight()) -
         tickLabelMetrics.getHeight();
    graphHeight = originY - upperLeftY + 1;


    // Add the legend along the side.
    int legendWidth = axisLabelMetrics.getHeight() + 2;
    if (includeLegend)
    {
      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);
      for (int i=0; i < dataSetLabels.length; i++)
      {
        final int labelWidth =
             legendItemMetrics.stringWidth(dataSetLabels[i]) +
             legendItemMetrics.getHeight() + 2;
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
      }

      legendWidth += maxLabelWidth + 6;
      final int legendHeight = 4 + legendTitleMetrics.getHeight() +
           (dataSetLabels.length * legendItemMetrics.getHeight());

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth + legendTitleMetrics.stringWidth(legendTitle)) / 2) -
           6;
      int labelY = ((height - legendHeight) / 2) +
           legendTitleMetrics.getAscent() + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      g.setFont(legendItemFont);
      labelY += legendTitleMetrics.getHeight();

      // Draw the legend entries using the appropriate colors.
      labelX = width - maxLabelWidth - 6;
      for (int i=0; i < dataSetLabels.length; i++)
      {
        final int captionAscent = legendItemMetrics.getAscent();
        final int captionHeight = legendItemMetrics.getHeight();
        g.setColor(COLORS[i % COLORS.length]);
        g.fillRect(labelX, labelY-captionAscent, captionAscent, captionAscent);

        g.setColor(Color.black);
        g.drawString(dataSetLabels[i], labelX + captionHeight + 2, labelY);
        labelY += captionHeight;
      }

      g.setColor(Color.black);
      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // The graph should be centered horizontally, so make the right border the
    // same distance from the image edge as the left border.
    lowerRightX = width - legendWidth;


    // Draw the vertical axis caption.  It should be rotated 90 degrees, which
    // makes for some funky math.  Just trust that this works.
    g.rotate(1.5*Math.PI);
    int captionWidth  = axisLabelMetrics.stringWidth(verticalAxisTitle);
    g.setFont(axisLabelFont);
    g.drawString(verticalAxisTitle, -((height+captionWidth)/2),
                 (axisLabelMetrics.getHeight()-2));
    g.setFont(tickLabelFont);
    g.rotate(Math.PI/2);


    // Figure out how many labels to draw along the vertical axis.  It will
    // basically be double-spaced, so figure out how many labels could fit along
    // the vertical axis and divide by 2.
    final int numVerticalLabels =
         graphHeight / tickLabelMetrics.getHeight() / 2;


    // If there is space for more vertical labels than there are integers, then
    // go up by integer values.  Otherwise, use the calculated number of labels.
    int labelX = 5 + axisLabelMetrics.getHeight();
    int labelY = originY;
    int maxLabelWidth = 0;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);
        final String label = decimalFormat.format(valueY);
        final int labelWidth = tickLabelMetrics.stringWidth(label);
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
        g.drawString(label, labelX,
             (labelY + (tickLabelMetrics.getHeight()/2)));
      }
    }


    // Draw the vertical axis and add the tick marks along its side
    originX = labelX + maxLabelWidth + 5;
    g.drawLine(originX, upperLeftY, originX, originY);
    labelY = originY;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);

        if (includeHorizontalGrid)
        {
          g.setColor(Color.LIGHT_GRAY);
          g.drawLine(originX, labelY, lowerRightX, labelY);
          g.setColor(Color.BLACK);
        }
        g.drawLine((originX - 2), labelY, (originX + 2), labelY);
      }
    }


    // We know that the number of intervals will go from zero to n, so the
    // width of n should be roughly equal to the greatest width.
    final String label = String.valueOf(numSeconds);
    maxLabelWidth = tickLabelMetrics.stringWidth(label);
    final int maxHorizontalLabels = (lowerRightX - originX) / maxLabelWidth / 2;
    int secondsPerInterval;
    if ((numSeconds > maxHorizontalLabels) && (maxHorizontalLabels > 0))
    {
      secondsPerInterval = numSeconds / maxHorizontalLabels;
      if ((secondsPerInterval % collectionIntervals[0]) != 0)
      {
        secondsPerInterval = (secondsPerInterval / collectionIntervals[0] + 1) *
                             collectionIntervals[0];
      }
    }
    else
    {
      secondsPerInterval = 1;
    }



    // Draw the labels at the bottom of the horizontal axis.  The tick marks
    // can also be added at the same time.
    labelY = originY + tickLabelMetrics.getHeight() + 2;
    for (int value = 0; value < numSeconds; value += secondsPerInterval)
    {
      final String valueString = String.valueOf(value);
      labelX = valueXToGraphX(value);
      final int width = tickLabelMetrics.stringWidth(valueString);
      g.drawString(valueString, (labelX-(width/2)), labelY);
      if (includeVerticalGrid && (value != 0))
      {
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(labelX, originY, labelX, upperLeftY);
        g.setColor(Color.BLACK);
      }
      g.drawLine(labelX, (originY - 2), labelX, (originY + 2));
    }


    // Now draw the horizontal axis.
    g.drawLine(originX, originY, lowerRightX, originY);


    // Add the caption at the top of the image
    captionWidth = titleMetrics.stringWidth(graphTitle);
    int captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    int captionY = titleMetrics.getAscent() + 2;
    g.setFont(titleFont);
    g.drawString(graphTitle, captionX, captionY);


    // Add the horizontal axis caption at the bottom of the image
    captionWidth = axisLabelMetrics.stringWidth(horizontalAxisTitle);
    captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    captionY = (height - 2);
    g.setFont(axisLabelFont);
    g.drawString(horizontalAxisTitle, captionX, captionY);


    // Iterate through the data and make the area graphs.  Make sure that we
    // don't draw more data sets than we have colors available.
    final double[] currentTotals = new double[data[0].length];
    for (int i=0; ((i < data.length) && (i < COLORS.length)); i++)
    {
      g.setColor(COLORS[i]);

      int j, o1, o2, x1, x2, y1, y2;
      for (j=0; ((j < data[i].length) && ignoreZeroValues &&
                 Double.isNaN(data[i][j])); j++);
      if (j >= data[i].length)
      {
        continue;
      }

      x1 = valueXToGraphX((j+1)*collectionIntervals[i]);
      y1 = valueYToGraphY(data[i][j] + currentTotals[j]);
      o1 = valueYToGraphY(currentTotals[j]);
      currentTotals[j] += data[i][j];

      for (j=j+1; j < data[i].length; j++)
      {
        if (! (ignoreZeroValues && Double.isNaN(data[i][j])))
        {
          x2 = valueXToGraphX((j+1) * collectionIntervals[i]);
          y2 = valueYToGraphY(data[i][j] + currentTotals[j]);
          o2 = valueYToGraphY(currentTotals[j]);
          currentTotals[j] += data[i][j];

          final int[] xPoints = new int[] { x1, x2, x2, x1 };
          final int[] yPoints = new int[] { y1, y2, o2, o1 };
          g.fill(new Polygon(xPoints, yPoints, 4));

          x1 = x2;
          y1 = y2;
          o1 = o2;
        }
      }
    }


    // Draw a line with the average value, if specified.
    if (includeAverage)
    {
      final int y = valueYToGraphY(sy/n);
      g.drawLine(originX, y, lowerRightX, y);
    }


    // Draw the regression line, if specified.
    if (includeRegression)
    {
      final double b = (sxy - (sx*sy)/n) / (sxx - (sx*sx)/n);
      final double a = (sy - b*sx) / n;

      final int y1 = valueYToGraphY(a);
      final int y2 = valueYToGraphY(a + b*(numSeconds-1));
      g.drawLine(originX, y1, lowerRightX, y2);
    }

    return image;
  }



  /**
   * Generates a line graph that can be used to overlay data for two different
   * statistics.  The left X axis will be used for the first statistic and the
   * right X axis will be used for the second.
   *
   * @param  label1                 The label for the first statistic.
   * @param  caption1               The caption that should be used for the
   *                                vertical axis for the first statistic.
   * @param  values1                The actual data values for the first
   *                                statistic.
   * @param  interval1              The collection interval used when gathering
   *                                the data for the first statistic.
   * @param  label2                 The label for the second statistic.
   * @param  caption2               The caption that should be used for the
   *                                vertical axis for the second statistic.
   * @param  values2                The actual data values for the second
   *                                statistic.
   * @param  interval2              The collection interval used when gathering
   *                                the data for the second statistic.
   * @param  useSameAxis            Indicates whether both statistics should be
   *                                graphed along the same axis.
   * @param  horizontalAxisCaption  The caption that should be displayed along
   *                                the horizontal axis for this graph.
   * @param  jobIDs                 The set of job IDs that correspond to each
   *                                data point in each value set.  This should
   *                                be <CODE>null</CODE> unless each data point
   *                                represents a different job.
   *
   * @return  A buffered image containing the generated line graph.
   */
  public BufferedImage generateDualLineGraph(final String label1,
                            final String caption1, final double[] values1,
                            final int interval1, final String label2,
                            final String caption2, final double[] values2,
                            final int interval2, final boolean useSameAxis,
                            final String horizontalAxisCaption,
                            final String[] jobIDs)
  {
    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font axisLabelFont =
         selectFont(g, "SansSerif", Font.PLAIN, 12, verticalAxisTitle, height);
    final Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 10);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics axisLabelMetrics   = g.getFontMetrics(axisLabelFont);
    final FontMetrics tickLabelMetrics   = g.getFontMetrics(tickLabelFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // Find the maximum and minimum for each data set.
    double max1  = -Double.MAX_VALUE;
    double min1  = Double.MAX_VALUE;
    for (int i=0; i < values1.length; i++)
    {
      if (values1[i] > max1)
      {
        max1 = values1[i];
      }

      if (values1[i] < min1)
      {
        min1 = values1[i];
      }
    }

    double max2  = -Double.MAX_VALUE;
    double min2  = Double.MAX_VALUE;
    for (int i=0; i < values2.length; i++)
    {
      if (values2[i] > max2)
      {
        max2 = values2[i];
      }

      if (values2[i] < min2)
      {
        min2 = values2[i];
      }
    }

    final double max = Math.max(max1, max2);
    final double min = Math.min(min1, min2);


    // Calculate the real max and min value to use for the axes of each data
    // set.
    double span1 = max1 - min1;
    if (span1 < 0.00001)
    {
      span1 += 1.0;
      max1  += 0.5;
      if (min1 >= 0.5)
      {
        min1 -= 0.5;
      }
    }

    double span2 = max2 - min2;
    if (span2 < 0.00001)
    {
      span2 += 1.0;
      max2  += 0.5;
      if (min2 >= 0.5)
      {
        min2 -= 0.5;
      }
    }

    final double graphMax1 = max1 / 0.95;
    double graphMin1;
    double graphSpan1;
    if (baseAtZero)
    {
      graphMin1 = 0;
    }
    else
    {
      graphMin1 = min1 - (graphMax1 * 0.05);

      if ((graphMin1 < 0) && (min1 >= 0))
      {
        graphMin1 = 0;
      }

      graphSpan1 = graphMax1 - graphMin1;
      final int largestPowerOfTen = (int) (Math.log(graphSpan1) / Math.log(10));
      if (largestPowerOfTen > 0)
      {
        graphMin1 = ((int) (graphMin1 / Math.pow(10, largestPowerOfTen))) *
                     Math.pow(10, largestPowerOfTen);
      }
    }
    graphSpan1 = graphMax1 - graphMin1;

    final double graphMax2 = max2 / 0.95;
    double graphMin2;
    double graphSpan2;
    if (baseAtZero)
    {
      graphMin2 = 0;
    }
    else
    {
      graphMin2 = min2 - (graphMax2 * 0.05);

      if ((graphMin2 < 0) && (min2 >= 0))
      {
        graphMin2 = 0;
      }

      graphSpan2 = graphMax2 - graphMin2;
      final int largestPowerOfTen = (int) (Math.log(graphSpan2) / Math.log(10));
      if (largestPowerOfTen > 0)
      {
        graphMin2 = ((int) (graphMin2 / Math.pow(10, largestPowerOfTen))) *
                     Math.pow(10, largestPowerOfTen);
      }
    }
    graphSpan2 = graphMax2 - graphMin2;

    graphMax = Math.max(graphMax1, graphMax2);
    graphMin = Math.min(graphMin1, graphMin2);
    graphSpan = graphMax - graphMin;


    // The top of the graph will be just below the bottom of the caption, and
    // the bottom of the graph will be twice the distance from the bottom of the
    // image.
    upperLeftY = titleMetrics.getHeight() + 5;
    originY = height - (2 * axisLabelMetrics.getHeight()) -
         tickLabelMetrics.getHeight();
    graphHeight = originY - upperLeftY + 1;


    // Add the legend along the side.
    int legendWidth = axisLabelMetrics.getHeight() + 2;
    if (includeLegend)
    {
      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);

      int labelWidth =
           legendItemMetrics.stringWidth(label1) +
           legendItemMetrics.getHeight() + 2;
      if (labelWidth > maxLabelWidth)
      {
        maxLabelWidth = labelWidth;
      }

      labelWidth =
           legendItemMetrics.stringWidth(label2) +
           legendItemMetrics.getHeight() + 2;
      if (labelWidth > maxLabelWidth)
      {
        maxLabelWidth = labelWidth;
      }

      legendWidth += maxLabelWidth + 6;
      int legendHeight = 4 + legendTitleMetrics.getHeight() +
           (2 * legendItemMetrics.getHeight());

      if ((jobIDs != null) && (jobIDs.length > 0))
      {
        // If we should include job ID information in the legend, then we need
        // to adjust our numbers to accommodate them.
        for (int i=0; i < jobIDs.length; i++)
        {
          labelWidth =
               legendItemMetrics.stringWidth((i+1) + " -- " + jobIDs[i]);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
        }

        final int captionHeight = legendItemMetrics.getHeight();
        legendHeight += (jobIDs.length + 2) * captionHeight;
        legendWidth = captionHeight + 2 + maxLabelWidth + 6;
      }

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth+legendTitleMetrics.stringWidth(legendTitle))/2) - 6;
      int labelY = ((height - legendHeight) / 2) +
           legendTitleMetrics.getHeight() + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      g.setFont(legendItemFont);

      // Draw the legend entries using the appropriate colors.
      final int legendItemAscent = legendItemMetrics.getAscent();
      final int legendItemHeight = legendItemMetrics.getHeight();
      labelX = width - maxLabelWidth - 6;
      labelY += legendItemHeight;
      g.setColor(COLORS[0]);
      g.fillRect(labelX, labelY-legendItemAscent, legendItemAscent,
           legendItemAscent);
      g.setColor(Color.black);
      g.drawString(label1, labelX + legendItemHeight + 2, labelY);

      labelY += legendItemHeight;
      g.setColor(COLORS[1]);
      g.fillRect(labelX, labelY-legendItemAscent, legendItemAscent,
           legendItemAscent);
      g.setColor(Color.black);
      g.drawString(label2, labelX + legendItemHeight + 2, labelY);

      g.setColor(Color.black);
      if ((jobIDs != null) && (jobIDs.length > 0))
      {
        labelY += (2 * legendItemHeight);
        labelWidth = legendItemMetrics.stringWidth("Job IDs");
        labelX = width - 6 - ((maxLabelWidth + labelWidth) / 2);
        g.drawString("Job IDs", labelX, labelY);

        labelX = width - maxLabelWidth - 6;
        for (int i=0; i < jobIDs.length; i++)
        {
          labelY += legendItemHeight;
          g.drawString((i+1) + " -- " + jobIDs[i], labelX, labelY);
        }
      }


      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // Draw the left and right vertical axis captions.  They should be rotated
    // 90 degrees, which will make for some funky math.  Just trust that this
    // works.
    g.setFont(axisLabelFont);
    if (! useSameAxis)
    {
      g.rotate(1.5*Math.PI);
      int captionWidth = axisLabelMetrics.stringWidth(caption1);
      g.drawString(caption1, -((height+captionWidth)/2),
           (axisLabelMetrics.getHeight()-2));

      captionWidth = axisLabelMetrics.stringWidth(caption2);
      g.drawString(caption2, -((height+captionWidth)/2),
                   (width-legendWidth));
      g.rotate(Math.PI/2);
    }


    // The vertical axis will be different depending on whether a single or two
    // different axes are to be included.
    g.setFont(tickLabelFont);
    lowerRightX = width - legendWidth - 5;
    if (useSameAxis)
    {
      // Figure out how many labels to draw along the vertical axis.  It will
      // basically be double-spaced, so figure out how many labels could fit
      // along the vertical axis and divide by two.
      final int captionHeight = tickLabelMetrics.getHeight();
      final int numVerticalLabels = graphHeight / captionHeight / 2;

      // If there is space for more vertical labels than there are integers,
      // then go up by integer values.  Otherwise, use the calculated number of
      // labels.
      final int labelX = 5 + captionHeight;
      int labelY = originY;
      int maxLabelWidth = 0;
      if (numVerticalLabels > graphSpan)
      {
        if (numVerticalLabels > (2 * graphSpan))
        {
          final double increment = graphSpan / numVerticalLabels;
          for (double value = graphMin; value < graphMax; value += increment)
          {
            labelY = valueYToGraphY(value);
            final String label = decimalFormat.format(value);
            if (tickLabelMetrics.stringWidth(label) > maxLabelWidth)
            {
              maxLabelWidth = tickLabelMetrics.stringWidth(label);
            }
            g.drawString(label, labelX,
                 (labelY+(tickLabelMetrics.getHeight()/2)));
          }
        }
        else
        {
          for (double value = graphMin; value < graphMax; value++)
          {
            labelY = valueYToGraphY(value);
            final String label = decimalFormat.format(value);
            if (tickLabelMetrics.stringWidth(label) > maxLabelWidth)
            {
              maxLabelWidth = tickLabelMetrics.stringWidth(label);
            }
            g.drawString(label, labelX,
                 (labelY+(tickLabelMetrics.getHeight()/2)));
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels);
        for (double valueY = graphMin; valueY < graphMax; valueY += increment)
        {
          labelY = valueYToGraphY(valueY);
          final String label = decimalFormat.format(valueY);
          if (tickLabelMetrics.stringWidth(label) > maxLabelWidth)
          {
            maxLabelWidth = tickLabelMetrics.stringWidth(label);
          }
          g.drawString(label, labelX,
               (labelY+(tickLabelMetrics.getHeight()/2)));
        }
      }


      // Draw the vertical axis and add the tick marks along its side
      originX = labelX + maxLabelWidth + 5;
      g.setColor(new Color(0xF1, 0xF1, 0xF1));
      g.fillRect(originX, upperLeftY, (lowerRightX - originX),
           (originY - upperLeftY));
      g.setColor(Color.BLACK);
      g.drawLine(originX, upperLeftY, originX, originY);
      labelY = originY;
      if (numVerticalLabels > graphSpan)
      {
        if (numVerticalLabels > (2 * graphSpan))
        {
          final double increment = graphSpan / numVerticalLabels;
          for (double value = graphMin; value < graphMax; value += increment)
          {
            labelY = valueYToGraphY(value);
            if (value > graphMin)
            {
              g.setColor(Color.LIGHT_GRAY);
              g.drawLine(originX, labelY, lowerRightX, labelY);
              g.setColor(Color.BLACK);
            }
            g.drawLine((originX - 2), labelY, (originX + 2), labelY);
          }
        }
        else
        {
          for (double value = graphMin; value < graphMax; value++)
          {
            labelY = valueYToGraphY(value);
            g.drawLine((originX - 2), labelY, (originX + 2), labelY);
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels);
        for (double valueY = graphMin; valueY < graphMax; valueY += increment)
        {
          labelY = valueYToGraphY(valueY);
          if (valueY > graphMin)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
    }
    else
    {
      // Figure out how many labels to draw along the left vertical axis.  It
      // will basically be double-spaced, so figure out how many labels could
      // fit along the vertical axis and divide by two.
      final int captionHeight = tickLabelMetrics.getHeight();
      final int halfHeight = captionHeight / 2;
      final int numVerticalLabels = graphHeight / captionHeight / 2;

      // If there is space for more vertical labels than there are integers,
      // then go up by integer values.  Otherwise, use the calculated number of
      // labels.
      int labelX            = 5 + captionHeight;
      int labelY            = originY;
      int maxLabelWidth     = 0;
      if (numVerticalLabels > graphSpan1)
      {
        if (numVerticalLabels > (2 * graphSpan1))
        {
          final double increment = graphSpan1 / numVerticalLabels;
          for (double value = graphMin1; value < graphMax1; value += increment)
          {
            labelY = valueYToGraphY(value, graphMin1, graphSpan1);
            final String label = decimalFormat.format(value);
            if (tickLabelMetrics.stringWidth(label) > maxLabelWidth)
            {
              maxLabelWidth = tickLabelMetrics.stringWidth(label);
            }
            g.drawString(label, labelX, (labelY+halfHeight));
          }
        }
        else
        {
          for (double value = graphMin1; value < graphMax1; value++)
          {
            labelY = valueYToGraphY(value, graphMin1, graphSpan1);
            final String label = decimalFormat.format(value);
            if (tickLabelMetrics.stringWidth(label) > maxLabelWidth)
            {
              maxLabelWidth = tickLabelMetrics.stringWidth(label);
            }
            g.drawString(label, labelX, (labelY+halfHeight));
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels, graphSpan1);
        for (double valueY = graphMin1; valueY < graphMax1; valueY += increment)
        {
          labelY = valueYToGraphY(valueY, graphMin1, graphSpan1);
          final String label = decimalFormat.format(valueY);
          if (tickLabelMetrics.stringWidth(label) > maxLabelWidth)
          {
            maxLabelWidth = tickLabelMetrics.stringWidth(label);
          }
          g.drawString(label, labelX, (labelY+halfHeight));
        }
      }


      // Figure out the maximum label width for labels on the right side.
      labelY = originY;
      int maxRightLabelWidth = 0;
      if (numVerticalLabels > graphSpan2)
      {
        if (numVerticalLabels > (2 * graphSpan2))
        {
          final double increment = graphSpan2 / numVerticalLabels;
          for (double value = graphMin2; value < graphMax2; value += increment)
          {
            labelY = valueYToGraphY(value, graphMin2, graphSpan2);
            final String label = decimalFormat.format(value);
            final int labelWidth = tickLabelMetrics.stringWidth(label);
            if (labelWidth > maxRightLabelWidth)
            {
              maxRightLabelWidth = labelWidth;
            }
          }
        }
        else
        {
          for (double value = graphMin2; value < graphMax2; value++)
          {
            labelY = valueYToGraphY(value, graphMin2, graphSpan2);
            final String label = decimalFormat.format(value);
            final int labelWidth = tickLabelMetrics.stringWidth(label);
            if (labelWidth > maxRightLabelWidth)
            {
              maxRightLabelWidth = labelWidth;
            }
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels, graphSpan2);
        for (double valueY = graphMin2; valueY < graphMax2; valueY += increment)
        {
          labelY = valueYToGraphY(valueY, graphMin2, graphSpan2);
          final String label = decimalFormat.format(valueY);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxRightLabelWidth)
          {
            maxRightLabelWidth = labelWidth;
          }
        }
      }


      // Draw the left vertical axis and add the tick marks along its side
      originX = labelX + maxLabelWidth + 5;
      lowerRightX =
           width - legendWidth - captionHeight - maxRightLabelWidth - 5;
      g.setColor(new Color(0xF1, 0xF1, 0xF1));
      g.fillRect(originX, upperLeftY, (lowerRightX - originX),
           (originY - upperLeftY));
      g.setColor(COLORS[0]);
      g.drawLine(originX, upperLeftY, originX, originY);
      labelY = originY;
      if (numVerticalLabels > graphSpan1)
      {
        if (numVerticalLabels > (2 * graphSpan1))
        {
          final double increment = graphSpan1 / numVerticalLabels;
          for (double value = graphMin1; value < graphMax1; value += increment)
          {
            labelY = valueYToGraphY(value, graphMin1, graphSpan1);
            if (value > graphMin1)
            {
              g.setColor(Color.LIGHT_GRAY);
              g.drawLine(originX, labelY, lowerRightX, labelY);
              g.setColor(COLORS[0]);
            }
            g.drawLine((originX - 2), labelY, (originX + 2), labelY);
          }
        }
        else
        {
          for (double value = graphMin1; value < graphMax1; value++)
          {
            labelY = valueYToGraphY(value, graphMin1, graphSpan1);
            if (value > graphMin1)
            {
              g.setColor(Color.LIGHT_GRAY);
              g.drawLine(originX, labelY, lowerRightX, labelY);
              g.setColor(COLORS[0]);
            }
            g.drawLine((originX - 2), labelY, (originX + 2), labelY);
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels, graphSpan1);
        for (double valueY = graphMin1; valueY < graphMax1; valueY += increment)
        {
          labelY = valueYToGraphY(valueY, graphMin1, graphSpan1);
          if (valueY > graphMin1)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(COLORS[0]);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
      g.setColor(Color.black);


      // Figure out how many labels to draw along the right vertical axis.  If
      // there is space for more vertical labels than there are integers, then
      // go up by integer values.  Otherwise, use the calculated number of
      // labels.
      labelX        = width - legendWidth - captionHeight;
      labelY        = originY;
      if (numVerticalLabels > graphSpan2)
      {
        if (numVerticalLabels > (2 * graphSpan2))
        {
          final double increment = graphSpan2 / numVerticalLabels;
          for (double value = graphMin2; value < graphMax2; value += increment)
          {
            labelY = valueYToGraphY(value, graphMin2, graphSpan2);
            final String label = decimalFormat.format(value);
            final int labelWidth = tickLabelMetrics.stringWidth(label);
            g.drawString(label, labelX-labelWidth, (labelY+halfHeight));
          }
        }
        else
        {
          for (double value = graphMin2; value < graphMax2; value++)
          {
            labelY = valueYToGraphY(value, graphMin2, graphSpan2);
            final String label = decimalFormat.format(value);
            final int labelWidth = tickLabelMetrics.stringWidth(label);
            g.drawString(label, labelX-labelWidth, (labelY+halfHeight));
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels, graphSpan2);
        for (double valueY = graphMin2; valueY < graphMax2; valueY += increment)
        {
          labelY = valueYToGraphY(valueY, graphMin2, graphSpan2);
          final String label = decimalFormat.format(valueY);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          g.drawString(label, labelX-labelWidth, (labelY+halfHeight));
        }
      }


      // Draw the right vertical axis and add the tick marks along its side
      g.setColor(COLORS[1]);
      g.drawLine(lowerRightX, upperLeftY, lowerRightX, originY);
      labelY = originY;
      if (numVerticalLabels > graphSpan2)
      {
        if (numVerticalLabels > (2 * graphSpan2))
        {
          final double increment = graphSpan2 / numVerticalLabels;
          for (double value = graphMin2; value < graphMax2; value += increment)
          {
            labelY = valueYToGraphY(value, graphMin2, graphSpan2);
            g.drawLine((lowerRightX - 2), labelY, (lowerRightX + 2), labelY);
          }
        }
        else
        {
          for (double value = graphMin2; value < graphMax2; value++)
          {
            labelY = valueYToGraphY(value, graphMin2, graphSpan2);
            g.drawLine((lowerRightX - 2), labelY, (lowerRightX + 2), labelY);
          }
        }
      }
      else
      {
        final double increment =
             chooseVerticalLabelIncrement(numVerticalLabels, graphSpan2);
        for (double valueY = graphMin2; valueY < graphMax2; valueY += increment)
        {
          labelY = valueYToGraphY(valueY, graphMin2, graphSpan2);
          g.drawLine((lowerRightX - 2), labelY, (lowerRightX + 2), labelY);
        }
      }
      g.setColor(Color.black);
    }


    // We know that the number of intervals will go from startSeconds to
    // startSeconds+n, so the width of startSeconds+n should be roughly equal to
    // the greatest width.
    numSeconds = Math.max((interval1*values1.length),
         (interval2*values2.length));
    final String label = String.valueOf(startSeconds+numSeconds);
    final int maxLabelWidth = tickLabelMetrics.stringWidth(label);
    final int maxHorizontalLabels = (lowerRightX - originX) / maxLabelWidth / 2;
    int secondsPerInterval;
    if ((numSeconds > maxHorizontalLabels) && (maxHorizontalLabels > 0))
    {
      secondsPerInterval = numSeconds / maxHorizontalLabels;
      if ((secondsPerInterval % interval1) != 0)
      {
        secondsPerInterval = (secondsPerInterval / interval1 + 1) * interval1;
      }
    }
    else
    {
      secondsPerInterval = 1;
    }



    // Draw the labels at the bottom of the horizontal axis.  The tick marks
    // can also be added at the same time.
    final int labelY = originY + tickLabelMetrics.getHeight() + 2;
    for (int value = 0; value < numSeconds; value += secondsPerInterval)
    {
      final String valueString = String.valueOf(startSeconds+value);
      final int labelX = valueXToGraphX(value);
      final int width = tickLabelMetrics.stringWidth(valueString);
      if (value > 0)
      {
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(labelX, originY, labelX, upperLeftY);
        g.setColor(Color.BLACK);
      }
      g.drawString(valueString, (labelX-(width/2)), labelY);
      g.drawLine(labelX, (originY - 2), labelX, (originY + 2));
    }


    // Now draw the horizontal axis.
    g.drawLine(originX, originY, lowerRightX, originY);


    // Add the caption at the top of the image
    int captionWidth = titleMetrics.stringWidth(graphTitle);
    int captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    int captionY = titleMetrics.getHeight() + 2;
    g.setFont(titleFont);
    g.drawString(graphTitle, captionX, captionY);


    // Add the horizontal axis caption at the bottom of the image
    captionWidth = axisLabelMetrics.stringWidth(horizontalAxisCaption);
    captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    captionY = (height - 2);
    g.setFont(axisLabelFont);
    g.drawString(horizontalAxisCaption, captionX, captionY);


    // Actually draw the graph data.  This will be different based on whether
    // one or two axes will be used.
    final Stroke defaultStroke = g.getStroke();
    g.setStroke(new BasicStroke(2.0f));
    if (useSameAxis)
    {
      // Iterate through the first set of data and make the line graph.
      g.setColor(COLORS[0]);
      int j, x1, x2, y1, y2;
      for (j=0; ((j < values1.length) && ignoreZeroValues &&
                 Double.isNaN(values1[j])); j++);
      x1 = valueXToGraphX(j*interval1);
      y1 = valueYToGraphY(values1[j]);
      for (j=j+1; j < values1.length; j++)
      {
        if (! (ignoreZeroValues && Double.isNaN(values1[j])))
        {
          x2 = valueXToGraphX((j+1) * interval1);
          y2 = valueYToGraphY(values1[j]);

          if (flatBetweenPoints)
          {
            g.drawLine(x1, y1, x2, y1);
            g.drawLine(x2, y1, x2, y2);
          }
          else
          {
            g.drawLine(x1, y1, x2, y2);
          }

          x1 = x2;
          y1 = y2;
        }
      }


      // Iterate through the second set of data and make the line graph.
      g.setColor(COLORS[1]);
      for (j=0; ((j < values2.length) && ignoreZeroValues &&
                 Double.isNaN(values2[j])); j++);
      x1 = valueXToGraphX(j*interval2);
      y1 = valueYToGraphY(values2[j]);
      for (j=j+1; j < values2.length; j++)
      {
        if (! (ignoreZeroValues && Double.isNaN(values2[j])))
        {
          x2 = valueXToGraphX((j+1) * interval2);
          y2 = valueYToGraphY(values2[j]);

          if (flatBetweenPoints)
          {
            g.drawLine(x1, y1, x2, y1);
            g.drawLine(x2, y1, x2, y2);
          }
          else
          {
            g.drawLine(x1, y1, x2, y2);
          }

          x1 = x2;
          y1 = y2;
        }
      }
    }
    else
    {
      // Iterate through the first set of data and make the line graph.
      g.setColor(COLORS[0]);
      int j, x1, x2, y1, y2;
      for (j=0; ((j < values1.length) && ignoreZeroValues &&
                 Double.isNaN(values1[j])); j++);
      if (j < values1.length)
      {
        x1 = valueXToGraphX((j+1)*interval1);
        y1 = valueYToGraphY(values1[j], graphMin1, graphSpan1);
        for (j=j+1; j < values1.length; j++)
        {
          if (! (ignoreZeroValues && Double.isNaN(values1[j])))
          {
            x2 = valueXToGraphX((j+1) * interval1);
            y2 = valueYToGraphY(values1[j], graphMin1, graphSpan1);

            if (flatBetweenPoints)
            {
              g.drawLine(x1, y1, x2, y1);
              g.drawLine(x2, y1, x2, y2);
            }
            else
            {
              g.drawLine(x1, y1, x2, y2);
            }

            x1 = x2;
            y1 = y2;
          }
        }
      }


      // Iterate through the second set of data and make the line graph.
      g.setColor(COLORS[1]);
      for (j=0; ((j < values2.length) && ignoreZeroValues &&
                 Double.isNaN(values2[j])); j++);
      if (j < values2.length)
      {
        x1 = valueXToGraphX((j+1)*interval2);
        y1 = valueYToGraphY(values2[j], graphMin2, graphSpan2);
        for (j=j+1; j < values2.length; j++)
        {
          if (! (ignoreZeroValues && Double.isNaN(values2[j])))
          {
            x2 = valueXToGraphX((j+1) * interval2);
            y2 = valueYToGraphY(values2[j], graphMin2, graphSpan2);

            if (flatBetweenPoints)
            {
              g.drawLine(x1, y1, x2, y1);
              g.drawLine(x2, y1, x2, y2);
            }
            else
            {
              g.drawLine(x1, y1, x2, y2);
            }

            x1 = x2;
            y1 = y2;
          }
        }
      }
    }

    g.setStroke(defaultStroke);
    return image;
  }



  /**
   * Generates a bar graph based on the information that has been provided.
   *
   * @return  A buffered image containing the generated bar graph.
   */
  public BufferedImage generateBarGraph()
  {
    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // Set the fonts to use for the grapher.
    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font axisLabelFont =
         selectFont(g, "SansSerif", Font.PLAIN, 12, verticalAxisTitle, height);
    final Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 10);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics axisLabelMetrics   = g.getFontMetrics(axisLabelFont);
    final FontMetrics tickLabelMetrics   = g.getFontMetrics(tickLabelFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // We're working with the averages for each data set rather than the
    // maximums, so calculate them.
    double maxAverage = 0.0;
    final double[] averages = new double[data.length];
    for (int i=0; i < averages.length; i++)
    {
      if (data[i].length == 0)
      {
        averages[i] = 0.0;
      }
      else
      {
        double sum = 0.0;
        for (int j=0; j < data[i].length; j++)
        {
          sum += data[i][j];
        }
        averages[i] = sum / data[i].length;
        if (averages[i] > maxAverage)
        {
          maxAverage = averages[i];
        }
      }
    }


    // Bar graphs will always be based at zero, so there is not much work
    // necessary to calculate the values.
    final double span = maxAverage;
    graphMax  = maxAverage / 0.9;
    graphMin  = 0;
    graphSpan = graphMax;


    // The top of the graph will be just below the bottom of the caption, and
    // the bottom of the graph will be the same distance from the bottom of the
    // image.
    upperLeftY = titleMetrics.getHeight() + 5;
    originY = height - (2 * axisLabelMetrics.getHeight()) -
         tickLabelMetrics.getHeight();
    graphHeight = originY - upperLeftY + 1;


    // Add the legend along the side.
    int legendWidth = axisLabelMetrics.getHeight() + 2;
    if (includeLegend)
    {
      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);
      for (int i=0; i < dataSetLabels.length; i++)
      {
        final int labelWidth =
             legendItemMetrics.stringWidth(dataSetLabels[i]) +
             legendItemMetrics.getHeight() + 2;
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
      }

      legendWidth += maxLabelWidth + 6;
      final int legendHeight = 4 + legendTitleMetrics.getHeight() +
           (dataSetLabels.length * legendItemMetrics.getHeight());

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth + legendTitleMetrics.stringWidth(legendTitle)) / 2) -
           6;
      int labelY = ((height - legendHeight) / 2) +
           legendTitleMetrics.getAscent() + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      g.setFont(legendItemFont);
      labelY += legendTitleMetrics.getHeight();

      // Draw the legend entries using the appropriate colors.
      labelX = width - maxLabelWidth - 6;
      for (int i=0; i < dataSetLabels.length; i++)
      {
        final int captionAscent = legendItemMetrics.getAscent();
        final int captionHeight = legendItemMetrics.getHeight();
        g.setColor(COLORS[i % COLORS.length]);
        g.fillRect(labelX, labelY-captionAscent, captionAscent, captionAscent);

        g.setColor(Color.black);
        g.drawString(dataSetLabels[i], labelX + captionHeight + 2, labelY);
        labelY += captionHeight;
      }

      g.setColor(Color.black);
      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // The graph should be centered horizontally, so make the right border the
    // same distance from the image edge as the left border.
    lowerRightX = width - legendWidth;


    // Draw the vertical axis caption.  It should be rotated 90 degrees, which
    // makes for some funky math.  Just trust that this works.
    g.rotate(1.5*Math.PI);
    int captionWidth  = axisLabelMetrics.stringWidth(verticalAxisTitle);
    g.setFont(axisLabelFont);
    g.drawString(verticalAxisTitle, -((height+captionWidth)/2),
                 (axisLabelMetrics.getHeight()-2));
    g.setFont(tickLabelFont);
    g.rotate(Math.PI/2);


    // Figure out how many labels to draw along the vertical axis.  It will
    // basically be double-spaced, so figure out how many labels could fit along
    // the vertical axis and divide by 2.
    final int numVerticalLabels =
         graphHeight / tickLabelMetrics.getHeight() / 2;


    // If there is space for more vertical labels than there are integers, then
    // go up by integer values.  Otherwise, use the calculated number of labels.
    int labelX = 5 + axisLabelMetrics.getHeight();
    int labelY = originY;
    int maxLabelWidth = 0;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);

          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);

          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);
        final String label = decimalFormat.format(valueY);
        final int labelWidth = tickLabelMetrics.stringWidth(label);
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
        g.drawString(label, labelX,
             (labelY + (tickLabelMetrics.getHeight()/2)));
      }
    }


    // Draw the vertical axis and add the tick marks along its side
    originX = labelX + maxLabelWidth + 5;
    g.drawLine(originX, upperLeftY, originX, originY);
    labelY = originY;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);

        if (includeHorizontalGrid)
        {
          g.setColor(Color.LIGHT_GRAY);
          g.drawLine(originX, labelY, lowerRightX, labelY);
          g.setColor(Color.BLACK);
        }
        g.drawLine((originX - 2), labelY, (originX + 2), labelY);
      }
    }


    // Now draw the horizontal axis.
    g.drawLine(originX, originY, lowerRightX, originY);


    // Add the caption at the top of the image
    captionWidth = titleMetrics.stringWidth(graphTitle);
    final int captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    final int captionY = titleMetrics.getHeight() + 2;
    g.setFont(titleFont);
    g.drawString(graphTitle, captionX, captionY);


    // Figure out how wide each bar should be.
    final int numBars = data.length;
    if (numBars == 0)
    {
      return null;
    }
    final int barWidth = (lowerRightX - originX) / numBars;
    int barX = originX + (((lowerRightX - originX) - (numBars * barWidth)) / 2);



    // Finally, iterate through the data and make the bar graphs.
    for (int i=0; i < data.length; i++)
    {
      if (i < COLORS.length)
      {
        g.setColor(COLORS[i]);
      }
      else
      {
        g.setColor(COLORS[i % COLORS.length]);
      }

      //Draw the filled rectangle.
      final int barY = valueYToGraphY(averages[i]);
      g.fillRect(barX, barY, barWidth, (originY - barY));

      g.setColor(Color.DARK_GRAY);
      g.drawRect(barX, barY, barWidth, (originY - barY));

      barX += barWidth;
    }

    return image;
  }



  /**
   * Generates a stacked bar graph based on the information that has been
   * provided.
   *
   * @return  A buffered image containing the generated stacked bar graph.
   */
  public BufferedImage generateStackedBarGraph()
  {
    // Create the image and get the graphics context.
    final BufferedImage image = new BufferedImage(width, height,
         BufferedImage.TYPE_BYTE_INDEXED);
    final Graphics2D g = image.createGraphics();


    // Configure the graph to enable antialiasing.
    final HashMap<RenderingHints.Key,Object> renderingHints =
         new HashMap<RenderingHints.Key,Object>(3);
    renderingHints.put(RenderingHints.KEY_ANTIALIASING,
         RenderingHints.VALUE_ANTIALIAS_OFF);
    renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    renderingHints.put(RenderingHints.KEY_RENDERING,
         RenderingHints.VALUE_RENDER_QUALITY);
    g.addRenderingHints(renderingHints);


    // Give the graph a white background and use black to add text and the axes.
    g.setColor(Color.white);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);


    // Set the fonts to use for the grapher.
    final Font titleFont =
         selectFont(g, "SansSerif", Font.BOLD, 18, graphTitle, width);
    final Font axisLabelFont =
         selectFont(g, "SansSerif", Font.PLAIN, 12, verticalAxisTitle, height);
    final Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 10);
    final Font legendTitleFont = new Font("SansSerif", Font.BOLD, 12);
    final Font legendItemFont  = new Font("SansSerif", Font.PLAIN, 10);

    final FontMetrics titleMetrics       = g.getFontMetrics(titleFont);
    final FontMetrics axisLabelMetrics   = g.getFontMetrics(axisLabelFont);
    final FontMetrics tickLabelMetrics   = g.getFontMetrics(tickLabelFont);
    final FontMetrics legendTitleMetrics = g.getFontMetrics(legendTitleFont);
    final FontMetrics legendItemMetrics  = g.getFontMetrics(legendItemFont);


    // Bar graphs will always be based at zero, so there is not much work
    // necessary to calculate the values.
    final double span = max;
    graphMax  = max + (span/20);
    graphMin  = 0;
    graphSpan = graphMax;


    // The top of the graph will be just below the bottom of the caption, and
    // the bottom of the graph will be the same distance from the bottom of the
    // image.
    upperLeftY = titleMetrics.getHeight() + 5;
    originY = height - (2 * axisLabelMetrics.getHeight()) -
         tickLabelMetrics.getHeight();
    graphHeight = originY - upperLeftY + 1;


    // Add the legend along the side.
    int legendHeightItems = 0;
    int legendWidth = axisLabelMetrics.getHeight() + 2;
    if (includeLegend)
    {
      int maxLabelWidth = legendTitleMetrics.stringWidth(legendTitle);
      for (int i=0; i < dataSetNames.length; i++)
      {
        for (int j=0; j < categoryNames[i].length; j++)
        {
          legendHeightItems++;
          final String label = dataSetNames[i] + " - " + categoryNames[i][j];
          final int labelWidth =
               legendItemMetrics.stringWidth(dataSetLabels[i]) +
               legendItemMetrics.getHeight() + 2;
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
        }
      }

      legendWidth += maxLabelWidth + 6;
      final int legendHeight = 4 + legendTitleMetrics.getHeight() +
           (dataSetLabels.length * legendItemMetrics.getHeight());

      // Draw the caption at the top of the legend.
      int labelX = width -
           ((maxLabelWidth + legendTitleMetrics.stringWidth(legendTitle)) / 2) -
           6;
      int labelY = ((height - legendHeight) / 2) +
           legendTitleMetrics.getAscent() + 2;
      g.setFont(legendTitleFont);
      g.drawString(legendTitle, labelX, labelY);
      labelY += legendTitleMetrics.getHeight();
      g.setFont(legendItemFont);

      // Draw the legend entries using the appropriate colors.
      labelX = width - maxLabelWidth - 6;
      int colorSlot = 0;
      for (int i=0; i < dataSetNames.length; i++)
      {
        for (int j=0; j < categoryNames[i].length; j++)
        {
          final int captionAscent = legendItemMetrics.getAscent();
          final int captionHeight = legendItemMetrics.getHeight();
          g.setColor(COLORS[colorSlot++ % COLORS.length]);
          g.fillRect(labelX, labelY-captionAscent, captionAscent,
                     captionAscent);

          g.setColor(Color.black);
          final String label = dataSetNames[i] + " - " + categoryNames[i][j];
          g.drawString(label, labelX + captionHeight + 2, labelY);
          labelY += captionHeight;
        }
      }

      g.setColor(Color.black);
      g.drawRect((labelX - 2), ((height - legendHeight) / 2),
                 (maxLabelWidth + 4), legendHeight);
    }


    // The graph should be centered horizontally, so make the right border the
    // same distance from the image edge as the left border.
    lowerRightX = width - legendWidth;


    // Draw the vertical axis caption.  It should be rotated 90 degrees, which
    // makes for some funky math.  Just trust that this works.
    g.rotate(1.5*Math.PI);
    int captionWidth  = axisLabelMetrics.stringWidth(verticalAxisTitle);
    g.setFont(axisLabelFont);
    g.drawString(verticalAxisTitle, -((height+captionWidth)/2),
                 (axisLabelMetrics.getHeight()-2));
    g.setFont(tickLabelFont);
    g.rotate(Math.PI/2);


    // Figure out how many labels to draw along the vertical axis.  It will
    // basically be double-spaced, so figure out how many labels could fit along
    // the vertical axis and divide by 2.
    final int numVerticalLabels =
         graphHeight / tickLabelMetrics.getHeight() / 2;


    // If there is space for more vertical labels than there are integers, then
    // go up by integer values.  Otherwise, use the calculated number of labels.
    int labelX = 5 + axisLabelMetrics.getHeight();
    int labelY = originY;
    int maxLabelWidth = 0;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);

          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          final String label = decimalFormat.format(value);
          final int labelWidth = tickLabelMetrics.stringWidth(label);
          if (labelWidth > maxLabelWidth)
          {
            maxLabelWidth = labelWidth;
          }
          g.drawString(label, labelX,
               (labelY + (tickLabelMetrics.getHeight()/2)));
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);
        final String label = decimalFormat.format(valueY);
        final int labelWidth = tickLabelMetrics.stringWidth(label);
        if (labelWidth > maxLabelWidth)
        {
          maxLabelWidth = labelWidth;
        }
        g.drawString(label, labelX,
             (labelY + (tickLabelMetrics.getHeight()/2)));
      }
    }


    // Draw the vertical axis and add the tick marks along its side
    originX = labelX + maxLabelWidth + 5;
    g.drawLine(originX, upperLeftY, originX, originY);
    labelY = originY;
    if (numVerticalLabels > graphSpan)
    {
      if (numVerticalLabels > (2 * graphSpan))
      {
        final double increment = graphSpan / numVerticalLabels;
        for (double value = graphMin; value < graphMax; value += increment)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
      else
      {
        for (double value = graphMin; value < graphMax; value++)
        {
          labelY = valueYToGraphY(value);
          if (includeHorizontalGrid)
          {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(originX, labelY, lowerRightX, labelY);
            g.setColor(Color.BLACK);
          }
          g.drawLine((originX - 2), labelY, (originX + 2), labelY);
        }
      }
    }
    else
    {
      final double increment = chooseVerticalLabelIncrement(numVerticalLabels);
      for (double valueY = graphMin; valueY < graphMax; valueY += increment)
      {
        labelY = valueYToGraphY(valueY);

        if (includeHorizontalGrid)
        {
          g.setColor(Color.LIGHT_GRAY);
          g.drawLine(originX, labelY, lowerRightX, labelY);
          g.setColor(Color.BLACK);
        }
        g.drawLine((originX - 2), labelY, (originX + 2), labelY);
      }
    }


    // Now draw the horizontal axis.
    g.drawLine(originX, originY, lowerRightX, originY);


    // Add the caption at the top of the image
    captionWidth = titleMetrics.stringWidth(graphTitle);
    final int captionX = ((lowerRightX - originX - captionWidth) / 2) + originX;
    final int captionY = titleMetrics.getHeight() + 2;
    g.setFont(titleFont);
    g.drawString(graphTitle, captionX, captionY);


    // Figure out how wide each bar should be.
    final int numBars = dataSetNames.length;
    if (numBars == 0)
    {
      return null;
    }
    final int barWidth = (lowerRightX - originX) / numBars;
    int barX = originX + (((lowerRightX - originX) - (numBars * barWidth)) / 2);



    // Finally, iterate through the data and make the bar graphs.
    int colorSlot = 0;
    for (int i=0; i < dataSetAverages.length; i++)
    {
      int    lastY     = originY;
      int    barY      = 0;
      double lastValue = 0.0;
      for (int j=0; j < dataSetAverages[i].length; j++)
      {
        g.setColor(COLORS[colorSlot++ % COLORS.length]);
        barY = valueYToGraphY(lastValue + dataSetAverages[i][j]);
        g.fillRect(barX, barY, barWidth, (lastY - barY));
        lastY = barY;
        lastValue += dataSetAverages[i][j];
      }

      g.setColor(Color.DARK_GRAY);
      g.drawRect(barX, barY, barWidth, (originY - barY));

      barX += barWidth;
    }

    return image;
  }



  /**
   * Selects an appropriate font to use based on the provided information.
   *
   * @param  g            The graphics context to use.
   * @param  fontName     The name of the font to use.
   * @param  style        The style to use for the font.
   * @param  desiredSize  The desired size for the font.
   * @param  text         The text that will be displayed.
   * @param  maxLength    The maximum length available for the font.
   *
   * @return  The font that should be used.
   */
  private static Font selectFont(final Graphics2D g, final String fontName,
                                 final int style, final int desiredSize,
                                 final String text, final int maxLength)
  {
    final Font f = new Font(fontName, style, desiredSize);
    final FontMetrics fm = g.getFontMetrics(f);
    if (fm.stringWidth(text) < maxLength)
    {
      return f;
    }
    else
    {
      return selectFont(g, fontName, style, (desiredSize - 1), text, maxLength);
    }
  }




  /**
   * This method attempts to determine an appropriate increment between labels
   * on the vertical axis.  It has a preference for whole numbers if possible.
   *
   * @param  numVerticalLabels  The maximum number of labels that may be used
   *                            along the vertical axis.  The vertical span of
   *                            the graph divided by the returned increment may
   *                            not exceed this value.
   *
   * @return  The increment that should be used between labels on the vertical
   *          axis of the generated line graph.
   */
  private double chooseVerticalLabelIncrement(final int numVerticalLabels)
  {
    // Determine whether the graph span is larger or smaller than 1.  If larger
    // than 1, then we'll try to use whole numbers.  If smaller than 1, then
    // obviously we can't so just do it the cheap way.
    if (graphSpan > 1)
    {
      // Determine the increment size if we wanted to break things up into
      // evenly-sized increments.
      final double roughIncrement = graphSpan / numVerticalLabels;

      // If the rough increment is itself less than 1, then we won't bother
      // with anything complex on it.
      if (roughIncrement < 1)
      {
        return roughIncrement;
      }

      // Determine the largest power of ten that is less than or equal to this
      // rough increment.  This can be found using a base 10 logarithm.
      // However, since Java doesn't provide a function for finding a base 10
      // logarithm, then we'll have to use a base e logarithm and divide the
      // result by the log base e of 10.
      final int largestPowerOfTen =
           (int) (Math.log(roughIncrement) / Math.log(10));

      // OK.  Now we at least have a decent starting point.  This last part is
      // too complicated to comment, so either figure it out for yourself or
      // just trust that it does what we want.
      final int refinedIncrement =
           ((int) (roughIncrement / Math.pow(10, largestPowerOfTen))) *
           ((int) Math.pow(10, largestPowerOfTen));
      final double fudgeFactor = 5 * Math.pow(10, (largestPowerOfTen - 1));
      if ((refinedIncrement + fudgeFactor) < roughIncrement)
      {
        return (refinedIncrement + fudgeFactor);
      }
      else
      {
        return refinedIncrement;
      }
    }
    else
    {
      return graphSpan / numVerticalLabels;
    }
  }



  /**
   * This method attempts to determine an appropriate increment between labels
   * on the vertical axis.  It has a preference for whole numbers if possible.
   *
   * @param  numVerticalLabels  The maximum number of labels that may be used
   *                            along the vertical axis.  The vertical span of
   *                            the graph divided by the returned increment may
   *                            not exceed this value.
   * @param  graphSpan          The span of values covered along the vertical
   *                            axis.
   *
   * @return  The increment that should be used between labels on the vertical
   *          axis of the generated line graph.
   */
  private static double chooseVerticalLabelIncrement(
                             final int numVerticalLabels,
                             final double graphSpan)
  {
    // Determine whether the graph span is larger or smaller than 1.  If larger
    // than 1, then we'll try to use whole numbers.  If smaller than 1, then
    // obviously we can't so just do it the cheap way.
    if (graphSpan > 1)
    {
      // Determine the increment size if we wanted to break things up into
      // evenly-sized increments.
      final double roughIncrement = graphSpan / numVerticalLabels;

      // If the rough increment is itself less than 1, then we won't bother
      // with anything complex on it.
      if (roughIncrement < 1)
      {
        return roughIncrement;
      }

      // Determine the largest power of ten that is less than or equal to this
      // rough increment.  This can be found using a base 10 logarithm.
      // However, since Java doesn't provide a function for finding a base 10
      // logarithm, then we'll have to use a base e logarithm and divide the
      // result by the log base e of 10.
      final int largestPowerOfTen =
           (int) (Math.log(roughIncrement) / Math.log(10));

      // OK.  Now we at least have a decent starting point.  This last part is
      // too complicated to comment, so either figure it out for yourself or
      // just trust that it does what we want.
      final int refinedIncrement =
           ((int) (roughIncrement / Math.pow(10, largestPowerOfTen))) *
           ((int) Math.pow(10, largestPowerOfTen));
      final double fudgeFactor = 5 * Math.pow(10, (largestPowerOfTen - 1));
      if ((refinedIncrement + fudgeFactor) < roughIncrement)
      {
        return (refinedIncrement + fudgeFactor);
      }
      else
      {
        return refinedIncrement;
      }
    }
    else
    {
      return graphSpan / numVerticalLabels;
    }
  }



  /**
   * Converts the provided X coordinate to the value along the horizontal axis
   * to which it corresponds.
   *
   * @param  graphX  The X coordinate of the point in the image.
   *
   * @return  The value that corresponds to the provided X coordinate.
   */
  private int graphXToValueX(final int graphX)
  {
    final int distFromOrigin = graphX - originX;
    final double fractionOfTotal =
         1.0 * distFromOrigin / (lowerRightX - originX);
    return (int) (fractionOfTotal * numSeconds);
  }



  /**
   * Converts the provided value along the horizontal axis to an X coordinate in
   * the graph image.
   *
   * @param  valueX  The value along the horizontal axis for which to retrieve
   *                 the X coordinate.
   *
   * @return  The X coordinate that corresponds to the specified value.
   */
  private int valueXToGraphX(final int valueX)
  {
    final double fractionOfTotal = 1.0 * valueX / numSeconds;
    final int distFromOrigin =
         (int) (fractionOfTotal * (lowerRightX - originX));

    return originX + distFromOrigin;
  }



  /**
   * Converts the provided value along the horizontal axis to an X coordinate in
   * the graph image.
   *
   * @param  xValue  The value along the horizontal axis for which to retrieve
   *                 the X coordinate.
   * @param  xMax    The maximum x value of any data point to be graphed.
   *
   * @return  The X coordinate that corresponds to the specified value.
   */
  private int valueXToGraphX(final double xValue, final double xMax)
  {
    final double fractionOfTotal = xValue / xMax;
    final int distFromOrigin =
         (int) (fractionOfTotal * (lowerRightX - originX));

    return originX + distFromOrigin;
  }



  /**
   * Converts the provided Y coordinate to the value along the vertical axis to
   * which it corresponds.
   *
   * @param  graphY  The Y coordinate of the point in the image.
   *
   * @return  The value that corresponds to the provided Y coordinate.
   */
  private double graphYToValueY(final int graphY)
  {
    final int distFromOrigin = originY - graphY;
    final double fractionOfTotal =
         1.0 * distFromOrigin / (originY - upperLeftY);
    final double increaseOverGraphMin = fractionOfTotal * graphSpan;

    return graphMin + increaseOverGraphMin;
  }



  /**
   * Converts the provided Y coordinate to the value along the specific vertical
   * axis to which it corresponds.  This is to be used when the left and right
   * vertical axes represent different ranges of values.
   *
   * @param  graphY     The Y coordinate of the point in the image.
   * @param  graphMin   The minimum value for the vertical axis.
   * @param  graphSpan  The span for the vertical axis.
   *
   * @return  The value that corresponds to the provided Y coordinate.
   */
  private double graphYToValueY(final int graphY, final double graphMin,
                                final double graphSpan)
  {
    final int distFromOrigin = originY - graphY;
    final double fractionOfTotal =
         1.0 * distFromOrigin / (originY - upperLeftY);
    final double increaseOverGraphMin = fractionOfTotal * graphSpan;

    return graphMin + increaseOverGraphMin;
  }



  /**
   * Converts the provided value along the vertical axis to a Y coordinate in
   * the graph image.
   *
   * @param  valueY  The value along the vertical axis for which to retrieve
   *                 the Y coordinate.
   *
   * @return  The Y coordinate that corresponds to the specified value.
   */
  private int valueYToGraphY(final double valueY)
  {
    final double increaseOverGraphMin = valueY - graphMin;
    final double fractionOfTotal = increaseOverGraphMin / graphSpan;
    final int distFromOrigin =
         (int) (fractionOfTotal * (originY - upperLeftY));

    return originY - distFromOrigin;
  }



  /**
   * Converts the provided value along the vertical axis to a Y coordinate in
   * the graph image.  This is to be used when the left and right vertical axes
   * represent different ranges of values.
   *
   * @param  valueY     The value along the vertical axis for which to retrieve
   *                    the Y coordinate.
   * @param  graphMin   The minimum value for the vertical axis.
   * @param  graphSpan  The span for the vertical axis.
   *
   * @return  The Y coordinate that corresponds to the specified value.
   */
  private int valueYToGraphY(final double valueY, final double graphMin,
                             final double graphSpan)
  {
    final double increaseOverGraphMin = valueY - graphMin;
    final double fractionOfTotal = increaseOverGraphMin / graphSpan;
    final int distFromOrigin =
         (int) (fractionOfTotal * (originY - upperLeftY));

    return originY - distFromOrigin;
  }



  /**
   * Provides a simple test program, which displays the set of colors that will
   * be used to generate the graphs.
   *
   * @param  args  The command-line arguments provided to this program.
   *
   * @throws  Exception  If a problem occurs while generating the test image.
   */
  public static void main(final String[] args)
         throws Exception
  {
    final String[] categoryNames = new String[COLORS.length];
    for (int i=0; i < categoryNames.length; i++)
    {
      categoryNames[i] = "Color " + i;
    }

    final int[] numOccurrences = new int[categoryNames.length];
    Arrays.fill(numOccurrences, 1);

    final StatGrapher grapher = new StatGrapher(1280, 1024, "Color Test");
    grapher.setIncludeLegend(true, "Color Map");

    final BufferedImage image =
         grapher.generatePieGraph(categoryNames, numOccurrences);

    final FileOutputStream outputStream = new FileOutputStream("test.png");
    final ImageEncoder encoder =
         ImageCodec.createImageEncoder("png", outputStream, null);
    encoder.encode(image);
    outputStream.flush();
    outputStream.close();

    System.out.println("Wrote color test to test.png");
  }
}

