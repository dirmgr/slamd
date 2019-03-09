/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Alan Field.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson and Alan Field
 */
package com.slamd.scripting;



import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.bsf.BSFManager;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.IntegerParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.LabelParameter;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.StringParameter;
import com.slamd.stat.RealTimeStatReporter;
import com.slamd.stat.StatTracker;



/**
 * This class implements a SLAMD job that executes a Script file to generate
 * load. It implements the standard SLAMD job methods and adds three new methods
 * to allow the job to be referenced from the Script file.
 *
 * @author Alan Field
 */
public class BSFJobClass
       extends JobClass
{
  /**
   * The name of the job parameter that defines the script file to use.
   */
  public static final String SCRIPT_LANGUAGE_PARAMETER_NAME = "script_language";

  /**
   * The name of the job parameter that defines the script file to use.
   */
  public static final String SCRIPT_EXTENSION_PARAMETER_NAME =
       "script_extension";

  /**
   * The name of the job parameter that defines the script file to use.
   */
  public static final String ENGINE_CLASS_PARAMETER_NAME = "engine_class";

  /**
   * The name of the job parameter that defines the script file to use.
   */
  public static final String FILE_PATH_PARAMETER_NAME = "file_path";

  /**
   * The name of the job parameter that allows the user to pass arguments to
   * the script.
   */
  public static final String SCRIPT_ARGUMENTS_PARAMETER_NAME =
       "script_arguments";

  /**
   * The name of the job parameter that defines the number of clients running
   * this job.
   */
  public static final String NUMBER_OF_CLIENTS_PARAMETER_NAME = "num_clients";

  // The map containing the set of script arguments provided when the
  // job was scheduled.
  private static HashMap<String, String> arguments = null;

  // The file parameter assigned to this job
  private static FileURLParameter bsfFile = null;

  // Parameters needed for an external BSFEngine class
  private static String bsfLanguage = null;
  private static String bsfLanguageExtension = null;
  private static String bsfEngine = null;

  // The contents of bsfFile
  private static StringBuilder script = null;

  // The BSF Manager object
  private BSFManager bsfManager = null;

  // A Vector of StatTracker objects used by the script file
  private ArrayList<StatTracker> statTrackers = null;



  /**
   * Creates a new instance of this job thread.  This constructor does not
   * need to do anything other than invoke the constructor for the
   * superclass.
   */
  public BSFJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void destroyThread()
  {
    if (this.bsfManager != null)
    {
      this.bsfManager.unregisterBean("jobClass");
      this.bsfManager.terminate();
      this.bsfManager = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "Bean Scripting Framework";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Execute a job written in a script supported by the Bean " +
           "Scripting Framework";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobCategoryName()
  {
    return "Scripting";
  }



  /**
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    LabelParameter scriptDescription = new LabelParameter(
         "To use an external script engine, supply the next three parameters." +
         " If the engine is included in the bsf.jar file, leave these " +
         "arguments blank.");
    StringParameter scriptLanguageParameter = new StringParameter(
         SCRIPT_LANGUAGE_PARAMETER_NAME, "BSF Language Name",
         "The name of the language to register with the BSFManager.", false,
         null);
    StringParameter scriptExtensionParameter = new StringParameter(
         SCRIPT_EXTENSION_PARAMETER_NAME, "Scripting File Extension",
         "The file name extension for this scripting language.", false, null);
    StringParameter scriptEngineParameter = new StringParameter(
         ENGINE_CLASS_PARAMETER_NAME, "Script Engine Class Name",
         "The fully qualified class name for the BSFEngine class implemented " +
         "for this scripting language.",
         false, null);
    FileURLParameter scriptFileParameter =
         new FileURLParameter(FILE_PATH_PARAMETER_NAME, "Script File URL",
                              "The URL to the file containing the script to " +
                              "execute.  This URL may reference the file "
                              + "location using http, https, ftp, or file.",
                              null, true);

    MultiLineTextParameter scriptArgumentsParameter =
         new MultiLineTextParameter(SCRIPT_ARGUMENTS_PARAMETER_NAME,
                                    "Script Arguments",
                                    "A set of arguments that may be provided "
                                    + "to the script that can customize the " +
                                    "way that it behaves.  The arguments "
                                    +
                                    "should be provided one per line in the " +
                                    "format \"name=value\".", new String[0],
                                                              false);
    scriptArgumentsParameter.setVisibleColumns(80);
    scriptArgumentsParameter.setVisibleRows(10);

    Parameter[] parameterArray = new Parameter[]
         {
              scriptDescription,
              scriptLanguageParameter,
              scriptExtensionParameter,
              scriptEngineParameter,
              scriptFileParameter,
              scriptArgumentsParameter
         };

    return new ParameterList(parameterArray);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackerStubs(String clientID, String threadID,
                                           int collectionInterval)
  {
    return new StatTracker[0];
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StatTracker[] getStatTrackers()
  {
    // If the script has added stat trackers, then get the stat trackers
    // associated with it.  Otherwise, just return an empty set.
    if (statTrackers.isEmpty())
    {
      return new StatTracker[0];
    }
    return statTrackers.toArray(new StatTracker[0]);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void validateJobInfo(int numClients, int threadsPerClient,
                              int threadStartupDelay, Date startTime,
                              Date stopTime, int duration,
                              int collectionInterval, ParameterList parameters)
       throws InvalidValueException
  {
    // This is sneaky.  We're going to pass the number of clients to the job as
    // a job parameter.  Otherwise, the job would have no idea how many clients
    // there are, and therefore would have no idea how to break up the work for
    // each client.
    IntegerParameter clientsParameter =
         new IntegerParameter(NUMBER_OF_CLIENTS_PARAMETER_NAME, numClients);
    parameters.addParameter(clientsParameter);

    //Determine if all of the arguments to register a script engine have been
    // supplied
    bsfLanguage = parameters.getStringParameter(SCRIPT_LANGUAGE_PARAMETER_NAME)
         .getStringValue();
    bsfLanguageExtension = parameters
         .getStringParameter(SCRIPT_EXTENSION_PARAMETER_NAME).getStringValue();
    bsfEngine = parameters.getStringParameter(ENGINE_CLASS_PARAMETER_NAME)
         .getStringValue();

    if ((!(bsfLanguage.length() + bsfLanguageExtension.length() +
           bsfEngine.length() == 0)) &&
                                     !((bsfLanguage.length() > 0) &&
                                       (bsfLanguageExtension.length() > 0) &&
                                       (bsfEngine.length() > 0)))
    {
      throw new InvalidValueException(
           "To use an external script engine, you must supply a value for " +
           "all three of these parameters, otherwise leave them all blank: " +
           "'BSF Language Name', 'Scripting File Extension', and 'Script " +
           "Engine Class Name'");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
       throws UnableToRunException
  {
    // Retrieve the Script file to ensure that it is available and can
    // be parsed properly.  If not, then throw an exception to let the end
    // user know what the problem is.
    bsfFile = parameters.getFileURLParameter(FILE_PATH_PARAMETER_NAME);
    try
    {
      String[] scriptLines = bsfFile.getFileLines();
      script = new StringBuilder();
      for (int i = 0; i < scriptLines.length; i++)
      {
        script.append(scriptLines[i])
             .append(System.getProperty("line.separator"));
      }
    }
    catch (Exception e)
    {
      throw new UnableToRunException("Unable to retrieve the Script file:  " +
                                     bsfFile.getValueString() + '\n' +
                                     stackTraceToString(e));
    }

    MultiLineTextParameter mtp =
         parameters.getMultiLineTextParameter(SCRIPT_ARGUMENTS_PARAMETER_NAME);

    arguments = new HashMap<String, String>();

    // Get the total number of clients.
    IntegerParameter clientsParameter =
         parameters.getIntegerParameter(NUMBER_OF_CLIENTS_PARAMETER_NAME);
    if (clientsParameter != null)
    {
      arguments.put(NUMBER_OF_CLIENTS_PARAMETER_NAME,
                    clientsParameter.getValueString());
    }

    // All three parameters are needed to register a new scripting engine,
    // because the JobClass uses getLangFromFilename() to determine the language
    // from the file extension.
    if ((bsfLanguage != null) && (bsfLanguageExtension != null) &&
        (bsfEngine != null))
    {
      BSFManager.registerScriptingEngine(bsfLanguage, bsfEngine, new String[]{
           bsfLanguage,
           bsfLanguageExtension });
    }

    if (mtp != null)
    {
      String[] lines = mtp.getNonBlankLines();
      for (int i = 0; i < lines.length; i++)
      {
        int equalPos = lines[i].indexOf('=');
        if (equalPos > 0)
        {
          String name = lines[i].substring(0, equalPos);
          String value = lines[i].substring(equalPos + 1);
          arguments.put(name, value);
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval,
                               ParameterList parameters)
       throws UnableToRunException
  {
    this.statTrackers = new ArrayList<StatTracker>();

    this.bsfManager = new BSFManager();

    //Add the jobClass to the namespace of the script
    this.bsfManager.registerBean("jobClass", this);

    //Add the Hashtable named "arguments"  to the namespace of the script
    this.bsfManager.registerBean("arguments", arguments);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void runJob()
  {
    // Execute the instructions contained in the script.
    try
    {
      // Execute the script using the file name extension to determine the
      // language
      this.bsfManager.exec(
           BSFManager.getLangFromFilename(bsfFile.getValueString()),
           new File(bsfFile.getValueString()).getName(), 1, 1,
           script.toString());
    }
    catch (Exception e)
    {
      this.logMessage(
           "Exception occurred while running script : " + e.toString());
      e.printStackTrace();
      indicateStoppedDueToError();
    }
    finally
    {
      if (this.bsfManager != null)
      {
        this.bsfManager.unregisterBean("jobClass");
        this.bsfManager.terminate();
        this.bsfManager = null;
      }
    }
  }



  /**
   * Initializes a StatTracker created in the Script file. Adds the
   * StatTracker to the Vector of StatTrackers that will be returned by the
   * job in the getStatTrackers() function. Starts the tracker and enables
   * real time statistics if necessary.
   *
   * @param theTracker  The StatTracker class.
   * @param displayName The StatTracker's display name.
   *
   * @return The initialized StatTracker.
   */
  public StatTracker initializeTracker(StatTracker theTracker,
                                       String displayName)
  {
    statTrackers.add(theTracker);
    theTracker.setDisplayName(displayName);
    theTracker.setCollectionInterval(this.getCollectionInterval());
    theTracker.setClientID(this.getClientID());
    theTracker.setThreadID(this.getThreadID());
    theTracker.startTracker();
    RealTimeStatReporter statReporter = this.getStatReporter();
    if ((this.enableRealTimeStats()) && (statReporter != null))
    {
      theTracker.enableRealTimeStats(statReporter, this.getJobID());
    }
    return theTracker;
  }



  /**
   * Retrieves the value from a specified job argument
   *
   * @param argName      The name of the argument to retrieve.
   * @param defaultValue The default value if one is not in the Hashtable.
   *
   * @return The argument's value as a string.
   */
  public String getArgument(String argName, String defaultValue)
  {
    String value = defaultValue;

    if (arguments.containsKey(argName))
    {
      value = arguments.get(argName);
    }
    else
    {
      if (defaultValue != null)
      {
        arguments.put(argName, defaultValue);
      }
    }
    if (this.getThreadNumber() == 0)
    {
      this.logMessage(GregorianCalendar.getInstance().getTime().toString() +
                      '\t' + this.getThreadID() + "\t:\t Argument '" + argName +
                      "' has the value '" + value + '\'');
    }
    return value;
  }



  /**
   * Retrieves the value from a specified job argument
   *
   * @param argName The name of the argument to retrieve.
   *
   * @return The argument's value as a string or null.
   */

  public String getArgument(String argName)
  {
    return getArgument(argName, null);
  }
}

