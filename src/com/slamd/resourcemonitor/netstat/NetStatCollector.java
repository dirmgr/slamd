package com.slamd.resourcemonitor.netstat;

import com.slamd.resourcemonitor.ResourceMonitor;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.io.IOException;

/**
 * Abstract class for network interface statistics collection. This class is to
 * be extended by operating system specific collectors, since it does not
 * define any particular collection mechanisms. This class handles the
 * configuration of collectors.
 */
public abstract class NetStatCollector
{
  // Resource monitor this collector is associated with. Used for invoking
  // logging methods only.
  private ResourceMonitor monitor;

  // Collection interval in seconds.
  private int collectionIntervalSecs;

  // Flag to indicate whether the statistics from network interfaces should
  // be aggregated.
  private boolean aggregateStatistics;

  // Flag to indicate that all network interfaces should be monitored.
  private boolean monitorAllInterfaces;

  // Interface name to interface statistics map.
  private Map<String, InterfaceStatistics> interfaceStatisticsMap =
      Collections.emptyMap();

  // Names of monitored interfaces. It is empty if all interfaces are monitored.
  private Set<String> interfaceNames = Collections.emptySet();

  // Aggregate interface.
  private InterfaceStatistics aggregateInterfaceStatistics;


  /**
   * Builder class for network statistics collectors.
   */
  public static class Builder
  {
    private ResourceMonitor       monitor;
    private int                   osType;
    private int collectionIntervalSecs;
    private boolean               aggregateStatistics;
    private boolean               monitorAllInterfaces;
    private InterfaceStatistics[] interfaceStatistics;
    private String[]              interfaceNames;


    /**
     * Sets the resource monitor.
     *
     * @param monitor   resource monitor
     */
    public void setMonitor(ResourceMonitor monitor)
    {
      this.monitor = monitor;
    }


    /**
     * Sets the OS type
     * @param osType   os type
     */
    public void setOSType(int osType)
    {
      this.osType = osType;
    }


    /**
     * Sets the collection interval in seconds.
     *
     * @param collectionIntervalSecs   collection interval in seconds
     */
    public void setCollectionIntervalSecs(int collectionIntervalSecs)
    {
      this.collectionIntervalSecs = collectionIntervalSecs;
    }


    /**
     * Sets the aggregate statistics flag.
     *
     * @param aggregateStatistics   aggregate statistics value
     */
    public void setAggregateStatistics(boolean aggregateStatistics)
    {
      this.aggregateStatistics = aggregateStatistics;
    }


    /**
     * Sets the monitor all interfaces flag.
     *
     * @param monitorAllInterfaces   monitor all interfaces value
     */
    public void setMonitorAllInterfaces(boolean monitorAllInterfaces)
    {
      this.monitorAllInterfaces = monitorAllInterfaces;
    }


    /**
     * Sets the interface statistics.
     *
     * @param interfaceStatistics    interface statistics
     */
    public void setInterfaceStatistics(
        final InterfaceStatistics... interfaceStatistics)
    {
      this.interfaceStatistics = interfaceStatistics;
    }


    /**
     * Sets the interface names.
     *
     * @param interfaceNames   interface names
     */
    public void setInterfaceNames(String... interfaceNames)
    {
      this.interfaceNames = interfaceNames;
    }


    /**
     * Builds a network statistics collector instance based on the provided
     * parameters.
     *
     * @return a newly constructed network statistics instance.
     */
    public NetStatCollector build()
    {
      final NetStatCollector collector;
      switch (osType)
      {
        case ResourceMonitor.OS_TYPE_SOLARIS:
          collector = new SolarisNetStatCollector();
          break;
        case ResourceMonitor.OS_TYPE_LINUX:
          collector = new LinuxNetStatCollector();
          break;
        case ResourceMonitor.OS_TYPE_AIX:
          collector = new AIXNetStatCollector();
          break;
        case ResourceMonitor.OS_TYPE_HPUX:
          collector = new HPUXNetStatCollector();
          break;
        case ResourceMonitor.OS_TYPE_WINDOWS:
          collector = new WindowsNetStatCollector();
          break;
        default:
          // can't happen
          throw new RuntimeException("invalid os type");
      }

      collector.setMonitor(this.monitor);
      collector.setCollectionIntervalSecs(this.collectionIntervalSecs);
      collector.setAggregateStatistics(this.aggregateStatistics);
      collector.setMonitorAllInterfaces(this.monitorAllInterfaces);
      collector.setInterfaceStatistics(this.interfaceStatistics);
      collector.setInterfaceNames(this.interfaceNames);

      return collector;
    }
  }


  /**
   * Writes a verbose message to the resource monitor log.
   *
   * @param message   message to log
   */
  final void writeVerbose(String message)
  {
    if (this.monitor != null)
    {
      this.monitor.writeVerbose(message);
    }
  }


