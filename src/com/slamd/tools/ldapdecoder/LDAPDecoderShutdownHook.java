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
package com.slamd.tools.ldapdecoder;



import java.io.PrintStream;
import java.util.Date;



/**
 * This class defines a shutdown hook that will be registered with the
 * LDAPDecoder tool when it starts running so that any generated script file can
 * be properly closed.  Note that the use of shutdown hooks requires a Java
 * version of at least 1.3.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPDecoderShutdownHook
       extends Thread
{
  // The LDAPDecoder with which this shutdown hook is associated.
  private final LDAPDecoder decoder;



  /**
   * Creates a shutdown hook that is associated with the provided LDAPDecoder.
   *
   * @param  decoder  The LDAPDecoder instance with which this shutdown hook is
   *                  associated.
   */
  public LDAPDecoderShutdownHook(LDAPDecoder decoder)
  {
    setName("LDAPDecoder Shutdown Hook");
    this.decoder = decoder;
  }



  /**
   * Attempts to write the end of the script file and close it properly, if one
   * is being generated.
   */
  public void run()
  {
    try
    {
      if (decoder.writeJobScript)
      {
        PrintStream scriptWriter = decoder.scriptWriter;
        scriptWriter.println();
        scriptWriter.println("#### LDAPDecoder shutdown detected at " +
                             new Date());
        scriptWriter.println("# Close the connection to the directory server.");
        scriptWriter.println("conn.disconnect();");
        scriptWriter.println();
        scriptWriter.flush();
        scriptWriter.close();
      }
    }
    catch (Exception e)
    {
      // We're already shutting down.  Nothing we can really do about an
      // exception here.
    }
  }
}

