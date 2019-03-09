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
package com.slamd.scripting;



import java.util.Date;
import java.util.HashMap;

import com.slamd.job.JobClass;
import com.slamd.job.UnableToRunException;
import com.slamd.parameter.BooleanParameter;
import com.slamd.parameter.FileURLParameter;
import com.slamd.parameter.InvalidValueException;
import com.slamd.parameter.MultiLineTextParameter;
import com.slamd.parameter.Parameter;
import com.slamd.parameter.ParameterList;
import com.slamd.parameter.PlaceholderParameter;
import com.slamd.stat.StatTracker;
import com.slamd.scripting.engine.ScriptException;
import com.slamd.scripting.engine.ScriptParser;



/**
 * This class defines a SLAMD job that will perform its work by executing a
 * the instructions in a script file.
 *
 *
 * @author   Neil A. Wilson
 */
public class ScriptedJobClass
       extends JobClass
{
  /**
   * The name of the job parameter that indicates whether the script should be
   * executed in debug mode.
   */
  public static final String DEBUG_PARAMETER_NAME = "debug_mode";



  /**
   * The name of the job parameter that defines the script file to use.
   */
  public static final String SCRIPT_FILE_PARAMETER_NAME = "script_file";



  /**
   * The name of the job parameter that allows the user to pass arguments to the
   * script.
   */
  public static final String SCRIPT_ARGUMENTS_PARAMETER_NAME =
       "script_arguments";



  // Indicates whether the script should be executed in debug mode.
  private static boolean debugScript;

  // The map containing the set of script arguments provided when the job was
  // scheduled.
  private static HashMap<String,String> scriptArgumentHash;

  // The lines contained in the script file to be executed.
  private static String[] scriptFileLines;

  // The script parser used to handle all interaction with the script file.
  private ScriptParser parser;



  /**
   * Creates a new instance of this job thread.  This constructor
   * does not need to do anything other than invoke the constructor
   * for the superclass.
   */
  public ScriptedJobClass()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getJobName()
  {
    return "SLAMD Script";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getShortDescription()
  {
    return "Execute a user-provided SLAMD script";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String[] getLongDescription()
  {
    return new String[]
    {
      "This job can be used to execute a user-provided SLAMD script."
    };
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
   * {@inheritDoc}
   */
  @Override()
  public ParameterList getParameterStubs()
  {
    PlaceholderParameter placeholder = new PlaceholderParameter();
    BooleanParameter debugParameter =
         new BooleanParameter(DEBUG_PARAMETER_NAME, "Execute in Debug Mode",
                              "Indicates whether the script should be " +
                              "executed in debug mode.  This will be much " +
                              "less efficient, but can be useful for " +
                              "debugging problems that arise.", false);

    FileURLParameter scriptFileParameter =
         new FileURLParameter(SCRIPT_FILE_PARAMETER_NAME, "Script File URL",
                             "The URL to the file containing the script to " +
                              "execute.  This URL may reference the file " +
                              "location using http, https, ftp, or file.", null,
                              true);

    MultiLineTextParameter scriptArgumentsParameter =
         new MultiLineTextParameter(SCRIPT_ARGUMENTS_PARAMETER_NAME,
                                    "Script Arguments",
                                    "A set of arguments that may be provided " +
                                    "to the script that can customize the " +
                                    "way that it behaves.  The arguments " +
                                    "should be provided one per line in the " +
                                    "format \"name=value\".", new String[0],
                                    false);
    scriptArgumentsParameter.setVisibleColumns(80);
    scriptArgumentsParameter.setVisibleRows(10);

    Parameter[] parameterArray = new Parameter[]
    {
      placeholder,
      scriptFileParameter,
      scriptArgumentsParameter,
      debugParameter
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
    // If the script has been run, then get the stat trackers associated with
    // it.  Otherwise, just return an empty set.
    if (this.parser == null)
    {
      return new StatTracker[0];
    }
    else
    {
      return parser.getStatTrackers();
    }
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
    // Retrieve and parse the script file to ensure that it is available and can
    // be parsed properly.  If not, then throw an exception to let the end
    // user know what the problem is.
    FileURLParameter fp =
         parameters.getFileURLParameter(SCRIPT_FILE_PARAMETER_NAME);


    try
    {
      scriptFileLines = fp.getFileLines();
    }
    catch (Exception e)
    {
      throw new InvalidValueException("Unable to retrieve the script file:  " +
                                      e, e);
    }


    try
    {
      parser = new ScriptParser();
      parser.setScriptLines(scriptFileLines);
      parser.parse();
    }
    catch (ScriptException se)
    {
      throw new InvalidValueException("An error occurred while parsing the " +
                                      "script:  " + se.getMessage(), se);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeClient(String clientID, ParameterList parameters)
         throws UnableToRunException
  {
    FileURLParameter fp =
         parameters.getFileURLParameter(SCRIPT_FILE_PARAMETER_NAME);
    if (fp != null)
    {
      try
      {
        scriptFileLines = fp.getFileLines();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }


    scriptArgumentHash = new HashMap<String,String>();
    MultiLineTextParameter mtp =
         parameters.getMultiLineTextParameter(SCRIPT_ARGUMENTS_PARAMETER_NAME);
    if (mtp != null)
    {
      try
      {
        String[] lines = mtp.getNonBlankLines();
        for (int i=0; i < lines.length; i++)
        {
          int equalPos = lines[i].indexOf('=');
          if (equalPos > 0)
          {
            String name  = lines[i].substring(0, equalPos);
            String value = lines[i].substring(equalPos+1);
            scriptArgumentHash.put(name.toLowerCase(), value);
          }
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }


    debugScript = false;
    BooleanParameter bp =
         parameters.getBooleanParameter(DEBUG_PARAMETER_NAME);
    if (bp != null)
    {
      debugScript = bp.getBooleanValue();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializeThread(String clientID, String threadID,
                               int collectionInterval, ParameterList parameters)
         throws UnableToRunException
  {
    // Read and parse the script file.
    try
    {
      parser = new ScriptParser();
      parser.setScriptLines(scriptFileLines);
      parser.setScriptArguments(scriptArgumentHash);
      parser.parse();
    }
    catch (ScriptException se)
    {
      throw new UnableToRunException("Unable to parse the script:  " + se, se);
    }
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
      if (debugScript)
      {
        parser.debugExecute(this);
      }
      else
      {
        parser.execute(this);
      }
    }
    catch (Exception e)
    {
      logMessage("Exception while executing script:  " + stackTraceToString(e));
      indicateStoppedDueToError();
    }
  }
}

