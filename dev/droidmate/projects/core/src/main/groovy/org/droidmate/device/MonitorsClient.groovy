// Copyright (c) 2012-2016 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org
package org.droidmate.device

import groovy.util.logging.Slf4j
import org.droidmate.android_sdk.IAdbWrapper
import org.droidmate.exceptions.DeviceException
import org.droidmate.exceptions.DeviceNeedsRebootException
import org.droidmate.exceptions.TcpServerUnreachableException
import org.droidmate.lib_android.MonitorJavaTemplate

@Slf4j
class MonitorsClient implements IMonitorsClient
{

  private final ISerializableTCPClient<String, ArrayList<ArrayList<String>>> monitorTcpClient

  private final String deviceSerialNumber

  private final IAdbWrapper adbWrapper

  MonitorsClient(int socketTimeout, String deviceSerialNumber, IAdbWrapper adbWrapper)
  {
    this.monitorTcpClient = new SerializableTCPClient<>(socketTimeout)
    this.deviceSerialNumber = deviceSerialNumber
    this.adbWrapper = adbWrapper
  }

  @Override
  public boolean anyMonitorIsReachable() throws DeviceNeedsRebootException, DeviceException
  {
    boolean out = ports.any {
      this.isServerReachable(it)
    }
    if (out)
      log.trace("At least one monitor is reachable.")
    else
      log.trace("No monitor is reachable.")
    return out
  }

  private Boolean isServerReachable(int port)
  {
    ArrayList<ArrayList<String>> out
    try
    {
      out = this.monitorTcpClient.queryServer(MonitorJavaTemplate.srvCmd_connCheck, port)
    } catch (TcpServerUnreachableException ignored)
    {
      return false
    }

    ArrayList<String> diagnostics = out.findSingle()
    assert diagnostics.size() >= 2
    String pid = diagnostics[0]
    String packageName = diagnostics[1]
    log.trace("Reached server at port $port. PID: $pid package: $packageName")
    return true
  }

  @Override
  public ArrayList<ArrayList<String>> getCurrentTime() throws DeviceNeedsRebootException, DeviceException
  {
    ArrayList<ArrayList<String>> out = ports.findResult {
      try
      {
        return monitorTcpClient.queryServer(MonitorJavaTemplate.srvCmd_get_time, it)

      } catch (DeviceNeedsRebootException e)
      {
        throw e

      } catch (TcpServerUnreachableException ignored)
      {
        log.trace("Did not reach monitor TCP server at port $it.")
        return null
      }
    }

    if (out == null)
      throw new DeviceException("None of the monitor TCP servers were available.", /* stopFurtherApkExplorations */ true)

    assert out != null
    return out
  }

  @Override
  public ArrayList<ArrayList<String>> getLogs() throws DeviceNeedsRebootException, DeviceException
  {
    Collection<ArrayList<ArrayList<String>>> out = ports.findResults {
      try
      {
        return monitorTcpClient.queryServer(MonitorJavaTemplate.srvCmd_get_logs, it)
      } catch (TcpServerUnreachableException ignored)
      {
        log.trace("Did not reach monitor TCP server at port $it.")
        return null
      }
    }
    assert out != null

    if (out.empty)
    {
      log.trace("None of the monitor TCP servers were available while obtaining API logs.")
      return []
    }

    assert !out.empty
    return (out as Iterable<Iterable>).shallowFlatten()
  }

  @Override
  List<Integer> getPorts()
  {
    return MonitorJavaTemplate.serverPorts.collect {it}
  }

  @Override
  void forwardPorts()
  {
    this.ports.each {this.adbWrapper.forwardPort(this.deviceSerialNumber, it)}
  }
}
