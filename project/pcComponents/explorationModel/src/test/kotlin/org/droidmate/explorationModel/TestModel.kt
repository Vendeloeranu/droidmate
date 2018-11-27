package org.droidmate.explorationModel

import org.droidmate.deviceInterface.exploration.Rectangle
import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.interaction.*
import org.droidmate.explorationModel.retention.StringCreator
import org.droidmate.explorationModel.retention.StringCreator.parsePropertyString
import java.time.LocalDateTime

internal interface TestModel{
	val parentData: UiElementPropertiesI get() = DummyProperties
	val parentWidget: Widget get() = Widget(parentData, null)
	val testWidgetData: UiElementPropertiesI
	val testWidget: Widget get() = Widget(testWidgetData, parentWidget.id)
	val testWidgetDumpString: String
}

typealias TestAction = Interaction
@JvmOverloads fun createTestAction(targetWidget: Widget?=null, oldState: ConcreteId = emptyId,
                                   nextState: ConcreteId = emptyId, actionType:String = "TEST_ACTION",
                                   data: String = ""): TestAction
		= Interaction(actionType = actionType, target = targetWidget, startTimestamp = LocalDateTime.MIN, data = data,
		endTimestamp = LocalDateTime.MIN, successful = true, exception = "test action", prevState = oldState, resState = nextState)


internal class DefaultTestModel: TestModel {
	override val testWidgetData by lazy{
		val properties = StringCreator.createPropertyString(parentWidget,";").split(";")
		val namePropMap = StringCreator.baseAnnotations.parsePropertyString(properties, StringCreator.defaultMap).toMutableMap()
		namePropMap[Widget::text.name] = "text-mock"
		namePropMap[Widget::contentDesc.name] = "description-mock"
		namePropMap[Widget::resourceId.name] = "resourceId-mock"
		namePropMap[Widget::className.name] = "class-mock"
		namePropMap[Widget::packageName.name] = "package-mock"
		namePropMap[Widget::enabled.name] = true
		namePropMap[Widget::clickable.name] = true
		namePropMap[Widget::definedAsVisible.name] = true
		namePropMap[Widget::boundaries.name] = Rectangle(11,136,81,51)
		namePropMap[Widget::visibleAreas.name] = listOf(Rectangle(11,136,81,51))
		namePropMap[Widget::visibleBounds.name] = Rectangle(11,136,81,51)
		UiElementP(namePropMap)
	}

	// per default we don't want to re-generate the widgets on each access, therefore make them persistent values
	override val testWidget: Widget by lazy{ super.testWidget }
	override val parentWidget: Widget by lazy{ super.parentWidget }

	override val testWidgetDumpString = "f875d52c-f9ef-31fb-897e-04434b2ef74e_defcef0e-5a90-38a2-a77d-b62d34e93723;" +
			"class-mock;text-mock;description-mock;disabled;false;11:136:81:51;11:136:81:51;[];true;true;true;disabled;" +
			"0;0;false;false;false;false;package-mock;0;resourceId-mock;false;false;[11:136:81:51];No-xPath"
}