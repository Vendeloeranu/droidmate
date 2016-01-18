// Copyright (c) 2012-2016 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org
package org.droidmate.exploration.device

import org.droidmate.device.IExplorableAndroidDevice
import org.droidmate.exceptions.DeviceException
import org.droidmate.exceptions.DeviceNeedsRebootException
import org.droidmate.logcat.IApiLogcatMessage
import org.droidmate.logcat.ITimeFormattedLogcatMessage

import java.time.LocalDateTime

/**
 * <p>
 * This class is responsible for reading messages from the device. It can read messages from the device logcat or from the
 * monitor TCP server (for the server source code, see {@code org.droidmate.lib_android.MonitorJavaTemplate.MonitorTCPServer}).
 *
 * </p><p>
 * The messages read are either monitor init messages coming from logcat, method instrumentation messages coming from logcat, or
 * monitored API logs coming from monitor TCP server. In addition, this class maintains the time difference between the device
 * and the host machine, to sync the time logs from the device's clock with the host machine's clock.
 * </p>
 */
class DeviceMessagesReader implements IDeviceMessagesReader
{
  @Deprecated
  private final IInitMsgsReader initMsgsReader
  private final IApiLogsReader  apiLogsReader
  private final IDeviceTimeDiff deviceTimeDiff

  DeviceMessagesReader(IExplorableAndroidDevice device, int monitorServerStartTimeout, int monitorServerStartQueryDelay)
  {
    this.initMsgsReader = new InitMsgsReader(device, monitorServerStartTimeout, monitorServerStartQueryDelay)
    this.apiLogsReader = new ApiLogsReader(device)
    this.deviceTimeDiff = new DeviceTimeDiff(device)
  }

  @Override
  void resetTimeSync()
  {
    this.deviceTimeDiff.reset()

  }

  // Used by old exploration code
  @Deprecated
  @Override
  LocalDateTime readMonitorMessages() throws DeviceException
  {
    return initMsgsReader.readMonitorMessages(deviceTimeDiff)
  }

  // Used by old exploration code
  @Deprecated
  @Override
  List<ITimeFormattedLogcatMessage> readInstrumentationMessages() throws DeviceException
  {
    return initMsgsReader.readInstrumentationMessages(deviceTimeDiff)
  }

  @Override
  List<IApiLogcatMessage> getAndClearCurrentApiLogsFromMonitorTcpServer() throws DeviceNeedsRebootException, DeviceException
  {
    return apiLogsReader.getAndClearCurrentApiLogsFromMonitorTcpServer(deviceTimeDiff)
  }
}
