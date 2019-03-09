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



import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.common.Constants;
import com.slamd.common.SLAMDException;
import com.slamd.protocol.SLAMDMessage;
import com.slamd.resourcemonitor.ResourceMonitor;



/**
 * This class defines a data structure used to hold information about a SLAMD
 * statistic used for the purpose of resource monitoring.  It encapsulates a
 * stat tracker and associates it with the name of the resource monitor class
 * that was used to collect that data.
 *
 *
 * @author   Neil A. Wilson
 */
public class ResourceMonitorStatTracker
{
  /**
   * The name that will be used to identify the resource monitor class in
   * encoded resource monitor stat tracker elements.
   */
  public static final String PROPERTY_MONITOR_CLASS = "monitor_class";



  /**
   * The name that will be used to identify the stat tracker data in encoded
   * resource monitor stat tracker elements.
   */
  public static final String PROPERTY_STAT_TRACKER = "stat_tracker";



  // The stat tracker with the data that was collected.
  private StatTracker statTracker;

  // An instance of the resource monitor that was used to capture the data.
  private ResourceMonitor resourceMonitor;



  /**
   * Creates a new instance of this resource monitor stat tracker with the
   * provided information.
   *
   * @param  resourceMonitor  An instance of the resource monitor that was used
   *                          to capture the data.
   * @param  statTracker      The stat tracker with the data that was collected.
   */
  public ResourceMonitorStatTracker(ResourceMonitor resourceMonitor,
                                    StatTracker statTracker)
  {
    this.resourceMonitor = resourceMonitor;
    this.statTracker     = statTracker;
  }



  /**
   * Retrieves an instance of the resource monitor that was used to capture the
   * data.
   *
   * @return  An instance of the resource monitor that was used to capture the
   *          data.
   */
  public ResourceMonitor getResourceMonitor()
  {
    return resourceMonitor;
  }



  /**
   * Retrieves the stat tracker with the data that was collected.
   *
   * @return  The stat tracker with the data that was collected.
   */
  public StatTracker getStatTracker()
  {
    return statTracker;
  }



  /**
   * Encodes the information in this resource monitor stat tracker to an ASN.1
   * element.
   *
   * @return  The ASN.1 element containing the encoded resource monitor stat
   *          tracker.
   */
  public ASN1Element encode()
  {
    ASN1Element[] elements = new ASN1Element[]
    {
      SLAMDMessage.encodeNameValuePair(PROPERTY_MONITOR_CLASS,
           new ASN1OctetString(resourceMonitor.getClass().getName())),
      SLAMDMessage.encodeNameValuePair(PROPERTY_STAT_TRACKER,
           StatEncoder.trackerToSequence(statTracker))
    };

    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 element as a resource monitor stat tracker.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded resource monitor stat tracker.
   *
   * @throws SLAMDException  If a problem occurs while attempting to decode the
   *                         provided ASN.1 element as a resource monitor stat
   *                         tracker.
   */
  public static ResourceMonitorStatTracker decode(ASN1Element element)
         throws SLAMDException
  {
    HashMap<String,ASN1Element> propertyMap =
         SLAMDMessage.decodeNameValuePairSequence(element);

    ResourceMonitor resourceMonitor;
    ASN1Element valueElement = propertyMap.get(PROPERTY_MONITOR_CLASS);
    if (valueElement == null)
    {
      throw new SLAMDException("Resource monitor stat tracker sequence does " +
                               "not include the monitor class name element.");
    }
    else
    {
      try
      {
        String monitorClassName =
             valueElement.decodeAsOctetString().getStringValue();
        Class<?> monitorClass = Constants.classForName(monitorClassName);
        resourceMonitor = (ResourceMonitor) monitorClass.newInstance();
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the monitor class name:  " +
                                 e, e);
      }
    }


    StatTracker statTracker;
    valueElement = propertyMap.get(PROPERTY_STAT_TRACKER);
    if (valueElement == null)
    {
      throw new SLAMDException("Resource monitor stat tracker sequence does " +
                               "not include the stat tracker data element.");
    }
    else
    {
      try
      {
        ASN1Sequence sequence = valueElement.decodeAsSequence();
        statTracker = StatEncoder.sequenceToTracker(sequence);
      }
      catch (Exception e)
      {
        throw new SLAMDException("Unable to decode the stat tracker data:  " +
                                 e, e);
      }
    }


    return new ResourceMonitorStatTracker(resourceMonitor, statTracker);
  }



  /**
   * Encodes the provided list of resource monitor stat trackers to an ASN.1
   * sequence.
   *
   * @param  trackerArray  The array of resource monitor stat trackers to be
   *                       encoded.
   *
   * @return  The ASN.1 sequence containing the encoded resource monitor stat
   *          tracker information.
   */
  public static ASN1Sequence trackersToSequence(
                                  ResourceMonitorStatTracker[] trackerArray)
  {
    ASN1Element[] elements = new ASN1Element[trackerArray.length];
    for (int i=0; i < elements.length; i++)
    {
      elements[i] = trackerArray[i].encode();
    }

    return new ASN1Sequence(elements);
  }



  /**
   * Encodes the provided list of resource monitor stat trackers to an ASN.1
   * sequence.
   *
   * @param  trackerList  The list containing the resource monitor stat trackers
   *                      to be encoded.
   *
   * @return  The ASN.1 sequence containing the encoded resource monitor stat
   *          tracker information.
   */
  public static ASN1Sequence trackersToSequence(ArrayList trackerList)
  {
    ASN1Element[] elements = new ASN1Element[trackerList.size()];
    for (int i=0; i < elements.length; i++)
    {
      elements[i] = ((ResourceMonitorStatTracker) trackerList.get(i)).encode();
    }

    return new ASN1Sequence(elements);
  }



  /**
   * Decodes the provided ASN.1 sequence as an array of resource monitor stat
   * trackers.
   *
   * @param  trackerSequence  The sequence containing the data to be decoded.
   *
   * @return  The set of resource monitor stat trackers decoded from the
   *          provided sequence.
   *
   * @throws  SLAMDException  If a problem occurs while attempting to decode the
   *                          provided sequence to a set of resource monitor
   *                          stat trackers.
   */
  public static ResourceMonitorStatTracker[] sequenceToTrackers(ASN1Sequence
                                                                trackerSequence)
         throws SLAMDException
  {
    ASN1Element[] elements = trackerSequence.getElements();
    ResourceMonitorStatTracker[] trackers =
         new ResourceMonitorStatTracker[elements.length];
    for (int i=0; i < elements.length; i++)
    {
      trackers[i] = decode(elements[i]);
    }

    return trackers;
  }
}

