package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.TopCallButton
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class TopCallButtonTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that start meeting view shows correctly when chat room is pending meeting and available and user doesn't join`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isActive } doReturn true },
                schedIsPending = true,
                callInThisChat = ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.Unknown
                ),
            )
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                R.string.meetings_chat_room_start_scheduled_meeting_option
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that join meeting view shows correctly when chat room is pending meeting and available and user doesn't join`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isActive } doReturn true
                    on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly
                },
                schedIsPending = true,
                callInThisChat = ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.UserNoPresent
                ),
            )
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                R.string.meetings_chat_room_join_scheduled_meeting_option
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that join meeting view does not show  when there is no call in this chat`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isActive } doReturn true
                    on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly
                },
                schedIsPending = true,
                callInThisChat = null,
            )
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                R.string.meetings_chat_room_join_scheduled_meeting_option
            )
        ).assertDoesNotExist()
    }

    @Test
    fun `test that start meeting view does not show  when chat room is readonly for the user`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isActive } doReturn true
                    on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly
                },
                schedIsPending = true,
                callInThisChat = ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.Unknown
                ),
            )
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                R.string.meetings_chat_room_start_scheduled_meeting_option
            )
        ).assertDoesNotExist()
    }

    private fun initComposeRuleContent(uiState: ChatUiState) {
        composeRule.setContent {
            TopCallButton(
                uiState = uiState,
            )
        }
    }
}
