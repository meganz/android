package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.dialogs.internal.BaseMegaAlertDialog
import mega.privacy.android.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.CONFIRM_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.TITLE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MegaAlertDialogTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that if title is not set then title is not shown`() {
        composeRule.setContent {
            MegaAlertDialog(
                text = "text",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag(TITLE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that if title is set then title is shown`() {
        composeRule.setContent {
            BaseMegaAlertDialog(
                text = "text",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = {},
                onDismiss = {},
                title = "Title"
            )
        }
        composeRule.onNodeWithTag(TITLE_TAG).assertExists()
    }

    @Test
    fun `test that if cancel button is not set then cancel button is not shown`() {
        composeRule.setContent {
            MegaAlertDialog(
                text = "text",
                confirmButtonText = "confirm",
                cancelButtonText = null,
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag(CANCEL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that if cancel button is set then cancel button is shown`() {
        composeRule.setContent {
            MegaAlertDialog(
                text = "text",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag(CANCEL_TAG).assertExists()
    }

    @Test
    fun `test that on dismiss is fired when cancel button is pressed`() {
        val onDismiss = mock<() -> Unit>()
        composeRule.setContent {
            MegaAlertDialog(
                text = "text",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = {},
                onDismiss = onDismiss,
            )
        }
        composeRule.onNodeWithTag(CANCEL_TAG).performClick()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that on onConfirm is fired when confirm button is pressed`() {
        val onConfirm = mock<() -> Unit>()
        composeRule.setContent {
            MegaAlertDialog(
                text = "text",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = onConfirm,
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag(CONFIRM_TAG).performClick()
        verify(onConfirm).invoke()
    }
}