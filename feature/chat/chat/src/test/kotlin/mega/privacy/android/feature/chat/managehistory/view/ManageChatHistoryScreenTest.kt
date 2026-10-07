package mega.privacy.android.feature.chat.managehistory.view

import mega.privacy.android.feature.chat.managehistory.model.ChatHistoryRetention
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class ManageChatHistoryScreenTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the chat title is shown when the conversation is not a meeting`() {
        setScreen(isMeeting = false)

        composeRule.onNodeWithText(string(sharedR.string.chat_manage_history_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the meeting title is shown when the conversation is a meeting`() {
        setScreen(isMeeting = true)

        composeRule.onNodeWithText(string(sharedR.string.chat_manage_history_meeting_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the retention period row is hidden when message clearing is off`() {
        setScreen(retention = ChatHistoryRetention.Disabled)

        composeRule.onNodeWithTag(RETENTION_PERIOD_ROW_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that the retention period row is shown when message clearing is on`() {
        setScreen(retention = ChatHistoryRetention.OneDay)

        composeRule.onNodeWithTag(RETENTION_PERIOD_ROW_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onSetRetentionTime turns message clearing off when the toggle is switched off`() {
        val onSetRetention = mock<(ChatHistoryRetention) -> Unit>()
        setScreen(retention = ChatHistoryRetention.OneDay, onSetRetention = onSetRetention)

        composeRule.onNodeWithTag(HISTORY_CLEARING_TOGGLE_TAG).performClick()

        verify(onSetRetention).invoke(ChatHistoryRetention.Disabled)
    }

    @Test
    fun `test that the retention dialog opens when the toggle is switched on`() {
        setScreen(retention = ChatHistoryRetention.Disabled)

        composeRule.onNodeWithTag(HISTORY_CLEARING_TOGGLE_TAG).performClick()

        composeRule.onAllNodesWithTag(CHAT_HISTORY_RETENTION_DIALOG_TAG).onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `test that the retention dialog opens when the retention period row is clicked`() {
        setScreen(retention = ChatHistoryRetention.OneDay)

        composeRule.onNodeWithTag(RETENTION_PERIOD_ROW_TAG)
            .performClick()

        composeRule.onAllNodesWithTag(CHAT_HISTORY_RETENTION_DIALOG_TAG).onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `test that the clear history dialog opens when the clear history row is clicked`() {
        setScreen()

        composeRule.onNodeWithTag(CLEAR_HISTORY_ROW_TAG).performClick()

        composeRule.onNodeWithTag(CLEAR_CHAT_HISTORY_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onClearHistory is invoked when the clear history dialog is confirmed`() {
        val onClearHistory = mock<() -> Unit>()
        setScreen(onClearHistory = onClearHistory)

        composeRule.onNodeWithTag(CLEAR_HISTORY_ROW_TAG).performClick()
        composeRule.onNodeWithText(string(sharedR.string.general_clear)).performClick()

        verify(onClearHistory).invoke()
    }

    @Test
    fun `test that onSetRetentionTime applies the period picked in the retention dialog`() {
        val onSetRetention = mock<(ChatHistoryRetention) -> Unit>()
        setScreen(retention = ChatHistoryRetention.Disabled, onSetRetention = onSetRetention)

        composeRule.onNodeWithTag(HISTORY_CLEARING_TOGGLE_TAG).performClick()
        composeRule.onNodeWithText(
            string(sharedR.string.chat_manage_history_retention_option_one_week)
        ).performClick()
        composeRule.onNodeWithText(string(sharedR.string.general_ok_only)).performClick()

        verify(onSetRetention).invoke(ChatHistoryRetention.OneWeek)
    }

    @Test
    fun `test that the custom period picker opens when the custom option is picked`() {
        setScreen(retention = ChatHistoryRetention.Disabled)

        composeRule.onNodeWithTag(HISTORY_CLEARING_TOGGLE_TAG).performClick()
        composeRule.onNodeWithText(
            string(sharedR.string.chat_manage_history_retention_option_custom)
        ).performClick()
        composeRule.onNodeWithText(
            string(sharedR.string.chat_manage_history_retention_next_button)
        ).performClick()

        composeRule.onNodeWithTag(CUSTOM_RETENTION_TIME_PICKER_TAG).assertIsDisplayed()
    }

    private fun setScreen(
        isMeeting: Boolean = false,
        retention: ChatHistoryRetention = ChatHistoryRetention.OneDay,
        onSetRetention: (ChatHistoryRetention) -> Unit = {},
        onClearHistory: () -> Unit = {},
        onBackClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                ManageChatHistoryScreen(
                    isMeeting = isMeeting,
                    retention = retention,
                    onBackClick = onBackClick,
                    onSetRetention = onSetRetention,
                    onClearHistory = onClearHistory,
                )
            }
        }
    }

    private fun string(id: Int) = composeRule.activity.getString(id)

    private companion object {
        const val ONE_DAY = 86_400L
        const val ONE_WEEK = 604_800L
    }
}
