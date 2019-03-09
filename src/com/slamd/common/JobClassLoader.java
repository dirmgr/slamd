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
package com.slamd.common;



import java.io.File;
import java.io.FileInputStream;

import com.slamd.job.JobClass;



/**
 * This class defines a class loader that will be used to load SLAMD job
 * classes.  Defining a custom class loader for job classes makes it possible to
 * unload them if necessary to replace them on the fly in a running VM.
 *
 *
 * @author   Neil A. Wilson
 */
public class JobClassLoader
       extends ClassLoader
{
  // The parent class loader associated with this loader.
  ClassLoader parent;

  // The filesystem path that indicates where the classes may be found.
  String classPath;



  /**
   * Creates a new instance of this job class loader.
   *
   * @param  parentClassLoader  The class loader that should be used if this
   *                            class loader cannot create the class.
   * @param  classPath          The path to the root of the hierarchy for the
   *                            class files to be loaded.
   */
  public JobClassLoader(ClassLoader parentClassLoader, String classPath)
  {
    super(parentClassLoader);
    this.parent    = parentClassLoader;
    this.classPath = classPath;
  }



  /**
   * Retrieves the job class with the specified name.
   *
   * @param  className  The fully-qualified name of the Java class to retrieve.
   *
   * @return  The requested job class.
   *
   * @throws  SLAMDException  If a problem occurs while trying to load the
   *                          requested class.
   */
  public JobClass getJobClass(String className)
         throws SLAMDException
  {
    String filePath  = classPath + '/' + className.replace('.', '/') + ".class";
    File   classFile = new File(filePath);

    if (! (classFile.exists() && classFile.isFile()))
    {
      try
      {
        return (JobClass) Constants.classForName(className).newInstance();
      }
      catch (Exception e)
      {
        throw new SLAMDException("File " + classFile + " associated with job " +
                                 "class " + className +
                                 " either does not exist or is not a file.", e);
      }
    }

    byte[] fileBytes;
    try
    {
      int fileSize = (int) classFile.length();
      fileBytes = new byte[fileSize];

      FileInputStream inputStream = new FileInputStream(classFile);
      int totalBytesRead = inputStream.read(fileBytes);
      while (totalBytesRead < fileSize)
      {
        totalBytesRead += inputStream.read(fileBytes, totalBytesRead,
                                           (fileSize - totalBytesRead));
      }

      inputStream.close();
    }
    catch (Exception e)
    {
      throw new SLAMDException("Error loading class file " + filePath +
                               " for job class " + className + ":  " + e, e);
    }

    Class jobClass;
    try
    {
      jobClass = defineClass(className, fileBytes, 0, fileBytes.length);
      resolveClass(jobClass);
    }
    catch (Exception e)
    {
      throw new SLAMDException("Error parsing file " + filePath +
                               " as a Java class:  " + e, e);
    }

    try
    {
      return (JobClass) jobClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new SLAMDException("Unable to instantiate class " + className +
                               " as a job class:  " + e, e);
    }
  }



  /**
   * Retrieves the binary representation of the specified job class.
   *
   * @param  className  The fully-qualified name of the Java class to retrieve.
   *
   * @return  The bytes contained in the job class file.
   *
   * @throws  SLAMDException  If a problem occurs while trying to load the
   *                          requested class.
   */
  public byte[] getJobClassBytes(String className)
         throws SLAMDException
  {
    String filePath  = classPath + '/' + className.replace('.', '/') + ".class";
    File   classFile = new File(filePath);

    if (! (classFile.exists() && classFile.isFile()))
    {
      throw new SLAMDException("File " + classFile + " associated with job " +
                               "class " + className +
                               " either does not exist or is not a file.");
    }

    try
    {
      int    fileSize  = (int) classFile.length();
      byte[] fileBytes = new byte[fileSize];

      FileInputStream inputStream = new FileInputStream(classFile);
      int totalBytesRead = inputStream.read(fileBytes);
      while (totalBytesRead < fileSize)
      {
        totalBytesRead += inputStream.read(fileBytes, totalBytesRead,
                                           (fileSize - totalBytesRead));
      }

      inputStream.close();
      return fileBytes;
    }
    catch (Exception e)
    {
      throw new SLAMDException("Error loading class file " + filePath +
                               " for job class " + className + ":  " + e, e);
    }
  }
}

