package mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.shared.original.core.ui.controls.dialogs.CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText

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
                currentRetentionTime = 86400L,
                onConfirmClick = { confirmOption = it }
            )

            onNodeWithText(R.string.general_ok).performClick()

            assertThat(confirmOption).isEqualTo(currentOption)
        }
    }

    @Test
    fun `test that the selected chat history retention time should be set with the disabled option`() {
        composeRule.apply {
            setDialog(currentRetentionTime = DISABLED_RETENTION_TIME)

            onNode(
                isSelectable().and(
                    hasAnySibling(
                        hasText(
                            fromId(
                                ChatHistoryRetentionOption.Disabled.stringId
                            )
                        )
                    )
                ),
                useUnmergedTree = true
            ).assertIsSelected()
        }
    }

    @Test
    fun `test that the selected chat history retention time should be set with the 'One Day' option`() {
        composeRule.apply {
            setDialog(currentRetentionTime = SECONDS_IN_DAY.toLong())

            onNode(
                isSelectable().and(
                    hasAnySibling(
                        hasText(
                            fromId(
                                ChatHistoryRetentionOption.OneDay.stringId
                            )
                        )
                    )
                ),
                useUnmergedTree = true
            ).assertIsSelected()
        }
    }

    @Test
    fun `test that the selected chat history retention time should be set with the 'One Week' option`() {
        composeRule.apply {
            setDialog(currentRetentionTime = SECONDS_IN_WEEK.toLong())

            onNode(
                isSelectable().and(
                    hasAnySibling(
                        hasText(
                            fromId(
                                ChatHistoryRetentionOption.OneWeek.stringId
                            )
                        )
                    )
                ),
                useUnmergedTree = true
            ).assertIsSelected()
        }
    }

    @Test
    fun `test that the selected chat history retention time should be set with the 'One Month' option`() {
        composeRule.apply {
            setDialog(currentRetentionTime = SECONDS_IN_MONTH_30.toLong())

            onNode(
                isSelectable().and(
                    hasAnySibling(
                        hasText(
                            fromId(
                                ChatHistoryRetentionOption.OneMonth.stringId
                            )
                        )
                    )
                ),
                useUnmergedTree = true
            ).assertIsSelected()
        }
    }

    @Test
    fun `test that the selected chat history retention time should be set with the custom option`() {
        composeRule.apply {
            setDialog(
                currentRetentionTime = 100L
            )

            onNode(
                isSelectable().and(
                    hasAnySibling(
                        hasText(
                            fromId(
                                ChatHistoryRetentionOption.Custom.stringId
                            )
                        )
                    )
                ),
                useUnmergedTree = true
            ).assertIsSelected()
        }
    }

    @Test
    fun `test that 'Next' is displayed when showing the retention time is custom`() {
        composeRule.apply {
            setDialog(currentRetentionTime = 100L)

            onNodeWithText(R.string.general_next).assertIsDisplayed()
        }
    }

    @Test
    fun `test that 'OK' is displayed when showing the retention time is not custom`() {
        composeRule.apply {
            setDialog(currentRetentionTime = DISABLED_RETENTION_TIME)

            onNodeWithText(R.string.general_ok).assertIsDisplayed()
        }
    }

    @Test
    fun `test that confirm button is enabled`() {
        composeRule.apply {
            setDialog(currentRetentionTime = SECONDS_IN_DAY.toLong())

            onNodeWithTag(CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG).assertIsEnabled()
        }
    }

    @Test
    fun `test that confirm button is disabled`() {
        composeRule.apply {
            setDialog(currentRetentionTime = DISABLED_RETENTION_TIME)

            onNodeWithTag(CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG).assertIsNotEnabled()
        }
    }

    private fun ComposeContentTestRule.setDialog(
        currentRetentionTime: Long = 0L,
        onDismissRequest: () -> Unit = {},
        onConfirmClick: (option: ChatHistoryRetentionOption) -> Unit = {},
    ) {
        setContent {
            ChatHistoryRetentionConfirmationDialog(
                currentRetentionTime = currentRetentionTime,
                onDismissRequest = onDismissRequest,
                onConfirmClick = onConfirmClick
            )
        }
    }
}
