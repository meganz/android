package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.ReturnToCallBanner
import mega.privacy.android.core.ui.controls.chat.TEST_TAG_RETURN_TO_CALL
import mega.privacy.android.core.ui.controls.chat.TEST_TAG_RETURN_TO_CALL_CHRONOMETER
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ReturnToCallBannerTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that banner is shown with correct ui when there is only a call in this 1on1 chat answered`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.InProgress
            on { duration } doReturn 10L
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callInThisChat = callInThisChat,
            )
        )
        with(composeRule) {
            onNodeWithText(activity.getString(R.string.call_in_progress_layout), ignoreCase = true)
                .assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that banner is shown with correct ui when there is only a call in this 1on1 chat not answered and ringing`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
            on { isRinging } doReturn true
        }
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                isConnected = true,
                callInThisChat = callInThisChat,
            )
        )
        with(composeRule) {
            onNodeWithText(activity.getString(R.string.call_in_progress_layout), ignoreCase = true)
                .assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is shown with correct ui when there is only a call in this meeting chat not answered`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isMeeting } doReturn true
                },
                isConnected = true,
                callInThisChat = callInThisChat,
            )
        )
        with(composeRule) {
            onNodeWithText(
                activity.getString(R.string.join_meeting_layout_in_group_call),
                ignoreCase = true
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is shown with correct ui when there is only a call in this 1on1 chat not answered`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                isChatNotificationMute = true,
                callInThisChat = callInThisChat,
            )
        )
        with(composeRule) {
            onNodeWithText(activity.getString(R.string.join_call_layout), ignoreCase = true)
                .assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is shown with correct ui when there is only a call in this group chat not answered`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                callInThisChat = callInThisChat,
            )
        )
        with(composeRule) {
            onNodeWithText(
                activity.getString(R.string.join_call_layout_in_group_call),
                ignoreCase = true
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is shown with correct ui when there is only other chat call and is answered`() {
        val callInOtherChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.InProgress
            on { duration } doReturn 10L
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callsInOtherChats = listOf(callInOtherChat),
            )
        )
        with(composeRule) {
            onNodeWithText(
                activity.getString(R.string.call_in_progress_layout),
                ignoreCase = true
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that banner is not shown if there is no network connection`() {
        initComposeRuleContent(ChatUiState(isConnected = false))
        with(composeRule) {
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is not shown if there are no calls`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callsInOtherChats = emptyList(),
                callInThisChat = null
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is not shown if there is only one call not answered in other chat`() {
        val callInOtherChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callsInOtherChats = listOf(callInOtherChat)
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is not shown if there is only one call not answered in this scheduled meeting chat`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                schedIsPending = true,
                callInThisChat = callInThisChat
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
        }
    }

    @Test
    fun `test that banner is shown with correct ui if there is only one call already answered in other chat`() {
        val callInOtherChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.InProgress
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callsInOtherChats = listOf(callInOtherChat)
            )
        )
        with(composeRule) {
            onNodeWithText(
                activity.getString(R.string.call_in_progress_layout),
                ignoreCase = true
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that banner is shown with correct ui if there are two calls and one is answered`() {
        val callInOtherChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.InProgress
        }
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
                callInThisChat = callInThisChat,
                callsInOtherChats = listOf(callInOtherChat)
            )
        )
        with(composeRule) {
            onNodeWithText(
                activity.getString(R.string.call_in_progress_layout),
                ignoreCase = true
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(uiState: ChatUiState) {
        composeRule.setContent {
            ReturnToCallBanner(
                uiState = uiState,
                isAudioPermissionGranted = true,
                onAnswerCall = {}
            )
        }
    }
}