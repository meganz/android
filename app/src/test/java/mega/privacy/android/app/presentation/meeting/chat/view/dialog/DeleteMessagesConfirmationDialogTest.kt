package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.DeleteMessagesConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteMessagesConfirmationDialogTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that correct dialog is shown when is a meeting`() {
        initComposeRuleContent(messagesCount = 1)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.confirmation_delete_one_message))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.button_cancel)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.context_remove)).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct dialog is shown when is not a meeting`() {
        initComposeRuleContent(messagesCount = 10)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.confirmation_delete_several_messages))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.button_cancel)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.context_remove)).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(messagesCount: Int) {
        composeRule.setContent {
            DeleteMessagesConfirmationDialog(
                messagesCount = messagesCount,
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}