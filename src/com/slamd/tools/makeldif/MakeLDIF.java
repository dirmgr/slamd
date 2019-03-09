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



import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import com.unboundid.util.Base64;

import com.slamd.common.Constants;



/**
 * This program allows for the creation of LDIF files in an automated but
 * customizable manner.  You can use template files to specify the kinds of
 * entries that will be created in the final LDIF, and a few kinds of operations
 * can be done to help generate more realistic data.
 *
 *
 * @author   Neil A. Wilson
 */
public class MakeLDIF
{
  /**
   * The version number assigned to the current MakeLDIF source base.
   */
  public static final String VERSION_STRING = "1.3.2";



  /**
   * The end of line character that should be used on the current platform.
   */
  public static String EOL = System.getProperty("line.separator");



  /**
   * The RDN attributes that will be used by default if none are specified.
   */
  public static final String[] DEFAULT_RDN_ATTRS = { "cn" };



  /**
   * The set of characters that should be included in numeric values.
   */
  public static final char[] NUMERIC_CHARS = "0123456789".toCharArray();



  /**
   * The set of characters that should be included in alphabetic values.
   */
  public static final char[] ALPHA_CHARS =
       "abcdefghijklmnopqrstuvwxyz".toCharArray();



  /**
   * The set of characters that should be included in alphanumeric values.
   */
  public static final char[] ALPHANUMERIC_CHARS =
       "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();



  /**
   * The set of characters that should be included in hexadecimal values.
   */
  public static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();



