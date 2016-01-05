// Copyright (c) 2012-2015 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org
package org.droidmate.command.exploration

import org.droidmate.android_sdk.ApkTestHelper
import org.droidmate.android_sdk.IApk
import org.droidmate.common.logcat.MonitoredInlinedApkFixtureApiLogs
import org.droidmate.configuration.Configuration
import org.droidmate.device_simulation.AndroidDeviceSimulator
import org.droidmate.device_simulation.DeviceSimulation
import org.droidmate.exceptions.DeviceException
import org.droidmate.exceptions.ExceptionSpec
import org.droidmate.exceptions.IExceptionSpec
import org.droidmate.exploration.actions.RunnableExplorationActionWithResult
import org.droidmate.exploration.data_aggregators.IApkExplorationOutput2
import org.droidmate.exploration.device.IDeviceWithReadableLogs
import org.droidmate.exploration.device.RobustDevice
import org.droidmate.init.InitConstants
import org.droidmate.logcat.IApiLogcatMessage
import org.droidmate.misc.Failable
import org.droidmate.misc.ITimeGenerator
import org.droidmate.misc.TimeGenerator
import org.droidmate.test_base.DroidmateGroovyTestCase
import org.droidmate.test_helpers.configuration.ConfigurationForTests
import org.droidmate.test_suite_categories.RequiresDevice
import org.droidmate.test_suite_categories.RequiresSimulator
import org.droidmate.tools.DeviceTools
import org.droidmate.tools.IDeviceTools
import org.droidmate.tools.SingleApkFixture
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4)
public class ExplorationTest extends DroidmateGroovyTestCase
{

  // it fails because the device simulator now always uses UnreliableGuiSnapshot and the code doesn't handle it yet.
  // after it passes 'app has stopped' dialog box, work on providing invalid gui snapshots, within the retry attempts, of course.
  @Category([RequiresSimulator])
  @Test
  void "Runs on simulator"()
  {
    String simulatorSpec = "s1-w1->s1"
    runOnSimulator(simulatorSpec)
  }

  @Category([RequiresSimulator])
  @Test
  void "Supports external app displayed after explored app reset"()
  {
    String simulatorSpec = "s1-w1->chrome"
    runOnSimulator(simulatorSpec)
  }

  @Category([RequiresSimulator])
  @Test
  void "Supports external app displayed after exploration termination"()
  {
    String simulatorSpec = "s1-w1->chrome"
    Configuration cfg = new ConfigurationForTests().setArgs([Configuration.pn_actionsLimit, "1"]).get()
    runOnSimulator(simulatorSpec, [], cfg)
  }

  @Category([RequiresDevice])
  @Test
  void "Collects monitored API calls logs during device exploration"()
  {
    Configuration cfg = new ConfigurationForTests().forDevice().setArgs([
      Configuration.pn_apksNames          , "[$InitConstants.monitored_inlined_apk_fixture_name]",
      Configuration.pn_widgetIndexes      , "[0]",
    ]).get()

    // Configuration cfg = new ConfigurationBuilder().build(args)
    IDeviceTools deviceTools = new DeviceTools(cfg)

    IApk apk = new SingleApkFixture(deviceTools.aapt, cfg)

    Exploration exploration = Exploration.build(cfg)

    IApkExplorationOutput2 out = null
    deviceTools.deviceDeployer.withSetupDevice(0) {IDeviceWithReadableLogs device ->
      deviceTools.apkDeployer.withDeployedApk(device, apk) {IApk deployedApk ->

        // Act
        out = exploration.run(deployedApk, new RobustDevice(device,
          cfg.monitorServerStartTimeout,
          cfg.monitorServerStartQueryInterval,
          cfg.clearPackageRetryAttempts,
          cfg.clearPackageRetryDelay,
          cfg.getValidGuiSnapshotRetryAttempts,
          cfg.getValidGuiSnapshotRetryDelay
        )).result

      }
    }

    MonitoredInlinedApkFixtureApiLogs apiLogs = new MonitoredInlinedApkFixtureApiLogs(extractApiLogsList(out.actRess))
    apiLogs.assertCheck()
  }

  /**
   * <p>
   * Bug: Assertion error in Exploration#tryAssertDeviceHasPackageInstalled
   *
   * </p><p>
   * The call to
   * <pre>org.droidmate.device.IExplorableAndroidDevice#hasPackageInstalled(java.lang.String)</pre>
   * returns false, causing an assert to fail.
   *
   * </p><p>
   * https://hg.st.cs.uni-saarland.de/issues/994
   *
   * </p>
   */
  @Category([RequiresSimulator])
  @Test
  void "Has no bug #994"()
  {
    String simulatorSpec = "s1-w1->s1"
    def failableOut = runOnSimulator(simulatorSpec, [new ExceptionSpec("hasPackageInstalled", null, 1, false, false)])
    assert failableOut.result == null
    assert failableOut.exception != null
  }

  private Failable<IApkExplorationOutput2, DeviceException> runOnSimulator(String simulatorSpec, List<IExceptionSpec> exceptionSpecs = [], Configuration cfg = new ConfigurationForTests().get())
  {
    ITimeGenerator timeGenerator = new TimeGenerator()

    def apk = ApkTestHelper.build("mock_app1")
    def simulator = new AndroidDeviceSimulator(timeGenerator, [apk.packageName], simulatorSpec, exceptionSpecs)
    def simulatedDevice = new RobustDevice(simulator,
      cfg.monitorServerStartTimeout,
      cfg.monitorServerStartQueryInterval,
      cfg.clearPackageRetryAttempts,
      cfg.clearPackageRetryDelay,
      cfg.getValidGuiSnapshotRetryAttempts,
      cfg.getValidGuiSnapshotRetryDelay)

    Exploration exploration = Exploration.build(cfg, timeGenerator)

    // Act
    Failable<IApkExplorationOutput2, DeviceException> failableOut = exploration.run(apk, simulatedDevice)

    if (failableOut.result != null)
    {
      if (!failableOut.result.noException)
        failableOut.exception.printStackTrace()

      assert failableOut.result.noException

      def out2Simulation = new DeviceSimulation(failableOut.result)
      def expectedSimulation = simulator.currentSimulation
      out2Simulation.assertEqual(expectedSimulation)
    }
    return failableOut

  }

  private List<List<IApiLogcatMessage>> extractApiLogsList(List<RunnableExplorationActionWithResult> actions)
  {
    return actions.collect {RunnableExplorationActionWithResult pair -> return pair.result.deviceLogs.apiLogsOrEmpty}
  }
}