// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2018. Saarland University
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// Current Maintainers:
// Nataniel Borges Jr. <nataniel dot borges at cispa dot saarland>
// Jenny Hotzkow <jenny dot hotzkow at cispa dot saarland>
//
// Former Maintainers:
// Konrad Jamrozik <jamrozik at st dot cs dot uni-saarland dot de>
//
// web: www.droidmate.org
package org.droidmate.command

import org.droidmate.configuration.ConfigProperties.Report.includePlots
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.report.AggregateStats
import org.droidmate.report.OutputDir
import org.droidmate.report.Summary
import org.droidmate.report.apk.*

class ReportCommand : DroidmateCommand() {
	override fun execute(cfg: ConfigurationWrapper) {
		val out = OutputDir(cfg.reportInputDirPath).explorationOutput2
		val data = out
		val includePlots = cfg[includePlots]

		AggregateStats().write(cfg.droidmateOutputReportDirPath, data)
		Summary().write(cfg.droidmateOutputReportDirPath, data)
		ApkViewsFile().write(cfg.droidmateOutputReportDirPath, data)
		ApiCount(includePlots).write(cfg.droidmateOutputReportDirPath, data)
		ClickFrequency(includePlots).write(cfg.droidmateOutputReportDirPath, data)
		WidgetSeenClickedCount(includePlots).write(cfg.droidmateOutputReportDirPath, data)
		ApiActionTrace().write(cfg.droidmateOutputReportDirPath, data)
		ActivitySeenSummary().write(cfg.droidmateOutputReportDirPath, data)
		ActionTrace().write(cfg.droidmateOutputReportDirPath, data)
		WidgetApiTrace().write(cfg.droidmateOutputReportDirPath, data)
		EffectiveActions().write(cfg.droidmateOutputReportDirPath, data)
	}
}
