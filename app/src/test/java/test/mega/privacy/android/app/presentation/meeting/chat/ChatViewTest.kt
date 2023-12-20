package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_ADD_PARTICIPANTS_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_CLEAR_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_END_CALL_FOR_ALL_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_VIDEO_CALL_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_ENABLE_GEOLOCATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.message.TEST_TAG_FIRST_MESSAGE_HEADER
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_LOCATION
import mega.privacy.android.core.ui.controls.chat.TEST_TAG_ATTACHMENT_ICON
import mega.privacy.android.core.ui.controls.chat.messages.TEST_TAG_LOADING_MESSAGES
import mega.privacy.android.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

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
                currentCall = mock()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.ongoing_call_content))
            .assertIsDisplayed()
    }

    @Test
    fun `test that participating in a call dialog doesn't show when click to audio call and user is in another call`() {
        initComposeRuleContent(
            ChatUiState(
                myPermission = ChatRoomPermission.Standard,
                currentCall = null
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.ongoing_call_content))
            .assertDoesNotExist()
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
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ADD_PARTICIPANTS_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_add_participants_no_contacts_message))
            .assertIsDisplayed()
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
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ADD_PARTICIPANTS_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_add_participants_no_contacts_left_to_add_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_add_participants_no_contacts_left_to_add_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that clear chat confirmation dialog show when click clear`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                myPermission = ChatRoomPermission.Moderator,
                isGroup = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_ACTION).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that end call for all dialog show when click to end call for all menu option`() {
        initComposeRuleContent(
            ChatUiState(
                myPermission = ChatRoomPermission.Moderator,
                callInThisChat = mock(),
                isGroup = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_END_CALL_FOR_ALL_ACTION).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.meetings_chat_screen_dialog_title_end_call_for_all))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.meetings_chat_screen_dialog_description_end_call_for_all))
            .assertIsDisplayed()
    }


    @Test
    fun `test that start meeting view shows correctly when chat room is pending meeting and available and user doesn't join`() {
        initComposeRuleContent(
            ChatUiState(
                schedIsPending = true,
                isActive = true,
                callInThisChat = ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.Unknown
                ),
            )
        )
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                R.string.meetings_chat_room_start_scheduled_meeting_option
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that join meeting view shows correctly when chat room is pending meeting and available and user doesn't join`() {
        initComposeRuleContent(
            ChatUiState(
                schedIsPending = true,
                isActive = true,
                callInThisChat = ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.UserNoPresent
                ),
            )
        )
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                R.string.meetings_chat_room_join_scheduled_meeting_option
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that join current call banner is shown in 1on1 chat, in which there is a call I am not participating yet`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callInThisChat = ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.UserNoPresent
                ),
                isGroup = false,
            )
        )
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.join_call_layout)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that return to call banner is shown when I am already participating in a call`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                currentCall = mock(),
            )
        )
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.call_in_progress_layout)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that enable geolocation dialog shows when geolocation is not enabled and user clicks on location`() {
        initComposeRuleContent(
            ChatUiState(
                isGeolocationEnabled = false
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACHMENT_ICON, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_LOCATION).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ENABLE_GEOLOCATION_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that first message header is shown if there is no more chat history to load`() {
        initComposeRuleContent(
            ChatUiState(
                chatHistoryLoadStatus = ChatHistoryLoadStatus.NONE
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_FIRST_MESSAGE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_LOADING_MESSAGES).assertDoesNotExist()

    }

    @Test
    fun `test that loading messages header is shown if there is more chat history to load`() {
        initComposeRuleContent(
            ChatUiState(
                chatHistoryLoadStatus = ChatHistoryLoadStatus.LOCAL
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_FIRST_MESSAGE_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_LOADING_MESSAGES).assertIsDisplayed()

    }

    private fun initComposeRuleContent(state: ChatUiState) {
        composeTestRule.setContent {
            ChatView(
                uiState = state,
                onBackPressed = {},
                onMenuActionPressed = actionPressed,
            )
        }
    }
}