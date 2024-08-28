package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.model.ChatRoomUiState
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog.CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.shared.original.core.ui.controls.dialogs.CONFIRMATION_DIALOG_CANCEL_BUTTON_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CONFIRM_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ManageChatHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that meeting history title is shown when chat room is a meeting`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = ChatRoomUiState(isMeeting = true)
                )
            )

            onNodeWithText(R.string.meetings_manage_history_view_title).assertIsDisplayed()
        }
    }

    @Test
    fun `test that chat history title is shown when chat room is not a meeting`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = ChatRoomUiState()
                )
            )

            onNodeWithText(R.string.title_properties_manage_chat).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the screen is navigated up when navigation icon is clicked`() {
        with(composeRule) {
            var isNavigatedUp = false
            setScreen(
                onNavigateUp = { isNavigatedUp = true }
            )

            onNodeWithTag("appbar:button_back").performClick()

            assertThat(isNavigatedUp).isTrue()
        }
    }

    @Test
    fun `test that the custom retention time picker is shown`() {
        with(composeRule) {
            setScreen()

            selectARetentionTimeOption(ChatHistoryRetentionOption.Custom)

            onNodeWithTag(CUSTOM_TIME_PICKER_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the custom retention time picker is not shown`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CUSTOM_TIME_PICKER_TAG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that 'clear meeting history' text is shown when chat room is a meeting`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = ChatRoomUiState(isMeeting = true)
                )
            )

            onNodeWithText(R.string.meetings_manage_history_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that 'clear chat history' text is shown when chat room is not a meeting`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = ChatRoomUiState()
                )
            )

            onNodeWithText(R.string.title_properties_clear_chat_history).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the clear chat confirmation is shown`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = ChatRoomUiState()
                )
            )

            onNodeWithTag(CLEAR_HISTORY_OPTION_TAG).performClick()

            onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the clear chat confirmation is dismissed after the user confirms it`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = ChatRoomUiState()
                )
            )

            onNodeWithTag(CLEAR_HISTORY_OPTION_TAG).performClick()
            onNodeWithTag(CONFIRM_TAG).performClick()

            onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that the chat history retention confirmation is shown`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).performClick()

            onNodeWithTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the chat history retention confirmation is not shown`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that correct subtitle is shown when retention time is blank`() {
        with(composeRule) {
            setScreen()

            onNodeWithText(R.string.subtitle_properties_history_retention).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct subtitle is shown when retention time is not blank`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    retentionTime = 3600L
                )
            )

            onNodeWithText(R.string.subtitle_properties_manage_chat).assertIsDisplayed()
        }
    }

    @Test
    fun `test that retention is shown when retention time is not blank`() {
        val retentionTime = 3600L

        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    retentionTime = retentionTime
                )
            )

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val expected = getRetentionTimeString(context, retentionTime)
            onNodeWithText(expected.orEmpty()).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the retention time is updated to seconds in day when the confirmed option is one day`() {
        with(composeRule) {
            var chosenTime: Long? = null
            setScreen(
                onSetChatRetentionTime = { chosenTime = it }
            )

            selectARetentionTimeOption(ChatHistoryRetentionOption.OneDay)

            assertThat(chosenTime).isEqualTo(SECONDS_IN_DAY)
        }
    }

    @Test
    fun `test that the retention time is updated to seconds in week when the confirmed option is one week`() {
        with(composeRule) {
            var chosenTime: Long? = null
            setScreen(
                onSetChatRetentionTime = { chosenTime = it }
            )

            selectARetentionTimeOption(ChatHistoryRetentionOption.OneWeek)

            assertThat(chosenTime).isEqualTo(SECONDS_IN_WEEK)
        }
    }

    @Test
    fun `test that the retention time is updated to seconds in month (30 days) when the confirmed option is one month`() {
        with(composeRule) {
            var chosenTime: Long? = null
            setScreen(
                onSetChatRetentionTime = { chosenTime = it }
            )

            selectARetentionTimeOption(ChatHistoryRetentionOption.OneMonth)

            assertThat(chosenTime).isEqualTo(SECONDS_IN_MONTH_30)
        }
    }

    @Test
    fun `test that the custom picker is not displayed after being set`() {
        with(composeRule) {
            setScreen()

            selectARetentionTimeOption(ChatHistoryRetentionOption.Custom)
            onNodeWithText(R.string.general_ok).performClick()

            onNodeWithTag(CUSTOM_TIME_PICKER_TAG).assertIsNotDisplayed()
        }
    }

    private fun ComposeContentTestRule.selectARetentionTimeOption(option: ChatHistoryRetentionOption) {
        onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).performClick()

        onNode(
            isSelectable().and(hasAnySibling(hasText(fromId(option.stringId)))),
            useUnmergedTree = true
        ).performClick()

        onNodeWithTag(CONFIRMATION_DIALOG_CONFIRM_BUTTON_TAG).performClick()
    }

    @Test
    fun `test that the history retention confirmation is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).performClick()

            onNodeWithTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the history retention confirmation is not displayed when dismissed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).performClick()
            onNodeWithTag(CONFIRMATION_DIALOG_CANCEL_BUTTON_TAG).performClick()

            onNodeWithTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that the switch is on when the selected option is not disabled`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(retentionTime = SECONDS_IN_WEEK.toLong())
            )

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).performClick()

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).assertIsOn()
        }
    }

    @Test
    fun `test that the switch is off when the selected option is disabled`() {
        with(composeRule) {
            setScreen(
                uiState = ManageChatHistoryUIState(retentionTime = DISABLED_RETENTION_TIME)
            )

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).performClick()

            onNodeWithTag(HISTORY_CLEARING_OPTION_SWITCH_TAG).assertIsOff()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: ManageChatHistoryUIState = ManageChatHistoryUIState(),
        onNavigateUp: () -> Unit = {},
        onConfirmClearChatClick: (chatRoomId: Long) -> Unit = {},
        onSetChatRetentionTime: (period: Long) -> Unit = {},
    ) {
        setContent {
            ManageChatHistoryScreen(
                uiState = uiState,
                onNavigateUp = onNavigateUp,
                onConfirmClearChatClick = onConfirmClearChatClick,
                onSetChatRetentionTime = onSetChatRetentionTime,
            )
        }
    }
}