  /**
   * The set of characters that should be included in base64 values.
   */
  public static final char[] BASE64_CHARS =
       ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
        "0123456789+/").toCharArray();



  /**
   * The set of months that will be used if the name of a month is required.
   */
  public static final String[] MONTH_NAMES =
  {
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
  };



  // A list of the branches defined in the template file.  These need to be
  // processed in order.
  ArrayList<Branch> branches;

  // Indicates whether the end of the CSV file has been reached.
  boolean csvEndReached = false;

  // Indicates whether MakeLDIF is running in debug mode.
  boolean debugMode = false;

  // Indicates whether filter lists are to be generated.
  boolean generateFilterList = false;

  // Indicates whether to ignore the first line of the CSV file.
  boolean ignoreCSVHeaderLine = false;

  // Indicates whether all the filter information should be stored in a single
  // filter file or separated into files based on the filter types.
  boolean separateFilterFiles = false;

  // Indicates whether branch entries should not be written to the resulting
  // LDIF file.
  boolean skipBranchEntries = false;

  // Indicates whether long lines in the LDIF output are to be wrapped.  By
  // default they are not, but can be if you include the "-w" parameter.
  boolean wrapLongLines = false;

  // The reader used to read information from the CSV file.
  BufferedReader csvReader;

  // The writer used to send the DNs of the entries created into a file
  BufferedWriter dnWriter;

  // The writer used to send bind information into a file.
  BufferedWriter bindInfoWriter;

  // The writer used to send information into the LDIF file.
  BufferedWriter ldifWriter;

  // The writer used to send information to the login information file.
  BufferedWriter loginWriter;

  // A character array that contains 5000 characters.  This will be re-used
  // multiple times for improved performance rather than repeatedly
  // re-allocating memory each time an array is needed.
  char[] chars5000 = new char[5000];

  // A map containing lists of values read from file.
  HashMap<String,ValueList> fileLists;

  // A map containing the filter lists.
  HashMap<String,UniqueSortedList> filterListHash;

  // A map containing the templates defined in the template file.
  HashMap<String,Template> templateHash;

  // A map containing value lists
  HashMap<String,ValueList> valueLists;

  // The total number of entries written to the LDIF file so far
  int entriesWritten;

  // The counter used to keep track of the LDIF filename counter if a limited
  // number of entries should be written to a single LDIF file.
  int fileNameCounter = 1;

  // The value that indicates the current position in the list of first names
  int firstNameIndex;

  // The value that indicates the current position in the list of last names
  int lastNameIndex;

  // The maximum number of entries that will be allowed to match a search filter
  // in order for it to be included in the filter file.
  int maxFilterMatches = -1;

  // The maximum number of entries that should be written to a single LDIF file.
  int maxPerFile = -1;

  // The maximum number of entries that should be generated for each template
  // under each branch.
  int maxPerTemplate = -1;

  // The minimum number of entries that will be needed to match a search filter
  // in order for it to be included in the filter file.
  int minFilterMatches = 1;

  // The number of times that the larger of the first/last name lists has been
  // completed
  int nameLoopCounter;

  // The counter that is added to the last name after all unique combinations of
  // first and last names have been exhausted so that we can re-use all of those
  // values and still maintain uniqueness.
  int nameUniquenessCounter;

  // The number of elements in the first name list.
  int numFirstNames;

  // The number of elements in the last name list.
  int numLastNames;

  // The number of characters to include in substring filters.
  int numSubstringChars = 3;

  // The seed that is to be used to initialize the random number generator.
  long randomSeed = -1;

  // The random number generator being used.
  Random random;

  // The name of the file to use to write the DN and password for generated
  // entries (provided that they have a password).  Specified with the "-b"
  // parameter.
  String bindInfoFile = null;

  // The delimiter to use for the CSV data.
  String csvDelimiter = null;

  // The path to the CSV file containing data to use when generating the
  // entries.
  String csvFile = null;

  // The name of the file to use to write the DNs of the entries that have been
  // created.
  String dnFile = null;

  // The base that we will use for GUIDs instead of the MAC address of the
  // network interface (since that may not exist, and Java can't get to it
  // without native calls anyway).
  String guidBase;

  // The name of the file to which search filter information will be written.
  // Specified with the "-F" parameter.
  String filterFile = null;

  // The name of the file containing the list of first names to use.  Specified
  // with the "-f" parameter.
  String firstNameFile = "first.names";

  // The name of the file containing the list of last names to use.  Specified
  // with the "-l" parameter.
  String lastNameFile = "last.names";

  // The path and name of the LDIF file to create.  Specified with the "-o"
  // parameter.
  String ldifFile = null;

  // The path and name of the file to create with login ID/password information.
  // Specified with the "-L" parameter.
  String loginFile = null;

  // The name of the attribute that will be used as the login ID for the user.
  // Specified with the "-i" parameter.
  String loginIDAttr = "uid";

  // The path to the directory containing the MakeLDIF resource files.
  // Specified with the "-r" parameter.
  String resourceDir = null;

  // The path and name of the template file to use.  Specified with the "-t"
  // parameter.
  String templateFile = null;

  // The list of first names to use when generating the LDIF
  String[] firstNames;

  // The list of last names to use when generating the LDIF
  String[] lastNames;



  /**
   * Creates a new instance of the LDIF generator and passes the command-line
   * arguments along to it.
   *
   * @param  args  The command-line arguments provided to the program.
   */
  public static void main(String[] args)
  {
    new MakeLDIF(args);
  }



  /**
   * Creates a new instance of the LDIF generator that parses the command-line
   * parameters and coordinates the LDIF creation.
   *
   * @param  args  The command-line arguments provided to the program.
   */
  public MakeLDIF(String[] args)
  {
    // Temporary storage for the filter types information.
    ArrayList<String[]> filterTypesList = new ArrayList<String[]>();

    // Initialize the filter list hash.
    filterListHash = new HashMap<String,UniqueSortedList>();


    // Iterate through the command-line parameters and set the values of the
    // corresponding instance variables
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-r"))
      {
        resourceDir = args[++i];
      }
      else if (args[i].equals("-f"))
      {
        firstNameFile = args[++i];
      }
      else if (args[i].equals("-l"))
      {
        lastNameFile = args[++i];
      }
      else if (args[i].equals("-o"))
      {
        ldifFile = args[++i];
      }
      else if (args[i].equals("-t"))
      {
        templateFile = args[++i];
      }
      else if (args[i].equals("-c"))
      {
        csvFile = args[++i];
      }
      else if (args[i].equals("-C"))
      {
        csvDelimiter = args[++i];
        int tabPos = csvDelimiter.indexOf("\\t");
        while (tabPos >= 0)
        {
          csvDelimiter = csvDelimiter.substring(0, tabPos) + '\t' +
                         csvDelimiter.substring(tabPos+2);
          tabPos = csvDelimiter.indexOf("\\t");
        }
      }
      else if (args[i].equals("-I"))
      {
        ignoreCSVHeaderLine = true;
      }
      else if (args[i].equals("-d"))
      {
        dnFile = args[++i];
      }
      else if (args[i].equals("-b"))
      {
        bindInfoFile = args[++i];
      }
      else if (args[i].equals("-i"))
      {
        loginIDAttr = args[++i].toLowerCase();
      }
      else if (args[i].equals("-L"))
      {
        loginFile = args[++i];
      }
      else if (args[i].equals("-F"))
      {
        filterFile = args[++i];
      }
      else if (args[i].equals("-M"))
      {
        separateFilterFiles = true;
      }
      else if (args[i].equals("-T"))
      {
        String typesStr = args[++i];
        int colonPos = typesStr.indexOf(':');
        if ((colonPos <= 0) || (colonPos == (typesStr.length() - 1)))
        {
          System.err.println("ERROR:  filter types must be in the format " +
                             "attr:type[,type2]");
          displayUsage();
          System.exit(1);
        }

        String attrName = typesStr.substring(0, colonPos).toLowerCase();
        ArrayList<String> typesList = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(typesStr.substring(colonPos+1),
                                                 ",");
        boolean maintainEq         = false;
        boolean maintainSubstring  = false;
        boolean maintainSubInitial = false;
        boolean maintainSubAny     = false;
        boolean maintainSubFinal   = false;

        while (st.hasMoreTokens())
        {
          String type = st.nextToken().toLowerCase();
          if (type.equals("eq"))
          {
            maintainEq = true;
          }
          else if (type.equals("sub"))
          {
            maintainSubstring  = true;
            maintainSubInitial = true;
            maintainSubAny     = true;
            maintainSubFinal   = true;
          }
          else if (type.equals("subinitial"))
          {
            maintainSubInitial = true;
          }
          else if (type.equals("subany"))
          {
            maintainSubAny = true;
          }
          else if (type.equals("subfinal"))
          {
            maintainSubFinal = true;
          }
          else
          {
            System.err.println("ERROR:  " + type + " is not a valid filter " +
                               "type.  Allowed types are eq, sub, subInitial," +
                               "subAny, and subFinal");
            displayUsage();
            System.exit(1);
          }
        }

        filterListHash.put(attrName, new UniqueSortedList(maintainEq,
                                                          maintainSubstring,
                                                          maintainSubInitial,
                                                          maintainSubAny,
                                                          maintainSubFinal));

        String[] attrFilterElements = new String[typesList.size()+1];
        attrFilterElements[0] = attrName;
        for (int j=0; j < typesList.size(); j++)
        {
          attrFilterElements[j+1] = typesList.get(j);
        }
        filterTypesList.add(attrFilterElements);
      }
      else if (args[i].equals("-s"))
      {
        randomSeed = Long.parseLong(args[++i]);
      }
      else if (args[i].equals("-m"))
      {
        maxPerFile = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-x"))
      {
        maxPerTemplate = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-n"))
      {
        numSubstringChars = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-N"))
      {
        minFilterMatches = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-X"))
      {
        maxFilterMatches = Integer.parseInt(args[++i]);
      }
      else if (args[i].equals("-w"))
      {
        wrapLongLines = true;
      }
      else if (args[i].equals("-S"))
      {
        skipBranchEntries = true;
      }
      else if (args[i].equals("-U"))
      {
        EOL = "\n";
      }
      else if (args[i].equals("-D"))
      {
        debugMode = true;
        System.err.println("Debug mode enabled.");
      }
      else if (args[i].equals("-V"))
      {
        displayVersion();
        System.exit(0);
      }
      else if (args[i].equals("-H"))
      {
        displayUsage();
        System.exit(0);
      }
      else
      {
        System.err.println("Unknown argument:  " + args[i]);
        displayUsage();
        System.exit(1);
      }
    }


    // Make sure that values were specified for the required attributes
    if (ldifFile == null)
    {
      System.err.println("Error:  No output file specified (use -o)");
      displayUsage();
      System.exit(1);
    }

    if (templateFile == null)
    {
      System.err.println("Error:  No template file specified (use -t)");
      displayUsage();
      System.exit(1);
    }


    // If there were any filter types specified, then make sure there was a
    // filter file specified.  If so, then finalize the filter type information.
    if (! filterListHash.isEmpty())
    {
      generateFilterList = true;

      if (filterFile == null)
      {
        System.err.println("Error:  A filter file must be specified when " +
                           "a list of filter types is provided.");
        displayUsage();
        System.exit(1);
      }
      else
      {
        for (UniqueSortedList usl : filterListHash.values())
        {
          usl.setMatchCriteria(numSubstringChars, minFilterMatches,
                               maxFilterMatches);
        }
      }
    }
    else if (filterFile != null)
    {
      System.err.println("A filter file was specified, but no filter types " +
                         "were provided.  A filter file will not be created.");
    }

    // Initialize the random number generator.
    if (randomSeed >= 0)
    {
      random = new Random(randomSeed);
    }
    else
    {
      random = new Random();
    }

    // Set the values of the remaining instance variables
    fileLists             = new HashMap<String,ValueList>();
    templateHash          = new HashMap<String,Template>();
    valueLists            = new HashMap<String,ValueList>();
    branches              = new ArrayList<Branch>();
    firstNames            = new String[0];
    lastNames             = new String[0];
    firstNameIndex        = 0;
    lastNameIndex         = 0;
    nameUniquenessCounter = 1;
    nameLoopCounter       = 0;

    StringBuilder buffer = new StringBuilder(12);
    generateRandomValue(HEX_CHARS, 12, buffer);
    guidBase = buffer.toString();


    // Load the first and last name lists into memory
    loadNames();


    // Load the template file information into memory
    loadTemplate();


    // Now do the actual work
    generateLDIF();
  }



  /**
   * Reads the first and last name files and stores the values read into the
   * appropriate lists.  The memory requirements for this should be minimal,
   * because we are storing the names separately and then using an algorithm to
   * extract every possible unique combination.
   */
  public void loadNames()
  {
    BufferedReader reader = null;


    // Open the file containing the first names
    File f = getFileForName(firstNameFile);
    try
    {
      reader = new BufferedReader(new FileReader(f));
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Could not open first name file " +
                         f.getAbsolutePath() + " -- " + ioe);
      System.exit(1);
    }


    // Read the first names into the first name list
    ArrayList<String> firstNameList = new ArrayList<String>();
    try
    {
      while (reader.ready())
      {
        String line = reader.readLine();
        if (line.length() > 0)
        {
          firstNameList.add(line);
        }
      }
      reader.close();
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Error reading first name file -- " + ioe);
      System.exit(1);
    }
    firstNames = new String[firstNameList.size()];
    firstNameList.toArray(firstNames);
    numFirstNames = firstNames.length;


    // Open the file containing the last names
    f = getFileForName(lastNameFile);
    try
    {
      reader = new BufferedReader(new FileReader(f));
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Could not open last name file " +
                         f.getAbsolutePath() + " -- " + ioe);
      System.exit(1);
    }


    // Read the last names into the last name list
    ArrayList<String> lastNameList = new ArrayList<String>();
    try
    {
      while (reader.ready())
      {
        String line = reader.readLine();
        if (line.length() > 0)
        {
          lastNameList.add(line);
        }
      }
      reader.close();
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Error reading last name file -- " + ioe);
      System.exit(1);
    }
    lastNames = new String[lastNameList.size()];
    lastNameList.toArray(lastNames);
    numLastNames = lastNames.length;
  }



  /**
   * Reads the template file and extracts the branch and template definitions
   * from it.
   */
  public void loadTemplate()
  {
    // The set of variables that have been defined in the template file.
    HashMap<String,String> variableHash = new HashMap<String,String>();

    // The lines contained in the current definition
    ArrayList<String> currentDefinition = new ArrayList<String>();

    // The type of definition we are working on (0=unknown, 1=branch,
    // 2=template)
    int definitionType = 0;

    // The name of the branch or template we are working on
    String name = null;


    // Open the template file
    File f = getFileForName(templateFile);
    BufferedReader reader = null;
    try
    {
      reader = new BufferedReader(new FileReader(f));
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Could not open template file " + f.getAbsolutePath() +
                         " -- " + ioe);
      System.exit(1);
    }


    // Read through the template file an entry at a time (an entry is a set of
    // consecutive non-blank lines) and process it.  This code is pretty
    // inefficient, but it works and is only called at the very beginning of the
    // program and not when actually creating the LDIF.
    boolean lastLine = false;
    try
    {
      while (! lastLine)
      {
        String line = "";
        if (reader.ready())
        {
          line = reader.readLine();
        }
        else
        {
          lastLine = true;
        }

        // If the line starts with "define", then it is a variable definition.
        // Parse it and store it in the variable hash.
        if (line.toLowerCase().startsWith("define "))
        {
          int equalPos = line.indexOf('=');
          if (equalPos < 0)
          {
            System.err.println("ERROR:  Variable definitions must be in the " +
                               "form:");
            System.err.println("     define name=value");
            System.err.println("Line '" + line + "' ignored.");
          }

          String variableName  = line.substring(7, equalPos).trim();
          String variableValue = line.substring(equalPos+1).trim();
          variableHash.put(variableName, variableValue);
          continue;
        }

        // If the line contains a value between opening and closing brackets
        // ("[" and "]"), then it will be considered a variable definition and
        // its value will be replaced with the value of the specified variable
        // as we are reading in the template.  This allows variable definitions
        // to be used anywhere in branch or template entries, even in the name
        // of the branch or template.
        boolean needReprocess = true;
        int openPos = 0;
        int startPos = 0;
        while (needReprocess && ((openPos = line.indexOf('[', startPos)) >= 0))
        {
          if ((openPos > 0) && (line.charAt(openPos-1) == '\\'))
          {
            needReprocess = true;
            line = line.substring(0, openPos-1) + '[' +
                   line.substring(openPos+1);
            startPos = openPos;
            continue;
          }

          needReprocess = false;

          int closePos = line.indexOf(']');
          if (closePos > 0)
          {
            String variableName  = line.substring(openPos+1, closePos);
            String variableValue = variableHash.get(variableName);
            line = line.substring(0, openPos) + variableValue +
                   line.substring(closePos+1);
            needReprocess = true;
          }
        }

        // If we have a non-blank line, then just store it in the current
        // definition list.  Also, try to determine whether it is a branch
        // or a template definition to ease processing later.
        if (line.length() > 0)
        {
          currentDefinition.add(line);
          if (line.toLowerCase().startsWith("branch: "))
          {
            definitionType = 1;
            name = line.substring(8);
          }
          else if (line.toLowerCase().startsWith("template: "))
          {
            definitionType = 2;
            name = line.substring(10);
          }
        }
        else
        {
          // Only try to process it if there is something to process
          if (! currentDefinition.isEmpty())
          {
            if (definitionType == 1)
            {
              // It's a branch, so try to get the relevant information from it.
              Branch branch = new Branch(name);
              int numSubordinates = 0;
              String templateName = null;

              for (int i=0; i < currentDefinition.size(); i++)
              {
                line = currentDefinition.get(i);
                if (line.toLowerCase().startsWith("branch: "))
                {
                  // Ignore this
                }
                else if (line.toLowerCase().startsWith("subordinatetemplate: "))
                {
                  String templateLine = line.substring(21);
                  int colonPos = templateLine.indexOf(':');
                  if (colonPos > 0)
                  {
                    templateName = templateLine.substring(0, colonPos);
                    numSubordinates = Integer.parseInt(
                                           templateLine.substring(colonPos+1));
                    branch.addTemplate(templateName, numSubordinates);
                  }
                }
                else
                {
                  branch.addExtraLine(line);
                }
              }
              branches.add(branch);
            }
            else if (definitionType == 2)
            {
              // It's a template, so determine which RDN attribute to use and
              // parse out the template and subtemplate names.  Everything else
              // will go into the attribute list.
              Template template = new Template(name, DEFAULT_RDN_ATTRS);
              for (int i=0; i < currentDefinition.size(); i++)
              {
                line = currentDefinition.get(i);
                if (line.toLowerCase().startsWith("template: "))
                {
                  // ignore this because we already have the template name and
                  // don't want to make it an attribute
                }
                else if (line.toLowerCase().startsWith(
                                      "subordinatetemplate: "))
                {
                  // It's a subtemplate definition so get the name and count
                  // from it
                  String stLine = line.substring(21);
                  int colonPos = stLine.indexOf(':');
                  if (colonPos > 0)
                  {
                    String stName = stLine.substring(0, colonPos);
                    int stCnt = Integer.parseInt(stLine.substring(colonPos+1));
                    template.addSubtemplate(stName, stCnt);
                  }
                  else
                  {
                    template.addSubtemplate(stLine, 1);
                  }
                }
                else if (line.toLowerCase().startsWith("rdnattr: "))
                {
                  String rdnAttrStr = line.substring(9);
                  if (rdnAttrStr.indexOf('+') < 0)
                  {
                    template.setRDNAttributes(new String[] { rdnAttrStr });
                  }
                  else
                  {
                    ArrayList<String> rdnAttrs = new ArrayList<String>();
                    StringTokenizer st = new StringTokenizer(rdnAttrStr, "+");
                    while (st.hasMoreTokens())
                    {
                      rdnAttrs.add(st.nextToken());
                    }

                    String[] rdnAttrArray = new String[rdnAttrs.size()];
                    rdnAttrs.toArray(rdnAttrArray);
                    template.setRDNAttributes(rdnAttrArray);
                  }
                }
                else if (line.toLowerCase().startsWith("extends: "))
                {
                  template.parentTemplateName = line.substring(9);
                }
                else if (line.indexOf(": ") > 0)
                {
                  if ((line.indexOf(":: ") > 0) &&
                      (line.indexOf(":: ") < line.indexOf(": ")))
                  {
                    String attrName = line.substring(0, line.indexOf(":: "));
                    String value = line.substring(line.indexOf(":: ") + 3);
                    template.addAttribute(attrName, ":: ", value);
                  }
                  else
                  {
                    String attrName = line.substring(0, line.indexOf(": "));
                    String value = line.substring(line.indexOf(": ") + 2);
                    template.addAttribute(attrName, ": ", value);
                  }
                }
              }


              // Finalize this template to configure it for more efficient
              // processing later.
              template.completeInitialization();
              templateHash.put(name.toLowerCase(), template);
            }

            currentDefinition.clear();
            definitionType = 0;
            name = null;
          }
        }
      }

      reader.close();
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Error reading template file " + templateFile);
      ioe.printStackTrace();
      System.exit(1);
    }
  }



  /**
   * Actually generates the LDIF file by creating the specified branch entries,
   * then the subordinate entries, and writing them to disk.
   */
  public void generateLDIF()
  {
    try
    {
      // Open the CSV file for reading.
      if (csvFile != null)
      {
        File f = getFileForName(csvFile);
        csvReader = new BufferedReader(new FileReader(f));
        if (ignoreCSVHeaderLine)
        {
          csvReader.readLine();
        }
      }

      // Open the output file for writing
      if (dnFile != null)
      {
        dnWriter = new BufferedWriter(new FileWriter(dnFile));
      }
      if (bindInfoFile != null)
      {
        bindInfoWriter = new BufferedWriter(new FileWriter(bindInfoFile));
      }
      if (loginFile != null)
      {
        loginWriter = new BufferedWriter(new FileWriter(loginFile));
      }
      ldifWriter = new BufferedWriter(new FileWriter(ldifFile));


      // Iterate through all the branches and process them in the order they
      // were defined in the template file
      for (int i=0; i < branches.size(); i++)
      {
        // Create the branch entry itself.  This will just be a standard entry
        // with no real processing.
        Branch branch = branches.get(i);
        if (! skipBranchEntries)
        {
          createBranchEntry(branch);
        }

        // Create the appropriate number of subordinate entries for that branch
        if (branch.hasSubordinates())
        {
          String[] templates = branch.getSubordinateTemplates();
          for (int j=0; j < templates.length; j++)
          {
            Template template = getTemplate(templates[j]);
            if (template == null)
            {
              System.err.println("Unable to find template " + templates[j] +
                                 " for branch " + branch.getDN() +
                                 " -- aborting");
              ldifWriter.flush();
              ldifWriter.close();

              if (dnFile != null)
              {
                dnWriter.flush();
                dnWriter.close();
              }

              if (bindInfoFile != null)
              {
                bindInfoWriter.flush();
                bindInfoWriter.close();
              }

              System.exit(1);
            }

            String parentDN = branch.getDN();
            int numSubordinates = branch.numEntriesForTemplate(templates[j]);
            if (maxPerTemplate >= 0)
            {
              numSubordinates = Math.min(numSubordinates, maxPerTemplate);
            }
            template.resetCounters();
            template.reinitializeCustomTags();
            for (int k=0; ((! csvEndReached) && (k < numSubordinates)); k++)
            {
              try
              {
                createTemplateEntry(parentDN, null, template);
              }
              catch (Exception e)
              {
                if (debugMode)
                {
                  e.printStackTrace();
                }

                System.err.println("ERROR creating entry below parent  " +
                                   parentDN + ":  " + e);
                System.err.println("The entry was not written to the LDIF " +
                                   "file.");
              }
            }
          }
        }
      }

      ldifWriter.flush();
      ldifWriter.close();

      if (dnFile != null)
      {
        dnWriter.flush();
        dnWriter.close();
      }

      if (bindInfoFile != null)
      {
        bindInfoWriter.flush();
        bindInfoWriter.close();
      }

      if (loginFile != null)
      {
        loginWriter.flush();
        loginWriter.close();
      }

      if (csvReader != null)
      {
        csvReader.close();
      }

      System.out.println("Processing complete.");
      System.out.println(entriesWritten + " total entries written.");

      if (! filterListHash.isEmpty())
      {
        if (! separateFilterFiles)
        {
          System.out.println("Writing filters to " + filterFile);
          BufferedWriter filterWriter =
               new BufferedWriter(new FileWriter(filterFile));
          for (String attr : filterListHash.keySet())
          {
            UniqueSortedList usl = filterListHash.get(attr);

            if (usl.maintainEqualityList())
            {
              String[] values = usl.getEqualityValues();
              for (int i=0; i < values.length; i++)
              {
                filterWriter.write('(' + attr + '=' + values[i] + ')' + EOL);
              }
              System.out.println("Wrote " + values.length +
                                 " equality filters for " + attr);
            }
            if (usl.maintainSubInitialList())
            {
              String[] values = usl.getSubInitialValues();
              for (int i=0; i < values.length; i++)
              {
                filterWriter.write('(' + attr + '=' + values[i] + "*)" + EOL);
              }
              System.out.println("Wrote " + values.length +
                                 " subInitial filters for " + attr);
            }
            if (usl.maintainSubAnyList())
            {
              String[] values = usl.getSubAnyValues();
              for (int i=0; i < values.length; i++)
              {
                filterWriter.write('(' + attr + "=*" + values[i] + "*)" + EOL);
              }
              System.out.println("Wrote " + values.length +
                                 " subAny filters for " + attr);
            }
            if (usl.maintainSubFinalList())
            {
              String[] values = usl.getSubFinalValues();
              for (int i=0; i < values.length; i++)
              {
                filterWriter.write('(' + attr + "=*" + values[i] + ')' + EOL);
              }
              System.out.println("Wrote " + values.length +
                                 " subFinal filters for " + attr);
            }
          }

          filterWriter.flush();
          filterWriter.close();
        }
        else
        {
          for (String attr : filterListHash.keySet())
          {
            UniqueSortedList usl = filterListHash.get(attr);

            if (usl.maintainEqualityList())
            {
              String filename = filterFile + '.' + attr + ".eq";
              BufferedWriter fileWriter =
                   new BufferedWriter(new FileWriter(filename));

              String[] values = usl.getEqualityValues();
              for (int i=0; i < values.length; i++)
              {
                fileWriter.write('(' + attr + '=' + values[i] + ')' + EOL);
              }
              System.out.println("Wrote " + values.length +
                                 " equality filters for " + attr + " into " +
                                 filename);

              fileWriter.flush();
              fileWriter.close();
            }

            if (usl.maintainSubstringList())
            {
              String filename = filterFile + '.' + attr + ".sub";
              BufferedWriter fileWriter =
                   new BufferedWriter(new FileWriter(filename));
              int totalValues = 0;

              String[] values = usl.getSubInitialValues();
              for (int i=0; i < values.length; i++)
              {
                fileWriter.write('(' + attr + '=' + values[i] + "*)" + EOL);
              }
              totalValues += values.length;

              values = usl.getSubAnyValues();
              for (int i=0; i < values.length; i++)
              {
                fileWriter.write('(' + attr + "=*" + values[i] + "*)" + EOL);
              }
              totalValues += values.length;

              values = usl.getSubFinalValues();
              for (int i=0; i < values.length; i++)
              {
                fileWriter.write('(' + attr + "=*" + values[i] + ')' + EOL);
              }
              totalValues += values.length;

              System.out.println("Wrote " + totalValues +
                                 " substring filters for " + attr + " into " +
                                 filename);

              fileWriter.flush();
              fileWriter.close();
            }
            else
            {

              if (usl.maintainSubInitialList())
              {
                String filename = filterFile + '.' + attr + ".subInitial";
                BufferedWriter fileWriter =
                     new BufferedWriter(new FileWriter(filename));
                String[] values = usl.getSubInitialValues();
                for (int i=0; i < values.length; i++)
                {
                  fileWriter.write('(' + attr + '=' + values[i] + "*)" + EOL);
                }
                System.out.println("Wrote " + values.length +
                                   " subInitial filters for " + attr +
                                   " into " + filename);
                fileWriter.flush();
                fileWriter.close();
              }

              if (usl.maintainSubAnyList())
              {
                String filename = filterFile + '.' + attr + ".subAny";
                BufferedWriter fileWriter =
                     new BufferedWriter(new FileWriter(filename));
                String[] values = usl.getSubAnyValues();
                for (int i=0; i < values.length; i++)
                {
                  fileWriter.write('(' + attr + "=*" + values[i] + "*)" + EOL);
                }
                System.out.println("Wrote " + values.length +
                                   " subAny filters for " + attr +
                                   " into " + filename);
                fileWriter.flush();
                fileWriter.close();
              }

              if (usl.maintainSubFinalList())
              {
                String filename = filterFile + '.' + attr + ".subFinal";
                BufferedWriter fileWriter =
                     new BufferedWriter(new FileWriter(filename));
                String[] values = usl.getSubFinalValues();
                for (int i=0; i < values.length; i++)
                {
                  fileWriter.write('(' + attr + "=*" + values[i] + ')' + EOL);
                }
                System.out.println("Wrote " + values.length +
                                   " subFinal filters for " + attr +
                                   " into " + filename);
                fileWriter.flush();
                fileWriter.close();
              }
            }
          }
        }
      }
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Error writing to LDIF file " + templateFile);
      ioe.printStackTrace();
      System.exit(1);
    }
  }



  /**
   * Creates an entry in LDIF form based on the provided DN.  It attempts to
   * determine the type of entry to create based on the RDN attribute.
   * Supported RDN attributes are dc, o, ou, c, and l.  If any other RDN
   * attributes are specified, then the entry will be created using the
   * extensibleObject objectclass.  Multivalued RDNs are not supported, and
   * neither are escaped commas in the RDN.
   *
   * @param  branch  The branch entry to create.
   */
  public void createBranchEntry(Branch branch)
  {
    // If the entry DN is null or empty, then abort.  If the entry DN doesn't
    // have an equal sign, then abort.
    String entryDN = branch.getDN();
    if ((entryDN == null) || (entryDN.length() == 0) ||
        (entryDN.indexOf('=') <= 0))
    {
      return;
    }


    // Try to find the RDN attribute and value.
    String rdnAttr = entryDN.substring(0, entryDN.indexOf('='));
    String lowerRDNAttr = rdnAttr.toLowerCase();
    String rdnValue = "";
    if (entryDN.indexOf(',', entryDN.indexOf('=')) > 0)
    {
      rdnValue = entryDN.substring(entryDN.indexOf('=') + 1,
                                   entryDN.indexOf(','));
    }
    else
    {
      rdnValue = entryDN.substring(entryDN.indexOf('=') + 1);
    }


    // Create the string to be returned.  StringBuilders are faster than string
    // concatenation, but it makes this code much harder to read, and because
    // branch entries are the vast minority in LDIF files, I prefer greatly
    // improved readability to a very minor performance boost.
    String entry = null;


    // Create the appropriate entry type based on the RDN attribute
    if (lowerRDNAttr.equals("dc"))
    {
      entry = "dn: " + entryDN + EOL +
              "objectclass: top" + EOL +
              "objectclass: domain" + EOL +
              "dc: " + rdnValue + EOL;
    }
    else if (lowerRDNAttr.equals("o"))
    {
      entry = "dn: " + entryDN + EOL +
              "objectclass: top" + EOL +
              "objectclass: organization" + EOL +
              "o: " + rdnValue + EOL;
    }
    else if (lowerRDNAttr.equals("ou"))
    {
      entry = "dn: " + entryDN + EOL +
              "objectclass: top" + EOL +
              "objectclass: organizationalUnit" + EOL +
              "ou: " + rdnValue + EOL;
    }
    else if (lowerRDNAttr.equals("c"))
    {
      entry = "dn: " + entryDN + EOL +
              "objectclass: top" + EOL +
              "objectclass: country" + EOL +
              "c: " + rdnValue + EOL;
    }
    else if (lowerRDNAttr.equals("l"))
    {
      entry = "dn: " + entryDN + EOL +
              "objectclass: top" + EOL +
              "objectclass: locality" + EOL +
              "l: " + rdnValue + EOL;
    }
    else
    {
      entry = "dn: " + entryDN + EOL +
              "objectclass: top" + EOL +
              "objectclass: extensibleObject" + EOL +
               rdnAttr + ": " + rdnValue + EOL;
    }


    // Add the extra line information.  Extra lines will be parsed for
    // variable replacement, but no other tokens will be recognized.
    String[] extraLines = branch.getExtraLines();
    for (int i=0; i < extraLines.length; i++)
    {
      entry += extraLines[i] + EOL;
    }

    entry += EOL;

    writeEntry(entryDN, entry, null, null);
  }



  /**
   * Creates a subordinate entry for the specified branch.  Any translation that
   * needs to be done on the attribute values will be taken care of here.
   *
   * @param  parentDN          The DN under which to create the entries.
   * @param  parentAttributes  The set of attributes that comprise the parent
   *                           entry.
   * @param  template          The template to use for the entry.
   */
  public void createTemplateEntry(String parentDN,
                                  AttributeList parentAttributes,
                                  Template template)
  {
    int[]         subtemplateCounts = template.getSubtemplateCounts();
    String[]      rdnAttrs          = template.getRDNAttributes();
    String[]      lowerRDNAttrs     = template.getLowerRDNAttributes();
    String[]      subtemplateNames  = template.getSubtemplateNames();
    String[][]    attrComponents    = template.getAttributeComponents();
    AttributeList attrList          = new AttributeList();


    // Get placeholders for the first and last names to use.  They won't
    // actually be retrieved unless the "<first>" or "<last>" tags are
    // encountered.
    String firstName = null;
    String lastName  = null;


    // Set a flag that indicates whether this value needs to be reprocessed or
    // not.  If any changes were made to a value in one iteration, then it
    // should be reprocessed so that additional processing can be performed if
    // necessary.  Another flag should be used to determine if we're in a
    // reprocess in order to know
    boolean  needReprocess = false;
    String   value         = null;
    String   loginIDValue  = null;
    String   passwordValue = null;
    String[] rdnValues     = new String[rdnAttrs.length];


    // The name of the current attribute we are working on in lowercase.
    String lowerAttrName;


    // Get the next CSV line data if appropriate.
    String[] csvData = null;
    if (csvFile != null)
    {
      csvData = nextCSVLine();
      if (csvData == null)
      {
        if (debugMode)
        {
          System.err.println("Reached the end of the CSV file");
        }

        csvEndReached = true;
        return;
      }
    }


    // Iterate through all the attributes and do any processing that needs to be
    // done.
    for (int i=0; i < attrComponents.length; i++)
    {
      lowerAttrName   = attrComponents[i][3];
      value           = attrComponents[i][2];
      needReprocess = true;
      int pos;

      // If the value contains "<presence:", then determine if it should
      // actually be included in this entry.  If not, then just go to the next
      // attribute
      if ((pos = value.indexOf("<presence:")) >= 0)
      {
        int closePos = value.indexOf('>', pos);
        if (closePos > pos)
        {
          String numStr = value.substring(pos+10, closePos);
          try
          {
            int percentage = Integer.parseInt(numStr);
            int randomValue = ((random.nextInt() & 0x7FFFFFFF) % 100) + 1;
            if (randomValue <= percentage)
            {
              // We have determined that this value should be included in the
              // LDIF output, so remove the "<presence:x>" tag and let it go on
              // to do the rest of the processing on this entry
              value = value.substring(0, pos) + value.substring(closePos+1);
            }
            else
            {
              // We have determined that this value should not be included in
              // the LDIF output, so just go on to the next one.
              continue;
            }
          }
          catch (NumberFormatException nfe)
          {
            if (debugMode)
            {
              nfe.printStackTrace();
            }
          }
        }
      }

      // If the value contains "<ifpresent:{attrname}>", then determine if it
      // should actually be included in this entry.  If not, then just go to the
      // next attribute.
      if ((pos = value.indexOf("<ifpresent:")) >= 0)
      {
        int closePos = value.indexOf('>', pos);
        if (closePos > pos)
        {
          int colonPos = value.indexOf(':', pos+11);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            // Look for a specific value to be present.
            boolean  matchFound = false;
            String   attrName   = value.substring(pos+11, colonPos);
            String   matchValue = value.substring(colonPos+1, closePos);
            String[] values     = attrList.getValues(attrName);

            for (int j=0; j < values.length; j++)
            {
              if (matchValue.equalsIgnoreCase(values[j]))
              {
                value = value.substring(0, pos) + value.substring(closePos+1);
                matchFound = true;
                break;
              }
            }

            if (! matchFound)
            {
              continue;
            }
          }
          else
          {
            // Just look for the attribute to be present.
            String attrName  = value.substring(pos+11, closePos);
            String attrValue = attrList.getValue(attrName);
            if ((attrValue == null) || (attrValue.length() == 0))
            {
              // The requested attribute is not present, so skip this line.
              continue;
            }
            else
            {
              value = value.substring(0, pos) + value.substring(closePos+1);
            }
          }
        }
      }

      // If the value contains "<ifabsent:{attrname}>", then determine if it
      // should actually be included in this entry.  If not, then just go to the
      // next attribute.
      if ((pos = value.indexOf("<ifabsent:")) >= 0)
      {
        int closePos = value.indexOf('>', pos);
        if (closePos > pos)
        {
          int colonPos = value.indexOf(':', pos+10);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            // Look for a specific value to be present.
            boolean  matchFound = false;
            String   attrName   = value.substring(pos+10, colonPos);
            String   matchValue = value.substring(colonPos+1, closePos);
            String[] values     = attrList.getValues(attrName);

            for (int j=0; j < values.length; j++)
            {
              if (matchValue.equalsIgnoreCase(values[j]))
              {
                matchFound = true;
                break;
              }
            }

            if (matchFound)
            {
              continue;
            }
            else
            {
              value = value.substring(0, pos) + value.substring(closePos+1);
            }
          }
          else
          {
            // Just look for the attribute to be present.
            String attrName  = value.substring(pos+10, closePos);
            String attrValue = attrList.getValue(attrName);
            if ((attrValue != null) && (attrValue.length() > 0))
            {
              // The requested attribute is present, so skip this line.
              continue;
            }
            else
            {
              value = value.substring(0, pos) + value.substring(closePos+1);
            }
          }
        }
      }

      while (needReprocess && (value.indexOf('<') >= 0))
      {
        needReprocess = false;


        // If the value contains "<first>" then replace that with the first name
        if ((pos = value.indexOf("<first>")) >= 0)
        {
          if (firstName == null)
          {
            String[] names = nextFirstAndLastNames();
            firstName = names[0];
            lastName  = names[1];
          }

          value = value.substring(0, pos) + firstName +
                  value.substring(pos + 7);
          needReprocess = true;
        }
        // If the value contains "<last>" then replace that with the last name
        if ((pos = value.indexOf("<last>")) >= 0)
        {
          if (firstName == null)
          {
            String[] names = nextFirstAndLastNames();
            firstName = names[0];
            lastName  = names[1];
          }

          value = value.substring(0, pos) + lastName + value.substring(pos + 6);
          needReprocess = true;
        }

        // If the value contains "<dn>" then replace that with the DN of the
        // current entry.  Note for this to be valid, the RDN attribute must
        // already have a value.
        if ((pos = value.indexOf("<dn>")) >= 0)
        {
          StringBuilder buffer = new StringBuilder(75);
          buffer.append(value.substring(0, pos));

          String separator = "";
          for (int j=0; j < rdnAttrs.length; j++)
          {
            buffer.append(separator);
            buffer.append(rdnAttrs[j]);
            buffer.append('=');
            buffer.append(rdnValues[j]);

            separator = "+";
          }

          buffer.append(',');
          buffer.append(parentDN);
          buffer.append(value.substring(pos+4));

          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<_dn>", then replace that with the DN of the
        // current entry, but with underscores instead of commas.  This is an
        // undocumented feature for use with identity server.  Note for this to
        // be valid, the RDN attribute must already have a value.
        if ((pos = value.indexOf("<_dn>")) >= 0)
        {
          StringBuilder buffer = new StringBuilder(75);
          String separator = "";
          for (int j=0; j < rdnAttrs.length; j++)
          {
            buffer.append(separator);
            buffer.append(rdnAttrs[j]);
            buffer.append('=');
            buffer.append(rdnValues[j]);

            separator = "+";
          }

          buffer.append('_');
          buffer.append(parentDN);

          value = value.substring(0, pos) +
                  buffer.toString().replace(',', '_') +
                  value.substring(pos + 5);
          needReprocess = true;
        }

        // If the value contains "<parentdn>" then replace that with the DN of
        // the parent entry
        if ((pos = value.indexOf("<parentdn>")) >= 0)
        {
          value = value.substring(0, pos) + parentDN +
                  value.substring(pos + 10);
          needReprocess = true;
        }

        // If the value contains "<_parentdn>", then replace that with the DN of
        // the parent entry, but with underscores instead of commas.  This is an
        // undocumented feature for use with identity server.
        if ((pos = value.indexOf("<_parentdn>")) >= 0)
        {
          value = value.substring(0, pos) + parentDN.replace(',', '_') +
                  value.substring(pos + 11);
          needReprocess = true;
        }

        // If the value contains "<parent:attr>" then replace that with the
        // value of the specified attribute from the parent entry if that is
        // available or an empty string if not.
        if ((pos = value.indexOf("<parent:")) >= 0)
        {
          int    closePos = value.indexOf('>', pos);
          String attrName = value.substring(pos+8, closePos);
          String attrValue;
          if (parentAttributes == null)
          {
            attrValue = "";
          }
          else
          {
            attrValue = parentAttributes.getValue(attrName);
          }

          value = value.substring(0, pos) + attrValue +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<ancestordn:depth>", then replace that with
        // the DN of the specified ancestor entry (e.g., if "depth" is 1, then
        // the result will be the entry's parent, 2 will be the grandparent,
        // etc.).  Note that if the entry does not have an ancestor with the
        // specified depth, then this token will be replaced with an empty
        // string.
        if ((pos = value.indexOf("<ancestordn:")) >= 0)
        {
          int closePos = value.indexOf('>', pos);
          int depth = Integer.parseInt(value.substring(pos+12, closePos));

          String ancestordn = "";
          if (depth == 1)
          {
            ancestordn = parentDN;
          }
          else
          {
            int startPos = 0;
            for (int j=1; ((startPos >= 0) && (j < depth)); j++)
            {
              startPos = parentDN.indexOf(',', startPos+1);
            }
            if (startPos >= 0)
            {
              ancestordn = parentDN.substring(startPos+1);
            }
          }

          value = value.substring(0, pos) + ancestordn +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<_ancestordn:depth>", then replace that with
        // the DN of the specified ancestor entry, but with underscores instead
        // of commas in the ancestor entry.
        if ((pos = value.indexOf("<_ancestordn:")) >= 0)
        {
          int closePos = value.indexOf('>', pos);
          int depth = Integer.parseInt(value.substring(pos+13, closePos));

          String ancestordn = "";
          int startPos = 0;
          for (int j=1; ((startPos >= 0) && (j < depth)); j++)
          {
            startPos = parentDN.indexOf(',', startPos);
          }
          if (startPos > 0)
          {
            ancestordn = parentDN.substring(startPos+1).replace(',', '_');
          }

          value = value.substring(0, pos) + ancestordn +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<csvfield:number>", then replace that with the
        // corresponding field from the CSV data.
        if ((pos = value.indexOf("<csvfield:")) >= 0)
        {
          if (csvFile == null)
          {
            System.err.println("ERROR:  CSV field referenced but no CSV file " +
                               "provided (use -c)");
            csvEndReached = true;
            return;
          }

          int    closePos = value.indexOf('>', pos);
          int    fieldNum = Integer.parseInt(value.substring(pos+10, closePos));
          String fieldData;
          if (fieldNum >= csvData.length)
          {
            if (debugMode)
            {
              System.err.println("Invalid field number " + fieldNum +
                                 " -- only " + csvData.length +
                                 " fields on CSV data line");
            }

            fieldData = "";
          }
          else
          {
            fieldData = csvData[fieldNum];
          }

          value = value.substring(0, pos) + fieldData +
                  value.substring(closePos+1);
        }

        // If the value contains "<exec:cmd,arg1,arg2,...,argn>" then replace
        // that with the output of that command.  Note that this is a real
        // performance killer, so it should be used only if necessary.  Also,
        // any end of line characters included in the output will be stripped
        // so if there are multiple lines of output, then they will be run
        // together.
        if ((pos = value.indexOf("<exec:")) >= 0)
        {
          int closePos = value.indexOf('>', pos);
          String cmdStr = value.substring(pos+6, closePos);
          StringTokenizer tokenizer = new StringTokenizer(cmdStr, ",");
          ArrayList<String> cmdList = new ArrayList<String>();
          while (tokenizer.hasMoreTokens())
          {
            cmdList.add(tokenizer.nextToken());
          }
          String[] cmdArray = new String[cmdList.size()];
          for (int j=0; j < cmdArray.length; j++)
          {
            cmdArray[j] = cmdList.get(j);
          }

          try
          {
            Process p = Runtime.getRuntime().exec(cmdArray);
            BufferedInputStream bis =
                 new BufferedInputStream(p.getInputStream());
            StringBuilder buffer = new StringBuilder(80);
            byte[] buf = new byte[1024];
            int readLength = 0;
            while ((readLength = bis.read(buf)) > 0)
            {
              for (int j=0; j < readLength; j++)
              {
                if ((buf[j] != '\r') && (buf[j] != '\n'))
                {
                  buffer.append((char) buf[j]);
                }
              }
            }

            bis.close();

            value = value.substring(0, pos) + buffer.toString() +
                    value.substring(closePos+1);
            needReprocess = true;
          }
          catch (IOException ioe)
          {
            if (debugMode)
            {
              ioe.printStackTrace();
            }

            System.err.println("Error executing command " + cmdArray[0] +
                               ":  " + ioe);
          }
        }

        // If the value contains "<random:chars:characters:length>" then
        // generate a random string of length characters from the provided
        // character set.
        if ((pos = value.indexOf("<random:chars:")) >= 0)
        {
          // Get the set of characters to use in the resulting value.
          int colonPos = value.indexOf(':', pos+14);
          int closePos = value.indexOf('>', colonPos+1);
          String charSet = value.substring(pos+14, colonPos);

          // See if there is an additional colon followed by a number.  If so,
          // then the length will be a random number between the two.
          int count;
          int colonPos2 = value.indexOf(':', colonPos+1);
          if ((colonPos2 > 0) && (colonPos2 < closePos))
          {
            int minValue = Integer.parseInt(value.substring(colonPos+1,
                                                            colonPos2));
            int maxValue = Integer.parseInt(value.substring(colonPos2+1,
                                                            closePos));
            int span = maxValue - minValue + 1;
            count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
          }
          else
          {
            count = Integer.parseInt(value.substring(colonPos+1, closePos));
          }

          StringBuilder buffer = new StringBuilder(value.length() + count);
          buffer.append(value.substring(0, pos));
          generateRandomValue(charSet.toCharArray(), count, buffer);
          buffer.append(value.substring(closePos+1));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<random:alpha:num>" then generate a random
        // alphabetic value and use it.
        if ((pos = value.indexOf("<random:alpha:")) >= 0)
        {
          // See if there is an additional colon followed by a number.  If so,
          // then the length will be a random number between the two.
          int count;
          int closePos = value.indexOf('>', pos+14);
          int colonPos = value.indexOf(':', pos+14);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            int minValue = Integer.parseInt(value.substring(pos+14, colonPos));
            int maxValue = Integer.parseInt(value.substring(colonPos+1,
                                                            closePos));
            int span = maxValue - minValue + 1;
            count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
           }
          else
          {
            count = Integer.parseInt(value.substring(pos+14, closePos));
          }

          // Generate the new value.
          StringBuilder buffer = new StringBuilder(value.length() + count);
          buffer.append(value.substring(0, pos));
          generateRandomValue(ALPHA_CHARS, count, buffer);
          buffer.append(value.substring(closePos+1));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<random:numeric:num>" then generate a random
        // numeric value and use it.  This can also take the form
        // "<random:numeric:min:max>" or "<random:numeric:min:max:length>".
        if ((pos = value.indexOf("<random:numeric:")) >= 0)
        {
          int closePos = value.indexOf('>', pos);

          // See if there is an extra colon.  If so, then generate a random
          // number between x and y.  Otherwise, generate a random number with
          // the specified number of digits.
          int extraColonPos = value.indexOf(':', pos+16);
          if ((extraColonPos > 0) && (extraColonPos < closePos))
          {
            // See if there is one more colon separating the max from the
            // length.  If so, then get it and create a padded value of at least
            // length digits.  If not, then just generate the random value.
            int extraColonPos2 = value.indexOf(':', extraColonPos+1);
            if ((extraColonPos2 > 0) && (extraColonPos2 < closePos))
            {
              String lowerBoundStr = value.substring(pos+16, extraColonPos);
              String upperBoundStr = value.substring(extraColonPos+1,
                                                     extraColonPos2);
              String lengthStr = value.substring(extraColonPos2+1, closePos);
              int lowerBound = Integer.parseInt(lowerBoundStr);
              int upperBound = Integer.parseInt(upperBoundStr);
              int length = Integer.parseInt(lengthStr);
              int span = (upperBound - lowerBound + 1);
              int randomValue = (random.nextInt() & 0x7FFFFFFF) % span +
                                lowerBound;
              String valueStr = String.valueOf(randomValue);
              while (valueStr.length() < length)
              {
                valueStr = '0' + valueStr;
              }
              value = value.substring(0, pos) + valueStr +
                      value.substring(closePos+1);
            }
            else
            {
              String lowerBoundStr = value.substring(pos+16, extraColonPos);
              String upperBoundStr = value.substring(extraColonPos+1, closePos);
              int lowerBound = Integer.parseInt(lowerBoundStr);
              int upperBound = Integer.parseInt(upperBoundStr);
              int span = (upperBound - lowerBound + 1);
              int randomValue = (random.nextInt() & 0x7FFFFFFF) % span +
                                lowerBound;
              value = value.substring(0, pos) + randomValue +
                      value.substring(closePos+1);
            }
          }
          else
          {
            // Get the number of characters to include in the value
            int numPos = pos + 16;
            int count = Integer.parseInt(value.substring(numPos, closePos));
            StringBuilder buffer = new StringBuilder(value.length() + count);
            buffer.append(value.substring(0, pos));
            generateRandomValue(NUMERIC_CHARS, count, buffer);
            buffer.append(value.substring(closePos+1));
            value = buffer.toString();
          }

          needReprocess = true;
        }

        // If the value contains "<random:alphanumeric:num>" then generate a
        // random alphanumeric value and use it
        if ((pos = value.indexOf("<random:alphanumeric:")) >= 0)
        {
          // See if there is an additional colon followed by a number.  If so,
          // then the length will be a random number between the two.
          int count;
          int closePos = value.indexOf('>', pos+21);
          int colonPos = value.indexOf(':', pos+21);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            int minValue = Integer.parseInt(value.substring(pos+21, colonPos));
            int maxValue = Integer.parseInt(value.substring(colonPos+1,
                                                            closePos));
            int span = maxValue - minValue + 1;
            count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
           }
          else
          {
            count = Integer.parseInt(value.substring(pos+21, closePos));
          }

          // Generate the new value.
          StringBuilder buffer = new StringBuilder(value.length()+count);
          buffer.append(value.substring(0, pos));
          generateRandomValue(ALPHANUMERIC_CHARS, count, buffer);
          buffer.append(value.substring(closePos+1));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<random:hex:num>" then generate a random
        // hexadecimal value and use it
        if ((pos = value.indexOf("<random:hex:")) >= 0)
        {
          // See if there is an additional colon followed by a number.  If so,
          // then the length will be a random number between the two.
          int count;
          int closePos = value.indexOf('>', pos+12);
          int colonPos = value.indexOf(':', pos+12);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            int minValue = Integer.parseInt(value.substring(pos+12, colonPos));
            int maxValue = Integer.parseInt(value.substring(colonPos+1,
                                                            closePos));
            int span = maxValue - minValue + 1;
            count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
           }
          else
          {
            count = Integer.parseInt(value.substring(pos+12, closePos));
          }

          // Generate the new value.
          StringBuilder buffer = new StringBuilder(value.length()+count);
          buffer.append(value.substring(0, pos));
          generateRandomValue(HEX_CHARS, count, buffer);
          buffer.append(value.substring(closePos+1));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<random:base64:num>" then generate a random
        // base64 value and use it
        if ((pos = value.indexOf("<random:base64:")) >= 0)
        {
          // See if there is an additional colon followed by a number.  If so,
          // then the length will be a random number between the two.
          int count;
          int closePos = value.indexOf('>', pos+15);
          int colonPos = value.indexOf(':', pos+15);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            int minValue = Integer.parseInt(value.substring(pos+15, colonPos));
            int maxValue = Integer.parseInt(value.substring(colonPos+1,
                                                            closePos));
            int span = maxValue - minValue + 1;
            count = (random.nextInt() & 0x7FFFFFFF) % span + minValue;
           }
          else
          {
            count = Integer.parseInt(value.substring(pos+15, closePos));
          }

          // Generate the new value.
          StringBuilder buffer = new StringBuilder(value.length()+count);
          buffer.append(value.substring(0, pos));
          generateRandomValue(BASE64_CHARS, count, buffer);
          switch (count % 4)
          {
            case 1:  buffer.append("===");
                     break;
            case 2:  buffer.append("==");
                     break;
            case 3:  buffer.append('=');
                     break;
          }
          buffer.append(value.substring(closePos+1));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<random:telephone>" then generate a random
        // telephone number and use it
        if ((pos = value.indexOf("<random:telephone>")) >= 0)
        {
          // Get the number of characters to include in the value
          StringBuilder buffer = new StringBuilder(value.length()+10);
          buffer.append(value.substring(0, pos));
          generateRandomValue(NUMERIC_CHARS, 3, buffer);
          buffer.append('-');
          generateRandomValue(NUMERIC_CHARS, 3, buffer);
          buffer.append('-');
          generateRandomValue(NUMERIC_CHARS, 4, buffer);
          buffer.append(value.substring(pos+18));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<random:month>" then choose a random month
        // name.  Optionally, look for "<random:month:length>" and use at most
        // length characters of the month name.
        if ((pos = value.indexOf("<random:month")) >= 0)
        {
          int closePos = value.indexOf('>', pos+13);
          String monthStr = MONTH_NAMES[(random.nextInt() & 0x7FFFFFFF) % 12];

          // See if there is another colon that specifies the length.
          int colonPos = value.indexOf(':', pos+13);
          if ((colonPos > 0) && (colonPos < closePos))
          {
            String lengthStr = value.substring(colonPos+1, closePos);
            int length = Integer.parseInt(lengthStr);
            if (monthStr.length() > length)
            {
              monthStr = monthStr.substring(0, length);
            }
          }

          value = value.substring(0, pos) + monthStr +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<guid>" then generate a GUID and use it
        if ((pos = value.indexOf("<guid>")) >= 0)
        {
          // Get the number of characters to include in the value
          StringBuilder buffer = new StringBuilder(value.length()+32);
          buffer.append(value.substring(0, pos));
          generateGUID(buffer);
          buffer.append(value.substring(pos+6));
          value = buffer.toString();
          needReprocess = true;
        }

        // If the value contains "<sequential>" then use the next sequential
        // value for that attribute
        if ((pos = value.indexOf("<sequential")) >= 0)
        {
          int closePos = value.indexOf('>', pos);

          // Get the sequential counter for that attribute
          Counter c = template.getCounter(attrComponents[i][3]);
          if (c == null)
          {
            // If a starting point was specified, then use it.  If not, then
            // use 0.
            int colonPos = value.indexOf(':', pos);
            int startingValue = 0;
            if ((colonPos > pos) && (colonPos < closePos))
            {
              startingValue = Integer.parseInt(value.substring(colonPos+1,
                                                               closePos));
            }
            c = new Counter(startingValue);
            template.addCounter(attrComponents[i][3], c);
          }

          value = value.substring(0, pos) + c.getNext() +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<list:" then treat it as a value list and get
        // the value from that
        if ((pos = value.indexOf("<list:")) >= 0)
        {
          int closePos = value.indexOf('>', pos);

          // See if a value list already exists for the specified attribute.  If
          // not, then create one
          ValueList vl = valueLists.get(attrComponents[i][3]);
          if (vl == null)
          {
            vl = new ValueList(randomSeed);
            String listVals = value.substring(pos+6, closePos);
            StringTokenizer tokenizer = new StringTokenizer(listVals, ",");
            while (tokenizer.hasMoreElements())
            {
              String listValue = tokenizer.nextToken();
              int colonPos = listValue.indexOf(':');
              if (colonPos > 0)
              {
                String val = listValue.substring(0, colonPos);
                int weight = Integer.parseInt(listValue.substring(colonPos+1));
                vl.addValue(val, weight);
              }
              else
              {
                vl.addValue(listValue);
              }
            }
            vl.completeInitialization();
            valueLists.put(attrComponents[i][3], vl);
          }

          // Get the list value and use it
          value = value.substring(0, pos) + vl.nextValue() +
                  value.substring(closePos+1);
          needReprocess = true;
        }

        // If the value contains "<file:" then treat it as a file containing
        // potential values.
        if ((pos = value.indexOf("<file:")) >= 0)
        {
          int    closePos = value.indexOf('>', pos);
          String filename = value.substring(pos+6, closePos);

          // See if a value list already exists for the specified attribute.  If
          // not, then create one
          ValueList vl = fileLists.get(filename);
          if (vl == null)
          {
            vl = new ValueList(randomSeed);
            BufferedReader fileReader = null;

            File f = getFileForName(filename);
            try
            {
              fileReader = new BufferedReader(new FileReader(f));
            }
            catch (IOException ioe)
            {
              if (debugMode)
              {
                ioe.printStackTrace();
              }

              System.err.println("Error:  Unable to open file" +
                   f.getAbsolutePath() + " to get value list -- aborting");
              try
              {
                ldifWriter.flush();
                ldifWriter.close();
              } catch (IOException ioe2) {}
              System.exit(1);
            }

            try
            {
              while (fileReader.ready())
              {
                vl.addValue(fileReader.readLine());

              }
            }
            catch (IOException ioe)
            {
              if (debugMode)
              {
                ioe.printStackTrace();
              }

              System.err.println("Error reading file" + f.getAbsolutePath() +
                                 " to get value list -- " + ioe +
                                 "-- aborting");
              try
              {
                ldifWriter.flush();
                ldifWriter.close();
              } catch (IOException ioe2) {}
              System.exit(1);
            }

            try
            {
              fileReader.close();
            } catch (IOException ioe) {}
            vl.completeInitialization();
            fileLists.put(filename, vl);
          }

          value = value.substring(0, pos) + vl.nextValue() +
                  value.substring(closePos+1);
          needReprocess = true;
        }
      }

      needReprocess = true;
      while (needReprocess && ((pos = value.indexOf('{')) >= 0))
      {
        // If there is a backslash in front of the curly brace, then we don't
        // want to consider it an attribute name.
        if ((pos > 0) && (value.charAt(pos-1) == '\\'))
        {
          boolean keepGoing  = true;
          boolean nonEscaped = false;
          while (keepGoing)
          {
            value = value.substring(0, pos-1) + value.substring(pos);

            pos = value.indexOf('{', pos);
            if (pos < 0)
            {
              keepGoing = false;
            }
            else if (value.charAt(pos-1) != '\\')
            {
              nonEscaped = true;
            }
          }

          if (! nonEscaped)
          {
            break;
          }
        }


        // If the value has "{attr}", then try to replace it with the value of
        // that attribute.  Note that attribute replacement will only work
        // properly for attributes that are defined in the template before the
        // attribute that attempts to use its value.  If the specified attribute
        // has more than one value, then the first value found will be used.
        int closePos = value.indexOf('}', pos);
        if (closePos > 0)
        {
          int colonPos = value.indexOf(':', pos);
          int substringChars = -1;
          String attrName = null;
          if ((colonPos > 0) && (colonPos < closePos))
          {
            attrName = value.substring(pos+1, colonPos).toLowerCase();
            String numStr = value.substring(colonPos+1, closePos);
            try
            {
              substringChars = Integer.parseInt(numStr);
            }
            catch (NumberFormatException nfe)
            {
              if (debugMode)
              {
                nfe.printStackTrace();
              }
            }
          }
          else
          {
            attrName = value.substring(pos+1, closePos).toLowerCase();
          }

          String attrValue = attrList.getValueForLowerName(attrName);
          if ((colonPos > 0) && (colonPos < closePos) && (substringChars > 0) &&
              (attrValue.length() > substringChars))
          {
            attrValue = attrValue.substring(0, substringChars);
          }

          StringBuilder buffer = new StringBuilder(75);
          buffer.append(value.substring(0, pos)).append(attrValue).
                           append(value.substring(closePos+1));
          value = buffer.toString();
          needReprocess = true;
        }
      }

      if ((pos = value.indexOf("<base64:")) >= 0)
      {
        String charset;
        String valueToEncode;

        int closePos = value.indexOf('>', pos+8);
        int colonPos = value.indexOf(':', pos+8);
        if ((closePos > 0) && (colonPos > 0) && (colonPos < closePos))
        {
          charset       = value.substring(pos+8, colonPos);
          valueToEncode = value.substring(colonPos+1, closePos);
        }
        else
        {
          charset       = "UTF-8";
          valueToEncode = value.substring(pos+8, closePos);
        }

        try
        {
          String encodedStr = Base64.encode(valueToEncode.getBytes(charset));
          value = value.substring(0, pos) + encodedStr +
                  value.substring(closePos+1);
        }
        catch (UnsupportedEncodingException uee)
        {
          if (debugMode)
          {
            uee.printStackTrace();
          }

          System.out.println("Could not base64-encode the value \"" +
                             valueToEncode + "\" -- unsupported encoding " +
                             "type " + charset);
        }
      }

      if ((pos = value.indexOf("<custom:")) >= 0)
      {
        int closePos = value.indexOf('>', pos+8);

        String   className;
        String[] tagArgs;
        int colonPos = value.indexOf(':', pos+8);
        if (colonPos > 0)
        {
          className = value.substring(pos+8, colonPos);
          String argString = value.substring(colonPos+1, closePos);
          ArrayList<String> argList = new ArrayList<String>();
          StringTokenizer tokenizer = new StringTokenizer(argString, ",");
          while (tokenizer.hasMoreTokens())
          {
            argList.add(tokenizer.nextToken());
          }
          tagArgs = new String[argList.size()];
          argList.toArray(tagArgs);
        }
        else
        {
          className = value.substring(pos+8, closePos);
          tagArgs = new String[0];
        }

        String    tagName   = attrComponents[i][3] + ' ' + className;
        CustomTag customTag = template.getCustomTag(tagName);
        if (customTag == null)
        {
          try
          {
            Class tagClass = Constants.classForName(className);
            customTag = (CustomTag) tagClass.newInstance();

            customTag.initialize();
            template.addCustomTag(tagName, customTag);
          }
          catch (Exception e)
          {
            if (debugMode)
            {
              e.printStackTrace();
            }

            System.out.println("Could not create custom tag " + className +
                               ":  " + e);
          }
        }

        value = value.substring(0, pos) + customTag.generateOutput(tagArgs) +
                value.substring(closePos+1);
      }

      if ((pos = value.indexOf("<loop:")) >= 0)
      {
        int closePos = value.indexOf('>', pos+6);
        int colonPos = value.indexOf(':', pos+6);
        if ((closePos > 0) && (colonPos > 0) && (colonPos < closePos))
        {
          int lowerBound = Integer.parseInt(value.substring(pos+6, colonPos));
          int upperBound = Integer.parseInt(value.substring(colonPos+1,
                                                            closePos));
          int span = (upperBound - lowerBound + 1);

          for (int j=0; j < span; j++)
          {
            String copy = value;
            while ((pos = copy.indexOf("<loop:")) >= 0)
            {
              closePos = copy.indexOf('>', pos+6);
              colonPos = copy.indexOf(':', pos+6);
              lowerBound = Integer.parseInt(copy.substring(pos+6, colonPos));
              copy = copy.substring(0, pos) + (lowerBound + j) +
                     copy.substring(closePos+1);
            }

            attrList.addAttribute(attrComponents[i][0], attrComponents[i][3],
                                  attrComponents[i][1], copy);
          }
        }
      }
      else
      {
        attrList.addAttribute(attrComponents[i][0], attrComponents[i][3],
                              attrComponents[i][1], value);
      }


      for (int j=0; j < rdnAttrs.length; j++)
      {
        if ((rdnValues[j] == null) && (lowerAttrName.equals(lowerRDNAttrs[j])))
        {
          rdnValues[j] = value;
        }
      }

      if ((loginIDValue == null) && (lowerAttrName.equals(loginIDAttr)))
      {
        loginIDValue = value;
      }

      if ((passwordValue == null) && (lowerAttrName.equals("userpassword")))
      {
        passwordValue = value;
      }

      if (generateFilterList)
      {
        UniqueSortedList usl = filterListHash.get(lowerAttrName);
        if (usl != null)
        {
          usl.addString(value.toLowerCase());
        }
      }
    }


    // All of the values have been processed.  Now try to calculate the DN for
    // the entry.
    for (int i=0; i < rdnValues.length; i++)
    {
      if (rdnValues[i] == null)
      {
        System.out.println("Could not calculate the DN for the entry (no " +
                           "value for RDN attribute " + rdnAttrs[i] +
                           " found)");
        System.out.println(attrList.toString());
        return;
      }
    }


    // Create the DN for the entry
    StringBuilder dnBuffer = new StringBuilder(75);

    String separator = "";
    for (int i=0; i < rdnAttrs.length; i++)
    {
      dnBuffer.append(separator);
      dnBuffer.append(rdnAttrs[i]);
      dnBuffer.append('=');
      dnBuffer.append(rdnValues[i]);

      separator = "+";
    }

    dnBuffer.append(',');
    dnBuffer.append(parentDN);
    String entryDN = dnBuffer.toString();


    // Write the completed entry to the LDIF file.
    writeEntry(entryDN, "dn: " + entryDN + EOL + attrList.toString(),
               passwordValue, loginIDValue);


    // If this entry is to have subordinate entries, then create them
    for (int i=0; i < subtemplateNames.length; i++)
    {
      Template subtemplate =
           templateHash.get(subtemplateNames[i].toLowerCase());
      if (subtemplate != null)
      {
        subtemplate.resetCounters();
        subtemplate.reinitializeCustomTags();
        int numSubEntries = subtemplateCounts[i];
        if (maxPerTemplate >= 0)
        {
          numSubEntries = Math.min(maxPerTemplate, numSubEntries);
        }
        for (int j=0; ((! csvEndReached) && (j < numSubEntries)); j++)
        {
          try
          {
            createTemplateEntry(entryDN, attrList, subtemplate);
          }
          catch (Exception e)
          {
            if (debugMode)
            {
              e.printStackTrace();
            }
            System.err.println("ERROR creating entry below parent  " + entryDN +
                               ":  " + e);
            System.err.println("The entry was not written to the LDIF file.");
          }
        }
      }
      else
      {
        System.out.println("ERROR:  Subordinate template " +
                           subtemplateNames[i] + " requested by template " +
                           template.getName() + " is not defined.  Skipping " +
                           subtemplateCounts[i] + " subordinate entries.");
      }
    }
  }



  /**
   * Generates a random value of the indicated length from the specified
   * character set.
   *
   * @param  charSet  The character set from which the random characters are to
   *                  be taken.
   * @param  length   The length of the random value to generate.
   * @param  buffer   The string buffer into which the random characters are to
   *                  be written.
   */
  public void generateRandomValue(char[] charSet, int length,
                                  StringBuilder buffer)
  {
    // A performance optimization in an attempt to avoid continually
    // re-allocating character arrays.  If the requested array is less than
    // 5000 characters long, then use the space we've already allocated for this
    // purpose.  If it is longer than 5000 characters, then we'll take the
    // hit of re-allocating the memory.
    if (length <= 5000)
    {
      for (int i=0; i < length; i++)
      {
        chars5000[i] = charSet[(random.nextInt() & 0x7FFFFFFF) %
                               charSet.length];
      }

      buffer.append(chars5000, 0, length);
    }
    else
    {
      char[] retArray = new char[length];

      for (int i=0; i < length; i++)
      {
        retArray[i] = charSet[(random.nextInt() & 0x7FFFFFFF) % charSet.length];
      }

      buffer.append(retArray);
    }
  }



  /**
   * Generates a globally-unique identifier.  Technically speaking, it's not
   * guaranteed to be globally unique, but this should be good enough for most
   * purposes.
   *
   * @param  buffer  The string buffer into which the GUID should be written.
   */
  public void generateGUID(StringBuilder buffer)
  {
    StringBuilder buf2 = new StringBuilder(20);

    if (randomSeed < 0)
    {
      buf2.append(Long.toHexString(System.currentTimeMillis()));
    }
    else
    {
      buf2.append(Long.toHexString(random.nextLong()));
    }

    generateRandomValue(HEX_CHARS, 20-buf2.length(), buf2);
    buffer.append(buf2.substring(0, 8));
    buffer.append('-');
    buffer.append(buf2.substring(8, 12));
    buffer.append('-');
    buffer.append(buf2.substring(12, 16));
    buffer.append('-');
    buffer.append(buf2.substring(16));
    buffer.append('-');
    buffer.append(guidBase);
  }



  /**
   * Creates a "wrapped" version of the entry, where long lines are wrapped at
   * 75 columns.
   *
   * @param  entry  The entry to be wrapped.
   *
   * @return  The wrapped entry.
   */
  public String wrapEntry(String entry)
  {
    // Using a string buffer here instead of string concatenation gives a huge
    // performance boost.  Even things like " " + line are slower than
    // returnStr.append(" "); returnStr.append(line);
    StringBuilder buffer = new StringBuilder(1000);

    StringTokenizer tokenizer = new StringTokenizer(entry, EOL);
    while (tokenizer.hasMoreTokens())
    {
      String line = tokenizer.nextToken();
      if (line.length() > 75)
      {
        buffer.append(line.substring(0, 76));
        buffer.append(EOL);
        line = line.substring(76);

        while (line.length() > 74)
        {
          buffer.append(' ');
          buffer.append(line.substring(0, 75));
          buffer.append(EOL);
          line = line.substring(75);
        }
        if (line.length() > 0)
        {
          buffer.append(' ');
          buffer.append(line);
          buffer.append(EOL);
        }
      }
      else
      {
        buffer.append(line);
        buffer.append(EOL);
      }
    }

    return buffer.toString() + EOL;
  }



  /**
   * Retrieves the next values that should be used for the first and last names
   * (the first element will be the first name, the second element will be the
   * last name).  The combination of the first and last name is guaranteed to be
   * unique for this LDIF file and therefore could be used for the uid.
   *
   * @return  The next values that should be used for the first and last names.
   */
  public String[] nextFirstAndLastNames()
  {
    // Start with the plain first and last names at the appropriate positions
    // in the name lists
    String first = firstNames[firstNameIndex];
    String last  = lastNames[lastNameIndex];


    // If we have exhausted all possible unique combinations of first and last
    // names, then we need to add a counter to the end of the last name to
    // ensure that we can still have unique combinations
    if (nameUniquenessCounter > 1)
    {
      last += nameUniquenessCounter;
    }


    // Create the array to return
    String[] names = new String[] { first, last };


    // Now find the indexes of the next values to retrieve.  Most of the time
    // (if neither index is at the end of the list), they will both just be
    // incremented by one.  Check for that condition first.
    if ((firstNameIndex+1 < numFirstNames) &&
        (lastNameIndex+1 < numLastNames))
    {
      firstNameIndex++;
      lastNameIndex++;
    }
    else
    {
      if ((firstNameIndex+1 >= numFirstNames) &&
          (numFirstNames >= numLastNames))
      {
        // We're at the end of the first names list and the first names list is
        // larger than the last names list.  Set the first name list counter to
        // zero, increment the name loop counter, and set the last name index
        // value to that.  If the resulting last name index is greater than or
        // equal to the length, then we've been through all possible unique
        // combinations at least once so increment the name uniqueness counter.
        firstNameIndex = 0;
        nameLoopCounter++;
        lastNameIndex = nameLoopCounter;

        if (lastNameIndex >= numLastNames)
        {
          lastNameIndex = 0;
          nameUniquenessCounter++;
        }
      }
      else if ((lastNameIndex+1 >= numLastNames) &&
               (numLastNames > numFirstNames))
      {
        // This is the same condition as above, except we are at the end of the
        // last name list and that list is bigger.  So do exactly the same thing
        // but interchange the first and last name references
        lastNameIndex = 0;
        nameLoopCounter++;
        firstNameIndex = nameLoopCounter;

        if (firstNameIndex >= numFirstNames)
        {
          firstNameIndex = 0;
          nameUniquenessCounter++;
        }
      }
      else if (firstNameIndex+1 >= numFirstNames)
      {
        // We are at the end of the first name list and the first name list is
        // smaller than the last name list.  Just start it over at zero and
        // increment the last name index.
        firstNameIndex = 0;
        lastNameIndex++;
      }
      else
      {
        // We are at the end of the last name list and the last name list is
        // smaller than the first name list.  Just start it over at zero and
        // increment the first name index
        lastNameIndex = 0;
        firstNameIndex++;
      }
    }

    return names;
  }



  /**
   * Reads the next line from the CSV file and splits it into separate fields.
   *
   * @return  An array containing the fields read from the next line of the CSV
   *          file, or <CODE>null</CODE> if there is no more data in the file.
   */
  public String[] nextCSVLine()
  {
    try
    {
      String line = csvReader.readLine();
      if (line == null)
      {
        return null;
      }

      ArrayList<String> elementList = new ArrayList<String>();
      if (csvDelimiter == null)
      {
        // This is actually a CSV file, so parse it accordingly.
        boolean keepReading    = true;
        int     startPos       = 0;
        int     searchStartPos = 0;
        while (keepReading)
        {
          int commaPos = line.indexOf(',', searchStartPos);
          if (commaPos < 0)
          {
            // No more delimiters on the line, so the remainder is the last
            // element.
            String substring = line.substring(startPos).trim();
            int lastPos = substring.length() - 1;
            if ((substring.charAt(0) == '"') &&
                (substring.charAt(lastPos) == '"'))
            {
              substring = substring.substring(1, lastPos);
            }

            elementList.add(substring);
            keepReading = false;
          }
          else
          {
            // We have found a comma.  See if it is one that should be
            // considered a delimiter.
            if ((commaPos > 0) && (line.charAt(commaPos-1) == '\\'))
            {
              // This is an escaped comma, so it shouldn't be a delimiter.
              line = line.substring(0, commaPos-1) + line.substring(commaPos);
              searchStartPos = commaPos;
              continue;
            }

            String substring = line.substring(startPos, commaPos);
            if (substring.indexOf('"') >= 0)
            {
              // There is at least one quotation mark on the line.  Determine
              // whether the comma we found was inside quoted section.
              int numQuotes = 0;
              for (int i=0; i < substring.length(); i++)
              {
                if (substring.charAt(i) == '"')
                {
                  numQuotes++;
                }

                if ((numQuotes % 2) == 0)
                {
                  // There are an even number of quotation marks, so we can
                  // assume we're not in the middle of a quoted section.
                  substring   = substring.trim();
                  int lastPos = substring.length() - 1;

                  // If the substring is surrounded by quotes, then strip them
                  // off.
                  if ((substring.charAt(0) == '"') &&
                      (substring.charAt(lastPos) == '"'))
                  {
                    substring = substring.substring(1, lastPos);
                  }

                  elementList.add(substring);
                  startPos       = commaPos+1;
                  searchStartPos = startPos;
                }
                else
                {
                  // There are an odd number of quotes, so we can assume we are
                  // in a quoted section.
                  searchStartPos = commaPos+1;
                  continue;
                }
              }
            }
            else
            {
              // No quotation marks in the substring, so we have an element.
              elementList.add(line.substring(startPos, commaPos).trim());
              startPos       = commaPos + 1;
              searchStartPos = startPos;
            }
          }
        }
      }
      else
      {
        // This is a delimited text file, so parse it accordingly.
        boolean keepReading = true;
        int     startPos    = 0;
        while (keepReading)
        {
          int delimiterPos = line.indexOf(csvDelimiter, startPos);
          if (delimiterPos < 0)
          {
            // No more delimiters on the line, so the remainder is the last
            // element.
            elementList.add(line.substring(startPos));
            keepReading = false;
          }
          else
          {
            // There is a delimiter, so grab the element and move forward in the
            // string.
            elementList.add(line.substring(startPos, delimiterPos));
            startPos = delimiterPos + csvDelimiter.length();
          }
        }
      }

      String[] elements = new String[elementList.size()];
      elementList.toArray(elements);
      return elements;
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Error reading CSV file \"" + csvFile + "\" -- " +
                         ioe);
      return null;
    }
  }



  /**
   * Retrieves the template definition with the specified name.  If the
   * requested template could not be found, <CODE>null</CODE> is returned.  If
   * the requested template is a subordinate of some other template, then the
   * returned template will have the complete set of attributes from any parent
   * templates that it might have.
   *
   * @param  templateName  The name of the template to retrieve.
   *
   * @return  The requested template, or <CODE>null</CODE> if the template could
   *          not be found.
   */
  public Template getTemplate(String templateName)
  {
    Template t = templateHash.get(templateName.toLowerCase());
    if (t == null)
    {
      return null;
    }

    if ((t.getParentTemplateName() == null) ||
        (t.getParentTemplateName().length() == 0))
    {
      return t;
    }
    else
    {
      Template pt = getTemplate(t.getParentTemplateName());
      if (pt == null)
      {
        return t;
      }
      else
      {
        String[][] parentComponents = pt.getAttributeComponents();
        String[][] childComponents = t.getAttributeComponents();
        String[][] newComponents =
             new String[parentComponents.length + childComponents.length][];
        Template nt = (Template) t.clone();
        nt.parentTemplateName = null;
        for (int i=0; i < parentComponents.length; i++)
        {
          newComponents[i] = parentComponents[i];
        }
        for (int i=0; i < childComponents.length; i++)
        {
          newComponents[i+parentComponents.length] = childComponents[i];
        }
        nt.attrComponents = newComponents;
        return nt;
      }
    }
  }



  /**
   * Writes the specified entry into the LDIF file.  If the entry needs to be
   * wrapped, then that will be done.  This will also keep track of the total
   * number of entries written so that progress details will be printed as the
   * entries are written.
   *
   * @param  entryDN        The DN of the entry to be written.
   * @param  entry          The entry to be written into the LDIF file.
   * @param  passwordValue  The password from the entry, if present.
   * @param  loginIDValue   The login ID from the entry, if present.
   */
  public void writeEntry(String entryDN, String entry, String passwordValue,
                         String loginIDValue)
  {
    if (dnFile != null)
    {
      try
      {
        dnWriter.write(entryDN + EOL);
      }
      catch (IOException ioe)
      {
        if (debugMode)
        {
          ioe.printStackTrace();
        }

        System.err.println("Error writing to DN file:  " + ioe);
      }
    }

    if ((loginIDValue != null) && (passwordValue != null) &&
        (loginFile != null))
    {
      try
      {
        loginWriter.write(loginIDValue + '\t' + passwordValue + EOL);
      }
      catch (IOException ioe)
      {
        if (debugMode)
        {
          ioe.printStackTrace();
        }

        System.err.println("Error writing to login info file:  " + ioe);
      }
    }

    if ((passwordValue != null) && (bindInfoFile != null))
    {
      try
      {
        bindInfoWriter.write(entryDN + '\t' + passwordValue + EOL);
      }
      catch (IOException ioe)
      {
        if (debugMode)
        {
          ioe.printStackTrace();
        }

        System.err.println("Error writing to bind info file:  " + ioe);
      }
    }

    if (wrapLongLines)
    {
      entry = wrapEntry(entry);
    }

    try
    {
      ldifWriter.write(entry);
    }
    catch (IOException ioe)
    {
      if (debugMode)
      {
        ioe.printStackTrace();
      }

      System.err.println("Error writing entry to LDIF file:  " + ioe);
    }

    entriesWritten++;
    if ((entriesWritten % 1000) == 0)
    {
      System.out.println("Processed " + entriesWritten + " entries");
    }
    if ((maxPerFile > 0) && ((entriesWritten % maxPerFile) == 0))
    {
      try
      {
        ldifWriter.flush();
        ldifWriter.close();

        fileNameCounter++;
        ldifWriter = new BufferedWriter(new FileWriter(ldifFile + '.' +
                                                       fileNameCounter));
      }
      catch (IOException ioe)
      {
        if (debugMode)
        {
          ioe.printStackTrace();
        }

        System.err.println("Error creating a new LDIF file:  " + ioe);
      }
    }
  }



  /**
   * Retrieves the path to the specified file.
   *
   * @param  name  The name of the file to retrieve.
   *
   * @return  The appropriate path to use for the file.
   */
  private File getFileForName(String name)
  {
    File f = new File(name);
    if (f.exists() || (resourceDir == null))
    {
      return f;
    }

    return new File(resourceDir, name);
  }



  /**
   * Displays version information for the MakeLDIF program.
   */
  public void displayVersion()
  {
    System.err.println("MakeLDIF version " + VERSION_STRING);
  }



  /**
   * Displays information about how to use this program.
   */
  public void displayUsage()
  {
    System.err.println(
"Usage:  java MakeLDIF {options}" + EOL +
"        valid {options} include:" + EOL +
"-r {path}        --  The path to the directory containing MakeLDIF " + EOL +
"                     resource files" + EOL +
"-f {filename}    --  The name of the file containing first names" + EOL +
"-l {filename}    --  The name of the file containing last names" + EOL +
"-t {filename}    --  The LDIF template file" + EOL +
"-o {filename}    --  The output file to create" + EOL +
"-c {filename}    --  A CSV file containing data to use in generating" + EOL +
"                     LDIF data" + EOL +
"-C {delimiter}   --  The delimiter to use for the fields in the CSV " + EOL +
"                     file instead of commas" + EOL +
"-I               --  Indicates that the first line of the CSV file" + EOL +
"                     should be ignored as a header line" + EOL +
"-d {filename}    --  Write DNs of created entries to this file" + EOL +
"-b {filename}    --  Write bind information to this file" + EOL +
"                     (format:  dn{tab}password)" + EOL +
"-i {attr}        --  Specifies the login ID attribute (default is uid)" + EOL +
"-L {filename}    --  Write login information to this file" + EOL +
"                     (format:  loginID{tab}password)" + EOL +
"-F {filename}    --  Write generated filters to this file" + EOL +
"-M               --  Indicates that a separate filter file should be" + EOL +
"                     used per index type." + EOL +
"-T {attr:types}  --  Specifies that the specified filter types are to" + EOL +
"                     be generated for the given attribute.  Filter" + EOL +
"                     types may be eq, sub, subInitial, subAny, or " + EOL +
"                     subFinal (ex: \"cn:eq,sub\")." + EOL +
"-n {value}       --  Specifies the number of characters to include in " + EOL +
"                     substring filters (default is 3)." + EOL +
"-N {value}       --  Specifies the minimum number of entries that " + EOL +
"                     should match a filter before it will be written" + EOL +
"                     to the filter file (default is 1)." + EOL +
"-X {value}       --  Specifies the maximum number of entries that " + EOL +
"                     should match a filter for it to be written to the" + EOL +
"                     filter file (default is unlimited)." + EOL +
"-s {value}       --  Specifies a seed to use for the random number" + EOL +
"                     generator (default is time-based)." + EOL +
"-m {value}       --  Specifies the maximum number of entries that" + EOL +
"                     should be written to a single file (default is" + EOL +
"                     unlimited)." + EOL +
"-x {value}       --  Specifies the maximum number of entries for each" + EOL +
"                     template that should be created under each branch" + EOL +
"                     (can be used to validate the configuration)." + EOL +
"-w               --  Wrap long lines" + EOL +
"-S               --  Skip branch entries" + EOL +
"-U               --  Always use UNIX line separators (\\n)" + EOL +
"-D               --  Operate in debug mode (print some additional " + EOL +
"                     information about failures)." + EOL +
"-H               --  Display usage information" + EOL +
"-V               --  Display version information" + EOL
                      );
  }
}

