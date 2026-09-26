package com.apexus.storagelens.ui

import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.selection.Selection
import com.apexus.storagelens.domain.selection.TriState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TriStateSelectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val items = listOf("/r/1.log", "/r/2.log", "/r/3.log").map {
        CleanupCandidate(it, it.substringAfterLast('/'), 10, 0, false, CleanupCategory.LOGS, true)
    }

    private fun hasState(state: ToggleableState) = SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)

    @Test
    fun categoryCheckboxCyclesFromIndeterminateToAllToNone() {
        var selection by mutableStateOf(setOf("/r/1.log"))
        compose.setContent {
            val state = when (Selection.stateOf(items, selection)) {
                TriState.ON -> ToggleableState.On
                TriState.OFF -> ToggleableState.Off
                TriState.INDETERMINATE -> ToggleableState.Indeterminate
            }
            TriStateCheckbox(state = state, onClick = { selection = Selection.toggle(items, selection) }, modifier = Modifier.testTag("category"))
        }

        compose.onNodeWithTag("category").assert(isToggleable()).assert(hasState(ToggleableState.Indeterminate))
        compose.onNodeWithTag("category").performClick().assert(hasState(ToggleableState.On))
        compose.onNodeWithTag("category").performClick().assert(hasState(ToggleableState.Off))
    }
}
