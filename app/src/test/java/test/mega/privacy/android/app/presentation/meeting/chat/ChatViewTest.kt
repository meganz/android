package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_ADD_PARTICIPANTS_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_VIDEO_CALL_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView
import mega.privacy.android.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import mega.privacy.android.domain.entity.ChatRoomPermission
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ChatViewTest {

    private val actionPressed = mock<(ChatRoomMenuAction) -> Unit>()

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that participating in a call dialog show when click to audio call and user is in another call`() {
        initComposeRuleContent(
            ChatUiState(
                myPermission = ChatRoomPermission.Standard,
                isParticipatingInACall = true
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.ongoing_call_content))
            .assertIsDisplayed()
    }

    @Test
    fun `test that participating in a call dialog doesn't show when click to audio call and user is in another call`() {
        initComposeRuleContent(
            ChatUiState(
                myPermission = ChatRoomPermission.Standard,
                isParticipatingInACall = false
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.ongoing_call_content))
            .assertDoesNotExist()
    }

    @Test
    fun `test that first message header is shown`() {
        initComposeRuleContent(ChatUiState())
        composeTestRule.onNodeWithText(R.string.chat_chatroom_first_message_header_mega_info_text)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.title_mega_confidentiality_empty_screen)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.mega_confidentiality_empty_screen).assertExists()
        composeTestRule.onNodeWithText(R.string.title_mega_confidentiality_empty_screen)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.chat_chatroom_first_message_header_authenticity_info_text)
            .assertExists()

    }

    @Test
    fun `test that no contact to add dialog shows when hasAnyContact is false and user clicks add participant menu action`() {
        initComposeRuleContent(
            ChatUiState(
                hasAnyContact = false,
                myPermission = ChatRoomPermission.Moderator,
                isGroup = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ADD_PARTICIPANTS_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_add_participants_no_contacts_message))
            .assertIsDisplayed()
    }

    @Test
    fun `test that first message header title is shown when chat title is available`() {
        val expectedTitle = "expected title"
        initComposeRuleContent(ChatUiState(title = expectedTitle))
        composeTestRule.onAllNodesWithText(expectedTitle)[1].assertExists()
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

    @Test
    fun `test that all contacts added in a call dialog show when click to add participants to call`() {
        initComposeRuleContent(
            ChatUiState(
                myPermission = ChatRoomPermission.Moderator,
                allContactsParticipateInChat = true,
                isGroup = true,
                hasAnyContact = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ADD_PARTICIPANTS_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_add_participants_no_contacts_left_to_add_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_add_participants_no_contacts_left_to_add_title))
            .assertIsDisplayed()
    }
}