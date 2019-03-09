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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.slamd.common.Constants;
import com.slamd.stat.StatTracker;



/**
 * Monitored entry represents an LDAP entry with one or more monitored
 * attributes. The resource monitore retrieves only the monitored attributes
 * of monitored entries in each collection period.
 */
class LDAPMonitoredEntry
{
  // DN of the monitored entry
  private final String dn;

  // array of attribute names to be monitored
  private final String[] attrNames;

  // map of attribute names to monitored attr objects
  private final Map<String,LDAPMonitoredAttr> attrs;

  // property base within the configuration file (used for logging purposes)
  private final String propBase;

  private final List<StatTracker> trackers = new ArrayList<StatTracker>();

  /**
   * Creates a monitored entry using the properties
   * @param propBase property base in the main configuration files (characters
   * up to the first dot)
   * @param entryProps properties as a map under the property base
   * @throws IllegalArgumentException if the properties contain illegal values
   */
  LDAPMonitoredEntry(String propBase,
                     final Map<String,String> entryProps)
  {
    final String dn = entryProps.get("dn");
    if (dn == null)
    {
      throw new IllegalArgumentException(
          "missing dn for monitored entry under property base" + propBase);
    }

    final Map<String,LDAPMonitoredAttr> attrMap =
        new HashMap<String,LDAPMonitoredAttr>();
    final Pattern attrPattern =
        Pattern.compile("^attr\\.(.+)\\.(display|tracker)$");

    for (Map.Entry<String,String> attrEntry : entryProps.entrySet())
    {
      final String key = attrEntry.getKey();
      final String val = attrEntry.getValue();

      final Matcher attrMatcher = attrPattern.matcher(key);
      if (!attrMatcher.matches())
      {
        continue;
      }

      // attribute name is the first capturing group
      final String attrName = attrMatcher.group(1);
      LDAPMonitoredAttr attr = attrMap.get(attrName);
      if (attr == null)
      {
        attr = new LDAPMonitoredAttr(attrName);
        attrMap.put(attrName, attr);
      }

      // suffix is the second capturing group
      final String suffix = attrMatcher.group(2);
      if ("display".equals(suffix))
      {
        attr.setDisplayName(val);

      }
      else if ("tracker".equals(suffix))
      {
        StatTracker t = createTracker(val);
        trackers.add(t);
        attr.setTracker(t);
      }
      else
      {
        throw new IllegalArgumentException(
            String.format("Invalid suffix %s for attribute %s in " +
                "property base %s", suffix, attrName, propBase)
        );
      }
    }

    final Set<String> attrNameSet = attrMap.keySet();
    this.attrNames = attrNameSet.toArray(new String[attrNameSet.size()]);
    this.attrs = attrMap;
    this.propBase = propBase;
    this.dn = dn;
  }


  /**
   * Returns the distinguished name of the monitored entry.
   * @return the distinguished name of the monitored entry
   */
  String getDN()
  {
    return this.dn;
  }

  /**
   * Returns the names of the monitoried attributes.
   * @return the names of the monitoried attributes
   */
  String[] getMonitoredAttrNames()
  {
    return this.attrNames;
  }

  /**
   * Returns the
   * {@link com.slamd.resourcemonitor.LDAPMonitoredAttr}
   * object corresponding to the attribute name.
   * @param attrName attribute name
   * @return a
   * {@link com.slamd.resourcemonitor.LDAPMonitoredAttr}
   * object
   */
  LDAPMonitoredAttr getMonitoredAttr(String attrName)
  {
    return this.attrs.get(attrName);
  }

  /**
   * Returns the {@link LDAPMonitoredAttr} objects encapsulated in this entry.
   * @return the {@link LDAPMonitoredAttr} objects encapsulated in this entry
   */
  Collection<LDAPMonitoredAttr> getMonitoredAttrs()
  {
    return this.attrs.values();
  }

  Collection<StatTracker> getStatTrackers()
  {
    return this.trackers;
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    return String.format("property base: %s, dn: %s, attrs: %s",
        this.propBase, this.dn, Arrays.asList(this.attrNames)
    );
  }

  /**
   * Returns the {@link com.slamd.stat.StatTracker} corresponding to the
   * name. The name is the class name (without the full package name) of the
   * tracker class.
   *
   * @param trackerClass class name of the
   *                     {@link com.slamd.stat.StatTracker} without the
   *                     full package path
   * @return the {@link com.slamd.stat.StatTracker} corresponding to the
   *         name
   * @throws IllegalArgumentException if the class cannot be instantiated for
   *         some reason, or the instantiated class is not a
   *         {@link com.slamd.stat.StatTracker}
   */
  @SuppressWarnings("unchecked")
  private static StatTracker createTracker(final String trackerClass)
  {
    String fullClass = "com.slamd.stat." + trackerClass;
    try
    {
      Class<?> cl = Constants.classForName(fullClass);
      Object o = cl.newInstance();
      if (!(o instanceof StatTracker))
      {
        throw new IllegalArgumentException(
            "Class " + fullClass + " is not a stat tracker"
        );
      }
      return (StatTracker)o;

    }
    catch (ClassNotFoundException e)
    {
      throw new IllegalArgumentException(
          "Could not instantiate class " + fullClass, e
      );
    }
    catch (InstantiationException e)
    {
      throw new IllegalArgumentException(
          "Could not instantiate class " + fullClass, e
      );
    }
    catch (IllegalAccessException e)
    {
      throw new IllegalArgumentException(
          "Could not instantiate class " + fullClass, e
      );
    }
  }
}