  /**
   * Writes a log message to the resource monitor log.
   *
   * @param message   message to log
   */
  final void logMessage(String message)
  {
    if (this.monitor != null)
    {
      this.monitor.logMessage(message);
    }
  }


  /**
   * Sets the associated resource monitor.
   *
   * @param monitor   resource monitor
   */
  private void setMonitor(ResourceMonitor monitor)
  {
    this.monitor = monitor;
  }


  /**
   * Returns the collection interval in seconds.
   *
   * @return the collection interval in seconds.
   */
  final int getCollectionIntervalSecs()
  {
    return collectionIntervalSecs;
  }


  /**
   * Sets the collection interval in seconds.
   *
   * @param collectionIntervalSecs   collection interval in seconds.
   */
  private void setCollectionIntervalSecs(int collectionIntervalSecs)
  {
    this.collectionIntervalSecs = collectionIntervalSecs;
  }


  /**
   * Returns true if network interface statistics are aggregated.
   *
   * @return true if network interface statistics are aggregated.
   */
  final boolean isAggregateStatistics()
  {
    return aggregateStatistics;
  }


  /**
   * Sets the aggregate network statistics flag.
   *
   * @param aggregateStatistics   aggregate network interface statistics value
   */
  private void setAggregateStatistics(boolean aggregateStatistics)
  {
    this.aggregateStatistics = aggregateStatistics;
  }


  /**
   * Returns true if all available network interfaces are monitored.
   *
   * @return true if all available network interfaces are monitored.
   */
  final boolean isMonitorAllInterfaces()
  {
    return this.monitorAllInterfaces;
  }


  /**
   * Sets the monitor all network interfaces flag.
   *
   * @param monitorAllInterfaces   monitor all network interfaces flag value
   */
  private void setMonitorAllInterfaces(boolean monitorAllInterfaces)
  {
    this.monitorAllInterfaces = monitorAllInterfaces;
  }


  /**
   * Sets the network interface statistics instances.
   *
   * @param interfaceStatistics   network interface statistics instances
   */
  private void setInterfaceStatistics(
      final InterfaceStatistics... interfaceStatistics)
  {
    assert interfaceStatistics != null;

    if (interfaceStatistics.length == 1 &&
        InterfaceStatistics.AGGREGATE_INTERFACE_NAME.equals(
            interfaceStatistics[0].getName()))
    {
      this.aggregateInterfaceStatistics = interfaceStatistics[0];
    }
    else
    {
      final Map<String, InterfaceStatistics> descriptorMap =
          new HashMap<String, InterfaceStatistics>();

      for (InterfaceStatistics stats : interfaceStatistics)
      {
        descriptorMap.put(stats.getName(), stats);
      }

      this.interfaceStatisticsMap = descriptorMap;
      this.interfaceNames = descriptorMap.keySet();
    }
  }


  /**
   * Returns the interface statistics corresponding to the interface name.
   *
   * @param interfaceName   interface name

   * @return the interface statistics corresponding to the interface name.
   */
  protected InterfaceStatistics getInterfaceStatistics(String interfaceName)
  {
    if (this.aggregateInterfaceStatistics != null)
    {
      return this.aggregateInterfaceStatistics;
    }
    else
    {
      return this.interfaceStatisticsMap.get(interfaceName);
    }
  }


  /**
   * Returns the set of monitored network interfaces. If all network interfaces
   * are monitored, then an empty set is returned.
   *
   * @return the set of monitored network interfaces.
   */
  final Set<String> getInterfaceNames()
  {
    return this.interfaceNames;
  }


  /**
   * Sets the network interfaces to monitor.
   *
   * @param interfaceNames   names of network interfaces
   */
  private void setInterfaceNames(String ... interfaceNames)
  {
    if (interfaceNames != null)
    {
      this.interfaceNames = new HashSet<String>();
      Collections.addAll(this.interfaceNames, interfaceNames);
    }
  }


  /**
   * Completes the collection of network interface statistics.
   */
  public final void finalizeCollection()
  {
    stop();

    if (this.aggregateStatistics)
    {
      this.aggregateInterfaceStatistics.completeCollection();
    }
    else
    {
      for (InterfaceStatistics ifd : this.interfaceStatisticsMap.values())
      {
        ifd.completeCollection();
      }
    }
  }

  /**
   * Initializes the network interface statistics collector.
   */
  public void initialize()
  {
    // do not do anything by default
  }

  /**
   * Returns the set of available network interface names.

   * @return the set of available network interface names.
   */
//  public abstract Set<String> getAvailableInterfaceNames();

  /**
   * Collects network interface statistics.
   *
   * @throws IOException   if an error occurs while capturing network interface
   *                       statistics.
   */
  public abstract void collect() throws IOException;


  /**
   * Stops the collection of network interface statistics.
   */
  void stop()
  {
    // do not do anything by default
  }
}

