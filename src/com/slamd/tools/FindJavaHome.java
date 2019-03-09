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



/**
 * This program provides a utility that may be used to automatically determine
 * an appropriate JAVA_HOME setting when invoked by a Java executable that may
 * already be in the environment path.
 *
 *
 * @author   Neil A. Wilson
 */
public class FindJavaHome
{
  /**
   * Performs the work of determining the location of the Java installation.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    // First, use the java.home system property to get the location of the
    // runtime.
    String javaHome = System.getProperty("java.home");
    if ((javaHome == null) || (javaHome.length() == 0))
    {
      // This should never happen.
      System.exit(1);
    }


    // In most cases, the java.home property will point to the JRE rather than
    // the JDK, even if it is part of the JDK.  We'll prefer the JDK over the
    // JRE, so see if that's available.  We can distinguish between the two
    // because the JDK will have a javac or javac.exe file while the JRE will
    // not.
    File javaHomeDir = new File(javaHome);
    if (! javaHomeDir.exists())
    {
      // The home directory doesn't exist.  We can't continue.
      System.exit(1);
    }

    File javacFile = new File(javaHomeDir.getAbsolutePath() + File.separator +
                              "bin" + File.separator + "javac");
    if (javacFile.exists())
    {
      System.out.print(javaHomeDir.getAbsolutePath());
      System.exit(0);
    }

    javacFile = new File(javacFile.getAbsolutePath() + ".exe");
    if (javacFile.exists())
    {
      System.out.print(javaHomeDir.getAbsolutePath());
      System.exit(0);
    }


    // We didn't find a javac in the provided JAVA_HOME, but look up a level to
    // see if it actually points to a JRE below a JDK installation.
    File jdkHomeDir = javaHomeDir.getParentFile();
    if (! jdkHomeDir.exists())
    {
      // There is no parent.  Just return the path to the JRE home.
      System.out.print(javaHomeDir.getAbsolutePath());
      System.exit(0);
    }

    javacFile = new File(jdkHomeDir.getAbsolutePath() + File.separator +
                         "bin" + File.separator + "javac");
    if (javacFile.exists())
    {
      System.out.print(jdkHomeDir.getAbsolutePath());
      System.exit(0);
    }

    javacFile = new File(javacFile.getAbsolutePath() + ".exe");
    if (javacFile.exists())
    {
      System.out.print(jdkHomeDir.getAbsolutePath());
      System.exit(0);
    }


    // If we've gotten here, then we couldn't find a JDK so go with the JRE
    // instead.
    System.out.print(javaHomeDir.getAbsolutePath());
    System.exit(0);
  }
}

