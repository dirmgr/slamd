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
package com.slamd.tools;



import java.io.File;
import java.util.Iterator;
import java.util.List;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.slamd.asn1.ASN1Element;



/**
 * This class defines a utility that can be used to print out debug information
 * from the contents of the SLAMD configuration database.  It can be used to
 * either print only keys or both print keys and values.
 *
 *
 * @author   Neil A. Wilson
 */
public class DBDump
{
  /**
   * Parses the command-line arguments, opens the database, and prints out the
   * specified debug information.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // Indicates whether to print out a list of all the individual databases in
    // the DB environment.
    boolean listDBs = false;

    // Indicates whether to list all the individual keys in each of the
    // databases.
    boolean listKeys = false;

    // Indicates whether to list the values for the keys in each of the
    // databases.
    boolean listValues = false;

    // Indicates whether to operate in verbose mode.
    boolean verboseMode = false;

    // The path to the directory containing the database files.
    String dbDirectory = null;

    // The name of the database to debug.
    String dbName = null;

    // The name of the database key to retrieve.
    String keyName = null;


    // Process the command-line arguments provided to the program.
    for (int i=0; i < args.length; i++)
    {
      if (args[i].equals("-d"))
      {
        dbDirectory = args[++i];
      }
      else if (args[i].equals("-l"))
      {
        listDBs = true;
      }
      else if (args[i].equals("-k"))
      {
        listKeys = true;
      }
      else if (args[i].equals("-v"))
      {
        listValues = true;
      }
      else if (args[i].equals("-V"))
      {
        verboseMode = true;
      }
      else if (args[i].equals("-K"))
      {
        keyName = args[++i];
      }
      else if (args[i].equals("-D"))
      {
        dbName = args[++i];
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
        System.exit(1);
      }
    }


    // Make sure that at least the database location was specified.
    if (dbDirectory == null)
    {
      System.err.println("ERROR:  No database location specified (use -d)");
      displayUsage();
      System.exit(1);
    }


    // Make sure that the database directory exists.
    File dbDir = new File(dbDirectory);
    if (! (dbDir.exists() || dbDir.isDirectory()))
    {
      System.err.println("ERROR:  Specified database directory \"" +
                         dbDirectory +
                         "\" does not exist or is not a directory.");
      System.exit(1);
    }


    // Try to open the database environment.
    EnvironmentConfig envConfig = new EnvironmentConfig();
    envConfig.setAllowCreate(false);
    envConfig.setReadOnly(true);

    Environment dbEnv = null;
    try
    {
      if (verboseMode)
      {
        System.err.println("Going to open the database environment...");
      }

      dbEnv = new Environment(dbDir, envConfig);

      if (verboseMode)
      {
        System.err.println("     DB environment opened successfully.");
      }
    }
    catch (Exception e)
    {
      System.err.println("ERROR:  Unable to open the database environment.");
      e.printStackTrace();
      System.exit(1);
    }


    // Get a list of the databases contained in the DB environment.
    List dbList = null;
    try
    {
      if (verboseMode)
      {
        System.err.println("Going to obtain a list of the available DBs...");
      }

      dbList = dbEnv.getDatabaseNames();

      if (verboseMode)
      {
        System.err.println("     Successfully obtained the DB list:");
        Iterator iterator = dbList.iterator();
        while (iterator.hasNext())
        {
          System.err.println("          " + iterator.next());
        }
      }
    }
    catch (Exception e)
    {
      try
      {
        dbEnv.close();
      } catch (Exception e2) {}

      System.err.println("ERROR:  Unable to obtain a list of the available " +
                         "databases.");
      e.printStackTrace();
      System.exit(1);
    }


    // If we were supposed to list the available databases, then do so now.
    if (listDBs)
    {
      System.out.println("Databases available in the DB environment:");
      Iterator iterator = dbList.iterator();
      while (iterator.hasNext())
      {
        System.out.println("- " + iterator.next());
      }

      System.out.println();
    }


    // If a specific database was specified, then see if it exists.
    if (dbName != null)
    {
      if (! dbList.contains(dbName))
      {
        try
        {
          dbEnv.close();
        } catch (Exception e) {}

        System.err.println("ERROR:  Specified database \"" + dbName +
                           "\" could not be found.");
        System.exit(1);
      }
    }


    // If don't need to list the keys and/or values, then we're done.
    if (! (listKeys || listValues || (keyName != null)))
    {
      if (verboseMode)
      {
        System.err.println("Going to close the database environment...");
      }

      try
      {
        dbEnv.close();
      } catch (Exception e) {}

      if (verboseMode)
      {
        System.err.println("     DB environment closed.");
      }

      return;
    }


    // Iterate through the DBs and print out the debug information from them.
    Iterator iterator = dbList.iterator();
    while (iterator.hasNext())
    {
      // Check to see if this DB is one that we should investigate.
      String name = (String) iterator.next();
      if ((dbName != null) && (! dbName.equals(name)))
      {
        continue;
      }


      // Try to open the specified database.
      Database db = null;
      try
      {
        if (verboseMode)
        {
          System.err.println("Going to open DB " + name + "...");
        }

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(false);
        dbConfig.setReadOnly(true);

        db = dbEnv.openDatabase(null, name, dbConfig);

        if (verboseMode)
        {
          System.err.println("     Database opened successfully.");
        }
      }
      catch (Exception e)
      {
        try
        {
          dbEnv.close();
        } catch (Exception e2) {}

        System.err.println("Error:  Unable to open the " + name + " DB:");
        e.printStackTrace();
        System.exit(1);
      }


      // If a specific key was given, then retrieve it.
      if (keyName != null)
      {
        DatabaseEntry   keyEntry   = new DatabaseEntry(keyName.getBytes());
        DatabaseEntry   valueEntry = new DatabaseEntry();
        OperationStatus status     = null;

        try
        {
          if (verboseMode)
          {
            System.err.println("Going to get " + keyName + "...");
          }

          status = db.get(null, keyEntry, valueEntry,
                          LockMode.READ_UNCOMMITTED);

          if (verboseMode)
          {
            System.err.println("     Successfully performed the get.");
          }
        }
        catch (Exception e)
        {
          try
          {
            db.close();
          } catch (Exception e2) {}

          try
          {
            dbEnv.close();
          } catch (Exception e2) {}

          System.err.println("ERROR:  Unable to get DB key \"" + keyName +
                             "\":");
          e.printStackTrace();
          System.exit(1);
        }


        if (status == OperationStatus.SUCCESS)
        {
          System.out.println("DB \"" + name + "\" key \"" + keyName + '"');
          if (listValues)
          {
            String valueStr = ASN1Element.byteArrayToStringWithASCII(
                                               valueEntry.getData(), 5);
            System.out.println("Value:");
            System.out.println(valueStr);
          }
        }
        else
        {
          System.out.println("Could not retrieve record with key \"" + keyName +
                             "\" from database \"" + name + "\" -- " + status);
        }


        try
        {
          db.close();
        } catch (Exception e) {}

        continue;
      }


      // Get a cursor to use for the database.
      Cursor cursor = null;
      try
      {
        if (verboseMode)
        {
          System.err.println("Going to create a cursor for " + name + "...");
        }

        CursorConfig cursorConfig = new CursorConfig();
        cursorConfig.setReadUncommitted(true);

        cursor = db.openCursor(null, cursorConfig);

        if (verboseMode)
        {
          System.err.println("     Cursor created successfully.");
        }
      }
      catch (Exception e)
      {
        try
        {
          db.close();
        } catch (Exception e2) {}

        try
        {
          dbEnv.close();
        } catch (Exception e2) {}

        System.err.println("Error:  Unable to create a cursor for the " + name +
                           " DB:");
        e.printStackTrace();
        System.exit(1);
      }


      // Iterate through all the keys in the database.
      try
      {
        if (verboseMode)
        {
          System.err.println("Going to cursor through DB " + name + "...");
        }

        DatabaseEntry keyEntry   = new DatabaseEntry();
        DatabaseEntry valueEntry = new DatabaseEntry();

        OperationStatus status = cursor.getFirst(keyEntry, valueEntry,
                                                 LockMode.READ_UNCOMMITTED);
        while (status == OperationStatus.SUCCESS)
        {
          String keyStr = new String(keyEntry.getData());
          System.out.println("DB \"" + name + "\" key \"" + keyStr + '"');
          if (listValues)
          {
            String valueStr = ASN1Element.byteArrayToStringWithASCII(
                                               valueEntry.getData(), 5);
            System.out.println("Value:");
            System.out.println(valueStr);
          }

          status = cursor.getNext(keyEntry, valueEntry,
                                  LockMode.READ_UNCOMMITTED);
        }

        cursor.close();

        if (verboseMode)
        {
          System.err.println("     Done cursoring through DB " + name + '.');
        }
      }
      catch (Exception e)
      {
        try
        {
          cursor.close();
        } catch (Exception e2) {}

        try
        {
          db.close();
        } catch (Exception e2) {}

        try
        {
          dbEnv.close();
        } catch (Exception e2) {}

        System.err.println("Error:  Unable to cursor through the " + name +
                           " DB:");
        e.printStackTrace();
        System.exit(1);
      }


      // Close the DB and move onto the next one.
      try
      {
        if (verboseMode)
        {
          System.err.println("Going to close the database " + name + "...");
        }

        db.close();

        if (verboseMode)
        {
          System.err.println("     DB successfully closed.");
        }
      }
      catch (Exception e)
      {
        try
        {
          dbEnv.close();
        } catch (Exception e2) {}

        System.err.println("Error:  Unable to close the " + name + " DB:");
        e.printStackTrace();
        System.exit(1);
      }
    }


    // Close the DB environment and exit.
    if (verboseMode)
    {
      System.err.println("Going to close the database environment...");
    }

    try
    {
      dbEnv.close();
    } catch (Exception e) {}

    if (verboseMode)
    {
      System.err.println("     DB environment closed.");
    }
    return;
  }



  /**
   * Displays usage information for this program.
   */
  public static void displayUsage()
  {
    String EOL = System.getProperty("line.separator");

    System.err.println(
"USAGE:  java DBDump {options}" + EOL +
"        where {options} include:" + EOL +
"-d {dir}     -- The path to the directory containing the DB files" + EOL +
"-l           -- Indicates that the names of the DBs should be listed" + EOL +
"-k           -- Indicates that the database keys should be listed" + EOL +
"-v           -- Indicates that the database values should be listed" + EOL +
"-D {dbname}  -- The name of the DB to use (default is all DBs)" + EOL +
"-K {key}     -- The specific key to retrieve (default is all keys)" + EOL +
"-V           -- Indicates that this program should run in verbose mode" + EOL +
"-H           -- Displays this usage information"
                      );
  }
}

