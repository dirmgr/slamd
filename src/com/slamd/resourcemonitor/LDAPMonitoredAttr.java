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
 * Contributor(s):  Bertold Kolics
 */
package com.slamd.resourcemonitor;



import java.util.ArrayList;
import java.util.List;

import com.slamd.stat.FloatValueTracker;
import com.slamd.stat.IncrementalTracker;
import com.slamd.stat.IntegerValueTracker;
import com.slamd.stat.LongValueTracker;
import com.slamd.stat.StatTracker;




/**
 * This helper class holds information about a tracked LDAP attribute (name,
 * type, display name) as well as the captured data.
 */
class LDAPMonitoredAttr
{
  // monitored attribute name
  final String name;

  // display name - this name is displayed on the results page
  private String displayName;

  // default tracker (can be overridden)
  private StatTracker tracker = new IntegerValueTracker();

  // captured data
  private List<Number> capturedData = new ArrayList<Number>();

  // type of data to capture
  private MonitoredType attrType;

  /**
   * Creates a new monitored attribute instance with the provided name.
   * The display name of the monitored attribute will default to the
   * attribute name.
   * @param name attribute name
   */
  LDAPMonitoredAttr(final String name)
  {
    this.name = name;
    this.displayName = name;
  }

  /**
   * Sets the statistics tracker for this attribute.
   * @param tracker statistics tracker
   */
  void setTracker(StatTracker tracker)
  {
    tracker.setDisplayName(this.displayName);

    // set the type of the captured data
    if (tracker instanceof IntegerValueTracker)
    {
      this.attrType = MonitoredType.INT;
    }
    else if (tracker instanceof IncrementalTracker)
    {
      this.attrType = MonitoredType.INCREMENTAL;
    }
    else if (tracker instanceof FloatValueTracker)
    {
      this.attrType = MonitoredType.DOUBLE;
    }
    else if (tracker instanceof LongValueTracker)
    {
      this.attrType = MonitoredType.LONG;
    }
    else
    {
      throw new IllegalArgumentException("Invalid tracker type: " +
          tracker.getClass());
    }

    this.tracker = tracker;
  }

  /**
   * Sets the display name of the monitored attribute.
   * @param displayName display name
   */
  void setDisplayName(final String displayName)
  {
    this.displayName = displayName;
    this.tracker.setDisplayName(displayName);
  }

  /**
   * Adds a new captured data to the existing data set.
   * @param capturedData raw captured data
   */
  void addCapturedData(final String capturedData)
  {
    switch(this.attrType)
    {
      case INT:
        // fall through
      case INCREMENTAL:
        this.capturedData.add(Integer.valueOf(capturedData));
        break;
      case LONG:
        this.capturedData.add(Long.valueOf(capturedData));
        break;
      case DOUBLE:
        this.capturedData.add(Double.valueOf(capturedData));
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid attribute type: " + attrType);
    }
  }

  /**
   * Returns the type of the monitored attribute.
   * @return the type of the monitored attribute
   */
  MonitoredType getAttrType()
  {
    return this.attrType;
  }

  /**
   * Collects the captured data and populates the corresponding
   * {@link StatTracker}.
   */
  void collectCapturedData()
  {
    final int[] count = new int[capturedData.size()];
    int i=0;

    if (this.tracker instanceof IntegerValueTracker)
    {
      IntegerValueTracker t = (IntegerValueTracker)this.tracker;
      final int[] data = new int[capturedData.size()];

      for (Number n : capturedData)
      {
        data[i] = n.intValue();
        count[i++] = 1;
      }

      t.setIntervalData(data, count);
    }
    else if (this.tracker instanceof IncrementalTracker)
    {
      IncrementalTracker t = (IncrementalTracker)this.tracker;

      for (Number n : capturedData)
      {
        count[i++] = n.intValue();
      }
      t.setIntervalCounts(count);
    }

    else if (this.tracker instanceof LongValueTracker)
    {
      LongValueTracker t = (LongValueTracker)this.tracker;
      final long[] data = new long[capturedData.size()];

      for (Number n : capturedData)
      {
        data[i] = n.longValue();
        count[i++] = 1;
      }

      t.setIntervalData(data, count);
    }
    else if (this.tracker instanceof FloatValueTracker)
    {
      FloatValueTracker t = (FloatValueTracker)this.tracker;
      double[] data = new double[capturedData.size()];

      for (Number n : capturedData)
      {
        data[i] = n.doubleValue();
        count[i++] = 1;
      }

      t.setIntervalData(data, count);
    }
    else
    {
      throw new IllegalArgumentException("Invalid tracker type: " +
          tracker.getClass());
    }
  }
}
