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



/**
 * This class implements a mutex based on reference counts so that multiple
 * readers may hold the lock at the same time, but a writer must have exclusive
 * access to the lock (that is, no read locks may be in use while a write lock
 * is held).
 *
 *
 * @author   Neil A. Wilson
 */
public class RefCountMutex
{
  // Indicates whether this
  private final boolean debugMode;

  // Indicates whether the write lock is currently being held.
  private boolean writeLockHeld;

  // Used to keep track of the number of read locks currently held.
  private int referenceCount;

  // The mutex used to block lock requests while a write lock has been granted.
  private final Object lockRequestedMutex;

  // The mutex used to provide threadsafe access to the reference count.
  private final Object referenceCountMutex;

  // The mutex used to ensure that only a single write lock can be held at any
  // given time.
  private final Object writeLockMutex;



  /**
   * Creates a new reference count mutex.
   */
  public RefCountMutex()
  {
    debugMode           = false;
    writeLockHeld       = false;
    referenceCount      = 0;
    lockRequestedMutex  = new Object();
    referenceCountMutex = new Object();
    writeLockMutex      = new Object();
  }



  /**
   * Creates a new reference count mutex, optionally operating in debug mode.
   *
   * @param  debugMode  Indicates debug logging should be performed.
   */
  public RefCountMutex(boolean debugMode)
  {
    this.debugMode      = debugMode;
    writeLockHeld       = false;
    referenceCount      = 0;
    lockRequestedMutex  = new Object();
    referenceCountMutex = new Object();
    writeLockMutex      = new Object();
  }



  /**
   * Obtains a read lock.  This method will not return until the read lock has
   * been granted.
   */
  public void getReadLock()
  {
    debugPrint("In getReadLock()");
    synchronized (lockRequestedMutex)
    {
      boolean lockHeld = true;
      while (lockHeld)
      {
        synchronized (writeLockMutex)
        {
          lockHeld = writeLockHeld;
        }

        if (lockHeld)
        {
          try
          {
            Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
          } catch (InterruptedException ie) {}
        }
      }

      synchronized (referenceCountMutex)
      {
        referenceCount++;
        debugPrint("Successfully got a read lock -- refcount is " +
                   referenceCount);
      }
    }
  }



  /**
   * Releases a previously obtained read lock.
   */
  public void releaseReadLock()
  {
    debugPrint("In releaseReadLock()");
    synchronized (referenceCountMutex)
    {
      referenceCount--;
      debugPrint("Released a read lock -- refcount is " + referenceCount);

      // If this happens, then it means that there was a case in which a
      // read lock was released multiple times.
      if (referenceCount < 0)
      {
        referenceCount = 0;
        debugPrint("Reset the refcount to zero");
      }
    }
  }



  /**
   * Obtains a write lock.  This method will not return until the write lock has
   * been granted.
   */
  public void getWriteLock()
  {
    debugPrint("In getWriteLock()");
    synchronized (lockRequestedMutex)
    {
      boolean readHeld  = true;
      boolean writeHeld = true;
      while (readHeld || writeHeld)
      {
        synchronized (referenceCountMutex)
        {
          debugPrint("Blocking until the lock can be acquired -- refcount is " +
                     referenceCount);
          readHeld = (referenceCount > 0);
        }

        if (! readHeld)
        {
          synchronized (writeLockMutex)
          {
            writeHeld = writeLockHeld;
            if (! writeHeld)
            {
              writeLockHeld = true;
            }
          }
        }

        if (readHeld || writeHeld)
        {
          try
          {
            Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
          } catch (InterruptedException ie) {}
        }
      }
    }

    debugPrint("Successfully obtained the write lock.");
  }



  /**
   * Obtains a write lock.  This method will not return until the write lock has
   * been granted, or until the specified timeout occurs.
   *
   * @param  timeout  The maximum length of time (in milliseconds) to wait on
   *                  the write lock.
   *
   * @throws  InterruptedException  If the write lock could not be obtained
   *                                before the timeout occurred.
   */
  public void getWriteLock(long timeout)
         throws InterruptedException
  {
    debugPrint("In getWriteLock()");

    boolean interrupted = true;
    long interruptTime = System.currentTimeMillis() + timeout;
    synchronized (lockRequestedMutex)
    {
      boolean readHeld  = true;
      boolean writeHeld = true;
      while ((readHeld || writeHeld) &&
             (System.currentTimeMillis() < interruptTime))
      {
        synchronized (referenceCountMutex)
        {
          debugPrint("Blocking until the lock can be acquired -- refcount is " +
                     referenceCount);
          readHeld = (referenceCount > 0);
        }

        if (! readHeld)
        {
          synchronized (writeLockMutex)
          {
            writeHeld = writeLockHeld;
            if (! writeHeld)
            {
              interrupted   = false;
              writeLockHeld = true;
            }
          }
        }

        if (readHeld || writeHeld)
        {
          try
          {
            Thread.sleep(Constants.THREAD_BLOCK_SLEEP_TIME);
          } catch (InterruptedException ie) {}
        }
      }
    }

    if (interrupted)
    {
      throw new InterruptedException("Unable to obtain the write lock " +
                                     "before the specified timeout.");
    }

     debugPrint("Successfully obtained the write lock.");
  }



  /**
   * Releases a previously obtained write lock.
   */
  public void releaseWriteLock()
  {
    debugPrint("In releaseWriteLock()");

    synchronized (writeLockMutex)
    {
      writeLockHeld = false;
    }

    debugPrint("Successfully released the write lock.");
  }



  /**
   * Prints the specified message to standard error if debug mode is enabled.
   * If debug mode is not enabled, then nothing will be printed.
   *
   * @param  message  The message to be printed.
   */
  public void debugPrint(String message)
  {
    if (debugMode)
    {
      System.err.println(message);
    }
  }
}

