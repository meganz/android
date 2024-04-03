package mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog

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
        val currentOption = ChatHistoryRetentionOption.OneDay

        composeRule.apply {
            var confirmOption: ChatHistoryRetentionOption? = null
            setDialog(
                selectedOption = currentOption,
                onConfirmClick = { confirmOption = it }
            )

            onNodeWithText(R.string.general_ok).performClick()

            assertThat(confirmOption).isEqualTo(currentOption)
        }
    }

    @Test
    fun `test that the option selected callback is invoked when an option is chosen`() {
        val option = ChatHistoryRetentionOption.OneDay

        composeRule.apply {
            var selectedOption: ChatHistoryRetentionOption? = null
            setDialog(
                onOptionSelected = { selectedOption = it }
            )

            onNodeWithText(option.stringId).performClick()

            assertThat(selectedOption).isEqualTo(option)
        }
    }

    private fun ComposeContentTestRule.setDialog(
        selectedOption: ChatHistoryRetentionOption = ChatHistoryRetentionOption.Disabled,
        onOptionSelected: (option: ChatHistoryRetentionOption) -> Unit = {},
        confirmButtonText: String = "OK",
        isConfirmButtonEnable: () -> Boolean = { true },
        onDismissRequest: () -> Unit = {},
        onConfirmClick: (option: ChatHistoryRetentionOption) -> Unit = {},
    ) {
        setContent {
            ChatHistoryRetentionConfirmationDialog(
                selectedOption = selectedOption,
                onOptionSelected = onOptionSelected,
                confirmButtonText = confirmButtonText,
                isConfirmButtonEnable = isConfirmButtonEnable,
                onDismissRequest = onDismissRequest,
                onConfirmClick = onConfirmClick
            )
        }
    }
}
