// Copyright (c) 2012-2016 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org

package org.droidmate.device

import org.droidmate.android_sdk.IApk
import org.droidmate.exceptions.DeviceException
import org.droidmate.exceptions.DeviceNeedsRebootException

import java.nio.file.Path

public interface IDeployableAndroidDevice
{
  void pushFile(Path sourceFile) throws DeviceException

  void pushFile(Path sourceFile, String targetFileName, String targetDirectory) throws DeviceException

  void pullFile(String sourceFileName, String targetFileName) throws DeviceException

  void removeFile(Path file) throws DeviceException

  void installApk(Path apk) throws DeviceException

  void installApk(IApk apk) throws DeviceException

  void uninstallApk(String apkPackageName, boolean ignoreFailure) throws DeviceException

  void closeMonitorServers() throws DeviceException

  void clearPackage(String apkPackageName) throws DeviceException

  void clearLogcat() throws DeviceException

  void closeConnection() throws DeviceNeedsRebootException, DeviceException

  void reboot() throws DeviceException

  boolean isAvailable() throws DeviceException

  boolean uiaDaemonClientThreadIsAlive()

  void setupConnection() throws DeviceException

  void removeLogcatLogFile() throws DeviceException

  void pullLogcatLogFile() throws DeviceException

  void initModel() throws DeviceException
}
