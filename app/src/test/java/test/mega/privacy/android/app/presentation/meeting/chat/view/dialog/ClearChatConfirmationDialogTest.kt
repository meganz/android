package test.mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClearChatConfirmationDialogTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that correct dialog is shown when is a meeting`() {
        initComposeRuleContent(isMeeting = true)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.meetings_clear_history_confirmation_dialog_title))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.meetings_clear_history_confirmation_dialog_message))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.button_cancel)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.general_clear)).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct dialog is shown when is not a meeting`() {
        initComposeRuleContent(isMeeting = false)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.title_properties_chat_clear))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.confirmation_clear_chat_history))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.button_cancel)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.general_clear)).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(isMeeting: Boolean) {
        composeRule.setContent {
            ClearChatConfirmationDialog(
                isMeeting = isMeeting,
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}