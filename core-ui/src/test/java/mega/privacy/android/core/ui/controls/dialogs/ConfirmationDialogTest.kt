package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.OPTION1_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.OPTION2_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ConfirmationDialogTest {

    @get:Rule
    var composeRule = createComposeRule()


    @Test
    fun `test that buttonOption1Text is set to option 1 button`() {
        createDialog()
        composeRule.onNodeWithTag(OPTION1_TAG).assertTextEquals(ACTION1_TEXT)
    }

    @Test
    fun `test that buttonOption2Text is set to option 2 button`() {
        createDialog()
        composeRule.onNodeWithTag(OPTION2_TAG).assertTextEquals(ACTION2_TEXT)
    }

    @Test
    fun `test that cancelButtonText is set to cancel button`() {
        createDialog()
        composeRule.onNodeWithTag(CANCEL_TAG).assertTextEquals(CANCEL_TEXT)
    }

    @Test
    fun `test that onOption1 is fired when option 1 button is pressed`() {
        val event = mock<() -> Unit>()
        createDialog(onOption1 = event)
        composeRule.onNodeWithTag(OPTION1_TAG).performClick()
        verify(event).invoke()
    }

    @Test
    fun `test that onOption2 is fired when option 2 button is pressed`() {
        val event = mock<() -> Unit>()
        createDialog(onOption2 = event)
        composeRule.onNodeWithTag(OPTION2_TAG).performClick()
        verify(event).invoke()
    }

    @Test
    fun `test that onCancel is fired when cancel button is pressed`() {
        val event = mock<() -> Unit>()
        createDialog(onCancel = event)
        composeRule.onNodeWithTag(CANCEL_TAG).performClick()
        verify(event).invoke()
    }

    private fun createDialog(
        onOption1: () -> Unit = {},
        onOption2: () -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeRule.setContent {
            ConfirmationDialog(
                title = "title",
                text = "text",
                buttonOption1Text = ACTION1_TEXT,
                buttonOption2Text = ACTION2_TEXT,
                cancelButtonText = CANCEL_TEXT,
                onOption1 = onOption1,
                onOption2 = onOption2,
                onCancel = onCancel,
                onDismiss = {}
            )
        }
    }

    companion object {
        private const val ACTION1_TEXT = "Action 1"
        private const val ACTION2_TEXT = "Action 2"
        private const val CANCEL_TEXT = "Cancel"
    }
}