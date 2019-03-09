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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



/**
 * This program provides a mechanism to easily split a text file into multiple
 * files based on a number of criteria (specific number of files, number of
 * lines per file, or number of bytes per file).
 *
 *
 * @author   Neil A. Wilson
 */
public class SplitFile
{
  /**
   * The split type that indicates the file should be split up into a specific
   * number of files.  The order of the original file will not be preserved.
   */
  public static final int SPLIT_TYPE_NUM_FILES = 1;



  /**
   * The split type that indicates the file should be split up into a specific
   * number of files.  The order of the original file will be preserved.
   */
  public static final int SPLIT_TYPE_NUM_FILES_PRESERVE_ORDER = 2;



  /**
   * The split type that indicates the file should be split up so that there are
   * a maximum number of lines per file.
   */
  public static final int SPLIT_TYPE_NUM_LINES = 3;



  /**
   * The split type that indicates the file should be split up so that there are
   * a maximum number of bytes per file.
   */
  public static final int SPLIT_TYPE_NUM_BYTES = 4;



  // The maximum number of bytes to include in a file.
  private int bytesPerFile;

  // The maximum number of lines to include in a file.
  private int linesPerFile;

  // The number of files to create.
  private int numFiles;

  // The criteria to use when splitting the file.
  private int splitType;

  // The base path and name of the split files to create.
  private String outputBase;

  // The file to be split.
  private String inputFile;



  /**
   * The main method for this program, which simply invokes the constructor.
   *
   * @param  args  The command line arguments provided to the program.
   */
  public static void main(String[] args)
  {
    new SplitFile(args);
  }



  /**
   * Creates a new instance of this program and coordinates the process of
   * splitting the file.
   *
   * @param  args  The command line arguments provided to the program.
   */
  public SplitFile(String[] args)
  {
    // Set default values for the arguments.
    bytesPerFile = -1;
    linesPerFile = -1;
    numFiles     = -1;
    splitType    = -1;
    inputFile    = null;
    outputBase   = null;


    // Parse the command-line arguments.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-b"))
      {
        splitType    = SPLIT_TYPE_NUM_BYTES;
        bytesPerFile = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-l"))
      {
        splitType    = SPLIT_TYPE_NUM_LINES;
        linesPerFile = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-n"))
      {
        splitType = SPLIT_TYPE_NUM_FILES;
        numFiles  = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-N"))
      {
        splitType = SPLIT_TYPE_NUM_FILES_PRESERVE_ORDER;
        numFiles  = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-i"))
      {
        inputFile = args[++i];
      }
      else if (args[i].equals("-o"))
      {
        outputBase = args[++i];
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        return;
      }
      else
      {
        System.err.println("ERROR:  Unrecognized argument \"" + args[i] + '"');
        displayUsage();
        return;
      }
    }


    // Make sure that both a split type was specified.
    if (splitType < 0)
    {
      System.err.println("ERROR:  No split mechanism specified");
      displayUsage();
      return;
    }


    // Make sure that an input file was specified.
    if (inputFile == null)
    {
      System.err.println("ERROR:  No input file specified");
      displayUsage();
      return;
    }


    // If no output base was specified, then use the input file as the base.
    if (outputBase == null)
    {
      outputBase = inputFile;
    }


    // Split the file as per the user's request.
    try
    {
      switch (splitType)
      {
        case SPLIT_TYPE_NUM_FILES:
          splitToNumFiles();
          break;
        case SPLIT_TYPE_NUM_FILES_PRESERVE_ORDER:
          int totalLines = countLines();
          linesPerFile = totalLines / numFiles;
          if ((totalLines % numFiles) != 0)
          {
            linesPerFile++;
          }
          splitByMaxLines();
          break;
        case SPLIT_TYPE_NUM_LINES:
          splitByMaxLines();
          break;
        case SPLIT_TYPE_NUM_BYTES:
          splitByMaxBytes();
          break;
      }
    }
    catch (IOException ioe)
    {
      System.err.println("ERROR:  Unable to split file -- " + ioe);
    }
  }



  /**
   * Counts the number of lines in the input file.
   *
   * @return  The number of lines in the input file.
   *
   * @throws  IOException  If a problem occurs while counting the lines in the
   *                       file.
   */
  public int countLines()
         throws IOException
  {
    int numLines = 0;

    BufferedReader reader = new BufferedReader(new FileReader(inputFile));
    while (reader.ready())
    {
      reader.readLine();
      numLines++;
    }
    reader.close();

    return numLines;
  }



  /**
   * Splits the input file into the specified number of output files.
   *
   * @throws  IOException  If a problem occurs while splitting the file.
   */
  public void splitToNumFiles()
         throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(inputFile));

    BufferedWriter[] writers = new BufferedWriter[numFiles];
    for (int i=0; i < writers.length; i++)
    {
      writers[i] = new BufferedWriter(new FileWriter(outputBase + '.' + (i+1)));
    }

    int fileNum = 0;
    int numLines = 0;
    while (reader.ready())
    {
      String line = reader.readLine();
      writers[fileNum].write(line);
      writers[fileNum].newLine();

      numLines++;
      if ((numLines % 1000) == 0)
      {
        System.out.println("Processed " + numLines + " lines");
      }

      fileNum++;
      if (fileNum >= writers.length)
      {
        fileNum = 0;
      }
    }

    reader.close();


    for (int i=0; i < writers.length; i++)
    {
      writers[i].flush();
      writers[i].close();
    }

    System.out.println("Processed a total of " + numLines + " lines");
  }



  /**
   * Splits the input file into a number of output files based on the number of
   * lines per file.
   *
   * @throws  IOException  If a problem occurs while splitting the file.
   */
  public void splitByMaxLines()
         throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(inputFile));

