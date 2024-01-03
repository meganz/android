package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.TopCallButton
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopCallButtonTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

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
                schedIsPending = true,
                isActive = true,
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

    private fun initComposeRuleContent(uiState: ChatUiState) {
        composeRule.setContent {
            TopCallButton(
                uiState = uiState,
            )
        }
    }
}