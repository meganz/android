package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.textfields.GENERIC_TEXT_FIELD_ERROR_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputDialogTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that error text is not shown when error is passed to input dialog`() {
        composeRule.setContent {
            InputDialog(
                text = "text",
                title = "Dialog title",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("text").assertExists()
        composeRule.onNodeWithText("Dialog title").assertExists()
        composeRule.onNodeWithTag(GENERIC_TEXT_FIELD_ERROR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that error text is shown when error is passed to input dialog`() {
        composeRule.setContent {
            InputDialog(
                text = "text",
                title = "Dialog title",
                confirmButtonText = "confirm",
                cancelButtonText = "cancel",
                onConfirm = {},
                onDismiss = {},
                error = "error text",
            )
        }
        composeRule.onNodeWithText("text").assertExists()
        composeRule.onNodeWithText("Dialog title").assertExists()
        composeRule.onNodeWithTag(GENERIC_TEXT_FIELD_ERROR_TAG).assertExists()
    }
}