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

package org.droidmate.report.apk

import kotlinx.coroutines.runBlocking
import org.droidmate.device.logcat.ApiLogcatMessage
import org.droidmate.deviceInterface.exploration.isClick
import org.droidmate.explorationModel.interaction.Interaction
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.exploration.ExplorationContext
import java.nio.file.Files
import java.nio.file.Path

class WidgetApiTrace(private val fileName: String = "widget_api_trace.txt") : ApkReport() {
	override fun safeWriteApkReport(data: ExplorationContext, apkReportDir: Path, resourceDir: Path) {
		val sb = StringBuilder()
		val header = "actionNr\ttext\tapi\tuniqueStr\taction\n"
		sb.append(header)

		data.explorationTrace.getActions().forEachIndexed { actionNr, record ->
			if (record.actionType.isClick()) {
				val text = runBlocking { data.getState(record.resState)?.let { getActionWidget(record, it) } }
				val logs = record.deviceLogs
				val widget = record.targetWidget

				logs.forEach { ApiLogcatMessage.from(it).let{ apiLog ->
					sb.appendln("$actionNr\t$text\t${apiLog.objectClass}->${apiLog.methodName}\t$widget\t${apiLog.uniqueString}")
				}}
			}
		}

		val reportFile = apkReportDir.resolve(fileName)
		Files.write(reportFile, sb.toString().toByteArray())
	}

	private fun getActionWidget(actionResult: Interaction, state: State): Widget? {
		return if (actionResult.actionType.isClick()) {

			getWidgetWithTextFromAction(actionResult.targetWidget!!, state)
		} else
			null
	}

	private fun getWidgetWithTextFromAction(widget: Widget, state: State): Widget {
		// If has Text
		if (widget.text.isNotEmpty())
			return widget

		val children = state.widgets
				.filter { p -> p.parentId == widget.id }

		// If doesn't have any children
		if (children.isEmpty()) {
			return widget
		}

		val childrenWithText = children.filter { p -> p.text.isNotEmpty() }

		return when {
		// If a single children have text
			childrenWithText.size == 1 -> childrenWithText.first()

		// Single child, drill down
			children.size == 1 -> getWidgetWithTextFromAction(children.first(), state)

		// Multiple children, skip
			else -> widget
		}
	}
}