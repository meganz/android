package mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ChatHistoryRetentionConfirmationDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the string 'next' is displayed when the custom option is selected`() {
        composeRule.apply {
            setDialog(currentOption = ChatHistoryRetentionOption.Custom)

            onNodeWithText(R.string.general_next).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the string 'OK' is displayed when an option other than the custom option is selected`() {
        composeRule.apply {
            setDialog(currentOption = ChatHistoryRetentionOption.Day)

            onNodeWithText(R.string.general_ok).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the dismiss request callback is invoked when the cancel button is clicked`() {
        composeRule.apply {
            var isDismissRequestInvoked = false
            setDialog(
                onDismissRequest = { isDismissRequestInvoked = true }
            )

            onNodeWithText(R.string.general_cancel).performClick()

            assertThat(isDismissRequestInvoked).isTrue()
        }
    }

    @Test
    fun `test that the confirm callback is invoked with the correct selected option when the confirm button is clicked`() {
        val currentOption = ChatHistoryRetentionOption.Day

        composeRule.apply {
            var confirmOption: ChatHistoryRetentionOption? = null
            setDialog(
                currentOption = currentOption,
                onConfirmClick = { confirmOption = it }
            )

            onNodeWithText(R.string.general_ok).performClick()

            assertThat(confirmOption).isEqualTo(currentOption)
        }
    }

    private fun ComposeContentTestRule.setDialog(
        currentOption: ChatHistoryRetentionOption = ChatHistoryRetentionOption.Disabled,
        onDismissRequest: () -> Unit = {},
        onConfirmClick: (option: ChatHistoryRetentionOption) -> Unit = {},
    ) {
        setContent {
            ChatHistoryRetentionConfirmationDialog(
                currentOption = currentOption,
                onDismissRequest = onDismissRequest,
                onConfirmClick = onConfirmClick
            )
        }
    }
}
