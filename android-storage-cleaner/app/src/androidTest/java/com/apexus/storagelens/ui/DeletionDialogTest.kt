package com.apexus.storagelens.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.model.RiskLevel
import com.apexus.storagelens.ui.components.DeleteConfirmDialog
import com.apexus.storagelens.ui.components.PendingDeletion
import com.apexus.storagelens.ui.components.PendingItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeletionDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun riskyItemsAreListedAndConfirmationIsExplicit() {
        var confirmed = 0
        var dismissed = 0
        val pending = PendingDeletion(
            items = listOf(
                PendingItem("/s/a.log", "a.log", 1024, RiskLevel.LOW, permanent = true),
                PendingItem("/s/WhatsApp Video", "WhatsApp Video", 5_000_000, RiskLevel.HIGH, permanent = false),
            ),
            trashEnabled = true,
        )
        compose.setContent {
            DeleteConfirmDialog(pending, onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        }

        compose.onNodeWithText("WhatsApp Video").assertIsDisplayed()
        compose.onNodeWithText("a.log").assertDoesNotExist()

        compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        assertEquals(1, dismissed)
        assertEquals(0, confirmed)

        compose.onNodeWithText(context.getString(R.string.action_delete)).performClick()
        assertEquals(1, confirmed)
    }
}
