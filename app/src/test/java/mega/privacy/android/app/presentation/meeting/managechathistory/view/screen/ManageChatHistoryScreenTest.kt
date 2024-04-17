package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog.CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText
import test.mega.privacy.android.app.presentation.meeting.model.newChatRoom

@RunWith(AndroidJUnit4::class)
class ManageChatHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that meeting history title is shown when chat room is a meeting`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = newChatRoom(withIsMeeting = true)
                )
            )

            onNodeWithText(R.string.meetings_manage_history_view_title).assertIsDisplayed()
        }
    }

    @Test
    fun `test that chat history title is shown when chat room is not a meeting`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = newChatRoom()
                )
            )

            onNodeWithText(R.string.title_properties_manage_chat).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the screen is navigated up when navigation icon is clicked`() {
        composeRule.apply {
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
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(shouldShowCustomTimePicker = true)
            )

            onNodeWithTag(CUSTOM_TIME_PICKER_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the custom retention time picker is not shown`() {
        composeRule.apply {
            setScreen()

            onNodeWithTag(CUSTOM_TIME_PICKER_TAG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that 'clear meeting history' text is shown when chat room is a meeting`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = newChatRoom(withIsMeeting = true)
                )
            )

            onNodeWithText(R.string.meetings_manage_history_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that 'clear chat history' text is shown when chat room is not a meeting`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = newChatRoom()
                )
            )

            onNodeWithText(R.string.title_properties_clear_chat_history).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the clear chat confirmation is shown`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = newChatRoom(),
                    shouldShowClearChatConfirmation = true
                )
            )

            onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the clear chat confirmation is not shown`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    chatRoom = newChatRoom()
                )
            )

            onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that the chat history retention confirmation is shown`() {
        composeRule.apply {
            setScreen(
                uiState = ManageChatHistoryUIState(
                    shouldShowHistoryRetentionConfirmation = true
                )
            )

            onNodeWithTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the chat history retention confirmation is not shown`() {
        composeRule.apply {
            setScreen()

            onNodeWithTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that correct subtitle is shown when retention time is blank`() {
        composeRule.apply {
            setScreen()

            onNodeWithText(R.string.subtitle_properties_history_retention).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct subtitle is shown when retention time is not blank`() {
        composeRule.apply {
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

        composeRule.apply {
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

    private fun ComposeContentTestRule.setScreen(
        uiState: ManageChatHistoryUIState = ManageChatHistoryUIState(),
        onNavigateUp: () -> Unit = {},
        onConfirmClearChatClick: (chatRoomId: Long) -> Unit = {},
        onClearChatConfirmationDismiss: () -> Unit = {},
        onConfirmRetentionTimeClick: (option: ChatHistoryRetentionOption) -> Unit = {},
        onRetentionTimeConfirmationDismiss: () -> Unit = {},
        onHistoryClearingCheckChange: (value: Boolean) -> Unit = {},
        onCustomTimePickerClick: () -> Unit = {},
    ) {
        setContent {
            ManageChatHistoryScreen(
                uiState = uiState,
                onNavigateUp = onNavigateUp,
                onConfirmClearChatClick = onConfirmClearChatClick,
                onClearChatConfirmationDismiss = onClearChatConfirmationDismiss,
                onConfirmRetentionTimeClick = onConfirmRetentionTimeClick,
                onRetentionTimeConfirmationDismiss = onRetentionTimeConfirmationDismiss,
                onHistoryClearingCheckChange = onHistoryClearingCheckChange,
                onCustomTimePickerClick = onCustomTimePickerClick
            )
        }
    }
}
