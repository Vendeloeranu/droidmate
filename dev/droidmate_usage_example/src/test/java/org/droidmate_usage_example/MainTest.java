// Copyright (c) 2012-2016 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org
package org.droidmate_usage_example;

import org.droidmate.android_sdk.IApk;
import org.droidmate.command.ExploreCommand;
import org.droidmate.common.exploration.datatypes.Widget;
import org.droidmate.configuration.Configuration;
import org.droidmate.device.datatypes.IDeviceGuiSnapshot;
import org.droidmate.exceptions.DeviceException;
import org.droidmate.exploration.actions.ExplorationAction;
import org.droidmate.exploration.actions.IExplorationActionRunResult;
import org.droidmate.exploration.actions.RunnableExplorationActionWithResult;
import org.droidmate.exploration.actions.WidgetExplorationAction;
import org.droidmate.exploration.data_aggregators.ExplorationOutput2;
import org.droidmate.exploration.data_aggregators.IApkExplorationOutput2;
import org.droidmate.exploration.strategy.IExplorationStrategyProvider;
import org.droidmate.frontend.DroidmateFrontend;
import org.droidmate.frontend.ICommandProvider;
import org.droidmate.logcat.IApiLogcatMessage;
import org.droidmate.report.OutputDir;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class contains tests showing example use cases of DroidMate API. To understand better how to work with DroidMate API, 
 * please explore the source code of the DroidMate classes called by the examples here. For where to find the sources and how to 
 * navigate them, please read <pre>https://github.com/konrad-jamrozik/droidmate/blob/master/README.md</pre>
 */
public class MainTest
{
  /**
   * This test shows how to access DroidMate API with default settings. If you run it right off the bat, DroidMate will inform 
   * you into which dir to put apks. If you put apks there, DroidMate will inform you why and how you should inline them.
   *
   * In any case, please read the README.md mentioned in {@link MainTest}
   */
  @Test
  public void explore_with_default_settings()
  {
    call_main_then_assert_exit_status_is_0(new String[]{});
  }

  /**
   * This test shows how to access various part of the data structure serialized by DroidMate to file system, containing all the
   * results from the exploration (minus the diagnostic logs). Note that the methods used are not exhaustive. Explore the sources
   * of the used types to find out more.
   *
   * To get any meaningful output to stdout from this test, first run DroidMate on an inlined apk. Confused? Please read the doc 
   * mentioned in {@link MainTest}
   */
  @Test
  public void deserialize_and_work_with_exploration_result()
  {
    final ExplorationOutput2 output = new OutputDir(Paths.get(Configuration.defaultDroidmateOutputDir)).read();
    output.forEach(this::work_with_single_apk_exploration_output);
  }

  /**
   * Please see comment of {@link #deserialize_and_work_with_exploration_result}
   */
  private void work_with_single_apk_exploration_output(IApkExplorationOutput2 apkOut)
  {
    final IApk apk = apkOut.getApk();
    if (apkOut.getNoException())
    {

      int actionCounter = 0;
      for (RunnableExplorationActionWithResult actionWithResult : apkOut.getActRess())
      {
        actionCounter++;

        final ExplorationAction action = actionWithResult.getAction().getBase();
        System.out.println("Action " + actionCounter + " is of type " + action.getClass().getSimpleName());

        if (action instanceof WidgetExplorationAction)
        {
          WidgetExplorationAction widgetAction = (WidgetExplorationAction) action;
          Widget w = widgetAction.getWidget();
          System.out.println("Text of acted-upon widget of given action: " + w.getText());
        }

        final IExplorationActionRunResult result = actionWithResult.getResult();
        final IDeviceGuiSnapshot guiSnapshot = result.getGuiSnapshot();

        System.out.println("Action " + actionCounter + " resulted in a screen containing following actionable widgets: ");
        for (Widget widget : guiSnapshot.getGuiState().getActionableWidgets())
          System.out.println("Widget of class " + widget.getClassName() + " with bounds: " + widget.getBoundsString());

        final List<IApiLogcatMessage> apiLogs = result.getDeviceLogs().getApiLogsOrEmpty();
        System.out.println("Action " + actionCounter + " resulted in following calls to monitored Android SDK's APIs being made:");
        for (IApiLogcatMessage apiLog : apiLogs)
          System.out.println(apiLog.getObjectClass() + "." + apiLog.getMethodName());
      }

      // Convenience method for accessing GUI snapshots resulting from all actions.
      @SuppressWarnings("unused")
      final List<IDeviceGuiSnapshot> guiSnapshots = apkOut.getGuiSnapshots();

      // Convenience method for accessing API logs resulting from all actions.
      @SuppressWarnings("unused")
      final List<List<IApiLogcatMessage>> apiLogs = apkOut.getApiLogs();
    } else
    {
      @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      final DeviceException exception = apkOut.getException();
      System.out.println("Exploration of " + apk.getFileName() + " resulted in exception: " + exception.toString());
    }
  }

  /**
   * This test will make DroidMate inline all the apks present in the default input directory.
   */
  @Test
  public void inline_apks()
  {
    call_main_then_assert_exit_status_is_0(new String[]{Configuration.pn_inline});
  }

  /**
   * This test shows some common settings you would wish to override when running DroidMate. In any case, you can always consult
   *source of Configuration class for more settings.
   */
  @Test
  public void explore_with_common_settings_changed()
  {
    List<String> args = new ArrayList<>();
    
    // Notation explanation: "pn" means "parameter name"
    
    Collections.addAll(args, Configuration.pn_apksDir, "apks/inlined");
    Collections.addAll(args, Configuration.pn_timeLimit, "20");
    Collections.addAll(args, Configuration.pn_resetEveryNthExplorationForward, "5");
    Collections.addAll(args, Configuration.pn_randomSeed, "43");
    
    call_main_then_assert_exit_status_is_0(args.toArray(new String[args.size()]));
  }

  /**
   * This test shows how to make DroidMate run with your custom exploration strategy and termination criterion. Right now there
   * is no base ExplorationStrategy from which you can inherit and the ITerminationCriterion interface is a bit rough. To help
   * yourself, see how the actual DroidMate exploration strategy is implemented an its components 
   * <a href="https://github.com/konrad-jamrozik/droidmate/blob/ffd6da96e16978418d34b7f186699423d548e1f3/dev/droidmate/projects/core/src/main/groovy/org/droidmate/exploration/strategy/ExplorationStrategy.groovy#L90">on GitHub</a>
   */
  @Test
  public void explore_with_custom_exploration_strategy_and_termination_criterion()
  {
    final IExplorationStrategyProvider strategyProvider = () -> new ExampleExplorationStrategy(new ExampleTerminationCriterion());
    final ICommandProvider commandProvider = cfg -> ExploreCommand.build(cfg, strategyProvider);
    call_main_then_assert_exit_status_is_0(new String[]{}, commandProvider);
  }


  private void call_main_then_assert_exit_status_is_0(String[] args)
  {
    // null commandProvider means "do not override DroidMate command (and thus: any components) with custom implementation" 
    final ICommandProvider commandProvider = null;
    call_main_then_assert_exit_status_is_0(args, commandProvider);
  }

  private void call_main_then_assert_exit_status_is_0(String[] args, ICommandProvider commandProvider)
  {
    int exitStatus = DroidmateFrontend.main(args, commandProvider);
    Assert.assertEquals(0, exitStatus);
  }


}