package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.BottomCallButton
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_BOTTOM_ANSWER_CALL_BUTTON
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_BOTTOM_CALL_ON_HOLD_BUTTON
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_BOTTOM_JOIN_CALL_BUTTON
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class BottomCallButtonTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val enablePassCode = mock<() -> Unit>()
    private val onClick = mock<() -> Unit>()
    private val callOnHold = mock<ChatCall> {
        on { isOnHold } doReturn true
    }
    private val callNotParticipating = mock<ChatCall> {
        on { status } doReturn ChatCallStatus.UserNoPresent
    }

    @Test
    fun `test that call on hold button is shown if call in this chat is on hold and there is a call in other chat I am participating`() {

        initComposeRuleContent(
            ChatUiState(
                callInThisChat = callOnHold,
                callsInOtherChats = mock()
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_CALL_ON_HOLD_BUTTON).assertIsDisplayed()
            onNodeWithText(R.string.call_on_hold).assertIsDisplayed()
        }
    }

    @Test
    fun `test that enable passcode is invoked if call on hold button is clicked`() {
        initComposeRuleContent(
            ChatUiState(
                callInThisChat = callOnHold,
                callsInOtherChats = mock()
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_CALL_ON_HOLD_BUTTON).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(enablePassCode).invoke()
        }
    }

    @Test
    fun `test that join call button is shown if call in this group chat I am not participating yet and there is a call in other chat on hold`() {
        initComposeRuleContent(
            ChatUiState(
                callInThisChat = callNotParticipating,
                callsInOtherChats = listOf(callOnHold),
                isGroup = true
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_JOIN_CALL_BUTTON).assertIsDisplayed()
            onNodeWithText(R.string.title_join_call).assertIsDisplayed()
        }
    }

    @Test
    fun `test that answer call button is shown if call in this 1on1 chat I am not participating yet and there is a call in other chat on hold`() {
        initComposeRuleContent(
            ChatUiState(
                callInThisChat = callNotParticipating,
                callsInOtherChats = listOf(callOnHold),
                isGroup = false
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_ANSWER_CALL_BUTTON).assertIsDisplayed()
            onNodeWithText(R.string.title_join_one_to_one_call).assertIsDisplayed()
        }
    }

    @Test
    fun `test that click is invoked if join call button is clicked`() {
        initComposeRuleContent(
            ChatUiState(
                callInThisChat = callNotParticipating,
                callsInOtherChats = listOf(callOnHold),
                isGroup = true
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_JOIN_CALL_BUTTON).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(onClick).invoke()
        }
    }

    @Test
    fun `test that click is invoked if answer call button is clicked`() {
        initComposeRuleContent(
            ChatUiState(
                callInThisChat = callNotParticipating,
                callsInOtherChats = listOf(callOnHold)
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_ANSWER_CALL_BUTTON).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(onClick).invoke()
        }
    }

    private fun initComposeRuleContent(uiState: ChatUiState) {
        composeRule.setContent {
            BottomCallButton(
                uiState = uiState,
                enablePasscodeCheck = enablePassCode,
                onJoinAnswerCallClick = onClick,
            )
        }
    }
}