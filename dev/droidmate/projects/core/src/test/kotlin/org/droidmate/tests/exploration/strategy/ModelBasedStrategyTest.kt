// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2016 Konrad Jamrozik
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
// email: jamrozik@st.cs.uni-saarland.de
// web: www.droidmate.org

package org.droidmate.tests.exploration.strategy

import org.droidmate.exploration.actions.WidgetExplorationAction
import org.droidmate.exploration.strategy.Memory
import org.droidmate.exploration.strategy.widget.ModelBased
import org.droidmate.misc.uniqueString
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

/**
 * Unit tests for adaptive exploration strategy
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)
class ModelBasedStrategyTest {

    @Test
    fun selectWidgetTest() {
        // Initialization
        val nrOfActions = 10
        val explArgs = arrayOf("-resetEvery=3", "-actionsLimit=10", "-randomSeed=0")
        val cfg = Auxiliary.createTestConfig(explArgs)
        cfg.actionsLimit = nrOfActions

        val strategy = ModelBased.build(cfg)

        // Mocking
        val guiState = Auxiliary.createGuiStateFromFile()

        // The timer starts here

        val memory = Memory()
        val widgetContext = memory.getWidgetContext(guiState, "ch.bailu.aat")
        val chosenAction = strategy.decide(widgetContext) as WidgetExplorationAction
        assert(chosenAction.getSelectedWidget().uniqueString == "android.view.ViewGroup[0]    java.awt.Rectangle[x=0,y=63,width=263,height=263]")
        //assert(chosenAction.selectedWidget.uniqueString == "android.view.ViewGroup    java.awt.Rectangle[x=789,y=63,width=263,height=263]")
    }

}