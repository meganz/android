package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.ChatView
import mega.privacy.android.app.presentation.meeting.chat.TEST_TAG_NOTIFICATION_MUTE
import mega.privacy.android.app.presentation.meeting.chat.TEST_TAG_PRIVATE_ICON
import mega.privacy.android.app.presentation.meeting.chat.TEST_TAG_USER_CHAT_STATE
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that title shows correctly when passing title to uiState`() {
        val title = "my chat room title"
        initComposeRuleContent(
            ChatUiState(title = title)
        )
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that status icon is not visible when the user chat status is null`() {
        initComposeRuleContent(
            ChatUiState(userChatStatus = null)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_USER_CHAT_STATE).assertDoesNotExist()
    }

    @Test
    fun `test that status icon is not visible when the user chat status is invalid`() {
        initComposeRuleContent(
            ChatUiState(userChatStatus = UserChatStatus.Invalid)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_USER_CHAT_STATE).assertDoesNotExist()
    }

    @Test
    fun `test that status icon is visible when the user chat status is online`() {
        initComposeRuleContent(
            ChatUiState(userChatStatus = UserChatStatus.Online)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_USER_CHAT_STATE).assertIsDisplayed()
    }

    @Test
    fun `test that mute icon is visible when chat notification is mute`() {
        initComposeRuleContent(
            ChatUiState(isChatNotificationMute = true)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_NOTIFICATION_MUTE).assertIsDisplayed()
    }

    @Test
    fun `test that mute icon is hidden when chat notification is mute`() {
        initComposeRuleContent(
            ChatUiState(isChatNotificationMute = false)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_NOTIFICATION_MUTE).assertDoesNotExist()
    }

    @Test
    fun `test that private icon is visible when chat room is private`() {
        initComposeRuleContent(
            ChatUiState(isPrivateChat = true)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_PRIVATE_ICON).assertIsDisplayed()
    }

    @Test
    fun `test that private icon is hidden when chat room is public`() {
        initComposeRuleContent(
            ChatUiState(isPrivateChat = false)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_PRIVATE_ICON).assertDoesNotExist()
    }

    private fun initComposeRuleContent(
        state: ChatUiState,
    ) {
        composeTestRule.setContent {
            ChatView(
                uiState = state,
                onBackPressed = {}
            )
        }
    }
}