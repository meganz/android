package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_AUDIO_CALL_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_NOTIFICATION_MUTE
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_PRIVATE_ICON
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_USER_CHAT_STATE
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class ChatViewTest {

    private val actionPressed = mock<(ChatRoomMenuAction) -> Unit>()

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

    @Test
    fun `test that audio call is hidden when my permission is unknown`() {
        initComposeRuleContent(ChatUiState())
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when my permission is removed`() {
        initComposeRuleContent(ChatUiState(myPermission = ChatRoomPermission.Removed))
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when my permission is read only`() {
        initComposeRuleContent(ChatUiState(myPermission = ChatRoomPermission.ReadOnly))
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when is joining or leaving`() {
        initComposeRuleContent(ChatUiState(isJoiningOrLeaving = true))
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when is preview mode`() {
        initComposeRuleContent(ChatUiState(isPreviewMode = true))
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that audio call is shown and enabled when my permission is standard`() {
        initComposeRuleContent(ChatUiState(myPermission = ChatRoomPermission.Standard))
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).performClick()
        verify(actionPressed).invoke(any())
    }

    @Test
    fun `test that audio call is shown and enabled when my permission is moderator`() {
        initComposeRuleContent(
            ChatUiState(myPermission = ChatRoomPermission.Moderator)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).performClick()
        verify(actionPressed).invoke(any())
    }

    @Test
    fun `test that audio call is shown but disabled when my permission is moderator and I have a call in this chat`() {
        initComposeRuleContent(
            ChatUiState(
                myPermission = ChatRoomPermission.Moderator,
                hasACallInThisChat = true,
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_AUDIO_CALL_ACTION).performClick()
        verifyNoInteractions(actionPressed)
    }

    private fun initComposeRuleContent(state: ChatUiState) {
        composeTestRule.setContent {
            ChatView(
                uiState = state,
                onBackPressed = {},
                onMenuActionPressed = actionPressed
            )
        }
    }
}