    int fileNum      = 1;
    int currentLines = 0;
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputBase + '.' +
                                                              fileNum));
    int numLines = 0;
    while (reader.ready())
    {
      String line = reader.readLine();
      currentLines++;

      if (currentLines > linesPerFile)
      {
        writer.flush();
        writer.close();
        fileNum++;
        writer = new BufferedWriter(new FileWriter(outputBase + '.' + fileNum));
        currentLines = 1;
      }

      writer.write(line);
      writer.newLine();

      numLines++;
      if ((numLines % 1000) == 0)
      {
        System.out.println("Processed " + numLines + " lines");
      }
    }

    writer.flush();
    writer.close();
    reader.close();

    System.out.println("Processed a total of " + numLines + " lines");
  }



  /**
   * Splits the input file into a number of output files based on the number of
   * bytes per file.
   *
   * @throws  IOException  If a problem occurs while splitting the file.
   */
  public void splitByMaxBytes()
         throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(inputFile));

    int    eolBytes = 0;
    String eol      = System.getProperty("line.separator");
    if (eol == null)
    {
      eolBytes = 1;
      eol      = "\n";
    }
    else
    {
      eolBytes = eol.length();
    }

    int fileNum      = 1;
    int currentBytes = 0;
    int numLines     = 0;
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputBase + '.' +
                                                              fileNum));
    while (reader.ready())
    {
      String line = reader.readLine();
      currentBytes += (line.length() + eolBytes);

      if (currentBytes > bytesPerFile)
      {
        writer.flush();
        writer.close();
        fileNum++;
        writer = new BufferedWriter(new FileWriter(outputBase + '.' + fileNum));
        currentBytes = line.length() + eolBytes;
      }

      writer.write(line);
      writer.write(eol);

      numLines++;
      if ((numLines % 1000) == 0)
      {
        System.out.println("Processed " + numLines + " lines");
      }
    }

    writer.flush();
    writer.close();
    reader.close();

    System.out.println("Processed a total of " + numLines + " lines");
  }



  /**
   * Prints usage information for this program to standard error.
   */
  public void displayUsage()
  {
    System.err.println("Available Options:");
    System.err.println("-i {path} -- The path to the input file");
    System.err.println("-o {path} -- The path to the base name of the output " +
                       "file");
    System.err.println("-n {num}  -- The number of files to create");
    System.err.println("-N {num}  -- The number of files to create (preserve " +
                       "line order");
    System.err.println("-l {num}  -- Splits into files of at most {num} lines");
    System.err.println("-b {num}  -- Splits into files of at most {num} bytes");
    System.err.println("-H        -- Displays usage information for this " +
                       "program");
  }
}

