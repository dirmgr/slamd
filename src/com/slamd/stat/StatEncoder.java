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



import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Exception;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;



/**
 * This class provides a mechanism to encode and decode stat trackers in a
 * manner that can be transferred over the network or written to persistent
 * storage.  The encoding will be as an ASN.1 element with the following BNF:
 *
 * <CODE>StatTracker ::= SEQUENCE {</CODE><BR>
 * <CODE>  className           OCTET STRING,</CODE><BR>
 * <CODE>  clientID            OCTET STRING,</CODE><BR>
 * <CODE>  threadID            OCTET STRING,</CODE><BR>
 * <CODE>  displayName         OCTET STRING,</CODE><BR>
 * <CODE>  collectionInterval  INTEGER,</CODE><BR>
 * <CODE>  duration            INTEGER,</CODE><BR>
 * <CODE>  data                OCTET STRING }</CODE><BR>
 *
 *
 * @author   Neil A. Wilson
 */
public class StatEncoder
{
  /**
   * Encodes the provided stat tracker into an ASN.1 sequence.
   *
   * @param  tracker  The stat tracker to be encoded.
   *
   * @return  The ASN.1 sequence encoded from the information in the provided
   *          stat tracker.
   */
  public static ASN1Sequence trackerToSequence(StatTracker tracker)
  {
    ASN1Element[] trackerElements = new ASN1Element[]
    {
      new ASN1OctetString(tracker.getClass().getName()),
      new ASN1OctetString(tracker.getClientID()),
      new ASN1OctetString(tracker.getThreadID()),
      new ASN1OctetString(tracker.getDisplayName()),
      new ASN1Integer(tracker.getCollectionInterval()),
      new ASN1Integer(tracker.getDuration()),
      new ASN1OctetString(tracker.encode())
    };

    return new ASN1Sequence(trackerElements);
  }



  /**
   * Decodes the provided ASN.1 sequence as a stat tracker.
   *
   * @param  sequence  The ASN.1 sequence to be decoded as a stat tracker.
   *
   * @return  The decoded stat tracker.
   *
   * @throws  SLAMDException  If the provided sequence cannot be decoded as a
   *                          stat tracker.
   */
  public static StatTracker sequenceToTracker(ASN1Sequence sequence)
         throws SLAMDException
  {
    try
    {
      ASN1Element[] elements = sequence.getElements();
      if (elements.length != 7)
      {
        throw new SLAMDException("There must be 7 elements in a stat tracker " +
                                 "sequence");
      }

      String className   = elements[0].decodeAsOctetString().getStringValue();
      String clientID    = elements[1].decodeAsOctetString().getStringValue();
      String threadID    = elements[2].decodeAsOctetString().getStringValue();
      String displayName = elements[3].decodeAsOctetString().getStringValue();
      int    interval    = elements[4].decodeAsInteger().getIntValue();
      int    duration    = elements[5].decodeAsInteger().getIntValue();
      byte[] data        = elements[6].decodeAsOctetString().getValue();

      Class<?> trackerClass = Constants.classForName(className);
      StatTracker tracker = (StatTracker) trackerClass.newInstance();
      tracker.setClientID(clientID);
      tracker.setThreadID(threadID);
      tracker.setDisplayName(displayName);
      tracker.setCollectionInterval(interval);
      tracker.setDuration(duration);
      tracker.decode(data);

      // If the stat tracker was not stopped properly, or if it was a persistent
      // statistic taken while the job was still running, then the duration
      // might be zero which could cause some problems.  If that is the case,
      // then at least provide an estimate of the duration.
      if ((duration <= 0) && (interval > 0) && (tracker.getNumIntervals() > 0))
      {
        tracker.setDuration(interval * tracker.getNumIntervals());
      }

      return tracker;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      if (e instanceof SLAMDException)
      {
        throw (SLAMDException) e;
      }
      else
      {
        throw new SLAMDException("Unable to decode sequence as a stat " +
                                 "tracker:  " + e, e);
      }
    }
  }



  /**
   * Encodes the provided set of stat trackers into an ASN.1 sequence.  Each
   * element of the sequence will itself be a sequence with information about a
   * single stat tracker.
   *
   * @param  trackers  The set of stat trackers to be encoded.
   *
   * @return  The ASN.1 sequence encoded from the information in the provided
   *          stat trackers.
   */
  public static ASN1Sequence trackersToSequence(StatTracker[] trackers)
  {
    ASN1Element[] elements = new ASN1Element[trackers.length];

    for (int i=0; i < elements.length; i++)
    {
      elements[i] = trackerToSequence(trackers[i]);
    }

    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 sequence as a set of stat trackers.
   *
   * @param  sequence  The ASN.1 sequence to be decoded as a set of stat
   *                   trackers.
   *
   * @return  The decoded stat trackers.
   *
   * @throws  SLAMDException  If the provided sequence cannot be decoded as a
   *                          set of stat trackers.
   */
  public static StatTracker[] sequenceToTrackers(ASN1Sequence sequence)
         throws SLAMDException
  {
    ASN1Element[] elements = sequence.getElements();
    StatTracker[] trackers = new StatTracker[elements.length];

    for (int i=0; i < elements.length; i++)
    {
      try
      {
        trackers[i] = sequenceToTracker(elements[i].decodeAsSequence());
      }
      catch (ASN1Exception ae)
      {
        throw new SLAMDException("Could not decode sequence element " + i +
                                 "as a sequence", ae);
      }
    }

    return trackers;
  }
}

